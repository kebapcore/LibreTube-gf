package com.github.libretube.youtube.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeListResponse<T>(
    val items: List<T> = emptyList(),
    val kind: String? = null,
    val etag: String? = null,
    val pageInfo: PageInfo? = null,
    val nextPageToken: String? = null,
)

@Serializable
data class PageInfo(
    val totalResults: Int? = null,
    val resultsPerPage: Int? = null,
)

@Serializable
data class Thumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
data class Thumbnails(
    val default: Thumbnail? = null,
    val medium: Thumbnail? = null,
    val high: Thumbnail? = null,
    val standard: Thumbnail? = null,
    val maxres: Thumbnail? = null,
)

@Serializable
data class YouTubeChannel(
    val id: String? = null,
    val snippet: ChannelSnippet? = null,
    val contentDetails: ChannelContentDetails? = null,
)

@Serializable
data class ChannelSnippet(
    val title: String? = null,
    val thumbnails: Thumbnails? = null,
)

@Serializable
data class ChannelContentDetails(
    val relatedPlaylists: RelatedPlaylists? = null,
)

@Serializable
data class RelatedPlaylists(
    val likes: String? = null,
    val favorites: String? = null,
    val uploads: String? = null,
    val watchHistory: String? = null,
    val watchLater: String? = null,
)

@Serializable
data class YouTubeSubscription(
    val id: String? = null,
    val snippet: SubscriptionSnippet? = null,
)

@Serializable
data class SubscriptionSnippet(
    val title: String? = null,
    val channelId: String? = null,
    val thumbnails: Thumbnails? = null,
    val resourceId: ResourceId? = null,
)

@Serializable
data class ResourceId(
    val kind: String? = null,
    val channelId: String? = null,
    val videoId: String? = null,
)

@Serializable
data class SubscriptionInsertBody(
    val snippet: SubscriptionInsertSnippet,
)

@Serializable
data class SubscriptionInsertSnippet(
    val resourceId: ResourceId,
)

@Serializable
data class YouTubeActivity(
    val snippet: ActivitySnippet? = null,
    val contentDetails: ActivityContentDetails? = null,
)

@Serializable
data class ActivitySnippet(
    val publishedAt: String? = null,
)

@Serializable
data class ActivityContentDetails(
    val upload: ActivityUpload? = null,
)

@Serializable
data class ActivityUpload(
    val videoId: String? = null,
)

@Serializable
data class YouTubeVideo(
    val id: String? = null,
    val snippet: VideoSnippet? = null,
    val contentDetails: VideoContentDetails? = null,
    val statistics: VideoStatistics? = null,
)

@Serializable
data class VideoSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val channelId: String? = null,
    val publishedAt: String? = null,
    val thumbnails: Thumbnails? = null,
)

@Serializable
data class VideoContentDetails(
    val duration: String? = null,
)

@Serializable
data class VideoStatistics(
    val viewCount: String? = null,
    val likeCount: String? = null,
)

@Serializable
data class YouTubePlaylist(
    val id: String? = null,
    val snippet: PlaylistSnippet? = null,
    val contentDetails: PlaylistContentDetails? = null,
)

@Serializable
data class PlaylistSnippet(
    val title: String? = null,
    val description: String? = null,
    val thumbnails: Thumbnails? = null,
)

@Serializable
data class PlaylistContentDetails(
    val itemCount: Long? = null,
)

@Serializable
data class PlaylistInsertBody(
    val snippet: PlaylistInsertSnippet,
    val status: PlaylistStatus? = null,
)

@Serializable
data class PlaylistInsertSnippet(
    val title: String,
    val description: String? = null,
)

@Serializable
data class PlaylistStatus(
    val privacyStatus: String = "private",
)

@Serializable
data class PlaylistUpdateBody(
    val id: String,
    val snippet: PlaylistUpdateSnippet,
)

@Serializable
data class PlaylistUpdateSnippet(
    val title: String,
    val description: String? = null,
)

@Serializable
data class YouTubePlaylistItem(
    val id: String? = null,
    val snippet: PlaylistItemSnippet? = null,
)

@Serializable
data class PlaylistItemSnippet(
    val playlistId: String? = null,
    val title: String? = null,
    val publishedAt: String? = null,
    val channelId: String? = null,
    val channelTitle: String? = null,
    val thumbnails: Thumbnails? = null,
    val resourceId: ResourceId? = null,
)

@Serializable
data class PlaylistItemInsertBody(
    val snippet: PlaylistItemInsertSnippet,
)

@Serializable
data class PlaylistItemInsertSnippet(
    val playlistId: String,
    val resourceId: ResourceId,
)

@Serializable
data class YouTubeCommentThread(
    val id: String? = null,
)

@Serializable
data class CommentThreadInsertBody(
    val snippet: CommentThreadSnippet,
)

@Serializable
data class CommentThreadSnippet(
    val videoId: String,
    val topLevelComment: TopLevelComment,
)

@Serializable
data class TopLevelComment(
    val snippet: CommentSnippet,
)

@Serializable
data class CommentSnippet(
    val textOriginal: String,
)

