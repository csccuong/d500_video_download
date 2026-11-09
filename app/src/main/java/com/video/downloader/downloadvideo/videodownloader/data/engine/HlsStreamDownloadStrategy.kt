package com.video.downloader.downloadvideo.videodownloader.data.engine

import android.net.Uri
import com.video.downloader.downloadvideo.videodownloader.core.utils.FileUtil
import com.video.downloader.downloadvideo.videodownloader.core.utils.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class HlsStreamDownloadStrategy @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val mediaStoreHelper: MediaStoreHelper
) : DownloadStrategy {

    private val isCancelled = AtomicBoolean(false)

    override suspend fun executeDownload(
        sourceUrl: String,
        destinationUri: Uri,
        listener: DownloadProgressListener,
        headers: Map<String, String>?
    ) = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        var outputStream: OutputStream? = null

        try {
            listener.onProgress(0, 100, 1, "Parsing HLS manifest...")

            val defaultUa = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            val reqBuilder = Request.Builder().url(sourceUrl)
            reqBuilder.header("User-Agent", headers?.get("User-Agent") ?: defaultUa)
            headers?.forEach { (k, v) ->
                if (!k.equals("User-Agent", ignoreCase = true)) reqBuilder.header(k, v)
            }

            val manifestRes = okHttpClient.newCall(reqBuilder.build()).execute()
            if (!manifestRes.isSuccessful) {
                listener.onError(Exception("Failed to fetch M3U8 manifest: ${manifestRes.code}"))
                return@withContext
            }

            val manifestContent = manifestRes.body?.string().orEmpty()
            manifestRes.close()

            if (manifestContent.isEmpty()) {
                listener.onError(Exception("Empty M3U8 manifest"))
                return@withContext
            }

            var mediaPlaylistUrl = sourceUrl
            var lines = manifestContent.lines()

            // Check if Master Playlist
            if (manifestContent.contains("#EXT-X-STREAM-INF")) {
                var bestSubUrl: String? = null
                for (i in lines.indices) {
                    val line = lines[i].trim()
                    if (line.startsWith("#EXT-X-STREAM-INF") && i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        if (!nextLine.startsWith("#") && nextLine.isNotEmpty()) {
                            bestSubUrl = resolveUrl(sourceUrl, nextLine)
                        }
                    }
                }
                if (bestSubUrl != null) {
                    mediaPlaylistUrl = bestSubUrl
                    val subReq = Request.Builder().url(mediaPlaylistUrl)
                    subReq.header("User-Agent", headers?.get("User-Agent") ?: defaultUa)
                    headers?.forEach { (k, v) ->
                        if (!k.equals("User-Agent", ignoreCase = true)) subReq.header(k, v)
                    }
                    val subRes = okHttpClient.newCall(subReq.build()).execute()
                    if (subRes.isSuccessful) {
                        lines = subRes.body?.string().orEmpty().lines()
                        subRes.close()
                    }
                }
            }

            // Extract segment URLs
            val segmentUrls = mutableListOf<String>()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    segmentUrls.add(resolveUrl(mediaPlaylistUrl, trimmed))
                }
            }

            if (segmentUrls.isEmpty()) {
                listener.onError(Exception("No video segments found in M3U8"))
                return@withContext
            }

            outputStream = mediaStoreHelper.openOutputStream(destinationUri) ?: run {
                listener.onError(Exception("Cannot open OutputStream for destination"))
                return@withContext
            }

            var totalBytesCopied = 0L
            val totalSegments = segmentUrls.size
            var lastSpeedCheck = System.currentTimeMillis()
            var bytesSinceCheck = 0L
            var speedText = "0 KB/s"

            for (index in segmentUrls.indices) {
                if (isCancelled.get()) {
                    listener.onError(Exception("Download cancelled by user"))
                    return@withContext
                }

                val segUrl = segmentUrls[index]
                val segReqBuilder = Request.Builder().url(segUrl)
                segReqBuilder.header("User-Agent", headers?.get("User-Agent") ?: defaultUa)
                headers?.forEach { (k, v) ->
                    if (!k.equals("User-Agent", ignoreCase = true)) segReqBuilder.header(k, v)
                }

                val segRes = okHttpClient.newCall(segReqBuilder.build()).execute()
                if (segRes.isSuccessful && segRes.body != null) {
                    val bytes = segRes.body!!.bytes()
                    outputStream.write(bytes)
                    totalBytesCopied += bytes.size
                    bytesSinceCheck += bytes.size
                }
                segRes.close()

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedCheck
                if (elapsed >= 500) {
                    val speedBytes = (bytesSinceCheck * 1000) / elapsed
                    speedText = "${FileUtil.formatFileSize(speedBytes)}/s"
                    bytesSinceCheck = 0L
                    lastSpeedCheck = now
                }

                val percent = (((index + 1) * 100) / totalSegments)
                listener.onProgress(totalBytesCopied, 0L, percent, speedText)
            }

            outputStream.flush()

            if (totalBytesCopied < 5 * 1024) {
                listener.onError(Exception("Downloaded file is too small or corrupted"))
                return@withContext
            }

            mediaStoreHelper.markVideoReady(destinationUri)
            listener.onComplete(destinationUri)

        } catch (e: Exception) {
            listener.onError(e)
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
        }
    }

    private fun resolveUrl(baseUrl: String, relativeOrAbsolute: String): String {
        return try {
            val baseUri = URI(baseUrl)
            baseUri.resolve(relativeOrAbsolute).toString()
        } catch (_: Exception) {
            relativeOrAbsolute
        }
    }

    override fun cancel() {
        isCancelled.set(true)
    }
}
