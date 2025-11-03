from typing import Optional, List, Dict, Any
from fastapi import FastAPI, UploadFile, File, Form, Depends, HTTPException, status
from fastapi.responses import JSONResponse, PlainTextResponse
from loguru import logger
from .config import settings
from .deps import verify_shared_token


app = FastAPI(title="Guiro Vision API", version="0.2.0")


@app.get("/health", response_class=PlainTextResponse)
async def health():
    """서버 상태 확인용"""
    return "OK"


@app.post(
    "/analyze/photo",
    response_class=JSONResponse,
    dependencies=[Depends(verify_shared_token)],
)
async def analyze_photo(
        image: UploadFile = File(..., description="단일 이미지 파일"),
        poiKey: str = Form(...),
        sessionId: str = Form(...),
):
    """
    요청: 1장 이미지 + poiKey + sessionId
    응답: 분석 결과(JSON)
    """
    # 파일 크기 제한 체크
    content = await image.read()
    size_mb = len(content) / (1024 * 1024)
    if size_mb > settings.MAX_IMAGE_MB:
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Image larger than {settings.MAX_IMAGE_MB}MB",
        )

    # 로깅
    logger.info(f"[sessionId={sessionId}] poiKey={poiKey}, file={image.filename}, size={size_mb:.2f}MB")

    # TODO: 전처리 + 색채 분석 + YOLO 분석 (다음 단계에서 구현)
    # 지금은 더미 값
    dummy_score = 0.82
    dummy_colors = ["#d7b97d", "#4a3f28", "#c1a35e"]
    dummy_objects = [{"label": "building", "confidence": 0.93}, {"label": "tree", "confidence": 0.88}]

    # JSON 응답 생성
    result: Dict[str, Any] = {
        "poiKey": poiKey,
        "sessionId": sessionId,
        "score": dummy_score,
        "dominantColors": dummy_colors,
        "objects": dummy_objects,
        "sharpness": 0.67,
        "brightness": 0.54,
        "contrast": 0.48,
    }

    return JSONResponse(content=result, status_code=status.HTTP_200_OK)
