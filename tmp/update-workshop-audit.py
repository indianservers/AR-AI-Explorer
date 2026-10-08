from pathlib import Path
p=Path('app/src/main/java/com/indianservers/aiexplorer/gamifymaths/GamifyMathsScreen.kt')
s=p.read_text();a=s.index('internal fun gamifyMissionAudit()');b=s.index('\n@Composable',a)
s=s[:a]+'''internal data class GameWorkshopAudit(val gameId:String,val level:Int,val kind:WorkshopKind,val prompt:String,val mastery:Int)
internal fun gamifyWorkshopAudit():List<GameWorkshopAudit> = Games.filterNot{it.id.startsWith("speed-")}.flatMap{game->
    (0 until GeneratedLevelCount).map{level->
        val round=generateWorkshopChallenge(game.id,level,0,level.toLong())
        GameWorkshopAudit(game.id,level+1,round.kind,round.prompt,round.mastery)
    }
}
'''+s[b:];a=s.index('internal data class GameMissionAudit(');b=s.index('internal data class GameCatalogueAudit',a);s=s[:a]+s[b:];p.write_text(s)
p=Path('app/src/test/java/com/indianservers/aiexplorer/gamifymaths/GameLearningSupportTest.kt')
s=p.read_text();a=s.index('    @Test\n    fun everyGeneralMission');b=s.index('    @Test\n    fun onlyCalculation',a)
s=s[:a]+'''    @Test fun catalogueAuditCoversAllLiveBoardsAndBothMasteries() {
        val boards=gamifyWorkshopAudit()
        assertEquals(27*122,boards.size)
        assertEquals(boards.size,boards.map{"${it.gameId}:${it.level}"}.distinct().size)
        boards.groupBy{it.gameId}.forEach{(_,levels)->
            assertEquals(122,levels.size)
            assertEquals(listOf(1,2),levels.takeLast(2).map{it.mastery})
            assertTrue(levels.all{it.prompt.isNotBlank()})
        }
        assertEquals(WorkshopKind.entries.toSet(),boards.map{it.kind}.toSet())
    }

'''+s[b:];p.write_text(s)
