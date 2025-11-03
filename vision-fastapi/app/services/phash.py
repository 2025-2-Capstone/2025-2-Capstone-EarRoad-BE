# app/services/phash.py
from typing import Tuple
import numpy as np
import cv2


def compute_phash(
        img_bgr: np.ndarray,
        *,
        hash_size: int = 8,
        highfreq_factor: int = 4
) -> str:
    """
    Perceptual hash (pHash) 계산 후 16진 문자열(hex)로 반환

    Args:
    img_bgr: OpenCV BGR 이미지
    hash_size: 최종 해시 블록 크기(기본 8 → 8x8 = 64bit)
    highfreq_factor: DCT 전 리사이즈 배수(기본 4 → 32x32로 DCT)

    Returns:
    str: 16자 16진수(64bit) 해시
    """
    if img_bgr is None or img_bgr.ndim != 3 or img_bgr.shape[2] != 3:
        return "0" * (hash_size * hash_size // 4)

    # GRAY & 리사이징
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    size = hash_size * highfreq_factor
    gray_small = cv2.resize(gray, (size, size), interpolation=cv2.INTER_AREA)

    # 3) DCT (float32)
    # cv2.dct 입력은 float32/64. overflow 방지 위해 정규화
    gray_f = np.float32(gray_small)
    dct = cv2.dct(gray_f)

    dct_low = dct[:hash_size, :hash_size].copy()
    dct_flat = dct_low.flatten()
    if dct_flat.size <= 1:
        return "0" * (hash_size * hash_size // 4)
    median = np.median(dct_flat[1:])  # [0]은 DC 성분

    # 이진화
    bits = (dct_low > median).flatten().astype(np.uint8)

    # 16진 변환
    return _bits_to_hex(bits)


def _bits_to_hex(bits: np.ndarray) -> str:
    """
    (hash_size=8 기준, 64bit - 16 hex chars)
    """
    value = 0
    for b in bits.tolist():
        value = (value << 1) | int(b)
    # 총 비트 수 → hex 자릿수 계산
    total_bits = bits.size
    hex_len = total_bits // 4
    return f"{value:0{hex_len}x}"


def hamming_distance_hex(h1: str, h2: str) -> int:
    """
    16진 pHash 문자열 두 개의 해밍거리 계산
    (길이가 다르면 짧은 쪽을 왼쪽 0 padding하여 길이 맞춤)
    """
    if not h1 and not h2:
        return 0
    max_len = max(len(h1), len(h2))
    a = int(h1.zfill(max_len), 16)
    b = int(h2.zfill(max_len), 16)
    x = a ^ b
    # Python 3.9: int.bit_count() 사용 가능
    return x.bit_count()
