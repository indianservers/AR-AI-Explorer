from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json,re

tmp=Path(r"C:\Indian Servers\AIExplorer\.codex_tmp_lesson_audit")
root=Path(r"C:\Users\saisa\.codex\generated_images\01a0032b-d6bc-7f91-bb33-355182f94655")
known=set()
for p in tmp.glob("generated_*.json"):
    if "recovered" in p.name or p.name=="generated_591_670.json": continue
    try: rows=json.loads(p.read_text(encoding="utf-8"))
    except Exception: continue
    for r in rows:
        s=str(r.get("source", ""))
        names=re.findall(r"exec-[0-9a-f-]+\.png",s,re.I)
        if names: known.add(str(root/names[-1]).lower())
unknown=[p for p in root.glob("*.png") if str(p).lower() not in known]
unknown=sorted(unknown,key=lambda p:p.stat().st_mtime)[-80:]
records=[{"slot":i+1,"source":str(p),"mtime":p.stat().st_mtime} for i,p in enumerate(unknown)]
(tmp/"second_batch_unmapped.json").write_text(json.dumps(records,indent=2),encoding="utf-8")
out=tmp/"second_batch_sheets";out.mkdir(exist_ok=True)
font=ImageFont.load_default()
for page in range(8):
 c=Image.new("RGB",(1600,1000),"white");d=ImageDraw.Draw(c)
 for j,p in enumerate(unknown[page*10:(page+1)*10]):
  im=Image.open(p).convert("RGB");im.thumbnail((300,420));x=(j%5)*320+10;y=(j//5)*500+35
  c.paste(im,(x+(300-im.width)//2,y));d.text((x,y-20),f"S{page*10+j+1:02d} {p.stem[-8:]}",fill="black",font=font)
 c.save(out/f"second_{page+1}.jpg",quality=88)
print(json.dumps({"known":len(known),"unknownSelected":len(unknown),"first":unknown[0].name,"last":unknown[-1].name}))
