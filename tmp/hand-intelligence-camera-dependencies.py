from pathlib import Path
p=Path('app/build.gradle.kts');s=p.read_text();s=s.replace('    implementation(project(":arengine"))','    implementation("androidx.camera:camera-camera2:1.6.1")\n    implementation("androidx.camera:camera-lifecycle:1.6.1")\n    implementation("androidx.camera:camera-view:1.6.1")\n    implementation(project(":arengine"))',1);p.write_text(s)
p=Path('arengine/src/main/java/com/indianservers/aiexplorer/arengine/interaction/ArHandLandmarker.kt');s=p.read_text();s=s.replace('import android.graphics.Matrix','import android.graphics.Matrix\nimport android.graphics.Canvas\nimport android.graphics.Paint');s=s.replace('    @Volatile private var ready', '    private var reusableRaw: Bitmap? = null\n    private var reusableUpright: Bitmap? = null\n    private var reusablePixels = IntArray(0)\n    private val rotationMatrix = Matrix()\n    private val rotationPaint = Paint(Paint.FILTER_BITMAP_FLAG)\n    @Volatile private var ready');s=s.replace('                upright = if (rotation == 0) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height,\n                    Matrix().apply { postRotate(rotation.toFloat()) }, true)', '''                upright = if (rotation == 0) raw else {
                    val w = if (rotation == 90 || rotation == 270) raw.height else raw.width
                    val h = if (rotation == 90 || rotation == 270) raw.width else raw.height
                    if (reusableUpright?.width != w || reusableUpright?.height != h) {
                        reusableUpright?.recycle(); reusableUpright = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                    }
                    rotationMatrix.reset(); rotationMatrix.postRotate(rotation.toFloat())
                    rotationMatrix.postTranslate(if (rotation == 90 || rotation == 180) w.toFloat() else 0f, if (rotation == 180 || rotation == 270) h.toFloat() else 0f)
                    reusableUpright!!.also { Canvas(it).drawBitmap(raw,rotationMatrix,rotationPaint) }
                }''');s=s.replace('                if (upright !== raw) upright?.recycle()\n                raw?.recycle()\n','');s=s.replace('        val pixels = IntArray(width * height)','        if (reusablePixels.size != width*height) reusablePixels = IntArray(width*height)\n        val pixels = reusablePixels');s=s.replace('        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)', '''        if (reusableRaw?.width != width || reusableRaw?.height != height) {
            reusableRaw?.recycle(); reusableRaw = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        }
        return reusableRaw!!.also { it.setPixels(pixels,0,width,0,0,width,height) }''');s=s.replace('executor.execute { detector?.close(); detector = null }','executor.execute { detector?.close(); detector = null; reusableRaw?.recycle(); reusableUpright?.recycle(); reusableRaw=null; reusableUpright=null }');p.write_text(s)
