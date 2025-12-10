// ⭐ 전역 변수: 현재 친구 관계 데이터 저장 (IIFE 밖에!)
window.currentFriendships = { received: [], sent: [], accepted: [] };

// ⭐ 현재 열려 있는 채팅 대상 전역 보관
window.currentChatUserId = null;
window.currentChatUsername = null;
window.currentChatEmail = null;


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

		// ✅ show 클래스가 없으면 추가
		if (!$('.modalPOP').hasClass('show')) {
			$('.modalPOP').addClass('show');
		}

		/*$('#sidebarModalBackdrop').css('display', 'block');*/
		$('#friendsModal').css('display', 'flex');
		$('#chatModal').css('display', 'none');

		loadAllFriendships();
	}

	function closeFriendModal() {
		// ✅ .hide() 메서드 → .removeClass('show') 클래스 제거로 변경
		$('.modalPOP').removeClass('show');  // ← 이렇게!
		$('#sidebarModalBackdrop').css('display', 'none');

		$('#friendshipContainer').html('');
		$('#friendSearch').val('');
		currentFriendships = { received: [], sent: [], accepted: [] };

		currentChatUserId = null;
		currentChatUsername = null;
		currentChatEmail = null;
		$('#messageHistory').empty();
		$('#messageInput').val('');
	}

	/**
	 * ✅ 친구 목록 → 채팅창으로 전환
	 */
	function switchToChatView(userId, username, email) {
		console.log('채팅 모달로 전환:', userId, username, email);

		currentChatUserId = userId;
		currentChatUsername = username;
		currentChatEmail = email;

		// 친구 목록 숨기고 채팅 영역 보이기
		$('#friendsModal').hide();
		$('#chatModal').show();

		// 채팅 상대 정보 세팅
		$('#chatFriendName').text(username || '알 수 없는 사용자');
		$('#chatFriendEmail').text(email || '');

		// 기존 메시지 영역 초기화
		$('#messageHistory').html(
			'<p class="text-center text-muted small">메시지가 없습니다.</p>'
		);

		// 입력창 포커스
		setTimeout(function() {
			$('#messageInput').focus();
		}, 100);

		loadMessageHistory(userId);
	}

	/**
	 * ✅ 채팅창 → 친구 목록으로 복귀
	 */
	function backToFriendsList() {
		console.log('친구 목록으로 돌아가기');

		$('#chatModal').hide();
		$('#friendsModal').show();

		currentChatUserId = null;
		currentChatUsername = null;
		currentChatEmail = null;
		$('#messageHistory').empty();
		$('#messageInput').val('');
	}

	/**
	 * 모든 친구 관계 로드 (항상 위에 고정)
	 */
	function loadAllFriendships() {
		console.log("모든 친구 관계 로드 중...");

		$.ajax({
			type: 'GET',
			url: '/api/friends/all',
			dataType: 'json',
			success: function(data) {
				currentFriendships = data;

				const received = data.received || [];
				const sent = data.sent || [];
				const accepted = data.accepted || [];

				console.log("친구 관계 로드 성공 - 받은요청:", received.length, "보낸요청:", sent.length, "친구:", accepted.length);

				displayFriendshipsOnly(received, sent, accepted);
			},
			error: function(error) {
				console.error("조회 실패:", error);
				displayEmptyState('친구 요청을 불러올 수 없습니다.');
			}
		});
	}

	/**
	 * 이메일로 사용자 검색
	 */
	function searchUsers(email) {
		console.log('사용자 검색:', email);

		$.ajax({
			url: '/api/friends/search',
			type: 'GET',
			data: { email: email },
			success: function(users) {
				console.log('검색 결과:', users.length + '명');

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
		console.log('사용자:', user.email, '상태:', user.friendshipStatus);

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
                        class="btn btn-info btn-sm send-message-btn"
                        data-user-id="${user.id}"
                        data-username="${user.username}"
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
		console.log('친구 요청 함수 실행:', receiverId);

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
				console.log('✅ 친구 요청 성공!');
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
				console.error('❌ 친구 요청 실패:', xhr);
				alert(xhr.responseText || '친구 요청에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 요청 수락
	 */
	function acceptFriendRequest(friendshipId) {
		console.log('친구 요청 수락:', friendshipId);

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
				console.log('✅ 친구 요청 수락 성공!');
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 1000);
			},
			error: function(xhr) {
				console.error('❌ 친구 요청 수락 실패:', xhr);
				alert(xhr.responseText || '친구 요청 수락에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 요청 거절/차단
	 */
	function handleRejectOption(friendshipId, action) {
		console.log(action === 'reject' ? '친구 요청 거절' : '친구 차단:', friendshipId);

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
				console.log('✅ ' + (action === 'reject' ? '거절' : '차단') + ' 완료!');
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 1000);
			},
			error: function(xhr) {
				console.error('❌ ' + (action === 'reject' ? '거절' : '차단') + ' 실패:', xhr);
				alert(xhr.responseText || (action === 'reject' ? '거절' : '차단') + '에 실패했습니다.');
			}
		});
	}

	/**
	 * 친구 삭제
	 */
	function removeFriend(friendshipId) {
		console.log('친구 삭제:', friendshipId);

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
				console.log('✅ 친구 삭제 성공!');
				alert(response);

				setTimeout(function() {
					loadAllFriendships();
				}, 500);
			},
			error: function(xhr) {
				console.error('❌ 친구 삭제 실패:', xhr);
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

		if (!currentChatUserId) {
			alert('대화 대상이 선택되지 않았습니다');
			return;
		}

		console.log('메시지 전송:', currentChatUserId, text);

		// ⭐ CSRF 토큰 가져오기
		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		// ⭐ Friendship ID 가져오기
		const friendshipId = findFriendshipId(currentChatUserId);
		if (!friendshipId) {
			alert('친구 관계를 찾을 수 없습니다');
			return;
		}

		// ⭐ AJAX로 메시지 전송
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
				console.log('✅ 메시지 전송 성공!', response);

				// ⭐ [중요] 입력창 즉시 클리어
				$('#messageInput').val('').focus();

				// ⭐ [중요] 응답으로 받은 메시지를 즉시 화면에 표시
				if (response && response.id) {
					console.log('⭐ 로컬에서 즉시 메시지 표시:', response);
					displayMessage(response);

					// 스크롤 자동 아래로
					setTimeout(function() {
						var historyDiv = $('#messageHistory');
						historyDiv.scrollTop(historyDiv[0].scrollHeight);
					}, 50);
				}
			}
			,
			error: function(xhr) {
				console.error('❌ 메시지 전송 실패:', xhr);
				const errorMsg = xhr.responseText || '메시지 전송에 실패했습니다';
				alert(errorMsg);
			}
		});
	}

	/**
	 * 메시지 기록 조회 및 표시
	 */
	/**
	 * 메시지 기록 조회 및 표시
	 */
	function loadMessageHistory(friendUserId) {
		console.log('메시지 기록 조회:', friendUserId);

		// ⭐ CSRF 토큰
		const csrfToken = $('meta[name="_csrf"]').attr('content');
		const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

		$.ajax({
			url: '/api/friend-messages/' + friendUserId,
			type: 'GET',
			beforeSend: function(xhr) {
				if (csrfToken && csrfHeader) {
					xhr.setRequestHeader(csrfHeader, csrfToken);
				}
			},
			success: function(messages) {
				console.log('✅ 메시지 기록 조회 성공!');
				console.log('전체 응답:', JSON.stringify(messages, null, 2));  // ⭐ 전체 구조 출력

				if (!messages || messages.length === 0) {
					$('#messageHistory').html(
						'<p class="text-center text-muted small">메시지가 없습니다.</p>'
					);
					return;
				}

				// 첫 번째 메시지 구조 상세히 출력
				console.log('첫 번째 메시지 상세:', JSON.stringify(messages[0], null, 2));

				// 메시지 히스토리 초기화
				$('#messageHistory').empty();

				// 메시지 표시
				messages.forEach(function(msg) {
					displayMessage(msg);
				});

				// 스크롤을 맨 아래로
				setTimeout(function() {
					$('#messageHistory').scrollTop($('#messageHistory')[0].scrollHeight);
				}, 100);
			},
			error: function(xhr) {
				console.error('❌ 메시지 기록 조회 실패:', xhr);
				$('#messageHistory').html(
					'<p class="text-center text-muted small">메시지를 불러올 수 없습니다.</p>'
				);
			}
		});
	}


	/**
	 * 메시지 하나 표시
	 */
	/**
	 * 메시지 하나 표시
	 */
	function displayMessage(msg) {
		console.log('메시지 표시:', JSON.stringify(msg, null, 2));

		const currentUserId = $('body').data('user-id');
		console.log('현재 사용자 ID:', currentUserId);
		console.log('메시지 발신자 ID:', msg.senderId);

		// ⭐ sentAt을 Date 객체로 변환
		const messageTime = new Date(msg.sentAt).toLocaleTimeString('ko-KR', {
			hour: '2-digit',
			minute: '2-digit'
		});

		// 내 메시지는 오른쪽, 상대 메시지는 왼쪽
		if (msg.senderId === currentUserId) {
			// 내 메시지
			$('#messageHistory').append(`
	            <div class="mb-2 d-flex justify-content-end">
	                <div class="card bg-success text-white" style="max-width: 70%; word-break: break-word;">
	                    <div class="card-body p-2">
	                        <p class="mb-0">${escapeHtml(msg.messageText)}</p>
	                        <small class="text-white-50" style="font-size: 0.75rem;">
	                            ${messageTime}
	                        </small>
	                    </div>
	                </div>
	            </div>
	        `);
		} else {
			// 상대 메시지
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
	}



	/**
	 * User ID로 Friendship ID 찾기
	 */
	function findFriendshipId(userId) {
		const accepted = currentFriendships.accepted || [];

		console.log("friendshipId 찾기 - userId:", userId);
		console.log("accepted 배열:", JSON.stringify(accepted));

		for (let friend of accepted) {
			console.log("비교 중 - friend.id:", friend.id, "userId:", userId);

			// ⭐ friend.id가 userId와 일치하면, friend 객체 자체가 friendship
			if (friend.id === userId) {
				console.log("✅ 찾음! friend:", JSON.stringify(friend));
				// friendshipId 또는 id를 반환 (API 응답 구조에 따라)
				return friend.friendshipId || friend.id;
			}
		}

		console.log("❌ friendshipId를 찾을 수 없습니다");
		return null;
	}


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

	window.switchToChatView = switchToChatView;
	window.backToFriendsList = backToFriendsList;
	window.sendMessage = sendMessage;

	/**
	 * 1:1 채팅 메시지 실시간 수신 대기 (재구독 가능한 버전)
	 */
	function subscribeToPrivateMessages() {
		console.log('📢 [subscribeToPrivateMessages] 함수 호출됨');

		if (!stompClient || !stompClient.connected) {
			console.warn('⚠️ WebSocket이 아직 연결되지 않았습니다. 5초 후 재시도...');
			setTimeout(subscribeToPrivateMessages, 5000);
			return;
		}

		console.log('✅ WebSocket 연결 확인됨, stompClient.connected =', stompClient.connected);

		if (window.messageSubscription) {
			console.log('⚠️ 이미 구독 중입니다. 기존 구독 해제 후 재구독...');
			window.messageSubscription.unsubscribe();
		}

		console.log('📢 [SUBSCRIBE] /user/queue/friend-messages 구독 시작...');

		try {
			window.messageSubscription = stompClient.subscribe('/user/queue/friend-messages', function(message) {
				var msg = JSON.parse(message.body);

				console.log('\n⚡⚡⚡ [WebSocket 실시간 메시지 도착!]');
				console.log('📬 메시지 ID:', msg.id);
				console.log('📬 메시지 발신자 ID:', msg.senderId);
				console.log('📬 메시지 발신자명:', msg.senderName);
				console.log('📬 메시지 내용:', msg.messageText);
				console.log('📬 메시지 시간:', msg.sentAt);

				// 현재 상태 확인
				console.log('📢 현재 채팅 모달 열려있음?', $('#chatModal').is(':visible'));
				console.log('📢 현재 채팅 대상 ID:', window.currentChatUserId);
				console.log('📢 비교 결과: window.currentChatUserId(' + window.currentChatUserId + ') == msg.senderId(' + msg.senderId + ') = ' + (window.currentChatUserId == msg.senderId));

				// ⭐ [수정] 조건 체크 없이 무조건 표시!
				// 이유: 
				// 1. 메시지 도착 = 상대방이 보낸 것
				// 2. 내가 받은 메시지를 무조건 표시해야 함
				// 3. 채팅 모달 상태와 상관없이 받으면 표시

				console.log('✅ WebSocket 메시지 수신 → displayMessage() 호출');
				displayMessage(msg);

				// ⭐ 자동 스크롤 (메시지 도착하면 아래로)
				setTimeout(function() {
					var historyDiv = $('#messageHistory');
					if (historyDiv.length > 0) {
						historyDiv.scrollTop(historyDiv[0].scrollHeight);
						console.log('📜 스크롤 위치 이동:', historyDiv.scrollTop());
					}
				}, 50);

				console.log('');  // 빈 줄
			});

			window.messageSubscribed = true;
			console.log('✅ 개인 메시지 수신 대기 중... (구독 등록 완료)\n');

		} catch (error) {
			console.error('❌ 구독 중 에러 발생:', error);
		}
	}


	/**
	 * 초대 메시지 수신 대기 (재구독 가능한 버전)
	 */
	function subscribeToInvitations() {
		console.log('📢 [subscribeToInvitations] 함수 호출됨');

		if (!stompClient || !stompClient.connected) {
			console.warn('⚠️ WebSocket이 아직 연결되지 않았습니다. 5초 후 재시도...');
			setTimeout(subscribeToInvitations, 5000);
			return;
		}

		console.log('✅ WebSocket 연결 확인됨, stompClient.connected =', stompClient.connected);

		// ⭐ 이미 구독한 경우도 다시 구독
		if (window.invitationSubscription) {
			console.log('⚠️ 이미 구독 중입니다. 기존 구독 해제 후 재구독...');
			window.invitationSubscription.unsubscribe();
		}

		console.log('📢 [SUBSCRIBE] /user/queue/room-invitations 구독 시작...');

		try {
			window.invitationSubscription = stompClient.subscribe('/user/queue/room-invitations', function(message) {
				var invitation = JSON.parse(message.body);
				console.log('🎯 [WebSocket 초대 메시지 수신]', invitation);
				showInvitationNotification(invitation);
			});

			window.invitationSubscribed = true;
			console.log('✅ 초대 메시지 수신 대기 중... (구독 등록 완료)');
		} catch (error) {
			console.error('❌ 초대 구독 중 에러 발생:', error);
		}
	}

	window.subscribeToPrivateMessages = subscribeToPrivateMessages;
	window.subscribeToInvitations = subscribeToInvitations;

	/**
	 * DOM 로드 후 초기화 (올바른 순서)
	 */
	$(document).ready(function() {

		console.log('========== friends-modal.js 로드됨 ==========');

		console.log('🔌 [1단계] WebSocket 초기화 시작...');

		initGlobalWebSocket().then(function() {
			console.log('✅ [1단계 완료] WebSocket 연결됨');

			// ⭐ [2단계] 사용자 정보 로드
			console.log('👤 [2단계] 현재 사용자 정보 로드 시작...');

			$.ajax({
				url: '/api/user/current',
				type: 'GET',
				success: function(user) {
					$('body').data('user-id', user.id);
					$('body').data('user-email', user.email);
					console.log('✅ [2단계 완료] 현재 사용자:', user.id, user.email);

					// ⭐ [3단계] 이제 subscribeToPrivateMessages 호출!
					console.log('📢 [3단계] 메시지 구독 시작...');
					subscribeToPrivateMessages();
					subscribeToInvitations();
				},
				error: function(xhr) {
					console.error('❌ [2단계 실패] 사용자 정보를 가져올 수 없습니다');
				}
			});

		}).catch(function(error) {
			console.error('❌ [1단계 실패] WebSocket 연결 실패:', error);
		});

		console.log('friends-modal.js 초기화 완료\n');

		// ⭐ [이벤트 바인딩] (WebSocket과 상관없으므로 언제든 가능)

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

		// 메시지 버튼 → 채팅 모달로 전환
		$(document).on('click', '.send-message-btn', function() {
			const userId = $(this).data('user-id');
			const username = $(this).data('username');

			const email = $(this)
				.closest('.friend-item')
				.find('.text-muted.small')
				.text()
				.trim();

			console.log('메시지 보내기 클릭 - userId:', userId, 'username:', username, 'email:', email);

			switchToChatView(userId, username, email);
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

		console.log('✅ 이벤트 바인딩 완료\n');
	});

})(jQuery);
