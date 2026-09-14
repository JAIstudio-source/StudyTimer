import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const CANVAS_W = 1290;
const CANVAS_H = 2796;

// Real-sized modern flagship phone proportions (19.5 : 9)
const DEVICE_W = 980;
const DEVICE_H = 2110;
const BEZEL = 14;
const SCREEN_W = DEVICE_W - 2 * BEZEL; // 952
const SCREEN_H = DEVICE_H - 2 * BEZEL; // 2082
const SCREEN_CORNER_R = 48;
const DEVICE_Y = 560;

const FONT_PATH = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Black.ttf');
GlobalFonts.registerFromPath(FONT_PATH, 'MontserratBlack');

function fitFont(ctx, text, maxW, startSize, minSize) {
  for (let size = startSize; size >= minSize; size -= 2) {
    ctx.font = `900 ${size}px MontserratBlack`;
    if (ctx.measureText(text).width <= maxW) {
      return size;
    }
  }
  return minSize;
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

export async function composeSoftScreenshot({
  bgGradient,
  textColor,
  descColor,
  verb,
  desc,
  screenshotPath,
  outputPath,
  topCrop = 64
}) {
  const canvas = createCanvas(CANVAS_W, CANVAS_H);
  const ctx = canvas.getContext('2d');

  // 1. Soft Background Gradient
  const grad = ctx.createLinearGradient(0, 0, 0, CANVAS_H);
  grad.addColorStop(0, bgGradient[0]);
  grad.addColorStop(1, bgGradient[1]);
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

  // Soft Ambient Glow behind phone
  ctx.save();
  const ambientGlow = ctx.createRadialGradient(CANVAS_W / 2, 720, 60, CANVAS_W / 2, 850, 780);
  ambientGlow.addColorStop(0, 'rgba(255, 255, 255, 0.65)');
  ambientGlow.addColorStop(1, 'rgba(255, 255, 255, 0)');
  ctx.fillStyle = ambientGlow;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);
  ctx.restore();

  // 2. Text Header
  ctx.textAlign = 'center';
  const MAX_TEXT_W = CANVAS_W * 0.82;

  // Verb / Action Headline
  const verbSize = fitFont(ctx, verb.toUpperCase(), MAX_TEXT_W, 94, 60);
  ctx.font = `900 ${verbSize}px MontserratBlack`;
  ctx.fillStyle = textColor;
  ctx.fillText(verb.toUpperCase(), CANVAS_W / 2, 255);

  // Descriptor
  const words = desc.toUpperCase().split(' ');
  const lines = [];
  let curLine = '';
  ctx.font = '900 42px MontserratBlack';

  for (const w of words) {
    const test = curLine ? `${curLine} ${w}` : w;
    if (ctx.measureText(test).width <= MAX_TEXT_W) {
      curLine = test;
    } else {
      if (curLine) lines.push(curLine);
      curLine = w;
    }
  }
  if (curLine) lines.push(curLine);

  let textY = 345;
  const descSize = fitFont(ctx, lines[0] || desc.toUpperCase(), MAX_TEXT_W, 42, 32);
  ctx.font = `900 ${descSize}px MontserratBlack`;
  ctx.fillStyle = descColor;

  for (const line of lines) {
    ctx.fillText(line, CANVAS_W / 2, textY);
    textY += descSize + 14;
  }

  // 3. Real-Sized Phone Mockup Geometry
  const deviceX = Math.round((CANVAS_W - DEVICE_W) / 2);
  const screenX = deviceX + BEZEL;
  const screenY = DEVICE_Y + BEZEL;

  // Process Screenshot
  const rawMeta = await sharp(screenshotPath).metadata();
  const cropT = Math.min(topCrop, rawMeta.height - 300);
  const cropH = rawMeta.height - cropT;

  const processedShotBuffer = await sharp(screenshotPath)
    .extract({ left: 0, top: cropT, width: rawMeta.width, height: cropH })
    .resize({ width: SCREEN_W, height: SCREEN_H, fit: 'cover', position: 'top' })
    .png()
    .toBuffer();

  const shotImg = await loadImage(processedShotBuffer);

  // Multi-layer Soft Natural Shadow
  ctx.save();
  ctx.shadowColor = 'rgba(15, 23, 42, 0.16)';
  ctx.shadowBlur = 65;
  ctx.shadowOffsetY = 35;
  ctx.fillStyle = '#08080E';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, DEVICE_H, SCREEN_CORNER_R + 8);
  ctx.fill();

  ctx.shadowColor = 'rgba(15, 23, 42, 0.08)';
  ctx.shadowBlur = 24;
  ctx.shadowOffsetY = 12;
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, DEVICE_H, SCREEN_CORNER_R + 8);
  ctx.fill();
  ctx.restore();

  // Phone Frame Outer Body
  ctx.save();
  ctx.fillStyle = '#14141E';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, DEVICE_H, SCREEN_CORNER_R + 8);
  ctx.fill();

  // Precision Chamfer Bezel
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.20)';
  ctx.lineWidth = 2.5;
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, DEVICE_H, SCREEN_CORNER_R + 8);
  ctx.stroke();
  ctx.restore();

  // Screenshot Clipping & Drawing
  ctx.save();
  roundRect(ctx, screenX, screenY, SCREEN_W, SCREEN_H, SCREEN_CORNER_R);
  ctx.clip();
  ctx.fillStyle = '#000000';
  ctx.fillRect(screenX, screenY, SCREEN_W, SCREEN_H);
  ctx.drawImage(shotImg, screenX, screenY, SCREEN_W, SCREEN_H);
  ctx.restore();

  // Modern Discreet Punch-Hole Camera Dot
  ctx.save();
  ctx.fillStyle = '#000000';
  ctx.beginPath();
  ctx.arc(CANVAS_W / 2, screenY + 22, 11, 0, Math.PI * 2);
  ctx.fill();
  
  ctx.fillStyle = 'rgba(60, 75, 115, 0.8)';
  ctx.beginPath();
  ctx.arc(CANVAS_W / 2 + 2, screenY + 21, 3, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  // Write file
  const outBuf = canvas.toBuffer('image/png');
  fs.writeFileSync(outputPath, outBuf);
  console.log(`Saved soft screenshot: ${outputPath}`);
}

async function runSoftSuite() {
  const items = [
    {
      // 01 Focus Timer
      bgGradient: ['#EEF2FF', '#E0E7FF'], // Soft Periwinkle
      textColor: '#1E1B4B',
      descColor: '#4338CA',
      verb: 'MASTER YOUR FOCUS',
      desc: 'DISTRACTION-FREE AMOLED TIMER & MODES',
      screenshotPath: 'Screenshot/Pomodoro_timer_Screen.jpg',
      outputPath: 'screenshots/final/01_master_focus.png',
      topCrop: 64
    },
    {
      // 02 Smart Analytics
      bgGradient: ['#F5F3FF', '#EDE9FE'], // Soft Lavender
      textColor: '#2E1065',
      descColor: '#6D28D9',
      verb: 'SMART INSIGHTS',
      desc: 'DAILY AVERAGES, STREAKS & HEATMAP',
      screenshotPath: 'Screenshot/Insight_screen_1.jpg',
      outputPath: 'screenshots/final/02_deep_insights.png',
      topCrop: 64
    },
    {
      // 03 Subject Distribution
      bgGradient: ['#ECFDF5', '#D1FAE5'], // Soft Mint
      textColor: '#064E3B',
      descColor: '#059669',
      verb: 'TRACK BY SUBJECT',
      desc: 'COLOR-CODED TAGS & ANALYTICS PIE',
      screenshotPath: 'Screenshot/Insight_screen_2.jpg',
      outputPath: 'screenshots/final/03_subject_analytics.png',
      topCrop: 64
    },
    {
      // 04 Daily Planner & Habits
      bgGradient: ['#FFFBEB', '#FEF3C7'], // Soft Amber
      textColor: '#78350F',
      descColor: '#B45309',
      verb: 'DAILY PLANNER',
      desc: 'SYLLABUS TARGETS & HABIT MATRIX',
      screenshotPath: 'Screenshot/Habit-Goal_screen.jpg',
      outputPath: 'screenshots/final/04_daily_planner.png',
      topCrop: 64
    },
    {
      // 05 Visual Calendar History
      bgGradient: ['#F0F9FF', '#E0F2FE'], // Soft Sky Blue
      textColor: '#0C4A6E',
      descColor: '#0284C7',
      verb: 'LOG EVERY SESSION',
      desc: 'INTERACTIVE CALENDAR & TIMELINE',
      screenshotPath: 'Screenshot/Goal_calendar_screen.jpg',
      outputPath: 'screenshots/final/05_goal_calendar.png',
      topCrop: 64
    },
    {
      // 06 Custom Subjects
      bgGradient: ['#FDF2F8', '#FCE7F3'], // Soft Rose
      textColor: '#831843',
      descColor: '#DB2777',
      verb: 'CUSTOM SUBJECTS',
      desc: 'TAG SESSIONS WITH ICONS & COLORS',
      screenshotPath: 'Screenshot/Custom_subject_screen.jpg',
      outputPath: 'screenshots/final/06_custom_subjects.png',
      topCrop: 64
    },
    {
      // 07 AMOLED & OLED Themes
      bgGradient: ['#F8FAFC', '#E2E8F0'], // Soft Slate Mist
      textColor: '#0F172A',
      descColor: '#334155',
      verb: 'AMOLED THEMES',
      desc: 'PURE BLACK, GLASS & ACCENT PALETTES',
      screenshotPath: 'Screenshot/Themes_1_settings_sreen.jpg',
      outputPath: 'screenshots/final/07_amoled_themes.png',
      topCrop: 64
    },
    {
      // 08 Weekly Summary Progress Card
      bgGradient: ['#FAF5FF', '#F3E8FF'], // Soft Royal Lilac
      textColor: '#3B0764',
      descColor: '#7E22CE',
      verb: 'SHARE PROGRESS',
      desc: 'BEAUTIFUL WEEKLY SUMMARY CARDS',
      screenshotPath: 'screenshots/raw/screen_06_summary.png',
      outputPath: 'screenshots/final/08_share_milestones.png',
      topCrop: 64
    }
  ];

  for (const item of items) {
    await composeSoftScreenshot(item);
  }
}

runSoftSuite().catch(console.error);
