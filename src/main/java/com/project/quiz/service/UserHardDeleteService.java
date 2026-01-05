package com.project.quiz.service;

import com.project.quiz.domain.User;
import com.project.quiz.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserHardDeleteService {

    private final UserRepository userRepository;

    // ✅ JPA EntityManager: 리포지토리 없이 DB에 직접 명령을 내리는 도구
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Lazy
    private UserHardDeleteService self;

    // Ghost 계정 ID (DB에 있는 관리자 ID)
    private static final Long GHOST_USER_ID = 1L;

    public String executeHardDeleteProcess() {
        if (!userRepository.existsById(GHOST_USER_ID)) {
            return "❌ 실패: Ghost 계정(ID=" + GHOST_USER_ID + ")이 없습니다.";
        }

        List<User> deletedUsers = userRepository.findByStatus("deleted");
        if (deletedUsers.isEmpty()) {
            return "✅ 삭제할 대상이 없습니다.";
        }

        log.info("🧹 총 {}명의 유저 정리 시작...", deletedUsers.size());

        int success = 0;
        int fail = 0;

        for (User user : deletedUsers) {
            if (user.getId().equals(GHOST_USER_ID)) continue;

            try {
                self.deleteSingleUser(user);
                success++;
            } catch (Exception e) {
                log.error("❌ 유저 ID {} 정리 실패: {}", user.getId(), e.getMessage());
                fail++;
            }
        }

        return String.format("✅ 정리 완료: 성공 %d명, 실패 %d명", success, fail);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSingleUser(User user) {
        Long targetId = user.getId();
        log.info("🗑️ 유저 ID {} ({}) 삭제 시작", targetId, user.getEmail());

        // =========================================================
        // ✅ 1. 콘텐츠 소유권 이관 (리포지토리 X -> 직접 SQL 사용)
        // =========================================================
        // 게시글, 댓글, 퀴즈, 방장 권한 등을 Ghost 계정으로 넘깁니다.
        
        updateOwner("board_post", "user_id", targetId);       // 게시글
        updateOwner("board_comment", "user_id", targetId);    // 댓글
        updateOwner("quiz", "user_id", targetId);             // 퀴즈
        updateOwner("quiz_submission", "user_id", targetId);  // 퀴즈 제출 기록
        updateOwner("room", "host_user_id", targetId);        // 방장 권한

        // =========================================================
        // ✅ 2. 찌꺼기 데이터 강제 삭제 (Native SQL)
        // =========================================================
        
        // (1) 친구 메시지 삭제 (조인 삭제)
        // "이 유저가 낀 채팅방의 메시지는 다 지워라"
        entityManager.createNativeQuery(
            "DELETE fm FROM friend_message fm " +
            "JOIN friendship f ON fm.friendship_id = f.id " +
            "WHERE f.user_id = :uid OR f.friend_user_id = :uid")
            .setParameter("uid", targetId)
            .executeUpdate();

        // (2) 친구 관계 삭제
        entityManager.createNativeQuery(
            "DELETE FROM friendship WHERE user_id = :uid OR friend_user_id = :uid")
            .setParameter("uid", targetId)
            .executeUpdate();

        // (3) 단순 종속 테이블 삭제
        deleteFromTable("user_inventory", "user_id", targetId);
        deleteFromTable("user_achievement", "user_id", targetId);
        deleteFromTable("notification_recipient", "user_id", targetId);
        deleteFromTable("participant", "user_id", targetId);

        // (4) 활동 로그 삭제 (자식 -> 부모 순서)
        // 로그의 자식들(출석, 포인트) 먼저 삭제
        entityManager.createNativeQuery(
            "DELETE FROM user_activity_attendance WHERE activity_log_id IN (SELECT id FROM user_activity_log WHERE user_id = :uid)")
            .setParameter("uid", targetId).executeUpdate();
            
        entityManager.createNativeQuery(
            "DELETE FROM user_activity_point_change WHERE activity_log_id IN (SELECT id FROM user_activity_log WHERE user_id = :uid)")
            .setParameter("uid", targetId).executeUpdate();

        // 로그 본체 삭제
        deleteFromTable("user_activity_log", "user_id", targetId);

        // =========================================================
        // ✅ 3. 유저 완전 삭제
        // =========================================================
        userRepository.delete(user);
        
        log.info("✨ 유저 ID {} 삭제 완료", targetId);
    }

    // [헬퍼 메서드 1] 소유권 이관 SQL 실행기
    private void updateOwner(String tableName, String columnName, Long targetId) {
        String sql = String.format("UPDATE %s SET %s = :ghostId WHERE %s = :targetId", tableName, columnName, columnName);
        entityManager.createNativeQuery(sql)
                .setParameter("ghostId", GHOST_USER_ID)
                .setParameter("targetId", targetId)
                .executeUpdate();
    }

    // [헬퍼 메서드 2] 단순 삭제 SQL 실행기
    private void deleteFromTable(String tableName, String columnName, Long userId) {
        String sql = String.format("DELETE FROM %s WHERE %s = :uid", tableName, columnName);
        entityManager.createNativeQuery(sql)
                .setParameter("uid", userId)
                .executeUpdate();
    }
}