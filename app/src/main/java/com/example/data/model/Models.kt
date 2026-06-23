package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val pin: String? = null,
    val isKid: Boolean = false
) : Serializable

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String? = null,
    val rawM3uText: String? = null,
    val profileId: Int = 1
) : Serializable

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playlistId: Int,
    val title: String,
    val url: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isFavorite: Boolean = false,
    val isBookmarked: Boolean = false,
    val status: String = "UNKNOWN", // ONLINE, OFFLINE, UNKNOWN
    val lastValidated: Long = 0L,
    val profileId: Int = 1
) : Serializable

@Entity(tableName = "epg_programs")
data class EPGProgram(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelTvgId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long, // Epoch timestamp in ms
    val endTime: Long // Epoch timestamp in ms
) : Serializable

@Entity(tableName = "history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val channelId: Int,
    val channelTitle: String,
    val url: String,
    val logoUrl: String? = null,
    val watchedAt: Long = System.currentTimeMillis(),
    val profileId: Int = 1
) : Serializable
