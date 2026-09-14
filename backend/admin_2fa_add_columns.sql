-- Fixes admin_2fa.sql: `admin_2fa_codes` already existed (from the earlier SCHEMA.md draft,
-- applied at some point before this session) with only its original 5 columns (id, profile_id,
-- code, expires_at, used_at) — so `create table if not exists` in admin_2fa.sql silently did
-- nothing, never adding `created_at`/`attempts`, which verify-2fa-code needs. Adds them to the
-- EXISTING table without touching any data already in it.
--
-- Run this once in the Supabase Dashboard -> SQL Editor.

alter table public.admin_2fa_codes add column if not exists attempts int not null default 0;
alter table public.admin_2fa_codes add column if not exists created_at timestamptz not null default now();
