$ErrorActionPreference = 'Stop'

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$out = 'D:\tempuiverification\first10_v6'
New-Item -ItemType Directory -Force -Path $out | Out-Null

function To-B64Url([string]$text) {
    [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($text)).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$twoD = @(
    'sin^2(x)',
    'cos^2(x)+sin^2(x)',
    'tan^2(x/2)',
    'sin(x)+cos(2*x)',
    'sqrt(x^2+1)',
    'abs(x)-2',
    'x^3-3*x',
    'ln(x+4)',
    'exp(x/3)-2',
    'if(x>0,x^2,-x)'
)

$threeD = @(
    'z=sin^2(x)+cos^2(y)',
    'z=tan^2(x/2)+y',
    'z=x^2+y^2',
    'z=x^2-y^2',
    'z=sin(x)+cos(y)',
    'z=sqrt(x^2+y^2)',
    'z=exp(-(x^2+y^2)/8)',
    'z=abs(x)-abs(y)',
    'z=if(x>y,x,y)',
    'z=ln(x^2+y^2+1)'
)

$manifest = @('kind`tindex`expression`tfile')

function Wait-ForGraphScreen([string]$kind, [string]$index) {
    $xmlPath = Join-Path $out "$($kind.ToLower())_$index.xml"
    for ($attempt = 0; $attempt -lt 40; $attempt++) {
        Start-Sleep -Milliseconds 750
        & $adb -s emulator-5554 shell uiautomator dump "/sdcard/verify_$kind`_$index.xml" | Out-Null
        & $adb -s emulator-5554 pull "/sdcard/verify_$kind`_$index.xml" $xmlPath | Out-Null
        $xml = Get-Content -Path $xmlPath -Raw -ErrorAction SilentlyContinue
        $isReady = if ($kind -eq '2D') {
            ($xml -match 'Equations \(1\)|Interactive graph|Maths[^"]*Graph') -and ($xml -notmatch '3D Graph|Interactive 3D')
        } else {
            $xml -match 'Maths[^"]*3D Graph|3D Graph|Interactive 3D graph'
        }
        if ($isReady) {
            Start-Sleep -Seconds 2
            return $true
        }
    }
    Write-Warning "Timed out waiting for $kind graph screen $index"
    return $false
}

for ($i = 0; $i -lt $twoD.Count; $i++) {
    $n = '{0:D2}' -f ($i + 1)
    $expr = $twoD[$i]
    $manifest += "2D`t$n`t$expr`t2d_$n.png"
    & $adb -s emulator-5554 shell am force-stop com.indianservers.aiexplorer | Out-Null
    & $adb -s emulator-5554 shell am start -W -n com.indianservers.aiexplorer/.GraphVerificationActivity --es verify_graph_mode 2d --es verify_graph_expression_b64 (To-B64Url $expr) | Out-Null
    Wait-ForGraphScreen '2D' $n | Out-Null
    & $adb -s emulator-5554 shell screencap -p "/sdcard/2d_$n.png" | Out-Null
    & $adb -s emulator-5554 pull "/sdcard/2d_$n.png" "$out\2d_$n.png" | Out-Null
}

for ($i = 0; $i -lt $threeD.Count; $i++) {
    $n = '{0:D2}' -f ($i + 1)
    $expr = $threeD[$i]
    $manifest += "3D`t$n`t$expr`t3d_$n.png"
    & $adb -s emulator-5554 shell am force-stop com.indianservers.aiexplorer | Out-Null
    & $adb -s emulator-5554 shell am start -W -n com.indianservers.aiexplorer/.GraphVerificationActivity --es verify_graph_mode 3d --es verify_graph_expression_b64 (To-B64Url $expr) | Out-Null
    Wait-ForGraphScreen '3D' $n | Out-Null
    & $adb -s emulator-5554 shell screencap -p "/sdcard/3d_$n.png" | Out-Null
    & $adb -s emulator-5554 pull "/sdcard/3d_$n.png" "$out\3d_$n.png" | Out-Null
}

$manifest | Set-Content -Path "$out\manifest.tsv" -Encoding UTF8
Get-ChildItem -Path $out | Select-Object Name,Length,LastWriteTime
