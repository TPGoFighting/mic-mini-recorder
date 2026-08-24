package com.dji.recorder.audio

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import com.dji.recorder.model.AudioConfig
import com.dji.recorder.model.StorageLocationType
import java.io.File

object StorageHelper {

    private const val TAG = "StorageHelper"

    fun getTargetDirectory(context: Context, config: AudioConfig): File {
        val dir = when (config.storageLocation) {
            StorageLocationType.CUSTOM_DIR -> {
                if (!config.customFolderPath.isNullOrBlank()) {
                    val custom = File(config.customFolderPath)
                    if (custom.exists() || custom.mkdirs()) {
                        custom
                    } else {
                        getDefaultRecordingsDir(context)
                    }
                } else {
                    getDefaultRecordingsDir(context)
                }
            }
            StorageLocationType.PUBLIC_RECORDINGS -> {
                getDefaultRecordingsDir(context)
            }
            StorageLocationType.PUBLIC_MUSIC -> {
                val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                File(base, "DJIRecorder").takeIf { it.exists() || it.mkdirs() }
                    ?: context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    ?: File(context.filesDir, "recordings")
            }
            StorageLocationType.PUBLIC_DOWNLOAD -> {
                val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(base, "DJIRecorder").takeIf { it.exists() || it.mkdirs() }
                    ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: File(context.filesDir, "recordings")
            }
            StorageLocationType.APP_INTERNAL -> {
                File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }
            }
        }

        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getDefaultRecordingsDir(context: Context): File {
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS)
        return File(base, "DJIRecorder").takeIf { it.exists() || it.mkdirs() }
            ?: context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
            ?: File(context.filesDir, "recordings")
    }

    /**
     * 将用户通过 SAF 选择的 Tree URI 解析为物理文件路径
     */
    fun resolvePathFromTreeUri(context: Context, treeUri: Uri): String? {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri) ?: return null
            val split = docId.split(":")
            if (split.size >= 2) {
                val type = split[0]
                val relativePath = split[1]

                if ("primary".equals(type, ignoreCase = true)) {
                    val base = Environment.getExternalStorageDirectory().absolutePath
                    val target = if (relativePath.isNotEmpty()) "$base/$relativePath" else base
                    val f = File(target)
                    if (!f.exists()) f.mkdirs()
                    return f.absolutePath
                } else {
                    // SD 卡或其他外部卷
                    val storageRoot = File("/storage/$type")
                    if (storageRoot.exists()) {
                        val target = "$storageRoot/$relativePath"
                        val f = File(target)
                        if (!f.exists()) f.mkdirs()
                        return f.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving path from treeUri: $treeUri", e)
        }
        return null
    }
}
