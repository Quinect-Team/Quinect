package com.project.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.BoardPost;

public interface BoardRepository extends JpaRepository<BoardPost, Long> {
	List<BoardPost> findByBoardTypeCodeOrderByCreatedAtDesc(String boardTypeCode);
	
	Optional<BoardPost> findFirstByBoardTypeCodeOrderByCreatedAtDesc(String boardTypeCode);
}
