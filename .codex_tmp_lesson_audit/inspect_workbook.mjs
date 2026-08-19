import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = "C:/Users/saisa/Downloads/Maths_Curriculum_Class1_to_PhD_Master.xlsx";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
const sheet = workbook.worksheets.getItem("Maths Curriculum");
const rows = sheet.getRange("A472:K671").values;
const records = rows.map(r => ({
  className: r[0], chapter: r[1], topic: r[2], subtopic: r[3], lessonId: r[10]
}));
await fs.writeFile("lessons_471_670.json", JSON.stringify(records, null, 2));
console.log(JSON.stringify({count: records.length, first: records[0], last: records.at(-1)}, null, 2));
