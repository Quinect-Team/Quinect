package com.project.quiz.controller;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import com.project.quiz.config.QRCodeService;
import com.project.quiz.domain.Quiz;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.GuestUserDto;
import com.project.quiz.dto.VoteRequest;
import com.project.quiz.dto.VoteResponse;
import com.project.quiz.service.ParticipantService;
import com.project.quiz.service.QuizService;
import com.project.quiz.service.RoomQuizService;
import com.project.quiz.service.RoomService;
import com.project.quiz.service.UserService;
import com.project.quiz.service.VoteManager;

import jakarta.servlet.http.HttpSession;

@Controller
public class RoomController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private QRCodeService qrCodeService;

	@Autowired
	private UserService userService;

	@Autowired
	private QuizService quizService; // ✅ QuizService 주입

	@Autowired
	private RoomQuizService roomQuizService; // ✅ 이 줄 추가

	@Autowired
	private ParticipantService participantService;

	@Autowired
	private VoteManager voteManager;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	private final Map<String, Map<Long, Boolean>> roomReadyStatus = new ConcurrentHashMap<>();

	@Value("${BASE_URL}")
	private String baseUrl;

	@GetMapping("/waitroom/create")
	public String createRoomPost(Principal principal, HttpSession session) {
		if (principal == null) {
			return "redirect:/login";
		}

		// 현재 로그인한 유저 정보에서 ID 가져오기
		User user = userService.findByEmail(principal.getName());
		if (user == null) {
			return "redirect:/login";
		}

		// user.getId()를 호스트유저아이디로 사용
		Room room = roomService.createRoom(user.getId(), "opened");

		if (principal == null && session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/" + roomCode;
		}
	}

	@GetMapping("/waitroom/{roomCode}")
	public String showRoomByCode(@PathVariable("roomCode") String roomCode, Model model, Principal principal,
			HttpSession session) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null || "CLOSED".equals(room.getStatusCode())) {
			return "room-error"; // 방 없음 처리 페이지
		}

		User user = null;
		String guestId = null;
		String nickname = null;
		String avatarUrl = null;

		GuestUserDto guestUser = (GuestUserDto) session.getAttribute("guestUser");

		if (guestUser != null) {
			guestId = guestUser.getGuestId();
			nickname = guestUser.getNickname();
			avatarUrl = guestUser.getCharacterImageUrl();
			System.out.println("방 참가: guestId=" + guestId + ", nickname=" + nickname + ", avatarUrl=" + avatarUrl);
		}

		if (principal != null) {
			user = userService.findByEmail(principal.getName());
		}

		if (user != null) {
			UserProfile userProfile = user.getUserProfile();
			if (userProfile != null) {
				nickname = userProfile.getUsername(); // UserProfile의 username 사용
				avatarUrl = userProfile.getProfileImage(); // UserProfile의 profileImage 사용
				System.out.println("로그인 사용자 프로필: username=" + nickname + ", profileImage=" + avatarUrl);
			} else {
				// UserProfile이 없으면 email 사용
				nickname = user.getEmail();
				avatarUrl = null;
				System.out.println("UserProfile이 없음, email 사용: " + nickname);
			}
		}

		Long effectiveUserId = null;
		if (principal != null && user != null) {
			effectiveUserId = user.getId(); // 회원: DB ID (예: 2)
		} else if (guestUser != null) {
			effectiveUserId = (long) guestUser.getGuestId().hashCode(); // 게스트: 해시 (511719744)
			System.out.println("🆔 게스트 effectiveUserId: " + effectiveUserId);
		}

		participantService.joinRoomIfNotExists(room, user, guestId, nickname, avatarUrl);

		List<?> participants = participantService.findByRoom(room);
		Map<String, Object> message = new HashMap<>();
		message.put("type", "PARTICIPANTUPDATE");
		message.put("participants", participants);
		messagingTemplate.convertAndSend("/topic/participants/" + roomCode, message);

		model.addAttribute("room", room);
		model.addAttribute("participants", participantService.findByRoom(room));
		model.addAttribute("guestNickname", nickname);
		model.addAttribute("guestAvatarUrl", avatarUrl);
		model.addAttribute("currentUser", user);
		model.addAttribute("guestId", guestId);
		model.addAttribute("effectiveUserId", effectiveUserId); // ✅ 이 줄 추가!

		boolean isRoomMaster = (user != null && room.getHostUserId().equals(user.getId()));
		model.addAttribute("isRoomMaster", isRoomMaster);

		try {
			String roomUrl = baseUrl + "/guest/setup?next=/waitroom/" + roomCode;

			byte[] qrImage = qrCodeService.generateQRCodeImage(roomUrl, 250, 250);

			@SuppressWarnings("deprecation")
			String qrCodeBase64 = Base64.encodeBase64String(qrImage);

			model.addAttribute("qrCodeBase64", qrCodeBase64);

		} catch (Exception e) {
			model.addAttribute("qrCodeBase64", null);
		}

		List<Quiz> quizzes = quizService.findAll();
		model.addAttribute("quizzes", quizzes);

		return "waitroom";
	}

	@GetMapping("/joinroom")
	public String joinRoom(Principal principal, HttpSession session) {
		if (principal == null && session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/joinroom";
		}
		return "joinroom";
	}

	@PostMapping("/waitroom/join")
	public String joinRoom(@RequestParam("roomCode") String roomCode, Principal principal, HttpSession session) {
		if (principal != null) {
			return "redirect:/waitroom/" + roomCode;
		}

		if (session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/" + roomCode;
		}

		return "redirect:/waitroom/" + roomCode;
	}

	// ==================== WebSocket: 투표 시작 ====================

	@MessageMapping("/vote/start/{roomCode}")
	@SendTo("/topic/vote/{roomCode}")
	public VoteResponse startVote(@DestinationVariable("roomCode") String roomCode, VoteRequest request) {

		// duration 기본값/최대값 처리
		int duration = (request.getDuration() != null) ? request.getDuration() : 30;
		if (duration > 300)
			duration = 300; // 최대 5분

		// VoteManager에 실제 투표 시작 위임 (타이머 포함)
		voteManager.startVote(roomCode, request.getVoteId(), request.getQuestion(), request.getDescription(),
				request.getCreator(), duration);

		// 클라이언트들에게 브로드캐스트할 정보
		return VoteResponse.builder().type("START").voteId(request.getVoteId()).question(request.getQuestion())
				.description(request.getDescription()).creator(request.getCreator()).duration(duration).build();
	}

	// ==================== WebSocket: 투표 제출 ====================

	@MessageMapping("/vote/submit/{roomCode}")
	public void submitVote(@DestinationVariable("roomCode") String roomCode, VoteRequest request) {
		// 여기서는 반환값 없이, VoteManager가 내부에서 UPDATE 브로드캐스트
		voteManager.submitVote(roomCode, request.getVoteId(), request.getVoter(), request.getChoice());
	}

	@PostMapping("/waitroom/{roomCode}/close")
	@ResponseBody
	public String closeRoom(@PathVariable("roomCode") String roomCode, Principal principal) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null) {
			return "NOT_FOUND";
		}

		if (principal == null) {
			return "UNAUTHORIZED";
		}

		User user = userService.findByEmail(principal.getName());
		if (user == null || !room.getHostUserId().equals(user.getId())) {
			return "FORBIDDEN";
		}

		roomService.closeRoom(roomCode);
		return "OK";
	}

	@MessageMapping("/ready/{roomCode}")
	@SendTo("/topic/ready/{roomCode}")
	public Map<String, Object> handleReadyStatus(@DestinationVariable("roomCode") String roomCode,
			Map<String, Object> readyData) {

		Long userId = ((Number) readyData.get("userId")).longValue();
		boolean isReady = (Boolean) readyData.get("isReady");

		roomReadyStatus.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>()).put(userId, isReady);

		System.out
				.println("Ready status received from room: " + roomCode + ", user: " + userId + ", ready: " + isReady);

		// 3. 모든 참가자 READY인지 체크
		Room room = roomService.getRoomByCode(roomCode);
		if (room != null) {
			var participants = participantService.findByRoom(room);
			var readyMap = roomReadyStatus.get(roomCode);

			boolean allReady = true;

			// ✅ 인라인으로 처리 (엔티티 수정 없음!)
			for (var p : participants) {
				// ✅ 게스트와 회원 모두 처리
				Long pId = (p.getUser() != null) ? p.getUser().getId() : Long.valueOf(p.getGuestId().hashCode());

				System.out.println("참가자: " + p.getNickname() + ", pId: " + pId + ", ready: " + readyMap.get(pId));

				if (!Boolean.TRUE.equals(readyMap.get(pId))) {
					allReady = false;
					break;
				}
			}

			System.out.println("Room " + roomCode + " allReady: " + allReady);

			// 모두 READY면 퀴즈 시작
			if (allReady) {
				Long quizId = roomQuizService.getLatestQuizIdByRoom(room.getId());
				if (quizId != null) {
					Map<String, Object> startSignal = new HashMap<>();
					startSignal.put("type", "QUIZ_START");
					startSignal.put("quizId", quizId);
					startSignal.put("countdown", 5);

					System.out.println("🚀 QUIZ_START 신호 전송: " + roomCode);
					messagingTemplate.convertAndSend("/topic/ready/" + roomCode, startSignal);
				} else {
					System.out.println("❌ 퀴즈가 선택되지 않았습니다");
				}
			}
		}

		return readyData;
	}

	// ✅ getParticipantId() 메서드 삭제!

	/**
	 * 친구를 대기방으로 초대 (email 사용)
	 */
	@PostMapping("/api/quiz/room/{roomCode}/invite") // ⭐ 경로 수정
	@ResponseBody
	public Map<String, Object> inviteFriendToRoom(@PathVariable("roomCode") String roomCode,
			@RequestParam("friendEmail") String friendEmail // ⭐ email로 변경
	) {
		Map<String, Object> response = new HashMap<>();

		try {
			// 1. 대기방 조회
			Room room = roomService.getRoomByCode(roomCode);
			if (room == null) {
				response.put("success", false);
				response.put("message", "대기방을 찾을 수 없습니다");
				return response;
			}

			// 2. 초대받는 사용자 조회 (email로)
			User invitedUser = userService.findByEmail(friendEmail); // ⭐ email 사용
			if (invitedUser == null) {
				response.put("success", false);
				response.put("message", "사용자를 찾을 수 없습니다");
				return response;
			}

			// 3. 참가자 추가 (중복 제거)
			participantService.joinRoomIfNotExists(room, invitedUser, null,
					invitedUser.getUserProfile() != null ? invitedUser.getUserProfile().getUsername()
							: invitedUser.getEmail(),
					invitedUser.getUserProfile() != null ? invitedUser.getUserProfile().getProfileImage() : null);

			List<?> participants = participantService.findByRoom(room);
			Map<String, Object> wsMessage = new HashMap<>();
			wsMessage.put("type", "PARTICIPANTUPDATE");
			wsMessage.put("participants", participants);
			messagingTemplate.convertAndSend("/topic/participants/" + roomCode, wsMessage);

			response.put("success", true);
			System.out.println("✅ 친구 초대 성공 - roomCode: " + roomCode + ", email: " + friendEmail);

		} catch (Exception e) {
			System.err.println("❌ 친구 초대 실패: " + e.getMessage());
			e.printStackTrace();
			response.put("success", false);
			response.put("message", "초대 중 오류가 발생했습니다: " + e.getMessage());
		}

		return response;
	}

	@PostMapping("/api/room/{roomCode}/select-quiz")
	@ResponseBody
	public Map<String, Object> selectQuiz(@PathVariable("roomCode") String roomCode,
			@RequestParam("quizId") Long quizId, Principal principal) {
		Map<String, Object> response = new HashMap<>();

		try {
			// 1. 방 조회 & 권한 확인
			Room room = roomService.getRoomByCode(roomCode);
			if (room == null) {
				response.put("success", false);
				response.put("message", "대기방을 찾을 수 없습니다.");
				return response;
			}

			User user = userService.findByEmail(principal.getName());
			if (user == null || !room.getHostUserId().equals(user.getId())) {
				response.put("success", false);
				response.put("message", "방장만 퀴즈를 선택할 수 있습니다.");
				return response;
			}

			// 2. 퀴즈 조회
			Quiz quiz = quizService.findById(quizId);
			if (quiz == null) {
				response.put("success", false);
				response.put("message", "선택한 퀴즈가 존재하지 않습니다.");
				return response;
			}

			// 3. 기존 퀴즈 삭제 후 새로 저장 (중복 방지)
			roomQuizService.deleteByRoomId(room.getId());
			roomQuizService.saveRoomQuiz(room.getId(), quizId);

			// 4. 성공 응답 (클라이언트가 기대하는 형식)
			response.put("success", true);
			response.put("quizId", quizId);
			response.put("quizTitle", quiz.getTitle());

			// 5. WebSocket으로 모든 참가자에게 알림
			Map<String, Object> quizNotification = new HashMap<>();
			quizNotification.put("type", "QUIZ_SELECTED");
			quizNotification.put("quizId", quizId);
			quizNotification.put("quizTitle", quiz.getTitle());
			messagingTemplate.convertAndSend("/topic/room/" + roomCode, quizNotification);

			System.out.println("✅ [퀴즈 선택] " + roomCode + " → " + quiz.getTitle());

		} catch (Exception e) {
			System.err.println("❌ 퀴즈 선택 오류: " + e.getMessage());
			response.put("success", false);
			response.put("message", "퀴즈 선택 중 오류가 발생했습니다: " + e.getMessage());
		}

		return response;
	}

}