package com.indianservers.aiexplorer

import com.indianservers.aiexplorer.biology.data.BundledBiologyCatalogue
import com.indianservers.aiexplorer.biology.domain.BiologyCatalogueValidator
import com.indianservers.aiexplorer.biology.model.*
import com.indianservers.aiexplorer.biology.navigation.BiologyFeatureViewModel
import com.indianservers.aiexplorer.biology.navigation.BiologyRoute
import com.indianservers.aiexplorer.biology.repository.OfflineBiologyRepository
import org.junit.Assert.*
import org.junit.Test

class BiologyPhase1And2Test {
    private val repository = OfflineBiologyRepository()

    @Test fun loadsCompleteTwentyTwoDomainCatalogue() {
        assertEquals(22, repository.getDomains().size)
        assertEquals(22, repository.getDomains().map { it.id }.toSet().size)
        assertTrue(repository.getDomains().all { it.unitIds.isNotEmpty() })
    }

    @Test fun domainUnitChapterTopicConceptHierarchyIsNavigable() {
        repository.getDomains().forEach { domain ->
            val units = repository.getUnits(domain.id); assertEquals(domain.unitIds, units.map { it.id })
            units.forEach { unit ->
                val chapters = repository.getChapters(unit.id); assertEquals(unit.chapterIds, chapters.map { it.id })
                chapters.forEach { chapter ->
                    val topics = repository.getTopics(chapter.id); assertEquals(chapter.topicIds, topics.map { it.id }); assertTrue(topics.size <= 6)
                    topics.forEach { topic -> assertNotNull(repository.getConceptForTopic(topic.id)) }
                }
            }
        }
    }

    @Test fun requestedMajorDomainsAndTopicsArePresent() {
        val titles = repository.getDomains().map { it.title }.toSet()
        assertTrue("Cell Biology" in titles); assertTrue("Human Anatomy" in titles); assertTrue("Research Methods and Advanced Biology" in titles)
        assertNotNull(repository.search("Cell Theory", BiologyLearningLevel.POSTGRADUATE).firstOrNull { it.title == "Cell Theory" })
        assertNotNull(repository.search("Human Heart", BiologyLearningLevel.POSTGRADUATE).firstOrNull { it.title == "Human Heart" })
        assertNotNull(repository.search("CRISPR", BiologyLearningLevel.POSTGRADUATE).firstOrNull())
    }

    @Test fun searchSupportsHierarchyKeywordsAndGlossary() {
        assertTrue(repository.search("ecosystem", BiologyLearningLevel.POSTGRADUATE).any { it.title.contains("Ecosystem") })
        assertTrue(repository.search("antibody", BiologyLearningLevel.POSTGRADUATE).any { it.title.contains("Antibod", true) })
        assertTrue(repository.search("osmosis", BiologyLearningLevel.CLASS_10).any { it.title == "Osmosis" })
    }

    @Test fun learningLevelFiltersAdvancedSearchResults() {
        val foundation = repository.search("Mass Spectrometry", BiologyLearningLevel.FOUNDATION)
        val postgraduate = repository.search("Mass Spectrometry", BiologyLearningLevel.POSTGRADUATE)
        assertTrue(foundation.isEmpty())
        assertTrue(postgraduate.any { it.title == "Mass Spectrometry Foundations" })
    }

    @Test fun progressiveContentBlocksRespectLearningDepth() {
        val cellTheoryResult = repository.search("Cell Theory", BiologyLearningLevel.POSTGRADUATE).first { it.title == "Cell Theory" }
        val concept = repository.getConceptForTopic(cellTheoryResult.id)!!
        assertTrue(concept.blocks.any { it.minimumLevel == BiologyLearningLevel.FOUNDATION })
        assertTrue(concept.blocks.any { it.minimumLevel == BiologyLearningLevel.POSTGRADUATE })
        assertTrue(concept.blocks.count { it.minimumLevel.rank <= BiologyLearningLevel.CLASS_10.rank } < concept.blocks.size)
    }

    @Test fun incompleteConceptsHaveSafeNonEmptyOverviewPages() {
        BundledBiologyCatalogue.catalogue.concepts.filter { it.status == BiologyContentStatus.OverviewReady }.forEach { concept ->
            assertTrue(concept.summary.isNotBlank()); assertTrue(concept.learningObjectives.isNotEmpty()); assertTrue(concept.plannedSections.isNotEmpty()); assertTrue(concept.blocks.isNotEmpty())
        }
    }

    @Test fun glossaryLinksResolveWhenProvided() {
        repository.getGlossaryTerms().forEach { term ->
            assertNotNull(repository.getDomain(term.domainId))
            term.conceptId?.let { assertNotNull(repository.getConcept(it)) }
        }
    }

    @Test fun relatedConceptLinksResolve() {
        BundledBiologyCatalogue.catalogue.concepts.forEach { concept ->
            assertEquals(concept.relatedConceptIds.size, repository.getRelatedConcepts(concept.id).size)
        }
    }

    @Test fun diagramsAndFuture3DMetadataHaveValidFallbacks() {
        val catalogue = BundledBiologyCatalogue.catalogue; val diagramIds = catalogue.diagrams.map { it.id }.toSet(); val conceptIds = catalogue.concepts.map { it.id }.toSet()
        assertTrue(catalogue.diagrams.isNotEmpty()); assertTrue(catalogue.future3D.isNotEmpty())
        catalogue.future3D.forEach { metadata -> assertTrue(metadata.conceptId in conceptIds); assertTrue(metadata.fallbackDiagramId in diagramIds); assertEquals(com.indianservers.aiexplorer.biology.future3d.Biology3DAssetStatus.Planned, metadata.assetStatus) }
    }

    @Test fun quizAnswersAreConfiguredAndExplainable() {
        BundledBiologyCatalogue.catalogue.quizzes.forEach { question ->
            assertTrue(question.correctAnswers.isNotEmpty()); assertTrue(question.correctAnswers.all { it in question.options.indices }); assertTrue(question.explanation.isNotBlank()); assertTrue(question.hint.isNotBlank())
        }
    }

    @Test fun completeCataloguePassesBrokenLinkAndCycleValidation() {
        val report = BiologyCatalogueValidator.validate(BundledBiologyCatalogue.catalogue)
        assertTrue(report.errors.joinToString(), report.valid)
    }

    @Test fun validationRejectsDuplicateIdsAndBrokenLinks() {
        val catalogue = BundledBiologyCatalogue.catalogue
        val invalid = catalogue.copy(
            domains = catalogue.domains + catalogue.domains.first(),
            concepts = catalogue.concepts.mapIndexed { index, concept ->
                if (index == 0) concept.copy(topicId = "missing-topic") else concept
            },
        )
        val report = BiologyCatalogueValidator.validate(invalid)
        assertFalse(report.valid)
        assertTrue(report.errors.any { it.contains("Duplicate domain ID") })
        assertTrue(report.errors.any { it.contains("broken content link") })
    }

    @Test fun navigationStateSupportsDeepRoutesAndBack() {
        val vm = BiologyFeatureViewModel(repository)
        val domain = repository.getDomains().first(); val unit = repository.getUnits(domain.id).first(); val chapter = repository.getChapters(unit.id).first(); val topic = repository.getTopics(chapter.id).first(); val concept = repository.getConceptForTopic(topic.id)!!
        vm.navigate(BiologyRoute.Domain(domain.id)); vm.navigate(BiologyRoute.Unit(unit.id)); vm.navigate(BiologyRoute.Chapter(chapter.id)); vm.navigate(BiologyRoute.Topic(topic.id)); vm.navigate(BiologyRoute.Concept(concept.id))
        assertEquals(BiologyRoute.Concept(concept.id).route, vm.state.route.route)
        assertTrue(vm.back()); assertEquals(BiologyRoute.Topic(topic.id).route, vm.state.route.route)
    }

    @Test fun learningToolsRequireExplicitActions() {
        val vm = BiologyFeatureViewModel(repository); val concept = BundledBiologyCatalogue.catalogue.concepts.first()
        assertFalse(concept.id in vm.state.completedConcepts); vm.toggleComplete(concept.id); assertTrue(concept.id in vm.state.completedConcepts)
        assertFalse(concept.id in vm.state.bookmarks); vm.toggleBookmark(concept.id); assertTrue(concept.id in vm.state.bookmarks)
    }
}
