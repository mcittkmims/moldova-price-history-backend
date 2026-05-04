package md.pricehistory.backend.user.repository;

import java.util.Optional;
import md.pricehistory.backend.user.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
