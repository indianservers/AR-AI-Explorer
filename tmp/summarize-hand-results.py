from pathlib import Path
import xml.etree.ElementTree as ET
root=Path(r'C:\Indian Servers\AIExplorer')
for folder in ['arengine/build/test-results/testDebugUnitTest','app/build/test-results/testDebugUnitTest']:
    totals=[0,0,0]
    for p in (root/folder).glob('TEST-*.xml'):
        s=ET.parse(p).getroot()
        for i,k in enumerate(['tests','failures','errors']): totals[i]+=int(s.attrib.get(k,0))
    print(folder, dict(zip(['tests','failures','errors'],totals)))
