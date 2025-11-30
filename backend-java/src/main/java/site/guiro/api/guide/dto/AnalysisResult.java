package site.guiro.api.guide.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectDetection {
        private String label;
        private double confidence;
    }

    private boolean qualityPassed;         // 품질 검사 통과 여부
    private double colorfulness;            // colorfulness_hasler()
    private double warmRatio;               // warm_ratio_lab()
    private String pHash;                   // 중복 검사용 perceptual hash
    private List<ObjectDetection> objects;   // YOLO 결과
    private double score;                   // FastAPI가 계산한 최종 스코어
}
