package site.guiro.api.poi.dto;


import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckRequest {

    @NotBlank
    private String poiKey;

    // WGS84 위도
    @NotNull
    @DecimalMin(value = "-90.0", inclusive = true)
    @DecimalMax(value = "90.0", inclusive = true)
    private Double currentLat;

    // WGS84 경도: -180 ~ 180 (필수)
    @NotNull
    @DecimalMin(value = "-180.0", inclusive = true)
    @DecimalMax(value = "180.0", inclusive = true)
    private Double currentLng;

}
