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
            @RequestParam(value = "userId", required = false) Long targetUserId, // ⭐ 추가됨
            Principal principal) {
        
        // 1. 타겟 유저 결정
        // targetUserId가 오면 그 사람 것을, 안 오면 로그인한 내 것을 조회
        if (targetUserId != null) {
            // (선택) 여기서 친구 관계인지 체크하는 보안 로직을 넣을 수도 있음
            return ResponseEntity.ok(
                timelineService.getTimelineByPage(targetUserId, page, size) // ⭐ Service 메서드 오버로딩 필요
            );
        } 
        
        // 기존 로직 (내 타임라인)
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(
            timelineService.getTimelineByPage(principal.getName(), page, size)
        );
    }
}