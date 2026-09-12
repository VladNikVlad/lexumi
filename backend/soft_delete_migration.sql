-- Soft-delete migration: replaces real DELETE with a `deleted_at` timestamp for every content
-- table except videos/audio_dialogs/test_questions (those keep real deletes, per product decision).
-- Also adds `updated_at`, auto-maintained by a trigger so it stays correct regardless of which
-- client performs the UPDATE (admin-web today, potentially others later).
--
-- Run this once in the Supabase Dashboard -> SQL Editor.

create or replace function public.set_updated_at() returns trigger as $$
begin
    new.updated_at = now();
    return new;
end;
$$ language plpgsql;

-- languages
alter table public.languages add column if not exists updated_at timestamptz not null default now();
alter table public.languages add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.languages;
create policy "read own or global" on public.languages
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.languages;
create trigger set_updated_at before update on public.languages
    for each row execute function public.set_updated_at();

-- sections
alter table public.sections add column if not exists updated_at timestamptz not null default now();
alter table public.sections add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.sections;
create policy "read own or global" on public.sections
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.sections;
create trigger set_updated_at before update on public.sections
    for each row execute function public.set_updated_at();

-- topics
alter table public.topics add column if not exists updated_at timestamptz not null default now();
alter table public.topics add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.topics;
create policy "read own or global" on public.topics
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.topics;
create trigger set_updated_at before update on public.topics
    for each row execute function public.set_updated_at();

-- words
alter table public.words add column if not exists updated_at timestamptz not null default now();
alter table public.words add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.words;
create policy "read own or global" on public.words
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.words;
create trigger set_updated_at before update on public.words
    for each row execute function public.set_updated_at();

-- topic_words
alter table public.topic_words add column if not exists updated_at timestamptz not null default now();
alter table public.topic_words add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.topic_words;
create policy "read own or global" on public.topic_words
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.topic_words;
create trigger set_updated_at before update on public.topic_words
    for each row execute function public.set_updated_at();

-- rules
alter table public.rules add column if not exists updated_at timestamptz not null default now();
alter table public.rules add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.rules;
create policy "read own or global" on public.rules
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.rules;
create trigger set_updated_at before update on public.rules
    for each row execute function public.set_updated_at();

-- sentences
alter table public.sentences add column if not exists updated_at timestamptz not null default now();
alter table public.sentences add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.sentences;
create policy "read own or global" on public.sentences
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.sentences;
create trigger set_updated_at before update on public.sentences
    for each row execute function public.set_updated_at();

-- topic_sentences
alter table public.topic_sentences add column if not exists updated_at timestamptz not null default now();
alter table public.topic_sentences add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.topic_sentences;
create policy "read own or global" on public.topic_sentences
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.topic_sentences;
create trigger set_updated_at before update on public.topic_sentences
    for each row execute function public.set_updated_at();

-- image_content
alter table public.image_content add column if not exists updated_at timestamptz not null default now();
alter table public.image_content add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.image_content;
create policy "read own or global" on public.image_content
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.image_content;
create trigger set_updated_at before update on public.image_content
    for each row execute function public.set_updated_at();

-- stories
alter table public.stories add column if not exists updated_at timestamptz not null default now();
alter table public.stories add column if not exists deleted_at timestamptz;
drop policy if exists "read own or global" on public.stories;
create policy "read own or global" on public.stories
    for select using ((owner_id is null or owner_id = auth.uid()) and deleted_at is null);
drop trigger if exists set_updated_at on public.stories;
create trigger set_updated_at before update on public.stories
    for each row execute function public.set_updated_at();

-- videos / audio_dialogs / test_questions: deliberately UNCHANGED (still real DELETE, no
-- deleted_at/updated_at columns, no policy change) per the explicit exception in this migration.
