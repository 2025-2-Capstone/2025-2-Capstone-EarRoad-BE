package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.guide.entity.TourSession;

public interface TourSessionRepository extends JpaRepository<TourSession, Long> {
}
