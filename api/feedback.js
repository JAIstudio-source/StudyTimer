import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// In-memory rate limiting store for serverless instances (5 min cooldown per IP/client)
const rateLimitMap = new Map();
const COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

export default async function handler(req, res) {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-forwarded-for');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  if (req.method === 'GET') {
    return res.status(200).json({
      status: 'online',
      service: 'StudyTimer Feedback API',
      version: '1.0.0',
      allowed_types: ['BUG_REPORT', 'FEATURE_REQUEST', 'GENERAL_FEEDBACK']
    });
  }

  if (req.method !== 'POST') {
    res.setHeader('Allow', ['POST', 'GET', 'OPTIONS']);
    return res.status(405).json({ error: `Method ${req.method} not allowed` });
  }

  try {
    const clientIp = req.headers['x-forwarded-for']?.split(',')[0]?.trim() || req.socket?.remoteAddress || 'unknown';
    const now = Date.now();

    // Rate Limiting Check
    const lastSubmission = rateLimitMap.get(clientIp);
    if (lastSubmission && (now - lastSubmission) < COOLDOWN_MS) {
      const retryAfterSec = Math.ceil((COOLDOWN_MS - (now - lastSubmission)) / 1000);
      res.setHeader('Retry-After', retryAfterSec.toString());
      return res.status(429).json({
        error: 'Too Many Requests',
        message: `Rate limit exceeded. Please wait ${retryAfterSec} seconds before submitting again.`,
        retry_after_seconds: retryAfterSec
      });
    }

    const { type, user_contact, message, diagnostics } = req.body || {};

    // Category Validation
    const validTypes = ['BUG_REPORT', 'FEATURE_REQUEST', 'GENERAL_FEEDBACK'];
    if (!type || !validTypes.includes(type.toUpperCase())) {
      return res.status(400).json({
        error: 'Invalid feedback type',
        valid_types: validTypes
      });
    }

    // Message Length Validation (5 to 2000 chars)
    if (!message || typeof message !== 'string' || message.trim().length < 5 || message.trim().length > 2000) {
      return res.status(400).json({
        error: 'Message must be between 5 and 2000 characters in length.'
      });
    }

    const normalizedType = type.toUpperCase();
    const cleanMessage = message.trim();
    const cleanContact = (typeof user_contact === 'string' && user_contact.trim().length > 0) ? user_contact.trim() : null;
    const cleanDiagnostics = (typeof diagnostics === 'object' && diagnostics !== null) ? diagnostics : {};

    // Insert feedback into Supabase feedback_reports table
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
      console.error('Supabase feedback insert error:', error);
      return res.status(500).json({
        error: 'Failed to record feedback in database',
        details: error.message
      });
    }

    // Record submission time for IP rate limiting
    rateLimitMap.set(clientIp, now);

    // Optional Notification Alert Webhook (Discord / Telegram / Email webhook support)
    const webhookUrl = process.env.FEEDBACK_ALERT_WEBHOOK;
    if (webhookUrl && normalizedType === 'BUG_REPORT') {
      try {
        const appVer = cleanDiagnostics.app_version || 'Unknown';
        const osVer = cleanDiagnostics.android_os || 'Unknown';
        const deviceMod = cleanDiagnostics.device || 'Unknown';
        const userEmail = cleanContact || 'Anonymous';

        await fetch(webhookUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            content: `🚨 **New StudyTimer Bug Report**\n**From:** ${userEmail}\n**App:** ${appVer} | **OS:** ${osVer} | **Device:** ${deviceMod}\n**Message:**\n> ${cleanMessage.substring(0, 500)}`
          })
        }).catch(err => console.error('Webhook notification failed:', err));
      } catch (err) {
        console.error('Notification dispatch error:', err);
      }
    }

    return res.status(201).json({
      status: 'success',
      id: data.id,
      message: 'Feedback submitted successfully. Thank you!',
      data: {
        id: data.id,
        type: data.type,
        status: data.status,
        created_at: data.created_at
      }
    });

  } catch (err) {
    console.error('Unhandled feedback error:', err);
    return res.status(500).json({
      error: 'Internal Server Error',
      details: err.message
    });
  }
}
