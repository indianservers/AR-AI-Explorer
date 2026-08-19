import fs from "node:fs/promises";
import path from "node:path";
const root="D:/Math Images";
const lessons=JSON.parse(await fs.readFile("lessons_371_470.json","utf8"));
const ids=new Set(lessons.map(x=>x.lessonId));
const pngs=[]; const metadataFiles=[];
async function walk(dir){for(const e of await fs.readdir(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())await walk(p);else if(e.name.toLowerCase().endsWith(".png"))pngs.push(p);else if(e.name==="metadata.json")metadataFiles.push(p);}}
await walk(root);
let approved=0; const allRecords=[];
for(const p of metadataFiles){const data=JSON.parse((await fs.readFile(p,"utf8")).replace(/^\uFEFF/,""));let changed=false;for(const im of data.images||[]){if(ids.has(im.lessonId)){im.qaStatus="approved";im.qaReviewedAt=new Date().toISOString();approved++;changed=true;}allRecords.push({lessonId:im.lessonId,file:path.join(path.dirname(p),im.fileName)});}if(changed){data.generatedAt=new Date().toISOString();data.imageCount=(data.images||[]).length;await fs.writeFile(p,JSON.stringify(data,null,2),"utf8");}}
const dupIds=[...new Set(allRecords.map(x=>x.lessonId).filter((x,i,a)=>a.indexOf(x)!==i))];
const missing=[];for(const r of allRecords){try{await fs.access(r.file);}catch{missing.push(r.file);}}
const batchRecords=allRecords.filter(x=>ids.has(x.lessonId));
console.log(JSON.stringify({pngCount:pngs.length,metadataFileCount:metadataFiles.length,metadataRecordCount:allRecords.length,batchApproved:approved,batchMetadataRecords:batchRecords.length,duplicateLessonIds:dupIds.length,missingReferencedFiles:missing.length},null,2));
