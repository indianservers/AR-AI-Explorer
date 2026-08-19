from PIL import Image, ImageDraw
from pathlib import Path
import sys, math
paths=[Path(x) for x in sys.argv[2:]]
thumbs=[]
for i,p in enumerate(paths):
    im=Image.open(p).convert("RGB")
    im.thumbnail((360,270))
    tile=Image.new("RGB",(380,310),"white")
    tile.paste(im,((380-im.width)//2,25))
    ImageDraw.Draw(tile).text((8,5),p.stem.split("_")[0]+"-"+p.stem.split("_")[1],fill="black")
    thumbs.append(tile)
cols=2; rows=math.ceil(len(thumbs)/cols)
sheet=Image.new("RGB",(cols*380,rows*310),(225,225,225))
for i,t in enumerate(thumbs): sheet.paste(t,((i%cols)*380,(i//cols)*310))
sheet.save(sys.argv[1])
