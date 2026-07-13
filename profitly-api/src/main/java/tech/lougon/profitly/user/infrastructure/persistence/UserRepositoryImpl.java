package tech.lougon.profitly.user.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import tech.lougon.profitly.user.domain.model.User;
import tech.lougon.profitly.user.domain.repository.UserRepository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpa;

    public UserRepositoryImpl(JpaUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public User save(User user) {
        return toDomain(jpa.save(toEntity(user)));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(this::toDomain);
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        return jpa.findByGoogleId(googleId).map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpa.existsByUsername(username);
    }

    private UserJpaEntity toEntity(User u) {
        UserJpaEntity e = new UserJpaEntity();
        e.setId(u.id());
        e.setUsername(u.username());
        e.setEmail(u.email());
        e.setGoogleId(u.googleId());
        e.setCreatedAt(u.createdAt());
        return e;
    }

    private User toDomain(UserJpaEntity e) {
        return new User(
                e.getId(), e.getUsername(), e.getEmail(), e.getGoogleId(),
                e.getCreatedAt()
        );
    }
}
