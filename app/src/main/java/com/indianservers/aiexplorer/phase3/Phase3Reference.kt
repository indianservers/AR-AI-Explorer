package com.indianservers.aiexplorer.phase3

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learning.ncert.assessment.MathematicsAssessmentAdapter
import com.indianservers.aiexplorer.physics.ncert.assessment.PhysicsAssessmentAdapter
import com.indianservers.aiexplorer.chemistry.ncert.assessment.ChemistryAssessmentAdapter
import com.indianservers.aiexplorer.biology.ncert.assessment.BiologyAssessmentAdapter
import com.indianservers.aiexplorer.learningintelligence.model.LearnerErrorType
import com.indianservers.aiexplorer.phase3.assessment.*
import com.indianservers.aiexplorer.phase3.governance.*
import com.indianservers.aiexplorer.phase3.ar.ArLearningRegistry

object Phase3Reference{
 val validators=mapOf(SchoolSubject.MATHEMATICS to MathematicsAssessmentAdapter,SchoolSubject.PHYSICS to PhysicsAssessmentAdapter,SchoolSubject.CHEMISTRY to ChemistryAssessmentAdapter,SchoolSubject.BIOLOGY to BiologyAssessmentAdapter)
 val rubrics=listOf(
  StepScoringRubric("math-linear-step",listOf(StepCriterion("formula","Form equation",1.0,"linear-formula",emptySet(),LearnerErrorType.FORMULA),StepCriterion("transform","Preserve equality",2.0,"linear-transform",setOf("formula"),LearnerErrorType.ALGEBRAIC_TRANSFORMATION),StepCriterion("conclusion","Verify solution",1.0,"proof-reason",setOf("transform"),LearnerErrorType.CONCEPTUAL)),4.0,SchoolSubject.MATHEMATICS,ScientificReviewStatus.Verified),
  StepScoringRubric("physics-mechanics",listOf(StepCriterion("formula","Select formula",1.0,"mechanics-formula",emptySet(),LearnerErrorType.FORMULA),StepCriterion("value","Calculate with unit",2.0,"mechanics-value",setOf("formula"),LearnerErrorType.DIMENSION)),3.0,SchoolSubject.PHYSICS,ScientificReviewStatus.Verified),
  StepScoringRubric("chemistry-stoichiometry",listOf(StepCriterion("ratio","Mole ratio",2.0,"stoichiometry-ratio",emptySet(),LearnerErrorType.FORMULA),StepCriterion("conservation","Atoms and charge",1.0,"atom-charge-conservation",setOf("ratio"),LearnerErrorType.ATOM_CONSERVATION)),3.0,SchoolSubject.CHEMISTRY,ScientificReviewStatus.Verified),
  StepScoringRubric("biology-genetics",listOf(StepCriterion("ratio","Genetic ratio",2.0,"genetics-ratio",emptySet(),LearnerErrorType.PROBABILITY),StepCriterion("sequence","Explain segregation",1.0,"process-sequence",setOf("ratio"),LearnerErrorType.SEQUENCE)),3.0,SchoolSubject.BIOLOGY,ScientificReviewStatus.Verified))
 val formulas=listOf(ScientificFormulaDefinition("physics-force",SchoolSubject.PHYSICS,DimensionExpression.Variable("F"),DimensionExpression.Product(DimensionExpression.Variable("m"),DimensionExpression.Variable("a")),mapOf("F" to Dimension(mapOf(BaseDimension.MASS to 1,BaseDimension.LENGTH to 1,BaseDimension.TIME to -2)),"m" to Dimension(mapOf(BaseDimension.MASS to 1)),"a" to Dimension(mapOf(BaseDimension.LENGTH to 1,BaseDimension.TIME to -2))),true),ScientificFormulaDefinition("chemistry-concentration",SchoolSubject.CHEMISTRY,DimensionExpression.Variable("c"),DimensionExpression.Quotient(DimensionExpression.Variable("n"),DimensionExpression.Variable("V")),mapOf("c" to Dimension(mapOf(BaseDimension.AMOUNT to 1,BaseDimension.LENGTH to -3)),"n" to Dimension(mapOf(BaseDimension.AMOUNT to 1)),"V" to Dimension(mapOf(BaseDimension.LENGTH to 3))),true))
}

data class Phase3ReleaseValidation(val errors:List<String>){val valid get()=errors.isEmpty()}
object Phase3ReleaseValidator{fun validate():Phase3ReleaseValidation{val errors=mutableListOf<String>();if(Phase3Reference.rubrics.any{it.subject !in Phase3Reference.validators})errors+="Rubric lacks subject adapter.";errors+=DimensionalConsistencyValidator().validateVerified(Phase3Reference.formulas).flatMap{it.issues};errors+=ArLearningRegistry.validate();errors+=ContentLineageRegistry.validateReleased(com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog.conceptIds());return Phase3ReleaseValidation(errors)}}
