from pathlib import Path
r=Path(r'C:\Indian Servers\AIExplorer');p=r/'arengine/src/main/java/com/indianservers/aiexplorer/handintelligence/spatial/SpatialProcessingLayer.kt';s=p.read_text(encoding='utf-8')
s=s.replace('    fun reset() { ids=', '''    private fun turn(from:Vec3,to:Vec3):com.indianservers.aiexplorer.arengine.contract.ArQuaternion {
        val a=from.unit();val b=to.unit();val dot=a.dot(b).coerceIn(-1.0,1.0)
        if(a.magnitude()<.5 || b.magnitude()<.5) return com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity
        val axis=if(dot<-.9999) a.cross(if(abs(a.x)<.9) Vec3(1.0,0.0,0.0) else Vec3(0.0,1.0,0.0)).unit() else a.cross(b)
        return com.indianservers.aiexplorer.arengine.contract.ArQuaternion(axis.x,axis.y,axis.z,if(dot<-.9999) 0.0 else 1+dot).normalized()
    }
    private fun euler(q:com.indianservers.aiexplorer.arengine.contract.ArQuaternion)=Vec3(
        Math.toDegrees(atan2(2*(q.w*q.x+q.y*q.z),1-2*(q.x*q.x+q.y*q.y))),
        Math.toDegrees(asin((2*(q.w*q.y-q.z*q.x)).coerceIn(-1.0,1.0))),
        Math.toDegrees(atan2(2*(q.w*q.z+q.x*q.y),1-2*(q.y*q.y+q.z*q.z))))
    fun reset() { ids=''',1)
s=s.replace('        var rotation=angleDelta(hands[0].pose.angle,baseAngles[0]); var scale=1.0','        var rotation=angleDelta(hands[0].pose.angle,baseAngles[0]); var scale=1.0\n        var interHand=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity')
s=s.replace('            // Full inter-hand vector rotation, expressed in scene-local Euler axes.','            interHand=turn(before,after)\n            // Full inter-hand vector rotation, expressed in scene-local Euler axes.')
a=s.index('        val n0=baseNormals');b=s.index('        val scaled=',a)
s=s[:a]+'''        val n0=baseNormals.firstOrNull() ?: Vec3.Zero; val n1=hands[0].pose.palmNormal
        val normalAngle=Math.toDegrees(acos(n0.dot(n1).coerceIn(-1.0,1.0)))
        val tilt=if(normalAngle>5 && normalAngle<120) turn(n0,n1) else com.indianservers.aiexplorer.arengine.contract.ArQuaternion.Identity
        val spin=if(hands.size==2) interHand else com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(0.0,0.0,if(rotationActive) rotation else 0.0)
        val combined=com.indianservers.aiexplorer.arengine.contract.ArQuaternion.fromEulerDegrees(baseTransform.rotation.x,baseTransform.rotation.y,baseTransform.rotation.z)*spin*tilt
        val turned=euler(combined)
'''+s[b:];p.write_text(s,encoding='utf-8')
p=r/'app/src/androidTest/java/com/indianservers/aiexplorer/ArHandInferenceDeviceTest.kt';s=p.read_text(encoding='utf-8').replace('            android.util.Log.i("HandInferenceMetrics",','''            // Same pooled bitmap must read NEW pixels rather than a cached MediaPipe image.
            val blankY=ByteArray(y.size) { 16 };val blankU=ByteArray(u.size) { 128.toByte() };val blankV=ByteArray(v.size) { 128.toByte() }
            while(!detector.canAcceptFrame) Thread.sleep(5)
            detector.submit(ArCameraImage(SystemClock.uptimeMillis(),width,height,listOf(ArCameraImagePlane(blankY,width,1),ArCameraImagePlane(blankU,cw,1),ArCameraImagePlane(blankV,cw,1)),listOf(ArVector2(0f,0f),ArVector2(1f,0f),ArVector2(0f,1f))))
            val blank=frames.poll(15,TimeUnit.SECONDS);assertNotNull(blank);assertEquals("Pooled image must reflect replacement pixels",0,blank!!.hands.size)
            android.util.Log.i("HandInferenceMetrics",''');p.write_text(s,encoding='utf-8')
