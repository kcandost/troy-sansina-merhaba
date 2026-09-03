package com.troy.sansina

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.random.Random

/**
 * INVITE   idle: "merhaba" + CTA breathe, waiting for a touch
 * SELECT   four cards slide in one by one; the user picks one
 * SHUFFLE  cards swap places for ~2.5 s, then the picked card comes to the centre, the rest fade out
 * READY    one card in the centre, glowing; "Hemen çevir, avantajını gör!"
 * REVEAL   the card flips (0.6 s) with a light burst
 * RESULT   amount scales up, "avantajına merhaba", confetti; QR + instruction fade in after 0.5 s;
 *          the screen then stays still until the idle timer returns to INVITE
 */
enum class Phase { INVITE, SELECT, SHUFFLE, READY, REVEAL, RESULT }

/** Number of cards offered on the selection screen. */
const val CARD_COUNT = 4

/** What the face-down side of a card shows. */
enum class CardBack(val label: String, val hint: String) {
    TROY("TROY", "Tüm kartlar TROY logosu taşır"),
    PRODUCT("Ürün", "Her kartta bir ürün fotoğrafı")
}

/** Colour ladder: material gets richer as value climbs. */
enum class Tier { ALUMINIUM, BRONZE, SILVER, GOLD, PREMIUM }

/** One promo lever: a discount amount, its share of draws (in %), and a grant cap (0 = unlimited). */
data class Promo(val amount: Int, val weight: Int, val limit: Int = 0) {
    val label: String get() = "%,d".format(amount).replace(',', '.') + " TL"
}

data class ProductAsset(val file: String, val category: String)

data class Card(val product: ProductAsset, val promo: Promo, val tier: Tier)

/** Promo configuration, persisted; weights must sum to 100. */
data class PromoConfig(val promos: List<Promo>) {
    val totalWeight get() = promos.sumOf { it.weight }
    val isValid get() = promos.size in MIN_PROMOS..MAX_PROMOS && totalWeight == 100 && promos.all { it.amount > 0 && it.weight >= 0 && it.limit >= 0 }

    /** Promos still grantable given the local counts since the last config change. */
    fun active(counts: Map<Int, Int>) = promos.filter { it.limit == 0 || (counts[it.amount] ?: 0) < it.limit }

    /** Sorted by amount ascending, each mapped onto the 5-step colour ladder. */
    fun ladder(): List<Pair<Promo, Tier>> {
        val sorted = promos.sortedBy { it.amount }
        val n = sorted.size
        return sorted.mapIndexed { i, p ->
            val t = if (n == 1) Tier.PREMIUM else Tier.entries[(i * (Tier.entries.size - 1) + (n - 1) / 2) / (n - 1)]
            p to t
        }
    }

    fun tierOf(promo: Promo): Tier = ladder().firstOrNull { it.first.amount == promo.amount }?.second ?: Tier.PREMIUM

    fun serialize() = promos.joinToString(";") { "${it.amount},${it.weight},${it.limit}" }

    companion object {
        const val MIN_PROMOS = 2
        const val MAX_PROMOS = 6
        val DEFAULT = PromoConfig(listOf(Promo(250, 40), Promo(500, 30), Promo(750, 20), Promo(1000, 10)))
        fun parse(s: String?): PromoConfig = runCatching {
            PromoConfig(s!!.split(";").map {
                val f = it.split(",")
                Promo(f[0].toInt(), f[1].toInt(), f.getOrNull(2)?.toInt() ?: 0)
            })
        }.getOrDefault(DEFAULT)
    }
}

/** Running tally of how often each promo was shown. Reset whenever the config changes. */
class PromoStats(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("sansina_stats", Context.MODE_PRIVATE)
    var counts by mutableStateOf<Map<Int, Int>>(emptyMap())
        private set
    val total get() = counts.values.sum()

    fun load(config: PromoConfig) {
        if (prefs.getString("config", null) != config.serialize()) { reset(config); return }
        counts = config.promos.associate { it.amount to prefs.getInt("c_${it.amount}", 0) }
    }

    fun reset(config: PromoConfig) {
        prefs.edit().clear().putString("config", config.serialize()).apply()
        counts = config.promos.associate { it.amount to 0 }
    }

    fun record(promo: Promo) {
        val n = (counts[promo.amount] ?: 0) + 1
        counts = counts + (promo.amount to n)
        prefs.edit().putInt("c_${promo.amount}", n).apply()
    }
}

// ───────────────────────── Product catalogue ─────────────────────────

object Catalogue {
    @Volatile private var all: List<ProductAsset> = emptyList()
    private val bitmaps = HashMap<String, ImageBitmap>()

    fun load(ctx: Context): List<ProductAsset> {
        if (all.isNotEmpty()) return all
        val json = ctx.assets.open("products/manifest.json").bufferedReader().readText()
        val arr = JSONArray(json)
        all = (0 until arr.length()).map { arr.getJSONObject(it) }.map { ProductAsset(it.getString("file"), it.getString("category")) }
        return all
    }

    /** Round-robin across categories so no single kind dominates. */
    fun balanced(ctx: Context, n: Int, rnd: Random = Random.Default): List<ProductAsset> {
        val byCat = load(ctx).groupBy { it.category }.mapValues { it.value.shuffled(rnd).toMutableList() }
        val cats = byCat.keys.shuffled(rnd)
        val out = ArrayList<ProductAsset>(n)
        val cursors = HashMap<String, Int>()
        var i = 0
        while (out.size < n) {
            val c = cats[i % cats.size]; i++
            val list = byCat[c]!!
            val k = cursors.getOrDefault(c, 0)
            out += list[k % list.size]
            cursors[c] = k + 1
        }
        return out
    }

    suspend fun bitmap(ctx: Context, file: String): ImageBitmap? {
        bitmaps[file]?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                ctx.assets.open("products/$file").use { BitmapFactory.decodeStream(it) }.asImageBitmap()
            }.getOrNull()?.also { synchronized(bitmaps) { bitmaps[file] = it } }
        }
    }
}

// ───────────────────────── Game ─────────────────────────

/** Motion timings (ms), tuned to the requirements doc. */
object Timing {
    const val CARD_ENTER_GAP = 170L        // stagger between the four cards sliding in
    const val CARD_ENTER = 420L            // one card's fade/slide
    const val SHUFFLE_TOTAL = 2500L        // 2–3 s of swapping
    const val SHUFFLE_STEPS = 7
    const val COLLAPSE = 650L              // picked card to centre, others fade
    const val FLIP = 640L                  // 0.5–0.8 s card turn
    const val REVEAL_HOLD = 1100L          // burst + a beat before the result screen
    const val RESULT_QR_DELAY = 500L       // QR + instruction fade in after the amount
    const val SELECT_IDLE = 60_000L        // nobody picked / flipped: back to invite
}

class GameState(private val ctx: Context, var config: PromoConfig, private val onWin: (Promo) -> Unit) {
    var phase by mutableStateOf(Phase.INVITE)
    var cards by mutableStateOf(deal())
    /** Cards that have entered the selection screen (staggered). */
    var entered by mutableStateOf<Set<Int>>(emptySet())
    /** Slot index for each card during shuffle; identity when not shuffling. */
    var slots by mutableStateOf<List<Int>>(cards.indices.toList())
    var shuffleStep by mutableStateOf(0)
    /** Whether the shuffle has finished and the losers are collapsing away. */
    var collapsed by mutableStateOf(false)
    var winner by mutableStateOf(0)
    var revealed by mutableStateOf(false)
    var runId by mutableStateOf(0)

    val winningCard: Card get() = cards[winner]

    fun reset() {
        runId++
        phase = Phase.INVITE
        entered = emptySet(); shuffleStep = 0; collapsed = false; revealed = false
        cards = deal()
        slots = cards.indices.toList()
    }

    fun applyConfig(c: PromoConfig) { config = c; reset() }

    private fun deal(): List<Card> {
        val products = Catalogue.balanced(ctx, CARD_COUNT)
        val ladder = config.ladder().shuffled()
        val promoList = ArrayList<Pair<Promo, Tier>>(CARD_COUNT)
        promoList += ladder.take(CARD_COUNT)
        while (promoList.size < CARD_COUNT) {
            var r = Random.nextInt(config.totalWeight.coerceAtLeast(1))
            promoList += ladder.firstOrNull { r -= it.first.weight; r < 0 } ?: ladder.last()
        }
        promoList.shuffle()
        return products.mapIndexed { i, p -> Card(p, promoList[i].first, promoList[i].second) }
    }

    /** Weighted draw by the configured percentages. */
    private fun drawPromo(): Promo {
        var r = Random.nextInt(config.totalWeight.coerceAtLeast(1))
        return config.promos.firstOrNull { r -= it.weight; r < 0 } ?: config.promos.last()
    }

    private suspend fun step(my: Int, ms: Long): Boolean { delay(ms); return my == runId }

    /** Invite → selection: the four cards enter one after another. */
    suspend fun startSelection() {
        if (phase != Phase.INVITE) return
        runId++
        val my = runId
        entered = emptySet()
        slots = cards.indices.toList()
        phase = Phase.SELECT
        for (i in cards.indices) {
            entered = entered + i
            if (!step(my, Timing.CARD_ENTER_GAP)) return
        }
    }

    /** The user touched card [i]: shuffle, then bring that card to the centre. */
    suspend fun pick(i: Int) {
        if (phase != Phase.SELECT || i !in entered) return
        runId++
        val my = runId
        winner = i
        phase = Phase.SHUFFLE
        val stepMs = Timing.SHUFFLE_TOTAL / Timing.SHUFFLE_STEPS
        for (s in 1..Timing.SHUFFLE_STEPS) {
            shuffleStep = s
            var next = slots.shuffled()
            while (next == slots) next = slots.shuffled()
            slots = next
            if (!step(my, stepMs)) return
        }
        // The prize is decided by the configured odds; the picked card carries it.
        val promo = drawPromo()
        cards = cards.mapIndexed { k, c -> if (k == i) c.copy(promo = promo, tier = config.tierOf(promo)) else c }
        collapsed = true
        if (!step(my, Timing.COLLAPSE)) return
        phase = Phase.READY
    }

    /** The user touched the centre card: flip it and move on to the result. */
    suspend fun flip() {
        if (phase != Phase.READY) return
        runId++
        val my = runId
        phase = Phase.REVEAL
        revealed = true
        onWin(winningCard.promo)
        if (!step(my, Timing.FLIP + Timing.REVEAL_HOLD)) return
        phase = Phase.RESULT
    }
}
