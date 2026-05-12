package com.example.flashlearn.data.repository

import com.example.flashlearn.data.remote.SessionApiService
import com.example.flashlearn.data.remote.dto.SessionRequest
import com.example.flashlearn.data.remote.dto.SessionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class SessionRepository(
    private val sessionApi: SessionApiService
) {
    fun saveSession(request: SessionRequest): Flow<Result<SessionResponse>> = flow {
        try {
            val response = sessionApi.saveSession(request)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Pusta odpowiedź z serwera")))
            } else {
                emit(Result.failure(Exception("Błąd zapisu sesji: ${response.code()}")))
            }
        } catch (e: HttpException) {
            emit(Result.failure(Exception("Błąd sieci: ${e.message}")))
        } catch (e: IOException) {
            emit(Result.failure(Exception("Brak połączenia z siecią: ${e.message}")))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
