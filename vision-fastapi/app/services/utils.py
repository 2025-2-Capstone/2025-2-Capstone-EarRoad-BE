# app/services/utils.py
from io import BytesIO
from typing import Optional
import numpy as np
from loguru import logger

try:
    from pillow_heif import register_heif_opener  # HEIC/HEIF 지원
    register_heif_opener()
    _HEIF_AVAILABLE = True
except Exception:
    _HEIF_AVAILABLE = False

from PIL import Image, ImageOps, ImageFile
import cv2

def _pil_open_with_exif_orientation(file_bytes: bytes) -> Optional[Image.Image]:
    """
    바이트에서 PIL 이미지 로드 + EXIF 회전 보정
    - JPEG, PNG, WEBP, HEIC, HEIF, BMP 등 대부분의 포맷 지원
    - 트렁케이트된 JPEG 허용
    """
    try:
        # 손상된 JPEG 일부 허용
        ImageFile.LOAD_TRUNCATED_IMAGES = True

        img = Image.open(BytesIO(file_bytes))
        img = ImageOps.exif_transpose(img)  # EXIF 회전 보정

        # 색공간 정규화
        if img.mode not in ("RGB", "RGBA"):
            img = img.convert("RGB")
        return img
    except Exception as e:
        logger.warning(f"Pillow load 실패: {e}")
        return None


def _pil_to_cv_bgr(img: Image.Image) -> np.ndarray:
    """
    PIL.Image - OpenCV BGR ndarray
    RGBA인 경우 RGB로 변환 후 진행
    """
    if img.mode == "RGBA":
        img = img.convert("RGB")
    arr = np.array(img, dtype=np.uint8)  # RGB ndarray (H, W, 3), dtype=uint8
    # RGB - BGR
    bgr = arr[:, :, ::-1].copy()
    return bgr


def load_image_from_bytes(file_bytes: bytes) -> Optional[np.ndarray]:
    """
    입력 바이트를 OpenCV BGR(ndarray, uint8)로 반환
    - EXIF 회전 보정
    - 색공간 RGB - BGR 변환
    실패 시 None.
    """
    # Pillow 경로
    pil_img = _pil_open_with_exif_orientation(file_bytes)
    if pil_img is not None:
        try:
            bgr = _pil_to_cv_bgr(pil_img)
            if isinstance(bgr, np.ndarray) and bgr.ndim == 3 and bgr.shape[2] == 3:
                return bgr
        except Exception as e:
            logger.warning(f"Pillow-OpenCV 변환 실패: {e}")

    # OpenCV 폴백 (raw JPEG 등)
    try:
        arr = np.frombuffer(file_bytes, dtype=np.uint8)
        img_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img_bgr is not None and img_bgr.ndim == 3:
            return img_bgr
    except Exception as e:
        logger.warning(f"cv2.imdecode 실패: {e}")

    # 실패 시 None
    logger.warning("이미지 디코딩 실패 (지원되지 않는 포맷이거나 손상된 파일)")
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
