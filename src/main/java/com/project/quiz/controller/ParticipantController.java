package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.domain.Participant;
import com.project.quiz.service.ParticipantService;

@Controller
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantService participantService;

    @PostMapping("/participant/save")
    @ResponseBody
    public String saveParticipant(@RequestBody Participant participant) {
        participantService.saveParticipant(participant);
        return "saved";
    }
}
