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
 * INVITE   cards orbit, invite text in the centre
 * FADE     invite text dissolves, cards keep orbiting
 * FILL     cards leave the orbit and tile the screen
 * FLIP     cards turn to promo values in escalating batches
 * SHUFFLE  cards swap slots, faster each step
 * PICK     losers fall away, the winner grows
 * PRIZE / QR
 */
enum class Phase { INVITE, FADE, FILL, FLIP, SHUFFLE, PICK, PRIZE, QR }

/** Colour ladder: material gets richer as value climbs. */
enum class Tier { ALUMINIUM, BRONZE, SILVER, GOLD, PREMIUM }

/** One promo lever: a discount amount and the share of draws (in %) it should win. */
data class Promo(val amount: Int, val weight: Int) {
    val label: String get() = "%,d".format(amount).replace(',', '.') + " TL"
}

data class ProductAsset(val file: String, val category: String)

data class Card(val product: ProductAsset, val promo: Promo, val tier: Tier)

/** Promo configuration, persisted; weights must sum to 100. */
data class PromoConfig(val promos: List<Promo>) {
    val totalWeight get() = promos.sumOf { it.weight }
    val isValid get() = promos.size in MIN_PROMOS..MAX_PROMOS && totalWeight == 100 && promos.all { it.amount > 0 && it.weight >= 0 }

    /** Sorted by amount ascending, each mapped onto the 5-step colour ladder. */
    fun ladder(): List<Pair<Promo, Tier>> {
        val sorted = promos.sortedBy { it.amount }
        val n = sorted.size
        return sorted.mapIndexed { i, p ->
            val t = if (n == 1) Tier.PREMIUM else Tier.entries[(i * (Tier.entries.size - 1) + (n - 1) / 2) / (n - 1)]
            p to t
        }
    }

    fun serialize() = promos.joinToString(";") { "${it.amount},${it.weight}" }

    companion object {
        const val MIN_PROMOS = 2
        const val MAX_PROMOS = 6
        const val MIN_CARDS = 8
        const val MAX_CARDS = 40
        const val DEFAULT_CARDS = 20
        val DEFAULT = PromoConfig(listOf(Promo(250, 40), Promo(500, 30), Promo(750, 15), Promo(1000, 10), Promo(1500, 5)))
        fun parse(s: String?): PromoConfig = runCatching {
            PromoConfig(s!!.split(";").map { val (a, w) = it.split(","); Promo(a.toInt(), w.toInt()) })
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

    /** Round-robin across categories so no single kind dominates the ring. */
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

class GameState(private val ctx: Context, var config: PromoConfig, var cardCount: Int, private val onWin: (Promo) -> Unit) {
    var phase by mutableStateOf(Phase.INVITE)
    var cards by mutableStateOf(deal())
    /** Which cards are face-up (promo side). Grows in escalating batches. */
    var flipped by mutableStateOf<Set<Int>>(emptySet())
    /** Slot index for each card during shuffle; identity when not shuffling. */
    var slots by mutableStateOf<List<Int>>(emptyList())
    var shuffleStep by mutableStateOf(0)
    var winner by mutableStateOf(0)
    var revealed by mutableStateOf(false)
    var runId by mutableStateOf(0)

    val winningCard: Card get() = cards[winner]

    fun reset() {
        runId++
        phase = Phase.INVITE
        flipped = emptySet(); shuffleStep = 0; revealed = false
        cards = deal()
        slots = cards.indices.toList()
    }

    fun applyConfig(c: PromoConfig, count: Int = cardCount) { config = c; cardCount = count; reset() }

    private fun deal(): List<Card> {
        val n = cardCount.coerceIn(PromoConfig.MIN_CARDS, PromoConfig.MAX_CARDS)
        val products = Catalogue.balanced(ctx, n)
        val ladder = config.ladder()
        // Every promo appears at least once; the rest are distributed by weight so the board "looks" like the odds.
        val promoList = ArrayList<Pair<Promo, Tier>>(n)
        promoList += ladder
        while (promoList.size < n) {
            var r = Random.nextInt(config.totalWeight.coerceAtLeast(1))
            promoList += ladder.firstOrNull { r -= it.first.weight; r < 0 } ?: ladder.last()
        }
        promoList.shuffle()
        return products.mapIndexed { i, p -> Card(p, promoList[i].first, promoList[i].second) }
    }

    /** Weighted draw by the configured percentages, then a random card carrying that promo. */
    private fun drawWinner(): Int {
        var r = Random.nextInt(config.totalWeight.coerceAtLeast(1))
        val promo = config.promos.firstOrNull { r -= it.weight; r < 0 } ?: config.promos.last()
        val candidates = cards.indices.filter { cards[it].promo.amount == promo.amount }
        return candidates.random()
    }

    /** Plays the whole choreography. Cancelled when [runId] changes. */
    suspend fun play() {
        runId++
        val my = runId
        suspend fun step(ms: Long): Boolean { delay(ms); return my == runId }
        if (slots.size != cards.size) slots = cards.indices.toList()

        phase = Phase.FADE
        if (!step(1300)) return

        phase = Phase.FILL
        if (!step(1400)) return

        // Escalating flips: 1, 2, 3, 5, 8, 13 … until every card is face-up.
        phase = Phase.FLIP
        val order = cards.indices.shuffled()
        var a = 1; var b = 2; var i = 0
        var gap = 900L
        while (i < order.size) {
            val batch = order.subList(i, minOf(order.size, i + a))
            flipped = flipped + batch
            i += a
            val next = a + b; a = b; b = next
            if (!step(gap)) return
            gap = (gap * 0.82).toLong().coerceAtLeast(260)
        }
        if (!step(700)) return

        // Shuffle: swap slots, accelerating.
        phase = Phase.SHUFFLE
        var d = 620L
        for (s in 1..7) {
            shuffleStep = s
            slots = slots.shuffled()
            if (!step(d)) return
            d = (d * 0.85).toLong().coerceAtLeast(300)
        }
        slots = cards.indices.toList()
        if (!step(500)) return

        winner = drawWinner()
        phase = Phase.PICK
        revealed = false
        if (!step(700)) return
        revealed = true
        onWin(winningCard.promo)
        if (!step(2800)) return

        phase = Phase.PRIZE
        if (!step(3000)) return

        phase = Phase.QR
    }
}
