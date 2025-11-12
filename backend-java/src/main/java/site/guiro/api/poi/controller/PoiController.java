package site.guiro.api.poi.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import site.guiro.api.poi.dto.CheckRequest;
import site.guiro.api.poi.dto.CheckResponse;
import site.guiro.api.poi.dto.NearbyResponse;
import site.guiro.api.poi.dto.PoiResponse;
import site.guiro.api.poi.service.PoiService;


@RestController
@RequestMapping("/api/v1/pois")
@RequiredArgsConstructor
@Validated
public class PoiController {

    private final PoiService poiService;

    // PoiController → PoiService → TourApiClient 순으로 흘러가며,
    // 외부 TourAPI에서 가져온 데이터를 NearbyResponse DTO로 변환해 그대로 반환합니다.
    @GetMapping("/nearby")
    public NearbyResponse getNearbyPois(
            @RequestParam("lat") @NotNull @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") Double latitude,
            @RequestParam("lng") @NotNull @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") Double longitude,
            @RequestParam("radius") @NotNull @Positive Double radiusMeters
    ) {
        return poiService.getNearbyPois(latitude, longitude, radiusMeters);
    }

    @GetMapping("/{poiKey}")
    public PoiResponse getPoiDetail(@PathVariable("poiKey") @NotBlank String poiKey) {
        return poiService.getPoiDetail(poiKey);
    }

    @PostMapping("/check")
    public CheckResponse checkPoiDistance(@RequestBody @Valid CheckRequest request) {
        return poiService.checkPoiDistance(request);
    }
}