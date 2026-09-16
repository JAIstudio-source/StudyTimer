// ==========================================================================
// STUDYTIMER - CLIENT INTERACTIONS & ACCESSIBILITY (STUDENT EDITION)
// ==========================================================================

document.addEventListener('DOMContentLoaded', () => {
  initFaqAccordion();
  initUseCaseTabs();
  initMobileMenu();
  initSmoothScroll();
});

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
  const tabBtns = document.querySelectorAll('.tab-btn');
  const tabContents = document.querySelectorAll('.usecase-tab-content');

  tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');

      tabBtns.forEach(b => b.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      btn.classList.add('active');
      const targetEl = document.getElementById(`tab-${targetId}`);
      if (targetEl) targetEl.classList.add('active');
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
