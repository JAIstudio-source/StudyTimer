import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import mcpHandler from '../api/mcp.js';
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
console.log('  RUNNING AGENT READINESS & ORA AUDIT TEST SUITE');
console.log('======================================================\n');

// ---------------------------------------------------------------------------
// TEST 1: Agent-Friendly 404s
// ---------------------------------------------------------------------------
console.log('[1/10] Testing Agent-Friendly 404s & Recovery Body...');
const html404Path = path.join(rootDir, '404.html');
const md404Path = path.join(rootDir, '404.md');
assert(fs.existsSync(html404Path), '404.html exists');
assert(fs.existsSync(md404Path), '404.md exists');

const html404Content = fs.readFileSync(html404Path, 'utf8');
const md404Content = fs.readFileSync(md404Path, 'utf8');
assert(html404Content.length > 500, '404.html has meaningful body > 500 chars');
assert(html404Content.includes('llms.txt') && html404Content.includes('sitemap.xml'), '404.html links to sitemap and llms.txt');
assert(md404Content.includes('https://get-studytimer.vercel.app/llms.txt'), '404.md provides markdown recovery links');

// ---------------------------------------------------------------------------
// TEST 2: Content Without JavaScript & Sequential Heading Hierarchy
// ---------------------------------------------------------------------------
console.log('\n[2/10] Testing Content Without JavaScript & Heading Hierarchy...');
const htmlFiles = [
  'index.html',
  'about.html',
  'contact.html',
  'docs.html',
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
console.log('\n[3/10] Testing Markdown Content Negotiation (acceptmarkdown.com)...');
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
// TEST 4: Developer Resource Discoverability
// ---------------------------------------------------------------------------
console.log('\n[4/10] Testing Developer Resource Discoverability...');
const openapiPath = path.join(rootDir, 'openapi.json');
const openapiYamlPath = path.join(rootDir, 'openapi.yaml');
const docsPath = path.join(rootDir, 'docs.html');
const llmsPath = path.join(rootDir, 'llms.txt');

assert(fs.existsSync(openapiPath), 'openapi.json exists');
assert(fs.existsSync(openapiYamlPath), 'openapi.yaml exists');
assert(fs.existsSync(docsPath), 'docs.html developer portal exists');
assert(fs.existsSync(llmsPath), 'llms.txt exists');

const openapi = JSON.parse(fs.readFileSync(openapiPath, 'utf8'));
assert(openapi.openapi.startsWith('3.1'), 'OpenAPI version is 3.1.0');
assert(openapi.info.title.includes('StudyTimer'), 'OpenAPI title contains StudyTimer');
assert(Boolean(openapi.paths['/api/feedback']), 'OpenAPI includes /api/feedback');
assert(Boolean(openapi.paths['/.well-known/mcp']), 'OpenAPI includes /.well-known/mcp');

// ---------------------------------------------------------------------------
// TEST 5: Brand Name Discoverability & NAP Consistency
// ---------------------------------------------------------------------------
console.log('\n[5/10] Testing Brand Name Discoverability & NAP Consistency...');
const indexContent = fs.readFileSync(path.join(rootDir, 'index.html'), 'utf8');
assert(indexContent.includes('StudyTimer - Track Your Progress'), 'Title includes prominent brand name StudyTimer');
assert(indexContent.includes('https://get-studytimer.vercel.app/'), 'Canonical URL points to apex domain');
assert(indexContent.includes('studytimer737@gmail.com'), 'Consistent contact email across index.html');
assert(indexContent.includes('500 Howard Street'), 'Consistent postal address in index.html');

// ---------------------------------------------------------------------------
// TEST 6: Agent Instruction / When-to-Use Guidance
// ---------------------------------------------------------------------------
console.log('\n[6/10] Testing Agent Instruction / When-to-Use Guidance...');
const llmsContent = fs.readFileSync(llmsPath, 'utf8');
assert(llmsContent.includes('## When to Use StudyTimer'), 'llms.txt contains ## When to Use StudyTimer section');
assert(llmsContent.includes('Best-Fit Use Cases'), 'llms.txt specifies Best-Fit Use Cases');
assert(llmsContent.includes('When NOT to Use StudyTimer'), 'llms.txt specifies negative filtering (When NOT to use)');
assert(llmsContent.includes('https://get-studytimer.vercel.app/StudyTimer-release.apk'), 'llms.txt includes direct download link for agents');

// ---------------------------------------------------------------------------
// TEST 7: Organization Schema Completeness
// ---------------------------------------------------------------------------
console.log('\n[7/10] Testing Organization Schema Completeness...');
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
// TEST 8: Trust Anchor Pages (/about, /contact, /privacy)
// ---------------------------------------------------------------------------
console.log('\n[8/10] Testing Trust Anchor Pages (/about, /contact, /privacy)...');
const aboutContent = fs.readFileSync(path.join(rootDir, 'about.html'), 'utf8');
const contactContent = fs.readFileSync(path.join(rootDir, 'contact.html'), 'utf8');
const privacyContent = fs.readFileSync(path.join(rootDir, 'privacy.html'), 'utf8');

assert(aboutContent.length >= 1000, `About page length >= 1000 chars (actual: ${aboutContent.length})`);
assert(contactContent.length >= 1000, `Contact page length >= 1000 chars (actual: ${contactContent.length})`);
assert(privacyContent.length >= 1000, `Privacy page length >= 1000 chars (actual: ${privacyContent.length})`);

assert(privacyContent.includes('Data Safety'), 'Privacy page includes data safety section');

// ---------------------------------------------------------------------------
// TEST 9: SoftwareApplication JSON-LD Completeness
// ---------------------------------------------------------------------------
console.log('\n[9/10] Testing SoftwareApplication JSON-LD Completeness...');
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
// TEST 10: Model Context Protocol (MCP) Server & Manifest Handshake
// ---------------------------------------------------------------------------
console.log('\n[10/10] Testing MCP Server & Manifest Handshake...');
const mcpManifestPath = path.join(rootDir, '.well-known', 'mcp.json');
assert(fs.existsSync(mcpManifestPath), '.well-known/mcp.json exists');

// Simulate MCP GET Handshake
let getStatusCode = 0;
let getHeaders = {};
let getJsonBody = null;

const fakeGetRes = {
  setHeader(k, v) { getHeaders[k] = v; },
  status(code) { getStatusCode = code; return this; },
  json(body) { getJsonBody = body; return this; }
};

await mcpHandler({ method: 'GET' }, fakeGetRes);
assert(getStatusCode === 200, 'MCP GET handshake returns HTTP 200');
assert(getJsonBody?.protocolVersion === '2024-11-05', 'MCP protocol version is 2024-11-05');
assert(getJsonBody?.serverInfo?.name === 'studytimer-mcp-server', 'MCP server name matches studytimer-mcp-server');
assert(Array.isArray(getJsonBody?.tools) && getJsonBody.tools.length >= 5, 'MCP exposes 5 tools');

// Simulate MCP JSON-RPC 'initialize'
let rpcStatusCode = 0;
let rpcJsonBody = null;
const fakeRpcRes = {
  setHeader() {},
  status(code) { rpcStatusCode = code; return this; },
  json(body) { rpcJsonBody = body; return this; }
};

await mcpHandler({
  method: 'POST',
  body: {
    jsonrpc: '2.0',
    id: 1,
    method: 'initialize',
    params: {
      protocolVersion: '2024-11-05',
      clientInfo: { name: 'ora-evaluator', version: '1.0' }
    }
  }
}, fakeRpcRes);

assert(rpcStatusCode === 200, 'MCP initialize returns HTTP 200');
assert(rpcJsonBody?.result?.protocolVersion === '2024-11-05', 'MCP initialize returns protocol version');

// Simulate MCP JSON-RPC 'tools/call' for 'calculate_pomodoro_schedule'
let toolStatusCode = 0;
let toolJsonBody = null;
const fakeToolRes = {
  setHeader() {},
  status(code) { toolStatusCode = code; return this; },
  json(body) { toolJsonBody = body; return this; }
};

await mcpHandler({
  method: 'POST',
  body: {
    jsonrpc: '2.0',
    id: 2,
    method: 'tools/call',
    params: {
      name: 'calculate_pomodoro_schedule',
      arguments: { total_minutes: 60, work_block_minutes: 25, short_break_minutes: 5 }
    }
  }
}, fakeToolRes);

assert(toolStatusCode === 200, 'MCP tools/call returns HTTP 200');
assert(!toolJsonBody?.result?.isError, 'MCP tools/call executes calculate_pomodoro_schedule without error');
const toolOutput = JSON.parse(toolJsonBody.result.content[0].text);
assert(toolOutput.completed_cycles === 2, 'calculate_pomodoro_schedule correctly calculates 2 cycles for 60m');

// ---------------------------------------------------------------------------
// FINAL SUMMARY
// ---------------------------------------------------------------------------
console.log('\n======================================================');
console.log(`  RESULTS: ${passedTests}/${totalTests} TESTS PASSED`);
if (failedTests.length === 0) {
  console.log('  ALL 10 AGENT READINESS FIXES VERIFIED SUCCESSFULLY!');
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
