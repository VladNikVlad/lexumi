// Verifies the code sent by send-2fa-code. Always checks the MOST RECENT code for this profile
// (a newer "Надіслати ще раз" silently supersedes any earlier unused one) and rejects it once
// MAX_ATTEMPTS wrong guesses have been made against it, forcing a fresh send instead of allowing
// unlimited brute-forcing of a 6-digit code within its expiry window.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

// See the identical block in send-2fa-code/index.ts for why this is inlined rather than a shared
// import — the Dashboard editor deploys one function's files at a time, not across functions.
const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

const MAX_ATTEMPTS = 5;

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

    const userClient = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_ANON_KEY")!,
      { global: { headers: { Authorization: authHeader } } },
    );
    const { data: userData } = await userClient.auth.getUser();
    const user = userData.user;
    if (!user) return json({ error: "Unauthorized" }, 401);

    const body = await req.json().catch(() => null);
    const code = body?.code;
    if (!code || typeof code !== "string") return json({ error: "Код обов'язковий" }, 400);

    const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
    const { data: latest } = await admin
      .from("admin_2fa_codes")
      .select("id, code, attempts, expires_at, used_at")
      .eq("profile_id", user.id)
      .order("created_at", { ascending: false })
      .limit(1)
      .maybeSingle();

    if (!latest) return json({ error: "Спершу запросіть код" }, 400);
    if (latest.used_at) return json({ error: "Цей код уже використано — запросіть новий" }, 400);
    if (new Date(latest.expires_at).getTime() < Date.now()) return json({ error: "Код прострочено — запросіть новий" }, 400);
    if (latest.attempts >= MAX_ATTEMPTS) return json({ error: "Забагато невдалих спроб — запросіть новий код" }, 400);

    if (latest.code !== code.trim()) {
      await admin.from("admin_2fa_codes").update({ attempts: latest.attempts + 1 }).eq("id", latest.id);
      return json({ error: "Невірний код" }, 400);
    }

    await admin.from("admin_2fa_codes").update({ used_at: new Date().toISOString() }).eq("id", latest.id);
    return json({ ok: true });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
