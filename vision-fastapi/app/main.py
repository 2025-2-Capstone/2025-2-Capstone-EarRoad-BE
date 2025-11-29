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
from .services.pipeline import analyze_photo_pipeline, PipelineError
from .services.yolo import warmup_yolo, unload_yolo

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
    # --- startup ---
    setup_logging()
    try:
        if settings.MODEL_PATH:
            ok = warmup_yolo(model_path=settings.MODEL_PATH, imgsz=640)
            logger.info("YOLO preload/warmup: {} (MODEL_PATH={})", ok, settings.MODEL_PATH)
        else:
            logger.info("MODEL_PATH 비어있음 → YOLO 더미 모드로 시작")
    except Exception:
        logger.exception("YOLO preload/warmup 중 예외 (더미 모드로 계속)")

    # 애플리케이션 실행
    yield

    # --- shutdown ---
    try:
        unload_yolo()  # ← yolo.py의 안전한 언로드/캐시 비움
    except Exception:
        logger.exception("YOLO unload 중 예외")
    finally:
        try:
            _cleanup_tmpdirs()
        except Exception:
            logger.exception("임시 디렉터리 정리 중 예외")
        finally:
            try:
                _cleanup_logging()
            except Exception:
                # 마지막 정리이므로 로거 예외는 억제
                pass
            gc.collect()


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
            blur_threshold=2000.0,
            occlusion_threshold=0.7,
        )

        # pydantic v2: 모델 인스턴스로 반환하면 FastAPI가 알아서 JSON 직렬화
        return AnalysisResult(**result_dict)

    except PipelineError as e:
        raise HTTPException(status_code=400, detail=f"Pipeline error: {str(e)}")
    except Exception as e:
        logger.exception(f"Unexpected error: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")
