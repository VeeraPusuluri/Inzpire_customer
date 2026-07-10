// ============================================================================
// send-push — Inzpire push delivery (FCM HTTP v1)
//
// Invoked by the AFTER INSERT trigger on public.notifications (via pg_net) with
// { "notification_id": "<uuid>" }. It:
//   1. verifies the x-dispatch-secret header against PUSH_DISPATCH_SECRET,
//   2. loads the notification row + the recipient's device_tokens (service role),
//   3. mints an FCM v1 OAuth token from the FCM_SERVICE_ACCOUNT secret,
//   4. sends one push per token, pruning tokens FCM reports as unregistered.
//
// Secrets (set with `supabase secrets set ...`):
//   FCM_SERVICE_ACCOUNT   – the inzpire-66bd3 service-account JSON (one line)
//   PUSH_DISPATCH_SECRET  – shared secret, also stored in private.push_settings
// Auto-injected by the platform: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY
//
// Deploy:  supabase functions deploy send-push --no-verify-jwt
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

interface ServiceAccount {
  client_email: string;
  private_key: string;
  project_id: string;
  token_uri: string;
}

// ---- Google OAuth: sign a JWT with the service-account key, exchange for a token ----
let cachedToken: { token: string; exp: number } | null = null;

function pemToPkcs8(pem: string): ArrayBuffer {
  const b64 = pem
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const bin = atob(b64);
  const buf = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) buf[i] = bin.charCodeAt(i);
  return buf.buffer;
}

function b64url(bytes: Uint8Array | string): string {
  const str = typeof bytes === "string"
    ? bytes
    : String.fromCharCode(...bytes);
  return btoa(str).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

async function getAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cachedToken && cachedToken.exp - 60 > now) return cachedToken.token;

  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: sa.token_uri,
    iat: now,
    exp: now + 3600,
  }));
  const unsigned = `${header}.${claim}`;

  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToPkcs8(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = new Uint8Array(
    await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(unsigned)),
  );
  const jwt = `${unsigned}.${b64url(sig)}`;

  const res = await fetch(sa.token_uri, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });
  if (!res.ok) throw new Error(`token exchange failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  cachedToken = { token: json.access_token, exp: now + (json.expires_in ?? 3600) };
  return cachedToken.token;
}

// data payload must be all-strings for FCM
function stringifyData(data: Record<string, unknown>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(data ?? {})) {
    if (v === null || v === undefined) continue;
    out[k] = typeof v === "string" ? v : JSON.stringify(v);
  }
  return out;
}

Deno.serve(async (req) => {
  try {
    const dispatchSecret = Deno.env.get("PUSH_DISPATCH_SECRET");
    if (dispatchSecret && req.headers.get("x-dispatch-secret") !== dispatchSecret) {
      return new Response("unauthorized", { status: 401 });
    }

    const { notification_id } = await req.json();
    if (!notification_id) return new Response("missing notification_id", { status: 400 });

    const sa: ServiceAccount = JSON.parse(Deno.env.get("FCM_SERVICE_ACCOUNT") ?? "{}");
    if (!sa.private_key) return new Response("FCM_SERVICE_ACCOUNT not set", { status: 500 });

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    const { data: n, error: nErr } = await supabase
      .from("notifications")
      .select("id, user_id, title, body, type, link, data")
      .eq("id", notification_id)
      .single();
    if (nErr || !n) return new Response("notification not found", { status: 404 });

    const { data: tokens } = await supabase
      .from("device_tokens")
      .select("token")
      .eq("user_id", n.user_id);

    if (!tokens || tokens.length === 0) {
      return Response.json({ ok: true, sent: 0, reason: "no device tokens" });
    }

    const accessToken = await getAccessToken(sa);
    const endpoint = `https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`;
    const dataPayload = stringifyData({
      ...(n.data ?? {}),
      type: n.type,
      link: n.link ?? "",
      notification_id: n.id,
    });

    let sent = 0;
    const dead: string[] = [];
    await Promise.all(tokens.map(async ({ token }) => {
      const res = await fetch(endpoint, {
        method: "POST",
        headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token,
            notification: { title: n.title ?? "Inzpire", body: n.body ?? "" },
            data: dataPayload,
            android: { priority: "high", notification: { channel_id: "inzpire_default" } },
          },
        }),
      });
      if (res.ok) { sent++; return; }
      const errText = await res.text();
      // 404 UNREGISTERED / 400 invalid → token is dead, prune it
      if (res.status === 404 || errText.includes("UNREGISTERED") || errText.includes("INVALID_ARGUMENT")) {
        dead.push(token);
      } else {
        console.error(`FCM send failed (${res.status}): ${errText}`);
      }
    }));

    if (dead.length) {
      await supabase.from("device_tokens").delete().in("token", dead);
    }

    return Response.json({ ok: true, sent, pruned: dead.length });
  } catch (e) {
    console.error("send-push error:", e);
    return new Response(`error: ${e instanceof Error ? e.message : String(e)}`, { status: 500 });
  }
});
