package com.indianservers.aiexplorer
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.indianservers.aiexplorer.curriculum.SchoolSubject
import com.indianservers.aiexplorer.learningintelligence.model.*
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.learningintelligence.ui.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class LearningIntelligenceUiTest{
 @get:Rule val compose=createComposeRule()
 @Test fun diagnosticShowsConfidenceAndCanSubmit(){val q=LearningIntelligenceCatalog.diagnosticQuestions.first{it.subject==SchoolSubject.MATHEMATICS};compose.setContent{MaterialTheme{DiagnosticOnboardingScreen(setOf(SchoolSubject.MATHEMATICS),q,1,0,{}, {_,_->}, {},null,{})}};compose.onNodeWithText("Mathematics").performClick();compose.onNodeWithText(q.prompt).assertIsDisplayed();compose.onNodeWithText("How confident are you?").assertIsDisplayed();compose.onNodeWithText(q.options.first()).performClick();compose.onNodeWithText("Submit").assertIsEnabled()}
 @Test fun hintAndErrorBookSurfacesExposeActions(){val c=LearningIntelligenceCatalog.concepts.first();val entry=ErrorBookEntry("e",c.conceptId,c.curriculumNodeId,"q","wrong",emptyList(),null,LearnerErrorType.CONCEPTUAL,c.misconceptions.first().id,LearnerConfidence.VERY_SURE,HintLevel.CONCEPT_CUE,listOf("Use the defining relationship."),c.activityId,Instant.now(),ErrorBookStatus.RETRY_SCHEDULED);compose.setContent{MaterialTheme{PersonalErrorBookScreen(listOf(entry),{},{})}};compose.onNodeWithText("Personal error book").assertIsDisplayed();compose.onNodeWithText("Retry").assertHasClickAction();compose.onNodeWithText("Mark resolved").assertHasClickAction()}
 @Test fun recommendationShowsReasonAndOpensConcept(){val c=LearningIntelligenceCatalog.concepts.first();val recommendation=LearningRecommendation(c.conceptId,c.activityId,RecommendationReason.MISSING_PREREQUISITE,"A required bridge is missing.",8,100.0);compose.setContent{MaterialTheme{LearningIntelligenceDashboard(recommendation,emptyList(),emptyList(),emptyList()) {}}};compose.onNodeWithText("Continue learning").assertHasClickAction();compose.onNodeWithText("A required bridge is missing.",substring=true).assertIsDisplayed()}
 @Test fun hintLadderTracksIncreasingDisclosure(){val c=LearningIntelligenceCatalog.concepts.first();val selection=HintSelection(c.hints[3],EvidenceIndependence.PARTIAL_WORKING,"Escalated after repeated attempts.");compose.setContent{MaterialTheme{HintLadderPanel(selection){}}};compose.onNodeWithText("PARTIAL STEP").assertIsDisplayed();compose.onNodeWithText("Need a deeper hint").assertHasClickAction()}
 @Test fun dueReviewExposesRetrievalAction(){val c=LearningIntelligenceCatalog.concepts.first();val review=ScheduledReview(c.conceptId,Instant.now(),ReviewUrgency.DUE_TODAY,"Delayed retrieval is due.",MasteryEvidenceType.DELAYED_RETRIEVAL);compose.setContent{MaterialTheme{ReviewQueuePanel(listOf(review)) {}}};compose.onNodeWithText(c.conceptId).assertHasClickAction();compose.onNodeWithText("Delayed retrieval is due.",substring=true).assertIsDisplayed()}
}
