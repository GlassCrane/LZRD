/* ============================================================
   LZRD INTHEKITCHEN — motion system
   GSAP + ScrollTrigger
   ============================================================ */

gsap.registerPlugin(ScrollTrigger, ScrollToPlugin);

const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
const isMobile = window.matchMedia("(max-width: 900px)").matches;

document.getElementById("year").textContent = "2026";

/* ------------------------------------------------------------
   Utility: split a line's text into per-char spans
------------------------------------------------------------ */
function splitChars(el) {
  const text = el.textContent;
  el.textContent = "";
  return [...text].map((ch) => {
    const span = document.createElement("span");
    span.className = "char";
    span.textContent = ch === " " ? " " : ch;
    el.appendChild(span);
    return span;
  });
}

/* ------------------------------------------------------------
   PRELOADER
------------------------------------------------------------ */
function runPreloader() {
  const pre = document.getElementById("preloader");
  const countEl = document.getElementById("count");
  const fill = document.querySelector(".preloader__fill");
  const curtain = document.querySelector(".preloader__curtain");

  if (reduceMotion) {
    pre.style.display = "none";
    startHero();
    return;
  }

  const counter = { v: 0 };
  const tl = gsap.timeline();

  tl.to(counter, {
    v: 100,
    duration: 2.2,
    ease: "power2.inOut",
    onUpdate: () => {
      const val = Math.round(counter.v);
      countEl.textContent = val;
      fill.style.width = val + "%";
    },
  })
    .to(".preloader__inner", { y: -30, opacity: 0, duration: 0.5, ease: "power2.in" }, "+=0.15")
    .set(curtain, { zIndex: 2 })
    .to(curtain, {
      y: "0%",
      duration: 0.9,
      ease: "expo.inOut",
    }, "-=0.1")
    .set(pre, { display: "none" })
    .add(startHero, "-=0.2");
}

/* ------------------------------------------------------------
   HERO INTRO
------------------------------------------------------------ */
function startHero() {
  const logo = document.querySelector(".hero__logo");
  const fades = gsap.utils.toArray(".hero [data-fade]");

  if (reduceMotion) return;

  const tl = gsap.timeline({ defaults: { ease: "expo.out" } });

  // Logo slides up from behind its own mask
  tl.from(logo, { yPercent: 120, duration: 1.3 });

  tl.from(
    fades,
    { y: 24, opacity: 0, duration: 0.9, stagger: 0.12, ease: "power3.out" },
    "-=0.7"
  );

  // Blobs drift forever
  gsap.to(".blob--1", { x: 60, y: -40, scale: 1.15, duration: 9, yoyo: true, repeat: -1, ease: "sine.inOut" });
  gsap.to(".blob--2", { x: -70, y: 50, scale: 1.1, duration: 11, yoyo: true, repeat: -1, ease: "sine.inOut" });
  gsap.to(".blob--3", { x: 40, y: -30, scale: 1.2, duration: 8, yoyo: true, repeat: -1, ease: "sine.inOut" });

  // Parallax hero title on scroll
  gsap.to(".hero__title", {
    yPercent: 30,
    ease: "none",
    scrollTrigger: { trigger: ".hero", start: "top top", end: "bottom top", scrub: true },
  });
}

/* ------------------------------------------------------------
   CUSTOM CURSOR + MAGNETIC
------------------------------------------------------------ */
function initCursor() {
  if (isMobile) return;
  const ring = document.querySelector(".cursor");
  const dot = document.querySelector(".cursor-dot");

  const rx = gsap.quickTo(ring, "x", { duration: 0.5, ease: "power3" });
  const ry = gsap.quickTo(ring, "y", { duration: 0.5, ease: "power3" });
  const dx = gsap.quickTo(dot, "x", { duration: 0.12, ease: "power3" });
  const dy = gsap.quickTo(dot, "y", { duration: 0.12, ease: "power3" });

  window.addEventListener("mousemove", (e) => {
    rx(e.clientX); ry(e.clientY);
    dx(e.clientX); dy(e.clientY);
  });

  document.querySelectorAll("a, button, .magnetic, input").forEach((el) => {
    el.addEventListener("mouseenter", () => ring.classList.add("is-hover"));
    el.addEventListener("mouseleave", () => ring.classList.remove("is-hover"));
  });

  // Magnetic pull
  document.querySelectorAll(".magnetic").forEach((el) => {
    const strength = 0.4;
    el.addEventListener("mousemove", (e) => {
      const r = el.getBoundingClientRect();
      const mx = e.clientX - (r.left + r.width / 2);
      const my = e.clientY - (r.top + r.height / 2);
      gsap.to(el, { x: mx * strength, y: my * strength, duration: 0.6, ease: "power3.out" });
    });
    el.addEventListener("mouseleave", () => {
      gsap.to(el, { x: 0, y: 0, duration: 0.6, ease: "elastic.out(1, 0.4)" });
    });
  });
}

/* ------------------------------------------------------------
   TRACKING EYEBALL
------------------------------------------------------------ */
function initEye() {
  const unit = document.querySelector(".lizardwrap .eye"); // eye socket, for centering
  const ball = document.getElementById("eyeball"); // iris + pupil, moves
  const lid = document.getElementById("eyelid");
  if (!unit || !ball) return;

  const MAX = 16; // max iris travel, in the eye's 100-unit viewBox

  const moveX = gsap.quickTo(ball, "x", { duration: 0.35, ease: "power3" });
  const moveY = gsap.quickTo(ball, "y", { duration: 0.35, ease: "power3" });

  function look(clientX, clientY) {
    const r = unit.getBoundingClientRect();
    const cx = r.left + r.width / 2;
    const cy = r.top + r.height / 2;
    const dx = clientX - cx;
    const dy = clientY - cy;
    const dist = Math.hypot(dx, dy) || 1;
    // ease in: barely moves when pointer is near, fully engaged when far
    const engage = Math.min(dist / 240, 1);
    moveX((dx / dist) * MAX * engage);
    moveY((dy / dist) * MAX * engage);
  }

  window.addEventListener("mousemove", (e) => look(e.clientX, e.clientY));
  window.addEventListener("touchmove", (e) => {
    if (e.touches[0]) look(e.touches[0].clientX, e.touches[0].clientY);
  }, { passive: true });

  // Blink on a loose interval (lid drops over the socket and lifts)
  if (!reduceMotion && lid) {
    const blink = () => {
      gsap.timeline()
        .to(lid, { attr: { y: 0 }, duration: 0.08, ease: "power2.in" })
        .to(lid, { attr: { y: -100 }, duration: 0.14, ease: "power2.out", delay: 0.02 });
      const next = 2500 + Math.random() * 4000;
      setTimeout(blink, next);
    };
    setTimeout(blink, 2000);
  }
}

/* ------------------------------------------------------------
   MARQUEE — velocity reactive
------------------------------------------------------------ */
function initMarquee() {
  const track = document.getElementById("marquee");
  if (!track) return;

  let direction = 1;
  const baseSpeed = 0.06; // % of half-width per frame baseline via xPercent tween

  const loop = gsap.to(track, {
    xPercent: -50,
    repeat: -1,
    duration: 22,
    ease: "none",
  });

  // Scroll velocity nudges speed + direction
  ScrollTrigger.create({
    trigger: document.body,
    start: "top top",
    end: "bottom bottom",
    onUpdate: (self) => {
      const v = self.getVelocity();
      direction = v < 0 ? -1 : 1;
      const boost = gsap.utils.clamp(1, 6, 1 + Math.abs(v) / 400);
      loop.timeScale(direction * boost);
      gsap.to(loop, { timeScale: direction * 1, duration: 0.8, overwrite: true, delay: 0.05 });
    },
  });
}

/* ------------------------------------------------------------
   DROP — horizontal pinned scroll
------------------------------------------------------------ */
function initDrop() {
  const panels = document.getElementById("dropPanels");
  const pin = document.getElementById("dropPin");
  if (!panels) return;

  const getScrollAmount = () => panels.scrollWidth - window.innerWidth + window.innerWidth * 0.12;

  const tween = gsap.to(panels, {
    x: () => -getScrollAmount(),
    ease: "none",
  });

  ScrollTrigger.create({
    trigger: pin,
    start: "top top",
    end: () => "+=" + getScrollAmount(),
    pin: true,
    animation: tween,
    scrub: 1,
    invalidateOnRefresh: true,
    anticipatePin: 1,
  });

  // Panels rise as they enter
  gsap.utils.toArray(".panel").forEach((p) => {
    gsap.from(p, {
      opacity: 0,
      scale: 0.9,
      duration: 0.6,
      ease: "power2.out",
      scrollTrigger: { trigger: p, containerAnimation: tween, start: "left 92%" },
    });
  });
}

/* ------------------------------------------------------------
   MANIFESTO — word ignition
------------------------------------------------------------ */
function initManifesto() {
  const words = gsap.utils.toArray(".manifesto__text [data-word]");
  if (!words.length) return;

  const colors = ["#c6ff00", "#ff2d95", "#00e5ff", "#ff6a00", "#7b2dff", "#f4f1ea"];

  gsap.to(words, {
    color: (i) => colors[i % colors.length],
    stagger: 1,
    ease: "none",
    scrollTrigger: {
      trigger: ".manifesto",
      start: "top 70%",
      end: "bottom 75%",
      scrub: true,
    },
  });

  // Settle everything to ink at the end so it reads clean
  gsap.to(words, {
    color: "#f4f1ea",
    scrollTrigger: { trigger: ".manifesto", start: "bottom 60%", end: "bottom 30%", scrub: true },
  });
}

/* ------------------------------------------------------------
   LOOKBOOK — parallax
------------------------------------------------------------ */
function initLookbook() {
  if (reduceMotion) return;
  gsap.utils.toArray(".shot").forEach((shot) => {
    const speed = parseFloat(shot.dataset.speed) || 1;
    gsap.fromTo(
      shot,
      { yPercent: -(speed - 1) * 40 },
      {
        yPercent: (speed - 1) * 40,
        ease: "none",
        scrollTrigger: { trigger: ".lookbook__grid", start: "top bottom", end: "bottom top", scrub: true },
      }
    );
  });

  gsap.from(".lookbook__head [data-fade]", {
    y: 40, opacity: 0, duration: 1, stagger: 0.15, ease: "power3.out",
    scrollTrigger: { trigger: ".lookbook", start: "top 75%" },
  });
}

/* ------------------------------------------------------------
   GENERIC FADE-UPS (waitlist etc.)
------------------------------------------------------------ */
function initFades() {
  gsap.utils.toArray(".waitlist [data-fade]").forEach((el) => {
    gsap.from(el, {
      y: 40, opacity: 0, duration: 1, ease: "power3.out",
      scrollTrigger: { trigger: el, start: "top 85%" },
    });
  });

  // Footer logo reveal
  gsap.from("#footerLogo", {
    scale: 1.15, opacity: 0, duration: 1.2, ease: "power3.out",
    scrollTrigger: { trigger: ".footer", start: "top 80%" },
  });
}

/* ------------------------------------------------------------
   NAV + SMOOTH ANCHORS + MOBILE MENU
------------------------------------------------------------ */
function initNav() {
  const burger = document.getElementById("burger");
  const menu = document.getElementById("menu");

  burger.addEventListener("click", () => {
    burger.classList.toggle("is-open");
    menu.classList.toggle("is-open");
  });

  document.querySelectorAll("[data-link]").forEach((link) => {
    link.addEventListener("click", (e) => {
      const id = link.getAttribute("href");
      if (id && id.startsWith("#")) {
        e.preventDefault();
        const target = document.querySelector(id);
        if (target) {
          burger.classList.remove("is-open");
          menu.classList.remove("is-open");
          gsap.to(window, { scrollTo: { y: target, autoKill: false }, duration: 1.1, ease: "power3.inOut" });
        }
      }
    });
  });
}

/* ------------------------------------------------------------
   WAITLIST FORM
------------------------------------------------------------ */
function initForm() {
  const form = document.getElementById("waitForm");
  const note = document.getElementById("waitNote");
  if (!form) return;
  form.addEventListener("submit", (e) => {
    e.preventDefault();
    const input = form.querySelector("input");
    note.textContent = "You're on the list. Watch your inbox for the cook. ♛";
    note.classList.add("is-done");
    input.value = "";
    input.blur();
    gsap.fromTo(note, { y: 8, opacity: 0.4 }, { y: 0, opacity: 1, duration: 0.5, ease: "power2.out" });
  });
}

/* ------------------------------------------------------------
   BOOT
------------------------------------------------------------ */
let booted = false;
function boot() {
  if (booted) return;
  booted = true;

  initNav();
  initCursor();
  initEye();
  initMarquee();
  initDrop();
  initManifesto();
  initLookbook();
  initFades();
  initForm();
  runPreloader();

  ScrollTrigger.refresh();
}

// Boot on full load for accurate layout, but never let a slow/blocked
// font or asset request hang the preloader — fall back shortly after DOM ready.
window.addEventListener("load", boot);
document.addEventListener("DOMContentLoaded", () => setTimeout(boot, 1200));

window.addEventListener("resize", () => ScrollTrigger.refresh());
