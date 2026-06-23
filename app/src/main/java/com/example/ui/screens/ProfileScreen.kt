package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Profile
import com.example.ui.viewmodel.MainViewModel

@Composable
fun ProfileScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val profiles by viewModel.profilesState.collectAsState()
    val activeProfile by viewModel.currentProfile.collectAsState()

    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var isNewProfileKid by remember { mutableStateOf(false) }
    var newProfilePin by remember { mutableStateOf("") }

    // PIN Unlock Dialog State
    var showPinUnlockDialog by remember { mutableStateOf<Profile?>(null) }
    var pinInputText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profil Keluarga",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pisahkan koleksi channel, history dan favorit anggota keluarga Anda.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Button(
                    onClick = { showAddProfileDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Profile")
                }
            }
        }

        // Active notification
        activeProfile?.let { prof ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Verified", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Profil Aktif: ${prof.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (prof.isKid) "Saringan Konten Anak Aktif" else "Akses Utama Tanpa Batas",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Pilih Profil Pengguna", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }

        // List Profiles
        items(profiles) { profile ->
            val isActive = activeProfile?.id == profile.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!profile.pin.isNullOrEmpty()) {
                            // Needs Pin Unlock
                            showPinUnlockDialog = profile
                            pinInputText = ""
                        } else {
                            viewModel.selectProfile(profile)
                            Toast
                                .makeText(context, "Profil beralih ke: ${profile.name}", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Profile Avatar Layout
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (profile.isKid) Color(0xFFD4AF37).copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (profile.isKid) Icons.Default.ChildCare else Icons.Default.Person,
                                contentDescription = "Avatar",
                                tint = if (profile.isKid) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(profile.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!profile.pin.isNullOrEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PIN Terkunci", fontSize = 11.sp, color = Color.Gray)
                                } else {
                                    Text("Terbuka Publik", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    // Delete option (for non-default profiles)
                    if (profile.id != 1 && profile.id != 2) {
                        IconButton(onClick = { viewModel.deleteProfile(profile) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.background(Color.Transparent)) // simplistic delete representation
                        }
                    }
                }
            }
        }
    }

    // PIN Unlock Dialog
    showPinUnlockDialog?.let { profile ->
        AlertDialog(
            onDismissRequest = { showPinUnlockDialog = null },
            title = { Text("Masukkan PIN Profil") },
            text = {
                Column {
                    Text(
                        text = "Profil ${profile.name} diproteksi PIN pengaman.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = pinInputText,
                        onValueChange = { if (it.length <= 4) pinInputText = it },
                        label = { Text("PIN 4-Angka") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInputText == profile.pin) {
                            viewModel.selectProfile(profile)
                            showPinUnlockDialog = null
                            Toast.makeText(context, "Profil beralih ke: ${profile.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "PIN yang dimasukkan SALAH!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Buka")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinUnlockDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Add Profile Dialog
    if (showAddProfileDialog) {
        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = { Text("Tambah Profil Baru") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Nama Profil") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = isNewProfileKid, onCheckedChange = { isNewProfileKid = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Profil Anak (Kids)?", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Menyembunyikan kategori/konten dewasa", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    OutlinedTextField(
                        value = newProfilePin,
                        onValueChange = { if (it.length <= 4) newProfilePin = it },
                        label = { Text("PIN Kunci (Opsional, 4-Angka)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isEmpty()) return@Button
                        viewModel.createProfile(
                            name = newProfileName,
                            pin = if (newProfilePin.isEmpty()) null else newProfilePin,
                            isKid = isNewProfileKid
                        )
                        newProfileName = ""
                        newProfilePin = ""
                        isNewProfileKid = false
                        showAddProfileDialog = false
                        Toast.makeText(context, "Profil baru berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = newProfileName.isNotEmpty()
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
