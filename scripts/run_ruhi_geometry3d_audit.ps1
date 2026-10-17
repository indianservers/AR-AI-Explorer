param([string]$DeviceSerial='emulator-5556')
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$folder=Join-Path $PWD 'docs/qa/ruhi-geometry3d-300'
New-Item -ItemType Directory -Path $folder -Force > $null
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$arguments=@(':app:testDebugUnitTest')
foreach($filter in @('RuhiGeometryKidCommandsTest','RuhiDialogueTest','RuhiDialogueScenarioTest','RuhiSharedArchitectureTest','RuhiGraphAssistantTest','RuhiGraphTransformTest','RuhiImplicitSurfaceTopologyTest','Graph3DSurfaceRenderingTest','GraphAppearanceRegressionTest','AR3DGraphEngineParityTest','GraphExpressionAccuracyTest','NewGraphRenderExpressionRegressionTest','GeometryGraphBatchTransactionTest','GeometryGraphSchemaEndToEndTest')) { $arguments+=@('--tests',"*$filter") }
$arguments+=@('--console=plain')
& .\gradlew.bat @arguments *> (Join-Path $folder 'unit-build.log')
Copy-Item (Join-Path $folder 'unit-build.log') (Join-Path $folder 'build.log') -Force
if($LASTEXITCODE -ne 0) {throw 'Build or scene assertions failed; inspect build.log.'}
Copy-Item app/build/test-results/testDebugUnitTest/TEST-*.xml $folder -Force
Copy-Item app/build/reports/ruhi-geometry3d-300.json (Join-Path $folder 'jvm-300.json') -Force
& .\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain *> (Join-Path $folder 'apk-build.log')
Get-Content (Join-Path $folder 'apk-build.log') | Add-Content (Join-Path $folder 'build.log')
if($LASTEXITCODE -ne 0) {throw 'APK build failed; inspect build.log.'}
$auditAdb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $auditAdb -s $DeviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
if($LASTEXITCODE -ne 0) {throw 'App installation failed.'}
& $auditAdb -s $DeviceSerial install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if($LASTEXITCODE -ne 0) {throw 'Test APK installation failed.'}
$output=& $auditAdb -s $DeviceSerial shell am instrument -w -e class com.indianservers.aiexplorer.RuhiGeometryKidRenderingTest com.indianservers.aiexplorer.test/androidx.test.runner.AndroidJUnitRunner
$output | Set-Content (Join-Path $folder 'device-run.log') -Encoding utf8
$device=Join-Path $folder 'device';New-Item -ItemType Directory -Path $device -Force > $null
foreach($name in @('native-300.json','canvas-result.json','empty.png','red-cube-transformed.png')) {
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-geometry3d-300/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0) {throw "Missing device evidence: $name"}
}
& python scripts/build_ruhi_geometry3d_report.py
if($LASTEXITCODE -ne 0) {throw 'Report generation failed.'}
if(($output -join "`n") -notmatch 'OK \(2 tests\)' -or ($output -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {throw 'Device tests failed; inspect device-run.log.'}
