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

    LazyColumn(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF071B3D), Color(0xFF081326), SurfaceA)))
            .padding(horizontal = if (wide) 18.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Math Dictionary", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("Understand every maths word", color = Color(0xFFDCEAFF), fontSize = 15.sp)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search ${stats.termCount.coerceAtLeast(300)} terms") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                )
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton(if (bookmarksOnly) "Saved only" else "All words", icon = if (bookmarksOnly) "S" else "A") { bookmarksOnly = !bookmarksOnly }
                selectedCategory?.let { GlowButton("Clear category", icon = "X") { selectedCategory = null } }
                selectedDifficulty?.let { GlowButton("Clear level", icon = "X") { selectedDifficulty = null } }
                if (selectedLetter != null || query.isNotBlank()) GlowButton("Clear filters", icon = "X") {
                    selectedLetter = null
                    query = ""
                    debouncedQuery = ""
                    selectedCategory = null
                    selectedDifficulty = null
                    bookmarksOnly = false
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ('A'..'Z').forEach { letter ->
                    val active = selectedLetter == letter.toString()
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) Color(0xFF1D62F0) else Color.White.copy(alpha = .94f))
                            .border(1.dp, if (active) Cyan else Muted.copy(.24f), RoundedCornerShape(20.dp))
                            .clickable { selectedLetter = if (active) null else letter.toString() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(letter.toString(), color = if (active) Color.White else Ink, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                stats.categoryCounts.keys.forEach { category ->
                    GlowButton(if (selectedCategory == category) "• $category" else category) {
                        selectedCategory = if (selectedCategory == category) null else category
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Beginner", "Intermediate", "Advanced").forEach { difficulty ->
                    GlowButton(if (selectedDifficulty == difficulty) "• $difficulty" else difficulty) {
                        selectedDifficulty = if (selectedDifficulty == difficulty) null else difficulty
                    }
                }
            }
        }
        item {
            Text(
                if (loading) "Loading dictionary..." else "${summaries.size} words found",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        if (!loading && summaries.isEmpty()) {
            item {
                EmptyDictionaryState(
                    if (selectedLetter != null) "No words under ${selectedLetter.orEmpty()} with these filters." else "No dictionary words match this search.",
                    onClear = {
                        query = ""
                        debouncedQuery = ""
                        selectedLetter = null
                        selectedCategory = null
                        selectedDifficulty = null
                        bookmarksOnly = false
                    },
                )
            }
        }
        items(summaries, key = { it.termKey }) { term ->
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
        item { Spacer(Modifier.height(18.dp)) }
    }
}

@Composable
private fun DictionaryListCard(term: MathDictionaryTermSummary, onOpen: () -> Unit, onBookmark: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .96f))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DictionaryConceptIcon(term.word, term.imageAsset, Modifier.size(58.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(term.word, color = Color(0xFF111733), fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(if (term.isBookmarked) "■" else "□", color = Color(0xFF0D56DB), fontSize = 22.sp, modifier = Modifier.clickable(onClick = onBookmark).padding(4.dp))
            }
            Text(term.shortDefinition, color = Color(0xFF26324D), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                DictionaryChip(term.category, Color(0xFF0D56DB))
                DictionaryChip(term.difficultyLevel, if (term.difficultyLevel == "Advanced") Violet else Green)
            }
        }
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
    val previous = summaries.getOrNull(currentIndex - 1)?.termKey
    val next = summaries.getOrNull(currentIndex + 1)?.termKey
    LazyColumn(
        modifier.fillMaxSize().background(Color.White).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("←", color = Color(0xFF111733), fontSize = 30.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
                Text(if (detail.isBookmarked) "■" else "□", color = Color(0xFF0D56DB), fontSize = 28.sp, modifier = Modifier.clickable { onBookmark(detail) }.padding(8.dp))
            }
        }
        item {
            DictionaryConceptIcon(detail.word, detail.imageAsset, Modifier.size(72.dp))
            Spacer(Modifier.height(10.dp))
            Text(detail.word, color = Color(0xFF111733), fontSize = 40.sp, fontWeight = FontWeight.Black)
            if (detail.pronunciation.isNotBlank()) Text("🔊  ${detail.pronunciation}", color = Color(0xFF4B5563), fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DictionaryChip(detail.category, Color(0xFF0D56DB))
                DictionaryChip(detail.difficultyLevel, Violet)
                detail.minimumGrade?.let { DictionaryChip("Grade $it+", Green) }
            }
        }
        item { DividerLine() }
        item { DetailSection("Meaning", detail.detailedDefinition) }
        if (detail.formulaLatex.isNotBlank()) {
            item {
                DetailPanel("Formula") {
                    MathFormulaText(detail.formulaLatex, color = Color(0xFF111733), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            DetailPanel("Simple Example") {
                Text(detail.simpleExample.removePrefix("Example: "), color = Color(0xFF111733), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(detail.exampleExplanation, color = Color(0xFF26324D), fontSize = 13.sp)
                DictionaryConceptSketch(detail.word, Modifier.fillMaxWidth().height(86.dp))
            }
        }
        if (detail.analogy.isNotBlank()) item { DetailSection("Think of it like this", detail.analogy) }
        if (detail.relatedTerms.isNotEmpty()) {
            item {
                Text("Related Words", color = Color(0xFF111733), fontSize = 18.sp, fontWeight = FontWeight.Black)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    detail.relatedTerms.forEach { related ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(Color(0xFFEFF6FF))
                                .border(1.dp, Color(0xFF0D56DB), RoundedCornerShape(13.dp))
                                .clickable { onOpenRelated(related.termKey) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(related.word, color = Color(0xFF0D56DB), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = previous != null, onClick = { previous?.let(onNavigate) }) { Text("Previous") }
                TextButton(enabled = next != null, onClick = { next?.let(onNavigate) }) { Text("Next") }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, color = Color(0xFF111733), fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(body, color = Color(0xFF26324D), fontSize = 15.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun DetailPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFF8FAFF)).border(1.dp, Color(0xFFD7E4FF), RoundedCornerShape(14.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            Text(title, color = Color(0xFF111733), fontSize = 18.sp, fontWeight = FontWeight.Black)
            content()
        },
    )
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.94f)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(message, color = Color(0xFF111733), fontWeight = FontWeight.Bold)
        GlowButton("Clear filters", icon = "X", onClick = onClear)
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE5E7EB)))
}

@Composable
private fun DictionaryConceptIcon(word: String, asset: String, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(99.dp)).background(Color(0xFFE8F1FF)), contentAlignment = Alignment.Center) {
        if (asset.isBlank()) {
            TransparentIcon(word.take(2).uppercase(), Color(0xFF0D56DB))
        } else {
            DictionaryConceptSketch(word, Modifier.fillMaxSize().padding(8.dp))
        }
    }
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
