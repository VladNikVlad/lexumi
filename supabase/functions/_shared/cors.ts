// Shared by both 2FA functions (send-2fa-code/verify-2fa-code) — admin-web calls these from a
// different origin (Vercel), so both the preflight OPTIONS response and every real response need
// these headers or the browser's fetch (via supabaseClient.functions.invoke) rejects it silently.
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
