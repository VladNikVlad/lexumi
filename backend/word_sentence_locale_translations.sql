-- Per-locale translations for admin words/sentences — the base `words.translations`/
-- `sentences.translations` columns stay exactly as they are today (the default locale, 'uk',
-- since that's what all existing content already is) — these new tables hold translations for
-- any OTHER locale ('en', and later more), keyed to the same word/sentence by id.
--
-- v1 scope: words and sentences only (see the plan notes) — rules/videos/stories/image_content
-- get the same treatment in a later pass once this is confirmed working.
--
-- Run this once in the Supabase Dashboard -> SQL Editor. Depends on `public.is_admin()` (Профілі
-- й адміни) and `public.set_updated_at()` (soft_delete_migration.sql) already existing.

create table public.word_translations (
    id uuid primary key default gen_random_uuid(),
    word_id uuid not null references public.words(id) on delete cascade,
    locale text not null,
    translations text not null,
    updated_at timestamptz,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    unique (word_id, locale)
);
alter table public.word_translations enable row level security;
create policy "read all" on public.word_translations
    for select using (deleted_at is null);
create policy "admin writes" on public.word_translations
    for all using (public.is_admin()) with check (public.is_admin());
create trigger set_updated_at before update on public.word_translations
    for each row execute function public.set_updated_at();

create table public.sentence_translations (
    id uuid primary key default gen_random_uuid(),
    sentence_id uuid not null references public.sentences(id) on delete cascade,
    locale text not null,
    translations text not null,
    updated_at timestamptz,
    deleted_at timestamptz,
    created_at timestamptz not null default now(),
    unique (sentence_id, locale)
);
alter table public.sentence_translations enable row level security;
create policy "read all" on public.sentence_translations
    for select using (deleted_at is null);
create policy "admin writes" on public.sentence_translations
    for all using (public.is_admin()) with check (public.is_admin());
create trigger set_updated_at before update on public.sentence_translations
    for each row execute function public.set_updated_at();
