package com.project.quiz.repository;

import com.project.quiz.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 이메일로 로그인한다고 가정 (username으로 하려면 findByUsername)
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
}