from functools import lru_cache
from typing import Optional

import google.generativeai as genai
from fastapi import HTTPException, status
from google.api_core.exceptions import GoogleAPICallError
from loguru import logger

from app.config import settings


class GeminiModelUnavailable(Exception):
    pass

def _extract_text_from_response(response) -> str:
    """
    response.text 대신 직접 candidates -> content.parts 에서 text만 뽑아오는 헬퍼.
    이미지 inline_data 때문에 response.text 내부 구현이 터지는 문제를 우회한다.
    """
    texts = []
    try:
        for cand in getattr(response, "candidates", []) or []:
            content = getattr(cand, "content", None)
            if not content:
                continue
            for part in getattr(content, "parts", []) or []:
                t = getattr(part, "text", None)
                if t:
                    texts.append(t)
    except Exception as e:
        logger.warning(f"[Gemini] 응답 파싱 중 오류: {e}")

    return "\n".join(texts).strip()

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
        text = _extract_text_from_response(response)
        if not text:
            logger.warning("[Gemini] 응답에서 텍스트를 찾지 못했습니다.")
        return text

    except GeminiModelUnavailable as exc:
        logger.error("[Gemini] configuration error: {}", exc)
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=str(exc)) from exc
    except GoogleAPICallError as exc:
        logger.exception("[Gemini] API call failed")
        raise HTTPException(status_code=status.HTTP_502_BAD_GATEWAY, detail="Gemini API call failed") from exc
    except Exception as exc:  # noqa: BLE001
        logger.exception("[Gemini] Unexpected error")
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Gemini generation failed") from exc