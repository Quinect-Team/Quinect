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

	$('#saveProfileBtn').on('click', function(e) {
		e.preventDefault(); // 일단 버튼 기본 동작 차단

		const currentUsername = $("#nickname").val();
		const initialUsername = initialProfileValues.username;

		// 1. 닉네임이 변경되었는지 확인
		if (currentUsername !== initialUsername) {
			const msg = "닉네임을 바꾸시겠습니까?\n변경 후 7일 동안 다시 바꿀 수 없습니다.";

			// 2. confirm 창 띄우기
			if (confirm(msg)) {
				// [확인] 누르면 -> 저장 함수 실행 (HTML에서 뺀 거 여기서 호출)
				saveViaAjax('settingsForm', '/profile/settings/profilesave');
			} else {
				// [취소] 누르면 -> 아무 일도 안 일어남 (함수 호출 안 함)
				return false;
			}
		}
		// 3. 닉네임은 그대로고, 다른 정보만 바뀐 경우
		else {
			// 묻지 않고 바로 저장
			saveViaAjax('settingsForm', '/profile/settings/profilesave');
		}
	});
});

// ==========================================
// [A] 프로필 정보 탭 로직
// ==========================================

function checkProfileChanges() {
	// 1. Selector 수정: name 대신 ID 사용 (hidden input과의 충돌 방지)
	const nicknameInput = $("#nickname");
	const nickname = nicknameInput.val();
	const msgBox = $("#nicknameCheckMsg");

	// ⭐ [핵심 수정] 변수 선언을 가장 위로 올리고 기본값 true 설정
	let isNicknameValid = true;
	let length = 0;

	// 2. 닉네임 입력창이 '비활성화(disabled)' 상태인지 체크
	// (7일 제한 등으로 잠겨있으면 유효성 검사 패스)
	if (nicknameInput.is(':disabled')) {
		isNicknameValid = true;
	} else {
		// --- 활성화 상태일 때만 유효성 검사 수행 ---

		// 2-1. 길이 계산 (한글 2, 영문 1)
		for (let i = 0; i < nickname.length; i++) {
			const c = nickname.charCodeAt(i);
			if (c >= 0xAC00 && c <= 0xD7A3) length += 2;
			else length += 1;
		}

		// 2-2. 유효성 판단
		if (!nickname || nickname.trim() === "") {
			isNicknameValid = false;
			nicknameInput.addClass("is-invalid");
			msgBox.text("닉네임을 입력해주세요.").css("color", "red");
		} else if (length > 16.5) {
			isNicknameValid = false;
			nicknameInput.addClass("is-invalid");
			msgBox.text("닉네임이 너무 깁니다! (" + length + "/16.5)").css("color", "red");
		} else {
			// ⭐ [핵심] 통과했을 때 명시적으로 true 설정 및 UI 초기화
			isNicknameValid = true;
			nicknameInput.removeClass("is-invalid");
			msgBox.text("");
		}
	}

	// --- [3] 변경 사항 확인 ---
	// Selector 수정: name 대신 ID 사용 권장 (HTML에 id="organization", id="bio"가 있다고 가정)
	// 만약 ID가 없다면 기존처럼 $('input[name="organization"]') 사용하되, hidden input이 없는지 주의하세요.
	const currentOrg = $('input[name="organization"]').val();
	const currentBio = $('input[name="bio"]').val();

	// 텍스트 변경 여부
	const isTextChanged = (nickname !== initialProfileValues.username) ||
		(currentOrg !== initialProfileValues.organization) ||
		(currentBio !== initialProfileValues.bio);

	// 이미지 변경 여부
	let isImageChanged = false;
	const fileInput = $('#profileImageFile')[0];
	const isFileUploaded = fileInput && fileInput.files.length > 0;
	const selectedDefault = $('#inputDefaultProfileImage').val();

	if (isFileUploaded) {
		isImageChanged = true;
	} else if (selectedDefault !== '') {
		// 초기값과 다르면 변경된 것
		if (selectedDefault !== initialProfileValues.image) {
			isImageChanged = true;
		}
	}

	const isChanged = isTextChanged || isImageChanged;

	// --- [4] 최종 버튼 상태 제어 ---
	// 변경사항이 있고(AND) 닉네임도 유효해야 저장 버튼 활성화
	toggleButton('#saveProfileBtn', isChanged && isNicknameValid);
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