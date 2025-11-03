package site.guiro.api.guide.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptRequest {

    @NotBlank
    private String poiKey;

    private MultipartFile image;

}
