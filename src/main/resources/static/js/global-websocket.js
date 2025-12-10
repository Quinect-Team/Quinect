/**
 * ✅ global-websocket.js (완벽한 버전)
 * WebSocket 연결 + 사용자 ID 직접 전달
 */

// ⭐ 전역 변수
var stompClient = null;
var connectionAttempts = 0;
var maxRetries = 3;

/**
 * 웹소켓 연결 (사용자 ID와 함께)
 */
function initGlobalWebSocket() {
	if (stompClient && stompClient.connected) {
		console.log('✅ 이미 웹소켓에 연결되어 있습니다.');
		if (typeof subscribeToPrivateMessages === 'function') {
			subscribeToPrivateMessages();
		}
		if (typeof subscribeToInvitations === 'function') {
			subscribeToInvitations();
		}
		return Promise.resolve(stompClient);
	}

	console.log('🔌 웹소켓 새로 연결 시작...');

	return new Promise((resolve, reject) => {
		// ⭐ 먼저 현재 사용자 ID를 가져오기
		$.ajax({
			url: '/api/user/current',
			type: 'GET',
			success: function(user) {
				console.log('✅ 현재 사용자 ID 획득:', user.id);

				var socket = new SockJS('/ws/friend-chat');
				stompClient = Stomp.over(socket);

				stompClient.debug = function(str) {
					console.log('📡 [STOMP DEBUG]:', str);
				};

				var connectTimeout = setTimeout(function() {
					console.error('❌ STOMP CONNECT 타임아웃 (10초)');
					stompClient = null;
					clearTimeout(connectTimeout);
					reject(new Error('WebSocket 연결 타임아웃'));
				}, 10000);

				// ⭐ CONNECT 헤더에 사용자 ID와 CSRF 토큰 포함
				var headers = {};
				var csrfToken = $('meta[name="_csrf"]').attr('content');
				var csrfHeader = $('meta[name="_csrf_header"]').attr('content');

				if (csrfToken && csrfHeader) {
					headers[csrfHeader] = csrfToken;
				}

				// ⭐ [중요] 커스텀 헤더에 사용자 ID 추가
				headers['X-User-ID'] = user.id;

				console.log('🔐 CONNECT 헤더:', headers);

				stompClient.connect(headers, function(frame) {
					clearTimeout(connectTimeout);

					console.log('✅ 웹소켓 STOMP 연결 성공!');
					console.log('✅ 서버 응답:', frame);

					// ⭐ Principal 직접 설정 (클라이언트 사이드)
					stompClient._userId = String(user.id);
					console.log('✅ stompClient._userId 설정:', stompClient._userId);

					if (typeof subscribeToPrivateMessages === 'function') {
						console.log('📢 subscribeToPrivateMessages 호출 중...');
						subscribeToPrivateMessages();
					}

					if (typeof subscribeToInvitations === 'function') {
						console.log('📢 subscribeToInvitations 호출 중...');
						subscribeToInvitations();
					}

					resolve(stompClient);
				}, function(error) {
					clearTimeout(connectTimeout);
					console.error('❌ 웹소켓 STOMP 연결 실패:', error);
					stompClient = null;
					reject(error);
				});
			},
			error: function(xhr) {
				console.error('❌ 사용자 ID 획득 실패:', xhr);
				reject(new Error('사용자 정보를 가져올 수 없습니다'));
			}
		});
	});
}

/**
 * 웹소켓 연결 해제
 */
function disconnectWebSocket() {
	if (stompClient && stompClient.connected) {
		stompClient.disconnect(function() {
			console.log('🔌 웹소켓 연결 해제');
			stompClient = null;
			connectionAttempts = 0;
		});
	}
}