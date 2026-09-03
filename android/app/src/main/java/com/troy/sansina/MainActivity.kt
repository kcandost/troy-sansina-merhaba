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
import androidx.compose.ui.input.pointer.pointerInput
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
    // Robot pause-on-touch (same mechanism as Rozy Assistant): while a visitor uses the
    // kiosk the paired cleaning robot halts; the controller owns the pause-once +
    // sliding-60s-resume logic. The robot address is fixed (RobotPause.BASE_URL).
    val robot = remember { RobotPause.controller(ctx) }
    val state = remember { GameState(ctx, config, counts = { stats.counts }) { stats.record(it) } }
    // Every promo at its grant cap: the game must not start (brand directive).
    val exhausted = config.active(stats.counts).isEmpty()
    val voice = remember { Voice(ctx) }
    val scope = rememberCoroutineScope()
    val phase = state.phase

    // Robot voice: invite on idle (cooldown inside Voice), win line on the reveal.
    LaunchedEffect(phase, settingsOpen) {
        if (settingsOpen) { voice.stop(); return@LaunchedEffect }
        when (phase) {
            Phase.INVITE -> { delay(600); voice.invite() }
            Phase.REVEAL -> voice.win()
            else -> Unit
        }
    }

    fun start() { if (!exhausted) scope.launch { state.startSelection() } }
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

    Box(
        // Robot pause-on-touch capture. On the Initial pass the root sees every event
        // BEFORE any child can consume it, so a tap on a card, the gear, the PIN pad or
        // the settings panel all count — the whole screen, as the spec requires.
        //
        // Any PRESSED pointer counts, not just changedToDown(): a finger held down or
        // dragged emits no new down event, so a down-only trigger would let the 60 s
        // window elapse while a visitor is still touching and the robot would drive off
        // mid-interaction — the one failure this feature exists to prevent.
        Modifier.fillMaxSize().pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                    if (event.changes.any { it.pressed }) robot.onUserTouch()
                }
            }
        }
    ) {
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

        AnimatedVisibility(exhausted && phase == Phase.INVITE, enter = fadeIn(tween(420)), exit = fadeOut(tween(260))) {
            CampaignEnded(theme)
        }

        // Figma: the campaign look carries no top wordmark — frame 5's big logo is part of the
        // invite composition (drawn by InviteText); frames 6–11 have none at all.
        if (theme.id != "F") BrandLockup(theme, phase, Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 36.dp))

        // On the campaign look the gear tucks into the CERCEVE frame's top-left corner (frame inset ≈45dp).
        val gearInset = if (theme.id == "F") 58.dp else 24.dp
        SettingsButton(theme, phase, onClick = { gate = true }, modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(start = gearInset, top = gearInset))

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
