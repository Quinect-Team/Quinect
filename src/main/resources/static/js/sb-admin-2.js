(function($) {
  "use strict"; // Start of use strict
  
  // 모달 열기 함수 (jQuery 버전)
  function openSidebarModal() {
    $('body').css('overflow', 'hidden'); // [translate:배경 스크롤 막기]
    $('#sidebarModalBackdrop').css('display', 'block');
    $('#sidebarModal').css('display', 'block');
    setTimeout(function() {
      $('#sidebarModal').addClass('show');
    }, 10);
  }

  // 모달 닫기 함수 (jQuery 버전)
  function closeSidebarModal() {
    $('body').css('overflow', 'auto'); // [translate:배경 스크롤 허용]
    $('#sidebarModal').removeClass('show');
    setTimeout(function() {
      $('#sidebarModal').css('display', 'none');
      $('#sidebarModalBackdrop').css('display', 'none');
    }, 300);
  }

  // 페이지 로드 시 초기화 및 이벤트 바인딩
  $(document).ready(function() {
    $('#sidebarToggle').on('click', openSidebarModal);
    $('#sidebarModalBackdrop').on('click', closeSidebarModal);

    // [translate:페이지 로드 시 모달 상태 초기화]
    $('#sidebarModal').removeClass('show').css('display', 'none');
    $('#sidebarModalBackdrop').css('display', 'none');
    $('body').css('overflow', 'auto'); // [translate:스크롤 가능 상태로 복구]
  });

  // pageshow 이벤트 캐시 복원 시 모달 초기화
  $(window).on('pageshow', function(event) {
    if (event.originalEvent.persisted) { // [translate:페이지가 캐시에서 복원된 경우]
      $('#sidebarModal').removeClass('show').css('display', 'none');
      $('#sidebarModalBackdrop').css('display', 'none');
      $('body').css('overflow', 'auto'); // [translate:배경 스크롤 허용]
    }
  });

  // Toggle the side navigation
//   $("#sidebarToggle").on("click", function(e) {
//     var $sidebar = $("#accordionSidebar");
//     if ($sidebar.hasClass("d-none")) {
//         $sidebar.removeClass("d-none"); // 보이게
//     } else {
//         $sidebar.addClass("d-none");    // 숨기기
//     }
// });

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
