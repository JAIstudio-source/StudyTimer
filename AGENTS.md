# 🛡️ StudyTimer — AI Agent Engineering & Security Rules

## MANDATORY SECURITY & ARCHITECTURE DIRECTIVES

Every AI Agent and developer working on StudyTimer MUST strictly comply with the following permanent security mandates:

### 1. Zero Public Admin Panels / Web Dashboards
- **NEVER** create client-side admin panels, admin HTML pages (`admin/index.html`), or telemetry inspection UIs in public website roots or static deployment directories.
- Static hosting platforms (e.g., Vercel, Netlify, GitHub Pages) deploy all HTML/JS files to public URLs without server authentication.
- Client-side password prompts in JavaScript or local storage offer **zero security** and leak database queries and business logic.
- All administrative triage, user ban management, and database inspections MUST be conducted directly through the **Supabase Dashboard** or via authenticated backend APIs.

### 2. Strict Database Row Level Security (RLS) Enforcement
- **NEVER** write or execute SQL scripts that disable Row Level Security (`DISABLE ROW LEVEL SECURITY`).
- **NEVER** create wildcard open policies (e.g. `CREATE POLICY ... FOR ALL USING (true) WITH CHECK (true)`).
- All mutations and sensitive queries must use constrained, rate-clamped PostgreSQL `SECURITY DEFINER` functions (e.g. `record_study_session_leaderboard`, `sync_study_progress_leaderboard`, `sync_offline_study_sessions`).

### 3. Protection of Diagnostics & Credentials
- **NEVER** commit HTTP Archive dumps (`.har`), memory dumps (`.hprof`, `.dump`), or API secrets into git repositories.
- `service_role` keys or secret environment variables must NEVER be bundled in frontend JavaScript or mobile client code.
- `robots.txt` must NEVER advertise secret or internal URL paths.

### 4. Continuous Automated Verification
- Always execute `npm test` before committing changes to ensure that:
  - No `admin/` directory exists.
  - Zero `.har` or `.dump` capture files exist.
  - All database SQL scripts strictly maintain Row Level Security.
