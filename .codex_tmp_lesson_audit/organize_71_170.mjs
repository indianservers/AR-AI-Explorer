import fs from "node:fs/promises";
import path from "node:path";
const tmp="C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit";
const root="D:/Math Images";
const lessons=JSON.parse(await fs.readFile(path.join(tmp,"lessons_71_170.json"),"utf8"));
const generated=JSON.parse(await fs.readFile(path.join(tmp,"generated_71_170.json"),"utf8"));
const slug=s=>s.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"");
function model(r){
 const t=(r.topic+" "+r.subtopic).toLowerCase();
 if(/money|coin|note|bill|purchase|currency/.test(t))return"Indian currency objects, price cards, and exact arithmetic";
 if(/clock|hour/.test(t))return"accurate analog and digital clock representations";
 if(/calendar|day|week|month/.test(t))return"calendar grids, ordered time units, and highlighted dates";
 if(/pattern/.test(t))return"an explicit repeating or growing sequence with its rule visibly marked";
 if(/tally|pictograph|table|data|frequent|categories|sorting|classifying/.test(t))return"countable categories, aligned records, and an exact visual data display";
 if(/addition|subtraction/.test(t))return"place-value blocks, aligned columns, and a verified worked equation";
 if(/multiplication|table of|array|equal group|repeated addition/.test(t))return"equal groups, arrays, and matching multiplication facts";
 if(/division|sharing|grouping/.test(t))return"equal sharing or grouping with exact object counts and division notation";
 if(/fraction|half|third|fourth|whole|equal part/.test(t))return"equal partitions with shaded parts and matching fraction notation";
 if(/shape|line|symmetry|solid|side|corner/.test(t))return"property-labeled geometric diagrams and carefully chosen examples";
 if(/centimetre|metre|gram|kilogram|litre|measurement|length/.test(t))return"accurate measuring instruments, unit labels, and comparison benchmarks";
 return"place-value charts, number lines, and concrete number representations";
}
let copied=0;
for(let i=0;i<lessons.length;i++){
 const r=lessons[i],g=generated[i];
 if(!g?.source||g.ordinal!==71+i)throw new Error("Mapping mismatch at "+i);
 const dir=path.join(root,r.className,r.chapter,r.topic);
 await fs.mkdir(dir,{recursive:true});
 const fileName=r.lessonId+"_"+slug(r.subtopic)+".png";
 const source=g.source.includes(" as ")?g.source.split(" as ").at(-1):g.source;
 await fs.copyFile(source,path.join(dir,fileName));
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
