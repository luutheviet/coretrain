package vn.coretrain.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.coretrain.domain.Section;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByModuleIdOrderBySortOrderAsc(Long moduleId);

    boolean existsByCode(String code);

    Optional<Section> findByCode(String code);
}
