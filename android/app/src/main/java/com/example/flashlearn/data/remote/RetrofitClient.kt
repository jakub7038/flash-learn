package com.example.flashlearn.data.remote

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var retrofit: Retrofit? = null
    private var prefs: SharedPreferences? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        TokenManager.getAccessToken()?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .build()

    fun init(context: Context) {
        Config.init(context)
        TokenManager.init(context)
        retrofit = Retrofit.Builder()
            .baseUrl(Config.getBaseUrl())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApiService
        get() = retrofit?.create(AuthApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val syncApi: SyncApiService
        get() = retrofit?.create(SyncApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val deckApi: DeckApiService
        get() = retrofit?.create(DeckApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val categoryApi: CategoryApiService
        get() = retrofit?.create(CategoryApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val flashcardApi: FlashcardApiService
        get() = retrofit?.create(FlashcardApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val statsApi: StatsApiService
        get() = retrofit?.create(StatsApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val sessionApi: SessionApiService
        get() = retrofit?.create(SessionApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")

    val marketplaceApi: MarketplaceApiService
        get() = retrofit?.create(MarketplaceApiService::class.java)
            ?: throw IllegalStateException("RetrofitClient not initialized. Call init(context) first.")
}
