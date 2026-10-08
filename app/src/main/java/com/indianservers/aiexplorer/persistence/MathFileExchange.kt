package com.indianservers.aiexplorer.persistence

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.FileProvider
import com.indianservers.aiexplorer.workspace.AIExplorerProjectArchive
import com.indianservers.aiexplorer.workspace.WorkspaceProjectCodec
import com.indianservers.aiexplorer.workspace.WorkspaceProjectRecovery
import com.indianservers.aiexplorer.workspace.WorkspaceState
import com.indianservers.aiexplorer.workspace.GeoGebraImport
import com.indianservers.aiexplorer.workspace.GeoGebraPackageExchange
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MathFileExchange {
    private const val authoritySuffix = ".files"

    suspend fun readProject(activity: Activity, uri: Uri): WorkspaceProjectRecovery = withContext(Dispatchers.IO) {
        val source = activity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val buffer = CharArray(8192); val result = StringBuilder()
            while (true) {
                val read = reader.read(buffer)
                if (read < 0) break
                result.append(buffer, 0, read)
                require(result.length <= AIExplorerProjectArchive.maximumChars) { "Project exceeds the 8 MB safety limit." }
            }
            result.toString()
        } ?: error("The selected file could not be opened.")
        WorkspaceProjectCodec.decode(source, recover = true)
    }

    suspend fun shareProject(activity: Activity, state: WorkspaceState) {
        val file = withContext(Dispatchers.IO) {
            shareDirectory(activity).resolve(safeName(state.name) + ".aiexplorer").apply {
                writeText(WorkspaceProjectCodec.encode(state))
            }
        }
        share(activity, file, "application/vnd.aiexplorer.project", "Share maths project")
    }

    suspend fun sharePng(activity: Activity, state: WorkspaceState) {
        val view = activity.window.decorView.rootView
        val bitmap = withContext(Dispatchers.Main) {
            require(view.width > 0 && view.height > 0) { "Workspace is not ready to capture." }
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
        }
        val file = try { withContext(Dispatchers.IO) {
            shareDirectory(activity).resolve(safeName(state.name) + ".png").apply {
                outputStream().use { stream -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) }
            }
        } } finally { bitmap.recycle() }
        share(activity, file, "image/png", "Share workspace image")
    }

    /** Copies the actual GL surface; View.draw cannot capture the AR camera surface. */
    suspend fun shareArImage(activity:Activity,view:android.view.SurfaceView) {
        require(view.width>0 && view.height>0) { "AR scene is not ready" }
        val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        try {
            withContext(kotlinx.coroutines.NonCancellable) {
                kotlin.coroutines.suspendCoroutine<Unit> { continuation ->
                    android.view.PixelCopy.request(view,bitmap,{ result ->
                        if(result==android.view.PixelCopy.SUCCESS) continuation.resumeWith(Result.success(Unit))
                        else continuation.resumeWith(Result.failure(IllegalStateException("AR image capture failed ($result)")))
                    },android.os.Handler(android.os.Looper.getMainLooper()))
                }
            }
            val file=withContext(Dispatchers.IO) {
                shareDirectory(activity).resolve("ar-space.png").apply { outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) } }
            }
            share(activity,file,"image/png","Share AR scene image")
        } finally { bitmap.recycle() }
    }
    suspend fun shareArReport(activity:Activity,report:String) {
        val file=withContext(Dispatchers.IO) { shareDirectory(activity).resolve("ar-space-report.txt").apply { writeText(report) } }
        share(activity,file,"text/plain","Share equations and measurements")
    }

    suspend fun readGeoGebra(activity: Activity, uri: Uri, base: WorkspaceState): GeoGebraImport = withContext(Dispatchers.IO) {
        val bytes = activity.contentResolver.openInputStream(uri)?.use { input ->
            val result = input.readUpTo(GeoGebraPackageExchange.maximumPackageBytes + 1)
            require(result.size <= GeoGebraPackageExchange.maximumPackageBytes) { "GeoGebra package exceeds the 8 MB safety limit." }
            result
        } ?: error("The selected GeoGebra file could not be opened.")
        GeoGebraPackageExchange.import(bytes, base)
    }

    suspend fun shareGeoGebra(activity: Activity, state: WorkspaceState) {
        val file = withContext(Dispatchers.IO) {
            shareDirectory(activity).resolve(safeName(state.name) + ".ggb").apply { writeBytes(GeoGebraPackageExchange.export(state)) }
        }
        share(activity, file, "application/vnd.geogebra.file", "Share GeoGebra construction")
    }

    private fun share(activity: Activity, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(activity, activity.packageName + authoritySuffix, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, title))
    }

    private fun InputStream.readUpTo(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 16 * 1024))
        val buffer = ByteArray(8 * 1024)
        var remaining = limit
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(buffer.size, remaining))
            if (read < 0) break
            output.write(buffer, 0, read)
            remaining -= read
        }
        return output.toByteArray()
    }

    private fun shareDirectory(activity: Activity) = File(activity.cacheDir, "shared-maths").apply { mkdirs() }
    private fun safeName(name: String) = name.lowercase().replace(Regex("[^a-z0-9._-]+"), "-").trim('-').ifBlank { "maths-workspace" }.take(64)
}
