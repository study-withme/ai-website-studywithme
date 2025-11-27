// 배경 슬라이드
const slides = document.querySelector('.slides');
let current = 0;
const total = document.querySelectorAll('.slide').length;
setInterval(() => {
  current = (current + 1) % total;
  slides.style.transform = `translateX(-${current * 100}%)`;
}, 4000);

// AI 로딩 → 메인 등장
const loading = document.getElementById('loadingScreen');
const main = document.getElementById('mainContent');
setTimeout(() => {
  main.style.transition = 'opacity 1s';
  main.style.opacity = 1;
  loading.style.display = 'none';
}, 2500);

// 카테고리 선택
const cards = document.querySelectorAll('.category-card');
cards.forEach(card => {
  card.addEventListener('click', () => {
    card.classList.toggle('selected');
  });
});

// 다크모드
const toggleDark = document.getElementById('toggleDark');
toggleDark.addEventListener('click', () => {
  document.body.classList.toggle('dark');
  toggleDark.textContent = document.body.classList.contains('dark')
    ? "☀️ 라이트모드"
    : "🌙 다크모드";
});

// 선택 완료
const btn = document.getElementById('submitBtn');
btn.addEventListener('click', () => {
  const selected = [...document.querySelectorAll('.selected')].map(c => c.textContent.trim());
  if (selected.length === 0) {
    alert("관심 분야를 하나 이상 선택해주세요 😊");
    return;
  }
  
  // 선택한 카테고리를 서버에 전송
  const form = document.createElement('form');
  form.method = 'POST';
  form.action = '/ai/complete';
  
  // 선택한 카테고리를 hidden input으로 추가
  selected.forEach((category, index) => {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'categories';
    input.value = category;
    form.appendChild(input);
  });
  
  document.body.appendChild(form);
  form.submit();
});
