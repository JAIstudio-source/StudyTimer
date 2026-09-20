-- =========================================================================
-- StudyTimer Daily & Weekly Leaderboard + Live Presence Schema (Supabase SQL)
-- Free Tier Optimized: Aggregated Rollups, Fast Indexes & Stale Presence Timeout
-- =========================================================================

-- 1. Create Daily Leaderboard Table
CREATE TABLE IF NOT EXISTS public.daily_leaderboard (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_name TEXT NOT NULL DEFAULT 'Student',
    avatar_url TEXT DEFAULT '',
    study_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_seconds INT NOT NULL DEFAULT 0,
    is_studying BOOLEAN NOT NULL DEFAULT FALSE,
    current_subject TEXT DEFAULT '',
    subject_color TEXT DEFAULT '#3b82f6',
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_user_daily_entry UNIQUE (user_id, study_date),
    CONSTRAINT anti_cheat_max_daily_seconds CHECK (total_seconds >= 0 AND total_seconds <= 86400)
);

-- 2. Fast Lookup & Ranking Indexes
CREATE INDEX IF NOT EXISTS idx_leaderboard_date_seconds ON public.daily_leaderboard(study_date, total_seconds DESC);
CREATE INDEX IF NOT EXISTS idx_leaderboard_user_date ON public.daily_leaderboard(user_id, study_date);
CREATE INDEX IF NOT EXISTS idx_leaderboard_active ON public.daily_leaderboard(is_studying, last_active_at);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.daily_leaderboard ENABLE ROW LEVEL SECURITY;

-- Clean existing policies
DROP POLICY IF EXISTS "Allow public read for leaderboard" ON public.daily_leaderboard;
DROP POLICY IF EXISTS "Allow anon upsert for leaderboard" ON public.daily_leaderboard;
DROP POLICY IF EXISTS "Allow anon update for leaderboard" ON public.daily_leaderboard;

-- RLS Policies
CREATE POLICY "Allow public read for leaderboard" ON public.daily_leaderboard 
    FOR SELECT USING (true);

CREATE POLICY "Allow anon upsert for leaderboard" ON public.daily_leaderboard 
    FOR ALL USING (true) WITH CHECK (true);

-- =========================================================================
-- 4. RPC FUNCTION: Update Live Study Presence & Heartbeat
-- =========================================================================
CREATE OR REPLACE FUNCTION public.update_study_presence(
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT,
    p_is_studying BOOLEAN,
    p_current_subject TEXT DEFAULT '',
    p_subject_color TEXT DEFAULT '#3b82f6'
)
RETURNS VOID AS $$
BEGIN
    INSERT INTO public.daily_leaderboard (
        user_id,
        user_name,
        avatar_url,
        study_date,
        total_seconds,
        is_studying,
        current_subject,
        subject_color,
        last_active_at,
        updated_at
    )
    VALUES (
        p_user_id,
        COALESCE(NULLIF(p_user_name, ''), 'Student'),
        COALESCE(p_avatar_url, ''),
        CURRENT_DATE,
        0,
        p_is_studying,
        COALESCE(p_current_subject, ''),
        COALESCE(p_subject_color, '#3b82f6'),
        NOW(),
        NOW()
    )
    ON CONFLICT (user_id, study_date) DO UPDATE SET
        user_name = CASE WHEN EXCLUDED.user_name <> 'Student' THEN EXCLUDED.user_name ELSE daily_leaderboard.user_name END,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE daily_leaderboard.avatar_url END,
        is_studying = EXCLUDED.is_studying,
        current_subject = EXCLUDED.current_subject,
        subject_color = EXCLUDED.subject_color,
        last_active_at = NOW(),
        updated_at = NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =========================================================================
-- 5. RPC FUNCTION: Record Completed Study Session (Atomic Increment)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.record_study_session_leaderboard(
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT,
    p_duration_seconds INT,
    p_subject TEXT DEFAULT '',
    p_subject_color TEXT DEFAULT '#3b82f6'
)
RETURNS VOID AS $$
DECLARE
    v_clamped_duration INT;
BEGIN
    -- Anti-cheat sanity check: max single session increment = 14 hours (50,400s)
    v_clamped_duration := LEAST(GREATEST(p_duration_seconds, 0), 50400);

    INSERT INTO public.daily_leaderboard (
        user_id,
        user_name,
        avatar_url,
        study_date,
        total_seconds,
        is_studying,
        current_subject,
        subject_color,
        last_active_at,
        updated_at
    )
    VALUES (
        p_user_id,
        COALESCE(NULLIF(p_user_name, ''), 'Student'),
        COALESCE(p_avatar_url, ''),
        CURRENT_DATE,
        v_clamped_duration,
        FALSE,
        COALESCE(p_subject, ''),
        COALESCE(p_subject_color, '#3b82f6'),
        NOW(),
        NOW()
    )
    ON CONFLICT (user_id, study_date) DO UPDATE SET
        user_name = CASE WHEN EXCLUDED.user_name <> 'Student' THEN EXCLUDED.user_name ELSE daily_leaderboard.user_name END,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE daily_leaderboard.avatar_url END,
        total_seconds = LEAST(daily_leaderboard.total_seconds + v_clamped_duration, 86400),
        is_studying = FALSE,
        current_subject = EXCLUDED.current_subject,
        subject_color = EXCLUDED.subject_color,
        last_active_at = NOW(),
        updated_at = NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =========================================================================
-- 6. RPC FUNCTION: Get Daily Leaderboard Top 25 with Stale Presence Filter
-- (If user has been inactive for > 3 minutes, is_studying is marked FALSE)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.get_daily_leaderboard(
    p_date DATE DEFAULT CURRENT_DATE,
    p_limit INT DEFAULT 25
)
RETURNS TABLE (
    rank BIGINT,
    user_id TEXT,
    user_name TEXT,
    avatar_url TEXT,
    total_seconds INT,
    is_studying BOOLEAN,
    current_subject TEXT,
    subject_color TEXT,
    last_active_at TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        ROW_NUMBER() OVER (ORDER BY d.total_seconds DESC, d.last_active_at DESC) AS rank,
        d.user_id,
        d.user_name,
        d.avatar_url,
        d.total_seconds,
        (d.is_studying AND d.last_active_at > (NOW() - INTERVAL '3 minutes')) AS is_studying,
        d.current_subject,
        d.subject_color,
        d.last_active_at
    FROM public.daily_leaderboard d
    WHERE d.study_date = p_date AND (d.total_seconds > 0 OR d.is_studying = true)
    ORDER BY d.total_seconds DESC, d.last_active_at DESC
    LIMIT LEAST(p_limit, 50);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
