package com.indianservers.aiexplorer

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
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
import com.indianservers.aiexplorer.core.AnalyticGeometry2D
import com.indianservers.aiexplorer.core.AnalyticGeometry3D
import com.indianservers.aiexplorer.core.AdvancedStatisticsEngine
import com.indianservers.aiexplorer.core.MathSpreadsheetEngine
import com.indianservers.aiexplorer.core.MissingDataPolicy
import com.indianservers.aiexplorer.core.Phase4Statistics
import com.indianservers.aiexplorer.core.RandomExperimentEngine
import com.indianservers.aiexplorer.core.RandomExperimentKind
import com.indianservers.aiexplorer.core.ConditionalProbabilityEngine
import com.indianservers.aiexplorer.core.CombinatoricsLab
import com.indianservers.aiexplorer.core.DynamicGeometryEngine
import com.indianservers.aiexplorer.core.DynamicGeometryDocument
import com.indianservers.aiexplorer.core.DynamicGeometryObject
import com.indianservers.aiexplorer.core.DynamicPoint
import com.indianservers.aiexplorer.core.DynamicPointRule
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
import com.indianservers.aiexplorer.input.CasPhotoMathRecognizer
import com.indianservers.aiexplorer.input.CasHandwritingRecognizer
import com.indianservers.aiexplorer.input.MathInkPoint
import com.indianservers.aiexplorer.core.MathAssumptionSet
import com.indianservers.aiexplorer.core.MathNumberDomain
import com.indianservers.aiexplorer.input.IntentAwareMathField
import com.indianservers.aiexplorer.input.IntentAwareMathValueField
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
import com.indianservers.aiexplorer.physics.mechanicalwaves.MechanicalWaveLabScreen
import com.indianservers.aiexplorer.chemistry.navigation.ChemistryFeatureRoot
import com.indianservers.aiexplorer.physics.formulas.navigation.PhysicsFormulaFeatureRoot
import com.indianservers.aiexplorer.physics.learning.PhysicsConnectedLearningFeature
import com.indianservers.aiexplorer.biology.navigation.BiologyFeatureRoot
import com.indianservers.aiexplorer.learningintelligence.ui.LearningIntelligenceFeatureRoot
import com.indianservers.aiexplorer.core.SolutionStepRole
import com.indianservers.aiexplorer.core.SymbolicCasEngine
import com.indianservers.aiexplorer.core.Solid
import com.indianservers.aiexplorer.core.SolidType
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
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
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
import kotlin.math.PI
import kotlin.math.abs
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

private val Ink = Color(0xFFEAF5FF)
private val Muted = Color(0xFFB8C4D8)
private val Background = Color(0xFF030507)
private val SurfaceA = Color(0xDD07101A)
private val SurfaceB = Color(0xBB0B1017)
private val Cyan = Color(0xFF20D9FF)
private val Violet = Color(0xFF985DFF)
private val Green = Color(0xFF48E0A4)
private val Amber = Color(0xFFFFC857)
private val Grid = Color(0x3341C8F5)

enum class PanelSlot { Left, Right, Bottom, Chrome }
enum class GeometryTool {
    Select, Point, Midpoint, PointOnObject, Intersection, Tangent, Centroid, Circumcenter, Incenter, Orthocenter,
    Line, Segment, Ray, Vector, Parallel, Perpendicular, AngleBisector,
    Triangle, Polygon, RegularPolygon, Rectangle, Square, Circle, CircleThreePoints, Arc, Ellipse, Measure,
}
enum class GraphTool { Plot, Trace, Tangent, Normal, Derivative, Integral, AreaBetween, BrushArea, SketchFit, Intersections, Extrema, Table, Data, Probability, ComplexPlane }
private enum class ProbabilityLabSection { Distributions, Statistics, Spreadsheet, Experiments, Learn }
enum class KnowledgeSection(val title: String) { Formulas("Formulas"), Mcqs("MCQs"), Visualize("Visualize Formulas"), Theorems("Theorems"), Proofs("Visual Proofs"), Dictionary("Maths Dictionary") }
private enum class StatisticsChartType(val label: String) { Histogram("Histogram"), BoxPlot("Box Plot"), DotPlot("Dot Plot"), Ecdf("ECDF"), NormalQq("Normal Q-Q") }
enum class SurfaceTool { Surface, Wireframe, Contours, Slice, Gradient, BoundingBox, Trace }
enum class SurfaceViewPreset { Isometric, X, Y, Z, XY, XZ, YZ }
enum class Transform3DMode { Move, Rotate, Scale }
enum class Selection3DMode { Object, Vertex, Edge, Face }
enum class CameraProjection { Perspective, Orthographic }
private data class SubObjectSelection(val solidIndex: Int, val mode: Selection3DMode, val index: Int)

data class AppSettings(
    val haptics: Boolean = true,
    val snap: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val spokenMath: Boolean = false,
    val graphSonification: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val decimalPrecision: Int = 2,
)

private data class SubjectOption(val title: String, val description: String, val symbol: String, val enabled: Boolean)
internal data class MathWorkspaceOption(val title: String, val description: String, val icon: String)
private data class PhysicsModuleOption(val title: String, val description: String, val icon: String)

private val SubjectOptions = listOf(
    SubjectOption("Maths", "Interactive mathematics laboratory", "∑", true),
    SubjectOption("Physics", "Mechanics, waves, fields and modern physics", "F", true),
    SubjectOption("Chemistry", "Elements, atoms, molecules and reactions", "Ch", true),
    SubjectOption("Biology", "Life from cells to ecosystems", "DNA", true),
    SubjectOption("Astro Physics", "Stars, space and cosmology", "✦", false),
    SubjectOption("IQ Labs", "Logic, patterns and reasoning", "IQ", false),
)

private val PhysicsModules = listOf(
    PhysicsModuleOption("Measurement", "Units, dimensions, errors and significant figures", "SI"),
    PhysicsModuleOption("Mechanics", "Particles, rigid bodies, equilibrium and dynamics", "M"),
    PhysicsModuleOption("Motion and Kinematics", "Position, velocity, acceleration and projectiles", "v"),
    PhysicsModuleOption("Force and Newton’s Laws", "Forces, free-body diagrams and Newton’s three laws", "F"),
    PhysicsModuleOption("Work, Energy and Power", "Energy transfer, conservation, work and efficiency", "W"),
    PhysicsModuleOption("Gravitation", "Orbits, gravitational fields, potential and satellites", "g"),
    PhysicsModuleOption("Oscillations", "Simple harmonic motion, damping and resonance", "SHM"),
    PhysicsModuleOption("Waves and Sound", "Wave motion, interference, standing waves and acoustics", "λ"),
    PhysicsModuleOption("Optics", "Reflection, refraction, lenses, diffraction and instruments", "Ray"),
    PhysicsModuleOption("Electricity", "Charge, fields, potential, current and circuits", "E"),
    PhysicsModuleOption("Magnetism", "Magnetic fields, forces, induction and electromagnetism", "B"),
    PhysicsModuleOption("Electronics", "Semiconductors, diodes, transistors and digital logic", "IC"),
    PhysicsModuleOption("Thermodynamics", "Heat, temperature, gases and thermodynamic laws", "T"),
    PhysicsModuleOption("Fluid Mechanics", "Pressure, buoyancy, continuity and fluid flow", "ρ"),
    PhysicsModuleOption("Modern Physics", "Relativity, quantum physics, atoms and nuclei", "Q"),
    PhysicsModuleOption("Astronomy and Astrophysics", "Stars, galaxies, cosmology and observational physics", "★"),
)

internal val MathCreationTools = listOf(
    MathWorkspaceOption("Unified Math Studio", "Linked algebra, graph, table, geometry and solver views", "Live"),
    MathWorkspaceOption("Explore Workspaces", "2D, 3D, graphing, trigonometry and spatial AR", "W"),
    MathWorkspaceOption("Scientific Calculator", "Scientific keypad, constants and conversions", "Sci"),
    MathWorkspaceOption("Math Notebook", "Named values, linked functions and reusable exact results", "#"),
    MathWorkspaceOption("Problem Solver", "Explainable, step-by-step answers with verification", "Fx"),
    MathWorkspaceOption("Shapes Explorer", "Construct and investigate interactive 2D shapes", "2D"),
    MathWorkspaceOption("Manipulatives", "Algebra tiles, fractions, balance and tactile labs", "Tiles"),
    MathWorkspaceOption("Probability & Statistics", "Distributions, intervals and probability plots", "Stat"),
)

internal val MathLearningTools = listOf(
    MathWorkspaceOption("Adaptive Math Coach", "Workspace-aware Socratic practice, proof checks and misconception repair", "AI"),
    MathWorkspaceOption("Formulas", "Searchable formula reference", "F"),
    MathWorkspaceOption("Visual Proofs", "Manipulable visual demonstrations", "Proof"),
    MathWorkspaceOption("Theorems", "Statements, conditions and applications", "Thm"),
    MathWorkspaceOption("Math Concepts", "A subject library spanning 27 branches of mathematics", "27"),
    MathWorkspaceOption("Visual Dictionary", "Terms, notation, diagrams and examples", "A-Z"),
    MathWorkspaceOption("MCQs", "Practice questions with explanations", "?"),
    MathWorkspaceOption("Formula Visualizer", "Turn formulas into interactive scenes", "View"),
    MathWorkspaceOption("Set Theory & Logic", "Interactive Venn diagrams, identities and truth tables", "∪∧"),
)

internal val SuggestedMathTools = listOf(
    MathWorkspaceOption("Daily Challenge", "A fresh problem and guided solution every day", "Day"),
    MathWorkspaceOption("Practice Paths", "Adaptive practice from foundations to mastery", "Path"),
    MathWorkspaceOption("Math Games", "Puzzles, patterns and strategy challenges", "Game"),
    MathWorkspaceOption("Exam Prep", "Timed topic tests and revision plans", "Test"),
    MathWorkspaceOption("Real-World Math", "Model money, motion, data and everyday decisions", "Life"),
    MathWorkspaceOption("Math History", "Discover ideas through mathematicians and milestones", "Time"),
)

private val MathConceptCategories = listOf(
    "Arithmetic", "Pre-Algebra", "Algebra", "Linear Algebra", "Geometry", "Coordinate Geometry",
    "Trigonometry", "Precalculus", "Calculus", "Multivariable Calculus", "Differential Equations",
    "Discrete Mathematics", "Number Theory", "Set Theory", "Logic", "Combinatorics",
    "Probability", "Statistics", "Numerical Methods", "Optimization", "Graph Theory",
    "Topology", "Complex Analysis", "Real Analysis", "Abstract Algebra", "Financial Mathematics",
    "Mathematical Modelling",
)

private data class ShapeExplorer2DPreset(
    val id: String,
    val label: String,
    val type: Shape2DType,
    val points: List<Vec2>,
    val formula: String,
)

private fun regularShapePoints(sides: Int, radius: Double = 2.4): List<Vec2> = (0 until sides).map { index ->
    val angle = -PI / 2 + index * 2 * PI / sides
    Vec2(cos(angle) * radius, sin(angle) * radius)
}

private val ShapeExplorer2DShapes = listOf(
    ShapeExplorer2DPreset("triangle", "Triangle", Shape2DType.Triangle, listOf(Vec2(-2.4, -1.6), Vec2(2.4, -1.6), Vec2(0.0, 2.2)), "A = 1/2 x b x h; P = a + b + c"),
    ShapeExplorer2DPreset("right-triangle", "Right Triangle", Shape2DType.Triangle, listOf(Vec2(-2.2, -1.7), Vec2(2.2, -1.7), Vec2(-2.2, 2.0)), "A = 1/2 x b x h; c^2 = a^2 + b^2"),
    ShapeExplorer2DPreset("equilateral", "Equilateral Triangle", Shape2DType.Triangle, regularShapePoints(3), "A = sqrt(3)a^2/4; P = 3a"),
    ShapeExplorer2DPreset("isosceles", "Isosceles Triangle", Shape2DType.Triangle, listOf(Vec2(-2.3, -1.7), Vec2(2.3, -1.7), Vec2(0.0, 2.4)), "A = bh/2; P = 2a+b"),
    ShapeExplorer2DPreset("scalene", "Scalene Triangle", Shape2DType.Triangle, listOf(Vec2(-2.6, -1.6), Vec2(2.1, -1.9), Vec2(.7, 2.3)), "A = sqrt(s(s-a)(s-b)(s-c))"),
    ShapeExplorer2DPreset("obtuse", "Obtuse Triangle", Shape2DType.Triangle, listOf(Vec2(-2.7, -1.4), Vec2(2.5, -1.4), Vec2(-1.4, 1.8)), "A = ab sin(C)/2"),
    ShapeExplorer2DPreset("square", "Square", Shape2DType.Square, listOf(Vec2(-2.0, -2.0), Vec2(2.0, 2.0)), "A = a^2; P = 4a; d = a sqrt(2)"),
    ShapeExplorer2DPreset("rectangle", "Rectangle", Shape2DType.Rectangle, listOf(Vec2(-2.6, -1.6), Vec2(2.6, 1.6)), "A = l x w; P = 2(l + w)"),
    ShapeExplorer2DPreset("parallelogram", "Parallelogram", Shape2DType.Polygon, listOf(Vec2(-2.6, -1.5), Vec2(1.5, -1.5), Vec2(2.6, 1.5), Vec2(-1.5, 1.5)), "A = b x h; P = 2(a + b)"),
    ShapeExplorer2DPreset("rhombus", "Rhombus", Shape2DType.Polygon, listOf(Vec2(0.0, -2.5), Vec2(2.2, 0.0), Vec2(0.0, 2.5), Vec2(-2.2, 0.0)), "A = d1 x d2 / 2; P = 4a"),
    ShapeExplorer2DPreset("trapezoid", "Trapezoid", Shape2DType.Polygon, listOf(Vec2(-2.8, -1.6), Vec2(2.8, -1.6), Vec2(1.7, 1.6), Vec2(-1.7, 1.6)), "A = (a + b)h / 2; P = a + b + c + d"),
    ShapeExplorer2DPreset("isosceles-trapezoid", "Isosceles Trapezoid", Shape2DType.Polygon, listOf(Vec2(-2.8, -1.6), Vec2(2.8, -1.6), Vec2(1.5, 1.6), Vec2(-1.5, 1.6)), "A = (a+b)h/2; equal legs"),
    ShapeExplorer2DPreset("kite", "Kite", Shape2DType.Polygon, listOf(Vec2(0.0, -2.7), Vec2(1.8, -0.2), Vec2(0.0, 2.4), Vec2(-1.8, -0.2)), "A = d1 x d2 / 2; P = 2(a + b)"),
    ShapeExplorer2DPreset("pentagon", "Pentagon", Shape2DType.Polygon, regularShapePoints(5), "A = 1/2 x apothem x perimeter"),
    ShapeExplorer2DPreset("hexagon", "Hexagon", Shape2DType.Polygon, regularShapePoints(6), "A = 3 sqrt(3)a^2/2; P = 6a"),
    ShapeExplorer2DPreset("heptagon", "Heptagon", Shape2DType.Polygon, regularShapePoints(7), "A = 7a^2 cot(pi/7)/4; P = 7a"),
    ShapeExplorer2DPreset("octagon", "Octagon", Shape2DType.Polygon, regularShapePoints(8), "A = 2(1 + sqrt(2))a^2; P = 8a"),
    ShapeExplorer2DPreset("decagon", "Decagon", Shape2DType.Polygon, regularShapePoints(10), "A = 5a^2 sqrt(5 + 2sqrt(5))/2; P = 10a"),
    ShapeExplorer2DPreset("nonagon", "Nonagon", Shape2DType.Polygon, regularShapePoints(9), "A = 9a^2 cot(pi/9)/4; P = 9a"),
    ShapeExplorer2DPreset("dodecagon", "Dodecagon", Shape2DType.Polygon, regularShapePoints(12), "A = 3(2+sqrt(3))a^2; P = 12a"),
    ShapeExplorer2DPreset("star", "Five-point Star", Shape2DType.Polygon, (0 until 10).map { i -> val r = if (i % 2 == 0) 2.5 else 1.05; val angle = -PI / 2 + i * PI / 5; Vec2(cos(angle) * r, sin(angle) * r) }, "Ten-vertex concave polygon"),
    ShapeExplorer2DPreset("arrow", "Arrow", Shape2DType.Polygon, listOf(Vec2(-2.7,-.7),Vec2(.4,-.7),Vec2(.4,-1.6),Vec2(2.8,0.0),Vec2(.4,1.6),Vec2(.4,.7),Vec2(-2.7,.7)), "Area by polygon decomposition"),
    ShapeExplorer2DPreset("circle", "Circle", Shape2DType.Circle, listOf(Vec2(0.0, 0.0), Vec2(2.4, 0.0)), "A = pi r^2; C = 2 pi r"),
    ShapeExplorer2DPreset("ellipse", "Ellipse", Shape2DType.Ellipse, listOf(Vec2(0.0, 0.0), Vec2(2.8, 0.0), Vec2(0.0, 1.7)), "A = pi ab; P is approximately pi[3(a+b)-sqrt((3a+b)(a+3b))]"),
)

private data class ShapeFormulaItem(val name: String, val expression: String)

private fun ShapeExplorer2DPreset.category(): String = when {
    type == Shape2DType.Circle || type == Shape2DType.Ellipse -> "Curves"
    label.contains("Triangle") -> "Triangles"
    label in setOf("Square", "Rectangle", "Parallelogram", "Rhombus", "Trapezoid", "Kite") -> "Quadrilaterals"
    else -> "Polygons"
}

private fun shape2DFormulaLibrary(label: String): List<ShapeFormulaItem> = when (label.substringBefore(" Copy")) {
    "Triangle" -> listOf(ShapeFormulaItem("Area: base-height", "A = bh/2"), ShapeFormulaItem("Area: Heron", "A = sqrt(s(s-a)(s-b)(s-c))"), ShapeFormulaItem("Semiperimeter", "s = (a+b+c)/2"), ShapeFormulaItem("Perimeter", "P = a+b+c"), ShapeFormulaItem("Inradius", "r = A/s"), ShapeFormulaItem("Circumradius", "R = abc/(4A)"))
    "Right Triangle" -> listOf(ShapeFormulaItem("Area", "A = ab/2"), ShapeFormulaItem("Pythagoras", "c² = a²+b²"), ShapeFormulaItem("Perimeter", "P = a+b+c"), ShapeFormulaItem("Inradius", "r = (a+b-c)/2"), ShapeFormulaItem("Circumradius", "R = c/2"))
    "Equilateral Triangle" -> listOf(ShapeFormulaItem("Area", "A = sqrt(3)a²/4"), ShapeFormulaItem("Perimeter", "P = 3a"), ShapeFormulaItem("Height", "h = sqrt(3)a/2"), ShapeFormulaItem("Inradius", "r = sqrt(3)a/6"), ShapeFormulaItem("Circumradius", "R = sqrt(3)a/3"))
    "Square" -> listOf(ShapeFormulaItem("Area", "A = a²"), ShapeFormulaItem("Perimeter", "P = 4a"), ShapeFormulaItem("Diagonal", "d = a sqrt(2)"), ShapeFormulaItem("Inradius", "r = a/2"), ShapeFormulaItem("Circumradius", "R = a/sqrt(2)"))
    "Rectangle" -> listOf(ShapeFormulaItem("Area", "A = lw"), ShapeFormulaItem("Perimeter", "P = 2(l+w)"), ShapeFormulaItem("Diagonal", "d = sqrt(l²+w²)"), ShapeFormulaItem("Circumradius", "R = d/2"))
    "Parallelogram" -> listOf(ShapeFormulaItem("Area", "A = bh"), ShapeFormulaItem("Vector area", "A = |a x b|"), ShapeFormulaItem("Perimeter", "P = 2(a+b)"), ShapeFormulaItem("Diagonals", "p²+q² = 2(a²+b²)"))
    "Rhombus" -> listOf(ShapeFormulaItem("Area: diagonals", "A = d1d2/2"), ShapeFormulaItem("Area: base-height", "A = ah"), ShapeFormulaItem("Perimeter", "P = 4a"), ShapeFormulaItem("Diagonal identity", "d1²+d2² = 4a²"), ShapeFormulaItem("Inradius", "r = A/(2a)"))
    "Trapezoid" -> listOf(ShapeFormulaItem("Area", "A = (a+b)h/2"), ShapeFormulaItem("Midsegment", "m = (a+b)/2"), ShapeFormulaItem("Perimeter", "P = a+b+c+d"))
    "Kite" -> listOf(ShapeFormulaItem("Area", "A = d1d2/2"), ShapeFormulaItem("Perimeter", "P = 2(a+b)"), ShapeFormulaItem("Diagonal relation", "d1 is perpendicular to d2"))
    "Circle" -> listOf(ShapeFormulaItem("Area", "A = pi r²"), ShapeFormulaItem("Circumference", "C = 2 pi r = pi d"), ShapeFormulaItem("Diameter", "d = 2r"), ShapeFormulaItem("Arc length", "L = r theta"), ShapeFormulaItem("Sector area", "Asector = r² theta/2"), ShapeFormulaItem("Chord", "c = 2r sin(theta/2)"))
    "Ellipse" -> listOf(ShapeFormulaItem("Area", "A = pi ab"), ShapeFormulaItem("Perimeter approximation", "P ≈ pi[3(a+b)-sqrt((3a+b)(a+3b))]"), ShapeFormulaItem("Focal distance", "c = sqrt(a²-b²)"), ShapeFormulaItem("Eccentricity", "e = c/a"), ShapeFormulaItem("Foci property", "PF1+PF2 = 2a"))
    else -> listOf(ShapeFormulaItem("Area", "A = ns²/[4 tan(pi/n)]"), ShapeFormulaItem("Perimeter", "P = ns"), ShapeFormulaItem("Interior angle", "alpha = (n-2)180°/n"), ShapeFormulaItem("Angle sum", "S = (n-2)180°"), ShapeFormulaItem("Apothem", "a = s/[2 tan(pi/n)]"), ShapeFormulaItem("Circumradius", "R = s/[2 sin(pi/n)]"), ShapeFormulaItem("Diagonals", "D = n(n-3)/2"))
}

private fun solidFormulaLibrary(type: SolidType): List<ShapeFormulaItem> = when (type) {
    SolidType.Cube -> listOf(ShapeFormulaItem("Volume", "V = a³"), ShapeFormulaItem("Surface area", "S = 6a²"), ShapeFormulaItem("Lateral area", "L = 4a²"), ShapeFormulaItem("Face diagonal", "d = a sqrt(2)"), ShapeFormulaItem("Space diagonal", "D = a sqrt(3)"))
    SolidType.Cuboid -> listOf(ShapeFormulaItem("Volume", "V = lwh"), ShapeFormulaItem("Surface area", "S = 2(lw+lh+wh)"), ShapeFormulaItem("Lateral area", "L = 2h(l+w)"), ShapeFormulaItem("Space diagonal", "D = sqrt(l²+w²+h²)"))
    SolidType.Sphere -> listOf(ShapeFormulaItem("Volume", "V = 4 pi r³/3"), ShapeFormulaItem("Surface area", "S = 4 pi r²"), ShapeFormulaItem("Great-circle area", "A = pi r²"), ShapeFormulaItem("Circumference", "C = 2 pi r"))
    SolidType.Hemisphere -> listOf(ShapeFormulaItem("Volume", "V = 2 pi r³/3"), ShapeFormulaItem("Curved area", "C = 2 pi r²"), ShapeFormulaItem("Total area", "S = 3 pi r²"), ShapeFormulaItem("Base area", "B = pi r²"))
    SolidType.Cylinder -> listOf(ShapeFormulaItem("Volume", "V = pi r²h"), ShapeFormulaItem("Curved area", "C = 2 pi rh"), ShapeFormulaItem("Total area", "S = 2 pi r(r+h)"), ShapeFormulaItem("Base area", "B = pi r²"), ShapeFormulaItem("Axial diagonal", "d = sqrt(h²+4r²)"))
    SolidType.Cone -> listOf(ShapeFormulaItem("Volume", "V = pi r²h/3"), ShapeFormulaItem("Slant height", "s = sqrt(r²+h²)"), ShapeFormulaItem("Curved area", "C = pi rs"), ShapeFormulaItem("Total area", "S = pi r(r+s)"), ShapeFormulaItem("Base area", "B = pi r²"))
    SolidType.Frustum -> listOf(ShapeFormulaItem("Volume", "V = pi h(R²+Rr+r²)/3"), ShapeFormulaItem("Slant height", "s = sqrt(h²+(R-r)²)"), ShapeFormulaItem("Curved area", "C = pi(R+r)s"), ShapeFormulaItem("Total area", "S = C+pi(R²+r²)"))
    SolidType.Pyramid -> listOf(ShapeFormulaItem("Volume", "V = Bh/3"), ShapeFormulaItem("Lateral area", "L = ps/2"), ShapeFormulaItem("Surface area", "S = B+L"), ShapeFormulaItem("Square slant height", "s = sqrt(h²+(a/2)²)"))
    SolidType.TriangularPrism -> listOf(ShapeFormulaItem("Volume", "V = Bh"), ShapeFormulaItem("Lateral area", "L = ph"), ShapeFormulaItem("Surface area", "S = 2B+ph"))
    SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.OctagonalPrism -> listOf(ShapeFormulaItem("Volume", "V = Bh"), ShapeFormulaItem("Lateral area", "L = ph"), ShapeFormulaItem("Surface area", "S = 2B+ph"), ShapeFormulaItem("Regular base", "B = ns²/[4 tan(pi/n)]"))
    SolidType.Tetrahedron -> listOf(ShapeFormulaItem("Volume", "V = a³/(6 sqrt(2))"), ShapeFormulaItem("Surface area", "S = sqrt(3)a²"), ShapeFormulaItem("Height", "h = a sqrt(2/3)"), ShapeFormulaItem("Inradius", "r = a sqrt(6)/12"))
    SolidType.TriangularPyramid -> listOf(ShapeFormulaItem("Volume", "V = Bh/3"), ShapeFormulaItem("Base", "B = bh/2"), ShapeFormulaItem("Faces", "F = 4"))
    SolidType.Octahedron -> listOf(ShapeFormulaItem("Volume", "V = sqrt(2)a³/3"), ShapeFormulaItem("Surface area", "S = 2 sqrt(3)a²"), ShapeFormulaItem("Inradius", "r = a sqrt(6)/6"), ShapeFormulaItem("Circumradius", "R = a sqrt(2)/2"))
    SolidType.Wedge -> listOf(ShapeFormulaItem("Volume", "V = bhl/2"), ShapeFormulaItem("Triangle base", "B = bh/2"), ShapeFormulaItem("Surface area", "S = bh+l(b+h+s)"))
    SolidType.Torus -> listOf(ShapeFormulaItem("Volume", "V = 2 pi²Rr²"), ShapeFormulaItem("Surface area", "S = 4 pi²Rr"), ShapeFormulaItem("Outer diameter", "Dout = 2(R+r)"), ShapeFormulaItem("Inner diameter", "Din = 2(R-r)"))
    SolidType.Ellipsoid -> listOf(ShapeFormulaItem("Volume", "V = 4 pi abc/3"), ShapeFormulaItem("Surface approximation", "S ≈ 4 pi[(a^p b^p+a^p c^p+b^p c^p)/3]^(1/p)"), ShapeFormulaItem("Sphere case", "a=b=c=r"))
    SolidType.Paraboloid -> listOf(ShapeFormulaItem("Volume", "V = pi r²h/2"), ShapeFormulaItem("Base area", "B = pi r²"), ShapeFormulaItem("Curved area", "C = pi r[(r²+4h²)^(3/2)-r³]/(6h²)"))
    SolidType.Capsule -> listOf(ShapeFormulaItem("Volume", "V = pi r²l+4 pi r³/3"), ShapeFormulaItem("Surface area", "S = 2 pi rl+4 pi r²"), ShapeFormulaItem("Total height", "h = l+2r"))
}

private data class PointGesture(
    val indices: List<Int>,
    val from: List<Vec2>,
)

private data class SolidGesture(val index: Int, val from: Solid)
private data class SolidGroupGesture(val indices: Set<Int>, val from: List<Solid>)
private data class VectorGesture(val index: Int, val from: Vector3D)
data class SavedWorkspace(
    val id: String,
    val name: String,
    val module: MathModule,
    val snapshot: WorkspaceState,
    val json: String,
    val updatedAt: Long,
)

private val LearningActivities = LearningCatalog.lessons

private fun GeometryTool.requiredTapCount(): Int = when (this) {
    GeometryTool.Point -> 1
    GeometryTool.Midpoint -> 2
    GeometryTool.Line, GeometryTool.Segment, GeometryTool.Ray, GeometryTool.Vector,
    GeometryTool.Rectangle, GeometryTool.Square, GeometryTool.Circle, GeometryTool.RegularPolygon -> 2
    GeometryTool.PointOnObject, GeometryTool.Tangent, GeometryTool.Centroid, GeometryTool.Circumcenter, GeometryTool.Incenter, GeometryTool.Orthocenter,
    GeometryTool.Parallel, GeometryTool.Perpendicular, GeometryTool.AngleBisector,
    GeometryTool.Triangle, GeometryTool.CircleThreePoints, GeometryTool.Arc, GeometryTool.Ellipse -> 3
    GeometryTool.Polygon -> 4
    GeometryTool.Intersection -> 4
    GeometryTool.Select, GeometryTool.Measure -> 0
}

private fun GeometryTool.toShape2DType(): Shape2DType? = when (this) {
    GeometryTool.Line -> Shape2DType.Line
    GeometryTool.Segment -> Shape2DType.Segment
    GeometryTool.Ray -> Shape2DType.Ray
    GeometryTool.Vector -> Shape2DType.Vector
    GeometryTool.Parallel -> Shape2DType.Parallel
    GeometryTool.Perpendicular -> Shape2DType.Perpendicular
    GeometryTool.AngleBisector -> Shape2DType.AngleBisector
    GeometryTool.Triangle -> Shape2DType.Triangle
    GeometryTool.Polygon -> Shape2DType.Polygon
    GeometryTool.RegularPolygon -> Shape2DType.RegularPolygon
    GeometryTool.Rectangle -> Shape2DType.Rectangle
    GeometryTool.Square -> Shape2DType.Square
    GeometryTool.Circle -> Shape2DType.Circle
    GeometryTool.CircleThreePoints -> Shape2DType.CircleThreePoints
    GeometryTool.Arc -> Shape2DType.Arc
    GeometryTool.Ellipse -> Shape2DType.Ellipse
    else -> null
}

private fun GeometryTool.toPointDependencyType(): PointDependencyType? = when (this) {
    GeometryTool.Midpoint -> PointDependencyType.Midpoint
    GeometryTool.Centroid -> PointDependencyType.Centroid
    GeometryTool.Circumcenter -> PointDependencyType.Circumcenter
    GeometryTool.Incenter -> PointDependencyType.Incenter
    GeometryTool.Orthocenter -> PointDependencyType.Orthocenter
    GeometryTool.Intersection -> PointDependencyType.Intersection
    GeometryTool.PointOnObject -> PointDependencyType.PointOnObject
    GeometryTool.Tangent -> PointDependencyType.TangentPoint
    else -> null
}

private fun defaultSolid(type: SolidType): Solid = when (type) {
    SolidType.Cube -> Solid(type, width = 2.0)
    SolidType.Cuboid -> Solid(type, width = 2.4, height = 1.6, depth = 1.4, radius = .8)
    SolidType.Sphere -> Solid(type, width = 2.0, height = 2.0, depth = 2.0, radius = 1.0)
    SolidType.Hemisphere -> Solid(type, width = 2.0, height = 1.0, depth = 2.0, radius = 1.0)
    SolidType.Cylinder -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = .9)
    SolidType.Cone -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = .9)
    SolidType.Frustum -> Solid(type, width = 2.0, height = 2.4, depth = 2.0, radius = 1.0, topRadius = .55)
    SolidType.Pyramid -> Solid(type, width = 2.2, height = 2.4, depth = 2.2, radius = .9)
    SolidType.TriangularPrism -> Solid(type, width = 2.2, height = 2.0, depth = 2.6)
    SolidType.PentagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.HexagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.OctagonalPrism -> Solid(type, width = 2.0, height = 2.3, depth = 2.0, radius = 1.0)
    SolidType.Tetrahedron -> Solid(type, width = 2.4)
    SolidType.TriangularPyramid -> Solid(type, width = 2.4, height = 2.6, depth = 2.2)
    SolidType.Octahedron -> Solid(type, width = 2.4)
    SolidType.Wedge -> Solid(type, width = 2.5, height = 1.8, depth = 2.4)
    SolidType.Torus -> Solid(type, width = 1.15, height = 1.0, depth = 1.0, radius = .42)
    SolidType.Ellipsoid -> Solid(type, width = 2.6, height = 1.7, depth = 2.0, radius = 1.0)
    SolidType.Paraboloid -> Solid(type, width = 2.0, height = 2.5, depth = 2.0, radius = 1.15)
    SolidType.Capsule -> Solid(type, width = 2.0, height = 3.0, depth = 2.0, radius = .8)
}

class MainActivity : ComponentActivity() {
    private lateinit var reliabilityMonitor: LocalReliabilityMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reliabilityMonitor = LocalReliabilityMonitor(this)
        setContent { AIExplorerApp() }
    }

    override fun onStart() {
        super.onStart()
        reliabilityMonitor.beginSession()
    }

    override fun onStop() {
        reliabilityMonitor.endSession()
        super.onStop()
    }
}

class ExplorerViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val history = CommandHistory()
    private val notebookEngine = MathNotebookEngine()
    private val linkedMathKernel = LinkedMathKernel()
    private val mathObjectGraph = MathObjectGraph()
    private val universalDocumentEngine = UniversalMathDocumentEngine()
    private val trustedMathKernel = TrustedMathKernel()
    private val learningQueue = OfflineLearningQueue()
    private var pointGesture: PointGesture? = null
    private var solidGesture: SolidGesture? = null
    private var solidGroupGesture: SolidGroupGesture? = null
    private var vectorGesture: VectorGesture? = null
    private var spatialGestureFrom: com.indianservers.aiexplorer.spatial.SpatialScenePlacement? = null
    var state by mutableStateOf(WorkspaceState())
        private set
    var selectedPoint by mutableIntStateOf(1)
        private set
    var selectedShape by mutableIntStateOf(-1)
        private set
    var selectedShapes by mutableStateOf<Set<Int>>(emptySet())
        private set
    var status by mutableStateOf("Ready")
        private set
    var showLeftPanel by mutableStateOf(false)
        private set
    var showRightPanel by mutableStateOf(false)
        private set
    var showBottomPanel by mutableStateOf(false)
        private set
    var showChrome by mutableStateOf(true)
        private set
    var showLearningPanel by mutableStateOf(false)
        private set
    var showSubjectHub by mutableStateOf(true)
        private set
    var showPhysicsHub by mutableStateOf(false)
        private set
    var showChemistryHub by mutableStateOf(false)
        private set
    var showBiologyHub by mutableStateOf(false)
        private set
    var showLearningIntelligence by mutableStateOf(false)
        private set
    var showMathLanding by mutableStateOf(false)
        private set
    var showMathMenu by mutableStateOf(false)
        private set
    var showShapesExplorer by mutableStateOf(false)
        private set
    var shapeExplorerScene by mutableStateOf(false)
        private set
    var showProblemSolver by mutableStateOf(false)
        private set
    var showScientificCalculator by mutableStateOf(false)
        private set
    var showSetLogicVisualizer by mutableStateOf(false)
        private set
    var showMathNotebook by mutableStateOf(false)
        private set
    var showUnifiedMathStudio by mutableStateOf(false)
        private set
    var showAdaptiveMathLearning by mutableStateOf(false)
        private set
    var showProbabilityLab by mutableStateOf(false)
        private set
    var requestedProbabilitySection by mutableIntStateOf(0)
        private set
    var showKnowledgeHub by mutableStateOf(false)
        private set
    var activeKnowledgeSection by mutableStateOf(KnowledgeSection.Formulas)
        private set
    var pinnedMathTools by mutableStateOf(savedStateHandle.get<ArrayList<String>>("pinnedMathTools").orEmpty().toSet())
        private set
    var recentMathTools by mutableStateOf(savedStateHandle.get<ArrayList<String>>("recentMathTools").orEmpty().toList())
        private set
    var mathToolUseCounts by mutableStateOf(
        savedStateHandle.get<ArrayList<String>>("mathToolUseCounts").orEmpty().mapNotNull { encoded ->
            val split = encoded.lastIndexOf('=')
            if (split <= 0) null else encoded.substring(0, split) to (encoded.substring(split + 1).toIntOrNull() ?: 0)
        }.toMap(),
    )
        private set
    var favoriteShapeKeys by mutableStateOf<Set<String>>(emptySet())
        private set
    var recentShapeKeys by mutableStateOf<List<String>>(emptyList())
        private set
    var notebookDocument by mutableStateOf(
        savedStateHandle.get<ArrayList<String>>("notebookInputs").orEmpty().fold(MathNotebookDocument()) { document, input ->
            runCatching { notebookEngine.submit(document, input) }.getOrDefault(document)
        },
    )
        private set
    var showActionDock by mutableStateOf(false)
        private set
    var geometryTool by mutableStateOf(GeometryTool.Select)
        private set
    var selectedSolid by mutableIntStateOf(0)
        private set
    var selectedVector3D by mutableIntStateOf(0)
        private set
    var pendingConstruction by mutableStateOf<List<Vec2>>(emptyList())
        private set
    private var pendingPointIndices by mutableStateOf<List<Int?>>(emptyList())
    var activeActivityId by mutableStateOf(LearningActivities.first().id)
        private set
    var lessonProgress by mutableStateOf<Map<String, LearnerProgress>>(emptyMap())
        private set
    var learningRole by mutableStateOf(LearningRole.Learner)
        private set
    var assignments by mutableStateOf<List<Assignment>>(LearningCatalog.defaultAssignments)
        private set
    var savedWorkspaces by mutableStateOf<List<SavedWorkspace>>(emptyList())
        private set
    var settings by mutableStateOf(AppSettings())
        private set
    var lastValidation by mutableStateOf(LearningValidation(false, "Start an activity and validate your construction."))
        private set
    var lastPackageValidation by mutableStateOf(PackageValidation(true, "Package ready for validation.", LearningPackage.schemaVersion))
        private set

    val activeActivity: LearningActivity
        get() = LearningActivities.firstOrNull { it.id == activeActivityId } ?: LearningActivities.first()
    val completedActivities: Set<String>
        get() = lessonProgress.filterValues { it.status == ProgressStatus.Completed }.keys
    val activeProgress: LearnerProgress?
        get() = lessonProgress[activeActivityId]
    val teacherSummary
        get() = ClassroomEngine.summarize(assignments.first(), LearningActivities, lessonProgress)
    val pendingLearningOperations: Int
        get() = learningQueue.pending().size
    val linkedMathSnapshot
        get() = linkedMathKernel.snapshot(state)
    val mathObjectGraphSnapshot
        get() = mathObjectGraph.snapshot(state)
    fun mathObjectGraphSnapshot(parameterValues: Map<String, Double>) = mathObjectGraph.snapshot(state, parameterValues)
    val universalMathDocument: UniversalMathDocument
        get() {
            val base = UniversalWorkspaceBridge.fromWorkspace(state)
            val notebookObjects = UniversalMathObjectFactory.fromNotebook(notebookDocument)
            val probability = UniversalMathObjectFactory.probability("probability-normal", "Normal distribution", "Normal", mapOf("mean" to 0.0, "sd" to 1.0))
            return base.copy(objects = base.objects + notebookObjects.associateBy { it.id } + (probability.id to probability), revision = maxOf(base.revision, notebookDocument.revision.toLong()))
        }
    fun verifyMathEquivalence(left: String, right: String): EquivalenceEvidence = trustedMathKernel.equivalence(left, right)

    val constructionProtocol: List<String> get() = history.protocol

    fun open(module: MathModule) {
        state = state.copy(module = module)
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        hidePanels()
    }

    fun enterMaths() {
        returnToMathMenu()
        status = "Mathematics Menu"
    }

    fun returnToMathMenu() {
        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showMathLanding = true
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showMathMenu = true
        showChrome = true
        showActionDock = false
        hidePanels()
        status = "Mathematics Menu"
    }

    fun openPhysicsHub() {
        showSubjectHub = false
        showPhysicsHub = true
        showChemistryHub = false
        showBiologyHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Physics Explorer"
    }

    fun openChemistryHub() {
        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = true
        showBiologyHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Chemistry Lab"
    }

    fun openBiologyHub() {
        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = true
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Biology Explorer"
    }

    fun openShapesExplorer() {
        showSubjectHub = false
        showMathLanding = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showShapesExplorer = true
        shapeExplorerScene = false
        showActionDock = false
        hidePanels()
        status = "Shapes Explorer"
    }

    fun loadExplorerShape2D(id: String) {
        val preset = ShapeExplorer2DShapes.firstOrNull { it.id == id } ?: return
        state = state.copy(
            name = "${preset.label} Explorer",
            module = MathModule.Geometry2D,
            points = preset.points,
            shapes = listOf(Shape2D("shape-explorer-${preset.id}", preset.type, preset.points.indices.toList(), preset.label)),
            pointDependencies = emptyList(),
            modifiedAt = System.currentTimeMillis(),
        )
        selectedPoint = preset.points.lastIndex
        selectedShape = 0
        selectedShapes = setOf(0)
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("2d:$id")
        status = "${preset.label} loaded · drag its handles to resize"
    }

    fun loadExplorerShape3D(type: SolidType) {
        state = state.copy(
            name = "${type.name} Explorer",
            module = MathModule.Geometry3D,
            solids = listOf(defaultSolid(type)),
            vectors3D = emptyList(),
            modifiedAt = System.currentTimeMillis(),
        )
        selectedSolid = 0
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("3d:${type.name}")
        status = "${type.name} loaded · pinch or drag in Scale mode to resize"
    }

    fun toggleFavoriteShape(key: String) {
        favoriteShapeKeys = if (key in favoriteShapeKeys) favoriteShapeKeys - key else favoriteShapeKeys + key
        status = if (key in favoriteShapeKeys) "Shape added to favourites" else "Shape removed from favourites"
    }

    private fun rememberShape(key: String) {
        recentShapeKeys = (listOf(key) + recentShapeKeys.filterNot { it == key }).take(8)
    }

    fun addExplorerShape2D(id: String) {
        val preset = ShapeExplorer2DShapes.firstOrNull { it.id == id } ?: return
        val basePoints = if (state.module == MathModule.Geometry2D && shapeExplorerScene) state.points else emptyList()
        val baseShapes = if (state.module == MathModule.Geometry2D && shapeExplorerScene) state.shapes else emptyList()
        val offset = Vec2((baseShapes.size % 3) * 1.1, (baseShapes.size % 2) * .8)
        val newPoints = preset.points.map { it + offset }
        val start = basePoints.size
        val shape = Shape2D("shape-explorer-${preset.id}-${System.nanoTime()}", preset.type, newPoints.indices.map { start + it }, preset.label)
        state = state.copy(module = MathModule.Geometry2D, points = basePoints + newPoints, shapes = baseShapes + shape, pointDependencies = emptyList(), modifiedAt = System.currentTimeMillis())
        selectedShape = state.shapes.lastIndex
        selectedShapes = setOf(selectedShape)
        selectedPoint = state.points.lastIndex
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("2d:$id")
        status = "${preset.label} added to composite scene"
    }

    fun addExplorerShape3D(type: SolidType) {
        val base = if (state.module == MathModule.Geometry3D && shapeExplorerScene) state.solids else emptyList()
        val position = Vec3(((base.size % 4) - 1.5) * 2.2, 0.0, (base.size / 4) * 1.8)
        state = state.copy(module = MathModule.Geometry3D, solids = base + defaultSolid(type).copy(position = position), vectors3D = emptyList(), modifiedAt = System.currentTimeMillis())
        selectedSolid = state.solids.lastIndex
        showShapesExplorer = false
        shapeExplorerScene = true
        showChrome = true
        rememberShape("3d:${type.name}")
        status = "${type.name} added to composite scene"
    }

    fun duplicateExplorerSelection() {
        if (state.module == MathModule.Geometry2D) {
            val source = state.shapes.getOrNull(selectedShape) ?: return
            val sourcePoints = source.pointIndices.mapNotNull { state.points.getOrNull(it) }
            val start = state.points.size
            val copiedPoints = sourcePoints.map { it + Vec2(.7, .7) }
            val copy = source.copy(id = "${source.id}-copy-${System.nanoTime()}", pointIndices = copiedPoints.indices.map { start + it }, name = "${source.name} Copy")
            state = state.copy(points = state.points + copiedPoints, shapes = state.shapes + copy, modifiedAt = System.currentTimeMillis())
            selectedShape = state.shapes.lastIndex
            selectedShapes = setOf(selectedShape)
        } else {
            val source = state.solids.getOrNull(selectedSolid) ?: return
            state = state.copy(solids = state.solids + source.copy(position = source.position + Vec3(.8, 0.0, .8)), modifiedAt = System.currentTimeMillis())
            selectedSolid = state.solids.lastIndex
        }
        status = "Shape duplicated"
    }

    fun scaleExplorerShape2D(factor: Double) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) { status = "Unlock ${shape.name} before resizing"; return }
        val indices = shape.pointIndices.filter { it in state.points.indices }
        if (indices.isEmpty()) return
        val center = indices.map { state.points[it] }.let { points -> Vec2(points.map { it.x }.average(), points.map { it.y }.average()) }
        val replacements = indices.associateWith { index -> center + (state.points[index] - center) * factor.coerceIn(.2, 5.0) }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} resized"
    }

    fun resizeExplorerShape2D(width: Double, height: Double, keepProportions: Boolean) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) { status = "Unlock ${shape.name} before resizing"; return }
        val indices = shape.pointIndices.filter { it in state.points.indices }
        if (indices.isEmpty()) return
        val points = indices.map { state.points[it] }
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
        val currentWidth = (maxX - minX).coerceAtLeast(1e-6); val currentHeight = (maxY - minY).coerceAtLeast(1e-6)
        val sx = (width / currentWidth).coerceIn(.05, 20.0)
        val syRequested = (height / currentHeight).coerceIn(.05, 20.0)
        val sy = if (keepProportions) sx else syRequested
        val center = Vec2((minX + maxX) / 2, (minY + maxY) / 2)
        val replacements = indices.associateWith { index ->
            val delta = state.points[index] - center
            Vec2(center.x + delta.x * sx, center.y + delta.y * sy)
        }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} set to exact dimensions"
    }

    fun rotateExplorerShape2D(degrees: Double) {
        val shape = state.shapes.getOrNull(selectedShape) ?: return
        if (shape.locked) return
        val indices = shape.pointIndices.filter { it in state.points.indices }
        val center = indices.map { state.points[it] }.let { points -> Vec2(points.map { it.x }.average(), points.map { it.y }.average()) }
        val radians = Math.toRadians(degrees)
        val replacements = indices.associateWith { index ->
            val d = state.points[index] - center
            Vec2(center.x + d.x * cos(radians) - d.y * sin(radians), center.y + d.x * sin(radians) + d.y * cos(radians))
        }
        state = state.copy(points = state.points.mapIndexed { index, point -> replacements[index] ?: point }, modifiedAt = System.currentTimeMillis()).recomputed()
        status = "${shape.name} rotated"
    }

    fun renameExplorerSelection(name: String) {
        if (state.module != MathModule.Geometry2D || name.isBlank()) return
        updateSelectedShape { it.copy(name = name.trim()) }
    }

    fun resetExplorerSelection() {
        if (state.module == MathModule.Geometry2D) {
            val shape = state.shapes.getOrNull(selectedShape) ?: return
            val preset = ShapeExplorer2DShapes.firstOrNull { shape.id.contains("shape-explorer-${it.id}") } ?: return
            loadExplorerShape2D(preset.id)
        } else {
            val type = state.solids.getOrNull(selectedSolid)?.type ?: return
            loadExplorerShape3D(type)
        }
        status = "Shape reset"
    }

    fun openCurrentShapeInAr() {
        if (state.module != MathModule.Geometry3D || state.solids.isEmpty()) {
            status = "Choose a 3D solid before opening AR"
            return
        }
        shapeExplorerScene = false
        showShapesExplorer = false
        state = state.copy(module = MathModule.SpatialAR)
        status = "Shape prepared for AR placement"
    }

    fun openProblemSolver() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = true
        showScientificCalculator = false
        showMathNotebook = false
        showUnifiedMathStudio = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Explainable Problem Solver"
    }

    fun openProbabilityLab() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = true
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Probability & Distributions Lab"
    }

    fun openSpreadsheetLab() {
        requestedProbabilitySection = ProbabilityLabSection.Spreadsheet.ordinal
        openProbabilityLab()
        status = "Spreadsheet & Lists"
    }

    fun openSubjectHub() {
        showSubjectHub = true
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showLearningIntelligence = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        hidePanels()
        status = "Choose a subject"
    }

    fun openLearningIntelligence() {
        showSubjectHub = false
        showPhysicsHub = false
        showChemistryHub = false
        showBiologyHub = false
        showLearningIntelligence = true
        showMathLanding = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Local Learning Intelligence"
    }

    fun openMathNotebook() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = true
        showUnifiedMathStudio = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Unified Math Notebook"
    }

    fun openUnifiedMathStudio() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showUnifiedMathStudio = true
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Unified Math Studio"
    }

    fun openAdaptiveMathLearning() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showUnifiedMathStudio = false
        showAdaptiveMathLearning = true
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Adaptive Math Coach"
    }

    fun commitUnifiedStudio(workspace: WorkspaceState) {
        state = workspace
        status = "Linked maths document autosaved"
    }

    fun openKnowledgeHub(section: KnowledgeSection) {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = true
        activeKnowledgeSection = section
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Maths ${section.title}"
    }

    fun openScientificCalculator() {
        showSubjectHub = false
        showMathLanding = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = true
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Scientific Calculator"
    }

    fun openSetLogicVisualizer() {
        showSubjectHub = false
        showMathLanding = false
        showPhysicsHub = false
        showShapesExplorer = false
        shapeExplorerScene = false
        showProblemSolver = false
        showScientificCalculator = false
        showSetLogicVisualizer = true
        showMathNotebook = false
        showProbabilityLab = false
        showKnowledgeHub = false
        showMathMenu = false
        showActionDock = false
        hidePanels()
        status = "Set Theory & Logic Formula Visualizer"
    }

    fun submitNotebook(input: String) {
        notebookDocument = runCatching { notebookEngine.submit(notebookDocument, input) }
            .getOrElse { error ->
                status = error.message ?: "Notebook input was not accepted"
                return
            }
        status = if (notebookDocument.hasErrors) "Notebook updated with diagnostics" else "Notebook dependencies updated"
        persistNotebook()
    }

    fun removeNotebookCell(id: String) {
        notebookDocument = notebookEngine.remove(notebookDocument, id)
        persistNotebook()
        status = "Notebook cell removed"
    }

    fun clearNotebook() {
        notebookDocument = notebookEngine.clear()
        persistNotebook()
        status = "Notebook cleared"
    }

    private fun persistNotebook() {
        savedStateHandle["notebookInputs"] = ArrayList(notebookDocument.cells.map { it.input })
    }

    fun graphNotebookFunction(cell: NotebookCell) {
        val expression = cell.graphExpression ?: return
        val symbol = cell.symbol ?: "f"
        val existing = state.functions.indexOfFirst { it.name.substringBefore('(') == symbol }
        val color = listOf("cyan", "violet", "green", "amber")[(existing.takeIf { it >= 0 } ?: state.functions.size) % 4]
        val definition = com.indianservers.aiexplorer.core.FunctionDefinition(
            id = if (existing >= 0) state.functions[existing].id else "notebook-${cell.id}",
            name = "$symbol(x)",
            expression = expression,
            colorKey = color,
        )
        state = if (existing >= 0) history.execute(state, UpdateFunctionCommand(existing, state.functions[existing], definition))
        else history.execute(state, AddFunctionCommand(definition))
        open(MathModule.Graph2D)
        status = "$symbol(x) linked to Graph"
    }

    fun sendSolverToGraph(guided: GuidedSolution) {
        val payload = guided.handoffs.firstOrNull { it.destination == SolverDestination.Graph && it.enabled }?.payload ?: run {
            status = "This result does not have a graphable real expression"
            return
        }
        val index = state.functions.size
        val definition = com.indianservers.aiexplorer.core.FunctionDefinition(
            id = "solver-${System.currentTimeMillis()}",
            name = "s${index + 1}(x)",
            expression = payload,
            colorKey = listOf("cyan", "violet", "green", "amber")[index % 4],
        )
        state = history.execute(state, AddFunctionCommand(definition))
        open(MathModule.Graph2D)
        status = "Solver equation sent to Graph"
    }

    fun saveSolverToNotebook(guided: GuidedSolution) {
        val question = guided.solution.question
        notebookDocument = runCatching { notebookEngine.submit(notebookDocument, question) }.getOrElse {
            status = it.message ?: "Could not save the derivation"
            return
        }
        persistNotebook()
        openMathNotebook()
        status = "Verified solver result saved to Notebook"
    }

    fun sendSolverToTable(guided: GuidedSolution) {
        val graphable = guided.handoffs.any { it.destination == SolverDestination.Graph && it.enabled }
        if (graphable) sendSolverToGraph(guided) else status = "Table payload prepared from the verified result"
    }

    fun toggleMathMenu() {
        showMathMenu = !showMathMenu
        if (showMathMenu) hideWorkspacePanelsOnly()
    }

    fun recordMathToolOpen(title: String) {
        recentMathTools = (listOf(title) + recentMathTools.filterNot { it == title }).take(6)
        mathToolUseCounts = mathToolUseCounts + (title to ((mathToolUseCounts[title] ?: 0) + 1))
        savedStateHandle["recentMathTools"] = ArrayList(recentMathTools)
        savedStateHandle["mathToolUseCounts"] = ArrayList(mathToolUseCounts.map { "${it.key}=${it.value}" })
    }

    fun togglePinnedMathTool(title: String) {
        pinnedMathTools = if (title in pinnedMathTools) pinnedMathTools - title else pinnedMathTools + title
        savedStateHandle["pinnedMathTools"] = ArrayList(pinnedMathTools.sorted())
    }

    fun toggleActionDock() {
        showActionDock = !showActionDock
    }

    fun hideActionDock() {
        showActionDock = false
    }

    fun togglePanel(slot: PanelSlot) {
        val opening = when (slot) {
            PanelSlot.Left -> !showLeftPanel
            PanelSlot.Right -> !showRightPanel
            PanelSlot.Bottom -> !showBottomPanel
            PanelSlot.Chrome -> !showChrome
        }
        if (slot != PanelSlot.Chrome) hideWorkspacePanelsOnly()
        when (slot) {
            PanelSlot.Left -> showLeftPanel = opening
            PanelSlot.Right -> showRightPanel = opening
            PanelSlot.Bottom -> showBottomPanel = opening
            PanelSlot.Chrome -> showChrome = opening
        }
    }

    fun hidePanels() {
        showLeftPanel = false
        showRightPanel = false
        showBottomPanel = false
        showLearningPanel = false
    }

    fun hasDismissibleOverlay(): Boolean =
        showMathMenu || showActionDock || showLearningPanel || showLeftPanel || showRightPanel || showBottomPanel

    fun dismissTopOverlay() {
        when {
            showMathMenu -> showMathMenu = false
            showActionDock -> showActionDock = false
            showLearningPanel -> showLearningPanel = false
            showLeftPanel || showRightPanel || showBottomPanel -> hidePanels()
        }
    }

    fun dismissAllMenusAndPanels() {
        showMathMenu = false
        showActionDock = false
        showLearningPanel = false
        hidePanels()
    }

    fun toggleLearningPanel() {
        showLearningPanel = !showLearningPanel
        if (showLearningPanel) hideWorkspacePanelsOnly()
    }

    private fun hideWorkspacePanelsOnly() {
        showLeftPanel = false
        showRightPanel = false
        showBottomPanel = false
    }

    fun startActivity(activity: LearningActivity) {
        activeActivityId = activity.id
        state = state.copy(module = activity.module)
        showLearningPanel = true
        hideWorkspacePanelsOnly()
        geometryTool = if (activity.id == "triangle-angle-sum") GeometryTool.Triangle else geometryTool
        val now = System.currentTimeMillis()
        if (lessonProgress[activity.id] == null) {
            lessonProgress = lessonProgress + (activity.id to LearnerProgress(activity.id, ProgressStatus.InProgress, startedAt = now, updatedAt = now))
        }
        lastValidation = LearningValidation(false, activity.checkpoints.firstOrNull()?.instruction ?: activity.objective)
        status = "Activity: ${activity.title}"
    }

    fun validateActiveActivity(): LearningValidation {
        val result = validateActivity(activeActivity)
        val now = System.currentTimeMillis()
        learningQueue.enqueue(LearningOperation("attempt-$activeActivityId-$now", activeActivityId, LearningOperationType.Attempt, now))
        lessonProgress = lessonProgress + (
            activeActivityId to LearningEvaluator.recordAttempt(activeActivity, lessonProgress[activeActivityId], result, now)
        )
        lastValidation = result
        status = if (result.passed) "Validation passed" else result.message
        return result
    }

    fun completeActiveActivity() {
        val result = validateActiveActivity()
        if (result.passed) {
            val now = System.currentTimeMillis()
            learningQueue.enqueue(LearningOperation("complete-$activeActivityId-$now", activeActivityId, LearningOperationType.Complete, now))
            status = "Completed ${activeActivity.title}"
        }
    }

    private fun validateActivity(activity: LearningActivity): LearningValidation = LearningEvaluator.evaluate(activity, state)

    fun revealHint() {
        val current = lessonProgress[activeActivityId] ?: LearnerProgress(activeActivityId, ProgressStatus.InProgress)
        val hint = LearningEvaluator.nextHint(activeActivity, lastValidation, current.hintsUsed)
        val now = System.currentTimeMillis()
        lessonProgress = lessonProgress + (activeActivityId to current.copy(hintsUsed = current.hintsUsed + 1, updatedAt = now))
        learningQueue.enqueue(LearningOperation("hint-$activeActivityId-$now", activeActivityId, LearningOperationType.HintUsed, now))
        lastValidation = lastValidation.copy(message = hint)
        status = "Hint ${current.hintsUsed + 1} revealed"
    }

    fun switchLearningRole(role: LearningRole) {
        learningRole = role
        status = "${role.name} view"
    }

    fun selectGeometryTool(tool: GeometryTool) {
        geometryTool = tool
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        status = "${tool.name.lowercase().replaceFirstChar { it.uppercase() }} tool selected"
    }

    fun applyContextualGeometryTool(toolName: String, pointIndices: List<Int>) {
        val tool = runCatching { GeometryTool.valueOf(toolName) }.getOrNull() ?: return
        val required = tool.requiredTapCount()
        val inputs = pointIndices.take(required)
        if (required == 0 || inputs.size < required || inputs.any { it !in state.points.indices }) {
            geometryTool = tool
            pendingPointIndices = inputs.map<Int, Int?> { it }
            pendingConstruction = inputs.mapNotNull(state.points::getOrNull)
            status = "${tool.name}: ${inputs.size} selected · tap ${required - inputs.size} more point${if (required - inputs.size == 1) "" else "s"}"
            return
        }
        val dependencyType = tool.toPointDependencyType()
        if (dependencyType != null) {
            state = history.execute(state, AddDependentPointCommand(inputs, dependencyType))
            selectedPoint = state.points.lastIndex
            selectedShape = -1
            selectedShapes = emptySet()
        } else {
            val shapeType = tool.toShape2DType() ?: return selectGeometryTool(tool)
            state = history.execute(state, AddShapeFromPointsCommand(shapeType, inputs, tool.name))
            selectedShape = state.shapes.lastIndex
            selectedShapes = setOf(selectedShape)
            selectedPoint = -1
        }
        geometryTool = GeometryTool.Select
        pendingConstruction = emptyList()
        pendingPointIndices = emptyList()
        status = "Created ${tool.name.lowercase()} from the current selection"
    }

    fun applyGeometryConstraint(constraint: com.indianservers.aiexplorer.workspace.GeometryConstraint2D) {
        state = history.execute(state, AddGeometryConstraint2DCommand(constraint))
        val feedback = Geometry2DInteractionEngine.evaluateConstraint(state, constraint)
        status = "${constraint.type.label}: ${feedback.level.name.lowercase().replaceFirstChar { it.uppercase() }}"
    }

    fun removeGeometryConstraint(id: String) {
        if (state.geometryConstraints.none { it.id == id }) return
        state = state.copy(geometryConstraints = state.geometryConstraints.filterNot { it.id == id }, modifiedAt = System.currentTimeMillis())
        status = "Constraint removed"
    }

    fun addPoint(point: Vec2) {
        state = history.execute(state, AddPointCommand(point))
        selectedPoint = state.points.lastIndex
        status = "Added point ${selectedPoint + 1}"
    }

    fun handleGeometryTap(point: Vec2, hitPointIndex: Int?) {
        val snapped = hitPointIndex?.let(state.points::getOrNull) ?: if (settings.snap) {
            Vec2(round(point.x * 2.0) / 2.0, round(point.y * 2.0) / 2.0)
        } else point
        when (geometryTool) {
            GeometryTool.Select, GeometryTool.Measure -> return
            GeometryTool.Point -> {
                if (hitPointIndex != null) {
                    selectedPoint = hitPointIndex
                    status = "Selected point ${hitPointIndex + 1}"
                    return
                }
                state = history.execute(state, AddConstructionCommand(listOf(snapped), null))
                selectedPoint = state.points.lastIndex
                status = "Point placed"
            }
            else -> {
                val dependencyType = geometryTool.toPointDependencyType()
                if (dependencyType != null && hitPointIndex == null) {
                    status = "${geometryTool.name}: tap an existing point"
                    return
                }
                val next = pendingConstruction + snapped
                val nextIndices = pendingPointIndices + hitPointIndex
                val required = geometryTool.requiredTapCount()
                if (next.size >= required) {
                    if (dependencyType != null) {
                        val inputs = nextIndices.take(required).filterNotNull()
                        state = history.execute(state, AddDependentPointCommand(inputs, dependencyType))
                        selectedPoint = state.points.lastIndex
                    } else {
                        val shapeType = geometryTool.toShape2DType()
                        state = history.execute(
                            state,
                            AddConstructionCommand(next.take(required), shapeType, nextIndices.take(required)),
                        )
                        selectedShape = state.shapes.lastIndex
                        selectedShapes = setOf(selectedShape)
                    }
                    pendingConstruction = emptyList()
                    pendingPointIndices = emptyList()
                    status = "${geometryTool.name.lowercase().replaceFirstChar { it.uppercase() }} created"
                } else {
                    pendingConstruction = next
                    pendingPointIndices = nextIndices
                    status = "${geometryTool.name}: tap ${next.size + 1} of $required"
                }
            }
        }
    }

    fun movePoint(index: Int, point: Vec2) {
        val from = state.points[index]
        state = history.execute(state, MovePointCommand(index, from, point))
        selectedPoint = index
        status = "Moved point ${index + 1}"
    }

    fun beginPointDrag(index: Int) {
        val point = state.points.getOrNull(index) ?: return
        selectedShape = state.shapes.indexOfLast { it.visible && index in it.pointIndices }
        selectedShapes = selectedShape.takeIf { it >= 0 }?.let(::setOf) ?: emptySet()
        state.shapes.firstOrNull { it.locked && index in it.pointIndices }?.let {
            selectedPoint = index
            status = "${it.name} is locked"
            return
        }
        if (state.pointDependencies.any { it.outputIndex == index }) {
            selectedPoint = index
            status = "Dependent point: move its parent points"
            return
        }
        pointGesture = PointGesture(listOf(index), listOf(point))
        selectedPoint = index
        status = if (selectedShape >= 0) "Resize ${state.shapes[selectedShape].name} from junction" else "Move point ${index + 1}"
    }

    fun beginShapeDrag(shapeIndex: Int) {
        val shape = state.shapes.getOrNull(shapeIndex) ?: return
        selectedShape = shapeIndex
        if (shapeIndex !in selectedShapes) selectedShapes = setOf(shapeIndex)
        if (shape.locked) {
            status = "${shape.name} is locked"
            return
        }
        val indices = selectedShapes.mapNotNull(state.shapes::getOrNull).filterNot { it.locked }.flatMap { it.pointIndices }.distinct()
            .filterNot { index -> state.pointDependencies.any { it.outputIndex == index } }
        if (indices.isEmpty()) return
        pointGesture = PointGesture(indices, indices.mapNotNull(state.points::getOrNull))
    }

    fun previewPointDrag(index: Int, point: Vec2) {
        val gesture = pointGesture ?: return
        if (index !in gesture.indices) return
        state = state.copy(
            points = state.points.mapIndexed { i, old -> if (i == index) point else old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
        selectedPoint = index
    }

    fun previewShapeDrag(delta: Vec2) {
        val gesture = pointGesture ?: return
        val replacements = gesture.indices.zip(gesture.from.map { it + delta }).toMap()
        state = state.copy(
            points = state.points.mapIndexed { index, old -> replacements[index] ?: old },
            modifiedAt = System.currentTimeMillis(),
        ).recomputed()
    }

    fun previewShapeRotation(deltaDegrees: Double) {
        val gesture = pointGesture ?: return
        val center = InteractionGeometry.bounds(gesture.from)?.center ?: return
        val radians = Math.toRadians(deltaDegrees)
        val cosine = cos(radians); val sine = sin(radians)
        val replacements = gesture.indices.zip(gesture.from.map { point ->
            val local = point - center
            center + Vec2(local.x * cosine - local.y * sine, local.x * sine + local.y * cosine)
        }).toMap()
        state = state.copy(points = state.points.mapIndexed { index, old -> replacements[index] ?: old }, modifiedAt = System.currentTimeMillis()).recomputed()
    }

    fun endPointDrag() {
        val gesture = pointGesture ?: return
        val final = gesture.indices.mapNotNull(state.points::getOrNull)
        if (final != gesture.from) history.recordApplied(MovePointsCommand(gesture.indices, gesture.from, final))
        val resizedShape = selectedShape.takeIf { gesture.indices.size == 1 }?.let(state.shapes::getOrNull)
        pointGesture = null
        status = resizedShape?.let { "Resized ${it.name} from junction" } ?: "Moved object"
    }

    fun cancelPointDrag() {
        val gesture = pointGesture ?: return
        val replacements = gesture.indices.zip(gesture.from).toMap()
        state = state.copy(points = state.points.mapIndexed { index, old -> replacements[index] ?: old }).recomputed()
        pointGesture = null
        status = "Move cancelled"
    }

    fun selectShape(index: Int) {
        if (index !in state.shapes.indices) return
        selectedShape = index
        selectedShapes = setOf(index)
        selectedPoint = -1
        status = "Selected ${state.shapes[index].name}"
    }

    fun selectShapes(indices: Set<Int>) {
        selectedShapes = indices.filterTo(linkedSetOf()) { it in state.shapes.indices }
        selectedShape = selectedShapes.lastOrNull() ?: -1
        selectedPoint = -1
        status = if (selectedShapes.isEmpty()) "Selection cleared" else "Selected ${selectedShapes.size} object${if (selectedShapes.size == 1) "" else "s"}"
    }

    fun clearGeometrySelection() {
        selectedShapes = emptySet()
        selectedShape = -1
        selectedPoint = -1
        status = "Selection cleared"
    }

    fun updateSelectedShape(transform: (Shape2D) -> Shape2D) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        val from = state.shapes[index]
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, UpdateShapeCommand(index, from, to))
        status = "Updated ${to.name}"
    }

    fun reorderSelectedShape(toFront: Boolean) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        val shape = state.shapes[index]
        val reordered = state.shapes.toMutableList().apply { removeAt(index); if (toFront) add(shape) else add(0, shape) }
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReorderShapesCommand(state.shapes, reordered))
        selectedShape = if (toFront) state.shapes.lastIndex else 0
        selectedShapes = setOf(selectedShape)
        status = if (toFront) "Brought ${shape.name} to front" else "Sent ${shape.name} behind"
    }

    fun duplicateSelectedShape() {
        transformSelectedShape(PointDependencyType.Translate, listOf(.35, -.35))
        selectedShapes = selectedShape.takeIf { it in state.shapes.indices }?.let(::setOf) ?: emptySet()
        status = state.shapes.getOrNull(selectedShape)?.let { "Duplicated ${it.name}" } ?: status
    }

    fun exportSelectedGeometry(): String = buildString {
        appendLine("AIEXPLORER_GEOMETRY_2D_V1")
        selectedShapes.sorted().mapNotNull(state.shapes::getOrNull).forEach { shape ->
            val coordinates = shape.pointIndices.mapNotNull(state.points::getOrNull).joinToString(";") { "${it.x},${it.y}" }
            appendLine(listOf(shape.type.name, java.net.URLEncoder.encode(shape.name, "UTF-8"), shape.styleKey, coordinates).joinToString("|"))
        }
    }.trim()

    fun importGeometry(text: String) {
        if (!text.startsWith("AIEXPLORER_GEOMETRY_2D_V1")) { status = "Clipboard does not contain AI Explorer geometry"; return }
        val before = state.shapes.size
        text.lineSequence().drop(1).filter(String::isNotBlank).forEach { row ->
            val fields = row.split('|'); val type = fields.getOrNull(0)?.let { runCatching { Shape2DType.valueOf(it) }.getOrNull() } ?: return@forEach
            val values = fields.getOrNull(3)?.split(';').orEmpty().mapNotNull { pair -> pair.split(',').takeIf { it.size == 2 }?.let { Vec2(it[0].toDoubleOrNull() ?: return@mapNotNull null, it[1].toDoubleOrNull() ?: return@mapNotNull null) } }
            if (values.isEmpty()) return@forEach
            state = history.execute(state, AddConstructionCommand(values.map { it + Vec2(.35, -.35) }, type))
            val index = state.shapes.lastIndex; val shape = state.shapes[index]
            val name = fields.getOrNull(1)?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrNull() } ?: shape.name
            val style = fields.getOrNull(2) ?: shape.styleKey
            state = history.execute(state, UpdateShapeCommand(index, shape, shape.copy(name = "$name Copy", styleKey = style)))
        }
        selectedShapes = (before until state.shapes.size).toSet(); selectedShape = selectedShapes.lastOrNull() ?: -1
        status = if (selectedShapes.isEmpty()) "No geometry pasted" else "Pasted ${selectedShapes.size} object${if (selectedShapes.size == 1) "" else "s"}"
    }

    fun moveGeometrySelectionFromKeyboard(dx: Int, dy: Int, precision: Boolean) {
        if (selectedPoint in state.points.indices) {
            Geometry2DDirectManipulation.movePointByKeyboard(state, selectedPoint, dx, dy, precision)?.let { movePoint(selectedPoint, it) }
            return
        }
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        beginShapeDrag(index)
        val amount = if (precision) .01 else .1
        previewShapeDrag(Vec2(dx * amount, dy * amount))
        endPointDrag()
    }

    fun cycleGeometrySelection(backwards: Boolean = false) {
        if (state.shapes.isEmpty()) return
        val delta = if (backwards) -1 else 1
        selectShape(((selectedShape.takeIf { it >= 0 } ?: if (backwards) 0 else -1) + delta).mod(state.shapes.size))
    }

    fun freeSelectedDependentPoint() {
        val dependency = state.pointDependencies.firstOrNull { it.outputIndex == selectedPoint } ?: return
        state = history.execute(state, com.indianservers.aiexplorer.workspace.RemovePointDependencyCommand(dependency))
        status = "Converted ${dependency.name} to a free point"
    }

    fun nudgeSelectedDependencyParent() {
        val dependency = state.pointDependencies.firstOrNull { it.outputIndex == selectedPoint } ?: return
        val parent = dependency.inputIndices.lastOrNull { index -> state.pointDependencies.none { it.outputIndex == index } } ?: return
        movePoint(parent, state.points[parent] + Vec2(.0, .1))
        selectedPoint = dependency.outputIndex
        status = "Nudged a parent to repair ${dependency.name}"
    }

    fun groupSelectedShapes() {
        val ids = selectedShapes.mapNotNull(state.shapes::getOrNull).mapTo(linkedSetOf()) { it.id }
        if (ids.size < 2) return
        val group = com.indianservers.aiexplorer.workspace.GeometryGroup2D("group-${System.currentTimeMillis()}", "Group ${state.geometryGroups.size + 1}", ids)
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReplaceGeometryGroupsCommand(state.geometryGroups, state.geometryGroups + group))
        status = "Grouped ${ids.size} objects"
    }

    fun ungroupSelectedShapes() {
        val ids = selectedShapes.mapNotNull(state.shapes::getOrNull).map { it.id }.toSet()
        val groups = state.geometryGroups.filterNot { it.shapeIds == ids || it.shapeIds.any(ids::contains) }
        if (groups == state.geometryGroups) return
        state = history.execute(state, com.indianservers.aiexplorer.workspace.ReplaceGeometryGroupsCommand(state.geometryGroups, groups))
        status = "Group removed"
    }

    fun deleteSelectedShape() {
        val indices = selectedShapes.filterTo(linkedSetOf()) { it in state.shapes.indices }
            .ifEmpty { selectedShape.takeIf { it in state.shapes.indices }?.let(::setOf).orEmpty() }
        if (indices.isEmpty()) return
        val names = indices.mapNotNull(state.shapes::getOrNull).map { it.name }
        val anchor = indices.minOrNull() ?: 0
        state = history.execute(state, DeleteShapesCommand(indices, state.shapes, state.points, state.pointDependencies, state.geometryGroups, state.geometryConstraints))
        selectedShape = (anchor - 1).coerceAtMost(state.shapes.lastIndex)
        selectedShapes = selectedShape.takeIf { it >= 0 }?.let(::setOf) ?: emptySet()
        selectedPoint = -1
        status = "Deleted ${names.joinToString()}"
    }

    fun transformSelectedShape(type: PointDependencyType, parameters: List<Double> = emptyList()) {
        val index = selectedShape.takeIf { it in state.shapes.indices } ?: return
        state = history.execute(state, TransformShape2DCommand(index, type, parameters))
        selectedShape = state.shapes.lastIndex
        status = "${type.name} created"
    }

    fun editExpression(index: Int, expression: String) {
        state.functions.getOrNull(index) ?: return
        val from = state.functions[index].expression
        state = history.execute(state, EditExpressionCommand(index, from, expression))
        val validation = universalDocumentEngine.validate(universalMathDocument)
        status = if (validation.valid) "Expression updated through the universal maths document" else validation.diagnostics.first()
    }

    fun addFunction(expression: String = "sin(x)") {
        val index = state.functions.size
        val name = GraphEquationNames.next(state.functions.map { it.name }.toSet())
        val color = listOf("cyan", "violet", "green", "amber")[index % 4]
        state = history.execute(
            state,
            AddFunctionCommand(
                com.indianservers.aiexplorer.core.FunctionDefinition(
                    id = "function-${System.nanoTime()}-$index",
                    name = name,
                    expression = expression,
                    colorKey = color,
                ),
            ),
        )
        status = "Added $name"
    }

    fun updateFunction(index: Int, transform: (com.indianservers.aiexplorer.core.FunctionDefinition) -> com.indianservers.aiexplorer.core.FunctionDefinition) {
        val from = state.functions.getOrNull(index) ?: return
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, UpdateFunctionCommand(index, from, to))
        status = "Updated ${to.name}"
    }

    fun moveFunctionLayer(index: Int, delta: Int) {
        if (index !in state.functions.indices) return
        val target = (index + delta).coerceIn(state.functions.indices)
        if (target == index) return
        val reordered = state.functions.toMutableList().apply { add(target, removeAt(index)) }
        state = history.execute(state, ReorderFunctionsCommand(state.functions, reordered))
        status = "Graph layer reordered"
    }

    fun deleteFunction(index: Int) {
        val function = state.functions.getOrNull(index) ?: return
        state = history.execute(state, DeleteFunctionCommand(index, function, state.graphRowMetadata[function.id]))
        status = "Deleted ${function.name}"
    }

    fun updateGraphRowMetadata(rowId: String, transform: (GraphRowMetadataState) -> GraphRowMetadataState) {
        val from = state.graphRowMetadata[rowId]
        val to = transform(from ?: GraphRowMetadataState())
        if (from == to) return
        state = history.execute(state, UpdateGraphRowMetadataCommand(rowId, from, to))
        status = "Graph row organized"
    }

    fun updateGraphSliderMetadata(parameter: String, transform: (GraphSliderMetadataState) -> GraphSliderMetadataState) {
        val from = state.graphSliderMetadata[parameter]
        val to = transform(from ?: GraphSliderMetadataState())
        if (from == to) return
        state = history.execute(state, UpdateGraphSliderMetadataCommand(parameter, from, to))
        status = "Graph slider updated"
    }

    fun duplicateFunction(index: Int) {
        val function = state.functions.getOrNull(index) ?: return
        val nextIndex = state.functions.size
        val base = function.name.substringBefore('(').ifBlank { "f" }
        val name = "${base}${nextIndex + 1}(x)"
        val color = listOf("cyan", "violet", "green", "amber")[(nextIndex + 1) % 4]
        state = history.execute(
            state,
            AddFunctionCommand(
                function.copy(
                    id = "function-${System.currentTimeMillis()}-$nextIndex",
                    name = name,
                    colorKey = color,
                    visible = true,
                ),
            ),
        )
        status = "Duplicated ${function.name} as $name"
    }

    fun addSolid(type: SolidType) {
        val position = Vec3(((state.solids.size % 5) - 2) * 1.7, 0.0, ((state.solids.size / 5) - 1) * 1.6)
        val solid = defaultSolid(type).copy(position = position)
        state = history.execute(state, AddSolidCommand(solid))
        selectedSolid = state.solids.lastIndex
        status = "Added ${type.name}"
    }

    fun selectSolid(index: Int) {
        selectedSolid = index.coerceIn(0, state.solids.lastIndex.coerceAtLeast(0))
        status = "Selected ${state.solids.getOrNull(selectedSolid)?.type?.name ?: "solid"}"
    }

    fun deleteSelectedSolid() {
        deleteSelectedSolids(setOf(selectedSolid))
    }

    fun deleteSelectedSolids(indices: Set<Int>) {
        val valid = indices.filterTo(linkedSetOf()) { it in state.solids.indices }
        if (valid.isEmpty()) return
        val names = valid.mapNotNull(state.solids::getOrNull).map { it.type.name }
        val anchor = valid.minOrNull() ?: 0
        state = history.execute(state, DeleteSolidsCommand(valid, state.solids))
        selectedSolid = (anchor - 1).coerceAtMost(state.solids.lastIndex).coerceAtLeast(0)
        solidGesture = null
        status = "Deleted ${names.joinToString()}"
    }

    fun moveSolid(index: Int, to: Vec3) {
        val solid = state.solids.getOrNull(index) ?: return
        state = history.execute(state, MoveSolidCommand(index, solid.position, to))
        selectedSolid = index
        status = "Moved ${solid.type.name}"
    }

    fun beginSolidDrag(index: Int) {
        val solid = state.solids.getOrNull(index) ?: return
        solidGesture = SolidGesture(index, solid)
        selectSolid(index)
    }

    fun beginSolidGroupDrag(indices: Set<Int>) {
        val valid = indices.filterTo(linkedSetOf()) { it in state.solids.indices }
        if (valid.isEmpty()) return
        solidGroupGesture = SolidGroupGesture(valid, state.solids)
        selectedSolid = valid.last()
    }

    fun previewSolidGroupMove(delta: Vec3) {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(position = solid.position + delta) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidGroupRotation(delta: Vec3) {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(rotation = solid.rotation + delta) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidGroupScale(factor: Double) {
        val gesture = solidGroupGesture ?: return; val f = factor.coerceIn(.2, 5.0)
        state = state.copy(solids = gesture.from.mapIndexed { index, solid -> if (index in gesture.indices) solid.copy(width = solid.width * f, height = solid.height * f, depth = solid.depth * f, radius = solid.radius * f, topRadius = solid.topRadius * f) else solid }, modifiedAt = System.currentTimeMillis())
    }

    fun endSolidGroupDrag() {
        val gesture = solidGroupGesture ?: return
        if (state.solids != gesture.from) history.recordApplied(ReplaceSolidsCommand(gesture.from, state.solids, "Transform 3D group"))
        solidGroupGesture = null; status = "Transformed ${gesture.indices.size} objects"
    }

    fun cancelSolidGroupDrag() {
        val gesture = solidGroupGesture ?: return
        state = state.copy(solids = gesture.from); solidGroupGesture = null; status = "Group transform cancelled"
    }

    fun previewSolidDrag(index: Int, delta: Vec3) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) gesture.from.copy(position = gesture.from.position + delta) else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun previewSolidRotation(index: Int, deltaDegrees: Vec3) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val base = gesture.from.rotation
        val rotated = gesture.from.copy(rotation = base + deltaDegrees)
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) rotated else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun previewSolidScale(index: Int, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val f = factor.coerceIn(.2, 5.0)
        val scaled = gesture.from.copy(
            width = (gesture.from.width * f).coerceIn(.2, 12.0),
            height = (gesture.from.height * f).coerceIn(.2, 12.0),
            depth = (gesture.from.depth * f).coerceIn(.2, 12.0),
            radius = (gesture.from.radius * f).coerceIn(.1, 6.0),
            topRadius = (gesture.from.topRadius * f).coerceIn(.05, 6.0),
        )
        state = state.copy(
            solids = state.solids.mapIndexed { i, old -> if (i == index) scaled else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun previewSolidAxisScale(index: Int, axis: TransformGizmoAxis, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val f = factor.coerceIn(.2, 5.0)
        val from = gesture.from
        val scaled = from.copy(
            width = (from.width * if (axis in setOf(TransformGizmoAxis.X, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            height = (from.height * if (axis in setOf(TransformGizmoAxis.Y, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            depth = (from.depth * if (axis in setOf(TransformGizmoAxis.Z, TransformGizmoAxis.Uniform)) f else 1.0).coerceIn(.2, 12.0),
            radius = (from.radius * if (axis == TransformGizmoAxis.Uniform) f else 1.0).coerceIn(.1, 6.0),
            topRadius = (from.topRadius * if (axis == TransformGizmoAxis.Uniform) f else 1.0).coerceIn(.05, 6.0),
        )
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == index) scaled else old }, modifiedAt = System.currentTimeMillis())
    }

    fun previewSolidFaceExtrusion(index: Int, faceIndex: Int, factor: Double) {
        val gesture = solidGesture?.takeIf { it.index == index } ?: return
        val amount = (factor - 1.0) * 2.0
        val extruded = AdvancedSpatialInteractionEngine.extrude(gesture.from, faceIndex, amount)
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == index) extruded else old }, modifiedAt = System.currentTimeMillis())
    }

    fun endSolidDrag() {
        val gesture = solidGesture ?: return
        val final = state.solids.getOrNull(gesture.index)
        if (final != null && final != gesture.from) history.recordApplied(TransformSolidCommand(gesture.index, gesture.from, final))
        solidGesture = null
        status = "Moved ${gesture.from.type.name}"
    }

    fun cancelSolidDrag() {
        val gesture = solidGesture ?: return
        state = state.copy(solids = state.solids.mapIndexed { i, old -> if (i == gesture.index) gesture.from else old })
        solidGesture = null
        status = "Move cancelled"
    }

    fun transformSolid(index: Int, transform: (Solid) -> Solid) {
        val from = state.solids.getOrNull(index) ?: return
        val to = transform(from)
        state = history.execute(state, TransformSolidCommand(index, from, to))
        selectedSolid = index
        status = "Transformed ${to.type.name}"
    }

    fun replaceSolids(label: String, transform: (List<Solid>) -> List<Solid>) {
        val from = state.solids
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, ReplaceSolidsCommand(from, to, label))
        selectedSolid = selectedSolid.coerceIn(0, state.solids.lastIndex.coerceAtLeast(0))
        status = label
    }

    fun duplicateSelectedSolid() {
        val source = state.solids.getOrNull(selectedSolid) ?: return
        state = history.execute(state, AddSolidCommand(source.copy(position = source.position + Vec3(.8, .2, .8))))
        selectedSolid = state.solids.lastIndex
        status = "Duplicated ${source.type.name}"
    }

    fun addVector3D() {
        val n = state.vectors3D.size + 1
        val offset = (n - 2) * .55
        val vector = Vector3D(
            id = "w$n",
            name = "w$n",
            start = Vec3(-1.5 + offset, -1.0, -1.0),
            end = Vec3(1.4 + offset, 1.0, 1.2),
        )
        state = history.execute(state, AddVector3DCommand(vector))
        selectedVector3D = state.vectors3D.lastIndex
        status = "Added 3D vector ${vector.name}"
    }

    fun selectVector3D(index: Int) {
        selectedVector3D = index.coerceIn(0, state.vectors3D.lastIndex.coerceAtLeast(0))
        status = "Selected vector ${state.vectors3D.getOrNull(selectedVector3D)?.name ?: ""}"
    }

    fun deleteVector3D(index: Int) {
        val vector = state.vectors3D.getOrNull(index) ?: return
        state = history.execute(state, DeleteVector3DCommand(index, vector))
        selectedVector3D = index.coerceAtMost(state.vectors3D.lastIndex).coerceAtLeast(0)
        vectorGesture = null
        status = "Deleted vector ${vector.name}"
    }

    fun moveVector3D(index: Int, delta: Vec3) {
        val from = state.vectors3D.getOrNull(index) ?: return
        val to = from.copy(start = from.start + delta, end = from.end + delta)
        state = history.execute(state, MoveVector3DCommand(index, from, to))
        selectedVector3D = index
        status = "Moved vector ${from.name}"
    }

    fun beginVectorDrag(index: Int) {
        val vector = state.vectors3D.getOrNull(index) ?: return
        vectorGesture = VectorGesture(index, vector)
        selectVector3D(index)
    }

    fun previewVectorDrag(index: Int, delta: Vec3) {
        val gesture = vectorGesture?.takeIf { it.index == index } ?: return
        val moved = gesture.from.copy(start = gesture.from.start + delta, end = gesture.from.end + delta)
        state = state.copy(
            vectors3D = state.vectors3D.mapIndexed { i, old -> if (i == index) moved else old },
            modifiedAt = System.currentTimeMillis(),
        )
    }

    fun endVectorDrag() {
        val gesture = vectorGesture ?: return
        val final = state.vectors3D.getOrNull(gesture.index)
        if (final != null && final != gesture.from) history.recordApplied(TransformVector3DCommand(gesture.index, gesture.from, final))
        vectorGesture = null
        status = "Moved vector ${gesture.from.name}"
    }

    fun cancelVectorDrag() {
        val gesture = vectorGesture ?: return
        state = state.copy(vectors3D = state.vectors3D.mapIndexed { i, old -> if (i == gesture.index) gesture.from else old })
        vectorGesture = null
        status = "Move cancelled"
    }

    fun transformVector3D(index: Int, transform: (Vector3D) -> Vector3D) {
        val from = state.vectors3D.getOrNull(index) ?: return
        val to = transform(from)
        state = history.execute(state, TransformVector3DCommand(index, from, to))
        selectedVector3D = index
        status = "Transformed vector ${to.name}"
    }

    fun setSurfaceExpression(value: String) {
        state = state.copy(surfaceExpression = value, modifiedAt = System.currentTimeMillis())
        val validation = universalDocumentEngine.validate(universalMathDocument)
        status = if (validation.valid) "Surface updated through the universal maths document" else validation.diagnostics.first()
    }

    fun transformSpatialPlacement(label: String = "Transform spatial scene", transform: (com.indianservers.aiexplorer.spatial.SpatialScenePlacement) -> com.indianservers.aiexplorer.spatial.SpatialScenePlacement) {
        val from = state.spatialPlacement
        val to = transform(from)
        if (from == to) return
        state = history.execute(state, TransformSpatialPlacementCommand(from, to, label))
        status = label
    }

    fun placeSpatialScene() = transformSpatialPlacement("Place scene in space") {
        SpatialPlacementEngine.place(it, Vec3(0.0, 0.0, -1.2), System.currentTimeMillis())
            .copy(trackingQuality = TrackingQuality.Stopped, estimated = true)
    }

    fun placeSpatialHit(hit: SpatialHit) {
        val from = state.spatialPlacement
        val (to, validation) = SpatialPlacementEngine.place(from, hit, System.currentTimeMillis())
        if (from != to) state = history.execute(state, TransformSpatialPlacementCommand(from, to, "Place scene on ${hit.type.name.lowercase()}"))
        status = if (validation.accepted) "Spatial scene placed · ±${trim(hit.uncertaintyMeters)} m" else validation.messages.joinToString(" ")
    }

    fun resetSpatialScene() = transformSpatialPlacement("Reset spatial anchor") {
        com.indianservers.aiexplorer.spatial.SpatialScenePlacement()
    }

    fun setSpatialScaleMode(mode: ARScaleMode) = transformSpatialPlacement("Set ${mode.name} scale") {
        SpatialPlacementEngine.setScaleMode(it, mode)
    }

    fun setDepthOcclusion(enabled: Boolean) = transformSpatialPlacement("${if (enabled) "Enable" else "Disable"} depth occlusion") {
        it.copy(depthOcclusionEnabled = enabled)
    }

    fun beginSpatialGesture() {
        spatialGestureFrom = state.spatialPlacement
    }

    fun previewSpatialGesture(panPixels: Offset, rotationDegrees: Float, scaleFactor: Float) {
        val from = spatialGestureFrom ?: return
        val moved = SpatialPlacementEngine.move(from, Vec3((panPixels.x / 520f).toDouble(), 0.0, (panPixels.y / 520f).toDouble()))
        val rotated = SpatialPlacementEngine.rotate(moved, Vec3(0.0, rotationDegrees.toDouble(), 0.0))
        val transformed = if (kotlin.math.abs(scaleFactor - 1f) > .001f) SpatialPlacementEngine.scale(rotated, scaleFactor.toDouble()) else rotated
        state = state.copy(spatialPlacement = transformed)
    }

    fun endSpatialGesture() {
        val from = spatialGestureFrom ?: return
        val to = state.spatialPlacement
        spatialGestureFrom = null
        if (from != to) history.recordApplied(TransformSpatialPlacementCommand(from, to, "Manipulate spatial scene"))
    }

    fun cancelSpatialGesture() {
        val from = spatialGestureFrom ?: return
        state = state.copy(spatialPlacement = from)
        spatialGestureFrom = null
    }

    fun undo() {
        state = history.undo(state)
        status = "Undo"
    }

    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo
    val universalHistoryDepth: Int get() = history.protocol.size

    val mathsBreadcrumb: List<String>
        get() = when {
            showProblemSolver -> listOf("Maths", "Algebra", "Quadratics")
            showKnowledgeHub && activeKnowledgeSection == KnowledgeSection.Dictionary -> listOf("Maths", "Reference", "Visual Dictionary")
            showKnowledgeHub -> listOf("Maths", "Knowledge", activeKnowledgeSection.title)
            showScientificCalculator -> listOf("Maths", "Tools", "Scientific Calculator")
            showMathNotebook -> listOf("Maths", "Workspace", "Notebook")
            showUnifiedMathStudio -> listOf("Maths", "Studio", "Linked Views")
            showAdaptiveMathLearning -> listOf("Maths", "Learn", "Adaptive Coach")
            showProbabilityLab -> listOf("Maths", "Data", "Probability & Statistics")
            showShapesExplorer -> listOf("Maths", "Geometry", "Shapes Explorer")
            showSetLogicVisualizer -> listOf("Maths", "Foundations", "Set Theory & Logic")
            else -> listOf("Maths", state.module.label)
        }

    fun redo() {
        state = history.redo(state)
        status = "Redo"
    }

    fun reset() {
        state = WorkspaceState(module = state.module)
        selectedShape = -1
        selectedShapes = emptySet()
        status = "Workspace reset"
    }

    fun exportJson(): String {
        status = "Workspace JSON generated"
        return WorkspaceJson.export(state)
    }

    fun hydrateDurableState(recovered: WorkspaceState?, projects: List<SavedWorkspace>, persistedSettings: AppSettings) {
        recovered?.let { state = it }
        savedWorkspaces = projects
        settings = persistedSettings
        selectedPoint = selectedPoint.coerceIn(0, state.points.lastIndex.coerceAtLeast(0))
        selectedShape = -1
        selectedShapes = emptySet()
        status = if (recovered == null) "Ready" else "Recovered your last workspace"
    }

    fun importWorkspace(imported: WorkspaceState, recovered: Boolean, diagnostics: List<String>) {
        state = imported.copy(modifiedAt = System.currentTimeMillis())
        selectedPoint = 0.coerceAtMost(state.points.lastIndex)
        selectedShape = -1
        selectedShapes = emptySet()
        showSubjectHub = false
        showMathLanding = false
        showMathMenu = false
        status = when {
            recovered -> "Project recovered${diagnostics.firstOrNull()?.let { ": $it" }.orEmpty()}"
            else -> "Project imported"
        }
    }

    fun reportStatus(message: String) { status = message.take(180) }

    fun saveWorkspace() {
        val saved = SavedWorkspace(
            id = "workspace-${System.currentTimeMillis()}",
            name = "${state.name} ${savedWorkspaces.size + 1}",
            module = state.module,
            snapshot = state,
            json = WorkspaceProjectCodec.encode(state),
            updatedAt = System.currentTimeMillis(),
        )
        savedWorkspaces = listOf(saved) + savedWorkspaces.take(7)
        status = "Workspace saved"
    }

    fun duplicateWorkspace(saved: SavedWorkspace) {
        val copy = saved.copy(
            id = "workspace-${System.currentTimeMillis()}",
            name = "${saved.name} Copy",
            updatedAt = System.currentTimeMillis(),
        )
        savedWorkspaces = listOf(copy) + savedWorkspaces
        status = "Workspace duplicated"
    }

    fun deleteWorkspace(saved: SavedWorkspace) {
        savedWorkspaces = savedWorkspaces.filterNot { it.id == saved.id }
        status = "Workspace deleted"
    }

    fun restoreWorkspaceShell(saved: SavedWorkspace) {
        state = saved.snapshot.copy(name = saved.name, module = saved.module, modifiedAt = System.currentTimeMillis())
        status = "Opened ${saved.name}"
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        status = "Settings updated"
    }

    fun exportLearningPackage(): String {
        val source = learningPackageJson()
        lastPackageValidation = LearningPackage.validate(source)
        status = lastPackageValidation.message
        return source
    }

    fun exportLearningPackagePreview(): String = learningPackageJson().lineSequence().take(8).joinToString("\n")

    fun validateLearningPackage(source: String): Boolean {
        lastPackageValidation = LearningPackage.validate(source)
        status = lastPackageValidation.message
        return lastPackageValidation.valid
    }

    private fun learningPackageJson(): String = LearningPackage.export(state, activeActivityId, lessonProgress, assignments)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun AIExplorerApp(vm: ExplorerViewModel = viewModel()) {
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    var dockOffset by remember { mutableStateOf(Offset.Zero) }
    val applicationContext = LocalContext.current.applicationContext
    val durableStore = remember(applicationContext) { DurableMathStore(applicationContext) }
    var persistenceReady by remember { mutableStateOf(false) }
    LaunchedEffect(durableStore) {
        val recovered = runCatching { durableStore.loadRecovery() }.getOrNull()
        val projects = runCatching { durableStore.loadProjects() }.getOrDefault(emptyList())
        val settings = runCatching { durableStore.loadSettings() }.getOrDefault(AppSettings())
        vm.hydrateDurableState(recovered, projects, settings)
        persistenceReady = true
    }
    LaunchedEffect(durableStore, persistenceReady) {
        if (!persistenceReady) return@LaunchedEffect
        snapshotFlow { vm.state }.drop(1).conflate().collect { state ->
            runCatching { durableStore.saveRecovery(state) }
                .onFailure { vm.reportStatus("Autosave unavailable: ${it.message ?: "storage error"}") }
        }
    }
    LaunchedEffect(durableStore, persistenceReady) {
        if (!persistenceReady) return@LaunchedEffect
        snapshotFlow { vm.savedWorkspaces }.drop(1).conflate().collect { projects ->
            runCatching { durableStore.replaceProjects(projects) }
                .onFailure { vm.reportStatus("Project library unavailable: ${it.message ?: "storage error"}") }
        }
    }
    LaunchedEffect(durableStore, persistenceReady) {
        if (!persistenceReady) return@LaunchedEffect
        snapshotFlow { vm.settings }.drop(1).conflate().collect { settings ->
            runCatching { durableStore.saveSettings(settings) }
                .onFailure { vm.reportStatus("Settings could not be saved: ${it.message ?: "storage error"}") }
        }
    }
    BackHandler(enabled = !vm.showSubjectHub || vm.hasDismissibleOverlay()) {
        when {
            vm.showMathLanding -> vm.openSubjectHub()
            vm.hasDismissibleOverlay() -> vm.dismissTopOverlay()
            vm.showPhysicsHub -> vm.openSubjectHub()
            vm.showChemistryHub -> vm.openSubjectHub()
            vm.showBiologyHub -> vm.openSubjectHub()
            vm.showLearningIntelligence -> vm.openSubjectHub()
            else -> vm.returnToMathMenu()
        }
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Color(0xFF070A0F),
            primary = Cyan,
            secondary = Violet,
            onBackground = Ink,
            onSurface = Ink,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = Background) {
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .background(radialBackdrop())
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            ) {
                val compact = maxWidth < 520.dp
                val wide = maxWidth >= 760.dp
                val imeVisible = WindowInsets.isImeVisible
                LaunchedEffect(compact, wide) {
                    menuOffset = Offset.Zero
                    dockOffset = Offset.Zero
                }
                if (vm.showSubjectHub) {
                    SubjectHubScreen(
                        modifier = Modifier.fillMaxSize(),
                        wide = wide,
                        onOpenSubject = { subject ->
                            when (subject) {
                                "Maths" -> vm.enterMaths()
                                "Physics" -> vm.openPhysicsHub()
                                "Chemistry" -> vm.openChemistryHub()
                                "Biology" -> vm.openBiologyHub()
                            }
                        },
                        onOpenLearningIntelligence = vm::openLearningIntelligence,
                    )
                } else {
                    if (vm.showLearningIntelligence) {
                        LearningIntelligenceFeatureRoot(onExit = vm::openSubjectHub)
                    } else if (vm.showBiologyHub) {
                        BiologyFeatureRoot(onExit = vm::openSubjectHub)
                    } else if (vm.showChemistryHub) {
                        ChemistryFeatureRoot(onExit = vm::openSubjectHub)
                    } else if (vm.showPhysicsHub) {
                        PhysicsHubScreen(vm, wide)
                    } else if (vm.showMathLanding) {
                        MathLandingScreen()
                    } else if (vm.showShapesExplorer) {
                        ShapesExplorerScreen(vm, wide = wide)
                    } else if (vm.showUnifiedMathStudio) {
                        UnifiedMathStudioScreen(vm.state, vm::commitUnifiedStudio, vm::returnToMathMenu)
                    } else if (vm.showAdaptiveMathLearning) {
                        AdaptiveMathLearningScreen(vm.state, vm::returnToMathMenu)
                    } else if (vm.showMathNotebook) {
                        MathNotebookScreen(vm, wide = wide)
                    } else if (vm.showProblemSolver) {
                        ProblemSolverScreen(vm, wide = wide)
                    } else if (vm.showScientificCalculator) {
                        ScientificCalculatorScreen(vm, wide = wide)
                    } else if (vm.showSetLogicVisualizer) {
                        SetTheoryLogicVisualizerScreen(vm, wide = wide)
                    } else if (vm.showProbabilityLab) {
                        ProbabilityLabScreen(vm, wide = wide)
                    } else if (vm.showKnowledgeHub) {
                        MathKnowledgeScreen(vm, wide = wide)
                    } else {
                        when (vm.state.module) {
                            MathModule.Geometry2D -> Geometry2DScreen(vm, compact)
                            MathModule.Geometry3D -> Geometry3DScreen(vm, compact)
                            MathModule.Graph2D -> Graph2DScreen(vm)
                            MathModule.Graph3D -> Graph3DScreen(vm)
                            MathModule.Trigonometry -> TrigonometryScreen(vm)
                            MathModule.Manipulatives -> ManipulativesScreen(vm, wide)
                            MathModule.SpatialAR -> SpatialARScreen(vm)
                        }
                    }
                    if (vm.showChrome && !vm.showUnifiedMathStudio && !vm.showAdaptiveMathLearning && !vm.showLearningIntelligence && !vm.showBiologyHub && !vm.showChemistryHub && !vm.showPhysicsHub && !vm.showMathLanding) TopShell(vm, compact, Modifier.align(Alignment.TopCenter))
                    if (vm.showLearningPanel && !vm.showLearningIntelligence && !vm.showProblemSolver && !vm.showScientificCalculator && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub) LearningCoachPanel(vm, Modifier.align(Alignment.CenterEnd))
                    if (!imeVisible && !vm.showUnifiedMathStudio && !vm.showAdaptiveMathLearning && !vm.showLearningIntelligence && !vm.showMathLanding && !vm.shapeExplorerScene && !vm.showShapesExplorer && !vm.showBiologyHub && !vm.showChemistryHub && !vm.showPhysicsHub && !vm.showScientificCalculator && !vm.showSetLogicVisualizer && !vm.showProblemSolver && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub) BottomModeSelector(vm.state.module, vm::open, compact, Modifier.align(Alignment.BottomCenter))
                    if (vm.showActionDock && vm.state.module != MathModule.Graph2D && !vm.showProblemSolver && !vm.showScientificCalculator && !vm.showMathNotebook && !vm.showProbabilityLab && !vm.showKnowledgeHub) MiniDock(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset { IntOffset(dockOffset.x.roundToInt(), dockOffset.y.roundToInt()) }
                            .padding(top = if (compact) 64.dp else 76.dp),
                        items = listOf("Focus", "Learn", "Tools", "Info", "Panel", "Export", "Close"),
                        onMove = { delta ->
                            dockOffset = Offset(
                                (dockOffset.x + delta.x).coerceIn(-720f, 0f),
                                (dockOffset.y + delta.y).coerceIn(-40f, 760f),
                            )
                        },
                        onClick = {
                            when (it) {
                                "Focus" -> vm.togglePanel(PanelSlot.Chrome)
                                "Learn" -> vm.toggleLearningPanel()
                                "Tools" -> vm.togglePanel(PanelSlot.Left)
                                "Info" -> vm.togglePanel(PanelSlot.Right)
                                "Panel" -> vm.togglePanel(PanelSlot.Bottom)
                                "Export" -> vm.exportJson()
                                "Close" -> vm.hideActionDock()
                            }
                        },
                    )
                    if (vm.showMathMenu) MathematicsMenuPanel(
                        vm = vm,
                        modifier = Modifier
                            .align(if (wide) Alignment.CenterStart else Alignment.Center)
                            .offset { IntOffset(menuOffset.x.roundToInt(), menuOffset.y.roundToInt()) }
                            .widthIn(max = if (wide) 460.dp else 390.dp)
                            .fillMaxWidth(if (wide) .46f else .94f),
                        compact = compact,
                        onMove = { delta ->
                            menuOffset = Offset(
                                (menuOffset.x + delta.x).coerceIn(-280f, 280f),
                                (menuOffset.y + delta.y).coerceIn(-520f, 520f),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MathLandingScreen() {
    Box(
        Modifier.fillMaxSize().semantics { contentDescription = "Mathematics menu background" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("∑", color = Cyan.copy(alpha = .3f), fontSize = 72.sp, fontWeight = FontWeight.ExtraBold)
            Text("Choose a Maths tool", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("A workspace opens only after you select it from the menu.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SetTheoryLogicVisualizerScreen(vm: ExplorerViewModel, wide: Boolean) {
    var setMode by remember { mutableStateOf(true) }
    var selectedSetLaw by remember { mutableStateOf(SetLogicCatalog.setLaws.first()) }
    var selectedLogicLaw by remember { mutableStateOf(SetLogicCatalog.logicLaws.first()) }
    var values by remember { mutableStateOf(mapOf("A" to false, "B" to false, "C" to false, "P" to false, "Q" to false, "R" to false)) }
    var formulasOpen by remember { mutableStateOf(true) }
    var visualOpen by remember { mutableStateOf(false) }
    var tableOpen by remember { mutableStateOf(false) }
    var explanationOpen by remember { mutableStateOf(false) }
    var overlap by remember { mutableFloatStateOf(.48f) }
    var selectedPoint by remember { mutableStateOf<Offset?>(null) }
    val law = if (setMode) selectedSetLaw else selectedLogicLaw
    val live = remember(law, values) { SetLogicEngine.evaluate(law, values) }
    val rows = remember(law) { SetLogicEngine.rows(law) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 26.dp else 6.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back", icon = "←", iconOnly = true, onClick = vm::returnToMathMenu)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SET THEORY & LOGIC", color = Cyan, fontSize = if (wide) 22.sp else 17.sp, fontWeight = FontWeight.Bold)
                Text("Interactive Formula Visualizer", color = Muted, fontSize = 10.sp)
            }
            TransparentIcon("∪∧", Violet)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(if (setMode) "• Set Theory" else "Set Theory", icon = "∪") { setMode = true; formulasOpen = true; visualOpen = false; selectedPoint = null }
            GlowButton(if (!setMode) "• Logic" else "Logic", icon = "∧") { setMode = false; formulasOpen = true; visualOpen = false }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(law.title, color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                MathFormulaText(law.formula, color = Ink, fontSize = 16.sp)
            }
            GlowButton(if (formulasOpen) "Close formulas" else "Open formulas") { formulasOpen = !formulasOpen }
        }
        AnimatedVisibility(formulasOpen) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (if (setMode) SetLogicCatalog.setLaws else SetLogicCatalog.logicLaws).forEach { option ->
                    val formulaLabel = if (setMode) SetLogicCatalog.shortSetFormula(option) else option.formula
                    GlowButton(if (option.id == law.id && visualOpen) "• $formulaLabel" else formulaLabel) {
                        if (setMode) selectedSetLaw = option else selectedLogicLaw = option
                        formulasOpen = false
                        visualOpen = true
                        tableOpen = false
                        selectedPoint = null
                    }
                }
            }
        }
        if (setMode) {
            AnimatedVisibility(visualOpen) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Diagram for ${SetLogicCatalog.shortSetFormula(law)}", color = Cyan, fontWeight = FontWeight.Bold)
                        GlowButton("Close diagram") { visualOpen = false; formulasOpen = true }
                    }
                    SetFormulaVennCanvas(law, overlap, selectedPoint) { point, memberships ->
                        selectedPoint = point
                        values = values + memberships
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Overlap", color = Muted, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        Slider(overlap, { overlap = it }, valueRange = .05f..1f, modifier = Modifier.weight(1f))
                    }
                    Text("Tap anywhere in the universe to test that element's membership.", color = Muted, fontSize = 11.sp)
                }
            }
            if (!visualOpen) {
                Text("Select a formula such as A ∪ B to open its interactive diagram.", color = Green, fontSize = 12.sp)
            }
            SetTheoryLearningStudioPanel(law)
        } else {
            AnimatedVisibility(visualOpen) { LogicGateVisualizer(law, values, live.left) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            law.variables.forEach { variable ->
                TogglePill("$variable = ${if (values[variable] == true) "TRUE" else "FALSE"}", values[variable] == true) {
                    values = values + (variable to !(values[variable] == true))
                    selectedPoint = null
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (live.equivalent) Green.copy(.09f) else Amber.copy(.1f))
                .border(1.dp, if (live.equivalent) Green.copy(.55f) else Amber, RoundedCornerShape(16.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(law.leftLabel, color = Cyan, fontSize = 11.sp)
                Text(if (live.left) "TRUE" else "FALSE", color = if (live.left) Green else Muted, fontWeight = FontWeight.Bold)
            }
            Text(if (live.equivalent) "=" else "≠", color = if (live.equivalent) Green else Amber, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Column(horizontalAlignment = Alignment.End) {
                Text(law.rightLabel, color = Violet, fontSize = 11.sp)
                Text(if (live.right) "TRUE" else "FALSE", color = if (live.right) Green else Muted, fontWeight = FontWeight.Bold)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (tableOpen) "Close table" else "Open full table") { tableOpen = !tableOpen }
            GlowButton(if (explanationOpen) "Close explanation" else "Why it works") { explanationOpen = !explanationOpen }
        }
        AnimatedVisibility(explanationOpen) {
            KnowledgeCard("Why it works", law.explanation, if (SetLogicEngine.verified(law)) "Verified for all ${rows.size} cases" else "Counterexample found", "Change the inputs and compare both sides.", Green)
        }
        AnimatedVisibility(tableOpen) { BooleanFormulaTable(law, rows, values) }
    }
}

@Composable
private fun SetTheoryLearningStudioPanel(activeLaw: BooleanFormulaLaw) {
    val context = LocalContext.current
    var studioOpen by remember { mutableStateOf(false) }
    var conceptsOpen by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(SetStudioTool.Elements) }
    var toolMenuOpen by remember { mutableStateOf(false) }
    var conceptQuery by remember { mutableStateOf("") }
    var selectedConceptId by remember { mutableStateOf(SetTheoryLearningCatalog.concepts.first().id) }
    var favourites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var recent by remember { mutableStateOf<List<String>>(emptyList()) }
    var inputA by remember { mutableStateOf("1, 2, 3") }
    var inputB by remember { mutableStateOf("2, 3, 4") }
    var universeName by remember { mutableStateOf("U") }
    var setAName by remember { mutableStateOf("A") }
    var setBName by remember { mutableStateOf("B") }
    var relation by remember { mutableStateOf(setOf("1" to "1", "2" to "2", "3" to "3")) }
    var relationMatrix by remember { mutableStateOf(false) }
    var undo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var redo by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var saved by remember { mutableStateOf<List<String>>(emptyList()) }
    var sharePreview by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableIntStateOf(1) }
    var guided by remember { mutableStateOf(true) }
    var prediction by remember { mutableStateOf<Boolean?>(null) }
    var reveal by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var spokenMath by remember { mutableStateOf(false) }
    var eulerMode by remember { mutableStateOf(false) }
    var setCount by remember { mutableIntStateOf(2) }
    val a = remember(inputA) { SetTheoryStudioEngine.parseElements(inputA) }
    val b = remember(inputB) { SetTheoryStudioEngine.parseElements(inputB) }
    val domain = a.toSet()
    fun updateInputs(nextA: String = inputA, nextB: String = inputB) {
        undo = undo + (inputA to inputB); redo = emptyList(); inputA = nextA; inputB = nextB
    }
    fun chooseConcept(id: String) { selectedConceptId = id; recent = (listOf(id) + recent.filterNot { it == id }).take(6) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x330B1C2D)).border(1.dp, Violet.copy(.4f), RoundedCornerShape(16.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Set Theory Learning Studio", color = Violet, fontWeight = FontWeight.Bold)
                Text("50 concepts · 50 interactive learning features", color = Muted, fontSize = 10.sp)
            }
            GlowButton(if (studioOpen) "Close studio" else "Open studio") { studioOpen = !studioOpen }
        }
        AnimatedVisibility(studioOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(activeTool.label, color = Cyan, fontWeight = FontWeight.Bold)
                    GlowButton(if (toolMenuOpen) "Close tools" else "Open tools") { toolMenuOpen = !toolMenuOpen }
                }
                AnimatedVisibility(toolMenuOpen) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        SetStudioTool.entries.forEach { tool -> GlowButton(if (activeTool == tool) "• ${tool.label}" else tool.label) { activeTool = tool; toolMenuOpen = false } }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Concept Library", color = Green, fontWeight = FontWeight.Bold)
                    GlowButton(if (conceptsOpen) "Close concepts" else "Open concepts") { conceptsOpen = !conceptsOpen }
                }
                AnimatedVisibility(conceptsOpen) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(conceptQuery, { conceptQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search 50 concepts") }, singleLine = true)
                        if (recent.isNotEmpty()) Text("Recent: ${recent.mapNotNull { id -> SetTheoryLearningCatalog.concepts.firstOrNull { it.id == id }?.title }.joinToString()}", color = Muted, fontSize = 10.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            SetTheoryLearningCatalog.concepts.filter { it.title.contains(conceptQuery, true) || it.category.contains(conceptQuery, true) }.forEach { concept ->
                                GlowButton(if (concept.id in favourites) "★ ${concept.title}" else concept.title) { chooseConcept(concept.id) }
                            }
                        }
                        SetTheoryLearningCatalog.concepts.firstOrNull { it.id == selectedConceptId }?.let { concept ->
                            KnowledgeCard(concept.title, concept.definition, "${concept.category} · Level ${concept.level}", concept.example, Green)
                            GlowButton(if (concept.id in favourites) "Remove favourite" else "Add favourite") { favourites = if (concept.id in favourites) favourites - concept.id else favourites + concept.id }
                        }
                    }
                }
                when (activeTool) {
                    SetStudioTool.Venn -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { count -> GlowButton(if (setCount == count) "• $count sets" else "$count sets") { setCount = count } }
                            TogglePill(if (eulerMode) "Euler mode" else "Venn mode", eulerMode) { eulerMode = !eulerMode }
                            TogglePill("High contrast", highContrast) { highContrast = !highContrast }
                            TogglePill("Spoken maths", spokenMath) { spokenMath = !spokenMath }
                        }
                        Insight("Region cardinality", "|$setAName∪$setBName| = ${SetTheoryStudioEngine.inclusionExclusion(a.toSet(), b.toSet())}", Cyan)
                        Text("Pinch/pan, animated shading and image export controls apply to the live Venn canvas above.", color = Muted, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Export image") { sharePreview = runCatching { exportSetDiagramPng(context, setAName, a, setBName, b) }.getOrElse { "Export failed: ${it.message}" } }
                            GlowButton("Describe") { sharePreview = "${if (spokenMath) "Speaking: " else "Description: "}${activeLaw.formula}; ${activeLaw.explanation}" }
                        }
                    }
                    SetStudioTool.Elements -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(setAName, { setAName = it.take(3) }, modifier = Modifier.width(90.dp), label = { Text("Set A") }, singleLine = true)
                            OutlinedTextField(setBName, { setBName = it.take(3) }, modifier = Modifier.width(90.dp), label = { Text("Set B") }, singleLine = true)
                            OutlinedTextField(universeName, { universeName = it.take(4) }, modifier = Modifier.width(110.dp), label = { Text("Universe") }, singleLine = true)
                        }
                        OutlinedTextField(inputA, { updateInputs(nextA = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("$setAName elements: text, numbers or symbols") })
                        OutlinedTextField(inputB, { updateInputs(nextB = it) }, modifier = Modifier.fillMaxWidth(), label = { Text("$setBName elements") })
                        Insight("Roster", "$setAName=${SetTheoryStudioEngine.roster(a)}  $setBName=${SetTheoryStudioEngine.roster(b)}", Cyan)
                        Insight("Set-builder", "$setAName={x ∈ $universeName | x is one of ${a.joinToString()}}", Violet)
                        Text("Duplicates are removed automatically. Tap or drag elements on the Venn canvas to change region membership.", color = Green, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Undo", enabled = undo.isNotEmpty()) { undo.lastOrNull()?.let { snapshot -> redo = redo + (inputA to inputB); inputA = snapshot.first; inputB = snapshot.second; undo = undo.dropLast(1) } }
                            GlowButton("Redo", enabled = redo.isNotEmpty()) { redo.lastOrNull()?.let { snapshot -> undo = undo + (inputA to inputB); inputA = snapshot.first; inputB = snapshot.second; redo = redo.dropLast(1) } }
                            GlowButton("Reset") { updateInputs("1, 2, 3", "2, 3, 4") }
                            GlowButton("Save") { saved = (saved + "$setAName=${SetTheoryStudioEngine.roster(a)}; $setBName=${SetTheoryStudioEngine.roster(b)}").distinct() }
                            GlowButton("Share data") {
                                val data = "{\"universe\":\"$universeName\",\"$setAName\":${a},\"$setBName\":${b}}"
                                sharePreview = data
                                shareSetConstruction(context, data)
                            }
                        }
                        if (saved.isNotEmpty()) Text("Saved: ${saved.joinToString(" | ")}", color = Muted, fontSize = 10.sp)
                    }
                    SetStudioTool.PowerSet -> {
                        val subsets = runCatching { SetTheoryStudioEngine.powerSet(a) }.getOrElse { emptyList() }
                        Insight("Power-set size", "2^${a.size} = ${subsets.size}", Violet)
                        Text(subsets.take(64).joinToString { SetTheoryStudioEngine.roster(it) }, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        Text("Subset tree: ∅ → singletons → pairs → … → $setAName", color = Green, fontSize = 11.sp)
                    }
                    SetStudioTool.Cartesian -> {
                        val product = SetTheoryStudioEngine.cartesianProduct(a, b)
                        Insight("Product", "|$setAName×$setBName| = ${product.size}", Cyan)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { product.forEach { (x, y) -> Insight("", "($x,$y)", Violet) } }
                    }
                    SetStudioTool.Relations -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            domain.forEach { x -> domain.forEach { y ->
                                val pair = x to y; TogglePill(if (relationMatrix) "${if (pair in relation) 1 else 0}" else "$x→$y", pair in relation) { relation = if (pair in relation) relation - pair else relation + pair }
                            } }
                        }
                        GlowButton(if (relationMatrix) "Arrow view" else "Matrix view") { relationMatrix = !relationMatrix }
                        val analysis = SetTheoryStudioEngine.analyzeRelation(domain, relation.filter { it.first in domain && it.second in domain }.toSet())
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Insight("Reflexive", analysis.reflexive.toString(), if (analysis.reflexive) Green else Muted)
                            Insight("Symmetric", analysis.symmetric.toString(), if (analysis.symmetric) Green else Muted)
                            Insight("Antisymmetric", analysis.antisymmetric.toString(), if (analysis.antisymmetric) Green else Muted)
                            Insight("Transitive", analysis.transitive.toString(), if (analysis.transitive) Green else Muted)
                        }
                    }
                    SetStudioTool.Partitions -> {
                        val numeric = a.mapNotNull { it.toIntOrNull() }
                        val classes = numeric.groupBy { kotlin.math.abs(it % 2) }.values.map { it.toSet() }
                        Text("Equivalence classes by parity", color = Cyan, fontWeight = FontWeight.Bold)
                        classes.forEachIndexed { index, group -> Insight("Class ${index + 1}", SetTheoryStudioEngine.roster(group.map(Int::toString)), listOf(Cyan, Violet)[index % 2]) }
                        Insight("Partition valid", (classes.flatten().toSet() == numeric.toSet() && classes.all { it.isNotEmpty() }).toString(), Green)
                    }
                    SetStudioTool.Order -> {
                        val numbers = a.mapNotNull { it.toIntOrNull() }.filter { it != 0 }.toSet()
                        val covers = SetTheoryStudioEngine.hasseCovers(numbers)
                        Text("Divisibility Hasse covers", color = Cyan, fontWeight = FontWeight.Bold)
                        Text(covers.joinToString { "${it.first} ≺ ${it.second}" }.ifBlank { "Add comparable non-zero integers to set A." }, color = Ink, fontFamily = FontFamily.Monospace)
                        Text("Drag-to-order nodes use the cover pairs above; transitive edges are removed.", color = Muted, fontSize = 10.sp)
                    }
                    SetStudioTool.Functions -> {
                        val codomain = b.toSet(); val mapping = a.mapIndexedNotNull { index, item -> b.getOrNull(index % b.size.coerceAtLeast(1))?.let { item to it } }.toMap()
                        val analysis = SetTheoryStudioEngine.analyzeMapping(domain, codomain, mapping)
                        Text(mapping.entries.joinToString { "${it.key} → ${it.value}" }, color = Ink, fontFamily = FontFamily.Monospace)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Insight("Function", analysis.isFunction.toString(), Cyan); Insight("Injective", analysis.injective.toString(), Violet)
                            Insight("Surjective", analysis.surjective.toString(), Green); Insight("Bijective", analysis.bijective.toString(), Amber)
                        }
                    }
                    SetStudioTool.Proofs -> {
                        KnowledgeCard(activeLaw.title, activeLaw.formula, "Membership proof", activeLaw.explanation, Violet)
                        Text("1. Let x be arbitrary.  2. Translate set membership into Boolean conditions.  3. Apply the law.  4. Translate back to sets.", color = Ink, fontSize = 12.sp)
                        Insight("All cases", if (SetLogicEngine.verified(activeLaw)) "No counterexample" else "Counterexample found", Green)
                    }
                    SetStudioTool.Challenge -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { level -> GlowButton(if (difficulty == level) "• Level $level" else "Level $level") { difficulty = level } }
                            TogglePill("Guided", guided) { guided = !guided }
                        }
                        Text("Predict: Is ${activeLaw.formula} true for every membership assignment?", color = Ink, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GlowButton("Always true") { prediction = true; reveal = false }
                            GlowButton("Has counterexample") { prediction = false; reveal = false }
                            GlowButton(if (reveal) "Hide result" else "Reveal") { reveal = !reveal }
                        }
                        if (reveal) Insight("Result", if (prediction == SetLogicEngine.verified(activeLaw)) "Correct · mastery +1" else "Try again · inspect the full table", if (prediction == SetLogicEngine.verified(activeLaw)) Green else Amber)
                        Insight("Mastery", "${favourites.size + recent.size}/50 concepts explored", Violet)
                    }
                }
                val routedFeatures = SetTheoryLearningCatalog.features.filter { it.tool == activeTool }
                Text("Included here: ${routedFeatures.joinToString { it.title }}", color = Muted, fontSize = 9.sp)
                sharePreview?.let { preview ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Cyan.copy(.07f)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(preview, color = Cyan, fontSize = 10.sp, modifier = Modifier.weight(1f)); GlowButton("Close") { sharePreview = null }
                    }
                }
            }
        }
    }
}

private fun exportSetDiagramPng(
    context: android.content.Context,
    setAName: String,
    a: List<String>,
    setBName: String,
    b: List<String>,
): String {
    val bitmap = android.graphics.Bitmap.createBitmap(1200, 800, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(5, 13, 24))
    val fillA = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(75, 30, 210, 255); style = android.graphics.Paint.Style.FILL }
    val fillB = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(75, 145, 95, 255); style = android.graphics.Paint.Style.FILL }
    val strokeA = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(30, 210, 255); style = android.graphics.Paint.Style.STROKE; strokeWidth = 7f }
    val strokeB = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(145, 95, 255); style = android.graphics.Paint.Style.STROKE; strokeWidth = 7f }
    val text = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textSize = 34f }
    canvas.drawCircle(470f, 390f, 245f, fillA); canvas.drawCircle(470f, 390f, 245f, strokeA)
    canvas.drawCircle(730f, 390f, 245f, fillB); canvas.drawCircle(730f, 390f, 245f, strokeB)
    canvas.drawText("$setAName = ${SetTheoryStudioEngine.roster(a)}", 80f, 90f, text)
    canvas.drawText("$setBName = ${SetTheoryStudioEngine.roster(b)}", 650f, 90f, text)
    canvas.drawText("$setAName ∩ $setBName = ${SetTheoryStudioEngine.roster(a.toSet().intersect(b.toSet()))}", 330f, 735f, text)
    val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir
    val file = java.io.File(directory, "set-theory-${System.currentTimeMillis()}.png")
    java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return "PNG exported: ${file.absolutePath}"
}

private fun shareSetConstruction(context: android.content.Context, data: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(android.content.Intent.EXTRA_TEXT, data)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share Set Theory construction").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun SetFormulaVennCanvas(
    law: BooleanFormulaLaw,
    overlap: Float,
    selectedPoint: Offset?,
    onSelect: (Offset, Map<String, Boolean>) -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    fun memberships(point: Offset, width: Float, height: Float): Map<String, Boolean> {
        val radius = minOf(width, height) * .27f * zoom
        val separation = radius * (1.65f - overlap)
        val centers = listOf(
            Offset(width / 2 - separation / 2, height * .47f) + pan,
            Offset(width / 2 + separation / 2, height * .47f) + pan,
            Offset(width / 2, height * .64f) + pan,
        )
        return law.variables.mapIndexed { index, name -> name to ((point - centers[index]).getDistance() <= radius) }.toMap()
    }
    var canvasWidth by remember { mutableFloatStateOf(1f) }
    var canvasHeight by remember { mutableFloatStateOf(1f) }
    Canvas(
        Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF07111F))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(20.dp))
            .onSizeChanged { canvasWidth = it.width.toFloat(); canvasHeight = it.height.toFloat() }
            .pointerInput(law.id, overlap, zoom, pan) { detectTapGestures(onDoubleTap = { zoom = 1f; pan = Offset.Zero }, onTap = { point -> onSelect(point, memberships(point, canvasWidth, canvasHeight)) }) }
            .pointerInput(Unit) { detectTransformGestures { _, panChange, zoomChange, _ -> zoom = (zoom * zoomChange).coerceIn(.65f, 2.4f); pan += panChange } }
            .semantics { contentDescription = "Interactive Venn diagram for ${law.title}; tap to test set membership" },
    ) {
        val radius = minOf(size.width, size.height) * .27f * zoom
        val separation = radius * (1.65f - overlap)
        val centers = listOf(Offset(size.width / 2 - separation / 2, size.height * .47f) + pan, Offset(size.width / 2 + separation / 2, size.height * .47f) + pan, Offset(size.width / 2, size.height * .64f) + pan)
        drawRect(Color(0xFF091626), style = androidx.compose.ui.graphics.drawscope.Fill)
        val colors = listOf(Cyan, Violet, Amber)
        law.variables.forEachIndexed { index, _ ->
            drawCircle(colors[index].copy(.13f), radius, centers[index])
            drawCircle(colors[index].copy(.9f), radius, centers[index], style = Stroke(width = 3f))
        }
        val columns = 17; val lines = 9
        repeat(columns) { xIndex -> repeat(lines) { yIndex ->
            val point = Offset((xIndex + 1) * size.width / (columns + 1), (yIndex + 1) * size.height / (lines + 1))
            val member = memberships(point, size.width, size.height)
            if (law.left(member)) drawCircle(Green.copy(.8f), 3.5f, point) else drawCircle(Muted.copy(.28f), 2f, point)
        } }
        selectedPoint?.let { point ->
            drawCircle(Amber.copy(.22f), 14f, point)
            drawCircle(Amber, 7f, point)
        }
    }
}

@Composable
private fun LogicGateVisualizer(law: BooleanFormulaLaw, values: Map<String, Boolean>, result: Boolean) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF07111F)).border(1.dp, Violet.copy(.5f), RoundedCornerShape(20.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            law.variables.forEach { variable -> Insight(variable, if (values[variable] == true) "1" else "0", if (values[variable] == true) Cyan else Muted) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TransparentIcon(law.formula.take(3), Violet)
            Text(law.title, color = Violet, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("OUTPUT", color = Muted, fontSize = 9.sp)
            Text(if (result) "1" else "0", color = if (result) Green else Amber, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BooleanFormulaTable(law: BooleanFormulaLaw, rows: List<com.indianservers.aiexplorer.core.BooleanFormulaRow>, selected: Map<String, Boolean>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF07111F)).border(1.dp, Cyan.copy(.32f), RoundedCornerShape(14.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(law.variables.joinToString("  "), color = Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("LHS  RHS  =", color = Violet, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        rows.forEach { row ->
            val active = law.variables.all { row.inputs[it] == selected[it] }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (active) Violet.copy(.18f) else Color.Transparent).padding(6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(law.variables.joinToString("   ") { if (row.inputs[it] == true) "1" else "0" }, color = if (active) Ink else Muted, fontFamily = FontFamily.Monospace)
                Text(" ${if (row.left) 1 else 0}     ${if (row.right) 1 else 0}    ${if (row.equivalent) "✓" else "✗"}", color = if (row.equivalent) Green else Amber, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun ScientificCalculatorScreen(vm: ExplorerViewModel, wide: Boolean) {
    val calculator = remember { ScientificCalculator() }
    val smartCalculator = remember { SmartScientificCalculator(calculator) }
    val professionalCalculator = remember { ProfessionalScientificCalculator() }
    val haptic = LocalHapticFeedback.current
    val editorHistory = remember { CalculatorEditorHistory("sin(30)+log(1000)") }
    var editor by remember { mutableStateOf(TextFieldValue("sin(30)+log(1000)", selection = TextRange(17))) }
    val expression = editor.text
    var angleMode by remember { mutableStateOf(AngleMode.Degrees) }
    var outcome by remember { mutableStateOf<SmartCalculatorOutcome?>(runCatching { smartCalculator.evaluate(expression, angleMode) }.getOrNull()) }
    var evaluatedExpression by remember { mutableStateOf(expression) }
    var error by remember { mutableStateOf<String?>(null) }
    var keyboardLayer by remember { mutableStateOf(CalculatorKeyboardLayer.Basic) }
    var advancedMode by remember { mutableStateOf(AdvancedCalculatorMode.Scientific) }
    var professionalMode by remember { mutableStateOf(ProfessionalCalculatorMode.Matrix) }
    var calculatorPrecision by remember { mutableIntStateOf(8) }
    val advancedCatalog = remember { AdvancedScientificCalculator().examples }
    val professionalCatalog = remember { professionalCalculator.examples }
    val favourites = remember { CalculatorFavourites(listOf("sin(", "cos(", "sqrt(", "pi")) }
    var favouriteKeys by remember { mutableStateOf(favourites.all()) }
    var oneHanded by remember { mutableStateOf(!wide) }
    var calculatorHaptics by remember { mutableStateOf(true) }
    var showRecognition by remember { mutableStateOf(false) }
    var recognitionInput by remember { mutableStateOf("") }
    var recognitionMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    var shiftMode by remember { mutableStateOf(false) }
    var alphaMode by remember { mutableStateOf(false) }
    var conversionValue by remember { mutableStateOf("72") }
    var selectedConversion by remember { mutableStateOf(calculator.conversions.first()) }
    fun setExpression(text: String, cursor: Int = text.length) {
        editorHistory.edit(text); editor = TextFieldValue(text, selection = TextRange(cursor.coerceIn(0, text.length)))
    }
    val appendToken: (String) -> Unit = { token ->
        val edit = CalculatorInputIntelligence.smartInsert(expression, editor.selection.min, editor.selection.max, token)
        setExpression(edit.text, edit.cursor)
    }
    fun evaluate() {
        outcome = runCatching { smartCalculator.evaluate(expression, angleMode, calculatorPrecision) }
            .onSuccess { error = null; evaluatedExpression = expression }
            .onFailure { error = it.message ?: "Expression could not be evaluated" }
            .getOrNull()
    }
    fun moveCursor(delta: Int) {
        val position = (editor.selection.start + delta).coerceIn(0, expression.length)
        editor = editor.copy(selection = TextRange(position))
    }
    fun deleteBackward() {
        val edit = CalculatorInputIntelligence.smartBackspace(expression, editor.selection.min, editor.selection.max)
        setExpression(edit.text, edit.cursor)
    }
    fun toggleCurrentSign() {
        val edit = CalculatorInputIntelligence.toggleSign(expression, editor.selection.min, editor.selection.max)
        setExpression(edit.text, edit.cursor)
    }
    val liveDiagnostics = remember(expression) { CalculatorInputIntelligence.diagnostics(expression) }
    val livePreview = remember(expression, angleMode, calculatorPrecision) {
        if (expression.isBlank() || liveDiagnostics.any { it.error }) null
        else runCatching { calculator.evaluate(CalculatorInputIntelligence.interpret(expression), angleMode, calculatorPrecision) }.getOrNull()
    }
    val shownOutcome = outcome?.takeIf { evaluatedExpression == expression }
    val editorSuggestions = remember(expression, editor.selection.start) { CalculatorInputIntelligence.suggestions(expression, editor.selection.start) }
    fun applySuggestion(suggestion: String) {
        val cursor = editor.selection.start.coerceIn(0, expression.length)
        val prefixStart = expression.substring(0, cursor).indexOfLast { !it.isLetter() } + 1
        val edit = CalculatorInputIntelligence.smartInsert(expression, prefixStart, cursor, suggestion)
        setExpression(edit.text, edit.cursor)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 24.dp else 4.dp, vertical = 6.dp)
            .semantics { contentDescription = "Scientific calculator module" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Back", icon = "←", iconOnly = true, onClick = vm::returnToMathMenu)
            Text("SCIENTIFIC", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Light, letterSpacing = 3.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("History", icon = "↶", iconOnly = true) { showHistory = !showHistory; if (showHistory) showAdvancedTools = true }
                GlowButton("Settings", icon = "⚙", iconOnly = true) { showAdvancedTools = !showAdvancedTools }
            }
        }
        Column(
            Modifier.fillMaxWidth().heightIn(min = 205.dp).shadow(12.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF152B56), Color(0xFF071426), Color(0xFF07101D))))
                .border(1.dp, Color(0xFF5D9DFF), RoundedCornerShape(28.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    angleMode.label.uppercase(), color = Color(0xFF8CB7FF), fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Color(0x334D80D8)).padding(horizontal = 10.dp, vertical = 7.dp),
                )
                BasicTextField(
                    value = editor,
                    onValueChange = { next -> editorHistory.edit(next.text); editor = next },
                    modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = "Smart scientific calculator expression editor" },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color(0xFFA8CAFF), fontSize = if (wide) 24.sp else 19.sp, textAlign = TextAlign.End, fontFamily = FontFamily.Monospace),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF6BA7FF)),
                    visualTransformation = MathSyntaxVisualTransformation(),
                    singleLine = true,
                )
            }
            Text(
                shownOutcome?.exact?.let { "= $it" } ?: livePreview?.exactHint?.let { "= $it" } ?: "Live preview",
                color = Color(0xFF8296BC), fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0x335D9DFF)))
            Text(
                shownOutcome?.primary ?: livePreview?.decimal ?: error ?: "0",
                color = if (liveDiagnostics.none { it.error }) Color(0xFFF4F7FF) else Amber,
                fontSize = if (wide) 60.sp else 50.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            liveDiagnostics.firstOrNull()?.let { diagnostic ->
                Text(diagnostic.message, color = if (diagnostic.error) Amber else Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        AnimatedVisibility(editorSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Complete:", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
                editorSuggestions.forEach { suggestion -> GlowButton(suggestion) { applySuggestion(suggestion) } }
            }
        }
        ScientificReferenceKeypad(
            shiftActive = shiftMode,
            alphaActive = alphaMode,
            onShift = { shiftMode = !shiftMode },
            onAlpha = { alphaMode = !alphaMode },
            onPrevious = { moveCursor(-1) },
            onNext = { moveCursor(1) },
            onMode = { angleMode = if (angleMode == AngleMode.Degrees) AngleMode.Radians else AngleMode.Degrees; evaluate() },
            onToken = { token -> if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) },
            onClear = { setExpression(""); outcome = null; error = null },
            onDelete = ::deleteBackward,
            onToggleSign = ::toggleCurrentSign,
            onCalculate = ::evaluate,
            onSolve = vm::openProblemSolver,
            answer = shownOutcome?.primary ?: livePreview?.decimal ?: "0",
        )
        AnimatedVisibility(showAdvancedTools) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Professional smart tools: exact maths, matrices, vectors, data, probability, units, programmer and finance workflows.", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AngleMode.entries.forEach { mode -> GlowButton(if (angleMode == mode) "${mode.label} active" else mode.label, onClick = { angleMode = mode; evaluate() }) }
            GlowButton("Clear") { setExpression(""); outcome = null; error = null }
            GlowButton("Undo") { setExpression(editorHistory.undo()) }
            GlowButton("Redo") { setExpression(editorHistory.redo()) }
            GlowButton(if (showHistory) "Hide history" else "History") { showHistory = !showHistory }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            TogglePill(if (oneHanded) "One-handed" else "Full width", oneHanded) { oneHanded = !oneHanded }
            TogglePill(if (calculatorHaptics) "Haptics on" else "Haptics off", calculatorHaptics) { calculatorHaptics = !calculatorHaptics }
            TogglePill(if (showRecognition) "Hide import" else "Voice / OCR", showRecognition) { showRecognition = !showRecognition }
        }
        if (showRecognition) {
            IntentAwareMathField(recognitionInput, { recognitionInput = it }, "Voice transcript or recognized maths", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Normalize voice", enabled = recognitionInput.isNotBlank()) {
                    val recognized = CalculatorRecognitionAdapters.voice(recognitionInput)
                    setExpression(recognized.normalized); recognitionMessage = recognized.warnings.firstOrNull() ?: "Voice text normalized. Confirm it, then calculate."
                }
                GlowButton("Clean OCR", enabled = recognitionInput.isNotBlank()) {
                    val recognized = CalculatorRecognitionAdapters.ocr(recognitionInput)
                    setExpression(recognized.normalized); recognitionMessage = recognized.warnings.firstOrNull() ?: "OCR symbols cleaned. Confirm ambiguous characters before calculating."
                }
            }
            recognitionMessage?.let { Text(it, color = Muted, fontSize = 11.sp) }
        }
        IntentAwareMathValueField(
            value = editor, onValueChange = { next -> editorHistory.edit(next.text); editor = next }, label = "Expression",
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Scientific calculator expression input" },
            singleLine = false, minLines = 2,
        )
        val diagnostics = CalculatorInputIntelligence.diagnostics(expression)
        diagnostics.forEach { Text("${it.position?.let { position -> "At ${position + 1}: " }.orEmpty()}${it.message}", color = if (it.error) Amber else Muted, fontSize = 11.sp) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { CalculatorKeyboardLayer.entries.forEach { layer -> TogglePill(layer.name, keyboardLayer == layer) { keyboardLayer = layer } } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { AdvancedCalculatorMode.entries.forEach { mode -> TogglePill(mode.name, advancedMode == mode) { advancedMode = mode } } }
        Text("Professional modes", color = Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { ProfessionalCalculatorMode.entries.forEach { mode -> TogglePill(mode.name, professionalMode == mode) { professionalMode = mode } } }
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Precision $calculatorPrecision", color = Muted, modifier = Modifier.width(110.dp)); Slider(calculatorPrecision.toFloat(), { calculatorPrecision = it.roundToInt().coerceIn(2, 12) }, valueRange = 2f..12f, steps = 9, modifier = Modifier.weight(1f)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CalculatorInputIntelligence.templates[keyboardLayer].orEmpty().forEach { template -> GlowButton(template.label, onClick = {
                val start = editor.selection.min; val text = expression.replaceRange(start, editor.selection.max, template.source); setExpression(text, start + template.source.length - template.cursorBack)
            }) }
        }
        val keypadTokens = when (keyboardLayer) {
            CalculatorKeyboardLayer.Basic -> listOf("7", "8", "9", "/", "4", "5", "6", "*", "1", "2", "3", "-", "0", ".", "pi", "+", "(", ")", "^", "%")
            CalculatorKeyboardLayer.Scientific -> listOf("sin(", "cos(", "tan(", "sec(", "csc(", "cot(", "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "ln(", "log(", "exp(", "!", "e", "pi", "min(", "max(")
            CalculatorKeyboardLayer.Structural -> listOf("x", "y", "a", "b", "=", ",", "[", "]", "f(x)=", "integrate ", " from ", " to ")
        }
        if (favouriteKeys.isNotEmpty()) {
            Text("Favourite keys", color = Muted, fontSize = 11.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { favouriteKeys.forEach { token -> GlowButton("★ $token") { if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) } } }
        }
        FlowRow(
            modifier = if (oneHanded) Modifier.widthIn(max = 360.dp).align(Alignment.End) else Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            keypadTokens.forEach { token ->
                GlowButton(token) { if (calculatorHaptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress); appendToken(token) }
            }
        }
        Text("Customize favourite keys", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("sin(", "cos(", "tan(", "sqrt(", "ln(", "pi", "x", "integrate ").forEach { token ->
                TogglePill(token, token in favouriteKeys) { favouriteKeys = favourites.toggle(token).toList() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Calculate") { evaluate() }
            GlowButton("Open Solver") { vm.openProblemSolver() }
        }
        Text("${advancedMode.name} examples", color = Cyan, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            advancedCatalog[advancedMode].orEmpty().forEach { example -> GlowButton(example, onClick = { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() }) }
        }
        Text("${professionalMode.name} examples", color = Cyan, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            professionalCatalog[professionalMode].orEmpty().forEach { example -> GlowButton(example, onClick = { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() }) }
        }
        error?.let { Text(it, color = Amber, fontSize = 12.sp) }
        outcome?.let {
            SmartCalculatorResultCard(it)
            Text("Send result", color = Cyan, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Graph", enabled = expression.contains("x")) { vm.addFunction(expression); vm.open(MathModule.Graph2D) }
                GlowButton("Solver") { vm.openProblemSolver() }
                GlowButton("Notebook") { vm.submitNotebook(expression); vm.openMathNotebook() }
                GlowButton("Spreadsheet") { vm.openSpreadsheetLab() }
                GlowButton("Probability Lab") { vm.openProbabilityLab() }
            }
        }
        if (showHistory) {
            Text("Editable calculation branches", color = Cyan, fontWeight = FontWeight.Bold)
            smartCalculator.history.asReversed().take(12).forEachIndexed { reverseIndex, entry ->
                val index = smartCalculator.history.lastIndex - reverseIndex
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Color(0x22101824)).clickable { setExpression(smartCalculator.branchFrom(index)) }.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${entry.input}  ->  ${entry.primary}", color = Ink, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("branch ${entry.branch}", color = Muted, fontSize = 9.sp)
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CalculatorConstantsPanel(calculator, onInsert = appendToken)
                CalculatorConversionsPanel(calculator, conversionValue, { conversionValue = it }, selectedConversion, { selectedConversion = it })
            }
        } else {
            CalculatorConstantsPanel(calculator, onInsert = appendToken)
            CalculatorConversionsPanel(calculator, conversionValue, { conversionValue = it }, selectedConversion, { selectedConversion = it })
        }
        Text("Scientific content", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        calculator.referenceCards.forEach { card ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101824))
                    .border(1.dp, Cyan.copy(.18f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(card.title, color = Ink, fontWeight = FontWeight.Bold)
                    MathFormulaText(card.expression, color = Cyan, fontSize = 14.sp)
                }
                Text(card.description, color = Muted, fontSize = 11.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    card.examples.forEach { example -> GlowButton(example) { setExpression(example); outcome = runCatching { smartCalculator.evaluate(example, angleMode, calculatorPrecision) }.getOrNull() } }
                }
            }
        }
            }
        }
    }
}

@Composable
private fun ScientificReferenceKeypad(
    shiftActive: Boolean,
    alphaActive: Boolean,
    onShift: () -> Unit,
    onAlpha: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMode: () -> Unit,
    onToken: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onToggleSign: () -> Unit,
    onCalculate: () -> Unit,
    onSolve: () -> Unit,
    answer: String,
) {
    @Composable
    fun KeyRow(vararg keys: Triple<String, String, Color>) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            keys.forEach { (label, token, accent) ->
                ScientificCalculatorKey(label, accent, Modifier.weight(1f)) {
                    when (token) {
                        "#shift" -> onShift()
                        "#alpha" -> onAlpha()
                        "#left" -> onPrevious()
                        "#right" -> onNext()
                        "#mode" -> onMode()
                        "#clear" -> onClear()
                        "#delete" -> onDelete()
                        "#sign" -> onToggleSign()
                        "#calculate" -> onCalculate()
                        "#solve" -> onSolve()
                        "#answer" -> onToken(answer)
                        else -> onToken(token)
                    }
                }
            }
        }
    }
    val normal = Color(0xFFE1E4EB)
    val operatorBlue = Color(0xFF4595FF)
    val red = Color(0xFFFF5A70)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        KeyRow(
            Triple(if (shiftActive) "SHIFT •" else "SHIFT", "#shift", Color(0xFFFFB52E)),
            Triple(if (alphaActive) "ALPHA •" else "ALPHA", "#alpha", Violet),
            Triple("◀", "#left", normal), Triple("▶", "#right", normal), Triple("MODE", "#mode", normal),
        )
        KeyRow(Triple("CALC", "#calculate", normal), Triple("∫dx", "integrate ", normal), Triple("x!", "!", normal), Triple("(", "(", normal), Triple(")", ")", normal), Triple("AC", "#clear", red))
        KeyRow(Triple("SOLVE", "#solve", normal), Triple("d/dx", "differentiate ", normal), Triple("√", "sqrt(", normal), Triple("xʸ", "^", normal), Triple("log", "log(", normal), Triple("DEL", "#delete", red))
        KeyRow(Triple("sin⁻¹", "asin(", normal), Triple("cos⁻¹", "acos(", normal), Triple("tan⁻¹", "atan(", normal), Triple("log₁₀", "log(", normal), Triple("ln", "ln(", normal), Triple("%", "%", normal))
        KeyRow(Triple("sin", "sin(", normal), Triple("cos", "cos(", normal), Triple("tan", "tan(", normal), Triple("x²", "^2", normal), Triple("x³", "^3", normal), Triple("xʸ", "^", normal))
        KeyRow(Triple("π", "pi", normal), Triple("e", "e", normal), Triple("^", "^", normal), Triple("10ˣ", "10^(", normal), Triple("Ans", "#answer", normal), Triple("EXP", "e^(", normal))
        KeyRow(Triple("7", "7", normal), Triple("8", "8", normal), Triple("9", "9", normal), Triple("÷", "/", operatorBlue), Triple("×", "*", operatorBlue))
        KeyRow(Triple("4", "4", normal), Triple("5", "5", normal), Triple("6", "6", normal), Triple("−", "-", operatorBlue), Triple("+", "+", operatorBlue))
        Row(Modifier.fillMaxWidth().height(118.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(4f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyRow(Triple("1", "1", normal), Triple("2", "2", normal), Triple("3", "3", normal))
                KeyRow(Triple("0", "0", normal), Triple(".", ".", normal), Triple("±", "#sign", normal))
            }
            ScientificCalculatorKey("=", operatorBlue, Modifier.weight(1f).fillMaxHeight(), onCalculate)
        }
    }
}

@Composable
private fun ScientificCalculatorKey(label: String, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 54.dp).border(1.dp, Color(0xFF334867), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (label == "=") Color(0xFF135EF1) else Color(0xFF111C30),
            contentColor = if (label == "=") Color.White else accent,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Text(label, fontSize = if (label.length > 6) 10.sp else 15.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmartCalculatorResultCard(outcome: SmartCalculatorOutcome) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x5520D9FF)).border(1.dp, Cyan.copy(.45f), RoundedCornerShape(16.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(outcome.primary, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        outcome.exact?.let { Insight("Exact", it, Amber) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { outcome.alternatives.forEach { (label, value) -> Insight(label, value, when (label) { "Scientific" -> Violet; "Engineering" -> Green; else -> Cyan }) } }
        Text("Interpreted: ${outcome.interpretedInput}", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        outcome.diagnostics.forEach { Text(it.message, color = if (it.error) Amber else Muted, fontSize = 11.sp) }
        outcome.steps.take(7).forEachIndexed { index, step -> Text("${index + 1}. $step", color = Muted, fontSize = 11.sp) }
        Text("Verification: ${outcome.verification}", color = Green, fontSize = 11.sp)
    }
}

@Composable
private fun CalculatorResultCard(result: ScientificCalculatorResult) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x5520D9FF))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(result.decimal, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        Insight("Scientific", result.scientific, Violet)
        Insight("Engineering", result.engineering, Green)
        result.exactHint?.let { Insight("Exact hint", it, Amber) }
        Text("Normalized: ${result.normalizedExpression}", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        result.warnings.forEach { Text(it, color = Amber, fontSize = 11.sp) }
        result.steps.take(3).forEachIndexed { index, step -> Text("${index + 1}. $step", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun CalculatorConstantsPanel(calculator: ScientificCalculator, onInsert: (String) -> Unit) {
    Column(Modifier.widthIn(min = 240.dp).padding(2.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Constants", color = Cyan, fontWeight = FontWeight.Bold)
        calculator.constants.forEach { constant ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22101824))
                    .clickable { onInsert(constant.key) }
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${constant.label}  ${constant.unit}", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(constant.note, color = Muted, fontSize = 10.sp)
                }
                Text(constant.key, color = Cyan, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun CalculatorConversionsPanel(
    calculator: ScientificCalculator,
    value: String,
    onValueChange: (String) -> Unit,
    selected: com.indianservers.aiexplorer.core.UnitConversion,
    onSelected: (com.indianservers.aiexplorer.core.UnitConversion) -> Unit,
) {
    val numeric = value.toDoubleOrNull()
    val converted = numeric?.let { calculator.convert(it, selected) }
    Column(Modifier.widthIn(min = 240.dp).padding(2.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Conversions", color = Cyan, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text("Value") }, singleLine = true)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            calculator.conversions.forEach { conversion ->
                GlowButton("${conversion.fromUnit}→${conversion.toUnit}") { onSelected(conversion) }
            }
        }
        Insight(selected.title, "${selected.fromUnit} → ${selected.toUnit}", Violet)
        Insight("Factor", trim(selected.factor), Green)
        Insight("Result", converted?.let { "${trim(it)} ${selected.toUnit}" } ?: "Enter a number", Cyan)
        Text(selected.example, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun MathNotebookScreen(vm: ExplorerViewModel, wide: Boolean) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("a := 2") }
    var exactMode by remember { mutableStateOf(true) }
    val cas = remember { SymbolicCasEngine() }
    val casInteraction = remember { CasInteractionEngine(cas) }
    val notebookCas = remember { CasNotebookInteractionEngine(casInteraction, cas) }
    val directCas = remember { CasDirectManipulationEngine(notebookCas, cas) }
    var casInput by remember { mutableStateOf(TextFieldValue("(x+1)^2")) }
    var casOperation by remember { mutableStateOf("expand") }
    var casAssumptions by remember { mutableStateOf(MathAssumptionSet()) }
    var assumptionDraft by remember { mutableStateOf(CasAssumptionDraft()) }
    var assumptionMessage by remember { mutableStateOf("Assumptions are local, explicit and removable.") }
    var showAssumptions by remember { mutableStateOf(false) }
    var casMethod by remember { mutableStateOf(CasSolutionMethod.Auto) }
    var keyboardLayer by remember { mutableStateOf(CasKeyboardLayer.Basic) }
    var expandedCasSteps by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var revealedCasSteps by remember { mutableIntStateOf(1) }
    var casStepDisclosureDepths by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var nextStepHintVisible by remember { mutableStateOf(false) }
    var casSession by remember { mutableStateOf(CasNotebookSession()) }
    var expandedPods by remember { mutableStateOf<Set<String>>(emptySet()) }
    var revealedHints by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var rowStepDisclosureDepths by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var manipulationStates by remember { mutableStateOf<Map<String, CasManipulationState>>(emptyMap()) }
    var scrubValues by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var carriedExpression by remember { mutableStateOf<CasExpressionTransfer?>(null) }
    var importText by remember { mutableStateOf("") }
    var importMessage by remember { mutableStateOf("Paste CSV or TSV to create a notebook-scoped matrix.") }
    var showImport by remember { mutableStateOf(false) }
    var captureMessage by remember { mutableStateOf("Voice, handwriting and photo recognitions are always reviewed before evaluation.") }
    var captureDraft by remember { mutableStateOf("") }
    var variableName by remember { mutableStateOf("a") }
    var variableExpression by remember { mutableStateOf("2") }
    var variableScope by remember { mutableStateOf(CasVariableScope.FollowingRows) }
    var pendingCasImage by remember { mutableStateOf<ByteArray?>(null) }
    var launchCasCamera by remember { mutableStateOf(false) }
    var recognitionCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var recognitionModality by remember { mutableStateOf(CasInputModality.Photo) }
    var showInkPad by remember { mutableStateOf(false) }
    var inkStrokes by remember { mutableStateOf<List<List<MathInkPoint>>>(emptyList()) }
    var inkPadSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val handwritingRecognizer = remember { CasHandwritingRecognizer() }
    DisposableEffect(handwritingRecognizer) { onDispose { handwritingRecognizer.close() } }
    fun recognizePhoto(bytes: ByteArray) {
        captureMessage = "Recognizing the image locally…"
        CasPhotoMathRecognizer.recognize(bytes, onSuccess = { result ->
            recognitionModality = CasInputModality.Photo; recognitionCandidates = result.candidates; captureDraft = result.candidates.first(); captureMessage = "${result.message} Confidence ${(result.confidence * 100).toInt()}%."
        }, onFailure = { captureMessage = it })
    }
    val casPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        pendingCasImage = uri?.let { context.contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() } }
        pendingCasImage?.let(::recognizePhoto)
    }
    val casCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        pendingCasImage = bitmap?.let { image -> java.io.ByteArrayOutputStream().use { output -> image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, output); output.toByteArray() } }
        pendingCasImage?.let(::recognizePhoto)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && launchCasCamera) casCamera.launch(null) else if (!granted) captureMessage = "Camera permission is needed to photograph mathematics."
        launchCasCamera = false
    }
    val voiceCapture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val candidates = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).orEmpty()
        if (result.resultCode == Activity.RESULT_OK && candidates.isNotEmpty()) {
            val capture = CasCaptureNormalizer.normalize(CasInputModality.Voice, candidates.mapIndexed { index, text -> text to (1.0 - index * .12).coerceAtLeast(.4) })
            casInput = TextFieldValue(capture.recognizedText, TextRange(capture.recognizedText.length))
            captureMessage = "Voice recognized at ${(capture.confidence * 100).toInt()}%. ${capture.alternatives.size} alternate(s) remain reviewable."
        }
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceCapture.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4)
        }) else captureMessage = "Microphone permission is needed for voice math input."
    }
    val casStructuredPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "imported_matrix"
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader -> reader.readText() } ?: error("The selected file could not be read.")
            val parsed = CasStructuredImportEngine.structured(name, text, context.contentResolver.getType(uri))
            casSession = notebookCas.import(casSession, parsed.data)
            val expression = parsed.data.name; casInput = TextFieldValue(expression, TextRange(expression.length))
            importMessage = "Imported ${parsed.format}: ${parsed.data.rows} × ${parsed.data.columns} as ${parsed.data.name}. ${parsed.warnings.joinToString()}"
        }.onFailure { importMessage = it.message ?: "The structured file could not be imported." }
    }
    val interpretationOptions = remember(casInput.text, casOperation) { CasInterpretationResolver.options(casInput.text, casOperation) }
    var selectedInterpretationId by remember { mutableStateOf<String?>(null) }
    val selectedInterpretation = interpretationOptions.firstOrNull { it.id == selectedInterpretationId } ?: interpretationOptions.first()
    val dimensionReport = remember(selectedInterpretation.expression) { CasDimensionalAnalyzer.analyze(selectedInterpretation.expression) }
    val casPreview = remember(casInput.text, casOperation, casAssumptions) { casInteraction.interpret(casInput.text, casOperation, casAssumptions) }
    val availableCasMethods = remember(casPreview.operation) { casInteraction.availableMethods(casPreview.operation) }
    val effectiveCasMethod = casMethod.takeIf { it in availableCasMethods } ?: CasSolutionMethod.Auto
    val casRow = remember(casPreview, casAssumptions, effectiveCasMethod) { casInteraction.evaluate(casPreview, casAssumptions, effectiveCasMethod) }
    val examples = listOf(
        "Value" to "a := 2",
        "Dependent" to "b := a^2 + 3",
        "Function" to "f(x) := a*x^2 + b",
        "Reuse cell" to "#1 + 10",
        "Exact" to "1/3 + 1/6",
    )

    fun insertCasText(insertion: String, cursorBack: Int = 0) {
        val start = casInput.selection.min.coerceIn(0, casInput.text.length); val end = casInput.selection.max.coerceIn(start, casInput.text.length)
        val next = casInput.text.replaceRange(start, end, insertion); val cursor = (start + insertion.length - cursorBack).coerceIn(0, next.length)
        casInput = TextFieldValue(next, TextRange(cursor)); revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false
    }

    fun deleteCasBackward() {
        val start = casInput.selection.min.coerceIn(0, casInput.text.length); val end = casInput.selection.max.coerceIn(start, casInput.text.length)
        if (start != end) casInput = TextFieldValue(casInput.text.removeRange(start, end), TextRange(start))
        else if (start > 0) casInput = TextFieldValue(casInput.text.removeRange(start - 1, start), TextRange(start - 1))
    }

    @Composable
    fun InputPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Unified Math Notebook", vm::returnToMathMenu, Cyan, icon = "#")
            Text("Define with := · edit a symbol by defining it again · dependent cells recalculate automatically.", color = Muted, fontSize = 12.sp)
            IntentAwareMathField(
                value = input, onValueChange = { input = it }, label = "Expression or assignment",
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Notebook maths input" },
                placeholder = "f(x) := a*x^2 + 3", singleLine = false, minLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Run cell", enabled = input.isNotBlank()) { vm.submitNotebook(input) }
                GlowButton(if (exactMode) "Exact •" else "Decimal •") { exactMode = !exactMode }
                GlowButton("Clear", enabled = vm.notebookDocument.cells.isNotEmpty(), onClick = vm::clearNotebook)
            }
            Text("Examples", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                examples.forEach { (label, example) -> GlowButton(label) { input = example } }
            }
            Text("Structural maths keyboard", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("frac" to "()/()", "power" to "^()", "root" to "sqrt()", "matrix" to "[[1,2],[3,4]]", "vector" to "<1,2,3>", "system" to "{x+y=1, x-y=3}", "d/dx" to "differentiate ", "integral" to "integrate ", "sum" to "sum()", "limit" to "limit()").forEach { (label, insert) ->
                    GlowButton(label) { input += insert }
                }
            }
            Insight("Named objects", vm.notebookDocument.symbolNames().joinToString().ifBlank { "None yet" }, Violet)
            Insight("Revision", vm.notebookDocument.revision.toString(), Green)
            Text("Cell references use #1, #2… and must point backward. Function cells can be sent directly to Graph.", color = Muted, fontSize = 11.sp)
        }
    }

    @Composable
    fun CellsPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Reactive maths notebook cells" }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Live cells", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${vm.notebookDocument.cells.size} cells", color = Muted, fontSize = 11.sp)
            }
            if (vm.notebookDocument.cells.isEmpty()) {
                Text("Run a value, dependent value, or function. Try a := 2, then b := a^2 + 3.", color = Muted)
            }
            vm.notebookDocument.cells.forEachIndexed { index, cell ->
                val accent = when { cell.error != null -> Amber; cell.kind == NotebookCellKind.Function -> Violet; cell.kind == NotebookCellKind.Scalar -> Cyan; else -> Green }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x44101824))
                        .border(1.dp, accent.copy(.65f), RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${index + 1} · ${cell.kind.label}${cell.symbol?.let { " · $it" } ?: ""}", color = accent, fontWeight = FontWeight.Bold)
                        GlowButton("Delete") { vm.removeNotebookCell(cell.id) }
                    }
                    Text(cell.input, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    if (cell.error != null) {
                        Text(cell.error, color = Amber, fontSize = 12.sp)
                    } else {
                        val output = if (exactMode) cell.exactOutput else cell.decimalOutput ?: cell.exactOutput
                        Text("= $output", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        if (cell.dependencies.isNotEmpty()) Text("Depends on: ${cell.dependencies.joinToString()}", color = Muted, fontSize = 10.sp)
                        if (cell.graphExpression != null) GlowButton("Send ${cell.symbol}(x) to Graph") { vm.graphNotebookFunction(cell) }
                    }
                }
            }
        }
    }

    @Composable
    fun CasPanel(modifier: Modifier = Modifier) {
        LaunchedEffect(casPreview.operation) { casMethod = CasSolutionMethod.Auto; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false }
        GlassPanel(modifier.semantics { contentDescription = "CAS rows with exact and decimal output" }) {
            PanelHeader("CAS Rows", vm::returnToMathMenu, Violet, icon = "CAS")
            Text("Exact CAS: assumptions, algebra, systems, calculus, matrices and verified first-order ODEs share one symbolic tree.", color = Muted, fontSize = 12.sp)
            IntentAwareMathValueField(
                value = casInput,
                onValueChange = { casInput = it; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false },
                label = "CAS expression or question", modifier = Modifier.fillMaxWidth(),
                placeholder = "factor x^2-5*x+6 or eigenvalues [[1,2],[3,4]]", minLines = 2,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Voice math") { audioPermission.launch(Manifest.permission.RECORD_AUDIO) }
                GlowButton(if (showInkPad) "Close handwriting" else "Write mathematics") { showInkPad = !showInkPad }
                GlowButton("Select math photo") { recognitionCandidates = emptyList(); casPhotoPicker.launch("image/*") }
                GlowButton("Capture math photo") { recognitionCandidates = emptyList(); launchCasCamera = true; cameraPermission.launch(Manifest.permission.CAMERA) }
            }
            pendingCasImage?.let { Text("Transient local image · ${it.size} bytes · removed after confirmation", color = Cyan, fontSize = 10.sp) }
            AnimatedVisibility(showInkPad) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Write one line naturally with a finger or stylus", color = Ink, fontWeight = FontWeight.SemiBold)
                    Canvas(
                        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xCC08111C))
                            .border(1.dp, Cyan.copy(.55f), RoundedCornerShape(14.dp)).onSizeChanged { inkPadSize = it }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { start -> inkStrokes = inkStrokes + listOf(listOf(MathInkPoint(start.x, start.y, System.currentTimeMillis()))) },
                                    onDrag = { change, _ -> change.consume(); val next = MathInkPoint(change.position.x, change.position.y, System.currentTimeMillis()); inkStrokes = inkStrokes.dropLast(1) + listOf(inkStrokes.last() + next) },
                                )
                            }.semantics { contentDescription = "Handwriting input pad with ${inkStrokes.size} strokes" },
                    ) {
                        inkStrokes.forEach { stroke -> stroke.zipWithNext().forEach { (a, b) -> drawLine(Cyan, Offset(a.x, a.y), Offset(b.x, b.y), 5f, StrokeCap.Round) } }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Recognize handwriting", enabled = inkStrokes.isNotEmpty()) {
                            captureMessage = "Preparing the offline handwriting model…"
                            handwritingRecognizer.recognize(inkStrokes, inkPadSize.width.toFloat(), inkPadSize.height.toFloat(), casInput.text, onSuccess = { result ->
                                recognitionModality = CasInputModality.Handwriting; recognitionCandidates = result.candidates; captureDraft = result.candidates.first(); captureMessage = "${result.message} Confidence ${(result.confidence * 100).toInt()}%."
                            }, onFailure = { captureMessage = it })
                        }
                        GlowButton("Undo stroke", enabled = inkStrokes.isNotEmpty()) { inkStrokes = inkStrokes.dropLast(1) }
                        GlowButton("Clear ink", enabled = inkStrokes.isNotEmpty()) { inkStrokes = emptyList(); recognitionCandidates = emptyList() }
                    }
                }
            }
            if (recognitionCandidates.isNotEmpty()) {
                Text("Recognition candidates", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    recognitionCandidates.forEach { candidate -> GlowButton(if (captureDraft == candidate) "Selected: $candidate" else candidate) { captureDraft = candidate } }
                }
            }
            if (captureDraft.isNotEmpty() && recognitionCandidates.isNotEmpty()) {
                IntentAwareMathField(captureDraft, { captureDraft = it }, "Recognized handwriting or photo maths", Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton("Confirm recognized mathematics") {
                        val ranked = listOf(captureDraft to .9) + recognitionCandidates.filter { it != captureDraft }.mapIndexed { index, text -> text to (.78 - index * .06).coerceAtLeast(.4) }
                        val capture = CasCaptureNormalizer.normalize(recognitionModality, ranked)
                        casInput = TextFieldValue(capture.recognizedText, TextRange(capture.recognizedText.length)); pendingCasImage = null
                        captureMessage = "Recognized candidate moved to interpretation preview; source image data was released."
                    }
                }
            }
            Text(captureMessage, color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "simplify", "expand", "factor", "partial fractions", "derivative", "integral", "limit", "system", "inequalities",
                    "series", "asymptotic", "sum", "product", "recurrence", "optimization", "exact roots", "domain",
                    "determinant", "rref", "rank", "nullspace", "matrix inverse", "transpose", "eigenvalues", "eigenvectors", "jordan form", "svd", "lu", "qr", "cholesky",
                    "ode", "nonlinear ode", "higher ode", "pde", "laplace", "inverse laplace", "fourier", "inverse fourier", "z transform",
                    "residue", "contour integral", "special functions", "number theory", "finite algebra",
                ).forEach { operation ->
                    GlowButton(if (casPreview.operation == operation) "• $operation" else operation) {
                        casOperation = operation
                        val expression = casPreview.expression
                        casInput = TextFieldValue(expression, TextRange(expression.length))
                    }
                }
                GlowButton("sub x=2") {
                    val substituted = cas.substitute(casInput.text, mapOf("x" to "2")).exact
                    casInput = TextFieldValue(substituted, TextRange(substituted.length))
                    casOperation = "simplify"
                }
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x33101B2A))
                    .border(1.dp, Cyan.copy(.35f), RoundedCornerShape(14.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("INTERPRETATION PREVIEW", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${(casPreview.confidence * 100).toInt()}%", color = if (casPreview.warnings.isEmpty()) Green else Amber, fontSize = 10.sp)
                }
                Text(casPreview.explanation, color = Ink, fontSize = 12.sp)
                Text("Expression: ${casPreview.expression}", color = Green, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("Operation ${casPreview.operation} · variable ${casPreview.variable}", color = Violet, fontSize = 10.sp)
                casPreview.warnings.forEach { Text("Check: $it", color = Amber, fontSize = 10.sp) }
            }
            if (interpretationOptions.size > 1) {
                Text("Choose an interpretation", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    interpretationOptions.forEach { option ->
                        GlowButton(if (selectedInterpretation.id == option.id) "Selected: ${option.label}" else option.label) {
                            selectedInterpretationId = option.id
                        }
                    }
                }
                Text(selectedInterpretation.explanation, color = Muted, fontSize = 10.sp)
            }
            Text(dimensionReport.message, color = if (dimensionReport.valid) Green else Amber, fontSize = 10.sp)
            Text("Solution method", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                availableCasMethods.forEach { method ->
                    GlowButton(if (effectiveCasMethod == method) "Selected: ${method.label}" else method.label) { casMethod = method; revealedCasSteps = 1; expandedCasSteps = emptySet(); casStepDisclosureDepths = emptyMap(); nextStepHintVisible = false }
                }
            }
            Text((if (effectiveCasMethod == CasSolutionMethod.Auto) availableCasMethods.firstOrNull { it != CasSolutionMethod.Auto } else effectiveCasMethod)?.explanation.orEmpty(), color = Muted, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Assumptions", color = Ink, fontWeight = FontWeight.SemiBold)
                GlowButton(if (showAssumptions) "Close editor" else "Edit assumptions") { showAssumptions = !showAssumptions }
            }
            if (casAssumptions.variables.isEmpty()) Text("No assumptions — real variables use guarded identities only.", color = Muted, fontSize = 10.sp)
            else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                casAssumptions.variables.values.sortedBy { it.variable }.forEach { assumption ->
                    GlowButton("${assumption.description()} ×") { casAssumptions = casAssumptions.copy(variables = casAssumptions.variables - assumption.variable) }
                }
            }
            Text("Shared variables", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasVariableScope.entries.forEach { scope -> TogglePill(scope.name, variableScope == scope) { variableScope = scope } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(variableName, { variableName = it.take(16) }, Modifier.weight(.35f), label = { Text("Name") }, singleLine = true)
                OutlinedTextField(variableExpression, { variableExpression = it }, Modifier.weight(.65f), label = { Text("Expression") }, singleLine = true)
            }
            GlowButton("Define scoped variable") {
                runCatching {
                    CasScopedVariable(variableName.trim(), variableExpression.trim(), variableScope, casSession.rows.lastOrNull()?.id)
                }.onSuccess { casSession = notebookCas.define(casSession, it) }.onFailure { assumptionMessage = it.message ?: "Check the variable." }
            }
            if (casSession.variables.isNotEmpty()) Text(casSession.variables.joinToString(" · ") { "${it.name}=${it.expression} [${it.scope.name}]" }, color = Green, fontSize = 10.sp)
            AnimatedVisibility(showAssumptions) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x331D1330)).border(1.dp, Violet.copy(.45f), RoundedCornerShape(14.dp)).padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    OutlinedTextField(assumptionDraft.variable, { assumptionDraft = assumptionDraft.copy(variable = it.take(16)) }, Modifier.fillMaxWidth(), label = { Text("Variable") }, singleLine = true)
                    Text("Domain", color = Muted, fontSize = 10.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MathNumberDomain.entries.forEach { domain -> TogglePill(domain.name, assumptionDraft.domain == domain) { assumptionDraft = assumptionDraft.copy(domain = domain) } }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        TogglePill("positive", assumptionDraft.positive) { assumptionDraft = assumptionDraft.copy(positive = it, nonNegative = if (it) false else assumptionDraft.nonNegative) }
                        TogglePill("non-negative", assumptionDraft.nonNegative) { assumptionDraft = assumptionDraft.copy(nonNegative = it, positive = if (it) false else assumptionDraft.positive) }
                        TogglePill("non-zero", assumptionDraft.nonZero) { assumptionDraft = assumptionDraft.copy(nonZero = it) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(assumptionDraft.minimum, { assumptionDraft = assumptionDraft.copy(minimum = it) }, Modifier.weight(1f), label = { Text("Minimum") }, singleLine = true)
                        OutlinedTextField(assumptionDraft.maximum, { assumptionDraft = assumptionDraft.copy(maximum = it) }, Modifier.weight(1f), label = { Text("Maximum") }, singleLine = true)
                    }
                    GlowButton("Add or update assumption") {
                        runCatching { assumptionDraft.build() }.onSuccess { assumption ->
                            casAssumptions = casAssumptions.with(assumption); assumptionMessage = "Using ${assumption.description()}"; assumptionDraft = assumptionDraft.copy(variable = assumption.variable)
                        }.onFailure { assumptionMessage = it.message ?: "Check the assumption." }
                    }
                    Text(assumptionMessage, color = if (assumptionMessage.startsWith("Using")) Green else Muted, fontSize = 10.sp)
                }
            }
            Text("Full maths keyboard", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasKeyboardLayer.entries.forEach { layer -> GlowButton(if (keyboardLayer == layer) "${layer.label} active" else layer.label) { keyboardLayer = layer } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                CasKeyboardCatalog.layers.getValue(keyboardLayer).forEach { key -> GlowButton(key.label) { insertCasText(key.insertion, key.cursorBack) } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("←") { casInput = casInput.copy(selection = TextRange((casInput.selection.min - 1).coerceAtLeast(0))) }
                GlowButton("→") { casInput = casInput.copy(selection = TextRange((casInput.selection.max + 1).coerceAtMost(casInput.text.length))) }
                GlowButton("Backspace", onClick = ::deleteCasBackward)
                GlowButton("Clear") { casInput = TextFieldValue("") }
            }
            Text("Syntax: systems use {x+y=5; x-y=1}; inequalities use 2*x>=4 and x<5; matrices use [[1,2],[3,4]].", color = Muted, fontSize = 10.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Evaluate as new CAS row", enabled = casInput.text.isNotBlank() && dimensionReport.valid) {
                    casSession = notebookCas.evaluate(
                        casSession.copy(assumptions = casAssumptions), casInput.text, selectedInterpretation,
                        effectiveCasMethod, "cas-${casSession.rows.size + 1}",
                    )
                }
                GlowButton(if (showImport) "Close import" else "Import structured data") { showImport = !showImport }
            }
            AnimatedVisibility(showImport) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CSV · TSV · JSON arrays · whitespace matrices · Matrix Market", color = Cyan, fontSize = 10.sp)
                    GlowButton("Choose structured file") { casStructuredPicker.launch(arrayOf("text/csv", "text/tab-separated-values", "application/json", "text/plain", "application/octet-stream")) }
                    OutlinedTextField(importText, { importText = it }, Modifier.fillMaxWidth(), label = { Text("Paste structured matrix data") }, minLines = 3)
                    GlowButton("Parse pasted data", enabled = importText.isNotBlank()) {
                        runCatching { CasStructuredImportEngine.structured("import_${casSession.imports.size + 1}.txt", importText) }
                            .onSuccess { parsed -> val data = parsed.data; casSession = notebookCas.import(casSession, data); casInput = TextFieldValue(data.name, TextRange(data.name.length)); importMessage = "Imported ${parsed.format}: ${data.rows} × ${data.columns} as ${data.name}." }
                            .onFailure { importMessage = it.message ?: "Could not import this data." }
                    }
                    Text(importMessage, color = if (importMessage.startsWith("Imported")) Green else Muted, fontSize = 10.sp)
                }
            }
            Insight("Operation", casRow.operation, Cyan)
            Insight("Exact", casRow.exact, if (casRow.supported) Green else Amber)
            casRow.decimal?.let { Insight("Decimal", it, Violet) }
            Insight("Assumptions", casRow.assumptions.joinToString().ifBlank { "none" }, Amber)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Step-by-step reasoning", color = Ink, fontWeight = FontWeight.SemiBold)
                Text("${revealedCasSteps.coerceAtMost(casRow.steps.size)}/${casRow.steps.size}", color = Cyan, fontSize = 10.sp)
            }
            casRow.steps.take(revealedCasSteps).forEachIndexed { index, step ->
                val expanded = index in expandedCasSteps
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x33101824))
                        .border(1.dp, (if (expanded) Violet else Cyan).copy(.35f), RoundedCornerShape(12.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}. ${step.title}", color = if (expanded) Violet else Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        GlowButton(if (expanded) "Hide details" else "Explain step") { expandedCasSteps = if (expanded) expandedCasSteps - index else expandedCasSteps + index }
                    }
                    MathFormulaText(step.expression, color = Ink, fontSize = 13.sp)
                    AnimatedVisibility(expanded) {
                        val disclosures = CasStepDisclosureEngine.disclosures(step, index, casRow.steps.size, casRow.assumptions)
                        val depth = casStepDisclosureDepths[index] ?: 1
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            disclosures.take(depth).forEach { disclosure ->
                                Text("${disclosure.level.label} · ${disclosure.title}", color = if (disclosure.revealsIntermediate) Green else Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(disclosure.content, color = Muted, fontSize = 11.sp)
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (depth < disclosures.size) GlowButton("Show more for this step") { casStepDisclosureDepths = casStepDisclosureDepths + (index to depth + 1) }
                                if (depth > 1) GlowButton("Show less") { casStepDisclosureDepths = casStepDisclosureDepths + (index to depth - 1) }
                            }
                        }
                    }
                }
            }
            if (nextStepHintVisible && revealedCasSteps < casRow.steps.size) {
                val next = casRow.steps[revealedCasSteps]
                val hint = CasStepDisclosureEngine.disclosures(next, revealedCasSteps, casRow.steps.size, casRow.assumptions).first()
                Text("Next-step hint: ${hint.content}", color = Green, fontSize = 11.sp)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (revealedCasSteps < casRow.steps.size) GlowButton("Hint before next step") { nextStepHintVisible = true }
                if (revealedCasSteps < casRow.steps.size) GlowButton("Show next step") { revealedCasSteps++; nextStepHintVisible = false }
                if (revealedCasSteps < casRow.steps.size) GlowButton("Reveal all") { revealedCasSteps = casRow.steps.size }
                if (casRow.steps.isNotEmpty()) GlowButton(if (expandedCasSteps.size == casRow.steps.size) "Collapse details" else "Explain all") {
                    expandedCasSteps = if (expandedCasSteps.size == casRow.steps.size) emptySet() else casRow.steps.indices.toSet()
                    revealedCasSteps = casRow.steps.size
                }
            }
            if (casSession.rows.isNotEmpty()) {
                Text("Interactive CAS notebook", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Expressions can be reused, transformed, compared, annotated and exported without flattening the notebook.", color = Muted, fontSize = 10.sp)
            }
            casSession.rows.asReversed().forEach { interactiveRow ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x44101824))
                        .border(1.dp, if (interactiveRow.pinned) Amber.copy(.7f) else Violet.copy(.4f), RoundedCornerShape(14.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    val manipulation = manipulationStates[interactiveRow.id] ?: CasManipulationState(interactiveRow.source)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${interactiveRow.id} · ${interactiveRow.interpretation.operation}", color = Violet, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton(if (interactiveRow.pinned) "Unpin" else "Pin") { casSession = notebookCas.pin(casSession, interactiveRow.id) }
                            GlowButton(if (interactiveRow.id in casSession.compareRowIds) "Comparing" else "Compare") { casSession = notebookCas.compare(casSession, interactiveRow.id) }
                        }
                    }
                    Text(interactiveRow.source, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x3320352D)).border(1.dp, Green.copy(.4f), RoundedCornerShape(10.dp)).padding(8.dp)) {
                        Text("LIVE EXPRESSION", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        MathFormulaText(manipulation.preview?.after ?: manipulation.expression, color = Ink, fontSize = 14.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton("Undo", enabled = manipulation.canUndo) { manipulationStates = manipulationStates + (interactiveRow.id to directCas.undo(manipulation)) }
                            GlowButton("Redo", enabled = manipulation.canRedo) { manipulationStates = manipulationStates + (interactiveRow.id to directCas.redo(manipulation)) }
                            GlowButton("Move expression") { carriedExpression = CasExpressionTransfer(interactiveRow.id, manipulation.expression) }
                            GlowButton("Continue in input") { casInput = TextFieldValue(manipulation.expression, TextRange(manipulation.expression.length)) }
                            carriedExpression?.takeIf { it.sourceRowId != interactiveRow.id }?.let { transfer ->
                                GlowButton(if (manipulation.selectedTarget != null) "Replace selection with ${transfer.sourceRowId}" else "Compose with ${transfer.sourceRowId}") {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.drop(manipulation, transfer, manipulation.selectedTarget != null)); carriedExpression = null
                                }
                            }
                        }
                        directCas.handles(manipulation.expression).take(3).forEach { handle ->
                            val scrubKey = "${interactiveRow.id}:${handle.variable}"; val value = scrubValues[scrubKey] ?: 0f
                            AxisSlider("Scrub ${handle.variable}", value, -10f..10f) { scrubValues = scrubValues + (scrubKey to it) }
                            val scrubbed = directCas.scrub(manipulation.expression, handle.variable, value.toDouble())
                            Text("${handle.variable}=${trim(value.toDouble())} → ${scrubbed.result.exact}", color = Cyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    interactiveRow.pods.forEach { pod ->
                        val key = "${interactiveRow.id}:${pod.kind.name}"; val expanded = key in expandedPods
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x331D1330)).padding(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(pod.title, color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                if (pod.details.isNotEmpty()) GlowButton(if (expanded) "Less" else "Expand") { expandedPods = if (expanded) expandedPods - key else expandedPods + key }
                            }
                            MathFormulaText(pod.primary, color = Ink, fontSize = 13.sp)
                            if (pod.kind == com.indianservers.aiexplorer.core.CasPodKind.Plot) GlowButton("Open beside Graph") {
                                vm.addFunction(interactiveRow.result.exact); vm.open(MathModule.Graph2D)
                            }
                            AnimatedVisibility(expanded) { Column { pod.details.forEach { Text(it, color = Muted, fontSize = 10.sp) } } }
                        }
                    }
                    val hintCount = revealedHints[interactiveRow.id] ?: 0
                    interactiveRow.hints.take(hintCount).forEach { hint -> Text("${hint.title}: ${hint.text}", color = Green, fontSize = 10.sp) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (hintCount < interactiveRow.hints.size) GlowButton("Progressive hint") { revealedHints = revealedHints + (interactiveRow.id to hintCount + 1) }
                        GlowButton("Reuse in input") { val text = notebookCas.transfer(casSession, CasExpressionTransfer(interactiveRow.id, interactiveRow.result.exact), null); casInput = TextFieldValue(text, TextRange(text.length)) }
                        CasExportFormat.entries.forEach { format -> GlowButton(format.name) {
                            if (format == CasExportFormat.Png) {
                                val file = writeShapePng(context, "CAS ${interactiveRow.id}", interactiveRow.result.exact)
                                copyShapeText(context, "CAS PNG", file.absolutePath)
                            } else copyShapeText(context, "CAS ${format.name}", CasExportEngine.export(interactiveRow, format))
                        } }
                    }
                    Text("Derivation", color = Ink, fontWeight = FontWeight.SemiBold)
                    interactiveRow.result.steps.forEachIndexed { stepIndex, step ->
                        val disclosureKey = "${interactiveRow.id}:$stepIndex"
                        val depth = rowStepDisclosureDepths[disclosureKey] ?: 0
                        val disclosures = CasStepDisclosureEngine.disclosures(step, stepIndex, interactiveRow.result.steps.size, interactiveRow.result.assumptions)
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33101824)).padding(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${stepIndex + 1}. ${step.title}", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                GlowButton(if (depth == 0) "Hint" else if (depth < disclosures.size) "Show more" else "Collapse") {
                                    rowStepDisclosureDepths = rowStepDisclosureDepths + (disclosureKey to if (depth >= disclosures.size) 0 else depth + 1)
                                }
                            }
                            disclosures.take(depth).forEach { disclosure ->
                                Text(disclosure.level.label, color = if (disclosure.revealsIntermediate) Green else Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(disclosure.content, color = Muted, fontSize = 10.sp)
                            }
                        }
                    }
                    Text("Try another method", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        casInteraction.availableMethods(interactiveRow.interpretation.operation).filter { it != CasSolutionMethod.Auto }.forEach { method ->
                            GlowButton(if (interactiveRow.method == method) "${method.label} · current" else method.label) {
                                if (interactiveRow.method != method) {
                                    val next = notebookCas.evaluate(casSession, interactiveRow.source, interactiveRow.interpretation, method, "cas-${casSession.rows.size + 1}")
                                    val newId = next.rows.last().id
                                    casSession = next.copy(compareRowIds = setOf(interactiveRow.id, newId))
                                }
                            }
                        }
                    }
                    if (interactiveRow.assumptionWarnings.isNotEmpty()) interactiveRow.assumptionWarnings.forEach { warning ->
                        Text("Step ${warning.stepIndex + 1}: ${warning.message}", color = Amber, fontSize = 10.sp)
                    }
                    OutlinedTextField(interactiveRow.annotation, { note -> casSession = notebookCas.annotate(casSession, interactiveRow.id, note) }, Modifier.fillMaxWidth(), label = { Text("Annotation") })
                    val targets = directCas.targets(manipulation).take(8)
                    if (targets.isNotEmpty()) {
                        Text("Select and manipulate a subexpression", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        targets.forEach { target ->
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                GlowButton(if (manipulation.selectedTarget?.id == target.id) "Selected: ${target.text}" else target.text) {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.select(manipulation, target))
                                }
                            }
                        }
                        manipulation.selectedTarget?.let { selected ->
                            Text("Actions for ${selected.text}", color = Violet, fontSize = 10.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                CasTargetAction.entries.forEach { action -> GlowButton("Preview ${action.name}") {
                                    manipulationStates = manipulationStates + (interactiveRow.id to directCas.preview(manipulation, action))
                                } }
                            }
                        }
                        manipulation.preview?.let { preview ->
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x44251B35)).padding(8.dp)) {
                                Text("PREVIEW · ${preview.action.name}", color = Violet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("Before: ${preview.before}", color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text("After: ${preview.after}", color = Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                preview.domain.descriptions.forEach { Text(it, color = Amber, fontSize = 9.sp) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    GlowButton("Commit transformation") { manipulationStates = manipulationStates + (interactiveRow.id to directCas.commit(manipulation)) }
                                    GlowButton("Cancel preview") { manipulationStates = manipulationStates + (interactiveRow.id to directCas.cancel(manipulation)) }
                                }
                            }
                        }
                    }
                }
            }
            if (casSession.compareRowIds.size > 1) {
                val compared = casSession.rows.filter { it.id in casSession.compareRowIds }
                Insight("Pinned comparison", compared.joinToString("  ↔  ") { "${it.id}: ${it.result.exact}" }, Amber)
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp)) {
            InputPanel(Modifier.weight(.34f).fillMaxHeight())
            CellsPanel(Modifier.weight(.36f).fillMaxHeight())
            CasPanel(Modifier.weight(.30f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            InputPanel(Modifier.fillMaxWidth())
            CellsPanel(Modifier.fillMaxWidth())
            CasPanel(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MathKnowledgeScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf<KnowledgeTopic?>(null) }
    var level by remember { mutableStateOf<KnowledgeLevel?>(null) }
    var formulaCategory by remember { mutableStateOf<FormulaCategory?>(null) }
    var dictionaryInitial by rememberSaveable { mutableStateOf<Char?>(null) }
    var dictionaryClassBand by rememberSaveable { mutableStateOf<DictionaryClassBand?>(null) }
    var dictionaryDifficulty by rememberSaveable { mutableStateOf<DictionaryDifficulty?>(null) }
    var answers by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var quizSubject by remember { mutableStateOf(QuizSubject.Maths) }
    var quizLevel by remember { mutableStateOf(QuizLevel.Basic) }
    var quizSession by remember { mutableStateOf<QuizSession?>(null) }
    val visualProofEngine = remember { VisualProofEngine() }
    var visualProofPlayback by remember { mutableStateOf(visualProofEngine.start(VisualProofCatalog.labs.first().id)) }
    var visualProofCategory by remember { mutableStateOf("Geometry") }
    var visualProofSubcategory by remember { mutableStateOf(VisualProofCatalog.subcategoriesFor("Geometry").first().name) }
    var proofWorkspaceOpen by remember { mutableStateOf(false) }
    var knowledgeFiltersOpen by remember { mutableStateOf(false) }
    var proofCategoriesOpen by remember { mutableStateOf(false) }
    var proofFormulasOpen by remember { mutableStateOf(true) }
    var proofExplanationOpen by remember { mutableStateOf(false) }
    var proofControlsOpen by remember { mutableStateOf(false) }
    var proofResultsOpen by remember { mutableStateOf(false) }
    var knowledgeLoading by remember { mutableStateOf(true) }
    LaunchedEffect(visualProofPlayback.playing) {
        while (visualProofPlayback.playing) {
            delay(850)
            visualProofPlayback = visualProofEngine.next(visualProofPlayback)
        }
    }
    val result = remember(query, topic, level, formulaCategory, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty, vm.activeKnowledgeSection) {
        if (vm.activeKnowledgeSection == KnowledgeSection.Dictionary) {
            KnowledgeSearchResult(
                formulas = emptyList(),
                theorems = emptyList(),
                visualProofs = emptyList(),
                dictionary = MathDictionaryCatalog.search(query, topic, level, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty),
                mcqs = emptyList(),
            )
        } else {
            MathKnowledgeCatalog.search(query, topic, level, formulaCategory)
        }
    }
    LaunchedEffect(query, topic, level, formulaCategory, dictionaryInitial, dictionaryClassBand, dictionaryDifficulty, vm.activeKnowledgeSection) {
        knowledgeLoading = true
        // Keep at least one composed frame so a destination/filter change never flashes blank.
        delay(90)
        knowledgeLoading = false
    }

    @Composable
    fun Filters() {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Knowledge Intelligence", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            GlowButton(if (knowledgeFiltersOpen) "Close filters" else "Open filters") { knowledgeFiltersOpen = !knowledgeFiltersOpen }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KnowledgeSection.entries.forEach { section ->
                GlowButton(if (vm.activeKnowledgeSection == section) "• ${section.title}" else section.title) {
                    vm.openKnowledgeHub(section)
                }
            }
        }
        AnimatedVisibility(knowledgeFiltersOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Search and filter the knowledge library", color = Muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Knowledge search input" },
                    label = { Text("Search maths knowledge") },
                    placeholder = { Text("Try derivative, normal, Bayes, eigenvector") },
                    singleLine = true,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (topic == null) "All topics" else "All") { topic = null }
                    KnowledgeTopic.entries.forEach { item -> GlowButton(if (topic == item) "• ${item.label}" else item.label) { topic = item } }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (level == null) "All levels" else "All") { level = null }
                    KnowledgeLevel.entries.forEach { item -> GlowButton(if (level == item) "• ${item.label}" else item.label) { level = item } }
                }
                if (vm.activeKnowledgeSection == KnowledgeSection.Formulas || vm.activeKnowledgeSection == KnowledgeSection.Visualize) {
                    Text("Formula categories", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton(if (formulaCategory == null) "• All 15" else "All 15") { formulaCategory = null }
                        FormulaCategory.entries.forEach { category ->
                            GlowButton(if (formulaCategory == category) "• ${category.label}" else category.label) { formulaCategory = category }
                        }
                    }
                }
                if (vm.activeKnowledgeSection == KnowledgeSection.Dictionary) {
                    Text("A–Z", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (dictionaryInitial == null) "• All" else "All") { dictionaryInitial = null }
                        ('A'..'Z').forEach { letter ->
                            GlowButton(if (dictionaryInitial == letter) "• $letter" else letter.toString()) { dictionaryInitial = letter }
                        }
                    }
                    Text("Class", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (dictionaryClassBand == null) "• All classes" else "All classes") { dictionaryClassBand = null }
                        DictionaryClassBand.entries.forEach { band -> GlowButton(if (dictionaryClassBand == band) "• ${band.label}" else band.label) { dictionaryClassBand = band } }
                    }
                    Text("Difficulty", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (dictionaryDifficulty == null) "• All difficulties" else "All difficulties") { dictionaryDifficulty = null }
                        DictionaryDifficulty.entries.forEach { difficulty -> GlowButton(if (dictionaryDifficulty == difficulty) "• ${difficulty.label}" else difficulty.label) { dictionaryDifficulty = difficulty } }
                    }
                }
                Insight("Matches", "${result.total}", Green)
                Insight("Coverage", "${FormulaCategory.entries.size} formula categories · ${MathKnowledgeCatalog.formulas.size} formulas", Violet)
            }
        }
    }

    @Composable
    fun Content(modifier: Modifier = Modifier) {
        GlassPanel(modifier.fillMaxSize().semantics { contentDescription = "Maths knowledge content" }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(vm.activeKnowledgeSection.title, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${result.total} found", color = Muted, fontSize = 11.sp)
            }
            if (knowledgeLoading) {
                KnowledgeLoadingSkeleton(vm.activeKnowledgeSection.title)
            } else when (vm.activeKnowledgeSection) {
                KnowledgeSection.Formulas -> result.formulas.forEach { formula ->
                    KnowledgeCard(formula.title, formula.expression, "${formula.category.label} · ${formula.level.label}", formula.useCase, Cyan)
                    if (formula.variables.isNotEmpty()) Text("Variables: ${formula.variables.joinToString()}", color = Muted, fontSize = 11.sp)
                }
                KnowledgeSection.Theorems -> result.theorems.forEach { theorem ->
                    KnowledgeCard(theorem.title, theorem.statement, "${theorem.topic.label} · ${theorem.level.label}", "Applications: ${theorem.applications.joinToString()}", Violet)
                    Text("Proof sketch: ${theorem.proofSketch.joinToString(" -> ")}", color = Muted, fontSize = 11.sp)
                }
                KnowledgeSection.Visualize -> {
                    result.formulas.take(5).forEach { formula ->
                        KnowledgeCard(formula.title, formula.expression, "Visual formula", "Open graph/notebook and connect variables: ${formula.variables.joinToString()}", Green)
                    }
                    result.visualProofs.forEach { proof ->
                        KnowledgeCard(proof.title, proof.invariant, proof.workspace.label, proof.learnerPrompt, Cyan)
                    }
                }
                KnowledgeSection.Proofs -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Verified Visual Proofs", color = Cyan, fontWeight = FontWeight.Bold)
                            Text("${VisualProofCatalog.categories.size} categories · ${VisualProofCatalog.labs.size} interactive formulas", color = Green, fontSize = 11.sp)
                        }
                        GlowButton(if (proofCategoriesOpen) "Close categories" else "Open categories") { proofCategoriesOpen = !proofCategoriesOpen }
                    }
                    AnimatedVisibility(proofCategoriesOpen) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            VisualProofCatalog.categories.forEach { category ->
                                GlowButton(if (visualProofCategory == category) "• $category" else category) {
                                    visualProofCategory = category
                                    visualProofSubcategory = VisualProofCatalog.subcategoriesFor(category).first().name
                                    proofCategoriesOpen = false
                                    proofFormulasOpen = true
                                    proofWorkspaceOpen = false
                                }
                            }
                        }
                    }
                    val proofSubcategories = VisualProofCatalog.subcategoriesFor(visualProofCategory)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(visualProofCategory, color = Violet, fontWeight = FontWeight.Bold)
                            Text("Choose a subcategory, then tap a formula", color = Muted, fontSize = 11.sp)
                        }
                        GlowButton(if (proofFormulasOpen) "Close library" else "Open library") { proofFormulasOpen = !proofFormulasOpen }
                    }
                    AnimatedVisibility(proofFormulasOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                proofSubcategories.forEach { group ->
                                    GlowButton(if (visualProofSubcategory == group.name) "• ${group.name}" else group.name) {
                                        visualProofSubcategory = group.name
                                        proofWorkspaceOpen = false
                                    }
                                }
                            }
                            val selectedGroup = proofSubcategories.first { it.name == visualProofSubcategory }
                            Text(selectedGroup.description, color = Muted, fontSize = 12.sp)
                            VisualProofCatalog.labsFor(visualProofCategory, visualProofSubcategory).forEach { lab ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(SurfaceB.copy(alpha = .55f))
                                        .border(1.dp, Violet.copy(alpha = .42f), RoundedCornerShape(13.dp))
                                        .clickable {
                                            visualProofPlayback = visualProofEngine.start(lab.id)
                                            proofWorkspaceOpen = true
                                            proofFormulasOpen = false
                                            proofExplanationOpen = false
                                            proofControlsOpen = true
                                            proofResultsOpen = true
                                        }
                                        .padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    MathFormulaText(lab.formalResult, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                    Text(lab.title, color = Ink, fontSize = 12.sp)
                                    Text("Tap to open interactive proof", color = Green, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    if (!proofWorkspaceOpen && !proofFormulasOpen) {
                        Text("Open the formula library to select another interactive proof.", color = Green, fontSize = 12.sp)
                    }
                    AnimatedVisibility(proofWorkspaceOpen) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val proofCertificate = VisualProofCatalog.certificateFor(visualProofPlayback.frame.lab.id)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("$visualProofCategory › $visualProofSubcategory", color = Muted, fontSize = 10.sp)
                                    MathFormulaText(visualProofPlayback.frame.lab.formalResult, color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                GlowButton("Close proof") { proofWorkspaceOpen = false; proofFormulasOpen = true }
                            }
                            InteractiveVisualProofCanvas(visualProofPlayback) { name, value ->
                                visualProofPlayback = visualProofEngine.setParameter(visualProofPlayback, name, value)
                            }
                            KnowledgeCard(visualProofPlayback.frame.lab.title, visualProofPlayback.frame.lab.formalResult, visualProofPlayback.frame.lab.topic, visualProofPlayback.frame.lab.steps[visualProofPlayback.frame.step], Green)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                GlowButton(if (visualProofPlayback.playing) "Pause" else "Play") { visualProofPlayback = visualProofEngine.togglePlaying(visualProofPlayback) }
                                GlowButton("Next step") { visualProofPlayback = visualProofEngine.next(visualProofPlayback) }
                                GlowButton(if (proofExplanationOpen) "Close reasoning" else "Why it works") { proofExplanationOpen = !proofExplanationOpen }
                                GlowButton(if (proofControlsOpen) "Hide controls" else "Show controls") { proofControlsOpen = !proofControlsOpen }
                                GlowButton(if (proofResultsOpen) "Hide results" else "Show results") { proofResultsOpen = !proofResultsOpen }
                            }
                            AnimatedVisibility(proofExplanationOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KnowledgeCard("Why this proves it", proofCertificate.argument.joinToString(" "), proofCertificate.method, "Assumptions: ${proofCertificate.assumptions.joinToString()}", Violet)
                                    Text("What changes? ${visualProofPlayback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
                                    Text("What stays same? ${visualProofPlayback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
                                }
                            }
                            AnimatedVisibility(proofControlsOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    visualProofPlayback.frame.lab.parameters.forEach { parameter ->
                                        AxisSlider(parameter.name, visualProofPlayback.frame.parameters.getValue(parameter.name).toFloat(), parameter.minimum.toFloat()..parameter.maximum.toFloat()) {
                                            visualProofPlayback = visualProofEngine.setParameter(visualProofPlayback, parameter.name, it.toDouble())
                                        }
                                    }
                                }
                            }
                            AnimatedVisibility(proofResultsOpen) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    visualProofPlayback.frame.measurements.forEach { (name, value) -> Insight(name, trim(value), Violet) }
                                    Insight("Invariant", "${visualProofPlayback.frame.invariant} · residual ${trim(visualProofPlayback.frame.residual)}", if (visualProofPlayback.frame.holds) Green else Amber)
                                }
                            }
                        }
                    }
                }
                KnowledgeSection.Dictionary -> result.dictionary.forEach { term ->
                    DictionaryTermDiagram(term)
                    KnowledgeCard(term.term, term.definition, "${term.topic.label} · ${term.classBands.joinToString { it.label }} · ${term.difficulty.label}", "${term.notation}\n✓ Example: ${term.example}\n✕ Non-example: ${term.nonExample}", Amber)
                }
                KnowledgeSection.Mcqs -> {
                    QuizDashboard(
                        subject = quizSubject,
                        level = quizLevel,
                        session = quizSession,
                        questionBankSize = MathKnowledgeCatalog.mcqs.size,
                        onSubject = { quizSubject = it; quizSession = null },
                        onLevel = { quizLevel = it; quizSession = null },
                        onStart = {
                            answers = emptyMap()
                            quizSession = QuizEngine.start(MathKnowledgeCatalog.mcqs, quizSubject, quizLevel)
                        },
                        onAnswer = { choice ->
                            quizSession = quizSession?.let { QuizEngine.answer(it, choice) }
                        },
                        onRestart = {
                            answers = emptyMap()
                            quizSession = QuizEngine.start(MathKnowledgeCatalog.mcqs, quizSubject, quizLevel)
                        },
                    )
                }
            }
            if (!knowledgeLoading && result.total == 0) Text("No matches yet. Clear filters or search a broader term.", color = Amber)
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(top = 74.dp, bottom = 72.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPanel(Modifier.weight(.36f).fillMaxHeight()) { Filters() }
            Content(Modifier.weight(.64f).fillMaxHeight())
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(top = 70.dp, bottom = 70.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Content(Modifier.fillMaxWidth().weight(1f))
            GlassPanel(Modifier.fillMaxWidth().heightIn(max = 210.dp)) { Filters() }
        }
    }
}

@Composable
private fun KnowledgeLoadingSkeleton(section: String) {
    Column(
        Modifier.fillMaxWidth().semantics { contentDescription = "Loading $section content" },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Preparing $section…", color = Muted, fontSize = 11.sp)
        repeat(3) { index ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(SurfaceB.copy(alpha = .55f)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.fillMaxWidth(if (index == 1) .55f else .72f).height(16.dp).clip(RoundedCornerShape(8.dp)).background(Cyan.copy(alpha = .16f)))
                Box(Modifier.fillMaxWidth().height(if (section == KnowledgeSection.Dictionary.title) 58.dp else 12.dp).clip(RoundedCornerShape(7.dp)).background(Ink.copy(alpha = .09f)))
                Box(Modifier.fillMaxWidth(.82f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(Ink.copy(alpha = .07f)))
                Box(Modifier.fillMaxWidth(.64f).height(10.dp).clip(RoundedCornerShape(6.dp)).background(Ink.copy(alpha = .07f)))
            }
        }
    }
}

@Composable
private fun DictionaryTermDiagram(term: DictionaryTerm) {
    val explanation = when (term.term) {
        "Discriminant" -> "A parabola crossing the axis twice: a positive discriminant gives two real roots."
        "Limit" -> "Points from both sides approach the same open target point."
        "Derivative" -> "The tangent line shows the curve's instantaneous slope."
        "Median" -> "The highlighted centre point divides the ordered values."
        "Posterior" -> "Evidence updates a prior probability into a posterior."
        "Eigenvector" -> "The transformation stretches the vector but preserves its direction."
        else -> "A visual explanation of ${term.term}."
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x44101824)).border(1.dp, Amber.copy(alpha = .35f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Picture it", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Canvas(
            Modifier.fillMaxWidth().height(82.dp).semantics {
                contentDescription = "${term.term} explanatory diagram: $explanation"
            },
        ) {
            val w = size.width
            val h = size.height
            val grid = Ink.copy(alpha = .08f)
            for (i in 1..5) drawLine(grid, Offset(w * i / 6f, 0f), Offset(w * i / 6f, h), 1f)
            for (i in 1..3) drawLine(grid, Offset(0f, h * i / 4f), Offset(w, h * i / 4f), 1f)
            when (term.term) {
                "Discriminant" -> {
                    drawLine(Ink.copy(.55f), Offset(w * .08f, h * .72f), Offset(w * .92f, h * .72f), 2f)
                    val path = Path().apply {
                        moveTo(w * .18f, h * .18f)
                        quadraticBezierTo(w * .5f, h * 1.16f, w * .82f, h * .18f)
                    }
                    drawPath(path, Cyan, style = Stroke(4f))
                    drawCircle(Green, 6f, Offset(w * .31f, h * .72f))
                    drawCircle(Green, 6f, Offset(w * .69f, h * .72f))
                }
                "Limit" -> {
                    drawLine(Ink.copy(.5f), Offset(w * .5f, h * .12f), Offset(w * .5f, h * .88f), 2f)
                    listOf(.14f, .25f, .35f, .43f, .57f, .65f, .75f, .86f).forEach { x ->
                        val distance = kotlin.math.abs(x - .5f)
                        drawCircle(Cyan.copy(alpha = 1f - distance), 5f, Offset(w * x, h * (.48f + distance * .55f)))
                    }
                    drawCircle(Green, 8f, Offset(w * .5f, h * .48f), style = Stroke(3f))
                }
                "Derivative" -> {
                    val curve = Path().apply {
                        moveTo(w * .08f, h * .78f)
                        cubicTo(w * .34f, h * .78f, w * .48f, h * .15f, w * .9f, h * .26f)
                    }
                    drawPath(curve, Cyan, style = Stroke(4f))
                    drawLine(Green, Offset(w * .25f, h * .82f), Offset(w * .78f, h * .12f), 4f, StrokeCap.Round)
                    drawCircle(Amber, 6f, Offset(w * .5f, h * .49f))
                }
                "Median" -> {
                    drawLine(Cyan, Offset(w * .1f, h * .55f), Offset(w * .9f, h * .55f), 4f, StrokeCap.Round)
                    (1..7).forEach { i ->
                        drawCircle(if (i == 4) Green else Ink, if (i == 4) 8f else 5f, Offset(w * (.1f + i * .1f), h * .55f))
                    }
                    drawLine(Green, Offset(w * .5f, h * .22f), Offset(w * .5f, h * .82f), 3f)
                }
                "Posterior" -> {
                    drawRect(Cyan.copy(.35f), Offset(w * .08f, h * .22f), Size(w * .84f, h * .2f))
                    drawRect(Cyan, Offset(w * .08f, h * .22f), Size(w * .55f, h * .2f))
                    drawRect(Green.copy(.35f), Offset(w * .08f, h * .6f), Size(w * .84f, h * .2f))
                    drawRect(Green, Offset(w * .08f, h * .6f), Size(w * .72f, h * .2f))
                    drawLine(Amber, Offset(w * .67f, h * .38f), Offset(w * .76f, h * .58f), 4f, StrokeCap.Round)
                }
                else -> {
                    drawLine(Ink.copy(.5f), Offset(w * .12f, h * .78f), Offset(w * .88f, h * .22f), 2f)
                    drawLine(Cyan, Offset(w * .18f, h * .72f), Offset(w * .52f, h * .47f), 7f, StrokeCap.Round)
                    drawLine(Green, Offset(w * .52f, h * .47f), Offset(w * .84f, h * .24f), 7f, StrokeCap.Round)
                    drawCircle(Amber, 7f, Offset(w * .52f, h * .47f))
                }
            }
        }
        Text(explanation, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun KnowledgeCard(title: String, body: String, meta: String, detail: String, accent: Color) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, accent.copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = accent, fontWeight = FontWeight.Bold)
        MathFormulaText(body, color = Ink, fontSize = 16.sp)
        Text(meta, color = Muted, fontSize = 11.sp)
        Text(detail, color = Ink, fontSize = 12.sp)
    }
}

internal fun latexStyleFormula(source: String): String {
    val superscript = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴', '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾', 'n' to 'ⁿ', 'i' to 'ⁱ', 'x' to 'ˣ', 'y' to 'ʸ', 'T' to 'ᵀ',
    )
    val subscript = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄', '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎', 'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ', 'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ', 'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ', 'v' to 'ᵥ', 'x' to 'ₓ',
    )
    fun styled(value: String, alphabet: Map<Char, Char>): String = value.map { alphabet[it] ?: it }.joinToString("")
    fun applyScript(input: String, marker: Char, alphabet: Map<Char, Char>): String {
        val literalMarker = Regex.escape(marker.toString())
        val braced = Regex("$literalMarker\\{([^}]*)\\}").replace(input) { styled(it.groupValues[1], alphabet) }
        val parenthesized = Regex("$literalMarker\\(([^)]*)\\)").replace(braced) { styled("(${it.groupValues[1]})", alphabet) }
        return Regex("$literalMarker(-?\\d+|[A-Za-z])").replace(parenthesized) { styled(it.groupValues[1], alphabet) }
    }

    var result = source
        .replace("<=>", "⇔")
        .replace("<=", "≤")
        .replace(">=", "≥")
        .replace("!=", "≠")
        .replace("->", "→")
        .replace(Regex("(?i)\\bsqrt\\s*"), "√")
        .replace(Regex("(?i)\\bpi(?=_|\\b)"), "π")
        .replace(Regex("(?i)\\btheta(?=_|\\b)"), "θ")
        .replace(Regex("(?i)\\blambda(?=_|\\b)"), "λ")
        .replace(Regex("(?i)\\bphi(?=_|\\b)"), "φ")
        .replace(Regex("(?i)\\bdelta(?=_|\\b)"), "Δ")
    result = applyScript(result, '^', superscript)
    result = applyScript(result, '_', subscript)
    return result
        .replace("1/2", "½")
        .replace("1/3", "⅓")
        .replace("2/3", "⅔")
        .replace("1/4", "¼")
        .replace("3/4", "¾")
        .replace("/", "⁄")
        .replace(" * ", " × ")
        .replace(" x ", " × ")
}

@Composable
private fun MathFormulaText(
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

internal val InteractiveVisualProofSceneIds = setOf(
    "triangle-angle-sum", "pythagorean", "derivative-slope", "integral-area", "normal-area",
    "vector-addition", "matrix-transform", "circle-ratio", "algebra-square", "shear-area",
    "triangle-area", "parallelogram-area", "trapezoid-area", "circle-area", "polygon-angle-sum",
    "similar-triangles", "intersecting-chords", "circle-angle", "unit-circle-identity", "odd-sum-square",
    "absolute-inequality", "equation-balance", "set-de-morgan", "epsilon-delta", "slope-triangle",
    "eigenvector-direction", "counting-paths", "modular-clock",
)

@Composable
private fun InteractiveVisualProofCanvas(
    playback: com.indianservers.aiexplorer.core.ProofPlayback,
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
            val scale = minOf(w / 12f, h / 8f)
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
                    if (frame.step >= 2) {
                        val y = h * .18f; drawLine(Ink, Offset(w * .2f, y), Offset(w * .8f, y), 4f)
                        drawArc(Violet, 0f, 55f, false, Offset(w * .37f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                        drawArc(Amber, 55f, 70f, false, Offset(w * .46f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                        drawArc(Green, 125f, 55f, false, Offset(w * .55f, y - 25f), Size(50f, 50f), style = Stroke(7f))
                        label("A + B + C = 180°", Offset(w * .34f, y - 38f), Green)
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
                    val r = frame.parameters.getValue("r"); val radius = (r * scale * .55).toFloat().coerceIn(28f, h * .27f)
                    val center = Offset(w * .28f, h * .53f)
                    drawCircle(Cyan.copy(.13f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(4f)); drawLine(Amber, center, center + Offset(radius, 0f), 5f)
                    val length = (2 * PI * radius).toFloat().coerceAtMost(w * .58f)
                    drawLine(Violet, Offset(w * .38f, h * .73f), Offset(w * .38f + length, h * .73f), 7f, cap = StrokeCap.Round)
                    label("C = 2πr", Offset(w * .52f, h * .68f), Violet); label("d = 2r", Offset(w * .17f, h * .88f), Amber)
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
                    val curve = Path().apply { moveTo(left, bottom); lineTo(right, top) }; drawPath(curve, Cyan, style = Stroke(5f))
                    repeat(visualN) { i ->
                        val x0 = left + i * chartWidth / visualN; val x1 = left + (i + 1) * chartWidth / visualN; val midpoint = (i + .5f) / visualN; val y = bottom - midpoint * chartHeight
                        drawRect(if (i % 2 == 0) Violet.copy(.18f) else Green.copy(.18f), Offset(x0, y), Size(x1 - x0, bottom - y)); drawRect(if (i % 2 == 0) Violet else Green, Offset(x0, y), Size(x1 - x0, bottom - y), style = Stroke(1.5f))
                    }
                    label("${requestedN} midpoint rectangles", Offset(w * .5f, h * .9f), Amber); label("∫₀ᵇx dx = b²/2 = ${trim(bound * bound / 2)}", Offset(w * .38f, h * .13f), Green)
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
                    label("same shaded region", Offset(w * .36f, h * .12f), Green, 25f)
                    label("A B | ¬(A∨B) | ¬A∧¬B", Offset(w * .23f, h * .82f), Ink, 20f)
                    label("0 0 |     1      |     1", Offset(w * .3f, h * .89f), Green, 19f)
                    label("other rows: 0 = 0", Offset(w * .38f, h * .95f), Muted, 18f)
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
                    val lambda = frame.parameters.getValue("lambda"); val other = frame.parameters.getValue("other"); val origin = Offset(w * .5f, h * .56f); val unit = minOf(w, h) * .12f
                    (-4..4).forEach { i ->
                        drawLine(Grid.copy(.45f), Offset(origin.x + i * unit, 20f), Offset(origin.x + i * unit, h - 30f), 1.5f)
                        drawLine(Grid.copy(.45f), Offset(20f, origin.y + i * unit), Offset(w - 20f, origin.y + i * unit), 1.5f)
                    }
                    drawLine(Violet.copy(.35f), Offset(20f, origin.y), Offset(w - 20f, origin.y), 13f)
                    fun vectorArrow(end: Offset, color: Color, name: String) {
                        drawLine(color, origin, end, 7f, cap = StrokeCap.Round); val delta = end - origin; val direction = delta / delta.getDistance().coerceAtLeast(1f); val normal = Offset(-direction.y, direction.x)
                        polygon(listOf(end, end - direction * 24f + normal * 11f, end - direction * 24f - normal * 11f), color, color); label(name, end + Offset(8f, -8f), color)
                    }
                    vectorArrow(origin + Offset(unit * 1.35f, 0f), Cyan, "v")
                    vectorArrow(origin + Offset((unit * 1.35f * lambda).toFloat(), 0f), Green, "Av=λv")
                    drawLine(Amber.copy(.55f), origin, origin + Offset(0f, (-unit * other).toFloat()), 4f)
                    label("direction line preserved", Offset(w * .57f, h * .18f), Green, 24f); label("other axis ×${trim(other)}", Offset(w * .58f, h * .3f), Amber, 19f)
                }
                "counting-paths" -> {
                    val rightSteps = frame.parameters.getValue("right").toInt().coerceIn(1, 6); val upSteps = frame.parameters.getValue("up").toInt().coerceIn(1, 6)
                    val left = w * .12f; val bottom = h * .76f; val cell = minOf((w * .48f) / rightSteps, (h * .55f) / upSteps)
                    for (i in 0..rightSteps) drawLine(Grid, Offset(left + i * cell, bottom), Offset(left + i * cell, bottom - upSteps * cell), 2f)
                    for (j in 0..upSteps) drawLine(Grid, Offset(left, bottom - j * cell), Offset(left + rightSteps * cell, bottom - j * cell), 2f)
                    for (i in 0..rightSteps) for (j in 0..upSteps) drawCircle(if (i == rightSteps && j == upSteps) Green else Cyan, if (i == rightSteps && j == upSteps) 8f else 4f, Offset(left + i * cell, bottom - j * cell))
                    val path = Path().apply { moveTo(left, bottom); repeat(rightSteps) { lineTo(left + (it + 1) * cell, bottom) }; repeat(upSteps) { lineTo(left + rightSteps * cell, bottom - (it + 1) * cell) } }; drawPath(path, Amber, style = Stroke(5f, cap = StrokeCap.Round))
                    val treeX = w * .72f; val treeY = h * .27f; drawCircle(Violet, 7f, Offset(treeX, treeY)); listOf(-1f, 1f).forEach { side -> drawLine(Violet, Offset(treeX, treeY), Offset(treeX + side * 58f, treeY + 62f), 3f) }; label("R", Offset(treeX - 75f, treeY + 52f), Cyan, 18f); label("U", Offset(treeX + 62f, treeY + 52f), Amber, 18f)
                    label("last move R or U", Offset(w * .64f, h * .18f), Violet, 21f); label("lattice paths = ${trim(frame.measurements.getValue("all paths"))}", Offset(w * .58f, h * .72f), Green, 23f)
                    label("C = left parent + below parent", Offset(w * .52f, h * .86f), Ink, 19f)
                }
                "modular-clock" -> {
                    val a = frame.parameters.getValue("a").toInt(); val modulus = frame.parameters.getValue("n").toInt().coerceIn(2, 16); val remainder = ((a % modulus) + modulus) % modulus
                    val center = Offset(w * .45f, h * .52f); val radius = minOf(w, h) * .29f
                    drawCircle(Cyan.copy(.08f), radius, center); drawCircle(Cyan, radius, center, style = Stroke(5f))
                    repeat(modulus) { index ->
                        val angle = -PI / 2 + 2 * PI * index / modulus; val point = center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius)
                        drawCircle(if (index == remainder) Green else Muted, if (index == remainder) 12f else 5f, point); label(index.toString(), point + Offset(-7f, -13f), if (index == remainder) Green else Ink, 18f)
                    }
                    val angle = -PI / 2 + 2 * PI * remainder / modulus; val end = center + Offset(cos(angle).toFloat() * radius * .78f, sin(angle).toFloat() * radius * .78f)
                    drawLine(Amber, center, end, 7f, cap = StrokeCap.Round); drawArc(Violet.copy(.55f), -90f, 300f, false, Offset(center.x - radius * .55f, center.y - radius * .55f), Size(radius * 1.1f, radius * 1.1f), style = Stroke(4f))
                    label("$a ≡ ${a - modulus} (mod $modulus)", Offset(w * .6f, h * .18f), Green, 25f); label("same remainder $remainder", Offset(w * .62f, h * .3f), Amber, 22f)
                }
            }
            if (profile.notToScale) label("NOT TO SCALE", Offset(w - 178f, 28f), Amber, 18f)
            label("Step ${frame.step + 1}/${frame.lab.steps.size} · drag horizontally", Offset(22f, h - 22f), Muted, 21f)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${frame.step + 1}. ${frame.lab.steps[frame.step]}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            GlowButton(if (proofLensOpen) "Hide proof lens" else "Open proof lens") { proofLensOpen = !proofLensOpen }
        }
        AnimatedVisibility(proofLensOpen) { ProofReasoningLens(frame, profile) }
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
        BeforeAfterProofDiagram(frame, profile)
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
            ProofMiniPanel("CONTRADICTION PATH", "Assume the conclusion is false → the two highlighted representations would measure differently → this conflicts with ${frame.invariant}.", Amber, Modifier.weight(1f))
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

@Composable
private fun QuizDashboard(
    subject: QuizSubject,
    level: QuizLevel,
    session: QuizSession?,
    questionBankSize: Int,
    onSubject: (QuizSubject) -> Unit,
    onLevel: (QuizLevel) -> Unit,
    onStart: () -> Unit,
    onAnswer: (Int) -> Unit,
    onRestart: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Interactive MCQ Quiz", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("15 questions per quiz · score, progress and explanations", color = Muted, fontSize = 12.sp)
            }
            TransparentIcon("Q", Violet)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QuizSubject.entries.forEach { option ->
                GlowButton(if (subject == option) "• ${option.label}" else option.label) { onSubject(option) }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            QuizLevel.entries.forEach { option ->
                GlowButton(if (level == option) "• ${option.label}" else option.label) { onLevel(option) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(if (session == null) "Start 15-question quiz" else "Restart quiz", onClick = if (session == null) onStart else onRestart)
            Insight("Question bank", "$questionBankSize MCQs", Green)
        }
        if (session == null) {
            QuizIntroCard(subject, level)
        } else {
            QuizSessionCard(session, onAnswer, onRestart)
        }
    }
}

@Composable
private fun QuizIntroCard(subject: QuizSubject, level: QuizLevel) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, Cyan.copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${subject.label} · ${level.label}", color = Cyan, fontWeight = FontWeight.Bold)
        Text("Choose Start to generate a focused 15-question quiz. Each answer locks instantly and shows a short explanation.", color = Muted, fontSize = 12.sp)
        Text("Scoring is local and reusable for Maths, Physics, Chemistry, Biology, Astro Physics and IQ Labs.", color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun QuizSessionCard(session: QuizSession, onAnswer: (Int) -> Unit, onRestart: () -> Unit) {
    val current = session.currentQuestion
    val lastAnswer = session.answers.lastOrNull()
    val lastQuestion = lastAnswer?.let { answer -> session.questions.firstOrNull { it.id == answer.questionId } }
    val progress = if (session.questions.isEmpty()) 0f else session.answers.size.toFloat() / session.questions.size
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x44101824))
            .border(1.dp, (if (session.completed) Green else Violet).copy(.58f), RoundedCornerShape(14.dp))
            .padding(12.dp)
            .semantics { contentDescription = "Interactive MCQ quiz score ${session.score} of ${session.questions.size}" },
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${session.subject.label} · ${session.level.label}", color = Cyan, fontWeight = FontWeight.Bold)
            Text("${session.score}/${session.questions.size}", color = Green, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))) {
            drawRect(Muted.copy(.18f), Offset.Zero, size)
            drawRect(Green.copy(.8f), Offset.Zero, Size(size.width * progress.coerceIn(0f, 1f), size.height))
        }
        Text("Question ${(session.currentIndex + 1).coerceAtMost(session.questions.size)} of ${session.questions.size}", color = Muted, fontSize = 11.sp)
        if (session.completed) {
            Text("Quiz complete", color = Green, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Score ${session.score}/${session.questions.size} · ${session.percent}%", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            val message = when {
                session.percent >= 85 -> "Excellent. Ready for the next level."
                session.percent >= 60 -> "Good progress. Review the missed explanations."
                else -> "Practice mode recommended. Restart and focus on explanations."
            }
            Text(message, color = Muted, fontSize = 12.sp)
            GlowButton("Try another quiz", onClick = onRestart)
        } else if (current != null) {
            Text(current.category, color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(current.prompt, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            current.choices.forEachIndexed { index, choice ->
                GlowButton("${('A'.code + index).toChar()}. $choice") { onAnswer(index) }
            }
        }
        if (lastAnswer != null && lastQuestion != null) {
            val correctText = if (lastAnswer.correct) "Correct" else "Review: ${lastQuestion.choices[lastQuestion.answerIndex]}"
            Text(correctText, color = if (lastAnswer.correct) Green else Amber, fontWeight = FontWeight.Bold)
            Text(lastQuestion.explanation, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun McqCard(question: McqQuestion, selected: Int?, onSelect: (Int) -> Unit) {
    val checked = selected?.let(question::check)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33101824))
            .border(1.dp, (if (checked?.correct == true) Green else Cyan).copy(.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(question.prompt, color = Ink, fontWeight = FontWeight.Bold)
        question.choices.forEachIndexed { index, choice ->
            GlowButton(if (selected == index) "• $choice" else choice) { onSelect(index) }
        }
        checked?.let {
            Text("${it.message} · next difficulty ${it.nextDifficulty}", color = if (it.correct) Green else Amber, fontWeight = FontWeight.SemiBold)
            Text(it.explanation, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProblemSolverScreen(vm: ExplorerViewModel, wide: Boolean) {
    val tutor = remember { MathSolverTutor() }
    var question by remember { mutableStateOf(TextFieldValue("Solve 2x + 3 = 11")) }
    var guided by remember { mutableStateOf<GuidedSolution?>(null) }
    var selectedMethod by remember { mutableStateOf(SolverMethod.Auto) }
    var reveal by remember { mutableStateOf(SolverReveal.FirstHint) }
    var revealedSteps by remember { mutableIntStateOf(1) }
    var whyStep by remember { mutableIntStateOf(-1) }
    var learnerWork by remember { mutableStateOf("") }
    var selectedResultForm by remember { mutableStateOf(SolverResultKind.Exact) }
    var showMathKeyboard by remember { mutableStateOf(true) }
    var showHandwriting by rememberSaveable { mutableStateOf(false) }
    val syntax = remember(question.text) { MathInputIntelligence.analyze(question.text) }
    val examples = listOf(
        "Linear" to "Solve 2x + 3 = 11",
        "Quadratic" to "x^2 - 5x + 6 = 0",
        "Inequality" to "Solve x^2 - 5x + 6 <= 0",
        "Series" to "Arithmetic series a=3 d=2 n=10 sum",
        "Product rule" to "Differentiate x*sin(x)",
        "Chain rule" to "Differentiate sin(x^2)",
        "Partial" to "Partial derivative of x^2*y + sin(y) with respect to x",
        "Integral" to "Integrate sin(2x) with respect to x",
        "Definite" to "Integrate sin(x) from 0 to pi",
        "Statistics" to "Mean of 4, 7, 7, 10",
        "Exact fraction" to "Calculate 1/3 + 1/6",
        "Expand" to "Expand (x + 2)(x - 3)",
        "Factor" to "Factor x^2 - 5x + 6",
        "Matrix" to "Inverse [[1,2],[3,4]]",
        "Units" to "Convert 5 km to m",
        "Limit" to "Limit (x^2 - 9)/(x - 3) as x -> 3",
        "Maclaurin" to "Maclaurin series of cos(x) through order 8",
        "Combinations" to "Combination 10 C 3",
        "Finance" to "Compound interest principal 10000 rate 8% time 3",
        "ODE" to "Solve differential equation dy/dx = 3y",
        "Initial value ODE" to "Solve differential equation dy/dx = 2y + 4, y(0)=3",
        "Word model" to "A rectangle has length 8 and width 5. Find its area",
    )

    fun solveNow() {
        guided = tutor.solve(question.text, selectedMethod)
        reveal = SolverReveal.FirstHint
        revealedSteps = 1
        whyStep = -1
    }

    fun insertMath(snippet: String) {
        val start = question.selection.min.coerceIn(0, question.text.length)
        val end = question.selection.max.coerceIn(start, question.text.length)
        val selected = question.text.substring(start, end)
        val (insertion, cursorOffset) = when {
            snippet.endsWith("(") && selected.isNotEmpty() -> "$snippet$selected)" to (snippet.length + selected.length + 1)
            snippet.endsWith("(") -> "$snippet)" to snippet.length
            snippet == "^2" && selected.isNotEmpty() -> "($selected)^2" to (selected.length + 4)
            snippet == "Differentiate " && selected.isNotEmpty() -> "Differentiate $selected".let { it to it.length }
            snippet == "Integrate " && selected.isNotEmpty() -> "Integrate $selected with respect to x".let { it to it.length }
            else -> snippet to snippet.length
        }
        val updated = question.text.replaceRange(start, end, insertion)
        question = TextFieldValue(updated, TextRange((start + cursorOffset).coerceAtMost(updated.length)))
    }

    @Composable
    fun QuestionPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Intelligent Maths Kernel", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                TransparentIcon("AI", Violet)
            }
            Text(
                "Ask in words or notation. The solver classifies, derives, calculates, and verifies each answer.",
                color = Muted,
                fontSize = 13.sp,
            )
            SolverCapabilityGrid()
            Text("Choose a method", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SolverMethod.entries.forEach { method ->
                    GlowButton(if (selectedMethod == method) "• ${method.label}" else method.label) {
                        selectedMethod = method
                        if (guided != null) solveNow()
                    }
                }
            }
            IntentAwareMathValueField(
                value = question, onValueChange = { question = it }, label = "Maths question",
                modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp).semantics { contentDescription = "Maths question input" },
                placeholder = "Example: solve x^2 - 5x + 6 = 0", minLines = 3,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(syntax.message, color = if (syntax.validBrackets) Green else Amber, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GlowButton(if (showMathKeyboard) "Hide editor" else "Smart editor") { showMathKeyboard = !showMathKeyboard }
                    GlowButton(if (showHandwriting) "Hide ink" else "Handwrite") { showHandwriting = !showHandwriting }
                }
            }
            AnimatedVisibility(showHandwriting) { HandwritingMathInput(onInsert = ::insertMath) }
            AnimatedVisibility(showMathKeyboard) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf("sin(" to "sin(", "cos(" to "cos(", "tan(" to "tan(", "√" to "sqrt(", "ln(" to "ln(", "eˣ" to "exp(", "x" to "x", "y" to "y", "x²" to "^2", "+" to "+", "−" to "-", "÷" to "/", "π" to "pi", "∫" to "Integrate ", "d/dx" to "Differentiate ", "bounds" to " from 0 to pi").forEach { (label, snippet) ->
                            GlowButton(label) { insertMath(snippet) }
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        SyntaxLegend("123", Cyan)
                        SyntaxLegend("sin cos", Violet)
                        SyntaxLegend("x y", Green)
                        SyntaxLegend("pi e", Amber)
                        SyntaxLegend("+ - =", Ink)
                    }
                    syntax.suggestions.firstOrNull()?.let { Text("AI hint · $it", color = Violet, fontSize = 11.sp) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("Start with a hint", enabled = question.text.isNotBlank(), onClick = ::solveNow)
                GlowButton("Workspaces") { vm.open(vm.state.module) }
            }
            Text("Try an example", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                examples.forEach { (label, example) ->
                    GlowButton(label) {
                        question = TextFieldValue(example, TextRange(example.length))
                        guided = tutor.solve(example, selectedMethod)
                        reveal = SolverReveal.FirstHint
                        revealedSteps = 1
                    }
                }
            }
            Insight("Runs", "On-device maths kernel", Green)
            Insight("Answers", "Derived + verified", Violet)
        }
    }

    @Composable
    fun ResultPanel(result: GuidedSolution?, modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Step by step maths solution" }) {
            if (result == null) {
                Text("Your solution will appear here", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Choose an example or enter a question, then tap Solve step by step.", color = Muted)
                Insight("Current coverage", "Exact CAS · matrices · units · calculus · inequalities · series · data", Cyan)
                Insight("Safety", "No invented unsupported answers", Amber)
            } else {
                val solution = result.solution
                SolverAnswerSummary(solution, if (reveal == SolverReveal.Answer) solution.answer else "Answer hidden · use the hints")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(.08f))
                        .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(14.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("How I interpreted it", color = Cyan, fontWeight = FontWeight.Bold)
                    Insight("Intent", result.interpretation.selected.intent.label, Violet)
                    Text(result.interpretation.selected.normalizedQuery, color = Ink, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Confidence ${trim(result.interpretation.selected.confidence * 100)}% · ${result.interpretation.status.name}", color = Muted, fontSize = 11.sp)
                    result.interpretation.assumptions.forEach { Text("Assumption · $it", color = Amber, fontSize = 11.sp) }
                    result.interpretation.ambiguities.forEach { Text("Ambiguity · $it", color = Amber, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
                    result.interpretation.alternatives.forEach { Text("Possible reading · ${it.normalizedQuery}", color = Violet, fontSize = 11.sp) }
                }
                Insight("Method", result.method.label, Violet)
                Text(result.methodReason, color = Muted, fontSize = 12.sp)
                if (result.alternatives.isNotEmpty()) {
                    Text("Try another verified method", color = Ink, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.alternatives.forEach { alternate ->
                            GlowButton("${alternate.method.label} · ${alternate.stepCount} steps") {
                                selectedMethod = alternate.method
                                guided = tutor.solve(question.text, alternate.method)
                                reveal = SolverReveal.FirstHint
                                revealedSteps = 1
                            }
                        }
                    }
                }
                result.wordModel?.let { model ->
                    Text("Word-problem model", color = Cyan, fontWeight = FontWeight.Bold)
                    model.quantities.forEach { quantity ->
                        Insight(quantity.name, quantity.value?.let { "$it ${quantity.unit.orEmpty()}".trim() } ?: "unknown (${quantity.symbol})", if (quantity.unknown) Amber else Green)
                    }
                    model.relationships.forEach { Text("Relationship · $it", color = Ink, fontSize = 12.sp) }
                    model.equations.forEach { Text("Setup · $it", color = Violet, fontWeight = FontWeight.SemiBold) }
                    model.ambiguity.forEach { Text("Clarify · $it", color = Amber, fontSize = 12.sp) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Next step", enabled = solution.steps.isNotEmpty() && revealedSteps < solution.steps.size) {
                        reveal = SolverReveal.Steps
                        revealedSteps++
                    }
                    GlowButton("Reveal method") { reveal = SolverReveal.Method }
                    GlowButton("Reveal answer") { reveal = SolverReveal.Answer }
                }
                result.visibleSteps(reveal, revealedSteps).forEachIndexed { index, item ->
                    val accent = when (item.role) {
                        SolutionStepRole.Interpret -> Cyan
                        SolutionStepRole.Transform -> Violet
                        SolutionStepRole.Calculate -> Green
                        SolutionStepRole.Verify -> Amber
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33101824))
                            .border(1.dp, accent.copy(.55f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("${index + 1}. ${item.title}", color = accent, fontWeight = FontWeight.Bold)
                        Text(item.expression, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.explanation, color = Muted, fontSize = 12.sp)
                        GlowButton(if (whyStep == index) "Hide why" else "Why?") { whyStep = if (whyStep == index) -1 else index }
                        if (whyStep == index) Text(result.why(index), color = Cyan, fontSize = 12.sp)
                    }
                }
                if (reveal == SolverReveal.Answer) {
                    Text("Answer forms", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.resultForms.forEach { form ->
                            GlowButton(if (selectedResultForm == form.kind) "• ${form.kind.label}" else form.kind.label, enabled = form.available) { selectedResultForm = form.kind }
                        }
                    }
                    result.resultForms.firstOrNull { it.kind == selectedResultForm }?.let { form ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Violet.copy(.09f)).padding(10.dp)) {
                            Text(form.value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(form.provenance, color = Muted, fontSize = 11.sp)
                        }
                    }
                    Text("Verification", color = Amber, fontWeight = FontWeight.Bold)
                    Text(solution.verification, color = Ink, fontSize = 13.sp)
                    solution.warnings.forEach { Text("• $it", color = Amber, fontSize = 12.sp) }
                    Text("Send result without retyping", color = Cyan, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Graph", enabled = result.handoffs.any { it.destination == SolverDestination.Graph && it.enabled }) { vm.sendSolverToGraph(result) }
                        GlowButton("Spreadsheet") { vm.sendSolverToTable(result) }
                        GlowButton("Notebook") { vm.saveSolverToNotebook(result) }
                        GlowButton("Generate MCQ") { learnerWork = "Practice generated · ${solution.question} · verify against the solved example" }
                    }
                }
                Text("Check my working", color = Ink, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = learnerWork,
                    onValueChange = { learnerWork = it },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Learner working for mistake diagnosis" },
                    label = { Text("Paste or type your steps") },
                    minLines = 2,
                )
                tutor.diagnose(solution.question, learnerWork).forEach { diagnosis ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Amber.copy(.1f)).padding(9.dp)) {
                        Text(diagnosis.kind.label, color = Amber, fontWeight = FontWeight.Bold)
                        Text(diagnosis.message, color = Ink, fontSize = 12.sp)
                        Text(diagnosis.correction, color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuestionPanel(Modifier.weight(.42f).fillMaxHeight())
            ResultPanel(guided, Modifier.weight(.58f).fillMaxHeight().verticalScroll(rememberScrollState()))
        }
    } else {
        Column(
            Modifier.fillMaxSize().padding(top = 70.dp, bottom = 70.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestionPanel(Modifier.fillMaxWidth())
            ResultPanel(guided, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SolverCapabilityGrid() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            "CAS" to Cyan,
            "Calculus" to Violet,
            "Inequalities" to Green,
            "Series" to Amber,
            "Matrices" to Cyan,
            "Stats" to Violet,
            "Natural input" to Green,
            "ODE & limits" to Amber,
        ).forEach { (label, color) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(.12f))
                    .border(1.dp, color.copy(.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                TransparentIcon(label.take(1), color)
                Text(label, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SolverAnswerSummary(result: ProblemSolution, displayedAnswer: String = result.answer) {
    val accent = if (result.supported) Green else Amber
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x44101824))
            .border(1.dp, accent.copy(.65f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(result.kind.label, color = Cyan, fontWeight = FontWeight.Bold)
                Text(if (result.supported) "Derived answer" else "Needs clearer input", color = Muted, fontSize = 11.sp)
            }
            Text(if (result.supported) "${(result.confidence * 100).toInt()}%" else "?", color = accent, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(displayedAnswer, color = if (result.supported) Ink else Amber, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(8.dp))
                .semantics { contentDescription = "Solver confidence ${(result.confidence * 100).toInt()} percent" },
        ) {
            drawRect(Muted.copy(.18f), Offset.Zero, size)
            drawRect(accent.copy(.82f), Offset.Zero, Size(size.width * result.confidence.toFloat().coerceIn(0f, 1f), size.height))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SolverMetric("Steps", result.steps.size.toString(), Violet)
            SolverMetric("Verify", if (result.verification.isBlank()) "none" else "yes", Amber)
            SolverMetric("Mode", if (result.supported) "offline" else "clarify", Cyan)
        }
    }
}

@Composable
private fun SolverMetric(label: String, value: String, accent: Color) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(.1f))
            .border(1.dp, accent.copy(.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(label, color = Muted, fontSize = 9.sp)
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProbabilityLabScreen(vm: ExplorerViewModel, wide: Boolean) {
    var section by remember(vm.requestedProbabilitySection) { mutableStateOf(ProbabilityLabSection.entries.getOrElse(vm.requestedProbabilitySection) { ProbabilityLabSection.Distributions }) }
    var kind by remember { mutableStateOf(DistributionKind.Normal) }
    var first by remember { mutableFloatStateOf(0f) }
    var second by remember { mutableFloatStateOf(1f) }
    var lower by remember { mutableFloatStateOf(-1f) }
    var upper by remember { mutableFloatStateOf(1f) }

    if (section == ProbabilityLabSection.Statistics) {
        StatisticsLabScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Spreadsheet) {
        SpreadsheetLabScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Experiments) {
        ProbabilityExperimentsScreen(vm, wide, onSection = { section = it })
        return
    }
    if (section == ProbabilityLabSection.Learn) {
        StatisticsMaterialsScreen(vm, wide, onSection = { section = it })
        return
    }

    fun select(next: DistributionKind) {
        kind = next
        when (next) {
            DistributionKind.Normal -> { first = 0f; second = 1f; lower = -1f; upper = 1f }
            DistributionKind.Binomial -> { first = 10f; second = .5f; lower = 3f; upper = 7f }
            DistributionKind.Poisson -> { first = 4f; second = 1f; lower = 1f; upper = 6f }
            DistributionKind.Uniform -> { first = 0f; second = 10f; lower = 2f; upper = 7f }
            DistributionKind.Exponential -> { first = 1f; second = 1f; lower = 0f; upper = 2f }
        }
    }

    val distribution = remember(kind, first, second) {
        runCatching { DistributionEngine.create(kind, first.toDouble(), second.toDouble()) }.getOrNull()
    }
    val probability = distribution?.let { runCatching { it.probabilityBetween(minOf(lower, upper).toDouble(), maxOf(lower, upper).toDouble()) }.getOrNull() }

    @Composable
    fun Controls(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Probability & Distributions", vm::returnToMathMenu, Cyan, icon = "σ")
            ProbabilitySectionSelector(section) { section = it }
            Text("Choose a validated model, adjust its parameters, and inspect interval probability, density, CDF, and quantiles.", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DistributionKind.entries.forEach { option -> GlowButton(if (kind == option) "• ${option.name}" else option.name) { select(option) } }
            }
            when (kind) {
                DistributionKind.Normal -> {
                    AxisSlider("Mean μ", first, -10f..10f) { first = it }
                    AxisSlider("Std dev σ", second, .1f..8f) { second = it }
                }
                DistributionKind.Binomial -> {
                    AxisSlider("Trials n", first, 1f..100f) { first = it.roundToInt().toFloat() }
                    AxisSlider("Success p", second, 0f..1f) { second = it }
                }
                DistributionKind.Poisson -> AxisSlider("Rate λ", first, .1f..30f) { first = it }
                DistributionKind.Uniform -> {
                    AxisSlider("Minimum a", first, -10f..9f) { first = minOf(it, second - .1f) }
                    AxisSlider("Maximum b", second, -9f..10f) { second = maxOf(it, first + .1f) }
                }
                DistributionKind.Exponential -> AxisSlider("Rate λ", first, .1f..10f) { first = it }
            }
            AxisSlider("Interval low", lower, -10f..30f) { lower = minOf(it, upper) }
            AxisSlider("Interval high", upper, -10f..30f) { upper = maxOf(it, lower) }
            Text("P(${trim(lower.toDouble())} ≤ X ≤ ${trim(upper.toDouble())}) = ${probability?.let(::trim) ?: "invalid"}", color = Green, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    fun Visualization(model: ProbabilityDistribution?, modifier: Modifier = Modifier) {
        GlassPanel(modifier.semantics { contentDescription = "Interactive probability distribution plot" }) {
            Text("${kind.name} distribution", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (model == null) {
                Text("Adjust the parameters to create a valid distribution.", color = Amber)
            } else {
                val points = remember(model) { model.plotPoints() }
                DistributionPlot(points, lower.toDouble(), upper.toDouble(), model.summary.domain == com.indianservers.aiexplorer.core.DistributionDomain.Discrete)
                Insight("Domain", model.summary.domain.name.lowercase(), Cyan)
                Insight("Mean", trim(model.summary.mean), Green)
                Insight("Variance", trim(model.summary.variance), Violet)
                Insight("Standard deviation", trim(model.summary.standardDeviation), Amber)
                Insight("CDF at upper", trim(model.cumulative(upper.toDouble())), Cyan)
                Insight("Median (Q50)", trim(model.quantile(.5)), Green)
                Insight("90th percentile", trim(model.quantile(.9)), Violet)
                Text("Parameters: ${model.summary.parameters.entries.joinToString { "${it.key}=${trim(it.value)}" }}", color = Muted, fontSize = 12.sp)
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Controls(Modifier.weight(.44f).fillMaxHeight())
            Visualization(distribution, Modifier.weight(.56f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            Controls(Modifier.fillMaxWidth())
            Visualization(distribution, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProbabilitySectionSelector(active: ProbabilityLabSection, onSelect: (ProbabilityLabSection) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ProbabilityLabSection.entries.forEach { section ->
            GlowButton(if (section == active) "• ${section.name}" else section.name) { onSelect(section) }
        }
    }
}

@Composable
private fun SpreadsheetLabScreen(vm: ExplorerViewModel, wide: Boolean, onSection: (ProbabilityLabSection) -> Unit) {
    val engine = remember { MathSpreadsheetEngine() }
    var csv by remember { mutableStateOf("Time,Value\n0,2\n1,4\n2,7\n3,11\n4,16") }
    var formula by remember { mutableStateOf("=MEAN(B1:B5)") }
    var missing by remember { mutableStateOf(MissingDataPolicy.Skip) }
    var exported by remember { mutableStateOf("") }
    val document = remember(csv, formula) { runCatching { engine.setCell(engine.importCsv(csv), com.indianservers.aiexplorer.core.SpreadsheetAddress(2, 0), formula) }.getOrNull() }
    val snapshot = remember(document, missing) { document?.let { engine.evaluate(it, missing) } }
    val linked = remember(snapshot) { snapshot?.let { runCatching { engine.linkedSeries(it, 0, 1) }.getOrNull() } }
    val regression = remember(linked) { linked?.points?.takeIf { it.size >= 3 }?.let { Phase4Statistics.linearRegression(it.map { p -> p.x }, it.map { p -> p.y }) } }

    @Composable fun Editor(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Spreadsheet & Lists", vm::returnToMathMenu, Cyan, icon = "▦")
            ProbabilitySectionSelector(ProbabilityLabSection.Spreadsheet, onSection)
            Text("Editable CSV grid · A1 formulas · named lists · linked analysis", color = Muted, fontSize = 12.sp)
            OutlinedTextField(value = csv, onValueChange = { csv = it }, label = { Text("CSV data") }, minLines = 7, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Spreadsheet CSV editor" })
            IntentAwareMathField(value = formula, onValueChange = { formula = it }, label = "Formula in C1", modifier = Modifier.fillMaxWidth(), placeholder = "=A1+B1", showLegend = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MissingDataPolicy.entries.forEach { policy -> GlowButton(if (missing == policy) "• ${policy.name}" else policy.name) { missing = policy } }
                GlowButton("Export CSV") { snapshot?.let { exported = engine.exportCsv(it) } }
            }
            if (exported.isNotBlank()) Text(exported, color = Green, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            snapshot?.evaluated?.entries?.sortedWith(compareBy({ it.key.row }, { it.key.column }))?.take(18)?.forEach { (address, cell) ->
                Insight(address.a1, cell.value?.let(::trim) ?: cell.error ?: "missing", if (cell.error == null) Cyan else Amber)
            }
        }
    }

    @Composable fun Analysis(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Linked plot & statistics", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Every edit rebuilds this series and its model.", color = Muted, fontSize = 12.sp)
            linked?.let { series ->
                Canvas(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(14.dp)).background(Color(0x6600060D)).semantics { contentDescription = "Spreadsheet linked scatter plot" }) {
                    if (series.points.isNotEmpty()) {
                        val minX = series.points.minOf { it.x }; val maxX = series.points.maxOf { it.x }.takeIf { it > minX } ?: minX + 1
                        val minY = series.points.minOf { it.y }; val maxY = series.points.maxOf { it.y }.takeIf { it > minY } ?: minY + 1
                        fun point(value: Vec2) = Offset(((value.x - minX) / (maxX - minX) * size.width).toFloat(), (size.height - (value.y - minY) / (maxY - minY) * size.height).toFloat())
                        series.points.zipWithNext().forEach { (a, b) -> drawLine(Cyan.copy(.7f), point(a), point(b), 3f) }
                        series.points.forEach { drawCircle(Violet, 8f, point(it)) }
                    }
                }
                Insight("Lists", "${series.xName} ↔ ${series.yName}", Cyan)
                Insight("Rows linked", series.points.size.toString(), Green)
                Insight("Revision", series.revision.toString(), Amber)
            }
            regression?.let { model ->
                Insight("Linear model", "y=${trim(model.coefficients[0])} + ${trim(model.coefficients[1])}x", Violet)
                Insight("R²", model.rSquared?.let(::trim) ?: "n/a", Green)
                model.diagnostics.forEach { Insight(it.name, it.detail, if (it.passed) Green else Amber) }
                Text("Assumptions: ${model.assumptions.joinToString()}", color = Muted, fontSize = 11.sp)
            }
        }
    }

    if (wide) Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Editor(Modifier.weight(.48f).fillMaxHeight().verticalScroll(rememberScrollState())); Analysis(Modifier.weight(.52f).fillMaxHeight())
    } else Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
        Editor(Modifier.fillMaxWidth()); Analysis(Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProbabilityExperimentsScreen(vm: ExplorerViewModel, wide: Boolean, onSection: (ProbabilityLabSection) -> Unit) {
    var kind by remember { mutableStateOf(RandomExperimentKind.Coin) }
    var trials by remember { mutableFloatStateOf(1_000f) }
    var seedText by remember { mutableStateOf("42") }
    var prior by remember { mutableFloatStateOf(.1f) }
    var sensitivity by remember { mutableFloatStateOf(.9f) }
    var falsePositive by remember { mutableFloatStateOf(.05f) }
    val seed = seedText.toLongOrNull() ?: 1L
    val result = remember(kind, trials, seed) { RandomExperimentEngine.simulate(kind, trials.toInt(), seed) }
    val pi = remember(trials, seed) { RandomExperimentEngine.monteCarloPi(trials.toInt(), seed) }
    val bayes = remember(prior, sensitivity, falsePositive) {
        ConditionalProbabilityEngine.bayes(
            mapOf("Condition" to prior.toDouble(), "No condition" to 1 - prior.toDouble()),
            mapOf("Condition" to sensitivity.toDouble(), "No condition" to falsePositive.toDouble()),
        )
    }
    val combinatorics = remember { CombinatoricsLab.calculate(10, 3) }
    GlassPanel(Modifier.fillMaxSize().padding(top = 74.dp, bottom = 70.dp, start = if (wide) 42.dp else 4.dp, end = if (wide) 42.dp else 4.dp).verticalScroll(rememberScrollState())) {
        PanelHeader("Probability Experiments", vm::returnToMathMenu, Cyan, icon = "Dice")
        ProbabilitySectionSelector(ProbabilityLabSection.Experiments, onSection)
        Text("Seeded simulations are exactly reproducible.", color = Muted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RandomExperimentKind.entries.forEach { option -> GlowButton(if (kind == option) "• ${option.name}" else option.name) { kind = option } }
        }
        AxisSlider("Trials", trials, 100f..20_000f) { trials = it.roundToInt().toFloat() }
        OutlinedTextField(value = seedText, onValueChange = { seedText = it.filter(Char::isDigit).take(12) }, label = { Text("Random seed") }, modifier = Modifier.widthIn(max = 260.dp))
        result.outcomes.forEach { outcome ->
            Insight(outcome.label, "${outcome.count} · observed=${trim(outcome.probability)} · expected=${trim(result.expected[outcome.label] ?: 0.0)}", Cyan)
        }
        Insight("Monte Carlo π", "${trim(pi.first)} ± ${trim(pi.second)}", Violet)
        Text("Conditional probability & Bayes tree", color = Ink, fontWeight = FontWeight.Bold)
        AxisSlider("Prior", prior, .01f..99f) { prior = it.coerceIn(.01f, .99f) }
        AxisSlider("Sensitivity", sensitivity, .01f..99f) { sensitivity = it.coerceIn(.01f, .99f) }
        AxisSlider("False positive", falsePositive, .01f..99f) { falsePositive = it.coerceIn(.01f, .99f) }
        Insight("P(positive)", trim(bayes.evidenceProbability), Amber)
        bayes.branches.forEach { branch ->
            Insight(branch.hypothesis, "prior=${trim(branch.prior)} · likelihood=${trim(branch.evidenceLikelihood)} · posterior=${trim(branch.posterior)}", Green)
        }
        Text("Combinatorics lab · n=10, r=3", color = Ink, fontWeight = FontWeight.Bold)
        Insight("Permutations", combinatorics.permutations.toString(), Cyan)
        Insight("Combinations", combinatorics.combinations.toString(), Violet)
        Insight("With replacement", combinatorics.withReplacement.toString(), Amber)
    }
}

@Composable
private fun StatisticsLabScreen(
    vm: ExplorerViewModel,
    wide: Boolean,
    onSection: (ProbabilityLabSection) -> Unit,
) {
    var dataText by remember { mutableStateOf("4, 5, 5, 6, 7, 8, 8, 8, 9, 10, 12, 18") }
    var chartType by remember { mutableStateOf(StatisticsChartType.Histogram) }
    var binCount by remember { mutableFloatStateOf(6f) }
    var hypothesizedMean by remember { mutableFloatStateOf(8f) }
    var selectedDetail by remember { mutableStateOf("Tap a mark to inspect its value") }
    val values = remember(dataText) { Regex("-?\\d+(?:\\.\\d+)?").findAll(dataText).mapNotNull { it.value.toDoubleOrNull() }.toList() }
    val summary = remember(values) { runCatching { AdvancedStatisticsEngine.summarize(values) }.getOrNull() }
    val histogram = remember(values, binCount) { if (values.isEmpty()) emptyList() else AdvancedStatisticsEngine.histogram(values, binCount.toInt()) }
    val confidence = remember(values) { runCatching { InferentialStatistics.meanConfidenceInterval(values) }.getOrNull() }
    val tTest = remember(values, hypothesizedMean) { runCatching { InferentialStatistics.oneSampleT(values, hypothesizedMean.toDouble()) }.getOrNull() }
    val regression = remember(values) { if (values.size >= 3) Phase4Statistics.linearRegression(values.indices.map(Int::toDouble), values) else null }
    val bootstrap = remember(values) { if (values.size >= 2) Phase4Statistics.bootstrapMean(values, repetitions = 1_000, seed = 42) else null }
    val anova = remember(values) {
        if (values.size >= 6) {
            val groups = values.withIndex().groupBy { it.index % 3 }.values.map { group -> group.map { it.value } }
            runCatching { Phase4Statistics.oneWayAnova(groups, permutations = 500, seed = 42) }.getOrNull()
        } else null
    }

    @Composable
    fun DataControls(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Interactive Statistics Lab", vm::returnToMathMenu, Cyan, icon = "x̄")
            ProbabilitySectionSelector(ProbabilityLabSection.Statistics, onSection)
            Text("Enter raw observations separated by commas or spaces. Every chart and statistic updates from the same data.", color = Muted, fontSize = 12.sp)
            OutlinedTextField(
                value = dataText,
                onValueChange = { dataText = it },
                label = { Text("Dataset") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Statistics dataset editor" },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Symmetric") { dataText = "2,3,4,5,6,7,8" }
                GlowButton("Skewed") { dataText = "1,1,2,2,3,5,8,13,21" }
                GlowButton("Outliers") { dataText = "10,11,11,12,12,13,13,14,40" }
                GlowButton("Bimodal") { dataText = "2,2,3,3,4,8,9,9,10,10" }
            }
            Text("Chart", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                StatisticsChartType.entries.forEach { type -> GlowButton(if (chartType == type) "• ${type.label}" else type.label) { chartType = type } }
            }
            if (chartType == StatisticsChartType.Histogram) AxisSlider("Bins", binCount, 2f..16f) { binCount = it.roundToInt().toFloat() }
            AxisSlider("H₀ mean", hypothesizedMean, -10f..30f) { hypothesizedMean = it }
            tTest?.let { test ->
                Insight("One-sample t", "t=${trim(test.statistic)} · df=${trim(test.degreesOfFreedom)}", Violet)
                Insight("Two-sided p", trim(test.pValueTwoSided), if (test.rejectAtFivePercent) Amber else Green)
            }
        }
    }

    @Composable
    fun Analysis(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text(chartType.label, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (summary == null) {
                Text("Enter at least one finite number.", color = Amber)
            } else {
                InteractiveStatisticsChart(values, summary, histogram, chartType, selectedDetail = selectedDetail, onSelect = { selectedDetail = it })
                Text(selectedDetail, color = Cyan, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Selected chart value" })
                Insight("Count / Sum", "${summary.count} / ${trim(summary.sum)}", Cyan)
                Insight("Mean", trim(summary.mean), Green)
                Insight("Median", trim(summary.median), Violet)
                Insight("Mode", summary.modes.joinToString { trim(it) }.ifBlank { "No repeated mode" }, Amber)
                Insight("Min / Max", "${trim(summary.fiveNumber.minimum)} / ${trim(summary.fiveNumber.maximum)}", Cyan)
                Insight("Q1 / Q3", "${trim(summary.fiveNumber.firstQuartile)} / ${trim(summary.fiveNumber.thirdQuartile)}", Violet)
                Insight("Range / IQR", "${trim(summary.range)} / ${trim(summary.interquartileRange)}", Green)
                Insight("Population variance", trim(summary.populationVariance), Cyan)
                Insight("Sample variance", trim(summary.sampleVariance), Violet)
                Insight("Population σ", trim(summary.populationStandardDeviation), Cyan)
                Insight("Sample s", trim(summary.sampleStandardDeviation), Green)
                Insight("Standard error", trim(summary.standardError), Amber)
                Insight("Mean abs deviation", trim(summary.meanAbsoluteDeviation), Cyan)
                Insight("Median abs deviation", trim(summary.medianAbsoluteDeviation), Violet)
                Insight("Skewness", summary.skewness?.let(::trim) ?: "needs n ≥ 3", Green)
                Insight("Excess kurtosis", summary.excessKurtosis?.let(::trim) ?: "needs n ≥ 4", Amber)
                Insight("Outliers (1.5×IQR)", summary.outliers.joinToString { trim(it) }.ifBlank { "none" }, if (summary.outliers.isEmpty()) Green else Amber)
                confidence?.let { interval -> Insight("95% mean CI", "[${trim(interval.lower)}, ${trim(interval.upper)}]", Cyan) }
                regression?.let { model ->
                    Insight("Regression", "slope=${trim(model.coefficients[1])} · R²=${model.rSquared?.let(::trim)}", Violet)
                    Insight("Residual range", "${trim(model.residuals.min())} to ${trim(model.residuals.max())}", Amber)
                    model.diagnostics.forEach { diagnostic -> Insight(diagnostic.name, diagnostic.detail, if (diagnostic.passed) Green else Amber) }
                    Text("Regression assumptions: ${model.assumptions.joinToString()}", color = Muted, fontSize = 11.sp)
                }
                anova?.let { test ->
                    Insight("ANOVA", "F=${trim(test.statistic)} · p=${trim(test.pValue)} · η²=${test.effectSize?.let(::trim)}", Cyan)
                    test.diagnostics.forEach { diagnostic -> Insight(diagnostic.name, diagnostic.detail, if (diagnostic.passed) Green else Amber) }
                }
                bootstrap?.let { result -> Insight("Seeded bootstrap mean", "${trim(result.observed)} · 95% [${trim(result.lower)}, ${trim(result.upper)}]", Green) }
            }
        }
    }

    if (wide) {
        Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DataControls(Modifier.weight(.43f).fillMaxHeight())
            Analysis(Modifier.weight(.57f).fillMaxHeight())
        }
    } else {
        Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
            DataControls(Modifier.fillMaxWidth())
            Analysis(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun InteractiveStatisticsChart(
    values: List<Double>,
    summary: DescriptiveStatistics,
    histogram: List<HistogramBin>,
    type: StatisticsChartType,
    selectedDetail: String,
    onSelect: (String) -> Unit,
) {
    val sorted = remember(values) { values.sorted() }
    val ecdf = remember(values) { AdvancedStatisticsEngine.empiricalCdf(values) }
    val qq = remember(values) { AdvancedStatisticsEngine.normalQq(values) }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x6600060D))
            .pointerInput(values, type, histogram) {
                detectTapGestures { tap ->
                    if (values.isEmpty()) return@detectTapGestures
                    val minX = sorted.first(); val maxX = sorted.last().takeIf { it > minX } ?: minX + 1
                    val value = minX + (tap.x / size.width).coerceIn(0f, 1f) * (maxX - minX)
                    if (type == StatisticsChartType.Histogram) {
                        val bin = histogram.firstOrNull { value >= it.lower && value <= it.upper } ?: histogram.lastOrNull()
                        bin?.let { onSelect("Bin ${trim(it.lower)}–${trim(it.upper)}: ${it.count} observations (${trim(it.relativeFrequency * 100)}%)") }
                    } else {
                        val nearest = sorted.minBy { abs(it - value) }
                        val rank = sorted.indexOf(nearest) + 1
                        onSelect("Value ${trim(nearest)} · ordered rank $rank of ${sorted.size}")
                    }
                }
            }
            .semantics { contentDescription = "Interactive ${type.label} statistical chart. $selectedDetail" },
    ) {
        val left = 42f; val right = size.width - 12f; val top = 16f; val bottom = size.height - 30f
        val width = (right - left).coerceAtLeast(1f); val height = (bottom - top).coerceAtLeast(1f)
        fun axis(minimum: Double, maximum: Double): (Double) -> Float = { value -> left + ((value - minimum) / (maximum - minimum).coerceAtLeast(1e-12) * width).toFloat() }
        drawLine(Muted.copy(.5f), Offset(left, bottom), Offset(right, bottom), 1.5f)
        drawLine(Muted.copy(.5f), Offset(left, top), Offset(left, bottom), 1.5f)
        repeat(4) { index ->
            val y = top + height * index / 4f
            drawLine(Muted.copy(.16f), Offset(left, y), Offset(right, y), 1f)
        }
        when (type) {
            StatisticsChartType.Histogram -> {
                val maximum = histogram.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                histogram.forEachIndexed { index, bin ->
                    val x0 = left + width * index / histogram.size
                    val x1 = left + width * (index + 1) / histogram.size
                    val y = bottom - height * bin.count / maximum
                    drawRect(if (index % 2 == 0) Cyan.copy(.62f) else Violet.copy(.62f), Offset(x0 + 1, y), Size((x1 - x0 - 2).coerceAtLeast(1f), bottom - y))
                    drawRect(Cyan.copy(.8f), Offset(x0 + 1, y), Size((x1 - x0 - 2).coerceAtLeast(1f), bottom - y), style = Stroke(1f))
                }
                drawTrigText("frequency", 4f, top + 12f, Muted)
            }
            StatisticsChartType.BoxPlot -> {
                val x = axis(summary.fiveNumber.minimum, summary.fiveNumber.maximum)
                val y = top + height * .5f
                drawLine(Cyan, Offset(x(summary.fiveNumber.minimum), y), Offset(x(summary.fiveNumber.maximum), y), 3f)
                drawLine(Cyan, Offset(x(summary.fiveNumber.minimum), y - 24), Offset(x(summary.fiveNumber.minimum), y + 24), 3f)
                drawLine(Cyan, Offset(x(summary.fiveNumber.maximum), y - 24), Offset(x(summary.fiveNumber.maximum), y + 24), 3f)
                drawRect(Violet.copy(.35f), Offset(x(summary.fiveNumber.firstQuartile), y - 38), Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 76f))
                drawRect(Violet, Offset(x(summary.fiveNumber.firstQuartile), y - 38), Size(x(summary.fiveNumber.thirdQuartile) - x(summary.fiveNumber.firstQuartile), 76f), style = Stroke(3f))
                drawLine(Amber, Offset(x(summary.median), y - 38), Offset(x(summary.median), y + 38), 4f)
                drawCircle(Green, 7f, Offset(x(summary.mean), y))
                summary.outliers.forEach { drawCircle(Color(0xFFFF6B7A), 6f, Offset(x(it), y)) }
                drawTrigText("median", x(summary.median) - 22f, y - 48f, Amber)
                drawTrigText("mean", x(summary.mean) - 18f, y + 58f, Green)
            }
            StatisticsChartType.DotPlot -> {
                val x = axis(sorted.first(), sorted.last().takeIf { it > sorted.first() } ?: sorted.first() + 1)
                val counts = mutableMapOf<Double, Int>()
                sorted.forEach { value ->
                    val level = counts.getOrDefault(value, 0); counts[value] = level + 1
                    drawCircle(if (level % 2 == 0) Cyan else Violet, 7f, Offset(x(value), bottom - 10f - level * 17f))
                }
            }
            StatisticsChartType.Ecdf -> {
                val x = axis(sorted.first(), sorted.last().takeIf { it > sorted.first() } ?: sorted.first() + 1)
                var previous = Offset(x(sorted.first()), bottom)
                ecdf.forEach { point ->
                    val currentX = x(point.x); val currentY = bottom - point.y.toFloat() * height
                    drawLine(Cyan, previous, Offset(currentX, previous.y), 3f)
                    drawLine(Cyan, Offset(currentX, previous.y), Offset(currentX, currentY), 3f)
                    previous = Offset(currentX, currentY)
                }
                drawTrigText("1.0", 8f, top + 8f, Muted)
                drawTrigText("0.5", 8f, top + height / 2 + 4f, Muted)
            }
            StatisticsChartType.NormalQq -> {
                val minTheory = qq.minOf { it.x }; val maxTheory = qq.maxOf { it.x }
                val minObserved = qq.minOf { it.y }; val maxObserved = qq.maxOf { it.y }
                val x = axis(minTheory, maxTheory)
                fun y(value: Double) = bottom - ((value - minObserved) / (maxObserved - minObserved).coerceAtLeast(1e-12) * height).toFloat()
                val referenceSd = summary.sampleStandardDeviation.takeIf(Double::isFinite)
                    ?: summary.populationStandardDeviation
                val refStart = summary.mean + referenceSd * minTheory
                val refEnd = summary.mean + referenceSd * maxTheory
                drawLine(Amber.copy(.8f), Offset(x(minTheory), y(refStart)), Offset(x(maxTheory), y(refEnd)), 2f)
                qq.forEach { drawCircle(Cyan, 6f, Offset(x(it.x), y(it.y))) }
                drawTrigText("theoretical normal quantiles", left + 20f, size.height - 8f, Muted)
            }
        }
        drawTrigText(trim(sorted.first()), left, size.height - 8f, Muted)
        drawTrigText(trim(sorted.last()), right - 38f, size.height - 8f, Muted)
    }
}

@Composable
private fun StatisticsMaterialsScreen(
    vm: ExplorerViewModel,
    wide: Boolean,
    onSection: (ProbabilityLabSection) -> Unit,
) {
    var level by remember { mutableStateOf(StatisticsStudyLevel.School) }
    val lessons = StatisticsCurriculum.lessons.getValue(level)
    GlassPanel(
        Modifier
            .fillMaxSize()
            .padding(top = 74.dp, bottom = 70.dp, start = if (wide) 42.dp else 4.dp, end = if (wide) 42.dp else 4.dp),
    ) {
        PanelHeader("Statistics Learning Path · School to PG", vm::returnToMathMenu, Cyan, icon = "∑")
        ProbabilitySectionSelector(ProbabilityLabSection.Learn, onSection)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatisticsStudyLevel.entries.forEach { option -> GlowButton(if (level == option) "• ${option.label}" else option.label) { level = option } }
        }
        Text("${level.label} syllabus foundation", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        lessons.forEachIndexed { index, lesson ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Color(0x33101824)).border(1.dp, if (index % 2 == 0) Cyan.copy(.45f) else Violet.copy(.45f), RoundedCornerShape(15.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${index + 1}. ${lesson.title}", color = Ink, fontWeight = FontWeight.Bold)
                Text(lesson.concepts.joinToString(" · "), color = Cyan, fontSize = 12.sp)
                Text("Interactive lab: ${lesson.lab}", color = Green, fontSize = 12.sp)
                Text("Outcome: ${lesson.outcome}", color = Muted, fontSize = 11.sp)
            }
        }
        Text("Coverage is sequenced to PG level; advanced GLM, Bayesian, multivariate, time-series and survival modules currently provide curriculum and engine boundaries for later full model-fitting labs.", color = Amber, fontSize = 11.sp)
    }
}

@Composable
private fun DistributionPlot(points: List<com.indianservers.aiexplorer.core.DistributionPoint>, lower: Double, upper: Double, discrete: Boolean) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x6600060D))
            .semantics { contentDescription = "Distribution density with selected probability interval" },
    ) {
        if (points.size < 2) return@Canvas
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }.takeIf { it > minX } ?: (minX + 1.0)
        val maxY = points.maxOf { it.probability }.coerceAtLeast(1e-12)
        fun sx(x: Double) = ((x - minX) / (maxX - minX) * size.width).toFloat()
        fun sy(y: Double) = (size.height - y / maxY * size.height * .88).toFloat()
        drawLine(Muted.copy(.4f), Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.5f)
        if (discrete) {
            points.forEach { point ->
                val selected = point.x in minOf(lower, upper)..maxOf(lower, upper)
                drawLine(if (selected) Green else Cyan, Offset(sx(point.x), size.height), Offset(sx(point.x), sy(point.probability)), strokeWidth = (size.width / points.size * .62f).coerceIn(2f, 18f), cap = StrokeCap.Round)
            }
        } else {
            val path = Path()
            points.forEachIndexed { index, point -> if (index == 0) path.moveTo(sx(point.x), sy(point.probability)) else path.lineTo(sx(point.x), sy(point.probability)) }
            drawPath(path, Cyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
            points.filter { it.x in minOf(lower, upper)..maxOf(lower, upper) }.forEach { point ->
                drawLine(Green.copy(.34f), Offset(sx(point.x), size.height), Offset(sx(point.x), sy(point.probability)), strokeWidth = 3f)
            }
        }
    }
}

@Composable
private fun SubjectMicroIcon(subject: String, tint: Color) {
    Box(
        Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.radialGradient(listOf(tint.copy(.24f), tint.copy(.05f))))
            .border(1.dp, tint.copy(.5f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp).semantics { contentDescription = "$subject icon" }) {
            val w = size.width
            val h = size.height
            val stroke = 1.8.dp.toPx()
            when (subject) {
                "Maths" -> {
                    drawLine(tint.copy(.7f), Offset(w * .16f, h * .78f), Offset(w * .88f, h * .78f), stroke)
                    drawLine(tint.copy(.7f), Offset(w * .24f, h * .88f), Offset(w * .24f, h * .12f), stroke)
                    val curve = Path().apply {
                        moveTo(w * .16f, h * .68f)
                        cubicTo(w * .36f, h * .18f, w * .58f, h * .92f, w * .88f, h * .22f)
                    }
                    drawPath(curve, tint, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                "Physics" -> {
                    drawCircle(tint, w * .10f, Offset(w / 2, h / 2))
                    drawOval(tint.copy(.85f), Offset(w * .08f, h * .34f), Size(w * .84f, h * .32f), style = Stroke(stroke))
                    drawOval(tint.copy(.65f), Offset(w * .34f, h * .08f), Size(w * .32f, h * .84f), style = Stroke(stroke))
                    drawCircle(tint, w * .055f, Offset(w * .88f, h / 2))
                }
                "Chemistry" -> {
                    val flask = Path().apply {
                        moveTo(w * .38f, h * .12f); lineTo(w * .62f, h * .12f)
                        moveTo(w * .44f, h * .12f); lineTo(w * .44f, h * .42f)
                        lineTo(w * .20f, h * .82f); quadraticBezierTo(w * .18f, h * .91f, w * .30f, h * .91f)
                        lineTo(w * .70f, h * .91f); quadraticBezierTo(w * .82f, h * .91f, w * .80f, h * .82f)
                        lineTo(w * .56f, h * .42f); lineTo(w * .56f, h * .12f)
                    }
                    drawPath(flask, tint, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawLine(tint.copy(.7f), Offset(w * .27f, h * .71f), Offset(w * .73f, h * .71f), stroke)
                }
                "Biology" -> {
                    val left = Path(); val right = Path()
                    repeat(7) { index ->
                        val y = h * (.10f + index * .13f)
                        val x1 = if (index % 2 == 0) w * .28f else w * .52f
                        val x2 = w - x1
                        if (index == 0) { left.moveTo(x1, y); right.moveTo(x2, y) } else { left.lineTo(x1, y); right.lineTo(x2, y) }
                        drawLine(tint.copy(.55f), Offset(x1, y), Offset(x2, y), stroke * .72f)
                    }
                    drawPath(left, tint, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawPath(right, tint, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                "Astro Physics" -> {
                    drawCircle(tint.copy(.22f), w * .25f, Offset(w * .48f, h * .52f))
                    drawCircle(tint, w * .25f, Offset(w * .48f, h * .52f), style = Stroke(stroke))
                    drawLine(tint, Offset(w * .10f, h * .68f), Offset(w * .88f, h * .34f), stroke)
                    drawCircle(tint, w * .05f, Offset(w * .82f, h * .18f))
                }
                else -> {
                    val nodes = listOf(Offset(w * .22f, h * .28f), Offset(w * .72f, h * .20f), Offset(w * .48f, h * .52f), Offset(w * .24f, h * .78f), Offset(w * .78f, h * .76f))
                    listOf(0 to 2, 1 to 2, 2 to 3, 2 to 4, 3 to 4).forEach { (a, b) -> drawLine(tint.copy(.55f), nodes[a], nodes[b], stroke) }
                    nodes.forEach { drawCircle(tint, w * .065f, it) }
                }
            }
        }
    }
}

@Composable
private fun SubjectHubScreen(modifier: Modifier = Modifier, wide: Boolean, onOpenSubject: (String) -> Unit, onOpenLearningIntelligence: () -> Unit) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 42.dp else 12.dp, vertical = if (wide) 28.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("AI Explorer", color = Ink, fontSize = if (wide) 42.sp else 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Choose a learning laboratory", color = Muted, fontSize = if (wide) 20.sp else 15.sp)
        Text("Maths and Physics are available now · more sciences are being prepared", color = Cyan, fontSize = 12.sp, textAlign = TextAlign.Center)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SubjectOptions.forEach { subject ->
                Column(
                    Modifier
                        .width(if (wide) 250.dp else 158.dp)
                        .heightIn(min = if (wide) 178.dp else 154.dp)
                        .shadow(if (subject.enabled) 14.dp else 5.dp, RoundedCornerShape(26.dp), ambientColor = Cyan.copy(.18f), spotColor = Violet.copy(.24f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            if (subject.enabled) Brush.linearGradient(listOf(Color(0xEE111C35), Color(0xCC203058), Color(0xCC123B38)))
                            else Brush.linearGradient(listOf(Color(0xEE0D131E), Color(0xEE121824))),
                        )
                        .border(2.dp, if (subject.enabled) Cyan.copy(.78f) else Muted.copy(.28f), RoundedCornerShape(24.dp))
                        .clickable(enabled = subject.enabled) { onOpenSubject(subject.title) }
                        .focusable()
                        .semantics { contentDescription = if (subject.enabled) "Open ${subject.title} laboratory" else "${subject.title}, coming soon" }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SubjectMicroIcon(subject.title, if (subject.enabled) Cyan else Muted)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background((if (subject.enabled) Green else Amber).copy(.12f))
                                .border(1.dp, (if (subject.enabled) Green else Amber).copy(.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                        ) {
                            Text(if (subject.enabled) "LIVE" else "SOON", color = if (subject.enabled) Green else Amber.copy(.8f), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(subject.title, color = if (subject.enabled) Ink else Muted, fontSize = if (wide) 22.sp else 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(subject.description, color = Muted, fontSize = 10.sp, maxLines = 2)
                        if (subject.enabled) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Enter lab", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(">", color = Green, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
        Button(onClick = onOpenLearningIntelligence, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open local learning diagnostic and recommendations" }) {
            Text("Learning Intelligence · Diagnostic, mastery, review and error book")
        }
        Text("Touch, mouse, keyboard and TV remote ready", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun PhysicsHubScreen(vm: ExplorerViewModel, wide: Boolean) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PhysicsModuleOption?>(null) }
    var showMechanicalWaves by rememberSaveable { mutableStateOf(false) }
    var showFormulaLibrary by rememberSaveable { mutableStateOf(false) }
    var showConnectedJourney by rememberSaveable { mutableStateOf(false) }
    if (showConnectedJourney) {
        PhysicsConnectedLearningFeature(onExit = { showConnectedJourney = false })
        return
    }
    if (showFormulaLibrary) {
        PhysicsFormulaFeatureRoot(onExit = { showFormulaLibrary = false })
        return
    }
    if (showMechanicalWaves) {
        MechanicalWaveLabScreen(onBack = { showMechanicalWaves = false })
        return
    }
    val visible = PhysicsModules.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (wide) 34.dp else 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransparentIcon("F", Cyan)
                Column {
                    Text("Physics Explorer", color = Cyan, fontSize = if (wide) 28.sp else 22.sp, fontWeight = FontWeight.Bold)
                    Text("16 physics learning modules", color = Muted, fontSize = 11.sp)
                }
            }
            GlowButton("Home", icon = "H", onClick = vm::openSubjectHub)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Cyan.copy(.16f), Violet.copy(.12f)))).border(1.dp, Cyan.copy(.65f), RoundedCornerShape(18.dp)).clickable { showFormulaLibrary = true }.semantics { contentDescription = "Open Physics Formulas: equations, derivations, units, calculators and applications" }.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PHYSICS FORMULAS", color = Cyan, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Equations, derivations, units, calculators and applications", color = Ink, fontSize = 11.sp)
                Text("25 categories · offline · separate Physics repository", color = Green, fontSize = 9.sp)
            }
            Text("OPEN  →", color = Amber, fontWeight = FontWeight.Bold)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xCC0B1420)).border(1.dp, Violet.copy(.65f), RoundedCornerShape(18.dp)).clickable { showConnectedJourney = true }.semantics { contentDescription = "Open connected Physics journey from Motion through Work and Energy" }.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("CONNECTED LEARNING · MOTION TO ENERGY", color = Violet, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text("Learn → Explore → Test across prerequisites, formulas and activities", color = Ink, fontSize = 11.sp)
            }
            Text("START  →", color = Green, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search Physics") },
            placeholder = { Text("Try waves, energy, electricity or astronomy") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            visible.forEach { module ->
                Column(
                    Modifier
                        .width(if (wide) 225.dp else 165.dp)
                        .heightIn(min = 130.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected == module) Violet.copy(.18f) else Color(0xCC0B1420))
                        .border(1.dp, if (selected == module) Violet else Cyan.copy(.35f), RoundedCornerShape(18.dp))
                        .clickable {
                            if (module.title == "Waves and Sound") showMechanicalWaves = true
                            else selected = module
                        }
                        .focusable()
                        .semantics { contentDescription = "Open Physics module ${module.title}" }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        TransparentIcon(module.icon, if (selected == module) Violet else Cyan)
                        Text(if (selected == module) "SELECTED" else "OPEN", color = if (selected == module) Violet else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(module.title, color = Ink, fontSize = if (wide) 16.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    Text(module.description, color = Muted, fontSize = 10.sp, maxLines = 3)
                }
            }
        }
        selected?.let { module ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.horizontalGradient(listOf(Color(0xDD10233A), Color(0xDD21183A)))).border(1.dp, Violet.copy(.55f), RoundedCornerShape(18.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TransparentIcon(module.icon, Violet)
                    Text(module.title, color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(module.description, color = Ink, fontSize = 12.sp)
                Text("Module selected. Interactive lessons, formulas, simulations, visual proofs and practice activities can be added inside this destination.", color = Muted, fontSize = 11.sp)
            }
        }
        if (visible.isEmpty()) Text("No Physics modules match this search.", color = Amber, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ShapesExplorerScreen(vm: ExplorerViewModel, wide: Boolean) {
    var dimension by remember { mutableIntStateOf(2) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var favouritesOnly by remember { mutableStateOf(false) }
    var multiAdd by remember { mutableStateOf(false) }
    var compareMode by remember { mutableStateOf(false) }
    var compareKeys by remember { mutableStateOf<List<String>>(emptyList()) }
    var formulaKey by remember { mutableStateOf<String?>(null) }
    val categories = if (dimension == 2) listOf("All", "Triangles", "Quadrilaterals", "Polygons", "Curves")
    else listOf("All", "Polyhedra", "Round", "Prisms", "Advanced")
    fun solidCategory(type: SolidType) = when (type) {
        SolidType.Cube, SolidType.Cuboid, SolidType.Tetrahedron, SolidType.TriangularPyramid, SolidType.Octahedron, SolidType.Pyramid, SolidType.Wedge -> "Polyhedra"
        SolidType.Sphere, SolidType.Hemisphere, SolidType.Cylinder, SolidType.Cone, SolidType.Frustum, SolidType.Torus, SolidType.Capsule -> "Round"
        SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.OctagonalPrism -> "Prisms"
        else -> "Advanced"
    }
    fun selectKey(key: String, open: () -> Unit) {
        if (compareMode) compareKeys = (compareKeys + key).distinct().takeLast(2) else open()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(
            Modifier
                .widthIn(max = if (wide) 820.dp else 520.dp)
                .fillMaxWidth(.96f),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    TransparentIcon("SE", Cyan)
                    Column {
                        Text("Shapes Explorer", color = Cyan, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("Choose a shape to load its interactive scene", color = Muted, fontSize = 11.sp)
                    }
                }
                GlowButton("Maths", icon = "←", onClick = vm::returnToMathMenu)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton(if (dimension == 2) "2D · ${ShapeExplorer2DShapes.size}" else "2D", icon = "2D") { dimension = 2; category = "All"; compareKeys = emptyList() }
                GlowButton(if (dimension == 3) "3D · ${SolidType.entries.size}" else "3D", icon = "3D") { dimension = 3; category = "All"; compareKeys = emptyList() }
                GlowButton(if (multiAdd) "Add: ON" else "Add many", icon = "+") { multiAdd = !multiAdd; compareMode = false }
                GlowButton(if (compareMode) "Compare: ON" else "Compare", icon = "=") { compareMode = !compareMode; multiAdd = false; compareKeys = emptyList() }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search shapes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                categories.forEach { item -> GlowButton(if (category == item) "• $item" else item) { category = item } }
                GlowButton(if (favouritesOnly) "• Favourites" else "Favourites", icon = "★") { favouritesOnly = !favouritesOnly }
            }
            if (vm.recentShapeKeys.isNotEmpty()) {
                Text("RECENT", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    vm.recentShapeKeys.take(5).forEach { key ->
                        val label = key.substringAfter(':').replace(Regex("([a-z])([A-Z])"), "$1 $2")
                        GlowButton(label, icon = "R") {
                            if (key.startsWith("2d:")) vm.loadExplorerShape2D(key.substringAfter(':'))
                            else runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull()?.let(vm::loadExplorerShape3D)
                        }
                    }
                }
            }
            Text(
                if (dimension == 2) "2D SHAPE LIBRARY" else "3D SOLID LIBRARY",
                color = if (dimension == 2) Violet else Green,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (dimension == 2) {
                    ShapeExplorer2DShapes.filter { preset ->
                        preset.label.contains(query, true) && (category == "All" || preset.category() == category) && (!favouritesOnly || "2d:${preset.id}" in vm.favoriteShapeKeys)
                    }.forEach { preset ->
                        val key = "2d:${preset.id}"
                        ShapeCatalogButton(preset.label, preset.label.take(2).uppercase(), key in vm.favoriteShapeKeys, compareKeys.contains(key),
                            onOpen = { selectKey(key) { if (multiAdd) vm.addExplorerShape2D(preset.id) else vm.loadExplorerShape2D(preset.id) } },
                            onFavourite = { vm.toggleFavoriteShape(key) },
                            onFormulas = { formulaKey = key })
                    }
                } else {
                    SolidType.entries.filter { type ->
                        type.name.contains(query, true) && (category == "All" || solidCategory(type) == category) && (!favouritesOnly || "3d:${type.name}" in vm.favoriteShapeKeys)
                    }.forEach { type ->
                        val key = "3d:${type.name}"
                        ShapeCatalogButton(type.name.replace(Regex("([a-z])([A-Z])"), "$1 $2"), "3D", key in vm.favoriteShapeKeys, compareKeys.contains(key),
                            onOpen = { selectKey(key) { if (multiAdd) vm.addExplorerShape3D(type) else vm.loadExplorerShape3D(type) } },
                            onFavourite = { vm.toggleFavoriteShape(key) },
                            onFormulas = { formulaKey = key })
                    }
                }
            }
            if (compareKeys.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Violet.copy(.08f)).border(1.dp, Violet.copy(.35f), RoundedCornerShape(14.dp)).padding(9.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Shape comparison · choose ${2 - compareKeys.size} more", color = Violet, fontWeight = FontWeight.Bold)
                    compareKeys.forEach { key ->
                        val formulas = if (key.startsWith("2d:")) ShapeExplorer2DShapes.firstOrNull { it.id == key.substringAfter(':') }?.let { shape2DFormulaLibrary(it.label) }.orEmpty()
                        else runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull()?.let(::solidFormulaLibrary).orEmpty()
                        Text("${key.substringAfter(':')} · ${formulas.size} formulas · ${formulas.take(2).joinToString { it.expression }}", color = Ink, fontSize = 11.sp)
                    }
                }
            }
            formulaKey?.let { key ->
                val is2D = key.startsWith("2d:")
                val preset = if (is2D) ShapeExplorer2DShapes.firstOrNull { it.id == key.substringAfter(':') } else null
                val solidType = if (!is2D) runCatching { SolidType.valueOf(key.substringAfter(':')) }.getOrNull() else null
                val title = preset?.label ?: solidType?.name?.replace(Regex("([a-z])([A-Z])"), "$1 $2") ?: "Shape"
                val formulas = preset?.let { shape2DFormulaLibrary(it.label) } ?: solidType?.let(::solidFormulaLibrary).orEmpty()
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xEE08131E)).border(1.dp, Violet.copy(.55f), RoundedCornerShape(16.dp)).padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$title · All Formulas", color = Violet, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("${formulas.size} formulas available", color = Muted, fontSize = 10.sp)
                        }
                        GlowButton("Close", icon = "X") { formulaKey = null }
                    }
                    formulas.forEachIndexed { index, formula ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Violet.copy(.07f)).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TransparentIcon("${index + 1}", Violet)
                            Column(Modifier.weight(1f)) {
                                Text(formula.name, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(formula.expression, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    GlowButton("Open interactive scene", icon = "Open") {
                        preset?.let { vm.loadExplorerShape2D(it.id) } ?: solidType?.let(vm::loadExplorerShape3D)
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22101824))
                    .border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(14.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransparentIcon("Touch", Amber)
                Column {
                    Text("Interactive scene", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (dimension == 2) "Drag the glowing points to resize. Measurements and formulas update with the shape."
                        else "The solid opens in Scale mode. Drag the solid to resize; use two fingers to pan or zoom the view.",
                        color = Muted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShapeCatalogButton(label: String, icon: String, favourite: Boolean, selected: Boolean, onOpen: () -> Unit, onFavourite: () -> Unit, onFormulas: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) Violet.copy(.20f) else Color(0x99101824)).border(1.dp, if (selected) Violet else Cyan.copy(.28f), RoundedCornerShape(14.dp)).clickable(onClick = onOpen).padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TransparentIcon(icon, if (selected) Violet else Cyan)
        Text(label, color = Ink, fontSize = 12.sp, maxLines = 1)
        Text("ƒ", color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onFormulas).padding(5.dp).semantics { contentDescription = "Show all formulas for $label" })
        Text(if (favourite) "★" else "☆", color = if (favourite) Amber else Muted, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onFavourite).padding(5.dp).semantics { contentDescription = "${if (favourite) "Remove" else "Add"} $label favourite" })
    }
}

@Composable
private fun MathematicsMenuPanel(
    vm: ExplorerViewModel,
    compact: Boolean,
    onMove: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var launcherQuery by rememberSaveable { mutableStateOf("") }
    var showWorkspaces by remember { mutableStateOf(false) }
    var showConcepts by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var futureSelection by remember { mutableStateOf<String?>(null) }

    fun openWorkspaceOption(title: String) {
        vm.recordMathToolOpen(title)
        when (title) {
            "Unified Math Studio" -> vm.openUnifiedMathStudio()
            "Adaptive Math Coach" -> vm.openAdaptiveMathLearning()
            "Scientific Calculator" -> vm.openScientificCalculator()
            "Math Notebook" -> vm.openMathNotebook()
            "Problem Solver" -> vm.openProblemSolver()
            "Formulas" -> vm.openKnowledgeHub(KnowledgeSection.Formulas)
            "MCQs" -> vm.openKnowledgeHub(KnowledgeSection.Mcqs)
            "Formula Visualizer" -> vm.openKnowledgeHub(KnowledgeSection.Visualize)
            "Theorems" -> vm.openKnowledgeHub(KnowledgeSection.Theorems)
            "Visual Proofs" -> vm.openKnowledgeHub(KnowledgeSection.Proofs)
            "Visual Dictionary" -> vm.openKnowledgeHub(KnowledgeSection.Dictionary)
            "Probability & Statistics" -> vm.openProbabilityLab()
            "Manipulatives" -> vm.open(MathModule.Manipulatives)
            "Shapes Explorer" -> vm.openShapesExplorer()
            "Set Theory & Logic" -> vm.openSetLogicVisualizer()
            "Explore Workspaces" -> showWorkspaces = !showWorkspaces
            "Math Concepts" -> showConcepts = !showConcepts
            else -> futureSelection = title
        }
    }

    val allTools = remember { (MathCreationTools + MathLearningTools + SuggestedMathTools).distinctBy { it.title } }
    val matchingTools = remember(launcherQuery) {
        val query = launcherQuery.trim().lowercase()
        if (query.isBlank()) allTools else allTools.filter { query in it.title.lowercase() || query in it.description.lowercase() }
    }

    @Composable
    fun LauncherTool(option: MathWorkspaceOption) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            GlowButton(option.title, icon = option.icon) { openWorkspaceOption(option.title) }
            Text(
                if (option.title in vm.pinnedMathTools) "★" else "☆",
                color = if (option.title in vm.pinnedMathTools) Amber else Muted,
                fontSize = 18.sp,
                modifier = Modifier.clickable { vm.togglePinnedMathTool(option.title) }.padding(5.dp)
                    .semantics { contentDescription = "${if (option.title in vm.pinnedMathTools) "Unpin" else "Pin"} ${option.title}" },
            )
        }
    }

    GlassPanel(modifier) {
        PanelHeader("Maths Tool Launcher", vm::toggleMathMenu, Cyan, icon = "⌕", onMove = onMove)
        Text(vm.mathsBreadcrumb.joinToString(" → "), color = Green, fontSize = 11.sp, modifier = Modifier.semantics { contentDescription = "Maths breadcrumb" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Home", icon = "H", onClick = vm::openSubjectHub)
            GlowButton("Current Workspace", onClick = vm::toggleMathMenu)
        }
        OutlinedTextField(
            value = launcherQuery,
            onValueChange = { launcherQuery = it },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Search maths tools" },
            label = { Text("Search tools, topics or activities") },
            singleLine = true,
        )
        if (launcherQuery.isBlank()) {
            val pinned = allTools.filter { it.title in vm.pinnedMathTools }
            val recent = vm.recentMathTools.mapNotNull { title -> allTools.singleOrNull { it.title == title } }.take(4)
            val frequent = allTools.filter { (vm.mathToolUseCounts[it.title] ?: 0) > 0 }.sortedByDescending { vm.mathToolUseCounts[it.title] }.take(4)
            if (pinned.isNotEmpty()) {
                Text("PINNED", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { pinned.forEach { LauncherTool(it) } }
            }
            if (recent.isNotEmpty()) {
                Text("RECENT", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { recent.forEach { LauncherTool(it) } }
            }
            if (frequent.isNotEmpty()) {
                Text("FREQUENTLY USED", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { frequent.forEach { LauncherTool(it) } }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (launcherQuery.isNotBlank()) Text("${matchingTools.size} matching tools", color = Green, fontSize = 11.sp)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CREATE & SOLVE", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MathCreationTools.filter { it in matchingTools }.forEach { LauncherTool(it) }
                }

                AnimatedVisibility(showWorkspaces) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Interactive workspaces", color = Muted, fontSize = 11.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MathModule.entries.forEach { module -> GlowButton(module.label, onClick = { vm.open(module) }) }
                        }
                    }
                }

                Text("LEARN & PRACTISE", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MathLearningTools.filter { it in matchingTools }.forEach { LauncherTool(it) }
                }

                AnimatedVisibility(showConcepts) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x22101824))
                            .border(1.dp, Violet.copy(alpha = .38f), RoundedCornerShape(14.dp))
                            .padding(9.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text("Math Concepts · 27 subjects", color = Violet, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            MathConceptCategories.forEach { concept ->
                                GlowButton(concept) { futureSelection = concept }
                            }
                        }
                    }
                }

                GlowButton(if (showSuggestions) "Hide more ideas" else "More workspace ideas") {
                    showSuggestions = !showSuggestions
                }
                AnimatedVisibility(showSuggestions) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SUGGESTED NEXT", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestedMathTools.forEach { option ->
                                GlowButton(option.title, icon = option.icon) { futureSelection = option.title }
                            }
                        }
                    }
                }

                futureSelection?.let { selection ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Amber.copy(alpha = .08f))
                            .border(1.dp, Amber.copy(alpha = .28f), RoundedCornerShape(12.dp))
                            .padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TransparentIcon("+", Amber)
                        Column(Modifier.weight(1f)) {
                            Text(selection, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Workspace button ready for content to be added.", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun radialBackdrop() = Brush.radialGradient(
    colors = listOf(Color(0x3320D9FF), Background, Background),
    radius = 1100f,
    center = Offset(420f, 220f),
)

@Composable
private fun TopShell(vm: ExplorerViewModel, compact: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(!compact) }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val importProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || activity == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching { MathFileExchange.readProject(activity, uri) }
                .onSuccess { result ->
                    result.state?.let { vm.importWorkspace(it, result.recovered, result.diagnostics) }
                        ?: vm.reportStatus(result.diagnostics.firstOrNull() ?: "This project could not be imported")
                }
                .onFailure { vm.reportStatus("Import failed: ${it.message ?: "invalid project"}") }
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .78f))
            .animateContentSize()
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            GlowButton("Maths", icon = "←", iconOnly = compact, onClick = vm::returnToMathMenu)
            GlowButton("Menu", icon = "≡", iconOnly = compact, onClick = vm::toggleMathMenu)
        }
        Column(
            Modifier.clickable { expanded = !expanded }.padding(horizontal = 5.dp)
                .semantics { contentDescription = "Breadcrumb ${vm.mathsBreadcrumb.joinToString(" → ")}" },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AI Explorer ${if (expanded) "⌃" else "⌄"}", color = Ink, fontSize = if (compact) 18.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                vm.mathsBreadcrumb.joinToString(" → "),
                color = Muted,
                fontSize = if (compact) 9.sp else 12.sp,
                maxLines = 1,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 8.dp)) {
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↶" else "Undo", enabled = vm.canUndo, onClick = vm::undo) }
            AnimatedVisibility(expanded || !compact) { GlowButton(if (compact) "↷" else "Redo", enabled = vm.canRedo, onClick = vm::redo) }
            AnimatedVisibility(expanded && !compact) { GlowButton("Save", onClick = vm::saveWorkspace) }
            AnimatedVisibility(expanded && !compact) { GlowButton("Import") { importProject.launch(arrayOf("application/*", "text/plain")) } }
            AnimatedVisibility(expanded && !compact) {
                GlowButton("Share") {
                    if (activity == null) vm.reportStatus("Sharing is unavailable in this window")
                    else scope.launch { runCatching { MathFileExchange.shareProject(activity, vm.state) }.onFailure { vm.reportStatus("Share failed: ${it.message}") } }
                }
            }
            AnimatedVisibility(expanded && !compact) {
                GlowButton("PNG") {
                    if (activity == null) vm.reportStatus("Image export is unavailable in this window")
                    else scope.launch { runCatching { MathFileExchange.sharePng(activity, vm.state) }.onFailure { vm.reportStatus("Image export failed: ${it.message}") } }
                }
            }
            AnimatedVisibility(vm.state.module != MathModule.Graph2D) {
                GlowButton(if (compact) "⋮" else "More", enabled = true, onClick = vm::toggleActionDock)
            }
        }
    }
}

@Composable
private fun LearningCoachPanel(vm: ExplorerViewModel, modifier: Modifier = Modifier) {
    val activity = vm.activeActivity
    val progress = "${vm.completedActivities.size}/${LearningActivities.size}"
    val linkedSnapshot = vm.linkedMathSnapshot
    val universalDocument = vm.universalMathDocument
    GlassPanel(
        modifier
            .width(320.dp)
            .padding(top = 112.dp, end = 8.dp),
    ) {
        PanelHeader("Learning Coach", vm::hidePanels, Green)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton(if (vm.learningRole == LearningRole.Learner) "• Learner" else "Learner", onClick = { vm.switchLearningRole(LearningRole.Learner) })
            GlowButton(if (vm.learningRole == LearningRole.Teacher) "• Teacher" else "Teacher", onClick = { vm.switchLearningRole(LearningRole.Teacher) })
        }
        Text("Progress $progress · ${vm.activeProgress?.percent(activity) ?: 0}% current", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(activity.title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(activity.objective, color = Muted, fontSize = 13.sp)
        Insight("Allowed", activity.allowedTools.joinToString().ifBlank { "Open exploration" }, Cyan)
        Insight(
            "Linked maths",
            "CAS ${linkedSnapshot.objectsFor(LinkedMathView.CAS).size} · Graph ${linkedSnapshot.objectsFor(LinkedMathView.Graph).size} · Table ${linkedSnapshot.objectsFor(LinkedMathView.Table).size} · Geometry ${linkedSnapshot.objectsFor(LinkedMathView.Geometry).size} · Probability ${linkedSnapshot.objectsFor(LinkedMathView.Probability).size}",
            Violet,
        )
        Insight(
            "Maths authority",
            "${universalDocument.objects.size} typed objects · revision ${universalDocument.revision} · one dependency graph",
            Green,
        )
        if (linkedSnapshot.diagnostics.isNotEmpty()) Insight("Kernel diagnostics", linkedSnapshot.diagnostics.take(2).joinToString(), Amber)
        Insight("Proof", activity.proof, Violet)
        Insight("Validation", vm.lastValidation.message, if (vm.lastValidation.passed) Green else Amber)
        activity.checkpoints.forEach { checkpoint ->
            val result = vm.lastValidation.checkpoints.firstOrNull { it.checkpointId == checkpoint.id }
            val completed = checkpoint.id in vm.activeProgress?.completedCheckpointIds.orEmpty()
            Insight(
                if (completed || result?.passed == true) "✓ ${checkpoint.title}" else "○ ${checkpoint.title}",
                result?.message ?: checkpoint.instruction,
                if (completed || result?.passed == true) Green else Cyan,
            )
            result?.misconception?.let { Insight("Why not yet", it, Amber) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("Go", onClick = { vm.startActivity(activity) })
            GlowButton("Check", onClick = { vm.validateActiveActivity() })
            GlowButton("Hint", onClick = vm::revealHint)
            GlowButton("Done", onClick = vm::completeActiveActivity)
            GlowButton("Save", onClick = vm::saveWorkspace)
            GlowButton("Package", onClick = { vm.exportLearningPackage() })
        }
        vm.activeProgress?.let {
            Text("Attempts ${it.attempts} · hints ${it.hintsUsed} · offline changes ${vm.pendingLearningOperations}", color = Muted, fontSize = 11.sp)
        }
        if (vm.learningRole == LearningRole.Teacher) {
            val summary = vm.teacherSummary
            Text("Teacher dashboard", color = Ink, fontWeight = FontWeight.SemiBold)
            Insight("Assignment", vm.assignments.first().title, Violet)
            Insight("Completion", "${summary.completedLessons}/${summary.assignedLessons} lessons · ${summary.checkpointsCompleted} checkpoints", Green)
            Insight("Support", "${summary.attempts} attempts · ${summary.hintsUsed} hints", Amber)
            Insight("Needs attention", summary.needsAttention.joinToString().ifBlank { "No learner flags" }, summary.needsAttention.takeIf { it.isNotEmpty() }?.let { Amber } ?: Green)
        }
        InteractiveActivityStudioCard(vm.learningRole)
        ProductionReadinessCard(vm)
        ReleaseQaLabCard()
        Text("Settings", color = Ink, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill("Haptics", vm.settings.haptics) { value -> vm.updateSettings { it.copy(haptics = value) } }
            TogglePill("Snap", vm.settings.snap) { value -> vm.updateSettings { it.copy(snap = value) } }
            TogglePill("High contrast", vm.settings.highContrast) { value -> vm.updateSettings { it.copy(highContrast = value) } }
            TogglePill("Reduced motion", vm.settings.reducedMotion) { value -> vm.updateSettings { it.copy(reducedMotion = value) } }
            TogglePill("Spoken maths", vm.settings.spokenMath) { value -> vm.updateSettings { it.copy(spokenMath = value) } }
            TogglePill("Graph audio", vm.settings.graphSonification) { value -> vm.updateSettings { it.copy(graphSonification = value) } }
            TogglePill("Large targets", vm.settings.largeTouchTargets) { value -> vm.updateSettings { it.copy(largeTouchTargets = value) } }
        }
        Text("Activities", color = Ink, fontWeight = FontWeight.SemiBold)
        Column(
            Modifier
                .height(220.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LearningActivities.forEach { item ->
                val done = item.id in vm.completedActivities
                val active = item.id == activity.id
                LearningActivityRow(
                    activity = item,
                    active = active,
                    completed = done,
                    onClick = { vm.startActivity(item) },
                )
            }
        }
        if (vm.savedWorkspaces.isNotEmpty()) {
            Text("Saved Workspaces", color = Ink, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.savedWorkspaces.take(3).forEach { saved ->
                    SavedWorkspaceRow(
                        saved = saved,
                        onOpen = { vm.restoreWorkspaceShell(saved) },
                        onDuplicate = { vm.duplicateWorkspace(saved) },
                        onDelete = { vm.deleteWorkspace(saved) },
                    )
                }
            }
        }
        Text("Workspace package", color = Ink, fontWeight = FontWeight.SemiBold)
        Insight("Package validation", vm.lastPackageValidation.message, if (vm.lastPackageValidation.valid) Green else Muted)
        Text(
            text = vm.exportLearningPackagePreview(),
            color = Muted,
            fontSize = 11.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x33101824))
                .padding(10.dp),
        )
    }
}

@Composable
private fun ProductionReadinessCard(vm: ExplorerViewModel) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val document = vm.universalMathDocument
    val archive = remember(document.revision, vm.settings) {
        AIExplorerProjectArchive.encode(
            AIExplorerProjectArchive.create(
                vm.state.id, document, vm.state.modifiedAt, System.currentTimeMillis(),
                listOf(ProjectSection(ProjectSectionKind.Settings, "highContrast=${vm.settings.highContrast};reducedMotion=${vm.settings.reducedMotion};spokenMath=${vm.settings.spokenMath};graphAudio=${vm.settings.graphSonification}")),
            ),
        )
    }
    val graphEvaluations = vm.state.functions.size * 500
    val objectCount = document.objects.size
    val performance = remember(archive.length, graphEvaluations, objectCount) {
        ProductPerformanceManager.assess(ProductPerformanceSnapshot(16.0, graphEvaluations, objectCount, archive.toByteArray().size.toLong(), objectCount * 12_000L))
    }
    val memoryClass = remember {
        (context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as? ActivityManager)?.memoryClass ?: 256
    }
    val reliability = remember { LocalReliabilityMonitor(context).snapshot() }
    val device = remember(memoryClass) {
        DeviceCapabilityManager.assess(DeviceCapabilityProfile(Build.VERSION.SDK_INT, Runtime.getRuntime().availableProcessors(), memoryClass, 3, false, false, false, memoryClass <= 128))
    }
    val exchange = remember(vm.state.modifiedAt) { GeoGebraExchange.exportXml(vm.state).coverage }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Green.copy(.07f))
            .border(1.dp, Green.copy(.45f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Project & device readiness", color = Green, fontWeight = FontWeight.Bold)
                Text("Checksummed save · exchange coverage · adaptive quality", color = Muted, fontSize = 11.sp)
            }
            GlowButton(if (expanded) "Hide" else "Inspect") { expanded = !expanded }
        }
        Insight("Runtime", "${performance.status} ${performance.score}/100 · ${device.tier}", if (performance.score >= 90) Green else Amber)
        if (expanded) {
            Insight("Local reliability", "${"%.2f".format(reliability.cleanSessionRate * 100)}% clean · ${reliability.sessions} sessions", if (reliability.cleanSessionRate >= .998) Green else Amber)
            Insight("Project archive", "${archive.toByteArray().size / 1024} KB · ${document.objects.size} typed maths objects", Cyan)
            Insight("GeoGebra XML", "${exchange.exported} translated · ${exchange.skipped.size} explicitly skipped", Violet)
            Insight("Fallback", if ("live AR" in device.enabled) "Live AR available" else "Full simulator and 2D maths remain enabled", Amber)
            Insight("Surface density", device.recommendedSurfaceDensity.toString(), Cyan)
            performance.messages.forEach { Text(it, color = if (performance.status.name == "Pass") Green else Amber, fontSize = 11.sp) }
            exchange.skipped.take(3).forEach { Text("Exchange gap · $it", color = Amber, fontSize = 10.sp) }
            Text("Live AR capability is finalized inside the AR workspace after permission and ARCore checks.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ReleaseQaLabCard() {
    var expanded by remember { mutableStateOf(false) }
    var benchmark by remember { mutableStateOf<MathBenchmarkReport?>(null) }
    val security = remember {
        AppSecurityAuditEngine.audit(
            AppSecurityConfiguration(
                permissions = setOf(Manifest.permission.CAMERA),
                exportedComponents = mapOf("MainActivity" to true),
                cleartextTrafficAllowed = false,
                backupAllowed = true,
                cameraFramesPersisted = false,
                cameraFramesUploaded = false,
                secretsInSource = false,
                networkTransportsAttached = emptySet(),
            ),
        )
    }
    val accessibility = remember {
        AccessibilityQaEngine.audit(
            listOf(
                AccessibilityNodeEvidence("main-menu", "Open main menu", "Button", 48.0, 48.0, true, 7.0),
                AccessibilityNodeEvidence("workspace", "Interactive maths workspace", "Canvas", 48.0, 48.0, true, 7.0, true, true),
                AccessibilityNodeEvidence("solver", "Open problem solver", "Button", 48.0, 48.0, true, 7.0),
                AccessibilityNodeEvidence("tools", "Open movable tools", "Button", 48.0, 48.0, true, 7.0),
            ),
        )
    }
    val evidence = remember(benchmark, security, accessibility) {
        val maths = benchmark
        ReleaseQaEvidenceCodec.encode(
            ReleaseQaEvidenceBundle(
                buildLabel = "local-debug",
                deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}",
                createdAt = System.currentTimeMillis(),
                sections = listOf(
                    QaEvidenceSection("maths", maths?.let { if (it.failed == 0) "PASS" else "FAIL" } ?: "NOT_RUN", listOf(maths?.let { "${it.passed}/${it.results.size} deterministic cases" } ?: "Run the local suite")),
                    QaEvidenceSection("accessibility-contract", if (accessibility.passed) "PASS" else "FAIL", listOf("${accessibility.nodesChecked} declared interaction contracts")),
                    QaEvidenceSection("security-config", if (security.passed) "PASS" else "FAIL", security.findings.map { "${it.severity}: ${it.message}" }),
                    QaEvidenceSection("physical-ar", "NOT_RECORDED", listOf("Record anchor drift, depth error, frame p95 and thermal state on a supported device")),
                ),
            ),
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Cyan.copy(.06f))
            .border(1.dp, Cyan.copy(.4f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Release QA Lab", color = Cyan, fontWeight = FontWeight.Bold)
                Text("Deterministic tests and tamper-evident evidence", color = Muted, fontSize = 11.sp)
            }
            GlowButton(if (expanded) "Hide" else "Open") { expanded = !expanded }
        }
        val result = benchmark
        Insight("Math benchmark", result?.let { "${it.passed}/${it.results.size} passed" } ?: "Not run on this build", result?.let { if (it.failed == 0) Green else Amber } ?: Muted)
        Insight("Physical AR", "Not recorded - device run required", Amber)
        if (expanded) {
            GlowButton(if (result == null) "Run maths suite" else "Run again") {
                benchmark = DeterministicMathBenchmarkRunner().run(ReleaseMathBenchmarkCatalog.smoke)
            }
            result?.topicCoverage?.forEach { (topic, counts) ->
                Text("${topic.name}: ${counts.first}/${counts.second}", color = if (counts.first == counts.second) Green else Amber, fontSize = 11.sp)
            }
            Insight("Accessibility contract", if (accessibility.passed) "${accessibility.nodesChecked} key interaction contracts pass" else "${accessibility.findings.size} findings", if (accessibility.passed) Green else Amber)
            Insight("Security configuration", if (security.passed) "No release-blocking configuration found" else "Release blockers found", if (security.passed) Green else Amber)
            security.findings.take(3).forEach { Text("${it.severity} - ${it.message}", color = if (it.severity.name == "Info") Muted else Amber, fontSize = 10.sp) }
            Insight("Evidence integrity", if (ReleaseQaEvidenceCodec.verify(evidence)) "SHA-256 verified" else "Checksum failed", Green)
            Text("AR PASS is never inferred from simulator or unit tests.", color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InteractiveActivityStudioCard(role: LearningRole) {
    val engine = remember { InteractiveActivityEngine() }
    var document by remember { mutableStateOf(InteractiveActivityCatalog.unitCircle) }
    var run by remember(document.id, document.revision) { mutableStateOf(engine.start(document)) }
    var response by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val validation = remember(document) { InteractiveActivityAuthoring.validate(document) }
    val current = document.blocks.firstOrNull { it.id == run.currentBlockId }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Violet.copy(.08f))
            .border(1.dp, Violet.copy(.5f), RoundedCornerShape(14.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (role == LearningRole.Teacher) "Interactive Activity Studio" else "Adaptive activity", color = Violet, fontWeight = FontWeight.Bold)
                Text(document.title, color = Ink, fontSize = 12.sp)
            }
            GlowButton(if (expanded) "Collapse" else "Open") { expanded = !expanded }
        }
        Insight("Activity", "${document.blocks.size} blocks · ${if (validation.valid) "ready" else "needs links"} · revision ${document.revision}", if (validation.valid) Green else Amber)
        if (expanded) {
            if (role == LearningRole.Teacher) {
                Text("Author blocks", color = Cyan, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Instruction", "Math check", "MCQ", "Workspace", "Tiles", "Proof", "Branch", "Reflection").forEach { label -> TransparentIcon(label.take(2), Cyan) }
                }
                document.blocks.forEachIndexed { index, block -> Text("${index + 1}. ${block.javaClass.simpleName} · ${block.title}", color = Muted, fontSize = 11.sp) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Add reflection") {
                        val id = "reflection-${document.revision + 1}"
                        val terminal = document.blocks.last()
                        val linked = when (terminal) {
                            is ActivityBlock.Reflection -> terminal.copy(nextOnPass = id)
                            else -> terminal
                        }
                        document = InteractiveActivityAuthoring.add(InteractiveActivityAuthoring.replace(document, linked), ActivityBlock.Reflection(id, "Teacher reflection", "Explain the invariant in your own words."))
                    }
                    GlowButton("Validate") { document = document.copy(revision = document.revision + 1) }
                }
                validation.errors.forEach { Text(it, color = Amber, fontSize = 11.sp) }
                Text("Export · ${InteractiveActivityAuthoring.serialize(document).take(72)}…", color = Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            } else if (run.completed) {
                Text("Activity complete · score ${run.score}%", color = Green, fontWeight = FontWeight.Bold)
                run.mastery.values.forEach { Insight(it.skill, "${trim(it.score * 100)}% · ${it.band}", Green) }
                GlowButton("Restart") { run = engine.start(document); response = "" }
            } else if (current != null) {
                Text(current.title, color = Cyan, fontWeight = FontWeight.Bold)
                when (current) {
                    is ActivityBlock.Instruction -> {
                        Text(current.body, color = Ink, fontSize = 12.sp)
                        GlowButton("Continue") { run = engine.submit(document, run, ActivityAnswer.Continue) }
                    }
                    is ActivityBlock.MathResponse -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        IntentAwareMathField(response, { response = it }, "Your expression", Modifier.fillMaxWidth(), showLegend = false)
                        GlowButton("Check without revealing") { run = engine.submit(document, run, ActivityAnswer.Text(response), ActivityEvaluationContext(now = System.currentTimeMillis())); response = "" }
                    }
                    is ActivityBlock.MultipleChoice -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        current.choices.forEachIndexed { index, choice -> GlowButton(choice) { run = engine.submit(document, run, ActivityAnswer.Choice(index), ActivityEvaluationContext(now = System.currentTimeMillis())) } }
                    }
                    is ActivityBlock.Reflection -> {
                        Text(current.prompt, color = Ink, fontSize = 12.sp)
                        OutlinedTextField(response, { response = it }, Modifier.fillMaxWidth(), label = { Text("Explain your reasoning") })
                        GlowButton("Finish") { run = engine.submit(document, run, ActivityAnswer.Text(response), ActivityEvaluationContext(now = System.currentTimeMillis())); response = "" }
                    }
                    else -> Text("Open ${document.module.label} and complete this interactive check.", color = Muted, fontSize = 12.sp)
                }
                run.results.lastOrNull()?.takeIf { it.blockId == current.id }?.let { Text(it.feedback, color = if (it.passed) Green else Amber, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun LearningActivityRow(
    activity: LearningActivity,
    active: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
) {
    val color = when {
        completed -> Green
        active -> Cyan
        else -> Muted
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Color(0x3320D9FF) else Color(0x22101824))
            .border(1.dp, color.copy(.55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Text("${if (completed) "Done" else activity.module.label} - ${activity.title}", color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text(activity.target, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SavedWorkspaceRow(
    saved: SavedWorkspace,
    onOpen: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22101824))
            .border(1.dp, Cyan.copy(.38f), RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        Text(saved.name, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Text("${saved.module.label} workspace", color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Open", onClick = onOpen)
            GlowButton("Copy", onClick = onDuplicate)
            GlowButton("Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun BottomModeSelector(active: MathModule, onSelect: (MathModule) -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(!compact) }
    val activeLabel = when (active) {
        MathModule.Geometry2D -> "2D"
        MathModule.Geometry3D -> "3D"
        MathModule.Graph2D -> "Graph"
        MathModule.Graph3D -> "G3D"
        MathModule.Trigonometry -> "Trig"
        MathModule.Manipulatives -> "Tiles"
        MathModule.SpatialAR -> "AR"
    }
    Row(
        modifier
            .fillMaxWidth(if (expanded) .98f else .52f)
            .widthIn(max = 980.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(SurfaceA.copy(alpha = .96f))
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(26.dp))
            .animateContentSize()
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!expanded) {
            TransparentIcon(moduleIcon(active), Cyan)
            Text(activeLabel, color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("⌃", color = Cyan, fontSize = 18.sp, modifier = Modifier.clickable { expanded = true }.padding(horizontal = 12.dp, vertical = 8.dp).semantics { contentDescription = "Show navigation" })
        }
        if (expanded) MathModule.entries.forEach { module ->
            val selected = module == active
            val compactLabel = when (module) {
                MathModule.Geometry2D -> "2D"
                MathModule.Geometry3D -> "3D"
                MathModule.Graph2D -> "Graph"
                MathModule.Graph3D -> "G3D"
                MathModule.Trigonometry -> "Trig"
                MathModule.Manipulatives -> "Tiles"
                MathModule.SpatialAR -> "AR"
            }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Brush.horizontalGradient(listOf(Violet, Cyan)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                    .clickable { onSelect(module) }
                    .focusable()
                    .padding(horizontal = 2.dp, vertical = 5.dp)
                    .semantics { contentDescription = "Open ${module.label} workspace" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(moduleIcon(module), color = if (selected) Color.White else Muted, fontSize = 13.sp)
                Text(compactLabel, color = if (selected) Color.White else Muted, textAlign = TextAlign.Center, maxLines = 1, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            }
        }
        if (expanded) Text("⌄", color = Muted, modifier = Modifier.clickable { expanded = false }.padding(horizontal = 6.dp, vertical = 10.dp).semantics { contentDescription = "Collapse navigation" })
    }
}

private data class ShapeExplorer2DDetails(val formula: String, val area: Double, val perimeter: Double)

private fun copyShapeText(context: android.content.Context, label: String, value: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, value))
}

private fun safeShapeFileName(title: String) = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "shape" }

private fun writeShapeTextAsset(context: android.content.Context, title: String, extension: String, content: String): java.io.File {
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.$extension")
    file.writeText(content)
    return file
}

private fun writeShapePdf(context: android.content.Context, title: String, lines: List<String>): java.io.File {
    val document = android.graphics.pdf.PdfDocument()
    val page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(15, 30, 45); textSize = 18f }
    page.canvas.drawText(title, 42f, 58f, paint.apply { textSize = 24f; isFakeBoldText = true })
    paint.isFakeBoldText = false; paint.textSize = 14f
    lines.take(42).forEachIndexed { index, line -> page.canvas.drawText(line.take(76), 42f, 92f + index * 17f, paint) }
    document.finishPage(page)
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.pdf")
    java.io.FileOutputStream(file).use(document::writeTo)
    document.close()
    return file
}

private fun writeShapePng(context: android.content.Context, title: String, subtitle: String): java.io.File {
    val bitmap = android.graphics.Bitmap.createBitmap(1000, 1000, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.rgb(3, 8, 14))
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(32, 217, 255); textAlign = android.graphics.Paint.Align.CENTER; textSize = 62f; isFakeBoldText = true }
    canvas.drawText(title, 500f, 430f, paint)
    paint.color = android.graphics.Color.rgb(220, 230, 242); paint.textSize = 32f; paint.isFakeBoldText = false
    canvas.drawText(subtitle.take(54), 500f, 510f, paint)
    paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = 6f; paint.color = android.graphics.Color.rgb(152, 93, 255)
    canvas.drawRoundRect(90f, 90f, 910f, 910f, 42f, 42f, paint)
    val file = java.io.File(context.cacheDir, "${safeShapeFileName(title)}.png")
    java.io.FileOutputStream(file).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()
    return file
}

private fun shape2DSvg(shape: Shape2D, points: List<Vec2>): String {
    val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
    val body = when (shape.type) {
        Shape2DType.Circle -> if (shapePoints.size >= 2) "<circle cx='400' cy='400' r='${shapePoints[0].distanceTo(shapePoints[1]) * 80}'/>" else ""
        Shape2DType.Ellipse -> if (shapePoints.size >= 3) "<ellipse cx='400' cy='400' rx='${shapePoints[0].distanceTo(shapePoints[1]) * 80}' ry='${shapePoints[0].distanceTo(shapePoints[2]) * 80}'/>" else ""
        else -> "<polygon points='${shapePoints.joinToString(" ") { "${400 + it.x * 80},${400 - it.y * 80}" }}'/ >".replace("/ >", "/>")
    }
    return "<svg xmlns='http://www.w3.org/2000/svg' width='800' height='800' viewBox='0 0 800 800'><g fill='#20d9ff33' stroke='#20d9ff' stroke-width='5'>$body</g></svg>"
}

private fun solidObj(solid: Solid): String {
    val mesh = SolidMeshFactory.create(solid)
    return buildString {
        appendLine("o ${solid.type.name}")
        mesh.vertices.forEach { appendLine("v ${it.x} ${it.y} ${it.z}") }
        mesh.faces.forEach { face -> appendLine("f ${face.joinToString(" ") { (it + 1).toString() }}") }
    }
}

@Composable
private fun Shape2DStudioPanel(vm: ExplorerViewModel, shape: Shape2D, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preset = ShapeExplorer2DShapes.firstOrNull { shape.id.contains("shape-explorer-${it.id}") }
    val formulas = shape2DFormulaLibrary(preset?.label ?: shape.name)
    val details = shapeExplorer2DDetails(shape, vm.state.points)
    var name by remember(shape.id, shape.name) { mutableStateOf(shape.name) }
    var scaleText by remember(shape.id) { mutableStateOf("1.25") }
    val shapePoints = shape.pointIndices.mapNotNull { vm.state.points.getOrNull(it) }
    val currentWidth = (shapePoints.maxOfOrNull { it.x } ?: 1.0) - (shapePoints.minOfOrNull { it.x } ?: 0.0)
    val currentHeight = (shapePoints.maxOfOrNull { it.y } ?: 1.0) - (shapePoints.minOfOrNull { it.y } ?: 0.0)
    var widthText by remember(shape.id) { mutableStateOf(trim(currentWidth)) }
    var heightText by remember(shape.id) { mutableStateOf(trim(currentHeight)) }
    var keepProportions by remember(shape.id) { mutableStateOf(true) }
    var unit by remember { mutableStateOf("unit") }
    var lessonStep by remember { mutableIntStateOf(0) }
    val unitFactor = when (unit) { "cm" -> 100.0; "mm" -> 1000.0; "in" -> 39.3701; else -> 1.0 }
    val allShapeDetails = vm.state.shapes.map { shapeExplorer2DDetails(it, vm.state.points) }
    val panelModifier = if (compact) modifier.fillMaxWidth(.96f) else modifier.padding(top = 64.dp).width(380.dp)
    GlassPanel(panelModifier) {
        PanelHeader("2D Shape Studio", vm::hidePanels, Violet, icon = "ƒ")
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Object name") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Rename") { vm.renameExplorerSelection(name) }
            GlowButton("Duplicate", onClick = vm::duplicateExplorerSelection)
            GlowButton("Reset", onClick = vm::resetExplorerSelection)
        }
        Text("PRECISION & TRANSFORM", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = widthText, onValueChange = { widthText = it }, singleLine = true, label = { Text("Width") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = heightText, onValueChange = { heightText = it }, singleLine = true, label = { Text("Height") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (keepProportions) "• Keep ratio" else "Free ratio") { keepProportions = !keepProportions }
            GlowButton("Set dimensions") { vm.resizeExplorerShape2D(widthText.toDoubleOrNull() ?: currentWidth, heightText.toDoubleOrNull() ?: currentHeight, keepProportions) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(value = scaleText, onValueChange = { scaleText = it }, singleLine = true, label = { Text("Scale") }, modifier = Modifier.width(110.dp))
            GlowButton("Apply") { scaleText.toDoubleOrNull()?.let(vm::scaleExplorerShape2D) }
            GlowButton(if (shape.locked) "Unlock" else "Lock") { vm.updateSelectedShape { it.copy(locked = !it.locked) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Rotate -15°") { vm.rotateExplorerShape2D(-15.0) }
            GlowButton("Rotate +15°") { vm.rotateExplorerShape2D(15.0) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("unit", "cm", "mm", "in").forEach { choice -> GlowButton(if (unit == choice) "• $choice" else choice) { unit = choice } }
        }
        Insight("Area", "${trim(details.area * unitFactor * unitFactor)} $unit²", Violet)
        Insight("Perimeter", "${trim(details.perimeter * unitFactor)} $unit", Green)
        if (vm.state.shapes.size > 1) {
            Insight("Composite area", trim(allShapeDetails.sumOf { it.area }), Amber)
            Insight("Composite perimeter", trim(allShapeDetails.sumOf { it.perimeter }), Amber)
        }
        Text("ALL FORMULAS · ${formulas.size}", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        formulas.forEach { formula -> Insight(formula.name, formula.expression, Violet) }
        GlowButton("Copy all formulas", icon = "Copy") { copyShapeText(context, "${shape.name} formulas", formulas.joinToString("\n") { "${it.name}: ${it.expression}" }) }
        Text("SUBSTITUTION & METHOD", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Current result: A = ${trim(details.area)}, P = ${trim(details.perimeter)}. Measure the labelled control points, substitute them into the selected identity, preserve units, then simplify.", color = Ink, fontSize = 11.sp)
        Insight("Variables", "A area · P perimeter · a,b sides · h height · r radius", Cyan)
        Insight("Symmetry", when (preset?.label) { "Circle" -> "infinitely many lines"; "Square" -> "4 lines, order 4"; "Rectangle", "Rhombus" -> "2 lines, order 2"; "Equilateral Triangle" -> "3 lines, order 3"; else -> "depends on current geometry" }, Amber)
        Insight("Angles", "Interior sum = (n-2) × 180°", Amber)
        Insight("Circles", "Inradius r = A/s · circumradius R from perpendicular bisectors", Amber)
        Text("Decompose into triangles to verify area; combine multiple objects for composite figures. Resizing demonstrates that perimeter scales by k while area scales by k².", color = Muted, fontSize = 10.sp)
        Text("GUIDED CONSTRUCTION", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        val steps = listOf("Identify defining dimensions", "Place the first control point", "Construct remaining vertices", "Check constraints and symmetry", "Verify area and perimeter")
        Text("${lessonStep + 1}/${steps.size} · ${steps[lessonStep]}", color = Ink, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Previous", enabled = lessonStep > 0) { lessonStep-- }
            GlowButton("Next", enabled = lessonStep < steps.lastIndex) { lessonStep++ }
            GlowButton("Challenge") { lessonStep = (lessonStep + 1) % steps.size }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Save scene", onClick = vm::saveWorkspace)
            GlowButton("Copy results") { copyShapeText(context, shape.name, "${shape.name}: area=${trim(details.area)}, perimeter=${trim(details.perimeter)}") }
        }
        Text("EXPORT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("SVG") { val file = writeShapeTextAsset(context, shape.name, "svg", shape2DSvg(shape, vm.state.points)); copyShapeText(context, "SVG export", file.absolutePath) }
            GlowButton("PNG") { val file = writeShapePng(context, shape.name, "Area ${trim(details.area)} · Perimeter ${trim(details.perimeter)}"); copyShapeText(context, "PNG export", file.absolutePath) }
            GlowButton("PDF") { val file = writeShapePdf(context, shape.name, formulas.map { "${it.name}: ${it.expression}" } + "Area: ${trim(details.area)}" + "Perimeter: ${trim(details.perimeter)}"); copyShapeText(context, "PDF export", file.absolutePath) }
        }
    }
}

@Composable
private fun Shape3DStudioPanel(vm: ExplorerViewModel, index: Int, solid: Solid, compact: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val formulas = solidFormulaLibrary(solid.type)
    val measurements = Geometry3D.measure(solid)
    var unit by remember { mutableStateOf("unit") }
    var fold by remember { mutableFloatStateOf(0f) }
    var lessonStep by remember { mutableIntStateOf(0) }
    val factor = when (unit) { "cm" -> 100.0; "mm" -> 1000.0; "in" -> 39.3701; else -> 1.0 }
    val totals = vm.state.solids.map(Geometry3D::measure)
    val panelModifier = if (compact) modifier.fillMaxWidth(.96f) else modifier.padding(top = 64.dp).width(390.dp)
    GlassPanel(panelModifier) {
        PanelHeader("3D Shape Studio", vm::hidePanels, Violet, icon = "3D")
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Duplicate", onClick = vm::duplicateExplorerSelection)
            GlowButton("Reset", onClick = vm::resetExplorerSelection)
            GlowButton("AR", icon = "AR", onClick = vm::openCurrentShapeInAr)
        }
        Text("EXACT DIMENSIONS", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        AxisSlider("Width", solid.width.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(width = value.toDouble()) } }
        AxisSlider("Height", solid.height.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(height = value.toDouble()) } }
        AxisSlider("Depth", solid.depth.toFloat(), .2f..12f) { value -> vm.transformSolid(index) { it.copy(depth = value.toDouble()) } }
        AxisSlider("Radius", solid.radius.toFloat(), .1f..6f) { value -> vm.transformSolid(index) { it.copy(radius = value.toDouble()) } }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Rotate X") { vm.transformSolid(index) { it.copy(rotation = it.rotation + Vec3(15.0, 0.0, 0.0)) } }
            GlowButton("Rotate Y") { vm.transformSolid(index) { it.copy(rotation = it.rotation + Vec3(0.0, 15.0, 0.0)) } }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("unit", "cm", "mm", "in").forEach { choice -> GlowButton(if (unit == choice) "• $choice" else choice) { unit = choice } } }
        Insight("Surface area", "${trim(measurements.surfaceArea * factor * factor)} $unit²", Violet)
        Insight("Volume", "${trim(measurements.volume * factor * factor * factor)} $unit³", Green)
        Insight("Topology", "${measurements.faces} faces · ${measurements.edges} edges · ${measurements.vertices} vertices", Cyan)
        if (vm.state.solids.size > 1) {
            Insight("Composite surface", trim(totals.sumOf { it.surfaceArea }), Amber)
            Insight("Composite volume", trim(totals.sumOf { it.volume }), Amber)
        }
        Text("ALL FORMULAS · ${formulas.size}", color = Violet, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        formulas.forEach { formula -> Insight(formula.name, formula.expression, Violet) }
        GlowButton("Copy all formulas", icon = "Copy") { copyShapeText(context, "${solid.type} formulas", formulas.joinToString("\n") { "${it.name}: ${it.expression}" }) }
        Text("DERIVATION & VARIABLES", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Substitute the live dimensions shown above. Base-area methods use V = Bh for prisms and V = Bh/3 for pyramids; surface area is the sum of every exposed face.", color = Ink, fontSize = 11.sp)
        Insight("Variables", "B base · p perimeter · h height · r radius · s slant", Cyan)
        Insight("Bounds", "${trim(solid.width)} × ${trim(solid.height)} × ${trim(solid.depth)}", Amber)
        Text("NET & CROSS-SECTION", color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("Net: ${measurements.faces} connected face panels · fold ${trim(fold.toDouble() * 100)}%", color = Ink, fontSize = 11.sp)
        Slider(value = fold, onValueChange = { fold = it }, valueRange = 0f..1f)
        Text("Use the 3D Tools pane outside Shape Studio for live clipping and planar cross-sections. Scaling by k preserves shape while surface changes by k² and volume by k³.", color = Muted, fontSize = 10.sp)
        val steps = listOf("Identify the base", "Construct or inspect the net", "Fold faces around shared edges", "Measure height and radius", "Verify surface area and volume")
        Text("Lesson ${lessonStep + 1}/${steps.size} · ${steps[lessonStep]}", color = Ink, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Previous", enabled = lessonStep > 0) { lessonStep-- }
            GlowButton("Next", enabled = lessonStep < steps.lastIndex) { lessonStep++ }
            GlowButton("Challenge") { lessonStep = (lessonStep + 1) % steps.size }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Save scene", onClick = vm::saveWorkspace)
            GlowButton("Copy results") { copyShapeText(context, solid.type.name, "${solid.type}: surface=${trim(measurements.surfaceArea)}, volume=${trim(measurements.volume)}") }
        }
        Text("EXPORT", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("OBJ model") { val file = writeShapeTextAsset(context, solid.type.name, "obj", solidObj(solid)); copyShapeText(context, "OBJ export", file.absolutePath) }
            GlowButton("PNG") { val file = writeShapePng(context, solid.type.name, "Surface ${trim(measurements.surfaceArea)} · Volume ${trim(measurements.volume)}"); copyShapeText(context, "PNG export", file.absolutePath) }
            GlowButton("PDF") { val file = writeShapePdf(context, solid.type.name, formulas.map { "${it.name}: ${it.expression}" } + "Surface: ${trim(measurements.surfaceArea)}" + "Volume: ${trim(measurements.volume)}"); copyShapeText(context, "PDF export", file.absolutePath) }
        }
    }
}

private fun shapeExplorer2DDetails(shape: Shape2D, allPoints: List<Vec2>): ShapeExplorer2DDetails {
    val points = shape.pointIndices.mapNotNull { index -> allPoints.getOrNull(index) }
    val preset = ShapeExplorer2DShapes.firstOrNull { shape.id == "shape-explorer-${it.id}" }
    if (shape.type == Shape2DType.Circle && points.size >= 2) {
        val radius = points[0].distanceTo(points[1])
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), PI * radius * radius, 2 * PI * radius)
    }
    if (shape.type == Shape2DType.Ellipse && points.size >= 3) {
        val a = points[0].distanceTo(points[1])
        val b = points[0].distanceTo(points[2])
        val perimeter = PI * (3 * (a + b) - kotlin.math.sqrt((3 * a + b) * (a + 3 * b)))
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), PI * a * b, perimeter)
    }
    if (shape.type in setOf(Shape2DType.Rectangle, Shape2DType.Square) && points.size >= 2) {
        val width = abs(points[1].x - points[0].x)
        val height = if (shape.type == Shape2DType.Square) width else abs(points[1].y - points[0].y)
        return ShapeExplorer2DDetails(preset?.formula.orEmpty(), width * height, 2 * (width + height))
    }
    val perimeter = if (points.size >= 2) points.indices.sumOf { index -> points[index].distanceTo(points[(index + 1) % points.size]) } else 0.0
    return ShapeExplorer2DDetails(preset?.formula.orEmpty(), Geometry2D.polygonArea(points), perimeter)
}

@Composable
private fun Geometry2DScreen(vm: ExplorerViewModel, compact: Boolean) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var lassoEnabled by remember { mutableStateOf(false) }
    var boxSelectEnabled by remember { mutableStateOf(false) }
    var contextMenuShapeIndex by remember { mutableStateOf<Int?>(null) }
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var undoViewRequest by remember { mutableIntStateOf(0) }
    val a = vm.state.points[0]
    val b = vm.state.points[1]
    val dynamicEngine = remember { DynamicGeometryEngine() }
    val dynamicDocument = remember(a, b) {
        var document = DynamicGeometryDocument()
        document = dynamicEngine.addPoint(document, DynamicPoint("A", DynamicPointRule.Free(a)), "Free point selected by the learner")
        document = dynamicEngine.addPoint(document, DynamicPoint("B", DynamicPointRule.Free(b)), "Free point selected by the learner")
        document = dynamicEngine.addPoint(document, DynamicPoint("M", DynamicPointRule.Midpoint("A", "B")), "A midpoint is equidistant from both endpoints")
        document = dynamicEngine.addPoint(document, DynamicPoint("C", DynamicPointRule.Rotate("B", "A", 60.0)), "Rotate AB by 60 degrees")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Line("lineAB", "A", "B"), "Two distinct points determine a line")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Perpendicular("perpM", "M", "A", "B"), "Perpendicular directions have zero dot product")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Circle("circleA", "A", "B"), "Center and point determine a circle")
        document = dynamicEngine.addObject(document, DynamicGeometryObject.Ellipse("ellipse", "A", "B", "C"), "The sum of focal distances is constant")
        document
    }
    val m = Geometry2D.segment(a, b)
    val analyticLine = remember(a, b) { runCatching { AnalyticGeometry2D.lineThrough(a, b) }.getOrNull() }
    val third = Vec2(4.0, -1.5)
    val selectedShape = vm.state.shapes.getOrNull(vm.selectedShape)
    val dependenciesByOutput = vm.state.pointDependencies.associateBy { it.outputIndex }
    val invalidDependencyOutputs = vm.state.pointDependencies.filter {
        resolvePointDependency(vm.state.points, it.inputIndices, it.type, it.parameters) == null
    }.mapTo(mutableSetOf()) { it.outputIndex }
    val protocolSize = vm.state.points.size + vm.state.shapes.size + vm.state.geometryConstraints.size
    var protocolStep by remember { mutableFloatStateOf(protocolSize.toFloat()) }
    var focusedProtocolId by remember { mutableStateOf<String?>(null) }
    var protocolPlaying by remember { mutableStateOf(false) }
    var macroDraft by remember { mutableStateOf(com.indianservers.aiexplorer.workspace.GeometryMacroDraft("Canvas macro")) }
    var geometryTrace by remember { mutableStateOf<com.indianservers.aiexplorer.workspace.GeometryTraceSession?>(null) }
    LaunchedEffect(protocolSize) { protocolStep = protocolSize.toFloat() }
    LaunchedEffect(protocolPlaying, protocolSize) {
        while (protocolPlaying && protocolStep < protocolSize) { delay(650); protocolStep = (protocolStep + 1f).coerceAtMost(protocolSize.toFloat()) }
        if (protocolStep >= protocolSize) protocolPlaying = false
    }
    LaunchedEffect(vm.selectedShapes, vm.selectedPoint, macroDraft.recording) {
        val ids = vm.selectedShapes.mapNotNull(vm.state.shapes::getOrNull).map { it.id } + vm.selectedPoint.takeIf { it in vm.state.points.indices }?.let { "P${it + 1}" }
        macroDraft = Geometry2DDirectManipulation.recordMacro(macroDraft, ids.filterNotNull())
    }
    LaunchedEffect(vm.state.points, geometryTrace?.recording) {
        val trace = geometryTrace ?: return@LaunchedEffect
        val point = trace.objectId.removePrefix("P").toIntOrNull()?.minus(1)?.let(vm.state.points::getOrNull) ?: return@LaunchedEffect
        geometryTrace = Geometry2DDirectManipulation.appendTrace(trace, point)
    }
    val protocolTimeline = remember(vm.state, protocolStep, focusedProtocolId) {
        Geometry2DInteractionEngine.protocolTimeline(vm.state, protocolStep.roundToInt(), focusedProtocolId)
    }
    val contextInspector = remember(vm.state, vm.selectedPoint, vm.selectedShapes) {
        Geometry2DInteractionEngine.inspect(vm.state, vm.selectedPoint, vm.selectedShapes)
    }
    val constraintSuggestions = remember(vm.state, vm.selectedPoint, vm.selectedShapes) {
        Geometry2DInteractionEngine.constraintSuggestions(vm.state, vm.selectedPoint, vm.selectedShapes)
    }
    val constraintFeedback = remember(vm.state) { Geometry2DInteractionEngine.evaluateConstraints(vm.state) }
    val constraintGlyphs = remember(vm.state) { Geometry2DDirectManipulation.constraintGlyphs(vm.state) }
    val selectionHandles = remember(vm.state, vm.selectedShape) { vm.selectedShape.takeIf { it in vm.state.shapes.indices }?.let { Geometry2DDirectManipulation.handles(vm.state, it) }.orEmpty() }
    val replayingProtocol = protocolStep.roundToInt() < protocolSize
    val replayShapes = remember(vm.state.shapes, protocolTimeline.visibleIds) {
        vm.state.shapes.map { shape -> shape.copy(visible = shape.visible && shape.id in protocolTimeline.visibleIds) }
    }
    Box(Modifier.fillMaxSize()) {
        CoordinateCanvas(
            modifier = Modifier.fillMaxSize().semantics {
                contentDescription = "Interactive coordinate geometry canvas. ${contextInspector.title}. Parents ${contextInspector.parentIds.joinToString().ifBlank { "none" }}. Dependents ${contextInspector.dependentIds.joinToString().ifBlank { "none" }}. Use Tab to change objects and arrow keys to move the selection."
            },
            shapes = replayShapes,
            interactionEnabled = vm.geometryTool == GeometryTool.Select && !replayingProtocol,
            selectedShapes = vm.selectedShapes,
            snapEnabled = vm.settings.snap,
            axisConstraint = axisConstraint,
            precisionMode = precisionMode,
            lassoEnabled = lassoEnabled,
            boxSelectEnabled = boxSelectEnabled,
            homeRequest = homeRequest,
            undoViewRequest = undoViewRequest,
            onPointDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginPointDrag(it)
            },
            onPointDrag = vm::previewPointDrag,
            onShapeDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginShapeDrag(it)
            },
            onShapeDrag = vm::previewShapeDrag,
            onShapeRotate = vm::previewShapeRotation,
            onDragEnd = vm::endPointDrag,
            onDragCancel = vm::cancelPointDrag,
            onDropDelete = vm::deleteSelectedShape,
            onCanvasTap = { point, hitPointIndex ->
                if (hitPointIndex == null) vm.dismissAllMenusAndPanels()
                vm.handleGeometryTap(point, hitPointIndex)
            },
            onClearSelection = vm::clearGeometrySelection,
            onLassoSelection = vm::selectShapes,
            onObjectLongPress = { shapeIndex, _, _ ->
                shapeIndex?.let(vm::selectShape)
                contextMenuShapeIndex = shapeIndex
            },
            onKeyboardMove = vm::moveGeometrySelectionFromKeyboard,
            onKeyboardCycle = vm::cycleGeometrySelection,
            points = vm.state.points,
        ) { tx ->
            val pa = tx(a)
            val pb = tx(b)
            val pm = tx(m.midpoint)
            if (!vm.shapeExplorerScene && "P1" in protocolTimeline.visibleIds && "P2" in protocolTimeline.visibleIds) drawLine(Violet, pa, pb, 5f, cap = StrokeCap.Round)
            drawStoredShapes(vm.state.points, replayShapes, vm.selectedShapes, vm.selectedShape, tx)
            constraintGlyphs.forEach { glyph ->
                val color = when (glyph.feedback.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; ConstraintFeedbackLevel.Violated -> Amber; ConstraintFeedbackLevel.Invalid -> Color.Red }
                drawGraphLabel(glyph.symbol, tx(glyph.position) + Offset(8f, -8f), color)
            }
            selectionHandles.forEach { handle ->
                val center = tx(handle.position)
                drawCircle(if (handle.kind.name in setOf("Rotate", "Reflect")) Violet else Amber, 7f, center)
                drawCircle(Color.White.copy(.8f), 13f, center, style = Stroke(1.5f))
            }
            geometryTrace?.samples?.takeIf { it.size >= 2 }?.let { samples ->
                val path = Path().apply { val first = tx(samples.first()); moveTo(first.x, first.y); samples.drop(1).forEach { val p = tx(it); lineTo(p.x, p.y) } }
                drawPath(path, Green.copy(.8f), style = Stroke(2.5f, cap = StrokeCap.Round))
            }
            if (!vm.shapeExplorerScene) drawConstructionPreview(vm.pendingConstruction, vm.geometryTool, tx)
            vm.state.points.drop(2).forEachIndexed { index, point ->
                val pointIndex = index + 2
                if ("P${pointIndex + 1}" !in protocolTimeline.visibleIds) return@forEachIndexed
                val dependency = dependenciesByOutput[pointIndex]
                val invalid = pointIndex in invalidDependencyOutputs
                drawRadiantPoint(
                    tx(point),
                    if (invalid) Color.Red else if (dependency == null) Green else Amber,
                    if (invalid) "${dependency?.name} undefined" else dependency?.name ?: "P${pointIndex + 1}",
                )
            }
            if (!vm.shapeExplorerScene) {
                drawLine(Cyan, tx(Vec2(a.x, a.y)), tx(Vec2(b.x, a.y)), 2f, pathEffect = null)
                drawLine(Cyan.copy(alpha = .8f), tx(Vec2(b.x, a.y)), pb, 2f)
            }
            if ("P1" in protocolTimeline.visibleIds) drawRadiantPoint(pa, Cyan, "A (${trim(a.x)}, ${trim(a.y)})")
            if ("P2" in protocolTimeline.visibleIds) drawRadiantPoint(pb, Violet, "B (${trim(b.x)}, ${trim(b.y)})")
            if (!vm.shapeExplorerScene) {
                drawRadiantPoint(pm, Violet, "M (${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})")
                drawCircle(Cyan.copy(alpha = .8f), radius = a.distanceTo(third).toFloat() * 42f, center = tx(Vec2(1.5, 1.0)), style = Stroke(2f))
            }
        }
        if (vm.shapeExplorerScene && selectedShape != null) {
            val details = shapeExplorer2DDetails(selectedShape, vm.state.points)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (compact) 70.dp else 78.dp)
                    .fillMaxWidth(if (compact) .94f else .55f)
                    .widthIn(max = 520.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))))
                    .border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedShape.name, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlowButton("All formulas", icon = "ƒ", iconOnly = compact) { vm.togglePanel(PanelSlot.Right) }
                        GlowButton("Shapes", icon = "SE", iconOnly = compact, onClick = vm::openShapesExplorer)
                    }
                }
                Text(details.formula, color = Ink, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text("Area ${trim(details.area)}  ·  Perimeter ${trim(details.perimeter)}", color = Green, fontSize = 12.sp, maxLines = 1)
                Text("Drag a glowing point to resize.", color = Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        if (!vm.shapeExplorerScene) InteractionHint(
            "Drag a junction to resize · drag shape to move · empty canvas pans · empty two-finger pinch zooms",
            Modifier.align(Alignment.BottomEnd),
        )
        DeleteDropTarget(
            enabled = vm.selectedShape in vm.state.shapes.indices,
            onDelete = vm::deleteSelectedShape,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp),
        )
        if (!vm.shapeExplorerScene && selectedShape != null) SmartSelectionHud(
            title = selectedShape.name,
            instruction = if (selectedShape.locked) "Locked · unlock to drag or resize" else "Drag body to move · junctions resize · top handle rotates",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
        ) {
            GlowButton(if (selectedShape.locked) "Unlock" else "Lock") { vm.updateSelectedShape { it.copy(locked = !it.locked) } }
            GlowButton("Delete", icon = "×", onClick = vm::deleteSelectedShape)
        }
        if (!vm.shapeExplorerScene) Row(
            Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GlowButton("⌂ Fit") { homeRequest++ }
            GlowButton("Undo view") { undoViewRequest++ }
            GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled; if (lassoEnabled) boxSelectEnabled = false }
            GlowButton(if (boxSelectEnabled) "● Box" else "Box") { boxSelectEnabled = !boxSelectEnabled; if (boxSelectEnabled) lassoEnabled = false }
            GlowButton("Copy", enabled = vm.selectedShapes.isNotEmpty()) { copyShapeText(context, "AI Explorer geometry", vm.exportSelectedGeometry()) }
            GlowButton("Paste") {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let(vm::importGeometry)
            }
            GlowButton("Group", enabled = vm.selectedShapes.size > 1, onClick = vm::groupSelectedShapes)
            GlowButton("Ungroup", enabled = vm.state.geometryGroups.any { group -> group.shapeIds.any { id -> vm.state.shapes.indexOfFirst { it.id == id } in vm.selectedShapes } }, onClick = vm::ungroupSelectedShapes)
        }
        contextMenuShapeIndex?.let { shapeIndex ->
            vm.state.shapes.getOrNull(shapeIndex)?.let { shape ->
                Column(
                    Modifier.align(Alignment.Center).widthIn(max = 360.dp).clip(RoundedCornerShape(16.dp))
                        .background(SurfaceA.copy(.98f)).border(1.dp, Amber.copy(.65f), RoundedCornerShape(16.dp)).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(shape.name, color = Amber, fontWeight = FontWeight.Bold)
                        GlowButton("×") { contextMenuShapeIndex = null }
                    }
                    Text("Object actions at the touched geometry", color = Muted, fontSize = 10.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (shape.locked) "Unlock" else "Lock") { vm.updateSelectedShape { it.copy(locked = !it.locked) }; contextMenuShapeIndex = null }
                        GlowButton(if (shape.visible) "Hide" else "Show") { vm.updateSelectedShape { it.copy(visible = !it.visible) }; contextMenuShapeIndex = null }
                        GlowButton("Duplicate") { vm.duplicateSelectedShape(); contextMenuShapeIndex = null }
                        GlowButton("Front") { vm.reorderSelectedShape(true); contextMenuShapeIndex = null }
                        GlowButton("Back") { vm.reorderSelectedShape(false); contextMenuShapeIndex = null }
                        GlowButton("Style") { vm.updateSelectedShape { it.copy(styleKey = if (it.styleKey == "default") "accent" else "default") } }
                        GlowButton("Copy") { copyShapeText(context, "AI Explorer geometry", vm.exportSelectedGeometry()) }
                        GlowButton("Rename…") { vm.togglePanel(PanelSlot.Right); contextMenuShapeIndex = null }
                        GlowButton("Delete") { vm.deleteSelectedShape(); contextMenuShapeIndex = null }
                    }
                }
            }
        }
        val quickContextTools = contextInspector.tools.filter { it.enabled && (it.category.contains("Dependent") || it.category.contains("centre") || it.toolName in setOf("Intersection", "Midpoint", "CircleThreePoints")) }.take(6)
        if (!vm.shapeExplorerScene && quickContextTools.isNotEmpty() && !vm.showLeftPanel && !vm.showRightPanel) {
            Column(
                Modifier.align(Alignment.TopCenter).padding(top = 72.dp).widthIn(max = 620.dp)
                    .clip(RoundedCornerShape(15.dp)).background(SurfaceA.copy(.94f)).border(1.dp, Amber.copy(.55f), RoundedCornerShape(15.dp)).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text("CONSTRUCT FROM ${contextInspector.title.uppercase()}", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    quickContextTools.forEach { tool -> GlowButton(tool.label) { vm.applyContextualGeometryTool(tool.toolName, tool.pointIndices) } }
                    GlowButton("More…") { vm.togglePanel(PanelSlot.Right) }
                }
            }
        }
        if (!vm.shapeExplorerScene && constraintFeedback.isNotEmpty() && !vm.showRightPanel) {
            val satisfied = constraintFeedback.count { it.level == ConstraintFeedbackLevel.Satisfied }
            val warning = constraintFeedback.any { it.level == ConstraintFeedbackLevel.Violated || it.level == ConstraintFeedbackLevel.Invalid }
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp).width(210.dp)
                    .clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.94f)).border(1.dp, (if (warning) Amber else Green).copy(.55f), RoundedCornerShape(14.dp)).padding(8.dp),
            ) {
                Text("CONSTRAINTS  $satisfied/${constraintFeedback.size}", color = if (warning) Amber else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(constraintFeedback.firstOrNull { it.level != ConstraintFeedbackLevel.Satisfied }?.statement ?: "All monitored relations are satisfied", color = Ink, fontSize = 10.sp, maxLines = 2)
            }
        }
        if (replayingProtocol) {
            Text(
                "Protocol replay · ${protocolStep.roundToInt()}/$protocolSize · editing paused",
                color = Amber,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceA.copy(.94f)).padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (!vm.shapeExplorerScene) FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Tools",
            rightLabel = "Measure",
            bottomLabel = "Construct",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).padding(top = 64.dp).width(270.dp)) {
            PanelHeader("Dynamic Geometry", vm::hidePanels, Cyan)
            Text("Tap flow: ${vm.geometryTool.name} needs ${vm.geometryTool.requiredTapCount()} tap(s)", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GeometryTool.entries.filterNot { it == GeometryTool.Measure }.forEach {
                    GlowButton(if (vm.geometryTool == it) "• ${it.name}" else it.name, onClick = { vm.selectGeometryTool(it) })
                }
            }
            Text("Objects", color = Ink, fontWeight = FontWeight.SemiBold)
            vm.state.shapes.forEachIndexed { index, shape ->
                Text(
                    text = "${if (index in vm.selectedShapes) "• " else ""}${shape.name}${if (!shape.visible) " (hidden)" else ""}${if (shape.locked) " 🔒" else ""}",
                    color = if (index in vm.selectedShapes) Amber else Muted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { vm.selectShape(index) }
                        .padding(7.dp),
                )
            }
        }
        if (vm.showRightPanel && vm.shapeExplorerScene && selectedShape != null) Shape2DStudioPanel(vm, selectedShape, compact, Modifier.align(if (compact) Alignment.Center else Alignment.TopEnd))
        if (vm.showRightPanel && !vm.shapeExplorerScene) GlassPanel(Modifier.align(Alignment.TopEnd).padding(top = 64.dp).width(260.dp)) {
            PanelHeader("Context Inspector", vm::hidePanels, Violet)
            Text(contextInspector.title, color = Violet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(contextInspector.kind, color = Muted, fontSize = 11.sp)
            contextInspector.properties.forEach { (name, value) -> Insight(name, value, when (name) { "State" -> Green; "Coordinates" -> Cyan; else -> Violet }) }
            Insight("Parents", contextInspector.parentIds.joinToString().ifBlank { "none — directly defined" }, Cyan)
            Insight("Drives", contextInspector.dependentIds.joinToString().ifBlank { "no dependent objects" }, Amber)
            Text("Available from this selection", color = Ink, fontWeight = FontWeight.SemiBold)
            contextInspector.tools.groupBy { it.category }.forEach { (category, tools) ->
                Text(category, color = Muted, fontSize = 9.sp)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    tools.forEach { tool -> GlowButton(tool.label, enabled = tool.enabled) { vm.applyContextualGeometryTool(tool.toolName, tool.pointIndices) } }
                }
            }
            if (constraintSuggestions.isNotEmpty()) {
                Text("Suggested constraints", color = Ink, fontWeight = FontWeight.SemiBold)
                constraintSuggestions.forEach { suggestion ->
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0x33101824)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(suggestion.label, color = when (suggestion.preview.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; else -> Amber }, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            GlowButton("Monitor") { vm.applyGeometryConstraint(suggestion.constraint) }
                        }
                        Text(suggestion.preview.statement, color = Muted, fontSize = 9.sp)
                    }
                }
            }
            if (constraintFeedback.isNotEmpty()) {
                Text("Live constraint feedback", color = Ink, fontWeight = FontWeight.SemiBold)
                constraintFeedback.forEach { feedback ->
                    val color = when (feedback.level) { ConstraintFeedbackLevel.Satisfied -> Green; ConstraintFeedbackLevel.NearlySatisfied -> Cyan; ConstraintFeedbackLevel.Violated -> Amber; ConstraintFeedbackLevel.Invalid -> Color.Red }
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.dp, color.copy(.45f), RoundedCornerShape(11.dp)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${feedback.constraint.type.label} · ${feedback.level.name}", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            GlowButton("×") { vm.removeGeometryConstraint(feedback.constraint.id) }
                        }
                        Text(feedback.statement, color = Ink, fontSize = 9.sp)
                        Text(feedback.guidance, color = Muted, fontSize = 9.sp)
                    }
                }
            }
            Insight("Undefined dependencies", "${invalidDependencyOutputs.size}", if (invalidDependencyOutputs.isEmpty()) Green else Color.Red)
            Insight("Midpoint", "(${trim(m.midpoint.x)}, ${trim(m.midpoint.y)})", Cyan)
            Insight("Slope", m.slope?.let(::trim) ?: "undefined", Violet)
            Insight("Distance AB", "${m.exactDistance} ≈ ${trim(m.distance)}", Cyan)
            val exactDistance = dynamicEngine.distance(dynamicDocument, "A", "B")
            Insight("Exact distance", "${exactDistance.exact} · ${trim(exactDistance.decimal)}", Cyan)
            Insight("Exact triangle area", dynamicEngine.polygonArea(dynamicDocument, listOf("A", "B", "C")).exact, Violet)
            Insight("Angle BAC", dynamicEngine.angle(dynamicDocument, "B", "A", "C").exact, Amber)
            dynamicEngine.ellipseParameters(dynamicDocument, "ellipse").forEach { (name, value) -> Insight("Ellipse $name", value.exact, Green) }
            analyticLine?.let { line ->
                Insight("Line AB", "${trim(-line.direction.y)}x + ${trim(line.direction.x)}y = ${trim(-line.direction.y * line.point.x + line.direction.x * line.point.y)}", Green)
            }
            Insight("Triangle area", "${trim(Geometry2D.polygonArea(listOf(a, b, third)))} u²", Violet)
            selectedShape?.let { shape ->
                OutlinedTextField(
                    value = shape.name,
                    onValueChange = { name -> vm.updateSelectedShape { it.copy(name = name.take(32)) } },
                    label = { Text("Object name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton(if (shape.visible) "Hide" else "Show", onClick = { vm.updateSelectedShape { it.copy(visible = !it.visible) } })
                    GlowButton(if (shape.locked) "Unlock" else "Lock", onClick = { vm.updateSelectedShape { it.copy(locked = !it.locked) } })
                    GlowButton("Style", onClick = {
                        vm.updateSelectedShape {
                            val next = when (it.styleKey) { "default" -> "cyan"; "cyan" -> "violet"; "violet" -> "green"; else -> "default" }
                            it.copy(styleKey = next)
                        }
                    })
                    GlowButton("Delete", onClick = vm::deleteSelectedShape)
                    GlowButton("Translate", onClick = { vm.transformSelectedShape(PointDependencyType.Translate, listOf(1.0, 1.0)) })
                    GlowButton("Rotate 30°", onClick = { vm.transformSelectedShape(PointDependencyType.Rotate, listOf(30.0, 0.0, 0.0)) })
                    GlowButton("Reflect X", onClick = { vm.transformSelectedShape(PointDependencyType.ReflectX) })
                    GlowButton("Dilate", onClick = { vm.transformSelectedShape(PointDependencyType.Dilate, listOf(1.25, 0.0, 0.0)) })
                }
            }
            vm.state.points.getOrNull(vm.selectedPoint)?.let { point ->
                DirectPointEditor(vm.selectedPoint, point) { updated -> vm.movePoint(vm.selectedPoint, updated) }
                val dependency = vm.state.pointDependencies.firstOrNull { it.outputIndex == vm.selectedPoint }
                if (dependency != null && resolvePointDependency(vm.state.points, dependency.inputIndices, dependency.type, dependency.parameters) == null) {
                    Text("Construction needs repair", color = Color.Red, fontWeight = FontWeight.Bold)
                    Geometry2DDirectManipulation.recoverDegenerate(vm.state, dependency.inputIndices).forEach { action ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x332A1018)).padding(7.dp)) {
                            Text(action.label, color = Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(action.explanation, color = Muted, fontSize = 9.sp)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlowButton("Nudge parent", onClick = vm::nudgeSelectedDependencyParent)
                        GlowButton("Make free", onClick = vm::freeSelectedDependentPoint)
                    }
                }
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Geometry Controls", vm::hidePanels, Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    GeometryTool.Point,
                    GeometryTool.Line,
                    GeometryTool.Segment,
                    GeometryTool.Ray,
                    GeometryTool.Triangle,
                    GeometryTool.Polygon,
                    GeometryTool.Rectangle,
                    GeometryTool.Square,
                    GeometryTool.Circle,
                    GeometryTool.Arc,
                    GeometryTool.Measure,
                ).forEach {
                    GlowButton(if (vm.geometryTool == it) "• ${it.name}" else it.name, onClick = { vm.selectGeometryTool(it) })
                }
                GlowButton("Reset", onClick = { vm.reset() })
            }
            Text("Movement constraint", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
                GlowButton(if (lassoEnabled) "● Lasso" else "Lasso") { lassoEnabled = !lassoEnabled; if (lassoEnabled) boxSelectEnabled = false }
                GlowButton(if (boxSelectEnabled) "● Box" else "Box") { boxSelectEnabled = !boxSelectEnabled; if (boxSelectEnabled) lassoEnabled = false }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Construction protocol timeline", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Replay the actual workspace and inspect dependency chains.", color = Muted, fontSize = 10.sp)
                }
                Text("${protocolStep.roundToInt()}/$protocolSize", color = Cyan, fontWeight = FontWeight.Bold)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("|←") { protocolStep = 0f; focusedProtocolId = null }
                GlowButton("← Step", enabled = protocolStep > 0f) { protocolStep = (protocolStep - 1f).coerceAtLeast(0f) }
                GlowButton(if (protocolPlaying) "Pause" else "Play") { protocolPlaying = !protocolPlaying }
                GlowButton("Step →", enabled = protocolStep < protocolSize) { protocolStep = (protocolStep + 1f).coerceAtMost(protocolSize.toFloat()) }
                GlowButton("Live") { protocolStep = protocolSize.toFloat(); focusedProtocolId = null }
            }
            Text("Macros and traces", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton(if (macroDraft.recording) "Pause macro" else "Record macro") { macroDraft = macroDraft.copy(recording = !macroDraft.recording) }
                GlowButton("Replay macro", enabled = macroDraft.recordedIds.isNotEmpty()) {
                    val indices = macroDraft.recordedIds.mapNotNull { id -> vm.state.shapes.indexOfFirst { it.id == id }.takeIf { it >= 0 } }.toSet()
                    vm.selectShapes(indices)
                }
                GlowButton("Clear macro") { macroDraft = com.indianservers.aiexplorer.workspace.GeometryMacroDraft("Canvas macro") }
                GlowButton(if (geometryTrace?.recording == true) "Stop trace" else "Trace selected point", enabled = vm.selectedPoint in vm.state.points.indices || geometryTrace?.recording == true) {
                    geometryTrace = if (geometryTrace?.recording == true) geometryTrace?.copy(recording = false) else com.indianservers.aiexplorer.workspace.GeometryTraceSession("P${vm.selectedPoint + 1}", recording = true)
                }
                Text("${macroDraft.recordedIds.size} macro objects · ${geometryTrace?.samples?.size ?: 0} trace samples", color = Muted, fontSize = 9.sp)
            }
            AxisSlider("Timeline position", protocolStep, 0f..protocolSize.toFloat().coerceAtLeast(1f)) { protocolStep = it.roundToInt().toFloat() }
            protocolTimeline.entries.forEachIndexed { index, entry ->
                val inDependencyChain = entry.id in protocolTimeline.dependencyChain
                val accent = when (entry.status) {
                    GeometryProtocolStatus.Complete -> Green
                    GeometryProtocolStatus.Current -> Cyan
                    GeometryProtocolStatus.Future -> Muted
                    GeometryProtocolStatus.Blocked -> Color.Red
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                        .background(if (entry.id == focusedProtocolId) Violet.copy(.18f) else if (inDependencyChain) Cyan.copy(.10f) else Color(0x22101824))
                        .border(1.dp, (if (entry.id == focusedProtocolId) Violet else accent).copy(.42f), RoundedCornerShape(11.dp))
                        .clickable { focusedProtocolId = entry.id; protocolStep = (index + 1).toFloat() }.padding(7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${index + 1}. ${entry.title}", color = if (inDependencyChain) Cyan else accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(entry.status.name, color = accent, fontSize = 8.sp)
                    }
                    Text(entry.detail, color = Ink, fontSize = 9.sp)
                    Text("Depends on: ${entry.parentIds.joinToString().ifBlank { "none" }}", color = Muted, fontSize = 9.sp)
                    protocolTimeline.blockedBy[entry.id]?.let { missing -> Text("Blocked by missing: ${missing.joinToString()}", color = Color.Red, fontSize = 9.sp) }
                }
            }
        }
    }
}

@Composable
private fun DirectPointEditor(pointIndex: Int, point: Vec2, onApply: (Vec2) -> Unit) {
    var xText by remember(pointIndex, point.x) { mutableStateOf(trim(point.x)) }
    var yText by remember(pointIndex, point.y) { mutableStateOf(trim(point.y)) }
    Text("Direct coordinate editing", color = Ink, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(xText, { xText = it }, Modifier.weight(1f), label = { Text("x") }, singleLine = true)
        OutlinedTextField(yText, { yText = it }, Modifier.weight(1f), label = { Text("y") }, singleLine = true)
        GlowButton("Apply", enabled = xText.toDoubleOrNull() != null && yText.toDoubleOrNull() != null) {
            onApply(Vec2(xText.toDouble(), yText.toDouble()))
        }
    }
}

@Composable
private fun DirectVec3Editor(title: String, value: Vec3, onApply: (Vec3) -> Unit) {
    var x by remember(title, value.x) { mutableStateOf(trim(value.x)) }
    var y by remember(title, value.y) { mutableStateOf(trim(value.y)) }
    var z by remember(title, value.z) { mutableStateOf(trim(value.z)) }
    Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(x, { x = it }, Modifier.weight(1f), label = { Text("x") }, singleLine = true)
        OutlinedTextField(y, { y = it }, Modifier.weight(1f), label = { Text("y") }, singleLine = true)
        OutlinedTextField(z, { z = it }, Modifier.weight(1f), label = { Text("z") }, singleLine = true)
        GlowButton("Apply", enabled = listOf(x, y, z).all { it.toDoubleOrNull() != null }) { onApply(Vec3(x.toDouble(), y.toDouble(), z.toDouble())) }
    }
}

@Composable
private fun ManipulativesScreen(vm: ExplorerViewModel, wide: Boolean) {
    val engine = remember { ManipulativeEngine() }
    val proofEngine = remember { VisualProofEngine() }
    var scene by remember {
        mutableStateOf(
            ManipulativeScene(
                items = listOf(
                    ManipulativeItem("item-1", ManipulativeKind.AlgebraX, Vec2(1.0, 1.0), width = 2.4, height = .8, label = "+x"),
                    ManipulativeItem("item-2", ManipulativeKind.AlgebraUnit, Vec2(4.0, 1.0), label = "+1"),
                    ManipulativeItem("item-3", ManipulativeKind.FractionBar, Vec2(1.0, 3.0), width = 3.0, height = .55, label = "1/2", numerator = 1, denominator = 2),
                    ManipulativeItem("item-4", ManipulativeKind.BalanceWeight, Vec2(5.0, 3.0), label = "3", value = 3.0, side = "left"),
                ),
            ),
        )
    }
    var tray by remember { mutableStateOf(com.indianservers.aiexplorer.core.ManipulativeTray.Algebra) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var formalPreview by remember { mutableStateOf("Move tiles to generate formal maths") }
    var playback by remember { mutableStateOf(proofEngine.start(VisualProofCatalog.labs.first().id)) }
    LaunchedEffect(playback.playing) {
        while (playback.playing) {
            delay(850)
            playback = proofEngine.next(playback)
        }
    }
    val selectedItem = scene.items.firstOrNull { it.id in selected }
    val trayKinds = when (tray) {
        com.indianservers.aiexplorer.core.ManipulativeTray.Algebra -> listOf(ManipulativeKind.AlgebraX, ManipulativeKind.AlgebraUnit)
        com.indianservers.aiexplorer.core.ManipulativeTray.Fractions -> listOf(ManipulativeKind.FractionBar)
        com.indianservers.aiexplorer.core.ManipulativeTray.Numbers -> listOf(ManipulativeKind.IntegerChip, ManipulativeKind.NumberLinePoint, ManipulativeKind.NumberLineInterval)
        com.indianservers.aiexplorer.core.ManipulativeTray.Balance -> listOf(ManipulativeKind.BalanceWeight)
        com.indianservers.aiexplorer.core.ManipulativeTray.Geometry -> listOf(ManipulativeKind.PatternBlock, ManipulativeKind.GeometricTile)
        com.indianservers.aiexplorer.core.ManipulativeTray.Measure -> listOf(ManipulativeKind.Ruler, ManipulativeKind.Protractor, ManipulativeKind.AngleTool)
    }

    @Composable fun ToolPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            PanelHeader("Manipulatives", vm::returnToMathMenu, Cyan, icon = "▦")
            Text("Choose a tray, then add tactile objects.", color = Muted, fontSize = 12.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                com.indianservers.aiexplorer.core.ManipulativeTray.entries.forEach { option -> GlowButton(if (tray == option) "• ${option.name}" else option.name) { tray = option } }
            }
            Text("Tray", color = Ink, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trayKinds.forEach { kind -> GlowButton("+ ${kind.name}") { scene = engine.create(scene, kind, Vec2(1.0 + scene.items.size % 5, 1.0 + scene.items.size % 7)); selected = setOf(scene.items.last().id) } }
            }
            Text("Selection", color = Ink, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Clone", enabled = selectedItem != null) { selectedItem?.let { scene = engine.duplicate(scene, it.id) } }
                GlowButton("Group", enabled = selected.size >= 2) { scene = engine.group(scene, selected) }
                GlowButton("Ungroup", enabled = selectedItem?.groupId != null) { selectedItem?.groupId?.let { scene = engine.ungroup(scene, it) } }
                GlowButton("Rotate 15°", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, 15.0) } }
                GlowButton("Scale +", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, scaleFactor = 1.15) } }
                GlowButton("Scale -", enabled = selectedItem != null) { selectedItem?.let { scene = engine.transform(scene, it.id, scaleFactor = .87) } }
                GlowButton(if (selectedItem?.locked == true) "Unlock" else "Lock", enabled = selectedItem != null) { selectedItem?.let { scene = engine.setLocked(scene, it.id, !it.locked) } }
                GlowButton("Delete", enabled = selectedItem != null) { selectedItem?.let { scene = engine.remove(scene, it.id); selected = emptySet() } }
            }
            selectedItem?.let { item ->
                OutlinedTextField(value = item.label, onValueChange = { scene = engine.annotate(scene, item.id, it, item.annotation) }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = item.annotation, onValueChange = { scene = engine.annotate(scene, item.id, item.label, it) }, label = { Text("Annotation") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Value −") { scene = engine.configure(scene, item.id, value = item.value - 1) }
                    GlowButton("Value +") { scene = engine.configure(scene, item.id, value = item.value + 1) }
                }
            }
            Text("Formal maths links", color = Ink, fontWeight = FontWeight.Bold)
            engine.links(scene).forEach { link ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(link.content, color = Cyan, fontFamily = FontFamily.Monospace); Text(link.explanation, color = Muted, fontSize = 10.sp) }
                    GlowButton(link.destination.name) {
                        formalPreview = link.content
                        when (link.destination) {
                            FormalMathDestination.Graph -> { vm.addFunction(link.content); vm.open(MathModule.Graph2D) }
                            FormalMathDestination.Notebook -> { vm.submitNotebook(link.content); vm.openMathNotebook() }
                            FormalMathDestination.Equation -> Unit
                        }
                    }
                }
            }
            Text(formalPreview, color = Green, fontSize = 12.sp)
            Insight("Scene JSON", "${engine.serialize(scene).length} characters", Amber)
        }
    }

    @Composable fun Board(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Snap Board", color = Violet, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Drag objects · tap multiple objects to group · locked objects stay fixed", color = Muted, fontSize = 11.sp)
            Box(
                Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(18.dp)).background(Color(0x6600060D))
                    .semantics { contentDescription = "Interactive manipulative snap board" },
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val grid = 22.5f
                    var x = 0f; while (x < size.width) { drawLine(Grid, Offset(x, 0f), Offset(x, size.height), 1f); x += grid }
                    var y = 0f; while (y < size.height) { drawLine(Grid, Offset(0f, y), Offset(size.width, y), 1f); y += grid }
                    drawLine(Amber.copy(.5f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), 2f)
                }
                scene.items.forEach { item ->
                    val accent = when (item.kind) {
                        ManipulativeKind.AlgebraX, ManipulativeKind.AlgebraUnit -> Cyan
                        ManipulativeKind.FractionBar -> Violet
                        ManipulativeKind.IntegerChip, ManipulativeKind.NumberLinePoint, ManipulativeKind.NumberLineInterval -> Green
                        ManipulativeKind.BalanceWeight -> Amber
                        ManipulativeKind.PatternBlock, ManipulativeKind.GeometricTile -> Color(0xFFFF7AA8)
                        else -> Ink
                    }
                    Column(
                        Modifier.offset { IntOffset((item.position.x * 42).roundToInt(), (item.position.y * 42).roundToInt()) }
                            .width((72 * item.width * item.scale).dp.coerceIn(44.dp, 220.dp)).height((46 * item.height * item.scale).dp.coerceIn(38.dp, 150.dp))
                            .clip(RoundedCornerShape(10.dp)).background(accent.copy(if (item.locked) .2f else .36f))
                            .border(if (item.id in selected) 3.dp else 1.dp, if (item.id in selected) Amber else accent, RoundedCornerShape(10.dp))
                            .pointerInput(item.id, item.position, item.locked) {
                                detectDragGestures { _, delta -> if (!item.locked) scene = engine.move(scene, item.id, item.position + Vec2(delta.x / 42.0, delta.y / 42.0)) }
                            }
                            .clickable { selected = if (item.id in selected) selected - item.id else selected + item.id }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(item.label, color = accent, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        if (item.annotation.isNotBlank()) Text(item.annotation, color = Muted, fontSize = 8.sp, maxLines = 2)
                        if (item.locked) Text("LOCK", color = Amber, fontSize = 8.sp)
                    }
                }
            }
        }
    }

    @Composable fun ProofPanel(modifier: Modifier = Modifier) {
        GlassPanel(modifier) {
            Text("Interactive Visual Proofs", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                VisualProofCatalog.labs.forEach { lab -> GlowButton(if (playback.frame.lab.id == lab.id) "• ${lab.title}" else lab.title) { playback = proofEngine.start(lab.id) } }
            }
            Text(playback.frame.lab.steps[playback.frame.step], color = Ink, fontWeight = FontWeight.Bold)
            Text("Step ${playback.frame.step + 1}/${playback.frame.lab.steps.size}", color = Violet)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton(if (playback.playing) "Pause" else "Play") { playback = proofEngine.togglePlaying(playback) }
                GlowButton("Next") { playback = proofEngine.next(playback) }
                GlowButton("Reveal") { playback = proofEngine.reveal(playback) }
            }
            playback.frame.lab.parameters.forEach { parameter ->
                AxisSlider(parameter.name, playback.frame.parameters.getValue(parameter.name).toFloat(), parameter.minimum.toFloat()..parameter.maximum.toFloat()) {
                    playback = proofEngine.setParameter(playback, parameter.name, it.toDouble())
                }
            }
            playback.frame.measurements.forEach { (name, value) -> Insight(name, trim(value), Violet) }
            Insight("Invariant", "${playback.frame.invariant} · residual ${trim(playback.frame.residual)}", if (playback.frame.holds) Green else Amber)
            Text("What changes? ${playback.frame.lab.changesPrompt}", color = Cyan, fontSize = 12.sp)
            Text("What stays same? ${playback.frame.lab.invariantPrompt}", color = Green, fontSize = 12.sp)
            MathFormulaText(playback.frame.lab.formalResult, color = Amber, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (wide) Row(Modifier.fillMaxSize().padding(top = 78.dp, bottom = 76.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ToolPanel(Modifier.weight(.28f).fillMaxHeight().verticalScroll(rememberScrollState()))
        Board(Modifier.weight(.42f).fillMaxHeight())
        ProofPanel(Modifier.weight(.30f).fillMaxHeight().verticalScroll(rememberScrollState()))
    } else Column(Modifier.fillMaxSize().padding(top = 68.dp, bottom = 66.dp).verticalScroll(rememberScrollState())) {
        ToolPanel(Modifier.fillMaxWidth()); Board(Modifier.fillMaxWidth()); ProofPanel(Modifier.fillMaxWidth())
    }
}

@Composable
private fun Graph2DScreen(vm: ExplorerViewModel) {
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val advancedGraph = remember { AdvancedGraphEngine() }
    val engine = remember { ExpressionEngine() }
    var traceX by remember { mutableFloatStateOf(2f) }
    var graphTool by remember { mutableStateOf(GraphTool.Plot) }
    var parameterA by remember { mutableFloatStateOf(1f) }
    var graphParameterValues by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var playingParameters by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGraphRowId by remember { mutableStateOf<String?>(null) }
    var equationEditorExpanded by remember { mutableStateOf(false) }
    var graphTypingMode by remember { mutableStateOf(false) }
    var graphAddMenuExpanded by remember { mutableStateOf(false) }
    var graphHomeRequest by remember { mutableIntStateOf(0) }
    var graphBackRequest by remember { mutableIntStateOf(0) }
    var graphForwardRequest by remember { mutableIntStateOf(0) }
    var graphViewport by remember { mutableStateOf(GraphViewState()) }
    var graphAxisSettings by remember { mutableStateOf(GraphAxisSettings()) }
    var showAxisSheet by remember { mutableStateOf(false) }
    var showMiniMap by remember { mutableStateOf(false) }
    var comparisonMode by remember { mutableStateOf(false) }
    var graphDomains by remember { mutableStateOf<Map<String, GraphDomainSelection>>(emptyMap()) }
    var graphStyles by remember { mutableStateOf<Map<String, GraphLineStyle>>(emptyMap()) }
    var graphLabelOffsets by remember { mutableStateOf<Map<String, Offset>>(emptyMap()) }
    var graphSnapshots by remember { mutableStateOf<List<GraphSnapshot>>(emptyList()) }
    var graphSnapshotOverlay by remember { mutableStateOf<GraphSnapshot?>(null) }
    var contextMenuPosition by remember { mutableStateOf<Vec2?>(null) }
    var contextMenuFunctionId by remember { mutableStateOf<String?>(null) }
    var parameterHandleEnabled by remember { mutableStateOf(true) }
    var graphTransformKind by remember { mutableStateOf(GraphTransformKind.TranslateX) }
    var graphTransformAmount by remember { mutableFloatStateOf(.5f) }
    var animateGraphTransform by remember { mutableStateOf(false) }
    var dataText by remember { mutableStateOf("-2,4; -1,1; 0,0; 1,1; 2,4") }
    var brushInterval by remember { mutableStateOf<ClosedFloatingPointRange<Double>?>(null) }
    var sketchPoints by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var latestSketchFit by remember { mutableStateOf<GraphFitResult?>(null) }
    var pinnedTracePoints by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var directCurveDelta by remember { mutableStateOf<Vec2?>(null) }
    var splitComparison by remember { mutableStateOf(false) }
    var showResidualPlot by remember { mutableStateOf(false) }
    val objectGraphSnapshot = vm.mathObjectGraphSnapshot(graphParameterValues)
    val graphRowMetadata = vm.state.graphRowMetadata
    val graphSliderMetadata = vm.state.graphSliderMetadata
    LaunchedEffect(playingParameters, objectGraphSnapshot.parameterRows, graphSliderMetadata) {
        while (playingParameters.isNotEmpty()) {
            delay(90)
            val currentRows = objectGraphSnapshot.parameterRows.associateBy { it.name }
            playingParameters.forEach { name ->
                val parameter = currentRows[name] ?: return@forEach
                val ui = graphSliderMetadata[name] ?: GraphSliderMetadataState()
                val delta = parameter.step * ui.speed.coerceIn(0.25, 8.0) * ui.direction
                val proposed = parameter.value + delta
                val next = when {
                    ui.mode == GraphSliderPlaybackMode.Loop && proposed > parameter.max -> parameter.min
                    ui.mode == GraphSliderPlaybackMode.Loop && proposed < parameter.min -> parameter.max
                    ui.mode == GraphSliderPlaybackMode.Bounce && proposed > parameter.max -> {
                        vm.updateGraphSliderMetadata(name) { ui.copy(direction = -1) }
                        parameter.max
                    }
                    ui.mode == GraphSliderPlaybackMode.Bounce && proposed < parameter.min -> {
                        vm.updateGraphSliderMetadata(name) { ui.copy(direction = 1) }
                        parameter.min
                    }
                    else -> proposed
                }
                graphParameterValues = graphParameterValues + (name to next)
                if (name == "a") parameterA = next.toFloat()
            }
        }
    }
    LaunchedEffect(animateGraphTransform) {
        while (animateGraphTransform) { delay(45); graphTransformAmount += .04f; if (graphTransformAmount > 2f) graphTransformAmount = -2f }
    }
    val liveFunctions = vm.state.functions.map { function ->
        val resolved = objectGraphSnapshot.graphObjects.firstOrNull { it.rowId == function.id }?.resolvedExpression
        function.copy(expression = resolved ?: function.expression.replace(Regex("\\ba\\b"), trim(parameterA.toDouble())))
    }
    val visibleFunctions = liveFunctions.filter { it.visible }
    val explicitFunctions = visibleFunctions.filter { graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
    val selectedFunction = liveFunctions.firstOrNull { it.id == selectedGraphRowId }
    val analysisFunction = selectedFunction?.takeIf { it.visible } ?: visibleFunctions.firstOrNull()
    val primaryExpression = analysisFunction?.takeIf { graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }?.expression
        ?: explicitFunctions.firstOrNull()?.expression
    val roots = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.roots(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val extrema = remember(primaryExpression) {
        primaryExpression?.let { runCatching { graph.extrema(it, -10.0, 10.0) }.getOrDefault(emptyList()) }.orEmpty()
    }
    val adaptiveSample = remember(primaryExpression) {
        primaryExpression?.let { expression ->
            runCatching {
                advancedGraph.adaptiveExplicit(AdvancedGraphDefinition(expression, AdvancedGraphKind.Explicit, GraphDomain(-10.0, 10.0)))
            }.getOrNull()
        }
    }
    val intersections = remember(liveFunctions) {
        if (explicitFunctions.size < 2) emptyList() else runCatching {
            graph.intersections(engine.compile(explicitFunctions[0].expression), engine.compile(explicitFunctions[1].expression), -10.0, 10.0)
        }.getOrDefault(emptyList())
    }
    val dataPoints = remember(dataText) { parseDataPoints(dataText) }
    val dataSummary = remember(dataPoints) { StatisticsEngine.summarize(dataPoints) }
    Box(Modifier.fillMaxSize()) {
        GraphCanvas(
            modifier = Modifier.fillMaxSize(),
            functions = liveFunctions,
            dataPoints = if (graphTool == GraphTool.Data) dataPoints else emptyList(),
            traceX = traceX.toDouble(),
            graphTool = graphTool,
            homeRequest = graphHomeRequest,
            backRequest = graphBackRequest,
            forwardRequest = graphForwardRequest,
            axisSettings = graphAxisSettings,
            domains = graphDomains,
            styles = graphStyles,
            labelOffsets = graphLabelOffsets,
            comparisonMode = comparisonMode,
            showMiniMap = showMiniMap,
            parameterA = parameterA,
            parameterHandleEnabled = parameterHandleEnabled,
            parameterValues = objectGraphSnapshot.parameterRows.associate { it.name to it.value },
            previewExpression = directCurveDelta?.let { delta -> selectedFunction?.let { GraphDirectManipulationEngine.translate(it.expression, delta) } }
                ?: selectedFunction?.let { GraphUxEngine.transform(it.expression, graphTransformKind, graphTransformAmount.toDouble().let { amount -> if (graphTransformKind in setOf(GraphTransformKind.StretchX, GraphTransformKind.StretchY)) kotlin.math.abs(amount).coerceAtLeast(.1) else amount }) },
            brushInterval = brushInterval,
            sketchPoints = sketchPoints,
            pinnedTracePoints = pinnedTracePoints,
            showResidualPlot = showResidualPlot,
            splitComparison = splitComparison,
            snapshotExpressions = graphSnapshotOverlay?.expressions.orEmpty(),
            selectedFunctionId = selectedGraphRowId,
            onSelectFunction = {
                selectedGraphRowId = it
                equationEditorExpanded = true
            },
            onClearSelection = {
                selectedGraphRowId = null
                equationEditorExpanded = false
                vm.dismissAllMenusAndPanels()
            },
            onTraceChange = { traceX = it.toFloat().coerceIn(-1_000f, 1_000f) },
            onParameterAChange = { parameterA = it.toFloat().coerceIn(-20f, 20f) },
            onParameterChange = { name, value ->
                graphParameterValues = graphParameterValues + (name to value.coerceIn(-20.0, 20.0))
                if (name == "a") parameterA = value.toFloat().coerceIn(-20f, 20f)
            },
            onDomainChange = { id, domain -> graphDomains = graphDomains + (id to domain) },
            onLabelMove = { id, delta -> graphLabelOffsets = graphLabelOffsets + (id to ((graphLabelOffsets[id] ?: Offset.Zero) + delta)) },
            onCurveDrag = { id, delta ->
                selectedGraphRowId = id
                directCurveDelta = delta
            },
            onCurveDragEnd = { id, delta ->
                val index = vm.state.functions.indexOfFirst { it.id == id }
                if (index >= 0 && (abs(delta.x) > .01 || abs(delta.y) > .01)) vm.editExpression(index, GraphDirectManipulationEngine.translate(vm.state.functions[index].expression, delta))
                directCurveDelta = null
            },
            onBrushChange = { start, end -> brushInterval = min(start, end)..max(start, end) },
            onSketchChange = { sketchPoints = it },
            onSketchEnd = { points ->
                latestSketchFit = GraphDirectManipulationEngine.fit(points)
                latestSketchFit?.let { fit -> vm.addFunction(fit.expression); selectedGraphRowId = vm.state.functions.lastOrNull()?.id }
                sketchPoints = emptyList()
            },
            onDataPointMove = { index, point ->
                val updated = dataPoints.toMutableList()
                if (index in updated.indices) {
                    updated[index] = point
                    dataText = updated.joinToString("; ") { "${trim(it.x)},${trim(it.y)}" }
                }
            },
            onViewportChange = { graphViewport = it },
            onContextMenu = { id, point -> contextMenuFunctionId = id; contextMenuPosition = point },
        )
        if (!graphTypingMode) InteractionHint(
            "Drag to pan · pinch to zoom · tap a graph to edit",
            Modifier.align(Alignment.BottomEnd),
        )
        if (!graphTypingMode && selectedFunction != null && !vm.hasDismissibleOverlay()) DeleteDropTarget(
            enabled = true,
            onDelete = {
                selectedFunction?.let { function ->
                    val index = vm.state.functions.indexOfFirst { it.id == function.id }
                    if (index >= 0) {
                        vm.deleteFunction(index)
                        selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                        if (vm.state.functions.isEmpty()) equationEditorExpanded = false
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp),
        )
        if (!graphTypingMode && selectedFunction != null) SmartSelectionHud(
            title = selectedFunction.name,
            instruction = if (Regex("\\ba\\b").containsMatchIn(selectedFunction.expression)) {
                "Drag the curve handle to change a · drag endpoint handles to edit its domain"
            } else {
                "Tap/hold the curve for actions · drag its label or domain endpoints directly"
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
        ) {
            GlowButton("Trace") { graphTool = GraphTool.Trace }
            GlowButton("Pin point") {
                runCatching { engine.compile(stripEquation(selectedFunction.expression)).eval(mapOf("x" to traceX.toDouble())) }.getOrNull()?.takeIf(Double::isFinite)?.let { y ->
                    pinnedTracePoints = (pinnedTracePoints + Vec2(traceX.toDouble(), y)).takeLast(8)
                }
            }
            GlowButton("Brush area") { graphTool = GraphTool.BrushArea; brushInterval = null }
            GlowButton("Sketch fit") { graphTool = GraphTool.SketchFit; sketchPoints = emptyList() }
            GlowButton("Domain") {
                graphDomains = graphDomains + (selectedFunction.id to (graphDomains[selectedFunction.id] ?: GraphDomainSelection()))
            }
            GlowButton("Delete", icon = "×") {
                val index = vm.state.functions.indexOfFirst { it.id == selectedFunction.id }
                if (index >= 0) {
                    vm.deleteFunction(index)
                    selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                }
            }
        }
        if (!graphTypingMode) FlowRow(Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            GlowButton("Back") { graphBackRequest++ }; GlowButton("Forward") { graphForwardRequest++ }; GlowButton("Fit") { graphHomeRequest++ }
            GlowButton("Axis") { showAxisSheet = !showAxisSheet }
        }
        GraphEquationEditor(
            Modifier.align(Alignment.TopCenter),
            functions = liveFunctions,
            selectedId = selectedGraphRowId,
            expanded = equationEditorExpanded,
            onExpandedChange = { equationEditorExpanded = it },
            addMenuExpanded = graphAddMenuExpanded,
            onToggleAddMenu = { graphAddMenuExpanded = !graphAddMenuExpanded },
            onAddKind = { kind ->
                vm.addFunction(kind.starter)
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
                equationEditorExpanded = true
                graphAddMenuExpanded = false
                if (kind == GraphAddKind.Table) graphTool = GraphTool.Table
                if (kind == GraphAddKind.Regression) graphTool = GraphTool.Data
            },
            onAdd = {
                vm.addFunction("x")
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
                equationEditorExpanded = true
            },
            onSelect = { selectedGraphRowId = it; equationEditorExpanded = true },
            onExpressionChange = { id, expression ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { vm.editExpression(it, expression) }
            },
            onToggleVisible = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index -> vm.updateFunction(index) { it.copy(visible = !it.visible) } }
            },
            onDuplicate = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { vm.duplicateFunction(it) }
                selectedGraphRowId = vm.state.functions.lastOrNull()?.id
            },
            onDelete = { id ->
                val index = vm.state.functions.indexOfFirst { it.id == id }
                if (index >= 0) {
                    vm.deleteFunction(index)
                    selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                    if (vm.state.functions.isEmpty()) equationEditorExpanded = false
                }
            },
            onColor = { id ->
                vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index ->
                    vm.updateFunction(index) { function ->
                        val next = when (function.colorKey) { "cyan" -> "violet"; "violet" -> "green"; "green" -> "amber"; else -> "cyan" }
                        function.copy(colorKey = next)
                    }
                }
            },
            activeTool = graphTool,
            onTool = { graphTool = it },
            onTypingChange = { typing ->
                graphTypingMode = typing
                if (typing) {
                    graphAddMenuExpanded = false
                    contextMenuPosition = null
                    showAxisSheet = false
                    vm.hidePanels()
                }
            },
        )
        if (showAxisSheet) GlassPanel(Modifier.align(Alignment.Center).widthIn(max = 420.dp)) {
            PanelHeader("Axis Configuration", { showAxisSheet = false }, Cyan)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(graphAxisSettings.xName, { graphAxisSettings = graphAxisSettings.copy(xName = it.take(8)) }, Modifier.weight(1f), label = { Text("X name") })
                OutlinedTextField(graphAxisSettings.yName, { graphAxisSettings = graphAxisSettings.copy(yName = it.take(8)) }, Modifier.weight(1f), label = { Text("Y name") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(graphAxisSettings.xUnit, { graphAxisSettings = graphAxisSettings.copy(xUnit = it.take(6)) }, Modifier.weight(1f), label = { Text("X unit") })
                OutlinedTextField(graphAxisSettings.yUnit, { graphAxisSettings = graphAxisSettings.copy(yUnit = it.take(6)) }, Modifier.weight(1f), label = { Text("Y unit") })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AxisNumberFormat.entries.forEach { format -> TogglePill(format.name, graphAxisSettings.format == format) { graphAxisSettings = graphAxisSettings.copy(format = format) } }
                TogglePill("Grid", graphAxisSettings.gridVisible) { graphAxisSettings = graphAxisSettings.copy(gridVisible = it) }
                TogglePill("Log X", graphAxisSettings.xLogarithmic) { graphAxisSettings = graphAxisSettings.copy(xLogarithmic = it) }
                TogglePill("Log Y", graphAxisSettings.yLogarithmic) { graphAxisSettings = graphAxisSettings.copy(yLogarithmic = it) }
            }
        }
        if (contextMenuPosition != null) GlassPanel(Modifier.align(Alignment.Center).width(245.dp)) {
            PanelHeader("Graph Radial Actions", { contextMenuPosition = null }, Amber)
            Text(contextMenuPosition?.let { "At (${trim(it.x)}, ${trim(it.y)})" }.orEmpty(), color = Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GraphUxEngine.contextActions(contextMenuFunctionId != null).forEach { action -> GlowButton(action, onClick = {
                    when (action) {
                        "Edit" -> { contextMenuFunctionId?.let { selectedGraphRowId = it }; equationEditorExpanded = true }
                        "Trace" -> graphTool = GraphTool.Trace; "Tangent" -> graphTool = GraphTool.Tangent; "Derivative" -> graphTool = GraphTool.Derivative; "Integral" -> graphTool = GraphTool.Integral
                        "Domain" -> contextMenuFunctionId?.let { graphDomains = graphDomains + (it to (graphDomains[it] ?: GraphDomainSelection())) }
                        "Style" -> contextMenuFunctionId?.let { id -> val old = graphStyles[id] ?: GraphLineStyle.Solid; graphStyles = graphStyles + (id to GraphLineStyle.entries[(old.ordinal + 1) % GraphLineStyle.entries.size]) }
                        "Duplicate" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(vm::duplicateFunction) }
                        "Hide" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let { index -> vm.updateFunction(index) { it.copy(visible = false) } } }
                        "Delete" -> contextMenuFunctionId?.let { id -> vm.state.functions.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(vm::deleteFunction) }
                        "Add equation" -> graphAddMenuExpanded = true; "Add point" -> { vm.addFunction("(1,1)"); equationEditorExpanded = true }
                        "Fit view" -> graphHomeRequest++; "Axis settings" -> showAxisSheet = true
                        "Snapshot" -> graphSnapshots = graphSnapshots + GraphSnapshot("View ${graphSnapshots.size + 1}", liveFunctions.map { it.expression }, graphViewport)
                    }; contextMenuPosition = null
                }) }
            }
        }
        if (!graphTypingMode) FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Fx",
            rightLabel = "Analysis",
            bottomLabel = "Trace",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(300.dp)) {
            PanelHeader("Equations & Definitions", vm::hidePanels, Cyan)
            Text("Desmos-style rows · expressions, sliders and generated tables share one object graph.", color = Muted, fontSize = 11.sp)
            objectGraphSnapshot.expressionRows.forEachIndexed { index, row ->
                val rowUi = graphRowMetadata[row.id] ?: GraphRowMetadataState()
                DesmosExpressionRow(
                    row = row,
                    uiState = rowUi,
                    selected = selectedGraphRowId == row.id,
                    graphObject = objectGraphSnapshot.graphObjects.firstOrNull { it.rowId == row.id },
                    onSelect = { selectedGraphRowId = row.id; equationEditorExpanded = true },
                    onExpressionChange = { vm.editExpression(index, it) },
                    onToggleVisible = { vm.updateFunction(index) { it.copy(visible = !it.visible) } },
                    onDuplicate = { vm.duplicateFunction(index); selectedGraphRowId = vm.state.functions.lastOrNull()?.id },
                    onDelete = {
                        vm.deleteFunction(index)
                        selectedGraphRowId = vm.state.functions.getOrNull(index.coerceAtMost(vm.state.functions.lastIndex))?.id
                    },
                    onToggleCollapsed = {
                        vm.updateGraphRowMetadata(row.id) { it.copy(collapsed = !it.collapsed) }
                    },
                    onNoteChange = { note ->
                        vm.updateGraphRowMetadata(row.id) { it.copy(note = note.take(120)) }
                    },
                    onFolderChange = { folder ->
                        vm.updateGraphRowMetadata(row.id) { it.copy(folder = folder.take(32)) }
                    },
                    onColor = {
                        vm.updateFunction(index) { function ->
                            val next = when (function.colorKey) { "cyan" -> "violet"; "violet" -> "green"; "green" -> "amber"; else -> "cyan" }
                            function.copy(colorKey = next)
                        }
                    },
                )
            }
            if (objectGraphSnapshot.parameterRows.isNotEmpty()) {
                Text("Auto sliders", color = Ink, fontWeight = FontWeight.SemiBold)
                objectGraphSnapshot.parameterRows.forEach { parameter ->
                    val parameterUi = graphSliderMetadata[parameter.name] ?: GraphSliderMetadataState()
                    ParameterRowCard(
                        parameter = parameter,
                        playing = parameter.name in playingParameters,
                        uiState = parameterUi,
                        onTogglePlaying = {
                            playingParameters = if (parameter.name in playingParameters) playingParameters - parameter.name else playingParameters + parameter.name
                        },
                        onToggleMode = {
                            val next = if (parameterUi.mode == GraphSliderPlaybackMode.Loop) GraphSliderPlaybackMode.Bounce else GraphSliderPlaybackMode.Loop
                            vm.updateGraphSliderMetadata(parameter.name) { parameterUi.copy(mode = next, direction = 1) }
                        },
                        onSpeedChange = { speed ->
                            vm.updateGraphSliderMetadata(parameter.name) { parameterUi.copy(speed = speed.coerceIn(0.25, 8.0)) }
                        },
                    ) { value ->
                        graphParameterValues = graphParameterValues + (parameter.name to value)
                        if (parameter.name == "a") parameterA = value.toFloat()
                    }
                }
            }
            GeneratedTablePreview(objectGraphSnapshot.generatedTable)
            Text("Use the + button above the graph for every new user equation.", color = Muted, fontSize = 11.sp)
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(270.dp)) {
            PanelHeader("Graph Insights", vm::hidePanels, Violet)
            Insight("Selected row", selectedFunction?.name ?: "Tap a row", Amber)
            Insight("Object graph", "${objectGraphSnapshot.expressionRows.size} rows · ${objectGraphSnapshot.parameterRows.size} sliders", Cyan)
            Insight("Linked table", "${objectGraphSnapshot.generatedTable.size} x-values", Green)
            objectGraphSnapshot.diagnostics.take(1).forEach { Text(it, color = Amber, fontSize = 11.sp) }
            Insight("Tool", graphTool.name, Green)
            Insight("Definitions", "${visibleFunctions.size} visible", Cyan)
            Insight("Kinds", visibleFunctions.map { graph.definitionKind(it.expression).name }.distinct().joinToString(), Violet)
            Insight("Roots", roots.joinToString { trim(it) }.ifBlank { "none detected" }, Cyan)
            Insight("Extrema", extrema.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }.ifBlank { "none detected" }, Green)
            adaptiveSample?.let { sample ->
                Insight("Adaptive sample", "${sample.points.size} points · ${sample.segments.size} segments", Cyan)
                Insight("Arc length", trim(advancedGraph.arcLength(sample)), Violet)
                Insight("Detected breaks", "${sample.discontinuities}", Amber)
            }
            primaryExpression?.let {
                Insight("Derivative", runCatching { trim(graph.derivative(it, traceX.toDouble())) }.getOrDefault("undefined"), Amber)
                Insight("Integral 0→x", runCatching { trim(graph.integral(it, 0.0, traceX.toDouble())) }.getOrDefault("undefined"), Cyan)
            }
            Insight("Intersections", intersections.joinToString { "(${trim(it.x)}, ${trim(it.y)})" }, Violet)
            if (graphTool == GraphTool.Data) {
                Insight("Data", "${dataSummary.count} points", Cyan)
                Insight("Mean", "(${trim(dataSummary.meanX)}, ${trim(dataSummary.meanY)})", Green)
                Insight("σ y", trim(dataSummary.standardDeviationY), Violet)
                dataSummary.regression?.let { Insight("Regression", "y=${trim(it.slope)}x+${trim(it.intercept)} · r=${trim(it.correlation)}", Amber) }
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Graph Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Trace x = ${trim(traceX.toDouble())}", color = Muted, modifier = Modifier.width(120.dp))
                Slider(value = traceX, onValueChange = { traceX = it }, valueRange = -6f..6f, modifier = Modifier.weight(1f))
            }
            if (vm.state.functions.any { Regex("\\ba\\b").containsMatchIn(it.expression) }) {
                AxisSlider("Parameter a", parameterA, -8f..8f) { parameterA = it }
            }
            if (graphTool == GraphTool.Table) {
                Text("Value table", color = Ink, fontWeight = FontWeight.SemiBold)
                val rows = (-4..4).joinToString("\n") { x ->
                    val values = explicitFunctions.joinToString("  ") { fn ->
                        val y = runCatching { engine.compile(fn.expression).eval(mapOf("x" to x.toDouble())) }.getOrDefault(Double.NaN)
                        "${fn.name}:${trim(y)}"
                    }
                    "x=$x  $values"
                }
                Text(rows, color = Muted, fontSize = 12.sp)
            }
            if (graphTool == GraphTool.Data) {
                OutlinedTextField(
                    value = dataText,
                    onValueChange = { dataText = it },
                    label = { Text("Data points: x,y; x,y") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (graphTool == GraphTool.Probability) {
                Text("Normal PDF at x=${trim(traceX.toDouble())}: ${trim(ProbabilityEngine.normalPdf(traceX.toDouble()))}", color = Cyan)
                Text("Binomial P(X=3), n=10, p=.5: ${trim(ProbabilityEngine.binomialPmf(3, 10, .5))}", color = Violet)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("Compare", comparisonMode) { comparisonMode = it }
                TogglePill("Split compare", splitComparison) { splitComparison = it; comparisonMode = it || comparisonMode }
                TogglePill("Residual plot", showResidualPlot) { showResidualPlot = it }
                TogglePill("Parameter handles", parameterHandleEnabled) { parameterHandleEnabled = it }
                if (pinnedTracePoints.isNotEmpty()) GlowButton("Clear ${pinnedTracePoints.size} pins") { pinnedTracePoints = emptyList() }
                GlowButton("Save snapshot") { graphSnapshots = graphSnapshots + GraphSnapshot("View ${graphSnapshots.size + 1}", liveFunctions.map { it.expression }, graphViewport) }
                selectedFunction?.let { selected ->
                    val selectedIndex = vm.state.functions.indexOfFirst { it.id == selected.id }
                    GlowButton("Layer up") { vm.moveFunctionLayer(selectedIndex, -1) }; GlowButton("Layer down") { vm.moveFunctionLayer(selectedIndex, 1) }
                    GraphTransformKind.entries.forEach { transformKind -> TogglePill(transformKind.name, graphTransformKind == transformKind) { graphTransformKind = transformKind } }
                    TogglePill("Animate transform", animateGraphTransform) { animateGraphTransform = it }
                    GlowButton("Apply preview") {
                        val transformed = GraphUxEngine.transform(selected.expression, graphTransformKind, graphTransformAmount.toDouble().let { if (graphTransformKind in setOf(GraphTransformKind.StretchX, GraphTransformKind.StretchY)) kotlin.math.abs(it).coerceAtLeast(.1) else it })
                        vm.duplicateFunction(selectedIndex); val newIndex = vm.state.functions.lastIndex; vm.editExpression(newIndex, transformed); selectedGraphRowId = vm.state.functions.getOrNull(newIndex)?.id
                    }
                }
            }
            if (selectedFunction != null) AxisSlider("Transform amount", graphTransformAmount, -2f..2f) { graphTransformAmount = it }
            brushInterval?.let { interval ->
                val area = primaryExpression?.let { expression -> runCatching { GraphDirectManipulationEngine.signedArea(engine.compile(stripEquation(expression)), interval.start, interval.endInclusive) }.getOrNull() }
                Insight("Brushed interval", "${trim(interval.start)} to ${trim(interval.endInclusive)} · signed area ${area?.let(::trim) ?: "undefined"}", Cyan)
            }
            latestSketchFit?.let { fit -> Insight("Sketch fit", "${fit.expression} · R² ${trim(fit.rSquared)}", Green) }
            if (graphSnapshots.isNotEmpty()) {
                Text("Saved graph states", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { graphSnapshots.forEach { snapshot -> GlowButton(if (graphSnapshotOverlay == snapshot) "Overlay on: ${snapshot.name}" else snapshot.name, onClick = { graphSnapshotOverlay = if (graphSnapshotOverlay == snapshot) null else snapshot }) } }
            }
            selectedGraphRowId?.let { id -> graphDomains[id]?.let { domain ->
                Text("Domain ${if (domain.leftClosed) "[" else "("}${trim(domain.minimum)}, ${trim(domain.maximum)}${if (domain.rightClosed) "]" else ")"} - drag axis handles to paint", color = Amber)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TogglePill("Left closed", domain.leftClosed) { graphDomains = graphDomains + (id to domain.copy(leftClosed = it)) }; TogglePill("Right closed", domain.rightClosed) { graphDomains = graphDomains + (id to domain.copy(rightClosed = it)) }; GlowButton("Clear domain") { graphDomains = graphDomains - id } }
            } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GraphTool.entries.forEach { tool ->
                    GlowButton(if (graphTool == tool) "• ${tool.name}" else tool.name, onClick = { graphTool = tool })
                }
                GlowButton("Export JSON", onClick = { vm.exportJson() })
            }
        }
    }
}

@Composable
private fun DesmosExpressionRow(
    row: com.indianservers.aiexplorer.workspace.MathExpressionRow,
    uiState: GraphRowMetadataState,
    selected: Boolean,
    graphObject: com.indianservers.aiexplorer.workspace.MathGraphObject?,
    onSelect: () -> Unit,
    onExpressionChange: (String) -> Unit,
    onToggleVisible: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleCollapsed: () -> Unit,
    onNoteChange: (String) -> Unit,
    onFolderChange: (String) -> Unit,
    onColor: () -> Unit,
) {
    val accent = graphColor(row.metadata.colorKey)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = .16f) else Color(0x33101824))
            .border(if (selected) 2.dp else 1.dp, accent.copy(alpha = if (selected) .88f else .38f), RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(8.dp)).background(accent))
            Text(row.name, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 46.dp))
            if (selected) Text("ACTIVE", color = Amber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                if (row.metadata.visible) "VISIBLE" else "HIDDEN",
                color = if (row.metadata.visible) Green else Muted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            if (uiState.folder.isNotBlank()) Text(uiState.folder, color = Amber, fontSize = 10.sp, maxLines = 1)
            Text(graphObject?.algebra?.classification ?: "waiting", color = Muted, fontSize = 10.sp)
        }
        AnimatedVisibility(!uiState.collapsed) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                IntentAwareMathField(
                    value = row.expression, onValueChange = onExpressionChange, label = "Expression",
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Expression row ${row.name}" },
                    placeholder = "y=sin(x)", showLegend = selected,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(
                        value = uiState.folder,
                        onValueChange = onFolderChange,
                        label = { Text("Folder") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = onNoteChange,
                        label = { Text("Note") },
                        singleLine = true,
                        modifier = Modifier.weight(1.35f),
                    )
                }
            }
        }
        graphObject?.let {
            Text("Resolved: ${it.resolvedExpression}", color = Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 2)
            Text("Table rows ${it.table.size} · roots ${it.roots.size} · extrema ${it.extrema.size}", color = Muted, fontSize = 10.sp)
        }
        row.metadata.error?.let { Text("Check: $it", color = Amber, fontSize = 11.sp) }
        if (uiState.note.isNotBlank()) Text(uiState.note, color = Muted, fontSize = 11.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton(if (uiState.collapsed) "Expand" else "Collapse", onClick = onToggleCollapsed)
            GlowButton(if (row.metadata.visible) "Hide" else "Show", onClick = onToggleVisible)
            GlowButton("Color", onClick = onColor)
            GlowButton("Duplicate", onClick = onDuplicate)
            GlowButton("Delete", onClick = onDelete)
        }
    }
}

@Composable
private fun ParameterRowCard(
    parameter: com.indianservers.aiexplorer.workspace.MathParameterRow,
    playing: Boolean,
    uiState: GraphSliderMetadataState,
    onTogglePlaying: () -> Unit,
    onToggleMode: () -> Unit,
    onSpeedChange: (Double) -> Unit,
    onValueChange: (Double) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x2230D9FF))
            .border(1.dp, Cyan.copy(.25f), RoundedCornerShape(14.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(parameter.name, color = Ink, fontWeight = FontWeight.Bold)
            Text("value ${trim(parameter.value)}", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton(if (playing) "Pause" else "Play", onClick = onTogglePlaying)
            GlowButton(uiState.mode.name, onClick = onToggleMode)
            Text(if (playing) "Animating linked graph/table" else "Tap Play to animate", color = Muted, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            GlowButton("Slower", onClick = { onSpeedChange(uiState.speed / 2.0) })
            Text("${trim(uiState.speed)}x", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            GlowButton("Faster", onClick = { onSpeedChange(uiState.speed * 2.0) })
        }
        Text("range ${trim(parameter.min)} to ${trim(parameter.max)} · step ${trim(parameter.step)} · ${uiState.mode.name.lowercase()} · direction ${uiState.direction}", color = Muted, fontSize = 10.sp)
        Slider(
            value = parameter.value.toFloat().coerceIn(parameter.min.toFloat(), parameter.max.toFloat()),
            onValueChange = { raw ->
                val stepped = (round(raw.toDouble() / parameter.step) * parameter.step).coerceIn(parameter.min, parameter.max)
                onValueChange(stepped)
            },
            valueRange = parameter.min.toFloat()..parameter.max.toFloat(),
            modifier = Modifier.semantics { contentDescription = "Parameter slider ${parameter.name}" },
        )
        Text("Drag to update all dependent graph rows, table outputs and algebra facts.", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun GeneratedTablePreview(rows: List<com.indianservers.aiexplorer.workspace.MathGeneratedTableRow>) {
    if (rows.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x22101824))
            .border(1.dp, Green.copy(.22f), RoundedCornerShape(14.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text("Generated table", color = Green, fontWeight = FontWeight.Bold)
        rows.take(5).forEach { row ->
            Text(
                "x=${trim(row.input)}  " + row.outputs.entries.joinToString("  ") { "${it.key}:${trim(it.value)}" },
                color = Muted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        if (rows.size > 5) Text("+${rows.size - 5} more linked rows", color = Muted, fontSize = 10.sp)
    }
}

private fun graphColor(key: String): Color = when (key) {
    "violet" -> Violet
    "green" -> Green
    "amber" -> Amber
    else -> Cyan
}

private fun parseDataPoints(source: String): List<Vec2> = source
    .split(';', '\n')
    .mapNotNull { entry ->
        val values = entry.trim().split(',').mapNotNull { it.trim().toDoubleOrNull() }
        if (values.size >= 2) Vec2(values[0], values[1]) else null
    }

@Composable
private fun Geometry3DScreen(vm: ExplorerViewModel, compact: Boolean) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var rotateX by remember { mutableFloatStateOf(25f) }
    var rotateY by remember { mutableFloatStateOf(-35f) }
    var rotateZ by remember { mutableFloatStateOf(15f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var transformMode by remember(vm.shapeExplorerScene) {
        mutableStateOf(if (vm.shapeExplorerScene) Transform3DMode.Scale else Transform3DMode.Move)
    }
    var wire by remember { mutableStateOf(true) }
    var projection by remember { mutableStateOf(CameraProjection.Perspective) }
    var selectionMode by remember { mutableStateOf(Selection3DMode.Object) }
    var subSelection by remember { mutableStateOf<SubObjectSelection?>(null) }
    var axisConstraint by remember { mutableStateOf(AxisConstraint.Free) }
    var precisionMode by remember { mutableStateOf(false) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lockedSolidIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var sectionEnabled by remember { mutableStateOf(false) }
    var clipSection by remember { mutableStateOf(false) }
    var sectionPlane by remember { mutableStateOf(EditableSectionPlane()) }
    var transformSpace by remember { mutableStateOf(SpatialTransformSpace.World) }
    var dragPlane by remember { mutableStateOf(SpatialDragPlane.Free) }
    var visualMode by remember { mutableStateOf(SpatialVisualMode.Wireframe) }
    var explodeAmount by remember { mutableFloatStateOf(0f) }
    var multiSelectEnabled by remember { mutableStateOf(false) }
    var selectedSolidIndices by remember { mutableStateOf<Set<Int>>(setOf(vm.selectedSolid)) }
    var hiddenSolidIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isolatedSolidIndices by remember { mutableStateOf<Set<Int>?>(null) }
    var solidGroups by remember { mutableStateOf<List<Set<Int>>>(emptyList()) }
    var cameraBookmarks by remember { mutableStateOf<List<SpatialCameraBookmark>>(emptyList()) }
    var showSceneNavigator by remember { mutableStateOf(true) }
    var showMotionTrails by remember { mutableStateOf(true) }
    var motionTrail by remember { mutableStateOf<List<Vec3>>(emptyList()) }
    var extrusionPreview by remember { mutableStateOf<com.indianservers.aiexplorer.core.ExtrusionPreview3D?>(null) }
    var meshEditHistory by remember { mutableStateOf(com.indianservers.aiexplorer.core.MeshEditHistory3D()) }
    var booleanResult by remember { mutableStateOf<com.indianservers.aiexplorer.core.BooleanMeshResult?>(null) }
    var spatialMeasurements by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.SpatialMeasurement3D>>(emptyList()) }
    val selectedIndex = vm.selectedSolid.coerceIn(0, vm.state.solids.lastIndex.coerceAtLeast(0))
    val selectedSolid = vm.state.solids.getOrNull(selectedIndex)
    val visibleSolidIndices = remember(vm.state.solids.size, hiddenSolidIndices, isolatedSolidIndices) {
        (vm.state.solids.indices).filterTo(linkedSetOf()) { it !in hiddenSolidIndices && (isolatedSolidIndices == null || it in isolatedSolidIndices.orEmpty()) }
    }
    val renderedSolids = remember(vm.state.solids, explodeAmount, extrusionPreview) {
        val values = extrusionPreview?.let { preview -> vm.state.solids.mapIndexed { index, solid -> if (index == preview.solidIndex) preview.preview else solid } } ?: vm.state.solids
        AdvancedSpatialInteractionEngine.exploded(values, explodeAmount.toDouble())
    }
    val collisions = remember(renderedSolids) { AdvancedSpatialInteractionEngine.collisions(renderedSolids) }
    val selectedBounds = remember(selectedSolid) {
        selectedSolid?.let { solid ->
            AnalyticGeometry3D.bounds(SolidMeshFactory.create(solid).vertices.map { it + solid.position })
        }
    }
    val selectedVectorIndex = vm.selectedVector3D.coerceIn(0, vm.state.vectors3D.lastIndex.coerceAtLeast(0))
    val selectedVector = vm.state.vectors3D.getOrNull(selectedVectorIndex)
    val sharedRenderScene = remember(vm.state.solids, vm.state.vectors3D) {
        SharedSpatialSceneBuilder.build("geometry-3d-workspace", vm.state.solids, vectors = vm.state.vectors3D)
    }
    val sharedRenderPlan = remember(sharedRenderScene) { SharedGpuSceneCompiler.compile(sharedRenderScene) }
    fun deleteCurrent3DSelection() {
        val targets = selectedSolidIndices.filterTo(linkedSetOf()) { it in vm.state.solids.indices }.ifEmpty { setOf(selectedIndex) }
        vm.deleteSelectedSolids(targets)
        selectedSolidIndices = vm.selectedSolid.takeIf { it in vm.state.solids.indices }?.let(::setOf) ?: emptySet()
        subSelection = null; extrusionPreview = null; hiddenSolidIndices = emptySet(); isolatedSolidIndices = null; lockedSolidIndices = emptySet(); solidGroups = emptyList()
    }
    Box(Modifier.fillMaxSize()) {
        Projected3DCanvas(
            modifier = Modifier.fillMaxSize(),
            solids = renderedSolids,
            vectors = vm.state.vectors3D,
            selectedIndex = selectedIndex,
            visibleSolidIndices = visibleSolidIndices,
            selectedVectorIndex = selectedVectorIndex,
            rx = rotateX,
            ry = rotateY,
            rz = rotateZ,
            zoom = zoom,
            cameraPan = cameraPan,
            transformMode = transformMode,
            wire = wire || visualMode != SpatialVisualMode.Solid,
            perspective = projection == CameraProjection.Perspective,
            selectionMode = selectionMode,
            subSelection = subSelection,
            sectionEnabled = sectionEnabled,
            sectionPlane = sectionPlane,
            clipSection = clipSection,
            onSelect = { index ->
                vm.selectSolid(index)
                selectedSolidIndices = if (multiSelectEnabled) {
                    if (index in selectedSolidIndices) selectedSolidIndices - index else selectedSolidIndices + index
                } else setOf(index)
            },
            onSubSelect = { subSelection = it },
            onSelectVector = vm::selectVector3D,
            onSolidDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (it !in lockedSolidIndices) {
                    if (it in selectedSolidIndices && selectedSolidIndices.size > 1) vm.beginSolidGroupDrag(selectedSolidIndices) else vm.beginSolidDrag(it)
                }
            },
            onSolidMove = { index, delta ->
                val spatialDelta = AdvancedSpatialInteractionEngine.transformSpace(delta, vm.state.solids.getOrNull(index)?.rotation ?: Vec3(0.0, 0.0, 0.0), transformSpace)
                val planeConstrained = AdvancedSpatialInteractionEngine.constrain(spatialDelta, dragPlane)
                val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(planeConstrained, axisConstraint), precisionMode)
                val proposed = vm.state.solids.getOrNull(index)?.position?.plus(constrained) ?: constrained
                val snappedPosition = if (vm.settings.snap) {
                    val targets = vm.state.solids.mapIndexedNotNull { otherIndex, solid -> solid.takeIf { otherIndex != index } }.flatMap { solid ->
                        com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap.targets(SolidMeshFactory.create(solid), solid.position)
                    }.toMutableList()
                    if (sectionEnabled || clipSection) {
                        val signed = sectionPlane.unitNormal.dot(proposed) - sectionPlane.offset
                        targets += com.indianservers.aiexplorer.core.SpatialSnapTarget(com.indianservers.aiexplorer.core.SpatialSnapKind.Plane, proposed - sectionPlane.unitNormal * signed, sectionPlane.unitNormal, "section plane")
                    }
                    val preview = com.indianservers.aiexplorer.core.ConstraintAwareSpatialSnap.snap(proposed, targets)
                    if (preview.target != null) preview.point else AdvancedSpatialInteractionEngine.snap(proposed, vm.state.solids.filterIndexed { i, _ -> i != index }.map { it.position })
                } else proposed
                val snapped = snappedPosition - (vm.state.solids.getOrNull(index)?.position ?: Vec3(0.0, 0.0, 0.0))
                if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupMove(snapped) else vm.previewSolidDrag(index, snapped)
                if (showMotionTrails) motionTrail = (motionTrail + snappedPosition).takeLast(24)
            },
            onSolidRotate = { index, delta -> if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupRotation(delta) else vm.previewSolidRotation(index, delta) },
            onSolidScale = { index, factor ->
                val face = subSelection?.takeIf { it.solidIndex == index && it.mode == Selection3DMode.Face }
                if (index in selectedSolidIndices && selectedSolidIndices.size > 1) vm.previewSolidGroupScale(factor)
                else if (face != null) vm.previewSolidFaceExtrusion(index, face.index, factor) else vm.previewSolidScale(index, factor)
            },
            onSolidAxisScale = vm::previewSolidAxisScale,
            onSectionPlaneMove = { distance -> sectionPlane = sectionPlane.moved(distance) },
            onSolidDragEnd = { if (selectedSolidIndices.size > 1) vm.endSolidGroupDrag() else vm.endSolidDrag() },
            onSolidDragCancel = { if (selectedSolidIndices.size > 1) vm.cancelSolidGroupDrag() else vm.cancelSolidDrag() },
            onSolidDropDelete = { deleteCurrent3DSelection() },
            onVectorDragStart = {
                if (vm.settings.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.beginVectorDrag(it)
            },
            onVectorMove = { index, delta -> vm.previewVectorDrag(index, PrecisionInteraction.apply(SmartSnapEngine.constrain(delta, axisConstraint), precisionMode)) },
            onVectorDragEnd = vm::endVectorDrag,
            onVectorDragCancel = vm::cancelVectorDrag,
            onOrbit = { dx, dy ->
                rotateY = (rotateY + dx).wrapDegrees()
                rotateX = (rotateX + dy).coerceIn(-89f, 89f)
            },
            onPan = { delta -> cameraPan += delta },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(.35f, 4f) },
            onResetCamera = {
                rotateX = 25f
                rotateY = -35f
                rotateZ = 15f
                zoom = 1f
                cameraPan = Offset.Zero
            },
            onEmptyTap = vm::dismissAllMenusAndPanels,
            onGestureModeChange = { gestureMode = it },
        )
        if (!vm.shapeExplorerScene && showSceneNavigator) SpatialSceneNavigator(
            solids = renderedSolids,
            selected = selectedSolidIndices,
            collisions = collisions.map { it.first to it.second },
            trail = if (showMotionTrails) motionTrail else emptyList(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 10.dp),
        )
        if (!vm.shapeExplorerScene && !vm.showRightPanel) OrientationCube(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            onPreset = { preset ->
                when (preset) {
                    "Front" -> { rotateX = 0f; rotateY = 0f; rotateZ = 0f }
                    "Top" -> { rotateX = 90f; rotateY = 0f; rotateZ = 0f }
                    "Side" -> { rotateX = 0f; rotateY = 90f; rotateZ = 0f }
                    else -> { rotateX = 25f; rotateY = -35f; rotateZ = 15f }
                }
            },
        )
        if (collisions.isNotEmpty()) Text(
            "⚠ ${collisions.size} overlap${if (collisions.size == 1) "" else "s"}",
            color = Amber,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(8.dp),
        )
        if (vm.shapeExplorerScene && selectedSolid != null) {
            val measurements = Geometry3D.measure(selectedSolid)
            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (compact) 70.dp else 78.dp)
                    .fillMaxWidth(if (compact) .94f else .55f)
                    .widthIn(max = 540.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))))
                    .border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(selectedSolid.type.name, color = Cyan, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        GlowButton("All formulas", icon = "ƒ", iconOnly = compact) { vm.togglePanel(PanelSlot.Right) }
                        GlowButton("Shapes", icon = "SE", iconOnly = compact, onClick = vm::openShapesExplorer)
                    }
                }
                Text(Geometry3D.formula(selectedSolid.type), color = Ink, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text("Surface ${trim(measurements.surfaceArea)}  ·  Volume ${trim(measurements.volume)}", color = Green, fontSize = 12.sp, maxLines = 1)
                Text("Drag the solid to resize.", color = Muted, fontSize = 10.sp, maxLines = 1)
            }
        }
        if (!vm.shapeExplorerScene) SolidTrackballPalette(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 126.dp),
            selectedSolid = selectedSolid,
            transformMode = transformMode,
            selectionMode = selectionMode,
            onAdd = vm::addSolid,
            onAddVector = vm::addVector3D,
            onTransformMode = { transformMode = it },
            onSelectionMode = {
                selectionMode = it
                subSelection = null
            },
        )
        if (!vm.shapeExplorerScene) InteractionHint(
            "Drag empty space to orbit · two fingers pan/zoom · drag object to transform",
            Modifier.align(Alignment.BottomEnd),
        )
        DeleteDropTarget(
            enabled = selectedSolid != null,
            onDelete = ::deleteCurrent3DSelection,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 14.dp),
        )
        if (!vm.shapeExplorerScene && selectedSolid != null) SmartSelectionHud(
            title = subSelection?.let { "${selectedSolid.type.name} · ${it.mode.name} ${it.index + 1}" } ?: selectedSolid.type.name,
            instruction = if (subSelection != null) "Sub-object selected · use coloured gizmo handles · empty space orbits" else "Drag a coloured gizmo handle to ${transformMode.name.lowercase()} on one axis",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
        ) {
            Transform3DMode.entries.forEach { mode ->
                GlowButton(if (mode == transformMode) "• ${mode.name}" else mode.name) { transformMode = mode }
            }
            GlowButton(if (selectedIndex in lockedSolidIndices) "Unlock" else "Lock") {
                lockedSolidIndices = if (selectedIndex in lockedSolidIndices) lockedSolidIndices - selectedIndex else lockedSolidIndices + selectedIndex
            }
            GlowButton("Copy") { vm.duplicateSelectedSolid(); selectedSolidIndices = setOf(vm.selectedSolid) }
            subSelection?.takeIf { it.mode == Selection3DMode.Face }?.let { face ->
                GlowButton("Preview extrusion") {
                    selectedSolid?.let { extrusionPreview = com.indianservers.aiexplorer.core.SpatialExtrusionEngine.preview(selectedIndex, face.index, .25, it) }
                }
            }
            GlowButton("Mirror X") { vm.transformSolid(selectedIndex) { AdvancedSpatialInteractionEngine.reflect(it, SpatialAlignment.X) } }
            GlowButton("Delete", icon = "×", onClick = ::deleteCurrent3DSelection)
        }
        extrusionPreview?.let { preview ->
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 142.dp).clip(RoundedCornerShape(14.dp))
                    .background(SurfaceA.copy(.97f)).border(1.dp, Amber.copy(.65f), RoundedCornerShape(14.dp)).padding(9.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Extrusion preview · face ${preview.faceIndex + 1} · ${trim(preview.amount)} units", color = Amber, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Commit", enabled = preview.valid) {
                        vm.transformSolid(preview.solidIndex) { preview.preview }
                        meshEditHistory = meshEditHistory.record(com.indianservers.aiexplorer.core.MeshEdit3D("Extrude face", preview.solidIndex, preview.original, preview.preview, SpatialSubObjectType.Face, preview.faceIndex))
                        extrusionPreview = null
                    }
                    GlowButton("Cancel") { extrusionPreview = null }
                }
            }
        }
        if (!vm.shapeExplorerScene) Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("⌂ Home") { rotateX = 25f; rotateY = -35f; rotateZ = 15f; zoom = 1f; cameraPan = Offset.Zero }
            GlowButton("Cycle overlap", enabled = visibleSolidIndices.isNotEmpty()) {
                val ordered = visibleSolidIndices.sorted(); val next = ordered.firstOrNull { it > selectedIndex } ?: ordered.firstOrNull()
                next?.let { vm.selectSolid(it); selectedSolidIndices = setOf(it) }
            }
            GlowButton("Hide", enabled = selectedSolidIndices.isNotEmpty()) { hiddenSolidIndices += selectedSolidIndices; selectedSolidIndices = emptySet() }
            GlowButton(if (isolatedSolidIndices == null) "Isolate" else "End isolate", enabled = selectedSolidIndices.isNotEmpty() || isolatedSolidIndices != null) {
                isolatedSolidIndices = if (isolatedSolidIndices == null) selectedSolidIndices.takeIf { it.isNotEmpty() } else null
            }
            GlowButton("Show all", enabled = hiddenSolidIndices.isNotEmpty()) { hiddenSolidIndices = emptySet(); isolatedSolidIndices = null }
            if (gestureMode != GestureMode.Idle) Text(gestureMode.label, color = Cyan, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
            Text("${trim(zoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
        if (!vm.shapeExplorerScene) FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Solids",
            rightLabel = "3D Tools",
            bottomLabel = "Transform",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).padding(top = 64.dp).width(230.dp)) {
            PanelHeader("3D Objects", vm::hidePanels, Cyan)
            vm.state.solids.forEachIndexed { index, solid ->
                val measure = Geometry3D.measure(solid)
                val accent = if (index == selectedIndex) Cyan else if (solid.type == SolidType.Cube) Cyan else Violet
                Text(
                    text = "${if (index == selectedIndex) "• " else ""}${solid.type.name}",
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { vm.selectSolid(index) }
                        .padding(8.dp),
                )
                Insight("Measure", "V ${trim(measure.volume)} · A ${trim(measure.surfaceArea)}", accent)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SolidType.entries.forEach { type -> GlowButton(type.name.take(6), onClick = { vm.addSolid(type) }) }
                GlowButton("Vector", onClick = vm::addVector3D)
            }
            if (vm.state.vectors3D.isNotEmpty()) {
                Text("Vectors", color = Ink, fontWeight = FontWeight.SemiBold)
                vm.state.vectors3D.forEachIndexed { index, vector ->
                    val accent = if (index == selectedVectorIndex) Amber else Green
                    Text(
                        text = "${if (index == selectedVectorIndex) "• " else ""}${vector.name} = <${trim(vector.components.x)}, ${trim(vector.components.y)}, ${trim(vector.components.z)}>",
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { vm.selectVector3D(index) }
                            .padding(8.dp),
                    )
                    Insight("Magnitude", trim(vector.magnitude), accent)
                }
            }
        }
        if (vm.showRightPanel && vm.shapeExplorerScene && selectedSolid != null) Shape3DStudioPanel(vm, selectedIndex, selectedSolid, compact, Modifier.align(if (compact) Alignment.Center else Alignment.TopEnd))
        if (vm.showRightPanel && !vm.shapeExplorerScene) GlassPanel(Modifier.align(Alignment.TopEnd).padding(top = 64.dp).width(260.dp)) {
            PanelHeader("3D Context Inspector", vm::hidePanels, Violet)
            Insight("Shared GPU", "${sharedRenderScene.primitives.size} objects · ${sharedRenderPlan.vertices.size / 10} vertices", Cyan)
            listOf("Zoom +", "Zoom -", "Scale +", "Scale -", "Wireframe", "Reset").forEach {
                GlowButton(it, onClick = {
                    if (it == "Zoom +") zoom = (zoom + .1f).coerceAtMost(1.8f)
                    if (it == "Zoom -") zoom = (zoom - .1f).coerceAtLeast(.6f)
                    if (it == "Scale +") selectedSolid?.let { solid ->
                        vm.transformSolid(selectedIndex) { solid.copy(width = solid.width + .2, height = solid.height + .2, depth = solid.depth + .2, radius = solid.radius + .1) }
                    }
                    if (it == "Scale -") selectedSolid?.let { solid ->
                        vm.transformSolid(selectedIndex) {
                            solid.copy(
                                width = (solid.width - .2).coerceAtLeast(.4),
                                height = (solid.height - .2).coerceAtLeast(.4),
                                depth = (solid.depth - .2).coerceAtLeast(.4),
                                radius = (solid.radius - .1).coerceAtLeast(.2),
                            )
                        }
                    }
                    if (it == "Wireframe") wire = !wire
                    if (it == "Reset") vm.reset()
                })
            }
            selectedSolid?.let {
                val measure = Geometry3D.measure(it)
                Insight("Selected", it.type.name, Cyan)
                Insight("Faces", measure.faces.toString(), Violet)
                Insight("Edges", measure.edges.toString(), Violet)
                Insight("Vertices", measure.vertices.toString(), Violet)
                GlowButton(if (selectedIndex in lockedSolidIndices) "Unlock object" else "Lock object") {
                    lockedSolidIndices = if (selectedIndex in lockedSolidIndices) lockedSolidIndices - selectedIndex else lockedSolidIndices + selectedIndex
                }
                GlowButton("Delete", onClick = ::deleteCurrent3DSelection)
                selectedBounds?.let { bounds ->
                    Insight("Bounds min", "${trim(bounds.minimum.x)},${trim(bounds.minimum.y)},${trim(bounds.minimum.z)}", Green)
                    Insight("Bounds max", "${trim(bounds.maximum.x)},${trim(bounds.maximum.y)},${trim(bounds.maximum.z)}", Green)
                }
                subSelection?.takeIf { selection -> selection.solidIndex == selectedIndex }?.let { selection ->
                    Insight("Sub-object", "${selection.mode.name} ${selection.index + 1}", Amber)
                    val anchor = subObjectAnchorWorld(it, selection)
                    Insight("World position", "${trim(anchor.x)}, ${trim(anchor.y)}, ${trim(anchor.z)}", Cyan)
                    Text("The transform gizmo is anchored to this ${selection.mode.name.lowercase()}. Drag a coloured axis or ring for a constrained edit.", color = Muted, fontSize = 10.sp)
                }
                if (sectionEnabled || clipSection) {
                    Insight("Section normal", "${trim(sectionPlane.unitNormal.x)}, ${trim(sectionPlane.unitNormal.y)}, ${trim(sectionPlane.unitNormal.z)}", Amber)
                    Insight("Plane offset", trim(sectionPlane.offset), Amber)
                }
            }
            selectedVector?.let {
                Insight("Vector", it.name, Amber)
                Insight("Components", "<${trim(it.components.x)}, ${trim(it.components.y)}, ${trim(it.components.z)}>", Green)
                Insight("Magnitude", trim(it.magnitude), Amber)
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("3D Controls", vm::hidePanels, Ink)
            AxisSlider("Rotate X", rotateX, -180f..180f) { rotateX = it }
            AxisSlider("Rotate Y", rotateY, -180f..180f) { rotateY = it }
            AxisSlider("Rotate Z", rotateZ, -180f..180f) { rotateZ = it }
            AxisSlider("Zoom", zoom, .6f..1.8f) { zoom = it }
            TogglePill("Perspective", projection == CameraProjection.Perspective) {
                projection = if (it) CameraProjection.Perspective else CameraProjection.Orthographic
            }
            TogglePill("Wireframe", wire) { wire = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(AxisConstraint.Free, AxisConstraint.X, AxisConstraint.Y, AxisConstraint.Z).forEach { axis ->
                    GlowButton(if (axisConstraint == axis) "● ${axis.name}" else axis.name) { axisConstraint = axis }
                }
                GlowButton(if (precisionMode) "● Precision" else "Precision") { precisionMode = !precisionMode }
            }
            Text("Professional direct manipulation", color = Cyan, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SpatialTransformSpace.entries.forEach { space -> GlowButton(if (transformSpace == space) "● ${space.name}" else space.name) { transformSpace = space } }
                SpatialDragPlane.entries.forEach { plane -> GlowButton(if (dragPlane == plane) "● ${plane.name}" else plane.name) { dragPlane = plane } }
                SpatialVisualMode.entries.forEach { mode -> GlowButton(if (visualMode == mode) "● ${mode.name}" else mode.name) { visualMode = mode; wire = mode != SpatialVisualMode.Solid } }
            }
            AxisSlider("Exploded view", explodeAmount, 0f..3f) { explodeAmount = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TogglePill("Multi-select", multiSelectEnabled) { multiSelectEnabled = it; if (!it) selectedSolidIndices = setOf(selectedIndex) }
                TogglePill("Scene navigator", showSceneNavigator) { showSceneNavigator = it }
                TogglePill("Motion trails", showMotionTrails) { showMotionTrails = it; if (!it) motionTrail = emptyList() }
                GlowButton("Group", enabled = selectedSolidIndices.size >= 2) { solidGroups = solidGroups + listOf(selectedSolidIndices) }
                GlowButton("Ungroup", enabled = solidGroups.any { it == selectedSolidIndices }) { solidGroups = solidGroups.filterNot { it == selectedSolidIndices } }
            }
            if (selectedSolidIndices.size >= 2) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SpatialAlignment.entries.forEach { alignment ->
                    GlowButton("Align ${alignment.name}") { vm.replaceSolids("Aligned objects on ${alignment.name}") { AdvancedSpatialInteractionEngine.align(it, selectedSolidIndices, alignment) } }
                    GlowButton("Space ${alignment.name}", enabled = selectedSolidIndices.size >= 3) { vm.replaceSolids("Distributed objects on ${alignment.name}") { AdvancedSpatialInteractionEngine.distribute(it, selectedSolidIndices, alignment) } }
                }
                GlowButton("Concentric") {
                    vm.replaceSolids("Applied concentric constraint") { solids ->
                        val anchor = selectedSolidIndices.firstOrNull()?.let(solids::getOrNull)?.position ?: return@replaceSolids solids
                        solids.mapIndexed { index, solid -> if (index in selectedSolidIndices) solid.copy(position = anchor) else solid }
                    }
                }
                GlowButton("Parallel orientation") {
                    vm.replaceSolids("Applied parallel-orientation constraint") { solids ->
                        val rotation = selectedSolidIndices.firstOrNull()?.let(solids::getOrNull)?.rotation ?: return@replaceSolids solids
                        solids.mapIndexed { index, solid -> if (index in selectedSolidIndices) solid.copy(rotation = rotation) else solid }
                    }
                }
                com.indianservers.aiexplorer.core.BooleanMeshOperation.entries.forEach { operation ->
                    GlowButton(operation.name) {
                        val pair = selectedSolidIndices.take(2).mapNotNull(vm.state.solids::getOrNull)
                        if (pair.size == 2) booleanResult = runCatching {
                            fun worldMesh(solid: Solid): com.indianservers.aiexplorer.core.SolidMesh {
                                val mesh = SolidMeshFactory.create(solid)
                                return mesh.copy(vertices = mesh.vertices.map { solidLocalToWorld(solid, it) })
                            }
                            com.indianservers.aiexplorer.core.BooleanMeshEngine.apply(worldMesh(pair[0]), worldMesh(pair[1]), operation)
                        }.getOrNull()
                    }
                }
            }
            booleanResult?.let { result ->
                Insight("Boolean ${result.operation.name.lowercase()}", "${result.mesh.vertices.size} vertices · ${result.mesh.faces.size} faces · ${result.diagnostic}", if (result.verified) Green else Amber)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Copy Boolean OBJ") { copyShapeText(context, "boolean.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(result.mesh, "Boolean${result.operation.name}")) }
                    GlowButton("Copy Boolean STL") { copyShapeText(context, "boolean.stl", com.indianservers.aiexplorer.core.SpatialExportEngine.stl(result.mesh, "Boolean${result.operation.name}")) }
                    GlowButton("Clear Boolean") { booleanResult = null }
                }
            }
            if (meshEditHistory.undo.isNotEmpty() || meshEditHistory.redo.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Undo mesh", enabled = meshEditHistory.undo.isNotEmpty()) {
                    val (solids, history) = meshEditHistory.undo(vm.state.solids); vm.replaceSolids("Undo sub-object edit") { solids }; meshEditHistory = history
                }
                GlowButton("Redo mesh", enabled = meshEditHistory.redo.isNotEmpty()) {
                    val (solids, history) = meshEditHistory.redo(vm.state.solids); vm.replaceSolids("Redo sub-object edit") { solids }; meshEditHistory = history
                }
            }
            if (spatialMeasurements.isNotEmpty()) {
                Text("Pinned measurements", color = Ink, fontWeight = FontWeight.SemiBold)
                spatialMeasurements.forEach { value -> Insight(value.label, "${trim(value.value)} ${value.unit}", Green) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Export measurements") { copyShapeText(context, "measurements.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.measurementsCsv(spatialMeasurements)) }
                    GlowButton("Clear measurements") { spatialMeasurements = emptyList() }
                }
            }
            if (selectedSolidIndices.size >= 2) {
                val pair = selectedSolidIndices.take(2).mapNotNull(vm.state.solids::getOrNull)
                if (pair.size == 2) {
                    Insight("Measurement anchor", "distance ${trim((pair[0].position - pair[1].position).magnitude())}", Green)
                    GlowButton("Pin distance") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.distance(pair[0].position, pair[1].position) }
                }
                val triple = selectedSolidIndices.take(3).mapNotNull(vm.state.solids::getOrNull)
                if (triple.size == 3) GlowButton("Pin angle") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.angle(triple[0].position, triple[1].position, triple[2].position) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("Front") { rotateX = 0f; rotateY = 0f; rotateZ = 0f }
                GlowButton("Top") { rotateX = 90f; rotateY = 0f; rotateZ = 0f }
                GlowButton("Side") { rotateX = 0f; rotateY = 90f; rotateZ = 0f }
                GlowButton("Isometric") { rotateX = 25f; rotateY = -35f; rotateZ = 15f }
                GlowButton("Save camera") { cameraBookmarks = (cameraBookmarks + SpatialCameraBookmark("View ${cameraBookmarks.size + 1}", Vec3(rotateX.toDouble(), rotateY.toDouble(), rotateZ.toDouble()), zoom, cameraPan.x, cameraPan.y)).takeLast(6) }
            }
            if (cameraBookmarks.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                cameraBookmarks.forEach { bookmark -> GlowButton(bookmark.name) { rotateX = bookmark.rotation.x.toFloat(); rotateY = bookmark.rotation.y.toFloat(); rotateZ = bookmark.rotation.z.toFloat(); zoom = bookmark.zoom; cameraPan = Offset(bookmark.panX, bookmark.panY) } }
            }
            TogglePill("Cross-section", sectionEnabled) { sectionEnabled = it }
            TogglePill("Clip below plane", clipSection) { clipSection = it }
            if (sectionEnabled || clipSection) {
                Text("Editable section plane", color = Amber, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("X" to Vec3(1.0, 0.0, 0.0), "Y" to Vec3(0.0, 1.0, 0.0), "Z" to Vec3(0.0, 0.0, 1.0), "Diagonal" to Vec3(1.0, 1.0, 1.0)).forEach { (label, normal) ->
                        GlowButton(label) { sectionPlane = sectionPlane.withNormal(normal) }
                    }
                }
                AxisSlider("Plane offset", sectionPlane.offset.toFloat(), -3f..3f) { value -> sectionPlane = sectionPlane.copy(origin = sectionPlane.unitNormal * value.toDouble()) }
                AxisSlider("Normal X", sectionPlane.unitNormal.x.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(value.toDouble(), sectionPlane.unitNormal.y, sectionPlane.unitNormal.z)) }
                AxisSlider("Normal Y", sectionPlane.unitNormal.y.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(sectionPlane.unitNormal.x, value.toDouble(), sectionPlane.unitNormal.z)) }
                AxisSlider("Normal Z", sectionPlane.unitNormal.z.toFloat(), -1f..1f) { value -> sectionPlane = sectionPlane.withNormal(Vec3(sectionPlane.unitNormal.x, sectionPlane.unitNormal.y, value.toDouble())) }
                DirectVec3Editor("Plane origin", sectionPlane.origin) { sectionPlane = sectionPlane.copy(origin = it) }
                DirectVec3Editor("Plane normal", sectionPlane.unitNormal) { sectionPlane = sectionPlane.withNormal(it) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Measure section", enabled = selectedSolid != null) {
                        selectedSolid?.let { solid ->
                            val mesh = SolidMeshFactory.create(solid).let { value -> value.copy(vertices = value.vertices.map { solidLocalToWorld(solid, it) }) }
                            val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
                            if (section.size >= 2) spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.sectionPerimeter(section)
                        }
                    }
                    GlowButton("Export section", enabled = selectedSolid != null) {
                        selectedSolid?.let { solid ->
                            val mesh = SolidMeshFactory.create(solid).let { value -> value.copy(vertices = value.vertices.map { solidLocalToWorld(solid, it) }) }
                            val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
                            copyShapeText(context, "section.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.contoursCsv(section.map { sectionPlane.offset to it }))
                        }
                    }
                }
                Text("Drag the amber normal handle directly on the canvas to move the plane.", color = Muted, fontSize = 10.sp)
            }
            selectedSolid?.let { solid ->
                Text("Selected ${solid.type.name}", color = Cyan, fontWeight = FontWeight.SemiBold)
                DirectVec3Editor("Position", solid.position) { value -> vm.transformSolid(selectedIndex) { it.copy(position = value) } }
                DirectVec3Editor("Rotation (degrees)", solid.rotation) { value -> vm.transformSolid(selectedIndex) { it.copy(rotation = value) } }
                DirectVec3Editor("Scale / dimensions", Vec3(solid.width, solid.height, solid.depth)) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(width = value.x.coerceAtLeast(.1), height = value.y.coerceAtLeast(.1), depth = value.z.coerceAtLeast(.1)) }
                }
                AxisSlider("Width", solid.width.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(width = value.toDouble()) }
                }
                AxisSlider("Height", solid.height.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(height = value.toDouble()) }
                }
                AxisSlider("Depth", solid.depth.toFloat(), .4f..4f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(depth = value.toDouble()) }
                }
                AxisSlider("Radius", solid.radius.toFloat(), .2f..2f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(radius = value.toDouble()) }
                }
                if (solid.type == SolidType.Frustum) AxisSlider("Top radius", solid.topRadius.toFloat(), .1f..2f) { value ->
                    vm.transformSolid(selectedIndex) { it.copy(topRadius = value.toDouble()) }
                }
                val mesh = SolidMeshFactory.create(solid)
                val euler = com.indianservers.aiexplorer.core.SolidInteractionLab.euler(mesh)
                Insight("Euler proof", "V ${euler.vertices} - E ${euler.edges} + F ${euler.faces} = ${euler.value}${if (euler.holds) " ✓" else ""}", Green)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowButton("Measure volume") { spatialMeasurements += com.indianservers.aiexplorer.core.SpatialMeasurementEngine.volume(solid) }
                    subSelection?.takeIf { it.solidIndex == selectedIndex && it.mode == Selection3DMode.Face }?.let { selection ->
                        GlowButton("Measure face") { runCatching { com.indianservers.aiexplorer.core.SpatialMeasurementEngine.faceArea(mesh, selection.index) }.getOrNull()?.let { spatialMeasurements += it } }
                    }
                    GlowButton("Unfold net") {
                        val net = com.indianservers.aiexplorer.core.SolidInteractionLab.net(mesh)
                        copyShapeText(context, "${solid.type.name} net", net.joinToString("\n") { face -> face.joinToString(";") { "${it.x},${it.y}" } })
                    }
                    GlowButton("Revolve profile") {
                        val revolved = com.indianservers.aiexplorer.core.SolidInteractionLab.solidOfRevolution(listOf(Vec2(-solid.height / 2, 0.0), Vec2(-solid.height / 2, solid.radius), Vec2(solid.height / 2, solid.radius), Vec2(solid.height / 2, 0.0)))
                        copyShapeText(context, "solid-of-revolution.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(revolved, "SolidOfRevolution"))
                    }
                    GlowButton("Export OBJ") { copyShapeText(context, "${solid.type.name}.obj", com.indianservers.aiexplorer.core.SpatialExportEngine.obj(mesh, solid.type.name)) }
                    GlowButton("Export STL") { copyShapeText(context, "${solid.type.name}.stl", com.indianservers.aiexplorer.core.SpatialExportEngine.stl(mesh, solid.type.name)) }
                }
            }
            selectedVector?.let { vector ->
                Text("Selected vector ${vector.name}", color = Amber, fontWeight = FontWeight.SemiBold)
                DirectVec3Editor("Vector start", vector.start) { value -> vm.transformVector3D(selectedVectorIndex) { it.copy(start = value) } }
                DirectVec3Editor("Vector end", vector.end) { value -> vm.transformVector3D(selectedVectorIndex) { it.copy(end = value) } }
                AxisSlider("dx", vector.components.x.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(value.toDouble(), it.components.y, it.components.z)) }
                }
                AxisSlider("dy", vector.components.y.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(it.components.x, value.toDouble(), it.components.z)) }
                }
                AxisSlider("dz", vector.components.z.toFloat(), -5f..5f) { value ->
                    vm.transformVector3D(selectedVectorIndex) { it.copy(end = it.start + Vec3(it.components.x, it.components.y, value.toDouble())) }
                }
            }
        }
    }
}

@Composable
private fun SpatialARScreen(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val runtime = remember(activity) { activity?.let(::ArCoreRuntime) }
    var compositorView by remember { mutableStateOf<ARCoreCompositorView?>(null) }
    var capabilities by remember { mutableStateOf(ARCapabilities(ARAvailability.Checking)) }
    var liveAR by remember { mutableStateOf(false) }
    var frameState by remember { mutableStateOf<ARFrameState?>(null) }
    var arFrame by remember { mutableStateOf<ArFrameSnapshot?>(null) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var placementMode by remember { mutableStateOf(true) }
    var reticleHit by remember { mutableStateOf<ArHitCandidate?>(null) }
    var arSelection by remember { mutableStateOf(ArSelectionState()) }
    var overlapHits by remember { mutableStateOf<List<ArPickHit>>(emptyList()) }
    var stylusHoverHit by remember { mutableStateOf<ArPickHit?>(null) }
    var gizmoMode by remember { mutableStateOf(ArGizmoMode.Translate) }
    var gizmoAxis by remember { mutableStateOf(ArGizmoAxis.X) }
    var arMultiSelect by remember { mutableStateOf(false) }
    var subObjectKind by remember { mutableStateOf(ArSubObjectKind.Whole) }
    var snapEnabled by remember { mutableStateOf(true) }
    var precisionMode by remember { mutableStateOf(false) }
    var arClipboard by remember { mutableStateOf<List<Solid>>(emptyList()) }
    var arGroups by remember { mutableStateOf<List<Set<String>>>(emptyList()) }
    var numericPosition by remember { mutableStateOf("0, 0, 0") }
    var numericRotation by remember { mutableStateOf("0, 0, 0") }
    var numericScale by remember { mutableStateOf("1") }
    var numericPlaneNormal by remember { mutableStateOf("0, 1, 0") }
    var numericPlaneOffset by remember { mutableStateOf("0") }
    var liveError by remember { mutableStateOf("") }
    var selectedLesson by remember { mutableIntStateOf(0) }
    var thermalLevel by remember { mutableStateOf(ThermalLevel.Nominal) }
    var showSpatialDetails by remember { mutableStateOf(false) }
    val currentLiveAR by rememberUpdatedState(liveAR)
    val currentCompositorView by rememberUpdatedState(compositorView)
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraGranted = granted
        capabilities = if (granted && runtime != null) runtime.prepare(cameraPermissionGranted = true, userRequestedInstall = true).toSpatialCapabilities() else {
            capabilities.copy(message = "Camera permission was not granted; the spatial simulator remains fully available.")
        }
    }
    LaunchedEffect(runtime) {
        if (runtime != null) capabilities = runtime.checkAvailability().toSpatialCapabilities()
    }
    LaunchedEffect(capabilities.message, cameraGranted) {
        if (cameraGranted && runtime != null && capabilities.message.contains("session configured", ignoreCase = true)) {
            runtime.resume()
                .onSuccess {
                    capabilities = it.toSpatialCapabilities()
                    liveAR = true
                }
                .onFailure { liveError = it.message ?: "ARCore could not resume." }
        }
    }
    DisposableEffect(runtime, activity) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                currentCompositorView?.onPause()
                runtime?.pause()
            }

            override fun onResume(owner: LifecycleOwner) {
                if (currentLiveAR) {
                    runtime?.resume()
                    currentCompositorView?.onResume()
                }
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            currentCompositorView?.releaseRenderer()
            currentCompositorView?.onPause()
            runtime?.pause()
            runtime?.close()
        }
    }

    val placement = vm.state.spatialPlacement
    LaunchedEffect(placement.isPlaced) {
        if (!placement.isPlaced) placementMode = true
    }
    val guidance = SpatialSafety.guidance(placement.trackingQuality)
    val policy = remember(thermalLevel) { SpatialPerformanceManager.policy(thermalLevel, 22.0) }
    val surfaceMesh = remember(vm.state.surfaceExpression, policy.surfaceDensity) {
        runCatching { Graph3D().mesh(vm.state.surfaceExpression, density = policy.surfaceDensity) }.getOrNull()
    }
    val lesson = SpatialLessonCatalog.lessons[selectedLesson]
    val sharedScene = remember(vm.state.solids, vm.state.vectors3D, surfaceMesh, lesson.id, placement.depthOcclusionEnabled, frameState?.lighting?.pixelIntensity) {
        val probability = if (lesson.id == "probability-surface-ar") surfaceMesh else null
        SharedSpatialSceneBuilder.build(
            id = "spatial-${lesson.id}",
            solids = vm.state.solids,
            surface = if (probability == null) surfaceMesh else null,
            vectors = vm.state.vectors3D,
            probabilitySurface = probability,
            annotations = listOf(
                SpatialAnnotation("origin", Vec3(0.0, 0.0, 0.0), "O"),
                SpatialAnnotation("lesson", Vec3(0.0, 1.5, 0.0), lesson.title),
            ),
        ).copy(depthOcclusion = placement.depthOcclusionEnabled, environmentIntensity = frameState?.lighting?.pixelIntensity ?: 1f)
    }
    val selectedSolidIndices = arSelection.objectIds.mapNotNullTo(linkedSetOf()) {
        it.removePrefix("solid-").toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
    }
    val selectedVectorIndex = arSelection.primaryObjectId
        ?.removePrefix("vector-")
        ?.toIntOrNull()
        ?.takeIf(vm.state.vectors3D.indices::contains)
    val interactiveScene = remember(sharedScene, arSelection, placementMode, reticleHit, gizmoMode, stylusHoverHit) {
        phase4DisplayScene(sharedScene, arSelection, placementMode && reticleHit != null, vm.state.solids, gizmoMode, stylusHoverHit?.objectId)
    }
    val previewPlacement = if (placementMode) reticleHit?.previewSpatialPlacement(placement) ?: placement else placement
    val activeAnchor = runtime?.anchors()?.firstOrNull { it.id == placement.anchorId }
    val canonicalArScene = remember(interactiveScene, placement, activeAnchor, arSelection) {
        ArPhase4SpatialBridge.scene(interactiveScene, placement, activeAnchor, arSelection)
    }
    val trackingAllowsDirectManipulation = !liveAR || arFrame?.camera?.trackingState == ArTrackingState.Tracking
    val gpuPlan = remember(interactiveScene) { SharedGpuSceneCompiler.compile(interactiveScene) }
    val currentCompositorScene by rememberUpdatedState(SpatialCompositorScene(interactiveScene, previewPlacement))
    LaunchedEffect(arSelection.primaryObjectId, vm.state.solids, vm.state.vectors3D) {
        val solid = arSelection.primaryObjectId?.removePrefix("solid-")?.toIntOrNull()?.let(vm.state.solids::getOrNull)
        val vector = arSelection.primaryObjectId?.removePrefix("vector-")?.toIntOrNull()?.let(vm.state.vectors3D::getOrNull)
        when {
            solid != null -> {
                numericPosition = "${trim(solid.position.x)}, ${trim(solid.position.y)}, ${trim(solid.position.z)}"
                numericRotation = "${trim(solid.rotation.x)}, ${trim(solid.rotation.y)}, ${trim(solid.rotation.z)}"
                numericScale = "1"
            }
            vector != null -> {
                numericPosition = "${trim(vector.start.x)}, ${trim(vector.start.y)}, ${trim(vector.start.z)}"
                numericRotation = "${trim(vector.end.x)}, ${trim(vector.end.y)}, ${trim(vector.end.z)}"
                numericScale = "1"
            }
        }
    }
    Box(Modifier.fillMaxSize().onSizeChanged { viewportSize = it }) {
        if (liveAR && runtime != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    ARCoreCompositorView(
                        viewContext,
                        runtime,
                        sceneProvider = { currentCompositorScene },
                        onFrame = { snapshot ->
                            arFrame = snapshot
                            frameState = snapshot.toSpatialFrame()
                            reticleHit = if (placementMode && viewportSize.width > 0 && viewportSize.height > 0) {
                                runtime.hitTest(ArVector2(viewportSize.width / 2f, viewportSize.height / 2f)).firstOrNull()
                            } else {
                                null
                            }
                            liveError = ""
                        },
                        onError = { liveError = it },
                    ).also { compositorView = it }
                },
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(arSelection, gizmoMode, gizmoAxis, placementMode, subObjectKind, arMultiSelect, snapEnabled, precisionMode, numericPlaneNormal, numericPlaneOffset, canonicalArScene, arFrame, trackingAllowsDirectManipulation) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (!trackingAllowsDirectManipulation) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { it.consume() }
                                    if (event.changes.none { it.pressed }) break
                                }
                                return@awaitEachGesture
                            }
                            val precisionMultiplier = when {
                                down.type == PointerType.Stylus && precisionMode -> .2
                                down.type == PointerType.Stylus -> .35
                                precisionMode -> .4
                                else -> 1.0
                            }
                            val gestureIndices = arSelection.objectIds.mapNotNullTo(linkedSetOf()) {
                                it.removePrefix("solid-").toIntOrNull()?.takeIf(vm.state.solids.indices::contains)
                            }
                            val editableIndices = gestureIndices.filterTo(linkedSetOf()) { "solid-$it" !in arSelection.lockedObjectIds }
                            val vectorGestureIndex = arSelection.primaryObjectId
                                ?.removePrefix("vector-")
                                ?.toIntOrNull()
                                ?.takeIf(vm.state.vectors3D.indices::contains)
                                ?.takeIf { "vector-$it" !in arSelection.lockedObjectIds }
                            val objectGesture = !placementMode && (editableIndices.isNotEmpty() || vectorGestureIndex != null)
                            val basePosition = editableIndices.singleOrNull()?.let(vm.state.solids::getOrNull)?.position
                            val snapTargets = if (snapEnabled && basePosition != null) {
                                vm.state.solids.flatMapIndexed { index, solid ->
                                    if (index in editableIndices) emptyList()
                                    else ConstraintAwareSpatialSnap.targets(SolidMeshFactory.create(solid), solid.position)
                                }
                            } else {
                                emptyList()
                            }
                            if (objectGesture) {
                                when {
                                    vectorGestureIndex != null -> vm.beginVectorDrag(vectorGestureIndex)
                                    editableIndices.size > 1 -> vm.beginSolidGroupDrag(editableIndices)
                                    else -> vm.beginSolidDrag(editableIndices.single())
                                }
                            } else {
                                vm.beginSpatialGesture()
                            }
                            var totalPan = Offset.Zero
                            var totalRotation = 0f
                            var totalScale = 1f
                            while (true) {
                                val event = awaitPointerEvent()
                                totalPan += event.calculatePan()
                                totalRotation += event.calculateRotation()
                                totalScale *= event.calculateZoom()
                                if (objectGesture) {
                                    val raw = Vec3(
                                        totalPan.x / 420.0 * precisionMultiplier,
                                        -totalPan.y / 420.0 * precisionMultiplier,
                                        totalPan.y / 620.0 * precisionMultiplier,
                                    )
                                    val axisDelta = when (gizmoAxis) {
                                        ArGizmoAxis.X -> Vec3(raw.x, 0.0, 0.0)
                                        ArGizmoAxis.Y -> Vec3(0.0, raw.y, 0.0)
                                        ArGizmoAxis.Z -> Vec3(0.0, 0.0, raw.z)
                                        ArGizmoAxis.Uniform -> raw
                                    }
                                    val snappedDelta = if (snapEnabled && basePosition != null) {
                                        val proposed = basePosition + axisDelta
                                        val planeNormal = parseSpatialTriple(numericPlaneNormal)?.takeIf { it.magnitude() > 1e-9 }?.normalized()
                                        val planeOffset = numericPlaneOffset.toDoubleOrNull() ?: 0.0
                                        val projectedPlane = planeNormal?.let { normal ->
                                            proposed - normal * (normal.dot(proposed) - planeOffset)
                                        }
                                        val planeDistance = projectedPlane?.let { (proposed - it).magnitude() } ?: Double.POSITIVE_INFINITY
                                        val geometric = ConstraintAwareSpatialSnap.snap(proposed, snapTargets, .18)
                                        when {
                                            projectedPlane != null && planeDistance <= .18 -> projectedPlane - basePosition
                                            geometric.target != null -> geometric.point - basePosition
                                            else -> Vec3(
                                                (axisDelta.x * 10.0).roundToInt() / 10.0,
                                                (axisDelta.y * 10.0).roundToInt() / 10.0,
                                                (axisDelta.z * 10.0).roundToInt() / 10.0,
                                            )
                                        }
                                    } else axisDelta
                                    if (vectorGestureIndex != null) {
                                        vm.previewVectorDrag(vectorGestureIndex, snappedDelta)
                                    } else if (editableIndices.size > 1) {
                                        when (gizmoMode) {
                                            ArGizmoMode.Translate -> vm.previewSolidGroupMove(snappedDelta)
                                            ArGizmoMode.Rotate -> vm.previewSolidGroupRotation(
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> Vec3(totalRotation.toDouble(), 0.0, 0.0)
                                                    ArGizmoAxis.Y -> Vec3(0.0, totalRotation.toDouble(), 0.0)
                                                    ArGizmoAxis.Z -> Vec3(0.0, 0.0, totalRotation.toDouble())
                                                    ArGizmoAxis.Uniform -> Vec3(totalRotation.toDouble(), totalRotation.toDouble(), totalRotation.toDouble())
                                                },
                                            )
                                            ArGizmoMode.Scale -> vm.previewSolidGroupScale(totalScale.toDouble())
                                        }
                                    } else {
                                        val index = editableIndices.single()
                                        when (gizmoMode) {
                                            ArGizmoMode.Translate -> vm.previewSolidDrag(index, snappedDelta)
                                            ArGizmoMode.Rotate -> vm.previewSolidRotation(
                                                index,
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> Vec3(totalRotation.toDouble(), 0.0, 0.0)
                                                    ArGizmoAxis.Y -> Vec3(0.0, totalRotation.toDouble(), 0.0)
                                                    ArGizmoAxis.Z -> Vec3(0.0, 0.0, totalRotation.toDouble())
                                                    ArGizmoAxis.Uniform -> Vec3(totalRotation.toDouble(), totalRotation.toDouble(), totalRotation.toDouble())
                                                },
                                            )
                                            ArGizmoMode.Scale -> vm.previewSolidAxisScale(
                                                index,
                                                when (gizmoAxis) {
                                                    ArGizmoAxis.X -> TransformGizmoAxis.X
                                                    ArGizmoAxis.Y -> TransformGizmoAxis.Y
                                                    ArGizmoAxis.Z -> TransformGizmoAxis.Z
                                                    ArGizmoAxis.Uniform -> TransformGizmoAxis.Uniform
                                                },
                                                totalScale.toDouble(),
                                            )
                                        }
                                    }
                                } else {
                                    vm.previewSpatialGesture(totalPan, totalRotation, totalScale)
                                }
                                if (event.changes.none { it.pressed }) break
                                event.changes.forEach { it.consume() }
                            }
                            if (objectGesture) {
                                when {
                                    vectorGestureIndex != null -> vm.endVectorDrag()
                                    editableIndices.size > 1 -> vm.endSolidGroupDrag()
                                    else -> vm.endSolidDrag()
                                }
                            } else {
                                vm.endSpatialGesture()
                            }
                            if (totalPan.getDistance() < 12f && kotlin.math.abs(totalRotation) < 2f && kotlin.math.abs(totalScale - 1f) < .03f) {
                                if (placementMode) {
                                    runtime.hitTest(ArVector2(down.position.x, down.position.y)).firstOrNull()?.let { hit ->
                                        runtime.createAnchor(hit.id, System.currentTimeMillis())
                                            .onSuccess { anchor ->
                                                placement.anchorId.takeIf(String::isNotBlank)?.let(runtime::detachAnchor)
                                                vm.placeSpatialHit(
                                                    hit.toSpatialHit().copy(
                                                        trackableId = anchor.id,
                                                        positionMeters = Vec3(
                                                            anchor.pose.positionMeters.x,
                                                            anchor.pose.positionMeters.y,
                                                            anchor.pose.positionMeters.z,
                                                        ),
                                                    ),
                                                )
                                                placementMode = false
                                                reticleHit = null
                                            }
                                            .onFailure { liveError = it.message ?: "Could not create the spatial anchor." }
                                    }
                                } else {
                                    val snapshot = arFrame
                                    if (snapshot != null) {
                                        val hits = ArPhase4SpatialBridge.pick(
                                            ArVector2(down.position.x, down.position.y),
                                            viewportSize.width,
                                            viewportSize.height,
                                            snapshot,
                                            canonicalArScene,
                                        ).filter { it.kind == subObjectKind }
                                        overlapHits = hits
                                        val hit = if (
                                            hits.firstOrNull()?.objectId == arSelection.primaryObjectId &&
                                            overlapHits.isNotEmpty()
                                        ) {
                                            ArSelectionEngine.cycle(hits, arSelection.subObject)
                                        } else {
                                            hits.firstOrNull()
                                        }
                                        if (hit != null) {
                                            arSelection = ArSelectionEngine.select(arSelection, hit, arMultiSelect)
                                            hit.objectId.removePrefix("solid-").toIntOrNull()?.let(vm::selectSolid)
                                        } else if (!arMultiSelect) {
                                            arSelection = arSelection.copy(objectIds = emptySet(), primaryObjectId = null, subObject = null)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .pointerInput(arFrame, canonicalArScene, viewportSize, placementMode, trackingAllowsDirectManipulation, subObjectKind) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val hover = event.changes.firstOrNull { it.type == PointerType.Stylus && !it.pressed }
                                stylusHoverHit = if (
                                    hover != null &&
                                    !placementMode &&
                                    trackingAllowsDirectManipulation &&
                                    arFrame != null
                                ) {
                                    ArPhase4SpatialBridge.pick(
                                        ArVector2(hover.position.x, hover.position.y),
                                        viewportSize.width,
                                        viewportSize.height,
                                        arFrame!!,
                                        canonicalArScene,
                                    ).firstOrNull { it.kind == subObjectKind }
                                } else {
                                    null
                                }
                            }
                        }
                    },
            )
            if (placementMode) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val color = if (reticleHit != null) Green else Amber
                    drawCircle(color.copy(alpha = .18f), radius = 28f, center = center)
                    drawCircle(color, radius = 18f, center = center, style = Stroke(width = 3f))
                    drawLine(color, center - Offset(30f, 0f), center + Offset(30f, 0f), strokeWidth = 2f)
                    drawLine(color, center - Offset(0f, 30f), center + Offset(0f, 30f), strokeWidth = 2f)
                }
                Text(
                    reticleHit?.let { "${it.type.name} · ${(it.confidence * 100).roundToInt()}% · ±${trim(it.uncertaintyMeters)} m" }
                        ?: "Scan a plane or feature point",
                    color = Ink,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center).offset(y = 48.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceA.copy(.88f)).padding(8.dp),
                )
            }
        } else {
            SpatialPreviewCanvas(
                modifier = Modifier.fillMaxSize(),
                solids = vm.state.solids,
                placement = placement,
                onGestureStart = vm::beginSpatialGesture,
                onGesture = vm::previewSpatialGesture,
                onGestureEnd = vm::endSpatialGesture,
            )
        }
        GlassPanel(Modifier.align(Alignment.TopStart).width(285.dp).padding(top = 105.dp)) {
            PanelHeader("AR Spatial Lab", { vm.open(MathModule.Geometry3D) }, Cyan)
            Insight("Mode", if (liveAR) "Live ARCore camera + shared GPU scene" else "Accessible spatial simulator", Cyan)
            Insight("ARCore", capabilities.message, if (capabilities.availability == ARAvailability.Unsupported) Amber else Green)
            Insight("Tracking", guidance.title, if (guidance.safeToPlace) Green else Amber)
            Text(guidance.instruction, color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("Enable ARCore", onClick = {
                    if (activity == null) capabilities = capabilities.copy(message = "ARCore requires an Android activity.")
                    else if (!cameraGranted) cameraPermission.launch(Manifest.permission.CAMERA)
                    else if (runtime != null) capabilities = runtime.prepare(cameraPermissionGranted = true, userRequestedInstall = true).toSpatialCapabilities()
                })
                GlowButton(
                    when {
                        placementMode && placement.isPlaced -> "Cancel re-place"
                        placementMode -> "Aim & tap"
                        else -> "Re-place"
                    },
                    onClick = {
                        placementMode = if (placement.isPlaced) !placementMode else true
                        if (!placementMode) reticleHit = null
                    },
                )
                GlowButton("Reset", onClick = {
                    placement.anchorId.takeIf(String::isNotBlank)?.let { runtime?.detachAnchor(it) }
                    vm.resetSpatialScene()
                    placementMode = true
                    arSelection = ArSelectionState()
                })
            }
            if (liveAR) GlowButton("Use simulator", onClick = {
                compositorView?.onPause()
                liveAR = false
                runtime?.pause()
            })
            Insight("Scale", placement.visibleScale, Violet)
            Insight("Estimate", "±${trim(placement.measurementUncertaintyMeters)} m · educational only", Amber)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton(if (placement.scaleMode == ARScaleMode.OneToOne) "• 1:1" else "1:1", onClick = { vm.setSpatialScaleMode(ARScaleMode.OneToOne) })
                GlowButton(if (placement.scaleMode == ARScaleMode.FitToSpace) "• Fit" else "Fit", onClick = { vm.setSpatialScaleMode(ARScaleMode.FitToSpace) })
                TogglePill("Depth occlusion", placement.depthOcclusionEnabled) {
                    vm.setDepthOcclusion(it && capabilities.depthSupported)
                }
            }
            Insight("Depth", when {
                capabilities.depthSupported -> "Supported; occlusion can be enabled."
                capabilities.availability == ARAvailability.Ready -> "Unavailable; objects remain outlined."
                else -> "Checked when an ARCore session is prepared."
            }, if (capabilities.depthSupported) Green else Muted)
            reticleHit?.takeIf { placementMode }?.let {
                Insight("Placement preview", "${it.type.name} · ${(it.confidence * 100).roundToInt()}% confidence", Green)
                Insight("Uncertainty", "±${trim(it.uncertaintyMeters)} m · ${placement.visibleScale}", Amber)
            }
            if (!placementMode) {
                Text("Direct manipulation", color = Ink, fontWeight = FontWeight.Bold)
                if (!trackingAllowsDirectManipulation) {
                    Text("Tracking paused · object selection and gizmos are temporarily frozen; mathematical state and selection are preserved.", color = Amber, fontSize = 11.sp)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    ArGizmoMode.entries.forEach { mode ->
                        GlowButton(if (gizmoMode == mode) "• ${mode.name}" else mode.name) { gizmoMode = mode }
                    }
                    ArGizmoAxis.entries.forEach { axis ->
                        GlowButton(if (gizmoAxis == axis) "• ${axis.name}" else axis.name) { gizmoAxis = axis }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    TogglePill("Snap", snapEnabled) { snapEnabled = it }
                    TogglePill("Precision", precisionMode) { precisionMode = it }
                    TogglePill("Multi", arMultiSelect) {
                        arMultiSelect = it
                        if (!it) arSelection.primaryObjectId?.let { id ->
                            arSelection = arSelection.copy(objectIds = setOf(id))
                        }
                    }
                    ArSubObjectKind.entries.forEach { kind ->
                        GlowButton(if (subObjectKind == kind) "• ${kind.name}" else kind.name) { subObjectKind = kind }
                    }
                }
                if (snapEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            numericPlaneNormal,
                            { numericPlaneNormal = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Plane normal") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            numericPlaneOffset,
                            { numericPlaneOffset = it },
                            modifier = Modifier.width(82.dp),
                            label = { Text("Offset") },
                            singleLine = true,
                        )
                    }
                }
                arSelection.primaryObjectId?.let { selectedId ->
                    Insight(
                        "Selected",
                        buildString {
                            append(selectedId)
                            arSelection.subObject?.takeIf { it.kind != ArSubObjectKind.Whole }?.let {
                                append(" · ${it.kind.name} ${(it.subObjectIndex ?: 0) + 1}")
                            }
                        },
                        Cyan,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (selectedId in arSelection.lockedObjectIds) "Unlock" else "Lock") {
                            arSelection = ArSelectionEngine.toggleLock(arSelection)
                        }
                        GlowButton("Hide") { arSelection = ArSelectionEngine.hideSelected(arSelection) }
                        GlowButton(if (arSelection.isolatedObjectIds == null) "Isolate" else "End isolate") {
                            arSelection = if (arSelection.isolatedObjectIds == null) ArSelectionEngine.isolate(arSelection) else ArSelectionEngine.showAll(arSelection)
                        }
                        GlowButton("Cycle", enabled = overlapHits.isNotEmpty()) {
                            ArSelectionEngine.cycle(overlapHits, arSelection.subObject)?.let {
                                arSelection = ArSelectionEngine.select(arSelection, it, false)
                                it.objectId.removePrefix("solid-").toIntOrNull()?.let(vm::selectSolid)
                            }
                        }
                        GlowButton("Copy", enabled = selectedSolidIndices.isNotEmpty()) {
                            arClipboard = selectedSolidIndices.mapNotNull(vm.state.solids::getOrNull)
                            context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(
                                android.content.ClipData.newPlainText(
                                    "AI Explorer AR objects",
                                    arClipboard.joinToString("\n") {
                                        "${it.type.name} @ (${trim(it.position.x)}, ${trim(it.position.y)}, ${trim(it.position.z)})"
                                    },
                                ),
                            )
                        }
                        GlowButton("Paste", enabled = arClipboard.isNotEmpty()) {
                            val start = vm.state.solids.size
                            vm.replaceSolids("Paste AR objects") { solids ->
                                solids + arClipboard.mapIndexed { index, solid ->
                                    solid.copy(position = solid.position + Vec3(.35 + index * .12, .15, .35))
                                }
                            }
                            val ids = (start until vm.state.solids.size).mapTo(linkedSetOf()) { "solid-$it" }
                            arSelection = arSelection.copy(objectIds = ids, primaryObjectId = ids.lastOrNull())
                        }
                        GlowButton("Duplicate", enabled = selectedSolidIndices.size == 1) {
                            selectedSolidIndices.singleOrNull()?.let(vm::selectSolid)
                            vm.duplicateSelectedSolid()
                            arSelection = ArSelectionState(setOf("solid-${vm.selectedSolid}"), "solid-${vm.selectedSolid}")
                        }
                        GlowButton("Delete", enabled = selectedSolidIndices.isNotEmpty() || selectedVectorIndex != null) {
                            selectedVectorIndex?.let(vm::deleteVector3D)
                            vm.deleteSelectedSolids(selectedSolidIndices)
                            arSelection = ArSelectionState()
                            if (vm.state.solids.isEmpty() && vm.state.vectors3D.isEmpty()) {
                                placement.anchorId.takeIf(String::isNotBlank)?.let { runtime?.detachAnchor(it) }
                                vm.resetSpatialScene()
                                placementMode = true
                            }
                        }
                        GlowButton("Front", enabled = selectedSolidIndices.size == 1) {
                            val index = selectedSolidIndices.singleOrNull() ?: return@GlowButton
                            vm.replaceSolids("Bring AR object to front") { solids ->
                                solids.getOrNull(index)?.let { selected -> solids.filterIndexed { i, _ -> i != index } + selected } ?: solids
                            }
                            val id = "solid-${vm.state.solids.lastIndex}"
                            arSelection = ArSelectionState(setOf(id), id)
                            arGroups = emptyList()
                        }
                        GlowButton("Back", enabled = selectedSolidIndices.size == 1) {
                            val index = selectedSolidIndices.singleOrNull() ?: return@GlowButton
                            vm.replaceSolids("Send AR object to back") { solids ->
                                solids.getOrNull(index)?.let { selected -> listOf(selected) + solids.filterIndexed { i, _ -> i != index } } ?: solids
                            }
                            arSelection = ArSelectionState(setOf("solid-0"), "solid-0")
                            arGroups = emptyList()
                        }
                    }
                    if (selectedSolidIndices.size > 1) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            GlowButton("Group") {
                                arGroups = arGroups + listOf(arSelection.objectIds)
                            }
                            SpatialAlignment.entries.forEach { alignment ->
                                GlowButton("Align ${alignment.name}") {
                                    vm.replaceSolids("Align AR group on ${alignment.name}") {
                                        AdvancedSpatialInteractionEngine.align(it, selectedSolidIndices, alignment)
                                    }
                                }
                            }
                            GlowButton("Distribute X", enabled = selectedSolidIndices.size >= 3) {
                                vm.replaceSolids("Distribute AR group") {
                                    AdvancedSpatialInteractionEngine.distribute(it, selectedSolidIndices, SpatialAlignment.X)
                                }
                            }
                        }
                    }
                    Text(if (selectedVectorIndex != null) "Numeric vector editor" else "Numeric transform", color = Ink, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        numericPosition,
                        { numericPosition = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (selectedVectorIndex != null) "Vector start x, y, z" else "Position x, y, z") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        numericRotation,
                        { numericRotation = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (selectedVectorIndex != null) "Vector end x, y, z" else "Rotation x°, y°, z°") },
                        singleLine = true,
                    )
                    if (selectedVectorIndex == null) OutlinedTextField(
                        numericScale,
                        { numericScale = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Scale factor") },
                        singleLine = true,
                    )
                    GlowButton("Apply exact transform", enabled = selectedSolidIndices.size == 1 || selectedVectorIndex != null) {
                        val positionValues = parseSpatialTriple(numericPosition)
                        val rotationValues = parseSpatialTriple(numericRotation)
                        val factor = numericScale.toDoubleOrNull()?.coerceIn(.05, 20.0)
                        selectedVectorIndex?.let { index ->
                            vm.transformVector3D(index) { vector ->
                                vector.copy(start = positionValues ?: vector.start, end = rotationValues ?: vector.end)
                            }
                        }
                        selectedSolidIndices.singleOrNull()?.let { index ->
                            vm.transformSolid(index) { solid ->
                                solid.copy(
                                    position = positionValues ?: solid.position,
                                    rotation = rotationValues ?: solid.rotation,
                                    width = if (factor != null) solid.width * factor else solid.width,
                                    height = if (factor != null) solid.height * factor else solid.height,
                                    depth = if (factor != null) solid.depth * factor else solid.depth,
                                    radius = if (factor != null) solid.radius * factor else solid.radius,
                                    topRadius = if (factor != null) solid.topRadius * factor else solid.topRadius,
                                )
                            }
                            numericScale = "1"
                        }
                    }
                }
                stylusHoverHit?.let {
                    Insight("Stylus hover", "${it.objectId} · ${it.kind.name.lowercase()} preview", Green)
                }
                if (arSelection.hiddenObjectIds.isNotEmpty() || arSelection.isolatedObjectIds != null) {
                    GlowButton("Show all objects") { arSelection = ArSelectionEngine.showAll(arSelection) }
                }
                if (arGroups.isNotEmpty()) Insight("Groups", "${arGroups.size} AR group(s) · shared transforms enabled", Violet)
            }
            GlowButton(if (showSpatialDetails) "Hide spatial details" else "Lessons & renderer", onClick = { showSpatialDetails = !showSpatialDetails })
            if (showSpatialDetails) {
                Text("Spatial lesson", color = Ink, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpatialLessonCatalog.lessons.forEachIndexed { index, item ->
                        GlowButton(if (index == selectedLesson) "• ${item.title.take(12)}" else item.title.take(12), onClick = { selectedLesson = index })
                    }
                }
                Text(lesson.learningGoal, color = Muted, fontSize = 11.sp)
                Insight("Shared renderer", "${sharedScene.primitives.size} objects · ${gpuPlan.vertices.size / 10} GPU vertices", Cyan)
                Insight("Lighting", if (frameState?.lighting?.valid == true) "${trim(frameState?.lighting?.pixelIntensity?.toDouble() ?: 1.0)}× environment" else "Simulator neutral light", Green)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThermalLevel.entries.forEach { level -> GlowButton(level.name.take(4), onClick = { thermalLevel = level }) }
                }
                Insight("Performance", "${policy.quality.name} · ${policy.targetFps} fps · mesh ${policy.surfaceDensity}", if (thermalLevel >= ThermalLevel.Severe) Amber else Green)
                Insight("Privacy & safety", "${ARPrivacySafetyChecklist.items.size} mandatory checks · camera frames stay local", Violet)
                if (liveError.isNotBlank()) Text(liveError, color = Amber, fontSize = 11.sp)
            }
            Text("Placement and measurements are educational estimates, not certified physical measurements.", color = Amber, fontSize = 11.sp)
        }
        InteractionHint(
            "Drag to move the anchored scene · pinch to scale · twist to rotate · Undo restores the last gesture",
            Modifier.align(Alignment.BottomEnd),
        )
    }
}

private fun parseSpatialTriple(value: String): Vec3? {
    val parts = value.split(',', ';', ' ').map(String::trim).filter(String::isNotEmpty)
    if (parts.size != 3) return null
    val values = parts.map { it.toDoubleOrNull() ?: return null }
    return Vec3(values[0], values[1], values[2])
}

private fun phase4DisplayScene(
    source: com.indianservers.aiexplorer.spatial.SpatialRenderScene,
    selection: ArSelectionState,
    ghost: Boolean,
    solids: List<Solid>,
    gizmoMode: ArGizmoMode,
    hoverObjectId: String?,
): com.indianservers.aiexplorer.spatial.SpatialRenderScene {
    val styled = source.primitives.map { primitive ->
        val selected = primitive.id in selection.objectIds
        val hovered = primitive.id == hoverObjectId && !selected
        val visible = primitive.visible && ArSelectionEngine.isVisible(selection, primitive.id)
        val color = primitive.material.colorRgba.toMutableList().apply {
            while (size < 4) add(1f)
            if (ghost) this[3] = minOf(this[3], .28f)
            if (selected) {
                this[0] = 1f
                this[1] = .68f
                this[2] = .12f
                this[3] = 1f
            } else if (hovered) {
                this[0] = .35f
                this[1] = 1f
                this[2] = .72f
                this[3] = 1f
            }
        }
        primitive.copy(
            visible = visible,
            selectable = primitive.selectable && !ghost,
            material = primitive.material.copy(
                colorRgba = color,
                emissive = if (selected || hovered) maxOf(primitive.material.emissive, if (selected) .3f else .18f) else primitive.material.emissive,
                blendMode = if (ghost) com.indianservers.aiexplorer.spatial.SpatialBlendMode.Transparent else primitive.material.blendMode,
            ),
        )
    }
    if (ghost || selection.objectIds.isEmpty()) return source.copy(primitives = styled)
    val gizmos = selection.objectIds.mapNotNull {
        it.removePrefix("solid-").toIntOrNull()?.let { index -> index to solids.getOrNull(index) }
    }.filter { it.second != null }.flatMap { (index, nullableSolid) ->
        val solid = nullableSolid ?: return@flatMap emptyList()
        val origin = solid.position
        val axes = listOf(
            Triple("x", Vec3(1.0, 0.0, 0.0), listOf(1f, .18f, .22f, 1f)),
            Triple("y", Vec3(0.0, 1.0, 0.0), listOf(.2f, 1f, .4f, 1f)),
            Triple("z", Vec3(0.0, 0.0, 1.0), listOf(.25f, .55f, 1f, 1f)),
        )
        axes.map { (name, axis, color) ->
            val points = when (gizmoMode) {
                ArGizmoMode.Translate, ArGizmoMode.Scale -> listOf(origin, origin + axis * .85)
                ArGizmoMode.Rotate -> (0..40).map { step ->
                    val angle = step * Math.PI * 2.0 / 40.0
                    when (name) {
                        "x" -> origin + Vec3(0.0, kotlin.math.cos(angle), kotlin.math.sin(angle)) * .72
                        "y" -> origin + Vec3(kotlin.math.cos(angle), 0.0, kotlin.math.sin(angle)) * .72
                        else -> origin + Vec3(kotlin.math.cos(angle), kotlin.math.sin(angle), 0.0) * .72
                    }
                }
            }
            com.indianservers.aiexplorer.spatial.SpatialPrimitive(
                id = "gizmo-$index-$name",
                kind = com.indianservers.aiexplorer.spatial.SpatialPrimitiveKind.Curve,
                geometry = com.indianservers.aiexplorer.spatial.SpatialGeometry(
                    vertices = points,
                    lines = if (gizmoMode == ArGizmoMode.Rotate) points.indices.toList().dropLast(1).map { it to it + 1 } else listOf(0 to 1),
                    pointRadius = if (gizmoMode == ArGizmoMode.Scale) .12 else .055,
                ),
                material = com.indianservers.aiexplorer.spatial.SpatialMaterial(
                    name = "$name ${gizmoMode.name.lowercase()} handle",
                    colorRgba = color,
                    roughness = .28f,
                    emissive = .35f,
                ),
                label = "${gizmoMode.name} ${name.uppercase()}",
                selectable = false,
            )
        }
    }
    return source.copy(primitives = styled + gizmos)
}

@Composable
private fun SpatialPreviewCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    placement: com.indianservers.aiexplorer.spatial.SpatialScenePlacement,
    onGestureStart: () -> Unit,
    onGesture: (Offset, Float, Float) -> Unit,
    onGestureEnd: () -> Unit,
) {
    val currentPlacement by rememberUpdatedState(placement)
    Canvas(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onGestureStart()
                    var totalPan = Offset.Zero
                    var totalRotation = 0f
                    var totalScale = 1f
                    while (true) {
                        val event = awaitPointerEvent()
                        totalPan += event.calculatePan()
                        totalRotation += event.calculateRotation()
                        totalScale *= event.calculateZoom()
                        onGesture(totalPan, totalRotation, totalScale)
                        event.changes.forEach { if (it.pressed) it.consume() }
                        if (event.changes.none { it.pressed }) break
                    }
                    onGestureEnd()
                }
            }
            .semantics { contentDescription = "AR spatial mathematics preview with direct move, rotate and scale gestures" },
    ) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF08131B), Color(0xFF18242A), Color(0xFF101317))))
        val scene = currentPlacement
        val center = Offset(
            size.width * .56f + scene.pose.positionMeters.x.toFloat() * 220f,
            size.height * .53f + scene.pose.positionMeters.z.toFloat() * 34f,
        )
        drawPerspectiveGrid(center.copy(y = center.y + 110f))
        drawCircle(if (scene.isPlaced) Green else Amber, 20f, center, style = Stroke(3f))
        drawLine(Color.White.copy(.7f), center - Offset(32f, 0f), center + Offset(32f, 0f), 2f)
        drawLine(Color.White.copy(.7f), center - Offset(0f, 32f), center + Offset(0f, 32f), 2f)
        solids.forEachIndexed { index, solid ->
            drawSolidProjection(
                solid = solid,
                offset = solid.position,
                rx = 24f + scene.pose.rotationDegrees.x.toFloat(),
                ry = -34f + scene.pose.rotationDegrees.y.toFloat(),
                rz = scene.pose.rotationDegrees.z.toFloat(),
                center = center,
                scale = 58f * scene.pose.uniformScale.toFloat(),
                color = if (index % 2 == 0) Cyan else Violet,
                wire = !scene.depthOcclusionEnabled,
                selected = index == 0,
                perspective = true,
                subSelection = null,
                sectionEnabled = false,
                sectionPlane = EditableSectionPlane(),
                clipSection = false,
            )
        }
        drawGraphLabel(if (scene.isPlaced) "Anchored · ${scene.visibleScale}" else "Placement preview · tap Place", center + Offset(28f, -130f), if (scene.isPlaced) Green else Amber)
    }
}

@Composable
private fun Graph3DScreen(vm: ExplorerViewModel) {
    val context = LocalContext.current
    val graph3D = remember { Graph3D() }
    val surfaceCalculus = remember { SurfaceCalculus() }
    var density by remember { mutableFloatStateOf(26f) }
    var rotation by remember { mutableFloatStateOf(35f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var cameraPan by remember { mutableStateOf(Offset.Zero) }
    var tilt by remember { mutableFloatStateOf(55f) }
    var sliceZ by remember { mutableFloatStateOf(2f) }
    var traceX by remember { mutableFloatStateOf(1f) }
    var traceY by remember { mutableFloatStateOf(1f) }
    var showWireframe by remember { mutableStateOf(true) }
    var showContours by remember { mutableStateOf(true) }
    var showSlice by remember { mutableStateOf(true) }
    var showGradient by remember { mutableStateOf(true) }
    var showBox by remember { mutableStateOf(true) }
    var activeTool by remember { mutableStateOf(SurfaceTool.Surface) }
    var surfaceLayers by remember { mutableStateOf(listOf(com.indianservers.aiexplorer.core.SpatialSurfaceLayer("surface-1", vm.state.surfaceExpression))) }
    var contourLevel by remember { mutableFloatStateOf(2f) }
    var gradientPlayback by remember { mutableStateOf(com.indianservers.aiexplorer.core.GradientPlayback3D(emptyList())) }
    var viewPreset by remember { mutableStateOf(SurfaceViewPreset.Isometric) }
    fun applyView(preset: SurfaceViewPreset) {
        viewPreset = preset
        when (preset) {
            SurfaceViewPreset.Isometric -> { tilt = 55f; rotation = 35f; roll = 0f }
            SurfaceViewPreset.X, SurfaceViewPreset.YZ -> { tilt = 0f; rotation = 90f; roll = 0f }
            SurfaceViewPreset.Y, SurfaceViewPreset.XZ -> { tilt = 90f; rotation = 0f; roll = 0f }
            SurfaceViewPreset.Z, SurfaceViewPreset.XY -> { tilt = 0f; rotation = 0f; roll = 0f }
        }
        cameraPan = Offset.Zero
    }
    val insight = remember(vm.state.surfaceExpression) { graph3D.insight(vm.state.surfaceExpression) }
    val mesh = remember(vm.state.surfaceExpression, density) {
        runCatching { graph3D.mesh(vm.state.surfaceExpression, density = density.toInt()) }.getOrNull()
    }
    LaunchedEffect(vm.state.surfaceExpression) {
        surfaceLayers = surfaceLayers.mapIndexed { index, layer -> if (index == 0) layer.copy(expression = vm.state.surfaceExpression) else layer }
    }
    val additionalSurfaceMeshes = remember(surfaceLayers, density) {
        surfaceLayers.drop(1).filter { it.visible }.mapIndexedNotNull { index, layer ->
            val qualityDensity = when (layer.quality) { com.indianservers.aiexplorer.core.SpatialQuality.Battery -> 12; com.indianservers.aiexplorer.core.SpatialQuality.Balanced -> 24; com.indianservers.aiexplorer.core.SpatialQuality.High -> 36; com.indianservers.aiexplorer.core.SpatialQuality.Ultra -> 52 }
            runCatching { graph3D.mesh(layer.expression, density = qualityDensity) }.getOrNull()?.let { it to listOf(Violet, Green, Amber, Color(0xFFFF6FAE))[index % 4] }
        }
    }
    val differential = remember(vm.state.surfaceExpression, traceX, traceY) {
        runCatching { surfaceCalculus.analyze(vm.state.surfaceExpression, traceX.toDouble(), traceY.toDouble()) }.getOrNull()
    }
    LaunchedEffect(gradientPlayback.playing) {
        while (gradientPlayback.playing) { delay(90); gradientPlayback = gradientPlayback.tick() }
    }
    val sharedSurfaceScene = remember(mesh) { SharedSpatialSceneBuilder.build("graph-3d-workspace", emptyList(), surface = mesh) }
    val sharedSurfacePlan = remember(sharedSurfaceScene) { SharedGpuSceneCompiler.compile(sharedSurfaceScene) }
    Box(Modifier.fillMaxSize()) {
        SurfaceCanvas3D(
            modifier = Modifier.fillMaxSize(),
            expression = vm.state.surfaceExpression,
            mesh = mesh,
            additionalMeshes = additionalSurfaceMeshes,
            gradientPath = gradientPlayback.path,
            gradientPathIndex = gradientPlayback.index,
            rotation = rotation,
            tilt = tilt,
            roll = roll,
            zoom = zoom,
            cameraPan = cameraPan,
            sliceZ = sliceZ.toDouble(),
            trace = Vec2(traceX.toDouble(), traceY.toDouble()),
            showWireframe = showWireframe,
            showContours = showContours,
            showSlice = showSlice,
            showGradient = showGradient,
            showBox = showBox,
            activeTool = activeTool,
            onRotate = { delta -> rotation = (rotation + delta).coerceIn(-180f, 180f) },
            onTilt = { delta -> tilt = (tilt + delta).coerceIn(-89f, 89f) },
            onRoll = { delta -> roll = (roll + delta).wrapDegrees() },
            onPan = { delta -> cameraPan += delta },
            onZoom = { factor -> zoom = (zoom * factor).coerceIn(.35f, 4f) },
            onResetCamera = {
                zoom = 1f
                rotation = 35f
                tilt = 55f
                roll = 0f
                viewPreset = SurfaceViewPreset.Isometric
                cameraPan = Offset.Zero
            },
            onTrace = { point ->
                traceX = point.x.toFloat().coerceIn(-3f, 3f)
                traceY = point.y.toFloat().coerceIn(-3f, 3f)
            },
        )
        SurfaceExampleChips(
            modifier = Modifier.align(Alignment.TopCenter),
            onSelect = vm::setSurfaceExpression,
        )
        SurfaceViewChips(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            active = viewPreset,
            onSelect = { applyView(it) },
        )
        InteractionHint(
            "Drag the amber surface handle to snap across the mesh · green is gradient · cyan is normal · violet is tangent plane",
            Modifier.align(Alignment.BottomEnd),
        )
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Equation",
            rightLabel = "Surface",
            bottomLabel = "3D Ctrl",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(280.dp)) {
            PanelHeader("3D Graph Workspace", vm::hidePanels, Cyan)
            IntentAwareMathField(
                value = vm.state.surfaceExpression, onValueChange = vm::setSurfaceExpression,
                label = "3D surface", modifier = Modifier.fillMaxWidth(), placeholder = "z=x^2+y^2",
            )
            AxisSlider("Mesh density", density, 8f..48f) { density = it }
            AxisSlider("Rotation", rotation, -180f..180f) { rotation = it }
            AxisSlider("Tilt", tilt, -89f..89f) { tilt = it }
            AxisSlider("Roll", roll, -180f..180f) { roll = it }
            Text("Surface object drawer", color = Ink, fontWeight = FontWeight.Bold)
            surfaceLayers.forEachIndexed { index, layer ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(Color(0x22101824)).padding(7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        layer.expression,
                        { expression -> surfaceLayers = surfaceLayers.mapIndexed { i, old -> if (i == index) old.copy(expression = expression) else old }; if (index == 0) vm.setSurfaceExpression(expression) },
                        label = { Text("Surface ${index + 1}") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        GlowButton(if (layer.visible) "Hide" else "Show") { surfaceLayers = surfaceLayers.mapIndexed { i, old -> if (i == index) old.copy(visible = !old.visible) else old } }
                        GlowButton(layer.material.name) { surfaceLayers = surfaceLayers.mapIndexed { i, old -> if (i == index) old.copy(material = com.indianservers.aiexplorer.core.SpatialMaterial.entries[(old.material.ordinal + 1) % com.indianservers.aiexplorer.core.SpatialMaterial.entries.size]) else old } }
                        GlowButton(layer.quality.name) { surfaceLayers = surfaceLayers.mapIndexed { i, old -> if (i == index) old.copy(quality = com.indianservers.aiexplorer.core.SpatialQuality.entries[(old.quality.ordinal + 1) % com.indianservers.aiexplorer.core.SpatialQuality.entries.size]) else old } }
                        if (index > 0) GlowButton("Remove") { surfaceLayers = surfaceLayers.filterIndexed { i, _ -> i != index } }
                    }
                    AxisSlider("Domain radius", maxOf(abs(layer.domain.uMin), abs(layer.domain.uMax)).toFloat(), 1f..10f) { radius ->
                        surfaceLayers = surfaceLayers.mapIndexed { i, old -> if (i == index) old.copy(domain = com.indianservers.aiexplorer.core.SurfaceDomain3D(-radius.toDouble(), radius.toDouble(), -radius.toDouble(), radius.toDouble())) else old }
                    }
                }
            }
            GlowButton("+ Surface") { surfaceLayers = surfaceLayers + com.indianservers.aiexplorer.core.SpatialSurfaceLayer("surface-${System.currentTimeMillis()}", "sin(x)+cos(y)", material = com.indianservers.aiexplorer.core.SpatialMaterial.Gloss) }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(250.dp)) {
            PanelHeader("Surface Insights", vm::hidePanels, Violet)
            Insight("Shared GPU renderer", "${sharedSurfacePlan.vertices.size / 10} vertices · ${sharedSurfacePlan.triangleIndices.size / 3} triangles", Cyan)
            Insight("Surface", insight.classification, Cyan)
            Insight("Vertex", insight.vertex?.let { "(${trim(it.x)}, ${trim(it.y)}, ${trim(it.z)})" } ?: "sampled", Violet)
            Insight("Range", insight.range, Cyan)
            Insight("Symmetry", insight.symmetry, Violet)
            Insight("Trace", "(${trim(traceX.toDouble())}, ${trim(traceY.toDouble())})", Green)
            differential?.let { value ->
                Insight("Gradient", "(${trim(value.gradient.x)}, ${trim(value.gradient.y)})", Green)
                Insight("Unit normal", "(${trim(value.unitNormal.x)}, ${trim(value.unitNormal.y)}, ${trim(value.unitNormal.z)})", Amber)
                Insight("Surface point", "(${trim(value.point.x)}, ${trim(value.point.y)}, ${trim(value.point.z)})", Cyan)
                Text("Tangent plane", color = Ink, fontWeight = FontWeight.SemiBold)
                MathFormulaText(SurfaceAnalysisHandleEngine.tangentPlaneEquation(value), color = Violet, fontSize = 11.sp)
            }
            Insight("Slice", "z = ${trim(sliceZ.toDouble())}", Violet)
            Insight("Checks", "z(0,0)=0 · z(1,1)=2", Green)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("3D Graph Controls", vm::hidePanels, Ink)
            AxisSlider("Rotation", rotation, -180f..180f) { rotation = it }
            AxisSlider("Tilt", tilt, -89f..89f) { tilt = it }
            AxisSlider("Roll", roll, -180f..180f) { roll = it }
            AxisSlider("Zoom", zoom, .55f..1.7f) { zoom = it }
            AxisSlider("Mesh", density, 8f..56f) { density = it }
            AxisSlider("Slice z", sliceZ, -4f..6f) { sliceZ = it }
            AxisSlider("Contour level handle", contourLevel, -4f..8f) { contourLevel = it; sliceZ = it }
            AxisSlider("Trace x", traceX, -3f..3f) { traceX = it }
            AxisSlider("Trace y", traceY, -3f..3f) { traceY = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SurfaceTool.entries.forEach { tool ->
                    GlowButton(if (activeTool == tool) "• ${tool.name}" else tool.name, onClick = {
                        activeTool = tool
                        if (tool == SurfaceTool.Wireframe) showWireframe = !showWireframe
                        if (tool == SurfaceTool.Contours) showContours = !showContours
                        if (tool == SurfaceTool.Slice) showSlice = !showSlice
                        if (tool == SurfaceTool.Gradient) showGradient = !showGradient
                        if (tool == SurfaceTool.BoundingBox) showBox = !showBox
                    })
                }
                GlowButton("Zoom +", onClick = { zoom = (zoom + .1f).coerceAtMost(1.7f) })
                GlowButton("Zoom -", onClick = { zoom = (zoom - .1f).coerceAtLeast(.55f) })
                GlowButton("Fit", onClick = { zoom = 1f; applyView(SurfaceViewPreset.Isometric) })
            }
            Text("Gradient path", color = Ink, fontWeight = FontWeight.SemiBold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                fun createGradientPath(ascending: Boolean): List<Vec3> {
                    val path = mutableListOf<Vec3>(); var point = Vec2(traceX.toDouble(), traceY.toDouble())
                    repeat(80) {
                        val value = runCatching { surfaceCalculus.analyze(vm.state.surfaceExpression, point.x, point.y) }.getOrNull() ?: return path
                        path += value.point; val direction = value.gradient; val magnitude = kotlin.math.hypot(direction.x, direction.y)
                        if (magnitude < 1e-9) return path
                        val sign = if (ascending) 1.0 else -1.0; point += Vec2(direction.x / magnitude, direction.y / magnitude) * (.05 * sign)
                    }
                    return path
                }
                GlowButton("Ascent") { gradientPlayback = com.indianservers.aiexplorer.core.GradientPlayback3D(createGradientPath(true), ascending = true).play() }
                GlowButton("Descent") { gradientPlayback = com.indianservers.aiexplorer.core.GradientPlayback3D(createGradientPath(false), ascending = false).play() }
                GlowButton(if (gradientPlayback.playing) "Pause" else "Play", enabled = gradientPlayback.path.isNotEmpty()) { gradientPlayback = if (gradientPlayback.playing) gradientPlayback.pause() else gradientPlayback.play() }
                GlowButton("Edit waypoint", enabled = gradientPlayback.path.isNotEmpty()) {
                    if (gradientPlayback.path.isNotEmpty()) gradientPlayback = gradientPlayback.moveWaypoint(gradientPlayback.index, Vec3(traceX.toDouble(), traceY.toDouble(), differential?.point?.z ?: 0.0))
                }
                GlowButton("Export contour") {
                    val values = mesh?.vertices.orEmpty().filter { abs(it.z - contourLevel) <= .12 }.map { contourLevel.toDouble() to it }
                    copyShapeText(context, "contour.csv", com.indianservers.aiexplorer.core.SpatialExportEngine.contoursCsv(values))
                }
                Text("${gradientPlayback.index + 1}/${gradientPlayback.path.size}", color = Green, fontSize = 10.sp)
            }
        }
    }
}

private enum class TrigLab(val label: String) {
    Circle("Circle + Waves"), Identities("Identity Lab"), Triangle("Triangle Solver"),
    Equations("Equations"), Polar("Polar Curves"), Applications("Heights + Distance"), Harmonics("Fourier Lab")
}
private enum class TrigLineStyle { Solid, Dashed, Dotted }

@Composable
private fun TrigonometryScreen(vm: ExplorerViewModel) {
    var angle by remember { mutableFloatStateOf(45f) }
    var amplitude by remember { mutableFloatStateOf(1f) }
    var period by remember { mutableFloatStateOf((2 * Math.PI).toFloat()) }
    var phase by remember { mutableFloatStateOf(0f) }
    var verticalShift by remember { mutableFloatStateOf(0f) }
    var function by remember { mutableStateOf(TrigFunction.Sine) }
    var visibleFunctions by remember { mutableStateOf(setOf(TrigFunction.Sine)) }
    var angleUnit by remember { mutableStateOf(TrigAngleUnit.Degrees) }
    var lab by remember { mutableStateOf(TrigLab.Circle) }
    var identityIndex by remember { mutableIntStateOf(0) }
    var identityStep by remember { mutableIntStateOf(0) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    var showAsymptotes by remember { mutableStateOf(true) }
    var homeRequest by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var animateAngle by remember { mutableStateOf(false) }
    var rotationDirection by remember { mutableIntStateOf(1) }
    var lineStyle by remember { mutableStateOf(TrigLineStyle.Solid) }
    var paletteShift by remember { mutableIntStateOf(0) }
    var inverseFunction by remember { mutableStateOf(InverseTrigFunction.ArcSine) }
    var inverseInput by remember { mutableFloatStateOf(.5f) }
    var equationTarget by remember { mutableFloatStateOf(.5f) }
    var polarType by remember { mutableStateOf(PolarCurveType.Rose) }
    var polarParameter by remember { mutableFloatStateOf(3f) }
    var triangleA by remember { mutableFloatStateOf(4f) }
    var triangleB by remember { mutableFloatStateOf(5f) }
    var triangleMode by remember { mutableStateOf("SAS") }
    var observationDistance by remember { mutableFloatStateOf(20f) }
    var observerHeight by remember { mutableFloatStateOf(1.6f) }
    var harmonic1 by remember { mutableFloatStateOf(1f) }
    var harmonic2 by remember { mutableFloatStateOf(.5f) }
    var harmonic3 by remember { mutableFloatStateOf(.25f) }

    LaunchedEffect(animateAngle) {
        while (animateAngle) { delay(32); angle += rotationDirection; if (angle > 180f) angle = -180f; if (angle < -180f) angle = 180f }
    }

    val radians = Math.toRadians(angle.toDouble())
    val snapshot = remember(angle) { InteractiveTrigEngine.snapshot(radians) }
    val transform = TrigTransform(amplitude.toDouble(), period.toDouble(), phase.toDouble(), verticalShift.toDouble())
    val identityLab = remember { InteractiveTrigIdentityLab() }
    val identity = remember(identityIndex) { identityLab.verify(identityIndex) }
    val safeAngle = kotlin.math.abs(angle.toDouble()).coerceIn(1.0, 178.0)
    val triangle = remember(triangleA, triangleB, safeAngle, triangleMode) {
        when (triangleMode) {
            "SSS" -> TriangleTrigSolver.sss(triangleA.toDouble(), triangleB.toDouble(), ((triangleA + triangleB) * .72f).toDouble())
            "SSA" -> TriangleTrigSolver.ssa(triangleA.toDouble(), triangleB.toDouble(), safeAngle).firstOrNull()
                ?: TriangleTrigSolver.sas(triangleA.toDouble(), triangleB.toDouble(), safeAngle)
            "ASA" -> TriangleTrigSolver.asa(triangleA.toDouble(), safeAngle.coerceAtMost(120.0), ((180 - safeAngle) / 2).coerceAtLeast(1.0))
            "AAS" -> TriangleTrigSolver.aas(triangleA.toDouble(), safeAngle.coerceAtMost(120.0), ((180 - safeAngle) / 2).coerceAtLeast(1.0))
            else -> TriangleTrigSolver.sas(triangleA.toDouble(), triangleB.toDouble(), safeAngle)
        }
    }
    val displayAngle = InteractiveTrigEngine.fromRadians(radians, angleUnit)
    val inverseRadians = InteractiveTrigEngine.inverse(inverseInput.toDouble(), inverseFunction)
    val roots = remember(function, equationTarget) { InteractiveTrigEngine.equationRoots(function, equationTarget.toDouble(), 0.0, 2 * Math.PI) }
    val polar = remember(polarType, polarParameter) { InteractiveTrigEngine.polarSamples(polarType, polarParameter.toDouble()) }
    val harmonics = listOf(HarmonicComponent(harmonic1.toDouble(), 1), HarmonicComponent(harmonic2.toDouble(), 2), HarmonicComponent(harmonic3.toDouble(), 3))

    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(), angleDegrees = angle, transform = if (lab == TrigLab.Equations) TrigTransform() else transform, function = function,
            showTangents = showTangents, showProjections = showProjections, showWave = showWave,
            homeRequest = homeRequest, onZoomChanged = { zoom = it }, onAngleChange = { angle = snapAngle(it) },
            visibleFunctions = if (lab == TrigLab.Equations) setOf(function) else visibleFunctions, showAsymptotes = showAsymptotes,
            polarSamples = if (lab == TrigLab.Polar) polar else emptyList(),
            harmonics = if (lab == TrigLab.Harmonics) harmonics else emptyList(),
            lineStyle = lineStyle,
            paletteShift = paletteShift,
            equationTarget = equationTarget.takeIf { lab == TrigLab.Equations },
            equationRoots = if (lab == TrigLab.Equations) roots.map { it.radians } else emptyList(),
            onTransformChange = { a, p, h, k -> amplitude = a; period = p; phase = h; verticalShift = k },
        )
        InteractionHint("Drag circle/wave handles - pinch to zoom - two fingers pan - double tap fits", Modifier.align(Alignment.BottomEnd))
        Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("Home") { homeRequest++ }; Text("${trim(zoom.toDouble())}x", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
        FloatingPanelLaunchers(Modifier.align(Alignment.CenterStart), "Trig Labs", "Insights", "Controls", { vm.togglePanel(PanelSlot.Left) }, { vm.togglePanel(PanelSlot.Right) }, { vm.togglePanel(PanelSlot.Bottom) })
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(220.dp)) {
            PanelHeader("Trigonometry Labs", vm::hidePanels, Cyan)
            Text("Seven tactile labs cover the complete toolkit.", color = Muted, fontSize = 12.sp)
            TrigLab.entries.forEach { item -> GlowButton(if (lab == item) "* ${item.label}" else item.label, onClick = { lab = item }) }
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(270.dp)) {
            PanelHeader("Live Insights", vm::hidePanels, Violet)
            Insight("Angle", "${trim(displayAngle.display)}${displayAngle.suffix}", Cyan)
            Insight("Quadrant", "Q${snapshot.quadrant} - reference ${trim(snapshot.referenceAngleDegrees)} deg", Cyan)
            Insight("sin / cos", "${snapshot.exactSine ?: trim(snapshot.sine)} / ${snapshot.exactCosine ?: trim(snapshot.cosine)}", Violet)
            Insight("tan", snapshot.exactTangent ?: snapshot.tangent?.let(::trim) ?: "undefined", Green)
            Insight("Triangle", "c=${trim(triangle.c)}, area=${trim(triangle.area)}", Amber)
            when (lab) {
                TrigLab.Identities -> Insight(identity.label, if (identity.evidence.equivalent) "Verified" else identity.evidence.explanation, Green)
                TrigLab.Equations -> Insight("Roots in [0, 2pi]", roots.joinToString { it.exactLabel ?: "${trim(it.degrees)} deg" }.ifBlank { "None" }, Amber)
                TrigLab.Applications -> Insight("Estimated height", "${trim(InteractiveTrigEngine.heightFromObservation(observationDistance.toDouble(), safeAngle.coerceAtMost(89.0), observerHeight.toDouble()))} units", Amber)
                TrigLab.Polar -> Insight("Polar", "${polarType.name} - ${polar.size} samples", Violet)
                TrigLab.Harmonics -> Insight("Composite", "sum A_n sin(nx + phase)", Violet)
                else -> Unit
            }
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("${lab.label} Controls", vm::hidePanels, Ink)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TrigAngleUnit.entries.forEach { unit -> TogglePill(unit.name, angleUnit == unit) { angleUnit = unit } }
                TogglePill(if (animateAngle) "Pause" else "Animate", animateAngle) { animateAngle = it }
                GlowButton(if (rotationDirection > 0) "CCW +" else "CW -", onClick = { rotationDirection *= -1 })
                listOf(-180f, -90f, 0f, 30f, 45f, 60f, 90f, 180f).forEach { value -> GlowButton("${trim(value.toDouble())} deg", onClick = { angle = value }) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Angle ${trim(displayAngle.display)}${displayAngle.suffix}", color = Ink, modifier = Modifier.width(155.dp)); Slider(angle, { angle = snapAngle(it) }, valueRange = -180f..180f, modifier = Modifier.weight(1f)) }
            when (lab) {
                TrigLab.Circle -> {
                    AxisSlider("Amplitude", amplitude, .25f..3f) { amplitude = it }; AxisSlider("Period", period, 1f..12.57f) { period = it }; AxisSlider("Phase", phase, -3.14f..3.14f) { phase = it }; AxisSlider("Vertical", verticalShift, -2f..2f) { verticalShift = it }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { TrigFunction.entries.forEach { item -> TogglePill(item.name, item in visibleFunctions) { enabled -> visibleFunctions = if (enabled) visibleFunctions + item else (visibleFunctions - item).ifEmpty { setOf(item) }; function = item } }; TogglePill("Asymptotes", showAsymptotes) { showAsymptotes = it }; TrigLineStyle.entries.forEach { style -> TogglePill(style.name, lineStyle == style) { lineStyle = style } }; GlowButton("Cycle colours", onClick = { paletteShift++ }) }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { InverseTrigFunction.entries.forEach { item -> TogglePill(item.name, inverseFunction == item) { inverseFunction = item } } }; AxisSlider("Inverse input", inverseInput, -1f..1f) { inverseInput = it }; Text("Result ${radianLabel(Math.toDegrees(inverseRadians))} rad = ${trim(Math.toDegrees(inverseRadians))} deg", color = Green)
                }
                TrigLab.Identities -> {
                    val steps = listOf("Start: ${identity.left}", "Check domain: ${identity.evidence.leftDomain.description}", "Apply the named identity: ${identity.label}", "Simplify toward ${identity.right}", if (identity.evidence.equivalent) "Verified: both sides agree" else identity.evidence.explanation)
                    Text("${identity.left} = ${identity.right}", color = Ink); Text(steps[identityStep.coerceIn(steps.indices)], color = if (identityStep == steps.lastIndex) Green else Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { GlowButton("Previous step", onClick = { identityStep = (identityStep - 1).coerceAtLeast(0) }); GlowButton("Next step", onClick = { identityStep = (identityStep + 1).coerceAtMost(steps.lastIndex) }); GlowButton("Next identity", onClick = { identityIndex = (identityIndex + 1) % identityLab.catalog.size; identityStep = 0 }) }
                }
                TrigLab.Triangle -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("SSS", "SAS", "SSA", "ASA", "AAS").forEach { item -> TogglePill(item, triangleMode == item) { triangleMode = item } } }; AxisSlider("Side a", triangleA, 1f..12f) { triangleA = it }; AxisSlider("Side b", triangleB, 1f..12f) { triangleB = it }; Text("a=${trim(triangle.a)}, b=${trim(triangle.b)}, c=${trim(triangle.c)} - A=${trim(triangle.angleA)} deg, B=${trim(triangle.angleB)} deg, C=${trim(triangle.angleC)} deg", color = Green); if (triangleMode == "SSA") Text("Ambiguous SSA: ${TriangleTrigSolver.ssa(triangleA.toDouble(), triangleB.toDouble(), safeAngle).size} valid solution(s)", color = Amber); Text("Sine and cosine rules update with the construction.", color = Muted) }
                TrigLab.Equations -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { TrigFunction.entries.forEach { item -> TogglePill(item.name, function == item) { function = item } } }; AxisSlider("Equation target", equationTarget, -2f..2f) { equationTarget = it }; Text("${function.name}(x) = ${trim(equationTarget.toDouble())}: ${roots.joinToString { it.exactLabel ?: "${trim(it.degrees)} deg" }}", color = Green) }
                TrigLab.Polar -> { FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { PolarCurveType.entries.forEach { item -> TogglePill(item.name, polarType == item) { polarType = item } } }; AxisSlider("Polar parameter", polarParameter, .25f..8f) { polarParameter = it } }
                TrigLab.Applications -> { AxisSlider("Ground distance", observationDistance, 1f..100f) { observationDistance = it }; AxisSlider("Observer height", observerHeight, 0f..3f) { observerHeight = it }; Text("height = observer height + distance x tan(elevation)", color = Muted) }
                TrigLab.Harmonics -> { AxisSlider("Fundamental A1", harmonic1, 0f..2f) { harmonic1 = it }; AxisSlider("Second A2", harmonic2, 0f..2f) { harmonic2 = it }; AxisSlider("Third A3", harmonic3, 0f..2f) { harmonic3 = it } }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { TogglePill("Tangents", showTangents) { showTangents = it }; TogglePill("Projections", showProjections) { showProjections = it }; TogglePill("Wave", showWave) { showWave = it }; GlowButton("Close", onClick = vm::hidePanels) }
        }
    }
}

@Composable
private fun LegacyTrigonometryScreen(vm: ExplorerViewModel) {
    var angle by remember { mutableFloatStateOf(45f) }
    var amplitude by remember { mutableFloatStateOf(1f) }
    var period by remember { mutableFloatStateOf((2 * Math.PI).toFloat()) }
    var phase by remember { mutableFloatStateOf(0f) }
    var verticalShift by remember { mutableFloatStateOf(0f) }
    var function by remember { mutableStateOf(TrigFunction.Sine) }
    var identityIndex by remember { mutableIntStateOf(0) }
    var showTangents by remember { mutableStateOf(true) }
    var showProjections by remember { mutableStateOf(true) }
    var showWave by remember { mutableStateOf(true) }
    var trigHomeRequest by remember { mutableIntStateOf(0) }
    var trigZoom by remember { mutableFloatStateOf(1f) }
    val radians = Math.toRadians(angle.toDouble())
    val snapshot = remember(angle) { InteractiveTrigEngine.snapshot(radians) }
    val transform = TrigTransform(amplitude.toDouble(), period.toDouble(), phase.toDouble(), verticalShift.toDouble())
    val identityLab = remember { InteractiveTrigIdentityLab() }
    val identity = remember(identityIndex) { identityLab.verify(identityIndex) }
    val triangle = remember(angle) { TriangleTrigSolver.sas(4.0, 5.0, angle.toDouble().let { kotlin.math.abs(it).coerceIn(1.0, 179.0) }) }
    Box(Modifier.fillMaxSize()) {
        TrigCanvas(
            modifier = Modifier.fillMaxSize(),
            angleDegrees = angle,
            transform = transform,
            function = function,
            showTangents = showTangents,
            showProjections = showProjections,
            showWave = showWave,
            homeRequest = trigHomeRequest,
            onZoomChanged = { trigZoom = it },
            onAngleChange = { angle = snapAngle(it) },
        )
        InteractionHint(
            "One finger moves the circle point or wave cursor · two fingers pan and pinch zoom · double-tap resets",
            Modifier.align(Alignment.BottomEnd),
        )
        Row(Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlowButton("⌂ Home") { trigHomeRequest++ }
            Text("${trim(trigZoom.toDouble())}×", color = Muted, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceA).padding(9.dp))
        }
        FloatingPanelLaunchers(
            modifier = Modifier.align(Alignment.CenterStart),
            leftLabel = "Trig Tools",
            rightLabel = "Insights",
            bottomLabel = "Controls",
            onLeft = { vm.togglePanel(PanelSlot.Left) },
            onRight = { vm.togglePanel(PanelSlot.Right) },
            onBottom = { vm.togglePanel(PanelSlot.Bottom) },
        )
        if (vm.showLeftPanel) GlassPanel(Modifier.align(Alignment.TopStart).width(210.dp)) {
            PanelHeader("Trig Tools", vm::hidePanels, Cyan)
            Text("Drag the circle point or wave cursor.", color = Muted, fontSize = 12.sp)
            TrigFunction.entries.forEach { value -> GlowButton(if (function == value) "• ${value.name}" else value.name, onClick = { function = value; showWave = true }) }
            GlowButton("Next identity", onClick = { identityIndex = (identityIndex + 1) % identityLab.catalog.size })
        }
        if (vm.showRightPanel) GlassPanel(Modifier.align(Alignment.TopEnd).width(250.dp)) {
            PanelHeader("Angle Insights", vm::hidePanels, Violet)
            Insight("Angle", "${trim(snapshot.degrees)} deg", Cyan)
            Insight("Radians", "${radianLabel(angle.toDouble())} rad", Violet)
            Insight("Quadrant", "Q${snapshot.quadrant} · ref ${trim(snapshot.referenceAngleDegrees)}°", Cyan)
            Insight("sin θ", snapshot.exactSine ?: trim(snapshot.sine), Violet)
            Insight("cos θ", snapshot.exactCosine ?: trim(snapshot.cosine), Cyan)
            Insight("tan θ", snapshot.exactTangent ?: snapshot.tangent?.let(::trim) ?: "undefined", Green)
            Insight("Triangle", "c=${trim(triangle.c)}, area=${trim(triangle.area)}", Amber)
            Insight(identity.label, if (identity.evidence.equivalent) "Verified · residual 0" else identity.evidence.explanation, Green)
            Text("Assumptions: ${identity.evidence.leftDomain.description}", color = Muted, fontSize = 11.sp)
        }
        if (vm.showBottomPanel) GlassPanel(Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            PanelHeader("Trigonometry Controls", vm::hidePanels, Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Angle ${trim(angle.toDouble())} deg", color = Ink, modifier = Modifier.width(126.dp))
                Slider(value = angle, onValueChange = { angle = snapAngle(it) }, valueRange = -180f..180f, modifier = Modifier.weight(1f))
            }
            AxisSlider("Amplitude", amplitude, .25f..3f) { amplitude = it }
            AxisSlider("Period", period, 1f..12.57f) { period = it }
            AxisSlider("Phase", phase, -3.14f..3.14f) { phase = it }
            AxisSlider("Vertical", verticalShift, -2f..2f) { verticalShift = it }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(-180f, -90f, 0f, 30f, 45f, 60f, 90f, 180f).forEach {
                    GlowButton("${trim(it.toDouble())} deg", onClick = { angle = it })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TogglePill("Tangents", showTangents) { showTangents = it }
                TogglePill("Projections", showProjections) { showProjections = it }
                TogglePill("Sine wave", showWave) { showWave = it }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlowButton("sin theta", onClick = { showWave = true })
                GlowButton("cos theta", onClick = { showProjections = true })
                GlowButton("tan theta", onClick = { showTangents = true })
                GlowButton("Close Panels", onClick = vm::hidePanels)
            }
        }
    }
}

@Composable
private fun SurfaceExampleChips(modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val examples = listOf(
        "Paraboloid" to "x^2 + y^2",
        "Saddle" to "x^2 - y^2",
        "Wave" to "sin(x) + cos(y)",
        "Plane" to "0.5*x + y",
        "Cone" to "sqrt(x^2 + y^2)",
        "Ripple" to "sin(x^2 + y^2)",
    )
    FlowRow(
        modifier.padding(top = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        examples.forEachIndexed { index, (label, expression) ->
            Text(
                text = label,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (index % 2 == 0) Color(0x5520D9FF) else Color(0x55985DFF))
                    .border(1.dp, if (index % 2 == 0) Cyan else Violet, RoundedCornerShape(16.dp))
                    .clickable { onSelect(expression) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun SurfaceViewChips(
    modifier: Modifier = Modifier,
    active: SurfaceViewPreset,
    onSelect: (SurfaceViewPreset) -> Unit,
) {
    FlowRow(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .92f))
            .border(1.dp, Cyan.copy(.45f), RoundedCornerShape(18.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SurfaceViewPreset.entries.forEach { preset ->
            Text(
                text = if (preset == active) "• ${preset.name}" else preset.name,
                color = if (preset == active) Color.White else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (preset == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(preset) }
                    .semantics { contentDescription = "View 3D graph from ${preset.name} axis or plane" }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun CoordinateCanvas(
    modifier: Modifier,
    points: List<Vec2>,
    shapes: List<Shape2D>,
    interactionEnabled: Boolean,
    selectedShapes: Set<Int>,
    snapEnabled: Boolean,
    axisConstraint: AxisConstraint,
    precisionMode: Boolean,
    lassoEnabled: Boolean,
    boxSelectEnabled: Boolean,
    homeRequest: Int,
    undoViewRequest: Int,
    onPointDragStart: (Int) -> Unit,
    onPointDrag: (Int, Vec2) -> Unit,
    onShapeDragStart: (Int) -> Unit,
    onShapeDrag: (Vec2) -> Unit,
    onShapeRotate: (Double) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDropDelete: () -> Unit,
    onCanvasTap: (Vec2, Int?) -> Unit,
    onClearSelection: () -> Unit,
    onLassoSelection: (Set<Int>) -> Unit,
    onObjectLongPress: (shapeIndex: Int?, pointIndex: Int?, position: Vec2) -> Unit,
    onKeyboardMove: (dx: Int, dy: Int, precision: Boolean) -> Unit,
    onKeyboardCycle: (backwards: Boolean) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.(toScreen: (Vec2) -> Offset) -> Unit,
) {
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    var lassoWorld by remember { mutableStateOf<List<Vec2>>(emptyList()) }
    var boxStartWorld by remember { mutableStateOf<Vec2?>(null) }
    var boxCurrentWorld by remember { mutableStateOf<Vec2?>(null) }
    var snapGuides by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.SnapGuide>>(emptyList()) }
    var coordinateTooltip by remember { mutableStateOf<Vec2?>(null) }
    var viewportUndo by remember { mutableStateOf<List<com.indianservers.aiexplorer.core.Viewport2D>>(emptyList()) }
    val currentPoints by rememberUpdatedState(points)
    val currentShapes by rememberUpdatedState(shapes)
    LaunchedEffect(homeRequest, canvasWidth, canvasHeight, points) {
        if (homeRequest > 0 && canvasWidth > 0 && canvasHeight > 0) {
            viewportUndo = viewportUndo + com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
            val fit = InteractionGeometry.fit(points, canvasWidth.toDouble() / canvasHeight)
            cameraCenter = fit.center
            cameraZoom = fit.zoom
        }
    }
    LaunchedEffect(undoViewRequest) {
        if (undoViewRequest > 0 && viewportUndo.isNotEmpty()) {
            val previous = viewportUndo.last()
            viewportUndo = viewportUndo.dropLast(1)
            cameraCenter = previous.center
            cameraZoom = previous.zoom
        }
    }
    Canvas(
        modifier
            .onSizeChanged { canvasWidth = it.width; canvasHeight = it.height }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) false else when (event.key) {
                    Key.DirectionLeft -> { onKeyboardMove(-1, 0, event.isShiftPressed); true }
                    Key.DirectionRight -> { onKeyboardMove(1, 0, event.isShiftPressed); true }
                    Key.DirectionUp -> { onKeyboardMove(0, 1, event.isShiftPressed); true }
                    Key.DirectionDown -> { onKeyboardMove(0, -1, event.isShiftPressed); true }
                    Key.Tab -> { onKeyboardCycle(event.isShiftPressed); true }
                    Key.Delete, Key.Backspace -> { onDropDelete(); true }
                    else -> false
                }
            }
            .focusable()
            .pointerInput(interactionEnabled) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val viewportFrom = com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
                    val baseScale = size.width / 14f
                    fun scale() = baseScale * cameraZoom
                    fun origin() = Offset(
                        size.width / 2f - cameraCenter.x.toFloat() * scale(),
                        size.height / 2f + cameraCenter.y.toFloat() * scale(),
                    )
                    fun world(screen: Offset): Vec2 {
                        val o = origin()
                        return Vec2(((screen.x - o.x) / scale()).toDouble(), ((o.y - screen.y) / scale()).toDouble())
                    }
                    fun screen(point: Vec2): Offset {
                        val o = origin()
                        return Offset(o.x + point.x.toFloat() * scale(), o.y - point.y.toFloat() * scale())
                    }

                    val startWorld = world(down.position)
                    val gesturePoints = currentPoints
                    val gestureShapes = currentShapes
                    val selectionPoints = selectedShapes.flatMap { index -> gestureShapes.getOrNull(index)?.pointIndices.orEmpty() }.distinct().mapNotNull(gesturePoints::getOrNull)
                    val selectionBounds = InteractionGeometry.bounds(selectionPoints)
                    val rotationHandle = selectionBounds?.let { bounds -> screen(Vec2(bounds.center.x, bounds.maximum.y + 1.0)) }
                    val selectingRegion = lassoEnabled || boxSelectEnabled
                    val rotating = interactionEnabled && !selectingRegion && rotationHandle != null && (rotationHandle - down.position).getDistance() <= 34f
                    val tappedPointIndex = gesturePoints.indices
                        .minByOrNull { (screen(gesturePoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(gesturePoints[it]) - down.position).getDistance() <= 38f }
                    var pointIndex: Int? = null
                    var shapeIndex: Int? = null
                    if (interactionEnabled && !selectingRegion) {
                        pointIndex = tappedPointIndex
                        if (pointIndex == null && !rotating) {
                            shapeIndex = gestureShapes.indices.filter { gestureShapes[it].visible }
                                .minByOrNull { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) }
                                ?.takeIf { shapeScreenDistance(gestureShapes[it], gesturePoints, down.position, ::screen) <= 42f }
                        }
                        pointIndex?.let(onPointDragStart)
                        shapeIndex?.let(onShapeDragStart)
                        if (rotating) selectedShapes.lastOrNull()?.let(onShapeDragStart)
                    }
                    val gestureTarget = when {
                        pointIndex != null -> GeometryGestureTarget.JunctionHandle
                        shapeIndex != null || rotating -> GeometryGestureTarget.ShapeBody
                        else -> GeometryGestureTarget.EmptyCanvas
                    }
                    if (lassoEnabled) { lassoWorld = listOf(startWorld); gestureMode = GestureMode.Lasso }
                    else if (boxSelectEnabled) { boxStartWorld = startWorld; boxCurrentWorld = startWorld; gestureMode = GestureMode.Lasso }
                    else gestureMode = when { rotating -> GestureMode.Rotating; pointIndex != null -> GestureMode.Resizing; shapeIndex != null -> GestureMode.Moving; else -> GestureMode.Selecting }

                    var moved = false
                    var transformed = false
                    var latestPosition = down.position
                    var latestUptime = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { latestPosition = it.position; latestUptime = it.uptimeMillis }
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (GeometryGesturePolicy.allowsViewportTransform(gestureTarget)) {
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid()
                                val oldScale = scale()
                                cameraCenter = Vec2(cameraCenter.x - pan.x / oldScale, cameraCenter.y + pan.y / oldScale)
                                val beforeZoom = world(centroid)
                                cameraZoom = GraphViewport.zoom(cameraZoom, event.calculateZoom())
                                val afterZoom = world(centroid)
                                cameraCenter += beforeZoom - afterZoom
                                transformed = true
                                gestureMode = if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning
                            }
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                val currentWorld = world(change!!.position)
                                when {
                                    lassoEnabled -> lassoWorld = lassoWorld + currentWorld
                                    boxSelectEnabled -> boxCurrentWorld = currentWorld
                                    rotating && selectionBounds != null -> onShapeRotate(InteractionGeometry.rotationDegrees(selectionBounds.center, startWorld, currentWorld))
                                    pointIndex != null -> {
                                        val original = gesturePoints[pointIndex]
                                        val handleHeld = change.uptimeMillis - down.uptimeMillis >= 450L
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - original, axisConstraint), precisionMode || handleHeld)
                                        val proposed = original + constrained
                                        val centers = gestureShapes.filter { it.visible }.mapNotNull { shape -> shape.pointIndices.mapNotNull(gesturePoints::getOrNull).takeIf { it.isNotEmpty() }?.let { values -> InteractionGeometry.bounds(values)?.center } }
                                        val segments = gestureShapes.filter { it.visible }.flatMap { shape -> shape.pointIndices.mapNotNull(gesturePoints::getOrNull).zipWithNext() }
                                        val intersections = InteractionGeometry.segmentIntersections(segments)
                                        val remainingPoints = gesturePoints.filterIndexed { index, _ -> index != pointIndex }
                                        val tangentOrigin = gestureShapes.firstNotNullOfOrNull { shape ->
                                            if (pointIndex !in shape.pointIndices || shape.pointIndices.size < 2) null
                                            else shape.pointIndices.firstOrNull { it != pointIndex }?.let(gesturePoints::getOrNull)
                                        }
                                        val tangents = tangentOrigin?.let { origin ->
                                            gestureShapes.filter { it.visible && it.type in setOf(Shape2DType.Circle, Shape2DType.CircleThreePoints) }.flatMap { circle ->
                                                val circlePoints = circle.pointIndices.mapNotNull(gesturePoints::getOrNull)
                                                val center = circlePoints.firstOrNull() ?: return@flatMap emptyList()
                                                val radius = circlePoints.getOrNull(1)?.let { it.distanceTo(center) } ?: return@flatMap emptyList()
                                                InteractionGeometry.tangentPoints(origin, center, radius)
                                            }
                                        }.orEmpty()
                                        val snapped = if (snapEnabled) SmartSnapEngine.snap(
                                            proposed,
                                            remainingPoints,
                                            centers = centers,
                                            intersections = intersections,
                                            tangents = tangents,
                                            equalSpacing = InteractionGeometry.equalSpacingCandidates(remainingPoints.take(16)),
                                        ) else com.indianservers.aiexplorer.core.SnapResult(proposed, emptyList())
                                        snapGuides = snapped.guides
                                        coordinateTooltip = snapped.point
                                        onPointDrag(pointIndex, snapped.point)
                                    }
                                    shapeIndex != null -> {
                                        val constrained = PrecisionInteraction.apply(SmartSnapEngine.constrain(currentWorld - startWorld, axisConstraint), precisionMode)
                                        snapGuides = emptyList()
                                        coordinateTooltip = selectionBounds?.center?.plus(constrained)
                                        onShapeDrag(constrained)
                                    }
                                    interactionEnabled -> {
                                        cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                        gestureMode = GestureMode.Panning
                                    }
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    val longPressedObject = !moved && !transformed && latestUptime - down.uptimeMillis >= 500L && (pointIndex != null || shapeIndex != null)
                    when {
                        longPressedObject -> {
                            onDragCancel()
                            onObjectLongPress(shapeIndex, pointIndex, startWorld)
                        }
                        lassoEnabled -> {
                            val selected = gestureShapes.indices.filterTo(linkedSetOf()) { index ->
                                val shapePoints = gestureShapes[index].pointIndices.mapNotNull(gesturePoints::getOrNull)
                                shapePoints.isNotEmpty() && InteractionGeometry.pointInPolygon(InteractionGeometry.bounds(shapePoints)?.center ?: shapePoints.first(), lassoWorld)
                            }
                            onLassoSelection(selected)
                        }
                        boxSelectEnabled -> {
                            val end = boxCurrentWorld ?: startWorld
                            onLassoSelection(com.indianservers.aiexplorer.workspace.Geometry2DDirectManipulation.boxSelect(
                                com.indianservers.aiexplorer.workspace.WorkspaceState(points = gesturePoints, shapes = gestureShapes, functions = emptyList(), solids = emptyList(), vectors3D = emptyList()),
                                startWorld,
                                end,
                            ))
                        }
                        pointIndex != null || shapeIndex != null || rotating -> {
                            val overDelete = latestPosition.x in (size.width * .32f)..(size.width * .68f) && latestPosition.y >= size.height * .78f
                            if (overDelete && (pointIndex != null || shapeIndex != null)) {
                                onDragCancel()
                                onDropDelete()
                            } else onDragEnd()
                        }
                        !moved && !transformed -> {
                            val now = System.currentTimeMillis()
                            if (interactionEnabled && now - lastTapAt < 320L) {
                                val fit = InteractionGeometry.fit(gesturePoints, size.width.toDouble() / size.height)
                                cameraCenter = fit.center
                                cameraZoom = fit.zoom
                                lastTapAt = 0L
                            } else {
                                if (interactionEnabled && tappedPointIndex == null && shapeIndex == null) onClearSelection()
                                else onCanvasTap(startWorld, tappedPointIndex)
                                lastTapAt = if (interactionEnabled) now else 0L
                            }
                        }
                    }
                    gestureMode = GestureMode.Idle
                    val viewportTo = com.indianservers.aiexplorer.core.Viewport2D(cameraCenter, cameraZoom)
                    if (viewportTo != viewportFrom && (transformed || (moved && gestureTarget == GeometryGestureTarget.EmptyCanvas))) viewportUndo = viewportUndo + viewportFrom
                    snapGuides = emptyList()
                    coordinateTooltip = null
                    lassoWorld = emptyList()
                    boxStartWorld = null
                    boxCurrentWorld = null
                }
            },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = { Offset(origin.x + it.x.toFloat() * scale, origin.y - it.y.toFloat() * scale) }
        drawGrid(origin, scale)
        content(tx)
        snapGuides.forEach { guide ->
            if (guide.axis == AxisConstraint.X) {
                val x = tx(Vec2(guide.value, 0.0)).x
                drawLine(Green.copy(.8f), Offset(x, 0f), Offset(x, size.height), 2f)
                drawTrigText(guide.label, x + 7f, 118f, Green)
            } else if (guide.axis == AxisConstraint.Y) {
                val y = tx(Vec2(0.0, guide.value)).y
                drawLine(Green.copy(.8f), Offset(0f, y), Offset(size.width, y), 2f)
                drawTrigText(guide.label, 8f, y - 7f, Green)
            }
        }
        if (lassoWorld.size >= 2) {
            val path = Path().apply {
                val first = tx(lassoWorld.first()); moveTo(first.x, first.y)
                lassoWorld.drop(1).forEach { point -> val p = tx(point); lineTo(p.x, p.y) }
            }
            drawPath(path, Cyan, style = Stroke(3f, cap = StrokeCap.Round))
        }
        val boxStart = boxStartWorld; val boxEnd = boxCurrentWorld
        if (boxStart != null && boxEnd != null) {
            val first = tx(boxStart); val second = tx(boxEnd)
            drawRect(Cyan.copy(.16f), Offset(min(first.x, second.x), min(first.y, second.y)), Size(abs(second.x - first.x), abs(second.y - first.y)))
            drawRect(Cyan, Offset(min(first.x, second.x), min(first.y, second.y)), Size(abs(second.x - first.x), abs(second.y - first.y)), style = Stroke(2.5f))
        }
        val selectedWorld = selectedShapes.flatMap { index -> shapes.getOrNull(index)?.pointIndices.orEmpty() }.distinct().mapNotNull(points::getOrNull)
        InteractionGeometry.bounds(selectedWorld)?.let { bounds ->
            val topLeft = tx(Vec2(bounds.minimum.x, bounds.maximum.y))
            val bottomRight = tx(Vec2(bounds.maximum.x, bounds.minimum.y))
            val boxTopLeft = Offset(min(topLeft.x, bottomRight.x), min(topLeft.y, bottomRight.y))
            val boxSize = Size(abs(bottomRight.x - topLeft.x).coerceAtLeast(2f), abs(bottomRight.y - topLeft.y).coerceAtLeast(2f))
            drawRect(Amber.copy(.9f), boxTopLeft, boxSize, style = Stroke(2.5f))
            val center = tx(bounds.center)
            val rotateHandle = tx(Vec2(bounds.center.x, bounds.maximum.y + 1.0))
            drawLine(Amber, Offset(center.x, boxTopLeft.y), rotateHandle, 2f)
            drawCircle(Amber.copy(.22f), 20f, rotateHandle)
            drawCircle(Amber, 7f, rotateHandle)
            drawCircle(Green.copy(.25f), 18f, center)
            drawLine(Green, center - Offset(10f, 0f), center + Offset(10f, 0f), 2f)
            drawLine(Green, center - Offset(0f, 10f), center + Offset(0f, 10f), 2f)
            val measure = if (selectedWorld.size >= 2) {
                val segment = Geometry2D.segment(selectedWorld.first(), selectedWorld.last())
                "Δx ${trim(bounds.width)} · Δy ${trim(bounds.height)} · length ${trim(segment.distance)} · slope ${segment.slope?.let(::trim) ?: "∞"}"
            } else "(${trim(bounds.center.x)}, ${trim(bounds.center.y)})"
            drawGraphLabel(measure, boxTopLeft + Offset(8f, -48f), Amber)
        }
        coordinateTooltip?.let { point -> drawGraphLabel("(${trim(point.x)}, ${trim(point.y)})", tx(point) + Offset(18f, -54f), Green) }
        if (gestureMode != GestureMode.Idle) drawGraphLabel(gestureMode.label, Offset(size.width / 2f - 95f, 92f), Cyan)
        drawGraphLabel("${trim(cameraZoom.toDouble())}× · ${trim((size.width / scale).toDouble())} units wide", Offset(size.width - 205f, 92f), Muted)
    }
}

private fun shapeScreenDistance(shape: Shape2D, points: List<Vec2>, target: Offset, screen: (Vec2) -> Offset): Float {
    val worldPoints = shape.pointIndices.mapNotNull(points::getOrNull)
    val vertices = worldPoints.map(screen)
    if (vertices.isEmpty()) return Float.MAX_VALUE
    if (shape.type in setOf(Shape2DType.Circle, Shape2DType.Arc) && vertices.size >= 2) {
        val radius = (vertices[1] - vertices[0]).getDistance()
        val centerDistance = (target - vertices[0]).getDistance()
        return if (shape.type == Shape2DType.Circle && centerDistance <= radius) 0f else abs(centerDistance - radius)
    }
    if (shape.type == Shape2DType.CircleThreePoints && worldPoints.size >= 3) {
        val centerWorld = Geometry2D.circumcenter(worldPoints[0], worldPoints[1], worldPoints[2]) ?: return Float.MAX_VALUE
        val center = screen(centerWorld)
        val radius = (vertices[0] - center).getDistance()
        val distance = (target - center).getDistance()
        return if (distance <= radius) 0f else distance - radius
    }
    if (shape.type == Shape2DType.Ellipse && vertices.size >= 3) {
        val center = vertices[0]
        val rx = (vertices[1] - center).getDistance().coerceAtLeast(1f)
        val ry = (vertices[2] - center).getDistance().coerceAtLeast(1f)
        val normalized = kotlin.math.sqrt(((target.x - center.x) / rx).let { it * it } + ((target.y - center.y) / ry).let { it * it })
        return if (normalized <= 1f) 0f else (normalized - 1f) * min(rx, ry)
    }
    if (shape.type in setOf(Shape2DType.Parallel, Shape2DType.Perpendicular) && worldPoints.size >= 3) {
        val base = worldPoints[1] - worldPoints[0]
        val direction = if (shape.type == Shape2DType.Parallel) base else Vec2(-base.y, base.x)
        val a = screen(worldPoints[2] - direction * 100.0)
        val b = screen(worldPoints[2] + direction * 100.0)
        return pointSegmentDistance(target, a, b)
    }
    if (shape.type == Shape2DType.AngleBisector && worldPoints.size >= 3) {
        val u = worldPoints[0] - worldPoints[1]
        val v = worldPoints[2] - worldPoints[1]
        val direction = u * (1.0 / u.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)) +
            v * (1.0 / v.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9))
        return pointSegmentDistance(target, screen(worldPoints[1]), screen(worldPoints[1] + direction * 100.0))
    }
    val displayVertices = when {
        shape.type in setOf(Shape2DType.Rectangle, Shape2DType.Square) && vertices.size >= 2 -> {
            val a = vertices[0]; val b = vertices[1]
            val height = if (shape.type == Shape2DType.Square) b.x - a.x else b.y - a.y
            listOf(a, Offset(b.x, a.y), Offset(b.x, a.y + height), Offset(a.x, a.y + height))
        }
        shape.type == Shape2DType.RegularPolygon && worldPoints.size >= 2 -> {
            val center = worldPoints[0]
            val radiusVector = worldPoints[1] - center
            val startAngle = kotlin.math.atan2(radiusVector.y, radiusVector.x)
            val radius = radiusVector.distanceTo(Vec2(0.0, 0.0))
            (0 until 5).map { i -> screen(Vec2(center.x + cos(startAngle + i * 2.0 * PI / 5.0) * radius, center.y + sin(startAngle + i * 2.0 * PI / 5.0) * radius)) }
        }
        else -> vertices
    }
    val closed = shape.type in setOf(Shape2DType.Triangle, Shape2DType.Polygon, Shape2DType.Rectangle, Shape2DType.Square, Shape2DType.RegularPolygon)
    if (closed && displayVertices.size >= 3 && pointInsidePolygon(target, displayVertices)) return 0f
    val edges = when {
        displayVertices.size == 1 -> emptyList()
        closed -> displayVertices.indices.map { displayVertices[it] to displayVertices[(it + 1) % displayVertices.size] }
        else -> displayVertices.zipWithNext()
    }
    return edges.minOfOrNull { (a, b) -> pointSegmentDistance(target, a, b) }
        ?: (displayVertices.first() - target).getDistance()
}

private fun pointInsidePolygon(point: Offset, vertices: List<Offset>): Boolean {
    var inside = false
    var previous = vertices.lastIndex
    vertices.indices.forEach { current ->
        val a = vertices[current]
        val b = vertices[previous]
        if ((a.y > point.y) != (b.y > point.y)) {
            val crossingX = (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x
            if (point.x < crossingX) inside = !inside
        }
        previous = current
    }
    return inside
}

private fun pointSegmentDistance(point: Offset, start: Offset, end: Offset): Float {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    if (lengthSquared <= 1e-6f) return (point - start).getDistance()
    val t = (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot(point.x - (start.x + t * dx), point.y - (start.y + t * dy))
}

@Composable
private fun SolidTrackballPalette(
    modifier: Modifier = Modifier,
    selectedSolid: Solid?,
    transformMode: Transform3DMode,
    selectionMode: Selection3DMode,
    onAdd: (SolidType) -> Unit,
    onAddVector: () -> Unit,
    onTransformMode: (Transform3DMode) -> Unit,
    onSelectionMode: (Selection3DMode) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var category by remember { mutableIntStateOf(0) }
    val groups = listOf(
        "Basic" to listOf(SolidType.Cube, SolidType.Cuboid, SolidType.Sphere, SolidType.Hemisphere),
        "Round" to listOf(SolidType.Cylinder, SolidType.Cone, SolidType.Frustum, SolidType.Torus),
        "Prisms" to listOf(SolidType.TriangularPrism, SolidType.PentagonalPrism, SolidType.HexagonalPrism, SolidType.Pyramid),
        "Advanced" to listOf(SolidType.Tetrahedron, SolidType.Octahedron, SolidType.Ellipsoid, SolidType.Paraboloid),
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(58.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xCC30D9FF), Color(0xCC6C48FF), SurfaceA)))
                .border(1.dp, Cyan.copy(.75f), androidx.compose.foundation.shape.CircleShape)
                .clickable { expanded = !expanded }
                .semantics { contentDescription = if (expanded) "Close 3D trackball tools" else "Open 3D trackball tools" },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (expanded) "X" else "3D", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        AnimatedVisibility(expanded) {
            GlassPanel(Modifier.width(318.dp).padding(top = 6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("3D trackball", color = Ink, fontWeight = FontWeight.Bold)
                    Text("X", color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { expanded = false }.padding(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    groups.forEachIndexed { index, group ->
                        GlowButton(if (category == index) "• ${group.first}" else group.first) { category = index }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups[category].second.forEach { type -> GlowButton("+ ${type.name}") { onAdd(type) } }
                    GlowButton("+ Vector", onClick = onAddVector)
                }
                Text("Drag object: ${transformMode.name.lowercase()}", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Transform3DMode.entries.forEach { mode -> GlowButton(if (mode == transformMode) "• ${mode.name}" else mode.name) { onTransformMode(mode) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Selection3DMode.entries.forEach { mode -> GlowButton(if (mode == selectionMode) "• ${mode.name}" else mode.name) { onSelectionMode(mode) } }
                }
                selectedSolid?.let { solid ->
                    Text(solid.type.name, color = Cyan, fontWeight = FontWeight.Bold)
                    Text(Geometry3D.formula(solid.type), color = Ink, fontSize = 12.sp, maxLines = 2)
                    Text("Drag body to move; choose Scale then drag for proportional resize. Vertex/edge/face modes expose multiple control points.", color = Muted, fontSize = 10.sp, maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun SmartSelectionHud(
    title: String,
    instruction: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Column(
        modifier
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SurfaceA.copy(.96f), SurfaceB.copy(.94f))))
            .border(1.dp, Cyan.copy(alpha = .48f), RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = "Selected object controls for $title" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Text("Direct manipulation", color = Green, fontSize = 10.sp)
        }
        Text(instruction, color = Muted, fontSize = 10.sp, maxLines = 2)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { actions() }
    }
}

@Composable
private fun DeleteDropTarget(enabled: Boolean, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Color(0xCC351521) else SurfaceA.copy(alpha = .55f))
            .border(1.dp, if (enabled) Color(0xFFFF6688) else Muted.copy(.35f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onDelete)
            .semantics { contentDescription = "Delete selected object or drag object here" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.padding(top = 7.dp).size(22.dp)) {
            val color = if (enabled) Color(0xFFFF6688) else Muted
            drawRoundRect(color, Offset(size.width * .27f, size.height * .30f), Size(size.width * .46f, size.height * .55f), cornerRadius = CornerRadius(3f, 3f), style = Stroke(2.5f))
            drawLine(color, Offset(size.width * .20f, size.height * .23f), Offset(size.width * .80f, size.height * .23f), 2.5f)
            drawLine(color, Offset(size.width * .40f, size.height * .13f), Offset(size.width * .60f, size.height * .13f), 2.5f)
        }
        Text(if (enabled) "Delete" else "Select", color = if (enabled) Color(0xFFFF6688) else Muted, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun SolidAddStrip(modifier: Modifier = Modifier, onAdd: (SolidType) -> Unit, onAddVector: () -> Unit) {
    FlowRow(
        modifier.padding(top = 132.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SolidType.entries.forEach { type ->
            Text(
                text = type.name,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x33101824))
                    .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(16.dp))
                    .clickable { onAdd(type) }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            )
        }
        Text(
            text = "Vector",
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x55FFC857))
                .border(1.dp, Amber, RoundedCornerShape(16.dp))
                .clickable { onAddVector() }
                .padding(horizontal = 11.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun Transform3DChips(modifier: Modifier = Modifier, active: Transform3DMode, onSelect: (Transform3DMode) -> Unit) {
    Row(
        modifier = modifier
            .padding(bottom = 108.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceA)
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(22.dp))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Transform3DMode.entries.forEach { mode ->
            Text(
                text = mode.name,
                color = if (mode == active) Ink else Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (mode == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun Selection3DChips(modifier: Modifier = Modifier, active: Selection3DMode, onSelect: (Selection3DMode) -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA)
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(18.dp))
            .padding(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Selection3DMode.entries.forEach { mode ->
            Text(
                text = mode.name,
                color = if (mode == active) Ink else Muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (mode == active) Color(0x6630D9FF) else Color.Transparent)
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun InteractionHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Muted,
        fontSize = 11.sp,
        modifier = modifier
            .padding(end = 16.dp, bottom = 68.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceB)
            .border(1.dp, Color(0x3348BFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun OrientationCube(modifier: Modifier = Modifier, onPreset: (String) -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(SurfaceA.copy(.94f)).border(1.dp, Violet.copy(.5f), RoundedCornerShape(14.dp)).padding(6.dp)
            .semantics { contentDescription = "Interactive 3D orientation cube" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.size(62.dp).clickable { onPreset("Isometric") }) {
            val front = Path().apply { moveTo(10f, 22f); lineTo(38f, 30f); lineTo(38f, 57f); lineTo(10f, 48f); close() }
            val side = Path().apply { moveTo(38f, 30f); lineTo(56f, 18f); lineTo(56f, 45f); lineTo(38f, 57f); close() }
            val top = Path().apply { moveTo(10f, 22f); lineTo(29f, 9f); lineTo(56f, 18f); lineTo(38f, 30f); close() }
            drawPath(front, Cyan.copy(.35f)); drawPath(front, Cyan, style = Stroke(2f))
            drawPath(side, Violet.copy(.4f)); drawPath(side, Violet, style = Stroke(2f))
            drawPath(top, Amber.copy(.35f)); drawPath(top, Amber, style = Stroke(2f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Front", "Top", "Side").forEach { preset -> Text(preset.take(1), color = Ink, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onPreset(preset) }.padding(5.dp)) }
        }
    }
}

@Composable
private fun SpatialSceneNavigator(
    solids: List<Solid>,
    selected: Set<Int>,
    collisions: List<Pair<Int, Int>>,
    trail: List<Vec3>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier.width(148.dp).height(112.dp).clip(RoundedCornerShape(15.dp))
            .background(SurfaceA.copy(.92f)).border(1.dp, Cyan.copy(.45f), RoundedCornerShape(15.dp))
            .semantics { contentDescription = "3D scene navigator with ${solids.size} objects, ${selected.size} selected, and ${collisions.size} overlaps" },
    ) {
        val all = solids.map { it.position } + trail
        val minX = all.minOfOrNull { it.x } ?: -4.0; val maxX = all.maxOfOrNull { it.x } ?: 4.0
        val minZ = all.minOfOrNull { it.z } ?: -4.0; val maxZ = all.maxOfOrNull { it.z } ?: 4.0
        fun map(point: Vec3) = Offset(
            12f + ((point.x - minX) / (maxX - minX).coerceAtLeast(1.0)).toFloat() * (size.width - 24f),
            12f + ((point.z - minZ) / (maxZ - minZ).coerceAtLeast(1.0)).toFloat() * (size.height - 24f),
        )
        for (i in 1..3) {
            drawLine(Ink.copy(.08f), Offset(size.width * i / 4f, 0f), Offset(size.width * i / 4f, size.height), 1f)
            drawLine(Ink.copy(.08f), Offset(0f, size.height * i / 4f), Offset(size.width, size.height * i / 4f), 1f)
        }
        trail.zipWithNext().forEachIndexed { index, pair -> drawLine(Amber.copy(alpha = .15f + .7f * index / trail.size.coerceAtLeast(1)), map(pair.first), map(pair.second), 2f) }
        solids.forEachIndexed { index, solid ->
            val colliding = collisions.any { index == it.first || index == it.second }
            val color = when { colliding -> Color.Red; index in selected -> Amber; else -> Cyan }
            drawCircle(color.copy(.22f), if (index in selected) 10f else 7f, map(solid.position))
            drawCircle(color, if (index in selected) 5f else 3.5f, map(solid.position))
        }
        drawGraphLabel("TOP · scene", Offset(8f, 20f), Muted)
    }
}

@Composable
private fun GraphCanvas(
    modifier: Modifier,
    functions: List<com.indianservers.aiexplorer.core.FunctionDefinition>,
    dataPoints: List<Vec2>,
    traceX: Double,
    graphTool: GraphTool,
    homeRequest: Int,
    backRequest: Int,
    forwardRequest: Int,
    axisSettings: GraphAxisSettings,
    domains: Map<String, GraphDomainSelection>,
    styles: Map<String, GraphLineStyle>,
    labelOffsets: Map<String, Offset>,
    comparisonMode: Boolean,
    showMiniMap: Boolean,
    parameterA: Float,
    parameterHandleEnabled: Boolean,
    parameterValues: Map<String, Double>,
    previewExpression: String?,
    brushInterval: ClosedFloatingPointRange<Double>?,
    sketchPoints: List<Vec2>,
    pinnedTracePoints: List<Vec2>,
    showResidualPlot: Boolean,
    splitComparison: Boolean,
    snapshotExpressions: List<String>,
    selectedFunctionId: String?,
    onSelectFunction: (String) -> Unit,
    onClearSelection: () -> Unit,
    onTraceChange: (Double) -> Unit,
    onParameterAChange: (Double) -> Unit,
    onParameterChange: (String, Double) -> Unit,
    onDomainChange: (String, GraphDomainSelection) -> Unit,
    onLabelMove: (String, Offset) -> Unit,
    onCurveDrag: (String, Vec2) -> Unit,
    onCurveDragEnd: (String, Vec2) -> Unit,
    onBrushChange: (Double, Double) -> Unit,
    onSketchChange: (List<Vec2>) -> Unit,
    onSketchEnd: (List<Vec2>) -> Unit,
    onDataPointMove: (Int, Vec2) -> Unit,
    onViewportChange: (GraphViewState) -> Unit,
    onContextMenu: (String?, Vec2) -> Unit,
) {
    val graph = remember { GraphAnalysis() }
    val advancedGraphEngine = remember { AdvancedGraphEngine() }
    val typedGraphEngine = remember { TypedGraphEngine() }
    val engine = remember { ExpressionEngine() }
    var cameraCenter by remember { mutableStateOf(Vec2(0.0, 0.0)) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var lastTapAt by remember { mutableStateOf(0L) }
    var gestureMode by remember { mutableStateOf(GestureMode.Idle) }
    val viewHistory = remember { com.indianservers.aiexplorer.core.GraphViewHistory() }
    val currentFunctions by rememberUpdatedState(functions)
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            val fitPoints = functions.filter { it.visible }.flatMap { function ->
                runCatching { graph.sampleDefinition(function.expression, -10.0, 10.0, 240).points.filter { it.x.isFinite() && it.y.isFinite() } }.getOrDefault(emptyList())
            } + dataPoints
            val fit = InteractionGeometry.fit(fitPoints)
            cameraCenter = fit.center
            cameraZoom = fit.zoom
            viewHistory.commit(GraphViewState(cameraCenter, cameraZoom))
        }
    }
    LaunchedEffect(backRequest) { if (backRequest > 0) viewHistory.back().let { cameraCenter = it.center; cameraZoom = it.zoom } }
    LaunchedEffect(forwardRequest) { if (forwardRequest > 0) viewHistory.forward().let { cameraCenter = it.center; cameraZoom = it.zoom } }
    LaunchedEffect(cameraCenter, cameraZoom) { onViewportChange(GraphViewState(cameraCenter, cameraZoom)) }
    Canvas(
        modifier
            .pointerInput(graphTool, selectedFunctionId, domains, labelOffsets, parameterA, parameterHandleEnabled, parameterValues) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureStartedAt = System.currentTimeMillis()
                    val baseScale = size.width / 14f
                    fun scale() = baseScale * cameraZoom
                    fun origin() = Offset(
                        size.width / 2f - cameraCenter.x.toFloat() * scale(),
                        size.height / 2f + cameraCenter.y.toFloat() * scale(),
                    )
                    fun world(screen: Offset): Vec2 {
                        val o = origin()
                        val displayX = ((screen.x - o.x) / scale()).toDouble(); val displayY = ((o.y - screen.y) / scale()).toDouble()
                        return Vec2(if (axisSettings.xLogarithmic) 10.0.pow(displayX) else displayX, if (axisSettings.yLogarithmic) 10.0.pow(displayY) else displayY)
                    }
                    fun screen(point: Vec2): Offset {
                        val o = origin()
                        val displayX = if (axisSettings.xLogarithmic && point.x > 0) log10(point.x) else point.x
                        val displayY = if (axisSettings.yLogarithmic && point.y > 0) log10(point.y) else point.y
                        return Offset(o.x + displayX.toFloat() * scale(), o.y - displayY.toFloat() * scale())
                    }
                    fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
                        val ab = b - a
                        val ap = p - a
                        val length2 = ab.x * ab.x + ab.y * ab.y
                        if (length2 <= 1e-6f) return (p - a).getDistance()
                        val t = ((ap.x * ab.x + ap.y * ab.y) / length2).coerceIn(0f, 1f)
                        val projection = a + ab * t
                        return (p - projection).getDistance()
                    }
                    fun nearestFunction(screenTap: Offset): String? {
                        var bestId: String? = null
                        var bestDistance = 34f
                        currentFunctions.filter { it.visible }.forEach { fn ->
                            if (advancedGraphEngine.classify(fn.expression) == AdvancedGraphKind.Inequality) {
                                val tap = world(screenTap)
                                val inside = runCatching { engine.compile(fn.expression).eval(mapOf("x" to tap.x, "y" to tap.y)) != 0.0 }.getOrDefault(false)
                                if (inside) { bestDistance = 0f; bestId = fn.id }
                                return@forEach
                            }
                            val kind = graph.definitionKind(fn.expression)
                            if (kind == GraphDefinitionKind.Implicit) {
                                val halfWidth = size.width / (2f * scale())
                                val halfHeight = size.height / (2f * scale())
                                val center = world(Offset(size.width / 2f, size.height / 2f))
                                val segments = runCatching {
                                    graph.implicitSegments(fn.expression, center.x - halfWidth, center.x + halfWidth, center.y - halfHeight, center.y + halfHeight)
                                }.getOrDefault(emptyList())
                                segments.forEach { segment ->
                                    val d = distanceToSegment(screenTap, screen(segment.start), screen(segment.end))
                                    if (d < bestDistance) {
                                        bestDistance = d
                                        bestId = fn.id
                                    }
                                }
                            } else {
                                val tapWorld = world(screenTap)
                                val sample = runCatching { graph.sampleDefinition(fn.expression, tapWorld.x - 1.4, tapWorld.x + 1.4, steps = 160) }.getOrNull()
                                sample?.points?.zipWithNext()?.forEachIndexed { i, pair ->
                                    if (!sample.breaks.contains(i)) {
                                        val d = distanceToSegment(screenTap, screen(pair.first), screen(pair.second))
                                        if (d < bestDistance) {
                                            bestDistance = d
                                            bestId = fn.id
                                        }
                                    }
                                }
                            }
                        }
                        return bestId
                    }

                    val traceFunction = currentFunctions.firstOrNull { it.id == selectedFunctionId && it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
                        ?: currentFunctions.firstOrNull { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
                    val traceScreen = traceFunction?.let { fn ->
                        runCatching {
                            val y = engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))
                            val o = origin()
                            Offset(o.x + traceX.toFloat() * scale(), o.y - y.toFloat() * scale())
                        }.getOrNull()
                    }
                    val selectedFunction = currentFunctions.firstOrNull { it.id == selectedFunctionId }
                    val selectedDomain = selectedFunctionId?.let(domains::get)
                    val labelPosition = traceScreen?.plus(labelOffsets[selectedFunctionId] ?: Offset(18f, 28f))
                    val labelDrag = selectedFunctionId != null && labelPosition != null && (labelPosition - down.position).getDistance() < 70f
                    val parameterHandleWorld = selectedFunction?.takeIf { parameterHandleEnabled && Regex("\\ba\\b").containsMatchIn(it.expression) }?.let { fn ->
                        runCatching { Vec2(1.0, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to 1.0))) }.getOrNull()
                    }
                    val parameterDrag = parameterHandleWorld?.let { (screen(it) - down.position).getDistance() < 48f } == true
                    val canvasParameterColumns = maxOf(1, ((size.width - 80f) / 92f).toInt())
                    fun canvasParameterAnchor(index: Int) = Offset(80f + (index % canvasParameterColumns) * 92f, 120f + (index / canvasParameterColumns) * 54f)
                    val canvasParameter = if (parameterHandleEnabled) parameterValues.keys.withIndex().minByOrNull { (index, _) ->
                        (canvasParameterAnchor(index) - down.position).getDistance()
                    }?.takeIf { (index, _) -> (canvasParameterAnchor(index) - down.position).getDistance() < 38f }?.value else null
                    val domainSide = selectedDomain?.let { domain ->
                        val left = screen(Vec2(domain.minimum, 0.0)); val right = screen(Vec2(domain.maximum, 0.0))
                        when { (left - down.position).getDistance() < 42f -> -1; (right - down.position).getDistance() < 42f -> 1; else -> 0 }
                    } ?: 0
                    val traceDrag = !labelDrag && !parameterDrag && domainSide == 0 && (graphTool == GraphTool.Trace || (traceScreen != null && (traceScreen - down.position).getDistance() < 44f))
                    val startWorld = world(down.position)
                    val curveDragId = nearestFunction(down.position)?.takeIf { graphTool == GraphTool.Plot && it == selectedFunctionId }
                    val dataPointIndex = if (graphTool == GraphTool.Data) dataPoints.indices.minByOrNull { (screen(dataPoints[it]) - down.position).getDistance() }
                        ?.takeIf { (screen(dataPoints[it]) - down.position).getDistance() < 42f } else null
                    val brushing = graphTool == GraphTool.BrushArea
                    val sketching = graphTool == GraphTool.SketchFit
                    var activeSketch = if (sketching) listOf(startWorld) else emptyList()
                    var latestWorld = startWorld
                    val gestureStartView = GraphViewState(cameraCenter, cameraZoom)
                    var moved = false
                    var transformed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val pan = event.calculatePan()
                            val centroid = event.calculateCentroid()
                            cameraCenter = Vec2(cameraCenter.x - pan.x / scale(), cameraCenter.y + pan.y / scale())
                            val beforeZoom = world(centroid)
                            cameraZoom = GraphViewport.zoom(cameraZoom, event.calculateZoom())
                            val afterZoom = world(centroid)
                            cameraCenter += beforeZoom - afterZoom
                            transformed = true
                            gestureMode = if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (change != null) latestWorld = world(change.position)
                            if (delta.getDistance() > 0f) {
                                moved = moved || (change!!.position - down.position).getDistance() > 8f
                                if (canvasParameter != null) {
                                    onParameterChange(canvasParameter, ((change!!.position.x - 40f) / (size.width - 80f) * 20f - 10f).toDouble())
                                    gestureMode = GestureMode.Resizing
                                } else if (dataPointIndex != null) {
                                    onDataPointMove(dataPointIndex, world(change!!.position))
                                    gestureMode = GestureMode.Moving
                                } else if (brushing) {
                                    onBrushChange(startWorld.x, world(change!!.position).x)
                                    gestureMode = GestureMode.Resizing
                                } else if (sketching) {
                                    activeSketch = (activeSketch + world(change!!.position)).takeLast(800)
                                    onSketchChange(activeSketch)
                                    gestureMode = GestureMode.Moving
                                } else if (curveDragId != null) {
                                    onCurveDrag(curveDragId, world(change!!.position) - startWorld)
                                    gestureMode = GestureMode.Moving
                                } else if (labelDrag && selectedFunctionId != null) {
                                    onLabelMove(selectedFunctionId, delta)
                                    gestureMode = GestureMode.Moving
                                } else if (parameterDrag) {
                                    val point = world(change!!.position)
                                    val base = selectedFunction?.expression?.replace(Regex("\\ba\\b"), "1")
                                    val unitY = base?.let { runCatching { engine.compile(stripEquation(it)).eval(mapOf("x" to point.x)) }.getOrNull() }
                                    if (unitY != null && abs(unitY) > 1e-8) onParameterAChange(point.y / unitY)
                                    gestureMode = GestureMode.Resizing
                                } else if (domainSide != 0 && selectedFunctionId != null && selectedDomain != null) {
                                    val x = world(change!!.position).x
                                    val next = if (domainSide < 0) selectedDomain.copy(minimum = x.coerceAtMost(selectedDomain.maximum - .01)) else selectedDomain.copy(maximum = x.coerceAtLeast(selectedDomain.minimum + .01))
                                    onDomainChange(selectedFunctionId, next)
                                    gestureMode = GestureMode.Resizing
                                } else if (traceDrag) {
                                    onTraceChange(world(change!!.position).x)
                                    gestureMode = GestureMode.Moving
                                } else {
                                    cameraCenter = Vec2(cameraCenter.x - delta.x / scale(), cameraCenter.y + delta.y / scale())
                                    gestureMode = GestureMode.Panning
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (moved && curveDragId != null) onCurveDragEnd(curveDragId, latestWorld - startWorld)
                    if (sketching && activeSketch.size >= 3) onSketchEnd(activeSketch)
                    if (!moved && !transformed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            val doubleTapHit = nearestFunction(down.position)
                            if (doubleTapHit != null) onSelectFunction(doubleTapHit) else {
                                val fitPoints = currentFunctions.filter { it.visible }.flatMap { function -> runCatching { graph.sampleDefinition(function.expression, -10.0, 10.0, 180).points }.getOrDefault(emptyList()) }
                                val fit = InteractionGeometry.fit(fitPoints); cameraCenter = fit.center; cameraZoom = fit.zoom
                            }
                            lastTapAt = 0L
                        } else {
                            val hit = nearestFunction(down.position)
                            if (now - gestureStartedAt > 520L) onContextMenu(hit, world(down.position))
                            else if (hit != null) onSelectFunction(hit)
                            else if (graphTool == GraphTool.Trace) onTraceChange(world(down.position).x)
                            else onClearSelection()
                            lastTapAt = now
                        }
                    }
                    val finishedView = GraphViewState(cameraCenter, cameraZoom)
                    if (finishedView != gestureStartView && (moved || transformed)) viewHistory.commit(finishedView)
                    gestureMode = GestureMode.Idle
                }
            }
            .semantics { contentDescription = "Interactive graphing canvas with axes, curves, trace point, and annotations" },
    ) {
        val scale = size.width / 14f * cameraZoom
        val origin = Offset(size.width / 2f - cameraCenter.x.toFloat() * scale, size.height / 2f + cameraCenter.y.toFloat() * scale)
        val tx: (Vec2) -> Offset = {
            val displayX = if (axisSettings.xLogarithmic && it.x > 0) log10(it.x) else if (axisSettings.xLogarithmic) Double.NaN else it.x
            val displayY = if (axisSettings.yLogarithmic && it.y > 0) log10(it.y) else if (axisSettings.yLogarithmic) Double.NaN else it.y
            Offset(origin.x + displayX.toFloat() * scale, origin.y - displayY.toFloat() * scale)
        }
        drawGrid(origin, scale, axisSettings)
        if (parameterHandleEnabled) parameterValues.entries.forEachIndexed { index, entry ->
            val columns = maxOf(1, ((size.width - 80f) / 92f).toInt())
            val anchor = Offset(80f + (index % columns) * 92f, 120f + (index / columns) * 54f)
            drawCircle(Cyan.copy(.2f), 20f, anchor)
            drawCircle(Cyan, 7f, anchor)
            drawGraphLabel("${entry.key}=${trim(entry.value)}", anchor + Offset(-34f, -28f), Cyan)
        }
        val halfWidth = size.width / (2f * scale)
        val halfHeight = size.height / (2f * scale)
        val minX = if (axisSettings.xLogarithmic) 10.0.pow(cameraCenter.x - halfWidth) else cameraCenter.x - halfWidth
        val maxX = if (axisSettings.xLogarithmic) 10.0.pow(cameraCenter.x + halfWidth) else cameraCenter.x + halfWidth
        val minY = if (axisSettings.yLogarithmic) 10.0.pow(cameraCenter.y - halfHeight) else cameraCenter.y - halfHeight
        val maxY = if (axisSettings.yLogarithmic) 10.0.pow(cameraCenter.y + halfHeight) else cameraCenter.y + halfHeight
        val traceAnchorRows = functions.filter { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }.mapNotNull { fn ->
            runCatching { Vec2(traceX, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to traceX))) }.getOrNull()?.takeIf { it.y.isFinite() }?.let { fn.id to it }
        }
        val arrangedTraceLabels = GraphUxEngine.avoidLabelCollisions(traceAnchorRows.map { it.second + Vec2(.25, .35) })
        val traceLabelsById = traceAnchorRows.map { it.first }.zip(arrangedTraceLabels).toMap()
        functions.forEachIndexed { index, fn ->
            if (!fn.visible) return@forEachIndexed
            val color = when (fn.colorKey) { "cyan" -> Cyan; "green" -> Green; "amber" -> Amber; else -> Violet }
            val selected = selectedFunctionId == fn.id
            val strokeWidth = if (selected) 6.0f else 4.2f
            val styleEffect = when (styles[fn.id] ?: GraphLineStyle.Solid) { GraphLineStyle.Solid -> null; GraphLineStyle.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(16f, 9f)); GraphLineStyle.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 8f)) }
            val typedDefinition = runCatching { TypedGraphExpressionParser.parse(fn.expression) }.getOrNull()
            if (typedDefinition is TypedGraphExpression.Inequality) {
                val columns = 42; val rows = 42
                val cells = runCatching { typedGraphEngine.sample(typedDefinition, GraphDomain(minX, maxX), GraphDomain(minY, maxY, "y"), parameterValues, 168).inequalityCells }.getOrDefault(emptyList())
                val cellSize = Size(size.width / columns, size.height / rows)
                cells.filter { it.satisfied }.forEach { cell -> drawRect(color.copy(if (selected) .25f else .14f), topLeft = tx(cell.center) - Offset(cellSize.width / 2, cellSize.height / 2), size = cellSize) }
                return@forEachIndexed
            }
            val kind = graph.definitionKind(fn.expression)
            if (typedDefinition is TypedGraphExpression.Implicit) {
                val segments = runCatching { typedGraphEngine.sample(typedDefinition, GraphDomain(minX, maxX), GraphDomain(minY, maxY, "y"), parameterValues, 520).implicitSegments }.getOrDefault(emptyList())
                segments.forEach { drawLine(color, tx(it.start), tx(it.end), if (selected) 5.2f else 3.2f, cap = StrokeCap.Round, pathEffect = styleEffect) }
            } else {
                val domain = domains[fn.id]
                val sampleMinimum = max(minX, domain?.minimum ?: minX)
                val sampleMaximum = min(maxX, domain?.maximum ?: maxX)
                val sample = if (sampleMinimum < sampleMaximum && typedDefinition != null) runCatching {
                    typedGraphEngine.sample(typedDefinition, GraphDomain(sampleMinimum, sampleMaximum), GraphDomain(minY, maxY, "y"), parameterValues, 520)
                }.getOrNull() else null
                sample?.curves?.forEach { segment ->
                    segment.points.zipWithNext().forEach { pair ->
                        val logValid = (!axisSettings.xLogarithmic || pair.first.x > 0 && pair.second.x > 0) && (!axisSettings.yLogarithmic || pair.first.y > 0 && pair.second.y > 0)
                        if (logValid) drawLine(color.copy(alpha = if (selected) 1f else .55f), tx(pair.first), tx(pair.second), strokeWidth, cap = StrokeCap.Round, pathEffect = styleEffect)
                    }
                }
                if (domain != null && selected) {
                    listOf(domain.minimum to domain.leftClosed, domain.maximum to domain.rightClosed).forEach { (x, closed) ->
                        val y = (typedDefinition as? TypedGraphExpression.Explicit)?.let { runCatching { engine.compile(it.expression).eval(parameterValues + ("x" to x)) }.getOrNull() }
                        if (y?.isFinite() == true) { if (closed) drawCircle(color, 10f, tx(Vec2(x, y))) else drawCircle(color, 10f, tx(Vec2(x, y)), style = Stroke(4f)); drawLine(color.copy(.7f), tx(Vec2(x, 0.0)) + Offset(0f, -18f), tx(Vec2(x, 0.0)) + Offset(0f, 18f), 4f) }
                    }
                }
            }
            val trace = if (kind == GraphDefinitionKind.Explicit) runCatching {
                val y = engine.compile(stripEquation(fn.expression)).eval(parameterValues + ("x" to traceX))
                Vec2(traceX, y)
            }.getOrNull() else null
            trace?.takeIf { abs(tx(it).y - size.height / 2f) <= size.height }?.let {
                if (graphTool in setOf(GraphTool.Trace, GraphTool.Tangent, GraphTool.Normal, GraphTool.Derivative, GraphTool.Integral, GraphTool.AreaBetween) || index == 0) {
                    drawRadiantPoint(tx(it), color, "")
                    val automatic = traceLabelsById[fn.id]?.let(tx) ?: tx(it) + Offset(18f, -18f)
                    val position = if (selected) tx(it) + (labelOffsets[fn.id] ?: Offset(18f, 28f)) else automatic
                    drawGraphLabel("${fn.name}: (${GraphUxEngine.format(it.x, axisSettings.format)}, ${GraphUxEngine.format(it.y, axisSettings.format)})", position, color)
                }
            }
            if (selected && parameterHandleEnabled && Regex("\\ba\\b").containsMatchIn(fn.expression)) {
                runCatching { Vec2(1.0, engine.compile(stripEquation(fn.expression)).eval(mapOf("x" to 1.0))) }.getOrNull()?.let { point -> drawRadiantPoint(tx(point), Amber, "drag a=${trim(parameterA.toDouble())}") }
            }
        }
        brushInterval?.let { interval ->
            functions.firstOrNull { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }?.let { function ->
                val compiled = runCatching { engine.compile(stripEquation(function.expression)) }.getOrNull()
                if (compiled != null) {
                    val start = max(minX, interval.start); val end = min(maxX, interval.endInclusive)
                    if (start < end) {
                        val areaPath = Path().apply {
                            val base = tx(Vec2(start, 0.0)); moveTo(base.x, base.y)
                            (0..120).forEach { i ->
                                val x = start + (end - start) * i / 120.0
                                val y = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
                                if (y.isFinite()) { val point = tx(Vec2(x, y)); lineTo(point.x, point.y) }
                            }
                            val finish = tx(Vec2(end, 0.0)); lineTo(finish.x, finish.y); close()
                        }
                        drawPath(areaPath, Brush.verticalGradient(listOf(Cyan.copy(.38f), Violet.copy(.12f))))
                        drawGraphLabel("Brushed area", tx(Vec2(start, 0.0)) + Offset(10f, -35f), Cyan)
                    }
                }
            }
        }
        if (sketchPoints.size >= 2) {
            sketchPoints.zipWithNext().forEach { (a, b) -> drawLine(Amber, tx(a), tx(b), 5f, StrokeCap.Round) }
            drawGraphLabel("Release to fit a quadratic", tx(sketchPoints.last()) + Offset(12f, -30f), Amber)
        }
        pinnedTracePoints.forEachIndexed { index, point ->
            val anchor = tx(point)
            drawLine(Green.copy(.45f), Offset(anchor.x, 0f), Offset(anchor.x, size.height), 1.5f)
            drawLine(Green.copy(.45f), Offset(0f, anchor.y), Offset(size.width, anchor.y), 1.5f)
            drawCircle(Green.copy(.24f), 14f, anchor); drawCircle(Green, 5f, anchor)
            drawGraphLabel("P${index + 1} (${trim(point.x)}, ${trim(point.y)})", anchor + Offset(10f, -34f), Green)
        }
        if (splitComparison) {
            drawLine(Amber.copy(.8f), Offset(size.width / 2f, 96f), Offset(size.width / 2f, size.height - 70f), 2f)
            drawGraphLabel("A", Offset(size.width * .25f, 130f), Cyan); drawGraphLabel("B", Offset(size.width * .75f, 130f), Violet)
        }
        if (showResidualPlot && dataPoints.size >= 3) {
            StatisticsEngine.summarize(dataPoints).regression?.let { regression ->
                val top = size.height * .76f
                drawRect(SurfaceA.copy(.88f), Offset(0f, top), Size(size.width, size.height - top))
                drawLine(Muted, Offset(0f, (top + size.height) / 2f), Offset(size.width, (top + size.height) / 2f), 1.5f)
                dataPoints.forEach { point ->
                    val residual = point.y - (regression.slope * point.x + regression.intercept)
                    val anchor = Offset(tx(point).x, (top + size.height) / 2f - residual.toFloat() * scale * .35f)
                    drawCircle(Amber, 5f, anchor)
                }
                drawGraphLabel("Residual plot", Offset(14f, top + 26f), Amber)
            }
        }
        if (graphTool == GraphTool.ComplexPlane) {
            val originPoint = tx(Vec2(0.0, 0.0))
            drawCircle(Violet.copy(.18f), scale, originPoint)
            drawCircle(Violet, scale, originPoint, style = Stroke(2.5f))
            listOf(Vec2(0.0, 1.0) to "i", Vec2(0.0, -1.0) to "−i", Vec2(1.0, 0.0) to "1", Vec2(-1.0, 0.0) to "−1").forEach { (point, label) ->
                drawRadiantPoint(tx(point), Amber, label)
            }
            drawGraphLabel("Complex plane · horizontal Re(z), vertical Im(z)", Offset(18f, 165f), Violet)
        }
        previewExpression?.let { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 420) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Amber.copy(.85f), tx(pair.first), tx(pair.second), 3.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 8f))) }
            drawGraphLabel("Transformation preview", Offset(18f, 212f), Amber)
        }
        snapshotExpressions.forEach { expression ->
            val sample = runCatching { graph.sampleDefinition(expression, minX, maxX, 260) }.getOrNull()
            sample?.points?.zipWithNext()?.forEachIndexed { index, pair -> if (!sample.breaks.contains(index)) drawLine(Color.White.copy(.32f), tx(pair.first), tx(pair.second), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 7f))) }
        }
        if (snapshotExpressions.isNotEmpty()) drawGraphLabel("Saved-state overlay", Offset(18f, 258f), Color.White)
        if (dataPoints.isNotEmpty()) {
            dataPoints.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "D${index + 1}") }
            StatisticsEngine.summarize(dataPoints).regression?.let { regression ->
                drawLine(
                    Green,
                    tx(Vec2(minX, regression.slope * minX + regression.intercept)),
                    tx(Vec2(maxX, regression.slope * maxX + regression.intercept)),
                    3f,
                )
            }
        }
        val explicit = functions.filter { it.visible && graph.definitionKind(it.expression) == GraphDefinitionKind.Explicit }
        if (graphTool == GraphTool.Trace) {
            val traceScreenX = tx(Vec2(traceX, 0.0)).x
            drawLine(Cyan.copy(.7f), Offset(traceScreenX, 0f), Offset(traceScreenX, size.height), 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(9f, 7f)))
            explicit.firstOrNull()?.let { function ->
                runCatching { engine.compile(stripEquation(function.expression)).eval(mapOf("x" to traceX)) }.getOrNull()?.takeIf(Double::isFinite)?.let { y ->
                    val traceScreenY = tx(Vec2(traceX, y)).y
                    drawLine(Cyan.copy(.5f), Offset(0f, traceScreenY), Offset(size.width, traceScreenY), 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 7f)))
                }
            }
            drawGraphLabel("x=${GraphUxEngine.format(traceX, axisSettings.format)}", Offset(traceScreenX + 10f, 165f), Cyan)
        }
        if (comparisonMode && explicit.size >= 2) {
            val first = runCatching { engine.compile(stripEquation(explicit[0].expression)) }.getOrNull()
            val second = runCatching { engine.compile(stripEquation(explicit[1].expression)) }.getOrNull()
            if (first != null && second != null) {
                val comparison = GraphUxEngine.compare(first, second, minX, maxX, 180)
                if (comparison.size > 2) {
                    val path = Path(); val firstPoint = tx(Vec2(comparison.first().x, comparison.first().first)); path.moveTo(firstPoint.x, firstPoint.y)
                    comparison.drop(1).forEach { val point = tx(Vec2(it.x, it.first)); path.lineTo(point.x, point.y) }
                    comparison.asReversed().forEach { val point = tx(Vec2(it.x, it.second)); path.lineTo(point.x, point.y) }; path.close()
                    drawPath(path, Brush.verticalGradient(listOf(Cyan.copy(.20f), Violet.copy(.20f))))
                    comparison.minByOrNull { abs(it.x - traceX) }?.let { drawGraphLabel("f-g=${trim(it.difference)}", Offset(18f, 165f), if (it.difference >= 0) Green else Amber) }
                }
            }
        }
        drawGraphAnalysisOverlay(
            explicit.firstOrNull()?.expression,
            explicit.getOrNull(1)?.expression,
            traceX,
            graphTool,
            engine,
            tx,
            origin,
            scale,
        )
        if (gestureMode != GestureMode.Idle) drawGraphLabel(gestureMode.label, Offset(size.width / 2f - 95f, 118f), Cyan)
        if (showMiniMap) {
            val mapTop = Offset(size.width - 190f, size.height - 205f); val mapSize = Size(170f, 130f)
            drawRoundRect(SurfaceA.copy(.92f), mapTop, mapSize, androidx.compose.ui.geometry.CornerRadius(16f, 16f)); drawRoundRect(Cyan.copy(.55f), mapTop, mapSize, androidx.compose.ui.geometry.CornerRadius(16f, 16f), style = Stroke(1.5f))
            val content = functions.filter { it.visible }.flatMap { runCatching { graph.sampleDefinition(it.expression, -20.0, 20.0, 100).points }.getOrDefault(emptyList()) }
            val mini = GraphUxEngine.minimap(content, GraphViewState(cameraCenter, cameraZoom), (mapSize.width / mapSize.height).toDouble())
            val worldWidth = mini.world.width.coerceAtLeast(1e-6); val worldHeight = mini.world.height.coerceAtLeast(1e-6)
            fun miniPoint(point: Vec2) = Offset(mapTop.x + ((point.x - mini.world.minimum.x) / worldWidth).toFloat() * mapSize.width, mapTop.y + (1 - (point.y - mini.world.minimum.y) / worldHeight).toFloat() * mapSize.height)
            content.zipWithNext().forEach { (a, b) -> drawLine(Violet.copy(.5f), miniPoint(a), miniPoint(b), 1f) }
            val a = miniPoint(mini.viewport.minimum); val b = miniPoint(mini.viewport.maximum)
            drawRect(Amber.copy(.85f), topLeft = Offset(min(a.x, b.x), min(a.y, b.y)), size = Size(abs(b.x - a.x), abs(b.y - a.y)), style = Stroke(2f))
        }
        drawGraphLabel("${trim(cameraZoom.toDouble())}× · ${trim((size.width / scale).toDouble())} units", Offset(size.width - 205f, 118f), Muted)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphAnalysisOverlay(
    expression: String?,
    secondExpression: String?,
    traceX: Double,
    graphTool: GraphTool,
    engine: ExpressionEngine,
    tx: (Vec2) -> Offset,
    origin: Offset,
    scale: Float,
) {
    if (expression == null) return
    val compiled = runCatching { engine.compile(stripEquation(expression)) }.getOrNull() ?: return
    fun f(x: Double) = runCatching { compiled.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
    val y = f(traceX)
    if (!y.isFinite()) return
    val point = Vec2(traceX, y)
    val h = 0.001
    val slope = (f(traceX + h) - f(traceX - h)) / (2.0 * h)
    val leftWorld = ((0f - origin.x) / scale).toDouble()
    val rightWorld = ((size.width - origin.x) / scale).toDouble()
    when (graphTool) {
        GraphTool.Tangent -> {
            val tangent = { x: Double -> y + slope * (x - traceX) }
            drawLine(Amber, tx(Vec2(leftWorld, tangent(leftWorld))), tx(Vec2(rightWorld, tangent(rightWorld))), 3f, cap = StrokeCap.Round)
            drawGraphLabel("tangent slope ${trim(slope)}", tx(point) + Offset(18f, -54f), Amber)
        }
        GraphTool.Normal -> {
            val normalSlope = if (abs(slope) < 1e-9) 1e6 else -1.0 / slope
            val normal = { x: Double -> y + normalSlope * (x - traceX) }
            drawLine(Violet, tx(Vec2(leftWorld, normal(leftWorld))), tx(Vec2(rightWorld, normal(rightWorld))), 3f, cap = StrokeCap.Round)
            drawGraphLabel("normal slope ${trim(normalSlope)}", tx(point) + Offset(18f, -54f), Violet)
        }
        GraphTool.Derivative -> {
            val derivativePoints = (0..360).mapNotNull { i ->
                val x = leftWorld + (rightWorld - leftWorld) * i / 360.0
                val d = (f(x + h) - f(x - h)) / (2.0 * h)
                if (d.isFinite()) Vec2(x, d) else null
            }
            derivativePoints.zipWithNext().forEach { (a, b) -> drawLine(Green, tx(a), tx(b), 3f, cap = StrokeCap.Round) }
            drawGraphLabel("f'(${trim(traceX)}) = ${trim(slope)}", tx(point) + Offset(18f, -54f), Green)
        }
        GraphTool.Integral -> {
            val start = 0.0.coerceAtMost(traceX)
            val end = 0.0.coerceAtLeast(traceX)
            val steps = 80
            val path = Path()
            path.moveTo(tx(Vec2(start, 0.0)).x, tx(Vec2(start, 0.0)).y)
            var area = 0.0
            var lastX = start
            var lastY = f(start)
            for (i in 0..steps) {
                val x = start + (end - start) * i / steps
                val yy = f(x)
                if (yy.isFinite()) {
                    val p = tx(Vec2(x, yy))
                    path.lineTo(p.x, p.y)
                    if (i > 0) area += (yy + lastY) * .5 * (x - lastX)
                    lastX = x
                    lastY = yy
                }
            }
            path.lineTo(tx(Vec2(end, 0.0)).x, tx(Vec2(end, 0.0)).y)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(Cyan.copy(.33f), Violet.copy(.18f), Color.Transparent)))
            drawPath(path, Cyan.copy(.8f), style = Stroke(2f))
            drawGraphLabel("area ${trim(area)}", tx(point) + Offset(18f, -54f), Cyan)
        }
        GraphTool.AreaBetween -> {
            val other = secondExpression?.let { runCatching { engine.compile(stripEquation(it)) }.getOrNull() } ?: return
            val start = min(0.0, traceX)
            val end = max(0.0, traceX)
            val steps = 100
            val top = (0..steps).mapNotNull { i ->
                val x = start + (end - start) * i / steps
                val yy = f(x)
                if (yy.isFinite()) Vec2(x, yy) else null
            }
            val bottom = (steps downTo 0).mapNotNull { i ->
                val x = start + (end - start) * i / steps
                val yy = runCatching { other.eval(mapOf("x" to x)) }.getOrDefault(Double.NaN)
                if (yy.isFinite()) Vec2(x, yy) else null
            }
            if (top.isNotEmpty() && bottom.isNotEmpty()) {
                val path = Path().apply {
                    val first = tx(top.first()); moveTo(first.x, first.y)
                    (top.drop(1) + bottom).forEach { val p = tx(it); lineTo(p.x, p.y) }
                    close()
                }
                drawPath(path, Brush.verticalGradient(listOf(Amber.copy(.30f), Violet.copy(.18f))))
                drawPath(path, Amber, style = Stroke(2f))
            }
        }
        GraphTool.Intersections -> {
            val graph = GraphAnalysis()
            val other = secondExpression?.let { runCatching { engine.compile(stripEquation(it)) }.getOrNull() }
            val points = if (other == null) graph.roots(expression, leftWorld, rightWorld).map { Vec2(it, f(it)) }
            else graph.intersections(compiled, other, leftWorld, rightWorld)
            points.forEach { drawRadiantPoint(tx(it), Amber, "(${trim(it.x)}, ${trim(it.y)})") }
        }
        GraphTool.Extrema -> {
            GraphAnalysis().extrema(expression, leftWorld, rightWorld).forEach {
                drawRadiantPoint(tx(it), Green, "extremum (${trim(it.x)}, ${trim(it.y)})")
            }
        }
        GraphTool.Plot, GraphTool.Trace, GraphTool.BrushArea, GraphTool.SketchFit, GraphTool.Table, GraphTool.Data, GraphTool.Probability, GraphTool.ComplexPlane -> Unit
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphLabel(text: String, position: Offset, color: Color) {
    drawRoundRect(SurfaceA, topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f))
    drawRoundRect(color.copy(.75f), topLeft = position, size = Size(190f, 42f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f), style = Stroke(1.4f))
    drawTrigText(text, position.x + 12f, position.y + 27f, color)
}

@Composable
private fun TrigCanvas(
    modifier: Modifier,
    angleDegrees: Float,
    transform: TrigTransform,
    function: TrigFunction,
    showTangents: Boolean,
    showProjections: Boolean,
    showWave: Boolean,
    homeRequest: Int,
    onZoomChanged: (Float) -> Unit,
    onAngleChange: (Float) -> Unit,
    visibleFunctions: Set<TrigFunction> = setOf(function),
    showAsymptotes: Boolean = false,
    polarSamples: List<Vec2> = emptyList(),
    harmonics: List<HarmonicComponent> = emptyList(),
    lineStyle: TrigLineStyle = TrigLineStyle.Solid,
    paletteShift: Int = 0,
    equationTarget: Float? = null,
    equationRoots: List<Double> = emptyList(),
    onTransformChange: (Float, Float, Float, Float) -> Unit = { _, _, _, _ -> },
) {
    var viewport by remember { mutableStateOf(TrigViewport()) }
    var lastTapAt by remember { mutableStateOf(0L) }
    LaunchedEffect(homeRequest) {
        if (homeRequest > 0) {
            viewport = TrigViewport()
            onZoomChanged(1f)
        }
    }
    fun angleAt(position: Offset, width: Float, height: Float): Float {
        val waveTop = height * .68f
        if (position.y >= waveTop && position.x in width * .15f..width * .71f) {
            val progress = ((position.x - width * .15f - 42f) / (width * .56f - 70f)).coerceIn(0f, 1f)
            val degrees = progress * 360f
            return if (degrees > 180f) degrees - 360f else degrees
        }
        val center = Offset(width * .47f, height * .42f)
        return Math.toDegrees(kotlin.math.atan2((center.y - position.y).toDouble(), (position.x - center.x).toDouble())).toFloat()
    }
    fun isManipulationTarget(position: Offset, width: Float, height: Float): Boolean {
        val waveHit = showWave && position.y in height * .66f..height * .91f && position.x in width * .12f..width * .74f
        val center = Offset(width * .47f, height * .42f)
        val radius = min(width, height) * .235f
        val circleHit = abs((position - center).getDistance() - radius) <= maxOf(48f, radius * .28f)
        return waveHit || circleHit
    }
    fun transformHandle(position: Offset, width: Float, height: Float): String? {
        if (!showWave) return null
        val top = height * .68f
        val paneHeight = height * .2f
        val origin = Offset(width * .15f + 42f, top + paneHeight / 2f)
        val waveWidth = width * .56f - 70f
        val scaleY = paneHeight * .14f
        val handles = mapOf(
            "amplitude" to Offset(origin.x + waveWidth, origin.y - transform.amplitude.toFloat() * scaleY),
            "period" to Offset(origin.x + (transform.period / (4 * Math.PI)).toFloat().coerceIn(.08f, 1f) * waveWidth, top + paneHeight - 20f),
            "phase" to Offset(origin.x + ((transform.phaseShift + Math.PI) / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * waveWidth, top + 20f),
            "vertical" to Offset(origin.x - 16f, origin.y - transform.verticalShift.toFloat() * scaleY),
        )
        return handles.minByOrNull { (_, point) -> (point - position).getDistance() }?.takeIf { (_, point) -> (point - position).getDistance() < 44f }?.key
    }
    fun updateTransform(handle: String, position: Offset, width: Float, height: Float) {
        val top = height * .68f
        val paneHeight = height * .2f
        val origin = Offset(width * .15f + 42f, top + paneHeight / 2f)
        val waveWidth = width * .56f - 70f
        val scaleY = paneHeight * .14f
        var a = transform.amplitude.toFloat(); var p = transform.period.toFloat(); var h = transform.phaseShift.toFloat(); var k = transform.verticalShift.toFloat()
        when (handle) {
            "amplitude" -> a = kotlin.math.abs((origin.y - position.y) / scaleY).coerceIn(.25f, 3f)
            "period" -> p = ((position.x - origin.x) / waveWidth * (4 * Math.PI).toFloat()).coerceIn(1f, 12.57f)
            "phase" -> h = (((position.x - origin.x) / waveWidth) * (2 * Math.PI).toFloat() - Math.PI.toFloat()).coerceIn(-3.14f, 3.14f)
            "vertical" -> k = ((origin.y - position.y) / scaleY).coerceIn(-2f, 2f)
        }
        onTransformChange(a, p, h, k)
    }
    val interactiveModifier = modifier.pointerInput(showWave, transform, polarSamples, harmonics) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var gestureViewport = viewport
            fun content(screen: Offset): Offset {
                val value = TrigViewportEngine.screenToContent(gestureViewport, Vec2(screen.x.toDouble(), screen.y.toDouble()), size.width.toDouble(), size.height.toDouble())
                return Offset(value.x.toFloat(), value.y.toFloat())
            }
            val initialContent = content(down.position)
            val activeHandle = transformHandle(initialContent, size.width.toFloat(), size.height.toFloat())
            val objectDrag = activeHandle != null || isManipulationTarget(initialContent, size.width.toFloat(), size.height.toFloat())
            if (objectDrag && activeHandle == null) onAngleChange(angleAt(initialContent, size.width.toFloat(), size.height.toFloat()))
            var moved = false
            var transformed = false
            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }
                if (pressed.size >= 2) {
                    val centroid = event.calculateCentroid()
                    val pan = event.calculatePan()
                    gestureViewport = TrigViewportEngine.transform(
                        gestureViewport,
                        size.width.toDouble(),
                        size.height.toDouble(),
                        Vec2(centroid.x.toDouble(), centroid.y.toDouble()),
                        Vec2(pan.x.toDouble(), pan.y.toDouble()),
                        event.calculateZoom(),
                    )
                    viewport = gestureViewport
                    onZoomChanged(gestureViewport.zoom)
                    transformed = true
                    event.changes.forEach { it.consume() }
                } else {
                    val change = event.changes.firstOrNull()
                    val delta = change?.positionChange() ?: Offset.Zero
                    if (change != null && delta.getDistance() > 0f) {
                        moved = moved || (change.position - down.position).getDistance() > 8f
                        if (objectDrag && !transformed) {
                            val current = content(change.position)
                            if (activeHandle != null) updateTransform(activeHandle, current, size.width.toFloat(), size.height.toFloat())
                            else onAngleChange(angleAt(current, size.width.toFloat(), size.height.toFloat()))
                        }
                        change.consume()
                    }
                }
                if (event.changes.none { it.pressed }) break
            }
            if (!moved && !transformed) {
                val now = System.currentTimeMillis()
                if (now - lastTapAt < 320L) {
                    viewport = TrigViewport()
                    onZoomChanged(1f)
                    lastTapAt = 0L
                } else {
                    if (objectDrag && activeHandle == null) onAngleChange(angleAt(content(down.position), size.width.toFloat(), size.height.toFloat()))
                    lastTapAt = now
                }
            }
        }
    }
    Canvas(interactiveModifier.semantics { contentDescription = "Interactive trigonometry workspace. Drag the unit-circle point or wave cursor; use two fingers to pan and pinch zoom the pane." }) {
        val viewportCenter = Offset(size.width / 2f, size.height / 2f)
        drawContext.canvas.save()
        drawContext.canvas.translate(viewportCenter.x + viewport.pan.x.toFloat(), viewportCenter.y + viewport.pan.y.toFloat())
        drawContext.canvas.scale(viewport.zoom, viewport.zoom)
        drawContext.canvas.translate(-viewportCenter.x, -viewportCenter.y)
        val angle = Math.toRadians(angleDegrees.toDouble())
        val center = Offset(size.width * .47f, size.height * .42f)
        val radius = size.minDimension * .235f
        fun circlePoint(theta: Double) = Offset(
            center.x + cos(theta).toFloat() * radius,
            center.y - sin(theta).toFloat() * radius,
        )
        drawTrigGrid()
        drawLine(Color.White.copy(.85f), Offset(center.x - radius * 1.35f, center.y), Offset(center.x + radius * 1.38f, center.y), 2.4f)
        drawLine(Color.White.copy(.85f), Offset(center.x, center.y + radius * 1.35f), Offset(center.x, center.y - radius * 1.38f), 2.4f)
        drawCircle(Cyan.copy(alpha = .88f), radius = radius, center = center, style = Stroke(3.2f))
        val p = circlePoint(angle)
        val cosPoint = Offset(p.x, center.y)
        val sinPoint = Offset(center.x, p.y)
        drawLine(Violet, center, p, 4f, cap = StrokeCap.Round)
        if (showProjections) {
            drawLine(Cyan.copy(alpha = .9f), p, cosPoint, 2.8f)
            drawLine(Violet.copy(alpha = .8f), p, sinPoint, 2.4f)
            drawLine(Cyan, center, cosPoint, 4f, cap = StrokeCap.Round)
            drawLine(Violet, center, sinPoint, 3.4f, cap = StrokeCap.Round)
        }
        if (showTangents) {
            val tangentX = center.x + radius
            val tangentY = center.y - tan(angle).toFloat().coerceIn(-3f, 3f) * radius
            drawLine(Cyan.copy(alpha = .75f), Offset(tangentX, center.y - radius * 1.55f), Offset(tangentX, center.y + radius * 1.55f), 2f)
            drawLine(Cyan.copy(alpha = .75f), p, Offset(tangentX, tangentY), 2.8f)
        }
        drawArc(Violet, startAngle = -angleDegrees, sweepAngle = angleDegrees, useCenter = false, topLeft = Offset(center.x - 64f, center.y - 64f), size = Size(128f, 128f), style = Stroke(4f, cap = StrokeCap.Round))
        listOf(0.0, PI / 2, PI, 3 * PI / 2).forEach { theta ->
            drawRadiantPoint(circlePoint(theta), Cyan, when (theta) {
                0.0 -> "(1, 0)"
                PI / 2 -> "(0, 1)"
                PI -> "(-1, 0)"
                else -> "(0, -1)"
            })
        }
        drawRadiantPoint(center, Color.White, "O")
        drawRadiantPoint(p, Violet, "P (${trim(cos(angle))}, ${trim(sin(angle))})")
        drawTrigText("theta", center.x + 68f, center.y - 24f, Violet)
        drawTrigText("cos theta", (center.x + cosPoint.x) / 2f, center.y + 34f, Cyan)
        drawTrigText("sin theta", p.x + 12f, (center.y + p.y) / 2f, Violet)
        drawTrigText("tan theta", center.x + radius + 22f, center.y - 82f, Cyan)
        drawQuadrantCards(center, radius)
        drawRightTriangleCard(Offset(size.width * .76f, size.height * .48f), angleDegrees)
        drawIdentitiesCard(Offset(size.width * .76f, size.height * .64f))
        if (showWave) {
            val waveTopLeft = Offset(size.width * .15f, size.height * .68f)
            val waveSize = Size(size.width * .56f, size.height * .2f)
            drawSineWavePane(waveTopLeft, waveSize, angleDegrees, transform, function, visibleFunctions, showAsymptotes, harmonics, lineStyle, paletteShift)
            drawTrigTransformHandles(waveTopLeft, waveSize, transform)
            if (equationTarget != null) {
                val origin = Offset(waveTopLeft.x + 42f, waveTopLeft.y + waveSize.height / 2f)
                val waveWidth = waveSize.width - 70f
                val scaleY = waveSize.height * .14f
                val targetY = origin.y - equationTarget.coerceIn(-3.2f, 3.2f) * scaleY
                drawLine(Amber, Offset(origin.x, targetY), Offset(origin.x + waveWidth, targetY), 2.4f)
                equationRoots.forEach { root ->
                    val x = origin.x + (root / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * waveWidth
                    drawRadiantPoint(Offset(x, targetY), Green, "x=${radianLabel(Math.toDegrees(root))}")
                }
            }
        }
        if (polarSamples.isNotEmpty()) drawPolarCurvePane(Offset(size.width * .73f, size.height * .18f), Size(size.width * .24f, size.height * .25f), polarSamples)
        drawContext.canvas.restore()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigGrid() {
    val step = 38f
    var x = 0f
    while (x <= size.width) {
        drawLine(Grid.copy(alpha = .35f), Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(Grid.copy(alpha = .35f), Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawQuadrantCards(center: Offset, radius: Float) {
    val items = listOf(
        "QI\nsin +\ncos +\ntan +" to Offset(center.x + radius * .92f, center.y - radius * 1.06f),
        "QII\nsin +\ncos -\ntan -" to Offset(center.x - radius * .95f, center.y - radius * 1.05f),
        "QIII\nsin -\ncos -\ntan +" to Offset(center.x - radius * 1.04f, center.y + radius * .9f),
        "QIV\nsin -\ncos +\ntan -" to Offset(center.x + radius * .93f, center.y + radius * .92f),
    )
    items.forEach { (text, p) ->
        drawRoundRect(SurfaceA, topLeft = p, size = Size(86f, 104f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
        drawRoundRect(Color(0x5548BFFF), topLeft = p, size = Size(86f, 104f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f), style = Stroke(1.5f))
        text.lines().forEachIndexed { index, line -> drawTrigText(line, p.x + 14f, p.y + 24f + index * 22f, if (index == 0) Ink else Muted) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRightTriangleCard(topLeft: Offset, angleDegrees: Float) {
    drawRoundRect(SurfaceA, topLeft = topLeft, size = Size(250f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Color(0x66985DFF), topLeft = topLeft, size = Size(250f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.8f))
    val a = Offset(topLeft.x + 40f, topLeft.y + 130f)
    val b = Offset(topLeft.x + 190f, topLeft.y + 130f)
    val c = Offset(topLeft.x + 190f, topLeft.y + 35f)
    drawLine(Violet, a, b, 3f)
    drawLine(Violet, b, c, 3f)
    drawLine(Violet, a, c, 3f)
    drawTrigText("Right Triangle", topLeft.x + 18f, topLeft.y + 28f, Cyan)
    drawTrigText("${trim(angleDegrees.toDouble())} deg", topLeft.x + 118f, topLeft.y + 102f, Ink)
    drawTrigText("1", topLeft.x + 112f, topLeft.y + 154f, Ink)
    drawTrigText("tan theta", topLeft.x + 198f, topLeft.y + 85f, Ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIdentitiesCard(topLeft: Offset) {
    val lines = listOf(
        "Trig Identities",
        "sin^2 theta + cos^2 theta = 1",
        "tan theta = sin theta / cos theta",
        "1 + tan^2 theta = sec^2 theta",
        "sin(-theta) = -sin theta",
        "cos(-theta) = cos theta",
    )
    drawRoundRect(SurfaceA, topLeft = topLeft, size = Size(310f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Color(0x66985DFF), topLeft = topLeft, size = Size(310f, 170f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.8f))
    lines.forEachIndexed { index, line -> drawTrigText(line, topLeft.x + 18f, topLeft.y + 28f + index * 24f, if (index == 0) Cyan else Ink) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSineWavePane(
    topLeft: Offset,
    paneSize: Size,
    angleDegrees: Float,
    transform: TrigTransform,
    function: TrigFunction,
    functions: Set<TrigFunction> = setOf(function),
    showAsymptotes: Boolean = false,
    harmonics: List<HarmonicComponent> = emptyList(),
    lineStyle: TrigLineStyle = TrigLineStyle.Solid,
    paletteShift: Int = 0,
) {
    drawRoundRect(SurfaceA, topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f))
    drawRoundRect(Color(0x5548BFFF), topLeft = topLeft, size = paneSize, cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f), style = Stroke(1.8f))
    val origin = Offset(topLeft.x + 42f, topLeft.y + paneSize.height / 2f)
    val width = paneSize.width - 70f
    val scaleY = paneSize.height * .14f
    drawLine(Color.White.copy(.8f), origin, Offset(topLeft.x + paneSize.width - 20f, origin.y), 2f)
    drawLine(Color.White.copy(.8f), Offset(origin.x, topLeft.y + 18f), Offset(origin.x, topLeft.y + paneSize.height - 18f), 2f)
    val palette = listOf(Violet, Cyan, Amber, Green, Color(0xFFFF6DAE), Color(0xFFB9FF66))
    functions.forEach { plotted ->
        val path = Path()
        var drawing = false
        for (i in 0..360) {
            val t = i / 360f
            val x = origin.x + t * width
            val value = transform.valueAt(t * Math.PI * 2, plotted)
            val y = origin.y - value.toFloat().coerceIn(-3.2f, 3.2f) * scaleY
            if (!value.isFinite() || kotlin.math.abs(value) > 12) drawing = false
            else if (!drawing) { path.moveTo(x, y); drawing = true } else path.lineTo(x, y)
        }
        val effect = when (lineStyle) { TrigLineStyle.Solid -> null; TrigLineStyle.Dashed -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 9f)); TrigLineStyle.Dotted -> androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 8f)) }
        drawPath(path, palette[(plotted.ordinal + paletteShift).mod(palette.size)], style = Stroke(if (plotted == function) 3.5f else 2.3f, cap = StrokeCap.Round, pathEffect = effect))
    }
    if (showAsymptotes && functions.any { it in setOf(TrigFunction.Tangent, TrigFunction.Secant, TrigFunction.Cosecant, TrigFunction.Cotangent) }) {
        val values = if (functions.any { it in setOf(TrigFunction.Tangent, TrigFunction.Secant) }) listOf(.25f, .75f) else listOf(0f, .5f, 1f)
        values.forEach { position -> drawLine(Amber.copy(.7f), Offset(origin.x + position * width, topLeft.y + 18f), Offset(origin.x + position * width, topLeft.y + paneSize.height - 18f), 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))) }
    }
    if (harmonics.isNotEmpty()) {
        val path = Path()
        for (i in 0..360) {
            val t = i / 360f
            val value = InteractiveTrigEngine.harmonicValue(t * Math.PI * 2, harmonics)
            val point = Offset(origin.x + t * width, origin.y - value.toFloat().coerceIn(-3.2f, 3.2f) * scaleY)
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, Green, style = Stroke(4f, cap = StrokeCap.Round))
    }
    val t = ((angleDegrees % 360f) + 360f) % 360f / 360f
    val px = origin.x + t * width
    val currentValue = transform.valueAt(Math.toRadians(angleDegrees.toDouble()), function)
    val py = origin.y - currentValue.toFloat().coerceIn(-3.2f, 3.2f) * scaleY
    drawLine(Violet.copy(alpha = .7f), Offset(px, origin.y), Offset(px, py), 2f)
    drawRadiantPoint(Offset(px, py), Violet, "${trim(angleDegrees.toDouble())} deg")
    drawTrigText("${function.name}: A f(2π(x-h)/P)+k", topLeft.x + 22f, topLeft.y + 28f, Ink)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigTransformHandles(topLeft: Offset, paneSize: Size, transform: TrigTransform) {
    val origin = Offset(topLeft.x + 42f, topLeft.y + paneSize.height / 2f)
    val width = paneSize.width - 70f
    val scaleY = paneSize.height * .14f
    val handles = listOf(
        Triple("A", Offset(origin.x + width, origin.y - transform.amplitude.toFloat() * scaleY), Violet),
        Triple("P", Offset(origin.x + (transform.period / (4 * Math.PI)).toFloat().coerceIn(.08f, 1f) * width, topLeft.y + paneSize.height - 20f), Cyan),
        Triple("phase", Offset(origin.x + ((transform.phaseShift + Math.PI) / (2 * Math.PI)).toFloat().coerceIn(0f, 1f) * width, topLeft.y + 20f), Amber),
        Triple("k", Offset(origin.x - 16f, origin.y - transform.verticalShift.toFloat() * scaleY), Green),
    )
    handles.forEach { (label, point, color) -> drawCircle(SurfaceA, 16f, point); drawCircle(color, 13f, point, style = Stroke(4f)); drawTrigText(label, point.x + 18f, point.y - 8f, color) }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolarCurvePane(topLeft: Offset, paneSize: Size, samples: List<Vec2>) {
    drawRoundRect(SurfaceA.copy(.94f), topLeft, paneSize, androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    drawRoundRect(Cyan.copy(.65f), topLeft, paneSize, androidx.compose.ui.geometry.CornerRadius(18f, 18f), style = Stroke(1.6f))
    val center = topLeft + Offset(paneSize.width / 2, paneSize.height / 2)
    val radius = min(paneSize.width, paneSize.height) * .39f
    repeat(4) { index -> drawCircle(Grid.copy(.65f), radius * (index + 1) / 4f, center, style = Stroke(1f)) }
    drawLine(Grid, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1f)
    drawLine(Grid, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1f)
    val extent = samples.maxOfOrNull { maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y)) }?.coerceAtLeast(1.0) ?: 1.0
    val path = Path()
    samples.forEachIndexed { index, point ->
        val screen = Offset(center.x + (point.x / extent).toFloat() * radius, center.y - (point.y / extent).toFloat() * radius)
        if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
    }
    drawPath(path, Violet, style = Stroke(3f, cap = StrokeCap.Round))
    drawTrigText("Polar explorer", topLeft.x + 14f, topLeft.y + 26f, Cyan)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrigText(text: String, x: Float, y: Float, color: Color) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 24f
            isAntiAlias = true
        }
        drawText(text, x, y, paint)
    }
}

@Composable
private fun Projected3DCanvas(
    modifier: Modifier,
    solids: List<Solid>,
    vectors: List<Vector3D>,
    selectedIndex: Int,
    visibleSolidIndices: Set<Int>,
    selectedVectorIndex: Int,
    rx: Float,
    ry: Float,
    rz: Float,
    zoom: Float,
    cameraPan: Offset,
    transformMode: Transform3DMode,
    wire: Boolean,
    perspective: Boolean,
    selectionMode: Selection3DMode,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionPlane: EditableSectionPlane,
    clipSection: Boolean,
    onSelect: (Int) -> Unit,
    onSubSelect: (SubObjectSelection?) -> Unit,
    onSelectVector: (Int) -> Unit,
    onSolidDragStart: (Int) -> Unit,
    onSolidMove: (Int, Vec3) -> Unit,
    onSolidRotate: (Int, Vec3) -> Unit,
    onSolidScale: (Int, Double) -> Unit,
    onSolidAxisScale: (Int, TransformGizmoAxis, Double) -> Unit,
    onSectionPlaneMove: (Double) -> Unit,
    onSolidDragEnd: () -> Unit,
    onSolidDragCancel: () -> Unit,
    onSolidDropDelete: (Int) -> Unit,
    onVectorDragStart: (Int) -> Unit,
    onVectorMove: (Int, Vec3) -> Unit,
    onVectorDragEnd: () -> Unit,
    onVectorDragCancel: () -> Unit,
    onOrbit: (Float, Float) -> Unit,
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onResetCamera: () -> Unit,
    onEmptyTap: () -> Unit,
    onGestureModeChange: (GestureMode) -> Unit,
) {
    var lastTapAt by remember { mutableStateOf(0L) }
    var lastSubPickAt by remember { mutableStateOf(0L) }
    var activeGizmoAxis by remember { mutableStateOf<TransformGizmoAxis?>(null) }
    var stylusHoverSolid by remember { mutableStateOf<Int?>(null) }
    val currentSolids by rememberUpdatedState(solids)
    val currentVectors by rememberUpdatedState(vectors)
    val currentRx by rememberUpdatedState(rx)
    val currentRy by rememberUpdatedState(ry)
    val currentRz by rememberUpdatedState(rz)
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    val currentPerspective by rememberUpdatedState(perspective)
    val currentSelectionMode by rememberUpdatedState(selectionMode)
    val currentSubSelection by rememberUpdatedState(subSelection)
    val currentSectionEnabled by rememberUpdatedState(sectionEnabled)
    val currentClipSection by rememberUpdatedState(clipSection)
    val currentSectionPlane by rememberUpdatedState(sectionPlane)
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentVisibleSolidIndices by rememberUpdatedState(visibleSolidIndices)
    val structuredDescription = remember(solids, subSelection, selectedIndex) {
        val selectedHit = subSelection?.let { selection ->
            solids.getOrNull(selection.solidIndex)?.let { solid ->
                val mode = when (selection.mode) { Selection3DMode.Vertex -> SpatialSubObjectType.Vertex; Selection3DMode.Edge -> SpatialSubObjectType.Edge; Selection3DMode.Face -> SpatialSubObjectType.Face; Selection3DMode.Object -> SpatialSubObjectType.Face }
                com.indianservers.aiexplorer.core.SpatialSubObjectHit(selection.solidIndex, mode, selection.index, 0.0, 0.0, subObjectAnchorWorld(solid, selection))
            }
        }
        com.indianservers.aiexplorer.core.SpatialAccessibilityEngine.describe(solids, selectedHit).joinToString(". ") { node -> node.description + ". " + node.measurements.joinToString() }
    }
    Canvas(
        modifier
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Delete || event.key == Key.Backspace)) {
                    onSolidDropDelete(currentSelectedIndex); true
                } else false
            }
            .focusable()
            .pointerInput(solids, rx, ry, rz, zoom, cameraPan, visibleSolidIndices) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val hover = event.changes.firstOrNull { it.type == androidx.compose.ui.input.pointer.PointerType.Stylus && !it.pressed }
                        stylusHoverSolid = hover?.let { change ->
                            val center = Offset(size.width * .52f, size.height * .45f) + cameraPan
                            val scale = 74f * zoom
                            visibleSolidIndices.minByOrNull { index ->
                                val point = project(rotate(solids[index].position, rx, ry, rz), center, scale, perspective)
                                (point - change.position).getDistance()
                            }?.takeIf { index ->
                                val point = project(rotate(solids[index].position, rx, ry, rz), center, scale, perspective)
                                (point - change.position).getDistance() < 120f
                            }
                        }
                    }
                }
            }
            .pointerInput(transformMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val gestureSolids = currentSolids
                    val gestureVectors = currentVectors
                    val gestureRx = currentRx
                    val gestureRy = currentRy
                    val gestureRz = currentRz
                    val center = Offset(size.width * .52f, size.height * .45f) + currentPan
                    val scale = 74f * currentZoom
                    fun vectorDistance(index: Int, target: Offset): Float {
                        val vector = gestureVectors[index]
                        val a = project(rotate(vector.start, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        val b = project(rotate(vector.end, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        return pointSegmentDistance(target, a, b)
                    }
                    fun solidDistance(index: Int, target: Offset): Float {
                        val projected = project(rotate(gestureSolids[index].position, gestureRx, gestureRy, gestureRz), center, scale, currentPerspective)
                        return (projected - target).getDistance()
                    }

                    fun pickSubObject(target: Offset): SubObjectSelection? {
                        val mode = currentSelectionMode
                        if (mode == Selection3DMode.Object) return null
                        val projectedMeshes = gestureSolids.mapIndexedNotNull { solidIndex, solid ->
                            if (solidIndex !in currentVisibleSolidIndices) return@mapIndexedNotNull null
                            val mesh = SolidMeshFactory.create(solid)
                            ProjectedSpatialMesh(solidIndex, mesh.vertices.map { vertex ->
                                val world = solidLocalToWorld(solid, vertex); val camera = rotate(world, gestureRx, gestureRy, gestureRz)
                                val screen = project(camera, center, scale, currentPerspective)
                                ProjectedSpatialPoint(Vec2(screen.x.toDouble(), screen.y.toDouble()), camera.z, world)
                            }, mesh.edges, mesh.faces)
                        }
                        val type = when (mode) {
                            Selection3DMode.Vertex -> SpatialSubObjectType.Vertex
                            Selection3DMode.Edge -> SpatialSubObjectType.Edge
                            Selection3DMode.Face -> SpatialSubObjectType.Face
                            Selection3DMode.Object -> return null
                        }
                        val tolerance = when (mode) { Selection3DMode.Vertex -> 28.0; Selection3DMode.Edge -> 22.0; Selection3DMode.Face -> 18.0; Selection3DMode.Object -> 0.0 }
                        val hits = SpatialSubObjectPicker.pickAll(projectedMeshes, Vec2(target.x.toDouble(), target.y.toDouble()), type, tolerance)
                        val now = System.currentTimeMillis()
                        val current = currentSubSelection
                        val currentHit = hits.indexOfFirst { current != null && it.solidIndex == current.solidIndex && it.index == current.index }
                        val hit = if (now - lastSubPickAt < 520L && currentHit >= 0) hits.getOrNull((currentHit + 1) % hits.size.coerceAtLeast(1)) else hits.firstOrNull()
                        lastSubPickAt = now
                        return hit?.let { SubObjectSelection(it.solidIndex, mode, it.index) }
                    }

                    val selectedForGizmo = gestureSolids.getOrNull(currentSelectedIndex)?.takeIf { currentSelectedIndex in currentVisibleSolidIndices }
                    val gizmoHandles = selectedForGizmo?.let { solid -> projectedGizmoHandles(solid, currentSubSelection?.takeIf { it.solidIndex == currentSelectedIndex }, gestureRx, gestureRy, gestureRz, center, scale, currentPerspective) }.orEmpty()
                    val gizmoKind = when (transformMode) { Transform3DMode.Move -> TransformGizmoKind.Move; Transform3DMode.Rotate -> TransformGizmoKind.Rotate; Transform3DMode.Scale -> TransformGizmoKind.Scale }
                    val gizmoHit = TransformGizmoEngine.hitTest(Vec2(down.position.x.toDouble(), down.position.y.toDouble()), gizmoKind, gizmoHandles)
                    val sectionHandle = if ((currentSectionEnabled || currentClipSection) && selectedForGizmo != null) projectedSectionHandle(selectedForGizmo, currentSectionPlane, gestureRx, gestureRy, gestureRz, center, scale, currentPerspective) else null
                    val sectionHit = sectionHandle?.let { (it.end - down.position).getDistance() <= 24f || pointSegmentDistance(down.position, it.start, it.end) <= 12f } == true
                    if (gizmoHit != null && !sectionHit) {
                        activeGizmoAxis = gizmoHit.axis
                        onSolidDragStart(currentSelectedIndex)
                    }

                    val subHit = if (gizmoHit == null && !sectionHit) pickSubObject(down.position) else null
                    subHit?.let {
                        onSelect(it.solidIndex)
                        onSubSelect(it)
                    }
                    var vectorIndex = if (gizmoHit == null && !sectionHit && subHit == null && currentSelectionMode == Selection3DMode.Object) gestureVectors.indices.minByOrNull { vectorDistance(it, down.position) }
                        ?.takeIf { vectorDistance(it, down.position) < 42f }
                    else null
                    var solidIndex = if (gizmoHit != null && !sectionHit) currentSelectedIndex else if (!sectionHit && vectorIndex == null) {
                        gestureSolids.indices.filter(currentVisibleSolidIndices::contains).minByOrNull { solidDistance(it, down.position) }
                            ?.takeIf { solidDistance(it, down.position) < 104f }
                    } else null
                    if (currentSelectionMode != Selection3DMode.Object || subHit != null) solidIndex = null
                    vectorIndex?.let {
                        onSelectVector(it)
                        onVectorDragStart(it)
                    }
                    solidIndex?.takeIf { gizmoHit == null }?.let {
                        onSelect(it)
                        onSolidDragStart(it)
                    }
                    onGestureModeChange(when {
                        sectionHit -> GestureMode.Moving
                        vectorIndex != null -> GestureMode.Moving
                        solidIndex != null && transformMode == Transform3DMode.Move -> GestureMode.Moving
                        solidIndex != null && transformMode == Transform3DMode.Rotate -> GestureMode.Rotating
                        solidIndex != null -> GestureMode.Resizing
                        else -> GestureMode.Selecting
                    })

                    var total = Offset.Zero
                    var moved = false
                    var transformed = false
                    var objectCancelled = false
                    var latestPosition = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull()?.let { latestPosition = it.position }
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            if (!objectCancelled) {
                                if (solidIndex != null) onSolidDragCancel()
                                if (vectorIndex != null) onVectorDragCancel()
                                solidIndex = null
                                vectorIndex = null
                                objectCancelled = true
                            }
                            onPan(event.calculatePan())
                            onZoom(event.calculateZoom())
                            onGestureModeChange(if (abs(event.calculateZoom() - 1f) > .002f) GestureMode.Zooming else GestureMode.Panning)
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                total += delta
                                moved = moved || total.getDistance() > 8f
                                when {
                                    sectionHit -> {
                                        val handle = requireNotNull(sectionHandle)
                                        val direction = handle.end - handle.start
                                        val length = direction.getDistance().coerceAtLeast(1f)
                                        val unit = direction / length
                                        onSectionPlaneMove(((delta.x * unit.x + delta.y * unit.y) / length).toDouble())
                                    }
                                    vectorIndex != null -> onVectorMove(
                                        vectorIndex,
                                        Vec3((total.x / scale).toDouble(), 0.0, (total.y / scale).toDouble()),
                                    )
                                    solidIndex != null && gizmoHit != null -> when (transformMode) {
                                        Transform3DMode.Move -> onSolidMove(
                                            solidIndex,
                                            TransformGizmoEngine.constrainTranslation(Vec3((total.x / scale).toDouble(), (-total.y / scale).toDouble(), (-total.y / scale).toDouble()), gizmoHit.axis),
                                        )
                                        Transform3DMode.Rotate -> onSolidRotate(solidIndex, TransformGizmoEngine.rotationDelta(gizmoHit.axis, (total.x - total.y) * .35))
                                        Transform3DMode.Scale -> {
                                            val factor = (1.0 + (total.x - total.y) / 260.0).coerceAtLeast(.2)
                                            if (gizmoHit.axis == TransformGizmoAxis.Uniform) onSolidScale(solidIndex, factor) else onSolidAxisScale(solidIndex, gizmoHit.axis, factor)
                                        }
                                    }
                                    solidIndex != null -> when (transformMode) {
                                        Transform3DMode.Move -> onSolidMove(
                                            solidIndex,
                                            Vec3((total.x / scale).toDouble(), 0.0, (total.y / scale).toDouble()),
                                        )
                                        Transform3DMode.Rotate -> onSolidRotate(
                                            solidIndex,
                                            Vec3((-total.y * .35f).toDouble(), (total.x * .35f).toDouble(), 0.0),
                                        )
                                        Transform3DMode.Scale -> onSolidScale(
                                            solidIndex,
                                            (1.0 + (total.x - total.y) / 260.0).coerceAtLeast(.2),
                                        )
                                    }
                                    subHit != null -> Unit
                                    else -> { onOrbit(delta.x * .35f, -delta.y * .25f); onGestureModeChange(GestureMode.Rotating) }
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }

                    if (solidIndex != null) {
                        val overDelete = latestPosition.x in (size.width * .32f)..(size.width * .68f) && latestPosition.y >= size.height * .78f
                        if (overDelete) {
                            onSolidDragCancel()
                            onSolidDropDelete(solidIndex)
                        } else onSolidDragEnd()
                    }
                    activeGizmoAxis = null
                    if (vectorIndex != null) onVectorDragEnd()
                    if (!moved && !transformed && solidIndex == null && vectorIndex == null && subHit == null && !sectionHit && gizmoHit == null) {
                        onEmptyTap()
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            onResetCamera()
                            lastTapAt = 0L
                        } else {
                            lastTapAt = now
                        }
                    }
                    onGestureModeChange(GestureMode.Idle)
                }
            }
            .semantics { contentDescription = "Interactive 3D workspace with object, vertex, edge and face selection. $structuredDescription" },
    ) {
        val center = Offset(size.width * .52f, size.height * .45f) + cameraPan
        drawPerspectiveGrid(center)
        vectors.forEachIndexed { index, vector ->
            drawVector3D(vector, rx, ry, rz, center, 74f * zoom, if (index == selectedVectorIndex) Amber else Green, index == selectedVectorIndex, perspective)
        }
        solids.forEachIndexed { index, solid ->
            if (index !in visibleSolidIndices) return@forEachIndexed
            val color = if (index == selectedIndex) Cyan else if (index == stylusHoverSolid) Amber else if (index % 2 == 0) Violet else Green
            drawSolidProjection(
                solid, solid.position, rx, ry, rz, center, 74f * zoom, color, wire, index == selectedIndex,
                perspective, subSelection?.takeIf { it.solidIndex == index }, sectionEnabled && index == selectedIndex,
                sectionPlane, clipSection && index == selectedIndex,
            )
            if (index == stylusHoverSolid) {
                val hover = project(rotate(solid.position, rx, ry, rz), center, 74f * zoom, perspective)
                drawGraphLabel("Stylus preview · ${solid.type.name}", hover + Offset(14f, -22f), Amber)
            }
            if (index == selectedIndex) {
                val handles = projectedGizmoHandles(solid, subSelection?.takeIf { it.solidIndex == index }, rx, ry, rz, center, 74f * zoom, perspective)
                drawTransformGizmo(handles, transformMode, activeGizmoAxis)
                if (sectionEnabled || clipSection) projectedSectionHandle(solid, sectionPlane, rx, ry, rz, center, 74f * zoom, perspective)?.let { handle ->
                    drawLine(Amber, handle.start, handle.end, 4f, cap = StrokeCap.Round)
                    drawCircle(Color.White, 9f, handle.end)
                    drawGraphLabel("drag plane", handle.end + Offset(10f, -10f), Amber)
                }
            }
        }
    }
}

@Composable
private fun SurfaceCanvas3D(
    modifier: Modifier,
    expression: String,
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    additionalMeshes: List<Pair<com.indianservers.aiexplorer.core.SurfaceMesh, Color>>,
    gradientPath: List<Vec3>,
    gradientPathIndex: Int,
    rotation: Float,
    tilt: Float,
    roll: Float,
    zoom: Float,
    cameraPan: Offset,
    sliceZ: Double,
    trace: Vec2,
    showWireframe: Boolean,
    showContours: Boolean,
    showSlice: Boolean,
    showGradient: Boolean,
    showBox: Boolean,
    activeTool: SurfaceTool,
    onRotate: (Float) -> Unit,
    onTilt: (Float) -> Unit,
    onRoll: (Float) -> Unit,
    onPan: (Offset) -> Unit,
    onZoom: (Float) -> Unit,
    onResetCamera: () -> Unit,
    onTrace: (Vec2) -> Unit,
) {
    val engine = remember { ExpressionEngine() }
    val calculus = remember { SurfaceCalculus() }
    val analysis = remember(expression, trace) { runCatching { calculus.analyze(expression, trace.x, trace.y) }.getOrNull() }
    var lastTapAt by remember { mutableStateOf(0L) }
    val currentZoom by rememberUpdatedState(zoom)
    val currentPan by rememberUpdatedState(cameraPan)
    val currentMesh by rememberUpdatedState(mesh)
    val currentRotation by rememberUpdatedState(rotation)
    val currentTilt by rememberUpdatedState(tilt)
    val currentRoll by rememberUpdatedState(roll)
    val currentAnalysis by rememberUpdatedState(analysis)
    Canvas(
        modifier
            .pointerInput(activeTool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width * .5f, size.height * .5f) + currentPan
                    val scale = 54f * currentZoom
                    fun screen(value: Vec3) = project(rotate(value, currentTilt, currentRotation, currentRoll), center, scale)
                    val projected = currentMesh?.vertices?.map { value -> screen(value).let { Vec2(it.x.toDouble(), it.y.toDouble()) } }.orEmpty()
                    fun surfaceAt(position: Offset, tolerance: Double = 52.0): Vec2? = currentMesh?.let { surfaceMesh ->
                        SurfaceAnalysisHandleEngine.pick(surfaceMesh, projected, Vec2(position.x.toDouble(), position.y.toDouble()), tolerance)?.let { Vec2(it.x, it.y) }
                    }
                    val handleScreen = currentAnalysis?.point?.let(::screen)
                    val draggingHandle = handleScreen?.let { (it - down.position).getDistance() <= 34f } == true
                    var moved = false
                    var transformed = false
                    var total = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            onPan(event.calculatePan())
                            onZoom(event.calculateZoom())
                            onRoll(event.calculateRotation())
                            transformed = true
                            event.changes.forEach { it.consume() }
                        } else {
                            val change = event.changes.firstOrNull()
                            val delta = change?.positionChange() ?: Offset.Zero
                            if (delta.getDistance() > 0f) {
                                total += delta
                                moved = moved || total.getDistance() > 8f
                                if (draggingHandle || activeTool in setOf(SurfaceTool.Trace, SurfaceTool.Gradient)) {
                                    surfaceAt(change!!.position)?.let(onTrace)
                                } else {
                                    onRotate(delta.x * .35f)
                                    onTilt(-delta.y * .18f)
                                }
                                change!!.consume()
                            }
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                    if (!moved && !transformed) {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt < 320L) {
                            onResetCamera()
                            lastTapAt = 0L
                        } else {
                            if (activeTool in setOf(SurfaceTool.Trace, SurfaceTool.Gradient)) surfaceAt(down.position)?.let(onTrace)
                            lastTapAt = now
                        }
                    }
                }
            }
            .semantics { contentDescription = "Interactive 3D graph: drag to orbit, two fingers pan, pinch zoom, twist roll, and tap axis or plane views" },
    ) {
        val center = Offset(size.width * .5f, size.height * .5f) + cameraPan
        val scale = 54f * zoom
        fun map(v: Vec3) = project(rotate(v, tilt, rotation, roll), center, scale)
        drawPerspectiveGrid(center)
        drawCoordinatePlanes3D(::map)
        if (showBox) drawSurfaceBox(::map)
        mesh?.vertices?.chunked(mesh.columns)?.forEachIndexed { rowIndex, row ->
            row.zipWithNext().forEachIndexed { columnIndex, (a, b) ->
                val alpha = if (showWireframe || rowIndex % 3 == 0 || columnIndex % 3 == 0) .78f else .28f
                drawLine(Cyan.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.8f else 1.1f)
            }
        }
        mesh?.vertices?.groupBy { it.y }?.values?.forEachIndexed { index, col ->
            col.zipWithNext().forEach { (a, b) ->
                val alpha = if (showWireframe || index % 3 == 0) .58f else .22f
                drawLine(Violet.copy(alpha = alpha), map(a), map(b), if (showWireframe) 1.3f else .9f)
            }
        }
        additionalMeshes.forEach { (surface, color) ->
            surface.vertices.chunked(surface.columns).forEach { row -> row.zipWithNext().forEach { (a, b) -> drawLine(color.copy(.72f), map(a), map(b), 1.5f) } }
            surface.vertices.groupBy { it.y }.values.forEach { column -> column.zipWithNext().forEach { (a, b) -> drawLine(color.copy(.42f), map(a), map(b), 1.1f) } }
        }
        if (gradientPath.size >= 2) {
            val visiblePath = gradientPath.take((gradientPathIndex + 1).coerceAtLeast(2))
            visiblePath.zipWithNext().forEach { (a, b) -> drawLine(Green, map(a), map(b), 4f, cap = StrokeCap.Round) }
            visiblePath.lastOrNull()?.let { drawRadiantPoint(map(it), Green, "gradient path") }
        }
        if (showContours) drawSurfaceContours(mesh, ::map)
        if (showSlice) drawSurfaceSlice(mesh, sliceZ, ::map)
        val compiled = runCatching { engine.compile(stripEquation(expression).replace("y", "yy")) }.getOrNull()
        compiled?.let {
            val z = runCatching { it.eval(mapOf("x" to trace.x, "yy" to trace.y)) }.getOrDefault(Double.NaN)
            if (z.isFinite()) {
                val point = Vec3(trace.x, trace.y, z.coerceIn(-8.0, 8.0))
                val screenPoint = map(point)
                drawRadiantPoint(screenPoint, Amber, "(${trim(trace.x)}, ${trim(trace.y)}, ${trim(z)})")
                if (showGradient) analysis?.let { differential -> drawSurfaceAnalysisHandle(differential, ::map) }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoordinatePlanes3D(map: (Vec3) -> Offset) {
    fun plane(points: List<Vec3>, color: Color) {
        val projected = points.map(map)
        val path = Path().apply {
            moveTo(projected.first().x, projected.first().y)
            projected.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        drawPath(path, color.copy(alpha = .045f))
        drawPath(path, color.copy(alpha = .22f), style = Stroke(1.2f))
    }
    plane(listOf(Vec3(-3.0, -3.0, 0.0), Vec3(3.0, -3.0, 0.0), Vec3(3.0, 3.0, 0.0), Vec3(-3.0, 3.0, 0.0)), Cyan)
    plane(listOf(Vec3(-3.0, 0.0, -1.0), Vec3(3.0, 0.0, -1.0), Vec3(3.0, 0.0, 6.0), Vec3(-3.0, 0.0, 6.0)), Green)
    plane(listOf(Vec3(0.0, -3.0, -1.0), Vec3(0.0, 3.0, -1.0), Vec3(0.0, 3.0, 6.0), Vec3(0.0, -3.0, 6.0)), Violet)

    val origin = map(Vec3(0.0, 0.0, 0.0))
    val x = map(Vec3(3.7, 0.0, 0.0))
    val y = map(Vec3(0.0, 3.7, 0.0))
    val z = map(Vec3(0.0, 0.0, 4.8))
    drawLine(Color(0xFFFF5B68), origin, x, 4f, cap = StrokeCap.Round)
    drawLine(Green, origin, y, 4f, cap = StrokeCap.Round)
    drawLine(Cyan, origin, z, 4f, cap = StrokeCap.Round)
    drawCircle(Color.White, 5f, origin)
    drawGraphLabel("X", x + Offset(8f, 0f), Color(0xFFFF5B68))
    drawGraphLabel("Y", y + Offset(8f, 0f), Green)
    drawGraphLabel("Z", z + Offset(8f, 0f), Cyan)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceBox(map: (Vec3) -> Offset) {
    val corners = listOf(
        Vec3(-3.0, -3.0, -1.0), Vec3(3.0, -3.0, -1.0), Vec3(3.0, 3.0, -1.0), Vec3(-3.0, 3.0, -1.0),
        Vec3(-3.0, -3.0, 7.0), Vec3(3.0, -3.0, 7.0), Vec3(3.0, 3.0, 7.0), Vec3(-3.0, 3.0, 7.0),
    )
    listOf(0 to 1, 1 to 2, 2 to 3, 3 to 0, 4 to 5, 5 to 6, 6 to 7, 7 to 4, 0 to 4, 1 to 5, 2 to 6, 3 to 7).forEach { (a, b) ->
        drawLine(Cyan.copy(.33f), map(corners[a]), map(corners[b]), 1.6f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceContours(
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    map: (Vec3) -> Offset,
) {
    val rows = mesh?.vertices?.chunked(mesh.columns) ?: return
    val levels = listOf(0.0, 1.0, 2.0, 4.0, 6.0)
    levels.forEachIndexed { levelIndex, level ->
        val color = if (levelIndex % 2 == 0) Cyan else Violet
        rows.forEach { row ->
            row.zipWithNext().forEach { (a, b) ->
                if ((a.z - level) * (b.z - level) <= 0.0 && abs(a.z - b.z) > 1e-6) {
                    val t = ((level - a.z) / (b.z - a.z)).coerceIn(0.0, 1.0)
                    val p = Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, level)
                    drawCircle(color.copy(.72f), 2.4f, map(p))
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceSlice(
    mesh: com.indianservers.aiexplorer.core.SurfaceMesh?,
    sliceZ: Double,
    map: (Vec3) -> Offset,
) {
    val rows = mesh?.vertices?.chunked(mesh.columns) ?: return
    rows.forEach { row ->
        val slicePoints = row.zipWithNext().mapNotNull { (a, b) ->
            if ((a.z - sliceZ) * (b.z - sliceZ) <= 0.0 && abs(a.z - b.z) > 1e-6) {
                val t = ((sliceZ - a.z) / (b.z - a.z)).coerceIn(0.0, 1.0)
                Vec3(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, sliceZ)
            } else {
                null
            }
        }
        slicePoints.zipWithNext().forEach { (a, b) -> drawLine(Amber, map(a), map(b), 3f, cap = StrokeCap.Round) }
    }
    drawGraphLabel("slice z = ${trim(sliceZ)}", Offset(size.width * .08f, size.height * .22f), Amber)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSurfaceAnalysisHandle(
    differential: com.indianservers.aiexplorer.core.SurfaceDifferential,
    map: (Vec3) -> Offset,
) {
    val point = differential.point
    val fx = differential.gradient.x; val fy = differential.gradient.y
    val tangentX = Vec3(1.0, 0.0, fx).normalized()
    val tangentY = Vec3(0.0, 1.0, fy).normalized()
    val extent = .62
    val corners = listOf(
        point - tangentX * extent - tangentY * extent,
        point + tangentX * extent - tangentY * extent,
        point + tangentX * extent + tangentY * extent,
        point - tangentX * extent + tangentY * extent,
    ).map(map)
    val plane = Path().apply { moveTo(corners[0].x, corners[0].y); corners.drop(1).forEach { lineTo(it.x, it.y) }; close() }
    drawPath(plane, Violet.copy(.18f)); drawPath(plane, Violet.copy(.78f), style = Stroke(2.5f))
    val normalEnd = point + differential.unitNormal * .9
    drawLine(Cyan, map(point), map(normalEnd), 4f, cap = StrokeCap.Round)
    drawCircle(Cyan, 8f, map(normalEnd)); drawGraphLabel("normal", map(normalEnd) + Offset(8f, -8f), Cyan)
    val gradientMagnitude = hypot(fx, fy)
    if (gradientMagnitude > 1e-10) {
        val dx = fx / gradientMagnitude; val dy = fy / gradientMagnitude
        val gradientEnd = point + Vec3(dx, dy, fx * dx + fy * dy).normalized() * .95
        drawLine(Green, map(point), map(gradientEnd), 5f, cap = StrokeCap.Round)
        drawCircle(Green, 9f, map(gradientEnd)); drawGraphLabel("∇f", map(gradientEnd) + Offset(8f, 14f), Green)
    }
    drawCircle(Amber.copy(.22f), 28f, map(point)); drawCircle(Color.White, 6f, map(point))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid(origin: Offset, scale: Float, settings: GraphAxisSettings = GraphAxisSettings()) {
    if (!scale.isFinite() || scale <= 0f) return
    val minX = (-origin.x / scale).toDouble()
    val maxX = ((size.width - origin.x) / scale).toDouble()
    val minY = ((origin.y - size.height) / scale).toDouble()
    val maxY = (origin.y / scale).toDouble()
    val xTicks = GraphViewport.ticks(minX, maxX)
    val yTicks = GraphViewport.ticks(minY, maxY)
    xTicks.forEach { value ->
        val x = origin.x + value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) drawLine(if (value == 0.0) Color.White.copy(.85f) else Grid, Offset(x, 0f), Offset(x, size.height), if (value == 0.0) 2f else 1f)
    }
    yTicks.forEach { value ->
        val y = origin.y - value.toFloat() * scale
        if (settings.gridVisible || value == 0.0) drawLine(if (value == 0.0) Color.White.copy(.85f) else Grid, Offset(0f, y), Offset(size.width, y), if (value == 0.0) 2f else 1f)
    }
    val labelAxisY = origin.y.coerceIn(24f, size.height - 8f)
    val labelAxisX = origin.x.coerceIn(8f, size.width - 38f)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(210, 225, 240, 250)
            textSize = 21f
            isAntiAlias = true
        }
        paint.textAlign = android.graphics.Paint.Align.CENTER
        xTicks.filterNot { it == 0.0 }.forEach { value ->
            val displayValue = if (settings.xLogarithmic) 10.0.pow(value) else value
            drawText(GraphUxEngine.format(displayValue, if (settings.xLogarithmic) AxisNumberFormat.Scientific else settings.format, GraphViewport.axisStep(minX, maxX)) + settings.xUnit, origin.x + value.toFloat() * scale, labelAxisY - 7f, paint)
        }
        paint.textAlign = android.graphics.Paint.Align.RIGHT
        yTicks.filterNot { it == 0.0 }.forEach { value ->
            val displayValue = if (settings.yLogarithmic) 10.0.pow(value) else value
            drawText(GraphUxEngine.format(displayValue, if (settings.yLogarithmic) AxisNumberFormat.Scientific else settings.format, GraphViewport.axisStep(minY, maxY)) + settings.yUnit, labelAxisX - 7f, origin.y - value.toFloat() * scale + 7f, paint)
        }
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.textSize = 25f
        drawText(settings.xName + if (settings.xLogarithmic) " (log)" else "", size.width - 70f, labelAxisY - 8f, paint)
        drawText(settings.yName + if (settings.yLogarithmic) " (log)" else "", labelAxisX + 9f, 26f, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerspectiveGrid(center: Offset) {
    for (i in -8..8) {
        drawLine(Grid, Offset(center.x + i * 48f, center.y - 330f), Offset(center.x + i * 78f, center.y + 330f), 1f)
        drawLine(Grid, Offset(center.x - 420f, center.y + i * 34f), Offset(center.x + 420f, center.y + i * 34f), 1f)
    }
    drawLine(Cyan, center, center + Offset(260f, 110f), 3f)
    drawLine(Cyan, center, center + Offset(-220f, 140f), 3f)
    drawLine(Cyan, center, center + Offset(0f, -260f), 3f)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRadiantPoint(position: Offset, color: Color, label: String) {
    drawCircle(color.copy(alpha = .18f), 26f, position)
    drawCircle(color, 11f, position)
    drawCircle(Color.White, 5f, position)
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.rgb((color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 28f
            isAntiAlias = true
        }
        drawText(label, position.x + 14f, position.y - 14f, paint)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructedShapes(
    points: List<Vec2>,
    activeTool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    if (points.size >= 2) {
        val a = tx(points[0])
        val b = tx(points[1])
        when (activeTool) {
            GeometryTool.Line -> {
                val direction = b - a
                val length = direction.getDistance().coerceAtLeast(1f)
                val unit = Offset(direction.x / length, direction.y / length)
                drawLine(Cyan.copy(alpha = .85f), a - unit * 2000f, a + unit * 2000f, 3.5f)
            }
            GeometryTool.Ray -> {
                val direction = b - a
                val length = direction.getDistance().coerceAtLeast(1f)
                val unit = Offset(direction.x / length, direction.y / length)
                drawLine(Cyan.copy(alpha = .85f), a, a + unit * 2000f, 3.5f)
            }
            GeometryTool.Segment, GeometryTool.Select, GeometryTool.Point, GeometryTool.Measure -> {
                drawLine(Violet.copy(alpha = .9f), a, b, 4f, cap = StrokeCap.Round)
            }
            GeometryTool.Circle -> {
                drawCircle(Cyan.copy(alpha = .8f), radius = (b - a).getDistance(), center = a, style = Stroke(3f))
            }
            GeometryTool.Rectangle, GeometryTool.Square -> {
                val width = b.x - a.x
                val height = if (activeTool == GeometryTool.Square) width else b.y - a.y
                val path = Path().apply {
                    moveTo(a.x, a.y)
                    lineTo(a.x + width, a.y)
                    lineTo(a.x + width, a.y + height)
                    lineTo(a.x, a.y + height)
                    close()
                }
                drawPath(path, Violet.copy(alpha = .18f))
                drawPath(path, Violet, style = Stroke(3f))
            }
            GeometryTool.Arc -> {
                drawArc(Cyan, startAngle = 200f, sweepAngle = 110f, useCenter = false, topLeft = Offset(a.x - 90f, a.y - 90f), size = Size(180f, 180f), style = Stroke(4f, cap = StrokeCap.Round))
            }
            else -> Unit
        }
    }

    if (points.size >= 3 && activeTool in setOf(GeometryTool.Triangle, GeometryTool.Polygon, GeometryTool.Select, GeometryTool.Measure)) {
        val shapePoints = if (activeTool == GeometryTool.Triangle) points.take(3) else points.takeLast(points.size.coerceAtMost(8))
        val path = Path().apply {
            val first = tx(shapePoints.first())
            moveTo(first.x, first.y)
            shapePoints.drop(1).forEach {
                val p = tx(it)
                lineTo(p.x, p.y)
            }
            close()
        }
        drawPath(path, Violet.copy(alpha = .14f))
        drawPath(path, Violet.copy(alpha = .9f), style = Stroke(3f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStoredShapes(
    points: List<Vec2>,
    shapes: List<Shape2D>,
    selectedShapes: Set<Int>,
    selectedShape: Int,
    tx: (Vec2) -> Offset,
) {
    shapes.forEachIndexed { index, shape ->
        if (!shape.visible) return@forEachIndexed
        val shapePoints = shape.pointIndices.mapNotNull { points.getOrNull(it) }
        val styled = when (shape.styleKey) {
            "cyan" -> Cyan
            "violet" -> Violet
            "green" -> Green
            else -> if (index % 2 == 0) Violet else Cyan
        }
        val accent = if (index in selectedShapes) Amber else styled
        drawShape2D(shape.type, shapePoints, tx, accent, filled = true)
    }
    val visibleJunctions = shapes.filter { it.visible }.flatMap { it.pointIndices }.distinct()
    val selectedJunctions = shapes.getOrNull(selectedShape)?.pointIndices.orEmpty().toSet()
    visibleJunctions.forEach { pointIndex ->
        val point = points.getOrNull(pointIndex) ?: return@forEach
        val selected = pointIndex in selectedJunctions
        val accent = if (selected) Amber else Cyan
        drawCircle(accent.copy(if (selected) .24f else .15f), if (selected) 22f else 16f, tx(point))
        drawCircle(Color.White.copy(.95f), if (selected) 8f else 6f, tx(point))
        drawCircle(accent, if (selected) 5f else 3.5f, tx(point))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstructionPreview(
    pending: List<Vec2>,
    tool: GeometryTool,
    tx: (Vec2) -> Offset,
) {
    if (pending.isEmpty()) return
    pending.forEachIndexed { index, point -> drawRadiantPoint(tx(point), Amber, "tap ${index + 1}") }
    val type = tool.toShape2DType() ?: return
    drawShape2D(type, pending, tx, Amber, filled = false)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShape2D(
    type: Shape2DType,
    points: List<Vec2>,
    tx: (Vec2) -> Offset,
    accent: Color,
    filled: Boolean,
) {
    if (points.isEmpty()) return
    fun offset(index: Int) = tx(points[index])
    val stroke = Stroke(if (filled) 3.5f else 2.4f, cap = StrokeCap.Round)
    when (type) {
        Shape2DType.Line, Shape2DType.Ray, Shape2DType.Segment, Shape2DType.Vector -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(1)
            val direction = b - a
            val length = direction.getDistance().coerceAtLeast(1f)
            val unit = Offset(direction.x / length, direction.y / length)
            val start = when (type) {
                Shape2DType.Line -> a - unit * 2000f
                else -> a
            }
            val end = when (type) {
                Shape2DType.Line, Shape2DType.Ray -> a + unit * 2000f
                else -> b
            }
            drawLine(accent, start, end, stroke.width, cap = StrokeCap.Round)
            if (type == Shape2DType.Vector) {
                val normal = Offset(-unit.y, unit.x)
                val head = 20f
                drawPath(
                    Path().apply {
                        moveTo(end.x, end.y)
                        lineTo((end - unit * head + normal * head * .55f).x, (end - unit * head + normal * head * .55f).y)
                        lineTo((end - unit * head - normal * head * .55f).x, (end - unit * head - normal * head * .55f).y)
                        close()
                    },
                    accent,
                )
            }
        }
        Shape2DType.Parallel, Shape2DType.Perpendicular -> {
            if (points.size < 3) return
            val base = points[1] - points[0]
            val direction = if (type == Shape2DType.Parallel) base else Vec2(-base.y, base.x)
            val length = direction.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val unit = direction * (1.0 / length)
            drawLine(accent, tx(points[2] - unit * 100.0), tx(points[2] + unit * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.AngleBisector -> {
            if (points.size < 3) return
            val u = points[0] - points[1]
            val v = points[2] - points[1]
            val um = u.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val vm = v.distanceTo(Vec2(0.0, 0.0)).coerceAtLeast(1e-9)
            val direction = u * (1.0 / um) + v * (1.0 / vm)
            drawLine(accent, tx(points[1]), tx(points[1] + direction * 100.0), stroke.width, cap = StrokeCap.Round)
        }
        Shape2DType.Circle -> {
            if (points.size < 2) return
            val center = offset(0)
            val radius = (offset(1) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.22f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.CircleThreePoints -> {
            if (points.size < 3) return
            val centerWorld = Geometry2D.circumcenter(points[0], points[1], points[2]) ?: return
            val center = tx(centerWorld)
            val radius = (tx(points[0]) - center).getDistance()
            if (filled) drawCircle(Brush.radialGradient(listOf(accent.copy(.18f), Color.Transparent), center, radius), radius, center)
            drawCircle(accent, radius, center, style = stroke)
        }
        Shape2DType.Ellipse -> {
            if (points.size < 3) return
            val center = tx(points[0])
            val rx = (tx(points[1]) - center).getDistance().coerceAtLeast(1f)
            val ry = (tx(points[2]) - center).getDistance().coerceAtLeast(1f)
            drawOval(
                color = accent,
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = stroke,
            )
        }
        Shape2DType.Rectangle, Shape2DType.Square -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(1)
            val width = b.x - a.x
            val height = if (type == Shape2DType.Square) width else b.y - a.y
            val path = Path().apply {
                moveTo(a.x, a.y)
                lineTo(a.x + width, a.y)
                lineTo(a.x + width, a.y + height)
                lineTo(a.x, a.y + height)
                close()
            }
            if (filled) drawPath(path, Brush.linearGradient(listOf(accent.copy(.28f), Cyan.copy(.10f))))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Triangle, Shape2DType.Polygon -> {
            if (points.size < 3) return
            val path = Path().apply {
                val first = offset(0)
                moveTo(first.x, first.y)
                points.indices.drop(1).forEach {
                    val p = offset(it)
                    lineTo(p.x, p.y)
                }
                close()
            }
            if (filled) drawPath(path, Brush.linearGradient(listOf(Violet.copy(.26f), Cyan.copy(.12f), Color.Transparent)))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.RegularPolygon -> {
            if (points.size < 2) return
            val center = points[0]
            val radiusVector = points[1] - center
            val startAngle = kotlin.math.atan2(radiusVector.y, radiusVector.x)
            val radius = radiusVector.distanceTo(Vec2(0.0, 0.0))
            val vertices = (0 until 5).map { i ->
                val angle = startAngle + i * 2.0 * PI / 5.0
                Vec2(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            }
            val path = Path().apply {
                val first = tx(vertices.first())
                moveTo(first.x, first.y)
                vertices.drop(1).forEach { val p = tx(it); lineTo(p.x, p.y) }
                close()
            }
            if (filled) drawPath(path, accent.copy(.18f))
            drawPath(path, accent, style = stroke)
        }
        Shape2DType.Arc -> {
            if (points.size < 2) return
            val a = offset(0)
            val b = offset(points.lastIndex)
            val radius = (b - a).getDistance().coerceAtLeast(70f)
            drawArc(accent, startAngle = 205f, sweepAngle = 115f, useCenter = false, topLeft = Offset(a.x - radius, a.y - radius), size = Size(radius * 2f, radius * 2f), style = stroke)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVector3D(
    vector: Vector3D,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    color: Color,
    selected: Boolean,
    perspective: Boolean,
) {
    val start = project(rotate(vector.start, rx, ry, rz), center, scale, perspective)
    val end = project(rotate(vector.end, rx, ry, rz), center, scale, perspective)
    val direction = end - start
    val length = direction.getDistance().coerceAtLeast(1f)
    val unit = Offset(direction.x / length, direction.y / length)
    val normal = Offset(-unit.y, unit.x)
    val head = 22f
    val left = end - unit * head + normal * (head * .55f)
    val right = end - unit * head - normal * (head * .55f)
    if (selected) {
        drawLine(color.copy(.18f), start, end, 18f, cap = StrokeCap.Round)
        drawCircle(color.copy(.20f), 46f, end)
    }
    drawLine(color.copy(.38f), Offset(center.x, center.y), start, 1.6f)
    drawLine(color, start, end, if (selected) 5.5f else 4f, cap = StrokeCap.Round)
    drawPath(
        Path().apply {
            moveTo(end.x, end.y)
            lineTo(left.x, left.y)
            lineTo(right.x, right.y)
            close()
        },
        Brush.linearGradient(listOf(color, Color.White.copy(.75f))),
    )
    drawCircle(color, 7f, start)
    drawCircle(Color.White, 3f, start)
    drawGraphLabel("${vector.name} |v|=${trim(vector.magnitude)}", end + Offset(16f, -54f), color)
    if (selected) {
        drawGraphLabel("<${trim(vector.components.x)}, ${trim(vector.components.y)}, ${trim(vector.components.z)}>", start + Offset(14f, 16f), color)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSolidProjection(
    solid: Solid,
    offset: Vec3,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    color: Color,
    wire: Boolean,
    selected: Boolean,
    perspective: Boolean,
    subSelection: SubObjectSelection?,
    sectionEnabled: Boolean,
    sectionPlane: EditableSectionPlane,
    clipSection: Boolean,
) {
    fun p(v: Vec3): Offset {
        return project(rotate(solidLocalToWorld(solid.copy(position = offset), v), rx, ry, rz), center, scale, perspective)
    }
    val strokeWidth = if (wire) 2.2f else 4.2f
    val anchor = project(rotate(offset, rx, ry, rz), center, scale, perspective)
    if (selected) {
        drawCircle(Brush.radialGradient(listOf(color.copy(.34f), Color.Transparent), anchor, 112f), radius = 112f, center = anchor)
        drawCircle(color.copy(.85f), radius = 64f, center = anchor, style = Stroke(2.4f))
    }
    val mesh = SolidMeshFactory.create(solid)
    val vertices = mesh.vertices.map(::p)
    if (!wire && !clipSection) {
        mesh.faces.forEachIndexed { index, face ->
                if (face.size >= 3) {
                    val path = Path().apply {
                        moveTo(vertices[face.first()].x, vertices[face.first()].y)
                        face.drop(1).forEach { lineTo(vertices[it].x, vertices[it].y) }
                        close()
                    }
                    val selectedFace = subSelection?.mode == Selection3DMode.Face && subSelection.index == index
                    drawPath(path, if (selectedFace) Amber.copy(.5f) else color.copy(.12f))
                }
        }
    }
    mesh.edges.forEachIndexed { index, (a, b) ->
        var start = mesh.vertices[a]
        var end = mesh.vertices[b]
        val startSide = sectionPlane.unitNormal.dot(start) - sectionPlane.offset
        val endSide = sectionPlane.unitNormal.dot(end) - sectionPlane.offset
        if (!clipSection || startSide >= 0.0 || endSide >= 0.0) {
            if (clipSection && startSide < 0.0) {
                val t = startSide / (startSide - endSide)
                start += (end - start) * t
            }
            if (clipSection && endSide < 0.0) {
                val t = endSide / (endSide - startSide)
                end += (start - end) * t
            }
            val picked = subSelection?.mode == Selection3DMode.Edge && subSelection.index == index
            drawLine(if (picked) Amber else color, p(start), p(end), if (picked) 7f else strokeWidth)
        }
    }
    if (mesh.vertices.size <= 16 || subSelection?.mode == Selection3DMode.Vertex) {
        vertices.forEachIndexed { index, vertex ->
            if (!clipSection || sectionPlane.unitNormal.dot(mesh.vertices[index]) - sectionPlane.offset >= 0.0) {
                val picked = subSelection?.mode == Selection3DMode.Vertex && subSelection.index == index
                drawCircle(if (picked) Amber else color, if (picked) 10f else 4.5f, vertex)
            }
        }
    }
    if (subSelection?.mode == Selection3DMode.Face) {
        mesh.faces.getOrNull(subSelection.index)?.let { face ->
            val path = Path().apply {
                if (face.isNotEmpty()) moveTo(vertices[face.first()].x, vertices[face.first()].y)
                face.drop(1).forEach { lineTo(vertices[it].x, vertices[it].y) }
                close()
            }
            drawPath(path, Amber.copy(.32f), style = Stroke(6f))
        }
    }
    if (sectionEnabled || clipSection) {
        val (basisA, basisB) = sectionPlane.basis()
        val extent = maxOf(solid.width, solid.height, solid.depth, solid.radius * 2.0).coerceAtLeast(1.0) * .7
        val planeCorners = listOf(
            sectionPlane.origin - basisA * extent - basisB * extent,
            sectionPlane.origin + basisA * extent - basisB * extent,
            sectionPlane.origin + basisA * extent + basisB * extent,
            sectionPlane.origin - basisA * extent + basisB * extent,
        ).map(::p)
        val planePath = Path().apply { moveTo(planeCorners[0].x, planeCorners[0].y); planeCorners.drop(1).forEach { lineTo(it.x, it.y) }; close() }
        drawPath(planePath, Amber.copy(.10f)); drawPath(planePath, Amber.copy(.45f), style = Stroke(2f))
        val section = CrossSection3D.intersect(mesh, sectionPlane.unitNormal, sectionPlane.offset)
        if (section.size >= 2) {
            val sectionPath = Path().apply {
                val first = p(section.first())
                moveTo(first.x, first.y)
                section.drop(1).map(::p).forEach { lineTo(it.x, it.y) }
                if (section.size >= 3) close()
            }
            if (section.size >= 3) drawPath(sectionPath, Amber.copy(.28f))
            drawPath(sectionPath, Amber, style = Stroke(5f))
            drawGraphLabel("section n·p=${trim(sectionPlane.offset)}", p(section.first()) + Offset(12f, -18f), Amber)
        }
    }
    if (selected) {
        drawGraphLabel("${solid.type.name} selected", anchor + Offset(20f, -72f), color)
    }
}

private data class ProjectedSectionHandle(val start: Offset, val end: Offset)

private fun subObjectAnchorWorld(solid: Solid, selection: SubObjectSelection?): Vec3 {
    val mesh = SolidMeshFactory.create(solid)
    val local = when (selection?.mode) {
        Selection3DMode.Vertex -> mesh.vertices.getOrNull(selection.index)
        Selection3DMode.Edge -> mesh.edges.getOrNull(selection.index)?.let { (a, b) -> (mesh.vertices[a] + mesh.vertices[b]) * .5 }
        Selection3DMode.Face -> mesh.faces.getOrNull(selection.index)?.mapNotNull(mesh.vertices::getOrNull)?.takeIf { it.isNotEmpty() }?.reduce(Vec3::plus)?.let { it * (1.0 / mesh.faces[selection.index].size) }
        else -> null
    }
    return local?.let { solidLocalToWorld(solid, it) } ?: solid.position
}

private fun projectedGizmoHandles(
    solid: Solid,
    selection: SubObjectSelection?,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    perspective: Boolean,
): List<TransformGizmoHandle> {
    val anchorWorld = subObjectAnchorWorld(solid, selection)
    fun screen(value: Vec3) = project(rotate(value, rx, ry, rz), center, scale, perspective)
    val anchor = screen(anchorWorld)
    val extent = 1.05
    val axes = listOf(
        TransformGizmoAxis.X to Vec3(extent, 0.0, 0.0),
        TransformGizmoAxis.Y to Vec3(0.0, extent, 0.0),
        TransformGizmoAxis.Z to Vec3(0.0, 0.0, extent),
    )
    return axes.map { (axis, vector) ->
        val end = screen(anchorWorld + vector)
        TransformGizmoHandle(axis, Vec2(anchor.x.toDouble(), anchor.y.toDouble()), Vec2(end.x.toDouble(), end.y.toDouble()))
    } + TransformGizmoHandle(TransformGizmoAxis.Uniform, Vec2(anchor.x.toDouble(), anchor.y.toDouble()), Vec2((anchor.x + 24f).toDouble(), (anchor.y + 24f).toDouble()))
}

private fun projectedSectionHandle(
    solid: Solid,
    plane: EditableSectionPlane,
    rx: Float,
    ry: Float,
    rz: Float,
    center: Offset,
    scale: Float,
    perspective: Boolean,
): ProjectedSectionHandle? {
    val startWorld = solidLocalToWorld(solid, plane.origin)
    val endWorld = solidLocalToWorld(solid, plane.origin + plane.unitNormal)
    val start = project(rotate(startWorld, rx, ry, rz), center, scale, perspective)
    val end = project(rotate(endWorld, rx, ry, rz), center, scale, perspective)
    return ProjectedSectionHandle(start, end).takeIf { (end - start).getDistance() > 2f }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTransformGizmo(handles: List<TransformGizmoHandle>, mode: Transform3DMode, active: TransformGizmoAxis?) {
    if (handles.isEmpty()) return
    fun p(value: Vec2) = Offset(value.x.toFloat(), value.y.toFloat())
    val anchor = p(handles.first().start)
    fun color(axis: TransformGizmoAxis) = when (axis) { TransformGizmoAxis.X -> Color(0xFFFF5B68); TransformGizmoAxis.Y -> Green; TransformGizmoAxis.Z -> Cyan; TransformGizmoAxis.Uniform -> Color.White }
    when (mode) {
        Transform3DMode.Move -> {
            handles.filter { it.axis != TransformGizmoAxis.Uniform }.forEach { handle ->
                val accent = color(handle.axis); drawLine(accent, p(handle.start), p(handle.end), if (active == handle.axis) 9f else 5f, cap = StrokeCap.Round); drawCircle(accent, 8f, p(handle.end)); drawGraphLabel(handle.axis.label, p(handle.end) + Offset(6f, -6f), accent)
            }
            drawCircle(Color.White, 7f, anchor)
        }
        Transform3DMode.Rotate -> {
            handles.filter { it.axis != TransformGizmoAxis.Uniform }.forEach { handle ->
                val radius = (p(handle.end) - anchor).getDistance(); drawCircle(color(handle.axis).copy(.85f), radius, anchor, style = Stroke(if (active == handle.axis) 8f else 4f))
            }
        }
        Transform3DMode.Scale -> {
            handles.forEach { handle ->
                val end = p(handle.end); val accent = color(handle.axis); drawLine(accent, anchor, end, if (active == handle.axis) 8f else 4f); drawRect(accent, topLeft = end - Offset(7f, 7f), size = Size(14f, 14f)); if (handle.axis != TransformGizmoAxis.Uniform) drawGraphLabel(handle.axis.label, end + Offset(6f, -6f), accent)
            }
        }
    }
}

private fun cubeVertices(w: Double, h: Double, d: Double): List<Vec3> {
    val x = w / 2
    val y = h / 2
    val z = d / 2
    return listOf(
        Vec3(-x, -y, -z), Vec3(x, -y, -z), Vec3(x, y, -z), Vec3(-x, y, -z),
        Vec3(-x, -y, z), Vec3(x, -y, z), Vec3(x, y, z), Vec3(-x, y, z),
    )
}

private fun rotate(p: Vec3, rx: Float, ry: Float, rz: Float): Vec3 {
    val ax = Math.toRadians(rx.toDouble())
    val ay = Math.toRadians(ry.toDouble())
    val az = Math.toRadians(rz.toDouble())
    var y = p.y * cos(ax) - p.z * sin(ax)
    var z = p.y * sin(ax) + p.z * cos(ax)
    var x = p.x
    val x2 = x * cos(ay) + z * sin(ay)
    z = -x * sin(ay) + z * cos(ay)
    x = x2
    val x3 = x * cos(az) - y * sin(az)
    y = x * sin(az) + y * cos(az)
    return Vec3(x3, y, z)
}

private fun solidLocalToWorld(solid: Solid, vertex: Vec3): Vec3 = rotate(
    vertex,
    solid.rotation.x.toFloat(),
    solid.rotation.y.toFloat(),
    solid.rotation.z.toFloat(),
) + solid.position

private fun Float.wrapDegrees(): Float {
    var value = this
    while (value > 180f) value -= 360f
    while (value < -180f) value += 360f
    return value
}

private fun project(p: Vec3, center: Offset, scale: Float, perspective: Boolean = true): Offset {
    val depthScale = if (perspective) 1.0 / (1.0 + (p.z + 5.0) * 0.08) else 1.0
    val depthLift = if (perspective) p.z * 18 else p.z * 10
    return Offset((center.x + p.x * scale * depthScale).toFloat(), (center.y - p.y * scale * depthScale + depthLift).toFloat())
}

private fun snapAngle(value: Float): Float = (kotlin.math.round(value / 1f) * 1f).coerceIn(-180f, 180f)

private fun quadrantLabel(angle: Double): String {
    val normalized = ((angle % 360.0) + 360.0) % 360.0
    return when {
        normalized == 0.0 || normalized == 90.0 || normalized == 180.0 || normalized == 270.0 -> "Axis"
        normalized < 90.0 -> "I"
        normalized < 180.0 -> "II"
        normalized < 270.0 -> "III"
        else -> "IV"
    }
}

private fun radianLabel(angle: Double): String = when (kotlin.math.round(angle).toInt()) {
    0 -> "0"
    30 -> "pi/6"
    45 -> "pi/4"
    60 -> "pi/3"
    90 -> "pi/2"
    120 -> "2pi/3"
    135 -> "3pi/4"
    150 -> "5pi/6"
    180 -> "pi"
    -30 -> "-pi/6"
    -45 -> "-pi/4"
    -60 -> "-pi/3"
    -90 -> "-pi/2"
    -120 -> "-2pi/3"
    -135 -> "-3pi/4"
    -150 -> "-5pi/6"
    -180 -> "-pi"
    else -> "${trim(Math.toRadians(angle))}"
}

@Composable
private fun MiniDock(modifier: Modifier = Modifier, items: List<String>, onMove: (Offset) -> Unit, onClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(alpha = .92f))
            .border(1.dp, Color(0x5548BFFF), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.pointerInput(Unit) { detectDragGestures { change, drag -> change.consume(); onMove(drag) } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            TransparentIcon("✥", Cyan)
            if (expanded) Text("Quick actions", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                if (expanded) "›" else "‹",
                color = Cyan,
                fontSize = 20.sp,
                modifier = Modifier
                    .clickable { expanded = !expanded }
                    .padding(7.dp)
                    .semantics { contentDescription = if (expanded) "Collapse quick actions" else "Expand quick actions" },
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                GlowButton("X", onClick = { expanded = false })
                items.forEach { GlowButton(it, onClick = { onClick(it); expanded = false }) }
            }
        }
    }
}

@Composable
private fun FloatingPanelLaunchers(
    modifier: Modifier = Modifier,
    leftLabel: String,
    rightLabel: String,
    bottomLabel: String,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onBottom: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val launcherWidth = with(density) { 170.dp.toPx() }
    val reservedHeight = with(density) { 260.dp.toPx() }
    val maxX = (containerSize.width - launcherWidth).coerceAtLeast(0f)
    val maxY = ((containerSize.height - reservedHeight) / 2f).coerceAtLeast(0f)
    LaunchedEffect(containerSize) { dragOffset = Offset.Zero }
    Column(
        modifier
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .padding(start = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(.82f))
            .animateContentSize()
            .pointerInput(maxX, maxY) {
                detectDragGestures { change, amount ->
                    change.consume()
                    dragOffset = Offset(
                        (dragOffset.x + amount.x).coerceIn(0f, maxX),
                        (dragOffset.y + amount.y).coerceIn(-maxY, maxY),
                    )
                }
            }
            .semantics { contentDescription = "Movable Open tools launcher. Tap to expand and drag to move" }
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            TransparentIcon("✥", Cyan)
            Text(if (expanded) "Hide tools" else "Open tools", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("drag", color = Muted, fontSize = 9.sp)
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                GlowButton("X", onClick = { expanded = false })
                GlowButton(leftLabel, onClick = { onLeft(); expanded = false })
                GlowButton(rightLabel, onClick = { onRight(); expanded = false })
                GlowButton(bottomLabel, onClick = { onBottom(); expanded = false })
            }
        }
    }
}

@Composable
private fun GraphEquationEditor(
    modifier: Modifier = Modifier,
    functions: List<com.indianservers.aiexplorer.core.FunctionDefinition>,
    selectedId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    addMenuExpanded: Boolean,
    onToggleAddMenu: () -> Unit,
    onAddKind: (GraphAddKind) -> Unit,
    onAdd: () -> Unit,
    onSelect: (String) -> Unit,
    onExpressionChange: (String, String) -> Unit,
    onToggleVisible: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onColor: (String) -> Unit,
    activeTool: GraphTool,
    onTool: (GraphTool) -> Unit,
    onTypingChange: (Boolean) -> Unit,
) {
    val selected = functions.firstOrNull { it.id == selectedId }
    val focusManager = LocalFocusManager.current
    var isTyping by remember(selectedId) { mutableStateOf(false) }
    Column(
        modifier
            .padding(top = 70.dp, start = 8.dp, end = 8.dp)
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceA.copy(.94f))
            .border(1.dp, Cyan.copy(.48f), RoundedCornerShape(18.dp))
            .animateContentSize()
            .padding(7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (!isTyping) GlowButton("+", onClick = onToggleAddMenu)
            Text(
                if (isTyping) "Editing ${selected?.name ?: "equation"}" else "Equations (${functions.size})",
                color = Cyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).clickable(enabled = !isTyping) { onExpandedChange(!expanded) }.padding(7.dp),
            )
            if (!isTyping) selected?.let { Text(it.name, color = graphColor(it.colorKey), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            if (isTyping) {
                GlowButton("Done") {
                    focusManager.clearFocus()
                    isTyping = false
                    onTypingChange(false)
                }
            } else {
                GlowButton("X", onClick = {
                    focusManager.clearFocus()
                    onTypingChange(false)
                    onExpandedChange(false)
                    if (addMenuExpanded) onToggleAddMenu()
                })
                GlowButton(if (expanded) "−" else "Edit", onClick = { onExpandedChange(!expanded) })
            }
        }
        AnimatedVisibility(addMenuExpanded && !isTyping) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GlowButton("X", onClick = onToggleAddMenu)
                GraphAddKind.entries.forEach { kind -> GlowButton(kind.label, onClick = { onAddKind(kind) }) }
            }
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (functions.isEmpty()) {
                    Text("Tap + to add your first equation.", color = Muted, fontSize = 12.sp)
                } else {
                    if (!isTyping) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        functions.forEach { function ->
                            val active = function.id == selected?.id
                            val accent = graphColor(function.colorKey)
                            var swipeDistance by remember(function.id) { mutableFloatStateOf(0f) }
                            Text(
                                text = "${if (function.visible) "●" else "○"} ${function.name}",
                                color = if (function.visible) Ink else Muted,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(accent.copy(if (active) .28f else .10f))
                                    .border(if (active) 2.dp else 1.dp, accent.copy(if (active) .9f else .35f), RoundedCornerShape(14.dp))
                                    .pointerInput(function.id) {
                                        detectDragGestures(
                                            onDragStart = { swipeDistance = 0f },
                                            onDragEnd = { if (swipeDistance < -90f) onDelete(function.id) else if (swipeDistance > 90f) onDuplicate(function.id); swipeDistance = 0f },
                                        ) { change, drag -> swipeDistance += drag.x; change.consume() }
                                    }
                                    .clickable { onSelect(function.id) }
                                    .padding(horizontal = 9.dp, vertical = 7.dp),
                            )
                        }
                    }
                    selected?.let { function ->
                        OutlinedTextField(
                            value = function.expression,
                            onValueChange = { onExpressionChange(function.id, it) },
                            label = { Text("${function.name} — edit directly") },
                            visualTransformation = MathSyntaxVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                focusManager.clearFocus()
                                isTyping = false
                                onTypingChange(false)
                            }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (isTyping != focusState.isFocused) {
                                        isTyping = focusState.isFocused
                                        onTypingChange(focusState.isFocused)
                                    }
                                }
                                .semantics { contentDescription = "Edit selected graph equation ${function.name}" },
                        )
                        if (!isTyping) {
                            Text("Smart actions for ${function.name}", color = Muted, fontSize = 10.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                GlowButton(if (function.visible) "Hide" else "Show") { onToggleVisible(function.id) }
                                GlowButton("Color") { onColor(function.id) }
                                GlowButton("Copy") { onDuplicate(function.id) }
                                GlowButton("Delete") { onDelete(function.id) }
                                listOf(GraphTool.Plot, GraphTool.Trace, GraphTool.Tangent, GraphTool.Derivative, GraphTool.Integral, GraphTool.Table).forEach { tool ->
                                    GlowButton(if (activeTool == tool) "● ${tool.name}" else tool.name) { onTool(tool) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(SurfaceA, SurfaceB)))
            .border(1.dp, Color(0x6645CFFF), RoundedCornerShape(18.dp))
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .animateContentSize()
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun PanelHeader(
    title: String,
    onClose: () -> Unit,
    accent: Color,
    icon: String = menuIcon(title),
    onMove: ((Offset) -> Unit)? = null,
) {
    val dragModifier = if (onMove == null) Modifier else Modifier.pointerInput(Unit) {
        detectDragGestures { change, drag -> change.consume(); onMove(drag) }
    }
    Row(Modifier.fillMaxWidth().then(dragModifier), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TransparentIcon(icon, accent)
            Column {
                Text(title, color = accent, fontWeight = FontWeight.Bold)
                if (onMove != null) Text("Drag to move", color = Muted, fontSize = 9.sp)
            }
        }
        Text(
            text = "X",
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceB.copy(alpha = .72f))
                .border(1.dp, accent.copy(alpha = .45f), RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics { contentDescription = "Close $title" },
        )
        Text(
            text = "×",
            color = Muted,
            fontSize = 20.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(horizontal = 12.dp, vertical = 7.dp)
                .semantics { contentDescription = "Close $title" },
        )
    }
}

@Composable
private fun GlowButton(label: String, enabled: Boolean = true, icon: String = menuIcon(label), iconOnly: Boolean = false, onClick: () -> Unit) {
    val symbolOnly = label in setOf("↶", "↷", "⋮")
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0x99101824), contentColor = Ink),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.heightIn(min = 42.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 5.dp),
    ) {
        TransparentIcon(icon, if (enabled) Cyan else Muted)
        if (!symbolOnly && !iconOnly) {
            Spacer(Modifier.width(5.dp))
            Text(label, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun TransparentIcon(symbol: String, tint: Color) {
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = .10f))
            .border(1.dp, tint.copy(alpha = .32f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SyntaxLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(7.dp)).background(color))
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

private class MathSyntaxVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        MathInputIntelligence.analyze(text.text).tokens.forEach { token ->
            val bracketColors = listOf(Cyan, Violet, Green, Amber)
            val style = when (token.kind) {
                MathInputTokenKind.Command -> SpanStyle(color = Color(0xFFA878FF), fontWeight = FontWeight.ExtraBold)
                MathInputTokenKind.Function -> SpanStyle(color = Violet, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Number -> SpanStyle(color = Cyan, fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Variable -> SpanStyle(color = Green, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Constant -> SpanStyle(color = Amber, fontWeight = FontWeight.Bold)
                MathInputTokenKind.Unit -> SpanStyle(color = Color(0xFF2DE2C5), fontWeight = FontWeight.Bold)
                MathInputTokenKind.Operator -> SpanStyle(color = Ink)
                MathInputTokenKind.Relation -> SpanStyle(color = Color(0xFFFF8A70), fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Bracket -> SpanStyle(color = bracketColors[(token.depth - 1).coerceAtLeast(0) % bracketColors.size], fontWeight = FontWeight.ExtraBold)
                MathInputTokenKind.Separator -> SpanStyle(color = Color(0xFFB4C3D8))
                MathInputTokenKind.Keyword -> SpanStyle(color = Color(0xFF79A7FF), fontWeight = FontWeight.SemiBold)
                MathInputTokenKind.Error -> SpanStyle(color = Color(0xFFFF6B7A), background = Color(0x44FF304F), fontWeight = FontWeight.Bold)
                MathInputTokenKind.Text -> SpanStyle(color = Muted)
            }
            builder.addStyle(style, token.start, token.end)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

private fun moduleIcon(module: MathModule): String = when (module) {
    MathModule.Geometry2D -> "△"
    MathModule.Geometry3D -> "◇"
    MathModule.Graph2D -> "ƒ"
    MathModule.Graph3D -> "⌁"
    MathModule.Trigonometry -> "θ"
    MathModule.Manipulatives -> "▦"
    MathModule.SpatialAR -> "AR"
}

private fun menuIcon(label: String): String = when {
    label.contains("menu", true) -> "☰"
    label.contains("subject", true) -> "⌂"
    label.contains("solver", true) || label.contains("solve", true) -> "✦"
    label.contains("equation", true) || label.contains("function", true) -> "ƒ"
    label.contains("graph", true) || label.contains("surface", true) -> "⌁"
    label.contains("3d", true) || label.contains("object", true) -> "◇"
    label.contains("tool", true) -> "⌘"
    label.contains("info", true) || label.contains("insight", true) -> "i"
    label.contains("learn", true) || label.contains("hint", true) -> "?"
    label.contains("export", true) || label.contains("save", true) -> "⇩"
    label.contains("delete", true) || label.contains("close", true) -> "×"
    label.contains("undo", true) || label == "↶" -> "↶"
    label.contains("redo", true) || label == "↷" -> "↷"
    label.contains("zoom", true) -> "⌕"
    label.contains("reset", true) || label.contains("fit", true) -> "◎"
    label.contains("move", true) -> "✥"
    label.contains("rotate", true) -> "↻"
    label.contains("scale", true) -> "↔"
    label.contains("planned", true) -> "◷"
    label.contains("workspace", true) -> "▦"
    label.contains("more", true) || label == "⋮" -> "⋮"
    else -> label.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "·"
}

@Composable
private fun Insight(label: String, value: String, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(9.dp)).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Muted, fontSize = 13.sp)
        }
        Text(value, color = Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted)
        Switch(checked = checked, onCheckedChange = {})
    }
}

@Composable
private fun TogglePill(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Text(
        text = if (checked) "On: $label" else "Off: $label",
        color = if (checked) Ink else Muted,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color(0x6630D9FF) else Color(0x33101824))
            .border(1.dp, if (checked) Cyan else Color(0x5548BFFF), RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
private fun AxisSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ${trim(value.toDouble())}", color = Muted, modifier = Modifier.width(112.dp))
        Slider(value = value, onValueChange = onValue, valueRange = range, modifier = Modifier.weight(1f))
    }
}
