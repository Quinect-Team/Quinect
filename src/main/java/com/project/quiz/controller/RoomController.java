package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.config.QRCodeService;
import com.project.quiz.domain.CodeTable;
import com.project.quiz.domain.Room;
import com.project.quiz.domain.User;
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

	@GetMapping("/waitroom/form")
	public String showRoomForm(Model model, Principal principal, HttpSession session) {
		if (principal == null && session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/form";
		}
		List<CodeTable> roomTypes = codeTableRepository.findByGroupId("room_type");
		model.addAttribute("roomTypes", roomTypes); // 드롭다운용
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
		return "redirect:/guest/setup?next=/waitroom/" + room.getRoomCode();
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

		participantService.joinRoomIfNotExists(room, user, guestId, nickname, avatarUrl);

		model.addAttribute("room", room);
		model.addAttribute("roomTypeName", roomTypeName);
		model.addAttribute("participants", participantService.findByRoom(room));
		model.addAttribute("guestNickname", nickname);
		model.addAttribute("guestAvatarUrl", avatarUrl);

		boolean isRoomMaster = (user != null && room.getHostUserId().equals(user.getId()));
		System.out.println("isRoomMaster=" + isRoomMaster + ", userId=" + (user != null ? user.getId() : null)
				+ ", hostUserId=" + room.getHostUserId());
		model.addAttribute("isRoomMaster", isRoomMaster);
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
		if (principal == null && session.getAttribute("guestUser") == null) {
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

}