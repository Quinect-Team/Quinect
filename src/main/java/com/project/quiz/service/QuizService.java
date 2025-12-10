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
import com.project.quiz.repository.QuizRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;

    // 기존 saveQuiz 수정: 신규/수정 모두 처리
    public Long saveQuiz(QuizDto quizDto) {

        Quiz quiz;

        if (quizDto.getQuizId() != null) {
            // ── 기존 퀴즈 수정 ──
            quiz = quizRepository.findById(quizDto.getQuizId())
                    .orElseThrow(() -> new RuntimeException("퀴즈가 존재하지 않습니다."));
            quiz.setTitle(quizDto.getTitle());
            quiz.setDescription(quizDto.getDescription());
            quiz.setUpdatedAt(LocalDateTime.now());

            List<QuizQuestion> newQuestions = new ArrayList<>();
            for (QuizDto.QuestionDto q : quizDto.getQuestions()) {
                QuizQuestion question = new QuizQuestion();
                question.setQuiz(quiz);
                question.setQuestionText(q.getQuestionText());
                question.setQuizTypeCode(q.getQuizTypeCode());
                question.setImage(q.getImage());
                question.setPoint(q.getPoint());
                question.setAnswerOption(q.getAnswerOption());
                question.setSubjectiveAnswer(q.getSubjectiveAnswer());

                List<QuizOption> options = new ArrayList<>();
                if (q.getOptions() != null) {
                    int number = 1;
                    for (String text : q.getOptions()) {
                        QuizOption option = new QuizOption();
                        option.setQuestion(question);
                        option.setOptionNumber(number++);
                        option.setOptionText(text);
                        options.add(option);
                    }
                }
                question.setOptions(options);
                newQuestions.add(question);
            }

            // 기존 컬렉션 유지하면서 업데이트
            quiz.getQuestions().clear();
            quiz.getQuestions().addAll(newQuestions);

        } else {
            // ── 새 퀴즈 생성 ──
            quiz = new Quiz();
            quiz.setTitle(quizDto.getTitle());
            quiz.setDescription(quizDto.getDescription());
            quiz.setUserId(quizDto.getUserId());
            quiz.setCreatedAt(LocalDateTime.now());
            quiz.setUpdatedAt(LocalDateTime.now());

            List<QuizQuestion> questionEntities = new ArrayList<>();
            for (QuizDto.QuestionDto q : quizDto.getQuestions()) {
                QuizQuestion question = new QuizQuestion();
                question.setQuiz(quiz);
                question.setQuestionText(q.getQuestionText());
                question.setQuizTypeCode(q.getQuizTypeCode());
                question.setImage(q.getImage());
                question.setPoint(q.getPoint());
                question.setAnswerOption(q.getAnswerOption());
                question.setSubjectiveAnswer(q.getSubjectiveAnswer());

                List<QuizOption> options = new ArrayList<>();
                if (q.getOptions() != null) {
                    int number = 1;
                    for (String text : q.getOptions()) {
                        QuizOption option = new QuizOption();
                        option.setQuestion(question);
                        option.setOptionNumber(number++);
                        option.setOptionText(text);
                        options.add(option);
                    }
                }
                question.setOptions(options);
                questionEntities.add(question);
            }

            quiz.setQuestions(questionEntities);
        }

        quizRepository.save(quiz);
        return quiz.getQuizId();
    }


    public QuizDto getQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 퀴즈 ID: " + quizId));
        return QuizDto.fromEntity(quiz);
    }

    public String storeImage(MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new Exception("Empty file");
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/quiz/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        File target = new File(uploadDir + fileName);
        file.transferTo(target);

        return fileName;
    }

    public List<QuizDto> getQuizList() {
        List<Quiz> list = quizRepository.findAll();
        return list.stream()
                .map(QuizDto::fromEntity)
                .toList();
    }
}
