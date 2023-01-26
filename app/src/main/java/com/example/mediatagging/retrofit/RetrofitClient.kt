package com.example.mediatagging.retrofit

import android.util.Log
import com.example.mediatagging.model.response.GptResponse
import com.example.mediatagging.model.request.GptRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession


object RetrofitClient {
    private const val BASE_URL = "https://api.openai.com/"

    private val retrofit by lazy {

        val client = okhttpClient()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    val api by lazy {
        retrofit.create(ApiInterface::class.java)
    }

    fun okhttpClient(): OkHttpClient {
        try {
            val builder = OkHttpClient.Builder()
            val httpLogging = HttpLoggingInterceptor()
            httpLogging.setLevel(HttpLoggingInterceptor.Level.BODY)
            builder.hostnameVerifier(HostnameVerifier { hostname: String?, session: SSLSession? -> true })
            builder.connectTimeout(60, TimeUnit.SECONDS)
            builder.callTimeout(60, TimeUnit.SECONDS)
            builder.readTimeout(300, TimeUnit.SECONDS)
            builder.addInterceptor(HeaderInterceptor())
            builder.addInterceptor(httpLogging)
            return builder.build()
        } catch (e: Exception) {
            Log.i("Retrofit", "errorException: $e")
            throw RuntimeException(e)
        }
    }

    interface ApiInterface {
        @POST("v1/completions")
        fun getGptAnswer(
            @Body request: GptRequest
        ): Call<GptResponse>
    }

}
