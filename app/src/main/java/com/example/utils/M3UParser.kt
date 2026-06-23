package com.example.utils

import android.util.Log
import com.example.data.model.Channel
import java.io.BufferedReader
import java.io.StringReader

object M3UParser {
    private const val TAG = "M3UParser"

    fun parse(m3uContent: String, playlistId: Int, profileId: Int): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(m3uContent))
        var line: String?
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogoUrl: String? = null
        var currentCategory: String? = null
        var currentTitle: String? = null
        var hasExtInf = false

        val tvgIdRegex = """tvg-id=["']?([^"']*)["']?""".toRegex(RegexOption.IGNORE_CASE)
        val tvgNameRegex = """tvg-name=["']?([^"']*)["']?""".toRegex(RegexOption.IGNORE_CASE)
        val tvgLogoRegex = """tvg-logo=["']?([^"']*)["']?""".toRegex(RegexOption.IGNORE_CASE)
        val groupTitleRegex = """group-title=["']?([^"']*)["']?""".toRegex(RegexOption.IGNORE_CASE)

        try {
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isEmpty()) continue

                if (trimmed.startsWith("#EXTINF:")) {
                    hasExtInf = true
                    // Parse metadata
                    currentTvgId = tvgIdRegex.find(trimmed)?.groupValues?.get(1)
                    currentTvgName = tvgNameRegex.find(trimmed)?.groupValues?.get(1)
                    currentLogoUrl = tvgLogoRegex.find(trimmed)?.groupValues?.get(1)
                    currentCategory = groupTitleRegex.find(trimmed)?.groupValues?.get(1)

                    // Find title - everything after the last comma
                    val commaIndex = trimmed.lastIndexOf(',')
                    currentTitle = if (commaIndex != -1 && commaIndex < trimmed.length - 1) {
                        trimmed.substring(commaIndex + 1).trim()
                    } else {
                        "Unknown Channel"
                    }
                } else if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                    // It's a stream URL
                    if (hasExtInf) {
                        val title = currentTitle ?: "Channel ${channels.size + 1}"
                        channels.add(
                            Channel(
                                playlistId = playlistId,
                                title = title,
                                url = trimmed,
                                logoUrl = currentLogoUrl,
                                category = currentCategory ?: "Lainnya",
                                tvgId = currentTvgId ?: currentTvgName,
                                tvgName = currentTvgName,
                                profileId = profileId
                            )
                        )
                        // Reset line details
                        currentTvgId = null
                        currentTvgName = null
                        currentLogoUrl = null
                        currentCategory = null
                        currentTitle = null
                        hasExtInf = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing M3U list", e)
        } finally {
            reader.close()
        }

        return channels
    }
}
