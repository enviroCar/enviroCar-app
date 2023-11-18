package org.envirocar.voicecommand.service

import okhttp3.ResponseBody
import org.envirocar.voicecommand.model.VoiceModel
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface VoiceModelService {

    @GET("models")
    suspend fun getModelsList(): Response<List<VoiceModel>>

    @Streaming
    @GET("models/{name}")
    suspend fun getModel(@Path("name") name: String): ResponseBody


}

object VoiceModelNetwork {

    const val BASE_URL = "http://ec2-15-206-186-192.ap-south-1.compute.amazonaws.com:8000/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val voiceModel = retrofit.create(VoiceModelService::class.java)

}