package com.indianservers.aiexplorer.physics.learning

import com.indianservers.aiexplorer.connectedlearning.*

object PhysicsConnectedLearningRepository {
    private fun activities(id:String,formulaId:String,simulation:String)=listOf(
        LearningActivityReference("$id-lesson","Structured lesson",LearningActivityKind.Lesson,"Explanation, objectives, misconception and application."),
        LearningActivityReference("$id-diagram","Vector and relationship diagram",LearningActivityKind.Diagram,"Accessible 2D axes, quantities and direction relationships."),
        LearningActivityReference(formulaId,"Related formula",LearningActivityKind.Formula,"Open the independently owned Physics formula reference."),
        LearningActivityReference("$formulaId-calculator","Formula calculator",LearningActivityKind.Calculator,"Validated calculator contract; interactive solving follows in Formula Phase 3.",false),
        LearningActivityReference(simulation,"Interactive exploration",LearningActivityKind.Interactive,"Guided variable-prediction activity with live-value contract."),
        LearningActivityReference("$id-practice","Physics practice",LearningActivityKind.Practice,"Unit, graph, calculation and conceptual checks."),
        LearningActivityReference("$id-quiz","Concept quiz",LearningActivityKind.Quiz,"Explain formula choice, direction and units after every answer."),
        LearningActivityReference("$id-revision","Revision card",LearningActivityKind.Revision,"Key relationship, unit check and misconception review."),
    )
    private fun c(id:String,title:String,description:String,why:String,prerequisites:List<String>,formula:String,simulation:String,explanation:String)=ConnectedConcept(id,title,description,why,ConnectedLearningLevel.Class9,2,18,prerequisites,listOf("Explain $title using quantities and evidence.","Connect the diagram, formula and physical interpretation.","Check units and limiting cases."),prerequisites.takeLast(1),emptyList(),activities(id,formula,simulation),CompletionCriteria(),ScientificReviewStatus.InternallyReviewed,explanation,"At higher levels, vector notation, calculus and uncertainty refine this model.","A formula is not meaningful until its direction, reference frame and assumptions are identified.","The same relationship is used to interpret transport, engineering and measured motion.")
    val journey=ConnectedLearningJourney("physics-motion-force-energy","Physics","Motion → Force → Energy","A dependency-based journey connecting lessons, vectors, formulas, interaction, practice and assessment.",listOf("motion","velocity","acceleration","newton-second-law","work-energy"),listOf(
        c("motion","Motion","Describe position change relative to a reference frame.","Motion is the language used before velocity, acceleration and force can be defined.",emptyList(),"physics-speed","physics-motion-graph","Position and displacement describe how an object's location changes relative to a chosen origin and clock."),
        c("velocity","Velocity","Relate displacement to elapsed time with direction.","Velocity distinguishes directed motion from scalar speed.",listOf("motion"),"physics-speed","physics-vector-velocity","Average velocity is displacement divided by elapsed time; instantaneous velocity is the limiting rate of change of position."),
        c("acceleration","Acceleration","Explain how velocity changes with time.","Acceleration connects motion descriptions to interactions and forces.",listOf("velocity"),"physics-final-velocity","physics-uniform-acceleration","Acceleration is the rate of change of velocity, so it may arise from a speed change, a direction change, or both."),
        c("newton-second-law","Newton's Second Law","Connect net force, mass and acceleration.","This law predicts how interactions change motion.",listOf("acceleration"),"physics-newton-second-law","physics-force-vector","For constant mass, the vector sum of external forces equals mass times acceleration: ΣF = ma."),
        c("work-energy","Work and Energy","Use energy transfer to explain changes in motion.","Energy provides a complementary system-level account of motion and force.",listOf("newton-second-law"),"physics-kinetic-energy","physics-work-energy","Work done by a net force changes kinetic energy; energy accounting can simplify motion problems."),
    ))
    fun getConcept(id:String)=journey.concepts.find{it.id==id}
    fun validate()=ConnectedLearningValidator.validate(journey,setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.Formula,LearningActivityKind.Calculator,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision))
}

