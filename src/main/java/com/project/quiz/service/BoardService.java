package com.project.quiz.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.BoardPost;
import com.project.quiz.domain.User;
import com.project.quiz.repository.BoardRepository;
import com.project.quiz.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final UserRepository userRepository;

    // 공지사항 목록 조회 (읽기 전용)
    @Transactional(readOnly = true)
    public List<BoardPost> getNoticeList() {
        // 코드 테이블의 'notice'와 일치해야 함
        return boardRepository.findByBoardTypeCodeOrderByCreatedAtDesc("notice");
    }

    // 공지사항 상세 조회
    @Transactional(readOnly = true)
    public BoardPost getPost(Long postId) {
        BoardPost post = boardRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        
        // 조회수 증가 로직 (선택 사항)
        post.setViewCount(post.getViewCount() + 1);
        return post;
    }

    // 공지사항 작성 (관리자 권한 체크는 Controller/Security에서 1차 수행)
    public void writeNotice(String email, String title, String content) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // (선택) 여기서 한 번 더 ADMIN 체크를 할 수도 있음
        if (!"ADMIN".equals(user.getRole())) {
             throw new IllegalStateException("관리자만 작성 가능합니다.");
        }

        BoardPost post = new BoardPost();
        post.setBoardTypeCode("notice"); // ★ 핵심: 코드값 notice로 고정
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setViewCount(0);
        post.setCreatedAt(LocalDateTime.now());
        
        boardRepository.save(post);
    }
}