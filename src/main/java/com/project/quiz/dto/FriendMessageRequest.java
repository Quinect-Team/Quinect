package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendMessageRequest {

    /**
     * 받는 사람 ID
     * 예: 1 (Dolmeng_E의 userId)
     */
    private Long recipientId;

    /**
     * 친구 관계 ID (Friendship 테이블의 id)
     * 예: 1 (Alice ↔ Dolmeng_E 관계)
     */
    private Long friendshipId;

    /**
     * 메시지 내용
     * 예: "안녕!"
     */
    private String content;
}