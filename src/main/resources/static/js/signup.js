function checkEmail() {
	const email = $("#email").val();
	const msgBox = $("#emailCheckMsg");
	const submitBtn = $("button[type='submit']"); // 가입 버튼

	// 1. 빈 값 체크
	if (email.trim() === "") {
		msgBox.text("");
		return;
	}

	// 2. AJAX로 서버에 물어보기
	$.ajax({
		url: '/api/user/check-email',
		type: 'GET',
		data: { email: email },
		success: function(exists) {
			if (exists) {
				// 중복이면 빨간 글씨 & 버튼 비활성화
				msgBox.text("이미 가입된 이메일입니다.").css("color", "red");
				$("#email").addClass("is-invalid").removeClass("is-valid");
				submitBtn.prop("disabled", true);
			} else {
				// 통과면 초록 글씨 & 버튼 활성화
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

$("#password").on("focus", function() {
	$("#emailCheckMsg").text(""); // 메시지 글자 지우기
});

// [추가] 2. 이메일을 다시 수정하려고 누르거나 타이핑하면 메시지 & 색깔 초기화
$("#email").on("input click", function() {
	$("#emailCheckMsg").text("");                // 메시지 지우기
	$(this).removeClass("is-valid is-invalid");  // 초록/빨간 테두리 없애기
	$("button[type='submit']").prop("disabled", false); // 가입 버튼 다시 활성화
});