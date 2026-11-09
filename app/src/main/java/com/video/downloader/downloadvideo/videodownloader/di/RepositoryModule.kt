package com.video.downloader.downloadvideo.videodownloader.di

import com.video.downloader.downloadvideo.videodownloader.data.repository.DownloadRepositoryImpl
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        impl: DownloadRepositoryImpl
    ): DownloadRepository
}
