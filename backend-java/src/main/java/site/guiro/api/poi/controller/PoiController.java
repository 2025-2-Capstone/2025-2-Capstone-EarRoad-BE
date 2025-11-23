package site.guiro.api.poi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import site.guiro.api.poi.dto.CheckRequest;
import site.guiro.api.poi.dto.CheckResponse;
import site.guiro.api.poi.dto.NearbyResponse;
import site.guiro.api.poi.dto.PoiResponse;
import site.guiro.api.poi.service.PoiService;

@Slf4j
@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
@Validated
public class PoiController {

    private final PoiService poiService;

    // PoiController - PoiService - TourApiClient 순으로 흘러가며,
    // 외부 TourAPI에서 가져온 데이터를 NearbyResponse DTO로 변환해 그대로 반환합니다.
    @GetMapping("/nearby")
    public NearbyResponse getNearbyPois(
            @RequestParam("lat") @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
            @RequestParam("lng") @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,
            @RequestParam("radius") @NotNull @Positive Double radiusMeters
    ) {
        log.info("[GET /api/v1/pois/nearby] request lat={}, lng={}, radius={}", latitude, longitude, radiusMeters);
        NearbyResponse response = poiService.getNearbyPois(latitude, longitude, radiusMeters);
        log.info("[GET /api/v1/pois/nearby] response size={} page={} total={}",
                response.getContent() == null ? 0 : response.getContent().size(),
                response.getPage(),
                response.getTotalElements());
        return response;
    }

    @GetMapping("/{poiKey}")
    public PoiResponse getPoiDetail(@PathVariable("poiKey") @NotBlank String poiKey) {
        log.info("[GET /api/v1/pois/{}] request", poiKey);
        PoiResponse response = poiService.getPoiDetail(poiKey);
        log.info("[GET /api/v1/pois/{}] response name={} imageUrl={}", poiKey, response.getName(), response.getImageUrl());
        return response;    }

    @PostMapping("/check")
    public CheckResponse checkPoiDistance(@RequestBody @Valid CheckRequest request) {
        log.info("[POST /api/v1/pois/check] request poiKey={} lat={} lng={}",
                request.getPoiKey(), request.getCurrentLat(), request.getCurrentLng());
        CheckResponse response = poiService.checkPoiDistance(request);
        log.info("[POST /api/v1/pois/check] response poiKey={} distance={}",
                response.getPoiKey(), response.getDistance());
        return response;
    }
}