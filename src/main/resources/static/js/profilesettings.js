document.addEventListener("DOMContentLoaded", function() {
        // 변수 설정
        const inputs = document.querySelectorAll('.check-change');
        const saveButton = document.getElementById('saveButton');
        let initialValues = {};
        let nextTabTarget = null; // 이동하려고 했던 탭 저장용
        let bypassCheck = false;  // 모달 확인 후 강제 이동용 플래그

        // 1. 초기값 저장
        inputs.forEach(input => {
            initialValues[input.name] = input.value;
        });

        // 2. 변경 여부 확인 함수 (true면 변경됨)
        function hasUnsavedChanges() {
            let isChanged = false;
            inputs.forEach(input => {
                if (input.value !== initialValues[input.name]) {
                    isChanged = true;
                }
            });
            return isChanged;
        }

        // 3. 입력 감지 및 저장 버튼 활성화 로직
        function checkChanges() {
            const isChanged = hasUnsavedChanges();
            saveButton.disabled = !isChanged; // 변경되면 버튼 활성화
        }

        inputs.forEach(input => {
            input.addEventListener('input', checkChanges);
        });

        // ▼▼▼ [기능 1] 탭 이동 시 저장 버튼 숨기기/보이기 ▼▼▼
        $('a[data-toggle="pill"]').on('shown.bs.tab', function (e) {
            // 현재 활성화된 탭의 href 가져오기
            const targetId = $(e.target).attr('href');

            // '일반 설정(#v-pills-general)' 탭이면 버튼 숨김 (display: none)
            if (targetId === '#v-pills-general') {
                saveButton.style.display = 'none';
            } else {
                // 다른 탭이면 버튼 다시 보임
                saveButton.style.display = 'inline-flex'; // flex 유지
            }
        });

        // ▼▼▼ [기능 2] 탭 이동 가로채기 (변경사항 확인) ▼▼▼
        $('a[data-toggle="pill"]').on('hide.bs.tab', function (e) {
            if (bypassCheck) {
                // 모달에서 '이동'을 눌렀으면 검사 없이 통과
                bypassCheck = false; 
                return;
            }

            // 값이 바뀌었는데 다른 탭으로 가려고 하면?
            if (hasUnsavedChanges()) {
                e.preventDefault(); // 1. 이동 일단 멈춰!
                
                // 2. 가려고 했던 탭 기억해두기
                nextTabTarget = $(e.relatedTarget); 
                
                // 3. 경고 모달 띄우기
                $('#unsavedChangesModal').modal('show');
            }
        });

        // ▼▼▼ 모달에서 '네, 이동합니다' 클릭 시 처리 ▼▼▼
        document.getElementById('confirmLeaveBtn').addEventListener('click', function() {
            // 1. 폼 값을 초기값으로 되돌리기 (취소했으므로)
            inputs.forEach(input => {
                input.value = initialValues[input.name];
            });
            
            // 2. 저장 버튼 비활성화 (변경사항 없어졌으므로)
            saveButton.disabled = true;

            // 3. 모달 닫기
            $('#unsavedChangesModal').modal('hide');

            // 4. 아까 가려고 했던 탭으로 강제 이동
            if (nextTabTarget) {
                bypassCheck = true; // 검사 건너뛰기 플래그 ON
                nextTabTarget.tab('show'); // 탭 이동
            }
        });
    });