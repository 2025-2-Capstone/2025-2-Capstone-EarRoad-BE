from typing import Optional
from pydantic_settings import BaseSettings


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


    class Config:
        env_file = ".env"


settings = Settings()
