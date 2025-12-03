$(document).ready(function() {

    // [기능 1] 아이템 모달이 열릴 때 실행되는 로직
    $('#itemModal').on('show.bs.modal', function(event) {
        var button = $(event.relatedTarget);

        var id = button.data('id');
        var name = button.data('name');
        var price = Number(button.data('price'));
        var desc = button.data('desc');
        var isOwned = button.data('owned');
        var imgUrl = button.data('img');
        var category = button.data('category');

        var modal = $(this);

        modal.find('#modalItemName').text(name);
        modal.find('#modalItemDesc').text(desc);

        // [이미지 렌더링 로직]
        var $imgContainer = modal.find('.modal-item-img');
        $imgContainer.empty();

        if (imgUrl) {
            var htmlContent = '';

            if (category === 'THEME') {
                // [수정] fill -> contain 으로 변경
                // 비율을 유지하면서 박스 안에 '포함(contain)'되게 렌더링합니다.
                // 이미지가 찌그러지지 않고, 남는 공간은 회색 배경으로 깔끔하게 처리됩니다.
                htmlContent = `<img src="${imgUrl}" style="width: 100%; height: 100%; object-fit: contain;">`;
                
            } else if (category === 'BORDER') {
                // 테두리는 기존대로 원형 유지
                htmlContent = `
                    <div style="position: relative; width: 150px; height: 150px; border-radius: 50%; background-color: #e9ecef; display: flex; align-items: center; justify-content: center;">
                        <img src="${imgUrl}" style="position: absolute; width: 135%; height: 135%; object-fit: contain;">
                    </div>
                `;
            } else {
                htmlContent = `<img src="${imgUrl}" style="max-width: 90%; max-height: 90%;">`;
            }

            $imgContainer.html(htmlContent);

        } else {
            $imgContainer.html('<i class="fas fa-gift fa-4x text-gray-300"></i>');
        }


        // [구매/보유 버튼 로직] (기존 유지)
        if (isOwned === true) {
            modal.find('#purchaseView').hide();
            modal.find('#ownedView').show();
        } else {
            modal.find('#ownedView').hide();
            modal.find('#purchaseView').show();

            var formattedPrice = price.toLocaleString();
            modal.find('#modalItemPrice').text(formattedPrice + ' P');
            modal.find('#hiddenItemId').val(id);

            var userPointText = modal.find('#modalUserPoint').text();
            var userPoint = parseInt(userPointText.replace(/[^0-9]/g, ''));
            if (isNaN(userPoint)) userPoint = 0;

            console.log("내 포인트:", userPoint, " / 아이템 가격:", price);

            var warningText = modal.find('#pointWarning');
            var buyButton = modal.find('#btnBuy');

            if (userPoint < price) {
                buyButton.prop('disabled', true);
                buyButton.removeClass('btn-success').addClass('btn-secondary');
                buyButton.text('포인트 부족');
                warningText.show();
            } else {
                buyButton.prop('disabled', false);
                buyButton.removeClass('btn-secondary').addClass('btn-success');
                buyButton.text('구매');
                warningText.hide();
            }
        }
    });

    // [기능 2] 포인트 내역 모달
    $('#pointHistoryModal').on('show.bs.modal', function() {
        var $tbody = $(this).find('tbody');
        $tbody.html('<tr><td colspan="3" class="text-center">로딩 중...</td></tr>');

        $.ajax({
            url: '/api/point/history',
            type: 'GET',
            success: function(data) {
                $tbody.empty();
                if (data.length === 0) {
                    $tbody.html('<tr><td colspan="3" class="text-center py-3">내역이 없습니다.</td></tr>');
                    return;
                }
                data.forEach(function(history) {
                    var colorClass = (history.amount > 0) ? 'text-success' : 'text-danger';
                    var sign = (history.amount > 0) ? '+' : '';
                    var row = `
                        <tr>
                            <td class="small align-middle">${history.date}</td>
                            <td class="align-middle"><div class="font-weight-bold">${history.reason}</div></td>
                            <td class="text-right align-middle ${colorClass} font-weight-bold">
                                ${sign} ${history.amount.toLocaleString()}
                            </td>
                        </tr>`;
                    $tbody.append(row);
                });
            },
            error: function() {
                $tbody.html('<tr><td colspan="3" class="text-center text-danger">불러오기 실패</td></tr>');
            }
        });
    });

    // [기능 3] 공통 UI 보정
    $('#itemModal').on('hidden.bs.modal', function() { $('body').css('padding-right', ''); });
    $('#pointHistoryModal').on('hidden.bs.modal', function() { $('body').css('padding-right', ''); });

    // [기능 4] 플로팅 배지
    var pointText = $('#pagePointDisplay span').text();
    $('#floatingPointText').text(pointText);

    $(document).on('scroll', function() {
        var $target = $('#pagePointDisplay');
        if ($target.length > 0) {
            var scrollDistance = $(this).scrollTop();
            var triggerPoint = $target.offset().top - 70;
            if (scrollDistance > triggerPoint) {
                $('#floatingPointBadge').fadeIn();
            } else {
                $('#floatingPointBadge').fadeOut();
            }
        }
    });
});