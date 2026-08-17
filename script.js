document.addEventListener('DOMContentLoaded', () => {
    
    // ----------------------------------------------------
    // Mobile Haptic Feedback Helper (Only for Major Actions)
    // ----------------------------------------------------
    function triggerHaptic() {
        if ('vibrate' in navigator) {
            try {
                navigator.vibrate(50);
            } catch (e) {
                // Ignore if vibration permission denied
            }
        }
    }

    // Attach Haptic Feedback & Tactile State to Major APK Download Buttons
    document.querySelectorAll('.download-link').forEach(link => {
        link.addEventListener('click', () => {
            triggerHaptic();
            const textSpan = link.querySelector('span:not(.btn-subtext)');
            if (textSpan && !link.classList.contains('downloading-feedback')) {
                const originalText = textSpan.textContent;
                textSpan.textContent = 'Starting Download... 🚀';
                link.classList.add('downloading-feedback');
                setTimeout(() => {
                    textSpan.textContent = originalText;
                    link.classList.remove('downloading-feedback');
                }, 2200);
            }
        });
    });

    // ----------------------------------------------------
    // 1. Fetch version.json & update release specs
    // ----------------------------------------------------
    async function loadAppVersion() {
        const versionSources = [
            'version.json',
            'https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/version.json'
        ];

        let data = null;

        for (const source of versionSources) {
            try {
                const response = await fetch(source);
                if (!response.ok) continue;

                data = await response.json();
                break;
            } catch (error) {
                console.warn(`Version fetch failed for ${source}:`, error);
            }
        }

        if (data) {
            const versionText = data.versionName || data.version || 'v2.5.0';
            const versionElements = document.querySelectorAll('.app-version');
            versionElements.forEach(el => {
                el.textContent = versionText;
            });

            const apkUrl = data.apkUrl || data.url || 'StudyTimer-release.apk';
            const downloadLinks = document.querySelectorAll('.download-link');
            downloadLinks.forEach(el => {
                el.href = apkUrl;
            });

            if (data.releaseNotes) {
                const releaseNotesEl = document.querySelector('.release-notes-text');
                if (releaseNotesEl) {
                    releaseNotesEl.textContent = data.releaseNotes;
                }
            }

            // Auto download detection
            const urlParams = new URLSearchParams(window.location.search);
            if (urlParams.get('download') === 'true' || urlParams.get('autodownload') === 'true' || window.location.hash === '#download-now') {
                setTimeout(() => {
                    const downloadAnchor = document.createElement('a');
                    downloadAnchor.href = apkUrl;
                    downloadAnchor.setAttribute('download', 'StudyTimer-release.apk');
                    document.body.appendChild(downloadAnchor);
                    downloadAnchor.click();
                    document.body.removeChild(downloadAnchor);
                }, 500);
            }

            updateAppSize(apkUrl);
        } else {
            setFallbackVersion();
        }
    }

    async function updateAppSize(apkUrl) {
        try {
            const response = await fetch(apkUrl, { method: 'HEAD' });
            const bytes = parseInt(response.headers.get('Content-Length'), 10);
            if (!isNaN(bytes) && bytes > 0) {
                const mb = (bytes / (1024 * 1024)).toFixed(2);
                const sizeElements = document.querySelectorAll('.app-size');
                sizeElements.forEach(el => {
                    el.textContent = mb + ' MB';
                });
            }
        } catch (error) {
            console.warn(`Size fetch failed for ${apkUrl}:`, error);
        }
    }

    function setFallbackVersion() {
        const versionElements = document.querySelectorAll('.app-version');
        versionElements.forEach(el => { el.textContent = 'v2.5.0'; });

        const downloadLinks = document.querySelectorAll('.download-link');
        downloadLinks.forEach(el => { el.href = 'StudyTimer-release.apk'; });
    }

    loadAppVersion();

    // ----------------------------------------------------
    // 2. Theme Customizer Switcher (AMOLED, Indigo, Slate, Emerald)
    // ----------------------------------------------------
    const themePills = document.querySelectorAll('.theme-pill');
    const savedTheme = localStorage.getItem('studytimer_theme') || 'amoled';

    function applyTheme(themeName) {
        document.body.setAttribute('data-theme', themeName);
        themePills.forEach(pill => {
            if (pill.getAttribute('data-theme-name') === themeName) {
                pill.classList.add('active');
            } else {
                pill.classList.remove('active');
            }
        });
        localStorage.setItem('studytimer_theme', themeName);
    }

    applyTheme(savedTheme);

    themePills.forEach(pill => {
        pill.addEventListener('click', () => {
            const theme = pill.getAttribute('data-theme-name');
            applyTheme(theme);
        });
    });

    // ----------------------------------------------------
    // 3. Mobile Navigation Drawer Toggle & Backdrop
    // ----------------------------------------------------
    const menuToggle = document.getElementById('menu-toggle');
    const navLinks = document.getElementById('nav-links');
    const navBackdrop = document.getElementById('nav-backdrop');

    function closeNavDrawer() {
        if (navLinks) navLinks.classList.remove('mobile-open');
        if (navBackdrop) navBackdrop.classList.remove('active');
    }

    if (menuToggle && navLinks) {
        menuToggle.addEventListener('click', () => {
            const isOpen = navLinks.classList.toggle('mobile-open');
            if (navBackdrop) navBackdrop.classList.toggle('active', isOpen);
        });

        if (navBackdrop) {
            navBackdrop.addEventListener('click', closeNavDrawer);
        }

        // Close menu on link click
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', closeNavDrawer);
        });

        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && navLinks.classList.contains('mobile-open')) {
                closeNavDrawer();
            }
        });
    }

    // ----------------------------------------------------
    // 4. Interactive Live Focus Web Timer Engine
    // ----------------------------------------------------
    const modeButtons = document.querySelectorAll('.timer-mode-btn');
    const timerClock = document.getElementById('timer-clock');
    const timerModeLabel = document.getElementById('timer-mode-label');
    const timerStatusText = document.getElementById('timer-status-text');
    const timerToggleBtn = document.getElementById('timer-toggle-btn');
    const timerResetBtn = document.getElementById('timer-reset-btn');
    const timerSoundBtn = document.getElementById('timer-sound-btn');
    const timerBtnText = document.getElementById('timer-btn-text');
    const timerProgress = document.getElementById('timer-progress');
    const iconPlay = timerToggleBtn ? timerToggleBtn.querySelector('.icon-play') : null;
    const iconPause = timerToggleBtn ? timerToggleBtn.querySelector('.icon-pause') : null;

    let totalDurationSeconds = 25 * 60;
    let remainingSeconds = totalDurationSeconds;
    let timerInterval = null;
    let isRunning = false;
    let isSoundEnabled = true;

    const ringCircumference = 660; // 2 * PI * 105

    function formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    function updateTimerUI() {
        if (timerClock) timerClock.textContent = formatTime(remainingSeconds);
        
        if (timerProgress) {
            const fraction = remainingSeconds / totalDurationSeconds;
            const offset = ringCircumference * (1 - fraction);
            timerProgress.style.strokeDashoffset = offset;
        }
    }

    function setTimerMode(modeName, minutes) {
        pauseTimer();
        totalDurationSeconds = minutes * 60;
        remainingSeconds = totalDurationSeconds;

        modeButtons.forEach(btn => {
            if (btn.getAttribute('data-mode') === modeName) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        });

        if (timerModeLabel) {
            const labelMap = {
                focus: 'Deep Focus',
                shortBreak: 'Short Break',
                longBreak: 'Long Break',
                lecture: 'Lecture Mode'
            };
            timerModeLabel.textContent = labelMap[modeName] || 'Focus Session';
        }

        if (timerStatusText) {
            timerStatusText.textContent = `Ready for ${minutes} min session`;
        }

        const timerReadout = document.querySelector('.timer-readout');
        if (timerReadout) {
            timerReadout.classList.remove('mode-pulse');
            void timerReadout.offsetWidth; // trigger reflow
            timerReadout.classList.add('mode-pulse');
        }

        updateTimerUI();
    }

    function startTimer() {
        if (isRunning) return;
        isRunning = true;
        if (timerBtnText) timerBtnText.textContent = 'Pause Session';
        if (iconPlay) iconPlay.classList.add('hidden');
        if (iconPause) iconPause.classList.remove('hidden');
        if (timerStatusText) timerStatusText.textContent = 'Session active - stay focused!';

        timerInterval = setInterval(() => {
            if (remainingSeconds > 0) {
                remainingSeconds--;
                updateTimerUI();
            } else {
                finishTimer();
            }
        }, 1000);
    }

    function pauseTimer() {
        if (!isRunning) return;
        isRunning = false;
        clearInterval(timerInterval);
        if (timerBtnText) timerBtnText.textContent = 'Resume Session';
        if (iconPlay) iconPlay.classList.remove('hidden');
        if (iconPause) iconPause.classList.add('hidden');
        if (timerStatusText) timerStatusText.textContent = 'Session paused';
    }

    function resetTimer() {
        pauseTimer();
        remainingSeconds = totalDurationSeconds;
        if (timerBtnText) timerBtnText.textContent = 'Start Session';
        if (timerStatusText) timerStatusText.textContent = 'Ready to study';
        updateTimerUI();
    }

    function finishTimer() {
        pauseTimer();
        remainingSeconds = 0;
        updateTimerUI();
        if (timerStatusText) timerStatusText.textContent = '🎉 Session Completed!';

        triggerHaptic();

        if (isSoundEnabled) {
            playChimeSound();
        }
    }

    function playChimeSound() {
        try {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (!AudioContext) return;
            const ctx = new AudioContext();
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();

            osc.type = 'sine';
            osc.frequency.setValueAtTime(523.25, ctx.currentTime); // C5
            osc.frequency.exponentialRampToValueAtTime(659.25, ctx.currentTime + 0.3); // E5
            osc.frequency.exponentialRampToValueAtTime(783.99, ctx.currentTime + 0.6); // G5

            gain.gain.setValueAtTime(0.3, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 1.2);

            osc.connect(gain);
            gain.connect(ctx.destination);

            osc.start();
            osc.stop(ctx.currentTime + 1.2);
        } catch (e) {
            console.warn('Web Audio chime unavailable:', e);
        }
    }

    if (timerToggleBtn) {
        timerToggleBtn.addEventListener('click', () => {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });
    }

    if (timerResetBtn) {
        timerResetBtn.addEventListener('click', () => {
            resetTimer();
        });
    }

    if (timerSoundBtn) {
        timerSoundBtn.addEventListener('click', () => {
            isSoundEnabled = !isSoundEnabled;
            timerSoundBtn.style.opacity = isSoundEnabled ? '1' : '0.4';
        });
    }

    modeButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const mode = btn.getAttribute('data-mode');
            const minutes = parseInt(btn.getAttribute('data-minutes'), 10);
            setTimerMode(mode, minutes);
        });
    });

    setTimerMode('focus', 25);

    // ----------------------------------------------------
    // 5. Interactive 6-Month Heatmap Grid Generator
    // ----------------------------------------------------
    const heatmapGrid = document.getElementById('heatmap-grid');
    const heatmapTooltip = document.getElementById('heatmap-tooltip');

    if (heatmapGrid) {
        const totalDays = 140; // 20 weeks x 7 days
        const today = new Date();
        
        for (let i = totalDays - 1; i >= 0; i--) {
            const cell = document.createElement('div');
            const cellDate = new Date(today);
            cellDate.setDate(today.getDate() - i);

            // Generate deterministic mock level based on day index
            const levelSeed = (i * 7 + i % 3 + Math.floor(i / 5)) % 5;
            const level = (i % 7 === 0 || i % 7 === 6) ? Math.min(levelSeed, 2) : levelSeed;

            cell.className = `heatmap-cell level-${level}`;

            const dateStr = cellDate.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
            let hours = (level * 1.5).toFixed(1);
            let sessions = level * 2;

            cell.setAttribute('data-date', dateStr);
            cell.setAttribute('data-hours', hours);
            cell.setAttribute('data-sessions', sessions);

            const showTooltip = () => {
                if (heatmapTooltip) {
                    if (level === 0) {
                        heatmapTooltip.innerHTML = `<span class="h-tooltip-date">${dateStr}: <strong>Rest Day (0 hrs)</strong></span>`;
                    } else {
                        heatmapTooltip.innerHTML = `<span class="h-tooltip-date">${dateStr}: <strong class="primary-text">${hours} hrs focus</strong> (${sessions} sessions)</span>`;
                    }
                }
            };

            cell.addEventListener('mouseenter', showTooltip);
            cell.addEventListener('click', showTooltip);

            heatmapGrid.appendChild(cell);
        }
    }

    // ----------------------------------------------------
    // 6. Screenshot Carousel & Tab Selector (Smooth Mobile Native Scroll)
    // ----------------------------------------------------
    const track = document.getElementById('carousel-track');
    const btnPrev = document.querySelector('.prev-btn');
    const btnNext = document.querySelector('.next-btn');
    const indicators = document.querySelectorAll('.indicator');
    const tabBtns = document.querySelectorAll('.screenshot-tab-btn');

    if (track) {
        const getScrollAmount = () => track.clientWidth || 320;

        if (btnPrev && btnNext) {
            btnPrev.addEventListener('click', () => {
                track.scrollBy({ left: -getScrollAmount(), behavior: 'smooth' });
            });

            btnNext.addEventListener('click', () => {
                track.scrollBy({ left: getScrollAmount(), behavior: 'smooth' });
            });
        }

        // Active indicator and tab synchronization on scroll
        track.addEventListener('scroll', () => {
            const scrollPos = track.scrollLeft;
            const singleWidth = getScrollAmount();
            const index = Math.min(Math.round(scrollPos / singleWidth), indicators.length - 1);

            indicators.forEach((ind, i) => {
                if (i === index) ind.classList.add('active');
                else ind.classList.remove('active');
            });

            tabBtns.forEach((tab, i) => {
                if (i === index) tab.classList.add('active');
                else tab.classList.remove('active');
            });
        }, { passive: true });

        // Tab click navigation
        tabBtns.forEach((tab, i) => {
            tab.addEventListener('click', () => {
                const singleWidth = getScrollAmount();
                track.scrollTo({ left: singleWidth * i, behavior: 'smooth' });
            });
        });

        // Indicator click navigation
        indicators.forEach((ind, i) => {
            ind.addEventListener('click', () => {
                const singleWidth = getScrollAmount();
                track.scrollTo({ left: singleWidth * i, behavior: 'smooth' });
            });
        });

        // Mouse Drag Support for Desktop
        let isDown = false;
        let startX;
        let scrollLeftPos;

        track.addEventListener('mousedown', (e) => {
            isDown = true;
            startX = e.pageX - track.offsetLeft;
            scrollLeftPos = track.scrollLeft;
        });

        track.addEventListener('mouseleave', () => { isDown = false; });
        track.addEventListener('mouseup', () => { isDown = false; });

        track.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - track.offsetLeft;
            const walk = (x - startX) * 2;
            track.scrollLeft = scrollLeftPos - walk;
        });
    }

    // ----------------------------------------------------
    // 7. Lightbox for Screenshot Zoom
    // ----------------------------------------------------
    const lightbox = document.getElementById('lightbox');
    const lightboxImg = lightbox ? lightbox.querySelector('.lightbox-img') : null;
    const lightboxCaption = document.getElementById('lightbox-caption');
    const lightboxClose = lightbox ? lightbox.querySelector('.lightbox-close') : null;
    const screenshots = document.querySelectorAll('.screenshot-img');

    if (lightbox && lightboxImg && lightboxClose) {
        screenshots.forEach(img => {
            img.addEventListener('click', () => {
                lightboxImg.src = img.src;
                if (lightboxCaption) {
                    lightboxCaption.textContent = img.getAttribute('data-title') || img.alt;
                }
                lightbox.classList.add('active');
                document.body.style.overflow = 'hidden';
            });
        });

        const closeLightbox = () => {
            lightbox.classList.remove('active');
            document.body.style.overflow = '';
            setTimeout(() => { lightboxImg.src = ''; }, 300);
        };

        lightboxClose.addEventListener('click', closeLightbox);
        lightbox.addEventListener('click', (e) => {
            if (e.target === lightbox) closeLightbox();
        });
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape' && lightbox.classList.contains('active')) {
                closeLightbox();
            }
        });
    }

    // ----------------------------------------------------
    // 8. FAQ Accordion Toggle Logic
    // ----------------------------------------------------
    const faqItems = document.querySelectorAll('.faq-item');

    faqItems.forEach(item => {
        const questionBtn = item.querySelector('.faq-question');
        if (questionBtn) {
            questionBtn.addEventListener('click', () => {
                const isActive = item.classList.contains('active');
                faqItems.forEach(otherItem => otherItem.classList.remove('active'));

                if (!isActive) {
                    item.classList.add('active');
                }
            });
        }
    });

    // ----------------------------------------------------
    // 9. Web Share API & QR Code Modal Toggle
    // ----------------------------------------------------
    const shareAppBtn = document.getElementById('share-app-btn');
    if (shareAppBtn) {
        shareAppBtn.addEventListener('click', async () => {
            const shareData = {
                title: 'StudyTimer: Focus, Track & Plan',
                url: window.location.href
            };

            if (navigator.share) {
                try {
                    await navigator.share(shareData);
                } catch (err) {
                    console.log('Share dismissed:', err);
                }
            } else {
                try {
                    await navigator.clipboard.writeText(window.location.href);
                    alert('Link copied to clipboard! Share it with your friends.');
                } catch (err) {
                    prompt('Copy this link to share:', window.location.href);
                }
            }
        });
    }

    const qrModal = document.getElementById('qr-modal');
    const qrModalBtn = document.getElementById('qr-modal-btn');
    const qrCloseBtn = document.getElementById('qr-close-btn');

    if (qrModal && qrModalBtn && qrCloseBtn) {
        qrModalBtn.addEventListener('click', () => {
            qrModal.classList.add('active');
            document.body.style.overflow = 'hidden';
        });

        const closeQrModal = () => {
            qrModal.classList.remove('active');
            document.body.style.overflow = '';
        };

        qrCloseBtn.addEventListener('click', closeQrModal);
        qrModal.addEventListener('click', (e) => {
            if (e.target === qrModal) closeQrModal();
        });
    }

    // ----------------------------------------------------
    // 10. Scroll Progress Bar & Scroll Animations
    // ----------------------------------------------------
    const scrollProgress = document.getElementById('scroll-progress');
    const navbar = document.getElementById('navbar');
    const backToTopBtn = document.getElementById('back-to-top');

    window.addEventListener('scroll', () => {
        const scrollTop = window.scrollY;
        const docHeight = document.body.scrollHeight - window.innerHeight;
        const scrollPercent = (scrollTop / docHeight) * 100;
        
        if (scrollProgress) {
            scrollProgress.style.width = `${scrollPercent}%`;
        }

        if (navbar) {
            if (scrollTop > 60) {
                navbar.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.6)';
            } else {
                navbar.style.boxShadow = 'none';
            }
        }

        if (backToTopBtn) {
            if (scrollTop > 500) {
                backToTopBtn.classList.add('visible');
            } else {
                backToTopBtn.classList.remove('visible');
            }
        }
    }, { passive: true });

    // Scroll Reveal Observer
    const revealElements = document.querySelectorAll('.reveal');
    if (revealElements.length) {
        if ('IntersectionObserver' in window) {
            const revealObserver = new IntersectionObserver((entries, observer) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('active');
                        observer.unobserve(entry.target);
                    }
                });
            }, {
                root: null,
                threshold: 0.05,
                rootMargin: '0px 0px -40px 0px'
            });

            revealElements.forEach(el => revealObserver.observe(el));
        } else {
            revealElements.forEach(el => el.classList.add('active'));
        }
    }

    // ----------------------------------------------------
    // Active Navigation Scrollspy
    // ----------------------------------------------------
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('header[id], section[id]');

    if (navItems.length && sections.length) {
        const scrollspyObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    const id = entry.target.getAttribute('id');
                    navItems.forEach(item => {
                        if (item.getAttribute('href') === `#${id}`) {
                            item.classList.add('active');
                        } else {
                            item.classList.remove('active');
                        }
                    });
                }
            });
        }, {
            root: null,
            rootMargin: '-15% 0px -65% 0px',
            threshold: 0.05
        });

        sections.forEach(sec => scrollspyObserver.observe(sec));
    }

    // Footer Year
    const yearEl = document.getElementById('year');
    if (yearEl) yearEl.textContent = new Date().getFullYear();

    // ----------------------------------------------------
    // 11. Floating Particles Canvas
    // ----------------------------------------------------
    const canvas = document.getElementById('particles-canvas');
    if (canvas) {
        const ctx = canvas.getContext('2d');
        let width, height;
        let particles = [];

        function resizeCanvas() {
            width = canvas.width = window.innerWidth;
            height = canvas.height = window.innerHeight;
        }

        window.addEventListener('resize', resizeCanvas);
        resizeCanvas();

        class Particle {
            constructor() {
                this.x = Math.random() * width;
                this.y = Math.random() * height;
                this.size = Math.random() * 2 + 0.5;
                this.speedX = Math.random() * 0.4 - 0.2;
                this.speedY = Math.random() * 0.4 - 0.2;
                this.opacity = Math.random() * 0.4 + 0.1;
            }

            update() {
                this.x += this.speedX;
                this.y += this.speedY;

                if (this.x > width) this.x = 0;
                if (this.x < 0) this.x = width;
                if (this.y > height) this.y = 0;
                if (this.y < 0) this.y = height;
            }

            draw() {
                ctx.fillStyle = `rgba(255, 255, 255, ${this.opacity})`;
                ctx.beginPath();
                ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
                ctx.fill();
            }
        }

        function initParticles() {
            particles = [];
            const particleCount = Math.min(window.innerWidth / 20, 50); 
            for (let i = 0; i < particleCount; i++) {
                particles.push(new Particle());
            }
        }

        function animateParticles() {
            ctx.clearRect(0, 0, width, height);
            particles.forEach(p => {
                p.update();
                p.draw();
            });
            requestAnimationFrame(animateParticles);
        }

        initParticles();
        animateParticles();
    }

    // ----------------------------------------------------
    // 12. Privacy-First Website Telemetry Engine
    // ----------------------------------------------------
    const SUPABASE_URL = 'https://vkveimpvrpnzelbsvdrg.supabase.co';
    const SUPABASE_KEY = 'sb_publishable_Aec72P1pUF1I6eeO-C5vcA_i2jQgEx6';

    function getWebVisitorId() {
        let id = localStorage.getItem('st_web_visitor_id');
        if (!id) {
            id = 'web_' + Math.random().toString(36).substring(2, 11) + Date.now().toString(36);
            localStorage.setItem('st_web_visitor_id', id);
        }
        return id;
    }

    function getWebFirstSeen() {
        let firstSeen = localStorage.getItem('st_web_first_seen');
        const now = Date.now();
        if (!firstSeen) {
            firstSeen = now;
            localStorage.setItem('st_web_first_seen', firstSeen);
        }
        return parseInt(firstSeen, 10);
    }

    async function sendWebTelemetry(eventName, properties = {}) {
        try {
            const visitorId = getWebVisitorId();
            const now = Date.now();
            const eventId = `${visitorId}_${eventName}_${Math.floor(now / 5000)}`;

            const payload = [{
                event_id: eventId,
                anonymous_id: visitorId,
                user_id: null,
                is_authenticated: false,
                platform: 'web',
                event_name: eventName,
                app_version: 'Web Landing v2.5.0',
                device_model: navigator.userAgent.includes('Mobile') ? 'Mobile Browser' : 'Desktop Browser',
                timestamp: now,
                properties: properties
            }];

            await fetch(`${SUPABASE_URL}/rest/v1/app_analytics_events`, {
                method: 'POST',
                headers: {
                    'apikey': SUPABASE_KEY,
                    'Authorization': `Bearer ${SUPABASE_KEY}`,
                    'Content-Type': 'application/json',
                    'Prefer': 'resolution=ignore-duplicates'
                },
                body: JSON.stringify(payload)
            });

            syncWebCohort();
        } catch (e) {
            console.warn('Web telemetry skipped:', e);
        }
    }

    async function syncWebCohort() {
        try {
            const visitorId = getWebVisitorId();
            const firstSeen = getWebFirstSeen();
            const now = Date.now();
            const todayStr = new Date().toISOString().slice(0, 10);

            const payload = {
                anonymous_id: visitorId,
                user_id: null,
                is_authenticated: false,
                platform: 'web',
                first_seen: firstSeen,
                last_active_at: now,
                last_active_date: todayStr,
                app_version: 'Web Landing v2.5.0',
                device_model: navigator.userAgent.includes('Mobile') ? 'Mobile Browser' : 'Desktop Browser',
                updated_at: now
            };

            await fetch(`${SUPABASE_URL}/rest/v1/app_user_cohorts?on_conflict=anonymous_id`, {
                method: 'POST',
                headers: {
                    'apikey': SUPABASE_KEY,
                    'Authorization': `Bearer ${SUPABASE_KEY}`,
                    'Content-Type': 'application/json',
                    'Prefer': 'resolution=merge-duplicates'
                },
                body: JSON.stringify(payload)
            });
        } catch (e) {}
    }

    // Auto-record pageview on load
    sendWebTelemetry('web_pageview', {
        referrer: document.referrer || 'direct',
        screen_width: window.innerWidth
    });

    // Download button telemetry
    document.querySelectorAll('.download-link').forEach(link => {
        link.addEventListener('click', () => {
            sendWebTelemetry('web_download_click', { href: link.href });
        });
    });
});

