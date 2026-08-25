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

@Composable
fun SansinaApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var theme by remember { mutableStateOf(AllThemes.firstOrNull { it.id == prefs.getString(KEY_THEME, "A") } ?: ThemeA) }
    var pickerOpen by remember { mutableStateOf(false) }
    val state = remember { GameState() }
    val scope = rememberCoroutineScope()
    val phase = state.phase

    fun start() { scope.launch { state.play() } }

    Box(Modifier.fillMaxSize()) {
        WorldBackground(theme, phase)

        // Tap anywhere: starts the flow from invite, returns to invite mid-flow.
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (phase == Phase.INVITE) start() else if (phase != Phase.QR) state.reset()
            }
        )

        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(220)) },
            label = "phase",
            contentKey = { p ->
                when (p) { Phase.DEAL, Phase.FLIP, Phase.SHUFFLE, Phase.PICK -> "deck"; else -> p.name }
            }
        ) { p ->
            when (p) {
                Phase.INVITE -> InviteScreen(theme, onStart = ::start)
                Phase.MERHABA -> HelloScreen(theme)
                Phase.DEAL, Phase.FLIP, Phase.SHUFFLE, Phase.PICK -> DeckScreen(state, theme)
                Phase.PRIZE -> PrizeScreen(state, theme)
                Phase.QR -> QrScreen(state, theme, onRestart = { state.reset() })
            }
        }

        BrandLockup(theme, phase, Modifier.align(Alignment.TopCenter).systemBarsPadding().padding(top = 36.dp))

        SettingsButton(theme, phase, onClick = { pickerOpen = true }, modifier = Modifier.align(Alignment.TopStart).systemBarsPadding().padding(start = 24.dp, top = 24.dp))

        AnimatedVisibility(pickerOpen, enter = fadeIn(tween(200)), exit = fadeOut(tween(160))) {
            ThemePicker(
                current = theme,
                onPick = { t ->
                    theme = t
                    prefs.edit().putString(KEY_THEME, t.id).apply()
                    pickerOpen = false
                },
                onDismiss = { pickerOpen = false }
            )
        }
    }
}
