package com.troy.sansina

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTest {
    @Test fun encodeEventsProducesJsonArray() {
        val s = SyncCodec.encodeEvents(listOf(GrantEvent("u1", 250, 3, 0L)))
        val o = org.json.JSONArray(s).getJSONObject(0)
        assertEquals("u1", o.getString("client_uuid"))
        assertEquals(250, o.getInt("amount"))
        assertEquals(3, o.getInt("config_version"))
        assertEquals("1970-01-01T00:00:00Z", o.getString("granted_at"))
    }

    @Test fun eventsRoundTripThroughQueueJson() {
        val events = listOf(GrantEvent("u1", 250, 3, 123L), GrantEvent("u2", 500, 3, 456L))
        assertEquals(events, SyncCodec.parseQueue(SyncCodec.encodeQueue(events)))
        assertEquals(emptyList<GrantEvent>(), SyncCodec.parseQueue("garbage"))
    }

    @Test fun parseConfigValid() {
        val r = SyncCodec.parseConfig(
            """{"version":7,"promos":[{"amount":250,"weight":40,"limit":5},{"amount":500,"weight":60,"limit":0}],"paused":[500]}"""
        )!!
        assertEquals(7, r.version)
        assertTrue(r.config.isValid)
        assertEquals(5, r.config.promos[0].limit)
        assertEquals(setOf(500), r.paused)
    }

    @Test fun parseConfigDefaultsPausedEmpty() {
        val r = SyncCodec.parseConfig(
            """{"version":1,"promos":[{"amount":250,"weight":40,"limit":0},{"amount":500,"weight":60,"limit":0}]}"""
        )!!
        assertEquals(emptySet<Int>(), r.paused)
    }

    @Test fun parseConfigRejectsInvalid() {
        assertNull(SyncCodec.parseConfig("""{"version":1,"promos":[{"amount":250,"weight":40,"limit":0}]}"""))
        assertNull(SyncCodec.parseConfig("""{"version":0,"promos":null}"""))
        assertNull(SyncCodec.parseConfig("not json"))
    }
}
