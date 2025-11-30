const $ = (s) => document.querySelector(s);
const $$ = (s) => Array.from(document.querySelectorAll(s));

const path = location.pathname;
const isList = path.endsWith("/") || path === "";
const isDetail = path.includes("/posts/") && !path.includes("/edit") && !path.includes("/write");
const isCreate = path.includes("/posts/write");

/* =========================================================
   다크모드
========================================================= */
(function initDarkMode() {
  const btn = $("#darkModeBtn");
  if (!btn) return;
  btn.addEventListener("click", () => {
    document.body.classList.toggle("dark");
    btn.textContent = document.body.classList.contains("dark")
      ? "☀️ 라이트모드"
      : "🌙 다크모드";
  });
})();

/* =========================================================
   알림 아이콘
========================================================= */
(function initNotifications() {
  const icon = document.getElementById("notifIcon");
  const badge = document.getElementById("notifBadge");
  const dropdown = document.getElementById("notifDropdown");
  if (!icon || !badge || !dropdown) return;

  let open = false;

  async function loadCount() {
    try {
      const res = await fetch("/api/notifications/unread-count");
      const data = await res.json();
      const count = data.count || 0;
      if (count > 0) {
        badge.textContent = count > 9 ? "9+" : String(count);
        badge.classList.remove("hidden");
      } else {
        badge.classList.add("hidden");
      }
    } catch (e) {
      // quiet 실패
    }
  }

  function formatTime(text) {
    if (!text) return "";
    try {
      const d = new Date(text);
      if (Number.isNaN(d.getTime())) return text;
      return d.toLocaleString("ko-KR");
    } catch {
      return text;
    }
  }

  async function loadList() {
    try {
      const res = await fetch("/api/notifications/recent");
      const data = await res.json();
      dropdown.innerHTML = "";
      if (!Array.isArray(data) || data.length === 0) {
        dropdown.innerHTML =
          '<div style="font-size:12px;color:#6b7280;padding:10px;">새로운 알림이 없습니다.</div>';
        return;
      }
      data.forEach((n) => {
        const item = document.createElement("div");
        item.className = "notif-item" + (n.isRead ? "" : " unread");
        
        // 알림 타입별 아이콘
        let icon = "🔔";
        if (n.type === "NEW_APPLICATION") icon = "📝";
        else if (n.type === "APPLICATION_ACCEPTED") icon = "✅";
        else if (n.type === "APPLICATION_REJECTED") icon = "❌";
        else if (n.type === "APPLICATION_CANCELLED") icon = "🚫";
        else if (n.type === "STUDY_GROUP_JOINED") icon = "👥";
        else if (n.type === "NEW_COMMENT") icon = "💬";
        else if (n.type === "NEW_REPLY") icon = "↩️";
        else if (n.type === "COMMENT_LIKE") icon = "❤️";
        
        item.innerHTML = `
          <div class="notif-item-header">
            <span class="notif-icon">${icon}</span>
            <div class="notif-item-content">
              <div class="notif-item-title">${n.title}</div>
              ${n.body ? `<div class="notif-item-body">${n.body}</div>` : ""}
              <div class="notif-item-time">${formatTime(n.createdAt)}</div>
            </div>
            ${!n.isRead ? '<span class="notif-unread-dot"></span>' : ''}
          </div>
        `;
        item.addEventListener("click", async () => {
          try {
            await fetch(`/api/notifications/${n.id}/read`, { method: "POST" });
            item.classList.remove("unread");
            const unreadDot = item.querySelector(".notif-unread-dot");
            if (unreadDot) unreadDot.remove();
          } catch {}
          if (n.linkUrl) {
            location.href = n.linkUrl;
          }
        });
        dropdown.appendChild(item);
      });
    } catch (e) {
      dropdown.innerHTML =
        '<div style="font-size:12px;color:#dc2626;padding:10px;">알림을 불러오지 못했습니다.</div>';
    }
  }

  icon.addEventListener("click", async (e) => {
    e.stopPropagation();
    open = !open;
    if (open) {
      dropdown.classList.remove("hidden");
      await loadList();
      await loadCount();
    } else {
      dropdown.classList.add("hidden");
    }
  });

  document.addEventListener("click", (e) => {
    if (!dropdown.contains(e.target) && e.target !== icon) {
      dropdown.classList.add("hidden");
      open = false;
    }
  });

  // 초기/주기 갱신
  loadCount();
  setInterval(loadCount, 30000);
})();

/* =========================================================
   공통: localStorage helper
========================================================= */
function loadJSON(key, fallback) {
  try {
    const v = localStorage.getItem(key);
    return v ? JSON.parse(v) : fallback;
  } catch {
    return fallback;
  }
}
function saveJSON(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore
  }
}

/* =========================================================
   목록 페이지
   - 검색 + 필터 + 태그 필터
   - 검색 자동완성
   - Spring Boot API 연동
========================================================= */
if (isList) {
  const boardList = $("#boardList");
  const PAGE_SIZE = 9;
  let currentPage = 1;
  let activeTag = "";
  let postsData = [];

  async function fetchPosts() {
    try {
      const sort = $("#sortFilter")?.value || "latest";
      const response = await fetch(`/api/posts?sort=${sort}&size=100`);
      if (!response.ok) throw new Error('Failed to fetch');
      const data = await response.json();
      return data.content || data || [];
    } catch (error) {
      console.error('Error fetching posts:', error);
      return [];
    }
  }

  function buildTagList(posts) {
    const tagSet = new Set();
    posts.forEach((p) => {
      if (p.tags) {
        const tags = typeof p.tags === 'string' ? p.tags.split(',').map(t => t.trim()) : p.tags;
        tags.forEach((t) => tagSet.add(t));
      }
    });
    return Array.from(tagSet);
  }

  function getFiltered() {
    const q = ($("#searchInput")?.value || "").trim();
    const cat = $("#categoryFilter")?.value || "";
    const sort = $("#sortFilter")?.value || "";
    let data = [...postsData];

    if (q) {
      const lower = q.toLowerCase();
      data = data.filter(
        (p) =>
          p.title.toLowerCase().includes(lower) ||
          p.content.toLowerCase().includes(lower) ||
          (p.tags && (typeof p.tags === 'string' ? p.tags : p.tags.join(',')).toLowerCase().includes(lower))
      );
    }
    if (cat) data = data.filter((p) => p.category === cat);
    if (activeTag) {
      data = data.filter((p) => {
        const tags = typeof p.tags === 'string' ? p.tags.split(',').map(t => t.trim()) : (p.tags || []);
        return tags.includes(activeTag);
      });
    }

    if (sort === "popular") {
      data.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0));
    } else {
      data.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    return data;
  }

  function renderTagFilter(tags) {
    const row = $("#tagFilterRow");
    if (!row) return;
    row.innerHTML = "";
    tags.slice(0, 8).forEach((tag) => {
      const pill = document.createElement("span");
      pill.className = "tag-filter-pill";
      pill.textContent = `#${tag}`;
      if (tag === activeTag) pill.classList.add("active");
      pill.addEventListener("click", () => {
        activeTag = activeTag === tag ? "" : tag;
        renderTagFilter(tags);
        renderList(1);
      });
      row.appendChild(pill);
    });
  }

  function renderList(page = 1) {
    const filtered = getFiltered();
    const totalPage = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
    if (page > totalPage) page = totalPage;
    currentPage = page;

    if (!boardList) return;
    boardList.innerHTML = "";

    const slice = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
    if (!slice.length) {
      boardList.innerHTML =
        '<p style="font-size:13px;color:#9ca3af;text-align:center;padding:40px;">조건에 맞는 게시글이 없습니다.</p>';
      return;
    }

    slice.forEach((p) => {
      const card = document.createElement("div");
      card.className = "card";
      const tags = typeof p.tags === 'string' ? p.tags.split(',').map(t => t.trim()).filter(t => t) : (p.tags || []);
      const thumbUrl = tags.length > 0 
        ? `https://picsum.photos/seed/${p.id}/400/300`
        : 'https://picsum.photos/400/300';
      
      card.innerHTML = `
        <div class="card-thumb" style="background-image:url('${thumbUrl}')"></div>
        <div class="card-body">
          <div class="card-title">${p.title || '제목 없음'}</div>
          <div class="card-meta">
            ${p.category || '기타'} · 조회수 ${p.viewCount || 0} · 좋아요 ${p.likeCount || 0}
          </div>
        </div>
      `;
      card.addEventListener("click", () => {
        location.href = `/posts/${p.id}`;
      });
      boardList.appendChild(card);
    });

    const pageInfo = $("#pageInfo");
    if (pageInfo) pageInfo.textContent = `${page} / ${totalPage}`;
  }

  function initSearchSuggest() {
    const input = $("#searchInput");
    const box = $("#searchSuggest");
    if (!input || !box) return;

    function hide() {
      box.classList.add("hidden");
    }

    input.addEventListener("input", () => {
      const q = input.value.trim();
      if (!q) {
        hide();
        return;
      }
      const lower = q.toLowerCase();
      const matches = postsData
        .filter(
          (p) =>
            p.title.toLowerCase().includes(lower) ||
            (p.tags && (typeof p.tags === 'string' ? p.tags : p.tags.join(',')).toLowerCase().includes(lower))
        )
        .slice(0, 8);

      if (!matches.length) {
        hide();
        return;
      }

      box.innerHTML = "";
      matches.forEach((p) => {
        const div = document.createElement("div");
        div.className = "search-suggest-item";
        const tags = typeof p.tags === 'string' ? p.tags.split(',').map(t => t.trim()).slice(0, 2) : (p.tags || []).slice(0, 2);
        const tagText = tags.map((t) => `#${t}`).join(" ");
        div.innerHTML = `<div>${p.title}</div>${
          tagText ? `<div class="tag">${tagText}</div>` : ""
        }`;
        div.addEventListener("click", () => {
          input.value = p.title;
          hide();
          renderList(1);
        });
        box.appendChild(div);
      });

      box.classList.remove("hidden");
    });

    document.addEventListener("click", (e) => {
      if (!box.contains(e.target) && e.target !== input) hide();
    });
  }

  (async function initList() {
    postsData = await fetchPosts();
    renderList(1);

    const allTags = buildTagList(postsData);
    renderTagFilter(allTags);
    initSearchSuggest();

    $("#searchBtn")?.addEventListener("click", () => {
      postsData = [];
      fetchPosts().then(data => {
        postsData = data;
        renderList(1);
      });
    });
    $("#categoryFilter")?.addEventListener("change", () => {
      postsData = [];
      fetchPosts().then(data => {
        postsData = data;
        renderList(1);
      });
    });
    $("#sortFilter")?.addEventListener("change", () => {
      postsData = [];
      fetchPosts().then(data => {
        postsData = data;
        renderList(1);
        // URL 업데이트
        const url = new URL(window.location);
        url.searchParams.set('sort', $("#sortFilter").value);
        window.history.pushState({}, '', url);
      });
    });

    $("#prevPage")?.addEventListener("click", () => {
      if (currentPage > 1) renderList(currentPage - 1);
    });
    $("#nextPage")?.addEventListener("click", () => {
      const total = Math.max(1, Math.ceil(getFiltered().length / PAGE_SIZE));
      if (currentPage < total) renderList(currentPage + 1);
    });
  })();
}

/* =========================================================
   자격증 슬라이드쇼
========================================================= */
(function initCertSlider() {
  const track = document.getElementById("certSliderTrack");
  const prevBtn = document.getElementById("certSliderPrev");
  const nextBtn = document.getElementById("certSliderNext");
  const dotsContainer = document.getElementById("certSliderDots");
  
  if (!track) return;
  
  const cards = track.querySelectorAll(".cert-card");
  if (cards.length === 0) return;
  
  let currentIndex = 0;
  const cardsPerView = window.innerWidth > 768 ? 3 : 1;
  const totalSlides = Math.ceil(cards.length / cardsPerView);
  
  function updateSlider() {
    const offset = -currentIndex * (cards[0].offsetWidth + 20);
    track.style.transform = `translateX(${offset}px)`;
    
    // Dots 업데이트
    if (dotsContainer) {
      const dots = dotsContainer.querySelectorAll(".slider-dot");
      dots.forEach((dot, i) => {
        dot.classList.toggle("active", i === currentIndex);
      });
    }
  }
  
  // Dots 생성
  if (dotsContainer && totalSlides > 1) {
    dotsContainer.innerHTML = "";
    for (let i = 0; i < totalSlides; i++) {
      const dot = document.createElement("div");
      dot.className = "slider-dot" + (i === 0 ? " active" : "");
      dot.addEventListener("click", () => {
        currentIndex = i;
        updateSlider();
      });
      dotsContainer.appendChild(dot);
    }
  }
  
  prevBtn?.addEventListener("click", () => {
    if (currentIndex > 0) {
      currentIndex--;
    } else {
      currentIndex = totalSlides - 1;
    }
    updateSlider();
  });
  
  nextBtn?.addEventListener("click", () => {
    if (currentIndex < totalSlides - 1) {
      currentIndex++;
    } else {
      currentIndex = 0;
    }
    updateSlider();
  });
  
  // 자동 슬라이드
  setInterval(() => {
    if (currentIndex < totalSlides - 1) {
      currentIndex++;
    } else {
      currentIndex = 0;
    }
    updateSlider();
  }, 5000);
  
  // 반응형 처리
  let resizeTimer;
  window.addEventListener("resize", () => {
    clearTimeout(resizeTimer);
    resizeTimer = setTimeout(() => {
      currentIndex = 0;
      updateSlider();
    }, 250);
  });
})();

/* =========================================================
   상세 페이지
   - 자동 목차 + active 스크롤 하이라이트
   - 스크롤 진행바
   - 댓글 기능 (목업)
   - AI 요약 (목업)
   - 최근 본 스터디 localStorage
========================================================= */
if (isDetail) {
  const LS_RECENT = "swm_recent_posts";

  // URL에서 post ID 추출
  const pathParts = location.pathname.split('/');
  const postId = pathParts[pathParts.length - 1];

  // 최근 본 스터디 저장
  let recent = loadJSON(LS_RECENT, []);
  const postTitle = $("#detailTitle")?.textContent || "";
  const postCategory = $("#detailCategory")?.textContent || "";
  
  recent = recent.filter((r) => r.id !== postId);
  recent.unshift({
    id: postId,
    title: postTitle,
    category: postCategory
  });
  if (recent.length > 5) recent = recent.slice(0, 5);
  saveJSON(LS_RECENT, recent);

  // 최근 본 스터디 렌더 (detail 사이드바에 #recentPosts 있을 때만)
  const recentBox = $("#recentPosts");
  if (recentBox) {
    recentBox.innerHTML = "";
    recent.forEach((r) => {
      const li = document.createElement("li");
      li.className = "author-post-list-item";
      li.innerHTML = `
        <div style="font-size:13px;font-weight:600;">${r.title}</div>
        <div style="font-size:11px;color:#6b7280;">
          ${r.category}
        </div>
      `;
      li.addEventListener("click", () => {
        location.href = `/posts/${r.id}`;
      });
      recentBox.appendChild(li);
    });
  }

  /* ---------- 상단 이미지 캐러셀 (content 안에 carouselInner가 있을 때만) ---------- */
  (function initCarousel() {
    const carouselInner = document.getElementById("carouselInner");
    const carouselRoot = carouselInner?.closest(".carousel");
    if (!carouselInner || !carouselRoot) return;

    const slides = Array.from(carouselInner.querySelectorAll("img"));
    if (!slides.length) return;

    let current = 0;

    function update() {
      carouselInner.style.transform = `translateX(-${current * 100}%)`;
      dots.forEach((d, i) => d.classList.toggle("active", i === current));
    }

    // 이전/다음 버튼 생성
    const prevBtn = document.createElement("button");
    prevBtn.type = "button";
    prevBtn.className = "carousel-btn left";
    prevBtn.textContent = "‹";

    const nextBtn = document.createElement("button");
    nextBtn.type = "button";
    nextBtn.className = "carousel-btn right";
    nextBtn.textContent = "›";

    prevBtn.addEventListener("click", () => {
      current = (current - 1 + slides.length) % slides.length;
      update();
    });
    nextBtn.addEventListener("click", () => {
      current = (current + 1) % slides.length;
      update();
    });

    carouselRoot.appendChild(prevBtn);
    carouselRoot.appendChild(nextBtn);

    // 하단 점(dot) 생성
    const dotsWrap = document.createElement("div");
    dotsWrap.className = "carousel-dots";
    const dots = slides.map((_, idx) => {
      const d = document.createElement("div");
      if (idx === 0) d.classList.add("active");
      d.addEventListener("click", () => {
        current = idx;
        update();
      });
      dotsWrap.appendChild(d);
      return d;
    });
    carouselRoot.appendChild(dotsWrap);

    // 자동 슬라이드
    setInterval(() => {
      current = (current + 1) % slides.length;
      update();
    }, 6000);
  })();

  /* ---------- 댓글 데이터 + 좋아요 애니메이션 + 정렬 (실제 API 연동) ---------- */
  let comments = [];
  let commentSort = "latest";

  async function fetchComments() {
    try {
      const res = await fetch(`/api/posts/${postId}/comments?sort=${commentSort}`);
      const data = await res.json();
      comments = Array.isArray(data) ? data : [];
      renderComments();
    } catch (e) {
      console.error("Failed to load comments", e);
    }
  }

  function renderComments() {
    const list = $("#commentList");
    if (!list) return;
    list.innerHTML = "";

    let sorted = [...comments];
    if (commentSort === "popular") {
      sorted.sort((a, b) => (b.likes || 0) - (a.likes || 0));
    } else {
      sorted.sort((a, b) => (b.id || 0) - (a.id || 0));
    }

    const roots = sorted.filter((c) => !c.parentId);
    const replies = sorted.filter((c) => c.parentId);

    function createCommentNode(c, isReply = false) {
      const item = document.createElement("div");
      item.className = isReply ? "comment-item reply-item" : "comment-item";
      item.dataset.id = c.id;
      item.innerHTML = `
        <img class="comment-avatar" src="${c.avatar || 'https://i.pravatar.cc/40?img=5'}">
        <div class="comment-body">
          <div class="comment-top">
            <span class="comment-user">${c.user || '익명'}</span>
            <span class="comment-time">${c.time || ''}</span>
          </div>
          <div class="comment-content">${c.content}</div>
          <div class="comment-actions">
            <span class="comment-like" data-id="${c.id}">❤️ 좋아요 (${c.likes || 0})</span>
            <span class="comment-reply" data-id="${c.id}">↪ 답글</span>
            <span class="comment-report" data-id="${c.id}">🚩 신고</span>
          </div>
        </div>
      `;
      return item;
    }

    roots.forEach((root) => {
      const node = createCommentNode(root, false);
      list.appendChild(node);
      replies
        .filter((r) => r.parentId === root.id)
        .forEach((r) => {
          const rn = createCommentNode(r, true);
          list.appendChild(rn);
        });
    });

    const cnt = $("#commentCount");
    if (cnt) cnt.textContent = `(${comments.length})`;
  }

  fetchComments();

  $("#commentSort")?.addEventListener("change", (e) => {
    commentSort = e.target.value;
    fetchComments();
  });

  $("#commentBtn")?.addEventListener("click", async () => {
    const input = $("#commentInput");
    const txt = (input?.value || "").trim();
    if (!txt) return;

    if (!window.loginUser) {
      alert("로그인이 필요합니다.");
      location.href = "/auth?error=login_required";
      return;
    }

    try {
      const formData = new FormData();
      formData.append("content", txt);
      const res = await fetch(`/api/posts/${postId}/comments`, {
        method: "POST",
        body: formData
      });
      const result = await res.json();
      if (result.success) {
        input.value = "";
        await fetchComments();
      } else {
        alert(result.message || "댓글 등록에 실패했습니다.");
      }
    } catch (e) {
      console.error("Failed to add comment", e);
      alert("댓글 등록 중 오류가 발생했습니다.");
    }
  });

  $("#commentList")?.addEventListener("click", async (e) => {
    const id = Number(e.target.dataset.id);
    if (!id) return;

    if (e.target.classList.contains("comment-like")) {
      if (!window.loginUser) {
        alert("로그인이 필요합니다.");
        location.href = "/auth?error=login_required";
        return;
      }
      try {
        const res = await fetch(`/api/comments/${id}/like`, { method: "POST" });
        const result = await res.json();
        if (result.success) {
          await fetchComments();
          const likeEl = document.querySelector(`.comment-like[data-id="${id}"]`);
          if (likeEl) {
            likeEl.classList.remove("like-anim");
            void likeEl.offsetWidth;
            likeEl.classList.add("like-anim");
          }
        } else {
          alert(result.message || "댓글 좋아요 처리 중 오류가 발생했습니다.");
        }
      } catch (err) {
        console.error("toggle comment like error", err);
        alert("댓글 좋아요 처리 중 오류가 발생했습니다.");
      }
    } else if (e.target.classList.contains("comment-reply")) {
      if (!window.loginUser) {
        alert("로그인이 필요합니다.");
        location.href = "/auth?error=login_required";
        return;
      }
      const replyText = prompt("대댓글 내용을 입력하세요:");
      if (!replyText) return;
      try {
        const formData = new FormData();
        formData.append("content", replyText);
        formData.append("parentId", String(id));
        const res = await fetch(`/api/posts/${postId}/comments`, {
          method: "POST",
          body: formData
        });
        const result = await res.json();
        if (result.success) {
          await fetchComments();
        } else {
          alert(result.message || "대댓글 등록에 실패했습니다.");
        }
      } catch (err) {
        console.error("add reply error", err);
        alert("대댓글 등록 중 오류가 발생했습니다.");
      }
    } else if (e.target.classList.contains("comment-report")) {
      alert("댓글 신고 기능은 준비 중입니다.");
    }
  });

  /* ---------- AI 요약 기능은 post-detail.html의 인라인 스크립트에서 처리 ---------- */
  // board.js의 중복 코드 제거 - post-detail.html에서 직접 처리

  /* ---------- 자동 목차 + active 하이라이트 ---------- */
  (function initTOC() {
    const toc = $("#tocContainer");
    const content = $("#detailContent");
    if (!toc || !content) return;

    const headers = content.querySelectorAll("h2, h3");
    if (headers.length === 0) {
      toc.style.display = "none";
      return;
    }

    let html = "<h3>목차</h3><ul>";
    headers.forEach((h, i) => {
      const id = "section-" + i;
      h.id = id;
      html += `<li data-id="${id}" style="margin-left:${
        h.tagName === "H3" ? "10px" : "0"
      }">${h.textContent}</li>`;
    });
    html += "</ul>";
    toc.innerHTML = html;

    toc.addEventListener("click", (e) => {
      const id = e.target.dataset.id;
      if (!id) return;
      const el = document.getElementById(id);
      if (el) el.scrollIntoView({ behavior: "smooth" });
    });

    const tocItems = toc.querySelectorAll("li");

    function updateActiveTOC() {
      let activeIndex = 0;
      headers.forEach((h, i) => {
        const rect = h.getBoundingClientRect();
        if (rect.top < window.innerHeight * 0.3) {
          activeIndex = i;
        }
      });
      tocItems.forEach((li, i) => {
        li.classList.toggle("active", i === activeIndex);
      });
    }

    updateActiveTOC();
    document.addEventListener("scroll", updateActiveTOC);
  })();

  /* ---------- 스크롤 진행바 ---------- */
  (function scrollProgressBar() {
    const bar = $("#scrollProgress");
    if (!bar) return;
    document.addEventListener("scroll", () => {
      const top = window.scrollY;
      const height = document.body.scrollHeight - window.innerHeight;
      const percent = height > 0 ? (top / height) * 100 : 0;
      bar.style.width = percent + "%";
    });
  })();

  /* ---------- 작성자의 다른 스터디 추천 ---------- */
  (function renderAuthorPosts() {
    const list = $("#authorPosts");
    if (!list) return;
    // TODO: 실제 API에서 작성자의 다른 게시글 가져오기
    list.innerHTML = '<li style="font-size:12px;color:#9ca3af;">다른 게시글이 없습니다.</li>';
  })();

  /* ---------- 코드 하이라이트 (highlight.js) ---------- */
  if (window.hljs) {
    $$("pre code").forEach((block) => {
      window.hljs.highlightElement(block);
    });
  }
}

/* =========================================================
   작성 페이지
   - WYSIWYG toolbar
   - 태그 입력 + AI 자동 태그 추천(목업)
   - 실시간 미리보기
========================================================= */
if (isCreate) {
  const titleInput = $("#postTitle");
  const previewTitle = $("#previewTitle");
  const previewCategory = $("#previewCategory");
  const categorySelect = $("#categorySelect");
  const modeSelect = $("#modeSelect");
  const locationInput = $("#locationInput");
  const capacityInput = $("#capacityInput");
  const previewMode = $("#previewMode");
  const previewLocation = $("#previewLocation");
  const previewCapacity = $("#previewCapacity");
  const editor = $("#editor");
  const contentHidden = $("#content");
  const editorLen = $("#editorLength");
  const tagsInput = $("#tagsInput");
  const previewTags = $("#previewTags");
  const form = document.querySelector("form");

  // 폼 제출 시 에디터 내용을 hidden textarea에 복사
  form?.addEventListener("submit", () => {
    if (contentHidden && editor) {
      contentHidden.value = editor.innerHTML;
    }
  });

  // WYSIWYG 툴바
  $(".editor-toolbar")?.addEventListener("click", (e) => {
    const cmd = e.target.dataset.cmd;
    if (!cmd || !editor) return;
    editor.focus();
    if (cmd === "ul") document.execCommand("insertUnorderedList");
    else if (cmd === "ol") document.execCommand("insertOrderedList");
    else if (cmd === "quote") document.execCommand("formatBlock", false, "blockquote");
    else if (cmd === "code") document.execCommand("formatBlock", false, "pre");
    else document.execCommand(cmd);
  });

  // 카테고리 변경
  categorySelect?.addEventListener("change", () => {
    const value = categorySelect.value || "카테고리";
    previewCategory.textContent = value;
  });

  // 제목 변경
  titleInput?.addEventListener("input", () => {
    previewTitle.textContent = titleInput.value || "제목 미리보기";
  });

  // 진행 방식 변경
  modeSelect?.addEventListener("change", () => {
    previewMode.textContent = modeSelect.value;
  });

  // 지역 변경
  locationInput?.addEventListener("input", () => {
    previewLocation.textContent = locationInput.value || "지역";
  });

  // 모집 정원 변경
  capacityInput?.addEventListener("input", () => {
    const v = capacityInput.value || 0;
    previewCapacity.textContent = `0 / ${v}명`;
  });

  // 본문 글자 수
  editor?.addEventListener("input", () => {
    if (editorLen) editorLen.textContent = editor.innerText.length;
  });
  
  // 초기 글자 수 계산
  if (editor && editorLen) {
    editorLen.textContent = editor.innerText.length;
  }

  // 태그 미리보기
  tagsInput?.addEventListener("input", () => {
    const tags = tagsInput.value.split(',').map(t => t.trim()).filter(t => t);
    if (previewTags) {
      previewTags.innerHTML = "";
      tags.forEach(tag => {
        const span = document.createElement("span");
        span.className = "tag-pill";
        span.textContent = tag;
        previewTags.appendChild(span);
      });
    }
  });

  // AI 자동 태그 추천 (목업)
  const aiTagBtn = $("#aiTagBtn");
  if (aiTagBtn) {
    aiTagBtn.addEventListener("click", () => {
      const title = titleInput?.value || "";
      const body = editor?.innerText || "";
      const full = (title + " " + body).toLowerCase();

      const candidates = [];
      function pushIf(cond, tag) {
        if (cond) candidates.push(tag);
      }

      pushIf(full.includes("spring"), "SpringBoot");
      pushIf(full.includes("jpa"), "JPA");
      pushIf(full.includes("rest"), "REST API");
      pushIf(full.includes("jwt"), "JWT");
      pushIf(full.includes("cs") || full.includes("네트워크"), "CS기초");
      pushIf(full.includes("알고리즘") || full.includes("algorithm"), "알고리즘");
      pushIf(full.includes("자료구조"), "자료구조");
      pushIf(full.includes("면접") || full.includes("interview"), "기술면접");

      const unique = Array.from(new Set(candidates));
      if (!unique.length) {
        alert("본문이 아직 짧아서 추천할 태그가 많지 않아요. 조금만 더 작성해 주세요!");
        return;
      }

      const currentTags = (tagsInput.value || "").split(',').map(t => t.trim()).filter(t => t);
      const newTags = [...currentTags, ...unique].filter((v, i, a) => a.indexOf(v) === i);
      tagsInput.value = newTags.join(',');
      
      // 미리보기 업데이트
      if (previewTags) {
        previewTags.innerHTML = "";
        newTags.forEach(tag => {
          const span = document.createElement("span");
          span.className = "tag-pill";
          span.textContent = tag;
          previewTags.appendChild(span);
        });
      }
      
      alert("AI가 추천한 태그를 추가했습니다. (목업)");
    });
  }
}

