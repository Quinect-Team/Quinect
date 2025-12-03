package com.project.quiz.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestUserDto {
	private String guestId;
	private String nickname;
	private String characterImageUrl; // 캐릭터 이미지 경로/URL

	// 빌더나 생성자에서 ID 자동 생성하고 싶으면 추가
	public GuestUserDto(String nickname, String characterImageUrl) {
		this.nickname = nickname;
		this.characterImageUrl = characterImageUrl;
		this.guestId = generateGuestId(nickname);
	}

	// 게스트 ID 생성 로직
	private String generateGuestId(String nickname) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMddHHmm");
		String datetime = LocalDateTime.now().format(formatter);
		return nickname + datetime;
	}
}
