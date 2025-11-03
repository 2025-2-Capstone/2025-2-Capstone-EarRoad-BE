from typing import Optional
from fastapi import Header, HTTPException, status
from .config import settings


async def verify_shared_token(x_auth_token: Optional[str] = Header(default=None)):
    """
    내부 통신이라도 간단한 위조 방지용 토큰 검사.
    운영에선 systemd Environment로 주입해 사용.
    """
    expected = settings.SHARED_AUTH_TOKEN
    if expected and x_auth_token != expected:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid X-Auth-Token",
        )
