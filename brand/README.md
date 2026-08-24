# Brand assets

Sourced from troyestore.com on 2026-08-24. The header wordmark and the Apple
Premium Partner badge are **inline SVG in the site's markup**, not exported files —
these were extracted from the live DOM, cleaned, and re-cut to correct bounds.

## Files

| File | What it is |
|---|---|
| `logo/troy-wordmark.svg` | The TROY wordmark, `fill="currentColor"` — the one to use in code |
| `logo/troy-wordmark-black.svg` | Locked to `#0E1116` (token `ink.900`) |
| `logo/troy-wordmark-white.svg` | Locked to `#FFFFFF` |
| `logo/apple-premium-partner-badge.svg` | Apple's badge as the site serves it: black mark, hairline frame, transparent field |
| `logo/troy-lockup-premium-partner.svg` | Horizontal lockup for light surfaces |
| `logo/troy-lockup-premium-partner-dark.svg` | Dark-surface lockup: white wordmark, badge on a white plate |
| `logo/logo-sheet.html` | Contact sheet — open it to eyeball every variant on both grounds |
| `favicon/troy-favicon.ico` | 32×32, from the site |
| `reference/troyclub.png` | "Troy Dünyası" / TROY CLUB artwork, 2000×2000, greyscale + alpha |

## What was fixed on extraction
- The site ships the wordmark at `viewBox="0 0 61 30"` while the paths run to `y=30.5`
  — the mark is **clipped at the baseline on the live site**. Ours is re-cut to the true
  bounding box, `viewBox="0 0.891 60.17 29.61"`.
- Removed a dangling `clip-path` and a `mask` that referenced defs the page never emitted,
  plus an injected `<script>` element inside the SVG.
- Added `role="img"` + `<title>` so the mark is announced, not skipped, by screen readers.

## Usage rules

**Geometry.** The wordmark's aspect ratio is `60.17 : 29.61` (≈2.03:1). Never stretch,
outline, rotate, add a shadow, or place it inside a coloured shape.

**Clear space.** Minimum on all four sides = the height of the `T` stem (≈ 18% of mark
height). In the app that is `space.4` (12pt) at nav-bar scale.

**Minimum size.** 44pt wide on screen. Below that, use the favicon glyph instead.

**Colour.** `ink.900` on light, `#FFFFFF` on dark. It is never blue, never volt, never
tinted to a campaign colour — the wordmark is the one element in the system that stays
achromatic.

**Apple's badge — read before touching.** The Premium Partner badge is Apple's trademark
artwork and its use is governed by TROY's Apple channel agreement, not by this design
system. Two rules follow from that:
1. **Do not recolour it.** The dark lockup places the unmodified badge on a white plate
   rather than inverting the mark — that is why the dark variant is built the way it is.
2. **Get the official artwork before launch.** These paths were scraped from a web page;
   the shipping app should carry the badge from Apple's own Marketing Resources, at the
   sizes and lockups Apple specifies for the Premium Partner tier.

**Still needed from TROY.** No official brand book, master logo files, app icon, or
Apple co-branding pack has been supplied. Everything here is reconstructed from the
public site and should be treated as a working proxy until the real kit arrives.
