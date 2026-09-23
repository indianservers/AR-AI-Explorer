package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

object ArtExport {
    fun share(context:Context,layers:List<ArtLayer>,size:Int=1200,transparent:Boolean=false){
        val n=size.coerceIn(600,4800);val bitmap=Bitmap.createBitmap(n,n,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);if(!transparent)canvas.drawColor(android.graphics.Color.WHITE)
        val visible=layers.filter{it.visible&&it.points.any{p->p.x.isFinite()&&p.y.isFinite()}};val ps=visible.flatMap{it.points}.filter{it.x.isFinite()&&it.y.isFinite()};if(ps.isNotEmpty()){
            val minX=ps.minOf{it.x};val maxX=ps.maxOf{it.x};val minY=ps.minOf{it.y};val maxY=ps.maxOf{it.y};val span=max(maxX-minX,maxY-minY).coerceAtLeast(1e-9);val scale=n*.78/span;val cx=(minX+maxX)/2;val cy=(minY+maxY)/2
            visible.forEach{layer->val points=layer.points;val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=max(2f,n/480f);strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND}
                points.zipWithNext().forEachIndexed{index,(a,b)->if(a.x.isFinite()&&a.y.isFinite()&&b.x.isFinite()&&b.y.isFinite()){val colors=layer.color;paint.color=android.graphics.Color.rgb((colors.red*255).roundToInt(),(colors.green*255).roundToInt(),(colors.blue*255).roundToInt());val path=Path().apply{moveTo((n/2+(a.x-cx)*scale).toFloat(),(n/2-(a.y-cy)*scale).toFloat());lineTo((n/2+(b.x-cx)*scale).toFloat(),(n/2-(b.y-cy)*scale).toFloat())};canvas.drawPath(path,paint)}}
            }
        }
        val folder=File(context.cacheDir,"shared-art").apply{mkdirs()};val file=File(folder,"mathematical-art.png");FileOutputStream(file).use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle()
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file);val intent=Intent(Intent.ACTION_SEND).apply{type="image/png";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_SUBJECT,"Mathematical Art");addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)};context.startActivity(Intent.createChooser(intent,"Share artwork"))
    }
}
