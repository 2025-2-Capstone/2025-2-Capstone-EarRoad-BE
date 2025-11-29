# app/services/pipeline.py
from typing import Dict, Any, List
from time import perf_counter
from loguru import logger

from . import tourist_filter
from ..config import settings
from ..models.dto import AnalysisResult, ObjectDetection
from ..services import utils, quality, color, phash, yolo


class PipelineError(RuntimeError):
    """사진 분석 중 오류가 발생한 경우"""

def _clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def _score_colorfulness(colorfulness: float) -> float:
    max_ref = settings.SCORE_COLORFULNESS_MAX
    if max_ref <= 0:
        return 0.0
    return _clamp01(colorfulness / max_ref)


def _score_warm_ratio(warm_ratio: float) -> float:
    tolerance = settings.SCORE_WARM_TOLERANCE
    if tolerance <= 0:
        return 0.0
    delta = abs(warm_ratio - settings.SCORE_WARM_TARGET)
    return _clamp01(1.0 - (delta / tolerance))


def _score_objects(objects: List[ObjectDetection]) -> float:
    label_weights = settings.SCORE_OBJECT_LABEL_WEIGHTS or {}
    default_weight = settings.SCORE_OBJECT_DEFAULT_WEIGHT
    score = 0.0
    for obj in objects:
        weight = label_weights.get(obj.label, default_weight)
        score += max(0.0, obj.confidence) * max(0.0, weight)
    return score


def score_analysis(
        *,
        quality_passed: bool,
        colorfulness: float,
        warm_ratio: float,
        objects: List[ObjectDetection],
) -> float:
    if not quality_passed:
        return 0.0

    color_score = _score_colorfulness(colorfulness)
    warm_score = _score_warm_ratio(warm_ratio)
    object_score = _score_objects(objects)

    total = (
            settings.SCORE_QUALITY_BONUS
            + settings.SCORE_COLOR_WEIGHT * color_score
            + settings.SCORE_WARM_WEIGHT * warm_score
            + settings.SCORE_OBJECT_WEIGHT * object_score
    )
    return max(0.0, total)

def analyze_photo_pipeline(
        file_bytes: bytes,
        poi_key: str,
        session_id: str,
        *,
        max_side,           # 이미지 리사이즈 제한
        blur_threshold,  # 샤프니스 기준
        occlusion_threshold,  # 렌즈가림 기준
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

        c_dt, y_dt, t_dt = 0.0, 0.0, 0.0

        if passed:
            # 관광지 필터
            t_t0 = perf_counter()
            filter_result = tourist_filter.classify_tourist(
                img_bgr,
                backbone_path=settings.TOURIST_BACKBONE_PATH,
                mlp_path=settings.TOURIST_MLP_PATH,
                device=settings.TOURIST_DEVICE,
            )
            t_dt = (perf_counter() - t_t0) * 1000.0
            if filter_result is not None and not filter_result.is_tourist:
                passed = False


        if passed:
            #  색채 분석
            c_t0 = perf_counter()
            colorfulness_val = float(color.colorfulness_hasler(img_bgr))
            warm_ratio_val = float(color.warm_ratio_lab(img_bgr))
            c_dt = (perf_counter() - c_t0) * 1000.0

            #  객체 탐지 (더미/YOLO)
            y_t0 = perf_counter()
            objects_val = yolo.detect_objects(
                img_bgr=img_bgr,
                model_path=settings.MODEL_PATH,  # ← 명시 전달 (preload가 안돼도 안전)
                imgsz=settings.YOLO_IMG_SIZE,
                conf_threshold=settings.YOLO_CONF,
                max_det=settings.YOLO_MAX_DET,
                device=settings.YOLO_DEVICE,
            )
            y_dt = (perf_counter() - y_t0) * 1000.0

        total_ms = (perf_counter() - t0) * 1000.0

        # 로깅
        logger.info(
            "[sessionId={}] [poiKey={}] quality={} "
            "timing(ms)={{quality:{:.1f}, phash:{:.1f}, tourist_filter:{:.f}, color:{:.1f}, yolo:{:.1f}, total:{:.1f}}}",
            session_id, poi_key, passed, q_dt, h_dt, t_dt, c_dt, y_dt, total_ms
        )


        object_models = [ObjectDetection(**o) if isinstance(o, dict) else o for o in objects_val]
        score = score_analysis(
            quality_passed=passed,
            colorfulness=colorfulness_val,
            warm_ratio=warm_ratio_val,
            objects=object_models,
        )
        logger.info(f"Score={score:.8f} ")

        # 결과 모델 구성
        result = AnalysisResult(
            qualityPassed=passed,
            colorfulness=colorfulness_val,
            warmRatio=warm_ratio_val,
            pHash=p_hash,
            objects=object_models,
            score=score,
        )

        return result.model_dump()

    except Exception as e:
        logger.exception(f"[sessionId={session_id}] 분석 중 오류: {e}")
        raise PipelineError(str(e))
