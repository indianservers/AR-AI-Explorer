package com.indianservers.aiexplorer.curriculum.production

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Neutral on-demand renderer. All educational content remains in the subject-owned repositories. */
@Composable
fun CurriculumLearningScreen(chapter: SubjectOwnedCurriculumChapter, progressPercent: Int, bookmarked: Boolean, revisionDue: Boolean, onBookmarkChange: (Boolean) -> Unit, onMarkComplete: () -> Unit) {
    var showAnswers by remember(chapter.id) { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
        item {
            Text(chapter.officialChapterTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Class ${chapter.classLevel.number} · ${chapter.subject.name.lowercase().replaceFirstChar { it.uppercase() }} · ${chapter.unitTitle}")
            Text("${chapter.estimatedMinutes} min · $progressPercent% complete · ${if (revisionDue) "Revision due" else "Revision current"}")
            TextButton(onClick = { onBookmarkChange(!bookmarked) }) { Text(if (bookmarked) "Remove bookmark" else "Bookmark") }
        }
        section("Learning objectives") { chapter.learningObjectives.forEach { Text("• $it") } }
        section("Prerequisites") { chapter.prerequisites.forEach { Text("• $it") } }
        chapter.explanationSections.forEach { (id, text) -> section(id.replace('-', ' ').replaceFirstChar { it.uppercase() }) { Text(text) } }
        section("Visual explanation") { chapter.diagrams.forEach { diagram -> CurriculumDiagramView(diagram) } }
        section("Key terms") { chapter.keyTerms.forEach { (term, meaning) -> Text("$term — $meaning") } }
        section("Worked examples") { chapter.workedExamples.forEach { example -> Text(example.prompt, fontWeight = FontWeight.SemiBold); example.steps.forEachIndexed { index, step -> Text("${index + 1}. $step") }; Text("Answer: ${example.answer}"); Text("Check: ${example.validation}") } }
        if (chapter.formulaLinks.isNotEmpty()) section("Formula support") { chapter.formulaLinks.forEach { formula -> Text(formula.relationship, fontWeight = FontWeight.SemiBold); Text(formula.symbolDefinitions.entries.joinToString { "${it.key}: ${it.value}" }); Text("Assumptions: ${formula.assumptions.joinToString()}") } }
        section("Activity or practical") { chapter.activities.forEach { Text("• $it") }; chapter.practicals.forEach { practical -> Text(practical.objective, fontWeight = FontWeight.SemiBold); Text(if (practical.physicalLabRequired) "Supervised physical-lab guidance; not a virtual substitute." else practical.supportType.name); practical.procedure.forEachIndexed { i, step -> Text("${i + 1}. $step") }; practical.safetyNotes.forEach { Text("Safety: $it") } } }
        section("Common mistakes") { chapter.misconceptions.forEach { (claim, correction) -> Text(claim, fontWeight = FontWeight.SemiBold); Text(correction) } }
        section("Practice and competency check") { chapter.questions.forEach { question -> Text("${question.questionType.name.replace('_', ' ')} · ${question.prompt}"); if (showAnswers) Text("Answer: ${question.answer.expected}\n${question.explanation}") }; TextButton(onClick = { showAnswers = !showAnswers }) { Text(if (showAnswers) "Hide answers" else "Check answers") } }
        section("Revision") { chapter.revision.takeaways.forEach { Text("• $it") }; chapter.revision.formulaRecap.forEach { Text("Formula: $it") }; chapter.revision.confusedPoints.forEach { Text("Do not confuse: $it") } }
        item { Button(onClick = onMarkComplete, modifier = Modifier.fillMaxWidth()) { Text("Mark lesson complete") } }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(title: String, content: @Composable ColumnScope.() -> Unit) {
    item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); content() } } }
}
