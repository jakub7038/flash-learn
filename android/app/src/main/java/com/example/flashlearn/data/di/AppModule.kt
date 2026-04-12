package com.example.flashlearn.di

import android.content.Context
import android.content.SharedPreferences
import com.example.flashlearn.data.AuthRepositoryImpl
import com.example.flashlearn.data.remote.AuthApiService
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.data.repository.DeckRepository
import com.example.flashlearn.domain.repository.AuthRepository
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideDeckDao(db: AppDatabase): DeckDao = db.deckDao()

    @Provides
    @Singleton
    fun provideFlashcardDao(db: AppDatabase): FlashcardDao = db.flashcardDao()

    @Provides
    @Singleton
    fun provideDeckRepository(deckDao: DeckDao): DeckRepository = DeckRepository(deckDao)

    @Provides
    @Singleton
    fun provideAuthApiService(): AuthApiService {
        return RetrofitClient.authApi
    }

    @Provides
    @Singleton
    fun provideSyncApiService(): com.example.flashlearn.data.remote.SyncApiService {
        return RetrofitClient.syncApi
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences("flashlearn_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApiService,
        prefs: SharedPreferences
    ): AuthRepository {
        return AuthRepositoryImpl(api, prefs)
    }
}