package com.project.quiz.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityAttendance;

public interface AttendanceRepository extends JpaRepository<UserActivityAttendance, Long> {
    // Log 테이블을 통해 User를 찾아서 조회 (최신순 정렬 등은 선택사항)
    List<UserActivityAttendance> findAllByUserActivityLog_User(User user);
}