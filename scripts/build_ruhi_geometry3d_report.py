"""Build evidence from actual model, JVM and device outputs; never infer a PASS."""
from pathlib import Path
import hashlib,json,re,xml.etree.ElementTree as ET
from collections import Counter
root=Path(__file__).resolve().parents[1];folder=root/'docs/qa/ruhi-geometry3d-300'
def read(path):return json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else {}
def log(path):
    if not path.exists():return ''
    data=path.read_bytes()
    return data.decode('utf-16' if data[:2] in (b'\xff\xfe',b'\xfe\xff') else 'utf-8-sig',errors='replace')
model=read(folder/'model-evaluation.json');jvm=read(root/'app/build/reports/ruhi-geometry3d-300.json');device=read(folder/'device/native-300.json');canvas=read(folder/'device/canvas-result.json')
requests=read(folder/'requests.json');counts=Counter(r['status'] for r in device.get('results',[]))
build=log(folder/'build.log')
run=log(folder/'device-run.log')
units=[ET.parse(p).getroot() for p in folder.glob('TEST-*.xml')]
test_count=sum(int(x.get('tests','0')) for x in units);failures=sum(int(x.get('failures','0'))+int(x.get('errors','0')) for x in units)
passed=jvm.get('passed')==300 and device.get('passed')==300 and canvas.get('passed') is True and 'BUILD SUCCESSFUL' in build and bool(re.search(r'OK \(\d+ tests\)',run)) and failures==0 and test_count>0
fit=model.get('supplied_training_fit',{});holdout=model.get('heldout',{})
lines=['# Ruhi: 300 kid-style requests in native 3D geometry','',f"Verification: **{'PASS' if passed else 'INCOMPLETE / FAILED'}**",'',
f"TensorFlow supplied-example intent fit: **{fit.get('correct',0)}/{fit.get('total',300)}**. Every supplied example was training data; this measures fit, not generalization.",
f"Small held-out development check: **{holdout.get('correct',0)}/{holdout.get('total',20)}**; confidence gate accepted {holdout.get('accepted',0)}, with {holdout.get('accepted_errors',0)} accepted errors. Related templates remain in training, and this set was inspected during development. It is not broad NLP accuracy.",'',
f"JVM scene replay: **{jvm.get('passed',0)}/300** expected outcomes; {test_count} JUnit methods, {failures} failures/errors.",
f"Android emulator: **{device.get('passed',0)}/300** native TensorFlow intent and scene checks. Rendering was checked separately through the visible assistant on the real geometry canvas; all 300 were not individually screenshot-certified.",'',
'| Native outcome | Requests |','|---|---:|']
for status in ('APPLIED','CLARIFICATION','INVALID','UNSUPPORTED'):lines.append(f'| {status} | {counts[status]} |')
lines+=['','APPLIED includes read-only mathematical answers and selection, as well as edits. Clarifications, rejections and unsupported responses count as safe expected outcomes, not implemented constructions.','',
'The visible assistant creates a cube, changes its side to 5, colors it red, moves it +2 along X and rotates it 90° around Z. The test checks volume 125, rejects a negative sphere radius without adding a solid, and compares canvas pixels.','',
'The saved image was visually inspected. The existing 2× camera view clips part of the translated side-5 cube at the right edge; automatic camera fitting is not implemented by this change (request 300 remains unsupported). Pixel changes certify rendering, not full viewport containment.','',
'## Test context','',
'Each numbered case starts in an explicit fixture, rather than treating this mixed list as one continuous conversation. The default scene contains a side-2 cube at (1,2,3), a 6×4×2 box, radius-3 sphere at (3,2,1), radius-2 height-5 cylinder, radius-3 height-6 cone, side-4 height-7 square pyramid, 2×3×5 triangular prism, and side-2 tetrahedron. A=(2,0,0), B=(0,3,0), C=(0,0,4); vectors AB and AC are present. Creation cases start empty. “Both” cases add a second matching solid; selected cone/pyramid/sphere cases select that target; the blue-color case seeds a blue sphere; the no-cones case removes cones. Undo/redo and load cases seed a prior change/save. See the checked-in audit fixture for exact contexts.','',
'Additional multi-turn tests check independent copies, target preservation, coordinate edits, scale/axis questions and answers, rejection after scene changes, workspace persistence, finite meshes and scoped undo. Questions do not create undo entries.','',
'## Training and runtime','',
'The bundled TensorFlow model proposes a geometry intent. Reviewed language rules extract and validate dimensions, named targets and coordinates; native geometry formulas and meshes produce results. No model generates pixels, formulas, coordinates or arbitrary code. Invalid radius/length inputs keep the scene unchanged. Model confidence never overrides validation. No student conversations are collected or used for training.','',
'Creation defaults are explicit in the response. Unspecified cube side is 2; radius is 1; height is 2. “Bigger” uses 1.25 and “smaller” uses 0.8; ask-first requests wait for a factor/axis. Solid dimensions are limited to 50 units and coordinates to ±10000. Explicit Z movement uses Z. Native solid meshes have their intrinsic height along Y; downward/upside-down turns therefore reverse Y using a half-turn about X.','',
'The existing UI layout and lesson content are unchanged; native appearance mapping was extended for requested colors. Voice hardware/microphone recognition was not certified in this run.','',
'## Remaining engine limitations','',
'Unsupported items remain visible below. They include native line/ray/plane constructions, arbitrary line/plane intersections, slicing and nets, several scene arrangements/comparisons, hide/lock/outline controls, selective color undo, and camera fitting. They were not made to pass by inventing geometry or silently editing a different solid. Save/load stores a native scene snapshot inside the current workspace, not an external file/export.','',
'## Evidence','',
'- [Native results](device/native-300.json) · [Canvas assertions](device/canvas-result.json) · [Model evaluation](model-evaluation.json)','- [Empty canvas](device/empty.png) · [Transformed red cube](device/red-cube-transformed.png)','- [Build log](build.log) · [Device log](device-run.log) · [Source/model hashes](source-manifest.json)','',
'## All supplied requests','', '| # | Request | Expected outcome | Actual native outcome |','|---:|---|---|---|']
actual={r['id']:r for r in device.get('results',[])}
for r in requests:
    a=actual.get(r['id'],{});lines.append(f"| {r['id']} | {r['text'].replace('|','/')} | {r['expected_status']} | {a.get('status','NOT RUN')}{' — FAILED' if a.get('errors') else ''} |")
(folder/'REPORT.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
paths=[*root.glob('app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/Ruhi*.kt'),root/'app/src/main/java/com/indianservers/aiexplorer/assistant/workspace/TensorFlowGraphNlp.kt',root/'app/src/main/java/com/indianservers/aiexplorer/MainActivity.kt',root/'app/src/debug/java/com/indianservers/aiexplorer/assistant/workspace/RuhiGeometryKidAudit.kt',root/'app/src/test/java/com/indianservers/aiexplorer/RuhiGeometryKidCommandsTest.kt',root/'app/src/androidTest/java/com/indianservers/aiexplorer/RuhiGeometryKidRenderingTest.kt',*root.glob('app/src/main/assets/ruhi_geometry3d_nlp/*'),root/'scripts/train_ruhi_geometry3d_nlp.py',root/'scripts/run_ruhi_geometry3d_audit.ps1',Path(__file__),root/'app/build/outputs/apk/debug/app-debug.apk',root/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk']
paths.append(root/'app/src/main/java/com/indianservers/aiexplorer/workspace/StudioReferenceMetadata.kt')
(folder/'source-manifest.json').write_text(json.dumps({str(p.relative_to(root)).replace('\\','/'):dict(bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in paths if p.is_file()},indent=2),encoding='utf-8')
print(json.dumps(dict(passed=passed,jvm=jvm.get('passed'),native=device.get('passed'),unit_tests=test_count,outcomes=counts)))
