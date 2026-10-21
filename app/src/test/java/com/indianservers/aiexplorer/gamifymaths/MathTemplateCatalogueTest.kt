package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class MathTemplateCatalogueTest {
    @Test fun catalogueContainsHundredsOfTemplatesAndCoversAll29Games(){
        val catalogue=workshopTemplateCatalogue+speedTemplateCatalogue
        assertEquals(288,workshopTemplateCatalogue.size)
        assertEquals(108,speedTemplateCatalogue.size)
        assertEquals(396,catalogue.size)
        assertEquals(catalogue.size,catalogue.map{it.id}.distinct().size)
        assertEquals(gamifyGamesForAudit().map{it.id}.toSet(),catalogue.map{it.gameId}.toSet())
    }

    @Test fun everyWorkshopTemplateProducesVariedQuestionsWithAnAcceptedWorkedSolution(){
        val report=mutableListOf(listOf("Template ID","Game","Question","Correct answer","Worked solution"))
        for(template in workshopTemplateCatalogue){
            val questions=mutableSetOf<String>()
            repeat(24){sample->
                val level=when{
                    template.kind==WorkshopKind.Sort->1
                    template.gameId=="measure" && template.kind==WorkshopKind.Pieces->3
                    template.gameId=="math-market" && template.kind==WorkshopKind.Pieces->2
                    else->0
                }
                val seed=sample*1777L+53L
                val base=generateBaseWorkshopChallenge(template.gameId,level,sample%4,seed)
                assertEquals(template.id,template.kind,base.kind)
                val round=instantiateWorkshopTemplate(template.gameId,base,template.variant,sample%4,seed)
                assertEquals(template.id,round.templateId)
                val solution=workshopSolution(round)
                if(sample==0)report+=listOf(template.id,template.gameId,round.prompt,solution.answer,solution.method)
                assertTrue("${template.id}/$sample: ${round.prompt}\n${solution.method}",round.accepts(solution.selection,solution.assembly,solution.marks))
                assertFalse("${template.id} must reject an empty construction",round.accepts(emptyList()))
                assertTrue(solution.answer.isNotBlank());assertTrue(solution.method.isNotBlank())
                questions+=round.signature
                round.source?.let{source->
                    assertTrue(source.answer.isFinite())
                    if(source.choices.isNotEmpty()){
                        assertEquals(source.choices.size,source.choices.distinct().size)
                        assertTrue(source.choices.any(source::accepts))
                    }
                }
            }
            assertTrue("${template.id} needs changing values",questions.size>8)
        }
        exportReport("workshop.csv",report)
    }

    @Test fun everySprintTemplateHasCorrectAnswersAndChoices(){
        val report=mutableListOf(listOf("Template ID","Game","Question","Correct answer","Worked solution"))
        for(template in speedTemplateCatalogue)repeat(32){sample->
            val mode=if(template.gameId=="speed-basic")SpeedCalculationMode.Basic else SpeedCalculationMode.Advanced
            val name=template.id.removePrefix("${template.gameId}.").substringBeforeLast('.')
            val mastery=if(name.startsWith("mastery"))name.removePrefix("mastery").toInt()else 0
            val problem=generateTemplateSpeedProblem(mode,sample%3+1,setOf(if(mastery==0)name else "Addition"),setOf(if(mastery==0)name else "Advanced Calculation"),Random(sample),mastery,template.variant)
            assertEquals(template.id,problem.templateId)
            val computed=checkNotNull(evaluateMathAssembly(problem.solutionTokens))
            assertEquals("${template.id}: ${problem.prompt}",computed.value,problem.answer.toDouble(),.00000051)
            val choices=speedAnswerChoices(problem,Random(sample))
            assertEquals(4,choices.distinct().size);assertTrue(problem.answer in choices)
            assertTrue(problem.solution.contains(problem.answer))
            if(sample==0)report+=listOf(template.id,template.gameId,problem.prompt,problem.answer,problem.solution)
        }
        exportReport("sprint.csv",report)
    }

    @Test fun chanceDrawsAndDependentDrawsMatchIndependentProbabilityCalculations(){
        repeat(100){seed->
            val base=generateBaseWorkshopChallenge("chance",0,0,seed.toLong())
            val single=instantiateWorkshopTemplate("chance",base,0,seed%4,seed.toLong())
            val counts=Regex("\\d+").findAll(single.prompt).take(3).map{it.value.toInt()}.toList()
            assertEquals(counts[0].toDouble()/counts.sum(),single.source!!.answer,1e-12)
            val dependent=instantiateWorkshopTemplate("chance",base,4,seed%4,seed.toLong())
            val drawCounts=Regex("\\d+").findAll(dependent.prompt).take(2).map{it.value.toInt()}.toList()
            val(a,b)=drawCounts
            assertEquals(a.toDouble()/(a+b)*(a-1)/(a+b-1),dependent.source!!.answer,1e-12)
        }
    }

    @Test fun robotRuleChecksRejectInvalidTeamsEvenWhenTheCollectionSizeIsRight(){
        val base=generateBaseWorkshopChallenge("combinatorics",0,0,91L)
        val round=instantiateWorkshopTemplate("combinatorics",base,4,0,91L)
        val required=round.rules.getValue("requiredRobot")
        assertTrue(round.witness.all{required in it.split(',').map(String::toInt)})
        val invalid=(0 until round.parameters[0]).filter{it!=required}.take(round.parameters[1])
        assertFalse(round.validConfiguration(invalid))
    }

    @Test fun diagramsUseSemanticValuesAndNeverRevealAnUnknownPercentage(){
        repeat(32){seed->
            val base=generateBaseWorkshopChallenge("percent-studio",0,0,seed.toLong())
            for(v in 0..11){
                val round=instantiateWorkshopTemplate("percent-studio",base,v,seed%4,seed.toLong())
                val visual=round.source!!.visuals
                if(v in listOf(5,7,8))assertTrue(visual.isEmpty())
                else{assertTrue(visual.getValue("percent") in 0.0..100.0);assertTrue(visual.containsKey("whole") || visual.containsKey("knownPart"))}
            }
            val perimeter=instantiateWorkshopTemplate("measure",generateBaseWorkshopChallenge("measure",0,0,seed.toLong()),0,seed%4,seed.toLong()).source!!
            assertEquals(perimeter.answer,2*(perimeter.visuals.getValue("length")+perimeter.visuals.getValue("width")),0.0)
        }
    }

    private fun exportReport(name:String,rows:List<List<String>>){
        java.io.File("build/reports/maths-question-templates/$name").apply{parentFile.mkdirs();writeText(rows.joinToString("\n"){row->row.joinToString(","){"\""+it.replace("\"","\"\"")+"\""}})}
    }
}
