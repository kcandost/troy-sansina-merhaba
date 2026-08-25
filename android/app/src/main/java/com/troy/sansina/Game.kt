package com.troy.sansina

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
enum class Tier(val amount: Int, val label: String) {
    ALUMINIUM(250, "250 TL"),
    BRONZE(500, "500 TL"),
    SILVER(750, "750 TL"),
    GOLD(1000, "1.000 TL"),
    PREMIUM(1500, "1.500 TL")
}

data class Card(val product: Product, val tier: Tier)

class GameState {
    var phase by mutableStateOf(Phase.INVITE)
    var cards by mutableStateOf(defaultDeal())
    var dealtCount by mutableStateOf(0)
    var flippedCount by mutableStateOf(0)
    var shuffleStep by mutableStateOf(0)   // 0 rest, 1 fan, 2 split, 3 cross, 4 collapse, 5 cut, 6 re-deal
    var winner by mutableStateOf(2)
    var revealed by mutableStateOf(false)
    var runId by mutableStateOf(0)

    val winningCard: Card get() = cards[winner]

    fun reset() {
        runId++
        phase = Phase.INVITE
        dealtCount = 0; flippedCount = 0; shuffleStep = 0; revealed = false
        cards = defaultDeal()
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
        for (i in 1..5) { dealtCount = i; if (!step(260)) return }
        if (!step(500)) return

        phase = Phase.FLIP
        flippedCount = 0
        for (i in 1..5) { flippedCount = i; if (!step(240)) return }
        if (!step(700)) return

        phase = Phase.SHUFFLE
        for (s in 1..6) { shuffleStep = s; if (!step(460)) return }
        winner = Random.nextInt(5)
        shuffleStep = 0
        if (!step(400)) return

        phase = Phase.PICK
        revealed = false
        if (!step(600)) return
        revealed = true
        if (!step(2600)) return

        phase = Phase.PRIZE
        if (!step(3000)) return

        phase = Phase.QR
    }

    companion object {
        fun defaultDeal(): List<Card> {
            val tiers = Tier.entries.shuffled()
            return Product.entries.mapIndexed { i, p -> Card(p, tiers[i]) }
        }
    }
}
