package com.project.quiz.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityAttendance;
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.dto.AttendanceEventDto;
import com.project.quiz.repository.AttendanceRepository;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final UserActivityLogRepository logRepository;
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    // [읽기] 오늘 출석 여부 확인 (Log 테이블만 조회)
    @Transactional(readOnly = true)
    public boolean hasCheckedInToday(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        return logRepository.existsByUserAndActivityTypeAndCreatedAtBetween(
                user, "ATTENDANCE", start, end
        );
    }

    // [쓰기] 출석 체크 진행 (Log -> Attendance -> Point 순서)
    @Transactional
    public void checkIn(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // 1. 중복 검사
        if (hasCheckedInToday(email)) {
            throw new IllegalStateException("이미 출석했습니다.");
        }

        // 2. 활동 로그 생성 (부모)
        UserActivityLog log = UserActivityLog.builder()
                .user(user)
                .activityType("ATTENDANCE")
                .description("일일 출석체크")
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(log); // ID 생성됨

        // 3. 출석 상세 기록 저장 (자식 - 로그와 연결)
        UserActivityAttendance attendance = UserActivityAttendance.builder()
                .userActivityLog(log) // 연결!
                .checkInDate(LocalDate.now())
                .pointsEarned(100)
                .build();
        attendanceRepository.save(attendance);

        // 4. 포인트 지급 (PointService가 별도 로그 생성함 - 정상)
        pointService.addPoint(user, 100, "일일 출석체크 보상");
    }
    
    @Transactional(readOnly = true)
    public List<AttendanceEventDto> getMyAttendanceEvents(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        return attendanceRepository.findAllByUserActivityLog_User(user)
                .stream()
                .map(AttendanceEventDto::new)
                .collect(Collectors.toList());
    }
}