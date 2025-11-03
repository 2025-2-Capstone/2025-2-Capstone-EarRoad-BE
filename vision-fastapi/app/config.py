from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    APP_HOST: str = "127.0.0.1"
    APP_PORT: int = 8000
    MAX_IMAGE_MB: int = 20
    SHARED_AUTH_TOKEN: str = "dev-shared-token"  # 스프링이 보내는 간단한 사전공유토큰

    class Config:
        env_file = ".env"


settings = Settings()
