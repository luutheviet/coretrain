package vn.coretrain.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.coretrain.domain.Module;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findAllByOrderBySortOrderAsc();

    Optional<Module> findByCode(String code);

    boolean existsByCode(String code);
}
