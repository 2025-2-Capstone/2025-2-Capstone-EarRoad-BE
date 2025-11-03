# app/services/pipeline.py
from typing import Dict, Any
from time import perf_counter
from loguru import logger

from app.models.dto import AnalysisResult, ObjectDetection
from app.services import utils, quality, color, phash, yolo


class PipelineError(RuntimeError):
    """사진 분석 중 오류가 발생한 경우"""


def analyze_photo_pipeline(
        file_bytes: bytes,
        poi_key: str,
        session_id: str,
        *,
        max_side: int = 1024,           # 이미지 리사이즈 제한
        blur_threshold: float = 100.0,  # 샤프니스 기준
        occlusion_threshold: float = 0.7,  # 렌즈가림 기준
) -> Dict[str, Any]:
    """
    단일 사진에 대한 전체 분석 파이프라인
    """
    t0 = perf_counter()
    try:
        # 이미지 로드
        img_bgr = utils.load_image_from_bytes(file_bytes)
        if img_bgr is None:
            raise PipelineError("이미지 로드 실패")

        # 리사이즈
        img_bgr = utils.resize_max_side(img_bgr, max_side=max_side)

        # 품질검사 (early-stop)
        q_t0 = perf_counter()
        passed = quality.quality_check(
            img_bgr,
            blur_threshold=blur_threshold,
            occlusion_threshold=occlusion_threshold,
        )
        q_dt = (perf_counter() - q_t0) * 1000.0

        # pHash (중복검사용)
        h_t0 = perf_counter()
        p_hash = phash.compute_phash(img_bgr)
        h_dt = (perf_counter() - h_t0) * 1000.0

        # 품질 불통과 시 조기 종료
        colorfulness_val, warm_ratio_val, objects_val = 0.0, 0.0, []

        c_dt, y_dt = 0.0, 0.0
        if passed:
            #  색채 분석
            c_t0 = perf_counter()
            colorfulness_val = float(color.colorfulness_hasler(img_bgr))
            warm_ratio_val = float(color.warm_ratio_lab(img_bgr))
            c_dt = (perf_counter() - c_t0) * 1000.0

            #  객체 탐지 (더미/YOLO)
            y_t0 = perf_counter()
            objects_val = yolo.detect_objects(img_bgr)
            y_dt = (perf_counter() - y_t0) * 1000.0

        total_ms = (perf_counter() - t0) * 1000.0

        # 로깅
        logger.info(
            "[sessionId={}] [poiKey={}] quality={} "
            "timing(ms)={{quality:{:.1f}, phash:{:.1f}, color:{:.1f}, yolo:{:.1f}, total:{:.1f}}}",
            session_id, poi_key, passed, q_dt, h_dt, c_dt, y_dt, total_ms
        )

        # 결과 모델 구성
        result = AnalysisResult(
            qualityPassed=passed,
            colorfulness=colorfulness_val,
            warmRatio=warm_ratio_val,
            pHash=p_hash,
            objects=[ObjectDetection(**o) if isinstance(o, dict) else o for o in objects_val],
        )
        return result.model_dump()

    except Exception as e:
        logger.exception(f"[sessionId={session_id}] 분석 중 오류: {e}")
        raise PipelineError(str(e))
