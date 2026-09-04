-- Install-time naming: the tablet's first-run setup screen sends a device name with
-- registration. A named device joins the fleet immediately (claimed); the dashboard's
-- pending-approval flow remains only for legacy/unnamed registrations.

drop function if exists register_device(text, text);

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
  elsif v_robot.claimed then
    -- Token locked: the device must already hold it. No re-issue without dashboard unclaim.
    return jsonb_build_object('robot_id', v_robot.id, 'claimed', true);
  elsif v_name is not null then
    update public.robots set name = v_name, claimed = true where id = v_robot.id
    returning * into v_robot;
  end if;
  update public.robots set last_seen_at = now() where id = v_robot.id;
  return jsonb_build_object('robot_id', v_robot.id, 'claimed', v_robot.claimed, 'device_token', v_robot.device_token);
end;
$$;
