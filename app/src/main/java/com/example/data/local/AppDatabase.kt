package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Delete
    suspend fun deleteProfile(profile: Profile)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE profileId = :profileId")
    fun getPlaylistsByProfileFlow(profileId: Int): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE profileId = :profileId")
    suspend fun getPlaylistsByProfile(profileId: Int): List<Playlist>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Int)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE profileId = :profileId")
    fun getChannelsFlow(profileId: Int): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE profileId = :profileId")
    suspend fun getChannels(profileId: Int): List<Channel>

    @Query("SELECT DISTINCT category FROM channels WHERE profileId = :profileId AND category IS NOT NULL AND category != ''")
    fun getCategoriesFlow(profileId: Int): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE profileId = :profileId AND isFavorite = 1")
    fun getFavoritesFlow(profileId: Int): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE profileId = :profileId AND isBookmarked = 1")
    fun getBookmarkedFlow(profileId: Int): Flow<List<Channel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel): Long

    @Update
    suspend fun updateChannel(channel: Channel)

    @Query("UPDATE channels SET isFavorite = :isFav WHERE id = :channelId")
    suspend fun updateFavorite(channelId: Int, isFav: Boolean)

    @Query("UPDATE channels SET isBookmarked = :isBookmarked WHERE id = :channelId")
    suspend fun updateBookmark(channelId: Int, isBookmarked: Boolean)

    @Query("UPDATE channels SET status = :status, lastValidated = :timestamp WHERE id = :channelId")
    suspend fun updateStatus(channelId: Int, status: String, timestamp: Long)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylist(playlistId: Int)

    @Query("DELETE FROM channels WHERE profileId = :profileId AND status = 'OFFLINE'")
    suspend fun deleteOfflineChannels(profileId: Int)
}

@Dao
interface EpgDao {
    @Query("SELECT * FROM epg_programs WHERE channelTvgId = :tvgId ORDER BY startTime ASC")
    fun getProgramsForChannelFlow(tvgId: String): Flow<List<EPGProgram>>

    @Query("SELECT * FROM epg_programs WHERE startTime >= :fromTime AND endTime <= :toTime ORDER BY startTime ASC")
    suspend fun getProgramsInRange(fromTime: Long, toTime: Long): List<EPGProgram>

    @Query("SELECT * FROM epg_programs ORDER BY startTime ASC")
    fun getAllProgramsFlow(): Flow<List<EPGProgram>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EPGProgram>)

    @Query("DELETE FROM epg_programs")
    suspend fun clearAllEPG()
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE profileId = :profileId ORDER BY watchedAt DESC LIMIT 50")
    fun getHistoryFlow(profileId: Int): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: Int)
}

@Database(
    entities = [Profile::class, Playlist::class, Channel::class, EPGProgram::class, HistoryItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun historyDao(): HistoryDao
}
