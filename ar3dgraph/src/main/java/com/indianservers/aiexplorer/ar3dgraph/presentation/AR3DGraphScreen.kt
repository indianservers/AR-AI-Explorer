package com.indianservers.aiexplorer.ar3dgraph.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.google.ar.core.Config
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCameraPermissionManager
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCapabilityChecker
import com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState
import com.indianservers.aiexplorer.ar3dgraph.integration.DisconnectedGraphEngineContract
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineContract
import io.github.sceneview.ar.PlacementScene
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AR3DGraphScreen(
    onBack: () -> Unit,
    graphEngine: GraphEngineContract = DisconnectedGraphEngineContract,
    model: AR3DGraphViewModel = viewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val checker = remember(context) { ARCapabilityChecker(context) }
    var showHelp by remember { mutableStateOf(false) }
    var controlsCollapsed by remember { mutableStateOf(false) }
    val ui = model.uiState
    val configuration = LocalConfiguration.current
    val controlsMaxHeight = if (configuration.screenWidthDp > configuration.screenHeightDp) 196.dp else 340.dp

    LaunchedEffect(graphEngine) {
        model.connect(graphEngine)
    }

    fun refreshPermission() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val rationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA) } ?: false
        model.onCameraPermission(ARCameraPermissionManager.classify(granted, rationale, ui.hasRequestedCamera))
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshPermission()
    }

    fun checkCapability() {
        model.beginCapabilityCheck()
        checker.check(model::onCapabilityResult)
    }

    LaunchedEffect(checker) {
        refreshPermission()
        checkCapability()
    }

    DisposableEffect(checker) {
        onDispose {
            checker.cancel()
            model.onScreenExit()
        }
    }

    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("ar3dgraph-screen"),
        topBar = {
            TopAppBar(
                title = { Text(AR3DGraphRoute.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back from AR 3D Graph" }) {
                        Text("← Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { controlsCollapsed = !controlsCollapsed },
                        modifier = Modifier.semantics {
                            contentDescription = if (controlsCollapsed) "Show AR 3D Graph controls" else "Collapse AR 3D Graph controls"
                        },
                    ) {
                        Text(if (controlsCollapsed) "Controls" else "Collapse")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF07111E), RoundedCornerShape(18.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .5f), RoundedCornerShape(18.dp))
                    .semantics { contentDescription = "AR camera and graph placement viewport" }
                    .testTag("ar3dgraph-camera-container"),
            ) {
                if (ui.capability == ARCapabilityState.Supported && ui.cameraPermission == ARCameraPermissionState.Granted) {
                    SceneViewARGraphViewport(
                        ui = ui,
                        onSessionRunning = model::onSceneViewSessionRunning,
                        onSessionPaused = model::onSceneViewSessionPaused,
                        onTrackingChanged = model::onTrackingChanged,
                        onPlacementStarted = model::onPlacementStarted,
                        onPlacementPlaced = { model.onPlacementResult(com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult.Placed) },
                    )
                } else {
                    Column(
                        Modifier.align(Alignment.Center).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(statusTitle(ui), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(ui.message, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    ui.graphMessage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color(0xCC07111E))
                        .padding(10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("ar3dgraph-placement-message"),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (controlsCollapsed) {
                OutlinedButton(
                    onClick = { controlsCollapsed = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("ar3dgraph-show-controls"),
                ) { Text("Controls") }
            } else {
            Column(
                Modifier.fillMaxWidth().heightIn(max = controlsMaxHeight).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            OutlinedTextField(
                value = ui.equations,
                onValueChange = model::onEquationsChanged,
                label = { Text("Surface equation(s), one per line") },
                minLines = 1,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().heightIn(max = 116.dp).testTag("ar3dgraph-equation-input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { model.plot() }),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = ui.domainMinimum,
                    onValueChange = model::onDomainMinimumChanged,
                    label = { Text("Min") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = ui.domainMaximum,
                    onValueChange = model::onDomainMaximumChanged,
                    label = { Text("Max") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = ui.density,
                    onValueChange = model::onDensityChanged,
                    label = { Text("Resolution") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = model::plot,
                    enabled = ui.placement != ARGraphPlacementState.GeneratingGraph,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ar3dgraph-plot"),
                ) { Text(if (ui.renderData == null) "Plot" else "Update") }
                OutlinedButton(
                    onClick = { showHelp = true },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ar3dgraph-help"),
                ) { Text("Help") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        model.resetView()
                    },
                    enabled = ui.placement == ARGraphPlacementState.Placed,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ar3dgraph-reset-view"),
                ) { Text("Reset View") }
                OutlinedButton(
                    onClick = {
                        model.resetPlacement()
                    },
                    enabled = ui.renderData != null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ar3dgraph-reset-placement"),
                ) { Text("Reset Placement") }
                OutlinedButton(
                    onClick = {
                        model.clearGraph()
                    },
                    enabled = ui.renderData != null || ui.placement == ARGraphPlacementState.GeneratingGraph,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("ar3dgraph-clear"),
                ) { Text("Clear all") }
            }

            StatusRow("AR", ui.capability.name, "Graph", ui.placement.name)

            when {
                ui.capability == ARCapabilityState.ARCoreNotInstalled || ui.capability == ARCapabilityState.ARCoreUpdateRequired -> {
                    Button(onClick = {
                        model.onInstallationRequested()
                        checkCapability()
                    }) { Text(if (ui.capability == ARCapabilityState.ARCoreUpdateRequired) "Update ARCore" else "Install ARCore") }
                }
                ui.capability == ARCapabilityState.Supported &&
                    (ui.cameraPermission == ARCameraPermissionState.PermissionRequired || ui.cameraPermission == ARCameraPermissionState.PermissionDenied) -> {
                    Button(onClick = {
                        model.markCameraRequested()
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }) { Text(if (ui.cameraPermission == ARCameraPermissionState.PermissionDenied) "Try camera permission again" else "Allow camera") }
                }
                ui.cameraPermission == ARCameraPermissionState.PermissionPermanentlyDenied -> {
                    Button(onClick = openSettings) { Text("Open Settings") }
                }
            }

            if (ui.capability == ARCapabilityState.Error || ui.session == ARSessionState.SessionError) {
                OutlinedButton(onClick = {
                    model.prepareSessionRetry()
                    checkCapability()
                }) { Text("Retry") }
            }
            }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("AR graph controls") },
            text = {
                Text("Drag to rotate • Pinch to resize • Tap to reposition\n\nReset View restores rotation and size. Reset Placement keeps the graph ready for a new tap. Clear removes the graph.")
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelp = false },
                    modifier = Modifier.testTag("ar3dgraph-help-close"),
                ) { Text("Got it") }
            },
            modifier = Modifier.testTag("ar3dgraph-help-dialog"),
        )
    }
}

@Composable
private fun SceneViewARGraphViewport(
    ui: AR3DGraphUiState,
    onSessionRunning: (String) -> Unit,
    onSessionPaused: () -> Unit,
    onTrackingChanged: (ARTrackingState, String) -> Unit,
    onPlacementStarted: () -> Unit,
    onPlacementPlaced: () -> Unit,
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    var lastPlacedGraphSignature by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onSessionRunning("SceneView AR session ready. Scan a floor, table or wall, then tap to anchor the 3D graph.")
        onDispose { onSessionPaused() }
    }

    PlacementScene(
        modifier = Modifier.fillMaxSize().testTag("ar3dgraph-sceneview"),
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        planeRenderer = ui.renderData == null || ui.placement != ARGraphPlacementState.Placed,
        planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
        instantPlacement = true,
        showReticle = true,
        coaching = true,
        groundShadows = true,
        sessionConfiguration = { session, config ->
            if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                config.depthMode = Config.DepthMode.AUTOMATIC
            }
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.focusMode = Config.FocusMode.AUTO
        },
        onPlaced = { anchor ->
            LaunchedEffect(anchor, ui.renderData) {
                onPlacementStarted()
                val signature = ui.renderData?.hashCode()
                if (signature != null && signature != lastPlacedGraphSignature) {
                    lastPlacedGraphSignature = signature
                }
                onPlacementPlaced()
            }
            AnchorNode(anchor = anchor) {
                ui.renderData?.let { graph ->
                    SceneViewGraphSurfaceNodes(
                        materialLoader = materialLoader,
                        graph = graph,
                    )
                }
            }
        },
        content = { controller ->
            LaunchedEffect(ui.renderData, ui.placement) {
                if (ui.renderData == null) {
                    controller.clear()
                    lastPlacedGraphSignature = null
                    onTrackingChanged(ARTrackingState.Tracking, "Graph cleared. Generate a 3D surface, then tap to place it.")
                } else if (ui.placement == ARGraphPlacementState.GraphReadyForPlacement && controller.count > 0) {
                    controller.clear()
                    lastPlacedGraphSignature = null
                    onTrackingChanged(ARTrackingState.Tracking, "Placement cleared. Tap a detected floor, table or wall to anchor it again.")
                } else if (controller.count == 0) {
                    onTrackingChanged(ARTrackingState.Tracking, "3D graph ready. Tap a detected floor, table or wall to anchor it.")
                }
            }
        },
    )
}

@Composable
private fun StatusRow(firstLabel: String, firstValue: String, secondLabel: String, secondValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$firstLabel: $firstValue", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("$secondLabel: $secondValue", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

private fun statusTitle(ui: AR3DGraphUiState): String = when {
    ui.capability == ARCapabilityState.Unsupported -> "AR is unavailable on this device"
    ui.capability == ARCapabilityState.ARCoreNotInstalled -> "ARCore is not installed"
    ui.capability == ARCapabilityState.ARCoreUpdateRequired -> "ARCore update required"
    ui.capability == ARCapabilityState.Supported && ui.cameraPermission != ARCameraPermissionState.Granted -> "Camera permission required"
    ui.session == ARSessionState.SessionRunning -> "AR session active"
    ui.session == ARSessionState.SessionError -> "AR initialization failed"
    else -> "AR 3D Graph"
}
