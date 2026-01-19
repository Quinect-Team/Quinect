package com.project.quiz.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;
import com.project.quiz.domain.QuizQuestion;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.repository.QuizQuestionRepository;
import com.project.quiz.repository.QuizRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

	private final QuizRepository quizRepository;
	private final UserRepository userRepository;

	/* ================== 저장 ================== */
	@Transactional
	public Long saveQuiz(QuizDto quizDto) {
	    Quiz quiz;

	    if (quizDto.getQuizId() != null) {
	        quiz = quizRepository.findById(quizDto.getQuizId())
	                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 ID: " + quizDto.getQuizId()));

	        quiz.setTitle(quizDto.getTitle());
	        quiz.setDescription(quizDto.getDescription());
	        quiz.setScorePublic(quizDto.isScorePublic());
	        quiz.setUpdatedAt(LocalDateTime.now());
	        
	        // quiz.getQuestions().clear(); <- 이 부분을 삭제했습니다.
	    } else {
	        quiz = new Quiz();
	        quiz.setTitle(quizDto.getTitle());
	        quiz.setDescription(quizDto.getDescription());
	        quiz.setScorePublic(quizDto.isScorePublic());
	        quiz.setUserId(getLoginUserId());
	        quiz.setCreatedAt(LocalDateTime.now());
	        quiz.setUpdatedAt(LocalDateTime.now());
	    }

	    // [수정/추가 로직 시작]
	    for (QuizDto.QuestionDto q : quizDto.getQuestions()) {
	        QuizQuestion question = null;

	        // 1. 수정 모드일 때 기존 질문이 있는지 ID로 확인
	        if (quizDto.getQuizId() != null && q.getQuestionId() != null) {
	            question = quiz.getQuestions().stream()
	                    .filter(existingQ -> existingQ.getQuestionId().equals(q.getQuestionId()))
	                    .findFirst()
	                    .orElse(null);
	        }

	        // 2. 기존 질문이 없으면 새로 생성, 있으면 기존 객체 사용
	        if (question == null) {
	            question = new QuizQuestion();
	            quiz.addQuestion(question); // 신규일 때만 리스트에 추가
	        }

	        question.setQuestionText(q.getQuestionText());
	        question.setQuizTypeCode(q.getQuizTypeCode());
	        question.setPoint(q.getPoint());
	        question.setImage(q.getImage());

	        /* ===== 객관식 ===== */
	        if (q.getQuizTypeCode() == 2) {
	            question.setAnswerOption(q.getAnswerOption());
	            question.setSubjectiveAnswer(null);

	            // 보기는 관계가 복잡하므로 수정 시 일단 비우고 새로 넣는 것이 안전합니다 (addOption 사용)
	            if (question.getOptions() != null) {
	                question.getOptions().clear();
	            }

	            if (q.getOptions() != null) {
	                for (QuizDto.OptionDto opt : q.getOptions()) {
	                    QuizOption option = new QuizOption();
	                    option.setOptionNumber(opt.getOptionNumber());
	                    option.setOptionText(opt.getOptionText());
	                    question.addOption(option);
	                }
	            }
	        /* ===== 서술형 ===== */
	        } else {
	            question.setSubjectiveAnswer(q.getSubjectiveAnswer());
	            question.setAnswerOption(null);
	            if (question.getOptions() != null) {
	                question.getOptions().clear();
	            }
	        }
	    }

	    quizRepository.save(quiz);
	    return quiz.getQuizId();
	}

	public String storeImage(MultipartFile file) throws Exception {

		if (file == null || file.isEmpty()) {
			throw new Exception("Empty file");
		}

		String uploadDir = System.getProperty("user.dir") + "/uploads/quiz/";
		File dir = new File(uploadDir);
		if (!dir.exists())
			dir.mkdirs();

		String original = file.getOriginalFilename();
		String ext = original.substring(original.lastIndexOf("."));
		String fileName = UUID.randomUUID() + ext;

		File target = new File(uploadDir + fileName);
		file.transferTo(target);

		return fileName;
	}

	/* ================== 조회 ================== */
	public QuizDto findQuizDto(Long id) {
		Quiz quiz = quizRepository.findByIdWithQuestions(id)
				.orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + id));
		return QuizDto.fromEntity(quiz);
	}

	public Quiz findById(Long id) {
		return quizRepository.findById(id).orElse(null);
	}

	public List<Quiz> findAll() {
		return quizRepository.findAll();
	}

	private Long getLoginUserId() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal().equals("anonymousUser")) {
			throw new IllegalStateException("로그인 정보 없음");
		}

		Object principal = authentication.getPrincipal();

		System.out.println("principal class = " + principal.getClass());

		String email = null;

		// 1️⃣ 일반 로그인 (Form Login)
		if (principal instanceof UserDetails userDetails) {
			email = userDetails.getUsername();
		}

		// 2️⃣ OAuth2 로그인
		else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oauthUser) {
			email = oauthUser.getAttribute("email");
		}

		// 3️⃣ 진짜 최후의 보루
		else if (principal instanceof String str) {
			email = str;
		}

		if (email == null) {
			throw new IllegalStateException("email 추출 실패");
		}

		return userRepository.findByEmail(email).orElseThrow(() -> new IllegalStateException("유저 없음")).getId();
	}

	public List<Quiz> findMyQuizzes() {
		Long userId = getLoginUserId();
		return quizRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public QuizDto getQuizForPlay(Long quizId) {
		Quiz quiz = quizRepository.findByIdWithQuestions(quizId)
				.orElseThrow(() -> new IllegalArgumentException("Quiz not found: " + quizId));

		return QuizDto.fromEntity(quiz);
	}

}
