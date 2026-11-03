package com.mbox.source.youtube.di

import com.mediaplayer.data.model.MusicSource
import com.mbox.source.youtube.YouTubeMusicSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class YouTubeSourceModule {

    @Multibinds
    abstract fun musicSources(): Set<MusicSource>

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindYouTubeMusicSource(impl: YouTubeMusicSource): MusicSource
}
