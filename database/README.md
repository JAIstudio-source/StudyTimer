# 🗄️ StudyTimer Database Migrations & Schemas (Supabase PostgreSQL)

This directory contains production SQL migration scripts, schema definitions, Row Level Security (RLS) policies, and hardened stored procedures for the StudyTimer backend hosted on Supabase.

---

## 📜 Migration Catalog

| Script | Purpose & Key Components |
| :--- | :--- |
| **`cloud_sync_v2_schema.sql`** | Multi-device study session sync (`user_sync_data`, `offline_study_sessions`), device tracking, and optimistic locking. |
| **`create_storage_avatars.sql`** | Supabase Storage bucket setup (`user-avatars`) for student profile images with size limits and public read / authenticated write policies. |
| **`harden_backend_security.sql`** | Comprehensive security hardening: rate-limiting clamps, velocity checks (max 18h daily cap), anti-cheat filters, and idempotent offline delta sync RPC (`sync_offline_study_sessions`). |
| **`leaderboard_schema.sql`** | Community leaderboard tables (`daily_leaderboard`, `weekly_leaderboard`, `monthly_leaderboard`), live study presence, and ranking calculation views. |
| **`schema_analytics.sql`** | Anonymous event telemetry, crash diagnostics, feature usage counters, and opt-in telemetry tables with strict privacy scrubbing. |
| **`secure_function_search_paths.sql`** | Hardens all `SECURITY DEFINER` functions with immutable search paths (`SET search_path = public, pg_temp;`) to protect against privilege escalation and resolve Supabase security advisory warnings. |
| **`server_cleanup_and_optimize.sql`** | Automated database indexes, vacuum optimizations, and orphan record pruning routines to keep free-tier storage lightweight. |
| **`terms_consent_schema.sql`** | Records student consent timestamps for Terms of Service and DPDP Act 2023 compliance audits. |

---

## 🛡️ Mandatory Security Rules

All database scripts in this directory must strictly adhere to the project security directives:
1. **Never disable RLS**: `DISABLE ROW LEVEL SECURITY` is permanently banned.
2. **No wildcard policies**: Open access rules (`CREATE POLICY ... FOR ALL USING (true) WITH CHECK (true)`) are strictly prohibited.
3. **Immutable Search Paths**: Every `SECURITY DEFINER` function must explicitly declare `SET search_path = public, pg_temp;`.
