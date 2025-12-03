(function($) {
	'use strict';

	// ⭐ 전역 변수: 현재 친구 관계 데이터 저장
	let currentFriendships = { received: [], sent: [], accepted: [] };

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

		$('.modalPOP').show();
		loadAllFriendships();
	}

	/**
	 * 친구 모달 닫기
	 */
	function closeFriendModal() {
		$('.modalPOP').hide();
		$('#friendshipContainer').html('');
		$('#friendSearch').val('');
		currentFriendships = { received: [], sent: [], accepted: [] };
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

				// ⭐ 친구 관계를 항상 맨 위에 표시 (카테고리별로)
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

				// ⭐ 현재 친구 관계 + 검색 결과를 함께 표시
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
	 * ⭐⭐⭐ 친구 관계 + 검색 결과 함께 표시 (카테고리별)
	 */
	function displayFriendshipsWithSearch(searchResults, email) {
		const $container = $('#friendshipContainer');
		$container.empty();

		// 1️⃣ 먼저 현재 친구 관계 표시 (위에 고정)
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

		// 2️⃣ 그 다음 검색 결과 표시 (아래)
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
			// ⭐ 거절 드롭다운 버튼
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
			// ⭐ 차단됨
			buttonHtml = `
	            <button type="button" class="btn btn-danger btn-sm" disabled>
	                <i class="fas fa-ban"></i> 차단됨
	            </button>
	        `;
			statusBadge = '<span class="badge badge-danger ml-2 small">차단됨</span>';

		} else if (user.friendshipStatus === 'BANNED_BY_ME') {
			// ⭐ 내가 차단함
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
	 * 친구 요청 거절
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
	 * ⭐ 친구 삭제
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
	 * 글로벌 함수 노출
	 */
	window.openFriendModal = openFriendModal;
	window.closeFriendModal = closeFriendModal;
	window.acceptFriendRequest = acceptFriendRequest;
	/*window.rejectFriendRequest = rejectFriendRequest;*/
	window.sendFriendRequest = sendFriendRequest;
	window.removeFriend = removeFriend;

	/**
	 * DOM 로드 후 이벤트 바인딩
	 */
	$(document).ready(function() {
		console.log('friends-modal.js 로드됨');

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
				// 검색어 없으면 친구 관계만 표시
				loadAllFriendships();
				return;
			}

			if (email.length < 2) {
				alert('최소 2글자 이상 입력하세요.');
				return;
			}

			// ⭐ 검색 실행 (친구 관계 + 검색 결과 함께 표시)
			searchUsers(email);
		});

		// 엔터키로 검색
		$('#friendSearch').on('keypress', function(e) {
			if (e.which === 13) {
				e.preventDefault();
				$('#searchBtn').click();
			}
		});

		// ⭐ 친구 추가 버튼
		$(document).on('click', '.add-friend-btn', function() {
			const receiverId = $(this).data('user-id');

			if (confirm('친구 요청을 보내시겠습니까?')) {
				sendFriendRequest(receiverId, $(this));
			}
		});

		// ⭐ 친구 수락 버튼
		$(document).on('click', '.accept-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');

			if (confirm('친구 요청을 수락하시겠습니까?')) {
				acceptFriendRequest(friendshipId);
			}
		});

		// ⭐ 친구 거절 버튼
		$(document).on('click', '.reject-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');

			if (confirm('친구 요청을 거절하시겠습니까?')) {
				rejectFriendRequest(friendshipId);
			}
		});

		// ⭐ 친구 삭제 버튼
		$(document).on('click', '.remove-friend-btn', function() {
			const friendshipId = $(this).data('friendship-id');

			if (confirm('이 친구를 삭제하시겠습니까?')) {
				removeFriend(friendshipId);
			}
		});
		
		$(document).on('click', '.send-message-btn', function() {
	        const userId = $(this).data('user-id');
	        const username = $(this).data('username');
	        
	        console.log('메시지 보내기 클릭 - userId:', userId, 'username:', username);
	        
	        // TODO: 메시지 전송 모달 열기 또는 채팅 페이지로 이동
	        alert(username + '님에게 메시지를 보냅니다.');
	        
	        // 예: 채팅 페이지로 이동
	        // window.location.href = '/chat/' + userId;
	    });		

		// ⭐ 거절/차단 드롭다운 이벤트
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

		console.log('이벤트 바인딩 완료');
	});

})(jQuery);
