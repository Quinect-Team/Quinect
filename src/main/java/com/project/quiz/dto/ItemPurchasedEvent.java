package com.project.quiz.dto;

import com.project.quiz.domain.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ItemPurchasedEvent {
    private User user;
}