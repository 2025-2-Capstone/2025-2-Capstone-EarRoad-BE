from fastapi import HTTPException, UploadFile, status
from loguru import logger

from app.gemini.prompts import short_visual_guide_prompt
from app.gemini.service import run_gemini_with_image


class DemoScriptService:
    async def generate_demo_script(self, name: str, content: str, image: UploadFile) -> str:
        image_bytes = await image.read()
        if not image_bytes:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="이미지 파일이 비어 있습니다.",
            )

        logger.info(
            "[DemoScript] name={} content_len={} filename={} size={} bytes",
            name,
            len(content or ""),
            image.filename,
            len(image_bytes),
        )

        prompt = short_visual_guide_prompt(name, content)
        script = run_gemini_with_image(prompt, image_bytes, image.content_type)

        if not script:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Gemini에서 스크립트를 생성하지 못했습니다.",
            )

        return script