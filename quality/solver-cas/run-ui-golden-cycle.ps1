param(
    [Parameter(Mandatory = $true)][ValidateSet(1, 2)][int]$Cycle,
    [ValidateRange(1, 200)][int]$BatchSize = 25,
    [switch]$Resume
)

$ErrorActionPreference = 'Stop'
$adb = 'C:\Users\saisa\AppData\Local\Android\Sdk\platform-tools\adb.exe'
$runner = 'com.indianservers.aiexplorer.test/androidx.test.runner.AndroidJUnitRunner'
$testClass = 'com.indianservers.aiexplorer.SolverCasUiGoldenDatasetTest'
$deviceRoot = '/sdcard/Android/data/com.indianservers.aiexplorer/files/solver-cas-ui'
$hostRoot = Join-Path $PSScriptRoot "ui-results\cycle-$Cycle"
$resultsRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot 'ui-results'))
$resolvedHostRoot = [System.IO.Path]::GetFullPath($hostRoot)
if (-not $resolvedHostRoot.StartsWith($resultsRoot + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw "Refusing to manage results outside $resultsRoot"
}
if (-not $Resume -and (Test-Path -LiteralPath $resolvedHostRoot)) {
    Remove-Item -LiteralPath $resolvedHostRoot -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $hostRoot | Out-Null

for ($start = 1; $start -le 1200; $start += $BatchSize) {
    $end = [Math]::Min(1200, $start + $BatchSize - 1)
    $name = "SOLVER_CAS_UI_GOLDEN_RESULTS_cycle${Cycle}_${start}_${end}.csv"
    if ($Resume -and (Test-Path -LiteralPath (Join-Path $hostRoot $name))) {
        Write-Output "UI cycle $Cycle batch $start-$end already captured; skipping"
        continue
    }
    Write-Output "UI cycle $Cycle batch $start-$end"
    $output = & $adb -s emulator-5554 shell am instrument -w -r `
        -e class $testClass `
        -e solverCycle $Cycle `
        -e solverStart $start `
        -e solverEnd $end `
        $runner 2>&1
    $output | Tee-Object -FilePath (Join-Path $hostRoot "instrumentation_${start}_${end}.log")

    & $adb -s emulator-5554 pull "$deviceRoot/$name" (Join-Path $hostRoot $name) | Out-Null
    if (($output -join "`n") -notmatch 'OK \(1 test\)') {
        throw "UI cycle $Cycle batch $start-$end failed. Evidence was pulled before stopping."
    }
}

$fragments = Get-ChildItem $hostRoot -Filter "SOLVER_CAS_UI_GOLDEN_RESULTS_cycle${Cycle}_*.csv" | Sort-Object {
    if ($_.BaseName -match '_cycle\d+_(\d+)_') { [int]$Matches[1] } else { 0 }
}
$header = Get-Content $fragments[0].FullName -TotalCount 1
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add($header)
foreach ($fragment in $fragments) {
    (Get-Content $fragment.FullName | Select-Object -Skip 1).ForEach({ $lines.Add($_) })
}
$consolidated = Join-Path $hostRoot "SOLVER_CAS_UI_GOLDEN_RESULTS_cycle$Cycle.csv"
[System.IO.File]::WriteAllLines($consolidated, $lines, [System.Text.UTF8Encoding]::new($false))

$rows = Import-Csv $consolidated
if ($rows.Count -ne 1200) { throw "Cycle $Cycle consolidated $($rows.Count) rows, expected 1200." }
if (@($rows.case_id | Sort-Object -Unique).Count -ne 1200) { throw "Cycle $Cycle has duplicate or missing case IDs." }
if (@($rows | Where-Object status -ne PASS).Count -ne 0) { throw "Cycle $Cycle contains failing rows." }

& $adb -s emulator-5554 pull "$deviceRoot/cycle-$Cycle-evidence" $hostRoot | Out-Null
Write-Output "UI cycle $Cycle PASS: 1200/1200 -> $consolidated"
