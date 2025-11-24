package com.project.quiz.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.quiz.domain.Participant;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    List<Participant> findByRoomId(Long roomId);
}
