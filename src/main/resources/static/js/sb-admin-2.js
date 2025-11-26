(function($) {
	"use strict"; // Start of use strict

	function getScrollbarWidth() {
		return window.innerWidth - document.documentElement.clientWidth;
	}

	// 2. 모달 열기 함수
	function openSidebarModal() {
		// 친구 모달이 열려있으면 먼저 닫기
		const friendModal = document.querySelector('.modalPOP');
		if (friendModal && friendModal.style.display === 'block') {
			friendModal.style.display = 'none';
		}

		// 스크롤바 너비 구하기
		var scrollbarWidth = getScrollbarWidth();

		// body에 스크롤바 너비만큼 패딩을 줘서 밀림 방지 + 스크롤 막기
		$('body').css({
			'overflow': 'hidden',
			'padding-right': scrollbarWidth + 'px'
		});

		// 기존 로직 유지 (슬라이드 효과)
		$('#sidebarModalBackdrop').css('display', 'block');
		$('#sidebarModal').css('display', 'block');

		setTimeout(function() {
			$('#sidebarModal').addClass('show');
		}, 10);
	}

	// 3. 모달 닫기 함수
	function closeSidebarModal() {
		// 먼저 슬라이드 들어가게 하기 (클래스 제거)
		$('#sidebarModal').removeClass('show');

		// 애니메이션 시간(300ms) 뒤에 뒷정리
		setTimeout(function() {
			$('#sidebarModal').css('display', 'none');
			$('#sidebarModalBackdrop').css('display', 'none');
			// 스크롤바 및 패딩 원상복구
			$('body').css({
				'overflow': 'auto',
				'padding-right': '0px'
			});


		}, 300);
	}

	// 전역으로 노출 (친구 모달에서 접근 가능하도록)
	window.closeSidebarModal = closeSidebarModal;

	// 페이지 로드 시 초기화 및 이벤트 바인딩
	$(document).ready(function() {
		$('#sidebarToggle').on('click', openSidebarModal);
		$('#sidebarModalBackdrop').on('click', closeSidebarModal);

		// 페이지 로드 시 모달 상태 초기화
		$('#sidebarModal').removeClass('show').css('display', 'none');
		$('#sidebarModalBackdrop').css('display', 'none');
		$('body').css('overflow', 'auto');
	});

	// pageshow 이벤트 캐시 복원 시 모달 초기화
	$(window).on('pageshow', function(event) {
		if (event.originalEvent.persisted) {
			$('#sidebarModal').removeClass('show').css('display', 'none');
			$('#sidebarModalBackdrop').css('display', 'none');
			$('body').css('overflow', 'auto');
		}
	});

	// Close any open menu accordions when window is resized below 768px
	$(window).resize(function() {
		if ($(window).width() < 768) {
			$('.sidebar .collapse').collapse('hide');
		};

		// Toggle the side navigation when window is resized below 480px
		if ($(window).width() < 480 && !$(".sidebar").hasClass("toggled")) {
			$("body").addClass("sidebar-toggled");
			$(".sidebar").addClass("toggled");
			$('.sidebar .collapse').collapse('hide');
		};
	});

	// Prevent the content wrapper from scrolling when the fixed side navigation hovered over
	$('body.fixed-nav .sidebar').on('mousewheel DOMMouseScroll wheel', function(e) {
		if ($(window).width() > 768) {
			var e0 = e.originalEvent,
				delta = e0.wheelDelta || -e0.detail;
			this.scrollTop += (delta < 0 ? 1 : -1) * 30;
			e.preventDefault();
		}
	});

	// Scroll to top button appear
	$(document).on('scroll', function() {
		var scrollDistance = $(this).scrollTop();
		if (scrollDistance > 100) {
			$('.scroll-to-top').fadeIn();
		} else {
			$('.scroll-to-top').fadeOut();
		}
	});

	// Smooth scrolling using jQuery easing
	$(document).on('click', 'a.scroll-to-top', function(e) {
		var $anchor = $(this);
		$('html, body').stop().animate({
			scrollTop: ($($anchor.attr('href')).offset().top)
		}, 1000, 'easeInOutExpo');
		e.preventDefault();
	});

})(jQuery); // End of use strict
