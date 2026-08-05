package com.indianservers.aiexplorer.gamifymaths

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val GameSpace = Color(0xFF06102B)
internal val GamePanel = Color(0xED101B43)
internal val GameInk = Color(0xFFF8FAFF)
internal val GameMuted = Color(0xFFB6C3E5)
internal val GamePurple = Color(0xFF8F42EA)
internal val GameBlue = Color(0xFF2489EB)
internal val GameGreen = Color(0xFF54BE24)
internal val GameGold = Color(0xFFFFB620)
internal val GameRed = Color(0xFFFF503B)
internal val LocalCompactGameLayout = staticCompositionLocalOf { false }

internal data class GameLevel(
    val title: String,
    val subtitle: String,
    val accent: Color,
)

@Composable
internal fun GameScreen(
    title: String,
    level: Int,
    accent: Color,
    hearts: Int,
    onBack: () -> Unit,
    onHint: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                listOf(accent.copy(.28f), GameSpace, Color(0xFF020714)),
                center = Offset(230f, 160f),
                radius = 1050f,
            ),
        ),
    ) {
        val compact = maxHeight < 760.dp || maxWidth < 390.dp
        CompositionLocalProvider(LocalCompactGameLayout provides compact) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(if (compact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            ) {
                GameTopBar(title, level, hearts, accent, onBack, onHint)
                GameLearningPhaseBanner(level, accent)
                content()
                Spacer(Modifier.height(if (compact) 2.dp else 8.dp))
            }
        }
    }
}

@Composable
internal fun GameLearningPhaseBanner(level: Int, accent: Color, prompt: String = "") {
    val guidance = GameLearningCoach.guidance(level, prompt)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(accent.copy(.1f)).border(1.dp, accent.copy(.35f), RoundedCornerShape(14.dp))
            .semantics { contentDescription = "${guidance.phase.label} phase. ${guidance.objective}" }
            .padding(horizontal = 11.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(guidance.phase.label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black)
        Text(guidance.phase.goal, color = GameMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun GameTopBar(
    title: String,
    level: Int,
    hearts: Int,
    accent: Color,
    onBack: () -> Unit,
    onHint: () -> Unit,
) {
    val compact = LocalCompactGameLayout.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RoundGameButton("‹", accent, "Back", onBack)
        Box(
            Modifier.size(if (compact) 34.dp else 38.dp).clip(CircleShape).background(Brush.radialGradient(listOf(accent, accent.copy(.45f))))
                .border(1.dp, Color.White.copy(.55f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(level.toString(), color = GameInk, fontWeight = FontWeight.Black, fontSize = if (compact) 16.sp else 18.sp) }
        Column(Modifier.weight(1f)) {
            Text(title, color = GameInk, fontSize = if (compact) 16.sp else 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
            Text("LEVEL $level", color = accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Text((1..3).joinToString("") { if (it <= hearts) "♥" else "♡" }, color = GameRed, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Black)
        RoundGameButton("?", GameGold, "Show hint", onHint)
    }
}

@Composable
internal fun GameIntroScreen(
    number: Int,
    title: String,
    subtitle: String,
    accent: Color,
    concepts: List<Pair<String, String>>,
    completed: Int,
    total: Int,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onMap: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.3f), GameSpace, Color(0xFF030616))))
            .verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RoundGameButton("‹", accent, "Back to GamifyMaths", onBack)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ScorePill("◆ 1250", GamePurple)
                ScorePill("★ 48", GameGold)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(accent.copy(.42f), Color(0xFF111745), GamePanel)))
                .border(1.dp, accent.copy(.7f), RoundedCornerShape(26.dp)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("$number. ${title.uppercase()}", color = GameInk, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = GameInk, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                concepts.forEach { (symbol, label) ->
                    Column(
                        Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(.18f)).padding(9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(symbol, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(label, color = GameInk, fontSize = 9.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        GlossyPanel(accent) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("WORLD PROGRESS", color = GameInk, fontWeight = FontWeight.Black)
                Text("$completed/$total", color = accent, fontWeight = FontWeight.Black)
            }
            GameProgress(completed / total.toFloat(), accent)
        }
        PrimaryGameButton("Start Adventure", accent, onStart)
        SecondaryGameButton("Open Level Map", accent, onMap)
    }
}

@Composable
internal fun LevelMapScreen(
    worldTitle: String,
    levels: List<GameLevel>,
    unlocked: Int,
    accent: Color,
    onBack: () -> Unit,
    onLevel: (Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Brush.radialGradient(listOf(accent.copy(.26f), GameSpace, Color(0xFF020713))))
            .verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundGameButton("‹", accent, "Back", onBack)
            Column {
                Text(worldTitle, color = GameInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("CHOOSE A LEVEL", color = accent, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.5.sp)
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cardWidth = if (maxWidth >= 900.dp) (maxWidth - 28.dp) / 3f else if (maxWidth >= 600.dp) (maxWidth - 14.dp) / 2f else maxWidth
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                levels.forEachIndexed { index, item ->
                    val enabled = index <= unlocked
                    val learningPhase = GameLearningCoach.phase(index + 1)
                    Row(
                        Modifier.width(cardWidth).heightIn(min = 104.dp).clip(RoundedCornerShape(20.dp))
                            .background(if (enabled) item.accent.copy(.18f) else Color.White.copy(.05f))
                            .border(1.dp, if (enabled) item.accent.copy(.7f) else GameMuted.copy(.22f), RoundedCornerShape(20.dp))
                            .clickable(enabled = enabled) { onLevel(index) }.focusable(enabled).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(if (enabled) item.accent else GameMuted.copy(.22f)), contentAlignment = Alignment.Center) {
                            Text(if (enabled) "${index + 1}" else "LOCK", color = if (enabled) GameSpace else GameMuted, fontWeight = FontWeight.Black)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = if (enabled) GameInk else GameMuted, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            Text(item.subtitle, color = GameMuted, fontSize = 10.sp)
                            Text(learningPhase.label, color = if (enabled) item.accent else GameMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                        Text(if (index < unlocked) "★★★" else if (enabled) "PLAY ›" else "LOCKED", color = if (enabled) GameGold else GameMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
internal fun GlossyPanel(accent: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val compact = LocalCompactGameLayout.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color.White.copy(.09f), accent.copy(.1f), GamePanel)))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(22.dp)).padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp),
        content = content,
    )
}

@Composable
internal fun ScorePill(label: String, accent: Color) {
    Text(
        label, color = GameInk, fontWeight = FontWeight.Black,
        modifier = Modifier.clip(CircleShape).background(GamePanel).border(1.dp, accent.copy(.5f), CircleShape).padding(horizontal = 11.dp, vertical = 7.dp),
    )
}

@Composable
internal fun RoundGameButton(label: String, accent: Color, description: String, onClick: () -> Unit) {
    val compact = LocalCompactGameLayout.current
    Box(
        Modifier.size(if (compact) 38.dp else 42.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(accent, accent.copy(.55f))))
            .border(1.dp, Color.White.copy(.4f), CircleShape).clickable(onClick = onClick).focusable().semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = GameInk, fontSize = if (compact) 17.sp else 19.sp, fontWeight = FontWeight.Black) }
}

@Composable
internal fun PrimaryGameButton(label: String, accent: Color, onClick: () -> Unit, enabled: Boolean = true) {
    val compact = LocalCompactGameLayout.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(if (compact) 48.dp else 54.dp).shadow(if (enabled) 10.dp else 0.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = GameSpace, disabledContainerColor = GameMuted.copy(.25f)),
    ) { Text(label, fontSize = 17.sp, fontWeight = FontWeight.Black) }
}

@Composable
internal fun SecondaryGameButton(label: String, accent: Color, onClick: () -> Unit) {
    val compact = LocalCompactGameLayout.current
    Box(
        Modifier.fillMaxWidth().height(if (compact) 44.dp else 50.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(.12f))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(18.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = GameInk, fontWeight = FontWeight.Black) }
}

@Composable
internal fun GameProgress(progress: Float, accent: Color) {
    Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(Color.Black.copy(.35f))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(9.dp).clip(CircleShape).background(Brush.horizontalGradient(listOf(accent, GameGold))))
    }
}

@Composable
internal fun ResultPanel(correct: Boolean, correctText: String, retryText: String, onContinue: (() -> Unit)? = null) {
    val accent = if (correct) GameGreen else GameRed
    val guidance = GameLearningCoach.guidance(1, "$retryText $correctText")
    GlossyPanel(accent) {
        Text(if (correct) "GREAT — CORRECT!" else "TRY THAT AGAIN", color = accent, fontWeight = FontWeight.Black)
        Text(if (correct) correctText else retryText, color = GameInk, fontSize = 12.sp)
        if (correct) {
            Text("WHY IT WORKS", color = GameGreen, fontSize = 9.sp, fontWeight = FontWeight.Black)
            Text(guidance.reflection, color = GameMuted, fontSize = 11.sp)
        } else {
            Text("RETRY PLAN", color = GameGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            guidance.retrySteps.forEachIndexed { index, step ->
                Text("${index + 1}. $step", color = GameInk, fontSize = 11.sp)
            }
        }
        if (correct && onContinue != null) PrimaryGameButton("Continue", GameGreen, onContinue)
    }
}

@Composable
internal fun DraggableGameTile(
    label: String,
    accent: Color,
    description: String = label,
    onDrop: () -> Unit,
) {
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.size(width = 76.dp, height = 62.dp).graphicsLayer { translationX = dx; translationY = dy }
            .shadow(8.dp, RoundedCornerShape(15.dp)).clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(accent.copy(.9f), accent.copy(.42f), GamePanel)))
            .border(1.dp, Color.White.copy(.35f), RoundedCornerShape(15.dp))
            .pointerInput(label) {
                detectDragGestures(
                    onDragEnd = {
                        val accepted = dy < -28f || kotlin.math.abs(dx) > 50f
                        dx = 0f; dy = 0f
                        if (accepted) onDrop()
                    },
                    onDragCancel = { dx = 0f; dy = 0f },
                ) { change, amount -> change.consume(); dx += amount.x; dy += amount.y }
            }
            .clickable(onClick = onDrop).focusable()
            .semantics { contentDescription = "Drag $description" },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = GameInk, fontSize = 17.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center) }
}

@Composable
internal fun BaseTenVisual(hundreds: Int, tens: Int, ones: Int, modifier: Modifier = Modifier) {
    val compact = LocalCompactGameLayout.current
    Canvas(modifier.height(if (compact) 98.dp else 128.dp).fillMaxWidth()) {
        val green = Color(0xFF65CD31)
        val gold = Color(0xFFFFB61E)
        val blue = Color(0xFF2B8AF0)
        val gap = 7f
        var x = 8f
        repeat(hundreds.coerceAtMost(3)) {
            val side = minOf(86f, size.height - 20f)
            drawRoundRect(green, Offset(x, 12f), Size(side, side), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            repeat(9) { n ->
                val p = (n + 1) * side / 10f
                drawLine(GameSpace.copy(.3f), Offset(x + p, 12f), Offset(x + p, 12f + side), 1f)
                drawLine(GameSpace.copy(.3f), Offset(x, 12f + p), Offset(x + side, 12f + p), 1f)
            }
            x += side + gap
        }
        repeat(tens.coerceAtMost(6)) {
            val w = 13f
            val h = 92f
            drawRoundRect(gold, Offset(x, 12f), Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f))
            repeat(9) { n -> drawLine(GameSpace.copy(.3f), Offset(x, 12f + (n + 1) * h / 10f), Offset(x + w, 12f + (n + 1) * h / 10f), 1f) }
            x += w + 5f
        }
        repeat(ones.coerceAtMost(9)) { index ->
            val col = index % 3
            val row = index / 3
            drawRoundRect(blue, Offset(x + col * 22f, 14f + row * 22f), Size(17f, 17f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
        }
    }
}

@Composable
internal fun BalanceVisual(left: Int, right: Int, accent: Color, modifier: Modifier = Modifier) {
    val compact = LocalCompactGameLayout.current
    Canvas(modifier.fillMaxWidth().height(if (compact) 135.dp else 170.dp)) {
        val delta = (right - left).coerceIn(-4, 4) * 8f
        val center = Offset(size.width / 2f, 72f)
        drawLine(Color(0xFF8DBBFF), Offset(center.x, 68f), Offset(center.x, 145f), 10f)
        drawLine(Color(0xFF5D79B6), Offset(center.x - 52f, 148f), Offset(center.x + 52f, 148f), 12f)
        drawLine(accent, Offset(52f, 65f + delta), Offset(size.width - 52f, 65f - delta), 9f)
        drawCircle(Color(0xFFD5E4FF), 9f, center)
        fun pan(x: Float, y: Float, count: Int) {
            drawLine(Color(0xFFA8C8F8), Offset(x, y), Offset(x, y + 52f), 3f)
            drawArc(Color(0xFF8CB1E8), 0f, 180f, false, Offset(x - 48f, y + 30f), Size(96f, 38f), style = Stroke(5f))
            repeat(count.coerceAtMost(5)) { index ->
                val bx = x - (count.coerceAtMost(5) * 19f) / 2f + index * 20f
                drawRoundRect(if (index % 2 == 0) GameGreen else GameBlue, Offset(bx, y + 24f), Size(17f, 24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f))
            }
        }
        pan(92f, 65f + delta, left)
        pan(size.width - 92f, 65f - delta, right)
    }
}
