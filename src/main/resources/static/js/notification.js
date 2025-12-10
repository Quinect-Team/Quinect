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