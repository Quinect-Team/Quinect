package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.Achievement;
import com.project.quiz.domain.AchievementType;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserAchievement;
import com.project.quiz.domain.UserInventory;
import com.project.quiz.dto.AttendanceEvent;
import com.project.quiz.dto.ItemPurchasedEvent;
import com.project.quiz.dto.QuizSolvedEvent;
import com.project.quiz.repository.AchievementRepository;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserInventoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final UserInventoryRepository userInventoryRepository; // 보상 지급용
    private final NotificationService notificationService; // 메시지 전달

    /**
     * ✅ 출석 이벤트 리스너
     * AttendanceService가 출석 이벤트를 던지면 여기서 받습니다.
     */
    @Async // 메인 로직(출석체크)에 영향 안 주게 비동기 처리 권장
    @EventListener
    @Transactional
    public void handleAttendance(AttendanceEvent event) {
        processAchievement(event.getUser(), AchievementType.ATTENDANCE_COUNT);
    }

    /**
     * ✅ 퀴즈 풀이 이벤트 리스너
     */
    @Async
    @EventListener
    @Transactional
    public void handleQuizSolved(QuizSolvedEvent event) {
        // 1. 그냥 '풀기만 해도' 오르는 업적 처리
        processAchievement(event.getUser(), AchievementType.QUIZ_SOLVED);

        // 2. '정답'일 경우만 오르는 업적 처리
        if (event.isCorrect()) {
            processAchievement(event.getUser(), AchievementType.QUIZ_CORRECT);
        }
    }

    /**
     * ⭐ 공통 업적 처리 로직 (핵심)
     * 1. 해당 타입의 업적들을 가져온다.
     * 2. 유저의 진행도(UserAchievement)를 찾거나 만든다.
     * 3. 숫자를 +1 한다.
     * 4. 목표 달성 시 보상을 준다.
     */
    private void processAchievement(User user, AchievementType type) {
        // 1. 해당 타입의 활성 업적 조회 (예: 50회 출석, 100회 출석 등)
        List<Achievement> targets = achievementRepository.findByAchievementTypeAndIsActiveTrue(type);

        for (Achievement achievement : targets) {
            // 2. 내 진행 상황 가져오기 (없으면 0으로 생성)
            UserAchievement progress = userAchievementRepository.findByUserAndAchievement(user, achievement)
                    .orElseGet(() -> createNewProgress(user, achievement));

            // 이미 달성했으면 스킵 (중복 보상 방지)
            if (progress.getIsAchieved()) {
                continue;
            }

            // 3. 진행도 증가 (+1)
            progress.incrementValue();
            
            // (옵션) 연속 출석 로직은 여기서 lastUpdatedAt 비교하여 초기화 로직 추가 가능

            // 4. 목표 달성 체크
            if (progress.getCurrentValue() >= achievement.getGoalValue()) {
                achieve(progress);
            }
        }
    }

    // 신규 진행도 생성
    private UserAchievement createNewProgress(User user, Achievement achievement) {
        UserAchievement ua = UserAchievement.builder()
                .user(user)
                .achievement(achievement)
                .currentValue(0L)
                .isAchieved(false)
                .isRewarded(false)
                .build();
        return userAchievementRepository.save(ua);
    }

    // 달성 처리 및 보상 지급
    private void achieve(UserAchievement progress) {
        progress.markAchieved(); // 달성 상태 변경 (DB 업데이트)
        
        Achievement goal = progress.getAchievement();
        log.info("🎉 업적 달성! 유저: {}, 업적: {}", progress.getUser().getEmail(), goal.getTitle());

        // 🎁 보상 아이템 지급 (UserInventory에 추가)
        if (goal.getRewardItem() != null) {
            UserInventory reward = UserInventory.builder()
                    .user(progress.getUser())
                    .item(goal.getRewardItem())
                    .purchasedAt(LocalDateTime.now())
                    .isEquipped(false)
                    .build();
            userInventoryRepository.save(reward);
            
            progress.markRewarded(); // 보상 지급 완료 처리
        }

        notificationService.send(
                progress.getUser(),
                "업적 달성! 🎉", 
                "[" + progress.getAchievement().getTitle() + "] 메달을 확인하세요.",
                "ACHIEVEMENT"
        );
    }
    
    @Async
    @EventListener
    @Transactional
    public void handleItemPurchase(ItemPurchasedEvent event) {
        // ITEM_COLLECTOR 타입의 업적을 찾아서 진행도 증가
        processAchievement(event.getUser(), AchievementType.ITEM_COLLECTOR);
    }
}