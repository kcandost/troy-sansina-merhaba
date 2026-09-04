package com.troy.sansina

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

const val SETTINGS_PIN = "152723"

private val Ink = Color(0xFF0E1116)
private val Ink600 = Color(0xFF565E6C)
private val Ink200 = Color(0xFFE4E7EC)
private val Ink50 = Color(0xFFF7F8FA)
private val Blue = Color(0xFF0071E3)
private val Blue100 = Color(0xFFE3F0FD)
private val Red = Color(0xFFC42B1C)
private val Green = Color(0xFF12855C)

// ───────────────────────── PIN gate ─────────────────────────

@Composable
fun PinGate(onUnlock: () -> Unit, onCancel: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    BackHandler(onBack = onCancel)
    var error by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onCancel), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(360.dp).background(Color.White, RoundedCornerShape(24.dp)).clickable(enabled = false) {}.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ayarlar", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Devam etmek için şifreyi gir.", color = Ink600, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                repeat(6) { i ->
                    Box(Modifier.size(14.dp).clip(CircleShape).background(if (i < pin.length) (if (error) Red else Blue) else Ink200))
                }
            }
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    row.forEach { k ->
                        Box(
                            Modifier.size(84.dp, 56.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (k == "✓") Blue else Ink50)
                                .clickable {
                                    error = false
                                    when (k) {
                                        "⌫" -> pin = pin.dropLast(1)
                                        "✓" -> if (pin == SETTINGS_PIN) onUnlock() else { error = true; pin = "" }
                                        else -> if (pin.length < 6) pin += k
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) { Text(k, color = if (k == "✓") Color.White else Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            if (error) Text("Hatalı şifre", color = Red, fontSize = 13.sp)
        }
    }
}

/**
 * First-run setup: the device gets its fleet name at install time and appears on the
 * dashboard under it. Blocks the game until named; offline installs can defer (the
 * stored name keeps retrying in the background).
 */
@Composable
fun SetupScreen(deviceId: String, onRegister: suspend (String) -> Boolean, onDone: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize().background(Ink50), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(520.dp).background(Color.White, RoundedCornerShape(24.dp)).border(1.dp, Ink200, RoundedCornerShape(24.dp)).padding(32.dp)
        ) {
            Text("Cihaz kurulumu", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "Bu tablete bir ad ver; filo panelinde bu adla görünecek.",
                color = Ink600, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(64) },
                singleLine = true,
                textStyle = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth().height(54.dp).background(Ink50, RoundedCornerShape(12.dp)).border(1.dp, if (name.isBlank()) Ink200 else Blue, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) Text("ör. Kadıköy Mağaza", color = Ink600, fontSize = 18.sp)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (failed) Text(
                "Sunucuya ulaşılamadı. Yeniden dene veya çevrimdışı devam et — ad kaydedildi, bağlantı gelince otomatik tanıtılır.",
                color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp)
            )
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(deviceId, color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (failed) SmallButton("Çevrimdışı devam et", Ink50, Ink) { onDone(name.trim()) }
                val can = name.isNotBlank() && !saving
                SmallButton(if (saving) "Kaydediliyor…" else "Kaydet ve başla", if (can) Blue else Ink200, if (can) Color.White else Ink600, enabled = can) {
                    scope.launch {
                        saving = true
                        val ok = onRegister(name.trim())
                        saving = false
                        if (ok) onDone(name.trim()) else failed = true
                    }
                }
            }
        }
    }
}

// ───────────────────────── Settings page ─────────────────────────

private enum class Category(val label: String, val hint: String) {
    THEME("Tema", "Görünüm ve davranış"),
    PROMO("Promo", "Kollar ve pano"),
    ROBOT("Robot", "Duraklatma ve bağlantı"),
    SYNC("Bağlantı", "Uzak takip ve yönetim"),
}

@Composable
fun SettingsScreen(
    theme: SansinaTheme,
    config: PromoConfig,
    stats: PromoStats,
    idleSeconds: Int,
    cardBack: CardBack,
    onIdleSeconds: (Int) -> Unit,
    onCardBack: (CardBack) -> Unit,
    onTheme: (SansinaTheme) -> Unit,
    onConfig: (PromoConfig) -> Unit,
    onClose: () -> Unit,
    syncSettings: SyncSettings,
    sync: Sync,
) {
    BackHandler(onBack = onClose)
    var category by remember { mutableStateOf(Category.THEME) }

    Column(Modifier.fillMaxSize().background(Ink50)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 32.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ayarlar", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SmallButton("Kapat", Ink50, Ink, onClick = onClose)
        }
        Row(Modifier.fillMaxSize()) {
            // Left rail
            Column(Modifier.width(240.dp).fillMaxHeight().background(Color.White).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Category.entries.forEach { c ->
                    val sel = c == category
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (sel) Blue100 else Color.Transparent)
                            .clickable { category = c }.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(c.label, color = if (sel) Blue else Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(c.hint, color = Ink600, fontSize = 12.sp)
                    }
                }
            }
            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp)) {
                when (category) {
                    Category.THEME -> ThemeCategory(theme, config, idleSeconds, cardBack, onIdleSeconds, onCardBack, onTheme)
                    Category.PROMO -> PromoCategory(config, stats, onConfig)
                    Category.ROBOT -> RobotCategory()
                    Category.SYNC -> SyncCategory(syncSettings, sync)
                }
            }
        }
    }
}

@Composable
private fun ThemeCategory(theme: SansinaTheme, config: PromoConfig, idleSeconds: Int, cardBack: CardBack, onIdleSeconds: (Int) -> Unit, onCardBack: (CardBack) -> Unit, onTheme: (SansinaTheme) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f)) {
            Section("Tema", "Beş tasarım yönünden birini seç.") {
                AllThemes.forEach { t ->
                    val selected = t.id == theme.id
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Blue100 else Ink50)
                            .border(1.dp, if (selected) Blue else Ink200, RoundedCornerShape(14.dp))
                            .clickable { onTheme(t) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(t.bg).border(1.dp, Color(0x1A000000), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Text(t.letter, color = t.accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(t.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(t.subtitle, color = Ink600, fontSize = 12.sp)
                        }
                        if (selected) Box(Modifier.size(10.dp).background(Blue, CircleShape))
                    }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Section("Önizleme", "Seçili temanın davet ve kart ekranı.") {
                var previewDeck by remember { mutableStateOf(false) }
                ThemePreview(theme, config, cardBack, previewDeck, Modifier.fillMaxWidth())
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton("Davet", if (!previewDeck) Blue else Ink50, if (!previewDeck) Color.White else Ink) { previewDeck = false }
                    SmallButton("Kartlar", if (previewDeck) Blue else Ink50, if (previewDeck) Color.White else Ink) { previewDeck = true }
                }
            }
            Spacer(Modifier.height(24.dp))
            Section("Kart yüzü", "Seçim ekranında kartların kapalı yüzünde ne görünsün?") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CardBack.entries.forEach { b ->
                        val sel = b == cardBack
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (sel) Blue100 else Ink50)
                                .border(1.dp, if (sel) Blue else Ink200, RoundedCornerShape(14.dp)).clickable { onCardBack(b) }.padding(14.dp)
                        ) {
                            Text(b.label, color = if (sel) Blue else Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(b.hint, color = Ink600, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Section("Otomatik dönüş", "QR ekranında kimse dokunmazsa bu süre sonunda ana ekrana döner (önerilen 15–20 sn).") {
                var draft by remember(idleSeconds) { mutableStateOf(idleSeconds) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SmallButton("−5", Ink50, Ink, enabled = draft > 5) { draft = (draft - 5).coerceAtLeast(5) }
                    Text("$draft sn", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(96.dp), textAlign = TextAlign.Center)
                    SmallButton("+5", Ink50, Ink, enabled = draft < 300) { draft = (draft + 5).coerceAtMost(300) }
                    Spacer(Modifier.weight(1f))
                    val can = draft != idleSeconds
                    SmallButton("Uygula", if (can) Blue else Ink200, if (can) Color.White else Ink600, enabled = can) { onIdleSeconds(draft) }
                }
            }
        }
    }
}

/** The real stage rendered at tablet size and scaled into a small window. */
@Composable
private fun ThemePreview(theme: SansinaTheme, config: PromoConfig, cardBack: CardBack, deck: Boolean, modifier: Modifier) {
    val fullW = 1280.dp; val fullH = 800.dp
    val ctx = LocalContext.current
    BoxWithConstraints(modifier.aspectRatio(fullW / fullH).clip(RoundedCornerShape(16.dp)).border(1.dp, Ink200, RoundedCornerShape(16.dp))) {
        val scale = maxWidth / fullW
        val preview = remember(config, deck) {
            GameState(ctx, config) {}.apply {
                if (deck) { phase = Phase.SELECT; entered = cards.indices.toSet() }
            }
        }
        Box(
            Modifier.requiredSize(fullW, fullH).graphicsLayer {
                scaleX = scale; scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
        ) {
            WorldBackground(theme, preview.phase)
            StageScreen(preview, theme, cardBack, onStart = {}, onPick = {}, onFlip = {})
            BrandLockup(theme, preview.phase, Modifier.align(Alignment.TopCenter).padding(top = 36.dp))
        }
    }
}

@Composable
private fun PromoCategory(config: PromoConfig, stats: PromoStats, onConfig: (PromoConfig) -> Unit) {
    var draft by remember(config) { mutableStateOf(config.promos) }
    val draftCfg = PromoConfig(draft)
    val dirty = draft != config.promos
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1.1f)) {
            Section("Promosyon kolları", "İndirim tutarları ve çıkma yüzdeleri. Yüzdeler toplamı 100 olmalı. Kaydedince pano sıfırlanır.") {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text("Tutar (TL)", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Yüzde (%)", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Limit (0=∞)", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(44.dp))
                }
                draft.forEachIndexed { i, p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NumberField(p.amount, Modifier.weight(1f)) { v -> draft = draft.toMutableList().also { it[i] = p.copy(amount = v) } }
                        NumberField(p.weight, Modifier.weight(1f)) { v -> draft = draft.toMutableList().also { it[i] = p.copy(weight = v) } }
                        NumberField(p.limit, Modifier.weight(1f)) { v -> draft = draft.toMutableList().also { it[i] = p.copy(limit = v) } }
                        Box(
                            Modifier.size(36.dp).clip(CircleShape).background(if (draft.size > PromoConfig.MIN_PROMOS) Color(0xFFFBE3E0) else Ink50)
                                .clickable(enabled = draft.size > PromoConfig.MIN_PROMOS) { draft = draft.toMutableList().also { it.removeAt(i) } },
                            contentAlignment = Alignment.Center
                        ) { Text("−", color = if (draft.size > PromoConfig.MIN_PROMOS) Red else Ink200, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (draft.size < PromoConfig.MAX_PROMOS) SmallButton("+ Promosyon ekle", Ink50, Ink) {
                        draft = draft + Promo((draft.maxOfOrNull { it.amount } ?: 0) + 250, 0)
                    }
                    Spacer(Modifier.weight(1f))
                    val ok = draftCfg.totalWeight == 100
                    Text("Toplam: ${draftCfg.totalWeight}%", color = if (ok) Green else Red, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                if (draftCfg.totalWeight != 100) Text("Yüzdeler 100'e tamamlanmalı (${100 - draftCfg.totalWeight} fark).", color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                else if (draft.map { it.amount }.distinct().size != draft.size) Text("Tutarlar birbirinden farklı olmalı.", color = Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val canSave = dirty && draftCfg.isValid && draft.map { it.amount }.distinct().size == draft.size
                    SmallButton("Kaydet ve panoyu sıfırla", if (canSave) Blue else Ink200, if (canSave) Color.White else Ink600, enabled = canSave) { onConfig(draftCfg) }
                    if (dirty) SmallButton("Geri al", Ink50, Ink) { draft = config.promos }
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Section("Pano", "Her indirimin kaç kez gösterildiği ve gerçekleşen yüzdeler. Kollar değişince sıfırlanır.") {
                val total = stats.total
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                    Text("$total", color = Ink, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp)
                    Text("  oyun", color = Ink600, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text("İndirim", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(1.1f))
                    Text("Adet", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                    Text("Gerçekleşen", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                    Text("Hedef", color = Ink600, fontSize = 12.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                }
                config.ladder().forEach { (p, tier) ->
                    val c = stats.counts[p.amount] ?: 0
                    val pct = if (total == 0) 0f else c * 100f / total
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(Modifier.weight(1.1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(tier.palette.bg).border(1.dp, tier.palette.border, RoundedCornerShape(4.dp)))
                                Text(p.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                            }
                            Text("$c", color = Ink, fontSize = 15.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                            Text("%.1f%%".format(pct), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End)
                            Text("${p.weight}%", color = Ink600, fontSize = 15.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                        }
                        Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Ink200)) {
                            Box(Modifier.fillMaxWidth(p.weight / 100f).fillMaxHeight().background(Color(0xFFCDD2DA)))
                            Box(Modifier.fillMaxWidth(pct / 100f).fillMaxHeight().background(Blue, RoundedCornerShape(4.dp)))
                        }
                    }
                }
                Text("Mavi: gerçekleşen · Gri: hedef", color = Ink600, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
                    SmallButton("Panoyu sıfırla", Color(0xFFFBE3E0), Red) { stats.reset(config) }
                }
            }
        }
    }
}

@Composable
private fun RobotCategory() {
    var probeResult by remember { mutableStateOf<String?>(null) }
    var probing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(0.62f)) {
        Section(
            "Temizlik robotu",
            "Ekrana her dokunuşta robot duraklatılır; son dokunuştan 60 sn sonra görevine devam eder."
        ) {
            // Every robot serves its local API at the same fixed address on its own
            // network, so there is nothing to configure and nothing to mistype in the
            // field: the address is a constant in RobotPause.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Spacer(Modifier.weight(1f))
                SmallButton(if (probing) "Deneniyor…" else "Bağlantıyı test et", Ink50, Ink, enabled = !probing) {
                    probing = true; probeResult = null
                    scope.launch {
                        probeResult = RobotPause.probe()
                        probing = false
                    }
                }
            }
            probeResult?.let { r ->
                val ok = r.startsWith("Bağlantı başarılı")
                Text(r, color = if (ok) Green else Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                if (ok) Text(
                    "Test yalnızca durum uç noktasını okur; duraklat/devam davranışı sahada doğrulanmalıdır (dokun → durur, 60 sn sonra → devam eder).",
                    color = Ink600, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SyncCategory(settings: SyncSettings, sync: Sync) {
    var url by remember { mutableStateOf(settings.url) }
    var anonKey by remember { mutableStateOf(settings.anonKey) }
    var robotId by remember { mutableStateOf(settings.robotId) }
    var token by remember { mutableStateOf(settings.deviceToken) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth(0.62f)) {
        Section(
            "Uzak takip ve yönetim",
            "Supabase bağlantısı: kupon kullanımları merkeze raporlanır, kupon ayarları merkezden alınır. Boş bırakılırsa cihaz tamamen çevrimdışı çalışır."
        ) {
            TextRow("Sunucu adresi (https://…supabase.co)", url) { url = it }
            TextRow("API anahtarı (anon)", anonKey) { anonKey = it }
            TextRow("Robot kimliği", robotId) { robotId = it }
            TextRow("Cihaz anahtarı", token) { token = it }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val dirty = url != settings.url || anonKey != settings.anonKey || robotId != settings.robotId || token != settings.deviceToken
                SmallButton("Kaydet", if (dirty) Blue else Ink200, if (dirty) Color.White else Ink600, enabled = dirty) {
                    settings.save(url, anonKey, robotId, token); testResult = null
                }
                SmallButton(if (testing) "Deneniyor…" else "Bağlantıyı test et", Ink50, Ink, enabled = !testing && settings.configured) {
                    testing = true; testResult = null
                    scope.launch {
                        val cfg = sync.fetchConfig()
                        testResult = if (cfg != null) "Bağlantı başarılı — sunucudaki ayar sürümü: ${cfg.version}"
                        else "Bağlantı kurulamadı. Adres, anahtarlar ve ağı kontrol edin."
                        testing = false
                    }
                }
            }
            testResult?.let {
                Text(it, color = if (it.startsWith("Bağlantı başarılı")) Green else Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
            }
            val last = settings.lastSyncAt
            val lastText = if (last == 0L) "hiç" else android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", last).toString()
            Text(
                "Son eşitleme: $lastText · Kuyrukta ${sync.pending} kayıt",
                color = Ink600, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun TextRow(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(label, color = Ink600, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
        BasicTextField(
            value = value,
            onValueChange = { onChange(it.trim()) },
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().height(44.dp).background(Ink50, RoundedCornerShape(10.dp)).border(1.dp, Ink200, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) { inner() }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Section(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).border(1.dp, Ink200, RoundedCornerShape(20.dp)).padding(20.dp)) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Ink600, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))
        content()
    }
}

@Composable
private fun SmallButton(text: String, bg: Color, fg: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp)).background(bg).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun NumberField(value: Int, modifier: Modifier, onChange: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if ((text.toIntOrNull() ?: 0) != value) text = value.toString() }
    BasicTextField(
        value = text,
        onValueChange = { s ->
            val digits = s.filter { it.isDigit() }.take(6)
            text = digits
            onChange(digits.toIntOrNull() ?: 0)
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        decorationBox = { inner ->
            Box(modifier.height(44.dp).background(Ink50, RoundedCornerShape(10.dp)).border(1.dp, Ink200, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) { inner() }
        },
        modifier = modifier
    )
}
