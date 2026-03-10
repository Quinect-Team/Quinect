var roomCode = document.body.getAttribute('data-room-code');
var totalQuestions = parseInt(document.body.getAttribute('data-total-questions'));
var currentQuestionData = null;
var userId = parseInt(document.body.getAttribute('data-user-id'));
var stompClient = null;
let autosaveTimer = null;

let submittedUsers = new Set();
let latestRanking = [];

function getAnswerValue() {
	if (!currentQuestionData) return null;
	if (currentQuestionData.quizTypeCode === 2) {
		const selected = document.querySelector('input[name="answer"]:checked');
		return selected ? selected.value : null;
	} else {
		return document.getElementById('answer-textarea').value;
	}
}

function debounceAutoSave() {
	if (autosaveTimer) clearTimeout(autosaveTimer);
	const indicator = document.getElementById('autosave-status');
	if (indicator) indicator.textContent = '임시 저장 중...';

	autosaveTimer = setTimeout(() => {
		const answer = getAnswerValue();
		if (answer) {
			const key = `quiz_temp_${roomCode}_q${currentQuestionData.questionId}`;
			const data = { roomCode, questionId: currentQuestionData.questionId, answer: answer, timestamp: new Date().toISOString() };
			localStorage.setItem(key, JSON.stringify(data));
			if (indicator) {
				indicator.textContent = '✓ 자동 저장됨';
				indicator.classList.add('saved');
				setTimeout(() => indicator.classList.remove('saved'), 2000);
			}
		}
	}, 500);
}

function attachAutoSaveEvents() {
	document.querySelectorAll('textarea').forEach(el => {
		el.removeEventListener('input', debounceAutoSave);
		el.removeEventListener('change', debounceAutoSave);
		el.addEventListener('input', debounceAutoSave);
		el.addEventListener('change', debounceAutoSave);
	});

	document.querySelectorAll('input[type="radio"][name="answer"]').forEach(el => {
		el.removeEventListener('change', debounceAutoSave);
		el.addEventListener('change', debounceAutoSave);
	});
}

function autoRestoreIfExists() {
	if (!currentQuestionData) return;
	const key = `quiz_temp_${roomCode}_q${currentQuestionData.questionId}`;
	const saved = localStorage.getItem(key);
	if (!saved) return;

	try {
		const data = JSON.parse(saved);
		if (currentQuestionData.quizTypeCode === 2) {
			const radio = document.querySelector(`input[name="answer"][value="${data.answer}"]`);
			if (radio) radio.checked = true;
		} else {
			document.getElementById('answer-textarea').value = data.answer || '';
		}
	} catch (e) {
		console.warn('[autosave] 임시본 파싱 실패', e);
	}
}

function connectWebSocket() {
	var socket = new SockJS('/ws');
	stompClient = Stomp.over(socket);
	stompClient.connect({}, function(frame) {
		stompClient.subscribe('/topic/quiz/' + roomCode, function(message) {
			var data = JSON.parse(message.body);

			if (data.type === 'QUESTION') {
				submittedUsers.clear();
				displayQuestion(data);
				updateSubmitProgress(0, data.totalPlayers);
			}
			else if (data.type === 'RANKING') {
				latestRanking = data.ranking || [];
				displayRanking(latestRanking);
			}
			else if (data.type === 'SUBMIT_STATUS') {
				markSubmitted(data.userId);
				updateSubmitProgress(data.submittedCount, data.totalPlayers);
			}
			else if (data.type === 'FINISH') {
				finishQuiz();
			}
		});

		var userStorageKey = 'quiz_started_' + roomCode + '_' + userId;
		if (!sessionStorage.getItem(userStorageKey)) {
			sessionStorage.setItem(userStorageKey, 'true');
			console.log('📤 nextQuestion 호출 (userId=' + userId + ')');
			stompClient.send('/app/quiz/next-question/' + roomCode, {}, {});
		} else {
			console.log('⏭️ 이미 호출함 (userId=' + userId + ')');
		}
	});
}

function displayQuestion(question) {
	currentQuestionData = question;
	document.getElementById('loading-state').style.display = 'none';
	document.getElementById('question-area').style.display = 'block';
	document.getElementById('ranking-container').style.display = 'block';

	document.getElementById('current-question').textContent = question.questionNumber;
	document.getElementById('progress-bar').style.width = (question.questionNumber / totalQuestions * 100) + '%';
	document.getElementById('question-text').textContent = question.questionText;
	document.getElementById('point-badge').textContent = question.point + '점';

	if (question.imageUrl) {
		var img = document.getElementById('question-image');
		img.src = '/uploads/quiz/' + question.imageUrl;
		img.style.display = 'block';
	} else {
		document.getElementById('question-image').style.display = 'none';
	}

	if (question.quizTypeCode === 2) {
		document.getElementById('options-container').style.display = 'block';
		document.getElementById('textarea-container').style.display = 'none';
		displayMultipleChoice(question.options);
	} else if (question.quizTypeCode === 1) {
		document.getElementById('options-container').style.display = 'none';
		document.getElementById('textarea-container').style.display = 'block';
		document.getElementById('answer-textarea').value = '';
	}

	document.getElementById('error-message').style.display = 'none';
	autoRestoreIfExists();
	attachAutoSaveEvents();
}

function displayMultipleChoice(options) {
	var container = document.getElementById('options-container');
	container.innerHTML = '';

	if (!Array.isArray(options) || options.length === 0) {
		container.innerHTML = '<p class="text-danger fs-5">객관식 보기가 없습니다.</p>';
		return;
	}

	options.forEach(function(option) {
		var optionHtml = `
		            <div class="mb-3">
		                <div class="option-box p-3 border rounded" 
		                     data-option="${option.optionNumber}"
		                     style="border: 2px solid #e9ecef; cursor: pointer; transition: all 0.2s ease; background-color: #fff;">
		                    <input type="radio" name="answer" 
		                           id="option${option.optionNumber}" 
		                           value="${option.optionNumber}"
		                           style="display: none;">
		                    <label style="cursor: pointer; display: flex; align-items: center; margin: 0; width: 100%;">
		                        <span class="badge badge-success px-3 py-2" style="font-size: 15px; min-width: 50px;">${option.optionNumber}</span>
		                        <span style="margin-left: 10px; font-size: 18px;">${option.optionText}</span>
		                    </label>
		                </div>
		            </div>
		        `;
		container.innerHTML += optionHtml;
	});

	// 클릭 이벤트 추가
	document.querySelectorAll('.option-box').forEach(box => {
		box.addEventListener('click', function() {
			const optionNum = this.getAttribute('data-option');
			const radio = document.getElementById(`option${optionNum}`);
			radio.checked = true;

			// 모든 박스의 스타일 초기화
			document.querySelectorAll('.option-box').forEach(b => {
				b.style.borderColor = '#e9ecef';
				b.style.backgroundColor = '#fff';
			});

			// 선택된 박스 강조
			this.style.borderColor = '#1cc88a';
			this.style.backgroundColor = '#f0fff4';

			// 자동 저장
			debounceAutoSave();
		});
	});

	attachAutoSaveEvents();
}


function submitAnswer() {
	if (!currentQuestionData) return;
	var answer = getAnswerValue();

	if (!answer) {
		showError('답을 선택/입력해주세요');
		return;
	}

	document.getElementById('submit-btn').disabled = true;

	var payload = {
		userId: userId,
		questionId: currentQuestionData.questionId,
		selectedOption: currentQuestionData.quizTypeCode === 2 ? parseInt(answer) : null,
		textAnswer: currentQuestionData.quizTypeCode === 1 ? answer : null
	};

	stompClient.send('/app/quiz/answer/' + roomCode, {}, JSON.stringify(payload));

	const key = `quiz_temp_${roomCode}_q${currentQuestionData.questionId}`;
	localStorage.removeItem(key);

	setTimeout(function() {
		document.getElementById('submit-btn').disabled = false;
	}, 1000);
}

function displayRanking(ranking) {
	if (!ranking || ranking.length === 0) return;

	var rankingHtml = '';
	ranking.forEach(function(player) {
		const isSubmitted = submittedUsers.has(player.userId);

		rankingHtml += `
			<div class="d-flex justify-content-between align-items-center pb-3 border-bottom" data-user-id="${player.userId}">
				<span class="badge badge-pill badge-success px-3 py-2" style="font-size: 14px;">${player.rank}</span>
				<span class="font-weight-500 fs-6">
					${player.nickname}
					<span class="ml-2" style="font-size: 16px;">
						${isSubmitted ? '✅' : '⏳'}
					</span>
				</span>
				<span class="badge badge-success px-3 py-2" style="font-size: 14px;">${player.score || 0}점</span>
			</div>
		`;
	});

	document.getElementById('ranking-list').innerHTML = rankingHtml;
}

function markSubmitted(userId) {
	submittedUsers.add(userId);
	displayRanking(latestRanking);
}

function updateSubmitProgress(submittedCount, totalPlayers) {
	const el = document.getElementById('submit-progress');
	if (!el) return;

	if (!totalPlayers || totalPlayers <= 0) {
		el.textContent = '';
		return;
	}

	el.textContent = `제출 현황: ${submittedCount} / ${totalPlayers}`;
}

function finishQuiz() {
	alert('퀴즈가 완료되었습니다!');
	window.location.href = '/quiz-result/' + roomCode;
}

function showError(message) {
	var errorDiv = document.getElementById('error-message');
	errorDiv.textContent = message;
	errorDiv.style.display = 'block';
	setTimeout(() => { errorDiv.style.display = 'none'; }, 3000);
}

window.addEventListener('load', function() { connectWebSocket(); });
window.addEventListener('beforeunload', function() {
	if (stompClient && stompClient.connected) {
		stompClient.disconnect();
	}
});