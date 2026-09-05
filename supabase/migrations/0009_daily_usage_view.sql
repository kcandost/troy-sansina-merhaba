-- Daily usage for the dashboard's "Günlük kupon" chart.
--
-- Grants bucketed by Istanbul calendar day × robot × amount. The dashboard sums
-- rows client-side for the fleet view and filters by robot_id for the robot view,
-- so one read serves both toggles. Read-only, lifetime (no config-version join),
-- security_invoker so the dashboard's RLS read policy on grants applies.
create view daily_usage_view with (security_invoker = true) as
  select (granted_at at time zone 'Europe/Istanbul')::date as day,
         robot_id, amount, count(*)::int as used
  from grants
  group by 1, robot_id, amount;

-- The dashboard filters this view by day; robot_id alone was indexed before.
create index if not exists grants_granted_at_idx on grants (granted_at);
