import fs from "node:fs/promises";
import { FileBlob, SpreadsheetFile } from "@oai/artifact-tool";

const source = "C:/Users/saisa/Downloads/Maths_Curriculum_Class1_to_PhD_Master.xlsx";
const output = "C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit/Maths_Curriculum_with_ids.xlsx";
const previewPath = "C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit/lesson_ids_preview.png";
const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(source));
const sheet = workbook.worksheets.getItem("Maths Curriculum");
const rows = sheet.getRange("A2:D1988").values;
const counts = new Map();
const ids = rows.map((row, index) => {
  const level = String(row[0] ?? "UNSPECIFIED").toUpperCase().replace(/[^A-Z0-9]+/g, "-").replace(/^-|-$/g, "");
  const count = (counts.get(level) ?? 0) + 1;
  counts.set(level, count);
  return [`MATH-${level}-${String(count).padStart(4, "0")}`];
});

sheet.getRange("K1:K1988").copyFrom(sheet.getRange("J1:J1988"), "all");
sheet.getRange("K1").values = [["Lesson ID"]];
sheet.getRange("K2:K1988").values = ids;
sheet.getRange("K1:K1988").format.columnWidth = 26;
sheet.getRange("K1").format.font = { bold: true };

const preview = await workbook.render({ sheetName: "Maths Curriculum", range: "A1:K10", scale: 1.2, format: "png" });
await fs.writeFile(previewPath, new Uint8Array(await preview.arrayBuffer()));
const exported = await SpreadsheetFile.exportXlsx(workbook);
await exported.save(output);
const inspection = await workbook.inspect({ kind: "table", sheetId: "Maths Curriculum", range: "A1:K8", include: "values,formulas", tableMaxRows: 8, tableMaxCols: 11, maxChars: 9000 });
await fs.writeFile("C:/Indian Servers/AIExplorer/.codex_tmp_lesson_audit/lesson_ids_inspection.txt", inspection.ndjson, "utf8");
