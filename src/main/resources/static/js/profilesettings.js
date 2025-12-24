// 초기값을 저장할 전역 변수들
let initialProfileValues = {}; // 프로필 정보용
let initialThemeValues = {};   // 테마/테두리용

$(document).ready(function() {
	// --- [1. 테마/테두리 탭 초기화] ---
	if ($('#borderSection').length > 0) checkDefaultActive('#borderSection');
	if ($('#themeSection').length > 0) checkDefaultActive('#themeSection');

	// (중요) 화면의 Active 상태를 보고 hidden input에 값을 먼저 채워넣음
	initCurrentValues();

	// [추가] 테마 탭 초기값 저장 (initCurrentValues 실행 직후에 해야 함)
	initialThemeValues = {
		borderId: $('#inputBorderId').val(),
		themeId: $('#inputThemeId').val()
	};

	// --- [2. 프로필 정보 탭 초기화] ---
	initialProfileValues = {
		username: $('input[name="username"]').val(),
		organization: $('input[name="organization"]').val(),
		bio: $('input[name="bio"]').val(),
		image: $('#initialProfileImageValue').val()
	};

	// 텍스트 입력 감지
	$('input[name="username"], input[name="organization"], input[name="bio"]').on('keyup change', function() {
		checkProfileChanges();
	});
});

// ==========================================
// [A] 프로필 정보 탭 로직
// ==========================================

function checkProfileChanges() {
	// 1. 텍스트 변경 확인
	const currentUsername = $('input[name="username"]').val();
	const currentOrg = $('input[name="organization"]').val();
	const currentBio = $('input[name="bio"]').val();

	const isTextChanged = (currentUsername !== initialProfileValues.username) ||
		(currentOrg !== initialProfileValues.organization) ||
		(currentBio !== initialProfileValues.bio);

	// 2. 이미지 변경 확인
	let isImageChanged = false;
	const fileInput = $('#profileImageFile')[0];
	const isFileUploaded = fileInput && fileInput.files.length > 0;
	const selectedDefault = $('#inputDefaultProfileImage').val();

	if (isFileUploaded) {
		isImageChanged = true;
	} else if (selectedDefault !== '') {
		if (selectedDefault !== initialProfileValues.image) {
			isImageChanged = true;
		}
	}

	// 3. 버튼 상태 제어 (saveProfileBtn)
	toggleButton('#saveProfileBtn', isTextChanged || isImageChanged);
}

// 기본 프로필 선택
function selectDefaultProfile(path, element) {
	$('#defaultProfileSection .selection-box').removeClass('active');
	$(element).addClass('active');
	$('#inputDefaultProfileImage').val(path);
	$('#profileImageFile').val('');
	$('.custom-file-label').text('파일 선택...');
	$('#profilePreview').hide();
	$('#profileIconPlaceholder').css('display', 'flex');

	checkProfileChanges(); // 변경 감지 호출
}

// 파일 업로드 미리보기
function previewImage(input) {
	if (input.files && input.files[0]) {
		$('#defaultProfileSection .selection-box').removeClass('active');
		$('#inputDefaultProfileImage').val('');

		var reader = new FileReader();
		reader.onload = function(e) {
			$('#profilePreview').attr('src', e.target.result);
			$('#profileIconPlaceholder').hide();
			$('#profilePreview').show();
		}
		reader.readAsDataURL(input.files[0]);
		var fileName = input.files[0].name;
		$(input).next('.custom-file-label').html(fileName);

		checkProfileChanges(); // 변경 감지 호출
	}
}


// ==========================================
// [B] 테마/테두리 탭 로직
// ==========================================

// [추가] 테마 변경 감지 함수
function checkThemeChanges() {
	const currentBorderId = $('#inputBorderId').val();
	const currentThemeId = $('#inputThemeId').val();

	// 초기값과 비교
	const isBorderChanged = currentBorderId !== initialThemeValues.borderId;
	const isThemeChanged = currentThemeId !== initialThemeValues.themeId;

	// 버튼 상태 제어 (saveThemeBtn)
	toggleButton('#saveThemeBtn', isBorderChanged || isThemeChanged);
}

// 아이템 클릭 (테마/테두리)
function selectItem(type, id, element) {
	var $row = $(element).closest('.row');
	$row.find('.selection-box').removeClass('active');
	$(element).addClass('active');

	if (type === 'border') {
		$('#inputBorderId').val(id);
	} else if (type === 'theme') {
		$('#inputThemeId').val(id);
	}

	// [추가] 변경 감지 호출
	checkThemeChanges();
}

// 테마 저장 전송
function submitEquipForm() {
	$('#realBorderId').val($('#inputBorderId').val());
	$('#realThemeId').val($('#inputThemeId').val());
	saveViaAjax('equipForm', '/profile/settings/equip');
}


// ==========================================
// [C] 공통 유틸리티 함수
// ==========================================

// 버튼 활성화/비활성화 토글 헬퍼
function toggleButton(btnId, isActive) {
	if (isActive) {
		$(btnId).prop('disabled', false);
		$(btnId).removeClass('btn-secondary').addClass('btn-success');
	} else {
		$(btnId).prop('disabled', true);
		$(btnId).removeClass('btn-success').addClass('btn-secondary');
	}
}

// 초기값 세팅 (HTML Active 클래스 -> Hidden Input)
function initCurrentValues() {
	var $activeBorder = $('#borderSection .selection-box.active');
	if ($activeBorder.length > 0) {
		$('#inputBorderId').val($activeBorder.data('id'));
	}

	var $activeTheme = $('#themeSection .selection-box.active');
	if ($activeTheme.length > 0) {
		$('#inputThemeId').val($activeTheme.data('id'));
	}
}

function checkDefaultActive(sectionId) {
	if ($(sectionId).find('.selection-box.active').length === 0) {
		$(sectionId).find('.default-box').addClass('active');
	}
}

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

// 탈퇴 모달: 체크박스 동의 시 버튼 활성화
function toggleWithdrawBtn() {
	const checkBox = document.getElementById('checkWithdrawal');
	const btn = document.getElementById('btnFinalWithdraw');
	btn.disabled = !checkBox.checked;
}

// 탈퇴 요청 전송 (AJAX 또는 Form Submit)
function submitWithdrawal() {

	var token = $("meta[name='_csrf']").attr("content");
	var header = $("meta[name='_csrf_header']").attr("content");

	if (!confirm("정말로 탈퇴 처리를 진행하시겠습니까?")) return;


	fetch('/api/user/withdraw', {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
			[header]: token // ⭐ 핵심: CSRF 토큰 헤더 추가
		}
	}).then(async res => {
		if (res.ok) {
			alert("탈퇴 처리가 완료되었습니다.\n7일 후 완전히 삭제됩니다.\n로그인 페이지로 이동합니다.");
			window.location.href = "/login?logout";
		} else {
			// 에러 메시지 읽기
			const errorText = await res.text();
			alert("처리 중 오류가 발생했습니다: " + errorText);
		}
	}).catch(err => {
		console.error(err);
		alert("서버 통신 오류가 발생했습니다.");
	});
}