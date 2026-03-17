package com.example.flashlearn.di

import android.content.Context
import android.content.SharedPreferences
import com.example.flashlearn.data.AuthRepositoryImpl
import com.example.flashlearn.data.remote.AuthApiService
import com.example.flashlearn.data.remote.RetrofitClient
import com.example.flashlearn.domain.repository.AuthRepository
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
    fun provideAuthApiService(): AuthApiService {
        return RetrofitClient.authApi
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