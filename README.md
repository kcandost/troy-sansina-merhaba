# TROY — custom app

Design and build repo for the TROY (Apple Premium Partner, Türkiye) mobile app.

**Status:** design system v0.1. No application code yet.

```
docs/
  01-brand-audit.md        what troyestore.com actually is, measured from the live site
  02-design-principles.md  five testable rules the system enforces
  03-components.md         component inventory, specified in tokens
  04-turkish-commerce.md   money formatting, TR strings, KVKK rules
design-system/
  tokens/color.json  type.json  layout.json  motion.json   source of truth
  tokens/tokens.css                                        web / preview build
  tokens/tokens.ts                                         React Native / TS build
  preview/index.html                                       visual specimen page
```

## Rules for consumers
1. Components read **semantic** tokens only. Reaching into primitives is a review block.
2. Light and dark are both first-class. Never declare a colour outside the token layer.
3. Volt (`#D6FF4B`) marks exactly one live commercial offer per screen. Nothing else.
4. Every price is accompanied by its installment line.

Token JSON is the source of truth; `tokens.css` and `tokens.ts` are hand-kept in sync
until a build step exists (Style Dictionary is the intended next step).
