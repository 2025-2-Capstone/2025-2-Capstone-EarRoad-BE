package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.guide.entity.CaptureImage;

public interface CaptureImageRepository extends JpaRepository<CaptureImage, Long> {
}
