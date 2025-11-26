(function($) {
    'use strict';

    // 친구 모달 열기
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

        $('.modalPOP').show();
    }

    // 친구 모달 닫기
    function closeFriendModal() {
        $('.modalPOP').hide();
        $('.friends-list').html('');
        $('#friendSearch').val('');
    }

    // 사용자 검색
    function searchUsers(email) {
        $.ajax({
            url: '/api/friends/search',
            type: 'GET',
            data: { email: email },
            success: function(users) {
                displayResults(users);
            },
            error: function(xhr) {
                console.error('검색 실패:', xhr);
                alert('사용자 검색에 실패했습니다.');
            }
        });
    }

    // 검색 결과 표시
    function displayResults(users) {
        const $list = $('.friends-list');
        $list.empty();

        if (users.length === 0) {
            $list.html(`
                <div class="text-center py-5 text-muted">
                    <i class="fas fa-search mb-3" style="font-size: 48px; opacity: 0.3;"></i>
                    <p class="mb-1">검색 결과가 없습니다.</p>
                </div>
            `);
            return;
        }

        users.forEach(function(user) {
            $list.append(createUserItem(user));
        });
    }

    // 사용자 항목 HTML 생성
    function createUserItem(user) {
        return `
            <div class="friend-item bg-light p-3 mb-2 rounded d-flex align-items-center">
                <div class="position-relative mr-3">
                    <img src="${user.profileImageUrl || '/img/default-avatar.png'}" 
                         alt="프로필"
                         class="rounded-circle border border-gray-300"
                         style="width: 48px; height: 48px; object-fit: cover;">
                </div>
                <div class="flex-grow-1 text-truncate">
                    <div class="font-weight-bold text-gray-800 small mb-1">
                        ${user.username || '이름 없음'}
                    </div>
                    <div class="text-muted small text-truncate">
                        ${user.email}
                    </div>
                </div>
                <div>
                    <button type="button" class="btn btn-primary btn-sm">
                        <i class="fas fa-user-plus"></i> 추가
                    </button>
                </div>
            </div>
        `;
    }

    // 전역 함수로 노출
    window.openFriendModal = openFriendModal;
    window.closeFriendModal = closeFriendModal;

    // DOM 로드 후 이벤트 바인딩
    $(document).ready(function() {
        // 닫기 버튼
        $('.closebtn').on('click', closeFriendModal);

        // 배경 클릭 시 닫기
        $('.modalPOP').on('click', function(e) {
            if ($(e.target).hasClass('modalPOP')) {
                closeFriendModal();
            }
        });

        // 검색 버튼 클릭
        $('#searchBtn').on('click', function() {
            const email = $('#friendSearch').val().trim();

            if (email.length === 0) {
                alert('이메일을 입력하세요.');
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
            if (e.which === 13) { // Enter
                e.preventDefault();
                $('#searchBtn').click();
            }
        });
    });

})(jQuery);
