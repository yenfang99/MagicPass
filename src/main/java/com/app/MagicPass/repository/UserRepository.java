package com.app.MagicPass.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.app.MagicPass.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
