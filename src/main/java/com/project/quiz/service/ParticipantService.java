package com.project.quiz.service;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.repository.ParticipantRepository;

@Service
@RequiredArgsConstructor
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
}
