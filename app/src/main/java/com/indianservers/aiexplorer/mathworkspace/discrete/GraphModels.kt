package com.indianservers.aiexplorer.mathworkspace.discrete

data class GraphVertex(val id: Int, val x: Double, val y: Double, val label: String = "")
data class GraphEdge(val from: Int, val to: Int, val weight: Double = 1.0)
data class GraphModel(val vertices: List<GraphVertex>, val edges: List<GraphEdge>, val directed: Boolean = false, val weighted: Boolean = false)
data class AlgorithmStep(val current: Int?, val visited: Set<Int>, val edges: Set<Pair<Int,Int>>, val frontier: List<Int>, val distances: Map<Int,Double> = emptyMap(), val note: String = "")
data class AlgorithmRun(val steps: List<AlgorithmStep>, val result: String)
