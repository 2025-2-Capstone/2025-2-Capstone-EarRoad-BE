package site.guiro.api.device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.device.entity.Device;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByUuid(String uuid);

}
