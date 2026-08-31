package com.troy.sansina

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "RobotPause"

/** The paired robot as the controller needs it: id for logs, base URL to call. */
data class RobotTarget(val robotId: String, val baseUrl: String)

/**
 * Outcome of a robot task command (pause/resume), read from the Saha local API's
 * response envelope: `{"status_code":200,"success":true,"message":"","data":{},"error":{...}}`.
 *
 * The robot answers HTTP 200 even when it REFUSES the command and reports the real
 * outcome in the `success` field — so the status code alone is NOT confirmation.
 * A 2xx body that is not the known envelope leaves the status code as the only signal,
 * so it is trusted and logged verbatim rather than silently read as a refusal
 * (older firmware must keep working). Ported from Rozy Assistant.
 */
data class RobotCommandEnvelope(val confirmed: Boolean, val detail: String) {
    companion object {
        fun read(statusCode: Int, body: String?): RobotCommandEnvelope {
            if (statusCode !in 200..299) {
                return RobotCommandEnvelope(false, "HTTP $statusCode ${body.orEmpty().take(200)}".trim())
            }
            val json = body?.let { runCatching { JSONObject(it) }.getOrNull() }
                ?: return RobotCommandEnvelope(true, "HTTP $statusCode, no JSON envelope: ${body.orEmpty().take(200)}".trim())
            if (!json.has("success")) {
                return RobotCommandEnvelope(true, "HTTP $statusCode, no success field: ${body.take(200)}")
            }
            if (json.optBoolean("success")) return RobotCommandEnvelope(true, "HTTP $statusCode success=true")
            val error = json.optJSONObject("error")
            val reason = listOfNotNull(
                error?.optString("code")?.ifBlank { null },
                error?.optString("message")?.ifBlank { null },
                json.optString("message").ifBlank { null },
            ).joinToString(" ").ifBlank { "no reason given" }
            return RobotCommandEnvelope(false, "robot refused: $reason")
        }
    }
}

/**
 * Halts the paired Saha Robotik cleaning robot while a visitor is using the kiosk.
 * Ported from Rozy Assistant (spec 2026-07-07), identical semantics:
 *
 *  - Any screen touch pauses the robot (`POST <base>/api/v1/tasks/pause`).
 *    Pause is sent ONCE per engagement — repeated touches never re-POST.
 *  - Resume (`POST <base>/api/v1/tasks/resume`) fires 60 s after the LAST touch: a
 *    sliding window, restarted on every touch.
 *  - The robot must NEVER be stranded paused: resume retries with backoff
 *    (5 s → 15 s → 30 s, then every 60 s indefinitely), a `pausedByUs` flag is persisted
 *    so a crashed/killed app resumes the robot on next boot, and the resume target is the
 *    address captured AT PAUSE TIME (a mid-pause re-configure still resumes the old robot).
 *  - An unconfigured kiosk (no robot URL) is a total no-op.
 */
class RobotPauseController(
    private val scope: CoroutineScope,
    private val target: () -> RobotTarget?,
    private val persistPausedByUs: (Boolean) -> Unit,
    private val post: suspend (url: String) -> Boolean,
    private val pauseWindowMs: Long = DEFAULT_PAUSE_WINDOW_MS,
) {
    private enum class State { IDLE, PAUSING, PAUSED }

    private var state = State.IDLE
    private var pausedTarget: RobotTarget? = null
    private var resumeJob: Job? = null

    // True only while a resume request is actually on the wire. Cancelling then would
    // not un-send it — the robot may resume whether or not we are still listening — so
    // handleTouch waits for it to settle instead of cancelling.
    private var resumeInFlight = false

    // Set while a touch is waiting on that attempt, so a failed attempt hands control
    // back to the touch instead of sleeping through its next backoff.
    private var touchWaitingOnResume = false

    // Buffer(1)+DROP_OLDEST: a touch storm collapses to "touched again" — exactly the
    // signal the sliding window needs — while the collector serializes state changes.
    private val touches = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope.launch { touches.collect { handleTouch() } }
    }

    /** UI-thread entry point — called on every finger-down anywhere on screen. */
    fun onUserTouch() {
        touches.tryEmit(Unit)
    }

    /**
     * Boot-time crash recovery: if a previous process paused the robot and died before
     * resuming, resume it now (using the current configured address).
     */
    fun resumeIfPreviouslyPausedOnBoot(wasPausedByUs: Boolean) {
        if (!wasPausedByUs) return
        val t = target()
        if (t == null) {
            Log.w(TAG, "paused flag set but no robot target on boot — clearing")
            persistPausedByUs(false)
            return
        }
        Log.i(TAG, "previous run left ${t.robotId} paused — resuming on boot")
        state = State.PAUSED
        pausedTarget = t
        resumeJob?.cancel()
        resumeJob = scope.launch { resumeUntilSuccess(t) }
    }

    private var loggedNoTarget = false

    private suspend fun handleTouch() {
        when (state) {
            State.PAUSING -> Unit // pause in flight; the queued touch after it extends the window
            State.PAUSED -> {
                val inFlight = resumeJob?.takeIf { resumeInFlight }
                if (inFlight == null) {
                    restartResumeWindow()
                } else {
                    // A resume POST is on the wire; see it through, then react to the
                    // real outcome (re-pause if it landed, extend the window if not).
                    Log.d(TAG, "touch while resume is on the wire — waiting for it to settle")
                    touchWaitingOnResume = true
                    inFlight.join()
                    touchWaitingOnResume = false
                    if (state == State.IDLE) handleTouch() else restartResumeWindow()
                }
            }
            State.IDLE -> {
                val t = target()
                if (t == null) {
                    // Unconfigured → total no-op. Logged once per process so a field
                    // kiosk that SHOULD be pausing a robot is diagnosable.
                    if (!loggedNoTarget) {
                        loggedNoTarget = true
                        Log.d(TAG, "touch ignored — no robot URL configured")
                    }
                    return
                }
                loggedNoTarget = false
                state = State.PAUSING
                if (postPauseWithRetries(t)) {
                    state = State.PAUSED
                    pausedTarget = t
                    persistPausedByUs(true)
                    Log.i(TAG, "paused ${t.robotId}")
                    restartResumeWindow()
                } else {
                    // Robot unreachable — it never paused, so there is nothing to resume.
                    // The next touch tries again.
                    Log.w(TAG, "pause failed for ${t.robotId} — giving up until next touch")
                    state = State.IDLE
                }
            }
        }
    }

    /** Sliding 60 s window: each touch cancels the pending resume and starts it anew. */
    private fun restartResumeWindow() {
        val t = pausedTarget ?: return
        resumeJob?.cancel()
        resumeJob = scope.launch {
            delay(pauseWindowMs)
            resumeUntilSuccess(t)
        }
    }

    private suspend fun postPauseWithRetries(t: RobotTarget): Boolean {
        for (backoffMs in PAUSE_RETRY_DELAYS_MS) {
            delay(backoffMs)
            if (post("${t.baseUrl}$PAUSE_PATH")) return true
        }
        return false
    }

    /**
     * Resume with escalating backoff, then every [RESUME_STEADY_RETRY_MS] FOREVER while
     * paused — a stranded paused robot is the one unacceptable failure mode. Only
     * cancellable BETWEEN attempts: a request already on the wire is always seen
     * through, because the robot may act on it whether or not we are still listening.
     */
    private suspend fun resumeUntilSuccess(t: RobotTarget) {
        var attempt = 0
        while (true) {
            resumeInFlight = true
            val confirmed = try {
                post("${t.baseUrl}$RESUME_PATH")
            } finally {
                resumeInFlight = false
            }
            if (confirmed) {
                state = State.IDLE
                pausedTarget = null
                persistPausedByUs(false)
                Log.i(TAG, "resumed ${t.robotId}")
                return
            }
            if (touchWaitingOnResume) {
                // The robot is still paused, so there is nothing to undo: let the
                // waiting touch own the window instead of holding it through backoff.
                Log.d(TAG, "resume refused for ${t.robotId} — handing back to the waiting touch")
                return
            }
            val backoff = RESUME_RETRY_DELAYS_MS.getOrElse(attempt) { RESUME_STEADY_RETRY_MS }
            Log.w(TAG, "resume failed for ${t.robotId} — retrying in ${backoff}ms")
            delay(backoff)
            attempt++
        }
    }

    companion object {
        /** Resume this long after the LAST touch (spec: 60 s sliding window). */
        const val DEFAULT_PAUSE_WINDOW_MS = 60_000L
        const val PAUSE_PATH = "/api/v1/tasks/pause"
        const val RESUME_PATH = "/api/v1/tasks/resume"
        private val PAUSE_RETRY_DELAYS_MS = listOf(0L, 2_000L, 5_000L)
        private val RESUME_RETRY_DELAYS_MS = listOf(5_000L, 15_000L, 30_000L)
        private const val RESUME_STEADY_RETRY_MS = 60_000L
    }
}

/**
 * App-lifetime wiring: prefs-backed robot URL, one shared controller whose 60 s window
 * and resume-retry loop survive activity recreation, plus a read-only reachability probe
 * for the settings console.
 *
 * The robot API is a plain-HTTP LAN endpoint on the robot itself (port 7242, no auth);
 * a tight 3 s per-request timeout keeps an unreachable LAN address from blocking.
 */
object RobotPause {
    private const val PREFS = "sansina"
    private const val KEY_URL = "robot_base_url"
    private const val KEY_PAUSED = "robot_paused_by_us"

    @Volatile private var controller: RobotPauseController? = null

    fun baseUrl(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_URL, null)?.takeIf { it.isNotBlank() }

    fun setBaseUrl(ctx: Context, value: String?) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val v = value?.trim()?.trimEnd('/')
        prefs.edit().apply { if (v.isNullOrBlank()) remove(KEY_URL) else putString(KEY_URL, v) }.apply()
    }

    fun controller(context: Context): RobotPauseController {
        controller?.let { return it }
        synchronized(this) {
            controller?.let { return it }
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val c = RobotPauseController(
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
                target = { baseUrl(app)?.let { RobotTarget("saha-robot", it) } },
                persistPausedByUs = { prefs.edit().putBoolean(KEY_PAUSED, it).apply() },
                post = ::post,
            )
            controller = c
            c.resumeIfPreviouslyPausedOnBoot(prefs.getBoolean(KEY_PAUSED, false))
            return c
        }
    }

    private suspend fun post(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 3_000
            conn.readTimeout = 3_000
            val code = conn.responseCode
            val body = runCatching {
                (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText()
            }.getOrNull()
            conn.disconnect()
            val outcome = RobotCommandEnvelope.read(code, body)
            if (outcome.confirmed) Log.d(TAG, "POST confirmed: $url (${outcome.detail})")
            else Log.w(TAG, "POST not confirmed: $url (${outcome.detail})")
            outcome.confirmed
        } catch (e: Exception) {
            Log.w(TAG, "POST failed: $url", e)
            false
        }
    }

    /**
     * Settings-console "Test connection": read-only GET `<base>/api/v1/status` —
     * never touches the pause/resume endpoints. Returns a human-readable result line.
     */
    suspend fun probe(baseUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val conn = URL(baseUrl.trimEnd('/') + "/api/v1/status").openConnection() as HttpURLConnection
            conn.connectTimeout = 5_000
            conn.readTimeout = 5_000
            val code = conn.responseCode
            val body = runCatching { conn.inputStream.bufferedReader().readText() }.getOrNull()
            conn.disconnect()
            if (code !in 200..299) return@withContext "Ulaşılamadı: HTTP $code"
            val json = body?.let { runCatching { JSONObject(it) }.getOrNull() }
            val stateLabel = json?.optString("state_label")?.ifBlank { null }
            val battery = json?.takeIf { it.has("battery_percent") }?.optInt("battery_percent")
            listOfNotNull("Bağlantı başarılı", stateLabel, battery?.let { "pil %$it" }).joinToString(" · ")
        } catch (e: Exception) {
            "Ulaşılamadı: ${e.message ?: e.javaClass.simpleName}"
        }
    }
}
