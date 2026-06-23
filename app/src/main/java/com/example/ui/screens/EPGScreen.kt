package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EPGProgram
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EPGScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val epgPrograms by viewModel.allEPGPrograms.collectAsState()

    var epgUrl by remember { mutableStateOf("https://raw.githubusercontent.com/rizkyevory/epg/master/epg.xml") }
    var isDownloading by remember { mutableStateOf(false) }

    val formatTime = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val formatDate = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        item {
            Text(
                text = "Live EPG (Panduan Program)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sinkronkan EPG TV Kabel menggunakan file XMLTV favorit Anda.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Section 1: Fetch EPG feed form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Muat Sumber XMLTV EPG",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = epgUrl,
                        onValueChange = { epgUrl = it },
                        label = { Text("URL Link XMLTV") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    Button(
                        onClick = {
                            if (epgUrl.isEmpty()) return@Button
                            isDownloading = true
                            viewModel.reloadXmlEPG(epgUrl) { success ->
                                isDownloading = false
                                if (success) {
                                    Toast.makeText(context, "Selesai memperbarui EPG!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal memproses XMLTV target", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isDownloading && epgUrl.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CloudDownload, contentDescription = "Sync EPG")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unduh & Sinkronkan EPG")
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Real live grid display of listings
        item {
            Text(
                text = "Jadwal Acara TV Sedang Berlangsung",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (epgPrograms.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Panduan program kosong. input link XmlTV di atas untuk memperbarui.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            // Group programs by channelTvgId first
            val grouped = epgPrograms.groupBy { it.channelTvgId }
            items(grouped.entries.toList()) { (channelId, programs) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Monitor, contentDescription = "TV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = channelId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Horizontally scrollable timetables
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            programs.forEach { program ->
                                val now = System.currentTimeMillis()
                                val isOngoing = now in program.startTime..program.endTime

                                val itemBg = if (isOngoing) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.background
                                }

                                val itemBorder = if (isOngoing) {
                                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                }

                                Card(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .height(90.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = itemBorder,
                                    colors = CardDefaults.cardColors(containerColor = itemBg)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = program.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = if (isOngoing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "${formatTime.format(Date(program.startTime))} - ${formatTime.format(Date(program.endTime))} (${formatDate.format(Date(program.startTime))})",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )

                                        if (isOngoing) {
                                            Row(
                                                modifier = Modifier.padding(top = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(Color(0xFF2EA44F))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Sedang Berlangsung",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF2EA44F),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
