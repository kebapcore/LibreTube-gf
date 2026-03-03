package com.github.libretube.youtube.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface YouTubeDataApi {
    @GET("channels")
    suspend fun channels(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("mine") mine: Boolean = true,
    ): YouTubeListResponse<YouTubeChannel>

    @GET("subscriptions")
    suspend fun subscriptions(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("mine") mine: Boolean = true,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
        @Query("forChannelId") forChannelId: String? = null,
    ): YouTubeListResponse<YouTubeSubscription>

    @POST("subscriptions")
    suspend fun subscribe(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Body body: SubscriptionInsertBody,
    ): YouTubeSubscription

    @DELETE("subscriptions")
    suspend fun unsubscribe(
        @Header("Authorization") authorization: String,
        @Query("id") subscriptionId: String,
    )

    @GET("activities")
    suspend fun activities(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("channelId") channelId: String,
        @Query("maxResults") maxResults: Int = 5,
        @Query("pageToken") pageToken: String? = null,
    ): YouTubeListResponse<YouTubeActivity>

    @GET("videos")
    suspend fun videos(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("id") ids: String,
        @Query("maxResults") maxResults: Int = 50,
    ): YouTubeListResponse<YouTubeVideo>

    @GET("playlists")
    suspend fun playlists(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("mine") mine: Boolean = true,
        @Query("id") id: String? = null,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): YouTubeListResponse<YouTubePlaylist>

    @POST("playlists")
    suspend fun createPlaylist(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet,status",
        @Body body: PlaylistInsertBody,
    ): YouTubePlaylist

    @PUT("playlists")
    suspend fun updatePlaylist(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Body body: PlaylistUpdateBody,
    ): YouTubePlaylist

    @DELETE("playlists")
    suspend fun deletePlaylist(
        @Header("Authorization") authorization: String,
        @Query("id") playlistId: String,
    )

    @GET("playlistItems")
    suspend fun playlistItems(
        @Header("Authorization") authorization: String,
        @Query("part") part: String,
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int = 50,
        @Query("pageToken") pageToken: String? = null,
    ): YouTubeListResponse<YouTubePlaylistItem>

    @POST("playlistItems")
    suspend fun addPlaylistItem(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Body body: PlaylistItemInsertBody,
    ): YouTubePlaylistItem

    @DELETE("playlistItems")
    suspend fun deletePlaylistItem(
        @Header("Authorization") authorization: String,
        @Query("id") playlistItemId: String,
    )

    @POST("commentThreads")
    suspend fun insertCommentThread(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet",
        @Body body: CommentThreadInsertBody,
    ): YouTubeCommentThread

    @POST("videos/rate")
    suspend fun rateVideo(
        @Header("Authorization") authorization: String,
        @Query("id") videoId: String,
        @Query("rating") rating: String,
    )
}

