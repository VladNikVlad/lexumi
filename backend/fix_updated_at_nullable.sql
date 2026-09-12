-- Fixes soft_delete_migration.sql: `updated_at ... not null default now()` meant every existing
-- row got backfilled to "now" at ALTER TABLE time (nothing wrong, just an unavoidable side effect
-- of adding a NOT NULL DEFAULT column), AND every newly inserted row got stamped immediately on
-- creation — neither of those is a real edit. Wanted behavior: `updated_at` stays NULL until a
-- row is genuinely updated for the first time (the `set_updated_at` trigger from the original
-- migration already only fires on UPDATE, so it needs no change — only the column itself does).
--
-- First attempt at this script didn't actually clear anything: `set_updated_at` fires on EVERY
-- update, including the `update ... set updated_at = null` below — so the trigger was re-stamping
-- `now()` right back before the row was written, undoing its own reset. Disabling the trigger for
-- just that one statement (then re-enabling it) fixes it.
--
-- Run this once in the Supabase Dashboard -> SQL Editor, after soft_delete_migration.sql.

do $$
declare
    t text;
begin
    foreach t in array array[
        'languages', 'sections', 'topics', 'words', 'topic_words', 'rules',
        'sentences', 'topic_sentences', 'image_content', 'stories'
    ]
    loop
        execute format('alter table public.%I alter column updated_at drop not null', t);
        execute format('alter table public.%I alter column updated_at drop default', t);
        execute format('alter table public.%I disable trigger set_updated_at', t);
        execute format('update public.%I set updated_at = null', t);
        execute format('alter table public.%I enable trigger set_updated_at', t);
    end loop;
end $$;
