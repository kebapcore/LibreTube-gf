package com.github.libretube.youtube

import com.github.libretube.api.JsonHelper
import com.github.libretube.api.RetrofitInstance
import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Subscription
import com.github.libretube.extensions.toID
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import com.github.libretube.util.TextUtils.parseDurationString
import com.github.libretube.youtube.api.CommentSnippet
import com.github.libretube.youtube.api.CommentThreadInsertBody
import com.github.libretube.youtube.api.CommentThreadSnippet
import com.github.libretube.youtube.api.PlaylistInsertBody
import com.github.libretube.youtube.api.PlaylistInsertSnippet
import com.github.libretube.youtube.api.PlaylistItemInsertBody
import com.github.libretube.youtube.api.PlaylistItemInsertSnippet
import com.github.libretube.youtube.api.PlaylistStatus
import com.github.libretube.youtube.api.PlaylistUpdateBody
import com.github.libretube.youtube.api.PlaylistUpdateSnippet
import com.github.libretube.youtube.api.ResourceId
import com.github.libretube.youtube.api.SubscriptionInsertBody
import com.github.libretube.youtube.api.SubscriptionInsertSnippet
import com.github.libretube.youtube.api.TopLevelComment
import com.github.libretube.youtube.api.YouTubeDataApi
import com.github.libretube.youtube.api.YouTubePlaylistItem
import com.github.libretube.youtube.api.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant

class YouTubeDataRepository {
    private val converter = JsonHelper.json.asConverterFactory("application/json".toMediaType())

    private val api: YouTubeDataApi = Retrofit.Builder()
        .baseUrl(DATA_API_BASE_URL)
        .client(RetrofitInstance.httpClient)
        .addConverterFactory(converter)
        .build()
        .create(YouTubeDataApi::class.java)

    private suspend fun authHeader(): String {
        val token = YouTubeAuthManager.ensureValidAccessToken()
        return "Bearer $token"
    }

    suspend fun getMyChannelRelatedPlaylists(): com.github.libretube.youtube.api.RelatedPlaylists? {
        val res = api.channels(
            authorization = authHeader(),
            part = "snippet,contentDetails",
            mine = true
        )
        return res.items.firstOrNull()?.contentDetails?.relatedPlaylists
    }

    suspend fun getMySubscriptions(): List<Subscription> = withContext(Dispatchers.IO) {
        val auth = authHeader()
        val out = mutableListOf<Subscription>()
        var pageToken: String? = null

        do {
            val res = api.subscriptions(
                authorization = auth,
                part = "snippet",
                mine = true,
                pageToken = pageToken
            )
            out += res.items.mapNotNull { item ->
                val channelId = item.snippet?.resourceId?.channelId ?: item.snippet?.channelId ?: return@mapNotNull null
                Subscription(
                    url = "$YOUTUBE_FRONTEND_URL/channel/$channelId",
                    name = item.snippet?.title,
                    avatar = item.snippet?.thumbnails?.high?.url ?: item.snippet?.thumbnails?.medium?.url,
                    verified = false
                )
            }
            pageToken = res.nextPageToken
        } while (!pageToken.isNullOrBlank())

        out
    }

    suspend fun isSubscribed(channelId: String): Boolean = withContext(Dispatchers.IO) {
        val res = api.subscriptions(
            authorization = authHeader(),
            part = "id",
            mine = true,
            forChannelId = channelId
        )
        res.items.isNotEmpty()
    }

    suspend fun subscribe(channelId: String) {
        api.subscribe(
            authorization = authHeader(),
            body = SubscriptionInsertBody(
                snippet = SubscriptionInsertSnippet(
                    resourceId = ResourceId(kind = "youtube#channel", channelId = channelId)
                )
            )
        )
    }

    suspend fun unsubscribe(channelId: String) {
        val auth = authHeader()
        val res = api.subscriptions(
            authorization = auth,
            part = "id",
            mine = true,
            forChannelId = channelId
        )
        val subscriptionId = res.items.firstOrNull()?.id ?: return
        api.unsubscribe(authorization = auth, subscriptionId = subscriptionId)
    }

    suspend fun getUploadsFromSubscriptions(
        channelIds: List<String>,
        perChannel: Int = 2,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        if (channelIds.isEmpty()) return@withContext emptyList()
        val auth = authHeader()

        val total = channelIds.size
        val videoIdsWithPublished = mutableListOf<Pair<String, String?>>()

        // Fetch per-channel upload activity (cheap quota-wise) and collect video IDs.
        channelIds.forEachIndexed { index, channelId ->
            val activities = runCatching {
                api.activities(
                    authorization = auth,
                    part = "snippet,contentDetails",
                    channelId = channelId,
                    maxResults = perChannel
                ).items
            }.getOrDefault(emptyList())

            activities.forEach { act ->
                val videoId = act.contentDetails?.upload?.videoId ?: return@forEach
                videoIdsWithPublished += videoId to act.snippet?.publishedAt
            }
            onProgress(index + 1, total)
        }

        val videoIds = videoIdsWithPublished.map { it.first }.distinct()
        val publishedAtById = videoIdsWithPublished.toMap()

        // Batch fetch details for videos (50 ids per call).
        val parts = "snippet,contentDetails,statistics"
        val videos = videoIds.chunked(50).map { chunk ->
            async {
                api.videos(
                    authorization = auth,
                    part = parts,
                    ids = chunk.joinToString(",")
                ).items
            }
        }.awaitAll().flatten()

        videos.mapNotNull { it.toStreamItem(publishedAtById[it.id]) }
            .sortedByDescending { it.uploaded }
    }

    suspend fun getMyPlaylists(): List<Playlists> = withContext(Dispatchers.IO) {
        val auth = authHeader()
        val out = mutableListOf<Playlists>()
        var pageToken: String? = null
        do {
            val res = api.playlists(
                authorization = auth,
                part = "snippet,contentDetails",
                mine = true,
                pageToken = pageToken
            )
            out += res.items.mapNotNull { p ->
                val id = p.id ?: return@mapNotNull null
                Playlists(
                    id = id,
                    name = p.snippet?.title,
                    shortDescription = p.snippet?.description,
                    thumbnail = p.snippet?.thumbnails?.high?.url ?: p.snippet?.thumbnails?.medium?.url,
                    videos = p.contentDetails?.itemCount ?: 0
                )
            }
            pageToken = res.nextPageToken
        } while (!pageToken.isNullOrBlank())
        out
    }

    suspend fun getPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        val auth = authHeader()
        val playlistMeta = api.playlists(
            authorization = auth,
            part = "snippet,contentDetails",
            mine = false,
            id = playlistId
        ).items.firstOrNull()

        val (items, nextPage) = getAllPlaylistItems(auth, playlistId, maxPages = 1)
        val videos = items.mapNotNull { it.snippet?.resourceId?.videoId }.distinct()
        val videoDetails = if (videos.isNotEmpty()) {
            api.videos(
                authorization = auth,
                part = "snippet,contentDetails,statistics",
                ids = videos.take(50).joinToString(",")
            ).items.associateBy { it.id }
        } else emptyMap()

        val streams = items.mapNotNull { pi ->
            val videoId = pi.snippet?.resourceId?.videoId ?: return@mapNotNull null
            videoDetails[videoId]?.toStreamItem(pi.snippet?.publishedAt)
        }

        Playlist(
            name = playlistMeta?.snippet?.title,
            description = playlistMeta?.snippet?.description,
            thumbnailUrl = playlistMeta?.snippet?.thumbnails?.high?.url ?: playlistMeta?.snippet?.thumbnails?.medium?.url,
            videos = (playlistMeta?.contentDetails?.itemCount ?: streams.size.toLong()).toInt(),
            nextpage = nextPage,
            relatedStreams = streams
        )
    }

    private suspend fun getAllPlaylistItems(
        auth: String,
        playlistId: String,
        maxPages: Int = 10,
    ): Pair<List<YouTubePlaylistItem>, String?> {
        val out = mutableListOf<YouTubePlaylistItem>()
        var pageToken: String? = null
        var pages = 0
        do {
            val res = api.playlistItems(
                authorization = auth,
                part = "snippet",
                playlistId = playlistId,
                pageToken = pageToken
            )
            out += res.items
            pageToken = res.nextPageToken
            pages++
        } while (!pageToken.isNullOrBlank() && pages < maxPages)
        return out to pageToken
    }

    suspend fun createPlaylist(title: String): String? {
        val res = api.createPlaylist(
            authorization = authHeader(),
            body = PlaylistInsertBody(
                snippet = PlaylistInsertSnippet(title = title),
                status = PlaylistStatus(privacyStatus = "private")
            )
        )
        return res.id
    }

    suspend fun updatePlaylist(playlistId: String, title: String, description: String?): Boolean {
        api.updatePlaylist(
            authorization = authHeader(),
            body = PlaylistUpdateBody(
                id = playlistId,
                snippet = PlaylistUpdateSnippet(title = title, description = description)
            )
        )
        return true
    }

    suspend fun deletePlaylist(playlistId: String): Boolean {
        api.deletePlaylist(authorization = authHeader(), playlistId = playlistId)
        return true
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String): Boolean {
        api.addPlaylistItem(
            authorization = authHeader(),
            body = PlaylistItemInsertBody(
                snippet = PlaylistItemInsertSnippet(
                    playlistId = playlistId,
                    resourceId = ResourceId(kind = "youtube#video", videoId = videoId)
                )
            )
        )
        return true
    }

    suspend fun removeFromPlaylistByIndex(playlistId: String, index: Int): Boolean {
        val auth = authHeader()
        val items = api.playlistItems(
            authorization = auth,
            part = "snippet",
            playlistId = playlistId,
            maxResults = 50,
            pageToken = null
        ).items
        val playlistItemId = items.getOrNull(index)?.id ?: return false
        api.deletePlaylistItem(authorization = auth, playlistItemId = playlistItemId)
        return true
    }

    suspend fun rateVideo(videoId: String, rating: String) {
        api.rateVideo(authorization = authHeader(), videoId = videoId, rating = rating)
    }

    suspend fun comment(videoId: String, text: String) {
        api.insertCommentThread(
            authorization = authHeader(),
            body = CommentThreadInsertBody(
                snippet = CommentThreadSnippet(
                    videoId = videoId,
                    topLevelComment = TopLevelComment(
                        snippet = CommentSnippet(textOriginal = text)
                    )
                )
            )
        )
    }

    private fun YouTubeVideo.toStreamItem(fallbackPublishedAt: String?): StreamItem? {
        val id = this.id ?: return null
        val snip = this.snippet ?: return null

        val publishedAt = snip.publishedAt ?: fallbackPublishedAt
        val uploadedMillis = runCatching {
            publishedAt?.let { Instant.parse(it).toEpochMilli() } ?: 0L
        }.getOrDefault(0L)

        val durationSeconds = contentDetails?.duration
            ?.parseDurationString()
            ?.toLong()

        val views = statistics?.viewCount?.toLongOrNull()

        val thumb = snip.thumbnails?.maxres?.url
            ?: snip.thumbnails?.standard?.url
            ?: snip.thumbnails?.high?.url
            ?: snip.thumbnails?.medium?.url
            ?: snip.thumbnails?.default?.url

        val channelId = snip.channelId
        val uploaderUrl = channelId?.let { "$YOUTUBE_FRONTEND_URL/channel/$it" }

        return StreamItem(
            url = "$YOUTUBE_FRONTEND_URL/watch?v=$id",
            type = StreamItem.TYPE_STREAM,
            title = snip.title,
            thumbnail = thumb,
            uploaderName = snip.channelTitle,
            uploaderUrl = uploaderUrl,
            uploaderAvatar = null,
            uploadedDate = publishedAt,
            duration = durationSeconds,
            views = views,
            uploaderVerified = false,
            uploaded = uploadedMillis,
            shortDescription = null,
            isShort = false
        )
    }

    companion object {
        const val DATA_API_BASE_URL = "https://www.googleapis.com/youtube/v3/"
    }
}

