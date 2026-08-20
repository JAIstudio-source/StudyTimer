# StudyTimer — Google Play Data Safety & App Compliance Declaration

## 1. Target Audience & Content Rating
- **Target Age Group:** 13 and older (General Audience / Students).
- **COPPA Compliance:** Not directed primarily at children under 13.
- **Ads / Monetization:** 100% Free. No advertisements, no in-app purchases (IAP), no subscriptions.
- **Generative AI Content:** No user-facing generative AI features present in the app.

---

## 2. Data Safety Questionnaire Answers (Google Play Console)

### A. Data Collection Overview
- **Does the app collect or share user data?** Yes (Only when users optionally sign in).
- **Is all user data encrypted in transit?** Yes (HTTPS / TLS 1.3).
- **Do you provide a way for users to request data deletion?** Yes (In-app account deletion and web portal: `https://get-studytimer.vercel.app/delete-account.html`).

### B. Data Types Collected & Handled

| Data Type | Category | Purpose | Shared with 3rd Parties? | Optional / Mandatory |
| :--- | :--- | :--- | :--- | :--- |
| **Email Address** | Personal Info | Account Management / Auth | No | Optional (Guest mode available) |
| **User ID / Name** | Personal Info | Account Management / Profile | No | Optional (Guest mode available) |
| **App Activity / Timer Logs** | App Activity | App Functionality (Study Analytics, Sync) | No | Stored locally / Synced on login |
| **Crash & Bug Reports** | Diagnostics | App Functionality / Bug Fixes | No | Optional (User-initiated feedback only) |

*Note: No financial info, precise location, contacts, photos, or audio recordings are collected or transmitted.*

---

## 3. Third-Party SDK & Service Disclosures
- **Supabase:** Used strictly as the backend database and authentication handler for users who opt into cloud sync.
- **Google Sign-In:** Used strictly for identity verification and OAuth authentication.
- **No third-party ad networks, tracking analytics SDKs, or data brokers are integrated.**

---

## 4. Legal Endpoints
- **Privacy Policy:** `https://get-studytimer.vercel.app/privacy.html`
- **Terms of Service:** `https://get-studytimer.vercel.app/terms.html`
- **Account Deletion Portal:** `https://get-studytimer.vercel.app/delete-account.html`
- **Support Contact:** `studytimer737@gmail.com`
