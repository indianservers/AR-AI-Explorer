package com.indianservers.aiexplorer.learnall

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class LearnerPath(val label: String, val note: String) {
    Foundation("Step-by-step", "Shorter explanations, more concrete examples"),
    Average("Standard", "Balanced school-level explanation"),
    Advanced("Advanced", "Deeper reasoning and extension notes"),
    Revision("Revision", "Fast recap and practice focus"),
}

@Composable
fun MathsLearnAllScreen(
    onBack: () -> Unit,
    initialChapter: String? = null,
    initialTopic: String? = null,
    initialQuery: String = "",
) {
    val context = LocalContext.current
    val repository = remember { MathsLearnAllRepository(context) }
    var stats by remember { mutableStateOf(MathsLearnAllStats(0, 0, 0)) }
    var classes by remember { mutableStateOf<List<String>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<String>>(emptyList()) }
    var topics by remember { mutableStateOf<List<String>>(emptyList()) }
    var summaries by remember { mutableStateOf<List<MathsLessonSummary>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(initialChapter) }
    var selectedTopic by remember { mutableStateOf<String?>(initialTopic) }
    var selectedLessonId by remember { mutableStateOf<String?>(null) }
    var selectedLesson by remember { mutableStateOf<MathsLesson?>(null) }
    var learnerPath by remember { mutableStateOf(LearnerPath.Average) }
    var query by remember { mutableStateOf(initialQuery) }

    LaunchedEffect(Unit) {
        repository.seedBundledLessons()
        stats = repository.stats()
        classes = repository.classes()
        summaries = repository.summaries(null, selectedChapter, selectedTopic, query)
    }
    LaunchedEffect(selectedClass) {
        if (selectedClass != null) selectedChapter = null
        selectedTopic = null
        chapters = repository.chapters(selectedClass)
    }
    LaunchedEffect(selectedClass, selectedChapter) {
        if (selectedChapter == null || selectedTopic !in repository.topics(selectedClass, selectedChapter)) selectedTopic = null
        topics = repository.topics(selectedClass, selectedChapter)
    }
    LaunchedEffect(selectedClass, selectedChapter, selectedTopic, query) {
        summaries = repository.summaries(selectedClass, selectedChapter, selectedTopic, query)
    }
    LaunchedEffect(selectedLessonId) {
        selectedLesson = selectedLessonId?.let { repository.lesson(it) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Maths > Learn All", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text("Class 1 to PG lessons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onBack) { Text("Back") }
                }
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${stats.lessonCount} lessons") })
                    AssistChip(onClick = {}, label = { Text("${stats.chapterCount} chapters") })
                    AssistChip(onClick = {}, label = { Text("${stats.classCount} classes") })
                }
            }
            item {
                Text("Learning style", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LearnerPath.entries.forEach { path ->
                        FilterChip(
                            selected = learnerPath == path,
                            onClick = { learnerPath = path },
                            label = { Text(path.label) },
                        )
                    }
                }
                Text(learnerPath.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search class, chapter, subtopic or explanation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                Text("Class", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedClass == null, onClick = { selectedClass = null }, label = { Text("All") })
                    classes.forEach { value ->
                        FilterChip(selected = selectedClass == value, onClick = { selectedClass = value }, label = { Text(value) })
                    }
                }
            }
            if (chapters.isNotEmpty()) {
                item {
                    Text("Chapter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedChapter == null, onClick = { selectedChapter = null }, label = { Text("All") })
                        chapters.forEach { value ->
                            FilterChip(selected = selectedChapter == value, onClick = { selectedChapter = value }, label = { Text(value) })
                        }
                    }
                }
            }
            if (topics.isNotEmpty()) {
                item {
                    Text("Topic", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selectedTopic == null, onClick = { selectedTopic = null }, label = { Text("All") })
                        topics.forEach { value ->
                            FilterChip(selected = selectedTopic == value, onClick = { selectedTopic = value }, label = { Text(value) })
                        }
                    }
                }
            }
            selectedLesson?.let { lesson ->
                item { LessonDetailCard(lesson, learnerPath) }
            }
            if (stats.lessonCount == 0) {
                item { EmptyContentCard() }
            } else {
                items(summaries, key = { it.id }) { item ->
                    LessonSummaryCard(item, selectedLessonId == item.id) { selectedLessonId = item.id }
                }
            }
        }
    }
}

@Composable
private fun EmptyContentCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ready for real Maths content", fontWeight = FontWeight.Bold)
            Text("The SQLite database is created, but no lessons have been imported yet.")
            Text("Excel columns expected: Class, Chapter, Subtopic, Introduction, Detailed Explanation, Realtime examples.")
            Text("Optional personalization columns: Simplified Explanation, Advanced Explanation, Practice Prompt.")
        }
    }
}

@Composable
private fun LessonSummaryCard(summary: MathsLessonSummary, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f),
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${summary.classLevel} > ${summary.chapter}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text(summary.topic, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            Text(summary.subtopic, fontWeight = FontWeight.Bold)
            Text("Open lesson", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LessonDetailCard(lesson: MathsLesson, learnerPath: LearnerPath) {
    val explanation = when (learnerPath) {
        LearnerPath.Foundation -> lesson.simplifiedExplanation.ifBlank { lesson.detailedExplanation }
        LearnerPath.Advanced -> lesson.advancedExplanation.ifBlank { lesson.detailedExplanation }
        LearnerPath.Revision -> lesson.practicePrompt.ifBlank { lesson.detailedExplanation }
        LearnerPath.Average -> lesson.detailedExplanation
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(lesson.subtopic, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${lesson.classLevel} > ${lesson.chapter} > ${lesson.topic}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text("Path: ${learnerPath.label}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            Section("Introduction", lesson.introduction)
            Section("Explanation", explanation)
            Section("Realtime Examples", lesson.realtimeExamples)
            if (lesson.practicePrompt.isNotBlank() && learnerPath != LearnerPath.Revision) Section("Practice Prompt", lesson.practicePrompt)
        }
    }
}

@Composable
private fun Section(title: String, body: String) {
    Column {
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(body.ifBlank { "Content pending." })
    }
}
