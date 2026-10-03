import json
from pathlib import Path
evidence = Path('docs/qa/workspace-ux-150-evidence')
catalog = json.loads(Path('app/src/androidTest/assets/workspace-ux-150.json').read_text(encoding='utf-8'))
results = [case for mode in ['graph2d','graph3d','geometry2d','geometry3d'] for case in json.loads((evidence/f'{mode}.json').read_text(encoding='utf-8-sig'))]
assert len(results) == 150
assert {c['id'] for c in results} == {c['id'] for c in catalog}
assert all(c['status'] == 'PASS' for c in results)
summary = {mode:{'activities':len([c for c in results if c['workspace']==mode]),'passed':len([c for c in results if c['workspace']==mode and c['status']=='PASS'])} for mode in ['graph2d','graph3d','geometry2d','geometry3d']}
(evidence/'results.json').write_text(json.dumps({'total':150,'passed':150,'failed':0,'workspaces':summary,'results':results},indent=2),encoding='utf-8')
by_id = {c['id']:c for c in results}
Path('docs/qa/workspace-ux-150-activities.txt').write_text(
    '150 UX activities — all passed on Android API 35 emulator-5556.\n'
    '38 each for 2D/3D graphs; 37 each for 2D/3D geometry.\n'
    'Method: scripted UI touch instrumentation, with Back/lifecycle simulation.\n'
    'A separate direct ADB smoke check covers the reported 2D graph flow.\n'
    'Expected: background interactions preserve mathematical objects and open controls.\n'
    'Explicit Close/Back or panel changes may close controls. Add changes object count only.\n\n'+
    '\n\n'.join(f"{c['id']} | PASS\nActivity: {c['activity']}\nObjects after: {by_id[c['id']]['objectCount']} | Inspector open: {by_id[c['id']]['inspectorOpen']}" for c in catalog)+'\n',encoding='utf-8')
print(json.dumps(summary))
