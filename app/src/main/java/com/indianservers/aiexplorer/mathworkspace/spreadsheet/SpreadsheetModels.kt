package com.indianservers.aiexplorer.mathworkspace.spreadsheet

data class SpreadsheetCell(val raw: String = "", val value: String = "")
data class CellRange(val start: CellAddress, val end: CellAddress) {
    val rows get() = minOf(start.row, end.row)..maxOf(start.row, end.row)
    val columns get() = minOf(start.column, end.column)..maxOf(start.column, end.column)
}
data class SelectionStatistics(val count: Int, val sum: Double, val mean: Double?, val min: Double?, val max: Double?, val median: Double?, val standardDeviation: Double?)
