package com.z22zzw.dailycheckin.data.api

import com.z22zzw.dailycheckin.data.api.dto.DeepSeekRequest
import com.z22zzw.dailycheckin.data.api.dto.DeepSeekResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Streaming

interface DeepSeekApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chatCompletion(@Body request: DeepSeekRequest): Response<DeepSeekResponse>

    @Streaming
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chatCompletionStream(@Body request: DeepSeekRequest): Response<okhttp3.ResponseBody>
}
