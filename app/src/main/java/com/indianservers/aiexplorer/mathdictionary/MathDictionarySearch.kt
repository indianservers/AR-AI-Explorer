package com.indianservers.aiexplorer.mathdictionary

object MathDictionarySearch {
    fun normalize(query: String): String =
        query.trim().lowercase().replace(Regex("\\s+"), " ")
}
