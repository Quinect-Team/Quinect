package com.project.quiz.controller;

import java.util.List;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.project.quiz.config.QRCodeService;
import com.project.quiz.domain.CodeTable;
import com.project.quiz.domain.Room;
import com.project.quiz.repository.CodeTableRepository;
import com.project.quiz.service.RoomService;

@Controller
public class RoomController {
	@Autowired
	private RoomService roomService;

	@Autowired
	private CodeTableRepository codeTableRepository;

	@Autowired
	private QRCodeService qrCodeService;

	@GetMapping("/waitroom/form")
	public String showRoomForm(Model model) {
		List<CodeTable> roomTypes = codeTableRepository.findByGroupId("room_type");
		model.addAttribute("roomTypes", roomTypes); // 드롭다운용
		return "waitroom_form";
	}

	@PostMapping("/waitroom/create")
	public String createRoomPost(@RequestParam(name = "hostUserId") Long hostUserId,
			@RequestParam(name = "roomTypeCode") String roomTypeCode) {
		Room room = roomService.createRoom(hostUserId, roomTypeCode, "opened");
		return "redirect:/waitroom/" + room.getRoomCode();
	}

	@GetMapping("/waitroom/{roomCode}")
	public String showRoomByCode(@PathVariable("roomCode") String roomCode, Model model) {
		Room room = roomService.getRoomByCode(roomCode);
		if (room == null) {
			return "error/404"; // 방 없음 처리 페이지
		}
		// codeTableRepository를 활용하여 roomTypeCode에 해당하는 이름 조회
		CodeTable codeInfo = codeTableRepository.findById(room.getRoomTypeCode()).orElse(null);

		String roomTypeName = (codeInfo != null) ? codeInfo.getName() : "알 수 없음";

		model.addAttribute("room", room);
		model.addAttribute("roomTypeName", roomTypeName);

		try {
			// QR코드에 들어갈 URL
			String url = "https://yourdomain.com/waitroom/" + roomCode; // 도메인에 맞게 변경!
			String url2 = "https://search.naver.com/search.naver?where=nexearch&sm=top_hty&fbm=0&ie=utf8&query=" + roomCode + "&ackey=088trqms"; // 도메인에 맞게 변경!
			byte[] qrImage = qrCodeService.generateQRCodeImage(url2, 250, 250);
			String qrCodeBase64 = Base64.encodeBase64String(qrImage);
			model.addAttribute("qrCodeBase64", qrCodeBase64);
		} catch (Exception e) {
			// 에러시 기본 값 등 처리
			model.addAttribute("qrCodeBase64", null);
		}

		return "waitroom";
	}

	@GetMapping("/joinroom")
	public String joinRoom() {
		return "joinroom";
	}

	@PostMapping("/waitroom/join")
	public String joinRoom(@RequestParam("roomCode") String roomCode) {
		// 방 존재 체크, 참가 처리 등 추가 가능
		return "redirect:/waitroom/" + roomCode; // 그 방의 대기실로 이동
	}
}