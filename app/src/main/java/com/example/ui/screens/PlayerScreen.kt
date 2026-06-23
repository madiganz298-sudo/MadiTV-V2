package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    channel: Channel,
    onBack: () -> Unit,
    onNextChannel: (() -> Unit)? = null,
    onPrevChannel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // ExoPlayer State
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Configure source url
    LaunchedEffect(channel.url) {
        val mediaItem = MediaItem.fromUri(Uri.parse(channel.url))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Gestures Control State
    var volume by remember { mutableStateOf(50) }
    var brightness by remember { mutableStateOf(50) }
    var showGestureFeedback by remember { mutableStateOf<String?>(null) }
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    // Sleep Timer State
    var sleepTimerLeft by remember { mutableStateOf(0L) } // remaining seconds
    var timerActive by remember { mutableStateOf(false) }
    var showSleepTimerMenu by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    // Auto Hide controls timer
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    // Sleep Timer ticking
    LaunchedEffect(sleepTimerLeft, timerActive) {
        if (timerActive && sleepTimerLeft > 0) {
            delay(1000)
            sleepTimerLeft--
            if (sleepTimerLeft <= 0L) {
                exoPlayer.pause()
                timerActive = false
                activity?.finish()
            }
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(isLocked) {
                if (isLocked) return@pointerInput
                detectDragGestures(
                    onDragStart = { /* Reset controls show */ },
                    onDragEnd = { showGestureFeedback = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        showControls = true
                        val height = size.height
                        val width = size.width
                        val x = change.position.x

                        if (x < width / 2) {
                            // Brightness drag on left screen half
                            val delta = -(dragAmount.y / height) * 100
                            brightness = (brightness + delta.toInt()).coerceIn(0, 100)
                            // Change android window brightness
                            activity?.runOnUiThread {
                                val window = activity.window
                                val params = window.attributes
                                params.screenBrightness = brightness / 100f
                                window.attributes = params
                            }
                            showGestureFeedback = "Brightness: $brightness%"
                        } else {
                            // Volume drag on right screen half
                            val delta = -(dragAmount.y / height) * maxVolume
                            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            val targetVol = (currentVol + delta).coerceIn(0f, maxVolume.toFloat()).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            volume = (targetVol / maxVolume.toFloat() * 100).toInt()
                            showGestureFeedback = "Volume: $volume%"
                        }
                    }
                )
            }
            .clickable {
                showControls = !showControls
            }
    ) {

        // 1. Android PlayerView element
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Lock / Unlock Overlay Actions
        if (isLocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                IconButton(
                    onClick = { isLocked = false },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(Color.DarkGray.copy(alpha = 0.6f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Buka Kunci",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // 3. Control overlays
        AnimatedVisibility(
            visible = showControls && !isLocked,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {

                // Top controls bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = channel.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.category ?: "Lainnya",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    // Timer displays
                    if (sleepTimerLeft > 0) {
                        val minutes = sleepTimerLeft / 60
                        val seconds = sleepTimerLeft % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Color(0xFFD4AF37),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    // Actions
                    IconButton(onClick = { showSleepTimerMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { isLocked = true }) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Kunci Layar",
                            tint = Color.White
                        )
                    }
                }

                // Center main controller buttons
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onPrevChannel?.invoke() },
                        enabled = onPrevChannel != null,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Channel Sebelumnya",
                            tint = if (onPrevChannel != null) Color.White else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    IconButton(
                        onClick = { onNextChannel?.invoke() },
                        enabled = onNextChannel != null,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Channel Selanjutnya",
                            tint = if (onNextChannel != null) Color.White else Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Volume / Mute trigger bottom line
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        isMuted = !isMuted
                        exoPlayer.volume = if (isMuted) 0f else 1f
                    }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "M4DiTV Premium Live Stream Player",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Gestures visual toast/feedback
        showGestureFeedback?.let { feedback ->
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = feedback,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // 5. Sleep timer configuration sheet / dialog
        if (showSleepTimerMenu) {
            AlertDialog(
                onDismissRequest = { showSleepTimerMenu = false },
                title = { Text("Sleep Timer") },
                text = {
                    Column {
                        val intervals = listOf(
                            "Off" to 0L,
                            "5 Menit" to 5L * 60,
                            "15 Menit" to 15L * 60,
                            "30 Menit" to 30L * 60,
                            "60 Menit" to 60L * 60
                        )
                        intervals.forEach { (label, duration) ->
                            TextButton(
                                onClick = {
                                    if (duration == 0L) {
                                        sleepTimerLeft = 0L
                                        timerActive = false
                                    } else {
                                        sleepTimerLeft = duration
                                        timerActive = true
                                    }
                                    showSleepTimerMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    color = if ((duration == 0L && !timerActive) || (duration == sleepTimerLeft)) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerMenu = false }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}
