import fs from "node:fs/promises";
import {FileBlob,SpreadsheetFile} from "@oai/artifact-tool";
const source="C:/Users/saisa/Downloads/Maths_Curriculum_Class1_to_PhD_Master.xlsx";
const wb=await SpreadsheetFile.importXlsx(await FileBlob.load(source));const sh=wb.worksheets.getItem("Maths Curriculum");
const startRow=600;const rows=sh.getRange(`A${startRow}:K900`).values;const records=[];
for(let i=0;i<rows.length;i++){const r=rows[i],excelRow=startRow+i;if(r[0]==="Class 10")records.push({ordinal:excelRow-1,excelRow,className:r[0],chapter:r[1],topic:r[2],subtopic:r[3],lessonId:r[10]})}
await fs.writeFile("lessons_class10.json",JSON.stringify(records,null,2));console.log(JSON.stringify({count:records.length,first:records[0],last:records.at(-1)},null,2));
