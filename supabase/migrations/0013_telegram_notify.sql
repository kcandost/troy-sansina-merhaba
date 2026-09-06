-- Telegram notifications (2026-09-06). Push-only bot (@TROYSansinaBot) posting into a
-- private Telegram group: a daily report at 09:00 Istanbul and state-change alerts every
-- 5 minutes. Runs entirely inside Postgres (pg_cron + pg_net); no app or dashboard change.
-- Secrets live in Vault and are NOT in this file:
--   select vault.create_secret('<bot token>', 'telegram_bot_token');
--   select vault.create_secret('<group chat id>', 'telegram_chat_id');
-- Tunables live in tg_settings. Alerts only fire on transitions: tg_alert_state remembers
-- what was already sent, so a condition that persists is reported once.

create extension if not exists pg_cron with schema pg_catalog;
create extension if not exists pg_net with schema extensions;

create table tg_settings (
  key text primary key,
  value text not null
);
insert into tg_settings (key, value) values
  ('store_open', '10:00'),      -- offline alerts only inside store hours (Istanbul)
  ('store_close', '22:00'),
  ('offline_minutes', '15'),    -- how long a robot must be offline before we say so
  ('quota_warn_pct', '80'),     -- first fleet-quota warning threshold
  ('dashboard_url', 'https://troy-sansina-dashboard.vercel.app');
alter table tg_settings enable row level security;

create table tg_alert_state (
  key text primary key,
  value text,
  updated_at timestamptz not null default now()
);
alter table tg_alert_state enable row level security;

create table tg_outbox (
  id bigint generated always as identity primary key,
  sent_at timestamptz not null default now(),
  kind text not null,
  text text not null,
  request_id bigint
);
alter table tg_outbox enable row level security;

-- ── helpers ────────────────────────────────────────────────────────────────

create or replace function tg_setting(p_key text)
returns text language sql stable set search_path = ''
as $$ select value from public.tg_settings where key = p_key $$;

create or replace function tg_esc(p text)
returns text language sql immutable set search_path = ''
as $$ select replace(replace(replace(coalesce(p, ''), '&', '&amp;'), '<', '&lt;'), '>', '&gt;') $$;

-- 18750 → "18.750"
create or replace function tg_num(p bigint)
returns text language sql immutable set search_path = ''
as $$ select replace(to_char(p, 'FM999,999,999,999'), ',', '.') $$;

-- 22800 s → "6 sa 20 dk"; 0 → "0 dk"
create or replace function tg_dur(p_seconds bigint)
returns text language sql immutable set search_path = ''
as $$
  select case
    when coalesce(p_seconds, 0) < 3600 then (coalesce(p_seconds, 0) / 60)::text || ' dk'
    when (p_seconds % 3600) / 60 = 0 then (p_seconds / 3600)::text || ' sa'
    else (p_seconds / 3600)::text || ' sa ' || ((p_seconds % 3600) / 60)::text || ' dk'
  end
$$;

-- 2026-09-05 → "5 Eylül Cumartesi"
create or replace function tg_date_tr(p date)
returns text language sql immutable set search_path = ''
as $$
  select extract(day from p)::int::text || ' '
      || (array['Ocak','Şubat','Mart','Nisan','Mayıs','Haziran','Temmuz','Ağustos','Eylül','Ekim','Kasım','Aralık'])[extract(month from p)::int]
      || ' '
      || (array['Pazartesi','Salı','Çarşamba','Perşembe','Cuma','Cumartesi','Pazar'])[extract(isodow from p)::int]
$$;

create or replace function tg_now_ist()
returns timestamp language sql stable set search_path = ''
as $$ select now() at time zone 'Europe/Istanbul' $$;

-- Online as the dashboard defines it: seen within 5 min and the screen is not reported off.
create or replace function tg_is_online(p_last_seen timestamptz, p_screen_state text)
returns boolean language sql immutable set search_path = ''
as $$ select p_last_seen > now() - interval '5 minutes' and p_screen_state is distinct from 'off' $$;

-- Sends one HTML message to the group. Never raises: a Telegram/Vault problem must not
-- break anything that calls it. Returns the pg_net request id (null if not sent).
create or replace function tg_send(p_text text, p_kind text default 'message')
returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_token text;
  v_chat text;
  v_req bigint;
begin
  select decrypted_secret into v_token from vault.decrypted_secrets where name = 'telegram_bot_token';
  select decrypted_secret into v_chat from vault.decrypted_secrets where name = 'telegram_chat_id';
  if v_token is null or v_chat is null then
    raise warning 'tg_send: telegram secrets missing in vault';
    insert into public.tg_outbox (kind, text) values (p_kind, p_text);
    return null;
  end if;
  select net.http_post(
    url := 'https://api.telegram.org/bot' || v_token || '/sendMessage',
    body := jsonb_build_object('chat_id', v_chat, 'text', p_text, 'parse_mode', 'HTML', 'disable_web_page_preview', true),
    timeout_milliseconds := 8000
  ) into v_req;
  insert into public.tg_outbox (kind, text, request_id) values (p_kind, p_text, v_req);
  return v_req;
exception when others then
  raise warning 'tg_send failed: %', sqlerrm;
  return null;
end;
$$;

-- ── daily report ───────────────────────────────────────────────────────────

-- Report for one Istanbul calendar day (default: yesterday).
create or replace function tg_daily_report(p_day date default null)
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  v_day date := coalesce(p_day, (public.tg_now_ist())::date - 1);
  v_count bigint;
  v_sum bigint;
  v_lines text := '';
  v_idle text;
  v_quota text := '';
  v_msg text;
begin
  select count(*), coalesce(sum(amount), 0) into v_count, v_sum
  from public.grants
  where (granted_at at time zone 'Europe/Istanbul')::date = v_day;

  -- Robots that were on screen or gave coupons, busiest first.
  select coalesce(string_agg(
      '• ' || public.tg_esc(r.name) || ' · ' || coalesce(u.cnt, 0)
        || case when s.seconds is not null then ' · ' || public.tg_dur(s.seconds) else '' end,
      E'\n' order by coalesce(u.cnt, 0) desc, coalesce(s.seconds, 0) desc, r.name), '')
  into v_lines
  from public.robots r
  left join (select robot_id, count(*) as cnt, sum(amount) as amt
             from public.grants
             where (granted_at at time zone 'Europe/Istanbul')::date = v_day
             group by robot_id) u on u.robot_id = r.id
  left join (select robot_id, seconds from public.screen_time_view where day = v_day) s on s.robot_id = r.id
  where r.claimed and (coalesce(u.cnt, 0) > 0 or coalesce(s.seconds, 0) > 0);

  -- Robots active in the last week that never appeared that day (long-dead ones stay quiet).
  select string_agg(public.tg_esc(r.name), ', ' order by r.name) into v_idle
  from public.robots r
  where r.claimed
    and r.last_seen_at > (v_day::timestamp at time zone 'Europe/Istanbul') - interval '7 days'
    and not exists (select 1 from public.grants g where g.robot_id = r.id
                    and (g.granted_at at time zone 'Europe/Istanbul')::date = v_day)
    and not exists (select 1 from public.screen_time_view s where s.robot_id = r.id and s.day = v_day and s.seconds > 0);

  select coalesce(string_agg(
      q.amount || ' TL ' || coalesce(g.used, 0) || '/' || q.quota
        || case when coalesce(g.used, 0) >= q.quota then ' ⛔' else '' end,
      ' · ' order by q.amount), '')
  into v_quota
  from public.global_quotas q
  left join public.global_usage_view g on g.amount = q.amount
  where q.quota > 0;

  v_msg := '📊 <b>' || public.tg_date_tr(v_day) || '</b>' || E'\n'
        || case when v_count = 0 then 'Kupon verilmedi.'
                else '<b>' || v_count || ' kupon</b> · ' || public.tg_num(v_sum) || ' TL' end
        || E'\n\n' || case when v_lines = '' then 'Hiçbir robot görünmedi.' else v_lines end
        || case when v_idle is not null then E'\nGörünmeyen: ' || v_idle else '' end
        || case when v_quota <> '' then E'\n\nKota: ' || v_quota else '' end
        || E'\n' || public.tg_setting('dashboard_url');

  perform public.tg_send(v_msg, 'daily');
  return v_msg;
end;
$$;

-- ── alerts ─────────────────────────────────────────────────────────────────

-- Compares the current state with tg_alert_state and sends one message listing every
-- transition found in this pass. Safe to run any time; idempotent when nothing changed.
create or replace function tg_check_alerts()
returns text
language plpgsql security definer set search_path = ''
as $$
declare
  v_now timestamp := public.tg_now_ist();
  v_in_hours boolean := v_now::time >= public.tg_setting('store_open')::time
                    and v_now::time <  public.tg_setting('store_close')::time;
  v_offline_min int := public.tg_setting('offline_minutes')::int;
  v_warn_pct int := public.tg_setting('quota_warn_pct')::int;
  v_lines text[] := '{}';
  v_old text[] := '{}';
  v_max_version int[];
  r record;
begin
  -- 1. Offline / back online. Tracking starts only inside store hours so a tablet that is
  --    switched off overnight and on again at opening never alerts.
  for r in
    select id, name, last_seen_at, screen_state,
           public.tg_is_online(last_seen_at, screen_state) as online,
           (select value::timestamptz from public.tg_alert_state where key = 'offline_since:' || id) as offline_since,
           exists (select 1 from public.tg_alert_state where key = 'offline_sent:' || id) as sent
    from public.robots where claimed and last_seen_at is not null
  loop
    if r.online then
      if r.sent then
        v_lines := v_lines || ('🟢 <b>' || public.tg_esc(r.name) || '</b> çevrimiçi ('
          || public.tg_dur(extract(epoch from now() - r.offline_since)::bigint) || ' sonra)');
      end if;
      delete from public.tg_alert_state where key in ('offline_since:' || r.id, 'offline_sent:' || r.id);
    elsif not v_in_hours then
      if not r.sent then
        delete from public.tg_alert_state where key = 'offline_since:' || r.id;
      end if;
    elsif r.offline_since is null then
      insert into public.tg_alert_state (key, value) values ('offline_since:' || r.id, now()::text);
    elsif not r.sent and r.offline_since <= now() - make_interval(mins => v_offline_min) then
      v_lines := v_lines || ('🔴 <b>' || public.tg_esc(r.name) || '</b> çevrimdışı (son '
        || to_char(r.last_seen_at at time zone 'Europe/Istanbul', 'HH24:MI') || ')');
      insert into public.tg_alert_state (key, value) values ('offline_sent:' || r.id, now()::text);
    end if;
  end loop;

  -- 2. Fleet quotas: warn at quota_warn_pct, again at 100 %. Raising the quota resets.
  for r in
    select q.amount, q.quota, coalesce(g.used, 0) as used,
           coalesce(g.used, 0) * 100 / q.quota as pct,
           (select value::int from public.tg_alert_state where key = 'quota:' || q.amount) as level
    from public.global_quotas q
    left join public.global_usage_view g on g.amount = q.amount
    where q.quota > 0
  loop
    if r.pct >= 100 and coalesce(r.level, 0) < 100 then
      v_lines := v_lines || ('⛔ <b>' || r.amount || ' TL</b> kotası doldu (' || r.used || '/' || r.quota || '), kupon durdu');
      insert into public.tg_alert_state (key, value) values ('quota:' || r.amount, '100')
        on conflict (key) do update set value = '100', updated_at = now();
    elsif r.pct >= v_warn_pct and r.pct < 100 and coalesce(r.level, 0) < v_warn_pct then
      v_lines := v_lines || ('⚠️ <b>' || r.amount || ' TL</b> kota %' || r.pct || ' (' || r.used || '/' || r.quota || ')');
      insert into public.tg_alert_state (key, value) values ('quota:' || r.amount, v_warn_pct::text)
        on conflict (key) do update set value = v_warn_pct::text, updated_at = now();
    elsif r.pct < v_warn_pct and r.level is not null then
      delete from public.tg_alert_state where key = 'quota:' || r.amount;
    end if;
  end loop;
  delete from public.tg_alert_state s where s.key like 'quota:%'
    and not exists (select 1 from public.global_quotas q where q.quota > 0 and 'quota:' || q.amount = s.key);

  -- 3. Per-robot limits reached under the current config version. A version bump resets.
  for r in
    select c.robot_id, rb.name, c.version, (p->>'amount')::int as amount, (p->>'limit')::int as lim,
           (select count(*) from public.grants g where g.robot_id = c.robot_id
              and g.config_version = c.version and g.amount = (p->>'amount')::int) as used
    from public.robot_configs c
    join public.robots rb on rb.id = c.robot_id
    cross join lateral jsonb_array_elements(c.promos) p
    where coalesce((p->>'limit')::int, 0) > 0
  loop
    if r.used >= r.lim and not exists (select 1 from public.tg_alert_state
        where key = 'limit:' || r.robot_id || ':' || r.amount || ':' || r.version) then
      v_lines := v_lines || ('🏁 <b>' || public.tg_esc(r.name) || '</b> · ' || r.amount || ' TL limiti doldu (' || r.used || '/' || r.lim || ')');
      insert into public.tg_alert_state (key, value) values ('limit:' || r.robot_id || ':' || r.amount || ':' || r.version, r.used::text);
    end if;
  end loop;
  delete from public.tg_alert_state s where s.key like 'limit:%'
    and not exists (select 1 from public.robot_configs c
                    where s.key like 'limit:' || c.robot_id || ':%:' || c.version);

  -- 4. Devices waiting for approval.
  for r in select id, name from public.robots where not claimed loop
    if not exists (select 1 from public.tg_alert_state where key = 'pending:' || r.id) then
      v_lines := v_lines || ('🆕 Onay bekliyor: <b>' || public.tg_esc(coalesce(nullif(r.name, ''), r.id)) || '</b>');
      insert into public.tg_alert_state (key, value) values ('pending:' || r.id, now()::text);
    end if;
  end loop;
  delete from public.tg_alert_state s where s.key like 'pending:%'
    and not exists (select 1 from public.robots rb where not rb.claimed and 'pending:' || rb.id = s.key);

  -- 5. Outdated APK, once per robot per version it is stuck on. Versions are dotted ints.
  select max(string_to_array(app_version, '.')::int[]) into v_max_version
  from public.robots where claimed and app_version ~ '^\d+(\.\d+)*$';
  if v_max_version is not null then
    for r in
      select id, name, app_version from public.robots
      where claimed and last_seen_at > now() - interval '1 day'
        and (app_version is null or (app_version ~ '^\d+(\.\d+)*$' and string_to_array(app_version, '.')::int[] < v_max_version))
    loop
      if not exists (select 1 from public.tg_alert_state where key = 'version:' || r.id || ':' || coalesce(r.app_version, 'none')) then
        v_old := v_old || public.tg_esc(r.name);
        insert into public.tg_alert_state (key, value) values ('version:' || r.id || ':' || coalesce(r.app_version, 'none'), now()::text);
      end if;
    end loop;
    if array_length(v_old, 1) > 0 then
      v_lines := v_lines || ('⬆️ Eski sürüm (→ ' || array_to_string(v_max_version, '.') || '): ' || array_to_string(v_old, ', '));
    end if;
  end if;

  if array_length(v_lines, 1) is null then
    return null;
  end if;
  perform public.tg_send(array_to_string(v_lines, E'\n'), 'alert');
  return array_to_string(v_lines, E'\n');
end;
$$;

-- No client may call these; only cron (postgres) and the SQL console.
revoke execute on function tg_send(text, text) from public, anon, authenticated;
revoke execute on function tg_daily_report(date) from public, anon, authenticated;
revoke execute on function tg_check_alerts() from public, anon, authenticated;

-- pg_cron runs in UTC; Istanbul is UTC+3 all year.
select cron.schedule('tg_daily_report', '0 6 * * *', $$select public.tg_daily_report()$$);
select cron.schedule('tg_check_alerts', '*/5 * * * *', $$select public.tg_check_alerts()$$);
