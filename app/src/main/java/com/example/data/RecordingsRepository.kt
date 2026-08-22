package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.model.RecordingFileItem
import java.io.File

class RecordingsRepository(private val context: Context) {

    companion object {
        private const val TAG = "RecordingsRepository"
    }

    fun getRecordingsDir(): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MicMini")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun fetchRecordings(): List<RecordingFileItem> {
        val dir = getRecordingsDir()
        val files = dir.listFiles { file ->
            file.isFile && (file.name.endsWith(".m4a", ignoreCase = true) ||
                    file.name.endsWith(".mp3", ignoreCase = true) ||
                    file.name.endsWith(".wav", ignoreCase = true) ||
                    file.name.endsWith(".aac", ignoreCase = true))
        } ?: emptyArray()

        val retriever = MediaMetadataRetriever()
        val list = mutableListOf<RecordingFileItem>()

        for (file in files.sortedByDescending { it.lastModified() }) {
            var durationMs = 0L
            try {
                retriever.setDataSource(file.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                // If duration retrieval fails, estimate from file size for rough fallback
                Log.w(TAG, "Could not extract metadata for ${file.name}", e)
            }

            list.add(
                RecordingFileItem(
                    path = file.absolutePath,
                    name = file.name,
                    sizeBytes = file.length(),
                    durationMs = durationMs,
                    lastModifiedMs = file.lastModified()
                )
            )
        }

        try {
            retriever.release()
        } catch (_: Exception) {}

        return list
    }

    fun deleteRecording(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    fun createShareIntent(filePath: String): Intent? {
        val file = File(filePath)
        if (!file.exists()) return null

        val uri: Uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "FileProvider error", e)
            return null
        }

        val mimeType = when {
            file.name.endsWith(".m4a", true) -> "audio/mp4"
            file.name.endsWith(".mp3", true) -> "audio/mpeg"
            file.name.endsWith(".wav", true) -> "audio/wav"
            else -> "audio/*"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
