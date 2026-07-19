package com.indianservers.aiexplorer.biology.navigation

sealed class BiologyRoute(val route: String) {
    data object Home : BiologyRoute("biology/home")
    data object Catalogue : BiologyRoute("biology/catalogue")
    data class Domain(val domainId: String) : BiologyRoute("biology/domain/$domainId")
    data class Unit(val unitId: String) : BiologyRoute("biology/unit/$unitId")
    data class Chapter(val chapterId: String) : BiologyRoute("biology/chapter/$chapterId")
    data class Topic(val topicId: String) : BiologyRoute("biology/topic/$topicId")
    data class Concept(val conceptId: String) : BiologyRoute("biology/concept/$conceptId")
    data object Search : BiologyRoute("biology/search")
    data object Glossary : BiologyRoute("biology/glossary")
    data object Bookmarks : BiologyRoute("biology/bookmarks")
    data object Progress : BiologyRoute("biology/progress")
    data class Quiz(val topicId: String) : BiologyRoute("biology/quiz/$topicId")
    data class Revision(val topicId: String) : BiologyRoute("biology/revision/$topicId")
    data class Diagram(val diagramId: String) : BiologyRoute("biology/diagram/$diagramId")
    data class Future3D(val objectId: String) : BiologyRoute("biology/future-3d/$objectId")
}
