// Sends a 6-digit admin 2FA code by email via Resend. Called by admin-web right after a
// successful email+password sign-in, before the dashboard itself is shown (see app.js's
// render2faGate). Deploy via the Supabase Dashboard -> Edge Functions -> Create function (paste
// this file's contents) — see admin-web/README.md for the full one-time setup, including the
// RESEND_API_KEY secret this function needs.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// admin-web calls this from a different origin (Vercel) — both the preflight OPTIONS response and
// every real response need these headers or the browser's fetch (via supabaseClient.functions.
// invoke) rejects it silently. Inlined here (not a shared import) so this file pastes as-is into
// the Supabase Dashboard's one-file-per-function editor with nothing else to add.
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const CODE_TTL_MINUTES = 10;
// Resend's own sandbox sender — works immediately, no domain verification needed. Swap for a
// verified "from" address on your own domain later if you want a more polished sender name.
const RESEND_FROM = "Lexumi Admin <onboarding@resend.dev>";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response(null, { headers: corsHeaders });

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) return json({ error: "Unauthorized" }, 401);

    // Identifies the caller from their own JWT (functions.invoke sends it automatically) —
    // this client alone can't read admin_2fa_codes, RLS blocks it (see backend/admin_2fa.sql).
    const userClient = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } },
    );
    const { data: userData } = await userClient.auth.getUser();
    const user = userData.user;
    if (!user) return json({ error: "Unauthorized" }, 401);

    // Service-role client — the only way anything can touch admin_2fa_codes.
    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const { data: profile } = await admin
      .from("profiles")
      .select("is_admin, email")
      .eq("id", user.id)
      .maybeSingle();
    if (!profile?.is_admin) return json({ error: "Forbidden" }, 403);
    if (!profile.email) return json({ error: "У цього профілю немає збереженого email" }, 400);

    const code = String(Math.floor(100000 + Math.random() * 900000));
    const expiresAt = new Date(Date.now() + CODE_TTL_MINUTES * 60_000).toISOString();
    const { error: insertError } = await admin.from("admin_2fa_codes").insert({
      profile_id: user.id,
      code,
      expires_at: expiresAt,
    });
    if (insertError) return json({ error: insertError.message }, 500);

    const resendResponse = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        from: RESEND_FROM,
        to: profile.email,
        subject: "Код підтвердження Lexumi",
        text: `Ваш код: ${code}\n\nДіє ${CODE_TTL_MINUTES} хвилин. Якщо ви не намагались увійти в адмін-панель Lexumi, просто проігноруйте цей лист.`,
      }),
    });
    if (!resendResponse.ok) {
      const detail = await resendResponse.text();
      return json({ error: `Не вдалося надіслати лист: ${detail}` }, 502);
    }

    return json({ ok: true });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
