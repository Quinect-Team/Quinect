package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.dto.PointHistoryDto;
import com.project.quiz.service.PointService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PointApiController {

    private final PointService pointService;

    @GetMapping("/api/point/history")
    public ResponseEntity<List<PointHistoryDto>> getMyPointHistory(Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(pointService.getPointHistory(principal.getName()));
    }
}