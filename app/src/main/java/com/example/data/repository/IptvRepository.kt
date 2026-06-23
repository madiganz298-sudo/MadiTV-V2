package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.utils.M3UParser
import com.example.utils.EPGParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class IptvRepository(
    private val db: AppDatabase,
    private val okHttpClient: OkHttpClient
) {
    private val profileDao = db.profileDao()
    private val playlistDao = db.playlistDao()
    private val channelDao = db.channelDao()
    private val epgDao = db.epgDao()
    private val historyDao = db.historyDao()

    // Profile Methods
    val allProfilesFlow: Flow<List<Profile>> = profileDao.getAllProfilesFlow()
    suspend fun getProfiles(): List<Profile> = profileDao.getAllProfiles()
    suspend fun createProfile(profile: Profile): Long = profileDao.insertProfile(profile)
    suspend fun deleteProfile(profile: Profile) = profileDao.deleteProfile(profile)
    suspend fun getProfileById(id: Int): Profile? = profileDao.getProfileById(id)

    // Ensure default profiles exist
    suspend fun ensureDefaultProfilesExist() {
        val current = profileDao.getAllProfiles()
        if (current.isEmpty()) {
            profileDao.insertProfile(Profile(id = 1, name = "Utama (Default)", pin = null, isKid = false))
            profileDao.insertProfile(Profile(id = 2, name = "Anak-Anak", pin = "1234", isKid = true))
        }
    }

    // Playlist Methods
    fun getPlaylistsFlow(profileId: Int): Flow<List<Playlist>> = playlistDao.getPlaylistsByProfileFlow(profileId)
    suspend fun getPlaylists(profileId: Int): List<Playlist> = playlistDao.getPlaylistsByProfile(profileId)
    suspend fun deletePlaylist(playlistId: Int) {
        playlistDao.deletePlaylistById(playlistId)
        channelDao.deleteChannelsByPlaylist(playlistId)
    }

    // Add Playlist from URL
    suspend fun addPlaylistFromUrl(name: String, url: String, profileId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val bodyText = response.body?.string() ?: return@withContext false

                // Save playlist
                val playlist = Playlist(name = name, url = url, rawM3uText = null, profileId = profileId)
                val playlistId = playlistDao.insertPlaylist(playlist).toInt()

                // Parse and insert channels
                val channels = M3UParser.parse(bodyText, playlistId, profileId)
                channelDao.insertChannels(channels)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("IptvRepository", "Error adding playlist from URL", e)
            return@withContext false
        }
    }

    // Add Playlist from raw text (Copy-Paste)
    suspend fun addPlaylistFromRawText(name: String, text: String, profileId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val playlist = Playlist(name = name, url = null, rawM3uText = text, profileId = profileId)
            val playlistId = playlistDao.insertPlaylist(playlist).toInt()

            val channels = M3UParser.parse(text, playlistId, profileId)
            channelDao.insertChannels(channels)
            return@withContext true
        } catch (e: Exception) {
            Log.e("IptvRepository", "Error adding raw playlist", e)
            return@withContext false
        }
    }

    // Merge multiple playlists, deduplicate automatically by comparing URL
    suspend fun mergePlaylists(playlistIds: List<Int>, profileId: Int, mergedName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val mergedPlaylist = Playlist(name = mergedName, url = null, rawM3uText = "Merged", profileId = profileId)
            val mergedPlaylistId = playlistDao.insertPlaylist(mergedPlaylist).toInt()

            val currentChannels = channelDao.getChannels(profileId)
            val selectedChannels = currentChannels.filter { it.playlistId in playlistIds }

            // Deduplicate by URL
            val uniqueChannels = selectedChannels.distinctBy { it.url }.map {
                it.copy(id = 0, playlistId = mergedPlaylistId)
            }

            channelDao.insertChannels(uniqueChannels)
            return@withContext true
        } catch (e: Exception) {
            Log.e("IptvRepository", "Error merging playlists", e)
            return@withContext false
        }
    }

    // Export a merged playlist back to .m3u string representation
    suspend fun exportPlaylistToM3u(playlistId: Int, profileId: Int): String = withContext(Dispatchers.IO) {
        try {
            val currentChannels = channelDao.getChannels(profileId).filter { it.playlistId == playlistId }
            val builder = StringBuilder()
            builder.append("#EXTM3U\n")
            for (channel in currentChannels) {
                var line = "#EXTINF:-1"
                if (!channel.tvgId.isNullOrEmpty()) line += " tvg-id=\"${channel.tvgId}\""
                if (!channel.tvgName.isNullOrEmpty()) line += " tvg-name=\"${channel.tvgName}\""
                if (!channel.logoUrl.isNullOrEmpty()) line += " tvg-logo=\"${channel.logoUrl}\""
                if (!channel.category.isNullOrEmpty()) line += " group-title=\"${channel.category}\""
                line += ",${channel.title}\n"
                builder.append(line)
                builder.append("${channel.url}\n")
            }
            return@withContext builder.toString()
        } catch (e: Exception) {
            Log.e("IptvRepository", "Error exporting M3U", e)
            return@withContext "#EXTM3U\n"
        }
    }

    // Channel Methods
    fun getChannelsFlow(profileId: Int): Flow<List<Channel>> = channelDao.getChannelsFlow(profileId)
    suspend fun getChannels(profileId: Int): List<Channel> = channelDao.getChannels(profileId)
    fun getCategoriesFlow(profileId: Int): Flow<List<String>> = channelDao.getCategoriesFlow(profileId)
    fun getFavoritesFlow(profileId: Int): Flow<List<Channel>> = channelDao.getFavoritesFlow(profileId)
    fun getBookmarkedFlow(profileId: Int): Flow<List<Channel>> = channelDao.getBookmarkedFlow(profileId)

    suspend fun toggleFavorite(channelId: Int, isFav: Boolean) {
        channelDao.updateFavorite(channelId, isFav)
    }

    suspend fun toggleBookmark(channelId: Int, isBookmarked: Boolean) {
        channelDao.updateBookmark(channelId, isBookmarked)
    }

    suspend fun deleteOfflineChannels(profileId: Int) {
        channelDao.deleteOfflineChannels(profileId)
    }

    // Single Channel URL Validator (runs lightweight head or connection check)
    suspend fun validateChannelUrl(channel: Channel): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(channel.url)
                .head()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val status = if (response.isSuccessful || response.code in 200..399) "ONLINE" else "OFFLINE"
                channelDao.updateStatus(channel.id, status, System.currentTimeMillis())
                return@withContext status
            }
        } catch (e: Exception) {
            channelDao.updateStatus(channel.id, "OFFLINE", System.currentTimeMillis())
            return@withContext "OFFLINE"
        }
    }

    // History Methods
    fun getHistoryFlow(profileId: Int): Flow<List<HistoryItem>> = historyDao.getHistoryFlow(profileId)
    suspend fun addHistoryItem(item: HistoryItem) = historyDao.insertHistoryItem(item)
    suspend fun clearHistory(profileId: Int) = historyDao.clearHistory(profileId)

    // EPG Methods
    fun getProgramsForChannel(tvgId: String): Flow<List<EPGProgram>> = epgDao.getProgramsForChannelFlow(tvgId)
    fun getAllProgramsFlow(): Flow<List<EPGProgram>> = epgDao.getAllProgramsFlow()
    suspend fun clearAllEPG() = epgDao.clearAllEPG()

    suspend fun fetchAndParseEPG(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false
                val epgText = response.body?.string() ?: return@withContext false
                val programs = EPGParser.parse(epgText)
                if (programs.isNotEmpty()) {
                    epgDao.clearAllEPG()
                    epgDao.insertPrograms(programs)
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("IptvRepository", "Error fetching/parsing EPG", e)
            return@withContext false
        }
    }
}
