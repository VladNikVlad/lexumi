-- Per-user progress for admin ("Самостійне вивчення") words/sentences — kept OFF the shared
-- `words`/`sentences` rows (those are one global admin-authored record per language, shared by
-- every user who studies it; writing rating/streaks directly onto them would mean every user
-- overwrites every other user's progress). Mirrors the rating/streak columns already on `words`/
-- the local Room entities, just keyed by (user_id, word_id) instead of being a single shared row.
--
-- Required by the "Duolingo model" sync redesign: a topic's content (and this table's rows for
-- the words/sentences in it) is only synced locally on demand, so progress has to be recoverable
-- from the server the next time that topic is opened, rather than living only on-device.
--
-- Run this once in the Supabase Dashboard -> SQL Editor. Depends on `public.set_updated_at()`
-- from backend/soft_delete_migration.sql (run that first if you haven't already).

create table public.word_progress (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    word_id uuid not null references public.words(id) on delete cascade,
    rating int not null default 0,
    correct_streak int not null default 0,
    typed_streak int not null default 0,
    typed_reverse_active boolean not null default false,
    voice_streak int not null default 0,
    final_streak int not null default 0,
    times_seen int not null default 0,
    in_review_list boolean not null default false,
    total_correct int not null default 0,
    best_streak int not null default 0,
    current_stats_streak int not null default 0,
    updated_at timestamptz,
    unique (user_id, word_id)
);
alter table public.word_progress enable row level security;
create policy "own progress only" on public.word_progress
    for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create trigger set_updated_at before update on public.word_progress
    for each row execute function public.set_updated_at();

create table public.sentence_progress (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.profiles(id) on delete cascade,
    sentence_id uuid not null references public.sentences(id) on delete cascade,
    rating int not null default 0,
    direct_streak int not null default 0,
    reverse_streak int not null default 0,
    audio_streak int not null default 0,
    voice_streak int not null default 0,
    times_seen int not null default 0,
    total_correct int not null default 0,
    best_streak int not null default 0,
    current_stats_streak int not null default 0,
    known boolean not null default false,
    updated_at timestamptz,
    unique (user_id, sentence_id)
);
alter table public.sentence_progress enable row level security;
create policy "own progress only" on public.sentence_progress
    for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create trigger set_updated_at before update on public.sentence_progress
    for each row execute function public.set_updated_at();
