/* ===== 배경 슬라이드 ===== */
const slides = document.querySelector('.slides');
let current = 0;
const total = document.querySelectorAll('.slide').length;

setInterval(() => {
  current = (current + 1) % total;
  slides.style.transform = `translateX(-${current * 100}%)`;
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

/* ===== 회원가입 검증 ===== */
const form = document.getElementById("registerForm");

form.addEventListener("submit", (e) => {
  const pw = document.getElementById("password").value;
  const confirm = document.getElementById("confirm").value;

  if (pw !== confirm) {
    e.preventDefault();
    showMessage("비밀번호가 일치하지 않습니다.", "error");
    return;
  }
});
