$(document).ready(function() {
    // 1. [테마 탭] 초기 UI 설정 (기본 선택박스 활성화)
    // 요소가 화면에 존재할 때만 실행 (에러 방지)
    if ($('#borderSection').length > 0) checkDefaultActive('#borderSection');
    if ($('#themeSection').length > 0) checkDefaultActive('#themeSection');
});

// --- [UI 제어 함수들] ---

// 섹션 내에 active가 없으면 '기본' 박스를 active로 만듦
function checkDefaultActive(sectionId) {
    if ($(sectionId).find('.selection-box.active').length === 0) {
        $(sectionId).find('.default-box').addClass('active');
    }
}

// 아이템 클릭 시 UI 변경 및 값 주입
function selectItem(type, id, element) {
    // 1. UI 변경 (형제 요소들의 active 제거 -> 나에게 추가)
    var $row = $(element).closest('.row');
    $row.find('.selection-box').removeClass('active');
    $(element).addClass('active');

    // 2. UI용 input에 값 넣기 (화면용)
    if (type === 'border') {
        $('#inputBorderId').val(id);
    } else if (type === 'theme') {
        $('#inputThemeId').val(id);
    }
}

// --- [저장 로직 (AJAX)] ---

// [테마 탭] 저장 버튼 클릭 시 호출되는 함수
function submitEquipForm() {
    // 1. UI용 인풋 값을 실제 전송용 폼(hidden)으로 복사
    $('#realBorderId').val( $('#inputBorderId').val() );
    $('#realThemeId').val( $('#inputThemeId').val() );
    
    // 2. AJAX 전송 함수 호출
    saveViaAjax('equipForm', '/profile/settings/equip');
}

// [공통] AJAX 저장 및 알림 처리 함수
function saveViaAjax(formId, url) {
    var form = $('#' + formId)[0];
    var formData = new FormData(form);

    // HTML 헤더에서 CSRF 토큰 값 가져오기 (403 Forbidden 방지)
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajax({
        url: url,
        type: 'POST',
        data: formData,
        processData: false, // 파일 전송 시 필수
        contentType: false, // 파일 전송 시 필수
        beforeSend: function(xhr) {
            // 헤더에 토큰 싣기
            if (token && header) {
                xhr.setRequestHeader(header, token);
            }
        },
        success: function(response) {
            // 성공 시 SweetAlert 알림
            Swal.fire({
                icon: 'success',
                title: '저장 완료!',
                text: '성공적으로 변경되었습니다.',
                confirmButtonColor: '#1cc88a',
                confirmButtonText: '확인',
                allowOutsideClick: false
            }).then((result) => {
                if (result.isConfirmed) {
                    // 확인 누르면 프로필 페이지로 이동
                    window.location.href = '/profile';
                }
            });
        },
        error: function(xhr) {
            // 실패 시 알림
            Swal.fire({
                icon: 'error',
                title: '오류 발생',
                text: '저장 중 문제가 발생했습니다. 다시 시도해주세요.',
                confirmButtonColor: '#e74a3b'
            });
        }
    });
}