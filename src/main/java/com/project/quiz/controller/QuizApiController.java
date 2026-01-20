package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.project.quiz.config.SecurityConfig;
import com.project.quiz.domain.Quiz;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.repository.QuizRepository;
import com.project.quiz.service.QuizService;

@RestController
@RequestMapping("/quiz")
@RequiredArgsConstructor
public class QuizApiController {


    private final QuizService quizService;
    private final QuizRepository quizRepository;



    
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

    // JSON ONLY
    @ResponseBody
    @GetMapping("/api/{id}")
    public QuizDto getQuiz(@PathVariable("id") Long id) {
        return quizService.findQuizDto(id);
    }
    
    @GetMapping("/list")
    public List<QuizDto.ListResponse> getQuizList(
            @RequestParam(name = "sort", defaultValue = "latest") String sort,
            @RequestParam(name = "keyword", defaultValue = "") String keyword) {
        
        return quizService.getQuizList(sort, keyword).stream()
                .map(QuizDto.ListResponse::fromEntity)
                .toList();
    }

    @GetMapping("/api/my")
    public List<QuizDto.ListResponse> myQuizzes() {
        return quizService.findMyQuizzes().stream()
                .map(QuizDto.ListResponse::fromEntity)
                .toList();
    }

    
}