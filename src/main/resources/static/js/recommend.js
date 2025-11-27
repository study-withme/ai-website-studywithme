// AI 추천 게시글 불러오기
async function loadRecommendations() {
  const grid = document.querySelector('.card-grid');
  if (!grid) return;

  try {
    const res = await fetch('/api/recommendations/posts?size=12');
    if (!res.ok) throw new Error('failed to fetch recommendations');
    const posts = await res.json();

    grid.innerHTML = '';

    if (!Array.isArray(posts) || posts.length === 0) {
      grid.innerHTML =
        '<p style="padding:40px;text-align:center;color:#6b7280;">아직 추천할 스터디가 충분하지 않습니다.</p>';
      return;
    }

    posts.forEach((p) => {
      const article = document.createElement('article');
      article.className = 'card';
      const thumbUrl = `https://picsum.photos/seed/${p.id}/800/500`;
      article.innerHTML = `
        <img src="${thumbUrl}" alt="">
        <div class="card-body">
          <h3>${p.title}</h3>
          <p>${(p.category || '기타')} · 좋아요 ${p.likeCount || 0}</p>
          <div class="meta">
            <span>${p.category || '기타'}</span>
            <span class="price">추천</span>
          </div>
        </div>
      `;
      article.addEventListener('click', () => {
        location.href = '/posts/' + p.id;
      });
      grid.appendChild(article);
    });
  } catch (e) {
    console.error(e);
    alert('추천을 불러오지 못했습니다.');
  }
}

document.getElementById('refreshBtn')?.addEventListener('click', () => {
  loadRecommendations();
});

// 초기 로드
loadRecommendations();
