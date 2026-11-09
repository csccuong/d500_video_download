package com.video.downloader.downloadvideo.videodownloader.data.engine

import android.net.Uri
import com.video.downloader.downloadvideo.videodownloader.core.utils.FileUtil
import com.video.downloader.downloadvideo.videodownloader.core.utils.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class DirectHttpDownloadStrategy @Inject constructor(
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
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            val requestBuilder = Request.Builder().url(sourceUrl)
            val defaultUa = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
            requestBuilder.header("User-Agent", headers?.get("User-Agent") ?: defaultUa)

            headers?.forEach { (key, value) ->
                if (!key.equals("User-Agent", ignoreCase = true)) {
                    requestBuilder.header(key, value)
                }
            }

            try {
                val cookies = android.webkit.CookieManager.getInstance().getCookie(sourceUrl)
                if (!cookies.isNullOrEmpty() && headers?.containsKey("Cookie") != true) {
                    requestBuilder.header("Cookie", cookies)
                }
            } catch (_: Exception) {}

            val request = requestBuilder.build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                listener.onError(Exception("HTTP Error: ${response.code}"))
                return@withContext
            }

            val body = response.body ?: run {
                listener.onError(Exception("Response body is empty"))
                return@withContext
            }

            val totalBytes = body.contentLength()
            inputStream = body.byteStream()
            outputStream = mediaStoreHelper.openOutputStream(destinationUri) ?: run {
                listener.onError(Exception("Cannot open OutputStream for URI: $destinationUri"))
                return@withContext
            }

            val buffer = ByteArray(16 * 1024)
            var bytesCopied: Long = 0
            var bytesSinceLastSpeedCheck: Long = 0
            var lastSpeedCheckTime = System.currentTimeMillis()
            var speedText = "0 KB/s"

            while (true) {
                if (isCancelled.get()) {
                    listener.onError(Exception("Download cancelled by user"))
                    return@withContext
                }

                val read = inputStream.read(buffer)
                if (read == -1) break

                outputStream.write(buffer, 0, read)
                bytesCopied += read
                bytesSinceLastSpeedCheck += read

                val now = System.currentTimeMillis()
                val elapsed = now - lastSpeedCheckTime
                if (elapsed >= 500) {
                    val speedBytesPerSec = (bytesSinceLastSpeedCheck * 1000) / elapsed
                    speedText = "${FileUtil.formatFileSize(speedBytesPerSec)}/s"
                    bytesSinceLastSpeedCheck = 0
                    lastSpeedCheckTime = now

                    val progressPercent = if (totalBytes > 0) ((bytesCopied * 100) / totalBytes).toInt() else 0
                    listener.onProgress(bytesCopied, totalBytes, progressPercent, speedText)
                }
            }

            outputStream.flush()

            if (bytesCopied < 5 * 1024) {
                listener.onError(Exception("Downloaded file is too small or corrupted"))
                return@withContext
            }

            mediaStoreHelper.markVideoReady(destinationUri)
            listener.onComplete(destinationUri)

        } catch (e: Exception) {
            listener.onError(e)
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
        }
    }

    override fun cancel() {
        isCancelled.set(true)
    }
}
