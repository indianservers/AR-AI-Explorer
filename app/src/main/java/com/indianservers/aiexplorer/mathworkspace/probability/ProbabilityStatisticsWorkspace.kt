package com.indianservers.aiexplorer.mathworkspace.probability

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.mathworkspace.MathWorkspaceShell
import com.indianservers.aiexplorer.mathworkspace.components.*
import com.indianservers.aiexplorer.mathworkspace.components.MathWorkspacePalette as P
import kotlin.math.*

@Composable
fun ProbabilityStatisticsWorkspace(viewModel: ProbabilityStatisticsViewModel, onBack: () -> Unit) {
    val vm = viewModel; val kind by vm.kind; val mode by vm.mode; val p = vm.parameters()
    val valid = ProbabilityMath.valid(kind,p)
    val lo = vm.lower.value.toDoubleOrNull(); val hi = vm.upper.value.toDoubleOrNull()
    val result = if (valid && lo != null && hi != null) ProbabilityMath.interval(kind,lo,hi,p) else Double.NaN
    var menu by remember { mutableStateOf(false) }; var properties by remember { mutableStateOf(false) }
    val concise = when(kind) {
        DistributionKind.Normal -> "Normal · μ ${fmt(p.first)} · σ ${fmt(p.second)}"
        DistributionKind.Uniform -> "Uniform · ${fmt(p.first)} to ${fmt(p.second)}"
        DistributionKind.Exponential -> "Exponential · λ ${fmt(p.first)}"
        DistributionKind.Binomial -> "Binomial · n ${p.trials} · p ${fmt(p.probability)}"
        DistributionKind.Poisson -> "Poisson · λ ${fmt(p.first)}"
    }
    MathWorkspaceShell(
        title = "Probability & Statistics Lab", subtitle = "Distributions · sampling · simulation", onBack = onBack,
        sheetTitle = if (vm.sheet.value == WorkspaceSheetStop.Collapsed) concise else if (mode == ProbabilityMode.Distributions) "$kind Distribution" else mode.title,
        sheetStop = vm.sheet.value, onSheetStopChange = { vm.sheet.value = it },
        topBarTrailing = { WorkspaceSegmentedControl(ProbabilityMode.entries.map { it.title }, mode.ordinal) { vm.mode.value = ProbabilityMode.entries[it] } },
        canvas = { DistributionRenderer(vm) },
        tools = {
            Row(Modifier.align(Alignment.TopCenter).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    WorkspaceAction(kind.title, "Choose distribution", { menu = true })
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DistributionKind.entries.forEach { item -> DropdownMenuItem(text = { Text(item.title) }, onClick = { vm.kind.value = item; menu = false; vm.resetView(); vm.inspectedX.value = null }) }
                    }
                }
                WorkspaceAction("−", "Zoom out", { vm.viewport.value = vm.viewport.value.copy(scaleX = vm.viewport.value.scaleX / 1.25, scaleY = vm.viewport.value.scaleY / 1.25) })
                WorkspaceAction("+", "Zoom in", { vm.viewport.value = vm.viewport.value.copy(scaleX = vm.viewport.value.scaleX * 1.25, scaleY = vm.viewport.value.scaleY * 1.25) })
                WorkspaceAction("◎", "Reset view", vm::resetView)
            }
            vm.inspectedX.value?.let { x ->
                val y = if (kind.discrete) ProbabilityMath.pmf(kind,round(x),p) else ProbabilityMath.pdf(kind,x,p)
                Column(Modifier.align(Alignment.TopStart).padding(start=12.dp, top=60.dp).clip(RoundedCornerShape(10.dp)).background(P.panel.copy(alpha=.94f)).padding(9.dp)) {
                    Text("x = ${fmt(x)}", color=P.ink, fontSize=12.sp, fontWeight=FontWeight.SemiBold)
                    Text(if(kind.discrete) "PMF = ${fmt(y)}" else "PDF = ${fmt(y)} · CDF = ${fmt(ProbabilityMath.cdf(kind,x,p))}", color=P.muted,fontSize=11.sp)
                }
            }
        },
        sheet = {
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (mode == ProbabilityMode.Distributions) {
                    if (!valid) Text(validation(kind), color=P.red, fontSize=11.sp)
                    when(kind) {
                        DistributionKind.Normal -> { InputRow(listOf("μ" to vm.first.value, "σ" to vm.second.value), listOf({vm.first.value=it},{vm.second.value=it}), listOf("Mean","Standard deviation")) }
                        DistributionKind.Uniform -> InputRow(listOf("Minimum" to vm.first.value,"Maximum" to vm.second.value),listOf({vm.first.value=it},{vm.second.value=it}),listOf("Minimum a","Maximum b"))
                        DistributionKind.Exponential, DistributionKind.Poisson -> InputRow(listOf("λ" to vm.first.value),listOf({vm.first.value=it}),listOf("Rate λ"))
                        DistributionKind.Binomial -> InputRow(listOf("n" to vm.trials.value,"p" to vm.probability.value),listOf({vm.trials.value=it},{vm.probability.value=it}),listOf("Trials n","Success p"))
                    }
                    WorkspaceSectionTitle(if(kind.discrete) "Probability query" else "Interval · P(a ≤ X ≤ b)")
                    InputRow(listOf("a" to vm.lower.value,"b / k" to vm.upper.value),listOf({vm.lower.value=it},{vm.upper.value=it}),listOf("Lower bound","Upper bound / k"))
                    val q = if (valid && result.isFinite()) fmt(result) else "—"
                    Text(if(kind.discrete) "P(${fmt(lo?:0.0)} ≤ X ≤ ${fmt(hi?:0.0)}) = $q" else "P(a ≤ X ≤ b) = $q", color=P.blue,fontSize=18.sp,fontWeight=FontWeight.Bold)
                    if(kind.discrete && lo != null && hi != null && valid) Text("P(X = ${fmt(lo)}) = ${fmt(ProbabilityMath.pmf(kind,lo,p))}     P(X ≤ ${fmt(hi)}) = ${fmt(ProbabilityMath.cdf(kind,hi,p))}", color=P.muted,fontSize=11.sp)
                    WorkspaceSectionTitle("Properties", { Text(if(properties) "⌃" else "⌄", Modifier.clickable { properties=!properties },color=P.blue) })
                    if(properties && valid) Text("Mean ${fmt(ProbabilityMath.mean(kind,p))}   ·   Variance ${fmt(ProbabilityMath.variance(kind,p))}   ·   SD ${fmt(sqrt(ProbabilityMath.variance(kind,p)))}", color=P.muted,fontSize=12.sp)
                } else {
                    Text(if(mode==ProbabilityMode.Sampling) "Generate observations from the selected distribution." else "Repeat samples and inspect the sampling distribution of the mean.",color=P.muted,fontSize=12.sp)
                    InputRow(listOf("Sample size" to vm.sampleSize.value,"Seed" to vm.seed.value),listOf({vm.sampleSize.value=it},{vm.seed.value=it}),listOf("Observations per run","Deterministic seed"))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        WorkspaceAction("Generate sample", "Generate seeded sample", { vm.sample(false) }, enabled=valid)
                        if(mode==ProbabilityMode.Simulation) WorkspaceAction("200 repetitions", "Run repeated sampling", {vm.sample(true)}, enabled=valid)
                    }
                    val values = if(mode==ProbabilityMode.Simulation && vm.simulationMeans.value.isNotEmpty()) vm.simulationMeans.value else vm.samples.value
                    Text(if(values.isEmpty()) "No sample generated" else "${values.size} generated · empirical mean ${fmt(values.average())} · SD ${fmt(sqrt(values.map{(it-values.average()).pow(2)}.average()))}",color=P.blue,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                }
            }
        }
    )
}

@Composable private fun InputRow(fields: List<Pair<String,String>>, setters: List<(String)->Unit>, descriptions: List<String>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp), verticalAlignment=Alignment.CenterVertically) {
        fields.forEachIndexed { i, field -> NumericField(field.first,field.second,setters[i],Modifier.weight(1f).semantics { contentDescription = descriptions[i] }) }
    }
}
private fun fmt(v: Double) = if(!v.isFinite()) "—" else if(abs(v)>=1e6 || (abs(v)<1e-4 && v!=0.0)) "%.4e".format(v) else "%.6g".format(v)
private fun validation(kind: DistributionKind) = when(kind) {
    DistributionKind.Normal -> "Enter finite μ and σ > 0."; DistributionKind.Uniform -> "Enter finite bounds with maximum > minimum."
    DistributionKind.Exponential, DistributionKind.Poisson -> "Enter a finite rate λ > 0."; DistributionKind.Binomial -> "Enter n ≥ 0 and probability from 0 to 1."
}
