/**
 * ✅ notification.js
 * 알림 + 메시지 드롭다운 UI (순수 UI만)
 */

$(document).ready(function() {
	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");

	if (token && header) {
		$(document).ajaxSend(function(e, xhr, options) {
			xhr.setRequestHeader(header, token);
		});
	}

});

// ====================================
// ⭐ 알림 관련 함수들
// ====================================

/**
 * 알림 수신 시 UI 업데이트 함수
 */
function updateTopbarAlert(notification) {

	var $badge = $('#alertBadge');
	var currentText = $badge.text().replace('+', '');
	var currentCount = parseInt(currentText) || 0;

	if (currentCount === 0) {
		$badge.show();
	}

	$badge.text((currentCount + 1) + "+");
	$('#noAlertsMessage').hide();

	var now = new Date();
	var timeString = now.getFullYear() + "-" +
		String(now.getMonth() + 1).padStart(2, '0') + "-" +
		String(now.getDate()).padStart(2, '0') + " " +
		String(now.getHours()).padStart(2, '0') + ":" +
		String(now.getMinutes()).padStart(2, '0');

	var linkUrl = notification.url ? notification.url : "#";
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
	event.preventDefault();

	$.post("/api/notification/" + id + "/read")
		.done(function() {
			if (url && url !== '#' && url !== 'null') {
				window.location.href = url;
			} else {
				location.reload();
			}
		})
		.fail(function() {
			if (url) window.location.href = url;
		});
}

// ====================================
// ⭐ 메시지 드롭다운 관련 함수들
// ====================================

/**
 * 안 읽은 친구 메시지 조회 (페이지 로드 시)
 */
function loadUnreadMessages() {

	fetch('/api/friend-messages/unread/list', {
		method: 'GET'
	})
		.then(response => response.json())
		.then(data => {
			updateMessageDropdown(data);
		})
		.catch(error => console.error('❌ 메시지 로드 실패:', error));
}

/**
 * 메시지 드롭다운 업데이트 (초기 로드 시)
 */
function updateMessageDropdown(messages) {
	const messageItems = document.getElementById('messageItems');
	const messageBadge = document.getElementById('messageBadge');
	const noMessagesMessage = document.getElementById('noMessagesMessage');

	messageItems.innerHTML = '';

	if (!messages || messages.length === 0) {
		messageBadge.style.display = 'none';
		messageBadge.textContent = '';
		if (noMessagesMessage) noMessagesMessage.style.display = 'block';
		return;
	}

	let totalMessageCount = 0;
	messages.forEach(function(msg) {
		totalMessageCount += (msg.messageCount || 1);
	});

	const displayMessages = messages.slice(0, 5);

	messageBadge.textContent = totalMessageCount > 5 ? '5+' : totalMessageCount.toString();
	messageBadge.style.display = 'block';

	if (noMessagesMessage) noMessagesMessage.style.display = 'none';

	displayMessages.forEach(function(msg) {
		const item = document.createElement('a');
		item.className = 'dropdown-item d-flex align-items-center';
		item.href = '#';
		item.style.cursor = 'pointer';

		item.onclick = function(e) {
			e.preventDefault();
			document.getElementById('messagesDropdown').click();

			// ⭐ 배지 감소
			if (typeof decrementMessageBadgeByCount === 'function') {
				decrementMessageBadgeByCount(msg.messageCount || 1);
			}

			// ⭐ switchToChatView() 직접 호출
			if (typeof openFriendModal === 'function') {
				openFriendModal();
			}

			setTimeout(() => {
				if (typeof switchToChatView === 'function') {
					switchToChatView(
						msg.senderId,
						msg.senderName,
						msg.friendshipId  // ⭐ 이미 있는 데이터
					);
				}
			}, 300);
		};

		let preview = msg.lastMessage || msg.content || '';
		if (preview.length > 50) {
			preview = preview.substring(0, 50) + '...';
		}

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
                <small class="text-muted d-block" style="font-size: 11px;">
                    ${msg.messageCount || 1}개의 메시지
                </small>
            </div>
        `;

		messageItems.appendChild(item);
	});

	// Show All 버튼
	if (messages.length > 5) {
		const showAllItem = document.createElement('a');
		showAllItem.className = 'dropdown-item text-center small text-primary';
		showAllItem.href = '#';
		showAllItem.style.cursor = 'pointer';
		showAllItem.textContent = 'Show all messages';

		showAllItem.onclick = function(e) {
			e.preventDefault();
			document.getElementById('messagesDropdown').click();
			goToFriendsModal();
		};

		messageItems.appendChild(showAllItem);
	}
}


/**
 * WebSocket으로 수신한 친구 메시지를 드롭다운에 실시간 추가
 */
function updateFriendMessageDropdown(message) {
	const messageItems = document.getElementById('messageItems');
	const messageBadge = document.getElementById('messageBadge');
	const noMessagesMessage = document.getElementById('noMessagesMessage');

	if (!messageItems) {
		console.warn('⚠️ messageItems 엘리먼트 없음');
		return;
	}

	if (noMessagesMessage) {
		noMessagesMessage.style.display = 'none';
	}

	messageBadge.style.display = 'block';

	const existingItems = messageItems.querySelectorAll('a.dropdown-item:not(.text-center)');
	existingItems.forEach(function(item) {
		const senderName = message.senderName || '';
		if (item.textContent.includes(senderName)) {
			item.remove();
		}
	});

	const item = document.createElement('a');
	item.className = 'dropdown-item d-flex align-items-center';
	item.href = '#';
	item.style.cursor = 'pointer';

	item.onclick = function(e) {
		e.preventDefault();
		document.getElementById('messagesDropdown').click();

		if (typeof decrementMessageBadgeByCount === 'function') {
			decrementMessageBadgeByCount(1);
		}

		// ⭐ switchToChatView() 직접 호출
		if (typeof openFriendModal === 'function') {
			openFriendModal();
		}

		setTimeout(() => {
			if (typeof switchToChatView === 'function') {
				switchToChatView(
					message.senderId,
					message.senderName,
					message.friendshipId  // ⭐ WebSocket 메시지에 friendshipId가 있는지 확인!
				);
			}
		}, 300);
	};

	let preview = message.messageText || message.content || '';
	if (preview.length > 50) {
		preview = preview.substring(0, 50) + '...';
	}

	let profileImageHtml;
	if (message.profileImage) {
		profileImageHtml = `<img src="${escapeHtml(message.profileImage)}" 
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
        ${escapeHtml(message.senderName || '알 수 없는 사용자')}
        </div>
        <span class="font-weight-bold" style="font-size: 13px;">
        ${escapeHtml(preview)}
        </span>
        <small class="text-muted d-block" style="font-size: 11px;">
            1개의 메시지
        </small>
        </div>
    `;

	if (messageItems.firstChild) {
		messageItems.insertBefore(item, messageItems.firstChild);
	} else {
		messageItems.appendChild(item);
	}

	const displayItems = messageItems.querySelectorAll('a.dropdown-item:not(.text-center)');
	while (displayItems.length > 5) {
		displayItems[displayItems.length - 1].remove();
	}
}


/**
 * ⭐ 배지 숫자 증가 (WebSocket 메시지 1개)
 */
function incrementMessageBadge() {

	const messageBadge = document.getElementById('messageBadge');

	if (!messageBadge) {
		console.error('❌ messageBadge 엘리먼트를 찾을 수 없습니다!');
		return;
	}


	// ⭐ 배지가 숨겨져 있으면 1부터 시작
	if (messageBadge.style.display === 'none' || messageBadge.textContent === '') {
		messageBadge.textContent = '1';
	} else {
		// ⭐ 배지가 이미 표시되면 숫자 증가
		let currentBadgeText = messageBadge.textContent.replace('+', '');
		let currentCount = parseInt(currentBadgeText) || 0;

		const newCount = currentCount + 1;

		// ⭐ 5 초과일 때만 5+ 표시
		messageBadge.textContent = newCount > 5 ? '5+' : newCount.toString();
	}

	messageBadge.style.display = 'block';
}

/**
 * ⭐ 배지를 특정 개수만큼 감소 (친구 클릭할 때)
 */
function decrementMessageBadgeByCount(count) {

	const messageBadge = document.getElementById('messageBadge');
	const noMessagesMessage = document.getElementById('noMessagesMessage');

	if (!messageBadge) {
		console.error('❌ messageBadge 엘리먼트를 찾을 수 없습니다!');
		return;
	}


	let currentBadgeText = messageBadge.textContent.replace('+', '');
	let currentCount = parseInt(currentBadgeText) || 0;


	// ⭐ 해당 개수만큼 감소
	const newCount = currentCount - count;

	if (newCount <= 0) {
		// ⭐ 0이면 배지 숨기기
		messageBadge.style.display = 'none';
		messageBadge.textContent = '';

		if (noMessagesMessage) {
			noMessagesMessage.style.display = 'block';
		}

	} else if (newCount > 5) {
		// ⭐ 5 초과면 5+
		messageBadge.textContent = '5+';

	} else {
		// ⭐ 1-5 사이
		messageBadge.textContent = newCount.toString();
	}
}

/**
 * 친구 채팅창으로 이동
 */
/**
 * 친구 채팅창으로 이동 (수정된 버전)
 */
function goToFriendChat(friendId, friendName) {

	fetch(`/api/friend-messages/friendships/find/${friendId}`, {
		method: 'GET'
	})
		.then(response => response.json())
		.then(data => {
			if (!data || !data.id) {
				console.error('❌ 서버에서 friendshipId를 찾을 수 없습니다');
				return;
			}

			const friendshipId = data.id;

			// ⭐ 친구 모달이 이미 열려있지 않으면 열기
			if (typeof openFriendModal === 'function') {
				openFriendModal();
			}

			// ⭐ switchToChatView() 호출 (DOM data 설정 + loadMessageHistory 등)
			setTimeout(() => {
				if (typeof switchToChatView === 'function') {
					switchToChatView(friendId, friendName, friendshipId);
				} else {
					console.error('❌ switchToChatView 함수를 찾을 수 없습니다');
				}
			}, 300);
		})
		.catch(error => console.error('❌ friendshipId 조회 실패:', error));
}


/**
 * 친구 목록 모달로 이동
 */
function goToFriendsModal() {

	if (typeof openFriendModal === 'function') {
		openFriendModal();
	} else {
		console.error('❌ openFriendModal 함수를 찾을 수 없습니다');
	}
}

// ====================================
// ⭐ 유틸리티 함수
// ====================================

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

// ====================================
// ⭐ 글로벌 함수 노출
// ====================================

window.updateTopbarAlert = updateTopbarAlert;
window.readNotification = readNotification;
window.loadUnreadMessages = loadUnreadMessages;
window.updateMessageDropdown = updateMessageDropdown;
window.updateFriendMessageDropdown = updateFriendMessageDropdown;
window.incrementMessageBadge = incrementMessageBadge;
window.decrementMessageBadgeByCount = decrementMessageBadgeByCount;
window.goToFriendChat = goToFriendChat;
window.goToFriendsModal = goToFriendsModal;
window.escapeHtml = escapeHtml;

/**
 * 페이지 로드 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
});
