import sharp from 'sharp';
import fs from 'fs';
import path from 'path';

const screenshotsDir = './assets/screenshots';
const files = fs.readdirSync(screenshotsDir);

console.log('Optimizing screenshot images to WebP...');

for (const file of files) {
  if (file.endsWith('.jpg') || file.endsWith('.png')) {
    const inputPath = path.join(screenshotsDir, file);
    const outputPath = path.join(screenshotsDir, file.replace(/\.(jpg|png)$/, '.webp'));
    
    const inputStats = fs.statSync(inputPath);
    await sharp(inputPath)
      .webp({ quality: 82, effort: 6 })
      .toFile(outputPath);
      
    const outputStats = fs.statSync(outputPath);
    const savings = Math.round((1 - outputStats.size / inputStats.size) * 100);
    console.log(`✓ ${file} (${Math.round(inputStats.size / 1024)}KB) -> ${path.basename(outputPath)} (${Math.round(outputStats.size / 1024)}KB) [${savings}% smaller]`);
  }
}

console.log('Image optimization complete!');
