import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = process.env.SUPABASE_URL || 'https://vkveimpvrpnzelbsvdrg.supabase.co';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY || 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';
const TELEGRAM_MODERATION_BOT_TOKEN = process.env.TELEGRAM_BOT_TOKEN || '8755792560:AAFrTNyOjveVTV9vtRgwVD6tkNMwfRBDG2k';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(200).json({ ok: true, message: 'StudyTimer Telegram Webhook Live' });
  }

  const update = req.body || {};
  const callbackQuery = update.callback_query;

  if (callbackQuery) {
    const callbackId = callbackQuery.id;
    const data = callbackQuery.data || '';
    const chatId = callbackQuery.message?.chat?.id;
    const messageId = callbackQuery.message?.message_id;

    const isApprove = data.startsWith('approve:');
    const isReject = data.startsWith('reject:');
    const userId = data.split(':')[1];

    if (userId && (isApprove || isReject)) {
      try {
        if (isApprove) {
          // Fetch user sync data to inspect current profile
          const { data: syncRow } = await supabase
            .from('user_sync_data')
            .select('*')
            .eq('user_id', userId)
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

            await supabase
              .from('user_sync_data')
              .update({
                profile_status: 'approved',
                user_name: targetName,
                custom_preferences: updatedPrefs,
                updated_at: new Date().toISOString()
              })
              .eq('user_id', userId);
          }

          // Update daily_leaderboard table
          await supabase
            .from('daily_leaderboard')
            .update({
              avatar_url: targetAvatarUrl,
              avatar_ring: targetRing,
              is_stealth: false
            })
            .eq('user_id', userId);

          // 1. Answer Callback Query to stop the spinning loader
          await fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/answerCallbackQuery`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              callback_query_id: callbackId,
              text: `✅ Approved ${targetName}! Leaderboard updated.`,
              show_alert: true
            })
          });

          // 2. Edit message caption/text to show verified
          if (chatId && messageId) {
            await fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageCaption`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                chat_id: chatId,
                message_id: messageId,
                caption: `✅ <b>APPROVED BY ADMIN</b>\n👤 <b>Student:</b> <code>${targetName}</code>\n🆔 <b>ID:</b> <code>${userId}</code>\n⚡ <i>Activated on live global leaderboard at ${new Date().toLocaleTimeString()}</i>`,
                parse_mode: 'HTML'
              })
            }).catch(() => {});
          }
        } else {
          // Reject action
          await supabase
            .from('user_sync_data')
            .update({ profile_status: 'rejected', updated_at: new Date().toISOString() })
            .eq('user_id', userId);

          await supabase
            .from('daily_leaderboard')
            .update({ avatar_url: '🐱', is_stealth: false })
            .eq('user_id', userId);

          await fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/answerCallbackQuery`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              callback_query_id: callbackId,
              text: `❌ Profile rejected and reset to default sticker.`,
              show_alert: true
            })
          });

          if (chatId && messageId) {
            await fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageCaption`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                chat_id: chatId,
                message_id: messageId,
                caption: `❌ <b>REJECTED BY ADMIN</b>\n🆔 <b>ID:</b> <code>${userId}</code>\n⚠️ <i>Reset to default sticker.</i>`,
                parse_mode: 'HTML'
              })
            }).catch(() => {});
          }
        }
      } catch (err) {
        console.error('Webhook error:', err);
      }
    }
  }

  return res.status(200).json({ ok: true });
}
