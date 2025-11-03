package site.guiro.api.guide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.guide.dto.AnalysisResult;
import site.guiro.api.guide.dto.ScriptRequest;
import site.guiro.api.guide.dto.ScriptResponse;

/**
 * - 이미지 분석(FastAPI/Python) : 여러 장 중 품질/콘텐츠 기준으로 대표 이미지 1장 선택
 * - 텍스트 컨텍스트 준비(예: poiKey로 POI 콘텐츠 조회)
 * - Gemini API 호출로 최종 스크립트 생성
 * - 스크립트 ID 발급/저장(선택) 후 응답 반환
 */
@Service
@RequiredArgsConstructor
public class ScriptService {

    private final VisionAnalyzeClient visionAnalyzeClient;

    public AnalysisResult analyzePhoto(MultipartFile image, String poiKey, String sessionId) {
        // FastAPI에 실제 분석 요청 → 품질검사 실패 시에도 JSON 형식은 동일
        return visionAnalyzeClient.analyzePhoto(image, poiKey, sessionId);
    }


}
