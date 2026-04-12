package com.example.flashlearn.data.remote

import com.example.flashlearn.data.remote.dto.SyncPullResponse
import com.example.flashlearn.data.remote.dto.SyncPushRequest
import com.example.flashlearn.data.remote.dto.SyncPushResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SyncApiService {

    @POST("/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("/sync/pull")
    suspend fun pull(
        @Query("since") since: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 50
    ): SyncPullResponse
}
