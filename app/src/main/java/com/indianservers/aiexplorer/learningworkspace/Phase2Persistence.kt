package com.indianservers.aiexplorer.learningworkspace

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.indianservers.aiexplorer.assistant.contracts.AssistantResponse
import com.indianservers.aiexplorer.assistant.contracts.ExplanationStyle
import com.indianservers.aiexplorer.assistant.privacy.ProviderSettings
import kotlinx.coroutines.flow.first
import java.util.Base64

private val Context.phase2DataStore by preferencesDataStore("phase2_local_workspace")
data class Phase2Record(val type:String,val id:String,val fields:Map<String,String>)
data class Phase2Snapshot(val schemaVersion:Int=1,val records:List<Phase2Record> = emptyList())
interface Phase2Persistence { suspend fun load():Phase2Snapshot; suspend fun save(snapshot:Phase2Snapshot) }

class DataStorePhase2Persistence(private val context:Context):Phase2Persistence{
    private val key=stringPreferencesKey("snapshot_v1")
    override suspend fun load():Phase2Snapshot=decode(context.phase2DataStore.data.first()[key].orEmpty())
    override suspend fun save(snapshot:Phase2Snapshot){context.phase2DataStore.edit{it[key]=encode(snapshot)}}
    private fun encode(snapshot:Phase2Snapshot)=snapshot.records.joinToString("\n"){r->listOf(r.type,r.id,r.fields.entries.joinToString(";"){"${b64(it.key)}:${b64(it.value)}"}).joinToString("|"){b64(it)}}
    private fun decode(raw:String)=Phase2Snapshot(records=raw.lines().filter{it.isNotBlank()}.mapNotNull{line->runCatching{val p=line.split('|').map(::unb64);val fields=p.getOrElse(2){""}.split(';').filter{it.contains(':')}.associate{pair->val i=pair.indexOf(':');unb64(pair.substring(0,i)) to unb64(pair.substring(i+1))};Phase2Record(p[0],p[1],fields)}.getOrNull()})
    private fun b64(s:String)=Base64.getUrlEncoder().withoutPadding().encodeToString(s.toByteArray())
    private fun unb64(s:String)=String(Base64.getUrlDecoder().decode(s))
}

object Phase2SnapshotMapper{
    fun settings(snapshot:Phase2Snapshot):ProviderSettings{val f=snapshot.records.firstOrNull{it.type=="provider-settings"}?.fields?:return ProviderSettings();val consent=com.indianservers.aiexplorer.assistant.privacy.AssistantConsent(f["cloud"].toBoolean(),f["camera"].toBoolean(),f["voice"].toBoolean(),f["steps"].toBoolean(),f["categories"].orEmpty().split(',').mapNotNull{runCatching{com.indianservers.aiexplorer.assistant.privacy.SharedDataCategory.valueOf(it)}.getOrNull()}.toSet());return ProviderSettings(runCatching{com.indianservers.aiexplorer.assistant.privacy.ProviderKind.valueOf(f["provider"].orEmpty())}.getOrDefault(com.indianservers.aiexplorer.assistant.privacy.ProviderKind.NONE),consent)}
    fun snapshot(
        tutorResponses:List<Pair<String,AssistantResponse>> = emptyList(), notebooks:List<ExperimentNotebookEntry> = emptyList(), replays:List<SimulationReplay> = emptyList(), pinnedStates:List<String> = emptyList(), sweeps:List<ParameterSweepResult> = emptyList(), sketches:Map<String,List<SketchPoint>> = emptyMap(), designs:List<StudentExperimentDesign> = emptyList(), style:ExplanationStyle = ExplanationStyle.INTUITIVE, settings:ProviderSettings = ProviderSettings()
    ):Phase2Snapshot=Phase2Snapshot(records=buildList{
        add(Phase2Record("preference","explanation-style",mapOf("value" to style.name)))
        add(Phase2Record("provider-settings","selected",mapOf("provider" to settings.selectedProvider.name,"cloud" to settings.consent.cloudEnabled.toString(),"camera" to settings.consent.cameraMayBeSent.toString(),"voice" to settings.consent.voiceMayBeSent.toString(),"steps" to settings.consent.learnerStepsMayBeSent.toString(),"categories" to settings.consent.allowedCategories.joinToString(","){it.name})))
        tutorResponses.forEach{(id,r)->add(Phase2Record("tutor-reference",id,mapOf("references" to r.groundingReferences.joinToString(","),"status" to r.verificationStatus.name,"provider" to r.providerId.orEmpty())))}
        notebooks.forEach{n->add(Phase2Record("notebook",n.id,mapOf("subject" to n.subject.name,"concept" to n.conceptId,"activity" to n.activityId,"title" to n.title,"question" to n.question.orEmpty(),"hypothesis" to n.hypothesis.orEmpty(),"prediction" to n.prediction.orEmpty(),"observations" to n.observations.joinToString("\u001f"),"graphs" to n.graphReferences.joinToString(","),"conclusion" to n.conclusion.orEmpty(),"states" to n.savedStateIds.joinToString(","),"updated" to n.updatedAt.toString())))}
        replays.forEach{r->add(Phase2Record("replay",r.simulationId,mapOf("initial" to r.initialState.id,"version" to r.initialState.simulationVersion.toString(),"events" to r.events.joinToString("\u001f"){"${it.atMs},${it.type},${it.targetId.orEmpty()},${it.numericValue?:""},${it.textValue.orEmpty()}"},"snapshots" to r.snapshots.joinToString(","){it.id},"duration" to r.durationMs.toString())))}
        pinnedStates.forEach{add(Phase2Record("pinned-state",it,emptyMap()))}
        sweeps.forEach{s->add(Phase2Record("parameter-sweep","${s.definition.simulationId}-${s.definition.independentVariableId}",mapOf("start" to s.definition.startValue.toString(),"end" to s.definition.endValue.toString(),"count" to s.definition.sampleCount.toString(),"outputs" to s.definition.measuredOutputIds.joinToString(","),"provenance" to s.provenance)))}
        sketches.forEach{(id,points)->add(Phase2Record("graph-sketch",id,mapOf("points" to points.joinToString(";"){"${it.x},${it.y}"})))}
        designs.forEach{d->add(Phase2Record("experiment-design",d.title,mapOf("question" to d.question,"hypothesis" to d.hypothesis.orEmpty(),"independent" to d.independentVariable?.id.orEmpty(),"dependent" to d.dependentVariables.joinToString(","){it.id},"controls" to d.controls.joinToString(","){it.id},"apparatus" to d.apparatusIds.joinToString(","),"success" to d.successCriteria.joinToString("\u001f"),"safety" to d.safetyAcknowledgements.joinToString("\u001f"))))}
    })
}
