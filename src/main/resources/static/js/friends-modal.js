(function($) {
    'use strict';

    /**
     * ========================================
     * 친구 모달 관련 함수들
     * ========================================
     */

    /**
     * 친구 모달 열기
     * 모달을 열면서 모든 친구 관계 자동 로드
     */
    function openFriendModal(e) {
        if (e) e.preventDefault();

        // 사이드바가 열려있으면 닫기
        if ($('#sidebarModal').hasClass('show')) {
            $('#sidebarModal').removeClass('show');
            $('#sidebarModalBackdrop').css('display', 'none');
            setTimeout(function() {
                $('#sidebarModal').css('display', 'none');
            }, 300);
        }

        // 모달 표시
        $('.modalPOP').show();
        
        // ⭐ 모달 열릴 때 내가 포함된 모든 친구 관계 자동 로드
        loadAllFriendships();
    }

    /**
     * 친구 모달 닫기
     */
    function closeFriendModal() {
        $('.modalPOP').hide();
        $('.friends-list').html('');
        $('#friendSearch').val('');
    }

    /**
     * ========================================
     * 데이터 로드 함수들
     * ========================================
     */

    /**
     * ⭐⭐⭐ 내가 포함된 모든 친구 관계 로드
     * - 내가 보낸 요청
     * - 받은 요청
     * - 이미 친구인 관계
     * - 거절된 관계
     * 모두 한 번에 가져옴
     */
    function loadAllFriendships() {
        console.log('모든 친구 관계 로드 중...');
        
        $.ajax({
            url: '/api/friends/all',
            type: 'GET',
            success: function(users) {
                console.log('조회된 관계:', users.length + '개');
                
                if (users.length === 0) {
                    // 관계가 없으면 빈 상태 표시
                    displayEmptyState('친구 요청 및 관계');
                } else {
                    // 관계가 있으면 목록 표시
                    displayResults(users, '나와 관련된 친구 요청');
                }
            },
            error: function(xhr) {
                console.error('조회 실패:', xhr);
                displayEmptyState('친구 요청 및 관계');
            }
        });
    }

    /**
     * 이메일로 사용자 검색
     * 
     * @param email 검색할 이메일
     */
    function searchUsers(email) {
        console.log('사용자 검색:', email);
        
        $.ajax({
            url: '/api/friends/search',
            type: 'GET',
            data: { email: email },
            success: function(users) {
                console.log('검색 결과:', users.length + '명');
                displayResults(users, '검색 결과');
            },
            error: function(xhr) {
                console.error('검색 실패:', xhr);
                alert('사용자 검색에 실패했습니다.');
            }
        });
    }

    /**
     * ========================================
     * UI 표시 함수들
     * ========================================
     */

    /**
     * 검색 결과 또는 관계 목록 표시
     * 
     * @param users 표시할 사용자 목록
     * @param headerText 헤더 텍스트
     */
    function displayResults(users, headerText) {
        const $list = $('.friends-list');
        $list.empty();

        if (users.length === 0) {
            displayEmptyState(headerText);
            return;
        }

        // 헤더 추가 (개수 포함)
        $list.append(`
            <div class="mb-3">
                <h6 class="text-muted small font-weight-bold">
                    ${headerText} (${users.length}개)
                </h6>
            </div>
        `);

        // 각 사용자 항목 추가
        users.forEach(function(user) {
            $list.append(createUserItem(user));
        });
    }

    /**
     * 빈 상태 표시
     * 검색 결과나 친구 관계가 없을 때 표시
     * 
     * @param headerText 헤더 텍스트
     */
    function displayEmptyState(headerText) {
        const $list = $('.friends-list');
        $list.html(`
            <div class="text-center py-5 text-muted">
                <i class="fas fa-user-friends mb-3" style="font-size: 48px; opacity: 0.3;"></i>
                <p class="mb-1">${headerText}</p>
                <p class="small mb-0">아직 친구 요청이 없습니다.</p>
            </div>
        `);
    }

    /**
     * ========================================
     * 사용자 항목 생성 함수
     * ========================================
     */

    /**
     * ⭐⭐⭐ 사용자 항목 HTML 생성
     * 친구 상태에 따라 다른 버튼을 표시
     * 
     * @param user 사용자 정보
     * @returns HTML 문자열
     */
    function createUserItem(user) {
        console.log('사용자:', user.email, '상태:', user.friendshipStatus);
        
        let buttonHtml;
        let statusBadge = '';
        
        if (user.friendshipStatus === 'PENDING_SENT') {
            // ⭐ 내가 보낸 요청 - 대기중
            buttonHtml = `
                <button type="button" class="btn btn-warning btn-sm" disabled>
                    <i class="fas fa-clock"></i> 대기중
                </button>
            `;
            statusBadge = '<span class="badge badge-light ml-2 text-dark small">내가 보냄</span>';
            
        } else if (user.friendshipStatus === 'PENDING_RECEIVED') {
            // ⭐⭐⭐ 받은 요청 - 수락/거절 버튼
            buttonHtml = `
                <div class="d-flex gap-2">
                    <button type="button" 
                            class="btn btn-success btn-sm accept-friend-btn" 
                            data-user-id="${user.id}"
                            title="수락">
                        <i class="fas fa-check"></i> 수락
                    </button>
                    <button type="button" 
                            class="btn btn-outline-danger btn-sm reject-friend-btn" 
                            data-user-id="${user.id}"
                            title="거절">
                        <i class="fas fa-times"></i> 거절
                    </button>
                </div>
            `;
            statusBadge = '<span class="badge badge-info ml-2 small">받은 요청</span>';
            
        } else if (user.friendshipStatus === 'ACCEPTED') {
            // ⭐ 이미 친구
            buttonHtml = `
                <button type="button" class="btn btn-secondary btn-sm" disabled>
                    <i class="fas fa-check"></i> 친구
                </button>
            `;
            statusBadge = '<span class="badge badge-success ml-2 small">친구</span>';
            
        } else if (user.friendshipStatus === 'REJECTED') {
            // ⭐ 거절된 요청
            buttonHtml = `
                <button type="button" class="btn btn-danger btn-sm" disabled>
                    <i class="fas fa-times"></i> 거절됨
                </button>
            `;
            statusBadge = '<span class="badge badge-danger ml-2 small">거절됨</span>';
            
        } else {
            // ⭐ 친구 요청 가능 (아무 관계 없음)
            buttonHtml = `
                <button type="button" 
                        class="btn btn-success btn-sm add-friend-btn" 
                        data-user-id="${user.id}">
                    <i class="fas fa-user-plus"></i> 추가
                </button>
            `;
        }
        
        // 사용자 항목 HTML 반환
        return `
            <div class="friend-item bg-light p-3 mb-2 rounded d-flex align-items-center">
                <!-- 프로필 이미지 -->
                <div class="position-relative mr-3">
                    <img src="${user.profileImage || '/img/default-avatar.png'}" 
                         alt="프로필"
                         class="rounded-circle border border-gray-300"
                         style="width: 48px; height: 48px; object-fit: cover;">
                </div>
                
                <!-- 사용자 정보 -->
                <div class="flex-grow-1">
                    <div class="font-weight-bold text-gray-800 small mb-1">
                        ${user.username || '이름 없음'}
                        ${statusBadge}
                    </div>
                    <div class="text-muted small text-truncate">
                        ${user.email}
                    </div>
                </div>
                
                <!-- 버튼 -->
                <div style="margin-left: auto; flex-shrink: 0;">
                    ${buttonHtml}
                </div>
            </div>
        `;
    }

    /**
     * ========================================
     * 친구 요청 함수
     * ========================================
     */

    /**
     * ⭐ 친구 요청 보내기
     * CSRF 토큰을 포함해서 서버에 POST 요청
     * 
     * @param receiverId 친구 요청을 받을 사용자 ID
     * @param $button 클릭된 버튼 jQuery 객체
     */
    function sendFriendRequest(receiverId, $button) {
        console.log('친구 요청 함수 실행:', receiverId);

        // CSRF 토큰 가져오기 (HTML meta 태그에서)
        const csrfToken = $('meta[name="_csrf"]').attr('content');
        const csrfHeader = $('meta[name="_csrf_header"]').attr('content');

        console.log('CSRF 토큰:', csrfToken ? '있음' : '없음');

        $.ajax({
            url: '/api/friends/request',
            type: 'POST',
            data: { receiverId: receiverId },
            beforeSend: function(xhr) {
                // CSRF 토큰을 헤더에 추가
                if (csrfToken && csrfHeader) {
                    xhr.setRequestHeader(csrfHeader, csrfToken);
                }
            },
            success: function(response, textStatus, xhr) {
                console.log('=== 응답 받음 ===');
                console.log('상태 코드:', xhr.status);
                console.log('응답 내용:', response);

                // HTML 응답 체크 (CSRF 에러 등으로 로그인 페이지 반환 시)
                const responseStr = String(response).trim();
                if (responseStr.startsWith('<!DOCTYPE') ||
                    responseStr.startsWith('<html')) {
                    
                    console.error('❌ HTML 응답 받음 - 인증 실패!');
                    alert('인증 오류입니다.\n페이지를 새로고침해주세요.');
                    return;
                }

                // ⭐ 정상 응답
                console.log('✅ 친구 요청 성공!');
                alert(response);

                // 버튼을 "대기중"으로 변경
                $button.removeClass('btn-success add-friend-btn')
                       .addClass('btn-warning')
                       .prop('disabled', true)
                       .html('<i class="fas fa-clock"></i> 대기중');
                
                // 0.5초 후 목록 새로고침
                setTimeout(function() {
                    loadAllFriendships();
                }, 500);
            },
            error: function(xhr, status, error) {
                console.error('=== 에러 발생 ===');
                console.error('상태 코드:', xhr.status);
                console.error('상태:', status);
                console.error('에러:', error);

                let errorMsg = '친구 요청에 실패했습니다.';

                if (xhr.status === 403) {
                    errorMsg = 'CSRF 토큰 오류입니다.\n페이지를 새로고침해주세요.';
                } else if (xhr.status === 400 && xhr.responseText) {
                    errorMsg = xhr.responseText;
                } else if (xhr.status === 500) {
                    errorMsg = '서버 오류가 발생했습니다.';
                }

                alert(errorMsg);
            }
        });
    }

    /**
     * ========================================
     * 글로벌 함수 노출
     * ========================================
     */

    // HTML에서 직접 호출할 수 있도록 window에 추가
    window.openFriendModal = openFriendModal;
    window.closeFriendModal = closeFriendModal;

    /**
     * ========================================
     * DOM 로드 후 이벤트 바인딩
     * ========================================
     */

    $(document).ready(function() {
        console.log('friends-modal.js 로드됨');
        
        // ========== 모달 관련 이벤트 ==========
        
        // 닫기 버튼 클릭
        $('.closebtn').on('click', closeFriendModal);

        // 배경 클릭 시 모달 닫기
        $('.modalPOP').on('click', function(e) {
            if ($(e.target).hasClass('modalPOP')) {
                closeFriendModal();
            }
        });

        // ========== 검색 관련 이벤트 ==========
        
        // 검색 버튼 클릭
        $('#searchBtn').on('click', function() {
            const email = $('#friendSearch').val().trim();

            if (email.length === 0) {
                // 검색어 없으면 전체 목록
                loadAllFriendships();
                return;
            }

            if (email.length < 2) {
                alert('최소 2글자 이상 입력하세요.');
                return;
            }

            // 검색 실행
            searchUsers(email);
        });

        // 엔터키로 검색
        $('#friendSearch').on('keypress', function(e) {
            if (e.which === 13) { // Enter 키
                e.preventDefault();
                $('#searchBtn').click();
            }
        });

        // ========== 친구 관련 이벤트 (이벤트 위임) ==========
        
        // ⭐ 친구 추가 버튼 클릭
        $('.friends-list').on('click', '.add-friend-btn', function() {
            console.log('추가 버튼 클릭됨!');
            
            const receiverId = $(this).data('user-id');
            const $button = $(this);
            
            console.log('받는 사람 ID:', receiverId);
            
            if (confirm('친구 요청을 보내시겠습니까?')) {
                sendFriendRequest(receiverId, $button);
            }
        });
        
        // ⭐⭐⭐ 친구 수락 버튼 클릭 (기능 아직 없음)
        $('.friends-list').on('click', '.accept-friend-btn', function() {
            console.log('수락 버튼 클릭됨!');
            
            const userId = $(this).data('user-id');
            console.log('수락할 사용자 ID:', userId);
            
            // TODO: 수락 기능 구현
            // acceptFriendRequest(userId);
        });
        
        // ⭐⭐⭐ 친구 거절 버튼 클릭 (기능 아직 없음)
        $('.friends-list').on('click', '.reject-friend-btn', function() {
            console.log('거절 버튼 클릭됨!');
            
            const userId = $(this).data('user-id');
            console.log('거절할 사용자 ID:', userId);
            
            // TODO: 거절 기능 구현
            // rejectFriendRequest(userId);
        });
        
        console.log('이벤트 바인딩 완료');
    });

})(jQuery);
