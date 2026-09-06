-- App version + current screen session (2026-09-06).
-- device_presence now carries the APK's versionName so the panel can flag robots that
-- still need an update; and robots.screen_on_since tracks the start of the current
-- on-screen session ("2 sa 10 dk'dır ekranda"). The 3-argument overload is dropped:
-- no field APK ever called it (it shipped only to the emulator earlier today).

alter table robots add column app_version text;
alter table robots add column screen_on_since timestamptz;

drop function if exists device_presence(text, text, bigint);

create or replace function device_presence(p_token text, p_state text, p_client_ms bigint, p_app_version text default null)
returns void
language plpgsql security definer set search_path = ''
as $$
declare
  v_robot text;
begin
  select id into v_robot from public.robots where device_token = p_token;
  if v_robot is null then
    raise exception 'invalid token';
  end if;
  if p_state not in ('on', 'ping', 'off') then
    raise exception 'invalid state';
  end if;
  insert into public.presence_events (robot_id, state, client_ms) values (v_robot, p_state, p_client_ms);
  -- A late `off` (fired at stop) can arrive after the `on` of an immediate restart: only
  -- the newest event by device clock decides the displayed state and the session start.
  -- last_seen_at moves on on/ping only, so an `off` never extends the online window.
  update public.robots
  set screen_state = case when screen_ms is null or p_client_ms >= screen_ms then p_state else screen_state end,
      screen_on_since = case
        when screen_ms is not null and p_client_ms < screen_ms then screen_on_since
        when p_state = 'off' then null
        when p_state = 'on' or screen_on_since is null then now()
        else screen_on_since end,
      screen_ms = greatest(coalesce(screen_ms, 0), p_client_ms),
      last_seen_at = case when p_state = 'off' then last_seen_at else now() end,
      app_version = coalesce(p_app_version, app_version)
  where id = v_robot;
end;
$$;
