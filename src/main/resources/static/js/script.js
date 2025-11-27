// 슬라이드 자동 전환
const slides = document.querySelector('.slides');
const dots = document.querySelectorAll('.dot');
let currentIndex = 0;

function showSlide(index) {
  slides.style.transform = `translateX(-${index * 100}%)`;
  dots.forEach((dot, i) => dot.classList.toggle('active', i === index));
}

function nextSlide() {
  currentIndex = (currentIndex + 1) % 3;
  showSlide(currentIndex);
}

let slideInterval = setInterval(nextSlide, 3500);
dots.forEach((dot, i) =>
  dot.addEventListener('click', () => {
    currentIndex = i;
    showSlide(i);
    clearInterval(slideInterval);
    slideInterval = setInterval(nextSlide, 3500);
  })
);
