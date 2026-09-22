import fs from 'fs';
import path from 'path';

const OG_IMAGE_URL = 'https://get-studytimer.vercel.app/assets/og-preview.png';
const OG_IMAGE_WIDTH = '1024';
const OG_IMAGE_HEIGHT = '500';
const OG_IMAGE_TYPE = 'image/png';

const pages = [
  {
    file: 'index.html',
    url: 'https://get-studytimer.vercel.app/',
    title: 'StudyTimer - Free Pomodoro Timer &amp; Study Habit Tracker',
    titlePlain: 'StudyTimer - Free Pomodoro Timer & Study Habit Tracker',
    description: 'Boost exam scores with StudyTimer. Free Pomodoro timer, subject tracker, study streaks, and analytics for students on Android and Web. 100% ad-free.',
    alt: 'StudyTimer - Free Pomodoro Timer & Study Habit Tracker Preview'
  },
  {
    file: 'demo.html',
    url: 'https://get-studytimer.vercel.app/demo.html',
    title: 'Online Pomodoro Timer &amp; Habit Tracker | StudyTimer Studio',
    titlePlain: 'Online Pomodoro Timer & Habit Tracker | StudyTimer Studio',
    description: 'Free online Pomodoro study timer with customizable countdown intervals, stopwatch, subject tracking, offline support, and seamless Android cloud sync.',
    alt: 'StudyTimer Web Studio - Online Focus Timer Preview'
  },
  {
    file: 'about.html',
    url: 'https://get-studytimer.vercel.app/about.html',
    title: 'About StudyTimer - Free Ad-Free Study &amp; Focus Companion',
    titlePlain: 'About StudyTimer - Free Ad-Free Study & Focus Companion',
    description: 'Discover the mission behind StudyTimer: building a free, privacy-first, 100% ad-free Pomodoro study timer and habit tracker empowering students worldwide.',
    alt: 'About StudyTimer - Free Ad-Free Study & Focus Companion'
  },
  {
    file: 'contact.html',
    url: 'https://get-studytimer.vercel.app/contact.html',
    title: 'Contact &amp; Support - StudyTimer Focus &amp; Study Habit Tracker',
    titlePlain: 'Contact & Support - StudyTimer Focus & Study Habit Tracker',
    description: 'Contact the StudyTimer team for support, feature suggestions, bug reports, and partnership inquiries. We are here to help your student study journey.',
    alt: 'Contact & Support - StudyTimer Focus & Study Habit Tracker'
  },
  {
    file: 'privacy.html',
    url: 'https://get-studytimer.vercel.app/privacy.html',
    title: 'Privacy Policy &amp; Data Safety - StudyTimer Offline-First Study App',
    titlePlain: 'Privacy Policy & Data Safety - StudyTimer Offline-First Study App',
    description: 'Read the official StudyTimer Privacy Policy. Learn about our zero-data-harvesting approach, offline-first local SQLite storage, and encrypted cloud sync.',
    alt: 'StudyTimer Privacy Policy & Data Safety'
  },
  {
    file: 'terms.html',
    url: 'https://get-studytimer.vercel.app/terms.html',
    title: 'Terms of Service &amp; User Agreement - StudyTimer Study App',
    titlePlain: 'Terms of Service & User Agreement - StudyTimer Study App',
    description: 'Review the Terms of Service and End-User Agreement for the StudyTimer application and online Pomodoro web tools. Free and ad-free focus companion.',
    alt: 'Terms of Service & User Agreement - StudyTimer'
  },
  {
    file: 'delete-account.html',
    url: 'https://get-studytimer.vercel.app/delete-account.html',
    title: 'Delete Account &amp; Cloud Sync Data - StudyTimer Data Management',
    titlePlain: 'Delete Account & Cloud Sync Data - StudyTimer Data Management',
    description: 'Request permanent erasure of your StudyTimer cloud sync backups, leaderboard entries, and account credentials. Full Google Play compliance.',
    alt: 'Delete Account & Cloud Sync Data - StudyTimer'
  },
  {
    file: 'thank-you.html',
    url: 'https://get-studytimer.vercel.app/thank-you.html',
    title: 'Download Complete - Get Started with StudyTimer Android Focus App',
    titlePlain: 'Download Complete - Get Started with StudyTimer Android Focus App',
    description: 'Thank you for downloading StudyTimer! Follow our quick setup guide to start tracking study sessions, building streaks, and acing your exams.',
    alt: 'StudyTimer Download & Setup Guide'
  },
  {
    file: '404.html',
    url: 'https://get-studytimer.vercel.app/404.html',
    title: 'Page Not Found (404) - Return to StudyTimer Focus Timer',
    titlePlain: 'Page Not Found (404) - Return to StudyTimer Focus Timer',
    description: 'Oops! The requested page could not be found. Return to StudyTimer to continue tracking your study sessions and building your daily focus streaks.',
    alt: 'Page Not Found (404) - StudyTimer'
  }
];

function generateSocialMetaBlock(page) {
  return `  <!-- Schema.org / Google / Reddit Microdata -->
  <meta itemprop="name" content="${page.titlePlain}">
  <meta itemprop="description" content="${page.description}">
  <meta itemprop="image" content="${OG_IMAGE_URL}">
  <link rel="image_src" href="${OG_IMAGE_URL}">

  <!-- Open Graph / Facebook / WhatsApp / Threads / LinkedIn / Discord -->
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="StudyTimer">
  <meta property="og:url" content="${page.url}">
  <meta property="og:title" content="${page.titlePlain}">
  <meta property="og:description" content="${page.description}">
  <meta property="og:image" content="${OG_IMAGE_URL}">
  <meta property="og:image:secure_url" content="${OG_IMAGE_URL}">
  <meta property="og:image:width" content="${OG_IMAGE_WIDTH}">
  <meta property="og:image:height" content="${OG_IMAGE_HEIGHT}">
  <meta property="og:image:type" content="${OG_IMAGE_TYPE}">
  <meta property="og:image:alt" content="${page.alt}">
  <meta property="og:locale" content="en_US">

  <!-- Twitter / X Cards -->
  <meta name="twitter:card" content="summary_large_image">
  <meta name="twitter:url" content="${page.url}">
  <meta name="twitter:title" content="${page.titlePlain}">
  <meta name="twitter:description" content="${page.description}">
  <meta name="twitter:image" content="${OG_IMAGE_URL}">
  <meta name="twitter:image:alt" content="${page.alt}">`;
}

pages.forEach(page => {
  if (!fs.existsSync(page.file)) return;
  let content = fs.readFileSync(page.file, 'utf8');

  // Ensure theme-color is present
  if (!content.includes('<meta name="theme-color"')) {
    content = content.replace(/<link rel="apple-touch-icon"[^>]*>/i, match => `${match}\n  <meta name="theme-color" content="#09090b">`);
  }

  // Remove existing Schema/Reddit microdata if present
  content = content.replace(/\s*<meta\s+itemprop=["'][^"']*["'][^>]*>/gi, '');
  content = content.replace(/\s*<link\s+rel=["']image_src["'][^>]*>/gi, '');

  // Remove existing OpenGraph and Twitter meta blocks
  // Regex to match existing OG / Twitter meta tags
  content = content.replace(/\s*<!--\s*(Open Graph|Twitter|Social)[^>]*?-->/gi, '');
  content = content.replace(/\s*<meta\s+(property|name)=["'](og:|twitter:)[^"']*["'][^>]*>/gi, '');

  const newSocialBlock = generateSocialMetaBlock(page);

  // Insert before Google Fonts or Stylesheet or Head closing or after apple-touch-icon / theme-color / canonical
  if (content.includes('<!-- Google Fonts -->')) {
    content = content.replace('  <!-- Google Fonts -->', `${newSocialBlock}\n\n  <!-- Google Fonts -->`);
  } else if (content.includes('<link rel="preconnect" href="https://fonts.googleapis.com">')) {
    content = content.replace('  <link rel="preconnect" href="https://fonts.googleapis.com">', `${newSocialBlock}\n\n  <link rel="preconnect" href="https://fonts.googleapis.com">`);
  } else if (content.includes('<link rel="stylesheet"')) {
    content = content.replace('  <link rel="stylesheet"', `${newSocialBlock}\n\n  <link rel="stylesheet"`);
  }

  // Update image in JSON-LD schemas
  content = content.replace(
    /https:\/\/get-studytimer\.vercel\.app\/assets\/feature_graphic_1024x500\.png/g,
    OG_IMAGE_URL
  );

  fs.writeFileSync(page.file, content, 'utf8');
  console.log(`Successfully updated social preview tags in ${page.file}`);
});
