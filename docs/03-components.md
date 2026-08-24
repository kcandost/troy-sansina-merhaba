# Component inventory (v0.1)

Every component below is specified by tokens only. Sizes in pt/dp.

## Foundations
| Component | Spec |
|---|---|
| **Button / primary** | height `touch.primaryButtonHeight` (50), radius `pill`, bg `bg.brand`, label `headline` on `text.onBrand`, pressed → `bg.brand.pressed` + 0.97 scale (`dur.fast`) |
| **Button / secondary** | same metrics, transparent bg, `border.strong` hairline, label `text.primary` |
| **Button / offer** | bg `bg.highlight` (volt), label `text.onHighlight`. Reserved for campaign CTAs — max one per screen |
| **Chip / filter** | height 34, radius `sm`, `subhead`; selected = `bg.inverse` + `text.onBrand` |
| **Chip / variant** | storage: text chip, radius `sm`, 2px `border.focus` ring when selected. Colour: 28pt swatch, ring offset 3pt, Turkish colour name below in `caption` |
| **Field / search** | height 44, radius `pill`, bg `bg.sunken`, placeholder `Ürün, model veya aksesuar ara` |
| **Pill / stock** | `micro`; Stokta → `status.success` on `status.successBg`; Son N adet → warning; Tükendi → `text.tertiary` on `bg.sunken`; Mağazada var → `finance.accent` on `finance.bg` |
| **Sheet** | radius `2xl` top corners, `bg.raised`, grabber 36×4 `border.strong`, enters `dur.sheet` / `ease.standard` |
| **Skeleton** | `bg.sunken` blocks, shimmer disabled under Reduce Motion |
| **Toast** | `bg.inverse`, `text.onBrand`, radius `md`, 3s, above tab bar |

## Commerce
**PriceBlock** — the system's most important component. Three sizes (`sm` grid card,
`md` cart row, `lg` PDP). Fixed vertical order:
1. `priceWas` struck, `text.price-was`, only when a discount exists
2. current price, `priceLg/Md/Sm`, `text.price`
3. discount badge `-%9`, `status.deal` on `status.dealBg`, radius `xs`
4. installment line: `9 x 12.111,00 ₺` + `vade farksız`, `footnote`, `finance.accent`
5. optional trade-in line: `DeğişTokuş ile 6.000 ₺ ek destek`, `footnote`

**ProductCard** — 2-up grid. Image square on `bg.surface`, radius `lg`, stock pill top-left,
name `subhead` max 2 lines, PriceBlock `sm`. Out-of-stock: image at 55% opacity, CTA becomes
`Gelince Haber Ver`.

**CampaignBanner** — navy ground, volt keyline, `title3` headline, bank/partner lockup,
expiry `caption` (`31 Ağustos'a kadar`), legal footnote link. Never auto-rotates faster than 6s;
carousel pauses on interaction and under Reduce Motion becomes a static stack.

**InstallmentSheet** — bank rows × month columns, tabular numerals, `vade farksız` rows
marked with `finance.accent`; the shopper's saved bank sorts first.

**TradeInEstimator (DeğişTokuş)** — 4 steps (cihaz → durum → tahmini değer → mağazada onay).
Estimated value shown as a range in `priceLg`; the "ek destek" top-up is the only volt element.

**EducationBadge** — `micro` on `finance.bg`, shown on eligible products; tapping opens the
verification flow rather than the discount detail.

## Service surfaces
| Component | Spec |
|---|---|
| **StoreRow** | store name `headline`, distance `footnote`, open/closed as status pill, actions: Yol Tarifi / Ara / Randevu |
| **AppointmentCard (Teknik Servis)** | navy ground, date block left, device + issue right, status pill (Randevu alındı / Serviste / Hazır) |
| **ClubTierCard** | tier gradient from `primitive.tier.*` on navy, points `priceLg` tabular, next-tier progress bar 6pt radius `pill` |
| **OrderTracker** | 4-step stepper, active step `bg.brand`, completed `status.success`, TR labels: Hazırlanıyor / Kargoda / Dağıtımda / Teslim edildi |

## Navigation
Tab bar, 5 items: **Alışveriş · Kampanyalar · Mağazalar · Troy Club · Hesabım**.
Icons 24pt outline / filled-when-active, label `caption`. Campaign badge on Kampanyalar uses
`status.deal`, never volt (volt stays on-canvas, not in chrome).
