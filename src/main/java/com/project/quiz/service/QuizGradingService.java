package com.project.quiz.service;

import com.project.quiz.domain.*;
import com.project.quiz.dto.QuizSolvedEvent;
import com.project.quiz.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGradingService {

	private final QuizSubmissionRepository submissionRepository;
	private final QuizGradingRepository gradingRepository;
	private final QuizAnswerRepository quizAnswerRepository;

	// ⭐ [추가] 연결을 위해 필요한 의존성 주입
	private final UserRepository userRepository;
	private final UserActivityLogRepository logRepository; // 타임라인용 로그
	private final PointService pointService; // 포인트 지급
	private final ApplicationEventPublisher eventPublisher;// 업적 이벤트 발행

	/**
	 * 제출된 답안 전체 채점 (자동)
	 */
	@Transactional
	public void grade(Long submissionId) {

		// 1. 제출 정보 조회
		QuizSubmission submission = submissionRepository.findById(submissionId)
				.orElseThrow(() -> new RuntimeException("제출 정보를 찾을 수 없습니다."));

		int totalScore = 0;
		List<QuizAnswer> answers = new ArrayList<>(submission.getAnswers());

		// 2. 채점 로직 (기존과 동일)
		for (QuizAnswer answer : answers) {
			QuizQuestion question = answer.getQuestion();
			boolean isCorrect = false;
			int score = 0;

			// 객관식
			if (question.getQuizTypeCode() == 2) {
				if (answer.getSelectedOption() != null && question.getAnswerOption() != null) {
					if (question.getAnswerOption().trim().equals(answer.getSelectedOption().toString().trim())) {
						isCorrect = true;
						score = (question.getPoint() != null) ? question.getPoint() : 0;
					}
				}
			}
			// 서술형
			else if (question.getQuizTypeCode() == 1) {
				if (answer.getAnswerText() != null && question.getSubjectiveAnswer() != null) {
					String userAnswer = answer.getAnswerText().trim().replaceAll("\\s+", " ");
					String correctAnswer = question.getSubjectiveAnswer().trim().replaceAll("\\s+", " ");
					if (userAnswer.equalsIgnoreCase(correctAnswer)) {
						isCorrect = true;
						score = (question.getPoint() != null) ? question.getPoint() : 0;
					}
				}
			}

			// 결과 저장
			boolean finalCorrect = isCorrect;
			int finalScore = score;

			QuizGrading grading = gradingRepository.findByAnswer_AnswerId(answer.getAnswerId()).orElseGet(() -> {
				QuizGrading g = new QuizGrading();
				g.setAnswer(answer);
				return g;
			});

			grading.setCorrect(finalCorrect);
			grading.setScore(finalScore);
			grading.setGrader("AUTO");
			grading.setGradedAt(LocalDateTime.now());

			if (grading.getGradingId() == null) {
				answer.setGrading(grading);
			}
			gradingRepository.save(grading);

			totalScore += finalScore;
		}

		// 3. 제출 정보 업데이트
		submission.setTotalScore(totalScore);
		submission.setGraded(true);
		submissionRepository.save(submission);

		// ============================================================
		// ⭐ [추가됨] 로그, 포인트, 업적 연결 로직
		// ============================================================

		// 유저 정보 조회
		User user = userRepository.findById(submission.getUserId()).orElse(null);

		if (user != null) {
			String quizTitle = submission.getQuiz().getTitle();

			// 1️⃣ [타임라인] 활동 로그 기록 (ActivityType: "QUIZ")
			// TimelineService가 이 로그를 읽어서 타임라인에 표시함 [cite: 1050, 1053]
			UserActivityLog activityLog = UserActivityLog.builder().user(user).activityType("QUIZ")
					.description(user.getUserProfile().getUsername() + "님이 [" + quizTitle + "] 퀴즈를 풀었습니다.")
					.createdAt(LocalDateTime.now()).build();
			logRepository.save(activityLog);

			// 2️⃣ [포인트] 점수만큼 포인트 지급
			// PointService 내부에서 포인트 로그("POINT_EARN")도 자동으로 남김 [cite: 974, 975]
			if (totalScore > 0) {
				try {
					pointService.addPoint(user, totalScore, "퀴즈 보상: " + quizTitle);
					log.info("💰 포인트 지급 완료: {}P", totalScore);
				} catch (Exception e) {
					log.error("포인트 지급 실패", e);
				}
			}

			// 3️⃣ [업적] 이벤트 발행 -> AchievementService가 수신
			// AchievementService.handleQuizSolved()가 실행됨
			// isCorrect가 true면 '정답 관련 업적', 그냥 풀기만 해도 '참여 업적' 카운트 증가
			boolean isPerfect = (totalScore > 0); // 일단 0점 이상이면 '성공'으로 간주 (조건 변경 가능)
			eventPublisher.publishEvent(new QuizSolvedEvent(user, isPerfect));

			log.info("✅ 퀴즈 활동 기록 완료 (타임라인/포인트/업적)");
		}
	}

	/**
	 * 개별 문항 수동/재채점용 (필요 시 사용)
	 */
	@Transactional
	public void grade(Long answerId, int score, boolean correct) {

		QuizGrading grading = gradingRepository.findByAnswer_AnswerId(answerId).orElseGet(() -> {
			QuizGrading g = new QuizGrading();
			g.setAnswer(quizAnswerRepository.getReferenceById(answerId));
			return g;
		});

		grading.setScore(score);
		grading.setCorrect(correct);
		grading.setGrader("MANUAL"); // 수동 채점임을 표시
		grading.setGradedAt(LocalDateTime.now());

		gradingRepository.save(grading);
	}
}