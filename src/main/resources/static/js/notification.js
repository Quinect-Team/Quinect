/**
 * notification.js
 * 웹소켓을 통해 실시간 알림을 수신하고 Topbar의 뱃지와 목록을 업데이트합니다.
 */

var stompClient = null;

$(document).ready(function() {

	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");

	// 모든 AJAX 요청 헤더에 CSRF 토큰을 포함시킵니다.
	if (token && header) {
		$(document).ajaxSend(function(e, xhr, options) {
			xhr.setRequestHeader(header, token);
		});
	}

	connectWebSocket();
});

function connectWebSocket() {
	// 1. 웹소켓 연결
	var socket = new SockJS('/ws');
	stompClient = Stomp.over(socket);
	stompClient.debug = null; // 콘솔 로그 끄기 (배포 시 유용)

	stompClient.connect({}, function(frame) {
		console.log('Notification WS Connected');

		// 2. 내 전용 알림 채널 구독 (/user/queue/notifications)
		stompClient.subscribe('/user/queue/notifications', function(message) {
			var notification = JSON.parse(message.body);
			updateTopbarAlert(notification);
		});

	}, function(error) {
		console.log('WS Error, Reconnecting in 5s...');
		setTimeout(connectWebSocket, 5000);
	});
}

/**
 * 알림 수신 시 UI 업데이트 함수
 */
function updateTopbarAlert(notification) {
	// ----------------------------------------
	// 1. 뱃지 숫자 업데이트 (빨간색 숫자)
	// ----------------------------------------
	var $badge = $('#alertBadge');

	// 현재 숫자 가져오기 ("3+" -> "3", 없으면 0)
	var currentText = $badge.text().replace('+', '');
	var currentCount = parseInt(currentText) || 0;

	// 숫자가 0(숨김 상태)이었다면 보이게 전환
	if (currentCount === 0) {
		$badge.show();
	}

	// 숫자 증가 후 적용
	$badge.text((currentCount + 1) + "+");

	// ----------------------------------------
	// 2. 드롭다운 목록에 새 알림 끼워넣기
	// ----------------------------------------

	// "알림이 없습니다" 메시지가 떠 있다면 숨김
	$('#noAlertsMessage').hide();

	// 현재 시간 포맷팅 (YYYY-MM-DD HH:mm) - 간단하게 구현
	var now = new Date();
	var timeString = now.getFullYear() + "-" +
		String(now.getMonth() + 1).padStart(2, '0') + "-" +
		String(now.getDate()).padStart(2, '0') + " " +
		String(now.getHours()).padStart(2, '0') + ":" +
		String(now.getMinutes()).padStart(2, '0');

	// 새 알림 아이템 HTML 생성 (Thymeleaf 구조와 동일하게 맞춤)
	var linkUrl = notification.url ? notification.url : "#"; // URL 없으면 #
	var notiId = notification.id;

	var newItemHtml = `
	        <a class="dropdown-item d-flex align-items-center" href="#" 
	           onclick="readNotification(${notiId}, '${linkUrl}', event)">
	            <div class="mr-3">
	                <div class="icon-circle bg-primary">
	                    <i class="fas fa-trophy text-white"></i>
	                </div>
	            </div>
	            <div>
	                <div class="small text-gray-500">${timeString}</div>
	                <span class="font-weight-bold">${notification.content}</span>
	            </div>
	        </a>
	    `;

	$('#notificationItems').prepend(newItemHtml);
}

function readNotification(id, url, event) {
	event.preventDefault(); // 즉시 이동 방지

	// AJAX 요청: 읽음 처리
	$.post("/api/notification/" + id + "/read")
		.done(function() {
			// 성공하면 페이지 이동 (이동 후 새로고침되면 뱃지는 자동으로 사라짐)
			if (url && url !== '#' && url !== 'null') {
				window.location.href = url;
			} else {
				// 이동할 곳이 없으면(단순 알림) 현재 페이지 새로고침 or 뱃지만 제거
				location.reload();
			}
		})
		.fail(function() {
			console.log("알림 읽음 처리 실패");
			// 실패해도 이동은 시켜줌
			if (url) window.location.href = url;
		});
}



/**
 * 안 읽은 친구 메시지 5개 조회 (드롭다운용)
 */
function loadUnreadMessages() {
	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	fetch('/api/friend-messages/unread/list', {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	})
		.then(response => response.json())
		.then(data => {
			updateMessageDropdown(data);
		})
		.catch(error => console.error('❌ 메시지 로드 실패:', error));
}

/**
 * 메시지 드롭다운 업데이트 (프로필 이미지 포함, 중복 제거, Show All 버튼)
 */
function updateMessageDropdown(messages) {
	const messageItems = document.getElementById('messageItems');
	const messageBadge = document.getElementById('messageBadge');
	const noMessagesMessage = document.getElementById('noMessagesMessage');

	// 기존 아이템 모두 제거
	messageItems.innerHTML = '';

	if (!messages || messages.length === 0) {
		messageBadge.style.display = 'none';
		if (noMessagesMessage) noMessagesMessage.style.display = 'block';
		return;
	}

	// ⭐ 발신자별 중복 제거 (JavaScript에서도 한 번 더)
	const uniqueMessages = [];
	const senderIds = new Set();

	messages.forEach(function(msg) {
		if (!senderIds.has(msg.senderId)) {
			senderIds.add(msg.senderId);
			uniqueMessages.push(msg);
		}
	});

	// 최대 5개까지만 표시
	const displayMessages = uniqueMessages.slice(0, 5);

	// 배지 업데이트 (실제 고유 발신자 수)
	if (uniqueMessages.length <= 5) {
		messageBadge.textContent = uniqueMessages.length.toString();
	} else {
		messageBadge.textContent = '5+';
	}
	messageBadge.style.display = 'block';
	if (noMessagesMessage) noMessagesMessage.style.display = 'none';

	// 메시지 아이템 생성
	displayMessages.forEach(function(msg) {
		const item = document.createElement('a');
		item.className = 'dropdown-item d-flex align-items-center';
		item.href = '#';
		item.style.cursor = 'pointer';

		item.onclick = function(e) {
			e.preventDefault();
			// 드롭다운 닫기
			document.getElementById('messagesDropdown').click();
			// 채팅 시작
			goToFriendChat(msg.senderId, msg.senderName);
		};

		// 메시지 미리보기 자르기
		let preview = msg.content;
		if (preview.length > 50) {
			preview = preview.substring(0, 50) + '...';
		}

		// ⭐ 프로필 이미지 또는 기본 아이콘
		let profileImageHtml;
		if (msg.profileImage) {
			profileImageHtml = `<img src="${escapeHtml(msg.profileImage)}" 
                                     style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover;">`;
		} else {
			profileImageHtml = `<div class="icon-circle bg-info">
                                    <i class="fas fa-envelope text-white"></i>
                                </div>`;
		}

		item.innerHTML = `
            <div class="mr-3">
                ${profileImageHtml}
            </div>
            
            <div style="flex-grow: 1;">
                <div class="small text-gray-500">
                    ${escapeHtml(msg.senderName)}
                </div>
                <span class="font-weight-bold" style="font-size: 13px;">
                    ${escapeHtml(preview)}
                </span>
            </div>
        `;

		messageItems.appendChild(item);
	});

	// ⭐ "Show All Messages" 버튼 추가 (고유 발신자가 5명 초과일 때)
	if (uniqueMessages.length > 5) {
		const showAllItem = document.createElement('a');
		showAllItem.className = 'dropdown-item text-center small text-primary';
		showAllItem.href = '#';
		showAllItem.style.cursor = 'pointer';
		showAllItem.textContent = 'Show all messages';

		showAllItem.onclick = function(e) {
			e.preventDefault();
			// 드롭다운 닫기
			document.getElementById('messagesDropdown').click();
			// 친구 창으로 이동
			goToFriendsModal();
		};

		messageItems.appendChild(showAllItem);
	}
}

/**
 * ⭐ 친구 창으로 이동하는 함수
 */
function goToFriendsModal() {
	console.log('✅ 친구 목록 창으로 이동');

	if (typeof openFriendModal === 'function') {
		openFriendModal();
	} else {
		console.error('❌ openFriendModal 함수를 찾을 수 없습니다');
	}
}

/**
 * 친구 채팅창으로 이동 (friendshipId를 서버에서 조회)
 */
function goToFriendChat(friendId, friendName) {
	console.log('✅ 친구 채팅창으로 이동:', friendName);

	const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
	const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

	// ⭐ 서버에서 friendshipId 조회
	fetch(`/api/friendships/find/${friendId}`, {
		method: 'GET',
		headers: {
			[csrfHeader]: csrfToken,
			'Content-Type': 'application/json'
		}
	})
		.then(response => response.json())
		.then(data => {
			if (data && data.id) {
				const friendshipId = data.id;

				window.currentChatUserId = friendId;
				window.currentChatUsername = friendName;

				if (typeof openFriendModal === 'function') {
					openFriendModal();

					setTimeout(() => {
						$('#friendsModal').hide();
						$('#chatModal').show();

						$('#chatFriendName').text(friendName || '알 수 없는 사용자');
						$('#chatFriendEmail').text('');
						$('#messageHistory').html(
							'<p class="text-center text-muted small">메시지가 없습니다.</p>'
						);

						setTimeout(function() {
							$('#messageInput').focus();
						}, 100);

						if (typeof loadMessageHistory === 'function') {
							loadMessageHistory(friendshipId);
						}

						if (typeof markChatRoomAsRead === 'function') {
							markChatRoomAsRead(friendshipId);
						}
					}, 300);
				} else {
					console.error('❌ openFriendModal 함수를 찾을 수 없습니다');
				}
			} else {
				console.error('❌ 서버에서 friendshipId를 찾을 수 없습니다');
			}
		})
		.catch(error => console.error('❌ 서버 요청 실패:', error));
}


/**
 * HTML 특수문자 이스케이프 (XSS 방지)
 */
function escapeHtml(text) {
	if (!text) return '';

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
window.loadUnreadMessages = loadUnreadMessages;
window.updateMessageDropdown = updateMessageDropdown;
window.goToFriendChat = goToFriendChat;
window.goToFriendsModal = goToFriendsModal;

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
	console.log('📍 friend-message-dropdown.js 로드됨');

	// 초기 로드
	loadUnreadMessages();

	// 5초마다 자동 갱신
	setInterval(loadUnreadMessages, 5000);
});

