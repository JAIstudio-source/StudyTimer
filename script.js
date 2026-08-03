document.addEventListener('DOMContentLoaded', () => {
    
    // 1. Fetch version.json from GitHub Raw URL
    async function loadAppVersion() {
        try {
            const response = await fetch('https://raw.githubusercontent.com/JAIstudio-source/StudyTimer/main/version.json');
            
            if (response.ok) {
                const data = await response.json();
                
                // Read versionName from JSON format (adds 'v' prefix if missing)
                const versionString = data.versionName 
                    ? (data.versionName.startsWith('v') ? data.versionName : `v${data.versionName}`)
                    : 'v1.0.0';
                
                // Update version text across all elements
                const versionElements = document.querySelectorAll('.app-version');
                versionElements.forEach(el => {
                    el.textContent = versionString;
                });

                // Update download URL
                const downloadLinks = document.querySelectorAll('.download-link');
                downloadLinks.forEach(el => {
                    el.href = data.url || 'StudyTimer-release.apk';
                });
            } else {
                setFallbackVersion();
            }
        } catch (error) {
            console.error('Failed to load version.json from GitHub:', error);
            setFallbackVersion();
        }
    }

    function setFallbackVersion() {
        const downloadLinks = document.querySelectorAll('.download-link');
        downloadLinks.forEach(el => {
            el.href = 'StudyTimer-release.apk';
        });
    }

    loadAppVersion();

    // 2. Scroll Progress Bar & Glass Navbar Shadow
    const scrollProgress = document.getElementById('scroll-progress');
    const navbar = document.getElementById('navbar');
    
    window.addEventListener('scroll', () => {
        const scrollTop = window.scrollY;
        const docHeight = document.body.scrollHeight - window.innerHeight;
        const scrollPercent = (scrollTop / docHeight) * 100;
        
        scrollProgress.style.width = `${scrollPercent}%`;

        if (scrollTop > 50) {
            navbar.style.background = 'rgba(5, 5, 5, 0.85)';
            navbar.style.boxShadow = '0 4px 30px rgba(0, 0, 0, 0.5)';
        } else {
            navbar.style.background = 'rgba(5, 5, 5, 0.7)';
            navbar.style.boxShadow = 'none';
        }
    });

    // 3. Navbar Active Section Highlighting
    const sections = document.querySelectorAll('section, header');
    const navLinks = document.querySelectorAll('.nav-links a');

    const observerOptions = {
        root: null,
        rootMargin: '-50% 0px -50% 0px',
        threshold: 0
    };

    const sectionObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                navLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === `#${entry.target.id}`) {
                        link.classList.add('active');
                    }
                });
            }
        });
    }, observerOptions);

    sections.forEach(section => {
        sectionObserver.observe(section);
    });

    // 4. Reveal Animations on Scroll
    const revealElements = document.querySelectorAll('.reveal');
    
    const revealObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('active');
                observer.unobserve(entry.target);
            }
        });
    }, {
        root: null,
        rootMargin: '0px 0px -100px 0px',
        threshold: 0.1
    });

    revealElements.forEach(el => revealObserver.observe(el));

    // 5. Modern Carousel & Indicator Logic
    const track = document.getElementById('carousel-track');
    const btnPrev = document.querySelector('.prev-btn');
    const btnNext = document.querySelector('.next-btn');
    const indicators = document.querySelectorAll('.indicator');
    const items = document.querySelectorAll('.modern-image-wrapper');

    if (track && btnPrev && btnNext) {
        const scrollAmount = 350; 

        btnPrev.addEventListener('click', () => {
            track.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
        });

        btnNext.addEventListener('click', () => {
            track.scrollBy({ left: scrollAmount, behavior: 'smooth' });
        });

        // Sync indicators on scroll
        track.addEventListener('scroll', () => {
            const scrollPos = track.scrollLeft;
            const itemWidth = items[0].offsetWidth + 32;
            const activeIndex = Math.round(scrollPos / itemWidth);

            indicators.forEach((ind, index) => {
                if (index === activeIndex) {
                    ind.classList.add('active');
                } else {
                    ind.classList.remove('active');
                }
            });
        });

        // Click indicator to scroll
        indicators.forEach((ind, index) => {
            ind.addEventListener('click', () => {
                const itemWidth = items[0].offsetWidth + 32;
                track.scrollTo({
                    left: index * itemWidth,
                    behavior: 'smooth'
                });
            });
        });

        // Mouse Drag to Scroll
        let isDown = false;
        let startX;
        let scrollLeft;

        track.addEventListener('mousedown', (e) => {
            isDown = true;
            startX = e.pageX - track.offsetLeft;
            scrollLeft = track.scrollLeft;
        });

        track.addEventListener('mouseleave', () => { isDown = false; });
        track.addEventListener('mouseup', () => { isDown = false; });

        track.addEventListener('mousemove', (e) => {
            if (!isDown) return;
            e.preventDefault();
            const x = e.pageX - track.offsetLeft;
            const walk = (x - startX) * 2; 
            track.scrollLeft = scrollLeft - walk;
        });
    }

    // 6. Lightbox with Swipe and Keyboard Navigation
    const lightbox = document.getElementById('lightbox');
    const lightboxImg = lightbox.querySelector('.lightbox-img');
    const lightboxClose = lightbox.querySelector('.lightbox-close');
    const screenshots = Array.from(document.querySelectorAll('.screenshot-img'));
    let currentImageIndex = 0;

    function showLightboxImage(index) {
        if (index < 0) index = screenshots.length - 1;
        if (index >= screenshots.length) index = 0;
        currentImageIndex = index;
        lightboxImg.src = screenshots[currentImageIndex].src;
    }

    screenshots.forEach((img, index) => {
        img.addEventListener('click', () => {
            showLightboxImage(index);
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

    // Touch Swipe Support for Lightbox
    let touchStartX = 0;
    let touchEndX = 0;

    lightbox.addEventListener('touchstart', (e) => {
        touchStartX = e.changedTouches[0].screenX;
    }, { passive: true });

    lightbox.addEventListener('touchend', (e) => {
        touchEndX = e.changedTouches[0].screenX;
        handleLightboxSwipe();
    }, { passive: true });

    function handleLightboxSwipe() {
        const swipeThreshold = 50; 
        if (touchEndX < touchStartX - swipeThreshold) {
            // Swiped Left -> Next Image
            showLightboxImage(currentImageIndex + 1);
        }
        if (touchEndX > touchStartX + swipeThreshold) {
            // Swiped Right -> Previous Image
            showLightboxImage(currentImageIndex - 1);
        }
    }

    // Keyboard Arrow Keys & Escape for Lightbox
    document.addEventListener('keydown', (e) => {
        if (!lightbox.classList.contains('active')) return;

        if (e.key === 'ArrowRight') {
            showLightboxImage(currentImageIndex + 1);
        } else if (e.key === 'ArrowLeft') {
            showLightboxImage(currentImageIndex - 1);
        } else if (e.key === 'Escape') {
            closeLightbox();
        }
    });

    // 7. Back to Top Button
    const backToTopBtn = document.getElementById('back-to-top');

    window.addEventListener('scroll', () => {
        if (window.scrollY > 500) {
            backToTopBtn.classList.add('visible');
        } else {
            backToTopBtn.classList.remove('visible');
        }
    });

    backToTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    // 8. Footer Year
    document.getElementById('year').textContent = new Date().getFullYear();

    // 9. Floating Canvas Particles
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
                this.speedX = Math.random() * 0.5 - 0.25;
                this.speedY = Math.random() * 0.5 - 0.25;
                this.opacity = Math.random() * 0.5 + 0.1;
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
            const particleCount = Math.min(window.innerWidth / 15, 100); 
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
});
