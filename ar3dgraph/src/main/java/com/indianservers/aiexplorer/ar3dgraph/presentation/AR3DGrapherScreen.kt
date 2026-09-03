package com.indianservers.aiexplorer.ar3dgraph.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCameraPermissionManager
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCapabilityChecker
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import com.indianservers.aiexplorer.ar3dgraph.integration.DisconnectedGraphEngineContract
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineContract
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val ArBackground = Color(0xFF03050B)
private val ArPanel = Color(0xFF080C17)
private val ArPanelRaised = Color(0xFF0C1120)
private val ArBorder = Color(0xFF20283B)
private val ArText = Color(0xFFF5F4FF)
private val ArMuted = Color(0xFF9DA6B9)
private val ArPurple = Color(0xFF8B5CFF)
private val ArViolet = Color(0xFFB66DFF)
private val ArBlue = Color(0xFF4A63F4)
private val ArMint = Color(0xFF43E7A2)

private val GraphThemes = listOf(
    listOf(Color(0xFFFFCB50), Color(0xFF35DFBA), Color(0xFF4A74FF), Color(0xFFB14CFF)),
    listOf(Color(0xFF5EF0A5), Color(0xFF087D65)),
    listOf(Color(0xFFFF9C43), Color(0xFF5E2513)),
    listOf(Color(0xFF42AAFF), Color(0xFF071D48)),
    listOf(Color(0xFFE9E9F1), Color(0xFF33343B)),
)

private val GraphExamples = listOf(
    "z = x^2 + y^2",
    "z = sin(x) * cos(y)",
    "z = sin(sqrt(x^2 + y^2))",
    "z = x^2 - y^2",
)

@Composable
fun AR3DGraphScreen(
    onBack: () -> Unit,
    graphEngine: GraphEngineContract = DisconnectedGraphEngineContract,
    model: AR3DGraphViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val checker = remember(context) { ARCapabilityChecker(context) }
    val ui = model.uiState
    var showHelp by remember { mutableStateOf(false) }
    var showAxes by remember { mutableStateOf(true) }
    var showGrid by remember { mutableStateOf(true) }
    var autoRotate by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(false) }
    var themeIndex by remember { mutableIntStateOf(0) }
    var exampleIndex by remember { mutableIntStateOf(0) }
    var showScanHint by remember { mutableStateOf(true) }

    BackHandler(enabled = showHelp) {
        showHelp = false
    }

    LaunchedEffect(graphEngine) {
        model.connect(graphEngine)
        if (model.uiState.renderData == null) model.plot()
    }

    fun refreshPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
        model.onCameraPermission(
            ARCameraPermissionManager.classify(
                granted = granted,
                shouldShowRationale = rationale,
                hasRequested = model.uiState.hasRequestedCamera,
            ),
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermission() }

    fun checkCapability() {
        model.beginCapabilityCheck()
        checker.check(model::onCapabilityResult)
    }

    LaunchedEffect(checker) {
        refreshPermission()
        checkCapability()
    }

    // Permission dialogs and the system App Info screen pause this Activity. Some OEMs deliver
    // the result before Compose's launcher callback is active again, so reconcile the real
    // package permission whenever the screen resumes.
    DisposableEffect(lifecycleOwner, checker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermission()
                if (model.uiState.capability != ARCapabilityState.Supported) checkCapability()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(checker) {
        onDispose {
            checker.cancel()
            model.onScreenExit()
        }
    }

    LaunchedEffect(autoRotate) {
        while (autoRotate) {
            delay(32)
            val current = model.uiState.userTransform
            model.onUserTransformChanged(current.copy(yawDegrees = (current.yawDegrees + .45f) % 360f))
        }
    }

    val permissionAction: @Composable () -> Unit = {
        when {
            ui.capability == ARCapabilityState.ARCoreNotInstalled || ui.capability == ARCapabilityState.ARCoreUpdateRequired -> {
                GraphActionButton(if (ui.capability == ARCapabilityState.ARCoreUpdateRequired) "Update ARCore" else "Install ARCore", primary = true) {
                    model.onInstallationRequested()
                    checkCapability()
                }
            }
            ui.capability == ARCapabilityState.Supported && ui.cameraPermission in setOf(ARCameraPermissionState.PermissionRequired, ARCameraPermissionState.PermissionDenied) -> {
                GraphActionButton("Allow Camera", primary = true) {
                    model.markCameraRequested()
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
            ui.cameraPermission == ARCameraPermissionState.PermissionPermanentlyDenied -> {
                GraphActionButton("Open Settings", primary = true) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }
            }
            ui.capability == ARCapabilityState.Error || ui.session == ARSessionState.SessionError -> {
                GraphActionButton("Retry AR", primary = true) {
                    model.prepareSessionRetry()
                    checkCapability()
                }
            }
        }
    }

    if (fullScreen) {
        Box(Modifier.fillMaxSize().background(ArBackground).safeDrawingPadding()) {
            ARGraphViewport(
                ui = ui,
                showAxes = showAxes,
                showGrid = showGrid,
                autoRotate = autoRotate,
                theme = GraphThemes[themeIndex],
                onAxes = { showAxes = !showAxes },
                onGrid = { showGrid = !showGrid },
                onRotate = { autoRotate = !autoRotate },
                onReset = model::resetView,
                onFullScreen = { fullScreen = false },
                modifier = Modifier.fillMaxSize(),
                model = model,
                showScanHint = showScanHint,
                onDismissScanHint = { showScanHint = false },
            )
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(ArBackground)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CompactARHeader(onMenu = onBack, active = ui.session == ARSessionState.SessionRunning, onCamera = { fullScreen = true })

            CompactEquationPanel(
                ui = ui,
                onEquation = model::onEquationsChanged,
                onPlot = model::plot,
                onExample = {
                    exampleIndex = (exampleIndex + 1) % GraphExamples.size
                    model.onEquationsChanged(GraphExamples[exampleIndex])
                },
            )

            ARPreviewPanel(
                ui = ui,
                active = ui.session == ARSessionState.SessionRunning,
                showAxes = showAxes,
                showGrid = showGrid,
                autoRotate = autoRotate,
                theme = GraphThemes[themeIndex],
                onARView = { fullScreen = true },
                model = model,
                showScanHint = showScanHint,
                onDismissScanHint = { showScanHint = false },
            )

            QuickSettingsPanel(ui, model)

            if (showMore) {
                MoreOptionsPanel(
                    ui = ui,
                    themeIndex = themeIndex,
                    showAxes = showAxes,
                    showGrid = showGrid,
                    autoRotate = autoRotate,
                    graphMessage = ui.graphMessage,
                    permissionAction = permissionAction,
                    onAxes = { showAxes = !showAxes },
                    onGrid = { showGrid = !showGrid },
                    onRotate = { autoRotate = !autoRotate },
                    onHelp = { showHelp = true },
                    onTheme = { themeIndex = it },
                    onRange = { minimum, maximum ->
                        model.onDomainMinimumChanged(minimum)
                        model.onDomainMaximumChanged(maximum)
                    },
                    onResetPlacement = model::resetPlacement,
                    onClearGraph = model::clearGraph,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GraphActionButton("Reset", icon = GraphControlIcon.Reset, modifier = Modifier.weight(1f)) { model.resetView() }
                GraphActionButton(
                    "Plot",
                    icon = GraphControlIcon.Graph,
                    primary = true,
                    modifier = Modifier.weight(1.5f).testTag("ar3dgraph-plot"),
                    enabled = ui.placement != ARGraphPlacementState.GeneratingGraph,
                ) { model.plot() }
                GraphActionButton(if (showMore) "Less" else "More", icon = GraphControlIcon.More, modifier = Modifier.weight(1f)) { showMore = !showMore }
            }

            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                StatusText("AR", if (ui.capability == ARCapabilityState.Supported) "Supported" else ui.capability.name)
                StatusText("Graph", if (ui.renderData != null) "Ready" else ui.placement.name)
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = ArPanelRaised,
            titleContentColor = ArText,
            textContentColor = ArMuted,
            title = { Text("AR 3D Grapher") },
            text = { Text("Plot a surface, scan a floor, table or wall, and tap to anchor it. Drag to rotate, pinch to scale, and use Reset View to restore the camera-facing graph.") },
            confirmButton = {
                Button(
                    onClick = { showHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ArPurple, contentColor = ArText),
                    modifier = Modifier.heightIn(min = 48.dp).testTag("ar3dgraph-help-close"),
                ) { Text("Got it") }
            },
            dismissButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Close", color = ArMuted)
                }
            },
            modifier = Modifier.testTag("ar3dgraph-help-dialog"),
        )
    }
}

@Composable
private fun SurfaceScanHint(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth(.86f)
            .background(Color(0xE50C1120), RoundedCornerShape(12.dp))
            .border(1.dp, ArMint.copy(.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).background(ArMint, CircleShape))
        Text(
            "Scan a textured floor, table or wall. Tap when the plane grid appears.",
            color = ArText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "Close",
            color = ArMint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .semantics { contentDescription = "Close AR scan hint" }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun CompactARHeader(onMenu: () -> Unit, active: Boolean, onCamera: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).clickable(onClick = onMenu).semantics { contentDescription = "Open navigation" },
            contentAlignment = Alignment.Center,
        ) {
            GraphControlIconView(GraphControlIcon.Menu, ArText, Modifier.size(28.dp))
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("AR Math", color = ArText, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text("3D Grapher", color = ArViolet, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
        GraphIconButton(GraphControlIcon.Camera, active, 52.dp, onCamera)
    }
}

@Composable
private fun CompactEquationPanel(ui: AR3DGraphUiState, onEquation: (String) -> Unit, onPlot: () -> Unit, onExample: () -> Unit) {
    Column(panelModifier().padding(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Surface Equation", color = ArText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            GraphControlIconView(GraphControlIcon.Help, ArPurple, Modifier.size(20.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkInput(
                value = ui.equations,
                onValueChange = onEquation,
                modifier = Modifier.weight(1f).height(64.dp).testTag("ar3dgraph-equation-input"),
                imeAction = ImeAction.Done,
                onDone = onPlot,
            )
            GraphActionButton(
                label = "PLOT",
                icon = GraphControlIcon.Graph,
                primary = true,
                modifier = Modifier.width(112.dp).height(64.dp).testTag("ar3dgraph-plot"),
                enabled = ui.placement != ARGraphPlacementState.GeneratingGraph,
                onClick = onPlot,
            )
        }
        Text("Examples  v", color = ArViolet, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onExample).padding(vertical = 5.dp))
    }
}

@Composable
private fun ARPreviewPanel(
    ui: AR3DGraphUiState,
    active: Boolean,
    showAxes: Boolean,
    showGrid: Boolean,
    autoRotate: Boolean,
    theme: List<Color>,
    onARView: () -> Unit,
    model: AR3DGraphViewModel,
    showScanHint: Boolean,
    onDismissScanHint: () -> Unit,
) {
    val liveArReady = ui.capability == ARCapabilityState.Supported && ui.cameraPermission == ARCameraPermissionState.Granted
    Column(panelModifier().padding(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphControlIconView(GraphControlIcon.AR, ArText, Modifier.size(21.dp))
                Text("AR Preview", color = ArText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).background(ArMint, CircleShape))
                Text(if (active) "AR Active" else "AR Ready", color = ArMint, fontSize = 12.sp)
            }
        }
        val previewShape = RoundedCornerShape(16.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(.82f)
                .background(Color(0xFF111018), previewShape)
                .border(1.dp, ArBorder, previewShape)
                .testTag("ar3dgraph-camera-container")
                .semantics { contentDescription = "AR camera and graph placement viewport" },
        ) {
            if (liveArReady) {
                SceneViewARGraphViewport(
                    ui = ui,
                    onSessionRunning = model::onSceneViewSessionRunning,
                    onSessionPaused = model::onSceneViewSessionPaused,
                    onTrackingChanged = model::onTrackingChanged,
                    onPlacementStarted = model::onPlacementStarted,
                    onPlacementPlaced = { model.onPlacementResult(com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult.Placed) },
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF27231F), Color(0xFF101520), Color(0xFF080B12)))))
            }
            if (!liveArReady && ui.renderData != null && ui.placement != ARGraphPlacementState.Placed) {
                GraphSurfacePreview(showAxes, showGrid, autoRotate, theme, ui.userTransform.yawDegrees, Modifier.fillMaxSize().padding(22.dp))
            }
            if (showScanHint && liveArReady && ui.renderData != null && ui.placement != ARGraphPlacementState.Placed) {
                SurfaceScanHint(
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 12.dp),
                    onDismiss = onDismissScanHint,
                )
            }
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color(0xE50D1423), RoundedCornerShape(12.dp))
                    .border(1.dp, ArBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onARView)
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                GraphControlIconView(GraphControlIcon.AR, ArText, Modifier.size(20.dp))
                Text("AR View", color = ArText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QuickSettingsPanel(ui: AR3DGraphUiState, model: AR3DGraphViewModel) {
    Column(panelModifier().padding(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick Settings", color = ArText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactRangeControl("X Range", ui.domainMinimum, ui.domainMaximum, Modifier.weight(1f), model::onDomainMinimumChanged, model::onDomainMaximumChanged)
            Box(Modifier.width(1.dp).height(128.dp).background(ArBorder))
            CompactRangeControl("Y Range", ui.domainMinimum, ui.domainMaximum, Modifier.weight(1f), model::onDomainMinimumChanged, model::onDomainMaximumChanged)
            Box(Modifier.width(1.dp).height(128.dp).background(ArBorder))
            CompactResolutionControl(ui.density, Modifier.weight(1f), model::onDensityChanged)
        }
    }
}

@Composable
private fun CompactRangeControl(title: String, minimum: String, maximum: String, modifier: Modifier, onMinimum: (String) -> Unit, onMaximum: (String) -> Unit) {
    val min = minimum.toFloatOrNull() ?: -3f
    val max = maximum.toFloatOrNull() ?: 3f
    val safeMin = min.coerceIn(-10f, 9f)
    val safeMax = max.coerceIn(safeMin + 1f, 10f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = ArMuted, fontSize = 11.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            LabeledMiniInput("Min", minimum, Modifier.weight(1f), onMinimum)
            LabeledMiniInput("Max", maximum, Modifier.weight(1f), onMaximum)
        }
        RangeSlider(
            value = safeMin..safeMax,
            onValueChange = { range ->
                onMinimum(range.start.toInt().toString())
                onMaximum(range.endInclusive.toInt().toString())
            },
            valueRange = -10f..10f,
            colors = SliderDefaults.colors(thumbColor = ArPurple, activeTrackColor = ArViolet, inactiveTrackColor = ArBorder),
        )
    }
}

@Composable
private fun CompactResolutionControl(value: String, modifier: Modifier, onChange: (String) -> Unit) {
    val resolution = value.toFloatOrNull()?.coerceIn(8f, 56f) ?: 26f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Resolution", color = ArMuted, fontSize = 11.sp)
        DarkInput(value, onChange, Modifier.fillMaxWidth().height(48.dp))
        Slider(resolution, { onChange(it.toInt().toString()) }, valueRange = 8f..56f, colors = SliderDefaults.colors(thumbColor = ArPurple, activeTrackColor = ArViolet, inactiveTrackColor = ArBorder))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("10", color = ArMuted, fontSize = 8.sp)
            Text("100", color = ArMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun MoreOptionsPanel(
    ui: AR3DGraphUiState,
    themeIndex: Int,
    showAxes: Boolean,
    showGrid: Boolean,
    autoRotate: Boolean,
    graphMessage: String,
    permissionAction: @Composable () -> Unit,
    onAxes: () -> Unit,
    onGrid: () -> Unit,
    onRotate: () -> Unit,
    onHelp: () -> Unit,
    onTheme: (Int) -> Unit,
    onRange: (String, String) -> Unit,
    onResetPlacement: () -> Unit,
    onClearGraph: () -> Unit,
) {
    Column(panelModifier(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GraphControlIconView(GraphControlIcon.Settings, ArViolet, Modifier.size(20.dp))
            Text("More Options", color = ArText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Display", color = ArMuted, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ViewportTool("Axes", GraphControlIcon.Axes, showAxes, onAxes)
                ViewportTool("Grid", GraphControlIcon.Grid, showGrid, onGrid)
                ViewportTool("Rotate", GraphControlIcon.Rotate, autoRotate, onRotate)
                ViewportTool("Help", GraphControlIcon.Help, false, onHelp)
            }
        }

        PresetAndThemeControls(
            ui = ui,
            themeIndex = themeIndex,
            onTheme = onTheme,
            onRange = onRange,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GraphActionButton("Reset Placement", modifier = Modifier.weight(1f), enabled = ui.renderData != null, onClick = onResetPlacement)
            GraphActionButton("Clear Graph", modifier = Modifier.weight(1f), enabled = ui.renderData != null, onClick = onClearGraph)
        }
        if (graphMessage.isNotBlank()) {
            Text(graphMessage, color = ArMuted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
        }
        permissionAction()
    }
}

@Composable
private fun ARGraphHeader(onBack: () -> Unit, active: Boolean, onHelp: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(78.dp)) {
        Row(
            Modifier.align(Alignment.CenterStart).clickable(onClick = onBack).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text("<", color = ArPurple, fontSize = 26.sp)
            Text("Back", color = ArPurple, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("AR Math", color = ArText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("3D Grapher", color = ArViolet, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GraphIconButton(GraphControlIcon.Camera, active, 46.dp) {}
                Text(if (active) "AR Active" else "AR Ready", color = if (active) ArMint else ArMuted, fontSize = 9.sp)
            }
            GraphIconButton(GraphControlIcon.Help, false, 46.dp, onHelp)
        }
    }
}

@Composable
private fun ARGraphViewport(
    ui: AR3DGraphUiState,
    showAxes: Boolean,
    showGrid: Boolean,
    autoRotate: Boolean,
    theme: List<Color>,
    onAxes: () -> Unit,
    onGrid: () -> Unit,
    onRotate: () -> Unit,
    onReset: () -> Unit,
    onFullScreen: () -> Unit,
    modifier: Modifier,
    model: AR3DGraphViewModel,
    showScanHint: Boolean,
    onDismissScanHint: () -> Unit,
) {
    val liveArReady = ui.capability == ARCapabilityState.Supported && ui.cameraPermission == ARCameraPermissionState.Granted
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier.background(Color(0xFF111018), shape).border(1.dp, ArBorder, shape).testTag("ar3dgraph-camera-container").semantics {
            contentDescription = "AR camera and graph placement viewport"
        },
    ) {
        if (liveArReady) {
            SceneViewARGraphViewport(
                ui = ui,
                onSessionRunning = model::onSceneViewSessionRunning,
                onSessionPaused = model::onSceneViewSessionPaused,
                onTrackingChanged = model::onTrackingChanged,
                onPlacementStarted = model::onPlacementStarted,
                onPlacementPlaced = { model.onPlacementResult(com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult.Placed) },
            )
        } else {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF25221F), Color(0xFF0D1016)))))
        }

        if (!liveArReady && ui.renderData != null && ui.placement != ARGraphPlacementState.Placed) {
            GraphSurfacePreview(showAxes, showGrid, autoRotate, theme, ui.userTransform.yawDegrees, Modifier.fillMaxSize().padding(28.dp))
        }
        if (showScanHint && liveArReady && ui.renderData != null && ui.placement != ARGraphPlacementState.Placed) {
            SurfaceScanHint(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp),
                onDismiss = onDismissScanHint,
            )
        }

        Row(
            Modifier.align(Alignment.TopStart).padding(18.dp).background(Color(0xC9111420), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.18f), RoundedCornerShape(18.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GraphControlIconView(GraphControlIcon.AR, Color.White, Modifier.size(18.dp))
            Text("AR", color = ArText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.size(7.dp).background(ArMint, CircleShape))
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).background(Color(0xD8101420), RoundedCornerShape(15.dp)).border(1.dp, Color.White.copy(.08f), RoundedCornerShape(15.dp)),
        ) {
            ViewportTool("Axes", GraphControlIcon.Axes, showAxes, onAxes)
            ViewportTool("Grid", GraphControlIcon.Grid, showGrid, onGrid)
            ViewportTool("Rotate", GraphControlIcon.Rotate, autoRotate, onRotate)
            ViewportTool("Reset", GraphControlIcon.Reset, false, onReset)
        }

        Column(
            Modifier.align(Alignment.BottomStart).padding(18.dp).background(Color(0xC9111420), RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("Equation", color = ArMuted, fontSize = 10.sp)
            Text(ui.equations.lineSequence().firstOrNull() ?: "z = x^2 + y^2", color = ArMint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier.align(Alignment.BottomEnd).padding(18.dp).background(Color(0xC9111420), RoundedCornerShape(18.dp)).border(1.dp, Color.White.copy(.14f), RoundedCornerShape(18.dp)).clickable(onClick = onFullScreen).padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            GraphControlIconView(GraphControlIcon.FullScreen, ArText, Modifier.size(17.dp))
            Text("Full Screen", color = ArText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GraphSurfacePreview(showAxes: Boolean, showGrid: Boolean, rotating: Boolean, theme: List<Color>, yaw: Float, modifier: Modifier) {
    Canvas(modifier) {
        val origin = Offset(size.width * .48f, size.height * .72f)
        val width = size.width * .62f
        val height = size.height * .55f
        if (showGrid) {
            repeat(13) { index ->
                val p = index / 12f
                drawLine(Color.White.copy(.16f), Offset(origin.x - width * .68f + p * width * 1.35f, origin.y + height * .22f), Offset(origin.x + (p - .5f) * width * .25f, origin.y - height * .05f), 1f)
                drawLine(Color.White.copy(.13f), Offset(origin.x - width * .72f, origin.y + p * height * .26f), Offset(origin.x + width * .72f, origin.y + p * height * .26f), 1f)
            }
        }
        val gradient = Brush.verticalGradient(theme, startY = origin.y - height, endY = origin.y)
        repeat(17) { row ->
            val v = row / 16f
            val yBase = origin.y - height * v
            val half = width * (.08f + .48f * v)
            val curve = Path()
            repeat(41) { column ->
                val u = column / 40f * 2f - 1f
                val x = origin.x + u * half
                val y = yBase + (1f - u * u) * height * .34f * v
                if (column == 0) curve.moveTo(x, y) else curve.lineTo(x, y)
            }
            drawPath(curve, gradient, style = Stroke(if (row % 4 == 0) 2f else 1f, cap = StrokeCap.Round))
        }
        repeat(17) { column ->
            val u = column / 16f * 2f - 1f
            val path = Path()
            repeat(31) { row ->
                val v = row / 30f
                val yBase = origin.y - height * v
                val half = width * (.08f + .48f * v)
                val x = origin.x + u * half
                val y = yBase + (1f - u * u) * height * .34f * v
                if (row == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, theme.last().copy(alpha = .75f), style = Stroke(1f))
        }
        drawCircle(theme.first(), 7f, origin)
        if (showAxes) {
            drawLine(Color.White, origin, Offset(origin.x, origin.y - height * .96f), 2f, StrokeCap.Round)
            drawLine(Color.White, origin, Offset(origin.x - width * .72f, origin.y + height * .18f), 2f, StrokeCap.Round)
            drawLine(Color.White, origin, Offset(origin.x + width * .70f, origin.y + height * .16f), 2f, StrokeCap.Round)
        }
    }
}

@Composable
private fun EquationPanel(ui: AR3DGraphUiState, onEquation: (String) -> Unit, onPlot: () -> Unit, onExample: () -> Unit) {
    Column(panelModifier(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Surface equation(s)", color = ArText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                GraphControlIconView(GraphControlIcon.Help, ArPurple, Modifier.size(18.dp))
            }
            Text("Examples  v", color = ArViolet, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onExample).padding(5.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DarkInput(
                value = ui.equations,
                onValueChange = onEquation,
                modifier = Modifier.weight(1f).testTag("ar3dgraph-equation-input"),
                imeAction = ImeAction.Done,
                onDone = onPlot,
            )
            GraphActionButton("Plot", icon = GraphControlIcon.Graph, primary = true, modifier = Modifier.width(134.dp).height(58.dp), enabled = ui.placement != ARGraphPlacementState.GeneratingGraph, onClick = onPlot)
        }
    }
}

@Composable
private fun RangeAndResolutionControls(ui: AR3DGraphUiState, model: AR3DGraphViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RangeCard("X Range", ui.domainMinimum, ui.domainMaximum, Modifier.weight(1f), model::onDomainMinimumChanged, model::onDomainMaximumChanged)
        RangeCard("Y Range", ui.domainMinimum, ui.domainMaximum, Modifier.weight(1f), model::onDomainMinimumChanged, model::onDomainMaximumChanged)
        ResolutionCard(ui.density, Modifier.weight(.95f), model::onDensityChanged)
    }
}

@Composable
private fun RangeCard(title: String, minimum: String, maximum: String, modifier: Modifier, onMinimum: (String) -> Unit, onMaximum: (String) -> Unit) {
    val min = minimum.toFloatOrNull() ?: -3f
    val max = maximum.toFloatOrNull() ?: 3f
    val safeMin = min.coerceIn(-10f, 9f)
    val safeMax = max.coerceIn(safeMin + 1f, 10f)
    Column(modifier.then(panelModifier()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = ArText, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            LabeledMiniInput("Min", minimum, Modifier.weight(1f), onMinimum)
            LabeledMiniInput("Max", maximum, Modifier.weight(1f), onMaximum)
        }
        RangeSlider(
            value = safeMin..safeMax,
            onValueChange = { range -> onMinimum(range.start.toInt().toString()); onMaximum(range.endInclusive.toInt().toString()) },
            valueRange = -10f..10f,
            colors = SliderDefaults.colors(thumbColor = ArPurple, activeTrackColor = ArViolet, inactiveTrackColor = ArBorder),
        )
    }
}

@Composable
private fun ResolutionCard(value: String, modifier: Modifier, onChange: (String) -> Unit) {
    val resolution = value.toFloatOrNull()?.coerceIn(8f, 56f) ?: 26f
    Column(modifier.then(panelModifier()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Resolution", color = ArText, fontSize = 12.sp)
        DarkInput(value, onChange, Modifier.fillMaxWidth().height(48.dp))
        Slider(resolution, { onChange(it.toInt().toString()) }, valueRange = 8f..56f, colors = SliderDefaults.colors(thumbColor = ArPurple, activeTrackColor = ArViolet, inactiveTrackColor = ArBorder))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("10", color = ArMuted, fontSize = 8.sp)
            Text("100", color = ArMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun PresetAndThemeControls(ui: AR3DGraphUiState, themeIndex: Int, onTheme: (Int) -> Unit, onRange: (String, String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Preset Range", color = ArMuted, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Small" to ("-2" to "2"), "Medium" to ("-3" to "3"), "Large" to ("-5" to "5"), "XL" to ("-10" to "10")).forEach { (label, range) ->
                    val selected = ui.domainMinimum == range.first && ui.domainMaximum == range.second
                    Column(
                        Modifier.background(if (selected) ArPurple.copy(.22f) else ArPanelRaised, RoundedCornerShape(8.dp)).border(1.dp, if (selected) ArViolet.copy(.55f) else ArBorder, RoundedCornerShape(8.dp)).clickable { onRange(range.first, range.second) }.padding(horizontal = 13.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(label, color = ArText, fontSize = 10.sp)
                        Text("[${range.first}, ${range.second}]", color = ArMuted, fontSize = 8.sp)
                    }
                }
            }
        }
        Column(Modifier.weight(.9f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color Theme", color = ArMuted, fontSize = 11.sp)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                GraphThemes.forEachIndexed { index, colors ->
                    Box(
                        Modifier.size(38.dp).background(Brush.radialGradient(colors), RoundedCornerShape(8.dp)).border(1.dp, if (index == themeIndex) Color.White else ArBorder, RoundedCornerShape(8.dp)).clickable { onTheme(index) },
                        contentAlignment = Alignment.BottomEnd,
                    ) {
                        if (index == themeIndex) Text("v", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(3.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreOptionsRow(expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).background(ArPanelRaised, RoundedCornerShape(14.dp)).border(1.dp, ArBorder, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            GraphControlIconView(GraphControlIcon.Settings, ArMuted, Modifier.size(22.dp))
            Text("More Options", color = ArText, fontSize = 13.sp)
        }
        Text(if (expanded) "^" else "v", color = ArMuted, fontSize = 16.sp)
    }
}

@Composable
private fun ViewportTool(label: String, icon: GraphControlIcon, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.width(64.dp).clickable(onClick = onClick).background(if (selected) ArPurple.copy(.18f) else Color.Transparent).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        GraphControlIconView(icon, if (selected) ArViolet else ArText, Modifier.size(25.dp))
        Text(label, color = if (selected) ArViolet else ArText, fontSize = 9.sp)
    }
}

@Composable
private fun GraphActionButton(label: String, icon: GraphControlIcon? = null, primary: Boolean = false, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) ArPurple else ArPanelRaised,
            contentColor = ArText,
            disabledContainerColor = ArPanelRaised.copy(.5f),
            disabledContentColor = ArMuted.copy(.45f),
        ),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, ArBorder),
    ) {
        if (icon != null) {
            GraphControlIconView(icon, if (enabled) ArText else ArMuted, Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(label, fontSize = 12.sp, fontWeight = if (primary) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun GraphIconButton(icon: GraphControlIcon, active: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).background(ArPanelRaised, RoundedCornerShape(14.dp)).border(1.dp, if (active) ArMint.copy(.5f) else ArBorder, RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GraphControlIconView(icon, if (active) ArMint else ArText, Modifier.size(24.dp))
        if (active) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(6.dp).background(ArMint, CircleShape))
    }
}

private enum class GraphControlIcon { AR, Camera, Help, Axes, Grid, Rotate, Reset, FullScreen, Graph, Settings, Menu, More }

@Composable
private fun GraphControlIconView(icon: GraphControlIcon, color: Color, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = icon.name }) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, Offset(w * x1, h * y1), Offset(w * x2, h * y2), stroke, StrokeCap.Round)
        when (icon) {
            GraphControlIcon.AR -> { drawCircle(color, w * .33f, style = Stroke(stroke)); line(.5f, .05f, .5f, .22f); line(.5f, .78f, .5f, .95f); line(.05f, .5f, .22f, .5f); line(.78f, .5f, .95f, .5f) }
            GraphControlIcon.Camera -> { drawRoundRect(color, Offset(w * .12f, h * .25f), androidx.compose.ui.geometry.Size(w * .76f, h * .58f), androidx.compose.ui.geometry.CornerRadius(4f), style = Stroke(stroke)); drawCircle(color, w * .16f, style = Stroke(stroke)); line(.28f, .25f, .36f, .13f); line(.36f, .13f, .62f, .13f); line(.62f, .13f, .70f, .25f) }
            GraphControlIcon.Help -> { drawCircle(color, w * .43f, style = Stroke(stroke)); drawContext.canvas.nativeCanvas.drawText("?", w * .37f, h * .69f, android.graphics.Paint().apply { this.color = android.graphics.Color.WHITE; textSize = h * .55f; isAntiAlias = true }) }
            GraphControlIcon.Axes -> { line(.5f, .5f, .5f, .08f); line(.5f, .5f, .12f, .82f); line(.5f, .5f, .88f, .82f); drawCircle(color, 2.5f, Offset(w * .5f, h * .08f)); drawCircle(color, 2.5f, Offset(w * .12f, h * .82f)); drawCircle(color, 2.5f, Offset(w * .88f, h * .82f)) }
            GraphControlIcon.Grid -> { repeat(4) { i -> line(.15f + i * .23f, .12f, .15f + i * .23f, .88f); line(.12f, .15f + i * .23f, .88f, .15f + i * .23f) } }
            GraphControlIcon.Rotate, GraphControlIcon.Reset -> { drawArc(color, 35f, 285f, false, Offset(w * .13f, h * .13f), androidx.compose.ui.geometry.Size(w * .74f, h * .74f), style = Stroke(stroke, cap = StrokeCap.Round)); line(.68f, .08f, .87f, .14f); line(.87f, .14f, .81f, .33f) }
            GraphControlIcon.FullScreen -> { line(.12f, .36f, .12f, .12f); line(.12f, .12f, .36f, .12f); line(.64f, .12f, .88f, .12f); line(.88f, .12f, .88f, .36f); line(.12f, .64f, .12f, .88f); line(.12f, .88f, .36f, .88f); line(.64f, .88f, .88f, .88f); line(.88f, .64f, .88f, .88f) }
            GraphControlIcon.Graph -> { line(.15f, .82f, .85f, .82f); line(.2f, .82f, .2f, .15f); val path = Path().apply { moveTo(w * .22f, h * .7f); cubicTo(w * .4f, h * .25f, w * .62f, h * .25f, w * .82f, h * .7f) }; drawPath(path, color, style = Stroke(stroke)) }
            GraphControlIcon.Settings -> { drawCircle(color, w * .22f, style = Stroke(stroke)); repeat(8) { i -> val a = i * PI / 4; line(.5f + cos(a).toFloat() * .3f, .5f + sin(a).toFloat() * .3f, .5f + cos(a).toFloat() * .45f, .5f + sin(a).toFloat() * .45f) } }
            GraphControlIcon.Menu -> { line(.16f, .25f, .84f, .25f); line(.16f, .5f, .84f, .5f); line(.16f, .75f, .84f, .75f) }
            GraphControlIcon.More -> { drawCircle(color, w * .07f, Offset(w * .5f, h * .22f)); drawCircle(color, w * .07f, Offset(w * .5f, h * .5f)); drawCircle(color, w * .07f, Offset(w * .5f, h * .78f)) }
        }
    }
}

@Composable
private fun DarkInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier, imeAction: ImeAction = ImeAction.Default, onDone: () -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = ArText, fontSize = 13.sp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ArPurple, unfocusedBorderColor = ArBorder, cursorColor = ArViolet, focusedContainerColor = ArBackground, unfocusedContainerColor = ArBackground),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Composable
private fun LabeledMiniInput(label: String, value: String, modifier: Modifier, onValueChange: (String) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = ArMuted, fontSize = 8.sp)
        DarkInput(value, onValueChange, Modifier.fillMaxWidth().height(48.dp))
    }
}

@Composable
private fun StatusText(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(7.dp).background(ArMint, CircleShape))
        Text("$label: $value", color = ArMuted, fontSize = 10.sp)
    }
}

private fun panelModifier() = Modifier.fillMaxWidth().background(ArPanel, RoundedCornerShape(16.dp)).border(1.dp, ArBorder, RoundedCornerShape(16.dp)).padding(12.dp)
