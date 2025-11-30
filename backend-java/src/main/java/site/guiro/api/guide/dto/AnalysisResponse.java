package site.guiro.api.guide.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResponse {
    boolean qualityPassed;
    String imageKey;
    String imageUrl;
    Double score;
}
