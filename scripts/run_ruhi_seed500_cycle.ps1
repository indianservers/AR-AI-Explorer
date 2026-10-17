param([Parameter(Mandatory=$true)][int]$Cycle,[string]$DeviceSerial='emulator-5556',[string]$TrainingPython="$env:LOCALAPPDATA\Codex\RuhiNlpTraining312\Scripts\python.exe")
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if($Cycle -lt 1){throw 'Cycle must be positive.'}
if(-not(Test-Path -LiteralPath $TrainingPython)){throw 'Provide the Python environment with TensorFlow using -TrainingPython.'}
$folder=Join-Path $PWD 'docs/qa/ruhi-500'
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
if($Cycle -ge 69){
    & $TrainingPython scripts/verify_ruhi_seed500_source.py --cycle $Cycle
    if($LASTEXITCODE -ne 0){throw 'Original workbook fields do not match seed labels, Android asset and replay.'}
}
$filters=@('RuhiSeed500AuditTest','RuhiPointLanguageTest','RuhiMidpointLanguageTest','RuhiSlopeLanguageTest','RuhiAngleLanguageTest','RuhiGeometryKidCommandsTest','RuhiDialogueTest','RuhiDialogueScenarioTest','RuhiSharedArchitectureTest','RuhiGraphAssistantTest','RuhiGraphTransformTest','RuhiImplicitSurfaceTopologyTest','Graph3DSurfaceRenderingTest','GraphAppearanceRegressionTest','AR3DGraphEngineParityTest','GraphExpressionAccuracyTest','NewGraphRenderExpressionRegressionTest','GeometryGraphBatchTransactionTest','GeometryGraphSchemaEndToEndTest')
$filters+='RuhiReferenceAngleLanguageTest'
$filters+=@('RuhiSpatialAngleLanguageTest','UniversalGeometryGraphSchemaTest','UniversalMathRuntimeTest','SharedSpatialMathSchemaTest','UniversalAlgebraExperienceTest')
$filters+='RuhiPlaneLanguageTest'
$filters+='RuhiCircleLanguageTest'
$filters+='RuhiTriangleLanguageTest'
$filters+='RuhiFunctionLanguageTest'
$filters+='RuhiIntersectionLanguageTest'
$filters+='RuhiDerivativeLanguageTest'
$filters+='RuhiIntegralLanguageTest'
$filters+='RuhiSurfaceLanguageTest'
$filters+='RuhiSolidMeasureLanguageTest'
$filters+='RuhiTransform3DLanguageTest'
$filters+='RuhiSceneEditLanguageTest'
$filters+='RuhiDialogueFollowUpTest'
$filters+='RuhiContextualAngleTest'
$filters+='RuhiContextualGraphTest'
$filters+='RuhiConstructionConversationTest'
$filters+='GeometrySelectionMeasurementsTest'
$filters+='RuhiColorTargetTest'
$filters+='RuhiOtherObjectTest'
$filters+='RuhiPreserveCubeTest'
$filters+='RuhiMeasurementUnitTest'
$filters+='RuhiDialogueSeedMatrixTest'
$filters+='RuhiPointDistanceUnitTest'
$filters+='RuhiAppliedTargetCorrectionTest'
$filters+='RuhiDerivativePointContextTest'
$filters+='RuhiDerivativePendingSlotsTest'
$filters+='RuhiDerivativeDomainFollowUpTest'
$filters+='RuhiDistancePolicyMatrixTest'
$filters+='RuhiChoosePointTest'
$filters+='RuhiAbsoluteValueExplanationTest'
$filters+='RuhiLexicalPointLabelTest'
$filters+='RuhiCriticalPointTest'
$filters+='RuhiDerivativeFormulaFollowUpTest'
$filters+='RuhiIntegralFormulaFollowUpTest'
$filters+='RuhiFormulaReplyLanguageTest'
$filters+='RuhiEndpointClarificationTest'
$filters+='RuhiMidpointLabelFollowUpTest'
$filters+=@('GraphProductionTest','Graph2DStrengthenedEngineTest','MathKeyboardGraph2D150Test','AdvancedGraphFeatureEngineTest','ScientificCalculatorTest','ScientificCalculatorPhase1Test','ScientificCalculatorPhase2Test','ScientificCalculatorPhase3Test')
$testArgs=@(':app:testDebugUnitTest','--max-workers=1');foreach($filter in $filters){$testArgs+=@('--tests',"*$filter")};$testArgs+='--console=plain'
$env:RUHI_AUDIT_CYCLE="pre-cycle-$Cycle"
$ErrorActionPreference='Continue'
& .\gradlew.bat @testArgs *> (Join-Path $folder "pre-cycle-$Cycle-test.log")
$preTestExit=$LASTEXITCODE
$ErrorActionPreference='Stop'
if($preTestExit -ne 0){throw 'Pre-training tests failed; fix the implementation before retraining.'}
if($Cycle -ge 55){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_context_gaps.init.gradle :app:auditRuhiContextGaps --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-context-audit.log")
    $contextExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($contextExit -ne 0){throw 'Could not establish contextual counterexample audit prerequisites.'}
    Copy-Item (Join-Path $folder 'context-gap-counterexamples.json') (Join-Path $folder "pre-cycle-$Cycle-context-gap-counterexamples.json") -Force
}
if($Cycle -ge 65){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_absolute_value.init.gradle :app:auditRuhiAbsoluteValueCoordinates --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-absolute-value-audit.log")
    $absoluteAuditExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($absoluteAuditExit -ne 0){throw 'Could not execute the compiled absolute-value coordinate audit.'}
    Copy-Item (Join-Path $folder 'absolute-value-coordinate-audit.json') (Join-Path $folder "pre-cycle-$Cycle-absolute-value-coordinate-audit.json") -Force
}
if($Cycle -ge 67){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_lexical_point_execution.init.gradle :app:auditRuhiLexicalPointExecution --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-lexical-execution-audit.log")
    $lexicalExecutionExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($lexicalExecutionExit -ne 0){throw 'Lexical-label parser/engine regression failed before training.'}
    Copy-Item (Join-Path $folder 'lexical-point-execution-audit.json') (Join-Path $folder "pre-cycle-$Cycle-lexical-point-execution-audit.json") -Force
}
if($Cycle -ge 68){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_critical_points.init.gradle :app:auditRuhiCriticalPointSemantics --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-critical-point-audit.log")
    $criticalExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($criticalExit -ne 0){throw 'Compiled critical-point audit could not run.'}
    $critical=Get-Content (Join-Path $folder 'critical-point-semantics-audit.json') -Raw | ConvertFrom-Json
    if($critical.cases -ne 6 -or $critical.verified -ne 6){throw 'Critical-point behavior regression failed before training.'}
    Copy-Item (Join-Path $folder 'critical-point-semantics-audit.json') (Join-Path $folder "pre-cycle-$Cycle-critical-point-semantics-audit.json") -Force
}
if($Cycle -ge 70){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_empty_integral.init.gradle :app:auditRuhiEmptyIntegralFollowUps --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-empty-integral-audit.log")
    $emptyIntegralExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($emptyIntegralExit -ne 0){throw 'Compiled empty-integral follow-up audit could not run.'}
    $emptyIntegral=Get-Content (Join-Path $folder 'empty-integral-follow-up-audit.json') -Raw | ConvertFrom-Json
    if($emptyIntegral.cases -ne 2 -or $emptyIntegral.verified -ne 2){throw 'Original empty-integral follow-up counterexamples still fail before training.'}
    Copy-Item (Join-Path $folder 'empty-integral-follow-up-audit.json') (Join-Path $folder "pre-cycle-$Cycle-empty-integral-follow-up-audit.json") -Force
}
if($Cycle -ge 71){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_formula_reply_language.init.gradle :app:auditRuhiFormulaReplyLanguage --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-formula-reply-language-audit.log")
    $formulaLanguageExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($formulaLanguageExit -ne 0){throw 'Compiled mathematical-word reply audit could not run.'}
    $formulaLanguage=Get-Content (Join-Path $folder 'formula-reply-language-audit.json') -Raw | ConvertFrom-Json
    if($formulaLanguage.cases -ne 6 -or $formulaLanguage.verified -ne 6){throw 'Mathematical-word formula reply regression failed before training.'}
    Copy-Item (Join-Path $folder 'formula-reply-language-audit.json') (Join-Path $folder "pre-cycle-$Cycle-formula-reply-language-audit.json") -Force
}
if($Cycle -ge 72){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_endpoint_pair_followups.init.gradle :app:auditRuhiEndpointPairFollowUps --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-endpoint-pair-followup-audit.log")
    $endpointExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($endpointExit -ne 0){throw 'Compiled endpoint clarification audit could not run.'}
    $endpoint=Get-Content (Join-Path $folder 'endpoint-pair-follow-up-audit.json') -Raw | ConvertFrom-Json
    if($endpoint.cases -ne 2 -or $endpoint.verified -ne 2){throw 'Original endpoint clarification counterexamples fail before training.'}
    Copy-Item (Join-Path $folder 'endpoint-pair-follow-up-audit.json') (Join-Path $folder "pre-cycle-$Cycle-endpoint-pair-follow-up-audit.json") -Force
}
if($Cycle -ge 73){
    $ErrorActionPreference='Continue'
    & .\gradlew.bat -I scripts/audit_ruhi_midpoint_label_followup.init.gradle :app:auditRuhiMidpointLabelFollowUp --max-workers=1 *> (Join-Path $folder "pre-cycle-$Cycle-midpoint-label-follow-up-audit.log")
    $labelExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($labelExit -ne 0){throw 'Compiled midpoint label audit could not run.'}
    $label=Get-Content (Join-Path $folder 'midpoint-label-follow-up-audit.json') -Raw | ConvertFrom-Json
    if($label.cases -ne 3 -or $label.verified -ne 3){throw 'Original midpoint replacement-label cases fail before training.'}
    Copy-Item (Join-Path $folder 'midpoint-label-follow-up-audit.json') (Join-Path $folder "pre-cycle-$Cycle-midpoint-label-follow-up-audit.json") -Force
}
$oldErrorPreference=$ErrorActionPreference
$ErrorActionPreference='Continue' # TensorFlow sends informational/deprecation messages to stderr.
& $TrainingPython -u scripts/train_ruhi_seed500.py --cycle $Cycle --test-evidence (Join-Path $folder "pre-cycle-$Cycle-scene-results.json") *> (Join-Path $folder "cycle-$Cycle-training.log")
$trainingExit=$LASTEXITCODE
$ErrorActionPreference=$oldErrorPreference
if($trainingExit -ne 0){throw 'TensorFlow training failed.'}
if($Cycle -ge 67){
    $ErrorActionPreference='Continue'
    & $TrainingPython -u scripts/audit_ruhi_point_label_heads.py --cycle $Cycle *> (Join-Path $folder "cycle-$Cycle-lexical-point-label-audit.log")
    $lexicalHeadExit=$LASTEXITCODE
    $ErrorActionPreference='Stop'
    if($lexicalHeadExit -ne 0){throw 'Trained lexical-label TensorFlow head regression failed.'}
}
$env:RUHI_AUDIT_CYCLE="cycle-$Cycle"
$buildArgs=$testArgs+@(':app:assembleDebug',':app:assembleDebugAndroidTest')
$ErrorActionPreference='Continue'
& .\gradlew.bat @buildArgs *> (Join-Path $folder "cycle-$Cycle-build.log")
$buildExit=$LASTEXITCODE
$ErrorActionPreference='Stop'
if($buildExit -ne 0){throw 'Post-training tests or APK build failed.'}
Copy-Item app/build/test-results/testDebugUnitTest/TEST-*.xml $folder -Force
& (Join-Path $PSScriptRoot 'verify_ruhi_seed500_device.ps1') -Cycle $Cycle -DeviceSerial $DeviceSerial -TrainingPython $TrainingPython
