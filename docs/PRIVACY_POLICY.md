# Privacy Policy for StudyTimer

> **Last Updated:** November 2026 &bull; **Effective Date:** August 14, 2026 &bull; **Applicable to:** StudyTimer Android App & Web Platform

At **StudyTimer**, we believe student focus tools should protect user privacy, not exploit it. We built StudyTimer from the ground up with a strict **offline-first architecture**, zero third-party advertising trackers, and transparent data practices fully compliant with India’s **Digital Personal Data Protection Act, 2023 (DPDP Act)** and global privacy regulations.

---

## 1. Core Architecture: Offline-First by Design

StudyTimer operates primarily on your device without requiring continuous internet connectivity or mandatory account registration:
- **On Your Android Device:** 100% of your study timers, custom subjects, color tags, Pomodoro intervals, and daily streaks are stored locally in an on-device encrypted SQLite database. You can use the full application completely offline without creating an account or sharing personal information.
- **On Browser (Try in Browser):** If you use our browser-based timer at [https://studytimer.site/study](https://studytimer.site/study), sessions and preferences are stored locally in your browser's private IndexedDB and LocalStorage cache.
- **Optional Cloud Synchronization:** Cloud features (multi-device backup, streak restoration, opt-in community leaderboard) are completely optional and only activated when you explicitly sign in.

---

## 2. Information We Process & How It Is Used

We practice strict **data minimization**. We only process data necessary to provide core study features:
1. **Study Logs & Metrics:** Session start/end timestamps, elapsed study duration, subject titles, goal targets, and streak records.
2. **Account Identifiers (Optional):** If you sign in with Google or Email for cloud backups, we store your unique user ID and email strictly to authenticate and secure your private data vault.
3. **Public Student Profile (Opt-In):** If you customize your student profile, your chosen display name, username, bio, study goal/exam, and optional avatar image are stored in our secure database. All profile bios and names pass through an automated profanity filter to maintain a safe, student-friendly environment.
4. **Leaderboard & Live Status (Opt-In):** If you enable public leaderboard participation or live study status, your current session duration and subject tag are visible to other students in the community. You can disable this visibility at any time in Settings.
5. **Fair-Play & Anti-Cheat Telemetry:** To maintain genuine student motivation on public leaderboards, the system verifies study streak integrity and uses automated velocity/inactivity checks for long continuous sessions (preventing unattended 24/7 background running).

---

## 3. Zero Third-Party Advertising & No Data Selling

**We do not sell, rent, monetize, or trade student data with any third-party brokers or advertisers.**
- The StudyTimer Android app contains **zero third-party advertisement SDKs** (no AdMob, Unity Ads, Meta Audience Network, or data broker trackers).
- We do not perform cross-app tracking, device fingerprinting, or behavioral profiling for commercial advertising.
- Cloud database infrastructure is hosted securely on Supabase PostgreSQL with strict Row-Level Security (RLS) policies and HTTPS/TLS 1.3 encryption in transit and at rest.

---

## 4. Compliance with the DPDP Act, 2023 (Digital Personal Data Protection Act)

StudyTimer fully complies with India’s **Digital Personal Data Protection Act, 2023 (DPDP Act)** regarding the processing of digital personal data:
- **Right to Access & Summary:** View a complete summary of all personal study data and profile information stored in our systems at any time directly through the app or web interface.
- **Right to Correction & Updating:** Edit, modify, or correct your profile details, avatar, study goals, or session tags instantly via the profile editor.
- **Right to Erasure & Withdrawal of Consent:** You have the unconditional right to withdraw consent and delete your digital personal data. You can delete your account and all associated cloud records in 1-click via our [Self-Serve Account Deletion Portal](https://studytimer.site/delete-account.html) or within app Settings.
- **Right of Grievance Redressal:** Access readily available grievance redressal provided by our Data Protection & Grievance Officer.
- **Right to Nominate:** In the event of death or incapacity, a user or their lawful representative may nominate an individual to exercise their data protection rights.

---

## 5. Protection of Children & Minor Students

Recognizing that many StudyTimer users are high school and college students, we implement special safeguards in accordance with Section 9 of the DPDP Act:
- We do not engage in targeted advertising directed at minors or students.
- We do not track or profile children’s browsing behaviors across the web.
- We enforce automated moderation and profanity filters across all student usernames and bios to ensure a safe learning space.

---

## 6. Data Retention & Permanent Deletion

Local study logs remain on your Android device for as long as the application remains installed. When cloud sync is enabled, snapshots are securely stored to allow students to restore their data if they change devices.

If you submit a deletion request via our [Account Deletion Portal](https://studytimer.site/delete-account.html), your cloud profile, study backups, and leaderboard entries are immediately and permanently erased from our production servers.

---

## 7. Designated Grievance Redressal Officer

In accordance with the DPDP Act 2023 and Information Technology rules, if you have questions, feedback, concerns, or grievances regarding personal data:

- **Grievance / Data Protection Officer:** StudyTimer Privacy & Moderation Team
- **Official Contact Email:** `studytimer737@gmail.com`
- **Response Turnaround Time:** Within 48 hours
- **Direct Support Portal:** [Contact Support Form](https://studytimer.site/contact.html)
- **Official Website:** [https://studytimer.site/](https://studytimer.site/)
