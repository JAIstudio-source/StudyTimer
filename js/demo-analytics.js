// ============================================================================
// STUDYTIMER ANALYTICS, CHARTS, HEATMAP, CALENDAR & PLANNER MODULE
// ============================================================================

// 7. WORKSPACE VIEW SWITCHER & ANALYTICS SUITE
// ============================================================================

let currentWorkspaceView = 'timer';

function switchWorkspaceView(viewKey) {
  if (viewKey === 'leaderboard') {
    openLeaderboardModal();
    return;
  }

  currentWorkspaceView = viewKey;

  // Update sidebar active buttons
  document.querySelectorAll('.sidebar-nav-item').forEach(btn => {
    if (btn.dataset.view === viewKey) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // Switch workspace view sections
  const viewMap = {
    timer: 'viewFocusStudio',
    overview: 'viewOverview',
    calendar: 'viewCalendar',
    planner: 'viewPlanner'
  };

  Object.entries(viewMap).forEach(([k, id]) => {
    const el = document.getElementById(id);
    if (k === viewKey) {
      el?.classList.remove('hidden');
      el?.classList.add('active');
    } else {
      el?.classList.add('hidden');
      el?.classList.remove('active');
    }
  });

  // Close mobile drawer if open
  closeMobileSidebar();

  // Trigger render of view data
  if (viewKey === 'timer') {
    updateTimerDisplay();
    updateProgressAndStreak();
  } else if (viewKey === 'overview') {
    updateProgressAndStreak();
    renderSubjectDonutChart();
    renderActivityHeatmap();
  } else if (viewKey === 'calendar') {
    renderMonthlyCalendar();
  } else if (viewKey === 'planner') {
    renderPlannerGoals();
  }
}

function switchInsightsTab(tabName) {
  switchWorkspaceView(tabName);
}

// Mobile Sidebar Drawer Controls
function openMobileSidebar() {
  const sidebar = document.getElementById('appSidebar');
  const backdrop = document.getElementById('sidebarBackdrop');
  if (sidebar) {
    sidebar.classList.remove('collapsed');
    sidebar.classList.add('open');
  }
  backdrop?.classList.remove('hidden');
  document.body.style.overflow = 'hidden';
}

function closeMobileSidebar() {
  const sidebar = document.getElementById('appSidebar');
  const backdrop = document.getElementById('sidebarBackdrop');
  sidebar?.classList.remove('open');
  backdrop?.classList.add('hidden');
  document.body.style.overflow = '';
}

function toggleSidebarCollapse() {
  if (window.innerWidth <= 980) {
    // On mobile / small screens, toggle acts as close drawer to prevent double-blur or stuck mini-sidebar
    closeMobileSidebar();
    return;
  }
  const sidebar = document.getElementById('appSidebar');
  if (!sidebar) return;
  const isCollapsed = sidebar.classList.toggle('collapsed');
  try {
    localStorage.setItem('studytimer_sidebar_collapsed', isCollapsed ? 'true' : 'false');
  } catch (_) {}
}

function initSidebarState() {
  try {
    const isCollapsed = localStorage.getItem('studytimer_sidebar_collapsed') === 'true';
    const sidebar = document.getElementById('appSidebar');
    if (isCollapsed && sidebar && window.innerWidth > 980) {
      sidebar.classList.add('collapsed');
    }
  } catch (_) {}
}

function openInsightsDrawer() {
  openMobileSidebar();
}

function closeInsightsDrawer() {
  closeMobileSidebar();
}

// ----------------------------------------------------------------------------
// TAB 1: OVERVIEW (Daily Goal, Donut Chart, 52-Week Heatmap)
// ----------------------------------------------------------------------------

function updateProgressAndStreak() {
  let totalSecToday = 0;
  (appState.todaySessions || []).forEach(s => {
    if (s && typeof s.durationSec === 'number' && s.durationSec > 0) {
      totalSecToday += s.durationSec;
    }
  });

  const totalMinToday = Math.round(totalSecToday / 60);
  const goalMin = Math.max(1, timerConfig.dailyGoalMinutes || 120);
  const goalSec = goalMin * 60;
  const percent = Math.min(100, Math.round((totalSecToday / goalSec) * 100));
  const remainingSec = Math.max(0, goalSec - totalSecToday);
  const remainingMin = Math.ceil(remainingSec / 60);

  const hubStreakCount = document.getElementById('hubStreakCount');
  if (hubStreakCount) hubStreakCount.textContent = `${appState.streakCount || 1} Day Streak`;

  const statStudiedToday = document.getElementById('statStudiedToday');
  const statDailyGoal = document.getElementById('statDailyGoal');
  const statGoalRemaining = document.getElementById('statGoalRemaining');
  const statGoalPercent = document.getElementById('statGoalPercent');
  const largeGoalProgressBar = document.getElementById('largeGoalProgressBar');

  if (statStudiedToday) {
    if (totalSecToday === 0) {
      statStudiedToday.textContent = '0m';
    } else if (totalMinToday >= 60) {
      const h = Math.floor(totalMinToday / 60);
      const m = totalMinToday % 60;
      statStudiedToday.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMinToday === 0 && totalSecToday > 0) {
      statStudiedToday.textContent = `${totalSecToday}s`;
    } else {
      statStudiedToday.textContent = `${totalMinToday}m`;
    }
  }

  if (statDailyGoal) {
    if (goalMin >= 60) {
      const h = Math.floor(goalMin / 60);
      const m = goalMin % 60;
      statDailyGoal.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else {
      statDailyGoal.textContent = `${goalMin}m`;
    }
  }

  if (statGoalRemaining) {
    if (totalSecToday >= goalSec) {
      statGoalRemaining.textContent = 'Goal Met! 🎉';
      statGoalRemaining.style.color = 'var(--accent-emerald)';
    } else {
      if (remainingMin >= 60) {
        const h = Math.floor(remainingMin / 60);
        const m = remainingMin % 60;
        statGoalRemaining.textContent = `${h}h ${m > 0 ? m + 'm' : ''} left`;
      } else {
        statGoalRemaining.textContent = `${remainingMin}m left`;
      }
      statGoalRemaining.style.color = '#f59e0b';
    }
  }

  if (statGoalPercent) statGoalPercent.textContent = `${percent}%`;
  if (largeGoalProgressBar) largeGoalProgressBar.style.width = `${percent}%`;

  // 4-Card Glanceable Status Bar Update
  const metricTodayVal = document.getElementById('metricTodayVal');
  const metricStreakVal = document.getElementById('metricStreakVal');
  const metricGoalVal = document.getElementById('metricGoalVal');
  const metricSubjectVal = document.getElementById('metricSubjectVal');

  if (metricTodayVal) {
    if (totalSecToday === 0) {
      metricTodayVal.textContent = '0m';
    } else if (totalMinToday >= 60) {
      const h = Math.floor(totalMinToday / 60);
      const m = totalMinToday % 60;
      metricTodayVal.textContent = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMinToday === 0 && totalSecToday > 0) {
      metricTodayVal.textContent = `${totalSecToday}s`;
    } else {
      metricTodayVal.textContent = `${totalMinToday}m`;
    }
  }

  if (metricStreakVal) {
    metricStreakVal.textContent = `🔥 ${appState.streakCount || 0} ${appState.streakCount === 1 ? 'Day' : 'Days'}`;
  }

  if (metricGoalVal) {
    metricGoalVal.textContent = `${percent}%`;
  }

  if (metricSubjectVal) {
    const activeSub = (appState.subjects || []).find(s => s.id === appState.selectedSubject?.id) || appState.selectedSubject || DEFAULT_SUBJECTS[0];
    metricSubjectVal.textContent = activeSub ? activeSub.name : 'Mathematics';
  }
}

// Interactive Subject Distribution Donut / Pie Chart (SubjectPieChartView.kt)
// ========================================================
// 1. OVERVIEW TODAY SUBJECT DONUT CHART (Clean Original Design)
// ========================================================
let activeHighlightedSubjectId = null;

function renderSubjectDonutChart() {
  const svg = document.getElementById('subjectDonutSvg');
  const svgWrap = document.getElementById('donutSvgWrap');
  const totalBadge = document.getElementById('donutTotalBadge');
  const centerSub = document.getElementById('donutCenterSub');
  const centerVal = document.getElementById('donutCenterVal');
  const centerPct = document.getElementById('donutCenterPct');
  const legendList = document.getElementById('donutLegendList');
  if (!svg || !legendList) return;

  const todayStr = getLocalDateStr();
  const subjectTotals = getSubjectDistributionForDate(todayStr);

  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) {
      totalSec += item.durationSec;
    }
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (totalBadge) totalBadge.textContent = `${formattedTotal} total`;
  if (centerSub) centerSub.textContent = 'Today';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  activeHighlightedSubjectId = null;

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    // Render dashed placeholder circle
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state">
        <span>No study sessions recorded today yet.</span>
      </div>
    `;
    return;
  }

  // Chart Geometry (Clean Donut Ring)
  const radius = 70;
  const strokeW = 22;
  const circumference = 2 * Math.PI * radius;
  let accumulatedPercent = 0;

  // Background track
  const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
  bgTrack.setAttribute('cx', '100');
  bgTrack.setAttribute('cy', '100');
  bgTrack.setAttribute('r', radius.toString());
  bgTrack.setAttribute('class', 'donut-bg-track');
  bgTrack.setAttribute('stroke-width', strokeW.toString());
  svg.appendChild(bgTrack);

  entries.sort((a, b) => b.durationSec - a.durationSec);

  function highlightSubject(item) {
    activeHighlightedSubjectId = item.id;
    const itemPct = Math.round((item.durationSec / totalSec) * 100);
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    if (centerSub) centerSub.textContent = item.name;
    if (centerVal) centerVal.textContent = itemTimeStr;
    if (centerPct) {
      centerPct.textContent = `${itemPct}% of study`;
      centerPct.classList.remove('hidden');
    }

    svg.querySelectorAll('.donut-slice').forEach(s => {
      if (s.getAttribute('data-sub-id') === item.id) {
        s.classList.add('active');
        s.style.opacity = '1';
        s.style.strokeWidth = (strokeW + 5).toString();
      } else {
        s.classList.remove('active');
        s.style.opacity = '0.35';
        s.style.strokeWidth = strokeW.toString();
      }
    });

    legendList.querySelectorAll('.donut-legend-item').forEach(l => {
      if (l.getAttribute('data-sub-id') === item.id) {
        l.classList.add('active');
      } else {
        l.classList.remove('active');
      }
    });
  }

  function resetHighlight() {
    activeHighlightedSubjectId = null;
    if (centerSub) centerSub.textContent = 'Today';
    if (centerVal) centerVal.textContent = formattedTotal;
    if (centerPct) centerPct.classList.add('hidden');

    svg.querySelectorAll('.donut-slice').forEach(s => {
      s.classList.remove('active');
      s.style.opacity = '1';
      s.style.strokeWidth = strokeW.toString();
    });
    legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
  }

  // Fix C3: remove stale listener before adding new one to prevent stacking on each re-render
  if (svgWrap) {
    if (svgWrap._mouseleaveHandler) svgWrap.removeEventListener('mouseleave', svgWrap._mouseleaveHandler);
    svgWrap._mouseleaveHandler = () => { if (activeHighlightedSubjectId) resetHighlight(); };
    svgWrap.addEventListener('mouseleave', svgWrap._mouseleaveHandler);
  }

  entries.forEach(item => {
    const itemPct = (item.durationSec / totalSec) * 100;
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    const sliceLength = (itemPct / 100) * circumference;
    const offset = -((accumulatedPercent / 100) * circumference);

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', '100');
    circle.setAttribute('cy', '100');
    circle.setAttribute('r', radius.toString());
    circle.setAttribute('class', 'donut-slice');
    circle.setAttribute('data-sub-id', item.id);
    circle.setAttribute('stroke', item.color);
    circle.setAttribute('stroke-width', strokeW.toString());
    circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
    circle.setAttribute('stroke-dashoffset', offset.toString());
    circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

    circle.addEventListener('mouseenter', () => highlightSubject(item));
    circle.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightSubject(item);
    });

    svg.appendChild(circle);
    accumulatedPercent += itemPct;

    const legendItem = document.createElement('div');
    legendItem.className = 'donut-legend-item';
    legendItem.setAttribute('data-sub-id', item.id);
    legendItem.innerHTML = `
      <div class="donut-legend-left">
        <span class="donut-legend-dot" style="background-color: ${item.color};"></span>
        <span class="donut-legend-name">${item.name}</span>
      </div>
      <div class="donut-legend-right">
        <span class="donut-legend-pct">${Math.round(itemPct)}%</span>
        <span class="donut-legend-time">${itemTimeStr}</span>
      </div>
    `;

    legendItem.addEventListener('mouseenter', () => highlightSubject(item));
    legendItem.addEventListener('mouseleave', () => resetHighlight());
    legendItem.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightSubject(item);
    });

    legendList.appendChild(legendItem);
  });
}

// ========================================================
// 2. HISTORICAL PAST DAY SUBJECT DISTRIBUTION & PIE CHART
// (Matches Android CalendarTimeline.kt & Day Details Modal)
// ========================================================
function getSubjectDistributionForDate(dateStr) {
  const subjectTotals = {};
  const todayStr = getLocalDateStr();
  const allSessions = getAllValidatedSessions();

  // 1. Sessions for this specific date
  allSessions.forEach(s => {
    if (!s || !s.durationSec || s.durationSec <= 0) return;
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
    if (dStr === dateStr) {
      const subId = s.subject?.id || s.subject?.name || 'general';
      if (!subjectTotals[subId]) {
        const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
          id: subId,
          name: s.subject?.name || 'Focus Study',
          color: s.subject?.color || '#3b82f6'
        };
        subjectTotals[subId] = {
          id: subId,
          name: matching.name,
          color: matching.color || matching.colorHex || '#3b82f6',
          durationSec: 0
        };
      }
      subjectTotals[subId].durationSec += s.durationSec;
    }
  });

  // 2. If it is today and todaySessions has data, merge
  if (dateStr === todayStr && appState.todaySessions && appState.todaySessions.length > 0) {
    appState.todaySessions.forEach(s => {
      if (s && s.durationSec > 0) {
        const subId = s.subject?.id || s.subject?.name || 'general';
        if (!subjectTotals[subId]) {
          const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
            id: subId,
            name: s.subject?.name || 'Focus Study',
            color: s.subject?.color || '#3b82f6'
          };
          subjectTotals[subId] = {
            id: subId,
            name: matching.name,
            color: matching.color || matching.colorHex || '#3b82f6',
            durationSec: 0
          };
        }
        if (!allSessions.some(as => as.id === s.id)) {
          subjectTotals[subId].durationSec += s.durationSec;
        }
      }
    });
  }

  // 3. Check dailySubjectDurations for this date from Android Cloud sync
  if (appState.dailySubjectDurations && appState.dailySubjectDurations[dateStr]) {
    const dayBreakdown = appState.dailySubjectDurations[dateStr];
    if (dayBreakdown && typeof dayBreakdown === 'object') {
      Object.keys(dayBreakdown).forEach(subId => {
        const sec = Number(dayBreakdown[subId]) || 0;
        if (sec > 0) {
          if (!subjectTotals[subId]) {
            const matching = (appState.subjects || []).find(sub => sub.id === subId) || {
              id: subId,
              name: subId.startsWith('custom_') ? 'Subject' : subId.charAt(0).toUpperCase() + subId.slice(1),
              color: '#3b82f6'
            };
            subjectTotals[subId] = {
              id: subId,
              name: matching.name,
              color: matching.color || matching.colorHex || '#3b82f6',
              durationSec: 0
            };
          }
          subjectTotals[subId].durationSec = Math.max(subjectTotals[subId].durationSec, sec);
        }
      });
    }
  }

  // Harmonize names & colors with active user registered subjects
  Object.values(subjectTotals).forEach(item => {
    const matching = (appState.subjects || []).find(sub => sub.id === item.id || sub.name?.toLowerCase() === item.name?.toLowerCase());
    if (matching) {
      item.name = matching.name;
      item.color = matching.color || matching.colorHex || item.color;
    }
  });

  return subjectTotals;
}

function renderCalendarDayPieChart(dateStr) {
  const svg = document.getElementById('calDaySubjectDonutSvg');
  const svgWrap = document.getElementById('calDayDonutSvgWrap');
  const totalBadge = document.getElementById('calDayPieTotalBadge');
  const title = document.getElementById('calDayPieTitle');
  const centerSub = document.getElementById('calDayDonutCenterSub');
  const centerVal = document.getElementById('calDayDonutCenterVal');
  const centerPct = document.getElementById('calDayDonutCenterPct');
  const legendList = document.getElementById('calDayDonutLegendList');
  if (!svg || !legendList) return;

  const todayStr = getLocalDateStr();
  const isToday = dateStr === todayStr;
  const formattedDate = new Date(dateStr + 'T00:00:00').toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? "Today's Subject Breakdown" : `Subject Breakdown (${formattedDate})`;
  }

  const subjectTotals = getSubjectDistributionForDate(dateStr);
  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) totalSec += item.durationSec;
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (totalBadge) totalBadge.textContent = `${formattedTotal} total`;
  if (centerSub) centerSub.textContent = 'Studied';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state">
        <span>No study sessions recorded on ${formattedDate}.</span>
      </div>
    `;
    return;
  }

  const radius = 70;
  const strokeW = 22;
  const circumference = 2 * Math.PI * radius;
  let accumulatedPercent = 0;

  const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
  bgTrack.setAttribute('cx', '100');
  bgTrack.setAttribute('cy', '100');
  bgTrack.setAttribute('r', radius.toString());
  bgTrack.setAttribute('class', 'donut-bg-track');
  bgTrack.setAttribute('stroke-width', strokeW.toString());
  svg.appendChild(bgTrack);

  entries.sort((a, b) => b.durationSec - a.durationSec);

  function highlightDaySubject(item) {
    const itemPct = Math.round((item.durationSec / totalSec) * 100);
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    if (centerSub) centerSub.textContent = item.name;
    if (centerVal) centerVal.textContent = itemTimeStr;
    if (centerPct) {
      centerPct.textContent = `${itemPct}% of day`;
      centerPct.classList.remove('hidden');
    }

    svg.querySelectorAll('.donut-slice').forEach(s => {
      if (s.getAttribute('data-sub-id') === item.id) {
        s.classList.add('active');
        s.style.opacity = '1';
        s.style.strokeWidth = (strokeW + 5).toString();
      } else {
        s.classList.remove('active');
        s.style.opacity = '0.35';
        s.style.strokeWidth = strokeW.toString();
      }
    });

    legendList.querySelectorAll('.donut-legend-item').forEach(l => {
      if (l.getAttribute('data-sub-id') === item.id) {
        l.classList.add('active');
      } else {
        l.classList.remove('active');
      }
    });
  }

  function resetDayHighlight() {
    if (centerSub) centerSub.textContent = 'Studied';
    if (centerVal) centerVal.textContent = formattedTotal;
    if (centerPct) centerPct.classList.add('hidden');

    svg.querySelectorAll('.donut-slice').forEach(s => {
      s.classList.remove('active');
      s.style.opacity = '1';
      s.style.strokeWidth = strokeW.toString();
    });
    legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
  }

  // Fix M9: remove stale listener before adding new one to prevent stacking on each calendar re-render
  if (svgWrap) {
    if (svgWrap._dayMouseleaveHandler) svgWrap.removeEventListener('mouseleave', svgWrap._dayMouseleaveHandler);
    svgWrap._dayMouseleaveHandler = resetDayHighlight;
    svgWrap.addEventListener('mouseleave', svgWrap._dayMouseleaveHandler);
  }

  entries.forEach(item => {
    const itemPct = (item.durationSec / totalSec) * 100;
    const itemMin = Math.round(item.durationSec / 60);
    const itemTimeStr = itemMin >= 60 
      ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
      : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

    const sliceLength = (itemPct / 100) * circumference;
    const offset = -((accumulatedPercent / 100) * circumference);

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', '100');
    circle.setAttribute('cy', '100');
    circle.setAttribute('r', radius.toString());
    circle.setAttribute('class', 'donut-slice');
    circle.setAttribute('data-sub-id', item.id);
    circle.setAttribute('stroke', item.color);
    circle.setAttribute('stroke-width', strokeW.toString());
    circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
    circle.setAttribute('stroke-dashoffset', offset.toString());
    circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

    circle.addEventListener('mouseenter', () => highlightDaySubject(item));
    circle.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightDaySubject(item);
    });

    svg.appendChild(circle);
    accumulatedPercent += itemPct;

    const legendItem = document.createElement('div');
    legendItem.className = 'donut-legend-item';
    legendItem.setAttribute('data-sub-id', item.id);
    legendItem.innerHTML = `
      <div class="donut-legend-left">
        <span class="donut-legend-dot" style="background-color: ${item.color};"></span>
        <span class="donut-legend-name">${item.name}</span>
      </div>
      <div class="donut-legend-right">
        <span class="donut-legend-pct">${Math.round(itemPct)}%</span>
        <span class="donut-legend-time">${itemTimeStr}</span>
      </div>
    `;

    legendItem.addEventListener('mouseenter', () => highlightDaySubject(item));
    legendItem.addEventListener('mouseleave', resetDayHighlight);
    legendItem.addEventListener('click', (e) => {
      e.stopPropagation();
      highlightDaySubject(item);
    });

    legendList.appendChild(legendItem);
  });
}

// 52-Week Activity Heatmap (HeatmapView.kt)
function renderActivityHeatmap() {
  const container = document.getElementById('activityHeatmapGrid') || document.getElementById('heatmapGrid');
  const monthsRow = document.getElementById('heatmapMonthsRow');
  const yearLabel = document.getElementById('heatmapYearLabel');
  if (!container) return;

  const dateMap = {};
  let totalActiveDays = 0;
  let totalStudiedSec = 0;

  const allSessions = getAllValidatedSessions();

  allSessions.forEach(sess => {
    if (sess && sess.durationSec > 0) {
      const d = new Date(sess.timestamp || sess.startTime);
      const dateKey = getLocalDateStr(d);
      dateMap[dateKey] = (dateMap[dateKey] || 0) + sess.durationSec;
    }
  });

  Object.values(dateMap).forEach(sec => {
    if (sec > 0) {
      totalActiveDays++;
      totalStudiedSec += sec;
    }
  });

  if (yearLabel) {
    const totalHrs = Math.floor(totalStudiedSec / 3600);
    const totalMins = Math.round((totalStudiedSec % 3600) / 60);
    const formattedTotal = totalHrs > 0 ? `${totalHrs}h ${totalMins}m` : `${totalMins}m`;
    yearLabel.textContent = `${totalActiveDays} Active Day${totalActiveDays === 1 ? '' : 's'}${totalStudiedSec > 0 ? ' • ' + formattedTotal : ''}`;
  }

  const now = new Date();
  const currentDayOfWeek = (now.getDay() + 6) % 7; // Monday = 0, Sunday = 6
  const totalDays = (51 * 7) + (currentDayOfWeek + 1); // 52 weeks total

  container.innerHTML = '';
  if (monthsRow) monthsRow.innerHTML = '';

  const startDate = new Date();
  startDate.setDate(now.getDate() - totalDays + 1);

  const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  let lastMonthIndex = -1;
  let currentWeekCol = null;
  let colIndex = 0;

  for (let i = 0; i < totalDays; i++) {
    const curDate = new Date(startDate);
    curDate.setDate(startDate.getDate() + i);

    const dayOfWeek = (curDate.getDay() + 6) % 7; // 0 = Mon, 6 = Sun
    if (dayOfWeek === 0 || !currentWeekCol) {
      currentWeekCol = document.createElement('div');
      currentWeekCol.className = 'heatmap-col-week';
      container.appendChild(currentWeekCol);

      const curMonth = curDate.getMonth();
      if (curMonth !== lastMonthIndex && monthsRow) {
        lastMonthIndex = curMonth;
        const monthLabel = document.createElement('span');
        monthLabel.className = 'heatmap-month-label';
        monthLabel.style.left = `${colIndex * 13}px`;
        monthLabel.textContent = monthNames[curMonth];
        monthsRow.appendChild(monthLabel);
      }

      colIndex++;
    }

    const dateKey = getLocalDateStr(curDate);
    const secStudied = dateMap[dateKey] || 0;
    const minStudied = Math.round(secStudied / 60);

    let level = 0;
    if (minStudied > 0 && minStudied < 30) level = 1;
    else if (minStudied >= 30 && minStudied < 60) level = 2;
    else if (minStudied >= 60 && minStudied < 120) level = 3;
    else if (minStudied >= 120) level = 4;

    const cell = document.createElement('div');
    cell.className = `heatmap-cell lvl-${level}`;
    const formattedDate = curDate.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
    const formattedTime = minStudied >= 60 ? `${Math.floor(minStudied / 60)}h ${minStudied % 60}m` : `${minStudied}m`;
    cell.title = `${minStudied > 0 ? formattedTime : 'No study'} on ${formattedDate}`;

    cell.addEventListener('click', () => {
      switchInsightsTab('calendar');
      selectCalendarDate(dateKey);
    });

    currentWeekCol.appendChild(cell);
  }

  const scrollWrapper = document.querySelector('.heatmap-scroll-wrapper');
  if (scrollWrapper) {
    scrollWrapper.scrollLeft = scrollWrapper.scrollWidth;
  }
}

// ----------------------------------------------------------------------------
// TAB 2: CALENDAR & DAY TIMELINE (CalendarTimeline.kt)
// ----------------------------------------------------------------------------

let activeCalendarMonth = new Date().getMonth();
let activeCalendarYear = new Date().getFullYear();
let selectedCalendarDateStr = getLocalDateStr();

function renderMonthlyCalendar() {
  const monthTitle = document.getElementById('calendarMonthYear') || document.getElementById('calMonthTitle');
  const daysGrid = document.getElementById('calendarDaysGrid') || document.getElementById('calDaysGrid');
  const summaryRow = document.getElementById('calMonthSummaryRow');
  const goalsMetChip = document.getElementById('calMonthGoalsCount');
  const totalHoursChip = document.getElementById('calMonthTotalHours');
  if (!monthTitle || !daysGrid) return;

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  monthTitle.textContent = `${monthNames[activeCalendarMonth]} ${activeCalendarYear}`;
  daysGrid.innerHTML = '';

  const firstDayOfMonth = new Date(activeCalendarYear, activeCalendarMonth, 1);
  const lastDayOfMonth = new Date(activeCalendarYear, activeCalendarMonth + 1, 0);
  const daysInMonth = lastDayOfMonth.getDate();

  const startDayIndex = (firstDayOfMonth.getDay() + 6) % 7;
  const prevMonthLastDay = new Date(activeCalendarYear, activeCalendarMonth, 0).getDate();
  for (let i = startDayIndex - 1; i >= 0; i--) {
    const cell = document.createElement('div');
    cell.className = 'cal-day-cell other-month';
    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <span class="cal-day-num">${prevMonthLastDay - i}</span>
      </div>
    `;
    daysGrid.appendChild(cell);
  }

  const todayStr = getLocalDateStr();

  const dateSessionsMap = {};
  const dateDurationMap = {};

  const allSessions = getAllValidatedSessions();
  allSessions.forEach(s => {
    if (s && s.durationSec > 0) {
      const d = new Date(s.timestamp || s.startTime);
      const dateKey = getLocalDateStr(d);
      if (!dateSessionsMap[dateKey]) dateSessionsMap[dateKey] = [];
      dateSessionsMap[dateKey].push(s.subject?.color || '#3b82f6');
      dateDurationMap[dateKey] = (dateDurationMap[dateKey] || 0) + s.durationSec;
    }
  });

  const dailyGoalSec = (timerConfig.dailyGoalMinutes || 120) * 60;
  let monthGoalsMet = 0;
  let monthTotalSecs = 0;

  for (let day = 1; day <= daysInMonth; day++) {
    const dateKey = `${activeCalendarYear}-${String(activeCalendarMonth + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    
    const isToday = dateKey === todayStr;
    const isSelected = dateKey === selectedCalendarDateStr;
    const subjectColors = [...new Set(dateSessionsMap[dateKey] || [])];
    const focusSecs = dateDurationMap[dateKey] || 0;

    monthTotalSecs += focusSecs;
    const goalReached = dailyGoalSec > 0 && focusSecs >= dailyGoalSec;
    const goalIncomplete = focusSecs > 0 && !goalReached;
    if (goalReached) monthGoalsMet++;

    let goalStatusClass = '';
    if (goalReached) {
      goalStatusClass = 'goal-met';
    } else if (goalIncomplete) {
      goalStatusClass = 'goal-incomplete';
    }

    const pct = dailyGoalSec > 0 ? Math.min(1, focusSecs / dailyGoalSec) : 0;
    const ringCircumference = 75.4; // 2 * PI * 12
    const ringOffset = ringCircumference * (1 - pct);

    const cell = document.createElement('div');
    cell.className = `cal-day-cell ${isToday ? 'today' : ''} ${isSelected ? 'selected' : ''} ${goalStatusClass}`;
    
    const minStudied = Math.round(focusSecs / 60);
    const goalMin = Math.round(dailyGoalSec / 60);
    const leftMin = Math.max(0, goalMin - minStudied);
    const formattedDate = new Date(dateKey + 'T00:00:00').toLocaleDateString([], { month: 'short', day: 'numeric' });
    cell.title = focusSecs > 0 
      ? `${formattedDate}: ${minStudied}m / ${goalMin}m goal (${Math.round(pct * 100)}%${leftMin > 0 ? ' • ' + leftMin + 'm left' : ' • Goal Reached!'})` 
      : `${formattedDate}: No study sessions`;

    let dotsHtml = '';
    if (subjectColors.length > 0) {
      dotsHtml = `<div class="cal-study-dots-wrap">` + 
        subjectColors.slice(0, 3).map(color => `<span class="cal-dot" style="background-color: ${color};"></span>`).join('') +
        `</div>`;
    }

    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <svg class="cal-day-svg-ring" viewBox="0 0 30 30">
          <circle class="cal-ring-bg" cx="15" cy="15" r="12"></circle>
          ${pct > 0 ? `<circle class="cal-ring-fg" cx="15" cy="15" r="12" stroke-dasharray="${ringCircumference}" stroke-dashoffset="${ringOffset}"></circle>` : ''}
        </svg>
        <span class="cal-day-num">${day}</span>
      </div>
      ${dotsHtml}
    `;

    cell.addEventListener('click', () => {
      selectCalendarDate(dateKey);
    });

    daysGrid.appendChild(cell);
  }

  // Next month padding to complete grid
  const totalCellsSoFar = startDayIndex + daysInMonth;
  const remainingCells = (totalCellsSoFar <= 35 ? 35 : 42) - totalCellsSoFar;
  for (let d = 1; d <= remainingCells; d++) {
    const cell = document.createElement('div');
    cell.className = 'cal-day-cell other-month';
    cell.innerHTML = `
      <div class="cal-day-ring-wrap">
        <span class="cal-day-num">${d}</span>
      </div>
    `;
    daysGrid.appendChild(cell);
  }

  const totalHrs = Math.floor(monthTotalSecs / 3600);
  const totalMins = Math.round((monthTotalSecs % 3600) / 60);
  const formattedMonthStudy = totalHrs > 0 ? `${totalHrs}h ${totalMins}m` : `${totalMins}m`;

  if (goalsMetChip) goalsMetChip.textContent = `✓ ${monthGoalsMet} ${monthGoalsMet === 1 ? 'Goal' : 'Goals'} Met`;
  if (totalHoursChip) totalHoursChip.textContent = `⏱ ${formattedMonthStudy} Total Study`;
  if (summaryRow && !goalsMetChip && !totalHoursChip) {
    summaryRow.innerHTML = `
      <span class="cal-summary-chip goals-met chip-goals">✓ ${monthGoalsMet} ${monthGoalsMet === 1 ? 'Goal' : 'Goals'} Met</span>
      <span class="cal-summary-chip total-study chip-hours">⏱ ${formattedMonthStudy} Total Study</span>
    `;
  }

  renderSelectedDateTimeline(selectedCalendarDateStr);
}

function changeCalendarMonth(delta) {
  activeCalendarMonth += delta;
  if (activeCalendarMonth > 11) {
    activeCalendarMonth = 0;
    activeCalendarYear++;
  } else if (activeCalendarMonth < 0) {
    activeCalendarMonth = 11;
    activeCalendarYear--;
  }
  renderMonthlyCalendar();
}

function selectCalendarDate(dateStr) {
  selectedCalendarDateStr = dateStr;
  renderMonthlyCalendar();
}

// Precise Session Timeline with exact Start & End Time
function renderSelectedDateTimeline(dateStr) {
  const title = document.getElementById('timelineDateHeading') || document.getElementById('selectedDateTimelineTitle');
  const countBadge = document.getElementById('timelineSessionCount') || document.getElementById('selectedDateSessionCount');
  const container = document.getElementById('timelineList');
  if (!container) return;

  const todayStr = getLocalDateStr();
  const isToday = dateStr === todayStr;

  const formattedDate = new Date(dateStr + 'T00:00:00').toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? "Today's Sessions" : `Sessions (${formattedDate})`;
  }

  const allSessions = getAllValidatedSessions();
  const sessions = allSessions.filter(s => {
    if (!s || !s.durationSec) return false;
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
    return dStr === dateStr;
  });

  if (countBadge) countBadge.textContent = `${sessions.length} session${sessions.length === 1 ? '' : 's'}`;

  if (sessions.length === 0) {
    container.innerHTML = `
      <div class="timeline-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p>No study sessions on ${formattedDate}.</p>
        <span>${isToday ? 'Click Start Focus to log your first session!' : 'Select a date with activity dots to view history.'}</span>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  sessions.forEach(sess => {
    const mins = Math.max(1, Math.round(sess.durationSec / 60));
    const startTimeFormatted = new Date(sess.startTime || sess.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const endTimeFormatted = sess.endTime 
      ? new Date(sess.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : new Date((sess.startTime || sess.timestamp) + (sess.durationSec * 1000)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const item = document.createElement('div');
    item.className = 'timeline-item';
    item.innerHTML = `
      <div class="timeline-item-left">
        <span class="subject-color-dot" style="background-color: ${sess.subject?.color || '#3b82f6'};"></span>
        <div>
          <div class="timeline-subject-name">${sess.subject?.name || 'Focus Study'}</div>
          <span class="timeline-mode-pill">${sess.mode || 'Focus Study'}</span>
        </div>
      </div>
      <div class="timeline-item-right timeline-time-meta">
        <span class="timeline-duration">+${mins} min</span>
        <div class="timeline-time-range">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <polyline points="12 6 12 12 16 14"></polyline>
          </svg>
          <span>${startTimeFormatted} – ${endTimeFormatted}</span>
        </div>
      </div>
    `;
    container.appendChild(item);
  });
}

// ----------------------------------------------------------------------------
// TAB 3: PLANNER & GOALS (PlannerHistoryManager.kt)
// ----------------------------------------------------------------------------

function renderPlannerGoals() {
  const container = document.getElementById('plannerGoalsContainer');
  if (!container) return;

  const goals = appState.plannerGoals || [];

  // Update Planner Quick Stats Bar
  const totalGoalsEl = document.getElementById('plannerTotalGoalsCount');
  const completedGoalsEl = document.getElementById('plannerCompletedGoalsCount');
  const totalPlannedTimeEl = document.getElementById('plannerTotalPlannedTime');

  let totalPlannedMins = 0;
  let completedCount = 0;

  goals.forEach(g => {
    totalPlannedMins += (g.targetMinutes || g.dailyMinutes || 0);
    if (g.completed) completedCount++;
  });

  if (totalGoalsEl) totalGoalsEl.textContent = String(goals.length);
  if (completedGoalsEl) completedGoalsEl.textContent = String(completedCount);
  if (totalPlannedTimeEl) {
    const h = Math.floor(totalPlannedMins / 60);
    const m = totalPlannedMins % 60;
    totalPlannedTimeEl.textContent = h > 0 ? `${h}h${m > 0 ? ` ${m}m` : ''}` : `${m}m`;
  }

  if (goals.length === 0) {
    container.innerHTML = `
      <div class="planner-empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
        <p>No study goals set for today yet.</p>
        <span>Click <strong>+ New Goal</strong> above to set tasks, time targets &amp; daily habits!</span>
      </div>
    `;
    return;
  }

  // Calculate today's studied seconds per subject
  const studiedSecMap = {};
  (appState.todaySessions || []).forEach(s => {
    if (s && s.subject && typeof s.durationSec === 'number' && s.durationSec > 0) {
      const subId = s.subject.id || s.subject.name;
      studiedSecMap[subId] = (studiedSecMap[subId] || 0) + s.durationSec;
    }
  });

  container.innerHTML = '';
  goals.forEach(goal => {
    const subId = goal.subjectId;
    const subject = (appState.subjects || []).find(s => s.id === subId) || {
      id: 'all',
      name: 'All Subjects',
      color: '#3b82f6',
      iconEmoji: '📚'
    };

    const targetMin = Math.max(0, Number(goal.targetMinutes ?? goal.dailyMinutes) || 0);
    const targetSec = targetMin * 60;
    const studiedSec = subId ? (studiedSecMap[subId] || 0) : 0;
    const studiedMin = Math.round(studiedSec / 60);

    const isTimeGoal = targetMin > 0;
    const percent = isTimeGoal ? Math.min(100, Math.round((studiedSec / targetSec) * 100)) : (goal.completed ? 100 : 0);
    const isCompleted = goal.completed || (isTimeGoal && studiedSec >= targetSec);

    let progressChipText = '';
    if (isTimeGoal) {
      progressChipText = `${studiedMin}m / ${targetMin}m`;
    } else {
      progressChipText = 'Daily Habit';
    }

    const card = document.createElement('div');
    card.className = `planner-goal-card ${goal.completed ? 'is-completed' : ''}`;
    card.setAttribute('data-goal-id', goal.id);

    card.innerHTML = `
      <div class="goal-card-main-row">
        <!-- Animated Circle Checkbox -->
        <button class="goal-checkbox-btn ${goal.completed ? 'checked' : ''}" data-action="toggle-check" title="${goal.completed ? 'Mark as incomplete' : 'Mark as complete'}">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><polyline points="20 6 9 17 4 12"></polyline></svg>
        </button>

        <!-- Goal Content Column -->
        <div class="goal-content-col">
          <div class="goal-title-wrap">
            <h4 class="goal-title ${goal.completed ? 'is-completed' : ''}">${escapeHtml(goal.title || (subject ? `${subject.name} Goal` : 'Study Goal'))}</h4>
          </div>
          ${goal.note ? `<p class="goal-note">${escapeHtml(goal.note)}</p>` : ''}
          <div class="goal-badges-row">
            <span class="goal-subject-chip" style="background-color: rgba(59, 130, 246, 0.08); color: ${subject.color || '#3b82f6'}; border-color: ${subject.color ? subject.color + '40' : 'rgba(59, 130, 246, 0.2)'};">
              <span class="subject-color-dot" style="background-color: ${subject.color || '#3b82f6'}; width: 7px; height: 7px;"></span>
              <span>${escapeHtml(subject.name)}</span>
            </span>
            <span class="goal-progress-chip ${isCompleted ? 'completed' : ''}">${isCompleted ? '✓ Done' : progressChipText}</span>
          </div>
        </div>

        <!-- Action Buttons (Edit & Delete) -->
        <div class="goal-card-actions">
          <button class="goal-btn-action btn-edit" data-action="edit-goal" title="Edit Goal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
            </svg>
          </button>
          <button class="goal-btn-action btn-delete" data-action="delete-goal" title="Remove Goal">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>

      ${isTimeGoal ? `
        <div class="goal-progress-track">
          <div class="goal-progress-bar" style="width: ${percent}%; background: linear-gradient(90deg, ${subject.color || '#3b82f6'}, #10b981);"></div>
        </div>
      ` : ''}
    `;

    // Event Handlers for the card
    card.querySelector('[data-action="toggle-check"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleGoalCompleted(goal.id);
    });

    card.querySelector('[data-action="edit-goal"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      openEditGoalModal(goal.id);
    });

    card.querySelector('[data-action="delete-goal"]')?.addEventListener('click', (e) => {
      e.stopPropagation();
      openDeleteGoalModal(goal.id);
    });

    container.appendChild(card);
  });
}

function toggleGoalCompleted(goalId) {
  const goal = (appState.plannerGoals || []).find(g => g.id === goalId);
  if (!goal) return;

  goal.completed = !goal.completed;
  goal.checkedAt = goal.completed ? Date.now() : 0;

  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast(goal.completed ? `Completed: "${goal.title}"` : `Marked incomplete: "${goal.title}"`, 'info');
}

function openAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  const titleEl = document.getElementById('plannerGoalModalTitle');
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const minutesInput = document.getElementById('goalMinutesInput');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const btnSave = document.getElementById('btnSaveGoalModal');

  if (titleEl) titleEl.textContent = 'Add Study Goal';
  if (btnSave) btnSave.querySelector('span').textContent = 'Save Goal';
  if (idInput) idInput.value = '';
  if (titleInput) titleInput.value = '';
  if (noteInput) noteInput.value = '';
  if (minutesInput) minutesInput.value = '60';

  if (subjectSelect) {
    subjectSelect.innerHTML = `
      <option value="all">All Subjects / General</option>
      ${(appState.subjects || []).map(s => `<option value="${s.id}" ${s.id === appState.selectedSubject?.id ? 'selected' : ''}>${s.name}</option>`).join('')}
    `;
  }

  // Set 60m active in presets
  document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === '60');
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    setTimeout(() => titleInput?.focus(), 50);
  }
}

function openEditGoalModal(goalId) {
  const goal = (appState.plannerGoals || []).find(g => g.id === goalId);
  if (!goal) return;

  const modal = document.getElementById('plannerGoalModalOverlay');
  const titleEl = document.getElementById('plannerGoalModalTitle');
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const minutesInput = document.getElementById('goalMinutesInput');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const btnSave = document.getElementById('btnSaveGoalModal');

  if (titleEl) titleEl.textContent = 'Edit Study Goal';
  if (btnSave) btnSave.querySelector('span').textContent = 'Update Goal';
  if (idInput) idInput.value = goal.id;
  if (titleInput) titleInput.value = goal.title || '';
  if (noteInput) noteInput.value = goal.note || '';
  
  const targetMins = Number(goal.targetMinutes ?? goal.dailyMinutes) || 0;
  if (minutesInput) minutesInput.value = String(targetMins);

  if (subjectSelect) {
    subjectSelect.innerHTML = `
      <option value="all" ${!goal.subjectId || goal.subjectId === 'all' ? 'selected' : ''}>All Subjects / General</option>
      ${(appState.subjects || []).map(s => `<option value="${s.id}" ${s.id === goal.subjectId ? 'selected' : ''}>${s.name}</option>`).join('')}
    `;
  }

  // Highlight matching preset if any
  document.querySelectorAll('#goalDurationPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === String(targetMins));
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
    setTimeout(() => titleInput?.focus(), 50);
  }
}

function closeAddGoalModal() {
  const modal = document.getElementById('plannerGoalModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function handleSaveGoal(e) {
  e.preventDefault();
  const idInput = document.getElementById('editingGoalId');
  const titleInput = document.getElementById('inputGoalTitle');
  const noteInput = document.getElementById('inputGoalNote');
  const subjectSelect = document.getElementById('goalSubjectSelect');
  const minutesInput = document.getElementById('goalMinutesInput');

  const editingId = idInput?.value?.trim();
  const title = titleInput?.value?.trim() || 'Daily Goal';
  const note = noteInput?.value?.trim() || '';
  const subjectId = (subjectSelect?.value && subjectSelect.value !== 'all') ? subjectSelect.value : null;
  const targetMinutes = Math.min(1440, Math.max(0, parseInt(minutesInput?.value, 10) || 0));

  if (hasProfanity(title) || hasProfanity(note)) {
    showToast('Please keep study goals and notes respectful & friendly 🛡️', 'error');
    return;
  }

  if (!appState.plannerGoals) appState.plannerGoals = [];

  if (editingId) {
    const existing = appState.plannerGoals.find(g => g.id === editingId);
    if (existing) {
      existing.title = title;
      existing.note = note;
      existing.subjectId = subjectId;
      existing.targetMinutes = targetMinutes;
      existing.dailyMinutes = targetMinutes;
    }
  } else {
    appState.plannerGoals.push({
      id: 'goal_' + Date.now(),
      title,
      note,
      subjectId,
      targetMinutes,
      dailyMinutes: targetMinutes,
      completed: false,
      checkedAt: 0,
      createdAt: Date.now()
    });
  }

  closeAddGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast(editingId ? 'Goal updated & synced with app!' : 'New study goal created & synced!', 'success');
}

function openDeleteGoalModal(goalId) {
  pendingGoalIdToDelete = goalId;
  const goal = appState.plannerGoals?.find(g => g.id === goalId);
  const subtitle = document.getElementById('deleteGoalModalSubtitle');
  if (subtitle) {
    subtitle.textContent = `Are you sure you want to remove "${goal ? (goal.title || 'this goal') : 'this goal'}"?`;
  }
  lockBodyScroll();
  document.getElementById('deleteGoalModalOverlay')?.classList.remove('hidden');
}

function closeDeleteGoalModal() {
  pendingGoalIdToDelete = null;
  document.getElementById('deleteGoalModalOverlay')?.classList.add('hidden');
  unlockBodyScroll();
}

function confirmDeletePlannerGoal() {
  if (!pendingGoalIdToDelete || !appState.plannerGoals) return;
  appState.plannerGoals = appState.plannerGoals.filter(g => g.id !== pendingGoalIdToDelete);
  closeDeleteGoalModal();
  renderPlannerGoals();
  saveLocalState();
  pushDataToCloud();
  showToast('Study goal removed and synced.', 'info');
}

// ----------------------------------------------------------------------------
// EDIT OVERALL DAILY FOCUS GOAL MODAL
// ----------------------------------------------------------------------------

function openEditDailyGoalModal() {
  const modal = document.getElementById('dailyGoalModalOverlay');
  const input = document.getElementById('inputDailyGoalMinutes');
  const currentMins = timerConfig.dailyGoalMinutes || 120;

  if (input) input.value = String(currentMins);

  document.querySelectorAll('#dailyTargetPresetsRow .preset-chip-btn').forEach(b => {
    b.classList.toggle('active', b.dataset.mins === String(currentMins));
  });

  if (modal) {
    lockBodyScroll();
    modal.classList.remove('hidden');
  }
}

function closeDailyGoalModal() {
  const modal = document.getElementById('dailyGoalModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function handleSaveDailyGoal(e) {
  e.preventDefault();
  const input = document.getElementById('inputDailyGoalMinutes');
  const mins = Math.min(1440, Math.max(15, parseInt(input?.value, 10) || 120));

  timerConfig.dailyGoalMinutes = mins;
  closeDailyGoalModal();
  updateProgressAndStreak();
  saveLocalState();
  pushDataToCloud();
  showToast(`Daily study target updated to ${mins >= 60 ? `${Math.floor(mins / 60)}h ${mins % 60 > 0 ? (mins % 60) + 'm' : ''}` : `${mins}m`}!`, 'success');
}


// ----------------------------------------------------------------------------
// TAB 4: LIVE LEADERBOARD & PROFILE CUSTOMIZATION
// ----------------------------------------------------------------------------

let presenceHeartbeatInterval = null;
let leaderboardCache = { data: null, timestamp: 0 };
const LEADERBOARD_CACHE_TTL_MS = 60000; // 60-second client cache to conserve database quota
let resetCountdownInterval = null;

// Profile Customization State
let selectedAvatarPreset = '';
let selectedAvatarRing = 'glow-gold';
let selectedBannerTheme = 'banner-midnight';
let selectedCountryFlag = '🌐';

// ============================================================================

// DAY-BY-DAY SUBJECT PIE CHART & TIMELINE HISTORY MODAL
// (Matches Android showPieChartDetailsModal in MainActivity.kt)
// ============================================================================
let activeHistoryModalDateStr = getLocalDateStr();

function openDayPieHistoryModal(dateStr) {
  const modal = document.getElementById('dayPieHistoryModalOverlay');
  if (!modal) return;
  activeHistoryModalDateStr = dateStr || selectedCalendarDateStr || getLocalDateStr();
  lockBodyScroll();
  modal.classList.remove('hidden');
  renderDayPieHistoryModal(activeHistoryModalDateStr);
}

function closeDayPieHistoryModal() {
  const modal = document.getElementById('dayPieHistoryModalOverlay');
  if (modal) modal.classList.add('hidden');
  unlockBodyScroll();
}

function changeHistoryModalDay(delta) {
  const current = new Date(activeHistoryModalDateStr + 'T00:00:00');
  current.setDate(current.getDate() + delta);
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Do not allow navigating into the future
  if (current.getTime() > today.getTime()) return;

  const y = current.getFullYear();
  const m = String(current.getMonth() + 1).padStart(2, '0');
  const d = String(current.getDate()).padStart(2, '0');
  activeHistoryModalDateStr = `${y}-${m}-${d}`;
  renderDayPieHistoryModal(activeHistoryModalDateStr);
}

function renderDayPieHistoryModal(dateStr) {
  const title = document.getElementById('historyModalDateTitle');
  const nextBtn = document.getElementById('btnHistoryNextDay');
  const svg = document.getElementById('historySubjectDonutSvg');
  const svgWrap = document.getElementById('historyDonutSvgWrap');
  const centerSub = document.getElementById('historyDonutCenterSub');
  const centerVal = document.getElementById('historyDonutCenterVal');
  const centerPct = document.getElementById('historyDonutCenterPct');
  const legendList = document.getElementById('historyDonutLegendList');
  const sessionsTitle = document.getElementById('historySessionsSectionTitle');
  const sessionsCountBadge = document.getElementById('historySessionsCountBadge');
  const timelineList = document.getElementById('historyTimelineList');

  if (!svg || !legendList || !timelineList) return;

  const todayStr = getLocalDateStr();
  const isToday = dateStr === todayStr;

  const parsedDate = new Date(dateStr + 'T00:00:00');
  const formattedDisplayDate = parsedDate.toLocaleDateString([], {
    month: 'short', day: 'numeric', year: 'numeric'
  });

  if (title) {
    title.textContent = isToday ? `Today (${formattedDisplayDate})` : `${formattedDisplayDate}`;
  }

  if (nextBtn) {
    nextBtn.disabled = isToday;
  }

  if (sessionsTitle) {
    sessionsTitle.textContent = isToday ? "Today's Sessions" : `Sessions (${formattedDisplayDate})`;
  }

  // 1. Render Pie Chart
  const subjectTotals = getSubjectDistributionForDate(dateStr);
  let totalSec = 0;
  Object.values(subjectTotals).forEach(item => {
    if (item && item.durationSec > 0) totalSec += item.durationSec;
  });

  const totalMin = Math.round(totalSec / 60);
  let formattedTotal = '0m';
  if (totalSec > 0) {
    if (totalMin >= 60) {
      const h = Math.floor(totalMin / 60);
      const m = totalMin % 60;
      formattedTotal = `${h}h ${m > 0 ? m + 'm' : ''}`;
    } else if (totalMin === 0) {
      formattedTotal = `${totalSec}s`;
    } else {
      formattedTotal = `${totalMin}m`;
    }
  }

  if (centerSub) centerSub.textContent = 'Studied';
  if (centerVal) centerVal.textContent = formattedTotal;
  if (centerPct) centerPct.classList.add('hidden');

  svg.innerHTML = '';
  legendList.innerHTML = '';

  const entries = Object.values(subjectTotals).filter(item => item.durationSec > 0);

  if (entries.length === 0 || totalSec === 0) {
    const track = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    track.setAttribute('cx', '100');
    track.setAttribute('cy', '100');
    track.setAttribute('r', '70');
    track.setAttribute('class', 'donut-bg-track');
    svg.appendChild(track);

    legendList.innerHTML = `
      <div class="empty-hub-state" style="padding: 16px 8px;">
        <span>No study sessions recorded for ${formattedDisplayDate}.</span>
      </div>
    `;
  } else {
    const radius = 70;
    const strokeW = 22;
    const circumference = 2 * Math.PI * radius;
    let accumulatedPercent = 0;

    const bgTrack = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    bgTrack.setAttribute('cx', '100');
    bgTrack.setAttribute('cy', '100');
    bgTrack.setAttribute('r', radius.toString());
    bgTrack.setAttribute('class', 'donut-bg-track');
    bgTrack.setAttribute('stroke-width', strokeW.toString());
    svg.appendChild(bgTrack);

    entries.sort((a, b) => b.durationSec - a.durationSec);

    function highlightModalSubject(item) {
      const itemPct = Math.round((item.durationSec / totalSec) * 100);
      const itemMin = Math.round(item.durationSec / 60);
      const itemTimeStr = itemMin >= 60 
        ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
        : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

      if (centerSub) centerSub.textContent = item.name;
      if (centerVal) centerVal.textContent = itemTimeStr;
      if (centerPct) {
        centerPct.textContent = `${itemPct}% of day`;
        centerPct.classList.remove('hidden');
      }

      svg.querySelectorAll('.donut-slice').forEach(s => {
        if (s.getAttribute('data-sub-id') === item.id) {
          s.classList.add('active');
          s.style.opacity = '1';
          s.style.strokeWidth = (strokeW + 5).toString();
        } else {
          s.classList.remove('active');
          s.style.opacity = '0.35';
          s.style.strokeWidth = strokeW.toString();
        }
      });

      legendList.querySelectorAll('.donut-legend-item').forEach(l => {
        if (l.getAttribute('data-sub-id') === item.id) {
          l.classList.add('active');
        } else {
          l.classList.remove('active');
        }
      });
    }

    function resetModalHighlight() {
      if (centerSub) centerSub.textContent = 'Studied';
      if (centerVal) centerVal.textContent = formattedTotal;
      if (centerPct) centerPct.classList.add('hidden');

      svg.querySelectorAll('.donut-slice').forEach(s => {
        s.classList.remove('active');
        s.style.opacity = '1';
        s.style.strokeWidth = strokeW.toString();
      });
      legendList.querySelectorAll('.donut-legend-item').forEach(l => l.classList.remove('active'));
    }

    svgWrap?.addEventListener('mouseleave', resetModalHighlight);

    entries.forEach(item => {
      const itemPct = (item.durationSec / totalSec) * 100;
      const itemMin = Math.round(item.durationSec / 60);
      const itemTimeStr = itemMin >= 60 
        ? `${Math.floor(itemMin / 60)}h ${itemMin % 60 > 0 ? (itemMin % 60) + 'm' : ''}` 
        : (itemMin === 0 ? `${item.durationSec}s` : `${itemMin}m`);

      const sliceLength = (itemPct / 100) * circumference;
      const offset = -((accumulatedPercent / 100) * circumference);

      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
      circle.setAttribute('cx', '100');
      circle.setAttribute('cy', '100');
      circle.setAttribute('r', radius.toString());
      circle.setAttribute('class', 'donut-slice');
      circle.setAttribute('data-sub-id', item.id);
      circle.setAttribute('stroke', item.color);
      circle.setAttribute('stroke-width', strokeW.toString());
      circle.setAttribute('stroke-dasharray', `${sliceLength} ${circumference}`);
      circle.setAttribute('stroke-dashoffset', offset.toString());
      circle.innerHTML = `<title>${item.name}: ${Math.round(itemPct)}% (${itemTimeStr})</title>`;

      circle.addEventListener('mouseenter', () => highlightModalSubject(item));
      circle.addEventListener('click', (e) => {
        e.stopPropagation();
        highlightModalSubject(item);
      });

      svg.appendChild(circle);
      accumulatedPercent += itemPct;

      const legendItem = document.createElement('div');
      legendItem.className = 'donut-legend-item';
      legendItem.setAttribute('data-sub-id', item.id);
      legendItem.innerHTML = `
        <div class="donut-legend-left">
          <span class="donut-legend-dot" style="background-color: ${item.color};"></span>
          <span class="donut-legend-name">${item.name}</span>
        </div>
        <div class="donut-legend-right">
          <span class="donut-legend-pct">${Math.round(itemPct)}%</span>
          <span class="donut-legend-time">${itemTimeStr}</span>
        </div>
      `;

      legendItem.addEventListener('mouseenter', () => highlightModalSubject(item));
      legendItem.addEventListener('mouseleave', resetModalHighlight);
      legendItem.addEventListener('click', (e) => {
        e.stopPropagation();
        highlightModalSubject(item);
      });

      legendList.appendChild(legendItem);
    });
  }

  // 2. Render Timeline Sessions for this Date
  const allSessions = getAllValidatedSessions();
  const dateSessions = allSessions.filter(s => {
    if (!s || !s.durationSec) return false;
    const dStr = getLocalDateStr(s.timestamp || s.startTime);
    return dStr === dateStr;
  });

  if (sessionsCountBadge) {
    sessionsCountBadge.textContent = `${dateSessions.length} session${dateSessions.length === 1 ? '' : 's'}`;
  }

  if (dateSessions.length === 0) {
    timelineList.innerHTML = `
      <div class="timeline-empty-state" style="padding: 20px 10px;">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <circle cx="12" cy="12" r="10"></circle>
          <polyline points="12 6 12 12 16 14"></polyline>
        </svg>
        <p>No study sessions logged on this date.</p>
      </div>
    `;
    return;
  }

  timelineList.innerHTML = '';
  dateSessions.forEach(sess => {
    const mins = Math.max(1, Math.round(sess.durationSec / 60));
    const startTimeFormatted = new Date(sess.startTime || sess.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const endTimeFormatted = sess.endTime 
      ? new Date(sess.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : new Date((sess.startTime || sess.timestamp) + (sess.durationSec * 1000)).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const item = document.createElement('div');
    item.className = 'timeline-item';
    item.innerHTML = `
      <div class="timeline-item-left">
        <span class="subject-color-dot" style="background-color: ${sess.subject?.color || '#3b82f6'};"></span>
        <div>
          <div class="timeline-subject-name">${sess.subject?.name || 'Focus Study'}</div>
          <div class="timeline-meta-text">${startTimeFormatted} - ${endTimeFormatted}</div>
        </div>
      </div>
      <div class="timeline-item-right">
        <span class="timeline-duration-badge">${mins}m</span>
      </div>
    `;
    timelineList.appendChild(item);
  });
}

function renderUserProfileUI() {
  const profile = appState.userProfile || {
    displayName: 'Student',
    avatarPreset: '',
    avatarRing: 'glow-gold',
    countryFlag: '🌐',
    motto: '🎯 Deep focus & daily consistency',
    primarySubjectId: 'math',
    isPublicLeaderboard: true
  };

  const name = profile.displayName || 
               appState.currentUser?.user_metadata?.full_name || 
               appState.currentUser?.user_metadata?.name || 
               appState.currentUser?.email?.split('@')[0] || 
               'Student';
  const avatar = (profile.avatarUrl && /^(https?:\/\/|data:|blob:)/i.test(profile.avatarUrl) ? profile.avatarUrl : null) ||
                 (profile.avatarPreset && /^(https?:\/\/|data:|blob:)/i.test(profile.avatarPreset) ? profile.avatarPreset : null) ||
                 appState.currentUser?.user_metadata?.avatar_url || 
                 appState.currentUser?.user_metadata?.picture || 
                 profile.avatarPreset ||
                 '';
  const ring = profile.avatarRing || 'glow-gold';

  const userDisplayName = document.getElementById('userDisplayName');
  const dropdownUserName = document.getElementById('dropdownUserName');
  const dropdownUserEmail = document.getElementById('dropdownUserEmail');
  const navUserAvatarWrap = document.getElementById('navUserAvatarWrap');
  const userAvatarImg = document.getElementById('userAvatarImg');

  if (userDisplayName) userDisplayName.textContent = name;
  if (dropdownUserName) dropdownUserName.textContent = name;
  if (dropdownUserEmail && appState.currentUser) dropdownUserEmail.textContent = appState.currentUser.email || '';
  
  if (navUserAvatarWrap) {
    navUserAvatarWrap.innerHTML = getAvatarElementHtml(avatar, name, 'user-avatar', ring);
  } else if (userAvatarImg) {
    userAvatarImg.className = `user-avatar-img ${ring}`;
  }
}

// ----------------------------------------------------------------------------
