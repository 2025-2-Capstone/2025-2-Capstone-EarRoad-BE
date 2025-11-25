package site.guiro.api.guide.controller;

import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/v1/guide")
@RequiredArgsConstructor
public class GuideSessionController {

    private final TourSessionService tourSessionService;
    private final DeviceRepository deviceRepository;

    @PostMapping("/{poiKey}")
    public TourSessionDto setDestination(@AuthenticationPrincipal Jwt jwt, @PathVariable String poiKey) {
        Device device = resolveDevice(jwt);
        return tourSessionService.setDestination(device, poiKey);
    }

    @PostMapping("/start")
    public TourSessionDto start(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        return tourSessionService.start(device);
    }

    @PostMapping("/pause")
    public TourSessionDto pause(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        return tourSessionService.pause(device);
    }

    @PostMapping("/resume")
    public TourSessionDto resume(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        return tourSessionService.resume(device);
    }

    @PostMapping("/end")
    public TourSessionDto end(@AuthenticationPrincipal Jwt jwt) {
        Device device = resolveDevice(jwt);
        return tourSessionService.end(device);
    }

    private Device resolveDevice(Jwt jwt) {
        String deviceUuid = jwt.getClaim("device_id");
        return deviceRepository.findByUuid(deviceUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Device not found"));
    }
}
