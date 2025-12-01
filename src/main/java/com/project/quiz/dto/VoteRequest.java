package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteRequest {
    private String type;           // START, VOTE, UPDATE
    private Long voteId;           // 투표 고유 ID
    private String question;       // 투표 질문
    private String description;    // 투표 설명
    private String creator;        // 투표 생성자
    private String voter;          // 투표자
    private String choice;         // AGREE, DISAGREE
    private Long timestamp;        // 타임스탐프
    private Integer duration;

}
