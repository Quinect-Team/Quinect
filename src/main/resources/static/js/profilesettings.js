$(document).ready(function() {
    // 1. UI 초기화 (기본 선택박스 활성화 등)
    if ($('#borderSection').length > 0) checkDefaultActive('#borderSection');
    if ($('#themeSection').length > 0) checkDefaultActive('#themeSection');

    // 2. [핵심 추가] 초기값 로드
    // 페이지 로딩 시점에 'active'되어 있는 항목의 ID를 hidden input에 넣어둬야
    // 하나만 바꿨을 때 다른 하나가 사라지지 않습니다.
    initCurrentValues();
});

// --- [초기값 세팅 함수] ---
function initCurrentValues() {
    // 테두리 섹션에서 active인 녀석 찾기
    var $activeBorder = $('#borderSection .selection-box.active');
    if ($activeBorder.length > 0) {
        // data-id 값을 가져와서 input에 넣음
        var id = $activeBorder.data('id'); 
        $('#inputBorderId').val(id);
    }

    // 테마 섹션에서 active인 녀석 찾기
    var $activeTheme = $('#themeSection .selection-box.active');
    if ($activeTheme.length > 0) {
        var id = $activeTheme.data('id');
        $('#inputThemeId').val(id);
    }
}

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
    // closest('.row')를 사용하므로 다른 섹션(테두리<->테마)에는 영향을 주지 않음
    var $row = $(element).closest('.row');
    $row.find('.selection-box').removeClass('active');
    $(element).addClass('active');

    // 2. UI용 input에 값 넣기
    // 클릭한 순간 해당 타입의 값만 갱신됨 (나머지 타입은 initCurrentValues 덕분에 기존 값 유지)
    if (type === 'border') {
        $('#inputBorderId').val(id);
    } else if (type === 'theme') {
        $('#inputThemeId').val(id);
    }
}

// --- [저장 로직 (AJAX)] ---
function submitEquipForm() {
    // UI용 인풋 값을 실제 전송용 폼(hidden)으로 복사
    // 이때 값이 비어있으면(null) 서버에서 "장착 해제"로 처리됨
    $('#realBorderId').val( $('#inputBorderId').val() );
    $('#realThemeId').val( $('#inputThemeId').val() );
    
    saveViaAjax('equipForm', '/profile/settings/equip');
}

// ... saveViaAjax 함수는 기존과 동일 ...
function saveViaAjax(formId, url) {
    var form = $('#' + formId)[0];
    var formData = new FormData(form);
    var token = $("meta[name='_csrf']").attr("content");
    var header = $("meta[name='_csrf_header']").attr("content");

    $.ajax({
        url: url,
        type: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        beforeSend: function(xhr) {
            if (token && header) xhr.setRequestHeader(header, token);
        },
        success: function(response) {
            Swal.fire({
                icon: 'success',
                title: '저장 완료!',
                text: '성공적으로 변경되었습니다.',
                confirmButtonColor: '#1cc88a',
                confirmButtonText: '확인',
                allowOutsideClick: false
            }).then((result) => {
                if (result.isConfirmed) window.location.href = '/profile';
            });
        },
        error: function(xhr) {
            Swal.fire({
                icon: 'error',
                title: '오류 발생',
                text: '저장 중 문제가 발생했습니다.',
                confirmButtonColor: '#e74a3b'
            });
        }
    });
}

function previewImage(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function(e) {
            // 미리보기 이미지 태그의 src를 변경
            $('#profilePreview').attr('src', e.target.result);
            // 라벨 텍스트 변경
            $('.custom-file-label').text(input.files[0].name);
        }
        reader.readAsDataURL(input.files[0]);
    }
}