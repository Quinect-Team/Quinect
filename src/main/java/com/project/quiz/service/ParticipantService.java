package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizAnswer;
import com.project.quiz.domain.QuizGrading;
import com.project.quiz.domain.QuizQuestion;
import com.project.quiz.domain.QuizSubmission;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserActivityLog;
import com.project.quiz.dto.UserRank;
import com.project.quiz.repository.ParticipantRepository;
import com.project.quiz.repository.QuizRepository;
import com.project.quiz.repository.QuizSubmissionRepository;
import com.project.quiz.repository.UserActivityLogRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {
	
	@Autowired private QuizSubmissionRepository quizSubmissionRepository;
	@Autowired private QuizRepository quizRepository;
	@Autowired private PointService pointService;
	@Autowired private UserActivityLogRepository userActivityLogRepository;
	@Autowired private UserRepository userRepository;
	
	private final ParticipantRepository participantRepository;

	// 참가자 저장
	public Participant saveParticipant(Participant participant) {
		return participantRepository.save(participant);
	}

	public boolean existsByRoomAndUser(Room room, User user) {
		return participantRepository.existsByRoomAndUser(room, user);
	}

	public boolean existsByRoomAndGuestId(Room room, String guestId) {
		return participantRepository.existsByRoomAndGuestId(room, guestId);
	}

	public List<Participant> findByRoom(Room room) {
		return participantRepository.findByRoom(room);
	}

	public void joinRoomIfNotExists(Room room, User user, String guestId, String nickname, String avatarUrl) {
		// 게스트인 경우
		if (user == null && guestId != null) {
			Participant existing = participantRepository.findByRoomAndGuestId(room, guestId);
			if (existing == null) {
				Participant participant = new Participant();
				participant.setRoom(room);
				participant.setGuestId(guestId);
				participant.setNickname(nickname);
				participant.setAvatarUrl(avatarUrl);
				participant.setJoinAt(LocalDateTime.now());
				participantRepository.save(participant);
			}
		}
		// 로그인 사용자인 경우
		else if (user != null) {
			Participant existing = participantRepository.findByRoomAndUser(room, user);
			if (existing == null) {
				Participant participant = new Participant();
				participant.setRoom(room);
				participant.setUser(user);

				// UserProfile에서 가져오기
				if (user.getUserProfile() != null) {
					participant.setNickname(user.getUserProfile().getUsername());
					participant.setAvatarUrl(user.getUserProfile().getProfileImage());
				} else {
					participant.setNickname(user.getEmail());
					participant.setAvatarUrl(null);
				}

				participant.setJoinAt(LocalDateTime.now());
				participantRepository.save(participant);
			}
		}
	}

	public void saveQuizResults(Room room, List<UserRank> finalRanking) {
		try {
			if (room == null) {
				log.error("방이 null입니다");
				return;
			}

			// 이 방의 모든 참가자 조회
			List<Participant> participants = participantRepository.findByRoom(room);

			// 최종 순위 정보로 각 참가자 업데이트
			for (Participant participant : participants) {
				// 이 참가자가 finalRanking에 있는지 확인 (닉네임으로 매칭)
				UserRank userRank = finalRanking.stream()
						.filter(ur -> ur.getNickname().equals(participant.getNickname())).findFirst().orElse(null);

				if (userRank != null) {
					participant.setScore(userRank.getScore().longValue());
					participant.setRanking(userRank.getRank().longValue());
					log.info("✅ 참가자 결과 저장 - {}: 순위={}, 점수={}", participant.getNickname(), userRank.getRank(),
							userRank.getScore());
				}
			}

			// DB에 저장
			participantRepository.saveAll(participants);
			log.info("🎯 방 {} 의 모든 참가자 결과 저장 완료", room.getRoomCode());

		} catch (Exception e) {
			log.error("❌ 퀴즈 결과 저장 중 오류 발생", e);
		}
	}
	
	@Transactional // ⭐ 트랜잭션 보장 (가장 중요)
	public void processQuizRewards(Room room, List<UserRank> finalRanking, Long quizId, Map<Long, Set<Long>> correctQuestionsMap) {
	    // 1. 퀴즈 엔티티 조회 (확실하게 영속 상태로 가져옴)
	    Quiz quizEntity = quizRepository.findById(quizId).orElseThrow(() -> new IllegalArgumentException("퀴즈 없음"));
	    String quizTitle = quizEntity.getTitle();

	    for (UserRank rank : finalRanking) {
	        if (rank.getUserId() != null) {
	            User user = userRepository.findById(rank.getUserId()).orElse(null);
	            
	            if (user != null) {
	                // A. 제출 기록(Submission) 생성
	                QuizSubmission submission = new QuizSubmission();
	                submission.setQuiz(quizEntity);
	                submission.setUserId(user.getId());
	                submission.setTotalScore(rank.getScore());
	                submission.setGraded(true);
	                submission.setSubmittedAt(LocalDateTime.now());

	                // ⭐⭐ B. [핵심] 문제별 상세 채점 기록(Answer + Grading) 생성 ⭐⭐
	                // 유저가 맞춘 문제 ID 목록 가져오기
	                Set<Long> userCorrectIds = correctQuestionsMap.getOrDefault(user.getId(), new HashSet<>());

	                for (QuizQuestion q : quizEntity.getQuestions()) {
	                    // 1) 답안(Answer) 객체 생성 (통계용 더미 데이터)
	                    QuizAnswer answer = new QuizAnswer();
	                    answer.setQuestion(q);
	                    // 멀티플레이는 실제 입력값 저장이 어려우므로, 통계를 위해 빈 값이나 더미값 저장
	                    answer.setAnswerText(userCorrectIds.contains(q.getQuestionId()) ? "Correct" : "Wrong"); 
	                    
	                    // 2) 채점(Grading) 객체 생성
	                    QuizGrading grading = new QuizGrading();
	                    boolean isCorrect = userCorrectIds.contains(q.getQuestionId());
	                    
	                    grading.setCorrect(isCorrect);
	                    grading.setScore(isCorrect ? q.getPoint() : 0);
	                    grading.setGrader("AUTO_MULTI");
	                    grading.setGradedAt(LocalDateTime.now());
	                    
	                    // 3) 연결 (Submission -> Answer -> Grading)
	                    answer.setGrading(grading); // Answer에 Grading 연결
	                    submission.addAnswer(answer); // Submission에 Answer 연결
	                }

	                // C. 저장 (Cascade로 인해 Submission 저장 시 Answer, Grading도 자동 저장됨)
	                quizSubmissionRepository.save(submission);

	                // D. 포인트 및 로그 (기존과 동일)
	                if (rank.getScore() > 0) {
	                    pointService.addPoint(user, rank.getScore(), "멀티플레이 퀴즈 보상: " + quizTitle);
	                    UserActivityLog log = UserActivityLog.builder()
	                            .user(user)
	                            .activityType("QUIZ")
	                            .description(user.getUserProfile().getUsername() + "님이 [" + quizTitle + "] 멀티플레이 퀴즈를 완료했습니다.")
	                            .createdAt(LocalDateTime.now())
	                            .build();
	                    userActivityLogRepository.save(log);
	                }
	            }
	        }
	    }
	}
}
