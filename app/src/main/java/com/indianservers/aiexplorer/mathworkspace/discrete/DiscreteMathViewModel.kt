package com.indianservers.aiexplorer.mathworkspace.discrete

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.mathworkspace.components.WorkspaceSheetStop

enum class DiscreteMode(val title:String){Graphs("Graphs"),Sets("Sets"),Relations("Relations"),Combinatorics("Combinatorics"),Logic("Logic")}
class DiscreteMathViewModel:ViewModel(){
    val mode=mutableStateOf(DiscreteMode.Graphs);val sheet=mutableStateOf(WorkspaceSheetStop.Peek);val graph=mutableStateOf(GraphAlgorithms.preset("Path"));val tool=mutableStateOf("Select");val directed=mutableStateOf(false);val weighted=mutableStateOf(false);val source=mutableIntStateOf(0);val target=mutableIntStateOf(1);val run=mutableStateOf<AlgorithmRun?>(null);val step=mutableIntStateOf(0);val playing=mutableStateOf(false)
    val setA=mutableStateOf("1,2,3,4");val setB=mutableStateOf("3,4,5");val universal=mutableStateOf("1,2,3,4,5,6");val setOp=mutableStateOf("Union")
    val relationElements=mutableStateOf("1,2,3");val relationPairs=mutableStateOf("(1,1),(2,2),(3,3),(1,2),(2,1)")
    val n=mutableStateOf("5");val r=mutableStateOf("2");val comboMode=mutableStateOf("Choose");val pascalRow=mutableStateOf("6")
    val inclusionA=mutableStateOf("8");val inclusionB=mutableStateOf("7");val intersection=mutableStateOf("3")
    val logic=mutableStateOf("P OR NOT P");val secondLogic=mutableStateOf("");val error=mutableStateOf("")
    fun setPreset(name:String){graph.value=GraphAlgorithms.preset(name);directed.value=false;weighted.value=graph.value.weighted;graph.value=graph.value.copy(directed=false)}
    fun toggleDirected(){directed.value=!directed.value;graph.value=graph.value.copy(directed=directed.value)}
    fun toggleWeighted(){weighted.value=!weighted.value;graph.value=graph.value.copy(weighted=weighted.value)}
    fun startAlgorithm(name:String){playing.value=false;error.value="";run.value=runCatching{when(name){"BFS"->GraphAlgorithms.bfs(graph.value,source.intValue);"DFS"->GraphAlgorithms.dfs(graph.value,source.intValue);"Dijkstra"->GraphAlgorithms.dijkstra(graph.value,source.intValue,target.intValue);else->GraphAlgorithms.mst(graph.value)}}.onFailure{error.value=it.message.orEmpty()}.getOrNull();step.intValue=0}
}
