package com.project.quiz.repository;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizQuestion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
	    List<Quiz> findAllByOrderByCreatedAtDesc();  // 최신순
	    List<Quiz> findAllByOrderByQuizIdDesc();     // 인기순(예시)
	    List<QuizQuestion> findByQuizId(Long quizId);
	    
	    @Query("""
	    	    select distinct q
	    	    from Quiz q
	    	    left join fetch q.questions
	    	    where q.quizId = :id
	    	""")
	    	Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);
	
}