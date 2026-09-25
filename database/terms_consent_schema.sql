-- =========================================================================
-- StudyTimer User Terms & Privacy Policy Consent Audit Trail Schema
-- =========================================================================

-- 1. Dedicated Immutable Consent Audit Table
CREATE TABLE IF NOT EXISTS public.user_terms_consents (
    id BIGSERIAL PRIMARY KEY,
    user_id TEXT NOT NULL,
    user_email TEXT,
    user_name TEXT,
    terms_version TEXT NOT NULL DEFAULT 'v1.0',
    accepted_at TIMESTAMPTZ DEFAULT NOW(),
    accepted_at_iso TEXT NOT NULL,
    accepted_at_epoch BIGINT NOT NULL,
    device_hardware_id TEXT,
    device_model TEXT,
    app_version TEXT,
    platform TEXT DEFAULT 'android',
    ip_address TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_terms_consents_user_id ON public.user_terms_consents(user_id);
CREATE INDEX IF NOT EXISTS idx_terms_consents_accepted_at ON public.user_terms_consents(accepted_at);
CREATE INDEX IF NOT EXISTS idx_terms_consents_device_id ON public.user_terms_consents(device_hardware_id);

-- Enable RLS
ALTER TABLE public.user_terms_consents ENABLE ROW LEVEL SECURITY;

-- Allow anon & authenticated users to insert consent logs (App records acceptance)
DROP POLICY IF EXISTS "Allow anon insert for terms consents" ON public.user_terms_consents;
CREATE POLICY "Allow anon insert for terms consents" ON public.user_terms_consents FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Allow anon read for terms consents" ON public.user_terms_consents;
CREATE POLICY "Allow anon read for terms consents" ON public.user_terms_consents FOR SELECT USING (true);

-- 2. Add consent columns to user_sync_data & user_sync_profiles & app_user_cohorts for direct dashboard queries
ALTER TABLE public.user_sync_data ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE public.user_sync_data ADD COLUMN IF NOT EXISTS terms_version TEXT;

ALTER TABLE public.user_sync_profiles ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE public.user_sync_profiles ADD COLUMN IF NOT EXISTS terms_version TEXT;

ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS terms_accepted_at TIMESTAMPTZ;
ALTER TABLE public.app_user_cohorts ADD COLUMN IF NOT EXISTS terms_version TEXT;

-- Reload PostgREST Schema Cache
NOTIFY pgrst, 'reload schema';
