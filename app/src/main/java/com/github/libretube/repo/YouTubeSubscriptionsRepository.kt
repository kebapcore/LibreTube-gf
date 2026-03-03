package com.github.libretube.repo

import com.github.libretube.api.obj.Subscription
import com.github.libretube.youtube.YouTubeDataRepository

class YouTubeSubscriptionsRepository(
    private val repo: YouTubeDataRepository = YouTubeDataRepository()
) : SubscriptionsRepository {
    override suspend fun subscribe(
        channelId: String,
        name: String,
        uploaderAvatar: String?,
        verified: Boolean
    ) {
        repo.subscribe(channelId)
    }

    override suspend fun unsubscribe(channelId: String) {
        repo.unsubscribe(channelId)
    }

    override suspend fun isSubscribed(channelId: String): Boolean? {
        return repo.isSubscribed(channelId)
    }

    override suspend fun importSubscriptions(newChannels: List<String>) {
        // Best-effort import; ignore failures per channel
        newChannels.forEach { channelId ->
            runCatching { repo.subscribe(channelId) }
        }
    }

    override suspend fun getSubscriptions(): List<Subscription> {
        return repo.getMySubscriptions()
    }

    override suspend fun getSubscriptionChannelIds(): List<String> {
        return getSubscriptions().mapNotNull { it.url?.substringAfter("/channel/") }
    }
}

