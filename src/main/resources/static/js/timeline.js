let currentPage = 0;
const pageSize = 10;
let isLastPage = false;
let isLoading = false;
let targetUserId = null;

// 페이지 로드 시 첫 번째 페이지 가져오기
$(document).ready(function() {
	const val = $('#hiddenTargetUserId').val();
	if (val && val !== '') {
		targetUserId = Number(val);
	}

	console.log("📌 읽어온 ID:", targetUserId); // 확인용 로그

	// 3. 그 다음 데이터 로드 시작
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
	$('#loadMoreBtnContainer').hide();

	// 1. 보낼 데이터를 객체로 정의
	let requestData = {
		page: page,
		size: pageSize
	};

	// 2. targetUserId가 존재하면(타인 프로필) 데이터에 추가
	if (targetUserId) {
		requestData.userId = targetUserId;
	}

	$.ajax({
		url: '/api/timeline',
		type: 'GET',
		data: requestData, // ⭐ 수정됨: userId가 포함된 객체를 전송
		success: function(data) {
			// ... (기존 성공 로직 그대로) ...
			if (data.length === 0) {
				isLastPage = true;
				if (page === 0) {
					$('#timelineList').html('<div class="text-center text-gray-500 my-5">활동 기록이 없습니다.</div>');
				} else {
					$('#loadMoreBtnContainer').html('<span class="small text-gray-500">모든 기록을 불러왔습니다.</span>').show();
				}
			} else {
				renderTimelineItems(data);
				if (data.length < pageSize) {
					isLastPage = true;
					$('#loadMoreBtnContainer').html('<span class="small text-gray-500">모든 기록을 불러왔습니다.</span>').show();
				} else {
					$('#loadMoreBtnContainer').show();
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

// (선택사항) 스크롤이 바닥에 닿으면 자동 로딩하려면 아래 주석 해제
/*
$(window).scroll(function() {
	if ($(window).scrollTop() + $(window).height() > $(document).height() - 100) {
		if(!isLastPage && !isLoading) {
			loadNextPage();
		}
	}
});
*/