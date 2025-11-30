import google.generativeai as genai
import os
from dotenv import load_dotenv

load_dotenv()
genai.configure(api_key=os.getenv("GEMINI_API_KEY"))

# 사용할 멀티모달 모델
MODEL_NAME = "models/gemini-2.5-flash-image"

model = genai.GenerativeModel(MODEL_NAME)


# 텍스트 전용 호출
def run_gemini(prompt: str) -> str:
    try:
        response = model.generate_content(prompt)
        return response.text
    except Exception as e:
        return f"[Gemini Error] {str(e)}"


# 이미지 + 텍스트(멀티모달)
def run_gemini_with_image(prompt: str, image_bytes: bytes, mime_type: str = "image/png"):
    try:
        response = model.generate_content([
            {"text": prompt},
            {
                "inline_data": {
                    "data": image_bytes,
                    "mime_type": mime_type
                }
            }
        ])
        return response.text
    except Exception as e:
        return f"[Gemini Error] {str(e)}"
