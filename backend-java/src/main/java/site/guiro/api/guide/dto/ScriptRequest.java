package site.guiro.api.guide.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptRequest {

    private MultipartFile image;

    @NotBlank
    private String poiKey;

    @NotBlank
    private int sessionId;

}
