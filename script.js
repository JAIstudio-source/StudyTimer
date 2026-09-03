document.addEventListener('DOMContentLoaded', () => {
  initServiceWorker();
  preloadScreenshotAssets();
  initHeroPhoneSwitching();
  initAndroidShowcaseTabs();
  initStudyFaqAccordion();
  initModalTriggers();
  initStickyAndroidCta();
  initClipboardHelpers();
  initMobileDrawer();
  initTouchSwiping();
});

/* --------------------------------------------------------------------------
   Service Worker Registration & Instant Live Cache Update
   -------------------------------------------------------------------------- */
function initServiceWorker() {
  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('./sw.js').then((reg) => {
        // Force check for fresh code on every page load
        reg.update();
      }).catch(() => {});
    });
  }
}

/* --------------------------------------------------------------------------
   Async Image Pre-Decoder for 120fps Smooth Scrolling & Instant Transitions
   -------------------------------------------------------------------------- */
function preloadScreenshotAssets() {
  const screenshots = [
    'assets/screenshots/Pomodoro_timer_Screen.jpg',
    'assets/screenshots/Pomodoro-settings__screen.jpg',
    'assets/screenshots/Stopwatch_paused_screen.jpg',
    'assets/screenshots/Custom_subject_screen.jpg',
    'assets/screenshots/Insight_screen_1.jpg',
    'assets/screenshots/Insight_screen_3.jpg',
    'assets/screenshots/Habit-Goal_screen.jpg',
    'assets/screenshots/Habit-Goal_histroy_screen.jpg',
    'assets/screenshots/Goal_calendar_screen.jpg',
    'assets/screenshots/Cloud_sync_setting_screen.jpg'
  ];

  screenshots.forEach(src => {
    const img = new Image();
    img.decoding = 'async';
    img.src = src;
  });
}

/* --------------------------------------------------------------------------
   0. Interactive 9-Screen Continuous Wheel & Dynamic Detail Card
   -------------------------------------------------------------------------- */
function initHeroPhoneSwitching() {
  const leftPhone = document.getElementById('heroPhoneLeft');
  const rightPhone = document.getElementById('heroPhoneRight');
  const imgLeft = document.getElementById('heroImgLeft');
  const imgCenter = document.getElementById('heroImgCenter');
  const imgRight = document.getElementById('heroImgRight');

  const wheelBadge = document.getElementById('wheelBadge');
  const wheelTitle = document.getElementById('wheelTitle');
  const wheelDesc = document.getElementById('wheelDesc');
  const prevBtn = document.getElementById('wheelPrevBtn');
  const nextBtn = document.getElementById('wheelNextBtn');
  const dots = document.querySelectorAll('.wheel-dot');

  if (!leftPhone || !rightPhone || !imgLeft || !imgCenter || !imgRight) return;

  const screens = [
    {
      src: 'assets/screenshots/Pomodoro_timer_Screen.jpg',
      alt: 'StudyTimer Active Pomodoro Focus Timer Interface on Android',
      badge: '1 of 9 • Focus Engine',
      title: 'Active Pomodoro Focus Engine',
      desc: 'Real-time countdown dial with ambient soundscapes, customizable work cycles, and distraction-free visual stamina feedback.'
    },
    {
      src: 'assets/screenshots/Pomodoro-settings__screen.jpg',
      alt: 'StudyTimer Pomodoro Custom Settings on Android',
      badge: '2 of 9 • Interval Settings',
      title: 'Configurable Focus & Break Intervals',
      desc: 'Set custom work blocks (25m/50m), short break durations, long recovery cycles, and auto-start session preferences.'
    },
    {
      src: 'assets/screenshots/Stopwatch_paused_screen.jpg',
      alt: 'StudyTimer Precision Stopwatch Mode on Android',
      badge: '3 of 9 • Stopwatch Mode',
      title: 'Untimed Focus & Lap Logging',
      desc: 'Open-ended time tracking for unbounded problem-solving sets, untimed exam drills, and continuous study marathons.'
    },
    {
      src: 'assets/screenshots/Custom_subject_screen.jpg',
      alt: 'StudyTimer Custom Subject Screen on Android',
      badge: '4 of 9 • Custom Subjects',
      title: 'Custom Course Subjects & Targets',
      desc: 'Create unlimited course tags with hex color palettes, set daily target hours, and track real-time completion progress.'
    },
    {
      src: 'assets/screenshots/Insight_screen_1.jpg',
      alt: 'StudyTimer Focus Analytics on Android',
      badge: '5 of 9 • Analytics Depth',
      title: 'Comprehensive Study Insights',
      desc: 'Detailed overview of total focus hours, subject distribution ratios, and weekly stamina growth trends.'
    },
    {
      src: 'assets/screenshots/Insight_screen_3.jpg',
      alt: 'StudyTimer Time Heatmaps on Android',
      badge: '6 of 9 • Time Heatmaps',
      title: 'Hour-by-Hour Focus Heatmaps',
      desc: 'Granular breakdown of focused study time versus recovery intervals across day and night periods.'
    },
    {
      src: 'assets/screenshots/Habit-Goal_screen.jpg',
      alt: 'StudyTimer Daily Habits Checklist on Android',
      badge: '7 of 9 • Habit Checklist',
      title: 'Daily Accountability & Task Goals',
      desc: 'Check off daily study routines, flashcard revisions, textbook chapters, and maintain consecutive day streaks.'
    },
    {
      src: 'assets/screenshots/Goal_calendar_screen.jpg',
      alt: 'StudyTimer Monthly Streak Calendar on Android',
      badge: '8 of 9 • Streak Calendar',
      title: 'Monthly Streak Calendar',
      desc: 'Visual calendar with daily flame indicators showing consecutive day consistency and monthly study totals.'
    },
    {
      src: 'assets/screenshots/Cloud_sync_setting_screen.jpg',
      alt: 'StudyTimer Google Cloud Sync on Android',
      badge: '9 of 9 • Cloud & Safety',
      title: 'Cloud Backup & Data Privacy',
      desc: 'Encrypted Supabase PostgreSQL database sync, automatic Google account backup, and instant data export.'
    }
  ];

  let centerIndex = 0;
  const total = screens.length;

  function updateWheel(newIndex) {
    centerIndex = (newIndex + total) % total;
    const leftIdx = (centerIndex - 1 + total) % total;
    const rightIdx = (centerIndex + 1) % total;

    // Smooth transition
    [imgLeft, imgCenter, imgRight].forEach(img => {
      img.style.opacity = '0.35';
      img.style.transition = 'opacity 180ms ease';
    });

    if (wheelTitle && wheelDesc) {
      wheelTitle.style.opacity = '0.2';
      wheelDesc.style.opacity = '0.2';
    }

    setTimeout(() => {
      imgLeft.src = screens[leftIdx].src;
      imgLeft.alt = screens[leftIdx].alt;

      imgCenter.src = screens[centerIndex].src;
      imgCenter.alt = screens[centerIndex].alt;

      imgRight.src = screens[rightIdx].src;
      imgRight.alt = screens[rightIdx].alt;

      if (wheelBadge) wheelBadge.textContent = screens[centerIndex].badge;
      if (wheelTitle) wheelTitle.textContent = screens[centerIndex].title;
      if (wheelDesc) wheelDesc.textContent = screens[centerIndex].desc;

      dots.forEach((dot, i) => {
        dot.classList.toggle('active', i === centerIndex);
      });

      [imgLeft, imgCenter, imgRight].forEach(img => {
        img.style.opacity = '1';
      });

      if (wheelTitle && wheelDesc) {
        wheelTitle.style.opacity = '1';
        wheelDesc.style.opacity = '1';
      }
    }, 150);
  }

  leftPhone.addEventListener('click', () => updateWheel(centerIndex - 1));
  rightPhone.addEventListener('click', () => updateWheel(centerIndex + 1));
  if (prevBtn) prevBtn.addEventListener('click', () => updateWheel(centerIndex - 1));
  if (nextBtn) nextBtn.addEventListener('click', () => updateWheel(centerIndex + 1));

  dots.forEach(dot => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      updateWheel(idx);
    });
  });

  // Touch gesture swipe on hero phones
  const heroStage = document.getElementById('heroPhoneStage');
  if (heroStage) {
    let touchStartX = 0;
    let touchEndX = 0;
    heroStage.addEventListener('touchstart', (e) => {
      touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });
    heroStage.addEventListener('touchend', (e) => {
      touchEndX = e.changedTouches[0].screenX;
      const diff = touchEndX - touchStartX;
      if (Math.abs(diff) > 40) {
        if (diff < 0) updateWheel(centerIndex + 1);
        else updateWheel(centerIndex - 1);
      }
    }, { passive: true });
  }

  // Accessible keyboard navigation
  [leftPhone, rightPhone, prevBtn, nextBtn].filter(Boolean).forEach(el => {
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        el.click();
      }
    });
  });

  // Initialize screen 0
  updateWheel(0);
}

/* --------------------------------------------------------------------------
   1. Interactive Android Showcase Persistent Stage
   -------------------------------------------------------------------------- */
const showcaseSlides = [
  {
    img: 'assets/screenshots/Pomodoro-settings__screen.jpg',
    alt: 'StudyTimer Pomodoro Custom Settings on Android',
    title: 'Pomodoro Interval Customization',
    desc: 'Fine-tune every aspect of your focus sessions: adjust work blocks (25/50m), break durations, auto-start cycles, and ambient sound options.',
    b1: 'Customizable work & break interval durations',
    b2: 'Ambient soundscapes and vibration feedback',
    ctaText: 'Try Pomodoro Mode'
  },
  {
    img: 'assets/screenshots/Stopwatch_paused_screen.jpg',
    alt: 'StudyTimer Paused Stopwatch Mode on Android',
    title: 'Unconstrained Focus Stopwatch',
    desc: 'For open-ended deep work sessions or untimed problem-solving drills. Seamless pause, lap logging, and instant subject tag switching.',
    b1: 'Real-time session timekeeping with haptic cues',
    b2: 'Quick resume and automated background persistence',
    ctaText: 'Try Stopwatch Mode'
  },
  {
    img: 'assets/screenshots/Custom_subject_screen.jpg',
    alt: 'StudyTimer Custom Subject Screen on Android',
    title: 'Custom Subject & Tag Creation',
    desc: 'Organize multiple study streams effortlessly. Assign custom color palettes, configure daily targets, and manage subject priority in one clean view.',
    b1: 'Unlimited custom subject tags with hex colors',
    b2: 'Real-time completion percentage tracking',
    ctaText: 'Manage Subjects'
  },
  {
    img: 'assets/screenshots/Insight_screen_3.jpg',
    alt: 'StudyTimer Deep Focus Analytics on Android',
    title: 'Hour-by-Hour Study Heatmaps',
    desc: 'Granular breakdown of your daily study distribution. Identify your cognitive prime times and compare weekly study stamina gains with visual graphs.',
    b1: 'Detailed breakdown of focused time vs. break periods',
    b2: 'Custom time range filters (7 Days, 30 Days, All Time)',
    ctaText: 'View Analytics'
  },
  {
    img: 'assets/screenshots/Habit-Goal_histroy_screen.jpg',
    alt: 'StudyTimer Habit History and Goal Progress on Android',
    title: 'Daily Habits & Goal History',
    desc: 'Track secondary study habits alongside your timers: review flashcards, solve mock exam papers, read textbook chapters, and maintain streak history.',
    b1: 'Daily routine check-offs with streak protection',
    b2: 'Archived session logs for review and audits',
    ctaText: 'Build Study Habits'
  },
  {
    img: 'assets/screenshots/Cloud_sync_setting_screen.jpg',
    alt: 'StudyTimer Cloud Sync and Google Sign-in on Android',
    title: 'Google Cloud Sync & Backup',
    desc: 'Enjoy offline freedom with optional cloud synchronization. Sign in with Google to automatically back up your study streaks, subjects, and analytics across Android devices.',
    b1: 'Encrypted Supabase PostgreSQL database sync',
    b2: 'One-click data export and permanent account deletion',
    ctaText: 'Download StudyTimer'
  }
];

let currentShowcaseIndex = 0;

function initAndroidShowcaseTabs() {
  const tabButtons = document.querySelectorAll('.stage-tab-btn');
  const dots = document.querySelectorAll('.showcase-dot');

  tabButtons.forEach((button, index) => {
    button.addEventListener('click', () => {
      switchShowcaseSlide(index);
    });
  });

  dots.forEach((dot) => {
    dot.addEventListener('click', () => {
      const idx = parseInt(dot.getAttribute('data-index'), 10);
      switchShowcaseSlide(idx);
    });
  });
}

function switchShowcaseSlide(index, direction = 0) {
  const tabButtons = document.querySelectorAll('.stage-tab-btn');
  const dots = document.querySelectorAll('.showcase-dot');
  const imgEl = document.getElementById('showcaseImg');
  const titleEl = document.getElementById('showcaseTitle');
  const descEl = document.getElementById('showcaseDesc');
  const b1El = document.getElementById('showcaseBenefit1');
  const b2El = document.getElementById('showcaseBenefit2');
  const ctaTextEl = document.getElementById('showcaseCtaText');

  if (index < 0 || index >= showcaseSlides.length) return;

  const prevIndex = currentShowcaseIndex;
  currentShowcaseIndex = index;
  const isForward = direction !== 0 ? direction > 0 : index >= prevIndex;

  tabButtons.forEach((btn, i) => {
    const isActive = i === index;
    btn.classList.toggle('active', isActive);
    btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
  });

  dots.forEach((dot, i) => {
    dot.classList.toggle('active', i === index);
  });

  if (imgEl) {
    imgEl.style.transition = 'opacity 140ms ease, transform 140ms ease';
    imgEl.style.opacity = '0.2';
    imgEl.style.transform = isForward ? 'translateX(-30px)' : 'translateX(30px)';
  }

  const slide = showcaseSlides[index];

  setTimeout(() => {
    if (imgEl) {
      imgEl.src = slide.img;
      imgEl.alt = slide.alt;
      imgEl.style.transition = 'none';
      imgEl.style.transform = isForward ? 'translateX(30px)' : 'translateX(-30px)';

      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          imgEl.style.transition = 'opacity 220ms ease, transform 220ms var(--ease-spring)';
          imgEl.style.opacity = '1';
          imgEl.style.transform = 'translateX(0)';
        });
      });
    }

    if (titleEl) titleEl.textContent = slide.title;
    if (descEl) descEl.textContent = slide.desc;
    if (b1El) b1El.textContent = slide.b1;
    if (b2El) b2El.textContent = slide.b2;
    if (ctaTextEl) ctaTextEl.textContent = slide.ctaText;
  }, 140);
}

/* --------------------------------------------------------------------------
   2. Touch Gesture Swiping on Showcase Stage (Android Mobile Web UX)
   -------------------------------------------------------------------------- */
function initTouchSwiping() {
  const showcaseCard = document.querySelector('.showcase-stage-card');
  if (!showcaseCard) return;

  let startX = 0;
  let startY = 0;
  let endX = 0;
  let endY = 0;

  showcaseCard.addEventListener('touchstart', (e) => {
    if (!e.touches || e.touches.length === 0) return;
    startX = e.touches[0].clientX;
    startY = e.touches[0].clientY;
    endX = startX;
    endY = startY;
  }, { passive: true });

  showcaseCard.addEventListener('touchmove', (e) => {
    if (!e.touches || e.touches.length === 0) return;
    endX = e.touches[0].clientX;
    endY = e.touches[0].clientY;
  }, { passive: true });

  showcaseCard.addEventListener('touchend', () => {
    const diffX = endX - startX;
    const diffY = endY - startY;

    // Detect horizontal swipe
    if (Math.abs(diffX) > 35 && Math.abs(diffX) > Math.abs(diffY)) {
      if (diffX < 0) {
        // Swiped Left -> Next Slide
        switchShowcaseSlide((currentShowcaseIndex + 1) % showcaseSlides.length, 1);
      } else {
        // Swiped Right -> Previous Slide
        switchShowcaseSlide((currentShowcaseIndex - 1 + showcaseSlides.length) % showcaseSlides.length, -1);
      }
    }
  }, { passive: true });
}

/* --------------------------------------------------------------------------
   3. Study FAQ Accordion
   -------------------------------------------------------------------------- */
function initStudyFaqAccordion() {
  const faqItems = document.querySelectorAll('.study-faq-item');

  faqItems.forEach(item => {
    const trigger = item.querySelector('.study-faq-trigger');
    if (!trigger) return;

    trigger.addEventListener('click', () => {
      const isExpanded = item.classList.contains('active');

      // Close other open accordions
      faqItems.forEach(otherItem => {
        if (otherItem !== item) {
          otherItem.classList.remove('active');
          const otherTrigger = otherItem.querySelector('.study-faq-trigger');
          if (otherTrigger) otherTrigger.setAttribute('aria-expanded', 'false');
        }
      });

      item.classList.toggle('active', !isExpanded);
      trigger.setAttribute('aria-expanded', !isExpanded ? 'true' : 'false');
    });
  });
}

/* --------------------------------------------------------------------------
   4. Modals (QR Code & Social Share)
   -------------------------------------------------------------------------- */
function initModalTriggers() {
  const qrModal = document.getElementById('qrModal');
  const shareModal = document.getElementById('shareModal');

  // QR Modal
  document.querySelectorAll('.js-open-qr').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      if (qrModal) {
        qrModal.classList.add('active');
        document.body.style.overflow = 'hidden';
      }
    });
  });

  // Share Action: Native Android Share on Mobile; Custom Modal on Desktop
  document.querySelectorAll('.js-open-share').forEach(btn => {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      const shareData = {
        title: 'StudyTimer - Track Your Progress on Android',
        text: 'Track your study progress and focus sessions with StudyTimer for Android! Download free:',
        url: 'https://get-studytimer.vercel.app'
      };

      const isMobile = /Android|iPhone|iPad|iPod|webOS|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) || window.innerWidth <= 768;

      if (navigator.share && isMobile) {
        try {
          await navigator.share(shareData);
        } catch (err) {
          // If aborted by user, do nothing. If error occurs, fallback to modal.
          if (err && err.name !== 'AbortError' && shareModal) {
            shareModal.classList.add('active');
            document.body.style.overflow = 'hidden';
          }
        }
      } else if (shareModal) {
        // Desktop environment: open desktop share modal
        shareModal.classList.add('active');
        document.body.style.overflow = 'hidden';
      }
    });
  });

  // Close modals
  document.querySelectorAll('.js-close-modal, .js-close-share-modal').forEach(btn => {
    btn.addEventListener('click', () => {
      if (qrModal) qrModal.classList.remove('active');
      if (shareModal) shareModal.classList.remove('active');
      document.body.style.overflow = '';
    });
  });

  // Backdrop click
  [qrModal, shareModal].forEach(modal => {
    if (!modal) return;
    modal.addEventListener('click', (e) => {
      if (e.target === modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
      }
    });
  });

  // Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      if (qrModal) qrModal.classList.remove('active');
      if (shareModal) shareModal.classList.remove('active');
      document.body.style.overflow = '';
    }
  });
}

/* --------------------------------------------------------------------------
   6. Sticky Android CTA Observer
   -------------------------------------------------------------------------- */
function initStickyAndroidCta() {
  const stickyBar = document.querySelector('.sticky-android-cta');
  const heroBtn = document.getElementById('heroDownloadBtn');

  if (!stickyBar || !heroBtn) return;

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (!entry.isIntersecting) {
        stickyBar.style.display = 'block';
      } else {
        stickyBar.style.display = 'none';
      }
    });
  }, { threshold: 0.1 });

  observer.observe(heroBtn);
}

/* --------------------------------------------------------------------------
   7. Clipboard Toast
   -------------------------------------------------------------------------- */
function initClipboardHelpers() {
  document.querySelectorAll('.js-copy-link').forEach(btn => {
    btn.addEventListener('click', () => {
      const linkUrl = btn.getAttribute('data-url') || window.location.origin;
      navigator.clipboard.writeText(linkUrl).then(() => {
        const originalText = btn.innerHTML;
        btn.innerHTML = '<span>✓ Copied!</span>';
        setTimeout(() => {
          btn.innerHTML = originalText;
        }, 2000);
      });
    });
  });
}

/* --------------------------------------------------------------------------
   8. Mobile Navigation Drawer
   -------------------------------------------------------------------------- */
function initMobileDrawer() {
  const toggleBtn = document.querySelector('.mobile-menu-trigger');
  const navMenu = document.querySelector('.nav-menu');

  if (!toggleBtn || !navMenu) return;

  toggleBtn.addEventListener('click', () => {
    const isOpened = navMenu.style.display === 'flex';
    navMenu.style.display = isOpened ? 'none' : 'flex';
    if (!isOpened) {
      navMenu.style.position = 'absolute';
      navMenu.style.top = '68px';
      navMenu.style.left = '0';
      navMenu.style.width = '100%';
      navMenu.style.background = 'rgba(5, 7, 11, 0.98)';
      navMenu.style.flexDirection = 'column';
      navMenu.style.padding = '1.75rem';
      navMenu.style.borderBottom = '1px solid rgba(255, 255, 255, 0.1)';
      navMenu.style.boxShadow = '0 15px 40px rgba(0,0,0,0.9)';
      navMenu.style.zIndex = '1000';
    }
  });

  // Close nav when clicking any link
  navMenu.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => {
      if (window.innerWidth <= 768) {
        navMenu.style.display = 'none';
      }
    });
  });
}
