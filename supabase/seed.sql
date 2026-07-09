-- ============================================================================
-- Inzpire demo seed — "NM Reddy / 3BHK Flat" cockpit
-- ----------------------------------------------------------------------------
-- Target : the NEW Supabase project (drlggmunipoxxxoeqkje) whose SCHEMA is
--          already applied (all tables + RLS from Inzpire Home Hub migrations),
--          but which has NO rows yet.
-- How    : paste into Supabase dashboard → SQL editor → Run (runs as the
--          privileged `postgres` role, so it can write auth.users & bypass RLS).
-- Safe to re-run: idempotent (demo child rows are deleted + re-inserted; users
--          are created only if missing).
--
-- After running, the customer Android app can sign in as:
--     email    : nmreddy@demo.inzpire.app
--     password : Inzpire@123
-- and see the fully wired cockpit (Home / Designs / Materials / Pay / Approvals
-- / Documents / Chat) backed by real rows below.
--
-- Staff logins (for the web admin / to post site updates & chat replies):
--     sneha@demo.inzpire.app   / Inzpire@123   (Interior Designer)
--     karthik@demo.inzpire.app / Inzpire@123   (Project Manager)
-- ============================================================================

create extension if not exists pgcrypto with schema extensions;

-- ---------------------------------------------------------------------------
-- 0. Additive table the base schema is missing: customer-visible documents.
--    (The web app renders Documents off a frontend constant; give it a real,
--    RLS-scoped table so the native Documents screen is live too.)
-- ---------------------------------------------------------------------------
create table if not exists public.project_documents (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references public.projects(id) on delete cascade,
  name text not null,
  doc_type text,
  file_url text,
  created_at timestamptz not null default now()
);
grant select, insert, update, delete on public.project_documents to authenticated;
grant all on public.project_documents to service_role;
alter table public.project_documents enable row level security;

do $$ begin
  if not exists (select 1 from pg_policies where policyname = 'docs_project_read') then
    create policy "docs_project_read" on public.project_documents for select to authenticated using (
      exists (select 1 from public.projects p where p.id = project_documents.project_id
        and (p.customer_id = auth.uid() or p.manager_id = auth.uid() or p.designer_id = auth.uid()
             or public.is_staff(auth.uid()))));
  end if;
  if not exists (select 1 from pg_policies where policyname = 'docs_staff_write') then
    create policy "docs_staff_write" on public.project_documents for all to authenticated using (
      exists (select 1 from public.projects p where p.id = project_documents.project_id
        and (p.manager_id = auth.uid() or public.has_role(auth.uid(),'ops_admin') or public.has_role(auth.uid(),'super_admin'))))
    with check (
      exists (select 1 from public.projects p where p.id = project_documents.project_id
        and (p.manager_id = auth.uid() or public.has_role(auth.uid(),'ops_admin') or public.has_role(auth.uid(),'super_admin'))));
  end if;
end $$;
create index if not exists idx_project_documents_project on public.project_documents(project_id);

-- ---------------------------------------------------------------------------
-- 1. Demo auth users (customer + designer + PM). The handle_new_user() trigger
--    creates the matching profiles + user_roles rows from raw_user_meta_data.
-- ---------------------------------------------------------------------------
create or replace function public._seed_user(
  p_id uuid, p_email text, p_name text, p_phone text, p_role text, p_pw text
) returns void language plpgsql security definer
  set search_path = public, auth, extensions as $$
begin
  if not exists (select 1 from auth.users where id = p_id) then
    insert into auth.users (
      instance_id, id, aud, role, email, encrypted_password,
      email_confirmed_at, created_at, updated_at,
      raw_app_meta_data, raw_user_meta_data,
      confirmation_token, recovery_token, email_change_token_new, email_change
    ) values (
      '00000000-0000-0000-0000-000000000000', p_id, 'authenticated', 'authenticated',
      p_email, extensions.crypt(p_pw, extensions.gen_salt('bf')),
      now(), now(), now(),
      '{"provider":"email","providers":["email"]}'::jsonb,
      jsonb_build_object('name', p_name, 'phone', p_phone, 'role', p_role),
      '', '', '', ''
    );
    insert into auth.identities (
      id, provider_id, user_id, identity_data, provider,
      last_sign_in_at, created_at, updated_at
    ) values (
      gen_random_uuid(), p_id::text, p_id,
      jsonb_build_object('sub', p_id::text, 'email', p_email), 'email',
      now(), now(), now()
    );
  end if;
end $$;

select public._seed_user('11111111-1111-1111-1111-111111111111', 'nmreddy@demo.inzpire.app',    'NM Reddy',      '+919900000001', 'customer',           'Inzpire@123');
select public._seed_user('22222222-2222-2222-2222-222222222222', 'sneha@demo.inzpire.app',      'Sneha Rao',     '+919900000010', 'interior_designer',  'Inzpire@123');
select public._seed_user('33333333-3333-3333-3333-333333333333', 'karthik@demo.inzpire.app',    'Karthik Verma', '+919900000011', 'project_manager',    'Inzpire@123');
-- Influencer (for the Inzpire Connect app): influencer@demo.inzpire.app / Inzpire@123
select public._seed_user('55555555-5555-5555-5555-555555555555', 'influencer@demo.inzpire.app', 'Priya Menon',   '+919900000055', 'influencer',         'Inzpire@123');

update public.profiles set
  name = 'Priya Menon', location = 'Hyderabad', upi_id = 'priya@okhdfc', kyc_status = 'verified',
  photo_url = 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&q=80&auto=format&fit=crop'
  where id = '55555555-5555-5555-5555-555555555555';

-- Make sure staff also carry a customer-invisible staff role used by is_staff().
insert into public.user_roles (user_id, role) values
  ('22222222-2222-2222-2222-222222222222', 'interior_designer'),
  ('33333333-3333-3333-3333-333333333333', 'project_manager')
on conflict (user_id, role) do nothing;

-- Enrich profiles (name/phone/email are set by the trigger; add the rest).
update public.profiles set
  name = 'NM Reddy', location = 'Manikonda, Hyderabad', address = 'Manikonda, Hyderabad 500089',
  photo_url = 'https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=240&q=80&auto=format&fit=crop'
  where id = '11111111-1111-1111-1111-111111111111';
update public.profiles set
  name = 'Sneha Rao', location = 'Hyderabad',
  photo_url = 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&q=80&auto=format&fit=crop'
  where id = '22222222-2222-2222-2222-222222222222';
update public.profiles set
  name = 'Karthik Verma', location = 'Hyderabad',
  photo_url = 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80&auto=format&fit=crop'
  where id = '33333333-3333-3333-3333-333333333333';

-- Staff directory cards (used by web admin / team lists).
insert into public.profiles_staff (user_id, designation, avatar, specialities) values
  ('22222222-2222-2222-2222-222222222222', 'Interior Designer', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200&q=80&auto=format&fit=crop', array['Residential','Modular']),
  ('33333333-3333-3333-3333-333333333333', 'Project Manager',   'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&q=80&auto=format&fit=crop', array['Execution','Handover'])
on conflict (user_id) do nothing;

-- ---------------------------------------------------------------------------
-- 2. The project + all its cockpit child rows (deterministic UUIDs).
-- ---------------------------------------------------------------------------
insert into public.projects (
  id, customer_id, manager_id, designer_id, name, code, segment, status, stage,
  start_date, value, target_days, target_handover_date, published_to_customer
) values (
  '44444444-4444-4444-4444-444444444444',
  '11111111-1111-1111-1111-111111111111',
  '33333333-3333-3333-3333-333333333333',
  '22222222-2222-2222-2222-222222222222',
  '3BHK Flat', 'INZ-2026-014', 'Residential', 'active', 2,
  '2026-05-20', 1850000, 50, '2026-07-09', true
) on conflict (id) do update set
  manager_id = excluded.manager_id, designer_id = excluded.designer_id,
  name = excluded.name, code = excluded.code, segment = excluded.segment,
  status = excluded.status, stage = excluded.stage, start_date = excluded.start_date,
  value = excluded.value, target_days = excluded.target_days,
  target_handover_date = excluded.target_handover_date, published_to_customer = true;

-- Clean child rows for a clean re-run.
delete from public.rooms             where project_id = '44444444-4444-4444-4444-444444444444';  -- cascades selections
delete from public.moodboards        where project_id = '44444444-4444-4444-4444-444444444444';
delete from public.payments          where project_id = '44444444-4444-4444-4444-444444444444';
delete from public.approvals         where project_id = '44444444-4444-4444-4444-444444444444';
delete from public.site_updates      where project_id = '44444444-4444-4444-4444-444444444444';
delete from public.messages          where project_id = '44444444-4444-4444-4444-444444444444';
delete from public.project_documents where project_id = '44444444-4444-4444-4444-444444444444';

-- Rooms
insert into public.rooms (id, project_id, name, area_sft) values
  ('44444444-4444-4444-4444-000000000001', '44444444-4444-4444-4444-444444444444', 'Living Room',   320),
  ('44444444-4444-4444-4444-000000000002', '44444444-4444-4444-4444-444444444444', 'Kitchen',       120),
  ('44444444-4444-4444-4444-000000000003', '44444444-4444-4444-4444-444444444444', 'Master Bedroom',180),
  ('44444444-4444-4444-4444-000000000004', '44444444-4444-4444-4444-444444444444', 'Kids Bedroom',  140);

-- Material swatch products (image_url = swatch). Customer role can read pricing.
insert into public.products (code, category, description, unit, rate, make_notes, image_url) values
  ('MAT-LAM-01','Laminate',  'Walnut Natural',    'Sft', 1450, 'Merino',      'https://images.unsplash.com/photo-1604147706283-d7119b5b822c?w=400&q=80&auto=format&fit=crop'),
  ('MAT-FLR-01','Flooring',  'Italian Marble',    'Sft',  850, 'Kajaria',     'https://images.unsplash.com/photo-1615873968403-89e068629265?w=400&q=80&auto=format&fit=crop'),
  ('MAT-CTP-01','Countertop','Quartz Carrara',    'Rft', 1200, 'Caesarstone', 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?w=400&q=80&auto=format&fit=crop'),
  ('MAT-HW-01', 'Hardware',  'Soft-close Hinges', 'No',   380, 'Hettich',     'https://images.unsplash.com/photo-1581858726788-75bc0f6a952d?w=400&q=80&auto=format&fit=crop'),
  ('MAT-VEN-01','Veneer',    'Teak Veneer',       'Sft',  260, 'Greenply',    'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=400&q=80&auto=format&fit=crop'),
  ('MAT-PNT-01','Paint',     'Warm Ivory',        'Sft',   38, 'Asian Royale','https://images.unsplash.com/photo-1562184552-997c461cc6f6?w=400&q=80&auto=format&fit=crop')
on conflict (code) do update set image_url = excluded.image_url, description = excluded.description,
  category = excluded.category, make_notes = excluded.make_notes;

-- Material selections (grouped by room in the UI)
insert into public.selections (room_id, category, product_id, make, qty, unit_rate_snapshot, status)
select r.id, v.category, p.id, v.make, 1, p.rate, v.status::public.selection_status
from (values
  ('Living Room',   'Laminate',  'MAT-LAM-01','Merino',      'locked'),
  ('Living Room',   'Flooring',  'MAT-FLR-01','Kajaria',     'selected'),
  ('Kitchen',       'Countertop','MAT-CTP-01','Caesarstone', 'selected'),
  ('Kitchen',       'Hardware',  'MAT-HW-01', 'Hettich',     'locked'),
  ('Master Bedroom','Veneer',    'MAT-VEN-01','Greenply',    'suggested'),
  ('Master Bedroom','Paint',     'MAT-PNT-01','Asian Royale','suggested')
) as v(room, category, code, make, status)
join public.rooms r on r.project_id = '44444444-4444-4444-4444-444444444444' and r.name = v.room
join public.products p on p.code = v.code;

-- Designs (moodboards)
insert into public.moodboards (project_id, room, title, media_urls, type, version, status) values
  ('44444444-4444-4444-4444-444444444444','Living Room',   'Living — Modern Warm',    array['https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=900&q=80&auto=format&fit=crop'], 'three_d','2','approved'),
  ('44444444-4444-4444-4444-444444444444','Kitchen',       'Modular Kitchen Layout',  array['https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=900&q=80&auto=format&fit=crop'], 'two_d','1','approved'),
  ('44444444-4444-4444-4444-444444444444','Master Bedroom','Master — Calm Luxe',      array['https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80&auto=format&fit=crop'], 'three_d','1','sent'),
  ('44444444-4444-4444-4444-444444444444','Kids Bedroom',  'Kids Room Concept',       array['https://images.unsplash.com/photo-1558877385-8c1f51fdbf2c?w=900&q=80&auto=format&fit=crop'], 'moodboard','1','sent');

-- Payments (30/30/30/10)
insert into public.payments (project_id, milestone, amount, status, paid_at) values
  ('44444444-4444-4444-4444-444444444444','Booking (30%)',       555000, 'paid', '2026-05-20'),
  ('44444444-4444-4444-4444-444444444444','Production (30%)',    555000, 'pending', null),
  ('44444444-4444-4444-4444-444444444444','Installation (30%)',  555000, 'pending', null),
  ('44444444-4444-4444-4444-444444444444','Handover (10%)',      185000, 'pending', null);

-- Approvals
insert into public.approvals (project_id, type, title, status, approver_id, signed_at) values
  ('44444444-4444-4444-4444-444444444444','design',   'Living Room 3D — v2',        'approved', '11111111-1111-1111-1111-111111111111', '2026-06-02'),
  ('44444444-4444-4444-4444-444444444444','design',   'Master Bedroom 3D — v1',     'pending',  null, null),
  ('44444444-4444-4444-4444-444444444444','material', 'Living Room Material Board',  'pending',  null, null),
  ('44444444-4444-4444-4444-444444444444','quotation','Revised BOQ — v2',           'approved', '11111111-1111-1111-1111-111111111111', '2026-06-04');

-- Site updates (customer-visible), authored by the PM
insert into public.site_updates (project_id, author_id, media_urls, geotag, note, visible_to_customer) values
  ('44444444-4444-4444-4444-444444444444','33333333-3333-3333-3333-333333333333', array['https://images.unsplash.com/photo-1503387837-b154d5074bd2?w=800&q=80&auto=format&fit=crop'], 'Manikonda', 'Civil & false ceiling framework started', true),
  ('44444444-4444-4444-4444-444444444444','33333333-3333-3333-3333-333333333333', array['https://images.unsplash.com/photo-1556909114-44e3e9399a2f?w=800&q=80&auto=format&fit=crop'], 'Manikonda', 'Kitchen base units measured on site', true);

-- Chat messages
insert into public.messages (project_id, sender_id, body, created_at) values
  ('44444444-4444-4444-4444-444444444444','22222222-2222-2222-2222-222222222222','Hi NM! The Living Room 3D v2 is ready — please review and approve when you can.', now() - interval '3 days'),
  ('44444444-4444-4444-4444-444444444444','11111111-1111-1111-1111-111111111111','Looks fantastic, Sneha. Approving it now.', now() - interval '3 days' + interval '20 min'),
  ('44444444-4444-4444-4444-444444444444','33333333-3333-3333-3333-333333333333','Great — civil work has begun on site. I''ll keep posting daily site updates here.', now() - interval '1 day');

-- Documents
insert into public.project_documents (project_id, name, doc_type, created_at) values
  ('44444444-4444-4444-4444-444444444444','Signed Contract.pdf','Contract','2026-05-18'),
  ('44444444-4444-4444-4444-444444444444','Invoice — Booking.pdf','Invoice','2026-05-20'),
  ('44444444-4444-4444-4444-444444444444','Final BOQ v2.pdf','BOQ','2026-06-05'),
  ('44444444-4444-4444-4444-444444444444','Working Drawings.pdf','Drawing','2026-05-28');

-- ---------------------------------------------------------------------------
-- 3. Global content (offers + inspirations) — visible to everyone signed in.
-- ---------------------------------------------------------------------------
delete from public.offers where title in ('Monsoon Interior Fest','Refer & Earn ₹10,000','Smart Home Starter');
insert into public.offers (title, body, valid_till, is_active) values
  ('Monsoon Interior Fest', 'Flat 12% off modular kitchens & wardrobes booked this month. Free 3D design for full-home projects.', '2026-08-31', true),
  ('Refer & Earn ₹10,000',  'Refer a friend building their home — earn ₹10,000 on every successful conversion via Inzpire Connect.', null, true),
  ('Smart Home Starter',    'Add home automation (smart switches + video doorbell) at 15% off when bundled with interiors.', '2026-07-31', true);

delete from public.inspirations where title in ('Warm Minimal Living','Handleless Modular Kitchen','Calm Luxe Bedroom','Playful Kids Room');
insert into public.inspirations (title, room_type, style_tag, segment, budget_band, image_url) values
  ('Warm Minimal Living','Living Room','Modern','residential','15-30L','https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=900&q=80&auto=format&fit=crop'),
  ('Handleless Modular Kitchen','Kitchen','Contemporary','residential','5-15L','https://images.unsplash.com/photo-1556909114-f6e7ad7d3136?w=900&q=80&auto=format&fit=crop'),
  ('Calm Luxe Bedroom','Bedroom','Luxe','residential','15-30L','https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?w=900&q=80&auto=format&fit=crop'),
  ('Playful Kids Room','Kids Room','Playful','residential','<5L','https://images.unsplash.com/photo-1558877385-8c1f51fdbf2c?w=900&q=80&auto=format&fit=crop')
on conflict do nothing;

-- ---------------------------------------------------------------------------
-- 4. Inzpire Connect demo: influencer referrals across the milestone funnel.
-- ---------------------------------------------------------------------------
delete from public.referral_events where referral_id in (
  '55555555-5555-5555-5555-000000000001','55555555-5555-5555-5555-000000000002',
  '55555555-5555-5555-5555-000000000003','55555555-5555-5555-5555-000000000004',
  '55555555-5555-5555-5555-000000000005');
delete from public.referrals where id in (
  '55555555-5555-5555-5555-000000000001','55555555-5555-5555-5555-000000000002',
  '55555555-5555-5555-5555-000000000003','55555555-5555-5555-5555-000000000004',
  '55555555-5555-5555-5555-000000000005');

insert into public.referrals (id, influencer_id, referred_name, referred_phone, referred_location, requirement, budget_estimate, milestone, status, bonus_pct, bonus_amount, project_value, is_paid, created_at) values
  ('55555555-5555-5555-5555-000000000001','55555555-5555-5555-5555-555555555555','Anitha Rao','+919812300001','Gachibowli','Full Home Interiors · 3BHK', 2200000, 'bonus_released','paid',      2, 44000, 2200000, true,  now() - interval '40 days'),
  ('55555555-5555-5555-5555-000000000002','55555555-5555-5555-5555-555555555555','Ravi Kumar','+919812300002','Kondapur',  'Kitchen + Wardrobes',       1000000, 'go_live',       'converted', 2, 20000, 1000000, false, now() - interval '20 days'),
  ('55555555-5555-5555-5555-000000000003','55555555-5555-5555-5555-555555555555','Meera S',   '+919812300003','Madhapur',  'Full Home Interiors',       2200000, 'proposal_sent', 'verified',  2, 44000, null,    false, now() - interval '10 days'),
  ('55555555-5555-5555-5555-000000000004','55555555-5555-5555-5555-555555555555','Sunil P',   '+919812300004','Kukatpally','Renovation',                1000000, 'contacted',     'verified',  2, 20000, null,    false, now() - interval '4 days'),
  ('55555555-5555-5555-5555-000000000005','55555555-5555-5555-5555-555555555555','Divya N',   '+919812300005','Miyapur',   'Commercial Interior',       4000000, 'referred',      'submitted', 2, 80000, null,    false, now() - interval '1 day');

insert into public.referral_events (referral_id, milestone, note, created_at) values
  ('55555555-5555-5555-5555-000000000001','referred','Referral received', now() - interval '40 days'),
  ('55555555-5555-5555-5555-000000000001','contacted','Team reached out', now() - interval '38 days'),
  ('55555555-5555-5555-5555-000000000001','meeting_done','Design consultation done', now() - interval '35 days'),
  ('55555555-5555-5555-5555-000000000001','proposal_sent','Proposal & budget shared', now() - interval '32 days'),
  ('55555555-5555-5555-5555-000000000001','approved','Design & budget approved', now() - interval '28 days'),
  ('55555555-5555-5555-5555-000000000001','go_live','Contract signed — bonus locked in', now() - interval '25 days'),
  ('55555555-5555-5555-5555-000000000001','bonus_released','₹44,000 paid to wallet', now() - interval '18 days'),
  ('55555555-5555-5555-5555-000000000002','referred','Referral received', now() - interval '20 days'),
  ('55555555-5555-5555-5555-000000000002','contacted','Team reached out', now() - interval '18 days'),
  ('55555555-5555-5555-5555-000000000002','meeting_done','Design consultation done', now() - interval '15 days'),
  ('55555555-5555-5555-5555-000000000002','proposal_sent','Proposal & budget shared', now() - interval '12 days'),
  ('55555555-5555-5555-5555-000000000002','approved','Design & budget approved', now() - interval '9 days'),
  ('55555555-5555-5555-5555-000000000002','go_live','Contract signed — bonus locked in', now() - interval '6 days'),
  ('55555555-5555-5555-5555-000000000003','referred','Referral received', now() - interval '10 days'),
  ('55555555-5555-5555-5555-000000000003','contacted','Team reached out', now() - interval '8 days'),
  ('55555555-5555-5555-5555-000000000003','proposal_sent','Proposal & budget shared', now() - interval '5 days'),
  ('55555555-5555-5555-5555-000000000004','referred','Referral received', now() - interval '4 days'),
  ('55555555-5555-5555-5555-000000000004','contacted','Team reached out', now() - interval '2 days'),
  ('55555555-5555-5555-5555-000000000005','referred','Referral received', now() - interval '1 day');

-- Tidy up the helper.
drop function if exists public._seed_user(uuid, text, text, text, text, text);

-- Done. Sign into the customer app as nmreddy@demo.inzpire.app / Inzpire@123.
