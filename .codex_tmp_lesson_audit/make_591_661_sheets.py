from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,math
root=Path(r"D:\Math Images");tmp=Path(r"C:\Indian Servers\AIExplorer\.codex_tmp_lesson_audit")
lessons=json.loads((tmp/"lessons_591_690.json").read_text(encoding="utf-8"))[:71]
paths=[]
for r in lessons:
 d=root/r["className"]/r["chapter"]/r["topic"]
 p=next(d.glob(r["lessonId"]+"_*.png"));paths.append(p)
out=tmp/"qa_591_661";out.mkdir(exist_ok=True);font=ImageFont.load_default()
for page in range(math.ceil(len(paths)/10)):
 c=Image.new("RGB",(1600,1000),"white");dr=ImageDraw.Draw(c)
 for j,p in enumerate(paths[page*10:(page+1)*10]):
  im=Image.open(p).convert("RGB");im.thumbnail((300,420));x=(j%5)*320+10;y=(j//5)*500+35
  c.paste(im,(x+(300-im.width)//2,y));dr.text((x,y-20),f"{591+page*10+j} {p.stem.split('_')[0]}",fill="black",font=font)
 c.save(out/f"qa_{591+page*10}_{min(600+page*10,661)}.jpg",quality=88)
print(len(paths))
