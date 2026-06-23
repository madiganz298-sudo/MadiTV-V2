package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                var currentTab by remember { mutableStateOf("home") }
                val context = LocalContext.current

                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (!isLandscape) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentTab == "home",
                                    onClick = { currentTab = "home" },
                                    label = { Text("TV") },
                                    icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "Home") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "playlists",
                                    onClick = { currentTab = "playlists" },
                                    label = { Text("Playlist") },
                                    icon = { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Playlists") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "validator",
                                    onClick = { currentTab = "validator" },
                                    label = { Text("Validator") },
                                    icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Validator") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "epg",
                                    onClick = { currentTab = "epg" },
                                    label = { Text("EPG") },
                                    icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "EPG") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "profiles",
                                    onClick = { currentTab = "profiles" },
                                    label = { Text("Profil") },
                                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Profile") }
                                )
                                NavigationBarItem(
                                    selected = currentTab == "settings",
                                    onClick = { currentTab = "settings" },
                                    label = { Text("Setting") },
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isLandscape) {
                            NavigationRail {
                                NavigationRailItem(
                                    selected = currentTab == "home",
                                    onClick = { currentTab = "home" },
                                    label = { Text("TV") },
                                    icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "Home") }
                                )
                                NavigationRailItem(
                                    selected = currentTab == "playlists",
                                    onClick = { currentTab = "playlists" },
                                    label = { Text("Playlist") },
                                    icon = { Icon(imageVector = Icons.Default.LibraryMusic, contentDescription = "Playlists") }
                                )
                                NavigationRailItem(
                                    selected = currentTab == "validator",
                                    onClick = { currentTab = "validator" },
                                    label = { Text("Validator") },
                                    icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Validator") }
                                )
                                NavigationRailItem(
                                    selected = currentTab == "epg",
                                    onClick = { currentTab = "epg" },
                                    label = { Text("EPG") },
                                    icon = { Icon(imageVector = Icons.Default.CalendarToday, contentDescription = "EPG") }
                                )
                                NavigationRailItem(
                                    selected = currentTab == "profiles",
                                    onClick = { currentTab = "profiles" },
                                    label = { Text("Profil") },
                                    icon = { Icon(imageVector = Icons.Default.People, contentDescription = "Profile") }
                                )
                                NavigationRailItem(
                                    selected = currentTab == "settings",
                                    onClick = { currentTab = "settings" },
                                    label = { Text("Setting") },
                                    icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") }
                                )
                            }
                        }

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            when (currentTab) {
                                "home" -> HomeScreen(
                                    viewModel = viewModel,
                                    onPlayChannel = { selectedChannel, _ ->
                                        val intent = Intent(context, PlayerActivity::class.java).apply {
                                            putExtra("channel_key", selectedChannel)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                                "playlists" -> PlaylistScreen(viewModel = viewModel)
                                "validator" -> ValidatorScreen(viewModel = viewModel)
                                "epg" -> EPGScreen(viewModel = viewModel)
                                "profiles" -> ProfileScreen(viewModel = viewModel)
                                "settings" -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
