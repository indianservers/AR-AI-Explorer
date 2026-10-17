package com.indianservers.aiexplorer.assistant.workspace

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/** Small bundled TensorFlow-trained intent classifier, CPU-only, no network or student-data training. */
internal class TensorFlowTextModel(context: Context, directory:String, private val engineWords:Boolean=false) : AutoCloseable {
    private val assets=context.applicationContext.assets
    private val metadata=JSONObject(assets.open("$directory/metadata.json").bufferedReader().use { it.readText() })
    private val vocabulary=metadata.getJSONArray("vocabulary").let { a-> (0 until a.length()).associate { a.getString(it) to it } }
    private val labels=metadata.getJSONArray("labels").let { a-> List(a.length()) { a.getString(it) } }
    private val model=assets.open("$directory/model.tflite").use { it.readBytes() }.let { data -> ByteBuffer.allocateDirect(data.size).order(ByteOrder.nativeOrder()).apply { put(data); rewind() } }
    private val interpreter=Interpreter(model,Interpreter.Options().setNumThreads(1))
    init { require(interpreter.getInputTensor(0).shape().contentEquals(intArrayOf(1,vocabulary.size)));require(interpreter.getOutputTensor(0).shape().contentEquals(intArrayOf(1,labels.size))) }
    @Synchronized fun predict(text: String): GraphIntentPrediction {
        require(text.length<=2000)
        val words=Regex("[a-z]+|\\d+(?:\\.\\d+)?").findAll((if(engineWords) text.replace(Regex("(?i)\\b2d\\b"),"twod").replace(Regex("(?i)\\b3d\\b"),"threed") else text).lowercase(Locale.US)).map { if(it.value.first().isDigit()) "num" else it.value }.toList()
        val features=FloatArray(vocabulary.size)
        (words+words.zipWithNext { a,b -> "$a $b" }).forEach { word->vocabulary[word]?.let { features[it]=1f } }
        val output=arrayOf(FloatArray(labels.size));interpreter.run(arrayOf(features),output)
        val order=labels.indices.sortedByDescending { output[0][it] }
        return GraphIntentPrediction(labels[order[0]],output[0][order[0]],output[0][order[0]]-output[0][order[1]])
    }
    @Synchronized override fun close() { interpreter.close() }
 }

class TensorFlowGraphNlp(context:Context):GraphIntentClassifier,AutoCloseable {
    private val model=TensorFlowTextModel(context,"ruhi_nlp")
    override fun predict(text:String)=model.predict(text)
    override fun close()=model.close()
    companion object {
        @Synchronized fun install(context:Context) {
            val app=context.applicationContext
            if(GraphLanguageRouter.classifier==null) GraphLanguageRouter.classifier=object:GraphIntentClassifier {
                private val delegate by lazy { TensorFlowGraphNlp(app) }
                override fun predict(text:String)=delegate.predict(text)
            }
            if(RuhiLanguageServices.engineClassifier==null) RuhiLanguageServices.engineClassifier=object:RuhiEngineClassifier {
                private val delegate by lazy { TensorFlowEngineNlp(app) }
                override fun predict(text:String)=delegate.predict(text)
            }
            if(RuhiLanguageServices.dialogueClassifier==null) RuhiLanguageServices.dialogueClassifier=object:GraphIntentClassifier {
                private val delegate by lazy { TensorFlowTextModel(app,"ruhi_dialogue_nlp") }
                override fun predict(text:String)=delegate.predict(text)
            }
            if(RuhiLanguageServices.geometryClassifier==null) RuhiLanguageServices.geometryClassifier=object:GraphIntentClassifier {
                private val delegate by lazy { TensorFlowTextModel(app,"ruhi_geometry3d_nlp") }
                override fun predict(text:String)=delegate.predict(text)
            }
            if(RuhiLanguageServices.semanticClassifier==null) RuhiLanguageServices.semanticClassifier=object:GraphIntentClassifier {
                private val delegate by lazy { TensorFlowTextModel(app,"ruhi_semantic_nlp") }
                override fun predict(text:String)=delegate.predict(text)
            }
            if(RuhiLanguageServices.pointSlotClassifier==null) RuhiLanguageServices.pointSlotClassifier=object:RuhiPointSlotClassifier {
                private val delegate by lazy { TensorFlowTextModel(app,"ruhi_point_slots") }
                override fun predict(source:String)=RuhiPointSlotInput.tokens(source).map{token->val p=delegate.predict(token.features);RuhiPointSlot(p.intent,token.value,token.start,token.end,p.confidence,p.margin)}
            }
        }
    }
}
class TensorFlowEngineNlp(context:Context):RuhiEngineClassifier,AutoCloseable {
    private val model=TensorFlowTextModel(context,"ruhi_engine_nlp",engineWords=true)
    override fun predict(text:String):RuhiEnginePrediction {
        val result=model.predict(text)
        return RuhiEnginePrediction(RuhiEngine.entries.firstOrNull { it.name==result.intent },result.confidence,result.margin,result.intent=="CONTEXT")
    }
    override fun close()=model.close()
}
