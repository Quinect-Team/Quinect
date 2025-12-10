package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.project.quiz.domain.Quiz;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.repository.QuizRepository;
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
    
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {

        try {
            String fileName = quizService.storeImage(file);
            return ResponseEntity.ok(fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload error");
        }
    }
    @GetMapping("/list")
    public List<QuizDto> getQuizList() {
        return quizService.getQuizList();
    }
    
    @GetMapping("/{quizId}")
    public ResponseEntity<QuizDto> getQuiz(@PathVariable("quizId") Long quizId){
        QuizDto quiz = quizService.getQuiz(quizId);
        return ResponseEntity.ok(quiz);
    }




}