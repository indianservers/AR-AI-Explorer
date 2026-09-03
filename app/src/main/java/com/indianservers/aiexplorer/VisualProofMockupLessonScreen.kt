package com.indianservers.aiexplorer

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.core.ProofPlayback
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

private val ProofIvory = Color(0xFFFFFDF9)
private val ProofNavy = Color(0xFF09244D)
private val ProofBlue = Color(0xFF0965A9)
private val ProofCoral = Color(0xFFFF654F)
private val ProofAmber = Color(0xFFFFB31A)
private val ProofCyan = Color(0xFF25C7E8)

/** Pixel-faithful lesson shell for the supplied Android visual-proof mockups. */
@Composable
internal fun VisualProofMockupLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val changeDemonstration = {
        val parameter = playback.frame.lab.parameters.firstOrNull()
        if (parameter == null) {
            onTogglePlaying()
        } else {
            val current = playback.frame.parameters[parameter.name] ?: parameter.initial
            val increment = (parameter.maximum - parameter.minimum) / 10.0
            val next = (current + increment).let {
                if (it > parameter.maximum) parameter.minimum else it
            }
            onParameterChange(parameter.name, next)
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .testTag("visual-proof-route-action")
            .semantics {
                contentDescription = "Visual proof route ${playback.frame.lab.id}"
                onClick(label = "Change demonstration") {
                    changeDemonstration()
                    true
                }
            },
    ) {
    when (playback.frame.lab.id) {
        "triangle-angle-sum" -> TriangleAngleLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep, onParameterChange)
        "pythagorean" -> PythagoreanLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep)
        "triangle-area" -> TriangleAreaLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep, onParameterChange)
        "parallelogram-area" -> ParallelogramLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "trapezoid-area" -> TrapezoidLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep)
        "circle-area" -> CircleAreaLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "polygon-angle-sum" -> PolygonTriangulationLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "similar-triangles" -> SimilarTriangleRatiosLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "intersecting-chords" -> IntersectingChordsLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "circle-angle" -> CircleAnglesLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep, onParameterChange)
        "derivative-slope" -> DerivativeSlopeLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "integral-area" -> IntegralAreaLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep, onParameterChange)
        "epsilon-delta" -> EpsilonDeltaLessonScreen(playback, onBack, onParameterChange)
        "algebra-square" -> BinomialSquareLessonScreen(playback, onBack, onParameterChange)
        "absolute-inequality" -> AbsoluteInequalityLessonScreen(playback, onBack, onParameterChange)
        "equation-balance" -> EquationBalanceLessonScreen(playback, onBack, onSeekStep, onParameterChange)
        "matrix-transform" -> MatrixAreaLessonScreen(playback, onBack, onReset, onParameterChange)
        "eigenvector-direction" -> EigenvectorDirectionLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "odd-sum-square" -> OddNumbersSquareLessonScreen(playback, onBack, onReset, onParameterChange)
        "modular-clock" -> CongruenceClockLessonScreen(playback, onBack, onParameterChange)
        "normal-area" -> NormalProbabilityLessonScreen(playback, onBack, onTogglePlaying, onReset, onParameterChange)
        "anscombe-quartet" -> AnscombeQuartetLessonScreen(playback, onBack, onParameterChange)
        "vector-addition" -> VectorAdditionLessonScreen(playback, onBack, onParameterChange)
        "circle-ratio" -> CircleCircumferenceRatioLessonScreen(playback, onBack, onParameterChange)
        "shear-area" -> ShearPreservesAreaLessonScreen(playback, onBack, onParameterChange)
        "unit-circle-identity" -> UnitCircleIdentityLessonScreen(playback, onBack, onParameterChange)
        "set-de-morgan" -> DeMorganLessonScreen(playback, onBack, onParameterChange)
        "slope-triangle" -> SlopeTrianglesLessonScreen(playback, onBack, onParameterChange)
        "counting-paths" -> CountingPathsLessonScreen(playback, onBack)
        "nt-natural-sum" -> NaturalNumbersSumLessonScreen(playback, onBack, onParameterChange)
        "nt-odd-sum" -> OddNumbersBuildSquaresPatternScreen(playback, onBack, onParameterChange)
        "nt-triangular-numbers" -> TriangularNumbersPatternScreen(playback, onBack, onParameterChange)
        "nt-consecutive-squares" -> ConsecutiveSquaresDifferenceScreen(playback, onBack, onParameterChange)
        "nt-arithmetic-sum" -> ArithmeticSequenceSumScreen(playback, onBack, onParameterChange)
        "nt-even-sum" -> EvenNumbersSumScreen(playback, onBack, onParameterChange)
        "nt-square-odd-difference" -> SquaresConsecutiveOddSumsScreen(playback, onBack, onParameterChange)
        "nt-consecutive-integer-sum" -> ConsecutiveIntegersSumScreen(playback, onBack, onParameterChange)
        "nt-divisibility-2" -> DivisibilityByTwoScreen(playback, onBack)
        "nt-divisibility-3" -> DivisibilityByThreeScreen(playback, onBack)
        "nt-divisibility-4" -> DivisibilityByFourScreen(playback, onBack)
        "nt-divisibility-8" -> DivisibilityByEightScreen(playback, onBack)
        "nt-divisibility-9" -> DivisibilityByNineScreen(playback, onBack)
        "nt-divisibility-11" -> DivisibilityByElevenScreen(playback, onBack)
        "nt-divisibility-5-10" -> DivisibilityByFiveAndTenScreen(playback, onBack)
        "nt-parity-last-digit" -> LastDigitParityScreen(playback, onBack)
        "nt-factor-rectangles" -> FactorPairsRectanglesScreen(playback, onBack)
        "nt-multiples-line" -> MultiplesNumberLineScreen(playback, onBack)
        "nt-prime-building-blocks" -> PrimeFactorBuildingBlocksScreen(playback, onBack)
        "nt-lcm-cycles" -> LcmRepeatingCyclesScreen(playback, onBack)
        "nt-euclidean-algorithm" -> EuclideanAlgorithmScreen(playback, onBack)
        "nt-gcd-grouping" -> GcdLargestGroupingScreen(playback, onBack)
        "nt-gcd-lcm-product" -> GcdLcmRelationshipScreen(playback, onBack)
        "nt-sieve" -> SieveOfEratosthenesScreen(playback, onBack)
        "nt-composite-sqrt" -> CompositeFactorSqrtScreen(playback, onBack)
        "nt-prime-gaps" -> PrimeGapsExplorerScreen(playback, onBack)
        "nt-twin-primes" -> TwinPrimesExplorerScreen(playback, onBack)
        "nt-unique-factorization" -> FundamentalArithmeticScreen(playback, onBack)
        "nt-euclid-primes" -> InfinitelyManyPrimesScreen(playback, onBack)
        "nt-modular-clock" -> ModularArithmeticClockScreen(playback, onBack)
        "nt-modular-addition" -> ModularAdditionScreen(onBack)
        "nt-modular-multiplication" -> ModularMultiplicationScreen(onBack)
        "nt-negative-modulo" -> NegativeModuloScreen(onBack)
        "nt-remainder-classes" -> RemainderClassesScreen(onBack)
        "nt-exponent-product" -> ProductPowersScreen(onBack)
        "nt-exponent-quotient" -> QuotientPowersScreen(onBack)
        "nt-power-of-power" -> PowerOfPowerScreen(onBack)
        "nt-zero-exponent" -> ZeroExponentScreen(onBack)
        "nt-negative-exponent" -> NegativeExponentScreen(onBack)
        "nt-perfect-numbers" -> PerfectNumbersScreen(onBack)
        else -> TriangleAngleLessonScreen(playback, onBack, onTogglePlaying, onReset, onSeekStep, onParameterChange)
    }
    }
}

@Composable
private fun TriangleAngleLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val finalStep = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    val progress = playback.frame.step.toFloat() / finalStep

    Column(
        Modifier
            .fillMaxSize()
            .background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Triangle Angle Sum", "1 / 100", onBack)
        TriangleAngleCanvas(
            playback = playback,
            modifier = Modifier.fillMaxWidth().weight(1f),
            onParameterChange = onParameterChange,
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 52.dp, end = 52.dp, bottom = 42.dp, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            ProofControlButton(
                description = if (playback.playing) "Pause proof animation" else "Play proof animation",
                onClick = onTogglePlaying,
            ) { center, scale ->
                if (playback.playing) {
                    drawLine(ProofNavy, center + Offset(-scale * .25f, -scale * .45f), center + Offset(-scale * .25f, scale * .45f), scale * .17f, StrokeCap.Square)
                    drawLine(ProofNavy, center + Offset(scale * .25f, -scale * .45f), center + Offset(scale * .25f, scale * .45f), scale * .17f, StrokeCap.Square)
                } else {
                    val p = Path().apply {
                        moveTo(center.x - scale * .28f, center.y - scale * .48f)
                        lineTo(center.x + scale * .48f, center.y)
                        lineTo(center.x - scale * .28f, center.y + scale * .48f)
                        close()
                    }
                    drawPath(p, ProofNavy)
                }
            }
            ProofProgressSlider(progress) { onSeekStep((it * finalStep).roundToInt().coerceIn(0, finalStep)) }
            ProofControlButton(description = "Reset proof", onClick = onReset) { center, scale ->
                drawArc(
                    color = ProofNavy,
                    startAngle = -55f,
                    sweepAngle = 285f,
                    useCenter = false,
                    topLeft = center - Offset(scale * .58f, scale * .58f),
                    size = Size(scale * 1.16f, scale * 1.16f),
                    style = Stroke(scale * .12f, cap = StrokeCap.Round),
                )
                val tip = pointOnCircle(center, scale * .58f, -55f)
                val arrow = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(tip.x - scale * .34f, tip.y - scale * .02f)
                    lineTo(tip.x - scale * .10f, tip.y + scale * .27f)
                    close()
                }
                drawPath(arrow, ProofNavy)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ProofProgressSlider(
    progress: Float,
    onProgress: (Float) -> Unit,
) {
    Canvas(
        Modifier
            .weight(1f)
            .height(56.dp)
            .semantics { contentDescription = "Proof animation progress" }
            .pointerInput(onProgress) {
                detectTapGestures { at -> onProgress((at.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(onProgress) {
                detectDragGestures { change, _ ->
                    onProgress((change.position.x / size.width).coerceIn(0f, 1f))
                    change.consume()
                }
            },
    ) {
        val y = size.height / 2f
        val radius = 14.dp.toPx()
        val start = radius
        val end = size.width - radius
        val thumbX = start + (end - start) * progress.coerceIn(0f, 1f)
        drawLine(Color(0xFFD7DCE1), Offset(start, y), Offset(end, y), 8.dp.toPx(), StrokeCap.Round)
        drawLine(ProofNavy, Offset(start, y), Offset(thumbX, y), 8.dp.toPx(), StrokeCap.Round)
        drawCircle(Color.White, radius, Offset(thumbX, y))
        drawCircle(ProofNavy, radius, Offset(thumbX, y), style = Stroke(2.5.dp.toPx()))
    }
}

@Composable
private fun ProofLessonHeader(title: String, index: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(112.dp)) {
        Canvas(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(48.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Back to visual proofs"
                    onClick { onBack(); true }
                }
                .clickable(onClick = onBack),
        ) {
            drawLine(ProofNavy, Offset(size.width * .72f, size.height * .18f), Offset(size.width * .28f, size.height * .5f), 4.dp.toPx(), StrokeCap.Round)
            drawLine(ProofNavy, Offset(size.width * .28f, size.height * .5f), Offset(size.width * .72f, size.height * .82f), 4.dp.toPx(), StrokeCap.Round)
            drawLine(ProofNavy, Offset(size.width * .3f, size.height * .5f), Offset(size.width * .88f, size.height * .5f), 4.dp.toPx(), StrokeCap.Round)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = ProofNavy, fontWeight = FontWeight.Bold, fontSize = if (title.length > 24) 18.sp else if (title.length > 20) 20.sp else 24.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(index, color = if (index == "1 / 100") ProofNavy else Color(0xFF777777), fontSize = 16.sp)
        }
        Canvas(
            Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).size(40.dp).semantics { contentDescription = "More options" },
        ) {
            repeat(3) { index -> drawCircle(ProofNavy, 3.5.dp.toPx(), Offset(size.width / 2f, size.height * (.28f + index * .22f))) }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(Color(0xFFE5E0D8)))
    }
}

@Composable
private fun PythagoreanLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
) {
    val lastStep = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    val progress = playback.frame.step.toFloat() / lastStep
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Pythagorean Rearrangement", "2 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics { contentDescription = "Pythagorean area rearrangement. Four congruent right triangles move between two equal outer squares, proving a squared plus b squared equals c squared." },
        ) {
            val grid = 34.dp.toPx()
            var gx = 0f
            while (gx < size.width) { drawLine(Color(0xFFF0ECE5), Offset(gx, 0f), Offset(gx, size.height), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))); gx += grid }
            var gy = 0f
            while (gy < size.height) { drawLine(Color(0xFFF0ECE5), Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))); gy += grid }
            val square = size.width * .38f
            val top = size.height * .22f
            drawPythagoreanBefore(Offset(size.width * .055f, top), square)
            drawPythagoreanAfter(Offset(size.width * .565f, top), square, progress)
            val dividerX = size.width * .5f
            drawLine(Color(0xFFE3DED5), Offset(dividerX, size.height * .08f), Offset(dividerX, size.height * .72f), 2.dp.toPx())
            drawCircle(Color.White, 27.dp.toPx(), Offset(dividerX, top + square * .5f))
            drawCircle(Color(0xFFE0D9CF), 27.dp.toPx(), Offset(dividerX, top + square * .5f), style = Stroke(1.dp.toPx()))
            drawLabel("‹›", Offset(dividerX, top + square * .5f + 8.dp.toPx()), 24.sp.value, Color(0xFF333333), Paint.Align.CENTER)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 88.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundProofButton("Previous arrangement", icon = { center, scale ->
                drawArc(Color(0xFF333333), 35f, 280f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
                val tip = pointOnCircle(center, scale * .55f, 35f)
                drawLine(Color(0xFF333333), tip, tip + Offset(-scale * .28f, scale * .03f), scale * .12f, StrokeCap.Round)
                drawLine(Color(0xFF333333), tip, tip + Offset(-scale * .02f, scale * .28f), scale * .12f, StrokeCap.Round)
            }, onClick = { onSeekStep((playback.frame.step - 1).coerceAtLeast(0)) })
            RoundProofButton(if (playback.playing) "Pause rearrangement" else "Play rearrangement", large = true, icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF222222), center + Offset(-scale * .22f, -scale * .45f), center + Offset(-scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                    drawLine(Color(0xFF222222), center + Offset(scale * .22f, -scale * .45f), center + Offset(scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                } else {
                    val p = Path().apply { moveTo(center.x - scale * .28f, center.y - scale * .48f); lineTo(center.x + scale * .48f, center.y); lineTo(center.x - scale * .28f, center.y + scale * .48f); close() }
                    drawPath(p, Color(0xFF222222))
                }
            }, onClick = { onTogglePlaying() })
            RoundProofButton("Reset rearrangement", icon = { center, scale ->
                drawArc(Color(0xFF333333), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
                val tip = pointOnCircle(center, scale * .55f, -55f)
                drawLine(Color(0xFF333333), tip, tip + Offset(-scale * .28f, 0f), scale * .12f, StrokeCap.Round)
                drawLine(Color(0xFF333333), tip, tip + Offset(0f, scale * .28f), scale * .12f, StrokeCap.Round)
            }, onClick = { onReset() })
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, bottom = 42.dp, top = 18.dp)
                .shadow(8.dp, RoundedCornerShape(12.dp)).background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE3DDD4), RoundedCornerShape(12.dp)).padding(vertical = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("a²", color = Color(0xFFFFA526), fontSize = 38.sp, fontWeight = FontWeight.Medium)
            Text("  +  ", color = Color(0xFF333333), fontSize = 38.sp)
            Text("b²", color = Color(0xFF5B55D6), fontSize = 38.sp, fontWeight = FontWeight.Medium)
            Text("  =  ", color = Color(0xFF333333), fontSize = 38.sp)
            Text("c²", color = ProofCoral, fontSize = 38.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RoundProofButton(
    description: String,
    large: Boolean = false,
    compact: Boolean = false,
    background: Color = Color.White,
    icon: DrawScope.(Offset, Float) -> Unit,
    onClick: () -> Unit,
) {
    val buttonSize = if (large) 82.dp else if (compact) 52.dp else 64.dp
    Canvas(
        Modifier.size(buttonSize).shadow(9.dp, CircleShape).background(background, CircleShape)
            .border(1.dp, Color(0xFFE0D9CF), CircleShape).clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = description },
    ) { icon(center, min(size.width, size.height) * .40f) }
}

private fun DrawScope.drawPythagoreanBefore(origin: Offset, side: Float) {
    val t = .67f
    val pTop = origin + Offset(side * t, 0f)
    val pRight = origin + Offset(side, side * t)
    val pBottom = origin + Offset(side * (1f - t), side)
    val pLeft = origin + Offset(0f, side * (1f - t))
    fun triangle(a: Offset, b: Offset, c: Offset, color: Color) = drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); close() }, color.copy(alpha = .78f))
    triangle(origin, pTop, pLeft, ProofCoral)
    triangle(origin + Offset(side, 0f), pRight, pTop, ProofAmber)
    triangle(origin + Offset(side, side), pBottom, pRight, Color(0xFF18B8CE))
    triangle(origin + Offset(0f, side), pLeft, pBottom, Color(0xFF6258CE))
    val central = Path().apply { moveTo(pTop.x, pTop.y); lineTo(pRight.x, pRight.y); lineTo(pBottom.x, pBottom.y); lineTo(pLeft.x, pLeft.y); close() }
    drawPath(central, ProofIvory)
    drawPath(central, Color(0xFF303030), style = Stroke(2.dp.toPx()))
    drawRect(Color(0xFF303030), origin, Size(side, side), style = Stroke(2.dp.toPx()))
    drawLabel("c²", origin + Offset(side * .52f, side * .53f), 25.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawLabel("a", origin + Offset(side * .5f, side + 22.dp.toPx()), 18.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawLabel("b", origin + Offset(-15.dp.toPx(), side * .5f), 18.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawCornerNumbers(origin, side)
}

private fun DrawScope.drawPythagoreanAfter(origin: Offset, side: Float, progress: Float) {
    val split = side * .44f
    drawRect(Color.White, origin, Size(side, side))
    drawRect(ProofCoral.copy(alpha = .75f), origin, Size(split, split))
    drawPath(Path().apply { moveTo(origin.x, origin.y + split); lineTo(origin.x + split, origin.y); lineTo(origin.x + split, origin.y + split); close() }, ProofAmber.copy(alpha = .82f))
    drawRect(Color.White, origin + Offset(split, 0f), Size(side - split, split))
    drawRect(Color.White, origin + Offset(0f, split), Size(split, side - split))
    val lower = origin + Offset(split, split)
    drawRect(Color(0xFF20B8CE).copy(alpha = .35f + .45f * progress), lower, Size(side - split, side - split))
    drawPath(Path().apply { moveTo(lower.x, lower.y + side - split); lineTo(lower.x + side - split, lower.y); lineTo(lower.x + side - split, lower.y + side - split); close() }, Color(0xFF6258CE).copy(alpha = .45f + .45f * progress))
    drawLine(Color(0xFF303030), origin + Offset(split, 0f), origin + Offset(split, side), 2.dp.toPx())
    drawLine(Color(0xFF303030), origin + Offset(0f, split), origin + Offset(side, split), 2.dp.toPx())
    drawRect(Color(0xFF303030), origin, Size(side, side), style = Stroke(2.dp.toPx()))
    drawLabel("a²", origin + Offset(split + (side - split) * .5f, split * .55f), 25.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawLabel("b²", origin + Offset(split * .5f, split + (side - split) * .55f), 25.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawLabel("a", origin + Offset(side * .5f, side + 22.dp.toPx()), 18.sp.value, Color(0xFF333333), Paint.Align.CENTER)
    drawCornerNumbers(origin, side)
}

private fun DrawScope.drawCornerNumbers(origin: Offset, side: Float) {
    val colors = listOf(ProofCoral, ProofAmber, Color(0xFF18B8CE), Color(0xFF6258CE))
    val points = listOf(origin, origin + Offset(side, 0f), origin + Offset(side, side), origin + Offset(0f, side))
    points.forEachIndexed { index, point ->
        val n = when (index) { 0 -> point + Offset(-2.dp.toPx(), -10.dp.toPx()); 1 -> point + Offset(2.dp.toPx(), -10.dp.toPx()); 2 -> point + Offset(2.dp.toPx(), 10.dp.toPx()); else -> point + Offset(-2.dp.toPx(), 10.dp.toPx()) }
        drawCircle(colors[index], 11.dp.toPx(), n)
        drawLabel("${index + 1}", n + Offset(0f, 5.dp.toPx()), 11.sp.value, Color.White, Paint.Align.CENTER)
    }
}

@Composable
private fun TriangleAreaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val lastStep = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    val progress = playback.frame.step.toFloat() / lastStep
    val base = playback.frame.parameters["base"] ?: 4.0
    val height = playback.frame.parameters["height"] ?: 3.0
    val apex = playback.frame.parameters["apex"] ?: 1.5
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Triangle Area Dissection", "3 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f).onSizeChanged { canvasSize = it }
                .semantics { contentDescription = "Interactive triangle area dissection. Drag the top vertex to alter the height and apex position while the duplicate pieces form a rectangle of area one half base times height." }
                .pointerInput(canvasSize, base, height, apex) {
                    var dragging = false
                    detectDragGestures(
                        onDragStart = { touch ->
                            val w = canvasSize.width.toFloat(); val h = canvasSize.height.toFloat()
                            val top = Offset(w * (.16f + .66f * (apex / base).toFloat()), h * (.31f - .44f * (height / 6.0).toFloat()))
                            dragging = (touch - top).getDistance() < 78.dp.toPx()
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ ->
                        if (!dragging) return@detectDragGestures
                        val w = canvasSize.width.toFloat().coerceAtLeast(1f); val h = canvasSize.height.toFloat().coerceAtLeast(1f)
                        onParameterChange("apex", (((change.position.x / w - .16f) / .66f) * base).toDouble().coerceIn(-2.0, 6.0))
                        onParameterChange("height", (((.31f - change.position.y / h) / .44f) * 6f).toDouble().coerceIn(.5, 6.0))
                        change.consume()
                    }
                },
        ) {
            val w = size.width; val h = size.height
            val grid = 34.dp.toPx()
            var gx = 0f
            while (gx < w) { drawLine(Color(0xFFF0E8DC), Offset(gx, 0f), Offset(gx, h), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))); gx += grid }
            var gy = 0f
            while (gy < h) { drawLine(Color(0xFFF0E8DC), Offset(0f, gy), Offset(w, gy), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 7f))); gy += grid }

            val left = Offset(w * .16f, h * .30f)
            val right = Offset(w * .82f, h * .30f)
            val top = Offset(w * (.16f + .66f * (apex / base).toFloat()), h * (.31f - .44f * (height / 6.0).toFloat()))
            val triangle = Path().apply { moveTo(left.x, left.y); lineTo(top.x, top.y); lineTo(right.x, right.y); close() }
            drawPath(triangle, Color(0xFFFFEED0).copy(alpha = .46f))
            drawPath(triangle, Color(0xFF252D32), style = Stroke(2.4.dp.toPx()))
            drawLine(ProofCoral, Offset(top.x, top.y + 4.dp.toPx()), Offset(top.x, left.y), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            drawCircle(Color.White, 15.dp.toPx(), Offset(top.x, left.y))
            drawCircle(ProofCoral, 15.dp.toPx(), Offset(top.x, left.y), style = Stroke(2.dp.toPx()))
            drawCircle(ProofCoral, 6.dp.toPx(), Offset(top.x, left.y))
            drawDimensionLine(Offset(left.x, left.y + 31.dp.toPx()), Offset(right.x, right.y + 31.dp.toPx()), ProofBlue, "b", vertical = false)
            drawDimensionLine(Offset(w * .85f, top.y), Offset(w * .85f, left.y), ProofCoral, "h", vertical = true)

            val ghostAlpha = .16f + progress * .25f
            val midY = h * .49f
            val leftGhost = Path().apply { moveTo(w * .18f, midY + h * .11f); lineTo(w * .31f, midY - h * .04f); lineTo(w * .48f, midY - h * .02f); close() }
            val rightGhost = Path().apply { moveTo(w * .82f, midY + h * .11f); lineTo(w * .69f, midY - h * .04f); lineTo(w * .52f, midY - h * .02f); close() }
            drawPath(leftGhost, ProofCoral.copy(alpha = ghostAlpha)); drawPath(rightGhost, ProofCyan.copy(alpha = ghostAlpha))
            drawArc(ProofCoral.copy(alpha = .85f), 190f, 105f, false, topLeft = Offset(w * .13f, h * .40f), size = Size(w * .27f, h * .20f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            drawArc(ProofBlue.copy(alpha = .75f), 245f, 105f, false, topLeft = Offset(w * .60f, h * .40f), size = Size(w * .27f, h * .20f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))

            val rectLeft = w * .16f; val rectTop = h * .64f; val rectRight = w * .82f; val rectBottom = h * .79f
            val rectCenter = (rectLeft + rectRight) / 2f
            val leftPiece = Path().apply { moveTo(rectLeft, rectTop); lineTo(rectCenter, rectTop); lineTo(rectLeft, rectBottom); close() }
            val lowerPiece = Path().apply { moveTo(rectLeft, rectBottom); lineTo(rectCenter, rectTop); lineTo(rectRight, rectBottom); close() }
            val rightPiece = Path().apply { moveTo(rectCenter, rectTop); lineTo(rectRight, rectTop); lineTo(rectRight, rectBottom); close() }
            drawPath(leftPiece, ProofCoral.copy(alpha = .78f)); drawPath(lowerPiece, Color(0xFF159FD1).copy(alpha = .9f)); drawPath(rightPiece, Color(0xFF72CEE0).copy(alpha = .82f))
            drawRect(Color(0xFF252D32), Offset(rectLeft, rectTop), Size(rectRight - rectLeft, rectBottom - rectTop), style = Stroke(2.5.dp.toPx()))
            drawLine(ProofBlue, Offset(rectLeft, rectBottom + 23.dp.toPx()), Offset(rectRight, rectBottom + 23.dp.toPx()), 1.8.dp.toPx())
            drawLabel("b", Offset((rectLeft + rectRight) / 2f, rectBottom + 47.dp.toPx()), 23.sp.value, ProofBlue, Paint.Align.CENTER)
            drawDimensionLine(Offset(w * .85f, rectTop), Offset(w * .85f, rectBottom), ProofCoral, "h⁄2", vertical = true)
            drawRoundRect(Color(0xFFFFFAF1), topLeft = Offset(w * .39f, h * .86f), size = Size(w * .22f, h * .075f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            drawRoundRect(Color(0xFFE5DAC8), topLeft = Offset(w * .39f, h * .86f), size = Size(w * .22f, h * .075f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawLabel("½bh", Offset(w * .5f, h * .91f), 27.sp.value, ProofCoral, Paint.Align.CENTER)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 26.dp, end = 26.dp, bottom = 34.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            ProofControlButton(if (playback.playing) "Pause area dissection" else "Play area dissection", onTogglePlaying) { center, scale ->
                if (playback.playing) {
                    drawLine(ProofNavy, center + Offset(-scale * .25f, -scale * .45f), center + Offset(-scale * .25f, scale * .45f), scale * .17f, StrokeCap.Square)
                    drawLine(ProofNavy, center + Offset(scale * .25f, -scale * .45f), center + Offset(scale * .25f, scale * .45f), scale * .17f, StrokeCap.Square)
                } else {
                    drawPath(Path().apply { moveTo(center.x - scale * .28f, center.y - scale * .48f); lineTo(center.x + scale * .48f, center.y); lineTo(center.x - scale * .28f, center.y + scale * .48f); close() }, ProofNavy)
                }
            }
            ProofProgressSlider(progress) { onSeekStep((it * lastStep).roundToInt().coerceIn(0, lastStep)) }
            ProofControlButton("Reset area dissection", onReset) { center, scale ->
                drawArc(ProofNavy, -55f, 285f, false, center - Offset(scale * .58f, scale * .58f), Size(scale * 1.16f, scale * 1.16f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
                val tip = pointOnCircle(center, scale * .58f, -55f)
                drawLine(ProofNavy, tip, tip + Offset(-scale * .28f, 0f), scale * .12f, StrokeCap.Round)
                drawLine(ProofNavy, tip, tip + Offset(0f, scale * .28f), scale * .12f, StrokeCap.Round)
            }
        }
    }
}

private fun DrawScope.drawDimensionLine(start: Offset, end: Offset, color: Color, label: String, vertical: Boolean) {
    drawLine(color, start, end, 1.6.dp.toPx())
    if (vertical) {
        drawLine(color, start + Offset(-7.dp.toPx(), 0f), start + Offset(7.dp.toPx(), 0f), 1.6.dp.toPx())
        drawLine(color, end + Offset(-7.dp.toPx(), 0f), end + Offset(7.dp.toPx(), 0f), 1.6.dp.toPx())
        drawCircle(color, 4.dp.toPx(), (start + end) / 2f)
        drawLabel(label, (start + end) / 2f + Offset(13.dp.toPx(), 7.dp.toPx()), 21.sp.value, color)
    } else {
        drawLine(color, start + Offset(0f, -7.dp.toPx()), start + Offset(0f, 7.dp.toPx()), 1.6.dp.toPx())
        drawLine(color, end + Offset(0f, -7.dp.toPx()), end + Offset(0f, 7.dp.toPx()), 1.6.dp.toPx())
        drawCircle(color, 4.dp.toPx(), (start + end) / 2f)
        drawLabel(label, (start + end) / 2f + Offset(0f, 23.dp.toPx()), 21.sp.value, color, Paint.Align.CENTER)
    }
}

@Composable
private fun ParallelogramLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val finalStep = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    val progress = playback.frame.step.toFloat() / finalStep
    val shear = playback.frame.parameters["shear"] ?: 1.2
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Parallelogram Cut and Slide", "4 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f).onSizeChanged { canvasSize = it }
                .semantics { contentDescription = "Interactive parallelogram cut and slide proof. Drag the cut handle to change the slant; play moves the triangular end into the gap and preserves base times height." }
                .pointerInput(canvasSize, shear) {
                    var dragging = false
                    detectDragGestures(
                        onDragStart = { touch ->
                            val handle = Offset(canvasSize.width * (.24f + .08f * (shear / 3.0).toFloat()), canvasSize.height * .29f)
                            dragging = (touch - handle).getDistance() < 80.dp.toPx()
                        },
                        onDragEnd = { dragging = false }, onDragCancel = { dragging = false },
                    ) { change, _ ->
                        if (!dragging) return@detectDragGestures
                        val normalized = ((change.position.x / canvasSize.width.toFloat() - .24f) / .08f * 3f).toDouble()
                        onParameterChange("shear", normalized.coerceIn(-3.0, 3.0))
                        change.consume()
                    }
                },
        ) {
            val w = size.width; val h = size.height
            val grid = 42.dp.toPx()
            var gx = 0f
            while (gx < w) { drawLine(Color(0xFFF2EEE8), Offset(gx, 0f), Offset(gx, h), 1.dp.toPx()); gx += grid }
            var gy = 0f
            while (gy < h) { drawLine(Color(0xFFF2EEE8), Offset(0f, gy), Offset(w, gy), 1.dp.toPx()); gy += grid }
            val left = w * .18f; val right = w * .72f; val topY = h * .13f; val bottomY = h * .31f
            val shift = w * (.09f + .035f * (shear / 3.0).toFloat())
            val topLeft = left + shift; val topRight = right + shift
            val para = Path().apply { moveTo(left, bottomY); lineTo(topLeft, topY); lineTo(topRight, topY); lineTo(right, bottomY); close() }
            drawPath(para, Color(0xFFFFF8ED).copy(alpha = .5f)); drawPath(para, Color(0xFF24384A), style = Stroke(3.dp.toPx()))
            val cut = Path().apply { moveTo(left, bottomY); lineTo(topLeft, topY); lineTo(topLeft, bottomY); close() }
            drawPath(cut, ProofCoral.copy(alpha = .78f)); drawLine(Color(0xFF24384A), Offset(topLeft, topY), Offset(topLeft, bottomY), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f)))
            drawCircle(Color.White, 22.dp.toPx(), Offset(topLeft - 28.dp.toPx(), (topY + bottomY) / 2f)); drawLabel("↔", Offset(topLeft - 28.dp.toPx(), (topY + bottomY) / 2f + 7.dp.toPx()), 18.sp.value, ProofCoral, Paint.Align.CENTER)
            drawDimensionLine(Offset(left, bottomY + 30.dp.toPx()), Offset(right, bottomY + 30.dp.toPx()), Color(0xFF1698C9), "b", false)
            drawDimensionLine(Offset(w * .86f, topY), Offset(w * .86f, bottomY), Color(0xFF1698C9), "h", true)

            val moveStart = Offset(left + shift * .48f, h * .46f)
            val moveEnd = Offset(right + shift * .5f, h * .46f)
            val movingCenter = moveStart + (moveEnd - moveStart) * progress
            val triW = shift; val triH = bottomY - topY
            val moving = Path().apply { moveTo(movingCenter.x - triW * .5f, movingCenter.y + triH * .45f); lineTo(movingCenter.x + triW * .5f, movingCenter.y + triH * .45f); lineTo(movingCenter.x + triW * .5f, movingCenter.y - triH * .45f); close() }
            drawPath(moving, ProofCoral.copy(alpha = .75f)); drawPath(moving, Color(0xFF24384A), style = Stroke(2.dp.toPx()))
            val motionPath = Path().apply { moveTo(left, h * .39f); quadraticBezierTo(left, h * .46f, right - 20.dp.toPx(), h * .46f) }
            drawPath(motionPath, Color(0xFF888D94), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f))))
            drawLabel("›", Offset(right - 8.dp.toPx(), h * .465f), 26.sp.value, Color(0xFF888D94), Paint.Align.CENTER)

            val outLeft = w * .22f; val outRight = w * .76f; val outTop = h * .57f; val outBottom = h * .72f
            drawRect(Color(0xFFFFF8ED).copy(alpha = .45f), Offset(outLeft, outTop), Size(outRight - outLeft, outBottom - outTop))
            drawRect(Color(0xFF24384A), Offset(outLeft, outTop), Size(outRight - outLeft, outBottom - outTop), style = Stroke(3.dp.toPx()))
            val wedge = w * .10f
            val placed = Path().apply { moveTo(outRight - wedge, outBottom); lineTo(outRight, outTop); lineTo(outRight, outBottom); close() }
            drawPath(placed, ProofCoral.copy(alpha = .78f)); drawPath(placed, Color(0xFF24384A), style = Stroke(2.dp.toPx()))
            val ghost = Path().apply { moveTo(outLeft - wedge, outBottom); lineTo(outLeft, outTop); lineTo(outLeft, outBottom); close() }
            drawPath(ghost, Color(0xFF888D94), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))))
            drawDimensionLine(Offset(outLeft - wedge, outBottom + 28.dp.toPx()), Offset(outRight, outBottom + 28.dp.toPx()), Color(0xFF1698C9), "b", false)
            drawDimensionLine(Offset(w * .82f, outTop), Offset(w * .82f, outBottom), Color(0xFF1698C9), "h", true)
            drawRoundRect(Color.White, Offset(w * .34f, h * .83f), Size(w * .32f, h * .08f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawLabel("A = bh", Offset(w * .5f, h * .885f), 29.sp.value, Color(0xFF24384A), Paint.Align.CENTER)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 28.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundProofButton(if (playback.playing) "Pause cut and slide" else "Play cut and slide", large = true, icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF40569A), center + Offset(-scale * .22f, -scale * .45f), center + Offset(-scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                    drawLine(Color(0xFF40569A), center + Offset(scale * .22f, -scale * .45f), center + Offset(scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x - scale * .28f, center.y - scale * .48f); lineTo(center.x + scale * .48f, center.y); lineTo(center.x - scale * .28f, center.y + scale * .48f); close() }, Color(0xFF40569A))
            }, onClick = onTogglePlaying)
            RoundProofButton("Reset cut and slide", large = true, icon = { center, scale ->
                drawArc(Color(0xFF40569A), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
        }
    }
}

@Composable
private fun TrapezoidLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
) {
    val last = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    val progress = playback.frame.step.toFloat() / last
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Trapezoid Doubling Proof", "5 / 69", onBack)
        Canvas(Modifier.fillMaxWidth().weight(.43f).semantics { contentDescription = "A trapezoid and its rotating duplicate" }) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val left = w * .12f; val top = h * .23f; val bottom = h * .78f; val topLeft = w * .25f; val topRight = w * .45f; val right = w * .58f
            val trapezoid = Path().apply { moveTo(left, bottom); lineTo(topLeft, top); lineTo(topRight, top); lineTo(right, bottom); close() }
            drawPath(trapezoid, Color(0xFF817BCD).copy(alpha = .48f)); drawPath(trapezoid, Color(0xFF17304F), style = Stroke(2.7.dp.toPx()))
            drawLine(Color(0xFF17304F), Offset(w * .39f, top), Offset(w * .44f, bottom), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f)))
            val ghost = Path().apply { moveTo(w * .59f, bottom); lineTo(w * .67f, top); lineTo(w * .80f, top); lineTo(w * .93f, bottom); close() }
            drawPath(ghost, Color(0xFF817BCD).copy(alpha = .08f + .16f * progress)); drawPath(ghost, Color(0xFF827EB1).copy(alpha = .45f), style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f))))
            drawArc(Color(0xFF777CC5), 205f, 105f, false, Offset(w * .42f, h * .03f), Size(w * .36f, h * .36f), style = Stroke(2.2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            drawCircle(Color.White, 24.dp.toPx(), Offset(w * .54f, h * .51f)); drawCircle(Color(0xFFD7D1C8), 24.dp.toPx(), Offset(w * .54f, h * .51f), style = Stroke(1.dp.toPx())); drawLabel("↻", Offset(w * .54f, h * .525f), 25.sp.value, Color(0xFF5D55C8), Paint.Align.CENTER)
            drawDimensionLine(Offset(topLeft, top - 18.dp.toPx()), Offset(topRight, top - 18.dp.toPx()), Color(0xFF188AC3), "a", false)
            drawDimensionLine(Offset(left, bottom + 17.dp.toPx()), Offset(right, bottom + 17.dp.toPx()), ProofCoral, "b       a", false)
            drawDimensionLine(Offset(w * .085f, top), Offset(w * .085f, bottom), Color(0xFF7E858E), "h", true)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            ProofProgressSlider(progress) { onSeekStep((it * last).roundToInt().coerceIn(0, last)) }
        }
        Canvas(Modifier.fillMaxWidth().weight(.42f).semantics { contentDescription = "Two congruent trapezoids form a parallelogram with base a plus b and height h" }) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val l = w * .12f; val r = w * .88f; val t = h * .18f; val btm = h * .68f; val joinBottom = w * .45f; val joinTop = w * .57f
            val parallelogram = Path().apply { moveTo(l, btm); lineTo(w * .25f, t); lineTo(r, t); lineTo(w * .75f, btm); close() }
            drawPath(parallelogram, Color(0xFF817BCD).copy(alpha = .48f)); drawPath(parallelogram, Color(0xFF17304F), style = Stroke(2.7.dp.toPx()))
            drawLine(Color(0xFF17304F), Offset(joinBottom, btm), Offset(joinTop, t), 2.2.dp.toPx())
            drawLabel("🔒", Offset(joinTop, t + 14.dp.toPx()), 14.sp.value, Color(0xFF3A3C8F), Paint.Align.CENTER)
            drawLabel("🔒", Offset(joinBottom, btm - 5.dp.toPx()), 14.sp.value, Color(0xFF3A3C8F), Paint.Align.CENTER)
            drawDimensionLine(Offset(w * .25f, t - 19.dp.toPx()), Offset(joinTop, t - 19.dp.toPx()), Color(0xFF168FC3), "a", false)
            drawDimensionLine(Offset(joinTop, t - 19.dp.toPx()), Offset(r, t - 19.dp.toPx()), ProofCoral, "b", false)
            drawDimensionLine(Offset(l, btm + 22.dp.toPx()), Offset(w * .75f, btm + 22.dp.toPx()), ProofAmber, "a+b", false)
            drawDimensionLine(Offset(w * .085f, t), Offset(w * .085f, btm), Color(0xFF7E858E), "h", true)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 72.dp).border(1.5.dp, Color(0xFF263A54), RoundedCornerShape(8.dp)).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("A = ½(", color = Color(0xFF252D32), fontSize = 27.sp)
            Text("a", color = Color(0xFF188AC3), fontSize = 27.sp)
            Text("+", color = Color(0xFF252D32), fontSize = 27.sp)
            Text("b", color = ProofCoral, fontSize = 27.sp)
            Text(")h", color = Color(0xFF252D32), fontSize = 27.sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 130.dp, vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundProofButton("Reset trapezoid proof", icon = { center, scale ->
                drawArc(Color(0xFF5A50CE), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
            RoundProofButton(if (playback.playing) "Pause trapezoid proof" else "Play trapezoid proof", large = true, background = Color(0xFF5549D2), icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color.White, center + Offset(-scale * .22f, -scale * .45f), center + Offset(-scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                    drawLine(Color.White, center + Offset(scale * .22f, -scale * .45f), center + Offset(scale * .22f, scale * .45f), scale * .18f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x - scale * .25f, center.y - scale * .45f); lineTo(center.x + scale * .45f, center.y); lineTo(center.x - scale * .25f, center.y + scale * .45f); close() }, Color.White)
            }, onClick = onTogglePlaying)
        }
    }
}

private fun DrawScope.drawSoftGrid() {
    val grid = 38.dp.toPx()
    var x = 0f
    while (x < size.width) { drawLine(Color(0xFFF1ECE5), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += grid }
    var y = 0f
    while (y < size.height) { drawLine(Color(0xFFF1ECE5), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += grid }
}

@Composable
private fun CircleAreaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val sectorCount = (playback.frame.parameters["n"] ?: 12.0).roundToInt().coerceIn(6, 60).let { if (it % 2 == 0) it else it + 1 }
    val progress = playback.frame.step.toFloat() / playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Circle Sectors to Rectangle", "6 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics { contentDescription = "Interactive circle-area proof with $sectorCount alternating sectors rearranged into a strip of base pi r and height r." },
        ) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val circleCenter = Offset(w * .40f, h * .27f)
            val radius = min(w * .255f, h * .19f)
            val sweep = 360f / sectorCount
            repeat(sectorCount) { index ->
                drawArc(
                    if (index % 2 == 0) ProofCoral.copy(alpha = .78f) else Color(0xFF4CBFC1).copy(alpha = .82f),
                    -90f + index * sweep,
                    sweep,
                    true,
                    circleCenter - Offset(radius, radius),
                    Size(radius * 2f, radius * 2f),
                )
                drawLine(Color(0xFF27363B), circleCenter, pointOnCircle(circleCenter, radius, -90f + index * sweep), 1.dp.toPx())
            }
            drawCircle(Color(0xFF27363B), radius, circleCenter, style = Stroke(2.2.dp.toPx()))
            drawDimensionLine(Offset(w * .095f, circleCenter.y - radius), Offset(w * .095f, circleCenter.y + radius), Color(0xFF343E43), "r", true)

            val looseCenterX = w * .76f
            repeat(4) { index ->
                val sectorRadius = radius * .42f
                val y = circleCenter.y - radius * .75f + index * radius * .54f
                val endX = looseCenterX + if (index % 2 == 0) 8.dp.toPx() else -2.dp.toPx()
                val p = Path().apply {
                    moveTo(endX, y)
                    val a1 = if (index % 2 == 0) -80f else -35f
                    val a2 = a1 + sweep.coerceAtMost(36f)
                    lineTo(pointOnCircle(Offset(endX, y), sectorRadius, a1).x, pointOnCircle(Offset(endX, y), sectorRadius, a1).y)
                    lineTo(pointOnCircle(Offset(endX, y), sectorRadius, a2).x, pointOnCircle(Offset(endX, y), sectorRadius, a2).y)
                    close()
                }
                drawPath(p, (if (index % 2 == 0) Color(0xFF4CBFC1) else ProofCoral).copy(alpha = .52f + .35f * progress))
                drawPath(p, Color(0xFF27363B), style = Stroke(1.6.dp.toPx()))
                val trailStart = pointOnCircle(circleCenter, radius, -55f + index * 28f)
                drawLine(if (index % 2 == 0) Color(0xFF14A8AE) else ProofCoral, trailStart, Offset(endX - 18.dp.toPx(), y), 1.6.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f)))
            }
            drawLabel("↓", Offset(w * .49f, h * .54f), 34.sp.value, Color(0xFF7E8588), Paint.Align.CENTER)

            val stripLeft = w * .13f; val stripRight = w * .84f; val stripTop = h * .64f; val stripBottom = h * .78f
            val pieceWidth = (stripRight - stripLeft) / sectorCount
            repeat(sectorCount) { index ->
                fun boundaryTop(i: Int) = stripLeft + i * pieceWidth + if (i in 1 until sectorCount && i % 2 == 0) -pieceWidth * .22f else if (i in 1 until sectorCount) pieceWidth * .22f else 0f
                fun boundaryBottom(i: Int) = stripLeft + i * pieceWidth + if (i in 1 until sectorCount && i % 2 == 0) pieceWidth * .22f else if (i in 1 until sectorCount) -pieceWidth * .22f else 0f
                val up = index % 2 == 0
                val piece = Path().apply {
                    moveTo(boundaryTop(index), stripTop)
                    lineTo(boundaryTop(index + 1), stripTop)
                    lineTo(boundaryBottom(index + 1), stripBottom)
                    lineTo(boundaryBottom(index), stripBottom)
                    close()
                }
                drawPath(piece, (if (up) Color(0xFF4CBFC1) else ProofCoral).copy(alpha = .80f))
                drawPath(piece, Color(0xFF27363B), style = Stroke(.8.dp.toPx()))
            }
            drawDimensionLine(Offset(stripLeft, stripBottom + 29.dp.toPx()), Offset(stripRight, stripBottom + 29.dp.toPx()), Color(0xFF303A3E), "πr", false)
            drawDimensionLine(Offset(w * .89f, stripTop), Offset(w * .89f, stripBottom), Color(0xFF303A3E), "r", true)
            drawRoundRect(Color.White.copy(alpha = .88f), Offset(w * .35f, h * .89f), Size(w * .30f, h * .075f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawRoundRect(Color(0xFFF09A19), Offset(w * .35f, h * .89f), Size(w * .30f, h * .075f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(1.7.dp.toPx()))
            drawLabel("A = πr²", Offset(w * .5f, h * .94f), 27.sp.value, Color(0xFF27363B), Paint.Align.CENTER)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundProofButton("Reset circle sectors", compact = true, icon = { center, scale ->
                drawArc(Color(0xFF26343A), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
            RoundProofButton("Decrease sector count", compact = true, icon = { center, scale -> drawLine(Color(0xFF26343A), center + Offset(-scale * .38f, 0f), center + Offset(scale * .38f, 0f), scale * .1f, StrokeCap.Round) }, onClick = {
                onParameterChange("n", (sectorCount - 2).coerceAtLeast(6).toDouble())
            })
            Text("$sectorCount", color = Color(0xFF172C37), fontSize = 27.sp, fontWeight = FontWeight.Medium)
            RoundProofButton("Increase sector count", compact = true, icon = { center, scale ->
                drawLine(Color(0xFF26343A), center + Offset(-scale * .38f, 0f), center + Offset(scale * .38f, 0f), scale * .1f, StrokeCap.Round)
                drawLine(Color(0xFF26343A), center + Offset(0f, -scale * .38f), center + Offset(0f, scale * .38f), scale * .1f, StrokeCap.Round)
            }, onClick = { onParameterChange("n", (sectorCount + 2).coerceAtMost(60).toDouble()) })
            RoundProofButton(if (playback.playing) "Pause sector rearrangement" else "Play sector rearrangement", compact = true, background = Color(0xFFE4E6EE), icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF293D68), center + Offset(-scale * .22f, -scale * .43f), center + Offset(-scale * .22f, scale * .43f), scale * .17f, StrokeCap.Square)
                    drawLine(Color(0xFF293D68), center + Offset(scale * .22f, -scale * .43f), center + Offset(scale * .22f, scale * .43f), scale * .17f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x - scale * .25f, center.y - scale * .45f); lineTo(center.x + scale * .45f, center.y); lineTo(center.x - scale * .25f, center.y + scale * .45f); close() }, Color(0xFF293D68))
            }, onClick = onTogglePlaying)
            RoundProofButton("Restart sector animation", compact = true, icon = { center, scale ->
                drawArc(Color(0xFF3574B1), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
        }
    }
}

@Composable
private fun PolygonTriangulationLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val sides = (playback.frame.parameters["n"] ?: 6.0).roundToInt().coerceIn(3, 12)
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Polygon Triangulation", "7 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics { contentDescription = "Interactive $sides-sided polygon split from one selected vertex into ${sides - 2} triangles." }
                .pointerInput(sides) {
                    detectTapGestures { tap ->
                        val center = Offset(size.width * .5f, size.height * .91f)
                        val ringRadius = min(size.width, size.height) * .07f
                        val nearest = (0 until 10).minByOrNull { index ->
                            val dot = pointOnCircle(center, ringRadius, -90f + index * 36f)
                            (tap - dot).getDistance()
                        } ?: return@detectTapGestures
                        val dot = pointOnCircle(center, ringRadius, -90f + nearest * 36f)
                        if ((tap - dot).getDistance() < 32.dp.toPx()) onParameterChange("n", (nearest + 3).toDouble())
                    }
                },
        ) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val center = Offset(w * .51f, h * .40f)
            val radius = min(w * .39f, h * .31f)
            val points = List(sides) { index -> pointOnCircle(center, radius, 180f + index * 360f / sides) }
            val anchor = points.first()
            val palette = listOf(Color(0xFFF6B0A5), Color(0xFFFFD887), Color(0xFFB9E5EA), Color(0xFFC9C8F2), Color(0xFFD8E9B7), Color(0xFFF4C5DF))
            for (i in 1 until sides - 1) {
                val triangle = Path().apply { moveTo(anchor.x, anchor.y); lineTo(points[i].x, points[i].y); lineTo(points[i + 1].x, points[i + 1].y); close() }
                drawPath(triangle, palette[(i - 1) % palette.size].copy(alpha = .56f))
                if (i < sides - 2) drawLine(Color(0xFF26373F), anchor, points[i + 1], 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 8f)))
            }
            val outline = Path().apply { points.forEachIndexed { index, p -> if (index == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
            drawPath(outline, Color(0xFF27343A), style = Stroke(3.dp.toPx()))
            points.forEachIndexed { index, p ->
                if (index == 0) {
                    drawCircle(Color.Transparent, 25.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                    drawCircle(ProofCoral.copy(alpha = .12f), 20.dp.toPx(), p)
                    drawCircle(Color.White, 14.dp.toPx(), p)
                    drawCircle(ProofCoral, 14.dp.toPx(), p, style = Stroke(3.dp.toPx()))
                    drawCircle(ProofCoral, 7.dp.toPx(), p)
                } else {
                    drawCircle(Color.White, 11.dp.toPx(), p)
                    drawCircle(Color(0xFF27343A), 11.dp.toPx(), p, style = Stroke(2.5.dp.toPx()))
                }
            }
            val boxW = w * .20f; val boxH = 48.dp.toPx(); val firstY = h * .66f
            drawRoundRect(Color.White.copy(alpha = .9f), Offset(w * .40f, firstY), Size(boxW, boxH), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
            drawRoundRect(ProofCoral, Offset(w * .40f, firstY), Size(boxW, boxH), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            drawLabel("n = $sides", Offset(w * .50f, firstY + 32.dp.toPx()), 23.sp.value, Color(0xFF27343A), Paint.Align.CENTER)
            drawRoundRect(Color(0xFFF4F7FF), Offset(w * .34f, firstY + 62.dp.toPx()), Size(w * .32f, boxH), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
            drawRoundRect(Color(0xFF9BB5F4), Offset(w * .34f, firstY + 62.dp.toPx()), Size(w * .32f, boxH), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            drawLabel("n − 2 = ${sides - 2}", Offset(w * .50f, firstY + 94.dp.toPx()), 23.sp.value, Color(0xFF27343A), Paint.Align.CENTER)

            val ringCenter = Offset(w * .50f, h * .91f); val ringRadius = min(w, h) * .07f
            drawCircle(Color(0xFFC9CDD0), ringRadius, ringCenter, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))))
            repeat(10) { index ->
                val dot = pointOnCircle(ringCenter, ringRadius, -90f + index * 36f)
                val selected = index + 3 == sides
                if (selected) drawCircle(ProofCoral.copy(alpha = .14f), 17.dp.toPx(), dot)
                drawCircle(if (selected) ProofCoral else Color.White, if (selected) 8.dp.toPx() else 6.dp.toPx(), dot)
                drawCircle(if (selected) ProofCoral else Color(0xFF27343A), if (selected) 12.dp.toPx() else 7.dp.toPx(), dot, style = Stroke(2.dp.toPx()))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 82.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundProofButton(if (playback.playing) "Pause triangulation" else "Play triangulation", icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF27343A), center + Offset(-scale * .22f, -scale * .42f), center + Offset(-scale * .22f, scale * .42f), scale * .17f, StrokeCap.Square)
                    drawLine(Color(0xFF27343A), center + Offset(scale * .22f, -scale * .42f), center + Offset(scale * .22f, scale * .42f), scale * .17f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x - scale * .25f, center.y - scale * .45f); lineTo(center.x + scale * .45f, center.y); lineTo(center.x - scale * .25f, center.y + scale * .45f); close() }, Color(0xFF27343A))
            }, onClick = onTogglePlaying)
            Spacer(Modifier.size(80.dp))
            RoundProofButton("Reset polygon", icon = { center, scale ->
                drawArc(Color(0xFF27343A), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
        }
    }
}

@Composable
private fun SimilarTriangleRatiosLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val scaleK = (playback.frame.parameters["k"] ?: 1.5).coerceIn(.25, 3.0)
    val fraction = (scaleK / 3.0).toFloat().coerceIn(.12f, .88f)
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Similar Triangle Ratios", "8 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics {
                    contentDescription = "Interactive similar triangles. Scale factor ${"%.2f".format(scaleK)}; drag segment D E to compare equal corresponding side ratios."
                }
                .pointerInput(scaleK) {
                    detectDragGestures { change, _ ->
                        val normalized = ((change.position.y / size.height) - .08f) / .43f
                        val nextFraction = normalized.coerceIn(.12f, .88f)
                        onParameterChange("k", (nextFraction * 3f).toDouble())
                        change.consume()
                    }
                },
        ) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val apex = Offset(w * .50f, h * .055f)
            val left = Offset(w * .10f, h * .49f)
            val right = Offset(w * .90f, h * .49f)
            val d = apex + (left - apex) * fraction
            val e = apex + (right - apex) * fraction
            val ink = Color(0xFF343640)
            val violet = Color(0xFF5948C5)
            val blue = Color(0xFF25A6D8)

            drawLine(ink, apex, left, 2.7.dp.toPx(), StrokeCap.Round)
            drawLine(ink, apex, right, 2.7.dp.toPx(), StrokeCap.Round)
            drawLine(ink, left, right, 2.7.dp.toPx(), StrokeCap.Round)
            drawLine(violet, d, e, 2.7.dp.toPx(), StrokeCap.Round)

            fun vertex(p: Offset) { drawCircle(ink, 5.5.dp.toPx(), p) }
            listOf(apex, left, right, d, e).forEach(::vertex)
            drawLabel("A", apex + Offset(0f, -13.dp.toPx()), 23.sp.value, ink, Paint.Align.CENTER)
            drawLabel("B", left + Offset(-12.dp.toPx(), 23.dp.toPx()), 22.sp.value, ink, Paint.Align.CENTER)
            drawLabel("C", right + Offset(13.dp.toPx(), 23.dp.toPx()), 22.sp.value, ink, Paint.Align.CENTER)
            drawLabel("D", d + Offset(-16.dp.toPx(), 2.dp.toPx()), 21.sp.value, ink, Paint.Align.CENTER)
            drawLabel("E", e + Offset(16.dp.toPx(), 2.dp.toPx()), 21.sp.value, ink, Paint.Align.CENTER)
            drawLabel("a", Offset(w * .50f, d.y - 19.dp.toPx()), 23.sp.value, violet, Paint.Align.CENTER)

            // Matching-angle marks make the similarity invariant visible while D E moves.
            drawArc(ProofCoral, 54f, 72f, false, apex - Offset(30.dp.toPx(), 30.dp.toPx()), Size(60.dp.toPx(), 60.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(ProofAmber, 305f, 55f, false, d - Offset(25.dp.toPx(), 25.dp.toPx()), Size(50.dp.toPx(), 50.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(ProofAmber, 180f, 55f, false, e - Offset(25.dp.toPx(), 25.dp.toPx()), Size(50.dp.toPx(), 50.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(blue.copy(alpha = .16f), 305f, 55f, true, left - Offset(35.dp.toPx(), 35.dp.toPx()), Size(70.dp.toPx(), 70.dp.toPx()))
            drawArc(blue, 305f, 55f, false, left - Offset(35.dp.toPx(), 35.dp.toPx()), Size(70.dp.toPx(), 70.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(blue.copy(alpha = .16f), 180f, 55f, true, right - Offset(35.dp.toPx(), 35.dp.toPx()), Size(70.dp.toPx(), 70.dp.toPx()))
            drawArc(blue, 180f, 55f, false, right - Offset(35.dp.toPx(), 35.dp.toPx()), Size(70.dp.toPx(), 70.dp.toPx()), style = Stroke(2.dp.toPx()))

            fun ticks(start: Offset, end: Offset, at: Float, color: Color, count: Int) {
                val p = start + (end - start) * at
                val v = end - start
                val len = kotlin.math.sqrt(v.x * v.x + v.y * v.y).coerceAtLeast(1f)
                val normal = Offset(-v.y / len, v.x / len) * 11.dp.toPx()
                val along = Offset(v.x / len, v.y / len) * 6.dp.toPx()
                repeat(count) { i ->
                    val shift = along * (i - (count - 1) / 2f)
                    drawLine(color, p + shift - normal, p + shift + normal, 2.3.dp.toPx())
                }
            }
            ticks(apex, d, .55f, ProofCoral, 1); ticks(apex, e, .55f, ProofCoral, 1)
            ticks(d, left, .48f, blue, 2); ticks(e, right, .48f, blue, 2)

            // Draggable equality handle centered on D E.
            val handle = Offset(w * .50f, d.y)
            drawCircle(Color.White, 19.dp.toPx(), handle)
            drawCircle(violet, 17.dp.toPx(), handle)
            drawCircle(Color.White, 17.dp.toPx(), handle, style = Stroke(2.dp.toPx()))
            drawLabel("=", handle + Offset(0f, 7.dp.toPx()), 20.sp.value, Color.White, Paint.Align.CENTER)

            val bracketY = h * .535f
            drawLine(violet, Offset(w * .10f, bracketY), Offset(w * .90f, bracketY), 2.dp.toPx())
            drawArc(violet, 90f, 90f, false, Offset(w * .10f, bracketY - 18.dp.toPx()), Size(24.dp.toPx(), 24.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawArc(violet, 0f, 90f, false, Offset(w * .90f - 24.dp.toPx(), bracketY - 18.dp.toPx()), Size(24.dp.toPx(), 24.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawLabel("A", Offset(w * .50f, bracketY + 30.dp.toPx()), 23.sp.value, violet, Paint.Align.CENTER)

            val ratioTop = h * .615f
            val colors = listOf(ProofCoral, ProofAmber, blue)
            val names = listOf("a", "b", "c")
            val caps = listOf("A", "B", "C")
            repeat(3) { i ->
                val y = ratioTop + i * h * .083f
                drawLabel(names[i], Offset(w * .18f, y), 22.sp.value, colors[i], Paint.Align.CENTER)
                drawLine(ink, Offset(w * .157f, y + 8.dp.toPx()), Offset(w * .203f, y + 8.dp.toPx()), 1.6.dp.toPx())
                drawLabel(caps[i], Offset(w * .18f, y + 31.dp.toPx()), 22.sp.value, violet, Paint.Align.CENTER)
                drawLabel("=", Offset(w * .265f, y + 15.dp.toPx()), 23.sp.value, ink, Paint.Align.CENTER)
                val barLeft = w * .315f; val barRight = w * .86f
                drawLine(Color(0xFFE8E8E8), Offset(barLeft, y), Offset(barRight, y), 8.dp.toPx(), StrokeCap.Round)
                drawLine(colors[i], Offset(barLeft, y), Offset(barLeft + (barRight - barLeft) * fraction, y), 8.dp.toPx(), StrokeCap.Round)
                drawLine(violet, Offset(barLeft, y + 20.dp.toPx()), Offset(barRight, y + 20.dp.toPx()), 8.dp.toPx(), StrokeCap.Round)
            }

            val formulaTop = h * .875f
            drawRoundRect(Color.White.copy(alpha = .94f), Offset(w * .28f, formulaTop), Size(w * .44f, 64.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
            drawRoundRect(violet, Offset(w * .28f, formulaTop), Size(w * .44f, 64.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()), style = Stroke(1.7.dp.toPx()))
            drawLabel("a/A  =  b/B  =  c/C", Offset(w * .50f, formulaTop + 40.dp.toPx()), 20.sp.value, ink, Paint.Align.CENTER)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 46.dp, vertical = 14.dp)
                .shadow(7.dp, RoundedCornerShape(40.dp)).background(Color.White, RoundedCornerShape(40.dp)).padding(horizontal = 28.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundProofButton("Adjust scale ratio", compact = true, icon = { center, scale ->
                drawLine(Color(0xFF5948C5), center + Offset(-scale * .55f, 0f), center + Offset(scale * .55f, 0f), scale * .10f, StrokeCap.Round)
                drawCircle(Color(0xFF5948C5), scale * .28f, center + Offset(scale * .18f, 0f))
            }, onClick = { onParameterChange("k", if (scaleK >= 2.6) .75 else scaleK + .45) })
            RoundProofButton("Reset similar triangles", compact = true, icon = { center, scale ->
                drawArc(Color(0xFF343640), -55f, 285f, false, center - Offset(scale * .55f, scale * .55f), Size(scale * 1.1f, scale * 1.1f), style = Stroke(scale * .12f, cap = StrokeCap.Round))
            }, onClick = onReset)
            RoundProofButton(if (playback.playing) "Pause similarity animation" else "Play similarity animation", compact = true, icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF343640), center + Offset(-scale * .22f, -scale * .43f), center + Offset(-scale * .22f, scale * .43f), scale * .17f, StrokeCap.Square)
                    drawLine(Color(0xFF343640), center + Offset(scale * .22f, -scale * .43f), center + Offset(scale * .22f, scale * .43f), scale * .17f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x - scale * .25f, center.y - scale * .45f); lineTo(center.x + scale * .45f, center.y); lineTo(center.x - scale * .25f, center.y + scale * .45f); close() }, Color(0xFF343640))
            }, onClick = onTogglePlaying)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IntersectingChordsLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val pValue = (playback.frame.parameters["p"] ?: .2).coerceIn(-.75, .75)
    val angle = (playback.frame.parameters["angle"] ?: 70.0).coerceIn(15.0, 165.0)
    val product1 = playback.frame.measurements["PA×PB"] ?: 0.0
    val product2 = playback.frame.measurements["PC×PD"] ?: 0.0
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Intersecting Chords Theorem", "9 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics {
                    contentDescription = "Interactive intersecting chords. Point P is ${"%.2f".format(pValue)} radii from center and chord angle is ${angle.roundToInt()} degrees; both segment products are ${"%.2f".format(product1)} and ${"%.2f".format(product2)}. Drag P to test the theorem."
                }
                .pointerInput(pValue) {
                    detectDragGestures { change, _ ->
                        val normalized = ((change.position.x / size.width) - .5f) / .34f
                        onParameterChange("p", normalized.coerceIn(-.75f, .75f).toDouble())
                        change.consume()
                    }
                },
        ) {
            drawSoftGrid()
            val w = size.width; val h = size.height
            val circleCenter = Offset(w * .50f, h * .245f)
            val radius = min(w * .385f, h * .215f)
            val p = circleCenter + Offset((pValue * radius).toFloat(), 0f)
            val coral = Color(0xFFFF5F50); val amber = Color(0xFFFFA51B)
            val cyan = Color(0xFF1EB6D7); val violet = Color(0xFF5943C8); val ink = Color(0xFF27343D)

            fun intersections(directionDegrees: Float): Pair<Offset, Offset> {
                val rad = directionDegrees / 180f * PI.toFloat()
                val d = Offset(cos(rad), sin(rad))
                val rel = p - circleCenter
                val dot = rel.x * d.x + rel.y * d.y
                val root = kotlin.math.sqrt((radius * radius - (rel.x * rel.x + rel.y * rel.y) + dot * dot).coerceAtLeast(0f))
                return p + d * (-dot - root) to p + d * (-dot + root)
            }
            val chord1 = intersections(42f)
            val chord2 = intersections(42f - angle.toFloat())
            drawCircle(ProofIvory.copy(alpha = .75f), radius, circleCenter)
            drawCircle(ink, radius, circleCenter, style = Stroke(2.8.dp.toPx()))
            drawLine(coral, chord1.first, p, 3.2.dp.toPx(), StrokeCap.Round)
            drawLine(violet, p, chord1.second, 3.2.dp.toPx(), StrokeCap.Round)
            drawLine(cyan, chord2.first, p, 3.2.dp.toPx(), StrokeCap.Round)
            drawLine(amber, p, chord2.second, 3.2.dp.toPx(), StrokeCap.Round)
            val endpoints = listOf(chord1.first to coral, chord1.second to violet, chord2.first to cyan, chord2.second to amber)
            endpoints.forEach { (point, color) ->
                drawCircle(Color.White, 13.dp.toPx(), point)
                drawCircle(Color(0xFF2C343A).copy(alpha = .16f), 16.dp.toPx(), point)
                drawCircle(color, 8.dp.toPx(), point)
            }
            drawCircle(Color.White, 14.dp.toPx(), p); drawCircle(ink, 9.dp.toPx(), p)
            drawLabel("P", p + Offset(0f, -18.dp.toPx()), 24.sp.value, ink, Paint.Align.CENTER)
            fun segmentLabel(text: String, from: Offset, to: Offset, color: Color) {
                val mid = (from + to) * .5f
                val v = to - from; val len = v.getDistance().coerceAtLeast(1f)
                val normal = Offset(-v.y / len, v.x / len) * 19.dp.toPx()
                drawLabel(text, mid + normal, 24.sp.value, color, Paint.Align.CENTER)
            }
            segmentLabel("a", chord1.first, p, coral); segmentLabel("b", p, chord1.second, violet)
            segmentLabel("c", chord2.first, p, cyan); segmentLabel("d", p, chord2.second, amber)

            // Area models: the two products rearrange into the same rectangle.
            val top = h * .535f; val rectH = h * .105f
            val left1 = w * .085f; val right1 = w * .455f; val left2 = w * .565f; val right2 = w * .855f
            drawRect(coral.copy(alpha = .14f), Offset(left1, top), Size((right1-left1)*.58f, rectH))
            drawRect(violet.copy(alpha = .18f), Offset(left1+(right1-left1)*.58f, top), Size((right1-left1)*.42f, rectH))
            drawRect(ink, Offset(left1, top), Size(right1-left1, rectH), style = Stroke(1.5.dp.toPx()))
            drawDimensionLine(Offset(left1, top-17.dp.toPx()), Offset(left1+(right1-left1)*.58f, top-17.dp.toPx()), coral, "a", false)
            drawDimensionLine(Offset(left1-20.dp.toPx(), top), Offset(left1-20.dp.toPx(), top+rectH), violet, "b", true)
            drawRect(cyan.copy(alpha = .16f), Offset(left2, top), Size((right2-left2)*.52f, rectH))
            drawRect(amber.copy(alpha = .14f), Offset(left2+(right2-left2)*.52f, top), Size((right2-left2)*.48f, rectH))
            drawRect(ink, Offset(left2, top), Size(right2-left2, rectH), style = Stroke(1.5.dp.toPx()))
            drawDimensionLine(Offset(left2, top-17.dp.toPx()), Offset(left2+(right2-left2)*.52f, top-17.dp.toPx()), cyan, "c", false)
            drawDimensionLine(Offset(right2+20.dp.toPx(), top), Offset(right2+20.dp.toPx(), top+rectH), amber, "d", true)
            drawLabel("≫", Offset(w*.515f, top+rectH*.58f), 29.sp.value, Color(0xFFB9B9B9), Paint.Align.CENTER)

            val finalLeft = w * .315f; val finalRight = w * .685f; val finalTop = h * .695f; val finalH = h * .10f
            drawRect(violet.copy(alpha = .22f), Offset(finalLeft, finalTop), Size((finalRight-finalLeft)*.5f, finalH))
            drawRect(cyan.copy(alpha = .22f), Offset((finalLeft+finalRight)*.5f, finalTop), Size((finalRight-finalLeft)*.5f, finalH))
            drawRect(coral, Offset(finalLeft-8.dp.toPx(), finalTop-8.dp.toPx()), Size(finalRight-finalLeft+16.dp.toPx(), finalH+16.dp.toPx()), style = Stroke(1.5.dp.toPx()))
            drawRect(cyan, Offset(finalLeft, finalTop), Size(finalRight-finalLeft, finalH), style = Stroke(1.5.dp.toPx()))
            drawLabel("a · b  =  c · d", Offset(w*.50f, h*.855f), 34.sp.value, ink, Paint.Align.CENTER)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundProofButton("Move intersection point", compact = true, icon = { center, scale ->
                drawLine(ProofCoral, center+Offset(-scale*.55f,0f), center+Offset(scale*.55f,0f), scale*.1f, StrokeCap.Round)
                drawLine(ProofCoral, center+Offset(0f,-scale*.55f), center+Offset(0f,scale*.55f), scale*.1f, StrokeCap.Round)
            }, onClick = { onParameterChange("p", if (pValue > .55) -.45 else pValue + .25) })
            ProofProgressSlider(((angle - 15.0) / 150.0).toFloat()) { onParameterChange("angle", 15.0 + it * 150.0) }
            RoundProofButton("Reset intersecting chords", compact = true, icon = { center, scale ->
                drawArc(Color(0xFF27343D), -55f, 285f, false, center-Offset(scale*.55f,scale*.55f), Size(scale*1.1f,scale*1.1f), style=Stroke(scale*.12f, cap=StrokeCap.Round))
            }, onClick = onReset)
            RoundProofButton(if (playback.playing) "Pause chord animation" else "Play chord animation", compact = true, icon = { center, scale ->
                if (playback.playing) {
                    drawLine(Color(0xFF27343D), center+Offset(-scale*.22f,-scale*.43f), center+Offset(-scale*.22f,scale*.43f), scale*.17f, StrokeCap.Square)
                    drawLine(Color(0xFF27343D), center+Offset(scale*.22f,-scale*.43f), center+Offset(scale*.22f,scale*.43f), scale*.17f, StrokeCap.Square)
                } else drawPath(Path().apply { moveTo(center.x-scale*.25f,center.y-scale*.45f); lineTo(center.x+scale*.45f,center.y); lineTo(center.x-scale*.25f,center.y+scale*.45f); close() }, Color(0xFF27343D))
            }, onClick = onTogglePlaying)
        }
    }
}

@Composable
private fun CircleAnglesLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val arc = (playback.frame.parameters["arc"] ?: 80.0).coerceIn(20.0, 160.0)
    val cPosition = (playback.frame.parameters["c"] ?: 250.0).coerceIn(185.0, 270.0)
    val centerAngle = playback.frame.measurements["measured center angle"] ?: arc
    val circumferenceAngle = playback.frame.measurements["measured circumference angle"] ?: arc / 2.0
    val lastStep = playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    Column(
        Modifier.fillMaxSize().background(ProofIvory)
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        ProofLessonHeader("Center and Circumference Angles", "10 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics {
                    contentDescription = "Interactive circle angle theorem, proof step ${playback.frame.step + 1}. Circumference angle is ${circumferenceAngle.roundToInt()} degrees and center angle is ${centerAngle.roundToInt()} degrees, exactly twice as large. Drag point C or use the chord slider."
                }
                .pointerInput(cPosition) {
                    detectDragGestures { change, _ ->
                        if (change.position.y > size.height * .78f) {
                            val t = ((change.position.x / size.width) - .105f) / .79f
                            onParameterChange("arc", 20.0 + t.coerceIn(0f, 1f) * 140.0)
                        } else {
                            val center = Offset(size.width * .50f, size.height * .34f)
                            val degrees = Math.toDegrees(atan2((change.position.y-center.y).toDouble(), (change.position.x-center.x).toDouble())).toFloat()
                            val wrapped = if (degrees < 0f) degrees + 360f else degrees
                            val mapped = 185.0 + ((wrapped.coerceIn(195f, 280f)-195f)/85f)*85.0
                            onParameterChange("c", mapped.coerceIn(185.0,270.0))
                        }
                        change.consume()
                    }
                },
        ) {
            drawSoftGrid()
            val w=size.width; val h=size.height; val ink=Color(0xFF263743); val coral=Color(0xFFFF6255); val cyan=Color(0xFF12B8D2)
            val center=Offset(w*.50f,h*.34f); val radius=min(w*.43f,h*.285f)
            val a=pointOnCircle(center,radius,(90f+arc.toFloat()/2f))
            val b=pointOnCircle(center,radius,(90f-arc.toFloat()/2f))
            val cDegrees=(-165f+((cPosition-185.0)/85.0*85.0).toFloat())
            val c=pointOnCircle(center,radius,cDegrees)
            drawCircle(ink,radius,center,style=Stroke(2.7.dp.toPx()))
            // Highlight the major intercepted arc in coral.
            drawArc(coral,90f+arc.toFloat()/2f,360f-arc.toFloat(),false,center-Offset(radius,radius),Size(radius*2,radius*2),style=Stroke(3.4.dp.toPx(),cap=StrokeCap.Round))
            drawLine(coral,c,a,2.7.dp.toPx()); drawLine(coral,c,b,2.7.dp.toPx())
            drawLine(ink,center,a,2.7.dp.toPx()); drawLine(ink,center,b,2.7.dp.toPx())
            val centralRadius=58.dp.toPx()
            drawArc(cyan.copy(alpha=.13f),90f-arc.toFloat()/2f,arc.toFloat(),true,center-Offset(centralRadius,centralRadius),Size(centralRadius*2,centralRadius*2))
            drawArc(cyan,90f-arc.toFloat()/2f,arc.toFloat(),false,center-Offset(centralRadius,centralRadius),Size(centralRadius*2,centralRadius*2),style=Stroke(2.5.dp.toPx()))
            drawArc(cyan,90f-arc.toFloat()/2f,arc.toFloat(),false,center-Offset(centralRadius*.78f,centralRadius*.78f),Size(centralRadius*1.56f,centralRadius*1.56f),style=Stroke(2.dp.toPx()))
            val inscribedRadius=46.dp.toPx()
            val ca=atan2((a.y-c.y).toDouble(),(a.x-c.x).toDouble()).toFloat()*180f/PI.toFloat()
            val cb=atan2((b.y-c.y).toDouble(),(b.x-c.x).toDouble()).toFloat()*180f/PI.toFloat()
            var sweep=(cb-ca+360f)%360f; if(sweep>180f) sweep-=360f
            drawArc(coral.copy(alpha=.14f),ca,sweep,true,c-Offset(inscribedRadius,inscribedRadius),Size(inscribedRadius*2,inscribedRadius*2))
            drawArc(coral,ca,sweep,false,c-Offset(inscribedRadius,inscribedRadius),Size(inscribedRadius*2,inscribedRadius*2),style=Stroke(2.2.dp.toPx()))
            listOf(a,b,c,center).forEach { drawCircle(ink,9.dp.toPx(),it) }
            drawLabel("A",a+Offset(-23.dp.toPx(),22.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER)
            drawLabel("B",b+Offset(23.dp.toPx(),22.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER)
            drawLabel("C",c+Offset(0f,-16.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER)
            drawLabel("O",center+Offset(0f,-17.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER)
            drawLabel("θ",c+Offset(0f,70.dp.toPx()),27.sp.value,coral,Paint.Align.CENTER)
            drawLabel("2θ",center+Offset(0f,77.dp.toPx()),27.sp.value,cyan,Paint.Align.CENTER)

            val sliderY=h*.89f; val left=w*.105f; val right=w*.895f; val t=((arc-20.0)/140.0).toFloat()
            drawLine(ink,Offset(left,sliderY),Offset(right,sliderY),5.dp.toPx(),StrokeCap.Round)
            drawLine(coral,Offset(left,sliderY),Offset(left+(right-left)*t,sliderY),5.dp.toPx(),StrokeCap.Round)
            drawCircle(coral,7.dp.toPx(),Offset(left,sliderY)); drawCircle(ink,6.dp.toPx(),Offset(right,sliderY))
            val thumb=Offset(left+(right-left)*t,sliderY); drawCircle(Color.White,20.dp.toPx(),thumb); drawCircle(coral,13.dp.toPx(),thumb)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=28.dp,vertical=18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            RoundProofButton("Change chord endpoints",compact=true,icon={center,scale ->
                drawArc(ProofCoral,195f,150f,false,center-Offset(scale*.65f,scale*.65f),Size(scale*1.3f,scale*1.3f),style=Stroke(scale*.11f)); drawCircle(ProofCoral,scale*.16f,center+Offset(-scale*.55f,scale*.23f)); drawCircle(ProofCoral,scale*.16f,center+Offset(scale*.55f,scale*.23f))
            },onClick={onParameterChange("arc",if(arc>=145) 40.0 else arc+20.0)})
            RoundProofButton("Previous circle-angle step",compact=true,icon={center,scale -> drawLine(Color(0xFF263743),center+Offset(-scale*.42f,-scale*.45f),center+Offset(-scale*.42f,scale*.45f),scale*.11f); drawPath(Path().apply{moveTo(center.x+scale*.36f,center.y-scale*.45f);lineTo(center.x-scale*.24f,center.y);lineTo(center.x+scale*.36f,center.y+scale*.45f);close()},Color(0xFF263743))},onClick={onSeekStep((playback.frame.step-1).coerceAtLeast(0))})
            RoundProofButton(if(playback.playing)"Pause circle-angle animation" else "Play circle-angle animation",large=true,background=Color(0xFF5861E8),icon={center,scale -> if(playback.playing){drawLine(Color.White,center+Offset(-scale*.22f,-scale*.43f),center+Offset(-scale*.22f,scale*.43f),scale*.17f);drawLine(Color.White,center+Offset(scale*.22f,-scale*.43f),center+Offset(scale*.22f,scale*.43f),scale*.17f)}else drawPath(Path().apply{moveTo(center.x-scale*.25f,center.y-scale*.45f);lineTo(center.x+scale*.45f,center.y);lineTo(center.x-scale*.25f,center.y+scale*.45f);close()},Color.White)},onClick=onTogglePlaying)
            RoundProofButton("Next circle-angle step",compact=true,icon={center,scale -> drawLine(Color(0xFF263743),center+Offset(scale*.42f,-scale*.45f),center+Offset(scale*.42f,scale*.45f),scale*.11f); drawPath(Path().apply{moveTo(center.x-scale*.36f,center.y-scale*.45f);lineTo(center.x+scale*.24f,center.y);lineTo(center.x-scale*.36f,center.y+scale*.45f);close()},Color(0xFF263743))},onClick={onSeekStep((playback.frame.step+1).coerceAtMost(lastStep))})
            RoundProofButton("Reset circle angles",compact=true,icon={center,scale -> drawArc(Color(0xFF263743),-55f,285f,false,center-Offset(scale*.55f,scale*.55f),Size(scale*1.1f,scale*1.1f),style=Stroke(scale*.12f,cap=StrokeCap.Round))},onClick=onReset)
        }
    }
}

@Composable
private fun DerivativeSlopeLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val xValue=(playback.frame.parameters["x"]?:2.0).coerceIn(-4.0,4.0)
    val hValue=(playback.frame.parameters["h"]?:1.0).coerceIn(.001,2.0)
    val secant=playback.frame.measurements["secant slope"]?: (2*xValue+hValue)
    val tangent=playback.frame.measurements["tangent slope"]?:2*xValue
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Derivative as Slope","11 / 69",onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f).semantics {
                contentDescription="Interactive derivative graph at x ${"%.2f".format(xValue)} and h ${"%.3f".format(hValue)}. Secant slope ${"%.3f".format(secant)} approaches tangent slope ${"%.3f".format(tangent)}. Drag the graph point or h slider."
            }.pointerInput(xValue) {
                detectDragGestures { change,_ ->
                    val mapped=-4.0+(change.position.x/size.width)*8.0
                    onParameterChange("x",mapped.coerceIn(-4.0,4.0)); change.consume()
                }
            },
        ) {
            drawSoftGrid(); val w=size.width; val h=size.height; val ink=Color(0xFF27343D); val coral=Color(0xFFFF604D); val violet=Color(0xFF5C4BD1); val cyan=Color(0xFF22B8D2)
            val graphTop=h*.05f; val graphBottom=h*.58f; val origin=Offset(w*.13f,graphBottom); val xScale=w*.12f; val yScale=(graphBottom-graphTop)/20f
            fun plotX(v:Double)=origin.x+v.toFloat()*xScale
            fun plotY(v:Double)=origin.y-(v.toFloat()*yScale)
            drawLine(ink,Offset(w*.04f,origin.y),Offset(w*.96f,origin.y),1.7.dp.toPx()); drawLine(ink,Offset(origin.x,graphTop),Offset(origin.x,graphBottom+45.dp.toPx()),1.7.dp.toPx())
            drawPath(Path().apply{moveTo(w*.945f,origin.y);lineTo(w*.915f,origin.y-9.dp.toPx());lineTo(w*.915f,origin.y+9.dp.toPx());close()},ink)
            drawPath(Path().apply{moveTo(origin.x,graphTop);lineTo(origin.x-9.dp.toPx(),graphTop+18.dp.toPx());lineTo(origin.x+9.dp.toPx(),graphTop+18.dp.toPx());close()},ink)
            val curve=Path(); var first=true
            var t=-1.0; while(t<=6.6){val px=plotX(t);val py=plotY(t*t/2.0);if(first){curve.moveTo(px,py);first=false}else curve.lineTo(px,py);t+=.04}
            drawPath(curve,ink,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
            val p1=Offset(plotX(xValue),plotY(xValue*xValue/2.0)); val x2=xValue+hValue; val p2=Offset(plotX(x2),plotY(x2*x2/2.0))
            drawLine(coral,p1,p2,3.dp.toPx())
            val tangentVector=Offset(1f,-tangent.toFloat()*yScale/xScale)
            val tangentLength=tangentVector.getDistance().coerceAtLeast(1f)
            val tangentDelta=tangentVector/tangentLength*(w*.28f)
            drawLine(cyan,p1-tangentDelta,p1+tangentDelta,2.2.dp.toPx())
            repeat(7){i->val q=p1+(p2-p1)*(i/7f);drawLine(coral.copy(alpha=.12f),p1,q,1.dp.toPx())}
            drawLine(ink,p1,Offset(p1.x,origin.y),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
            drawLine(ink,p2,Offset(p2.x,origin.y),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
            drawLine(ink,p1,Offset(p2.x,p1.y),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,8f)))
            drawCircle(Color.White,11.dp.toPx(),p1);drawCircle(violet,8.dp.toPx(),p1);drawCircle(Color.White,11.dp.toPx(),p2);drawCircle(coral,8.dp.toPx(),p2)
            drawCircle(violet,7.dp.toPx(),Offset(p1.x,origin.y));drawCircle(coral,7.dp.toPx(),Offset(p2.x,origin.y))
            drawLabel("x",Offset(p1.x,origin.y+29.dp.toPx()),22.sp.value,violet,Paint.Align.CENTER);drawLabel("x+h",Offset(p2.x,origin.y+29.dp.toPx()),22.sp.value,coral,Paint.Align.CENTER)
            drawDimensionLine(Offset(p1.x,origin.y+42.dp.toPx()),Offset(p2.x,origin.y+42.dp.toPx()),ink,"Δx",false)
            drawDimensionLine(Offset(p2.x+18.dp.toPx(),p1.y),Offset(p2.x+18.dp.toPx(),p2.y),coral,"Δy",true)
            val cardTop=h*.71f; val cardH=74.dp.toPx(); val cardW=w*.27f
            fun card(left:Float,color:Color,label:String,value:String){drawRoundRect(color.copy(alpha=.055f),Offset(left,cardTop),Size(cardW,cardH),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(color.copy(alpha=.3f),Offset(left,cardTop),Size(cardW,cardH),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel(label,Offset(left+cardW*.5f,cardTop+30.dp.toPx()),19.sp.value,color,Paint.Align.CENTER);drawLabel(value,Offset(left+cardW*.5f,cardTop+58.dp.toPx()),15.sp.value,ink,Paint.Align.CENTER)}
            card(w*.055f,coral,"Δy / Δx","${"%.2f".format(secant)}");card(w*.365f,cyan,"h → 0","${"%.3f".format(hValue)}");card(w*.675f,violet,"f′(x)","${"%.2f".format(tangent)}")
        }
        Column(Modifier.fillMaxWidth().padding(horizontal=30.dp).background(Color.White,RoundedCornerShape(12.dp)).border(1.dp,Color(0xFFE2DED8),RoundedCornerShape(12.dp)).padding(horizontal=14.dp,vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("h → 0",color=Color(0xFF22AFC6),fontSize=19.sp)
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("−",fontSize=30.sp,color=Color(0xFF27343D))
                Canvas(Modifier.weight(1f).height(42.dp).semantics{contentDescription="h value slider"}.pointerInput(hValue){detectTapGestures{at->onParameterChange("h",(.001+(at.x/size.width)*1.999).coerceIn(.001,2.0))}}.pointerInput(hValue){detectDragGestures{change,_->onParameterChange("h",(.001+(change.position.x/size.width)*1.999).coerceIn(.001,2.0));change.consume()}}){val y=size.height/2;val t=((hValue-.001)/1.999).toFloat();drawLine(Color(0xFFD9DDE0),Offset(8.dp.toPx(),y),Offset(size.width-8.dp.toPx(),y),5.dp.toPx(),StrokeCap.Round);drawLine(ProofCoral,Offset(8.dp.toPx(),y),Offset(8.dp.toPx()+(size.width-16.dp.toPx())*t,y),5.dp.toPx(),StrokeCap.Round);drawCircle(Color.White,15.dp.toPx(),Offset(8.dp.toPx()+(size.width-16.dp.toPx())*t,y));drawCircle(ProofCoral,12.dp.toPx(),Offset(8.dp.toPx()+(size.width-16.dp.toPx())*t,y))}
                Text("+",fontSize=30.sp,color=Color(0xFF27343D))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=68.dp,vertical=18.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            ProofControlButton("Move derivative point",{onParameterChange("x",if(xValue>=3.5)-2.0 else xValue+.5)}){center,scale->drawCircle(Color(0xFF5C4BD1),scale*.26f,center);drawLine(Color(0xFF5C4BD1),center+Offset(0f,-scale*.6f),center,scale*.10f)}
            ProofControlButton("Reset derivative proof",onReset){center,scale->drawArc(Color(0xFF5C4BD1),-55f,285f,false,center-Offset(scale*.58f,scale*.58f),Size(scale*1.16f,scale*1.16f),style=Stroke(scale*.12f,cap=StrokeCap.Round))}
            ProofControlButton(if(playback.playing)"Pause derivative animation" else "Play derivative animation",onTogglePlaying){center,scale->if(playback.playing){drawLine(Color(0xFF5C4BD1),center+Offset(-scale*.22f,-scale*.43f),center+Offset(-scale*.22f,scale*.43f),scale*.17f);drawLine(Color(0xFF5C4BD1),center+Offset(scale*.22f,-scale*.43f),center+Offset(scale*.22f,scale*.43f),scale*.17f)}else drawPath(Path().apply{moveTo(center.x-scale*.25f,center.y-scale*.45f);lineTo(center.x+scale*.45f,center.y);lineTo(center.x-scale*.25f,center.y+scale*.45f);close()},Color(0xFF5C4BD1))}
        }
    }
}

@Composable
private fun IntegralAreaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val n=(playback.frame.parameters["n"]?:24.0).roundToInt().coerceIn(4,80)
    val b=(playback.frame.parameters["b"]?:3.0).coerceIn(.5,5.0)
    val area=playback.frame.measurements["midpoint rectangle sum"]?:0.0
    val exact=playback.frame.measurements["exact area"]?:b*b*b/3.0
    val lastStep=playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Integral as Accumulated Area","12 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Interactive accumulated area using $n midpoint rectangles from zero to ${"%.2f".format(b)}. Approximation ${"%.3f".format(area)}, exact area ${"%.3f".format(exact)}."}
            .pointerInput(n,b){detectTapGestures{at->val y=at.y/size.height;if(y in .61f.. .70f)onParameterChange("n",(4+(((at.x/size.width)-.18f)/.73f).coerceIn(0f,1f)*76).roundToInt().toDouble())else if(y in .70f.. .79f)onParameterChange("b",.5+(((at.x/size.width)-.18f)/.73f).coerceIn(0f,1f)*4.5)}}
            .pointerInput(n,b){detectDragGestures{change,_->val y=change.position.y/size.height;if(y in .61f.. .70f)onParameterChange("n",(4+(((change.position.x/size.width)-.18f)/.73f).coerceIn(0f,1f)*76).roundToInt().toDouble())else if(y in .70f.. .79f)onParameterChange("b",.5+(((change.position.x/size.width)-.18f)/.73f).coerceIn(0f,1f)*4.5);change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF27343D);val coral=Color(0xFFFF6754);val violet=Color(0xFF594ACB);val cyan=Color(0xFF2CB2C5)
            val origin=Offset(w*.10f,h*.54f);val graphRight=w*.92f;val top=h*.06f;val usable=graphRight-origin.x
            drawLine(ink,Offset(w*.045f,origin.y),Offset(w*.96f,origin.y),1.8.dp.toPx());drawLine(ink,Offset(origin.x,top),Offset(origin.x,origin.y+35.dp.toPx()),1.8.dp.toPx())
            val bX=origin.x+usable*(b/5.0).toFloat();val yMax=25.0;fun yOf(x:Double)=origin.y-(x*x/yMax*(origin.y-top)*.82).toFloat()
            val rectW=(bX-origin.x)/n
            repeat(n){i->val x0=origin.x+i*rectW;val mid=(i+.5)*b/n;val y=yOf(mid);val hue=i.toFloat()/(n-1).coerceAtLeast(1);val color=when{hue<.33f->Color(0xFFFF7760);hue<.66f->Color(0xFFFFC95B);else->Color(0xFF45BDD0)};drawRect(color.copy(alpha=.50f),Offset(x0,y),Size(rectW,(origin.y-y).coerceAtLeast(0f)));drawRect(ink.copy(alpha=.55f),Offset(x0,y),Size(rectW,(origin.y-y).coerceAtLeast(0f)),style=Stroke(.6.dp.toPx()))}
            val curve=Path();var first=true;var xv=0.0;while(xv<=5.0){val px=origin.x+usable*(xv/5).toFloat();val py=yOf(xv);if(first){curve.moveTo(px,py);first=false}else curve.lineTo(px,py);xv+=.03};drawPath(curve,ink,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round))
            drawLine(violet,Offset(bX,yOf(b)),Offset(bX,origin.y+15.dp.toPx()),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,8f)));drawCircle(violet,7.dp.toPx(),Offset(bX,yOf(b)));drawCircle(ink,6.dp.toPx(),origin);drawLabel("a",origin+Offset(0f,29.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawLabel("b",Offset(bX,origin.y+29.dp.toPx()),22.sp.value,violet,Paint.Align.CENTER)
            fun slider(y:Float,t:Float,color:Color,label:String,value:String){drawLabel(label,Offset(w*.075f,y+7.dp.toPx()),21.sp.value,color);drawLine(Color(0xFFDADDE0),Offset(w*.18f,y),Offset(w*.91f,y),7.dp.toPx(),StrokeCap.Round);drawLine(color,Offset(w*.18f,y),Offset(w*.18f+w*.73f*t,y),7.dp.toPx(),StrokeCap.Round);val thumb=Offset(w*.18f+w*.73f*t,y);drawCircle(Color.White,15.dp.toPx(),thumb);drawCircle(color,11.dp.toPx(),thumb);drawLabel(value,Offset(w*.125f,y+7.dp.toPx()),18.sp.value,color)}
            slider(h*.66f,(n-4)/76f,coral,"n =","$n");slider(h*.735f,((b-.5)/4.5).toFloat(),violet,"b","${"%.1f".format(b)}")
            val boxTop=h*.81f;drawRoundRect(Color.White,Offset(w*.07f,boxTop),Size(w*.86f,95.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD9D5CF),Offset(w*.07f,boxTop),Size(w*.86f,95.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("∫₀ᵇ f(x) dx",Offset(w*.29f,boxTop+58.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);drawLine(Color(0xFFE3E0DC),Offset(w*.50f,boxTop+12.dp.toPx()),Offset(w*.50f,boxTop+83.dp.toPx()),1.dp.toPx());drawLabel("${"%.3f".format(area)}",Offset(w*.72f,boxTop+63.dp.toPx()),35.sp.value,cyan,Paint.Align.CENTER)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=55.dp,vertical=14.dp),horizontalArrangement=Arrangement.SpaceBetween){
            ProofControlButton("Reset integral proof",onReset){center,scale->drawArc(ProofCoral,-55f,285f,false,center-Offset(scale*.58f,scale*.58f),Size(scale*1.16f,scale*1.16f),style=Stroke(scale*.13f,cap=StrokeCap.Round))}
            ProofControlButton("Previous integral step",{onSeekStep((playback.frame.step-1).coerceAtLeast(0))}){center,scale->drawPath(Path().apply{moveTo(center.x+scale*.42f,center.y-scale*.5f);lineTo(center.x-scale*.38f,center.y);lineTo(center.x+scale*.42f,center.y+scale*.5f);close()},ProofAmber)}
            ProofControlButton(if(playback.playing)"Pause integral animation" else "Play integral animation",onTogglePlaying){center,scale->val color=Color(0xFF2CB2C5);if(playback.playing){drawLine(color,center+Offset(-scale*.22f,-scale*.43f),center+Offset(-scale*.22f,scale*.43f),scale*.17f);drawLine(color,center+Offset(scale*.22f,-scale*.43f),center+Offset(scale*.22f,scale*.43f),scale*.17f)}else drawPath(Path().apply{moveTo(center.x-scale*.25f,center.y-scale*.45f);lineTo(center.x+scale*.45f,center.y);lineTo(center.x-scale*.25f,center.y+scale*.45f);close()},color)}
            ProofControlButton("Next integral step",{onSeekStep((playback.frame.step+1).coerceAtMost(lastStep))}){center,scale->drawPath(Path().apply{moveTo(center.x-scale*.42f,center.y-scale*.5f);lineTo(center.x+scale*.38f,center.y);lineTo(center.x-scale*.42f,center.y+scale*.5f);close()},Color(0xFF594ACB))}
        }
    }
}

@Composable
private fun EpsilonDeltaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val epsilon=(playback.frame.parameters["epsilon"]?:.8).coerceIn(.2,2.0)
    val delta=(playback.frame.parameters["delta"]?:.35).coerceIn(.05,1.0)
    val maxError=playback.frame.measurements["max |f(x)-L|"]?:2*delta
    val valid=maxError<=epsilon+1e-9
    Column(Modifier.fillMaxSize().background(Color(0xFF111920)).windowInsetsPadding(WindowInsets.statusBars)) {
        Box(Modifier.fillMaxWidth().height(112.dp).background(Color(0xFF111920))) {
            Canvas(Modifier.align(Alignment.CenterStart).padding(start=24.dp).size(48.dp).semantics{role=Role.Button;contentDescription="Back to visual proofs"}.clickable(onClick=onBack)) {drawLine(Color.White,Offset(size.width*.72f,size.height*.18f),Offset(size.width*.28f,size.height*.5f),4.dp.toPx(),StrokeCap.Round);drawLine(Color.White,Offset(size.width*.28f,size.height*.5f),Offset(size.width*.72f,size.height*.82f),4.dp.toPx(),StrokeCap.Round);drawLine(Color.White,Offset(size.width*.3f,size.height*.5f),Offset(size.width*.88f,size.height*.5f),4.dp.toPx(),StrokeCap.Round)}
            Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally){Text("Epsilon–Delta Limit",color=Color.White,fontSize=24.sp,fontWeight=FontWeight.Bold);Text("13 / 69",color=Color.White.copy(alpha=.82f),fontSize=16.sp)}
            Canvas(Modifier.align(Alignment.CenterEnd).padding(end=24.dp).size(40.dp).semantics{contentDescription="More options"}){repeat(3){drawCircle(Color.White,3.dp.toPx(),Offset(size.width/2,size.height*(.30f+it*.20f)))}}
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).background(ProofIvory).windowInsetsPadding(WindowInsets.navigationBars)
            .semantics{contentDescription="Interactive epsilon delta limit. Epsilon ${"%.2f".format(epsilon)}, delta ${"%.2f".format(delta)}, maximum output error ${"%.2f".format(maxError)}. Condition ${if(valid)"holds" else "fails"}. Drag the cyan delta handles or coral epsilon band."}
            .pointerInput(epsilon,delta){detectTapGestures{at->val center=Offset(size.width*.56f,size.height*.49f);if(at.y>size.height*.55f)onParameterChange("delta",(kotlin.math.abs(at.x-center.x)/(size.width*.28f)).coerceIn(.05f,1f).toDouble())else onParameterChange("epsilon",(kotlin.math.abs(at.y-center.y)/(size.height*.19f)*2f).coerceIn(.2f,2f).toDouble())}}
            .pointerInput(epsilon,delta){detectDragGestures{change,_->val center=Offset(size.width*.56f,size.height*.49f);if(change.position.y>size.height*.55f)onParameterChange("delta",(kotlin.math.abs(change.position.x-center.x)/(size.width*.28f)).coerceIn(.05f,1f).toDouble())else onParameterChange("epsilon",(kotlin.math.abs(change.position.y-center.y)/(size.height*.19f)*2f).coerceIn(.2f,2f).toDouble());change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF17273A);val coral=Color(0xFFEF4E45);val cyan=Color(0xFF10A9D0)
            val origin=Offset(w*.18f,h*.67f);val xScale=w*.28f;val yScale=h*.19f;val a=1.35f;val l=2.7f
            fun tx(x:Float)=origin.x+x*xScale
            fun ty(y:Float)=origin.y-y*yScale/2f
            drawLine(ink,Offset(w*.03f,origin.y),Offset(w*.97f,origin.y),1.8.dp.toPx());drawLine(ink,Offset(origin.x,h*.03f),Offset(origin.x,h*.77f),1.8.dp.toPx())
            val center=Offset(tx(a),ty(l));val epsPx=(epsilon/2.0).toFloat()*yScale;val deltaPx=delta.toFloat()*xScale
            drawRect(coral.copy(alpha=.13f),Offset(0f,center.y-epsPx),Size(w,epsPx*2));drawLine(coral,Offset(0f,center.y-epsPx),Offset(w,center.y-epsPx),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)));drawLine(coral,Offset(0f,center.y+epsPx),Offset(w,center.y+epsPx),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)));drawLine(coral,Offset(0f,center.y),Offset(w,center.y),2.dp.toPx())
            drawRect(cyan.copy(alpha=.11f),Offset(center.x-deltaPx,0f),Size(deltaPx*2,h*.78f));drawLine(cyan,Offset(center.x-deltaPx,h*.14f),Offset(center.x-deltaPx,h*.78f),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)));drawLine(cyan,Offset(center.x+deltaPx,h*.14f),Offset(center.x+deltaPx,h*.78f),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)))
            // f(x)=2x, the exact function certified by this lesson's engine.
            drawLine(Color(0xFF223B7B),Offset(tx(-.5f),ty(-1f)),Offset(tx(2.8f),ty(5.6f)),3.dp.toPx(),StrokeCap.Round)
            drawCircle(Color.White,12.dp.toPx(),center);drawCircle(coral,8.dp.toPx(),center);drawLine(Color(0xFF707E89),center,Offset(center.x,origin.y),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,8f)));drawCircle(Color(0xFF263B76),7.dp.toPx(),Offset(center.x,origin.y))
            listOf(-1f to -1,1f to 1).forEach{(side,arrow)->val p=Offset(center.x+side*deltaPx,origin.y);drawCircle(Color.White,14.dp.toPx(),p);drawCircle(cyan,11.dp.toPx(),p);val tri=Path().apply{moveTo(p.x+side*5.dp.toPx(),p.y);lineTo(p.x-side*4.dp.toPx(),p.y-5.dp.toPx());lineTo(p.x-side*4.dp.toPx(),p.y+5.dp.toPx());close()};drawPath(tri,Color.White)}
            drawDimensionLine(Offset(center.x-deltaPx,h*.18f),Offset(center.x+deltaPx,h*.18f),cyan,"δ",false);drawDimensionLine(Offset(center.x-deltaPx,h*.74f),Offset(center.x+deltaPx,h*.74f),cyan,"δ",false);drawDimensionLine(Offset(w*.13f,center.y-epsPx),Offset(w*.13f,center.y+epsPx),coral,"ε",true)
            drawLabel("L",Offset(w*.085f,center.y+8.dp.toPx()),27.sp.value,coral,Paint.Align.CENTER);drawLabel("a",Offset(center.x,origin.y+35.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER)
            val cardTop=h*.85f;fun formulaCard(left:Float,color:Color,text:String){drawRoundRect(Color.White,Offset(left,cardTop),Size(w*.31f,68.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(color,Offset(left,cardTop),Size(w*.31f,68.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel(text,Offset(left+w*.155f,cardTop+43.dp.toPx()),21.sp.value,color,Paint.Align.CENTER)};formulaCard(w*.045f,coral,"|f(x)−L| < ε");formulaCard(w*.42f,cyan,"|x−a| < δ")
        }
    }
}

@Composable
private fun BinomialSquareLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val a=(playback.frame.parameters["a"]?:2.0).coerceIn(.2,5.0)
    val b=(playback.frame.parameters["b"]?:1.0).coerceIn(.2,5.0)
    val fraction=(a/(a+b)).toFloat()
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Square of a Binomial","14 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Interactive square of a binomial with a ${"%.2f".format(a)} and b ${"%.2f".format(b)}. Drag either partition handle; the four areas remain a squared, a b, a b, and b squared."}
            .pointerInput(a,b){detectDragGestures{change,_->val left=size.width*.155f;val side=size.width*.74f;val ratio=((change.position.x-left)/side).coerceIn(.08f,.92f);val total=(a+b).coerceAtMost(5.2);onParameterChange("a",(total*ratio).coerceIn(.2,5.0));onParameterChange("b",(total*(1-ratio)).coerceIn(.2,5.0));change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF18313E);val coral=Color(0xFFF13F5E);val amber=Color(0xFFFF9814);val blue=Color(0xFF24A9DD)
            val left=w*.155f;val top=h*.12f;val side=min(w*.74f,h*.55f);val splitX=left+side*fraction;val splitY=top+side*fraction
            drawRect(Color(0xFFFF797D).copy(alpha=.67f),Offset(left,top),Size(side*fraction,side*fraction))
            drawRect(Color(0xFFFFB23E).copy(alpha=.70f),Offset(splitX,top),Size(side*(1-fraction),side*fraction))
            drawRect(Color(0xFFFFB23E).copy(alpha=.70f),Offset(left,splitY),Size(side*fraction,side*(1-fraction)))
            drawRect(Color(0xFF69CCEC).copy(alpha=.68f),Offset(splitX,splitY),Size(side*(1-fraction),side*(1-fraction)))
            drawRect(ink,Offset(left,top),Size(side,side),style=Stroke(2.7.dp.toPx()));drawLine(ink,Offset(splitX,top),Offset(splitX,top+side),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)));drawLine(ink,Offset(left,splitY),Offset(left+side,splitY),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(9f,7f)))
            drawLabel("a²",Offset(left+side*fraction*.5f,top+side*fraction*.55f),34.sp.value,Color(0xFFBF203F),Paint.Align.CENTER);drawLabel("ab",Offset(splitX+side*(1-fraction)*.5f,top+side*fraction*.55f),32.sp.value,Color(0xFFC95D00),Paint.Align.CENTER);drawLabel("ab",Offset(left+side*fraction*.5f,splitY+side*(1-fraction)*.55f),32.sp.value,Color(0xFFC95D00),Paint.Align.CENTER);drawLabel("b²",Offset(splitX+side*(1-fraction)*.5f,splitY+side*(1-fraction)*.58f),32.sp.value,Color(0xFF1268A8),Paint.Align.CENTER)
            drawDimensionLine(Offset(left,top-28.dp.toPx()),Offset(splitX,top-28.dp.toPx()),coral,"a",false);drawDimensionLine(Offset(splitX+15.dp.toPx(),top-28.dp.toPx()),Offset(left+side,top-28.dp.toPx()),amber,"b",false);drawDimensionLine(Offset(left-28.dp.toPx(),top),Offset(left-28.dp.toPx(),splitY),coral,"a",true);drawDimensionLine(Offset(left-28.dp.toPx(),splitY+15.dp.toPx()),Offset(left-28.dp.toPx(),top+side),amber,"b",true)
            val topHandle=Offset(splitX,top-28.dp.toPx())
            drawCircle(Color.White,17.dp.toPx(),topHandle);drawLabel("↔",topHandle+Offset(0f,7.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER)
            val leftHandle=Offset(left-28.dp.toPx(),splitY);drawCircle(Color.White,17.dp.toPx(),leftHandle);drawLabel("↕",leftHandle+Offset(0f,7.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER)
            val rearrangeTop=top+side+55.dp.toPx();drawRoundRect(Color(0xFFCCC8C2),Offset(w*.17f,rearrangeTop),Size(w*.66f,70.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(35.dp.toPx()),style=Stroke(1.4.dp.toPx()));drawRoundRect(amber.copy(alpha=.28f),Offset(w*.23f,rearrangeTop+12.dp.toPx()),Size(w*.18f,46.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLine(ink,Offset(w*.23f,rearrangeTop+35.dp.toPx()),Offset(w*.41f,rearrangeTop+35.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(7f,6f)));drawLabel("ab",Offset(w*.32f,rearrangeTop+28.dp.toPx()),15.sp.value,Color(0xFFC95D00),Paint.Align.CENTER);drawLabel("ab",Offset(w*.32f,rearrangeTop+52.dp.toPx()),15.sp.value,Color(0xFFC95D00),Paint.Align.CENTER);drawCircle(Color.White,27.dp.toPx(),Offset(w*.5f,rearrangeTop+35.dp.toPx()));drawLabel("↔",Offset(w*.5f,rearrangeTop+43.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawRoundRect(amber.copy(alpha=.28f),Offset(w*.61f,rearrangeTop+17.dp.toPx()),Size(w*.16f,36.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLine(ink,Offset(w*.69f,rearrangeTop+17.dp.toPx()),Offset(w*.69f,rearrangeTop+53.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(7f,6f)));drawLabel("ab  ab",Offset(w*.69f,rearrangeTop+42.dp.toPx()),14.sp.value,Color(0xFFC95D00),Paint.Align.CENTER)
            val formulaTop=h*.86f;drawRoundRect(Color.White,Offset(w*.09f,formulaTop),Size(w*.82f,94.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("(a + b)²  =  a² + 2ab + b²",Offset(w*.5f,formulaTop+59.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun AbsoluteInequalityLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val x=(playback.frame.parameters["x"]?:2.0).coerceIn(-6.0,6.0)
    val r=(playback.frame.parameters["r"]?:3.0).coerceIn(.5,6.0)
    var insideMode by remember { mutableStateOf(true) }
    val satisfies=if(insideMode) abs(x)<r else abs(x)>r
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Absolute-Value Inequality","15 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Interactive absolute value inequality. x ${"%.2f".format(x)}, radius ${"%.2f".format(r)}, ${if(insideMode)"inside" else "outside"} mode; condition ${if(satisfies)"true" else "false"}. Drag x or the radius endpoints and switch less-than or greater-than."}
            .pointerInput(insideMode){detectTapGestures{at->val y=at.y/size.height;if(y in .43f.. .55f)insideMode=at.x<size.width*.5f else {val value=((at.x/size.width)-.5f)*12.0;if(y<.34f)onParameterChange("r",abs(value).coerceIn(.5,6.0))else onParameterChange("x",value.coerceIn(-6.0,6.0))}}}
            .pointerInput(x,r){detectDragGestures{change,_->val value=((change.position.x/size.width)-.5f)*12.0;if(change.position.y<size.height*.34f)onParameterChange("r",abs(value).coerceIn(.5,6.0))else onParameterChange("x",value.coerceIn(-6.0,6.0));change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF172637);val coral=Color(0xFFFF654F);val blue=Color(0xFF159CDF);val centerX=w*.5f;val unit=w*.065f
            fun px(v:Double)=centerX+v.toFloat()*unit
            fun arrowLine(y:Float){drawLine(ink,Offset(w*.06f,y),Offset(w*.94f,y),2.4.dp.toPx());drawPath(Path().apply{moveTo(w*.055f,y);lineTo(w*.085f,y-10.dp.toPx());lineTo(w*.085f,y+10.dp.toPx());close()},ink);drawPath(Path().apply{moveTo(w*.945f,y);lineTo(w*.915f,y-10.dp.toPx());lineTo(w*.915f,y+10.dp.toPx());close()},ink);repeat(11){i->val xx=w*.13f+i*w*.074f;drawLine(ink,Offset(xx,y-7.dp.toPx()),Offset(xx,y+7.dp.toPx()),1.3.dp.toPx())}}
            val topY=h*.20f;arrowLine(topY);drawLine(ink,Offset(centerX,topY-12.dp.toPx()),Offset(centerX,topY+12.dp.toPx()),2.5.dp.toPx());drawLabel("0",Offset(centerX,topY+36.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)
            listOf(-r to "−r",r to "r").forEach{(v,label)->val p=Offset(px(v),topY);drawCircle(Color.White,15.dp.toPx(),p);drawCircle(coral,11.dp.toPx(),p);drawLabel(label,p+Offset(0f,34.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)}
            val xY=h*.33f;drawLine(ink,Offset(px(-r),xY),Offset(px(r),xY),1.6.dp.toPx());val xP=Offset(px(x),xY);drawCircle(Color.White,17.dp.toPx(),xP);drawCircle(blue,13.dp.toPx(),xP);drawLabel("x",xP+Offset(0f,-26.dp.toPx()),22.sp.value,blue,Paint.Align.CENTER);drawLine(coral,Offset(px(-r),topY+43.dp.toPx()),Offset(px(-r),h*.39f),1.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,8f)));drawLine(coral,Offset(px(r),topY+43.dp.toPx()),Offset(px(r),h*.39f),1.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,8f)));drawDimensionLine(Offset(px(-r),h*.39f),Offset(px(r),h*.39f),blue,"|x|",false)
            val segLeft=w*.29f;val segTop=h*.47f;val segW=w*.42f;val segH=58.dp.toPx();drawRoundRect(Color.White,Offset(segLeft,segTop),Size(segW,segH),androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()));drawRoundRect(Color(0xFFB6B8BC),Offset(segLeft,segTop),Size(segW,segH),androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),style=Stroke(1.dp.toPx()));drawRect(if(insideMode)blue.copy(alpha=.08f)else Color.Transparent,Offset(segLeft,segTop),Size(segW/2,segH));drawRect(if(!insideMode)blue.copy(alpha=.08f)else Color.Transparent,Offset(segLeft+segW/2,segTop),Size(segW/2,segH));drawLine(Color(0xFF9AA0A5),Offset(segLeft+segW/2,segTop),Offset(segLeft+segW/2,segTop+segH),1.dp.toPx());drawLabel("<",Offset(segLeft+segW*.25f,segTop+39.dp.toPx()),29.sp.value,if(insideMode)blue else ink,Paint.Align.CENTER);drawLabel(">",Offset(segLeft+segW*.75f,segTop+39.dp.toPx()),29.sp.value,if(!insideMode)blue else ink,Paint.Align.CENTER)
            fun box(top:Float,text:String,active:Boolean=true){drawRoundRect(Color.White,Offset(w*.055f,top),Size(w*.89f,91.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(if(active)blue else coral,Offset(w*.055f,top),Size(w*.89f,91.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.3.dp.toPx()));drawLabel(text,Offset(w*.5f,top+57.dp.toPx()),30.sp.value,if(active)blue else coral,Paint.Align.CENTER)}
            val statement=if(insideMode)"|x| < r" else "|x| > r";box(h*.61f,statement,satisfies);drawLabel("⇓",Offset(w*.5f,h*.73f),32.sp.value,blue,Paint.Align.CENTER)
            val proofTop=h*.77f;drawRoundRect(Color.White,Offset(w*.055f,proofTop),Size(w*.89f,105.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD0D1D3),Offset(w*.055f,proofTop),Size(w*.89f,105.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));val y=proofTop+52.dp.toPx();arrowLine(y);if(insideMode)drawLine(blue,Offset(px(-r),y),Offset(px(r),y),7.dp.toPx(),StrokeCap.Round)else{drawLine(blue,Offset(w*.10f,y),Offset(px(-r),y),7.dp.toPx());drawLine(blue,Offset(px(r),y),Offset(w*.90f,y),7.dp.toPx())};drawCircle(Color.White,10.dp.toPx(),Offset(px(-r),y));drawCircle(blue,10.dp.toPx(),Offset(px(-r),y),style=Stroke(2.dp.toPx()));drawCircle(Color.White,10.dp.toPx(),Offset(px(r),y));drawCircle(blue,10.dp.toPx(),Offset(px(r),y),style=Stroke(2.dp.toPx()))
            box(h*.91f,if(insideMode)"−r < x < r" else "x < −r  or  x > r",true)
        }
    }
}

@Composable
private fun EquationBalanceLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onSeekStep: (Int) -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val a=(playback.frame.parameters["a"]?:2.0).roundToInt().coerceIn(1,6)
    val b=(playback.frame.parameters["b"]?:3.0).roundToInt().coerceIn(-5,5)
    val c=(playback.frame.parameters["c"]?:11.0).roundToInt().coerceIn(-5,15)
    val solution=(c-b).toDouble()/a
    val step=playback.frame.step
    val last=playback.frame.lab.steps.lastIndex
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Equation as a Balance","16 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Interactive equation balance: $a x plus $b equals $c, solution ${"%.2f".format(solution)}, proof step ${step+1}. Tap to advance; drag horizontally to change the constant."}.pointerInput(step){detectTapGestures{onSeekStep(if(step>=last)0 else step+1)}}.pointerInput(c){detectDragGestures{change,_->onParameterChange("c",(-5+(change.position.x/size.width)*20).roundToInt().toDouble());change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF27343D);val coral=Color(0xFFFF624D);val blue=Color(0xFF2975C9);val cyan=Color(0xFF22AFC0)
            drawLabel("${a}x ${if(b>=0)"+ $b" else "− ${-b}"} = $c",Offset(w*.5f,h*.055f),34.sp.value,Color.Black,Paint.Align.CENTER)
            fun block(x:Float,y:Float,color:Color,label:String=""){val s=25.dp.toPx();drawRoundRect(color,Offset(x,y),Size(s,s),androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()));if(label.isNotEmpty())drawLabel(label,Offset(x+s/2,y+19.dp.toPx()),15.sp.value,Color.White,Paint.Align.CENTER)}
            fun scale(y:Float,leftVars:Int,leftUnits:Int,rightUnits:Int,alpha:Float=1f){val colorInk=ink.copy(alpha=alpha);val beamY=y+54.dp.toPx();drawLine(colorInk,Offset(w*.21f,beamY),Offset(w*.79f,beamY),5.dp.toPx(),StrokeCap.Round);drawLine(colorInk,Offset(w*.5f,beamY),Offset(w*.5f,beamY+105.dp.toPx()),7.dp.toPx());drawLine(colorInk,Offset(w*.43f,beamY+105.dp.toPx()),Offset(w*.57f,beamY+105.dp.toPx()),8.dp.toPx(),StrokeCap.Round);val panY=beamY+86.dp.toPx();drawLine(colorInk,Offset(w*.13f,panY),Offset(w*.39f,panY),5.dp.toPx());drawLine(colorInk,Offset(w*.61f,panY),Offset(w*.87f,panY),5.dp.toPx());repeat(leftVars){i->block(w*.15f+i*29.dp.toPx(),panY-29.dp.toPx(),blue.copy(alpha=alpha),"x")};repeat(leftUnits.coerceAtLeast(0)){i->block(w*.15f+(leftVars+i)*29.dp.toPx(),panY-25.dp.toPx(),cyan.copy(alpha=alpha))};repeat(rightUnits.coerceAtLeast(0)){i->val col=i%6;val row=i/6;block(w*.615f+col*26.dp.toPx(),panY-25.dp.toPx()-row*26.dp.toPx(),coral.copy(alpha=alpha))}}
            scale(h*.10f,a,b,c)
            drawLabel("− $b from both sides",Offset(w*.5f,h*.36f),19.sp.value,coral,Paint.Align.CENTER);drawLine(coral,Offset(w*.23f,h*.38f),Offset(w*.77f,h*.38f),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,7f)))
            scale(h*.39f,a,0,c-b,.62f)
            drawLabel("÷ $a equal groups",Offset(w*.5f,h*.65f),19.sp.value,cyan,Paint.Align.CENTER);drawLine(cyan,Offset(w*.23f,h*.67f),Offset(w*.77f,h*.67f),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(8f,7f)))
            scale(h*.68f,1,0,solution.roundToInt(),.72f)
            val resultTop=h*.925f;drawRoundRect(Color.White,Offset(w*.06f,resultTop),Size(w*.88f,70.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));block(w*.095f,resultTop+18.dp.toPx(),blue,"x");drawLabel("=",Offset(w*.19f,resultTop+43.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);repeat(solution.roundToInt().coerceIn(0,6)){i->block(w*.23f+i*28.dp.toPx(),resultTop+18.dp.toPx(),cyan)};drawLabel("x = ${"%.2f".format(solution)}",Offset(w*.77f,resultTop+45.dp.toPx()),24.sp.value,Color(0xFF25217C),Paint.Align.CENTER)
        }
    }
}

@Composable
private fun MatrixAreaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val a=(playback.frame.parameters["a"]?:2.0).coerceIn(-3.0,3.0)
    val b=(playback.frame.parameters["b"]?:1.0).coerceIn(-3.0,3.0)
    val c=(playback.frame.parameters["c"]?:1.0).coerceIn(-3.0,3.0)
    val d=(playback.frame.parameters["d"]?:2.0).coerceIn(-3.0,3.0)
    val determinant=playback.frame.measurements["determinant"]?:a*d-b*c
    val area=playback.frame.measurements["coordinate polygon area"]?:abs(determinant)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Matrix Area Transformation","17 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Interactive matrix area transformation. Matrix ${"%.1f".format(a)}, ${"%.1f".format(b)}, ${"%.1f".format(c)}, ${"%.1f".format(d)}; determinant ${"%.2f".format(determinant)} and transformed area ${"%.2f".format(area)}. Drag either transformed basis vector or any matrix slider."}
            .pointerInput(a,b,c,d){detectDragGestures{change,_->val y=change.position.y/size.height;if(y<.43f&&change.position.x>size.width*.5f){val origin=Offset(size.width*.72f,size.height*.28f);val unit=size.width*.085f;val vx=((change.position.x-origin.x)/unit).coerceIn(-3f,3f);val vy=((origin.y-change.position.y)/unit).coerceIn(-3f,3f);val e1=Offset(origin.x+a.toFloat()*unit,origin.y-c.toFloat()*unit);val e2=Offset(origin.x+b.toFloat()*unit,origin.y-d.toFloat()*unit);if((change.position-e1).getDistance()<(change.position-e2).getDistance()){onParameterChange("a",vx.toDouble());onParameterChange("c",vy.toDouble())}else{onParameterChange("b",vx.toDouble());onParameterChange("d",vy.toDouble())}}else if(y in .49f.. .67f){val index=(((change.position.x/size.width)-.48f)/.13f).roundToInt().coerceIn(0,3);val value=(3.0-(y-.49f)/.18f*6.0).coerceIn(-3.0,3.0);onParameterChange(listOf("a","b","c","d")[index],value)};change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF25313A);val coral=Color(0xFFFF5D50);val amber=Color(0xFFFF9B12);val cyan=Color(0xFF14AFCD);val violet=Color(0xFF5645C8)
            fun plane(origin:Offset,unit:Float){drawLine(ink,Offset(origin.x-unit*3.2f,origin.y),Offset(origin.x+unit*3.2f,origin.y),1.5.dp.toPx());drawLine(ink,Offset(origin.x,origin.y+unit*3.2f),Offset(origin.x,origin.y-unit*3.2f),1.5.dp.toPx());for(i in -3..3){drawLine(ink,Offset(origin.x+i*unit,origin.y-5.dp.toPx()),Offset(origin.x+i*unit,origin.y+5.dp.toPx()),1.dp.toPx());drawLine(ink,Offset(origin.x-5.dp.toPx(),origin.y-i*unit),Offset(origin.x+5.dp.toPx(),origin.y-i*unit),1.dp.toPx())}}
            val unit=w*.072f;val leftO=Offset(w*.245f,h*.255f);val rightO=Offset(w*.73f,h*.255f);plane(leftO,unit);plane(rightO,unit)
            val unitSquare=listOf(leftO,leftO+Offset(unit,0f),leftO+Offset(unit,-unit),leftO+Offset(0f,-unit));drawPath(Path().apply{unitSquare.forEachIndexed{i,p->if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)};close()},Color(0xFFEBF7FA));drawPath(Path().apply{unitSquare.forEachIndexed{i,p->if(i==0)moveTo(p.x,p.y)else lineTo(p.x,p.y)};close()},ink,style=Stroke(2.dp.toPx()));drawLine(coral,leftO,leftO+Offset(unit*2f,0f),4.dp.toPx());drawLine(amber,leftO,leftO+Offset(0f,-unit),4.dp.toPx());drawLabel("e₁",leftO+Offset(unit*1.8f,-12.dp.toPx()),19.sp.value,coral);drawLabel("e₂",leftO+Offset(-28.dp.toPx(),-unit*.9f),19.sp.value,amber)
            val e1=rightO+Offset(a.toFloat()*unit,-c.toFloat()*unit);val e2=rightO+Offset(b.toFloat()*unit,-d.toFloat()*unit);val sum=e1+e2-rightO;val poly=Path().apply{moveTo(rightO.x,rightO.y);lineTo(e1.x,e1.y);lineTo(sum.x,sum.y);lineTo(e2.x,e2.y);close()};drawPath(poly,cyan.copy(alpha=.32f));drawPath(poly,ink,style=Stroke(2.dp.toPx()));for(i in 1..2){val t=i/3f;drawLine(Color.White.copy(alpha=.78f),rightO+(e1-rightO)*t,e2+(sum-e2)*t,1.dp.toPx());drawLine(Color.White.copy(alpha=.78f),rightO+(e2-rightO)*t,e1+(sum-e1)*t,1.dp.toPx())};drawLine(cyan,rightO,e1,4.dp.toPx());drawLine(violet,rightO,e2,4.dp.toPx());drawCircle(Color.White,11.dp.toPx(),e1);drawCircle(cyan,8.dp.toPx(),e1);drawCircle(Color.White,11.dp.toPx(),e2);drawCircle(violet,8.dp.toPx(),e2)
            drawLabel("A",Offset(w*.5f,h*.20f),27.sp.value,ink,Paint.Align.CENTER);drawLabel("→",Offset(w*.5f,h*.25f),35.sp.value,ink,Paint.Align.CENTER)
            val panelTop=h*.48f;drawRoundRect(Color.White,Offset(w*.04f,panelTop),Size(w*.92f,h*.19f),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("A =  [ ${a.roundToInt()}   ${b.roundToInt()} ]",Offset(w*.20f,panelTop+44.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER);drawLabel("      [ ${c.roundToInt()}   ${d.roundToInt()} ]",Offset(w*.20f,panelTop+82.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER)
            val values=listOf(a,b,c,d);values.forEachIndexed{i,v->val x=w*(.50f+i*.13f);val top=panelTop+18.dp.toPx();val bottom=panelTop+h*.14f;drawLine(Color(0xFFCCD1D5),Offset(x,top),Offset(x,bottom),5.dp.toPx(),StrokeCap.Round);val y=bottom-((v+3)/6).toFloat()*(bottom-top);drawLine(if(i<2)cyan else violet,Offset(x,bottom),Offset(x,y),5.dp.toPx(),StrokeCap.Round);drawCircle(Color.White,11.dp.toPx(),Offset(x,y));drawCircle(if(i<2)cyan else violet,7.dp.toPx(),Offset(x,y));drawLabel("${v.roundToInt()}",Offset(x,bottom+24.dp.toPx()),16.sp.value,ink,Paint.Align.CENTER)}
            val resultTop=h*.70f;drawRoundRect(Color.White,Offset(w*.04f,resultTop),Size(w*.92f,h*.16f),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("det(A)",Offset(w*.27f,resultTop+35.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);drawLabel("${"%.2f".format(determinant)}",Offset(w*.27f,resultTop+75.dp.toPx()),31.sp.value,cyan,Paint.Align.CENTER);drawLine(Color(0xFFE0E0E0),Offset(w*.5f,resultTop+15.dp.toPx()),Offset(w*.5f,resultTop+h*.14f),1.dp.toPx());drawLabel("|det(A)|",Offset(w*.73f,resultTop+35.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);drawLabel("${"%.2f".format(area)}",Offset(w*.73f,resultTop+75.dp.toPx()),31.sp.value,cyan,Paint.Align.CENTER)
            repeat(9){i->val col=i%3;val row=i/3;val strength=if(i<area.roundToInt().coerceIn(0,9)) .72f else .18f;drawRect(cyan.copy(alpha=strength),Offset(w*.20f+col*16.dp.toPx(),resultTop+72.dp.toPx()+row*16.dp.toPx()),Size(13.dp.toPx(),13.dp.toPx()));drawRect(cyan.copy(alpha=if(i<area.roundToInt().coerceIn(0,9)) .24f else .09f),Offset(w*.66f+col*16.dp.toPx(),resultTop+72.dp.toPx()+row*16.dp.toPx()),Size(13.dp.toPx(),13.dp.toPx()))}
            val controlsTop=h*.89f;drawRoundRect(Color.White,Offset(w*.04f,controlsTop),Size(w*.92f,94.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel(if(determinant>=0)"+" else "−",Offset(w*.29f,controlsTop+20.dp.toPx()),17.sp.value,if(determinant>=0)Color(0xFF56B765) else coral,Paint.Align.CENTER);drawLabel("⇄",Offset(w*.5f,controlsTop+22.dp.toPx()),18.sp.value,ink,Paint.Align.CENTER);drawLabel(if(determinant>=0)"−" else "+",Offset(w*.71f,controlsTop+20.dp.toPx()),17.sp.value,if(determinant>=0)coral else Color(0xFF56B765),Paint.Align.CENTER)
            fun mini(origin:Offset,reversed:Boolean,alpha:Float){val x=if(reversed)-25.dp.toPx() else 25.dp.toPx();val p1=origin+Offset(x,-5.dp.toPx());val p2=origin+Offset(8.dp.toPx(),-32.dp.toPx());val p3=p1+p2-origin;val shape=Path().apply{moveTo(origin.x,origin.y);lineTo(p1.x,p1.y);lineTo(p3.x,p3.y);lineTo(p2.x,p2.y);close()};drawPath(shape,(if(reversed)coral else cyan).copy(alpha=alpha));drawPath(shape,ink.copy(alpha=alpha),style=Stroke(1.dp.toPx()));drawLine(cyan.copy(alpha=alpha),origin,p1,2.dp.toPx());drawLine(violet.copy(alpha=alpha),origin,p2,2.dp.toPx())}
            mini(Offset(w*.28f,controlsTop+61.dp.toPx()),false,1f);mini(Offset(w*.40f,controlsTop+58.dp.toPx()),false,.20f);mini(Offset(w*.50f,controlsTop+60.dp.toPx()),true,.14f);mini(Offset(w*.60f,controlsTop+58.dp.toPx()),true,.20f);mini(Offset(w*.72f,controlsTop+61.dp.toPx()),true,1f);drawLabel(if(determinant>=0)"orientation preserved" else "orientation reversed",Offset(w*.5f,controlsTop+88.dp.toPx()),13.sp.value,if(determinant>=0)cyan else coral,Paint.Align.CENTER)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=90.dp,vertical=10.dp),horizontalArrangement=Arrangement.SpaceBetween){RoundProofButton("Reset matrix",compact=true,icon={center,scale->drawArc(ProofCoral,-55f,285f,false,center-Offset(scale*.55f,scale*.55f),Size(scale*1.1f,scale*1.1f),style=Stroke(scale*.12f))},onClick=onReset);RoundProofButton("Flip matrix orientation",compact=true,icon={center,scale->drawLabel("±",center+Offset(0f,scale*.33f),24.sp.value,Color(0xFF16BBAE),Paint.Align.CENTER)},onClick={onParameterChange("a",-a)})}
    }
}

@Composable
private fun EigenvectorDirectionLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val lambda=(playback.frame.parameters["lambda"]?:1.6).coerceIn(-3.0,3.0)
    val other=(playback.frame.parameters["other"]?:1.6).coerceIn(-3.0,3.0)
    val vy=(playback.frame.parameters["vy"]?:.7).coerceIn(-2.0,2.0)
    val turn=playback.frame.measurements["normalized turn"]?:0.0
    val eigen=turn<.012
    val progress=playback.frame.step.toFloat()/playback.frame.lab.steps.lastIndex.coerceAtLeast(1)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Eigenvector Direction","18 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Interactive eigenvector direction field. Lambda ${"%.2f".format(lambda)}, vector slope ${"%.2f".format(vy)}, normalized turn ${"%.3f".format(turn)}. Drag the cyan direction, matrix pad, lambda gauge, or v slider; bottom modes and play are tappable."}
            .pointerInput(lambda,other,vy){detectDragGestures{change,_->val w=size.width.toFloat();val h=size.height.toFloat();val x=change.position.x/w;val y=change.position.y/h;when{y<.57f->{val origin=Offset(w*.5f,h*.29f);val dx=(change.position.x-origin.x).coerceAtLeast(35f);onParameterChange("vy",((origin.y-change.position.y)/dx).toDouble().coerceIn(-2.0,2.0))};y in .64f.. .86f&&x<.51f->{onParameterChange("lambda",(.4+x/.51*1.6).coerceIn(-3.0,3.0));onParameterChange("other",(.3+(1.0-y)/.36*1.3).coerceIn(-3.0,3.0))};y in .65f.. .82f&&x>.53f->{onParameterChange("lambda",((x-.58)/.34*2.2).coerceIn(-3.0,3.0))};y in .82f.. .91f&&x>.53f->{onParameterChange("vy",(((x-.58)/.34)*4.0-2.0).coerceIn(-2.0,2.0))}};change.consume()}}
            .pointerInput(Unit){detectTapGestures{p->if(p.y>size.height*.92f){when((p.x/(size.width/5f)).toInt().coerceIn(0,4)){0->{onParameterChange("lambda",1.6);onParameterChange("other",1.6);onParameterChange("vy",.7)};1->{onParameterChange("other",.6);onParameterChange("vy",.7)};2->onReset();3->{onParameterChange("lambda",1.0);onParameterChange("other",1.0)};4->onTogglePlaying()}}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF252D31);val cyan=Color(0xFF12BFD1);val purple=Color(0xFF654CC8);val coral=Color(0xFFEF5B68);val orange=Color(0xFFFF963C)
            fun arrow(start:Offset,end:Offset,color:Color,stroke:Float=1.6f){drawLine(color,start,end,stroke.dp.toPx(),StrokeCap.Round);val delta=end-start;val len=delta.getDistance().coerceAtLeast(1f);val dir=delta/len;val norm=Offset(-dir.y,dir.x);val tip=Path().apply{moveTo(end.x,end.y);lineTo(end.x-dir.x*10.dp.toPx()+norm.x*4.dp.toPx(),end.y-dir.y*10.dp.toPx()+norm.y*4.dp.toPx());lineTo(end.x-dir.x*10.dp.toPx()-norm.x*4.dp.toPx(),end.y-dir.y*10.dp.toPx()-norm.y*4.dp.toPx());close()};drawPath(tip,color)}
            val top=h*.57f;val origin=Offset(w*.5f,top*.51f);val unit=min(w*.19f,top*.22f);drawLine(ink,Offset(w*.035f,origin.y),Offset(w*.965f,origin.y),1.2.dp.toPx());drawLine(ink,Offset(origin.x,top*.03f),Offset(origin.x,top*.97f),1.2.dp.toPx());arrow(Offset(w*.06f,origin.y),Offset(w*.035f,origin.y),ink);arrow(Offset(w*.94f,origin.y),Offset(w*.965f,origin.y),ink);arrow(Offset(origin.x,top*.07f),Offset(origin.x,top*.03f),ink);arrow(Offset(origin.x,top*.93f),Offset(origin.x,top*.97f),ink)
            for(i in -2..2){if(i!=0){drawLabel("$i",Offset(origin.x+i*unit-4.dp.toPx(),origin.y+22.dp.toPx()),12.sp.value,ink,Paint.Align.CENTER);drawLabel("${-i}",Offset(origin.x-17.dp.toPx(),origin.y+i*unit+4.dp.toPx()),12.sp.value,ink,Paint.Align.CENTER)}}
            for(ix in -3..3)for(iy in -3..3){val s=Offset(origin.x+ix*unit*.68f,origin.y+iy*unit*.68f);if(s.x>w*.05f&&s.x<w*.95f&&s.y>top*.05f&&s.y<top*.94f){val phaseShift=(ix+iy+6)/12f;val color=when{phaseShift<.28f->Color(0xFFB8BCC0);phaseShift<.58f->orange;phaseShift<.78f->coral;else->purple};val vx=(.26f+.06f*progress)*unit;val fieldSlope=((other/lambda.coerceAtLeast(.15))*0.62).toFloat();val e=s+Offset(vx,-vx*fieldSlope);arrow(s,e,color.copy(alpha=.34f+phaseShift*.5f),1.1f)}}
            val xSpan=w*.43f;val lineStart=origin+Offset(-xSpan,(xSpan*vy).toFloat());val lineEnd=origin+Offset(xSpan,(-xSpan*vy).toFloat());drawLine(cyan.copy(alpha=.24f),lineStart,lineEnd,5.dp.toPx());arrow(lineStart,lineEnd,cyan,2.2f);for(t in listOf(.2f,.42f,.64f,.86f)){val p=lineStart+(lineEnd-lineStart)*t;val q=p+(lineEnd-lineStart)/18f;arrow(p,q,cyan,1.8f)}
            val formulaY=h*.59f;drawRoundRect(Color.White,Offset(w*.35f,formulaY),Size(w*.30f,54.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()));drawLabel("Av  =  λv",Offset(w*.5f,formulaY+36.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER)
            val panelTop=h*.63f;drawRoundRect(Color.White.copy(alpha=.96f),Offset(w*.025f,panelTop),Size(w*.95f,h*.285f),androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
            val left=Rect(w*.055f,panelTop+24.dp.toPx(),w*.49f,panelTop+h*.23f);drawRoundRect(Color.White,left.topLeft,left.size,androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()));val pad=Rect(left.left+24.dp.toPx(),left.top+26.dp.toPx(),left.right-22.dp.toPx(),left.bottom-28.dp.toPx());drawRoundRect(Color(0xFFF3EDF8),pad.topLeft,pad.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRect(Color(0xFFFFF0E6),Offset(pad.center.x,pad.top),Size(pad.width/2,pad.height));drawLine(Color.White,pad.center.copy(y=pad.top),pad.center.copy(y=pad.bottom),2.dp.toPx());drawLine(Color.White,pad.center.copy(x=pad.left),pad.center.copy(x=pad.right),2.dp.toPx());val knob=if(abs(lambda-other)<.02)pad.center else Offset(pad.left+((lambda-.4)/1.6).toFloat().coerceIn(0f,1f)*pad.width,pad.bottom-((other-.3)/1.3).toFloat().coerceIn(0f,1f)*pad.height);drawCircle(Color.White,16.dp.toPx(),knob);drawCircle(ink,13.dp.toPx(),knob);drawLabel("1.20",Offset(pad.left+18.dp.toPx(),pad.top+21.dp.toPx()),11.sp.value,ink);drawLabel("0.60",Offset(pad.right-45.dp.toPx(),pad.top+21.dp.toPx()),11.sp.value,ink);drawLabel("0.30",Offset(pad.left+18.dp.toPx(),pad.bottom-10.dp.toPx()),11.sp.value,ink);drawLabel("0.80",Offset(pad.right-45.dp.toPx(),pad.bottom-10.dp.toPx()),11.sp.value,ink)
            val gauge=Rect(w*.55f,panelTop+24.dp.toPx(),w*.95f,panelTop+h*.23f);drawRoundRect(Color.White,gauge.topLeft,gauge.size,androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()));drawLabel("λ",Offset(gauge.center.x,gauge.top+32.dp.toPx()),27.sp.value,purple,Paint.Align.CENTER);val gc=Offset(gauge.center.x,gauge.top+112.dp.toPx());val gr=gauge.width*.33f;for(i in 0..24){val ang=180f+i*7.2f;val col=when{ i<9->Color(0xFF4269DE);i<18->purple;else->orange};drawArc(col,ang,7.4f,false,gc-Offset(gr,gr),Size(gr*2,gr*2),style=Stroke(5.dp.toPx()))};val needle=(180.0+(lambda.coerceIn(0.0,2.0)/2.0)*180.0).toFloat();val np=pointOnCircle(gc,gr*.86f,needle);drawLine(ink,gc,np,3.dp.toPx());drawCircle(ink,10.dp.toPx(),gc);drawLabel("${"%.2f".format(lambda)}",Offset(gc.x,gc.y+40.dp.toPx()),22.sp.value,purple,Paint.Align.CENTER);val sliderY=gauge.bottom-22.dp.toPx();drawLabel("v",Offset(gauge.left+25.dp.toPx(),sliderY+5.dp.toPx()),20.sp.value,cyan,Paint.Align.CENTER);drawLine(cyan,Offset(gauge.left+48.dp.toPx(),sliderY),Offset(gauge.right-24.dp.toPx(),sliderY),3.dp.toPx(),StrokeCap.Round);val sx=gauge.left+48.dp.toPx()+((vy+2)/4).toFloat()*(gauge.width-72.dp.toPx());drawCircle(Color.White,10.dp.toPx(),Offset(sx,sliderY));drawCircle(cyan,8.dp.toPx(),Offset(sx,sliderY))
            val barTop=h*.93f;drawRoundRect(Color.White,Offset(w*.05f,barTop),Size(w*.90f,58.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()));for(i in 1..4)drawLine(Color(0xFFE4E1DE),Offset(w*(.05f+i*.18f),barTop+10.dp.toPx()),Offset(w*(.05f+i*.18f),barTop+48.dp.toPx()),1.dp.toPx());val icons=listOf("▦","≋","⊙","⤢",if(playback.playing)"Ⅱ" else "▶");icons.forEachIndexed{i,s->drawLabel(s,Offset(w*(.14f+i*.18f),barTop+40.dp.toPx()),24.sp.value,if(i==0)purple else ink,Paint.Align.CENTER)};drawLine(purple,Offset(w*.05f,barTop+57.dp.toPx()),Offset(w*.23f,barTop+57.dp.toPx()),4.dp.toPx())
        }
    }
}

@Composable
private fun OddNumbersSquareLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val n=(playback.frame.parameters["n"]?:4.0).roundToInt().coerceIn(1,4)
    var transfer by remember(n){mutableStateOf(1f)}
    val colors=listOf(Color(0xFF333942),ProofCoral,ProofAmber,Color(0xFF21BFC5))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Odd Numbers Build Squares","19 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Odd numbers build squares through stage $n. The first $n odd numbers sum to ${n*n}. Drag a colored L-border to assemble it, or choose stages 1 through 4."}
            .pointerInput(n){detectDragGestures(onDragStart={p->if(p.y>size.height*.82f){val next=(p.x/(size.width/4f)).toInt()+1;onParameterChange("n",next.toDouble())}},onDragEnd={transfer=1f}){change,delta->if(change.position.y>size.height*.80f){val next=(change.position.x/(size.width/4f)).toInt().coerceIn(0,3)+1;onParameterChange("n",next.toDouble())}else transfer=(transfer+delta.x/size.width).coerceIn(0f,1f);change.consume()}}
            .pointerInput(Unit){detectTapGestures{p->if(p.y>size.height*.80f&&p.y<size.height*.92f){onParameterChange("n",((p.x/(size.width/4f)).toInt().coerceIn(0,3)+1).toDouble())}else if(p.x>size.width*.9f&&p.y>size.height*.92f)onReset()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF303740);val rowsTop=h*.035f;val rowGap=h*.205f
            fun tile(x:Float,y:Float,s:Float,color:Color,alpha:Float=1f){drawRoundRect(color.copy(alpha=alpha),Offset(x,y),Size(s-1.5.dp.toPx(),s-1.5.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRoundRect(Color.White.copy(alpha=alpha*.85f),Offset(x+1.dp.toPx(),y+1.dp.toPx()),Size(s-3.5.dp.toPx(),s-3.5.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()),style=Stroke(.8.dp.toPx()))}
            for(k in 1..4){val active=k<=n;val alpha=if(active)1f else .16f;val rowY=rowsTop+(k-1)*rowGap;val cell=min(w*.073f,(rowGap*.72f)/k);val leftX=w*.18f;val rightX=w*.62f;val squareY=rowY+13.dp.toPx();drawCircle(Color.White,22.dp.toPx(),Offset(w*.09f,rowY+rowGap*.38f));drawCircle(Color(0xFFCBC8C4).copy(alpha=alpha),22.dp.toPx(),Offset(w*.09f,rowY+rowGap*.38f),style=Stroke(1.5.dp.toPx()));drawLabel("$k",Offset(w*.09f,rowY+rowGap*.40f+6.dp.toPx()),16.sp.value,ink.copy(alpha=alpha),Paint.Align.CENTER)
                for(r in 0 until k)for(c in 0 until k){val isNew=c==k-1||r==k-1;tile(leftX+c*cell,squareY+r*cell,cell,if(isNew)colors[k-1] else ink,alpha)}
                drawLabel("$k",Offset(leftX+k*cell/2,squareY-8.dp.toPx()),14.sp.value,ink.copy(alpha=alpha),Paint.Align.CENTER);drawLabel("$k",Offset(leftX-18.dp.toPx(),squareY+k*cell/2+5.dp.toPx()),14.sp.value,ink.copy(alpha=alpha),Paint.Align.CENTER)
                if(k>1)drawLabel("+${2*k-1}",Offset(w*.47f,rowY+rowGap*.36f),20.sp.value,colors[k-1].copy(alpha=alpha),Paint.Align.CENTER);drawLabel("→",Offset(w*.50f,rowY+rowGap*.58f),28.sp.value,Color(0xFFCBC7C3).copy(alpha=alpha),Paint.Align.CENTER)
                for(r in 0 until k)for(c in 0 until k){val isNew=r==0||c==k-1;val shift=if(k==n&&isNew)(1f-transfer)*w*.12f else 0f;tile(rightX+c*cell+shift,squareY+r*cell,cell,if(isNew)colors[k-1] else ink,alpha*(if(k==n&&isNew).55f+.45f*transfer else 1f))}
            }
            val lineY=h*.84f;drawLine(Color(0xFF263453),Offset(w*.22f,lineY),Offset(w*.78f,lineY),2.dp.toPx());for(i in 1..4){val x=w*(.22f+(i-1)*.1867f);drawCircle(if(i==n)Color(0xFF444986) else Color.White,18.dp.toPx(),Offset(x,lineY));drawCircle(Color(0xFF303D5B),18.dp.toPx(),Offset(x,lineY),style=Stroke(1.5.dp.toPx()));drawLabel("$i",Offset(x,lineY+6.dp.toPx()),14.sp.value,if(i==n)Color.White else ink,Paint.Align.CENTER)}
            val formulaTop=h*.90f;val formulaOffset=Offset(w*.13f,formulaTop);val formulaSize=Size(w*.74f,74.dp.toPx());drawRoundRect(Color.White,formulaOffset,formulaSize,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFF4E568E),formulaOffset,formulaSize,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,4f))));val odds=(1..n).joinToString(" + "){(2*it-1).toString()};drawLabel("$odds  =  $n²",Offset(w*.5f,formulaTop+48.dp.toPx()),25.sp.value,Color(0xFF3C426E),Paint.Align.CENTER);drawLabel("↺",Offset(w*.94f,formulaTop+48.dp.toPx()),22.sp.value,ProofCoral,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun CongruenceClockLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val a=(playback.frame.parameters["a"]?:9.0).roundToInt()
    val step=(playback.frame.parameters["b"]?:7.0).roundToInt()
    val n=(playback.frame.parameters["n"]?:12.0).roundToInt().coerceIn(2,16)
    fun mod(value:Int)=((value%n)+n)%n
    val start=if(mod(a)==0)n else mod(a);val result=if(mod(a+step)==0)n else mod(a+step);val negative=step-n
    var draggingStart by remember{mutableStateOf(true)}
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Congruence on a Clock","20 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Congruence clock modulo $n. Start $start plus $step lands on $result; $negative is congruent to $step modulo $n. Drag either highlighted clock marker and use the modulus minus, plus, or slider."}
            .pointerInput(a,step,n){detectDragGestures(onDragStart={p->val center=Offset(size.width*.5f,size.height*.31f);val radius=min(size.width*.39f,size.height*.255f);fun pos(v:Int):Offset{val deg=-90f+360f*(if(v==n)0 else v)/n;return pointOnCircle(center,radius,deg)};draggingStart=(p-pos(start)).getDistance()<(p-pos(result)).getDistance()}){change,_->if(change.position.y<size.height*.63f){val center=Offset(size.width*.5f,size.height*.31f);val angle=(Math.toDegrees(atan2((change.position.y-center.y).toDouble(),(change.position.x-center.x).toDouble()))+90+360)%360;val value=(angle/360*n).roundToInt().let{if(it==0)n else it};if(draggingStart)onParameterChange("a",value.toDouble())else onParameterChange("b",(value-start).let{if(it<0)it+n else it}.toDouble())}else if(change.position.y>size.height*.86f){val x=change.position.x/size.width;when{x<.22f->onParameterChange("n",(n-1).coerceAtLeast(2).toDouble());x>.78f->onParameterChange("n",(n+1).coerceAtMost(16).toDouble());else->onParameterChange("n",(2+(x-.25f)/.5f*14).roundToInt().coerceIn(2,16).toDouble())}};change.consume()}}
            .pointerInput(n){detectTapGestures{p->if(p.y>size.height*.86f){if(p.x<size.width*.22f)onParameterChange("n",(n-1).coerceAtLeast(2).toDouble())else if(p.x>size.width*.78f)onParameterChange("n",(n+1).coerceAtMost(16).toDouble())else onParameterChange("n",(2+(p.x/size.width-.25f)/.5f*14).roundToInt().coerceIn(2,16).toDouble())}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF1F2A36);val teal=Color(0xFF19AAA8);val cyan=Color(0xFF26BCE4);val violet=Color(0xFF6563D5);val center=Offset(w*.5f,h*.31f);val radius=min(w*.39f,h*.255f)
            fun p(v:Int,r:Float=radius):Offset{val deg=-90f+360f*(if(v==n)0 else v)/n;return pointOnCircle(center,r,deg)}
            for(i in 0 until n){val p1=p(i, radius);val p2=p(i,radius-10.dp.toPx());val hue=i.toFloat()/n;val col=when{hue<.25f->Color(0xFFFFB542);hue<.50f->cyan;hue<.75f->violet;else->Color(0xFFEF5555)};drawArc(col,-90f+i*360f/n,360f/n+1f,false,center-Offset(radius,radius),Size(radius*2,radius*2),style=Stroke(5.dp.toPx()));drawLine(Color(0xFFC9CDD0),p1,p2,1.dp.toPx())}
            for(i in 1..n){val pt=p(i);val highlighted=i==start||i==result;drawCircle(if(highlighted)Color.White else ProofIvory,if(highlighted)24.dp.toPx() else 18.dp.toPx(),pt);drawCircle(if(i==start)teal else if(i==result)cyan else Color(0xFF72839A),if(highlighted)20.dp.toPx() else 18.dp.toPx(),pt,style=if(highlighted)Stroke(4.dp.toPx()) else Stroke(1.5.dp.toPx()));drawLabel("$i",Offset(pt.x,pt.y+6.dp.toPx()),if(highlighted)18.sp.value else 13.sp.value,if(highlighted)ink else ink.copy(alpha=.9f),Paint.Align.CENTER)}
            for(i in 0 until n){drawLine(Color(0xFFE2E3E3),center,p(i,radius-30.dp.toPx()),.7.dp.toPx())};drawCircle(Color(0xFFD4D5D5),4.dp.toPx(),center)
            val s=p(start,radius-3.dp.toPx());val e=p(result,radius-3.dp.toPx());val control=Offset(center.x,center.y+radius*.58f);val forward=Path().apply{moveTo(s.x,s.y);quadraticTo(control.x,control.y,e.x,e.y)};drawPath(forward,teal,style=Stroke(3.dp.toPx()));drawCircle(teal,5.dp.toPx(),e)
            val dashed=Path().apply{moveTo(s.x,s.y);quadraticTo(center.x,center.y-radius*.58f,e.x,e.y)};drawPath(dashed,violet,style=Stroke(2.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(),8.dp.toPx()))))
            val card1=h*.63f;drawRoundRect(Color.White,Offset(w*.12f,card1),Size(w*.76f,76.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("→",Offset(w*.20f,card1+48.dp.toPx()),28.sp.value,teal,Paint.Align.CENTER);drawLabel("$start  +  $step  ≡  $result   (mod $n)",Offset(w*.56f,card1+48.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)
            val card2=h*.74f;drawRoundRect(Color.White,Offset(w*.12f,card2),Size(w*.76f,76.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("⇢",Offset(w*.20f,card2+48.dp.toPx()),27.sp.value,violet,Paint.Align.CENTER);drawLabel("$negative  ≡  $step   (mod $n)",Offset(w*.56f,card2+48.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)
            val controlTop=h*.87f;drawRoundRect(Color.White,Offset(w*.095f,controlTop),Size(w*.81f,78.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()));drawCircle(Color.White,25.dp.toPx(),Offset(w*.16f,controlTop+39.dp.toPx()));drawCircle(Color(0xFFE0E2E3),25.dp.toPx(),Offset(w*.16f,controlTop+39.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("−",Offset(w*.16f,controlTop+48.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER);drawCircle(Color.White,25.dp.toPx(),Offset(w*.84f,controlTop+39.dp.toPx()));drawCircle(Color(0xFFE0E2E3),25.dp.toPx(),Offset(w*.84f,controlTop+39.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("+",Offset(w*.84f,controlTop+49.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);drawLabel("$n",Offset(w*.5f,controlTop+27.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawLine(Color(0xFFD5D7D8),Offset(w*.28f,controlTop+54.dp.toPx()),Offset(w*.72f,controlTop+54.dp.toPx()),2.dp.toPx());for(i in 0..6){val x=w*(.28f+i*.0733f);drawCircle(if(i==((n-2)/14f*6).roundToInt())teal else Color(0xFFD5D7D8),if(i==((n-2)/14f*6).roundToInt())6.dp.toPx() else 4.dp.toPx(),Offset(x,controlTop+54.dp.toPx()))}
        }
    }
}

@Composable
private fun NormalProbabilityLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onTogglePlaying: () -> Unit,
    onReset: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val z=(playback.frame.parameters["z"]?:1.0).coerceIn(.1,3.5)
    val area=playback.frame.measurements["CDF identity area"]?:.682689
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Normal Probability Area","21 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{
                contentDescription="Normal probability area from ${"%.2f".format(-z)} to ${"%.2f".format(z)} equals ${"%.4f".format(area)}. Drag either bound, either z card, or the bottom slider; reset and play are tappable."
                onClick(label="Increase z") { onParameterChange("z", (z + .25).coerceAtMost(3.5)); true }
            }
            .pointerInput(Unit){detectDragGestures{change,_->val x=change.position.x/size.width;val y=change.position.y/size.height;when{y<.49f->onParameterChange("z",(abs((x-.5)*6.4)).coerceIn(.1,3.5));y in .51f.. .68f->{val local=if(x<.5)(.5-x)*7 else (x-.5)*7;onParameterChange("z",local.coerceIn(.1,3.5))};y>.84f&&x in .18f.. .82f->onParameterChange("z",(.1+(x-.18)/.64*3.4).coerceIn(.1,3.5))};change.consume()}}
            .pointerInput(Unit){detectTapGestures{p->if(p.y>size.height*.84f){if(p.x<size.width*.17f)onReset()else if(p.x>size.width*.83f)onTogglePlaying()else onParameterChange("z",(.1+(p.x/size.width-.18)/.64*3.4).coerceIn(.1,3.5))}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF21303C);val cyan=Color(0xFF22C6E0);val violet=Color(0xFF7654F2);val graphTop=h*.035f;val baseline=h*.45f;val peak=baseline-h*.38f
            fun px(x:Double):Float = w*(.5f+(x/6.4).toFloat())
            fun py(x:Double):Float = baseline-(exp(-x*x/2)*(baseline-peak)).toFloat()
            val left=px(-z);val right=px(z);for(i in 0..120){val x=-z+(2*z*i/120);val next=-z+(2*z*(i+1)/120);val t=i/120f;val col=Color(red=cyan.red*(1-t)+violet.red*t,green=cyan.green*(1-t)+violet.green*t,blue=cyan.blue*(1-t)+violet.blue*t,alpha=.82f);drawRect(col,Offset(px(x),py(x)),Size((px(next)-px(x)+1).coerceAtLeast(1f),baseline-py(x)))}
            val curve=Path();for(i in 0..180){val x=-3.2+6.4*i/180;val p=Offset(px(x),py(x));if(i==0)curve.moveTo(p.x,p.y)else curve.lineTo(p.x,p.y)};drawPath(curve,ink,style=Stroke(2.dp.toPx()));drawLine(ink,Offset(w*.04f,baseline),Offset(w*.97f,baseline),1.5.dp.toPx());for(i in -3..3){val x=px(i.toDouble());drawLine(ink,Offset(x,baseline-6.dp.toPx()),Offset(x,baseline+6.dp.toPx()),1.dp.toPx());drawLabel("$i",Offset(x,baseline+28.dp.toPx()),13.sp.value,ink,Paint.Align.CENTER)};drawLabel("z",Offset(w*.98f,baseline+23.dp.toPx()),16.sp.value,ink,Paint.Align.CENTER)
            drawLine(cyan,Offset(left,baseline),Offset(left,py(-z)),2.dp.toPx());drawLine(violet,Offset(right,baseline),Offset(right,py(z)),2.dp.toPx());listOf(left to cyan,right to violet).forEach{(x,c)->drawCircle(Color.White,11.dp.toPx(),Offset(x,baseline));drawCircle(c,8.dp.toPx(),Offset(x,baseline))};val badge=Offset(w*.5f,(py(0.0)+baseline)/2);drawRoundRect(Color.White,badge-Offset(54.dp.toPx(),18.dp.toPx()),Size(108.dp.toPx(),36.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()));drawCircle(cyan,4.dp.toPx(),badge-Offset(37.dp.toPx(),0f));drawLabel("${"%.4f".format(area)}",badge+Offset(8.dp.toPx(),7.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER)
            fun zCard(x:Float,value:Double,color:Color,label:String){val top=h*.52f;drawRoundRect(Color.White,Offset(x,top),Size(w*.40f,h*.14f),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(color,Offset(x,top),Size(w*.40f,h*.14f),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel(label,Offset(x+w*.20f,top+32.dp.toPx()),23.sp.value,color,Paint.Align.CENTER);drawLabel("${"%.2f".format(value)}",Offset(x+w*.20f,top+72.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);val sy=top+h*.112f;drawLine(Color(0xFFBAC3CC),Offset(x+25.dp.toPx(),sy),Offset(x+w*.40f-25.dp.toPx(),sy),1.dp.toPx());for(i in 0..16){val tx=x+25.dp.toPx()+i*(w*.40f-50.dp.toPx())/16;drawLine(Color(0xFF9AA9B7),Offset(tx,sy-4.dp.toPx()),Offset(tx,sy+4.dp.toPx()),.7.dp.toPx())};drawLine(color,Offset(x+w*.20f,sy-16.dp.toPx()),Offset(x+w*.20f,sy+16.dp.toPx()),2.dp.toPx())};zCard(w*.075f,-z,cyan,"z₁");zCard(w*.525f,z,violet,"z₂")
            val formulaTop=h*.70f;drawRoundRect(Color.White,Offset(w*.065f,formulaTop),Size(w*.87f,82.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("P(z₁ < Z < z₂)",Offset(w*.29f,formulaTop+51.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER);drawRoundRect(Color(0xFFF9FAFB),Offset(w*.62f,formulaTop+14.dp.toPx()),Size(w*.25f,54.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawCircle(cyan,5.dp.toPx(),Offset(w*.66f,formulaTop+41.dp.toPx()));drawLabel("${"%.4f".format(area)}",Offset(w*.76f,formulaTop+50.dp.toPx()),21.sp.value,ink,Paint.Align.CENTER)
            val controls=h*.86f;drawRoundRect(Color(0xFFFFECE9),Offset(w*.035f,controls),Size(54.dp.toPx(),54.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("↺",Offset(w*.035f+27.dp.toPx(),controls+37.dp.toPx()),25.sp.value,ProofCoral,Paint.Align.CENTER);drawRoundRect(Color(0xFFFFECE9),Offset(w*.88f,controls),Size(54.dp.toPx(),54.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel(if(playback.playing)"Ⅱ" else "▶",Offset(w*.88f+27.dp.toPx(),controls+38.dp.toPx()),24.sp.value,ProofCoral,Paint.Align.CENTER);val sliderY=controls+27.dp.toPx();drawLine(Color(0xFFE0E3E5),Offset(w*.18f,sliderY),Offset(w*.82f,sliderY),7.dp.toPx(),StrokeCap.Round);val sx=w*(.18f+((z-.1)/3.4).toFloat()*.64f);drawLine(ProofCoral,Offset(w*.18f,sliderY),Offset(sx,sliderY),7.dp.toPx(),StrokeCap.Round);drawCircle(Color.White,15.dp.toPx(),Offset(sx,sliderY));drawCircle(Color(0xFFFFA54A),12.dp.toPx(),Offset(sx,sliderY));drawLabel("▁▃▆",Offset(sx,sliderY+6.dp.toPx()),10.sp.value,Color.White,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun AnscombeQuartetLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val series=(playback.frame.parameters["series"]?:1.0).roundToInt().coerceIn(1,4)
    var selectedX by remember{mutableStateOf(10f)}
    val commonX=listOf(10f,8f,13f,9f,11f,14f,6f,4f,12f,7f,5f)
    val data=listOf(
        commonX zip listOf(8.04f,6.95f,7.58f,8.81f,8.33f,9.96f,7.24f,4.26f,10.84f,4.82f,5.68f),
        commonX zip listOf(9.14f,8.14f,8.74f,8.77f,9.26f,8.10f,6.13f,3.10f,9.13f,7.26f,4.74f),
        commonX zip listOf(7.46f,6.77f,12.74f,7.11f,7.81f,8.84f,6.08f,5.39f,8.15f,6.42f,5.73f),
        listOf(8f,8f,8f,8f,8f,8f,8f,19f,8f,8f,8f) zip listOf(6.58f,5.76f,7.71f,8.84f,8.47f,7.04f,5.25f,12.50f,5.56f,7.91f,6.89f),
    )
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Anscombe’s Quartet","22 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Anscombe's Quartet showing four different point patterns with mean x 9.00, mean y 7.50, and correlation 0.816. Series $series is selected and the shared x probe is ${"%.2f".format(selectedX)}. Tap a plot or drag the bottom probe."}
            .pointerInput(Unit){detectDragGestures{change,_->if(change.position.y>size.height*.82f)selectedX=(4f+((change.position.x/size.width-.14f)/.78f).coerceIn(0f,1f)*14f);change.consume()}}
            .pointerInput(Unit){detectTapGestures{p->if(p.y<size.height*.68f){val col=if(p.x<size.width*.5f)0 else 1;val row=if(p.y<size.height*.34f)0 else 1;onParameterChange("series",(row*2+col+1).toDouble())}else if(p.y>size.height*.82f)selectedX=(4f+((p.x/size.width-.14f)/.78f).coerceIn(0f,1f)*14f)}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF24303A);val colors=listOf(Color(0xFFF26445),Color(0xFFF0A000),Color(0xFF08A9C1),Color(0xFF5555C8));val labels=listOf("I","II","III","IV")
            fun plot(index:Int,rect:Rect){fun xp(x:Float):Float { return rect.left+((x-3f)/16f)*rect.width };fun yp(y:Float):Float { return rect.bottom-((y-4f)/10f)*rect.height };drawLine(ink,Offset(rect.left,rect.bottom),Offset(rect.right,rect.bottom),1.dp.toPx());drawLine(ink,Offset(rect.left,rect.top),Offset(rect.left,rect.bottom),1.dp.toPx());for(v in 4..18 step 2){val x=xp(v.toFloat());drawLine(Color(0xFFCFD3D6),Offset(x,rect.top),Offset(x,rect.bottom),.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,5f)));drawLabel("$v",Offset(x,rect.bottom+15.dp.toPx()),9.sp.value,ink,Paint.Align.CENTER)};for(v in 4..14 step 2){val y=yp(v.toFloat());drawLine(Color(0xFFCFD3D6),Offset(rect.left,y),Offset(rect.right,y),.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,5f)));drawLabel("$v",Offset(rect.left-8.dp.toPx(),y+3.dp.toPx()),9.sp.value,ink,Paint.Align.RIGHT)};drawLine(Color(0xFF656B72),Offset(xp(3f),yp(4.5f)),Offset(xp(18f),yp(12f)),1.dp.toPx());drawLine(Color(0xFF606974),Offset(xp(selectedX),rect.top),Offset(xp(selectedX),rect.bottom),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6f,5f)));val sy=yp(3f+.5f*selectedX);drawLine(Color(0xFF606974),Offset(rect.left,sy),Offset(rect.right,sy),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6f,5f)));drawCircle(Color.White,6.dp.toPx(),Offset(xp(selectedX),sy));drawCircle(ink,4.dp.toPx(),Offset(xp(selectedX),sy),style=Stroke(1.dp.toPx()));data[index].forEach{(x,y)->drawCircle(colors[index],4.5.dp.toPx(),Offset(xp(x),yp(y)))};val badge=Offset(rect.left+8.dp.toPx(),rect.top-18.dp.toPx());drawCircle(colors[index],14.dp.toPx(),badge);if(index+1==series)drawCircle(ink,17.dp.toPx(),badge,style=Stroke(1.5.dp.toPx()));drawLabel(labels[index],Offset(badge.x,badge.y+5.dp.toPx()),13.sp.value,Color.White,Paint.Align.CENTER)}
            val pw=w*.41f;val ph=h*.25f;plot(0,Rect(w*.065f,h*.05f,w*.065f+pw,h*.05f+ph));plot(1,Rect(w*.54f,h*.05f,w*.54f+pw,h*.05f+ph));plot(2,Rect(w*.065f,h*.39f,w*.065f+pw,h*.39f+ph));plot(3,Rect(w*.54f,h*.39f,w*.54f+pw,h*.39f+ph))
            val statsY=h*.70f;listOf(Triple("x̄","9.00",Color(0xFF08A9C1)),Triple("ȳ","7.50",Color(0xFFF0A000)),Triple("r","0.816",Color(0xFF5555C8))).forEachIndexed{i,(label,value,color)->val x=w*(.23f+i*.27f);drawLabel(label,Offset(x,statsY),22.sp.value,ink,Paint.Align.CENTER);drawLabel(value,Offset(x,statsY+35.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);drawLine(color,Offset(x-w*.09f,statsY+47.dp.toPx()),Offset(x+w*.09f,statsY+47.dp.toPx()),2.dp.toPx())}
            val sliderTop=h*.82f;drawRoundRect(Color.White,Offset(w*.04f,sliderTop),Size(w*.92f,105.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawLabel("x",Offset(w*.08f,sliderTop+63.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);val sx0=w*.14f;val sx1=w*.92f;val sy=sliderTop+58.dp.toPx();drawLine(Color(0xFF6953D1),Offset(sx0,sy),Offset(sx1,sy),4.dp.toPx(),StrokeCap.Round);for(v in 4..18 step 2){val x=sx0+(v-4)/14f*(sx1-sx0);drawLine(Color(0xFF9CA5AE),Offset(x,sy-13.dp.toPx()),Offset(x,sy-5.dp.toPx()),1.dp.toPx());drawLabel("$v",Offset(x,sy-20.dp.toPx()),10.sp.value,ink,Paint.Align.CENTER)};val knob=sx0+(selectedX-4)/14f*(sx1-sx0);drawCircle(Color.White,13.dp.toPx(),Offset(knob,sy));drawCircle(colors[series-1],10.dp.toPx(),Offset(knob,sy));drawRoundRect(Color.White,Offset(knob-34.dp.toPx(),sy+12.dp.toPx()),Size(68.dp.toPx(),30.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(15.dp.toPx()));drawLabel("${"%.2f".format(selectedX)}",Offset(knob,sy+34.dp.toPx()),14.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun VectorAdditionLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val ux=(playback.frame.parameters["ux"]?:3.0).coerceIn(-4.0,4.0);val uy=(playback.frame.parameters["uy"]?:2.0).coerceIn(-4.0,4.0);val vx=(playback.frame.parameters["vx"]?:1.0).coerceIn(-4.0,4.0);val vy=(playback.frame.parameters["vy"]?:3.0).coerceIn(-4.0,4.0)
    var draggingU by remember{mutableStateOf(true)}
    fun fmt(v:Double)=if(abs(v-v.roundToInt())<.02)v.roundToInt().toString() else "%.1f".format(v)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Vector Addition","23 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Vector addition. u is ${fmt(ux)}, ${fmt(uy)}; v is ${fmt(vx)}, ${fmt(vy)}; u plus v is ${fmt(ux+vx)}, ${fmt(uy+vy)}. Drag either colored endpoint."}
            .pointerInput(Unit){detectDragGestures(onDragStart={p->val origin=Offset(size.width*.20f,size.height*.43f);val unit=size.width*.14f;val up=origin+Offset(ux.toFloat()*unit,-uy.toFloat()*unit);val sum=origin+Offset((ux+vx).toFloat()*unit,-(uy+vy).toFloat()*unit);draggingU=(p-up).getDistance()<(p-sum).getDistance()}){change,_->val origin=Offset(size.width*.20f,size.height*.43f);val unit=size.width*.14f;val x=((change.position.x-origin.x)/unit).toDouble().coerceIn(-4.0,4.0);val y=((origin.y-change.position.y)/unit).toDouble().coerceIn(-4.0,4.0);if(draggingU){onParameterChange("ux",x);onParameterChange("uy",y)}else{onParameterChange("vx",(x-ux).coerceIn(-4.0,4.0));onParameterChange("vy",(y-uy).coerceIn(-4.0,4.0))};change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF2D2C2A);val coral=Color(0xFFEF5C4C);val orange=Color(0xFFE98608);val violet=Color(0xFF4742BC);val origin=Offset(w*.20f,h*.43f);val unit=w*.14f
            fun pt(x:Double,y:Double)=origin+Offset(x.toFloat()*unit,-y.toFloat()*unit)
            fun arrow(start:Offset,end:Offset,color:Color,width:Float=3f,dashed:Boolean=false){drawLine(color,start,end,width.dp.toPx(),StrokeCap.Round,pathEffect=if(dashed)PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(),7.dp.toPx()))else null);val delta=end-start;val len=delta.getDistance().coerceAtLeast(1f);val dir=delta/len;val norm=Offset(-dir.y,dir.x);drawPath(Path().apply{moveTo(end.x,end.y);lineTo(end.x-dir.x*15.dp.toPx()+norm.x*6.dp.toPx(),end.y-dir.y*15.dp.toPx()+norm.y*6.dp.toPx());lineTo(end.x-dir.x*15.dp.toPx()-norm.x*6.dp.toPx(),end.y-dir.y*15.dp.toPx()-norm.y*6.dp.toPx());close()},color)}
            drawLine(ink,Offset(w*.035f,origin.y),Offset(w*.97f,origin.y),1.5.dp.toPx());drawLine(ink,Offset(origin.x,h*.04f),Offset(origin.x,h*.53f),1.5.dp.toPx());for(i in -1..5){val x=origin.x+i*unit;drawLine(ink,Offset(x,origin.y-5.dp.toPx()),Offset(x,origin.y+5.dp.toPx()),1.dp.toPx());drawLabel("$i",Offset(x,origin.y+25.dp.toPx()),12.sp.value,ink,Paint.Align.CENTER);val y=origin.y-i*unit;drawLine(ink,Offset(origin.x-5.dp.toPx(),y),Offset(origin.x+5.dp.toPx(),y),1.dp.toPx());if(i!=0)drawLabel("$i",Offset(origin.x-17.dp.toPx(),y+4.dp.toPx()),12.sp.value,ink,Paint.Align.CENTER)}
            val up=pt(ux,uy);val sum=pt(ux+vx,uy+vy);val vp=pt(vx,vy);val poly=Path().apply{moveTo(origin.x,origin.y);lineTo(up.x,up.y);lineTo(sum.x,sum.y);lineTo(vp.x,vp.y);close()};drawPath(poly,violet.copy(alpha=.10f));arrow(origin,up,coral);arrow(up,sum,orange);arrow(origin,sum,violet,3.5f);arrow(origin,vp,coral.copy(alpha=.8f),2f,true);arrow(vp,sum,orange,2f,true);drawCircle(Color.White,11.dp.toPx(),up);drawCircle(coral,8.dp.toPx(),up);drawCircle(Color.White,11.dp.toPx(),sum);drawCircle(orange,8.dp.toPx(),sum);drawCircle(ink,9.dp.toPx(),origin);drawLabel("u",(origin+up)/2f+Offset(10.dp.toPx(),18.dp.toPx()),24.sp.value,coral,Paint.Align.CENTER);drawLabel("v",(up+sum)/2f+Offset(15.dp.toPx(),0f),24.sp.value,orange,Paint.Align.CENTER);drawLabel("u + v",(origin+sum)/2f+Offset(0f,-16.dp.toPx()),23.sp.value,violet,Paint.Align.CENTER);drawLabel("(${fmt(ux)},${fmt(uy)})",up+Offset(16.dp.toPx(),30.dp.toPx()),16.sp.value,coral);drawLabel("(${fmt(ux+vx)},${fmt(uy+vy)})",sum+Offset(12.dp.toPx(),-12.dp.toPx()),16.sp.value,orange)
            fun vectorRow(top:Float,label:String,x:Double,y:Double,color:Color){drawLabel(label,Offset(w*.10f,top+46.dp.toPx()),27.sp.value,color,Paint.Align.CENTER);drawRoundRect(color.copy(alpha=.06f),Offset(w*.21f,top),Size(w*.31f,62.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(color.copy(alpha=.06f),Offset(w*.56f,top),Size(w*.27f,62.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("( ${fmt(x)} , ${fmt(y)} )",Offset(w*.365f,top+41.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawLabel("( ${fmt(x)} , ${fmt(y)} )",Offset(w*.695f,top+41.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawLine(Color(0xFFD9D5D1),Offset(w*.54f,top+9.dp.toPx()),Offset(w*.54f,top+53.dp.toPx()),1.dp.toPx());drawCircle(Color.White,17.dp.toPx(),Offset(w*.91f,top+31.dp.toPx()));drawCircle(color,11.dp.toPx(),Offset(w*.91f,top+31.dp.toPx()))};vectorRow(h*.62f,"u",ux,uy,coral);vectorRow(h*.75f,"v",vx,vy,orange);vectorRow(h*.88f,"u+v",ux+vx,uy+vy,violet)
        }
    }
}

@Composable
private fun CircleCircumferenceRatioLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val radiusValue=(playback.frame.parameters["r"]?:2.0).coerceIn(.2,6.0);val sides=(playback.frame.parameters["n"]?:48.0).roundToInt().coerceIn(6,240);val ratio=playback.frame.measurements["perimeter/diameter"]?:PI
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Circle Circumference Ratio","24 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Circle circumference ratio with radius ${"%.2f".format(radiusValue)}, $sides polygon samples, and measured circumference divided by diameter ${"%.5f".format(ratio)}. Drag the red radius handle or the unrolled circumference."}
            .pointerInput(Unit){detectDragGestures{change,_->val x=change.position.x/size.width;val y=change.position.y/size.height;if(y<.43f){val center=Offset(size.width*.5f,size.height*.20f);val distance=(change.position-center).getDistance();onParameterChange("r",(.2+(distance/(size.width*.34f)).coerceIn(.08f,1f)*5.8).toDouble())}else if(y<.67f)onParameterChange("n",(6+x.coerceIn(.05f,.95f)*114).roundToInt().toDouble());change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF32312E);val coral=Color(0xFFFF6253);val cyan=Color(0xFF2D97B9);val violet=Color(0xFF7770C4);val center=Offset(w*.5f,h*.20f);val visualR=w*(.255f+.055f*((radiusValue-.2)/5.8).toFloat())
            drawCircle(Color(0xFF4D4B47),visualR+10.dp.toPx(),center,style=Stroke(8.dp.toPx()));drawCircle(ink,visualR+15.dp.toPx(),center,style=Stroke(2.dp.toPx()));for(i in 0 until 16){val p=pointOnCircle(center,visualR,(i*22.5f));drawLine(Color(0xFFC9C5BD),center,p,1.4.dp.toPx())};drawCircle(Color(0xFF55524D),16.dp.toPx(),center);drawCircle(ProofIvory,8.dp.toPx(),center);val end=pointOnCircle(center,visualR,-45f);drawLine(coral,center,end,4.dp.toPx());drawCircle(Color.White,11.dp.toPx(),end);drawCircle(coral,8.dp.toPx(),end);val mid=(center+end)/2f;drawCircle(Color.White,16.dp.toPx(),mid);drawCircle(coral,11.dp.toPx(),mid);drawLabel("↔",mid+Offset(0f,6.dp.toPx()),17.sp.value,Color.White,Paint.Align.CENTER)
            val stripTop=h*.43f;val stripLeft=w*.06f;val stripRight=w*.88f;drawRect(coral.copy(alpha=.86f),Offset(stripLeft,stripTop+18.dp.toPx()),Size(stripRight-stripLeft,55.dp.toPx()));val lobes=10;for(i in 0 until lobes){val x=stripLeft+(i+.5f)*(stripRight-stripLeft)/lobes;drawCircle(coral.copy(alpha=.88f),(stripRight-stripLeft)/lobes*.52f,Offset(x,stripTop+18.dp.toPx()))};drawLine(ink,Offset(stripLeft,stripTop+73.dp.toPx()),Offset(stripRight,stripTop+73.dp.toPx()),2.dp.toPx());drawRoundRect(coral.copy(alpha=.12f),Offset(stripRight,stripTop+18.dp.toPx()),Size(w*.08f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRoundRect(coral,Offset(stripRight,stripTop+18.dp.toPx()),Size(w*.08f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,4f))));drawLine(ink,Offset(center.x,center.y+visualR+12.dp.toPx()),Offset(center.x,stripTop-5.dp.toPx()),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(7f,6f)));drawLabel("↓",Offset(center.x,stripTop-7.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)
            val dimY=h*.61f;drawLine(ink,Offset(stripLeft,dimY),Offset(stripRight,dimY),1.5.dp.toPx());drawLine(ink,Offset(stripLeft,dimY-11.dp.toPx()),Offset(stripLeft,dimY+11.dp.toPx()),1.5.dp.toPx());drawLine(ink,Offset(stripRight,dimY-11.dp.toPx()),Offset(stripRight,dimY+11.dp.toPx()),1.5.dp.toPx());drawRoundRect(ProofIvory,Offset(center.x-28.dp.toPx(),dimY-17.dp.toPx()),Size(56.dp.toPx(),34.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()));drawLabel("d",Offset(center.x,dimY+9.dp.toPx()),26.sp.value,ink,Paint.Align.CENTER);for(i in 0..5){val x=stripLeft+(i+.5f)*(stripRight-stripLeft)/6;drawLabel("r",Offset(x,stripTop+118.dp.toPx()),22.sp.value,cyan,Paint.Align.CENTER)}
            val gaugeCenter=Offset(center.x,h*.85f);val gr=w*.30f;for(i in 0..35){val col=when{ i<12->Color(0xFFFFB13A);i<24->cyan;else->violet};drawArc(col,180f+i*5f,5.2f,false,gaugeCenter-Offset(gr,gr),Size(gr*2,gr*2),style=Stroke(13.dp.toPx()))};for(i in 0..18){val ang=180f+i*10f;drawLine(ink,pointOnCircle(gaugeCenter,gr-12.dp.toPx(),ang),pointOnCircle(gaugeCenter,gr-22.dp.toPx(),ang),if(i%3==0)2.dp.toPx() else 1.dp.toPx())};val needleAngle=(180f+((ratio-2.8)/(3.4-2.8)).toFloat().coerceIn(0f,1f)*180f);val needle=pointOnCircle(gaugeCenter,gr*.76f,needleAngle);drawLine(coral,gaugeCenter,needle,7.dp.toPx(),StrokeCap.Round);drawCircle(Color.White,15.dp.toPx(),gaugeCenter);drawCircle(coral,10.dp.toPx(),gaugeCenter);val formulaTop=h*.90f;drawRoundRect(Color.White,Offset(w*.32f,formulaTop),Size(w*.36f,64.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(Color(0xFF8D8982),Offset(w*.32f,formulaTop),Size(w*.36f,64.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.2.dp.toPx()));drawLabel("C / d = π",Offset(w*.5f,formulaTop+43.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER);drawLabel("${"%.5f".format(ratio)}",Offset(w*.5f,formulaTop-12.dp.toPx()),13.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun ShearPreservesAreaLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val base=(playback.frame.parameters["base"]?:4.0).coerceIn(.5,6.0);val height=(playback.frame.parameters["height"]?:2.0).coerceIn(.5,5.0);val shear=(playback.frame.parameters["shear"]?:1.0).coerceIn(-4.0,4.0);val area=base*height
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Shear Preserves Area","25 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Shear preserves area. Base ${"%.2f".format(base)}, height ${"%.2f".format(height)}, shear ${"%.2f".format(shear)}, area ${"%.2f".format(area)}, determinant 1. Drag the rectangle corner, shear slider, or parallelogram top edge."}
            .pointerInput(Unit){detectDragGestures{change,_->val x=change.position.x/size.width;val y=change.position.y/size.height;if(y<.32f){onParameterChange("base",(.5+x*6).coerceIn(.5,6.0));onParameterChange("height",(.5+(1-y/.32)*4.5).coerceIn(.5,5.0))}else if(y<.74f)onParameterChange("shear",(((x-.5)*8)).coerceIn(-4.0,4.0));change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF292D2E);val coral=Color(0xFFFF653F);val cyan=Color(0xFF269FBC);val topL=Offset(w*.22f,h*.055f);val rectW=w*(.48f+.12f*((base-.5)/5.5).toFloat());val rectH=h*(.13f+.07f*((height-.5)/4.5).toFloat());val topRect=Rect(topL.x,topL.y,topL.x+rectW,topL.y+rectH)
            drawRect(coral.copy(alpha=.15f),topRect.topLeft,topRect.size);drawRect(coral,topRect.topLeft,topRect.size,style=Stroke(1.5.dp.toPx()));for(i in 1 until 10){val x=topRect.left+i*topRect.width/10;drawLine(coral.copy(alpha=.75f),Offset(x,topRect.top),Offset(x,topRect.bottom),.8.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,5f)))};listOf(topRect.topLeft,Offset(topRect.right,topRect.top),Offset(topRect.left,topRect.bottom),Offset(topRect.right,topRect.bottom)).forEach{drawCircle(ink,4.dp.toPx(),it)}
            drawDimensionLine(Offset(topRect.left,topRect.bottom+25.dp.toPx()),Offset(topRect.right,topRect.bottom+25.dp.toPx()),coral,"b",false);drawDimensionLine(Offset(topRect.left-32.dp.toPx(),topRect.top),Offset(topRect.left-32.dp.toPx(),topRect.bottom),coral,"h",true)
            val sliderY=h*.35f;drawRoundRect(Color.White,Offset(w*.26f,sliderY-12.dp.toPx()),Size(w*.48f,24.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawLine(Color(0xFFDDD8D0),Offset(w*.28f,sliderY),Offset(w*.72f,sliderY),2.dp.toPx());val knobX=w*(.5f+(shear/8).toFloat()*.44f);drawCircle(Color.White,15.dp.toPx(),Offset(knobX,sliderY));drawCircle(cyan,11.dp.toPx(),Offset(knobX,sliderY));drawLabel("←",Offset(w*.23f,sliderY+7.dp.toPx()),22.sp.value,cyan,Paint.Align.CENTER);drawLabel("→",Offset(w*.77f,sliderY+7.dp.toPx()),22.sp.value,cyan,Paint.Align.CENTER)
            val bottomY=h*.68f;val paraLeft=w*.14f;val paraW=w*.58f;val paraH=h*.19f;val shift=shear.toFloat()/4f*w*.18f;val p0=Offset(paraLeft,bottomY);val p1=Offset(paraLeft+paraW,bottomY);val p2=Offset(paraLeft+paraW+shift,bottomY-paraH);val p3=Offset(paraLeft+shift,bottomY-paraH);val poly=Path().apply{moveTo(p0.x,p0.y);lineTo(p1.x,p1.y);lineTo(p2.x,p2.y);lineTo(p3.x,p3.y);close()};drawPath(poly,cyan.copy(alpha=.13f));drawPath(poly,cyan,style=Stroke(1.7.dp.toPx()));for(i in 0..10){val t=i/10f;val bot=p0+(p1-p0)*t;val top=p3+(p2-p3)*t;drawLine(cyan.copy(alpha=.75f),bot,top,.8.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6f,5f)));drawCircle(if(i==0||i==10)ink else cyan,4.dp.toPx(),bot);drawCircle(if(i==0||i==10)ink else cyan,4.dp.toPx(),top)};drawLine(ink.copy(alpha=.8f),Offset(w*.06f,bottomY),Offset(w*.96f,bottomY),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,5f)));drawLine(ink.copy(alpha=.8f),Offset(w*.06f,bottomY-paraH),Offset(w*.96f,bottomY-paraH),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,5f)));drawDimensionLine(Offset(w*.095f,bottomY-paraH),Offset(w*.095f,bottomY),coral,"h",true)
            val cardTop=h*.75f;drawRoundRect(Color.White,Offset(w*.035f,cardTop),Size(w*.93f,h*.20f),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));fun blocks(startX:Float,color:Color){for(r in 0..4)for(c in 0..9)drawRect(color.copy(alpha=.46f),Offset(startX+c*15.dp.toPx(),cardTop+24.dp.toPx()+r*15.dp.toPx()),Size(12.dp.toPx(),12.dp.toPx()))};blocks(w*.08f,coral);blocks(w*.55f,cyan);drawLabel("=",Offset(w*.50f,cardTop+67.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER);val divider=cardTop+h*.135f;drawLine(Color(0xFFE0DDD8),Offset(w*.035f,divider),Offset(w*.965f,divider),1.dp.toPx());drawLine(Color(0xFFE0DDD8),Offset(w*.5f,divider),Offset(w*.5f,cardTop+h*.20f),1.dp.toPx());drawLabel("A = bh",Offset(w*.27f,cardTop+h*.177f),27.sp.value,coral,Paint.Align.CENTER);drawLabel("det = 1",Offset(w*.73f,cardTop+h*.177f),27.sp.value,Color(0xFF3B4B97),Paint.Align.CENTER)
        }
    }
}

@Composable
private fun UnitCircleIdentityLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val theta=(playback.frame.parameters["theta"]?:50.0).coerceIn(-180.0,180.0);val radians=theta/180.0*PI;val cosine=cos(radians);val sine=sin(radians)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Unit-Circle Identity","26 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Unit circle identity at theta ${"%.1f".format(theta)} degrees. Cosine ${"%.3f".format(cosine)}, sine ${"%.3f".format(sine)}, and sine squared plus cosine squared equals 1. Drag point P or the top angle slider."}
            .pointerInput(Unit){detectDragGestures{change,_->val y=change.position.y/size.height;if(y<.07f)onParameterChange("theta",(-180.0+(change.position.x/size.width).toDouble()*360.0).coerceIn(-180.0,180.0))else if(y<.62f){val center=Offset(size.width*.5f,size.height*.29f);val deg=-Math.toDegrees(atan2((change.position.y-center.y).toDouble(),(change.position.x-center.x).toDouble()));onParameterChange("theta",deg.coerceIn(-180.0,180.0))};change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF26323A);val coral=Color(0xFFFF5C4A);val cyan=Color(0xFF12ABC2);val violet=Color(0xFF5555C7)
            val sliderY=h*.018f;drawLine(Color(0xFFE5DED3),Offset(w*.20f,sliderY),Offset(w*.80f,sliderY),6.dp.toPx(),StrokeCap.Round);val sx=w*(.20f+((theta+180)/360).toFloat()*.60f);drawLine(coral,Offset(w*.20f,sliderY),Offset(sx,sliderY),6.dp.toPx(),StrokeCap.Round);drawCircle(coral,7.dp.toPx(),Offset(sx,sliderY))
            val center=Offset(w*.5f,h*.30f);val radius=min(w*.37f,h*.24f);drawCircle(ink,radius,center,style=Stroke(2.dp.toPx()));drawLine(ink,Offset(w*.04f,center.y),Offset(w*.96f,center.y),1.4.dp.toPx());drawLine(ink,Offset(center.x,h*.055f),Offset(center.x,h*.55f),1.4.dp.toPx());listOf(-1f,1f).forEach{v->val px=center.x+v*radius;drawCircle(ink,5.dp.toPx(),Offset(px,center.y));drawLabel(if(v<0)"−1" else "1",Offset(px,center.y+25.dp.toPx()),14.sp.value,ink,Paint.Align.CENTER);val py=center.y-v*radius;drawCircle(ink,5.dp.toPx(),Offset(center.x,py));drawLabel(if(v<0)"−1" else "1",Offset(center.x-17.dp.toPx(),py+5.dp.toPx()),14.sp.value,ink,Paint.Align.CENTER)};drawLabel("0",center+Offset(-13.dp.toPx(),22.dp.toPx()),14.sp.value,ink)
            val p=center+Offset((cosine*radius).toFloat(),(-sine*radius).toFloat());val foot=Offset(p.x,center.y);drawLine(ink,center,p,2.dp.toPx());drawLine(coral,center,foot,3.dp.toPx());drawLine(cyan,foot,p,3.dp.toPx());drawLine(Color(0xFF888A89),p,Offset(center.x,p.y),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(7f,6f)));drawCircle(Color.White,11.dp.toPx(),p);drawCircle(violet,8.dp.toPx(),p);drawCircle(Color.White,8.dp.toPx(),foot);drawCircle(cyan,6.dp.toPx(),foot);drawCircle(Color.White,8.dp.toPx(),center);drawCircle(coral,6.dp.toPx(),center);drawLabel("P",p+Offset(12.dp.toPx(),-8.dp.toPx()),22.sp.value,violet);drawLabel("cos θ",(center+foot)/2f+Offset(0f,22.dp.toPx()),17.sp.value,coral,Paint.Align.CENTER);drawLabel("sin θ",(foot+p)/2f+Offset(15.dp.toPx(),0f),17.sp.value,cyan);drawArc(ink,(-theta).toFloat().coerceAtMost(0f),abs(theta).toFloat(),false,center-Offset(42.dp.toPx(),42.dp.toPx()),Size(84.dp.toPx(),84.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel("θ",center+Offset(48.dp.toPx(),-18.dp.toPx()),18.sp.value,ink)
            val sqTop=h*.65f;val maxSide=w*.25f;val cosSide=(abs(cosine).toFloat().coerceAtLeast(.18f)*maxSide);val sinSide=(abs(sine).toFloat().coerceAtLeast(.18f)*maxSide);fun square(x:Float,side:Float,color:Color,label:String){drawRect(color.copy(alpha=.76f),Offset(x,sqTop+maxSide-side),Size(side,side));for(i in 1..4){drawLine(Color.White.copy(alpha=.5f),Offset(x+i*side/5,sqTop+maxSide-side),Offset(x+i*side/5,sqTop+maxSide),.7.dp.toPx());drawLine(Color.White.copy(alpha=.5f),Offset(x,sqTop+maxSide-side+i*side/5),Offset(x+side,sqTop+maxSide-side+i*side/5),.7.dp.toPx())};drawLabel(label,Offset(x+side/2,sqTop+maxSide-side-12.dp.toPx()),17.sp.value,color,Paint.Align.CENTER)};square(w*.10f,cosSide,coral,"cos θ");square(w*.39f,sinSide,cyan,"sin θ");drawLabel("+",Offset(w*.33f,sqTop+maxSide*.65f),24.sp.value,ink,Paint.Align.CENTER);drawLabel("→",Offset(w*.61f,sqTop+maxSide*.65f),25.sp.value,ink,Paint.Align.CENTER);drawRect(violet.copy(alpha=.78f),Offset(w*.68f,sqTop),Size(maxSide,maxSide));for(i in 1..5){drawLine(Color.White.copy(alpha=.5f),Offset(w*.68f+i*maxSide/6,sqTop),Offset(w*.68f+i*maxSide/6,sqTop+maxSide),.7.dp.toPx());drawLine(Color.White.copy(alpha=.5f),Offset(w*.68f,sqTop+i*maxSide/6),Offset(w*.68f+maxSide,sqTop+i*maxSide/6),.7.dp.toPx())};drawLabel("1",Offset(w*.68f+maxSide/2,sqTop-12.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER)
            val formulaTop=h*.90f;drawRoundRect(Color.White,Offset(w*.12f,formulaTop),Size(w*.76f,72.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(Color(0xFFD8CDBD),Offset(w*.12f,formulaTop),Size(w*.76f,72.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("sin²θ  +  cos²θ  =  1",Offset(w*.5f,formulaTop+48.dp.toPx()),30.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DeMorganLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val inA = (playback.frame.parameters["inA"] ?: 0.0) >= .5
    val inB = (playback.frame.parameters["inB"] ?: 0.0) >= .5
    val left = !(inA || inB)
    val right = !inA && !inB
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("De Morgan’s Law", "27 / 69", onBack)
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics {
                    contentDescription = "De Morgan comparison. Test point in A is $inA and in B is $inB. Complement of A union B is $left; intersection of A complement and B complement is $right. Both sides agree. Tap set A or set B to toggle membership."
                }
                .pointerInput(inA, inB) {
                    detectTapGestures { p ->
                        if (p.y < size.height * .82f) {
                            if (p.x < size.width * .5f) onParameterChange("inA", if (inA) 0.0 else 1.0)
                            else onParameterChange("inB", if (inB) 0.0 else 1.0)
                        }
                    }
                },
        ) {
            drawSoftGrid()
            val w = size.width; val h = size.height; val ink = Color(0xFF182029)
            val panelTop = h * .035f; val panelBottom = h * .82f; val panelHeight = panelBottom - panelTop
            val panelLeft = w * .025f; val panelRight = w * .975f; val divider = w * .5f
            val coral = Color(0xFFFF7257); val cyan = Color(0xFF15A9C1); val violet = Color(0xFF5668D3)
            drawRoundRect(coral.copy(alpha = .16f), Offset(panelLeft, panelTop), Size(panelRight-panelLeft, panelHeight), androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()))
            clipRect(left=divider, top=panelTop, right=panelRight, bottom=panelBottom) {
                drawRect(violet.copy(alpha=.34f), Offset(divider,panelTop), Size(panelRight-divider,panelHeight))
            }
            val cy = panelTop + panelHeight * .50f; val r = min(w * .255f, panelHeight * .23f)
            val aCenter = Offset(w*.355f,cy); val bCenter=Offset(w*.645f,cy)
            clipRect(left=divider,top=panelTop,right=panelRight,bottom=panelBottom) {
                drawCircle(cyan.copy(alpha=.26f),r*2.05f,aCenter)
            }
            drawCircle(if(inA) coral.copy(alpha=.30f) else ProofIvory,r,aCenter); drawCircle(if(inB) cyan.copy(alpha=.30f) else ProofIvory,r,bCenter)
            drawCircle(ink,r,aCenter,style=Stroke(2.dp.toPx())); drawCircle(ink,r,bCenter,style=Stroke(2.dp.toPx()))
            drawLine(Color.White.copy(alpha=.9f),Offset(divider,panelTop+2.dp.toPx()),Offset(divider,panelBottom-2.dp.toPx()),3.dp.toPx())
            drawRoundRect(ink,Offset(panelLeft,panelTop),Size(panelRight-panelLeft,panelHeight),androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),style=Stroke(2.dp.toPx()))
            drawLabel("U",Offset(panelLeft+30.dp.toPx(),panelTop+52.dp.toPx()),34.sp.value,ink)
            drawLabel("A",aCenter+Offset(-r*.30f,-r*.56f),31.sp.value,ink,Paint.Align.CENTER)
            drawLabel("B",bCenter+Offset(r*.30f,-r*.56f),31.sp.value,ink,Paint.Align.CENTER)
            drawCircle(Color.White,31.dp.toPx(),Offset(divider,cy)); drawCircle(Color(0xFFD8D0C5),31.dp.toPx(),Offset(divider,cy),style=Stroke(1.dp.toPx()))
            drawLabel("‹",Offset(divider-12.dp.toPx(),cy+10.dp.toPx()),30.sp.value,coral,Paint.Align.CENTER)
            drawLabel("›",Offset(divider+13.dp.toPx(),cy+10.dp.toPx()),30.sp.value,cyan,Paint.Align.CENTER)
            val fy=h*.93f
            drawLabel("(A ∪ B)ᶜ",Offset(w*.30f,fy),34.sp.value,coral,Paint.Align.CENTER)
            drawLabel("=",Offset(w*.50f,fy),34.sp.value,ink,Paint.Align.CENTER)
            drawLabel("Aᶜ ∩ Bᶜ",Offset(w*.72f,fy),34.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun SlopeTrianglesLessonScreen(
    playback: ProofPlayback,
    onBack: () -> Unit,
    onParameterChange: (String, Double) -> Unit,
) {
    val slope=(playback.frame.parameters["m"]?:.5).coerceIn(-3.0,3.0)
    val baseRun=(playback.frame.parameters["run"]?:2.0).coerceIn(.5,4.0)
    fun number(v:Double)=if(abs(v-v.roundToInt())<.01)v.roundToInt().toString() else "%.1f".format(v)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        ProofLessonHeader("Slope Triangles","28 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Slope triangles with rise ${number(slope*baseRun)} and run ${number(baseRun)}. All three linked triangles have slope ${"%.2f".format(slope)}. Drag the graph to change slope or drag the ratio cards to change run."}
            .pointerInput(Unit){detectDragGestures{change,_->if(change.position.y<size.height*.61f){val origin=Offset(size.width*.435f,size.height*.31f);val dx=(change.position.x-origin.x).coerceAtLeast(35f);onParameterChange("m",((origin.y-change.position.y)/dx).toDouble().coerceIn(-3.0,3.0))}else if(change.position.y<size.height*.86f){onParameterChange("run",(.5+(change.position.x/size.width)*3.5).toDouble().coerceIn(.5,4.0))};change.consume()}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF252D31);val cyan=Color(0xFF14A9C2);val orange=Color(0xFFF18700);val violet=Color(0xFF594BD6)
            val graphBottom=h*.61f;val origin=Offset(w*.435f,h*.31f);val unit=w/14.7f
            drawLine(ink,Offset(w*.035f,origin.y),Offset(w*.97f,origin.y),1.7.dp.toPx());drawLine(ink,Offset(origin.x,h*.01f),Offset(origin.x,graphBottom),1.7.dp.toPx());drawLine(ink,Offset(w*.97f,origin.y),Offset(w*.94f,origin.y-10.dp.toPx()),1.7.dp.toPx());drawLine(ink,Offset(w*.97f,origin.y),Offset(w*.94f,origin.y+10.dp.toPx()),1.7.dp.toPx());drawLine(ink,Offset(origin.x,h*.01f),Offset(origin.x-10.dp.toPx(),h*.035f),1.7.dp.toPx());drawLine(ink,Offset(origin.x,h*.01f),Offset(origin.x+10.dp.toPx(),h*.035f),1.7.dp.toPx())
            for(v in -6..6 step 2){val x=origin.x+v*unit;drawLine(ink,Offset(x,origin.y-6.dp.toPx()),Offset(x,origin.y+6.dp.toPx()),1.dp.toPx());drawLabel(v.toString().replace("-","−"),Offset(x,origin.y+26.dp.toPx()),14.sp.value,ink,Paint.Align.CENTER)}
            for(v in -6..6 step 2){if(v!=0){val y=origin.y-v*unit;drawLine(ink,Offset(origin.x-6.dp.toPx(),y),Offset(origin.x+6.dp.toPx(),y),1.dp.toPx());drawLabel(v.toString().replace("-","−"),Offset(origin.x-19.dp.toPx(),y+5.dp.toPx()),14.sp.value,ink,Paint.Align.RIGHT)}};drawLabel("x",Offset(w*.955f,origin.y+27.dp.toPx()),22.sp.value,ink);drawLabel("y",Offset(origin.x-18.dp.toPx(),h*.025f),22.sp.value,ink)
            fun pt(x:Float)=Offset(origin.x+x*unit,origin.y-(slope*x).toFloat()*unit)
            drawLine(ink,pt(-6f),pt(8f),2.2.dp.toPx())
            fun triangle(x0:Float,x1:Float,color:Color){val p0=pt(x0);val p1=pt(x1);val corner=Offset(p1.x,p0.y);val path=Path().apply{moveTo(p0.x,p0.y);lineTo(corner.x,corner.y);lineTo(p1.x,p1.y);close()};drawPath(path,color.copy(alpha=.18f));drawLine(color,p0,corner,2.dp.toPx());drawLine(color,corner,p1,2.dp.toPx());drawCircle(Color.White,9.dp.toPx(),p0);drawCircle(color,6.dp.toPx(),p0);drawCircle(Color.White,9.dp.toPx(),corner);drawCircle(color,6.dp.toPx(),corner);drawCircle(Color.White,11.dp.toPx(),p1);drawCircle(color,8.dp.toPx(),p1);drawRect(color,Offset(corner.x-13.dp.toPx(),corner.y-13.dp.toPx()),Size(13.dp.toPx(),13.dp.toPx()),style=Stroke(1.5.dp.toPx()))}
            triangle(-5f,-3f,cyan);triangle(0f,3.2f,orange);triangle(3.2f,7.2f,violet)
            val cardTop=h*.64f;val cardH=h*.20f;val cardW=w*.255f
            listOf(cyan,orange,violet).forEachIndexed{i,color->val x=w*(.035f+i*.28f);drawRoundRect(Color.White,Offset(x,cardTop),Size(cardW,cardH),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawRoundRect(Color(0xFFE2D9CD),Offset(x,cardTop),Size(cardW,cardH),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),style=Stroke(1.dp.toPx()));val mul=1 shl i;drawLabel("rise",Offset(x+cardW/2,cardTop+27.dp.toPx()),15.sp.value,color,Paint.Align.CENTER);drawLine(color,Offset(x+cardW*.20f,cardTop+46.dp.toPx()),Offset(x+cardW*.20f,cardTop+82.dp.toPx()),2.dp.toPx());drawCircle(color,5.dp.toPx(),Offset(x+cardW*.20f,cardTop+46.dp.toPx()));drawCircle(color,5.dp.toPx(),Offset(x+cardW*.20f,cardTop+82.dp.toPx()));drawLabel(number(slope*baseRun*mul),Offset(x+cardW*.52f,cardTop+72.dp.toPx()),28.sp.value,color,Paint.Align.CENTER);drawLine(Color(0xFFE6DDD1),Offset(x+12.dp.toPx(),cardTop+95.dp.toPx()),Offset(x+cardW-12.dp.toPx(),cardTop+95.dp.toPx()),1.dp.toPx());drawLabel("run",Offset(x+cardW/2,cardTop+121.dp.toPx()),15.sp.value,color,Paint.Align.CENTER);drawLine(color,Offset(x+cardW*.16f,cardTop+145.dp.toPx()),Offset(x+cardW*.84f,cardTop+145.dp.toPx()),2.dp.toPx());drawCircle(color,5.dp.toPx(),Offset(x+cardW*.16f,cardTop+145.dp.toPx()));drawCircle(color,5.dp.toPx(),Offset(x+cardW*.84f,cardTop+145.dp.toPx()));drawLabel(number(baseRun*mul),Offset(x+cardW*.52f,cardTop+178.dp.toPx()),27.sp.value,color,Paint.Align.CENTER)}
            val lockX=w*.86f;drawRoundRect(Color.White,Offset(lockX,cardTop+h*.045f),Size(w*.11f,h*.12f),androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()));drawRoundRect(ink,Offset(lockX,cardTop+h*.045f),Size(w*.11f,h*.12f),androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel("🔗",Offset(lockX+w*.055f,cardTop+h*.12f),31.sp.value,ink,Paint.Align.CENTER)
            val formulaTop=h*.88f;drawRoundRect(Color.White,Offset(w*.035f,formulaTop),Size(w*.93f,h*.095f),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawRoundRect(Color(0xFFE2D9CD),Offset(w*.035f,formulaTop),Size(w*.93f,h*.095f),androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("m  =",Offset(w*.43f,formulaTop+h*.06f),32.sp.value,ink,Paint.Align.RIGHT);drawLabel("rise",Offset(w*.58f,formulaTop+h*.042f),22.sp.value,violet,Paint.Align.CENTER);drawLine(violet,Offset(w*.52f,formulaTop+h*.052f),Offset(w*.64f,formulaTop+h*.052f),2.dp.toPx());drawLabel("run",Offset(w*.58f,formulaTop+h*.079f),22.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun CountingPathsLessonScreen(playback: ProofPlayback,onBack:()->Unit) {
    var selectedX by remember { mutableStateOf(2) };var selectedY by remember { mutableStateOf(3) }
    val rows=listOf(listOf(0,1,1,1,1),listOf(1,2,1,0,0),listOf(1,3,3,1,0),listOf(1,4,6,4,1),listOf(1,5,10,10,5))
    val selected=rows[selectedY][selectedX];val parentLeft=if(selectedY>0&&selectedX>0)rows[selectedY-1][selectedX-1] else 0;val parentRight=if(selectedY>0)rows[selectedY-1][selectedX] else 0
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(132.dp).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=48.sp,color=Color(0xFF20262B),modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Counting Paths",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color(0xFF20262B),textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("29 / 69",fontSize=18.sp,color=Color(0xFF20262B));Text("⋮",fontSize=36.sp,fontWeight=FontWeight.Bold,color=Color(0xFF20262B),modifier=Modifier.padding(start=8.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Counting paths lattice. Selected Pascal node $selected is formed from $parentLeft plus $parentRight. Tap any lattice node to inspect its two incoming counts. The cyan route connects start S to end E."}
            .pointerInput(Unit){detectTapGestures{p->val w=size.width;val h=size.height;val x0=w*.10f;val dx=w*.165f;val yBottom=h*.61f;val dy=h*.105f;val x=((p.x-x0)/dx).roundToInt();val y=((yBottom-p.y)/dy).roundToInt();if(x in 0..4&&y in 1..4&&x<=y+1){selectedX=x;selectedY=y}}}) {
            val w=size.width;val h=size.height;val ink=Color(0xFF343536);val cyan=Color(0xFF13C8E8);val blue=Color(0xFF238BD0);val violet=Color(0xFF6843CD);val orange=Color(0xFFFF9F1A)
            val x0=w*.10f;val dx=w*.165f;val yBottom=h*.61f;val dy=h*.105f;fun node(x:Int,y:Int)=Offset(x0+x*dx,yBottom-y*dy)
            for(y in 0..4){for(x in 0..4){if(x<4)drawLine(Color(0xFF707170),node(x,y),node(x+1,y),2.dp.toPx());if(y<4)drawLine(Color(0xFF707170),node(x,y),node(x,y+1),2.dp.toPx())}}
            val end=Offset(x0+5*dx,node(0,4).y);drawLine(Color(0xFF707170),node(4,4),end,2.dp.toPx())
            drawLine(Color.White, node(0,0),node(4,0),9.dp.toPx());drawLine(cyan,node(0,0),node(4,0),5.dp.toPx());drawLine(Color.White,node(4,0),node(4,4),9.dp.toPx());drawLine(cyan,node(4,0),node(4,4),5.dp.toPx());drawLine(Color.White,node(4,4),end,9.dp.toPx());drawLine(cyan,node(4,4),end,5.dp.toPx())
            val selectedPos=node(selectedX,selectedY);if(selectedY>0){val p1=node((selectedX-1).coerceAtLeast(0),selectedY-1);val p2=node(selectedX,selectedY-1);drawLine(blue,p1,selectedPos,2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,7f)));drawLine(violet,p2,selectedPos,2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4f,7f)))}
            fun circleNode(pos:Offset,label:String,accent:Color?=null,dark:Boolean=false){if(accent!=null){drawCircle(accent.copy(alpha=.12f),31.dp.toPx(),pos);drawCircle(accent.copy(alpha=.25f),27.dp.toPx(),pos)};drawCircle(if(dark)Color(0xFF303131) else ProofIvory,23.dp.toPx(),pos);drawCircle(accent?:ink,25.dp.toPx(),pos,style=Stroke(if(accent==null)1.4.dp.toPx() else 2.dp.toPx()));drawLabel(label,pos+Offset(0f,9.dp.toPx()),25.sp.value,if(dark)Color.White else ink,Paint.Align.CENTER)}
            for(y in 0..4){for(x in 0..4){val accent=when{ x==selectedX&&y==selectedY->orange;selectedY>0&&y==selectedY-1&&x==selectedX-1->blue;selectedY>0&&y==selectedY-1&&x==selectedX->violet;x==4||y==0->cyan;else->null};circleNode(node(x,y),if(x==0&&y==0)"S" else rows[y][x].toString(),accent,dark=x==0&&y==0)}}
            circleNode(end,"E",Color(0xFFFF6B54),dark=true)
        }
    }
}

@Composable
private fun NaturalNumbersSumLessonScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:6.0).roundToInt().coerceIn(3,8);var focusStage by remember{mutableStateOf(0)};val total=n*(n+1)/2
    val palette=listOf(Color(0xFFFF6654),Color(0xFFFF9D28),Color(0xFFFFCA31),Color(0xFF43C5AD),Color(0xFF4099DF),Color(0xFF514FC6),Color(0xFF3D399E),Color(0xFF282572))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        ProofLessonHeader("Sum of First n Natural Numbers","30 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Sum of the first $n natural numbers is $total. Three-stage staircase rearrangement; stage ${focusStage+1} is highlighted. Tap the construction to advance, or tap the lower left and right edges to change n."}.pointerInput(n){detectTapGestures{p->if(p.y>size.height*.70f){onParameterChange("n",(n+if(p.x>size.width*.5f)1 else -1).coerceIn(3,8).toDouble())}else focusStage=(focusStage+1)%3}}){
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF20262C);val cell=min(w*.055f,h*.036f)
            drawLabel("1 + 2 + ⋯ + n",Offset(w*.5f,h*.055f),34.sp.value,ink,Paint.Align.CENTER)
            fun cellAt(x:Float,y:Float,color:Color,alpha:Float=1f){drawRoundRect(color.copy(alpha=alpha),Offset(x,y),Size(cell,cell),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRoundRect(Color.White.copy(alpha=.75f),Offset(x,y),Size(cell,cell),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),style=Stroke(.8.dp.toPx()))}
            fun staircase(x:Float,y:Float,flip:Boolean,alpha:Float){for(row in 0 until n){val count=row+1;for(col in 0 until count){val xx=if(!flip)x+col*cell else x+(n-1-col)*cell;val yy=y+(n-1-row)*cell;cellAt(xx,yy,palette[row.coerceAtMost(palette.lastIndex)],alpha)}}}
            val topAlpha=if(focusStage==0)1f else .64f;staircase(w*.13f,h*.10f,false,topAlpha);staircase(w*.55f,h*.10f,true,topAlpha);drawLabel("⋮",Offset(w*.275f,h*.23f),25.sp.value,ink,Paint.Align.CENTER)
            drawLabel("↻",Offset(w*.14f,h*.40f),30.sp.value,ink,Paint.Align.CENTER);drawLabel("→",Offset(w*.49f,h*.44f),35.sp.value,ink,Paint.Align.CENTER);staircase(w*.21f,h*.36f,false,if(focusStage==1)1f else .58f);staircase(w*.55f,h*.36f,true,if(focusStage==1)1f else .58f)
            val rectX=w*.24f;val rectY=h*.62f;val rectCell=min(cell,w*.50f/(n+1));for(row in 0 until n){for(col in 0..n){val fromFirst=col<=n-row-1;val color=if(fromFirst)palette[(n-1-row).coerceAtMost(palette.lastIndex)] else Color(0xFFE9E2D6);drawRoundRect(color.copy(alpha=if(focusStage==2)1f else .75f),Offset(rectX+col*rectCell,rectY+row*rectCell),Size(rectCell,rectCell),androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()));drawRect(Color.White.copy(alpha=.8f),Offset(rectX+col*rectCell,rectY+row*rectCell),Size(rectCell,rectCell),style=Stroke(.7.dp.toPx()))}}
            val rw=(n+1)*rectCell;val rh=n*rectCell;drawLine(ink,Offset(rectX,rectY-18.dp.toPx()),Offset(rectX+rw,rectY-18.dp.toPx()),1.dp.toPx());drawLabel("n + 1",Offset(rectX+rw/2,rectY-23.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawLine(ink,Offset(rectX-18.dp.toPx(),rectY),Offset(rectX-18.dp.toPx(),rectY+rh),1.dp.toPx());drawLabel("n",Offset(rectX-31.dp.toPx(),rectY+rh/2),24.sp.value,ink,Paint.Align.CENTER)
            val formulaY=h*.91f;drawRoundRect(Color(0xFFF0EEFF),Offset(w*.32f,formulaY),Size(w*.36f,80.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFF7774E9),Offset(w*.32f,formulaY),Size(w*.36f,80.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("n(n + 1)",Offset(w*.5f,formulaY+34.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawLine(ink,Offset(w*.41f,formulaY+42.dp.toPx()),Offset(w*.59f,formulaY+42.dp.toPx()),1.5.dp.toPx());drawLabel("2",Offset(w*.5f,formulaY+69.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun OddNumbersBuildSquaresPatternScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:5.0).roundToInt().coerceIn(1,5)
    val colors=listOf(Color(0xFF34383D),Color(0xFFF06451),Color(0xFFF2A12B),Color(0xFF2FB2BC),Color(0xFF3F55B4))
    fun select(value:Int)=onParameterChange("n",value.coerceIn(1,5).toDouble())
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        ProofLessonHeader("Odd Numbers Build Squares","31 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Odd numbers build squares. Stage n equals $n; the new border has ${2*n-1} tiles and the complete square has ${n*n} tiles. Select any stage from 1 through 5 or drag the stage rail."}
            .pointerInput(n){detectDragGestures{change,_->if(change.position.y>size.height*.55f){val stage=(5-(((change.position.y/size.height-.59f)/.285f)*4f).roundToInt()).coerceIn(1,5);select(stage)};change.consume()}}
            .pointerInput(n){detectTapGestures{p->if(p.y<size.height*.53f)select(if(n==5)1 else n+1) else {val stage=(5-(((p.y/size.height-.59f)/.285f)*4f).roundToInt()).coerceIn(1,5);select(stage)}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF24292E)
            val cell=min(w*.074f,h*.0385f);val side=2*n-1;val gridSide=(2*5-1)*cell;val gridLeft=(w-gridSide)/2;val gridTop=h*.055f
            for(r in 0 until 9)for(c in 0 until 9){drawCircle(Color(0xFFD5D0C8),1.5.dp.toPx(),Offset(gridLeft+(c+.5f)*cell,gridTop+(r+.5f)*cell))}
            val activeSide=side*cell;val activeLeft=(w-activeSide)/2;val activeTop=gridTop+(gridSide-activeSide)/2
            for(r in 0 until side)for(c in 0 until side){val layer=maxOf(abs(r-(side-1)/2),abs(c-(side-1)/2));val stage=layer+1;val color=colors[stage-1];drawRoundRect(color,Offset(activeLeft+c*cell,activeTop+r*cell),Size(cell-1.dp.toPx(),cell-1.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRoundRect(Color.White.copy(alpha=.30f),Offset(activeLeft+c*cell+1.dp.toPx(),activeTop+r*cell+1.dp.toPx()),Size(cell-3.dp.toPx(),cell-3.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),style=Stroke(.7.dp.toPx()))}
            for(k in 1..n){val s=(2*k-1)*cell;val l=(w-s)/2;val t=gridTop+(gridSide-s)/2;drawRoundRect(colors[k-1].copy(alpha=.75f),Offset(l,t),Size(s,s),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),style=Stroke(1.dp.toPx()))}
            listOf(1,3,5,7,9).take(n).forEachIndexed{i,value->val k=i+1;val p=if(k==1)Offset(w*.5f,gridTop+gridSide*.5f) else Offset(w*.5f+(k-1)*cell,gridTop+gridSide*.5f);drawLabel("$value",p+Offset(0f,7.dp.toPx()),18.sp.value,Color.White,Paint.Align.CENTER)}
            val railX=w*.095f;val listTop=h*.59f;val listBottom=h*.875f;drawLine(Color(0xFFC7C4BE),Offset(railX,listTop),Offset(railX,listBottom),5.dp.toPx());drawLine(colors[n-1],Offset(railX,listBottom-(n-1)*(listBottom-listTop)/4),Offset(railX,listBottom),5.dp.toPx())
            for(stage in 5 downTo 1){val index=stage-1;val y=listBottom-index*(listBottom-listTop)/4;val color=colors[index];drawCircle(ProofIvory,15.dp.toPx(),Offset(railX,y));drawCircle(if(stage<=n)color else Color(0xFFBDBBB6),11.dp.toPx(),Offset(railX,y),style=Stroke(3.dp.toPx()));drawLabel("${2*stage-1}",Offset(w*.205f,y+7.dp.toPx()),20.sp.value,if(stage<=n)color else Color(0xFF989793),Paint.Align.CENTER)
                val miniX=w*.28f;val miniCell=5.2.dp.toPx();for(rr in 0 until stage)for(cc in 0 until stage){val edge=rr==0||cc==0;drawRect(if(edge)color.copy(alpha=if(stage<=n)1f else .22f) else ProofIvory,Offset(miniX+cc*miniCell,y-stage*miniCell/2+rr*miniCell),Size(miniCell-1,miniCell-1));drawRect(Color(0xFFD8D4CC),Offset(miniX+cc*miniCell,y-stage*miniCell/2+rr*miniCell),Size(miniCell-1,miniCell-1),style=Stroke(.4.dp.toPx()))}
                drawLabel("n = $stage",Offset(w*.64f,y+7.dp.toPx()),20.sp.value,if(stage<=n)color else Color(0xFF989793),Paint.Align.CENTER);val badge=Rect(w*.79f,y-19.dp.toPx(),w*.865f,y+19.dp.toPx());drawRoundRect(ProofIvory,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(color.copy(alpha=if(stage<=n)1f else .35f),badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("$stage²",Offset(badge.center.x,badge.center.y+7.dp.toPx()),18.sp.value,if(stage<=n)color else Color(0xFF989793),Paint.Align.CENTER)}
            val formulaTop=h*.91f;drawRoundRect(Color.White,Offset(w*.055f,formulaTop),Size(w*.89f,72.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFDAD6CF),Offset(w*.055f,formulaTop),Size(w*.89f,72.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("1 + 3 + ⋯ + (2n − 1) = n²",Offset(w*.5f,formulaTop+45.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun TriangularNumbersPatternScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:7.0).roundToInt().coerceIn(3,10);val total=n*(n+1)/2
    fun select(value:Int)=onParameterChange("n",value.coerceIn(3,10).toDouble())
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        ProofLessonHeader("Triangular Numbers","32 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Triangular number T sub $n is $total. Two copies rearrange into an $n by ${n+1} rectangle. Use minus, plus, or drag the bottom number rail."}
            .pointerInput(n){detectDragGestures{change,_->if(change.position.y>size.height*.91f)select((3+(change.position.x/size.width*7)).roundToInt());change.consume()}}
            .pointerInput(n){detectTapGestures{p->if(p.y>size.height*.80f&&p.y<size.height*.91f){if(p.x<size.width*.36f)select(n-1) else if(p.x>size.width*.64f)select(n+1)}else if(p.y>size.height*.91f)select((3+(p.x/size.width*7)).roundToInt())}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF343B43);val coral=Color(0xFFFF6654);val cyan=Color(0xFF20B6C2);val indigo=Color(0xFF4F58C8)
            val dx=min(w*.056f,w*.37f/n);val dy=min(h*.032f,h*.205f/n);val leftX=w*.20f;val topY=h*.065f
            fun dot(pos:Offset,color:Color){drawCircle(Color(0x22000000),10.dp.toPx(),pos+Offset(2.dp.toPx(),3.dp.toPx()));drawCircle(color,8.5.dp.toPx(),pos);drawCircle(color.copy(alpha=.9f),8.5.dp.toPx(),pos,style=Stroke(1.dp.toPx()))}
            for(r in 0 until n)for(c in 0..r)dot(Offset(leftX+c*dx,topY+r*dy),coral)
            val rightX=w*.59f;for(r in 0 until n)for(c in 0..r)dot(Offset(rightX+c*dx,topY+(n-1-r)*dy),cyan)
            drawLine(ink,Offset(w*.11f,topY-8.dp.toPx()),Offset(w*.11f,topY+(n-1)*dy+8.dp.toPx()),1.5.dp.toPx());drawLine(ink,Offset(w*.095f,topY-8.dp.toPx()),Offset(w*.125f,topY-8.dp.toPx()),1.5.dp.toPx());drawLine(ink,Offset(w*.095f,topY+(n-1)*dy+8.dp.toPx()),Offset(w*.125f,topY+(n-1)*dy+8.dp.toPx()),1.5.dp.toPx());drawLabel("n",Offset(w*.07f,topY+(n-1)*dy/2+8.dp.toPx()),25.sp.value,coral,Paint.Align.CENTER)
            val arc=Path().apply{moveTo(w*.37f,topY+20.dp.toPx());quadraticTo(w*.50f,topY-18.dp.toPx(),w*.64f,topY+65.dp.toPx())};drawPath(arc,Color(0xFF9AA0A4),style=Stroke(1.8.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),6.dp.toPx()))));drawLabel("↻",Offset(w*.80f,topY+h*.10f),34.sp.value,ink,Paint.Align.CENTER);drawLabel("⌄",Offset(w*.50f,h*.305f),42.sp.value,ink,Paint.Align.CENTER)
            val rectDx=min(w*.055f,w*.50f/(n+1));val rectDy=min(h*.030f,h*.20f/n);val rectW=(n+1)*rectDx;val rectH=n*rectDy;val rectX=(w-rectW)/2;val rectY=h*.36f
            for(r in 0 until n)for(c in 0..n){val color=if(c<n-r)coral else cyan;dot(Offset(rectX+(c+.5f)*rectDx,rectY+(r+.5f)*rectDy),color)}
            drawLine(ink,Offset(rectX-30.dp.toPx(),rectY),Offset(rectX-30.dp.toPx(),rectY+rectH),1.5.dp.toPx());drawLine(ink,Offset(rectX-42.dp.toPx(),rectY),Offset(rectX-18.dp.toPx(),rectY),1.5.dp.toPx());drawLine(ink,Offset(rectX-42.dp.toPx(),rectY+rectH),Offset(rectX-18.dp.toPx(),rectY+rectH),1.5.dp.toPx());drawLabel("n",Offset(rectX-57.dp.toPx(),rectY+rectH/2+8.dp.toPx()),25.sp.value,coral,Paint.Align.CENTER)
            drawLine(ink,Offset(rectX,rectY+rectH+25.dp.toPx()),Offset(rectX+rectW,rectY+rectH+25.dp.toPx()),1.5.dp.toPx());drawLine(ink,Offset(rectX,rectY+rectH+14.dp.toPx()),Offset(rectX,rectY+rectH+36.dp.toPx()),1.5.dp.toPx());drawLine(ink,Offset(rectX+rectW,rectY+rectH+14.dp.toPx()),Offset(rectX+rectW,rectY+rectH+36.dp.toPx()),1.5.dp.toPx());drawLabel("n + 1",Offset(rectX+rectW/2,rectY+rectH+58.dp.toPx()),24.sp.value,cyan,Paint.Align.CENTER)
            val card=Rect(w*.23f,h*.64f,w*.77f,h*.75f);drawRoundRect(Color.White,card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawRoundRect(Color(0xFFD7D2C9),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("Tₙ  =",Offset(w*.39f,card.center.y+10.dp.toPx()),28.sp.value,coral,Paint.Align.CENTER);drawLabel("n(n + 1)",Offset(w*.59f,card.center.y-2.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawLine(ink,Offset(w*.50f,card.center.y+7.dp.toPx()),Offset(w*.68f,card.center.y+7.dp.toPx()),1.5.dp.toPx());drawLabel("2",Offset(w*.59f,card.center.y+36.dp.toPx()),24.sp.value,indigo,Paint.Align.CENTER)
            val control=Rect(w*.20f,h*.79f,w*.80f,h*.885f);drawRoundRect(Color.White,control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(34.dp.toPx()));drawRoundRect(Color(0xFFE1DDD6),control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(34.dp.toPx()),style=Stroke(1.dp.toPx()));drawCircle(indigo,24.dp.toPx(),Offset(w*.28f,control.center.y));drawLabel("−",Offset(w*.28f,control.center.y+10.dp.toPx()),31.sp.value,Color.White,Paint.Align.CENTER);drawCircle(indigo,24.dp.toPx(),Offset(w*.72f,control.center.y));drawLabel("+",Offset(w*.72f,control.center.y+10.dp.toPx()),30.sp.value,Color.White,Paint.Align.CENTER);drawLabel("$n",Offset(w*.50f,control.center.y+13.dp.toPx()),34.sp.value,ink,Paint.Align.CENTER);drawLine(Color(0xFFE0DDD8),Offset(w*.37f,control.top+10.dp.toPx()),Offset(w*.37f,control.bottom-10.dp.toPx()),1.dp.toPx());drawLine(Color(0xFFE0DDD8),Offset(w*.63f,control.top+10.dp.toPx()),Offset(w*.63f,control.bottom-10.dp.toPx()),1.dp.toPx())
            val railY=h*.94f;drawLine(Color(0xFFB8BABC),Offset(w*.16f,railY),Offset(w*.84f,railY),4.dp.toPx(),StrokeCap.Round);for(v in 3..10){val x=w*(.16f+(v-3)/7f*.68f);drawCircle(if(v==n)indigo else Color(0xFFB8BABC),if(v==n)11.dp.toPx() else 6.dp.toPx(),Offset(x,railY));if(v==n){drawCircle(Color.White,14.dp.toPx(),Offset(x,railY),style=Stroke(2.dp.toPx()));drawCircle(indigo,10.dp.toPx(),Offset(x,railY))}}
        }
    }
}

@Composable
private fun ConsecutiveSquaresDifferenceScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:6.0).roundToInt().coerceIn(3,9);val difference=2*n-1;var stage by remember{mutableStateOf(0)}
    fun select(v:Int)=onParameterChange("n",v.coerceIn(3,9).toDouble())
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(90.dp).padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=48.sp,color=ProofNavy,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Difference of Consecutive Squares",fontSize=20.sp,fontWeight=FontWeight.Bold,color=Color(0xFF20262B),textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("⋮",fontSize=36.sp,fontWeight=FontWeight.Bold,color=ProofNavy,modifier=Modifier.padding(start=8.dp))}
        Text("33 / 69",fontSize=18.sp,color=Color(0xFF64666A),textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth().height(34.dp))
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Difference of consecutive squares. $n squared minus ${n-1} squared equals $difference. Rearrangement stage ${stage+1} of 3. Tap the construction to advance; use the bottom arrows to change n."}
            .pointerInput(n){detectTapGestures{p->if(p.y>size.height*.88f){select(n+if(p.x>size.width*.5f)1 else -1)}else stage=(stage+1)%3}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF27323B);val coral=Color(0xFFFF654F);val cyan=Color(0xFF31B4C5);val amber=Color(0xFFFFAD28);val cell=min(w*.057f,h*.030f)
            fun tile(x:Float,y:Float,color:Color,alpha:Float=1f){drawRoundRect(color.copy(alpha=alpha),Offset(x,y),Size(cell-1.dp.toPx(),cell-1.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRoundRect(Color.White.copy(alpha=.30f*alpha),Offset(x+1,y+1),Size(cell-3,cell-3),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),style=Stroke(.7.dp.toPx()))}
            val top=h*.065f;val left=w*.085f;for(r in 0 until n)for(c in 0 until n)tile(left+c*cell,top+r*cell,coral);drawLabel("n²",Offset(left+n*cell/2,top-18.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER);drawDimensionLine(Offset(left,top+n*cell+15.dp.toPx()),Offset(left+n*cell,top+n*cell+15.dp.toPx()),ink,"n",false)
            val right=w*.55f;for(r in 0 until n)for(c in 0 until n){val edge=r==n-1||c==n-1;tile(right+c*cell,top+r*cell,if(edge)amber else cyan)};drawLabel("(n − 1)²",Offset(right+n*cell/2,top-18.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER)
            drawLabel("↓",Offset(w*.50f,h*.39f),36.sp.value,ink,Paint.Align.CENTER);val midX=w*.14f;val midY=h*.44f;for(r in 0 until n)for(c in 0 until n){val edge=r==n-1||c==n-1;tile(midX+c*cell,midY+r*cell,if(edge)amber else cyan)}
            val movingX=w*.60f;val movingY=h*.48f;for(r in 0 until 2)for(c in 0 until (n-1)){val alpha=if(stage==1)1f else .45f;tile(movingX+c*cell,movingY+r*cell,amber,alpha)};drawLabel("↷",Offset(w*.78f,h*.58f),40.sp.value,Color(0xFF8C9092),Paint.Align.CENTER)
            drawLabel("↓",Offset(w*.50f,h*.69f),36.sp.value,ink,Paint.Align.CENTER);val barX=(w-difference*cell)/2;val barY=h*.76f;for(c in 0 until difference)tile(barX+c*cell,barY,if(c<n)amber else cyan);drawDimensionLine(Offset(barX,barY+cell+20.dp.toPx()),Offset(barX+difference*cell,barY+cell+20.dp.toPx()),ink,"2n − 1",false)
            val formula=Rect(w*.17f,h*.845f,w*.83f,h*.91f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFAAA6A0),formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("n² − (n − 1)² = 2n − 1",Offset(w*.50f,formula.center.y+10.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER)
            val control=Rect(w*.27f,h*.93f,w*.73f,h*.995f);drawRoundRect(Color.White,control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()));drawRoundRect(Color(0xFFE0DDD7),control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("‹",Offset(w*.32f,control.center.y+12.dp.toPx()),38.sp.value,ink,Paint.Align.CENTER);drawLabel("n = $n",Offset(w*.50f,control.center.y+9.dp.toPx()),26.sp.value,ink,Paint.Align.CENTER);drawLabel("›",Offset(w*.68f,control.center.y+12.dp.toPx()),38.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun ArithmeticSequenceSumScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:10.0).roundToInt().coerceIn(4,12);val a=(playback.frame.parameters["a"]?:2.0).coerceIn(1.0,20.0);val d=(playback.frame.parameters["b"]?:1.0).coerceIn(1.0,8.0);val last=a+(n-1)*d;var paired by remember{mutableStateOf(true)}
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        ProofLessonHeader("Arithmetic Sequence Sum","34 / 69",onBack)
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Arithmetic sequence with $n terms, first term ${a.roundToInt()}, common difference ${d.roundToInt()}, and last term ${last.roundToInt()}. Pairing is ${if(paired)"shown" else "reversed"}; two copies form an n by a plus l rectangle. Tap to reverse the second row or drag horizontally to change n."}
            .pointerInput(n){detectDragGestures{change,_->onParameterChange("n",(4+change.position.x/size.width*8).roundToInt().toDouble());change.consume()}}
            .pointerInput(Unit){detectTapGestures{paired=!paired}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF272D31);val palette=listOf(Color(0xFFFF514B),Color(0xFFFF762B),Color(0xFFFF9B24),Color(0xFFFFC62B),Color(0xFF26BCC7),Color(0xFF278ED8),Color(0xFF315DD1),Color(0xFF323290));val left=w*.105f;val right=w*.895f;val gap=(right-left)/(n-1).coerceAtLeast(1);val barW=min(30.dp.toPx(),gap*.70f);val low=38.dp.toPx();val high=145.dp.toPx();fun color(i:Int)=palette[(i*(palette.lastIndex.toFloat()/(n-1).coerceAtLeast(1))).roundToInt().coerceIn(0,palette.lastIndex)]
            fun barRow(baseY:Float,reverse:Boolean,alpha:Float=1f){for(i in 0 until n){val index=if(reverse)n-1-i else i;val bh=low+(high-low)*index/(n-1).coerceAtLeast(1);val x=left+i*gap-barW/2;drawRect(Color(0x22000000),Offset(x+3.dp.toPx(),baseY-bh+4.dp.toPx()),Size(barW,bh));drawRoundRect(color(index).copy(alpha=alpha),Offset(x,baseY-bh),Size(barW,bh),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))}}
            val topBase=h*.25f;barRow(topBase,false);drawLine(ink,Offset(left-15.dp.toPx(),topBase+10.dp.toPx()),Offset(right+15.dp.toPx(),topBase+10.dp.toPx()),1.dp.toPx());drawLabel("a",Offset(left,topBase+38.dp.toPx()),18.sp.value,ink,Paint.Align.CENTER);drawLabel("a + d",Offset(left+gap,topBase+38.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER);drawLabel("⋯",Offset(w*.5f,topBase+35.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawLabel("l",Offset(right,topBase+38.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);drawDimensionLine(Offset(left,topBase+65.dp.toPx()),Offset(right,topBase+65.dp.toPx()),ink,"n",false)
            drawLabel("↓",Offset(w*.50f,h*.38f),35.sp.value,ink,Paint.Align.CENTER);val secondBase=h*.56f;barRow(secondBase,paired);drawLine(ink,Offset(left-15.dp.toPx(),secondBase+9.dp.toPx()),Offset(right+15.dp.toPx(),secondBase+9.dp.toPx()),1.dp.toPx());drawLabel(if(paired)"l" else "a",Offset(left,secondBase-160.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);drawLabel(if(paired)"a" else "l",Offset(right,secondBase-55.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);drawDimensionLine(Offset(left,secondBase+36.dp.toPx()),Offset(right,secondBase+36.dp.toPx()),ink,"n",false)
            drawLabel("=",Offset(w*.50f,h*.665f),36.sp.value,ink,Paint.Align.CENTER);val rectTop=h*.71f;val rectHeight=h*.14f;val rectLeft=w*.19f;val rectWidth=w*.64f;val colW=rectWidth/n;for(i in 0 until n){val col=color(i);drawRect(col,Offset(rectLeft+i*colW,rectTop),Size(colW,rectHeight/2));drawRect(col.copy(alpha=.35f),Offset(rectLeft+i*colW,rectTop+rectHeight/2),Size(colW,rectHeight/2));drawRect(Color(0xFF55585A),Offset(rectLeft+i*colW,rectTop),Size(colW,rectHeight),style=Stroke(.6.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3f,3f))))};drawRect(ink,Offset(rectLeft,rectTop),Size(rectWidth,rectHeight),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5f,4f))));drawDimensionLine(Offset(rectLeft,rectTop+rectHeight+24.dp.toPx()),Offset(rectLeft+rectWidth,rectTop+rectHeight+24.dp.toPx()),ink,"n",false);drawLabel("a + l",Offset(w*.10f,rectTop+rectHeight/2+7.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER)
            val formula=Rect(w*.31f,h*.89f,w*.69f,h*.98f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(Color(0xFFD7D1C8),formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("Sₙ  =",Offset(w*.43f,formula.center.y+8.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawLabel("n(a + l)",Offset(w*.58f,formula.center.y-4.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER);drawLine(ink,Offset(w*.51f,formula.center.y+4.dp.toPx()),Offset(w*.65f,formula.center.y+4.dp.toPx()),1.5.dp.toPx());drawLabel("2",Offset(w*.58f,formula.center.y+31.dp.toPx()),21.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun EvenNumbersSumScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:4.0).roundToInt().coerceIn(2,7);val total=n*(n+1);var arranged by remember{mutableStateOf(true)};val colors=listOf(Color(0xFFFF6654),Color(0xFFFFBA27),Color(0xFF20B9CE),Color(0xFF5755CC),Color(0xFF327FD2),Color(0xFF2BB59E),Color(0xFF7551B9))
    fun select(v:Int)=onParameterChange("n",v.coerceIn(2,7).toDouble())
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF202224)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=40.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Sum of First n Even Numbers",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("35 / 69",fontSize=15.sp,color=Color.White);Text("⋮",fontSize=30.sp,color=Color.White,modifier=Modifier.padding(start=6.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="The first $n even numbers sum to $total. The rows are ${if(arranged)"rearranged into" else "ready to form"} a $n by ${n+1} rectangle. Tap the construction or use minus and plus."}
            .pointerInput(n){detectTapGestures{p->if(p.y>size.height*.90f){select(n+if(p.x>size.width*.5f)1 else -1)}else arranged=!arranged}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF25282C);val cell=min(w*.071f,h*.038f);drawLabel("2 + 4 + ⋯ + 2n",Offset(w*.50f,h*.065f),35.sp.value,ink,Paint.Align.CENTER)
            val rowsTop=h*.14f;for(row in 0 until n){val count=2*(row+1);val color=colors[row];val y=rowsTop+row*cell*1.65f;val horizontal=count-2;val x=w*.18f;for(c in 0 until horizontal)drawRoundRect(color,Offset(x+c*cell,y),Size(cell-2.dp.toPx(),cell-2.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()));val tailX=w*.74f;for(r in 0..1)drawRoundRect(color,Offset(tailX,y+(r-.5f)*cell),Size(cell-2.dp.toPx(),cell-2.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()));drawLine(Color(0xFF929394),Offset(tailX-5.dp.toPx(),y),Offset(x+horizontal*cell+4.dp.toPx(),y),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())))}
            drawLabel("⌄",Offset(w*.50f,h*.45f),43.sp.value,ink,Paint.Align.CENTER);val rectCell=min(w*.115f,w*.54f/(n+1));val rectW=(n+1)*rectCell;val rectH=n*rectCell;val rectX=(w-rectW)/2;val rectY=h*.50f;for(r in 0 until n)for(c in 0..n){val shift=if(arranged)0f else (c-r)*3.dp.toPx();drawRoundRect(colors[r],Offset(rectX+c*rectCell+shift,rectY+r*rectCell),Size(rectCell-2.dp.toPx(),rectCell-2.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))};drawDimensionLine(Offset(rectX,rectY+rectH+23.dp.toPx()),Offset(rectX+rectW,rectY+rectH+23.dp.toPx()),ink,"n + 1",false);drawDimensionLine(Offset(rectX+rectW+38.dp.toPx(),rectY),Offset(rectX+rectW+38.dp.toPx(),rectY+rectH),ink,"n",true)
            val formula=Rect(w*.30f,h*.80f,w*.70f,h*.87f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD8D4CE),formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("n(n + 1)",Offset(w*.50f,formula.center.y+10.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER)
            val control=Rect(w*.23f,h*.90f,w*.77f,h*.98f);drawRoundRect(Color.White,control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()));drawRoundRect(Color(0xFFDEDAD4),control.topLeft,control.size,androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()),style=Stroke(1.dp.toPx()));drawCircle(Color(0xFF45474C),22.dp.toPx(),Offset(w*.30f,control.center.y));drawLabel("−",Offset(w*.30f,control.center.y+9.dp.toPx()),28.sp.value,Color.White,Paint.Align.CENTER);drawLabel("n",Offset(w*.43f,control.center.y+9.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER);drawRoundRect(Color(0xFFFAF9F6),Offset(w*.49f,control.center.y-22.dp.toPx()),Size(60.dp.toPx(),44.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("$n",Offset(w*.49f+30.dp.toPx(),control.center.y+10.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER);drawCircle(Color(0xFF45474C),22.dp.toPx(),Offset(w*.70f,control.center.y));drawLabel("+",Offset(w*.70f,control.center.y+9.dp.toPx()),27.sp.value,Color.White,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun SquaresConsecutiveOddSumsScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val k=(playback.frame.parameters["n"]?:4.0).roundToInt().coerceIn(1,7);val odd=2*k-1;val square=k*k;val colors=listOf(Color(0xFFFF584C),Color(0xFFFFA41D),Color(0xFF12A9BF),Color(0xFF3536A2),Color(0xFF7A65C4),Color(0xFF3B79CC),Color(0xFF269F8F))
    fun select(v:Int)=onParameterChange("n",v.coerceIn(1,7).toDouble())
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF1C222A)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=40.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Squares as Consecutive Odd Sums",fontSize=17.sp,fontWeight=FontWeight.Bold,color=Color.White,maxLines=1);Text("36 / 69",fontSize=16.sp,color=Color.White)};Text("⋮",fontSize=31.sp,color=Color.White)}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Squares as consecutive odd sums. Stage k equals $k adds $odd tiles and forms $square. Tap any numbered stage or drag the stage rail."}
            .pointerInput(k){detectDragGestures{change,_->if(change.position.y>size.height*.54f&&change.position.y<size.height*.64f)select((1+change.position.x/size.width*6).roundToInt());change.consume()}}
            .pointerInput(k){detectTapGestures{p->if(p.y>size.height*.54f&&p.y<size.height*.64f)select((1+p.x/size.width*6).roundToInt()) else if(p.y<size.height*.54f)select(if(k==7)1 else k+1)}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF2B2E31);val maxStage=7;val gridCount=12;val cell=min(w*.067f,h*.037f);val side=gridCount*cell;val left=(w-side)/2;val top=h*.035f
            for(r in 0 until gridCount)for(c in 0 until gridCount){val depth=maxOf(2*k-r,c-(gridCount-2*k)+1);val inActive=depth>0;val stage=if(inActive)((depth+1)/2).coerceIn(1,k) else 0;val color=if(stage>0)colors[stage-1] else Color(0xFFF8F6F1);drawRoundRect(color,Offset(left+c*cell,top+r*cell),Size(cell-1.dp.toPx(),cell-1.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawRect(if(inActive)Color(0x55000000) else Color(0xFFD8D4CC),Offset(left+c*cell,top+r*cell),Size(cell-1.dp.toPx(),cell-1.dp.toPx()),style=Stroke(.6.dp.toPx()))}
            drawDimensionLine(Offset(left-30.dp.toPx(),top),Offset(left-30.dp.toPx(),top+side),ink,"n",true);drawDimensionLine(Offset(left,top+side+25.dp.toPx()),Offset(left+side,top+side+25.dp.toPx()),ink,"n",false)
            val railY=h*.59f;drawLine(Color(0xFFBBB8B2),Offset(w*.08f,railY),Offset(w*.92f,railY),2.dp.toPx());for(stage in 1..7){val x=w*(.08f+(stage-1)/6f*.84f);drawCircle(if(stage==k)colors[stage-1] else ProofIvory,if(stage==k)22.dp.toPx() else 18.dp.toPx(),Offset(x,railY));drawCircle(if(stage<=k)colors[stage-1] else Color(0xFFCAC7C1),if(stage==k)24.dp.toPx() else 15.dp.toPx(),Offset(x,railY),style=if(stage==k)Stroke(4.dp.toPx()) else Stroke(2.dp.toPx()));drawLabel("$stage",Offset(x,railY+7.dp.toPx()),17.sp.value,if(stage==k)Color.White else if(stage<=k)colors[stage-1] else Color(0xFF777777),Paint.Align.CENTER)}
            val cardTop=h*.67f;val leftCard=Rect(w*.055f,cardTop,w*.48f,cardTop+h*.105f);val rightCard=Rect(w*.52f,cardTop,w*.945f,cardTop+h*.105f);listOf(leftCard,rightCard).forEach{drawRoundRect(Color.White,it.topLeft,it.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD7D2CA),it.topLeft,it.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()))};drawLabel("2k − 1",Offset(leftCard.center.x,leftCard.top+38.dp.toPx()),29.sp.value,colors[k-1],Paint.Align.CENTER);drawRect(colors[k-1],Offset(leftCard.left,leftCard.bottom-42.dp.toPx()),Size(leftCard.width,42.dp.toPx()));drawLabel("$odd",Offset(leftCard.center.x,leftCard.bottom-13.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER);drawLabel("k²",Offset(rightCard.center.x,rightCard.top+38.dp.toPx()),29.sp.value,ink,Paint.Align.CENTER);drawRect(Color(0xFF30343A),Offset(rightCard.left,rightCard.bottom-42.dp.toPx()),Size(rightCard.width,42.dp.toPx()));drawLabel("$square",Offset(rightCard.center.x,rightCard.bottom-13.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER)
            val formula=Rect(w*.05f,h*.86f,w*.95f,h*.95f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD7D2CA),formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("1 + 3 + ⋯ + (2n − 1) = n²",Offset(w*.50f,formula.center.y+10.dp.toPx()),25.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun ConsecutiveIntegersSumScreen(playback:ProofPlayback,onBack:()->Unit,onParameterChange:(String,Double)->Unit) {
    val n=(playback.frame.parameters["n"]?:7.0).roundToInt().coerceIn(5,9);val k=(playback.frame.parameters["a"]?:3.0).roundToInt().coerceIn(1,12);val result=n*(2*k+n-1)/2;var selectedPair by remember{mutableStateOf(0)};val colors=listOf(Color(0xFFFF5A55),Color(0xFFF28C28),Color(0xFFF4B51B),Color(0xFF1FAEBA),Color(0xFF2E8ED4),Color(0xFF4164CC),Color(0xFF9254B7))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF1C222A)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=40.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Sum of Consecutive Integers",fontSize=18.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("37 / 69",fontSize=15.sp,color=Color.White);Text("⋮",fontSize=30.sp,color=Color.White,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Sum of $n consecutive integers beginning at $k equals $result. Pair ${selectedPair+1} is highlighted; every outer pair has the same sum. Tap a pairing arc or drag horizontally to change k."}
            .pointerInput(Unit){detectDragGestures{change,_->onParameterChange("a",(1+change.position.x/size.width*11).roundToInt().toDouble());change.consume()}}
            .pointerInput(Unit){detectTapGestures{p->selectedPair=when{p.y<size.height*.30f->0;p.y<size.height*.54f->1;p.y<size.height*.67f->2;else->3}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF273039);val count=7;val colW=w*.10f;val gap=w*.025f;val total=count*colW+(count-1)*gap;val start=(w-total)/2;val top=h*.19f;val colH=h*.38f
            fun column(index:Int){val x=start+index*(colW+gap);val color=colors[index];drawRoundRect(color.copy(alpha=.14f),Offset(x,top),Size(colW,colH),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()));drawRoundRect(color,Offset(x,top),Size(colW,colH),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),style=Stroke(if(index==selectedPair||index==6-selectedPair)2.dp.toPx() else 1.dp.toPx()));for(s in 1..4)drawLine(Color.White,Offset(x,top+s*colH/5),Offset(x+colW,top+s*colH/5),1.dp.toPx());val labels=when(index){0->listOf("k","k+1","k+2","⋮","k+n−1");1->listOf("k+1","k+2","k+3","⋮","k+n−2");2->listOf("k+2","k+3","k+4","⋮","k+n−3");3->listOf("k+m","","⋮","","k+m");4->listOf("k+n−3","k+n−4","k+n−5","⋮","k+2");5->listOf("k+n−2","k+n−3","k+n−4","⋮","k+1");else->listOf("k+n−1","k+n−2","k+n−3","⋮","k")};labels.forEachIndexed{s,label->drawLabel(label,Offset(x+colW/2,top+(s+.58f)*colH/5),if(label.length>5)12.sp.value else 14.sp.value,if(s<3)color else ink,Paint.Align.CENTER)}};for(i in 0 until count)column(i)
            drawLabel("k",Offset(start+colW/2,top-35.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);drawLabel("k+1",Offset(start+colW+gap+colW/2,top-35.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);drawLabel("⋯",Offset(w*.50f,top-35.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);drawLabel("k+n−1",Offset(start+6*(colW+gap)+colW/2,top-35.dp.toPx()),18.sp.value,ink,Paint.Align.CENTER)
            val centerX=w*.5f;for(pair in 0..3){val lx=start+pair*(colW+gap)+colW/2;val rx=start+(6-pair)*(colW+gap)+colW/2;val y=when(pair){0->top-10.dp.toPx();1->top+colH*.06f;2->top+colH*.19f;else->top+colH+55.dp.toPx()};val controlY=if(pair<3)y-55.dp.toPx()-pair*8.dp.toPx() else y+48.dp.toPx();val path=Path().apply{moveTo(lx,y);quadraticTo(centerX,controlY,rx,y)};drawPath(path,colors[pair],style=Stroke(if(pair==selectedPair)3.dp.toPx() else 1.6.dp.toPx()));drawCircle(colors[pair],4.dp.toPx(),Offset(lx,y));drawCircle(colors[pair],4.dp.toPx(),Offset(rx,y));drawCircle(colors[pair],14.dp.toPx(),Offset(centerX,if(pair<3)controlY else controlY));drawLabel("✓",Offset(centerX,if(pair<3)controlY+6.dp.toPx() else controlY+6.dp.toPx()),17.sp.value,Color.White,Paint.Align.CENTER)}
            drawLine(Color(0xFF56B8C7),Offset(centerX,top-35.dp.toPx()),Offset(centerX,top+colH+115.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())))
            val formula=Rect(w*.17f,h*.78f,w*.83f,h*.93f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFF35AFC0),formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),5.dp.toPx()))));drawLabel("n (2k + n − 1)",Offset(w*.50f,formula.center.y-2.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER);drawLine(ink,Offset(w*.34f,formula.center.y+9.dp.toPx()),Offset(w*.66f,formula.center.y+9.dp.toPx()),1.6.dp.toPx());drawLabel("2",Offset(w*.50f,formula.center.y+48.dp.toPx()),26.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByTwoScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(4,7,3,8))};var routed by remember{mutableStateOf(false)};var selected by remember{mutableStateOf(3)};val even=digits.last()%2==0;val colors=listOf(Color(0xFF376BA7),Color(0xFF14949D),Color(0xFFE39422),Color(0xFFE65D4D))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF181A1C)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=40.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Divisibility Rule for 2",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("38 / 69",fontSize=16.sp,color=Color(0xFFD3D3D3))};Text("⋮",fontSize=31.sp,color=Color.White)}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Divisibility by 2 machine for ${digits.joinToString("")}. The last digit is ${digits.last()}, so the number is ${if(even)"even and divisible by 2" else "odd and not divisible by 2"}. Tap a digit card to change it, then tap the large last digit to route it to the correct tray."}
            .pointerInput(digits,routed){detectTapGestures{p->when{p.y<size.height*.22f->{val i=(p.x/(size.width/4f)).toInt().coerceIn(0,3);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10};selected=i;routed=false};p.y in size.height*.38f..size.height*.63f->{selected=3;routed=true};p.y>size.height*.68f->routed=false}}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF292D30);val panel=Rect(w*.07f,h*.045f,w*.93f,h*.225f);drawRoundRect(Color(0xFF25292C),panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()));drawRoundRect(Color(0xFF55595B),panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),style=Stroke(2.dp.toPx()));val cardW=w*.165f;val gap=w*.035f;val first=w*.12f
            for(i in 0..3){val x=first+i*(cardW+gap);val card=Rect(x,h*.067f,x+cardW,h*.195f);drawRoundRect(colors[i],card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(if(i==selected)Color.White else Color(0x66FFFFFF),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(if(i==selected)2.dp.toPx() else 1.dp.toPx()));drawLabel("${digits[i]}",Offset(card.center.x,card.top+62.dp.toPx()),42.sp.value,Color.White,Paint.Align.CENTER);drawLine(Color.White.copy(alpha=.42f),Offset(card.left+14.dp.toPx(),card.bottom-32.dp.toPx()),Offset(card.right-14.dp.toPx(),card.bottom-32.dp.toPx()),1.dp.toPx());drawCircle(Color.White.copy(alpha=.8f),4.dp.toPx(),Offset(card.center.x,card.bottom-16.dp.toPx()))}
            val fadedY=h*.29f;for(i in 0..3){val x=first+i*(cardW+gap);drawRoundRect(colors[i].copy(alpha=.20f),Offset(x,fadedY),Size(cardW,74.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFF9B9C9C),Offset(x,fadedY),Size(cardW,74.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));drawLabel("${digits[i]}",Offset(x+cardW/2,fadedY+50.dp.toPx()),31.sp.value,Color.White,Paint.Align.CENTER);drawLine(Color(0xFF929494),Offset(x+cardW/2,panel.bottom),Offset(x+cardW/2,fadedY-8.dp.toPx()),1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())))}
            for(group in 0..1){val cx=if(group==0)w*.27f else w*.62f;drawLabel("÷2",Offset(cx,h*.43f),30.sp.value,ink.copy(alpha=.70f),Paint.Align.CENTER);drawLabel(if(group==0)"${(digits[0]*10+digits[1])/2}" else "${(digits[2]*10+digits[3])/2}",Offset(cx,h*.50f),28.sp.value,Color(0xFF9A9A9A),Paint.Align.CENTER)}
            val activeCenter=Offset(w*.82f,h*.50f);if(!routed){drawRoundRect(colors[3],activeCenter-Offset(39.dp.toPx(),39.dp.toPx()),Size(78.dp.toPx(),78.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("${digits.last()}",activeCenter+Offset(0f,15.dp.toPx()),41.sp.value,Color.White,Paint.Align.CENTER);drawLine(colors[3],Offset(activeCenter.x,panel.bottom),Offset(activeCenter.x,activeCenter.y-44.dp.toPx()),2.dp.toPx())}
            val trayTop=h*.68f;val tray=Rect(w*.07f,trayTop,w*.93f,h*.98f);drawRoundRect(Color(0xFF25292C),tray.topLeft,tray.size,androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()));val leftTray=Rect(w*.105f,trayTop+42.dp.toPx(),w*.47f,h*.945f);val rightTray=Rect(w*.53f,trayTop+42.dp.toPx(),w*.895f,h*.945f);drawRoundRect(Color(0xFF0E2234),leftTray.topLeft,leftTray.size,androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()));drawRoundRect(Color(0xFF286CA9),leftTray.topLeft,leftTray.size,androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),style=Stroke(2.dp.toPx()));drawRoundRect(Color(0xFF35190F),rightTray.topLeft,rightTray.size,androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()));drawRoundRect(Color(0xFFFF862D),rightTray.topLeft,rightTray.size,androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),style=Stroke(2.dp.toPx()));drawCircle(Color(0xFF377EC0),23.dp.toPx(),Offset(leftTray.center.x,trayTop+24.dp.toPx()));drawLabel("✓",Offset(leftTray.center.x,trayTop+33.dp.toPx()),26.sp.value,Color.White,Paint.Align.CENTER);drawCircle(Color(0xFFE66B35),23.dp.toPx(),Offset(rightTray.center.x,trayTop+24.dp.toPx()));drawLabel("×",Offset(rightTray.center.x,trayTop+34.dp.toPx()),29.sp.value,Color.White,Paint.Align.CENTER)
            if(routed){val target=if(even)leftTray.center else rightTray.center;drawRoundRect(colors[3],target-Offset(38.dp.toPx(),38.dp.toPx()),Size(76.dp.toPx(),76.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("${digits.last()}",target+Offset(0f,15.dp.toPx()),41.sp.value,Color.White,Paint.Align.CENTER);drawLine(if(even)Color(0xFF4B91C9) else Color(0xFFFF7C35),activeCenter,target-Offset(0f,48.dp.toPx()),4.dp.toPx(),StrokeCap.Round)}
        }
    }
}

@Composable
private fun DivisibilityByThreeScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(4,7,2,5))};var stage by remember{mutableStateOf(0)};val sum=digits.sum();val divisible=sum%3==0;val colors=listOf(Color(0xFFEC5D48),Color(0xFFF0A517),Color(0xFF22AEB9),Color(0xFF5842B7));val places=listOf("1000","100","10","1")
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF1A1F25)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=38.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Divisibility Rule for 3",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("39 / 69",fontSize=15.sp,color=Color.White);Text("⋮",fontSize=29.sp,color=Color.White,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Divisibility by 3 proof for ${digits.joinToString("")}. Digit sum is $sum, which is ${if(divisible)"divisible" else "not divisible"} by 3. Stage ${stage+1} of 4 is highlighted. Tap a place-value panel to change its digit or tap elsewhere to advance."}
            .pointerInput(digits){detectTapGestures{p->if(p.y<size.height*.20f){val i=(p.x/(size.width/4f)).toInt().coerceIn(0,3);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10};stage=0}else stage=(stage+1)%4}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF273039);val colW=w*.205f;val gap=w*.025f;val start=w*.057f;val top=h*.03f
            for(i in 0..3){val x=start+i*(colW+gap);val panel=Rect(x,top,x+colW,top+h*.16f);drawRoundRect(colors[i].copy(alpha=.035f),panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(colors[i].copy(alpha=if(stage==0)1f else .45f),panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(if(stage==0)1.7.dp.toPx() else 1.dp.toPx()));val badge=Rect(panel.center.x-29.dp.toPx(),panel.top-14.dp.toPx(),panel.center.x+29.dp.toPx(),panel.top+16.dp.toPx());drawRoundRect(colors[i],badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLabel(places[i],Offset(badge.center.x,badge.center.y+7.dp.toPx()),16.sp.value,Color.White,Paint.Align.CENTER)
                val count=when(i){0->10;1->8;2->2;else->digits[i]};val unit=min(10.dp.toPx(),panel.width/12);when(i){0->{for(r in 0 until 8)for(c in 0 until 8)drawRect(colors[i],Offset(panel.left+18.dp.toPx()+c*unit,panel.top+38.dp.toPx()+r*unit),Size(unit-1,unit-1),style=Stroke(.7.dp.toPx()))};1->{for(c in 0 until count)drawRect(colors[i],Offset(panel.left+14.dp.toPx()+c*unit,panel.top+43.dp.toPx()),Size(unit*.7f,panel.height*.58f),style=Stroke(1.dp.toPx()))};2->{for(c in 0 until count)for(r in 0 until 7)drawRect(colors[i],Offset(panel.left+32.dp.toPx()+c*unit*2,panel.top+42.dp.toPx()+r*unit),Size(unit-1,unit-1))};else->{for(r in 0 until count)drawRect(colors[i],Offset(panel.center.x-5.dp.toPx(),panel.top+35.dp.toPx()+r*14.dp.toPx()),Size(10.dp.toPx(),10.dp.toPx()))}}
                drawLabel("↓",Offset(panel.center.x,panel.bottom+26.dp.toPx()),25.sp.value,ink.copy(alpha=.65f),Paint.Align.CENTER)}
            val cloudTop=h*.235f;for(i in 0..3){val x=start+i*(colW+gap);val dotCount=when(i){0->64;1->56;2->14;else->digits[i]};val cols=when(i){0->8;1->7;2->2;else->1};val spacing=min(10.dp.toPx(),colW/(cols+1));for(index in 0 until dotCount){val r=index/cols;val c=index%cols;drawCircle(colors[i],3.4.dp.toPx(),Offset(x+colW*.18f+c*spacing,cloudTop+r*spacing))};val boxTop=h*.39f;drawRoundRect(Color.Transparent,Offset(x+colW*.12f,boxTop),Size(colW*.76f,h*.16f),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(if(stage==1)1.8.dp.toPx() else 1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));val rem=digits[i]%3;for(r in 0 until rem)drawCircle(colors[i],4.dp.toPx(),Offset(x+colW*.5f,boxTop+35.dp.toPx()+r*13.dp.toPx()));val digitBadge=Rect(x+colW*.30f,boxTop+h*.135f,x+colW*.70f,boxTop+h*.18f);drawRoundRect(colors[i],digitBadge.topLeft,digitBadge.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLabel("${digits[i]}",Offset(digitBadge.center.x,digitBadge.center.y+8.dp.toPx()),20.sp.value,Color.White,Paint.Align.CENTER)}
            drawLabel("↓",Offset(w*.50f,h*.60f),28.sp.value,ink,Paint.Align.CENTER);val groupTop=h*.64f;for(g in 0 until 6){val cx=w*(.13f+g*.148f);drawCircle(Color.Transparent,33.dp.toPx(),Offset(cx,groupTop),style=Stroke(if(stage==2)1.8.dp.toPx() else 1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx()))));for(i in 0..2)drawCircle(colors[(g+i)%4],4.5.dp.toPx(),Offset(cx,groupTop-13.dp.toPx()+i*13.dp.toPx()))}
            val cardsY=h*.745f;for(i in 0..3){val cx=w*(.13f+i*.15f);drawRoundRect(colors[i],Offset(cx-23.dp.toPx(),cardsY-25.dp.toPx()),Size(46.dp.toPx(),50.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("${digits[i]}",Offset(cx,cardsY+10.dp.toPx()),23.sp.value,Color.White,Paint.Align.CENTER);if(i<3){drawLine(ink,Offset(cx+25.dp.toPx(),cardsY),Offset(cx+w*.15f-25.dp.toPx(),cardsY),2.dp.toPx());drawCircle(ProofIvory,5.dp.toPx(),Offset(cx+w*.075f,cardsY));drawCircle(ink,5.dp.toPx(),Offset(cx+w*.075f,cardsY),style=Stroke(1.dp.toPx()))}}
            val sumCard=Rect(w*.66f,cardsY-25.dp.toPx(),w*.96f,cardsY+25.dp.toPx());drawRoundRect(Color(0xFF2E3439),sumCard.topLeft,sumCard.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("${digits.joinToString(" + ")} = $sum",Offset(sumCard.center.x,sumCard.center.y+7.dp.toPx()),13.sp.value,Color.White,Paint.Align.CENTER)
            drawLabel("↓",Offset(w*.55f,h*.815f),28.sp.value,ink,Paint.Align.CENTER);val resultCard=Rect(w*.35f,h*.85f,w*.75f,h*.94f);drawRoundRect(Color(0xFF293037),resultCard.topLeft,resultCard.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel(if(divisible)"$sum ÷ 3 = ${sum/3}" else "$sum ÷ 3 leaves ${sum%3}",Offset(resultCard.center.x,resultCard.center.y+12.dp.toPx()),27.sp.value,Color.White,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByFourScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(8,3,1,2,4))};var grouped by remember{mutableStateOf(true)};val lastTwo=digits[3]*10+digits[4];val divisible=lastTwo%4==0;val places=listOf("10000","1000","100","10","1")
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF1A1F25)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=39.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Divisibility Rule for 4",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("40 / 69",fontSize=15.sp,color=Color(0xFFD2D2D2))};Text("⋮",fontSize=30.sp,color=Color.White)}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Divisibility by 4 proof for ${digits.joinToString("")}. Only the last two digits matter: $lastTwo is ${if(divisible)"divisible" else "not divisible"} by 4${if(divisible)", giving ${lastTwo/4} groups" else ", leaving remainder ${lastTwo%4}"}. Tap a digit to change it or tap the blocks to ${if(grouped)"ungroup" else "group"} them."}
            .pointerInput(digits){detectTapGestures{p->if(p.y<size.height*.18f){val i=(p.x/(size.width/5f)).toInt().coerceIn(0,4);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10}}else grouped=!grouped}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF292D31);val blue=Color(0xFF3268AC);val amber=Color(0xFFFFB329);val coral=Color(0xFFF06443);val cardW=w*.14f;val gap=w*.035f;val first=w*.065f;val cardTop=h*.045f
            for(i in 0..4){val x=first+i*(cardW+gap);val selected=i>=3;val card=Rect(x,cardTop,x+cardW,cardTop+h*.095f);drawRoundRect(if(selected)Color(0xFFFFE393) else Color(0xFF333537),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(if(selected)amber else Color(0xFF6A6D6F),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(if(selected)2.dp.toPx() else 1.dp.toPx()));drawLabel("${digits[i]}",Offset(card.center.x,card.center.y+18.dp.toPx()),35.sp.value,if(selected)Color.Black else Color(0xFFD1D1D1),Paint.Align.CENTER);drawLabel(places[i],Offset(card.center.x,card.bottom+24.dp.toPx()),13.sp.value,ink.copy(alpha=.75f),Paint.Align.CENTER)}
            val columnsTop=h*.22f;for(i in 0..3){val cx=first+i*(cardW+gap)+cardW/2;drawLine(Color(0xFF777A7C),Offset(cx,cardTop+h*.13f),Offset(cx,columnsTop+h*.29f),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),5.dp.toPx())));val count=digits[i].coerceAtLeast(1);for(j in 0 until count.coerceAtMost(8)){val y=columnsTop+j*33.dp.toPx();drawRoundRect(blue,Offset(cx-20.dp.toPx(),y),Size(40.dp.toPx(),27.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));for(g in 1..4)drawLine(Color.White.copy(alpha=.4f),Offset(cx-20.dp.toPx()+g*8.dp.toPx(),y),Offset(cx-20.dp.toPx()+g*8.dp.toPx(),y+27.dp.toPx()),.5.dp.toPx())}}
            val groupY=h*.56f;for(i in 0..3){val cx=first+i*(cardW+gap)+cardW/2;drawRoundRect(Color.White,Offset(cx-35.dp.toPx(),groupY),Size(70.dp.toPx(),70.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawRoundRect(blue,Offset(cx-35.dp.toPx(),groupY),Size(70.dp.toPx(),70.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(1.dp.toPx()));for(r in 0..1)for(c in 0..1)drawRoundRect(blue.copy(alpha=if(grouped)1f else .45f),Offset(cx-29.dp.toPx()+c*31.dp.toPx(),groupY+6.dp.toPx()+r*31.dp.toPx()),Size(25.dp.toPx(),25.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))}
            val merged=Rect(w*.18f,h*.72f,w*.59f,h*.84f);drawRoundRect(Color.White,merged.topLeft,merged.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(blue,merged.topLeft,merged.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.4.dp.toPx()));for(r in 0..2)for(c in 0..4)drawRoundRect(blue.copy(alpha=if(grouped)1f else .4f),Offset(merged.left+10.dp.toPx()+c*34.dp.toPx(),merged.top+9.dp.toPx()+r*29.dp.toPx()),Size(28.dp.toPx(),23.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));drawLabel("× 4",Offset(merged.center.x,merged.bottom+30.dp.toPx()),21.sp.value,blue,Paint.Align.CENTER)
            val rightX=w*.82f;drawLine(amber,Offset(rightX,cardTop+h*.13f),Offset(rightX,h*.89f),2.dp.toPx());val lastCard=Rect(rightX-36.dp.toPx(),h*.41f,rightX+36.dp.toPx(),h*.47f);drawRoundRect(Color(0xFFFFE58B),lastCard.topLeft,lastCard.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(amber,lastCard.topLeft,lastCard.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel("$lastTwo",Offset(lastCard.center.x,lastCard.center.y+12.dp.toPx()),28.sp.value,Color.Black,Paint.Align.CENTER)
            val groups=if(grouped)lastTwo/4 else 1;for(g in 0 until groups.coerceAtMost(6)){val y=h*.53f+g*34.dp.toPx();drawRoundRect(Color.Transparent,Offset(rightX-46.dp.toPx(),y),Size(92.dp.toPx(),25.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx()))));for(c in 0..3)drawRoundRect(coral,Offset(rightX-36.dp.toPx()+c*20.dp.toPx(),y+4.dp.toPx()),Size(13.dp.toPx(),17.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))}
            val result=Rect(w*.66f,h*.87f,w*.96f,h*.95f);drawRoundRect(Color(0xFFFFF0B5),result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(amber,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel(if(divisible)"$lastTwo ÷ 4 = ${lastTwo/4}" else "$lastTwo ÷ 4 r ${lastTwo%4}",Offset(result.center.x,result.center.y+10.dp.toPx()),24.sp.value,Color.Black,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByEightScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(1,2,3,7,6))};var grouped by remember{mutableStateOf(true)};val lastThree=digits[2]*100+digits[3]*10+digits[4];val quotient=lastThree/8;val remainder=lastThree%8;val divisible=remainder==0
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(76.dp).background(Color(0xFF181B1E)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=39.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Divisibility Rule for 8",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White);Text("41 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=Color.White)};Text("⋮",fontSize=30.sp,color=Color.White)}
        Canvas(Modifier.fillMaxWidth().weight(1f)
            .semantics{contentDescription="Divisibility by 8 proof for ${digits.joinToString("")}. The last three digits are $lastThree; division by 8 gives $quotient with remainder $remainder, so the number is ${if(divisible)"divisible" else "not divisible"} by 8. Tap a digit to edit it or tap the blocks to ${if(grouped)"separate" else "group"} them."}
            .pointerInput(digits){detectTapGestures{p->if(p.y<size.height*.17f){val i=(p.x/(size.width/5f)).toInt().coerceIn(0,4);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10}}else grouped=!grouped}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF252B30);val blue=Color(0xFF2574BD);val coral=Color(0xFFE95B45);val amber=Color(0xFFFFB52A);val cardW=w*.115f;val gap=w*.015f;val total=5*cardW+4*gap;val start=(w-total)/2;val top=h*.045f
            for(i in 0..4){val x=start+i*(cardW+gap);val highlighted=i>=2;val card=Rect(x,top,x+cardW,top+h*.085f);drawRoundRect(if(highlighted)coral else Color(0xFF292B2D),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawRoundRect(if(highlighted)Color(0xFFFF8B72) else Color(0xFF5C5E60),card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(if(highlighted)1.8.dp.toPx() else 1.dp.toPx()));drawLabel("${digits[i]}",Offset(card.center.x,card.center.y+17.dp.toPx()),34.sp.value,Color.White,Paint.Align.CENTER)};val bracketLeft=start+2*(cardW+gap)-8.dp.toPx();val bracketRight=start+5*cardW+4*gap+8.dp.toPx();drawLine(coral,Offset(bracketLeft,top-10.dp.toPx()),Offset(bracketRight,top-10.dp.toPx()),3.dp.toPx(),StrokeCap.Round);drawLine(coral,Offset(bracketLeft,top-10.dp.toPx()),Offset(bracketLeft,top+3.dp.toPx()),3.dp.toPx());drawLine(coral,Offset(bracketRight,top-10.dp.toPx()),Offset(bracketRight,top+3.dp.toPx()),3.dp.toPx());drawLabel("≡",Offset((bracketLeft+bracketRight)/2,top+h*.13f),23.sp.value,coral,Paint.Align.CENTER)
            val leftTop=h*.22f;for(r in 0..1)for(c in 0..4){val x=w*.06f+c*w*.105f;val y=leftTop+r*62.dp.toPx();drawRoundRect(blue,Offset(x,y),Size(48.dp.toPx(),48.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));for(g in 1..4)drawLine(Color.White.copy(alpha=.35f),Offset(x+g*9.6.dp.toPx(),y),Offset(x+g*9.6.dp.toPx(),y+48.dp.toPx()),.5.dp.toPx())}
            val stacksTop=h*.42f;for(r in 0..1)for(c in 0..2){val x=w*.06f+c*w*.16f;val y=stacksTop+r*82.dp.toPx();drawRoundRect(Color.Transparent,Offset(x,y),Size(78.dp.toPx(),68.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));for(s in 0..3)drawRoundRect(blue.copy(alpha=if(grouped)1f else .45f),Offset(x+10.dp.toPx(),y+44.dp.toPx()-s*10.dp.toPx()),Size(58.dp.toPx(),14.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))}
            for(i in 0 until 6){val x=w*.065f+i*w*.08f;drawLine(Color(0xFFAAA9A6),Offset(x,h*.62f),Offset(x,h*.66f),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx())));drawRoundRect(Color.Transparent,Offset(x-14.dp.toPx(),h*.66f),Size(28.dp.toPx(),28.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),3.dp.toPx()))));drawLabel("✳",Offset(x,h*.66f+20.dp.toPx()),14.sp.value,Color(0xFFAAA9A6),Paint.Align.CENTER)}
            val redGridX=w*.67f;val redGridY=h*.22f;for(index in 0 until 80){val r=index/10;val c=index%10;drawRoundRect(coral,Offset(redGridX+c*13.dp.toPx(),redGridY+r*13.dp.toPx()),Size(8.dp.toPx(),8.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))};drawLabel("→",Offset(w*.60f,h*.36f),35.sp.value,Color(0xFF888A8A),Paint.Align.CENTER)
            val barTop=h*.43f;for(g in 0 until quotient.coerceAtMost(24)){val col=g%8;val row=g/8;val x=w*.66f+col*14.dp.toPx();val y=barTop+row*73.dp.toPx();drawRoundRect(amber,Offset(x,y),Size(11.dp.toPx(),60.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),style=Stroke(1.2.dp.toPx()));for(u in 0 until 8)drawRoundRect(coral.copy(alpha=if(grouped)1f else .55f),Offset(x+2.dp.toPx(),y+3.dp.toPx()+u*7.dp.toPx()),Size(7.dp.toPx(),5.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))};if(remainder>0)for(i in 0 until remainder)drawRoundRect(coral,Offset(w*.68f+i*24.dp.toPx(),h*.73f),Size(13.dp.toPx(),13.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            val result=Rect(w*.10f,h*.84f,w*.90f,h*.94f);drawRoundRect(Color.Transparent,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));val tokens=listOf("$lastThree","÷","8","=","$quotient");val tokenColors=listOf(Color(0xFF292B2D),Color(0xFF24A9C3),Color(0xFF292B2D),Color(0xFF24A9C3),Color(0xFF5246B8));val widths=listOf(.30f,.12f,.12f,.12f,.22f);var x=result.left;tokens.forEachIndexed{i,t->val ww=result.width*widths[i];drawRoundRect(tokenColors[i],Offset(x,result.top),Size(ww-3.dp.toPx(),result.height),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel(t,Offset(x+ww/2,result.center.y+17.dp.toPx()),if(i==0||i==4)32.sp.value else 27.sp.value,Color.White,Paint.Align.CENTER);x+=ww};if(!divisible)drawLabel("remainder $remainder",Offset(result.center.x,result.bottom+25.dp.toPx()),17.sp.value,coral,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByNineScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(7,4,5,2))};var grouped by remember{mutableStateOf(true)};val sum=digits.sum();val quotient=sum/9;val remainder=sum%9;val colors=listOf(Color(0xFFEC5948),Color(0xFFF08A20),Color(0xFF18AEB6),Color(0xFF405EAF))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).background(Color(0xFF191D22)).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=38.sp,color=Color.White,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Divisibility Rule for 9",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("42 / 69",fontSize=15.sp,color=Color.White);Text("⋮",fontSize=29.sp,color=Color.White,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Divisibility by 9 proof for ${digits.joinToString("")}. Digit sum $sum gives $quotient groups of 9 and remainder $remainder, so it is ${if(remainder==0)"divisible" else "not divisible"} by 9. Tap a digit to edit it or tap the groups to ${if(grouped)"scatter" else "assemble"} them."}.pointerInput(digits){detectTapGestures{p->if(p.y<size.height*.18f){val i=(p.x/(size.width/4f)).toInt().coerceIn(0,3);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10}}else grouped=!grouped}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF292D31);val cardW=w*.14f;val gap=w*.035f;val total=4*cardW+3*gap;val start=(w-total)/2;val top=h*.035f
            for(i in 0..3){val x=start+i*(cardW+gap);val card=Rect(x,top,x+cardW,top+h*.085f);drawRoundRect(colors[i],card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("${digits[i]}",Offset(card.center.x,card.center.y+17.dp.toPx()),35.sp.value,Color.White,Paint.Align.CENTER);drawLabel("↓",Offset(card.center.x,card.bottom+25.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);for(j in 0 until digits[i]){val cols=2;drawCircle(colors[i],5.dp.toPx(),Offset(card.center.x-10.dp.toPx()+(j%cols)*20.dp.toPx(),card.bottom+47.dp.toPx()+(j/cols)*20.dp.toPx()))}}
            val tray=Rect(w*.19f,h*.28f,w*.81f,h*.36f);drawRoundRect(Color.White,tray.topLeft,tray.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFF8C8D8D),tray.topLeft,tray.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()));for(i in 0 until sum){val c=i%18;val r=i/18;drawCircle(colors[i%4],4.5.dp.toPx(),Offset(tray.left+15.dp.toPx()+c*14.dp.toPx(),tray.top+19.dp.toPx()+r*24.dp.toPx()))};drawLabel("↓",Offset(w*.50f,h*.39f),25.sp.value,ink,Paint.Align.CENTER)
            for(g in 0 until quotient.coerceAtMost(2)){val box=Rect(w*(.28f+g*.25f),h*.42f,w*(.45f+g*.25f),h*.53f);drawRoundRect(Color.Transparent,box.topLeft,box.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));for(i in 0 until 9){val c=i%3;val r=i/3;drawCircle(colors[(i+g)%4].copy(alpha=if(grouped)1f else .5f),6.dp.toPx(),Offset(box.left+18.dp.toPx()+c*23.dp.toPx(),box.top+18.dp.toPx()+r*23.dp.toPx()))}};val remBox=Rect(w*.43f,h*.56f,w*.57f,h*.65f);drawRoundRect(Color.Transparent,remBox.topLeft,remBox.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));drawLabel("$remainder",Offset(remBox.center.x,remBox.center.y+18.dp.toPx()),38.sp.value,Color(0xFF343434),Paint.Align.CENTER)
            val beadY=h*.73f;for(i in 0..3){val cx=w*(.16f+i*.18f);drawCircle(colors[i],25.dp.toPx(),Offset(cx,beadY));drawLabel("${digits[i]}",Offset(cx,beadY+10.dp.toPx()),24.sp.value,Color.White,Paint.Align.CENTER);if(i<3)drawLabel("+",Offset(cx+w*.09f,beadY+9.dp.toPx()),22.sp.value,ink,Paint.Align.CENTER)};drawLabel("=",Offset(w*.80f,beadY+9.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);drawCircle(Color(0xFF514EC9),27.dp.toPx(),Offset(w*.90f,beadY));drawLabel("$sum",Offset(w*.90f,beadY+10.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER)
            val sumCard=Rect(w*.20f,h*.78f,w*.80f,h*.84f);drawRoundRect(Color(0xFF282B2E),sumCard.topLeft,sumCard.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("${digits.joinToString(" + ")} = $sum",Offset(sumCard.center.x,sumCard.center.y+9.dp.toPx()),21.sp.value,Color.White,Paint.Align.CENTER)
            val result=Rect(w*.24f,h*.88f,w*.76f,h*.96f);drawRoundRect(Color(0xFF282B2E),result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel(if(remainder==0)"$sum ÷ 9 = $quotient" else "$sum ÷ 9 r $remainder",Offset(result.center.x,result.center.y+12.dp.toPx()),28.sp.value,Color.White,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByElevenScreen(playback:ProofPlayback,onBack:()->Unit) {
    var digits by remember{mutableStateOf(listOf(5,7,2,8))};var startPositive by remember{mutableStateOf(true)};val raw=digits[0]-digits[1]+digits[2]-digits[3];val alternating=if(startPositive)raw else -raw;val divisible=((alternating%11)+11)%11==0;val coral=Color(0xFFF05B46);val cyan=Color(0xFF13A8C2);val violet=Color(0xFF5444BE)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(96.dp).padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=43.sp,color=Color(0xFF20252A),modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Divisibility Rule for 11",fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color(0xFF20252A));Text("43 / 69",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color(0xFF20252A))};Text("⋮",fontSize=33.sp,color=Color(0xFF20252A))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Divisibility by 11 proof for ${digits.joinToString("")}. The alternating sum is $alternating, so the number is ${if(divisible)"divisible" else "not divisible"} by 11. Tap a digit to edit it or tap the sign rail to reverse all signs."}.pointerInput(digits,startPositive){detectTapGestures{p->if(p.y<size.height*.22f){val i=(p.x/(size.width/4f)).toInt().coerceIn(0,3);digits=digits.toMutableList().also{it[i]=(it[i]+1)%10}}else if(p.y>size.height*.72f)startPositive=!startPositive}}) {
            drawSoftGrid();val w=size.width;val h=size.height;val ink=Color(0xFF292D32);val colors=listOf(coral,cyan,coral,cyan);val cardW=w*.11f;val gap=w*.04f;val total=4*cardW+3*gap;val start=(w-total)/2;val top=h*.055f
            drawRoundRect(Color(0xFF242830),Offset(w*.38f,0f),Size(w*.24f,35.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()));drawLabel("◉",Offset(w*.50f,25.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER)
            for(i in 0..3){val x=start+i*(cardW+gap);val card=Rect(x,top,x+cardW,top+h*.075f);drawRoundRect(colors[i],card.topLeft,card.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("${digits[i]}",Offset(card.center.x,card.center.y+16.dp.toPx()),34.sp.value,Color.White,Paint.Align.CENTER);drawCircle(colors[i],7.dp.toPx(),Offset(card.center.x,card.bottom+14.dp.toPx()))}
            val leftTray=Rect(w*.08f,h*.27f,w*.45f,h*.38f);val rightTray=Rect(w*.55f,h*.27f,w*.92f,h*.38f);listOf(leftTray to coral,rightTray to cyan).forEach{(r,c)->drawRoundRect(c.copy(alpha=.15f),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(c,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(2.dp.toPx()))};drawLabel("${digits[0]}  +  ${digits[2]}",Offset(leftTray.center.x,leftTray.center.y+11.dp.toPx()),27.sp.value,coral,Paint.Align.CENTER);drawLabel("${digits[1]}  +  ${digits[3]}",Offset(rightTray.center.x,rightTray.center.y+11.dp.toPx()),27.sp.value,cyan,Paint.Align.CENTER);drawLabel("${digits[0]+digits[2]}",Offset(leftTray.center.x,leftTray.bottom+37.dp.toPx()),25.sp.value,coral,Paint.Align.CENTER);drawLabel("${digits[1]+digits[3]}",Offset(rightTray.center.x,rightTray.bottom+37.dp.toPx()),25.sp.value,cyan,Paint.Align.CENTER);for(i in 0..3){val from=Offset(start+i*(cardW+gap)+cardW/2,top+h*.075f+20.dp.toPx());val target=if(i%2==0)leftTray.center else rightTray.center;val path=Path().apply{moveTo(from.x,from.y);quadraticTo(w*.5f,from.y+35.dp.toPx(),target.x,target.y-45.dp.toPx())};drawPath(path,colors[i],style=Stroke(2.dp.toPx()))}
            val eq=Rect(w*.23f,h*.46f,w*.77f,h*.52f);drawRoundRect(Color(0xFF292D34),eq.topLeft,eq.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));val expression="${if(startPositive)digits[0] else -digits[0]} − ${digits[1]} + ${digits[2]} − ${digits[3]} = $alternating";drawLabel(expression,Offset(eq.center.x,eq.center.y+10.dp.toPx()),21.sp.value,Color.White,Paint.Align.CENTER)
            val lineY=h*.61f;drawLine(coral,Offset(w*.09f,lineY),Offset(w*.50f,lineY),4.dp.toPx());drawLine(violet,Offset(w*.50f,lineY),Offset(w*.91f,lineY),4.dp.toPx());for(i in 0..11){val x=w*(.09f+i/11f*.82f);drawLine(ink,Offset(x,lineY-6.dp.toPx()),Offset(x,lineY+6.dp.toPx()),1.dp.toPx())};val marker=w*(.09f+abs(alternating).coerceAtMost(11)/11f*.82f);drawCircle(violet,12.dp.toPx(),Offset(marker,lineY));drawLabel("${abs(alternating)}",Offset(marker,lineY-20.dp.toPx()),20.sp.value,violet,Paint.Align.CENTER);drawLabel("0",Offset(w*.09f,lineY+34.dp.toPx()),18.sp.value,ink,Paint.Align.CENTER);drawLabel("11",Offset(w*.91f,lineY+34.dp.toPx()),18.sp.value,ink,Paint.Align.CENTER)
            val rail=Rect(w*.08f,h*.72f,w*.92f,h*.81f);drawRoundRect(Color.White,rail.topLeft,rail.size,androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()));drawRoundRect(Color(0xFFD1CEC8),rail.topLeft,rail.size,androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),style=Stroke(1.dp.toPx()));for(i in 0..3){val cx=w*(.23f+i*.18f);drawCircle(colors[i],22.dp.toPx(),Offset(cx,rail.center.y));drawLabel(if((i%2==0)==startPositive)"+" else "−",Offset(cx,rail.center.y+9.dp.toPx()),27.sp.value,Color.White,Paint.Align.CENTER)}
            val verdict=Rect(w*.20f,h*.87f,w*.80f,h*.95f);drawRoundRect(Color(0xFF292D34),verdict.topLeft,verdict.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("11 ${if(divisible)"∣" else "∤"} ${digits.joinToString("")}",Offset(verdict.center.x,verdict.center.y+14.dp.toPx()),33.sp.value,Color.White,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun DivisibilityByFiveAndTenScreen(playback: ProofPlayback, onBack: () -> Unit) {
    var digits by remember { mutableStateOf(listOf(8, 4, 7, 2, 6, 9, 3, 5)) }
    var selected by remember { mutableStateOf(0) }
    val last = digits.last()
    val byFive = last == 0 || last == 5
    val byTen = last == 0
    val coral = Color(0xFFF25A3F)
    val cyan = Color(0xFF12A6BE)
    val blue = Color(0xFF294FAE)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 42.sp, color = Color(0xFF20252A), modifier = Modifier.clickable(onClick = onBack).semantics { contentDescription = "Back" })
            Text("Divisibility Rules for 5 and 10", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20252A), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            Text("44 / 69", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF20252A))
            Text("⋮", fontSize = 31.sp, color = Color(0xFF20252A), modifier = Modifier.padding(start = 6.dp))
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics {
            contentDescription = "Divisibility sorter for ${digits.joinToString("")}. Last digit $last means divisible by 5: $byFive, divisible by 10: $byTen. Tap a belt digit or turn the digit dial."
        }.pointerInput(digits) { detectTapGestures { p ->
            if (p.y > size.height * .68f) {
                selected = ((atan2((p.y - size.height * .79f).toDouble(), (p.x - size.width * .5f).toDouble()) * 180 / PI + 90 + 360) % 360 / 36).roundToInt() % 10
                digits = digits.dropLast(1) + selected
            } else if (p.y in size.height * .18f..size.height * .36f) {
                val i = ((p.x - size.width * .02f) / (size.width * .075f)).toInt().coerceIn(0, 7)
                digits = digits.toMutableList().also { it[i] = (it[i] + 1) % 10 }
                selected = digits.last()
            }
        }}) {
            drawSoftGrid(); val w = size.width; val h = size.height; val ink = Color(0xFF293039)
            val belt = Rect(0f, h * .22f, w * .48f, h * .34f)
            drawRoundRect(Color(0xFF20262D), belt.topLeft, belt.size, androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            repeat(9) { i -> val x = belt.left + i * belt.width / 8f; drawLine(Color(0xFF3C4650), Offset(x, belt.top), Offset(x, belt.bottom), 1.dp.toPx()) }
            val tileW = w * .058f; val tileGap = w * .013f
            digits.forEachIndexed { i, d ->
                val x = w * .018f + i * (tileW + tileGap); val r = Rect(x, h * .242f, x + tileW, h * .31f)
                drawRoundRect(if (i == 7) coral else Color(0xFF111820), r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                drawRoundRect(Color(0xFF66717C), r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()), style = Stroke(1.dp.toPx()))
                drawLabel("$d", Offset(r.center.x, r.center.y + 11.dp.toPx()), 23.sp.value, Color.White, Paint.Align.CENTER)
            }
            val gate = Rect(w * .42f, h * .17f, w * .54f, h * .39f); drawRoundRect(Color(0xFF343B42), gate.topLeft, gate.size, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())); drawRoundRect(Color(0xFF111820), gate.topLeft, gate.size, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()), style = Stroke(2.dp.toPx()))
            drawCircle(Color(0xFFFFB21C), 9.dp.toPx(), Offset(gate.center.x, gate.top + 13.dp.toPx())); drawLabel("$last", Offset(gate.center.x, h * .285f), 28.sp.value, Color.White, Paint.Align.CENTER)
            val tray5 = Rect(w * .65f, h * .16f, w * .88f, h * .28f); val tray10 = Rect(w * .65f, h * .31f, w * .88f, h * .43f)
            listOf(Triple(tray5, cyan, "÷5"), Triple(tray10, blue, "÷10")).forEach { (r, c, label) ->
                drawRoundRect(c.copy(alpha = .18f), r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(11.dp.toPx())); drawRoundRect(c, r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(11.dp.toPx()), style = Stroke(2.dp.toPx())); drawCircle(c.copy(alpha = .18f), 31.dp.toPx(), Offset(r.left, r.top)); drawCircle(c, 31.dp.toPx(), Offset(r.left, r.top), style = Stroke(2.dp.toPx())); drawLabel(label, Offset(r.left, r.top + 10.dp.toPx()), 22.sp.value, c, Paint.Align.CENTER)
            }
            if (byFive) { drawLabel("0", Offset(tray5.center.x - 18.dp.toPx(), tray5.center.y + 13.dp.toPx()), 29.sp.value, cyan, Paint.Align.CENTER); drawLabel("5", Offset(tray5.center.x + 24.dp.toPx(), tray5.center.y + 13.dp.toPx()), 29.sp.value, cyan, Paint.Align.CENTER) }
            if (byTen) drawLabel("0", Offset(tray10.center.x, tray10.center.y + 13.dp.toPx()), 29.sp.value, blue, Paint.Align.CENTER)
            val chute = Path().apply { moveTo(gate.left, gate.bottom - 8.dp.toPx()); lineTo(w * .25f, h * .40f); lineTo(w * .07f, h * .40f); lineTo(w * .07f, h * .56f); lineTo(w * .93f, h * .56f) }
            drawPath(chute, Color(0xFFC8C1B8), style = Stroke(22.dp.toPx(), cap = StrokeCap.Round)); drawPath(chute, Color(0xFFF1ECE4), style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
            listOf(1,2,3,4,6,7,8,9).forEachIndexed { i, d -> val x = w * (.11f + i * .105f); val y = h * (.49f + if(i%3==0)-.025f else if(i%3==1).01f else -.005f); val r=Rect(x-14.dp.toPx(),y-17.dp.toPx(),x+14.dp.toPx(),y+17.dp.toPx());drawRoundRect(coral,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()));drawLabel("$d",Offset(x,y+8.dp.toPx()),18.sp.value,Color.White,Paint.Align.CENTER) }
            val center = Offset(w * .5f, h * .79f); val radius = min(w * .235f, h * .14f)
            drawCircle(Color(0xFF252C33), radius, center); drawCircle(Color(0xFF111820), radius, center, style = Stroke(3.dp.toPx()))
            for (d in 0..9) { val angle = Math.toRadians((d * 36 - 90).toDouble()); val p = Offset(center.x + cos(angle).toFloat() * radius * .78f, center.y + sin(angle).toFloat() * radius * .78f); drawLine(Color(0xFF111820), Offset(center.x + cos(angle).toFloat()*radius*.48f,center.y+sin(angle).toFloat()*radius*.48f),Offset(center.x+cos(angle).toFloat()*radius,center.y+sin(angle).toFloat()*radius),1.dp.toPx());drawLabel("$d",p+Offset(0f,8.dp.toPx()),20.sp.value,if(d==selected)Color(0xFFFF743E) else Color.White,Paint.Align.CENTER) }
            drawCircle(Color(0xFF303841), radius * .43f, center); drawCircle(Color(0xFF111820), radius * .43f, center, style=Stroke(3.dp.toPx())); drawLabel("▼", Offset(center.x,center.y-radius-3.dp.toPx()),23.sp.value,Color(0xFFFF743E),Paint.Align.CENTER)
            drawLabel(if(byTen)"Ends in 0: divisible by 5 and 10" else if(byFive)"Ends in 5: divisible by 5" else "Ends in $last: neither rule applies",Offset(center.x,h*.965f),17.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun LastDigitParityScreen(playback: ProofPlayback, onBack: () -> Unit) {
    var lastDigit by remember { mutableStateOf(7) }
    val choices = listOf(0, 2, 4, 6, 7, 8)
    val even = lastDigit % 2 == 0
    val orange = Color(0xFFF57B18); val blue = Color(0xFF1768BC); val ink = Color(0xFF292E33)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 42.sp, color = ink, modifier = Modifier.clickable(onClick = onBack).semantics { contentDescription = "Back" })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text("Last Digit Determines Parity", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ink, maxLines = 1); Text("45 / 69", fontSize = 16.sp, color = Color.Gray) }
            Text("⋮", fontSize = 31.sp, color = ink)
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics { contentDescription = "Parity machine showing a number ending in $lastDigit. Since $lastDigit is ${if(even)"even" else "odd"}, the entire number is ${if(even)"even, of the form 2k" else "odd, of the form 2k plus 1"}. Tap a last digit choice." }.pointerInput(lastDigit) { detectTapGestures { p -> if (p.y in size.height*.56f..size.height*.70f) { val i=(p.x/(size.width/6f)).toInt().coerceIn(0,5);lastDigit=choices[i] } } }) {
            drawSoftGrid(); val w=size.width; val h=size.height
            val machine=Rect(w*.10f,h*.10f,w*.90f,h*.49f);drawRoundRect(Color.White.copy(alpha=.72f),machine.topLeft,machine.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFCDC9C2),machine.topLeft,machine.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.5.dp.toPx()))
            val widths=listOf(.20f,.20f,.20f,.20f);val labels=listOf("2k\nTHOUSANDS","2k\nHUNDREDS","2k\nTENS","UNITS")
            for(i in 0..3){val left=machine.left+i*machine.width*.25f;val r=Rect(left,machine.top,left+machine.width*.25f,machine.bottom);val dark=i==3;drawRoundRect(if(dark)Color(0xFF22272C).copy(alpha=.93f) else Color.White.copy(alpha=.20f),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFFC8C4BD),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()));labels[i].split("\n").forEachIndexed{j,s->drawLabel(s,Offset(r.center.x,r.top+(30+j*21).dp.toPx()),if(j==0)18.sp.value else 11.sp.value,if(dark)Color.White else Color.Gray,Paint.Align.CENTER)}
                if(i<3){for(row in 0..1){val cy=r.top+h*.16f+row*h*.15f;for(k in 0 until (if(i==0)1 else if(i==1)12 else 3)){val cols=if(i==1)4 else 3;val x=r.center.x+(k%cols-(cols-1)/2f)*12.dp.toPx();val y=cy+(k/cols)*12.dp.toPx();drawRect(Color(0xFFD9D7D2),Offset(x-5.dp.toPx(),y-5.dp.toPx()),Size(10.dp.toPx(),10.dp.toPx()));drawRect(Color(0xFFBDBAB5),Offset(x-5.dp.toPx(),y-5.dp.toPx()),Size(10.dp.toPx(),10.dp.toPx()),style=Stroke(1.dp.toPx()))}}}else{for(k in 0..6){val cy=r.top+h*(.13f+k*.041f);drawCircle(if(k==3)blue else Color(0xFFE0DEDA),18.dp.toPx(),Offset(r.center.x,cy));if(k==3)drawLabel("$lastDigit",Offset(r.center.x,cy+8.dp.toPx()),20.sp.value,Color.White,Paint.Align.CENTER)}}
            }
            val chooser=Rect(w*.12f,h*.56f,w*.88f,h*.68f);drawRoundRect(Color.White,chooser.topLeft,chooser.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD1CDC6),chooser.topLeft,chooser.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));choices.forEachIndexed{i,d->val cx=w*(.19f+i*.124f);drawCircle(if(d==lastDigit)blue else Color(0xFFE7ECF5),20.dp.toPx(),Offset(cx,chooser.center.y));drawCircle(if(d==lastDigit)Color(0xFF0C3F8B) else Color(0xFFAAB4C4),20.dp.toPx(),Offset(cx,chooser.center.y),style=Stroke(1.5.dp.toPx()));drawLabel("$d",Offset(cx,chooser.center.y+8.dp.toPx()),20.sp.value,if(d==lastDigit)Color.White else Color(0xFF1A62C6),Paint.Align.CENTER)}
            val panel=Rect(w*.025f,h*.72f,w*.975f,h*.95f);drawRoundRect(Color.White,panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD1CDC6),panel.topLeft,panel.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()))
            val rows=listOf(Triple("2k",orange,even),Triple("2k+1",blue,!even));rows.forEachIndexed{i,(label,c,active)->val y=h*(.785f+i*.105f);drawCircle(c,30.dp.toPx(),Offset(w*.12f,y));drawLabel(label,Offset(w*.12f,y+9.dp.toPx()),20.sp.value,Color.White,Paint.Align.CENTER);drawLine(c.copy(alpha=if(active)1f else .35f),Offset(w*.16f,y),Offset(w*.82f,y),3.dp.toPx());repeat(4){j->drawCircle(Color.White,15.dp.toPx(),Offset(w*(.32f+j*.14f),y));drawCircle(c.copy(alpha=if(active)1f else .35f),15.dp.toPx(),Offset(w*(.32f+j*.14f),y),style=Stroke(2.dp.toPx()));drawLabel("⧉",Offset(w*(.32f+j*.14f),y+6.dp.toPx()),13.sp.value,c.copy(alpha=if(active)1f else .35f),Paint.Align.CENTER)};drawCircle(if(active)c else Color.White,22.dp.toPx(),Offset(w*.88f,y));drawCircle(c,22.dp.toPx(),Offset(w*.88f,y),style=Stroke(2.dp.toPx()));if(active)drawLabel("✓",Offset(w*.88f,y+8.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER)}
        }
    }
}

@Composable
private fun FactorPairsRectanglesScreen(playback: ProofPlayback, onBack: () -> Unit) {
    val pairs=listOf(1 to 24,2 to 12,3 to 8,4 to 6);var selected by remember{mutableStateOf(0)};val accent=listOf(Color(0xFFF15B45),Color(0xFFF5A313),Color(0xFF11B8BD),Color(0xFF5544C8));val ink=Color(0xFF282D32)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(84.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=42.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Factor Pairs as Rectangles",fontSize=21.sp,fontWeight=FontWeight.Bold,color=ink);Text("46 / 69",fontSize=16.sp,color=Color.Gray)};Text("⋮",fontSize=31.sp,color=ink)}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{val p=pairs[selected];contentDescription="Factor pairs of 24 shown as rectangles. Selected ${p.first} by ${p.second}. Four factor pairs: 1 by 24, 2 by 12, 3 by 8, and 4 by 6. Tap the left or right arrow to change selection."}.pointerInput(selected){detectTapGestures{p->if(p.y in size.height*.27f..size.height*.48f){selected=if(p.x<size.width/2)(selected+3)%4 else (selected+1)%4}}}){
            drawSoftGrid();val w=size.width;val h=size.height
            val badge=Rect(w*.43f,h*.035f,w*.57f,h*.105f);drawRoundRect(ink,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color.Black,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel("24",Offset(badge.center.x,badge.center.y+15.dp.toPx()),32.sp.value,Color.White,Paint.Align.CENTER)
            fun drawArray(rows:Int,cols:Int,center:Offset,maxW:Float,maxH:Float,color:Color,active:Boolean){val cell=min(maxW/cols,maxH/rows).coerceAtMost(23.dp.toPx());val aw=cols*cell;val ah=rows*cell;val box=Rect(center.x-aw/2-4.dp.toPx(),center.y-ah/2-4.dp.toPx(),center.x+aw/2+4.dp.toPx(),center.y+ah/2+4.dp.toPx());drawRoundRect(if(active)Color.White else Color(0xFF343A40),box.topLeft,box.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(if(active)color else Color(0xFF111820),box.topLeft,box.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(1.5.dp.toPx()));for(r in 0 until rows)for(c in 0 until cols){val tl=Offset(center.x-aw/2+c*cell+2.dp.toPx(),center.y-ah/2+r*cell+2.dp.toPx());drawRoundRect(if(active)color else Color(0xFF4B5156),tl,Size(cell-4.dp.toPx(),cell-4.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()));if(!active)drawRoundRect(Color(0xFF20252A),tl,Size(cell-4.dp.toPx(),cell-4.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),style=Stroke(1.dp.toPx()))}}
            drawLabel("1×24",Offset(w*.5f,h*.16f),20.sp.value,Color.Black,Paint.Align.CENTER);drawArray(1,24,Offset(w*.54f,h*.205f),w*.72f,h*.035f,accent[0],false);drawCircle(Color.White,16.dp.toPx(),Offset(w*.145f,h*.205f));drawCircle(accent[0],5.dp.toPx(),Offset(w*.145f,h*.205f))
            val showcase=Rect(w*.14f,h*.27f,w*.86f,h*.45f);drawRoundRect(Color.White,showcase.topLeft,showcase.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(Color(0xFFD1CDC7),showcase.topLeft,showcase.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx()));val pair=pairs[selected];drawArray(pair.first,pair.second,showcase.center,showcase.width*.82f,showcase.height*.55f,accent[selected],true);drawCircle(ink,23.dp.toPx(),Offset(w*.065f,showcase.center.y));drawCircle(ink,23.dp.toPx(),Offset(w*.935f,showcase.center.y));drawLabel("‹",Offset(w*.065f,showcase.center.y+12.dp.toPx()),38.sp.value,Color.White,Paint.Align.CENTER);drawLabel("›",Offset(w*.935f,showcase.center.y+12.dp.toPx()),38.sp.value,Color.White,Paint.Align.CENTER)
            repeat(4){i->drawCircle(if(i==selected)accent[selected] else Color(0xFFD5D5D5),4.dp.toPx(),Offset(w*(.44f+i*.04f),h*.475f))}
            val ys=listOf(.55f,.69f,.83f);for(k in 1..3){val (r,c)=pairs[k];val y=h*ys[k-1];drawLabel("$r×$c",Offset(w*.5f,y-58.dp.toPx()),19.sp.value,Color.Black,Paint.Align.CENTER);drawArray(r,c,Offset(w*.52f,y),w*.48f,h*.095f,accent[k],false);drawCircle(Color.White,16.dp.toPx(),Offset(w*.145f,y));drawCircle(accent[k],5.dp.toPx(),Offset(w*.145f,y));drawLine(accent[k],Offset(w*.165f,y),Offset(w*.27f,y),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),4.dp.toPx())))}
            drawLabel("Every rectangle uses exactly 24 tiles",Offset(w*.5f,h*.955f),17.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun MultiplesNumberLineScreen(playback:ProofPlayback,onBack:()->Unit){
    var a by remember{mutableStateOf(4)};var b by remember{mutableStateOf(6)};fun gcd(x:Int,y:Int):Int{var m=x;var n=y;while(n!=0){val t=m%n;m=n;n=t};return m};val lcm=a/gcd(a,b)*b;val cyan=Color(0xFF11AFC1);val coral=Color(0xFFFF654E);val violet=Color(0xFF5645D8);val ink=Color(0xFF263038)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(84.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=42.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Multiples on the Number Line",fontSize=21.sp,fontWeight=FontWeight.Bold,color=ink);Text("47 / 69",fontSize=16.sp,fontWeight=FontWeight.Bold,color=ink)};Text("⋮",fontSize=31.sp,color=ink)}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Multiples number line with jumps of $a and $b. Their least common multiple is $lcm. Tap minus or plus around either dial to change a jump size."}.pointerInput(a,b){detectTapGestures{p->if(p.y in size.height*.60f..size.height*.86f){val left=p.x<size.width/2;val plus=if(left)p.x>size.width*.29f else p.x>size.width*.79f;if(left)a=(a+if(plus)1 else -1).coerceIn(2,10) else b=(b+if(plus)1 else -1).coerceIn(2,10)}}}){
            drawSoftGrid();val w=size.width;val h=size.height;val x0=w*.075f;val x1=w*.93f;fun xFor(n:Int)=x0+(x1-x0)*n/30f
            val lineY=h*.31f;drawLine(ink,Offset(x0-20.dp.toPx(),lineY),Offset(x1+20.dp.toPx(),lineY),2.dp.toPx());for(n in 0..30 step 2){val x=xFor(n);drawLine(ink,Offset(x,lineY-7.dp.toPx()),Offset(x,lineY+7.dp.toPx()),1.5.dp.toPx());drawLabel("$n",Offset(x,lineY+30.dp.toPx()),13.sp.value,ink,Paint.Align.CENTER)}
            fun arcJumps(step:Int,y:Float,color:Color,above:Boolean){var n=0;while(n+step<=30){val sx=xFor(n);val ex=xFor(n+step);val path=Path().apply{moveTo(sx,y);quadraticTo((sx+ex)/2,y+(if(above)-42 else 42).dp.toPx(),ex,y)};drawPath(path,color,style=Stroke(2.dp.toPx()));drawCircle(if((n+step)%lcm==0)violet else color,8.dp.toPx(),Offset(ex,y));n+=step}}
            arcJumps(a,lineY-22.dp.toPx(),cyan,true);arcJumps(b,lineY+95.dp.toPx(),coral,false);drawCircle(cyan,13.dp.toPx(),Offset(x0,lineY-22.dp.toPx()));drawCircle(coral,13.dp.toPx(),Offset(x0,lineY+95.dp.toPx()));for(n in lcm..30 step lcm){val x=xFor(n);drawCircle(violet,12.dp.toPx(),Offset(x,lineY-22.dp.toPx()));drawCircle(violet,12.dp.toPx(),Offset(x,lineY+95.dp.toPx()));drawLine(violet,Offset(x,lineY+8.dp.toPx()),Offset(x,lineY+82.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx())))}
            fun dial(center:Offset,value:Int,color:Color){val radius=66.dp.toPx();drawCircle(Color.White,radius,center);drawCircle(color,radius,center,style=Stroke(2.dp.toPx()));for(i in 0..30){val ang=Math.toRadians((200+i*140/30.0));val p1=Offset(center.x+cos(ang).toFloat()*radius*.77f,center.y+sin(ang).toFloat()*radius*.77f);val p2=Offset(center.x+cos(ang).toFloat()*radius*.90f,center.y+sin(ang).toFloat()*radius*.90f);drawLine(color.copy(alpha=.35f),p1,p2,1.dp.toPx())};drawCircle(Color.White,44.dp.toPx(),center);drawLabel("$value",Offset(center.x,center.y+17.dp.toPx()),37.sp.value,color,Paint.Align.CENTER);listOf(-1 to (center.x-radius),1 to(center.x+radius)).forEach{(d,x)->drawCircle(Color.White,25.dp.toPx(),Offset(x,center.y));drawCircle(Color(0xFFD3CEC8),25.dp.toPx(),Offset(x,center.y),style=Stroke(1.dp.toPx()));drawLabel(if(d<0)"−" else "+",Offset(x,center.y+10.dp.toPx()),28.sp.value,color,Paint.Align.CENTER)}}
            dial(Offset(w*.27f,h*.71f),a,cyan);dial(Offset(w*.73f,h*.71f),b,coral);val cardY=h*.90f;drawRoundRect(Color.White,Offset(w*.26f,cardY),Size(w*.14f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(violet,Offset(w*.26f,cardY),Size(w*.14f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel("$lcm",Offset(w*.33f,cardY+38.dp.toPx()),30.sp.value,violet,Paint.Align.CENTER);drawRoundRect(Color.White,Offset(w*.60f,cardY),Size(w*.14f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(violet,Offset(w*.60f,cardY),Size(w*.14f,55.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel("${lcm*2}",Offset(w*.67f,cardY+38.dp.toPx()),30.sp.value,violet,Paint.Align.CENTER);drawLine(violet,Offset(w*.40f,cardY+27.dp.toPx()),Offset(w*.60f,cardY+27.dp.toPx()),2.dp.toPx())
        }
    }
}

@Composable
private fun PrimeFactorBuildingBlocksScreen(playback:ProofPlayback,onBack:()->Unit){
    var alternate by remember{mutableStateOf(false)};val coral=Color(0xFFF25B43);val amber=Color(0xFFF4A50C);val cyan=Color(0xFF0FA7B8);val stone=Color(0xFF30363A);val ink=Color(0xFF252B30)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Prime Factorization as Building Blocks",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("48 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=4.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Prime factorization of 60. ${if(alternate)"The factor tree splits 60 into 4 and 15" else "The factor tree splits 60 into 6 and 10"}, but both end with the unique prime blocks 2, 2, 3, and 5. Tap the factor trees to switch decomposition."}.pointerInput(alternate){detectTapGestures{p->if(p.y>size.height*.55f)alternate=!alternate}}){
            drawSoftGrid();val w=size.width;val h=size.height
            fun block(center:Offset,label:String,color:Color,width:Float=72.dp.toPx(),height:Float=57.dp.toPx()){val r=Rect(center.x-width/2,center.y-height/2,center.x+width/2,center.y+height/2);drawRoundRect(color,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(Color(0xFF171B1E),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(2.dp.toPx()));drawLine(Color.White.copy(alpha=.25f),Offset(r.left+4.dp.toPx(),r.top+4.dp.toPx()),Offset(r.right-4.dp.toPx(),r.top+4.dp.toPx()),1.dp.toPx());drawLabel(label,Offset(center.x,center.y+12.dp.toPx()),26.sp.value,Color.White,Paint.Align.CENTER)}
            block(Offset(w*.5f,h*.085f),"60",stone,w*.55f,82.dp.toPx());drawLabel("⌄",Offset(w*.5f,h*.15f),30.sp.value,ink,Paint.Align.CENTER);block(Offset(w*.35f,h*.21f),if(alternate)"4" else "6",stone,w*.29f);block(Offset(w*.65f,h*.21f),if(alternate)"15" else "10",stone,w*.29f);drawLabel("×",Offset(w*.5f,h*.22f),27.sp.value,Color.White,Paint.Align.CENTER);drawLabel("⌄",Offset(w*.5f,h*.275f),30.sp.value,ink,Paint.Align.CENTER)
            val primes=listOf("2" to coral,"2" to coral,"3" to amber,"5" to cyan);primes.forEachIndexed{i,(s,c)->block(Offset(w*(.27f+i*.155f),h*.34f),s,c,w*.145f,72.dp.toPx())};val formula=Rect(w*.31f,h*.40f,w*.69f,h*.46f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("2² × 3 × 5",Offset(formula.center.x,formula.center.y+13.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);drawLine(Color(0xFFCBC7C1),Offset(0f,h*.50f),Offset(w,h*.50f),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(),7.dp.toPx())))
            primes.forEachIndexed{i,(s,c)->block(Offset(w*(.30f+i*.135f),h*.56f),s,c,w*.09f,49.dp.toPx())}
            fun tree(cx:Float,leftFirst:Boolean){block(Offset(cx,h*.66f),"60",stone,w*.11f,44.dp.toPx());val pair=if(leftFirst)(6 to 10) else (4 to 15);val lx=cx-w*.10f;val rx=cx+w*.10f;drawLine(stone,Offset(cx,h*.675f),Offset(lx,h*.715f),2.dp.toPx());drawLine(stone,Offset(cx,h*.675f),Offset(rx,h*.715f),2.dp.toPx());block(Offset(lx,h*.73f),"${pair.first}",stone,w*.10f,40.dp.toPx());block(Offset(rx,h*.73f),"${pair.second}",stone,w*.10f,40.dp.toPx());val leaves=if(leftFirst)listOf(2,3,2,5)else listOf(2,2,3,5);leaves.forEachIndexed{i,n->val x=cx+w*(-.16f+i*.105f);drawLine(stone,Offset(if(i<2)lx else rx,h*.75f),Offset(x,h*.79f),1.5.dp.toPx());block(Offset(x,h*.81f),"$n",if(n==2)coral else if(n==3)amber else cyan,w*.085f,42.dp.toPx())}}
            tree(w*.25f,!alternate);tree(w*.75f,alternate);drawCircle(Color.White,25.dp.toPx(),Offset(w*.5f,h*.72f));drawCircle(Color(0xFFD0CCC5),25.dp.toPx(),Offset(w*.5f,h*.72f),style=Stroke(1.dp.toPx()));drawLabel("⇄",Offset(w*.5f,h*.72f+9.dp.toPx()),24.sp.value,ink,Paint.Align.CENTER)
            val boxY=h*.91f;listOf(.26f,.74f).forEach{cx->val r=Rect(w*(cx-.17f),boxY,w*(cx+.17f),boxY+62.dp.toPx());drawRoundRect(Color.White,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));primes.forEachIndexed{i,(s,c)->block(Offset(r.left+27.dp.toPx()+i*34.dp.toPx(),r.center.y),s,c,27.dp.toPx(),34.dp.toPx())}};drawLabel("=",Offset(w*.5f,boxY+40.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun LcmRepeatingCyclesScreen(playback:ProofPlayback,onBack:()->Unit){
    var step by remember{mutableStateOf(0)};val coral=Color(0xFFFF604B);val cyan=Color(0xFF10AECA);val gold=Color(0xFFFFC44A);val dark=Color(0xFF252B31);val violet=Color(0xFF26284C)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=42.sp,color=dark,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("LCM Through Repeating Cycles",fontSize=20.sp,fontWeight=FontWeight.Bold,color=dark);Text("49 / 69",fontSize=16.sp,color=Color.Gray)};Text("⋮",fontSize=31.sp,color=dark)}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Repeating cycles of 4 and 6. Animation step $step of 3. Both cycles first meet at 12, so LCM of 4 and 6 is 12. Tap play or the timeline to advance."}.pointerInput(step){detectTapGestures{step=(step+1)%4}}){
            drawSoftGrid();val w=size.width;val h=size.height
            fun gear(center:Offset,r:Float,value:Int,color:Color){for(i in 0 until value*2){val a=Math.toRadians(i*360.0/(value*2));val p=Offset(center.x+cos(a).toFloat()*r*1.05f,center.y+sin(a).toFloat()*r*1.05f);drawRoundRect(dark,Offset(p.x-10.dp.toPx(),p.y-15.dp.toPx()),Size(20.dp.toPx(),30.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))};drawCircle(dark,r,center);drawCircle(Color.White,r*.72f,center,style=Stroke(2.dp.toPx()));for(i in 0 until value){val a=Math.toRadians((i*360.0/value)-90);val p=Offset(center.x+cos(a).toFloat()*r*.72f,center.y+sin(a).toFloat()*r*.72f);drawCircle(if(i==step%value)color else Color(0xFF5B6268),9.dp.toPx(),p);drawCircle(Color.White,9.dp.toPx(),p,style=Stroke(1.5.dp.toPx()))};drawLabel("$value",Offset(center.x,center.y+18.dp.toPx()),43.sp.value,color,Paint.Align.CENTER)}
            gear(Offset(w*.27f,h*.17f),70.dp.toPx(),4,coral);gear(Offset(w*.70f,h*.17f),88.dp.toPx(),6,cyan);drawLabel("↷",Offset(w*.10f,h*.10f),30.sp.value,dark,Paint.Align.CENTER);drawLabel("↷",Offset(w*.48f,h*.08f),30.sp.value,dark,Paint.Align.CENTER)
            repeat(7){i->drawCircle(if(i==step)dark else Color.Gray,3.dp.toPx(),Offset(w*(.38f+i*.04f),h*.35f))};drawCircle(dark,12.dp.toPx(),Offset(w*.5f,h*.35f));drawLabel("▶",Offset(w*.5f,h*.35f+5.dp.toPx()),12.sp.value,Color.White,Paint.Align.CENTER)
            fun timeline(y:Float,jump:Int,color:Color){drawRoundRect(color,Offset(w*.05f,y-22.dp.toPx()),Size(42.dp.toPx(),44.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("$jump",Offset(w*.05f+21.dp.toPx(),y+12.dp.toPx()),27.sp.value,Color.White,Paint.Align.CENTER);drawLine(dark,Offset(w*.16f,y),Offset(w*.92f,y),2.dp.toPx());var n=0;while(n<=12){val x=w*(.16f+n/12f*.76f);drawCircle(if(n==12)Color.White else color,if(n==12)12.dp.toPx() else 6.dp.toPx(),Offset(x,y));if(n==12)drawCircle(color,12.dp.toPx(),Offset(x,y),style=Stroke(3.dp.toPx()));drawLabel("$n",Offset(x,y+28.dp.toPx()),15.sp.value,dark,Paint.Align.CENTER);if(n+jump<=12){val ex=w*(.16f+(n+jump)/12f*.76f);val p=Path().apply{moveTo(x,y-5.dp.toPx());quadraticTo((x+ex)/2,y-35.dp.toPx(),ex,y-5.dp.toPx())};drawPath(p,color,style=Stroke(1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))))};n+=jump}}
            timeline(h*.45f,4,coral);timeline(h*.57f,6,cyan);val result=Rect(w*.12f,h*.66f,w*.88f,h*.74f);drawRoundRect(violet,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("LCM(4, 6) =",Offset(w*.50f,result.center.y+15.dp.toPx()),26.sp.value,Color.White,Paint.Align.RIGHT);drawLabel("12",Offset(w*.56f,result.center.y+15.dp.toPx()),37.sp.value,gold,Paint.Align.LEFT)
            drawLabel("⏮",Offset(w*.29f,h*.82f),34.sp.value,violet,Paint.Align.CENTER);drawCircle(violet,36.dp.toPx(),Offset(w*.5f,h*.82f));drawLabel("▶",Offset(w*.5f,h*.82f+12.dp.toPx()),29.sp.value,Color.White,Paint.Align.CENTER);drawLabel("⏭",Offset(w*.71f,h*.82f),34.sp.value,violet,Paint.Align.CENTER);val ctrl=Rect(w*.06f,h*.90f,w*.94f,h*.975f);drawRoundRect(Color.White,ctrl.topLeft,ctrl.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("⏱   1.0×",Offset(w*.13f,ctrl.center.y+9.dp.toPx()),20.sp.value,violet);drawLine(Color(0xFFC8C8C8),Offset(w*.35f,ctrl.center.y),Offset(w*.78f,ctrl.center.y),5.dp.toPx());drawLine(violet,Offset(w*.35f,ctrl.center.y),Offset(w*(.35f+step*.14f),ctrl.center.y),5.dp.toPx());drawCircle(Color.White,14.dp.toPx(),Offset(w*(.35f+step*.14f),ctrl.center.y));drawLabel("+",Offset(w*.86f,ctrl.center.y+10.dp.toPx()),27.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun EuclideanAlgorithmScreen(playback:ProofPlayback,onBack:()->Unit){
    var stage by remember{mutableStateOf(3)};val coral=Color(0xFFF45C43);val cyan=Color(0xFF13A5C1);val amber=Color(0xFFF2A313);val violet=Color(0xFF4E4DB5);val ink=Color(0xFF242A31)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=42.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Euclidean Algorithm",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("50 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=31.sp,color=ink,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Euclidean algorithm stage $stage. 252 equals 2 times 105 plus 42; 105 equals 2 times 42 plus 21; 42 equals 2 times 21. Therefore GCD is 21. Tap to reveal the next division step."}.pointerInput(stage){detectTapGestures{stage=if(stage>=3)1 else stage+1}}){
            drawSoftGrid();val w=size.width;val h=size.height
            fun segmentedBar(y:Float,total:Int,parts:List<Pair<Int,Color>>,width:Float=w*.88f){val left=(w-width)/2;var x=left;parts.forEach{(n,c)->val pw=width*n/total;val r=Rect(x,y,x+pw,y+58.dp.toPx());drawRect(c,r.topLeft,r.size);drawRect(ink,r.topLeft,r.size,style=Stroke(1.5.dp.toPx()));drawLabel("$n",Offset(r.center.x,r.center.y+10.dp.toPx()),22.sp.value,Color.White,Paint.Align.CENTER);x+=pw};drawLine(ink,Offset(left,y-17.dp.toPx()),Offset(left+width,y-17.dp.toPx()),1.dp.toPx());drawLabel("$total",Offset(w*.5f,y-9.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER)}
            drawLabel("252 =",Offset(w*.43f,h*.06f),27.sp.value,ink,Paint.Align.RIGHT);drawLabel("2·105",Offset(w*.44f,h*.06f),27.sp.value,coral);drawLabel("+ 42",Offset(w*.64f,h*.06f),27.sp.value,cyan);segmentedBar(h*.12f,252,listOf(105 to coral,105 to coral,42 to cyan));drawLabel("105       105       42",Offset(w*.50f,h*.205f),18.sp.value,ink,Paint.Align.CENTER)
            if(stage>=1){drawLabel("105 =",Offset(w*.43f,h*.34f),27.sp.value,ink,Paint.Align.RIGHT);drawLabel("2·42",Offset(w*.44f,h*.34f),27.sp.value,amber);drawLabel("+ 21",Offset(w*.62f,h*.34f),27.sp.value,violet);drawLabel("↳",Offset(w*.78f,h*.27f),34.sp.value,cyan,Paint.Align.CENTER);segmentedBar(h*.40f,105,listOf(42 to amber,42 to amber,21 to violet),w*.52f)}
            if(stage>=2){drawLabel("42 = 2·21",Offset(w*.50f,h*.62f),27.sp.value,ink,Paint.Align.CENTER);drawLabel("↳",Offset(w*.80f,h*.54f),34.sp.value,violet,Paint.Align.CENTER);segmentedBar(h*.68f,42,listOf(21 to violet,21 to violet),w*.38f)}
            if(stage>=3){val result=Rect(w*.23f,h*.84f,w*.77f,h*.94f);drawRoundRect(Color(0xFFF3F2F8),result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawRoundRect(ink,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel("GCD =",Offset(w*.53f,result.center.y+17.dp.toPx()),36.sp.value,ink,Paint.Align.RIGHT);drawLabel("21",Offset(w*.57f,result.center.y+17.dp.toPx()),39.sp.value,violet,Paint.Align.LEFT)}else drawLabel("Tap to continue",Offset(w*.5f,h*.91f),18.sp.value,Color.Gray,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun GcdLargestGroupingScreen(playback:ProofPlayback,onBack:()->Unit){
    val options=listOf(3,6,12);var index by remember{mutableStateOf(2)};val group=options[index];val cyan=Color(0xFF12A9B4);val coral=Color(0xFFF45F48);val violet=Color(0xFF554FBB);val ink=Color(0xFF252B30)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("GCD Through Largest Equal Grouping",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("51 / 69",fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=4.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Grouping 24 and 36 into equal groups of $group. This makes ${24/group} groups for 24 and ${36/group} groups for 36. The largest shared group size is 12, so GCD of 24 and 36 is 12. Tap minus or plus to test another shared divisor."}.pointerInput(index){detectTapGestures{p->if(p.y>size.height*.55f)index=if(p.x<size.width/2)(index+options.size-1)%options.size else (index+1)%options.size}}){
            drawSoftGrid();val w=size.width;val h=size.height
            fun groupDots(total:Int,y:Float,color:Color){val count=total/group;val margin=w*.20f;val gap=(w*.76f-margin)/count;for(g in 0 until count){val left=margin+g*gap;val box=Rect(left,y,left+gap-7.dp.toPx(),y+h*.105f);drawRoundRect(color,box.topLeft,box.size,androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),style=Stroke(1.5.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),4.dp.toPx()))));val cols=4;for(i in 0 until group){val cx=box.left+box.width*(.16f+(i%cols)*.23f);val cy=box.top+box.height*(.23f+(i/cols)*.25f);drawCircle(color,7.dp.toPx(),Offset(cx,cy));drawCircle(color.copy(alpha=.55f),7.dp.toPx(),Offset(cx,cy),style=Stroke(1.dp.toPx()))}};val badge=Rect(w*.04f,y+h*.02f,w*.16f,y+h*.075f);drawRoundRect(Color.White,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawRoundRect(color,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel("$total",Offset(badge.center.x,badge.center.y+13.dp.toPx()),27.sp.value,color,Paint.Align.CENTER)}
            groupDots(24,h*.07f,cyan);drawLine(Color(0xFFCDC9C3),Offset(w*.04f,h*.24f),Offset(w*.96f,h*.24f),1.dp.toPx());groupDots(36,h*.28f,coral);drawLine(Color(0xFFCDC9C3),Offset(w*.04f,h*.48f),Offset(w*.96f,h*.48f),1.dp.toPx())
            val center=Offset(w*.49f,h*.70f);val radius=112.dp.toPx();drawCircle(Color.White,radius,center);drawCircle(Color(0xFFD4D0CA),radius,center,style=Stroke(2.dp.toPx()));for(i in 0..8){val ang=Math.toRadians((220+i*100/8.0));drawLine(Color.Gray,Offset(center.x+cos(ang).toFloat()*radius*.78f,center.y+sin(ang).toFloat()*radius*.78f),Offset(center.x+cos(ang).toFloat()*radius*.90f,center.y+sin(ang).toFloat()*radius*.90f),1.dp.toPx())};drawCircle(Color.White,radius*.48f,center);drawCircle(Color(0xFFAAA7A2),radius*.48f,center,style=Stroke(1.dp.toPx()));drawLabel("$group",Offset(center.x,center.y+23.dp.toPx()),44.sp.value,violet,Paint.Align.CENTER);drawLabel("6",Offset(center.x-radius*.72f,center.y+45.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);drawLabel("12",Offset(center.x,center.y+radius+33.dp.toPx()),24.sp.value,violet,Paint.Align.CENTER);drawLabel("18",Offset(center.x+radius*.72f,center.y-40.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);drawCircle(Color.White,26.dp.toPx(),Offset(w*.12f,center.y));drawCircle(violet,26.dp.toPx(),Offset(w*.12f,center.y),style=Stroke(1.5.dp.toPx()));drawLabel("−",Offset(w*.12f,center.y+10.dp.toPx()),28.sp.value,violet,Paint.Align.CENTER);drawCircle(Color.White,26.dp.toPx(),Offset(w*.86f,center.y));drawCircle(violet,26.dp.toPx(),Offset(w*.86f,center.y),style=Stroke(1.5.dp.toPx()));drawLabel("+",Offset(w*.86f,center.y+10.dp.toPx()),28.sp.value,violet,Paint.Align.CENTER)
            val result=Rect(w*.14f,h*.88f,w*.86f,h*.96f);drawRoundRect(Color(0xFFF4F3FA),result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawRoundRect(violet,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel("GCD(24, 36) =",Offset(w*.67f,result.center.y+15.dp.toPx()),29.sp.value,ink,Paint.Align.RIGHT);drawLabel("12",Offset(w*.70f,result.center.y+15.dp.toPx()),31.sp.value,violet,Paint.Align.LEFT)
        }
    }
}

@Composable
private fun GcdLcmRelationshipScreen(playback:ProofPlayback,onBack:()->Unit){
    var distributed by remember{mutableStateOf(false)};val coral=Color(0xFFFF6151);val cyan=Color(0xFF0FB1C3);val violet=Color(0xFF574FBB);val ink=Color(0xFF252A2F)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Relationship Between GCD and LCM",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("52 / 69",fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=4.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Prime factor comparison of 12 and 18. Common factors form GCD 6, all maximum powers form LCM 36, proving 12 times 18 equals 6 times 36. Tap the balance to ${if(distributed)"collect" else "redistribute"} the factor blocks."}.pointerInput(distributed){detectTapGestures{distributed=!distributed}}){
            drawSoftGrid();val w=size.width;val h=size.height
            fun tile(center:Offset,label:String,color:Color,size:Float=48.dp.toPx()){val r=Rect(center.x-size/2,center.y-size/2,center.x+size/2,center.y+size/2);drawRoundRect(color,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(color.copy(alpha=.55f),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(2.dp.toPx()));drawLabel(label,Offset(center.x,center.y+12.dp.toPx()),25.sp.value,Color.White,Paint.Align.CENTER)}
            fun factorCard(cx:Float,title:String,factors:List<Pair<String,Color>>){drawLabel(title,Offset(cx,h*.06f),34.sp.value,Color.Black,Paint.Align.CENTER);val r=Rect(cx-w*.14f,h*.08f,cx+w*.14f,h*.30f);drawRoundRect(Color.White,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFFD0CBC4),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()));factors.forEachIndexed{i,(s,c)->tile(Offset(r.left+38.dp.toPx()+(i%2)*58.dp.toPx(),r.top+39.dp.toPx()+(i/2)*58.dp.toPx()),s,c)}}
            factorCard(w*.26f,"12",listOf("2" to coral,"2" to coral,"3" to cyan));factorCard(w*.74f,"18",listOf("2" to coral,"3" to cyan,"3" to violet));val gcd=Rect(w*.27f,h*.35f,w*.73f,h*.43f);drawRoundRect(Color.White,gcd.topLeft,gcd.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("GCD = 6",Offset(w*.39f,gcd.center.y+10.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);listOf("2" to coral,"3" to cyan).forEachIndexed{i,(s,c)->tile(Offset(w*(.55f+i*.09f),gcd.center.y),s,c,42.dp.toPx())};val lcm=Rect(w*.14f,h*.49f,w*.86f,h*.57f);drawRoundRect(Color.White,lcm.topLeft,lcm.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));drawLabel("LCM = 36",Offset(w*.27f,lcm.center.y+10.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);listOf("2" to coral,"2" to coral,"3" to cyan,"3" to cyan).forEachIndexed{i,(s,c)->tile(Offset(w*(.46f+i*.09f),lcm.center.y),s,c,42.dp.toPx())}
            val beamY=h*.74f;drawRoundRect(ink,Offset(w*.04f,beamY),Size(w*.92f,18.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLine(ink,Offset(w*.5f,beamY),Offset(w*.5f,h*.82f),8.dp.toPx());val tri=Path().apply{moveTo(w*.5f,h*.78f);lineTo(w*.43f,h*.86f);lineTo(w*.57f,h*.86f);close()};drawPath(tri,ink);val leftFactors=listOf("2" to coral,"2" to coral,"3" to cyan,"2" to coral,"2" to coral,"3" to cyan,"3" to cyan);leftFactors.forEachIndexed{i,(s,c)->tile(Offset(w*(.07f+i*.061f),beamY-27.dp.toPx()),s,c,34.dp.toPx())};leftFactors.forEachIndexed{i,(s,c)->tile(Offset(w*(.55f+i*.061f),beamY-27.dp.toPx()),s,c,34.dp.toPx())}
            val result=Rect(w*.30f,h*.90f,w*.70f,h*.96f);drawRoundRect(ink,result.topLeft,result.size,androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()));drawLabel("12 · 18 = 6 · 36",Offset(result.center.x,result.center.y+12.dp.toPx()),24.sp.value,Color.White,Paint.Align.CENTER);if(distributed)drawLabel("✓ factors redistributed",Offset(w*.5f,h*.64f),18.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun SieveOfEratosthenesScreen(playback:ProofPlayback,onBack:()->Unit){
    var stage by remember{mutableStateOf(3)};val primes=listOf(2,3,5,7);val colors=listOf(Color(0xFFF36A50),Color(0xFFFFB832),Color(0xFF64BCC3),Color(0xFF6255D7));val ink=Color(0xFF262B31)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Sieve of Eratosthenes",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("53 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Sieve of Eratosthenes from 1 to 100, processing prime ${primes[stage]}. Multiples of ${primes.take(stage+1).joinToString()} are crossed out, leaving primes. Tap next or the prime cards to change the sieve stage."}.pointerInput(stage){detectTapGestures{p->stage=if(p.x<size.width*.35f)(stage+3)%4 else (stage+1)%4}}){
            drawSoftGrid();val w=size.width;val h=size.height;val left=w*.025f;val top=h*.02f;val gridW=w*.95f;val cellW=gridW/10;val cellH=h*.047f
            fun isPrime(n:Int):Boolean = n>=2 && (2..kotlin.math.sqrt(n.toDouble()).toInt()).none { n%it==0 }
            for(n in 1..100){val idx=n-1;val c=idx%10;val r=idx/10;val x=left+c*cellW;val y=top+r*cellH;drawRect(Color.White.copy(alpha=.55f),Offset(x,y),Size(cellW,cellH));drawRect(Color(0xFFE4E0DA),Offset(x,y),Size(cellW,cellH),style=Stroke(.7.dp.toPx()));val divisor=primes.take(stage+1).firstOrNull{n!=it&&n%it==0};val prime=isPrime(n);if(prime){drawRoundRect(Color(0xFFFFF5D8),Offset(x+5.dp.toPx(),y+4.dp.toPx()),Size(cellW-10.dp.toPx(),cellH-8.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))};if(n==primes[stage]){drawRoundRect(colors[stage].copy(alpha=.22f),Offset(x+3.dp.toPx(),y+2.dp.toPx()),Size(cellW-6.dp.toPx(),cellH-4.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(colors[stage],Offset(x+3.dp.toPx(),y+2.dp.toPx()),Size(cellW-6.dp.toPx(),cellH-4.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(2.dp.toPx()))};drawLabel("$n",Offset(x+cellW/2,y+cellH*.67f),14.sp.value,if(divisor!=null)Color.Gray else ink,Paint.Align.CENTER);if(divisor!=null){val ci=primes.indexOf(divisor);drawLine(colors[ci],Offset(x+cellW*.25f,y+cellH*.25f),Offset(x+cellW*.75f,y+cellH*.75f),2.dp.toPx());drawLine(colors[ci],Offset(x+cellW*.75f,y+cellH*.25f),Offset(x+cellW*.25f,y+cellH*.75f),2.dp.toPx())}}
            drawLabel("1–100",Offset(w*.5f,h*.525f),22.sp.value,ink,Paint.Align.CENTER);primes.forEachIndexed{i,p->val cx=w*(.18f+i*.215f);val r=Rect(cx-29.dp.toPx(),h*.58f,cx+29.dp.toPx(),h*.645f);drawRoundRect(colors[i].copy(alpha=if(i<=stage).85f else .25f),r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("$p",Offset(cx,r.center.y+13.dp.toPx()),29.sp.value,Color.White,Paint.Align.CENTER);if(i<stage){drawCircle(Color.White,10.dp.toPx(),Offset(r.right-4.dp.toPx(),r.top+3.dp.toPx()));drawLabel("✓",Offset(r.right-4.dp.toPx(),r.top+8.dp.toPx()),12.sp.value,ink,Paint.Align.CENTER)};if(i<3)repeat(3){j->drawCircle(Color.Gray,2.dp.toPx(),Offset(cx+44.dp.toPx()+j*8.dp.toPx(),r.center.y))}}
            val ctrl=Rect(w*.09f,h*.72f,w*.91f,h*.80f);drawRoundRect(Color.White,ctrl.topLeft,ctrl.size,androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()));listOf("▶","⏮","›","⏭","⏩").forEachIndexed{i,s->{val cx=w*(.18f+i*.16f);if(i==2)drawCircle(ink,29.dp.toPx(),Offset(cx,ctrl.center.y));drawLabel(s,Offset(cx,ctrl.center.y+10.dp.toPx()),if(i==2)34.sp.value else 25.sp.value,if(i==2)Color.White else ink,Paint.Align.CENTER)}()};drawLine(Color(0xFFD5D2CD),Offset(w*.06f,h*.88f),Offset(w*.94f,h*.88f),4.dp.toPx());drawLine(colors[stage],Offset(w*.06f,h*.88f),Offset(w*(.25f+stage*.18f),h*.88f),4.dp.toPx());drawCircle(Color.White,11.dp.toPx(),Offset(w*(.25f+stage*.18f),h*.88f));repeat(10){i->drawCircle(Color.Gray,1.7.dp.toPx(),Offset(w*(.06f+i*.035f),h*.91f))}
        }
    }
}

@Composable
private fun CompositeFactorSqrtScreen(playback:ProofPlayback,onBack:()->Unit){
    val pairs=listOf(1 to 84,2 to 42,3 to 28,4 to 21,6 to 14,7 to 12);var selected by remember{mutableStateOf(5)};val colors=listOf(Color(0xFFFF604F),Color(0xFFFF773D),Color(0xFFF3A400),Color(0xFF0EAE83),Color(0xFF049CC4),Color(0xFF5744D0));val ink=Color(0xFF202830)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(84.dp).padding(horizontal=13.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){Text("Composite Factor at Most √n",fontSize=20.sp,fontWeight=FontWeight.Bold,color=ink);Text("54 / 69",fontSize=16.sp,color=Color.Gray)};Text("⋮",fontSize=30.sp,color=ink)}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{val p=pairs[selected];contentDescription="Factor curve for n equals 84. Selected factor pair ${p.first} and ${p.second}. Because ${p.first} is at most square root of 84 and ${p.second} is at least square root of 84, every composite number has a factor at most its square root. Tap arrows or a factor point."}.pointerInput(selected){detectTapGestures{p->selected=if(p.x<size.width/2)(selected+5)%6 else (selected+1)%6}}){
            drawSoftGrid();val w=size.width;val h=size.height;val graph=Rect(w*.08f,h*.08f,w*.92f,h*.58f);drawRoundRect(Color.White.copy(alpha=.35f),graph.topLeft,graph.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(Color(0xFFE0C9B0),graph.topLeft,graph.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()));val ox=graph.left+w*.05f;val oy=graph.bottom-h*.04f;val gx=graph.width-w*.10f;val gy=graph.height-h*.08f
            fun px(v:Float)=ox+gx*v/84f
            fun py(v:Float)=oy-gy*v/84f
            for(v in 0..80 step 10){drawLine(Color(0xFFE6CDB6),Offset(px(v.toFloat()),graph.top+15.dp.toPx()),Offset(px(v.toFloat()),oy),.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())));drawLine(Color(0xFFE6CDB6),Offset(ox,py(v.toFloat())),Offset(graph.right-15.dp.toPx(),py(v.toFloat())),.7.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())))};drawLine(ink,Offset(ox,graph.top+10.dp.toPx()),Offset(ox,oy),1.5.dp.toPx());drawLine(ink,Offset(ox,oy),Offset(graph.right-10.dp.toPx(),oy),1.5.dp.toPx());val curve=Path();for(a in 1..84){val p=Offset(px(a.toFloat()),py(84f/a));if(a==1)curve.moveTo(p.x,p.y)else curve.lineTo(p.x,p.y)};drawPath(curve,ink,style=Stroke(2.dp.toPx()));drawLine(ink,Offset(px(4f),oy),Offset(px(84f),py(84f)),1.2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(),5.dp.toPx())));pairs.forEachIndexed{i,p->val point=Offset(px(p.second.toFloat()),py(p.first.toFloat()));drawCircle(colors[i],if(i==selected)8.dp.toPx() else 6.dp.toPx(),point);drawLabel("(${p.first},${p.second})",point+Offset(8.dp.toPx(),-6.dp.toPx()),13.sp.value,colors[i])};val root=kotlin.math.sqrt(84.0).toFloat();drawLine(ink,Offset(px(root),oy),Offset(px(root),py(root)),1.2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(),4.dp.toPx())));drawLabel("√84",Offset(px(root)+8.dp.toPx(),py(root)-5.dp.toPx()),19.sp.value,ink);drawLabel("n = 84",Offset(graph.center.x,graph.top+32.dp.toPx()),25.sp.value,Color.Black,Paint.Align.CENTER)
            val formula=Rect(w*.38f,h*.62f,w*.62f,h*.68f);drawRoundRect(Color.White,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawRoundRect(ink,formula.topLeft,formula.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("a · b = 84",Offset(formula.center.x,formula.center.y+10.dp.toPx()),20.sp.value,ink,Paint.Align.CENTER);drawCircle(Color.White,24.dp.toPx(),Offset(w*.08f,h*.74f));drawLabel("‹",Offset(w*.08f,h*.74f+12.dp.toPx()),38.sp.value,ink,Paint.Align.CENTER);drawCircle(Color.White,24.dp.toPx(),Offset(w*.92f,h*.74f));drawLabel("›",Offset(w*.92f,h*.74f+12.dp.toPx()),38.sp.value,ink,Paint.Align.CENTER);pairs.forEachIndexed{i,p->val x=w*(.20f+i*.12f);drawCircle(colors[i],if(i==selected) 11.dp.toPx() else 8.dp.toPx(),Offset(x,h*.74f));drawLabel("(${p.first},${p.second})",Offset(x,h*.78f),11.sp.value,colors[i],Paint.Align.CENTER)};val pair=pairs[selected];drawLabel("a = ${pair.first}",Offset(w*.09f,h*.84f),19.sp.value,colors[selected]);drawLabel("b = ${pair.second}",Offset(w*.91f,h*.84f),19.sp.value,colors[selected],Paint.Align.RIGHT);drawLine(colors[selected],Offset(w*.18f,h*.84f),Offset(w*.82f,h*.84f),2.dp.toPx());drawLabel("a ≤ √n ≤ b",Offset(w*.5f,h*.94f),23.sp.value,colors[selected],Paint.Align.CENTER)
        }
    }
}

@Composable
private fun PrimeGapsExplorerScreen(playback:ProofPlayback,onBack:()->Unit){
    val primes=listOf(2,3,5,7,11,13,17,19,23,29,31,37,41,43,47,53);var index by remember{mutableStateOf(8)};val p=primes[index];val q=primes[index+1];val gap=q-p;val blue=Color(0xFF1359D1);val ink=Color(0xFF202830)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Prime Gaps Explorer",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("55 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Prime gap explorer selected consecutive primes $p and $q, with gap $gap. Tap left or right to inspect adjacent prime gaps."}.pointerInput(index){detectTapGestures{t->index=if(t.x<size.width/2)(index-1).coerceAtLeast(0)else(index+1).coerceAtMost(primes.size-2)}}){
            drawSoftGrid();val w=size.width;val h=size.height;val x0=w*.04f;val x1=w*.96f
            fun x(n:Int)=x0+(x1-x0)*(n-2)/51f
            val y=h*.36f;drawLine(ink,Offset(x0,y),Offset(x1,y),2.dp.toPx());primes.forEachIndexed{i,n->val cx=x(n);val hue=Color.hsv((i*23f)%300f+10f,.78f,.92f);drawCircle(if(i==index||i==index+1)blue else hue,if(i==index||i==index+1) 10.dp.toPx() else 7.dp.toPx(),Offset(cx,y));if(i<primes.lastIndex){val ex=x(primes[i+1]);val path=Path().apply{moveTo(cx,y);quadraticTo((cx+ex)/2,y-(30+(primes[i+1]-n)*8).dp.toPx(),ex,y)};drawPath(path,if(i==index)blue else hue,style=Stroke(if(i==index) 2.5.dp.toPx() else 1.3.dp.toPx()))}};for(n in 5..50 step 5){drawLine(ink,Offset(x(n),y-7.dp.toPx()),Offset(x(n),y+7.dp.toPx()),1.dp.toPx());drawLabel("$n",Offset(x(n),y+28.dp.toPx()),14.sp.value,ink,Paint.Align.CENTER)}
            val railY=h*.50f;drawLine(Color(0xFFD5D1CB),Offset(w*.17f,railY),Offset(w*.83f,railY),5.dp.toPx());drawLine(blue,Offset(w*.34f,railY),Offset(w*.70f,railY),5.dp.toPx());drawCircle(Color.White,23.dp.toPx(),Offset(w*.34f,railY));drawCircle(Color.White,23.dp.toPx(),Offset(w*.70f,railY));drawLabel("‹",Offset(w*.34f,railY+11.dp.toPx()),31.sp.value,ink,Paint.Align.CENTER);drawLabel("›",Offset(w*.70f,railY+11.dp.toPx()),31.sp.value,ink,Paint.Align.CENTER);drawRoundRect(Color.White,Offset(w*.06f,railY-23.dp.toPx()),Size(46.dp.toPx(),46.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("−",Offset(w*.06f+23.dp.toPx(),railY+10.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);drawRoundRect(Color.White,Offset(w*.86f,railY-23.dp.toPx()),Size(46.dp.toPx(),46.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawLabel("+",Offset(w*.86f+23.dp.toPx(),railY+10.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER)
            val detailY=h*.70f;drawCircle(Color.White,32.dp.toPx(),Offset(w*.12f,detailY));drawCircle(blue,32.dp.toPx(),Offset(w*.12f,detailY),style=Stroke(2.dp.toPx()));drawLabel("$p",Offset(w*.12f,detailY+13.dp.toPx()),29.sp.value,blue,Paint.Align.CENTER);drawCircle(Color.White,32.dp.toPx(),Offset(w*.88f,detailY));drawCircle(blue,32.dp.toPx(),Offset(w*.88f,detailY),style=Stroke(2.dp.toPx()));drawLabel("$q",Offset(w*.88f,detailY+13.dp.toPx()),29.sp.value,blue,Paint.Align.CENTER);drawLine(blue,Offset(w*.16f,detailY),Offset(w*.84f,detailY),2.dp.toPx());for(n in p+1 until q){val cx=w*(.16f+(n-p).toFloat()/gap*.68f);drawCircle(blue,4.dp.toPx(),Offset(cx,detailY));drawLine(blue,Offset(cx,detailY),Offset(cx,detailY-20.dp.toPx()),1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx())));drawLabel("$n",Offset(cx,detailY-27.dp.toPx()),12.sp.value,blue,Paint.Align.CENTER)};val badge=Rect(w*.41f,h*.77f,w*.59f,h*.82f);drawRoundRect(Color(0xFFEAF2FF),badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()));drawRoundRect(blue,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(1.dp.toPx()));drawLabel("gap = $gap",Offset(badge.center.x,badge.center.y+9.dp.toPx()),18.sp.value,blue,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun TwinPrimesExplorerScreen(playback:ProofPlayback,onBack:()->Unit){
    val twins=listOf(3 to 5,5 to 7,11 to 13,17 to 19,29 to 31);var selected by remember{mutableStateOf(2)};val cyan=Color(0xFF009CB7);val amber=Color(0xFFFFB33F);val violet=Color(0xFF4550A3);val ink=Color(0xFF20262B)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Twin Primes Explorer",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("56 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{val t=twins[selected];contentDescription="Twin primes explorer selected ${t.first} and ${t.second}. They are both prime and differ by 2. Their midpoint ${t.first+1} is a multiple of 6 for twin primes above 3. Tap a row or the controls to select another pair."}.pointerInput(selected){detectTapGestures{p->selected=((p.y/size.height*5).toInt()).coerceIn(0,4)}}){
            drawSoftGrid();val w=size.width;val h=size.height
            twins.forEachIndexed{i,(a,b)->val y=h*(.12f+i*.17f);val start=a-3;val end=b+5;drawLine(Color.Gray,Offset(w*.04f,y),Offset(w*.86f,y),1.dp.toPx());for(n in start..end){val x=w*(.08f+(n-start).toFloat()/(end-start)*.72f);drawCircle(ink,4.dp.toPx(),Offset(x,y));drawLabel("$n",Offset(x,y-17.dp.toPx()),13.sp.value,ink,Paint.Align.CENTER);if(n==a||n==b){drawCircle(if(i==selected)amber else amber.copy(alpha=.7f),20.dp.toPx(),Offset(x,y-20.dp.toPx()));drawLabel("$n",Offset(x,y-12.dp.toPx()),17.sp.value,ink,Paint.Align.CENTER)}};val xa=w*(.08f+(a-start).toFloat()/(end-start)*.72f);val xb=w*(.08f+(b-start).toFloat()/(end-start)*.72f);val bracket=Path().apply{moveTo(xa,y-48.dp.toPx());lineTo(xa,y-62.dp.toPx());lineTo(xb,y-62.dp.toPx());lineTo(xb,y-48.dp.toPx())};drawPath(bracket,cyan,style=Stroke(2.dp.toPx()));drawLabel("+2",Offset((xa+xb)/2,y-68.dp.toPx()),16.sp.value,cyan,Paint.Align.CENTER);val badge=Rect(w*.88f,y-22.dp.toPx(),w*.98f,y+22.dp.toPx());drawRoundRect(Color.White,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()));drawRoundRect(if(i==selected)violet else cyan,badge.topLeft,badge.size,androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),style=Stroke(1.5.dp.toPx()));drawLabel("$a,$b",Offset(badge.center.x,badge.center.y+7.dp.toPx()),13.sp.value,ink,Paint.Align.CENTER);if(i==selected){val mid=a+1;drawCircle(Color(0xFFD9E2FF),24.dp.toPx(),Offset((xa+xb)/2,y));drawLabel("$mid",Offset((xa+xb)/2,y+8.dp.toPx()),19.sp.value,ink,Paint.Align.CENTER);if(a>5){drawLabel("${mid/6} × 6 = $mid",Offset((xa+xb)/2,y+44.dp.toPx()),16.sp.value,violet,Paint.Align.CENTER)}}}
            val ctrl=Rect(w*.04f,h*.91f,w*.96f,h*.98f);drawRoundRect(Color.White,ctrl.topLeft,ctrl.size,androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()));drawLabel("▦",Offset(w*.11f,ctrl.center.y+10.dp.toPx()),28.sp.value,ink,Paint.Align.CENTER);drawLabel("±6",Offset(w*.25f,ctrl.center.y+10.dp.toPx()),23.sp.value,ink,Paint.Align.CENTER);drawLabel("−",Offset(w*.39f,ctrl.center.y+10.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);repeat(5){i->drawCircle(if(i==selected)violet else Color.LightGray,4.dp.toPx(),Offset(w*(.46f+i*.04f),ctrl.center.y))};drawLabel("+",Offset(w*.70f,ctrl.center.y+10.dp.toPx()),27.sp.value,ink,Paint.Align.CENTER);drawLabel("⌖",Offset(w*.87f,ctrl.center.y+10.dp.toPx()),28.sp.value,violet,Paint.Align.CENTER)
        }
    }
}

@Composable
private fun FundamentalArithmeticScreen(playback:ProofPlayback,onBack:()->Unit){
    var selected by remember{mutableStateOf(0)};val coral=Color(0xFFFF6250);val amber=Color(0xFFFFB22E);val cyan=Color(0xFF0FAFC5);val ink=Color(0xFF252B31)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){
        Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=40.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Fundamental Theorem of Arithmetic",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,maxLines=1,modifier=Modifier.weight(1f));Text("57 / 69",fontSize=14.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=4.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Three different factor trees for 360 all finish with the same primes: three factors of 2, two factors of 3, and one factor of 5. Selected tree ${selected+1}. Tap a tree to compare decompositions."}.pointerInput(selected){detectTapGestures{p->selected=(p.y/size.height*3).toInt().coerceIn(0,2)}}){
            drawSoftGrid();val w=size.width;val h=size.height;drawLabel("360",Offset(w*.5f,h*.065f),42.sp.value,ink,Paint.Align.CENTER)
            fun pill(center:Offset,text:String,width:Float=82.dp.toPx()){val r=Rect(center.x-width/2,center.y-18.dp.toPx(),center.x+width/2,center.y+18.dp.toPx());drawRoundRect(ink,r.topLeft,r.size,androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLabel(text,Offset(center.x,center.y+8.dp.toPx()),17.sp.value,Color.White,Paint.Align.CENTER)}
            fun leaf(center:Offset,n:Int){val c=if(n==2)coral else if(n==3)amber else cyan;drawCircle(c,18.dp.toPx(),center);drawLabel("$n",Offset(center.x,center.y+8.dp.toPx()),18.sp.value,Color.White,Paint.Align.CENTER)}
            fun tree(y:Float,root:String,second:List<String>,leaves:List<Int>){val alpha=if(selected==((y/h*3).toInt().coerceIn(0,2)))1f else .72f;pill(Offset(w*.5f,y),root);second.forEachIndexed{i,s->val x=w*(.30f+i*.40f);drawLine(ink.copy(alpha=alpha),Offset(w*.5f,y+18.dp.toPx()),Offset(x,y+50.dp.toPx()),1.5.dp.toPx());pill(Offset(x,y+65.dp.toPx()),s,w*.15f)};leaves.forEachIndexed{i,n->val x=w*(.17f+i*.13f);val parent=if(i<3)w*.30f else w*.70f;drawLine(ink.copy(alpha=alpha),Offset(parent,y+82.dp.toPx()),Offset(x,y+112.dp.toPx()),1.2.dp.toPx());leaf(Offset(x,y+126.dp.toPx()),n)}}
            tree(h*.12f,"36 × 10",listOf("6 × 6","2 × 5"),listOf(2,3,2,3,2,5));tree(h*.39f,"24 × 15",listOf("6 × 4","3 × 5"),listOf(2,3,2,2,3,5));tree(h*.66f,"6 × 60",listOf("2 × 3","6 × 10"),listOf(2,3,2,3,2,5));val tray=Rect(w*.10f,h*.88f,w*.90f,h*.94f);drawRoundRect(Color.White,tray.topLeft,tray.size,androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()));listOf(2,2,2,3,3,5).forEachIndexed{i,n->leaf(Offset(w*(.22f+i*.11f),tray.center.y),n)};drawLabel("2³ · 3² · 5",Offset(w*.5f,h*.985f),31.sp.value,ink,Paint.Align.CENTER)
        }
    }
}

@Composable private fun InfinitelyManyPrimesScreen(playback:ProofPlayback,onBack:()->Unit){
    var count by remember{mutableStateOf(3)};val ps=listOf(2,3,5,7,11);val product=ps.take(count).fold(1){a,b->a*b};val n=product+1;val ink=Color(0xFF272D32);val colors=listOf(Color(0xFFF25E4A),Color(0xFFF3A316),Color(0xFF514CB8))
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)){Row(Modifier.fillMaxWidth().height(72.dp).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically){Text("←",fontSize=41.sp,color=ink,modifier=Modifier.clickable(onClick=onBack).semantics{contentDescription="Back"});Text("Infinitely Many Primes",fontSize=22.sp,fontWeight=FontWeight.Bold,color=ink,textAlign=TextAlign.Center,modifier=Modifier.weight(1f));Text("58 / 69",fontSize=15.sp,fontWeight=FontWeight.Bold,color=ink);Text("⋮",fontSize=30.sp,color=ink,modifier=Modifier.padding(start=5.dp))}
        Canvas(Modifier.fillMaxWidth().weight(1f).semantics{contentDescription="Euclid prime machine multiplies the first $count primes to get $product, then adds 1 to get $n. Division by every chosen prime leaves remainder 1, so a new prime factor must exist. Tap the crank to add another starting prime."}.pointerInput(count){detectTapGestures{count=if(count==5)2 else count+1}}){drawSoftGrid();val w=size.width;val h=size.height;val box=Rect(w*.16f,h*.04f,w*.84f,h*.16f);drawRoundRect(Color.Transparent,box.topLeft,box.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),style=Stroke(1.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),5.dp.toPx()))));ps.take(count).forEachIndexed{i,p->val cx=w*(.24f+i*.13f);drawRoundRect(ink,Offset(cx-25.dp.toPx(),h*.065f),Size(50.dp.toPx(),50.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()));drawLabel("$p",Offset(cx,h*.10f),24.sp.value,Color.White,Paint.Align.CENTER)};val machine=Rect(w*.31f,h*.22f,w*.69f,h*.36f);drawRoundRect(ink,machine.topLeft,machine.size,androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()));drawLabel("×",Offset(machine.center.x,machine.center.y+22.dp.toPx()),50.sp.value,Color.White,Paint.Align.CENTER);drawLabel("$product",Offset(w*.5f,h*.42f),28.sp.value,ink,Paint.Align.CENTER);drawRoundRect(ink,Offset(w*.43f,h*.46f),Size(w*.14f,50.dp.toPx()),androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()));drawLabel("+1",Offset(w*.5f,h*.495f),27.sp.value,Color.White,Paint.Align.CENTER);drawCircle(Color(0xFF14A9C1),38.dp.toPx(),Offset(w*.5f,h*.58f));drawLabel("$n",Offset(w*.5f,h*.59f),25.sp.value,Color.White,Paint.Align.CENTER);drawLabel("${ps.take(count).joinToString("·")} + 1 = $n",Offset(w*.5f,h*.65f),25.sp.value,ink,Paint.Align.CENTER);ps.take(count).take(3).forEachIndexed{i,p->val y=h*(.72f+i*.075f);val r=Rect(w*.13f,y,w*.87f,y+45.dp.toPx());drawRoundRect(colors[i],r.topLeft,Size(r.width*.34f,r.height),androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()));drawLabel("$n ÷ $p",Offset(w*.29f,y+30.dp.toPx()),19.sp.value,Color.White,Paint.Align.CENTER);drawLabel("=   remainder 1",Offset(w*.63f,y+30.dp.toPx()),19.sp.value,colors[i],Paint.Align.CENTER)};drawCircle(Color(0xFF514CB8),38.dp.toPx(),Offset(w*.66f,h*.95f));drawLabel("new",Offset(w*.66f,h*.96f),18.sp.value,Color.White,Paint.Align.CENTER)} }
}

@Composable
private fun ModularArithmeticClockScreen(playback: ProofPlayback, onBack: () -> Unit) {
    var modulus by remember { mutableStateOf(7) }
    var start by remember { mutableStateOf(5) }
    val add = 4
    val result = (start + add) % modulus
    val coral = Color(0xFFF25D45)
    val amber = Color(0xFFFFA51A)
    val cyan = Color(0xFF18AFBF)
    val violet = Color(0xFF5150AE)
    val ink = Color(0xFF273038)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 40.sp, color = ink, modifier = Modifier.clickable(onClick = onBack).semantics { contentDescription = "Back" })
            Text("Modular Arithmetic as a Clock", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            Text("59 / 69", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
            Text("⋮", fontSize = 30.sp, color = ink, modifier = Modifier.padding(start = 4.dp))
        }
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics {
                    contentDescription = "Modular clock modulo $modulus. Starting at $start and moving $add steps lands on $result. Tap a clock number to change the start, or tap the lower modulus ring to change the modulus."
                    onClick { modulus = if (modulus == 10) 5 else modulus + 1; start %= modulus; true }
                }
                .pointerInput(modulus, start) {
                    detectTapGestures { p ->
                        if (p.y > size.height * .74f) {
                            modulus = if (modulus == 10) 5 else modulus + 1
                            start %= modulus
                        } else {
                            val c = Offset(size.width / 2f, size.height * .29f)
                            val angle = (Math.toDegrees(atan2((p.y - c.y).toDouble(), (p.x - c.x).toDouble())) + 450) % 360
                            start = (angle / (360.0 / modulus)).roundToInt() % modulus
                        }
                    }
                },
        ) {
            drawSoftGrid()
            val w = size.width
            val h = size.height
            val center = Offset(w * .5f, h * .29f)
            val radius = min(w * .36f, h * .25f)
            val badge = Rect(w * .41f, h * .025f, w * .59f, h * .075f)
            drawRoundRect(Color.White, badge.topLeft, badge.size, androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
            drawRoundRect(Color(0xFFD5D1CA), badge.topLeft, badge.size, androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()), style = Stroke(1.dp.toPx()))
            drawLabel("mod $modulus", Offset(badge.center.x, badge.center.y + 10.dp.toPx()), 20.sp.value, ink, Paint.Align.CENTER)
            drawCircle(ink, radius, center, style = Stroke(4.dp.toPx()))
            for (n in 0 until modulus) {
                val a = Math.toRadians(n * 360.0 / modulus - 90)
                val p = Offset(center.x + cos(a).toFloat() * radius, center.y + sin(a).toFloat() * radius)
                drawLine(Color(0xFFD2CEC8), center, p, 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())))
                drawCircle(if (n == start) coral else if (n == result) cyan else ink, if (n == start || n == result) 27.dp.toPx() else 23.dp.toPx(), p)
                if (n == start || n == result) drawCircle(Color.White, 30.dp.toPx(), p, style = Stroke(2.dp.toPx()))
                drawLabel("$n", Offset(p.x, p.y + 11.dp.toPx()), 28.sp.value, Color.White, Paint.Align.CENTER)
            }
            val path = Path()
            for (i in 0 until add) {
                val n = (start - i + modulus * 2) % modulus
                val a = Math.toRadians(n * 360.0 / modulus - 90)
                val p = Offset(center.x + cos(a).toFloat() * radius * .92f, center.y + sin(a).toFloat() * radius * .92f)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, amber, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            drawLabel("$start + $add ≡ $result", Offset(w * .5f, h * .60f), 43.sp.value, ink, Paint.Align.CENTER)
            drawArc(violet, -86f, 345f, false, Offset(w * .40f, h * .68f), Size(w * .20f, w * .20f), style = Stroke(6.dp.toPx()))
            drawLabel("$modulus", Offset(w * .5f, h * .745f), 39.sp.value, ink, Paint.Align.CENTER)
        }
    }
}

@Composable
private fun ProofMockupCanvas(
    title: String,
    index: Int,
    onBack: () -> Unit,
    description: String,
    onTap: (Offset, Size) -> Unit,
    draw: DrawScope.() -> Unit,
) {
    val ink = Color(0xFF273038)
    Column(Modifier.fillMaxSize().background(ProofIvory).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("←", fontSize = 40.sp, color = ink, modifier = Modifier.clickable(onClick = onBack).semantics { contentDescription = "Back" })
            Text(title, fontSize = when { title.length > 29 -> 15.sp; title.length > 23 -> 17.sp; else -> 21.sp }, fontWeight = FontWeight.Bold, color = ink, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            Text("$index / 69", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink)
            Text("⋮", fontSize = 30.sp, color = ink, modifier = Modifier.padding(start = 4.dp))
        }
        Canvas(
            Modifier.fillMaxWidth().weight(1f)
                .semantics { contentDescription = description; onClick { onTap(Offset.Zero, Size(1f, 1f)); true } }
                .pointerInput(description) { detectTapGestures { onTap(it, Size(size.width.toFloat(), size.height.toFloat())) } },
            onDraw = draw,
        )
    }
}

private fun DrawScope.clockPoint(center: Offset, radius: Float, value: Int, modulus: Int): Offset {
    val angle = Math.toRadians(value * 360.0 / modulus - 90)
    return Offset(center.x + cos(angle).toFloat() * radius, center.y + sin(angle).toFloat() * radius)
}

private fun exponent(value: Int): String = value.toString().map { character ->
    when (character) {
        '-' -> '⁻'; '0' -> '⁰'; '1' -> '¹'; '2' -> '²'; '3' -> '³'; '4' -> '⁴'
        '5' -> '⁵'; '6' -> '⁶'; '7' -> '⁷'; '8' -> '⁸'; '9' -> '⁹'; else -> character
    }
}.joinToString("")

@Composable
private fun ModularAdditionScreen(onBack: () -> Unit) {
    var first by remember { mutableStateOf(5) }
    var second by remember { mutableStateOf(6) }
    val modulus = 8
    val result = (first + second) % modulus
    val ink = Color(0xFF273038); val coral = Color(0xFFFF5B49); val cyan = Color(0xFF12AEDA); val violet = Color(0xFF5951C8)
    ProofMockupCanvas("Addition Modulo n", 60, onBack, "$first plus $second is congruent to $result modulo 8. Tap the left or right half to change either addend.", { p, s -> if (p.x < s.width / 2) first = (first + 1) % modulus else second = (second + 1) % modulus }) {
        drawSoftGrid(); val w = size.width; val h = size.height; val c = Offset(w * .5f, h * .34f); val r = w * .31f
        drawCircle(ink, r, c, style = Stroke(4.dp.toPx()))
        for (n in 0 until modulus) { val p = clockPoint(c, r, n, modulus); drawCircle(ink, 12.dp.toPx(), p); drawLabel("$n", p + Offset(if (n < 5) 28.dp.toPx() else -28.dp.toPx(), 10.dp.toPx()), 20.sp.value, ink, Paint.Align.CENTER) }
        fun arcPath(start: Int, steps: Int, radius: Float): Path { val path = Path(); for (i in 0..steps) { val p = clockPoint(c, radius, (start + i) % modulus, modulus); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }; return path }
        drawPath(arcPath(0, first, r), coral, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round)); drawPath(arcPath(first, second, r * .88f), cyan, style = Stroke(7.dp.toPx(), cap = StrokeCap.Round))
        listOf(0 to coral, first to cyan, result to violet).forEach { (n, color) -> val p = clockPoint(c, if (n == result) r * .88f else r, n, modulus); drawCircle(Color.White, 24.dp.toPx(), p); drawCircle(color, 18.dp.toPx(), p) }
        drawLabel("$modulus", c + Offset(0f, 28.dp.toPx()), 55.sp.value, ink, Paint.Align.CENTER)
        drawLabel("$first  +  $second  ≡  $result  (mod $modulus)", Offset(w * .5f, h * .78f), 35.sp.value, ink, Paint.Align.CENTER)
    }
}

@Composable
private fun ModularMultiplicationScreen(onBack: () -> Unit) {
    var multiplier by remember { mutableStateOf(5) }; val step = 3; val modulus = 7; val result = multiplier * step % modulus
    val ink = Color(0xFF273038); val coral = Color(0xFFF15E4B); val colors = listOf(coral, Color(0xFFF5A51B), Color(0xFF1AA9B8), Color(0xFF4E7ED5), Color(0xFF6B47C4))
    ProofMockupCanvas("Multiplication Modulo n", 61, onBack, "$multiplier jumps of $step land at $result modulo $modulus. Tap to change the number of repeated additions.", { _, _ -> multiplier = if (multiplier == 6) 2 else multiplier + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; val c = Offset(w * .5f, h * .27f); val r = w * .31f; drawCircle(ink, r, c, style = Stroke(3.dp.toPx()))
        for (n in 0 until modulus) { val p = clockPoint(c, r, n, modulus); drawCircle(if (n == result) coral else ink, if (n == result) 22.dp.toPx() else 14.dp.toPx(), p); drawLabel("$n", p + Offset(0f, if (n < 4) -27.dp.toPx() else 36.dp.toPx()), 23.sp.value, if (n == result) coral else ink, Paint.Align.CENTER) }
        for (k in 0 until multiplier) { val a = clockPoint(c, r * (.98f - k * .09f), k * step % modulus, modulus); val b = clockPoint(c, r * (.98f - k * .09f), (k + 1) * step % modulus, modulus); val path = Path().apply { moveTo(a.x, a.y); quadraticBezierTo(c.x, c.y, b.x, b.y) }; drawPath(path, colors[k % colors.size].copy(alpha = .75f), style = Stroke(3.dp.toPx())) }
        val tableTop = h * .55f; drawRoundRect(Color.White, Offset(w * .06f, tableTop), Size(w * .88f, h * .22f), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())); for (i in 0..multiplier) { val x = w * (.14f + i * .72f / multiplier.coerceAtLeast(1)); drawLabel("$i", Offset(x, tableTop + 35.dp.toPx()), 19.sp.value, ink, Paint.Align.CENTER); drawLabel("${i * step % modulus}", Offset(x, tableTop + 92.dp.toPx()), 22.sp.value, colors.getOrElse((i - 1).coerceAtLeast(0)) { colors.last() }, Paint.Align.CENTER) }
        drawLabel("$multiplier × $step ≡ $result  (mod $modulus)", Offset(w * .5f, h * .84f), 34.sp.value, ink, Paint.Align.CENTER); drawLabel((1..multiplier).joinToString(" + ") { "$step" }, Offset(w * .5f, h * .90f), 21.sp.value, coral, Paint.Align.CENTER)
    }
}

@Composable
private fun NegativeModuloScreen(onBack: () -> Unit) {
    var magnitude by remember { mutableStateOf(4) }; val modulus = 9; val result = (modulus - magnitude % modulus) % modulus; val ink = Color(0xFF17213A); val coral = Color(0xFFFF5B49); val cyan = Color(0xFF08A9C0)
    ProofMockupCanvas("Negative Numbers Modulo n", 62, onBack, "Negative $magnitude is congruent to $result modulo $modulus. Tap to increase the negative magnitude.", { _, _ -> magnitude = if (magnitude == 8) 1 else magnitude + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; val c = Offset(w * .5f, h * .31f); val r = w * .35f; drawCircle(ink, r, c, style = Stroke(4.dp.toPx())); drawArc(coral, 90f, 180f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(6.dp.toPx())); drawArc(cyan, -90f, 180f, false, c - Offset(r, r), Size(r * 2, r * 2), style = Stroke(6.dp.toPx()))
        for (n in 0 until modulus) { val p = clockPoint(c, r, n, modulus); drawLine(Color(0xFFD4D2CE), c, p, 1.dp.toPx()); drawCircle(if (n == result) ink else ink, if (n == result) 27.dp.toPx() else 22.dp.toPx(), p); drawLabel("$n", p + Offset(0f, 10.dp.toPx()), 23.sp.value, Color.White, Paint.Align.CENTER) }
        drawLabel("−", Offset(w * .27f, h * .10f), 48.sp.value, coral, Paint.Align.CENTER); drawLabel("+", Offset(w * .73f, h * .10f), 48.sp.value, cyan, Paint.Align.CENTER)
        drawLabel("−$magnitude ≡ $result  (mod $modulus)", Offset(w * .5f, h * .68f), 39.sp.value, ink, Paint.Align.CENTER); val y = h * .83f; drawLine(coral, Offset(w * .05f, y), Offset(w * .5f, y), 6.dp.toPx()); drawLine(cyan, Offset(w * .5f, y), Offset(w * .95f, y), 6.dp.toPx()); drawCircle(ink, 24.dp.toPx(), Offset(w * .5f, y)); drawLabel("0", Offset(w * .5f, y + 9.dp.toPx()), 22.sp.value, Color.White, Paint.Align.CENTER)
    }
}

@Composable
private fun RemainderClassesScreen(onBack: () -> Unit) {
    var modulus by remember { mutableStateOf(5) }; val palette = listOf(Color(0xFFFF5B55), Color(0xFFF1A51A), Color(0xFF17B4C5), Color(0xFF4D76DD), Color(0xFF8345CC), Color(0xFF2E9B72)); val ink = Color(0xFF273038)
    ProofMockupCanvas("Remainder Classes", 63, onBack, "Integers from negative 8 through 12 are partitioned into $modulus remainder classes. Tap to change the modulus.", { _, _ -> modulus = if (modulus == 6) 3 else modulus + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; val top = h * .15f; val bottom = h * .90f; val left = w * .18f; val gap = w * .64f / (modulus - 1).coerceAtLeast(1); val badge = Rect(w * .41f, h * .015f, w * .59f, h * .075f); drawRoundRect(ink, badge.topLeft, badge.size, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())); drawLabel("mod $modulus", Offset(w * .5f, h * .057f), 20.sp.value, Color.White, Paint.Align.CENTER)
        for (r in 0 until modulus) { val x = left + gap * r; val color = palette[r]; drawLine(color.copy(alpha = .28f), Offset(x, top), Offset(x, bottom), 4.dp.toPx()); drawRoundRect(color, Offset(x - 27.dp.toPx(), top - 40.dp.toPx()), Size(54.dp.toPx(), 36.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(7.dp.toPx())); drawLabel("$r", Offset(x, top - 14.dp.toPx()), 22.sp.value, Color.White, Paint.Align.CENTER); for (n in -8..12) if (((n % modulus) + modulus) % modulus == r) { val y = top + (n + 8) / 20f * (bottom - top); drawCircle(color, 17.dp.toPx(), Offset(x, y)); drawLabel("$n", Offset(x, y + 7.dp.toPx()), 16.sp.value, Color.White, Paint.Align.CENTER) }; drawLabel(((-modulus + r).toString()) + ", $r, ${modulus + r}…", Offset(x, h * .96f), 13.sp.value, color, Paint.Align.CENTER) }
    }
}

private fun DrawScope.powerBlock(center: Offset, color: Color, label: String = "x", width: Float = 76.dp.toPx(), height: Float = 55.dp.toPx(), alpha: Float = 1f) {
    val rect = Rect(center.x - width / 2, center.y - height / 2, center.x + width / 2, center.y + height / 2)
    drawRoundRect(color.copy(alpha = alpha), rect.topLeft, rect.size, androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
    drawRoundRect(color.copy(alpha = alpha), rect.topLeft, rect.size, androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()), style = Stroke(1.5.dp.toPx()))
    drawLabel(label, center + Offset(0f, 12.dp.toPx()), 27.sp.value, Color.White.copy(alpha = alpha), Paint.Align.CENTER)
}

@Composable
private fun ProductPowersScreen(onBack: () -> Unit) {
    var left by remember { mutableStateOf(3) }; var right by remember { mutableStateOf(2) }; val sum = left + right; val cyan = Color(0xFF12A7C5); val coral = Color(0xFFFF685A); val violet = Color(0xFF514BAA); val ink = Color(0xFF273038)
    ProofMockupCanvas("Product of Powers", 64, onBack, "x to the $left times x to the $right equals x to the $sum. Tap left or right to change an exponent.", { p, s -> if (p.x < s.width / 2) left = if (left == 5) 1 else left + 1 else right = if (right == 4) 1 else right + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; drawLabel("x${exponent(left)}  ·  x${exponent(right)}  =  x${exponent(sum)}", Offset(w * .5f, h * .14f), 51.sp.value, ink, Paint.Align.CENTER); drawLabel("$left  +  $right  =  $sum", Offset(w * .5f, h * .24f), 38.sp.value, ink, Paint.Align.CENTER)
        val baseY = h * .57f; for (i in 0 until left) powerBlock(Offset(w * .19f, baseY - i * 62.dp.toPx()), cyan); for (i in 0 until right) powerBlock(Offset(w * .40f, baseY - i * 62.dp.toPx()), coral); drawLabel("→", Offset(w * .57f, baseY - 60.dp.toPx()), 48.sp.value, ink, Paint.Align.CENTER); for (i in 0 until sum) powerBlock(Offset(w * .75f, baseY - i * 62.dp.toPx()), violet)
        drawLabel("$left", Offset(w * .08f, baseY - left * 31.dp.toPx()), 33.sp.value, cyan, Paint.Align.CENTER); drawLabel("$right", Offset(w * .51f, baseY - right * 31.dp.toPx()), 33.sp.value, coral, Paint.Align.CENTER); drawLabel("$sum", Offset(w * .91f, baseY - sum * 31.dp.toPx()), 33.sp.value, violet, Paint.Align.CENTER)
    }
}

@Composable
private fun QuotientPowersScreen(onBack: () -> Unit) {
    var denominator by remember { mutableStateOf(3) }; val numerator = 7; val remaining = numerator - denominator; val coral = Color(0xFFFF5D55); val cyan = Color(0xFF21B2D4); val violet = Color(0xFF5845CC); val ink = Color(0xFF273038)
    ProofMockupCanvas("Quotient of Powers", 65, onBack, "x to the $numerator divided by x to the $denominator cancels $denominator factors and leaves x to the $remaining. Tap to change the denominator.", { _, _ -> denominator = if (denominator == 6) 1 else denominator + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; val top = h * .08f; for (i in 0 until numerator) powerBlock(Offset(w * .41f, top + i * 48.dp.toPx()), if (i < remaining) coral else Color(0xFFB8B8B8), width = 100.dp.toPx(), height = 42.dp.toPx(), alpha = if (i < remaining) 1f else .5f); drawLine(ink, Offset(w * .20f, h * .43f), Offset(w * .80f, h * .43f), 3.dp.toPx()); for (i in 0 until denominator) powerBlock(Offset(w * .41f, h * .48f + i * 48.dp.toPx()), cyan, width = 100.dp.toPx(), height = 42.dp.toPx())
        val railY = h * .69f; drawRoundRect(Color.White, Offset(w * .10f, railY - 28.dp.toPx()), Size(w * .80f, 56.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(28.dp.toPx())); for (i in 1..6) { val x = w * (.16f + (i - 1) * .136f); drawLine(if (i <= denominator) coral else Color.LightGray, Offset(if (i == 1) x else x - w * .136f, railY), Offset(x, railY), 3.dp.toPx()); drawCircle(if (i == denominator) coral else if (i < denominator) coral.copy(alpha = .8f) else Color.LightGray, if (i == denominator) 18.dp.toPx() else 12.dp.toPx(), Offset(x, railY)) }
        drawLabel("x${exponent(numerator)}", Offset(w * .35f, h * .81f), 45.sp.value, coral, Paint.Align.CENTER); drawLine(ink, Offset(w * .26f, h * .825f), Offset(w * .44f, h * .825f), 3.dp.toPx()); drawLabel("x${exponent(denominator)}", Offset(w * .35f, h * .88f), 45.sp.value, cyan, Paint.Align.CENTER); drawLabel("=  x${exponent(remaining)}", Offset(w * .62f, h * .85f), 48.sp.value, violet, Paint.Align.CENTER); drawLabel("$numerator − $denominator = $remaining", Offset(w * .5f, h * .95f), 34.sp.value, ink, Paint.Align.CENTER)
    }
}

@Composable
private fun PowerOfPowerScreen(onBack: () -> Unit) {
    var outer by remember { mutableStateOf(4) }; val inner = 3; val total = inner * outer; val violet = Color(0xFF5454D6); val coral = Color(0xFFFF5750); val cyan = Color(0xFF0CA6D1); val ink = Color(0xFF273038)
    ProofMockupCanvas("Power of a Power", 66, onBack, "$outer groups of x to the $inner create $total factors, so the exponents multiply. Tap to change the outer exponent.", { _, _ -> outer = if (outer == 5) 2 else outer + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height; drawLabel("x${exponent(inner)}", Offset(w * .5f, h * .11f), 30.sp.value, violet, Paint.Align.CENTER); for (i in 0 until inner) powerBlock(Offset(w * (.40f + i * .10f), h * .17f), violet, width = 45.dp.toPx(), height = 48.dp.toPx()); drawLabel("↓", Offset(w * .5f, h * .25f), 40.sp.value, Color.Gray, Paint.Align.CENTER)
        for (g in 0 until outer) for (i in 0 until inner) powerBlock(Offset(w * (.09f + g * .23f + i * .055f), h * .34f), violet, width = 34.dp.toPx(), height = 38.dp.toPx()); drawLabel("↓", Offset(w * .5f, h * .43f), 40.sp.value, Color.Gray, Paint.Align.CENTER)
        val cols = 4; for (i in 0 until total) powerBlock(Offset(w * (.32f + (i % cols) * .12f), h * (.51f + (i / cols) * .07f)), violet, width = 50.dp.toPx(), height = 50.dp.toPx()); drawLabel("(x${exponent(inner)})${exponent(outer)} = x${exponent(total)}", Offset(w * .5f, h * .79f), 45.sp.value, ink, Paint.Align.CENTER); drawCircle(violet, 34.dp.toPx(), Offset(w * .31f, h * .88f)); drawCircle(coral, 34.dp.toPx(), Offset(w * .49f, h * .88f)); drawCircle(cyan, 34.dp.toPx(), Offset(w * .70f, h * .88f)); drawLabel("$inner", Offset(w * .31f, h * .895f), 28.sp.value, Color.White, Paint.Align.CENTER); drawLabel("× $outer =", Offset(w * .50f, h * .895f), 28.sp.value, ink, Paint.Align.CENTER); drawLabel("$total", Offset(w * .70f, h * .895f), 28.sp.value, Color.White, Paint.Align.CENTER)
    }
}

@Composable
private fun ZeroExponentScreen(onBack: () -> Unit) {
    var stage by remember { mutableStateOf(3) }; val colors = listOf(Color(0xFF4E4BD4), Color(0xFF1DB9C6), Color(0xFFFF951F), Color(0xFFFF5B4E)); val ink = Color(0xFF273038)
    ProofMockupCanvas("Why a⁰ = 1", 67, onBack, "Dividing by a lowers the exponent one step. The current highlighted stage is a to the $stage. Tap to step down toward a to the zero equals one.", { _, _ -> stage = if (stage == 0) 3 else stage - 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height
        for (row in 0..3) {
            val exp = 3 - row; val y = h * (.14f + row * .20f); val side = exp.coerceAtLeast(1); val count = if (exp == 0) 1 else exp * exp
            drawLabel("a${exponent(exp)}", Offset(w * .12f, y), 40.sp.value, colors[exp], Paint.Align.CENTER)
            for (i in 0 until count) {
                val column = i % side; val blockRow = i / side
                val x = w * .41f + (column - (side - 1) / 2f) * 62.dp.toPx()
                val blockY = y + (blockRow - (side - 1) / 2f) * 54.dp.toPx()
                powerBlock(Offset(x, blockY), colors[exp], if (exp == 0) "1" else "a", width = 54.dp.toPx(), height = 48.dp.toPx())
            }
            if (row < 3) drawLabel("÷ a   ↓", Offset(w * .41f, y + h * .105f), 25.sp.value, ink, Paint.Align.CENTER)
            if (exp == stage) { drawCircle(colors[exp], 19.dp.toPx(), Offset(w * .86f, y)); drawLabel("a${exponent(exp)}", Offset(w * .93f, y + 8.dp.toPx()), 21.sp.value, colors[exp], Paint.Align.CENTER) }
        }
        drawRoundRect(Color.White, Offset(w * .31f, h * .88f), Size(w * .38f, 72.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(12.dp.toPx())); drawRoundRect(colors[0], Offset(w * .31f, h * .88f), Size(w * .38f, 72.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()), style = Stroke(2.dp.toPx())); drawLabel("a⁰ = 1", Offset(w * .5f, h * .925f), 39.sp.value, ink, Paint.Align.CENTER)
    }
}

@Composable
private fun NegativeExponentScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf(-3) }; val coral = Color(0xFFFF654E); val cyan = Color(0xFF17A8C5); val ink = Color(0xFF272D32)
    ProofMockupCanvas("Negative Exponents", 68, onBack, "The selected exponent is $selected, showing a to that exponent equals one over a to ${-selected}. Tap to cycle through exponents.", { _, _ -> selected = if (selected == 2) -3 else selected + 1 }) {
        drawSoftGrid(); val w = size.width; val h = size.height
        for (i in 0..5) {
            val exp = 2 - i; val y = h * (.10f + i * .13f)
            val left = if (exp >= 0) { if (exp == 0) "a⁰ = 1" else "a${exponent(exp)}" } else "a${exponent(exp)} = 1/a${exponent(-exp)}"
            val right = if (exp >= 0) "1/a${exponent(exp)}" else "1/a${exponent(exp)} = a${exponent(-exp)}"
            fun card(x: Float, text: String, color: Color) { val rect = Rect(x-w*.105f,y-35.dp.toPx(),x+w*.105f,y+35.dp.toPx()); drawRoundRect(Color.White,rect.topLeft,rect.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())); drawRoundRect(if(exp==selected) color else Color(0xFFBDB6A9),rect.topLeft,rect.size,androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),style=Stroke(if(exp==selected) 2.dp.toPx() else 1.dp.toPx())); drawLabel(text,Offset(x,y+7.dp.toPx()),if(text.length>7)13.sp.value else 18.sp.value,ink,Paint.Align.CENTER) }
            card(w*.31f,left,coral); card(w*.69f,right,cyan); drawLine(ink,Offset(w*.41f,y),Offset(w*.59f,y),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),5.dp.toPx()))); drawCircle(Color.White,10.dp.toPx(),Offset(w*.5f,y)); drawCircle(ink,10.dp.toPx(),Offset(w*.5f,y),style=Stroke(2.dp.toPx()))
        }
        drawLabel("×a", Offset(w * .12f, h * .20f), 25.sp.value, coral, Paint.Align.CENTER); drawLabel("÷a", Offset(w * .87f, h * .20f), 25.sp.value, cyan, Paint.Align.CENTER); val y = h * .88f; drawRoundRect(Color.White, Offset(w * .10f, y - 60.dp.toPx()), Size(w * .34f, 120.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())); drawRoundRect(Color.White, Offset(w * .56f, y - 60.dp.toPx()), Size(w * .34f, 120.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())); for (i in 0 until -selected.coerceAtMost(0)) { powerBlock(Offset(w * (.60f + i * .09f), y + 30.dp.toPx()), cyan, "a", 42.dp.toPx(), 42.dp.toPx()) }; drawLabel("a${exponent(selected)} = 1/a${exponent(-selected)}", Offset(w * .5f, h * .98f), 29.sp.value, ink, Paint.Align.CENTER)
    }
}

@Composable
private fun PerfectNumbersScreen(onBack: () -> Unit) {
    var number by remember { mutableStateOf(28) }
    val divisors = if (number == 28) listOf(1, 2, 4, 7, 14) else listOf(1, 2, 3)
    val colors = listOf(Color(0xFFFF5B55), Color(0xFFF0A11A), Color(0xFF18AFC0), Color(0xFF5969D8), Color(0xFFF0A11A))
    val ink = Color(0xFF272D32)
    ProofMockupCanvas("Perfect Numbers and Divisor Pairing", 69, onBack, "Proper divisors ${divisors.joinToString()} sum to ${divisors.sum()}, ${if (divisors.sum()==number) "so $number is perfect" else "so $number is not perfect"}. Tap to compare 28 and 6.", { _, _ -> number = if (number == 28) 6 else 28 }) {
        drawSoftGrid(); val w = size.width; val h = size.height
        val badge = Rect(w * .40f, h * .015f, w * .60f, h * .085f)
        drawRoundRect(ink, badge.topLeft, badge.size, androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()))
        drawLabel("$number", Offset(w * .5f, h * .067f), 45.sp.value, Color.White, Paint.Align.CENTER)
        val pairs = if (number == 28) listOf(1 to 28, 2 to 14, 4 to 7) else listOf(1 to 6, 2 to 3)
        pairs.forEachIndexed { i, (a, b) ->
            val y = h * (.18f + i * .12f); val color = colors[i]
            drawArc(color, 190f, 160f, false, Offset(w * .19f, y - 55.dp.toPx()), Size(w * .62f, 110.dp.toPx()), style = Stroke(4.dp.toPx()))
            drawCircle(Color.White, 25.dp.toPx(), Offset(w * .19f, y)); drawCircle(Color.White, 25.dp.toPx(), Offset(w * .81f, y))
            drawLabel("$a", Offset(w * .19f, y + 10.dp.toPx()), 24.sp.value, color, Paint.Align.CENTER); drawLabel("$b", Offset(w * .81f, y + 10.dp.toPx()), 24.sp.value, color, Paint.Align.CENTER)
            drawRoundRect(color, Offset(w * .44f, y - 18.dp.toPx()), Size(w * .12f, 36.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawLabel("$a×$b", Offset(w * .5f, y + 8.dp.toPx()), 17.sp.value, Color.White, Paint.Align.CENTER)
        }
        if (number == 28) {
            val dot = 7.dp.toPx(); val gap = 18.dp.toPx()
            for (row in 0 until 7) for (column in 0..row) {
                val x = w * .5f + (column - row / 2f) * gap; val y = h * .245f + row * gap
                drawCircle(ink, dot, Offset(x, y))
            }
            for (row in 0 until 7) for (column in 0..row) {
                val x = w * .5f + (column - row / 2f) * gap; val y = h * .535f + row * gap
                val highlight = when (row) { 0 -> colors[0]; 1 -> colors[1]; 3 -> colors[2]; 5 -> colors[3]; else -> Color.White }
                drawCircle(highlight, dot, Offset(x, y)); drawCircle(Color(0xFFBDB7AD), dot, Offset(x, y), style = Stroke(1.dp.toPx()))
            }
        } else {
            for (row in 0 until 3) for (column in 0..row) drawCircle(colors[row], 9.dp.toPx(), Offset(w * .5f + (column - row / 2f) * 25.dp.toPx(), h * .59f + row * 25.dp.toPx()))
        }
        val formula = divisors.joinToString(" + ") + " = ${divisors.sum()}"; val box = Rect(w * .15f, h * .75f, w * .85f, h * .82f)
        drawRoundRect(ink, box.topLeft, box.size, androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())); drawLabel(formula, Offset(w * .5f, h * .797f), 29.sp.value, Color.White, Paint.Align.CENTER)
        divisors.forEachIndexed { i, d -> val x = w * (.14f + i * .18f); drawRoundRect(Color.White, Offset(x - 32.dp.toPx(), h * .87f), Size(64.dp.toPx(), 80.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())); drawLabel("$d", Offset(x, h * .92f), 27.sp.value, colors[i % colors.size], Paint.Align.CENTER) }
    }
}

@Composable
private fun ProofControlButton(
    description: String,
    onClick: () -> Unit,
    icon: DrawScope.(Offset, Float) -> Unit,
) {
    Canvas(
        Modifier
            .size(56.dp)
            .shadow(10.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFD8DDE3), RoundedCornerShape(12.dp))
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick { onClick(); true }
            }
            .clickable(onClick = onClick),
    ) { icon(center, min(size.width, size.height) * .42f) }
}

@Composable
private fun TriangleAngleCanvas(
    playback: ProofPlayback,
    modifier: Modifier,
    onParameterChange: (String, Double) -> Unit,
) {
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val heightParameter = playback.frame.parameters["height"] ?: 3.0
    val offsetParameter = playback.frame.parameters["offset"] ?: 1.0

    Canvas(
        modifier
            .onSizeChanged { measuredSize = it }
            .semantics { contentDescription = "Interactive triangle angle sum proof. Drag the top vertex to change all three angles while their sum remains 180 degrees." }
            .pointerInput(measuredSize, heightParameter, offsetParameter) {
                var draggingApex = false
                detectDragGestures(
                    onDragStart = { touch ->
                        val w = measuredSize.width.toFloat().coerceAtLeast(1f)
                        val h = measuredSize.height.toFloat().coerceAtLeast(1f)
                        val apex = triangleApex(w, h, heightParameter, offsetParameter)
                        draggingApex = (touch - apex).getDistance() < 82.dp.toPx()
                    },
                    onDragEnd = { draggingApex = false },
                    onDragCancel = { draggingApex = false },
                ) { change, _ ->
                    if (!draggingApex) return@detectDragGestures
                    val w = measuredSize.width.toFloat().coerceAtLeast(1f)
                    val h = measuredSize.height.toFloat().coerceAtLeast(1f)
                    val x = change.position.x.coerceIn(w * .18f, w * .82f)
                    val y = change.position.y.coerceIn(h * .035f, h * .37f)
                    val nextOffset = 1.0 + ((x / w - .5) / .55) * 8.0
                    val nextHeight = ((h * .43f - y) / (h * .70f) * 6.0).toDouble()
                    onParameterChange("offset", nextOffset.coerceIn(-3.0, 5.0))
                    onParameterChange("height", nextHeight.coerceIn(.2, 6.0))
                    change.consume()
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val grid = 34.dp.toPx()
        var x = 0f
        while (x <= w) {
            drawLine(Color(0xFFEDE9E1), Offset(x, 0f), Offset(x, h), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f)))
            x += grid
        }
        var y = 0f
        while (y <= h) {
            drawLine(Color(0xFFEDE9E1), Offset(0f, y), Offset(w, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f)))
            y += grid
        }

        val left = Offset(w * .12f, h * .42f)
        val right = Offset(w * .88f, h * .44f)
        val apex = triangleApex(w, h, heightParameter, offsetParameter)
        val stroke = 3.2.dp.toPx()
        drawAngleSector(left, right, apex, 52.dp.toPx(), ProofCoral.copy(alpha = .68f))
        drawAngleSector(right, apex, left, 52.dp.toPx(), ProofAmber.copy(alpha = .64f))
        drawAngleSector(apex, left, right, 58.dp.toPx(), ProofCyan.copy(alpha = .68f))
        drawLine(ProofNavy, left, apex, stroke, StrokeCap.Round)
        drawLine(ProofNavy, apex, right, stroke, StrokeCap.Round)
        drawLine(ProofNavy, right, left, stroke, StrokeCap.Round)

        val angleA = playback.frame.measurements["∠A"] ?: 48.0
        val angleB = playback.frame.measurements["∠B"] ?: 54.0
        val angleC = playback.frame.measurements["∠C"] ?: 78.0
        drawLabel("${angleA.roundToInt()}°", left + Offset(58.dp.toPx(), -27.dp.toPx()), 22.sp.value, ProofNavy)
        drawLabel("${angleB.roundToInt()}°", right + Offset(-84.dp.toPx(), -25.dp.toPx()), 22.sp.value, ProofNavy)
        drawLabel("${angleC.roundToInt()}°", apex + Offset(35.dp.toPx(), 30.dp.toPx()), 22.sp.value, ProofNavy)
        listOf(left, right, apex).forEach { vertex ->
            drawCircle(Color.White, 18.dp.toPx(), vertex)
            drawCircle(ProofBlue, 18.dp.toPx(), vertex, style = Stroke(3.5.dp.toPx()))
        }

        val center = Offset(w * .495f, h * .78f)
        val radius = min(w * .22f, h * .12f)
        val lineY = center.y
        val lineStart = Offset(w * .12f, lineY)
        val lineEnd = Offset(w * .88f, lineY)
        drawLine(ProofNavy, lineStart, lineEnd, stroke, StrokeCap.Round)
        drawCircle(ProofNavy, 6.dp.toPx(), lineStart)
        drawCircle(ProofNavy, 6.dp.toPx(), lineEnd)
        drawCircle(ProofNavy, 6.dp.toPx(), center)

        val cumulativeA = angleA.toFloat()
        val cumulativeC = cumulativeA + angleC.toFloat()
        val arcTopLeft = center - Offset(radius, radius)
        val arcSize = Size(radius * 2f, radius * 2f)
        drawArc(ProofCoral.copy(alpha = .67f), 180f, angleA.toFloat(), true, topLeft = arcTopLeft, size = arcSize)
        drawArc(ProofCyan.copy(alpha = .67f), 180f + cumulativeA, angleC.toFloat(), true, topLeft = arcTopLeft, size = arcSize)
        drawArc(ProofAmber.copy(alpha = .67f), 180f + cumulativeC, angleB.toFloat(), true, topLeft = arcTopLeft, size = arcSize)
        drawArc(ProofNavy, 180f, 180f, false, topLeft = arcTopLeft, size = arcSize, style = Stroke(2.dp.toPx()))
        val cut1 = pointOnCircle(center, radius, 180f + cumulativeA)
        val cut2 = pointOnCircle(center, radius, 180f + cumulativeC)
        drawLine(ProofNavy, center, cut1, 2.dp.toPx())
        drawLine(ProofNavy, center, cut2, 2.dp.toPx())
        drawLabel("${angleA.roundToInt()}°", center + Offset(-radius * .58f, -radius * .28f), 20.sp.value, ProofNavy, Paint.Align.CENTER)
        drawLabel("${angleC.roundToInt()}°", center + Offset(0f, -radius * .52f), 20.sp.value, ProofNavy, Paint.Align.CENTER)
        drawLabel("${angleB.roundToInt()}°", center + Offset(radius * .58f, -radius * .28f), 20.sp.value, ProofNavy, Paint.Align.CENTER)

        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 9.dp.toPx()))
        drawTransferArrow(left + Offset(8.dp.toPx(), 14.dp.toPx()), center + Offset(-radius * .72f, -radius * .83f), ProofCoral, dash)
        drawTransferArrow(apex + Offset(0f, 50.dp.toPx()), center + Offset(0f, -radius - 10.dp.toPx()), ProofCyan, dash)
        drawTransferArrow(right + Offset(-8.dp.toPx(), 14.dp.toPx()), center + Offset(radius * .72f, -radius * .83f), ProofAmber, dash)
        drawLabel("180°", center + Offset(0f, 60.dp.toPx()), 34.sp.value, ProofNavy, Paint.Align.CENTER)
    }
}

private fun triangleApex(w: Float, h: Float, height: Double, offset: Double): Offset = Offset(
    w * (.5f + (((offset - 1.0) / 8.0) * .55).toFloat()),
    h * (.43f - ((height / 6.0) * .70).toFloat()),
)

private fun DrawScope.drawAngleSector(vertex: Offset, first: Offset, second: Offset, radius: Float, color: Color) {
    val a1 = Math.toDegrees(atan2((first.y - vertex.y).toDouble(), (first.x - vertex.x).toDouble())).toFloat()
    val a2 = Math.toDegrees(atan2((second.y - vertex.y).toDouble(), (second.x - vertex.x).toDouble())).toFloat()
    var sweep = (a2 - a1 + 360f) % 360f
    if (sweep > 180f) sweep -= 360f
    val topLeft = vertex - Offset(radius, radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    drawArc(color, a1, sweep, true, topLeft = topLeft, size = arcSize)
    drawArc(color.copy(alpha = 1f), a1, sweep, false, topLeft = topLeft, size = arcSize, style = Stroke(2.dp.toPx()))
}

private fun DrawScope.drawTransferArrow(start: Offset, end: Offset, color: Color, dash: PathEffect) {
    drawLine(color, start, end, 2.2.dp.toPx(), pathEffect = dash)
    val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
    val wing = 12.dp.toPx()
    val p = Path().apply {
        moveTo(end.x, end.y)
        lineTo(end.x - (cos(angle - PI / 6) * wing).toFloat(), end.y - (sin(angle - PI / 6) * wing).toFloat())
        lineTo(end.x - (cos(angle + PI / 6) * wing).toFloat(), end.y - (sin(angle + PI / 6) * wing).toFloat())
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.drawLabel(text: String, at: Offset, textSizeSp: Float, color: Color, align: Paint.Align = Paint.Align.LEFT) {
    drawContext.canvas.nativeCanvas.drawText(
        text,
        at.x,
        at.y,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = textSizeSp * density
            textAlign = align
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        },
    )
}

private fun pointOnCircle(center: Offset, radius: Float, degrees: Float): Offset {
    val radians = degrees / 180f * PI.toFloat()
    return Offset(center.x + cos(radians) * radius, center.y + sin(radians) * radius)
}
