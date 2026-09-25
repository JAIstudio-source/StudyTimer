// ============================================================================
// STUDYTIMER TWO-WAY CLOUD SYNC & CONFLICT RESOLUTION ENGINE
// ============================================================================
// 4. TWO-WAY CLOUD SYNC & CONFLICT RESOLUTION ENGINE (100% Android App Compatible)
// ============================================================================

let isLocalStateDirty = false;
let localLastModifiedTimestamp = Date.now();
let lastCloudPushTime = 0;
let cloudPushDebounceTimer = null;
let autoSyncIntervalTimer = null;
let lastAutoSyncAttempt = 0;

function markLocalDataModified() {
  isLocalStateDirty = true;
  localLastModifiedTimestamp = Date.now();
}

function sanitizeString(str, maxLen = 100) {
  if (typeof str !== 'string') return '';
  return str
    .replace(/[<>]/g, '')
    .trim()
    .slice(0, maxLen);
}

function sanitizeUrl(url) {
  if (typeof url !== 'string') return '';
  const trimmed = url.trim();
  if (/^(https?:\/\/|assets\/|\/|blob:)/i.test(trimmed)) {
    return trimmed.slice(0, 2048);
  }
  if (/^data:image\/(png|jpeg|jpg|webp|gif);base64,/i.test(trimmed)) {
    return trimmed; // Full Base64 payload preserved
  }
  return '';
}

function sanitizeAvatar(avatar) {
  if (!avatar || typeof avatar !== 'string') return '';
  const trimmed = avatar.trim();
  if (!trimmed) return '';
  if (/^(https?:\/\/|assets\/|\/|blob:|data:image\/)/i.test(trimmed)) {
    return sanitizeUrl(trimmed) || '';
  }
  return sanitizeString(trimmed, 30) || '';
}

function parseSafeStringSet(val) {
  const set = new Set();
  if (!val) return set;
  if (Array.isArray(val)) {
    val.forEach(item => { if (item) set.add(String(item).trim()); });
    return set;
  }
  if (typeof val === 'object' && val !== null) {
    Object.keys(val).forEach(k => set.add(k));
    return set;
  }
  if (typeof val === 'string') {
    const trimmed = val.trim();
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          parsed.forEach(item => { if (item) set.add(String(item).trim()); });
          return set;
        }
      } catch (_) {}
    }
    const cleaned = trimmed.replace(/^\[|\]$/g, '');
    cleaned.split(',').map(s => s.trim().replace(/^["']|["']$/g, '')).filter(Boolean).forEach(s => set.add(s));
  }
  return set;
}

// Intelligent Two-Way Merge Algorithm (Prevents data loss between Web Studio & Android App)
function mergeCloudDataIntoLocal(data) {
  if (!data) return { mergedSubjects: 0, mergedGoals: 0, mergedTimeline: 0 };
  const cloudUpdatedAt = Number(data.updated_at) || 0;

  let cloudPrefs = {};
  if (data.prefs_data) {
    try {
      cloudPrefs = typeof data.prefs_data === 'string' ? JSON.parse(data.prefs_data) : data.prefs_data;
    } catch (e) {
      console.warn('Failed to parse cloud prefs_data:', e);
    }
  }

  // 1. TIMELINE ENTRIES MERGE (De-duplicate by unique ID / session timestamp)
  let cloudTimeline = [];
  if (data.timeline_data) {
    try {
      const parsed = typeof data.timeline_data === 'string' ? JSON.parse(data.timeline_data) : data.timeline_data;
      if (Array.isArray(parsed)) cloudTimeline = parsed;
    } catch (e) {
      console.warn('Failed to parse cloud timeline_data:', e);
    }
  }

  const localTimeline = Array.isArray(appState.timelineEntries) ? appState.timelineEntries : [];
  const mergedTimelineMap = new Map();

  localTimeline.forEach(e => {
    if (e && typeof e.t === 'number') {
      const key = e.id || `t_${e.t}_${e.s || ''}`;
      mergedTimelineMap.set(key, e);
    }
  });

  cloudTimeline.forEach(e => {
    if (e && typeof e.t === 'number') {
      const key = e.id || `t_${e.t}_${e.s || ''}`;
      if (!mergedTimelineMap.has(key)) {
        let foundNearby = false;
        for (const [existingKey, existingEntry] of mergedTimelineMap.entries()) {
          if (Math.abs(existingEntry.t - e.t) < 1500 && existingEntry.s === e.s) {
            foundNearby = true;
            if ((e.durationSec || 0) > (existingEntry.durationSec || 0)) {
              mergedTimelineMap.set(existingKey, { ...existingEntry, ...e });
            }
            break;
          }
        }
        if (!foundNearby) {
          mergedTimelineMap.set(key, e);
        }
      } else {
        const existing = mergedTimelineMap.get(key);
        if ((e.durationSec || 0) > (existing.durationSec || 0)) {
          mergedTimelineMap.set(key, { ...existing, ...e });
        }
      }
    }
  });

  const nowFutureThreshold = Date.now() + 60000;
  appState.timelineEntries = Array.from(mergedTimelineMap.values())
    .filter(e => e && typeof e.t === 'number' && e.t <= nowFutureThreshold)
    .sort((a, b) => a.t - b.t)
    .slice(-500);

  // 2. DAILY FOCUS TOTALS MERGE (Safe max retention for every calendar date)
  const mergedDailyFocus = { ...(appState.dailyFocusTotals || {}) };
  Object.keys(cloudPrefs).forEach(k => {
    const match = k.match(/^(\d{4}-\d{2}-\d{2})_focus_total$/);
    if (match) {
      const dStr = match[1];
      const cloudSec = Math.min(86400, Math.max(0, Number(cloudPrefs[k]) || 0));
      mergedDailyFocus[dStr] = Math.max(mergedDailyFocus[dStr] || 0, cloudSec);
    }
  });

  const parsedSessions = parseAllTimelineSessions(appState.timelineEntries);
  parsedSessions.forEach(s => {
    const dStr = getLocalDateStr(s.timestamp);
    mergedDailyFocus[dStr] = Math.min(86400, Math.max(mergedDailyFocus[dStr] || 0, s.durationSec));
  });
  appState.dailyFocusTotals = mergedDailyFocus;

  // 3. SUBJECT DURATIONS MERGE
  const mergedSubDur = { ...(appState.subjectDurations || {}) };
  let cloudSubDur = {};
  if (cloudPrefs.subject_durations_json) {
    try {
      cloudSubDur = typeof cloudPrefs.subject_durations_json === 'string'
        ? JSON.parse(cloudPrefs.subject_durations_json)
        : cloudPrefs.subject_durations_json;
    } catch (_) {}
  }
  Object.keys(cloudSubDur || {}).forEach(subId => {
    mergedSubDur[subId] = Math.max(mergedSubDur[subId] || 0, Number(cloudSubDur[subId]) || 0);
  });
  appState.subjectDurations = mergedSubDur;

  // 4. DAILY SUBJECT DURATIONS BREAKDOWN MERGE
  const mergedDailySub = { ...(appState.dailySubjectDurations || {}) };
  let cloudDailySub = {};
  if (cloudPrefs.daily_subject_durations_json) {
    try {
      cloudDailySub = typeof cloudPrefs.daily_subject_durations_json === 'string'
        ? JSON.parse(cloudPrefs.daily_subject_durations_json)
        : cloudPrefs.daily_subject_durations_json;
    } catch (_) {}
  }
  Object.keys(cloudDailySub || {}).forEach(dStr => {
    if (!mergedDailySub[dStr]) mergedDailySub[dStr] = {};
    const subObj = cloudDailySub[dStr];
    if (subObj && typeof subObj === 'object') {
      Object.keys(subObj).forEach(subId => {
        mergedDailySub[dStr][subId] = Math.max(mergedDailySub[dStr][subId] || 0, Number(subObj[subId]) || 0);
      });
    }
  });
  appState.dailySubjectDurations = mergedDailySub;

  // 5. STREAK MERGE
  const cloudStreak = Math.max(0, Number(cloudPrefs.current_streak || cloudPrefs.streak_count) || 0);
  appState.streakCount = Math.max(appState.streakCount || 0, cloudStreak);
  if (cloudPrefs.last_study_date && (!appState.lastStudyDate || cloudPrefs.last_study_date > appState.lastStudyDate)) {
    appState.lastStudyDate = sanitizeString(cloudPrefs.last_study_date, 20);
  }

  // 6. CUSTOM SUBJECTS MERGE
  const rawTags = cloudPrefs.__subject_tags_data__ || data.subject_tags_data || cloudPrefs.custom_subjects_json;
  let subjectPrefs = null;
  if (rawTags) {
    try {
      subjectPrefs = typeof rawTags === 'string' ? JSON.parse(rawTags) : rawTags;
    } catch (_) {}
  }
  let cloudCustomList = [];
  let cloudHiddenSet = new Set();
  let selectedSubId = cloudPrefs.selected_subject_id || 'general';

  if (subjectPrefs) {
    if (subjectPrefs.custom_subjects_json) {
      try {
        cloudCustomList = typeof subjectPrefs.custom_subjects_json === 'string'
          ? JSON.parse(subjectPrefs.custom_subjects_json)
          : subjectPrefs.custom_subjects_json;
      } catch (_) {}
    } else if (subjectPrefs.custom_subjects) {
      try {
        cloudCustomList = typeof subjectPrefs.custom_subjects === 'string'
          ? JSON.parse(subjectPrefs.custom_subjects)
          : subjectPrefs.custom_subjects;
      } catch (_) {}
    } else if (Array.isArray(subjectPrefs)) {
      cloudCustomList = subjectPrefs;
    }
    if (subjectPrefs.hidden_subjects_set) {
      cloudHiddenSet = parseSafeStringSet(subjectPrefs.hidden_subjects_set);
    }
    if (subjectPrefs.selected_subject_id) {
      selectedSubId = subjectPrefs.selected_subject_id;
    }
  }

  const existingSubMap = new Map();
  DEFAULT_SUBJECTS.forEach(d => {
    if (!cloudHiddenSet.has(d.id)) existingSubMap.set(d.id, { ...d });
  });
  (appState.subjects || []).forEach(s => {
    if (s && s.id && !cloudHiddenSet.has(s.id)) existingSubMap.set(s.id, { ...s });
  });
  if (Array.isArray(cloudCustomList)) {
    cloudCustomList.forEach(c => {
      if (c && c.id && !cloudHiddenSet.has(c.id)) {
        existingSubMap.set(c.id, {
          id: sanitizeString(c.id, 30),
          name: sanitizeString(c.name || 'Subject', 40),
          color: c.colorHex || c.color || '#3b82f6',
          colorHex: c.colorHex || c.color || '#3b82f6',
          iconEmoji: sanitizeString(c.iconEmoji || '📚', 10),
          isCustom: true
        });
      }
    });
  }
  appState.subjects = getCleanUniqueSubjects(Array.from(existingSubMap.values())).slice(0, 50);
  const foundSel = appState.subjects.find(s => s.id === selectedSubId);
  appState.selectedSubject = foundSel || appState.subjects[0];

  // 7. PLANNER GOALS MERGE (Union by unique ID, latest checked/updated status)
  let cloudGoals = [];
  if (cloudPrefs.session_goals_json) {
    try {
      cloudGoals = typeof cloudPrefs.session_goals_json === 'string'
        ? JSON.parse(cloudPrefs.session_goals_json)
        : cloudPrefs.session_goals_json;
    } catch (_) {}
  } else if (cloudPrefs.__planner_goals_data__) {
    try {
      cloudGoals = typeof cloudPrefs.__planner_goals_data__ === 'string'
        ? JSON.parse(cloudPrefs.__planner_goals_data__)
        : cloudPrefs.__planner_goals_data__;
    } catch (_) {}
  }

  const goalMap = new Map();
  (appState.plannerGoals || []).forEach(g => {
    if (g && g.id) goalMap.set(g.id, g);
  });
  if (Array.isArray(cloudGoals)) {
    cloudGoals.forEach(g => {
      if (g && g.id) {
        if (!goalMap.has(g.id)) {
          goalMap.set(g.id, {
            id: sanitizeString(g.id, 50),
            subjectId: g.subjectId ? sanitizeString(g.subjectId, 30) : null,
            dailyMinutes: Math.min(1440, Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0)),
            targetMinutes: Math.min(1440, Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0)),
            title: sanitizeString(g.title || '', 60),
            note: sanitizeString(g.note || '', 200),
            completed: !!g.completed,
            checkedAt: g.checkedAt || (g.completed ? Date.now() : null),
            createdAt: g.createdAt || Date.now()
          });
        } else {
          const existing = goalMap.get(g.id);
          const isCompleted = existing.completed || !!g.completed;
          const checkedAt = Math.max(existing.checkedAt || 0, g.checkedAt || 0) || (isCompleted ? Date.now() : null);
          goalMap.set(g.id, {
            ...existing,
            ...g,
            completed: isCompleted,
            checkedAt: checkedAt,
            title: sanitizeString(g.title || existing.title || '', 60),
            note: sanitizeString(g.note || existing.note || '', 200)
          });
        }
      }
    });
  }
  appState.plannerGoals = Array.from(goalMap.values()).slice(0, 50);

  // 8. TIMER SETTINGS (Cloud sync interop)
  const todayKey = getLocalDateStr();
  const todayGoalSecs = Number(cloudPrefs[`${todayKey}_goal_secs`]) || Number(cloudPrefs.daily_goal_secs) || 0;
  const goalMins = cloudPrefs.daily_goal_minutes || (todayGoalSecs > 0 ? Math.round(todayGoalSecs / 60) : null);
  if (goalMins) timerConfig.dailyGoalMinutes = Math.min(1440, Math.max(15, goalMins));

  if (cloudPrefs.study_interval_minutes || cloudPrefs.pomo_focus_minutes) {
    timerConfig.pomoFocusMinutes = Math.min(180, Math.max(1, Number(cloudPrefs.study_interval_minutes || cloudPrefs.pomo_focus_minutes)));
  }
  if (cloudPrefs.break_interval_minutes || cloudPrefs.pomo_break_minutes) {
    timerConfig.pomoBreakMinutes = Math.min(60, Math.max(1, Number(cloudPrefs.break_interval_minutes || cloudPrefs.pomo_break_minutes)));
  }
  if (cloudPrefs.pomo_long_break_minutes) timerConfig.pomoLongBreakMinutes = Math.min(120, Math.max(1, Number(cloudPrefs.pomo_long_break_minutes)));
  if (cloudPrefs.pomo_total_cycles) timerConfig.pomoTotalCycles = Math.min(12, Math.max(1, Number(cloudPrefs.pomo_total_cycles)));
  if (typeof cloudPrefs.pomo_auto_switch_break === 'boolean') timerConfig.pomoAutoSwitchBreak = cloudPrefs.pomo_auto_switch_break;
  if (typeof cloudPrefs.pomo_auto_switch_focus === 'boolean') timerConfig.pomoAutoSwitchFocus = cloudPrefs.pomo_auto_switch_focus;
  if (cloudPrefs.custom_timer_minutes) {
    timerConfig.customTimerMinutes = Math.min(720, Math.max(1, Number(cloudPrefs.custom_timer_minutes)));
  }

  // 9. USER PROFILE & MODERATION STATUS
  const serverProfileStatus = data.profile_status || 'approved';
  const remoteVerifiedName = data.user_name || cloudPrefs.auth_user_name || '';

  if (cloudPrefs.__user_profile__) {
    try {
      const loadedProfile = typeof cloudPrefs.__user_profile__ === 'string'
        ? JSON.parse(cloudPrefs.__user_profile__)
        : cloudPrefs.__user_profile__;
      if (loadedProfile && typeof loadedProfile === 'object') {
        const localPendingAvatar = (appState.userProfile?.profileStatus === 'pending' && appState.userProfile?.avatarPreset)
          ? appState.userProfile.avatarPreset
          : null;

        const customUrl = (data.profile_image_uri && /^(https?:\/\/|data:|blob:)/i.test(data.profile_image_uri))
          ? data.profile_image_uri
          : (loadedProfile.avatarUrl && /^(https?:\/\/|data:|blob:)/i.test(loadedProfile.avatarUrl)
              ? loadedProfile.avatarUrl
              : (loadedProfile.avatarPreset && /^(https?:\/\/|data:|blob:)/i.test(loadedProfile.avatarPreset) ? loadedProfile.avatarPreset : ''));

        const rawAvatarCandidate = (serverProfileStatus === 'pending' && localPendingAvatar)
          ? localPendingAvatar
          : (customUrl || (loadedProfile.avatarPreset && loadedProfile.avatarPreset !== 'avatar_default' ? loadedProfile.avatarPreset : '') || loadedProfile.avatarUrl || data.profile_image_uri || appState.userProfile?.avatarPreset || '');

        const resolvedAvatar = sanitizeAvatar(rawAvatarCandidate);

        appState.userProfile = {
          ...appState.userProfile,
          ...loadedProfile,
          displayName: sanitizeString(loadedProfile.displayName || appState.userProfile?.displayName || '', 50),
          avatarPreset: resolvedAvatar,
          avatarRing: loadedProfile.avatarRing || appState.userProfile?.avatarRing || 'glow-gold',
          bannerTheme: loadedProfile.bannerTheme || appState.userProfile?.bannerTheme || 'banner-midnight',
          countryFlag: loadedProfile.countryFlag || appState.userProfile?.countryFlag || '🌐',
          mood: loadedProfile.mood || appState.userProfile?.mood || '',
          examTarget: loadedProfile.examTarget || appState.userProfile?.examTarget || '',
          motto: loadedProfile.motto || appState.userProfile?.motto || '',
          primarySubjectId: loadedProfile.primarySubjectId || appState.userProfile?.primarySubjectId || 'math',
          isStealth: Boolean(loadedProfile.isStealth),
          isPublicLeaderboard: loadedProfile.isPublicLeaderboard !== false,
          profileStatus: serverProfileStatus
        };
      }
    } catch (_) {}
  } else {
    if (remoteVerifiedName && (!appState.userProfile?.displayName || appState.userProfile.displayName === 'Student')) {
      appState.userProfile.displayName = sanitizeString(remoteVerifiedName, 50);
    }
    if (data.profile_image_uri && (!appState.userProfile?.avatarPreset || appState.userProfile.avatarPreset === '')) {
      if (!appState.userProfile) appState.userProfile = {};
      appState.userProfile.avatarPreset = sanitizeAvatar(data.profile_image_uri);
    }
  }

  // Enforce server moderation decision on local state
  if (serverProfileStatus === 'rejected') {
    const fallbackName = appState.approvedDisplayName || remoteVerifiedName || appState.currentUser?.user_metadata?.full_name || appState.currentUser?.email?.split('@')[0] || 'Scholar';
    if (appState.userProfile) {
      appState.userProfile.displayName = sanitizeString(fallbackName, 50);
      appState.userProfile.mood = '';
      appState.userProfile.examTarget = '';
      appState.userProfile.profileStatus = 'rejected';
    }
    appState.approvedDisplayName = sanitizeString(fallbackName, 50);
  } else if (serverProfileStatus === 'approved') {
    if (appState.userProfile) {
      appState.userProfile.profileStatus = 'approved';
      const effectiveName = appState.userProfile.displayName || remoteVerifiedName || appState.currentUser?.user_metadata?.full_name || 'Student';
      appState.userProfile.displayName = sanitizeString(effectiveName, 50);
      appState.approvedDisplayName = sanitizeString(effectiveName, 50);
    }
  } else if (serverProfileStatus === 'pending') {
    if (appState.userProfile) {
      appState.userProfile.profileStatus = 'pending';
    }
    if (remoteVerifiedName) {
      appState.approvedDisplayName = sanitizeString(remoteVerifiedName, 50);
    }
  }

  reconstructTodaySessionsFromTimeline();

  return {
    mergedSubjects: appState.subjects.length,
    mergedGoals: appState.plannerGoals.length,
    mergedTimeline: appState.timelineEntries.length
  };
}

async function pullDataFromCloud(isUserTriggered = false) {
  if (!supabaseClient || !appState.currentUser) {
    if (isUserTriggered) showToast('Please sign in with Google to sync with mobile app', 'info');
    return;
  }

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const user = appState.currentUser;
    const userId = user.id;
    const userEmail = (user.email || user.user_metadata?.email || '').trim().toLowerCase();

    let query = supabaseClient.from('user_sync_data').select('*');
    if (userEmail && userEmail !== userId) {
      query = query.or(`user_id.eq.${userId},user_id.eq.${userEmail},user_email.eq.${userEmail}`);
    } else {
      query = query.eq('user_id', userId);
    }

    const { data: rows, error } = await query.order('updated_at', { ascending: false });

    if (error) {
      console.warn('Error fetching cloud record:', error);
    }

    let data = null;
    if (Array.isArray(rows) && rows.length > 0) {
      data = rows.find(r => {
        const hasTimeline = r.timeline_data && r.timeline_data !== '[]' && r.timeline_data.length > 10;
        const hasPrefs = r.prefs_data && r.prefs_data.length > 10;
        return hasTimeline || hasPrefs;
      }) || rows[0];
    }

    if (data) {
      const cloudUpdatedAt = Number(data.updated_at) || 0;
      if (cloudUpdatedAt > 0) {
        lastPulledCloudUpdatedAt = Math.max(lastPulledCloudUpdatedAt, cloudUpdatedAt);
      }

      // Execute non-destructive intelligent merge
      const mergeStats = mergeCloudDataIntoLocal(data);

      renderSubjects();
      renderUserProfileUI();
      updateProgressAndStreak();
      renderSubjectDonutChart();
      renderActivityHeatmap();
      renderMonthlyCalendar();
      renderPlannerGoals();
      switchInsightsTab(currentInsightsTab);
      saveLocalState();
      if (timerStatus === 'IDLE') {
        resetTimer();
      }

      // Sync active presence and score to leaderboard
      await syncStudyProgressToLeaderboard(0);

      // Refresh leaderboard UI if open
      const lbModal = document.getElementById('leaderboardModalOverlay');
      if (lbModal && !lbModal.classList.contains('hidden')) {
        fetchLeaderboard(false, false);
      }

      // If user manually clicked "Sync with App", push the consolidated merged state back to cloud
      if (isUserTriggered) {
        pushDataToCloud(true, true);
        showToast(`Sync complete! Merged ${mergeStats.mergedSubjects} subjects & ${mergeStats.mergedGoals} goals. ☁️`, 'success');
      }
    } else {
      if (isUserTriggered) {
        pushDataToCloud(true, true);
        showToast('First-time backup created for your account! ☁️', 'success');
      }
    }
  } catch (err) {
    console.error('Cloud pull exception:', err);
    if (isUserTriggered) showToast('Sync failed: Network error', 'error');
  } finally {
    if (syncStatusPill) {
      syncStatusPill.classList.remove('syncing');
      syncStatusPill.classList.add('synced');
      syncStatusText.textContent = 'Synced';
    }
  }
}

async function pushDataToCloud(silent = false, force = false) {
  saveLocalState();
  if (!supabaseClient || !appState.currentUser) return;

  const now = Date.now();
  // Rate Limiting & Cooldown Protection (Minimum 3 seconds between cloud API calls unless forced)
  if (!force && (now - lastCloudPushTime < 3000)) {
    if (cloudPushDebounceTimer) clearTimeout(cloudPushDebounceTimer);
    cloudPushDebounceTimer = setTimeout(() => {
      pushDataToCloud(silent, force);
    }, 3000);
    return;
  }

  // Server Concurrency Optimization: Skip heavy network writes if nothing has changed
  if (!force && !isLocalStateDirty && (now - lastCloudPushTime < 180000)) {
    return;
  }

  lastCloudPushTime = now;

  const syncStatusPill = document.getElementById('syncStatusPill');
  const syncStatusText = document.getElementById('syncStatusText');
  if (syncStatusPill) {
    syncStatusPill.classList.add('syncing');
    syncStatusText.textContent = 'Syncing...';
  }

  try {
    const user = appState.currentUser;
    const defaultAuthName = user.user_metadata?.full_name || user.user_metadata?.name || user.email?.split('@')[0] || 'Student';
    const profileStatus = appState.userProfile?.profileStatus || 'approved';
    const isPendingOrRejected = profileStatus === 'pending' || profileStatus === 'rejected';

    // Moderation Gate: Only write approved display name as public user_name in user_sync_data
    const userName = isPendingOrRejected
      ? sanitizeString(appState.approvedDisplayName || defaultAuthName, 50)
      : sanitizeString(appState.userProfile?.displayName || defaultAuthName, 50);

    const userEmail = sanitizeString(user.email || user.user_metadata?.email || '', 100);
    const profileImg = sanitizeAvatar(user.user_metadata?.avatar_url || appState.userProfile?.avatarPreset || '');

    // Payload sanitization & safety caps
    const sanitizedSubjects = (Array.isArray(appState.subjects) ? appState.subjects : []).slice(0, 50);
    const sanitizedTimeline = (Array.isArray(appState.timelineEntries) ? appState.timelineEntries : []).slice(-500);
    const sanitizedGoals = (Array.isArray(appState.plannerGoals) ? appState.plannerGoals : []).slice(0, 50);

    const defaultIds = DEFAULT_SUBJECTS.map(d => d.id);
    const currentSubjectIds = new Set(sanitizedSubjects.map(s => s.id));
    const hiddenDefaultIds = defaultIds.filter(id => !currentSubjectIds.has(id));

    const customSubjectsForAndroid = sanitizedSubjects
      .filter(s => !defaultIds.includes(s.id))
      .map(s => ({
        id: sanitizeString(s.id, 30),
        name: sanitizeString(s.name, 40),
        iconEmoji: sanitizeString(s.iconEmoji || '📚', 10),
        colorHex: s.color || s.colorHex || '#3b82f6',
        isCustom: true
      }));

    const allSubjectsForWeb = sanitizedSubjects.map(s => ({
      id: sanitizeString(s.id, 30),
      name: sanitizeString(s.name, 40),
      color: s.color || s.colorHex || '#3b82f6',
      colorHex: s.colorHex || s.color || '#3b82f6',
      iconEmoji: sanitizeString(s.iconEmoji || '📚', 10)
    }));

    const subjectTagsObj = {
      custom_subjects_json: JSON.stringify(customSubjectsForAndroid),
      custom_subjects: JSON.stringify(allSubjectsForWeb),
      selected_subject_id: sanitizeString(appState.selectedSubject?.id || 'general', 30),
      hidden_subjects_set: hiddenDefaultIds
    };

    const androidPlannerGoals = sanitizedGoals.map(g => {
      const matchingSub = sanitizedSubjects.find(s => s.id === g.subjectId);
      return {
        id: sanitizeString(g.id || ('goal_' + Date.now()), 50),
        title: sanitizeString(g.title || (matchingSub ? `${matchingSub.name} Goal` : 'Daily Study Goal'), 60),
        note: sanitizeString(g.note || '', 200),
        targetMinutes: Math.min(1440, Math.max(0, Number(g.targetMinutes ?? g.dailyMinutes) || 0)),
        subjectId: (g.subjectId && g.subjectId !== 'all') ? sanitizeString(g.subjectId, 30) : null,
        completed: !!g.completed,
        checkedAt: g.checkedAt || (g.completed ? Date.now() : 0),
        createdAt: g.createdAt || Date.now()
      };
    });

    const dailyGoalMin = Math.min(1440, Math.max(1, Number(timerConfig.dailyGoalMinutes) || 120));
    const todayKey = getLocalDateStr();
    let totalSecToday = 0;
    (appState.todaySessions || []).forEach(s => {
      if (s && typeof s.durationSec === 'number') totalSecToday += s.durationSec;
    });
    totalSecToday = Math.min(86400, totalSecToday);

    const prefsObj = {
      daily_goal_minutes: dailyGoalMin,
      daily_goal_secs: dailyGoalMin * 60,
      study_interval_minutes: Math.min(180, Math.max(1, Number(timerConfig.pomoFocusMinutes) || 25)),
      break_interval_minutes: Math.min(60, Math.max(1, Number(timerConfig.pomoBreakMinutes) || 5)),
      custom_timer_minutes: Math.min(720, Math.max(1, Number(timerConfig.customTimerMinutes) || 45)),
      pomo_focus_minutes: Math.min(180, Math.max(1, Number(timerConfig.pomoFocusMinutes) || 25)),
      pomo_break_minutes: Math.min(60, Math.max(1, Number(timerConfig.pomoBreakMinutes) || 5)),
      pomo_long_break_minutes: Math.min(120, Math.max(1, Number(timerConfig.pomoLongBreakMinutes) || 15)),
      pomo_total_cycles: Math.min(12, Math.max(1, Number(timerConfig.pomoTotalCycles) || 4)),
      pomo_auto_switch_break: timerConfig.pomoAutoSwitchBreak !== false,
      pomo_auto_switch_focus: timerConfig.pomoAutoSwitchFocus !== false,
      current_streak: Math.max(0, Number(appState.streakCount) || 0),
      streak_count: Math.max(0, Number(appState.streakCount) || 0),
      last_study_date: sanitizeString(appState.lastStudyDate || '', 20),
      accumulatedStudy: totalSecToday,
      [`${todayKey}_focus_total`]: totalSecToday,
      session_goals_json: JSON.stringify(androidPlannerGoals),
      __subject_tags_data__: JSON.stringify(subjectTagsObj),
      custom_subjects_json: JSON.stringify(customSubjectsForAndroid),
      __planner_goals_data__: JSON.stringify(androidPlannerGoals),
      __user_profile__: JSON.stringify(appState.userProfile)
    };

    if (appState.dailyFocusTotals) {
      Object.keys(appState.dailyFocusTotals).forEach(d => {
        if (d !== todayKey && appState.dailyFocusTotals[d]) {
          prefsObj[`${d}_focus_total`] = Math.min(86400, Number(appState.dailyFocusTotals[d]) || 0);
        }
      });
    }

    if (appState.subjectDurations) {
      prefsObj.subject_durations_json = JSON.stringify(appState.subjectDurations);
    }
    if (appState.dailySubjectDurations) {
      prefsObj.daily_subject_durations_json = JSON.stringify(appState.dailySubjectDurations);
    }

    const nowMs = Date.now();
    lastCloudPushTime = nowMs;
    lastPulledCloudUpdatedAt = nowMs;
    const payload = {
      user_id: user.id,
      user_name: userName,
      user_email: userEmail,
      profile_image_uri: profileImg,
      profile_status: appState.userProfile?.profileStatus || 'approved',
      pending_profile_json: appState.userProfile?.profileStatus === 'pending' ? JSON.stringify(appState.userProfile) : null,
      prefs_data: JSON.stringify(prefsObj),
      timeline_data: JSON.stringify(sanitizedTimeline),
      updated_at: nowMs
    };

    const { error } = await supabaseClient
      .from('user_sync_data')
      .upsert(payload, { onConflict: 'user_id' });

    if (error) {
      console.warn('Cloud sync push warning:', error);
      if (!silent) showToast('Cloud sync failed to update.', 'error');
    } else {
      isLocalStateDirty = false;
      if (!silent) showToast('Session synced with Android app!', 'success');
    }
  } catch (err) {
    console.warn('pushDataToCloud exception:', err);
  } finally {
    if (syncStatusPill) {
      syncStatusPill.classList.remove('syncing');
      syncStatusPill.classList.add('synced');
      syncStatusText.textContent = 'Synced';
    }
  }
}

// ----------------------------------------------------------------------------
// Automatic Background Sync Engine (2.5 - 3.5 Minute Jittered Periodic Sync)
// ----------------------------------------------------------------------------
function initAutoSyncEngine() {
  if (autoSyncIntervalTimer) {
    clearTimeout(autoSyncIntervalTimer);
    autoSyncIntervalTimer = null;
  }

  function scheduleNextAutoSync() {
    // Randomized jitter (+/- 25 seconds) to prevent 100+ concurrent active users hitting server in lockstep
    const jitterMs = Math.floor((Math.random() - 0.5) * 50000);
    const nextInterval = Math.max(120000, 180000 + jitterMs); // ~2.5 to 3.5 minutes

    autoSyncIntervalTimer = setTimeout(async () => {
      if (appState.currentUser && supabaseClient && navigator.onLine) {
        try {
          if (isLocalStateDirty) {
            await pushDataToCloud(true);
          } else {
            await pullDataFromCloud(false);
          }

          if (timerStatus === 'RUNNING' && currentMode !== 'break') {
            syncStudyProgressToLeaderboard(0);
          }

          const lbModal = document.getElementById('leaderboardModalOverlay');
          if (lbModal && !lbModal.classList.contains('hidden')) {
            fetchLeaderboard(false, false);
          }
        } catch (e) {
          console.warn('Periodic auto-sync exception:', e);
        }
      }
      scheduleNextAutoSync();
    }, nextInterval);
  }

  scheduleNextAutoSync();
}

// Universal timeline session parser (Handles Android TimelineLogger & Web formats)
function parseAllTimelineSessions(rawEntries) {
  if (!Array.isArray(rawEntries) || rawEntries.length === 0) return [];

  // Filter valid entries & sort ascending by timestamp
  const entries = rawEntries
    .filter(e => e && typeof e.t === 'number')
    .sort((a, b) => a.t - b.t);

  const studyStates = new Set(['STUDYING', 'POMODORO', 'COUNT_UP', 'COUNTDOWN', 'MANUAL_FOCUS', 'FOCUS']);
  const nonStudyStates = new Set(['IDLE', 'PAUSED', 'STOPPED', 'BREAK', 'MANUAL_BREAK']);

  const parsedSessions = [];
  let openSession = null;

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];

    if (studyStates.has(entry.s)) {
      if (openSession) {
        const gapSec = Math.round((entry.t - openSession.t) / 1000);
        if (gapSec > 0 && gapSec <= 86400) {
          parsedSessions.push(buildSessionFromEntry(openSession, entry.t, gapSec));
        }
      }
      if (typeof entry.durationSec === 'number' && entry.durationSec > 0) {
        parsedSessions.push(buildSessionFromEntry(entry, entry.endT || (entry.t + entry.durationSec * 1000), entry.durationSec));
        openSession = null;
      } else {
        openSession = entry;
      }
    } else if (nonStudyStates.has(entry.s)) {
      if (openSession) {
        const gapSec = Math.round((entry.t - openSession.t) / 1000);
        if (gapSec > 0 && gapSec <= 86400) {
          parsedSessions.push(buildSessionFromEntry(openSession, entry.t, gapSec));
        }
        openSession = null;
      }
    }
  }

  // Handle open session if it had explicit duration
  if (openSession && typeof openSession.durationSec === 'number' && openSession.durationSec > 0) {
    parsedSessions.push(buildSessionFromEntry(openSession, openSession.endT || (openSession.t + openSession.durationSec * 1000), openSession.durationSec));
  }

  return parsedSessions;
}

function buildSessionFromEntry(entry, endMs, durationSec) {
  let modeLabel = 'Focus Study';
  if (entry.s === 'POMODORO' || entry.s === 'COUNTDOWN') modeLabel = 'Pomodoro';
  else if (entry.s === 'COUNT_UP') modeLabel = 'Stopwatch';
  else if (entry.s === 'MANUAL_FOCUS') modeLabel = 'Manual Focus';

  const subId = entry.subId || 'general';
  const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
    id: subId,
    name: entry.subName || 'General',
    color: entry.subColor || '#6366f1'
  };

  return {
    id: 'sess_' + entry.t,
    subject: {
      id: matchingSub.id,
      name: entry.subName || matchingSub.name,
      color: entry.subColor || matchingSub.color || matchingSub.colorHex || '#6366f1'
    },
    durationSec: Math.max(1, durationSec),
    startTime: entry.t,
    endTime: endMs || (entry.t + durationSec * 1000),
    timestamp: entry.t,
    mode: modeLabel
  };
}

function reconstructTodaySessionsFromTimeline() {
  const startOfDay = new Date();
  startOfDay.setHours(0, 0, 0, 0);
  const startOfDayMs = startOfDay.getTime();
  const endOfDayMs = startOfDayMs + 86400000;
  const todayKey = getLocalDateStr();

  const allParsed = parseAllTimelineSessions(appState.timelineEntries || []);
  let sessions = allParsed
    .filter(s => s.timestamp >= startOfDayMs && s.timestamp < endOfDayMs)
    .sort((a, b) => b.timestamp - a.timestamp);

  // If timeline entries had no sessions for today, but Android recorded today's focus total in prefs
  const todayFocusSec = (appState.dailyFocusTotals && Number(appState.dailyFocusTotals[todayKey])) || 0;
  const currentSum = sessions.reduce((acc, s) => acc + (s.durationSec || 0), 0);

  if (todayFocusSec > currentSum) {
    const missingSec = todayFocusSec - currentSum;
    const dayBreakdown = (appState.dailySubjectDurations && appState.dailySubjectDurations[todayKey]) || null;
    if (dayBreakdown && typeof dayBreakdown === 'object') {
      Object.keys(dayBreakdown).forEach((subId, idx) => {
        const subSec = Number(dayBreakdown[subId]) || 0;
        if (subSec > 0) {
          const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
            id: subId,
            name: subId.charAt(0).toUpperCase() + subId.slice(1),
            color: '#3b82f6'
          };
          sessions.push({
            id: `today_cloud_${todayKey}_${subId}_${idx}`,
            subject: {
              id: matchingSub.id,
              name: matchingSub.name,
              color: matchingSub.color || matchingSub.colorHex || '#3b82f6'
            },
            durationSec: subSec,
            startTime: startOfDayMs + 36000000 + idx * 1000,
            endTime: startOfDayMs + 36000000 + idx * 1000 + subSec * 1000,
            timestamp: startOfDayMs + 36000000 + idx * 1000,
            mode: 'Focus Study'
          });
        }
      });
    } else {
      const defaultSub = appState.selectedSubject || (appState.subjects && appState.subjects[0]) || DEFAULT_SUBJECTS[0];
      sessions.push({
        id: `today_cloud_${todayKey}_${Date.now()}`,
        subject: {
          id: defaultSub.id,
          name: defaultSub.name,
          color: defaultSub.color || defaultSub.colorHex || '#3b82f6'
        },
        durationSec: missingSec,
        startTime: startOfDayMs + 36000000,
        endTime: startOfDayMs + 36000000 + missingSec * 1000,
        timestamp: startOfDayMs + 36000000,
        mode: 'Focus Study'
      });
    }
  }

  appState.todaySessions = sessions;
}

// Universal extractor for all completed sessions with zero duplicate or hardcoded inflations
function getAllValidatedSessions() {
  const allParsed = parseAllTimelineSessions(appState.timelineEntries || []);
  const seenDates = {};
  const sessions = [];

  // 1. Add all parsed timeline sessions
  allParsed.forEach(s => {
    const dStr = getLocalDateStr(s.timestamp);
    seenDates[dStr] = (seenDates[dStr] || 0) + s.durationSec;
    sessions.push(s);
  });

  // 2. Add real-time today sessions if not already in timeline
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      const alreadyHas = sessions.some(existing => Math.abs(existing.timestamp - s.timestamp) < 2000);
      if (!alreadyHas) {
        const dStr = getLocalDateStr(s.timestamp);
        seenDates[dStr] = (seenDates[dStr] || 0) + s.durationSec;
        sessions.push(s);
      }
    }
  });

  // 3. For any date in dailyFocusTotals from Android that doesn't have complete sessions in timeline:
  const dailyTotals = appState.dailyFocusTotals || {};
  Object.keys(dailyTotals).forEach(dateStr => {
    const totalRecordedSec = Number(dailyTotals[dateStr]) || 0;
    const currentParsedSec = seenDates[dateStr] || 0;
    const missingSec = totalRecordedSec - currentParsedSec;

    if (missingSec > 0) {
      const parsedDate = new Date(dateStr + 'T12:00:00Z');
      const ts = !isNaN(parsedDate.getTime()) ? parsedDate.getTime() : Date.now();

      const dayBreakdown = (appState.dailySubjectDurations && appState.dailySubjectDurations[dateStr]) || null;
      if (dayBreakdown && typeof dayBreakdown === 'object') {
        Object.keys(dayBreakdown).forEach((subId, idx) => {
          const subSec = Number(dayBreakdown[subId]) || 0;
          if (subSec > 0) {
            const matchingSub = (appState.subjects || []).find(s => s.id === subId) || {
              id: subId,
              name: subId.charAt(0).toUpperCase() + subId.slice(1),
              color: '#3b82f6'
            };
            sessions.push({
              id: `cloud_day_${dateStr}_${subId}_${idx}`,
              subject: {
                id: matchingSub.id,
                name: matchingSub.name,
                color: matchingSub.color || matchingSub.colorHex || '#3b82f6'
              },
              durationSec: subSec,
              startTime: ts + idx * 1000,
              endTime: ts + idx * 1000 + subSec * 1000,
              timestamp: ts + idx * 1000,
              mode: 'Focus Study'
            });
          }
        });
      } else {
        const defaultSub = appState.selectedSubject || (appState.subjects && appState.subjects[0]) || DEFAULT_SUBJECTS[0];
        sessions.push({
          id: `cloud_day_${dateStr}`,
          subject: {
            id: defaultSub.id,
            name: defaultSub.name,
            color: defaultSub.color || defaultSub.colorHex || '#3b82f6'
          },
          durationSec: missingSec,
          startTime: ts,
          endTime: ts + missingSec * 1000,
          timestamp: ts,
          mode: 'Focus Study'
        });
      }
    }
  });

  return sessions.sort((a, b) => b.timestamp - a.timestamp);
}

// ============================================================================