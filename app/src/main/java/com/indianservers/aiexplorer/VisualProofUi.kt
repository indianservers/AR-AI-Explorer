package com.indianservers.aiexplorer

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import com.indianservers.aiexplorer.core.ExpressionEngine
import com.indianservers.aiexplorer.persistence.DurableMathStore
import com.indianservers.aiexplorer.persistence.LocalReliabilityMonitor
import com.indianservers.aiexplorer.persistence.MathFileExchange
import com.indianservers.aiexplorer.phase2.mathstudio.UnifiedMathStudioScreen
import com.indianservers.aiexplorer.phase3.mathlearning.AdaptiveMathLearningScreen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase1Screen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase2Screen
import com.indianservers.aiexplorer.probabilitystats.ProbabilityStatisticsPhase3Screen
import com.indianservers.aiexplorer.core.TrustedMathKernel
import com.indianservers.aiexplorer.core.EquivalenceEvidence
import com.indianservers.aiexplorer.core.InteractiveTrigEngine
import com.indianservers.aiexplorer.core.InteractiveTrigIdentityLab
import com.indianservers.aiexplorer.core.TrigFunction
import com.indianservers.aiexplorer.core.TrigAngleUnit
import com.indianservers.aiexplorer.core.InverseTrigFunction
import com.indianservers.aiexplorer.core.PolarCurveType
import com.indianservers.aiexplorer.core.HarmonicComponent
import com.indianservers.aiexplorer.core.TrigTransform
import com.indianservers.aiexplorer.core.TrigViewport
import com.indianservers.aiexplorer.core.TrigViewportEngine
import com.indianservers.aiexplorer.core.TriangleTrigSolver
import com.indianservers.aiexplorer.core.Geometry2D
import com.indianservers.aiexplorer.core.Geometry3D
import com.indianservers.aiexplorer.core.GeometryGesturePolicy
import com.indianservers.aiexplorer.core.GeometryGestureTarget
import com.indianservers.aiexplorer.core.AxisConstraint
import com.indianservers.aiexplorer.core.GestureMode
import com.indianservers.aiexplorer.core.InteractionGeometry
import com.indianservers.aiexplorer.core.PrecisionInteraction
import com.indianservers.aiexplorer.core.SmartSnapEngine
import com.indianservers.aiexplorer.core.CrossSection3D
import com.indianservers.aiexplorer.core.EditableSectionPlane
import com.indianservers.aiexplorer.core.ProjectedSpatialMesh
import com.indianservers.aiexplorer.core.ProjectedSpatialPoint
import com.indianservers.aiexplorer.core.SpatialSubObjectPicker
import com.indianservers.aiexplorer.core.SpatialSubObjectType
import com.indianservers.aiexplorer.core.SurfaceAnalysisHandleEngine
import com.indianservers.aiexplorer.core.TransformGizmoAxis
import com.indianservers.aiexplorer.core.TransformGizmoEngine
import com.indianservers.aiexplorer.core.TransformGizmoHandle
import com.indianservers.aiexplorer.core.TransformGizmoKind
import com.indianservers.aiexplorer.core.Graph3D
import com.indianservers.aiexplorer.core.GraphAnalysis
import com.indianservers.aiexplorer.core.GraphDefinitionKind
import com.indianservers.aiexplorer.core.StatisticsEngine
import com.indianservers.aiexplorer.core.ProbabilityEngine
import com.indianservers.aiexplorer.core.AdvancedGraphDefinition
import com.indianservers.aiexplorer.core.AdvancedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphEngine
import com.indianservers.aiexplorer.core.TypedGraphExpression
import com.indianservers.aiexplorer.core.TypedGraphExpressionParser
import com.indianservers.aiexplorer.core.AdvancedGraphKind
import com.indianservers.aiexplorer.core.GraphDomain
import com.indianservers.aiexplorer.core.GraphViewport
import com.indianservers.aiexplorer.core.GraphEquationNames
import com.indianservers.aiexplorer.core.GraphAddKind
import com.indianservers.aiexplorer.core.GraphAxisSettings
import com.indianservers.aiexplorer.core.GraphDomainSelection
import com.indianservers.aiexplorer.core.GraphLineStyle
import com.indianservers.aiexplorer.core.GraphSnapshot
import com.indianservers.aiexplorer.core.GraphTransformKind
import com.indianservers.aiexplorer.core.GraphUxEngine
import com.indianservers.aiexplorer.core.GraphDirectManipulationEngine
import com.indianservers.aiexplorer.core.GraphFitResult
import com.indianservers.aiexplorer.core.AdvancedSpatialInteractionEngine
import com.indianservers.aiexplorer.core.SpatialAlignment
import com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap
import com.indianservers.aiexplorer.core.SpatialCameraBookmark
import com.indianservers.aiexplorer.core.SpatialDragPlane
import com.indianservers.aiexplorer.core.SpatialTransformSpace
import com.indianservers.aiexplorer.core.SpatialVisualMode
import com.indianservers.aiexplorer.core.GraphViewState
import com.indianservers.aiexplorer.core.AxisNumberFormat
import com.indianservers.aiexplorer.core.DistributionEngine
import com.indianservers.aiexplorer.core.DistributionKind
import com.indianservers.aiexplorer.core.ProbabilityDistribution
import com.indianservers.aiexplorer.core.SurfaceCalculus
import com.indianservers.aiexplorer.core.SurfaceInputInterpreter
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.MissingDataPolicy
import com.indianservers.aiexplorer.core.Phase4Statistics
import com.indianservers.aiexplorer.core.RandomExperimentEngine
import com.indianservers.aiexplorer.core.RandomExperimentKind
import com.indianservers.aiexplorer.core.ConditionalProbabilityEngine
import com.indianservers.aiexplorer.core.CombinatoricsLab
import com.indianservers.aiexplorer.core.ManipulativeEngine
import com.indianservers.aiexplorer.core.ManipulativeItem
import com.indianservers.aiexplorer.core.ManipulativeKind
import com.indianservers.aiexplorer.core.ManipulativeScene
import com.indianservers.aiexplorer.core.FormalMathDestination
import com.indianservers.aiexplorer.core.VisualProofCatalog
import com.indianservers.aiexplorer.core.VisualProofEngine
import com.indianservers.aiexplorer.core.BooleanFormulaLaw
import com.indianservers.aiexplorer.core.SetLogicCatalog
import com.indianservers.aiexplorer.core.SetLogicEngine
import com.indianservers.aiexplorer.core.SetStudioTool
import com.indianservers.aiexplorer.core.SetTheoryLearningCatalog
import com.indianservers.aiexplorer.core.SetTheoryStudioEngine
import com.indianservers.aiexplorer.core.DescriptiveStatistics
import com.indianservers.aiexplorer.core.HistogramBin
import com.indianservers.aiexplorer.core.InferentialStatistics
import com.indianservers.aiexplorer.core.StatisticsCurriculum
import com.indianservers.aiexplorer.core.StatisticsStudyLevel
import com.indianservers.aiexplorer.core.MathProblemSolver
import com.indianservers.aiexplorer.core.MathSolverTutor
import com.indianservers.aiexplorer.core.GuidedSolution
import com.indianservers.aiexplorer.core.SolverMethod
import com.indianservers.aiexplorer.core.SolverReveal
import com.indianservers.aiexplorer.core.SolverDestination
import com.indianservers.aiexplorer.core.SolverResultKind
import com.indianservers.aiexplorer.core.MathInputIntelligence
import com.indianservers.aiexplorer.core.MathInputTokenKind
import com.indianservers.aiexplorer.core.CasAssumptionDraft
import com.indianservers.aiexplorer.core.CasInteractionEngine
import com.indianservers.aiexplorer.core.CasKeyboardCatalog
import com.indianservers.aiexplorer.core.CasKeyboardLayer
import com.indianservers.aiexplorer.core.CasSolutionMethod
import com.indianservers.aiexplorer.core.CasNotebookInteractionEngine
import com.indianservers.aiexplorer.core.CasNotebookSession
import com.indianservers.aiexplorer.core.CasInterpretationResolver
import com.indianservers.aiexplorer.core.CasInterpretationOption
import com.indianservers.aiexplorer.core.CasStructuredImportEngine
import com.indianservers.aiexplorer.core.CasDimensionalAnalyzer
import com.indianservers.aiexplorer.core.CasExportEngine
import com.indianservers.aiexplorer.core.CasExportFormat
import com.indianservers.aiexplorer.core.CasExpressionTransfer
import com.indianservers.aiexplorer.core.CasTargetAction
import com.indianservers.aiexplorer.core.CasCaptureNormalizer
import com.indianservers.aiexplorer.core.CasInputModality
import com.indianservers.aiexplorer.core.CasScopedVariable
import com.indianservers.aiexplorer.core.CasVariableScope
import com.indianservers.aiexplorer.core.CasStepDisclosureEngine
import com.indianservers.aiexplorer.core.CasDirectManipulationEngine
import com.indianservers.aiexplorer.core.CasManipulationState
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import com.indianservers.aiexplorer.input.IntentAwareMathField
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
import com.indianservers.aiexplorer.input.CompactMathField
import com.indianservers.aiexplorer.input.MathKeyboardContext
import com.indianservers.aiexplorer.core.MathNotebookDocument
import com.indianservers.aiexplorer.core.MathNotebookEngine
import com.indianservers.aiexplorer.core.NotebookCell
import com.indianservers.aiexplorer.core.NotebookCellKind
import com.indianservers.aiexplorer.core.ProblemSolution
import com.indianservers.aiexplorer.core.DeviceCapabilityManager
import com.indianservers.aiexplorer.core.DeviceCapabilityProfile
import com.indianservers.aiexplorer.core.ProductPerformanceManager
import com.indianservers.aiexplorer.core.AccessibilityNodeEvidence
import com.indianservers.aiexplorer.core.AccessibilityQaEngine
import com.indianservers.aiexplorer.core.DeterministicMathBenchmarkRunner
import com.indianservers.aiexplorer.core.MathBenchmarkReport
import com.indianservers.aiexplorer.core.QaEvidenceSection
import com.indianservers.aiexplorer.core.ReleaseMathBenchmarkCatalog
import com.indianservers.aiexplorer.core.ReleaseQaEvidenceBundle
import com.indianservers.aiexplorer.core.ReleaseQaEvidenceCodec
import com.indianservers.aiexplorer.core.ProductPerformanceSnapshot
import com.indianservers.aiexplorer.core.AngleMode
import com.indianservers.aiexplorer.core.ScientificCalculator
import com.indianservers.aiexplorer.core.ScientificCalculatorResult
import com.indianservers.aiexplorer.core.SmartScientificCalculator
import com.indianservers.aiexplorer.core.SmartCalculatorOutcome
import com.indianservers.aiexplorer.core.CalculatorInputIntelligence
import com.indianservers.aiexplorer.core.CalculatorKeyboardLayer
import com.indianservers.aiexplorer.core.CalculatorEditorHistory
import com.indianservers.aiexplorer.core.AdvancedCalculatorMode
import com.indianservers.aiexplorer.core.AdvancedScientificCalculator
import com.indianservers.aiexplorer.core.CalculatorFavourites
import com.indianservers.aiexplorer.core.CalculatorRecognitionAdapters
import com.indianservers.aiexplorer.core.ProfessionalCalculatorMode
import com.indianservers.aiexplorer.core.ProfessionalScientificCalculator
import com.indianservers.aiexplorer.learningintelligence.ui.LearningIntelligenceFeatureRoot
import com.indianservers.aiexplorer.core.SolutionStepRole
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
import com.indianservers.aiexplorer.core.SmartSolidPlacementEngine
import com.indianservers.aiexplorer.core.SolidMeshFactory
import com.indianservers.aiexplorer.core.Vec2
import com.indianservers.aiexplorer.core.Vec3
import com.indianservers.aiexplorer.core.Vector3D
import com.indianservers.aiexplorer.core.stripEquation
import com.indianservers.aiexplorer.core.trim
import com.indianservers.aiexplorer.learning.Assignment
import com.indianservers.aiexplorer.learning.ActivityAnswer
import com.indianservers.aiexplorer.learning.ActivityBlock
import com.indianservers.aiexplorer.learning.ActivityEvaluationContext
import com.indianservers.aiexplorer.learning.ClassroomEngine
import com.indianservers.aiexplorer.learning.InteractiveActivityAuthoring
import com.indianservers.aiexplorer.learning.InteractiveActivityCatalog
import com.indianservers.aiexplorer.learning.InteractiveActivityEngine
import com.indianservers.aiexplorer.learning.LearnerProgress
import com.indianservers.aiexplorer.learning.LearningActivity
import com.indianservers.aiexplorer.learning.LearningCatalog
import com.indianservers.aiexplorer.learning.LearningEvaluator
import com.indianservers.aiexplorer.learning.LearningPackage
import com.indianservers.aiexplorer.learning.LearningOperation
import com.indianservers.aiexplorer.learning.LearningOperationType
import com.indianservers.aiexplorer.learning.LearningRole
import com.indianservers.aiexplorer.learning.LearningValidation
import com.indianservers.aiexplorer.learning.FormulaCategory
import com.indianservers.aiexplorer.learning.icon
import com.indianservers.aiexplorer.learning.KnowledgeLevel
import com.indianservers.aiexplorer.learning.KnowledgeTopic
import com.indianservers.aiexplorer.learning.DictionaryClassBand
import com.indianservers.aiexplorer.learning.DictionaryDifficulty
import com.indianservers.aiexplorer.learning.DictionaryTerm
import com.indianservers.aiexplorer.learning.MathKnowledgeCatalog
import com.indianservers.aiexplorer.learning.MathDictionaryCatalog
import com.indianservers.aiexplorer.learning.KnowledgeSearchResult
import com.indianservers.aiexplorer.learning.McqQuestion
import com.indianservers.aiexplorer.learning.OfflineLearningQueue
import com.indianservers.aiexplorer.learning.PackageValidation
import com.indianservers.aiexplorer.input.HandwritingMathInput
import com.indianservers.aiexplorer.learning.ProgressStatus
import com.indianservers.aiexplorer.learning.QuizEngine
import com.indianservers.aiexplorer.learning.QuizLevel
import com.indianservers.aiexplorer.learning.QuizSession
import com.indianservers.aiexplorer.learning.QuizSubject
import com.indianservers.aiexplorer.learning.theoremCategories
import com.indianservers.aiexplorer.workspace.AddVector3DCommand
import com.indianservers.aiexplorer.workspace.DeleteVector3DCommand
import com.indianservers.aiexplorer.workspace.AddConstructionCommand
import com.indianservers.aiexplorer.workspace.AddDependentPointCommand
import com.indianservers.aiexplorer.workspace.AddGeometryConstraint2DCommand
import com.indianservers.aiexplorer.workspace.AddShapeFromPointsCommand
import com.indianservers.aiexplorer.workspace.AddFunctionCommand
import com.indianservers.aiexplorer.workspace.AddPointCommand
import com.indianservers.aiexplorer.workspace.AddSolidCommand
import com.indianservers.aiexplorer.workspace.CommandHistory
import com.indianservers.aiexplorer.workspace.EditExpressionCommand
import com.indianservers.aiexplorer.workspace.DeleteShapeCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidCommand
import com.indianservers.aiexplorer.workspace.DeleteShapesCommand
import com.indianservers.aiexplorer.workspace.DeleteSolidsCommand
import com.indianservers.aiexplorer.workspace.ReplaceSolidsCommand
import com.indianservers.aiexplorer.workspace.DeleteFunctionCommand
import com.indianservers.aiexplorer.workspace.LinkedMathKernel
import com.indianservers.aiexplorer.workspace.LinkedMathView
import com.indianservers.aiexplorer.workspace.GraphRowMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderMetadataState
import com.indianservers.aiexplorer.workspace.GraphSliderPlaybackMode
import com.indianservers.aiexplorer.workspace.Geometry2DInteractionEngine
import com.indianservers.aiexplorer.workspace.Geometry2DDirectManipulation
import com.indianservers.aiexplorer.workspace.Geometry2DDragPlanner
import com.indianservers.aiexplorer.workspace.ConstraintFeedbackLevel
import com.indianservers.aiexplorer.workspace.GeometryProtocolStatus
import com.indianservers.aiexplorer.workspace.MathObjectGraph
import com.indianservers.aiexplorer.workspace.MathModule
import com.indianservers.aiexplorer.workspace.AIExplorerProjectArchive
import com.indianservers.aiexplorer.workspace.AppSecurityAuditEngine
import com.indianservers.aiexplorer.workspace.AppSecurityConfiguration
import com.indianservers.aiexplorer.workspace.GeoGebraExchange
import com.indianservers.aiexplorer.workspace.ProjectSection
import com.indianservers.aiexplorer.workspace.ProjectSectionKind
import com.indianservers.aiexplorer.workspace.MovePointCommand
import com.indianservers.aiexplorer.workspace.MovePointsCommand
import com.indianservers.aiexplorer.workspace.ReplaceGeometry2DCommand
import com.indianservers.aiexplorer.workspace.MoveSolidCommand
import com.indianservers.aiexplorer.workspace.MoveVector3DCommand
import com.indianservers.aiexplorer.workspace.Shape2D
import com.indianservers.aiexplorer.workspace.Shape2DType
import com.indianservers.aiexplorer.workspace.PointDependencyType
import com.indianservers.aiexplorer.workspace.TransformSolidCommand
import com.indianservers.aiexplorer.workspace.TransformVector3DCommand
import com.indianservers.aiexplorer.workspace.TransformSpatialPlacementCommand
import kotlin.math.roundToInt
import com.indianservers.aiexplorer.workspace.TransformShape2DCommand
import com.indianservers.aiexplorer.workspace.UpdateShapeCommand
import com.indianservers.aiexplorer.workspace.UpdateFunctionCommand
import com.indianservers.aiexplorer.workspace.ReorderFunctionsCommand
import com.indianservers.aiexplorer.workspace.UpdateGraphRowMetadataCommand
import com.indianservers.aiexplorer.workspace.UpdateGraphSliderMetadataCommand
import com.indianservers.aiexplorer.workspace.WorkspaceJson
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.Geometry2DSnapshot
import com.indianservers.aiexplorer.workspace.geometry2DSnapshot
import com.indianservers.aiexplorer.workspace.UniversalMathDocument
import com.indianservers.aiexplorer.workspace.UniversalMathDocumentEngine
import com.indianservers.aiexplorer.workspace.UniversalWorkspaceBridge
import com.indianservers.aiexplorer.workspace.UniversalMathObjectFactory
import com.indianservers.aiexplorer.workspace.recomputed
import com.indianservers.aiexplorer.workspace.resolvePointDependency
import com.indianservers.aiexplorer.spatial.ARScaleMode
import com.indianservers.aiexplorer.spatial.ARAvailability
import com.indianservers.aiexplorer.spatial.ARCapabilities
import com.indianservers.aiexplorer.arengine.arcore.ArCoreRuntime
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.interaction.ArGizmoAxis
import com.indianservers.aiexplorer.arengine.interaction.ArGizmoMode
import com.indianservers.aiexplorer.arengine.interaction.ArPickHit
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionEngine
import com.indianservers.aiexplorer.arengine.interaction.ArSelectionState
import com.indianservers.aiexplorer.arengine.interaction.ArSubObjectKind
import com.indianservers.aiexplorer.spatial.SpatialSafety
import com.indianservers.aiexplorer.spatial.TrackingQuality
import com.indianservers.aiexplorer.spatial.SpatialPlacementEngine
import com.indianservers.aiexplorer.spatial.ARCoreCompositorView
import com.indianservers.aiexplorer.spatial.ARFrameState
import com.indianservers.aiexplorer.spatial.toSpatialCapabilities
import com.indianservers.aiexplorer.spatial.toSpatialFrame
import com.indianservers.aiexplorer.spatial.toSpatialHit
import com.indianservers.aiexplorer.spatial.previewSpatialPlacement
import com.indianservers.aiexplorer.spatial.ArPhase4SpatialBridge
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisBridge
import com.indianservers.aiexplorer.spatial.ArPhase5AnalysisOptions
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceBridge
import com.indianservers.aiexplorer.spatial.ArMathWorkspaceMode
import com.indianservers.aiexplorer.spatial.ARPrivacySafetyChecklist
import com.indianservers.aiexplorer.spatial.SharedGpuSceneCompiler
import com.indianservers.aiexplorer.spatial.SharedSpatialSceneBuilder
import com.indianservers.aiexplorer.spatial.SpatialAnnotation
import com.indianservers.aiexplorer.spatial.SpatialCompositorScene
import com.indianservers.aiexplorer.spatial.SpatialHit
import com.indianservers.aiexplorer.spatial.SpatialHitType
import com.indianservers.aiexplorer.spatial.SpatialLessonCatalog
import com.indianservers.aiexplorer.spatial.SpatialPerformanceManager
import com.indianservers.aiexplorer.spatial.ThermalLevel
import com.indianservers.aiexplorer.gamifymaths.GamifyMathsRoot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.round
import kotlin.math.log10
import kotlin.math.pow

internal fun latexStyleFormula(source: String): String = displayLatexFormula(source)

@Composable
internal fun MathFormulaText(
    formula: String,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
) {
    Text(
        text = latexStyleFormula(formula),
        color = color,
        fontFamily = FontFamily.Serif,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

internal val InteractiveVisualProofSceneIds = VisualProofCatalog.labs.mapTo(linkedSetOf()) { it.id }

private fun proofValue(value: Double): String = trim(round(value * 100.0) / 100.0)

@Composable
internal fun InteractiveVisualProofCanvas(
    playback: com.indianservers.aiexplorer.core.ProofPlayback,
    zoom: Float = 1f,
    onParameter: (String, Double) -> Unit,
) {
    val frame = playback.frame
    val primary = frame.lab.parameters.firstOrNull()
    val profile = VisualProofCatalog.profileFor(frame.lab.id)
    var proofLensOpen by remember(frame.lab.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProofStageStrip(frame)
        ProofPathBreadcrumbs(frame)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(330.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xEE030A12))
                .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(16.dp))
                .appWorkspaceTreatment(cornerRadius = 16.dp, accent = Cyan, secondary = Violet)
                .pointerInput(frame.lab.id, frame.parameters) {
                    if (primary != null) detectDragGestures { change, _ ->
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onParameter(primary.name, primary.minimum + ratio * (primary.maximum - primary.minimum))
                        change.consume()
                    }
                }
                .semantics { contentDescription = "Interactive visual proof for ${frame.lab.title}; drag horizontally to change ${primary?.name ?: "the construction"}" },
        ) {
            val w = size.width
            val h = size.height
            val scale = minOf(w / 12f, h / 8f) * zoom.coerceIn(.5f, 2.5f)
            fun point(x: Double, y: Double) = Offset(w * .5f + x.toFloat() * scale, h * .58f - y.toFloat() * scale)
            fun label(text: String, at: Offset, color: Color = Ink, textSize: Float = 25f) {
                drawContext.canvas.nativeCanvas.drawText(text, at.x, at.y, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                    this.textSize = textSize
                    isFakeBoldText = true
                })
            }
            fun polygon(points: List<Offset>, color: Color, fill: Color = color.copy(.13f)) {
                if (points.isEmpty()) return
                val path = Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                drawPath(path, fill)
                drawPath(path, color, style = Stroke(4f, cap = StrokeCap.Round))
            }
            fun angleArc(vertex: Offset, first: Offset, second: Offset, radius: Float, color: Color) {
                val start = Math.toDegrees(atan2((first.y - vertex.y).toDouble(), (first.x - vertex.x).toDouble())).toFloat()
                val end = Math.toDegrees(atan2((second.y - vertex.y).toDouble(), (second.x - vertex.x).toDouble())).toFloat()
                var sweep = (end - start + 540f) % 360f - 180f
                if (sweep == -180f) sweep = 180f
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = vertex - Offset(radius, radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(5f, cap = StrokeCap.Round),
                )
            }
            fun evidence(name: String, fallback: Double = 0.0): Double =
                frame.measurements[name]?.takeIf(Double::isFinite) ?: fallback
            if (com.indianservers.aiexplorer.core.ProofEnhancement.GridBackground in profile.features) {
                val gridStep = 28f
                var gx = 0f
                while (gx <= w) { drawLine(Grid.copy(.42f), Offset(gx, 0f), Offset(gx, h), 1f); gx += gridStep }
                var gy = 0f
                while (gy <= h) { drawLine(Grid.copy(.42f), Offset(0f, gy), Offset(w, gy), 1f); gy += gridStep }
            }
            drawLine(Grid, Offset(18f, h * .58f), Offset(w - 18f, h * .58f), 1.5f)
            when (frame.lab.id) {
                "triangle-angle-sum" -> {
                    val a = point(-2.7, 0.0); val b = point(2.7, 0.0); val c = point(frame.parameters.getValue("offset") - 1.0, frame.parameters.getValue("height"))
                    polygon(listOf(a, b, c), Cyan)
                    drawCircle(Violet, 18f, a, style = Stroke(5f)); drawCircle(Green, 18f, b, style = Stroke(5f)); drawCircle(Amber, 18f, c, style = Stroke(5f))
                    val angleValues = frame.measurements.values.take(3).toList()
                    val angleA = angleValues.getOrElse(0) { 0.0 }
                    val angleB = angleValues.getOrElse(1) { 0.0 }
                    val angleC = angleValues.getOrElse(2) { 0.0 }
                    val sideA = evidence("side a = BC")
                    val sideB = evidence("side b = CA")
                    val sideC = evidence("side c = AB")
                    angleArc(a, b, c, 31f, Violet)
                    angleArc(b, c, a, 31f, Green)
                    angleArc(c, a, b, 31f, Amber)
                    label("A", a + Offset(-28f, 30f), Violet, 25f)
                    label("B", b + Offset(14f, 30f), Green, 25f)
                    label("C", c + Offset(-7f, -18f), Amber, 25f)
                    label("∠A ${proofValue(angleA)}°", a + Offset(26f, -12f), Violet, 19f)
                    label("∠B ${proofValue(angleB)}°", b + Offset(-112f, -12f), Green, 19f)
                    label("∠C ${proofValue(angleC)}°", c + Offset(-38f, 48f), Amber, 19f)
                    label("a = ${proofValue(sideA)}", (b + c) / 2f + Offset(14f, -8f), Green, 19f)
                    label("b = ${proofValue(sideB)}", (c + a) / 2f + Offset(-68f, -8f), Violet, 19f)
                    label("c = ${proofValue(sideC)}", (a + b) / 2f + Offset(-34f, 31f), Cyan, 19f)
                    label(
                        "${proofValue(angleA)}° + ${proofValue(angleB)}° + ${proofValue(angleC)}° = 180°",
                        Offset(w * .48f, h * .10f),
                        Green,
                        24f,
                    )
                    if (frame.step >= 2) {
                        val y = h * .18f; drawLine(Ink, Offset(w * .2f, y), Offset(w * .8f, y), 4f)
                        drawArc(Violet, 0f, 55f, false, Offset(w * .37f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                        drawArc(Amber, 55f, 70f, false, Offset(w * .46f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                        drawArc(Green, 125f, 55f, false, Offset(w * .55f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                    }
                }
                "pythagorean" -> {
                    val a = frame.parameters.getValue("a"); val b = frame.parameters.getValue("b"); val maxSide = maxOf(a, b).coerceAtLeast(1.0); val k = 2.6 / maxSide
                    val o = point(-1.8, -.8); val x = point(-1.8 + a * k, -.8); val y = point(-1.8, -.8 + b * k)
                    polygon(listOf(o, x, y), Amber)
                    val av = x - o; val bv = y - o
                    polygon(listOf(o, x, x + Offset(0f, av.x), o + Offset(0f, av.x)), Cyan)
                    polygon(listOf(o, y, y + Offset(-bv.y, 0f), o + Offset(-bv.y, 0f)), Violet)
                    val normal = Offset((y - x).y, -(y - x).x); val unit = normal / normal.getDistance().coerceAtLeast(1f)
                    polygon(listOf(x, y, y + unit * (y - x).getDistance(), x + unit * (y - x).getDistance()), Green)
                    label("a² + b² = c²", Offset(w * .35f, h * .14f), Green, 29f)
                }
                "circle-ratio" -> {
                    val r = frame.parameters.getValue("r"); val n = frame.parameters.getValue("n").toInt().coerceIn(6, 240)
                    val radius = (r * scale * .55).toFloat().coerceIn(28f, h * .27f)
                    val center = Offset(w * .28f, h * .53f)
                    val vertices = (0 until n).map { index ->
                        val theta = 2 * PI * index / n
                        center + Offset(cos(theta).toFloat() * radius, sin(theta).toFloat() * radius)
                    }
                    polygon(vertices, Cyan); drawCircle(Cyan.copy(.35f), radius, center, style = Stroke(2f)); drawLine(Amber, center, center + Offset(radius, 0f), 5f)
                    val length = (evidence("measured perimeter") / (2 * PI * r) * 2 * PI * radius).toFloat().coerceAtMost(w * .58f)
                    drawLine(Violet, Offset(w * .38f, h * .73f), Offset(w * .38f + length, h * .73f), 7f, cap = StrokeCap.Round)
                    label("$n-gon perimeter", Offset(w * .49f, h * .68f), Violet); label("d = 2r", Offset(w * .17f, h * .88f), Amber)
                }
                "triangle-area" -> {
                    val base = frame.parameters.getValue("base"); val height = frame.parameters.getValue("height"); val apex = frame.parameters.getValue("apex")
                    val k = 4.8 / maxOf(base, height); val a = point(-2.6, -.7); val b = point(-2.6 + base * k, -.7); val c = point(-2.6 + apex * k, -.7 + height * k)
                    polygon(listOf(a, b, c), Cyan)
                    if (frame.step >= 1) polygon(listOf(c, b, point(c.x.toDouble().let { 0.0 }, 0.0)), Violet)
                    drawLine(Amber, c, Offset(c.x, a.y), 3f); label("h", Offset(c.x + 8f, (c.y + a.y) / 2), Amber); label("A = bh/2", Offset(w * .62f, h * .2f), Green)
                }
                "parallelogram-area" -> {
                    val base = frame.parameters.getValue("base"); val height = frame.parameters.getValue("height"); val shear = frame.parameters.getValue("shear")
                    val k = 4.5 / maxOf(base, height); val p0 = point(-2.7, -.8); val p1 = point(-2.7 + base * k, -.8); val p2 = point(-2.7 + (base + shear) * k, -.8 + height * k); val p3 = point(-2.7 + shear * k, -.8 + height * k)
                    polygon(listOf(p0, p1, p2, p3), Cyan); drawLine(Amber, p3, Offset(p3.x, p0.y), 3f)
                    if (frame.step >= 1) { drawLine(Violet, p3, p0, 4f); drawLine(Violet.copy(.5f), p2, Offset(p1.x, p2.y), 3f) }
                    label("cut", Offset(p3.x + 8f, (p3.y + p0.y) / 2), Violet); label("A = bh", Offset(w * .68f, h * .18f), Green)
                }
                "trapezoid-area" -> {
                    val aLen = frame.parameters.getValue("a"); val bLen = frame.parameters.getValue("b"); val height = frame.parameters.getValue("height"); val k = 4.3 / maxOf(aLen, bLen, height)
                    val p0 = point(-2.5, -.8); val p1 = point(-2.5 + aLen * k, -.8); val p2 = point(-2.5 + (aLen + bLen) * k / 2, -.8 + height * k); val p3 = point(-2.5 + (aLen - bLen) * k / 2, -.8 + height * k)
                    polygon(listOf(p0, p1, p2, p3), Cyan)
                    if (frame.step >= 1) polygon(listOf(p3, p2, point(2.5, 2.1), point(2.5 - aLen * k, 2.1)), Violet)
                    label("a", Offset((p0.x + p1.x) / 2, p0.y + 28f), Amber); label("b", Offset((p2.x + p3.x) / 2, p2.y - 12f), Violet); label("A=(a+b)h/2", Offset(w * .6f, h * .13f), Green)
                }
                "circle-area" -> {
                    val r = frame.parameters.getValue("r"); val n = frame.parameters.getValue("n").toInt().coerceIn(6, 60); val radius = (r * scale * .45).toFloat().coerceIn(35f, h * .24f); val center = Offset(w * .25f, h * .5f)
                    drawCircle(Cyan.copy(.12f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(4f))
                    repeat(n) { index -> val angle = 2 * PI * index / n; drawLine(if (index % 2 == 0) Violet.copy(.55f) else Green.copy(.55f), center, center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius), 1.5f) }
                    val left = w * .5f; val top = h * .34f; val rectWidth = minOf(w * .43f, (PI * radius).toFloat())
                    repeat(n / 2) { i ->
                        val x0 = left + i * rectWidth / (n / 2); val x1 = left + (i + 1) * rectWidth / (n / 2)
                        polygon(listOf(Offset(x0, top + radius), Offset(x1, top + radius), Offset((x0 + x1) / 2, top)), if (i % 2 == 0) Violet else Green)
                    }
                    label("base → πr", Offset(left + rectWidth * .25f, top + radius + 34f), Amber); label("height = r", Offset(left, top - 12f), Cyan); label("A = πr²", Offset(w * .64f, h * .18f), Green)
                }
                "polygon-angle-sum" -> {
                    val n = frame.parameters.getValue("n").toInt().coerceIn(3, 12); val radius = (frame.parameters.getValue("radius") * scale * .55).toFloat().coerceIn(45f, h * .3f); val center = Offset(w * .45f, h * .52f)
                    val vertices = (0 until n).map { i -> val angle = -PI / 2 + 2 * PI * i / n; center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius) }
                    polygon(vertices, Cyan); vertices.drop(2).dropLast(1).forEach { drawLine(Violet, vertices.first(), it, 3f) }
                    label("${n - 2} triangles", Offset(w * .68f, h * .32f), Amber); label("S = (${n}-2)×180°", Offset(w * .59f, h * .17f), Green)
                }
                "similar-triangles" -> {
                    val aLen = frame.parameters.getValue("a"); val bLen = frame.parameters.getValue("b"); val k = frame.parameters.getValue("k"); val factor = 2.2 / maxOf(aLen, bLen)
                    val o = point(-3.0, -1.0); val x = point(-3.0 + aLen * factor, -1.0); val y = point(-3.0, -1.0 + bLen * factor)
                    polygon(listOf(o, x, y), Cyan); val x2 = o + (x - o) * k.toFloat(); val y2 = o + (y - o) * k.toFloat(); polygon(listOf(o, x2, y2), Violet)
                    drawLine(Amber, x, x2, 2f); drawLine(Amber, y, y2, 2f); label("scale k = ${trim(k)}", Offset(w * .62f, h * .24f), Amber); label("a'/a=b'/b=c'/c", Offset(w * .55f, h * .14f), Green)
                }
                "intersecting-chords" -> {
                    val radius = (frame.parameters.getValue("r") * scale * .6).toFloat().coerceIn(50f, h * .3f); val center = Offset(w * .43f, h * .52f); val px = center.x + frame.parameters.getValue("p").toFloat() * radius; val p = Offset(px, center.y); val angle = Math.toRadians(frame.parameters.getValue("angle")); val u = Offset(cos(angle).toFloat(), -sin(angle).toFloat())
                    drawCircle(Cyan.copy(.1f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(4f)); drawLine(Violet, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 4f)
                    val dot = (p.x - center.x) * u.x; val root = kotlin.math.sqrt((dot * dot + radius * radius - (p.x - center.x) * (p.x - center.x)).coerceAtLeast(0f)); drawLine(Amber, p - u * (root + dot), p + u * (root - dot), 4f); drawCircle(Green, 9f, p)
                    label("P", p + Offset(10f, -10f), Green); label("PA·PB = PC·PD", Offset(w * .56f, h * .16f), Green)
                }
                "circle-angle" -> {
                    val radius = (frame.parameters.getValue("r") * scale * .55).toFloat().coerceIn(50f, h * .3f); val center = Offset(w * .43f, h * .53f); val arc = Math.toRadians(frame.parameters.getValue("arc")); val cAngle = Math.toRadians(frame.parameters.getValue("c")); val a = center + Offset(cos(-arc / 2).toFloat() * radius, sin(-arc / 2).toFloat() * radius); val b = center + Offset(cos(arc / 2).toFloat() * radius, sin(arc / 2).toFloat() * radius); val c = center + Offset(cos(cAngle).toFloat() * radius, sin(cAngle).toFloat() * radius)
                    drawCircle(Cyan.copy(.08f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(4f)); drawLine(Violet, center, a, 4f); drawLine(Violet, center, b, 4f); drawLine(Amber, c, a, 4f); drawLine(Amber, c, b, 4f)
                    drawCircle(Green, 8f, center); label("O", center + Offset(10f, -8f), Green); label("∠AOB = 2∠ACB", Offset(w * .57f, h * .16f), Green)
                }
                "derivative-slope" -> {
                    val x0 = frame.parameters.getValue("x"); val delta = frame.parameters.getValue("h")
                    val left = w * .12f; val right = w * .88f; val top = h * .16f; val bottom = h * .82f
                    fun graphPoint(x: Double) = Offset(left + ((x + 5) / 10).toFloat() * (right - left), bottom - ((x * x) / 25).toFloat() * (bottom - top))
                    drawLine(Grid, Offset(left, bottom), Offset(right, bottom), 2f); drawLine(Grid, Offset((left + right) / 2, top), Offset((left + right) / 2, bottom), 2f)
                    val curve = Path(); (-100..100).forEachIndexed { index, i -> val p = graphPoint(i / 20.0); if (index == 0) curve.moveTo(p.x, p.y) else curve.lineTo(p.x, p.y) }; drawPath(curve, Cyan, style = Stroke(4f))
                    val p = graphPoint(x0); val q = graphPoint(x0 + delta); drawCircle(Violet, 8f, p); drawCircle(Amber, 8f, q)
                    val secantDirection = q - p; val secantUnit = secantDirection / secantDirection.getDistance().coerceAtLeast(1f); drawLine(Amber, p - secantUnit * 220f, q + secantUnit * 220f, 4f)
                    val tangentSlope = 2 * x0; val dx = 1.2; val t1 = graphPoint(x0 - dx); val rawT2 = graphPoint(x0 + dx); val pixelsPerX = (right - left) / 10f; val pixelsPerY = (bottom - top) / 25f; val t2 = Offset(t1.x + 2.4f * pixelsPerX, t1.y - (tangentSlope * 2.4).toFloat() * pixelsPerY); drawLine(Violet, t1, t2, 4f)
                    label("secant h=${trim(delta)}", Offset(w * .58f, h * .28f), Amber); label("h→0: slope→2x", Offset(w * .56f, h * .14f), Green)
                }
                "integral-area" -> {
                    val bound = frame.parameters.getValue("b"); val requestedN = frame.parameters.getValue("n").toInt().coerceAtLeast(2); val visualN = requestedN.coerceAtMost(40)
                    val left = w * .12f; val right = w * .88f; val bottom = h * .8f; val top = h * .18f; val chartWidth = right - left; val chartHeight = bottom - top
                    drawLine(Grid, Offset(left, bottom), Offset(right, bottom), 2f); drawLine(Grid, Offset(left, top), Offset(left, bottom), 2f)
                    val curve = Path()
                    (0..100).forEach { index ->
                        val ratio = index / 100f
                        val p = Offset(left + ratio * chartWidth, bottom - ratio * ratio * chartHeight)
                        if (index == 0) curve.moveTo(p.x, p.y) else curve.lineTo(p.x, p.y)
                    }
                    drawPath(curve, Cyan, style = Stroke(5f))
                    repeat(visualN) { i ->
                        val x0 = left + i * chartWidth / visualN; val x1 = left + (i + 1) * chartWidth / visualN; val midpoint = (i + .5f) / visualN; val y = bottom - midpoint * midpoint * chartHeight
                        drawRect(if (i % 2 == 0) Violet.copy(.18f) else Green.copy(.18f), Offset(x0, y), Size(x1 - x0, bottom - y)); drawRect(if (i % 2 == 0) Violet else Green, Offset(x0, y), Size(x1 - x0, bottom - y), style = Stroke(1.5f))
                    }
                    label("${requestedN} midpoint rectangles", Offset(w * .5f, h * .9f), Amber)
                    label("error ${proofValue(evidence("absolute error"))}", Offset(w * .68f, h * .82f), Violet, 19f)
                    label("∫₀ᵇx² dx = b³/3 = ${trim(bound * bound * bound / 3)}", Offset(w * .34f, h * .13f), Green)
                }
                "normal-area" -> {
                    val z = frame.parameters.getValue("z"); val left = w * .08f; val right = w * .92f; val baseline = h * .76f; val amplitude = h * .5f
                    fun normalPoint(x: Double) = Offset(left + ((x + 4) / 8).toFloat() * (right - left), baseline - kotlin.math.exp(-.5 * x * x).toFloat() * amplitude)
                    val curve = Path(); val shaded = Path().apply { moveTo(normalPoint(-z).x, baseline) }
                    (-160..160).forEachIndexed { index, i -> val x = i / 40.0; val p = normalPoint(x); if (index == 0) curve.moveTo(p.x, p.y) else curve.lineTo(p.x, p.y); if (x in -z..z) shaded.lineTo(p.x, p.y) }
                    shaded.lineTo(normalPoint(z).x, baseline); shaded.close(); drawPath(shaded, Violet.copy(.28f)); drawPath(curve, Cyan, style = Stroke(5f)); drawLine(Grid, Offset(left, baseline), Offset(right, baseline), 2f)
                    val zl = normalPoint(-z).x; val zr = normalPoint(z).x; drawLine(Amber, Offset(zl, baseline), normalPoint(-z), 3f); drawLine(Amber, Offset(zr, baseline), normalPoint(z), 3f)
                    label("-z", Offset(zl - 16f, baseline + 28f), Amber); label("z", Offset(zr - 4f, baseline + 28f), Amber); label("symmetric tails", Offset(w * .67f, h * .18f), Green)
                }
                "vector-addition" -> {
                    val origin = Offset(w * .22f, h * .72f); val vectorScale = minOf(w, h) / 12f
                    val u = Offset(frame.parameters.getValue("ux").toFloat() * vectorScale, -frame.parameters.getValue("uy").toFloat() * vectorScale); val v = Offset(frame.parameters.getValue("vx").toFloat() * vectorScale, -frame.parameters.getValue("vy").toFloat() * vectorScale)
                    fun arrow(start: Offset, delta: Offset, color: Color, name: String) { val end = start + delta; drawLine(color, start, end, 7f, cap = StrokeCap.Round); val unit = delta / delta.getDistance().coerceAtLeast(1f); val normal = Offset(-unit.y, unit.x); polygon(listOf(end, end - unit * 22f + normal * 10f, end - unit * 22f - normal * 10f), color, color); label(name, end + Offset(8f, -8f), color) }
                    arrow(origin, u, Cyan, "u"); arrow(origin + u, v, Violet, "v"); arrow(origin, v, Violet.copy(.65f), "v"); arrow(origin + v, u, Cyan.copy(.65f), "u"); arrow(origin, u + v, Green, "u+v")
                    drawLine(Grid, Offset(20f, origin.y), Offset(w - 20f, origin.y), 1.5f); label("same endpoint", origin + u + v + Offset(10f, 24f), Green)
                }
                "matrix-transform" -> {
                    val a = frame.parameters.getValue("a"); val b = frame.parameters.getValue("b"); val c = frame.parameters.getValue("c"); val d = frame.parameters.getValue("d"); val s = minOf(w, h) / 10f; val origin = Offset(w * .48f, h * .62f)
                    fun mapped(x: Double, y: Double) = origin + Offset(((a * x + b * y) * s).toFloat(), (-(c * x + d * y) * s).toFloat())
                    drawLine(Grid, Offset(20f, origin.y), Offset(w - 20f, origin.y), 2f); drawLine(Grid, Offset(origin.x, 20f), Offset(origin.x, h - 30f), 2f)
                    polygon(listOf(origin, origin + Offset(s, 0f), origin + Offset(s, -s), origin + Offset(0f, -s)), Muted)
                    polygon(listOf(mapped(0.0, 0.0), mapped(1.0, 0.0), mapped(1.0, 1.0), mapped(0.0, 1.0)), Cyan)
                    drawLine(Violet, origin, mapped(1.0, 0.0), 6f); drawLine(Amber, origin, mapped(0.0, 1.0), 6f); val determinant = a * d - b * c; label("det A = ${trim(determinant)}", Offset(w * .62f, h * .18f), Green); label("area scale = |det A|", Offset(w * .55f, h * .28f), Cyan)
                }
                "algebra-square" -> {
                    val a = frame.parameters.getValue("a"); val b = frame.parameters.getValue("b"); val total = a + b; val side = minOf(w * .58f, h * .58f); val left = w * .2f; val top = h * .18f; val splitX = left + side * (a / total).toFloat(); val splitY = top + side * (a / total).toFloat()
                    drawRect(Cyan.copy(.22f), Offset(left, top), Size(splitX - left, splitY - top)); drawRect(Violet.copy(.22f), Offset(splitX, top), Size(left + side - splitX, splitY - top)); drawRect(Violet.copy(.22f), Offset(left, splitY), Size(splitX - left, top + side - splitY)); drawRect(Green.copy(.22f), Offset(splitX, splitY), Size(left + side - splitX, top + side - splitY))
                    drawRect(Ink, Offset(left, top), Size(side, side), style = Stroke(5f)); drawLine(Ink, Offset(splitX, top), Offset(splitX, top + side), 3f); drawLine(Ink, Offset(left, splitY), Offset(left + side, splitY), 3f)
                    label("a²", Offset(left + 18f, top + 34f), Cyan); label("ab", Offset(splitX + 12f, top + 34f), Violet); label("ab", Offset(left + 18f, splitY + 34f), Violet); label("b²", Offset(splitX + 12f, splitY + 34f), Green); label("(a+b)² = a²+2ab+b²", Offset(w * .42f, h * .88f), Amber)
                }
                "shear-area" -> {
                    val base = frame.parameters.getValue("base"); val height = frame.parameters.getValue("height"); val shear = frame.parameters.getValue("shear"); val k = 4.2 / maxOf(base, height); val p0 = point(-2.5, -.9); val p1 = point(-2.5 + base * k, -.9); val p2 = point(-2.5 + (base + shear) * k, -.9 + height * k); val p3 = point(-2.5 + shear * k, -.9 + height * k)
                    drawRect(Muted.copy(.08f), p0.copy(y = p3.y), Size(p1.x - p0.x, p0.y - p3.y)); drawRect(Muted, p0.copy(y = p3.y), Size(p1.x - p0.x, p0.y - p3.y), style = Stroke(2f)); polygon(listOf(p0, p1, p2, p3), Cyan)
                    drawLine(Amber, p3, Offset(p3.x, p0.y), 4f); label("fixed h", Offset(p3.x + 10f, (p3.y + p0.y) / 2), Amber); label("same base × height", Offset(w * .58f, h * .17f), Green)
                }
                "unit-circle-identity" -> {
                    val theta = Math.toRadians(frame.parameters.getValue("theta")); val radius = minOf(w, h) * .27f; val center = Offset(w * .42f, h * .54f); val p = center + Offset(cos(theta).toFloat() * radius, -sin(theta).toFloat() * radius); val projection = Offset(p.x, center.y)
                    drawCircle(Cyan.copy(.09f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(5f)); drawLine(Grid, Offset(center.x - radius - 25f, center.y), Offset(center.x + radius + 25f, center.y), 2f); drawLine(Grid, Offset(center.x, center.y - radius - 25f), Offset(center.x, center.y + radius + 25f), 2f)
                    polygon(listOf(center, projection, p), Violet); drawLine(Cyan, center, projection, 6f); drawLine(Violet, projection, p, 6f); drawLine(Amber, center, p, 6f); drawCircle(Green, 10f, p)
                    label("cos θ", (center + projection) / 2f + Offset(0f, 28f), Cyan); label("sin θ", (projection + p) / 2f + Offset(10f, 0f), Violet); label("1", (center + p) / 2f, Amber); label("sin²θ + cos²θ = 1", Offset(w * .6f, h * .18f), Green)
                }
                "odd-sum-square" -> {
                    val n = frame.parameters.getValue("n").toInt().coerceIn(1, 15); val gridSide = minOf(w * .62f, h * .65f); val cell = gridSide / n; val left = w * .18f; val top = h * .13f
                    repeat(n) { row -> repeat(n) { col -> val layer = maxOf(row, col); val color = listOf(Cyan, Violet, Green, Amber)[layer % 4]; drawRect(color.copy(.22f), Offset(left + col * cell, top + row * cell), Size(cell, cell)); drawRect(color.copy(.7f), Offset(left + col * cell, top + row * cell), Size(cell, cell), style = Stroke(1.2f)) } }
                    repeat(n) { layer -> label((2 * layer + 1).toString(), Offset(left + gridSide + 14f, top + (layer + .65f) * cell), listOf(Cyan, Violet, Green, Amber)[layer % 4], 18f) }
                    label("1+3+...+${2 * n - 1} = ${n}²", Offset(w * .48f, h * .88f), Green)
                }
                "absolute-inequality" -> {
                    val xValue = frame.parameters.getValue("x"); val radius = frame.parameters.getValue("r")
                    val left = w * .1f; val right = w * .9f; val axisY = h * .55f
                    fun numberPoint(value: Double) = left + ((value + 6.0) / 12.0).toFloat() * (right - left)
                    drawLine(Ink, Offset(left, axisY), Offset(right, axisY), 5f, cap = StrokeCap.Round)
                    (-6..6).forEach { value -> val px = numberPoint(value.toDouble()); drawLine(Muted, Offset(px, axisY - 9f), Offset(px, axisY + 9f), 2f); if (value % 2 == 0) label(value.toString(), Offset(px - 8f, axisY + 35f), Muted, 18f) }
                    val intervalLeft = numberPoint(-radius); val intervalRight = numberPoint(radius)
                    drawLine(Cyan.copy(.28f), Offset(intervalLeft, axisY), Offset(intervalRight, axisY), 24f, cap = StrokeCap.Round)
                    drawCircle(Cyan, 10f, Offset(intervalLeft, axisY)); drawCircle(Cyan, 10f, Offset(intervalRight, axisY))
                    drawLine(Violet.copy(.28f), Offset(numberPoint(-xValue), axisY - 62f), Offset(numberPoint(xValue), axisY - 62f), 5f)
                    drawCircle(Amber, 13f, Offset(numberPoint(xValue), axisY)); drawCircle(Violet, 8f, Offset(numberPoint(-xValue), axisY))
                    drawLine(Green, Offset(numberPoint(0.0), axisY - 55f), Offset(numberPoint(xValue), axisY - 55f), 4f, cap = StrokeCap.Round)
                    label("distance |x|", Offset((numberPoint(0.0) + numberPoint(xValue)) / 2f - 45f, axisY - 72f), Green, 20f)
                    label("−r ≤ x ≤ r", Offset(w * .38f, h * .18f), Green, 29f)
                    label(if (abs(xValue) <= radius) "x is inside" else "x is outside", Offset(w * .7f, h * .32f), if (abs(xValue) <= radius) Green else Amber)
                }
                "equation-balance" -> {
                    val a = frame.parameters.getValue("a"); val b = frame.parameters.getValue("b"); val c = frame.parameters.getValue("c"); val solution = (c - b) / a
                    val fulcrum = Offset(w * .5f, h * .64f); val beamY = h * .45f
                    polygon(listOf(fulcrum, fulcrum + Offset(-32f, 70f), fulcrum + Offset(32f, 70f)), Amber, Amber.copy(.2f))
                    drawLine(Ink, Offset(w * .18f, beamY), Offset(w * .82f, beamY), 8f, cap = StrokeCap.Round)
                    listOf(w * .23f, w * .77f).forEach { px -> drawLine(Muted, Offset(px, beamY), Offset(px, beamY + 78f), 3f); drawLine(Cyan, Offset(px - 72f, beamY + 78f), Offset(px + 72f, beamY + 78f), 5f, cap = StrokeCap.Round) }
                    val leftText = if (frame.step == 0) "${trim(a)}x + ${trim(b)}" else if (frame.step == 1) "${trim(a)}x" else "x = ${trim(solution)}"
                    val rightText = if (frame.step == 0) trim(c) else if (frame.step == 1) trim(c - b) else trim(solution)
                    label(leftText, Offset(w * .17f, beamY + 66f), Cyan, 26f); label(rightText, Offset(w * .71f, beamY + 66f), Green, 26f)
                    drawLine(Violet.copy(.45f), Offset(w * .3f, h * .25f), Offset(w * .7f, h * .25f), 4f)
                    polygon(listOf(Offset(w * .7f, h * .25f), Offset(w * .67f, h * .225f), Offset(w * .67f, h * .275f)), Violet, Violet)
                    label(if (frame.step < 2) "same operation on both sides" else "equal groups reveal x", Offset(w * .3f, h * .18f), Violet, 23f)
                }
                "set-de-morgan" -> {
                    val universe = androidx.compose.ui.geometry.Rect(w * .1f, h * .17f, w * .9f, h * .72f)
                    drawRect(Violet.copy(.18f), universe.topLeft, universe.size)
                    val ca = Offset(w * .4f, h * .43f); val cb = Offset(w * .6f, h * .43f); val radius = minOf(w, h) * .19f
                    drawCircle(Color(0xFF07101B), radius, ca); drawCircle(Color(0xFF07101B), radius, cb)
                    drawRect(Ink, universe.topLeft, universe.size, style = Stroke(4f)); drawCircle(Cyan, radius, ca, style = Stroke(5f)); drawCircle(Amber, radius, cb, style = Stroke(5f))
                    label("A", ca + Offset(-radius * .62f, -radius * .55f), Cyan); label("B", cb + Offset(radius * .42f, -radius * .55f), Amber)
                    val inA = frame.parameters.getValue("inA") >= .5; val inB = frame.parameters.getValue("inB") >= .5
                    label("test point: A=${if (inA) 1 else 0}, B=${if (inB) 1 else 0}", Offset(w * .25f, h * .12f), Green, 22f)
                    label("A B | ¬(A∨B) | ¬A∧¬B", Offset(w * .23f, h * .82f), Ink, 20f)
                    listOf(false to false, false to true, true to false, true to true).forEachIndexed { index, (a, b) ->
                        val left = !(a || b); val right = !a && !b
                        label("${if (a) 1 else 0} ${if (b) 1 else 0} |     ${if (left) 1 else 0}      |     ${if (right) 1 else 0}", Offset(w * .3f, h * (.87f + index * .035f)), if (a == inA && b == inB) Green else Muted, 16f)
                    }
                }
                "epsilon-delta" -> {
                    val epsilon = frame.parameters.getValue("epsilon"); val delta = frame.parameters.getValue("delta")
                    val left = w * .12f; val right = w * .9f; val top = h * .12f; val bottom = h * .84f
                    fun graphPoint(x: Double, y: Double) = Offset(left + ((x + 1.0) / 4.0).toFloat() * (right - left), bottom - (y / 6.0).toFloat() * (bottom - top))
                    val a = 1.0; val limit = 2.0; val x0 = graphPoint(a, 0.0).x; val y0 = graphPoint(0.0, limit).y
                    val dxPixels = delta.toFloat() * (right - left) / 4f; val dyPixels = epsilon.toFloat() * (bottom - top) / 6f
                    drawRect(Cyan.copy(.13f), Offset(x0 - dxPixels, top), Size(dxPixels * 2f, bottom - top))
                    drawRect(Violet.copy(.16f), Offset(left, y0 - dyPixels), Size(right - left, dyPixels * 2f))
                    drawRect(Color(0xAA050A12), Offset(left, top), Size((x0 - dxPixels - left).coerceAtLeast(0f), bottom - top))
                    drawRect(Color(0xAA050A12), Offset(x0 + dxPixels, top), Size((right - x0 - dxPixels).coerceAtLeast(0f), bottom - top))
                    drawLine(Grid, Offset(left, bottom), Offset(right, bottom), 3f); drawLine(Grid, Offset(left, top), Offset(left, bottom), 3f)
                    val p1 = graphPoint(-1.0, -2.0); val p2 = graphPoint(3.0, 6.0); drawLine(Green, p1, p2, 5f)
                    drawLine(Cyan, Offset(x0 - dxPixels, top), Offset(x0 - dxPixels, bottom), 3f); drawLine(Cyan, Offset(x0 + dxPixels, top), Offset(x0 + dxPixels, bottom), 3f)
                    drawLine(Violet, Offset(left, y0 - dyPixels), Offset(right, y0 - dyPixels), 3f); drawLine(Violet, Offset(left, y0 + dyPixels), Offset(right, y0 + dyPixels), 3f)
                    label("δ input band", Offset(x0 - 65f, bottom - 14f), Cyan, 19f); label("ε output band", Offset(left + 8f, y0 - dyPixels - 8f), Violet, 19f)
                    label(if (2 * delta <= epsilon) "band containment verified" else "shrink δ to fit ε", Offset(w * .56f, h * .16f), if (2 * delta <= epsilon) Green else Amber, 22f)
                }
                "slope-triangle" -> {
                    val slope = frame.parameters.getValue("m"); val run = frame.parameters.getValue("run"); val rise = slope * run
                    val origin = Offset(w * .22f, h * .7f); val sx = w * .11f; val sy = h * .09f
                    drawLine(Cyan, origin - Offset(w * .08f, (-slope * w * .08f / sx * sy).toFloat()), origin + Offset(w * .62f, (-slope * w * .62f / sx * sy).toFloat()), 5f)
                    fun triangle(scaleFactor: Float, color: Color) {
                        val endX = origin.x + run.toFloat() * sx * scaleFactor; val corner = Offset(endX, origin.y); val end = Offset(endX, origin.y - rise.toFloat() * sy * scaleFactor)
                        drawLine(color, origin, corner, 6f); drawLine(color, corner, end, 6f); drawLine(color.copy(.5f), origin, end, 3f)
                        label("run", (origin + corner) / 2f + Offset(0f, 25f), color, 18f); label("rise", (corner + end) / 2f + Offset(8f, 0f), color, 18f)
                    }
                    triangle(1f, Amber); triangle(.55f, Violet)
                    label("same rise/run = ${trim(slope)}", Offset(w * .55f, h * .15f), Green, 25f)
                }
                "eigenvector-direction" -> {
                    val lambda = frame.parameters.getValue("lambda"); val other = frame.parameters.getValue("other"); val vy = frame.parameters.getValue("vy")
                    val origin = Offset(w * .5f, h * .56f); val unit = minOf(w, h) * .12f
                    (-4..4).forEach { i ->
                        drawLine(Grid.copy(.45f), Offset(origin.x + i * unit, 20f), Offset(origin.x + i * unit, h - 30f), 1.5f)
                        drawLine(Grid.copy(.45f), Offset(20f, origin.y + i * unit), Offset(w - 20f, origin.y + i * unit), 1.5f)
                    }
                    drawLine(Violet.copy(.35f), Offset(20f, origin.y), Offset(w - 20f, origin.y), 13f)
                    fun vectorArrow(end: Offset, color: Color, name: String) {
                        drawLine(color, origin, end, 7f, cap = StrokeCap.Round); val delta = end - origin; val direction = delta / delta.getDistance().coerceAtLeast(1f); val normal = Offset(-direction.y, direction.x)
                        polygon(listOf(end, end - direction * 24f + normal * 11f, end - direction * 24f - normal * 11f), color, color); label(name, end + Offset(8f, -8f), color)
                    }
                    vectorArrow(origin + Offset(unit * 1.35f, (-unit * 1.35f * vy).toFloat()), Cyan, "v")
                    vectorArrow(origin + Offset((unit * 1.35f * lambda).toFloat(), (-unit * 1.35f * other * vy).toFloat()), Green, "Av")
                    drawLine(Amber.copy(.55f), origin, origin + Offset(0f, (-unit * other).toFloat()), 4f)
                    val isEigenvector = frame.residual <= 1e-7
                    label(if (isEigenvector) "direction preserved" else "general vector turns", Offset(w * .57f, h * .18f), if (isEigenvector) Green else Amber, 24f)
                    label("cross(v,Av)=${trim(evidence("cross(v,Av)"))}", Offset(w * .57f, h * .3f), Amber, 19f)
                }
                "counting-paths" -> {
                    val rightSteps = frame.parameters.getValue("right").toInt().coerceIn(1, 6); val upSteps = frame.parameters.getValue("up").toInt().coerceIn(1, 6)
                    val left = w * .12f; val bottom = h * .76f; val cell = minOf((w * .48f) / rightSteps, (h * .55f) / upSteps)
                    for (i in 0..rightSteps) drawLine(Grid, Offset(left + i * cell, bottom), Offset(left + i * cell, bottom - upSteps * cell), 2f)
                    for (j in 0..upSteps) drawLine(Grid, Offset(left, bottom - j * cell), Offset(left + rightSteps * cell, bottom - j * cell), 2f)
                    for (i in 0..rightSteps) for (j in 0..upSteps) drawCircle(if (i == rightSteps && j == upSteps) Green else Cyan, if (i == rightSteps && j == upSteps) 8f else 4f, Offset(left + i * cell, bottom - j * cell))
                    val path = Path().apply { moveTo(left, bottom); repeat(rightSteps) { lineTo(left + (it + 1) * cell, bottom) }; repeat(upSteps) { lineTo(left + rightSteps * cell, bottom - (it + 1) * cell) } }; drawPath(path, Amber, style = Stroke(5f, cap = StrokeCap.Round))
                    val treeX = w * .72f; val treeY = h * .27f; drawCircle(Violet, 7f, Offset(treeX, treeY)); listOf(-1f, 1f).forEach { side -> drawLine(Violet, Offset(treeX, treeY), Offset(treeX + side * 58f, treeY + 62f), 3f) }; label("R", Offset(treeX - 75f, treeY + 52f), Cyan, 18f); label("U", Offset(treeX + 62f, treeY + 52f), Amber, 18f)
                    label("last move R or U", Offset(w * .64f, h * .18f), Violet, 21f); label("lattice paths = ${trim(evidence("all paths"))}", Offset(w * .58f, h * .72f), Green, 23f)
                    label("C = left parent + below parent", Offset(w * .52f, h * .86f), Ink, 19f)
                }
                "modular-clock" -> {
                    val a = frame.parameters.getValue("a").toInt(); val b = frame.parameters.getValue("b").toInt()
                    val modulus = frame.parameters.getValue("n").toInt().coerceIn(2, 16)
                    val remainderA = ((a % modulus) + modulus) % modulus; val remainderB = ((b % modulus) + modulus) % modulus
                    val center = Offset(w * .45f, h * .52f); val radius = minOf(w, h) * .29f
                    drawCircle(Cyan.copy(.08f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(5f))
                    repeat(modulus) { index ->
                        val angle = -PI / 2 + 2 * PI * index / modulus; val point = center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius)
                        val selected = index == remainderA || index == remainderB
                        drawCircle(if (selected) Green else Muted, if (selected) 12f else 5f, point); label(index.toString(), point + Offset(-7f, -13f), if (selected) Green else Ink, 18f)
                    }
                    fun hand(remainder: Int, color: Color) {
                        val angle = -PI / 2 + 2 * PI * remainder / modulus
                        drawLine(color, center, center + Offset(cos(angle).toFloat() * radius * .78f, sin(angle).toFloat() * radius * .78f), 7f, cap = StrokeCap.Round)
                    }
                    hand(remainderA, Amber); hand(remainderB, Violet)
                    label("a→$remainderA  b→$remainderB", Offset(w * .58f, h * .18f), if (remainderA == remainderB) Green else Amber, 24f)
                    label(if (remainderA == remainderB) "congruent" else "not congruent", Offset(w * .62f, h * .3f), if (remainderA == remainderB) Green else Muted, 22f)
                }
                "anscombe-quartet" -> {
                    drawAnscombeProof(frame, w, h)
                }
                else -> {
                    drawCircle(Cyan.copy(alpha = .12f), minOf(w, h) * .22f, Offset(w / 2f, h * .5f))
                    drawCircle(Cyan, minOf(w, h) * .22f, Offset(w / 2f, h * .5f), style = Stroke(4f))
                    label(frame.lab.formalResult, Offset(w * .18f, h * .5f), Green, 23f)
                    label("Use the live values and reasoning lens below.", Offset(w * .16f, h * .62f), Muted, 18f)
                }
            }
            if (profile.notToScale) label("NOT TO SCALE", Offset(w - 178f, 28f), Amber, 18f)
            label("Step ${frame.step + 1}/${frame.lab.steps.size} · drag horizontally", Offset(22f, h - 22f), Muted, 21f)
        }
        ProofLiveEvidence(frame)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${frame.step + 1}. ${frame.lab.steps[frame.step]}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            GlowButton(if (proofLensOpen) "Hide proof lens" else "Open proof lens") { proofLensOpen = !proofLensOpen }
        }
        AnimatedVisibility(proofLensOpen) { ProofReasoningLens(frame, profile) }
    }
}

@Composable
internal fun ProofLearningCycle(frame: com.indianservers.aiexplorer.core.ProofFrame) {
    val manipulation = if (frame.lab.parameters.isEmpty()) {
        "Use Previous, Next, and Reveal to compare every logical region or construction stage."
    } else {
        "Drag the visual or adjust a slider. Watch the live values before revealing the final stage."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceB.copy(alpha = .52f))
            .border(1.dp, Cyan.copy(alpha = .36f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("LEARN THROUGH THE PROOF", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Text("1. Predict: ${frame.lab.changesPrompt}", color = Ink, fontSize = 11.sp)
        Text("2. Manipulate: $manipulation", color = Muted, fontSize = 11.sp)
        Text("3. Explain: ${frame.lab.invariantPrompt}", color = Green, fontSize = 11.sp)
        Text(
            "4. Verify: ${if (frame.holds) "The invariant currently holds" else "Adjust the values until the invariant holds"}; residual ${proofValue(frame.residual)}.",
            color = if (frame.holds) Green else Amber,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ProofLiveEvidence(frame: com.indianservers.aiexplorer.core.ProofFrame) {
    fun displayValue(name: String, value: Double): String = when {
        name == "inside interval" -> if (value >= .5) "YES" else "NO"
        name in setOf("|x|≤r", "−r≤x≤r", "selected left expression", "selected right expression", "same clock position", "n divides a−b") ->
            if (value >= .5) "TRUE" else "FALSE"
        name.startsWith("∠") || "angle" in name.lowercase() -> "${proofValue(value)}°"
        else -> proofValue(value)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xCC07131E))
            .border(1.dp, Violet.copy(alpha = .38f), RoundedCornerShape(14.dp))
            .padding(9.dp)
            .semantics {
                contentDescription = buildString {
                    append("Live proof values. ")
                    frame.parameters.forEach { (name, value) -> append("$name ${proofValue(value)}. ") }
                    frame.measurements.forEach { (name, value) -> append("$name ${displayValue(name, value)}. ") }
                    append(frame.invariant)
                }
            },
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("LIVE VALUES", color = Violet, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                Text("Drag the diagram or sliders—every value updates immediately.", color = Muted, fontSize = 10.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(frame.lab.evidenceType.label, color = Cyan, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (frame.holds) "✓ VERIFIED" else "ADJUST TO VERIFY",
                    color = if (frame.holds) Green else Amber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
        if (frame.parameters.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                frame.lab.parameters.forEach { parameter ->
                    val value = frame.parameters.getValue(parameter.name)
                    Text(
                        "${parameter.name} = ${proofValue(value)}",
                        color = Cyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(Cyan.copy(alpha = .09f))
                            .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            frame.measurements.forEach { (name, value) ->
                Text(
                    "$name = ${displayValue(name, value)}",
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(Violet.copy(alpha = .09f))
                        .border(1.dp, Violet.copy(alpha = .26f), RoundedCornerShape(9.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Green.copy(alpha = .09f))
                .padding(horizontal = 9.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("THEREFORE", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(frame.lab.formalResult, color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("residual ${proofValue(frame.residual)}", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ProofStageStrip(frame: com.indianservers.aiexplorer.core.ProofFrame) {
    val stages = listOf(
        Triple("GIVEN", frame.lab.steps.first(), Cyan),
        Triple("TRANSFORM", frame.lab.steps[frame.step], Amber),
        Triple("RESULT", frame.lab.formalResult, Green),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, (title, detail, color) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(11.dp)).background(color.copy(.11f)).border(1.dp, color.copy(.55f), RoundedCornerShape(11.dp)).padding(7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("${index + 1} · $title", color = color, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Text(detail, color = Ink, fontSize = 9.sp, maxLines = 2)
            }
            if (index < stages.lastIndex) Text("➜", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProofPathBreadcrumbs(frame: com.indianservers.aiexplorer.core.ProofFrame) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        frame.lab.steps.forEachIndexed { index, step ->
            val reached = index <= frame.step
            Text(
                "${index + 1}. ${step.substringBefore('.').take(18)}",
                color = if (reached) Ink else Muted,
                fontSize = 9.sp,
                fontWeight = if (index == frame.step) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (index == frame.step) Violet.copy(.24f) else SurfaceB.copy(.45f)).border(1.dp, if (reached) Violet.copy(.55f) else Grid, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 6.dp),
            )
            if (index < frame.lab.steps.lastIndex) Text("→", color = if (reached) Violet else Muted)
        }
    }
}

@Composable
private fun ProofReasoningLens(
    frame: com.indianservers.aiexplorer.core.ProofFrame,
    profile: com.indianservers.aiexplorer.core.ProofVisualProfile,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (com.indianservers.aiexplorer.core.ProofEnhancement.BeforeAfter in profile.features) {
            BeforeAfterProofDiagram(frame, profile)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Cyan.copy(.09f)).border(1.dp, Cyan.copy(.4f), RoundedCornerShape(12.dp)).padding(9.dp)) {
                Text("${frame.step + 1}. SYMBOLIC", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                MathFormulaText(frame.lab.formalResult, color = Ink, fontSize = 14.sp)
                Text("Values: ${frame.parameters.entries.joinToString { "${it.key}=${trim(it.value)}" }.ifBlank { "logical regions" }}", color = Muted, fontSize = 9.sp)
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Amber.copy(.09f)).border(1.dp, Amber.copy(.4f), RoundedCornerShape(12.dp)).padding(9.dp)) {
                Text("${frame.step + 1}. VISUAL", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(frame.lab.steps[frame.step], color = Ink, fontSize = 11.sp)
                Text("Why valid: ${profile.validityNotes[frame.step.coerceAtMost(profile.validityNotes.lastIndex)]}", color = Green, fontSize = 9.sp)
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Green.copy(.09f)).border(1.dp, Green.copy(.55f), RoundedCornerShape(12.dp)).padding(10.dp)) {
            Text("CHECKPOINT ${frame.step + 1} · INVARIANT", color = Green, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            Text(frame.invariant, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(if (frame.holds) "Verified at current values · residual ${trim(frame.residual)}" else "Not yet contained · adjust the highlighted control · residual ${trim(frame.residual)}", color = if (frame.holds) Green else Amber, fontSize = 10.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProofMiniPanel("ASSUMPTIONS", profile.assumptions.joinToString(" · "), Cyan, Modifier.weight(1f))
            ProofMiniPanel("DOMAIN", profile.domainRestriction, Violet, Modifier.weight(1f))
        }
        ProofDefinitionsAndRepresentations(frame, profile)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProofMiniPanel("COUNTEREXAMPLE", profile.counterexample, Amber, Modifier.weight(1f))
            ProofMiniPanel("ERROR TRAP", profile.errorTrap, Color(0xFFFF6B7A), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProofMiniPanel("TEST A SIMPLE CASE", profile.simpleCase, Violet, Modifier.weight(1f))
            if (com.indianservers.aiexplorer.core.ProofEnhancement.ContradictionPanel in profile.features) {
                ProofMiniPanel("CONTRADICTION PATH", "Assume the conclusion is false → the independently evaluated representations would differ → this conflicts with ${frame.invariant}.", Amber, Modifier.weight(1f))
            }
        }
        profile.analogy?.let { ProofMiniPanel("REAL-WORLD ANALOGY", it, Cyan, Modifier.fillMaxWidth()) }
        ProofLegend()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Green.copy(.16f)).border(2.dp, Green, RoundedCornerShape(12.dp)).padding(11.dp)) {
            Text("THEREFORE · ${frame.lab.formalResult}", color = Green, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(profile.takeaway, color = Ink, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BeforeAfterProofDiagram(
    frame: com.indianservers.aiexplorer.core.ProofFrame,
    profile: com.indianservers.aiexplorer.core.ProofVisualProfile,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(SurfaceB.copy(.55f)).border(1.dp, Violet.copy(.48f), RoundedCornerShape(13.dp)).padding(8.dp)) {
        Text("BEFORE → SAME QUANTITY, DIFFERENT SHAPE → AFTER", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Canvas(Modifier.fillMaxWidth().height(128.dp).semantics { contentDescription = "Before and after comparison for ${frame.lab.title}; transparent overlay marks the invariant" }) {
            val leftCenter = Offset(size.width * .22f, size.height * .52f); val rightCenter = Offset(size.width * .78f, size.height * .52f)
            val before = Path().apply { moveTo(leftCenter.x - 58f, leftCenter.y + 38f); lineTo(leftCenter.x + 58f, leftCenter.y + 38f); lineTo(leftCenter.x + 22f, leftCenter.y - 42f); lineTo(leftCenter.x - 42f, leftCenter.y - 42f); close() }
            val after = Path().apply { moveTo(rightCenter.x - 55f, rightCenter.y + 38f); lineTo(rightCenter.x + 55f, rightCenter.y + 38f); lineTo(rightCenter.x + 55f, rightCenter.y - 42f); lineTo(rightCenter.x - 55f, rightCenter.y - 42f); close() }
            drawPath(before, Cyan.copy(.2f)); drawPath(before, Cyan, style = Stroke(4f))
            drawPath(after, Green.copy(.2f)); drawPath(after, Green, style = Stroke(4f))
            drawPath(after, Cyan.copy(.13f), style = Stroke(9f))
            drawLine(Violet, Offset(size.width * .38f, size.height * .5f), Offset(size.width * .62f, size.height * .5f), 5f, cap = StrokeCap.Round)
            val arrow = Offset(size.width * .62f, size.height * .5f); drawLine(Violet, arrow, arrow - Offset(18f, 13f), 5f); drawLine(Violet, arrow, arrow - Offset(18f, -13f), 5f)
            if (com.indianservers.aiexplorer.core.ProofEnhancement.MotionTrails in profile.features) repeat(3) { index -> drawLine(Violet.copy(.12f + index * .08f), Offset(size.width * (.39f + index * .035f), size.height * (.34f + index * .07f)), Offset(size.width * (.55f + index * .035f), size.height * (.34f + index * .07f)), 3f) }
        }
        Text("Invariant overlay: ${profile.invariant}", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        if (profile.notToScale) Text("Diagram is conceptual and not to scale.", color = Amber, fontSize = 9.sp)
    }
}

@Composable
private fun ProofDefinitionsAndRepresentations(
    frame: com.indianservers.aiexplorer.core.ProofFrame,
    profile: com.indianservers.aiexplorer.core.ProofVisualProfile,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceB.copy(.5f)).border(1.dp, Cyan.copy(.35f), RoundedCornerShape(12.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("DEFINITIONS BESIDE THE VISUAL", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        profile.definitions.entries.take(5).forEach { (symbol, definition) -> Text("$symbol · $definition", color = Ink, fontSize = 9.sp) }
        Text("EQUIVALENT REPRESENTATIONS", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf(
                "GRAPH" to "shape/position",
                "TABLE" to frame.measurements.entries.take(2).joinToString { "${it.key}=${trim(it.value)}" }.ifBlank { "same truth rows" },
                "FORMULA" to frame.lab.formalResult,
                "WORDS" to frame.invariant,
            ).forEach { (title, text) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Background.copy(.65f)).padding(6.dp)) {
                    Text(title, color = Violet, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(text, color = Ink, fontSize = 8.sp, maxLines = 3)
                }
            }
        }
    }
}

@Composable
private fun ProofMiniPanel(title: String, body: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(11.dp)).background(accent.copy(.08f)).border(1.dp, accent.copy(.42f), RoundedCornerShape(11.dp)).padding(9.dp)) {
        Text(title, color = accent, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        Text(body, color = Ink, fontSize = 9.sp)
    }
}

@Composable
private fun ProofLegend() {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(11.dp)).background(SurfaceB.copy(.5f)).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("LEGEND", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        listOf("● known" to Cyan, "● transform / unknown" to Amber, "● conclusion" to Green, "⇢ movement" to Violet, "▒ overlay = equal measure" to Muted).forEach { (label, color) -> Text(label, color = color, fontSize = 9.sp) }
    }
}
