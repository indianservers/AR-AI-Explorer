package com.indianservers.aiexplorer.learningintelligence.repository

import com.indianservers.aiexplorer.learningintelligence.model.*

interface LearnerStateRepository { fun get(conceptId:String):LearnerConceptState?; fun all():List<LearnerConceptState>; fun save(state:LearnerConceptState) }
interface DiagnosticRepository { fun saveSession(session:DiagnosticSession);fun session(id:String):DiagnosticSession?;fun saveResult(result:DiagnosticResult);fun result(subject:com.indianservers.aiexplorer.curriculum.SchoolSubject):DiagnosticResult? }
interface KnowledgeGraphRepository { fun graph():KnowledgeGraph }
interface MasteryRepository { fun masteryEvidence(conceptId:String):List<MasteryEvidence>;fun appendMastery(evidence:MasteryEvidence) }
interface MisconceptionRepository { fun definitions(conceptId:String?=null):List<MisconceptionDefinition>;fun misconceptionEvidence(conceptId:String):List<MisconceptionEvidence>;fun appendMisconception(evidence:MisconceptionEvidence) }
interface HintRepository { fun hints(conceptId:String):List<HintDefinition>;fun recordSelection(conceptId:String,hintId:String);fun used(conceptId:String):Set<String> }
interface ReviewScheduleRepository { fun review(conceptId:String):ScheduledReview?;fun due():List<ScheduledReview>;fun save(review:ScheduledReview) }
interface RecommendationRepository { fun current():List<LearningRecommendation>;fun replace(recommendations:List<LearningRecommendation>) }
interface ErrorBookRepository { fun entry(id:String):ErrorBookEntry?;fun entries(status:ErrorBookStatus?=null):List<ErrorBookEntry>;fun save(entry:ErrorBookEntry) }

class InMemoryLearningIntelligenceRepository(
 private val knowledgeGraph:KnowledgeGraph,
 private val authoredMisconceptions:List<MisconceptionDefinition> = emptyList(),
 private val authoredHints:List<HintDefinition> = emptyList()
):LearnerStateRepository,DiagnosticRepository,KnowledgeGraphRepository,MasteryRepository,MisconceptionRepository,HintRepository,ReviewScheduleRepository,RecommendationRepository,ErrorBookRepository{
 private val states=linkedMapOf<String,LearnerConceptState>();private val sessions=linkedMapOf<String,DiagnosticSession>();private val results=linkedMapOf<com.indianservers.aiexplorer.curriculum.SchoolSubject,DiagnosticResult>();private val mastery=mutableListOf<MasteryEvidence>();private val misconceptionEvidence=mutableListOf<MisconceptionEvidence>();private val usedHints=mutableMapOf<String,MutableSet<String>>();private val reviews=linkedMapOf<String,ScheduledReview>();private var recommendations=emptyList<LearningRecommendation>();private val errors=linkedMapOf<String,ErrorBookEntry>()
 override fun get(conceptId:String)=states[conceptId];override fun all()=states.values.toList();override fun save(state:LearnerConceptState){states[state.conceptId]=state}
 override fun saveSession(session:DiagnosticSession){sessions[session.id]=session};override fun session(id:String)=sessions[id];override fun saveResult(result:DiagnosticResult){results[result.subject]=result};override fun result(subject:com.indianservers.aiexplorer.curriculum.SchoolSubject)=results[subject]
 override fun graph()=knowledgeGraph;override fun masteryEvidence(conceptId:String)=mastery.filter{it.conceptId==conceptId};override fun appendMastery(evidence:MasteryEvidence){mastery+=evidence}
 override fun definitions(conceptId:String?)=authoredMisconceptions.filter{conceptId==null||it.conceptId==conceptId};override fun misconceptionEvidence(conceptId:String)=misconceptionEvidence.filter{it.conceptId==conceptId};override fun appendMisconception(evidence:MisconceptionEvidence){misconceptionEvidence+=evidence}
 override fun hints(conceptId:String)=authoredHints.filter{it.conceptId==conceptId};override fun recordSelection(conceptId:String,hintId:String){usedHints.getOrPut(conceptId){mutableSetOf()}+=hintId};override fun used(conceptId:String)=usedHints[conceptId].orEmpty()
 override fun review(conceptId:String)=reviews[conceptId];override fun due()=reviews.values.filter{it.urgency!=ReviewUrgency.NOT_DUE};override fun save(review:ScheduledReview){reviews[review.conceptId]=review}
 override fun current()=recommendations;override fun replace(recommendations:List<LearningRecommendation>){this.recommendations=recommendations}
 override fun entry(id:String)=errors[id];override fun entries(status:ErrorBookStatus?)=errors.values.filter{status==null||it.status==status};override fun save(entry:ErrorBookEntry){errors[entry.id]=entry}
}
