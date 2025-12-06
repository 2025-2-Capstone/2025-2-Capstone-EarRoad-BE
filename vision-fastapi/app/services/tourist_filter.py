from __future__ import annotations

import threading
from dataclasses import dataclass
from typing import Optional, Dict

import numpy as np
from loguru import logger

try:
    import torch
    from torch import nn
    _TORCH_AVAILABLE = True
except Exception:
    torch = None  # type: ignore
    nn = None  # type: ignore
    _TORCH_AVAILABLE = False

try:
    from ultralytics import YOLO  # type: ignore
    _ULTRALYTICS_AVAILABLE = True
except Exception:
    YOLO = None  # type: ignore
    _ULTRALYTICS_AVAILABLE = False


@dataclass
class TouristFilterResult:
    # tourist = 관광지 (가이드 필요), non_tourist = 화장실/주차장 (필터 대상)
    is_tourist: bool                # True면 tourist, False면 non_tourist
    confidence: float               # 예측 라벨에 대한 확률
    raw_probs: Optional[Dict[str, float]]  # {"non_tourist": p0, "tourist": p1}


class _MLPHead(nn.Module):
    """
    학습 시 사용한 MLPHead와 동일한 구조:
    - 입력: (B, C, H, W) feature map (YOLOv8s backbone의 상단 feature)
    - GAP → Flatten → Linear( C → 256 ) → BN → ReLU → Dropout
            → Linear( 256 → 64 ) → BN → ReLU → Dropout
            → Linear( 64 → 1 )  # binary logit
    """
    def __init__(self, in_channels: int, dropout_p: float = 0.5):
        super().__init__()
        self.gap = nn.AdaptiveAvgPool2d((1, 1))
        self.net = nn.Sequential(
            nn.Flatten(),              # (B, C, 1, 1) -> (B, C)
            nn.Linear(in_channels, 256),
            nn.BatchNorm1d(256),
            nn.ReLU(),
            nn.Dropout(dropout_p),
            nn.Linear(256, 64),
            nn.BatchNorm1d(64),
            nn.ReLU(),
            nn.Dropout(dropout_p),
            nn.Linear(64, 1),          # binary logit
        )

    def forward(self, feat: torch.Tensor) -> torch.Tensor:
        x = self.gap(feat)   # (B, C, 1, 1)
        x = self.net(x)      # (B, 1)
        return x


class _TouristFilter:
    """
    새 체크포인트(mlp_v2_yolov8s.pt)를 사용하는 Tourist 필터.

    - backbone_path: YOLOv8s 아키텍처 정의 파일 (예: "yolov8s.pt")
    - mlp_path: 학습된 체크포인트 (예: "mlp_v2_yolov8s.pt")
      -> 내부에 backbone_state_dict, mlp_state_dict, class_to_idx, in_channels,
         non_tourist_idx, target_layer_idx 포함 (없으면 에러)
    """
    def __init__(self, *, backbone_path: str, mlp_path: str, device: str):
        if not _TORCH_AVAILABLE:
            raise RuntimeError("torch 미설치로 Tourist 필터를 초기화할 수 없습니다")
        if not _ULTRALYTICS_AVAILABLE:
            raise RuntimeError("ultralytics 미설치로 Tourist 필터를 초기화할 수 없습니다")

        # 서버는 CPU-only 이므로, config에서 device="cpu" 로 넘겨주면 됨
        self.device = torch.device(device)

        # 1) YOLOv8s 아키텍처 로드
        yolo_wrapper = YOLO(backbone_path)
        self.model: nn.Module = yolo_wrapper.model  # type: ignore[assignment]

        # 2) 체크포인트 로드 (backbone + mlp 정보 포함)
        ckpt = torch.load(mlp_path, map_location="cpu")

        # 2-1) 필수 키 확인 및 로드 (없으면 바로 에러)
        if "backbone_state_dict" not in ckpt:
            raise RuntimeError("체크포인트에 'backbone_state_dict'가 없습니다. "
                               "새 학습 스크립트로 생성된 파일인지 확인해주세요.")
        if "mlp_state_dict" not in ckpt:
            raise RuntimeError("체크포인트에 'mlp_state_dict'가 없습니다. "
                               "새 학습 스크립트로 생성된 파일인지 확인해주세요.")
        if "in_channels" not in ckpt:
            raise RuntimeError("체크포인트에 'in_channels'가 없습니다. "
                               "새 학습 스크립트로 생성된 파일인지 확인해주세요.")
        if "target_layer_idx" not in ckpt:
            raise RuntimeError("체크포인트에 'target_layer_idx'가 없습니다. "
                               "새 학습 스크립트로 생성된 파일인지 확인해주세요.")

        # YOLO 아키텍처에 finetune weight 적용
        self.model.load_state_dict(ckpt["backbone_state_dict"])
        self.model.to(self.device)
        self.model.eval()
        for p in self.model.parameters():
            p.requires_grad = False

        # 3) Detect 바로 직전 레이어에 hook 등록 (학습 시와 동일 index)
        self._feat: Optional[torch.Tensor] = None

        modules = self.model.model  # type: ignore[assignment]
        target_layer_idx = int(ckpt["target_layer_idx"])
        if target_layer_idx < 0:
            target_layer_idx = len(modules) + target_layer_idx

        if not (0 <= target_layer_idx < len(modules)):
            raise RuntimeError(f"target_layer_idx={target_layer_idx} 가 유효하지 않습니다")

        target_layer: nn.Module = modules[target_layer_idx]  # type: ignore[index]

        def _hook(_module: nn.Module, _inp, out: torch.Tensor):
            # forward마다 마지막 feature를 저장
            self._feat = out

        target_layer.register_forward_hook(_hook)

        # 4) 라벨/채널 메타데이터
        self.class_to_idx: Dict[str, int] = ckpt.get(
            "class_to_idx",
            {"non_tourist": 0, "tourist": 1},
        )
        self.idx_to_class: Dict[int, str] = {v: k for k, v in self.class_to_idx.items()}
        self.non_tourist_idx: int = int(ckpt.get("non_tourist_idx", 0))

        in_channels: int = int(ckpt["in_channels"])

        # 5) MLPHead 생성 + 파라미터 로드
        self.mlp = _MLPHead(in_channels=in_channels)
        self.mlp.load_state_dict(ckpt["mlp_state_dict"])
        self.mlp.to(self.device)
        self.mlp.eval()

        logger.info(
            "Tourist 필터 로드 완료 (backbone={}, mlp={}, in_channels={}, classes={})",
            backbone_path,
            mlp_path,
            in_channels,
            self.class_to_idx,
        )

    def _preprocess(self, img_bgr: np.ndarray) -> torch.Tensor:
        """
        BGR(OpenCV) 이미지를 학습 시 validation과 동일하게 전처리:
        - BGR -> RGB
        - Resize(640, 640)
        - CenterCrop(640)
        - ToTensor()
        - Normalize 없음 (0~1 범위 유지, YOLOv8 입력 분포와 일치)
        """
        from PIL import Image
        import torchvision.transforms as T

        img_rgb = img_bgr[..., ::-1]          # BGR -> RGB (채널 뒤집기)
        pil_img = Image.fromarray(img_rgb.astype(np.uint8))

        transform = T.Compose([
            T.Resize((640, 640)),
            T.CenterCrop(640),
            T.ToTensor(),
        ])

        tensor = transform(pil_img).unsqueeze(0)  # (1, 3, H, W)
        return tensor.to(self.device)

    @torch.no_grad()
    def predict(self, img_bgr: np.ndarray) -> TouristFilterResult:
        """
        - single logit + sigmoid → tourist(1) 확률로 해석
        - class_to_idx: {'non_tourist': 0, 'tourist': 1} 기준
        """
        x = self._preprocess(img_bgr)  # (1, 3, 640, 640)

        # YOLO backbone forward (hook로 feature 채움)
        self._feat = None
        _ = self.model(x)
        feat = self._feat
        if feat is None:
            raise RuntimeError("YOLO backbone에서 feature를 얻지 못했습니다")

        if isinstance(feat, (list, tuple)):
            feat = feat[-1]

        logits = self.mlp(feat)             # (1, 1)
        prob_tourist = float(torch.sigmoid(logits).item())

        # binary: p1 = tourist, p0 = non_tourist
        prob_non_tourist = 1.0 - prob_tourist

        # class_to_idx 기준으로 raw_probs dict 구성
        raw_probs: Dict[str, float] = {}
        for name, idx in self.class_to_idx.items():
            if name == "tourist":
                raw_probs[name] = prob_tourist
            elif name == "non_tourist":
                raw_probs[name] = prob_non_tourist
            else:
                raw_probs[name] = 0.0

        # 기본 threshold = 0.5 (추후 운영하면서 조정 가능)
        is_tourist = prob_tourist >= 0.5
        confidence = max(prob_tourist, prob_non_tourist)

        return TouristFilterResult(
            is_tourist=is_tourist,
            confidence=confidence,
            raw_probs=raw_probs,
        )


_CACHED_FILTER: Optional[_TouristFilter] = None
_FILTER_LOCK = threading.Lock()


def get_filter(*, backbone_path: Optional[str], mlp_path: Optional[str], device: str) -> Optional[_TouristFilter]:
    """
    Lazy singleton loader for _TouristFilter.
    설정 누락 또는 로드 실패 시 None을 반환하여 필터를 비활성화한다.
    """
    global _CACHED_FILTER
    if _CACHED_FILTER is not None:
        return _CACHED_FILTER

    if not backbone_path or not mlp_path:
        logger.warning("Tourist 필터 경로가 설정되지 않아 비활성화됩니다")
        return None

    with _FILTER_LOCK:
        if _CACHED_FILTER is not None:
            return _CACHED_FILTER
        try:
            _CACHED_FILTER = _TouristFilter(
                backbone_path=backbone_path,
                mlp_path=mlp_path,
                device=device,
            )
        except Exception as e:
            logger.warning(f"Tourist 필터 로드 실패: {e} (필터 비활성화)")
            _CACHED_FILTER = None
    return _CACHED_FILTER


def classify_tourist(
        img_bgr: np.ndarray,
        *,
        backbone_path: Optional[str],
        mlp_path: Optional[str],
        device: str,
) -> Optional[TouristFilterResult]:
    """
    관광지/비관광지 분류. 로드 실패나 설정 미지정 시 None 반환으로 필터링을 건너뛴다.
    """
    clf = get_filter(backbone_path=backbone_path, mlp_path=mlp_path, device=device)
    if clf is None:
        return None
    try:
        return clf.predict(img_bgr)
    except Exception as e:  # pragma: no cover - 런타임 예외 방지용
        logger.warning(f"Tourist 필터 예외 발생: {e} (필터 건너뜀)")
        return None
