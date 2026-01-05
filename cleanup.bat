@echo off
chcp 65001 > nul
CLS

ECHO.
ECHO  [INFO] Quinect 데이터 정리 도구 (Data Cleanup Tool)
ECHO.

:: 1. 프로젝트 빌드 (테스트 건너뛰기)
ECHO 1. 프로젝트 빌드 중...
call ./gradlew build -x test

IF %ERRORLEVEL% NEQ 0 (
    ECHO.
    ECHO [오류] 빌드에 실패했습니다.
    PAUSE
    EXIT /B
)

:: 2. JAR 파일 찾기
FOR /F "delims=" %%I IN ('dir /b /s "build\libs\*.jar" ^| findstr /v "plain"') DO SET JAR_PATH=%%I

:: 3. 실행 (환경변수가 필요하다면 아래에 SET 명령어로 추가하세요)
:: 예: SET DB_PASSWORD=1234
SET MAIL_EMAIL=hyeongyumin949@gmail.com
SET MAIL_PASSWORD=jhfgwrvdleioforj
SET GEMINI_API_KEY=AIzaSyARpxR0QrtRBUS1MWs4sw23c6l4d14_Vcc
ECHO.
ECHO 2. 정리 작업 실행...
java -jar "%JAR_PATH%" --cleanup

ECHO.
ECHO [INFO] 작업 완료.
PAUSE