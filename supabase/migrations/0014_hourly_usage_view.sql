-- Hour-of-day usage for the dashboard's "Saatlik" heat-map (day × hour tiles).
--
-- Grants bucketed by Istanbul calendar day × hour × robot. The dashboard sums rows
-- client-side for the fleet view and filters by robot_id for the robot view, like
-- daily_usage_view. Read-only, lifetime, security_invoker so the dashboard's RLS
-- read policy on grants applies. Served by grants_granted_at_idx (0009).
create view hourly_usage_view with (security_invoker = true) as
  select (granted_at at time zone 'Europe/Istanbul')::date as day,
         extract(hour from granted_at at time zone 'Europe/Istanbul')::int as hour,
         robot_id, count(*)::int as used
  from grants
  group by 1, 2, robot_id;
