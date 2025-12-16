package com.project.quiz;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExtractCode {
public static void main(String[] args) {
        
        String targetPath = "src/main/java/com/project/quiz"; 

        System.out.println(">>> 코드 추출을 시작합니다...");
        
        // 프로젝트 루트 경로 (자동 감지)
        String projectRoot = System.getProperty("user.dir");
        Path sourceDir = Paths.get(projectRoot, targetPath);
        File outputFile = new File("all_classes_code.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            
            if (!Files.exists(sourceDir)) {
                System.err.println("오류: 경로를 찾을 수 없습니다 -> " + sourceDir.toAbsolutePath());
                return;
            }

            List<Path> javaFiles;
            try (Stream<Path> paths = Files.walk(sourceDir)) {
                javaFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .sorted()
                        .collect(Collectors.toList());
            }

            System.out.println(">>> 총 " + javaFiles.size() + "개의 자바 파일을 발견했습니다.");

            int count = 1;
            for (Path file : javaFiles) {
                String fileName = file.getFileName().toString();
                String content = Files.readString(file);

                // 요청하신 포맷
                writer.write(count + ". " + fileName + "=\"\n");
                writer.write(content);
                writer.write("\n\",\n\n"); 
                
                System.out.println("처리 중... " + fileName);
                count++;
            }

            System.out.println("------------------------------------------------");
            System.out.println("성공! 파일이 생성되었습니다: " + outputFile.getAbsolutePath());
            System.out.println("프로젝트 폴더에서 'all_classes_code.txt' 파일을 확인하세요.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
