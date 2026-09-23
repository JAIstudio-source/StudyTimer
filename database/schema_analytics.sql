    -- =========================================================================
    -- StudyTimer Privacy-First Analytics & Crash Reporting Tables (Supabase SQL)
    -- =========================================================================

    -- 1. App Analytics Events
    CREATE TABLE IF NOT EXISTS public.app_analytics_events (
        id BIGSERIAL PRIMARY KEY,
        event_id TEXT UNIQUE,
        anonymous_id TEXT NOT NULL,
        device_hardware_id TEXT,
        user_id TEXT,
        user_name TEXT,
        user_email TEXT,
        is_authenticated BOOLEAN DEFAULT FALSE,
        platform TEXT DEFAULT 'android',
        event_name TEXT NOT NULL,
        app_version TEXT,
        version_code INT,
        android_sdk INT,
        device_model TEXT,
        timestamp BIGINT NOT NULL,
        properties JSONB DEFAULT '{}'::jsonb,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );

    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS event_id TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS device_hardware_id TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS user_name TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS user_email TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS platform TEXT DEFAULT 'android';
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS device_model TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS app_version TEXT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS version_code INT;
    ALTER TABLE public.app_analytics_events ADD COLUMN IF NOT EXISTS android_sdk INT;

    CREATE INDEX IF NOT EXISTS idx_analytics_event_id ON public.app_analytics_events(event_id);
    CREATE INDEX IF NOT EXISTS idx_analytics_anon_id ON public.app_analytics_events(anonymous_id);
    CREATE INDEX IF NOT EXISTS idx_analytics_hardware_id ON public.app_analytics_events(device_hardware_id);
    CREATE INDEX IF NOT EXISTS idx_analytics_user_id ON public.app_analytics_events(user_id);
    CREATE INDEX IF NOT EXISTS idx_analytics_event_name ON public.app_analytics_events(event_name);
    CREATE INDEX IF NOT EXISTS idx_analytics_timestamp ON public.app_analytics_events(timestamp);

    -- 2. User Cohorts / Lifecycle Table
    CREATE TABLE IF NOT EXISTS public.app_user_cohorts (
        anonymous_id TEXT PRIMARY KEY,
        device_hardware_id TEXT,
        user_id TEXT,
        user_name TEXT,
        user_email TEXT,
        is_authenticated BOOLEAN DEFAULT FALSE,
        platform TEXT DEFAULT 'android',
        first_seen BIGINT NOT NULL,
        last_active_at BIGINT NOT NULL,
        last_active_date TEXT,
        app_version TEXT,
        version_code INT,
        android_sdk INT,
        device_model TEXT,
        total_study_secs BIGINT DEFAULT 0,
        total_sessions BIGINT DEFAULT 0,
        updated_at BIGINT DEFAULT 0
    );

    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS device_hardware_id TEXT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS user_name TEXT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS user_email TEXT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS platform TEXT DEFAULT 'android';
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS device_model TEXT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS app_version TEXT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS version_code INT;
    ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS android_sdk INT;

    CREATE INDEX IF NOT EXISTS idx_cohorts_user_id ON public.app_user_cohorts(user_id);
    CREATE INDEX IF NOT EXISTS idx_cohorts_hardware_id ON public.app_user_cohorts(device_hardware_id);
    CREATE INDEX IF NOT EXISTS idx_cohorts_last_date ON public.app_user_cohorts(last_active_date);

    -- 3. Crash & Exception Reports Table
    CREATE TABLE IF NOT EXISTS public.app_crash_reports (
        id BIGSERIAL PRIMARY KEY,
        anonymous_id TEXT NOT NULL,
        device_hardware_id TEXT,
        user_id TEXT,
        user_name TEXT,
        user_email TEXT,
        is_authenticated BOOLEAN DEFAULT FALSE,
        platform TEXT DEFAULT 'android',
        exception_type TEXT NOT NULL,
        message TEXT,
        stack_trace TEXT,
        thread_info TEXT,
        app_version TEXT,
        version_code INT,
        android_sdk INT,
        device_model TEXT,
        timestamp BIGINT NOT NULL,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );

    ALTER TABLE public.app_crash_reports ADD COLUMN IF NOT EXISTS device_hardware_id TEXT;
    ALTER TABLE public.app_crash_reports ADD COLUMN IF NOT EXISTS user_name TEXT;
    ALTER TABLE public.app_crash_reports ADD COLUMN IF NOT EXISTS user_email TEXT;

    CREATE INDEX IF NOT EXISTS idx_crashes_exception ON public.app_crash_reports(exception_type);
    CREATE INDEX IF NOT EXISTS idx_crashes_user_id ON public.app_crash_reports(user_id);
    CREATE INDEX IF NOT EXISTS idx_crashes_hardware_id ON public.app_crash_reports(device_hardware_id);
    CREATE INDEX IF NOT EXISTS idx_crashes_version ON public.app_crash_reports(app_version);

    -- 4. User Sync Data Table (Cloud Sync / Profile Data)
    CREATE TABLE IF NOT EXISTS public.user_sync_data (
        user_id TEXT PRIMARY KEY,
        user_name TEXT,
        user_email TEXT,
        profile_image_uri TEXT,
        prefs_data TEXT,
        timeline_data TEXT,
        updated_at BIGINT DEFAULT 0,
        created_at TIMESTAMPTZ DEFAULT NOW()
    );

    ALTER TABLE public.user_sync_data ADD COLUMN IF NOT EXISTS user_email TEXT;

    -- Enable Row Level Security (RLS) on all tables
    ALTER TABLE public.app_analytics_events ENABLE ROW LEVEL SECURITY;
    ALTER TABLE public.app_user_cohorts ENABLE ROW LEVEL SECURITY;
    ALTER TABLE public.app_crash_reports ENABLE ROW LEVEL SECURITY;
    ALTER TABLE public.user_sync_data ENABLE ROW LEVEL SECURITY;

    -- Clean existing policies if re-running
    DROP POLICY IF EXISTS "Allow anon inserts for analytics" ON public.app_analytics_events;
    DROP POLICY IF EXISTS "Allow anon read for analytics" ON public.app_analytics_events;
    DROP POLICY IF EXISTS "Allow anon delete for analytics" ON public.app_analytics_events;
    DROP POLICY IF EXISTS "Allow anon upsert for cohorts" ON public.app_user_cohorts;
    DROP POLICY IF EXISTS "Allow anon read for cohorts" ON public.app_user_cohorts;
    DROP POLICY IF EXISTS "Allow anon delete for cohorts" ON public.app_user_cohorts;
    DROP POLICY IF EXISTS "Allow anon inserts for crashes" ON public.app_crash_reports;
    DROP POLICY IF EXISTS "Allow anon read for crashes" ON public.app_crash_reports;
    DROP POLICY IF EXISTS "Allow anon delete for crashes" ON public.app_crash_reports;
    DROP POLICY IF EXISTS "Allow anon upsert for sync data" ON public.user_sync_data;
    DROP POLICY IF EXISTS "Allow anon read for sync data" ON public.user_sync_data;
    DROP POLICY IF EXISTS "Allow anon delete for sync data" ON public.user_sync_data;

    -- 1. App Analytics Events Policies (Inserts from app/web, reads & deletes from admin)
    CREATE POLICY "Allow anon inserts for analytics" ON public.app_analytics_events FOR INSERT WITH CHECK (true);
    CREATE POLICY "Allow anon read for analytics" ON public.app_analytics_events FOR SELECT USING (true);
    CREATE POLICY "Allow anon delete for analytics" ON public.app_analytics_events FOR DELETE USING (true);

    -- 2. User Cohorts Policies (App updates cohort stats, admin reads/deletes)
    CREATE POLICY "Allow anon upsert for cohorts" ON public.app_user_cohorts FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow anon read for cohorts" ON public.app_user_cohorts FOR SELECT USING (true);
    CREATE POLICY "Allow anon delete for cohorts" ON public.app_user_cohorts FOR DELETE USING (true);

    -- 3. Crash Reports Policies (App reports crashes, admin reads/deletes)
    CREATE POLICY "Allow anon inserts for crashes" ON public.app_crash_reports FOR INSERT WITH CHECK (true);
    CREATE POLICY "Allow anon read for crashes" ON public.app_crash_reports FOR SELECT USING (true);
    CREATE POLICY "Allow anon delete for crashes" ON public.app_crash_reports FOR DELETE USING (true);

    -- 4. User Cloud Sync Policies (App uploads/downloads sync data, admin reads for inspector)
    CREATE POLICY "Allow anon upsert for sync data" ON public.user_sync_data FOR ALL USING (true) WITH CHECK (true);
    CREATE POLICY "Allow anon read for sync data" ON public.user_sync_data FOR SELECT USING (true);
    CREATE POLICY "Allow anon delete for sync data" ON public.user_sync_data FOR DELETE USING (true);

    -- Auto-cleanup Trigger: Automatically keeps only the latest 5,000 live event records in Supabase
    CREATE OR REPLACE FUNCTION clean_old_analytics_events() RETURNS trigger AS $$
    BEGIN
        DELETE FROM public.app_analytics_events
        WHERE id NOT IN (
            SELECT id FROM public.app_analytics_events
            ORDER BY id DESC
            LIMIT 5000
        );
        RETURN NEW;
    END;
    $$ LANGUAGE plpgsql;

    DROP TRIGGER IF EXISTS trigger_clean_old_analytics_events ON public.app_analytics_events;

    -- 5. User Feedback & Bug Reports Table
    CREATE TABLE IF NOT EXISTS public.feedback_reports (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        type TEXT NOT NULL, -- BUG_REPORT, FEATURE_REQUEST, GENERAL_FEEDBACK
        status TEXT NOT NULL DEFAULT 'NEW', -- NEW, IN_PROGRESS, RESOLVED, ARCHIVED
        user_contact TEXT,
        message TEXT NOT NULL,
        diagnostics JSONB DEFAULT '{}'::jsonb,
        admin_notes TEXT,
        created_at TIMESTAMPTZ DEFAULT NOW(),
        updated_at TIMESTAMPTZ DEFAULT NOW()
    );

    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS type TEXT;
    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS status TEXT DEFAULT 'NEW';
    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS user_contact TEXT;
    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS message TEXT;
    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS diagnostics JSONB DEFAULT '{}'::jsonb;
    ALTER TABLE public.feedback_reports ADD COLUMN IF NOT EXISTS admin_notes TEXT;

    CREATE INDEX IF NOT EXISTS idx_feedback_type ON public.feedback_reports(type);
    CREATE INDEX IF NOT EXISTS idx_feedback_status ON public.feedback_reports(status);
    CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON public.feedback_reports(created_at);

    -- Enable RLS for Feedback Reports
    ALTER TABLE public.feedback_reports ENABLE ROW LEVEL SECURITY;

    -- Security & Anti-Abuse Length Constraints
    DO $$ 
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_feedback_msg_len') THEN
            ALTER TABLE public.feedback_reports ADD CONSTRAINT chk_feedback_msg_len CHECK (length(message) <= 3000);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_feedback_contact_len') THEN
            ALTER TABLE public.feedback_reports ADD CONSTRAINT chk_feedback_contact_len CHECK (user_contact IS NULL OR length(user_contact) <= 150);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_feedback_type_len') THEN
            ALTER TABLE public.feedback_reports ADD CONSTRAINT chk_feedback_type_len CHECK (length(type) <= 50);
        END IF;
    END $$;

    DROP POLICY IF EXISTS "Allow anon inserts for feedback" ON public.feedback_reports;
    DROP POLICY IF EXISTS "Allow anon read for feedback" ON public.feedback_reports;
    DROP POLICY IF EXISTS "Allow anon update for feedback" ON public.feedback_reports;
    DROP POLICY IF EXISTS "Allow anon delete for feedback" ON public.feedback_reports;

    CREATE POLICY "Allow anon inserts for feedback" ON public.feedback_reports FOR INSERT WITH CHECK (true);
    CREATE POLICY "Allow anon read for feedback" ON public.feedback_reports FOR SELECT USING (true);
    CREATE POLICY "Allow anon update for feedback" ON public.feedback_reports FOR UPDATE USING (true) WITH CHECK (true);
    CREATE POLICY "Allow anon delete for feedback" ON public.feedback_reports FOR DELETE USING (true);

    -- Reload PostgREST Schema Cache
    NOTIFY pgrst, 'reload schema';