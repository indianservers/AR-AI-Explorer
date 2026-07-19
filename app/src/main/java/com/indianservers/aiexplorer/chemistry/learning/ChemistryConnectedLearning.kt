package com.indianservers.aiexplorer.chemistry.learning

import com.indianservers.aiexplorer.connectedlearning.*

object ChemistryConnectedLearningRepository {
    private fun activities(id:String,formula:String?,tool:String)=buildList{
        add(LearningActivityReference("$id-lesson","Structured chemistry lesson",LearningActivityKind.Lesson,"Explanation, notation, evidence and model limitations."));add(LearningActivityReference("$id-diagram","Particle and structure diagram",LearningActivityKind.Diagram,"Accessible 2D atomic, orbital or bonding representation."));
        formula?.let{add(LearningActivityReference(it,"Related quantitative relationship",LearningActivityKind.Formula,"Open the independently owned Chemistry formula reference."))}
        add(LearningActivityReference(tool,"Interactive chemistry tool",LearningActivityKind.Interactive,"Guided builder or comparison contract using chemically valid states."));add(LearningActivityReference("$id-practice","Chemistry practice",LearningActivityKind.Practice,"Structure, trend, notation and reasoning questions."));add(LearningActivityReference("$id-quiz","Concept quiz",LearningActivityKind.Quiz,"Explanations classify model and reasoning errors."));add(LearningActivityReference("$id-revision","Revision map",LearningActivityKind.Revision,"Definitions, diagrams and commonly confused relationships."))
    }
    private fun c(id:String,title:String,prerequisites:List<String>,formula:String?,tool:String,explanation:String)=ConnectedConcept(id,title,"A connected step from atomic evidence to molecular shape.","$title explains and predicts observable chemical behaviour.",ConnectedLearningLevel.Class11,3,20,prerequisites,listOf("Represent $title with accepted chemical notation.","Relate evidence, model and prediction.","Recognise the limits of the simplified model."),prerequisites.takeLast(1),emptyList(),activities(id,formula,tool),CompletionCriteria(),ScientificReviewStatus.InternallyReviewed,explanation,"Undergraduate treatment adds quantum-mechanical detail and quantitative approximations.","A teaching model is not a literal picture of an atom or bond.","These ideas explain spectra, reactivity, materials and molecular properties.")
    val journey=ConnectedLearningJourney("chemistry-atomic-bonding","Chemistry","Atomic Structure → Molecular Geometry","Follow evidence from atomic structure through electrons, periodicity and bonding to three-dimensional molecular predictions.",listOf("atomic-structure","electron-configuration","periodic-trends","chemical-bonding","molecular-geometry"),listOf(
        c("atomic-structure","Atomic Structure",emptyList(),"chem-photon-energy","chemistry-atom-builder","Atoms contain a small nucleus and quantised electronic states; experimental evidence determines the model."),
        c("electron-configuration","Electron Configuration",listOf("atomic-structure"),"chem-electron-capacity","chemistry-orbital-filling","Electron configurations distribute electrons among permitted shells, subshells and orbitals while respecting occupancy rules."),
        c("periodic-trends","Periodic Trends",listOf("electron-configuration"),"chem-effective-charge","chemistry-trend-comparison","Repeating configurations and changing effective nuclear attraction produce systematic but exception-aware trends."),
        c("chemical-bonding","Chemical Bonding",listOf("periodic-trends"),"chem-formal-charge","chemistry-bond-builder","Bonding models describe redistribution or sharing of electron density and must be chosen for the question being asked."),
        c("molecular-geometry","Molecular Geometry",listOf("chemical-bonding"),null,"chemistry-vsepr-explorer","Electron-domain repulsions provide a useful first model for molecular shape, while lone pairs and multiple bonds modify angles."),
    ))
    fun getConcept(id:String)=journey.concepts.find{it.id==id}
    fun validate()=ConnectedLearningValidator.validate(journey,setOf(LearningActivityKind.Lesson,LearningActivityKind.Diagram,LearningActivityKind.Interactive,LearningActivityKind.Practice,LearningActivityKind.Quiz,LearningActivityKind.Revision))
}

