package com.indianservers.aiexplorer.phase3.ar

import com.indianservers.aiexplorer.connectedlearning.ScientificReviewStatus
import com.indianservers.aiexplorer.learningintelligence.reference.LearningIntelligenceCatalog

enum class ModelInteraction{ROTATE,SCALE,MOVE,SELECT,LABEL,MEASURE,CHANGE_PARAMETER,REPLAY}
data class ArLearningAsset(val id:String,val conceptId:String,val localAssetPath:String?,val supportedInteractions:Set<ModelInteraction>,val fallbackActivityId:String,val reviewStatus:ScientificReviewStatus,val deviceRequirements:List<String>)
data class ArCapabilityResult(val available:Boolean,val selectedActivityId:String,val reason:String)
object ArLearningRegistry{
 val assets=listOf(ArLearningAsset("ar-triangle","math-triangles",null,setOf(ModelInteraction.ROTATE,ModelInteraction.MEASURE),"math-triangle-lab",ScientificReviewStatus.Verified,listOf("ARCore supported","camera permission while in use")),ArLearningAsset("ar-circuit","physics-electric-circuits",null,setOf(ModelInteraction.SELECT,ModelInteraction.CHANGE_PARAMETER),"physics-electric-circuits",ScientificReviewStatus.Verified,listOf("ARCore supported")),ArLearningAsset("ar-atom","chemistry-atomic-number-mass-number",null,setOf(ModelInteraction.ROTATE,ModelInteraction.LABEL),"chemistry-atom-builder",ScientificReviewStatus.Verified,listOf("ARCore supported")),ArLearningAsset("ar-cell","biology-cell-structure",null,setOf(ModelInteraction.ROTATE,ModelInteraction.LABEL),"biology-cell-explorer",ScientificReviewStatus.Verified,listOf("ARCore supported")))
 fun validate()=assets.flatMap{a->buildList{if(a.conceptId !in LearningIntelligenceCatalog.conceptIds())add("Unknown concept ${a.conceptId}");val concept=LearningIntelligenceCatalog.concepts.singleOrNull{it.conceptId==a.conceptId};if(concept?.activityId!=a.fallbackActivityId)add("AR asset ${a.id} lacks the subject-owned non-AR fallback.")}}
 fun resolve(asset:ArLearningAsset,arCoreSupported:Boolean,cameraGranted:Boolean)=if(arCoreSupported&&cameraGranted)ArCapabilityResult(true,asset.id,"Optional AR is available.")else ArCapabilityResult(false,asset.fallbackActivityId,"Using the complete non-AR activity; no learning content is lost.")
}
