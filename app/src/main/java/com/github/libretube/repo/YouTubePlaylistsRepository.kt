package com.github.libretube.repo

import com.github.libretube.api.obj.Playlist
import com.github.libretube.api.obj.Playlists
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.obj.PipedImportPlaylist
import com.github.libretube.youtube.YouTubeDataRepository

class YouTubePlaylistsRepository(
    private val repo: YouTubeDataRepository = YouTubeDataRepository()
) : PlaylistRepository {
    override suspend fun getPlaylist(playlistId: String): Playlist {
        return repo.getPlaylist(playlistId)
    }

    override suspend fun getPlaylists(): List<Playlists> {
        return repo.getMyPlaylists()
    }

    override suspend fun addToPlaylist(playlistId: String, vararg videos: StreamItem): Boolean {
        videos.forEach { item ->
            val videoId = item.url?.substringAfter("v=")?.takeIf { it.isNotBlank() } ?: return@forEach
            runCatching { repo.addToPlaylist(playlistId, videoId) }
        }
        return true
    }

    override suspend fun renamePlaylist(playlistId: String, newName: String): Boolean {
        return repo.updatePlaylist(playlistId, title = newName, description = null)
    }

    override suspend fun changePlaylistDescription(playlistId: String, newDescription: String): Boolean {
        // keep the current title: fetch the playlist title quickly
        val current = repo.getPlaylist(playlistId).name ?: ""
        return repo.updatePlaylist(playlistId, title = current, description = newDescription)
    }

    override suspend fun clonePlaylist(playlistId: String): String? {
        // Not implemented yet (requires copying items)
        return null
    }

    override suspend fun removeFromPlaylist(playlistId: String, index: Int): Boolean {
        return repo.removeFromPlaylistByIndex(playlistId, index)
    }

    override suspend fun importPlaylists(playlists: List<PipedImportPlaylist>) {
        // Not supported in YouTube account mode.
    }

    override suspend fun createPlaylist(playlistName: String): String? {
        return repo.createPlaylist(playlistName)
    }

    override suspend fun deletePlaylist(playlistId: String): Boolean {
        return repo.deletePlaylist(playlistId)
    }
}

