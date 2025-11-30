from functools import lru_cache
from typing import Optional

import google.generativeai as genai
from fastapi import HTTPException, status
from google.api_core.exceptions import GoogleAPICallError
from loguru import logger

from app.config import settings


class GeminiModelUnavailable(Exception):
    pass


@lru_cache(maxsize=1)
def _get_model():
    api_key = settings.GEMINI_API_KEY
    if not api_key:
        raise GeminiModelUnavailable("GEMINI_API_KEY is not configured")

    genai.configure(api_key=api_key)
    logger.info("[Gemini] Using model {}", settings.GEMINI_MODEL)
    return genai.GenerativeModel(settings.GEMINI_MODEL)


def run_gemini(prompt: str) -> str:
    try:
        model = _get_model()
        response = model.generate_content(prompt)
        return response.text or ""
    except GeminiModelUnavailable as exc:
        logger.error("[Gemini] configuration error: {}", exc)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)) from exc
    except GoogleAPICallError as exc:
        logger.exception("[Gemini] API call failed")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail="Gemini API call failed") from exc
    except Exception as exc:  # noqa: BLE001
        logger.exception("[Gemini] Unexpected error")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Gemini generation failed") from exc


def run_gemini_with_image(prompt: str, image_bytes: bytes, mime_type: Optional[str] = "image/png") -> str:
    try:
        model = _get_model()
        response = model.generate_content([
            {"text": prompt},
            {
                "inline_data": {
                    "data": image_bytes,
                    "mime_type": mime_type or "application/octet-stream",
                }
            },
        ])
        return response.text or ""
    except GeminiModelUnavailable as exc:
        logger.error("[Gemini] configuration error: {}", exc)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)) from exc
    except GoogleAPICallError as exc:
        logger.exception("[Gemini] API call failed")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail="Gemini API call failed") from exc
    except Exception as exc:  # noqa: BLE001
        logger.exception("[Gemini] Unexpected error")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Gemini generation failed") from exc