# Troy Sansina backend — remote usage tracking, remote config, coupon count limits

Date: 2026-09-03 · Status: approved (Candost, in chat)

## Why

From Mertcan (2026-09-02):
1. We must be able to remotely track how many of which coupon were used on which robot, and control it per brand directives — a dashboard is needed.
2. Coupon settings are ratio-based; adding count limits would be much healthier.

Decisions made with Candost:
- **Full remote config**: dashboard views live counts AND pushes coupon settings (amounts, weights, limits). On-device settings panel stays as fallback.
- **Limits are per robot**, enforced from the robot's local counter so they work fully offline.
- **Backend is Supabase** (Postgres + auto REST + RLS). No server code.
- **A "use" = a grant** (QR screen shown). Real in-store redemption is outside our system.
- **Sync = append-only event log + 60s config poll.** No websockets, no snapshots.

## Architecture

Three pieces:
1. **Supabase project** — schema, RLS, and two SECURITY DEFINER RPCs. Everything under `supabase/migrations/`.
2. **Android app** — a `Sync.kt` layer: enqueue grant events locally, flush when online, poll config on the invite screen; limit-aware draw in `Game.kt`.
3. **Dashboard** — one static HTML page (`dashboard/index.html`) using `supabase-js` from CDN: per-robot × per-coupon used/limit/remaining table + config editor with "apply to all".

## Data model

```sql
robots(id text pk, name text, device_token text unique, last_seen_at timestamptz)
robot_configs(robot_id text pk references robots, version int, promos jsonb, updated_at timestamptz)
  -- promos: [{ "amount": 250, "weight": 40, "limit": 0 }]; limit 0 = unlimited
  -- check: weights sum to 100, 2..6 entries, amounts > 0
grants(id bigint identity pk, robot_id text references robots, amount int,
       config_version int, granted_at timestamptz, client_uuid uuid unique)
usage_view: robot_id, amount, used  -- grants aggregated where config_version = current version
```

## Auth

- RLS enabled on all tables; the anon key alone can read/write **nothing**.
- Robots call two RPCs with their `device_token` as an argument; the RPC validates the token in SQL:
  - `ingest_grants(p_token, p_events jsonb) -> int` — bulk insert, `on conflict (client_uuid) do nothing`, touches `last_seen_at`. Returns rows accepted.
  - `fetch_config(p_token) -> jsonb` — returns `{version, promos}`, touches `last_seen_at`.
- Dashboard signs in with Supabase email auth (one shared team login). Authenticated role gets RLS `select` on everything and `update` on `robot_configs` (via `save_config` RPC that bumps `version`), plus `insert` on `robots` for onboarding.

## App behavior

- **Promo gains a `limit` field** (`Promo(amount, weight, limit)`), serialized as `amount,weight,limit`; parser accepts the old 2-field form (limit=0) so existing installs migrate cleanly.
- **Draw** (`Game.kt`): exclude promos whose local count ≥ limit (limit>0), renormalize remaining weights. If all promos are exhausted, the invite screen shows a "Kampanya sona erdi" state and the game will not start.
- **Grant event**: when the result screen records a promo, also enqueue `{uuid, amount, ts, version}` into a SharedPreferences-backed queue. A flusher posts the whole queue via `ingest_grants` when online (on app start, after each grant, and on the idle screen); drains on success.
- **Config poll**: every 60s while on the invite screen, call `fetch_config`; if `version` is newer, validate, apply, and reset local counters (existing reset-on-config-change behavior). Keep last good config on any error.
- **Settings panel**: new "Bağlantı" fields — Supabase URL, anon key, robot id, device token — plus sync status (last sync time, queued events). On-device promo editing still works; it does not write back to the server (server pushes win at next poll — the dashboard is the source of truth once connected; offline-only installs behave exactly as today).

## Dashboard (v1)

Single page, auto-refresh every 30s:
- Robots table: name, online badge (`last_seen_at` < 5 min), per-coupon **used / limit / remaining**, fleet totals row.
- Config editor: pick robot (or "all"), edit rows of amount/weight%/limit, validation (weights sum 100), Save → `save_config` RPC per target robot.
- No charts, no history UI. The event log makes a timeline possible later without schema changes.

## Failure modes

- Offline robot: plays on, enforces limits locally, flushes on reconnect; dashboard shows staleness via `last_seen_at`.
- Retried flush: deduped by `client_uuid` unique constraint.
- Invalid config: rejected by DB check constraint; app also validates and keeps last good config.
- Token leak: rotate `device_token` per robot in the dashboard/SQL; old token stops working immediately.

## Testing

- App unit tests: queue enqueue/flush/drain, limit-aware draw + renormalization, exhausted-state, config parse (old/new format).
- SQL: RLS denial for anon; RPC happy path + bad token; duplicate `client_uuid`.
- Manual E2E: emulator (Pixel_Tablet_Fresh) against the real Supabase project; verify a grant appears on the dashboard and a pushed config lands on device.

## Out of scope (v1)

Real redemption tracking (appy.to), per-store grouping, history charts, multi-tenant/brand auth, fleet-wide shared quotas.
