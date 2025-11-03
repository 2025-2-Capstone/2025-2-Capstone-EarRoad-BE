# app/services/utils.py
from io import BytesIO
from typing import Tuple, Optional

import numpy as np
from PIL import Image, ImageOps
import cv2


def _pil_open_with_exif_orientation(file_bytes: bytes) -> Optional[Image.Image]:
    """
    바이트에서 PIL 이미지 로드 + EXIF 회전 보정.
    실패 시 None 반환.
    """
    try:
        img = Image.open(BytesIO(file_bytes))
        # 일부 이미지의 EXIF Orientation을 실제 픽셀 회전으로 반영
        img = ImageOps.exif_transpose(img)
        # 일관된 처리 위해 RGB로 통일 (P/L/CMYK 등 변환)
        if img.mode not in ("RGB", "RGBA"):
            img = img.convert("RGB")
        return img
    except Exception:
        return None


def _pil_to_cv_bgr(img: Image.Image) -> np.ndarray:
    """
    PIL.Image → OpenCV BGR ndarray
    RGBA인 경우 알파 채널은 드롭(RGB로 변환 후 진행).
    """
    if img.mode == "RGBA":
        img = img.convert("RGB")
    arr = np.array(img)  # RGB ndarray (H, W, 3), dtype=uint8
    # RGB → BGR
    bgr = arr[:, :, ::-1].copy()
    return bgr


def load_image_from_bytes(file_bytes: bytes) -> Optional[np.ndarray]:
    """
    입력 바이트를 OpenCV BGR(ndarray, uint8)로 반환.
    - EXIF 회전 보정
    - 색공간 RGB→BGR 변환
    실패 시 None.
    """
    pil_img = _pil_open_with_exif_orientation(file_bytes)
    if pil_img is None:
        return None
    try:
        bgr = _pil_to_cv_bgr(pil_img)
        # 안전장치: dtype/차원 확인
        if not isinstance(bgr, np.ndarray) or bgr.ndim != 3 or bgr.shape[2] != 3:
            return None
        if bgr.dtype != np.uint8:
            bgr = bgr.astype(np.uint8, copy=False)
        return bgr
    except Exception:
        return None


def resize_max_side(img_bgr: np.ndarray, max_side: int = 1024) -> np.ndarray:
    """
    긴 변 기준 리사이즈(종횡비 유지). max_side보다 크면 축소, 작으면 그대로 반환.
    - img_bgr: OpenCV BGR 이미지 (H, W, 3)
    - max_side: 긴 변의 최대 길이
    """
    if img_bgr is None or img_bgr.ndim != 3:
        return img_bgr

    h, w = img_bgr.shape[:2]
    long_side = max(h, w)
    if long_side <= max_side or long_side == 0:
        return img_bgr

    scale = float(max_side) / float(long_side)
    new_w = max(1, int(round(w * scale)))
    new_h = max(1, int(round(h * scale)))

    # OpenCV의 INTER_AREA는 축소에 적합
    resized = cv2.resize(img_bgr, (new_w, new_h), interpolation=cv2.INTER_AREA)
    return resized
