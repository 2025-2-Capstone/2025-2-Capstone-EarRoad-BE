package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import site.guiro.api.device.entity.Device;
import site.guiro.api.device.repository.DeviceRepository;
import site.guiro.api.guide.dto.TourSessionDto;
import site.guiro.api.guide.service.TourSessionService;

@Slf4j
@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class GuideSessionController {

    private final TourSessionService tourSessionService;
    private final DeviceRepository deviceRepository;

    @PostMapping("/{poiKey}")
    public TourSessionDto setDestination(@AuthenticationPrincipal Jwt jwt, @PathVariable String poiKey) {
        Device device = resolveDevice(jwt);
        log.info("[POST /api/v1/guide/{}] request deviceUuid={}", poiKey, device.getUuid());
        TourSessionDto response = tourSessionService.setDestination(device, poiKey);
        log.info("[POST /api/v1/guide/{}] response status={} updatedAt={}", poiKey, response.getStatus(), response.getUpdatedAt());
        return response;
    }

    @PostMapping("/start")
    public TourSessionDto start(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        log.info("[POST /api/v1/guide/start] request deviceUuid={}", device.getUuid());
        TourSessionDto response = tourSessionService.start(device);
        log.info("[POST /api/v1/guide/start] response status={} poiKey={} updatedAt={}",
                response.getStatus(), response.getPoiKey(), response.getUpdatedAt());
        return response;
    }

    @PostMapping("/pause")
    public TourSessionDto pause(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        log.info("[POST /api/v1/guide/pause] request deviceUuid={}", device.getUuid());
        TourSessionDto response = tourSessionService.pause(device);
        log.info("[POST /api/v1/guide/pause] response status={} updatedAt={}", response.getStatus(), response.getUpdatedAt());
        return response;
    }

    @PostMapping("/resume")
    public TourSessionDto resume(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        log.info("[POST /api/v1/guide/resume] request deviceUuid={}", device.getUuid());
        TourSessionDto response = tourSessionService.resume(device);
        log.info("[POST /api/v1/guide/resume] response status={} updatedAt={}", response.getStatus(), response.getUpdatedAt());
        return response;
    }

    @PostMapping("/end")
    public TourSessionDto end(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        log.info("[POST /api/v1/guide/end] request deviceUuid={}", device.getUuid());
        TourSessionDto response = tourSessionService.end(device);
        log.info("[POST /api/v1/guide/end] response status={} updatedAt={}", response.getStatus(), response.getUpdatedAt());
        return response;
    }

    private Device resolveDevice(Jwt jwt) {
        String deviceUuid = jwt.getClaim("device_id");
        return deviceRepository.findByUuid(deviceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device not found"));
    }
}
