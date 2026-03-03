package com.github.libretube.youtube.auth

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Google OAuth 2.0 endpoints for TVs and limited-input devices (device code flow).
 */
interface YouTubeOAuthApi {
    @FormUrlEncoded
    @POST("device/code")
    suspend fun deviceCode(
        @Field("client_id") clientId: String,
        @Field("scope") scope: String
    ): DeviceCodeResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun token(
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code",
    ): TokenResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refresh(
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenResponse
}

