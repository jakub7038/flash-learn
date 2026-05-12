package com.example.flashlearn.data.repository

import com.example.flashlearn.data.remote.StatsApiService
import com.example.flashlearn.data.remote.dto.StatsDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException

class StatsRepository(
    private val statsApi: StatsApiService
) {
    fun getStats(): Flow<Result<StatsDto>> = flow {
        try {
            val response = statsApi.getStats()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Result.success(it))
                } ?: emit(Result.failure(Exception("Pusta odpowiedź z serwera")))
            } else {
                emit(Result.failure(Exception("Błąd pobierania statystyk: ${response.code()}")))
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
