"""Report only recorded scene, model and emulator evidence."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1];folder=root/'docs/qa/ruhi-dialogue-evidence'
model=json.loads((folder/'model-evaluation.json').read_text(encoding='utf-8'))
scene=json.loads((folder/'scene-replay.json').read_text(encoding='utf-8')) if (folder/'scene-replay.json').exists() else {}
native=json.loads((folder/'device/result.json').read_text(encoding='utf-8')) if (folder/'device/result.json').exists() else {}
inference=json.loads((folder/'device/dialogue-model-native.json').read_text(encoding='utf-8')) if (folder/'device/dialogue-model-native.json').exists() else {}
log=(folder/'device-run.log').read_text(encoding='utf-8-sig') if (folder/'device-run.log').exists() else ''
passed=re.search(r'OK \((\d+) tests?\)',log)
tests=[ET.parse(p).getroot() for p in folder.glob('TEST-*.xml')]
lines=['# Ruhi conversational follow-up engine','',
'The existing four studios now share a bounded dialogue manager, pending slots, scene-ID resolution, default policies, actionable follow-up chips and persisted conversation memory. Lessons keep their original route. Mathematical engines remain deterministic.','',
'## Recorded verification','',
f'- JVM methods: {sum(int(t.get("tests",0)) for t in tests)}; failures/errors: {sum(int(t.get("failures",0))+int(t.get("errors",0)) for t in tests)}.',
f'- Real scene replay: {scene.get("scenarios","not recorded")} conversations, {scene.get("turns","not recorded")} turns; scene assertions passed: {scene.get("scene_assertions_passed",False)}. JVM rendering: {scene.get("rendering","NOT_RUN")}.',
f'- Android instrumentation success summary: {passed.group(1)+" methods passed" if passed else "not verified"}.',
f'- Visible incomplete command → clarification → clicked equation chip → native curve → conversational red style: {"PASS" if native.get("passed") and passed else "not verified"}; changed central display-region pixels: {native.get("changed_canvas_region_pixels","not recorded")}.',
f'- Canvas gesture while chat remains open: {native.get("canvas_interaction_while_open",False) if passed else "not verified"}.',
f'- Actual Android TensorFlow dialogue inference: {"PASS" if inference.get("passed") and inference.get("model_sha256")==model["model_sha256"] else "not verified"}.','',
'## Training and limits','',
f'- Additional authored multi-turn scenarios: {model["scenarios"]}; unique labeled training-turn text: {model["unique_training_turns"]}. They share templates and mathematical parameter families. No student conversations or correction records are used.',
f'- Exported dialogue model: `{model["model_sha256"]}`.',
f'- Small authored held-out phrase benchmark: {model["correct"]}/{model["total"]}; confidence gate accepts {model["accepted"]}, accepted errors {model["accepted_errors"]}. This does not establish unrestricted NLP accuracy or statistical probability calibration.',
'- Binary English word/bigram model; deterministic grammar supplies mathematical entities and slots. Arbitrary constructions, tutoring dialogue, reference forms and multi-slot variants outside the reviewed grammar remain limited. Existing unsupported engine features remain unsupported.',
'- Android speech-recognition launcher is connected; raw microphone/audio recognition quality was not certified by these tests. Text and reviewed voice transcripts use the same dialogue path.',
'- Current solid references remain snapshot-bound indices; pending target edits require the scene fingerprint to match. Native rendering remains Compose CPU projection.',
'- The original 150-command state/mesh test is included in JVM regressions. A new complete 150-command screenshot audit is not claimed by this conversation-specific run.','',
'## Evidence','',
'![Clarification and answer chips](device/clarification-chips.png)','',
'![Real red curve after the follow-up](device/red-curve.png)','',
'See scenarios.jsonl, test-replay.tsv, scene-replay.json, model-evaluation.json, TEST XML files, build.log, device-run.log and source-manifest.json. The Android JSON is written only after scene, chip and pixel assertions pass.','',
'Reproduce: train with scripts/train_ruhi_dialogue_nlp.py using scripts/ruhi-nlp-requirements.txt; run scripts/run_ruhi_dialogue_audit.ps1 with a connected emulator. See [architecture and defaults](../RUHI_CONVERSATION_ARCHITECTURE.md).','']
(folder/'REPORT.md').write_text('\n'.join(lines),encoding='utf-8')
print(json.dumps(dict(jvm_tests=sum(int(t.get('tests',0)) for t in tests),scenario_replay=scene,android_passed_methods=int(passed.group(1)) if passed else 0,visible_flow_passed=native.get('passed',False))))
