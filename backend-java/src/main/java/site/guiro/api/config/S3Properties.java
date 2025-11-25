package site.guiro.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS S3 연동을 위한 설정 값.
 * - bucket: 업로드 대상 버킷명
 * - urlPrefix: 퍼블릭 URL prefix (https://<bucket>.s3.<region>.amazonaws.com/)
 */
@ConfigurationProperties(prefix = "guiro.s3")
public record S3Properties(
        String bucket,
        String urlPrefix
) { }
