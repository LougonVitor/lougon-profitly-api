package tech.lougon.profitly.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserJpaEntity, String> {
    Optional<UserJpaEntity> findByEmail(String email);
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByGoogleId(String googleId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
