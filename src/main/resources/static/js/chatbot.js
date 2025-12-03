// AI 챗봇 JavaScript

(function() {
    'use strict';

    // DOM 요소
    const chatbotToggle = document.getElementById('chatbotToggle');
    const chatbotWindow = document.getElementById('chatbotWindow');
    const chatbotMessages = document.getElementById('chatbotMessages');
    const chatbotInput = document.getElementById('chatbotInput');
    const chatbotSendBtn = document.getElementById('chatbotSendBtn');
    const chatbotCloseBtn = document.getElementById('chatbotCloseBtn');
    const chatbotClearBtn = document.getElementById('chatbotClearBtn');

    let isOpen = false;
    let isLoading = false;

    // 초기화
    function init() {
        if (!chatbotToggle || !chatbotWindow) return;

        // 플로팅 버튼 클릭
        chatbotToggle.addEventListener('click', toggleChatbot);

        // 닫기 버튼
        if (chatbotCloseBtn) {
            chatbotCloseBtn.addEventListener('click', closeChatbot);
        }

        // 초기화 버튼
        if (chatbotClearBtn) {
            chatbotClearBtn.addEventListener('click', clearChatHistory);
        }

        // 전송 버튼
        if (chatbotSendBtn) {
            chatbotSendBtn.addEventListener('click', sendMessage);
        }

        // Enter 키로 전송
        if (chatbotInput) {
            chatbotInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    sendMessage();
                }
            });
        }

        // 대화 내역 로드
        loadChatHistory();
    }

    // 챗봇 토글
    function toggleChatbot() {
        isOpen = !isOpen;
        if (isOpen) {
            openChatbot();
        } else {
            closeChatbot();
        }
    }

    // 챗봇 열기
    function openChatbot() {
        chatbotWindow.classList.add('active');
        chatbotToggle.classList.add('active');
        chatbotToggle.innerHTML = '✕';
        isOpen = true;
        
        // 입력창 포커스
        if (chatbotInput) {
            setTimeout(() => chatbotInput.focus(), 100);
        }
    }

    // 챗봇 닫기
    function closeChatbot() {
        chatbotWindow.classList.remove('active');
        chatbotToggle.classList.remove('active');
        chatbotToggle.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>';
        isOpen = false;
    }

    // 메시지 전송
    async function sendMessage() {
        if (!chatbotInput || isLoading) return;

        const message = chatbotInput.value.trim();
        if (!message) return;

        // 사용자 메시지 표시
        addMessage('user', message);
        chatbotInput.value = '';
        
        // 로딩 표시
        showLoading();

        try {
            const response = await fetch('/api/chatbot/message', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `message=${encodeURIComponent(message)}`
            });

            // 응답 상태 확인
            if (!response.ok) {
                const errorText = await response.text();
                let errorMessage = '일시적인 오류가 발생했습니다.';
                try {
                    const errorData = JSON.parse(errorText);
                    errorMessage = errorData.message || errorData.error || errorMessage;
                } catch (e) {
                    errorMessage = `서버 오류 (${response.status}): ${errorText || '알 수 없는 오류'}`;
                }
                hideLoading();
                addMessage('assistant', errorMessage);
                return;
            }

            const data = await response.json();
            
            // 로딩 제거
            hideLoading();

            // AI 응답 표시
            addMessage('assistant', data.message || '응답을 생성할 수 없습니다.');

            // 확인이 필요한 액션인 경우
            if (data.needsConfirmation && data.action) {
                // 원본 메시지 저장
                data.originalMessage = message;
                showActionConfirmation(data.action, data.confirmationMessage || '이 작업을 진행할까요?', data);
                return;
            }

            // 액션 처리 (data가 있으면 처리)
            if (data.action) {
                if (data.data) {
                    handleAction(data.action, data.data);
                } else if (data.action === 'SEARCH_POSTS' && data.actionData) {
                    // 검색 액션인데 data가 없는 경우 (검색 결과가 비어있을 수 있음)
                    addMessage('assistant', `"${data.actionData}"에 대한 검색 결과를 찾지 못했습니다. 다른 키워드로 검색해보세요.`);
                }
            }

        } catch (error) {
            console.error('챗봇 오류:', error);
            hideLoading();
            addMessage('assistant', '죄송합니다. 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        }
    }

    // 메시지 추가
    function addMessage(role, content) {
        if (!chatbotMessages) return;

        const messageDiv = document.createElement('div');
        messageDiv.className = `chatbot-message ${role}`;

        const avatar = document.createElement('div');
        avatar.className = 'chatbot-message-avatar';
        if (role === 'user') {
            avatar.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>';
        } else {
            avatar.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>';
        }

        const messageContent = document.createElement('div');
        messageContent.className = 'chatbot-message-content';
        messageContent.textContent = content;

        messageDiv.appendChild(avatar);
        messageDiv.appendChild(messageContent);
        chatbotMessages.appendChild(messageDiv);

        // 스크롤 맨 아래로
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    // 로딩 표시
    function showLoading() {
        if (!chatbotMessages || isLoading) return;

        isLoading = true;
        if (chatbotSendBtn) chatbotSendBtn.disabled = true;

        const loadingDiv = document.createElement('div');
        loadingDiv.className = 'chatbot-message assistant';
        loadingDiv.id = 'chatbotLoading';

        const avatar = document.createElement('div');
        avatar.className = 'chatbot-message-avatar';
        avatar.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path></svg>';

        const loadingContent = document.createElement('div');
        loadingContent.className = 'chatbot-loading';
        loadingContent.innerHTML = '<span></span><span></span><span></span>';

        loadingDiv.appendChild(avatar);
        loadingDiv.appendChild(loadingContent);
        chatbotMessages.appendChild(loadingDiv);

        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    // 로딩 제거
    function hideLoading() {
        isLoading = false;
        if (chatbotSendBtn) chatbotSendBtn.disabled = false;

        const loadingDiv = document.getElementById('chatbotLoading');
        if (loadingDiv) {
            loadingDiv.remove();
        }
    }

    // 액션 처리
    function handleAction(action, data) {
        if (!data || !data.type) return;

        switch (data.type) {
            case 'posts':
                if (data.posts && data.posts.length > 0) {
                    const title = data.keyword ? `"${data.keyword}" 검색 결과` : null;
                    showPostCards(data.posts, data.redirectUrl, title);
                }
                break;

            case 'mypage':
                if (data.posts && data.posts.length > 0) {
                    showPostCards(data.posts, data.redirectUrl, '마이페이지');
                } else if (data.redirectUrl) {
                    // 게시글이 없어도 페이지로 이동
                    window.location.href = data.redirectUrl;
                }
                break;

            case 'bookmarks':
                if (data.posts && data.posts.length > 0) {
                    showPostCards(data.posts, data.redirectUrl, '북마크');
                } else if (data.redirectUrl) {
                    // 게시글이 없어도 페이지로 이동
                    window.location.href = data.redirectUrl;
                }
                break;

            case 'redirect':
                if (data.url) {
                    window.location.href = data.url;
                }
                break;
        }
    }

    // 액션 확인 표시
    function showActionConfirmation(action, message, data) {
        if (!chatbotMessages) return;

        const confirmDiv = document.createElement('div');
        confirmDiv.className = 'chatbot-confirmation';
        confirmDiv.dataset.action = action;
        confirmDiv.dataset.originalMessage = data.originalMessage || '';
        
        const confirmMessage = document.createElement('div');
        confirmMessage.className = 'chatbot-confirmation-message';
        confirmMessage.textContent = message;
        confirmDiv.appendChild(confirmMessage);

        const confirmButtons = document.createElement('div');
        confirmButtons.className = 'chatbot-confirmation-buttons';
        
        // 확인 버튼
        const confirmBtn = document.createElement('button');
        confirmBtn.className = 'chatbot-confirm-btn';
        confirmBtn.textContent = '확인';
        confirmBtn.onclick = async () => {
            confirmDiv.remove();
            showLoading();
            
            // 확인 후 다시 요청 전송
            try {
                const originalMessage = confirmDiv.dataset.originalMessage || '';
                const response = await fetch('/api/chatbot/message', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: `message=${encodeURIComponent(originalMessage)}&confirmed=true&actionType=${encodeURIComponent(action)}`
                });

                if (!response.ok) {
                    hideLoading();
                    addMessage('assistant', '요청 처리 중 오류가 발생했습니다.');
                    return;
                }

                const result = await response.json();
                hideLoading();

                // 액션 처리
                if (result.action && result.data) {
                    handleAction(result.action, result.data);
                } else if (result.message) {
                    addMessage('assistant', result.message);
                }
            } catch (error) {
                console.error('확인 후 요청 오류:', error);
                hideLoading();
                addMessage('assistant', '요청 처리 중 오류가 발생했습니다.');
            }
        };
        
        // 취소 버튼
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'chatbot-cancel-btn';
        cancelBtn.textContent = '취소';
        cancelBtn.onclick = () => {
            confirmDiv.remove();
        };

        confirmButtons.appendChild(confirmBtn);
        confirmButtons.appendChild(cancelBtn);
        confirmDiv.appendChild(confirmButtons);

        // 마지막 메시지에 추가
        const lastMessage = chatbotMessages.lastElementChild;
        if (lastMessage && lastMessage.classList.contains('assistant')) {
            lastMessage.appendChild(confirmDiv);
        } else {
            const messageDiv = document.createElement('div');
            messageDiv.className = 'chatbot-message assistant';
            messageDiv.appendChild(confirmDiv);
            chatbotMessages.appendChild(messageDiv);
        }

        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    // 게시글 카드 표시
    function showPostCards(posts, redirectUrl = null, pageTitle = null) {
        if (!chatbotMessages || !posts || posts.length === 0) return;

        const cardsDiv = document.createElement('div');
        cardsDiv.className = 'chatbot-cards';

        // 페이지 제목 표시
        if (pageTitle) {
            const titleDiv = document.createElement('div');
            titleDiv.className = 'chatbot-cards-title';
            titleDiv.textContent = pageTitle;
            cardsDiv.appendChild(titleDiv);
        }

        const container = document.createElement('div');
        container.className = 'chatbot-cards-container';

        posts.forEach(post => {
            const card = createPostCard(post);
            container.appendChild(card);
        });

        cardsDiv.appendChild(container);

        // 페이지로 이동 버튼 추가 (새창/현재창 선택)
        if (redirectUrl) {
            const actionDiv = document.createElement('div');
            actionDiv.className = 'chatbot-cards-action';
            
            // 현재창에서 열기 버튼
            const currentBtn = document.createElement('button');
            currentBtn.className = 'chatbot-action-btn';
            currentBtn.textContent = pageTitle ? `${pageTitle} 보기` : '전체 보기';
            currentBtn.onclick = () => {
                window.location.href = redirectUrl;
            };
            
            // 새창에서 열기 버튼
            const newWindowBtn = document.createElement('button');
            newWindowBtn.className = 'chatbot-action-btn chatbot-action-btn-secondary';
            newWindowBtn.textContent = '새창에서 열기';
            newWindowBtn.onclick = () => {
                window.open(redirectUrl, '_blank');
            };
            
            const buttonGroup = document.createElement('div');
            buttonGroup.className = 'chatbot-action-buttons';
            buttonGroup.appendChild(currentBtn);
            buttonGroup.appendChild(newWindowBtn);
            actionDiv.appendChild(buttonGroup);
            cardsDiv.appendChild(actionDiv);
        }

        // 마지막 메시지에 카드 추가
        const lastMessage = chatbotMessages.lastElementChild;
        if (lastMessage && lastMessage.classList.contains('assistant')) {
            lastMessage.appendChild(cardsDiv);
        } else {
            // 새 메시지로 추가
            const messageDiv = document.createElement('div');
            messageDiv.className = 'chatbot-message assistant';
            messageDiv.appendChild(cardsDiv);
            chatbotMessages.appendChild(messageDiv);
        }

        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    // 게시글 카드 생성
    function createPostCard(post) {
        const card = document.createElement('div');
        card.className = 'chatbot-card';
        card.onclick = () => {
            window.location.href = `/posts/${post.id}`;
        };

        const title = document.createElement('div');
        title.className = 'chatbot-card-title';
        title.textContent = post.title || '제목 없음';

        const meta = document.createElement('div');
        meta.className = 'chatbot-card-meta';
        meta.innerHTML = `
            ${post.category || '기타'} · 
            조회 ${post.viewCount || 0} · 
            좋아요 ${post.likeCount || 0}
        `;

        card.appendChild(title);
        card.appendChild(meta);

        // 태그 표시
        if (post.tags) {
            const tags = post.tags.split(',').filter(t => t.trim());
            if (tags.length > 0) {
                const tagsDiv = document.createElement('div');
                tagsDiv.className = 'chatbot-card-tags';
                tags.slice(0, 3).forEach(tag => {
                    const tagSpan = document.createElement('span');
                    tagSpan.className = 'chatbot-card-tag';
                    tagSpan.textContent = tag.trim();
                    tagsDiv.appendChild(tagSpan);
                });
                card.appendChild(tagsDiv);
            }
        }

        return card;
    }

    // 대화 내역 로드
    async function loadChatHistory() {
        try {
            const response = await fetch('/api/chatbot/history');
            const messages = await response.json();

            if (messages && messages.length > 0) {
                messages.forEach(msg => {
                    if (msg.role === 'USER' && msg.message) {
                        addMessage('user', msg.message);
                    } else if (msg.role === 'ASSISTANT' && msg.response) {
                        addMessage('assistant', msg.response);
                        
                        // 액션 데이터가 있으면 처리
                        if (msg.actionType && msg.actionData) {
                            try {
                                const actionData = JSON.parse(msg.actionData);
                                handleAction(msg.actionType, actionData);
                            } catch (e) {
                                // JSON 파싱 실패 시 무시
                            }
                        }
                    }
                });
            }
        } catch (error) {
            console.error('대화 내역 로드 오류:', error);
        }
    }

    // 대화 내역 초기화
    async function clearChatHistory() {
        if (!confirm('대화 내역을 모두 삭제하시겠습니까?')) {
            return;
        }

        try {
            const response = await fetch('/api/chatbot/history', {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            // 응답 상태 확인
            if (!response.ok) {
                const errorText = await response.text();
                alert('대화 내역 초기화에 실패했습니다: ' + (errorText || '서버 오류'));
                return;
            }

            const result = await response.json();

            if (result.success) {
                // 메시지 영역 초기화 (초기 환영 메시지만 남김)
                if (chatbotMessages) {
                    chatbotMessages.innerHTML = `
                        <div class="chatbot-message assistant">
                            <div class="chatbot-message-avatar">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
                                </svg>
                            </div>
                            <div class="chatbot-message-content">
                                안녕하세요! Study With Me AI 어시스턴트입니다. 무엇을 도와드릴까요?<br><br>
                                <strong>예시:</strong><br>
                                • "마이페이지 보여줘"<br>
                                • "프로그래밍 스터디 찾아줘"<br>
                                • "북마크 보여줘"<br>
                                • "게시글 작성하는 방법 알려줘"
                            </div>
                        </div>
                    `;
                }
                console.log('대화 내역이 초기화되었습니다.');
            } else {
                alert('대화 내역 초기화에 실패했습니다: ' + (result.message || '알 수 없는 오류'));
            }
        } catch (error) {
            console.error('대화 내역 초기화 오류:', error);
            alert('대화 내역 초기화 중 오류가 발생했습니다: ' + error.message);
        }
    }

    // 초기화 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
