package com.project.quiz.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public User create(String username, String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());

        // ▼▼▼ 프로필 별도 생성 후 연결 ▼▼▼
        UserProfile profile = UserProfile.builder()
                .username(username)       // 닉네임
                .pointBalance(100L)       // 기본 포인트
                .build();

        user.setUserProfile(profile); // 연관관계 설정 (User 저장 시 Profile도 자동 저장됨)
        
        return userRepository.save(user);
    }


    public User findByEmail(String name) {
		return userRepository.findByEmail(name).orElse(null);
	}
    
    @Transactional // DB 변경이 일어나므로 트랜잭션 필수
    public void updateProfile(String email, String newNickname, String newOrganization, String newBio) {
        // 1. 유저 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        
        // 2. 연결된 프로필 가져오기
        UserProfile profile = user.getUserProfile();
        
        // 3. 값 변경 (Dirty Checking에 의해 자동 저장됨)
        if(profile != null) {
            profile.setUsername(newNickname);
            profile.setOrganization(newOrganization);
            profile.setBio(newBio);
        }
        // (만약 profile이 null일 경우는 회원가입 로직상 없겠지만, 필요하다면 여기서 생성 로직 추가 가능)
    }
    
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
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));
        
        user.setPassword(passwordEncoder.encode(newPassword));
    }

    // (내부용) 6자리 난수 생성기
    private String generateRandomCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 100000 ~ 999999
        return String.valueOf(code);
    }
}