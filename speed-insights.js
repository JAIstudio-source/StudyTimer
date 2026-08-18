/**
 * Vercel Speed Insights initialization
 * For plain HTML/JS deployments, Vercel Speed Insights is automatically loaded via /_vercel/insights/script.js.
 */

if (typeof window !== 'undefined') {
    // If running in an environment with bundler / module resolution:
    try {
        if (typeof injectSpeedInsights === 'function') {
            injectSpeedInsights({ debug: false });
        }
    } catch (_e) {
        // Fallback safely in vanilla browser environments
    }
}
