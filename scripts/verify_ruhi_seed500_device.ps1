param([Parameter(Mandatory=$true)][int]$Cycle,[string]$DeviceSerial='emulator-5556',[string]$TrainingPython="$env:LOCALAPPDATA\Codex\RuhiNlpTraining312\Scripts\python.exe")
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$folder=Join-Path $PWD 'docs/qa/ruhi-500'
$modelPath=Join-Path $folder "cycle-$Cycle-model.json"
$buildPath=Join-Path $folder "cycle-$Cycle-build.log"
if(-not(Test-Path -LiteralPath $modelPath) -or -not(Test-Path -LiteralPath $buildPath)){throw 'Train and build this cycle before verifying its device results.'}
if((Get-Content -LiteralPath $buildPath -Raw) -notmatch 'BUILD SUCCESSFUL'){throw 'The cycle build is not successful.'}
$cycleModel=Get-Content -LiteralPath $modelPath -Raw | ConvertFrom-Json
$preHash=(Get-FileHash -LiteralPath $cycleModel.pre_training_test -Algorithm SHA256).Hash.ToLowerInvariant()
if($preHash -ne $cycleModel.pre_training_test_sha256 -or $cycleModel.cycle -ne $Cycle){throw 'Training does not match this cycle pre-test evidence.'}
$debugApk=Get-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk'
$testApk=Get-Item -LiteralPath 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
$changedSource=Get-ChildItem -LiteralPath 'app/src' -Recurse -File | Where-Object {$_.LastWriteTimeUtc -gt $debugApk.LastWriteTimeUtc} | Select-Object -First 1
if($changedSource){throw "App source changed after the build: $($changedSource.FullName). Rebuild before device verification."}
$auditAdb=Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
& $auditAdb -s $DeviceSerial install -r app/build/outputs/apk/debug/app-debug.apk
if($LASTEXITCODE -ne 0){throw 'App install failed.'}
& $auditAdb -s $DeviceSerial install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if($LASTEXITCODE -ne 0){throw 'Test install failed.'}
if($Cycle -ge 65){
    $installedPackages=@()
    foreach($packageInfo in @(@('com.indianservers.aiexplorer',$debugApk.FullName),@('com.indianservers.aiexplorer.test',$testApk.FullName))){
        $packagePath=(& $auditAdb -s $DeviceSerial shell pm path $packageInfo[0] | Select-Object -First 1).Trim().Replace('package:','')
        if($packagePath -notmatch '^/[A-Za-z0-9/~_=.+-]+/base\.apk$'){throw 'Unexpected installed APK path.'}
        $installedHash=((& $auditAdb -s $DeviceSerial shell sha256sum $packagePath) -split '\s+')[0].ToLowerInvariant()
        $builtHash=(Get-FileHash -LiteralPath $packageInfo[1] -Algorithm SHA256).Hash.ToLowerInvariant()
        $installedPackages += [pscustomobject]@{package=$packageInfo[0];installed_sha256=$installedHash;built_sha256=$builtHash;matches=($installedHash -eq $builtHash)}
    }
    $installedEvidence=[pscustomobject]@{cycle=$Cycle;device=$DeviceSerial;checked_at=(Get-Date -Format o);passed=(@($installedPackages | Where-Object {-not $_.matches}).Count -eq 0);packages=$installedPackages}
    $installedEvidence | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $folder "cycle-$Cycle-installed-apk-verification.json") -Encoding utf8
    if(-not $installedEvidence.passed){throw 'Installed app/test APKs do not match this cycle build.'}
}
$expectedNativeTests=if($Cycle -ge 78){54}elseif($Cycle -ge 77){53}elseif($Cycle -ge 76){52}elseif($Cycle -ge 75){51}elseif($Cycle -ge 74){50}elseif($Cycle -ge 73){49}elseif($Cycle -ge 72){48}elseif($Cycle -ge 71){47}elseif($Cycle -ge 70){46}elseif($Cycle -ge 69){45}elseif($Cycle -ge 68){44}elseif($Cycle -ge 67){43}elseif($Cycle -ge 66){42}elseif($Cycle -ge 64){41}elseif($Cycle -ge 62){39}elseif($Cycle -ge 61){38}elseif($Cycle -ge 60){37}elseif($Cycle -ge 58){36}elseif($Cycle -ge 57){35}elseif($Cycle -ge 55){34}elseif($Cycle -ge 53){32}elseif($Cycle -ge 52){30}elseif($Cycle -ge 51){29}elseif($Cycle -ge 50){28}else{27}
$output=& $auditAdb -s $DeviceSerial shell am instrument -w -e class com.indianservers.aiexplorer.RuhiSeed500RenderingTest,com.indianservers.aiexplorer.RuhiGeometryKidRenderingTest com.indianservers.aiexplorer.test/androidx.test.runner.AndroidJUnitRunner | Tee-Object -FilePath (Join-Path $folder "cycle-$Cycle-device.log")
if(($output -join "`n") -notmatch "OK \($expectedNativeTests tests\)" -or ($output -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|INSTRUMENTATION_ABORTED|Process crashed'){throw 'Emulator tests failed. Inspect the device log before collecting or trusting artifacts.'}
$device=Join-Path $folder "cycle-$Cycle-device";New-Item -ItemType Directory -Path $device -Force > $null
if($Cycle -ge 78){
    foreach($name in @('missing-prior-context-native.json','missing-prior-spatial-question.png','missing-prior-graph-question.png','missing-prior-spatial-applied.png','missing-prior-graph-applied.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing prior-context conversation evidence: $name"}
    }
}
if($Cycle -ge 77){
    foreach($name in @('spatial-linear-canvas.json','spatial-linear-before.png','spatial-linear-line.png','spatial-linear-segment.png','spatial-linear-ray.png','spatial-linear-edited.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing native point-linked line/segment/ray evidence: $name"}
    }
}
if($Cycle -ge 76){
    foreach($name in @('radical-helper-canvas.json','radical-helper-before.png','radical-helper-smooth-zero.png','radical-helper-edited.png','radical-helper-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing plotted radical derivative-helper evidence: $name"}
    }
}
if($Cycle -ge 75){
    foreach($name in @('radical-critical-canvas.json','radical-critical-corner.png','radical-critical-edited.png','radical-critical-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing real radical critical-point evidence: $name"}
    }
}
if($Cycle -ge 74){
    foreach($name in @('segment-selection-canvas.json','segment-selection-question.png','segment-selection-midpoint.png','segment-selection-midpoint-live.png','segment-selection-slope-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing native segment selection evidence: $name"}
    }
}
if($Cycle -ge 73){
    foreach($name in @('midpoint-label-canvas.json','midpoint-label-question.png','midpoint-label-answer.png','midpoint-label-live.png','midpoint-label-chip-created.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing midpoint label evidence: $name"}
    }
}
if($Cycle -ge 72){
    foreach($name in @('endpoint-pair-canvas.json','endpoint-pair-question.png','endpoint-pair-midpoint.png','endpoint-pair-midpoint-edited.png','endpoint-pair-slope-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing endpoint clarification evidence: $name"}
    }
}
if($Cycle -ge 71){
    foreach($name in @('formula-word-canvas.json','formula-word-derivative-marker.png','formula-word-derivative-answer.png','formula-word-integral-shading.png','formula-word-integral-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing mathematical-word formula evidence: $name"}
    }
}
if($Cycle -ge 70){
    foreach($name in @('integral-formula-canvas.json','integral-formula-question.png','integral-formula-unshaded.png','integral-formula-shaded.png','integral-formula-edited.png','integral-formula-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing integral formula evidence: $name"}
    }
}
if($Cycle -ge 69){
    foreach($name in @('derivative-formula-canvas.json','derivative-formula-question.png','derivative-formula-created.png','derivative-formula-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing derivative formula evidence: $name"}
    }
}
if($Cycle -ge 68){
    foreach($name in @('critical-point-canvas.json','critical-point-corner.png','critical-point-after-edit.png','critical-point-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing critical-point evidence: $name"}
    }
}
if($Cycle -ge 67){
    foreach($name in @('lexical-point-native.json','lexical-point-distance-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing lexical point evidence: $name"}
    }
}
if($Cycle -ge 66){
    foreach($name in @('absolute-value-canvas.json','absolute-value-positive-marker.png','absolute-value-positive-answer.png','absolute-value-negative-marker.png','absolute-value-negative-answer.png','absolute-value-zero-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing absolute-value evidence: $name"}
    }
}
if($Cycle -ge 64){
    foreach($name in @('choose-point-matrix.json','choose-point-canvas.json','choose-point-question.png','choose-point-answer-5.png','choose-point-answer-10.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing choose-point evidence: $name"}
    }
}
if($Cycle -ge 62){
    & $auditAdb -s $DeviceSerial pull '/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/distance-policy-matrix.json' (Join-Path $device 'distance-policy-matrix.json')
    if($LASTEXITCODE -ne 0){throw 'Missing native distance-policy matrix.'}
}
foreach($name in @('seed500-native.json','unseen-labels-native.json','visible-assistant.json','named-point-answer.png','midpoint-slope-canvas.json','midpoint-before.png','midpoint-marker.png','slope-triangle.png','midpoint-after-parent-drag.png','angle-canvas.json','angle-before.png','angle-right.png','angle-after-parent-drag.png','angle-bisector.png','angle-bisector-after-parent-drag.png','angle-counterclockwise.png','angle-counterclockwise-after-parent-drag.png','angle-selected-vertex.png','angle-reflex.png','angle-reflex-after-parent-drag.png','reference-angle-canvas.json','reference-angle-before.png','reference-angle-intersection.png','reference-angle-intersection-after-parent-drag.png','reference-angle-origin.png','reference-angle-axis.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('spatial-angle-canvas.json','spatial-angle-selected.png','spatial-angle-question.png','spatial-angle-right.png','spatial-angle-after-vector-edit.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('plane-angle-canvas.json','planes-before.png','planes-selected.png','plane-angle-question.png','plane-angle-right.png','plane-angle-after-point-edit.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('circle-canvas.json','circle-before.png','circle-named-center.png','circle-after-center-drag.png','circle-triangle-constructions.png','circle-triangle-after-parent-drag.png','circle-externally-tangent.png','circle-tangent-after-parent-drag.png','circle-red.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('triangle-canvas.json','triangle-before.png','triangle-equilateral.png','triangle-right.png','triangle-congruent.png','triangle-congruent-after-parent-drag.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('function-canvas.json','function-before.png','function-parabola.png','function-restricted.png','function-vertical.png','function-polar.png','function-piecewise.png','function-floor.png','function-upper-circle.png','function-piecewise-jump.png','function-restricted-pole.png','function-parametric.png','function-after-pan.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
foreach($name in @('construction-2d-canvas.json','construction-3d-canvas.json','construction-measurements-hidden.png','construction-measurements-shown.png','construction-copy-at-answer.png','construction-solid-measurements.png','construction-solid-copy-at-answer.png','dialogue-previous-graph-canvas.json','dialogue-previous-graph.png','dialogue-previous-graph-edited.png','dialogue-angle-canvas.json','dialogue-angle-before.png','dialogue-angle-sixty.png','dialogue-angle-parent-edit.png','dialogue-roles-canvas.json','dialogue-prior-radius.png','dialogue-corrected-height.png','dialogue-follow-up-canvas.json','dialogue-equation-resumed.png','dialogue-comparison-resumed.png','scene-geometry-canvas.json','scene-geometry-before.png','scene-triangle-red.png','scene-hidden-measurements.png','scene-grid-hidden.png','scene-circle-label.png','scene-labels-hidden.png','scene-point-label.png','scene-endpoints.png','scene-centered-point.png','scene-fit-all.png','scene-circle-copy.png','scene-cleared.png','scene-clear-undone.png','scene-graph-canvas.json','scene-graph-before.png','scene-graph-thick.png','scene-graph-blue.png','scene-graph-dashed.png','scene-graph-axes.png','scene-graph-grid-contrast.png','scene-graph-solo.png','scene-mesh-before.png','scene-mesh-opacity.png','transform-canvas.json','transform-before.png','transform-rotated-scaled.png','transform-opacity.png','transform-wireframe.png','transform-exploded.png','transform-assembled.png','transform-camera-top.png','transform-camera-isometric.png','transform-group-first.png','transform-group-second.png','solid-measure-canvas.json','solid-measure-before.png','solid-measure-question.png','solid-measure-sphere.png','solid-measure-after-radius-edit.png','solid-measure-cone.png','solid-measure-topology.png','solid-measure-euler.png','surface-canvas.json','surface-before.png','surface-paraboloid.png','surface-wireframe.png','surface-hemisphere.png','surface-pole.png','surface-paired.png','surface-after-parent-edit.png','integral-canvas.json','integral-before.png','integral-bounds-question.png','integral-preset-shading.png','integral-selected-area.png','integral-after-parent-edit.png','integral-between-curves.png','integral-split-signs.png','integral-riemann.png','integral-trapezoid.png','integral-accumulation.png','derivative-canvas.json','derivative-before.png','derivative-curve.png','derivative-slider.png','derivative-after-parent-edit.png','derivative-tangent.png','derivative-normal.png','derivative-absolute-corner.png','intersection-canvas.json','intersection-before.png','intersection-pair-question.png','intersection-crossings.png','intersection-after-curve-edit.png','intersection-circles.png','intersection-circles-after-parent-edit.png','intersection-tangent.png','intersection-after-pan.png','intersection-implicit.png','intersection-implicit-after-edit.png','intersection-implicit-vertical.png')){
    & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
    if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
}
& $auditAdb -s $DeviceSerial pull '/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-geometry3d-300/native-300.json' (Join-Path $device 'geometry300-native.json')
if($LASTEXITCODE -ne 0){throw 'Missing 300-command regression evidence.'}
if($Cycle -ge 50){
    foreach($name in @('dialogue-blue-canvas.json','dialogue-blue-before.png','dialogue-blue-moved.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 51){
    foreach($name in @('dialogue-other-canvas.json','dialogue-other-before.png','dialogue-other-scaled.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 52){
    foreach($name in @('dialogue-preserve-canvas.json','dialogue-preserve-before.png','dialogue-preserve-scaled.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 53){
    foreach($name in @('units-triangle-canvas.json','units-triangle-meters.png','units-triangle-centimeters.png','units-triangle-parent-edit.png','units-cube-canvas.json','units-cube-meters.png','units-cube-centimeters.png','units-cube-parent-edit.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 55){
    foreach($name in @('point-distance-units-2d.json','point-distance-units-3d.json','point-distance-segment-centimeters.png','point-distance-segment-parent-edit.png','point-distance-answer-2d.png','point-distance-answer-3d.png','point-distance-answer-3d-parent-edit.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 57){
    foreach($name in @('dialogue-blue-applied-canvas.json','dialogue-blue-applied-wrong.png','dialogue-blue-applied-corrected.png','dialogue-blue-applied-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 58){
    foreach($name in @('derivative-point-canvas.json','derivative-point-before.png','derivative-point-moved.png','derivative-point-off-curve-question.png','derivative-point-explicit-x.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 60){
    foreach($name in @('derivative-slots-canvas.json','derivative-slots-curve-question.png','derivative-slots-numeric-answer.png','derivative-slots-point-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if($Cycle -ge 61){
    foreach($name in @('derivative-domain-canvas.json','derivative-domain-log-question.png','derivative-domain-log-answer.png','derivative-domain-corner-answer.png')){
        & $auditAdb -s $DeviceSerial pull "/sdcard/Android/data/com.indianservers.aiexplorer/files/ruhi-seed500/$name" (Join-Path $device $name)
        if($LASTEXITCODE -ne 0){throw "Missing device evidence: $name"}
    }
}
if(($output -join "`n") -notmatch "OK \($expectedNativeTests tests\)" -or ($output -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed'){throw 'Emulator tests failed.'}
& $TrainingPython scripts/build_ruhi_seed500_report.py --cycle $Cycle
if($LASTEXITCODE -ne 0){throw 'Report generation failed.'}
Write-Output 'Cycle finished. Check the behavior gaps in REPORT.md before changing code and starting the next cycle. Seed intent fit is not full goal completion.'
