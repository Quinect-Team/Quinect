var roomCode = null;
var username = null;
var isRoomMaster = null;
var userId = null;

var currentVoteId = null;
var currentVoteChoice = null;
var voteResults = { AGREE: 0, DISAGREE: 0 };
var voteCountdownInterval = null;
var voteCountdownTimer = null;

var participants = [];

var stompClient = null;
var currentQuizRoomId = null;

var readyStatus = {};
var myReadyStatus = false;
var initialized = false;

var quizCountdownInterval = null;
var quizCountdownTimer = null;

var initialParticipants = [];


// ========== 모든 함수들 (DOMContentLoaded 밖에) ==========

function initWebSocket() {
	var socket = new SockJS('/ws');
	stompClient = Stomp.over(socket);

	stompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);

		var savedChats = JSON.parse(localStorage.getItem('chatMessages_' + roomCode) || '[]');
		savedChats.forEach(function(chat) {
			displayMessage(chat.sender, chat.content);
		});

		var savedVote = localStorage.getItem('currentVote_' + roomCode);
		if (savedVote) {
			var voteInfo = JSON.parse(savedVote);
			currentVoteId = voteInfo.voteId;

			var savedResults = localStorage.getItem('voteResults_' + roomCode + '_' + voteInfo.voteId);
			if (savedResults) {
				voteResults = JSON.parse(savedResults);
				displayVoteMessageInChat(voteInfo);
			}
		}

		var savedReadyStatus = localStorage.getItem('readyStatus_' + roomCode);
		if (savedReadyStatus) {
			readyStatus = JSON.parse(savedReadyStatus);
		}

		stompClient.subscribe('/topic/chat/' + roomCode, function(message) {
			var msg = JSON.parse(message.body);

			if (msg.type === 'SYSTEM') {
				displaySystemMessage(msg.sender, msg.content);
			} else {
				displayMessage(msg.sender, msg.content);
			}
		});

		stompClient.subscribe('/topic/vote/' + roomCode, function(message) {
			var voteData = JSON.parse(message.body);
			handleVoteUpdate(voteData);
		});

		stompClient.subscribe('/topic/ready/' + roomCode, function(message) {
			var readyData = JSON.parse(message.body);

			// QUIZ_START 신호 처리 (새로 추가!)
			if (readyData.type === 'QUIZ_START') {
				handleQuizStart(readyData);
			} else {
				// 기존 READY 메시지 처리
				handleReadyUpdate(readyData);
			}
		});

		// 참가자 업데이트 구독에서 시스템 메시지 완전 제거
		// WebSocket으로 새 참가자 정보 받음
		stompClient.subscribe('/topic/participants/' + roomCode, function(message) {
			var data = JSON.parse(message.body);
			if (data.type === 'PARTICIPANTUPDATE') {

				var newParticipants = [];  // ✅ 새로운 참가자 저장

				// ✅ 새로운 참가자 감지
				data.participants.forEach(function(participant) {
					if (!initialParticipants.includes(participant.id)) {
						initialParticipants.push(participant.id);
						newParticipants.push(participant);  // ✅ 새 참가자 목록에 추가
					}
				});

				// ✅ 새로운 참가자가 있으면 한 번만 UI 업데이트
				if (newParticipants.length > 0) {
					updateParticipantUI(data.participants);  // ✅ 모든 참가자로 업데이트

					// ✅ 새로운 참가자마다 개별 알림
					newParticipants.forEach(function(newParticipant) {
						displaySystemMessage('시스템', '👋 ' + newParticipant.nickname + '님이 입장하셨습니다!');
					});
				}
			}
		});

		// ✅ updateParticipantUI는 그대로 사용
		function updateParticipantUI(participants) {
			var cardBody = document.querySelector('.card-header.bg-info')?.parentElement?.querySelector('.card-body');
			if (!cardBody) {
				console.warn('Participant card body not found');
				return;
			}

			participants.forEach(function(participant) {
				var existingCard = document.querySelector('[data-user-id="' + participant.id + '"]');

				if (!existingCard) {
					var newCard = document.createElement('div');
					newCard.className = 'card border-success m-2 text-center';
					newCard.style.cssText = 'width: 114px; height: 180px; position: relative;';
					newCard.setAttribute('data-user-id', participant.id);

					newCard.innerHTML = `
		                <div class="mt-2">
		                    <img src="${participant.avatarUrl || '/img/default-avatar.png'}" 
		                         class="rounded-circle mb-2" width="55" height="55" alt="avatar">
		                </div>
		                <div class="font-weight-bold text-primary">${participant.nickname}</div>
		            `;

					cardBody.appendChild(newCard);
				}
			});
		}

		// ✅ 퀴즈 선택 WebSocket 구독 추가
		stompClient.subscribe('/topic/room/' + roomCode, function(message) {
			var data = JSON.parse(message.body);

			if (data.type === 'QUIZ_SELECTED') {
				handleQuizSelection(data);
			}
		});

		if (isRoomMaster) {
			var voteBtnEl = document.getElementById('voteBtn');
			if (voteBtnEl) {
				voteBtnEl.style.display = 'inline-block';
				voteBtnEl.onclick = function() {
					openCreateVoteModal();
				};
			}

			var selectQuizBtnEl = document.getElementById('selectQuizBtn');
			if (selectQuizBtnEl) {
				selectQuizBtnEl.style.display = 'inline-block';
				selectQuizBtnEl.onclick = function() {
					openSelectQuizModal();
				};
			}
		}

	}, function(error) {
		console.error('Error: ' + error);
	});
}

function displaySystemMessage(sender, content) {
	var messagesDiv = document.getElementById('messages');
	var msgDiv = document.createElement('div');
	msgDiv.innerHTML = '<strong style="color: #28a745;">✓ ' + sender + ':</strong> <em>' + content + '</em>';
	msgDiv.style.padding = '8px';
	msgDiv.style.marginBottom = '8px';
	msgDiv.style.borderBottom = '1px solid #eee';
	msgDiv.style.color = '#666';
	msgDiv.style.fontStyle = 'italic';
	messagesDiv.appendChild(msgDiv);
	messagesDiv.scrollTop = messagesDiv.scrollHeight;

	var chatMessages = JSON.parse(localStorage.getItem('chatMessages_' + roomCode) || '[]');
	chatMessages.push({
		sender: sender,
		content: content,
		type: 'SYSTEM',
		timestamp: new Date().getTime()
	});
	localStorage.setItem('chatMessages_' + roomCode, JSON.stringify(chatMessages));
}

function updateParticipantUI(participants) {
	console.log('Updating participant UI with:', participants);

	// ✅ 카드 컨테이너 찾기
	var cardBody = document.querySelector('.card-header.bg-info').parentElement.querySelector('.card-body');
	if (!cardBody) {
		console.warn('Participant card body not found');
		return;
	}

	participants.forEach(function(participant) {
		// ✅ 이미 있는 카드 확인
		var existingCard = document.querySelector('[data-user-id="' + participant.id + '"]');

		if (!existingCard) {
			// ✅ 새로운 참가자 카드 생성
			var newCard = document.createElement('div');
			newCard.className = 'card border-success m-2 text-center';
			newCard.style.cssText = 'width: 114px; height: 180px; position: relative;';
			newCard.setAttribute('data-user-id', participant.id);

			newCard.innerHTML = `
                <div class="mt-2">
                    <img src="${participant.avatarUrl ? participant.avatarUrl : '/img/default-avatar.png'}" 
                         class="rounded-circle mb-2" width="55" height="55" alt="avatar">
                </div>
                <div class="font-weight-bold text-primary">${participant.nickname}</div>
            `;

			cardBody.appendChild(newCard);

			// ✅ 입장 알림
			displaySystemMessage('시스템', '👋 ' + participant.nickname + '님이 입장하셨습니다!');
		}
	});
}


function displayMessage(sender, content) {
	var messagesDiv = document.getElementById('messages');
	var msgDiv = document.createElement('div');
	msgDiv.innerHTML = '<strong>' + sender + ':</strong> ' + content;
	msgDiv.style.padding = '8px';
	msgDiv.style.marginBottom = '8px';
	msgDiv.style.borderBottom = '1px solid #eee';
	messagesDiv.appendChild(msgDiv);
	messagesDiv.scrollTop = messagesDiv.scrollHeight;

	var chatMessages = JSON.parse(localStorage.getItem('chatMessages_' + roomCode) || '[]');
	chatMessages.push({
		sender: sender,
		content: content,
		timestamp: new Date().getTime()
	});
	localStorage.setItem('chatMessages_' + roomCode, JSON.stringify(chatMessages));
}

function sendMessage() {
	var input = document.getElementById('chat-input');
	var text = input.value.trim();
	if (text && stompClient.connected) {
		var chatMessage = {
			sender: username,
			content: text
		};
		stompClient.send('/app/chat/' + roomCode, {}, JSON.stringify(chatMessage));
		input.value = '';
	}
}

function copyCode() {
	var code = document.querySelector('.display-4').textContent;
	navigator.clipboard.writeText(code)
		.then(function() { alert("방 코드가 복사되었습니다: " + code); });
}

function openCreateVoteModal() {
	document.getElementById('voteTitle').value = '';
	document.getElementById('voteContent').value = '';
	$('#createVoteModal').modal('show');
	document.getElementById('voteTitle').focus();
}

function setVoteDuration(seconds) {
	document.getElementById('voteDuration').value = seconds;

	document.querySelectorAll('.btn-group-sm .btn').forEach(btn => {
		btn.classList.remove('active', 'btn-primary');
		btn.classList.add('btn-outline-secondary');
	});

	event.target.classList.remove('btn-outline-secondary');
	event.target.classList.add('active', 'btn-primary');
}

function submitVoteCreate() {
	var title = document.getElementById('voteTitle').value.trim();
	var content = document.getElementById('voteContent').value.trim();
	var duration = parseInt(document.getElementById('voteDuration').value) || 30;

	if (!title) {
		alert('투표 제목을 입력해주세요.');
		return;
	}

	if (duration < 10 || duration > 300) {
		alert('투표 시간은 10초 ~ 300초 범위여야 합니다.');
		return;
	}

	if (stompClient.connected) {
		var voteData = {
			type: 'START',
			voteId: Date.now(),
			question: title,
			description: content,
			creator: username,
			duration: duration,  // ⭐ 투표 시간 포함
			timestamp: new Date().getTime()
		};

		stompClient.send('/app/vote/start/' + roomCode, {}, JSON.stringify(voteData));

		// ⭐ 프로그레스바 타이머 시작
		startVoteProgressTimer(duration, voteData.voteId);

		$('#createVoteModal').modal('hide');
	} else {
		alert('WebSocket이 연결되지 않았습니다.');
	}
}

function startVoteProgressTimer(duration, voteId) {
	console.log('🗳️ 투표 프로그레스바 시작:', duration + '초', 'voteId:', voteId);

	const startTime = Date.now();
	const endTime = startTime + (duration * 1000); // 종료 시각 미리 계산

	var voteMessages = document.querySelectorAll('.vote-message');
	var voteMessage = voteMessages[voteMessages.length - 1];

	if (!voteMessage) return;

	// (프로그레스바 HTML 생성 부분은 기존과 동일하므로 생략)
	var progressContainer = document.createElement('div');
	progressContainer.className = 'vote-progress-container';
	progressContainer.id = 'vote-progress-' + voteId;
	// ... [중략: 기존 스타일 및 innerHTML 코드] ...
	progressContainer.innerHTML = `
            <small style="color: #ffffff; font-size: 15px;">
                <span class="vote-remaining-time">${duration}</span>초 / ${duration}초
            </small>
        <div class="progress" style="height: 20px; border-radius: 4px; overflow: hidden; background: #e9ecef;">
            <div class="progress-bar vote-progress-bar" 
                 role="progressbar" 
                 style="width: 0%; background: linear-gradient(90deg, #4e73df, #2e59d9); 
                         transition: width 0.05s linear; display: flex; align-items: center; justify-content: center;">
            </div>
        </div>
    `;
	voteMessage.appendChild(progressContainer);

	function updateProgress() {
		const now = Date.now();
		const remainingMs = endTime - now;
		const elapsed = (now - startTime) / 1000;

		let progress = Math.min((elapsed / duration) * 100, 100);

		// ⭐ 수정된 부분: 0.9초 이하로 남으면 바로 '0'을 출력하도록 설정
		// Math.floor를 쓰거나, 특정 임계점(0.1초 등) 이하일 때 0으로 강제
		let remainingSeconds = Math.max(0, Math.ceil(remainingMs / 1000));
		if (remainingMs <= 500) { // 0.5초 미만으로 남았을 때 미리 0으로 표시
			remainingSeconds = 0;
		}

		var progressBar = progressContainer.querySelector('.vote-progress-bar');
		var remainingTimeSpan = progressContainer.querySelector('.vote-remaining-time');

		if (progressBar) progressBar.style.width = progress + '%';
		if (remainingTimeSpan) remainingTimeSpan.textContent = remainingSeconds;

		// ⭐ 종료 조건 세분화
		if (now >= endTime) {
			// 마지막 렌더링 확인
			if (remainingTimeSpan) remainingTimeSpan.textContent = '0';
			if (progressBar) progressBar.style.width = '100%';

			console.log('⏰ 0초 표시 완료 -> 종료 프로세스 진입');

			// 멈추기 전에 0을 확실히 보여주기 위해 프레임 루프를 여기서 종료
			cancelAnimationFrame(voteCountdownInterval);

			// 0초를 눈으로 확인할 시간을 줍니다 (500ms)
			setTimeout(() => {
				stopVoteProgressTimer();
				endVote(voteId);
			}, 500);
			return;
		}

		voteCountdownInterval = requestAnimationFrame(updateProgress);
	}

	voteCountdownInterval = requestAnimationFrame(updateProgress);

	// 하단의 기존 setTimeout은 삭제하거나 시간을 훨씬 더 길게(duration + 2초) 잡으세요.
}

function stopVoteProgressTimer() {
	console.log('⏹️ 투표 프로그레스바 중지');

	if (voteCountdownInterval) {
		cancelAnimationFrame(voteCountdownInterval);
		voteCountdownInterval = null;
	}
	if (voteCountdownTimer) {
		clearTimeout(voteCountdownTimer);
		voteCountdownTimer = null;
	}
}

function endVote(voteId) {
	console.log('🗳️ 투표 종료:', voteId);

	// 투표 버튼 비활성화
	var voteMessages = document.querySelectorAll('[data-vote-id="' + voteId + '"]');
	voteMessages.forEach(function(msg) {
		var buttons = msg.querySelectorAll('.vote-agree-btn, .vote-disagree-btn');
		buttons.forEach(btn => {
			btn.disabled = true;
			btn.style.opacity = '0.5';
		});
	});

	// 시스템 메시지
	var messagesDiv = document.getElementById('messages');
	var msgDiv = document.createElement('div');
	msgDiv.innerHTML = '<strong style="color: #dc3545;">✓ 시스템:</strong> <em>투표가 종료되었습니다.</em>';
	msgDiv.style.padding = '8px';
	msgDiv.style.marginBottom = '8px';
	msgDiv.style.borderBottom = '1px solid #eee';
	msgDiv.style.color = '#dc3545';
	msgDiv.style.fontStyle = 'italic';
	messagesDiv.appendChild(msgDiv);
	messagesDiv.scrollTop = messagesDiv.scrollHeight;

	localStorage.removeItem('currentVote_' + roomCode);
	currentVoteId = null;
}


function openParticipateVoteModal(voteId, question, description) {
	currentVoteId = voteId;
	currentVoteChoice = null;

	document.querySelector('#voteQuestion h6').textContent = question;
	document.getElementById('voteDescription').textContent = description || '';

	document.querySelectorAll('.vote-agree-btn, .vote-disagree-btn').forEach(function(btn) {
		btn.classList.remove('selected');
	});

	document.getElementById('voteResults').style.display = 'block';
	updateVoteResults(voteResults);

	$('#participateVoteModal').modal('show');
}

function submitVoteChoice(choice) {
	if (choice === 'AGREE') {
		document.querySelector('.vote-agree-btn').classList.add('selected');
		document.querySelector('.vote-disagree-btn').classList.remove('selected');
	} else {
		document.querySelector('.vote-disagree-btn').classList.add('selected');
		document.querySelector('.vote-agree-btn').classList.remove('selected');
	}

	currentVoteChoice = choice;

	if (stompClient.connected) {
		var voteSubmission = {
			type: 'VOTE',
			voteId: currentVoteId,
			voter: username,
			choice: choice,
			timestamp: new Date().getTime()
		};

		stompClient.send('/app/vote/submit/' + roomCode, {}, JSON.stringify(voteSubmission));
		localStorage.setItem('myVoteChoice_' + roomCode + '_' + currentVoteId, choice);
	}
}

function displayVoteMessageInChat(voteData) {
	var messagesDiv = document.getElementById('messages');
	var msgDiv = document.createElement('div');

	msgDiv.className = 'vote-message';
	msgDiv.setAttribute('data-vote-id', voteData.voteId);
	msgDiv.innerHTML = '<div class="vote-message-title">🗳️ ' + escapeHtml(voteData.question) + '</div>' +
		(voteData.description ? '<div class="vote-message-desc">' + escapeHtml(voteData.description) + '</div>' : '') +
		'<div class="vote-message-button" onclick="openParticipateVoteModal(' +
		voteData.voteId + ', \'' + voteData.question.replace(/'/g, "\\'") + '\', \'' +
		(voteData.description || '').replace(/'/g, "\\'") + '\')">투표에 참가하세요 →</div>';

	messagesDiv.appendChild(msgDiv);
	messagesDiv.scrollTop = messagesDiv.scrollHeight;
}

function handleVoteUpdate(voteData) {
	console.log('투표 업데이트:', voteData);

	if (voteData.type === 'START') {
		displayVoteMessageInChat(voteData);
		currentVoteId = voteData.voteId;
		voteResults = { AGREE: 0, DISAGREE: 0 };

		localStorage.setItem('currentVote_' + roomCode, JSON.stringify({
			voteId: voteData.voteId,
			question: voteData.question,
			description: voteData.description,
			creator: voteData.creator,
			duration: voteData.duration
		}));

		// ⭐ 프로그레스바 타이머 시작
		if (voteData.duration) {
			startVoteProgressTimer(voteData.duration, voteData.voteId);
		}

	} else if (voteData.type === 'UPDATE') {
		updateVoteResults(voteData.results);
		localStorage.setItem('voteResults_' + roomCode + '_' + voteData.voteId, JSON.stringify(voteData.results));
	} else if (voteData.type === 'END') {
		stopVoteProgressTimer();
		localStorage.removeItem('currentVote_' + roomCode);
		localStorage.removeItem('voteResults_' + roomCode + '_' + currentVoteId);
		localStorage.removeItem('myVoteChoice_' + roomCode + '_' + currentVoteId);
		currentVoteId = null;
	}
}

function updateVoteResults(results) {
	voteResults = results;
	var total = results.AGREE + results.DISAGREE || 1;

	var agreePercentage = ((results.AGREE / total) * 100).toFixed(1);
	var disagreePercentage = ((results.DISAGREE / total) * 100).toFixed(1);

	document.getElementById('agreeCount').textContent = results.AGREE + '표';
	document.getElementById('disagreeCount').textContent = results.DISAGREE + '표';

	document.getElementById('agreeBar').style.width = agreePercentage + '%';
	document.getElementById('agreePercent').textContent = agreePercentage + '%';

	document.getElementById('disagreeBar').style.width = disagreePercentage + '%';
	document.getElementById('disagreePercent').textContent = disagreePercentage + '%';
}

function escapeHtml(text) {
	var map = {
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#039;'
	};
	return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function toggleReady() {
	var newReadyStatus = !myReadyStatus;

	if (stompClient && stompClient.connected) {
		var readyData = {
			type: 'READY',
			userId: userId,
			username: username,
			isReady: newReadyStatus,
			timestamp: new Date().getTime()
		};

		stompClient.send('/app/ready/' + roomCode, {}, JSON.stringify(readyData));
		console.log('Ready status sent:', readyData);
	} else {
		alert('WebSocket이 연결되지 않았습니다.');
	}

	updateReadyButton();
}

function handleReadyUpdate(readyData) {
	console.log('Ready update:', readyData);

	var receivedUserId = readyData.userId;
	var isReady = readyData.isReady;

	readyStatus[receivedUserId] = isReady;
	localStorage.setItem('readyStatus_' + roomCode, JSON.stringify(readyStatus));

	// 👇 한 명이라도 false면 카운트다운 중지!
	var allReady = true;
	Object.values(readyStatus).forEach(function(status) {
		if (!status) {
			allReady = false;
		}
	});

	if (!allReady) {
		console.log('❌ READY 취소됨 → 카운트다운 중지');
		stopQuizCountdown();
	}

	// 기존 UI 업데이트
	if (receivedUserId === userId) {
		myReadyStatus = isReady;
		updateReadyButton();
	}
	updateParticipantCardStatus(readyData.userId, readyData.isReady);
}


function updateParticipantCardStatus(participantUserId, isReady) {
	var card = document.querySelector('[data-user-id="' + participantUserId + '"]');

	if (!card) return;

	var existingReadyBadge = card.querySelector('.ready-badge');
	if (existingReadyBadge) {
		existingReadyBadge.remove();
	}
}

function updateReadyButton() {
	var readyBtn = document.querySelector('.btn-primary.btn-lg[data-ready-btn]');
	if (!readyBtn) {
		readyBtn = document.querySelectorAll('.btn-primary.btn-lg')[0];
	}

	if (readyBtn) {
		if (myReadyStatus) {
			readyBtn.classList.add('active');
			readyBtn.style.backgroundColor = '';
			readyBtn.innerHTML = '<i class="fas fa-check-square" style="width: 20px; height: 20px;"></i> READY';
		} else {
			readyBtn.classList.remove('active');
			readyBtn.style.backgroundColor = '#4e73df';
			readyBtn.style.border = '';
			readyBtn.innerHTML = '<i class="far fa-square" style="width: 20px; height: 20px;"></i>  READY';
		}
	}
}

function initializeReadyUI() {
	if (initialized) {
		return;
	}
	initialized = true;

	console.log('Initializing Ready UI for userId:', userId);

	var savedReadyStatus = localStorage.getItem('readyStatus_' + roomCode);
	if (savedReadyStatus) {
		readyStatus = JSON.parse(savedReadyStatus);
		myReadyStatus = readyStatus[userId] || false;
	}

	myReadyStatus = readyStatus[userId] || false;

	Object.keys(readyStatus).forEach(function(participantUserId) {
		updateParticipantCardStatus(parseInt(participantUserId), readyStatus[participantUserId]);
	});

	updateReadyButton();
}

// ========== 퀴즈 선택 함수들 ==========

/**
 * 퀴즈 선택 모달 열기
 */
function openSelectQuizModal() {
	console.log('퀴즈 선택 모달 열기');

	// 방장만 퀴즈 선택 가능
	if (!isRoomMaster) {
		alert('방장만 퀴즈를 선택할 수 있습니다.');
		return;
	}

	$('#selectQuizModal').modal('show');
	loadQuizList();
}

/**
 * 퀴즈 목록 로드
 */
function loadQuizList() {
	const spinner = document.getElementById('quizLoadingSpinner');
	const container = document.getElementById('quizListContainer');
	const emptyState = document.getElementById('emptyQuizState');
	const quizList = document.getElementById('quizList');

	// UI 초기화
	spinner.style.display = 'block';
	container.style.display = 'none';
	emptyState.style.display = 'none';
	quizList.innerHTML = '';

	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	fetch('/quiz/list', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('퀴즈 목록 조회 실패');
			}
			return response.json();
		})
		.then(quizzes => {
			console.log('✅ 퀴즈 목록 조회 성공:', quizzes);

			spinner.style.display = 'none';

			if (!quizzes || quizzes.length === 0) {
				emptyState.style.display = 'block';
			} else {
				container.style.display = 'block';
				renderQuizzes(quizzes);
			}
		})
		.catch(error => {
			console.error('❌ 퀴즈 목록 조회 실패:', error);
			spinner.style.display = 'none';
			emptyState.style.display = 'block';
		});
}

/**
 * 퀴즈 목록 렌더링
 */
function renderQuizzes(quizzes) {
	const quizList = document.getElementById('quizList');
	quizList.innerHTML = '';

	// 현재 선택된 퀴즈 ID 가져오기
	const selectedQuizData = localStorage.getItem('selectedQuiz_' + roomCode);
	const selectedQuizId = selectedQuizData ? JSON.parse(selectedQuizData).id : null;
	console.log('현재 선택된 퀴즈 ID:', selectedQuizId);

	quizzes.forEach(quiz => {
		const isSelected = quiz.quizId === selectedQuizId;

		const quizItem = document.createElement('a');
		quizItem.href = 'javascript:void(0)';
		quizItem.className = 'list-group-item list-group-item-action';

		// 👈 선택 상태에 따른 스타일
		if (isSelected) {
			quizItem.classList.add('list-group-item-success');
			quizItem.style.backgroundColor = '#d4edda';
			quizItem.style.borderLeft = '4px solid #28a745';
			quizItem.style.boxShadow = '0 2px 8px rgba(40, 167, 69, 0.2)';
		} else {
			quizItem.style.opacity = '0.6';
			quizItem.style.color = '#666';
		}
		quizItem.style.cursor = 'pointer';
		quizItem.style.transition = 'all 0.2s ease';

		// 👈 체크 표시 조건부 렌더링!
		const checkIcon = isSelected ? '<i class="fas fa-check-circle text-success mr-2"></i>' : '';
		const buttonText = isSelected ? '선택됨' : '선택';
		const buttonClass = isSelected ? 'btn-success' : 'btn-outline-success';

		quizItem.innerHTML = `
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <h6 class="mb-1 font-weight-bold ${isSelected ? 'text-success' : 'text-muted'}">
                        ${checkIcon}${escapeHtml(quiz.title)}
                    </h6>
                    ${quiz.description ? `
                        <p class="mb-1 small ${isSelected ? 'text-success' : 'text-muted'}">
                            ${escapeHtml(quiz.description)}
                        </p>
                    ` : ''}
                </div>
                <button type="button" class="btn btn-sm ${buttonClass} ml-2" 
                        onclick="selectQuiz(${quiz.quizId})">
                    ${isSelected ? '<i class="fas fa-check"></i>' : ''}
                    ${buttonText}
                </button>
            </div>
        `;

		quizItem.addEventListener('mouseenter', function() {
			if (!isSelected) {
				this.style.opacity = '0.9';
				this.style.transform = 'translateX(4px)';
			}
		});
		quizItem.addEventListener('mouseleave', function() {
			if (!isSelected) {
				this.style.opacity = '0.6';
				this.style.transform = 'translateX(0)';
			}
		});

		quizList.appendChild(quizItem);
	});
}


/**
 * 퀴즈 선택
 */
function selectQuiz(quizId) {
	console.log('퀴즈 선택:', quizId);

	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	fetch(`/api/room/${roomCode}/select-quiz`, {
		method: 'POST',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/x-www-form-urlencoded'
		},
		body: `quizId=${quizId}`
	})
		.then(response => {
			if (!response.ok) {
				return response.json().then(data => {
					throw new Error(data.message || '퀴즈 선택 실패');
				});
			}
			return response.json();
		})
		.then(data => {
			if (data.success) {
				console.log('✅ 퀴즈 선택 성공:', data);

				// 로컬스토리지에 선택된 퀴즈 저장
				localStorage.setItem('selectedQuiz_' + roomCode, JSON.stringify({
					id: quizId,
					title: data.quizTitle
				}));

				// 모달 닫기
				$('#selectQuizModal').modal('hide');

				// 화면 업데이트 (필요시)
				alert('퀴즈 "' + data.quizTitle + '"이(가) 선택되었습니다!');
			} else {
				alert('퀴즈 선택 실패: ' + data.message);
			}
		})
		.catch(error => {
			console.error('❌ 퀴즈 선택 중 오류:', error);
			alert('퀴즈 선택 중 오류가 발생했습니다: ' + error.message);
		});
}

/**
 * WebSocket에서 퀴즈 선택 알림 받기
 */
function handleQuizSelection(quizData) {
	console.log('퀴즈 선택 알림 받음:', quizData);

	// 로컬스토리지에 저장
	localStorage.setItem('selectedQuiz_' + roomCode, JSON.stringify({
		id: quizData.quizId,
		title: quizData.quizTitle
	}));

	// 채팅에 메시지 표시
	var messagesDiv = document.getElementById('messages');
	var msgDiv = document.createElement('div');
	msgDiv.innerHTML = '<strong style="color: #007bff;">📚 시스템:</strong> <em>' +
		escapeHtml(quizData.quizTitle) + '이(가) 선택되었습니다.</em>';
	msgDiv.style.padding = '8px';
	msgDiv.style.marginBottom = '8px';
	msgDiv.style.borderBottom = '1px solid #eee';
	msgDiv.style.color = '#666';
	msgDiv.style.fontStyle = 'italic';
	messagesDiv.appendChild(msgDiv);
	messagesDiv.scrollTop = messagesDiv.scrollHeight;
}



function handleQuizStart(quizData) {
	console.log('🚀 QUIZ_START 신호 수신:', quizData);

	// 기존 카운트다운 중지
	stopQuizCountdown();

	// 컨테이너 표시
	var container = document.getElementById('quizStartContainer');
	if (container) {
		console.log('✅ quizStartContainer 찾음');
		container.style.display = 'block';
	} else {
		console.error('❌ quizStartContainer를 찾을 수 없습니다');
	}

	// 카운트다운 시작
	startQuizCountdown(5);
}

function startQuizCountdown(seconds) {
	console.log('⏱️ 카운트다운 시작:', seconds);

	let remaining = seconds;

	var numberDiv = document.getElementById('countdownNumber');
	var messageDiv = document.getElementById('countdownMessage');  // 👈 이걸 찾기

	console.log('numberDiv:', numberDiv);
	console.log('messageDiv:', messageDiv);

	// 초기값 설정
	if (numberDiv) {
		numberDiv.textContent = remaining;
	}
	if (messageDiv) {
		messageDiv.innerHTML = '<strong style="color: #FFD700;">' + remaining + '초</strong> 안에 퀴즈 페이지로 이동합니다';
	}

	// 1초마다 감소
	quizCountdownInterval = setInterval(function() {
		remaining--;

		console.log('카운트다운:', remaining + '초');

		// 숫자 업데이트
		if (numberDiv) {
			numberDiv.textContent = remaining;
		}
		if (messageDiv) {
			messageDiv.innerHTML = '<strong style="color: #FFD700;">' + remaining + '초</strong> 안에 퀴즈 페이지로 이동합니다';
		}

		if (remaining <= 0) {
			stopQuizCountdown();
			window.location.href = '/quiz/' + roomCode;
		}
	}, 1000);

	// 5초 후 강제 이동 (보안용)
	quizCountdownTimer = setTimeout(function() {
		stopQuizCountdown();
		window.location.href = '/quiz/' + roomCode;
	}, seconds * 1000);
}


function stopQuizCountdown() {
	console.log('⏹️ 카운트다운 중지');

	if (quizCountdownInterval) {
		clearInterval(quizCountdownInterval);
		quizCountdownInterval = null;
	}
	if (quizCountdownTimer) {
		clearTimeout(quizCountdownTimer);
		quizCountdownTimer = null;
	}

	// 컨테이너 숨기기
	var container = document.getElementById('quizStartContainer');
	if (container) {
		container.style.display = 'none';
	}
}




// ========== 친구 초대 기능 ==========

/**
 * 친구 초대 모달 열기
 */
function openInviteFriendModal() {
	console.log('친구 초대 모달 열기');

	var body = document.body;
	currentQuizRoomId = body.getAttribute('data-room-code');
	console.log('현재 대기방 코드:', currentQuizRoomId);

	var inviteModal = document.getElementById('inviteFriendModal');
	if (inviteModal) {
		$('#inviteFriendModal').modal('show');
		loadFriendsForInvite();
	} else {
		console.error('inviteFriendModal을 찾을 수 없습니다');
	}
}

/**
 * 친구 목록 조회
 */
function loadFriendsForInvite() {
	console.log('친구 목록 조회 중...');

	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	document.getElementById('friendLoadingSpinner').style.display = 'block';
	document.getElementById('friendListForInvite').style.display = 'none';

	fetch('/api/friends/all', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	})
		.then(response => {
			if (!response.ok) {
				throw new Error('친구 목록 조회 실패');
			}
			return response.json();
		})
		.then(data => {
			console.log('✅ 친구 목록 조회 성공:', data);

			if (typeof displayFriendListForInvite === 'function') {
				displayFriendListForInvite(data.accepted);
			} else {
				displayFriendListForInviteLocal(data.accepted);
			}
		})
		.catch(error => {
			console.error('❌ 친구 목록 조회 실패:', error);
			document.getElementById('friendLoadingSpinner').style.display = 'none';
			document.getElementById('friendListForInvite').innerHTML =
				'<p class="text-danger text-center">친구 목록을 불러올 수 없습니다.</p>';
			document.getElementById('friendListForInvite').style.display = 'block';
		});
}

/**
 * 친구 목록 표시 (waitroom.js에서)
 */
function displayFriendListForInviteLocal(friends) {
	console.log('친구 목록 표시:', friends);

	document.getElementById('friendLoadingSpinner').style.display = 'none';

	let html = '';

	if (!friends || friends.length === 0) {
		html = '<p class="text-muted text-center p-3">초대할 친구가 없습니다.</p>';
	} else {
		friends.forEach(function(friend) {
			html += `
				<div class="card mb-2 p-3 d-flex flex-row justify-content-between align-items-center">
					<div class="d-flex align-items-center flex-grow-1">
						<img src="${friend.profileImage || '/img/default-avatar.png'}" 
							 class="rounded-circle mr-3" width="40" height="40" alt="프로필"
							 onerror="this.src='/img/default-avatar.png'">
						<div>
							<strong>${escapeHtml(friend.username)}</strong><br>
							<small class="text-muted">${escapeHtml(friend.email)}</small>
						</div>
					</div>
					<button type="button" class="btn btn-sm btn-primary ml-2 invite-friend-btn"
							data-email="${friend.email}"
							data-username="${friend.username}">
						<i class="fas fa-check"></i> 초대
					</button>
				</div>
			`;
		});
	}

	document.getElementById('friendListForInvite').innerHTML = html;
	document.getElementById('friendListForInvite').style.display = 'block';
}

/**
 * 친구를 대기방으로 초대
 */
function inviteFriendToQuizRoom(friendEmail, friendName) {
	if (!currentQuizRoomId) {
		alert('대기방을 찾을 수 없습니다');
		return;
	}

	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	console.log('친구 초대 메시지 발송:', {
		roomCode: currentQuizRoomId,
		email: friendEmail,
		name: friendName
	});

	// 초대 버튼
	const $btn = event.target;
	$btn.disabled = true;
	$btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 초대 중...';

	// ⭐ Step 1: 먼저 friendshipId 조회
	fetch('/api/friends/all', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	})
		.then(response => {
			if (!response.ok) throw new Error('친구 목록 조회 실패');
			return response.json();
		})
		.then(data => {
			// 수락된 친구 중에서 해당 이메일 찾기
			const friendshipList = data.accepted || [];
			const targetFriendship = friendshipList.find(friend => friend.email === friendEmail);

			if (!targetFriendship) {
				throw new Error('친구 관계를 찾을 수 없습니다. 먼저 친구 요청을 수락해주세요.');
			}

			const friendshipId = targetFriendship.friendshipId || targetFriendship.id;
			console.log('✅ FriendshipID 조회 완료:', friendshipId);

			// ⭐ Step 2: 초대 메시지 발송
			return sendInvitationMessage(friendshipId, friendName, $btn);
		})
		.catch(error => {
			console.error('❌ 초대 실패:', error);
			alert('초대 실패: ' + error.message);

			// 버튼 복구
			$btn.disabled = false;
			$btn.innerHTML = '<i class="fas fa-check"></i> 초대';
		});
}

/**
 * ⭐ 실제 초대 메시지 발송
 */
function sendInvitationMessage(friendshipId, friendName, $btn) {
	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	// 초대 메시지 내용 구성
	const invitationMessage = `🎮 ${username}님이 퀴즈 게임에 초대했습니다!
방 코드: ${currentQuizRoomId}

초대를 수락하려면 아래 링크를 클릭하세요:
${window.location.origin}/quiz/wait-room/${currentQuizRoomId}`;

	return new Promise((resolve, reject) => {
		fetch('/api/friend-messages/send', {
			method: 'POST',
			headers: {
				[csrfHeader]: csrfToken,
				'Content-Type': 'application/x-www-form-urlencoded'
			},
			body: 'friendshipId=' + encodeURIComponent(friendshipId) +
				'&content=' + encodeURIComponent(invitationMessage)
		})
			.then(response => {
				if (!response.ok) {
					return response.text().then(text => {
						throw new Error(text || '초대 메시지 발송 실패');
					});
				}
				return response.json();
			})
			.then(data => {
				console.log('✅ 초대 메시지 발송 성공');
				alert(friendName + '님에게 초대 메시지를 보냈습니다!\n\n친구가 메시지를 수락할 때까지 기다려주세요.');

				// UI 피드백
				$btn.innerHTML = '<i class="fas fa-check"></i> 초대됨';
				$btn.disabled = true;
				$btn.classList.add('btn-success');
				$btn.classList.remove('btn-primary');

				// 3초 후 모달 닫기
				setTimeout(function() {
					$('#inviteFriendModal').modal('hide');
				}, 1500);

				resolve();
			})
			.catch(error => {
				console.error('❌ 초대 메시지 발송 실패:', error);
				throw error;
			});
	});
}

/**
 * ⭐ 추가 기능: 초대 메시지 수신 및 처리
 */
function handleInvitationMessage(msg) {
	// 메시지 내용에서 "방 코드" 확인
	if (msg.messageText && msg.messageText.includes('방 코드:')) {
		// 초대 메시지임을 감지
		const roomCodeMatch = msg.messageText.match(/방 코드:\s*(\w+)/);
		const roomCode = roomCodeMatch ? roomCodeMatch[1] : null;

		if (roomCode) {
			showInvitationBanner(msg.senderName, roomCode, msg.id);
		}
	}
}

/**
 * ⭐ 초대 배너 표시 (화면 상단)
 */
function showInvitationBanner(senderName, roomCode, messageId) {
	// 이미 표시된 초대가 있으면 무시
	if (document.getElementById('invitation-banner-' + messageId)) {
		return;
	}

	const banner = document.createElement('div');
	banner.id = 'invitation-banner-' + messageId;
	banner.className = 'alert alert-info alert-dismissible fade show';
	banner.style.cssText = 'position: fixed; top: 70px; left: 20px; right: 20px; z-index: 9999; box-shadow: 0 4px 6px rgba(0,0,0,0.1);';

	banner.innerHTML = `
		<div class="d-flex align-items-center justify-content-between">
			<div>
				<h5 class="mb-1">
					<i class="fas fa-envelope mr-2"></i>
					${senderName}님의 게임 초대
				</h5>
				<p class="mb-0 small">
					방 코드: <strong>${roomCode}</strong>
				</p>
			</div>
			<div>
				<button type="button" class="btn btn-success btn-sm mr-2" 
						onclick="acceptInvitation('${roomCode}', '${messageId}')">
					<i class="fas fa-check"></i> 수락
				</button>
				<button type="button" class="btn btn-outline-secondary btn-sm" 
						onclick="declineInvitation('${messageId}')">
					<i class="fas fa-times"></i> 거절
				</button>
			</div>
		</div>
		<button type="button" class="close" data-dismiss="alert">
			<span>&times;</span>
		</button>
	`;

	document.body.insertBefore(banner, document.body.firstChild);
}

/**
 * ⭐ 초대 수락
 */
function acceptInvitation(roomCode, messageId) {

	// 배너 제거
	const banner = document.getElementById('invitation-banner-' + messageId);
	if (banner) {
		banner.style.opacity = '0';
		banner.style.transition = 'opacity 0.3s';
		setTimeout(() => banner.remove(), 300);
	}

	// 해당 방으로 이동
	setTimeout(() => {
		window.location.href = '/waitroom/' + roomCode;
	}, 300);
}

/**
 * ⭐ 초대 거절
 */
function declineInvitation(messageId) {
	console.log('❌ 초대 거절:', messageId);

	// 배너 제거
	const banner = document.getElementById('invitation-banner-' + messageId);
	if (banner) {
		banner.style.opacity = '0';
		banner.style.transition = 'opacity 0.3s';
		setTimeout(() => banner.remove(), 300);
	}
}

window.inviteFriendToQuizRoom = inviteFriendToQuizRoom;
window.handleInvitationMessage = handleInvitationMessage;
window.acceptInvitation = acceptInvitation;
window.declineInvitation = declineInvitation;

// ========== DOM 준비 후 초기화 ==========
document.addEventListener('DOMContentLoaded', function() {
	if (initialized) {
		return;
	}
	initialized = true;

	var body = document.body;
	roomCode = body.getAttribute('data-room-code');
	username = body.getAttribute('data-guest-nickname');
	isRoomMaster = body.getAttribute('data-is-room-master') === 'true';

	var userIdAttr = body.getAttribute('data-user-id');
	var guestIdAttr = body.getAttribute('data-guest-id');

	if (guestIdAttr && guestIdAttr !== '') {
		// ✅ getJavaHashCode() 사용 (Math.abs() 제거!)
		userId = getJavaHashCode(guestIdAttr);
		console.log('게스트 userId 계산:', guestIdAttr, '→', userId);
	} else {
		// 회원: 직접 사용
		userId = parseInt(userIdAttr) || 0;
		console.log('회원 userId:', userId);
	}

	console.log('Initialized with:', { roomCode, username, isRoomMaster, userId });

	initialParticipants = [];
	document.querySelectorAll('[data-user-id]').forEach(function(card) {
		var idStr = card.getAttribute('data-user-id');
		var pid = parseInt(idStr, 10);
		if (!isNaN(pid)) {
			initialParticipants.push(pid);
		}
	});

	initWebSocket();

	var formEl = document.querySelector('form');
	if (formEl) {
		formEl.addEventListener('submit', function(e) {
			e.preventDefault();
			sendMessage();
		});
	}

	var readyBtnElements = document.querySelectorAll('.btn-primary.btn-lg');
	readyBtnElements.forEach(function(btn) {
		if (btn.textContent.includes('READY')) {
			btn.addEventListener('click', function(e) {
				e.preventDefault();
				toggleReady();
			});
		}
	});

	setTimeout(function() {
		initializeReadyUI();
	}, 500);

	// ✅ 초대 친구 버튼 이벤트 바인딩
	document.addEventListener('click', function(e) {
		if (e.target.classList.contains('invite-friend-btn')) {
			const email = e.target.getAttribute('data-email');
			const username = e.target.getAttribute('data-username');
			inviteFriendToQuizRoom(email, username);
		}
	});
});

// ✅ 이 함수 추가 (맨 아래)
function getJavaHashCode(str) {
	let hash = 0;
	for (let i = 0; i < str.length; i++) {
		const char = str.charCodeAt(i);
		hash = ((hash << 5) - hash) + char;
		hash = hash & hash;  // 32-bit signed integer 유지
	}
	return hash;  // 음수도 그대로 반환!
}

