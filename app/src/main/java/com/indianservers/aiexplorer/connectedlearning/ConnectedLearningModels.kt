package com.indianservers.aiexplorer.connectedlearning

enum class ConnectedLearningLevel(val label:String,val rank:Int){Foundation("Foundation",0),Class7("Class 7",1),Class8("Class 8",2),Class9("Class 9",3),Class10("Class 10",4),Class11("Class 11",5),Class12("Class 12",6),Undergraduate("Undergraduate",7),Postgraduate("Postgraduate",8)}
enum class LearningMode{Learn,Explore,Test}
enum class ScientificReviewStatus{Draft,InternallyReviewed,Verified,NeedsRevision}
enum class LearningActivityKind{Lesson,Diagram,Formula,Calculator,Interactive,Simulation,Experiment,ProcessView,Practice,Quiz,Revision,Future3D}
enum class ConceptMasteryState{NotStarted,Introduced,Learning,Practising,Proficient,Mastered,NeedsReview}
data class LearningActivityReference(val id:String,val title:String,val kind:LearningActivityKind,val description:String,val available:Boolean=true)
data class CompletionCriteria(val lessonRequired:Boolean=true,val practiceAttempts:Int=2,val quizMinimumPercent:Int=70,val interactiveEvidenceRequired:Boolean=true)
data class ConnectedConcept(
    val id:String,val title:String,val description:String,val whyItMatters:String,val level:ConnectedLearningLevel,val difficulty:Int,val estimatedMinutes:Int,
    val prerequisiteIds:List<String>,val learningObjectives:List<String>,val relatedConceptIds:List<String>,val commonlyConfusedIds:List<String>,
    val activities:List<LearningActivityReference>,val completionCriteria:CompletionCriteria,val reviewStatus:ScientificReviewStatus,
    val coreExplanation:String,val advancedExplanation:String,val misconception:String,val realWorldApplication:String,
)
data class ConnectedLearningJourney(val id:String,val subject:String,val title:String,val description:String,val conceptIds:List<String>,val concepts:List<ConnectedConcept>)
data class ConnectedLearningValidationReport(val errors:List<String>){val valid get()=errors.isEmpty()}

object ConnectedLearningValidator{
    fun validate(journey:ConnectedLearningJourney,requiredKinds:Set<LearningActivityKind>):ConnectedLearningValidationReport{
        val errors=mutableListOf<String>();val ids=journey.concepts.map{it.id};if(ids.size!=ids.toSet().size)errors+="Duplicate concept ID.";val idSet=ids.toSet()
        if(journey.conceptIds!=ids)errors+="Journey order and concept catalogue differ."
        journey.concepts.forEach{concept->
            if(concept.prerequisiteIds.any{it !in idSet}||concept.relatedConceptIds.any{it !in idSet}||concept.commonlyConfusedIds.any{it !in idSet})errors+="${concept.id} has a broken concept link."
            if(concept.learningObjectives.isEmpty()||concept.coreExplanation.isBlank())errors+="${concept.id} has incomplete learning content."
            if(concept.activities.map{it.id}.let{it.size!=it.toSet().size})errors+="${concept.id} has duplicate activity IDs."
            val kinds=concept.activities.map{it.kind}.toSet();if(!kinds.containsAll(requiredKinds))errors+="${concept.id} lacks required connected activities: ${requiredKinds-kinds}."
            if(concept.activities.any{it.id.isBlank()||it.title.isBlank()||it.description.isBlank()})errors+="${concept.id} exposes an empty activity."
            if(concept.reviewStatus==ScientificReviewStatus.Draft)errors+="${concept.id} is draft content exposed in a reference journey."
        }
        val visiting=mutableSetOf<String>();val visited=mutableSetOf<String>();val byId=journey.concepts.associateBy{it.id}
        fun cycle(id:String):Boolean{if(id in visiting)return true;if(id in visited)return false;visiting+=id;val found=byId[id].orEmptyPrerequisites().any(::cycle);visiting-=id;visited+=id;return found}
        if(ids.any(::cycle))errors+="Circular prerequisite chain."
        return ConnectedLearningValidationReport(errors)
    }
    private fun ConnectedConcept?.orEmptyPrerequisites()=this?.prerequisiteIds.orEmpty()
}

data class RecommendedConcept(val concept:ConnectedConcept,val reason:String)
object RecommendedNextEngine{
    fun recommend(journey:ConnectedLearningJourney,completed:Set<String>,level:ConnectedLearningLevel):RecommendedConcept?{
        val concept=journey.concepts.firstOrNull{it.id !in completed&&it.level.rank<=level.rank&&it.prerequisiteIds.all(completed::contains)}?:return null
        val reason=if(concept.prerequisiteIds.isEmpty())"Recommended as the foundation for this learning path." else "Recommended because you completed ${concept.prerequisiteIds.mapNotNull{journey.concepts.find{c->c.id==it}?.title}.joinToString()} and this unlocks the next relationship."
        return RecommendedConcept(concept,reason)
    }
}

