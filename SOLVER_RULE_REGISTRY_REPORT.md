# Solver Rule Registry Report

## Structure

`MathRuleKnowledgeBase` extends the Phase 1 rule registry without replacing step rule IDs. Every knowledge entry contains:

- Stable ID and name
- Category
- Formal statement
- Conditions
- Four explanation profiles
- At least one example
- Counterexample where a commonly misapplied identity needs one

`SolverExplanationEngine` selects language by explanation profile while preserving the mathematical transformation and rule ID.

## Registered Phase 2 knowledge

The knowledge base currently documents rules used by the implemented engines:

- Operation order and exact fraction reduction
- Distributive law and combining like terms
- Equality division conditions
- Negative inequality reversal
- Zero-product property
- Quadratic formula
- Completing the square
- Factor theorem
- Remainder theorem
- Radical squaring and extraneous-root warning
- Logarithm domain
- Function composition
- Pythagorean identity
- Sine rule
- Cosine rule
- Geometric-series sum and convergence
- Coordinate distance formula
- Matrix product
- Matrix inverse conditions
- Euclidean algorithm
- Modular equivalence

Phase 1 registered rules remain available, including associative, commutative, sign, exponent, LCD, ratio, equality, elimination, and verification rules.

## Citation behavior

Rule names are not selected by keyword alone. A strategy assigns a rule ID to the exact generated step. The UI then resolves that ID and renders the profile-specific explanation and final citation list.

## Testing

The rule suite performs more than 150 deterministic checks across:

- Registry linkage
- Formal statements
- Conditions
- Examples
- All four explanation profiles
- Rejection of prohibited generic filler explanations
