package site.guiro.api.guide.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResult {
    boolean qualityPassed;         // 품질 검사 통과 여부
    double colorfulness;            // colorfulness_hasler()
    double warmRatio;               // warm_ratio_lab()
    String pHash;                   // 중복 검사용 perceptual hash
    List<ObjectDetection> objects;   // YOLO 결과

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectDetection {
        String label;
        double confidence;
    }
}
