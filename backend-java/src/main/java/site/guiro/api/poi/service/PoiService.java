package site.guiro.api.poi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.guiro.api.poi.client.TourApiClient;
import site.guiro.api.poi.dto.NearbyResponse;

@Service
@RequiredArgsConstructor
public class PoiService {

    private final TourApiClient tourApiClient;

    public NearbyResponse getNearbyPois(double latitude, double longitude, double radiusMeters) {
        return tourApiClient.fetchNearbyPois(latitude, longitude, radiusMeters);
    }
}