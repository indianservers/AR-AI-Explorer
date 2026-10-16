param([string]$DeviceSerial='emulator-5556')
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$folder=Join-Path $PWD 'docs/qa/ruhi-dialogue-evidence'
New-Item -ItemType Directory -Path $folder -Force > $null
if(-not $env:JAVA_HOME) { $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr' }
$filters=@('RuhiDialogueTest','RuhiDialogueScenarioTest','RuhiSharedArchitectureTest','RuhiGraphAssistantTest','RuhiGraphTransformTest','RuhiImplicitSurfaceTopologyTest','Graph3DSurfaceRenderingTest','GraphAppearanceRegressionTest','AR3DGraphEngineParityTest','GraphExpressionAccuracyTest','NewGraphRenderExpressionRegressionTest','GeometryGraphBatchTransactionTest','GeometryGraphSchemaEndToEndTest')
$arguments=@(':app:testDebugUnitTest')
foreach($filter in $filters) { $arguments+=@('--tests',"*$filter") }
$arguments+=@(':app:assembleDebug',':app:assembleDebugAndroidTest','--console=plain')
& .\gradlew.bat @arguments *> (Join-Path $folder 'build.log')
if($LASTEXITCODE -ne 0) { throw 'Build or JVM dialogue assertions failed; see build.log.' }
Copy-Item -Path app/build/test-results/testDebugUnitTest/TEST-*.xml -Destination $folder -Force
Copy-Item -LiteralPath app/build/reports/ruhi-dialogue-scenarios.json -Destination (Join-Path $folder 'scene-replay.json') -Force
$auditAdb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $auditAdb -s $DeviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
if($LASTEXITCODE -ne 0) { throw 'Debug APK installation failed.' }
& $auditAdb -s $DeviceSerial install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if($LASTEXITCODE -ne 0) { throw 'Instrumentation APK installation failed.' }
$classes='com.indianservers.aiexplorer.RuhiDialogueRenderingTest,com.indianservers.aiexplorer.RuhiSharedRenderingTest,com.indianservers.aiexplorer.RuhiGraphRenderingTest#bundledTensorFlowModelRecognizesAndExecutesParaphrases,com.indianservers.aiexplorer.RuhiGraphRenderingTest#requestedCommandExamplesHaveCorrectTensorFlowIntents'
& $auditAdb -s $DeviceSerial shell rm -r /sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-dialogue-audit
$output=& $auditAdb -s $DeviceSerial shell am instrument -w -e class $classes com.indianservers.aiexplorer.test/androidx.test.runner.AndroidJUnitRunner
$output | Set-Content -LiteralPath (Join-Path $folder 'device-run.log') -Encoding utf8
$device=Join-Path $folder 'device'
New-Item -ItemType Directory -Path $device -Force > $null
$missingEvidence=$false
foreach($name in @('before.png','clarification-chips.png','red-curve.png','open-chat-canvas-pan.png','result.json','dialogue-model-native.json')) {
    $destination=Join-Path $device $name
    if(Test-Path -LiteralPath $destination) { Remove-Item -LiteralPath $destination -Force }
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-dialogue-audit/$name" $destination
    if($LASTEXITCODE -ne 0) { $missingEvidence=$true }
}
& python scripts/build_ruhi_dialogue_report.py
if($LASTEXITCODE -ne 0) { throw 'Report generation failed.' }
if($missingEvidence) { throw 'Some dialogue device evidence is unavailable; see the report and device-run.log.' }
if(($output -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' -or ($output -join "`n") -notmatch 'OK \(8 tests\)') { throw 'Native verification did not pass all eight methods; inspect device-run.log.' }
