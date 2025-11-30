package site.guiro.api.common.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    UploadedImage upload(MultipartFile file, String folder, String keyHint);
    DownloadedImage download(String key);

    @Getter @AllArgsConstructor
    class UploadedImage {
        private String key;
        private String url;
    }

    @Getter @AllArgsConstructor
    class DownloadedImage {
        private byte[] bytes;
        private String contentType;
        private String filename;
    }

}