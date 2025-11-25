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
		boolean alreadyJoined = user != null ? existsByRoomAndUser(room, user) : existsByRoomAndGuestId(room, guestId);

		if (!alreadyJoined) {
			Participant participant = new Participant();
			participant.setRoom(room);
			participant.setUser(user);
			participant.setGuestId(guestId);
			participant.setNickname(nickname);
			participant.setAvatarUrl(avatarUrl);
			participant.setJoinAt(LocalDateTime.now());
			participantRepository.save(participant);
		}
	}

}
