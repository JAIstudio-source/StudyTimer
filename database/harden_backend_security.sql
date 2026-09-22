-- =========================================================================
-- StudyTimer Master Backend Hardening & Anti-Cheat Security Script
-- (Execute in Supabase Dashboard -> SQL Editor -> New Query -> Run)
-- =========================================================================

-- 1. ADD SECURITY & ANTI-CHEAT COLUMNS (IF NOT PRESENT)
ALTER TABLE IF EXISTS public.daily_leaderboard ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.user_sync_profiles ADD COLUMN IF NOT EXISTS is_banned BOOLEAN DEFAULT FALSE;
ALTER TABLE IF EXISTS public.user_sync_snapshots ADD COLUMN IF NOT EXISTS client_timestamp BIGINT DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_leaderboard_banned ON public.daily_leaderboard(is_banned);

-- 2. HARDENED RPC: Record Completed Study Session (Anti-Cheat & Rate-Clamped)
CREATE OR REPLACE FUNCTION public.record_study_session_leaderboard(
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT,
    p_duration_seconds INT,
    p_subject TEXT DEFAULT '',
    p_subject_color TEXT DEFAULT '#3b82f6',
    p_study_date DATE DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    v_clamped_duration INT;
    v_date DATE;
    v_existing_total INT;
    v_existing_last_active TIMESTAMPTZ;
    v_is_banned BOOLEAN;
    v_time_diff_secs INT;
BEGIN
    -- Input sanitization
    IF p_user_id IS NULL OR TRIM(p_user_id) = '' OR TRIM(p_user_id) = 'null' THEN
        RETURN;
    END IF;

    -- Date bounds sanity check: Past 3 days to +1 day (handles international timezones)
    v_date := COALESCE(p_study_date, CURRENT_DATE);
    IF v_date < (CURRENT_DATE - INTERVAL '3 days')::DATE OR v_date > (CURRENT_DATE + INTERVAL '1 day')::DATE THEN
        v_date := CURRENT_DATE;
    END IF;

    -- Max single session cap: 14 hours (50,400 seconds)
    v_clamped_duration := LEAST(GREATEST(p_duration_seconds, 0), 50400);
    IF v_clamped_duration <= 0 THEN
        RETURN;
    END IF;

    -- Shadowban & Velocity Check
    SELECT total_seconds, last_active_at, COALESCE(is_banned, false)
    INTO v_existing_total, v_existing_last_active, v_is_banned
    FROM public.daily_leaderboard
    WHERE user_id = p_user_id AND study_date = v_date;

    IF v_is_banned IS TRUE THEN
        RETURN;
    END IF;

    -- Anti-Spam burst flood protection (prevent automated script loop attacks)
    IF v_existing_last_active IS NOT NULL THEN
        v_time_diff_secs := EXTRACT(EPOCH FROM (NOW() - v_existing_last_active))::INT;
        IF v_time_diff_secs < 10 AND v_clamped_duration > 3600 THEN
            v_clamped_duration := 300; -- clamp burst to 5 mins max
        END IF;
    END IF;

    -- Insert or Atomic Increment with Maximum Daily Human Cap (18 hours = 64,800s)
    INSERT INTO public.daily_leaderboard (
        user_id,
        user_name,
        avatar_url,
        study_date,
        total_seconds,
        is_studying,
        current_subject,
        subject_color,
        is_banned,
        last_active_at,
        updated_at
    )
    VALUES (
        p_user_id,
        COALESCE(NULLIF(TRIM(p_user_name), ''), 'Student'),
        COALESCE(p_avatar_url, ''),
        v_date,
        LEAST(v_clamped_duration, 64800),
        FALSE,
        COALESCE(p_subject, ''),
        COALESCE(p_subject_color, '#3b82f6'),
        FALSE,
        NOW(),
        NOW()
    )
    ON CONFLICT (user_id, study_date) DO UPDATE SET
        user_name = CASE WHEN EXCLUDED.user_name <> 'Student' THEN EXCLUDED.user_name ELSE daily_leaderboard.user_name END,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE daily_leaderboard.avatar_url END,
        total_seconds = LEAST(daily_leaderboard.total_seconds + v_clamped_duration, 64800),
        is_studying = FALSE,
        current_subject = CASE WHEN EXCLUDED.current_subject <> '' THEN EXCLUDED.current_subject ELSE daily_leaderboard.current_subject END,
        subject_color = CASE WHEN EXCLUDED.subject_color <> '' THEN EXCLUDED.subject_color ELSE daily_leaderboard.subject_color END,
        last_active_at = NOW(),
        updated_at = NOW()
    WHERE daily_leaderboard.is_banned = FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. HARDENED RPC: Live Study Progress Sync (Velocity-Clamped & Real-World Rate Limited)
CREATE OR REPLACE FUNCTION public.sync_study_progress_leaderboard(
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT,
    p_incremental_seconds INT,
    p_is_studying BOOLEAN,
    p_subject TEXT DEFAULT '',
    p_subject_color TEXT DEFAULT '#3b82f6',
    p_study_date DATE DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    v_clamped_seconds INT;
    v_date DATE;
    v_existing_last_active TIMESTAMPTZ;
    v_is_banned BOOLEAN;
    v_elapsed_secs INT;
    v_allowed_max INT;
BEGIN
    IF p_user_id IS NULL OR TRIM(p_user_id) = '' OR TRIM(p_user_id) = 'null' THEN
        RETURN;
    END IF;

    v_date := COALESCE(p_study_date, CURRENT_DATE);
    IF v_date < (CURRENT_DATE - INTERVAL '3 days')::DATE OR v_date > (CURRENT_DATE + INTERVAL '1 day')::DATE THEN
        v_date := CURRENT_DATE;
    END IF;

    -- Heartbeat max clamp: 600s (10 mins) per ping
    v_clamped_seconds := LEAST(GREATEST(p_incremental_seconds, 0), 600);

    SELECT last_active_at, COALESCE(is_banned, false)
    INTO v_existing_last_active, v_is_banned
    FROM public.daily_leaderboard
    WHERE user_id = p_user_id AND study_date = v_date;

    IF v_is_banned IS TRUE THEN
        RETURN;
    END IF;

    -- Enforce real-world elapsed time velocity limit (prevents rapid REST call spoofing)
    IF v_existing_last_active IS NOT NULL AND v_clamped_seconds > 0 THEN
        v_elapsed_secs := GREATEST(EXTRACT(EPOCH FROM (NOW() - v_existing_last_active))::INT, 1);
        v_allowed_max := GREATEST(v_elapsed_secs * 2, 60);
        v_clamped_seconds := LEAST(v_clamped_seconds, v_allowed_max);
    END IF;

    INSERT INTO public.daily_leaderboard (
        user_id,
        user_name,
        avatar_url,
        study_date,
        total_seconds,
        is_studying,
        current_subject,
        subject_color,
        is_banned,
        last_active_at,
        updated_at
    )
    VALUES (
        p_user_id,
        COALESCE(NULLIF(TRIM(p_user_name), ''), 'Student'),
        COALESCE(p_avatar_url, ''),
        v_date,
        v_clamped_seconds,
        p_is_studying,
        COALESCE(p_subject, ''),
        COALESCE(p_subject_color, '#3b82f6'),
        FALSE,
        NOW(),
        NOW()
    )
    ON CONFLICT (user_id, study_date) DO UPDATE SET
        user_name = CASE WHEN EXCLUDED.user_name <> 'Student' THEN EXCLUDED.user_name ELSE daily_leaderboard.user_name END,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE daily_leaderboard.avatar_url END,
        total_seconds = LEAST(daily_leaderboard.total_seconds + v_clamped_seconds, 64800),
        is_studying = EXCLUDED.is_studying,
        current_subject = CASE WHEN EXCLUDED.current_subject <> '' THEN EXCLUDED.current_subject ELSE daily_leaderboard.current_subject END,
        subject_color = CASE WHEN EXCLUDED.subject_color <> '' THEN EXCLUDED.subject_color ELSE daily_leaderboard.subject_color END,
        last_active_at = NOW(),
        updated_at = NOW()
    WHERE daily_leaderboard.is_banned = FALSE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. HARDENED LEADERBOARD QUERIES (Excludes Shadowbanned & Stale Presence)
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
    WHERE d.study_date = p_date 
      AND (d.total_seconds > 0 OR d.is_studying = true)
      AND (d.is_banned IS FALSE OR d.is_banned IS NULL)
    ORDER BY d.total_seconds DESC, d.last_active_at DESC
    LIMIT LEAST(p_limit, 50);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE FUNCTION public.get_weekly_leaderboard(
    p_start_date DATE DEFAULT (CURRENT_DATE - INTERVAL '6 days')::DATE,
    p_end_date DATE DEFAULT CURRENT_DATE,
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
    WITH weekly_agg AS (
        SELECT
            d.user_id,
            MAX(d.user_name) AS user_name,
            MAX(d.avatar_url) AS avatar_url,
            SUM(d.total_seconds)::INT AS total_seconds,
            BOOL_OR(d.is_studying AND d.last_active_at > (NOW() - INTERVAL '3 minutes')) AS is_studying,
            MAX(d.current_subject) FILTER (WHERE d.study_date = p_end_date) AS current_subject,
            MAX(d.subject_color) FILTER (WHERE d.study_date = p_end_date) AS subject_color,
            MAX(d.last_active_at) AS last_active_at
        FROM public.daily_leaderboard d
        WHERE d.study_date >= p_start_date AND d.study_date <= p_end_date
          AND (d.is_banned IS FALSE OR d.is_banned IS NULL)
        GROUP BY d.user_id
        HAVING SUM(d.total_seconds) > 0 OR BOOL_OR(d.is_studying) = true
    )
    SELECT
        ROW_NUMBER() OVER (ORDER BY w.total_seconds DESC, w.last_active_at DESC) AS rank,
        w.user_id,
        w.user_name,
        w.avatar_url,
        w.total_seconds,
        w.is_studying,
        COALESCE(w.current_subject, ''),
        COALESCE(w.subject_color, '#3b82f6'),
        w.last_active_at
    FROM weekly_agg w
    ORDER BY w.total_seconds DESC, w.last_active_at DESC
    LIMIT LEAST(p_limit, 50);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. IDEMPOTENT OFFLINE DELTA SYNC RPC (Safe WiFi Reconnect & Deduplication)
CREATE OR REPLACE FUNCTION public.sync_offline_study_sessions(
    p_user_id TEXT,
    p_sessions JSONB
)
RETURNS JSONB AS $$
DECLARE
    v_item JSONB;
    v_count INT := 0;
BEGIN
    IF p_user_id IS NULL OR p_sessions IS NULL OR jsonb_array_length(p_sessions) = 0 THEN
        RETURN jsonb_build_object('success', false, 'processed', 0);
    END IF;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_sessions) LOOP
        INSERT INTO public.user_study_sessions (
            session_uuid,
            user_id,
            study_date,
            start_time,
            end_time,
            duration_secs,
            subject_id,
            subject_name,
            subject_color,
            platform,
            is_deleted,
            updated_at
        )
        VALUES (
            v_item->>'session_uuid',
            p_user_id,
            COALESCE(v_item->>'study_date', CURRENT_DATE::text),
            COALESCE((v_item->>'start_time')::BIGINT, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000),
            COALESCE((v_item->>'end_time')::BIGINT, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000),
            LEAST(GREATEST(COALESCE((v_item->>'duration_secs')::INT, 0), 0), 50400),
            COALESCE(v_item->>'subject_id', 'general'),
            COALESCE(v_item->>'subject_name', 'General'),
            COALESCE(v_item->>'subject_color', '#3b82f6'),
            COALESCE(v_item->>'platform', 'android'),
            COALESCE((v_item->>'is_deleted')::BOOLEAN, false),
            COALESCE((v_item->>'updated_at')::BIGINT, EXTRACT(EPOCH FROM NOW())::BIGINT * 1000)
        )
        ON CONFLICT (session_uuid) DO UPDATE SET
            is_deleted = EXCLUDED.is_deleted,
            subject_name = EXCLUDED.subject_name,
            subject_color = EXCLUDED.subject_color,
            updated_at = EXCLUDED.updated_at
        WHERE EXCLUDED.updated_at >= user_study_sessions.updated_at;

        v_count := v_count + 1;
    END LOOP;

    RETURN jsonb_build_object('success', true, 'processed', v_count);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. RELOAD SCHEMA CACHE IMMEDIATELY
NOTIFY pgrst, 'reload schema';
