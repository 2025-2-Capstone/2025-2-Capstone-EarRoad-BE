package site.guiro.api.guide.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    public ScriptResponse generateScript(ScriptRequest request) {
        MultipartFile image = request.getImage();
        if (image == null || image.isEmpty()) {
            // 실제에선 커스텀 예외 던지기
            return ScriptResponse.builder()
                    .scriptId("ERROR_NO_IMAGE")
                    .text("이미지가 없습니다.")
                    .build();
        }

        // 2) (예시) FastAPI 분석 → 대표 이미지 선택 (스텁)


        // 3) poiKey로 텍스트 조회 (스텁)
        String poiText = "POI(" + request.getPoiKey() + ") 기본 텍스트";

        // 4) Gemini 호출해 스크립트 생성 (스텁)
        String script = "[DEMO] " + poiText + " + 대표이미지(" + image.getOriginalFilename() + ") 기반 스크립트";

        // 5) ID 발급/저장 로직은 필요 시 추가
        return ScriptResponse.builder()
                .scriptId("SCR-0001")
                .text(script)
                .build();
    }


}
