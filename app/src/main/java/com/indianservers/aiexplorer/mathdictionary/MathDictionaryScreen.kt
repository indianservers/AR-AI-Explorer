package com.indianservers.aiexplorer.mathdictionary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.Amber
import com.indianservers.aiexplorer.Cyan
import com.indianservers.aiexplorer.GlowButton
import com.indianservers.aiexplorer.Green
import com.indianservers.aiexplorer.Ink
import com.indianservers.aiexplorer.MathFormulaText
import com.indianservers.aiexplorer.Muted
import com.indianservers.aiexplorer.SurfaceA
import com.indianservers.aiexplorer.SurfaceB
import com.indianservers.aiexplorer.TransparentIcon
import com.indianservers.aiexplorer.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MathDictionaryScreen(
    modifier: Modifier = Modifier,
    wide: Boolean,
) {
    val context = LocalContext.current
    val repository = remember { MathDictionaryRepository(context) }
    val scope = rememberCoroutineScope()
    var seeded by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(MathDictionaryStats(0, emptyMap())) }
    var query by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var selectedLetter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDifficulty by rememberSaveable { mutableStateOf<String?>(null) }
    var bookmarksOnly by rememberSaveable { mutableStateOf(false) }
    var summaries by remember { mutableStateOf<List<MathDictionaryTermSummary>>(emptyList()) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<MathDictionaryTermDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var descendingSort by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stats = repository.seedIfNeeded()
        seeded = true
    }
    LaunchedEffect(query) {
        delay(220)
        debouncedQuery = query
    }
    LaunchedEffect(seeded, debouncedQuery, selectedLetter, selectedCategory, selectedDifficulty, bookmarksOnly) {
        if (!seeded) return@LaunchedEffect
        loading = true
        summaries = repository.searchSummaries(debouncedQuery, selectedLetter, selectedCategory, selectedDifficulty, bookmarksOnly)
        loading = false
    }
    LaunchedEffect(selectedKey, seeded) {
        detail = if (seeded) selectedKey?.let { repository.loadTerm(it) } else null
    }
    BackHandler(enabled = selectedKey != null) {
        selectedKey = null
        detail = null
    }

    if (selectedKey != null && detail != null) {
        MathDictionaryDetailPage(
            detail = detail!!,
            summaries = summaries,
            onBack = { selectedKey = null; detail = null },
            onBookmark = { term ->
                scope.launch {
                    repository.setBookmarked(term.id, !term.isBookmarked)
                    detail = repository.loadTerm(term.termKey)
                    summaries = repository.searchSummaries(debouncedQuery, selectedLetter, selectedCategory, selectedDifficulty, bookmarksOnly)
                }
            },
            onOpenRelated = { selectedKey = it },
            onNavigate = { selectedKey = it },
            modifier = modifier,
        )
        return
    }

    val categories = stats.categoryCounts.keys.toList()
    fun cycleCategory() {
        selectedCategory = when {
            categories.isEmpty() -> null
            selectedCategory == null -> categories.first()
            categories.indexOf(selectedCategory) < 0 || categories.indexOf(selectedCategory) == categories.lastIndex -> null
            else -> categories[categories.indexOf(selectedCategory) + 1]
        }
    }
    fun cycleDifficulty() {
        val levels = listOf("Beginner", "Intermediate", "Advanced")
        selectedDifficulty = when (selectedDifficulty) {
            null -> levels.first()
            levels.last() -> null
            else -> levels.getOrNull(levels.indexOf(selectedDifficulty) + 1)
        }
    }
    val visibleSummaries = if (descendingSort) summaries.sortedByDescending { it.word.lowercase() } else summaries.sortedBy { it.word.lowercase() }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071326), Color(0xFF081A34), Color(0xFF060B19)))),
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (wide) 24.dp else 18.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("<-", color = Color(0xFFEAF0FF), fontSize = 26.sp)
                    Text(if (bookmarksOnly) "Saved" else "Save", color = Color(0xFFEAF0FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { bookmarksOnly = !bookmarksOnly })
                }
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Math Dictionary", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text("Understand every maths word", color = Color(0xFF9EAAC3), fontSize = 15.sp)
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search ${stats.termCount.coerceAtLeast(600)}+ terms") },
                    leadingIcon = { Text("Q", color = Color(0xFFDDE6FF), fontSize = 22.sp, fontWeight = FontWeight.Black) },
                    trailingIcon = { Text("mic", color = Color(0xFF8581FF), fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                )
            }
            item {
                Text("Browse by letter", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ('A'..'Z').forEach { letter ->
                        DictionaryLetterButton(letter.toString(), selectedLetter == letter.toString()) {
                            selectedLetter = if (selectedLetter == letter.toString()) null else letter.toString()
                        }
                    }
                }
            }
            item {
                Text("Filters", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DictionaryFilterButton("topics", selectedCategory ?: "All topics", Modifier.weight(1f), ::cycleCategory)
                    DictionaryFilterButton("level", selectedDifficulty ?: "All levels", Modifier.weight(1f), ::cycleDifficulty)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (loading) "Loading dictionary..." else "${visibleSummaries.size} terms", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(if (descendingSort) "Z-A" else "A-Z", color = Color(0xFF8581FF), fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { descendingSort = !descendingSort }.padding(horizontal = 10.dp, vertical = 8.dp))
                }
            }
            if (!loading && visibleSummaries.isEmpty()) {
                item {
                    EmptyDictionaryState(if (selectedLetter != null) "No words under ${selectedLetter.orEmpty()} with these filters." else "No dictionary words match this search.") {
                        query = ""; debouncedQuery = ""; selectedLetter = null; selectedCategory = null; selectedDifficulty = null; bookmarksOnly = false
                    }
                }
            }
            items(visibleSummaries, key = { it.termKey }) { term ->
                DictionaryListCard(
                    term = term,
                    onOpen = { selectedKey = term.termKey },
                    onBookmark = {
                        scope.launch {
                            repository.setBookmarked(term.id, !term.isBookmarked)
                            summaries = repository.searchSummaries(debouncedQuery, selectedLetter, selectedCategory, selectedDifficulty, bookmarksOnly)
                        }
                    },
                )
            }
        }
        DictionaryBottomNavigation(
            bookmarksOnly = bookmarksOnly,
            onLearn = { query = "" },
            onDictionary = { bookmarksOnly = false },
            onFavorites = { bookmarksOnly = true },
            onProfile = { descendingSort = !descendingSort },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DictionaryLetterButton(letter: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Brush.linearGradient(listOf(Color(0xFF8177FF), Color(0xFF476DFF))) else Brush.linearGradient(listOf(Color(0xFF192237), Color(0xFF11192B))))
            .border(1.dp, if (selected) Color(0xFF9C96FF) else Color(0xFF2C3854), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = Color(0xFFEAF0FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DictionaryFilterButton(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(Color(0xAA1A2540), Color(0x8A111A2D))))
            .border(1.dp, Color(0xFF2E3B5A), RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, color = Color(0xFFD9E2FF), fontSize = 11.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color(0xFFEAF0FF), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("v", color = Color(0xFFD9E2FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DictionaryBottomNavigation(
    bookmarksOnly: Boolean,
    onLearn: () -> Unit,
    onDictionary: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEE172238), Color(0xF00D1528))))
            .border(1.dp, Color(0x333E4A68), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DictionaryNavItem("Learn", "cap", false, onLearn)
        DictionaryNavItem("Dictionary", "book", !bookmarksOnly, onDictionary)
        DictionaryNavItem("Favorites", "heart", bookmarksOnly, onFavorites)
        DictionaryNavItem("Profile", "user", false, onProfile)
    }
}

@Composable
private fun DictionaryNavItem(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(icon, color = if (selected) Color(0xFF8581FF) else Color(0xFF7D879E), fontSize = 12.sp, fontWeight = FontWeight.Black)
        Text(label, color = if (selected) Color(0xFF8581FF) else Color(0xFF7D879E), fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun DictionaryListCard(term: MathDictionaryTermSummary, onOpen: () -> Unit, onBookmark: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Color(0xAA18233A), Color(0x8A0F1A2E))))
            .border(1.dp, Color(0xFF2A3652), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DictionaryConceptIcon(term.word, term.imageAsset, Modifier.size(74.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(term.word, color = Color(0xFFF7F9FF), fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (term.isBookmarked) "saved" else "save", color = Color(0xFFDDE6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onBookmark).padding(4.dp))
            }
            Text(term.shortDefinition, color = Color(0xFFB6C0D6), fontSize = 14.sp, lineHeight = 20.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                DictionaryChip(term.category, Color(0xFF8581FF))
                DictionaryChip(term.difficultyLevel, if (term.difficultyLevel == "Advanced") Violet else Green)
            }
        }
        Text(">", color = Color(0xFFDDE6FF), fontSize = 26.sp, fontWeight = FontWeight.Light)
    }
}
@Composable
private fun MathDictionaryDetailPage(
    detail: MathDictionaryTermDetail,
    summaries: List<MathDictionaryTermSummary>,
    onBack: () -> Unit,
    onBookmark: (MathDictionaryTermDetail) -> Unit,
    onOpenRelated: (String) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentIndex = summaries.indexOfFirst { it.termKey == detail.termKey }
    val previousSummary = summaries.getOrNull(currentIndex - 1)
    val nextSummary = summaries.getOrNull(currentIndex + 1)
    val formula = detail.formulaLatex.ifBlank { if (detail.word.contains("absolute", true)) "|x| = x if x >= 0   |   |x| = -x if x < 0" else "" }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071326), Color(0xFF081A34), Color(0xFF060B19)))),
    ) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(bottom = 98.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("<-", color = Color(0xFFDDE6FF), fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onBack).padding(6.dp))
                    Text("Math Dictionary", color = Color(0xFFB7C0D8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (detail.isBookmarked) "saved" else "save", color = Color(0xFFDDE6FF), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBookmark(detail) })
                        Text("share", color = Color(0xFFDDE6FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DictionaryConceptIcon(detail.word, detail.imageAsset, Modifier.size(104.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(detail.word, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black, maxLines = 2, lineHeight = 38.sp, modifier = Modifier.weight(1f))
                            Text("sound", color = Color(0xFF8581FF), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(Color(0x332B2B6F)).border(1.dp, Color(0xFF5550B5), RoundedCornerShape(99.dp)).padding(10.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DictionaryChip(detail.category, Color(0xFF8581FF))
                            DictionaryChip(detail.difficultyLevel, if (detail.difficultyLevel == "Advanced") Violet else Green)
                        }
                        if (detail.pronunciation.isNotBlank()) Text(detail.pronunciation, color = Color(0xFF9EAAC3), fontSize = 15.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
            }
            item {
                DictionaryDetailCard {
                    Text("Meaning", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(detail.detailedDefinition.ifBlank { detail.shortDefinition }, color = Color(0xFFC3CADB), fontSize = 16.sp, lineHeight = 24.sp)
                    if (formula.isNotBlank()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x55121C30))
                                .border(1.dp, Color(0xFF3A4664), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MathFormulaText(formula, color = Color(0xFFDDE6FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item {
                DictionaryDetailCard {
                    Text(if (detail.word.contains("absolute", true)) "See it on a number line" else "See it visually", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (detail.word.contains("absolute", true)) {
                        AbsoluteValueNumberLine(Modifier.fillMaxWidth().height(178.dp))
                    } else {
                        DictionaryConceptSketch(detail.word, Modifier.fillMaxWidth().height(132.dp))
                    }
                    Text(
                        if (detail.word.contains("absolute", true)) "Both numbers are the same distance from zero." else detail.exampleExplanation,
                        color = Color(0xFFB6C0D6),
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DictionarySmallPanel(Modifier.weight(1f)) {
                        Text("Try another", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text(samplePromptFor(detail.word), color = Color(0xFFC3CADB), fontSize = 15.sp)
                        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0x332D2A6E)).border(1.dp, Color(0xFF5550B5), RoundedCornerShape(12.dp)).padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Text(sampleAnswerFor(detail.word), color = Color(0xFF8581FF), fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    DictionarySmallPanel(Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x332D2A6E)), contentAlignment = Alignment.Center) {
                                Text("tip", color = Color(0xFF8581FF), fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                            Text(tipFor(detail.word, detail.analogy), color = Color(0xFFC3CADB), fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (detail.relatedTerms.isNotEmpty()) {
                item {
                    DictionaryDetailCard {
                        Text("Related words", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            detail.relatedTerms.forEach { related ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x1F8581FF))
                                        .border(1.dp, Color(0xFF625BCE), RoundedCornerShape(12.dp))
                                        .clickable { onOpenRelated(related.termKey) }
                                        .padding(horizontal = 16.dp, vertical = 9.dp),
                                ) {
                                    Text(related.word, color = Color(0xFF9993FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            item {
                DictionaryDetailCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(previousSummary?.let { "< ${it.word}" } ?: "< Previous", color = if (previousSummary != null) Color(0xFF9993FF) else Color(0xFF586174), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = previousSummary != null) { previousSummary?.termKey?.let(onNavigate) })
                        Box(Modifier.width(1.dp).height(28.dp).background(Color(0xFF2A3652)))
                        Text(nextSummary?.let { "${it.word} >" } ?: "Next >", color = if (nextSummary != null) Color(0xFF9993FF) else Color(0xFF586174), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = nextSummary != null) { nextSummary?.termKey?.let(onNavigate) })
                    }
                }
            }
        }
        DictionaryBottomNavigation(
            bookmarksOnly = detail.isBookmarked,
            onLearn = onBack,
            onDictionary = onBack,
            onFavorites = { onBookmark(detail) },
            onProfile = {},
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun DictionaryDetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0x9919233A), Color(0x7A0E182C))))
            .border(1.dp, Color(0xFF2A3652), RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun DictionarySmallPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0x8819233A), Color(0x660E182C))))
            .border(1.dp, Color(0xFF2A3652), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun AbsoluteValueNumberLine(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val axisY = size.height * .56f
        val startX = size.width * .08f
        val endX = size.width * .92f
        val step = (endX - startX) / 10f
        drawLine(Color(0xFFB7C0D8), Offset(startX, axisY), Offset(endX, axisY), 3f)
        drawLine(Color(0xFFB7C0D8), Offset(startX, axisY), Offset(startX + 12f, axisY - 12f), 3f)
        drawLine(Color(0xFFB7C0D8), Offset(startX, axisY), Offset(startX + 12f, axisY + 12f), 3f)
        drawLine(Color(0xFFB7C0D8), Offset(endX, axisY), Offset(endX - 12f, axisY - 12f), 3f)
        drawLine(Color(0xFFB7C0D8), Offset(endX, axisY), Offset(endX - 12f, axisY + 12f), 3f)
        for (n in -5..5) {
            val x = startX + (n + 5) * step
            drawLine(Color(0xFFB7C0D8), Offset(x, axisY - 10f), Offset(x, axisY + 10f), 2.2f)
            drawGraphNumber(n.toString(), Offset(x - 9f, axisY + 34f), if (n == -4 || n == 4) Color(0xFF8581FF) else Color(0xFFB7C0D8))
        }
        val zeroX = startX + 5 * step
        val leftX = startX + 1 * step
        val rightX = startX + 9 * step
        drawCircle(Color(0xFF8581FF), 8f, Offset(leftX, axisY))
        drawCircle(Color(0xFF8581FF), 8f, Offset(rightX, axisY))
        drawArc(Color(0xFF8581FF), 200f, 140f, false, topLeft = Offset(leftX, axisY - 64f), size = androidx.compose.ui.geometry.Size(zeroX - leftX, 76f), style = Stroke(3f))
        drawArc(Color(0xFF8581FF), 200f, 140f, false, topLeft = Offset(zeroX, axisY - 64f), size = androidx.compose.ui.geometry.Size(rightX - zeroX, 76f), style = Stroke(3f))
        drawGraphNumber("4 units", Offset(leftX + (zeroX - leftX) * .36f, axisY - 78f), Color.White)
        drawGraphNumber("4 units", Offset(zeroX + (rightX - zeroX) * .36f, axisY - 78f), Color.White)
        drawGraphNumber("|-4| = 4  and  |4| = 4", Offset(size.width * .31f, axisY + 72f), Color(0xFFDDE6FF))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGraphNumber(text: String, position: Offset, color: Color) {
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
        }
        drawText(text, position.x, position.y, paint)
    }
}

private fun samplePromptFor(word: String): String = when {
    word.contains("absolute", true) -> "Find |-7|"
    word.contains("angle", true) -> "Name this angle"
    word.contains("matrix", true) -> "Find the order"
    else -> "Try one example"
}

private fun sampleAnswerFor(word: String): String = when {
    word.contains("absolute", true) -> "7"
    word.contains("angle", true) -> "Angle"
    word.contains("matrix", true) -> "m x n"
    else -> "Check"
}

private fun tipFor(word: String, analogy: String): String = when {
    analogy.isNotBlank() -> analogy
    word.contains("absolute", true) -> "Think distance, not direction."
    word.contains("angle", true) -> "Look at the turn between two rays."
    else -> "Connect the word to a small example."
}
@Composable
private fun DictionaryChip(label: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .12f)).border(1.dp, color.copy(alpha = .42f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun EmptyDictionaryState(message: String, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xAA18233A)).border(1.dp, Color(0xFF2A3652), RoundedCornerShape(18.dp)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message, color = Color(0xFFEAF0FF), fontWeight = FontWeight.Bold)
        GlowButton("Clear filters", icon = "X", onClick = onClear)
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF2A3652)))
}

@Composable
private fun DictionaryConceptIcon(word: String, asset: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF28295F), Color(0xFF111A33)))), contentAlignment = Alignment.Center) {
        if (asset.isBlank()) {
            TransparentIcon(mathSymbolForWord(word), Color(0xFF8581FF))
        } else {
            DictionaryConceptSketch(word, Modifier.fillMaxSize().padding(8.dp))
        }
    }
}

private fun mathSymbolForWord(word: String): String = when {
    word.contains("absolute", true) -> "|x|"
    word.contains("angle", true) -> "angle"
    word.contains("circle", true) -> "circle"
    word.contains("matrix", true) -> "[ ]"
    word.contains("vector", true) -> "vec"
    word.contains("root", true) -> "root"
    word.contains("square", true) -> "x^2"
    word.contains("sum", true) || word.contains("series", true) -> "sum"
    word.contains("integral", true) -> "int"
    else -> word.take(2).uppercase()
}

@Composable
private fun DictionaryConceptSketch(word: String, modifier: Modifier) {
    Canvas(modifier) {
        val blue = Color(0xFF0D56DB)
        val purple = Color(0xFF5B37D6)
        val light = Color(0xFFD9E7FF)
        when {
            word.contains("angle", true) -> {
                drawLine(purple, Offset(size.width * .2f, size.height * .75f), Offset(size.width * .8f, size.height * .75f), 4f)
                drawLine(purple, Offset(size.width * .2f, size.height * .75f), Offset(size.width * .75f, size.height * .22f), 4f)
                drawArc(blue, -45f, 45f, false, topLeft = Offset(size.width * .23f, size.height * .45f), size = androidx.compose.ui.geometry.Size(size.width * .34f, size.height * .34f), style = Stroke(3f))
            }
            word.contains("circle", true) -> drawCircle(blue, size.minDimension * .32f, center, style = Stroke(5f))
            word.contains("triangle", true) -> {
                val path = Path().apply {
                    moveTo(size.width * .5f, size.height * .16f)
                    lineTo(size.width * .18f, size.height * .82f)
                    lineTo(size.width * .84f, size.height * .82f)
                    close()
                }
                drawPath(path, purple, style = Stroke(5f))
            }
            word.contains("matrix", true) -> {
                repeat(3) { row -> repeat(3) { col -> drawCircle(if ((row + col) % 2 == 0) blue else purple, 5f, Offset(size.width * (.28f + col * .22f), size.height * (.28f + row * .22f))) } }
            }
            word.contains("vector", true) || word.contains("slope", true) -> {
                drawLine(blue, Offset(size.width * .2f, size.height * .75f), Offset(size.width * .78f, size.height * .25f), 5f)
                drawCircle(purple, 7f, Offset(size.width * .78f, size.height * .25f))
            }
            word.contains("mean", true) || word.contains("bar", true) -> {
                listOf(.65f, .42f, .78f, .3f).forEachIndexed { index, h -> drawRect(if (index % 2 == 0) blue else purple, Offset(size.width * (.18f + index * .16f), size.height * h), androidx.compose.ui.geometry.Size(size.width * .09f, size.height * (.84f - h))) }
                drawLine(Green, Offset(size.width * .12f, size.height * .55f), Offset(size.width * .84f, size.height * .55f), 3f)
            }
            else -> {
                repeat(7) { index ->
                    val angle = index * 2.0 * PI / 7.0
                    val point = Offset(center.x + cos(angle).toFloat() * size.width * .28f, center.y + sin(angle).toFloat() * size.height * .28f)
                    drawLine(light, center, point, 2f)
                    drawCircle(if (index % 2 == 0) blue else purple, 7f, point)
                }
                drawCircle(Green, 8f, center)
            }
        }
    }
}





