import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const CANVAS_W = 1290;
const CANVAS_H = 2796;

// Exact, uniform phone mockup geometry for all screenshots
const DEVICE_W = 1040;
const BEZEL = 14;
const SCREEN_W = DEVICE_W - 2 * BEZEL; // 1012
const SCREEN_CORNER_R = 48;
const DEVICE_Y = 520;

const FONT_BLACK = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Black.ttf');
const FONT_REGULAR = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Regular.ttf');

GlobalFonts.registerFromPath(FONT_BLACK, 'MontserratBlack');
GlobalFonts.registerFromPath(FONT_REGULAR, 'MontserratRegular');

function wrapText(ctx, text, maxW) {
  const words = text.split(' ');
  const lines = [];
  let cur = '';
  for (const w of words) {
    const test = cur ? `${cur} ${w}` : w;
    if (ctx.measureText(test).width <= maxW) {
      cur = test;
    } else {
      if (cur) lines.push(cur);
      cur = w;
    }
  }
  if (cur) lines.push(cur);
  return lines;
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

export async function composeScreenshot({
  bgColors = ['#FBF9F6', '#F4EFEA'],
  title,
  subtitle,
  screenshotPath,
  outputPath
}) {
  const canvas = createCanvas(CANVAS_W, CANVAS_H);
  const ctx = canvas.getContext('2d');

  // 1. Premium Soft Background (Clean pastel / neutral tint)
  const grad = ctx.createLinearGradient(0, 0, 0, CANVAS_H);
  grad.addColorStop(0, bgColors[0]);
  grad.addColorStop(1, bgColors[1]);
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

  // Soft ambient central glow behind the phone
  ctx.save();
  const ambientGlow = ctx.createRadialGradient(CANVAS_W / 2, 750, 80, CANVAS_W / 2, 900, 850);
  ambientGlow.addColorStop(0, 'rgba(255, 255, 255, 0.75)');
  ambientGlow.addColorStop(1, 'rgba(255, 255, 255, 0)');
  ctx.fillStyle = ambientGlow;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);
  ctx.restore();

  // 2. Clean Typography (Matching Reference: Bold Headline + Crisp Subtitle)
  ctx.textAlign = 'center';
  const MAX_TEXT_W = CANVAS_W * 0.86;

  // Title / Headline (Title Case, 84px)
  ctx.font = '900 84px MontserratBlack';
  ctx.fillStyle = '#1A1C23';
  const titleLines = wrapText(ctx, title, MAX_TEXT_W);
  
  let currentY = titleLines.length > 1 ? 190 : 230;
  for (const line of titleLines) {
    ctx.fillText(line, CANVAS_W / 2, currentY);
    currentY += 96;
  }

  // Subtitle (Crisp, High Contrast, 44px)
  ctx.font = '900 44px MontserratBlack';
  ctx.fillStyle = '#475569';
  const subLines = wrapText(ctx, subtitle, MAX_TEXT_W);
  currentY += 12;
  for (const line of subLines) {
    ctx.fillText(line, CANVAS_W / 2, currentY);
    currentY += 56;
  }

  // 3. Exact Uniform Phone Frame Geometry
  const deviceX = Math.round((CANVAS_W - DEVICE_W) / 2);
  const screenX = deviceX + BEZEL;
  const screenY = DEVICE_Y + BEZEL;

  // Load and resize screenshot preserving 100% full width and aspect ratio
  const rawMeta = await sharp(screenshotPath).metadata();
  const shotH = Math.round((SCREEN_W * rawMeta.height) / rawMeta.width);
  const totalScreenHeight = Math.max(shotH, CANVAS_H - screenY + 200);
  const totalDeviceHeight = totalScreenHeight + 2 * BEZEL;

  const processedShotBuffer = await sharp(screenshotPath)
    .resize({ width: SCREEN_W, height: shotH })
    .png()
    .toBuffer();

  const shotImg = await loadImage(processedShotBuffer);

  // Multi-tier Soft Drop Shadows (100% Identical across all screenshots)
  ctx.save();
  ctx.shadowColor = 'rgba(15, 23, 42, 0.18)';
  ctx.shadowBlur = 65;
  ctx.shadowOffsetY = 32;
  ctx.fillStyle = '#08080E';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, totalDeviceHeight, SCREEN_CORNER_R + 8);
  ctx.fill();

  ctx.shadowColor = 'rgba(15, 23, 42, 0.09)';
  ctx.shadowBlur = 22;
  ctx.shadowOffsetY = 12;
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, totalDeviceHeight, SCREEN_CORNER_R + 8);
  ctx.fill();
  ctx.restore();

  // Phone Frame Outer Body
  ctx.save();
  ctx.fillStyle = '#14141E';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, totalDeviceHeight, SCREEN_CORNER_R + 8);
  ctx.fill();

  // Subtle Chamfer Bezel Edge Highlight
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.20)';
  ctx.lineWidth = 2.5;
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, totalDeviceHeight, SCREEN_CORNER_R + 8);
  ctx.stroke();
  ctx.restore();

  // Draw App Screen with exact corner clipping
  ctx.save();
  roundRect(ctx, screenX, screenY, SCREEN_W, totalScreenHeight, SCREEN_CORNER_R);
  ctx.clip();
  ctx.fillStyle = '#000000';
  ctx.fillRect(screenX, screenY, SCREEN_W, totalScreenHeight);
  ctx.drawImage(shotImg, screenX, screenY, SCREEN_W, shotH);
  ctx.restore();

  // Save to final JPG
  const outBuf = canvas.toBuffer('image/jpeg', { quality: 96 });
  fs.writeFileSync(outputPath, outBuf);
  console.log(`Generated: ${outputPath}`);
}

async function runAll() {
  const screenshots = [
    {
      title: 'Stay Motivated With Daily Goals',
      subtitle: 'Set subject targets and build consistent study habits',
      screenshotPath: 'screenshots/assets/screen_05_insight-goal-planner.jpg',
      outputPath: 'screenshots/final/01_goals.jpg',
      bgColors: ['#FDF9F3', '#F5EEE4'] // Warm Ivory
    },
    {
      title: 'Stay Focused With Pomodoro',
      subtitle: 'Take structured breaks to keep your mind fresh and energized',
      screenshotPath: 'screenshots/assets/screen_09_pomodoro.jpg',
      outputPath: 'screenshots/final/02_pomodoro.jpg',
      bgColors: ['#F3F8FB', '#E6EEF4'] // Soft Ice Blue
    },
    {
      title: 'Beautiful Study Insights',
      subtitle: 'Visualize daily averages, streaks, and study heatmaps',
      screenshotPath: 'screenshots/assets/screen_02_insight.jpg',
      outputPath: 'screenshots/final/03_insights.jpg',
      bgColors: ['#FAF5FF', '#EDE4F7'] // Soft Lilac
    },
    {
      title: 'Track Time Across Subjects',
      subtitle: 'See exact breakdown for all your study subjects',
      screenshotPath: 'screenshots/assets/screen_04_insight-pie-chart.jpg',
      outputPath: 'screenshots/final/04_subjects_pie.jpg',
      bgColors: ['#F2FBF7', '#E1F6ED'] // Soft Mint
    },
    {
      title: 'Organize Your Study Subjects',
      subtitle: 'Color-coded tags for lectures, homework, and custom topics',
      screenshotPath: 'screenshots/assets/screen_11_subjects.jpg',
      outputPath: 'screenshots/final/05_subjects_tags.jpg',
      bgColors: ['#FDF4F8', '#F8E3EE'] // Soft Rose
    },
    {
      title: 'Celebrate Your Milestones',
      subtitle: 'Export weekly summary report cards',
      screenshotPath: 'screenshots/assets/screen_06_summary.jpg',
      outputPath: 'screenshots/final/06_milestones.jpg',
      bgColors: ['#F4F5FB', '#E7EAF6'] // Soft Indigo
    },
    {
      title: 'Log Every Single Session',
      subtitle: 'Track your consistency with interactive calendar rings',
      screenshotPath: 'screenshots/assets/screen_03_insight-calendar.jpg',
      outputPath: 'screenshots/final/07_calendar.jpg',
      bgColors: ['#FBF7F0', '#F3ECE0'] // Soft Sand Amber
    },
    {
      title: 'Built for Pure Focus & Privacy',
      subtitle: 'AMOLED black themes, 100% offline with zero ads',
      screenshotPath: 'screenshots/assets/screen_08_setting-theme.jpg',
      outputPath: 'screenshots/final/08_privacy_theme.jpg',
      bgColors: ['#F8FAFC', '#E2E8F0'] // Soft Slate
    }
  ];

  for (const item of screenshots) {
    await composeScreenshot(item);
  }
}

runAll().catch(console.error);
