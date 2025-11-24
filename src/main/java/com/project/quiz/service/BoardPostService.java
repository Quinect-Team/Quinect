package com.project.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.quiz.domain.BoardPost;
import com.project.quiz.repository.BoardPostRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

    private final BoardPostRepository boardPostRepository;

    // 전체 게시글 목록 조회
    public List<BoardPost> findAll() {
        return boardPostRepository.findAll();
    }

    // 게시글 아이디로 단일 조회
    public BoardPost findById(Long postId) {
        return boardPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다. id=" + postId));
    }

    // 게시글 저장(등록 및 수정)
    @Transactional
    public BoardPost save(BoardPost boardPost) {
        return boardPostRepository.save(boardPost);
    }
}
