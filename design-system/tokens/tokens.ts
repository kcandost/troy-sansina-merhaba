/**
 * TROY Design System — typed tokens for React Native / TypeScript clients.
 * Components must only read from `semantic`, `type`, `space`, `radius`, `motion`.
 * Reaching into `primitive` from a component is a review-blocking mistake.
 */

export const primitive = {
  ink: { 900:'#0E1116', 800:'#22272F', 700:'#3A414D', 600:'#565E6C', 500:'#767E8C',
         400:'#A3AAB6', 300:'#CDD2DA', 200:'#E4E7EC', 100:'#F1F3F6', 50:'#F7F8FA', 0:'#FFFFFF' },
  navy: { 900:'#08182D', 700:'#0F2340', 500:'#1B3A63', 300:'#3C5F8F', 100:'#DCE5F1' },
  blue: { 700:'#0053AC', 600:'#0062C4', 500:'#0071E3', 300:'#5AA9F0', 100:'#E3F0FD' },
  volt: { 500:'#D6FF4B', 300:'#E8FF9B', 100:'#F4FFD6' },
  rose: { 600:'#C31350', 500:'#E5175A', 100:'#FDE4EC' },
  green:{ 600:'#0E6E4C', 500:'#12855C', 100:'#DFF3EA' },
  amber:{ 600:'#8F4B00', 500:'#B25E00', 100:'#FBEBD8' },
  red:  { 600:'#9E1F14', 500:'#C42B1C', 100:'#FBE3E0' },
  tier: { bronze:'#A9714B', silver:'#9AA3AE', gold:'#C9A227', premium:'#0F2340' },
  night:{ bg:'#0B0D11', surface:'#14181F', raised:'#1C222B', line:'#2A313C' },
} as const;

const p = primitive;

export const semantic = {
  light: {
    bgCanvas: p.ink[50], bgSurface: p.ink[0], bgRaised: p.ink[0], bgSunken: p.ink[100],
    bgInverse: p.navy[700], bgBrand: p.blue[500], bgBrandPressed: p.blue[600], bgHighlight: p.volt[500],
    textPrimary: p.ink[900], textSecondary: p.ink[600], textTertiary: p.ink[500],
    textOnBrand: p.ink[0], textOnHighlight: p.ink[900], textLink: p.blue[500],
    textPrice: p.ink[900], textPriceWas: p.ink[500],
    borderSubtle: p.ink[200], borderStrong: p.ink[300], borderFocus: p.blue[500],
    statusSuccess: p.green[500], statusSuccessBg: p.green[100],
    statusWarning: p.amber[500], statusWarningBg: p.amber[100],
    statusDanger:  p.red[500],   statusDangerBg:  p.red[100],
    statusDeal:    p.rose[500],  statusDealBg:    p.rose[100],
    financeAccent: p.navy[700],  financeBg:       p.navy[100],
  },
  dark: {
    bgCanvas: p.night.bg, bgSurface: p.night.surface, bgRaised: p.night.raised, bgSunken: p.night.bg,
    bgInverse: p.ink[0], bgBrand: p.blue[500], bgBrandPressed: p.blue[300], bgHighlight: p.volt[500],
    textPrimary: p.ink[0], textSecondary: p.ink[300], textTertiary: p.ink[400],
    textOnBrand: p.ink[0], textOnHighlight: p.ink[900], textLink: p.blue[300],
    textPrice: p.ink[0], textPriceWas: p.ink[400],
    borderSubtle: p.night.line, borderStrong: p.ink[700], borderFocus: p.blue[300],
    statusSuccess:'#3ECF97', statusSuccessBg:'#0E2A22',
    statusWarning:'#E9A13B', statusWarningBg:'#2C1E0B',
    statusDanger: '#F2685A', statusDangerBg: '#2E1512',
    statusDeal:   '#FF5C8A', statusDealBg:   '#2E1220',
    financeAccent:'#8FB4E8', financeBg:      '#111C2C',
  },
} as const;

export type ColorScheme = keyof typeof semantic;
export type SemanticColor = keyof typeof semantic.light;

export const space = { 0:0, 1:2, 2:4, 3:8, 4:12, 5:16, 6:20, 7:24, 8:32, 9:40, 10:48, 11:64 } as const;
export const radius = { xs:4, sm:8, md:12, lg:16, xl:22, xxl:28, pill:999 } as const;

export const type = {
  display:  { fontSize:34, lineHeight:40, fontWeight:'600', letterSpacing:-0.6 },
  title1:   { fontSize:28, lineHeight:34, fontWeight:'600', letterSpacing:-0.4 },
  title2:   { fontSize:22, lineHeight:28, fontWeight:'600', letterSpacing:-0.3 },
  title3:   { fontSize:20, lineHeight:26, fontWeight:'600', letterSpacing:-0.2 },
  headline: { fontSize:17, lineHeight:22, fontWeight:'600', letterSpacing:-0.1 },
  body:     { fontSize:17, lineHeight:24, fontWeight:'400', letterSpacing:0 },
  callout:  { fontSize:16, lineHeight:22, fontWeight:'400', letterSpacing:0 },
  subhead:  { fontSize:15, lineHeight:20, fontWeight:'400', letterSpacing:0 },
  footnote: { fontSize:13, lineHeight:18, fontWeight:'400', letterSpacing:0 },
  caption:  { fontSize:12, lineHeight:16, fontWeight:'400', letterSpacing:0.1 },
  micro:    { fontSize:11, lineHeight:14, fontWeight:'600', letterSpacing:0.6, textTransform:'uppercase' },
  priceLg:  { fontSize:24, lineHeight:28, fontWeight:'600', letterSpacing:-0.3, fontVariant:['tabular-nums'] },
  priceMd:  { fontSize:17, lineHeight:22, fontWeight:'600', letterSpacing:-0.1, fontVariant:['tabular-nums'] },
  priceSm:  { fontSize:14, lineHeight:18, fontWeight:'600', letterSpacing:0,    fontVariant:['tabular-nums'] },
} as const;

export const motion = {
  duration: { instant:80, fast:160, base:240, slow:360, sheet:460 },
  easing: { standard:[0.32,0.72,0,1], decelerate:[0.16,1,0.3,1], accelerate:[0.4,0,1,1] },
  spring: { stiffness:320, damping:32, mass:1 },
} as const;

export const touch = { minTarget:44, tabBarHeight:49, navBarHeight:44, primaryButtonHeight:50, chipHeight:34 } as const;

/** Turkish money formatting: 108.999,00 ₺ — always tabular, always suffixed. */
export const formatTRY = (value: number, withDecimals = true): string =>
  new Intl.NumberFormat('tr-TR', {
    style: 'currency', currency: 'TRY',
    minimumFractionDigits: withDecimals ? 2 : 0,
    maximumFractionDigits: withDecimals ? 2 : 0,
  }).format(value);

/** "9 x 12.111,00 ₺" — monthly figure is rounded to kurus, never truncated. */
export const formatInstallment = (total: number, months: number): string =>
  `${months} x ${formatTRY(Math.round((total / months) * 100) / 100)}`;
