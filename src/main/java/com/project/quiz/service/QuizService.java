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

    public Long saveQuiz(QuizDto quizDto) {

        // QUIZ 저장
        Quiz quiz = new Quiz();
        quiz.setTitle(quizDto.getTitle());
        quiz.setDescription(quizDto.getDescription());
        quiz.setUserId(quizDto.getUserId());
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUpdatedAt(LocalDateTime.now());

        List<QuizQuestion> questionEntities = new ArrayList<>();

        // 문항 저장
        for (QuizDto.QuestionDto q : quizDto.getQuestions()) {

            QuizQuestion question = new QuizQuestion();
            question.setQuiz(quiz);
            question.setQuestionText(q.getQuestionText());
            question.setQuizTypeCode(q.getQuizTypeCode());
            question.setImage(q.getImage());
            question.setPoint(q.getPoint());
            question.setAnswerOption(q.getAnswerOption());
            question.setSubjectiveAnswer(q.getSubjectiveAnswer());

            List<QuizOption> optionEntities = new ArrayList<>();

            // 보기 저장
            if (q.getOptions() != null) {
                int number = 1;
                for (String text : q.getOptions()) {
                    QuizOption option = new QuizOption();
                    option.setQuestion(question);
                    option.setOptionNumber(number++);
                    option.setOptionText(text);
                    optionEntities.add(option);
                }
            }

            question.setOptions(optionEntities);
            questionEntities.add(question);
        }

        quiz.setQuestions(questionEntities);
        quizRepository.save(quiz);

        return quiz.getQuizId();
    }
    public String storeImage(MultipartFile file) throws Exception {

        if (file.isEmpty()) {
            throw new Exception("Empty file");
        }

        // 저장 경로 (프로젝트 내부)
        String uploadDir = System.getProperty("user.dir") + "/uploads/quiz/";

        // 폴더 없으면 생성
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        // 파일명 생성 (UUID + 원본 확장자)
        String original = file.getOriginalFilename();
        String ext = original.substring(original.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + ext;

        // 실제 저장
        File target = new File(uploadDir + fileName);
        file.transferTo(target);

        return fileName; // DB에는 파일명만 저장
    }

}
