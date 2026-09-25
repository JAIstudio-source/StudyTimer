// ============================================================================
// STUDYTIMER MOTIVATIONAL QUOTE ENGINE
// ============================================================================
// 8. MOTIVATIONAL DAILY QUOTE ENGINE
// ============================================================================
const MOTIVATIONAL_QUOTES = [
  "Small daily improvements over time lead to stunning results.",
  "Focus on being productive instead of busy.",
  "The secret of getting ahead is getting started.",
  "Discipline is choosing between what you want now and what you want most.",
  "Deep work is the superpower of the 21st century.",
  "Action is the foundational key to all success.",
  "You don't have to be extreme, just consistent.",
  "Success is the sum of small efforts, repeated day in and day out.",
  "It always seems impossible until it's done.",
  "Your future is created by what you do today, not tomorrow.",
  "Energy flows where attention goes.",
  "Fall in love with the process and the results will come.",
  "Don't wish it were easier, wish you were better.",
  "Continuous learning is the minimum requirement for success in any field.",
  "Push yourself, because no one else is going to do it for you.",
  "Great things never come from comfort zones.",
  "Dream big. Start small. Act now.",
  "Stay focused, go after your dreams, and keep moving toward your goals."
];

function initQuoteManager() {
  const quoteText = document.getElementById('dailyQuoteText');
  const nextBtn = document.getElementById('btnNextQuote');
  const quoteContainer = document.getElementById('dailyQuoteContainer');

  let currentQuoteIndex = Math.floor(Math.random() * MOTIVATIONAL_QUOTES.length);

  function displayQuote(index) {
    if (quoteText) {
      quoteText.style.opacity = '0';
      quoteText.style.transform = 'translateY(-4px)';
      quoteText.style.transition = 'all 0.2s ease';
      setTimeout(() => {
        quoteText.textContent = `"${MOTIVATIONAL_QUOTES[index]}"`;
        quoteText.style.opacity = '1';
        quoteText.style.transform = 'translateY(0)';
      }, 200);
    }
  }

  function nextQuote() {
    currentQuoteIndex = (currentQuoteIndex + 1) % MOTIVATIONAL_QUOTES.length;
    displayQuote(currentQuoteIndex);
  }

  if (quoteText) {
    quoteText.textContent = `"${MOTIVATIONAL_QUOTES[currentQuoteIndex]}"`;
  }

  nextBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    nextQuote();
  });

  quoteContainer?.addEventListener('click', (e) => {
    if (e.target !== nextBtn && !nextBtn?.contains(e.target)) {
      nextQuote();
    }
  });
}

// ============================================================================