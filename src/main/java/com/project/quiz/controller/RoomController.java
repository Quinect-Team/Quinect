package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.config.QRCodeService;
import com.project.quiz.domain.CodeTable;
import com.project.quiz.domain.Room;
import com.project.quiz.dto.GuestUserDto;
import com.project.quiz.repository.CodeTableRepository;
import com.project.quiz.service.RoomService;

import jakarta.servlet.http.HttpSession;

@Controller
public class RoomController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private CodeTableRepository codeTableRepository;

	@Autowired
	private QRCodeService qrCodeService;

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
	public String createRoomPost(@RequestParam(name = "hostUserId") Long hostUserId,
			@RequestParam(name = "roomTypeCode") String roomTypeCode, Principal principal, HttpSession session) {
		if (principal == null && session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/form";
		}
		Room room = roomService.createRoom(hostUserId, roomTypeCode, "opened");
		return "redirect:/waitroom/" + room.getRoomCode();
	}

	@GetMapping("/waitroom/{roomCode}")
	public String showRoomByCode(@PathVariable("roomCode") String roomCode, Model model, Principal principal,
			HttpSession session) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null) {
			return "error/404"; // 방 없음 처리 페이지
		}

		// 로그인 체크: principal이 없고 세션에 게스트 정보도 없으면 setup으로 이동
		if (principal == null && session.getAttribute("guestUser") == null) {
			return "redirect:/guest/setup?next=/waitroom/" + roomCode;
		}
		GuestUserDto guestUser = (GuestUserDto) session.getAttribute("guestUser");
		if (guestUser != null) {
			model.addAttribute("guestNickname", guestUser.getNickname());
			model.addAttribute("guestAvatarUrl", guestUser.getCharacterImageUrl());
		}

		// codeTableRepository를 활용하여 roomTypeCode에 해당하는 이름 조회
		CodeTable codeInfo = codeTableRepository.findById(room.getRoomTypeCode()).orElse(null);

		String roomTypeName = (codeInfo != null) ? codeInfo.getName() : "알 수 없음";

		model.addAttribute("room", room);
		model.addAttribute("roomTypeName", roomTypeName);

		try {
			// QR코드에 들어갈 URL
			String url = "https://yourdomain.com/waitroom/" + roomCode; // 도메인에 맞게 변경!
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
}