"""Document recognition separately from deterministic execution capability."""
import json,hashlib,re,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
folder=root/'docs/qa/ruhi-nlp-command-training';folder.mkdir(parents=True,exist_ok=True)
assets=root/'app/src/main/assets/ruhi_nlp'
metadata=json.loads((assets/'metadata.json').read_text(encoding='utf-8'))
evaluation=json.loads((root/'docs/qa/ruhi-graph-evidence/nlp-evaluation.json').read_text(encoding='utf-8'))
independent=json.loads((folder/'independent-evaluation.json').read_text(encoding='utf-8'))
digest=hashlib.sha256((assets/'model.tflite').read_bytes()).hexdigest()
assert digest==metadata['model_sha256']==evaluation['model_sha256']==independent['model_sha256']
requested=evaluation['requested_replay']
assert len(requested['results'])==89
native_path=folder/'requested-intents-native.json'
native=json.loads(native_path.read_text(encoding='utf-8-sig')) if native_path.exists() else {}
native_ok=native.get('model_sha256')==digest and len(native.get('results',[]))==89 and all(r['expected']==r['actual'] for r in native.get('results',[]))
native_log=(folder/'device-run.log').read_text(encoding='utf-8-sig') if (folder/'device-run.log').exists() else ''
success=re.search(r'OK \((\d+) tests?\)',native_log)
tests=[ET.parse(p).getroot() for p in folder.glob('TEST-*.xml')]
lines=['# Ruhi requested-command TensorFlow training','',
'This increment trains recognition of the 89 developer-supplied commands while preserving the existing UI, lessons, deterministic parser and mathematical engines. Recognition does not add missing geometric constructors or bypass validation.','',
f'- Bundled model: TensorFlow-trained LiteRT model, {len(metadata["labels"])} intent labels; SHA-256 `{digest}`.',
f'- Training: {evaluation["unique_training_sentences"]} unique authored examples, including the explicitly requested developer commands and bounded numeric/object/polite variants. No harvested student conversations or correction records.',
f'- Supplied-command recognition replay: **{requested["correct"]}/{requested["total"]}**. These sentences informed training; this is training fit, not generalization accuracy.',
f'- Supplied examples meeting the unchanged confidence gate: {sum(r["accepted"] for r in requested["results"])}/{requested["total"]}. Top-label recognition alone is not execution approval.',
f'- Development paraphrases: {evaluation["correct"]}/{evaluation["total"]}; accepted {evaluation["accepted_holdout"]}, accepted errors {evaluation["accepted_holdout_errors"]}. Complete sentences are excluded from training, but related templates are present and these results were inspected during development.',
f'- Separate frozen sentence evaluation: **{independent["correct"]}/{independent["total"]}** ({100*independent["correct"]/independent["total"]:.1f}%); accepted {independent["accepted"]}, accepted errors {independent["accepted_errors"]}. This is a small authored benchmark, not unrestricted NLP or speech accuracy.',
f'- Actual Android model inference over the 89 examples: {"PASS" if native_ok else "not verified"}; saved instrumentation summary: {success.group(1)+" methods passed" if success else "no successful summary recorded"}.','',
f'- JVM regressions: {sum(int(t.get("tests",0)) for t in tests)}; failures/errors: {sum(int(t.get("failures",0))+int(t.get("errors",0)) for t in tests)}. These include the original 150-command state/mesh audit, not a new rendered replay.','',
'Confidence gates remain 0.85 probability and 0.35 top-two margin. Only whitelisted paraphrases can become canonical executable text. Models do not supply coordinates, dimensions, mesh vertices, formulas or pixels. Invalid coordinates, nonfinite sizes and excessive angles remain subject to deterministic validation. Missing parameters still require clarification; unknown engine operations remain unsupported.','',
'## Requested examples','',
'The execution-status column is the existing Graph3D audit expectation under its preconditions, not a new rendered verification. Shared geometry adapters may differ. Compound actions remain atomic.','',
'| ID | Command | Expected intent | Predicted intent | Confidence | Existing graph behavior |',
'|---:|---|---|---|---:|---|']
for r in requested['results']:
 lines.append(f'| {r["id"]} | {r["text"].replace("|","/")} | {r["intent"]} | {r["actual"]} | {r["confidence"]:.3f} | {r["graph_execution_status"]} |')
lines+=['','## Separate evaluation errors','']
errors=[r for r in independent['results'] if not r['correct']]
lines += [f'- `{r["text"]}`: expected {r["expected"]}, predicted {r["actual"]}; gate accepted: {r["accepted"]}.' for r in errors] or ['None in the recorded small benchmark.']
lines+=['','## Reproduction','',
'Use the Python 3.12 environment with scripts/ruhi-nlp-requirements.txt. Run scripts/train_ruhi_graph_nlp.py, then scripts/evaluate_ruhi_requested_nlp.py. The independent evaluation never trains the model. Run the two Android methods bundledTensorFlowModelRecognizesAndExecutesParaphrases and requestedCommandExamplesHaveCorrectTensorFlowIntents in RuhiGraphRenderingTest for native inference. Build the debug APK to package the new assets.','',
'Supporting files: evaluation.json, independent-evaluation.json, command_examples.json, training.log, build.log and device-run.log. The separate engine-selection classifier is unchanged; its existing 83.6% benchmark is not the intent score above. The earlier 150-command rendering report is historical evidence and was not recertified by this model-only increment.','']
(folder/'REPORT.md').write_text('\n'.join(lines),encoding='utf-8')
print(json.dumps({'requested':requested['correct'],'requested_total':89,'independent_correct':independent['correct'],'independent_total':independent['total'],'native_verified':native_ok}))
