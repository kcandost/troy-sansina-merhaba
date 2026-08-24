# Şansına Merhaba — five directions

Design exploration for the in-store tablet game in TROY's requirement document
(6-screen storyboard: Davet → Başlangıç → Shuffle → Sonuç → Vurgulama → QR).

Each `.dc.html` is one direction, and each one plays the **whole flow**: it opens on
the invite, auto-starts after ~2.6 s, shuffles, reveals the prize, shows the QR, and
loops back — a store attract loop. A tap starts it immediately; a tap during the flow
returns to the invite.

| File | Direction | Identity |
|---|---|---|
| `Main.dc.html` | A · Sessiz Vitrin — HIG deference, near-white, one blue | troyestore.com |
| `GlassDepth.dc.html` | B · Cam ve Derinlik — glass materials, 3D depth | deck (+artısı var) |
| `Stage.dc.html` | C · Sahne — keynote stage, slot reel, volt accent | troyestore.com |
| `FinishTray.dc.html` | D · Vitrin Tepsisi — Apple product finishes, Wallet stack | troyestore.com |
| `StoreLight.dc.html` | E · Mağaza Işığı — signage scale, closest to the deck | deck (+artısı var) |

`canvas.json` lays them out side by side with a note per direction.
`sansina-merhaba-yonleri.html` is the built canvas (regenerate it from these files;
never hand-edit it).

Artboards are 1194×834 — iPad Pro 11" landscape, the orientation in the storyboard.
Type is the platform UI face (SF Pro / Roboto) throughout, which is the honest choice
for an Apple-idiom brief.

**Placeholder:** the QR artwork is a drawn module grid, not a scannable code.
