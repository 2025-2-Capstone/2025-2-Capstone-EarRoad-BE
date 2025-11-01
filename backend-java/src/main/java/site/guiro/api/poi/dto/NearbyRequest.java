package site.guiro.api.poi.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyRequest {

    @NotNull
    @DecimalMin(value = "-90.0", inclusive = true)
    @DecimalMax(value = "90.0", inclusive = true)
    private Double lat;

    // WGS84 경도: -180 ~ 180 (필수)
    @NotNull
    @DecimalMin(value = "-180.0", inclusive = true)
    @DecimalMax(value = "180.0", inclusive = true)
    private Double lng;

    // 반경(m): 기본값 1000, 100 ~ 5000
    @Min(100) @Max(5000)
    @Builder.Default
    private Integer radius = 1000;

    // 페이지: 기본값 0, 최소 0
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    // 페이지 크기: 기본값 20, 1 ~ 100
    @Min(1) @Max(100)
    @Builder.Default
    private Integer size = 20;
}
