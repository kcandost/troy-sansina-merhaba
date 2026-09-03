# Troy Sansina backend — Supabase setup

One-time setup (~10 minutes, all in the Supabase web console):

1. **Create a project** at [supabase.com](https://supabase.com) (free tier is plenty). Note the **Project URL** and the **anon (public) API key** from Project Settings → API.
2. **Run the migration**: open SQL Editor, paste `migrations/0001_init.sql`, run it.
3. **Create the dashboard login**: Authentication → Users → Add user (email + password). This is the shared team login for the dashboard. Disable public signups under Authentication → Providers → Email.
4. **Register each robot** (SQL Editor):

   ```sql
   insert into robots (id, name, device_token)
   values ('robot-1', 'Kadıköy', encode(gen_random_bytes(16), 'hex'))
   returning device_token;
   ```

   Save the returned token — it goes into that robot's tablet.
5. **Configure each tablet**: hidden settings (long-press gear → PIN) → **Bağlantı** → enter the Project URL, anon key, robot id, and that robot's device token.
6. **Set the first coupon config** from the dashboard (`dashboard/index.html`) — or via SQL:

   ```sql
   select save_config('robot-1',
     '[{"amount":250,"weight":40,"limit":0},{"amount":500,"weight":30,"limit":0},
       {"amount":750,"weight":20,"limit":0},{"amount":1000,"weight":10,"limit":50}]'::jsonb);
   ```

## How it works

- Tablets append one row to `grants` per QR shown (queued locally while offline, deduped by `client_uuid`), and poll `fetch_config` every 60s on the invite screen. A higher `version` resets local counters and applies the new promos.
- `limit` is **per robot**; `0` = unlimited. Tablets enforce limits from their local counters, so they work offline. When every promo is exhausted the tablet shows "Kampanya sona erdi" until a new config version arrives.
- The anon key alone grants nothing: robots authenticate with their `device_token` inside the RPCs; the dashboard uses the email login.
- To rotate a leaked token: `update robots set device_token = encode(gen_random_bytes(16),'hex') where id = 'robot-1' returning device_token;` then update the tablet.

## Ops queries

```sql
select * from usage_view order by robot_id, amount;          -- used per robot × coupon
select id, name, last_seen_at from robots;                   -- fleet liveness
select robot_id, version, promos from robot_configs;         -- what each robot runs
```
