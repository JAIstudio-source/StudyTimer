// ============================================================================
// STUDYTIMER MOTIVATIONAL QUOTE ENGINE
// ============================================================================
// 8. MOTIVATIONAL DAILY QUOTE ENGINE
// ============================================================================
const MOTIVATIONAL_QUOTES = [
  "Deep focus creates mastery.",
  "Small daily steps, big results.",
  "Energy flows where attention goes.",
  "Stay consistent, stay focused.",
  "Action cures hesitation.",
  "Quiet the noise, find your flow.",
  "Your time to build is now.",
  "Discipline equals true freedom.",
  "Fall in love with the process.",
  "Great things take focused time.",
  "Progress over perfection.",
  "Dream big. Start small. Act now.",
  "One focused hour at a time.",
  "Show up every single day.",
  "Master your minutes, master your life.",
  "Consistency is the secret code."
];

function initQuoteManager() {
  const quoteText = document.getElementById('dailyQuoteText');
  const nextBtn = document.getElementById('btnNextQuote');
  const quoteContainer = document.getElementById('dailyQuoteContainer');

  const mobileQuoteText = document.getElementById('mobileDailyQuoteText');
  const mobileNextBtn = document.getElementById('btnNextMobileQuote');
  const mobileQuoteContainer = document.getElementById('mobileDailyQuoteBanner');

  const autohideQuoteText = document.getElementById('autohideQuoteText');

  let currentQuoteIndex = Math.floor(Math.random() * MOTIVATIONAL_QUOTES.length);

  function displayQuote(index) {
    const fullQuote = `"${MOTIVATIONAL_QUOTES[index]}"`;
    
    // Desktop topbar quote
    if (quoteText) {
      quoteText.style.opacity = '0';
      quoteText.style.transform = 'translateY(-4px)';
      quoteText.style.transition = 'all 0.2s ease';
      setTimeout(() => {
        quoteText.textContent = fullQuote;
        quoteText.style.opacity = '1';
        quoteText.style.transform = 'translateY(0)';
        quoteContainer?.setAttribute('title', `${fullQuote} • Click for next quote`);
      }, 200);
    }

    // Mobile dedicated inspiration banner
    if (mobileQuoteText) {
      mobileQuoteText.style.opacity = '0';
      mobileQuoteText.style.transform = 'translateY(-4px)';
      mobileQuoteText.style.transition = 'all 0.2s ease';
      setTimeout(() => {
        mobileQuoteText.textContent = fullQuote;
        mobileQuoteText.style.opacity = '1';
        mobileQuoteText.style.transform = 'translateY(0)';
        mobileQuoteContainer?.setAttribute('title', `${fullQuote} • Click for next quote`);
      }, 200);
    }

    // Autohide focus quote banner
    if (autohideQuoteText) {
      autohideQuoteText.textContent = fullQuote;
    }
  }

  function nextQuote() {
    currentQuoteIndex = (currentQuoteIndex + 1) % MOTIVATIONAL_QUOTES.length;
    displayQuote(currentQuoteIndex);
  }

  const initQuote = `"${MOTIVATIONAL_QUOTES[currentQuoteIndex]}"`;
  if (quoteText) {
    quoteText.textContent = initQuote;
    quoteContainer?.setAttribute('title', `${initQuote} • Click for next quote`);
  }
  if (mobileQuoteText) {
    mobileQuoteText.textContent = initQuote;
    mobileQuoteContainer?.setAttribute('title', `${initQuote} • Click for next quote`);
  }
  if (autohideQuoteText) {
    autohideQuoteText.textContent = initQuote;
  }

  nextBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    nextQuote();
  });

  mobileNextBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    nextQuote();
  });

  quoteContainer?.addEventListener('click', (e) => {
    if (e.target !== nextBtn && !nextBtn?.contains(e.target)) {
      nextQuote();
    }
  });

  mobileQuoteContainer?.addEventListener('click', (e) => {
    if (e.target !== mobileNextBtn && !mobileNextBtn?.contains(e.target)) {
      nextQuote();
    }
  });
}

// ============================================================================