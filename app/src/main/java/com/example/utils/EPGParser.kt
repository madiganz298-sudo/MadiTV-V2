package com.example.utils

import android.util.Log
import android.util.Xml
import com.example.data.model.EPGProgram
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale

object EPGParser {
    private const val TAG = "EPGParser"

    private val dateFormats = arrayOf(
        SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US),
        SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    )

    private fun parseDate(dateStr: String): Long {
        for (format in dateFormats) {
            try {
                return format.parse(dateStr)?.time ?: 0L
            } catch (e: Exception) {
                // Try next
            }
        }
        return 0L
    }

    fun parse(xmlContent: String): List<EPGProgram> {
        val programs = mutableListOf<EPGProgram>()
        val parser = Xml.newPullParser()
        try {
            parser.setInput(StringReader(xmlContent))
            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStart: String? = null
            var currentStop: String? = null
            var currentTitle: String? = null
            var currentDesc: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                var name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "programme") {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            currentStart = parser.getAttributeValue(null, "start")
                            currentStop = parser.getAttributeValue(null, "stop")
                        } else if (name == "title") {
                            currentTitle = parser.nextText()
                        } else if (name == "desc") {
                            currentDesc = parser.nextText()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "programme") {
                            if (currentChannelId != null && currentStart != null && currentStop != null && currentTitle != null) {
                                val startTime = parseDate(currentStart)
                                val endTime = parseDate(currentStop)
                                if (startTime > 0L && endTime > 0L) {
                                    programs.add(
                                        EPGProgram(
                                            channelTvgId = currentChannelId,
                                            title = currentTitle,
                                            description = currentDesc,
                                            startTime = startTime,
                                            endTime = endTime
                                        )
                                    )
                                }
                            }
                            currentChannelId = null
                            currentStart = null
                            currentStop = null
                            currentTitle = null
                            currentDesc = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XMLTV EPG data", e)
        }
        return programs
    }
}
