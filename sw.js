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
    './assets/logo.png',
    './assets/Featured.webp'
];

function getCacheName() {
    return fetch('./version.json')
        .then(function (response) { return response.ok ? response.json() : null; })
        .then(function (data) {
            var version = (data && (data.versionCode || data.versionName)) || 'v2.0.0';
            return 'studytimer-' + version;
        })
        .catch(function () { return 'studytimer-v2.0.0'; });
}

function openCurrentCache() {
    return getCacheName().then(function (name) { return caches.open(name); });
}

function purgeOldCaches(currentName) {
    return caches.keys().then(function (keys) {
        return Promise.all(
            keys.filter(function (key) { return key !== currentName; })
                .map(function (key) { return caches.delete(key); })
        );
    });
}

function storeInCurrentCache(request, response) {
    return getCacheName()
        .then(function (name) {
            return caches.open(name).then(function (cache) {
                return cache.put(request, response).then(function () { return name; });
            });
        })
        .then(purgeOldCaches)
        .catch(function () {});
}

self.addEventListener('install', function (event) {
    event.waitUntil(
        openCurrentCache()
            .then(function (cache) { return cache.addAll(CORE_ASSETS); })
            .catch(function () {})
    );
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        getCacheName()
            .then(function (currentName) { return purgeOldCaches(currentName); })
            .then(function () { return self.clients.claim(); })
    );
});

self.addEventListener('fetch', function (event) {
    var request = event.request;
    if (request.method !== 'GET') return;

    var url = new URL(request.url);
    if (url.origin !== location.origin) return;

    if (url.pathname.endsWith('.apk')) return;

    if (url.pathname.endsWith('version.json')) {
        event.respondWith(
            fetch(request)
                .then(function (response) {
                    if (response && response.status === 200) {
                        storeInCurrentCache(request, response.clone());
                    }
                    return response;
                })
                .catch(function () { return caches.match(request); })
        );
        return;
    }

    if (request.mode === 'navigate') {
        event.respondWith(
            fetch(request)
                .then(function (response) {
                    if (response && response.status === 200) {
                        storeInCurrentCache(request, response.clone());
                    }
                    return response;
                })
                .catch(function () {
                    return caches.match(request)
                        .then(function (cached) { return cached || caches.match('./index.html'); });
                })
        );
        return;
    }

    event.respondWith(
        caches.match(request).then(function (cached) {
            if (cached) return cached;
            return fetch(request).then(function (response) {
                if (response && response.status === 200 && response.type === 'basic') {
                    storeInCurrentCache(request, response.clone());
                }
                return response;
            }).catch(function () { return caches.match(request); });
        })
    );
});
