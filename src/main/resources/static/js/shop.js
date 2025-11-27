$(document).ready(function() {

	// [기능 1] 아이템 모달이 열릴 때 실행되는 로직
	$('#itemModal').on('show.bs.modal', function(event) {
		var button = $(event.relatedTarget);

		// 1. 데이터 가져오기
		var id = button.data('id');
		var name = button.data('name');
		// 가격을 확실하게 숫자로 변환
		var price = Number(button.data('price'));
		var desc = button.data('desc');
		var isOwned = button.data('owned');

		var modal = $(this);

		// 2. 공통 정보 세팅
		modal.find('#modalItemName').text(name);
		modal.find('#modalItemDesc').text(desc);

		// 3. 화면 스위칭 로직
		if (isOwned === true) {
			modal.find('#purchaseView').hide();
			modal.find('#ownedView').show();
		} else {
			modal.find('#ownedView').hide();
			modal.find('#purchaseView').show();

			// 3-1. 가격 포맷팅
			var formattedPrice = price.toLocaleString();
			modal.find('#modalItemPrice').text(formattedPrice + ' P');

			// 3-2. ID 주입
			modal.find('#hiddenItemId').val(id);

			// 3-3. [핵심] 포인트 부족 체크 로직 (디버깅 추가)

			// (1) HTML에서 텍스트 가져오기
			var userPointText = modal.find('#modalUserPoint').text();

			// (2) 숫자만 추출 (예: "3,000 P" -> 3000)
			var userPoint = parseInt(userPointText.replace(/[^0-9]/g, ''));

			// 값이 없거나(NaN) 읽지 못했으면 0으로 처리
			if (isNaN(userPoint)) userPoint = 0;

			// ★★★ [디버깅 로그] F12 콘솔에서 이 값이 제대로 나오는지 확인하세요 ★★★
			console.log("내 포인트(텍스트):", userPointText);
			console.log("내 포인트(숫자):", userPoint);
			console.log("아이템 가격:", price);

			// (3) UI 요소 가져오기
			var warningText = modal.find('#pointWarning');
			var buyButton = modal.find('#btnBuy');

			// (4) 비교 및 버튼 제어
			if (userPoint < price) {
				// 잔액 부족
				console.log("결과: 잔액 부족 (구매 불가)");
				buyButton.prop('disabled', true);
				buyButton.removeClass('btn-success').addClass('btn-secondary');
				buyButton.text('포인트 부족');
				warningText.show();
			} else {
				// 구매 가능
				console.log("결과: 구매 가능");
				buyButton.prop('disabled', false);
				buyButton.removeClass('btn-secondary').addClass('btn-success');
				buyButton.text('구매');
				warningText.hide();
			}
		}
	});

	$('#pointHistoryModal').on('show.bs.modal', function() {
		var $tbody = $(this).find('tbody');
		$tbody.html('<tr><td colspan="3" class="text-center">로딩 중...</td></tr>'); // 로딩 표시

		$.ajax({
			url: '/api/point/history',
			type: 'GET',
			success: function(data) {
				$tbody.empty(); // 비우기

				if (data.length === 0) {
					$tbody.html('<tr><td colspan="3" class="text-center py-3">내역이 없습니다.</td></tr>');
					return;
				}

				// 데이터 반복문으로 행(tr) 추가
				data.forEach(function(history) {
					var colorClass = (history.amount > 0) ? 'text-success' : 'text-danger';
					var sign = (history.amount > 0) ? '+' : ''; // 양수면 + 붙이기

					var row = `
	                        <tr>
	                            <td class="small align-middle">${history.date}</td>
	                            <td class="align-middle">
	                                <div class="font-weight-bold">${history.reason}</div>
	                            </td>
	                            <td class="text-right align-middle ${colorClass} font-weight-bold">
	                                ${sign} ${history.amount.toLocaleString()}
	                            </td>
	                        </tr>
	                    `;
					$tbody.append(row);
				});
			},
			error: function() {
				$tbody.html('<tr><td colspan="3" class="text-center text-danger">불러오기 실패</td></tr>');
			}
		});
	});

	// (나머지 모달 닫힘 처리 및 스크롤 배지 코드는 그대로 유지)
	$('#itemModal').on('hidden.bs.modal', function() {
		$('body').css('padding-right', '');
	});
	$('#pointHistoryModal').on('hidden.bs.modal', function() {
		$('body').css('padding-right', '');
	});
});

// [기능 3] 스크롤 시 우측 하단 포인트 배지 표시 (기존 코드 유지)
$(document).ready(function() {
	// 초기화: 상단 바의 포인트를 플로팅 배지에 복사
	var pointText = $('#pagePointDisplay span').text();
	$('#floatingPointText').text(pointText);

	$(document).on('scroll', function() {
		var $target = $('#pagePointDisplay');
		// 안전장치: 요소가 존재할 때만 실행
		if ($target.length > 0) {
			var scrollDistance = $(this).scrollTop();
			// 포인트 텍스트가 화면 상단(약 70px)에 닿아 사라질 때쯤 배지 등장
			var triggerPoint = $target.offset().top - 70;

			if (scrollDistance > triggerPoint) {
				$('#floatingPointBadge').fadeIn(); // 부드럽게 등장
			} else {
				$('#floatingPointBadge').fadeOut(); // 부드럽게 퇴장
			}
		}
	});
});