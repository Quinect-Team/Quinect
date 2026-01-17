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

import com.project.quiz.domain.Participant;
import com.project.quiz.domain.Room;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.dto.QuizSubmitRequest;
import com.project.quiz.dto.UserRank;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.ParticipantService;
import com.project.quiz.service.QuizService;
import com.project.quiz.service.QuizSubmitService;
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
	private ParticipantService participantService;

	@Autowired
	private QuizSubmitService quizSubmitService;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	// 현재 문제 상태 관리 (roomCode -> questionIndex)
	private final Map<String, Integer> roomCurrentQuestionIndex = new ConcurrentHashMap<>();

	private final Map<String, Set<Long>> roomSubmittedUsers = new ConcurrentHashMap<>();

	private final Map<String, Map<Long, Integer>> roomScores = new ConcurrentHashMap<>();

	private final Map<String, Integer> roomQuestionCallCount = new ConcurrentHashMap<>();

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
			if (!roomCurrentQuestionIndex.containsKey(roomCode)) {
				roomCurrentQuestionIndex.put(roomCode, -1);
			}

			if (!roomSubmittedUsers.containsKey(roomCode)) {
				roomSubmittedUsers.put(roomCode, Collections.synchronizedSet(new HashSet<>()));
			}

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

		// ⭐ 모든 문제를 다 풀었을 때
		if (questionIndex >= questions.size()) {

			// ✅ 최종 순위 계산
			List<UserRank> finalRanking = recalculateRanking(roomCode);

			// ✅ 방 조회
			Room room = roomService.getRoomByCode(roomCode);

			// ✅ DB에 저장
			participantService.saveQuizResults(room, finalRanking);

			// 클라이언트에 FINISH 신호 전송
			Map<String, Object> finishSignal = new HashMap<>();
			finishSignal.put("type", "FINISH");
			finishSignal.put("ranking", finalRanking);

			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, finishSignal);

			return;
		}

		// ⭐ 나머지 기존 코드 (변경 없음)
		QuizDto.QuestionDto question = questions.get(questionIndex);

		Map<String, Object> questionData = new HashMap<>();
		questionData.put("type", "QUESTION");
		questionData.put("questionNumber", questionIndex + 1);
		questionData.put("totalQuestions", questions.size());
		questionData.put("questionId", question.getQuestionId());
		questionData.put("questionText", question.getQuestionText());
		questionData.put("quizTypeCode", question.getQuizTypeCode());
		questionData.put("point", question.getPoint());
		questionData.put("imageUrl", question.getImage());

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

		System.out.println("📢 [문제 전송] " + roomCode + " / " + (questionIndex + 1) + "번 문제");
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, questionData);

		List<UserRank> currentRanking = recalculateRanking(roomCode);

		Map<String, Object> rankingData = new HashMap<>();
		rankingData.put("type", "RANKING");
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

		// ✅ 방에 참여한 실제 참가자 수
		List<Participant> participants = participantService.findByRoom(room);
		int totalPlayers = participants.size();

		// ✅ 호출 횟수 카운트
		int callCount = roomQuestionCallCount.getOrDefault(roomCode, 0) + 1;
		roomQuestionCallCount.put(roomCode, callCount);

		System.out.println("🔔 nextQuestion 호출: " + callCount + "/" + totalPlayers);

		// ✅ 첫 번째 호출일 때만 문제 로드!
		if (callCount == 1) { // ← 이렇게 간단히!
			int currentIndex = roomCurrentQuestionIndex.getOrDefault(roomCode, -1);
			int nextIndex = currentIndex + 1;

			System.out.println("✅ 문제 로드: nextIndex=" + nextIndex);

			roomCurrentQuestionIndex.put(roomCode, nextIndex);
			loadAndBroadcastQuestion(roomCode, quiz, nextIndex);
		} else {
			System.out.println("⏭️ 아직 대기 중... (" + callCount + "/" + totalPlayers + ")");
		}

		// ✅ 모든 참가자가 호출했으면 초기화
		if (callCount >= totalPlayers) {
			roomQuestionCallCount.put(roomCode, 0);
			System.out.println("🔄 카운트 초기화!");
		}
	}

	// 답 제출
	@MessageMapping("/quiz/answer/{roomCode}")
	public void submitAnswer(@DestinationVariable("roomCode") String roomCode, Map<String, Object> data) {
		try {
			Long userId = Long.valueOf(data.get("userId").toString());
			Integer selectedOption = (Integer) data.get("selectedOption");
			String textAnswer = (String) data.get("textAnswer");

			// ⭐ 1. 현재 퀴즈 + 문제 정보 정확히 가져오기
			Room room = roomService.getRoomByCode(roomCode);
			Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
			QuizDto quiz = quizService.getQuizForPlay(quizId); // 전체 퀴즈 로드
			int questionIndex = roomCurrentQuestionIndex.get(roomCode);
			Long questionId = quiz.getQuestions().get(questionIndex).getQuestionId(); // ✅ 실제 questionId!

			// ⭐ 2. QuizSubmitService 호출
			QuizSubmitRequest request = new QuizSubmitRequest();
			request.setUserId(userId);
			QuizSubmitRequest.AnswerRequest ar = new QuizSubmitRequest.AnswerRequest();
			ar.setQuestionId(questionId); // ✅ 실제 questionId 사용!
			ar.setSelectedOption(selectedOption);
			ar.setAnswerText(textAnswer);
			request.setAnswers(List.of(ar));
			quizSubmitService.submit(quizId, request);

			System.out.println("✅ DB 저장: questionId=" + questionId);

			System.out.println("📤 답변 수신: userId=" + userId + ", option=" + selectedOption + ", text=" + textAnswer);

			// 기존 코드 그대로 유지 (점수 계산, 정답/오답 등)
			int index = roomCurrentQuestionIndex.get(roomCode);

			QuizDto.QuestionDto q = quiz.getQuestions().get(index);

			System.out
					.println("📌 현재 문제: index=" + index + ", type=" + q.getQuizTypeCode() + ", point=" + q.getPoint());

			// 1. 제출자 목록 관리
			Set<Long> submitted = roomSubmittedUsers.computeIfAbsent(roomCode,
					k -> Collections.synchronizedSet(new HashSet<>()));
			if (submitted.contains(userId)) {
				System.out.println("⚠️ 중복 제출 감지: userId=" + userId);
				return;
			}
			submitted.add(userId);

			// 2. 점수 계산
			boolean correct = false;

			if (q.getQuizTypeCode() == 2) {
				// 객관식
				System.out.println("🔍 객관식 검증");
				System.out.println(
						"   선택한 답: " + selectedOption + " (타입: " + selectedOption.getClass().getSimpleName() + ")");
				System.out.println("   정답: " + q.getAnswerOption() + " (타입: "
						+ q.getAnswerOption().getClass().getSimpleName() + ")");

				if (q.getAnswerOption() != null) {
					// ✅ String을 Integer로 변환해서 비교
					try {
						Integer answerAsInt = Integer.parseInt(q.getAnswerOption());
						correct = answerAsInt.equals(selectedOption);
						System.out.println("   결과: " + (correct ? "✅ 정답" : "❌ 오답"));
					} catch (NumberFormatException e) {
						System.out.println("   ⚠️ 정답 변환 실패: " + q.getAnswerOption());
					}
				} else {
					System.out.println("   ⚠️ 정답이 없습니다!");
				}
			}

			// 3. 정답이면 점수 추가
			if (correct) {
				Integer points = q.getPoint() != null ? q.getPoint() : 0;
				System.out.println("✅ 정답! userId=" + userId + " 에게 " + points + " 점 추가");

				roomScores.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).merge(userId, points,
						Integer::sum);

				Integer currentScore = roomScores.get(roomCode).get(userId);
				System.out.println("📊 누적 점수: userId=" + userId + ", score=" + currentScore);
			} else {
				System.out.println("❌ 오답: userId=" + userId);
			}

			// 나머지 기존 코드...
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, Map.of("type", "ANSWER_RESULT", "userId",
					userId, "isCorrect", correct, "ranking", recalculateRanking(roomCode)));

			int totalPlayers = roomScores.getOrDefault(roomCode, new HashMap<>()).size();
			int submittedCount = submitted.size();

			System.out.println("📊 제출 현황 [" + roomCode + "]: " + submittedCount + "/" + totalPlayers);

			if (submittedCount >= totalPlayers) {
				System.out.println("✅ 모든 인원 제출 완료! 다음 문제로 이동합니다.");

				submitted.clear();
				int next = index + 1;
				roomCurrentQuestionIndex.put(roomCode, next);

				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						loadAndBroadcastQuestion(roomCode, quiz, next);
					}
				}, 2000);
			} else {
				System.out.println("⏳ 다른 유저의 제출을 기다리는 중...");
			}

		} catch (Exception e) {
			System.out.println("❌ 답변 처리 중 오류");
			e.printStackTrace();
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
		try {
			// 1. 방 조회
			Room room = roomService.getRoomByCode(roomCode);
			if (room == null) {

				return "redirect:/";
			}

			// 2. 퀴즈 정보 조회 ✅ 추가
			Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
			QuizDto quiz = null;
			String quizTitle = "퀴즈";

			if (quizId != null) {
				quiz = quizService.getQuizForPlay(quizId);
				if (quiz != null) {
					quizTitle = quiz.getTitle();
				}
			}

			// 3. DB에서 저장된 참가자들 조회
			List<Participant> participants = participantService.findByRoom(room);

			// 4. ranking 순서대로 정렬
			participants.sort((a, b) -> {
				if (a.getRanking() == null || b.getRanking() == null)
					return 0;
				return a.getRanking().compareTo(b.getRanking());
			});

			// 5. 모델에 담기
			model.addAttribute("roomCode", roomCode);
			model.addAttribute("quizTitle", quizTitle); // ✅ 추가
			model.addAttribute("ranking", participants);

			return "quizresult";

		} catch (Exception e) {

			return "redirect:/";
		}
	}

}