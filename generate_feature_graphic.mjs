import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const FG_W = 1024;
const FG_H = 500;
const FONT_PATH = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Black.ttf');

GlobalFonts.registerFromPath(FONT_PATH, 'MontserratBlack');

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

export async function generateFeatureGraphic(outputPath) {
  const canvas = createCanvas(FG_W, FG_H);
  const ctx = canvas.getContext('2d');

  // Soft Sophisticated Gradient
  const grad = ctx.createLinearGradient(0, 0, FG_W, FG_H);
  grad.addColorStop(0, '#EEF2FF'); // Soft Periwinkle
  grad.addColorStop(0.5, '#E0E7FF');
  grad.addColorStop(1, '#EDE9FE'); // Soft Lavender
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, FG_W, FG_H);

  // Soft Ambient Lighting Glows
  ctx.save();
  const glow1 = ctx.createRadialGradient(250, 160, 20, 250, 160, 360);
  glow1.addColorStop(0, 'rgba(255, 255, 255, 0.75)');
  glow1.addColorStop(1, 'rgba(255, 255, 255, 0)');
  ctx.fillStyle = glow1;
  ctx.fillRect(0, 0, FG_W, FG_H);

  const glow2 = ctx.createRadialGradient(780, 280, 30, 780, 280, 320);
  glow2.addColorStop(0, 'rgba(99, 102, 241, 0.18)');
  glow2.addColorStop(1, 'rgba(99, 102, 241, 0)');
  ctx.fillStyle = glow2;
  ctx.fillRect(0, 0, FG_W, FG_H);
  ctx.restore();

  // LEFT SIDE: Branding & Copy
  // Pill Badge
  ctx.save();
  ctx.fillStyle = 'rgba(79, 70, 229, 0.10)';
  roundRect(ctx, 60, 58, 220, 34, 17);
  ctx.fill();
  ctx.strokeStyle = 'rgba(79, 70, 229, 0.25)';
  ctx.lineWidth = 1.5;
  roundRect(ctx, 60, 58, 220, 34, 17);
  ctx.stroke();

  ctx.font = '900 13px MontserratBlack';
  ctx.fillStyle = '#4F46E5';
  ctx.textAlign = 'center';
  ctx.fillText('100% FREE & OFFLINE', 170, 80);
  ctx.restore();

  // Main Title
  ctx.save();
  ctx.font = '900 52px MontserratBlack';
  ctx.fillStyle = '#1E1B4B';
  ctx.textAlign = 'left';
  ctx.fillText('StudyTimer', 60, 155);

  // Subtitle
  ctx.font = '900 19px MontserratBlack';
  ctx.fillStyle = '#4338CA';
  ctx.fillText('FOCUS COMPANION & HABIT TRACKER', 60, 195);
  ctx.restore();

  // Bullet Features / Badges
  const badges = [
    '•  Dynamic Focus Dial & Modes',
    '•  Multi-Month Study Heatmaps',
    '•  Syllabus Planner & Habit Matrix',
    '•  Zero Ads & Offline Room DB'
  ];

  ctx.save();
  ctx.font = '900 14px MontserratBlack';
  let badgeY = 250;
  for (const b of badges) {
    ctx.fillStyle = '#FFFFFF';
    ctx.shadowColor = 'rgba(15, 23, 42, 0.05)';
    ctx.shadowBlur = 10;
    ctx.shadowOffsetY = 4;
    roundRect(ctx, 60, badgeY - 20, 375, 34, 10);
    ctx.fill();

    ctx.fillStyle = '#312E81';
    ctx.fillText(b, 78, badgeY + 3);
    badgeY += 46;
  }
  ctx.restore();

  // RIGHT SIDE: Angled Floating Real-Sized Mockups
  // Phone 1: Weekly Summary (Back Right)
  const phone1Buf = await sharp('screenshots/raw/screen_06_summary.png')
    .resize(250, 480, { fit: 'cover', position: 'top' })
    .png()
    .toBuffer();
  const phone1Img = await loadImage(phone1Buf);

  ctx.save();
  ctx.translate(700, 65);
  ctx.rotate(0.07);
  ctx.shadowColor = 'rgba(15, 23, 42, 0.22)';
  ctx.shadowBlur = 35;
  ctx.shadowOffsetY = 18;
  
  // Phone 1 Frame
  ctx.fillStyle = '#14141E';
  roundRect(ctx, 0, 0, 230, 440, 22);
  ctx.fill();
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.18)';
  ctx.lineWidth = 2;
  roundRect(ctx, 0, 0, 230, 440, 22);
  ctx.stroke();

  // Clip Image
  ctx.save();
  roundRect(ctx, 5, 5, 220, 430, 18);
  ctx.clip();
  ctx.drawImage(phone1Img, 5, 5, 220, 430);
  ctx.restore();
  ctx.restore();

  // Phone 2: Pomodoro Dial (Front Left)
  const phone2Buf = await sharp('Screenshot/Pomodoro_timer_Screen.jpg')
    .resize(270, 520, { fit: 'cover', position: 'top' })
    .png()
    .toBuffer();
  const phone2Img = await loadImage(phone2Buf);

  ctx.save();
  ctx.translate(500, 85);
  ctx.rotate(-0.06);
  ctx.shadowColor = 'rgba(15, 23, 42, 0.28)';
  ctx.shadowBlur = 45;
  ctx.shadowOffsetY = 24;

  // Phone 2 Frame
  ctx.fillStyle = '#0F0F16';
  roundRect(ctx, 0, 0, 245, 470, 24);
  ctx.fill();
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
  ctx.lineWidth = 2.5;
  roundRect(ctx, 0, 0, 245, 470, 24);
  ctx.stroke();

  // Clip Image
  ctx.save();
  roundRect(ctx, 6, 6, 233, 458, 20);
  ctx.clip();
  ctx.drawImage(phone2Img, 6, 6, 233, 458);
  ctx.restore();
  ctx.restore();

  // Save Feature Graphic
  const outBuf = canvas.toBuffer('image/png');
  fs.writeFileSync(outputPath, outBuf);
  console.log(`Saved soft Feature Graphic (1024x500): ${outputPath}`);
}

generateFeatureGraphic('screenshots/final/feature_graphic_1024x500.png').catch(console.error);
