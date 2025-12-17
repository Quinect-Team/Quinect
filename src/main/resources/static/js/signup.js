function checkEmail() {
    const email = $("#email").val();
    const msgBox = $("#emailCheckMsg");
    const submitBtn = $("button[type='submit']");

    // 1. 빈 값 체크
    if (email.trim() === "") {
        msgBox.text("");
        return;
    }

    // ▼▼▼ [추가] 이메일 형식 체크 (정규식) ▼▼▼
    // 설명: (문자) @ (문자) . (문자) 형식인지 검사
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email)) {
        // 형식이 틀린 경우
        msgBox.text("올바른 이메일 형식이 아닙니다.").css("color", "red");
        $("#email").addClass("is-invalid").removeClass("is-valid");
        submitBtn.prop("disabled", true);
        return; // ⭐ 형식이 틀리면 서버에 중복체크를 보낼 필요 없이 여기서 종료
    }
    // ▲▲▲ 추가 끝 ▲▲▲

    // 2. AJAX로 서버에 물어보기 (형식이 맞을 때만 실행됨)
    $.ajax({
        url: '/api/user/check-email',
        type: 'GET',
        data: { email: email },
        success: function(exists) {
            if (exists) {
                msgBox.text("이미 가입된 이메일입니다.").css("color", "red");
                $("#email").addClass("is-invalid").removeClass("is-valid");
                submitBtn.prop("disabled", true);
            } else {
                msgBox.text("사용 가능한 이메일입니다.").css("color", "green");
                $("#email").addClass("is-valid").removeClass("is-invalid");
                submitBtn.prop("disabled", false);
            }
        },
        error: function() {
            console.log("체크 에러");
        }
    });
}