package site.guiro.api.poi.dto;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyResponse {

    private List<PoiSummaryItem> content;
    private int page;
    private int size;
    private int totalElements;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PoiSummaryItem{
        private String poiKey;
        private String name;
        private double distance;
    }
}
