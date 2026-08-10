/**
 * Vercel Speed Insights initialization
 * This file initializes Speed Insights using the @vercel/speed-insights package
 */

import { injectSpeedInsights } from './node_modules/@vercel/speed-insights/dist/index.mjs';

// Initialize Speed Insights
injectSpeedInsights({
  debug: false, // Set to true for development debugging
  // Additional options can be configured here:
  // sampleRate: 1, // Sample rate for events (1 = 100%)
  // beforeSend: (event) => event, // Middleware to modify events before sending
});
