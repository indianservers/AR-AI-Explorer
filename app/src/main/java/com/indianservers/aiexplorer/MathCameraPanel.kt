package com.indianservers.aiexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.indianservers.aiexplorer.input.MathOcrResult
import com.indianservers.aiexplorer.input.OnDeviceMathOcr
import com.indianservers.aiexplorer.input.SampledImageLoader
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun MathCameraPanel(
    onUseText: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recognizer = remember { OnDeviceMathOcr() }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var preview by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var result by remember { mutableStateOf<MathOcrResult?>(null) }
    var editableText by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Capture a close, well-lit image or choose one from the device.") }
    var processing by remember { mutableStateOf(false) }

    fun recognize(uri: Uri) {
        imageUri = uri
        result = null
        editableText = ""
        processing = true
        status = "Reading the image on this device..."
        recognizer.recognize(
            context = context,
            uri = uri,
            onSuccess = { recognized ->
                result = recognized
                editableText = recognized.normalized.editableText
                processing = false
                status = if (editableText.isBlank()) {
                    "No readable maths was found. Crop closer, improve focus, and try again."
                } else {
                    "Review the transcription before sending it to the verified solver."
                }
            },
            onFailure = { error ->
                processing = false
                status = error.message ?: "The image could not be read. Try another image."
            },
        )
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = pendingCapture
        if (captured && uri != null) recognize(uri)
        else status = "Capture cancelled."
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val directory = File(context.cacheDir, "math-camera").apply { mkdirs() }
            val file = File.createTempFile("math-question-", ".jpg", directory)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            pendingCapture = uri
            takePicture.launch(uri)
        } else {
            status = "Camera permission was not granted. You can still choose an existing image."
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) recognize(uri)
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }
    LaunchedEffect(imageUri) {
        preview = imageUri?.let { uri ->
            withContext(Dispatchers.IO) {
                SampledImageLoader.decode(context.contentResolver, uri)?.asImageBitmap()
            }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .background(Cyan.copy(alpha = .07f), RoundedCornerShape(12.dp))
            .border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .semantics { contentDescription = "Math Camera on-device recognition panel" },
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("Math Camera", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(
            "Printed questions and clear handwriting stay on this device. Recognition is editable before solving.",
            color = Muted,
            fontSize = 12.sp,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            GlowButton("Take photo", enabled = !processing) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val directory = File(context.cacheDir, "math-camera").apply { mkdirs() }
                    val file = File.createTempFile("math-question-", ".jpg", directory)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    pendingCapture = uri
                    takePicture.launch(uri)
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
            GlowButton("Choose image", enabled = !processing) {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
            GlowButton("Close", onClick = onDismiss)
        }
        preview?.let {
            Image(
                bitmap = it,
                contentDescription = "Selected maths question",
                modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(status, color = if (editableText.isNotBlank()) Green else Muted, fontSize = 12.sp)
        result?.let { recognized ->
            val confidence = recognized.confidence?.let { "${(it * 100).toInt()}%" } ?: "not reported"
            Text("${recognized.lineCount} text lines · OCR confidence $confidence", color = Amber, fontSize = 11.sp)
            if (recognized.normalized.changes.isNotEmpty()) {
                Text(recognized.normalized.changes.joinToString(" · "), color = Violet, fontSize = 11.sp)
            }
        }
        if (editableText.isNotBlank()) {
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Editable transcription") },
                minLines = 3,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                GlowButton("Use in solver", enabled = editableText.isNotBlank()) { onUseText(editableText.trim()) }
                GlowButton("Scan again", enabled = !processing) {
                    result = null
                    editableText = ""
                    imageUri = null
                    status = "Capture another image or choose one from the device."
                }
            }
        }
    }
}
