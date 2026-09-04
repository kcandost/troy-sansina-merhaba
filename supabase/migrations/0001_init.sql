-- Troy Sansina fleet backend: robots, per-robot coupon config, grant event log.
-- Spec: docs/superpowers/specs/2026-09-03-backend-architecture-design.md

create table robots (
  id text primary key,
  name text not null,
  device_token text not null unique,
  last_seen_at timestamptz
);

-- promos: [{"amount":250,"weight":40,"limit":0}] — weights sum to 100, 2..6 entries, limit 0 = unlimited.
create or replace function check_promos(p jsonb)
returns boolean
language sql immutable set search_path = ''
as $$
  select jsonb_typeof(p) = 'array'
     and jsonb_array_length(p) between 2 and 6
     and (select sum((e->>'weight')::int) from jsonb_array_elements(p) e) = 100
     and not exists (
       select 1 from jsonb_array_elements(p) e
       where (e->>'amount')::int <= 0 or (e->>'weight')::int < 0 or coalesce((e->>'limit')::int, 0) < 0
     );
$$;

create table robot_configs (
  robot_id text primary key references robots (id) on delete cascade,
  version int not null default 1,
  promos jsonb not null check (check_promos(promos)),
  updated_at timestamptz not null default now()
);

create table grants (
  id bigint generated always as identity primary key,
  robot_id text not null references robots (id) on delete cascade,
  amount int not null,
  config_version int not null,
  granted_at timestamptz not null,
  client_uuid uuid not null unique
);

create index grants_robot_id_idx on grants (robot_id);

-- Usage under the CURRENT config version only (counters reset when config changes),
-- matching the tablet's local enforcement counters.
create view usage_view with (security_invoker = true) as
  select g.robot_id, g.amount, count(*)::int as used
  from grants g
  join robot_configs c on c.robot_id = g.robot_id and c.version = g.config_version
  group by g.robot_id, g.amount;

-- ── RLS: anon key alone can do nothing; dashboard (authenticated) reads all, manages robots/configs.
alter table robots enable row level security;
alter table robot_configs enable row level security;
alter table grants enable row level security;

create policy dashboard_read_robots on robots for select to authenticated using (true);
create policy dashboard_write_robots on robots for insert to authenticated with check (true);
create policy dashboard_update_robots on robots for update to authenticated using (true) with check (true);
create policy dashboard_read_configs on robot_configs for select to authenticated using (true);
create policy dashboard_read_grants on grants for select to authenticated using (true);
-- Config writes go through save_config() so the version bump can't be forgotten.

-- ── Robot RPCs: called with the anon key; the device token is the credential.

-- Bulk-insert grant events; duplicates (retried flushes) are ignored. Returns rows accepted.
create or replace function ingest_grants(p_token text, p_events jsonb)
returns int
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot text;
  v_count int;
begin
  select id into v_robot from public.robots where device_token = p_token;
  if v_robot is null then
    raise exception 'invalid token';
  end if;
  with ins as (
    insert into public.grants (robot_id, amount, config_version, granted_at, client_uuid)
    select v_robot, (e->>'amount')::int, (e->>'config_version')::int,
           (e->>'granted_at')::timestamptz, (e->>'client_uuid')::uuid
    from jsonb_array_elements(p_events) e
    on conflict (client_uuid) do nothing
    returning 1
  )
  select count(*) into v_count from ins;
  update public.robots set last_seen_at = now() where id = v_robot;
  return v_count;
end;
$$;

-- Current config for the calling robot: {"version": n, "promos": [...]}; null promos if none set yet.
create or replace function fetch_config(p_token text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot text;
  v_result jsonb;
begin
  select id into v_robot from public.robots where device_token = p_token;
  if v_robot is null then
    raise exception 'invalid token';
  end if;
  update public.robots set last_seen_at = now() where id = v_robot;
  select jsonb_build_object('version', version, 'promos', promos)
    into v_result from public.robot_configs where robot_id = v_robot;
  return coalesce(v_result, jsonb_build_object('version', 0, 'promos', null));
end;
$$;

-- Dashboard config push; bumps version so tablets detect the change. Returns the new version.
create or replace function save_config(p_robot text, p_promos jsonb)
returns int
language plpgsql security definer set search_path = ''
as $$
declare
  v_version int;
begin
  if (select auth.role()) <> 'authenticated' then
    raise exception 'not authorized';
  end if;
  insert into public.robot_configs (robot_id, version, promos)
  values (p_robot, 1, p_promos)
  on conflict (robot_id) do update
    set version = robot_configs.version + 1, promos = excluded.promos, updated_at = now()
  returning version into v_version;
  return v_version;
end;
$$;

revoke execute on function save_config(text, jsonb) from public, anon;
