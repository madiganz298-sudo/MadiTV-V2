package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val bufferSizeMs by viewModel.bufferSizeMs.collectAsState()

    var showClearHistoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        Text(
            text = "Pengaturan Umum",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Kustomisasi fungsional visual dan performa streaming feed M4DiTV Anda.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        // Theme selection card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Palette, contentDescription = "Tampilan", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Skema Tema", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(themeMode, fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("SYSTEM" to "Auto", "DARK" to "Gelap", "LIGHT" to "Terang").forEach { (id, label) ->
                        FilterChip(
                            selected = themeMode == id,
                            onClick = { viewModel.setThemeMode(id) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Language settings card
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Bahasa", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Bahasa Utama", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(if (language == "id") "Bahasa Indonesia" else "English (US)", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("id" to "Indonesia", "en" to "English").forEach { (id, label) ->
                        FilterChip(
                            selected = language == id,
                            onClick = { viewModel.setLanguage(id) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Buffer sizing configurations card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Speed, contentDescription = "Buffer", tint = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Durasi Buffer Pemutar (ExoPlayer)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Atur ukuran cache ideal pencegah lag", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(15000 to "Sangat Ringkas", 30000 to "Standar", 50000 to "Lebih Lancar (Bagus)").forEach { (ms, label) ->
                        FilterChip(
                            selected = bufferSizeMs == ms,
                            onClick = { viewModel.setBufferSize(ms) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Cache clear storage controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showClearHistoryDialog = true },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Pembersih", tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Bersihkan Cache Tontonan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Text("Hapus mutlak log riwayat & history cache", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Watermarks copyright footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Info, contentDescription = "Info", tint = Color.Gray, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "M4DiTV VIP Player v1.0.0 • Developer M4DI~UciH4",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }

    // Deletion Modal Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Konfirmasi Bersihkan Cache") },
            text = { Text("Seluruh riwayat streams tontonan (Continue watching) pada profil aktif Anda akan dihapus permanen. Lanjutkan?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Cache & Riwayat Berhasil Dibersihkan!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Hapus Riwayat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
