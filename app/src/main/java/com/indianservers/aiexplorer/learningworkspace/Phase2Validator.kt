package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.assistant.contracts.AssistantCapability
import com.indianservers.aiexplorer.assistant.local.LocalLearningAssistantProvider
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog
import com.indianservers.aiexplorer.practice.VerifiedPracticeCatalog
import com.indianservers.aiexplorer.tutor.AuthoredSocraticFlows

data class Phase2ValidationReport(val errors:List<String>){val valid get()=errors.isEmpty()}
object Phase2Validator{
    fun validate():Phase2ValidationReport{
        val errors=mutableListOf<String>();val concepts=LearningIntelligenceCatalog.conceptIds();val local=LocalLearningAssistantProvider()
        if(AssistantCapability.SOCRATIC_TUTORING !in local.capabilities)errors+="Local assistant lacks Socratic tutoring."
        AuthoredSocraticFlows.flows.forEach{(id,flow)->if(id !in concepts)errors+="Tutor flow has unresolved concept $id.";if(flow.prompts.size<8)errors+="Tutor flow is incomplete for $id."}
        VerifiedPracticeCatalog.templates.forEach{if(it.conceptId !in concepts)errors+="Practice template ${it.id} has unresolved concept.";if(it.validatorIds.isEmpty())errors+="Practice template ${it.id} has no validator."}
        CrossSubjectJourneyCatalog.journeys.forEach{journey->if(!CrossSubjectJourneyEngine().preservesOwnership(journey))errors+="Journey ${journey.id} changes subject ownership."}
        if(ReferenceSweepModels.models.keys.none{"gas-law"==it})errors+="Gas-law sweep model missing."
        return Phase2ValidationReport(errors.distinct())
    }
}
