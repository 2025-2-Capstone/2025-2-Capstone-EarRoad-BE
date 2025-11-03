# app/services/color.py
from typing import Tuple
import numpy as np
import cv2


def colorfulness_hasler(img_bgr: np.ndarray) -> float:
    """
    Hasler & Süsstrunk(2003) 방식의 colorfulness 지표.
    BGR → RGB 변환 후,
      rg = R - G
      yb = 0.5*(R + G) - B
    를 사용해 표준편차/평균 기반으로 계산.
    반환: float (상대 비교용 지표, 값이 클수록 색채가 풍부)
    """
    if img_bgr is None or img_bgr.ndim != 3 or img_bgr.shape[2] != 3:
        return 0.0

    rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB).astype(np.float32)
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]

    rg = r - g
    yb = 0.5 * (r + g) - b

    # 표준 편차 및 평균
    std_rg = float(np.std(rg))
    std_yb = float(np.std(yb))
    mean_rg = float(np.mean(rg))
    mean_yb = float(np.mean(yb))

    # Hasler 공식
    std_root = np.sqrt(std_rg * std_rg + std_yb * std_yb)
    mean_root = np.sqrt(mean_rg * mean_rg + mean_yb * mean_yb)
    C = float(std_root + 0.3 * mean_root)

    # 수치 안정화: NaN/inf 방지
    if not np.isfinite(C):
        return 0.0
    return C


def warm_ratio_lab(
        img_bgr: np.ndarray,
        *,
        a_thresh: float = 5.0,
        b_thresh: float = 5.0,
        l_min: float = 20.0
) -> float:
    """
    LAB 공간에서 '따뜻한 톤' 비율을 근사 계산.
      - L*: 명도 (너무 어두운 픽셀 제외: L* >= l_min)
      - a*: 녹색(-)↔적색(+) 축 (적색 쪽이면 warm 경향)
      - b*: 청색(-)↔황색(+) 축 (황색 쪽이면 warm 경향)
    조건: L* >= l_min AND a* > a_thresh AND b* > b_thresh
    반환: 0.0 ~ 1.0 (warm 픽셀 비율)
    """
    if img_bgr is None or img_bgr.ndim != 3 or img_bgr.shape[2] != 3:
        return 0.0

    lab = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2LAB).astype(np.float32)
    L = lab[:, :, 0] * (100.0 / 255.0)  # OpenCV L range [0,255] → [0,100]로 정규화
    a = lab[:, :, 1] - 128.0            # OpenCV a,b는 [0,255] 중심 128
    b = lab[:, :, 2] - 128.0

    valid_mask = (L >= l_min)
    if not np.any(valid_mask):
        return 0.0

    warm_mask = valid_mask & (a > a_thresh) & (b > b_thresh)
    warm_count = int(np.count_nonzero(warm_mask))
    valid_count = int(np.count_nonzero(valid_mask))
    ratio = float(warm_count) / float(valid_count) if valid_count > 0 else 0.0

    # 수치 안정화
    if not np.isfinite(ratio):
        return 0.0
    # 범위 클램프
    return float(max(0.0, min(1.0, ratio)))
