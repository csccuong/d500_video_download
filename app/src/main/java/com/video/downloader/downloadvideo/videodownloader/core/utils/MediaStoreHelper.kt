package com.video.downloader.downloadvideo.videodownloader.core.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun createPendingVideoUri(fileName: String, mimeType: String = "video/mp4"): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/D54Downloader")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val primaryCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                resolver.insert(primaryCollection, contentValues)?.let { return it }
            } catch (_: Exception) {
            }

            try {
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { return it }
            } catch (_: Exception) {
            }

            return createAppSpecificVideoUri(fileName)
        } else {
            val hasWritePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasWritePermission) {
                try {
                    resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { return it }
                } catch (_: Exception) {
                }
            }

            return createAppSpecificVideoUri(fileName)
        }
    }

    private fun createAppSpecificVideoUri(fileName: String): Uri? {
        return try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: File(context.filesDir, "Movies")
            val targetDir = File(baseDir, "D54Downloader")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            var targetFile = File(targetDir, fileName)
            if (targetFile.exists()) {
                val dotIndex = fileName.lastIndexOf('.')
                val uniqueName = if (dotIndex > 0) {
                    val namePart = fileName.substring(0, dotIndex)
                    val extPart = fileName.substring(dotIndex)
                    "${namePart}_${System.currentTimeMillis()}$extPart"
                } else {
                    "${fileName}_${System.currentTimeMillis()}"
                }
                targetFile = File(targetDir, uniqueName)
            }

            Uri.fromFile(targetFile)
        } catch (_: Exception) {
            null
        }
    }

    fun markVideoReady(uri: Uri) {
        if (uri.scheme == "content" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, contentValues, null, null)
            } catch (_: Exception) {
            }
        } else if (uri.scheme == "file") {
            val path = uri.path
            if (!path.isNullOrEmpty()) {
                try {
                    MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf("video/mp4"), null)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun openOutputStream(uri: Uri): OutputStream? {
        return try {
            if (uri.scheme == "file") {
                val path = uri.path ?: return null
                val file = File(path)
                file.parentFile?.let { parent ->
                    if (!parent.exists()) parent.mkdirs()
                }
                FileOutputStream(file)
            } else {
                context.contentResolver.openOutputStream(uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun extractAndSaveThumbnail(uri: Uri, fileName: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            if (uri.scheme == "file" && !uri.path.isNullOrEmpty()) {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

            if (bitmap != null) {
                val thumbFile = File(context.cacheDir, "thumb_${fileName}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbFile.absolutePath
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        fun resolveShareableUri(context: Context, contentUriString: String): Uri {
            return try {
                when {
                    contentUriString.startsWith("content://") -> Uri.parse(contentUriString)
                    contentUriString.startsWith("file://") -> {
                        val path = Uri.parse(contentUriString).path
                        val file = if (!path.isNullOrEmpty()) File(path) else null
                        if (file != null && file.exists()) {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            Uri.parse(contentUriString)
                        }
                    }
                    else -> {
                        val file = File(contentUriString)
                        if (file.exists()) {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            Uri.parse(contentUriString)
                        }
                    }
                }
            } catch (_: Exception) {
                Uri.parse(contentUriString)
            }
        }
    }
}
