from fastapi import HTTPException, UploadFile, status
from loguru import logger

from app.gemini.prompts import short_visual_guide_prompt
from app.gemini.service import run_gemini_with_image
from app.models.dto import DemoScriptResponse
from app.services.pipeline import analyze_photo_pipeline, PipelineError


class DemoScriptService:
    async def generate_demo_script(self, name: str, content: str, image: UploadFile) -> DemoScriptResponse:
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

        try:
            analysis = analyze_photo_pipeline(
                file_bytes=image_bytes,
                poi_key=name or "demo",
                session_id="demo-session",
                max_side=1024,
                blur_threshold=1800.0,
                occlusion_threshold=0.7,
            )
        except PipelineError as exc:
            logger.warning("[DemoScript] 분석 실패: {}", exc)
            return DemoScriptResponse(
                script="사진 분석에 실패했습니다.",
                qualityPassed=False,
            )

        if not analysis.get("qualityPassed"):
            logger.info(
                "[DemoScript] 품질 미통과: qualityPassed=False filename={} size={} bytes",
                image.filename,
                len(image_bytes),
            )
            return DemoScriptResponse(
                script="사진 분석에 실패했습니다.",
                qualityPassed=False,
            )

        prompt = short_visual_guide_prompt(name, content)
        script = run_gemini_with_image(prompt, image_bytes, image.content_type)

        if not script:
            raise HTTPException(
                status_code=status.HTTP_502_BAD_GATEWAY,
                detail="Gemini에서 스크립트를 생성하지 못했습니다.",
            )

        return DemoScriptResponse(script=script, qualityPassed=True)