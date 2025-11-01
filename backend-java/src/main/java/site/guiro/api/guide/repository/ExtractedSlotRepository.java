package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.guide.entity.ExtractedSlot;

public interface ExtractedSlotRepository extends JpaRepository<ExtractedSlot, Long> {
}
