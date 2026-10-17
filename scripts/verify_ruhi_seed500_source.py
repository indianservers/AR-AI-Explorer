"""Read-only original workbook/Android/replay fidelity check; never regenerate expected labels."""
import argparse,base64,hashlib,json,re,zipfile,xml.etree.ElementTree as ET
from pathlib import Path
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--cycle',type=int,required=True);args=parser.parse_args()
folder=root/'docs/qa/ruhi-500'
provenance=json.loads((folder/'source.json').read_text(encoding='utf-8-sig'))
workbook=Path(provenance['source']);digest=hashlib.sha256(workbook.read_bytes()).hexdigest()
errors=[]
if digest!=provenance['sha256']:errors.append('Original workbook SHA-256 changed')
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(workbook) as z:
    shared=[''.join(e.itertext()) for e in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('s:si',ns)] if 'xl/sharedStrings.xml' in z.namelist() else []
    rows=[]
    for row in ET.fromstring(z.read('xl/worksheets/sheet1.xml')).findall('.//s:sheetData/s:row',ns)[1:]:
        cells={}
        for cell in row.findall('s:c',ns):
            v=cell.find('s:v',ns);kind=cell.get('t')
            cells[re.sub(r'\d+','',cell.get('r'))]=shared[int(v.text)] if kind=='s' and v is not None else ''.join(cell.find('s:is',ns).itertext()) if kind=='inlineStr' else v.text if v is not None else ''
        rows.append(dict(id=int(cells['A']),category=cells['B'],text=cells['C'],intent=cells['D'],engine=cells['E'],expected=cells['F'],parameters=json.loads(cells['G']),follow_up=cells['H'],suggestions=cells['I']))
if [r['id'] for r in rows]!=list(range(1,501)):errors.append('Original row IDs/count')
if len({r['text'].casefold() for r in rows})!=500:errors.append('Original command uniqueness')
checks={}
for label,path in [('seed_rows',folder/'seed-rows.json'),('android_asset',root/'app/src/main/assets/ruhi_semantic_nlp/seed_examples.json')]:
    actual=json.loads(path.read_text(encoding='utf-8-sig'));checks[label]=actual==rows
    if not checks[label]:errors.append(label+' differs from all original fields')
lines=(folder/'replay.tsv').read_text(encoding='utf-8').splitlines();replay_ok=len(lines)==len(rows)
for r,line in zip(rows,lines):
    c=line.split('\t');decode=lambda i:base64.b64decode(c[i],validate=True).decode('utf-8')
    a=r['parameters'].get('point1',r['parameters'].get('a',''));b=r['parameters'].get('point2',r['parameters'].get('b',''))
    matched=len(c)==9 and int(c[0])==r['id'] and decode(1)==r['text'] and c[2]==r['intent'] and c[3]==r['engine'] and c[4]==str(a) and c[5]==str(b) and json.loads(decode(6))==r['parameters'] and decode(7)==r['follow_up'] and decode(8)==r['suggestions']
    if not matched:replay_ok=False;errors.append('replay row '+str(r['id']))
if not replay_ok and not any(e.startswith('replay') for e in errors):errors.append('replay count')
result=dict(passed=not errors,cycle=args.cycle,source_sha256=digest,rows=len(rows),unique_commands=len({r['text'].casefold() for r in rows}),fields_per_row=list(rows[0]),seed_rows_match=checks['seed_rows'],android_asset_rows_match=checks['android_asset'],replay_fields_match=replay_ok,errors=errors,scope='Read-only comparison with original workbook and pinned hash, all nine source fields and all replay columns; no mathematical behavior claim.')
(folder/f'cycle-{args.cycle}-source-fields-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result));raise SystemExit(0 if result['passed'] else 1)
