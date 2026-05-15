package com.example.flashlearn.data.remote

import android.content.Context
import com.example.flashlearn.data.local.TokenManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private var retrofit: Retrofit? = null

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

    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        if (responseCount(response) >= 2 || response.request.url.encodedPath == "/auth/refresh-token") {
            return@Authenticator null
        }

        val refreshToken = TokenManager.getRefreshToken() ?: return@Authenticator null
        val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

        synchronized(this) {
            val latestToken = TokenManager.getAccessToken()
            if (latestToken != null && latestToken != requestToken) {
                return@synchronized response.request.withBearer(latestToken)
            }

            refreshTokens(refreshToken)?.let { tokens ->
                TokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                return@synchronized response.request.withBearer(tokens.accessToken)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .build()

    private fun refreshTokens(refreshToken: String): RefreshTokenResponse? {
        return runCatching {
            val refreshApi = Retrofit.Builder()
                .baseUrl(Config.getBaseUrl())
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .build()
                )
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApiService::class.java)

            val response = refreshApi.refreshToken(RefreshTokenRequest(refreshToken)).execute()
            if (response.isSuccessful) response.body() else null
        }.getOrNull()
    }

    private fun responseCount(response: Response): Int {
        var current: Response? = response
        var count = 1
        while (current?.priorResponse != null) {
            count++
            current = current.priorResponse
        }
        return count
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    fun init(context: Context) {
        Config.init(context)
        TokenManager.init(context)
        TokenManager.migrateFrom(context.getSharedPreferences("flashlearn_prefs", Context.MODE_PRIVATE))
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
