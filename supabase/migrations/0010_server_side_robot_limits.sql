-- Server-arbitrated per-robot limits (2026-09-06). The tablet enforces its per-robot
-- limit from a LOCAL counter, which an uninstall+reinstall wipes while the server
-- keeps every grant under the same config version (0008). MEYDAN 1 reached 79/50 on
-- 750 TL this way after the 5 Sept reinstall. No APK change: fetch_config's `paused`
-- list already excludes amounts from the draw without touching local counters, so we
-- fold "this robot's server-side usage under its current config ≥ its limit" into it.
-- Same contract as the fleet quota: ≤1 poll (60 s) of overshoot, offline tablets fall
-- back to their local counter, a save_config version bump lifts the pause.

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
  with fleet as (
    select q.amount
    from public.global_quotas q
    where q.quota > 0 and (
      select count(*) from public.grants g
      join public.robot_configs c on c.robot_id = g.robot_id and c.version = g.config_version
      where g.amount = q.amount
    ) >= q.quota
  ), mine as (
    select (p->>'amount')::int as amount
    from public.robot_configs c, jsonb_array_elements(c.promos) p
    where c.robot_id = v_robot
      and coalesce((p->>'limit')::int, 0) > 0
      and (
        select count(*) from public.grants g
        where g.robot_id = v_robot and g.config_version = c.version
          and g.amount = (p->>'amount')::int
      ) >= (p->>'limit')::int
  )
  select coalesce(jsonb_agg(distinct amount), '[]'::jsonb) into v_paused
  from (select amount from fleet union select amount from mine) u;
  select jsonb_build_object('version', version, 'promos', promos, 'paused', v_paused)
    into v_result from public.robot_configs where robot_id = v_robot;
  return coalesce(v_result, jsonb_build_object('version', 0, 'promos', null, 'paused', v_paused));
end;
$$;
