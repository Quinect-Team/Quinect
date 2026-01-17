(function() {
  const canvas = document.getElementById("myPieChart");
  if (!canvas) {
    console.warn("Chart skipped: myPieChart not found");
    return;
  }

  // 1. 데이터 가져오기
  const correctCount = window.chartData?.correctCount || 0;
  const wrongCount = window.chartData?.wrongCount || 0;
  const total = correctCount + wrongCount;

  console.log('Chart Creating - correctCount:', correctCount, 'wrongCount:', wrongCount);

  // 2. 데이터가 0일 때 '빈 도넛(회색)' 처리 로직
  let chartLabels = ["정답", "오답"];
  let chartData = [correctCount, wrongCount];
  let chartColors = ['#1cc88a', '#e74a3b']; // 초록, 빨강
  let isDataEmpty = false;

  if (total === 0) {
    isDataEmpty = true;
    chartLabels = ["기록 없음"];
    chartData = [1]; // 회색 도넛을 그리기 위한 더미 데이터
    chartColors = ['#eaecf4']; // 밝은 회색 (SB Admin 2 기본 회색)
  }

  const ctx = canvas.getContext('2d');
  new Chart(ctx, {
    type: 'doughnut',
    data: {
      labels: chartLabels,
      datasets: [{
        data: chartData,
        backgroundColor: chartColors,
        hoverBackgroundColor: chartColors,
        hoverBorderColor: "rgba(234, 236, 244, 1)",
      }],
    },
    options: {
      maintainAspectRatio: false,
      cutoutPercentage: 80, // 얇은 도넛 유지

      legend: {
        display: false
      },

      animation: {
        animateScale: false,
        animateRotate: true,
        onComplete: function() {
          var textElement = document.getElementById("centerText");
          if (textElement) {
            // ⭐ [핵심] 데이터가 없으면 문구 변경
            if (isDataEmpty) {
              // <h5> 태그 등을 써서 두 줄로 예쁘게 표시
              textElement.innerHTML = '<h5 class="m-0 font-weight-bold text-gray-500" style="font-size:0.9rem; line-height:1.4;">푼 문제가<br>없습니다</h5>';
            }
            // 서서히 나타나게 함
            textElement.style.opacity = 1;
          }
        }
      },

      tooltips: {
        enabled: !isDataEmpty, // ⭐ 데이터 없으면 툴팁 끄기
        backgroundColor: "rgb(255,255,255)",
        titleFontColor: "#858796",
        bodyFontColor: "#858796",
        borderColor: '#dddfeb',
        borderWidth: 1,
        xPadding: 15,
        yPadding: 15,
        displayColors: false,
        callbacks: {
          label: function(tooltipItem, data) {
            const dataset = data.datasets[tooltipItem.datasetIndex];
            const value = dataset.data[tooltipItem.index];
            const totalVal = dataset.data.reduce((a, b) => a + b, 0);
            const percentage = totalVal ? ((value / totalVal) * 100).toFixed(1) : 0;
            return data.labels[tooltipItem.index] + ": " + value + "개 (" + percentage + "%)";
          }
        }
      }
    }
  });
})();