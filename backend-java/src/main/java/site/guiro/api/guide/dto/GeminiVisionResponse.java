package site.guiro.api.guide.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiVisionResponse {

    private String mode;
    private String place;
    private String filename;

    @JsonProperty("result")
    private String script;
}