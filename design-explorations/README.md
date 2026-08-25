# Şansına Merhaba — five directions

Design exploration for the in-store tablet game in TROY's requirement document.

## The mechanic (identical in all five)

1. Davet
2. Şansına merhaba
3. Cards are dealt one at a time, product faces up: iPhone, iPad, MacBook, Apple Watch, AirPods
4. The cards turn over: 250 · 500 · 750 · 1.000 · 1.500 TL
5. A real shuffle: fan, split, cross, collapse, cut, re-deal
6. One card is picked and opens, with a confetti burst
7. Prize screen, then QR

The colour ladder carries the value: aluminium, bronze, silver, gold, and TROY navy with a
gold frame at the top. The card's material gets richer as the value climbs, so the ranking
reads without labels. Every artboard has a `winner` tweak that changes which card wins.

## The five worlds

| File | Direction | Identity |
|---|---|---|
| `Main.dc.html` | A · Sessiz Vitrin, HIG deference, near-white, one blue | troyestore.com |
| `GlassDepth.dc.html` | B · Cam ve Derinlik, glass material and 3D depth | deck (+artısı var) |
| `Stage.dc.html` | C · Sahne, keynote stage, single spot, volt accent | troyestore.com |
| `FinishTray.dc.html` | D · Vitrin Tepsisi, warm tray, brushed-metal cards | troyestore.com |
| `StoreLight.dc.html` | E · Mağaza Işığı, signage scale, closest to the deck | deck (+artısı var) |

Each artboard plays the whole flow as a store attract loop and returns to the invite.
A tap starts it early; a tap during the flow returns to the invite.

## Repo notes

`deck_engine`-generated CSS carries the choreography, so the shuffle, the ladder and the
card anatomy stay identical across the five and only the world around them changes.
`canvas.json` lays them out with one note per direction plus the mechanic note above the row.
`sansina-merhaba-yonleri.html` is the built canvas; regenerate it, never hand-edit it.

Artboards are 1194×834, iPad Pro 11" landscape.

**Placeholders:** the QR artwork is a drawn module grid, not a scannable code, and the product
faces are flat geometric stand-ins for TROY's own product renders.
