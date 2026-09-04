-- Complement to the ingest remap: grants ingested while the robot had NO config at all
-- stay at version 0. When the robot's first config is created, fold them into it.
create or replace function save_config(p_robot text, p_promos jsonb)
returns int
language plpgsql security definer set search_path = ''
as $$
declare
  v_version int;
  v_existed boolean;
begin
  if (select auth.role()) <> 'authenticated' then
    raise exception 'not authorized';
  end if;
  select exists(select 1 from public.robot_configs where robot_id = p_robot) into v_existed;
  insert into public.robot_configs (robot_id, version, promos)
  values (p_robot, 1, p_promos)
  on conflict (robot_id) do update
    set version = robot_configs.version + 1, promos = excluded.promos, updated_at = now()
  returning version into v_version;
  if not v_existed then
    update public.grants set config_version = v_version
    where robot_id = p_robot and config_version = 0;
  end if;
  return v_version;
end;
$$;

revoke execute on function save_config(text, jsonb) from public, anon;
