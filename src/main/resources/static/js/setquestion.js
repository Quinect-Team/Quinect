document.addEventListener("DOMContentLoaded", () => {
	// 참조
	const container = document.getElementById("question-container");
	const addBtn = document.getElementById("add-question");
	const loadBtn = document.getElementById("load-question");
	const removeBtn = document.getElementById("remove-question");
	const tempSaveBtn = document.getElementById("temp-save");
	const saveBtn = document.getElementById("save");
	const quizTitle = document.getElementById("quiz-title");
	const quizDesc = document.getElementById("quiz-desc");

	const LOCAL_KEY = "quiz_temp_v1";
	let currentQuizId = null;
	let currentUserId = null;

	// CSRF (있으면 사용)
	const csrfTokenMeta = document.querySelector("meta[name='_csrf']");
	const csrfHeaderMeta = document.querySelector("meta[name='_csrf_header']");
	const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute("content") : null;
	const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute("content") : null;

	// 디바운스 자동저장 타이머
	let autosaveTimer = null;
	function debounceAutoSave() {
		if (autosaveTimer) clearTimeout(autosaveTimer);
		autosaveTimer = setTimeout(() => {
			const data = collectQuizDataForLocal();
			localStorage.setItem(LOCAL_KEY, JSON.stringify(data));
			console.log("[autosave] 로컬에 저장됨.", new Date().toISOString());
		}, 500);
	}

	// 초기: 기존 블록에 이벤트 연결 및 QID 설정
	setQidsAndAttachAll();

	// 자동 복구: 로컬에 임시본이 있으면 페이지 로드 시 자동 복원 (사용자 확인 없음)
	(function autoRestoreIfExists() {
		const saved = localStorage.getItem(LOCAL_KEY);
		if (!saved) return;
		try {
			const parsed = JSON.parse(saved);
			// 빈 상태일 때만 자동 복원 (혹은 항상 덮어쓰기를 원하면 조건 제거)
			// 여기서는 항상 복원하도록 함 (요청사항: 새로고침 시 자동 복구)
			loadQuizFromLocal(parsed);
			console.log("[autosore] 로컬 임시본 복원 완료.");
		} catch (e) {
			console.warn("로컬 임시본 파싱 실패", e);
		}
	})();


	// ========== 버튼 이벤트 ==========

	// 문제 추가
	addBtn.addEventListener("click", () => {
		const newBlock = createQuestionBlock();
		container.appendChild(newBlock);
		renumber();
		debounceAutoSave();
	});

	// 마지막 문제 제거
	if (removeBtn) {
		removeBtn.addEventListener("click", () => {
			const blocks = container.querySelectorAll(".question-block");
			if (blocks.length <= 1) { alert("문항은 최소 1개 이상이어야 합니다."); return; }
			blocks[blocks.length - 1].remove();
			renumber();
			debounceAutoSave();
		});
	}

	// 임시 저장 (수동 버튼)
	tempSaveBtn.addEventListener("click", () => {
		const data = collectQuizDataForLocal();
		localStorage.setItem(LOCAL_KEY, JSON.stringify(data));
		alert("임시 저장 완료! (이미지 파일은 저장되지 않습니다)");
	});

	// 불러오기 버튼 클릭 시 → DB 리스트 가져오기
	// 불러오기 버튼
	loadBtn.addEventListener("click", async () => {
		try {
			const res = await fetch("/quiz/list");
			if (!res.ok) throw new Error("불러오기 실패");

			const quizzes = await res.json();
			showQuizListModal(quizzes);

		} catch (e) {
			alert("퀴즈 목록을 불러올 수 없습니다.");
			console.error(e);
		}
	});


	// --- [AI 생성] 버튼 이벤트 ---
	const aiModalBtn = document.getElementById("open-ai-modal");
	const requestAiBtn = document.getElementById("btn-request-ai");

	// 1. 모달 열기
	if (aiModalBtn) {
		aiModalBtn.addEventListener("click", () => {
			$("#aiCreateModal").modal("show");
		});
	}

	// 2. 생성 요청 보내기
	if (requestAiBtn) {
		requestAiBtn.addEventListener("click", async () => {

			if (isEditorDirty()) {
				const isConfirmed = confirm("⚠️ 경고: AI 퀴즈를 생성하면 현재 작성 중인 모든 내용이 삭제되고 덮어씌워집니다.\n\n계속 진행하시겠습니까?");

				// 취소 누르면 함수 종료 (AI 요청 안 보냄)
				if (!isConfirmed) return;
			}

			const topic = document.getElementById("ai-topic").value.trim();
			if (!topic) { alert("주제를 입력해주세요!"); return; }

			const difficulty = document.querySelector('input[name="ai-difficulty"]:checked').value;
			const count = document.querySelector('input[name="ai-count"]:checked').value;
			const type = document.getElementById("ai-type").value;

			// 로딩 표시 & 버튼 비활성
			$("#ai-form").hide();
			$("#ai-loading").show();
			requestAiBtn.disabled = true;

			try {
				// 백엔드로 요청
				const res = await fetch("/api/quiz/generate-ai", {
					method: "POST",
					headers: {
						"Content-Type": "application/json",
						...(csrfToken ? { [csrfHeader]: csrfToken } : {})
					},
					body: JSON.stringify({ topic, difficulty, count: Number(count), type })
				});

				if (!res.ok) throw new Error("AI 생성 실패");

				const generatedQuiz = await res.json(); // JSON 응답 받기

				// 기존 함수(applyQuizToUI) 재사용하여 화면에 뿌리기!
				applyQuizToUI(generatedQuiz);

				// 모달 닫기 및 초기화
				$("#aiCreateModal").modal("hide");
				alert(`AI가 ${generatedQuiz.questions.length}개의 문제를 생성했습니다!`);

				// 자동 저장 트리거
				debounceAutoSave();

			} catch (e) {
				console.error(e);
				alert("문제 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			} finally {
				// UI 복구
				$("#ai-loading").hide();
				$("#ai-form").show();
				requestAiBtn.disabled = false;
			}
		});
	}


	// -----------------------------
	//   모달 열기
	// -----------------------------
	window.showQuizListModal = function(list) {
		const listBox = document.getElementById("quiz-list");
		listBox.innerHTML = "";

		if (!list || list.length === 0) {
			listBox.innerHTML = "<p>저장된 퀴즈가 없습니다.</p>";
		} else {
			list.forEach(q => {
				const btn = document.createElement("button");
				btn.className = "list-group-item list-group-item-action";
				btn.textContent = q.title;
				btn.onclick = () => loadQuiz(q.quizId);
				listBox.appendChild(btn);
			});
		}

		$("#quizListModal").modal("show");
	};





	// -----------------------------
	//   모달 닫기
	// -----------------------------
	window.closeQuizListModal = function() {
		document.activeElement.blur();
		$("#quizListModal").modal("hide");
	};


	// -----------------------------
	//   퀴즈 1개 불러오기
	// -----------------------------
	window.loadQuiz = async function(id) {
		try {
			const res = await fetch(`/quiz/api/${id}`);
			if (!res.ok) throw new Error("퀴즈 불러오기 실패");

			const quiz = await res.json();
			applyQuizToUI(quiz);

			currentQuizId = quiz.quizId; // 수정용 ID 저장
			closeQuizListModal();

		} catch (e) {
			alert("퀴즈를 불러올 수 없습니다.");
			console.error(e);
		}
	};



	function resetQuizUI() {
		container.innerHTML = "";
		quizTitle.value = "";
		quizDesc.value = "";
		currentQuizId = null; // 새 퀴즈 모드
	}



	// -----------------------------
	//   불러온 퀴즈 UI 반영
	// -----------------------------
	function applyQuizToUI(quiz) {
		// 1. 제목/설명 덮어쓰기
		if (quiz.title) quizTitle.value = quiz.title;
		if (quiz.description) quizDesc.value = quiz.description;

		// ⭐ [수정] 1번부터 채우기 위해 기존 문제 싹 비우기 (Overwrite)
		const container = document.getElementById("question-container");
		container.innerHTML = "";

		// 2. 문제 루프
		(quiz.questions || []).forEach((q, index) => {
			// 새 블록 생성
			const block = createQuestionBlock();
			container.appendChild(block);

			// 3. 질문/점수 매핑
			block.querySelector(".q-title").value = q.questionText || "";
			block.querySelector(".q-point").value = q.point || 10;

			// 4. 유형 처리 (1:서술형, 2:객관식)
			// (이전에 알려드린 dataset.type 로직 대신 화면 요소 제어로 처리)
			const isMultiple = (q.quizTypeCode === 2);
			const typeBtn = block.querySelector(".dropdown-toggle");

			if (isMultiple) {
				// [객관식]
				if (typeBtn) typeBtn.textContent = "객관식";
				block.querySelector(".short-answer-block").style.display = "none";
				block.querySelector(".multiple-choice-block").style.display = "block";

				const wrap = block.querySelector(".option-list");
				wrap.innerHTML = "";

				// 보기 채우기 (문자열 배열 가정)
				if (Array.isArray(q.options)) {
					q.options.forEach((optObj, i) => {
						const text = typeof optObj === "string"
							? optObj
							: optObj.optionText; // ⭐ 핵심

						const opt = makeOptionElement(block.dataset.qid, i + 1, text);
						wrap.appendChild(opt);
					});
				}
				// 정답 체크
				if (q.answerOption) {
					const idx = parseInt(q.answerOption) - 1;
					const radios = wrap.querySelectorAll("input[type=radio]");
					if (radios[idx]) radios[idx].checked = true;
				}

			} else {
				// [서술형]
				if (typeBtn) typeBtn.textContent = "서술형";
				block.querySelector(".short-answer-block").style.display = "block";
				block.querySelector(".multiple-choice-block").style.display = "none";
				block.querySelector(".q-answer").value = q.subjectiveAnswer || "";
			}

			attachEventsToBlock(block);
		});

		renumber();
	}


	// 내용 변경 시 자동 임시 저장 (디바운스)
	document.addEventListener("input", debounceAutoSave);
	document.addEventListener("change", debounceAutoSave);

	// 저장 버튼 => 서버 전송 (기존 로직 재사용)
	saveBtn.addEventListener('click', async () => {
		try {
			const quizJson = collectQuizData(); // UI 데이터 수집

			// 이미지 업로드 처리 (선택적)
			const { imagesMap } = collectForSave(true); // 기존 이미지 수집 로직 재사용
			for (let i = 0; i < imagesMap.length; i++) {
				const file = imagesMap[i];
				if (!file) continue;
				const fd = new FormData();
				fd.append("file", file);
				const headers = csrfToken ? { [csrfHeader]: csrfToken } : undefined;
				const upRes = await fetch("/quiz/upload-image", { method: "POST", headers: headers, body: fd });
				if (!upRes.ok) throw new Error("이미지 업로드 실패: " + upRes.status);
				const savedFileName = await upRes.text();
				if (quizJson.questions && quizJson.questions[i]) quizJson.questions[i].image = savedFileName;
			}

			// Quiz 저장 (새 퀴즈 or 수정)
			const postHeaders = { "Content-Type": "application/json" };
			if (csrfToken && csrfHeader) postHeaders[csrfHeader] = csrfToken;

			const saveRes = await fetch("/quiz/save", { method: "POST", headers: postHeaders, body: JSON.stringify(quizJson) });
			if (!saveRes.ok) {
				const errText = await saveRes.text().catch(() => null);
				throw new Error("서버 저장 실패: " + (errText || saveRes.status));
			}

			localStorage.removeItem(LOCAL_KEY); // 임시본 삭제
			alert("저장 성공!");
			resetQuizEditor();   // Q1 블록만 남는 초기 상태로 되돌림
			currentQuizId = null; // 기존 퀴즈 ID 제거 → 새 퀴즈 모드

		} catch (err) {
			console.error("저장 중 오류:", err);
			alert("저장 실패: 콘솔을 확인하세요\n" + (err?.message || ""));
		}
	});


	// ----------------- 주요 함수들 -----------------

	// 화면의 모든 question-block에 이벤트 연결 및 qid 설정
	function setQidsAndAttachAll() {
		const blocks = container.querySelectorAll(".question-block");
		blocks.forEach((b, idx) => {
			b.dataset.qid = idx + 1;
			const del = b.querySelector(".delete-question");
			if (del) del.style.display = (idx === 0 ? "none" : "inline-block");
			attachEventsToBlock(b);
		});
		renumber();
	}

	// 한 블록에 필요한 이벤트 연결
	function attachEventsToBlock(block) {
		if (!block) return;

		// ensure qid
		if (!block.dataset.qid) block.dataset.qid = container.querySelectorAll(".question-block").length;

		// 삭제 버튼
		const delBtn = block.querySelector(".delete-question");
		if (delBtn) {
			delBtn.onclick = (e) => {
				const total = container.querySelectorAll(".question-block").length;
				if (total <= 1) { alert("문항은 최소 1개 이상이어야 합니다."); return; }
				block.remove();
				renumber();
				debounceAutoSave();
			};
		}

		// 문제 유형 선택
		const selects = block.querySelectorAll(".select-type");
		selects.forEach(a => {
			a.onclick = (ev) => {
				ev.preventDefault();
				const type = a.dataset.type;
				if (type === "short_answer") {
					block.querySelector(".short-answer-block").style.display = "block";
					block.querySelector(".multiple-choice-block").style.display = "none";
				} else {
					block.querySelector(".short-answer-block").style.display = "none";
					block.querySelector(".multiple-choice-block").style.display = "block";
					const list = block.querySelector(".option-list");
					if (list.children.length === 0) {
						list.appendChild(makeOptionElement(block.dataset.qid, 1));
						list.appendChild(makeOptionElement(block.dataset.qid, 2));
					}
				}
				debounceAutoSave();
			};
		});

		// 이미지 입력 (preview 없음, 선택만 로그)
		const imageInput = block.querySelector(".image-input");
		if (imageInput) {
			imageInput.onchange = () => {
				console.log("이미지 선택됨 for qid", block.dataset.qid, imageInput.files[0]);
				debounceAutoSave();
			};
		}

		// 보기 추가 버튼 (객관식)
		const addOptBtn = block.querySelector(".add-option");
		if (addOptBtn) {
			addOptBtn.onclick = (e) => {
				e.preventDefault();
				const list = block.querySelector(".option-list");
				const idx = list.children.length + 1;
				list.appendChild(makeOptionElement(block.dataset.qid, idx));
				debounceAutoSave();
			};
		}

		// 기존에 있는 삭제 옵션 버튼들
		block.querySelectorAll(".delete-option").forEach(btn => {
			btn.onclick = (ev) => {
				ev.preventDefault();
				const row = btn.closest(".option-item");
				if (row) { row.remove(); debounceAutoSave(); }
			};
		});

		// 입력 변화가 생기면 autosave 트리거 (개별 필드도 글로벌 이벤트로 잡혀있지만 블록 수준에서도 안전하게)
		block.querySelectorAll("input, textarea").forEach(inp => {
			inp.addEventListener("input", debounceAutoSave);
			inp.addEventListener("change", debounceAutoSave);
		});
	}

	// 새로운 question-block DOM 생성 (초기 템플릿과 동일한 구조)
	// 새로운 질문 블록 생성 (디자인 + 너비 수정됨)
	function createQuestionBlock() {
		const div = document.createElement("div");

		// 1. 클래스: 카드 디자인 적용
		div.className = "question-block card shadow-sm mb-4 border-left-success";

		// ⭐ 2. [핵심 수정] 스타일: 너비 900px 및 중앙 정렬 강제 적용
		// 이 줄이 있어야 처음에 로딩된 Q1과 똑같은 폭으로 보입니다.
		div.style.cssText = "max-width: 900px; margin: 0 auto 20px auto; border-radius: 10px; border-left-width: 5px !important;";

		// Q번호 계산
		const container = document.getElementById("question-container");
		const qNum = container.querySelectorAll(".question-block").length + 1;

		// 3. 내부 HTML 구조
		div.innerHTML = `
	      <div class="card-body">
	          <div class="d-flex align-items-center mb-3" style="gap:10px;">
	            <span class="q-label badge badge-success badge-pill px-3 py-2" style="font-size: 1rem;">Q${qNum}</span>
	            
	            <input type="text" class="q-title form-control font-weight-bold" placeholder="문제를 입력하세요" style="flex:1;">
	            <input type="number" class="q-point form-control text-center" placeholder="점수" value="10" style="width:80px;">
	            
	            <div class="btn-group">
	                <button type="button" class="btn btn-outline-secondary dropdown-toggle btn-sm" data-toggle="dropdown">
	                    <i class="fas fa-list-ul mr-1"></i>유형
	                </button>
	                <div class="dropdown-menu dropdown-menu-right shadow">
	                    <a class="dropdown-item select-type" data-type="short_answer" href="#"><i class="fas fa-pen mr-2 text-gray-400"></i>서술형</a>
	                    <a class="dropdown-item select-type" data-type="multiple_choice" href="#"><i class="fas fa-check-square mr-2 text-gray-400"></i>객관식</a>
	                </div>
	            </div>

	            <button class="btn btn-outline-danger btn-sm delete-question">
	                <i class="fas fa-trash"></i>
	            </button>
	          </div>

	          <div class="image-container mb-3 p-3 bg-light rounded text-center" style="border: 2px dashed #e3e6f0;">
	             <input type="file" class="image-input" accept="image/*" style="display: none;">
	             <label class="btn btn-sm btn-light text-primary mb-0 cursor-pointer image-label">
	                <i class="fas fa-image mr-1"></i> 이미지 추가 (선택)
	             </label>
	          </div>

	          <div class="short-answer-block">
	            <input type="text" class="form-control q-answer bg-light border-0" placeholder="정답을 입력하세요." style="padding: 20px;">
	          </div>

	          <div class="multiple-choice-block" style="display:none;">
	            <div class="option-list mb-2"></div>
	            <button class="btn btn-outline-primary btn-sm btn-block add-option border-dashed">
	                <i class="fas fa-plus mr-1"></i> 보기 추가
	            </button>
	          </div>
	      </div>
	    `;

		div.dataset.qid = qNum;

		// 파일 인풋 ID 연결 (label 클릭 시 input 열리게)
		const fileInput = div.querySelector('.image-input');
		const fileLabel = div.querySelector('.image-label');
		const uniqueId = 'img-input-' + Date.now() + Math.floor(Math.random() * 1000);
		fileInput.id = uniqueId;
		fileLabel.setAttribute('for', uniqueId);

		// 이벤트 연결
		attachEventsToBlock(div);
		return div;
	}

	// 객관식 보기 한 항목 생성
	function makeOptionElement(qid, optNumber, text = "") {
		const div = document.createElement("div");
		div.className = "option-item";
		div.style.marginBottom = "8px";
		const radioName = `choice-${qid}-${Date.now()}`; // 고유 네임
		div.innerHTML = `
      <div class="d-flex align-items-center" style="gap:10px;">
        <input type="radio" name="${radioName}">
        <input type="text" class="option-text form-control" placeholder="선지 입력" style="flex:1;" value="${escapeHtml(text)}">
        <button class="btn btn-danger btn-sm delete-option">X</button>
      </div>
    `;
		const del = div.querySelector(".delete-option");
		del.onclick = (e) => { e.preventDefault(); div.remove(); debounceAutoSave(); };
		return div;
	}

	// q-label, radio name 등 순번 재정렬
	function renumber() {
		const blocks = container.querySelectorAll(".question-block");
		blocks.forEach((b, idx) => {
			b.dataset.qid = idx + 1;
			const label = b.querySelector(".q-label");
			if (label) label.textContent = `Q${idx + 1}.`;
			// option radio name 조정
			b.querySelectorAll(".option-list .option-item").forEach(opt => {
				const radio = opt.querySelector("input[type=radio]");
				if (radio) radio.name = `choice-${b.dataset.qid}`;
			});
			// first block delete 숨김
			const del = b.querySelector(".delete-question");
			if (del) del.style.display = (idx === 0 ? "none" : "inline-block");
		});
	}

	// 화면 상태를 localStorage용으로 수집
	function collectQuizDataForLocal() {
		const blocks = container.querySelectorAll(".question-block");
		const result = [];

		blocks.forEach((b, idx) => {
			const isMultiple = b.querySelector(".multiple-choice-block").style.display === "block";
			const item = {
				number: idx + 1,
				quizTypeCode: isMultiple ? 2 : 1,
				questionText: (b.querySelector(".q-title") || {}).value || "",
				point: Number((b.querySelector(".q-point") || {}).value || 0),
				subjectiveAnswer: !isMultiple ? (b.querySelector(".q-answer") || {}).value || "" : "",
				options: [],
				answerOption: null // 객관식이면 index (1-based)
			};

			if (isMultiple) {
				const optionEls = b.querySelectorAll(".option-list .option-item");
				optionEls.forEach((optEl, i) => {
					const txt = (optEl.querySelector(".option-text") || {}).value || "";
					const radio = optEl.querySelector("input[type=radio]");
					if (radio && radio.checked) item.answerOption = i + 1;
					item.options.push(txt);
				});
			}
			result.push(item);
		});

		return {
			title: quizTitle.value || "",
			description: quizDesc.value || "",
			questions: result
		};
	}

	// 로컬에서 불러온 데이터를 화면으로 복원
	function loadQuizFromLocal(list) {
		// 리스트 형식이 바로 questions 배열인지 대응
		const questions = list.questions || list;

		// 기존 모두 제거
		container.innerHTML = "";

		// 복원
		(questions || []).forEach((q, idx) => {
			const block = createQuestionBlock();
			container.appendChild(block);

			// 값 채우기
			block.querySelector(".q-title").value = q.questionText || q.question || "";
			block.querySelector(".q-point").value = q.point || q.points || 0;

			if (Number(q.quizTypeCode) === 2 || q.quizType === "multiple_choice") {
				block.querySelector(".short-answer-block").style.display = "none";
				block.querySelector(".multiple-choice-block").style.display = "block";
				const wrap = block.querySelector(".option-list");
				wrap.innerHTML = "";
				(q.options || []).forEach((optText, i) => {
					const opt = makeOptionElement(block.dataset.qid, i + 1, optText);
					wrap.appendChild(opt);
				});
				// 정답 체크
				if (q.answerOption) {
					const idxAns = Number(q.answerOption) - 1;
					const radios = wrap.querySelectorAll("input[type=radio]");
					if (radios[idxAns]) radios[idxAns].checked = true;
				}
			} else {
				block.querySelector(".short-answer-block").style.display = "block";
				block.querySelector(".multiple-choice-block").style.display = "none";
				block.querySelector(".q-answer").value = q.subjectiveAnswer || q.answer || "";
			}
			// 이미지는 복원 불가 (파일 객체가 없기 때문에)
			block.querySelector(".image-input").value = "";
			attachEventsToBlock(block);
		});

		// 타이틀/설명도 채움
		quizTitle.value = list.title || "";
		quizDesc.value = list.description || "";

		renumber();
	}

	// 서버 전송용 수집 (원래 있던 collectForSave과 호환)
	function collectForSave(includeFiles) {
		const blocks = container.querySelectorAll(".question-block");
		const questions = [];
		const imagesMap = [];

		blocks.forEach((b, idx) => {
			const isMultiple = b.querySelector(".multiple-choice-block").style.display === "block";

			let options = null;
			let answerOption = null;
			if (isMultiple) {
				options = [];
				const optionEls = b.querySelectorAll(".option-list .option-item");
				optionEls.forEach((optEl, i) => {
					const txt = (optEl.querySelector(".option-text") || {}).value || "";
					options.push(txt);
					const radio = optEl.querySelector("input[type=radio]");
					if (radio && radio.checked) answerOption = i + 1;
				});
			}

			const fileInput = b.querySelector(".image-input");
			const file = fileInput && fileInput.files && fileInput.files[0] ? fileInput.files[0] : null;
			imagesMap.push(includeFiles ? file : null);

			const qDto = {
				quizTypeCode: isMultiple ? 2 : 1,
				questionText: (b.querySelector(".q-title") || {}).value || "",
				answerOption: isMultiple ? (answerOption !== null ? String(answerOption) : null) : null,
				point: Number((b.querySelector(".q-point") || {}).value || 0),
				subjectiveAnswer: !isMultiple ? (b.querySelector(".q-answer") || {}).value || null : null,
				image: null,
				options: options
			};

			questions.push(qDto);
		});

		const quizJson = {
			title: quizTitle.value || "",
			description: quizDesc.value || "",
			userId: 1,
			questions: questions
		};

		return { quizJson, imagesMap };
	}

	function collectQuizData() {
		const quiz = {
			quizId: currentQuizId || null,
			title: quizTitle.value,
			description: quizDesc.value,
			userId: currentUserId,
			questions: []
		};

		const questionBlocks = document.querySelectorAll(".question-block");

		questionBlocks.forEach((block, index) => {
			// 1. 객관식인지 확인
			const isMultiple = block.querySelector(".multiple-choice-block").style.display === "block";

			const q = {
				questionText: block.querySelector(".q-title").value,
				quizTypeCode: isMultiple ? 2 : 1, // 1:주관식, 2:객관식
				point: parseInt(block.querySelector(".q-point").value) || 0,
				answerOption: null,
				subjectiveAnswer: null,
				image: block.dataset.image || null,
				options: []
			};

			if (q.quizTypeCode === 2) { // 객관식일 때
				const optionInputs = block.querySelectorAll(".option-list input[type=text]");

				// ▼▼▼ [수정된 부분] 문자열(input.value) 대신 객체({})로 포장해서 넣기 ▼▼▼
				optionInputs.forEach((input, i) => {
					q.options.push({
						optionNumber: i + 1,       // 순서 (1, 2, 3...)
						optionText: input.value    // 보기 내용
					});
				});
				// ▲▲▲ ------------------------------------------------------- ▲▲▲

				// 정답 체크 로직 (인덱스 + 1)
				const checked = block.querySelector(".option-list input[type=radio]:checked");
				if (checked) {
					const allRadios = Array.from(block.querySelectorAll(".option-list input[type=radio]"));
					const idx = allRadios.indexOf(checked);
					q.answerOption = String(idx + 1);
				}

			} else { // 주관식일 때
				q.subjectiveAnswer = block.querySelector(".q-answer").value;
			}

			quiz.questions.push(q);
		});

		console.log("서버로 전송할 데이터:", quiz); // 확인용 로그
		return quiz;
	}

	function resetQuizEditor() {
		// container, quizTitle, quizDesc는 이미 DOMContentLoaded 내부에서 선언된 변수로 보이므로 그대로 사용
		try {
			// 1) 기존 문제 전부 제거
			container.innerHTML = "";

			// 2) 기본 타이틀/설명 초기화
			if (quizTitle) quizTitle.value = "";
			if (quizDesc) quizDesc.value = "";

			// 3) 새 퀴즈 모드로 전환
			currentQuizId = null;

			// 4) Q1 블럭 생성 (가능하면 createQuestionBlock 사용)
			if (typeof createQuestionBlock === "function") {
				const firstBlock = createQuestionBlock();
				container.appendChild(firstBlock);
			} else {
				// fallback: create minimal block DOM if createQuestionBlock가 정의되어 있지 않다면 (방어 코드)
				const div = document.createElement("div");
				div.classList.add("question-block");
				div.innerHTML = `
          <div class="d-flex align-items-center" style="gap:10px;">
            <label class="q-label" style="margin:0; font-weight:bold;">Q1.</label>
            <input type="text" class="q-title" placeholder="문제를 입력하세요" style="flex:1;">
            <input type="number" class="q-point" placeholder="점수" style="width:80px;">
            <button class="btn btn-danger delete-question" style="display:none;">삭제</button>
          </div>
          <div class="image-container" style="margin-top:10px;">
            <input type="file" class="image-input" accept="image/*">
          </div>
          <div class="short-answer-block" style="margin-top:10px;">
            <input type="text" class="q-answer" placeholder="정답을 작성하세요." style="width:100%; padding:5px;">
          </div>
          <div class="multiple-choice-block" style="margin-top:10px; display:none;">
            <div class="option-list"></div>
            <button class="btn btn-secondary btn-sm add-option" style="margin-top:8px;">보기 추가</button>
          </div>
        `;
				container.appendChild(div);
				// attach events so new elements behave like others
				attachEventsToBlock(div);
			}

			// 5) 번호 재정렬 / 이벤트 재연결
			renumber();

			// 포커스 이동: 제목 입력으로 포커스
			if (quizTitle) quizTitle.focus();

		} catch (e) {
			console.error("resetQuizEditor 오류:", e);
			// 실패해도 화면이 완전히 비어있지 않도록 최소 한 블록 보장
			if (container && container.children.length === 0) {
				const fallback = createQuestionBlock ? createQuestionBlock() : document.createElement("div");
				container.appendChild(fallback);
				renumber();
			}
		}
	}




	// HTML 값 삽입 시 안전을 위한 이스케이프
	function escapeHtml(str) {
		if (!str) return "";
		return String(str).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
	}

}); // DOMContentLoaded

// [추가] 현재 에디터에 작성된 내용이 있는지 확인하는 함수
function isEditorDirty() {
	// 1. 제목이나 설명이 있는지 확인
	const title = document.getElementById("quiz-title").value.trim();
	const desc = document.getElementById("quiz-desc").value.trim();
	if (title.length > 0 || desc.length > 0) return true;

	// 2. 문제들 중에 내용이 있는 게 있는지 확인
	const blocks = document.querySelectorAll(".question-block");
	for (let block of blocks) {
		// 문제 제목
		if (block.querySelector(".q-title").value.trim().length > 0) return true;

		// 보기나 정답 등 다른 필드도 체크 (간단하게 제목만 체크해도 충분하지만 꼼꼼하게)
		if (block.querySelector(".q-answer") && block.querySelector(".q-answer").value.trim().length > 0) return true;
		// 객관식 보기 확인
		const opts = block.querySelectorAll(".option-text");
		for (let opt of opts) {
			if (opt.value.trim().length > 0) return true;
		}
	}

	// 아무것도 안 썼으면 false (깨끗함)
	return false;
}