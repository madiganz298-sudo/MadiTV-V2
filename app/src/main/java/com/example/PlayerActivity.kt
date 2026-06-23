package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Channel
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val channel = intent.getSerializableExtra("channel_key") as? Channel
        if (channel == null) {
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val channels by viewModel.filteredChannels.collectAsState()
                
                var currentChannel by remember { mutableStateOf(channel) }
                
                // Track next / prev enabled state by finding index in current list
                val currentIndex = channels.indexOfFirst { it.id == currentChannel.id }
                
                val onNext = if (currentIndex >= 0 && currentIndex < channels.size - 1) {
                    { currentChannel = channels[currentIndex + 1] }
                } else null
                
                val onPrev = if (currentIndex > 0) {
                    { currentChannel = channels[currentIndex - 1] }
                } else null

                PlayerScreen(
                    channel = currentChannel,
                    onBack = { finish() },
                    onNextChannel = onNext,
                    onPrevChannel = onPrev
                )
            }
        }
    }
}
