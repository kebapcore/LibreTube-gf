package com.github.libretube.youtube

import com.github.libretube.BuildConfig
import com.github.libretube.helpers.PreferenceHelper
import com.github.libretube.youtube.auth.DeviceCodeResponse
import com.github.libretube.youtube.auth.TokenResponse
import com.github.libretube.youtube.auth.YouTubeAuthRepository
import kotlinx.coroutines.delay
import kotlin.math.max

object YouTubeAuthManager {
    // Minimal set for reading + rating/commenting.
    // Note: YouTube Data API does NOT expose personalized home recommendations like the official app.
    private const val DEFAULT_SCOPE =
        "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/youtube.force-ssl"

    private fun repo(): YouTubeAuthRepository {
        val clientId = BuildConfig.YOUTUBE_OAUTH_CLIENT_ID.trim()
        require(clientId.isNotEmpty()) {
            "Missing BuildConfig.YOUTUBE_OAUTH_CLIENT_ID. Set Gradle property YOUTUBE_OAUTH_CLIENT_ID."
        }
        return YouTubeAuthRepository(clientId)
    }

    fun isSignedIn(): Boolean {
        return PreferenceHelper.getYouTubeRefreshToken().isNotEmpty() ||
                PreferenceHelper.getYouTubeAccessToken().isNotEmpty()
    }

    suspend fun startDeviceFlow(scope: String = DEFAULT_SCOPE): DeviceCodeResponse {
        return repo().requestDeviceCode(scope)
    }

    suspend fun completeDeviceFlow(
        deviceCode: String,
        pollingIntervalSeconds: Long,
        expiresInSeconds: Long,
        scope: String = DEFAULT_SCOPE,
        onStatus: (status: String) -> Unit = {}
    ): TokenResponse {
        val startMillis = System.currentTimeMillis()
        val expiresAtMillis = startMillis + expiresInSeconds * 1000
        var intervalSeconds = max(1, pollingIntervalSeconds)

        while (System.currentTimeMillis() < expiresAtMillis) {
            val res = runCatching { repo().pollDeviceToken(deviceCode) }.getOrElse { ex ->
                onStatus("network_error")
                delay(intervalSeconds * 1000)
                return@while
            }

            // Success
            if (!res.accessToken.isNullOrBlank() && res.expiresInSeconds != null) {
                saveToken(
                    accessToken = res.accessToken,
                    refreshToken = res.refreshToken,
                    expiresInSeconds = res.expiresInSeconds,
                    scope = res.scope ?: scope
                )
                return res
            }

            when (res.error) {
                "authorization_pending" -> {
                    onStatus("pending")
                    delay(intervalSeconds * 1000)
                }

                "slow_down" -> {
                    intervalSeconds += 5
                    onStatus("slow_down")
                    delay(intervalSeconds * 1000)
                }

                "access_denied" -> return res
                "expired_token" -> return res
                else -> {
                    onStatus(res.error.orEmpty())
                    delay(intervalSeconds * 1000)
                }
            }
        }

        return TokenResponse(error = "expired_token", errorDescription = "Device code expired")
    }

    suspend fun ensureValidAccessToken(): String {
        val current = PreferenceHelper.getYouTubeAccessToken()
        val expiresAt = PreferenceHelper.getYouTubeTokenExpiresAtEpochMillis()
        val now = System.currentTimeMillis()
        if (current.isNotEmpty() && expiresAt > now + 30_000) return current

        val refreshToken = PreferenceHelper.getYouTubeRefreshToken()
        if (refreshToken.isEmpty()) return ""

        val res = repo().refresh(refreshToken)
        if (!res.accessToken.isNullOrBlank() && res.expiresInSeconds != null) {
            saveToken(
                accessToken = res.accessToken,
                refreshToken = res.refreshToken, // usually null on refresh
                expiresInSeconds = res.expiresInSeconds,
                scope = res.scope ?: PreferenceHelper.getYouTubeScope()
            )
            return res.accessToken
        }
        return ""
    }

    fun signOut() {
        PreferenceHelper.clearYouTubeAuth()
    }

    private fun saveToken(
        accessToken: String,
        refreshToken: String?,
        expiresInSeconds: Long,
        scope: String,
    ) {
        PreferenceHelper.setYouTubeAccessToken(accessToken)
        refreshToken?.takeIf { it.isNotBlank() }?.let { PreferenceHelper.setYouTubeRefreshToken(it) }
        PreferenceHelper.setYouTubeScope(scope)
        PreferenceHelper.setYouTubeTokenExpiresAtEpochMillis(System.currentTimeMillis() + expiresInSeconds * 1000)
    }
}

