# Turkish commerce rules

Non-negotiable formatting and copy rules. These are correctness requirements, not style.

## Numbers and money
- Locale `tr-TR`. Thousands `.`, decimals `,`. `108.999,00 ₺` — symbol after, hair space before.
- Always tabular numerals. Never abbreviate prices (`109K ₺` is forbidden).
- Monthly installment rounds to the kuruş: `formatInstallment(108999, 9)` → `9 x 12.111,00 ₺`.
- Percentages: `-%9` (sign then percent then number), the Turkish order.

## Standard strings (source of truth for the app's tr.json)
| Key | Turkish | Note |
|---|---|---|
| `cta.buy` | Satın Al | primary |
| `cta.notify` | Gelince Haber Ver | out of stock |
| `cta.addToCart` | Sepete Ekle | |
| `stock.in` | Stokta | |
| `stock.out` | Tükendi | |
| `stock.store` | Mağazada var | |
| `finance.noInterest` | vade farksız | never translate as "faizsiz" |
| `finance.installments` | {n} taksit | |
| `tradein.name` | DeğişTokuş | capital T, one word |
| `tradein.bonus` | {amount} ek destek | |
| `edu.badge` | Eğitime özel | |
| `club.name` | TROY CLUB | always uppercase |
| `service.book` | Randevu Al | |

## Legal / KVKK
- Campaign cards carry a `caption` link to terms; every price screen states KDV dahil.
- Consent (KVKK + çerez) is a first-run sheet, not a floating banner over content —
  the site's overlay is the one pattern we are explicitly not carrying over.
- Bank campaign names (Maximum, Bonus, World, Axess…) are trademarks: logo lockups only,
  never recoloured to fit the palette.
