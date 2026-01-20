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


// 모든 정렬 버튼에 클릭 이벤트 등록
document.querySelectorAll('.sort-btn').forEach(button => {
    button.addEventListener('click', function() {
        const sortType = this.value; // 'popular' 또는 'latest'
        
        // 1. 버튼 디자인 변경 (선택된 버튼 강조)
        document.querySelectorAll('.sort-btn').forEach(btn => {
            btn.classList.replace('btn-secondary', 'btn-outline-secondary');
        });
        this.classList.replace('btn-outline-secondary', 'btn-secondary');

        // 2. API 호출 (파라미터 전달)
        loadQuizzes(sortType);
    });
});

// 퀴즈 목록을 가져오는 함수 예시
function loadQuizzes(sortType = 'latest') {
    // '/api'를 빼거나, 서버의 실제 Controller 경로와 똑같이 맞추세요.
    const url = `/quiz/list?sort=${sortType}`; 
    console.log("요청 URL:", url); // 콘솔에서 실제 주소를 확인해보세요.
    
    fetch(url)
        .then(res => {
            if (!res.ok) throw new Error('Network response was not ok');
            return res.json();
        })
        .then(data => {
            // renderQuizList(data); // 이 부분이 에러를 발생시킴
            displayQuizzes(data); // 기존에 사용하던 함수 이름으로 바꾸세요.
        })
        .catch(err => console.error("데이터 로딩 실패:", err));
}

document.addEventListener('DOMContentLoaded', function() {
    // 1. 초기 로딩 (최신순)
    loadQuizzes('latest');

    // 2. 버튼 클릭 이벤트 통합 관리
    document.querySelectorAll('.sort-btn').forEach(button => {
        button.addEventListener('click', function() {
            const sortType = this.value; // 'popular' 또는 'latest'
            
            // 버튼 UI 상태 변경
            document.querySelectorAll('.sort-btn').forEach(btn => {
                btn.classList.replace('btn-secondary', 'btn-outline-secondary');
            });
            this.classList.replace('btn-outline-secondary', 'btn-secondary');

            // 데이터 로드
            loadQuizzes(sortType);
        });
    });
});

function loadQuizzes(sortType = 'latest') {
    const url = `/quiz/list?sort=${sortType}`;
    
    fetch(url)
        .then(res => {
            if (!res.ok) throw new Error('데이터를 불러올 수 없습니다.');
            return res.json();
        })
        .then(data => {
            renderQuizList(data);
        })
        .catch(err => console.error("로딩 실패:", err));
}

function renderQuizList(quizzes) {
    // HTML의 id="list" 영역을 타겟팅합니다.
    const container = document.getElementById('list'); 
    if (!container) return;

    container.innerHTML = ''; 

    if (quizzes.length === 0) {
        container.innerHTML = '<div class="text-center py-5"><p class="text-gray-500">등록된 퀴즈가 없습니다.</p></div>';
        return;
    }

    quizzes.forEach(quiz => {
        // 이미지 경로 처리 (첫 번째 질문의 이미지가 있을 경우)
        const imagePath = quiz.questions && quiz.questions.length > 0 && quiz.questions[0].image 
                          ? `/uploads/quiz/${quiz.questions[0].image}` 
                          : null;

        const thumbHtml = imagePath 
            ? `<div class="thumb" style="background-image:url('${imagePath}')"></div>`
            : `<div class="thumb placeholder-thumb"><i class="fas fa-question fa-3x text-gray-300"></i></div>`;

        const item = `
            <a href="/quiz/view/${quiz.quizId}" class="quiz-item-link">
                <div class="quiz-card">
                    ${thumbHtml}
                    <div class="info">
                        <div class="title">${quiz.title}</div>
                        <div class="desc">${quiz.description || ''}</div>
                        <div class="mt-2" style="font-size: 0.85rem; color: #888;">
                            <i class="fas fa-edit"></i> ${new Date(quiz.createdAt).toLocaleDateString()}
                        </div>
                    </div>
                </div>
            </a>
        `;
        container.insertAdjacentHTML('beforeend', item);
    });
}