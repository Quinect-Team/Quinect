package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    
    @PostMapping(value = "/upload-image", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {

        try {
            String fileName = quizService.storeImage(file);
            return ResponseEntity.ok(fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Upload error");
        }
    }
}