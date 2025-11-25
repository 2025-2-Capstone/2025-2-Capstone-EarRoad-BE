package site.guiro.api.poi.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiResponse {

    private String poiKey;
    private String name;
    private String content;
    private String address;
}
