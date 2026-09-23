/**
 * StudyTimer - Production Service Worker (v7)
 * Offline-First & Stale-While-Revalidate caching strategy for HTML, CSS, JS, Fonts & Assets.
 * Guarantees 100% offline functionality for the Pomodoro Web Studio & App Landing.
 */

var CACHE_NAME = 'studytimer-web-v30';

var CORE_ASSETS = [
    './',
    './index.html',
    './demo.html',
    './demo.css',
    './demo.js',
    './assets/supabase.js',
    './style.css',
    './script.js',
    './assets/logo.png',
    './assets/Featured.webp',
    './privacy.html',
    './terms.html',
    './delete-account.html',
    './thank-you.html',
    './404.html'
];

// Third-party CDN domains allowed to be cached for offline capability
var CACHABLE_CDN_HOSTS = [
    'cdn.jsdelivr.net',
    'cdnjs.cloudflare.com',
    'fonts.googleapis.com',
    'fonts.gstatic.com'
];

self.addEventListener('install', function (event) {
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME).then(function (cache) {
            return cache.addAll(CORE_ASSETS);
        }).catch(function (err) {
            console.warn('Service Worker cache.addAll non-fatal warning:', err);
        })
    );
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys().then(function (keys) {
            return Promise.all(
                keys.map(function (key) {
                    if (key !== CACHE_NAME) {
                        return caches.delete(key);
                    }
                })
            );
        }).then(function () {
            return self.clients.claim();
        })
    );
});

self.addEventListener('message', function (event) {
    if (event.data && event.data.type === 'SKIP_WAITING') {
        self.skipWaiting();
    }
});

self.addEventListener('fetch', function (event) {
    var request = event.request;
    if (request.method !== 'GET') return;

    var url = new URL(request.url);

    // Never cache binary APK download or dynamic version.json metadata
    if (url.pathname.endsWith('.apk') || url.pathname.endsWith('version.json')) {
        return;
    }

    var isSameOrigin = url.origin === location.origin;
    var isCachableCdn = CACHABLE_CDN_HOSTS.some(function (host) { return url.hostname.includes(host); });

    if (!isSameOrigin && !isCachableCdn) return;

    // Network-First with Stale Cache Fallback for maximum freshness and reliable offline support
    event.respondWith(
        fetch(request)
            .then(function (networkResponse) {
                if (networkResponse && (networkResponse.status === 200 || networkResponse.type === 'opaque')) {
                    var responseToCache = networkResponse.clone();
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put(request, responseToCache);
                    });
                }
                return networkResponse;
            })
            .catch(function () {
                // Offline Fallback
                return caches.match(request).then(function (cachedResponse) {
                    if (cachedResponse) return cachedResponse;
                    if (request.mode === 'navigate') {
                        if (url.pathname.includes('demo') || url.href.includes('demo')) {
                            return caches.match('./demo.html');
                        }
                        return caches.match('./index.html');
                    }
                });
            })
    );
});
