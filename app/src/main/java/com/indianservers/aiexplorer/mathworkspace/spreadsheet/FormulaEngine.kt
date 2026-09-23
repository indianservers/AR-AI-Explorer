package com.indianservers.aiexplorer.mathworkspace.spreadsheet

import kotlin.math.*

/** Deterministic local evaluator. It rebuilds the dependency graph on edit and memoizes each formula once. */
class FormulaEngine(private val source: Map<CellAddress, String>) {
    private val cache = mutableMapOf<CellAddress, String>()
    private val active = mutableSetOf<CellAddress>()
    fun value(address: CellAddress): String = evaluate(address)
    fun values(): Map<CellAddress, String> = source.keys.associateWith { evaluate(it) }
    private fun evaluate(a: CellAddress): String {
        cache[a]?.let { return it }
        val raw = source[a].orEmpty()
        if (!raw.startsWith("=")) return raw.also { cache[a] = it }
        if (!active.add(a)) return "#CIRCULAR!"
        val result = runCatching { Parser(raw.drop(1), ::evaluate).parse().let(::format) }.getOrElse { e ->
            when (e.message) { "#DIV/0!", "#REF!", "#VALUE!", "#NUM!", "#CIRCULAR!" -> e.message!!; else -> "#ERROR!" }
        }
        active.remove(a)
        cache[a] = result
        return result
    }
    private fun format(v: Double): String = when {
        !v.isFinite() -> "#NUM!"
        abs(v) < 1e12 && v == v.toLong().toDouble() -> v.toLong().toString()
        else -> "%.8g".format(java.util.Locale.US, v)
    }

    private class Parser(private val input: String, private val cell: (CellAddress) -> String) {
        private var i = 0
        fun parse(): Double { val v = comparison(); spaces(); if (i != input.length) error("formula"); return v }
        private fun comparison(): Double {
            var x = expression(); spaces()
            val op = listOf(">=", "<=", "<>", ">", "<", "=").firstOrNull { input.startsWith(it, i) } ?: return x
            i += op.length; val y = expression()
            return if (when (op) { ">" -> x > y; "<" -> x < y; ">=" -> x >= y; "<=" -> x <= y; "=" -> x == y; else -> x != y }) 1.0 else 0.0
        }
        private fun expression(): Double { var x = term(); while (true) { spaces(); x = when { take('+') -> x + term(); take('-') -> x - term(); else -> return x } } }
        private fun term(): Double { var x = power(); while (true) { spaces(); x = when { take('*') -> x * power(); take('/') -> { val y = power(); if (y == 0.0) error("#DIV/0!"); x / y }; else -> return x } } }
        private fun power(): Double { var x = unary(); spaces(); if (take('^')) x = x.pow(power()); return x }
        private fun unary(): Double { spaces(); return when { take('+') -> unary(); take('-') -> -unary(); else -> atom() } }
        private fun atom(): Double {
            spaces(); if (take('(')) { val x = comparison(); spaces(); if (!take(')')) error("formula"); return x }
            if (i >= input.length) error("formula")
            if (input[i].isDigit() || input[i] == '.') return number()
            val name = identifier()
            spaces()
            if (take('(')) {
                val args = mutableListOf<Double>(); var nonEmptyRangeCells = 0; var rangeNumericCells = 0; spaces()
                if (!take(')')) {
                    while (true) {
                        spaces()
                        val rest = input.substring(i)
                        val range = Regex("^(\\$?[A-Za-z]+\\$?[1-9][0-9]*)\\s*:\\s*(\\$?[A-Za-z]+\\$?[1-9][0-9]*)").find(rest)
                        if (range != null) {
                            val a = CellAddress.parse(range.groupValues[1]) ?: error("#REF!")
                            val b = CellAddress.parse(range.groupValues[2]) ?: error("#REF!")
                            i += range.range.last + 1
                            val cells = CellRange(a, b)
                            cells.rows.forEach { row -> cells.columns.forEach { col ->
                                val raw = cell(CellAddress(row, col))
                                if (raw.startsWith("#")) error(raw)
                                if (raw.isNotBlank()) nonEmptyRangeCells++
                                raw.toDoubleOrNull()?.let { args.add(it); rangeNumericCells++ }
                            } }
                        } else args += comparison()
                        spaces()
                        if (take(')')) break
                        if (!take(',')) error("formula")
                    }
                }
                return function(name, args, nonEmptyRangeCells, rangeNumericCells)
            }
            val address = CellAddress.parse(name) ?: error("#REF!")
            spaces()
            if (take(':')) {
                val end = CellAddress.parse(identifier()) ?: error("#REF!")
                val values = CellRange(address, end).let { r -> r.rows.flatMap { row -> r.columns.map { col -> cell(CellAddress(row, col)).toDoubleOrNull() ?: 0.0 } } }
                return values.sum()
            }
            val raw = cell(address)
            if (raw.startsWith("#")) error(raw)
            return raw.toDoubleOrNull() ?: if (raw.isBlank()) 0.0 else error("#VALUE!")
        }
        private fun function(name: String, a: List<Double>, nonEmptyRangeCells: Int, rangeNumericCells: Int): Double = when (name.uppercase()) {
            "SUM" -> a.sum(); "AVERAGE" -> if (a.isEmpty()) error("#DIV/0!") else a.average()
            "MIN" -> a.minOrNull() ?: 0.0; "MAX" -> a.maxOrNull() ?: 0.0; "COUNT" -> a.size.toDouble(); "COUNTA" -> (a.size-rangeNumericCells+nonEmptyRangeCells).toDouble()
            "MEDIAN" -> a.sorted().let { if (it.isEmpty()) 0.0 else if (it.size % 2 == 1) it[it.size/2] else (it[it.size/2-1]+it[it.size/2])/2 }
            "VAR" -> variance(a); "STDEV" -> sqrt(variance(a)); "ABS" -> one(a).let(::abs); "SQRT" -> one(a).let { if (it < 0) error("#NUM!") else sqrt(it) }
            "ROUND" -> { val n = a.getOrNull(1)?.toInt() ?: 0; one(a).let { round(it * 10.0.pow(n)) / 10.0.pow(n) } }
            "POWER" -> { if (a.size != 2) error("#VALUE!"); a[0].pow(a[1]) }
            "IF" -> { if (a.size !in 2..3) error("#VALUE!"); if (a[0] != 0.0) a[1] else a.getOrElse(2) { 0.0 } }
            "AND" -> if (a.all { it != 0.0 }) 1.0 else 0.0; "OR" -> if (a.any { it != 0.0 }) 1.0 else 0.0; "NOT" -> if (one(a) == 0.0) 1.0 else 0.0
            else -> error("#VALUE!")
        }
        private fun variance(a: List<Double>): Double { if (a.isEmpty()) return 0.0; val m = a.average(); return a.sumOf { (it-m)*(it-m) } / a.size }
        private fun one(a: List<Double>) = a.singleOrNull() ?: error("#VALUE!")
        private fun number(): Double { val start=i; while(i<input.length && (input[i].isDigit() || input[i]=='.')) i++; return input.substring(start,i).toDoubleOrNull() ?: error("formula") }
        private fun identifier(): String { spaces(); val start=i; while(i<input.length && (input[i].isLetterOrDigit() || input[i]=='$' || input[i]=='_')) i++; if(start==i) error("formula"); return input.substring(start,i) }
        private fun spaces() { while(i<input.length && input[i].isWhitespace()) i++ }
        private fun take(c: Char): Boolean { if(i<input.length && input[i]==c) { i++; return true }; return false }
    }
}
