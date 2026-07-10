# Inzpire notifications — backend setup (one time)

Push + in-app notifications for all three surfaces run off the shared Supabase
project **`drlggmunipoxxxoeqkje`** and one Firebase project **`inzpire-66bd3`**
(both Android apps registered under it).

Delivery paths:
- **Customer app** (`com.inzpire.customer`) and **Connect/influencer app**
  (`com.inzpire.connect`) → **FCM push**.
- **Admin web** (`inzpire-admin`) → **in-app realtime bell** (reads `notifications`).

```
event row (lead / referral_event / message / …)
      → per-event trigger → public.notify_users() → INSERT public.notifications
      → trg_dispatch_push (pg_net) → send-push Edge Function → FCM v1 → device
      (admin bell just streams public.notifications over realtime)
```

## 1. Apply the schema
Supabase Dashboard → **SQL Editor** → paste & run [`notifications.sql`](./notifications.sql).
Idempotent — safe to re-run.

## 2. Set the Edge Function secrets
```bash
# from repo root; needs the Supabase CLI + `supabase login`
PROJECT=drlggmunipoxxxoeqkje

# the Firebase service-account JSON (as ONE line)
supabase secrets set --project-ref $PROJECT \
  FCM_SERVICE_ACCOUNT="$(jq -c . ~/Downloads/inzpire-66bd3-firebase-adminsdk-fbsvc-c71f5c7c56.json)"

# a shared secret the DB trigger sends to the function (any long random string)
DISPATCH_SECRET="$(openssl rand -hex 32)"
supabase secrets set --project-ref $PROJECT PUSH_DISPATCH_SECRET="$DISPATCH_SECRET"
echo "DISPATCH_SECRET=$DISPATCH_SECRET   # you need this for step 4"
```

## 3. Deploy the function
```bash
supabase functions deploy send-push --project-ref $PROJECT --no-verify-jwt
```

## 4. Point the DB trigger at the function
Run in the SQL Editor, pasting the secret printed in step 2:
```sql
insert into private.push_settings (id, function_url, dispatch_secret)
values (true,
        'https://drlggmunipoxxxoeqkje.supabase.co/functions/v1/send-push',
        '<DISPATCH_SECRET from step 2>')
on conflict (id) do update
  set function_url = excluded.function_url,
      dispatch_secret = excluded.dispatch_secret;
```

Until this row exists the in-app bell still works; only the push fan-out is paused.

## 5. Smoke test
```sql
-- replace with a real auth user id that has a device_tokens row
select public.notify_users(array['<user-uuid>']::uuid[], 'general',
       'Test', 'Hello from Inzpire', '/app');
```
Then check `supabase functions logs send-push` and the device.

## Event → recipient map
| Event (table)                    | Recipient(s)                     | Delivery |
|----------------------------------|----------------------------------|----------|
| `leads` INSERT (referral)        | admins                           | in-app   |
| `referral_events` INSERT         | admins                           | in-app   |
| `referral_events` approved/go_live/bonus_released | influencer      | push     |
| `team_assignments` INSERT        | project customer                 | push     |
| `site_updates` INSERT (visible)  | project customer                 | push     |
| `messages` INSERT                | other project participants       | push     |
| `approvals` INSERT (pending)     | project customer                 | push     |
| `payments` INSERT (pending)      | project customer                 | push     |
| `commissions` → paid             | influencer (owner)               | push     |

> Skipped for now (per decision): admin↔influencer messaging (no channel exists
> in the schema yet), browser push for the admin portal.
