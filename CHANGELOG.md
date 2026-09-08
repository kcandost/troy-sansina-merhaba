# Changelog

TROY "Şansına Merhaba" tablet app. The version name matches the GitHub release tag; APKs are on the [Releases](https://github.com/kcandost/troy-sansina-merhaba/releases) page. Every release installs over the previous one; settings and counters are preserved.

## v1.3.1 — 2026-09-08

- **"Uygulamadan çık" (Exit app) button** in the settings panel: closes the app and returns the tablet to its home screen (for installing updates or switching apps). Relaunch from the app icon.
- Added this changelog.

## v1.3.0 — 2026-09-08

Copy and layout updates from the agency's revised TROY_ROZYLABS Figma file:

- **Card selection (frame 6):** "Kartlardan birini seç, Troy'dan kazanacağın indirimi öğren."
- **Flip (frame 7):** "Hemen çevir, indirimi gör!"
- **Result (frames 8–11):** new layout — "Troy mağazalarında kullanabileceğin" / amount / "indirim kazandın." / "Hemen QR'ı okut, indirim kodunu al." / QR.
- Invite screen (frame 5) and backgrounds unchanged.

## v1.2.0 — 2026-09-07

- "Online" in the dashboard now means the game is on the tablet screen; the device shows offline the moment it goes to the home screen or the screen turns off.
- Dashboard shows how long each robot has been on screen and today's total screen time; new "Screen time" card (robot × 14 days).
- Tablets report their app version; robots on an outdated version are flagged with a red version chip.
- Telegram alerts (TROY Sansina group) use this version for offline/online and outdated-version notices.

## v1.1.1 — 2026-09-05

- Fixed the misleading "Sunucuya ulaşılamadı" (server unreachable) message after uninstall/reinstall: the tablet now says the device is already registered in the fleet and points to the dashboard's "release" action.
- Dashboard "release" action per device; a released tablet re-enrolls with the same identity and keeps its history.

## v1.1.0 — 2026-09-04

- Devices send a heartbeat every 5 minutes: the dashboard's online/offline state reflects whether the device is actually up (during gameplay too).
- Lifetime play counts no longer reset when the coupon config changes; the dashboard shows totals since enrollment while limit counters keep working per period.

## v1.0.0 — 2026-09-04

- Fleet dashboard release: device naming at setup, per-robot limits plus campaign-wide quotas, remote coupon management.
- Supabase backend: coupon grants, remote config, offline queue.

## v0.3.0 — 2026-09-01

- Community PR #1 (@menesnas): fixed robot address (no field configuration), pause window kept alive on held/dragged touches, explicit empty POST body to avoid ASGI 400 rejections, state-transition logging, richer status probe (e-stop, blocked navigation, charging).
- Store guide PDF updated.

## v0.2.0 — 2026-08-31

- Robot pause-on-touch: any screen touch halts the paired Saha cleaning robot; it resumes 60 s after the last touch, with crash-safe recovery.

## v0.1.0 — 2026-08-31

- Campaign launch build: faithful implementation of the TROY_ROZYLABS Figma designs (frames 5–11), 1920×1200 landscape tablet, Android 8.0+.
- Store operations guide: docs/Troy_Sansina_Merhaba_Kullanim_Kilavuzu.pdf
