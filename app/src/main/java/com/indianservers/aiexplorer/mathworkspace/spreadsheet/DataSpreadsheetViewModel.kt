package com.indianservers.aiexplorer.mathworkspace.spreadsheet

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop

class DataSpreadsheetViewModel: ViewModel() {
    val cells=mutableStateMapOf<CellAddress,String>();private val calculated=mutableStateMapOf<CellAddress,String>();val active=mutableStateOf(CellAddress(1,1));val selection=mutableStateOf(CellRange(active.value,active.value));val draft=mutableStateOf("");val rows=mutableIntStateOf(50);val columns=mutableIntStateOf(26);val sheet=mutableStateOf(WorkspaceSheetStop.Collapsed);val chart=mutableStateOf(false);val chartType=mutableStateOf("Bar");val chartXColumn=mutableStateOf("A");val chartYColumn=mutableStateOf("B")
    val displayed:Map<CellAddress,String> get()=calculated
    private fun recalculate(){calculated.clear();calculated.putAll(FormulaEngine(cells.toMap()).values())}
    private val history=ArrayDeque<Map<CellAddress,String>>()
    private fun checkpoint(){history.addLast(cells.toMap());while(history.size>50)history.removeFirst()}
    fun select(a:CellAddress){active.value=a;selection.value=CellRange(a,a);draft.value=cells[a].orEmpty()}
    fun commit(){val a=active.value;val v=draft.value;if(cells[a].orEmpty()!=v){checkpoint();if(v.isBlank())cells.remove(a)else cells[a]=v;recalculate()};draft.value=cells[a].orEmpty()}
    fun put(a:CellAddress,v:String){if(cells[a].orEmpty()!=v){checkpoint();if(v.isBlank())cells.remove(a)else cells[a]=v;recalculate()}}
    fun replaceAll(values:Map<CellAddress,String>){checkpoint();cells.clear();cells.putAll(values);recalculate()}
    fun updateMany(values:Map<CellAddress,String>){if(values.any{(a,v)->cells[a].orEmpty()!=v}){checkpoint();values.forEach{(a,v)->if(v.isBlank())cells.remove(a)else cells[a]=v};recalculate()}}
    fun undo(){history.removeLastOrNull()?.let{snapshot->cells.clear();cells.putAll(snapshot);recalculate()}}
    fun insertRow(at:Int=active.value.row){checkpoint();val shifted=cells.toMap();cells.clear();shifted.forEach{(a,v)->cells[if(a.row>=at)CellAddress(a.row+1,a.column)else a]=v};rows.intValue++;recalculate()}
    fun deleteRow(at:Int=active.value.row){if(rows.intValue<=1)return;checkpoint();val shifted=cells.toMap();cells.clear();shifted.forEach{(a,v)->if(a.row!=at)cells[if(a.row>at)CellAddress(a.row-1,a.column)else a]=v};rows.intValue--;recalculate();select(CellAddress(at.coerceAtMost(rows.intValue),active.value.column))}
    fun insertColumn(at:Int=active.value.column){checkpoint();val shifted=cells.toMap();cells.clear();shifted.forEach{(a,v)->cells[if(a.column>=at)CellAddress(a.row,a.column+1)else a]=v};columns.intValue++;recalculate()}
    fun deleteColumn(at:Int=active.value.column){if(columns.intValue<=1)return;checkpoint();val shifted=cells.toMap();cells.clear();shifted.forEach{(a,v)->if(a.column!=at)cells[if(a.column>at)CellAddress(a.row,a.column-1)else a]=v};columns.intValue--;recalculate();select(CellAddress(active.value.row,at.coerceAtMost(columns.intValue)))}
    fun selectRange(end:CellAddress){selection.value=CellRange(active.value,end)}
    fun paste(text:String){val lines=text.trimEnd().split(Regex("\\r?\\n"));val values=buildMap{lines.forEachIndexed{r,line->line.split('\t').forEachIndexed{c,v->put(CellAddress(active.value.row+r,active.value.column+c),v)}}};updateMany(values)}
    fun copyText():String {val s=selection.value;return s.rows.joinToString("\n"){r->s.columns.joinToString("\t"){c->cells[CellAddress(r,c)].orEmpty()}}}
    fun statistics():SelectionStatistics {val s=selection.value;val n=s.rows.flatMap{r->s.columns.mapNotNull{c->displayed[CellAddress(r,c)]?.toDoubleOrNull()}};val sorted=n.sorted();val median=sorted.takeIf{it.isNotEmpty()}?.let{if(it.size%2==1)it[it.size/2] else (it[it.size/2-1]+it[it.size/2])/2};val mean=n.average().takeIf{n.isNotEmpty()};return SelectionStatistics(n.size,n.sum(),mean,n.minOrNull(),n.maxOrNull(),median,mean?.let{m->kotlin.math.sqrt(n.sumOf{(it-m)*(it-m)}/n.size)}) }
    fun csv():String=(1..rows.intValue).joinToString("\n"){r->(1..columns.intValue).joinToString(","){c->val v=cells[CellAddress(r,c)].orEmpty();if(v.any{it==','||it=='"'||it=='\n'})"\"${v.replace("\"","\"\"")}\"" else v}}
}
