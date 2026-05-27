package com.example.volunteerhelp.data

import android.content.Context
import android.net.Uri
import com.example.volunteerhelp.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CloudinaryRepository(
    private val context: Context
) {
    suspend fun uploadImage(uri: Uri?): ResultState<String?> = withContext(Dispatchers.IO) {
        if (uri == null) {
            return@withContext ResultState.Success(null)
        }
        if (Constants.CLOUDINARY_CLOUD_NAME.isBlank() || Constants.CLOUDINARY_UPLOAD_PRESET.isBlank()) {
            return@withContext ResultState.Error("Cloudinary не налаштовано")
        }
        runCatching {
            val boundary = "Boundary-${System.currentTimeMillis()}"
            val resolver = context.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { input -> BufferedInputStream(input).readBytes() }
                ?: throw IllegalStateException("Не вдалося прочитати файл")
            val mimeType = resolver.getType(uri) ?: "image/jpeg"
            val fileName = "image_${UUID.randomUUID()}.jpg"
            val url = URL("https://api.cloudinary.com/v1_1/${Constants.CLOUDINARY_CLOUD_NAME}/image/upload")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                useCaches = false
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }
            DataOutputStream(connection.outputStream).use { output ->
                writeFormField(output, boundary, "upload_preset", Constants.CLOUDINARY_UPLOAD_PRESET)
                writeFileField(output, boundary, "file", fileName, mimeType, bytes)
                output.writeBytes("--$boundary--\r\n")
                output.flush()
            }
            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }
            if (responseCode !in 200..299) {
                throw IllegalStateException(responseText.ifBlank { "Не вдалося завантажити зображення" })
            }
            JSONObject(responseText).optString("secure_url").ifBlank {
                throw IllegalStateException("Cloudinary не повернув адресу зображення")
            }
        }.fold(
            onSuccess = { ResultState.Success(it) },
            onFailure = { ResultState.Error(it.message ?: "Помилка завантаження зображення") }
        )
    }

    private fun writeFormField(output: DataOutputStream, boundary: String, name: String, value: String) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.writeBytes(value)
        output.writeBytes("\r\n")
    }

    private fun writeFileField(
        output: DataOutputStream,
        boundary: String,
        fieldName: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"\r\n")
        output.writeBytes("Content-Type: $mimeType\r\n\r\n")
        output.write(bytes)
        output.writeBytes("\r\n")
    }
}
