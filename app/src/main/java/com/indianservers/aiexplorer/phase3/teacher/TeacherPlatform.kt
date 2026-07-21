package com.indianservers.aiexplorer.phase3.teacher

import com.indianservers.aiexplorer.curriculum.SchoolClassLevel
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningworkspace.ExperimentNotebookEntry
import java.time.Instant
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

enum class PlatformRole{LEARNER,TEACHER,REVIEWER,SCHOOL_ADMIN}
enum class PlatformPermission{CREATE_CLASS,MANAGE_ASSIGNMENT,READ_ASSIGNED_EVIDENCE,READ_PRIVATE_NOTE,EXPORT_CLASS_REPORT,DELETE_CLASSROOM,RELEASE_CONTENT}
data class Classroom(val id:String,val title:String,val subject:SchoolSubject?,val classLevel:SchoolClassLevel?,val teacherIds:Set<String>,val learnerIds:Set<String>,val retentionUntil:Instant?=null)
enum class DifferentiationPath{FOUNDATION_BRIDGE,STANDARD,CHALLENGE,PRACTICAL_FOCUS,EXAM_REVISION,MISCONCEPTION_REPAIR}
data class DifferentiationRule(val groupId:String,val path:DifferentiationPath,val conceptIds:Set<String>)
data class Assignment(val id:String,val classroomId:String,val title:String,val conceptIds:List<String>,val activityIds:List<String>,val questionSetId:String?,val dueAt:Instant?,val differentiationRules:List<DifferentiationRule>)
data class ClassroomInvite(val tokenHash:String,val classroomId:String,val expiresAt:Instant,val maximumUses:Int,val used:Int=0)
data class IssuedClassroomInvite(val plaintextToken:String,val persistedInvite:ClassroomInvite)
enum class SubmissionStatus{DRAFT,SUBMITTED,REVISION_REQUESTED,APPROVED,RETURNED}
data class ExperimentSubmission(val id:String,val assignmentId:String,val learnerId:String,val notebookEntryId:String,val measurementIds:List<String>,val graphReferences:List<String>,val observationCount:Int,val conclusion:String?,val savedStateIds:List<String>,val reflection:String?,val version:Int,val submittedAt:Instant,val status:SubmissionStatus)
data class TeacherFeedback(val id:String,val submissionId:String,val teacherId:String,val rubricScores:Map<String,Double>,val comment:String,val status:SubmissionStatus,val remediationConceptId:String?,val createdAt:Instant)
data class MasteryDistribution(val conceptId:String,val mastered:Int,val proficient:Int,val learning:Int,val needsReview:Int,val missingPrerequisite:Int,val evidenceQuality:Map<EvidenceQuality,Int>,val calibrationObservations:Int)
data class MisconceptionCluster(val misconceptionId:String,val learnerCount:Int,val evidenceCount:Int,val visible:Boolean,val explanation:String)

class TeacherPlatformEngine(private val minimumClusterSize:Int=5){
 fun createClassroom(id:String,title:String,subject:SchoolSubject?,level:SchoolClassLevel?,teacherId:String,retentionUntil:Instant?)=Classroom(id,title,subject,level,setOf(teacherId),emptySet(),retentionUntil)
 fun createAssignment(classroom:Classroom,id:String,title:String,conceptIds:List<String>,activityIds:List<String>,dueAt:Instant?,rules:List<DifferentiationRule>):Assignment=Assignment(id,classroom.id,title,conceptIds,activityIds,null,dueAt,rules).also{validate(classroom,it)}
 fun validate(classroom:Classroom,assignment:Assignment){require(assignment.classroomId==classroom.id);require(assignment.conceptIds.all{LearningIntelligenceCatalog.conceptIds().contains(it)});classroom.subject?.let{s->require(assignment.conceptIds.all{id->LearningIntelligenceCatalog.concepts.single{it.conceptId==id}.subject==s})}}
 fun canReadLearnerEvidence(teacherId:String,learnerId:String,classroom:Classroom)=teacherId in classroom.teacherIds&&learnerId in classroom.learnerIds
 fun canReadPrivateNote(requesterId:String,learnerId:String)=requesterId==learnerId
 fun masteryMap(classroom:Classroom,statesByLearner:Map<String,List<LearnerConceptState>>):List<MasteryDistribution>{val scoped=statesByLearner.filterKeys{it in classroom.learnerIds};return scoped.values.flatten().groupBy{it.conceptId}.map{(id,states)->MasteryDistribution(id,states.count{it.masteryState==ConceptMasteryState.MASTERED},states.count{it.masteryState==ConceptMasteryState.PROFICIENT},states.count{it.masteryState in setOf(ConceptMasteryState.LEARNING,ConceptMasteryState.PRACTISING)},states.count{it.masteryState==ConceptMasteryState.NEEDS_REVIEW},states.count{it.masteryState==ConceptMasteryState.NOT_STARTED},states.groupingBy{it.evidenceQuality}.eachCount(),states.sumOf{it.confidenceSummary.observations})}}
 fun misconceptionClusters(classroom:Classroom,statesByLearner:Map<String,List<LearnerConceptState>>):List<MisconceptionCluster>{val evidence=statesByLearner.filterKeys{it in classroom.learnerIds}.flatMap{(learner,states)->states.flatMap{it.misconceptionEvidence}.map{learner to it}};return evidence.groupBy{it.second.misconceptionId}.map{(id,items)->val learners=items.map{it.first}.distinct().size;MisconceptionCluster(id,learners,items.size,learners>=minimumClusterSize,if(learners>=minimumClusterSize)"Class-level pattern supported by ${items.size} evidence events." else "Hidden to preserve learner privacy.")}}
 fun submitNotebook(assignment:Assignment,learnerId:String,entry:ExperimentNotebookEntry,version:Int,now:Instant)=ExperimentSubmission("submission-${assignment.id}-$learnerId-$version",assignment.id,learnerId,entry.id,entry.measurements.map{"${it.variableId}-${it.recordedAt.toEpochMilli()}"},entry.graphReferences,entry.observations.size,entry.conclusion,entry.savedStateIds,entry.reflection,version,now,SubmissionStatus.SUBMITTED)
 fun feedback(classroom:Classroom,teacherId:String,submission:ExperimentSubmission,scores:Map<String,Double>,comment:String,status:SubmissionStatus,remediation:String?,now:Instant):TeacherFeedback{require(teacherId in classroom.teacherIds);require(submission.learnerId in classroom.learnerIds);require(status in setOf(SubmissionStatus.REVISION_REQUESTED,SubmissionStatus.APPROVED,SubmissionStatus.RETURNED));remediation?.let{require(it in LearningIntelligenceCatalog.conceptIds())};return TeacherFeedback("feedback-${submission.id}-${now.toEpochMilli()}",submission.id,teacherId,scores,comment,status,remediation,now)}
 fun deleteClassroomData(requesterId:String,role:PlatformRole,classroom:Classroom)=role==PlatformRole.SCHOOL_ADMIN||requesterId in classroom.teacherIds
}

object PlatformAccessPolicy{
 fun permits(role:PlatformRole,permission:PlatformPermission,assigned:Boolean=false,owner:Boolean=false)=when(permission){
  PlatformPermission.CREATE_CLASS,PlatformPermission.MANAGE_ASSIGNMENT->role in setOf(PlatformRole.TEACHER,PlatformRole.SCHOOL_ADMIN)
  PlatformPermission.READ_ASSIGNED_EVIDENCE->role==PlatformRole.SCHOOL_ADMIN||(role==PlatformRole.TEACHER&&assigned)||role==PlatformRole.LEARNER&&owner
  PlatformPermission.READ_PRIVATE_NOTE->role==PlatformRole.LEARNER&&owner
  PlatformPermission.EXPORT_CLASS_REPORT->role==PlatformRole.SCHOOL_ADMIN||(role==PlatformRole.TEACHER&&assigned)
  PlatformPermission.DELETE_CLASSROOM->role==PlatformRole.SCHOOL_ADMIN
  PlatformPermission.RELEASE_CONTENT->role==PlatformRole.REVIEWER
 }
}

class ClassroomInvitationService(private val random:SecureRandom=SecureRandom()){
 fun issue(classroomId:String,expiresAt:Instant,maximumUses:Int=1):IssuedClassroomInvite{require(maximumUses in 1..100);val bytes=ByteArray(24).also(random::nextBytes);val token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);return IssuedClassroomInvite(token,ClassroomInvite(hash(token),classroomId,expiresAt,maximumUses))}
 fun accept(invite:ClassroomInvite,plaintextToken:String,now:Instant):ClassroomInvite{require(now.isBefore(invite.expiresAt)){"Invitation expired"};require(invite.used<invite.maximumUses){"Invitation exhausted"};require(MessageDigest.isEqual(invite.tokenHash.toByteArray(),hash(plaintextToken).toByteArray())){"Invalid invitation"};return invite.copy(used=invite.used+1)}
 private fun hash(token:String)=MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString(""){"%02x".format(it)}
}

object ReferenceTeacherFlows{
 val mathClass=Classroom("class-math-10","Class 10 Mathematics",SchoolSubject.MATHEMATICS,SchoolClassLevel.CLASS_10,setOf("teacher-math"),setOf("learner-1","learner-2","learner-3","learner-4","learner-5"))
 val mathAssignment=Assignment("assign-triangle-coordinate",mathClass.id,"Triangles and coordinate geometry",listOf("math-triangles","math-coordinate-geometry"),listOf("math-triangle-lab","math-coordinate-geometry"),null,null,listOf(DifferentiationRule("foundation",DifferentiationPath.FOUNDATION_BRIDGE,setOf("math-triangles"))))
 val physicsClass=Classroom("class-physics-12","Class 12 Physics",SchoolSubject.PHYSICS,SchoolClassLevel.CLASS_12,setOf("teacher-physics"),mathClass.learnerIds)
 val chemistryClass=Classroom("class-chemistry-12","Class 12 Chemistry",SchoolSubject.CHEMISTRY,SchoolClassLevel.CLASS_12,setOf("teacher-chemistry"),mathClass.learnerIds)
 val biologyClass=Classroom("class-biology-12","Class 12 Biology",SchoolSubject.BIOLOGY,SchoolClassLevel.CLASS_12,setOf("teacher-biology"),mathClass.learnerIds)
}
