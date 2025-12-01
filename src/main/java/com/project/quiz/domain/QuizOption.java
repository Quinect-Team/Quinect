package com.project.quiz.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class QuizOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long optionId;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private QuizQuestion question;

    private Integer optionNumber;
    private String optionText;
}