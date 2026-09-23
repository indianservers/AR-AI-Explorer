package com.indianservers.aiexplorer.mathworkspace.lineartransform

data class MatrixPreset(val name:String,val matrix:Matrix2)
object MatrixPresets {
    val all=listOf(MatrixPreset("Identity",Matrix2(1.0,0.0,0.0,1.0)),MatrixPreset("Scale X",Matrix2(2.0,0.0,0.0,1.0)),MatrixPreset("Scale Y",Matrix2(1.0,0.0,0.0,2.0)),MatrixPreset("Uniform scale",Matrix2(2.0,0.0,0.0,2.0)),MatrixPreset("Shear X",Matrix2(1.0,1.0,0.0,1.0)),MatrixPreset("Shear Y",Matrix2(1.0,0.0,1.0,1.0)),MatrixPreset("Reflect X",Matrix2(1.0,0.0,0.0,-1.0)),MatrixPreset("Reflect Y",Matrix2(-1.0,0.0,0.0,1.0)),MatrixPreset("Swap axes",Matrix2(0.0,1.0,1.0,0.0)),MatrixPreset("Rotate 90°",Matrix2(0.0,-1.0,1.0,0.0)),MatrixPreset("Rotate 45°",MatrixMathEngine.rotation(45.0)),MatrixPreset("Project to x",Matrix2(1.0,0.0,0.0,0.0)))
}
