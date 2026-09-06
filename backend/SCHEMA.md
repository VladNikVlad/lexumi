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
`languages`/`topics`/`words`, а також `rules`/`sentences`/`videos`/
`stories`/`test_questions`/`image_content` (реалізовано нижче). Картинки
слів/правил/карток невеликі (до 100kb, `util/ImageCompressor.kt`), тому
зберігаються прямо в рядку як base64 (`image_data`) — окреме файлове
сховище (Supabase Storage) не знадобилось. Єдине, що досі не
синхронізується — `audio_dialogs`: аудіофайл не обмежений так само
жорстко за розміром, тож для нього таки потрібне окреме файлове сховище
(окрема задача, ще не зроблена).

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
    image_path text,   -- legacy/unused — local file path never makes sense on the server
    image_data text,   -- base64 image, <=100kb (util/ImageCompressor.kt), embedded directly instead of file storage
    rule_id uuid,      -- soft reference to rules(id) — no FK constraint, same as the local Room entity
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

-- Мовний рівень (не тема!) — одне й те саме правило можна прикріпити
-- до слів/речень/відео/історій з різних тем цієї мови, звідси і
-- посилання на нього по id, а не копія тексту.
create table public.rules (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    language_id uuid not null references languages(id) on delete cascade,
    name text not null,
    text text not null,
    image_path text,  -- legacy/unused
    image_data text,  -- base64 image, <=100kb, embedded directly instead of file storage
    created_at timestamptz not null default now()
);

-- Картки-зображення (point 11 & 21). Завжди мають картинку (<=100kb,
-- util/ImageCompressor.kt), тому image_data тут NOT NULL, на відміну
-- від rules/words, де картинка необов'язкова.
create table public.image_content (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    topic_id uuid not null references topics(id) on delete cascade,
    name text not null,
    translation text not null,
    image_data text not null,
    created_at timestamptz not null default now()
);

create table public.sentences (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    topic_id uuid not null references topics(id) on delete cascade,
    text text not null,          -- немає окремого "name" — речення й так ідентифікується власним текстом
    translations text not null,  -- декілька перекладів, з'єднаних тим самим unit-separator'ом (U+001F), що і локально
    rule_ids text,               -- comma-separated remote uuid правил, може бути NULL
    created_at timestamptz not null default now()
);

-- youtube_url навмисно NOT NULL — відео без YouTube-посилання (локальний файл)
-- ніколи не публікується (немає файлового сховища).
create table public.videos (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    topic_id uuid not null references topics(id) on delete cascade,
    name text not null,
    youtube_url text not null,
    original_text text,
    translation_text text,
    rule_ids text,
    created_at timestamptz not null default now()
);

create table public.stories (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    topic_id uuid not null references topics(id) on delete cascade,
    name text not null,
    text text not null,
    translation text,
    rule_ids text,
    created_at timestamptz not null default now()
);

-- Лише video_id зараз (питання аудіодіалогів ще не публікуються — самі
-- аудіодіалоги теж чекають на Storage). Колонку audio_dialog_id додамо,
-- коли дійде черга синхронізувати аудіо.
create table public.test_questions (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid references profiles(id),
    video_id uuid not null references videos(id) on delete cascade,
    question_text text not null,
    answer_type text not null,           -- 'TRUE_FALSE' | 'EXACT_TEXT'
    correct_boolean boolean,
    acceptable_answers text,             -- unit-separator (U+001F) joined list, NULL якщо порожньо
    created_at timestamptz not null default now()
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
повторюється для `languages`, `sections`, `topics`, `sentences`, `rules`,
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
