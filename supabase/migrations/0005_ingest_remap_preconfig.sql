-- Grants played before the tablet's first config poll arrive stamped version 0 and
-- would never match usage_view's current-version join. Fold them into the robot's
-- config version at ingest time instead.
create or replace function ingest_grants(p_token text, p_events jsonb)
returns int
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot text;
  v_current int;
  v_count int;
begin
  select id into v_robot from public.robots where device_token = p_token;
  if v_robot is null then
    raise exception 'invalid token';
  end if;
  select version into v_current from public.robot_configs where robot_id = v_robot;
  with ins as (
    insert into public.grants (robot_id, amount, config_version, granted_at, client_uuid)
    select v_robot, (e->>'amount')::int,
           coalesce(nullif((e->>'config_version')::int, 0), v_current, 0),
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

-- Repair grants already orphaned at version 0.
update grants g set config_version = c.version
from robot_configs c
where c.robot_id = g.robot_id and g.config_version = 0;
