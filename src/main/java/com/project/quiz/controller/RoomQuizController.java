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

import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.QuizOption;
import com.project.quiz.domain.QuizQuestion;
import com.project.quiz.domain.Room;
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
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null) {
			return "redirect:/waitroom/" + roomCode;
		}

		Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
		if (quizId == null) {
			return "redirect:/waitroom/" + roomCode;
		}

		Quiz quiz = quizService.findById(quizId);
		if (quiz == null) {
			return "redirect:/waitroom/" + roomCode;
		}

		model.addAttribute("roomCode", roomCode);
		model.addAttribute("quiz", quiz);
		model.addAttribute("totalQuestions", quiz.getQuestions().size());

		// 👇 첫 번째 문제 브로드캐스트
		roomCurrentQuestionIndex.put(roomCode, 0);
		loadAndBroadcastQuestion(roomCode, quiz, 0);

		return "quiz";
	}

	// 문제를 로드하고 브로드캐스트하는 메서드
	private void loadAndBroadcastQuestion(String roomCode, Quiz quiz, int questionIndex) {
		List<QuizQuestion> questions = quiz.getQuestions();

		if (questionIndex >= questions.size()) {
			// 모든 문제 완료 → 결과 화면으로
			Map<String, Object> finishSignal = new HashMap<>();
			finishSignal.put("type", "QUIZ_FINISH");
			messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, finishSignal);
			return;
		}

		QuizQuestion question = questions.get(questionIndex);

		Map<String, Object> questionData = new HashMap<>();
		questionData.put("type", "QUESTION");
		questionData.put("questionIndex", questionIndex + 1); // 1부터 시작
		questionData.put("totalQuestions", questions.size());
		questionData.put("questionId", question.getQuestionId());
		questionData.put("questionText", question.getQuestionText());
		questionData.put("quizTypeCode", question.getQuizTypeCode());
		questionData.put("point", question.getPoint());
		questionData.put("image", question.getImage());

		// 객관식이면 선택지도 포함 (quizTypeCode = 1)
		if (question.getQuizTypeCode() == 1) {
			List<Map<String, Object>> options = new ArrayList<>();
			for (QuizOption option : question.getOptions()) {
				Map<String, Object> optionMap = new HashMap<>();
				optionMap.put("optionId", option.getOptionId());
				optionMap.put("optionNumber", option.getOptionNumber());
				optionMap.put("optionText", option.getOptionText());
				options.add(optionMap);
			}
			questionData.put("options", options);
		}

		System.out.println("📢 브로드캐스트 - " + roomCode + " 문제 #" + (questionIndex + 1));
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, questionData);
	}

	// 다음 문제 로드
	@MessageMapping("/quiz/next-question/{roomCode}")
	public void nextQuestion(@DestinationVariable("roomCode") String roomCode) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null)
			return;

		Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
		Quiz quiz = quizService.findById(quizId);
		if (quiz == null)
			return;

		int currentIndex = roomCurrentQuestionIndex.getOrDefault(roomCode, 0);
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

		System.out.println("✅ 답 제출 - userId: " + userId + ", questionId: " + questionId + ", answer: " + answer);

		// QuizAnswer 저장 (추후 구현)
		// quizAnswerService.recordAnswer(userId, questionId, answer);

		// 실시간 순위 브로드캐스트
		Map<String, Object> rankingUpdate = new HashMap<>();
		rankingUpdate.put("type", "RANKING_UPDATE");
		rankingUpdate.put("ranking", getRealTimeRanking(roomCode));
		messagingTemplate.convertAndSend("/topic/quiz/" + roomCode, rankingUpdate);
	}

	private List<Map<String, Object>> getRealTimeRanking(String roomCode) {
		// 현재 순위 반환 (추후 구현)
		return new ArrayList<>();
	}
}
