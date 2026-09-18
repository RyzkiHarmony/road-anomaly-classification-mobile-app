package com.pemalang.roaddamage.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("api/trips/upload")
    suspend fun uploadTrip(
        @Part("userId") userId: RequestBody,
        @Part("tripId") tripId: RequestBody,
        @Part("metadata") metadata: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    @POST("api/rag/anomaly-context")
    suspend fun getAnomalyContext(
        @retrofit2.http.Body request: AnomalyContextRequest
    ): Response<AnomalyContextResponse>

    @GET("api/anomalies/all")
    suspend fun getAllAnomalies(): Response<List<AnomalyPointDto>>
}

data class UploadResponse(
    val success: Boolean,
    val message: String?
)


