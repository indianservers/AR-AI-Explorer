package com.indianservers.aiexplorer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@DrawableRes
internal fun mathConceptIconRes(title: String): Int = when (title) {
    "Abstract and Advanced Algebra" -> R.drawable.math_concept_abstract_advanced_algebra
    "Algebra" -> R.drawable.math_concept_algebra
    "Applied and Interdisciplinary Mathematics" -> R.drawable.math_concept_applied_interdisciplinary_mathematics
    "Arithmetic" -> R.drawable.math_concept_arithmetic
    "Calculus" -> R.drawable.math_concept_calculus
    "Complex Analysis" -> R.drawable.math_concept_complex_analysis
    "Coordinate Geometry" -> R.drawable.math_concept_coordinate_geometry
    "Data Handling and Patterns" -> R.drawable.math_concept_data_handling_patterns
    "Differential Equations and PDE" -> R.drawable.math_concept_differential_equations_pde
    "Differential Geometry" -> R.drawable.math_concept_differential_geometry
    "Discrete Mathematics" -> R.drawable.math_concept_discrete_mathematics
    "Dynamical Systems and Control" -> R.drawable.math_concept_dynamical_systems_control
    "Financial and Commercial Mathematics" -> R.drawable.math_concept_financial_commercial_mathematics
    "Fractions Decimals Ratio and Percent" -> R.drawable.math_concept_fractions_decimals_ratio_percent
    "Geometry" -> R.drawable.math_concept_geometry
    "Linear Algebra" -> R.drawable.math_concept_linear_algebra
    "Logic and Foundations" -> R.drawable.math_concept_logic_foundations
    "Measurement" -> R.drawable.math_concept_measurement
    "Mensuration" -> R.drawable.math_concept_mensuration
    "Multivariable and Vector Calculus" -> R.drawable.math_concept_multivariable_vector_calculus
    "Number Theory" -> R.drawable.math_concept_number_theory
    "Numbers and Number Systems" -> R.drawable.math_concept_numbers_number_systems
    "Numerical Analysis" -> R.drawable.math_concept_numerical_analysis
    "Optimization and Operations Research" -> R.drawable.math_concept_optimization_operations_research
    "Probability and Statistics" -> R.drawable.math_concept_probability_statistics
    "Real and Functional Analysis" -> R.drawable.math_concept_real_functional_analysis
    "Sets Relations and Functions" -> R.drawable.math_concept_sets_relations_functions
    "Topology and Geometry" -> R.drawable.math_concept_topology_geometry
    "Trigonometry" -> R.drawable.math_concept_trigonometry
    else -> R.drawable.math_concept_algebra
}

@Composable
internal fun MathConceptIconImage(
    title: String,
    modifier: Modifier = Modifier.size(44.dp),
    cornerRadius: Dp = 14.dp,
) {
    Image(
        painter = painterResource(mathConceptIconRes(title)),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
    )
}
