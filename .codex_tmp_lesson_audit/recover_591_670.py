from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json

root = Path(r"C:\Users\saisa\.codex\generated_images\01a0032b-d6bc-7f91-bb33-355182f94655")
out = Path(r"C:\Indian Servers\AIExplorer\.codex_tmp_lesson_audit\recovery_sheets")
out.mkdir(parents=True, exist_ok=True)
files = sorted(root.glob("*.png"), key=lambda p: p.stat().st_mtime)[-80:]
records = [{"ordinal": 591+i, "source": str(p), "mtime": p.stat().st_mtime} for i,p in enumerate(files)]
Path(r"C:\Indian Servers\AIExplorer\.codex_tmp_lesson_audit\generated_591_670_recovered.json").write_text(json.dumps(records, indent=2), encoding="utf-8")
font = ImageFont.load_default()
for page in range(8):
    canvas = Image.new("RGB", (1600, 1000), "white")
    draw = ImageDraw.Draw(canvas)
    for j,p in enumerate(files[page*10:(page+1)*10]):
        im = Image.open(p).convert("RGB")
        im.thumbnail((300, 420))
        x=(j%5)*320+10; y=(j//5)*500+35
        canvas.paste(im,(x+(300-im.width)//2,y))
        draw.text((x,y-20), f"{591+page*10+j}  {p.stem[-8:]}", fill="black", font=font)
    canvas.save(out/f"recovery_{591+page*10}_{600+page*10}.jpg", quality=88)
print(json.dumps({"count":len(files),"first":files[0].name,"last":files[-1].name}))
