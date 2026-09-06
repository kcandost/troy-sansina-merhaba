-- Screen presence (2026-09-06). "Online" on the panel must mean "the game is on the
-- tablet's screen". The app now reports explicit presence events: `on` when it comes
-- on screen, `ping` every 5 minutes while it stays there, `off` the moment it leaves
-- (Home, another app, screen off, power off if the OS gets a chance). A robot is online
-- when its last event is on/ping within 5 minutes; an `off` makes it offline at once.
-- Legacy APKs keep calling device_heartbeat (screen_state stays null → old 5-min rule).
-- Events are kept so on-screen time can be tracked per robot per day.

create table presence_events (
  id bigint generated always as identity primary key,
  robot_id text not null references robots (id) on delete cascade,
  state text not null check (state in ('on', 'ping', 'off')),
  at timestamptz not null default now(),
  client_ms bigint not null  -- device clock at send time; orders a late `off` behind a fresh `on`
);
create index presence_events_robot_at_idx on presence_events (robot_id, at);
alter table presence_events enable row level security;
create policy dashboard_read_presence on presence_events for select to authenticated using (true);

alter table robots add column screen_state text check (screen_state in ('on', 'ping', 'off'));
alter table robots add column screen_ms bigint;  -- client_ms of the event that set screen_state

create or replace function device_presence(p_token text, p_state text, p_client_ms bigint)
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
  -- An `off` fired at stop can arrive after the `on` of an immediate restart: only the
  -- newest event by device clock decides the displayed state. last_seen_at moves on
  -- on/ping only, so an `off` never extends the online window.
  update public.robots
  set screen_state = case when screen_ms is null or p_client_ms >= screen_ms then p_state else screen_state end,
      screen_ms = greatest(coalesce(screen_ms, 0), p_client_ms),
      last_seen_at = case when p_state = 'off' then last_seen_at else now() end
  where id = v_robot;
end;
$$;

-- On-screen seconds per robot per Istanbul day. Each on/ping event counts the time until
-- the next event, capped at 6 minutes (one missed ping plus slack) so a tablet that died
-- without an `off` is not credited forever; an open session counts up to now().
create view screen_time_view with (security_invoker = true) as
  with e as (
    select robot_id, state, at,
           lead(at) over (partition by robot_id order by at, id) as next_at
    from presence_events
  )
  select robot_id,
         (at at time zone 'Europe/Istanbul')::date as day,
         sum(extract(epoch from least(coalesce(next_at, now()), at + interval '6 minutes') - at))::int as seconds
  from e
  where state in ('on', 'ping')
  group by robot_id, (at at time zone 'Europe/Istanbul')::date;
