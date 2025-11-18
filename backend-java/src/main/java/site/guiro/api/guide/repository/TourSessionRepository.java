package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.device.entity.Device;
import site.guiro.api.guide.entity.Status;
import site.guiro.api.guide.entity.TourSession;

import java.util.Optional;

public interface TourSessionRepository extends JpaRepository<TourSession, Long> {

    Optional<TourSession> findTopByDeviceAndStatusNotOrderByStartedAtDesc(Device device, Status status);
}
