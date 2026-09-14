import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const FONT_PATH = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Black.ttf');
GlobalFonts.registerFromPath(FONT_PATH, 'MontserratBlack');

const screenshots = [
  'screenshots/final/01_master_focus.png',
  'screenshots/final/02_deep_insights.png',
  'screenshots/final/03_subject_analytics.png',
  'screenshots/final/04_daily_planner.png',
  'screenshots/final/05_goal_calendar.png',
  'screenshots/final/06_custom_subjects.png',
  'screenshots/final/07_amoled_themes.png',
  'screenshots/final/08_share_milestones.png'
];

async function generateShowcase() {
  const numScreens = screenshots.length;
  const thumbW = 380;
  const thumbH = Math.round((thumbW * 2796) / 1290); // ~823
  const padding = 28;
  const topBarH = 150;
  const totalW = padding * 2 + numScreens * thumbW + (numScreens - 1) * padding;
  const totalH = topBarH + thumbH + padding * 2;

  const canvas = createCanvas(totalW, totalH);
  const ctx = canvas.getContext('2d');

  // Background
  const grad = ctx.createLinearGradient(0, 0, 0, totalH);
  grad.addColorStop(0, '#F8FAFC');
  grad.addColorStop(1, '#EEF2FF');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, totalW, totalH);

  // Header Title
  ctx.font = '900 44px MontserratBlack';
  ctx.fillStyle = '#1E1B4B';
  ctx.textAlign = 'left';
  ctx.fillText('StudyTimer • Complete ASO & Google Play Screenshot Suite', padding, 75);

  ctx.font = '900 18px MontserratBlack';
  ctx.fillStyle = '#6366F1';
  ctx.fillText('8 Store Screenshots with Soft Modern Palette & Real-Sized Flagship Frames + Feature Graphic (1024x500)', padding, 115);

  // Draw Thumbnails
  let x = padding;
  const y = topBarH + padding;

  for (let i = 0; i < screenshots.length; i++) {
    const shotPath = screenshots[i];
    const resizedBuf = await sharp(shotPath).resize(thumbW, thumbH).png().toBuffer();
    const img = await loadImage(resizedBuf);

    ctx.save();
    ctx.shadowColor = 'rgba(15, 23, 42, 0.12)';
    ctx.shadowBlur = 24;
    ctx.shadowOffsetY = 12;
    
    ctx.drawImage(img, x, y, thumbW, thumbH);
    ctx.strokeStyle = 'rgba(0, 0, 0, 0.06)';
    ctx.lineWidth = 1;
    ctx.strokeRect(x, y, thumbW, thumbH);
    ctx.restore();

    x += thumbW + padding;
  }

  const outBuf = canvas.toBuffer('image/png');
  fs.writeFileSync('screenshots/showcase.png', outBuf);
  console.log('Saved showcase: screenshots/showcase.png');
}

generateShowcase().catch(console.error);
