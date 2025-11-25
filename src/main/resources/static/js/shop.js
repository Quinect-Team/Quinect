$(document).ready(function() {
	$('#itemModal').on('show.bs.modal', function(event) {
		var button = $(event.relatedTarget);
		var name = button.data('name');
		var price = button.data('price');
		var desc = button.data('desc');

		var modal = $(this);
		modal.find('#modalItemName').text(name);
		modal.find('#modalItemPrice').text(price + ' P');
		modal.find('#modalItemDesc').text(desc);
		modal.find('#hiddenItemName').val(name);
	});
	// [추가] 모달이 닫힐 때 잔여 패딩 제거 (안전 장치)
	$('#itemModal').on('hidden.bs.modal', function() {
		$('body').css('padding-right', '');
	});

	// 포인트 내역 모달 등 다른 모달도 동일하게 처리
	$('#pointHistoryModal').on('hidden.bs.modal', function() {
		$('body').css('padding-right', '');
	});

});

// [스크립트 2] 스크롤 시 포인트 배지 표시
$(document).ready(function() {
	// 초기화: 본문 포인트를 배지에 복사
	var pointText = $('#pagePointDisplay span').text();
	$('#floatingPointText').text(pointText);

	$(document).on('scroll', function() {
		var $target = $('#pagePointDisplay');
		// 안전장치: 요소가 존재할 때만 실행
		if ($target.length > 0) {
			var scrollDistance = $(this).scrollTop();

			// 텍스트가 탑바(약 70px)에 닿는 순간부터 표시
			var triggerPoint = $target.offset().top - 70;

			if (scrollDistance > triggerPoint) {
				$('#floatingPointBadge').fadeIn(); // jQuery fadeIn (부드럽게)
			} else {
				$('#floatingPointBadge').fadeOut();
			}
		}
	});
});