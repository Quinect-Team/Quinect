package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
	List<Participant> findByRoom(Room room);

	boolean existsByRoomAndUser(Room room, User user);

	boolean existsByRoomAndGuestId(Room room, String guestId);

	Participant findByRoomAndUser(Room room, User user);

	Participant findByRoomAndGuestId(Room room, String guestId);

	List<Participant> findByUserIsNotNullOrderByScoreDescIdAsc();

}
