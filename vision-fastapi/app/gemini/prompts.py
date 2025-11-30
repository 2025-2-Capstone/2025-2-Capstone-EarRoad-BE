PROMPT_VISUAL_GUIDE_LONG = """
시각장애인을 위해 사진 속 장면에 대한 스크립트를 20문장으로 작성합니다.
장소는 "{place}"입니다.

장소의 개요는 다음과 같습니다:
{overview}

위 장소 개요를 참고하되,
시각장애인이 실제 장면을 머릿속에 떠올릴 수 있도록
시각적·공간적·감각적 요소 중심으로 묘사해주세요.

스크립트는 **20문장 정도의 길이**로 구성하며,
자연스럽고 부드러운 흐름을 가진 설명형 스토리텔링으로 작성해주세요.
문단을 적절히 나누고, 들었을 때 편안하게 이해할 수 있도록 구성해주세요.

# 출력 형식 제한 (매우 중요)
1. "네, 알겠습니다.", "스크립트를 작성해 드립니다.", "도움이 되셨나요?"와 같은 **서론 및 결론 멘트를 절대 포함하지 마세요.**
2. 제목이나 부가적인 설명 없이, **오직 스크립트 본문 텍스트만** 출력하세요.
3. 바로 첫 문장부터 묘사를 시작하세요.
"""
def long_visual_guide_prompt(place: str, overview: str) -> str:
    return PROMPT_VISUAL_GUIDE_LONG.format(
        place=place,
        overview=overview,
    )


PROMPT_VISUAL_GUIDE_SHORT = """
시각장애인을 위해 사진 속 장면에 대한 스크립트를 3문장으로 작성합니다.
장소는 "{place}"입니다.

장소의 개요는 다음과 같습니다:
{overview}

위 장소 개요를 참고하되,
직접적으로 역사 사실을 나열하는 대신
사진 속 장면을 시각적·공간적·분위기 중심으로 3문장으로 자연스럽게 묘사해주세요.

# 출력 형식 제한 (매우 중요)
1. "네, 알겠습니다.", "스크립트를 작성해 드립니다.", "도움이 되셨나요?"와 같은 **서론 및 결론 멘트를 절대 포함하지 마세요.**
2. 제목이나 부가적인 설명 없이, **오직 스크립트 본문 텍스트만** 출력하세요.
3. 바로 첫 문장부터 묘사를 시작하세요.
"""


def short_visual_guide_prompt(place: str, overview: str) -> str:
    return PROMPT_VISUAL_GUIDE_SHORT.format(
        place=place,
        overview=overview,
    )