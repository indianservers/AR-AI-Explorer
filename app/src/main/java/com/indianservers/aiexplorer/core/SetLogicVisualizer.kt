package com.indianservers.aiexplorer.core

data class BooleanFormulaLaw(
    val id: String,
    val title: String,
    val formula: String,
    val variables: List<String>,
    val leftLabel: String,
    val rightLabel: String,
    val explanation: String,
    val left: (Map<String, Boolean>) -> Boolean,
    val right: (Map<String, Boolean>) -> Boolean,
)

data class BooleanFormulaRow(
    val inputs: Map<String, Boolean>,
    val left: Boolean,
    val right: Boolean,
) {
    val equivalent: Boolean get() = left == right
}

object SetLogicCatalog {
    private fun v(values: Map<String, Boolean>, name: String) = values[name] == true

    val setLaws = listOf(
        BooleanFormulaLaw("set-union", "Union", "x ∈ A ∪ B ⇔ x ∈ A ∨ x ∈ B", listOf("A", "B"), "x ∈ A ∪ B", "A ∨ B", "Union contains elements in either set or both.", { v(it, "A") || v(it, "B") }, { v(it, "A") || v(it, "B") }),
        BooleanFormulaLaw("set-intersection", "Intersection", "x ∈ A ∩ B ⇔ x ∈ A ∧ x ∈ B", listOf("A", "B"), "x ∈ A ∩ B", "A ∧ B", "Intersection contains only elements shared by both sets.", { v(it, "A") && v(it, "B") }, { v(it, "A") && v(it, "B") }),
        BooleanFormulaLaw("set-difference", "Difference", "x ∈ A \\ B ⇔ x ∈ A ∧ x ∉ B", listOf("A", "B"), "x ∈ A \\ B", "A ∧ ¬B", "Difference keeps the part of A outside B.", { v(it, "A") && !v(it, "B") }, { v(it, "A") && !v(it, "B") }),
        BooleanFormulaLaw("set-complement", "Complement", "x ∈ Aᶜ ⇔ x ∉ A", listOf("A"), "x ∈ Aᶜ", "¬A", "The complement contains every universe element outside A.", { !v(it, "A") }, { !v(it, "A") }),
        BooleanFormulaLaw("set-symmetric-difference", "Symmetric difference", "x ∈ A Δ B ⇔ (A ∨ B) ∧ ¬(A ∧ B)", listOf("A", "B"), "x ∈ A Δ B", "A ⊕ B", "Symmetric difference keeps elements in exactly one set.", { v(it, "A") xor v(it, "B") }, { (v(it, "A") || v(it, "B")) && !(v(it, "A") && v(it, "B")) }),
        BooleanFormulaLaw("set-de-morgan-union", "De Morgan: union", "(A ∪ B)ᶜ = Aᶜ ∩ Bᶜ", listOf("A", "B"), "¬(A ∨ B)", "¬A ∧ ¬B", "Outside a union means outside both original sets.", { !(v(it, "A") || v(it, "B")) }, { !v(it, "A") && !v(it, "B") }),
        BooleanFormulaLaw("set-de-morgan-intersection", "De Morgan: intersection", "(A ∩ B)ᶜ = Aᶜ ∪ Bᶜ", listOf("A", "B"), "¬(A ∧ B)", "¬A ∨ ¬B", "Outside an intersection means outside at least one original set.", { !(v(it, "A") && v(it, "B")) }, { !v(it, "A") || !v(it, "B") }),
        BooleanFormulaLaw("set-distributive-union", "Distributive union", "A ∪ (B ∩ C) = (A ∪ B) ∩ (A ∪ C)", listOf("A", "B", "C"), "A ∨ (B ∧ C)", "(A ∨ B) ∧ (A ∨ C)", "Membership distributes exactly like Boolean OR over AND.", { v(it, "A") || (v(it, "B") && v(it, "C")) }, { (v(it, "A") || v(it, "B")) && (v(it, "A") || v(it, "C")) }),
        BooleanFormulaLaw("set-distributive-intersection", "Distributive intersection", "A ∩ (B ∪ C) = (A ∩ B) ∪ (A ∩ C)", listOf("A", "B", "C"), "A ∧ (B ∨ C)", "(A ∧ B) ∨ (A ∧ C)", "Membership distributes exactly like Boolean AND over OR.", { v(it, "A") && (v(it, "B") || v(it, "C")) }, { (v(it, "A") && v(it, "B")) || (v(it, "A") && v(it, "C")) }),
        BooleanFormulaLaw("set-absorption", "Absorption", "A ∪ (A ∩ B) = A", listOf("A", "B"), "A ∨ (A ∧ B)", "A", "Adding a subset already contained in A changes nothing.", { v(it, "A") || (v(it, "A") && v(it, "B")) }, { v(it, "A") }),
        BooleanFormulaLaw("set-identity-union", "Union identity", "A ∪ ∅ = A", listOf("A"), "A ∪ ∅", "A", "Union with the empty set adds no elements.", { v(it, "A") || false }, { v(it, "A") }),
        BooleanFormulaLaw("set-identity-intersection", "Intersection identity", "A ∩ U = A", listOf("A"), "A ∩ U", "A", "Intersecting with the universe removes no elements.", { v(it, "A") && true }, { v(it, "A") }),
        BooleanFormulaLaw("set-domination-union", "Union domination", "A ∪ U = U", listOf("A"), "A ∪ U", "U", "The universe already contains every A-element.", { v(it, "A") || true }, { true }),
        BooleanFormulaLaw("set-domination-intersection", "Intersection domination", "A ∩ ∅ = ∅", listOf("A"), "A ∩ ∅", "∅", "No element can belong to the empty set.", { v(it, "A") && false }, { false }),
        BooleanFormulaLaw("set-idempotent-union", "Idempotent union", "A ∪ A = A", listOf("A"), "A ∪ A", "A", "Repeating the same membership condition changes nothing.", { v(it, "A") || v(it, "A") }, { v(it, "A") }),
        BooleanFormulaLaw("set-idempotent-intersection", "Idempotent intersection", "A ∩ A = A", listOf("A"), "A ∩ A", "A", "Requiring the same membership condition twice changes nothing.", { v(it, "A") && v(it, "A") }, { v(it, "A") }),
        BooleanFormulaLaw("set-double-complement", "Double complement", "(Aᶜ)ᶜ = A", listOf("A"), "¬¬A", "A", "Taking the complement twice restores the original set.", { !!v(it, "A") }, { v(it, "A") }),
        BooleanFormulaLaw("set-complement-union", "Complement union", "A ∪ Aᶜ = U", listOf("A"), "A ∪ Aᶜ", "U", "Every universe element is either in A or outside A.", { v(it, "A") || !v(it, "A") }, { true }),
        BooleanFormulaLaw("set-complement-intersection", "Complement intersection", "A ∩ Aᶜ = ∅", listOf("A"), "A ∩ Aᶜ", "∅", "No element can be both inside and outside A.", { v(it, "A") && !v(it, "A") }, { false }),
        BooleanFormulaLaw("set-commutative-union", "Commutative union", "A ∪ B = B ∪ A", listOf("A", "B"), "A ∨ B", "B ∨ A", "Swapping the sets does not change union membership.", { v(it, "A") || v(it, "B") }, { v(it, "B") || v(it, "A") }),
    )

    fun shortSetFormula(law: BooleanFormulaLaw): String = when (law.id) {
        "set-union" -> "A ∪ B"
        "set-intersection" -> "A ∩ B"
        "set-difference" -> "A \\ B"
        "set-complement" -> "Aᶜ"
        "set-symmetric-difference" -> "A Δ B"
        "set-de-morgan-union" -> "(A ∪ B)ᶜ"
        "set-de-morgan-intersection" -> "(A ∩ B)ᶜ"
        "set-distributive-union" -> "A ∪ (B ∩ C)"
        "set-distributive-intersection" -> "A ∩ (B ∪ C)"
        "set-absorption" -> "A ∪ (A ∩ B)"
        else -> law.formula.substringBefore(" = ")
    }

    val logicLaws = listOf(
        BooleanFormulaLaw("logic-and", "Conjunction", "P ∧ Q", listOf("P", "Q"), "P ∧ Q", "both true", "Conjunction is true only when both propositions are true.", { v(it, "P") && v(it, "Q") }, { v(it, "P") && v(it, "Q") }),
        BooleanFormulaLaw("logic-or", "Disjunction", "P ∨ Q", listOf("P", "Q"), "P ∨ Q", "at least one true", "Inclusive OR is true when either proposition is true.", { v(it, "P") || v(it, "Q") }, { v(it, "P") || v(it, "Q") }),
        BooleanFormulaLaw("logic-not", "Negation", "¬P", listOf("P"), "¬P", "P is false", "Negation reverses a truth value.", { !v(it, "P") }, { !v(it, "P") }),
        BooleanFormulaLaw("logic-implication", "Implication", "P → Q ≡ ¬P ∨ Q", listOf("P", "Q"), "P → Q", "¬P ∨ Q", "An implication fails only for a true premise and false conclusion.", { !v(it, "P") || v(it, "Q") }, { !v(it, "P") || v(it, "Q") }),
        BooleanFormulaLaw("logic-biconditional", "Biconditional", "P ↔ Q ≡ (P → Q) ∧ (Q → P)", listOf("P", "Q"), "P ↔ Q", "same truth value", "A biconditional is true when both propositions agree.", { v(it, "P") == v(it, "Q") }, { (!v(it, "P") || v(it, "Q")) && (!v(it, "Q") || v(it, "P")) }),
        BooleanFormulaLaw("logic-xor", "Exclusive OR", "P ⊕ Q ≡ (P ∨ Q) ∧ ¬(P ∧ Q)", listOf("P", "Q"), "P ⊕ Q", "exactly one true", "Exclusive OR is true when the inputs differ.", { v(it, "P") xor v(it, "Q") }, { (v(it, "P") || v(it, "Q")) && !(v(it, "P") && v(it, "Q")) }),
        BooleanFormulaLaw("logic-de-morgan-and", "De Morgan: AND", "¬(P ∧ Q) ≡ ¬P ∨ ¬Q", listOf("P", "Q"), "¬(P ∧ Q)", "¬P ∨ ¬Q", "Negating a conjunction produces a disjunction of negations.", { !(v(it, "P") && v(it, "Q")) }, { !v(it, "P") || !v(it, "Q") }),
        BooleanFormulaLaw("logic-de-morgan-or", "De Morgan: OR", "¬(P ∨ Q) ≡ ¬P ∧ ¬Q", listOf("P", "Q"), "¬(P ∨ Q)", "¬P ∧ ¬Q", "Negating a disjunction produces a conjunction of negations.", { !(v(it, "P") || v(it, "Q")) }, { !v(it, "P") && !v(it, "Q") }),
        BooleanFormulaLaw("logic-contrapositive", "Contrapositive", "P → Q ≡ ¬Q → ¬P", listOf("P", "Q"), "P → Q", "¬Q → ¬P", "Every implication is logically equivalent to its contrapositive.", { !v(it, "P") || v(it, "Q") }, { v(it, "Q") || !v(it, "P") }),
        BooleanFormulaLaw("logic-distributive", "Distributive law", "P ∧ (Q ∨ R) ≡ (P ∧ Q) ∨ (P ∧ R)", listOf("P", "Q", "R"), "P ∧ (Q ∨ R)", "(P ∧ Q) ∨ (P ∧ R)", "AND distributes over OR for every truth assignment.", { v(it, "P") && (v(it, "Q") || v(it, "R")) }, { (v(it, "P") && v(it, "Q")) || (v(it, "P") && v(it, "R")) }),
    )
}

object SetLogicEngine {
    fun rows(law: BooleanFormulaLaw): List<BooleanFormulaRow> {
        val count = 1 shl law.variables.size
        return (0 until count).map { mask ->
            val inputs = law.variables.mapIndexed { index, name -> name to (mask and (1 shl (law.variables.lastIndex - index)) != 0) }.toMap()
            BooleanFormulaRow(inputs, law.left(inputs), law.right(inputs))
        }
    }

    fun evaluate(law: BooleanFormulaLaw, inputs: Map<String, Boolean>) =
        BooleanFormulaRow(law.variables.associateWith { inputs[it] == true }, law.left(inputs), law.right(inputs))

    fun verified(law: BooleanFormulaLaw): Boolean = rows(law).all { it.equivalent }
}

enum class SetStudioTool(val label: String) {
    Venn("Venn / Euler"), Elements("Elements"), PowerSet("Power Set"), Cartesian("Cartesian Product"),
    Relations("Relations"), Partitions("Partitions"), Order("Hasse Diagram"), Functions("Functions"),
    Proofs("Identity Proofs"), Challenge("Challenges"),
}

data class SetLearningConcept(
    val id: String,
    val title: String,
    val category: String,
    val level: Int,
    val definition: String,
    val example: String,
)

data class SetStudioFeature(val id: String, val title: String, val tool: SetStudioTool)

object SetTheoryLearningCatalog {
    val concepts = listOf(
        SetLearningConcept("notation-membership", "Set notation and membership", "Foundations", 1, "A set is a well-defined collection; x ∈ A means x belongs to A.", "2 ∈ {1,2,3}"),
        SetLearningConcept("roster-builder", "Roster and set-builder notation", "Foundations", 1, "Roster notation lists elements; set-builder notation states a membership rule.", "{2,4,6} = {x | x is even, 1≤x≤6}"),
        SetLearningConcept("empty-singleton", "Empty and singleton sets", "Foundations", 1, "The empty set has no elements; a singleton has exactly one.", "∅ and {0}"),
        SetLearningConcept("finite-infinite", "Finite and infinite sets", "Foundations", 1, "Finite sets have a natural-number cardinality; infinite sets do not end.", "{1,2,3} versus ℕ"),
        SetLearningConcept("equal-equivalent", "Equal versus equivalent sets", "Foundations", 1, "Equal sets share elements; equivalent sets share cardinality.", "{a,b} and {1,2} are equivalent"),
        SetLearningConcept("subsets", "Subsets and proper subsets", "Foundations", 1, "A ⊆ B when every A-element is in B; A ⊂ B also requires A≠B.", "{1,2} ⊂ {1,2,3}"),
        SetLearningConcept("power-sets", "Power sets", "Operations", 2, "P(A) is the set of every subset of A and has 2^|A| members.", "P({a,b})={∅,{a},{b},{a,b}}"),
        SetLearningConcept("universal-sets", "Universal sets", "Foundations", 1, "A universe U specifies all objects currently under discussion.", "U={1,2,3,4}"),
        SetLearningConcept("complements", "Set complements", "Operations", 1, "Aᶜ contains universe elements not in A.", "U={1,2,3}, A={1}: Aᶜ={2,3}"),
        SetLearningConcept("union", "Union operations", "Operations", 1, "A∪B contains elements in A or B or both.", "{1,2}∪{2,3}={1,2,3}"),
        SetLearningConcept("intersection", "Intersection operations", "Operations", 1, "A∩B contains elements common to both sets.", "{1,2}∩{2,3}={2}"),
        SetLearningConcept("difference", "Set difference", "Operations", 1, "A\\B contains A-elements not in B.", "{1,2}\\{2,3}={1}"),
        SetLearningConcept("symmetric-difference", "Symmetric difference", "Operations", 2, "AΔB contains elements in exactly one of A and B.", "{1,2}Δ{2,3}={1,3}"),
        SetLearningConcept("disjoint", "Disjoint sets", "Structure", 1, "Disjoint sets have empty intersection.", "{1,3}∩{2,4}=∅"),
        SetLearningConcept("overlapping", "Overlapping sets", "Structure", 1, "Overlapping sets share at least one element.", "{1,2} and {2,3}"),
        SetLearningConcept("pairwise-disjoint", "Pairwise-disjoint families", "Structure", 2, "Every distinct pair in the family has empty intersection.", "{ {1},{2,3},{4} }"),
        SetLearningConcept("cartesian-products", "Cartesian products", "Relations", 2, "A×B contains every ordered pair (a,b) with a∈A and b∈B.", "{1,2}×{x}={(1,x),(2,x)}"),
        SetLearningConcept("ordered-pairs", "Ordered pairs", "Relations", 1, "Order matters: (a,b)=(c,d) exactly when a=c and b=d.", "(1,2)≠(2,1)"),
        SetLearningConcept("relations", "Relations as ordered pairs", "Relations", 2, "A relation from A to B is any subset of A×B.", "R={(1,a),(2,b)}"),
        SetLearningConcept("domain-range", "Domain, codomain and range", "Functions", 2, "Domain supplies inputs, codomain allows outputs, and range records used outputs.", "f:{1,2}→{a,b,c}, range={a,b}"),
        SetLearningConcept("equivalence-relations", "Equivalence relations", "Relations", 3, "An equivalence relation is reflexive, symmetric and transitive.", "Congruence modulo 3"),
        SetLearningConcept("partial-orders", "Partial-order relations", "Relations", 3, "A partial order is reflexive, antisymmetric and transitive.", "Divisibility on positive integers"),
        SetLearningConcept("partitions", "Partitions and equivalence classes", "Structure", 3, "A partition is a disjoint nonempty family whose union is the universe.", "Parity partitions integers into even and odd"),
        SetLearningConcept("indexed-families", "Indexed set families", "Advanced", 3, "An indexed family assigns a set Aᵢ to each index i.", "Aₙ={1,...,n}"),
        SetLearningConcept("generalized-operations", "Generalized unions and intersections", "Advanced", 3, "Union or intersection can be taken over an entire indexed family.", "⋃ₙ {n}=ℕ"),
        SetLearningConcept("de-morgan", "De Morgan’s laws", "Laws", 2, "Complement exchanges union and intersection while complementing operands.", "(A∪B)ᶜ=Aᶜ∩Bᶜ"),
        SetLearningConcept("commutative", "Commutative laws", "Laws", 2, "Union and intersection are unchanged when operands swap.", "A∪B=B∪A"),
        SetLearningConcept("associative", "Associative laws", "Laws", 2, "Regrouping repeated unions or intersections changes nothing.", "A∪(B∪C)=(A∪B)∪C"),
        SetLearningConcept("distributive", "Distributive laws", "Laws", 2, "Union and intersection distribute over each other.", "A∩(B∪C)=(A∩B)∪(A∩C)"),
        SetLearningConcept("identity-domination", "Identity and domination laws", "Laws", 2, "∅ and U act as identities or dominating operands.", "A∪∅=A; A∪U=U"),
        SetLearningConcept("idempotent", "Idempotent laws", "Laws", 2, "Combining a set with itself returns itself.", "A∩A=A"),
        SetLearningConcept("absorption", "Absorption laws", "Laws", 2, "A absorbs expressions already constrained by A.", "A∪(A∩B)=A"),
        SetLearningConcept("complement-laws", "Complement laws", "Laws", 2, "A and Aᶜ partition U and double complement restores A.", "A∪Aᶜ=U"),
        SetLearningConcept("duality", "Principle of duality", "Laws", 3, "Interchanging union/intersection and U/∅ converts a valid identity into its dual.", "Dual of A∪∅=A is A∩U=A"),
        SetLearningConcept("cardinality", "Cardinality", "Counting", 1, "Cardinality |A| counts distinct elements of A.", "|{a,b,c}|=3"),
        SetLearningConcept("inclusion-exclusion", "Inclusion–exclusion", "Counting", 2, "Overlap must be subtracted after adding two set sizes.", "|A∪B|=|A|+|B|-|A∩B|"),
        SetLearningConcept("pigeonhole", "Pigeonhole principle", "Counting", 2, "More objects than boxes forces some box to hold multiple objects.", "13 people force two birth months equal"),
        SetLearningConcept("countable", "Countable sets", "Infinity", 3, "A set is countable when it injects into or bijects with ℕ.", "ℤ is countable"),
        SetLearningConcept("uncountable", "Uncountable sets", "Infinity", 3, "An uncountable set cannot be listed in a natural-number sequence.", "ℝ is uncountable"),
        SetLearningConcept("cantor-diagonal", "Cantor’s diagonal argument", "Infinity", 4, "A diagonal construction creates an object missing from any proposed complete list.", "Binary sequences are uncountable"),
        SetLearningConcept("bijections-cardinality", "Bijections and equal cardinality", "Functions", 3, "A bijection pairs every element exactly once and proves equal cardinality.", "n↦n+1 pairs ℕ with positive integers"),
        SetLearningConcept("injections", "Injective functions", "Functions", 2, "An injection never maps distinct inputs to one output.", "f(x)=2x on integers"),
        SetLearningConcept("surjections", "Surjective functions", "Functions", 2, "A surjection reaches every codomain element.", "f(x)=x² from ℝ to nonnegative reals"),
        SetLearningConcept("images-preimages", "Images and inverse images", "Functions", 3, "Images push subsets forward; inverse images pull subsets back.", "f⁻¹(B)={x|f(x)∈B}"),
        SetLearningConcept("characteristic-functions", "Characteristic functions", "Functions", 3, "χₐ(x) is 1 inside A and 0 outside.", "χₐ∩ᴮ=χₐχᴮ"),
        SetLearningConcept("indicator-algebra", "Indicator-set algebra", "Applications", 3, "Set operations translate into arithmetic identities of indicator variables.", "χₐ∪ᴮ=χₐ+χᴮ-χₐχᴮ"),
        SetLearningConcept("russell-paradox", "Russell’s paradox", "Foundations", 4, "Unrestricted set formation creates contradiction for the set of all non-self-membered sets.", "R={x|x∉x}: ask whether R∈R"),
        SetLearningConcept("axiomatic-set-theory", "Axiomatic set theory introduction", "Foundations", 4, "Axioms such as extensionality, separation and replacement regulate set formation.", "ZFC avoids unrestricted comprehension"),
        SetLearningConcept("sets-probability", "Sets in probability", "Applications", 2, "Events are subsets of a sample space and probability respects set operations.", "P(Aᶜ)=1-P(A)"),
        SetLearningConcept("sets-databases", "Sets in databases and classification", "Applications", 2, "Queries use union, intersection, difference and membership over record collections.", "Students∩Passed"),
    )

    val features = listOf(
        SetStudioFeature("drag-elements", "Drag elements between Venn regions", SetStudioTool.Elements),
        SetStudioFeature("dynamic-sets", "Add or remove sets dynamically", SetStudioTool.Elements),
        SetStudioFeature("one-four-sets", "One-, two-, three- and four-set diagrams", SetStudioTool.Venn),
        SetStudioFeature("euler-mode", "Euler-diagram mode", SetStudioTool.Venn),
        SetStudioFeature("animate-operations", "Animated union and intersection shading", SetStudioTool.Venn),
        SetStudioFeature("animate-de-morgan", "Animated De Morgan transformations", SetStudioTool.Proofs),
        SetStudioFeature("tap-region", "Tap a region to inspect its expression", SetStudioTool.Venn),
        SetStudioFeature("tap-expression", "Tap an expression to highlight its region", SetStudioTool.Proofs),
        SetStudioFeature("consistent-colours", "Consistent set colour coding", SetStudioTool.Venn),
        SetStudioFeature("custom-set-names", "Custom set names", SetStudioTool.Elements),
        SetStudioFeature("custom-universe", "Custom universe name", SetStudioTool.Elements),
        SetStudioFeature("typed-elements", "Text, number and symbol elements", SetStudioTool.Elements),
        SetStudioFeature("duplicate-validation", "Duplicate-element validation", SetStudioTool.Elements),
        SetStudioFeature("live-roster", "Live roster notation", SetStudioTool.Elements),
        SetStudioFeature("live-builder", "Live set-builder notation", SetStudioTool.Elements),
        SetStudioFeature("region-cardinality", "Cardinality beside every region", SetStudioTool.Venn),
        SetStudioFeature("subset-tree", "Subset relationship tree", SetStudioTool.PowerSet),
        SetStudioFeature("power-generator", "Interactive power-set generator", SetStudioTool.PowerSet),
        SetStudioFeature("power-animation", "Animated power-set construction", SetStudioTool.PowerSet),
        SetStudioFeature("product-grid", "Cartesian-product grid", SetStudioTool.Cartesian),
        SetStudioFeature("relation-arrows", "Relations as arrow diagrams", SetStudioTool.Relations),
        SetStudioFeature("relation-matrix", "Relation arrow/matrix switch", SetStudioTool.Relations),
        SetStudioFeature("reflexive-highlight", "Reflexive entry highlighting", SetStudioTool.Relations),
        SetStudioFeature("symmetric-highlight", "Symmetric pair highlighting", SetStudioTool.Relations),
        SetStudioFeature("transitive-paths", "Animated transitive paths", SetStudioTool.Relations),
        SetStudioFeature("equivalence-classes", "Equivalence-class grouping", SetStudioTool.Partitions),
        SetStudioFeature("partition-containers", "Coloured partition containers", SetStudioTool.Partitions),
        SetStudioFeature("hasse-builder", "Hasse-diagram builder", SetStudioTool.Order),
        SetStudioFeature("drag-order", "Drag-to-order Hasse nodes", SetStudioTool.Order),
        SetStudioFeature("function-mapping", "Function-mapping visualization", SetStudioTool.Functions),
        SetStudioFeature("map-classification", "Injection, surjection and bijection classification", SetStudioTool.Functions),
        SetStudioFeature("inclusion-counters", "Inclusion–exclusion counters", SetStudioTool.Venn),
        SetStudioFeature("truth-table", "Truth tables beside set identities", SetStudioTool.Proofs),
        SetStudioFeature("table-sync", "Synchronized Venn and truth-table rows", SetStudioTool.Proofs),
        SetStudioFeature("proof-steps", "Step-by-step identity transformations", SetStudioTool.Proofs),
        SetStudioFeature("counterexamples", "Counterexamples for false identities", SetStudioTool.Proofs),
        SetStudioFeature("predict-reveal", "Predict before revealing", SetStudioTool.Challenge),
        SetStudioFeature("guided-mode", "Guided construction mode", SetStudioTool.Challenge),
        SetStudioFeature("challenge-mode", "Hidden-target challenge mode", SetStudioTool.Challenge),
        SetStudioFeature("difficulty", "Difficulty levels", SetStudioTool.Challenge),
        SetStudioFeature("undo-redo-reset", "Undo, redo and reset", SetStudioTool.Elements),
        SetStudioFeature("save-experiments", "Saved custom experiments", SetStudioTool.Elements),
        SetStudioFeature("export-image", "Diagram image export", SetStudioTool.Venn),
        SetStudioFeature("share-structured", "Structured construction sharing", SetStudioTool.Elements),
        SetStudioFeature("formula-search", "Formula search and filters", SetStudioTool.Proofs),
        SetStudioFeature("favourites-recent", "Favourites and recent concepts", SetStudioTool.Challenge),
        SetStudioFeature("collapsible-panels", "Collapsible formula, control and explanation panels", SetStudioTool.Venn),
        SetStudioFeature("zoom-pan", "Pinch zoom and canvas panning", SetStudioTool.Venn),
        SetStudioFeature("accessible-patterns", "Spoken descriptions and high-contrast patterns", SetStudioTool.Venn),
        SetStudioFeature("mastery", "Progress, quizzes and mastery badges", SetStudioTool.Challenge),
    )
}

data class RelationAnalysis(val reflexive: Boolean, val symmetric: Boolean, val antisymmetric: Boolean, val transitive: Boolean)
data class MappingAnalysis(val isFunction: Boolean, val injective: Boolean, val surjective: Boolean, val bijective: Boolean)

object SetTheoryStudioEngine {
    fun parseElements(source: String): List<String> = source.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    fun roster(elements: Collection<String>): String = elements.distinct().joinToString(prefix = "{", postfix = "}")
    fun powerSet(elements: List<String>): List<Set<String>> {
        val unique = elements.distinct(); require(unique.size <= 12) { "Power-set visualization supports at most 12 distinct elements." }
        return (0 until (1 shl unique.size)).map { mask -> unique.filterIndexed { index, _ -> mask and (1 shl index) != 0 }.toSet() }
    }
    fun cartesianProduct(a: List<String>, b: List<String>): List<Pair<String, String>> = a.distinct().flatMap { left -> b.distinct().map { right -> left to right } }
    fun analyzeRelation(domain: Set<String>, relation: Set<Pair<String, String>>): RelationAnalysis {
        val reflexive = domain.all { it to it in relation }
        val symmetric = relation.all { (a, b) -> b to a in relation }
        val antisymmetric = relation.all { (a, b) -> a == b || b to a !in relation }
        val transitive = relation.all { (a, b) -> relation.filter { it.first == b }.all { (_, c) -> a to c in relation } }
        return RelationAnalysis(reflexive, symmetric, antisymmetric, transitive)
    }
    fun equivalenceClasses(domain: Set<String>, relation: Set<Pair<String, String>>): List<Set<String>> {
        val analysis = analyzeRelation(domain, relation); require(analysis.reflexive && analysis.symmetric && analysis.transitive) { "Relation is not an equivalence relation." }
        return domain.map { item -> domain.filter { other -> item to other in relation }.toSet() }.distinct()
    }
    fun hasseCovers(domain: Set<Int>, divides: (Int, Int) -> Boolean = { a, b -> b % a == 0 }): Set<Pair<Int, Int>> =
        domain.flatMap { a -> domain.filter { b -> a != b && divides(a, b) && domain.none { c -> c != a && c != b && divides(a, c) && divides(c, b) } }.map { b -> a to b } }.toSet()
    fun analyzeMapping(domain: Set<String>, codomain: Set<String>, mapping: Map<String, String>): MappingAnalysis {
        val isFunction = domain.all { it in mapping } && mapping.keys.all { it in domain } && mapping.values.all { it in codomain }
        val injective = isFunction && mapping.values.size == mapping.values.toSet().size
        val surjective = isFunction && mapping.values.toSet() == codomain
        return MappingAnalysis(isFunction, injective, surjective, injective && surjective)
    }
    fun inclusionExclusion(a: Set<String>, b: Set<String>): Int = a.size + b.size - a.intersect(b).size
}
