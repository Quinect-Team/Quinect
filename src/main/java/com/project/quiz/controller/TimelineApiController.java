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
            @RequestParam(value = "targetEmail", required = false) String targetEmail, // ⭐ [추가] 조회할 대상 이메일
            Principal principal) {
        
        if (principal == null) return ResponseEntity.status(401).build();

        // ⭐ 파라미터로 targetEmail이 넘어오면 그 사람의 타임라인을, 없으면 내(principal) 타임라인을 조회
        String emailToSearch = (targetEmail != null && !targetEmail.isEmpty()) ? targetEmail : principal.getName();

        return ResponseEntity.ok(
            timelineService.getTimelineByPage(emailToSearch, page, size)
        );
    }
}