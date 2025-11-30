package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        log.info("[POST /api/v1/script/analysis] request deviceUuid={} poiKey={} sessionId={} imageName={}",
                device.getUuid(), poiKey, sessionId, image.getOriginalFilename());
        AnalysisResponse response = scriptService.analyzeAndStore(image, poiKey, sessionId, device);

        log.info("[POST /api/v1/script/analysis] response qualityPassed={} score={} imageKey={} imageUrl={}",
                response.isQualityPassed(), response.getScore(), response.getImageKey(), response.getImageUrl());
        return ResponseEntity.ok(response);
    }

    // 스크립트 생성 api 분리
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBestImageUrl(
            @RequestParam("poiKey") String poiKey,
            @RequestParam("sessionId") String sessionId
    ) {
        log.info("[GET /api/v1/script] request poiKey={} sessionId={}", poiKey, sessionId);
        String url = ".."; // qualityPassed=true만 고려
        log.info("[GET /api/v1/script] response url={}", url);
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
