package com.project.quiz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    // 지금은 프로젝트 루트 폴더 옆에 'uploads' 폴더를 자동으로 만듭니다.
	// !중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!
	// 이 서비스는 나중에 서버 연결하면 바꿀 서비스
	// !중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!중요!
    private final Path fileStorageLocation;

    public LocalFileStorageService() {
        // 프로젝트 실행 위치의 'uploads' 폴더 지정
        this.fileStorageLocation = Paths.get(System.getProperty("user.dir") + "/uploads")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("업로드 폴더를 생성할 수 없습니다.", ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        // 파일명 정제 (UUID로 중복 방지)
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String savedFileName = UUID.randomUUID().toString() + extension;

        try {
            // 파일 저장
            Path targetLocation = this.fileStorageLocation.resolve(savedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 접근 가능한 URL 경로 반환 (나중에 WebMvcConfig에서 매핑)
            return "/uploads/" + savedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("파일 저장 실패: " + savedFileName, ex);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        // 로컬에서는 굳이 삭제 로직을 엄격하게 구현 안 해도 됨 (생략 가능)
    }
}