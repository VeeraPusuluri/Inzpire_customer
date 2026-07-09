# Inzpire — native Android (Kotlin) — **Customer** app

A native Kotlin/Jetpack Compose rewrite of the **customer** experience from the `Inzpire Home Hub`
web app (`~/Downloads/Inzpire Home Hub`, the `/app` route). It is the counterpart to the influencer
app (`~/Documents/inzpire_android`, "Inzpire Connect") and shares the same architecture, brand theme,
and Supabase project. Distinct `applicationId` (`com.inzpire.customer`) so both apps coexist on one device.

## Screens

| Web route | Native screen | Data source |
|---|---|---|
| `/auth` | Sign in / sign up (`role=customer`) | **Live** Supabase Auth |
| `/app` (customer) | Home — project cockpit: day-line, team, milestone tracker, quick tiles, site updates | Seed |
| `/cockpit/designs` | Designs — 3D/2D/moodboard gallery + approve / request-changes | Seed |
| `/cockpit/materials` | Materials — swatches grouped by room | Seed |
| `/payments` | Payments — paid progress + milestone schedule (Pay-now stub) | Seed |
| `/approvals` | Approvals — sign-off list (approve / request-changes) | Seed |
| `/profile` | Profile — personal details load/save | **Live** Supabase |

The customer cockpit in the source web app renders off `src/lib/inzpire/cockpit-data.ts` ("portal
renders entirely off this data"). That seed is ported 1:1 to `data/CockpitData.kt` (customer NM Reddy —
3BHK Flat); images are the same Unsplash URLs, loaded via Coil. Only Auth + Profile hit Supabase.

## Architecture

Mirrors `inzpire_android`: Compose + Material3, Navigation-Compose, no DI (no-arg `ViewModel`s),
supabase-kt (Auth + Postgrest) over Ktor/OkHttp, one Activity-scoped `CustomerViewModel` for
session/profile. Shared infra (`SupabaseClientProvider`, `AuthRepository`, `ProfileRepository`,
theme, the redesigned navy-hero auth screen) was copied from the influencer app and re-packaged under
`com.inzpire.customer`.

Bottom nav (5 tabs): **Home · Designs · Materials · Pay · Profile**; Approvals is reached from a Home tile.

## Backend

Points at the Supabase project **`drlggmunipoxxxoeqkje`** (set in `local.properties`, gitignored).
Two one-time steps:

1. **Paste the anon key.** In `local.properties`, replace `PASTE_NEW_ANON_KEY_HERE` with the
   project's anon / publishable key (Supabase dashboard → Project Settings → API). Until it's set,
   the app builds and shows the bundled seed, but live Auth/queries return 401.
2. **Seed the demo data.** The project's *schema* is already applied but has no rows. Open the
   Supabase SQL editor and run [`supabase/seed.sql`](supabase/seed.sql). It creates the demo
   customer + designer + PM, the "3BHK Flat" project, and every cockpit row (designs, materials,
   payments, approvals, site updates, chat, documents, offers). Idempotent — safe to re-run.

Then sign in as **`nmreddy@demo.inzpire.app` / `Inzpire@123`**.

```bash
export JAVA_HOME=/Users/veerapusuluru/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home
./gradlew :app:installDebug
```

## How data flows

Home / Designs / Materials / Pay / Approvals / Documents / Chat now render off **live Supabase**:
`CockpitRepository` loads the signed-in customer's most recent `projects` row and its `moodboards`,
`rooms`+`selections`+`products`, `payments`, `approvals`, `site_updates`, `messages` and
`project_documents`, mapping them into the existing UI models. `CustomerViewModel` holds the
`Cockpit` snapshot; screens read slices of it. If the customer has no project yet (or the fetch
fails / the key is missing) the screens fall back to the bundled `CockpitData` seed so the UI is
always populated. Writes persist when live: **Approve / Request-changes** on Approvals, **Pay now**
on Payments, and **Send** on Chat. New Enquiry inserts a `leads` row; Offers reads the `offers` table.

## Known gaps / next steps

- **App icon / splash** reuse the influencer placeholder ring mark — swap in customer-branded assets.
- **Payments "Pay now"** flips the milestone to paid but has no real gateway (matches the web app —
  no Razorpay yet).
- **Designs "Approve"** updates optimistically; the customer can't write `moodboards` under RLS
  (designer/PM-owned), so the durable sign-off path is the **Approvals** tab, which does persist.
- **Documents** are metadata rows in `project_documents`; wiring real file downloads from the
  `documents` storage bucket is the next step.
# Inzpire_customer
