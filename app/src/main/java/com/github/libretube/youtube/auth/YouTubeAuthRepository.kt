package com.github.libretube.youtube.auth

import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class YouTubeAuthRepository(
    private val clientId: String,
) {
    private val converter = JsonHelper.json.asConverterFactory("application/json".toMediaType())

    private val api: YouTubeOAuthApi = Retrofit.Builder()
        .baseUrl(OAUTH_BASE_URL)
        .client(RetrofitInstance.httpClient)
        .addConverterFactory(converter)
        .build()
        .create(YouTubeOAuthApi::class.java)

    suspend fun requestDeviceCode(scope: String): DeviceCodeResponse {
        return api.deviceCode(clientId = clientId, scope = scope)
    }

    suspend fun pollDeviceToken(deviceCode: String): TokenResponse {
        return api.token(clientId = clientId, deviceCode = deviceCode)
    }

    suspend fun refresh(refreshToken: String): TokenResponse {
        return api.refresh(clientId = clientId, refreshToken = refreshToken)
    }

    companion object {
        const val OAUTH_BASE_URL = "https://oauth2.googleapis.com/"
    }
}

