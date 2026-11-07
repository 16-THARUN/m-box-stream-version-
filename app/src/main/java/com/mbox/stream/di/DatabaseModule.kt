package com.mbox.stream.di

import android.content.Context
import com.mbox.stream.db.StreamDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideStreamDatabase(@ApplicationContext context: Context): StreamDatabase {
        return StreamDatabase.buildDatabase(context)
    }
}
