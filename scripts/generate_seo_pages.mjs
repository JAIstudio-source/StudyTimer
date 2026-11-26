import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

const SEO_PAGES = [
  {
    slug: 'pomodoro-timer',
    title: 'Free Online Pomodoro Timer — StudyTimer 50/10 & 25/5 Focus',
    description: 'Free ad-free online Pomodoro timer with customizable 25/5 or 50/10 intervals, ambient study sounds, subject tracking, and seamless Android cloud sync.',
    keywords: 'online pomodoro timer, free pomodoro timer, pomodoro study timer, 25 5 timer, 50 10 pomodoro, aesthetic pomodoro clock',
    h1: 'Free Online Pomodoro Timer — StudyTimer Focus Studio',
    breadcrumbName: 'Pomodoro Timer',
    guideTitle: 'About StudyTimer Online Pomodoro Timer',
    guideIntro: 'StudyTimer opens directly in Pomodoro mode with customizable 25/5 and 50/10 focus intervals, automatic break transitions, ambient soundscapes, and optional Google Cloud Sync with the Android app.',
    faqs: [
      {
        q: 'Can I customize the Pomodoro focus and break lengths?',
        a: 'Yes. Open Timer Preferences to set any focus duration (such as 25, 50, or 90 minutes), short break, and long break interval.'
      },
      {
        q: 'Does my Pomodoro progress sync to Android?',
        a: 'Yes. Sign in with your Google account in the sidebar to sync your study sessions, streaks, and subjects between StudyTimer Web and the Android app.'
      }
    ],
    preset: {
      mode: 'pomodoro',
      enableSubjects: false
    }
  },
  {
    slug: 'stopwatch',
    title: 'Online Study Stopwatch — Track Deep Focus Hours Free | StudyTimer',
    description: 'Free online study stopwatch for tracking deep work and exam preparation hours without distractions. Includes subject logging and Android cloud sync.',
    keywords: 'study stopwatch, online study stopwatch, count up study timer, track study hours, free focus stopwatch',
    h1: 'Online Study Stopwatch — Track Deep Focus Hours',
    breadcrumbName: 'Study Stopwatch',
    guideTitle: 'About StudyTimer Online Study Stopwatch',
    guideIntro: 'StudyTimer Stopwatch lets you count up your uninterrupted study sessions without rigid countdown alarms—ideal for mock tests, problem solving, coding, and long reading blocks.',
    faqs: [
      {
        q: 'When should I use a study stopwatch instead of Pomodoro?',
        a: 'Use Stopwatch mode when you want to stay in flow past 25 or 50 minutes without a break alarm interrupting your concentration.'
      },
      {
        q: 'Are stopwatch sessions saved to my daily study total?',
        a: 'Yes. Every finished stopwatch session is logged to your daily focus total, streak calendar, and subject breakdown.'
      }
    ],
    preset: {
      mode: 'stopwatch',
      enableSubjects: false
    }
  },
  {
    slug: 'neet-study-timer',
    title: 'NEET Study Timer & Subject Tracker — Free Online | StudyTimer',
    description: 'Free online NEET study timer with preset Physics, Chemistry, and Biology subjects. Track daily target hours, Pomodoro sessions, and streaks with zero ads.',
    keywords: 'neet study timer, neet pomodoro timer, neet subject tracker, neet preparation timer, biology physics chemistry study tracker',
    h1: 'NEET Study Timer &amp; Subject Tracker — StudyTimer',
    breadcrumbName: 'NEET Study Timer',
    guideTitle: 'NEET Preparation Timer & Subject Breakdown',
    guideIntro: 'Pre-configured for NEET aspirants with Physics, Chemistry, Biology (Botany), Biology (Zoology), and Mock Tests & PYQs. Track how many hours you invest in each NCERT subject every week.',
    faqs: [
      {
        q: 'What happens if I sign in with Google on the NEET timer?',
        a: 'Your own saved subjects from your StudyTimer account automatically load and override the default preset subjects, so your personal study data stays consistent everywhere.'
      },
      {
        q: 'Can I track 3-hour NEET mock tests?',
        a: 'Yes. Switch to Stopwatch mode or set a custom 180-minute Pomodoro block under Mock Tests & PYQs to log full-length NEET practice exams.'
      }
    ],
    preset: {
      mode: 'pomodoro',
      enableSubjects: true,
      subjects: [
        { id: 'physics', name: 'Physics', iconEmoji: '⚛️', color: '#3b82f6' },
        { id: 'chemistry', name: 'Chemistry', iconEmoji: '🧪', color: '#10b981' },
        { id: 'botany', name: 'Biology (Botany)', iconEmoji: '🌿', color: '#22c55e' },
        { id: 'zoology', name: 'Biology (Zoology)', iconEmoji: '🧬', color: '#14b8a6' },
        { id: 'neet-mock', name: 'Mock Tests & PYQs', iconEmoji: '📝', color: '#f59e0b' }
      ]
    }
  },
  {
    slug: 'jee-study-timer',
    title: 'JEE Study Timer & Subject Tracker — Free Online | StudyTimer',
    description: 'Free online JEE Main & Advanced study timer with preset Physics, Chemistry, and Mathematics subjects. Track mock tests, streaks, and focus hours.',
    keywords: 'jee study timer, jee pomodoro timer, jee subject tracker, iit jee focus timer, physics chemistry maths study tracker',
    h1: 'JEE Study Timer &amp; Subject Tracker — StudyTimer',
    breadcrumbName: 'JEE Study Timer',
    guideTitle: 'JEE Main & Advanced Study Timer',
    guideIntro: 'Pre-loaded with Physics, Chemistry, Mathematics, and Mock Tests & PYQs so JEE aspirants can balance PCM problem-solving hours and review weekly subject charts.',
    faqs: [
      {
        q: 'Can I track Physics, Chemistry, and Maths hours separately?',
        a: 'Yes. Select your active subject before starting the timer to see exact daily and monthly hour breakdowns in Overview & Stats.'
      },
      {
        q: 'Does it keep my saved subjects if I sign in?',
        a: 'Yes. Signing in with Google syncs your personal subjects and history from your StudyTimer Android or Web account.'
      }
    ],
    preset: {
      mode: 'pomodoro',
      enableSubjects: true,
      subjects: [
        { id: 'physics', name: 'Physics', iconEmoji: '⚛️', color: '#3b82f6' },
        { id: 'chemistry', name: 'Chemistry', iconEmoji: '🧪', color: '#10b981' },
        { id: 'mathematics', name: 'Mathematics', iconEmoji: '📐', color: '#8b5cf6' },
        { id: 'jee-mock', name: 'Mock Tests & PYQs', iconEmoji: '📝', color: '#f59e0b' }
      ]
    }
  },
  {
    slug: 'upsc-study-timer',
    title: 'UPSC Study Timer & Subject Tracker — Free Online | StudyTimer',
    description: 'Free online UPSC CSE study timer with preset GS, Optional, Current Affairs, CSAT, and Answer Writing subjects. Built for long distraction-free sessions.',
    keywords: 'upsc study timer, upsc pomodoro timer, ias study tracker, upsc subject hours tracker, csat answer writing timer',
    h1: 'UPSC Study Timer &amp; Subject Tracker — StudyTimer',
    breadcrumbName: 'UPSC Study Timer',
    guideTitle: 'UPSC CSE Study & Answer Writing Tracker',
    guideIntro: 'Built for Civil Services preparation with General Studies, Optional Subject, Current Affairs, CSAT, and Answer Writing presets—helping you maintain consistent daily study streaks.',
    faqs: [
      {
        q: 'Can I customize or add GS Paper I–IV subjects?',
        a: 'Yes. You can add custom subjects anytime, and signing in with Google saves your custom UPSC subject list across Web and Android.'
      },
      {
        q: 'Is StudyTimer completely free and ad-free for long sessions?',
        a: 'Yes. StudyTimer has zero ads, no paywalls, and works offline-first so nothing interrupts your study blocks.'
      }
    ],
    preset: {
      mode: 'pomodoro',
      enableSubjects: true,
      subjects: [
        { id: 'gs', name: 'General Studies', iconEmoji: '🏛️', color: '#3b82f6' },
        { id: 'optional', name: 'Optional Subject', iconEmoji: '📚', color: '#8b5cf6' },
        { id: 'current-affairs', name: 'Current Affairs', iconEmoji: '📰', color: '#10b981' },
        { id: 'csat', name: 'CSAT', iconEmoji: '🧠', color: '#06b6d4' },
        { id: 'answer-writing', name: 'Answer Writing', iconEmoji: '✍️', color: '#f59e0b' }
      ]
    }
  }
];

function escapeHtml(str) {
  return str
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

const templatePath = path.join(rootDir, 'study.html');
const template = fs.readFileSync(templatePath, 'utf8');

for (const page of SEO_PAGES) {
  const canonicalUrl = `https://studytimer.site/${page.slug}`;
  const safeTitle = escapeHtml(page.title);
  const safeDesc = escapeHtml(page.description);
  const safeKeywords = escapeHtml(page.keywords);

  let html = template;

  // Replace primary meta tags
  html = html.replace(/<title>[\s\S]*?<\/title>/i, `<title>${safeTitle}</title>`);
  html = html.replace(/<meta name="title" content="[^"]*">/i, `<meta name="title" content="${safeTitle}">`);
  html = html.replace(/<meta name="description" content="[^"]*">/i, `<meta name="description" content="${safeDesc}">`);
  html = html.replace(/<meta name="keywords" content="[^"]*">/i, `<meta name="keywords" content="${safeKeywords}">`);
  html = html.replace(/<link rel="canonical" href="[^"]*">/i, `<link rel="canonical" href="${canonicalUrl}">`);

  // Replace Microdata, Open Graph & Twitter tags
  html = html.replace(/<meta itemprop="name" content="[^"]*">/i, `<meta itemprop="name" content="${safeTitle}">`);
  html = html.replace(/<meta itemprop="description" content="[^"]*">/i, `<meta itemprop="description" content="${safeDesc}">`);
  html = html.replace(/<meta property="og:url" content="[^"]*">/i, `<meta property="og:url" content="${canonicalUrl}">`);
  html = html.replace(/<meta property="og:title" content="[^"]*">/i, `<meta property="og:title" content="${safeTitle}">`);
  html = html.replace(/<meta property="og:description" content="[^"]*">/i, `<meta property="og:description" content="${safeDesc}">`);
  html = html.replace(/<meta name="twitter:url" content="[^"]*">/i, `<meta name="twitter:url" content="${canonicalUrl}">`);
  html = html.replace(/<meta name="twitter:title" content="[^"]*">/i, `<meta name="twitter:title" content="${safeTitle}">`);
  html = html.replace(/<meta name="twitter:description" content="[^"]*">/i, `<meta name="twitter:description" content="${safeDesc}">`);

  // Replace JSON-LD block with route-specific WebApplication + BreadcrumbList + FAQPage
  const jsonLdBlock = `<!-- Structured Data (JSON-LD Schema for WebApplication, Breadcrumb & FAQ) -->
  <script type="application/ld+json">
  ${JSON.stringify({
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "WebApplication",
        "name": page.breadcrumbName,
        "url": canonicalUrl,
        "applicationCategory": "EducationalApplication",
        "browserRequirements": "Requires JavaScript. Requires HTML5.",
        "offers": {
          "@type": "Offer",
          "price": "0",
          "priceCurrency": "USD"
        },
        "description": page.description,
        "image": "https://studytimer.site/assets/og-preview.png",
        "author": {
          "@type": "Organization",
          "name": "StudyTimer",
          "url": "https://studytimer.site/"
        }
      },
      {
        "@type": "BreadcrumbList",
        "itemListElement": [
          {
            "@type": "ListItem",
            "position": 1,
            "name": "Home",
            "item": "https://studytimer.site/"
          },
          {
            "@type": "ListItem",
            "position": 2,
            "name": page.breadcrumbName,
            "item": canonicalUrl
          }
        ]
      },
      {
        "@type": "FAQPage",
        "mainEntity": page.faqs.map(f => ({
          "@type": "Question",
          "name": f.q,
          "acceptedAnswer": {
            "@type": "Answer",
            "text": f.a
          }
        }))
      }
    ]
  }, null, 2)}
  </script>`;

  html = html.replace(
    /<!-- Structured Data \(JSON-LD[\s\S]*?<\/script>/i,
    jsonLdBlock
  );

  // Inject window.__STUDYTIMER_PRESET__ right before </head>
  const presetScript = `  <!-- Route Preset Configuration -->
  <script>
    window.__STUDYTIMER_PRESET__ = ${JSON.stringify(page.preset)};
  </script>
</head>`;
  html = html.replace(/<\/head>/i, presetScript);

  // Replace SR-only H1
  html = html.replace(
    /<h1 class="sr-only">[\s\S]*?<\/h1>/i,
    `<h1 class="sr-only">${page.h1}</h1>`
  );

  // Inject compact collapsible SEO guide & FAQ in footer
  const faqHtml = page.faqs
    .map(f => `<h3>${escapeHtml(f.q)}</h3><p>${escapeHtml(f.a)}</p>`)
    .join('');
  const guideHtml = `        </div>
        <details class="seo-preset-guide">
          <summary>${escapeHtml(page.guideTitle)} &amp; FAQ</summary>
          <div class="seo-preset-guide-body">
            <h2>${escapeHtml(page.guideTitle)}</h2>
            <p>${escapeHtml(page.guideIntro)}</p>
            ${faqHtml}
          </div>
        </details>
      </footer>`;
  html = html.replace(/<\/div>\s*<\/footer>/i, guideHtml);

  const outPath = path.join(rootDir, `${page.slug}.html`);
  fs.writeFileSync(outPath, html, 'utf8');
  console.log(`Generated ${page.slug}.html -> ${canonicalUrl}`);
}
