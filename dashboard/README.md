# Filo Paneli

Single self-contained page (`index.html`) — open it locally in a browser or drop it on any static host (Vercel, Netlify, an S3 bucket). No build step.

First run asks for the Supabase project URL + anon key (stored in the browser's localStorage) and the dashboard email login created during [backend setup](../supabase/README.md).

What it does:
- **Kullanım**: per-robot × per-coupon grant counts (`used / limit`), online badge (last contact < 5 min), fleet totals. Auto-refreshes every 30 s.
- **Kupon ayarları**: edit amounts / weights / limits, push to one robot or all (`save_config` bumps the version; tablets pick it up within 60 s and reset their counters).
- **Yeni robot**: registers a robot and shows its device token once — enter it in the tablet's hidden settings (Bağlantı).
