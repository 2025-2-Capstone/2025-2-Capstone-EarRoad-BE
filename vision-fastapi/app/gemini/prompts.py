PROMPT_VISUAL_GUIDE_LONG = """
시각장애인을 위해 사진 속 장면에 대한 스크립트를 작성합니다.
장소는 "{place}"입니다.

장소의 개요는 다음과 같습니다:
{overview}

위 장소 개요를 참고하되,
시각장애인이 실제 장면을 머릿속에 떠올릴 수 있도록
시각적·공간적·감각적 요소 중심으로 묘사해주세요.

스크립트는 **3분 동안 읽을 수 있는 길이**로 구성하며,
자연스럽고 부드러운 흐름을 가진 설명형 스토리텔링으로 작성해주세요.
문단을 적절히 나누고, 들었을 때 편안하게 이해할 수 있도록 구성해주세요.
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
"""


def short_visual_guide_prompt(place: str, overview: str) -> str:
    return PROMPT_VISUAL_GUIDE_SHORT.format(
        place=place,
        overview=overview,
    )