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
        chatbotToggle.innerHTML = '🤖';
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

            const data = await response.json();
            
            // 로딩 제거
            hideLoading();

            // AI 응답 표시
            addMessage('assistant', data.message || '응답을 생성할 수 없습니다.');

            // 액션 처리
            if (data.action && data.data) {
                handleAction(data.action, data.data);
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
        avatar.textContent = role === 'user' ? '👤' : '🤖';

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
        avatar.textContent = '🤖';

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
                    showPostCards(data.posts);
                }
                break;

            case 'bookmarks':
                if (data.bookmarks && data.bookmarks.length > 0) {
                    const posts = data.bookmarks.map(b => b.post || b);
                    showPostCards(posts);
                }
                break;

            case 'redirect':
                if (data.url) {
                    window.location.href = data.url;
                }
                break;
        }
    }

    // 게시글 카드 표시
    function showPostCards(posts) {
        if (!chatbotMessages || !posts || posts.length === 0) return;

        const cardsDiv = document.createElement('div');
        cardsDiv.className = 'chatbot-cards';

        const container = document.createElement('div');
        container.className = 'chatbot-cards-container';

        posts.forEach(post => {
            const card = createPostCard(post);
            container.appendChild(card);
        });

        cardsDiv.appendChild(container);

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

    // 초기화 실행
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
