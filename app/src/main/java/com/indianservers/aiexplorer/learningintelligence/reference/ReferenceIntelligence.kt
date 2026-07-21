package com.indianservers.aiexplorer.learningintelligence.reference

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.curriculum.*
import com.indianservers.aiexplorer.curriculum.interaction.ReferenceActivityRegistry
import com.indianservers.aiexplorer.learningintelligence.model.*

data class ReferenceConceptIntelligence(
 val conceptId:String,val curriculumNodeId:String,val subject:SchoolSubject,val classLevels:Set<SchoolClassLevel>,val prerequisiteConceptIds:List<String>,val activityId:String?,
 val policy:MasteryPolicy,val diagnosticQuestions:List<DiagnosticQuestion>,val misconceptions:List<MisconceptionDefinition>,val hints:List<HintDefinition>,val workedExample:FadedWorkedExample
)

object ReferenceCurriculumResolver{
 fun topic(subject:SchoolSubject,vararg keywords:String):String{val chapters=CbseNcert2026Curriculum.manifests.filter{it.subject==subject}.flatMap{it.units}.flatMap{it.chapters};return chapters.firstOrNull{c->keywords.any{c.officialTitle.contains(it,true)}}?.topics?.first()?.id?:chapters.first().topics.first().id}
}

object ReferenceConceptFactory{
 fun create(conceptId:String,curriculumNodeId:String,subject:SchoolSubject,levels:Set<SchoolClassLevel>,prerequisites:List<String>,activityId:String?,title:String,misconceptionId:String,misconceptionTitle:String,misconceptionDescription:String,correct:String,wrong:String,kind:DiagnosticQuestionKind,required:Set<MasteryEvidenceType>,application:Boolean=true):ReferenceConceptIntelligence{
  require(activityId==null||ReferenceActivityRegistry.definitions.any{it.activityId==activityId})
  val qid="diag-$conceptId";val remediation=activityId?:"revision-$conceptId"
  val question=DiagnosticQuestion(qid,subject,conceptId,title.substringBefore(' '),levels.minBy{it.number},kind,"Which statement best demonstrates $title?",listOf(correct,wrong,"Not enough information"),0,mapOf(1 to misconceptionId),1.0,asksConfidence=true)
  val misconception=MisconceptionDefinition(misconceptionId,conceptId,misconceptionTitle,misconceptionDescription,listOf(MisconceptionEvidenceRule.WrongOption(qid,1,1.0)),"explain-$conceptId",activityId,listOf(qid),ScientificReviewStatus.Verified)
  val texts=listOf("Restate what the question is asking without calculating.","Recall the defining idea for $title.","Focus the linked visual on the quantity or structure that changes.","Complete the first valid reasoning step, then pause.","Use the shown method but supply its final transformation.","Study the complete explanation, then solve a fresh retry; this attempt will not count as independent mastery.")
  val hints=HintLevel.entries.mapIndexed{i,level->HintDefinition("hint-$conceptId-$i",conceptId,activityId,level,texts[i],if(level==HintLevel.VISUAL_FOCUS)"focus-$conceptId" else null,(0 until maxOf(0,i-2)).map{"step-$conceptId-$it"}.toSet(),setOf(misconceptionId))}
  val steps=listOf(WorkedSolutionStep("step-$conceptId-0","Identify the governing concept",null,"Select $title because its definition matches the evidence.",emptySet(),"rule-$conceptId-concept"),WorkedSolutionStep("step-$conceptId-1","Apply the representation",correct,"Connect the statement, visual or calculation to the concept.",setOf("step-$conceptId-0"),"rule-$conceptId-apply"),WorkedSolutionStep("step-$conceptId-2","Check and explain",null,"Check units, constraints or biological/scientific meaning before concluding.",setOf("step-$conceptId-1"),"rule-$conceptId-check"))
  val example=FadedWorkedExample("example-$conceptId",conceptId,steps,FadeStage.entries,FadeStage.FULLY_WORKED,steps.mapNotNull{it.validationRuleId})
  return ReferenceConceptIntelligence(conceptId,curriculumNodeId,subject,levels,prerequisites,activityId,MasteryPolicy(conceptId,required,3,1,.7,.4,application),listOf(question),listOf(misconception),hints,example)
 }
}

object LearningIntelligenceCatalog{
 val concepts:List<ReferenceConceptIntelligence> by lazy { com.indianservers.aiexplorer.learning.ncert.intelligence.MathematicsLearningIntelligence.concepts+com.indianservers.aiexplorer.physics.ncert.intelligence.PhysicsLearningIntelligence.concepts+com.indianservers.aiexplorer.chemistry.ncert.intelligence.ChemistryLearningIntelligence.concepts+com.indianservers.aiexplorer.biology.ncert.intelligence.BiologyLearningIntelligence.concepts }
 val policies get()=concepts.associate{it.conceptId to it.policy};val diagnosticQuestions get()=concepts.flatMap{it.diagnosticQuestions};val misconceptions get()=concepts.flatMap{it.misconceptions};val hints get()=concepts.flatMap{it.hints};val examples get()=concepts.associate{it.conceptId to it.workedExample};val activities get()=concepts.mapNotNull{c->c.activityId?.let{c.conceptId to it}}.toMap()
 val graph:KnowledgeGraph by lazy{val nodes=mutableListOf<KnowledgeNode>();val edges=mutableListOf<KnowledgeEdge>();concepts.forEach{c->nodes+=KnowledgeNode(c.conceptId,KnowledgeNodeType.CONCEPT,c.subject,c.classLevels,ScientificReviewStatus.Verified);nodes+=KnowledgeNode(c.curriculumNodeId,KnowledgeNodeType.CURRICULUM_NODE,c.subject,c.classLevels,ScientificReviewStatus.Verified);edges+=KnowledgeEdge(c.curriculumNodeId,c.conceptId,KnowledgeRelation.EXTENDS_TO,1.0,false);c.prerequisiteConceptIds.forEach{edges+=KnowledgeEdge(it,c.conceptId,KnowledgeRelation.PREREQUISITE_OF,1.0,true)};c.diagnosticQuestions.forEach{q->nodes+=KnowledgeNode(q.id,KnowledgeNodeType.QUESTION,c.subject,c.classLevels,ScientificReviewStatus.Verified);edges+=KnowledgeEdge(c.conceptId,q.id,KnowledgeRelation.TESTED_BY_QUESTION,1.0,true)};c.misconceptions.forEach{m->nodes+=KnowledgeNode(m.id,KnowledgeNodeType.MISCONCEPTION,c.subject,c.classLevels,ScientificReviewStatus.Verified);val remediation=m.remediationActivityId?:"revision-${c.conceptId}";if(nodes.none{it.id==remediation})nodes+=KnowledgeNode(remediation,if(m.remediationActivityId==null)KnowledgeNodeType.REVISION_RESOURCE else KnowledgeNodeType.ACTIVITY,c.subject,c.classLevels,ScientificReviewStatus.Verified);edges+=KnowledgeEdge(m.id,remediation,KnowledgeRelation.REMEDIATES_MISCONCEPTION,1.0,true)};c.activityId?.let{a->if(nodes.none{it.id==a})nodes+=KnowledgeNode(a,KnowledgeNodeType.ACTIVITY,c.subject,c.classLevels,ScientificReviewStatus.Verified);edges+=KnowledgeEdge(c.conceptId,a,KnowledgeRelation.PRACTISED_BY_ACTIVITY,1.0,false)}};KnowledgeGraph(nodes.distinctBy{it.id},edges.distinct())}
 fun conceptIds()=concepts.map{it.conceptId}.toSet()
 fun curriculumIds()=CbseNcert2026Curriculum.manifests.flatMap{it.units}.flatMap{it.chapters}.flatMap{it.topics}.map{it.id}.toSet()
}
