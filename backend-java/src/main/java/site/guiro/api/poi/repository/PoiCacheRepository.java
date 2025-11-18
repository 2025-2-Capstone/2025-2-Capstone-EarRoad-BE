package site.guiro.api.poi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.poi.entity.PoiCache;

import java.util.Optional;

public interface PoiCacheRepository extends JpaRepository<PoiCache, Long> {
    Optional<PoiCache> findByPoiKey(String poiKey);

}
