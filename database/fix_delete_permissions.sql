-- =========================================================================
-- COPY AND PASTE THIS ENTIRE SCRIPT INTO YOUR SUPABASE SQL EDITOR
-- =========================================================================

-- Disable Row Level Security (RLS) on all analytics tables
-- This grants full REST API DELETE permissions to the Admin Panel
ALTER TABLE public.app_analytics_events DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_user_cohorts DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_crash_reports DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_sync_data DISABLE ROW LEVEL SECURITY;

-- Reload Supabase PostgREST API schema cache immediately
NOTIFY pgrst, 'reload schema';
