package com.indianservers.aiexplorer.mathworkspace.numbertheory

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class NumberTheoryViewModel : ViewModel() {
    val mode = mutableStateOf(NumberTheoryMode.Prime)
    val sheet = mutableStateOf(WorkspaceSheetStop.Peek)
    val input = mutableStateOf("97")
    val from = mutableStateOf("2")
    val to = mutableStateOf("200")
    val a = mutableStateOf("252"); val b = mutableStateOf("105"); val c = mutableStateOf("30"); val modulus = mutableStateOf("12")
    val exponent = mutableStateOf("100")
    val modularOperation = mutableStateOf("Power")
    val crt1 = mutableStateOf("2"); val crtM1 = mutableStateOf("3"); val crt2 = mutableStateOf("3"); val crtM2 = mutableStateOf("5")
    val primality = mutableStateOf<PrimalityResult?>(null)
    val factorization = mutableStateOf<Factorization?>(null)
    val primes = mutableStateOf<List<BigInteger>>(emptyList())
    val error = mutableStateOf(""); val loading = mutableStateOf(false)
    val step = mutableIntStateOf(0); val visual = mutableStateOf("Number line")
    private var task: Job? = null
    private fun big(text: String) = text.trim().toBigInteger()
    private fun launch(block: suspend () -> Unit) {
        task?.cancel(); error.value = ""; loading.value = true
        task = viewModelScope.launch { try { block() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { error.value = e.message ?: "Check the entered values" } finally { loading.value = false } }
    }
    fun testPrime() = launch { val n = big(input.value); primality.value = withContext(Dispatchers.Default) { PrimeEngine.test(n) }; if (!primality.value!!.isPrime) factorization.value = FactorizationEngine.factor(n) }
    fun factor() = launch { factorization.value = FactorizationEngine.factor(big(input.value)) }
    fun generatePrimes() = launch { primes.value = PrimeEngine.generateRangeCancellable(big(from.value), big(to.value)) }
    fun cancel() { task?.cancel(); loading.value = false }
    fun compute() { error.value = ""; runCatching { when(mode.value) {
        NumberTheoryMode.Prime -> testPrime()
        NumberTheoryMode.Factors -> factor()
        NumberTheoryMode.Euclidean -> { val result = EuclideanAlgorithmEngine.solve(big(a.value),big(b.value)); step.intValue=0; result }
        NumberTheoryMode.Modular -> ModularArithmeticEngine.power(big(a.value),big(exponent.value),big(modulus.value))
        NumberTheoryMode.Congruences -> CongruenceEngine.solveLinear(big(a.value),big(b.value),big(modulus.value))
        NumberTheoryMode.Diophantine -> DiophantineEngine.solve(big(a.value),big(b.value),big(c.value))
    } }.onFailure { error.value = it.message ?: "Check the entered values" } }
    fun euclidean() = EuclideanAlgorithmEngine.solve(big(a.value), big(b.value))
    fun modularResult() = when (modularOperation.value) {
        "Add" -> ModularArithmeticEngine.add(big(a.value), big(b.value), big(modulus.value))
        "Subtract" -> ModularArithmeticEngine.subtract(big(a.value), big(b.value), big(modulus.value))
        "Multiply" -> ModularArithmeticEngine.multiply(big(a.value), big(b.value), big(modulus.value))
        else -> ModularArithmeticEngine.power(big(a.value), big(exponent.value), big(modulus.value))
    }
    fun congruence() = CongruenceEngine.solveLinear(big(a.value), big(b.value), big(modulus.value))
    fun crt() = CongruenceEngine.chineseRemainder(listOf(big(crt1.value) to big(crtM1.value), big(crt2.value) to big(crtM2.value)))
    fun diophantine() = DiophantineEngine.solve(big(a.value), big(b.value), big(c.value))
    override fun onCleared() { cancel(); super.onCleared() }
}
