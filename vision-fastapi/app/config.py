from typing import Optional, Dict

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    APP_HOST: str = "127.0.0.1"
    APP_PORT: int = 8000
    MAX_IMAGE_MB: int = 20
    SHARED_AUTH_TOKEN: str = "dev-shared-token"  # 스프링이 보내는 간단한 사전공유토큰

    MODEL_PATH: Optional[str] = "yolov8n.pt"  # or None / "" 로 두면 더미 모드
    YOLO_IMG_SIZE: int = 512                  # CPU 권장 기본값
    YOLO_CONF: float = 0.35
    YOLO_MAX_DET: int = 10
    YOLO_DEVICE: str = "cpu"

    SCORE_QUALITY_BONUS: float = 0.2
    SCORE_COLORFULNESS_MAX: float = 180.0
    SCORE_COLOR_WEIGHT: float = 0.5
    SCORE_WARM_TARGET: float = 0.48
    SCORE_WARM_TOLERANCE: float = 0.4
    SCORE_WARM_WEIGHT: float = 0.2
    SCORE_OBJECT_WEIGHT: float = 0.3
    SCORE_OBJECT_DEFAULT_WEIGHT: float = 0.12
    SCORE_OBJECT_LABEL_WEIGHTS: Dict[str, float] = {
        "person": 0.9,
        "bench": 0.55,
        "bird": 0.5,
        "flower": 0.4,
    }

    model_config = SettingsConfigDict(
        env_file=".env",     # fastapi-app 내부에 별도 .env 있으면 여기서 읽고
        extra="ignore",      # 그 외 환경변수(DB_URL 등)는 그냥 무시
    )


settings = Settings()
