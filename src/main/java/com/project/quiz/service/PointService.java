package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.domain.UserActivityPointChange;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.PointHistoryDto;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserActivityPointChangeRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointService {

	private final UserRepository userRepository;
	private final UserActivityLogRepository logRepository;
	private final UserActivityPointChangeRepository pointChangeRepository;

	// 1. 포인트 사용 (차감) 메서드
	@Transactional
	public void usePoint(User user, int amount, String reason) {
		UserProfile profile = user.getUserProfile();

		// 잔액 검사
		if (profile.getPointBalance() < amount) {
			throw new IllegalStateException("포인트가 부족합니다.");
		}

		// (1) 잔액 차감
		profile.setPointBalance(profile.getPointBalance() - amount);

		// (2) 로그 기록 (사용은 음수로 기록)
		savePointLog(user, "POINT_USE", -amount, reason);
	}

	// 2. 포인트 적립 메서드
	@Transactional
	public void addPoint(User user, int amount, String reason) {
		UserProfile profile = user.getUserProfile();

		// (1) 잔액 증가
		profile.setPointBalance(profile.getPointBalance() + amount);

		// (2) 로그 기록 (적립은 양수로 기록)
		savePointLog(user, "POINT_EARN", amount, reason);
	}

	// (내부 메서드) 로그 저장 로직 공통화
	private void savePointLog(User user, String type, int amount, String reason) {
		// 1. 활동 로그 생성 (부모)
		UserActivityLog log = UserActivityLog.builder().user(user).activityType(type).description(reason) // "전설의 검 구매"
				.createdAt(LocalDateTime.now()).build();
		logRepository.save(log);

		// 2. 포인트 상세 내역 생성 (자식)
		UserActivityPointChange pointChange = UserActivityPointChange.builder().userActivityLog(log).amount(amount) // -500
																													// or
																													// +300
				.reason(reason).build();
		pointChangeRepository.save(pointChange);
	}

	// 3. 포인트 내역 조회
	@Transactional(readOnly = true)
	public List<PointHistoryDto> getPointHistory(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

		// 1. UserActivityLog를 통해 포인트 내역 조회 (최신순 정렬 필요)
		List<UserActivityPointChange> historyList = pointChangeRepository
				.findAllByUserDesc(user);

		// 2. DTO로 변환
		return historyList.stream().map(PointHistoryDto::new).collect(Collectors.toList());
	}
}