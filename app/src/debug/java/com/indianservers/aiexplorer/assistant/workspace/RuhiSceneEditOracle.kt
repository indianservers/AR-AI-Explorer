package com.indianservers.aiexplorer.assistant.workspace

import com.indianservers.aiexplorer.core.*
import com.indianservers.aiexplorer.workspace.*
import kotlin.math.*

object RuhiSceneEditOracle {
    fun check(p:Map<String,Any?>,before:WorkspaceState,r:RuhiExecutionResult,c:RuhiSceneContext):RuhiSeedBehaviorOracle.Check {
        val errors=mutableListOf<String>();fun need(v:Boolean,m:String){if(!v)errors+=m}
        fun same(a:Any?,b:Any?):Boolean=when{a is Number&&b is Number->abs(a.toDouble()-b.toDouble())<1e-8;else->a==b}
        need(r.status==GraphCommandStatus.APPLIED,"Status ${r.status}: ${r.message}");need(r.semanticFrame?.intent=="EDIT_SCENE","Missing scene intent");need(r.state.module==before.module,"Changed active engine");p.forEach{(k,v)->need(same(v,r.semanticFrame?.parameters?.get(k)),"Original slot $k differs: $v vs ${r.semanticFrame?.parameters?.get(k)}")}
        val after=r.state;val target=p["target"].toString();val action=p["action"]
        runCatching {
            when {
                p["color"]!=null->{val color=mapOf("blue" to "#0000FF","red" to "#FF0000","green" to "#008000").getValue(p["color"].toString());if(target=="last_graph")need(after.functions==before.functions.dropLast(1)+before.functions.last().copy(colorKey=color),"Wrong graph color or target")else{val id=c.selectedShapeId ?: before.shapes.single{if(target=="selected_triangle")it.type==Shape2DType.Triangle else it.type==Shape2DType.Segment}.id;need(after.shapes==before.shapes.map{if(it.id==id)it.copy(styleKey=color)else it},"Wrong geometry color or target")}}
                p["visible"]!=null->if(target=="grid")need(!RuhiSceneEditLanguage.grid(after),"Grid still visible")else need(RuhiSceneEditLanguage.flag(after,"axes"),"Axes still hidden")
                p["label"]!=null->{val q=before.shapes.single{RuhiCircleLanguage.isCircle(it)};need(after.shapes==before.shapes.map{if(it.id==q.id)it.copy(name=p["label"].toString())else it},"Circle renamed or recreated incorrectly");need(after.labSessionValues[RuhiSceneEditLanguage.PREFIX+"shapeLabel.${q.id}"]=="true","No visible circle label")}
                p["line_width"]!=null->{val q=before.functions.single{it.colorKey=="#FF0000"};need(after.functions==before.functions.map{if(it.id==q.id)it.copy(appearance=it.appearance.copy(width=it.appearance.width*1.5f))else it},"Wrong line width")}
                p["line_style"]!=null->{val id=c.selectedFunctionIds.single();need(after.functions==before.functions.map{if(it.id==id)it.copy(appearance=it.appearance.copy(line=GraphLineStyle.Dashed))else it},"Wrong curve dash style")}
                p["visible_label"]==true->{val i=before.labSessionValues.getValue("ruhi.studio.point.$target").toInt();need(RuhiSceneEditLanguage.pointLabelVisible(after,i),"Point label not visible")}
                p["measurements"]==false->need(!RuhiSceneEditLanguage.measurements(after,c.selectedShapeId!!),"Triangle measurements still visible")
                p["highlight_endpoints"]==true->need(after.labSessionValues[RuhiSceneEditLanguage.PREFIX+"endpoints.${before.shapes.single{it.name=="AB"}.id}"]=="true","No endpoint highlight")
                action=="clear_scene"->need(after.points.isEmpty()&&after.shapes.isEmpty()&&after.pointDependencies.isEmpty()&&after.geometryConstraints.isEmpty()&&after.geometryGroups.isEmpty(),"Native canvas not cleared")
                action=="delete"->need(after.shapes==before.shapes.dropLast(1),"Wrong last object deleted")
                action=="duplicate"->{need(after.shapes.size==before.shapes.size+1&&after.shapes.take(before.shapes.size)==before.shapes,"Original shapes changed during duplicate");val q=before.shapes.single{it.id==c.selectedShapeId};val clone=after.shapes.last();need(clone.id !in before.shapes.map{it.id}&&clone.type==q.type&&clone.styleKey==q.styleKey,"Duplicate does not have independent identity/style");need(clone.pointIndices.all{it>=before.points.size},"Duplicate reused original controls");val a=RuhiCircleLanguage.describe(before,q)!!;val b=RuhiCircleLanguage.describe(after,clone)!!;need(abs(a.radius-b.radius)<1e-8&&a.center!=b.center,"Duplicate is not a distinct congruent circle")}
                p["scene_name"]!=null->need(after.name==p["scene_name"],"Scene name not changed")
                action=="save_scene"->{val saved=WorkspaceProjectCodec.decode(after.labSessionValues.getValue(RuhiSceneEditLanguage.SAVED),recover=false).state!!;need(saved.points==before.points&&saved.shapes==before.shapes&&saved.solids==before.solids&&saved.functions==before.functions&&saved.module==before.module,"Saved snapshot does not match current construction")}
                action in setOf("undo","redo")->need(after.shapes==before.shapes.mapIndexed{i,q->if(i==0)q.copy(styleKey=if(action=="undo")"#0000FF"else"#FF0000")else q},"Wrong history operation")
                action=="zoom_to_fit"->{val view=after.graph2DView;val aspect=before.labSessionValues[RuhiSceneEditLanguage.ASPECT]?.toDoubleOrNull() ?: 2.0;val minX=before.points.minOf{it.x};val maxX=before.points.maxOf{it.x};val minY=before.points.minOf{it.y};val maxY=before.points.maxOf{it.y};need(abs(view.centerX-(minX+maxX)/2)<1e-9&&abs(view.centerY-(minY+maxY)/2)<1e-9,"Wrong fit center");val half=7/view.zoom;need(before.points.all{abs(it.x-view.centerX)<half&&abs(it.y-view.centerY)<half*aspect},"Not all objects fit the viewport")}
                action=="camera_center"->{val b=before.points[before.labSessionValues.getValue("ruhi.studio.point.$target").toInt()];need(after.graph2DView==before.graph2DView.copy(centerX=b.x,centerY=b.y),"Viewport did not center on current point")}
                p["opacity"]!=null->need(after.surfaceLayers==before.surfaceLayers.map{it.copy(opacity=(p["opacity"] as Number).toDouble())},"Wrong native mesh opacity")
                action=="solo"->need(after.functions==before.functions.mapIndexed{i,q->q.copy(visible=i==1)},"Solo deleted or recreated a graph")
                p["labels"]==false->need(before.points.indices.all{!RuhiSceneEditLanguage.pointLabelVisible(after,it)},"Point labels still visible")
                p["contrast"]!=null->need(abs(RuhiSceneEditLanguage.contrast(after)-1.5)<1e-9,"Grid contrast not increased")
                action=="reset_theme"->need(after.shapes==before.shapes.map{it.copy(styleKey="default")},"Default colors not restored")
                else->errors+="Missing independent scene-edit oracle"
            }
            if(action !in setOf("clear_scene","duplicate"))need(before.points==after.points,"Edit changed point coordinates")
            need(before.solids==after.solids&&before.points3D==after.points3D&&before.vectors3D==after.vectors3D&&before.planes3D==after.planes3D,"Edit changed unrelated 3D geometry")
            need(before.functions.map{it.id to it.expression}==after.functions.map{it.id to it.expression},"Edit changed graph IDs or formulas")
            if(action !in setOf("clear_scene","delete","duplicate"))need(before.shapes.map{Triple(it.id,it.type,it.pointIndices)}==after.shapes.map{Triple(it.id,it.type,it.pointIndices)},"Style edit recreated geometric objects or changed their controls")
            need(before.surfaceLayers.map{listOf(it.id,it.kind,it.expression,it.expressionY,it.expressionZ,it.domain)}==after.surfaceLayers.map{listOf(it.id,it.kind,it.expression,it.expressionY,it.expressionZ,it.domain)},"Style edit changed surface geometry")
            val loaded=WorkspaceProjectCodec.decode(WorkspaceProjectCodec.encode(after),recover=false).state!!;need(loaded.points==after.points&&loaded.shapes==after.shapes&&loaded.functions==after.functions&&loaded.surfaceLayers==after.surfaceLayers&&loaded.name==after.name,"Edit did not persist")
        }.onFailure{errors+=it.message ?: "Scene oracle failed"}
        return RuhiSeedBehaviorOracle.Check(errors.isEmpty(),errors)
    }
}
