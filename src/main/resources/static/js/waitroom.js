var roomCode = null;
var username = null;
var isRoomMaster = null;
var userId = null;

var currentVoteId = null;
var currentVoteChoice = null;
var voteResults = { AGREE: 0, DISAGREE: 0 };

var participants = [];
var teamAssignment = {};
var currentTeamMode = null;
var currentTeamCount = 0;
var stompClient = null;
var currentQuizRoomId = null;

var readyStatus = {};
var myReadyStatus = false;
var initialized = false;

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
			handleReadyUpdate(readyData);
		});

		stompClient.subscribe('/topic/participants/' + roomCode, function(message) {
			var data = JSON.parse(message.body);
			if (data.type === 'PARTICIPANT_UPDATE') {
				updateParticipantUI(data.participants);
			}
		});

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

		var teamSelectBtn = document.getElementById('teamSelectBtn');
		if (teamSelectBtn) {
			teamSelectBtn.style.display = isRoomMaster ? 'inline-block' : 'none';
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

	var cardBody = document.querySelector('.card.shadow.mb-4 .card-body');

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

			newCard.innerHTML =
				'<div class="mt-2">' +
				'<img src="' + (participant.avatarUrl ? participant.avatarUrl : '/img/default-avatar.png') + '" ' +
				'class="rounded-circle mb-2" width="55" height="55" alt="avatar">' +
				'</div>' +
				'<div class="font-weight-bold text-primary">' + participant.nickname + '</div>';

			cardBody.appendChild(newCard);
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

function submitVoteCreate() {
	var title = document.getElementById('voteTitle').value.trim();
	var content = document.getElementById('voteContent').value.trim();

	if (!title) {
		alert('투표 제목을 입력해주세요.');
		return;
	}

	if (stompClient.connected) {
		var voteData = {
			type: 'START',
			voteId: Date.now(),
			question: title,
			description: content,
			creator: username,
			timestamp: new Date().getTime()
		};

		stompClient.send('/app/vote/start/' + roomCode, {}, JSON.stringify(voteData));
		$('#createVoteModal').modal('hide');
	} else {
		alert('WebSocket이 연결되지 않았습니다.');
	}
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
			creator: voteData.creator
		}));
	} else if (voteData.type === 'UPDATE') {
		updateVoteResults(voteData.results);
		localStorage.setItem('voteResults_' + roomCode + '_' + voteData.voteId, JSON.stringify(voteData.results));
	} else if (voteData.type === 'END') {
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

function openTeamSelectModal() {
	participants = [];
	teamAssignment = {};

	var savedTeamAssignment = localStorage.getItem('teamAssignment_' + roomCode);
	if (savedTeamAssignment) {
		teamAssignment = JSON.parse(savedTeamAssignment);
	}

	var savedTeamMode = localStorage.getItem('teamMode_' + roomCode);
	if (savedTeamMode) {
		currentTeamMode = savedTeamMode;
		currentTeamCount = parseInt(localStorage.getItem('teamCount_' + roomCode) || '0');
	}

	document.querySelectorAll('[data-user-id]').forEach(function(el) {
		var userId = el.getAttribute('data-user-id');
		var nicknameEl = el.querySelector('.font-weight-bold');
		var nickname = nicknameEl ? nicknameEl.textContent : 'Unknown';

		if (userId && nickname && el.tagName !== 'BODY') {
			participants.push({ id: parseInt(userId), nickname: nickname });
			console.log('Added participant:', { id: userId, nickname: nickname });
		}
	});

	console.log('Participants:', participants);
	initializeTeamModal();
	$('#teamSelectModal').modal('show');
}

function initializeTeamModal() {
	document.querySelectorAll('input[name="mode"]').forEach(function(el) { el.checked = false; });
	document.querySelectorAll('input[name="teamCount"]').forEach(function(el) { el.checked = false; });

	if (currentTeamMode) {
		var modeEl = document.querySelector('input[name="mode"][value="' + currentTeamMode + '"]');
		if (modeEl) modeEl.checked = true;

		if (currentTeamMode === 'TEAM') {
			document.getElementById('teamCountDiv').style.display = 'block';
			var countEl = document.querySelector('input[name="teamCount"][value="' + currentTeamCount + '"]');
			if (countEl) countEl.checked = true;
			setupDragDrop(currentTeamCount);
			document.getElementById('dragDropArea').style.display = 'block';
		}
	}

	document.querySelectorAll('input[name="mode"]').forEach(function(el) {
		el.addEventListener('change', function() {
			console.log('Mode changed to:', this.value);
			currentTeamMode = this.value;
			if (currentTeamMode === 'TEAM') {
				document.getElementById('teamCountDiv').style.display = 'block';
				document.getElementById('dragDropArea').style.display = 'none';
			} else {
				document.getElementById('teamCountDiv').style.display = 'none';
				document.getElementById('dragDropArea').style.display = 'none';
				teamAssignment = {};
			}
		});
	});

	document.querySelectorAll('input[name="teamCount"]').forEach(function(el) {
		el.addEventListener('change', function() {
			console.log('Team count changed to:', this.value);
			var teamCount = parseInt(this.value);
			currentTeamCount = teamCount;
			setupDragDrop(teamCount);
			document.getElementById('dragDropArea').style.display = 'block';
		});
	});

	renderUnassignedParticipants();
}

function renderUnassignedParticipants() {
	var unassignedArea = document.getElementById('unassignedArea');
	unassignedArea.innerHTML = '';

	participants.forEach(function(participant) {
		if (!teamAssignment[participant.id]) {
			var participantEl = document.createElement('div');
			participantEl.className = 'draggable-participant';
			participantEl.draggable = true;
			participantEl.setAttribute('data-user-id', participant.id);
			participantEl.textContent = participant.nickname;

			participantEl.addEventListener('dragstart', function(e) {
				e.dataTransfer.effectAllowed = 'move';
				e.dataTransfer.setData('userId', String(participant.id));
				console.log('🎯 DRAGSTART - userId:', participant.id);
			});

			participantEl.addEventListener('dragend', function(e) {
				console.log('🎯 DRAGEND');
			});

			unassignedArea.appendChild(participantEl);
		}
	});

	console.log('Unassigned participants rendered');
}

function setupDragDrop(teamCount) {
	console.log('Setting up drag drop for', teamCount, 'teams');

	var teamsContainer = document.getElementById('teamsContainer');
	teamsContainer.innerHTML = '';

	var savedTeamNames = {};
	var savedTeamNamesJson = localStorage.getItem('teamNames_' + roomCode);
	if (savedTeamNamesJson) {
		savedTeamNames = JSON.parse(savedTeamNamesJson);
	}

	for (var i = 1; i <= teamCount; i++) {
		var colDiv = document.createElement('div');
		colDiv.className = 'col-md-6 mb-3';

		var teamName = savedTeamNames[i] || ('팀 ' + i);

		var teamBox = document.createElement('div');

		teamBox.innerHTML =
			'<div class="form-group mb-2">' +
			'<input type="text" id="teamName_' + i + '" class="form-control font-weight-bold text-center" value="' + teamName + '" placeholder="팀 이름 입력">' +
			'</div>' +
			'<div class="team-members" data-team="' + i + '" style="min-height: 100px; padding: 10px; border: 2px dashed #ccc; border-radius: 6px;"></div>';

		colDiv.appendChild(teamBox);
		teamsContainer.appendChild(colDiv);
	}

	document.querySelectorAll('.team-members').forEach(function(teamMembersDiv) {
		var teamNum = teamMembersDiv.getAttribute('data-team');
		console.log('Registering drop events for team', teamNum);

		teamMembersDiv.addEventListener('dragover', function(e) {
			e.preventDefault();
			e.stopPropagation();
			e.dataTransfer.dropEffect = 'move';
			teamMembersDiv.closest('.team-box').classList.add('drag-over');
			console.log('🔄 DRAGOVER - team:', teamNum);
		});

		teamMembersDiv.addEventListener('dragleave', function(e) {
			e.stopPropagation();
			teamMembersDiv.closest('.team-box').classList.remove('drag-over');
		});

		teamMembersDiv.addEventListener('drop', function(e) {
			e.preventDefault();
			e.stopPropagation();

			console.log('💧 DROP EVENT FIRED');

			var userId = e.dataTransfer.getData('userId');
			var teamNumber = parseInt(teamMembersDiv.getAttribute('data-team'));

			console.log('Drop data - userId:', userId, 'teamNumber:', teamNumber);

			if (userId && teamNumber) {
				var userIdInt = parseInt(userId);
				teamAssignment[userIdInt] = teamNumber;
				console.log('✅ Assigned userId', userIdInt, 'to team', teamNumber);
				console.log('Current assignments:', teamAssignment);

				renderTeamAssignment();
				renderUnassignedParticipants();
			} else {
				console.log('❌ Invalid userId or teamNumber');
			}

			teamMembersDiv.closest('.team-box').classList.remove('drag-over');
		});
	});

	renderTeamAssignment();
}

function renderTeamAssignment() {
	console.log('Rendering team assignments:', teamAssignment);

	document.querySelectorAll('.team-members').forEach(function(el) { el.innerHTML = ''; });

	Object.keys(teamAssignment).forEach(function(userId) {
		var teamNumber = teamAssignment[userId];
		var participant = participants.find(function(p) { return p.id == userId; });

		if (participant) {
			var teamMemberEl = document.createElement('div');
			teamMemberEl.className = 'team-member';
			teamMemberEl.innerHTML = participant.nickname + ' <span class="ml-2" style="cursor:pointer; opacity:0.7; font-weight:bold;" onclick="removeFromTeam(' + userId + ')">✕</span>';

			var targetTeam = document.querySelector('[data-team="' + teamNumber + '"]');
			if (targetTeam) {
				targetTeam.appendChild(teamMemberEl);
			}
		}
	});
}

function removeFromTeam(userId) {
	console.log('Removing userId', userId, 'from teams');
	delete teamAssignment[userId];
	renderTeamAssignment();
	renderUnassignedParticipants();
}

function updateParticipantList() {
	console.log('Updating participant list with team info');

	var savedTeamAssignment = localStorage.getItem('teamAssignment_' + roomCode);
	var savedTeamNames = localStorage.getItem('teamNames_' + roomCode);

	if (!savedTeamAssignment || !savedTeamNames) {
		console.log('No team assignment data found');
		return;
	}

	teamAssignment = JSON.parse(savedTeamAssignment);
	var teamNames = JSON.parse(savedTeamNames);

	var participantCards = document.querySelectorAll('.card[data-user-id]');

	participantCards.forEach(function(card) {
		var userId = parseInt(card.getAttribute('data-user-id'));
		var teamNumber = teamAssignment[userId];

		var existingBadge = card.querySelector('.team-badge');
		if (existingBadge) {
			existingBadge.remove();
		}

		if (teamNumber && teamNames[teamNumber]) {
			var badge = document.createElement('div');
			badge.className = 'team-badge';
			badge.style.cssText = 'position: absolute; top: 5px; right: 5px; background-color: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold;';
			badge.textContent = teamNames[teamNumber];
			card.appendChild(badge);
		}
	});
}

function submitTeamAssignment() {
	var mode = document.querySelector('input[name="mode"]:checked') ?
		document.querySelector('input[name="mode"]:checked').value : null;

	if (!mode) {
		alert('게임 모드를 선택해주세요.');
		return;
	}

	if (mode === 'TEAM') {
		var teamCountEl = document.querySelector('input[name="teamCount"]:checked');
		var teamCount = teamCountEl ? teamCountEl.value : null;

		if (!teamCount) {
			alert('팀 개수를 선택해주세요.');
			return;
		}

		var unassignedCount = participants.length - Object.keys(teamAssignment).length;
		if (unassignedCount > 0) {
			alert('모든 참가자를 팀에 배정해주세요. (' + unassignedCount + '명)');
			return;
		}

		var teamNames = {};
		for (var i = 1; i <= parseInt(teamCount); i++) {
			var teamNameInput = document.getElementById('teamName_' + i);
			var teamName = teamNameInput ? teamNameInput.value.trim() : '';
			teamNames[i] = teamName || ('팀 ' + i);
		}

		localStorage.setItem('teamAssignment_' + roomCode, JSON.stringify(teamAssignment));
		localStorage.setItem('teamMode_' + roomCode, 'TEAM');
		localStorage.setItem('teamCount_' + roomCode, teamCount);
		localStorage.setItem('teamNames_' + roomCode, JSON.stringify(teamNames));
	} else {
		localStorage.setItem('teamMode_' + roomCode, 'INDIVIDUAL');
		localStorage.removeItem('teamAssignment_' + roomCode);
		localStorage.removeItem('teamNames_' + roomCode);
	}

	alert('팀 설정이 저장되었습니다.');
	$('#teamSelectModal').modal('hide');
	updateParticipantList();
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
	console.log('Ready update received:', readyData);

	var receivedUserId = readyData.userId;
	var isReady = readyData.isReady;

	readyStatus[receivedUserId] = isReady;
	localStorage.setItem('readyStatus_' + roomCode, JSON.stringify(readyStatus));

	console.log('Updated readyStatus:', readyStatus);

	if (receivedUserId === userId) {
		console.log('Self update - syncing myReadyStatus to:', isReady);
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

	if (isReady) {
		var readyBadge = document.createElement('div');
		readyBadge.className = 'ready-badge';
		readyBadge.style.cssText = 'position: absolute; bottom: 5px; right: 5px; background-color: #28a745; color: white; padding: 4px 8px; border-radius: 4px; font-size: 11px; font-weight: bold; display: flex; align-items: center; gap: 4px;';
		readyBadge.innerHTML = '<i class="fas fa-check-circle"></i> READY';
		card.style.position = 'relative';
		card.appendChild(readyBadge);
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

	quizzes.forEach(quiz => {
		const quizItem = document.createElement('a');
		quizItem.href = 'javascript:void(0)';
		quizItem.className = 'list-group-item list-group-item-action';
		quizItem.style.cursor = 'pointer';

		quizItem.innerHTML = `
            <div class="d-flex justify-content-between align-items-start">
                <div class="flex-grow-1">
                    <h6 class="mb-1 font-weight-bold text-dark">
                        ${escapeHtml(quiz.title)}
                    </h6>
                    ${quiz.description ? `
                        <p class="mb-1 text-muted small">
                            ${escapeHtml(quiz.description)}
                        </p>
                    ` : ''}
                </div>
                <button type="button" class="btn btn-sm btn-primary ml-2" 
                        onclick="selectQuiz(${quiz.quizId})">
                    <i class="fas fa-check"></i>
                </button>
            </div>
        `;

		quizItem.addEventListener('mouseenter', function() {
			this.style.backgroundColor = '#f8f9fa';
		});
		quizItem.addEventListener('mouseleave', function() {
			this.style.backgroundColor = '';
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

				// 성공 메시지
				var messagesDiv = document.getElementById('messages');
				var msgDiv = document.createElement('div');
				msgDiv.innerHTML = '<strong style="color: #28a745;">✓ 시스템:</strong> <em>' +
					escapeHtml(data.quizTitle) + '이(가) 선택되었습니다.</em>';
				msgDiv.style.padding = '8px';
				msgDiv.style.marginBottom = '8px';
				msgDiv.style.borderBottom = '1px solid #eee';
				msgDiv.style.color = '#666';
				msgDiv.style.fontStyle = 'italic';
				messagesDiv.appendChild(msgDiv);
				messagesDiv.scrollTop = messagesDiv.scrollHeight;

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

	var body = document.body;
	roomCode = body.getAttribute('data-room-code');
	username = body.getAttribute('data-guest-nickname');
	isRoomMaster = body.getAttribute('data-is-room-master') === 'true';
	userId = parseInt(body.getAttribute('data-user-id') || '0');

	console.log('Initialized with:', { roomCode, username, isRoomMaster, userId });

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
