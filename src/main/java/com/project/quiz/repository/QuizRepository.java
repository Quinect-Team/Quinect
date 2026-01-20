package com.project.quiz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizQuestion;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {
	List<QuizQuestion> findByQuizId(Long quizId);

	List<Quiz> findByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserId(Long UserId);

	@Query("""
			    select distinct q
			    from Quiz q
			    left join fetch q.questions
			    where q.quizId = :id
			""")

	Optional<Quiz> findByIdWithQuestions(@Param("id") Long id);

	@Modifying
	@Query("update Quiz q set q.scorePublic = :scorePublic where q.id = :quizId")
	void updateScorePublic(@Param("quizId") Long quizId, @Param("scorePublic") boolean scorePublic);

	List<Quiz> findByTitleContainingOrderByCreatedAtDesc(String title); // 최신순

	// 2. 인기순: quiz_submission(QuizSubmission)의 개수가 많은 순서
	@Query("SELECT q FROM Quiz q " + "LEFT JOIN QuizSubmission s ON q.quizId = s.quiz.quizId "
			+ "WHERE q.title LIKE %:title% " + // 검색 조건 추가
			"GROUP BY q.quizId " + "ORDER BY COUNT(s.submissionId) DESC, q.createdAt DESC")
	List<Quiz> findAllOrderByPopularityAndTitle(@Param("title") String title);

}