package com.video.downloader.downloadvideo.videodownloader.data.network

import com.video.downloader.downloadvideo.videodownloader.data.model.DownloadModel
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class UrlRequestBody(
    val url: String
)

interface VideoExtractApiService {

    @Headers("Content-Type: application/json; charset=UTF-8")
    @POST("videodownloader/download")
    suspend fun extractVideo(
        @Body body: UrlRequestBody
    ): Response<DownloadModel>

    @Headers("Content-Type: application/json; charset=UTF-8")
    @POST("videodownloader/tiktok")
    suspend fun extractTiktok(
        @Body body: UrlRequestBody
    ): Response<DownloadModel>
}
