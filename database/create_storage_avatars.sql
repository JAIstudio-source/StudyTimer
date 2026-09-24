-- ============================================================================
-- Supabase Storage Setup for Profile Avatars
-- Run this in Supabase SQL Editor to enable avatar image hosting
-- ============================================================================

-- 1. Create public 'avatars' storage bucket if not present
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'avatars',
    'avatars',
    true,
    5242880, -- 5 MB limit per image
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

-- 2. Storage Policies for Avatars Bucket
DROP POLICY IF EXISTS "Public Read Avatars" ON storage.objects;
DROP POLICY IF EXISTS "Allow Upload Avatars" ON storage.objects;
DROP POLICY IF EXISTS "Allow Update Avatars" ON storage.objects;
DROP POLICY IF EXISTS "Allow Delete Avatars" ON storage.objects;

-- Allow anyone to view avatar images (for leaderboard, presence, etc.)
CREATE POLICY "Public Read Avatars"
ON storage.objects FOR SELECT
USING (bucket_id = 'avatars');

-- Allow authenticated and anon users to upload avatars
CREATE POLICY "Allow Upload Avatars"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'avatars');

-- Allow updating avatars
CREATE POLICY "Allow Update Avatars"
ON storage.objects FOR UPDATE
USING (bucket_id = 'avatars')
WITH CHECK (bucket_id = 'avatars');

-- Allow deleting avatars
CREATE POLICY "Allow Delete Avatars"
ON storage.objects FOR DELETE
USING (bucket_id = 'avatars');
