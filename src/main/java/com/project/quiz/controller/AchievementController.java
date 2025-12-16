package com.project.quiz.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.project.quiz.domain.Achievement;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserAchievement;
import com.project.quiz.dto.AchievementDisplayDto;
import com.project.quiz.repository.AchievementRepository;
import com.project.quiz.repository.UserAchievementRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class AchievementController {

	private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;

    // ✅ 이 부분이 있어야 /achievement 요청을 받아줍니다.
    @GetMapping("/achievement")
    public String achievementPage(Model model, Principal principal) {
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                // 1. 전체 업적 조회
                List<Achievement> allAchievements = achievementRepository.findAll();

                // 2. 내 진행 상황 조회
                Map<Long, UserAchievement> myProgressMap = userAchievementRepository.findAllByUser(user)
                        .stream()
                        .collect(Collectors.toMap(ua -> ua.getAchievement().getId(), ua -> ua));

                // 3. DTO 리스트 생성
                List<AchievementDisplayDto> displayList = new ArrayList<>();
                for (Achievement ach : allAchievements) {
                    UserAchievement progress = myProgressMap.get(ach.getId());
                    long current = (progress != null) ? progress.getCurrentValue() : 0;
                    boolean achieved = (progress != null) && progress.getIsAchieved();
                    displayList.add(AchievementDisplayDto.of(ach, current, achieved));
                }

                // 4. 정렬 (미달성 위, 달성 아래)
                displayList.sort(Comparator.comparing(AchievementDisplayDto::isAchieved)
                        .thenComparing(dto -> dto.getAchievement().getId()));

                model.addAttribute("achievements", displayList);

                // ▼▼▼ [추가] 상단 요약용 개수 계산 ▼▼▼
                int totalCount = displayList.size();
                long achievedCount = displayList.stream().filter(AchievementDisplayDto::isAchieved).count();
                
                model.addAttribute("totalCount", totalCount);
                model.addAttribute("achievedCount", achievedCount);
                
                // 퍼센트(전체 진행률) 계산 - 0으로 나누기 방지
                int totalPercent = (totalCount > 0) ? (int)((double)achievedCount / totalCount * 100) : 0;
                model.addAttribute("totalPercent", totalPercent);
            }
        }
        return "achievement";
    }
}