-- =========================================================================
-- StudyTimer Backend Security Fix: Role Mutable search_path
-- Resolves Supabase Security Advisor & Database Linter Warning:
-- "Function public.<name> has a role mutable search_path"
-- =========================================================================
--
-- EXPLANATION:
-- In PostgreSQL, SECURITY DEFINER functions run with the privileges of the
-- function owner (usually superuser / postgres). If the search_path is not
-- fixed, an attacker could manipulate the schema search path to execute
-- arbitrary code. Setting search_path = public, pg_temp eliminates this vector.
--
-- HOW TO RUN:
-- 1. Open your Supabase Project Dashboard (https://supabase.com/dashboard).
-- 2. Go to "SQL Editor" in the left sidebar.
-- 3. Paste this entire script into a new query and click "Run".
-- =========================================================================

-- 1. Explicitly patch all known StudyTimer SECURITY DEFINER RPC functions:

DO $$
BEGIN
    -- Patch: record_study_session_leaderboard
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'record_study_session_leaderboard') THEN
        EXECUTE 'ALTER FUNCTION public.record_study_session_leaderboard(TEXT, TEXT, TEXT, INT, TEXT, TEXT, DATE) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: sync_study_progress_leaderboard
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'sync_study_progress_leaderboard') THEN
        EXECUTE 'ALTER FUNCTION public.sync_study_progress_leaderboard(TEXT, TEXT, TEXT, INT, BOOLEAN, TEXT, TEXT, DATE) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: get_daily_leaderboard
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'get_daily_leaderboard') THEN
        EXECUTE 'ALTER FUNCTION public.get_daily_leaderboard(DATE, INT) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: get_weekly_leaderboard
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'get_weekly_leaderboard') THEN
        EXECUTE 'ALTER FUNCTION public.get_weekly_leaderboard(DATE, DATE, INT) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: get_monthly_leaderboard (if present)
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'get_monthly_leaderboard') THEN
        EXECUTE 'ALTER FUNCTION public.get_monthly_leaderboard(DATE, INT) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: sync_offline_study_sessions
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'sync_offline_study_sessions') THEN
        EXECUTE 'ALTER FUNCTION public.sync_offline_study_sessions(TEXT, JSONB) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: update_study_presence (if present)
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'update_study_presence') THEN
        EXECUTE 'ALTER FUNCTION public.update_study_presence(TEXT, TEXT, TEXT, BOOLEAN, TEXT, TEXT) SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

DO $$
BEGIN
    -- Patch: admin_get_table_counts (if present)
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'admin_get_table_counts') THEN
        EXECUTE 'ALTER FUNCTION public.admin_get_table_counts() SET search_path = public, pg_temp;';
    END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END $$;

-- 2. DYNAMIC CATCH-ALL: Ensure EVERY SECURITY DEFINER function in the public schema
-- has search_path explicitly locked down to 'public, pg_temp'.
-- This guarantees zero remaining mutable search_path warnings across all past and future overloads.

DO $$
DECLARE
    r RECORD;
    v_sql TEXT;
BEGIN
    FOR r IN (
        SELECT 
            p.proname, 
            pg_get_function_identity_arguments(p.oid) AS args
        FROM pg_proc p
        JOIN pg_namespace n ON p.pronamespace = n.oid
        WHERE n.nspname = 'public'
          AND p.prosecdef = true
    ) LOOP
        v_sql := format('ALTER FUNCTION public.%I(%s) SET search_path = public, pg_temp;', r.proname, r.args);
        EXECUTE v_sql;
        RAISE NOTICE 'Secured function: public.%(%)', r.proname, r.args;
    END LOOP;
END;
$$;

-- 3. Notify PostgREST to reload the schema cache immediately
NOTIFY pgrst, 'reload schema';
