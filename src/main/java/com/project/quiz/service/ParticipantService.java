package com.project.quiz.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.dto.UserRank;
import com.project.quiz.repository.ParticipantRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {
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
}
