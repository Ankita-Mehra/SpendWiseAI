package com.example.receiptai.ai

import android.content.Context
import android.net.Uri
import com.example.receiptai.data.Categories
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ScannedReceipt(
    val store: String,
    val date: String,
    val total: Double,
    val category: String
)

class ReceiptScanException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The AI pipeline:
 *   1. ML Kit reads the text from the photo ON-DEVICE (free, offline).
 *   2. Gemini turns that messy OCR text into clean structured JSON (cloud).
 */
class ReceiptScanner(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            responseMimeType = "application/json"   // force JSON output
            responseSchema = RECEIPT_SCHEMA         // ...matching this exact shape
            temperature = 0f                        // extraction task: be deterministic
        }
    )

    suspend fun scan(imageUri: Uri): ScannedReceipt {
        val ocrText = readText(imageUri)
        if (ocrText.isBlank()) {
            throw ReceiptScanException("No text found. Try a sharper, well-lit photo of the receipt.")
        }
        return structure(ocrText)
    }

    // Step 1: on-device OCR
    private suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        // fromFilePath also handles EXIF rotation for camera photos
        val image = InputImage.fromFilePath(context, uri)
        recognizer.process(image).await().text
    }

    // Step 2: LLM turns raw text into structured data
    private suspend fun structure(ocrText: String): ScannedReceipt {
        val today = LocalDate.now().toString()

        val instructions = """
            You extract data from receipt text produced by OCR. It may contain typos and noise.
            Today's date is $today.

            Return:
            - store: the merchant name
            - date: the purchase date as yyyy-MM-dd (use $today if no date is visible)
            - total: the final amount paid, including tax, as a plain number with no currency symbol
            - category: the best-fitting category
        """.trimIndent()

        val prompt = "$instructions\n\nReceipt text:\n---\n$ocrText\n---"

        val raw = model.generateContent(prompt).text
            ?: throw ReceiptScanException("The AI returned an empty response. Please try again.")

        val json = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw ReceiptScanException("The AI response couldn't be understood. Please try again.", e)
        }

        // Never trust model output blindly: validate before it reaches your UI/database.
        val date = json.optString("date").takeIf { runCatching { LocalDate.parse(it) }.isSuccess } ?: today
        val category = json.optString("category").takeIf { it in Categories.all } ?: "Other"
        val total = json.optDouble("total", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0
        val store = json.optString("store").trim().ifBlank { "Unknown store" }

        return ScannedReceipt(store, date, total, category)
    }

    companion object {
        // If you get a "model not found" error, check Firebase AI Logic docs for the current model name.
        private const val MODEL_NAME = "gemini-3.8-flash"

        private val RECEIPT_SCHEMA = Schema.obj(
            mapOf(
                "store" to Schema.string(),
                "date" to Schema.string(),
                "total" to Schema.double(),
                "category" to Schema.enumeration(Categories.all)
            )
        )
    }
}

/** Lets us `await()` a Google Play services Task from a coroutine without extra dependencies. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
