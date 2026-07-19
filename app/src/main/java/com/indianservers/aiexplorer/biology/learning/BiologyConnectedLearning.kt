package com.indianservers.aiexplorer.biology.learning

import com.indianservers.aiexplorer.connectedlearning.*

object BiologyConnectedLearningRepository {
    private fun activities(id:String,sourceConcept:String,process:String)=listOf(
        LearningActivityReference(sourceConcept,"Biology concept lesson",LearningActivityKind.Lesson,"Link to the existing Biology catalogue concept and progressive explanation."),
        LearningActivityReference("diagram-$id","Labelled 2D diagram",LearningActivityKind.Diagram,"Structure labels, functions and screen-reader reading order."),
        LearningActivityReference(process,"Process view",LearningActivityKind.ProcessView,"Stepwise membrane or homeostasis mechanism."),
        LearningActivityReference("$id-2d-explore","Interactive 2D exploration",LearningActivityKind.Interactive,"Select structures or vary a concentration gradient without requiring 3D."),
        LearningActivityReference("$id-practice","Biology practice",LearningActivityKind.Practice,"Structure–function, sequence and data-interpretation checks."),
        LearningActivityReference("$id-quiz","Biology concept quiz",LearningActivityKind.Quiz,"Explanations target terminology and mechanism mistakes."),
        LearningActivityReference("$id-revision","Biology revision",LearningActivityKind.Revision,"Diagram, key process and misconception recall."),
        LearningActivityReference("future3d-$id","Future 3D metadata",LearningActivityKind.Future3D,"Planned verified asset with the labelled 2D diagram as fallback.",false),
    )
    private fun c(id:String,title:String,prerequisites:List<String>,source:String,process:String,explanation:String)=ConnectedConcept(id,title,"A connected cell-biology concept from structure to stable internal conditions.","$title links cell structure to survival and coordinated function.",ConnectedLearningLevel.Class9,2,18,prerequisites,listOf("Identify the relevant structures.","Explain the mechanism in a correct sequence.","Predict an outcome when conditions change."),prerequisites.takeLast(1),emptyList(),activities(id,source,process),CompletionCriteria(),ScientificReviewStatus.InternallyReviewed,explanation,"Advanced study adds membrane energetics, channel kinetics, signalling networks and experimental evidence.","Cell diagrams are explanatory models and are not normally drawn to scale.","Membrane transport and homeostasis explain medical, plant and ecological observations without providing personal diagnosis.")
    val journey=ConnectedLearningJourney("biology-cell-homeostasis","Biology","Cell Structure → Homeostasis","Connect cell organisation, membranes and transport processes to the maintenance of viable internal conditions.",listOf("cell-structure","cell-membrane","diffusion-osmosis","active-transport","cell-homeostasis"),listOf(
        c("cell-structure","Cell Structure",emptyList(),"concept-cell-biology-eukaryotic-cells","biology-cell-part-selector","Eukaryotic cells contain specialised structures whose organisation supports metabolism, information handling and exchange."),
        c("cell-membrane","Cell Membrane",listOf("cell-structure"),"concept-cell-biology-plasma-membrane","biology-membrane-layer-view","The plasma membrane is a selectively permeable lipid-bilayer system containing proteins, carbohydrates and other components."),
        c("diffusion-osmosis","Diffusion and Osmosis",listOf("cell-membrane"),"concept-cell-biology-diffusion","biology-gradient-view","Diffusion is net movement down a concentration gradient; osmosis concerns water movement across a selectively permeable membrane."),
        c("active-transport","Active Transport",listOf("diffusion-osmosis"),"concept-cell-biology-active-transport","biology-pump-cycle","Active transport couples energy to movement against an electrochemical gradient through specific membrane proteins."),
        c("cell-homeostasis","Cell Homeostasis",listOf("active-transport"),"concept-human-physiology-homeostasis","biology-feedback-loop","Cells maintain dynamic internal ranges through transport, sensing, signalling and feedback rather than remaining chemically static."),
    ))
    fun getConcept(id:String)=journey.concepts.find{it.id==id}
    fun validate()=ConnectedLearningValidator.validate(journey,setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.ProcessView,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision,LearningActivityKind.Future3D))
}

