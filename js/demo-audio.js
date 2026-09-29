// ============================================================================
// STUDYTIMER FOCUS AUDIO & AMBIENCE ENGINE (Procedural WebAudio + YouTube Iframe)
// ============================================================================
// 9. FOCUS AUDIO & AMBIENCE PLAYER (Multi-Tier Resilient Engine)
// ============================================================================
const AUDIO_PRESETS = {
  lofi: { 
    name: 'Focus Lofi Beats', 
    id: '5yx6BWlEVcY'
  },
  minecraft: { 
    name: 'Minecraft Tracks', 
    id: 'vCTRNKPJr40'
  },
  piano: { 
    name: 'Peaceful Study Piano', 
    id: 'FjHGZj2IjBk'
  },
  synthwave: { 
    name: 'Synthwave Chill', 
    id: '4xDzrJKXOOY'
  },
  rain: { 
    name: 'Rain & Gentle Thunder', 
    id: 'mPZkdNFkNps'
  },
  cafe: { 
    name: 'Cozy Cafe Ambience', 
    id: 'h2zkV-l_TbY'
  },
  alpha: { 
    name: '432Hz Alpha Waves', 
    id: 'WPni755-Krg'
  },
  classical: { 
    name: 'Baroque Focus Music', 
    id: 'jgpJVI3tDbY'
  },
  custom: { 
    name: 'Custom YouTube Stream', 
    id: ''
  }
};

let activeAudioPresetKey = 'lofi';
let isAudioPlaying = false;
let currentAudioEngine = 'idle'; // 'youtube' | 'webaudio' | 'idle'
let audioVolume = parseInt(localStorage.getItem('studytimer_audio_vol') || '60', 10);
let customYoutubeVideoId = localStorage.getItem('studytimer_custom_yt_id') || '';

let ytPlayerInstance = null;
let isYtApiReady = false;
let currentPlayingVideoId = '';

// Web Audio Procedural Synthesis Nodes
let webAudioCtx = null;
let webAudioGainNode = null;
let webAudioNodes = [];

function updateAudioEngineBadge(engine = 'ready') {
  const badge = document.getElementById('audioEngineBadge');
  if (!badge) return;
  badge.className = 'audio-engine-badge';
  if (engine === 'youtube') {
    badge.classList.add('engine-youtube');
    badge.textContent = 'YouTube';
    badge.title = 'Official YouTube API Stream';
  } else {
    badge.classList.add('engine-offline');
    badge.textContent = 'Ready';
    badge.title = 'Audio player ready';
  }
}

// ----------------------------------------------------------------------------
// Procedural Web Audio Engine (100% Offline & Network-Proof)
// ----------------------------------------------------------------------------
function initWebAudioContext() {
  try {
    if (!webAudioCtx && (typeof window !== 'undefined')) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) {
        webAudioCtx = new AudioCtx();
      }
    }
    if (webAudioCtx && webAudioCtx.state === 'suspended') {
      webAudioCtx.resume().catch(() => {});
    }
  } catch (_) {}
  return webAudioCtx;
}

function stopWebAudioAmbience() {
  if (webAudioNodes && webAudioNodes.length) {
    webAudioNodes.forEach(n => {
      try { if (n.stop) n.stop(); } catch (_) {}
      try { if (n.disconnect) n.disconnect(); } catch (_) {}
    });
    webAudioNodes = [];
  }
  webAudioGainNode = null;
}

function setWebAudioVolume(vol) {
  if (webAudioGainNode && webAudioCtx) {
    try {
      const targetGain = Math.max(0, Math.min(100, vol)) / 100 * 0.35;
      webAudioGainNode.gain.setValueAtTime(targetGain, webAudioCtx.currentTime);
    } catch (_) {}
  }
}

function playProceduralAmbience(type) {
  // Pure tone synthesizer removed per specification
  return false;
}

// ----------------------------------------------------------------------------
// YouTube IFrame API Handler (Robust & Resilient Loader)
// ----------------------------------------------------------------------------
let isYtApiLoading = false;
let isYtPlayerInitializing = false;
let isUserExplicitPlayAction = false;
const ytApiReadyCallbacks = [];
let pendingPlayVideoId = null;
let pendingAutoPlay = false;

function ensureYouTubeIframeAPILoaded(callback) {
  if (typeof window === 'undefined') return;
  
  if (window.YT && window.YT.Player) {
    isYtApiReady = true;
    if (typeof callback === 'function') callback();
    return;
  }

  if (typeof callback === 'function') {
    ytApiReadyCallbacks.push(callback);
  }

  if (isYtApiLoading) return;
  isYtApiLoading = true;

  const prevReady = window.onYouTubeIframeAPIReady;
  window.onYouTubeIframeAPIReady = function() {
    isYtApiReady = true;
    isYtApiLoading = false;
    if (typeof prevReady === 'function') {
      try { prevReady(); } catch (_) {}
    }
    while (ytApiReadyCallbacks.length > 0) {
      const cb = ytApiReadyCallbacks.shift();
      try { cb(); } catch (err) { console.warn('[YouTube API] Callback error:', err); }
    }
  };

  if (!document.querySelector('script[src*="youtube.com/iframe_api"]')) {
    const tag = document.createElement('script');
    tag.src = 'https://www.youtube.com/iframe_api';
    const firstScript = document.getElementsByTagName('script')[0];
    if (firstScript && firstScript.parentNode) {
      firstScript.parentNode.insertBefore(tag, firstScript);
    } else {
      document.head.appendChild(tag);
    }
  }

  let attempts = 0;
  const pollInterval = setInterval(() => {
    attempts++;
    if (window.YT && window.YT.Player) {
      clearInterval(pollInterval);
      isYtApiReady = true;
      isYtApiLoading = false;
      while (ytApiReadyCallbacks.length > 0) {
        const cb = ytApiReadyCallbacks.shift();
        try { cb(); } catch (_) {}
      }
    } else if (attempts > 80) {
      clearInterval(pollInterval);
      isYtApiLoading = false;
    }
  }, 100);
}

if (typeof window !== 'undefined' && window.YT && window.YT.Player) {
  isYtApiReady = true;
}


let audioPlaybackWatchdogTimer = null;
function armAudioPlaybackWatchdog() {
  clearAudioPlaybackWatchdog();
  audioPlaybackWatchdogTimer = setTimeout(() => {
    if (!ytPlayerInstance || typeof ytPlayerInstance.getPlayerState !== 'function') return;
    try {
      const state = ytPlayerInstance.getPlayerState();
      if (state !== 1) { // Not playing
        setAudioPlayingUI(false);
        updateAudioEngineBadge('ready');
        if (isUserExplicitPlayAction) {
          showToast('Could not start stream. Please try again or select another track 🎵', 'info');
          isUserExplicitPlayAction = false;
        }
      }
    } catch (_) {}
  }, 7000);
}

function clearAudioPlaybackWatchdog() {
  if (audioPlaybackWatchdogTimer) {
    clearTimeout(audioPlaybackWatchdogTimer);
    audioPlaybackWatchdogTimer = null;
  }
}

// Global Audio Engine State Poller (Ensures UI never stays in fake playing state)
if (typeof window !== 'undefined' && !window.__studyTimerAudioPollerInitialized) {
  window.__studyTimerAudioPollerInitialized = true;
  setInterval(() => {
    if (ytPlayerInstance && typeof ytPlayerInstance.getPlayerState === 'function') {
      try {
        const state = ytPlayerInstance.getPlayerState();
        if (state === 1) {
          if (!isAudioPlaying) {
            setAudioPlayingUI(true);
            currentAudioEngine = 'youtube';
            updateAudioEngineBadge('youtube');
          }
        } else if (state === 2 || state === 0 || state === 5 || state === -1) {
          if (isAudioPlaying) {
            setAudioPlayingUI(false);
            updateAudioEngineBadge('ready');
          }
        }
      } catch (_) {}
    }
  }, 1200);
}

function initYouTubePlayerInstance(customVidId, autoPlay = false) {
  const container = document.getElementById('youtubePlayerAnchor');
  if (!container) return;

  const preset = AUDIO_PRESETS[activeAudioPresetKey];
  const targetVidId = customVidId || currentPlayingVideoId || (activeAudioPresetKey === 'custom' ? (customYoutubeVideoId || '5yx6BWlEVcY') : (preset ? preset.id : '5yx6BWlEVcY'));
  
  pendingPlayVideoId = targetVidId;
  pendingAutoPlay = autoPlay || isAudioPlaying;
  currentPlayingVideoId = targetVidId;

  // If already instantiated, reuse it cleanly
  if (ytPlayerInstance && typeof ytPlayerInstance.loadVideoById === 'function') {
    if (pendingAutoPlay) {
      try {
        ytPlayerInstance.loadVideoById({
          videoId: targetVidId,
          startSeconds: 0
        });
        ytPlayerInstance.unMute();
        ytPlayerInstance.setVolume(audioVolume);
        ytPlayerInstance.playVideo();
        currentAudioEngine = 'youtube';
        updateAudioEngineBadge('buffering');
      } catch (err) {
        console.warn('[Audio Engine] Reuse loadVideoById error:', err);
      }
    } else {
      try {
        ytPlayerInstance.cueVideoById({
          videoId: targetVidId,
          startSeconds: 0
        });
      } catch (_) {}
    }
    return;
  }

  if (isYtPlayerInitializing) {
    return; // Will be picked up by onReady
  }

  if (!window.YT || !window.YT.Player) {
    ensureYouTubeIframeAPILoaded(() => {
      initYouTubePlayerInstance(customVidId, autoPlay);
    });
    return;
  }

  let target = document.getElementById('ytPlayerTarget');
  if (!target) {
    target = document.createElement('div');
    target.id = 'ytPlayerTarget';
    container.appendChild(target);
  }

  isYtPlayerInitializing = true;

  try {
    const playerVars = {
      autoplay: pendingAutoPlay ? 1 : 0,
      controls: 0,
      disablekb: 1,
      enablejsapi: 1,
      fs: 0,
      iv_load_policy: 3,
      loop: 1,
      modestbranding: 1,
      playsinline: 1,
      rel: 0
    };

    if (typeof window !== 'undefined' && window.location && window.location.protocol.startsWith('http')) {
      playerVars.origin = window.location.origin;
      playerVars.widget_referrer = window.location.origin;
    }

    ytPlayerInstance = new window.YT.Player('ytPlayerTarget', {
      height: '112',
      width: '200',
      videoId: targetVidId,
      playerVars: playerVars,
      events: {
        onReady: (event) => {
          isYtApiReady = true;
          isYtPlayerInitializing = false;
          try {
            const iframe = typeof event.target.getIframe === 'function' ? event.target.getIframe() : null;
            if (iframe) {
              iframe.setAttribute('referrerpolicy', 'strict-origin-when-cross-origin');
              iframe.setAttribute('allow', 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share');
            }
          } catch (_) {}
          try {
            event.target.unMute();
            event.target.setVolume(audioVolume);
            const shouldPlay = pendingAutoPlay || isAudioPlaying;
            if (shouldPlay) {
              const toPlay = pendingPlayVideoId || targetVidId;
              if (toPlay !== targetVidId && typeof event.target.loadVideoById === 'function') {
                event.target.loadVideoById({ videoId: toPlay, startSeconds: 0 });
              }
              event.target.playVideo();
              currentAudioEngine = 'youtube';
              updateAudioEngineBadge('buffering');
            }
          } catch (err) {
            console.warn('[Audio Engine] onReady auto-play error:', err);
          }
        },
        onStateChange: (event) => {
          if (!window.YT) return;
          if (event.data === window.YT.PlayerState.PLAYING) {
            setAudioPlayingUI(true);
            currentAudioEngine = 'youtube';
            updateAudioEngineBadge('youtube');
            isUserExplicitPlayAction = false;
            clearAudioPlaybackWatchdog();
          } else if (event.data === window.YT.PlayerState.BUFFERING) {
            updateAudioEngineBadge('buffering');
          } else if (event.data === window.YT.PlayerState.PAUSED || event.data === window.YT.PlayerState.CUED || event.data === window.YT.PlayerState.UNSTARTED || event.data === window.YT.PlayerState.ENDED) {
            if (event.data === window.YT.PlayerState.ENDED) {
              try { event.target.playVideo(); } catch (_) {}
            } else {
              setAudioPlayingUI(false);
              updateAudioEngineBadge('ready');
            }
          }
        },
        onError: (event) => {
          console.warn('[Audio Engine] YouTube API error code:', event.data);
          isYtPlayerInitializing = false;
          setAudioPlayingUI(false);
          isAudioPlaying = false;
          currentAudioEngine = 'idle';
          updateAudioEngineBadge('ready');
          if (isUserExplicitPlayAction) {
            showToast('Unable to stream this audio track. Please select another station 🎵', 'info');
            isUserExplicitPlayAction = false;
          }
        }
      }
    });
  } catch (err) {
    console.warn('[Audio Engine] YT.Player constructor error:', err);
    isYtPlayerInitializing = false;
  }
}

function initFocusAudio() {
  const select = document.getElementById('audioPresetSelect');
  const playBtn = document.getElementById('btnAudioPlayToggle');
  const volSlider = document.getElementById('audioVolumeSlider');
  const trackName = document.getElementById('audioCurrentName');

  if (volSlider) {
    volSlider.value = audioVolume;
    volSlider.addEventListener('input', (e) => {
      audioVolume = parseInt(e.target.value, 10);
      localStorage.setItem('studytimer_audio_vol', audioVolume);
      setAudioVolume(audioVolume);
    });
  }

  if (select) {
    select.value = activeAudioPresetKey;
    select.addEventListener('change', (e) => {
      const val = e.target.value;
      if (val === 'custom') {
        openCustomYoutubeModal();
      } else {
        switchAudioTrack(val);
      }
    });
  }

  playBtn?.addEventListener('click', () => {
    initWebAudioContext();
    toggleAudioPlay();
  });

  const POPULAR_STREAM_NAMES = {
    '5yx6BWlEVcY': '🎧 Chillhop Lofi — Beats to Relax & Study',
    'amfWIRasxtI': '🎧 Lofi Chill Beats — Focus & Study',
    'vCTRNKPJr40': '⛏️ Minecraft Tracks — Peaceful Piano & Synth',
    'lTRiuFIWV54': '🎧 Lofi Girl — 1 A.M. Study Session',
    'jfKfPfyJRdk': '🎧 Lofi Girl — Beats to Relax/Study to',
    '5qap5aO4i9A': '🎧 Lofi Girl — Beats to Relax/Study to',
    'FjHGZj2IjBk': '🎹 Peaceful Study Piano — Relaxing Melodies',
    'DWcJFNfaw9c': '🎹 Peaceful Study Piano — Relaxing Melodies',
    '4xDzrJKXOOY': '🌆 Synthwave Chill Radio — Retro Focus Beats',
    'mPZkdNFkNps': '🌧️ Gentle Rain & Thunder — Calming Soundscape',
    'gaGrHUekGdc': '☕ Cozy Coffee Shop Ambience — Focus Background',
    'e3L1I7i4Z40': '☕ Cozy Coffee Shop Ambience — Focus Background',
    'WPni755-Krg': '🧠 432Hz Deep Alpha Waves — Study & Concentration',
    'jgpJVI3tDbY': '🎻 Baroque Classical Music — High Brain Focus'
  };

  function updateMusicCoverPreview(urlOrId) {
    const thumb = document.getElementById('customMusicPreviewThumb');
    const title = document.getElementById('customMusicPreviewTitle');
    const channel = document.getElementById('customMusicPreviewChannel');
    const badge = document.getElementById('customMusicPreviewBadge');
    if (!thumb || !title) return;

    const trimmed = (urlOrId || '').trim();
    const vidId = extractYouTubeVideoId(trimmed) || (trimmed.length === 11 ? trimmed : (customYoutubeVideoId || '5yx6BWlEVcY'));

    if (vidId) {
      thumb.src = `https://img.youtube.com/vi/${vidId}/hqdefault.jpg`;
      thumb.onerror = () => { thumb.src = 'assets/logo.png'; };
      const knownName = POPULAR_STREAM_NAMES[vidId];
      title.textContent = knownName || `YouTube Stream (${vidId})`;
      if (channel) channel.textContent = 'Focus Ambience • Official YouTube Stream';
      if (badge) badge.textContent = '▶ Click to Play Stream';
    }
  }

  // Real-time input listener for URL cover preview
  document.getElementById('customYoutubeUrlInput')?.addEventListener('input', (e) => {
    updateMusicCoverPreview(e.target.value);
  });

  // Wire suggestion chips in modal
  document.querySelectorAll('.yt-suggestion-chip').forEach(chip => {
    chip.addEventListener('click', () => {
      document.querySelectorAll('.yt-suggestion-chip').forEach(c => c.classList.remove('active'));
      chip.classList.add('active');
      const vidId = chip.dataset.id;
      const input = document.getElementById('customYoutubeUrlInput');
      if (input && vidId) {
        input.value = `https://www.youtube.com/watch?v=${vidId}`;
        customYoutubeVideoId = vidId;
        updateMusicCoverPreview(vidId);
        localStorage.setItem('studytimer_custom_yt_id', vidId);
        showToast(`Selected ${chip.textContent.trim()} — Click Preview to Play`, 'info');
      }
    });
  });

  // Dedicated function to commit and start playback
  function loadAndPlayCustomYoutube() {
    initWebAudioContext();
    const input = document.getElementById('customYoutubeUrlInput');
    const val = input?.value.trim() || customYoutubeVideoId || '5yx6BWlEVcY';
    if (val) {
      const parsedId = extractYouTubeVideoId(val) || (val.length === 11 ? val : null);
      if (parsedId) {
        customYoutubeVideoId = parsedId;
        localStorage.setItem('studytimer_custom_yt_id', parsedId);
      } else {
        customYoutubeVideoId = '5yx6BWlEVcY';
        localStorage.setItem('studytimer_custom_yt_id', '5yx6BWlEVcY');
      }
      closeCustomYoutubeModal();
      switchAudioTrack('custom');
      startCurrentAudio();
      showToast('Connecting to YouTube stream... 🎧', 'info');
    }
  }

  // Play when clicking "Load & Play" button
  document.getElementById('btnLoadCustomYoutube')?.addEventListener('click', loadAndPlayCustomYoutube);

  // Play when clicking directly on the Preview Card
  document.getElementById('customMusicPreviewCard')?.addEventListener('click', loadAndPlayCustomYoutube);

  // Initialize Draggable & Collapsible Audio Dock
  initDraggableAudioDock();

  // Wire topbar audio trigger
  document.getElementById('btnTopbarAudio')?.addEventListener('click', openCustomYoutubeModal);

  // Wire custom modal buttons
  document.getElementById('btnCloseCustomYoutubeModal')?.addEventListener('click', closeCustomYoutubeModal);
  document.getElementById('btnCancelCustomYoutube')?.addEventListener('click', closeCustomYoutubeModal);
  document.getElementById('customYoutubeModalOverlay')?.addEventListener('click', (e) => {
    if (e.target.id === 'customYoutubeModalOverlay') closeCustomYoutubeModal();
  });

  // Initialize YouTube API Player if ready
  if (typeof window !== 'undefined' && (window.YT || isYtApiReady)) {
    initYouTubePlayerInstance();
  }

  updateAudioEngineBadge('ready');
}

function extractYouTubeVideoId(urlOrId) {
  if (!urlOrId || typeof urlOrId !== 'string') return null;
  const trimmed = urlOrId.trim();
  if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) return trimmed;
  const regExp = /(?:youtu\.be\/|youtube\.com\/(?:embed\/|v\/|watch\?v=|watch\?.+&v=|live\/|shorts\/)|music\.youtube\.com\/watch\?v=)([\w-]{11})/;
  const match = trimmed.match(regExp);
  return match ? match[1] : null;
}

function initDraggableAudioDock() {
  const dock = document.getElementById('floatingAudioDock');
  const handle = document.getElementById('audioDragHandle');
  const collapseBtn = document.getElementById('btnAudioCollapse');
  if (!dock) return;

  function clampDockPosition() {
    if (!dock.style.left && !dock.style.top) return;

    const rect = dock.getBoundingClientRect();
    const dockW = dock.offsetWidth || rect.width || 200;
    const dockH = dock.offsetHeight || rect.height || 44;
    const maxX = Math.max(10, window.innerWidth - dockW - 12);
    const maxY = Math.max(10, window.innerHeight - dockH - 12);

    let currentLeft = parseFloat(dock.style.left) || rect.left;
    let currentTop = parseFloat(dock.style.top) || rect.top;

    const clampedX = Math.max(10, Math.min(currentLeft, maxX));
    const clampedY = Math.max(10, Math.min(currentTop, maxY));

    dock.style.left = `${clampedX}px`;
    dock.style.top = `${clampedY}px`;
    dock.style.bottom = 'auto';
    dock.style.right = 'auto';

    try {
      localStorage.setItem('studytimer_audio_dock_pos', JSON.stringify({ x: clampedX, y: clampedY }));
    } catch (_) {}
  }

  function resetDockToDefaultPosition() {
    dock.style.left = '';
    dock.style.top = '';
    dock.style.bottom = '';
    dock.style.right = '';
    try {
      localStorage.removeItem('studytimer_audio_dock_pos');
    } catch (_) {}
  }

  // Restore collapsed state
  const isCollapsed = localStorage.getItem('studytimer_audio_dock_collapsed') === 'true';
  if (isCollapsed) {
    dock.classList.add('dock-collapsed');
  }

  collapseBtn?.addEventListener('click', (e) => {
    e.stopPropagation();
    const collapsed = dock.classList.toggle('dock-collapsed');
    try {
      localStorage.setItem('studytimer_audio_dock_collapsed', collapsed ? 'true' : 'false');
    } catch (_) {}
    if (dock.style.left || dock.style.top) {
      setTimeout(clampDockPosition, 60);
    }
  });

  // Restore saved position ONLY if it is a genuine user-dragged position
  try {
    const rawPos = localStorage.getItem('studytimer_audio_dock_pos');
    if (rawPos) {
      const savedPos = JSON.parse(rawPos);
      if (savedPos && typeof savedPos.x === 'number' && typeof savedPos.y === 'number') {
        if (savedPos.x <= 80 && savedPos.y <= 80) {
          resetDockToDefaultPosition();
        } else {
          const maxX = Math.max(10, window.innerWidth - (dock.offsetWidth || 300) - 12);
          const maxY = Math.max(10, window.innerHeight - (dock.offsetHeight || 44) - 12);
          const x = Math.max(10, Math.min(savedPos.x, maxX));
          const y = Math.max(10, Math.min(savedPos.y, maxY));
          dock.style.left = `${x}px`;
          dock.style.top = `${y}px`;
          dock.style.bottom = 'auto';
          dock.style.right = 'auto';
        }
      }
    }
  } catch (_) {}

  handle?.addEventListener('dblclick', (e) => {
    e.stopPropagation();
    resetDockToDefaultPosition();
  });

  window.addEventListener('resize', () => {
    if (dock.style.left || dock.style.top) {
      clampDockPosition();
    }
  });

  let isDragging = false;
  let startX = 0;
  let startY = 0;
  let initialLeft = 0;
  let initialTop = 0;

  const dragTarget = handle || dock;

  function onPointerDown(e) {
    if (e.target.closest('button') || e.target.closest('select') || e.target.closest('input')) {
      return;
    }
    isDragging = true;
    const rect = dock.getBoundingClientRect();
    initialLeft = rect.left;
    initialTop = rect.top;
    const clientX = e.clientX ?? (e.touches && e.touches[0] ? e.touches[0].clientX : 0);
    const clientY = e.clientY ?? (e.touches && e.touches[0] ? e.touches[0].clientY : 0);
    startX = clientX;
    startY = clientY;
    dock.style.transition = 'none';

    try {
      if (e.pointerId && typeof dragTarget.setPointerCapture === 'function') {
        dragTarget.setPointerCapture(e.pointerId);
      }
    } catch (_) {}

    document.addEventListener('pointermove', onPointerMove, { passive: false });
    document.addEventListener('pointerup', onPointerUp);
    document.addEventListener('pointercancel', onPointerUp);
    document.addEventListener('touchmove', onTouchMove, { passive: false });
    document.addEventListener('touchend', onPointerUp);
  }

  function updateDockPosition(curX, curY) {
    const dx = curX - startX;
    const dy = curY - startY;

    let nextX = initialLeft + dx;
    let nextY = initialTop + dy;

    const maxX = Math.max(10, window.innerWidth - (dock.offsetWidth || 200) - 12);
    const maxY = Math.max(10, window.innerHeight - (dock.offsetHeight || 44) - 12);

    nextX = Math.max(10, Math.min(nextX, maxX));
    nextY = Math.max(10, Math.min(nextY, maxY));

    dock.style.left = `${nextX}px`;
    dock.style.top = `${nextY}px`;
    dock.style.bottom = 'auto';
    dock.style.right = 'auto';
  }

  function onPointerMove(e) {
    if (!isDragging) return;
    if (e.cancelable) e.preventDefault();
    const curX = e.clientX ?? 0;
    const curY = e.clientY ?? 0;
    updateDockPosition(curX, curY);
  }

  function onTouchMove(e) {
    if (!isDragging) return;
    if (e.cancelable) e.preventDefault();
    if (e.touches && e.touches[0]) {
      updateDockPosition(e.touches[0].clientX, e.touches[0].clientY);
    }
  }

  function onPointerUp(e) {
    if (!isDragging) return;
    isDragging = false;
    dock.style.transition = '';
    try {
      if (e && e.pointerId && typeof dragTarget.releasePointerCapture === 'function') {
        dragTarget.releasePointerCapture(e.pointerId);
      }
    } catch (_) {}
    document.removeEventListener('pointermove', onPointerMove);
    document.removeEventListener('pointerup', onPointerUp);
    document.removeEventListener('pointercancel', onPointerUp);
    document.removeEventListener('touchmove', onTouchMove);
    document.removeEventListener('touchend', onPointerUp);

    clampDockPosition();
  }

  dragTarget.addEventListener('pointerdown', onPointerDown);
  dragTarget.addEventListener('touchstart', onPointerDown, { passive: true });
}

function switchAudioTrack(presetKey) {
  activeAudioPresetKey = presetKey;
  const select = document.getElementById('audioPresetSelect');
  if (select && select.value !== presetKey) select.value = presetKey;

  const trackName = document.getElementById('audioCurrentName');
  const preset = AUDIO_PRESETS[presetKey];
  if (trackName) {
    if (presetKey === 'custom') {
      const knownNames = {
        '5yx6BWlEVcY': '🎧 Chillhop Lofi',
        'amfWIRasxtI': '🎧 Lofi Chill',
        'vCTRNKPJr40': '⛏️ Minecraft Tracks',
        'lTRiuFIWV54': '🎧 Lofi Girl',
        'jfKfPfyJRdk': '🎧 Lofi Girl',
        '5qap5aO4i9A': '🎧 Lofi Girl',
        'FjHGZj2IjBk': '🎹 Study Piano',
        'DWcJFNfaw9c': '🎹 Study Piano',
        '4xDzrJKXOOY': '🌆 Synthwave',
        'mPZkdNFkNps': '🌧️ Rain Storm',
        'gaGrHUekGdc': '☕ Coffee Shop',
        'e3L1I7i4Z40': '☕ Coffee Shop',
        'WPni755-Krg': '🧠 432Hz Alpha',
        'jgpJVI3tDbY': '🎻 Classical'
      };
      const known = knownNames[customYoutubeVideoId];
      trackName.textContent = known || (customYoutubeVideoId ? `Stream (${customYoutubeVideoId.substring(0, 8)}...)` : 'Custom YouTube Stream');
    } else if (preset) {
      trackName.textContent = preset.name;
    }
  }

  if (isAudioPlaying) {
    stopCurrentAudio();
    startCurrentAudio();
  }
}

function toggleAudioPlay() {
  if (isAudioPlaying) {
    pauseCurrentAudio();
    setAudioPlayingUI(false);
  } else {
    updateAudioEngineBadge('buffering');
    resumeCurrentAudio();
  }
}

function pauseCurrentAudio() {
  if (ytPlayerInstance && typeof ytPlayerInstance.pauseVideo === 'function') {
    try { ytPlayerInstance.pauseVideo(); } catch (_) {}
  }
  stopWebAudioAmbience();
  setAudioPlayingUI(false);
}

function resumeCurrentAudio() {
  initWebAudioContext();

  const preset = AUDIO_PRESETS[activeAudioPresetKey];
  const videoId = activeAudioPresetKey === 'custom' ? (customYoutubeVideoId || '5yx6BWlEVcY') : (preset ? preset.id : '5yx6BWlEVcY');

  if (currentPlayingVideoId === videoId && ytPlayerInstance && typeof ytPlayerInstance.playVideo === 'function') {
    try {
      ytPlayerInstance.unMute();
      ytPlayerInstance.setVolume(audioVolume);
      ytPlayerInstance.playVideo();
      currentAudioEngine = 'youtube';
      updateAudioEngineBadge('buffering');
      return;
    } catch (_) {}
  }

  startCurrentAudio();
}

function setAudioPlayingUI(playing) {
  isAudioPlaying = playing;
  const playBtn = document.getElementById('btnAudioPlayToggle');
  const eqBars = document.getElementById('audioEqualizerBars');
  const playIcon = playBtn?.querySelector('.audio-icon-play');
  const pauseIcon = playBtn?.querySelector('.audio-icon-pause');

  if (playing) {
    playIcon?.classList.add('hidden');
    pauseIcon?.classList.remove('hidden');
    eqBars?.classList.add('is-playing');
  } else {
    playIcon?.classList.remove('hidden');
    pauseIcon?.classList.add('hidden');
    eqBars?.classList.remove('is-playing');
  }
}

function startCurrentAudio() {
  const preset = AUDIO_PRESETS[activeAudioPresetKey];
  if (!preset) return;

  const videoId = activeAudioPresetKey === 'custom' ? (customYoutubeVideoId || '5yx6BWlEVcY') : preset.id;
  if (videoId) {
    if (typeof window !== 'undefined' && window.location && window.location.protocol === 'file:') {
      showToast('YouTube streaming requires running from a web server (e.g. http://localhost or online) due to browser security restrictions on local files 💡', 'info');
    }
    isUserExplicitPlayAction = true;
    updateAudioEngineBadge('buffering');
    armAudioPlaybackWatchdog();
    startYouTubeEmbedPlayer(videoId);
  } else {
    setAudioPlayingUI(false);
    isAudioPlaying = false;
    if (isUserExplicitPlayAction) {
      showToast('No valid audio track configured.', 'info');
      isUserExplicitPlayAction = false;
    }
  }
}

function stopCurrentAudio() {
  pauseCurrentAudio();
  stopYouTubeAudio();
  stopWebAudioAmbience();
  currentAudioEngine = 'idle';
  updateAudioEngineBadge('ready');
}

function setAudioVolume(vol) {
  const normVol = Math.max(0, Math.min(100, vol));
  if (ytPlayerInstance && typeof ytPlayerInstance.setVolume === 'function') {
    try {
      ytPlayerInstance.setVolume(normVol);
      if (normVol > 0) {
        ytPlayerInstance.unMute();
      } else {
        ytPlayerInstance.mute();
      }
    } catch (_) {}
  }
  setWebAudioVolume(normVol);
}

function startYouTubeEmbedPlayer(videoId) {
  if (!videoId) return;
  currentPlayingVideoId = videoId;
  stopWebAudioAmbience();

  // 1. If YT.Player instance is ready, load and play video cleanly via official API
  if (ytPlayerInstance && typeof ytPlayerInstance.loadVideoById === 'function') {
    try {
      ytPlayerInstance.loadVideoById({
        videoId: videoId,
        startSeconds: 0
      });
      ytPlayerInstance.unMute();
      ytPlayerInstance.setVolume(audioVolume);
      ytPlayerInstance.playVideo();
        currentAudioEngine = 'youtube';
        updateAudioEngineBadge('buffering');
      return;
    } catch (err) {
      console.warn('[Audio Engine] YT.Player loadVideoById failed:', err);
    }
  }

  // 2. Initialize official YT.Player with autoPlay
  initYouTubePlayerInstance(videoId, true);
}

function stopYouTubeAudio() {
  if (ytPlayerInstance && typeof ytPlayerInstance.stopVideo === 'function') {
    try { ytPlayerInstance.stopVideo(); } catch (_) {}
  } else if (ytPlayerInstance && typeof ytPlayerInstance.pauseVideo === 'function') {
    try { ytPlayerInstance.pauseVideo(); } catch (_) {}
  }
  currentPlayingVideoId = '';
}

function openCustomYoutubeModal() {
  const modal = document.getElementById('customYoutubeModalOverlay');
  const input = document.getElementById('customYoutubeUrlInput');
  const currentVal = customYoutubeVideoId ? `https://www.youtube.com/watch?v=${customYoutubeVideoId}` : '';

  if (input) {
    input.value = currentVal;
  }
  if (typeof updateMusicCoverPreview === 'function') {
    updateMusicCoverPreview(currentVal || customYoutubeVideoId || 'amfWIRasxtI');
  } else {
    const thumb = document.getElementById('customMusicPreviewThumb');
    if (thumb) {
      const vidId = extractYouTubeVideoId(currentVal) || customYoutubeVideoId || 'amfWIRasxtI';
      thumb.src = `https://img.youtube.com/vi/${vidId}/hqdefault.jpg`;
    }
  }
  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeCustomYoutubeModal() {
  const modal = document.getElementById('customYoutubeModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

// ============================================================================