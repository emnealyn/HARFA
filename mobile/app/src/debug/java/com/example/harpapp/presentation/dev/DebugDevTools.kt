package com.example.harpapp.presentation.dev

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.harpapp.device.simulator.FakeInstrumentRepository
import com.example.harpapp.domain.model.Accidental
import com.example.harpapp.domain.model.ConnectionState
import com.example.harpapp.domain.model.InstrumentState
import com.example.harpapp.domain.model.NoteName
import com.example.harpapp.domain.model.OctaveShift
import com.example.harpapp.domain.model.Pitch
import com.example.harpapp.domain.model.StringId
import com.example.harpapp.protocol.BatteryFlags
import com.example.harpapp.protocol.StatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Narzędzia deweloperskie buildu debug. [simulator] jest null, gdy aplikacja używa
 * prawdziwej harfy (przełącznik wyłączony).
 */
class DebugDevTools(
    private val prefs: SimulatorPreferences,
    private val simulator: FakeInstrumentRepository?
) : DevTools {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Stan Compose: przełącznik w Ustawieniach od razu chowa/pokazuje pływające okno. */
    private var floatingStringsEnabled by mutableStateOf(prefs.isFloatingStringsEnabled)

    @Composable
    override fun SettingsEntry(onOpenSimulatorPanel: () -> Unit) {
        val context = LocalContext.current
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Symulator harfy", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Tylko build debug. Zmiana restartuje aplikację.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = simulator != null,
                        onCheckedChange = { enabled ->
                            prefs.setSimulatorEnabled(enabled)
                            context.findActivity()?.restartTask()
                        }
                    )
                }
                if (simulator != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pływające struny w piosence",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = floatingStringsEnabled,
                            onCheckedChange = { enabled ->
                                floatingStringsEnabled = enabled
                                prefs.setFloatingStringsEnabled(enabled)
                            }
                        )
                    }
                    TextButton(onClick = onOpenSimulatorPanel) { Text("Otwórz panel symulatora") }
                }
            }
        }
    }

    @Composable
    override fun SimulatorPanel() {
        val sim = simulator
        if (sim == null) {
            Text(
                "Symulator jest wyłączony. Włącz go w Ustawieniach.",
                modifier = Modifier.padding(16.dp)
            )
            return
        }

        val connection by sim.connectionState.collectAsState(initial = sim.currentConnectionState)
        val deviceName by sim.connectedDeviceName.collectAsState(initial = null)
        val lit by sim.litStrings.collectAsState()
        val log by sim.commandLog.collectAsState()
        val battery by sim.battery.collectAsState()
        val tuning by sim.instrumentState.collectAsState(initial = InstrumentState.DEFAULT)
        var batteryPercent by remember { mutableFloatStateOf(battery.percent.toFloat()) }
        var charging by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Połączenie", style = MaterialTheme.typography.titleMedium)
            Text(
                "Stan: $connection  ·  ${deviceName ?: "-"}  ·  bateria ${battery.percent}%",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PanelToggleButton("Skanuj", selected = true, modifier = Modifier.weight(1f), onClick = {
                    scope.launch { sim.scanForDevices() }
                })
                PanelToggleButton("Połącz", selected = true, modifier = Modifier.weight(1f), onClick = {
                    scope.launch { runCatching { sim.reconnect() } }
                })
                PanelToggleButton("Rozłącz", selected = false, modifier = Modifier.weight(1f), onClick = {
                    sim.disconnect()
                })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PanelToggleButton("Utrata połączenia", selected = false, modifier = Modifier.weight(1f), onClick = {
                    sim.simulateConnectionLoss()
                })
                PanelToggleButton("Błąd przy połączeniu", selected = false, modifier = Modifier.weight(1f), onClick = {
                    sim.failNextConnect(IllegalStateException("Symulowany błąd połączenia"))
                })
            }

            HorizontalDivider()
            Text("Strojenie (przełączniki i transpozycja)", style = MaterialTheme.typography.titleMedium)
            NoteName.entries.forEach { name ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        name.name,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(20.dp)
                    )
                    Accidental.entries.forEach { accidental ->
                        val label = when (accidental) {
                            Accidental.FLAT -> "♭"
                            Accidental.NATURAL -> "♮"
                            Accidental.SHARP -> "♯"
                        }
                        val onClick = {
                            scope.launch { sim.emitConfiguration(tuning.withLever(name, accidental)) }
                            Unit
                        }
                        PanelToggleButton(
                            label = label,
                            selected = tuning.lever(name) == accidental,
                            onClick = onClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Oktawa",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(48.dp)
                )
                OctaveShift.entries.forEach { shift ->
                    val label = when (shift) {
                        OctaveShift.DOWN -> "−1"
                        OctaveShift.NONE -> "0"
                        OctaveShift.UP -> "+1"
                    }
                    val onClick = {
                        scope.launch { sim.emitConfiguration(tuning.copy(octaveShift = shift)) }
                        Unit
                    }
                    PanelToggleButton(
                        label = label,
                        selected = tuning.octaveShift == shift,
                        onClick = onClick,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()
            Text("Struny (podświetlone = dioda LED)", style = MaterialTheme.typography.titleMedium)
            StringGrid(sim = sim, tuning = tuning, lit = lit)

            HorizontalDivider()
            Text("Bateria i błędy", style = MaterialTheme.typography.titleMedium)
            Text("${batteryPercent.toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = batteryPercent, onValueChange = { batteryPercent = it }, valueRange = 0f..100f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = charging, onCheckedChange = { charging = it })
                Text("Ładowanie", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Button(onClick = {
                    val percent = batteryPercent.toInt()
                    var flags = if (charging) BatteryFlags.CHARGING else 0
                    if (percent < LOW_BATTERY_PERCENT) flags = flags or BatteryFlags.LOW
                    scope.launch { sim.emitBatteryState(percent, 3300 + percent * 9, flags) }
                }) { Text("Wyślij stan") }
            }
            OutlinedButton(onClick = {
                scope.launch { sim.emitError(StatusCode.INTERNAL.code, referenceSequence = 0xFF, referenceType = 0) }
            }) { Text("Zgłoś błąd harfy (INTERNAL)") }

            HorizontalDivider()
            Text("Komendy od aplikacji (najnowsze na górze)", style = MaterialTheme.typography.titleMedium)
            if (log.isEmpty()) Text("—")
            log.asReversed().forEach {
                Text(it.toString(), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    override fun FloatingStrings(modifier: Modifier) {
        val sim = simulator ?: return
        if (!floatingStringsEnabled) return

        val connection by sim.connectionState.collectAsState(initial = sim.currentConnectionState)
        val lit by sim.litStrings.collectAsState()
        val tuning by sim.instrumentState.collectAsState(initial = InstrumentState.DEFAULT)

        var expanded by rememberSaveable { mutableStateOf(false) }
        // NaN = jeszcze nie przesuwane → domyślnie prawy dolny róg.
        var posX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
        var posY by rememberSaveable { mutableFloatStateOf(Float.NaN) }

        BoxWithConstraints(modifier) {
            val marginPx = with(LocalDensity.current) { FLOATING_MARGIN.toPx() }
            var contentSize by remember { mutableStateOf(IntSize.Zero) }

            val maxX = (constraints.maxWidth - contentSize.width).toFloat().coerceAtLeast(0f)
            val maxY = (constraints.maxHeight - contentSize.height).toFloat().coerceAtLeast(0f)
            val x = (if (posX.isNaN()) maxX - marginPx else posX).coerceIn(0f, maxX)
            val y = (if (posY.isNaN()) maxY - marginPx else posY).coerceIn(0f, maxY)

            // Gest żyje dłużej niż jedna kompozycja – granice i pozycja startowa przez rememberUpdatedState.
            val currentX by rememberUpdatedState(x)
            val currentY by rememberUpdatedState(y)
            val currentMaxX by rememberUpdatedState(maxX)
            val currentMaxY by rememberUpdatedState(maxY)
            val dragModifier = Modifier.pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    posX = ((if (posX.isNaN()) currentX else posX) + amount.x).coerceIn(0f, currentMaxX)
                    posY = ((if (posY.isNaN()) currentY else posY) + amount.y).coerceIn(0f, currentMaxY)
                }
            }

            Box(
                Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .onSizeChanged { contentSize = it }
            ) {
                if (!expanded) {
                    SmallFloatingActionButton(
                        onClick = { expanded = true },
                        modifier = dragModifier,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Pokaż struny symulatora")
                    }
                } else {
                    Card(
                        modifier = Modifier.width(FLOATING_WIDTH),
                        shape = MaterialTheme.shapes.large,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)) {
                            Row(
                                modifier = dragModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DragIndicator,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Struny symulatora",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { expanded = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Zwiń")
                                }
                            }
                            if (connection == ConnectionState.CONNECTED) {
                                StringGrid(sim = sim, tuning = tuning, lit = lit)
                            } else {
                                Text(
                                    "Harfa niepołączona ($connection)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                PanelToggleButton(
                                    label = "Połącz",
                                    selected = true,
                                    enabled = connection == ConnectionState.DISCONNECTED,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { scope.launch { runCatching { sim.reconnect() } } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /** Siatka 15 strun (3 × 5) z podświetleniem diod i przyciskiem „Zagraj podświetloną”. */
    @Composable
    private fun StringGrid(
        sim: FakeInstrumentRepository,
        tuning: InstrumentState,
        lit: Int,
        modifier: Modifier = Modifier
    ) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StringId.ALL.chunked(STRINGS_PER_ROW).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { id ->
                        val pitch = Pitch.fromMidi(tuning.effectiveMidi(id))
                        PanelToggleButton(
                            label = "${id.index}\n${pitch.name}${pitch.accidental.symbol}${pitch.octave}",
                            selected = lit and (1 shl id.index) != 0,
                            onClick = { scope.launch { sim.pluck(id.index) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            PanelToggleButton(
                label = "Zagraj podświetloną strunę",
                selected = true,
                enabled = lit != 0,
                onClick = { scope.launch { sim.pluckLitString() } },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    private companion object {
        const val LOW_BATTERY_PERCENT = 15
        const val STRINGS_PER_ROW = 5
        val FLOATING_WIDTH = 280.dp
        val FLOATING_MARGIN = 12.dp
    }
}

/**
 * Mały przycisk do gęstych siatek panelu (struny, przełączniki, oktawa):
 * mniejsza czcionka i wąskie marginesy, żeby kilka mieściło się w jednym wierszu.
 */
@Composable
private fun PanelToggleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val padding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
    val content: @Composable RowScope.() -> Unit = {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
    if (selected) {
        Button(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = padding, content = content)
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, enabled = enabled, contentPadding = padding, content = content)
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Activity.restartTask() {
    startActivity(Intent.makeRestartActivityTask(componentName))
    finish()
}