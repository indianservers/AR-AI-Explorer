package com.indianservers.aiexplorer.mathworkspace.mathematicalart

import androidx.compose.ui.graphics.Color

enum class ArtMode(val label:String){Spirograph("Spirograph"),Rose("Rose Curves"),Lissajous("Lissajous"),Polar("Polar Curves"),Parametric("Parametric Curves")}
enum class SpiroType { Hypotrochoid, Epitrochoid }
data class ArtPoint(val x:Double,val y:Double)
data class ArtLayer(val name:String,val points:List<ArtPoint>,val color:Color,val visible:Boolean=true)
data class ArtViewport(val centerX:Double=0.0,val centerY:Double=0.0,val scale:Double=1.0)
enum class ArtPalette(val label:String,val colors:List<Color>){
    Azure("Azure",listOf(Color(0xff1687ff),Color(0xff31c5ff))), Aurora("Aurora",listOf(Color(0xff11a889),Color(0xff715bea),Color(0xff37b9dd))), Sunset("Sunset",listOf(Color(0xfff15b45),Color(0xffffad42),Color(0xffc242b9))), Monochrome("Monochrome",listOf(Color(0xff25364b))), Spectrum("Spectrum",listOf(Color(0xfffa4d79),Color(0xffffb642),Color(0xff42c8a0),Color(0xff478aff))), Neon("Neon",listOf(Color(0xff00a5ff),Color(0xffb62bff),Color(0xfffa3483))), Pastel("Pastel",listOf(Color(0xff7cb8f5),Color(0xffa987e9),Color(0xffff9fa8)));
}
