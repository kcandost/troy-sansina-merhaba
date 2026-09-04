-- Fleet-wide (global) quotas per coupon amount, on top of per-robot limits.
-- Enforcement: fetch_config returns the amounts whose fleet usage hit their quota as
-- "paused"; tablets exclude those from the draw at their next 60s poll. Per-robot limits
-- stay offline-safe; the global cap is server-arbitrated with ≤1 poll of overshoot.

create table global_quotas (
  amount int primary key check (amount > 0),
  quota int not null check (quota >= 0)  -- 0 = no fleet cap
);

alter table global_quotas enable row level security;
create policy dashboard_read_quotas on global_quotas for select to authenticated using (true);

-- Fleet usage per amount under each robot's current config version.
create view global_usage_view with (security_invoker = true) as
  select u.amount, sum(u.used)::int as used
  from usage_view u
  group by u.amount;

create or replace function save_global_quotas(p_quotas jsonb)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  if (select auth.role()) <> 'authenticated' then
    raise exception 'not authorized';
  end if;
  if jsonb_typeof(p_quotas) <> 'array' then
    raise exception 'invalid payload';
  end if;
  delete from public.global_quotas;
  insert into public.global_quotas (amount, quota)
  select (e->>'amount')::int, (e->>'quota')::int from jsonb_array_elements(p_quotas) e
  where (e->>'quota')::int > 0;
end;
$$;

revoke execute on function save_global_quotas(jsonb) from public, anon;

-- fetch_config now also reports globally paused amounts.
create or replace function fetch_config(p_token text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot text;
  v_result jsonb;
  v_paused jsonb;
begin
  select id into v_robot from public.robots where device_token = p_token;
  if v_robot is null then
    raise exception 'invalid token';
  end if;
  update public.robots set last_seen_at = now() where id = v_robot;
  select coalesce(jsonb_agg(q.amount), '[]'::jsonb) into v_paused
  from public.global_quotas q
  where q.quota > 0 and (
    select count(*) from public.grants g
    join public.robot_configs c on c.robot_id = g.robot_id and c.version = g.config_version
    where g.amount = q.amount
  ) >= q.quota;
  select jsonb_build_object('version', version, 'promos', promos, 'paused', v_paused)
    into v_result from public.robot_configs where robot_id = v_robot;
  return coalesce(v_result, jsonb_build_object('version', 0, 'promos', null, 'paused', v_paused));
end;
$$;
