package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.dto.QuizDto;
import com.project.quiz.service.QuizService;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizApiController {

    private final QuizService quizService;

    @PostMapping("/save")
    public ResponseEntity<?> saveQuiz(@RequestBody QuizDto quizDto) {
        quizService.saveQuiz(quizDto);
        return ResponseEntity.ok("saved");
    }

//    @PostMapping("/temp-save")
//    public ResponseEntity<?> tempSave(@RequestBody QuizDto quizDto) {
//        quizService.tempSaveQuiz(quizDto);
//        return ResponseEntity.ok("temp saved");
//    }
}