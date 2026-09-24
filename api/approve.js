import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';
const TELEGRAM_MODERATION_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '8755792560:AAFrTNyOjveVTV9vtRgwVD6tkNMwfRBDG2k';
const TELEGRAM_MODERATION_CHAT_ID = process.env.TELEGRAM_CHAT_ID || '6326462250';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export default async function handler(req, res) {
  const { user_id, action } = req.query || req.body || {};
  const isApprove = String(action || 'approve').toLowerCase() === 'approve';

  if (!user_id) {
    return res.status(400).send(`
      <!DOCTYPE html>
      <html>
      <head><meta charset="UTF-8"><title>StudyTimer Admin</title><meta name="viewport" content="width=device-width,initial-scale=1"></head>
      <body style="font-family:sans-serif; background:#09090b; color:#fff; display:flex; align-items:center; justify-content:center; height:100vh; margin:0;">
        <div style="background:#18181b; padding:28px; border-radius:16px; text-align:center; max-width:400px; border:1px solid rgba(255,255,255,0.1);">
          <h2 style="color:#ef4444; margin-top:0;">❌ Missing User ID</h2>
          <p style="color:#a1a1aa;">Please provide a valid student user_id in the request.</p>
        </div>
      </body>
      </html>
    `);
  }

  try {
    const safeUserId = String(user_id).trim();

    if (isApprove) {
      // 1. Fetch user sync data to inspect current profile
      const { data: syncRow, error: syncFetchErr } = await supabase
        .from('user_sync_data')
        .select('*')
        .eq('user_id', safeUserId)
        .maybeSingle();

      let targetAvatarUrl = '🐱';
      let targetRing = 'glow-gold';
      let targetName = 'Student';

      if (syncRow) {
        let profile = {};
        if (syncRow.custom_preferences && syncRow.custom_preferences.__user_profile__) {
          try {
            profile = typeof syncRow.custom_preferences.__user_profile__ === 'string'
              ? JSON.parse(syncRow.custom_preferences.__user_profile__)
              : syncRow.custom_preferences.__user_profile__;
          } catch (_) {}
        }

        profile.photoApproved = true;
        profile.profileStatus = 'approved';
        targetAvatarUrl = profile.avatarPreset || targetAvatarUrl;
        targetRing = profile.avatarRing || targetRing;
        targetName = profile.displayName || syncRow.user_name || targetName;

        const updatedPrefs = {
          ...(syncRow.custom_preferences || {}),
          __user_profile__: profile
        };

        // Update user_sync_data with approved status
        await supabase
          .from('user_sync_data')
          .update({
            profile_status: 'approved',
            user_name: targetName,
            custom_preferences: updatedPrefs,
            updated_at: new Date().toISOString()
          })
          .eq('user_id', safeUserId);
      }

      // 2. Update daily_leaderboard table so the photo appears online instantly
      await supabase
        .from('daily_leaderboard')
        .update({
          avatar_url: targetAvatarUrl,
          avatar_ring: targetRing,
          is_stealth: false
        })
        .eq('user_id', safeUserId);

      // 3. Notify Telegram of successful approval
      fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/sendMessage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: TELEGRAM_MODERATION_CHAT_ID,
          text: `✅ <b>Profile & Photo Approved!</b>\n👤 <b>Student:</b> <code>${targetName}</code>\n🆔 <b>ID:</b> <code>${safeUserId}</code>\n⚡ <i>Status updated to 'approved' across leaderboards.</i>`,
          parse_mode: 'HTML'
        })
      }).catch(() => {});

      return res.status(200).send(`
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>StudyTimer Moderation • Approved</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #09090b; color: #f4f4f5; display: flex; align-items: center; justify-content: center; min-height: 100vh; margin: 0; padding: 16px; }
            .card { background: #18181b; border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 20px; padding: 32px 24px; text-align: center; max-width: 440px; width: 100%; box-shadow: 0 20px 40px rgba(0,0,0,0.6); }
            .badge { display: inline-flex; width: 64px; height: 64px; border-radius: 50%; background: rgba(16, 185, 129, 0.15); color: #10b981; align-items: center; justify-content: center; font-size: 32px; margin-bottom: 16px; border: 2px solid #10b981; }
            h2 { margin: 0 0 8px; color: #10b981; font-size: 1.4rem; }
            p { color: #a1a1aa; font-size: 0.92rem; line-height: 1.5; margin: 0 0 20px; }
            .meta { background: #27272a; border-radius: 12px; padding: 12px 16px; text-align: left; font-size: 0.85rem; margin-bottom: 20px; word-break: break-all; }
            .meta-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
            .meta-row:last-child { margin-bottom: 0; }
            .meta-label { color: #71717a; }
            .meta-val { color: #e4e4e7; font-weight: 600; font-family: monospace; }
            .btn { display: inline-block; background: #10b981; color: #000; font-weight: 700; text-decoration: none; padding: 10px 24px; border-radius: 9999px; font-size: 0.88rem; transition: transform 0.15s; }
            .btn:hover { transform: scale(1.04); }
          </style>
        </head>
        <body>
          <div class="card">
            <div class="badge">✓</div>
            <h2>Profile & Photo Approved!</h2>
            <p>Student identity and custom photo have been verified and activated on the live global leaderboard.</p>
            <div class="meta">
              <div class="meta-row"><span class="meta-label">Student:</span><span class="meta-val">${targetName}</span></div>
              <div class="meta-row"><span class="meta-label">User ID:</span><span class="meta-val">${safeUserId.slice(0, 16)}...</span></div>
              <div class="meta-row"><span class="meta-label">Status:</span><span class="meta-val" style="color:#10b981;">APPROVED</span></div>
            </div>
            <a href="https://t.me" class="btn">Return to Telegram</a>
          </div>
        </body>
        </html>
      `);
    } else {
      // Reject action
      await supabase
        .from('user_sync_data')
        .update({
          profile_status: 'rejected',
          updated_at: new Date().toISOString()
        })
        .eq('user_id', safeUserId);

      // Reset leaderboard avatar back to default cat sticker
      await supabase
        .from('daily_leaderboard')
        .update({
          avatar_url: '🐱',
          is_stealth: false
        })
        .eq('user_id', safeUserId);

      // Notify Telegram of rejection
      fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/sendMessage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: TELEGRAM_MODERATION_CHAT_ID,
          text: `❌ <b>Profile Rejected & Reset</b>\n🆔 <b>User ID:</b> <code>${safeUserId}</code>\n⚠️ <i>Custom photo removed. Reset to default sticker.</i>`,
          parse_mode: 'HTML'
        })
      }).catch(() => {});

      return res.status(200).send(`
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"><title>StudyTimer Moderation • Rejected</title><meta name="viewport" content="width=device-width,initial-scale=1"></head>
        <body style="font-family:sans-serif; background:#09090b; color:#fff; display:flex; align-items:center; justify-content:center; height:100vh; margin:0; padding:16px;">
          <div style="background:#18181b; padding:32px; border-radius:18px; text-align:center; max-width:400px; border:1px solid rgba(239,68,68,0.3);">
            <div style="font-size:36px; margin-bottom:12px;">🚫</div>
            <h2 style="color:#ef4444; margin:0 0 8px;">Profile Rejected</h2>
            <p style="color:#a1a1aa; font-size:0.9rem; margin-bottom:20px;">The student profile has been rejected and reset to safe defaults.</p>
            <a href="https://t.me" style="background:#ef4444; color:#fff; text-decoration:none; padding:10px 20px; border-radius:9999px; font-weight:bold; font-size:0.85rem;">Return to Telegram</a>
          </div>
        </body>
        </html>
      `);
    }
  } catch (err) {
    return res.status(500).send(`
      <!DOCTYPE html>
      <html>
      <head><meta charset="UTF-8"><title>Error</title><meta name="viewport" content="width=device-width,initial-scale=1"></head>
      <body style="font-family:sans-serif; background:#09090b; color:#fff; display:flex; align-items:center; justify-content:center; height:100vh; margin:0;">
        <div style="background:#18181b; padding:24px; border-radius:16px; text-align:center; max-width:400px;">
          <h3 style="color:#ef4444;">Server Exception</h3>
          <p style="color:#a1a1aa;">${err.message || 'Failed to process request.'}</p>
        </div>
      </body>
      </html>
    `);
  }
}
