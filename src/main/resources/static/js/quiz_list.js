const btnHot = document.getElementById('btnHot');
const btnNew = document.getElementById('btnNew');

// 인기순 클릭
btnHot.addEventListener('click', function() {
	btnHot.classList.remove('btn-outline-secondary');
	btnHot.classList.add('btn-secondary');

	btnNew.classList.remove('btn-secondary');
	btnNew.classList.add('btn-outline-secondary');
});

// 최신순 클릭
btnNew.addEventListener('click', function() {
	btnNew.classList.remove('btn-outline-secondary');
	btnNew.classList.add('btn-secondary');

	btnHot.classList.remove('btn-secondary');
	btnHot.classList.add('btn-outline-secondary');
});