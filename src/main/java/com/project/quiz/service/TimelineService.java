package com.project.quiz.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.dto.TimelineDto;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final UserActivityLogRepository logRepository;
    private final UserRepository userRepository;
    // 타임라인에 필터링할 내용들, 다른 기능 추가되면 필터링 추가필요!
    private static final List<String> TARGET_ACTIVITIES = List.of("ATTENDANCE", "POINT_USE", "QUIZ", "UPDATE_NICKNAME", "ACHIEVEMENT");

    // 1. [프로필용] 최신순으로 딱 2개만 가져오기
    @Transactional(readOnly = true)
    public List<TimelineDto> getProfileTimeline(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // [수정] 필터링 메서드 호출
        Page<UserActivityLog> page = logRepository.findAllByUserAndActivityTypeIn(user, TARGET_ACTIVITIES, pageRequest);
        
        return page.stream().map(TimelineDto::new).collect(Collectors.toList());
    }

    // 2. [타임라인 페이지용] 페이징 처리하여 가져오기 (무한 스크롤)
    @Transactional(readOnly = true)
    public List<TimelineDto> getTimelineByPage(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // [수정] 필터링 메서드 호출
        Page<UserActivityLog> resultPage = logRepository.findAllByUserAndActivityTypeIn(user, TARGET_ACTIVITIES, pageRequest);

        return resultPage.stream().map(TimelineDto::new).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<TimelineDto> getTimelineByPage(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserActivityLog> resultPage = logRepository.findAllByUserAndActivityTypeIn(user, TARGET_ACTIVITIES, pageRequest);
        
        return resultPage.stream().map(TimelineDto::new).collect(Collectors.toList());
    }
}