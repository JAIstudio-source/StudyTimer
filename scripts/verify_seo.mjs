import fs from 'fs';
import path from 'path';

const files = fs.readdirSync('.').filter(f => f.endsWith('.html'));
const report = [];

files.forEach(file => {
  const content = fs.readFileSync(file, 'utf8');
  
  const titleMatch = content.match(/<title>([\s\S]*?)<\/title>/i);
  const title = titleMatch ? titleMatch[1].replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/\s+/g, ' ').trim() : 'MISSING';
  
  const descMatch = content.match(/<meta\s+name=["']description["']\s+content=["']([\s\S]*?)["']/i);
  const desc = descMatch ? descMatch[1].replace(/\s+/g, ' ').trim() : 'MISSING';
  
  const h1Match = content.match(/<h1[\s\S]*?>([\s\S]*?)<\/h1>/i);
  let h1 = 'MISSING';
  if (h1Match) {
    h1 = h1Match[1].replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();
  }

  // Check SERP thresholds
  // Google typically truncates titles over ~60 chars (or 600px)
  // Google typically truncates snippets over ~160 chars, while < 100 chars might be considered thin
  let titleStatus = 'Optimal';
  if (title.length > 65) titleStatus = 'Slightly Long (May truncate on mobile)';
  if (title.length < 30) titleStatus = 'Too Short';

  let descStatus = 'Optimal';
  if (desc.length > 165) descStatus = 'Slightly Long (May truncate in SERP)';
  if (desc.length < 80 && file !== '404.html') descStatus = 'Too Short';

  report.push({
    file,
    title,
    titleLen: title.length,
    titleStatus,
    desc,
    descLen: desc.length,
    descStatus,
    h1
  });
});

console.log(JSON.stringify(report, null, 2));
