package site.guiro.api.common.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadedImage upload(MultipartFile file, String folder, String keyHint);

    @Getter @AllArgsConstructor
    class UploadedImage {
        private String key;
        private String url;
    }
}