# app/services/yolo.py
from __future__ import annotations
import threading
from importlib import import_module
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

# torch는 장치 선택/캐시 비우기용으로만 선택적 사용
try:
    import torch  # type: ignore
    _TORCH_AVAILABLE = True
except Exception:
    torch = None  # type: ignore
    _TORCH_AVAILABLE = False

# 전역 캐시 (lazy load)
_YOLO_MODEL: Optional[Any] = None
_YOLO_LOCK = threading.Lock()

def _select_device(explicit: Optional[str] = None) -> str:
    """
    장치 선택: 명시값 우선 → CUDA → MPS(Apple) → CPU
    - predict()에 device=str 로 넘겨 사용 (모델은 CPU에 두어도 됨)
    """
    if explicit:
        return explicit
    if _TORCH_AVAILABLE:
        try:
            if torch.cuda.is_available():
                return "cuda:0"
            # Apple Silicon일 때 가끔 mps 사용 가능
            if hasattr(torch.backends, "mps") and torch.backends.mps.is_available():  # type: ignore
                return "mps"
        except Exception:
            pass
    return "cpu"

def _load_ultralytics_yolo_cls() -> Optional[Any]:
    """
    전역에 YOLO=None을 두지 않고, 필요할 때만 안전하게 로컬 import.
    IDE가 'YOLO가 None일 수 있음' 경고하는 문제를 없앰.
    """
    if not _ULTRALYTICS_AVAILABLE:
        return None
    try:
        mod = import_module("ultralytics")
        yolo_cls = getattr(mod, "YOLO", None)
        return yolo_cls
    except Exception:
        return None

def get_yolo_model(model_path: Optional[str] = None):
    """
    YOLO 모델을 지연 로드해서 전역 캐시에 보관.
    - model_path가 없거나 ultralytics 미설치면 None 유지(더미 모드).
    """
    global _YOLO_MODEL

    if _YOLO_MODEL is not None:
        return _YOLO_MODEL

    yolo_cls = _load_ultralytics_yolo_cls()

    if not _ULTRALYTICS_AVAILABLE:
        logger.warning("ultralytics가 설치되지 않아 YOLO가 비활성화됩니다 (더미 모드).")
        _YOLO_MODEL = None
        return _YOLO_MODEL

    if not model_path:
        logger.warning("YOLO model_path가 비어 있어 YOLO가 비활성화됩니다 (더미 모드).")
        _YOLO_MODEL = None
        return _YOLO_MODEL

    with _YOLO_LOCK:
        if _YOLO_MODEL is not None:
            return _YOLO_MODEL
        try:
            _YOLO_MODEL = yolo_cls(model_path)
            # model.names 존재 여부 로그
            names = getattr(_YOLO_MODEL, "names", None)
            if isinstance(names, dict):
                logger.info(f"YOLO 모델 로드 완료: {model_path} (classes={len(names)})")
            else:
                logger.info(f"YOLO 모델 로드 완료: {model_path}")
        except Exception as e:
            logger.exception(f"YOLO 모델 로드 실패: {e} (더미 모드로 계속)")
            _YOLO_MODEL = None

    return _YOLO_MODEL


def warmup_yolo(*, model_path: Optional[str] = None, imgsz: int = 640, device: Optional[str] = None) -> bool:
    """
    첫 호출 지연을 줄이기 위한 더미 예열.
    """
    model = get_yolo_model(model_path=model_path)
    if model is None:
        return False

    dev = _select_device(device)
    try:
        import cv2  # 지역 import (headless 환경 고려)
        dummy = np.zeros((imgsz, imgsz, 3), dtype=np.uint8)
        _ = model.predict(source=cv2.cvtColor(dummy, cv2.COLOR_BGR2RGB),
                          imgsz=imgsz, conf=0.25, max_det=1, device=dev, verbose=False)
        logger.info(f"YOLO warmup 완료 (device={dev}, imgsz={imgsz})")
        return True
    except Exception as e:
        logger.warning(f"YOLO warmup 실패: {e}")
        return False


def unload_yolo():
    """
    전역 모델 해제 및 (가능하면) 디바이스 캐시 정리.
    - lifespan의 shutdown에서 호출 추천.
    """
    global _YOLO_MODEL
    with _YOLO_LOCK:
        _YOLO_MODEL = None
    # torch GPU 캐시 비우기
    if _TORCH_AVAILABLE:
        try:
            if torch.cuda.is_available():
                torch.cuda.empty_cache()
                logger.info("torch CUDA 캐시 비움")
        except Exception:
            pass
    logger.info("YOLO 언로드 완료")



def detect_objects(
        img_bgr: np.ndarray,
        *,
        conf_threshold: float = 0.25,
        max_det: int = 20,
        imgsz: int = 640,
        classes: Optional[List[int]] = None,   # 추론 대상 클래스 화이트리스트 (인덱스)
        device: Optional[str] = None,          # "cpu" | "cuda:0" | "mps" ...
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
        dev = _select_device(device)

        results = model.predict(
            source=rgb,
            imgsz=imgsz,
            conf=conf_threshold,
            max_det=max_det,
            classes=classes,
            device=dev,
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
            label = names.get(int(k), str(int(k))) if isinstance(names, dict) else str(int(k))
            detections.append({
                "label": label,
                "confidence": float(c),
                "bbox": [float(x), float(y), float(w), float(h)],
            })

        return detections

    except Exception as e:
        logger.exception(f"YOLO 추론 중 예외: {e} (더미 모드로 [])")
        return []
