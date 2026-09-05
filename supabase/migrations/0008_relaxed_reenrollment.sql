-- Field-relaxed re-enrollment (2026-09-05): a reinstalled tablet re-enrolls itself
-- without any dashboard step. The token lock on claimed devices (0002/0004) blocked
-- field engineers after an uninstall+reinstall; the priority is retaining usage data,
-- and that is safe to relax because history is keyed by the hardware id — re-enrolling
-- under the same id keeps every grant. A name sent with re-registration renames the
-- robot (field names can be messy; they are tidied later on the panel).
-- Trade-off accepted: anyone with the anon key and a device's hardware id can obtain
-- that device's token. Tokens only gate grant ingestion and config reads.

create or replace function register_device(p_device_id text, p_model text, p_name text default null)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot public.robots;
  v_name text := nullif(trim(coalesce(p_name, '')), '');
begin
  if p_device_id !~ '^[a-z0-9-]{8,64}$' then
    raise exception 'invalid device id';
  end if;
  select * into v_robot from public.robots where id = p_device_id;
  if not found then
    insert into public.robots (id, name, device_token, model, claimed, last_seen_at)
    values (p_device_id, coalesce(v_name, 'Yeni cihaz'),
            encode(extensions.gen_random_bytes(16), 'hex'), left(p_model, 64), v_name is not null, now())
    returning * into v_robot;
  elsif v_name is not null then
    update public.robots set name = v_name, claimed = true where id = v_robot.id
    returning * into v_robot;
  end if;
  update public.robots set last_seen_at = now() where id = v_robot.id;
  return jsonb_build_object('robot_id', v_robot.id, 'claimed', v_robot.claimed, 'device_token', v_robot.device_token);
end;
$$;
