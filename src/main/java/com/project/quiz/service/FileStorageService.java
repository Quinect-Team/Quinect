package com.project.quiz.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    // 파일을 저장하고 접근 가능한 URL(또는 파일명)을 반환
    String storeFile(MultipartFile file);
    
    // 파일 삭제 (프로필 변경 시 기존 파일 삭제용)
    void deleteFile(String fileUrl);
}