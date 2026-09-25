// ============================================================================
// STUDYTIMER LIVE LEADERBOARD & PROFILE CUSTOMIZATION MODULE
// ============================================================================

// PROFILE CUSTOMIZATION SYSTEM
// ----------------------------------------------------------------------------

function openProfileModal() {
  const modal = document.getElementById('profileModalOverlay');
  if (!modal) return;

  const profile = appState.userProfile || {
    displayName: 'Student',
    avatarPreset: '',
    avatarRing: 'glow-gold',
    bannerTheme: 'banner-midnight',
    countryFlag: '🌐',
    mood: '☕ Deep Focus',
    examTarget: '🎯 4h Daily Target',
    motto: '🎯 Deep focus & daily consistency',
    primarySubjectId: 'general',
    isStealth: false,
    isPublicLeaderboard: true
  };

  selectedAvatarPreset = profile.avatarPreset || '';
  selectedAvatarRing = profile.avatarRing || 'glow-gold';
  selectedBannerTheme = profile.bannerTheme || 'banner-midnight';
  selectedCountryFlag = profile.countryFlag || '🌐';

  const nameInput = document.getElementById('inputProfileDisplayName');
  const moodInput = document.getElementById('inputProfileMood');
  const examInput = document.getElementById('inputProfileExam');
  const mottoInput = document.getElementById('inputProfileMotto');
  const flagSelect = document.getElementById('selectProfileCountryFlag');
  const subjectSelect = document.getElementById('selectProfilePrimarySubject');
  const stealthToggle = document.getElementById('checkStealthScholar');
  const publicToggle = document.getElementById('checkLeaderboardPublic');

  if (nameInput) {
    nameInput.value = profile.displayName || 
                      appState.currentUser?.user_metadata?.full_name || 
                      appState.currentUser?.user_metadata?.name || 
                      appState.currentUser?.email?.split('@')[0] || 
                      'Student';
  }
  if (moodInput) moodInput.value = profile.mood || '';
  if (examInput) examInput.value = profile.examTarget || '';
  if (mottoInput) mottoInput.value = profile.motto || '';
  if (flagSelect) flagSelect.value = selectedCountryFlag;
  if (stealthToggle) stealthToggle.checked = Boolean(profile.isStealth);

  if (subjectSelect) {
    const cleanSubjects = getCleanUniqueSubjects(appState.subjects);
    subjectSelect.innerHTML = cleanSubjects.map(s => 
      `<option value="${s.id}" ${s.id === profile.primarySubjectId ? 'selected' : ''}>${s.iconEmoji ? s.iconEmoji + ' ' : ''}${s.name}</option>`
    ).join('');
  }
  if (publicToggle) {
    publicToggle.checked = profile.isPublicLeaderboard !== false;
  }

  // Reset Submenu Tabs to Photo & Avatar by default
  switchProfileTab('tab-avatar');

  // Check if avatar is a custom photo vs preset
  const isCustomPhoto = /^(http|https|data:|assets\/|\/|blob:)/i.test((selectedAvatarPreset || '').trim());
  const photoPreviewImg = document.getElementById('customPhotoPreviewImg');
  const photoFallback = document.getElementById('customPhotoPreviewFallback');
  const btnRemovePhoto = document.getElementById('btnRemoveCustomPhoto');
  const photoUrlInput = document.getElementById('inputProfilePhotoUrl');
  const statusCard = document.getElementById('photoUrlStatusCard');
  const statusIcon = document.getElementById('photoUrlStatusIcon');
  const statusTitle = document.getElementById('photoUrlStatusTitle');
  const statusDesc = document.getElementById('photoUrlStatusDesc');

  if (photoPreviewImg && photoFallback) {
    if (isCustomPhoto) {
      photoPreviewImg.src = selectedAvatarPreset;
      photoPreviewImg.classList.remove('hidden');
      photoFallback.classList.add('hidden');
      btnRemovePhoto?.classList.remove('hidden');
      
      // Do not display internal storage or Google OAuth avatar URLs to the user in the input text field
      const isAutoAuthAvatar = selectedAvatarPreset.includes('supabase.co') || 
                               selectedAvatarPreset.includes('googleusercontent.com') ||
                               selectedAvatarPreset.includes('google.com') ||
                               selectedAvatarPreset.startsWith('data:image/');
      if (photoUrlInput) {
        photoUrlInput.value = (selectedAvatarPreset.startsWith('http') && !isAutoAuthAvatar) ? selectedAvatarPreset : '';
      }
      if (statusCard) {
        statusCard.className = 'photo-url-status-card is-valid';
        statusCard.classList.remove('hidden');
        if (statusIcon) statusIcon.textContent = '✓';
        if (statusTitle) statusTitle.textContent = 'Current Profile Photo';
        if (statusDesc) statusDesc.textContent = profile.photoApproved ? 'Approved & active on global leaderboard' : 'In review / safety verification';
      }
    } else {
      photoPreviewImg.src = '';
      photoPreviewImg.classList.add('hidden');
      photoFallback.textContent = selectedAvatarPreset || (profile.displayName || 'S').trim().charAt(0).toUpperCase() || 'S';
      photoFallback.classList.remove('hidden');
      btnRemovePhoto?.classList.add('hidden');
      if (photoUrlInput) photoUrlInput.value = '';
      if (statusCard) statusCard.classList.add('hidden');
    }
  }

  // Highlight active buttons
  document.querySelectorAll('.avatar-preset-btn').forEach(btn => {
    btn.classList.toggle('active', !isCustomPhoto && btn.dataset.avatar === selectedAvatarPreset);
  });
  document.querySelectorAll('.banner-theme-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.banner === selectedBannerTheme);
  });
  document.querySelectorAll('.glow-ring-btn').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.ring === selectedAvatarRing);
  });

  updateProfileLivePreview();
  lockBodyScroll();
  modal.classList.remove('hidden');
}

function switchProfileTab(tabName) {
  document.querySelectorAll('.profile-tab-btn').forEach(b => {
    const isTarget = b.dataset.tab === tabName;
    b.classList.toggle('active', isTarget);
    b.setAttribute('aria-selected', isTarget ? 'true' : 'false');
  });
  document.querySelectorAll('.profile-tab-panel').forEach(panel => {
    const isTarget = panel.id === `panel-${tabName}`;
    panel.classList.toggle('hidden', !isTarget);
    panel.classList.toggle('active', isTarget);
  });
}

function closeProfileModal() {
  document.getElementById('profileModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
}

function updateProfileLivePreview() {
  const nameInput = document.getElementById('inputProfileDisplayName');
  const moodInput = document.getElementById('inputProfileMood');
  const examInput = document.getElementById('inputProfileExam');
  const mottoInput = document.getElementById('inputProfileMotto');
  const flagSelect = document.getElementById('selectProfileCountryFlag');
  const stealthToggle = document.getElementById('checkStealthScholar');

  const previewCard = document.getElementById('previewProfileCard');
  const previewAvatarRing = document.getElementById('previewAvatarRing');
  const previewAvatarIcon = document.getElementById('previewAvatarIcon');
  const previewCountryFlag = document.getElementById('previewCountryFlag');
  const previewDisplayName = document.getElementById('previewDisplayName');
  const previewRolePill = document.getElementById('previewRolePill');
  const previewMoodPill = document.getElementById('previewMoodPill');
  const previewExamBadge = document.getElementById('previewExamBadge');
  const previewMottoText = document.getElementById('previewMottoText');

  const isStealth = Boolean(stealthToggle?.checked);
  const rawName = (nameInput?.value.trim() || 'Student').slice(0, 24);
  const nameVal = isStealth ? `Stealth Scholar #${Math.abs(hashString(rawName) % 9000 + 1000)}` : rawName;
  const rawFlag = flagSelect?.value || selectedCountryFlag || '🌐';
  const flagVal = getCountryFlagEmoji(rawFlag);
  const moodVal = (moodInput?.value.trim() || '☕ Deep Focus').slice(0, 40);
  const examVal = (examInput?.value.trim() || '🎯 Target: 4h Daily').slice(0, 30);
  const mottoVal = (mottoInput?.value.trim() || '🎯 Deep focus & daily consistency').slice(0, 60);

  if (previewCard) {
    previewCard.className = `profile-hero-showcase ${selectedBannerTheme}`;
  }
  if (previewAvatarRing) {
    previewAvatarRing.className = `hero-avatar-ring ${selectedAvatarRing}`;
  }
  if (previewAvatarIcon) {
    const isUrl = /^(http|https|data:|assets\/|\/|blob:)/i.test((selectedAvatarPreset || '').trim());
    if (isUrl) {
      const fallbackInit = escapeHtml((nameVal || 'S').trim().charAt(0).toUpperCase() || 'S');
      previewAvatarIcon.innerHTML = `<img src="${selectedAvatarPreset}" alt="Avatar Preview" style="width:100%; height:100%; object-fit:cover; border-radius:50%;" onerror="this.outerHTML='<span class=&quot;avatar-initial&quot;>${fallbackInit}</span>'">`;
    } else {
      previewAvatarIcon.textContent = (nameVal || 'S').trim().charAt(0).toUpperCase() || 'S';
    }
  }
  if (previewCountryFlag) previewCountryFlag.textContent = flagVal;
  if (previewDisplayName) previewDisplayName.textContent = nameVal;
  if (previewRolePill) previewRolePill.textContent = isStealth ? 'Stealth' : 'Scholar';
  if (previewMoodPill) previewMoodPill.textContent = moodVal;
  if (previewExamBadge) previewExamBadge.textContent = examVal;
  if (previewMottoText) previewMottoText.textContent = mottoVal;
}

// Simple deterministic hash for stealth IDs
function hashString(str) {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash) + str.charCodeAt(i);
    hash |= 0;
  }
  return hash;
}

function dataURLtoBlob(dataurl) {
  try {
    const parts = dataurl.split(',');
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'image/jpeg';
    const bstr = atob(parts[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new Blob([u8arr], { type: mime });
  } catch (err) {
    console.warn('Error converting dataURL to Blob:', err);
    return null;
  }
}

async function uploadAvatarToSupabaseStorage(dataUrlOrFile, userId) {
  if (!supabaseClient || !dataUrlOrFile || !userId) return null;
  try {
    let blob = null;
    if (typeof dataUrlOrFile === 'string') {
      if (dataUrlOrFile.startsWith('data:image/')) {
        blob = dataURLtoBlob(dataUrlOrFile);
      } else if (dataUrlOrFile.startsWith('http://') || dataUrlOrFile.startsWith('https://')) {
        return dataUrlOrFile;
      }
    } else if (dataUrlOrFile instanceof Blob || dataUrlOrFile instanceof File) {
      blob = dataUrlOrFile;
    }

    if (!blob) return null;

    const fileExt = blob.type === 'image/png' ? 'png' : (blob.type === 'image/webp' ? 'webp' : 'jpg');
    const safeUserId = String(userId).replace(/[^a-zA-Z0-9_-]/g, '_');
    const filePath = `user_${safeUserId}_${Date.now()}.${fileExt}`;

    const { data, error } = await supabaseClient.storage
      .from('avatars')
      .upload(filePath, blob, {
        cacheControl: '3600',
        upsert: true,
        contentType: blob.type || 'image/jpeg'
      });

    if (error) {
      console.warn('Supabase Storage avatars upload notice (using fallback format):', error.message || error);
      return null;
    }

    const { data: publicData } = supabaseClient.storage
      .from('avatars')
      .getPublicUrl(filePath);

    if (publicData?.publicUrl) {
      return publicData.publicUrl;
    }
  } catch (err) {
    console.warn('Avatar storage upload exception:', err);
  }
  return null;
}

// Note: Bot token is intentionally kept here for now (direct Telegram API).
// TODO (C1): Move to Supabase Edge Function or Cloudflare Worker to hide from client bundle.
const TELEGRAM_MODERATION_BOT_TOKEN = '8755792560:AAFrTNyOjveVTV9vtRgwVD6tkNMwfRBDG2k';
const TELEGRAM_MODERATION_CHAT_ID = '6326462250';
const APPROVAL_WORKER_URL = 'https://studytimer-approval-bot.jaistudio.workers.dev/notify-new-profile';
let _lastModerationNotifyMs = 0; // Rate-limit guard (B2)

async function notifyAdminModerationWebhook(payload) {
  const now = Date.now();
  if (now - _lastModerationNotifyMs < 10000) return;
  _lastModerationNotifyMs = now;

  try {
    const { user_id, display_name, previous_name, email, status_mood, previous_bio, avatar_url, previous_avatar, photo_changed } = payload;
    const isFlagged = hasProfanity(display_name) || hasProfanity(status_mood);
    const isCustomPhoto = avatar_url && /^(http|https|data:|blob:)/i.test(avatar_url);

    const oldName = (previous_name || '').replace(/[<>&"]/g, '');
    const newName = (display_name || 'Student').replace(/[<>&"]/g, '');
    const oldBio = (previous_bio || '').replace(/[<>&"]/g, '');
    const newBio = (status_mood || '').replace(/[<>&"]/g, '');
    const oldAvatar = (previous_avatar || '').replace(/[<>&"]/g, '');
    const newAvatar = (avatar_url || '').replace(/[<>&"]/g, '');
    const safeEmail = (email || '').replace(/[<>&"]/g, '');

    const nameChanged = Boolean(oldName && newName !== oldName);
    const bioChanged = Boolean(newBio !== oldBio);
    const avatarChanged = Boolean(newAvatar !== oldAvatar);

    let header = `🛡️ <b>[PROFILE APPROVAL REQUEST]</b>\n\n`;
    if (isFlagged) {
      header = `🚨 <b>[FLAGGED: INAPPROPRIATE CONTENT DETECTED]</b>\n⚠️ <i>Potential vulgar/prohibited words found in public profile!</i>\n\n`;
    }

    let nameSection = "";
    if (nameChanged) {
      nameSection = `👤 <b>Display Name:</b>\n<code>${oldName || "None"}</code> ➔ <b><code>${newName}</code></b>\n\n`;
    } else {
      nameSection = `👤 <b>Display Name:</b> <b><code>${newName}</code></b> <i>(Unchanged)</i>\n\n`;
    }

    let bioSection = "";
    if (bioChanged && (newBio || oldBio)) {
      bioSection = `💬 <b>Bio / Motto:</b>\n<i>"${oldBio || "None"}"</i> ➔ <b><i>"${newBio || "None"}"</i></b>\n\n`;
    } else if (newBio) {
      bioSection = `💬 <b>Bio / Motto:</b> <i>"${newBio}"</i> <i>(Unchanged)</i>\n\n`;
    }

    let avatarSection = "";
    if (isCustomPhoto) {
      avatarSection = `📸 <b>Profile Photo:</b> ⚠️ <code>Custom Photo Uploaded</code>\n\n`;
    } else if (avatarChanged) {
      avatarSection = `🎨 <b>Avatar Sticker:</b> <code>${oldAvatar}</code> ➔ <b><code>${newAvatar}</code></b>\n\n`;
    }

    const footer = (
      `──────────────────\n` +
      `🆔 <b>User ID:</b> <code>${user_id}</code>\n` +
      `📱 <b>Source:</b> Website${safeEmail ? ` • 📧 <code>${safeEmail}</code>` : ""}`
    );

    const caption = (header + nameSection + bioSection + avatarSection + footer).slice(0, 1024);

    const keyboard = {
      inline_keyboard: isFlagged ? [
        [
          { text: "❌ Reject & Sanitize", callback_data: `reject:${user_id}` },
          { text: "⚠️ Force Approve", callback_data: `approve:${user_id}` }
        ]
      ] : [
        [
          { text: "✅ Approve", callback_data: `approve:${user_id}` },
          { text: "❌ Reject", callback_data: `reject:${user_id}` }
        ]
      ]
    };

    // Forward notification to Cloudflare Worker as well
    fetch(APPROVAL_WORKER_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        user_id,
        display_name: newName,
        previous_name: oldName,
        bio: newBio,
        previous_bio: oldBio,
        avatar_url: newAvatar,
        previous_avatar: oldAvatar,
        email: safeEmail,
        source: 'Website'
      })
    }).catch(() => {});

    // Direct Telegram Dispatch
    if (isCustomPhoto) {
      if (avatar_url.startsWith('data:image/')) {
        const photoBlob = dataURLtoBlob(avatar_url);
        if (photoBlob) {
          const formData = new FormData();
          formData.append('chat_id', TELEGRAM_MODERATION_CHAT_ID);
          formData.append('photo', photoBlob, 'avatar.jpg');
          formData.append('caption', caption);
          formData.append('parse_mode', 'HTML');
          formData.append('reply_markup', JSON.stringify(keyboard));

          fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/sendPhoto`, {
            method: 'POST',
            body: formData
          }).catch(err => console.debug('Direct Telegram sendPhoto (Blob) error:', err));
        }
      } else if (avatar_url.startsWith('http://') || avatar_url.startsWith('https://')) {
        fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/sendPhoto`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            chat_id: TELEGRAM_MODERATION_CHAT_ID,
            photo: avatar_url,
            caption: caption,
            parse_mode: 'HTML',
            reply_markup: keyboard
          })
        }).catch(err => console.debug('Direct Telegram sendPhoto (URL) error:', err));
      }
    } else {
      fetch(`https://api.telegram.org/bot${TELEGRAM_MODERATION_BOT_TOKEN}/sendMessage`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          chat_id: TELEGRAM_MODERATION_CHAT_ID,
          text: caption,
          parse_mode: 'HTML',
          reply_markup: keyboard
        })
      }).catch(err => console.debug('Direct Telegram sendMessage error:', err));
    }
  } catch (e) {
    console.debug('Moderation webhook exception:', e);
  }
}

async function handleSaveProfile(e) {
  e.preventDefault();
  const nameInput = document.getElementById('inputProfileDisplayName');
  const moodInput = document.getElementById('inputProfileMood');
  const examInput = document.getElementById('inputProfileExam');
  const mottoInput = document.getElementById('inputProfileMotto');
  const flagSelect = document.getElementById('selectProfileCountryFlag');
  const subjectSelect = document.getElementById('selectProfilePrimarySubject');
  const stealthToggle = document.getElementById('checkStealthScholar');
  const publicToggle = document.getElementById('checkLeaderboardPublic');

  const displayName = (nameInput?.value.trim() || 'Student').slice(0, 24);
  const mood = (moodInput?.value.trim() || '').slice(0, 40);
  const examTarget = (examInput?.value.trim() || '').slice(0, 30);
  const motto = (mottoInput?.value.trim() || '').slice(0, 60);
  const countryFlag = flagSelect?.value || '🌐';
  const primarySubjectId = subjectSelect?.value || (appState.subjects[0]?.id || 'math');
  const isStealth = Boolean(stealthToggle?.checked);
  const isPublicLeaderboard = publicToggle ? publicToggle.checked : true;

  // Tier 1 Profanity Defense Check
  if (hasProfanity(displayName) || hasProfanity(mood) || hasProfanity(examTarget) || hasProfanity(motto)) {
    showToast('Please keep display names, moods, and motto respectful & friendly 🛡️', 'error');
    return;
  }

  // Attempt Supabase Storage upload for custom base64 photo to reduce DB row bandwidth
  let avatarValueToSave = sanitizeAvatar(selectedAvatarPreset || '');
  if (avatarValueToSave.startsWith('data:image/') && supabaseClient && appState.currentUser) {
    const storageUrl = await uploadAvatarToSupabaseStorage(avatarValueToSave, appState.currentUser.id);
    if (storageUrl) {
      avatarValueToSave = storageUrl;
      selectedAvatarPreset = storageUrl;
    }
  }

  const isCustomPhoto = /^(http|https|data:|blob:)/i.test(avatarValueToSave.trim());

  // Retain previously approved avatar/sticker as fallback during safety review
  const previousApprovedAvatar = (appState.userProfile?.photoApproved === true && appState.userProfile?.avatarPreset)
    ? appState.userProfile.avatarPreset
    : (appState.userProfile?.fallbackSticker || appState.approvedAvatar || '');

  const prevProfile = appState.userProfile || {};
  const photoChanged = isCustomPhoto && (avatarValueToSave !== prevProfile.avatarPreset);
  const detailsChanged = (displayName !== prevProfile.displayName) ||
                         (mood !== prevProfile.mood) ||
                         (examTarget !== prevProfile.examTarget) ||
                         (motto !== prevProfile.motto) ||
                         (countryFlag !== prevProfile.countryFlag) ||
                         (selectedAvatarRing !== prevProfile.avatarRing);

  const diffs = [];
  if (displayName !== prevProfile.displayName && prevProfile.displayName) {
    diffs.push({ field: 'Display Name', old: prevProfile.displayName, new: displayName });
  }
  if (mood !== prevProfile.mood && prevProfile.mood !== undefined) {
    diffs.push({ field: 'Mood', old: prevProfile.mood || 'None', new: mood || 'None' });
  }
  if (examTarget !== prevProfile.examTarget && prevProfile.examTarget !== undefined) {
    diffs.push({ field: 'Exam Target', old: prevProfile.examTarget || 'None', new: examTarget || 'None' });
  }
  if (motto !== prevProfile.motto && prevProfile.motto !== undefined) {
    diffs.push({ field: 'Motto', old: prevProfile.motto || 'None', new: motto || 'None' });
  }
  if (countryFlag !== prevProfile.countryFlag && prevProfile.countryFlag) {
    diffs.push({ field: 'Flag', old: prevProfile.countryFlag, new: countryFlag });
  }
  if (selectedAvatarRing !== prevProfile.avatarRing && prevProfile.avatarRing) {
    diffs.push({ field: 'Glow Ring', old: prevProfile.avatarRing, new: selectedAvatarRing });
  }
  if (photoChanged) {
    diffs.push({ field: 'Profile Photo', old: prevProfile.avatarPreset ? 'Previous Avatar' : 'Default Sticker', new: isCustomPhoto ? 'New Custom Photo' : avatarValueToSave });
  }

  appState.userProfile = {
    displayName,
    avatarPreset: avatarValueToSave,
    fallbackSticker: isCustomPhoto ? previousApprovedAvatar : avatarValueToSave,
    photoApproved: !isCustomPhoto, // Emoji stickers are auto-approved, custom photos strictly require admin approval
    avatarRing: selectedAvatarRing,
    bannerTheme: selectedBannerTheme,
    countryFlag,
    mood,
    examTarget,
    motto,
    primarySubjectId,
    isStealth,
    isPublicLeaderboard,
    profileStatus: isCustomPhoto ? 'pending' : (hasProfanity(displayName) ? 'pending' : 'approved')
  };

  if (appState.userProfile.profileStatus === 'approved') {
    appState.approvedDisplayName = displayName;
  }

  saveLocalState();
  renderUserProfileUI();
  closeProfileModal();

  if (isCustomPhoto) {
    showToast('Profile saved! Custom photo submitted for safety verification 🛡️', 'success');
  } else {
    showToast('Profile updated successfully! ✨', 'success');
  }

  // Public text modifications (display name, bio/motto, custom photos) trigger moderation review.
  const nameChanged = Boolean(prevProfile.displayName && displayName.trim() !== prevProfile.displayName.trim());
  const bioChanged = Boolean(prevProfile.mood !== undefined && mood.trim() !== (prevProfile.mood || '').trim());
  const requiresModeration = nameChanged || bioChanged || photoChanged || hasProfanity(displayName) || hasProfanity(mood);

  if (requiresModeration) {
    notifyAdminModerationWebhook({
      user_id: appState.currentUser?.id || 'guest_' + Date.now(),
      display_name: displayName,
      previous_name: prevProfile.displayName || '',
      email: appState.currentUser?.email || '',
      status_mood: mood,
      previous_bio: prevProfile.mood || '',
      avatar_url: avatarValueToSave,
      previous_avatar: prevProfile.avatarPreset || '',
      photo_changed: photoChanged
    });
  }

  // Direct cosmetic update to daily_leaderboard using strict approved avatar helper
  const publicAvatarUrl = getPublicLeaderboardAvatarUrl(appState.userProfile);
  if (supabaseClient && appState.currentUser) {
    try {
      await supabaseClient
        .from('daily_leaderboard')
        .update({
          avatar_ring: selectedAvatarRing || 'glow-gold',
          country_flag: countryFlag || '🌐',
          avatar_url: publicAvatarUrl,
          is_stealth: Boolean(isStealth)
        })
        .eq('user_id', appState.currentUser.id);
    } catch (dbErr) {
      console.warn('Leaderboard direct avatar ring sync error:', dbErr);
    }
  }

  await pushDataToCloud(false, true);

  leaderboardTimeframeCache.daily = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.weekly = { data: null, timestamp: 0 };
  leaderboardTimeframeCache.monthly = { data: null, timestamp: 0 };
  const lbModal = document.getElementById('leaderboardModalOverlay');
  if (lbModal && !lbModal.classList.contains('hidden')) {
    fetchLeaderboard(true);
  }
}

function initProfileCustomizationSystem() {
  const form = document.getElementById('profileCustomizationForm');
  form?.addEventListener('submit', handleSaveProfile);

  // Close triggers
  document.getElementById('btnCloseProfileModal')?.addEventListener('click', closeProfileModal);
  document.getElementById('btnCancelProfileModal')?.addEventListener('click', closeProfileModal);
  document.getElementById('profileModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'profileModalOverlay') closeProfileModal();
  });

  // Open triggers
  document.getElementById('btnOpenProfileModal')?.addEventListener('click', openProfileModal);
  document.getElementById('btnOpenProfileCustomizer')?.addEventListener('click', openProfileModal);
  document.getElementById('navUserAvatarWrap')?.addEventListener('click', openProfileModal);
  document.getElementById('btnTopbarUser')?.addEventListener('click', openProfileModal);

  // Quick Change Photo button on Hero Avatar Showcase
  document.getElementById('btnHeroQuickChangePhoto')?.addEventListener('click', (e) => {
    e.stopPropagation();
    switchProfileTab('tab-avatar');
    document.getElementById('inputProfilePhotoFile')?.click();
  });
  document.getElementById('heroAvatarCircleWrap')?.addEventListener('click', () => {
    switchProfileTab('tab-avatar');
  });

  // Tab navigation
  document.querySelectorAll('.profile-tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const tabName = btn.dataset.tab;
      if (tabName) switchProfileTab(tabName);
    });
  });

  // Avatar Submode Segmented Toggle (Custom Photo vs Emoji Stickers)
  document.getElementById('btnSubmodePhoto')?.addEventListener('click', () => {
    document.getElementById('btnSubmodePhoto')?.classList.add('active');
    document.getElementById('btnSubmodeStickers')?.classList.remove('active');
    document.getElementById('submodePhotoSection')?.classList.remove('hidden');
    document.getElementById('submodeStickersSection')?.classList.add('hidden');
  });

  document.getElementById('btnSubmodeStickers')?.addEventListener('click', () => {
    document.getElementById('btnSubmodeStickers')?.classList.add('active');
    document.getElementById('btnSubmodePhoto')?.classList.remove('active');
    document.getElementById('submodeStickersSection')?.classList.remove('hidden');
    document.getElementById('submodePhotoSection')?.classList.add('hidden');
  });

  // Sticker Category Filter Pills
  document.querySelectorAll('.sticker-cat-pill').forEach(pill => {
    pill.addEventListener('click', () => {
      document.querySelectorAll('.sticker-cat-pill').forEach(p => p.classList.remove('active'));
      pill.classList.add('active');
      const cat = pill.dataset.cat;
      document.querySelectorAll('#avatarPresetsGrid .avatar-preset-btn').forEach(btn => {
        if (cat === 'all' || btn.dataset.cat === cat) {
          btn.style.display = 'flex';
        } else {
          btn.style.display = 'none';
        }
      });
    });
  });

  // Photo URL Live Validation with debounce
  let photoUrlDebounceTimer = null;

  function testAndVerifyProfilePhotoUrl(url, callback) {
    const statusCard = document.getElementById('photoUrlStatusCard');
    const statusIcon = document.getElementById('photoUrlStatusIcon');
    const statusTitle = document.getElementById('photoUrlStatusTitle');
    const statusDesc = document.getElementById('photoUrlStatusDesc');
    const photoPreviewImg = document.getElementById('customPhotoPreviewImg');
    const photoFallback = document.getElementById('customPhotoPreviewFallback');
    const btnRemovePhoto = document.getElementById('btnRemoveCustomPhoto');

    const trimmed = (url || '').trim();
    if (!trimmed) {
      if (statusCard) statusCard.classList.add('hidden');
      if (typeof callback === 'function') callback(false);
      return;
    }

    if (statusCard) {
      statusCard.className = 'photo-url-status-card is-testing';
      statusCard.classList.remove('hidden');
      if (statusIcon) statusIcon.textContent = '⏳';
      if (statusTitle) statusTitle.textContent = 'Testing Image URL...';
      if (statusDesc) statusDesc.textContent = 'Checking image format and cross-origin access...';
    }

    if (!trimmed.startsWith('http://') && !trimmed.startsWith('https://') && !trimmed.startsWith('data:image/')) {
      if (statusCard) {
        statusCard.className = 'photo-url-status-card is-error';
        if (statusIcon) statusIcon.textContent = '❌';
        if (statusTitle) statusTitle.textContent = 'Invalid Format';
        if (statusDesc) statusDesc.textContent = 'URL must start with https:// or http://';
      }
      if (typeof callback === 'function') callback(false);
      return;
    }

    const testImg = new Image();
    testImg.onload = () => {
      if (statusCard) {
        statusCard.className = 'photo-url-status-card is-valid';
        if (statusIcon) statusIcon.textContent = '✓';
        if (statusTitle) statusTitle.textContent = 'Supported Image';
        const isGif = trimmed.toLowerCase().includes('.gif');
        statusDesc.textContent = `${isGif ? 'Animated GIF' : 'Image format'} (${testImg.naturalWidth || 0}×${testImg.naturalHeight || 0}px)`;
      }

      selectedAvatarPreset = trimmed;
      if (photoPreviewImg && photoFallback) {
        photoPreviewImg.src = trimmed;
        photoPreviewImg.classList.remove('hidden');
        photoFallback.classList.add('hidden');
        btnRemovePhoto?.classList.remove('hidden');
      }
      document.querySelectorAll('.avatar-preset-btn').forEach(b => b.classList.remove('active'));
      updateProfileLivePreview();
      if (typeof callback === 'function') callback(true);
    };

    testImg.onerror = () => {
      if (statusCard) {
        statusCard.className = 'photo-url-status-card is-error';
        if (statusIcon) statusIcon.textContent = '❌';
        if (statusTitle) statusTitle.textContent = 'Cannot Load Image';
        statusDesc.textContent = 'Link is broken, forbidden (hotlinking blocked), or unsupported format.';
      }
      if (typeof callback === 'function') callback(false);
    };

    testImg.src = trimmed;
  }

  // File Uploader with HTML5 Canvas Compression
  const photoInput = document.getElementById('inputProfilePhotoFile');
  photoInput?.addEventListener('change', (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 8 * 1024 * 1024) {
      showToast('Image file too large (max 8MB).', 'warning');
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const size = 180;
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext('2d');
        
        // Square crop from center
        const minDim = Math.min(img.width, img.height);
        const sx = (img.width - minDim) / 2;
        const sy = (img.height - minDim) / 2;
        ctx.drawImage(img, sx, sy, minDim, minDim, 0, 0, size, size);

        const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);
        selectedAvatarPreset = compressedDataUrl;

        const photoPreviewImg = document.getElementById('customPhotoPreviewImg');
        const photoFallback = document.getElementById('customPhotoPreviewFallback');
        const btnRemovePhoto = document.getElementById('btnRemoveCustomPhoto');
        if (photoPreviewImg && photoFallback) {
          photoPreviewImg.src = compressedDataUrl;
          photoPreviewImg.classList.remove('hidden');
          photoFallback.classList.add('hidden');
          btnRemovePhoto?.classList.remove('hidden');
        }

        const statusCard = document.getElementById('photoUrlStatusCard');
        const statusIcon = document.getElementById('photoUrlStatusIcon');
        const statusTitle = document.getElementById('photoUrlStatusTitle');
        const statusDesc = document.getElementById('photoUrlStatusDesc');
        if (statusCard) {
          statusCard.className = 'photo-url-status-card is-valid';
          statusCard.classList.remove('hidden');
          if (statusIcon) statusIcon.textContent = '✓';
          if (statusTitle) statusTitle.textContent = 'Custom Photo Selected';
          if (statusDesc) statusDesc.textContent = `${file.name} (Auto-compressed)`;
        }

        document.querySelectorAll('.avatar-preset-btn').forEach(b => b.classList.remove('active'));
        updateProfileLivePreview();
        showToast('Photo selected! Live preview updated.', 'info');
      };
      img.src = event.target.result;
    };
    reader.readAsDataURL(file);
  });

  // URL Photo input listeners with live auto-test
  const photoUrlInputEl = document.getElementById('inputProfilePhotoUrl');
  photoUrlInputEl?.addEventListener('input', (e) => {
    clearTimeout(photoUrlDebounceTimer);
    const val = e.target.value;
    if (!val || (!val.startsWith('http://') && !val.startsWith('https://'))) {
      testAndVerifyProfilePhotoUrl(val);
    } else {
      photoUrlDebounceTimer = setTimeout(() => {
        testAndVerifyProfilePhotoUrl(val);
      }, 200);
    }
  });

  photoUrlInputEl?.addEventListener('paste', () => {
    setTimeout(() => {
      testAndVerifyProfilePhotoUrl(photoUrlInputEl.value);
    }, 50);
  });

  // URL Photo Apply button
  document.getElementById('btnApplyPhotoUrl')?.addEventListener('click', () => {
    const val = photoUrlInputEl?.value.trim();
    if (!val) {
      showToast('Please enter an image URL.', 'warning');
      return;
    }
    testAndVerifyProfilePhotoUrl(val, (isValid) => {
      if (isValid) {
        showToast('Valid image verified & applied! Live preview updated.', 'success');
      } else {
        showToast('Image URL could not be loaded or format is unsupported.', 'error');
      }
    });
  });

  // Remove Photo / Reset
  document.getElementById('btnRemoveCustomPhoto')?.addEventListener('click', () => {
    selectedAvatarPreset = '';
    const photoPreviewImg = document.getElementById('customPhotoPreviewImg');
    const photoFallback = document.getElementById('customPhotoPreviewFallback');
    const btnRemovePhoto = document.getElementById('btnRemoveCustomPhoto');
    const photoUrlInput = document.getElementById('inputProfilePhotoUrl');
    const statusCard = document.getElementById('photoUrlStatusCard');

    if (photoPreviewImg && photoFallback) {
      photoPreviewImg.src = '';
      photoPreviewImg.classList.add('hidden');
      photoFallback.textContent = (appState.userProfile?.displayName || 'S').trim().charAt(0).toUpperCase() || 'S';
      photoFallback.classList.remove('hidden');
      btnRemovePhoto?.classList.add('hidden');
    }
    if (photoUrlInput) photoUrlInput.value = '';
    if (statusCard) statusCard.classList.add('hidden');

    document.querySelectorAll('.avatar-preset-btn').forEach(b => b.classList.remove('active'));
    updateProfileLivePreview();
    showToast('Profile photo removed.', 'info');
  });

  // Avatar Presets
  document.querySelectorAll('.avatar-preset-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.avatar-preset-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedAvatarPreset = btn.dataset.avatar || '';

      const photoPreviewImg = document.getElementById('customPhotoPreviewImg');
      const photoFallback = document.getElementById('customPhotoPreviewFallback');
      const btnRemovePhoto = document.getElementById('btnRemoveCustomPhoto');
      if (photoPreviewImg && photoFallback) {
        photoPreviewImg.src = '';
        photoPreviewImg.classList.add('hidden');
        photoFallback.classList.remove('hidden');
        btnRemovePhoto?.classList.add('hidden');
      }
      const photoUrlInput = document.getElementById('inputProfilePhotoUrl');
      if (photoUrlInput) photoUrlInput.value = '';

      updateProfileLivePreview();
    });
  });

  // Banner Theme Presets
  document.querySelectorAll('.banner-theme-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.banner-theme-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedBannerTheme = btn.dataset.banner || 'banner-midnight';
      updateProfileLivePreview();
    });
  });

  // Avatar Glow Ring Presets
  document.querySelectorAll('.glow-ring-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.glow-ring-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedAvatarRing = btn.dataset.ring || 'glow-gold';
      updateProfileLivePreview();
    });
  });

  // Live input change listeners
  ['inputProfileDisplayName', 'inputProfileMood', 'inputProfileExam', 'inputProfileMotto'].forEach(id => {
    document.getElementById(id)?.addEventListener('input', updateProfileLivePreview);
  });
  document.getElementById('selectProfileCountryFlag')?.addEventListener('change', (e) => {
    selectedCountryFlag = e.target.value;
    updateProfileLivePreview();
  });
  document.getElementById('checkStealthScholar')?.addEventListener('change', updateProfileLivePreview);
}

// Call during initial script parsing
if (typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initProfileCustomizationSystem);
  } else {
    initProfileCustomizationSystem();
  }
}

// ============================================================================

// REAL-TIME STUDY PRESENCE & LEADERBOARD DATA SYNC
// ----------------------------------------------------------------------------

async function updateStudyPresence(isStudying = false) {
  if (!supabaseClient || !appState.currentUser) return;

  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) {
    return;
  }

  const defaultAuthName = appState.currentUser.user_metadata?.full_name || 
                          appState.currentUser.user_metadata?.name || 
                          appState.currentUser.email?.split('@')[0] || 
                          'Student';
  const isPendingOrRejected = profile.profileStatus === 'pending' || profile.profileStatus === 'rejected';

  // Strict moderation gate: unapproved names NEVER go to daily_leaderboard table
  const userName = (isPendingOrRejected 
    ? (appState.approvedDisplayName || defaultAuthName)
    : (profile.displayName || defaultAuthName)
  ).slice(0, 50);

  const currentSub = appState.selectedSubject || { name: 'Focus Study', color: '#3b82f6' };
  // Strict moderation gate: unapproved custom photos strictly fallback to sticker on public presence
  const avatarUrl = getPublicLeaderboardAvatarUrl(profile);

  const shouldBeStudying = isStudying && currentMode !== 'break' && timerStatus === 'RUNNING';

  try {
    await supabaseClient.rpc('update_study_presence', {
      p_user_id: appState.currentUser.id,
      p_user_name: userName,
      p_avatar_url: avatarUrl,
      p_is_studying: shouldBeStudying,
      p_current_subject: shouldBeStudying ? (currentSub.name || 'Focus Study') : '',
      p_subject_color: shouldBeStudying ? (currentSub.color || '#3b82f6') : '#3b82f6',
      p_study_date: getLocalDateStr()
    });
  } catch (err) {
    console.warn('Presence update error:', err);
  }
}

async function syncStudyProgressToLeaderboard(incrementalSeconds = 0) {
  if (!supabaseClient || !appState.currentUser) return;
  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) return;

  const defaultAuthName = appState.currentUser.user_metadata?.full_name || 
                          appState.currentUser.user_metadata?.name || 
                          appState.currentUser.email?.split('@')[0] || 
                          'Student';
  const isPendingOrRejected = profile.profileStatus === 'pending' || profile.profileStatus === 'rejected';

  // Strict moderation gate: unapproved names NEVER go to daily_leaderboard table
  const userName = (isPendingOrRejected 
    ? (appState.approvedDisplayName || defaultAuthName)
    : (profile.displayName || defaultAuthName)
  ).slice(0, 50);

  const currentSub = appState.selectedSubject || { name: 'Focus Study', color: '#3b82f6' };
  // Strict moderation gate: unapproved custom photos strictly fallback to sticker on public progress sync
  const avatarUrl = getPublicLeaderboardAvatarUrl(profile);

  const isStudying = timerStatus === 'RUNNING' && currentMode !== 'break';
  const todayKey = getLocalDateStr();
  const currentDailyTotal = (appState.dailyFocusTotals && appState.dailyFocusTotals[todayKey]) || 0;
  // Enforce 16h daily hard cap on leaderboard increments
  const effectiveIncrementalSeconds = currentDailyTotal >= MAX_DAILY_LEADERBOARD_SECONDS ? 0 : Math.min(Math.max(incrementalSeconds, 0), 600);

  try {
    if (effectiveIncrementalSeconds > 0) {
      await supabaseClient.rpc('sync_study_progress_leaderboard', {
        p_user_id: appState.currentUser.id,
        p_user_name: userName,
        p_avatar_url: avatarUrl,
        p_incremental_seconds: effectiveIncrementalSeconds,
        p_is_studying: isStudying,
        p_subject: isStudying ? (currentSub.name || 'Focus Study') : '',
        p_subject_color: isStudying ? (currentSub.color || '#3b82f6') : '#3b82f6',
        p_study_date: todayKey
      });
    } else {
      await supabaseClient.rpc('update_study_presence', {
        p_user_id: appState.currentUser.id,
        p_user_name: userName,
        p_avatar_url: avatarUrl,
        p_is_studying: isStudying,
        p_current_subject: isStudying ? (currentSub.name || 'Focus Study') : '',
        p_subject_color: isStudying ? (currentSub.color || '#3b82f6') : '#3b82f6',
        p_study_date: todayKey
      });
    }
  } catch (err) {
    console.warn('Leaderboard progress sync error:', err);
  }
}

function startPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
  }
  syncStudyProgressToLeaderboard(0);
  // Send single aggregated heartbeat and incremental sync every 60 seconds while timer is actively running
  presenceHeartbeatInterval = setInterval(() => {
    if (timerStatus === 'RUNNING' && currentMode !== 'break') {
      syncStudyProgressToLeaderboard(60);
    }
  }, 60000);
}

function stopPresenceHeartbeat() {
  if (presenceHeartbeatInterval) {
    clearInterval(presenceHeartbeatInterval);
    presenceHeartbeatInterval = null;
  }
  updateStudyPresence(false);
}

async function logSessionToLeaderboard(durationSec, subject) {
  if (!supabaseClient || !appState.currentUser || durationSec < 10) return;

  const profile = appState.userProfile || {};
  if (profile.isPublicLeaderboard === false) {
    return;
  }

  const userName = (profile.displayName || 
                   appState.currentUser.user_metadata?.full_name || 
                   appState.currentUser.user_metadata?.name || 
                   appState.currentUser.email?.split('@')[0] || 
                   'Student').slice(0, 50);
  // Strict moderation gate: unapproved custom photos strictly fallback to sticker on recorded sessions
  const avatarUrl = getPublicLeaderboardAvatarUrl(profile);

  try {
    await supabaseClient.rpc('record_study_session_leaderboard', {
      p_user_id: appState.currentUser.id,
      p_user_name: userName,
      p_avatar_url: avatarUrl,
      p_duration_seconds: Math.min(Math.max(durationSec, 1), 50400),
      p_subject: subject?.name || 'Focus Study',
      p_subject_color: subject?.color || '#3b82f6',
      p_study_date: getLocalDateStr()
    });

    // Invalidate local cache and refresh leaderboard view if open
    leaderboardTimeframeCache.daily = { data: null, timestamp: 0 };
    leaderboardTimeframeCache.weekly = { data: null, timestamp: 0 };
    leaderboardTimeframeCache.monthly = { data: null, timestamp: 0 };

    const modal = document.getElementById('leaderboardModalOverlay');
    if (modal && !modal.classList.contains('hidden')) {
      fetchLeaderboard(true);
    }
  } catch (err) {
    console.warn('Leaderboard session log error:', err);
  }
}

let currentLeaderboardPeriod = 'daily'; // 'daily' | 'weekly' | 'monthly'
let leaderboardTimeframeCache = {
  daily: { data: null, timestamp: 0 },
  weekly: { data: null, timestamp: 0 },
  monthly: { data: null, timestamp: 0 }
};

function switchLeaderboardTimeframe(period) {
  if (!['daily', 'weekly', 'monthly'].includes(period)) period = 'daily';
  currentLeaderboardPeriod = period;

  // Update tabs UI
  document.querySelectorAll('.lb-timeframe-tab').forEach(btn => {
    const isTarget = btn.dataset.period === period;
    btn.classList.toggle('active', isTarget);
    btn.setAttribute('aria-selected', isTarget ? 'true' : 'false');
  });

  // Update title heading
  const heading = document.getElementById('leaderboardModalHeading');
  if (heading) {
    if (period === 'daily') heading.textContent = 'Daily Leaderboard';
    else if (period === 'weekly') heading.textContent = 'Weekly Leaderboard';
    else if (period === 'monthly') heading.textContent = 'Monthly Leaderboard';
  }

  // Update countdown timer
  updateLeaderboardCountdownDisplay();

  // Fetch rankings for the selected period
  fetchLeaderboard(false);
}

async function openLeaderboardModal() {
  const modal = document.getElementById('leaderboardModalOverlay');
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    initLeaderboardRealtime();
    switchLeaderboardTimeframe(currentLeaderboardPeriod || 'daily');
  }
}

function closeLeaderboardModal() {
  document.getElementById('leaderboardModalOverlay')?.classList.add('hidden');
  teardownLeaderboardRealtime();
  unlockBodyScroll();
}

// Helpers for dates
function getStartOfWeekDateStr() {
  const d = new Date();
  const day = d.getDay(); // 0 is Sunday
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Monday
  const monday = new Date(d.setDate(diff));
  return getLocalDateStr(monday);
}

function getStartOfMonthDateStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

let isLeaderboardFetchInProgress = false;

async function fetchLeaderboard(forceRefresh = false, showSpinning = false) {
  const now = Date.now();
  const period = currentLeaderboardPeriod || 'daily';
  startResetCountdownTimer();

  const cached = leaderboardTimeframeCache[period];
  // 1. SWR: Instant render from cache to eliminate UI lag
  if (cached && cached.data) {
    renderLeaderboard(cached.data, period);
    if (!forceRefresh && (now - cached.timestamp < LEADERBOARD_CACHE_TTL_MS)) {
      return;
    }
  }

  if (isLeaderboardFetchInProgress) return;
  isLeaderboardFetchInProgress = true;

  const refreshBtn = document.getElementById('btnRefreshLeaderboard');
  if (showSpinning) {
    refreshBtn?.classList.add('spinning');
  }

  const LB_SELECT_COLUMNS = 'user_id,user_name,avatar_url,avatar_ring,country_flag,status_mood,exam_tag,is_stealth,total_seconds,is_studying,current_subject,subject_color,last_active_at,study_date';

  try {
    if (supabaseClient) {
      const todayStr = getLocalDateStr();
      let rankings = null;

      if (period === 'daily') {
        const { data: rpcData, error: rpcErr } = await supabaseClient.rpc('get_daily_leaderboard', {
          p_date: todayStr,
          p_limit: 50
        });

        if (!rpcErr && rpcData && rpcData.length > 0) {
          rankings = rpcData;
        } else {
          const { data: tableData } = await supabaseClient
            .from('daily_leaderboard')
            .select(LB_SELECT_COLUMNS)
            .eq('study_date', todayStr)
            .order('total_seconds', { ascending: false })
            .limit(50);
          if (tableData && tableData.length > 0) {
            rankings = aggregateLeaderboardEntries(tableData);
          }
        }
      } else if (period === 'weekly') {
        const startWeekStr = getStartOfWeekDateStr();
        const { data: rpcData, error: rpcErr } = await supabaseClient.rpc('get_weekly_leaderboard', {
          p_start_date: startWeekStr,
          p_end_date: todayStr,
          p_limit: 50
        });

        if (!rpcErr && rpcData && rpcData.length > 0) {
          rankings = rpcData;
        } else {
          const { data: tableData } = await supabaseClient
            .from('daily_leaderboard')
            .select(LB_SELECT_COLUMNS)
            .gte('study_date', startWeekStr)
            .lte('study_date', todayStr)
            .order('total_seconds', { ascending: false })
            .limit(50);
          if (tableData && tableData.length > 0) {
            rankings = aggregateLeaderboardEntries(tableData);
          }
        }
      } else if (period === 'monthly') {
        const startMonthStr = getStartOfMonthDateStr();
        const { data: rpcData, error: rpcErr } = await supabaseClient.rpc('get_monthly_leaderboard', {
          p_start_date: startMonthStr,
          p_end_date: todayStr,
          p_limit: 50
        });

        if (!rpcErr && rpcData && rpcData.length > 0) {
          rankings = rpcData;
        } else {
          const { data: tableData } = await supabaseClient
            .from('daily_leaderboard')
            .select(LB_SELECT_COLUMNS)
            .gte('study_date', startMonthStr)
            .lte('study_date', todayStr)
            .order('total_seconds', { ascending: false })
            .limit(50);
          if (tableData && tableData.length > 0) {
            rankings = aggregateLeaderboardEntries(tableData);
          }
        }
      }

      if (rankings && Array.isArray(rankings)) {
        leaderboardTimeframeCache[period] = { data: rankings, timestamp: now };
        renderLeaderboard(rankings, period);
      } else if (!cached?.data) {
        fallbackLocalLeaderboard(period);
      }
    } else if (!cached?.data) {
      fallbackLocalLeaderboard(period);
    }
  } catch (err) {
    console.warn('Leaderboard fetch notice:', err);
    if (!cached?.data) fallbackLocalLeaderboard(period);
  } finally {
    isLeaderboardFetchInProgress = false;
    if (showSpinning) {
      setTimeout(() => refreshBtn?.classList.remove('spinning'), 300);
    }
  }
}

// Client-side aggregator for table rows
function aggregateLeaderboardEntries(rows) {
  const userMap = new Map();
  const threeMinsAgo = Date.now() - 3 * 60 * 1000;

  (rows || []).forEach(row => {
    if (!row || !row.user_id) return;
    const uid = row.user_id;
    const isRecentActive = row.last_active_at ? new Date(row.last_active_at).getTime() > threeMinsAgo : false;
    const isStudyingNow = Boolean(row.is_studying && isRecentActive);
    const ring = normalizeRingClass(row.avatar_ring);

    if (!userMap.has(uid)) {
      userMap.set(uid, {
        user_id: uid,
        user_name: row.user_name || 'Student',
        avatar_url: row.avatar_url || '',
        avatar_ring: ring,
        country_flag: row.country_flag || '🌐',
        status_mood: row.status_mood || '',
        exam_tag: row.exam_tag || '',
        is_stealth: Boolean(row.is_stealth),
        total_seconds: Number(row.total_seconds) || 0,
        is_studying: isStudyingNow,
        current_subject: row.current_subject || '',
        subject_color: row.subject_color || '#3b82f6',
        last_active_at: row.last_active_at || new Date().toISOString()
      });
    } else {
      const existing = userMap.get(uid);
      existing.total_seconds += (Number(row.total_seconds) || 0);
      if (row.user_name && row.user_name !== 'Student') existing.user_name = row.user_name;
      if (row.avatar_url) existing.avatar_url = row.avatar_url;
      if (row.avatar_ring) existing.avatar_ring = ring;
      if (row.country_flag) existing.country_flag = row.country_flag;
      if (row.status_mood) existing.status_mood = row.status_mood;
      if (row.exam_tag) existing.exam_tag = row.exam_tag;
      if (row.is_stealth !== undefined) existing.is_stealth = Boolean(row.is_stealth);
      if (isStudyingNow) {
        existing.is_studying = true;
        existing.current_subject = row.current_subject || existing.current_subject;
        existing.subject_color = row.subject_color || existing.subject_color;
      }
      if (new Date(row.last_active_at) > new Date(existing.last_active_at)) {
        existing.last_active_at = row.last_active_at;
      }
    }
  });

  const sorted = Array.from(userMap.values()).sort((a, b) => b.total_seconds - a.total_seconds);
  return sorted.slice(0, 50);
}

function isCurrentUserEntry(entry) {
  if (!entry) return false;
  const currentUserId = appState.currentUser?.id;
  const currentUserEmail = appState.currentUser?.email;

  if (currentUserId && entry.user_id === currentUserId) return true;
  if (currentUserEmail && (entry.user_id === currentUserEmail || entry.user_id === currentUserEmail.split('@')[0])) return true;
  return false;
}

function calculateLocalFocusSecondsForPeriod(period) {
  const todayKey = getIsoDateStr();
  let totalSec = 0;

  if (period === 'daily') {
    (appState.todaySessions || []).forEach(s => {
      if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
    });
    if (appState.dailyFocusTotals && appState.dailyFocusTotals[todayKey]) {
      totalSec = Math.max(totalSec, Number(appState.dailyFocusTotals[todayKey]) || 0);
    }
  } else if (period === 'weekly') {
    const startWeekStr = getStartOfWeekDateStr();
    const totals = appState.dailyFocusTotals || {};
    Object.keys(totals).forEach(k => {
      if (k >= startWeekStr && k <= todayKey) {
        totalSec += Number(totals[k]) || 0;
      }
    });
    if (totalSec === 0) {
      (appState.todaySessions || []).forEach(s => {
        if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
      });
    }
  } else if (period === 'monthly') {
    const startMonthStr = getStartOfMonthDateStr();
    const totals = appState.dailyFocusTotals || {};
    Object.keys(totals).forEach(k => {
      if (k >= startMonthStr && k <= todayKey) {
        totalSec += Number(totals[k]) || 0;
      }
    });
    if (totalSec === 0) {
      (appState.todaySessions || []).forEach(s => {
        if (s && typeof s.durationSec === 'number') totalSec += s.durationSec;
      });
    }
  }

  return totalSec;
}

function fallbackLocalLeaderboard(period = 'daily') {
  const totalSec = calculateLocalFocusSecondsForPeriod(period);
  const isUserStudying = timerStatus === 'RUNNING' && currentMode !== 'break';
  const dummyRanks = [];

  const profile = appState.userProfile || {};
  const userName = profile.displayName || appState.currentUser?.user_metadata?.full_name || 'You';
  const avatar = getPublicLeaderboardAvatarUrl(profile);

  if (totalSec > 0 || isUserStudying) {
    dummyRanks.push({
      rank: 1,
      user_id: appState.currentUser ? appState.currentUser.id : 'guest',
      user_name: userName,
      avatar_url: avatar,
      total_seconds: totalSec,
      is_studying: isUserStudying,
      current_subject: isUserStudying ? (appState.selectedSubject?.name || 'Mathematics') : '',
      subject_color: appState.selectedSubject?.color || '#3b82f6'
    });
  }

  renderLeaderboard(dummyRanks, period);
}

function renderLeaderboard(rankings, period = currentLeaderboardPeriod) {
  const podiumContainer = document.getElementById('leaderboardPodium');
  const listContainer = document.getElementById('leaderboardListItems');
  const activeStudyingText = document.getElementById('leaderboardActiveStudyingText');

  if (!podiumContainer || !listContainer) return;

  // Deduplicate rankings by canonical key (user_name or user_id)
  const seenUsers = new Set();
  const uniqueRankings = [];
  (rankings || []).forEach(r => {
    if (!r) return;
    const key = (r.user_name || '').trim().toLowerCase() || r.user_id;
    if (!seenUsers.has(key)) {
      seenUsers.add(key);
      uniqueRankings.push({ ...r });
    }
  });

  // Re-index ranks 1..N
  uniqueRankings.forEach((r, idx) => {
    r.rank = idx + 1;
  });

  // 1. Calculate Active Studiers Count
  const activeStudyingCount = uniqueRankings.filter(r => r.is_studying).length;
  if (activeStudyingText) {
    activeStudyingText.textContent = activeStudyingCount > 0 
      ? `${activeStudyingCount} Studying Now` 
      : 'Live Leaderboard';
  }

  const headerLiveDot = document.getElementById('headerLiveDot');
  const mobileLiveDot = document.getElementById('mobileLiveDot');
  const isAnyActive = activeStudyingCount > 0 || (timerStatus === 'RUNNING' && currentMode !== 'break');
  if (headerLiveDot) headerLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';
  if (mobileLiveDot) mobileLiveDot.style.display = isAnyActive ? 'inline-block' : 'none';

  const top1 = uniqueRankings.find(r => Number(r.rank) === 1);
  const top2 = uniqueRankings.find(r => Number(r.rank) === 2);
  const top3 = uniqueRankings.find(r => Number(r.rank) === 3);

  const renderPodiumCard = (entry, rankNum) => {
    if (!entry) {
      return `
        <div class="podium-card rank-${rankNum} empty-podium">
          <div class="podium-avatar-wrap">
            <div class="podium-avatar-img empty-avatar"></div>
            <span class="podium-rank-pill">${rankNum}</span>
          </div>
          <span class="podium-name text-muted">Open Spot</span>
          <span class="podium-time text-muted">--</span>
        </div>
      `;
    }

    const isCurrent = isCurrentUserEntry(entry);
    const ring = normalizeRingClass(entry.avatar_ring || (isCurrent ? (appState.userProfile?.avatarRing || 'glow-gold') : 'glow-gold'));
    const flagEmoji = getCountryFlagEmoji(entry.country_flag || (isCurrent ? (appState.userProfile?.countryFlag || '') : ''));
    const flagHtml = (flagEmoji && flagEmoji !== '🌐') ? `<span class="lb-flag-bottom" title="Region">${flagEmoji}</span>` : '';
    const crown = rankNum === 1 ? '<span class="podium-crown-badge">👑</span>' : '';
    const timeFormatted = formatLeaderboardTime(entry.total_seconds);
    const userAvatarVal = isCurrent 
      ? (appState.userProfile?.avatarPreset || appState.userProfile?.avatarUrl || entry.avatar_url || '')
      : (entry.avatar_url || '');
    const avatarHtml = getAvatarElementHtml(userAvatarVal, entry.user_name, 'podium-avatar-img', ring);

    let statusChip = '';
    if (entry.is_studying) {
      statusChip = `
        <span class="live-status-chip studying" title="Actively studying now">
          <span class="status-dot"></span>
          <span>${entry.current_subject ? entry.current_subject : 'Studying'}</span>
        </span>
      `;
    } else {
      statusChip = `
        <span class="live-status-chip resting" title="Resting">
          <span class="status-dot"></span>
          <span>Resting</span>
        </span>
      `;
    }

    return `
      <div class="podium-card rank-${rankNum} ${isCurrent ? 'is-current-user' : ''}">
        ${crown}
        <div class="podium-avatar-wrap">
          ${avatarHtml}
          <span class="podium-rank-pill">${rankNum}</span>
        </div>
        <div class="podium-name-block">
          <span class="podium-name" title="${entry.user_name}">${isCurrent ? 'You' : entry.user_name}</span>
          ${flagHtml}
        </div>
        <span class="podium-time">${timeFormatted}</span>
        ${statusChip}
      </div>
    `;
  };

  // Update podium only if HTML changed to prevent crown & avatar flicker
  // Fix H1: normalize whitespace before comparing to avoid spurious re-renders from whitespace differences
  const normalizeHtml = (s) => s.replace(/\s+/g, ' ').trim();
  const newPodiumHtml = `
    ${renderPodiumCard(top2, 2)}
    ${renderPodiumCard(top1, 1)}
    ${renderPodiumCard(top3, 3)}
  `.trim();

  if (normalizeHtml(podiumContainer.innerHTML) !== normalizeHtml(newPodiumHtml)) {
    podiumContainer.innerHTML = newPodiumHtml;
  }

  const periodLabel = period === 'daily' ? 'today' : (period === 'weekly' ? 'this week' : 'this month');
  let newListHtml = '';

  if (uniqueRankings.length === 0) {
    newListHtml = `
      <div class="leaderboard-empty-state" style="padding: 24px 16px;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="8" r="6"></circle>
          <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11"></path>
        </svg>
        <p>No study sessions recorded ${periodLabel} yet.</p>
        <span>Start a timer session to claim the #1 spot on the leaderboard!</span>
      </div>
    `;
    const localSec = calculateLocalFocusSecondsForPeriod(period);
    updatePersonalUserBar(null, localSec);
  } else {
    const remainingRanks = uniqueRankings.filter(r => Number(r.rank) > 3);
    if (remainingRanks.length === 0) {
      newListHtml = `
        <div class="leaderboard-empty-state" style="padding: 24px 16px;">
          <p style="font-size: 0.85rem;">Only ${uniqueRankings.length} on the board ${periodLabel}!</p>
          <span>Complete a session to join the top rankings.</span>
        </div>
      `;
    } else {
      newListHtml = remainingRanks.map(r => {
        const isCurrent = isCurrentUserEntry(r);
        const ring = normalizeRingClass(r.avatar_ring || (isCurrent ? (appState.userProfile?.avatarRing || 'glow-gold') : 'glow-gold'));
        const flagEmoji = getCountryFlagEmoji(r.country_flag || (isCurrent ? (appState.userProfile?.countryFlag || '') : ''));
        const flagHtml = (flagEmoji && flagEmoji !== '🌐') ? `<span class="lb-flag-bottom" title="Region">${flagEmoji}</span>` : '';
        const timeFormatted = formatLeaderboardTime(r.total_seconds);
        const userAvatarVal = isCurrent
          ? (appState.userProfile?.avatarPreset || appState.userProfile?.avatarUrl || r.avatar_url || '')
          : (r.avatar_url || '');
        const avatarHtml = getAvatarElementHtml(userAvatarVal, r.user_name, 'row-avatar-img', ring);

        const moodHtml = r.status_mood ? `<span class="preview-mood-pill" style="font-size:0.68rem; padding:1px 6px;">${r.status_mood}</span>` : '';
        const examHtml = r.exam_tag ? `<span class="preview-exam-badge" style="font-size:0.68rem; padding:1px 6px;">${r.exam_tag}</span>` : '';
        const tagsLine = (moodHtml || examHtml) ? `<div class="row-user-tags" style="display:flex; gap:4px; margin-top:2px;">${moodHtml}${examHtml}</div>` : '';

        const statusHtml = r.is_studying
          ? `<span class="live-status-chip studying"><span class="status-dot"></span><span>${r.current_subject || 'Studying'}</span></span>`
          : `<span class="live-status-chip resting"><span class="status-dot"></span><span>Resting</span></span>`;

        return `
          <div class="leaderboard-row ${isCurrent ? 'is-current-user' : ''}">
            <span class="row-rank-num">#${r.rank}</span>
            <div class="row-user-col">
              ${avatarHtml}
              <div class="row-user-info-col">
                <span class="row-user-name" title="${r.user_name}">${isCurrent ? 'You' : r.user_name}</span>
                ${flagHtml}
                ${tagsLine}
              </div>
            </div>
            <div>${statusHtml}</div>
            <span class="row-time">${timeFormatted}</span>
          </div>
        `;
      }).join('');
    }
  }

  if (listContainer.innerHTML.trim() !== newListHtml.trim()) {
    listContainer.innerHTML = newListHtml;
  }

  // 4. Update Personal User Bar
  const localTotalSec = calculateLocalFocusSecondsForPeriod(period);
  const myEntry = uniqueRankings.find(r => isCurrentUserEntry(r));
  updatePersonalUserBar(myEntry, localTotalSec);
}

function updatePersonalUserBar(myEntry, localTotalSec) {
  const guestCta = document.getElementById('leaderboardGuestCta');
  const userBar = document.getElementById('leaderboardUserBar');
  const userBarRank = document.getElementById('userBarRank');
  const userBarAvatarWrap = document.getElementById('userBarAvatarWrap');
  const userBarName = document.getElementById('userBarName');
  const userBarStatus = document.getElementById('userBarStatus');
  const userBarTime = document.getElementById('userBarTime');

  if (!appState.currentUser) {
    // GUEST: Show Join the Leaderboard CTA Card
    if (guestCta) guestCta.classList.remove('hidden');
    if (userBar) userBar.classList.add('hidden');
    return;
  }

  // LOGGED-IN: Show Personal Sticky Rank Card
  if (guestCta) guestCta.classList.add('hidden');
  if (userBar) userBar.classList.remove('hidden');

  const isStudyingNow = timerStatus === 'RUNNING' && currentMode !== 'break';
  const subName = appState.selectedSubject?.name || 'Focus';
  const profile = appState.userProfile || {};
  const currentName = profile.displayName || appState.currentUser.user_metadata?.full_name || 'You';
  const avatar = profile.avatarPreset || appState.currentUser.user_metadata?.avatar_url || '';
  const currentRing = profile.avatarRing || 'glow-gold';

  if (userBarName) {
    userBarName.textContent = currentName;
  }

  if (userBarAvatarWrap) {
    userBarAvatarWrap.innerHTML = getAvatarElementHtml(avatar, currentName, 'user-bar-avatar', currentRing);
  }

  if (userBarStatus) {
    userBarStatus.innerHTML = isStudyingNow 
      ? `<span class="user-bar-status-studying"><span class="status-dot"></span>Studying ${subName}</span>` 
      : `<span class="user-bar-status-resting"><span class="status-dot"></span>Resting</span>`;
  }

  if (myEntry) {
    if (userBarRank) userBarRank.textContent = `#${myEntry.rank}`;
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(myEntry.total_seconds);
  } else {
    if (userBarRank) userBarRank.textContent = '#--';
    if (userBarTime) userBarTime.textContent = formatLeaderboardTime(localTotalSec);
  }
}

function updateLeaderboardCountdownDisplay() {
  const countdownEl = document.getElementById('leaderboardResetCountdown');
  if (!countdownEl) return;

  const now = new Date();
  const period = currentLeaderboardPeriod || 'daily';

  if (period === 'daily') {
    const midnight = new Date(now);
    midnight.setHours(24, 0, 0, 0);
    const diffMs = midnight - now;
    if (diffMs <= 0) {
      countdownEl.textContent = 'Resets at 00:00';
    } else {
      const hours = Math.floor(diffMs / 3600000);
      const mins = Math.floor((diffMs % 3600000) / 60000);
      countdownEl.textContent = `Resets in ${hours}h ${mins}m`;
    }
  } else if (period === 'weekly') {
    // Next Sunday 23:59:59
    const day = now.getDay();
    const daysUntilSunday = (7 - day) % 7;
    const sundayNight = new Date(now);
    sundayNight.setDate(now.getDate() + daysUntilSunday);
    sundayNight.setHours(23, 59, 59, 999);
    const diffMs = sundayNight - now;
    const days = Math.floor(diffMs / (24 * 3600000));
    const hours = Math.floor((diffMs % (24 * 3600000)) / 3600000);
    countdownEl.textContent = `Resets in ${days}d ${hours}h`;
  } else if (period === 'monthly') {
    // End of current month
    const endOfMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
    const diffMs = endOfMonth - now;
    const days = Math.floor(diffMs / (24 * 3600000));
    const hours = Math.floor((diffMs % (24 * 3600000)) / 3600000);
    countdownEl.textContent = `Resets in ${days}d ${hours}h`;
  }
}

function startResetCountdownTimer() {
  if (resetCountdownInterval) clearInterval(resetCountdownInterval);
  updateLeaderboardCountdownDisplay();
  resetCountdownInterval = setInterval(updateLeaderboardCountdownDisplay, 60000);
}

// Window Unload Presence Cleanup
window.addEventListener('beforeunload', () => {
  if (timerStatus === 'RUNNING') {
    stopPresenceHeartbeat();
  }
});

window.addEventListener('pagehide', () => {
  if (timerStatus === 'RUNNING') {
    stopPresenceHeartbeat();
  }
});

// ============================================================================
