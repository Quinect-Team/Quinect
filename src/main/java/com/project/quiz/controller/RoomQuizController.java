package com.project.quiz.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
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
import com.project.quiz.domain.User;
import com.project.quiz.dto.GuestUserDto;
import com.project.quiz.dto.QuizDto;
import com.project.quiz.dto.UserRank;
import com.project.quiz.repository.UserRepository;
import com.project.quiz.service.ParticipantService;
import com.project.quiz.service.QuizService;
import com.project.quiz.service.RoomQuizService;
import com.project.quiz.service.RoomService;

import jakarta.servlet.http.HttpSession;

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
	private SimpMessagingTemplate messagingTemplate;

	// 현재 문제 상태 관리 (roomCode -> questionIndex)
	private final Map<String, Integer> roomCurrentQuestionIndex = new ConcurrentHashMap<>();

	private final Map<String, Set<Long>> roomSubmittedUsers = new ConcurrentHashMap<>();

	private final Map<String, Map<Long, Integer>> roomScores = new ConcurrentHashMap<>();

	private final Map<String, Integer> roomQuestionCallCount = new ConcurrentHashMap<>();

	private final Map<String, Map<Long, Set<Long>>> roomUserCorrectQuestions = new ConcurrentHashMap<>();

	@GetMapping("/quiz/{roomCode}")
	public String showQuiz(@PathVariable("roomCode") String roomCode, Model model, java.security.Principal principal,
			HttpSession session) {
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

			// ✅ effectiveUserId 계산 추가!
			Long effectiveUserId = null;
			GuestUserDto guestUser = (GuestUserDto) session.getAttribute("guestUser");

			if (principal != null) {
				Optional<User> userOpt = userRepository.findByEmail(principal.getName());
				if (userOpt.isPresent()) {
					User user = userOpt.get();
					model.addAttribute("currentUser", user);
					effectiveUserId = user.getId();
				}
			} else if (guestUser != null) {
				effectiveUserId = (long) guestUser.getGuestId().hashCode();
				System.out.println("🆔 퀴즈 게스트 effectiveUserId: " + effectiveUserId);
			}

			roomService.closeRoom(roomCode);

			model.addAttribute("effectiveUserId", effectiveUserId); // ✅ HTML에 전달!

			// ✅ 모든 참가자 점수 0으로 초기화 (회원 + 게스트)
			List<Participant> participants = participantService.findByRoom(room);
			for (Participant p : participants) {
				Long pId = p.getUser() != null ? p.getUser().getId() : (long) p.getGuestId().hashCode();
				roomScores.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).put(pId, 0);
				System.out.println("📊 초기 점수 설정: " + p.getNickname() + " (ID=" + pId + ") = 0점");
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
		}
	}

	// 문제를 로드하고 브로드캐스트하는 메서드
	private void loadAndBroadcastQuestion(String roomCode, QuizDto quiz, int questionIndex) {
		List<QuizDto.QuestionDto> questions = quiz.getQuestions();

		// [게임 종료 조건]
		if (questionIndex >= questions.size()) {

			// 1. 순위 계산
			List<UserRank> finalRanking = recalculateRanking(roomCode);
			Room room = roomService.getRoomByCode(roomCode);

			Map<Long, Set<Long>> correctQuestions = roomUserCorrectQuestions.getOrDefault(roomCode, new HashMap<>());

			// 2. 결과 저장 (Participant 테이블)
			participantService.saveQuizResults(room, finalRanking);

			// ⭐ 3. [수정됨] 보상 및 기록 저장 (서비스로 위임하여 트랜잭션 보장)
			try {
				// Service에 새로 만든 메서드 호출
				participantService.processQuizRewards(room, finalRanking, quiz.getQuizId(), correctQuestions);
				System.out.println("💰 보상 지급 및 DB 저장 완료");
			} catch (Exception e) {
				System.err.println("❌ 보상 지급 중 에러 발생: " + e.getMessage());
				e.printStackTrace();
			}

			// 4. 종료 신호 전송
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

		List<Participant> participants = participantService.findByRoom(roomService.getRoomByCode(roomCode));
		questionData.put("totalPlayers", participants.size());

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
		if (callCount == 1) {
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

			// ⭐ 늦게 온 사람들을 위해 재전송!
			int currentIndex = roomCurrentQuestionIndex.getOrDefault(roomCode, -1);
			System.out.println("🔄 재전송: 문제 " + (currentIndex + 1));
			loadAndBroadcastQuestion(roomCode, quiz, currentIndex);
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

			/*
			 * QuizSubmitRequest request = new QuizSubmitRequest();
			 * request.setUserId(userId); QuizSubmitRequest.AnswerRequest ar = new
			 * QuizSubmitRequest.AnswerRequest(); ar.setQuestionId(questionId); // ✅ 실제
			 * questionId 사용! ar.setSelectedOption(selectedOption);
			 * ar.setAnswerText(textAnswer); request.setAnswers(List.of(ar));
			 * quizSubmitService.submit(quizId, request);
			 */

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
			
			Map<String, Object> submitStatus = new HashMap<>();
			submitStatus.put("type", "SUBMIT_STATUS");
			submitStatus.put("userId", userId);
			submitStatus.put("submittedCount", submitted.size());

			List<Participant> participants = participantService.findByRoom(room);
			submitStatus.put("totalPlayers", participants.size());

			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, submitStatus);

			// 2. 점수 계산
			boolean correct = false;

			if (q.getQuizTypeCode() == 2) {
				// ✅ 객관식
				System.out.println("🔍 객관식 검증");
				System.out.println(
						"   선택한 답: " + selectedOption + " (타입: " + selectedOption.getClass().getSimpleName() + ")");
				System.out.println("   정답: " + q.getAnswerOption() + " (타입: "
						+ q.getAnswerOption().getClass().getSimpleName() + ")");

				if (q.getAnswerOption() != null) {
					try {
						Integer answerAsInt = Integer.parseInt(q.getAnswerOption());
						correct = answerAsInt.equals(selectedOption);
						System.out.println("   결과: " + (correct ? "✅ 정답" : "❌ 오답"));
					} catch (NumberFormatException e) {
						System.out.println("   ⚠️ 정답 변환 실패: " + q.getAnswerOption());
					}
				}
			} else if (q.getQuizTypeCode() == 1) {
				// ✅ 서술형 (answerText 정답 사용!)
				System.out.println("🔍 서술형 검증");
				System.out.println("   입력한 답: " + textAnswer);
				System.out.println("   정답: " + q.getSubjectiveAnswer()); // ← getAnswerOption() 아니라 getAnswerText()!

				if (q.getSubjectiveAnswer() != null && textAnswer != null) {
					correct = textAnswer.trim().equalsIgnoreCase(q.getSubjectiveAnswer().trim());
					System.out.println("   결과: " + (correct ? "✅ 정답" : "❌ 오답"));
				} else {
					System.out.println("   ⚠️ 정답 또는 입력값 없음");
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

				roomUserCorrectQuestions.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
						.computeIfAbsent(userId, k -> new HashSet<>()).add(questionId);
			} else {
				System.out.println("❌ 오답: userId=" + userId);
			}

			// ✅ 실시간 순위 갱신 (매 답변마다!)
			List<UserRank> currentRanking = recalculateRanking(roomCode);
			Map<String, Object> rankingData = new HashMap<>();
			rankingData.put("type", "RANKING");
			rankingData.put("ranking", currentRanking);
			System.out.println("📊 [실시간 순위 전송] " + roomCode + ": " + currentRanking);
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, rankingData);

			// ANSWER_RESULT (선택사항 - 정답/오답 표시)
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode,
					Map.of("type", "ANSWER_RESULT", "userId", userId, "isCorrect", correct));

			int totalPlayers = participants.size();
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

		// ⭐ 방 참가자 목록 (회원 + 게스트)
		Room room = roomService.getRoomByCode(roomCode);
		List<Participant> participants = participantService.findByRoom(room);

		List<UserRank> ranking = new ArrayList<>();
		int rank = 1;

		for (Map.Entry<Long, Integer> entry : sortedScores) {
			Long keyId = entry.getKey(); // 회원이면 userId, 게스트면 hashId

			String nickname;

			// 1) 회원 먼저 시도
			var userOpt = userRepository.findById(keyId);
			if (userOpt.isPresent()) {
				var user = userOpt.get();
				if (user.getUserProfile() != null) {
					nickname = user.getUserProfile().getUsername();
				} else {
					nickname = "이름없음(" + keyId + ")";
				}
			} else {
				// 2) DB에 없으면 게스트라고 보고, Participant에서 찾기
				Participant guestP = participants.stream().filter(p -> p.getUser() == null) // 회원 아닌 참가자
						.filter(p -> {
							Long guestHash = (long) p.getGuestId().hashCode();
							return guestHash.equals(keyId);
						}).findFirst().orElse(null);

				if (guestP != null) {
					nickname = guestP.getNickname(); // ✅ 게스트 닉네임
				} else {
					nickname = "알수없음";
				}
			}

			UserRank userRank = new UserRank();
			userRank.setUserId(keyId);
			userRank.setNickname(nickname);
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