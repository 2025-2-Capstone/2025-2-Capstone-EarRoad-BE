package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.device.entity.Device;
import site.guiro.api.device.repository.DeviceRepository;
import site.guiro.api.guide.dto.AnalysisResponse;
import site.guiro.api.guide.service.ScriptService;

@RestController
@RequestMapping("/api/v1/script")
@RequiredArgsConstructor
public class ScriptController {

    private final ScriptService scriptService;
    private final DeviceRepository deviceRepository;

    @PostMapping(
            path = "/analysis",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AnalysisResponse> analyzeOne(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("image") MultipartFile image,
            @RequestPart("poiKey") String poiKey,
            @RequestPart("sessionId") String sessionId
    ) {
        Device device = resolveDevice(jwt);
        AnalysisResponse response = scriptService.analyzeAndStore(image, poiKey, sessionId, device);


        return ResponseEntity.ok(response);
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

    private Device resolveDevice(Jwt jwt) {
        String deviceUuid = jwt.getClaim("device_id");
        return deviceRepository.findByUuid(deviceUuid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "인증된 디바이스를 찾을 수 없습니다."
                ));
    }


}
