import numpy as np
import cv2


def compute_sharpness_laplacian(img_bgr: np.ndarray) -> float:
    """
    라플라시안 분산값이 낮을수록 흐릿함
    """
    if img_bgr is None or img_bgr.ndim != 3:
        return 0.0
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    lap = cv2.Laplacian(gray, ddepth=cv2.CV_64F, ksize=3)
    return float(lap.var()) if lap.size else 0.0


def occlusion_score(img_bgr: np.ndarray) -> float:
    """
    렌즈 가림(손가락 등) 판단용 간단 지표
    - 스킨톤 비율(YCrCb)
    - 저주파(blurred) 비율
    두 지표를 0~1 스케일로 반환, 클수록 가림 확률 증가
    """
    if img_bgr is None or img_bgr.ndim != 3:
        return 0.0

    # 1) 스킨톤 비율 (대략적 범위)
    ycrcb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2YCrCb)
    skin_mask = cv2.inRange(ycrcb, (0, 135, 85), (255, 180, 135))
    skin_ratio = skin_mask.mean() / 255.0  # 0~1

    # 2) 저주파 비율
    gray = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2GRAY)
    low = cv2.GaussianBlur(gray, (35, 35), 0)
    lowfreq_ratio = float(np.mean(np.abs(gray - low) < 3))

    return float(0.5 * skin_ratio + 0.5 * lowfreq_ratio)


def quality_check(
        img_bgr: np.ndarray,
        *,
        blur_threshold: float = 100.0,
        occlusion_threshold: float = 0.7,
) -> bool:
    """
    품질검사 (fast early-stop)
      1) 샤프니스 >= blur_threshold ?
      2) 렌즈가림(occlusion_score) < occlusion_threshold ?
    두 조건 모두 만족 시 True(통과)
    """
    if img_bgr is None or img_bgr.ndim != 3:
        return False

    # 1) 흐림 검사
    sharp_val = compute_sharpness_laplacian(img_bgr)
    if sharp_val < blur_threshold:
        return False

    # 2) 렌즈가림 검사
    occ_val = occlusion_score(img_bgr)
    if occ_val > occlusion_threshold:
        return False

    return True
