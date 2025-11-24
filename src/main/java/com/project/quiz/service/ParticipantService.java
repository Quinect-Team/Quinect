package com.project.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.project.quiz.domain.Participant;
import com.project.quiz.repository.ParticipantRepository;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;

    // 참가자 저장
    public Participant saveParticipant(Participant participant) {
        return participantRepository.save(participant);
    }
}
