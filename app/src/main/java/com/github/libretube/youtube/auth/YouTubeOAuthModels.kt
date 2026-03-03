package com.github.libretube.youtube.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code")
    val deviceCode: String,
    @SerialName("user_code")
    val userCode: String,
    // Google historically used verification_url; newer responses may use verification_uri.
    @SerialName("verification_url")
    val verificationUrl: String? = null,
    @SerialName("verification_uri")
    val verificationUri: String? = null,
    @SerialName("expires_in")
    val expiresInSeconds: Long,
    val interval: Long = 5,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("expires_in")
    val expiresInSeconds: Long? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val scope: String? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    // Error responses:
    val error: String? = null,
    @SerialName("error_description")
    val errorDescription: String? = null,
)

