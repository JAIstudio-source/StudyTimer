import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import middleware from '../middleware.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, '..');

let totalTests = 0;
let passedTests = 0;
let failedTests = [];

function assert(condition, testName, details = '') {
  totalTests++;
  if (condition) {
    passedTests++;
    console.log(`  ✓ PASS: ${testName}`);
  } else {
    failedTests.push({ testName, details });
    console.error(`  ✕ FAIL: ${testName} - ${details}`);
  }
}

console.log('\n======================================================');
console.log('  RUNNING USER-FRIENDLY AGENT READINESS TEST SUITE');
console.log('======================================================\n');

// ---------------------------------------------------------------------------
// TEST 1: Agent-Friendly 404s
// ---------------------------------------------------------------------------
console.log('[1/8] Testing Agent-Friendly 404s & Recovery Body...');
const html404Path = path.join(rootDir, '404.html');
const md404Path = path.join(rootDir, '404.md');
assert(fs.existsSync(html404Path), '404.html exists');
assert(fs.existsSync(md404Path), '404.md exists');

const html404Content = fs.readFileSync(html404Path, 'utf8');
const md404Content = fs.readFileSync(md404Path, 'utf8');
assert(html404Content.length > 500, '404.html has meaningful body > 500 chars');
assert(html404Content.includes('sitemap.xml'), '404.html links to sitemap.xml');
assert(md404Content.includes('https://get-studytimer.vercel.app/'), '404.md provides recovery links');

// ---------------------------------------------------------------------------
// TEST 2: Content Without JavaScript & Sequential Heading Hierarchy
// ---------------------------------------------------------------------------
console.log('\n[2/8] Testing Content Without JavaScript & Heading Hierarchy...');
const htmlFiles = [
  'index.html',
  'about.html',
  'contact.html',
  'privacy.html',
  'terms.html',
  'delete-account.html',
  'thank-you.html',
  '404.html'
];

for (const file of htmlFiles) {
  const filePath = path.join(rootDir, file);
  assert(fs.existsSync(filePath), `${file} exists`);
  const content = fs.readFileSync(filePath, 'utf8');
  assert(content.length >= 500, `${file} raw HTML content length >= 500 chars (actual: ${content.length})`);

  // Parse headings hierarchy
  const headingRegex = /<h([1-6])[\s>]/gi;
  let match;
  const headings = [];
  while ((match = headingRegex.exec(content)) !== null) {
    headings.push(parseInt(match[1], 10));
  }

  assert(headings.length > 0, `${file} has heading tags`);
  assert(headings[0] === 1, `${file} starts with H1`);

  let hierarchyValid = true;
  let skipDetail = '';
  for (let i = 0; i < headings.length - 1; i++) {
    const current = headings[i];
    const next = headings[i + 1];
    if (next > current + 1) {
      hierarchyValid = false;
      skipDetail = `H${current} followed directly by H${next} at index ${i}`;
      break;
    }
  }
  assert(hierarchyValid, `${file} heading hierarchy is strictly sequential (no skipped levels)`, skipDetail);
}

// ---------------------------------------------------------------------------
// TEST 3: Markdown Content Negotiation (acceptmarkdown.com)
// ---------------------------------------------------------------------------
console.log('\n[3/8] Testing Markdown Content Negotiation (acceptmarkdown.com)...');
const vercelConfig = JSON.parse(fs.readFileSync(path.join(rootDir, 'vercel.json'), 'utf8'));

// Check vercel.json global Vary header
const globalHeaders = vercelConfig.headers.find(h => h.source === '/(.*)');
assert(Boolean(globalHeaders), 'vercel.json contains global header rule');
const varyHeader = globalHeaders?.headers.find(h => h.key === 'Vary');
assert(varyHeader && varyHeader.value.includes('Accept'), 'Global headers include Vary: Accept, Accept-Encoding');

// Test Middleware Content Negotiation
const reqWithMarkdown = new Request('https://get-studytimer.vercel.app/', {
  headers: { 'accept': 'text/markdown, text/html;q=0.9' }
});
const middlewareRes = middleware(reqWithMarkdown);
const rewriteHeader = middlewareRes.headers.get('x-middleware-rewrite');
const varyRes = middlewareRes.headers.get('Vary');
assert(rewriteHeader && rewriteHeader.includes('index.md'), 'Middleware rewrites / to /index.md when Accept: text/markdown is sent');
assert(varyRes && varyRes.includes('Accept'), 'Middleware sets Vary: Accept header on negotiated response');

// ---------------------------------------------------------------------------
// TEST 4: Brand Name Discoverability & NAP Consistency
// ---------------------------------------------------------------------------
console.log('\n[4/8] Testing Brand Name Discoverability & NAP Consistency...');
const indexContent = fs.readFileSync(path.join(rootDir, 'index.html'), 'utf8');
assert(indexContent.includes('StudyTimer - Track Your Progress'), 'Title includes prominent brand name StudyTimer');
assert(indexContent.includes('https://get-studytimer.vercel.app/'), 'Canonical URL points to apex domain');
assert(indexContent.includes('studytimer737@gmail.com'), 'Consistent contact email across index.html');
assert(indexContent.includes('500 Howard Street'), 'Consistent postal address in index.html');

// ---------------------------------------------------------------------------
// TEST 5: Agent Instruction / When-to-Use Guidance
// ---------------------------------------------------------------------------
console.log('\n[5/8] Testing Agent Instruction / When-to-Use Guidance...');
const llmsPath = path.join(rootDir, 'llms.txt');
assert(fs.existsSync(llmsPath), 'llms.txt exists');
const llmsContent = fs.readFileSync(llmsPath, 'utf8');
assert(llmsContent.includes('## When to Use StudyTimer'), 'llms.txt contains ## When to Use StudyTimer section');
assert(llmsContent.includes('Best-Fit Use Cases'), 'llms.txt specifies Best-Fit Use Cases');
assert(llmsContent.includes('When NOT to Use StudyTimer'), 'llms.txt specifies negative filtering (When NOT to use)');
assert(llmsContent.includes('https://get-studytimer.vercel.app/StudyTimer-release.apk'), 'llms.txt includes direct download link for agents');

// ---------------------------------------------------------------------------
// TEST 6: Organization Schema Completeness
// ---------------------------------------------------------------------------
console.log('\n[6/8] Testing Organization Schema Completeness...');
const jsonLdMatch = indexContent.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/);
assert(Boolean(jsonLdMatch), 'index.html contains JSON-LD structured data');
const jsonLd = JSON.parse(jsonLdMatch[1]);
const orgSchema = jsonLd['@graph']?.find(item => item['@type'] === 'Organization');
assert(Boolean(orgSchema), 'Organization schema found in @graph');
assert(orgSchema.name === 'StudyTimer', 'Organization name is StudyTimer');
assert(Boolean(orgSchema.contactPoint?.email), 'Organization includes contactPoint with email');
assert(orgSchema.contactPoint.contactType === 'customer support', 'Organization contactPoint has contactType');
assert(Boolean(orgSchema.address), 'Organization includes address');
assert(orgSchema.address['@type'] === 'PostalAddress', 'Organization address type is PostalAddress');
assert(Boolean(orgSchema.address.addressLocality), 'Organization address includes addressLocality');
assert(Boolean(orgSchema.address.addressCountry), 'Organization address includes addressCountry');

// ---------------------------------------------------------------------------
// TEST 7: Trust Anchor Pages (/about, /contact, /privacy)
// ---------------------------------------------------------------------------
console.log('\n[7/8] Testing Trust Anchor Pages (/about, /contact, /privacy)...');
const aboutContent = fs.readFileSync(path.join(rootDir, 'about.html'), 'utf8');
const contactContent = fs.readFileSync(path.join(rootDir, 'contact.html'), 'utf8');
const privacyContent = fs.readFileSync(path.join(rootDir, 'privacy.html'), 'utf8');

assert(aboutContent.length >= 1000, `About page length >= 1000 chars (actual: ${aboutContent.length})`);
assert(contactContent.length >= 1000, `Contact page length >= 1000 chars (actual: ${contactContent.length})`);
assert(privacyContent.length >= 1000, `Privacy page length >= 1000 chars (actual: ${privacyContent.length})`);

assert(aboutContent.includes('About StudyTimer'), 'About page has clear H1 title');
assert(contactContent.includes('studytimer737@gmail.com'), 'Contact page includes support email');
assert(privacyContent.includes('Data Safety'), 'Privacy page includes data safety section');

// ---------------------------------------------------------------------------
// TEST 8: SoftwareApplication JSON-LD Completeness
// ---------------------------------------------------------------------------
console.log('\n[8/8] Testing SoftwareApplication JSON-LD Completeness...');
const appSchema = jsonLd['@graph']?.find(item => item['@type'] === 'SoftwareApplication');
assert(Boolean(appSchema), 'SoftwareApplication schema found in @graph');
assert(appSchema.name === 'StudyTimer', 'SoftwareApplication name is StudyTimer');
assert(Boolean(appSchema.url), 'SoftwareApplication includes url');
assert(appSchema.applicationCategory === 'ProductivityApplication', 'SoftwareApplication category is ProductivityApplication');
assert(Boolean(appSchema.offers), 'SoftwareApplication includes offers');
assert(appSchema.offers.price === '0', 'SoftwareApplication price is 0');
assert(Boolean(appSchema.downloadUrl), 'SoftwareApplication includes downloadUrl');
assert(Boolean(appSchema.featureList && appSchema.featureList.length > 0), 'SoftwareApplication includes featureList');

// ---------------------------------------------------------------------------
// FINAL SUMMARY
// ---------------------------------------------------------------------------
console.log('\n======================================================');
console.log(`  RESULTS: ${passedTests}/${totalTests} TESTS PASSED`);
if (failedTests.length === 0) {
  console.log('  ALL USER-FRIENDLY READINESS TESTS PASSED SUCCESSFULLY!');
} else {
  console.error(`  ${failedTests.length} TESTS FAILED:`);
  for (const f of failedTests) {
    console.error(`  - ${f.testName}: ${f.details}`);
  }
}
console.log('======================================================\n');

if (failedTests.length > 0) {
  process.exit(1);
}
