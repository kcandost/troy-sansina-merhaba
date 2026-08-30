package com.troy.sansina

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent { SansinaApp() }
    }
}

private const val PREFS = "sansina"
private const val KEY_THEME = "theme"
private const val KEY_PROMOS = "promos"
private const val KEY_IDLE = "idle_seconds"
private const val KEY_CARD_BACK = "card_back"

/** How long the QR screen stays before returning to the invite (spec: 15–20 s). */
const val DEFAULT_IDLE_SECONDS = 18

@Composable
fun SansinaApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(AllThemes.firstOrNull { it.id == prefs.getString(KEY_THEME, "F") } ?: ThemeF) }
    var config by remember { mutableStateOf(PromoConfig.parse(prefs.getString(KEY_PROMOS, null)).let { if (it.isValid) it else PromoConfig.DEFAULT }) }
    val stats = remember { PromoStats(ctx).also { it.load(config) } }
    var gate by remember { mutableStateOf(false) }       // PIN prompt showing
    var settingsOpen by remember { mutableStateOf(false) }
    var idleSeconds by remember { mutableStateOf(prefs.getInt(KEY_IDLE, DEFAULT_IDLE_SECONDS)) }
    var cardBack by remember { mutableStateOf(runCatching { CardBack.valueOf(prefs.getString(KEY_CARD_BACK, null)!!) }.getOrDefault(CardBack.TROY)) }
    val state = remember { GameState(ctx, config) { stats.record(it) } }
    val scope = rememberCoroutineScope()
    val phase = state.phase

    fun start() { scope.launch { state.startSelection() } }
    fun pick(i: Int) { scope.launch { state.pick(i) } }
    fun flip() { scope.launch { state.flip() } }

    // Idle handling: the result/QR page returns after the configured time;
    // an abandoned selection or unflipped card returns after a longer grace period.
    LaunchedEffect(phase, idleSeconds, settingsOpen) {
        if (settingsOpen) return@LaunchedEffect
        when (phase) {
            Phase.RESULT -> { delay(Timing.RESULT_QR_DELAY + idleSeconds * 1000L); state.reset() }
            Phase.SELECT, Phase.READY -> { delay(Timing.SELECT_IDLE); state.reset() }
            else -> Unit
        }
    }

    androidx.activity.compose.BackHandler { if (phase != Phase.INVITE) state.reset() }

    Box(Modifier.fillMaxSize()) {
        WorldBackground(theme, phase)

        // Tap anywhere on the idle screen starts the flow.
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (phase == Phase.INVITE) start()
            }
        )

        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(260)) },
            label = "phase",
            contentKey = { p -> if (p == Phase.RESULT) "result" else "stage" }
        ) { p ->
            when (p) {
                Phase.RESULT -> ResultScreen(state, theme, onRestart = { state.reset() })
                else -> StageScreen(state, theme, cardBack, onStart = ::start, onPick = ::pick, onFlip = ::flip)
            }
        }

        BrandLockup(theme, phase, Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 36.dp))

        SettingsButton(theme, phase, onClick = { gate = true }, modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(start = 24.dp, top = 24.dp))

        AnimatedVisibility(gate, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            PinGate(onUnlock = { gate = false; settingsOpen = true; state.reset() }, onCancel = { gate = false })
        }
        AnimatedVisibility(settingsOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            SettingsScreen(
                theme = theme, config = config, stats = stats, idleSeconds = idleSeconds, cardBack = cardBack,
                onIdleSeconds = { v -> idleSeconds = v; prefs.edit().putInt(KEY_IDLE, v).apply() },
                onCardBack = { b -> cardBack = b; prefs.edit().putString(KEY_CARD_BACK, b.name).apply() },
                onTheme = { t -> theme = t; prefs.edit().putString(KEY_THEME, t.id).apply() },
                onConfig = { c ->
                    config = c
                    prefs.edit().putString(KEY_PROMOS, c.serialize()).apply()
                    stats.reset(c)
                    state.applyConfig(c)
                },
                onClose = { settingsOpen = false }
            )
        }
    }
}
