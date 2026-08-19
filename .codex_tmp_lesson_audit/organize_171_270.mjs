import fs from "node:fs/promises";
import path from "node:path";
const tmp="C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit";
const root="D:/Math Images";
const lessons=JSON.parse(await fs.readFile(path.join(tmp,"lessons_171_270.json"),"utf8"));
const generated=JSON.parse(await fs.readFile(path.join(tmp,"generated_171_270.json"),"utf8"));
const slug=s=>s.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"");
function model(r){
 const t=(r.topic+" "+r.subtopic).toLowerCase();
 if(/money|bill|receipt|change/.test(t))return"Indian currency, price cards, and exact arithmetic";
 if(/clock|time|calendar/.test(t))return"accurate clocks, timelines, or calendar models";
 if(/graph|pictograph|tally|frequency|data/.test(t))return"an exact, labelled visual data display";
 if(/addition|subtraction|regroup/.test(t))return"place-value models and a verified worked equation";
 if(/multiplication|factor|multiple/.test(t))return"equal groups, arrays, or structured partial products";
 if(/division|remainder/.test(t))return"equal grouping with exact counts and division notation";
 if(/fraction|numerator|denominator|tenths|hundredths|decimal/.test(t))return"equal partitions, place-value structure, and matching notation";
 if(/angle|point|line|ray|polygon|triangle|quadrilateral|circle|symmetry|tessellation|face|edge|vertice|net|view/.test(t))return"property-labelled geometric diagrams with precise construction";
 if(/millimetre|centimetre|metre|kilometre|gram|kilogram|millilitre|litre|perimeter|conversion/.test(t))return"accurate instruments, unit labels, and real-world benchmarks";
 if(/rounding/.test(t))return"a scaled number line with midpoint and nearest benchmark";
 if(/roman/.test(t))return"correct Roman numeral symbols and construction examples";
 if(/pattern|input-output/.test(t))return"an explicit growing sequence or rule machine";
 return"place-value charts and concrete number representations";
}
let copied=0;
for(let i=0;i<lessons.length;i++){
 const r=lessons[i],g=generated[i];
 if(!g?.source||g.ordinal!==171+i)throw new Error("Mapping mismatch at "+i);
 const dir=path.join(root,r.className,r.chapter,r.topic);
 await fs.mkdir(dir,{recursive:true});
 const fileName=r.lessonId+"_"+slug(r.subtopic)+".png";
 await fs.copyFile(g.source,path.join(dir,fileName));
 const metadataPath=path.join(dir,"metadata.json");
 let payload={schemaVersion:"1.0",class:r.className,chapter:r.chapter,topic:r.topic,generatedAt:new Date().toISOString(),imageCount:0,images:[]};
 try{payload=JSON.parse((await fs.readFile(metadataPath,"utf8")).replace(/^\uFEFF/,""));}catch{}
 payload.images=(payload.images||[]).filter(x=>x.lessonId!==r.lessonId);
 const relativePath=path.relative(path.join(root,r.className),path.join(dir,fileName)).replaceAll("\\","/");
 const visualModel=model(r);
 payload.images.push({lessonId:r.lessonId,class:r.className,chapter:r.chapter,topic:r.topic,subtopic:r.subtopic,imageType:"inline concept visual",fileName,relativePath,description:"A lesson-specific visual for "+r.subtopic+", built with "+visualModel+".",learningObjective:"Help learners understand "+r.subtopic+" through a concrete, visually checkable model.",altText:"Educational illustration explaining "+r.subtopic+" using "+visualModel+".",promptType:"scientific-educational",qaStatus:"needs_review"});
 payload.images.sort((a,b)=>a.lessonId.localeCompare(b.lessonId));
 payload.imageCount=payload.images.length;payload.generatedAt=new Date().toISOString();
 await fs.writeFile(metadataPath,JSON.stringify(payload,null,2),"utf8");
 copied++;
}
console.log(JSON.stringify({copied,first:lessons[0].lessonId,last:lessons.at(-1).lessonId},null,2));
