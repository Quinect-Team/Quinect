package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.BoardPost;

public interface BoardPostRepository extends JpaRepository<BoardPost, Long> {

}
