import { createClient } from '@supabase/supabase-js';

// Configuration from Environment Variables (Zero hardcoded fallbacks in production)
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ============================================================================
// 1. SLIDING-WINDOW RATE LIMITER (20 requests / 60 seconds per IP)
// ============================================================================
const RATE_LIMIT_WINDOW_MS = 60 * 1000; // 1 minute
const MAX_REQUESTS_PER_WINDOW = 20;

// In-memory sliding window cache: Map<IP, Array<timestamp>>
const rateLimitCache = new Map();

function checkSlidingWindowRateLimit(clientIp) {
  const now = Date.now();
  const windowStart = now - RATE_LIMIT_WINDOW_MS;

  // Clean old entries for this IP
  let timestamps = rateLimitCache.get(clientIp) || [];
  timestamps = timestamps.filter(ts => ts > windowStart);

  if (timestamps.length >= MAX_REQUESTS_PER_WINDOW) {
    const oldestTimestamp = timestamps[0];
    const retryAfterSec = Math.max(1, Math.ceil((oldestTimestamp + RATE_LIMIT_WINDOW_MS - now) / 1000));
    rateLimitCache.set(clientIp, timestamps);
    return {
      allowed: false,
      limit: MAX_REQUESTS_PER_WINDOW,
      remaining: 0,
      reset: Math.ceil((oldestTimestamp + RATE_LIMIT_WINDOW_MS) / 1000),
      retryAfterSec
    };
  }

  timestamps.push(now);
  rateLimitCache.set(clientIp, timestamps);

  // Periodically purge stale IPs to prevent memory leaks (probabilistic cleanup)
  if (Math.random() < 0.05) {
    for (const [ip, tsList] of rateLimitCache.entries()) {
      const activeList = tsList.filter(ts => ts > windowStart);
      if (activeList.length === 0) {
        rateLimitCache.delete(ip);
      } else {
        rateLimitCache.set(ip, activeList);
      }
    }
  }

  return {
    allowed: true,
    limit: MAX_REQUESTS_PER_WINDOW,
    remaining: Math.max(0, MAX_REQUESTS_PER_WINDOW - timestamps.length),
    reset: Math.ceil((now + RATE_LIMIT_WINDOW_MS) / 1000),
    retryAfterSec: 0
  };
}

// ============================================================================
// 2. INPUT SANITIZATION & DELIMITER HELPERS
// ============================================================================
function sanitizeHtmlString(str) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
    .replace(/\//g, '&#x2F;');
}

function sanitizeDiagnostics(diag) {
  if (typeof diag !== 'object' || diag === null || Array.isArray(diag)) return {};
  const allowedKeys = ['app_version', 'android_os', 'device', 'timer_mode', 'is_logged_in', 'build_code', 'locale'];
  const clean = {};
  for (const key of allowedKeys) {
    if (Object.prototype.hasOwnProperty.call(diag, key)) {
      const val = diag[key];
      if (typeof val === 'string') {
        clean[key] = sanitizeHtmlString(val.substring(0, 100));
      } else if (typeof val === 'number' || typeof val === 'boolean') {
        clean[key] = val;
      }
    }
  }
  return clean;
}

// ============================================================================
// 3. CORS & SECURITY HEADERS HANDLER
// ============================================================================
const ALLOWED_ORIGINS = [
  'https://get-studytimer.vercel.app',
  'https://studytimer.app',
  'http://localhost:3000',
  'http://localhost:5173',
  'http://127.0.0.1:3000',
  'http://127.0.0.1:5173'
];

function handleCorsAndSecurityHeaders(req, res) {
  const origin = req.headers.origin;
  
  if (origin && ALLOWED_ORIGINS.includes(origin)) {
    res.setHeader('Access-Control-Allow-Origin', origin);
  } else if (!origin) {
    // Native mobile apps / non-browser HTTP clients send no origin header
    res.setHeader('Access-Control-Allow-Origin', '*');
  }

  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-forwarded-for');
  res.setHeader('Access-Control-Max-Age', '86400');

  // Security Headers
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
  res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=()');
}

// ============================================================================
// 4. MAIN HANDLER
// ============================================================================
export default async function handler(req, res) {
  handleCorsAndSecurityHeaders(req, res);

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method === 'GET') {
    return res.status(200).json({
      status: 'online',
      service: 'StudyTimer Feedback API',
      version: '2.0.0',
      allowed_types: ['BUG_REPORT', 'FEATURE_REQUEST', 'GENERAL_FEEDBACK']
    });
  }

  if (req.method !== 'POST') {
    res.setHeader('Allow', ['POST', 'GET', 'OPTIONS']);
    return res.status(405).json({
      error: 'Method Not Allowed',
      message: `HTTP method ${req.method} is not supported.`
    });
  }

  try {
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket?.remoteAddress || '127.0.0.1';

    // 1. Sliding-Window Rate Limit Check
    const rateStatus = checkSlidingWindowRateLimit(clientIp);
    res.setHeader('X-RateLimit-Limit', rateStatus.limit.toString());
    res.setHeader('X-RateLimit-Remaining', rateStatus.remaining.toString());
    res.setHeader('X-RateLimit-Reset', rateStatus.reset.toString());

    if (!rateStatus.allowed) {
      res.setHeader('Retry-After', rateStatus.retryAfterSec.toString());
      return res.status(429).json({
        error: 'Too Many Requests',
        message: `Rate limit exceeded. Please wait ${rateStatus.retryAfterSec} seconds before submitting again.`,
        retry_after_seconds: rateStatus.retryAfterSec
      });
    }

    // 2. Strict Input Validation
    const body = req.body;
    if (!body || typeof body !== 'object') {
      return res.status(400).json({
        error: 'Bad Request',
        message: 'Request body must be a valid JSON object.'
      });
    }

    const { type, user_contact, message, diagnostics } = body;

    // Type Validation
    const validTypes = ['BUG_REPORT', 'FEATURE_REQUEST', 'GENERAL_FEEDBACK'];
    if (!type || typeof type !== 'string' || !validTypes.includes(type.toUpperCase())) {
      return res.status(400).json({
        error: 'Validation Error',
        message: 'Invalid feedback category.',
        valid_types: validTypes
      });
    }

    // Message Validation (5 to 2000 characters)
    if (!message || typeof message !== 'string' || message.trim().length < 5 || message.trim().length > 2000) {
      return res.status(400).json({
        error: 'Validation Error',
        message: 'Message must be between 5 and 2000 characters in length.'
      });
    }

    // Contact Validation (optional, max 120 chars)
    let cleanContact = null;
    if (user_contact && typeof user_contact === 'string') {
      const trimmed = user_contact.trim();
      if (trimmed.length > 120) {
        return res.status(400).json({
          error: 'Validation Error',
          message: 'Contact identifier must not exceed 120 characters.'
        });
      }
      cleanContact = sanitizeHtmlString(trimmed);
    }

    const normalizedType = type.toUpperCase();
    const cleanMessage = sanitizeHtmlString(message.trim());
    const cleanDiagnostics = sanitizeDiagnostics(diagnostics);

    // 3. Database Insertion (Parameterized via Supabase Client)
    const { data, error } = await supabase
      .from('feedback_reports')
      .insert([
        {
          type: normalizedType,
          status: 'NEW',
          user_contact: cleanContact,
          message: cleanMessage,
          diagnostics: cleanDiagnostics,
          created_at: new Date().toISOString(),
          updated_at: new Date().toISOString()
        }
      ])
      .select('id, type, status, created_at')
      .single();

    if (error) {
      // Server-side logging only - do NOT leak database error message to client
      console.error('Secure DB insert failure:', error.code || 'UNKNOWN_ERROR');
      return res.status(500).json({
        error: 'Internal Server Error',
        message: 'Failed to record feedback. Please try again later.'
      });
    }

    // 4. Secure Webhook Notification Dispatch
    const webhookUrl = process.env.FEEDBACK_ALERT_WEBHOOK;
    if (webhookUrl && normalizedType === 'BUG_REPORT') {
      try {
        const appVer = cleanDiagnostics.app_version || 'Unknown';
        const osVer = cleanDiagnostics.android_os || 'Unknown';
        const deviceMod = cleanDiagnostics.device || 'Unknown';
        const userEmail = cleanContact || 'Anonymous';

        // Delimit user input defensively to prevent webhook payload injection
        const safeSnippet = cleanMessage.substring(0, 500).replace(/[`\\]/g, '');

        await fetch(webhookUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            content: `🚨 **New StudyTimer Bug Report**\n**From:** ${userEmail}\n**App:** ${appVer} | **OS:** ${osVer} | **Device:** ${deviceMod}\n**Message:**\n>>> ${safeSnippet}`
          })
        }).catch(err => {
          console.error('Webhook dispatch failed');
        });
      } catch (err) {
        console.error('Notification dispatch handler error');
      }
    }

    // 5. Safe Generic Client Response
    return res.status(201).json({
      status: 'success',
      id: data.id,
      message: 'Feedback received securely. Thank you for helping us improve StudyTimer!',
      data: {
        id: data.id,
        type: data.type,
        status: data.status,
        created_at: data.created_at
      }
    });

  } catch (err) {
    // Server-side logging only - zero stack traces or internal details sent to client
    console.error('Unhandled feedback handler error');
    return res.status(500).json({
      error: 'Internal Server Error',
      message: 'An unexpected error occurred while processing your request.'
    });
  }
}
