package com.troy.sansina

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.random.Random

/** The mechanic, identical across all five directions. */
enum class Phase { INVITE, MERHABA, DEAL, FLIP, SHUFFLE, PICK, PRIZE, QR }

enum class Product(val label: String) {
    IPHONE("iPhone"), IPAD("iPad"), MACBOOK("MacBook"), WATCH("Apple Watch"), AIRPODS("AirPods")
}

/** Colour ladder: material gets richer as value climbs. */
enum class Tier { ALUMINIUM, BRONZE, SILVER, GOLD, PREMIUM }

/** One promo lever: a discount amount and the share of draws (in %) it should win. */
data class Promo(val amount: Int, val weight: Int) {
    val label: String get() = "%,d".format(amount).replace(',', '.') + " TL"
}

data class Card(val product: Product, val promo: Promo, val tier: Tier)

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
        val DEFAULT = PromoConfig(listOf(Promo(250, 40), Promo(500, 30), Promo(750, 15), Promo(1000, 10), Promo(1500, 5)))
        fun parse(s: String?): PromoConfig = runCatching {
            PromoConfig(s!!.split(";").map { val (a, w) = it.split(","); Promo(a.toInt(), w.toInt()) })
        }.getOrDefault(DEFAULT)
    }
}

/** Running tally of how often each promo was shown. Reset whenever the config changes. */
class PromoStats(private val ctx: Context) {
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

class GameState(var config: PromoConfig, private val onWin: (Promo) -> Unit) {
    var phase by mutableStateOf(Phase.INVITE)
    var cards by mutableStateOf(deal(config))
    var dealtCount by mutableStateOf(0)
    var flippedCount by mutableStateOf(0)
    var shuffleStep by mutableStateOf(0)   // 0 rest, 1 fan, 2 split, 3 cross, 4 collapse, 5 cut, 6 re-deal
    var winner by mutableStateOf(0)
    var revealed by mutableStateOf(false)
    var runId by mutableStateOf(0)

    val winningCard: Card get() = cards[winner]

    fun reset() {
        runId++
        phase = Phase.INVITE
        dealtCount = 0; flippedCount = 0; shuffleStep = 0; revealed = false
        cards = deal(config)
    }

    fun applyConfig(c: PromoConfig) { config = c; reset() }

    /** Weighted draw by the configured percentages. */
    private fun drawWinner(): Int {
        var r = Random.nextInt(config.totalWeight.coerceAtLeast(1))
        cards.forEachIndexed { i, c -> r -= c.promo.weight; if (r < 0) return i }
        return cards.lastIndex
    }

    /** Plays the whole choreography. Cancelled when [runId] changes. */
    suspend fun play() {
        runId++
        val my = runId
        suspend fun step(ms: Long): Boolean { delay(ms); return my == runId }

        phase = Phase.MERHABA
        if (!step(1700)) return

        phase = Phase.DEAL
        dealtCount = 0
        for (i in 1..cards.size) { dealtCount = i; if (!step(260)) return }
        if (!step(500)) return

        phase = Phase.FLIP
        flippedCount = 0
        for (i in 1..cards.size) { flippedCount = i; if (!step(240)) return }
        if (!step(700)) return

        phase = Phase.SHUFFLE
        for (s in 1..6) { shuffleStep = s; if (!step(460)) return }
        winner = drawWinner()
        shuffleStep = 0
        if (!step(400)) return

        phase = Phase.PICK
        revealed = false
        if (!step(600)) return
        revealed = true
        onWin(winningCard.promo)
        if (!step(2600)) return

        phase = Phase.PRIZE
        if (!step(3000)) return

        phase = Phase.QR
    }

    companion object {
        fun deal(config: PromoConfig): List<Card> {
            val ladder = config.ladder().shuffled()
            val products = Product.entries.shuffled()
            return ladder.mapIndexed { i, (p, t) -> Card(products[i % products.size], p, t) }
        }
    }
}
