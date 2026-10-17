"""Read the supplied seed workbook without changing it; preserve every labeled field."""
import argparse,base64,hashlib,json,re,zipfile,xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('workbook',type=Path);args=parser.parse_args()
ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
with zipfile.ZipFile(args.workbook) as z:
    shared=[''.join(e.itertext()) for e in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('s:si',ns)] if 'xl/sharedStrings.xml' in z.namelist() else []
    records=[]
    for row in ET.fromstring(z.read('xl/worksheets/sheet1.xml')).findall('.//s:sheetData/s:row',ns)[1:]:
        cells={}
        for c in row.findall('s:c',ns):
            v=c.find('s:v',ns);t=c.get('t')
            cells[re.sub(r'\d+','',c.get('r'))]=shared[int(v.text)] if t=='s' and v is not None else ''.join(c.find('s:is',ns).itertext()) if t=='inlineStr' else v.text if v is not None else ''
        records.append(dict(id=int(cells['A']),category=cells['B'],text=cells['C'],intent=cells['D'],engine=cells['E'],expected=cells['F'],parameters=json.loads(cells['G']),follow_up=cells['H'],suggestions=cells['I']))
assert [r['id'] for r in records]==list(range(1,501))
assert len({r['text'].casefold() for r in records})==500
folder=root/'docs/qa/ruhi-500';folder.mkdir(parents=True,exist_ok=True)
(folder/'seed-rows.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
def b64(value):return base64.b64encode(str(value).encode()).decode()
lines=[]
for r in records:
    p=r['parameters'];a=p.get('point1',p.get('a',''));b=p.get('point2',p.get('b',''))
    lines.append('\t'.join(map(str,[r['id'],b64(r['text']),r['intent'],r['engine'],a,b,b64(json.dumps(p,sort_keys=True)),b64(r['follow_up']),b64(r['suggestions'])])))
(folder/'replay.tsv').write_text('\n'.join(lines)+'\n',encoding='utf-8')
provenance=dict(source=str(args.workbook.resolve()),sha256=hashlib.sha256(args.workbook.read_bytes()).hexdigest(),sheet='500 NLP Commands',rows=500,intents=dict(Counter(r['intent'] for r in records)),source_fields_preserved=True)
(folder/'source.json').write_text(json.dumps(provenance,indent=2),encoding='utf-8')
print(json.dumps(provenance))
