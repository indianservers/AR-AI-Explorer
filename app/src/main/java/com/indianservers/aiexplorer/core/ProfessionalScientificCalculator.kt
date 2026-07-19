package com.indianservers.aiexplorer.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

enum class ProfessionalCalculatorMode { Matrix, Vector, Statistics, Probability, Units, Programmer, Finance }
enum class CalculatorHandoffTarget { Graph, Solver, Notebook, Spreadsheet, ProbabilityLab }
data class ProfessionalCalculatorResult(val primary: String, val details: List<Pair<String, String>>, val steps: List<String>, val verification: String, val warning: String? = null)
data class CalculatorHandoff(val target: CalculatorHandoffTarget, val content: String, val label: String)
data class RecognitionAdapterResult(val normalized: String, val confidence: Double, val warnings: List<String>)
data class MeasurementValue(val value: Double, val uncertainty: Double, val unit: String = "")
data class CalculatorLayoutPreferences(val oneHanded: Boolean = false, val haptics: Boolean = true, val favouriteKeys: List<String> = emptyList())

object CalculatorRecognitionAdapters {
    fun voice(transcript: String, confidence: Double = 1.0): RecognitionAdapterResult {
        val normalized = CalculatorInputIntelligence.interpret(transcript)
            .replace(Regex("(?i)\\bopen bracket\\b"), "(").replace(Regex("(?i)\\bclose bracket\\b"), ")")
            .replace(Regex("(?i)\\bto the power of\\b"), "^").replace(Regex("(?i)\\bpoint\\b"), ".")
            .replace(Regex("\\s*([+\\-*/^])\\s*"), "$1")
        return RecognitionAdapterResult(normalized, confidence.coerceIn(0.0, 1.0), if (confidence < .75) listOf("Low-confidence voice input: confirm the expression before calculating.") else emptyList())
    }
    fun ocr(text: String, confidence: Double = 1.0): RecognitionAdapterResult {
        val normalized = text.replace('×', '*').replace('÷', '/').replace('−', '-').replace("π", "pi").replace("√", "sqrt")
            .replace(Regex("(?<=\\d)[Oo](?=\\d)"), "0").replace(Regex("(?<=\\d)[lI](?=\\d)"), "1").trim()
        val warnings = buildList { if (confidence < .8) add("OCR confidence is low; inspect ambiguous 0/O and 1/I characters."); if (normalized.count { it == '(' } != normalized.count { it == ')' }) add("OCR produced unmatched brackets.") }
        return RecognitionAdapterResult(normalized, confidence.coerceIn(0.0, 1.0), warnings)
    }
}

class CalculatorFavourites(initial: Collection<String> = emptyList()) {
    private val values = linkedSetOf<String>().apply { addAll(initial) }
    fun toggle(key: String): Set<String> { if (!values.add(key)) values.remove(key); return values.toSet() }
    fun all(): List<String> = values.toList()
}

class ProfessionalScientificCalculator(private val solver: MathProblemSolver = MathProblemSolver()) {
    val examples = mapOf(
        ProfessionalCalculatorMode.Matrix to listOf("determinant [[1,2],[3,4]]", "inverse [[1,2],[3,5]]", "rref [[1,2,3],[2,4,7]]"),
        ProfessionalCalculatorMode.Vector to listOf("vector dot <1,2,3> ; <4,5,6>", "vector cross <1,0,0> ; <0,1,0>", "vector angle <1,0> ; <1,1>"),
        ProfessionalCalculatorMode.Statistics to listOf("stats 2,3,3,5,7,10", "confidence 1,2,3,4,5"),
        ProfessionalCalculatorMode.Probability to listOf("normal cdf 1.96 mean 0 sd 1", "binomial pmf 3 n 10 p .5", "poisson pmf 4 rate 3"),
        ProfessionalCalculatorMode.Units to listOf("convert 72 km/h to m/s", "dimension 3 m + 5 s", "uncertainty 5 +/- .1 * 2 +/- .2", "sigfig 1234.567 4"),
        ProfessionalCalculatorMode.Programmer to listOf("programmer 255", "bitwise 12 and 10", "shift 5 left 3"),
        ProfessionalCalculatorMode.Finance to listOf("emi principal 500000 rate 8 years 20", "future value 10000 rate 7 years 10", "present value 20000 rate 6 years 5"),
    )

    fun evaluate(source: String): ProfessionalCalculatorResult? = matrix(source) ?: vector(source) ?: statistics(source) ?: probability(source) ?: units(source) ?: uncertainty(source) ?: programmer(source) ?: finance(source) ?: constants(source)

    fun handoffs(source: String, result: ProfessionalCalculatorResult): List<CalculatorHandoff> = listOf(
        CalculatorHandoff(CalculatorHandoffTarget.Graph, source, "Graph input"), CalculatorHandoff(CalculatorHandoffTarget.Solver, source, "Explain steps"),
        CalculatorHandoff(CalculatorHandoffTarget.Notebook, "$source = ${result.primary}", "Save derivation"), CalculatorHandoff(CalculatorHandoffTarget.Spreadsheet, result.details.joinToString("\n") { "${it.first},${it.second}" }, "Export table"),
    )

    private fun matrix(source: String): ProfessionalCalculatorResult? {
        if (!Regex("(?i)^(det|determinant|inverse|invert|rref|transpose)\\b").containsMatchIn(source)) return null
        val solved = solver.solve(source)
        require(solved.supported) { solved.verification }
        return result(solved.answer, listOf("Operation" to solved.kind.label), solved.steps.map { "${it.title}: ${it.explanation}" }, solved.verification)
    }

    private fun vector(source: String): ProfessionalCalculatorResult? {
        val match = Regex("(?i)^vector\\s+(dot|cross|angle|projection)\\s+<([^>]+)>\\s*;\\s*<([^>]+)>$").matchEntire(source) ?: return null
        val operation = match.groupValues[1].lowercase(); val a = parseVector(match.groupValues[2]); val b = parseVector(match.groupValues[3]); require(a.size == b.size)
        val dot = a.indices.sumOf { a[it] * b[it] }
        return when (operation) {
            "dot" -> result(number(dot), listOf("Dimensions" to a.size.toString()), listOf("Multiply corresponding components.", "Add the products."), "Direct component sum recomputes to ${number(dot)}.")
            "cross" -> { require(a.size == 3); val c = listOf(a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]); result(c.displayVector(), listOf("a dot result" to number(a.indices.sumOf { a[it] * c[it] }), "b dot result" to number(b.indices.sumOf { b[it] * c[it] })), listOf("Evaluate the 3x3 determinant pattern.", "Construct the perpendicular vector."), "The cross product has zero dot product with both inputs.") }
            "angle" -> { val magnitudeA = sqrt(a.sumOf { it * it }); val magnitudeB = sqrt(b.sumOf { it * it }); require(magnitudeA > 0 && magnitudeB > 0); val angle = acos((dot / (magnitudeA * magnitudeB)).coerceIn(-1.0, 1.0)); result("${number(angle)} rad", listOf("Degrees" to number(angle * 180 / PI)), listOf("Compute a dot b.", "Divide by |a||b|.", "Apply arccos."), "cos(theta)=${number(dot / (magnitudeA * magnitudeB))}.") }
            else -> { val denominator = b.sumOf { it * it }; require(denominator > 0); val scale = dot / denominator; val projection = b.map { it * scale }; result(projection.displayVector(), listOf("Scale" to number(scale)), listOf("Compute (a dot b)/(b dot b).", "Multiply vector b by the scale."), "The residual is perpendicular to b.") }
        }
    }

    private fun statistics(source: String): ProfessionalCalculatorResult? {
        Regex("(?i)^stats\\s+(.+)$").matchEntire(source)?.let { match ->
            val values = parseData(match.groupValues[1]); val s = AdvancedStatisticsEngine.summarize(values)
            return result(number(s.mean), listOf("Count" to s.count.toString(), "Mean" to number(s.mean), "Median" to number(s.median), "Mode" to s.modes.joinToString().ifBlank { "none" }, "Sample SD" to number(s.sampleStandardDeviation), "Q1 / Q3" to "${number(s.fiveNumber.firstQuartile)} / ${number(s.fiveNumber.thirdQuartile)}", "Outliers" to s.outliers.joinToString()), listOf("Sort and validate finite observations.", "Compute centre, spread, quartiles and shape diagnostics."), "Count=${s.count}; deviations and quartiles use documented deterministic definitions.")
        }
        Regex("(?i)^confidence\\s+(.+)$").matchEntire(source)?.let { match ->
            val values = parseData(match.groupValues[1]); val interval = InferentialStatistics.meanConfidenceInterval(values)
            return result("[${number(interval.lower)}, ${number(interval.upper)}]", listOf("Estimate" to number(interval.estimate), "Confidence" to "95%"), listOf("Estimate the sample mean and standard error.", "Use the Student-t critical value.", "Construct estimate +/- margin."), "Interval assumptions require independent, representative observations.")
        }
        return null
    }

    private fun probability(source: String): ProfessionalCalculatorResult? {
        Regex("(?i)^normal\\s+cdf\\s+(-?\\d+(?:\\.\\d+)?)\\s+mean\\s+(-?\\d+(?:\\.\\d+)?)\\s+sd\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { m -> val x=m.groupValues[1].toDouble(); val d=NormalDistribution(m.groupValues[2].toDouble(),m.groupValues[3].toDouble()); val p=d.cumulative(x); return result(number(p), listOf("Mean" to number(d.mean), "SD" to number(d.standardDeviation)), listOf("Standardize x to z.", "Evaluate the normal cumulative distribution."), "Probability is constrained to [0,1].") }
        Regex("(?i)^binomial\\s+pmf\\s+(\\d+)\\s+n\\s+(\\d+)\\s+p\\s+(\\d*(?:\\.\\d+)?)$").matchEntire(source)?.let { m -> val k=m.groupValues[1].toInt(); val d=BinomialDistribution(m.groupValues[2].toInt(),m.groupValues[3].toDouble()); val p=d.density(k.toDouble()); return result(number(p), listOf("Mean" to number(d.summary.mean), "Variance" to number(d.summary.variance)), listOf("Use C(n,k)p^k(1-p)^(n-k)."), "PMF terms sum to one within floating-point tolerance.") }
        Regex("(?i)^poisson\\s+pmf\\s+(\\d+)\\s+rate\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { m -> val k=m.groupValues[1].toInt(); val d=PoissonDistribution(m.groupValues[2].toDouble()); val p=d.density(k.toDouble()); return result(number(p), listOf("Mean" to number(d.summary.mean)), listOf("Use exp(-lambda)lambda^k/k!."), "PMF is non-negative and normalized.") }
        return null
    }

    private fun units(source: String): ProfessionalCalculatorResult? {
        Regex("(?i)^convert\\s+(-?\\d+(?:\\.\\d+)?)\\s*([a-z°/]+)\\s+to\\s+([a-z°/]+)$").matchEntire(source)?.let { m ->
            val value=m.groupValues[1].toDouble(); val from=units[m.groupValues[2].lowercase()] ?: error("Unknown source unit"); val to=units[m.groupValues[3].lowercase()] ?: error("Unknown target unit"); require(from.dimension == to.dimension) { "${from.symbol} and ${to.symbol} have incompatible dimensions." }; val converted=to.fromBase(from.toBase(value)); return result("${number(converted)} ${to.symbol}", listOf("Dimension" to from.dimension), listOf("Convert ${from.symbol} to the dimension base unit.", "Convert the base value to ${to.symbol}."), "Reverse conversion returns ${number(value)} ${from.symbol}.")
        }
        Regex("(?i)^dimension\\s+(-?\\d+(?:\\.\\d+)?)\\s*([a-z°/]+)\\s*([+-])\\s*(-?\\d+(?:\\.\\d+)?)\\s*([a-z°/]+)$").matchEntire(source)?.let { m ->
            val a=units[m.groupValues[2].lowercase()] ?: error("Unknown first unit"); val b=units[m.groupValues[5].lowercase()] ?: error("Unknown second unit"); require(a.dimension == b.dimension) { "Unit mismatch: ${a.dimension} cannot be ${if(m.groupValues[3]=="+") "added to" else "subtracted from"} ${b.dimension}." }; val first=a.toBase(m.groupValues[1].toDouble()); val second=b.toBase(m.groupValues[4].toDouble()); val answer=if(m.groupValues[3]=="+") first+second else first-second; return result("${number(a.fromBase(answer))} ${a.symbol}", listOf("Dimension" to a.dimension), listOf("Confirm dimensional compatibility.", "Convert to a shared base unit.", "Apply the requested operation."), "Both operands share dimension ${a.dimension}.")
        }
        Regex("(?i)^sigfig\\s+(-?\\d+(?:\\.\\d+)?)\\s+(\\d+)$").matchEntire(source)?.let { m -> val value=m.groupValues[1].toDouble(); val digits=m.groupValues[2].toInt().coerceIn(1,15); val formatted=String.format("%.${digits-1}e",value); return result(formatted, listOf("Significant figures" to digits.toString()), listOf("Locate the first non-zero digit.", "Round at the requested significant position."), "Scientific notation explicitly preserves $digits significant figures.") }
        return null
    }

    private fun uncertainty(source: String): ProfessionalCalculatorResult? {
        val numberPattern = "-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)"
        val m=Regex("(?i)^uncertainty\\s+($numberPattern)\\s*(?:\\+/-|±)\\s*(${numberPattern.removePrefix("-?")})\\s*([*/])\\s*($numberPattern)\\s*(?:\\+/-|±)\\s*(${numberPattern.removePrefix("-?")})$").matchEntire(source) ?: return null
        val a=MeasurementValue(m.groupValues[1].toDouble(),m.groupValues[2].toDouble()); val b=MeasurementValue(m.groupValues[4].toDouble(),m.groupValues[5].toDouble()); require(a.value != 0.0 && b.value != 0.0); val value=if(m.groupValues[3]=="*") a.value*b.value else a.value/b.value; val relative=sqrt((a.uncertainty/a.value).pow(2)+(b.uncertainty/b.value).pow(2)); val uncertainty=abs(value)*relative
        return result("${number(value)} +/- ${number(uncertainty)}", listOf("Relative uncertainty" to number(relative)), listOf("Calculate the product or quotient.", "Combine independent relative uncertainties in quadrature.", "Convert relative uncertainty back to absolute uncertainty."), "Reported uncertainty is non-negative and uses first-order propagation.")
    }

    private fun programmer(source: String): ProfessionalCalculatorResult? {
        Regex("(?i)^programmer\\s+(-?\\d+)$").matchEntire(source)?.let { m -> val value=m.groupValues[1].toLong(); return result(value.toString(), listOf("Binary" to java.lang.Long.toBinaryString(value), "Octal" to java.lang.Long.toOctalString(value), "Hex" to java.lang.Long.toHexString(value).uppercase()), listOf("Interpret a signed 64-bit integer.", "Convert its bit pattern to each radix."), "Converting each representation back yields $value.") }
        Regex("(?i)^bitwise\\s+(-?\\d+)\\s+(and|or|xor)\\s+(-?\\d+)$").matchEntire(source)?.let { m -> val a=m.groupValues[1].toLong(); val b=m.groupValues[3].toLong(); val value=when(m.groupValues[2].lowercase()){"and"->a and b;"or"->a or b;else->a xor b}; return result(value.toString(), listOf("Binary" to java.lang.Long.toBinaryString(value)), listOf("Align the 64-bit operands.", "Apply ${m.groupValues[2].uppercase()} to each bit."), "Decimal and binary displays encode the same signed integer.") }
        Regex("(?i)^shift\\s+(-?\\d+)\\s+(left|right)\\s+(\\d+)$").matchEntire(source)?.let { m -> val a=m.groupValues[1].toLong(); val places=m.groupValues[3].toInt().coerceIn(0,63); val value=if(m.groupValues[2].equals("left",true)) a shl places else a shr places; return result(value.toString(), listOf("Hex" to java.lang.Long.toHexString(value).uppercase()), listOf("Shift the signed 64-bit pattern $places positions."), "The result uses Kotlin/JVM signed shift semantics.") }
        return null
    }

    private fun finance(source: String): ProfessionalCalculatorResult? {
        Regex("(?i)^emi\\s+principal\\s+(\\d+(?:\\.\\d+)?)\\s+rate\\s+(\\d+(?:\\.\\d+)?)\\s+years\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { m -> val principal=m.groupValues[1].toDouble(); val monthly=m.groupValues[2].toDouble()/1200; val months=round(m.groupValues[3].toDouble()*12).toInt(); val payment=if(monthly==0.0) principal/months else principal*monthly*(1+monthly).pow(months)/( (1+monthly).pow(months)-1 ); return result(number(payment), listOf("Monthly payment" to number(payment), "Total paid" to number(payment*months), "Interest" to number(payment*months-principal)), listOf("Convert annual percentage rate to monthly decimal rate.", "Convert term to monthly payments.", "Apply the amortizing-loan payment formula."), "The discounted payment stream equals the principal within rounding tolerance.") }
        Regex("(?i)^(future|present)\\s+value\\s+(\\d+(?:\\.\\d+)?)\\s+rate\\s+(\\d+(?:\\.\\d+)?)\\s+years\\s+(\\d+(?:\\.\\d+)?)$").matchEntire(source)?.let { m -> val future=m.groupValues[1].equals("future",true); val amount=m.groupValues[2].toDouble(); val factor=(1+m.groupValues[3].toDouble()/100).pow(m.groupValues[4].toDouble()); val value=if(future) amount*factor else amount/factor; return result(number(value), listOf("Growth factor" to number(factor)), listOf("Convert percentage rate to decimal.", if(future) "Compound the present value." else "Discount the future value."), "Reversing the present/future operation recovers ${number(amount)}.") }
        return null
    }

    private fun constants(source: String): ProfessionalCalculatorResult? {
        val match=Regex("(?i)^constant\\s+(.+)$").matchEntire(source) ?: return null; val query=match.groupValues[1].trim(); val found=ScientificCalculator().constants.firstOrNull { it.key.contains(query,true)||it.label.contains(query,true)||it.note.contains(query,true) } ?: return result("Not found", emptyList(), listOf("Search constant names, symbols and descriptions."), "No approximate substitute was guessed.", "Try pi, gravity, light, Planck or Boltzmann.")
        return result(found.value.toString(), listOf("Symbol" to found.label, "Unit" to found.unit, "Meaning" to found.note), listOf("Find the named constant in the offline catalogue."), "The stored value and SI unit are displayed together.")
    }

    private data class UnitDef(val symbol:String,val dimension:String,val scale:Double,val offset:Double=0.0){fun toBase(v:Double)=(v+offset)*scale;fun fromBase(v:Double)=v/scale-offset}
    private val units= listOf(UnitDef("m","length",1.0),UnitDef("cm","length",.01),UnitDef("km","length",1000.0),UnitDef("kg","mass",1.0),UnitDef("g","mass",.001),UnitDef("s","time",1.0),UnitDef("min","time",60.0),UnitDef("h","time",3600.0),UnitDef("m/s","speed",1.0),UnitDef("km/h","speed",1/3.6),UnitDef("rad","angle",1.0),UnitDef("°","angle",PI/180),UnitDef("k","temperature",1.0),UnitDef("c","temperature",1.0,273.15)).associateBy{it.symbol.lowercase()}
    private fun parseVector(source:String)=source.split(',').map{it.trim().toDouble()}.also{require(it.isNotEmpty()&&it.all(Double::isFinite))}
    private fun parseData(source:String)=source.split(Regex("[,;\\s]+")).filter(String::isNotBlank).map(String::toDouble).also{require(it.isNotEmpty())}
    private fun List<Double>.displayVector()=joinToString(prefix="<",postfix=">"){number(it)}
    private fun result(primary:String,details:List<Pair<String,String>>,steps:List<String>,verification:String,warning:String?=null)=ProfessionalCalculatorResult(primary,details,steps,verification,warning)
    private fun number(value:Double)=if(abs(value-round(value))<1e-10)round(value).toLong().toString()else"%.10f".format(value).trimEnd('0').trimEnd('.')
}
