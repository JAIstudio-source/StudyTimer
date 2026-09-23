// ==========================================================================
// STUDYTIMER - CLIENT INTERACTIONS & ACCESSIBILITY (STUDENT EDITION)
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  initFaqAccordion();
  initUseCaseTabs();
  initMobileMenu();
  initSmoothScroll();
  initDynamicVersion();
  initServiceWorkerSync();
});

/**
 * Theme Toggle (Light / Dark Mode)
 */
function initThemeToggle() {
  const toggleButtons = document.querySelectorAll('.theme-toggle-btn');
  
  // Read current theme from document attribute or localStorage (default light)
  const savedTheme = localStorage.getItem('studytimer-theme');
  const activeTheme = savedTheme || document.documentElement.getAttribute('data-theme') || 'light';
  document.documentElement.setAttribute('data-theme', activeTheme);

  toggleButtons.forEach(btn => {
    btn.setAttribute('aria-label', `Switch to ${activeTheme === 'dark' ? 'light' : 'dark'} mode`);
    
    btn.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme') || 'light';
      const targetTheme = current === 'dark' ? 'light' : 'dark';
      
      document.documentElement.setAttribute('data-theme', targetTheme);
      try {
        localStorage.setItem('studytimer-theme', targetTheme);
      } catch (e) {
        // localStorage error handling
      }
      
      toggleButtons.forEach(b => {
        b.setAttribute('aria-label', `Switch to ${targetTheme === 'dark' ? 'light' : 'dark'} mode`);
      });
    });
  });
}

/**
 * Accessible FAQ Accordion
 */
function initFaqAccordion() {
  const faqItems = document.querySelectorAll('.faq-item');

  faqItems.forEach(item => {
    const questionBtn = item.querySelector('.faq-question');
    if (!questionBtn) return;

    questionBtn.addEventListener('click', () => {
      const isCurrentlyActive = item.classList.contains('active');

      // Close other items
      faqItems.forEach(otherItem => {
        otherItem.classList.remove('active');
        const btn = otherItem.querySelector('.faq-question');
        if (btn) btn.setAttribute('aria-expanded', 'false');
      });

      // Toggle current item
      if (!isCurrentlyActive) {
        item.classList.add('active');
        questionBtn.setAttribute('aria-expanded', 'true');
      }
    });
  });
}

/**
 * Use Case Tabs for Students
 */
function initUseCaseTabs() {
  const tabBtns = Array.from(document.querySelectorAll('.tab-btn'));
  const tabContents = document.querySelectorAll('.usecase-tab-content');

  function activateTab(btn, setFocus = false) {
    const targetId = btn.getAttribute('data-target');

    tabBtns.forEach(b => {
      b.classList.remove('active');
      b.setAttribute('aria-selected', 'false');
      b.setAttribute('tabindex', '-1');
    });

    tabContents.forEach(c => c.classList.remove('active'));

    btn.classList.add('active');
    btn.setAttribute('aria-selected', 'true');
    btn.setAttribute('tabindex', '0');
    if (setFocus) btn.focus();

    const targetEl = document.getElementById(`tab-${targetId}`);
    if (targetEl) targetEl.classList.add('active');
  }

  tabBtns.forEach((btn, index) => {
    btn.addEventListener('click', () => {
      activateTab(btn);
    });

    btn.addEventListener('keydown', (e) => {
      let nextIndex = null;
      if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
        nextIndex = (index + 1) % tabBtns.length;
      } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
        nextIndex = (index - 1 + tabBtns.length) % tabBtns.length;
      } else if (e.key === 'Home') {
        nextIndex = 0;
      } else if (e.key === 'End') {
        nextIndex = tabBtns.length - 1;
      }

      if (nextIndex !== null) {
        e.preventDefault();
        activateTab(tabBtns[nextIndex], true);
      }
    });
  });
}

/**
 * Mobile Navigation Drawer
 */
function initMobileMenu() {
  const menuBtn = document.getElementById('mobileMenuBtn');
  const drawer = document.getElementById('mobileDrawer');
  const links = document.querySelectorAll('.mobile-nav-link, .mobile-nav-buttons a');

  if (menuBtn && drawer) {
    menuBtn.addEventListener('click', () => {
      drawer.classList.toggle('open');
    });

    links.forEach(link => {
      link.addEventListener('click', () => {
        drawer.classList.remove('open');
      });
    });
  }
}

/**
 * Smooth scrolling for anchors
 */
function initSmoothScroll() {
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      const targetId = this.getAttribute('href');
      if (targetId && targetId !== '#') {
        const targetElement = document.querySelector(targetId);
        if (targetElement) {
          e.preventDefault();
          targetElement.scrollIntoView({
            behavior: 'smooth'
          });
        }
      }
    });
  });
}

/**
 * Dynamic Version & Latest Release Loader
 * Always fetches the fresh version.json bypassing caches so website is 100% up-to-date.
 */
function initDynamicVersion() {
  fetch('/version.json?_t=' + Date.now(), { cache: 'no-store' })
    .then(res => res.json())
    .then(data => {
      if (data && data.versionName) {
        const vTag = 'v' + data.versionName;
        // Update all version badges
        document.querySelectorAll('.apk-version-badge, .version-text, [data-version-badge]').forEach(el => {
          el.textContent = vTag;
        });
        // Update download buttons
        document.querySelectorAll('.btn-apk-download, .apk-download-nav').forEach(btn => {
          btn.setAttribute('title', `Download StudyTimer ${vTag} (Latest Release)`);
        });
      }
    })
    .catch(() => {});
}

/**
 * Service Worker Update & Refresh Detector
 */
function initServiceWorkerSync() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').then(reg => {
        reg.update();
        reg.addEventListener('updatefound', () => {
          const newWorker = reg.installing;
          if (newWorker) {
            newWorker.addEventListener('statechange', () => {
              if (newWorker.state === 'installed' && navigator.serviceWorker.controller) {
                newWorker.postMessage({ type: 'SKIP_WAITING' });
              }
            });
          }
        });
      }).catch(() => {});

      let refreshing = false;
      navigator.serviceWorker.addEventListener('controllerchange', () => {
        if (!refreshing) {
          refreshing = true;
          window.location.reload();
        }
      });
    });
  }
}
