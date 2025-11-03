from typing import List, Optional
from pydantic import BaseModel, Field


class ObjectDetection(BaseModel):
    """YOLO 객체 탐지 결과"""
    label: str = Field(..., description="탐지된 객체 라벨명")
    confidence: float = Field(..., ge=0.0, le=1.0, description="신뢰도 (0~1 사이)")


class AnalysisResult(BaseModel):
    """
    Spring과 동일한 응답 구조
    - 품질검사 불통과 시 colorfulness/warmRatio=0, objects=[]
    """
    qualityPassed: bool = Field(..., description="품질검사 통과 여부")
    colorfulness: float = Field(..., ge=0.0, description="색채 풍부도")
    warmRatio: float = Field(..., ge=0.0, le=1.0, description="따뜻한 톤 비율 (0~1)")
    pHash: str = Field(..., description="Perceptual hash (중복 검사용)")
    objects: List[ObjectDetection] = Field(default_factory=list, description="YOLO 탐지 결과 리스트")


class AnalyzeRequest(BaseModel):
    """
    (옵션) 요청 데이터 구조 — 필요 시 내부 테스트용
    실제 FastAPI는 multipart/form-data로 받음.
    """
    poiKey: str
    sessionId: str
