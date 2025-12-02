package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.dto.TimelineDto;
import com.project.quiz.service.TimelineService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/timeline")
public class TimelineApiController {

    private final TimelineService timelineService;

    // GET /api/timeline?page=0&size=10
    @GetMapping
    public ResponseEntity<List<TimelineDto>> getTimeline(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Principal principal) {
        
        if (principal == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(
            timelineService.getTimelineByPage(principal.getName(), page, size)
        );
    }
}