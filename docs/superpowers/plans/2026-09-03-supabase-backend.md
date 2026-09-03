# Troy Sansina Supabase Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remote per-robot coupon usage tracking + remote config push + per-coupon count limits, backed by Supabase, with a single-page dashboard.

**Architecture:** Append-only `grants` event log + 60s config poll from the tablet; limits enforced from the tablet's local counters (offline-safe). Supabase is the entire backend (schema + 2 token-checked RPCs, no server code). Dashboard is one static HTML page using supabase-js.

**Tech Stack:** Kotlin/Compose (existing app, no new runtime deps — `HttpURLConnection` + `org.json`), Supabase Postgres migrations (SQL), static HTML + supabase-js v2 (CDN).

**Spec:** `docs/superpowers/specs/2026-09-03-backend-architecture-design.md`

## Global Constraints

- No new Android runtime dependencies; JVM target 17, minSdk 26.
- Old persisted config format `amount,weight` must keep parsing (limit defaults to 0 = unlimited).
- Weights must sum to 100; 2–6 promos; amounts > 0 (existing `isValid`, extended with `limit >= 0`).
- Build with cached Gradle 8.13 + Android Studio JBR (no gradlew): `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; ~/.gradle/wrapper/dists/gradle-8.13-bin/5xuhj0ry160q40clulazy9h7d/gradle-8.13/bin/gradle -q -p android test assembleDebug`
- Turkish UI copy; match existing code style (compact, comment-light).
- Unconfigured sync (empty URL) must leave the app behaving exactly as today.

---

### Task 1: `Promo.limit` + `PromoConfig.active()` (pure model, TDD)

**Files:**
- Modify: `android/app/src/main/java/com/troy/sansina/Game.kt:40-75`
- Modify: `android/app/build.gradle.kts` (add `testImplementation("junit:junit:4.13.2")`)
- Test: `android/app/src/test/java/com/troy/sansina/PromoConfigTest.kt` (create)

**Interfaces:**
- Produces: `Promo(amount: Int, weight: Int, limit: Int = 0)`; `PromoConfig.active(counts: Map<Int, Int>): List<Promo>` (promos not exhausted); `serialize()` → `"250,40,0;500,30,10"`; `parse` accepts 2- and 3-field entries.

- [ ] **Step 1: Add junit test dep, write failing tests**

```kotlin
package com.troy.sansina
import org.junit.Assert.*
import org.junit.Test

class PromoConfigTest {
    @Test fun parseOldFormatDefaultsLimitZero() {
        val c = PromoConfig.parse("250,40;500,60")
        assertEquals(listOf(Promo(250, 40, 0), Promo(500, 60, 0)), c.promos)
    }
    @Test fun serializeRoundTripsLimit() {
        val c = PromoConfig(listOf(Promo(250, 40, 5), Promo(500, 60, 0)))
        assertEquals(c, PromoConfig.parse(c.serialize()))
        assertEquals("250,40,5;500,60,0", c.serialize())
    }
    @Test fun activeExcludesExhausted() {
        val c = PromoConfig(listOf(Promo(250, 40, 2), Promo(500, 60, 0)))
        assertEquals(listOf(Promo(500, 60, 0)), c.active(mapOf(250 to 2)))
        assertEquals(c.promos, c.active(mapOf(250 to 1)))
    }
    @Test fun negativeLimitInvalid() {
        assertFalse(PromoConfig(listOf(Promo(250, 40, -1), Promo(500, 60, 0))).isValid)
    }
}
```

- [ ] **Step 2: Run to verify fail** — `gradle -q -p android test` → compile error (no `limit` param).
- [ ] **Step 3: Implement** — in `Game.kt`: `data class Promo(val amount: Int, val weight: Int, val limit: Int = 0)`; `serialize()` emits 3 fields; `parse` splits each entry on `,` and reads `getOrNull(2)?.toInt() ?: 0`; `isValid` adds `it.limit >= 0`; add `fun active(counts: Map<Int, Int>) = promos.filter { it.limit == 0 || (counts[it.amount] ?: 0) < it.limit }`.
- [ ] **Step 4: Run tests** → PASS.
- [ ] **Step 5: Commit** — `feat: coupon count limits in Promo model`

### Task 2: Limit-aware draw + campaign-ended state

**Files:**
- Modify: `android/app/src/main/java/com/troy/sansina/Game.kt:183-239` (deal/drawPromo filter by `active()`)
- Modify: `android/app/src/main/java/com/troy/sansina/MainActivity.kt:58-125` (exhausted check gates start)
- Modify: `android/app/src/main/java/com/troy/sansina/Screens.kt` (invite screen "Kampanya sona erdi" variant)
- Test: `android/app/src/test/java/com/troy/sansina/PromoConfigTest.kt` (draw helper test)

**Interfaces:**
- Consumes: `PromoConfig.active(counts)` from Task 1.
- Produces: `GameState(ctx, config, counts: () -> Map<Int, Int>, onWin)`; `PromoConfig.draw(counts, rnd): Promo?` (null when all exhausted).

- [ ] **Step 1: Failing test** — add to `PromoConfigTest`:

```kotlin
    @Test fun drawRespectsLimits() {
        val c = PromoConfig(listOf(Promo(250, 100, 1), Promo(500, 0, 0)))
        assertEquals(250, c.draw(emptyMap(), kotlin.random.Random(1))!!.amount)
        assertEquals(500, c.draw(mapOf(250 to 1), kotlin.random.Random(1))!!.amount) // zero-weight fallback still drawable
        assertNull(PromoConfig(listOf(Promo(250, 100, 1), Promo(500, 0, 1))).draw(mapOf(250 to 1, 500 to 1), kotlin.random.Random(1)))
    }
```

- [ ] **Step 2: Verify fail.**
- [ ] **Step 3: Implement** — `fun draw(counts: Map<Int, Int>, rnd: kotlin.random.Random = kotlin.random.Random): Promo?` over `active(counts)` with renormalized weights (`var r = rnd.nextInt(pool.sumOf{it.weight}.coerceAtLeast(1)); pool.firstOrNull { r -= it.weight; r < 0 } ?: pool.last()`), null on empty pool. `GameState.drawPromo()` delegates; `deal()` builds its ladder from `config.active(counts())` (fall back to all promos if pool < 2 so the board still renders). `MainActivity`: `val exhausted = config.active(stats.counts).isEmpty()`; tap-to-start ignored when exhausted; invite screen headline swaps to "Kampanya sona erdi" / subtitle "Yeni avantajlar için bizi takip et." when exhausted.
- [ ] **Step 4: Tests pass + `assembleDebug` compiles.**
- [ ] **Step 5: Commit** — `feat: limit-aware prize draw + campaign-ended invite state`

### Task 3: Supabase schema + RPCs

**Files:**
- Create: `supabase/migrations/0001_init.sql`
- Create: `supabase/README.md` (setup: create project, run SQL in SQL editor, create dashboard user, insert robots)

**Interfaces:**
- Produces: RPCs `ingest_grants(p_token text, p_events jsonb) returns int`, `fetch_config(p_token text) returns jsonb` (`{"version":n,"promos":[{"amount":..,"weight":..,"limit":..}]}`), `save_config(p_robot text, p_promos jsonb) returns int` (auth'd, bumps version); view `usage_view(robot_id, amount, used)`.

- [ ] **Step 1: Write `0001_init.sql`** — tables per spec; RLS enabled, no anon policies; `authenticated` gets select on `robots`/`robot_configs`/`grants`/`usage_view`, insert/update on `robots`; RPCs are `security definer set search_path = public`, token functions return early on no-match; `ingest_grants` inserts `on conflict (client_uuid) do nothing`, updates `last_seen_at`, returns accepted count; config check constraint validates weights sum 100, 2–6 entries via a `plpgsql` `check_promos(jsonb)` immutable function; `save_config` upserts with `version = coalesce(old.version,0)+1`.
- [ ] **Step 2: Validate SQL** — `psql` not assumed; review by eye + (if available) `supabase db lint`. Runtime validation happens in Task 7 E2E.
- [ ] **Step 3: Write README** — copy-paste setup steps incl. seeding: `insert into robots values ('robot-1','Kadıköy', encode(gen_random_bytes(16),'hex'), null);`
- [ ] **Step 4: Commit** — `feat: supabase schema — robots, configs, grants, RPCs`

### Task 4: `Sync.kt` — queue, flush, config fetch (TDD on pure parts)

**Files:**
- Create: `android/app/src/main/java/com/troy/sansina/Sync.kt`
- Modify: `android/app/build.gradle.kts` (add `testImplementation("org.json:json:20240303")`)
- Test: `android/app/src/test/java/com/troy/sansina/SyncTest.kt`

**Interfaces:**
- Produces:
  - `class SyncSettings(ctx)` — `url, anonKey, robotId, deviceToken: String` (SharedPreferences `sansina_sync`), `val configured: Boolean` (all non-blank), `lastSyncAt: Long`, `save(...)`.
  - `object SyncCodec` (pure, testable): `encodeEvents(events: List<GrantEvent>): String` (JSON array `[{"client_uuid","amount","config_version","granted_at"}]`, ISO-8601 UTC), `parseConfig(json: String): Pair<Int, PromoConfig>?` (null if invalid).
  - `data class GrantEvent(val uuid: String, val amount: Int, val version: Int, val atMs: Long)`
  - `class Sync(ctx, settings)` — `enqueue(amount: Int, version: Int)`, `suspend fun flush(): Boolean`, `suspend fun fetchConfig(): Pair<Int, PromoConfig>?`, `val pending: Int`. Queue persisted as JSON in prefs `sansina_queue`; HTTP = `HttpURLConnection` POST to `$url/rest/v1/rpc/<fn>` with `apikey`/`Authorization: Bearer <anonKey>` headers, 10s timeouts, all errors → `false`/`null` (queue kept).

- [ ] **Step 1: Failing tests**

```kotlin
package com.troy.sansina
import org.junit.Assert.*
import org.junit.Test

class SyncTest {
    @Test fun encodeEventsProducesJsonArray() {
        val s = SyncCodec.encodeEvents(listOf(GrantEvent("u1", 250, 3, 0L)))
        val o = org.json.JSONArray(s).getJSONObject(0)
        assertEquals("u1", o.getString("client_uuid")); assertEquals(250, o.getInt("amount"))
        assertEquals(3, o.getInt("config_version")); assertEquals("1970-01-01T00:00:00Z", o.getString("granted_at"))
    }
    @Test fun parseConfigValid() {
        val (v, c) = SyncCodec.parseConfig("""{"version":7,"promos":[{"amount":250,"weight":40,"limit":5},{"amount":500,"weight":60,"limit":0}]}""")!!
        assertEquals(7, v); assertTrue(c.isValid); assertEquals(5, c.promos[0].limit)
    }
    @Test fun parseConfigRejectsInvalid() {
        assertNull(SyncCodec.parseConfig("""{"version":1,"promos":[{"amount":250,"weight":40,"limit":0}]}""")) // weights != 100
        assertNull(SyncCodec.parseConfig("not json"))
    }
}
```

- [ ] **Step 2: Verify fail.** — `gradle -q -p android test`
- [ ] **Step 3: Implement `Sync.kt`** per interfaces above (codec pure; `Sync` uses `withContext(Dispatchers.IO)`).
- [ ] **Step 4: Tests pass.**
- [ ] **Step 5: Commit** — `feat: sync layer — offline grant queue, config fetch`

### Task 5: Wire sync into app + settings "Bağlantı" panel

**Files:**
- Modify: `android/app/src/main/java/com/troy/sansina/MainActivity.kt` (enqueue on win; poll loop on INVITE; apply remote config)
- Modify: `android/app/src/main/java/com/troy/sansina/Settings.kt` (new `Category.SYNC` "Bağlantı": URL/key/robot id/token fields + status line "Son eşitleme • Kuyrukta N kayıt"; reuse `Section`/`NumberField` styling with a plain text field)
- Modify: `android/app/src/main/java/com/troy/sansina/Game.kt` (`PromoStats` stores `configVersion: Int` in prefs alongside config)

**Interfaces:**
- Consumes: `Sync`, `SyncSettings`, `PromoConfig.active/draw` from earlier tasks.
- Produces: remote version stored in prefs key `KEY_CONFIG_VERSION` (`sansina_stats` prefs, int, default 0).

- [ ] **Step 1: Implement wiring** — in `MainActivity`: `val syncSettings = remember { SyncSettings(ctx) }; val sync = remember { Sync(ctx, syncSettings) }`; in the `stats.record` callback also `sync.enqueue(promo.amount, version)` + fire-and-forget `scope.launch { sync.flush() }`; `LaunchedEffect(phase, syncSettings.configured)`: while `phase == Phase.INVITE && configured` → `fetchConfig()`, if newer version: persist config + version, `stats.reset`, `state.applyConfig`, then `flush()`, `delay(60_000)`.
- [ ] **Step 2: Manual compile check** — `assembleDebug` passes.
- [ ] **Step 3: Settings panel** — add category with 4 text fields + save button + status; PIN gate already covers it.
- [ ] **Step 4: Build + quick emulator sanity (app unchanged when unconfigured).**
- [ ] **Step 5: Commit** — `feat: wire remote sync — enqueue grants, 60s config poll, settings panel`

### Task 6: Dashboard page

**Files:**
- Create: `dashboard/index.html` (self-contained; supabase-js v2 via `https://cdn.jsdelivr.net/npm/@supabase/supabase-js@2` UMD)
- Create: `dashboard/README.md` (open locally or drop on any static host; first-run prompts for Supabase URL + anon key, stored in localStorage; sign-in with dashboard email user)

**Interfaces:**
- Consumes: `usage_view`, `robots`, `robot_configs` selects; `save_config` RPC.

- [ ] **Step 1: Implement page** — email/password sign-in → robots × coupons table (used/limit/remaining, online badge `last_seen_at < 5 min`, fleet totals), 30s auto-refresh; config editor (robot picker + "Tümü", rows amount/weight/limit, client-side weight-sum validation, Save → `save_config` per target). Turkish labels.
- [ ] **Step 2: Static check** — open in browser, verify sign-in screen renders and errors surface (no Supabase project yet: graceful failure message).
- [ ] **Step 3: Commit** — `feat: fleet dashboard — usage counts + remote coupon config`

### Task 7: End-to-end verification

**Files:** none (verification only) — plus README updates in `README.md` (backend section).

- [ ] **Step 1: Unit suite green** — `gradle -q -p android test`.
- [ ] **Step 2: Emulator run** — build, install on Pixel_Tablet_Fresh, play a full round unconfigured (behavior unchanged), set a limit of 1 via on-device settings and confirm the coupon stops appearing, all-exhausted shows "Kampanya sona erdi".
- [ ] **Step 3: If a Supabase project is available**: run migration, seed a robot, configure the tablet, verify a grant lands in `grants` and a dashboard config push arrives on device within 60s. Otherwise record this as the single remaining manual step in `supabase/README.md`.
- [ ] **Step 4: Update root `README.md`** — short "Backend" section linking spec, `supabase/`, `dashboard/`.
- [ ] **Step 5: Commit** — `docs: backend setup + verification notes`
