package com.indianservers.aiexplorer.mathworkspace.spreadsheet

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceAction
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop

@Composable
fun DataSpreadsheetWorkspace(vm: DataSpreadsheetViewModel,onBack:()->Unit){
    val clipboard= LocalClipboardManager.current;val context= LocalContext.current
    var editing by remember { mutableStateOf(false) }
    val import= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->uri?.let{runCatching{context.contentResolver.openInputStream(it)?.bufferedReader()?.use{reader->parseCsv(reader.readText())}}.getOrNull()?.let{table->val values=buildMap{table.forEachIndexed{r,row->row.forEachIndexed{c,v->if(v.isNotEmpty())put(CellAddress(r+1,c+1),v)}}};vm.replaceAll(values);vm.rows.intValue=maxOf(vm.rows.intValue,table.size);vm.columns.intValue=maxOf(vm.columns.intValue,table.maxOfOrNull{it.size}?:1)}}}
    val export= rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")){uri->uri?.let{runCatching{context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use{w->w.write(vm.csv())}}}}
    MathWorkspaceShell(title="Data Table & Spreadsheet",subtitle="Cells · formulas · data analysis",onBack=onBack,sheetTitle="Selection & Data",sheetStop=vm.sheet.value,onSheetStopChange={vm.sheet.value=it},
        canvas={
            Column(Modifier.fillMaxSize().padding(bottom=62.dp)) {
                Row(Modifier.fillMaxWidth().background(P.panel).padding(horizontal=5.dp, vertical=3.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(vm.active.value.toString(),Modifier.width(48.dp),color=P.blue,fontSize=12.sp,fontWeight=FontWeight.Bold)
                    BasicTextField(vm.draft.value,{vm.draft.value=it;editing=true},Modifier.weight(1f).heightIn(min=34.dp).background(P.snow,RoundedCornerShape(7.dp)).padding(8.dp).semantics{contentDescription="Formula bar for ${vm.active.value}"},singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done),textStyle=androidx.compose.ui.text.TextStyle(color=P.ink,fontSize=13.sp),decorationBox={inner->if(vm.draft.value.isEmpty())Text("Value or formula…",color=P.muted,fontSize=12.sp);inner()})
                    WorkspaceAction("✓","Commit cell",{vm.commit();editing=false})
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(P.panelBlue),horizontalArrangement=Arrangement.spacedBy(3.dp),verticalAlignment=Alignment.CenterVertically){
                    WorkspaceAction("+ row","Insert row at active cell",{vm.insertRow()})
                    WorkspaceAction("+ col","Insert column at active cell",{vm.insertColumn()})
                    WorkspaceAction("Copy","Copy selected cells",{clipboard.setText(AnnotatedString(vm.copyText()))})
                    WorkspaceAction("Cut","Cut selected cells",{clipboard.setText(AnnotatedString(vm.copyText()));val s=vm.selection.value;vm.updateMany(s.rows.flatMap{r->s.columns.map{c->CellAddress(r,c) to ""}}.toMap())})
                    WorkspaceAction("Paste","Paste TSV cells",{clipboard.getText()?.text?.let(vm::paste)})
                    WorkspaceAction("CSV ↓","Import CSV/TSV",{import.launch(arrayOf("text/*","text/csv","text/tab-separated-values"))})
                    WorkspaceAction("CSV ↑","Export CSV",{export.launch("math-data.csv")})
                    WorkspaceAction("Chart","Chart selected cells",{val s=vm.selection.value;vm.chartXColumn.value=CellAddress.columnName(s.columns.first);vm.chartYColumn.value=CellAddress.columnName(s.columns.last);vm.chart.value=true})
                }
                SpreadsheetGrid(vm)
            }
        },
        tools={}
    ) { Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(6.dp)) {
        val s=vm.selection.value;val values=vm.statistics()
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically){Text("${s.start}:${s.end}",color=P.blue,fontSize=10.sp,fontWeight=FontWeight.Bold);Text("n ${values.count}",color=P.muted,fontSize=10.sp);Text("Σ ${fmt(values.sum)}",color=P.ink,fontSize=10.sp);Text("μ ${values.mean?.let(::fmt)?:"—"}",color=P.ink,fontSize=10.sp);Text("min ${values.min?.let(::fmt)?:"—"}",color=P.ink,fontSize=10.sp);Text("max ${values.max?.let(::fmt)?:"—"}",color=P.ink,fontSize=10.sp);Text("median ${values.median?.let(::fmt)?:"—"}",color=P.ink,fontSize=10.sp);Text("σ ${values.standardDeviation?.let(::fmt)?:"—"}",color=P.ink,fontSize=10.sp)}
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            WorkspaceAction("Ascending","Sort selected rows ascending",{sortSelection(vm,true)})
            WorkspaceAction("Descending","Sort selected rows descending",{sortSelection(vm,false)})
            WorkspaceAction("Delete cells","Clear selection",{vm.updateMany(s.rows.flatMap{r->s.columns.map{c->CellAddress(r,c) to ""}}.toMap())})
            WorkspaceAction("− row","Delete active row",{vm.deleteRow()})
            WorkspaceAction("− col","Delete active column",{vm.deleteColumn()})
            WorkspaceAction("Undo","Undo last edit",{vm.undo()})
        }
        if(vm.chart.value){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalAlignment=Alignment.CenterVertically){ChartColumnInput("X",vm.chartXColumn.value){vm.chartXColumn.value=it.uppercase()};ChartColumnInput("Y",vm.chartYColumn.value){vm.chartYColumn.value=it.uppercase()};listOf("Line","Bar","Scatter").forEach{WorkspaceAction(it,"${it} chart",{vm.chartType.value=it},vm.chartType.value==it)};WorkspaceAction("Close","Close chart",{vm.chart.value=false})};ChartPreview(vm)}
    }}
}

@Composable private fun SpreadsheetGrid(vm:DataSpreadsheetViewModel){
    val horizontal= rememberScrollState()
    Column(Modifier.fillMaxSize().padding(horizontal=5.dp)) {
            Row(Modifier.fillMaxWidth().height(29.dp).background(P.panelBlue),verticalAlignment=Alignment.CenterVertically){
            Text("",Modifier.width(40.dp));Row(Modifier.horizontalScroll(horizontal)){(1..vm.columns.intValue).forEach{c->Text(CellAddress.columnName(c),Modifier.width(76.dp).fillMaxHeight().border(.5.dp,P.border).wrapContentHeight(Alignment.CenterVertically),color=P.ink,fontSize=11.sp,fontWeight=FontWeight.Bold,textAlign=androidx.compose.ui.text.style.TextAlign.Center)}}
        }
    LazyColumn(Modifier.fillMaxSize()) { items((1..vm.rows.intValue).toList(),key={it}){r->Row(Modifier.height(34.dp),verticalAlignment=Alignment.CenterVertically){Text(r.toString(),Modifier.width(40.dp).fillMaxHeight().background(P.panelBlue).border(.5.dp,P.border).wrapContentHeight(Alignment.CenterVertically),color=P.muted,fontSize=10.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center);Row(Modifier.horizontalScroll(horizontal)){(1..vm.columns.intValue).forEach{c->val a=CellAddress(r,c);val selected=a==vm.active.value;val range=vm.selection.value;val inRange=r in range.rows&&c in range.columns;val value=vm.displayed[a].orEmpty();Box(Modifier.width(76.dp).fillMaxHeight().background(if(selected)Color(0xFFE0EDFF) else if(inRange)Color(0xFFF0F6FF) else Color.White).border(.5.dp,if(selected)P.blue else P.border).combinedClickable(onClick={vm.select(a)},onLongClick={if(vm.active.value==a)vm.selectRange(a)else vm.selectRange(a)}).semantics{contentDescription="Cell $a, ${value.ifBlank{"empty"}}"}.padding(horizontal=4.dp),contentAlignment=Alignment.CenterStart){Text(value,color=if(value.startsWith("#"))P.red else P.ink,fontSize=11.sp,maxLines=1)}}}}
        }}
    }
}

@Composable private fun ChartPreview(vm:DataSpreadsheetViewModel){
    val s=vm.selection.value;val xColumn=CellAddress.parse("${vm.chartXColumn.value}1")?.column?:s.columns.first;val yColumn=CellAddress.parse("${vm.chartYColumn.value}1")?.column?:s.columns.last;val points=s.rows.mapNotNull{r->val x=vm.displayed[CellAddress(r,xColumn)]?.toDoubleOrNull();val y=vm.displayed[CellAddress(r,yColumn)]?.toDoubleOrNull();if(x!=null&&y!=null)x to y else null}
    Canvas(Modifier.fillMaxWidth().height(112.dp).background(P.snow,RoundedCornerShape(12.dp)).padding(10.dp)){if(points.isNotEmpty()){val loX=points.minOf{it.first};val hiX=points.maxOf{it.first}.let{if(it==loX)loX+1 else it};val loY=points.minOf{it.second};val hiY=points.maxOf{it.second}.let{if(it==loY)loY+1 else it};val mapped=points.map{androidx.compose.ui.geometry.Offset(((it.first-loX)/(hiX-loX)*size.width).toFloat(),(size.height-(it.second-loY)/(hiY-loY)*size.height).toFloat())};mapped.forEachIndexed{i,p->if(vm.chartType.value=="Bar"){val w=size.width/(mapped.size*1.5f);drawRect(P.blue,topLeft=androidx.compose.ui.geometry.Offset(i*size.width/mapped.size,p.y),size=androidx.compose.ui.geometry.Size(w,size.height-p.y))}else{drawCircle(P.blue,5f,p);if(vm.chartType.value!="Scatter"&&i>0)drawLine(P.blue,mapped[i-1],p,3f)}}}}
}
private fun fmt(v:Double)=if(v.isFinite())"%.4g".format(java.util.Locale.US,v) else "—"
@Composable private fun ChartColumnInput(label:String,value:String,onChange:(String)->Unit){Row(Modifier.width(80.dp).background(P.snow,RoundedCornerShape(8.dp)).padding(6.dp),verticalAlignment=Alignment.CenterVertically){Text("$label ",color=P.muted,fontSize=10.sp);BasicTextField(value,onChange,Modifier.width(44.dp).semantics{contentDescription="$label chart data column"},singleLine=true,textStyle=androidx.compose.ui.text.TextStyle(color=P.ink,fontSize=12.sp))}}
private fun parseCsv(s:String):List<List<String>> {val delimiter=if(s.lineSequence().firstOrNull()?.count{it=='\t'}?:0 > (s.lineSequence().firstOrNull()?.count{it==','}?:0))'\t' else ',';val rows=mutableListOf<List<String>>();var row=mutableListOf<String>();val field=StringBuilder();var quoted=false;var i=0;while(i<s.length){val c=s[i++];if(quoted){if(c=='"'&&i<s.length&&s[i]=='"'){field.append('"');i++}else if(c=='"')quoted=false else field.append(c)}else when(c){'"'->quoted=true;delimiter->{row+=field.toString();field.clear()};'\n'->{row+=field.toString().removeSuffix("\r");rows+=row;row=mutableListOf();field.clear()};else->field.append(c)}};row+=field.toString();if(row.any{it.isNotEmpty()})rows+=row;return rows}
private fun sortSelection(vm:DataSpreadsheetViewModel,ascending:Boolean){val s=vm.selection.value;val allColumns=1..vm.columns.intValue;val original=s.rows.toList();val sorted=original.sortedBy{vm.displayed[CellAddress(it,s.start.column)]?.toDoubleOrNull()?:Double.POSITIVE_INFINITY}.let{if(ascending)it else it.reversed()};val snapshot=sorted.associateWith{r->allColumns.map{c->vm.cells[CellAddress(r,c)].orEmpty()}};vm.updateMany(buildMap{original.forEachIndexed{index,dest->snapshot[sorted[index]]?.forEachIndexed{j,v->put(CellAddress(dest,j+1),v)}}})}
