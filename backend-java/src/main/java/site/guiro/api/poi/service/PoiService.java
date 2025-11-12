package site.guiro.api.poi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.guiro.api.poi.client.TourApiClient;
import site.guiro.api.poi.config.TourApiProperties;
import site.guiro.api.poi.dto.CheckRequest;
import site.guiro.api.poi.dto.CheckResponse;
import site.guiro.api.poi.dto.NearbyResponse;
import site.guiro.api.poi.dto.PoiResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PoiService {

    private final TourApiClient tourApiClient;
    private final TourApiProperties tourApiProperties;

    public NearbyResponse getNearbyPois(double latitude, double longitude, double radiusMeters) {
        return tourApiClient.fetchNearbyPois(latitude, longitude, radiusMeters);
    }

    public PoiResponse getPoiDetail(String poiKey) {
        return tourApiClient.fetchPoiDetail(poiKey);
    }

    public CheckResponse checkPoiDistance(CheckRequest request) {
        double radiusMeters = tourApiProperties.checkRadiusMeters();
        NearbyResponse response = tourApiClient.fetchNearbyPois(
                request.getCurrentLat(),
                request.getCurrentLng(),
                radiusMeters
        );

        List<NearbyResponse.PoiSummaryItem> items = response.getContent();
        double distance = items == null ? 0.0 : items.stream()
                .filter(item -> request.getPoiKey().equals(item.getPoiKey()))
                .map(NearbyResponse.PoiSummaryItem::getDistance)
                .findFirst()
                .orElse(0.0);

        return CheckResponse.builder()
                .poiKey(request.getPoiKey())
                .distance(distance)
                .build();
    }
}