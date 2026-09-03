/**
 * StudyTimer - Production Service Worker
 * Network-First caching strategy for CSS, JS, and HTML to guarantee fresh loads
 * with instant offline fallback.
 */

var CACHE_NAME = 'studytimer-web-v1';

var CORE_ASSETS = [
    './',
    './index.html',
    './privacy.html',
    './terms.html',
    './delete-account.html',
    './thank-you.html',
    './404.html',
    './style.css',
    './script.js',
    './version.json',
    './assets/logo.png',
    './assets/Featured.webp'
];

self.addEventListener('install', function (event) {
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME).then(function (cache) {
            return cache.addAll(CORE_ASSETS);
        }).catch(function () {})
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

self.addEventListener('fetch', function (event) {
    var request = event.request;
    if (request.method !== 'GET') return;

    var url = new URL(request.url);
    if (url.origin !== location.origin) return;

    // Do not cache binary APK downloads
    if (url.pathname.endsWith('.apk')) return;

    // Network-First Strategy for HTML, CSS, JS, and Version JSON
    event.respondWith(
        fetch(request)
            .then(function (networkResponse) {
                if (networkResponse && networkResponse.status === 200) {
                    var responseToCache = networkResponse.clone();
                    caches.open(CACHE_NAME).then(function (cache) {
                        cache.put(request, responseToCache);
                    });
                }
                return networkResponse;
            })
            .catch(function () {
                // Offline fallback
                return caches.match(request).then(function (cachedResponse) {
                    if (cachedResponse) return cachedResponse;
                    if (request.mode === 'navigate') {
                        return caches.match('./index.html');
                    }
                });
            })
    );
});
