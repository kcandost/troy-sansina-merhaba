# Filo Paneli

Single self-contained page (`index.html`) — open it locally in a browser or drop it on any static host (Vercel, Netlify, an S3 bucket). No build step.

First run asks for the Supabase project URL + anon key (stored in the browser's localStorage) and the dashboard email login created during [backend setup](../supabase/README.md).

What it does:
- **Günlük grafik → Kupon**: stacked daily columns (one segment per coupon amount) with a Filo / Robot toggle and a Kampanya (from 3 Sep 2026) / 7 / 14 / 30-day range, figures for today / range total / top coupon, hover tooltips and a table view. Reads `daily_usage_view` (migration 0009); days are Istanbul calendar days.
- **Left rail**: section navigation with scroll-spy and a pending-device badge; collapses to an icon strip via the chevron (remembered per browser) and becomes a top bar on phones.
- **Kullanım**: per-robot × per-coupon grant counts (`used / limit`), online badge (the game is on the tablet's screen: last on/ping presence event < 5 min and no later `off`; migration 0011), current session length + today's on-screen time from `screen_time_view`, an APK column with a version chip (red ↑ when behind the newest version in the fleet or silent = legacy APK; migration 0012) plus a one-line fleet summary above the table, fleet totals.
- **Ekran süresi** is the second tab of the same card: on-screen hours per day (fleet sum or one robot), figures, per-robot tooltip breakdown, and a days × robots table view. Same range and scope controls. Auto-refreshes every 30 s.
- **Kampanya kotası**: fleet-wide cap per coupon with a live fill bar and % — a full coupon shows "durduruldu" and pauses on every robot within 60 s.
- **Kupon ayarları**: edit amounts / weights / limits, push to one robot or all (`save_config` bumps the version; tablets pick it up within 60 s and reset their counters).
- **Onay bekleyen cihazlar**: tablets self-enroll on first boot and appear here; give one a store name and approve to add it to the fleet.
