import fs from 'fs';
import path from 'path';

const htmlFiles = [
  'index.html',
  'demo.html',
  'about.html',
  'contact.html',
  'privacy.html',
  'terms.html',
  'delete-account.html',
  'thank-you.html',
  '404.html'
];

const newOgImage = 'https://get-studytimer.vercel.app/assets/feature_graphic_1024x500.png';

htmlFiles.forEach(file => {
  if (!fs.existsSync(file)) return;
  let content = fs.readFileSync(file, 'utf8');

  // Replace og:image
  content = content.replace(
    /<meta\s+property=["']og:image["']\s+content=["'][^"']*["']\s*\/?>/gi,
    `<meta property="og:image" content="${newOgImage}">\n  <meta property="og:image:width" content="1024">\n  <meta property="og:image:height" content="500">\n  <meta property="og:image:type" content="image/png">`
  );

  // Replace twitter:card
  content = content.replace(
    /<meta\s+property=["']twitter:card["']\s+content=["'][^"']*["']\s*\/?>/gi,
    `<meta property="twitter:card" content="summary_large_image">`
  );

  // Replace twitter:image
  content = content.replace(
    /<meta\s+property=["']twitter:image["']\s+content=["'][^"']*["']\s*\/?>/gi,
    `<meta property="twitter:image" content="${newOgImage}">`
  );

  // Replace schema.org image if present
  content = content.replace(
    /"image":\s*"https:\/\/get-studytimer\.vercel\.app\/assets\/[^"]*"/g,
    `"image": "${newOgImage}"`
  );

  fs.writeFileSync(file, content, 'utf8');
  console.log(`Updated social preview tags in ${file}`);
});
