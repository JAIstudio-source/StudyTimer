// ============================================================================
// STUDYTIMER COMMUNITY SAFETY & PROFANITY FILTER (English + Hindi + Hinglish)
// ============================================================================
// MULTI-TIER COMMUNITY SAFETY & PROFANITY FILTER (English + Hindi + Hinglish)
// ============================================================================
const VULGAR_HINDI_WORDS = [
  "आंड़","आंड","आँड","बहनचोद","बेहेनचोद","भेनचोद","बकचोद","बकचोदी","बेवड़ा","बेवड़े",
  "बेवकूफ","भड़ुआ","भड़वा","भोसड़ा","भोसड़ीके","भोसड़ीकी","भोसड़ीवाला","भोसड़ीवाले",
  "भोसरचोदल","भोसदचोद","भोसड़ाचोदल","भोसड़ाचोद","बब्बे","बूबे","बुर","चरसी","चूचे",
  "चूची","चुची","चोद","चुदने","चुदवा","चुदवाने","चूत","चूतिया","चुटिया","चूतिये",
  "चुत्तड़","चूत्तड़","दलाल","दलले","फट्टू","गधा","गधे","गधालंड","गांड","गांडू",
  "गंडफट","गंडिया","गंडिये","गू","गोटे","हग","हग्गू","हगने","हरामी","हरामजादा",
  "हरामज़ादा","हरामजादे","हरामज़ादे","हरामखोर","झाट","झाटू","कुत्ता","कुत्ते","कुतिया",
  "कुत्ती","लेंडी","लोड़े","लौड़े","लौड़ा","लोड़ा","लौडा","लिंग","लोडा","लोडे","लंड",
  "लौंडा","लौंडे","लौंडी","लौंडिया","लुल्ली","मार","मारो","मारूंगा","मादरचोद","मादरचूत",
  "मादरचुत","मम्मे","मूत","मुत","मूतने","मुतने","मूठ","मुठ","नुननी","नुननु","पाजी",
  "पेसाब","पेशाब","पिल्ला","पिल्ले","पिसाब","पोरकिस्तान","रांड","रंडी","सुअर","सूअर",
  "टट्टे","टट्टी","उल्लू"
];

const VULGAR_HINGLISH_WORDS = [
  "aad","aand","bahenchod","behenchod","bhenchod","bhenchodd","bc","bakchod","bakchodd",
  "bakchodi","bevda","bewda","bevdey","bewday","bevakoof","bevkoof","bevkuf","bewakoof",
  "bewkoof","bewkuf","bhadua","bhaduaa","bhadva","bhadvaa","bhadwa","bhadwaa","bhosada",
  "bhosda","bhosdaa","bhosdike","bhonsdike","bsdk","bhosdiki","bhosdiwala","bhosdiwale",
  "bhosadchodal","bhosadchod","babbe","babbey","bube","bubey","bur","burr","buurr","buur",
  "charsi","chooche","choochi","chuchi","chhod","chod","chodd","chudne","chudney","chudwa",
  "chudwaa","chudwane","chudwaane","choot","chut","chute","chutia","chutiya","chutiye",
  "chuttad","chutad","dalaal","dalal","dalle","dalley","fattu","gadha","gadhe","gadhalund",
  "gaand","gand","gandu","gandfat","gandfut","gandiya","gandiye","goo","gu","gote","gotey",
  "gotte","hag","haggu","hagne","hagney","harami","haramjada","haraamjaada","haramzyada",
  "haraamzyaada","haraamjaade","haraamzaade","haraamkhor","haramkhor","jhat","jhaat","jhaatu",
  "jhatu","kutta","kutte","kuttey","kutia","kutiya","kuttiya","kutti","landi","landy",
  "laude","laudey","laura","lora","lauda","ling","loda","lode","lund","launda","lounde",
  "laundey","laundi","loundi","laundiya","loundiya","lulli","maar","maro","marunga","madarchod",
  "madarchodd","madarchood","madarchoot","madarchut","mc","mamme","mammey","moot","mut",
  "mootne","mutne","mooth","muth","nunni","nunnu","paaji","paji","pesaab","pesab","peshaab",
  "peshab","pilla","pillay","pille","pilley","pisaab","pisab","pkmkb","porkistan","raand",
  "rand","randi","randy","suar","tatte","tatti","tatty","ullu"
];

const VULGAR_ENGLISH_REGEX = /\b(f+[u*@_.-]*c+k+|s+h+[i*@_.-]*t+|b+[i*@_.-]*t+c+h+|a+s+s+h+o+l+e+|d+[i*@_.-]*c+k+|p+u+s+s+y+|c+u+n+t+|w+h+o+r+e+|s+l+u+t+|n+[i*@_.-]*g+g+[a*e*r*]*|f+a+g+g*o*t*|r+e+t+a+r+d+|b+a+s+t+a+r+d+|p+o+r+n+|b+o+o+b+s+|t+i+t+s+|d+i+l+d+o+)\b/i;
const HINGLISH_SET = new Set(VULGAR_HINGLISH_WORDS);

function hasProfanity(text) {
  if (!text || typeof text !== 'string') return false;
  const raw = text.trim();
  if (!raw) return false;

  // 1. Direct Devanagari Match
  for (const w of VULGAR_HINDI_WORDS) {
    if (raw.includes(w)) return true;
  }

  // 2. Leetspeak Normalization
  const normalized = raw.toLowerCase()
    .replace(/[@]/g, 'a')
    .replace(/[$]/g, 's')
    .replace(/[0]/g, 'o')
    .replace(/[1!|]/g, 'i')
    .replace(/[3]/g, 'e');

  const cleanNoPunct = normalized.replace(/[*_.-]/g, '');

  // 3. English Regex Check
  if (VULGAR_ENGLISH_REGEX.test(raw) || VULGAR_ENGLISH_REGEX.test(normalized) || VULGAR_ENGLISH_REGEX.test(cleanNoPunct)) {
    return true;
  }

  // 4. Token Check for Hinglish Slurs
  const tokens = normalized.replace(/[^a-z0-9\s]/g, ' ').split(/\s+/).filter(Boolean);
  const cleanTokens = cleanNoPunct.split(/\s+/).filter(Boolean);

  for (const t of tokens.concat(cleanTokens)) {
    if (HINGLISH_SET.has(t)) return true;
  }

  // 5. Compact Acronym / Compound Phrase Check
  const compactStr = cleanNoPunct.replace(/\s+/g, '');
  const acronyms = ['bsdk', 'pkmkb', 'madarchod', 'bhenchod', 'behenchod', 'gandu'];
  for (const acr of acronyms) {
    if (compactStr.includes(acr)) return true;
  }

  return false;
}

function formatLeaderboardTime(totalSec) {
  if (!totalSec || totalSec <= 0) return '0m';
  const totalMin = Math.round(totalSec / 60);
  if (totalMin >= 60) {
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    return `${h}h ${m > 0 ? m + 'm' : ''}`;
  } else if (totalMin === 0 && totalSec > 0) {
    return `${totalSec}s`;
  }
  return `${totalMin}m`;
}

function normalizeRingClass(ring) {
  if (!ring) return 'glow-gold';
  let r = String(ring).trim().toLowerCase();
  if (r.startsWith('ring-')) r = 'glow-' + r.slice(5);
  if (!r.startsWith('glow-')) r = 'glow-' + r;
  const validRings = ['glow-gold', 'glow-cyan', 'glow-emerald', 'glow-violet', 'glow-rose', 'glow-blue', 'glow-slate'];
  return validRings.includes(r) ? r : 'glow-gold';
}

function getCountryFlagEmoji(codeOrFlag) {
  if (!codeOrFlag || codeOrFlag === 'GLOBAL' || codeOrFlag === '🌐') return '🌐';
  const str = String(codeOrFlag).trim();
  const upper = str.toUpperCase();
  const CODE_TO_FLAG = {
    'US': '🇺🇸', 'USA': '🇺🇸', 'UNITED STATES': '🇺🇸',
    'IN': '🇮🇳', 'IND': '🇮🇳', 'INDIA': '🇮🇳',
    'GB': '🇬🇧', 'UK': '🇬🇧', 'UNITED KINGDOM': '🇬🇧',
    'CA': '🇨🇦', 'CAN': '🇨🇦', 'CANADA': '🇨🇦',
    'DE': '🇩🇪', 'GER': '🇩🇪', 'GERMANY': '🇩🇪',
    'FR': '🇫🇷', 'FRA': '🇫🇷', 'FRANCE': '🇫🇷',
    'JP': '🇯🇵', 'JPN': '🇯🇵', 'JAPAN': '🇯🇵',
    'KR': '🇰🇷', 'KOR': '🇰🇷', 'KOREA': '🇰🇷', 'SOUTH KOREA': '🇰🇷',
    'BR': '🇧🇷', 'BRA': '🇧🇷', 'BRAZIL': '🇧🇷',
    'AU': '🇦🇺', 'AUS': '🇦🇺', 'AUSTRALIA': '🇦🇺',
    'IT': '🇮🇹', 'ITA': '🇮🇹', 'ITALY': '🇮🇹',
    'ES': '🇪🇸', 'ESP': '🇪🇸', 'SPAIN': '🇪🇸',
    'RU': '🇷🇺', 'RUS': '🇷🇺', 'RUSSIA': '🇷🇺',
    'CN': '🇨🇳', 'CHN': '🇨🇳', 'CHINA': '🇨🇳',
    'MX': '🇲🇽', 'MEX': '🇲🇽', 'MEXICO': '🇲🇽',
    'ID': '🇮🇩', 'IDN': '🇮🇩', 'INDONESIA': '🇮🇩',
    'PK': '🇵🇰', 'PAK': '🇵🇰', 'PAKISTAN': '🇵🇰',
    'BD': '🇧🇩', 'BGD': '🇧🇩', 'BANGLADESH': '🇧🇩',
    'NG': '🇳🇬', 'NGA': '🇳🇬', 'NIGERIA': '🇳🇬',
    'VN': '🇻🇳', 'VNM': '🇻🇳', 'VIETNAM': '🇻🇳',
    'PH': '🇵🇭', 'PHL': '🇵🇭', 'PHILIPPINES': '🇵🇭',
    'TR': '🇹🇷', 'TUR': '🇹🇷', 'TURKEY': '🇹🇷'
  };
  if (CODE_TO_FLAG[upper]) return CODE_TO_FLAG[upper];
  if (/\p{Regional_Indicator}/u.test(str)) return str;
  return '🌐';
}

const AVATAR_PRESET_STICKER_MAP = {
  'avatar_default': '🎓',
  'avatar_cat': '🐱',
  'avatar_fox': '🦊',
  'avatar_lion': '🦁',
  'avatar_panda': '🐼',
  'avatar_owl': '🦉',
  'avatar_rocket': '🚀',
  'avatar_fire': '🔥',
  'avatar_star': '⭐',
  'avatar_scholar': '🎓',
  'avatar_books': '📚',
  'avatar_brain': '🧠',
  'avatar_science': '🔬',
  'avatar_med': '🩺',
  'avatar_coder': '💻',
  'avatar_lightning': '⚡',
  'avatar_artist': '🎨',
  'avatar_lotus': '🌸',
  'avatar_forest': '🌲',
  'avatar_coffee': '☕',
  'avatar_moon': '🌙',
  'avatar_target': '🎯',
  'avatar_diamond': '💎',
  'avatar_champion': '🏆',
  'avatar_crown': '👑',
  'avatar_saturn': '🪐',
  'avatar_gamer': '🎮',
  'avatar_lofi': '🎧',
  'cat': '🐱',
  'fox': '🦊',
  'lion': '🦁',
  'panda': '🐼',
  'owl': '🦉',
  'rocket': '🚀',
  'fire': '🔥',
  'star': '⭐',
  'scholar': '🎓',
  'books': '📚',
  'brain': '🧠',
  'coder': '💻',
  'lightning': '⚡',
  'coffee': '☕',
  'target': '🎯',
  'diamond': '💎',
  'champion': '🏆',
  'crown': '👑',
  'default': '🎓'
};

function resolveAvatarSticker(val) {
  if (!val || typeof val !== 'string') return '';
  const trimmed = val.trim();
  if (trimmed === '') return '';
  const lower = trimmed.toLowerCase();
  if (AVATAR_PRESET_STICKER_MAP[lower]) {
    return AVATAR_PRESET_STICKER_MAP[lower];
  }
  return trimmed;
}

function getPublicLeaderboardAvatarUrl(profile) {
  if (!profile) return '';
  const rawAvatar = resolveAvatarSticker(profile.avatarPreset || profile.avatar_url || profile.profile_image_uri || appState.currentUser?.user_metadata?.avatar_url || '');
  if (!rawAvatar) {
    const defaultAuthAvatar = appState.currentUser?.user_metadata?.avatar_url || '';
    if (defaultAuthAvatar) return defaultAuthAvatar;
    return '';
  }
  return rawAvatar;
}

function getAvatarElementHtml(avatarVal, userName, className = 'row-avatar-img', ringClass = '') {
  const resolved = resolveAvatarSticker(avatarVal);
  const normalizedRing = ringClass ? normalizeRingClass(ringClass) : '';
  const ringCls = normalizedRing ? ` ${normalizedRing}` : '';
  const trimmed = typeof resolved === 'string' ? resolved.trim() : '';
  const isUrl = /^(http|https|data:|assets\/|\/|blob:)/i.test(trimmed);
  const fallbackInitial = (userName && userName.charAt(0).toUpperCase()) || 'S';

  if (isUrl) {
    return `<img src="${trimmed}" alt="${userName || 'Student'}" class="${className}${ringCls}" loading="eager" decoding="async" referrerpolicy="no-referrer" crossorigin="anonymous" onerror="this.onerror=null; this.outerHTML='<span class=\\'avatar-sticker ${className}${ringCls}\\'>${fallbackInitial}</span>';">`;
  } else {
    const isEmojiOrShort = trimmed.length > 0 && (trimmed.length <= 4 || /[\u{1F300}-\u{1F9FF}\u{2600}-\u{26FF}]/u.test(trimmed));
    const displaySticker = isEmojiOrShort ? trimmed : fallbackInitial;
    return `<span class="avatar-sticker ${className}${ringCls}">${displaySticker}</span>`;
  }
}

// ----------------------------------------------------------------------------