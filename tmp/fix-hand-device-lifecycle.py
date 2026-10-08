from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'app/src/main/java/com/indianservers/aiexplorer/HandIntelligenceFeedback.kt';s=p.read_text(encoding='utf-8').replace('withFrameNanos {','androidx.compose.animation.core.withInfiniteAnimationFrameNanos {');p.write_text(s,encoding='utf-8')
p=r/'arengine/src/main/java/com/indianservers/aiexplorer/arengine/interaction/ArHandLandmarker.kt';s=p.read_text(encoding='utf-8')
s=s.replace('    private var reusablePixels = IntArray(0)','    private var reusablePixels = IntArray(0)\n    private var rawImage: com.google.mediapipe.framework.image.MPImage? = null\n    private var uprightImage: com.google.mediapipe.framework.image.MPImage? = null')
s=s.replace('                        reusableUpright?.recycle(); reusableUpright =','                        uprightImage?.close(); uprightImage=null\n                        reusableUpright?.takeUnless { it.isRecycled }?.recycle(); reusableUpright =')
s=s.replace('                val mpImage = BitmapImageBuilder(upright).build()\n                val result = try { detector?.detectForVideo(mpImage, image.timestampMillis) } finally { mpImage.close() }','''                // BitmapImageBuilder owns its bitmap. Reuse the wrapper too and close it only
                // when its buffer is resized or the detector is disposed.
                val mpImage = if(rotation==0) rawImage ?: BitmapImageBuilder(upright).build().also { rawImage=it }
                    else uprightImage ?: BitmapImageBuilder(upright).build().also { uprightImage=it }
                val result = detector?.detectForVideo(mpImage, image.timestampMillis)''')
s=s.replace('            reusableRaw?.recycle(); reusableRaw =','            rawImage?.close(); rawImage=null\n            reusableRaw?.takeUnless { it.isRecycled }?.recycle(); reusableRaw =')
s=s.replace('detector = null; reusableRaw?.recycle(); reusableUpright?.recycle();','detector = null; rawImage?.close(); uprightImage?.close(); rawImage=null; uprightImage=null; reusableRaw?.takeUnless { it.isRecycled }?.recycle(); reusableUpright?.takeUnless { it.isRecycled }?.recycle();')
p.write_text(s,encoding='utf-8')
