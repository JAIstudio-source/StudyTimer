import sharp from 'sharp';
import path from 'path';

const WIDTH = 1200;
const HEIGHT = 630;

const imgInsights = 'C:/Users/user/.gemini/antigravity/brain/733fa789-9c40-435d-84f4-7b894a21b1f7/.user_uploaded/media_1795757370183_98c28a53.jpg';
const imgTimer = 'C:/Users/user/.gemini/antigravity/brain/733fa789-9c40-435d-84f4-7b894a21b1f7/.user_uploaded/media_1795757370187_4f3f336b.jpg';
const outPath = path.resolve('assets/og-preview.png');

async function makePhoneCard(inputPath, w, h, radius = 26) {
  const resized = await sharp(inputPath)
    .resize(w, h, { fit: 'cover', position: 'center' })
    .toBuffer();

  const mask = Buffer.from(
    `<svg width="${w}" height="${h}">
      <rect x="0" y="0" width="${w}" height="${h}" rx="${radius}" ry="${radius}" fill="#fff"/>
    </svg>`
  );

  const rounded = await sharp(resized)
    .composite([{ input: mask, blend: 'dest-in' }])
    .png()
    .toBuffer();

  // Add subtle dark bezel/border + soft shadow via SVG wrapper
  const pad = 18;
  const totalW = w + pad * 2;
  const totalH = h + pad * 2;
  const frameSvg = Buffer.from(
    `<svg width="${totalW}" height="${totalH}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <filter id="shadow" x="-15%" y="-10%" width="130%" height="130%">
          <feDropShadow dx="0" dy="14" stdDeviation="12" flood-color="#0f172a" flood-opacity="0.16"/>
        </filter>
      </defs>
      <rect x="${pad - 3}" y="${pad - 3}" width="${w + 6}" height="${h + 6}" rx="${radius + 3}" ry="${radius + 3}" fill="#0f172a" filter="url(#shadow)"/>
    </svg>`
  );

  const cameraSvg = Buffer.from(
    `<svg width="${totalW}" height="${totalH}" xmlns="http://www.w3.org/2000/svg">
      <circle cx="${Math.round(totalW / 2)}" cy="${pad + 11}" r="5.5" fill="#0f172a"/>
      <circle cx="${Math.round(totalW / 2) - 1}" cy="${pad + 10}" r="1.5" fill="#334155"/>
    </svg>`
  );

  return sharp(frameSvg)
    .composite([
      { input: rounded, left: pad, top: pad },
      { input: cameraSvg, left: 0, top: 0 }
    ])
    .png()
    .toBuffer();
}

async function run() {
  const bgSvg = Buffer.from(`
    <svg width="${WIDTH}" height="${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stop-color="#f8fafc"/>
          <stop offset="100%" stop-color="#eef4ff"/>
        </linearGradient>
      </defs>
      <rect width="${WIDTH}" height="${HEIGHT}" fill="url(#bg)"/>

      <!-- Top Pill -->
      <rect x="64" y="88" width="268" height="36" rx="18" fill="#e0edff"/>
      <text x="198" y="111" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="14" font-weight="700" letter-spacing="0.8" fill="#1d4ed8">100% FREE • NO ADS • OFFLINE</text>

      <!-- Brand Title -->
      <text x="64" y="198" font-family="Segoe UI, Arial, sans-serif" font-size="68" font-weight="800" letter-spacing="-1.5" fill="#09090b">StudyTimer</text>

      <!-- Short, Student-Friendly Sentence -->
      <text x="64" y="252" font-family="Segoe UI, Arial, sans-serif" font-size="27" font-weight="600" fill="#334155">Track your study hours and stay focused.</text>

      <!-- 3 Clean Points (No Jargon) -->
      <circle cx="76" cy="332" r="6" fill="#2563eb"/>
      <text x="98" y="340" font-family="Segoe UI, Arial, sans-serif" font-size="22" font-weight="600" fill="#1e293b">Pomodoro &amp; stopwatch timer</text>

      <circle cx="76" cy="388" r="6" fill="#2563eb"/>
      <text x="98" y="396" font-family="Segoe UI, Arial, sans-serif" font-size="22" font-weight="600" fill="#1e293b">See time spent per subject</text>

      <circle cx="76" cy="444" r="6" fill="#2563eb"/>
      <text x="98" y="452" font-family="Segoe UI, Arial, sans-serif" font-size="22" font-weight="600" fill="#1e293b">Works on Android &amp; Web</text>

      <!-- Footer URL -->
      <text x="64" y="538" font-family="Segoe UI, Arial, sans-serif" font-size="20" font-weight="700" fill="#2563eb">studytimer.site</text>
    </svg>
  `);

  const phoneTimer = await makePhoneCard(imgTimer, 224, 480, 24);
  const phoneInsights = await makePhoneCard(imgInsights, 236, 506, 24);

  await sharp(bgSvg)
    .composite([
      { input: phoneTimer, left: 650, top: 68 },
      { input: phoneInsights, left: 890, top: 44 }
    ])
    .png({ quality: 95 })
    .toFile(outPath);

  console.log('Generated', outPath);
}

run().catch(err => {
  console.error(err);
  process.exit(1);
});
