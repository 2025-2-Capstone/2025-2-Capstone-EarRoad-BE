package site.guiro.api.poi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "guiro.tour")
public record TourApiProperties(
        String baseUrl,
        String serviceKey
) {
}
