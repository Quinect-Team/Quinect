$(document).ready(function() {

	$('#btnCheckIn').on('click', function() {
		var $btn = $(this);
		$btn.prop('disabled', true); // 중복 클릭 방지

		var token = $("meta[name='_csrf']").attr("content");
		var header = $("meta[name='_csrf_header']").attr("content");

		$.ajax({
			url: '/api/attendance/check',
			type: 'POST',

			beforeSend: function(xhr) {
				xhr.setRequestHeader(header, token);
			},
			
			success: function(message) {
				// 성공 시 알림 (SweetAlert2)
				Swal.fire({
					icon: 'success',
					title: '출석 완료!',
					text: message,
					confirmButtonColor: '#1cc88a'
				}).then((result) => {
					// 확인 버튼 누르면 새로고침
					location.reload();
				});
			},
			error: function(xhr) {
				// 실패 시 알림
				Swal.fire({
					icon: 'error',
					title: '오류',
					text: xhr.responseText || '출석체크에 실패했습니다.'
				});
				$btn.prop('disabled', false); // 버튼 다시 활성화
			}
		}); // 1. ajax 끝
	}); // 2. click 이벤트 끝

}); // 3. document.ready 끝