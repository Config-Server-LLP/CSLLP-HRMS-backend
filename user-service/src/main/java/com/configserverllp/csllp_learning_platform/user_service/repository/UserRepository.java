package com.configserverllp.csllp_learning_platform.user_service.repository;

import com.configserverllp.csllp_learning_platform.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    List<User> findByManagerId(Long managerId);

    List<User> findByRole(String role);
}