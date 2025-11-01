package site.guiro.api.guide.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.guiro.api.guide.entity.Script;

public interface ScriptRepository extends JpaRepository<Script, Long> {
}
