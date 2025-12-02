package com.project.quiz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 브라우저에서 /uploads/** 로 요청하면 -> 로컬의 uploads 폴더를 보여줌
        String uploadPath = "file:///" + System.getProperty("user.dir") + "/uploads/";
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadPath);
    }
}