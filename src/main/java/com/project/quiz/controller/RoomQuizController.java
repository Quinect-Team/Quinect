package com.project.quiz.controller;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.project.quiz.domain.Room;
import com.project.quiz.dto.QuizDto;
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
	private SimpMessagingTemplate messagingTemplate;

	// 현재 문제 상태 관리 (roomCode -> questionIndex)
	private final Map<String, Integer> roomCurrentQuestionIndex = new ConcurrentHashMap<>();

	@GetMapping("/quiz/{roomCode}")
	public String showQuiz(@PathVariable("roomCode") String roomCode, Model model) {
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
			if (quiz == null) {
				return "redirect:/waitroom/" + roomCode;
			}

			// ✅ Quiz와 Questions가 있는지 확인
			if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
				System.out.println("❌ No questions found!");
				return "redirect:/waitroom/" + roomCode;
			}

			model.addAttribute("roomCode", roomCode);
			model.addAttribute("totalQuestions", quiz.getQuestions().size());

			roomCurrentQuestionIndex.put(roomCode, -1);
			loadAndBroadcastQuestion(roomCode, quiz, 0);

			return "quiz";

		} catch (Exception e) {
			e.printStackTrace(); // ← 에러 로그 출력
			return "redirect:/waitroom/" + roomCode;
		}
	}

	// 문제를 로드하고 브로드캐스트하는 메서드
	private void loadAndBroadcastQuestion(String roomCode, QuizDto quiz, int questionIndex) {
		List<QuizDto.QuestionDto> questions = quiz.getQuestions();

		if (questionIndex >= questions.size()) {
			Map<String, Object> finishSignal = new HashMap<>();
			finishSignal.put("type", "QUIZ_FINISH");
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, finishSignal);
			return;
		}

		QuizDto.QuestionDto question = questions.get(questionIndex);

		Map<String, Object> questionData = new HashMap<>();
		questionData.put("type", "QUESTION");
		questionData.put("questionIndex", questionIndex + 1);
		questionData.put("totalQuestions", questions.size());
		questionData.put("questionId", question.getQuestionId());
		questionData.put("questionText", question.getQuestionText());
		questionData.put("quizTypeCode", question.getQuizTypeCode());
		questionData.put("point", question.getPoint());
		questionData.put("image", question.getImage());

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

		// ✅ 이 부분 로그 추가!
		System.out.println("📢 브로드캐스트 시작 - roomCode: " + roomCode);
		System.out.println("📢 보낼 메시지: " + questionData);
		System.out.println("📢 대상 채널: /topic/quiz/" + roomCode);

		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, questionData);

		System.out.println("✅ 메시지 전송 완료!");
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
	public void submitAnswer(@DestinationVariable("roomCode") String roomCode, Map<String, Object> answerData) {
		Long userId = ((Number) answerData.get("userId")).longValue();
		Long questionId = ((Number) answerData.get("questionId")).longValue();
		String answer = (String) answerData.get("answer");

		System.out.println("\n✅ 답 제출 - userId: " + userId + ", answer: " + answer);

		// 순위 업데이트
		Map<String, Object> rankingUpdate = new HashMap<>();
		rankingUpdate.put("type", "RANKING_UPDATE");
		rankingUpdate.put("ranking", getRealTimeRanking(roomCode));
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, rankingUpdate);

		// ⭐ 2초 후 다음 문제 로드
		new Thread(() -> {
			try {
				Thread.sleep(2000);

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
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private List<Map<String, Object>> getRealTimeRanking(String roomCode) {
		// 현재 순위 반환 (추후 구현)
		return new ArrayList<>();
	}
}
