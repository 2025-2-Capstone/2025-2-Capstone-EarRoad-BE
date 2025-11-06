package site.guiro.api.poi.repository;

import org.springframework.data.repository.CrudRepository;
import site.guiro.api.poi.entity.PoiCache;

import java.util.Optional;

public interface PoiCacheRepository extends CrudRepository<PoiCache, Long> {
    Optional<PoiCache> findByPoiKey(String poiKey);

}
