# Brand audit — troyestore.com (captured 2026-08-24)

Observed directly from the live site (computed styles + rendered pages), not from memory.

## What TROY is
Turkey's Apple Premium Partner ("Troy Apple Premium Partner | Troyestore"). Full Apple
catalogue — Mac, iPad, iPhone, Watch, AirPods, TV & Ev, Aksesuarlar — plus a service layer
that is where the actual differentiation lives: **Kampanyalar**, **Eğitim** (student/teacher
pricing), **Mağazalar** (physical stores), **Teknik Servis** (authorised repair appointments),
and **TROY CLUB** (tiered loyalty).

## Observed visual language
| Aspect | Finding |
|---|---|
| Type | `SF Pro Text` everywhere, falling back to `-apple-system` / `system-ui` |
| Primary text | `#121212` at 75% opacity for body, solid `#121212` / `#000` for headings |
| Primary action | `#0071E3` (Apple's blue) on pill buttons, `border-radius: 42px`, 14px/600 |
| Surfaces | White on `#F2F2F2` / `#E6E7E9` bands; card radius `8px`, section radius `16px` |
| Announcement bar | Deep blue band above the header (`#036EDA` family) |
| Accents seen | Lime highlight behind campaign headlines; magenta `#FF048C`; orange `#BF4800` |
| Logo | Wordmark "TROY" in black, locked up beside the Apple "Premium Partner" badge |

## Commerce patterns that must survive into the app
- Price pair: `119.999,00 TL` struck through, `-9%`, `108.999,00 TL` — Turkish number format.
- Installments (`taksit`) are a first-class merchandising unit, bank-specific:
  "Maximum ile Mac modellerinde geçerli **vade farksız 9 taksit** fırsatı".
- Trade-in (`DeğişTokuş`) with a headline top-up: "6.000₺ ek destek".
- Education: %12 on selected MacBook, %5 Mac / %2 iPad for students and teachers.
- Stock language: `Stokta` / `Tükendi`; CTAs `Satın Al` and `Gelince Haber Ver`.
- Variants: storage (`256 GB / 512 GB / 1 TB`) and colour (`Abis`, `Kozmik Turuncu`, `Gümüş`).
- Compare tools per family ("Tüm iPhone modellerini karşılaştırın").

## The design problem
The site is, visually, Apple's own system re-hosted. In an app that reads as a knock-off of
the Apple Store app — and it wastes the only things TROY owns: financing, trade-in, stores,
service and Club. The system below stays quiet and Apple-adjacent on **product** surfaces,
and gives TROY a distinct voice on **the surfaces Apple does not have**.
