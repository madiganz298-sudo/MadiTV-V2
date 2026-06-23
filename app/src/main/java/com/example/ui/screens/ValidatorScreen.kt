package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
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
import com.example.data.model.Channel
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ValidatorScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val progress by viewModel.validationProgress.collectAsState()
    val isChecking by viewModel.isCheckingUrl.collectAsState()
    val channels by viewModel.filteredChannels.collectAsState()
    val filterStatus by viewModel.statusFilter.collectAsState()

    // Scheduler states
    var selectedInterval by remember { mutableStateOf(12) } // Hours: 6, 12, 24
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page headers
        item {
            Text(
                text = "Channel Validator & Audit",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Uji kelayakan URL stream IPTV untuk menyaring yang aktif (Online) atau mati (Offline).",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Section 1: Dashboard and Trigger Audit
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Progres Audit Manual",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (isChecking) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Menguji koneksi...", fontSize = 12.sp, color = Color.Gray)
                            Text("${(progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Tekan tombol Mulai Audit di bawah ini untuk menguji seluruh channel yang tampil pada daftar di bawah.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Button(
                            onClick = { viewModel.runUrlValidator() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Mulai")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mulai Audit Seluruh Channel")
                        }
                    }
                }
            }
        }

        // Section 2: Background Scheduler Settings & Mass Purge
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Opsi Otomasi & Pembersihan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Background Job Scheduler Hours picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Timer, contentDescription = "Schedules", tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Jadwal Audit Background", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Atur frekuensi WorkManager", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // Mini Dropdown or Choice chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(6, 12, 24).forEach { hr ->
                                FilterChip(
                                    selected = selectedInterval == hr,
                                    onClick = {
                                        selectedInterval = hr
                                        viewModel.configurePeriodicValidation(hr)
                                        Toast.makeText(context, "Audit otomatis dijadwalkan setiap $hr Jam!", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("${hr}J", fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delete All Offline mass button
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Hapus Massal")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hapus Semua Channel OFFLINE")
                    }
                }
            }
        }

        // Section 3: List Filter tabs (All / Online / Offline)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftar Saluran TV (${channels.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("ALL" to "Semua", "ONLINE" to "Online", "OFFLINE" to "Mati").forEach { (id, label) ->
                        FilterChip(
                            selected = filterStatus == id,
                            onClick = { viewModel.setStatusFilter(id) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Channels audit status list
        if (channels.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Daftar channel kosong atau tidak ada status yang cocok.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(channels) { channel ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Status indicator dot
                            val colorValue = when (channel.status) {
                                "ONLINE" -> Color(0xFF2EA44F)
                                "OFFLINE" -> Color(0xFFDA3637)
                                else -> Color.Gray
                            }

                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(colorValue)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(channel.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(channel.url, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            }
                        }

                        // Display validation Tag
                        Text(
                            text = channel.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (channel.status) {
                                "ONLINE" -> Color(0xFF2EA44F)
                                "OFFLINE" -> Color(0xFFDA3637)
                                else -> Color.Gray
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Confirmation Box
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Konfirmasi Hapus Massal") },
            text = { Text("Apakah Anda yakin ingin menghapus masal semua channel yang berstatus offline dari profile Anda?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearOfflineChannels()
                        showDeleteConfirm = false
                        Toast.makeText(context, "Seluruh channel offline berhasil dihapus!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ya, Hapus Semua")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
