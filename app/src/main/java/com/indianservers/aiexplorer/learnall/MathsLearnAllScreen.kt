package com.indianservers.aiexplorer.learnall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private enum class LearnerPath(val label: String, val note: String) {
    Foundation("Step-by-step", "Shorter explanations, more concrete examples"),
    Average("Standard", "Balanced school-level explanation"),
    Advanced("Advanced", "Deeper reasoning and extension notes"),
    Revision("Revision", "Fast recap and practice focus"),
}

private enum class LessonTab(val label: String) {
    Learn("Learn"),
    Examples("Examples"),
    Practice("Practice"),
}

private enum class LessonDifficulty(val label: String) {
    All("All"),
    Beginner("Beginner"),
    Intermediate("Intermediate"),
    Advanced("Advanced"),
}

enum class MathsLearnAllMode {
    Concepts,
    ClassExplore,
}

@Composable
fun MathsLearnAllScreen(
    onBack: () -> Unit,
    initialChapter: String? = null,
    initialTopic: String? = null,
    initialQuery: String = "",
    initialLessonId: String? = null,
    mode: MathsLearnAllMode = MathsLearnAllMode.Concepts,
) {
    val context = LocalContext.current
    val repository = remember { MathsLearnAllRepository(context) }
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf(MathsLearnAllStats(0, 0, 0)) }
    var classes by remember { mutableStateOf<List<String>>(emptyList()) }
    var chapters by remember { mutableStateOf<List<String>>(emptyList()) }
    var topics by remember { mutableStateOf<List<String>>(emptyList()) }
    var concepts by remember { mutableStateOf<List<MathsConcept>>(emptyList()) }
    var summaries by remember { mutableStateOf<List<MathsLessonSummary>>(emptyList()) }
    var conceptSummaries by remember { mutableStateOf<List<MathsLessonSummary>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<String?>(null) }
    var selectedChapter by remember { mutableStateOf<String?>(initialChapter) }
    var selectedTopic by remember { mutableStateOf<String?>(initialTopic) }
    var selectedConcept by remember { mutableStateOf<MathsConcept?>(null) }
    var selectedLessonId by remember { mutableStateOf<String?>(initialLessonId) }
    var selectedLesson by remember { mutableStateOf<MathsLesson?>(null) }
    var learnerPath by remember { mutableStateOf(LearnerPath.Average) }
    var difficulty by remember { mutableStateOf(LessonDifficulty.All) }
    var query by remember { mutableStateOf(initialQuery) }
    var progress by remember { mutableStateOf<List<MathsLessonProgress>>(emptyList()) }
    var savedSummaries by remember { mutableStateOf<List<MathsLessonSummary>>(emptyList()) }
    var recentSummaries by remember { mutableStateOf<List<MathsLessonSummary>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadRequest by remember { mutableStateOf(0) }
    val browsingClasses = mode == MathsLearnAllMode.ClassExplore && query.isBlank() && selectedClass == null && selectedChapter == null
    val browsingChapters = query.isBlank() && selectedClass != null && selectedChapter == null
    val browsingTopics = query.isBlank() && selectedChapter != null && selectedTopic == null
    val activeSummaries = if (mode == MathsLearnAllMode.Concepts) conceptSummaries else summaries
    val completedIds = progress.filter { it.completed }.map { it.lessonId }.toSet()
    val savedIds = progress.filter { it.saved }.map { it.lessonId }.toSet()
    val filteredSummaries = activeSummaries.filter { difficulty == LessonDifficulty.All || lessonDifficulty(it.classLevel) == difficulty }
    val nextLesson = filteredSummaries.firstOrNull { it.id !in completedIds } ?: filteredSummaries.firstOrNull()
    val progressPercent = if (stats.lessonCount == 0) 0 else ((completedIds.size.toFloat() / stats.lessonCount) * 100).toInt().coerceIn(0, 100)

    suspend fun refreshLearnerState() {
        runCatching {
            progress = repository.progress()
            val savedIdsOrdered = progress.filter { it.saved }.sortedByDescending { it.updatedAt }.map { it.lessonId }
            val recentIdsOrdered = progress.filter { it.lastViewedAt > 0 }.sortedByDescending { it.lastViewedAt }.map { it.lessonId }
            savedSummaries = repository.summariesForIds(savedIdsOrdered.take(8))
            recentSummaries = repository.summariesForIds(recentIdsOrdered.take(8))
        }.onFailure {
            progress = emptyList()
            savedSummaries = emptyList()
            recentSummaries = emptyList()
            loadError = "Learner progress is temporarily unavailable."
        }
    }

    LaunchedEffect(reloadRequest) {
        runCatching {
            repository.seedBundledLessons()
            stats = repository.stats()
            concepts = repository.concepts()
            classes = repository.classes()
            summaries = repository.summaries(null, selectedChapter, selectedTopic, query)
            refreshLearnerState()
            loadError = null
        }.onFailure { error ->
            loadError = "Bundled lessons could not load. Tap retry."
            stats = MathsLearnAllStats(0, 0, 0)
            concepts = emptyList()
            classes = emptyList()
            summaries = emptyList()
        }
    }
    LaunchedEffect(selectedClass) {
        runCatching {
            if (selectedClass != null) selectedChapter = null
            selectedTopic = null
            chapters = repository.chapters(selectedClass)
        }.onFailure {
            chapters = emptyList()
            selectedChapter = null
            loadError = "Class list could not load."
        }
    }
    LaunchedEffect(selectedClass, selectedChapter) {
        runCatching {
            val nextTopics = repository.topics(selectedClass, selectedChapter)
            if (selectedChapter == null || selectedTopic !in nextTopics) selectedTopic = null
            topics = nextTopics
        }.onFailure {
            topics = emptyList()
            selectedTopic = null
            loadError = "Topic list could not load."
        }
    }
    LaunchedEffect(selectedClass, selectedChapter, selectedTopic, query) {
        runCatching {
            summaries = repository.summaries(selectedClass, selectedChapter, selectedTopic, query)
        }.onFailure {
            summaries = emptyList()
            loadError = "Lesson results could not load."
        }
    }
    LaunchedEffect(selectedConcept) {
        runCatching {
            conceptSummaries = selectedConcept?.let { repository.summariesForConcept(it) }.orEmpty()
        }.onFailure {
            conceptSummaries = emptyList()
            loadError = "Concept lessons could not load."
        }
    }
    LaunchedEffect(selectedLessonId) {
        runCatching {
            selectedLesson = selectedLessonId?.let {
                repository.markViewed(it)
                refreshLearnerState()
                repository.lesson(it)
            }
        }.onFailure {
            selectedLesson = null
            selectedLessonId = null
            loadError = "That lesson could not open."
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        selectedLesson?.let { lesson ->
            val lessonIndex = filteredSummaries.indexOfFirst { it.id == lesson.id }
            LessonReaderPage(
                lesson = lesson,
                learnerPath = learnerPath,
                lessonIndex = lessonIndex.takeIf { it >= 0 } ?: 0,
                lessonCount = filteredSummaries.size.coerceAtLeast(1),
                isSaved = lesson.id in savedIds,
                isCompleted = lesson.id in completedIds,
                onBack = {
                    selectedLessonId = null
                    selectedLesson = null
                },
                onPrevious = if (lessonIndex > 0) {
                    { selectedLessonId = filteredSummaries[lessonIndex - 1].id }
                } else {
                    null
                },
                onNext = if (lessonIndex >= 0 && lessonIndex < filteredSummaries.lastIndex) {
                    { selectedLessonId = filteredSummaries[lessonIndex + 1].id }
                } else {
                    null
                },
                onToggleSaved = {
                    scope.launch {
                        repository.toggleSaved(lesson.id)
                        refreshLearnerState()
                    }
                },
                onComplete = {
                    scope.launch {
                        repository.markCompleted(lesson.id)
                        refreshLearnerState()
                    }
                },
                onHint = {
                    scope.launch {
                        repository.recordHint(lesson.id)
                        refreshLearnerState()
                    }
                },
            )
            return@Surface
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            loadError?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = {
                                loadError = null
                                reloadRequest++
                            }) { Text("Retry") }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Maths > ${if (mode == MathsLearnAllMode.Concepts) "Learn" else "Explore"}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(if (mode == MathsLearnAllMode.Concepts) "Math Concepts" else "Class-wise Study", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(
                            if (mode == MathsLearnAllMode.Concepts) "Choose one of the 29 mathematics concepts, then open its related lessons."
                            else "Study by class, chapter, topic, difficulty, and search filters.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    TextButton(onClick = onBack) { Text("Back") }
                }
            }
            if (mode == MathsLearnAllMode.Concepts) {
                if (selectedConcept == null) {
                    item {
                        LessonBrowseHeader(
                            title = "29 Math concepts",
                            subtitle = "Compact grid. Tap a concept to reveal grouped lessons.",
                            count = concepts.size,
                        )
                    }
                    concepts.chunked(2).forEachIndexed { rowIndex, row ->
                        item(key = "concept-grid-row:$rowIndex") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEachIndexed { offset, concept ->
                                    ConceptGridCard(
                                        concept = concept,
                                        accent = domainAccent(rowIndex * 2 + offset),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        selectedConcept = concept
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    item {
                        SelectedConceptHeader(
                            concept = selectedConcept!!,
                            count = filteredSummaries.size,
                            onBack = { selectedConcept = null },
                        )
                    }
                    item {
                        Text("Difficulty", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LessonDifficulty.entries.forEach { level ->
                                FilterChip(
                                    selected = difficulty == level,
                                    onClick = { difficulty = level },
                                    label = { Text(level.label) },
                                )
                            }
                        }
                    }
                    if (filteredSummaries.isEmpty()) {
                        item {
                            EmptyLessonMessage("No related lessons found", "Try another concept or change the difficulty filter.")
                        }
                    } else {
                        item {
                            MiniLessonMap(
                                summaries = filteredSummaries,
                                completedIds = completedIds,
                                currentId = selectedLessonId,
                                onOpen = { selectedLessonId = it },
                            )
                        }
                        val grouped = filteredSummaries.groupBy { it.topic.ifBlank { it.chapter } }
                        grouped.forEach { (topicName, lessons) ->
                            item(key = "concept-topic-header:$topicName") {
                                Text(topicName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            items(lessons, key = { it.id }) { item ->
                                LessonSummaryCard(
                                    summary = item,
                                    selected = selectedLessonId == item.id,
                                    saved = item.id in savedIds,
                                    completed = item.id in completedIds,
                                ) { selectedLessonId = item.id }
                            }
                        }
                    }
                }
            } else {
            item {
                ExploreCompactControls(
                    selectedClass = selectedClass,
                    selectedChapter = selectedChapter,
                    selectedTopic = selectedTopic,
                    difficulty = difficulty,
                    learnerPath = learnerPath,
                    onClear = {
                        selectedClass = null
                        selectedChapter = null
                        selectedTopic = null
                        query = ""
                    },
                    onDifficulty = { difficulty = it },
                    onLearnerPath = { learnerPath = it },
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search class, chapter, formula, subtopic or explanation") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            if (stats.lessonCount == 0 && loadError == null) {
                item { EmptyContentCard(onRetry = { reloadRequest++ }) }
            } else if (browsingClasses) {
                item {
                    LessonBrowseHeader(
                        title = "Choose class level",
                        subtitle = "Explore is class-wise. Pick one level first, then chapters, topics and lessons.",
                        count = classes.size,
                    )
                }
                classes.chunked(2).forEachIndexed { rowIndex, row ->
                    item(key = "class-grid-row:$rowIndex") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { classLevel ->
                                ClassLevelGridCard(
                                    classLevel = classLevel,
                                    accent = domainAccent(rowIndex),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    selectedClass = classLevel
                                    selectedChapter = null
                                    selectedTopic = null
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else if (browsingChapters) {
                item {
                    LessonBrowseHeader(
                        title = selectedClass ?: "Browse by chapter",
                        subtitle = "Choose a chapter first. Topics and subtopics open on the next screen.",
                        count = chapters.size,
                    )
                }
                items(chapters, key = { "chapter:$it" }) { chapter ->
                    LessonNavigationCard(
                        title = chapter,
                        subtitle = selectedClass ?: "All classes",
                        meta = "Open topics",
                        accent = MaterialTheme.colorScheme.primary,
                    ) {
                        selectedChapter = chapter
                        selectedTopic = null
                    }
                }
            } else if (browsingTopics) {
                item {
                    LessonBrowseHeader(
                        title = selectedChapter.orEmpty(),
                        subtitle = "Topics are grouped here so the subtopic list stays short.",
                        count = topics.size,
                    )
                }
                items(topics, key = { "topic:$it" }) { topicValue ->
                    LessonNavigationCard(
                        title = topicValue.ifBlank { "General" },
                        subtitle = selectedChapter.orEmpty(),
                        meta = "Open subtopics",
                        accent = MaterialTheme.colorScheme.secondary,
                    ) {
                        selectedTopic = topicValue
                    }
                }
            } else {
                item {
                    LessonBrowseHeader(
                        title = if (query.isBlank()) selectedTopic ?: selectedChapter ?: "Lessons" else "Search results",
                        subtitle = if (query.isBlank()) "Subtopics ready to learn" else "Matching classes, chapters, topics and explanations",
                        count = filteredSummaries.size,
                    )
                }
                if (filteredSummaries.isEmpty()) {
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("No lessons found", fontWeight = FontWeight.Bold)
                                Text("Clear search or choose a broader chapter/topic.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(onClick = {
                                    query = ""
                                    selectedTopic = null
                                }) { Text("Clear search") }
                            }
                        }
                    }
                } else {
                    item {
                        MiniLessonMap(
                            summaries = filteredSummaries,
                            completedIds = completedIds,
                            currentId = selectedLessonId,
                            onOpen = { selectedLessonId = it },
                        )
                    }
                    val grouped = filteredSummaries.groupBy { it.topic.ifBlank { "General" } }
                    grouped.forEach { (topicName, lessons) ->
                        item(key = "topic-header:$topicName") {
                            Text(topicName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        items(lessons, key = { it.id }) { item ->
                            LessonSummaryCard(
                                summary = item,
                                selected = selectedLessonId == item.id,
                                saved = item.id in savedIds,
                                completed = item.id in completedIds,
                            ) { selectedLessonId = item.id }
                        }
                    }
                }
            }
        }
            }
    }
}

@Composable
private fun ConceptListCard(concept: MathsConcept, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(.68f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(accent.copy(.18f)), contentAlignment = Alignment.Center) {
                Text(concept.icon, color = accent, fontWeight = FontWeight.Black, fontSize = if (concept.icon.length > 2) 14.sp else 20.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(concept.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(concept.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    concept.levels.take(5).forEach { level -> AssistChip(onClick = {}, label = { Text(level) }) }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${concept.lessonCount}", color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("lessons", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ConceptGridCard(
    concept: MathsConcept,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier.heightIn(min = 118.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(.68f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(.18f)), contentAlignment = Alignment.Center) {
                    Text(concept.icon, color = accent, fontWeight = FontWeight.Black, fontSize = if (concept.icon.length > 2) 11.sp else 17.sp)
                }
                Text("${concept.lessonCount}", color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            Text(concept.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleSmall, maxLines = 2)
            Text(concept.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
    }
}

@Composable
private fun ClassLevelGridCard(
    classLevel: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val band = classBandLabel(classLevel)
    Card(
        modifier = modifier.heightIn(min = 92.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(.62f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(band, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(classLevel, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 2)
            Text("Open chapters", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ExploreCompactControls(
    selectedClass: String?,
    selectedChapter: String?,
    selectedTopic: String?,
    difficulty: LessonDifficulty,
    learnerPath: LearnerPath,
    onClear: () -> Unit,
    onDifficulty: (LessonDifficulty) -> Unit,
    onLearnerPath: (LearnerPath) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(.24f))) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        listOfNotNull(selectedClass, selectedChapter, selectedTopic).joinToString(" > ").ifBlank { "Explore by class" },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                    )
                    Text("Class -> Chapter -> Topic -> Lesson", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onClear) { Text("Reset") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LessonDifficulty.entries.forEach { level ->
                    FilterChip(selected = difficulty == level, onClick = { onDifficulty(level) }, label = { Text(level.label) })
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                LearnerPath.entries.forEach { path ->
                    FilterChip(selected = learnerPath == path, onClick = { onLearnerPath(path) }, label = { Text(path.label) })
                }
            }
        }
    }
}

private fun classBandLabel(classLevel: String): String {
    val number = Regex("""\d+""").find(classLevel)?.value?.toIntOrNull()
    return when {
        number == null -> "Advanced"
        number <= 2 -> "Foundation"
        number <= 5 -> "Primary"
        number <= 8 -> "Middle"
        number <= 10 -> "Secondary"
        number <= 12 -> "Senior"
        else -> "Higher"
    }
}

@Composable
private fun SelectedConceptHeader(concept: MathsConcept, count: Int, onBack: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(.36f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(concept.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("$count related lessons · ${concept.levels.joinToString()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onBack) { Text("Concepts") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                concept.subtopics.forEach { subtopic -> AssistChip(onClick = {}, label = { Text(subtopic) }) }
            }
        }
    }
}

@Composable
private fun EmptyLessonMessage(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LearningDashboardCard(
    stats: MathsLearnAllStats,
    progressPercent: Int,
    nextLesson: MathsLessonSummary?,
    onContinue: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(colors.primaryContainer.copy(.82f), colors.secondaryContainer.copy(.72f))), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(progressPercent, Modifier.size(82.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Continue learning", color = colors.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(nextLesson?.subtopic ?: "Choose a topic to begin", color = colors.onSurface, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("${stats.lessonCount} lessons · ${stats.chapterCount} chapters · ${stats.classCount} levels", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text("Offline ready") })
                    AssistChip(onClick = {}, label = { Text("Revision") })
                    AssistChip(onClick = {}, label = { Text("Practice") })
                }
            }
            Button(onClick = onContinue, enabled = nextLesson != null) { Text("Start") }
        }
    }
}

@Composable
private fun ProgressRing(percent: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(colors.surface.copy(.82f), style = Stroke(10f))
            drawArc(
                color = colors.primary,
                startAngle = -90f,
                sweepAngle = percent.coerceIn(0, 100) * 3.6f,
                useCenter = false,
                style = Stroke(10f),
            )
        }
        Text("$percent%", color = colors.primary, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CategoryDashboard(summaries: List<MathsLessonSummary>, onSelect: (String) -> Unit) {
    val topTopics = summaries
        .groupBy { it.topic.ifBlank { it.chapter } }
        .entries
        .sortedByDescending { it.value.size }
        .take(8)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Explore by domain", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            topTopics.forEachIndexed { index, entry ->
                DomainCard(
                    title = entry.key,
                    count = entry.value.size,
                    accent = domainAccent(index),
                    onClick = { onSelect(entry.key) },
                )
            }
        }
    }
}

@Composable
private fun DomainCard(title: String, count: Int, accent: Color, onClick: () -> Unit) {
    Column(
        Modifier.width(172.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(.12f))
            .border(1.dp, accent.copy(.34f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(accent.copy(.18f)), contentAlignment = Alignment.Center) {
            Text(domainGlyph(title), color = accent, fontWeight = FontWeight.Black, fontSize = 18.sp)
        }
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, maxLines = 2)
        Text("$count lessons", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun QuickLessonShelves(
    recent: List<MathsLessonSummary>,
    saved: List<MathsLessonSummary>,
    completedIds: Set<String>,
    onOpen: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (recent.isNotEmpty()) LessonShelf("Recently viewed", recent, completedIds, onOpen)
        if (saved.isNotEmpty()) LessonShelf("Saved lessons", saved, completedIds, onOpen)
    }
}

@Composable
private fun LessonShelf(title: String, lessons: List<MathsLessonSummary>, completedIds: Set<String>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            lessons.forEach { lesson ->
                Column(
                    Modifier.width(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(.7f))
                        .clickable { onOpen(lesson.id) }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(if (lesson.id in completedIds) "Completed" else lessonDifficulty(lesson.classLevel).label, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(lesson.subtopic, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, maxLines = 2)
                    Text("${lesson.classLevel} · ${lesson.topic.ifBlank { lesson.chapter }}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MiniLessonMap(
    summaries: List<MathsLessonSummary>,
    completedIds: Set<String>,
    currentId: String?,
    onOpen: (String) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(.52f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lesson map", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                summaries.take(24).forEachIndexed { index, lesson ->
                    val completed = lesson.id in completedIds
                    val current = lesson.id == currentId
                    Box(
                        Modifier.size(if (current) 38.dp else 32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    completed -> Color(0xFF22C55E)
                                    current -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surface
                                },
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(.26f), CircleShape)
                            .clickable { onOpen(lesson.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = if (completed || current) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonBrowseHeader(title: String, subtitle: String, count: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .36f))) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            AssistChip(onClick = {}, label = { Text("$count") })
        }
    }
}

@Composable
private fun LessonNavigationCard(title: String, subtitle: String, meta: String, accent: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = .16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(title.take(2).uppercase(), color = accent, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(meta, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyContentCard(onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bundled lessons are being prepared", fontWeight = FontWeight.Bold)
            Text("This app includes the compressed Class 1 to advanced maths lesson pack. If this appears after opening Learn, retry loading the bundled content.")
            Button(onClick = onRetry) { Text("Retry lessons") }
        }
    }
}

@Composable
private fun LessonSummaryCard(summary: MathsLessonSummary, selected: Boolean, saved: Boolean, completed: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .65f),
        ),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(domainAccent(summary.topic.hashCode()).copy(.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(domainGlyph(summary.topic), color = domainAccent(summary.topic.hashCode()), fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${summary.classLevel} > ${summary.chapter}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(summary.topic.ifBlank { "General" }, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                Text(summary.subtopic, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(lessonDifficulty(summary.classLevel).label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    if (saved) Text("Saved", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    if (completed) Text("Completed", color = Color(0xFF16A34A), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Text("Open", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LessonReaderPage(
    lesson: MathsLesson,
    learnerPath: LearnerPath,
    lessonIndex: Int,
    lessonCount: Int,
    isSaved: Boolean,
    isCompleted: Boolean,
    onBack: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onToggleSaved: () -> Unit,
    onComplete: () -> Unit,
    onHint: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var activeTab by remember(lesson.id) { mutableStateOf(LessonTab.Learn) }
    val explanationSections = when (learnerPath) {
        LearnerPath.Foundation -> lesson.content.simplifiedExplanation.ifEmpty { lesson.content.detailedExplanation }
        LearnerPath.Advanced -> lesson.content.advancedExplanation.ifEmpty { lesson.content.detailedExplanation }
        LearnerPath.Revision -> lesson.content.practicePrompt.ifEmpty { lesson.content.detailedExplanation }
        LearnerPath.Average -> lesson.content.detailedExplanation
    }
    val explanationFallback = when (learnerPath) {
        LearnerPath.Foundation -> lesson.simplifiedExplanation.ifBlank { lesson.detailedExplanation }
        LearnerPath.Advanced -> lesson.advancedExplanation.ifBlank { lesson.detailedExplanation }
        LearnerPath.Revision -> lesson.practicePrompt.ifBlank { lesson.detailedExplanation }
        LearnerPath.Average -> lesson.detailedExplanation
    }
    val progress = ((lessonIndex + 1).toFloat() / lessonCount.toFloat()).coerceIn(0f, 1f)
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant.copy(alpha = .55f), colors.background))),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) { Text("< Back", color = colors.primary) }
            Text("Lesson", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = colors.onSurface)
            TextButton(onClick = onToggleSaved) { Text(if (isSaved) "Saved" else "Save", color = colors.primary) }
        }
        LessonHeroCard(lesson, progress, lessonIndex, lessonCount)
        PrimaryTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = colors.surface,
            contentColor = colors.primary,
            modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp)),
        ) {
            LessonTab.entries.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab.label, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.SemiBold) },
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (activeTab) {
                LessonTab.Learn -> {
                    item { LessonVisualCard(lesson) }
                    item { NativeLessonSection("What is it?", lesson.content.introduction, lesson.introduction, colors.primary) }
                    item { NativeLessonSection("Learn clearly", explanationSections, explanationFallback, colors.primary) }
                    if (lesson.content.simplifiedExplanation.isNotEmpty() || lesson.simplifiedExplanation.isNotBlank()) {
                        item { HighlightCard("Easy way to remember", lesson.content.simplifiedExplanation, lesson.simplifiedExplanation, Color(0xFFFFF8E1), Color(0xFFEF6C00)) }
                    }
                }
                LessonTab.Examples -> {
                    item { NativeLessonSection("Clear examples", lesson.content.realtimeExamples, lesson.realtimeExamples, colors.tertiary) }
                    item { ExampleImageCard(lesson) }
                    if (lesson.content.advancedExplanation.isNotEmpty() || lesson.advancedExplanation.isNotBlank()) {
                        item { HighlightCard("A little deeper", lesson.content.advancedExplanation, lesson.advancedExplanation, Color(0xFFEFF7FF), Color(0xFF1565C0)) }
                    }
                }
                LessonTab.Practice -> {
                    item { NativeLessonSection("Try it yourself", lesson.content.practicePrompt, lesson.practicePrompt.ifBlank { "Practise this lesson with one small example." }, colors.secondary) }
                    item { PracticeChecklistCard(lesson, isCompleted, onHint, onComplete) }
                }
            }
        }
        ReaderBottomBar(onPrevious, onNext, onMap = { activeTab = LessonTab.Learn })
    }
}

@Composable
private fun LessonHeroCard(lesson: MathsLesson, progress: Float, lessonIndex: Int, lessonCount: Int) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(colors.surfaceVariant.copy(alpha = .92f), colors.surface.copy(alpha = .96f))), RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(78.dp)
                    .clip(RoundedCornerShape(39.dp))
                    .background(Brush.linearGradient(listOf(colors.primary, colors.secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Text(lesson.subtopic.take(2).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(lesson.chapter, color = colors.primary, fontWeight = FontWeight.Bold)
                Text(lesson.subtopic, color = colors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${lesson.topic} · Subtopic ${lessonIndex + 1} of $lessonCount", color = Color(0xFF69708A), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                    color = colors.primary,
                    trackColor = colors.onSurface.copy(alpha = .16f),
                )
            }
            Text("${(progress * 100).toInt()}%", color = colors.primary, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NativeLessonSection(title: String, sections: List<MathsLessonSection>, fallback: String, accent: Color) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            LessonSectionContent(sections, fallback.ifBlank { "Review the lesson idea with one simple example, then check your answer carefully." }, accent)
        }
    }
}

@Composable
private fun HighlightCard(title: String, sections: List<MathsLessonSection>, fallback: String, background: Color, accent: Color) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.surfaceVariant.copy(alpha = .62f)).border(1.dp, accent.copy(.32f), RoundedCornerShape(20.dp)).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(.13f)), contentAlignment = Alignment.Center) {
            Text("i", color = accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            LessonSectionContent(sections, fallback, accent)
        }
    }
}

@Composable
private fun LessonSectionContent(sections: List<MathsLessonSection>, fallback: String, accent: Color) {
    val content = sections.takeIf { it.isNotEmpty() } ?: listOf(MathsLessonSection(body = fallback))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        content.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (section.title.isNotBlank()) {
                    Text(section.title, color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                }
                if (section.body.isNotBlank()) {
                    Text(section.body, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge, lineHeight = 23.sp)
                }
                section.bullets.forEach { bullet ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Text("•", color = accent, fontWeight = FontWeight.Black)
                        Text(bullet, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), lineHeight = 21.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonVisualCard(lesson: MathsLesson) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = .62f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ConceptSketch(lesson.subtopic, Modifier.width(120.dp).height(105.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("See the maths", color = colors.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(lessonVisualHint(lesson), color = colors.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExampleImageCard(lesson: MathsLesson) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = .62f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ConceptSketch(lesson.subtopic, Modifier.width(110.dp).height(96.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Real-life connection", color = colors.tertiary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                Text(realLifeHint(lesson.subtopic), color = colors.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PracticeChecklistCard(lesson: MathsLesson, isCompleted: Boolean, onHint: () -> Unit, onComplete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = .62f))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Before moving next", color = colors.primary, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("1. Say the main idea aloud.", color = colors.onSurface)
            Text("2. Work through one small example slowly.", color = colors.onSurface)
            Text("3. Explain how you know your answer is correct.", color = colors.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onHint) { Text("Need help?") }
                Button(onClick = onComplete) { Text(if (isCompleted) "Completed" else "Mark complete") }
            }
            Text("Mistake review: if the answer felt unclear, save this lesson and revisit it in Revision mode.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReaderBottomBar(onPrevious: (() -> Unit)?, onNext: (() -> Unit)?, onMap: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
            Text("Previous", maxLines = 1, textAlign = TextAlign.Center)
        }
        TextButton(onClick = onMap, modifier = Modifier.width(88.dp).heightIn(min = 52.dp)) {
            Text("Lesson\nMap", maxLines = 2, textAlign = TextAlign.Center)
        }
        Button(onClick = { onNext?.invoke() }, enabled = onNext != null, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
            Text("Next", maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ConceptSketch(subtopic: String, modifier: Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, Color(0xFFDDE7FF), RoundedCornerShape(18.dp)).padding(8.dp)) {
        val stroke = 6f
        val purple = Color(0xFF7048E8)
        val green = Color(0xFF16A34A)
        when {
            "circle" in subtopic.lowercase() -> drawCircle(purple, radius = size.minDimension * .32f, center = center, style = Stroke(stroke))
            "triangle" in subtopic.lowercase() -> {
                val path = Path().apply {
                    moveTo(size.width * .5f, size.height * .18f)
                    lineTo(size.width * .18f, size.height * .78f)
                    lineTo(size.width * .82f, size.height * .78f)
                    close()
                }
                drawPath(path, purple, style = Stroke(stroke))
            }
            "square" in subtopic.lowercase() -> drawRect(purple, topLeft = Offset(size.width * .22f, size.height * .18f), size = androidx.compose.ui.geometry.Size(size.width * .58f, size.width * .58f), style = Stroke(stroke))
            "rectangle" in subtopic.lowercase() -> drawRect(purple, topLeft = Offset(size.width * .14f, size.height * .28f), size = androidx.compose.ui.geometry.Size(size.width * .72f, size.height * .42f), style = Stroke(stroke))
            "pattern" in subtopic.lowercase() -> repeat(5) { index ->
                val x = size.width * (.16f + index * .17f)
                if (index % 2 == 0) drawCircle(purple, size.minDimension * .09f, Offset(x, size.height * .5f)) else drawRect(green, Offset(x - 12f, size.height * .5f - 12f), androidx.compose.ui.geometry.Size(24f, 24f))
            }
            else -> repeat(6) { index ->
                val angle = index * Math.PI / 3
                val point = Offset(center.x + cos(angle).toFloat() * size.width * .28f, center.y + sin(angle).toFloat() * size.height * .28f)
                drawCircle(if (index % 2 == 0) purple else green, 12f, point)
            }
        }
    }
}

private fun realLifeHint(subtopic: String): String = when {
    "circle" in subtopic.lowercase() -> "Coins, wheels, clocks, and buttons help students recognise circles."
    "triangle" in subtopic.lowercase() -> "Pizza slices, warning signs, and roof frames can show triangle shapes."
    "square" in subtopic.lowercase() -> "Tiles, chessboard boxes, and sticky notes often show square shapes."
    "rectangle" in subtopic.lowercase() -> "Books, doors, boards, and phone screens are common rectangle examples."
    "pattern" in subtopic.lowercase() -> "Tiles, borders, rangoli, and wrapping paper often repeat shape patterns."
    else -> "Blocks, paper cut-outs, tiles, and classroom objects make this idea visible."
}

private fun lessonVisualHint(lesson: MathsLesson): String = when {
    "shape" in lesson.chapter.lowercase() || "geometry" in lesson.chapter.lowercase() ->
        "Look at the figure, notice the important parts, then match the picture with the words in the lesson."
    "number" in lesson.chapter.lowercase() || "count" in lesson.topic.lowercase() ->
        "Use small groups, number lines, counters, or daily objects to make the number idea visible."
    "measurement" in lesson.chapter.lowercase() ->
        "Connect the idea with length, weight, time, money, or other quantities you can compare."
    "data" in lesson.chapter.lowercase() || "statistics" in lesson.chapter.lowercase() ->
        "Read the labels first, then compare the values carefully before answering."
    else ->
        "Turn the idea into a small example, say each step clearly, and connect it with the lesson card."
}

private fun lessonDifficulty(classLevel: String): LessonDifficulty {
    val value = classLevel.lowercase()
    return when {
        "phd" in value || "research" in value || "undergraduate" in value || "pg" in value || "class 11" in value || "class 12" in value -> LessonDifficulty.Advanced
        "class 6" in value || "class 7" in value || "class 8" in value || "class 9" in value || "class 10" in value -> LessonDifficulty.Intermediate
        else -> LessonDifficulty.Beginner
    }
}

private fun domainGlyph(title: String): String {
    val value = title.lowercase()
    return when {
        "algebra" in value || "equation" in value -> "x²"
        "geometry" in value || "shape" in value || "triangle" in value || "circle" in value -> "△"
        "calculus" in value || "limit" in value || "derivative" in value || "integral" in value -> "∫"
        "stat" in value || "data" in value || "probability" in value -> "σ"
        "number" in value || "arithmetic" in value -> "123"
        "trig" in value || "angle" in value -> "θ"
        "matrix" in value || "vector" in value -> "▦"
        else -> "∞"
    }
}

private fun domainAccent(index: Int): Color {
    val palette = listOf(
        Color(0xFF6D5DF6),
        Color(0xFF0EA5E9),
        Color(0xFF16A34A),
        Color(0xFFF97316),
        Color(0xFFDB2777),
        Color(0xFF8B5CF6),
        Color(0xFF0891B2),
        Color(0xFFEAB308),
    )
    return palette[kotlin.math.abs(index) % palette.size]
}
