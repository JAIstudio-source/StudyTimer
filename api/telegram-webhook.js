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
          let targetFlag = '🌐';
          let profile = {};

          if (syncRow) {
            // 1. Try reading from prefs_data
            if (syncRow.prefs_data) {
              try {
                const parsedPrefs = typeof syncRow.prefs_data === 'string'
                  ? JSON.parse(syncRow.prefs_data)
                  : syncRow.prefs_data;
                if (parsedPrefs && parsedPrefs.__user_profile__) {
                  profile = typeof parsedPrefs.__user_profile__ === 'string'
                    ? JSON.parse(parsedPrefs.__user_profile__)
                    : parsedPrefs.__user_profile__;
                }
              } catch (_) {}
            }

            // 2. Fallback to pending_profile_json or custom_preferences
            if ((!profile || !profile.avatarPreset) && syncRow.pending_profile_json) {
              try {
                const parsedPending = typeof syncRow.pending_profile_json === 'string'
                  ? JSON.parse(syncRow.pending_profile_json)
                  : syncRow.pending_profile_json;
                if (parsedPending) profile = { ...profile, ...parsedPending };
              } catch (_) {}
            }

            if ((!profile || !profile.avatarPreset) && syncRow.custom_preferences) {
              try {
                const parsedCustom = typeof syncRow.custom_preferences === 'string'
                  ? JSON.parse(syncRow.custom_preferences)
                  : syncRow.custom_preferences;
                if (parsedCustom && parsedCustom.__user_profile__) {
                  const customProf = typeof parsedCustom.__user_profile__ === 'string'
                    ? JSON.parse(parsedCustom.__user_profile__)
                    : parsedCustom.__user_profile__;
                  if (customProf) profile = { ...profile, ...customProf };
                }
              } catch (_) {}
            }

            targetAvatarUrl = profile.avatarPreset || syncRow.profile_image_uri || '🐱';
            targetRing = profile.avatarRing || 'glow-gold';
            targetName = profile.displayName || syncRow.user_name || targetName;
            targetFlag = profile.countryFlag || '🌐';

            profile.photoApproved = true;
            profile.profileStatus = 'approved';
            profile.avatarPreset = targetAvatarUrl;

            let updatedPrefs = {};
            if (syncRow.prefs_data) {
              try {
                updatedPrefs = typeof syncRow.prefs_data === 'string'
                  ? JSON.parse(syncRow.prefs_data)
                  : (syncRow.prefs_data || {});
              } catch (_) {}
            }
            profile.photoApproved = true;
            profile.profileStatus = 'approved';
            updatedPrefs.__user_profile__ = JSON.stringify(profile);

            await Promise.all([
              supabase
                .from('user_sync_data')
                .update({
                  profile_status: 'approved',
                  user_name: targetName,
                  profile_image_uri: targetAvatarUrl,
                  pending_profile_json: null,
                  prefs_data: JSON.stringify(updatedPrefs),
                  updated_at: Date.now()
                })
                .eq('user_id', userId),
              supabase
                .from('daily_leaderboard')
                .update({
                  user_name: targetName,
                  avatar_url: targetAvatarUrl,
                  avatar_ring: targetRing,
                  country_flag: targetFlag,
                  is_stealth: Boolean(profile.isStealth)
                })
                .eq('user_id', userId)
            ]);
          }

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
          // Reject action: Keep existing display name, only reject the photo and fallback to default sticker
          const { data: existingUser } = await supabase
            .from('user_sync_data')
            .select('prefs_data, user_name')
            .eq('user_id', userId)
            .maybeSingle();

          let userPrefs = {};
          let profileObj = {};
          if (existingUser && existingUser.prefs_data) {
            try {
              userPrefs = typeof existingUser.prefs_data === 'string' ? JSON.parse(existingUser.prefs_data) : existingUser.prefs_data;
              if (userPrefs.__user_profile__) {
                profileObj = typeof userPrefs.__user_profile__ === 'string' ? JSON.parse(userPrefs.__user_profile__) : userPrefs.__user_profile__;
              }
            } catch (_) {}
          }

          profileObj.photoApproved = false;
          profileObj.profileStatus = 'rejected';
          profileObj.avatarPreset = profileObj.fallbackSticker || '🐱';
          userPrefs.__user_profile__ = JSON.stringify(profileObj);

          await Promise.all([
            supabase
              .from('user_sync_data')
              .update({
                profile_status: 'rejected',
                pending_profile_json: null,
                profile_image_uri: profileObj.avatarPreset,
                prefs_data: JSON.stringify(userPrefs),
                updated_at: Date.now()
              })
              .eq('user_id', userId),
            supabase
              .from('daily_leaderboard')
              .update({ avatar_url: profileObj.avatarPreset })
              .eq('user_id', userId)
          ]);

          if (chatId && messageId) {
            const rejectText = `❌ <b>REJECTED BY ADMIN</b>\n🆔 <b>ID:</b> <code>${userId}</code>\n⚠️ <i>Profile photo reset to default sticker. Display name preserved.</i>`;
            
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
