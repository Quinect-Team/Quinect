package com.project.quiz.service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;
import com.project.quiz.domain.QuizQuestion;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.repository.QuizQuestionRepository;
import com.project.quiz.repository.QuizRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;

    /* ================== 저장 ================== */
    public Long saveQuiz(QuizDto quizDto) {

        Quiz quiz;

        if (quizDto.getQuizId() != null) {
            quiz = quizRepository.findById(quizDto.getQuizId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("존재하지 않는 퀴즈 ID: " + quizDto.getQuizId()));

            quiz.setTitle(quizDto.getTitle());
            quiz.setDescription(quizDto.getDescription());
            quiz.setUpdatedAt(LocalDateTime.now());

            quiz.getQuestions().clear();
        } else {
            quiz = new Quiz();
            quiz.setTitle(quizDto.getTitle());
            quiz.setDescription(quizDto.getDescription());
            quiz.setUserId(quizDto.getUserId());
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setUpdatedAt(LocalDateTime.now());
        }

        List<QuizQuestion> questionEntities = new ArrayList<>();

        for (QuizDto.QuestionDto q : quizDto.getQuestions()) {

            QuizQuestion question = new QuizQuestion();
            
            question.setQuestionText(q.getQuestionText());
            question.setQuizTypeCode(q.getQuizTypeCode());
            question.setPoint(q.getPoint());
            question.setImage(q.getImage());

            /* ===== 객관식 ===== */
            if (q.getQuizTypeCode() == 2) {
                question.setAnswerOption(q.getAnswerOption());
                question.setSubjectiveAnswer(null);

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
            }

            quiz.addQuestion(question);
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
        if (!dir.exists()) dir.mkdirs();

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

    public List<Quiz> findAll() {
        return quizRepository.findAll();
    }
}
