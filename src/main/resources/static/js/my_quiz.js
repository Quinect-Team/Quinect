const listEl = document.getElementById('quiz-list');

// API 호출 부분은 기존과 동일
fetch('/quiz/api/my')
    .then(res => {
        if (!res.ok) throw new Error('API 호출 실패');
        return res.json();
    })
    .then(renderQuizzes)
    .catch(err => {
        console.error(err);
        listEl.innerHTML = '<div class="col-12 text-center py-5"><p class="text-gray-500">목록을 불러오지 못했습니다.</p></div>';
    });

function renderQuizzes(quizzes) {
    if (!quizzes || quizzes.length === 0) {
        // 데이터가 없을 때 메시지 표시
        listEl.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="fas fa-folder-open fa-3x text-gray-300 mb-3"></i>
                <p class="text-gray-500 mb-0">아직 출제한 퀴즈가 없습니다.</p>
                <a href="/setquestion" class="btn btn-primary btn-sm mt-3">새 퀴즈 만들기</a>
            </div>`;
        return;
    }

    listEl.innerHTML = ''; // 초기화

    quizzes.forEach((q, idx) => {
        // 날짜 포맷팅
        const updatedAt = q.updatedAt
            ? new Date(q.updatedAt).toLocaleDateString('ko-KR')
            : '-';

        // 카드 HTML 생성 (Template Literal)
        // col-xl-4 col-md-6: 화면 크기에 따라 한 줄에 3개 또는 2개씩 배치
        const cardHtml = `
            <div class="col-xl-4 col-md-6 mb-4">
                <div class="card border-left-primary shadow h-100 py-2 quiz-card" 
                     onclick="location.href='/setquestion?quizId=${q.quizId}'"
                     style="cursor: pointer; transition: transform 0.2s;">
                    <div class="card-body">
                        <div class="row no-gutters align-items-center">
                            <div class="col mr-2">
                                <div class="text-xs font-weight-bold text-primary text-uppercase mb-1">
                                    Quiz #${idx + 1}
                                </div>
                                <div class="h5 mb-0 font-weight-bold text-gray-800 text-truncate" title="${q.title}">
                                    ${q.title}
                                </div>
                                <div class="mt-2 text-xs text-gray-500">
                                    <i class="far fa-calendar-alt mr-1"></i> ${updatedAt}
                                </div>
                            </div>
                            <div class="col-auto">
                                <i class="fas fa-pen fa-2x text-gray-300"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;

        // 리스트에 추가
        listEl.insertAdjacentHTML('beforeend', cardHtml);
    });
}