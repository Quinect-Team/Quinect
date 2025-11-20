package com.project.quiz.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileController {
	
	@GetMapping("/profile")
    public String profilePage() {
        return "profile";
    }
	
	@GetMapping("/profile/settings")
    public String settingsPage() {
        return "profilesettings";
	}
}
