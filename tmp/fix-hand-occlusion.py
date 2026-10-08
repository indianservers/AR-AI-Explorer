from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/Models.kt';s=p.read_text(encoding='utf-8')
s=s.replace('    val size=vertices.maxOfOrNull', '''    val boundsMin=Vec3(vertices.minOfOrNull { it.x } ?: 0.0,vertices.minOfOrNull { it.y } ?: 0.0,vertices.minOfOrNull { it.z } ?: 0.0)
    val boundsMax=Vec3(vertices.maxOfOrNull { it.x } ?: 0.0,vertices.maxOfOrNull { it.y } ?: 0.0,vertices.maxOfOrNull { it.z } ?: 0.0)
    val boundsCorners=(0..7).map { i -> Vec3(if(i and 1==0) boundsMin.x else boundsMax.x,if(i and 2==0) boundsMin.y else boundsMax.y,if(i and 4==0) boundsMin.z else boundsMax.z) }
    val size=vertices.maxOfOrNull''')
s=s.replace('val trajectoryScore:Float,val finalScore:Float)','val trajectoryScore:Float,val finalScore:Float,val rayDistance:Double=Double.POSITIVE_INFINITY)');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/ContactSolver.kt';s=p.read_text(encoding='utf-8')
s=s.replace('        return scene.objects.asSequence()', '        val candidates=scene.objects.asSequence()')
s=s.replace('            val edgeProjection=scene.mapper.project(obj.center+Vec3(obj.size,0.0,0.0)) ?: projectedCenter\n            val projectedSize=max(.025,(edgeProjection-projectedCenter).magnitude())','            val projectedSize=max(.025,obj.boundsCorners.maxOfOrNull { v -> scene.mapper.project(v)?.let { (it-projectedCenter).magnitude() } ?: 0.0 } ?: 0.0)')
s=s.replace('                val screen=scene.mapper.project(v) ?: return@forEachIndexed','                if(hit!=null && (v-ray.origin).dot(ray.direction)>nearest+obj.size*.03) return@forEachIndexed\n                val screen=scene.mapper.project(v) ?: return@forEachIndexed')
s=s.replace('            val depth=clamp(1/(1+abs(projectedCenter.z)*.01))','            val rayDistance=if(hit!=null) nearest else (position-ray.origin).dot(ray.direction)\n            val depth=clamp(1/(1+abs(rayDistance)*.01))')
s=s.replace('focusScore,1f,trajectory,score)','focusScore,1f,trajectory,score,rayDistance)')
s=s.replace('        }.sortedByDescending { it.finalScore }.take(8).toList()','''        }.toList()
        val front=candidates.minOfOrNull { it.rayDistance } ?: return emptyList()
        // An overlapping rear surface cannot win simply because it was previously selected.
        return candidates.filter { it.rayDistance<=front+.02 }.sortedByDescending { it.finalScore }.take(8)''')
p.write_text(s,encoding='utf-8')
p=r/'arengine/src/test/java/com/indianservers/aiexplorer/handintelligence/HandIntelligenceTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}');s=s[:i]+'''    @Test fun occludedSelectedObjectCannotWinOverFrontSurface() {
        val front=objectAt("front").copy(vertices=objectAt().vertices.map { it+Vec3(0.0,0.0,1.0) },selected=false)
        val back=objectAt("back").copy(selected=true)
        val candidates=ContactSolver().candidates(hand(),scene(listOf(back,front)),"back",300)
        assertEquals("front",candidates.first().objectId);assertTrue(candidates.none { it.objectId=="back" })
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
