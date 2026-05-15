package com.example.flashlearn.di

import android.content.Context
import android.content.SharedPreferences
import com.example.flashlearn.data.AuthRepositoryImpl
import com.example.flashlearn.data.remote.AuthApiService
import com.example.flashlearn.data.remote.CategoryApiService
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.data.remote.DeckApiService
import com.example.flashlearn.data.remote.FlashcardApiService
import com.example.flashlearn.data.remote.MarketplaceApiService
import com.example.flashlearn.data.remote.StatsApiService
import com.example.flashlearn.data.remote.SessionApiService
import com.example.flashlearn.data.repository.CategoryRepository
import com.example.flashlearn.data.repository.DeckRepository
import com.example.flashlearn.data.repository.FlashcardRepository
import com.example.flashlearn.data.repository.MarketplaceRepository
import com.example.flashlearn.data.repository.StatsRepository
import com.example.flashlearn.data.repository.SessionRepository
import com.example.flashlearn.domain.repository.AuthRepository
import com.example.flashlearn.sync.SyncManager
import com.flashlearn.data.dao.DeckDao
import com.flashlearn.data.dao.FlashcardDao
import com.flashlearn.data.dao.FlashcardProgressDao
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
    fun provideFlashcardProgressDao(db: AppDatabase): FlashcardProgressDao =
        db.flashcardProgressDao()

    @Provides
    @Singleton
    fun provideSyncManager(@ApplicationContext context: Context): SyncManager =
        SyncManager(context)

    @Provides
    @Singleton
    fun provideDeckRepository(
        deckDao: DeckDao, 
        syncManager: SyncManager,
        deckApi: DeckApiService
    ): DeckRepository =
        DeckRepository(deckDao, syncManager, deckApi)

    @Provides
    @Singleton
    fun provideFlashcardRepository(
        flashcardDao: FlashcardDao, 
        deckDao: DeckDao, 
        syncManager: SyncManager,
        flashcardApi: FlashcardApiService
    ): FlashcardRepository =
        FlashcardRepository(flashcardDao, deckDao, syncManager, flashcardApi)

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
    fun provideDeckApiService(): DeckApiService {
        return RetrofitClient.deckApi
    }

    @Provides
    @Singleton
    fun provideCategoryApiService(): CategoryApiService {
        return RetrofitClient.categoryApi
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryApi: CategoryApiService): CategoryRepository {
        return CategoryRepository(categoryApi)
    }

    @Provides
    @Singleton
    fun provideFlashcardApiService(): FlashcardApiService {
        return RetrofitClient.flashcardApi
    }

    @Provides
    @Singleton
    fun provideStatsApiService(): StatsApiService {
        return RetrofitClient.statsApi
    }

    @Provides
    @Singleton
    fun provideStatsRepository(statsApi: StatsApiService): StatsRepository {
        return StatsRepository(statsApi)
    }

    @Provides
    @Singleton
    fun provideSessionApiService(): SessionApiService {
        return RetrofitClient.sessionApi
    }

    @Provides
    @Singleton
    fun provideSessionRepository(sessionApi: SessionApiService): SessionRepository {
        return SessionRepository(sessionApi)
    }

    @Provides
    @Singleton
    fun provideMarketplaceApiService(): MarketplaceApiService {
        return RetrofitClient.marketplaceApi
    }

    @Provides
    @Singleton
    fun provideMarketplaceRepository(
        api: MarketplaceApiService,
        deckDao: DeckDao,
        flashcardDao: FlashcardDao
    ): MarketplaceRepository {
        return MarketplaceRepository(api, deckDao, flashcardDao)
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
        db: AppDatabase
    ): AuthRepository {
        return AuthRepositoryImpl(api, db)
    }
}
