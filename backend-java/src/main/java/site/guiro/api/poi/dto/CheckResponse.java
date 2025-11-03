package site.guiro.api.poi.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckResponse {

    private String poiKey;
    private Double distance;
}
