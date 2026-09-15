import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const FG_W = 1024;
const FG_H = 500;
const FONT_BLACK = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Black.ttf');
const FONT_REGULAR = path.join(process.cwd(), '.agents', 'skills', 'aso-screenshots', 'assets', 'Montserrat-Regular.ttf');

GlobalFonts.registerFromPath(FONT_BLACK, 'MontserratBlack');
GlobalFonts.registerFromPath(FONT_REGULAR, 'MontserratRegular');

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

  // 1. Soft Elegant Studio Background
  const grad = ctx.createLinearGradient(0, 0, FG_W, FG_H);
  grad.addColorStop(0, '#F8FAFD');
  grad.addColorStop(0.5, '#F1F5FA');
  grad.addColorStop(1, '#E6EDF7');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, FG_W, FG_H);

  // Ambient Glows
  ctx.save();
  const ambientGlow1 = ctx.createRadialGradient(220, 160, 20, 220, 160, 340);
  ambientGlow1.addColorStop(0, 'rgba(255, 255, 255, 0.95)');
  ambientGlow1.addColorStop(1, 'rgba(255, 255, 255, 0)');
  ctx.fillStyle = ambientGlow1;
  ctx.fillRect(0, 0, FG_W, FG_H);

  const ambientGlow2 = ctx.createRadialGradient(780, 250, 40, 780, 250, 400);
  ambientGlow2.addColorStop(0, 'rgba(224, 231, 255, 0.9)');
  ambientGlow2.addColorStop(1, 'rgba(224, 231, 255, 0)');
  ctx.fillStyle = ambientGlow2;
  ctx.fillRect(0, 0, FG_W, FG_H);
  ctx.restore();

  // 2. LEFT SIDE: ENLARGED DETAILS
  // Top Pill Badge
  ctx.save();
  const badgeX = 48;
  const badgeY = 38;
  const badgeW = 230;
  const badgeH = 38;
  const badgeR = 19;
  ctx.fillStyle = '#E8ECFD';
  roundRect(ctx, badgeX, badgeY, badgeW, badgeH, badgeR);
  ctx.fill();
  ctx.strokeStyle = '#CBD5FE';
  ctx.lineWidth = 1.6;
  roundRect(ctx, badgeX, badgeY, badgeW, badgeH, badgeR);
  ctx.stroke();

  ctx.font = '900 13px MontserratBlack';
  ctx.fillStyle = '#4F46E5';
  ctx.textAlign = 'center';
  ctx.fillText('100% FREE & OFFLINE', badgeX + badgeW / 2, badgeY + 24);
  ctx.restore();

  // Main Title: StudyTimer
  ctx.save();
  ctx.font = '900 58px MontserratBlack';
  ctx.fillStyle = '#16163F';
  ctx.textAlign = 'left';
  ctx.fillText('StudyTimer', 48, 140);

  // Subtitle: FOCUS COMPANION & GOAL TRACKER
  ctx.font = '900 18px MontserratBlack';
  ctx.fillStyle = '#4338CA';
  ctx.fillText('FOCUS COMPANION & GOAL TRACKER', 48, 178);
  ctx.restore();

  // Feature Cards (4 Exact items from reference)
  const items = [
    'Multi study mode - pomodoro or subject wise',
    'Daily study Graph and Charts',
    'Full month goal calendar',
    'Zero ads, 100% offline'
  ];

  ctx.save();
  let cardY = 224;
  const cardW = 430;
  const cardH = 40;
  const cardR = 14;

  for (const itemText of items) {
    ctx.save();
    ctx.shadowColor = 'rgba(20, 25, 50, 0.07)';
    ctx.shadowBlur = 12;
    ctx.shadowOffsetY = 4;
    ctx.fillStyle = '#FFFFFF';
    roundRect(ctx, 48, cardY - 24, cardW, cardH, cardR);
    ctx.fill();
    ctx.restore();

    // Purple bullet dot
    ctx.fillStyle = '#4F46E5';
    ctx.beginPath();
    ctx.arc(72, cardY - 4, 5, 0, Math.PI * 2);
    ctx.fill();

    // Item text
    ctx.font = '900 13.5px MontserratBlack';
    ctx.fillStyle = '#222055';
    ctx.fillText(itemText, 88, cardY + 1.5);

    cardY += 52;
  }
  ctx.restore();

  // 3. RIGHT SIDE: TALLER, SLIMMER FLAGSHIP PHONES (19.5:9 Aspect)
  const BEZEL = 6;
  const PHONE_SCREEN_W = 180;
  const PHONE_SCREEN_H = 384;
  const PHONE_DEVICE_W = PHONE_SCREEN_W + 2 * BEZEL; // 192px
  const PHONE_DEVICE_H = PHONE_SCREEN_H + 2 * BEZEL; // 396px
  const PHONE_R = 26;

  // Render screenshots to exact width so 0% of header text is cropped
  const shotImgH = Math.round((PHONE_SCREEN_W * 2169) / 1220); // 320px

  const phone1Buf = await sharp('screenshots/assets/screen_02_insight.jpg')
    .resize({ width: PHONE_SCREEN_W, height: shotImgH })
    .png()
    .toBuffer();
  const phone1Img = await loadImage(phone1Buf);

  const phone2Buf = await sharp('screenshots/assets/screen_09_pomodoro.jpg')
    .resize({ width: PHONE_SCREEN_W, height: shotImgH })
    .png()
    .toBuffer();
  const phone2Img = await loadImage(phone2Buf);

  // Placement Coordinates
  const phone1X = 776;
  const phone1Y = 44;

  const phone2X = 570;
  const phone2Y = 74;

  // --- DRAW PHONE 1 (Insights - Back Right) ---
  ctx.save();
  // Multi-tier drop shadow
  ctx.shadowColor = 'rgba(15, 23, 42, 0.24)';
  ctx.shadowBlur = 38;
  ctx.shadowOffsetY = 18;
  ctx.fillStyle = '#0B0B14';
  roundRect(ctx, phone1X, phone1Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();

  ctx.shadowColor = 'rgba(15, 23, 42, 0.10)';
  ctx.shadowBlur = 12;
  ctx.shadowOffsetY = 6;
  roundRect(ctx, phone1X, phone1Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();
  ctx.restore();

  // Phone 1 Body & Chamfer Highlight
  ctx.save();
  ctx.fillStyle = '#12121E';
  roundRect(ctx, phone1X, phone1Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
  ctx.lineWidth = 1.8;
  roundRect(ctx, phone1X, phone1Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.stroke();

  // Phone 1 Screen Content Clip
  roundRect(ctx, phone1X + BEZEL, phone1Y + BEZEL, PHONE_SCREEN_W, PHONE_SCREEN_H, PHONE_R - 4);
  ctx.clip();
  ctx.fillStyle = '#000000';
  ctx.fillRect(phone1X + BEZEL, phone1Y + BEZEL, PHONE_SCREEN_W, PHONE_SCREEN_H);
  ctx.drawImage(phone1Img, phone1X + BEZEL, phone1Y + BEZEL, PHONE_SCREEN_W, shotImgH);
  ctx.restore();

  // Phone 1 Camera Punch Hole (top center)
  ctx.save();
  ctx.fillStyle = '#05050A';
  ctx.beginPath();
  ctx.arc(phone1X + PHONE_DEVICE_W / 2, phone1Y + BEZEL + 8, 3.5, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  // --- DRAW PHONE 2 (Pomodoro - Front Left) ---
  ctx.save();
  // Deep multi-tier shadow casting over background AND over Phone 1
  ctx.shadowColor = 'rgba(15, 23, 42, 0.34)';
  ctx.shadowBlur = 46;
  ctx.shadowOffsetY = 24;
  ctx.fillStyle = '#0B0B14';
  roundRect(ctx, phone2X, phone2Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();

  ctx.shadowColor = 'rgba(15, 23, 42, 0.14)';
  ctx.shadowBlur = 14;
  ctx.shadowOffsetY = 7;
  roundRect(ctx, phone2X, phone2Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();
  ctx.restore();

  // Phone 2 Body & Chamfer Highlight
  ctx.save();
  ctx.fillStyle = '#12121E';
  roundRect(ctx, phone2X, phone2Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.fill();

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.28)';
  ctx.lineWidth = 1.8;
  roundRect(ctx, phone2X, phone2Y, PHONE_DEVICE_W, PHONE_DEVICE_H, PHONE_R);
  ctx.stroke();

  // Phone 2 Screen Content Clip
  roundRect(ctx, phone2X + BEZEL, phone2Y + BEZEL, PHONE_SCREEN_W, PHONE_SCREEN_H, PHONE_R - 4);
  ctx.clip();
  ctx.fillStyle = '#000000';
  ctx.fillRect(phone2X + BEZEL, phone2Y + BEZEL, PHONE_SCREEN_W, PHONE_SCREEN_H);
  ctx.drawImage(phone2Img, phone2X + BEZEL, phone2Y + BEZEL, PHONE_SCREEN_W, shotImgH);
  ctx.restore();

  // Phone 2 Camera Punch Hole (top center)
  ctx.save();
  ctx.fillStyle = '#05050A';
  ctx.beginPath();
  ctx.arc(phone2X + PHONE_DEVICE_W / 2, phone2Y + BEZEL + 8, 3.5, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  // Save Feature Graphic
  const outBuf = canvas.toBuffer('image/png');
  fs.writeFileSync(outputPath, outBuf);
  console.log(`Generated Feature Graphic at: ${outputPath}`);
}

generateFeatureGraphic('screenshots/final/feature_graphic_1024x500.png').catch(console.error);
