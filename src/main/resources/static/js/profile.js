$(document).ready(function() {

	// 1. 현재 페이지(프로필)를 히스토리에 한 번 더 쌓음 (방어막)
	history.pushState(null, null, location.href);

	// 2. 뒤로가기 버튼(마우스/키보드) 감지
	window.onpopstate = function(event) {
		// 3. 어디서 왔든 묻지도 따지지도 않고 '메인'으로 보냄
		window.location.href = '/main';
	};
});