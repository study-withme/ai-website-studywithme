/* ===== 배경 슬라이드 ===== */
const slides = document.querySelector('.slides');
let currentIndex = 0;
const totalSlides = document.querySelectorAll('.slide').length;
setInterval(() => {
  currentIndex = (currentIndex + 1) % totalSlides;
  slides.style.transform = `translateX(-${currentIndex * 100}%)`;
}, 4000);

/* ===== 다크모드 ===== */
const toggleDark = document.getElementById("toggleDark");
toggleDark.addEventListener("click", () => {
  document.body.classList.toggle("dark");
  toggleDark.textContent = document.body.classList.contains("dark")
    ? ""
    : "";
});

/* ===== 메시지 박스 ===== */
function showMessage(message, type = "success") {
  const box = document.createElement("div");
  box.className = `message-box ${type}`;
  box.textContent = message;
  document.body.appendChild(box);

  setTimeout(() => {
    box.style.opacity = "0";
    setTimeout(() => box.remove(), 500);
  }, 2500);
}

/* ===== 로그인 이벤트 ===== */
const loginForm = document.getElementById("loginForm");

loginForm.addEventListener("submit", (e) => {
  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!email || !password) {
    e.preventDefault();
    showMessage("이메일과 비밀번호를 모두 입력해주세요.", "error");
    return;
  }
});
