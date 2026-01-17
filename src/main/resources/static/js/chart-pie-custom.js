(function() {
	const canvas = document.getElementById("myPieChart");
	if (!canvas) {
		console.warn("Chart skipped: myPieChart not found");
		return;
	}

	const correctCount = window.chartData?.correctCount || 0;
	const wrongCount = window.chartData?.wrongCount || 0;

	console.log('Chart Creating - correctCount:', correctCount, 'wrongCount:', wrongCount);

	const ctx = canvas.getContext('2d');
	new Chart(ctx, {
		type: 'doughnut',
		data: {
			labels: ["정답", "오답"],
			datasets: [{
				data: [correctCount, wrongCount],
				backgroundColor: ['#1cc88a', '#e74a3b'],
				hoverBackgroundColor: ['#1cc88a', '#e74a3b'],
				hoverBorderColor: "rgba(234, 236, 244, 1)",
			}],
		},
		options: {
			maintainAspectRatio: false,

			legend: {
				display: false
			},

			tooltips: {   // ⚠️ tooltip ❌  tooltips ⭕
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

						const total = dataset.data.reduce((a, b) => a + b, 0);
						const percentage = total ? ((value / total) * 100).toFixed(1) : 0;

						return data.labels[tooltipItem.index] + ": " + value + "개 (" + percentage + "%)";
					}
				}
			}
		}
		,
	},
	);
})();
