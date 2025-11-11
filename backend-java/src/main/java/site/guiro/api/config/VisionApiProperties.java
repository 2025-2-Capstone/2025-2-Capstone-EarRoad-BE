package site.guiro.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guiro.vision")
public record VisionApiProperties(
    String baseUrl,
    String token,
    int connectTimeoutMs,
    int readTimeoutMs
) {}
