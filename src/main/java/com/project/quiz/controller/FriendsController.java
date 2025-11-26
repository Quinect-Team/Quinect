package com.project.quiz.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.quiz.domain.User;
import com.project.quiz.service.FriendshipService;

import lombok.RequiredArgsConstructor;

@RestController  // ⭐ @Controller → @RestController로 변경
@RequestMapping("/api/friends")  // ⭐ 경로 추가
@RequiredArgsConstructor
public class FriendsController {

    private final FriendshipService friendshipService;
	
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam("email") String email) {
        
        System.out.println("검색 요청 받음: " + email); // 디버깅용
        
        Long currentUserId = 1L; // 임시
        
        List<User> results = friendshipService.searchUsersByEmail(email, currentUserId);
        
        System.out.println("검색 결과: " + results.size() + "명"); // 디버깅용
        
        return ResponseEntity.ok(results);
    }
}
