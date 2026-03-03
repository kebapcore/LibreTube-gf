package com.github.libretube.repo

import com.github.libretube.api.SubscriptionHelper
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.youtube.YouTubeDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeFeedRepository(
    private val repo: YouTubeDataRepository = YouTubeDataRepository()
) : FeedRepository {
    override suspend fun getFeed(
        forceRefresh: Boolean,
        onProgressUpdate: (FeedProgress) -> Unit
    ): List<StreamItem> = withContext(Dispatchers.IO) {
        val channelIds = SubscriptionHelper.getSubscriptionChannelIds()
        repo.getUploadsFromSubscriptions(
            channelIds = channelIds,
            perChannel = 2
        ) { current, total ->
            onProgressUpdate(FeedProgress(current, total))
        }
    }
}

