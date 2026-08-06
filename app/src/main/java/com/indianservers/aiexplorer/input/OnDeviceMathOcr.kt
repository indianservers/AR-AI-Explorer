package com.indianservers.aiexplorer.input

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable

data class MathOcrResult(
    val normalized: NormalizedMathOcr,
    val confidence: Double?,
    val lineCount: Int,
)

class OnDeviceMathOcr : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognize(
        context: Context,
        uri: Uri,
        onSuccess: (MathOcrResult) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        runCatching { InputImage.fromFilePath(context, uri) }
            .onFailure(onFailure)
            .onSuccess { image ->
                recognizer.process(image)
                    .addOnSuccessListener { text ->
                        val lines = text.textBlocks.flatMap { it.lines }
                        val confidenceValues = lines.flatMap { it.elements }
                            .mapNotNull { element -> element.confidence.takeIf { it >= 0f }?.toDouble() }
                        onSuccess(
                            MathOcrResult(
                                normalized = MathOcrNormalizer.normalize(text.text),
                                confidence = confidenceValues.takeIf { it.isNotEmpty() }?.average(),
                                lineCount = lines.size,
                            ),
                        )
                    }
                    .addOnFailureListener(onFailure)
            }
    }

    override fun close() {
        recognizer.close()
    }
}
