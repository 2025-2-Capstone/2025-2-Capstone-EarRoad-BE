package site.guiro.api.device.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.device.entity.Device;

public interface DeviceRepository extends JpaRepository<Device, Long> {
}
