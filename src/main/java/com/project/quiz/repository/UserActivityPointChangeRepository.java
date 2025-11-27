package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityPointChange;

public interface UserActivityPointChangeRepository extends JpaRepository<UserActivityPointChange, Long> {
    
    @Query("SELECT p FROM UserActivityPointChange p " +
           "JOIN p.userActivityLog l " +
           "WHERE l.user = :user " +
           "ORDER BY l.createdAt DESC")
    List<UserActivityPointChange> findAllByUserDesc(@Param("user") User user);
}