package com.arvin.smartdocmobile.net

import android.util.Log
import com.arvin.smartdocmobile.model.VerifyRequest
import com.arvin.smartdocmobile.model.VerifyResponse
import com.yalantis.ucrop.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("/smart-doc-verification-admin/api.php")
    suspend fun verifyImage(@Body body: VerifyRequest): VerifyResponse
}

object ApiClient {
    private const val BASE_URL = "http://192.168.1.7"

    private fun buildLogger(): HttpLoggingInterceptor {
        val logger = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }
        logger.level = HttpLoggingInterceptor.Level.BODY
        return logger
    }


    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(buildLogger())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}