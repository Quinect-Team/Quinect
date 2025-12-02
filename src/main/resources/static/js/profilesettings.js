// 초기값을 저장할 전역 변수
let initialValues = {};

$(document).ready(function() {
	// --- [1. 테마/테두리 탭 초기화] ---
	if ($('#borderSection').length > 0) checkDefaultActive('#borderSection');
	if ($('#themeSection').length > 0) checkDefaultActive('#themeSection');

	initCurrentValues();

	// --- [2. 프로필 정보 탭 초기화 (변경 감지 로직)] ---

	// (A) 페이지 로딩 시점의 텍스트 초기값 저장
	initialValues = {
		username: $('input[name="username"]').val(),
		organization: $('input[name="organization"]').val(),
		bio: $('input[name="bio"]').val(),
		// [추가] HTML에서 받아온 원본 이미지 경로 저장
		image: $('#initialProfileImageValue').val()
	};

	// (B) 텍스트 입력 감지 이벤트 바인딩 (키보드 뗄 때, 값 변경될 때)
	$('input[name="username"], input[name="organization"], input[name="bio"]').on('keyup change', function() {
		checkChanges();
	});
});

// --- [변경사항 감지 및 저장 버튼 제어 함수] ---
function checkChanges() {
	// 1. 텍스트 필드 비교
	const currentUsername = $('input[name="username"]').val();
	const currentOrg = $('input[name="organization"]').val();
	const currentBio = $('input[name="bio"]').val();

	const isTextChanged = (currentUsername !== initialValues.username) ||
		(currentOrg !== initialValues.organization) ||
		(currentBio !== initialValues.bio);

	let isImageChanged = false;

	const fileInput = $('#profileImageFile')[0];
	const isFileUploaded = fileInput && fileInput.files.length > 0;
	const selectedDefault = $('#inputDefaultProfileImage').val(); // 현재 선택된 기본 이미지

	if (isFileUploaded) {
		// (A) 새 파일을 올렸다면 -> 무조건 변경됨
		isImageChanged = true;
	} else if (selectedDefault !== '') {
		// (B) 기본 캐릭터를 선택했다면 -> 원래 내 이미지랑 같은지 비교
		// 예: 원래 '/img/A.svg'였는데, 다시 '/img/A.svg'를 누르면 변경 안 된 것임
		if (selectedDefault !== initialValues.image) {
			isImageChanged = true;
		}
	} else {
		// (C) 파일도 안 올리고, 기본 캐릭터도 선택 안 함 (기존 상태 유지 중)
		isImageChanged = false;
	}

	// 3. 버튼 상태 결정
	// 텍스트나 이미지 중 하나라도 바뀌었으면 활성화
	if (isTextChanged || isImageChanged) {
		$('#saveProfileBtn').prop('disabled', false);
		$('#saveProfileBtn').removeClass('btn-secondary').addClass('btn-success');
	} else {
		$('#saveProfileBtn').prop('disabled', true);
		$('#saveProfileBtn').removeClass('btn-success').addClass('btn-secondary');
	}
}

// --- [테마/테두리 관련 함수들] ---

function initCurrentValues() {
	var $activeBorder = $('#borderSection .selection-box.active');
	if ($activeBorder.length > 0) {
		var id = $activeBorder.data('id');
		$('#inputBorderId').val(id);
	}

	var $activeTheme = $('#themeSection .selection-box.active');
	if ($activeTheme.length > 0) {
		var id = $activeTheme.data('id');
		$('#inputThemeId').val(id);
	}
}

// 섹션 내에 active가 없으면 '기본' 박스를 active로 만듦
function checkDefaultActive(sectionId) {
	if ($(sectionId).find('.selection-box.active').length === 0) {
		$(sectionId).find('.default-box').addClass('active');
	}
}

// 아이템 클릭 시 UI 변경 및 값 주입 (테마 탭)
function selectItem(type, id, element) {
	var $row = $(element).closest('.row');
	$row.find('.selection-box').removeClass('active')
	$(element).addClass('active');

	if (type === 'border') {
		$('#inputBorderId').val(id);
	} else if (type === 'theme') {
		$('#inputThemeId').val(id);
	}
}

// 테마 저장 로직 (AJAX)
function submitEquipForm() {
	$('#realBorderId').val($('#inputBorderId').val());
	$('#realThemeId').val($('#inputThemeId').val());

	saveViaAjax('equipForm', '/profile/settings/equip');
}

// --- [프로필 정보 관련 함수들] ---

// 기본 프로필 선택 로직
function selectDefaultProfile(path, element) {
	// 1. 선택 박스 UI 처리
	$('#defaultProfileSection .selection-box').removeClass('active');
	$(element).addClass('active');

	// 2. 값 주입
	$('#inputDefaultProfileImage').val(path);

	// 3. 파일 업로드 초기화 (중복 방지)
	$('#profileImageFile').val('');
	$('.custom-file-label').text('파일 선택...');

	// 4. 미리보기 영역 교체 (이미지 숨김 -> 아이콘 표시)
	$('#profilePreview').hide();
	$('#profileIconPlaceholder').css('display', 'flex');

	// [추가] 변경 감지 함수 호출 (버튼 활성화)
	checkChanges();
}

// 파일 업로드 미리보기 로직
function previewImage(input) {
	if (input.files && input.files[0]) {
		// 1. 기본 캐릭터 선택 해제
		$('#defaultProfileSection .selection-box').removeClass('active');
		$('#inputDefaultProfileImage').val('');

		var reader = new FileReader();
		reader.onload = function(e) {
			// 2. 이미지 데이터 주입
			$('#profilePreview').attr('src', e.target.result);

			// 3. 미리보기 영역 교체 (아이콘 숨김 -> 이미지 표시)
			$('#profileIconPlaceholder').hide();
			$('#profilePreview').show();
		}
		reader.readAsDataURL(input.files[0]);

		// 4. 라벨 변경
		var fileName = input.files[0].name;
		$(input).next('.custom-file-label').html(fileName);

		// [추가] 변경 감지 함수 호출 (버튼 활성화)
		checkChanges();
	}
}

// --- [공통 AJAX 전송 함수] ---
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