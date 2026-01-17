/**
 * ✅ global-websocket.js
 * WebSocket 연결 + 모든 구독 관리 (핵심)
 */

var stompClient = null;
var connectionAttempts = 0;
var maxRetries = 3;
var isInitializing = false;

// ⭐ 구독 상태 플래그
window.notificationsSubscribed = false;
window.privateMessagesSubscribed = false;
window.invitationsSubscribed = false;
window.messageReadSubscribed = false;  // ⭐ 새로 추가!


/**
 * 웹소켓 연결 (사용자 ID와 함께)
 */
function initGlobalWebSocket() {
	if (stompClient && stompClient.connected) {
		return Promise.resolve(stompClient);
	}

	if (isInitializing) {
		return new Promise((resolve) => {
			const checkInterval = setInterval(() => {
				if (stompClient && stompClient.connected) {
					clearInterval(checkInterval);
					resolve(stompClient);
				}
			}, 100);
		});
	}
	isInitializing = true;

	return new Promise((resolve, reject) => {
		$.ajax({
			url: '/api/user/current',
			type: 'GET',
			success: function(user) {

				var socket = new SockJS('/ws/friend-chat');
				stompClient = Stomp.over(socket);

				stompClient.debug = function(str) {
				};

				var connectTimeout = setTimeout(function() {
					console.error('❌ STOMP CONNECT 타임아웃 (10초)');
					stompClient = null;
					isInitializing = false;
					clearTimeout(connectTimeout);
					reject(new Error('WebSocket 연결 타임아웃'));
				}, 10000);

				var headers = {};
				var csrfToken = $('meta[name="_csrf"]').attr('content');
				var csrfHeader = $('meta[name="_csrf_header"]').attr('content');

				if (csrfToken && csrfHeader) {
					headers[csrfHeader] = csrfToken;
				}

				headers['X-User-ID'] = user.id;

				stompClient.connect(headers, function(frame) {
					clearTimeout(connectTimeout);

					stompClient._userId = String(user.id);

					$('body').data('user-id', user.id);
					$('body').data('user-email', user.email);

					console.log('=== WebSocket CONNECT 성공 ===');
					console.log('user.id:', user.id);
					console.log('user.id 타입:', typeof user.id);
					console.log('headers[X-User-ID]:', headers['X-User-ID']);
					console.log('===========================');

					subscribeToNotifications();

					isInitializing = false;
					resolve(stompClient);
				},
					function(error) {
						clearTimeout(connectTimeout);
						console.error('❌ 웹소켓 STOMP 연결 실패:', error);
						stompClient = null;
						isInitializing = false;
						reject(error);
					});
			},
			error: function(xhr) {
				console.error('❌ 사용자 ID 획득 실패:', xhr);
				isInitializing = false;
				reject(new Error('사용자 정보를 가져올 수 없습니다'));
			}
		});
	});
}

/**
 * ⭐ 알림 구독 (notification.js의 updateTopbarAlert 호출)
 */
function subscribeToNotifications() {
	if (!window.stompClient || !window.stompClient.connected) {
		console.warn('⚠️ WebSocket 연결 대기 중...');
		setTimeout(subscribeToNotifications, 3000);
		return;
	}

	if (window.notificationsSubscribed) {
		return;
	}

	window.notificationsSubscription =
		window.stompClient.subscribe('/user/queue/notifications', function(message) {
			console.log('📦 RAW:', message.body);

			var raw = JSON.parse(message.body);
			console.log('📦 PARSED:', raw);

			updateTopbarAlert(raw);
		});


	window.notificationsSubscribed = true;
}

/**
 * ⭐ 친구 메시지 구독 (friends-modal.js의 displayMessage 호출)
 */
function subscribeToPrivateMessages() {
	if (!window.stompClient || !window.stompClient.connected) {
		console.warn('⚠️ WebSocket 연결 대기 중...');
		setTimeout(subscribeToPrivateMessages, 3000);
		return;
	}

	if (window.privateMessagesSubscribed) {
		return;
	}

	window.privateMessageSubscription =
		window.stompClient.subscribe('/user/queue/friend-messages', function(message) {
			var msg = JSON.parse(message.body);

			// ⭐ friends-modal.js의 함수들 호출
			if (typeof displayMessage === 'function') {
				displayMessage(msg);
			}

			if (typeof updateFriendMessageDropdown === 'function') {
				updateFriendMessageDropdown(msg);
			}

			if (typeof incrementMessageBadge === 'function') {
				incrementMessageBadge();
			}
		});

	window.privateMessagesSubscribed = true;
}

/**
 * ⭐ 게임 초대 구독 (friends-modal.js의 showInvitationNotification 호출)
 */
function subscribeToInvitations() {
	if (!window.stompClient || !window.stompClient.connected) {
		console.warn('⚠️ WebSocket 연결 대기 중...');
		setTimeout(subscribeToInvitations, 5000);
		return;
	}

	if (window.invitationsSubscribed) {
		return;
	}


	try {
		window.invitationSubscription = window.stompClient.subscribe('/user/queue/room-invitations', function(message) {
			var invitation = JSON.parse(message.body);

			// ⭐ friends-modal.js의 함수 호출
			if (typeof showInvitationNotification === 'function') {
				showInvitationNotification(invitation);
			} else {
				console.warn('⚠️ showInvitationNotification 함수를 찾을 수 없습니다');
			}
		});

		window.invitationsSubscribed = true;
	} catch (error) {
		console.error('❌ 초대 구독 중 에러 발생:', error);
	}
}

/**
 * ⭐ 메시지 읽음 이벤트 구독 (notification.js의 removeFriendMessageFromDropdown 호출)
 */
function subscribeToMessageRead() {
	if (!window.stompClient || !window.stompClient.connected) {
		console.warn('⚠️ WebSocket 연결 대기 중...');
		setTimeout(subscribeToMessageRead, 3000);
		return;
	}

	if (window.messageReadSubscribed) {
		return;
	}

	try {
		window.messageReadSubscription = window.stompClient.subscribe('/user/queue/friend-messages-read', function(message) {
			var readEvent = JSON.parse(message.body);

			console.log('📖 읽음 이벤트 수신:', readEvent);

			if (readEvent.event === 'message-read') {
				console.log('📖 friendshipId ' + readEvent.friendshipId + '의 메시지를 읽음');

				// ⭐ notification.js의 함수 호출
				if (typeof removeFriendMessageFromDropdown === 'function') {
					removeFriendMessageFromDropdown(readEvent.friendshipId);
				} else {
					console.warn('⚠️ removeFriendMessageFromDropdown 함수를 찾을 수 없습니다');
				}
			}
		});

		window.messageReadSubscribed = true;
		console.log('✅ 메시지 읽음 이벤트 구독 성공');
	} catch (error) {
		console.error('❌ 읽음 이벤트 구독 중 에러 발생:', error);
	}
}

function subscribeToParticipantUpdates(roomCode) {
	if (!window.stompClient || !window.stompClient.connected) {
		console.warn('⚠️ WebSocket 연결 대기 중...');
		setTimeout(() => subscribeToParticipantUpdates(roomCode), 3000);
		return;
	}

	const subscribePath = '/topic/participants/' + roomCode;
	console.log('👥 참가자 업데이트 구독:', subscribePath);

	window.stompClient.subscribe(subscribePath, function(message) {
		var data = JSON.parse(message.body);
		console.log('👥 참가자 업데이트 수신:', data);

		if (data.type === 'PARTICIPANTUPDATE') {
			console.log('🔄 참가자 리스트 갱신:', data.participants);

			// ✅ 콜백 함수 호출 (각 페이지에서 처리)
			if (typeof updateParticipantsList === 'function') {
				updateParticipantsList(data.participants);
			}
		}
	});
}


/**
 * 웹소켓 연결 해제
 */
function disconnectWebSocket() {
	if (stompClient && stompClient.connected) {
		stompClient.disconnect(function() {
			stompClient = null;
			connectionAttempts = 0;
			isInitializing = false;

			window.notificationsSubscribed = false;
			window.privateMessagesSubscribed = false;
			window.invitationsSubscribed = false;
		});
	}
}

// ⭐ 글로벌 함수 노출
window.stompClient = stompClient;
window.initGlobalWebSocket = initGlobalWebSocket;
window.disconnectWebSocket = disconnectWebSocket;
window.subscribeToNotifications = subscribeToNotifications;
window.subscribeToPrivateMessages = subscribeToPrivateMessages;
window.subscribeToInvitations = subscribeToInvitations;
window.subscribeToMessageRead = subscribeToMessageRead;  // ⭐ 추가
window.subscribeToParticipantUpdates = subscribeToParticipantUpdates;
