-- Device auto-enrollment: tablets self-register with their hardware ANDROID_ID on first
-- boot and appear on the dashboard as unclaimed devices awaiting a name.
-- Recovery story: an unclaimed device can re-fetch its token (survives app data wipes);
-- claiming locks the token. To recover a claimed device, unclaim it from the dashboard.

alter table robots add column claimed boolean not null default false;
alter table robots add column model text;
update robots set claimed = true;  -- manually-created robots predate enrollment

create or replace function register_device(p_device_id text, p_model text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot public.robots;
begin
  if p_device_id !~ '^[a-z0-9-]{8,64}$' then
    raise exception 'invalid device id';
  end if;
  select * into v_robot from public.robots where id = p_device_id;
  if not found then
    insert into public.robots (id, name, device_token, model, claimed, last_seen_at)
    -- pgcrypto lives in the extensions schema on Supabase; search_path is pinned empty.
    values (p_device_id, 'Yeni cihaz', encode(extensions.gen_random_bytes(16), 'hex'), left(p_model, 64), false, now())
    returning * into v_robot;
  elsif v_robot.claimed then
    -- Token locked: the device must already hold it. No re-issue without dashboard unclaim.
    return jsonb_build_object('robot_id', v_robot.id, 'claimed', true);
  end if;
  update public.robots set last_seen_at = now() where id = v_robot.id;
  return jsonb_build_object('robot_id', v_robot.id, 'claimed', v_robot.claimed, 'device_token', v_robot.device_token);
end;
$$;
