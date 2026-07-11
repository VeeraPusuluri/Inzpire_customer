// ============================================================================
// verified — the page Supabase redirects to after a customer taps the email
// confirmation link. Replaces the old localhost Site URL (http://127.0.0.1:3000)
// which showed a blank page. Serves a confirmation on a real
// https://<ref>.supabase.co/functions/v1/verified URL.
//
// NOTE: Supabase's edge runtime deliberately rewrites `text/html` responses to
// `content-type: text/plain` + a `sandbox` CSP (anti-abuse — functions can't
// host styled web pages on the supabase.co domain). So we serve clean, readable
// PLAIN TEXT instead; it renders fine in a browser and needs no extra hosting.
//
// The confirmation flow: email link -> Supabase /auth/v1/verify (confirms the
// email) -> 302 redirect here (redirect_to, set per-signup in the app). We just
// show a success message; the customer returns to the app and signs in.
//
// Public (no JWT) — see [functions.verified] verify_jwt = false in config.toml.
// Deploy:  supabase functions deploy verified --no-verify-jwt
// ============================================================================

const MESSAGE = [
  "✅  EMAIL VERIFIED",
  "",
  "Your email address has been confirmed.",
  "",
  "You can close this tab, return to the Inzpire app,",
  "and sign in with your email and password.",
  "",
  "— INZPIRE",
  "",
].join("\n");

Deno.serve(() =>
  new Response(MESSAGE, {
    status: 200,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-store",
    },
  })
);
