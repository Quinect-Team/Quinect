package com.project.quiz.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestUserDto {
    private String nickname;
    private String characterImageUrl; // 캐릭터 이미지 경로/URL
}
