package com.project.quiz.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.project.quiz.domain.BoardPost;
import com.project.quiz.service.BoardService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final BoardService boardService;

    // 1. 공지사항 목록
    @GetMapping
    public String noticeList(Model model) {
        List<BoardPost> notices = boardService.getNoticeList();
        model.addAttribute("notices", notices);
        
        // ⭐ 수정됨: layout 폴더의 notice.html을 바로 보여줍니다.
        return "notice"; 
    }

    // 2. 공지사항 상세
    @GetMapping("/{id}")
    public String noticeDetail(@PathVariable("id") Long id, Model model) {
        BoardPost post = boardService.getPost(id);
        model.addAttribute("post", post);
        return "noticedetail"; // 이건 상세 페이지용 (나중에 필요하면 생성)
    }

    // 3. 공지사항 작성 폼 (관리자)
    @GetMapping("/write")
    public String noticeWriteForm(Principal principal) {
        if (principal == null) return "redirect:/login";
        return "noticewrite"; // 글쓰기 폼 파일 경로
    }

    // 4. 작성 처리
    @PostMapping("/write")
    public String noticeWrite(@RequestParam("title") String title,
                              @RequestParam("content") String content,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        boardService.writeNotice(principal.getName(), title, content);
        return "redirect:/notice";
    }
}