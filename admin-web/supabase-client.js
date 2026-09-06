// Same Supabase project/anon key the Android app uses (see app/build.gradle.kts) — this key is
// meant to be public; access control comes entirely from Postgres Row Level Security (see
// backend/SCHEMA.md), not from keeping the key secret.
const SUPABASE_URL = 'https://debimvrfizlsatvrjeam.supabase.co';
const SUPABASE_ANON_KEY =
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRlYmltdnJmaXpsc2F0dnJqZWFtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc0ODAzNTYsImV4cCI6MjEwMzA1NjM1Nn0.8q1Fav9Y_5Oy5_MNIqeN3qSfnrew3e2KbqFcoPKu_4c';

// window.supabase is the UMD SDK namespace (loaded via <script> in index.html); this module's
// own export is the actual client instance.
export const supabaseClient = window.supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

/** The signed-in user's profile row if they're an admin, otherwise null (covers "not signed in",
 * "signed in but no profile row", and "signed in but is_admin = false" the same way). */
export async function requireAdmin() {
  const { data: sessionData } = await supabaseClient.auth.getSession();
  if (!sessionData.session) return null;
  const { data, error } = await supabaseClient
    .from('profiles')
    .select('id, display_name, is_admin')
    .eq('id', sessionData.session.user.id)
    .maybeSingle();
  if (error || !data || !data.is_admin) return null;
  return data;
}
