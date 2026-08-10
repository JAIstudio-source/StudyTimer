/**
 * Vercel Speed Insights initialization
 * This file initializes Speed Insights using the @vercel/speed-insights package
 * 
 * Documentation: https://vercel.com/docs/speed-insights/quickstart
 */

import { injectSpeedInsights } from '@vercel/speed-insights';

// Initialize Speed Insights
// This will inject the tracking script and start collecting Web Vitals
injectSpeedInsights({
  debug: false, // Set to true for development debugging
  // Additional options can be configured here:
  // sampleRate: 1, // Sample rate for events (1 = 100%)
  // beforeSend: (event) => event, // Middleware to modify events before sending
});
