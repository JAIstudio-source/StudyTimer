import { createCanvas, GlobalFonts, loadImage } from '@napi-rs/canvas';
import fs from 'fs';
import path from 'path';
import sharp from 'sharp';

const CANVAS_W = 1290;
const CANVAS_H = 2796;
const DEVICE_W = 1040;
const BEZEL = 16;
const SCREEN_W = DEVICE_W - 2 * BEZEL; // 1008
const SCREEN_CORNER_R = 56;
const DEVICE_Y = 650;
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

export async function composeScreenshot({ bgHex, verb, desc, screenshotPath, outputPath, topCrop = 80 }) {
  const canvas = createCanvas(CANVAS_W, CANVAS_H);
  const ctx = canvas.getContext('2d');

  // 1. Background Fill
  ctx.fillStyle = bgHex;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

  // Luxury ambient glow behind phone
  const bgGrad = ctx.createRadialGradient(CANVAS_W / 2, 450, 100, CANVAS_W / 2, CANVAS_H / 2, 1100);
  bgGrad.addColorStop(0, 'rgba(255, 255, 255, 0.12)');
  bgGrad.addColorStop(0.6, 'rgba(0, 0, 0, 0.1)');
  bgGrad.addColorStop(1, 'rgba(0, 0, 0, 0.45)');
  ctx.fillStyle = bgGrad;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

  // 2. Text Header
  ctx.textAlign = 'center';
  const MAX_TEXT_W = CANVAS_W * 0.76;

  // Verb
  const verbSize = fitFont(ctx, verb.toUpperCase(), MAX_TEXT_W, 100, 60);
  ctx.font = `900 ${verbSize}px MontserratBlack`;
  ctx.fillStyle = '#FFFFFF';
  ctx.shadowColor = 'rgba(0, 0, 0, 0.4)';
  ctx.shadowBlur = 15;
  ctx.shadowOffsetY = 6;
  ctx.fillText(verb.toUpperCase(), CANVAS_W / 2, 270);

  // Descriptor (Support multiline)
  ctx.shadowBlur = 10;
  ctx.shadowOffsetY = 4;
  ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
  
  const words = desc.toUpperCase().split(' ');
  const lines = [];
  let curLine = '';
  ctx.font = '900 46px MontserratBlack';

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

  let textY = 360;
  const descSize = fitFont(ctx, lines[0] || desc.toUpperCase(), MAX_TEXT_W, 46, 36);
  ctx.font = `900 ${descSize}px MontserratBlack`;
  
  for (const line of lines) {
    ctx.fillText(line, CANVAS_W / 2, textY);
    textY += descSize + 16;
  }

  // Reset shadow for phone drawing
  ctx.shadowColor = 'transparent';
  ctx.shadowBlur = 0;
  ctx.shadowOffsetY = 0;

  // 3. Device Mockup Geometry
  const deviceX = Math.round((CANVAS_W - DEVICE_W) / 2);
  const screenX = deviceX + BEZEL;
  const screenY = DEVICE_Y + BEZEL;
  const totalScreenHeight = CANVAS_H - screenY + 300;

  // Process Screenshot
  const rawMeta = await sharp(screenshotPath).metadata();
  const cropT = Math.min(topCrop, rawMeta.height - 150);
  const cropH = rawMeta.height - cropT;

  const processedShotBuffer = await sharp(screenshotPath)
    .extract({ left: 0, top: cropT, width: rawMeta.width, height: cropH })
    .resize({ width: SCREEN_W })
    .png()
    .toBuffer();

  const shotImg = await loadImage(processedShotBuffer);

  // Phone outer shadow
  ctx.save();
  ctx.shadowColor = 'rgba(0, 0, 0, 0.7)';
  ctx.shadowBlur = 70;
  ctx.shadowOffsetY = 35;
  ctx.fillStyle = '#14141E';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, CANVAS_H - DEVICE_Y + 200, SCREEN_CORNER_R + 12);
  ctx.fill();
  ctx.restore();

  // Phone Bezel Frame Body
  ctx.save();
  ctx.fillStyle = '#1A1A26';
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, CANVAS_H - DEVICE_Y + 200, SCREEN_CORNER_R + 12);
  ctx.fill();

  // Polished Titanium Edge Highlight
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.22)';
  ctx.lineWidth = 3.5;
  roundRect(ctx, deviceX, DEVICE_Y, DEVICE_W, CANVAS_H - DEVICE_Y + 200, SCREEN_CORNER_R + 12);
  ctx.stroke();
  ctx.restore();

  // Screenshot Clipping & Drawing
  ctx.save();
  roundRect(ctx, screenX, screenY, SCREEN_W, totalScreenHeight, SCREEN_CORNER_R);
  ctx.clip();
  ctx.fillStyle = '#050508';
  ctx.fillRect(screenX, screenY, SCREEN_W, totalScreenHeight);
  
  const shotH = Math.round((shotImg.height * SCREEN_W) / shotImg.width);
  ctx.drawImage(shotImg, screenX, screenY, SCREEN_W, shotH);
  ctx.restore();

  // Dynamic Island / Speaker Pill
  ctx.save();
  ctx.fillStyle = '#000000';
  ctx.shadowColor = 'rgba(0, 0, 0, 0.5)';
  ctx.shadowBlur = 10;
  roundRect(ctx, CANVAS_W / 2 - 130, screenY + 18, 260, 52, 26);
  ctx.fill();
  
  // Camera lens reflection
  ctx.fillStyle = 'rgba(40, 45, 70, 0.8)';
  ctx.beginPath();
  ctx.arc(CANVAS_W / 2 + 75, screenY + 44, 11, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  // Write file
  const outBuf = canvas.toBuffer('image/png');
  fs.writeFileSync(outputPath, outBuf);
  console.log(`Successfully built: ${outputPath}`);
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

async function runAll() {
  const items = [
    {
      bgHex: '#4F46E5',
      verb: 'MASTER YOUR FOCUS',
      desc: 'DISTRACTION-FREE AMOLED TIMER & MODES',
      screenshotPath: 'screenshots/raw/screen_01_focus.png',
      outputPath: 'screenshots/01-master-focus/scaffold.png'
    },
    {
      bgHex: '#6366F1',
      verb: 'SMART INSIGHTS',
      desc: 'DAILY AVERAGES, STREAKS & HEATMAPS',
      screenshotPath: 'screenshots/raw/screen_02_overview.png',
      outputPath: 'screenshots/02-deep-insights/scaffold.png'
    },
    {
      bgHex: '#4338CA',
      verb: 'LOG EVERY SESSION',
      desc: 'INTERACTIVE CALENDAR & TIMELINE',
      screenshotPath: 'screenshots/raw/screen_03_calendar.png',
      outputPath: 'screenshots/03-log-sessions/scaffold.png'
    },
    {
      bgHex: '#7C3AED',
      verb: 'DAILY PLANNER',
      desc: 'SYLLABUS TARGETS & HABIT MATRIX',
      screenshotPath: 'screenshots/raw/screen_04_planner.png',
      outputPath: 'screenshots/04-build-habits/scaffold.png'
    },
    {
      bgHex: '#6366F1',
      verb: 'TAG BY SUBJECT',
      desc: 'COLOR-CODED STUDY CATEGORIES',
      screenshotPath: 'screenshots/raw/screen_05_subjects.png',
      outputPath: 'screenshots/05-track-subjects/scaffold.png'
    },
    {
      bgHex: '#581C87',
      verb: 'SHARE MILESTONES',
      desc: 'ELEGANT WEEKLY PROGRESS CARDS',
      screenshotPath: 'screenshots/raw/screen_06_summary.png',
      outputPath: 'screenshots/06-share-milestones/scaffold.png'
    },
    {
      bgHex: '#312E81',
      verb: 'ZERO ADS & OFFLINE',
      desc: '100% PRIVATE WITH CLOUD BACKUPS',
      screenshotPath: 'screenshots/raw/screen_07_settings.png',
      outputPath: 'screenshots/07-zero-ads-offline/scaffold.png'
    }
  ];

  for (const item of items) {
    await composeScreenshot(item);
  }
}

runAll().catch(console.error);
