// 1. onblur 이벤트 끝나면 실행될 함수 
function setFieldStatus(selector, msgSelector, isValid, message) {
    const input = $(selector);
    const msgBox = $(msgSelector);

    if (isValid) {
        input.removeClass("is-invalid").addClass("is-valid");
        msgBox.text(message).css("color", "green");
    } else {
        input.removeClass("is-valid").addClass("is-invalid");
        msgBox.text(message).css("color", "red");
    }

    // ⭐ 상태가 변경될 때마다 항상 버튼 상태를 재확인 요청
    updateSubmitButton();
}

// 2. setFieldStatus 후 모든 항목 다 true 되었나 확인하는 함수 (유지보수하려고 넣었음)
function updateSubmitButton() {
    // 각 필드에 'is-valid' 클래스가 붙어있는지 확인
    const isEmailValid = $("#email").hasClass("is-valid");
    const isNameValid = $("#username").hasClass("is-valid");
    const isPwdValid = $("#password").hasClass("is-valid");

    const submitBtn = $("button[type='submit']");

    // ✅ 모든 조건이 통과(true)여야 버튼 활성화
    // (나중에 전화번호 등이 추가되면 여기에 && isPhoneValid 만 넣으면 끝)
    if (isEmailValid && isNameValid && isPwdValid) {
        submitBtn.prop("disabled", false);
    } else {
        submitBtn.prop("disabled", true);
    }
}

// 3. 개별 항목 검사 함수들
function checkNickname() {
    const inputId = "#username";
    const msgId = "#nameCheckMsg";
    const nickname = $(inputId).val();

    // 빈 값 체크 (초기화)
    if (!nickname || nickname.trim() === "") {
        $(inputId).removeClass("is-valid is-invalid");
        $(msgId).text("");
        updateSubmitButton();
        return;
    }

    // 길이 계산 로직
    let length = 0;
    for (let i = 0; i < nickname.length; i++) {
        const c = nickname.charCodeAt(i);
        if (c >= 0xAC00 && c <= 0xD7A3) {
            length += 2; // 한글
        } else {
            length += 1; // 그 외
        }
    }

    // 결과 처리
    if (length > 16.5) {
        setFieldStatus(inputId, msgId, false, "닉네임이 너무 깁니다!");
    } else {
        setFieldStatus(inputId, msgId, true, "사용 가능한 닉네임입니다.");
    }
}

// 이메일 형식 및 중복 검사 (AJAX)
function checkEmail() {
    const inputId = "#email";
    const msgId = "#emailCheckMsg";
    const email = $(inputId).val();

    // 빈 값 체크
    if (!email || email.trim() === "") {
        $(inputId).removeClass("is-valid is-invalid");
        $(msgId).text("");
        updateSubmitButton();
        return;
    }

    // 정규식 검사
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        setFieldStatus(inputId, msgId, false, "올바른 이메일 형식이 아닙니다.");
        return;
    }

    // 서버 중복 체크
    $.ajax({
        url: '/api/user/check-email',
        type: 'GET',
        data: { email: email },
        success: function(exists) {
            if (exists) {
                setFieldStatus(inputId, msgId, false, "이미 가입된 이메일입니다.");
            } else {
                setFieldStatus(inputId, msgId, true, "사용 가능한 이메일입니다.");
            }
        },
        error: function() {
            console.error("이메일 체크 중 오류 발생");
        }
    });
}

/**
 * 비밀번호 유효성 검사 (단순 길이 체크 예시)
 */
function checkPassword() {
    const inputId = "#password";
    const msgId = "#pwdCheckMsg"; // HTML에 span 태그 추가 필요
    const password = $(inputId).val();

    // 빈 값 체크
    if (!password || password.trim() === "") {
        $(inputId).removeClass("is-valid is-invalid");
        $(msgId).text("");
        updateSubmitButton();
        return;
    }

    // 예시: 4글자 이상이어야 통과
    if (password.length < 4) {
        setFieldStatus(inputId, msgId, false, "비밀번호는 4자 이상 입력해주세요.");
    } else {
        setFieldStatus(inputId, msgId, true, ""); // 메시지 없이 초록색 테두리만
    }
}