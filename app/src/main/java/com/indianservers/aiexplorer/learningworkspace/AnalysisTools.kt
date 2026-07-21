package com.indianservers.aiexplorer.learningworkspace

import com.indianservers.aiexplorer.assistant.contracts.SimulationStateSnapshot
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class StateComparisonEngine {
    fun compare(first: SimulationStateSnapshot, second: SimulationStateSnapshot, inputIds: Set<String>, mode: ComparisonMode = ComparisonMode.SIDE_BY_SIDE): StateComparison {
        require(first.simulationId == second.simulationId)
        val all = first.values.keys + second.values.keys
        val differences = all.map { id -> val a=first.values[id];val b=second.values[id];StateDifference(id,a,b,second.units[id]?:first.units[id],if(a!=null&&b!=null)b-a else null) }
        val changed = differences.filter { it.firstValue == null || it.secondValue == null || abs(it.delta ?: 0.0) > 1e-9 }
        val unchanged = differences.filterNot { it in changed }.map { it.id }
        return StateComparison(first.id,second.id,changed.filter{it.id in inputIds},changed.filter{it.id !in inputIds},unchanged,mode,changed.map{"${it.id} changed from ${it.firstValue} to ${it.secondValue}; delta ${it.delta}."})
    }
}

class UncertaintyLaboratory {
    fun analyse(dataset: MeasurementDataset): MeasurementAnalysis {
        if (dataset.values.isEmpty()) return MeasurementAnalysis(null,null,null,null,null,null,emptyList(),null,listOf("Add readings before analysis."))
        val sorted=dataset.values.sorted();val mean=dataset.values.average();val median=if(sorted.size%2==1)sorted[sorted.size/2] else (sorted[sorted.size/2-1]+sorted[sorted.size/2])/2
        val range=sorted.last()-sorted.first();val sd=sqrt(dataset.values.sumOf{(it-mean).pow(2)}/dataset.values.size)
        val error=dataset.expectedValue?.takeIf{abs(it)>1e-12}?.let{abs(mean-it)/abs(it)*100}
        val uncertainty=dataset.instrumentResolution?.let{resolution->(resolution/2)/abs(mean).coerceAtLeast(1e-12)*100}
        val outliers=if(dataset.values.size<4||sd==0.0)emptyList() else dataset.values.mapIndexedNotNull{i,v->i.takeIf{abs(v-mean)>2.5*sd}}
        val halfBand=dataset.instrumentResolution?.div(2) ?: sd
        return MeasurementAnalysis(mean,median,range,sd,error,uncertainty,outliers,(mean-halfBand)..(mean+halfBand),buildList{add("Outliers are flagged for investigation, never deleted automatically.");if(dataset.values.size<3)add("Use repeated readings to distinguish spread from a single observation.");if(dataset.instrumentResolution==null)add("Add instrument resolution to estimate a measurement uncertainty band.")})
    }
}

class StudentExperimentValidator {
    fun validate(design: StudentExperimentDesign): ExperimentDesignValidation {
        val issues=buildList {
            if(design.title.isBlank()||design.question.isBlank())add("State a clear, testable question.")
            if(design.independentVariable==null)add("Select one clear independent variable.")
            if(design.additionalIndependentVariables.isNotEmpty())add("Two independent variables are changing.")
            if(design.dependentVariables.isEmpty())add("Select at least one measurable dependent variable.")
            if(design.dependentVariables.any{it.unit.isNullOrBlank()})add("A dependent variable has no unit.")
            if(design.independentVariable?.range==null)add("The independent variable needs a valid model range.")
            if(design.measurementPlan.sumOf{it.sampleCount}<3)add("One or two readings are not enough to identify a trend.")
            if(design.controls.isEmpty())add("Identify at least one controlled variable.")
            if(design.successCriteria.isEmpty())add("Define meaningful success criteria before running the experiment.")
            if(design.safetyAcknowledgements.isEmpty())add("Acknowledge the activity safety guidance.")
        }
        return ExperimentDesignValidation(issues.isEmpty(),issues)
    }
}

class ProofStudioEngine {
    fun evaluate(taskType: ProofTaskType, expected: List<ProofStep>, learner: List<ProofStep>, requiredInvariants: Set<String> = emptySet()): ProofEvaluation {
        var preserved=requiredInvariants.toMutableSet();val feedback=mutableListOf<String>();var invalid:Int?=null
        expected.forEachIndexed{index,e->
            val actual=learner.getOrNull(index);val valid=actual!=null&&normal(actual.statement)==normal(e.statement)&&(!requiresReason(taskType)||!actual.reason.isNullOrBlank())
            if(!valid&&invalid==null)invalid=index
            if(actual!=null)preserved.retainAll(actual.invariantIds+preserved.filter{it in actual.invariantIds})
            feedback += if(valid)"Checkpoint ${index+1}: statement and reason are connected." else "Checkpoint ${index+1}: justify the transformation; matching the final claim is not enough."
        }
        return ProofEvaluation(invalid==null&&learner.size==expected.size,invalid,feedback,preserved)
    }
    private fun requiresReason(type:ProofTaskType)=type!=ProofTaskType.COUNTEREXAMPLE
    private fun normal(s:String)=s.lowercase().replace(Regex("\\s+"),"")
}

class GraphSketchEvaluator {
    fun evaluate(predicted: GraphStructure, target: GraphStructure): GraphFeatureEvaluation {
        val intercepts=if(target.intercepts.isEmpty())null else sameValues(predicted.intercepts,target.intercepts,.15)
        val slopes=if(target.slopeSigns.isEmpty())null else predicted.slopeSigns==target.slopeSigns
        val turns=target.turningPoints?.let{predicted.turningPoints==it};val asymptotes=if(target.asymptotes.isEmpty())null else sameValues(predicted.asymptotes,target.asymptotes,.15)
        val domain=target.domain?.let{rangesClose(predicted.domain,it)};val range=target.range?.let{rangesClose(predicted.range,it)};val trend=predicted.trend==target.trend
        val features=listOfNotNull(intercepts,slopes,turns,asymptotes,domain,range,trend);val score=features.count{it}.toDouble()/features.size.coerceAtLeast(1)
        return GraphFeatureEvaluation(intercepts,trend,slopes,turns,asymptotes,domain,range,score)
    }
    fun structureFromPoints(points:List<SketchPoint>):GraphStructure{require(points.size>=2);val sorted=points.sortedBy{it.x};val slopeSigns=sorted.zipWithNext().map{(a,b)->(b.y-a.y).compareTo(0.0)}.filterIndexed{i,_->i==0||i==sorted.lastIndex-1}.distinct();val intercepts=sorted.zipWithNext().mapNotNull{(a,b)->if(a.y==0.0)a.x else if(a.y*b.y<0)a.x+(0-a.y)*(b.x-a.x)/(b.y-a.y) else null};val turns=sorted.windowed(3).count{(a,b,c)->(b.y-a.y)*(c.y-b.y)<0};return GraphStructure(intercepts,sorted.last().y.compareTo(sorted.first().y),slopeSigns,turns,domain=sorted.first().x..sorted.last().x,range=sorted.minOf{it.y}..sorted.maxOf{it.y})}
    private fun sameValues(a:List<Double>,b:List<Double>,t:Double)=a.size==b.size&&a.sorted().zip(b.sorted()).all{abs(it.first-it.second)<=t}
    private fun rangesClose(a:ClosedFloatingPointRange<Double>?,b:ClosedFloatingPointRange<Double>)=a!=null&&abs(a.start-b.start)<=.15&&abs(a.endInclusive-b.endInclusive)<=.15
}

class DataAnalysisWorkspace {
    fun analyse(rows: List<DataRow>, xColumn: String?, yColumn: String?, complexity: DataComplexity): DataAnalysisResult {
        val columns=rows.flatMap{it.values.keys}.toSet();val means=columns.associateWith{c->rows.mapNotNull{it.values[c]}.averageOrNull()?:Double.NaN};val medians=columns.associateWith{c->rows.mapNotNull{it.values[c]}.sorted().medianOrNaN()};val ranges=columns.associateWith{c->rows.mapNotNull{it.values[c]}.let{if(it.isEmpty())Double.NaN else it.max()-it.min()}};val sds=columns.associateWith{c->val v=rows.mapNotNull{it.values[c]};val m=v.averageOrNull()?:Double.NaN;if(v.isEmpty())Double.NaN else sqrt(v.sumOf{(it-m).pow(2)}/v.size)}
        val pairs=if(xColumn!=null&&yColumn!=null)rows.mapNotNull{r->r.values[xColumn]?.let{x->r.values[yColumn]?.let{y->x to y}}} else emptyList();val mx=pairs.map{it.first}.averageOrNull();val my=pairs.map{it.second}.averageOrNull();val slope=if(pairs.size>=2&&mx!=null&&my!=null){val den=pairs.sumOf{(it.first-mx).pow(2)};if(den==0.0)null else pairs.sumOf{(it.first-mx)*(it.second-my)}/den}else null;val intercept=if(slope!=null&&mx!=null&&my!=null)my-slope*mx else null;val residuals=if(slope!=null&&intercept!=null)pairs.map{it.second-(intercept+slope*it.first)}else emptyList()
        val tools=when(complexity){DataComplexity.CLASSES_7_8->setOf("table","sort","bar","mean","range");DataComplexity.CLASSES_9_10->setOf("table","filter","scatter","histogram","line","mean","median","best-fit");DataComplexity.CLASSES_11_12->setOf("scatter","regression","residuals","uncertainty","interpolation");DataComplexity.ADVANCED->setOf("regression","residuals","standard-deviation","uncertainty","export")}
        return DataAnalysisResult(rows,means,medians,ranges,sds,slope,intercept,residuals,tools)
    }
    private fun List<Double>.averageOrNull()=takeIf{it.isNotEmpty()}?.average()
    private fun List<Double>.medianOrNaN()=if(isEmpty())Double.NaN else if(size%2==1)this[size/2] else (this[size/2-1]+this[size/2])/2
}
