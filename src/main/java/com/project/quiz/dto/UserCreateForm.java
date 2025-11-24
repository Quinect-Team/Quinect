package com.project.quiz.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateForm {
    private String username;
    private String email;
    private String password;
}