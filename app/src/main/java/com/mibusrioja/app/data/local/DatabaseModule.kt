package com.mibusrioja.app.data.local

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import java.io.File
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mibusrioja.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideApiCache(@ApplicationContext context: Context, moshi: Moshi): ApiCache =
        DailyApiCache(File(context.filesDir, "api_cache"), moshi)

    @Provides
    @Singleton
    fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()

    @Provides
    @Singleton
    fun provideFrequentDestinationDao(db: AppDatabase): FrequentDestinationDao =
        db.frequentDestinationDao()
}
