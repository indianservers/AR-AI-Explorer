package com.indianservers.aiexplorer.biology.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.indianservers.aiexplorer.biology.future3d.FUTURE_3D_MESSAGE
import com.indianservers.aiexplorer.biology.model.*
import com.indianservers.aiexplorer.biology.navigation.BiologyFeatureViewModel
import com.indianservers.aiexplorer.biology.navigation.BiologyRoute

private val BioBackground = Color(0xFF050C0C); private val BioPanel = Color(0xEB0E2020)
private val BioGreen = Color(0xFF70E0A1); private val BioCyan = Color(0xFF60DDE5); private val BioViolet = Color(0xFFB5A1FF)
private val BioAmber = Color(0xFFFFC970); private val BioInk = Color(0xFFF2FAF7); private val BioMuted = Color(0xFFA1B8B1)

@Composable
fun BiologyHomePage(vm: BiologyFeatureViewModel, onExit: () -> Unit, onOpenConnectedJourney: () -> Unit = {}) {
    val state = vm.state; val repository = vm.repository; val validation = repository.validate(); val domains = repository.getDomains()
    val recommendedTopics = domains.take(3).mapNotNull { domain -> repository.getUnits(domain.id).firstOrNull() }
        .mapNotNull { unit -> repository.getChapters(unit.id).firstOrNull() }
        .mapNotNull { chapter -> repository.getTopics(chapter.id).firstOrNull() }
    val newTopics = domains.takeLast(3).mapNotNull { domain -> repository.getUnits(domain.id).firstOrNull() }
        .mapNotNull { unit -> repository.getChapters(unit.id).firstOrNull() }
        .mapNotNull { chapter -> repository.getTopics(chapter.id).firstOrNull() }
    BiologyPage {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("BIOLOGY EXPLORER", color = BioGreen, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("Explore life from molecules and cells to organisms, ecosystems and advanced biomedical science", color = BioMuted, fontSize = 11.sp)
            }
            BioButton("Subjects", onExit)
        }
        BioPanel("Learning level") { LevelSelector(state.learningLevel, vm::setLearningLevel); Text("Explanations and visible topics adapt progressively; advanced material remains available by choice.", color = BioMuted, fontSize = 10.sp) }
        BioActionCard("CONNECTED LEARNING · CELL TO HOMEOSTASIS", "Learn → Explore → Test with diagrams, processes and prerequisites", BioViolet, Modifier.fillMaxWidth(), onOpenConnectedJourney)
        BioPanel("Search Biology") {
            OutlinedTextField(state.searchQuery, vm::search, Modifier.fillMaxWidth().semantics { contentDescription = "Search Biology domains, units, chapters, topics and glossary" }, label = { Text("Search cells, heart, DNA, ecology…") }, singleLine = true)
            state.searchResults.take(8).forEach { result -> SearchResultCard(result) { vm.openSearchResult(result) } }
            if (state.searchQuery.isNotBlank() && state.searchResults.isEmpty()) Text("No results at the selected learning level.", color = BioAmber, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioActionCard("Explore all Biology", "${domains.size} scientific domains", BioCyan, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Catalogue) }
            BioActionCard("Glossary", "${repository.getGlossaryTerms().size} linked terms", BioViolet, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Glossary) }
            BioActionCard("Progress", "${state.completedConcepts.size} completed", BioGreen, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Progress) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioActionCard("Bookmarks", "${state.bookmarks.size} saved", BioAmber, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Bookmarks) }
            BioActionCard("Quick revision", "5, 15 or full chapter", BioViolet, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Revision("recommended")) }
            BioActionCard("Quiz challenge", "Foundation route ready", BioCyan, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Quiz("challenge")) }
        }
        BioPanel("Recommended learning path") {
            recommendedTopics.forEachIndexed { index, topic ->
                NodeCard(topic.title, topic.description, "Step ${index + 1}", BioGreen) { vm.navigate(BiologyRoute.Topic(topic.id)) }
            }
        }
        BioPanel("New topics to explore") {
            newTopics.forEach { topic -> NodeCard(topic.title, topic.description, topic.minimumLevel.label, BioCyan) { vm.navigate(BiologyRoute.Topic(topic.id)) } }
        }
        BioPanel("Catalogue status") {
            Text(if (validation.valid) "✓ Offline hierarchy validated" else "Content validation needs attention", color = if (validation.valid) BioGreen else BioAmber, fontWeight = FontWeight.Bold)
            Text("${repository.getDomains().size} domains · select a domain to follow Domain → Unit → Chapter → Topic → Concept", color = BioMuted, fontSize = 10.sp)
        }
        Text("Major domains", color = BioViolet, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        domains.take(10).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                row.forEach { domain -> DomainCard(domain, Modifier.weight(1f)) { vm.navigate(BiologyRoute.Domain(domain.id)) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        BioButton("All 22 domains") { vm.navigate(BiologyRoute.Catalogue) }
        BioPanel("Continue learning · recently viewed") {
            if (state.recentlyViewed.isEmpty()) Text("Open a concept and it will appear here for this session.", color = BioMuted, fontSize = 10.sp)
            state.recentlyViewed.take(3).mapNotNull(repository::getConcept).forEach { concept -> NodeCard(concept.title, concept.summary, "${concept.estimatedMinutes} min", BioGreen) { vm.navigate(BiologyRoute.Concept(concept.id)) } }
        }
        Text("Bookmarks, explicit completion, search, routes and progress state are active. Local persistence is scheduled for the personal-learning phase.", color = BioMuted, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun BiologyCataloguePage(vm: BiologyFeatureViewModel) = NodeListPage(vm, "All Biology Domains", "Complete offline catalogue · ${vm.repository.getDomains().size} domains") {
    vm.repository.getDomains().forEach { domain -> DomainCard(domain, Modifier.fillMaxWidth()) { vm.navigate(BiologyRoute.Domain(domain.id)) } }
}

@Composable
fun BiologyDomainPage(vm: BiologyFeatureViewModel, id: String) {
    val domain = vm.repository.getDomain(id) ?: return InvalidPage(vm, "Unknown domain")
    NodeListPage(vm, domain.title, domain.description) {
        InfoStrip("Domain", "${vm.repository.getUnits(id).size} units · ${domain.estimatedMinutes} estimated minutes", domain.iconText)
        vm.repository.getUnits(id).forEach { unit -> NodeCard(unit.title, unit.description, "${unit.chapterIds.size} chapters", BioCyan) { vm.navigate(BiologyRoute.Unit(unit.id)) } }
    }
}

@Composable
fun BiologyUnitPage(vm: BiologyFeatureViewModel, id: String) {
    val unit = vm.repository.getUnit(id) ?: return InvalidPage(vm, "Unknown unit")
    NodeListPage(vm, unit.title, unit.description) {
        vm.repository.getChapters(id).forEach { chapter -> NodeCard(chapter.title, chapter.description, "${chapter.topicIds.size} topics", BioViolet) { vm.navigate(BiologyRoute.Chapter(chapter.id)) } }
    }
}

@Composable
fun BiologyChapterPage(vm: BiologyFeatureViewModel, id: String) {
    val chapter = vm.repository.getChapter(id) ?: return InvalidPage(vm, "Unknown chapter")
    val all = vm.repository.getTopics(id); val visible = all.filter { it.minimumLevel.rank <= vm.state.learningLevel.rank }
    NodeListPage(vm, chapter.title, chapter.description) {
        if (visible.size < all.size) Text("${all.size - visible.size} advanced topics hidden by the current ${vm.state.learningLevel.label} profile.", color = BioAmber, fontSize = 10.sp)
        visible.forEach { topic -> NodeCard(topic.title, topic.description, "${topic.estimatedMinutes} min${if (topic.future3DObjectId != null) " · 3D-ready" else ""}", BioGreen) { vm.navigate(BiologyRoute.Topic(topic.id)) } }
    }
}

@Composable
fun BiologyTopicPage(vm: BiologyFeatureViewModel, id: String) {
    val topic = vm.repository.getTopic(id) ?: return InvalidPage(vm, "Unknown topic")
    NodeListPage(vm, topic.title, topic.description) {
        topic.conceptIds.mapNotNull(vm.repository::getConcept).forEach { concept ->
            NodeCard(concept.title, concept.summary, if (concept.status == BiologyContentStatus.Complete) "Complete sample lesson" else "Safe overview · detailed lesson developing", if (concept.status == BiologyContentStatus.Complete) BioGreen else BioAmber) { vm.navigate(BiologyRoute.Concept(concept.id)) }
        }
    }
}

@Composable
fun BiologyConceptPage(vm: BiologyFeatureViewModel, id: String) {
    val concept = vm.repository.getConcept(id) ?: return InvalidPage(vm, "Unknown concept")
    var showAdvanced by remember(concept.id) { mutableStateOf(false) }
    val visibleRank = if (showAdvanced) BiologyLearningLevel.POSTGRADUATE.rank else vm.state.learningLevel.rank
    BiologyPage {
        PageHeader(vm, concept.title, "Concept reading page")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Badge(vm.state.learningLevel.label, BioCyan); Badge("${concept.estimatedMinutes} min", BioViolet); Badge(if (concept.status == BiologyContentStatus.Complete) "Sample lesson" else "Overview ready", if (concept.status == BiologyContentStatus.Complete) BioGreen else BioAmber)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BioButton(if (concept.id in vm.state.bookmarks) "Remove bookmark" else "Bookmark") { vm.toggleBookmark(concept.id) }
            BioButton(if (concept.id in vm.state.completedConcepts) "Completed ✓" else "Mark complete") { vm.toggleComplete(concept.id) }
            BioButton(if (showAdvanced) "Use level profile" else "Show advanced") { showAdvanced = !showAdvanced }
        }
        BioPanel("Overview") { Text(concept.summary, color = BioInk, fontSize = 13.sp); concept.learningObjectives.forEach { Text("• $it", color = BioMuted, fontSize = 11.sp) } }
        concept.diagramIds.firstOrNull()?.let { diagramId -> vm.repository.getConcept(concept.id)?.let { DiagramPlaceholder(concept.title, diagramId) } }
        concept.blocks.filter { it.minimumLevel.rank <= visibleRank }.forEach { block ->
            ContentBlockView(block)
        }
        if (concept.future3DObjectId != null) BioPanel("Future 3D readiness") { Text(FUTURE_3D_MESSAGE, color = BioViolet, fontWeight = FontWeight.Bold); Text("A validated 2D fallback diagram is linked. No fake 3D controls or assets are loaded.", color = BioMuted, fontSize = 10.sp) }
        if (concept.status == BiologyContentStatus.OverviewReady) BioPanel("Detailed lesson under development") {
            Text("This route is safe and informative; it never opens an empty page.", color = BioAmber, fontSize = 11.sp)
            concept.plannedSections.forEach { Text("• $it", color = BioMuted, fontSize = 10.sp) }
        }
        BioPanel("Related concepts") { vm.repository.getRelatedConcepts(concept.id).forEach { related -> NodeCard(related.title, related.summary, "Related", BioCyan) { vm.navigate(BiologyRoute.Concept(related.id)) } } }
        BioPanel("Educational note") { Text("Human-health material is educational and does not provide diagnosis or personalised treatment.", color = BioMuted, fontSize = 10.sp) }
    }
}

@Composable fun BiologySearchPage(vm: BiologyFeatureViewModel) = NodeListPage(vm, "Biology Search", "Search the versioned offline catalogue") { OutlinedTextField(vm.state.searchQuery, vm::search, Modifier.fillMaxWidth(), label = { Text("Search") }); vm.state.searchResults.forEach { SearchResultCard(it) { vm.openSearchResult(it) } } }
@Composable fun BiologyGlossaryPage(vm: BiologyFeatureViewModel) = NodeListPage(vm, "Biology Glossary", "School and advanced definitions with concept links") { vm.repository.getGlossaryTerms().forEach { term -> NodeCard(term.term, term.schoolDefinition, term.advancedDefinition, BioViolet) { term.conceptId?.let { vm.navigate(BiologyRoute.Concept(it)) } } } }
@Composable fun BiologyBookmarksPage(vm: BiologyFeatureViewModel) = NodeListPage(vm, "Bookmarks", "Saved during this app session") { val saved = vm.state.bookmarks.mapNotNull(vm.repository::getConcept); if (saved.isEmpty()) Text("No Biology bookmarks yet.", color = BioMuted) else saved.forEach { NodeCard(it.title, it.summary, "Bookmarked", BioAmber) { vm.navigate(BiologyRoute.Concept(it.id)) } } }
@Composable fun BiologyProgressPage(vm: BiologyFeatureViewModel) = NodeListPage(vm, "Biology Progress", "Completion is explicit, never inferred from opening a page") { val all = vm.repository.getDomains().sumOf { domain -> vm.repository.getUnits(domain.id).sumOf { unit -> vm.repository.getChapters(unit.id).sumOf { chapter -> vm.repository.getTopics(chapter.id).size } } }; val fraction = if (all == 0) 0f else vm.state.completedConcepts.size.toFloat() / all; LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth()); Text("${vm.state.completedConcepts.size} of $all concepts completed", color = BioGreen) }
@Composable fun PlannedBiologyPage(vm: BiologyFeatureViewModel, message: String) = BiologyPage { PageHeader(vm, "Planned Biology destination", "Route registered safely"); BioPanel("Coming in a controlled phase") { Text(message, color = BioAmber); Text("This page deliberately exposes no fake interaction.", color = BioMuted, fontSize = 10.sp) } }

@Composable
private fun NodeListPage(vm: BiologyFeatureViewModel, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) = BiologyPage { PageHeader(vm, title, subtitle); content() }

@Composable
private fun PageHeader(vm: BiologyFeatureViewModel, title: String, subtitle: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { BioButton("Back") { vm.back() }; Column(Modifier.weight(1f)) { Text(title, color = BioGreen, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = BioMuted, fontSize = 10.sp) }; BioButton("Home", vm::home) } }

@Composable
private fun BiologyPage(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF15312A), BioBackground), radius = 1300f)).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content) }

@Composable
private fun LevelSelector(selected: BiologyLearningLevel, onSelect: (BiologyLearningLevel) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BiologyLearningLevel.entries.forEach { level ->
            Text(level.label, color = if (selected == level) BioBackground else BioInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(if (selected == level) BioGreen else BioPanel).border(1.dp, BioGreen.copy(.5f), RoundedCornerShape(13.dp)).clickable { onSelect(level) }.padding(horizontal = 11.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun DomainCard(domain: BiologyDomain, modifier: Modifier, onClick: () -> Unit) { Row(modifier.heightIn(min = 104.dp).clip(RoundedCornerShape(17.dp)).background(BioPanel).border(1.dp, BioGreen.copy(.35f), RoundedCornerShape(17.dp)).clickable(onClick = onClick).semantics { contentDescription = "Open Biology domain ${domain.title}" }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(BioGreen.copy(.12f)).border(1.dp, BioGreen.copy(.4f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) { Text(domain.iconText, color = BioGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f)) { Text(domain.title, color = BioInk, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(domain.description, color = BioMuted, fontSize = 9.sp, maxLines = 3); Text("${domain.unitIds.size} units", color = BioGreen, fontSize = 9.sp) } } }

@Composable
private fun NodeCard(title: String, description: String, metadata: String, accent: Color, onClick: () -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(BioPanel).border(1.dp, accent.copy(.35f), RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(title, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(description, color = BioInk, fontSize = 10.sp); Text(metadata, color = BioMuted, fontSize = 9.sp) } }

@Composable
private fun SearchResultCard(result: BiologySearchResult, onClick: () -> Unit) = NodeCard(result.title, result.context, "${result.type} · ${result.minimumLevel.label}${if (result.hasDiagram) " · diagram" else ""}${if (result.future3DReady) " · 3D-ready" else ""}", BioCyan, onClick)

@Composable
private fun BioActionCard(title: String, subtitle: String, accent: Color, modifier: Modifier, onClick: () -> Unit) { Column(modifier.heightIn(min = 92.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(.1f)).border(1.dp, accent.copy(.5f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(11.dp)) { Text(title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = BioMuted, fontSize = 9.sp) } }

@Composable
private fun InfoStrip(label: String, value: String, icon: String) { Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(BioCyan.copy(.09f)).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("$icon · $label", color = BioCyan, fontWeight = FontWeight.Bold); Text(value, color = BioMuted, fontSize = 10.sp) } }

@Composable
private fun DiagramPlaceholder(title: String, id: String) { BioPanel("2D diagram · $title") { Canvas(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF071514)).semantics { contentDescription = "$title diagram placeholder. Primary structure is centred and text alternative is available." }) { val center = Offset(size.width * .5f, size.height * .52f); drawCircle(BioCyan.copy(.12f), size.minDimension * .31f, center); drawCircle(BioCyan, size.minDimension * .31f, center, style = Stroke(4f)); drawCircle(BioViolet.copy(.2f), size.minDimension * .12f, center); drawCircle(BioViolet, size.minDimension * .12f, center, style = Stroke(3f)); repeat(6) { index -> val y = size.height * (.22f + index * .1f); drawLine(BioGreen.copy(.55f), Offset(size.width * .25f, y), Offset(size.width * .38f, y + 12f), 3f, cap = StrokeCap.Round) } }; Text("Diagram contract: $id · labelled zoom/pan viewer arrives in Phase 5.", color = BioMuted, fontSize = 9.sp) } }

@Composable
private fun ContentBlockView(block: BiologyContentBlock) { when (block) {
    is BiologyContentBlock.Paragraph -> Text(block.text, color = BioInk, fontSize = 12.sp)
    is BiologyContentBlock.Heading -> Text(block.text, color = BioCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    is BiologyContentBlock.BulletGroup -> BioPanel("Key points") { block.items.forEach { Text("• $it", color = BioInk, fontSize = 11.sp) } }
    is BiologyContentBlock.NumberedSteps -> BioPanel("Process") { block.steps.forEachIndexed { index, step -> Text("${index + 1}. $step", color = BioInk, fontSize = 11.sp) } }
    is BiologyContentBlock.KeyFact -> BioPanel("Key fact") { Text(block.text, color = BioGreen, fontSize = 12.sp) }
    is BiologyContentBlock.Definition -> BioPanel("Definition · ${block.term}") { Text(block.definition, color = BioInk, fontSize = 12.sp) }
    is BiologyContentBlock.Diagram -> Text("Diagram: ${block.diagramId}", color = BioCyan)
    is BiologyContentBlock.Comparison -> Text(block.title, color = BioViolet)
    is BiologyContentBlock.DataTable -> Text(block.title, color = BioViolet)
    is BiologyContentBlock.Formula -> BioPanel(block.expression) { Text(block.explanation, color = BioMuted) }
    is BiologyContentBlock.Misconception -> BioPanel("Common misconception") { Text("Claim: ${block.claim}", color = BioAmber); Text("Correction: ${block.correction}", color = BioInk) }
    is BiologyContentBlock.ClinicalContext -> BioPanel("Clinical context") { Text(block.text, color = BioInk) }
    is BiologyContentBlock.ResearchInsight -> BioPanel("Research insight") { Text(block.text, color = BioViolet) }
    is BiologyContentBlock.Warning -> BioPanel("Important") { Text(block.text, color = BioAmber) }
} }

@Composable private fun Badge(text: String, accent: Color) { Text(text, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(accent.copy(.1f)).border(1.dp, accent.copy(.4f), RoundedCornerShape(11.dp)).padding(horizontal = 8.dp, vertical = 6.dp)) }
@Composable private fun BioPanel(title: String, content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Brush.linearGradient(listOf(Color(0xED15312B), BioPanel))).border(1.dp, BioGreen.copy(.22f), RoundedCornerShape(17.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, color = BioGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold); content() } }
@Composable private fun BioButton(label: String, onClick: () -> Unit) { Button(onClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF17372F), contentColor = BioInk), shape = RoundedCornerShape(13.dp), modifier = Modifier.heightIn(min = 44.dp)) { Text(label, fontSize = 10.sp) } }
@Composable private fun InvalidPage(vm: BiologyFeatureViewModel, message: String) { BiologyPage { PageHeader(vm, "Content unavailable", "Recoverable navigation error"); BioPanel("Unable to open") { Text(message, color = BioAmber); Text("Return to the catalogue and choose a valid destination.", color = BioMuted) } } }
