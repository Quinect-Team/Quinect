package com.project.quiz.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.repository.QuizGradingRepository;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserProfileRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

	private final UserRepository userRepository;
	private final UserProfileRepository userProfileRepository;
	private final QuizGradingRepository quizGradingRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final FileStorageService fileStorageService;
	private final UserActivityLogRepository logRepository;

	public User create(String username, String email, String password) {
		User user = new User();
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole("USER");
		user.setStatus("ACTIVE");
		user.setCreatedAt(LocalDateTime.now());

		// ▼▼▼ 프로필 별도 생성 후 연결 ▼▼▼
		UserProfile profile = UserProfile.builder().username(username) // 닉네임
				.build();

		user.setUserProfile(profile); // 연관관계 설정 (User 저장 시 Profile도 자동 저장됨)

		return userRepository.save(user);
	}

	public User findByEmail(String name) {
		return userRepository.findByEmail(name).orElse(null);
	}

	/*
	 * @Transactional // DB 변경이 일어나므로 트랜잭션 필수 public void updateProfile(String
	 * email, String newNickname, String newOrganization, String newBio) { // 1. 유저
	 * 조회 User user = userRepository.findByEmail(email) .orElseThrow(() -> new
	 * IllegalArgumentException("회원이 존재하지 않습니다."));
	 * 
	 * // 2. 연결된 프로필 가져오기 UserProfile profile = user.getUserProfile();
	 * 
	 * // 3. 값 변경 (Dirty Checking에 의해 자동 저장됨) if(profile != null) {
	 * profile.setUsername(newNickname); profile.setOrganization(newOrganization);
	 * profile.setBio(newBio); } // (만약 profile이 null일 경우는 회원가입 로직상 없겠지만, 필요하다면 여기서
	 * 생성 로직 추가 가능) }
	 */

	// 여기서부터 이메일 인증
	private static class VerificationInfo {
		String code;
		LocalDateTime createAt;

		public VerificationInfo(String code) {
			this.code = code;
			this.createAt = LocalDateTime.now();
		}
	}

	private final Map<String, VerificationInfo> verificationCodes = new ConcurrentHashMap<>();

	public void sendVerificationCode(String email) {
		// 1. 유저 존재 여부 확인
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

		// 2. 소셜 로그인 유저인지 확인 (비밀번호 변경 불가)
		if (user.getProvider() != null) {
			throw new IllegalArgumentException("소셜 연동 계정입니다. 해당 플랫폼에서 로그인을 시도해주세요.");
		}

		// 3. 인증 번호 6자리 생성
		String code = generateRandomCode();

		// 4. 저장소에 저장 (이미 있으면 덮어쓰기)
		verificationCodes.put(email, new VerificationInfo(code));

		// 5. 이메일 발송
		String subject = "[Quinect] 비밀번호 찾기 인증 코드";
		String text = "인증 코드: " + code + "\n\n이 코드를 입력하여 비밀번호를 재설정하세요.";
		emailService.sendEmail(email, subject, text);
	}

	// ▼▼▼ [추가] 2. 인증 번호 검증 메서드 ▼▼▼
	public boolean verifyCode(String email, String code) {
		// 1. 메모리에 저장된 인증 번호 가져오기
		VerificationInfo info = verificationCodes.get(email);

		// 2. 인증 번호가 없거나 틀리면 즉시 실패
		if (info == null || !info.code.equals(code)) {
			return false;
		}

		long secondsDiff = Duration.between(info.createAt, LocalDateTime.now()).getSeconds();
		if (secondsDiff > 180) {
			verificationCodes.remove(email); // 만료된 코드 삭제
			return false; // 시간 초과로 실패 처리
		}
		// ▼▼▼ [추가된 로직] DB에서 유저 상태 재확인 (Double Check) ▼▼▼
		// 인증 번호가 맞았더라도, 실제 유저가 ACTIVE 상태인지 한 번 더 검사합니다.
		User user = userRepository.findByEmail(email).orElse(null);

		if (user == null || !"ACTIVE".equals(user.getStatus())) {
			// 유저가 없거나, 활동 정지(INACTIVE) 등의 상태라면 인증 거부
			verificationCodes.remove(email); // 보안상 인증 코드도 파기
			return false;
		}
		// ▲▲▲ 추가 끝 ▲▲▲

		// 3. 모든 검사를 통과했으므로 성공 처리
		verificationCodes.remove(email); // 재사용 방지 삭제
		return true;
	}

	// ▼▼▼ [추가] 3. 비밀번호 재설정 메서드 ▼▼▼
	@Transactional
	public void updatePassword(String email, String newPassword) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));

		user.setPassword(passwordEncoder.encode(newPassword));
	}

	// (내부용) 6자리 난수 생성기
	private String generateRandomCode() {
		Random random = new Random();
		int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
		return String.valueOf(code);
	}

	// 프로필 사진 업로드
	@Transactional
	public void updateProfile(String email, String nickname, String organization, String bio,
			MultipartFile profileImageFile, String defaultProfileImage) {

		User user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
		UserProfile profile = user.getUserProfile();

		if (!profile.getUsername().equals(nickname)) {
			String logDesc = String.format("%s님이 닉네임을 %s(으)로 변경했습니다.", profile.getUsername(), nickname); // "홍길동님이 닉네임을
																											// 고길동(으)로..."

			UserActivityLog log = UserActivityLog.builder().user(user).activityType("UPDATE_NICKNAME") // 새로운 타입
					.description(logDesc).createdAt(LocalDateTime.now()).build();

			logRepository.save(log);
		}
		// 1. 텍스트 정보 업데이트
		profile.setUsername(nickname);
		profile.setOrganization(organization);
		profile.setBio(bio);

		// 2. 이미지 처리 로직 (우선순위: 파일 업로드 > 기본 이미지 선택)
		if (profileImageFile != null && !profileImageFile.isEmpty()) {
			// (A) 파일이 업로드된 경우 -> 파일 저장 후 URL 사용
			String imageUrl = fileStorageService.storeFile(profileImageFile);
			profile.setProfileImage(imageUrl);

		} else if (defaultProfileImage != null && !defaultProfileImage.isEmpty()) {
			// (B) 파일은 없는데 기본 이미지를 선택한 경우 -> 해당 경로 그대로 사용
			// (보안상 /img/ 로 시작하는지 체크하는 것이 좋음)
			if (defaultProfileImage.startsWith("/img/")) {
				profile.setProfileImage(defaultProfileImage);
			}
		}
		// (C) 둘 다 없으면 -> 기존 이미지 유지
	}

	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));
	}

	@Transactional
	public void withdraw(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		user.setStatus("pending");
		user.setStatusChangedAt(LocalDateTime.now());
		// 필요하다면 여기서 user.setRefreshToken(null) 등 토큰 삭제 로직 추가
	}

	/**
	 * 계정 복구 (Reactivate)
	 */
	@Transactional
	public void reactivate(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

		if ("pending".equals(user.getStatus())) {
			user.setStatus("ACTIVE"); // 대문자로 통일 권장 (ACTIVE)
			user.setStatusChangedAt(null);
		}
	}

	@Transactional
	public void cleanupWithdrawnUsers() {
		// 1. 현재 시간 기준 7일 전
		// LocalDateTime sevenDaysAgo = LocalDateTime.now().minusMinutes(1); 테스트용
		LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

		// 2. 대상 조회 (pending 상태 + 7일 경과)
		List<User> users = userRepository.findByStatusAndStatusChangedAtBefore("pending", sevenDaysAgo);

		for (User user : users) {
			// (1) 이메일 익명화 (Unique 제약조건 회피를 위해 UUID 사용)
			// 예: deleted_550e8400-e29b...@quinect.com
			String randomStr = UUID.randomUUID().toString().substring(0, 8);
			user.setEmail("deleted_" + user.getId() + "_" + randomStr + "@quinect.com");

			// (2) 소셜 로그인 정보 파기
			user.setProvider(null);
			user.setProviderId(null);
			user.setPassword(null); // 혹시 모를 비번 삭제

			// (3) 프로필 정보 익명화
			UserProfile profile = user.getUserProfile();
			if (profile != null) {
				profile.setUsername("(알 수 없음)"); // 닉네임 변경
				profile.setBio(null); // 자기소개 삭제
				profile.setOrganization(null); // 소속 삭제
				profile.setProfileImage(null); // 프사 삭제 (기본이미지로)
				// 포인트는 남길지 말지 선택 (보통은 0으로 초기화)
				// profile.setPointBalance(0L);
			}

			// (4) 상태 변경 (최종 삭제 처리)
			user.setStatus("deleted");
			user.setStatusChangedAt(LocalDateTime.now());
		}
	}

	@Transactional
	public List<UserProfile> getTopUsersByPointBalance() {
		return userProfileRepository.findTopByPointBalance();
	}

	/**
	 * 현재 로그인 사용자의 정답/오답 개수
	 */
	@Transactional(readOnly = true)
	public Map<String, Long> getUserAnswerStats(Long userId) {
		long correctCount = quizGradingRepository.countByUserIdAndIsCorrectTrue(userId);
		long wrongCount = quizGradingRepository.countByUserIdAndIsCorrectFalse(userId);

		return Map.of("correct", correctCount, "wrong", wrongCount);
	}

	public long getActiveUsers() {
		return userRepository.count() - 1;
	}

}