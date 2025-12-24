package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "quiz_question")
@Getter @Setter
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @Column(name = "quiz_type_code", nullable = false)
    private Integer quizTypeCode;

    @Column(name = "question_text")
    private String questionText;

    @Column(name = "point")
    private Integer point;

    @Column(name = "answer_option")
    private String answerOption;

    @Column(name = "subjective_answer")
    private String subjectiveAnswer;

    @Column(name = "image")
    private String image;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizOption> options = new ArrayList<>();
    
    public void addOption(QuizOption option) {
        options.add(option);
        option.setQuestion(this);
    }
}