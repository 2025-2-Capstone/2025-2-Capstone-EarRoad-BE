package site.guiro.api.guide.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.guide.dto.AnalysisResult;
import site.guiro.api.guide.dto.ScriptResponse;
import site.guiro.api.guide.service.ScriptService;

@RestController
@RequestMapping("/api/v1/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ScriptResponse> analyzeOne(
            @RequestPart("image") MultipartFile image,
            @RequestPart("poiKey") String poiKey,
            @RequestPart("sessionId") String sessionId
    ) {
        AnalysisResult result = scriptService.analyzePhoto(image, poiKey, sessionId);

        // result를 통해 사진 데이터 저장
        ScriptResponse scriptresponse = new ScriptResponse("", "");

        // 스크립트 생성
        return ResponseEntity.ok(scriptresponse);
    }


}
