package site.guiro.api.guide.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptResponse {
    private String scriptId;
    private String text;
}
