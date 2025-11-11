package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.guide.entity.CaptureImage;

import java.util.List;
import java.util.Optional;

public interface CaptureImageRepository extends JpaRepository<CaptureImage, Long> {
    List<CaptureImage> findByPoiKey_PoiKeyAndSessionIdAndQualityPassedTrue(String poiKey, String sessionId);

    Optional<CaptureImage> findTopByPoiKey_PoiKeyAndSessionIdAndQualityPassedTrueOrderByCreatedAtDesc(
            String poiKey, String sessionId
    );
}
