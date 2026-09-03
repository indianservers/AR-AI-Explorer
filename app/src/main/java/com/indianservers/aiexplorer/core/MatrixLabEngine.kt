package com.indianservers.aiexplorer.core

import kotlin.math.abs
import kotlin.math.sqrt

data class ExactMatrix(val rows: List<List<ExactRational>>) {
    val rowCount: Int = rows.size
    val columnCount: Int = rows.firstOrNull()?.size ?: 0

    init {
        require(rows.isNotEmpty() && columnCount > 0) { "A matrix cannot be empty." }
        require(rows.all { it.size == columnCount }) { "Matrix rows must have equal length." }
    }

    operator fun get(row: Int, column: Int): ExactRational = rows[row][column]

    fun render(decimal: Boolean = false): String = rows.joinToString(prefix = "[", postfix = "]", separator = "\n ") { row ->
        row.joinToString(prefix = "[", postfix = "]") { value ->
            if (decimal) formatMatrixDecimal(value.toDouble()) else value.toString()
        }
    }

    companion object {
        fun parse(cells: List<List<String>>): ExactMatrix = ExactMatrix(cells.map { row ->
            row.map { cell -> ExactRational.parse(cell.ifBlank { "0" }) }
        })
    }
}

data class MatrixReduction(
    val matrix: ExactMatrix,
    val pivotColumns: List<Int>,
    val steps: List<String>,
) {
    val rank: Int get() = pivotColumns.size
}

sealed interface LinearSystemSolution {
    data class Unique(val values: List<ExactRational>) : LinearSystemSolution
    data class Infinite(val freeVariables: List<Int>, val reducedAugmented: ExactMatrix) : LinearSystemSolution
    data object Inconsistent : LinearSystemSolution
}

data class MatrixEigenvalues(val exact: List<String>, val decimal: List<Double>?)

object MatrixLabEngine {
    fun add(left: ExactMatrix, right: ExactMatrix): ExactMatrix = elementWise(left, right) { a, b -> a + b }

    fun subtract(left: ExactMatrix, right: ExactMatrix): ExactMatrix = elementWise(left, right) { a, b -> a - b }

    fun multiply(left: ExactMatrix, right: ExactMatrix): ExactMatrix {
        require(left.columnCount == right.rowCount) { "Inner matrix dimensions must match." }
        return ExactMatrix(List(left.rowCount) { row ->
            List(right.columnCount) { column ->
                (0 until left.columnCount).fold(ExactRational.ZERO) { sum, index -> sum + left[row, index] * right[index, column] }
            }
        })
    }

    fun transpose(matrix: ExactMatrix): ExactMatrix = ExactMatrix(List(matrix.columnCount) { column ->
        List(matrix.rowCount) { row -> matrix[row, column] }
    })

    fun scale(matrix: ExactMatrix, scalar: ExactRational): ExactMatrix =
        ExactMatrix(matrix.rows.map { row -> row.map { it * scalar } })

    fun trace(matrix: ExactMatrix): ExactRational {
        require(matrix.rowCount == matrix.columnCount) { "Trace requires a square matrix." }
        return (0 until matrix.rowCount).fold(ExactRational.ZERO) { sum, index -> sum + matrix[index, index] }
    }

    fun power(matrix: ExactMatrix, exponent: Int): ExactMatrix {
        require(matrix.rowCount == matrix.columnCount) { "Powers require a square matrix." }
        require(exponent >= 0) { "Use a non-negative matrix power." }
        var result = identity(matrix.rowCount)
        var base = matrix
        var remaining = exponent
        while (remaining > 0) {
            if (remaining and 1 == 1) result = multiply(result, base)
            remaining = remaining shr 1
            if (remaining > 0) base = multiply(base, base)
        }
        return result
    }

    fun characteristicPolynomial2x2(matrix: ExactMatrix): String {
        require(matrix.rowCount == 2 && matrix.columnCount == 2) { "Characteristic polynomial currently supports 2×2 matrices." }
        val trace = trace(matrix)
        val determinant = determinant(matrix)
        val middle = when {
            trace.isZero -> ""
            trace.numerator.signum() > 0 -> " − ${trace}λ"
            else -> " + ${-trace}λ"
        }
        val constant = when {
            determinant.isZero -> ""
            determinant.numerator.signum() > 0 -> " + $determinant"
            else -> " − ${-determinant}"
        }
        return "λ²$middle$constant"
    }

    fun determinant(matrix: ExactMatrix): ExactRational {
        require(matrix.rowCount == matrix.columnCount) { "Determinant requires a square matrix." }
        val working = matrix.rows.map { it.toMutableList() }.toMutableList()
        var result = ExactRational.ONE
        for (column in 0 until matrix.columnCount) {
            val pivot = (column until matrix.rowCount).firstOrNull { !working[it][column].isZero }
                ?: return ExactRational.ZERO
            if (pivot != column) {
                val swap = working[column]; working[column] = working[pivot]; working[pivot] = swap
                result = -result
            }
            val pivotValue = working[column][column]
            result *= pivotValue
            for (row in column + 1 until matrix.rowCount) {
                if (working[row][column].isZero) continue
                val factor = working[row][column] / pivotValue
                for (entry in column until matrix.columnCount) working[row][entry] = working[row][entry] - factor * working[column][entry]
            }
        }
        return result
    }

    fun rref(matrix: ExactMatrix): MatrixReduction {
        val working = matrix.rows.map { it.toMutableList() }.toMutableList()
        val pivots = mutableListOf<Int>()
        val steps = mutableListOf<String>()
        var pivotRow = 0
        for (column in 0 until matrix.columnCount) {
            if (pivotRow >= matrix.rowCount) break
            val candidate = (pivotRow until matrix.rowCount).firstOrNull { !working[it][column].isZero } ?: continue
            if (candidate != pivotRow) {
                val swap = working[pivotRow]; working[pivotRow] = working[candidate]; working[candidate] = swap
                steps += "Swap R${pivotRow + 1} and R${candidate + 1}."
            }
            val pivotValue = working[pivotRow][column]
            if (!pivotValue.isOne) {
                for (entry in column until matrix.columnCount) working[pivotRow][entry] = working[pivotRow][entry] / pivotValue
                steps += "Scale R${pivotRow + 1} by 1/$pivotValue."
            }
            for (row in 0 until matrix.rowCount) {
                if (row == pivotRow || working[row][column].isZero) continue
                val factor = working[row][column]
                for (entry in column until matrix.columnCount) working[row][entry] = working[row][entry] - factor * working[pivotRow][entry]
                steps += "R${row + 1} ← R${row + 1} − ($factor)R${pivotRow + 1}."
            }
            pivots += column
            pivotRow++
        }
        if (steps.isEmpty()) steps += "The matrix is already in reduced row-echelon form."
        return MatrixReduction(ExactMatrix(working), pivots, steps)
    }

    fun inverse(matrix: ExactMatrix): MatrixReduction {
        require(matrix.rowCount == matrix.columnCount) { "Inverse requires a square matrix." }
        val size = matrix.rowCount
        val augmented = ExactMatrix(List(size) { row ->
            matrix.rows[row] + List(size) { column -> if (row == column) ExactRational.ONE else ExactRational.ZERO }
        })
        val reduction = rref(augmented)
        val leftIsIdentity = (0 until size).all { row -> (0 until size).all { column ->
            reduction.matrix[row, column] == if (row == column) ExactRational.ONE else ExactRational.ZERO
        } }
        require(leftIsIdentity) { "The matrix is singular and has no inverse." }
        return reduction.copy(matrix = ExactMatrix(List(size) { row -> reduction.matrix.rows[row].drop(size) }))
    }

    fun solve(coefficients: ExactMatrix, constants: List<ExactRational>): LinearSystemSolution {
        require(constants.size == coefficients.rowCount) { "The constants vector must have one entry per equation." }
        val reduction = rref(ExactMatrix(List(coefficients.rowCount) { row -> coefficients.rows[row] + constants[row] }))
        val variableCount = coefficients.columnCount
        val inconsistent = reduction.matrix.rows.any { row -> row.take(variableCount).all { it.isZero } && !row.last().isZero }
        if (inconsistent) return LinearSystemSolution.Inconsistent
        val variablePivots = reduction.pivotColumns.filter { it < variableCount }
        if (variablePivots.size < variableCount) {
            return LinearSystemSolution.Infinite((0 until variableCount).filterNot(variablePivots::contains), reduction.matrix)
        }
        return LinearSystemSolution.Unique(List(variableCount) { variable ->
            val row = variablePivots.indexOf(variable)
            reduction.matrix[row, variableCount]
        })
    }

    fun eigenvalues2x2(matrix: ExactMatrix): MatrixEigenvalues {
        require(matrix.rowCount == 2 && matrix.columnCount == 2) { "Eigenvalue exploration currently supports 2×2 matrices." }
        val trace = matrix[0, 0] + matrix[1, 1]
        val determinant = determinant(matrix)
        val discriminant = trace * trace - ExactRational.of(4) * determinant
        val discriminantDouble = discriminant.toDouble()
        if (discriminantDouble < 0.0) {
            val real = trace.toDouble() / 2.0
            val imaginary = sqrt(-discriminantDouble) / 2.0
            return MatrixEigenvalues(listOf("$trace/2 + √(${-discriminant})/2 i", "$trace/2 − √(${-discriminant})/2 i"), null)
        }
        val root = rationalSquareRoot(discriminant)
        return if (root != null) {
            val two = ExactRational.of(2)
            val values = listOf((trace + root) / two, (trace - root) / two)
            MatrixEigenvalues(values.map(ExactRational::toString), values.map(ExactRational::toDouble))
        } else {
            MatrixEigenvalues(listOf("($trace + √$discriminant)/2", "($trace − √$discriminant)/2"), listOf(
                (trace.toDouble() + sqrt(discriminantDouble)) / 2.0,
                (trace.toDouble() - sqrt(discriminantDouble)) / 2.0,
            ))
        }
    }

    private fun elementWise(left: ExactMatrix, right: ExactMatrix, operation: (ExactRational, ExactRational) -> ExactRational): ExactMatrix {
        require(left.rowCount == right.rowCount && left.columnCount == right.columnCount) { "Matrix dimensions must match." }
        return ExactMatrix(List(left.rowCount) { row -> List(left.columnCount) { column -> operation(left[row, column], right[row, column]) } })
    }

    private fun identity(size: Int) = ExactMatrix(List(size) { row ->
        List(size) { column -> if (row == column) ExactRational.ONE else ExactRational.ZERO }
    })

    private fun rationalSquareRoot(value: ExactRational): ExactRational? {
        if (value.numerator.signum() < 0) return null
        fun integerRoot(number: java.math.BigInteger): java.math.BigInteger {
            if (number == java.math.BigInteger.ZERO) return number
            var x = java.math.BigInteger.ONE.shiftLeft((number.bitLength() + 1) / 2)
            while (true) {
                val next = (x + number / x).shiftRight(1)
                if (next >= x) return x
                x = next
            }
        }
        val numeratorRoot = integerRoot(value.numerator)
        val denominatorRoot = integerRoot(value.denominator)
        return if (numeratorRoot * numeratorRoot == value.numerator && denominatorRoot * denominatorRoot == value.denominator) {
            ExactRational.of(numeratorRoot, denominatorRoot)
        } else null
    }
}

private fun formatMatrixDecimal(value: Double): String = when {
    !value.isFinite() -> "—"
    abs(value - value.toLong()) < 1e-10 -> value.toLong().toString()
    else -> "%.5f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}
