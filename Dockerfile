# 1단계: Gradle로 앱 빌드
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# 프로젝트 소스 전체 복사
COPY . .
RUN chmod +x ./gradlew

# 스프링부트 JAR 빌드
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행용 이미지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Render에서 내려주는 포트 사용 (기본 8080)
ENV PORT=8080
EXPOSE 8080

# 빌드 결과 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션 실행 (PORT 환경변수로 포트 지정)
CMD ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]
