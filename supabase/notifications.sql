-- ============================================================================
-- Inzpire — push + in-app notifications
-- Shared Supabase project: drlggmunipoxxxoeqkje (customer app, connect/influencer
-- app, and the inzpire-admin web portal all point here).
--
-- Idempotent — safe to re-run in the Supabase SQL editor (same as seed.sql).
--
-- What it does
--   1. Extends public.notifications (title, type, data) — the unified in-app store.
--   2. Adds public.device_tokens — FCM registration tokens per user/device.
--   3. Adds private.push_settings — the send-push function URL + dispatch secret
--      (fill this in once; see NOTIFICATIONS_SETUP.md).
--   4. notify_users() — inserts N notification rows (SECURITY DEFINER, bypasses RLS).
--   5. Per-event triggers that call notify_users():
--        · leads INSERT (referral)            → admins            [in-app]
--        · referral_events INSERT             → admins            [in-app]
--        · referral_events approved/go_live   → influencer        [push]
--        · team_assignments INSERT            → customer          [push]
--        · site_updates INSERT (customer)     → customer          [push]
--        · moodboards → 'sent' (design)       → customer          [push]
--        · messages INSERT                    → other participants[push]
--        · approvals INSERT                   → customer          [push]
--        · payments INSERT                    → customer          [push]
--        · commissions → paid                 → influencer        [push]
--   6. tg_dispatch_push — AFTER INSERT on notifications, fans out to the
--      send-push Edge Function over pg_net (which delivers via FCM v1).
-- ============================================================================

create extension if not exists pg_net;

-- ---------------------------------------------------------------------------
-- 1. notifications: add title / type / data (channel, body, is_read, link exist)
-- ---------------------------------------------------------------------------
alter table public.notifications add column if not exists title text;
alter table public.notifications add column if not exists type  text not null default 'general';
alter table public.notifications add column if not exists data  jsonb not null default '{}'::jsonb;

-- In-app bells stream this table over realtime (idempotent add to the publication).
do $$
begin
  if not exists (
    select 1 from pg_publication_tables
    where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'notifications'
  ) then
    alter publication supabase_realtime add table public.notifications;
  end if;
end $$;

-- Let users mark their own notifications read (mark-read from every client).
grant update on public.notifications to authenticated;
drop policy if exists "notifications own update" on public.notifications;
create policy "notifications own update" on public.notifications
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());

-- Let users clear (delete) their own notifications from every client.
grant delete on public.notifications to authenticated;
drop policy if exists "notifications own delete" on public.notifications;
create policy "notifications own delete" on public.notifications
  for delete to authenticated using (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- 2. device_tokens — one row per (user, FCM token)
-- ---------------------------------------------------------------------------
create table if not exists public.device_tokens (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid not null references auth.users(id) on delete cascade,
  token      text not null unique,
  app        text not null default 'customer',   -- 'customer' | 'connect'
  platform   text not null default 'android',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists device_tokens_user_idx on public.device_tokens(user_id);

grant select, insert, update, delete on public.device_tokens to authenticated;
grant all on public.device_tokens to service_role;
alter table public.device_tokens enable row level security;

drop policy if exists "device_tokens own select" on public.device_tokens;
create policy "device_tokens own select" on public.device_tokens
  for select to authenticated using (user_id = auth.uid());
drop policy if exists "device_tokens own insert" on public.device_tokens;
create policy "device_tokens own insert" on public.device_tokens
  for insert to authenticated with check (user_id = auth.uid());
drop policy if exists "device_tokens own update" on public.device_tokens;
create policy "device_tokens own update" on public.device_tokens
  for update to authenticated using (user_id = auth.uid()) with check (user_id = auth.uid());
drop policy if exists "device_tokens own delete" on public.device_tokens;
create policy "device_tokens own delete" on public.device_tokens
  for delete to authenticated using (user_id = auth.uid());

create or replace function public.tg_set_device_token_updated()
returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;
drop trigger if exists trg_device_tokens_updated on public.device_tokens;
create trigger trg_device_tokens_updated before update on public.device_tokens
  for each row execute function public.tg_set_device_token_updated();

-- ---------------------------------------------------------------------------
-- 3. private.push_settings — populated once (see NOTIFICATIONS_SETUP.md).
--    Kept out of RLS-reachable schema; only the SECURITY DEFINER dispatcher reads it.
-- ---------------------------------------------------------------------------
create schema if not exists private;
create table if not exists private.push_settings (
  id              boolean primary key default true,
  function_url    text not null,
  dispatch_secret text not null,
  constraint push_settings_singleton check (id)
);
revoke all on private.push_settings from anon, authenticated;

-- ---------------------------------------------------------------------------
-- 4. notify_users(recipients, type, title, body, link, data, channel)
--    Inserts one notification per distinct recipient. SECURITY DEFINER so it
--    can write regardless of the caller's RLS.
-- ---------------------------------------------------------------------------
create or replace function public.notify_users(
  _recipients uuid[],
  _type       text,
  _title      text,
  _body       text,
  _link       text default null,
  _data       jsonb default '{}'::jsonb,
  _channel    public.notification_channel default 'push'
) returns void
language plpgsql security definer set search_path = public as $$
declare uid uuid;
begin
  foreach uid in array coalesce(_recipients, '{}') loop
    if uid is null then continue; end if;
    insert into public.notifications (user_id, channel, type, title, body, link, data)
    values (uid, _channel, _type, _title, _body, _link, _data);
  end loop;
end $$;

create or replace function public.admin_user_ids()
returns uuid[] language sql stable security definer set search_path = public as $$
  select coalesce(array_agg(distinct user_id), '{}')
  from public.user_roles
  where role in ('super_admin','ops_admin');
$$;

create or replace function public.display_name(_uid uuid)
returns text language sql stable security definer set search_path = public as $$
  select coalesce(nullif(p.name, ''), 'Someone') from public.profiles p where p.id = _uid;
$$;

-- ---------------------------------------------------------------------------
-- 5. Per-event triggers
-- ---------------------------------------------------------------------------

-- (1) New referral lead -> admins (in-app). Referral leads arrive via the
--     connect app (source='connect') or carry an influencer_id.
create or replace function public.tg_notify_referral_lead()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if new.source = 'connect' or new.influencer_id is not null then
    perform public.notify_users(
      public.admin_user_ids(),
      'referral_lead',
      'New referral lead',
      coalesce(nullif(new.requirement, ''), 'A new referral lead was submitted'),
      '/admin/leads/' || new.id,
      jsonb_build_object('lead_id', new.id, 'influencer_id', new.influencer_id),
      'push'
    );
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_referral_lead on public.leads;
create trigger trg_notify_referral_lead after insert on public.leads
  for each row execute function public.tg_notify_referral_lead();

-- (2 + 7) Referral stage update -> admins always; influencer on key milestones.
create or replace function public.tg_notify_referral_event()
returns trigger language plpgsql security definer set search_path = public as $$
declare inf uuid; lead uuid;
begin
  select influencer_id, lead_id into inf, lead from public.referrals where id = new.referral_id;

  -- admins: every stage update
  perform public.notify_users(
    public.admin_user_ids(),
    'referral_stage',
    'Referral: ' || replace(new.milestone::text, '_', ' '),
    coalesce(nullif(new.note, ''), 'Referral moved to ' || replace(new.milestone::text, '_', ' ')),
    '/admin/referrals/' || new.referral_id,
    jsonb_build_object('referral_id', new.referral_id, 'milestone', new.milestone),
    'push'
  );

  -- influencer: accepted (approved) and finished (go_live / bonus_released)
  if inf is not null and new.milestone in ('approved','go_live','bonus_released') then
    perform public.notify_users(
      array[inf],
      'referral_update',
      case new.milestone
        when 'approved' then 'Your referral was accepted 🎉'
        when 'go_live'  then 'Your referral project is live'
        else 'Your referral bonus was released'
      end,
      case new.milestone
        when 'approved' then 'Admin accepted your referral for a new project.'
        when 'go_live'  then 'The project from your referral has gone live.'
        else 'Your referral bonus has been released.'
      end,
      '/referrals/' || new.referral_id,
      jsonb_build_object('referral_id', new.referral_id, 'milestone', new.milestone),
      'push'
    );
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_referral_event on public.referral_events;
create trigger trg_notify_referral_event after insert on public.referral_events
  for each row execute function public.tg_notify_referral_event();

-- (3) Team assigned to a project -> customer (push), ONCE per project.
--     A project team has several members (manager, designer, …) inserted as
--     separate team_assignments rows. This per-row trigger fired once per member,
--     so setting a 4-person team spammed the customer with 4 near-identical
--     "Your project team is set" notifications. We now notify the customer only
--     the first time a member is assigned to the project: the guard skips if they
--     already have a project_assignment notification for this project (visible
--     across rows of the same batch/transaction, so batched inserts collapse to 1).
create or replace function public.tg_notify_team_assignment()
returns trigger language plpgsql security definer set search_path = public as $$
declare cust uuid; pname text;
begin
  select customer_id, coalesce(nullif(name,''),'your project') into cust, pname
  from public.projects where id = new.project_id;
  if cust is not null and cust <> new.user_id
     and not exists (
       select 1 from public.notifications
       where user_id = cust
         and type = 'project_assignment'
         and data->>'project_id' = new.project_id::text
     ) then
    perform public.notify_users(
      array[cust],
      'project_assignment',
      'Your project team is set',
      'Your team for ' || pname || ' has been assigned. Tap to see who''s working on your project.',
      '/app',
      jsonb_build_object('project_id', new.project_id),
      'push'
    );
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_team_assignment on public.team_assignments;
create trigger trg_notify_team_assignment after insert on public.team_assignments
  for each row execute function public.tg_notify_team_assignment();

-- (4) Site update posted -> customer (push), only when visible to the customer.
create or replace function public.tg_notify_site_update()
returns trigger language plpgsql security definer set search_path = public as $$
declare cust uuid;
begin
  if coalesce(new.visible_to_customer, true) then
    select customer_id into cust from public.projects where id = new.project_id;
    if cust is not null and cust <> coalesce(new.author_id, '00000000-0000-0000-0000-000000000000') then
      perform public.notify_users(
        array[cust],
        'site_update',
        'New project update',
        coalesce(nullif(new.note,''), 'A new site update was posted on your project.'),
        '/app',
        jsonb_build_object('project_id', new.project_id, 'site_update_id', new.id),
        'push'
      );
    end if;
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_site_update on public.site_updates;
create trigger trg_notify_site_update after insert on public.site_updates
  for each row execute function public.tg_notify_site_update();

-- (4b) Design shared -> customer (push). A moodboard / 2D layout / 3D / render
--      becomes visible to the customer when the admin "sends" it: status -> 'sent'.
--      Fires whether the row is inserted straight as 'sent' or moved to 'sent'
--      from 'draft'/'revision' (a re-send after requested changes re-notifies).
--      Guard on the status transition so editing an already-sent design is silent.
create or replace function public.tg_notify_moodboard_sent()
returns trigger language plpgsql security definer set search_path = public as $$
declare cust uuid; kind text;
begin
  if new.status = 'sent' and (tg_op = 'INSERT' or old.status is distinct from new.status) then
    select customer_id into cust from public.projects where id = new.project_id;
    if cust is not null then
      kind := case new.type::text
                when 'two_d'   then '2D layout'
                when 'three_d' then '3D design'
                when 'render'  then 'render'
                else 'design'
              end;
      perform public.notify_users(
        array[cust],
        'design',
        'New ' || kind || ' ready for review',
        'Your designer shared "' || coalesce(nullif(new.title, ''), 'a new design') || '"'
          || case when coalesce(new.room, '') <> '' then ' for ' || new.room else '' end
          || '. Tap to review and approve.',
        '/designs',
        jsonb_build_object('project_id', new.project_id, 'moodboard_id', new.id, 'moodboard_type', new.type::text),
        'push'
      );
    end if;
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_moodboard_sent on public.moodboards;
create trigger trg_notify_moodboard_sent after insert or update on public.moodboards
  for each row execute function public.tg_notify_moodboard_sent();

-- (5) New message -> the other project participants (push), never the sender.
create or replace function public.tg_notify_message()
returns trigger language plpgsql security definer set search_path = public as $$
declare rec record; recipients uuid[];
begin
  select customer_id, manager_id, designer_id into rec from public.projects where id = new.project_id;
  recipients := array_remove(
    array_remove(array[rec.customer_id, rec.manager_id, rec.designer_id], new.sender_id),
    null);
  if array_length(recipients, 1) is not null then
    perform public.notify_users(
      recipients,
      'message',
      public.display_name(new.sender_id),
      coalesce(nullif(new.body,''), '📎 Attachment'),
      '/app/chat',
      jsonb_build_object('project_id', new.project_id, 'message_id', new.id, 'sender_id', new.sender_id),
      'push'
    );
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_message on public.messages;
create trigger trg_notify_message after insert on public.messages
  for each row execute function public.tg_notify_message();

-- (extra) Approval requested -> customer (push).
create or replace function public.tg_notify_approval()
returns trigger language plpgsql security definer set search_path = public as $$
declare cust uuid;
begin
  if new.status = 'pending' then
    select customer_id into cust from public.projects where id = new.project_id;
    if cust is not null then
      perform public.notify_users(
        array[cust],
        'approval',
        'Sign-off needed',
        coalesce(nullif(new.title,''), replace(new.type::text,'_',' ') || ' is ready for your approval.'),
        '/approvals',
        jsonb_build_object('project_id', new.project_id, 'approval_id', new.id, 'kind', new.type),
        'push'
      );
    end if;
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_approval on public.approvals;
create trigger trg_notify_approval after insert on public.approvals
  for each row execute function public.tg_notify_approval();

-- (extra) New payment / milestone -> customer (push).
create or replace function public.tg_notify_payment()
returns trigger language plpgsql security definer set search_path = public as $$
declare cust uuid;
begin
  if new.status = 'pending' then
    select customer_id into cust from public.projects where id = new.project_id;
    if cust is not null then
      perform public.notify_users(
        array[cust],
        'payment',
        'Payment due',
        coalesce(nullif(new.milestone,''),'A milestone') || ' — ₹' || trim(to_char(new.amount, 'FM999999999')),
        '/payments',
        jsonb_build_object('project_id', new.project_id, 'payment_id', new.id, 'amount', new.amount),
        'push'
      );
    end if;
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_payment on public.payments;
create trigger trg_notify_payment after insert on public.payments
  for each row execute function public.tg_notify_payment();

-- (extra) Referral commission marked paid -> influencer (push).
create or replace function public.tg_notify_commission_paid()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if new.status = 'paid' and coalesce(old.status,'pending') <> 'paid' then
    perform public.notify_users(
      array[new.owner_id],
      'commission',
      'Commission paid 💰',
      'Your commission of ₹' || trim(to_char(new.amount, 'FM999999999')) || ' has been paid.',
      '/earnings',
      jsonb_build_object('commission_id', new.id, 'amount', new.amount),
      'push'
    );
  end if;
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_notify_commission_paid on public.commissions;
create trigger trg_notify_commission_paid after update on public.commissions
  for each row execute function public.tg_notify_commission_paid();

-- ---------------------------------------------------------------------------
-- 6. Dispatch: AFTER INSERT on notifications -> send-push Edge Function (pg_net).
--    The Edge Function looks up device_tokens for the user and delivers via FCM.
--    Rows for users with no device tokens simply no-op inside the function.
-- ---------------------------------------------------------------------------
create or replace function public.tg_dispatch_push()
returns trigger language plpgsql security definer set search_path = public, extensions as $$
declare cfg private.push_settings%rowtype;
begin
  select * into cfg from private.push_settings where id = true;
  if cfg.function_url is null then
    return new;  -- not configured yet; in-app bell still works
  end if;
  perform net.http_post(
    url     := cfg.function_url,
    headers := jsonb_build_object('Content-Type','application/json','x-dispatch-secret', cfg.dispatch_secret),
    body    := jsonb_build_object('notification_id', new.id)
  );
  return new;
exception when others then
  return new;  -- never let a notification side-effect roll back the primary write
end $$;
drop trigger if exists trg_dispatch_push on public.notifications;
create trigger trg_dispatch_push after insert on public.notifications
  for each row execute function public.tg_dispatch_push();

-- ============================================================================
-- Done. Next: deploy the send-push Edge Function and set FCM_SERVICE_ACCOUNT +
-- PUSH_DISPATCH_SECRET, then insert the private.push_settings row.
-- See supabase/NOTIFICATIONS_SETUP.md.
-- ============================================================================
