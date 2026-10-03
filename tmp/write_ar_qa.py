from pathlib import Path
import json, shutil, xml.etree.ElementTree as E

out = Path('docs/qa')
out.mkdir(exist_ok=True)
cases = []
scenarios = [
 ('Floor', 'Select Floor/Table, scan textured tiles slowly sideways, hold steady and tap the interior.', 'Only a stable upward floor anchors.'),
 ('Table', 'Select Floor/Table and scan a textured tabletop.', 'Tracked tabletop becomes ready and supports placement.'),
 ('Wall', 'Select Wall and scan brick or patterned wallpaper.', 'Only a stable vertical wall anchors.'),
 ('Wrong target floor', 'Select Wall and point only at the floor.', 'No floor placement in wall mode.'),
 ('Wrong target wall', 'Select Floor/Table and point only at a wall.', 'No wall placement in floor mode.'),
 ('Ceiling', 'Select Floor/Table and aim at a ceiling.', 'No ceiling placement as a floor.'),
 ('Polygon edge', 'Tap just outside a tracked plane polygon.', 'Out-of-polygon hit is rejected.'),
 ('Tiny plane', 'Tap immediately after a small plane is first detected.', 'Small or unstable plane is rejected.'),
 ('Textureless wall', 'Scan a plain white wall with Wall selected.', 'Useful scanning guidance; no estimated wall masquerades as tracked.'),
 ('Mirror', 'Aim Wall at a mirror or transparent glass.', 'No false readiness without a usable tracked plane; no crash.'),
 ('Darkness', 'Dim lighting while scanning; then turn a lamp on.', 'More-light guidance and stable recovery.'),
 ('Motion', 'Translate or rotate quickly while scanning; then slow down.', 'Move-slowly guidance resets readiness; stable recovery.'),
 ('Occlusion', 'Cover camera briefly, uncover and return to prior surface.', 'No placement during failure; readiness requires stable recovery.'),
 ('Target change', 'Detect floor and wall; alternate target and tap immediately.', 'Fresh matching stability is required after each target change.'),
 ('Near range', 'Try a hit closer than 15 cm.', 'Overly close hit is rejected.'),
 ('Far range', 'Try a hit farther than 8 metres.', 'Overly distant hit is rejected.'),
 ('Math input', 'Open Add/editor; clear input; use MathKeyboard to enter a new construction. For graph3D use x^2+y^2 with long-press 2; graph2D use y^2=x; geometry use point or solid controls.', 'Custom keyboard stays active; native IME hidden; valid construction updates.'),
 ('Invalid input', 'Enter an unfinished expression or invalid construction and submit.', 'Actionable error; no crash; previous valid mathematical work survives.'),
 ('Singularity', 'For graph3D enter 1/(x-y), graph2D enter 1/x; for geometry create intersecting constructions, then anchor.', 'Discontinuities or intersection handling do not crash; valid content renders.'),
 ('Permission', 'Deny camera permission, then grant it in settings and retry Full AR.', 'Visible permission state; successful retry does not erase work.'),
 ('Service', 'Open without AR service, install or update it, return and retry.', 'Installation state is visible; camera startup can recover.'),
 ('Background', 'Anchor, background the app, then return.', 'Anchor is preserved; placement readiness is freshly checked.'),
 ('Rotation', 'Rotate portrait-landscape-portrait while live camera is running.', 'Camera geometry, reticle and touch hit coordinates stay aligned.'),
 ('Replacement', 'Anchor, then cause a replacement hit to expire before placement.', 'Old anchor survives unsuccessful replacement.'),
 ('Shared session', 'Switch to another AR workspace and back while session is running.', 'Shared camera session survives; launch mode and mathematical work are correct.'),
]
for construction in ['x^2+y^2', 'x^2-y^2', 'sin(x)+cos(y)', 'x^2+y^2+z^2=4']:
    workspace = 'AR 3D Graph'
    for name, action, expected in scenarios:
        cases.append(dict(id=f'AR-M{len(cases)+1:03}', workspace=workspace, scenario=name, construction=construction, action=f'Using MathKeyboard prepare {construction}. '+action, expected=expected, status='NOT_RUN_REQUIRES_PHYSICAL_CAMERA'))
assert len(cases) == 100
data = dict(targetDevice='OnePlus Nord CE5', physicalCameraCasesRun=0, cases=cases)
(out/'ar-100-manual-cases.json').write_text(json.dumps(data, indent=2), encoding='utf-8')
lines = ['100 shared AR manual acceptance cases', 'Target: OnePlus Nord CE5; 25 scenarios across four distinct 3D constructions = 100 AR 3D Graph cases.', 'All physical-camera cases NOT RUN: phone not connected; emulator ARCore camera initialization failed.', 'Enter math through touchscreen MathKeyboard; use real camera rather than fake runtime.', 'Record Android/OxygenOS and AR service versions, observed guidance, surface, anchor result, screenshot and pass/fail.', '']
for c in cases:
    lines += [f"{c['id']} | {c['workspace']} | {c['scenario']}", f"Action: {c['action']}", f"Expected: {c['expected']}", f"Status: {c['status']}", '']
(out/'ar-100-manual-cases.txt').write_text('\n'.join(lines), encoding='utf-8')
results = {}
for module in ['arengine','app']:
    results[module] = []
    for p in Path(module+'/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        r=E.parse(p).getroot()
        results[module].append(dict(name=r.get('name'), tests=int(r.get('tests')), failures=int(r.get('failures')), errors=int(r.get('errors'))))
(out/'ar-shared-engine-results.json').write_text(json.dumps(dict(automatedSuites=results, physicalManualCasesRun=0, emulatorUiChecks='Workspace and floor/wall selection in all four modes; camera failure reporting; Add panel; MathKeyboard focus; touchscreen x^(2)+y^(2) entry with long-press 2; Update surface; native IME hidden.', cameraLimitation='ARCore expected camera 0; emulator exposed cameras 1 and 10. Initialization failed on API 35 and API 37.'), indent=2), encoding='utf-8')
for src, dest in [('tmp/ar35-app-log.txt','emulator-camera-failure.txt'), ('tmp/ar-final-build.log','build-and-test.txt')]:
    shutil.copyfile(src, out/'ar-shared-engine-evidence'/dest)
(out/'ar-shared-engine-report.txt').write_text('''AR shared engine verification — 2026-10-03
Target: OnePlus Nord CE5; no USB phone connected.

Implemented:
- AR 3D Graph now opens the same SpatialAR screen and ArCoreRuntime as 2D graph and 2D/3D geometry.
- Source workspace determines AR launch mode; geometry starts in live floor/table mode.
- Shared configuration enables horizontal and vertical planes, with fallback for optional depth/HDR/instant features.
- Shared intelligence checks target orientation, tracked plane size, camera failure, motion, confidence, uncertainty, distance, polygon membership and plane identity.
- Readiness requires 250 ms of stable distinct frames. Target changes, pause and camera restart reset readiness.
- Repeated prepare/resume and availability checks preserve running sessions.
- Failed anchor replacement preserves the previous anchor.
- Floor/Table and Wall controls are visible. Camera failures are reported rather than called Camera ready.
- Shared coordinate/vector lab placement uses the same intelligence filter.

Validation:
:app:assembleDebug SUCCESSFUL.
146 arengine JVM tests passed, including 100 new parameterized hit cases and temporal/configuration/lifecycle tests.
7 app navigation/adapter JVM tests passed.
Direct ADB touchscreen taps verified workspace and floor/wall selection in all four modes.
Opened AR graph Add panel and MathKeyboard, cleared input, entered x^(2)+y^(2) by touching x, long-press 2, +, y, long-press 2, and tapped Update surface.
Native IME: mInputShown=false; mIsInputViewShown=false.
Screenshots/logs are in ar-shared-engine-evidence/.

Unverified:
Real floor/wall detection, camera compositing and real-world anchoring on the phone.
ARCore 1.56 emulator service was installed on API 35 and API 37. Both failed initialization because ARCore expected camera 0 while the emulator exposed cameras 1 and 10.
The 100 manual camera cases are prepared, but ZERO are claimed as executed. Automated engine tests are not manual camera tests.

Next required evidence:
Connect the Nord CE5 with USB debugging enabled and run the 100 manual cases on its camera. Record Android/OxygenOS version. Fix observed device-specific failures before declaring acceptance complete.
Official compatibility reference: https://developers.google.com/ar/devices (Nord CE5 supports Depth API).
''', encoding='utf-8')
