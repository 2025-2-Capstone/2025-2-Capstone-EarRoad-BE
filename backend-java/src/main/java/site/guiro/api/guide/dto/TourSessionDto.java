package site.guiro.api.guide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.guiro.api.guide.entity.TourSession;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourSessionDto {
    private String status;
    private String poiKey;
    private Instant updatedAt;

    public static TourSessionDto from(TourSession tourSession) {
        return TourSessionDto.builder()
                .status(tourSession.getStatus().name())
                .poiKey(tourSession.getPoiKey().getPoiKey())
                .updatedAt(tourSession.getUpdatedAt())
                .build();
    }
}