package ch.oliverblaser.dithex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

private val Ink = Color(0xff10110f)
private val Paper = Color(0xffebe7dc)
private val Acid = Color(0xffd4ff45)
private val Panel = Color(0xff191b18)
private val Muted = Color(0xff92978c)

@Composable
fun App(state: DithexState = remember { DithexState() }) {
    val uiState by state.uiState.collectAsState()
    MaterialTheme(colorScheme = darkColorScheme(primary = Acid, surface = Panel, background = Ink)) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
            val compact = maxWidth < 760.dp
            if (compact) {
                Column(Modifier.fillMaxSize()) {
                    Header(uiState, state)
                    Preview(uiState, Modifier.weight(1f).fillMaxWidth())
                    Controls(uiState.settings, uiState.frame, state::updateSettings, Modifier.fillMaxWidth().height(340.dp))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Header(uiState, state)
                    Row(Modifier.fillMaxSize().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Controls(uiState.settings, uiState.frame, state::updateSettings, Modifier.width(280.dp).fillMaxHeight())
                        Preview(uiState, Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(uiState: DithexUiState, state: DithexState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("DITHEX", color = Paper, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("THREE TONES. HEXAGONAL RHYTHM.", color = Muted, fontSize = 9.sp, letterSpacing = 1.4.sp)
        }
        Spacer(Modifier.weight(1f))
        uiState.source?.let {
            Text("${it.width} x ${it.height}", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(14.dp))
        }
        PlatformImagePicker(state::load, state::setError)
    }
}

@Composable
private fun Controls(settings: DitherSettings, frame: HexFrame?, onChange: (DitherSettings) -> Unit, modifier: Modifier) {
    Column(
        modifier.background(Panel, RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(18.dp))
            .padding(18.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("OUTPUT SIGNAL", color = Acid, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(Modifier.height(18.dp))
        ModeSelector(settings.mode) { onChange(settings.copy(mode = it)) }
        Spacer(Modifier.height(20.dp))
        Setting("COLUMNS", settings.columns.toFloat(), 8f..400f, settings.columns.toString()) {
            onChange(settings.copy(columns = it.toInt()))
        }
        Setting("BRIGHTNESS", settings.brightness, -1f..1f, format(settings.brightness)) {
            onChange(settings.copy(brightness = it))
        }
        Setting("CONTRAST", settings.contrast, .2f..3f, format(settings.contrast)) {
            onChange(settings.copy(contrast = it))
        }
        Setting("DETAIL", settings.detail, 0f..2.5f, format(settings.detail)) {
            onChange(settings.copy(detail = it))
        }
        Setting("MIDDLE GRAY", settings.grayLevel, 0.1f..0.9f, "${(settings.grayLevel * 100).toInt()}%") {
            onChange(settings.copy(grayLevel = it))
        }
        PixelCounts(frame)
        Spacer(Modifier.height(16.dp))
        Text(
            "Black / gray ground / white\n${settings.mode.label}",
            color = Muted,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun PixelCounts(frame: HexFrame?) {
    val counts = remember(frame) {
        var black = 0
        var white = 0
        frame?.tones?.forEach {
            when (it.toInt()) {
                0 -> black++
                2 -> white++
            }
        }
        black to white
    }
    Row(
        Modifier.fillMaxWidth().background(Color.White.copy(alpha = .05f), RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PixelCount("BLACK", counts.first, Ink)
        PixelCount("WHITE", counts.second, Color.White)
        PixelCount("TOTAL", counts.first + counts.second, Acid)
    }
}

@Composable
private fun PixelCount(label: String, count: Int, swatch: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(9.dp).height(9.dp).background(swatch, RoundedCornerShape(50)))
        Column {
            Text(label, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(count.toString(), color = Paper, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun ModeSelector(selected: DitherMode, onSelect: (DitherMode) -> Unit) {
    Text("DITHER MODE", color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(9.dp))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DitherMode.entries.forEach { mode ->
            val active = mode == selected
            Button(
                onClick = { onSelect(mode) },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Acid else Color.White.copy(alpha = .06f),
                    contentColor = if (active) Ink else Paper,
                ),
            ) {
                Text(mode.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun Setting(label: String, value: Float, range: ClosedFloatingPointRange<Float>, display: String, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Paper, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = display,
            onValueChange = { text -> text.removeSuffix("%").toFloatOrNull()?.let(onChange) },
            singleLine = true,
            modifier = Modifier.widthIn(min = 68.dp, max = 82.dp),
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Acid, textAlign = TextAlign.End, fontFamily = FontFamily.Monospace),
        )
    }
    Slider(value = value, onValueChange = onChange, valueRange = range)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun Preview(uiState: DithexUiState, modifier: Modifier) {
    val viewportState = remember(uiState.sourceRevision) { HexViewportState() }
    Box(
        modifier.background(Paper, RoundedCornerShape(22.dp)).border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val frame = uiState.frame
        if (frame == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
                Text("DROP COLOR.\nKEEP THE RHYTHM.", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 29.sp)
                Spacer(Modifier.height(10.dp))
                Text("Load an image to translate it into a tightly packed three-tone lattice.", color = Ink.copy(alpha = .55f), textAlign = TextAlign.Center, fontSize = 12.sp)
            }
        } else {
            HexCanvas(
                frame,
                viewportState,
                Modifier.fillMaxSize().padding(22.dp),
                Color(uiState.settings.grayLevel, uiState.settings.grayLevel, uiState.settings.grayLevel),
            )
            if (!viewportState.isReset) {
                Button(
                    onClick = viewportState::reset,
                    modifier = Modifier.align(Alignment.TopStart).padding(18.dp).height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Ink.copy(alpha = .82f),
                        contentColor = Paper,
                    ),
                ) {
                    Text("RESET VIEW  ${(viewportState.zoom * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
        if (uiState.isProcessing) {
            CircularProgressIndicator(Modifier.align(Alignment.TopEnd).padding(18.dp).width(22.dp), color = Ink, strokeWidth = 2.dp)
        }
        uiState.error?.let {
            Text(it, color = Color(0xffd23636), modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp), fontSize = 11.sp)
        }
    }
}

private class HexViewportState {
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
    var viewportSize by mutableStateOf(IntSize.Zero)
    val isReset: Boolean get() = zoom == 1f && pan == Offset.Zero

    fun reset() {
        zoom = 1f
        pan = Offset.Zero
    }
}

@Composable
private fun HexCanvas(frame: HexFrame, viewportState: HexViewportState, modifier: Modifier, background: Color) {
    LaunchedEffect(frame, viewportState.viewportSize) {
        viewportState.pan = clampPan(viewportState.pan, viewportState.zoom, viewportState.viewportSize, frame)
    }
    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val newZoom = (viewportState.zoom * zoomChange).coerceIn(.25f, 12f)
        val appliedZoomChange = newZoom / viewportState.zoom
        val viewportCenter = Offset(viewportState.viewportSize.width * .5f, viewportState.viewportSize.height * .5f)
        val focusFromCenter = centroid - viewportCenter
        val transformedPan =
            viewportState.pan * appliedZoomChange + focusFromCenter * (1f - appliedZoomChange) + panChange
        viewportState.pan = clampPan(transformedPan, newZoom, viewportState.viewportSize, frame)
        viewportState.zoom = newZoom
    }

    Canvas(
        modifier
            .clipToBounds()
            .onSizeChanged { viewportState.viewportSize = it }
            .transformable(
                state = transformState,
                canPan = { viewportState.zoom > 1f },
                lockRotationOnZoomPan = true,
            ),
    ) {
        val scale = min(size.width / frame.width, size.height / frame.height)
        val radius = scale * viewportState.zoom
        val originX = (size.width - frame.width * radius) * .5f + viewportState.pan.x
        val originY = (size.height - frame.height * radius) * .5f + viewportState.pan.y
        drawRect(
            color = background,
            topLeft = Offset(originX, originY),
            size = androidx.compose.ui.geometry.Size(frame.width * radius, frame.height * radius),
        )
        drawHexFrame(frame, originX, originY, radius)
    }
}

private fun clampPan(pan: Offset, zoom: Float, viewport: IntSize, frame: HexFrame): Offset {
    if (zoom <= 1f || viewport == IntSize.Zero) return Offset.Zero

    val fitScale = min(viewport.width / frame.width, viewport.height / frame.height)
    val maxX = max(0f, (frame.width * fitScale * zoom - viewport.width) * .5f)
    val maxY = max(0f, (frame.height * fitScale * zoom - viewport.height) * .5f)
    return Offset(pan.x.coerceIn(-maxX, maxX), pan.y.coerceIn(-maxY, maxY))
}

private fun DrawScope.drawHexFrame(frame: HexFrame, originX: Float, originY: Float, radius: Float) {
    val black = Ink
    val white = Color.White
    var index = 0
    while (index < frame.size) {
        when (frame.tones[index].toInt()) {
            0 -> drawCircle(black, radius, Offset(originX + frame.centersX[index] * radius, originY + frame.centersY[index] * radius))
            2 -> drawCircle(white, radius, Offset(originX + frame.centersX[index] * radius, originY + frame.centersY[index] * radius))
        }
        index++
    }
}

private fun format(value: Float): String = ((value * 100).toInt() / 100f).toString()
