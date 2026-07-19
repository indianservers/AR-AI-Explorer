package com.indianservers.aiexplorer.learning.ncert

import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.production.*

object MathematicsNcertBatch02Content {
    private const val cid="cbse-2026-c10-mathematics-c1"; private const val tid="$cid-core"
    private fun q(id:String,type:CurriculumQuestionType,competency:CompetencyType,prompt:String,answer:String,explanation:String,section:String)=CurriculumQuestion("math-real-$id",SchoolSubject.MATHEMATICS,SchoolClassLevel.CLASS_10,cid,listOf(tid),competency,QuestionDifficulty.STANDARD,type,prompt,answer=AnswerDefinition(answer),explanation=explanation,commonErrorTags=listOf("number reasoning"),remediationSectionId=section)
    val realNumbers=SubjectOwnedCurriculumChapter(
        "math-ncert-c10-real-numbers-production",cid,SchoolSubject.MATHEMATICS,SchoolClassLevel.CLASS_10,"Real Numbers","Number Systems",105,"mathematics/ncert/10/real-numbers",listOf(tid),
        listOf("Use prime factorisation to reason about HCF and LCM.","Explain terminating and recurring decimal behaviour from denominator factors.","Construct contradiction arguments for irrationality.","Choose exact rather than rounded representations."),
        listOf("Prime numbers and factor trees","Fractions in lowest terms","Squares and square roots","Meaning of proof by contradiction"),
        linkedMapOf(
            "prime-factorisation" to "Every composite integer can be resolved into prime factors. Apart from the order of factors, this prime factorisation is unique; this invariant supports systematic HCF and LCM reasoning.",
            "hcf-lcm" to "For two positive integers, HCF uses the minimum exponent of each shared prime and LCM uses the maximum exponent across both numbers. Their product equals the product of the two integers.",
            "decimals" to "After reducing p/q to lowest terms, its decimal terminates exactly when q has no prime factor other than 2 or 5. Other denominator primes produce recurring decimals; checking before reduction gives unreliable conclusions.",
            "irrationality" to "A contradiction proof temporarily assumes the claimed irrational number equals p/q in lowest terms. Valid algebra then forces p and q to share a factor, contradicting lowest terms; the contradiction rejects the assumption.",
            "applications" to "Exact roots and fractions preserve information. Decimal approximations are useful for measurement, but an approximation must not be substituted into a proof as though it were exact."
        ),
        linkedMapOf("prime factorisation" to "A product of prime factors representing an integer.","HCF" to "Greatest positive integer dividing all given integers.","LCM" to "Least positive common multiple.","rational number" to "A number expressible as p/q with integers p and non-zero q.","irrational number" to "A real number not expressible as such a ratio."),
        listOf(
            WorkedExample("Find HCF and LCM of 72 and 120.",listOf("72=2³×3² and 120=2³×3×5.","HCF=2³×3=24.","LCM=2³×3²×5=360."),"HCF 24; LCM 360.","24×360=72×120=8640."),
            WorkedExample("Will 77/600 terminate?",listOf("The fraction is already in lowest terms.","600=2³×3×5².","The denominator contains prime factor 3."),"No; its decimal recurs.","The conclusion uses the reduced denominator.")),
        listOf(CurriculumDiagramSpecification("math-c10-real-factor-exponent-map",SchoolSubject.MATHEMATICS,SchoolClassLevel.CLASS_10,tid,"Prime-factor exponent map for HCF and LCM",DiagramType.TABLE,listOf(DiagramLabelRequirement("primes","prime columns","factor basis"),DiagramLabelRequirement("minimum","minimum exponents","HCF"),DiagramLabelRequirement("maximum","maximum exponents","LCM")),"Make minimum/maximum exponent selection visible.",listOf("Only prime columns are used.","Zero exponent is shown when a prime is absent.","Products are checked against the original integers."),DiagramInteractionMode.LABEL_HIDE_AND_QUIZ,"A table compares exponents of 2, 3 and 5 in 72 and 120; the minimum row forms 24 and the maximum row forms 360.",ScientificReviewStatus.VERIFIED)),
        emptyList(),
        listOf("Create factor cards for two integers and physically select minimum exponents for HCF and maximum exponents for LCM.","Test decimal predictions only after cancelling common factors; record one tempting wrong prediction and correct it."),emptyList(),
        linkedMapOf("A denominator containing 2 or 5 always terminates." to "After reduction, every denominator prime must be 2 or 5.","A long decimal is irrational." to "Recurring decimals can be long yet rational.","Checking examples proves an irrationality claim." to "Examples support a conjecture; a general argument proves it."),
        listOf(
            q("mcq",CurriculumQuestionType.MULTIPLE_CHOICE,CompetencyType.UNDERSTANDING,"Which reduced denominator guarantees a terminating decimal: 40, 42, 45 or 70?","40","40=2³×5; every other option contains another prime.","decimals"),
            q("numeric",CurriculumQuestionType.NUMERIC_INPUT,CompetencyType.APPLYING,"Given HCF(84,126)=42, find their LCM.","252","LCM=(84×126)/42=252.","hcf-lcm"),
            q("table",CurriculumQuestionType.TABLE_INTERPRETATION,CompetencyType.ANALYSING,"Exponent rows for A and B under primes 2,3,5 are (4,1,0) and (2,3,1). Give the HCF exponent row.","(2,1,0)","HCF takes component-wise minima.","hcf-lcm"),
            q("proof",CurriculumQuestionType.PROOF_DERIVATION,CompetencyType.ANALYSING,"Outline why √3 cannot equal p/q in lowest terms.","Squaring makes p² divisible by 3, so p is divisible by 3; substitution then makes q divisible by 3, contradicting lowest terms.","The contradiction rejects rationality.","irrationality"),
            q("case",CurriculumQuestionType.CASE_BASED,CompetencyType.EVALUATING,"A student uses 1.414 as exactly √2 in a proof. Diagnose the error.","1.414 is an approximation, not exact √2.","Rounding cannot preserve an exact equality required by proof.","applications")),
        CurriculumRevisionResource(listOf("Prime factorisation is unique up to order.","HCF uses minimum exponents; LCM uses maximum exponents.","Reduce before testing decimal termination.","Contradiction rejects an assumption by deriving impossibility."),listOf("HCF(a,b)×LCM(a,b)=a×b for positive a,b"),listOf("Exponent map separates prime basis from minimum/maximum selection."),listOf("finite-looking versus terminating","recurring versus irrational","example versus proof"),listOf("math-real-mcq","math-real-proof")),
        listOf("Class IX Number Systems","Class X Polynomials","Class XI Sets"),CurriculumCompletionRequirements(true,true,false,true,true,false,true,true,true),ScientificReviewStatus.VERIFIED)
    val chapters=listOf(realNumbers)
}
