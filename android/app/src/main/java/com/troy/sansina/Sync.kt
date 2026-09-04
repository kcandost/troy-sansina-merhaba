package com.troy.sansina

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** One QR shown, waiting to be reported to the backend. */
data class GrantEvent(val uuid: String, val amount: Int, val version: Int, val atMs: Long)

/** fetch_config payload: the robot's config plus the amounts paused by the fleet-wide quota. */
data class RemoteConfig(val version: Int, val config: PromoConfig, val paused: Set<Int>)

/** Pure JSON encoding/decoding for the sync layer; kept side-effect free for unit tests. */
object SyncCodec {
    private fun iso(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(ms))

    /** Payload for the ingest_grants RPC. */
    fun encodeEvents(events: List<GrantEvent>): String = JSONArray().apply {
        events.forEach {
            put(JSONObject()
                .put("client_uuid", it.uuid)
                .put("amount", it.amount)
                .put("config_version", it.version)
                .put("granted_at", iso(it.atMs)))
        }
    }.toString()

    /** Local queue persistence: keeps the raw millis so nothing is lost in round-trips. */
    fun encodeQueue(events: List<GrantEvent>): String = JSONArray().apply {
        events.forEach { put(JSONObject().put("u", it.uuid).put("a", it.amount).put("v", it.version).put("t", it.atMs)) }
    }.toString()

    fun parseQueue(s: String?): List<GrantEvent> = runCatching {
        val arr = JSONArray(s!!)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            GrantEvent(o.getString("u"), o.getInt("a"), o.getInt("v"), o.getLong("t"))
        }
    }.getOrDefault(emptyList())

    /** fetch_config response; null unless the config is present and valid. */
    fun parseConfig(json: String): RemoteConfig? = runCatching {
        val o = JSONObject(json)
        val arr = o.getJSONArray("promos")
        val config = PromoConfig((0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            Promo(p.getInt("amount"), p.getInt("weight"), p.optInt("limit", 0))
        })
        val pausedArr = o.optJSONArray("paused")
        val paused = pausedArr?.let { (0 until it.length()).map { i -> it.getInt(i) }.toSet() } ?: emptySet()
        if (config.isValid) RemoteConfig(o.getInt("version"), config, paused) else null
    }.getOrNull()
}

/** Fleet backend baked into the build so fresh installs can self-enroll; the anon key is public by design. */
const val FLEET_URL = "https://pvgzgmhqwffytrnjebkg.supabase.co"
const val FLEET_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InB2Z3pnbWhxd2ZmeXRybmplYmtnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODg0NzMwNjAsImV4cCI6MjEwNDA0OTA2MH0.5Ya86a5XL7-SnCyqt52a_UXyWLx1A0y6pGojFB1FGgU"

/** Backend connection settings, edited from the hidden settings panel. */
class SyncSettings(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("sansina_sync", Context.MODE_PRIVATE)
    var url by mutableStateOf(prefs.getString("url", "")?.ifBlank { FLEET_URL } ?: FLEET_URL)
        private set
    var anonKey by mutableStateOf(prefs.getString("anon_key", "")?.ifBlank { FLEET_ANON_KEY } ?: FLEET_ANON_KEY)
        private set
    var robotId by mutableStateOf(prefs.getString("robot_id", "") ?: "")
        private set
    var deviceToken by mutableStateOf(prefs.getString("device_token", "") ?: "")
        private set
    var lastSyncAt by mutableStateOf(prefs.getLong("last_sync", 0L))

    val configured get() = url.isNotBlank() && anonKey.isNotBlank() && deviceToken.isNotBlank()

    fun save(url: String, anonKey: String, robotId: String, deviceToken: String) {
        this.url = url.trim().trimEnd('/'); this.anonKey = anonKey.trim()
        this.robotId = robotId.trim(); this.deviceToken = deviceToken.trim()
        prefs.edit().putString("url", this.url).putString("anon_key", this.anonKey)
            .putString("robot_id", this.robotId).putString("device_token", this.deviceToken).apply()
    }

    fun touch() { lastSyncAt = System.currentTimeMillis(); prefs.edit().putLong("last_sync", lastSyncAt).apply() }
}

/**
 * Offline-first sync: grants queue locally and flush when the network allows;
 * config is pulled on demand. Every failure is silent — the game must never notice.
 */
class Sync(ctx: Context, private val settings: SyncSettings) {
    private val prefs = ctx.getSharedPreferences("sansina_queue", Context.MODE_PRIVATE)
    var pending by mutableStateOf(SyncCodec.parseQueue(prefs.getString("q", null)).size)
        private set

    private fun queue() = SyncCodec.parseQueue(prefs.getString("q", null))
    private fun store(q: List<GrantEvent>) {
        prefs.edit().putString("q", SyncCodec.encodeQueue(q)).apply()
        pending = q.size
    }

    /** Oldest events drop past 500 so an offline-only install can't grow the queue forever. */
    fun enqueue(amount: Int, version: Int) =
        store((queue() + GrantEvent(UUID.randomUUID().toString(), amount, version, System.currentTimeMillis())).takeLast(500))

    /** Posts the whole queue via ingest_grants; drains it on success. */
    suspend fun flush(): Boolean {
        if (!settings.configured) return false
        val q = queue()
        if (q.isEmpty()) return true
        val body = JSONObject()
            .put("p_token", settings.deviceToken)
            .put("p_events", JSONArray(SyncCodec.encodeEvents(q)))
        return withContext(Dispatchers.IO) {
            if (rpc("ingest_grants", body.toString()) != null) { store(emptyList()); settings.touch(); true } else false
        }
    }

    /**
     * First-boot self-enrollment: registers this device's hardware id and stores the
     * returned token. A device the dashboard already claimed gets no token re-issue —
     * recovery goes through the dashboard's unclaim action.
     */
    suspend fun register(deviceId: String, model: String, name: String): Boolean {
        val body = JSONObject().put("p_device_id", deviceId).put("p_model", model).put("p_name", name)
        return withContext(Dispatchers.IO) {
            val resp = rpc("register_device", body.toString()) ?: return@withContext false
            runCatching {
                val o = JSONObject(resp)
                val token = o.optString("device_token", "")
                if (token.isBlank()) return@runCatching false
                settings.save(settings.url, settings.anonKey, o.getString("robot_id"), token)
                settings.touch()
                true
            }.getOrDefault(false)
        }
    }

    /** Liveness ping so the dashboard can tell a powered-off tablet from an idle one. */
    suspend fun heartbeat(): Boolean {
        if (!settings.configured) return false
        val body = JSONObject().put("p_token", settings.deviceToken)
        return withContext(Dispatchers.IO) { rpc("device_heartbeat", body.toString()) != null }
    }

    /** Latest remote config, or null (not configured / offline / invalid payload). */
    suspend fun fetchConfig(): RemoteConfig? {
        if (!settings.configured) return null
        val body = JSONObject().put("p_token", settings.deviceToken)
        return withContext(Dispatchers.IO) {
            rpc("fetch_config", body.toString())?.let { SyncCodec.parseConfig(it) }?.also { settings.touch() }
        }
    }

    private fun rpc(fn: String, body: String): String? = runCatching {
        val conn = URL("${settings.url}/rest/v1/rpc/$fn").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", settings.anonKey)
            conn.setRequestProperty("Authorization", "Bearer ${settings.anonKey}")
            conn.outputStream.use { it.write(body.toByteArray()) }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader().readText()
        } finally { conn.disconnect() }
    }.getOrNull()
}
