package com.project.quiz.service;

import com.project.quiz.domain.*;
import com.project.quiz.dto.AnswerResult;
import com.project.quiz.repository.*;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizGradingService {

	private final QuizSubmissionRepository submissionRepository;
	private final QuizGradingRepository gradingRepository;
	private final QuizQuestionRepository questionRepository;
	private final ParticipantRepository participantRepository;
	private final QuizAnswerRepository quizAnswerRepository;

	@Transactional
	public void grade(Long submissionId) {

		QuizSubmission submission = submissionRepository.findById(submissionId)
				.orElseThrow(() -> new RuntimeException("제출 없음"));

		int totalScore = 0;

		List<QuizAnswer> answers = new ArrayList<>(submission.getAnswers());

		for (QuizAnswer answer : answers) {

			QuizQuestion question = answer.getQuestion();
			boolean correct = false;
			int score = 0;

			/* 1️⃣ 객관식 */
			if (question.getQuizTypeCode().equals(2)) {
				if (answer.getSelectedOption() != null && question.getAnswerOption() != null) {

					String correctOption = question.getAnswerOption().trim();
					String selected = answer.getSelectedOption().toString().trim();

					if (correctOption.equals(selected)) {
						correct = true;
						score = question.getPoint();
					}
				}
			}

			/* 2️⃣ 서술형 */
			if (question.getQuizTypeCode().equals(1)) {
				if (answer.getAnswerText() != null && question.getSubjectiveAnswer() != null) {

					String user = answer.getAnswerText().trim().replaceAll("\\s+", " ");

					String correctText = question.getSubjectiveAnswer().trim().replaceAll("\\s+", " ");

					if (user.equalsIgnoreCase(correctText)) {
						correct = true;
						score = question.getPoint();
					}
				}
				
				QuizGrading grading = gradingRepository
	                    .findByAnswer_AnswerId(answer.getAnswerId())
	                    .orElseGet(() -> {
	                        QuizGrading g = new QuizGrading();
	                        g.setAnswer(answer);
	                        return g;
	                    });

	            grading.setCorrect(correct);
	            grading.setScore(score);
	            grading.setGrader("AUTO");
	            grading.setGradedAt(LocalDateTime.now());

	            answer.setGrading(grading);
	            gradingRepository.save(grading);

	            totalScore += score;
			}

			QuizGrading grading = gradingRepository
			        .findByAnswer_AnswerId(answer.getAnswerId())
			        .orElseGet(() -> {
			            QuizGrading g = new QuizGrading();
			            g.setAnswer(answer);
			            return g;
			        });

			grading.setCorrect(correct);
			grading.setScore(score);
			grading.setGrader("AUTO");
			grading.setGradedAt(LocalDateTime.now());

			if (grading.getGradingId() == null) {
			    answer.setGrading(grading);
			}

			gradingRepository.save(grading);

			totalScore += score;
		}

		submission.setTotalScore(totalScore);
		submission.setGraded(true);
		submissionRepository.save(submission);
	}
	
	@Transactional
	public void grade(Long answerId, int score, boolean correct) {

	    QuizGrading grading = gradingRepository
	        .findByAnswer_AnswerId(answerId)
	        .orElseGet(() -> {
	            QuizGrading g = new QuizGrading();
	            g.setAnswer(
	                quizAnswerRepository.getReferenceById(answerId)
	            );
	            return g;
	        });

	    grading.setScore(score);
	    grading.setCorrect(correct);
	    grading.setGrader("AUTO");
	    grading.setGradedAt(LocalDateTime.now());

	    gradingRepository.save(grading);
	}



}
