package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.assistant.contracts.SimulationStateSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.sin

interface RepresentationCoordinator<S> { val state: StateFlow<S>; fun dispatch(action: RepresentationAction) }
fun interface CoordinatedStateCalculator { fun calculate(snapshot: SimulationStateSnapshot): CoordinatedConceptState }

class LocalRepresentationCoordinator(initial: SimulationStateSnapshot, private val calculator: CoordinatedStateCalculator) : RepresentationCoordinator<CoordinatedConceptState> {
    private val initialSnapshot = initial
    private val mutable = MutableStateFlow(calculator.calculate(initial))
    override val state = mutable.asStateFlow()
    override fun dispatch(action: RepresentationAction) {
        val current = mutable.value.snapshot
        val next = when (action) {
            is RepresentationAction.SetValue -> current.copy(values = current.values + (action.id to action.value))
            is RepresentationAction.SetMode -> current.copy(labels = current.labels + (action.id to action.value))
            is RepresentationAction.Restore -> action.snapshot
            RepresentationAction.Reset -> initialSnapshot
        }
        mutable.value = calculator.calculate(next)
    }
}

object ReferenceRepresentationCalculators {
    val gasLaw = CoordinatedStateCalculator { s ->
        val n=s.values["moles"]?:1.0;val t=s.values["temperature"]?:300.0;val v=s.values["volume"]?:10.0;val p=8.314*n*t/v
        val snapshot=s.copy(values=s.values+mapOf("pressure" to p),units=s.units+mapOf("pressure" to "kPa"))
        CoordinatedConceptState(snapshot,"PV = nRT · $p × $v = $n × 8.314 × $t",listOf(v to p),listOf(mapOf("P" to "%.2f kPa".format(p),"V" to "$v L","T" to "$t K")),"At fixed amount, the displayed pressure follows temperature divided by volume.")
    }
    val trigonometricGraph = CoordinatedStateCalculator { s ->
        val amplitude=s.values["amplitude"]?:1.0;val frequency=s.values["frequency"]?:1.0;val phase=s.values["phase"]?:0.0
        val points=(0..40).map{val x=it*2*PI/40;x to amplitude*sin(frequency*x+phase)}
        CoordinatedConceptState(s,"y = ${"%.2f".format(amplitude)} sin(${"%.2f".format(frequency)}x + ${"%.2f".format(phase)})",points,points.filterIndexed{i,_->i%10==0}.map{mapOf("x" to "%.2f".format(it.first),"y" to "%.2f".format(it.second))},"Amplitude controls height; frequency controls cycles; phase moves the graph horizontally.")
    }
}

fun interface SweepModel { fun evaluate(inputId: String, input: Double, fixed: Map<String, Double>): Map<String, Double> }
class ParameterSweepEngine(private val models: Map<String, SweepModel>) {
    fun run(definition: ParameterSweepDefinition): ParameterSweepResult {
        require(definition.sampleCount in 2..200) { "sample count must be between 2 and 200" }
        require(definition.startValue.isFinite() && definition.endValue.isFinite() && definition.startValue < definition.endValue) { "invalid sweep range" }
        val model = models[definition.simulationId] ?: error("Unknown simulation model")
        val step=(definition.endValue-definition.startValue)/(definition.sampleCount-1)
        val samples=(0 until definition.sampleCount).map { i -> val x=definition.startValue+i*step;SweepSample(x,model.evaluate(definition.independentVariableId,x,definition.fixedValues).filterKeys{it in definition.measuredOutputIds}) }
        require(samples.all { it.outputs.values.all(Double::isFinite) }) { "undefined model output" }
        val minima=definition.measuredOutputIds.associateWith{key->samples.minOf{it.outputs[key]?:Double.POSITIVE_INFINITY}}
        val maxima=definition.measuredOutputIds.associateWith{key->samples.maxOf{it.outputs[key]?:Double.NEGATIVE_INFINITY}}
        val trend=definition.measuredOutputIds.joinToString { key -> val first=samples.first().outputs[key]?:0.0;val last=samples.last().outputs[key]?:0.0;"$key ${if(last>first)"increases" else if(last<first)"decreases" else "stays constant"}" }
        return ParameterSweepResult(definition,samples,trend,minima,maxima,emptyList(),listOf("Values come from the selected simplified model and valid domain."))
    }
}

object ReferenceSweepModels {
    val models = mapOf(
        "motion-graph" to SweepModel { id,x,f->mapOf("acceleration" to if(id=="force")x/(f["mass"]?:1.0) else x) },
        "wave" to SweepModel { id,x,f->mapOf("wavelength" to if(id=="frequency")(f["speed"]?:340.0)/x else x) },
        "gas-law" to SweepModel { id,x,f->mapOf("pressure" to if(id=="volume")8.314*(f["moles"]?:1.0)*(f["temperature"]?:300.0)/x else x) },
        "trigonometric-graph" to SweepModel { _,x,_->mapOf("period" to 2*PI/x) },
        "electric-circuit" to SweepModel { id,x,f->mapOf("current" to if(id=="resistance")(f["voltage"]?:12.0)/x else x) },
    )
}
