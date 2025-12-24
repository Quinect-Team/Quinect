package com.project.quiz.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 로그인한다고 가정 (username으로 하려면 findByUsername)
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    List<User> findByEmailContaining(String email);
    
    List<User> findByStatusAndStatusChangedAtBefore(String status, LocalDateTime dateTime);

}