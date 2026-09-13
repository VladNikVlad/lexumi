-- Email-based 2FA for admins — the `admin_2fa_codes` table was already sketched in SCHEMA.md but
-- never actually created. Adds `created_at` (to find the latest code for a profile) and
-- `attempts` (brute-force guard) beyond that original draft.
--
-- Run this once in the Supabase Dashboard -> SQL Editor.

create table if not exists public.admin_2fa_codes (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references public.profiles(id) on delete cascade,
    code text not null,
    attempts int not null default 0,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);

alter table public.admin_2fa_codes enable row level security;
-- Deliberately no policy at all — neither the anon nor the authenticated key can read or write
-- this table directly. Only the service-role key (used exclusively inside the two Edge Functions
-- in supabase/functions/) can touch it, bypassing RLS entirely.
