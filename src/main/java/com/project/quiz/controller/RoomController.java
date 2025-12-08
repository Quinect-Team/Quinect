package com.project.quiz.controller;

import java.security.Principal;
import java.util.*;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.config.QRCodeService;
import com.project.quiz.domain.CodeTable;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
import com.project.quiz.domain.UserProfile;
import com.project.quiz.dto.GuestUserDto;
import com.project.quiz.dto.VoteRequest;
import com.project.quiz.dto.VoteResponse;
import com.project.quiz.repository.CodeTableRepository;
import com.project.quiz.service.ParticipantService;
import com.project.quiz.service.RoomService;
import com.project.quiz.service.UserService;
import com.project.quiz.service.VoteManager;

import jakarta.servlet.http.HttpSession;

@Controller
public class RoomController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private CodeTableRepository codeTableRepository;

	@Autowired
	private QRCodeService qrCodeService;

	@Autowired
	private UserService userService;

	@Autowired
	private ParticipantService participantService;

	@Autowired
	private VoteManager voteManager;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@GetMapping("/waitroom/form")
	public String showRoomForm(Model model, Principal principal, HttpSession session) {
		if (principal != null) {
			List<CodeTable> roomTypes = codeTableRepository.findByGroupId("room_type");
			model.addAttribute("roomTypes", roomTypes);
			return "waitroom_form";
		}

		if (session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/form";
		}

		List<CodeTable> roomTypes = codeTableRepository.findByGroupId("room_type");
		model.addAttribute("roomTypes", roomTypes);
		return "waitroom_form";
	}

	@PostMapping("/waitroom/create")
	public String createRoomPost(@RequestParam(name = "roomTypeCode") String roomTypeCode, Principal principal,
			HttpSession session) {
		if (principal == null) {
			return "redirect:/login";
		}

		// 현재 로그인한 유저 정보에서 ID 가져오기
		User user = userService.findByEmail(principal.getName());
		if (user == null) {
			return "redirect:/login";
		}

		// user.getId()를 호스트유저아이디로 사용
		Room room = roomService.createRoom(user.getId(), roomTypeCode, "opened");

		return "redirect:/waitroom/" + room.getRoomCode();
	}

	@GetMapping("/waitroom/{roomCode}")
	public String showRoomByCode(@PathVariable("roomCode") String roomCode, Model model, Principal principal,
			HttpSession session) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null) {
			return "error/404"; // 방 없음 처리 페이지
		}

		if ("CLOSED".equals(room.getStatusCode())) {
			return "room_closed"; // "이 방은 종료되었습니다" 같은 안내 템플릿 만들기
		}

		// codeTableRepository를 활용하여 roomTypeCode에 해당하는 이름 조회
		CodeTable codeInfo = codeTableRepository.findById(room.getRoomTypeCode()).orElse(null);

		String roomTypeName = (codeInfo != null) ? codeInfo.getName() : "알 수 없음";

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

		participantService.joinRoomIfNotExists(room, user, guestId, nickname, avatarUrl);

		model.addAttribute("room", room);
		model.addAttribute("roomTypeName", roomTypeName);
		model.addAttribute("participants", participantService.findByRoom(room));
		model.addAttribute("guestNickname", nickname);
		model.addAttribute("guestAvatarUrl", avatarUrl);
		model.addAttribute("currentUser", user);

		boolean isRoomMaster = (user != null && room.getHostUserId().equals(user.getId()));
		model.addAttribute("isRoomMaster", isRoomMaster);

		try {
			String url2 = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query="
					+ roomCode + "&ackey=088trqms"; // 도메인에 맞게 변경!
			byte[] qrImage = qrCodeService.generateQRCodeImage(url2, 250, 250);
			@SuppressWarnings("deprecation")
			String qrCodeBase64 = Base64.encodeBase64String(qrImage);
			model.addAttribute("qrCodeBase64", qrCodeBase64);
		} catch (Exception e) {
			// 에러시 기본 값 등 처리
			model.addAttribute("qrCodeBase64", null);
		}

		String joinMessage = nickname + "님이 입장하셨습니다.";
		Map<String, Object> joinNotification = new HashMap<>();
		joinNotification.put("type", "SYSTEM");
		joinNotification.put("sender", "시스템");
		joinNotification.put("content", joinMessage);
		joinNotification.put("timestamp", System.currentTimeMillis());

		// WebSocket으로 모든 클라이언트에 브로드캐스트
		messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinNotification);

		// ✅ 참가자 목록 업데이트 알림
		Map<String, Object> participantUpdate = new HashMap<>();
		participantUpdate.put("type", "PARTICIPANT_UPDATE");
		participantUpdate.put("participants", participantService.findByRoom(room));

		messagingTemplate.convertAndSend("/topic/participants/" + roomCode, participantUpdate);

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

		System.out.println("Ready status received from room: " + roomCode);
		System.out.println("Ready data: " + readyData);

		// 그대로 모든 참가자에게 브로드캐스트
		return readyData;
	}

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

			System.out.println("✅ 친구 초대 - roomCode: " + roomCode + ", email: " + friendEmail);

			// 3. 참가자 추가 (중복 제거)
			participantService.joinRoomIfNotExists(room, invitedUser, null,
					invitedUser.getUserProfile() != null ? invitedUser.getUserProfile().getUsername()
							: invitedUser.getEmail(),
					invitedUser.getUserProfile() != null ? invitedUser.getUserProfile().getProfileImage() : null);

			// 4. WebSocket으로 참가자 업데이트 브로드캐스트
			Map<String, Object> participantUpdate = new HashMap<>();
			participantUpdate.put("type", "PARTICIPANT_UPDATE");
			participantUpdate.put("participants", participantService.findByRoom(room));
			messagingTemplate.convertAndSend("/topic/participants/" + roomCode, participantUpdate);

			// 5. 입장 알림 메시지
			String userName = invitedUser.getUserProfile() != null ? invitedUser.getUserProfile().getUsername()
					: invitedUser.getEmail();
			String joinMessage = userName + "님이 입장하셨습니다.";

			Map<String, Object> joinNotification = new HashMap<>();
			joinNotification.put("type", "SYSTEM");
			joinNotification.put("sender", "시스템");
			joinNotification.put("content", joinMessage);
			joinNotification.put("timestamp", System.currentTimeMillis());

			messagingTemplate.convertAndSend("/topic/chat/" + roomCode, joinNotification);

			response.put("success", true);
			response.put("message", joinMessage);

		} catch (Exception e) {
			System.err.println("❌ 친구 초대 실패: " + e.getMessage());
			e.printStackTrace();
			response.put("success", false);
			response.put("message", "초대 중 오류가 발생했습니다: " + e.getMessage());
		}

		return response;
	}

}