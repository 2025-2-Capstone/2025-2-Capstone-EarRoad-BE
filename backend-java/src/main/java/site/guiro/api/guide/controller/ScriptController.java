package site.guiro.api.guide.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import site.guiro.api.guide.dto.ScriptRequest;
import site.guiro.api.guide.dto.ScriptResponse;
import site.guiro.api.guide.service.ScriptService;

@RestController
@RequestMapping("/api/v1/script")
@RequiredArgsConstructor
@Validated
public class ScriptController {

    private final ScriptService scriptService;

    /**
     * 사진 여러 장을 받아(Python 분석 → 대표 1장 선택),
     * poiKey에 맞는 텍스트와 함께 Gemini API로 스크립트를 생성합니다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScriptResponse> createScript(@Valid @ModelAttribute ScriptRequest request) {
        ScriptResponse result = scriptService.generateScript(request);
        return ResponseEntity.ok(result);
    }
}
