package com.project.quiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.dto.GuestUserDto;
import com.project.quiz.service.GuestUserService;

import org.springframework.ui.Model;

import jakarta.servlet.http.HttpSession;

@Controller
@RequiredArgsConstructor
public class GuestUserController {

    private final GuestUserService guestUserService;

    // 닉네임/캐릭터 선택 폼 보여주기
    @GetMapping("/guest/setup")
    public String showSetupForm(@RequestParam(name = "next", required = false) String next, Model model) {
        model.addAttribute("next", next);
        return "guest_setup";
    }

    // 닉네임/캐릭터 선택 제출 처리
    @PostMapping("/guest/setup")
    public String submitGuestInfo(@RequestParam("nickname") String nickname,
                                  @RequestParam("characterImageUrl") String characterImageUrl,
                                  @RequestParam(name="next", required = false) String next,
                                  HttpSession session,
                                  Model model) {
        // 유효성 체크
        if (!guestUserService.validateNickname(nickname) ||
            !guestUserService.validateCharacter(characterImageUrl)) {
            model.addAttribute("error", "닉네임 또는 캐릭터를 다시 선택해주세요.");
            return "guest_setup";
        }

        // 세션에 DTO 저장
        GuestUserDto guestUser = GuestUserDto.builder()
            .nickname(nickname)
            .characterImageUrl(characterImageUrl)
            .build();
        session.setAttribute("guestUser", guestUser);

        // next(목적지)가 있으면 해당 방으로 리다이렉트
        if (next != null && !next.isEmpty()) {
            return "redirect:" + next;
        }
        // 아니면 기본 페이지로 이동
        return "redirect:/";
    }

    // 퀴즈 종료/퇴장 시 세션에서 삭제
    @GetMapping("/exit")
    public String guestExit(HttpSession session) {
        session.removeAttribute("guestUser");
        return "redirect:/";
    }
}
