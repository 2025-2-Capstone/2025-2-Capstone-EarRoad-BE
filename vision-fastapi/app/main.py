import gc
import os
import shutil
import sys
from contextlib import asynccontextmanager
from typing import List

from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException, status
from fastapi.responses import PlainTextResponse
from loguru import logger
from .config import settings
from .deps import verify_shared_token
from .models.dto import AnalysisResult
from .services import yolo
from .services.pipeline import analyze_photo_pipeline, PipelineError


_LOG_SINK_IDS: List[int] = []
_TMP_DIRS: List[str] = []  # 필요 시 임시 디렉터리 경로를 여기에 append

def setup_logging() -> None:
    """Loguru 로거: 콘솔 + 파일(순환) 동시 설정"""
    os.makedirs(settings.LOG_DIR, exist_ok=True)
    logger.remove()  # 기본 핸들러 제거

    # 콘솔
    logger.add(sys.stdout, level="INFO", enqueue=True, backtrace=False, diagnose=False)

    # 파일 (10MB 회전, 14일 보관)
    """
    logger.add(
        os.path.join(settings.LOG_DIR, "vision.log"),
        level="INFO",
        rotation="10 MB",
        retention="14 days",
        compression="gz",
        enqueue=True,
        serialize=False,   # ELK 연동 시 True로 변경하여 JSON 로그 출력
    )
    """

def _cleanup_logging() -> None:
    # 등록된 sink를 역순으로 제거(닫기)
    while _LOG_SINK_IDS:
        sink_id = _LOG_SINK_IDS.pop()
        try:
            logger.remove(sink_id)
        except Exception:
            pass

def _cleanup_yolo() -> None:
    """
    YOLO 모델/메모리 정리:
      - 전역 모델 참조 해제
      - GPU 사용 시 캐시 비우기
      - 가비지 컬렉션 강제
    """
    try:
        # 전역 모델 캐시 해제 (yolo 모듈 내부 전역)
        if getattr(yolo, "_YOLO_MODEL", None) is not None:
            try:
                # torch가 있으면 GPU 캐시도 비우기
                import torch  # type: ignore
                if torch.cuda.is_available():
                    torch.cuda.empty_cache()
            except Exception:
                pass
            # 참조 해제
            try:
                del yolo._YOLO_MODEL  # type: ignore[attr-defined]
            except Exception:
                yolo._YOLO_MODEL = None  # type: ignore[attr-defined]
    except Exception:
        pass
    finally:
        gc.collect()

def _cleanup_tmpdirs() -> None:
    """
    임시 디렉터리(있다면) 정리. 현재는 사용 계획 없지만 훅만 남겨둠.
    """
    for path in list(_TMP_DIRS):
        try:
            shutil.rmtree(path, ignore_errors=True)
        except Exception:
            pass
        finally:
            _TMP_DIRS.remove(path)

@asynccontextmanager
async def lifespan(app: FastAPI):
    setup_logging()
    try:
        if settings.MODEL_PATH:
            yolo.get_yolo_model(settings.MODEL_PATH)  # 선택적 프리로드
    except Exception as e:
        logger.warning(f"YOLO preload skipped: {e}")

    # 여기서 앱 실행
    yield

    # 앱 종료
    try:
        _cleanup_yolo()
    finally:
        try:
            _cleanup_tmpdirs()
        finally:
            _cleanup_logging()


app = FastAPI(title="Guiro Vision API", version="0.2.0")


@app.get("/health", response_class=PlainTextResponse)
async def health():
    """서버 상태 확인용"""
    return "OK"


@app.post(
    "/analyze/photo",
    response_model=AnalysisResult,
    dependencies=[Depends(verify_shared_token)],
)
async def analyze_photo(
        image: UploadFile = File(..., description="단일 이미지 파일"),
        poiKey: str = Form(...),
        sessionId: str = Form(...),
):
    """
    - 입력: 1장 이미지 + poiKey + sessionId (multipart/form-data)
    - 동작: 품질검사 → (통과 시) 색감/YOLO → pHash 포함 분석
    - 출력: AnalysisResult(JSON)
      * 품질 불통과 시 colorfulness/warmRatio=0, objects=[]
    """
    # 파일 크기 제한
    content = await image.read()
    size_mb = len(content) / (1024 * 1024)
    if size_mb > settings.MAX_IMAGE_MB:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Image larger than {settings.MAX_IMAGE_MB}MB",
        )

    logger.info(
        "[/analyze/photo] sessionId={}, poiKey={}, file={}, size={:.2f}MB",
        sessionId, poiKey, image.filename, size_mb
    )

    try:
        result_dict = analyze_photo_pipeline(
            file_bytes=content,
            poi_key=poiKey,
            session_id=sessionId,
            max_side=1024,
            blur_threshold=100.0,
            occlusion_threshold=0.7,
        )
        # pydantic v2: 모델 인스턴스로 반환하면 FastAPI가 알아서 JSON 직렬화
        return AnalysisResult(**result_dict)

    except PipelineError as e:
        raise HTTPException(status_code=400, detail=f"Pipeline error: {str(e)}")
    except Exception as e:
        logger.exception(f"Unexpected error: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")
