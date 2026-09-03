package com.troy.sansina

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
