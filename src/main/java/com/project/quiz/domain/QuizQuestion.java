package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    private String quizTypeCode;
    private String questionText;
    private String answerOption;
    private Integer point;
    private String subjectiveAnswer;
    private String image;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<QuizOption> options = new ArrayList<>();
}