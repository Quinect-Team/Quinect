document.addEventListener("DOMContentLoaded", () => {
	console.log("📌 DOMContentLoaded 시작");
	console.log("현재 URL:", location.href);

	// ==========================================
	// 1️⃣ DOM 요소 참조 (DOMContentLoaded 내부)
	// ==========================================
	const container = document.getElementById("question-container");
	const addBtn = document.getElementById("add-question");
	const loadBtn = document.getElementById("load-question");
	const removeBtn = document.getElementById("remove-question");
	const tempSaveBtn = document.getElementById("temp-save");
	const saveBtn = document.getElementById("save");
	const quizTitle = document.getElementById("quiz-title");
	const quizDesc = document.getElementById("quiz-desc");

	// ==========================================
	// 2️⃣ 상수 및 전역 변수
	// ==========================================
	const LOCAL_KEY = "quiz_temp_v1";
	let currentQuizId = null;
	let currentUserId = null;
	let autosaveTimer = null;

	// CSRF 토큰
	const csrfTokenMeta = document.querySelector("meta[name='_csrf']");
	const csrfHeaderMeta = document.querySelector("meta[name='_csrf_header']");
	const csrfToken = csrfTokenMeta ? csrfTokenMeta.getAttribute("content") : null;
	const csrfHeader = csrfHeaderMeta ? csrfHeaderMeta.getAttribute("content") : null;

	// ==========================================
	// 3️⃣ quizId 처리 (URL 파라미터에서만 가져오기)
	// ==========================================
	const quizId = new URLSearchParams(location.search).get("quizId");
	console.log("quizId param:", quizId);

	if (quizId) {
		// 수정 모드
		console.log("▶ 수정 모드 진입, quizId =", quizId);
		const titleElement = document.getElementById("page-title");
		if (titleElement) {
			titleElement.innerText = "퀴즈 수정하기";
		}
		loadQuiz(quizId);
		return;
	}

	// 새 퀴즈 생성 모드
	console.log("▶ 새 퀴즈 생성 모드");

	// ==========================================
	// 4️⃣ 유틸리티 함수
	// ==========================================

	/**
	 * 디바운스 자동저장
	 */
	function debounceAutoSave() {
		if (autosaveTimer) clearTimeout(autosaveTimer);
		autosaveTimer = setTimeout(() => {
			const data = collectQuizDataForLocal();
			localStorage.setItem(LOCAL_KEY, JSON.stringify(data));
			console.log("[autosave] 로컬에 저장됨.", new Date().toISOString());
		}, 500);
	}

	/**
	 * HTML 이스케이프
	 */
	function escapeHtml(str) {
		if (!str) return "";
		return String(str)
			.replace(/&/g, '&amp;')
			.replace(/"/g, '&quot;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;');
	}

	/**
	 * 점수 공개 여부 반환
	 */
	function getScorePublicValue() {
		const toggle = document.getElementById("scorePublicToggle");
		return toggle ? toggle.checked : false;
	}

	/**
	 * 에디터에 작성된 내용이 있는지 확인
	 */
	function isEditorDirty() {
		const title = document.getElementById("quiz-title").value.trim();
		const desc = document.getElementById("quiz-desc").value.trim();
		if (title.length > 0 || desc.length > 0) return true;

		const blocks = document.querySelectorAll(".question-block");
		for (let block of blocks) {
			if (block.querySelector(".q-title").value.trim().length > 0) return true;
			if (block.querySelector(".q-answer") &&
				block.querySelector(".q-answer").value.trim().length > 0) return true;

			const opts = block.querySelectorAll(".option-text");
			for (let opt of opts) {
				if (opt.value.trim().length > 0) return true;
			}
		}
		return false;
	}

	// ==========================================
	// 5️⃣ 문제 블록 생성 및 이벤트 연결
	// ==========================================

	/**
	 * 새로운 질문 블록 생성
	 */
	function createQuestionBlock() {
		const div = document.createElement("div");
		div.className = "question-block card shadow-sm mb-4 border-left-success";
		div.style.cssText = "max-width: 900px; margin: 0 auto 20px auto; border-radius: 10px; border-left-width: 5px !important;";

		const qNum = container.querySelectorAll(".question-block").length + 1;

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
                    <!-- ⭐ 미리보기 영역 -->
                    <div class="image-preview-wrapper" style="margin-top: 12px; display: none;">
						<div class="file-name-display mb-2 p-2 bg-white rounded shadow-sm border" style="font-size: 0.9rem; word-break: break-all;">
						    <i class="fas fa-file-image mr-1 text-muted"></i>
						    <span class="filename-text"></span>
						</div>
                        <img class="image-preview" style="max-width: 100%; max-height: 200px; border-radius: 8px; object-fit: cover;">
                        <button type="button" class="btn btn-danger btn-sm mt-2 delete-image" style="width: 100%;">
                            <i class="fas fa-trash mr-1"></i> 이미지 제거
                        </button>
                    </div>
                </div>

                <div class="short-answer-block">
                    <input type="text" class="form-control q-answer bg-light border-0" placeholder="정답을 입력하세요." style="padding: 20px;">
                </div>

                <div class="multiple-choice-block" style="display:none;">
                    <div class="option-list mb-2"></div>
                    <button type="button" class="btn btn-outline-primary btn-sm btn-block add-option border-dashed">
                        <i class="fas fa-plus mr-1"></i> 보기 추가
                    </button>
                </div>
            </div>
        `;

		div.dataset.qid = qNum;

		// 파일 인풋 ID 연결
		const fileInput = div.querySelector('.image-input');
		const fileLabel = div.querySelector('.image-label');
		const uniqueId = 'img-input-' + Date.now() + Math.floor(Math.random() * 1000);
		fileInput.id = uniqueId;
		fileLabel.setAttribute('for', uniqueId);

		attachEventsToBlock(div);
		return div;
	}

	function createFileNameLabel(filename, block) {
		const label = document.createElement("div");
		label.className = "file-name-label small text-muted mt-1";
		label.textContent = filename;
		block.querySelector(".image-preview-wrapper").appendChild(label);
		return label;
	}

	/**
	 * 객관식 보기 한 항목 생성
	 */
	function makeOptionElement(qid, optNumber, text = "") {
		const div = document.createElement("div");
		div.className = "option-item";
		div.style.marginBottom = "8px";
		const radioName = `choice-${qid}-${Date.now()}`;

		div.innerHTML = `
            <div class="d-flex align-items-center" style="gap:10px;">
                <input type="radio" name="${radioName}">
                <input type="text" class="option-text form-control" placeholder="선지 입력" style="flex:1;" value="${escapeHtml(text)}">
                <button class="btn btn-danger btn-sm delete-option">X</button>
            </div>
        `;

		const del = div.querySelector(".delete-option");
		del.onclick = (e) => {
			e.preventDefault();
			div.remove();
			debounceAutoSave();
		};
		return div;
	}

	/**
	 * 한 블록에 필요한 이벤트 연결
	 */
	function attachEventsToBlock(block) {
		if (!block) return;

		if (!block.dataset.qid) {
			block.dataset.qid = container.querySelectorAll(".question-block").length;
		}

		// 삭제 버튼
		const delBtn = block.querySelector(".delete-question");
		if (delBtn) {
			delBtn.onclick = (e) => {
				const total = container.querySelectorAll(".question-block").length;
				if (total <= 1) {
					alert("문항은 최소 1개 이상이어야 합니다.");
					return;
				}
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

		// ⭐ 이미지 입력 + 미리보기
		const imageInput = block.querySelector(".image-input");
		if (imageInput) {
			imageInput.onchange = function() {
				const file = this.files[0];

				if (file) {
					const existingLabel = block.querySelector(".file-name-label");
					if (existingLabel) existingLabel.remove();

					const reader = new FileReader();
					reader.onload = function(e) {
						const previewWrapper = block.querySelector(".image-preview-wrapper");
						const previewImg = block.querySelector(".image-preview");
						const filenameText = block.querySelector(".filename-text");

						previewImg.src = e.target.result;
						filenameText.textContent = file.name;  // 파일명 표시
						previewWrapper.style.display = "block";
					};
					reader.readAsDataURL(file);
				} else {
					// 파일 선택 취소 시
					block.querySelector(".file-name-label")?.remove();
				}
				debounceAutoSave();
			};


			// 이미지 제거 버튼
			const deleteImageBtn = block.querySelector(".delete-image");
			if (deleteImageBtn) {
				deleteImageBtn.onclick = function(e) {
					e.preventDefault();
					imageInput.value = "";
					const previewWrapper = block.querySelector(".image-preview-wrapper");
					previewWrapper.style.display = "none";
					block.querySelector(".filename-text").textContent = "";  // 파일명 초기화
					block.querySelector(".image-preview").src = "";
					block.dataset.image = null;
					debounceAutoSave();
				};
			}
		}

		// 보기 추가 버튼
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

		// 기존 삭제 옵션 버튼들
		block.querySelectorAll(".delete-option").forEach(btn => {
			btn.onclick = (ev) => {
				ev.preventDefault();
				const row = btn.closest(".option-item");
				if (row) {
					row.remove();
					debounceAutoSave();
				}
			};
		});

		// 입력 변화 감지
		block.querySelectorAll("input, textarea").forEach(inp => {
			inp.addEventListener("input", debounceAutoSave);
			inp.addEventListener("change", debounceAutoSave);
		});
	}

	/**
	 * 순번 재정렬
	 */
	function renumber() {
		const blocks = container.querySelectorAll(".question-block");
		blocks.forEach((b, idx) => {
			b.dataset.qid = idx + 1;
			const label = b.querySelector(".q-label");
			if (label) label.textContent = `Q${idx + 1}.`;

			b.querySelectorAll(".option-list .option-item").forEach(opt => {
				const radio = opt.querySelector("input[type=radio]");
				if (radio) radio.name = `choice-${b.dataset.qid}`;
			});

			const del = b.querySelector(".delete-question");
			if (del) del.style.display = (idx === 0 ? "none" : "inline-block");
		});
	}

	/**
	 * 화면의 모든 블록에 이벤트 연결 및 QID 설정
	 */
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

	// ==========================================
	// 6️⃣ 데이터 수집 함수
	// ==========================================

	/**
	 * 로컬스토리지용 데이터 수집
	 */
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
				answerOption: null
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
			scorePublic: getScorePublicValue(),
			questions: result
		};
	}

	/**
	 * 로컬에서 불러온 데이터를 화면으로 복원
	 */
	function loadQuizFromLocal(list) {
		const questions = list.questions || list;
		container.innerHTML = "";

		(questions || []).forEach((q, idx) => {
			const block = createQuestionBlock();
			container.appendChild(block);

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
			block.querySelector(".image-input").value = "";
			attachEventsToBlock(block);
		});

		quizTitle.value = list.title || "";
		quizDesc.value = list.description || "";

		if (typeof list.scorePublic === "boolean") {
			const scoreToggle = document.getElementById("scorePublicToggle");
			const scoreText = document.getElementById("scorePublicText");
			if (scoreToggle && scoreText) {
				scoreToggle.checked = list.scorePublic;
				scoreText.textContent = list.scorePublic ? "점수 공개" : "점수 비공개";
			}
		}

		if (typeof list.answerPublic === "boolean") {
			const answerToggle = document.getElementById("answerPublicToggle");
			const answerText = document.getElementById("answerPublicText");
			if (answerToggle && answerText) {
				answerToggle.checked = list.answerPublic;
				answerText.textContent = list.answerPublic ? "정답 공개" : "정답 비공개";
			}
		}

		renumber();
	}

	/**
	 * 서버 전송용 데이터 수집
	 */
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

	/**
	 * 최종 저장용 데이터 수집
	 */
	function collectQuizData() {
		const quiz = {
			quizId: currentQuizId || null,
			title: quizTitle.value,
			description: quizDesc.value,
			scorePublic: getScorePublicValue(),
			userId: currentUserId,
			questions: []
		};

		const questionBlocks = document.querySelectorAll(".question-block");

		questionBlocks.forEach((block, index) => {
			const isMultiple = block.querySelector(".multiple-choice-block").style.display === "block";

			const q = {
				questionText: block.querySelector(".q-title").value,
				quizTypeCode: isMultiple ? 2 : 1,
				point: parseInt(block.querySelector(".q-point").value) || 0,
				answerOption: null,
				subjectiveAnswer: null,
				image: block.dataset.image || null,
				options: []
			};

			if (q.quizTypeCode === 2) {
				const optionInputs = block.querySelectorAll(".option-list input[type=text]");
				optionInputs.forEach((input, i) => {
					q.options.push({
						optionNumber: i + 1,
						optionText: input.value
					});
				});

				const checked = block.querySelector(".option-list input[type=radio]:checked");
				if (checked) {
					const allRadios = Array.from(block.querySelectorAll(".option-list input[type=radio]"));
					const idx = allRadios.indexOf(checked);
					q.answerOption = String(idx + 1);
				}
			} else {
				q.subjectiveAnswer = block.querySelector(".q-answer").value;
			}

			quiz.questions.push(q);
		});

		console.log("서버로 전송할 데이터:", quiz);
		return quiz;
	}

	// ==========================================
	// 7️⃣ 초기화 및 자동 복구
	// ==========================================

	setQidsAndAttachAll();

	// 자동 복구
	(function autoRestoreIfExists() {
		const saved = localStorage.getItem(LOCAL_KEY);
		if (!saved) return;
		try {
			const parsed = JSON.parse(saved);
			loadQuizFromLocal(parsed);
			console.log("[autosave] 로컬 임시본 복원 완료.");
		} catch (e) {
			console.warn("로컬 임시본 파싱 실패", e);
		}
	})();

	// 내용 변경 시 자동 임시 저장
	document.addEventListener("input", debounceAutoSave);
	document.addEventListener("change", debounceAutoSave);

	// ==========================================
	// 8️⃣ 버튼 이벤트 연결
	// ==========================================

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
			if (blocks.length <= 1) {
				alert("문항은 최소 1개 이상이어야 합니다.");
				return;
			}
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

	// 저장 버튼
	saveBtn.addEventListener('click', async () => {
		const scoreToggle = document.getElementById("scorePublicToggle");
		console.log(
			"[SAVE CLICK]",
			"UI checked =", scoreToggle?.checked,
			"currentQuizId =", currentQuizId
		);

		try {
			const quizJson = collectQuizData();

			// 이미지 업로드 처리
			const { imagesMap } = collectForSave(true);
			for (let i = 0; i < imagesMap.length; i++) {
				const file = imagesMap[i];
				if (!file) continue;
				const fd = new FormData();
				fd.append("file", file);
				const headers = csrfToken ? { [csrfHeader]: csrfToken } : undefined;
				const upRes = await fetch("/quiz/upload-image", {
					method: "POST",
					headers: headers,
					body: fd
				});
				if (!upRes.ok) throw new Error("이미지 업로드 실패: " + upRes.status);
				const savedFileName = await upRes.text();
				if (quizJson.questions && quizJson.questions[i]) {
					quizJson.questions[i].image = savedFileName;
				}
			}

			// Quiz 저장
			const postHeaders = { "Content-Type": "application/json" };
			if (csrfToken && csrfHeader) postHeaders[csrfHeader] = csrfToken;

			const saveRes = await fetch("/quiz/save", {
				method: "POST",
				headers: postHeaders,
				body: JSON.stringify(quizJson)
			});
			if (!saveRes.ok) {
				const errText = await saveRes.text().catch(() => null);
				throw new Error("서버 저장 실패: " + (errText || saveRes.status));
			}

			localStorage.removeItem(LOCAL_KEY);
			alert("저장 성공!");
			resetQuizEditor();
			currentQuizId = null;
			window.history.back();
		} catch (err) {
			console.error("저장 중 오류:", err);
			alert("저장 실패: 콘솔을 확인하세요\n" + (err?.message || ""));
		}
	});

	// ==========================================
	// 9️⃣ AI 퀴즈 생성
	// ==========================================

	const aiModalBtn = document.getElementById("open-ai-modal");
	const requestAiBtn = document.getElementById("btn-request-ai");

	if (aiModalBtn) {
		aiModalBtn.addEventListener("click", () => {
			$("#aiCreateModal").modal("show");
		});
	}

	if (requestAiBtn) {
		requestAiBtn.addEventListener("click", async () => {
			if (isEditorDirty()) {
				const isConfirmed = confirm(
					"⚠️ 경고: AI 퀴즈를 생성하면 현재 작성 중인 모든 내용이 삭제되고 덮어씌워집니다.\n\n계속 진행하시겠습니까?"
				);
				if (!isConfirmed) return;
			}

			const topic = document.getElementById("ai-topic").value.trim();
			if (!topic) {
				alert("주제를 입력해주세요!");
				return;
			}

			const difficulty = document.querySelector('input[name="ai-difficulty"]:checked').value;
			const count = document.querySelector('input[name="ai-count"]:checked').value;
			const type = document.getElementById("ai-type").value;

			$("#ai-form").hide();
			$("#ai-loading").show();
			requestAiBtn.disabled = true;

			try {
				const res = await fetch("/api/quiz/generate-ai", {
					method: "POST",
					headers: {
						"Content-Type": "application/json",
						...(csrfToken ? { [csrfHeader]: csrfToken } : {})
					},
					body: JSON.stringify({ topic, difficulty, count: Number(count), type })
				});

				if (!res.ok) throw new Error("AI 생성 실패");

				const generatedQuiz = await res.json();
				applyQuizToUI(generatedQuiz);

				$("#aiCreateModal").modal("hide");
				alert(`AI가 ${generatedQuiz.questions.length}개의 문제를 생성했습니다!`);

				debounceAutoSave();
			} catch (e) {
				alert("문제 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
			} finally {
				$("#ai-loading").hide();
				$("#ai-form").show();
				requestAiBtn.disabled = false;
			}
		});
	}

	// ==========================================
	// 🔟 리셋 함수
	// ==========================================

	function resetQuizUI() {
		container.innerHTML = "";
		quizTitle.value = "";
		quizDesc.value = "";
		currentQuizId = null;
	}

	function resetQuizEditor() {
		try {
			container.innerHTML = "";

			if (quizTitle) quizTitle.value = "";
			if (quizDesc) quizDesc.value = "";

			currentQuizId = null;

			if (typeof createQuestionBlock === "function") {
				const firstBlock = createQuestionBlock();
				container.appendChild(firstBlock);
			} else {
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
				attachEventsToBlock(div);
			}

			renumber();

			if (quizTitle) quizTitle.focus();
		} catch (e) {
			console.error("resetQuizEditor 오류:", e);
			if (container && container.children.length === 0) {
				const fallback = createQuestionBlock ? createQuestionBlock() : document.createElement("div");
				container.appendChild(fallback);
				renumber();
			}
		}
	}

	// ==========================================
	// 1️⃣1️⃣ 모달 및 로드 함수 (window 전역)
	// ==========================================

	/**
	 * 퀴즈 목록 모달 표시
	 */
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

	/**
	 * 퀴즈 목록 모달 닫기
	 */
	window.closeQuizListModal = function() {
		document.activeElement.blur();
		$("#quizListModal").modal("hide");
	};

	/**
	 * 퀴즈 1개 불러오기
	 */
	window.loadQuiz = async function(id) {
		try {
			const res = await fetch(`/quiz/api/${id}`);
			if (!res.ok) throw new Error("퀴즈 불러오기 실패");

			const quiz = await res.json();
			applyQuizToUI(quiz);

			currentQuizId = quiz.quizId;
			closeQuizListModal();
		} catch (e) {
			alert("퀴즈를 불러올 수 없습니다.");
			console.error(e);
		}
	};

	/**
	 * 불러온 퀴즈를 UI에 반영
	 */
	function applyQuizToUI(quiz) {
		if (typeof quiz.scorePublic === "boolean") {
			const scoreToggle = document.getElementById("scorePublicToggle");
			const scoreText = document.getElementById("scorePublicText");

			if (scoreToggle) {
				scoreToggle.checked = quiz.scorePublic;
			}
			if (scoreText) {
				scoreText.textContent = quiz.scorePublic ? "점수 공개" : "점수 비공개";
			}
		}

		if (quiz.title) quizTitle.value = quiz.title;
		if (quiz.description) quizDesc.value = quiz.description;

		container.innerHTML = "";

		(quiz.questions || []).forEach((q, index) => {
			const block = createQuestionBlock();
			container.appendChild(block);

			block.querySelector(".q-title").value = q.questionText || "";
			block.querySelector(".q-point").value = q.point || 10;

			const isMultiple = (q.quizTypeCode === 2);
			const typeBtn = block.querySelector(".dropdown-toggle");

			if (isMultiple) {
				if (typeBtn) typeBtn.textContent = "객관식";
				block.querySelector(".short-answer-block").style.display = "none";
				block.querySelector(".multiple-choice-block").style.display = "block";

				const wrap = block.querySelector(".option-list");
				wrap.innerHTML = "";

				if (Array.isArray(q.options)) {
					q.options.forEach((optObj, i) => {
						const text = typeof optObj === "string" ? optObj : optObj.optionText;
						const opt = makeOptionElement(block.dataset.qid, i + 1, text);
						wrap.appendChild(opt);
					});
				}

				if (q.answerOption) {
					const idx = parseInt(q.answerOption) - 1;
					const radios = wrap.querySelectorAll("input[type=radio]");
					if (radios[idx]) radios[idx].checked = true;
				}
			} else {
				if (typeBtn) typeBtn.textContent = "서술형";
				block.querySelector(".short-answer-block").style.display = "block";
				block.querySelector(".multiple-choice-block").style.display = "none";
				block.querySelector(".q-answer").value = q.subjectiveAnswer || "";
			}

			attachEventsToBlock(block);
		});

		renumber();
	}

	console.log("📌 DOMContentLoaded 완료 ✅");
});
