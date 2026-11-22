// ==========================================================================
// STUDYTIMER — script.js v5.0.0
// Vanilla JS · No external libraries · Student-first
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  initMobileMenu();
  initSmoothScroll();
  initFaqAccordion();
  initScreenshotModal();
  initHeroTimer();
  initScrollReveal();
  initNavScroll();
  initCounterAnimations();
  initVersionSync();
  handleAutoDownloadParam();
});

// ==========================================================================
// THEME TOGGLE
// ==========================================================================

function initThemeToggle() {
  const toggleBtns = document.querySelectorAll('.theme-toggle-btn');
  const savedTheme = localStorage.getItem('studytimer-theme');
  const defaultTheme = document.documentElement.getAttribute('data-theme') || 'light';
  const activeTheme = savedTheme || defaultTheme;

  document.documentElement.setAttribute('data-theme', activeTheme);
  updateThemeAriaLabels(activeTheme, toggleBtns);
  updateThemeColorMeta(activeTheme);

  toggleBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem('studytimer-theme', next); } catch (_) {}
      updateThemeAriaLabels(next, toggleBtns);
      updateThemeColorMeta(next);
    });
  });
}

function updateThemeColorMeta(theme) {
  const metaTheme = document.querySelectorAll('meta[name="theme-color"]');
  const color = theme === 'dark' ? '#09090b' : '#f9f9f7';
  metaTheme.forEach(m => m.setAttribute('content', color));
}

function updateThemeAriaLabels(theme, btns) {
  const label = theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode';
  btns.forEach(btn => btn.setAttribute('aria-label', label));
}

// ==========================================================================
// MOBILE BOTTOM-SHEET DRAWER
// ==========================================================================

function initMobileMenu() {
  const menuBtn  = document.getElementById('mobileMenuBtn');
  const drawer   = document.getElementById('mobileDrawer');
  const backdrop = document.getElementById('mobileDrawerBackdrop');
  if (!menuBtn || !drawer) return;

  function openDrawer() {
    menuBtn.setAttribute('aria-expanded', 'true');
    drawer.classList.add('active');
    if (backdrop) backdrop.classList.add('active');
    document.body.style.overflow = 'hidden';
  }

  function closeDrawer() {
    menuBtn.setAttribute('aria-expanded', 'false');
    drawer.classList.remove('active');
    if (backdrop) backdrop.classList.remove('active');
    document.body.style.overflow = '';
  }

  menuBtn.addEventListener('click', () => {
    menuBtn.getAttribute('aria-expanded') === 'true' ? closeDrawer() : openDrawer();
  });

  if (backdrop) backdrop.addEventListener('click', closeDrawer);

  drawer.querySelectorAll('a').forEach(link => link.addEventListener('click', closeDrawer));

  // Close on Escape key
  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && drawer.classList.contains('active')) closeDrawer();
  });
}

// ==========================================================================
// SMOOTH SCROLL
// ==========================================================================

function initSmoothScroll() {
  document.querySelectorAll('a[href^="#"]').forEach(link => {
    link.addEventListener('click', e => {
      const href = link.getAttribute('href');
      if (!href || href === '#') return;
      const target = document.querySelector(href);
      if (target) {
        e.preventDefault();
        const navH = parseInt(getComputedStyle(document.documentElement).getPropertyValue('--nav-h')) || 68;
        const top = target.getBoundingClientRect().top + window.scrollY - navH - 16;
        window.scrollTo({ top, behavior: 'smooth' });
      }
    });
  });
}

// ==========================================================================
// NAV: SCROLL SHADOW EFFECT
// ==========================================================================

function initNavScroll() {
  const header = document.getElementById('navbar');
  if (!header) return;

  let ticking = false;
  window.addEventListener('scroll', () => {
    if (!ticking) {
      requestAnimationFrame(() => {
        header.classList.toggle('scrolled', window.scrollY > 60);
        ticking = false;
      });
      ticking = true;
    }
  }, { passive: true });
}

// ==========================================================================
// SCROLL REVEAL (IntersectionObserver)
// ==========================================================================

function initScrollReveal() {
  const items = document.querySelectorAll('.reveal-item');
  if (!items.length) return;

  // If user prefers reduced motion, skip animation (CSS handles it already)
  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (prefersReduced) {
    items.forEach(el => el.classList.add('revealed'));
    return;
  }

  const observer = new IntersectionObserver(
    entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.12, rootMargin: '0px 0px -40px 0px' }
  );

  items.forEach(el => observer.observe(el));

  // Hero items: reveal immediately
  document.querySelectorAll('.hero-section .reveal-item').forEach(el => {
    el.classList.add('revealed');
  });
}

// ==========================================================================
// COUNTER ANIMATION (trust strip numbers)
// ==========================================================================

function initCounterAnimations() {
  const counters = document.querySelectorAll('.trust-num[data-count]');
  if (!counters.length) return;

  const prefersReduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) return;
      const el = entry.target;
      const target = parseFloat(el.getAttribute('data-count'));
      const decimals = parseInt(el.getAttribute('data-decimals') || '0');
      observer.unobserve(el);

      if (prefersReduced) {
        el.textContent = target.toFixed(decimals);
        return;
      }

      const duration = 1200;
      const start = performance.now();

      function step(now) {
        const progress = Math.min((now - start) / duration, 1);
        // Ease-out cubic
        const eased = 1 - Math.pow(1 - progress, 3);
        el.textContent = (target * eased).toFixed(decimals);
        if (progress < 1) requestAnimationFrame(step);
      }

      requestAnimationFrame(step);
    });
  }, { threshold: 0.5 });

  counters.forEach(c => observer.observe(c));
}

// ==========================================================================
// FAQ ACCORDION (Pure Question-Only Default, Toggle On Click)
// ==========================================================================

function initFaqAccordion() {
  const items = document.querySelectorAll('.faq-item');

  items.forEach(item => {
    const btn = item.querySelector('.faq-question-btn');
    const ans = item.querySelector('.faq-answer');
    if (!btn || !ans) return;

    btn.addEventListener('click', () => {
      const isCurrentlyOpen = item.classList.contains('active');

      // Close all items first
      items.forEach(other => {
        other.classList.remove('active');
        const b = other.querySelector('.faq-question-btn');
        const a = other.querySelector('.faq-answer');
        if (b) b.setAttribute('aria-expanded', 'false');
        if (a) a.setAttribute('hidden', '');
      });

      // If it was closed before, open it now
      if (!isCurrentlyOpen) {
        item.classList.add('active');
        btn.setAttribute('aria-expanded', 'true');
        ans.removeAttribute('hidden');
      }
    });
  });
}

// ==========================================================================
// SCREENSHOT LIGHTBOX MODAL (Click to Enlarge App Screenshots)
// ==========================================================================

function initScreenshotModal() {
  const modal = document.getElementById('screenshotModal');
  const backdrop = document.getElementById('screenshotModalBackdrop');
  const closeBtn = document.getElementById('screenshotModalClose');
  const modalImg = document.getElementById('screenshotModalImg');
  const modalCaption = document.getElementById('screenshotModalCaption');
  const modalDialog = document.getElementById('screenshotModalDialog') || modal?.querySelector('.screenshot-modal-dialog');
  const triggers = document.querySelectorAll('[data-screenshot]');

  if (!modal || !modalImg || !triggers.length) return;

  function setOrientation(isLandscape) {
    if (modalDialog) {
      if (isLandscape) {
        modalDialog.classList.add('is-landscape');
      } else {
        modalDialog.classList.remove('is-landscape');
      }
    }
  }

  function openPreview(src, caption) {
    // Detect landscape from filename or wait for image load
    const isLandscapeName = /landscape/i.test(src);
    setOrientation(isLandscapeName);

    modalImg.onload = () => {
      const isLandscapeDimensions = modalImg.naturalWidth > modalImg.naturalHeight * 1.15;
      setOrientation(isLandscapeDimensions);
    };

    modalImg.src = src;
    if (modalCaption) modalCaption.textContent = caption || '';
    modal.removeAttribute('hidden');
    requestAnimationFrame(() => {
      modal.classList.add('is-open');
    });
    document.body.style.overflow = 'hidden';
    if (closeBtn) closeBtn.focus();
  }

  function closePreview() {
    modal.classList.remove('is-open');
    modal.classList.add('is-closing');
    document.body.style.overflow = '';
    setTimeout(() => {
      modal.classList.remove('is-closing');
      modal.setAttribute('hidden', '');
      modalImg.src = '';
      setOrientation(false);
    }, 250);
  }

  triggers.forEach(trigger => {
    function activate(e) {
      if (e.target.closest('button:not([data-screenshot])') || e.target.closest('a')) return;
      const src = trigger.getAttribute('data-screenshot');
      const caption = trigger.getAttribute('data-caption');
      if (src) {
        e.preventDefault();
        openPreview(src, caption);
      }
    }

    trigger.addEventListener('click', activate);
    trigger.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ' ') {
        activate(e);
      }
    });
  });

  if (closeBtn) closeBtn.addEventListener('click', closePreview);
  if (backdrop) backdrop.addEventListener('click', closePreview);

  document.addEventListener('keydown', e => {
    if (e.key === 'Escape' && !modal.hasAttribute('hidden')) {
      closePreview();
    }
  });
}

// ==========================================================================
// HERO TIMER (animated circular ring countdown)
// ==========================================================================

function initHeroTimer() {
  // The hero shows a phone screenshot — the timer ring lives inside the
  // phone mockup via SVG overlay if present. If the page has the SVG
  // timer elements (from previous version), animate them; otherwise skip.

  const toggleBtn    = document.getElementById('heroTimerToggleBtn');
  const digitsEl     = document.getElementById('heroTimerDigits');
  const stateCaption = document.querySelector('.clock-state-caption');
  const progressCircle = document.querySelector('.ring-active-circle');

  // These elements only exist if the HTML includes them (optional)
  if (!digitsEl) return;

  let isRunning   = true;
  let totalSecs   = 25 * 60;
  let secondsLeft = 24 * 60 + 58;
  let interval    = null;
  const totalDash = 578; // 2 * PI * 92

  function updateDisplay() {
    const m = Math.floor(secondsLeft / 60);
    const s = secondsLeft % 60;
    digitsEl.textContent = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;

    if (progressCircle) {
      const ratio  = (totalSecs - secondsLeft) / totalSecs;
      const offset = totalDash - ratio * totalDash;
      progressCircle.style.strokeDashoffset = offset;
    }
  }

  function tick() {
    if (secondsLeft > 0) {
      secondsLeft--;
      updateDisplay();
    } else {
      secondsLeft = 5 * 60;
      if (stateCaption) stateCaption.textContent = 'Rest Break';
    }
  }

  interval = setInterval(tick, 1000);
  updateDisplay();

  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      isRunning = !isRunning;
      const iconEl  = toggleBtn.querySelector('.toggle-icon-wrap');
      const labelEl = toggleBtn.querySelector('.toggle-label-text');

      if (isRunning) {
        if (iconEl)  iconEl.textContent  = '⏸';
        if (labelEl) labelEl.textContent = 'Pause Focus';
        if (stateCaption) stateCaption.textContent = 'Deep Focus Block';
        interval = setInterval(tick, 1000);
      } else {
        if (iconEl)  iconEl.textContent  = '▶';
        if (labelEl) labelEl.textContent = 'Resume Focus';
        if (stateCaption) stateCaption.textContent = 'Focus Paused';
        clearInterval(interval);
      }
    });
  }
}

// ==========================================================================
// AUTO-DOWNLOAD (URL param: ?download=true or ?download=apk)
// ==========================================================================

function handleAutoDownloadParam() {
  try {
    const params = new URLSearchParams(window.location.search);
    if (params.get('download') === 'true' || params.get('download') === 'apk') {
      setTimeout(() => {
        const link = document.createElement('a');
        link.href = 'https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/StudyTimer-release.apk';
        link.download = 'StudyTimer.apk';
        document.body.appendChild(link);
        link.click();
        link.remove();
      }, 400);
    }
  } catch (_) {}
}

// ==========================================================================
// DYNAMIC APP VERSION SYNC
// ==========================================================================

function initVersionSync() {
  const versionEl = document.getElementById('heroAppVersion');
  if (!versionEl) return;
  fetch('version.json')
    .then(res => res.ok ? res.json() : null)
    .then(data => {
      if (data && data.versionName) {
        versionEl.textContent = `v${data.versionName}`;
      }
    })
    .catch(() => {});
}

