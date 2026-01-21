// ⭐ 상단에 전역 변수 추가
window.currentFriendships = { received: [], sent: [], accepted: [] };
window.currentChatUserId = null;
window.currentChatUsername = null;
window.currentChatEmail = null;

// ⭐ 새로 추가
window.privateMessagesSubscribed = false;
window.invitationsSubscribed = false;

(function($) {
	'use strict';

	/**
	 * 친구 모달 열기
	 */
	function openFriendModal(e) {
		if (e) e.preventDefault();

		if ($('#sidebarModal').hasClass('show')) {
			$('#sidebarModal').removeClass('show');
			$('#sidebarModalBackdrop').css('display', 'none');
			setTimeout(function() {
				$('#sidebarModal').css('display', 'none');
			}, 300);
		}

		if (!$('.modalPOP').hasClass('show')) {
			$('.modalPOP').addClass('show');
		}

		$('#friendsModal').css('display', 'flex');
		$('#chatModal').css('display', 'none');

		if (!window.privateMessagesSubscribed) {
			subscribeToPrivateMessages();
		}

		loadAllFriendships();

	}

	function switchToChatView(userId, username, friendshipId) {
		currentChatUserId = userId;
		currentChatUsername = username;

		// ⭐ DOM에 저장 (전역 변수 대신)
		$('#chatModal').data({
			'friendshipId': friendshipId,
			'userId': userId,
			'username': username
		});

		$('#friendsModal').hide();
		$('#chatModal').show();
		$('#chatFriendName').text(username || '알 수 없는 사용자');
		$('#messageHistory').html('<p class="text-center text-muted small">메시지가 없습니다.</p>');

		console.log('채팅 전환:', $('#chatModal').data());

		setTimeout(function() {
			$('#messageInput').focus();
		}, 100);

		loadMessageHistory(friendshipId);
		markChatRoomAsRead(friendshipId);
	}

	function sendMessage() {
		const text = $('#messageInput').val().trim();

		if (!text) {
			alert('메시지를 입력해주세요');
			return;
		}

		// ⭐ DOM에서 읽어오기
		const chatData = $('#chatModal').data();
		const friendshipId = chatData.friendshipId;
		const userId = chatData.userId;

		if (!friendshipId || !userId) {
			alert('채팅 정보를 찾을 수 없습니다. 다시 친구를 선택해주세요.');
			return;
		}

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friend-messages/send',
			type: 'POST',
			data: {
				friendshipId: friendshipId,
				content: text
			},
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				console.log('메시지 전송 성공:', response);
				$('#messageInput').val('').focus();

				if (response && response.id) {
					displayMessage(response);
					setTimeout(function() {
						var historyDiv = $('#messageHistory');
						historyDiv.scrollTop(historyDiv[0].scrollHeight);
					}, 50);
				}
			},
			error: function(xhr) {
				console.error('메시지 전송 실패:', xhr);
				alert(xhr.responseText || '메시지 전송에 실패했습니다');
			}
		});
	}

	function backToFriendsList() {
		$('#chatModal').hide();
		$('#friendsModal').show();

		$('#messageHistory').empty();
		$('#messageInput').val('');

		// ⭐ DOM 데이터도 초기화
		$('#chatModal').removeData();
	}

	function closeFriendModal() {
		$('.modalPOP').removeClass('show');
		$('#sidebarModalBackdrop').css('display', 'none');

		$('#friendshipContainer').html('');
		$('#friendSearch').val('');
		currentFriendships = { received: [], sent: [], accepted: [] };

		$('#messageHistory').empty();
		$('#messageInput').val('');

		// ⭐ DOM 데이터만 초기화 (충분함)
		currentChatUserId = null;
		currentChatUsername = null;
		$('#chatModal').removeData();
	}

	/**
	 * 모든 친구 관계 로드 (항상 위에 고정)
	 */
	function loadAllFriendships() {

		$.ajax({
			type: 'GET',
			url: '/api/friends/all',
			dataType: 'json',
			success: function(data) {
				currentFriendships = data;

				const received = data.received || [];
				const sent = data.sent || [];
				const accepted = data.accepted || [];

				displayFriendshipsOnly(received, sent, accepted);
			},
			error: function(error) {
				displayEmptyState('친구 요청을 불러올 수 없습니다.');
			}
		});
	}

	/**
	 * 이메일로 사용자 검색
	 */
	function searchUsers(email) {

		$.ajax({
			url: '/api/friends/search',
			type: 'GET',
			data: { email: email },
			success: function(users) {
				displayFriendshipsWithSearch(users, email);
			},
			error: function(xhr) {
				console.error('검색 실패:', xhr);
				alert('사용자 검색에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 관계만 표시 (검색 없을 때) - 카테고리별로 분류
	 */
	function displayFriendshipsOnly(received, sent, accepted) {
		const $container = $('#friendshipContainer');
		$container.empty();

		const totalCount = received.length + sent.length + accepted.length;

		if (totalCount === 0) {
			displayEmptyState('아직 친구 요청이 없습니다.');
			return;
		}

		let html = '';

		// 1. 수락된 친구
		if (accepted.length > 0) {
			html += `
            <div class="mb-3">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-heart mr-2 text-success"></i>내 친구 (${accepted.length}명)
                </h6>
                <hr class="my-2">
            </div>
        `;
			accepted.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'ACCEPTED' });
			});
		}

		// 2. 받은 요청
		if (received.length > 0) {
			html += `
            <div class="mb-3 mt-4">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-inbox mr-2 text-info"></i>받은 친구 요청 (${received.length}개)
                </h6>
                <hr class="my-2">
            </div>
        `;
			received.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'PENDING_RECEIVED' });
			});
		}

		// 3. 보낸 요청
		if (sent.length > 0) {
			html += `
            <div class="mb-3 mt-4">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-paper-plane mr-2 text-warning"></i>보낸 친구 요청 (${sent.length}개)
                </h6>
                <hr class="my-2">
            </div>
        `;
			sent.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'PENDING_SENT' });
			});
		}

		$container.html(html);
	}

	/**
	 * 친구 관계 + 검색 결과 함께 표시 (카테고리별)
	 */
	function displayFriendshipsWithSearch(searchResults, email) {
		const $container = $('#friendshipContainer');
		$container.empty();

		const received = currentFriendships.received || [];
		const sent = currentFriendships.sent || [];
		const accepted = currentFriendships.accepted || [];

		let html = '';

		// 수락된 친구
		if (accepted.length > 0) {
			html += `
            <div class="mb-3">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-heart mr-2 text-success"></i>내 친구 (${accepted.length}명)
                </h6>
                <hr class="my-2">
            </div>
        `;
			accepted.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'ACCEPTED' });
			});
		}

		// 받은 요청
		if (received.length > 0) {
			html += `
            <div class="mb-3 mt-4">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-inbox mr-2 text-info"></i>받은 친구 요청 (${received.length}개)
                </h6>
                <hr class="my-2">
            </div>
        `;
			received.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'PENDING_RECEIVED' });
			});
		}

		// 보낸 요청
		if (sent.length > 0) {
			html += `
            <div class="mb-3 mt-4">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-paper-plane mr-2 text-warning"></i>보낸 친구 요청 (${sent.length}개)
                </h6>
                <hr class="my-2">
            </div>
        `;
			sent.forEach(friend => {
				html += createUserItem({ ...friend, friendshipStatus: 'PENDING_SENT' });
			});
		}

		// 검색 결과
		if (searchResults.length === 0) {
			html += `
            <div class="text-center py-3 text-muted mt-4">
                <p class="small mb-0">"${email}"에 대한 검색 결과가 없습니다.</p>
            </div>
        `;
		} else {
			html += `
            <div class="mb-4 mt-5">
                <h6 class="text-muted small font-weight-bold">
                    <i class="fas fa-search mr-2"></i>검색 결과 (${searchResults.length}명)
                </h6>
                <hr class="my-2">
            </div>
        `;

			searchResults.forEach(function(user) {
				html += createUserItem(user);
			});
		}

		$container.html(html);
	}

	/**
	 * 빈 상태 표시
	 */
	function displayEmptyState(headerText) {
		const $container = $('#friendshipContainer');
		$container.html(`
        <div class="text-center py-5 text-muted">
            <i class="fas fa-user-friends mb-3" style="font-size: 48px; opacity: 0.3;"></i>
            <p class="mb-1">${headerText}</p>
            <p class="small mb-0">검색창에 친구의 이메일을 입력해주세요.</p>
        </div>
    `);
	}

	/**
	 * 사용자 항목 HTML 생성
	 */
	function createUserItem(user) {

		let buttonHtml;
		let statusBadge = '';

		if (user.friendshipStatus === 'PENDING_SENT') {
			buttonHtml = `
            <button type="button" class="btn btn-warning btn-sm" disabled>
                <i class="fas fa-clock"></i> 대기중
            </button>
        `;
			statusBadge = '<span class="badge badge-light ml-2 text-dark small">내가 보냄</span>';

		} else if (user.friendshipStatus === 'PENDING_RECEIVED') {
			buttonHtml = `
            <div class="d-flex gap-2">
                <button type="button" 
                        class="btn btn-success btn-sm accept-friend-btn" 
                        data-friendship-id="${user.id}"
                        title="수락">
                    <i class="fas fa-check"></i> 수락
                </button>
                
                <div class="dropdown">
                    <button class="btn btn-outline-danger btn-sm dropdown-toggle" 
                            type="button" 
                            id="rejectDropdown_${user.id}"
                            data-toggle="dropdown" 
                            aria-haspopup="true" 
                            aria-expanded="false"
                            title="거절 옵션">
                        <i class="fas fa-times"></i> 거절
                    </button>
                    <div class="dropdown-menu" aria-labelledby="rejectDropdown_${user.id}">
                        <button class="dropdown-item reject-friend-btn" 
                                data-friendship-id="${user.id}"
                                data-action="reject">
                            <i class="fas fa-times mr-2"></i>거절 (다시 요청 가능)
                        </button>
                        <div class="dropdown-divider"></div>
                        <button class="dropdown-item ban-friend-btn text-danger" 
                                data-friendship-id="${user.id}"
                                data-action="ban">
                            <i class="fas fa-ban mr-2"></i>차단 (요청 불가)
                        </button>
                    </div>
                </div>
            </div>
        `;
			statusBadge = '<span class="badge badge-info ml-2 small">받은 요청</span>';

		} else if (user.friendshipStatus === 'ACCEPTED') {
			buttonHtml = `
            <div class="d-flex gap-2">
				<button type="button"
                        class="btn btn-warning btn-sm firend-profile-btn"
                        data-user-id="${user.id}"
						data-profile-id="${user.profileId}"
                        title="메시지 보내기">
                    <i class="fas fa-user-circle"></i> 프로필 보기
                </button>
                <button type="button"
                        class="btn btn-info btn-sm send-message-btn"
                        data-user-id="${user.id}"
                        data-username="${user.username}"
						data-friendship-id="${user.friendshipId}"
                        title="메시지 보내기">
                    <i class="fas fa-comments"></i> 메시지
                </button>
                <button type="button" 
                        class="btn btn-outline-secondary btn-sm remove-friend-btn" 
                        data-friendship-id="${user.id}"
                        title="친구 삭제">
                    <i class="fas fa-user-minus"></i> 친구 삭제
                </button>
            </div>
        `;
			statusBadge = '<span class="badge badge-success ml-2 small">친구</span>';

		} else if (user.friendshipStatus === 'REJECTED') {
			buttonHtml = `
            <button type="button" class="btn btn-secondary btn-sm" disabled>
                <i class="fas fa-ban"></i> 거절됨
            </button>
        `;
			statusBadge = '<span class="badge badge-secondary ml-2 small">거절됨</span>';

		} else if (user.friendshipStatus === 'REJECTED_BY_ME') {
			buttonHtml = `
            <button type="button" class="btn btn-secondary btn-sm" disabled>
                <i class="fas fa-times"></i> 내가 거절함
            </button>
        `;
			statusBadge = '<span class="badge badge-secondary ml-2 small">내가 거절함</span>';

		} else if (user.friendshipStatus === 'BANNED') {
			buttonHtml = `
            <button type="button" class="btn btn-danger btn-sm" disabled>
                <i class="fas fa-ban"></i> 차단됨
            </button>
        `;
			statusBadge = '<span class="badge badge-danger ml-2 small">차단됨</span>';

		} else if (user.friendshipStatus === 'BANNED_BY_ME') {
			buttonHtml = `
            <button type="button" class="btn btn-danger btn-sm" disabled>
                <i class="fas fa-ban"></i> 내가 차단함
            </button>
        `;
			statusBadge = '<span class="badge badge-danger ml-2 small">내가 차단함</span>';

		} else {
			buttonHtml = `
            <button type="button" 
                    class="btn btn-success btn-sm add-friend-btn" 
                    data-user-id="${user.id}">
                <i class="fas fa-user-plus"></i> 추가
            </button>
        `;
		}

		return `
        <div class="friend-item bg-light p-3 mb-2 rounded d-flex align-items-center">
            <div class="position-relative mr-3">
                <img src="${user.profileImage || '/img/default-avatar.png'}" 
                     alt="프로필"
                     class="rounded-circle border border-gray-300"
                     style="width: 48px; height: 48px; object-fit: cover;">
            </div>
            
            <div class="flex-grow-1">
                <div class="font-weight-bold text-gray-800 small mb-1">
                    ${user.username || '이름 없음'}
                    ${statusBadge}
                </div>
                <div class="text-muted small text-truncate">
                    ${user.email}
                </div>
            </div>
            
            <div style="margin-left: auto; flex-shrink: 0;">
                ${buttonHtml}
            </div>
        </div>
    `;
	}

	/**
	 * 친구 요청 보내기
	 */
	function sendFriendRequest(receiverId, $button) {

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friends/request',
			type: 'POST',
			data: { receiverId: receiverId },
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				alert(response);

				$button.removeClass('btn-success add-friend-btn')
					.addClass('btn-warning')
					.prop('disabled', true)
					.html('<i class="fas fa-clock"></i> 대기중');

				setTimeout(function() {
					loadAllFriendships();
				}, 500);
			},
			error: function(xhr) {
				console.error('친구 요청 실패:', xhr);
				alert(xhr.responseText || '친구 요청에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 요청 수락
	 */
	function acceptFriendRequest(friendshipId) {

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friends/' + friendshipId + '/accept',
			type: 'POST',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 1000);
			},
			error: function(xhr) {
				alert(xhr.responseText || '친구 요청 수락에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 요청 거절/차단
	 */
	function handleRejectOption(friendshipId, action) {

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		const url = action === 'reject'
			? '/api/friends/' + friendshipId + '/reject'
			: '/api/friends/' + friendshipId + '/ban';

		$.ajax({
			url: url,
			type: 'POST',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 1000);
			},
			error: function(xhr) {
				console.error((action === 'reject' ? '거절' : '차단') + ' 실패:', xhr);
				alert(xhr.responseText || (action === 'reject' ? '거절' : '차단') + '에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 삭제
	 */
	function removeFriend(friendshipId) {

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friends/' + friendshipId,
			type: 'DELETE',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 500);
			},
			error: function(xhr) {
				console.error('친구 삭제 실패:', xhr);
				alert(xhr.responseText || '친구 삭제에 실패했습니다.');
			}
		});
	}

	/**
	 * 메시지 전송
	 */
	function sendMessage() {
		const text = $('#messageInput').val().trim();

		if (!text) {
			alert('메시지를 입력해주세요');
			return;
		}

		// ⭐ DOM에서 읽어오기 (NEW!)
		const chatData = $('#chatModal').data();
		const friendshipId = chatData.friendshipId;
		const userId = chatData.userId;

		if (!friendshipId || !userId) {
			alert('채팅 정보를 찾을 수 없습니다. 다시 친구를 선택해주세요.');
			return;
		}

		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friend-messages/send',
			type: 'POST',
			data: {
				friendshipId: friendshipId,
				content: text
			},
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				console.log('메시지 전송 성공:', response);
				$('#messageInput').val('').focus();

				if (response && response.id) {
					displayMessage(response);
					setTimeout(function() {
						var historyDiv = $('#messageHistory');
						historyDiv.scrollTop(historyDiv[0].scrollHeight);
					}, 50);
				}
			},
			error: function(xhr) {
				console.error('메시지 전송 실패:', xhr);
				alert(xhr.responseText || '메시지 전송에 실패했습니다');
			}
		});
	}

	/**
	 * 메시지 기록 조회 및 표시
	 */
	function loadMessageHistory(friendshipId) {  // ← 파라미터명을 friendshipId로 변경
		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friend-messages/' + friendshipId,  // ← 명확함
			type: 'GET',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(messages) {
				if (!messages || messages.length === 0) {
					$('#messageHistory').html(
						'<p class="text-center text-muted small">메시지가 없습니다.</p>'
					);
					return;
				}

				$('#messageHistory').empty();
				messages.forEach(function(msg) {
					displayMessage(msg);
				});

				setTimeout(function() {
					$('#messageHistory').scrollTop($('#messageHistory')[0].scrollHeight);
				}, 100);
			},
			error: function(xhr) {
				console.error('메시지 기록 조회 실패:', xhr);
				$('#messageHistory').html(
					'<p class="text-center text-muted small">메시지를 불러올 수 없습니다.</p>'
				);
			}
		});
	}

	/**
	 * 메시지 하나 표시 (디버깅 강화)
	 */
	function displayMessage(msg) {
		console.log('📨 displayMessage() 호출:', msg);

		const currentUserId = $('body').data('user-id');

		// ⭐ sentAt을 Date 객체로 변환
		const messageTime = new Date(msg.sentAt).toLocaleTimeString('ko-KR', {
			hour: '2-digit',
			minute: '2-digit'
		});

		console.log('  messageTime:', messageTime);

		// ⭐ 게임 초대 메시지 판별
		const roomCodeMatch = msg.messageText.match(/방 코드:\s*(\w+)/);
		const isGameInvitation = roomCodeMatch !== null;
		const roomCode = isGameInvitation ? roomCodeMatch[1] : null;

		console.log('  isGameInvitation:', isGameInvitation);

		// 게임 초대 처리...
		if (isGameInvitation && roomCode) {
			console.log('🎮 게임 초대 감지:', roomCode);
			console.log('📖 isRead:', msg.isRead);

			// ⭐ isRead = false일 때만 모달 팝업 (첫 1회)
			if (!msg.isRead) {
				console.log('🎉 첫 초대, 모달 팝업 표시');

				const invitationData = {
					type: 'ROOM_INVITATION',
					roomCode: roomCode,
					inviterName: msg.senderName || '친구',
					messageText: msg.messageText,
					messageId: msg.id,  // ⭐ 메시지 ID (읽음 처리용)
					senderId: msg.senderId,
					friendshipId: msg.friendshipId
				};

				showInvitationNotification(invitationData);
			} else {
				console.log('📨 이미 읽은 초대, 채팅창 메시지로만 표시');
			}

			// ⭐ 모든 초대: 채팅창에도 메시지로 표시
			displayInvitationMessage(msg);
			return;
		}

		// ⭐ 일반 메시지 처리
		if (msg.senderId === currentUserId) {
			$('#messageHistory').append(`
		            <div class="mb-2 d-flex justify-content-end">
		                <div class="card bg-success text-white" style="max-width: 70%; word-break: break-word;">
		                    <div class="card-body p-2">
		                        <p class="mb-0">${escapeHtml(msg.messageText)}</p>
		                        <small class="text-bright-50" style="font-size: 0.75rem;">
		                            ${messageTime}
		                        </small>
		                    </div>
		                </div>
		            </div>
		        `);
		} else {
			$('#messageHistory').append(`
		            <div class="mb-2 d-flex justify-content-start">
		                <div class="card bg-light" style="max-width: 70%; word-break: break-word;">
		                    <div class="card-body p-2">
		                        <p class="mb-0 text-dark">${escapeHtml(msg.messageText)}</p>
		                        <small class="text-muted" style="font-size: 0.75rem;">
		                            ${messageTime}
		                        </small>
		                    </div>
		                </div>
		            </div>
		        `);
		}

		setTimeout(function() {
			var historyDiv = $('#messageHistory');
			if (historyDiv.length > 0) {
				historyDiv.scrollTop(historyDiv[0].scrollHeight);
			}
		}, 50);
	}

	function displayInvitationMessage(msg) {
		const roomCodeMatch = msg.messageText.match(/방 코드:\s*(\w+)/);
		const roomCode = roomCodeMatch ? roomCodeMatch[1] : 'N/A';

		$('#messageHistory').append(`
	        <div class="mb-2 d-flex justify-content-start">
	            <div class="card bg-warning text-dark" style="max-width: 70%; word-break: break-word;">
	                <div class="card-body p-2">
	                    <p class="mb-2"><strong>🎮 게임 초대</strong></p>
	                    <p class="mb-2 text-dark">방 코드: <code>${roomCode}</code></p>
	                    <button type="button" 
	                            class="btn btn-success btn-sm accept-invitation-btn"
	                            data-room-code="${roomCode}"
	                            data-message-id="${msg.id}">
	                        <i class="fas fa-check"></i> 참가하기
	                    </button>
	                </div>
	            </div>
	        </div>
	    `);

		setTimeout(function() {
			var historyDiv = $('#messageHistory');
			if (historyDiv.length > 0) {
				historyDiv.scrollTop(historyDiv[0].scrollHeight);
			}
		}, 50);
	}



	/**
	 * ⭐ 게임 초대 수락
	 */
	function acceptGameInvitation(roomCode) {
		window.location.href = '/waitroom/' + roomCode;
	}

	window.acceptGameInvitation = acceptGameInvitation;

	/**
	 * HTML 특수문자 이스케이프 (XSS 방지)
	 */
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

	/**
	 * 글로벌 함수 노출
	 */
	window.openFriendModal = openFriendModal;
	window.closeFriendModal = closeFriendModal;
	window.acceptFriendRequest = acceptFriendRequest;
	window.sendFriendRequest = sendFriendRequest;
	window.removeFriend = removeFriend;

	window.showInvitationNotification = showInvitationNotification;

	window.switchToChatView = switchToChatView;
	window.backToFriendsList = backToFriendsList;
	window.sendMessage = sendMessage;

	window.loadMessageHistory = loadMessageHistory;
	window.markChatRoomAsRead = markChatRoomAsRead;

	/**
	 * 1:1 채팅 메시지 실시간 수신 대기 (재구독 가능한 버전)
	 */
	function subscribeToPrivateMessages() {
		console.log('[PM] subscribeToPrivateMessages 호출');

		if (!window.stompClient || !window.stompClient.connected) {
			console.warn('⚠️ WebSocket 연결 대기 중...');
			setTimeout(subscribeToPrivateMessages, 3000);
			return;
		}

		if (window.privateMessagesSubscribed) {
			console.log('[PM] 이미 구독됨, 재사용');
			return;
		}

		const userEmail = $('body').data('user-email');
		if (!userEmail) {
			console.warn('⚠️ user-email이 없음, 재시도');
			setTimeout(subscribeToPrivateMessages, 2000);
			return;
		}

		const subscribePath = '/user/' + userEmail + '/queue/friend-messages';
		console.log('[PM] 구독 경로:', subscribePath);

		window.privateMessageSubscription =
			window.stompClient.subscribe(subscribePath, function(message) {
				console.log('[PM] 수신 raw:', message);
				const msg = JSON.parse(message.body);
				console.log('[PM] 파싱된 msg:', msg);

				// ⭐ 현재 열려 있는 채팅방의 friendshipId
				const chatData = $('#chatModal').data() || {};
				const currentFriendshipId = chatData.friendshipId;

				// msg.friendshipId 없다면 백엔드에서 DTO에 꼭 넣어줘야 함
				const msgFriendshipId = msg.friendshipId;

				// 1) 현재 열려 있는 방과 같은 friendshipId일 때만 채팅창에 표시
				if (currentFriendshipId && msgFriendshipId === currentFriendshipId) {
					console.log('[PM] 현재 열린 대화방 메시지, 화면에 표시');
					displayMessage(msg);
				} else {
					console.log('[PM] 다른 대화방 메시지, 채팅창에는 표시 안 함');
				}

				// 2) 채팅 모달이 닫혀 있거나, 다른 방 메시지인 경우에만 드롭다운/뱃지 갱신
				const isChatModalOpen = $('#chatModal').css('display') !== 'none';
				const isSameRoom = currentFriendshipId && msgFriendshipId === currentFriendshipId;

				if (!isChatModalOpen || !isSameRoom) {
					if (typeof updateFriendMessageDropdown === 'function') {
						updateFriendMessageDropdown(msg);
					}
					if (typeof incrementMessageBadge === 'function') {
						incrementMessageBadge();
					}
				} else {
					console.log('[PM] 현재 방 메시지 + 채팅창 열려 있음 → 드롭다운/배지 갱신 스킵');
				}
			});

		window.privateMessagesSubscribed = true;
		console.log('[PM] 구독 완료: ' + subscribePath);
	}

	/**
	 * 초대 메시지 수신 (최종 버전, 이거 하나만 둔다)
	 */
	function subscribeToInvitations() {
		console.log('[INV] subscribeToInvitations 호출');

		if (!window.stompClient || !window.stompClient.connected) {
			console.warn('⚠️ WebSocket 연결 대기 중...');
			setTimeout(subscribeToInvitations, 5000);
			return;
		}

		if (window.invitationsSubscribed) {
			console.log('[INV] 이미 구독됨, 재사용');
			return;
		}

		try {
			console.log('[INV] /user/queue/room-invitations 구독 시작');

			window.invitationSubscription =
				window.stompClient.subscribe('/user/queue/room-invitations', function(message) {
					console.log('[INV] 수신 raw:', message);
					var invitation = JSON.parse(message.body);
					if (typeof showInvitationNotification === 'function') {
						showInvitationNotification(invitation);
					} else {
						console.warn('[INV] showInvitationNotification 없음');
					}
				});

			window.invitationsSubscribed = true;
			console.log('[INV] 구독 완료');
		} catch (error) {
			console.error('초대 구독 중 에러:', error);
		}
	}

	/**
	 * 친구 채팅창으로 이동
	 */
	function goToFriendChat(friendId, friendName) {

		// TODO: 친구 채팅 페이지 경로 설정
		window.location.href = '/chat/friend/' + friendId;

		// 또는 모달로 띄우기 (채팅 페이지가 없으면)
		// openFriendChatModal(friendId, friendName);
	}

	/**
	 * ⭐ 채팅방의 모든 메시지를 읽음 처리
	 */
	function markChatRoomAsRead(friendshipId) {
		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		if (!friendshipId) {
			console.warn('⚠️ friendshipId가 없습니다');
			return;
		}

		$.ajax({
			url: '/api/friend-messages/friendship/' + friendshipId + '/mark-as-read',
			type: 'PUT',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {

				// ⭐ 백엔드에서 받은 messageCount로 배지 감소
				if (response.messageCount && typeof decrementMessageBadgeByCount === 'function') {
					decrementMessageBadgeByCount(response.messageCount);
				}
			},
			error: function(xhr) {
				console.warn('⚠️ 읽음 처리 실패 (무시):', xhr);
			}
		});
	}

	function showInvitationNotification(invitation) {
		console.log('🎉 showInvitationNotification 호출');

		if (!invitation || !invitation.roomCode) {
			console.error('invitation 데이터 없음');
			return;
		}

		const roomCode = invitation.roomCode;
		const messageId = invitation.messageId;
		const inviterName = invitation.inviterName || '친구';

		console.log('🎮 방 초대 팝업 표시:', roomCode, 'by', inviterName);

		Swal.fire({
			title: '🎮 방 초대!',
			html: `
	            <div style="text-align: center; padding: 20px;">
	                <p style="font-size: 18px; margin-bottom: 20px; font-weight: bold;">
	                    <span style="color: #4e73df;">${escapeHtml(inviterName)}</span>님이 방으로 초대했습니다!
	                </p>
	                <div style="background: #f0f3ff; padding: 20px; border-radius: 12px; margin: 20px 0;">
	                    <p style="margin: 0 0 10px 0; color: #888; font-size: 12px; text-transform: uppercase;">방 코드</p>
	                    <p style="margin: 0; font-size: 32px; font-weight: bold; color: #4e73df; letter-spacing: 5px;">
	                        ${roomCode}
	                    </p>
	                </div>
	                <p style="color: #666; font-size: 13px; margin-top: 15px;">
	                    <i class="fas fa-clock mr-2"></i>지금 참가하세요!
	                </p>
	            </div>
	        `,
			icon: 'success',
			confirmButtonText: '✅ 참가하기',
			cancelButtonText: '❌ 나중에',
			showCancelButton: true,
			confirmButtonColor: '#4e73df',
			cancelButtonColor: '#858796',
			allowOutsideClick: false,
			allowEscapeKey: false,
			didOpen: function() {
				// ⭐ 1분(60초) 후 자동으로 팝업 닫기
				setTimeout(() => {
					Swal.close();
				}, 60000);  // 60000ms = 1분
			}

		}).then((result) => {
			if (result.isConfirmed) {
				markInvitationAsRead(messageId);

				// 참가자 업데이트 구독 시작 (새 탭에서도 실시간 업데이트)
				if (typeof subscribeToParticipantUpdates === 'function') {
					subscribeToParticipantUpdates(roomCode);
				}

				// 1초 후 이동 (구독 시간 확보)
				setTimeout(() => {
					window.location.href = '/waitroom/' + roomCode;
				}, 500);
			} else {
				markInvitationAsRead(messageId);
			}
		});
	}

	function markInvitationAsRead(messageId) {
		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		if (!messageId) return;

		$.ajax({
			url: '/api/friend-messages/' + messageId + '/mark-as-read',
			type: 'PUT',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(response) {
				console.log('초대 메시지 읽음 처리:', messageId);
			},
			error: function(xhr) {
				console.warn('읽음 처리 실패 (무시):', xhr);
			}
		});
	}

	/**
	 * DOM 로드 후 초기화
	 */
	$(document).ready(function() {

		// 1단계: WebSocket 연결
		initGlobalWebSocket()
			.then(function() {
				// 2단계: 현재 사용자 정보 셋업
				$.ajax({
					url: '/api/user/current',
					type: 'GET',
					success: function(user) {
						$('body').data('user-id', user.id);
						$('body').data('user-email', user.email);
					}
				});

				// 3단계: 메시지/초대 구독 시작
				subscribeToPrivateMessages();
				subscribeToInvitations();
			})
			.catch(function(error) {
				console.error('[1단계 실패] WebSocket 연결 실패:', error);
			});

		// 닫기 버튼 클릭
		$('.closebtn').on('click', closeFriendModal);

		// 배경 클릭 시 모달 닫기
		$('.modalPOP').on('click', function(e) {
			if ($(e.target).hasClass('modalPOP')) {
				closeFriendModal();
			}
		});

		// 검색 버튼 클릭
		$('#searchBtn').on('click', function() {
			const email = $('#friendSearch').val().trim();

			if (email.length === 0) {
				loadAllFriendships();
				return;
			}

			if (email.length < 2) {
				alert('최소 2글자 이상 입력하세요.');
				return;
			}

			searchUsers(email);
		});

		// 엔터키로 검색
		$('#friendSearch').on('keypress', function(e) {
			if (e.which === 13) {
				e.preventDefault();
				$('#searchBtn').click();
			}
		});

		// 친구 추가 버튼
		$(document).on('click', '.add-friend-btn', function() {
			const receiverId = $(this).data('user-id');

			if (confirm('친구 요청을 보내시겠습니까?')) {
				sendFriendRequest(receiverId, $(this));
			}
		});

		// 친구 수락 버튼
		$(document).on('click', '.accept-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');

			if (confirm('친구 요청을 수락하시겠습니까?')) {
				acceptFriendRequest(friendshipId);
			}
		});

		// 친구 삭제 버튼
		$(document).on('click', '.remove-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');

			if (confirm('이 친구를 삭제하시겠습니까?')) {
				removeFriend(friendshipId);
			}
		});

		$(document).on('click', '.firend-profile-btn', function() {
			const userId = $(this).data('user-id');
			const profileId = $(this).data('profile-id');

			window.location.href = '/profile/' + profileId;
		});

		// 메시지 버튼 → 채팅 모달로 전환
		$(document).on('click', '.send-message-btn', function() {
			const userId = $(this).data('user-id');
			const username = $(this).data('username');
			const friendshipId = $(this).data('friendship-id');

			console.log('클릭 시 값들');
			console.log('userId:', userId);
			console.log('username:', username);
			console.log('friendshipId:', friendshipId);

			switchToChatView(userId, username, friendshipId);
		});

		// 채팅 입력창 엔터로 전송
		$(document).on('keypress', '#messageInput', function(e) {
			if (e.which === 13) {
				e.preventDefault();
				sendMessage();
			}
		});

		// 거절/차단 드롭다운 이벤트
		$(document).on('click', '.reject-friend-btn, .ban-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');
			const action = $(this).data('action');

			const message = action === 'reject'
				? '이 친구 요청을 거절하시겠습니까?\n(상대방이 다시 요청할 수 있습니다)'
				: '이 사용자를 차단하시겠습니까?\n(상대방이 친구 요청을 할 수 없습니다)';

			if (confirm(message)) {
				handleRejectOption(friendshipId, action);
			}
		});

		$(document).on('click', '.accept-invitation-btn', function() {
			const roomCode = $(this).data('room-code');
			const messageId = $(this).data('message-id');

			markInvitationAsRead(messageId);

			if (typeof subscribeToParticipantUpdates === 'function') {
				subscribeToParticipantUpdates(roomCode);
			}

			setTimeout(() => {
				window.location.href = '/waitroom/' + roomCode;
			}, 500);

		});

	});

})(jQuery);