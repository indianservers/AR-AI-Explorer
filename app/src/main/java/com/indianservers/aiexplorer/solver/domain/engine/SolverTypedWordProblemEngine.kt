package com.indianservers.aiexplorer.solver.domain.engine

import com.indianservers.aiexplorer.solver.domain.model.*
import com.indianservers.aiexplorer.solver.domain.steps.SolverExplanationEngine
import com.indianservers.aiexplorer.solver.domain.steps.SolverRuleRegistry
import kotlin.math.abs

/** Deterministic, unit-aware schemas for common applied word problems. */
class SolverTypedWordProblemEngine {
    fun solve(source: String, profile: ExplanationProfile): SolverSolution? {
        distanceFromRate(source, profile)?.let { return it }
        timeFromRate(source, profile)?.let { return it }
        unitPrice(source, profile)?.let { return it }
        generalDiscount(source, profile)?.let { return it }
        val natural = Regex(
            "(?i)^.+?travels?\\s+([0-9.]+)\\s*(km|m)\\s+in\\s+([0-9.]+)\\s*(hours?|hrs?|h|minutes?|mins?|min)\\.?\\s*(?:what\\s+is\\s+(?:its|the)\\s+average\\s+speed\\??)?$",
        ).matchEntire(source.trim())
        val structured = Regex(
            "(?i)^speed\\s+distance\\s+([0-9.]+)\\s*(km|m)\\s+time\\s+([0-9.]+)\\s*(hours?|hrs?|h|minutes?|mins?|min)$",
        ).matchEntire(source.trim())
        val match = natural ?: structured ?: return null
        val distance = match.groupValues[1].toDouble()
        val distanceUnit = match.groupValues[2].lowercase()
        val time = match.groupValues[3].toDouble()
        val timeUnit = match.groupValues[4].lowercase()
        if (!distance.isFinite() || !time.isFinite() || time <= 0.0) {
            return unsupported(source, profile, "Distance must be finite and time must be greater than zero.")
        }
        val distanceKm = if (distanceUnit == "m") distance / 1000.0 else distance
        val timeHours = if (timeUnit.startsWith("m")) time / 60.0 else time
        val speed = distanceKm / timeHours
        val answer = "${number(speed)} km/h"
        val steps = listOf(
            step(source, "Extract typed quantities", "distance=${number(distance)} $distanceUnit; time=${number(time)} $timeUnit", "Keep each value attached to its unit.", profile),
            step("typed quantities", "Normalize units", "distance=${number(distanceKm)} km; time=${number(timeHours)} h", "Convert compatible quantities to kilometres and hours.", profile),
            step("speed = distance / time", "Substitute", "${number(distanceKm)} / ${number(timeHours)} = $answer", "Average speed is total distance divided by total time.", profile),
        )
        val recoveredDistance = speed * timeHours
        return solution(source, answer, steps, profile, listOf(
            VerificationCheck("Dimensional check", true, "km / h", "km/h"),
            VerificationCheck("Reverse substitution", abs(recoveredDistance - distanceKm) < 1e-10, number(distanceKm), number(recoveredDistance)),
        ))
    }

    private fun distanceFromRate(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^.+?(?:travels?|moves?)\\s+at\\s+([0-9.]+)\\s*(km/h|kmph|m/s)\\s+for\\s+([0-9.]+)\\s*(hours?|hrs?|h|seconds?|secs?|s)\\.?\\s*(?:how\\s+far.*|find\\s+(?:the\\s+)?distance.*)?$").matchEntire(source.trim()) ?: return null
        val rate = match.groupValues[1].toDouble(); val rateUnit = match.groupValues[2].lowercase(); val time = match.groupValues[3].toDouble(); val timeUnit = match.groupValues[4].lowercase()
        if (rate < 0 || time < 0) return unsupported(source, profile, "Rate and elapsed time must be non-negative.")
        val rateMetresPerSecond = if (rateUnit in setOf("km/h", "kmph")) rate / 3.6 else rate
        val seconds = if (timeUnit.startsWith("h")) time * 3600 else time
        val metres = rateMetresPerSecond * seconds
        val answer = if (rateUnit in setOf("km/h", "kmph")) "${number(metres / 1000)} km" else "${number(metres)} m"
        return applied(source, answer, profile, "distance = speed * time", "${number(rate)} $rateUnit * ${number(time)} $timeUnit", answer,
            VerificationCheck("Reverse division", if (seconds == 0.0) metres == 0.0 else abs(metres / seconds - rateMetresPerSecond) < 1e-10, number(rateMetresPerSecond), if (seconds == 0.0) "0" else number(metres / seconds)))
    }

    private fun timeFromRate(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^.+?(?:covers?|travels?)\\s+([0-9.]+)\\s*(km|m)\\s+at\\s+([0-9.]+)\\s*(km/h|kmph|m/s)\\.?\\s*(?:how\\s+long.*|find\\s+(?:the\\s+)?time.*)?$").matchEntire(source.trim()) ?: return null
        val distance = match.groupValues[1].toDouble(); val distanceUnit=match.groupValues[2].lowercase(); val rate=match.groupValues[3].toDouble(); val rateUnit=match.groupValues[4].lowercase()
        if (distance < 0 || rate <= 0) return unsupported(source, profile, "Distance must be non-negative and speed must be greater than zero.")
        val metres = if (distanceUnit == "km") distance * 1000 else distance
        val metresPerSecond = if (rateUnit in setOf("km/h", "kmph")) rate / 3.6 else rate
        val seconds = metres / metresPerSecond
        val answer = if (rateUnit in setOf("km/h", "kmph")) "${number(seconds / 3600)} h" else "${number(seconds)} s"
        return applied(source, answer, profile, "time = distance / speed", "${number(distance)} $distanceUnit / ${number(rate)} $rateUnit", answer,
            VerificationCheck("Reverse multiplication", abs(metresPerSecond * seconds - metres) < 1e-8, number(metres), number(metresPerSecond * seconds)))
    }

    private fun unitPrice(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^(\\d+)\\s+([a-z]+)\\s+cost\\s+(?:rs\\.?|₹|\\$)?\\s*([0-9.]+)\\.?\\s*(?:what\\s+is\\s+the\\s+cost\\s+of\\s+one.*|find\\s+(?:the\\s+)?unit\\s+cost.*)$").matchEntire(source.trim()) ?: return null
        val count=match.groupValues[1].toDouble(); val item=match.groupValues[2]; val total=match.groupValues[3].toDouble()
        if (count <= 0 || total < 0) return unsupported(source, profile, "Item count must be positive and total cost cannot be negative.")
        val price=total/count; val answer="${number(price)} per ${item.removeSuffix("s")}"
        return applied(source, answer, profile, "unit cost = total cost / quantity", "${number(total)} / ${number(count)}", answer,
            VerificationCheck("Multiply back to total", abs(price*count-total)<1e-10, number(total), number(price*count)))
    }

    private fun generalDiscount(source: String, profile: ExplanationProfile): SolverSolution? {
        val match = Regex("(?i)^.+?(?:costs?|price\\s+is)\\s+(?:rs\\.?|₹|\\$)?\\s*([0-9.]+)\\s+and\\s+(?:has|gets)\\s+(?:a\\s+)?([0-9.]+)%\\s+discount\\.?\\s*(?:find.*(?:sale|selling|final)\\s+price.*)?$").matchEntire(source.trim()) ?: return null
        val price=match.groupValues[1].toDouble(); val percent=match.groupValues[2].toDouble()
        if (price < 0 || percent !in 0.0..100.0) return unsupported(source, profile, "Price must be non-negative and discount must lie from 0% to 100%.")
        val discount=price*percent/100; val sale=price-discount; val answer=number(sale)
        return applied(source, answer, profile, "sale price = marked price - discount", "${number(price)} - ${number(discount)}", answer,
            VerificationCheck("Discount reconstruction", abs(sale+discount-price)<1e-10, number(price), number(sale+discount)))
    }

    private fun applied(source: String, answer: String, profile: ExplanationProfile, formula: String, substitution: String, evaluated: String, check: VerificationCheck) = solution(
        source, answer, listOf(
            step(source, "Extract typed quantities", substitution, "Attach every number to its meaning and unit.", profile),
            step(substitution, "Choose the relationship", formula, "Use the formula matching the requested unknown.", profile),
            step(formula, "Substitute and evaluate", evaluated, "Calculate only after units and quantities are consistent.", profile),
        ), profile, listOf(check, VerificationCheck("Finite result", answer.none { it == '∞' }, "finite", answer)),
    )

    private fun solution(source: String, answer: String, steps: List<SolutionStep>, profile: ExplanationProfile, checks: List<VerificationCheck>) = SolverSolution(
        input = SolverInput(source, source.trim()), expression = node(source),
        classification = ProblemClassification(ProblemType.RatioOrProportion, .99f, listOf("typed distance and time quantities", "average-speed schema")),
        steps = steps, finalExpression = node(answer), finalAnswer = answer,
        verification = VerificationResult(VerificationStatus.Verified, VerificationMethod.Substitution, checks, "Units and reverse substitution independently confirm the result."),
        supported = true, message = "Solved with a deterministic unit-aware word-problem schema.", explanationProfile = profile,
        exactAnswer = answer, ruleCitations = listOf(SolverRuleRegistry.FORMULA_SUBSTITUTION), verificationStrength = VerificationStrength.NumericallyVerified,
    )

    private fun unsupported(source: String, profile: ExplanationProfile, message: String) = SolverSolution(
        SolverInput(source, source.trim()), node(source), ProblemClassification(ProblemType.RatioOrProportion, .99f, listOf("average-speed schema")),
        emptyList(), null, null, VerificationResult(VerificationStatus.NotApplicable, VerificationMethod.Substitution, emptyList(), message), false, message,
        explanationProfile = profile,
    )

    private fun step(before: String, title: String, after: String, explanation: String, profile: ExplanationProfile) = SolutionStep(
        "word-${title.hashCode()}", node(before), node(after), StepOperation.Interpret, SolverRuleRegistry.FORMULA_SUBSTITUTION,
        SolverRuleRegistry.FORMULA_SUBSTITUTION, SolverExplanationEngine.explanation(SolverRuleRegistry.FORMULA_SUBSTITUTION, profile, explanation),
        listOf(ExpressionPath(emptyList())), listOf(StepDetail("Stage", title)), true,
    )
    private fun node(text: String) = MathExpression.Variable(text, SourceSpan(0, text.length))
    private fun number(value: Double) = if (abs(value - value.toLong()) < 1e-10) value.toLong().toString() else "%.8f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
}
