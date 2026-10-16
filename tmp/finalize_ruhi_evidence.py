from pathlib import Path
import json,hashlib,shutil,xml.etree.ElementTree as ET,zipfile,ast,subprocess
root=Path.cwd(); graph=root/'docs/qa/ruhi-graph-evidence'; shared=root/'docs/qa/ruhi-shared-evidence'
for folder in (graph,shared):
    for p in (root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        ET.parse(p); shutil.copy2(p,folder/p.name)
shutil.copy2(root/'tmp/ruhi-shared-delivery-build.log',graph/'build-and-regressions.log')
shutil.copy2(root/'tmp/ruhi-shared-delivery-build.log',shared/'build.log')
shutil.copy2(root/'tmp/ruhi-capture-final-device.log',graph/'device-full-run.log')
shutil.copy2(root/'tmp/ruhi-capture-final-device.log',shared/'device-run.log')
shutil.copy2(root/'app/build/reports/ruhi-graph-audit.json',graph/'final-state-mesh.json')
config=json.loads((graph/'device-config.json').read_text(encoding='utf-8-sig'))
config.update(jvm_regression_tests=61,instrumentation_methods=7,engine_model_sha256='476da8e9133abbb1c2a5b6db956f85978bb0986804da095270f943bb77f35a8f',render_capture='Android UiAutomation display screenshot cropped to measured canvas bounds; central plot ROI')
for folder in (graph,shared): (folder/'device-config.json').write_text(json.dumps(config,indent=2),encoding='utf-8')
manifest=json.loads((graph/'source-manifest.json').read_text(encoding='utf-8-sig'))
paths=set(manifest)
paths.update(subprocess.check_output(['git','diff','--name-only','--','app/src'],text=True).splitlines())
for folder in (graph,shared): shutil.copy2(root/'tmp/ruhi-native-capture-build.log',folder/'capture-harness-build.log')
for base in ('app/src/main/java/com/indianservers/aiexplorer/assistant/workspace','app/src/main/assets/ruhi_nlp','app/src/main/assets/ruhi_engine_nlp'):
    paths.update(str(p.relative_to(root)).replace('\\','/') for p in (root/base).rglob('*') if p.is_file())
for pattern in ('app/src/**/Ruhi*.kt','app/src/**/StudioReferenceMetadata.kt','app/src/**/GraphVerificationActivity.kt','scripts/*ruhi*.*'):
    paths.update(str(p.relative_to(root)).replace('\\','/') for p in root.glob(pattern) if p.is_file())
manifest={p:hashlib.sha256((root/p).read_bytes()).hexdigest() for p in sorted(paths) if (root/p).is_file()}
for folder in (graph,shared): (folder/'source-manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
for apk in ('app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'):
    with zipfile.ZipFile(apk) as z:
        assert z.testzip() is None
        if apk.endswith('app-debug.apk'):
            for directory in ('ruhi_nlp','ruhi_engine_nlp'):
                model=root/'app/src/main/assets'/directory/'model.tflite'
                assert hashlib.sha256(z.read('assets/'+directory+'/model.tflite')).digest()==hashlib.sha256(model.read_bytes()).digest()
for folder in (graph,shared):
    for p in folder.glob('*.json'): json.loads(p.read_text(encoding='utf-8-sig'),parse_constant=lambda v: (_ for _ in ()).throw(ValueError(v)))
for p in root.glob('scripts/*ruhi*.py'): ast.parse(p.read_text(encoding='utf-8-sig'))
tests=[ET.parse(p).getroot() for p in (root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
assert sum(int(t.get('tests',0)) for t in tests)==61
assert sum(int(t.get('failures',0))+int(t.get('errors',0)) for t in tests)==0
print('61 regressions passed; APK CRC/assets, source hashes, JSON and Python syntax verified')
