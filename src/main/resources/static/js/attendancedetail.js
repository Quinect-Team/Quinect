document.addEventListener('DOMContentLoaded', function() {
	var calendarEl = document.getElementById('calendar');

	var calendar = new FullCalendar.Calendar(calendarEl, {
		// 1. 기본 설정
		initialView: 'dayGridMonth', // 월간 달력
		locale: 'ko',               // 한국어 설정
		height: 'auto',             // 높이 자동 조절

		// 2. 상단 툴바 설정
		headerToolbar: {
			left: 'prev,next today',
			center: 'title',
			right: '' // 오른쪽 비움 (월간만 볼거니까)
		},

		// 3. 이벤트(데이터) 소스 연결 (AJAX 자동 처리)
		events: '/api/attendance/events',

		// 4. 이벤트 표시 모양 커스텀
		eventContent: function(arg) {
			return {
				html: `
		                    <div class='text-center'>
		                        <i class="fas fa-check-circle check-icon"></i>
		                        <span style="font-size:0.75rem; color:#1cc88a; font-weight:bold; margin-left:3px;">
		                            완료
		                        </span>
		                    </div>
		                `
			};
		}
	});

	calendar.render();
});