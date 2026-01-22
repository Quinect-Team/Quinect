let currentPage = 0;
const pageSize = 10;
let isLastPage = false;
let isLoading = false;

// ⭐ [추가] 컨트롤러(ProfileController)에서 Model로 넘겨준 targetEmail 값을 가져옴
// (타임리프 인라인 문법 사용, 없으면 빈 문자열)
const targetEmail = "[[${targetEmail}]]"; 

// 페이지 로드 시 첫 번째 페이지 가져오기
$(document).ready(function() {
	loadTimelineData(currentPage);
});

// '더 불러오기' 버튼 클릭 시
function loadNextPage() {
	if (!isLastPage && !isLoading) {
		currentPage++;
		loadTimelineData(currentPage);
	}
}

// API 호출 및 HTML 렌더링
function loadTimelineData(page) {
	isLoading = true;
	$('#loadingSpinner').show();
	$('#loadMoreBtnContainer').hide(); // 로딩 중엔 버튼 숨김

	$.ajax({
		url: '/api/timeline',
		type: 'GET',
		// ⭐ [수정] targetEmail 파라미터 추가
		data: { 
            page: page, 
            size: pageSize,
            targetEmail: targetEmail // 서버 API로 전달
        },
		success: function(data) {
			if (data.length === 0) {
				isLastPage = true;
				if (page === 0) {
					$('#timelineList').html('<div class="text-center text-gray-500 my-5">활동 기록이 없습니다.</div>');
				} else {
					// 더 이상 데이터가 없음
					$('#loadMoreBtnContainer').html('<span class="small text-gray-500">모든 기록을 불러왔습니다.</span>').show();
				}
			} else {
				renderTimelineItems(data);

				// 데이터가 pageSize보다 적게 왔다면 그게 마지막 페이지임
				if (data.length < pageSize) {
					isLastPage = true;
					$('#loadMoreBtnContainer').html('<span class="small text-gray-500">모든 기록을 불러왔습니다.</span>').show();
				} else {
					$('#loadMoreBtnContainer').show(); // 버튼 다시 표시
				}
			}
		},
		error: function() {
			alert('데이터를 불러오는 데 실패했습니다.');
		},
		complete: function() {
			isLoading = false;
			$('#loadingSpinner').hide();
		}
	});
}

// HTML 조립 함수
function renderTimelineItems(items) {
	let html = '';
	items.forEach(function(item) {
		// HTML 문자열 조립 (ES6 Template Literal 사용)
		// item.colorClass, item.iconClass 등 DTO 필드 사용
		html += `
                    <div class="timeline-item">
                        <div class="timeline-icon ${item.colorClass} shadow-sm">
                            <i class="fas ${item.iconClass}"></i>
                        </div>
                        <div class="timeline-content">
                            <div class="timeline-date">${item.date}</div>
                            <p class="timeline-text">${item.description}</p>
                        </div>
                    </div>
                `;
	});
	$('#timelineList').append(html);
}