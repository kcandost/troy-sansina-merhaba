-- Liveness heartbeat + lifetime usage.
--
-- device_heartbeat: tablets ping every 5 minutes regardless of which screen is
-- showing, so the dashboard's 5-minute online window reflects power state.
-- Before this, last_seen_at only moved inside ingest_grants/fetch_config, which
-- the app calls only while idling on the invite screen.
create or replace function device_heartbeat(p_token text)
returns void
language plpgsql security definer set search_path = ''
as $$
begin
  update public.robots set last_seen_at = now() where device_token = p_token;
  if not found then
    raise exception 'invalid token';
  end if;
end;
$$;

-- Lifetime usage: every grant since the robot registered, across all config
-- versions. usage_view stays as the per-config counter that limits enforce and
-- that resets on save_config; this one never resets — grants are never deleted.
create view lifetime_usage_view with (security_invoker = true) as
  select robot_id, amount, count(*)::int as used
  from grants
  group by robot_id, amount;
