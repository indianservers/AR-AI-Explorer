package com.indianservers.aiexplorer.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixLabEngineTest {
    private fun matrix(vararg rows: List<String>) = ExactMatrix.parse(rows.toList())

    @Test
    fun performsExactArithmeticAndMultiplication() {
        val a = matrix(listOf("1/2", "2"), listOf("-1", "3"))
        val b = matrix(listOf("2", "0"), listOf("1", "1/3"))

        assertEquals("[[5/2, 2]\n [0, 10/3]]", MatrixLabEngine.add(a, b).render())
        assertEquals("[[3, 2/3]\n [1, 1]]", MatrixLabEngine.multiply(a, b).render())
    }

    @Test
    fun computesDeterminantInverseAndRank() {
        val a = matrix(listOf("2", "1"), listOf("5", "3"))

        assertEquals(ExactRational.ONE, MatrixLabEngine.determinant(a))
        assertEquals("[[3, -1]\n [-5, 2]]", MatrixLabEngine.inverse(a).matrix.render())
        assertEquals(2, MatrixLabEngine.rref(a).rank)
        assertTrue(MatrixLabEngine.rref(a).steps.isNotEmpty())
    }

    @Test
    fun classifiesLinearSystems() {
        val unique = MatrixLabEngine.solve(
            matrix(listOf("1", "1"), listOf("1", "-1")),
            listOf(ExactRational.of(4), ExactRational.of(2)),
        ) as LinearSystemSolution.Unique
        assertEquals(listOf(ExactRational.of(3), ExactRational.ONE), unique.values)

        val infinite = MatrixLabEngine.solve(
            matrix(listOf("1", "2"), listOf("2", "4")),
            listOf(ExactRational.of(3), ExactRational.of(6)),
        )
        assertTrue(infinite is LinearSystemSolution.Infinite)

        val inconsistent = MatrixLabEngine.solve(
            matrix(listOf("1", "2"), listOf("2", "4")),
            listOf(ExactRational.of(3), ExactRational.of(7)),
        )
        assertEquals(LinearSystemSolution.Inconsistent, inconsistent)
    }

    @Test
    fun computesExactAndRadicalEigenvalues() {
        val diagonal = MatrixLabEngine.eigenvalues2x2(matrix(listOf("2", "0"), listOf("0", "5")))
        assertEquals(listOf("5", "2"), diagonal.exact)

        val rotation = MatrixLabEngine.eigenvalues2x2(matrix(listOf("0", "-1"), listOf("1", "0")))
        assertTrue(rotation.exact.all { "i" in it })
    }
}
