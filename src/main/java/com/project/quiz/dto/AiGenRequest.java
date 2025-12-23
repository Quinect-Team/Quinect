package com.project.quiz.dto;
import lombok.Data;

@Data
public class AiGenRequest {
    private String topic;
    private String difficulty; // "초급", "중급", "고급"
    private int count;         // 3, 6, 10
    private String type;       // "multiple", "short", "mixed"
}