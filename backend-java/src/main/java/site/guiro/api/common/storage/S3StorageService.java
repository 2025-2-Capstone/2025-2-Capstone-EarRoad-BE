package site.guiro.api.common.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.config.S3Properties;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

// AWS S3 업로드 구현체

@Service
@RequiredArgsConstructor
public class S3StorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Properties props;

    @Override
    public UploadedImage upload(MultipartFile file, String keyPrefix, String keyHint) {
        String prefix = normalizePrefix(keyPrefix);
        String extension = resolveExtension(file.getOriginalFilename());
        String seed = StringUtils.hasText(keyHint) ? sanitize(keyHint) : "capture";
        String key = prefix + seed + "-" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            throw new IllegalStateException("S3 업로드에 실패했습니다", e);
        }

        return new UploadedImage(key, buildFileUrl(key));
    }

    /**
     * 업로드된 객체의 퍼블릭 URL을 생성한다.
     */
    public String buildFileUrl(String key) {
        String prefix = props.urlPrefix();
        if (!StringUtils.hasText(prefix)) {
            return key;
        }
        return prefix.endsWith("/") ? prefix + key : prefix + "/" + key;
    }

    private String normalizePrefix(String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    private String resolveExtension(String filename) {
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        return "";
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9-_]", "-");
    }
}