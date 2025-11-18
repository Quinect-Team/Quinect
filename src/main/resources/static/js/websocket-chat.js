const socket = new SockJS('/ws-stomp');
const stompClient = Stomp.over(socket);

function connect() {
  if (stompClient && stompClient.connected) {
    stompClient.disconnect(() => {
      console.log('Disconnected before reconnecting');
      createAndConnectStompClient();
    });
  } else {
    createAndConnectStompClient();
  }
}

function createAndConnectStompClient() {
	stompClient.connect({}, function(frame) {
		console.log('Connected: ' + frame);

		// 1. 구독 - '/sub/quiz' 등으로
		stompClient.subscribe('/sub/message', function(message) {
			const data = JSON.parse(message.body);
			const chatList = document.getElementById('chatList');
			const item = document.createElement('div');
			item.textContent = data.sender + ': ' + data.content;
			chatList.appendChild(item);
			chatList.scrollTop = chatList.scrollHeight;
		});

		document.getElementById('sendBtn').onclick = function() {
			const msgInput = document.getElementById('msgInput');
			const nameInput = document.getElementById('nameInput');
			const sender = nameInput.value.trim() || '익명';
			const text = msgInput.value.trim();
			if (text) {
				// /pub/messages로 DTO 형태(JSON)로 발행
				stompClient.send('/pub/messages', {}, JSON.stringify({ sender: sender, content: text }));
				msgInput.value = '';
			}
		};
		document.getElementById('msgInput').addEventListener('keypress', function(e) {
			if (e.key === 'Enter') {
				document.getElementById('sendBtn').click();
			}
		});
	});
};
window.addEventListener('DOMContentLoaded', () => {
	connect();
});
