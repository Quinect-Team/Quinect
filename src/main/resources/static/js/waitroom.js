$(document).ready(function() {
	// ========== 전역 변수 ==========
	const $body = $('body');
	let roomCode = $body.data('room-code');
	let username = $body.data('guest-nickname') || 'Guest';
	let isRoomMaster = $body.data('is-room-master') === true || $body.data('is-room-master') === 'true';

	let currentVoteId = null;
	let currentVoteChoice = null;
	let voteResults = { AGREE: 0, DISAGREE: 0 };

	// ========== WebSocket 설정 ==========
	let socket = new SockJS('/ws');
	let stompClient = Stomp.over(socket);

	// WebSocket 접속 및 구독
	stompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);

		// 방 자동 닫기/유지 알림 설정 (예: 30분)
		const ROOM_ALERT_DELAY_MS = 1 * 60 * 1000;

		setTimeout(function() {
			if (!isRoomMaster) return; // 방장만 알림

			const message = "이 방은 생성된 지 30분이 지났습니다.\n방을 닫으시겠습니까?";
			const csrfToken = $('meta[name="_csrf"]').attr('content');
			const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

			if (confirm(message)) {
				$.ajax({
					url: '/waitroom/' + roomCode + '/close',
					method: 'POST',
					beforeSend: function(xhr) {
						if (csrfToken && csrfHeader) {
							xhr.setRequestHeader(csrfHeader, csrfToken);
						}
					},
					success: function(result) {
						if (result === 'OK') {
							alert('방이 닫혔습니다. 메인 화면으로 이동합니다.');
							window.location.href = '/';
						} else if (result === 'FORBIDDEN') {
							alert('방장만 방을 닫을 수 있습니다.');
						} else {
							alert('방을 닫을 수 없습니다: ' + result);
						}
					},
					error: function(xhr) {
						alert('방 닫기 요청 중 오류가 발생했습니다. (' + xhr.status + ')');
					}
				});
			} else {
				// 유지 선택 → 아무 일도 안 함
			}
		}, ROOM_ALERT_DELAY_MS);

		// ✅ LocalStorage에서 이전 채팅 복구
		let savedChats = JSON.parse(localStorage.getItem('chatMessages_' + roomCode) || '[]');
		savedChats.forEach(chat => {
			displayMessage(chat.sender, chat.content);
		});

		// ✅ LocalStorage에서 투표 정보 복구
		let savedVote = localStorage.getItem('currentVote_' + roomCode);
		if (savedVote) {
			let voteInfo = JSON.parse(savedVote);
			currentVoteId = voteInfo.voteId;

			// 저장된 결과도 복구
			let savedResults = localStorage.getItem('voteResults_' + roomCode + '_' + voteInfo.voteId);
			if (savedResults) {
				voteResults = JSON.parse(savedResults);
				// 투표 메시지 다시 표시
				displayVoteMessageInChat(voteInfo);
			}
		}

		// 채팅 구독
		stompClient.subscribe('/topic/chat/' + roomCode, function(message) {
			let msg = JSON.parse(message.body);
			displayMessage(msg.sender, msg.content);
		});

		// 투표 구독
		stompClient.subscribe('/topic/vote/' + roomCode, function(message) {
			let voteData = JSON.parse(message.body);
			handleVoteUpdate(voteData);
		});

		// 방장 버튼 표시
		if (isRoomMaster) {
			$('#voteBtn').show();
			$('#voteBtn').on('click', function() {
				openCreateVoteModal();
			});
		}
	}, function(error) {
		console.error('Error: ' + error);
	});

	// ========== 1. 기존 채팅 기능 ==========
	function displayMessage(sender, content) {
		let msgDiv = $('<div></div>')
			.html('<strong>' + sender + ':</strong> ' + content)
			.css({
				'padding': '8px',
				'margin-bottom': '8px',
				'border-bottom': '1px solid #eee'
			});

		$('#messages').append(msgDiv);
		$('#messages').scrollTop($('#messages')[0].scrollHeight);

		// ✅ LocalStorage에 채팅 저장
		let chatMessages = JSON.parse(localStorage.getItem('chatMessages_' + roomCode) || '[]');
		chatMessages.push({
			sender: sender,
			content: content,
			timestamp: new Date().getTime()
		});
		localStorage.setItem('chatMessages_' + roomCode, JSON.stringify(chatMessages));
	}

	function sendMessage() {
		let text = $('#chat-input').val().trim();
		if (text && stompClient.connected) {
			let chatMessage = {
				sender: username,
				content: text,
			};
			stompClient.send('/app/chat/' + roomCode, {}, JSON.stringify(chatMessage));
			$('#chat-input').val('');
		}
	}

	function copyCode() {
		const code = $('.display-4').text();
		navigator.clipboard.writeText(code)
			.then(() => alert("방 코드가 복사되었습니다: " + code));
	}

	function setReadyStatus(status) {
		stompClient.send('/app/ready/' + roomCode, {}, JSON.stringify({
			sender: username,
			ready: status
		}));
	}

	// 폼 submit 이벤트
	$('form').on('submit', function(e) {
		e.preventDefault();
		sendMessage();
	});

	// ========== 2. 투표 기능 ==========

	// 방장용 투표 시작 모달 열기
	window.openCreateVoteModal = function() {
		$('#voteTitle').val('');
		$('#voteContent').val('');
		$('#createVoteModal').modal('show');
		$('#voteTitle').focus();
	};

	// 투표 생성 제출
	window.submitVoteCreate = function() {
		let title = $('#voteTitle').val().trim();
		let content = $('#voteContent').val().trim();

		if (!title) {
			alert('투표 제목을 입력해주세요.');
			return;
		}

		if (stompClient.connected) {
			let voteData = {
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
	};

	// 사용자용 투표 참가 모달 열기
	window.openParticipateVoteModal = function(voteId, question, description) {
		currentVoteId = voteId;
		currentVoteChoice = null;

		// 투표 정보 표시
		$('#voteQuestion h6').text(question);
		$('#voteDescription').text(description || '');

		// 버튼 초기화
		$('.vote-agree-btn, .vote-disagree-btn').removeClass('selected');

		// 결과 표시
		$('#voteResults').show();
		updateVoteResults(voteResults);

		$('#participateVoteModal').modal('show');
	};

	// 투표 선택 제출
	window.submitVoteChoice = function(choice) {
		if (choice === 'AGREE') {
			$('.vote-agree-btn').addClass('selected');
			$('.vote-disagree-btn').removeClass('selected');
		} else {
			$('.vote-disagree-btn').addClass('selected');
			$('.vote-agree-btn').removeClass('selected');
		}

		currentVoteChoice = choice;

		if (stompClient.connected) {
			let voteSubmission = {
				type: 'VOTE',
				voteId: currentVoteId,
				voter: username,
				choice: choice,
				timestamp: new Date().getTime()
			};

			stompClient.send('/app/vote/submit/' + roomCode, {}, JSON.stringify(voteSubmission));

			// ✅ LocalStorage에 내 선택 저장
			localStorage.setItem('myVoteChoice_' + roomCode + '_' + currentVoteId, choice);
		}
	};

	// 채팅에 투표 메시지 표시
	function displayVoteMessageInChat(voteData) {
		let msgDiv = $('<div></div>')
			.addClass('vote-message')
			.html(`
                    <div class="vote-message-title">🗳️ ${escapeHtml(voteData.question)}</div>
                    ${voteData.description ? '<div class="vote-message-desc">' + escapeHtml(voteData.description) + '</div>' : ''}
                    <div class="vote-message-button" onclick="openParticipateVoteModal(
                        ${voteData.voteId}, 
                        '${voteData.question.replace(/'/g, "\\'")}',
                        '${(voteData.description || '').replace(/'/g, "\\'")}'
                    )">
                        투표에 참가하세요 →
                    </div>
                `);

		$('#messages').append(msgDiv);
		$('#messages').scrollTop($('#messages')[0].scrollHeight);
	}

	// 투표 업데이트 처리
	function handleVoteUpdate(voteData) {
		console.log('투표 업데이트:', voteData);

		if (voteData.type === 'START') {
			displayVoteMessageInChat(voteData);
			currentVoteId = voteData.voteId;
			voteResults = { AGREE: 0, DISAGREE: 0 };

			// ✅ LocalStorage에 현재 투표 정보 저장
			localStorage.setItem('currentVote_' + roomCode, JSON.stringify({
				voteId: voteData.voteId,
				question: voteData.question,
				description: voteData.description,
				creator: voteData.creator
			}));
		} else if (voteData.type === 'UPDATE') {
			updateVoteResults(voteData.results);

			// ✅ LocalStorage에 투표 결과 저장
			localStorage.setItem('voteResults_' + roomCode + '_' + voteData.voteId, JSON.stringify(voteData.results));
		} else if (voteData.type === 'END') {
			// 투표 종료 시 정보 제거
			localStorage.removeItem('currentVote_' + roomCode);
			localStorage.removeItem('voteResults_' + roomCode + '_' + currentVoteId);
			localStorage.removeItem('myVoteChoice_' + roomCode + '_' + currentVoteId);
			currentVoteId = null;
		}
	}

	// 투표 결과 업데이트
	function updateVoteResults(results) {
		voteResults = results;
		const total = results.AGREE + results.DISAGREE || 1;

		const agreePercentage = ((results.AGREE / total) * 100).toFixed(1);
		const disagreePercentage = ((results.DISAGREE / total) * 100).toFixed(1);

		$('#agreeCount').text(results.AGREE + '표');
		$('#disagreeCount').text(results.DISAGREE + '표');

		$('#agreeBar')
			.css('width', agreePercentage + '%')
			.find('span').text(agreePercentage + '%');

		$('#disagreeBar')
			.css('width', disagreePercentage + '%')
			.find('span').text(disagreePercentage + '%');
	}

	// XSS 방지
	function escapeHtml(text) {
		const map = {
			'&': '&amp;',
			'<': '&lt;',
			'>': '&gt;',
			'"': '&quot;',
			"'": '&#039;'
		};
		return text.replace(/[&<>"']/g, m => map[m]);
	}

	// ========== 3. 방 나갈 때 정리 (선택사항) ==========
	function clearRoomData() {
		localStorage.removeItem('chatMessages_' + roomCode);
		localStorage.removeItem('currentVote_' + roomCode);
	}

	// 페이지 떠날 때 (필요하면 주석 해제)
	// $(window).on('beforeunload', function() {
	//     clearRoomData();
	// });

	// ========== 팀 선택 기능 ==========
	let participants = [];
	let teamAssignment = {};
	let currentTeamMode = null;
	let currentTeamCount = 0;

	window.openTeamSelectModal = function() {
		participants = [];
		teamAssignment = {};

		let savedTeamAssignment = localStorage.getItem('teamAssignment_' + roomCode);
		if (savedTeamAssignment) {
			teamAssignment = JSON.parse(savedTeamAssignment);
		}

		let savedTeamMode = localStorage.getItem('teamMode_' + roomCode);
		if (savedTeamMode) {
			currentTeamMode = savedTeamMode;
			currentTeamCount = parseInt(localStorage.getItem('teamCount_' + roomCode) || '0');
		}

		// DOM에서 참가자 가져오기
		document.querySelectorAll('[data-user-id]').forEach(el => {
			const userId = el.getAttribute('data-user-id');
			const nickname = el.querySelector('.font-weight-bold')?.textContent;
			if (userId && nickname) {
				participants.push({ id: userId, nickname: nickname });
			}
		});

		console.log('Participants loaded:', participants);
		initializeTeamModal();
		$('#teamSelectModal').modal('show');
	};

	function initializeTeamModal() {
		document.querySelectorAll('input[name="mode"]').forEach(el => el.checked = false);
		document.querySelectorAll('input[name="teamCount"]').forEach(el => el.checked = false);

		if (currentTeamMode) {
			document.querySelector('input[name="mode"][value="' + currentTeamMode + '"]').checked = true;
			if (currentTeamMode === 'TEAM') {
				document.getElementById('teamCountDiv').style.display = 'block';
				document.querySelector('input[name="teamCount"][value="' + currentTeamCount + '"]').checked = true;
				setupDragDrop(currentTeamCount);
				document.getElementById('dragDropArea').style.display = 'block';
			}
		}

		document.querySelectorAll('input[name="mode"]').forEach(el => {
			el.addEventListener('change', function() {
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

		document.querySelectorAll('input[name="teamCount"]').forEach(el => {
			el.addEventListener('change', function() {
				const teamCount = parseInt(this.value);
				currentTeamCount = teamCount;
				setupDragDrop(teamCount);
				document.getElementById('dragDropArea').style.display = 'block';
			});
		});

		renderUnassignedParticipants();
	}

	function renderUnassignedParticipants() {
		const unassignedArea = document.getElementById('unassignedArea');
		unassignedArea.innerHTML = '';

		participants.forEach(participant => {
			if (!teamAssignment[participant.id]) {
				const participantEl = document.createElement('div');
				participantEl.className = 'draggable-participant';
				participantEl.draggable = true;
				participantEl.dataset.userId = participant.id;
				participantEl.textContent = participant.nickname;

				participantEl.addEventListener('dragstart', function(e) {
					e.dataTransfer.effectAllowed = 'move';
					e.dataTransfer.setData('userId', String(participant.id));
					console.log('🎯 Drag start - userId:', participant.id);
				});

				participantEl.addEventListener('dragend', function(e) {
					console.log('Drag end');
				});

				unassignedArea.appendChild(participantEl);
			}
		});
	}

	function setupDragDrop(teamCount) {
		const teamsContainer = document.getElementById('teamsContainer');
		teamsContainer.innerHTML = '';

		for (let i = 1; i <= teamCount; i++) {
			const teamBox = document.createElement('div');
			teamBox.className = 'col-md-6 mb-3';
			teamBox.innerHTML = `
	            <div class="team-box" data-team-number="${i}">
	                <h6 class="font-weight-bold mb-3">팀 ${i}</h6>
	                <div class="team-members" data-team="${i}"></div>
	            </div>
	        `;
			teamsContainer.appendChild(teamBox);
		}

		const teamMembers = document.querySelectorAll('.team-members');
		teamMembers.forEach(teamMembersDiv => {
			teamMembersDiv.addEventListener('dragover', function(e) {
				e.preventDefault();
				e.stopPropagation();
				e.dataTransfer.dropEffect = 'move';
				this.closest('.team-box').classList.add('drag-over');
				console.log('🔄 Dragover on team', this.dataset.team);
			});

			teamMembersDiv.addEventListener('dragleave', function(e) {
				this.closest('.team-box').classList.remove('drag-over');
			});

			teamMembersDiv.addEventListener('drop', function(e) {
				e.preventDefault();
				e.stopPropagation();

				const userId = e.dataTransfer.getData('userId');
				const teamNumber = parseInt(this.dataset.team);

				console.log('💧 DROP FIRED - userId:', userId, 'teamNumber:', teamNumber);

				if (userId && teamNumber) {
					teamAssignment[userId] = teamNumber;
					console.log('✅ Team assigned:', teamAssignment);
					renderTeamAssignment();
					renderUnassignedParticipants();
				}

				this.closest('.team-box').classList.remove('drag-over');
			});
		});

		renderTeamAssignment();
	}

	function renderTeamAssignment() {
		document.querySelectorAll('.team-members').forEach(el => el.innerHTML = '');

		Object.keys(teamAssignment).forEach(userId => {
			const teamNumber = teamAssignment[userId];
			const participant = participants.find(p => p.id == userId);

			if (participant) {
				const teamMemberEl = document.createElement('div');
				teamMemberEl.className = 'team-member';
				teamMemberEl.innerHTML = participant.nickname + ' <span class="ml-2" style="cursor:pointer; opacity:0.7;" onclick="removeFromTeam(' + userId + ')">✕</span>';

				document.querySelector('[data-team="' + teamNumber + '"]').appendChild(teamMemberEl);
			}
		});
	}

	window.removeFromTeam = function(userId) {
		delete teamAssignment[userId];
		renderTeamAssignment();
		renderUnassignedParticipants();
	};

	window.submitTeamAssignment = function() {
		const mode = document.querySelector('input[name="mode"]:checked')?.value;

		if (!mode) {
			alert('게임 모드를 선택해주세요.');
			return;
		}

		if (mode === 'TEAM') {
			const teamCount = document.querySelector('input[name="teamCount"]:checked')?.value;
			if (!teamCount) {
				alert('팀 개수를 선택해주세요.');
				return;
			}

			const unassignedCount = participants.length - Object.keys(teamAssignment).length;
			if (unassignedCount > 0) {
				alert('모든 참가자를 팀에 배정해주세요. (미배정: ' + unassignedCount + '명)');
				return;
			}

			localStorage.setItem('teamAssignment_' + roomCode, JSON.stringify(teamAssignment));
			localStorage.setItem('teamMode_' + roomCode, 'TEAM');
			localStorage.setItem('teamCount_' + roomCode, teamCount);
		} else {
			localStorage.setItem('teamMode_' + roomCode, 'INDIVIDUAL');
			localStorage.removeItem('teamAssignment_' + roomCode);
		}

		alert('팀 설정이 저장되었습니다.');
		$('#teamSelectModal').modal('hide');
	};
	
});