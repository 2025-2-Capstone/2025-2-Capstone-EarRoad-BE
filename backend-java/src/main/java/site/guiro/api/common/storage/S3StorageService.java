package site.guiro.api.common.storage;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import site.guiro.api.config.S3Properties;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

// AWS S3 업로드 구현체
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements ImageStorageService {

    private final S3Client s3Client;
    private final S3Properties props;
    @PostConstruct
    void logProps() {
        log.info("[S3Config] bucket={}, urlPrefix={}", props.bucket(), props.urlPrefix());
    }

    @Override
    public UploadedImage upload(MultipartFile file, String keyPrefix, String keyHint) {

        if (!StringUtils.hasText(props.bucket())) {
            throw new IllegalStateException("S3 bucket 이 설정되지 않았습니다. guiro.s3.bucket 값을 확인하세요.");
        }


        String prefix = normalizePrefix(keyPrefix);
        String extension = resolveExtension(file.getOriginalFilename());
        String seed = StringUtils.hasText(keyHint) ? sanitize(keyHint) : "capture";
        String key = prefix + seed + "-" + UUID.randomUUID() + extension;

        log.info("[S3 upload] bucket={}, key={}, size={}, contentType={}",
                props.bucket(), key, file.getSize(), file.getContentType());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(props.bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception e) {
            log.info("[S3 upload] failed: bucket={}, key={}", props.bucket(), key, e);
            throw new IllegalStateException("S3 업로드에 실패했습니다", e);
        }

        return new UploadedImage(key, buildFileUrl(key));
    }

    @Override
    public DownloadedImage download(String key) {
        if (!StringUtils.hasText(props.bucket())) {
            throw new IllegalStateException("S3 bucket 이 설정되지 않았습니다. guiro.s3.bucket 값을 확인하세요.");
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(props.bucket())
                .key(key)
                .build();

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            byte[] bytes = response.readAllBytes();
            String contentType = response.response().contentType();
            return new DownloadedImage(bytes, contentType, StringUtils.getFilename(key));
        } catch (IOException | S3Exception e) {
            log.info("[S3 download] failed: bucket={}, key={}", props.bucket(), key, e);
            throw new IllegalStateException("S3 다운로드에 실패했습니다", e);
        }
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