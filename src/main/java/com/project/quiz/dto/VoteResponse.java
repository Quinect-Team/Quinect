package com.project.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoteResponse {
    private String type;                      // START, UPDATE, END
    private Long voteId;                      // 투표 고유 ID
    private String question;                  // 투표 질문
    private String description;               // 투표 설명
    private String creator;                   // 투표 생성자
    private Map<String, Integer> results;     // 투표 결과 {AGREE: 5, DISAGREE: 3}
    private Integer duration;
}
