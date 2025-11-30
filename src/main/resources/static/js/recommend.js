// AI 추천 게시글 불러오기 (개선 버전)

let currentRecommendations = [];

// 사용자 선호도 정보 가져오기
async function getUserPreferences() {
  try {
    const res = await fetch('/api/user/preferences');
    if (res.ok) {
      const prefs = await res.json();
      return prefs || [];
    }
  } catch (e) {
    console.warn('선호도 정보를 가져오지 못했습니다:', e);
  }
  return [];
}

// 추천 결과와 사용자 선호도 비교
function analyzeRecommendations(posts, preferences) {
  const prefCategories = new Set(preferences.map(p => p.categoryName?.toLowerCase() || ''));
  let matchCount = 0;
  let matchedCategories = new Set();

  posts.forEach(post => {
    const postCategory = (post.category || '').toLowerCase();
    if (prefCategories.has(postCategory)) {
      matchCount++;
      matchedCategories.add(postCategory);
    }
  });

  return {
    matchCount,
    totalCount: posts.length,
    matchRate: posts.length > 0 ? (matchCount / posts.length * 100).toFixed(1) : 0,
    matchedCategories: Array.from(matchedCategories)
  };
}

// 추천 게시글 불러오기
async function loadRecommendations() {
  const grid = document.getElementById('cardGrid');
  const loadingState = document.getElementById('loadingState');
  const emptyState = document.getElementById('emptyState');
  const recommendStats = document.getElementById('recommendStats');
  
  if (!grid || !loadingState) return;

  // 로딩 상태 표시
  loadingState.style.display = 'flex';
  grid.style.display = 'none';
  emptyState.style.display = 'none';
  recommendStats.style.display = 'none';

  try {
    // 사용자 선호도와 추천 게시글을 동시에 가져오기
    const [postsRes, prefsRes] = await Promise.all([
      fetch('/api/recommendations/posts?size=12'),
      getUserPreferences()
    ]);

    if (!postsRes.ok) throw new Error('추천을 불러오지 못했습니다');

    const posts = await postsRes.json();
    currentRecommendations = Array.isArray(posts) ? posts : [];

    // 로딩 숨기기
    loadingState.style.display = 'none';

    if (!currentRecommendations || currentRecommendations.length === 0) {
      emptyState.style.display = 'flex';
      return;
    }

    // 추천 분석
    const analysis = analyzeRecommendations(currentRecommendations, prefsRes);
    
    // 통계 표시
    if (prefsRes.length > 0) {
      const criteriaText = analysis.matchedCategories.length > 0
        ? analysis.matchedCategories.join(', ')
        : '활동 기반';
      document.getElementById('recommendCriteria').textContent = criteriaText;
      document.getElementById('matchCount').textContent = `${analysis.matchCount}/${analysis.totalCount} (${analysis.matchRate}%)`;
      recommendStats.style.display = 'flex';
    }

    // 카드 그리드 표시
    grid.style.display = 'grid';
    grid.innerHTML = '';

    currentRecommendations.forEach((p, index) => {
      const article = document.createElement('article');
      article.className = 'card';
      article.style.animationDelay = `${index * 0.05}s`;
      
      const thumbUrl = `https://picsum.photos/seed/${p.id}/800/500`;
      const category = p.category || '기타';
      const isMatched = prefsRes.some(pref => 
        (pref.categoryName || '').toLowerCase() === category.toLowerCase()
      );
      
      // 본문에서 HTML 태그 제거 및 텍스트만 추출
      const cleanContent = stripHtmlTags((p.content || '').substring(0, 100));
      
      article.innerHTML = `
        <div class="card-image-wrapper">
          <img src="${thumbUrl}" alt="${p.title}" loading="lazy">
          ${isMatched ? '<div class="match-badge">매칭</div>' : ''}
          ${p.likeCount > 10 ? '<div class="popular-badge">인기</div>' : ''}
        </div>
        <div class="card-body">
          <div class="card-category">${category}</div>
          <h3 class="card-title">${escapeHtml(p.title)}</h3>
          <p class="card-description">${escapeHtml(cleanContent)}${p.content && p.content.length > 100 ? '...' : ''}</p>
          <div class="card-meta">
            <div class="meta-left">
              <span class="meta-item">조회 ${p.viewCount || 0}</span>
              <span class="meta-item">좋아요 ${p.likeCount || 0}</span>
            </div>
            <div class="meta-right">
              <span class="recommend-badge">추천</span>
            </div>
          </div>
          ${p.tags ? `<div class="card-tags">${formatTags(p.tags)}</div>` : ''}
        </div>
      `;
      
      article.addEventListener('click', () => {
        location.href = '/posts/' + p.id;
      });
      
      grid.appendChild(article);
    });

  } catch (e) {
    console.error('추천 로드 오류:', e);
    loadingState.style.display = 'none';
    emptyState.style.display = 'flex';
    emptyState.querySelector('h3').textContent = '추천을 불러오지 못했습니다';
    emptyState.querySelector('p').textContent = '잠시 후 다시 시도해주세요.';
  }
}

// HTML 이스케이프
function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

// HTML 태그 제거 (특히 <h2> 같은 태그)
function stripHtmlTags(html) {
  const div = document.createElement('div');
  div.innerHTML = html;
  return div.textContent || div.innerText || '';
}

// 태그 포맷팅
function formatTags(tags) {
  if (!tags) return '';
  const tagList = typeof tags === 'string' ? tags.split(',').map(t => t.trim()) : tags;
  return tagList.slice(0, 3).map(tag => 
    `<span class="tag">${escapeHtml(tag)}</span>`
  ).join('');
}

// 이벤트 리스너
document.getElementById('refreshBtn')?.addEventListener('click', () => {
  loadRecommendations();
});

// 초기 로드
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', loadRecommendations);
} else {
  loadRecommendations();
}
