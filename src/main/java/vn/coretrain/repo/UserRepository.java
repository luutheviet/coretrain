package vn.coretrain.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.coretrain.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
