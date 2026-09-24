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
      // 1. Instant acknowledgment: Answer Telegram callback in <20ms so button never spins
      fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/answerCallbackQuery`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          callback_query_id: callbackId,
          text: isApprove ? '✅ Profile & Photo Approved! Leaderboard updated.' : '❌ Profile Rejected & Reset.',
          show_alert: false
        })
      }).catch(() => {});

      try {
        if (isApprove) {
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

          // Direct sync to daily_leaderboard table
          await supabase
            .from('daily_leaderboard')
            .update({
              avatar_url: targetAvatarUrl,
              avatar_ring: targetRing,
              is_stealth: false
            })
            .eq('user_id', userId);

          // Update message in-place and remove action buttons
          if (chatId && messageId) {
            const confirmedText = `✅ <b>APPROVED BY ADMIN</b>\n👤 <b>Student:</b> <code>${targetName}</code>\n🆔 <b>ID:</b> <code>${userId}</code>\n⚡ <i>Activated on live leaderboard at ${new Date().toLocaleTimeString()}</i>`;
            
            fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageCaption`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                chat_id: chatId,
                message_id: messageId,
                caption: confirmedText,
                parse_mode: 'HTML',
                reply_markup: { inline_keyboard: [] }
              })
            }).then(r => {
              if (!r.ok) {
                return fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageText`, {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({
                    chat_id: chatId,
                    message_id: messageId,
                    text: confirmedText,
                    parse_mode: 'HTML',
                    reply_markup: { inline_keyboard: [] }
                  })
                });
              }
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

          if (chatId && messageId) {
            const rejectText = `❌ <b>REJECTED BY ADMIN</b>\n🆔 <b>ID:</b> <code>${userId}</code>\n⚠️ <i>Profile photo reset to default sticker.</i>`;
            
            fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageCaption`, {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                chat_id: chatId,
                message_id: messageId,
                caption: rejectText,
                parse_mode: 'HTML',
                reply_markup: { inline_keyboard: [] }
              })
            }).then(r => {
              if (!r.ok) {
                return fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/editMessageText`, {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify({
                    chat_id: chatId,
                    message_id: messageId,
                    text: rejectText,
                    parse_mode: 'HTML',
                    reply_markup: { inline_keyboard: [] }
                  })
                });
              }
            }).catch(() => {});
          }
        }
      } catch (err) {
        console.error('Telegram webhook callback processing error:', err);
      }
    }
  }

  return res.status(200).json({ ok: true });
}
