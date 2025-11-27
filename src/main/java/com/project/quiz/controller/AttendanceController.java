package com.project.quiz.controller;

import com.project.quiz.dto.AttendanceEventDto;
import com.project.quiz.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    // 1. 상세 페이지 화면 이동
    @GetMapping("/attendance/detail")
    public String attendanceDetailPage() {
        return "attendancedetail";
    }

    // 2. [API] 달력 데이터 제공 (FullCalendar가 이걸 호출함)
    @GetMapping("/api/attendance/events")
    @ResponseBody
    public ResponseEntity<List<AttendanceEventDto>> getAttendanceEvents(Principal principal) {
        if (principal == null) return ResponseEntity.badRequest().build();
        
        List<AttendanceEventDto> events = attendanceService.getMyAttendanceEvents(principal.getName());
        return ResponseEntity.ok(events);
    }
}