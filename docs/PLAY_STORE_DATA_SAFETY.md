# StudyTimer — Google Play Data Safety & App Compliance Declaration

> **Application Version:** v3.3.6 (Build 50) &bull; **Target SDK:** 34 (Android 14) &bull; **Compatibility:** Android 9.0 – 16.0  
> **Last Verified:** November 2026

This document provides official answers and disclosures for the Google Play Console **Data Safety Questionnaire** and store listing review.

---

## 1. Target Audience & Content Rating
- **Target Age Group:** 13 and older (General Audience / Students & Professionals).
- **COPPA Compliance:** Not directed primarily at children under 13.
- **Ads / Monetization:** **100% Free**. Zero third-party advertisements, zero in-app purchases (IAP), zero subscriptions.
- **Generative AI Content:** No user-facing generative AI models or generative content features present in the app.

---

## 2. Data Safety Questionnaire Answers (Google Play Console)

### A. Data Collection Overview
- **Does the app collect or share user data?** Yes (Only when users optionally sign in or opt into community leaderboards; guest mode collects zero personal data).
- **Is all user data encrypted in transit?** Yes (HTTPS / TLS 1.3).
- **Do you provide a way for users to request data deletion?** Yes (In-app one-click deletion under *Settings > Delete Account* and via public web portal: [https://get-studytimer.vercel.app/delete-account.html](https://get-studytimer.vercel.app/delete-account.html)).
- **Is account creation mandatory?** No (Full offline mode available without account or internet).

### B. Data Types Collected & Handled

| Data Type | Category | Purpose | Shared with 3rd Parties? | Optional / Mandatory |
| :--- | :--- | :--- | :--- | :--- |
| **Email Address** | Personal Info | Account Management & Authentication | No | Optional (Guest mode available) |
| **Name / Username** | Personal Info | Student Profile & Leaderboard Display | No | Optional (Guest mode available) |
| **Avatar Photo** | Photos & Videos | Optional Profile Customization | No | Optional (Stored in private Supabase bucket) |
| **App Activity / Timer Logs** | App Activity | App Functionality (Study Analytics, Sync) | No | Stored locally / Synced on user login |
| **Crash & Diagnostics** | Diagnostics | App Functionality & Bug Hotfixes | No | Optional (User feedback / scrubbed stack traces) |

*Note: No financial info, precise GPS location, device contacts, microphone audio recordings, or SMS messages are accessed, collected, or transmitted.*

---

## 3. Third-Party SDK & Service Disclosures
- **Supabase:** Used strictly as the backend cloud database, storage, and authentication handler for users who opt into cloud sync. Protected with PostgreSQL Row Level Security (RLS).
- **Google Sign-In / OAuth:** Used strictly for identity verification.
- **Zero Advertising or Data Broker SDKs:** No AdMob, Unity Ads, Meta Audience Network, AppsFlyer, or commercial tracking brokers are included in the binary.

---

## 4. Legal Endpoints & Support
- **Privacy Policy:** [https://get-studytimer.vercel.app/privacy.html](https://get-studytimer.vercel.app/privacy.html)
- **Terms of Service:** [https://get-studytimer.vercel.app/terms.html](https://get-studytimer.vercel.app/terms.html)
- **Account Deletion Portal:** [https://get-studytimer.vercel.app/delete-account.html](https://get-studytimer.vercel.app/delete-account.html)
- **Support Contact:** `studytimer737@gmail.com`
