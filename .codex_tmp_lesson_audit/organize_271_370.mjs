import fs from "node:fs/promises";
import path from "node:path";
const root="D:/Math Images",tmp="C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit";
const lessons=JSON.parse(await fs.readFile(path.join(tmp,"lessons_271_370.json"),"utf8"));
const generated=JSON.parse(await fs.readFile(path.join(tmp,"generated_271_370.json"),"utf8"));
const slug=s=>s.toLowerCase().replace(/[^a-z0-9]+/g,"-").replace(/^-|-$/g,"");
function model(r){const t=(r.topic+" "+r.subtopic).toLowerCase();
 if(/parallel|perpendicular|triangle|quadrilateral|rectangle|square|rhombus|circle|symmetry|mirror|rotation|tessellation|angle|coordinate/.test(t))return"a precise property-labelled geometric construction";
 if(/perimeter|area|volume|scale drawing/.test(t))return"a dimensioned grid, boundary, or unit-cube model";
 if(/conversion|mass|capacity|temperature|time|speed/.test(t))return"an accurate measurement scale, timeline, or unit model";
 if(/graph|plot|table|average|mean|mode|probability/.test(t))return"a checkable data display with exact values";
 if(/pattern|sequence|rule|function|unknown|equation/.test(t))return"a structured sequence, function machine, or balance model";
 if(/factor|multiple|prime|composite|divisibility|hcf|lcm/.test(t))return"arrays, lists, and number-line relationships";
 if(/fraction|percent|decimal/.test(t))return"equal partitions and aligned place-value notation";
 if(/multiplication|division|operation|bracket/.test(t))return"a verified step-by-step arithmetic model";
 if(/natural|whole|integer|number/.test(t))return"a place-value chart or scaled number line";
 return"a concrete, visually checkable mathematical model";}
for(let i=0;i<lessons.length;i++){const r=lessons[i],g=generated[i];if(!g?.source||g.ordinal!==271+i)throw new Error("Mapping mismatch "+i);const dir=path.join(root,r.className,r.chapter,r.topic);await fs.mkdir(dir,{recursive:true});const fileName=r.lessonId+"_"+slug(r.subtopic)+".png";await fs.copyFile(g.source,path.join(dir,fileName));const mp=path.join(dir,"metadata.json");let p={schemaVersion:"1.0",class:r.className,chapter:r.chapter,topic:r.topic,images:[]};try{p=JSON.parse((await fs.readFile(mp,"utf8")).replace(/^\uFEFF/,""));}catch{}p.images=(p.images||[]).filter(x=>x.lessonId!==r.lessonId);const vm=model(r);p.images.push({lessonId:r.lessonId,class:r.className,chapter:r.chapter,topic:r.topic,subtopic:r.subtopic,imageType:"inline concept visual",fileName,relativePath:path.relative(path.join(root,r.className),path.join(dir,fileName)).replaceAll("\\","/"),description:"A lesson-specific visual for "+r.subtopic+", built with "+vm+".",learningObjective:"Help learners understand "+r.subtopic+" through a concrete, visually checkable model.",altText:"Educational illustration explaining "+r.subtopic+" using "+vm+".",promptType:"scientific-educational",qaStatus:"needs_review"});p.images.sort((a,b)=>a.lessonId.localeCompare(b.lessonId));p.imageCount=p.images.length;p.generatedAt=new Date().toISOString();await fs.writeFile(mp,JSON.stringify(p,null,2),"utf8");}
console.log(JSON.stringify({copied:lessons.length,first:lessons[0].lessonId,last:lessons.at(-1).lessonId},null,2));
