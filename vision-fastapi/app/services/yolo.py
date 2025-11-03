# app/services/yolo.py
from typing import List, Dict, Any, Optional
import numpy as np
from loguru import logger

# optional import (없어도 동작해야 하므로 try/except)
try:
    from ultralytics import YOLO  # type: ignore
    _ULTRALYTICS_AVAILABLE = True
except Exception:
    YOLO = None  # type: ignore
    _ULTRALYTICS_AVAILABLE = False

# 전역 캐시 (lazy load)
_YOLO_MODEL = None  # type: Optional[Any]


def get_yolo_model(model_path: Optional[str] = None):
    """
    YOLO 모델을 지연 로드해서 전역 캐시에 보관.
    - model_path가 없거나 ultralytics 미설치면 None 유지(더미 모드).
    """
    global _YOLO_MODEL

    if _YOLO_MODEL is not None:
        return _YOLO_MODEL

    if not _ULTRALYTICS_AVAILABLE:
        logger.warning("ultralytics가 설치되지 않아 YOLO가 비활성화됩니다 (더미 모드).")
        _YOLO_MODEL = None
        return _YOLO_MODEL

    if not model_path:
        logger.warning("YOLO model_path가 비어 있어 YOLO가 비활성화됩니다 (더미 모드).")
        _YOLO_MODEL = None
        return _YOLO_MODEL

    try:
        _YOLO_MODEL = YOLO(model_path)  # type: ignore
        logger.info(f"YOLO 모델 로드 완료: {model_path}")
    except Exception as e:
        logger.exception(f"YOLO 모델 로드 실패: {e} (더미 모드로 계속)")
        _YOLO_MODEL = None

    return _YOLO_MODEL


def detect_objects(
        img_bgr: np.ndarray,
        *,
        conf_threshold: float = 0.25,
        max_det: int = 20,
        model_path: Optional[str] = None
) -> List[Dict[str, Any]]:
    """
    입력: OpenCV BGR 이미지
    반환: [{label:str, confidence:float, bbox:[x,y,w,h]}, ...]
    - 더미 모드(모델 없음/미설치/로드 실패)는 항상 [] 반환.
    - 실제 YOLO 연결 시에만 결과를 구성.
    """
    model = get_yolo_model(model_path=model_path)
    if model is None:
        return []  # 더미 동작

    try:
        # ultralytics YOLO는 입력으로 RGB를 기대 → 변환 필요
        import cv2  # 지역 import (headless 환경 고려)
        rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

        results = model.predict(
            source=rgb,
            conf=conf_threshold,
            max_det=max_det,
            verbose=False
        )

        detections: List[Dict[str, Any]] = []
        if not results:
            return detections

        r0 = results[0]
        # r0.boxes.xywh / r0.boxes.conf / r0.boxes.cls 등 사용
        boxes = getattr(r0, "boxes", None)
        names = getattr(model, "names", {})  # 클래스 인덱스 → 라벨명

        if boxes is None:
            return detections

        xywh = getattr(boxes, "xywh", None)
        conf = getattr(boxes, "conf", None)
        cls  = getattr(boxes, "cls", None)

        if xywh is None or conf is None or cls is None:
            return detections

        # 텐서를 numpy로 변환
        xywh = xywh.cpu().numpy()
        conf = conf.cpu().numpy()
        cls  = cls.cpu().numpy()

        for (x, y, w, h), c, k in zip(xywh, conf, cls):
            label = names.get(int(k), str(int(k)))
            detections.append({
                "label": label,
                "confidence": float(c),
                "bbox": [float(x), float(y), float(w), float(h)],
            })

        return detections

    except Exception as e:
        logger.exception(f"YOLO 추론 중 예외: {e} (더미 모드로 [])")
        return []
