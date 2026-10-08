from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'app/src/test/java/com/indianservers/aiexplorer/IntelligentHandMathEditTest.kt';s=p.read_text(encoding='utf-8');i=s.rfind('\n}')
s=s[:i]+'''    @Test fun parametricGraphInspectionCalculatesSlopeFromItsExpression() {
        val vm=ExplorerViewModel(SavedStateHandle());vm.upsertArCadNode(ArCadNode("curve",ArCadType.Curve,mapOf("expressionX" to "t","expressionY" to "t^2","expressionZ" to "0","tMin" to "0","tMax" to "2")))
        val g=SpatialGeometry(listOf(MathVec3(0.0,0.0,0.0),MathVec3(1.0,1.0,0.0),MathVec3(2.0,4.0,0.0)),lines=listOf(0 to 1,1 to 2))
        val p=SpatialPrimitive("curve",SpatialPrimitiveKind.Curve,g,SpatialMaterial("test",listOf(1f,1f,1f,1f)),metadata=mapOf("cadType" to "Curve"))
        val objects=HandMathSceneAdapter.objects(SpatialRenderScene("scene",listOf(p)),com.indianservers.aiexplorer.arengine.interaction.ArSelectionState(),vm.state)
        val values=objects.single().inspect!!(Vec3(1.0,1.0,0.0));assertEquals(1.0,values.getValue("t"),1e-9);assertEquals(2.0,values.getValue("slope"),1e-6)
    }
'''+s[i:];p.write_text(s,encoding='utf-8')
p=r/'app/src/main/java/com/indianservers/aiexplorer/HandMathSceneAdapter.kt';s=p.read_text(encoding='utf-8').replace('name.equals("Vector",true)->MathSemanticType.VECTOR;', 'name.equals("Vector",true)->MathSemanticType.VECTOR; name.equals("Curve",true)->MathSemanticType.PARAMETRIC_CURVE; name.equals("Point",true)->MathSemanticType.POINT; name.equals("Line",true)->MathSemanticType.LINE; name.equals("Ray",true)->MathSemanticType.RAY;');p.write_text(s,encoding='utf-8')
