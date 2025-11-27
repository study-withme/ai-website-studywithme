// lightbox.js
// 캐러셀 이미지 전체화면 확대 보기

document.addEventListener("DOMContentLoaded", () => {
  const lightbox = document.getElementById("lightbox");
  if (!lightbox) return; // board-detail이 아닐 때는 패스

  const lightboxImg = document.getElementById("lightboxImg");
  const carouselInner = document.getElementById("carouselInner");

  if (!lightboxImg || !carouselInner) return;

  // 캐러셀 이미지 목록
  const images = Array.from(carouselInner.querySelectorAll("img"));
  let currentIndex = 0;

  // 이미지 클릭 → 라이트박스 오픈
  images.forEach((img, index) => {
    img.style.cursor = "zoom-in";
    img.addEventListener("click", () => {
      openLightbox(index);
    });
  });

  function openLightbox(index) {
    currentIndex = index;
    const src = images[index].getAttribute("src");
    lightboxImg.src = src;
    lightbox.classList.remove("hidden");
    lightboxImg.classList.add("lightbox-open");
    setTimeout(() => lightboxImg.classList.remove("lightbox-open"), 200);
  }

  function closeLightbox() {
    lightbox.classList.add("hidden");
  }

  function showNext(delta) {
    if (!images.length) return;
    currentIndex = (currentIndex + delta + images.length) % images.length;
    lightboxImg.src = images[currentIndex].getAttribute("src");
  }

  // 라이트박스 아무 곳이나 클릭 → 닫기
  lightbox.addEventListener("click", () => {
    closeLightbox();
  });

  // 키보드 조작 (좌/우/ESC)
  document.addEventListener("keydown", (e) => {
    if (lightbox.classList.contains("hidden")) return;

    if (e.key === "Escape") {
      closeLightbox();
    } else if (e.key === "ArrowRight") {
      showNext(1);
    } else if (e.key === "ArrowLeft") {
      showNext(-1);
    }
  });
});

