param([int[]]$CaseIds, [string]$Category, [switch]$StateOnly, [string]$DeviceSerial)
$ErrorActionPreference = 'Stop'
if ($DeviceSerial) { $env:ANDROID_SERIAL = $DeviceSerial }
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
if (Test-Path -LiteralPath 'C:\Program Files\Android\Android Studio\jbr') { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
$gradleTasks = @(':app:testDebugUnitTest','--tests','*RuhiGraphAssistantTest','--tests','*RuhiImplicitSurfaceTopologyTest','--tests','*RuhiGraphTransformTest','--tests','*RuhiSharedArchitectureTest','--tests','*Graph3DSurfaceRenderingTest','--tests','*GraphAppearanceRegressionTest','--tests','*AR3DGraphEngineParityTest','--tests','*GraphExpressionAccuracyTest','--tests','*NewGraphRenderExpressionRegressionTest','--tests','*GeometryGraphBatchTransactionTest','--tests','*GeometryGraphSchemaEndToEndTest','--console=plain')
if (-not $StateOnly) { $gradleTasks += @(':app:assembleDebug',':app:assembleDebugAndroidTest') }
& .\gradlew.bat @gradleTasks
if ($LASTEXITCODE -ne 0) { throw 'Gradle audit/build failed; inspect the test report.' }
$auditEvidence = Join-Path (Get-Location).Path 'docs/qa/ruhi-graph-evidence'
Get-ChildItem -LiteralPath 'app/build/test-results/testDebugUnitTest' -Filter 'TEST-*.xml' -File | Copy-Item -Destination $auditEvidence
Copy-Item -LiteralPath 'app/build/reports/ruhi-graph-audit.json' -Destination (Join-Path $auditEvidence 'final-state-mesh.json')
if ($StateOnly) {
    & python scripts/build_ruhi_graph_report.py app/build/reports/ruhi-graph-audit.json
    exit $LASTEXITCODE
}
$auditAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $auditAdb get-state
if ($LASTEXITCODE -ne 0) { throw 'Connect exactly one Android device or emulator for rendered verification.' }
& python scripts/probe_ruhi_device.py
if ($LASTEXITCODE -ne 0) { throw 'Android package/activity services are not ready; rendered tests were not run.' }
& $auditAdb install -r app/build/outputs/apk/debug/app-debug.apk
if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
& $auditAdb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if ($LASTEXITCODE -ne 0) { throw 'Test installation failed.' }
if ($Category) {
    $ranges = @{'Object Creation'=1..20;'Delete & Reset'=21..30;'Positioning'=31..45;'Scaling'=46..60;'Rotation'=61..75;'Appearance'=76..90;'3D Graphing'=91..105;'Geometry Relations'=106..120;'Conversational AI'=121..135;'Error & Stress Tests'=136..150}
    if (-not $ranges.ContainsKey($Category)) { throw 'Unknown test category.' }
    $CaseIds = $ranges[$Category]
}
$instrumentArgs = @('shell','am','instrument','-w','-e','class','com.indianservers.aiexplorer.RuhiGraphRenderingTest,com.indianservers.aiexplorer.RuhiSharedRenderingTest')
if ($CaseIds) { $instrumentArgs += @('-e','caseIds',($CaseIds -join ',')) }
$instrumentArgs += 'com.indianservers.aiexplorer.test/androidx.test.runner.AndroidJUnitRunner'
$auditOutput = & $auditAdb @instrumentArgs
$auditOutput | Write-Output
& $auditAdb pull /sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-graph-audit docs/qa/ruhi-graph-evidence/device
if ($LASTEXITCODE -ne 0) { throw 'No device evidence was retrieved.' }
& $auditAdb pull /sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-shared-audit docs/qa/ruhi-shared-evidence/device
if ($LASTEXITCODE -ne 0) { throw 'No shared-studio evidence was retrieved.' }
$auditOutput | Set-Content -LiteralPath 'docs/qa/ruhi-shared-evidence/device-run.log' -Encoding utf8
& python scripts/build_ruhi_graph_report.py docs/qa/ruhi-graph-evidence/device/results.json
if ($LASTEXITCODE -ne 0) { throw 'Evidence report generation failed.' }
Copy-Item -Path 'app/build/test-results/testDebugUnitTest/TEST-*.xml' -Destination 'docs/qa/ruhi-shared-evidence' -Force
& python scripts/build_ruhi_shared_report.py
if ($LASTEXITCODE -ne 0) { throw 'Shared evidence report generation failed.' }
if (($auditOutput -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') { throw 'Rendered audit failed; inspect saved evidence.' }
