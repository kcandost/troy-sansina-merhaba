# Design principles

Five rules. Each one is testable in review — if a screen breaks one, it doesn't ship.

### 1. The product is Apple's. The service is TROY's.
On product surfaces (PDP hero, gallery, specs) TROY recedes: white ground, generous space,
no brand colour on the product itself. On service surfaces (Kampanyalar, DeğişTokuş, Club,
Mağazalar, Teknik Servis) TROY leads: navy grounds, volt highlight, denser information.
**Test:** cover the logo. A product screen should look like a well-made Apple retail screen;
a service screen should be unmistakably TROY.

### 2. The real price is the monthly price.
In this market the decision is made on `taksit`, not on the sticker. Every place a price
appears, the installment line appears with it, at the same visual weight class as the
secondary price — never hidden behind a tap. Discount, trade-in and education savings stack
in one place, in one order, always: sticker → indirim → taksit → DeğişTokuş → Eğitim.
**Test:** can a shopper answer "what do I pay per month" without tapping?

### 3. Volt is a scalpel.
`#D6FF4B` marks exactly one thing per screen: the live commercial offer. Two volt elements
in one viewport means neither is an offer. Navy carries structure, blue carries actions,
volt carries money-off. Nothing else is ever volt.

### 4. Turkish first, at any type size.
Copy is written in Turkish and laid out for Turkish: longer strings, diacritics in every
weight, `108.999,00 ₺`, tabular numerals. Layouts reflow to 200% Dynamic Type — prices and
installment lines are the last things allowed to truncate, which means never.

### 5. Stock and store are state, not fine print.
`Stokta` / `Tükendi` / `Mağazada var` change the shape of the card, not just its label —
pill colour, CTA wording, and card affordance move together. A shopper scanning a grid must
read availability before reading the name.
