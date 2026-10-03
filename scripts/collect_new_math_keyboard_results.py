from pathlib import Path
import json, re, hashlib

root = Path(__file__).resolve().parents[1]
fixture = json.loads((root / 'app/src/androidTest/assets/math-keyboard-300-new.json').read_text(encoding='utf-8-sig'))
prefix = json.loads((root / 'docs/qa/math-keyboard-new-300-prefix-results.json').read_text(encoding='utf-8-sig'))
remaining = json.loads((root / 'tmp/keyboard-device-progress.json').read_text(encoding='utf-8-sig'))
assert len(prefix) == 100 and len(remaining) == 200
rows = prefix + remaining
assert len(rows) == 300 and len({row['id'] for row in rows}) == 300
assert {row['id'] for row in rows} == {case['id'] for case in fixture}
by_id = {case['id']: case for case in fixture}
old_text = (root / 'app/src/test/java/com/indianservers/aiexplorer/KeyboardGraphCases.kt').read_text(encoding='utf-8-sig')
old = set(re.findall(r'KeyboardGraphCase\("[^\"]+", [23], "[^\"]+", "([^\"]+)"', old_text))
assert len(old) == 300
assert len({case['source'] for case in fixture}) == 300
assert not old.intersection(case['source'] for case in fixture)
for row in rows:
    case = by_id[row['id']]
    assert row['source'] == case['source']
    row.update(dimension=case['dimension'], kind=case['kind'])
    if row.get('rendered') == '':
        row.pop('rendered')
    assert row['status'] == 'PASS' and row['nativeKeyboardVisible'] is False
recheck_path = root / 'tmp/keyboard-device-final-rechecks.json'
rechecks = json.loads(recheck_path.read_text(encoding='utf-8-sig')) if recheck_path.exists() else []
assert all(row['status'] == 'PASS' for row in rechecks)
report = {
    'totalNewCases': 300, 'passed': 300, 'failed': 0,
    'dimensions': {'2D': 150, '3D': 150},
    'overlapWithPrevious300': 0,
    'nativeKeyboardAppearances': 0,
    'environment': {'avd': 'Medium_Phone', 'device': 'emulator-5554', 'android': 17, 'api': 37, 'resolution': '1080x2400'},
    'inputMethod': 'Instrumented touchscreen taps and digit long presses on the onscreen MathKeyboard in the full app. No paste, text replacement, or direct target-expression injection.',
    'manualInstructions': 'math-keyboard-300-new-manual-cases.txt',
    'validation': ['Exact submitted expression after touchscreen entry', 'Independent numerical expected values or implicit residuals', '2D graph mode and nonempty finite geometry for the last 50 2D cases', 'Android IME remains hidden', 'Periodic keyboard and plotted-result screenshots reviewed'],
    'phases': ['First 100 new 2D cases passed before bracket-color and render-source fixes', 'Remaining 50 new 2D and 150 new 3D cases passed after those fixes', 'Selected existing NEW IDs rechecked on the final renderer guard build; these do not increase the 300-case count'],
    'fixes': ['2D editor receives editable source rather than partially resolved parameter expressions', 'Escape closing brace in Android ICU restriction regex to prevent parser crashes', 'Preserve complete implicit, polar, parametric and inequality expressions for rendering', 'Apply expression token and bracket colors while preserving structured superscript offsets', 'Only draw supported transformation previews while the controls are open or animation is enabled; remove misleading constant overlays from implicit plots'],
    'newUnitRegressionChecks': 5,
    'limitations': ['Android emulator verification; no physical-device run', 'Touchscreen actions are replayed by instrumentation rather than by a human operator'],
    'finalRechecks': rechecks,
    'additionalScreenshotRechecks': json.loads((root / 'tmp/keyboard-device-screenshot-rechecks.json').read_text(encoding='utf-8-sig')),
    'directEmulatorVisualRecheck': {'id': 'NEW-3D-122', 'source': 'x^2+y^2=1.1', 'status': 'PASS', 'method': 'ADB touchscreen taps and 800ms stationary swipes on the visible MathKeyboard, outside Compose instrumentation; Enter plots the cylinder and closes the editor'},
    'screenshots': ['math-keyboard-new-300-evidence/2d-implicit-circle.png', 'math-keyboard-new-300-evidence/2d-inequality.png', 'math-keyboard-new-300-evidence/colored-keyboard.png', 'math-keyboard-new-300-evidence/3d-direct-keyboard.png', 'math-keyboard-new-300-evidence/3d-direct-cylinder.png'],
    'cases': rows,
}
apk = root / 'app/build/outputs/apk/debug/app-debug.apk'
report['finalApkSha256'] = hashlib.sha256(apk.read_bytes()).hexdigest()
output = root / 'docs/qa/math-keyboard-new-300-results.json'
output.write_text(json.dumps(report, indent=2), encoding='utf-8')
print(f'{len(rows)} new cases verified; report: {output}')
