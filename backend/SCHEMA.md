# Lexumi — схема бази даних (Етап 1, чернетка)

Це перший технічний документ для переходу на бекенд. Мета Етапу 1: додати
акаунти, синхронізацію "Самостійного вивчення" (тепер — адмінський контент)
і "Власного матеріалу" (особистий контент користувача), **нічого не
ламаючи** в застосунку, що вже працює.

Рушій — **Supabase** (Postgres + вбудована авторизація + Row Level Security).

## Головне архітектурне рішення: одна схема на "адмінське" і "особисте"

Замість двох окремих наборів таблиць (одна для адмінського контенту, друга
для особистого) — **одні й ті самі таблиці**, з полем-власником:

```
owner_id uuid REFERENCES profiles(id)  -- NULL = глобальний/адмінський контент
                                        -- не NULL = особистий контент цього користувача
```

Це напряму дає обидва режими без дублювання структур:
- **"Самостійне вивчення"** = рядки, де `owner_id IS NULL`.
- **"Власний матеріал"** = рядки, де `owner_id = поточний користувач`.

Хто що може робити — контролюється не кодом застосунку, а **правилами
безпосередньо в базі даних** (RLS), тому обійти це, підмінивши щось у
самому застосунку, неможливо.

## Профілі й адміни

```sql
-- Supabase вже має auth.users (Google Sign-In і т.д.) — тут лише наші додаткові поля.
create table public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    is_admin boolean not null default false,
    is_premium boolean not null default false,       -- кешоване значення, звіряється з subscriptions
    created_at timestamptz not null default now()
);

-- Email-код для 2FA адмінів (безкоштовний варіант, без SMS).
create table public.admin_2fa_codes (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references profiles(id) on delete cascade,
    code text not null,              -- 6-значний код, надісланий на email
    expires_at timestamptz not null,
    used_at timestamptz
);

-- Допоміжна функція для RLS-правил нижче.
create function public.is_admin() returns boolean as $$
    select coalesce((select is_admin from public.profiles where id = auth.uid()), false);
$$ language sql stable security definer;
```

## Ієрархія контенту (мови → розділи → теми → слова/речення/...)

Кожна з цих таблиць отримує `owner_id`. Показую повністю на прикладі
`languages`/`topics`/`words`, решта (`sentences`, `rules`, `image_content`,
`videos`, `audio_dialogs`, `stories`, `test_questions`) — за тим самим
шаблоном (як зараз в Room-entity, плюс `owner_id`).

```sql
create table public.languages (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),  -- NULL = адмінська (спільна) мова
    profile_id uuid,                         -- сумісність зі старою моделлю (можна прибрати пізніше)
    name text not null,
    voice_name text,
    created_at timestamptz not null default now()
);

create table public.sections (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    language_id uuid not null references languages(id) on delete cascade,
    name text not null,
    position int not null default 0
);

create table public.topics (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    section_id uuid not null references sections(id) on delete cascade,
    name text not null,
    position int not null default 0
);

create table public.words (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    topic_id uuid not null references topics(id) on delete cascade,
    term text not null,
    translation text not null,
    image_path text,
    rule_id uuid,
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
    created_at timestamptz not null default now(),
    -- дедублікація: одне й те саме слово (без урахування регістру/пробілів) в межах
    -- теми й того самого власника не повинно дублюватись
    unique (topic_id, owner_id, term)
);
```

### RLS-правила (шаблон, однаковий для кожної таблиці контенту)

```sql
alter table public.words enable row level security;

-- Читати можна: своє особисте АБО будь-який глобальний (адмінський) контент.
create policy "read own or global" on public.words
    for select using (owner_id is null or owner_id = auth.uid());

-- Створювати/редагувати/видаляти особисте — тільки власник.
create policy "write own" on public.words
    for all using (owner_id = auth.uid()) with check (owner_id = auth.uid());

-- Створювати/редагувати/видаляти глобальне — тільки адмін.
create policy "admin writes global" on public.words
    for all using (owner_id is null and public.is_admin())
    with check (owner_id is null and public.is_admin());
```

Те саме (3 політики: read own-or-global / write own / admin writes global)
повторюється для `sections`, `topics`, `sentences`, `rules`,
`image_content`, `videos`, `audio_dialogs`, `stories`, `test_questions`.

## Підписки (преміум)

```sql
create table public.subscriptions (
    id uuid primary key default gen_random_uuid(),
    profile_id uuid not null references profiles(id) on delete cascade,
    platform text not null default 'android',
    product_id text not null,             -- ідентифікатор товару в Google Play
    status text not null,                  -- 'active' | 'expired' | 'cancelled'
    current_period_end timestamptz,
    verified_at timestamptz not null default now(),
    raw_receipt jsonb                      -- відповідь Google Play Developer API, для звірки
);
```

`is_premium` у `profiles` — це лише кеш для швидких перевірок в застосунку;
**реальна перевірка завжди йде через `subscriptions`**, яку оновлює лише
серверна функція (Supabase Edge Function), що напряму питає Google Play
Developer API — застосунок сам ніколи не пише в цю таблицю.

## Що це означає для Android-застосунку (важливо)

1. **Зміна типу id**: зараз у Room все на `Long` (автоінкремент). У Postgres
   тут — `uuid`. Це означає міграцію моделей (`Word.id: Long` → `String`)
   і БД-міграцію Room (нова колонка з `uuid`-рядком, старий `Long` можна
   лишити як `localId` для сумісності зі старими даними).
2. **Room залишається** — тепер як локальний кеш/дзеркало, а не єдине
   джерело правди. Синхронізація — окремий шар (repository вчиться писати
   і туди, і туди), логіка ViewModel/UI не змінюється.
3. Дедублікація слів (`unique (topic_id, owner_id, term)`) — застосунок
   перед збереженням питає "чи є вже таке", і якщо є — пропонує
   використати наявний запис замість створення нового.

## Що свідомо НЕ включено зараз (Етап 3, щоб не зашкодити пізніше)

Таблиці для нового режиму "Вивчення мови" (`courses`, `course_steps`,
`course_step_content`, `course_exams`, `user_course_progress`,
`ad_watch_events`) навмисно не проєктую зараз у деталях — це Етап 3.
Головне вже закладено правильно: контент (`words`, `sentences` тощо)
залишиться тим самим, курс просто **посилатиметься на існуючі id**, а не
дублюватиме дані.

## ⚠️ Доповнення — RLS для `profiles`/`subscriptions`/`admin_2fa_codes`

Це виправлення до вже виконаної схеми — **виконайте окремо** в SQL Editor
(додатково до того, що вже запускали). Без цього будь-хто з ключем anon міг
би читати чи змінювати чужі профілі, у т.ч. видати собі преміум чи адмінку.

```sql
-- profiles: бачити можна своє АБО (якщо ти адмін) — усі; редагувати можна лише своє.
alter table public.profiles enable row level security;

create policy "read own or admin reads all" on public.profiles
    for select using (id = auth.uid() or public.is_admin());

create policy "insert own profile" on public.profiles
    for insert with check (id = auth.uid());

create policy "update own profile" on public.profiles
    for update using (id = auth.uid()) with check (id = auth.uid());

-- Ключове: сам факт "можеш оновити свій рядок" НЕ повинен означати "можеш
-- зробити себе адміном чи преміумом". RLS керує РЯДКАМИ, а не КОЛОНКАМИ —
-- тому колонки is_admin/is_premium явно забороняємо оновлювати клієнту
-- взагалі (на рівні прав доступу до колонок, а не через RLS-політику).
revoke update on public.profiles from authenticated;
grant update (display_name) on public.profiles to authenticated;

-- admin_2fa_codes: жодного прямого доступу з застосунку — тільки серверна
-- Edge Function (service role), яка обходить RLS. Enable без жодних policy
-- означає "ніхто через anon/authenticated ключ сюди не дістанеться".
alter table public.admin_2fa_codes enable row level security;

-- subscriptions: користувач може тільки ЧИТАТИ свій запис; писати сюди
-- може лише серверна функція, що звіряється з Google Play Developer API.
alter table public.subscriptions enable row level security;

create policy "read own subscription" on public.subscriptions
    for select using (profile_id = auth.uid());
```

---

Це чернетка для обговорення — не остаточна версія. Скажіть, якщо щось
із цього виглядає незручним чи ви бачите інакше, і підправимо перед тим,
як реально створювати проєкт у Supabase.
