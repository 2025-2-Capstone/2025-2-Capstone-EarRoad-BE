package site.guiro.api.guide.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import site.guiro.api.guide.entity.Status;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuideResponse {
    private String poiKey;
    private Enum<Status> status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

}
