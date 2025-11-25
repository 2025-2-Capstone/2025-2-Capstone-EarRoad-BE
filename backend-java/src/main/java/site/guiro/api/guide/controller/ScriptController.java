package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.guide.dto.AnalysisResponse;
import site.guiro.api.guide.dto.AnalysisResult;
import site.guiro.api.guide.service.ScriptService;

@RestController
@RequestMapping("/api/v1/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @PostMapping(
            path = "/analysis",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisResponse> analyzeOne(
            @RequestPart("image") MultipartFile image,
            @RequestPart("poiKey") String poiKey,
            @RequestPart("sessionId") String sessionId
    ) {
        AnalysisResult result = scriptService.analyzePhoto(image, poiKey, sessionId);

        // result를 통해 사진 데이터 저장


        // 품질 검사 결과 출력
        AnalysisResponse analysisresponse = AnalysisResponse.builder()
                .qualityPassed(result.isQualityPassed())
                .build();

        return ResponseEntity.ok(analysisresponse);
    }

    // 스크립트 생성 api 분리
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBestImageUrl(
            @RequestParam("poiKey") String poiKey,
            @RequestParam("sessionId") String sessionId
    ) {
        String url = ".."; // qualityPassed=true만 고려
        return ResponseEntity.ok(url);
    }


}
