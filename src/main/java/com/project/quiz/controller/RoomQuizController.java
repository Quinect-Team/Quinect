package com.project.quiz.controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.dto.UserRank;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.QuizService;
import com.project.quiz.service.RoomQuizService;
import com.project.quiz.service.RoomService;

@Controller
public class RoomQuizController {

	@Autowired
	private RoomService roomService;

	@Autowired
	private RoomQuizService roomQuizService;

	@Autowired
	private QuizService quizService;

	@Autowired
	private UserRepository userRepository; // UserProfileRepository 대신 사용 권장

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	// 현재 문제 상태 관리 (roomCode -> questionIndex)
	private final Map<String, Integer> roomCurrentQuestionIndex = new ConcurrentHashMap<>();

	private final Map<String, Set<Long>> roomSubmittedUsers = new ConcurrentHashMap<>();

	private final Map<String, Map<Long, Integer>> roomScores = new ConcurrentHashMap<>();

	@GetMapping("/quiz/{roomCode}")
	public String showQuiz(@PathVariable("roomCode") String roomCode, Model model, java.security.Principal principal) {
		try {
			Room room = roomService.getRoomByCode(roomCode);
			if (room == null) {
				return "redirect:/waitroom/" + roomCode;
			}

			Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
			if (quizId == null) {
				return "redirect:/waitroom/" + roomCode;
			}

			QuizDto quiz = quizService.getQuizForPlay(quizId);
			if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
				return "redirect:/waitroom/" + roomCode;
			}

			// 1. 현재 사용자 닉네임을 순위표에 미리 등록 (0점)
			if (principal != null) {
				userRepository.findByEmail(principal.getName()).ifPresent(user -> {
					model.addAttribute("currentUser", user);
					// 방별 점수판에 유저 등록 (없으면 생성)
					roomScores.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).putIfAbsent(user.getId(), 0);
				});
			}

			model.addAttribute("roomCode", roomCode);
			model.addAttribute("totalQuestions", quiz.getQuestions().size());

			// 2. 상태 초기화
			roomCurrentQuestionIndex.put(roomCode, -1);
			roomSubmittedUsers.put(roomCode, Collections.synchronizedSet(new HashSet<>()));

			// 첫 문제 로드
			loadAndBroadcastQuestion(roomCode, quiz, 0);

			return "quiz";

		} catch (Exception e) {
			System.err.println("❌ 퀴즈 진입 에러: " + e.getMessage());
			e.printStackTrace();
			return "redirect:/waitroom/" + roomCode;
		} // <-- 여기서 try-catch가 정확히 닫혀야 합니다.
	}

	// 문제를 로드하고 브로드캐스트하는 메서드
	private void loadAndBroadcastQuestion(String roomCode, QuizDto quiz, int questionIndex) {
		List<QuizDto.QuestionDto> questions = quiz.getQuestions();

		// 1. 모든 문제를 다 풀었을 때 처리
		if (questionIndex >= questions.size()) {
			Map<String, Object> finishSignal = new HashMap<>();
			finishSignal.put("type", "FINISH"); // JS에서 'FINISH'를 기다림 (QUIZ_FINISH -> FINISH로 통일 추천)
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, finishSignal);
			return;
		}

		QuizDto.QuestionDto question = questions.get(questionIndex);

		// 2. 문제 데이터 구성
		Map<String, Object> questionData = new HashMap<>();
		questionData.put("type", "QUESTION");
		questionData.put("questionNumber", questionIndex + 1); // JS는 questionNumber를 씀
		questionData.put("totalQuestions", questions.size());
		questionData.put("questionId", question.getQuestionId());
		questionData.put("questionText", question.getQuestionText());
		questionData.put("quizTypeCode", question.getQuizTypeCode());
		questionData.put("point", question.getPoint());
		questionData.put("imageUrl", question.getImage()); // JS는 imageUrl을 씀 (필드명 확인 필요)

		if (question.getQuizTypeCode() == 2) {
			List<Map<String, Object>> options = new ArrayList<>();
			for (QuizDto.OptionDto option : question.getOptions()) {
				Map<String, Object> optionMap = new HashMap<>();
				optionMap.put("optionNumber", option.getOptionNumber());
				optionMap.put("optionText", option.getOptionText());
				options.add(optionMap);
			}
			questionData.put("options", options);
		}

		// 3. 문제 전송 (QUESTION)
		System.out.println("📢 [문제 전송] " + roomCode + " / " + (questionIndex + 1) + "번 문제");
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, questionData);

		// ==========================================
		// ⭐ [추가됨] 4. 순위 데이터 별도 전송 (RANKING)
		// ==========================================
		List<UserRank> currentRanking = recalculateRanking(roomCode);

		Map<String, Object> rankingData = new HashMap<>();
		rankingData.put("type", "RANKING"); // JS가 기다리는 타입
		rankingData.put("ranking", currentRanking);

		System.out.println("📢 [순위 전송] 데이터: " + currentRanking);
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, rankingData);
	}

	// 다음 문제 로드
	@MessageMapping("/quiz/next-question/{roomCode}")
	public void nextQuestion(@DestinationVariable("roomCode") String roomCode) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null)
			return;

		Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
		QuizDto quiz = quizService.getQuizForPlay(quizId);
		if (quiz == null)
			return;

		int currentIndex = roomCurrentQuestionIndex.getOrDefault(roomCode, -1);
		int nextIndex = currentIndex + 1;

		roomCurrentQuestionIndex.put(roomCode, nextIndex);
		loadAndBroadcastQuestion(roomCode, quiz, nextIndex);
	}

	// 답 제출
	@MessageMapping("/quiz/answer/{roomCode}")
	public void submitAnswer(@DestinationVariable("roomCode") String roomCode, Map<String, Object> data) {
		Long userId = Long.valueOf(data.get("userId").toString());
		Integer selectedOption = (Integer) data.get("selectedOption");
		String textAnswer = (String) data.get("textAnswer");

		int index = roomCurrentQuestionIndex.get(roomCode);
		Room room = roomService.getRoomByCode(roomCode);
		Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
		QuizDto quiz = quizService.getQuizForPlay(quizId);
		QuizDto.QuestionDto q = quiz.getQuestions().get(index);

		// 1. 제출자 목록 관리
		Set<Long> submitted = roomSubmittedUsers.computeIfAbsent(roomCode,
				k -> Collections.synchronizedSet(new HashSet<>()));
		if (submitted.contains(userId))
			return; // 이미 제출한 유저면 무시
		submitted.add(userId);

		// 2. 점수 계산 로직 (기존과 동일)
		boolean correct = false;
		if (q.getQuizTypeCode() == 2 && q.getAnswerOption() != null && q.getAnswerOption().equals(selectedOption))
			correct = true;
		if (q.getQuizTypeCode() == 1 && q.getSubjectiveAnswer() != null
				&& q.getSubjectiveAnswer().equalsIgnoreCase(textAnswer))
			correct = true;

		if (correct) {
			roomScores.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).merge(userId, q.getPoint(),
					Integer::sum);
		}

		// 3. 개별 결과 전송 (본인 혹은 전체에게 현재 제출 현황 알림)
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, Map.of("type", "ANSWER_RESULT", "userId", userId,
				"isCorrect", correct, "ranking", recalculateRanking(roomCode)));

		// ============================================================
		// ⭐ [핵심] 모든 유저가 제출했는지 확인
		// ============================================================
		// 방법: 현재 roomScores에 등록된 유저 수(방에 들어온 유저 수)와 제출자 수 비교
		int totalPlayers = roomScores.getOrDefault(roomCode, new HashMap<>()).size();
		int submittedCount = submitted.size();

		System.out.println("📊 제출 현황 [" + roomCode + "]: " + submittedCount + "/" + totalPlayers);

		if (submittedCount >= totalPlayers) {
			// 모든 유저가 제출했을 때만 다음 문제로!
			System.out.println("✅ 모든 인원 제출 완료! 다음 문제로 이동합니다.");

			submitted.clear(); // 제출자 목록 초기화
			int next = index + 1;
			roomCurrentQuestionIndex.put(roomCode, next);

			// 약간의 지연 시간(예: 2초)을 주면 유저들이 결과 창을 볼 시간을 가질 수 있어 더 좋습니다.
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					loadAndBroadcastQuestion(roomCode, quiz, next);
				}
			}, 2000);
		} else {
			System.out.println("⏳ 다른 유저의 제출을 기다리는 중...");
		}
	}

	// ⭐ 이 메서드를 handleAnswerStatus() 아래에 추가!
	private List<UserRank> recalculateRanking(String roomCode) {
		Map<Long, Integer> scores = roomScores.get(roomCode);
		if (scores == null || scores.isEmpty())
			return new ArrayList<>();

		List<Map.Entry<Long, Integer>> sortedScores = scores.entrySet().stream()
				.sorted((a, b) -> b.getValue().compareTo(a.getValue())).collect(Collectors.toList());

		List<UserRank> ranking = new ArrayList<>();
		int rank = 1;

		for (Map.Entry<Long, Integer> entry : sortedScores) {
			Long userId = entry.getKey();

			// ⭐ User 엔티티를 찾으면 연관된 UserProfile도 자동으로 따라옵니다.
			String realNickname = userRepository.findById(userId).map(user -> {
				if (user.getUserProfile() != null) {
					return user.getUserProfile().getUsername(); // 프로필의 진짜 이름
				}
				return "이름없음(" + userId + ")";
			}).orElse("알수없음");

			UserRank userRank = new UserRank();
			userRank.setUserId(userId);
			userRank.setNickname(realNickname);
			userRank.setScore(entry.getValue());
			userRank.setRank(rank++);
			ranking.add(userRank);
		}
		return ranking;
	}

	@GetMapping("/quiz-result/{roomCode}")
	public String showResult(@PathVariable("roomCode") String roomCode, Model model) {
		// 1. 해당 방의 최종 순위 리스트를 가져옵니다.
		List<UserRank> finalRanking = recalculateRanking(roomCode);

		// 2. 점수가 높은 순서대로 정렬합니다.
		finalRanking.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

		// 3. HTML(Thymeleaf)에서 쓸 수 있도록 모델에 담습니다.
		model.addAttribute("roomCode", roomCode);
		model.addAttribute("ranking", finalRanking);

		// 4. "quizresult.html" 파일을 찾아가라고 명령합니다.
		return "quizresult";
	}

}