package com.indianservers.aiexplorer.mathworkspace.spreadsheet

data class CellAddress(val row: Int, val column: Int) {
    init { require(row >= 1 && column >= 1) }
    override fun toString(): String = columnName(column) + row
    companion object {
        fun columnName(index: Int): String {
            require(index >= 1)
            var n = index
            val out = StringBuilder()
            while (n > 0) { n--; out.append(('A'.code + n % 26).toChar()); n /= 26 }
            return out.reverse().toString()
        }
        fun parse(raw: String): CellAddress? {
            val m = Regex("^\\$?([A-Za-z]+)\\$?([1-9][0-9]*)$").matchEntire(raw.trim()) ?: return null
            val col = m.groupValues[1].uppercase().fold(0L) { a, c -> a * 26 + (c - 'A' + 1) }.takeIf { it <= Int.MAX_VALUE }?.toInt() ?: return null
            val row = m.groupValues[2].toIntOrNull() ?: return null
            return runCatching { CellAddress(row, col) }.getOrNull()
        }
    }
}
