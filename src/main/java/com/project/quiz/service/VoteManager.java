package com.project.quiz.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.project.quiz.dto.VoteResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoteManager {

	// WebSocket으로 메시지 보내기 위한 템플릿
	private final SimpMessagingTemplate messagingTemplate;

	// 방 코드별로 현재 진행 중인 투표 세션 하나씩 관리
	// key: roomCode, value: 그 방의 현재 투표 정보
	private final ConcurrentHashMap<String, VoteSession> activeVotes = new ConcurrentHashMap<>();

	// 투표 종료를 위한 타이머 스레드풀
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

	/**
	 * 투표 시작 (방장이 시작 버튼 눌렀을 때 컨트롤러에서 호출)
	 */
	public void startVote(String roomCode, Long voteId, String question, String description, String creator,
			int durationSeconds) {

		// 방마다 하나의 세션만 유지 (기존 투표 있으면 덮어씀)
		VoteSession session = new VoteSession(voteId, question, description, creator);
		activeVotes.put(roomCode, session);

		log.info("투표 시작: roomCode={}, question={}, duration={}s", roomCode, question, durationSeconds);

		// durationSeconds 후 자동 종료
		ScheduledFuture<?> timer = scheduler.schedule(() -> endVote(roomCode), durationSeconds, TimeUnit.SECONDS);
		session.setTimer(timer);
	}

	/**
	 * 누군가 찬성/반대를 눌렀을 때 호출
	 */
	public void submitVote(String roomCode, Long voteId, String voter, String choice) {

		VoteSession session = activeVotes.get(roomCode);
		if (session == null) {
			log.warn("진행 중인 투표가 없는 방입니다. roomCode={}", roomCode);
			return;
		}
		if (!session.getVoteId().equals(voteId)) {
			log.warn("투표 ID가 일치하지 않습니다. roomCode={}, voteId={}", roomCode, voteId);
			return;
		}

		// 이미 투표한 사람은 무시 (한 사람 한 표)
		if (session.hasVoted(voter)) {
			log.info("이미 투표한 사용자입니다. voter={}", voter);
			return;
		}

		// 투표 반영
		session.addVote(voter, choice);

		// 현재 결과 계산
		Map<String, Integer> results = session.getCurrentResults();
		log.info("현재 투표 결과: roomCode={}, results={}", roomCode, results);

		// 실시간 결과를 모든 클라이언트에게 WebSocket으로 전송
		VoteResponse response = VoteResponse.builder().type("UPDATE").voteId(voteId).results(results).build();

		messagingTemplate.convertAndSend("/topic/vote/" + roomCode, response);
	}

	/**
	 * 타이머에 의해 또는 수동으로 투표 종료
	 */
	public void endVote(String roomCode) {
		VoteSession session = activeVotes.remove(roomCode);
		if (session == null) {
			return;
		}

		// 타이머 취소
		ScheduledFuture<?> timer = session.getTimer();
		if (timer != null && !timer.isDone()) {
			timer.cancel(true);
		}

		Map<String, Integer> finalResults = session.getCurrentResults();
		int agree = finalResults.getOrDefault("AGREE", 0);
		int disagree = finalResults.getOrDefault("DISAGREE", 0);
		int total = agree + disagree;

		// 1) 클라이언트 쪽 투표 UI 정리를 위한 END 메시지
		VoteResponse endResponse = VoteResponse.builder().type("END").voteId(session.getVoteId()).results(finalResults)
				.build();
		messagingTemplate.convertAndSend("/topic/vote/" + roomCode, endResponse);

		// 2) 채팅창에 최종 결과를 한 줄로 출력
		VoteResultChatMessage chatMsg = new VoteResultChatMessage(session.getQuestion(), agree, disagree, total);
		messagingTemplate.convertAndSend("/topic/chat/" + roomCode, chatMsg);

		log.info("투표 종료: roomCode={}, question={}, results={}", roomCode, session.getQuestion(), finalResults);
	}

	/**
	 * 방이 완전히 닫힐 때 호출하면 깔끔하게 정리 가능 (선택)
	 */
	public void clearRoom(String roomCode) {
		endVote(roomCode);
	}

	// ================= 내부 클래스들 =================

	/**
	 * 한 방에서 진행 중인 투표 1개에 대한 상태
	 */
	private static class VoteSession {
		private final Long voteId;
		private final String question;
		private final String description;
		private final String creator;

		// 누가 어떤 선택을 했는지 (voter -> choice)
		private final ConcurrentHashMap<String, String> votes = new ConcurrentHashMap<>();
		// 투표 종료 예약 타이머
		private ScheduledFuture<?> timer;

		VoteSession(Long voteId, String question, String description, String creator) {
			this.voteId = voteId;
			this.question = question;
			this.description = description;
			this.creator = creator;
		}

		Long getVoteId() {
			return voteId;
		}

		String getQuestion() {
			return question;
		}

		ScheduledFuture<?> getTimer() {
			return timer;
		}

		void setTimer(ScheduledFuture<?> timer) {
			this.timer = timer;
		}

		boolean hasVoted(String voter) {
			return votes.containsKey(voter);
		}

		void addVote(String voter, String choice) {
			votes.put(voter, choice);
		}

		Map<String, Integer> getCurrentResults() {
			Map<String, Integer> result = new HashMap<>();
			result.put("AGREE", 0);
			result.put("DISAGREE", 0);

			for (String choice : votes.values()) {
				if ("AGREE".equals(choice)) {
					result.put("AGREE", result.get("AGREE") + 1);
				} else if ("DISAGREE".equals(choice)) {
					result.put("DISAGREE", result.get("DISAGREE") + 1);
				}
			}
			return result;
		}
	}

	/**
	 * 채팅창에 뿌릴 최종 결과용 메시지 DTO (기존 채팅과 동일하게 sender, content만 쓰면 됨)
	 */
	public static class VoteResultChatMessage {
		public String sender = "투표 시스템";
		public String content;

		public VoteResultChatMessage(String question, int agree, int disagree, int total) {
			double agreePercent = total > 0 ? (agree * 100.0 / total) : 0.0;
			double disagreePercent = total > 0 ? (disagree * 100.0 / total) : 0.0;

			this.content = String.format("[투표 종료]\n%s\n찬성: %d명 (%.1f%%), 반대: %d명 (%.1f%%), 총 %d명 참여", question, agree,
					agreePercent, disagree, disagreePercent, total);
		}
	}
}
