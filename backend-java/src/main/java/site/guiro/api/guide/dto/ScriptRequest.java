package site.guiro.api.guide.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScriptRequest {

    @NotBlank
    private String poiKey;

    private List<MultipartFile> images;

}
