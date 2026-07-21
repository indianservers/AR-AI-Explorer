package com.indianservers.aiexplorer.learningintelligence.migration

import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.curriculum.production.LearnerCurriculumProgress
import com.indianservers.aiexplorer.learningintelligence.model.*

object LearningProgressMigration{
 /** Legacy page completion is preserved as introduction evidence, never converted into mastery. */
 fun migrate(old:LearnerCurriculumProgress,contentToConcept:Map<String,String>,conceptSubjects:Map<String,SchoolSubject>,curriculumNodes:Map<String,Set<String>>,existing:Map<String,LearnerConceptState> = emptyMap()):Map<String,LearnerConceptState>{val migrated=existing.toMutableMap();old.completedContentIds.forEach{contentId->val concept=contentToConcept[contentId]?:return@forEach;if(concept !in migrated){val subject=conceptSubjects[concept]?:return@forEach;migrated[concept]=LearnerConceptState(concept,curriculumNodes[concept].orEmpty(),subject,exposureCount=1,lessonCompletionCount=1,masteryState=ConceptMasteryState.INTRODUCED)}};return migrated}
}
