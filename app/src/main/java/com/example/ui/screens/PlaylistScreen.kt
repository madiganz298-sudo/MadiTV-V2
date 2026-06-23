package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.data.model.Playlist
import com.example.ui.viewmodel.MainViewModel

@Composable
fun PlaylistScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsState()

    // Form inputs state
    var tabIndex by remember { mutableStateOf(0) } // 0: URL, 1: Copy-Paste, 2: File
    var playlistName by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var playlistContent by remember { mutableStateOf("") }
    var playlistFileUri by remember { mutableStateOf<Uri?>(null) }
    var playlistFileName by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Merge State
    val selectedPlaylistIds = remember { mutableStateListOf<Int>() }
    var showMergeDialog by remember { mutableStateOf(false) }
    var mergeName by remember { mutableStateOf("") }

    // local file selector launcher
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            playlistFileUri = uri
            playlistFileName = "M3U File Terpilih"
            Toast.makeText(context, "M3U File dipilih successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Form Title
        item {
            Text(
                text = "Kelola Playlist M3U",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sediakan playlist untuk diuraikan ke channel tontonan.",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        // Section 1: Add Playlist Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tambah Playlist Baru",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Tab switches
                    TabRow(
                        selectedTabIndex = tabIndex,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                            Text("URL Feed", Modifier.padding(8.dp), fontSize = 12.sp)
                        }
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                            Text("Copy-Paste", Modifier.padding(8.dp), fontSize = 12.sp)
                        }
                        Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }) {
                            Text("Pilih Berkas", Modifier.padding(8.dp), fontSize = 12.sp)
                        }
                    }

                    // Shared Playlist Name
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("Nama Playlist") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    when (tabIndex) {
                        0 -> {
                            OutlinedTextField(
                                value = playlistUrl,
                                onValueChange = { playlistUrl = it },
                                label = { Text("URL Link Playlist") },
                                placeholder = { Text("https://...") },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        1 -> {
                            OutlinedTextField(
                                value = playlistContent,
                                onValueChange = { playlistContent = it },
                                label = { Text("Konten Teks M3U") },
                                placeholder = { Text("#EXTM3U\n#EXTINF:-1,Channel...\nhttp://...") },
                                modifier = Modifier.fillMaxWidth().height(140.dp).padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 10
                            )
                        }
                        2 -> {
                            Button(
                                onClick = { fileLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "Pilih Berkas")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ambil File .m3u / .txt")
                                }
                            }

                            if (playlistFileUri != null) {
                                Text(
                                    text = "Ready to load: $playlistFileName",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2EA44F),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (playlistName.isEmpty()) {
                                Toast.makeText(context, "Mohon beri nama playlist", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSaving = true
                            val callback: (Boolean) -> Unit = { success ->
                                isSaving = false
                                if (success) {
                                    playlistName = ""
                                    playlistUrl = ""
                                    playlistContent = ""
                                    playlistFileUri = null
                                    Toast.makeText(context, "Playlist berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal mengimpor playlist", Toast.LENGTH_SHORT).show()
                                }
                            }

                            if (tabIndex == 0) {
                                viewModel.addPlaylistUrl(playlistName, playlistUrl, callback)
                            } else if (tabIndex == 1) {
                                viewModel.addPlaylistPaste(playlistName, playlistContent, callback)
                            } else if (tabIndex == 2 && playlistFileUri != null) {
                                try {
                                    context.contentResolver.openInputStream(playlistFileUri!!)?.use { stream ->
                                        val text = stream.bufferedReader().use { it.readText() }
                                        viewModel.addPlaylistPaste(playlistName, text, callback)
                                    } ?: run {
                                        isSaving = false
                                        Toast.makeText(context, "Gagal membaca berkas", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isSaving = false
                                    Toast.makeText(context, "Gagal membaca berkas: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSaving && playlistName.isNotEmpty() && (
                            (tabIndex == 0 && playlistUrl.isNotEmpty()) ||
                            (tabIndex == 1 && playlistContent.isNotEmpty()) ||
                            (tabIndex == 2 && playlistFileUri != null)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Impor Playlist")
                        }
                    }
                }
            }
        }

        // Section 2: Manage and Merge Playlist items
        if (playlists.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Playlist Aktif",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (selectedPlaylistIds.size >= 2) {
                        Button(
                            onClick = { showMergeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Merge ${selectedPlaylistIds.size} Lists", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(playlists) { playlist ->
                val isSelected = selectedPlaylistIds.contains(playlist.id)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedPlaylistIds.add(playlist.id)
                                    } else {
                                        selectedPlaylistIds.remove(playlist.id)
                                    }
                                }
                            )

                            Column(modifier = Modifier.padding(start = 4.dp)) {
                                Text(playlist.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    text = playlist.url ?: "Konten Tersimpan",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }

                        Row {
                            // Export share action icon
                            IconButton(onClick = {
                                viewModel.exportPlaylist(playlist.id) { m3uText ->
                                    try {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TITLE, "M4DiTV Playlist Export")
                                            putExtra(Intent.EXTRA_TEXT, m3uText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Ekspor Playlist"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Dapatkan", tint = Color(0xFFD4AF37))
                            }

                            IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFDA3637))
                            }
                        }
                    }
                }
            }
        }
    }

    // Merge Dialog Configuration
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Gabungkan Playlist") },
            text = {
                Column {
                    Text(
                        "Deduplikasi otomatis akan dijalankan untuk menyaring channel dengan url duplikat saat proses penggabungan.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = mergeName,
                        onValueChange = { mergeName = it },
                        label = { Text("Nama Hasil Penggabungan") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mergeName.isEmpty()) return@Button
                        viewModel.mergePlaylists(selectedPlaylistIds.toList(), mergeName) { success ->
                            showMergeDialog = false
                            if (success) {
                                selectedPlaylistIds.clear()
                                mergeName = ""
                                Toast.makeText(context, "Playlist Berhasil Digabung!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Gagal menggabungkan", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = mergeName.isNotEmpty()
                ) {
                    Text("Gabung")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
