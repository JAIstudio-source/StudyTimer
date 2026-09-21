-- =========================================================================
-- StudyTimer Cloud Sync & Backup Engine 2.0 Schema (Supabase SQL)
-- Immutable Snapshots, Granular Delta Sessions, and Strict Identity Scoping
-- =========================================================================

-- 1. Granular Study Sessions Table (Append-Only Delta Sync)
CREATE TABLE IF NOT EXISTS public.user_study_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_uuid TEXT NOT NULL UNIQUE,
    user_id TEXT NOT NULL,
    study_date TEXT NOT NULL,                  -- Format: YYYY-MM-DD
    start_time BIGINT NOT NULL,                -- Unix Epoch Milliseconds
    end_time BIGINT NOT NULL,                  -- Unix Epoch Milliseconds
    duration_secs INT NOT NULL,                -- Duration in seconds
    subject_id TEXT DEFAULT 'general',
    subject_name TEXT DEFAULT 'General',
    subject_color TEXT DEFAULT '#3b82f6',
    platform TEXT DEFAULT 'android',           -- 'android' or 'web'
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at BIGINT NOT NULL                 -- Client timestamp for LWW resolution
);

CREATE INDEX IF NOT EXISTS idx_study_sessions_user_id ON public.user_study_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_study_sessions_date ON public.user_study_sessions(study_date);
CREATE INDEX IF NOT EXISTS idx_study_sessions_start_time ON public.user_study_sessions(start_time);
CREATE INDEX IF NOT EXISTS idx_study_sessions_uuid ON public.user_study_sessions(session_uuid);

-- 2. Versioned Cloud Snapshots (Immutable Point-in-Time Backups)
CREATE TABLE IF NOT EXISTS public.user_sync_snapshots (
    id BIGSERIAL PRIMARY KEY,
    snapshot_uuid TEXT NOT NULL UNIQUE,
    user_id TEXT NOT NULL,
    snapshot_tag TEXT DEFAULT 'daily_auto',    -- 'daily_auto', 'pre_auth', 'manual_export', 'pre_delete'
    snapshot_date TEXT NOT NULL,               -- Format: YYYY-MM-DD
    prefs_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    timeline_data JSONB NOT NULL DEFAULT '[]'::jsonb,
    subject_tags_data JSONB DEFAULT '{}'::jsonb,
    session_count INT DEFAULT 0,
    total_study_secs BIGINT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    client_timestamp BIGINT NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_sync_snapshots_user_id ON public.user_sync_snapshots(user_id);
CREATE INDEX IF NOT EXISTS idx_sync_snapshots_date ON public.user_sync_snapshots(snapshot_date);
CREATE INDEX IF NOT EXISTS idx_sync_snapshots_created ON public.user_sync_snapshots(created_at);

-- 3. User Cloud Sync Profile & Config State
CREATE TABLE IF NOT EXISTS public.user_sync_profiles (
    user_id TEXT PRIMARY KEY,
    user_name TEXT,
    user_email TEXT,
    profile_image_uri TEXT,
    current_streak INT DEFAULT 0,
    daily_goal_minutes INT DEFAULT 120,
    pomo_focus_minutes INT DEFAULT 25,
    pomo_break_minutes INT DEFAULT 5,
    pomo_long_break_minutes INT DEFAULT 15,
    pomo_total_cycles INT DEFAULT 4,
    custom_timer_minutes INT DEFAULT 45,
    active_subject_id TEXT DEFAULT 'general',
    last_study_date TEXT,
    last_modified_timestamp BIGINT NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- =========================================================================
-- 4. 30-DAY RETENTION & PERMANENT LATEST SAVE PROTECTION
-- Auto-prunes older historical backups while ALWAYS safeguarding the latest full backup
-- =========================================================================

CREATE OR REPLACE FUNCTION public.prune_old_user_snapshots(retention_days INT DEFAULT 30)
RETURNS INT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    deleted_count INT := 0;
BEGIN
    WITH latest_snapshots AS (
        -- Identify the single most recent snapshot for every user (PROTECTED FROM DELETION)
        SELECT DISTINCT ON (user_id) id
        FROM public.user_sync_snapshots
        ORDER BY user_id, created_at DESC
    )
    DELETE FROM public.user_sync_snapshots
    WHERE created_at < NOW() - (retention_days || ' days')::INTERVAL
      AND id NOT IN (SELECT id FROM latest_snapshots);

    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$;

-- Trigger to auto-prune snapshots on new snapshot insertion
CREATE OR REPLACE FUNCTION public.trigger_prune_snapshots_on_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Keep at most 30 historical snapshots per user, always keeping the newest
    WITH user_older_snapshots AS (
        SELECT id
        FROM public.user_sync_snapshots
        WHERE user_id = NEW.user_id
        ORDER BY created_at DESC
        OFFSET 30
    )
    DELETE FROM public.user_sync_snapshots
    WHERE id IN (SELECT id FROM user_older_snapshots)
      AND created_at < NOW() - INTERVAL '30 days';

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_prune_snapshots_on_insert ON public.user_sync_snapshots;
CREATE TRIGGER trg_prune_snapshots_on_insert
    AFTER INSERT ON public.user_sync_snapshots
    FOR EACH ROW
    EXECUTE FUNCTION public.trigger_prune_snapshots_on_insert();

-- =========================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- Strict Isolation: Authenticated users can ONLY access their own data
-- =========================================================================

ALTER TABLE public.user_study_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_sync_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_sync_profiles ENABLE ROW LEVEL SECURITY;

-- user_study_sessions RLS
DROP POLICY IF EXISTS "Users can read own study sessions" ON public.user_study_sessions;
CREATE POLICY "Users can read own study sessions"
    ON public.user_study_sessions FOR SELECT
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

DROP POLICY IF EXISTS "Users can insert own study sessions" ON public.user_study_sessions;
CREATE POLICY "Users can insert own study sessions"
    ON public.user_study_sessions FOR INSERT
    WITH CHECK (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

DROP POLICY IF EXISTS "Users can update own study sessions" ON public.user_study_sessions;
CREATE POLICY "Users can update own study sessions"
    ON public.user_study_sessions FOR UPDATE
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

DROP POLICY IF EXISTS "Users can delete own study sessions" ON public.user_study_sessions;
CREATE POLICY "Users can delete own study sessions"
    ON public.user_study_sessions FOR DELETE
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

-- user_sync_snapshots RLS
DROP POLICY IF EXISTS "Users can read own snapshots" ON public.user_sync_snapshots;
CREATE POLICY "Users can read own snapshots"
    ON public.user_sync_snapshots FOR SELECT
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

DROP POLICY IF EXISTS "Users can insert own snapshots" ON public.user_sync_snapshots;
CREATE POLICY "Users can insert own snapshots"
    ON public.user_sync_snapshots FOR INSERT
    WITH CHECK (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

DROP POLICY IF EXISTS "Users can update own snapshots" ON public.user_sync_snapshots;
CREATE POLICY "Users can update own snapshots"
    ON public.user_sync_snapshots FOR UPDATE
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));

-- user_sync_profiles RLS
DROP POLICY IF EXISTS "Users can manage own sync profile" ON public.user_sync_profiles;
CREATE POLICY "Users can manage own sync profile"
    ON public.user_sync_profiles FOR ALL
    USING (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true))
    WITH CHECK (auth.uid()::text = user_id OR user_id = current_setting('request.jwt.claim.sub', true));
