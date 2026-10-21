package com.example.ui.screens

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MediaStudioRepository
import com.example.model.*
import com.example.ui.components.AmendTrendLogo
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun VideoStudioScreen(
    mediaStudioRepository: MediaStudioRepository,
    initialInstrument: Instrument? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedInstrument by remember {
        mutableStateOf(initialInstrument ?: InstrumentsCatalog.ALL_INSTRUMENTS.first())
    }
    var selectedTopic by remember { mutableStateOf("SMC_LIQUIDITY") }
    var selectedRatio by remember { mutableStateOf("16:9") } // "16:9" or "9:16"
    var speechRate by remember { mutableStateOf(1.0f) }

    var currentProject by remember {
        mutableStateOf(
            mediaStudioRepository.generateVideoProject(selectedInstrument, selectedTopic, selectedRatio)
        )
    }

    var currentSceneIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val savedVideos by mediaStudioRepository.savedVideosFlow.collectAsState(initial = emptyList())

    // Native TextToSpeech Engine
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialize TTS locale
            }
        }
        tts.language = Locale.US
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    // Playback loop through scenes
    LaunchedEffect(isPlaying, currentSceneIndex) {
        if (isPlaying) {
            val scene = currentProject.scenes.getOrNull(currentSceneIndex)
            if (scene != null) {
                // Speak narration via Android TextToSpeech
                ttsEngine?.setSpeechRate(speechRate)
                ttsEngine?.speak(scene.narrationText, TextToSpeech.QUEUE_FLUSH, null, "scene_$currentSceneIndex")

                val waitTimeMs = (scene.durationSeconds * 1000L / speechRate).toLong()
                delay(waitTimeMs)

                if (currentSceneIndex + 1 < currentProject.scenes.size) {
                    currentSceneIndex += 1
                } else {
                    isPlaying = false
                    currentSceneIndex = 0
                }
            } else {
                isPlaying = false
            }
        } else {
            ttsEngine?.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                }
                Column {
                    Text("AI VIDEO & AUDIO STUDIO", color = PrimaryText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Narrated Educational Lessons • YouTube & Shorts", color = MintGreen, fontSize = 11.sp)
                }
            }

            IconButton(onClick = {
                currentProject = mediaStudioRepository.generateVideoProject(selectedInstrument, selectedTopic, selectedRatio)
                currentSceneIndex = 0
                isPlaying = false
                statusMessage = "Generated fresh script storyboard!"
            }) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Regenerate", tint = MintGreen)
            }
        }

        // Instrument Carousel
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(InstrumentsCatalog.ALL_INSTRUMENTS) { inst ->
                val isSel = inst.id == selectedInstrument.id
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) MintGreen else CardBackground,
                    modifier = Modifier.clickable {
                        selectedInstrument = inst
                        currentProject = mediaStudioRepository.generateVideoProject(inst, selectedTopic, selectedRatio)
                        currentSceneIndex = 0
                    }
                ) {
                    Text(
                        text = inst.symbol,
                        color = if (isSel) DeepBackground else PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Topic & Aspect Ratio Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Aspect Ratio (16:9 or 9:16)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBackground)
                    .padding(2.dp)
            ) {
                listOf("16:9" to "Landscape", "9:16" to "Reels").forEach { (ratio, label) ->
                    val isSel = selectedRatio == ratio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) MintGreen else Color.Transparent)
                            .clickable {
                                selectedRatio = ratio
                                currentProject = currentProject.copy(aspectRatio = ratio)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$ratio $label",
                            color = if (isSel) DeepBackground else SecondaryText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Topic Selector
            listOf(
                "SMC_LIQUIDITY" to "Liquidity",
                "BOS_CHOCH" to "BOS/CHoCH",
                "ORDER_BLOCKS" to "Order Block"
            ).forEach { (key, label) ->
                val isSel = selectedTopic == key
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) MintGreen else CardBackground,
                    modifier = Modifier.clickable {
                        selectedTopic = key
                        currentProject = mediaStudioRepository.generateVideoProject(selectedInstrument, key, selectedRatio)
                    }
                ) {
                    Text(
                        text = label,
                        color = if (isSel) DeepBackground else PrimaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Animated Video Canvas Player
        val currentScene = currentProject.scenes[currentSceneIndex.coerceIn(0, currentProject.scenes.size - 1)]
        val isPortrait = selectedRatio == "9:16"

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isPortrait) 320.dp else 220.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepBackground)
                    .padding(12.dp)
            ) {
                // Scene Visual Content
                when (currentScene.visualType) {
                    "INTRO_LOGO" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AmendTrendLogo(size = 56.dp, horizontal = false)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "PRESENTS: ${selectedInstrument.symbol.uppercase()}",
                                color = MintGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                    "OUTRO" -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AmendTrendLogo(size = 48.dp, horizontal = false)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("AMEND TREND • STYLE SHIFT", color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Practice with virtual money. Never trade unprepared.", color = SecondaryText, fontSize = 10.sp)
                        }
                    }
                    else -> {
                        // Simulated Animated Candlestick Canvas
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Scene ${currentScene.sceneIndex + 1}: ${currentScene.sceneTitle}",
                                    color = MintGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentScene.chartHighlight,
                                    color = WarningGold,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            // Chart preview bars
                            Box(modifier = Modifier.weight(1f)) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val barWidth = size.width / 15f
                                    for (i in 0 until 14) {
                                        val x = i * barWidth + barWidth * 0.2f
                                        val h = size.height * (0.3f + (i % 5) * 0.12f)
                                        val isUp = (i % 2 == 0)
                                        drawRect(
                                            color = if (isUp) BullishGreen else BearishRed,
                                            topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h),
                                            size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, h * 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Subtitles Overlay (Synchronized Narration Captions)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = currentScene.narrationText,
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentSceneIndex > 0) currentSceneIndex -= 1
                }
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev Scene", tint = PrimaryText)
            }

            Button(
                onClick = { isPlaying = !isPlaying },
                colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = DeepBackground)
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPlaying) "PAUSE NARRATION" else "PLAY NARRATED VIDEO", color = DeepBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            IconButton(
                onClick = {
                    if (currentSceneIndex + 1 < currentProject.scenes.size) currentSceneIndex += 1
                }
            ) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Scene", tint = PrimaryText)
            }
        }

        // Narration Speed Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Narration Voice Rate:", color = SecondaryText, fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.75f to "0.75x", 1.0f to "1.0x", 1.25f to "1.25x").forEach { (rate, label) ->
                    val isSel = speechRate == rate
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSel) MintGreen else CardBackground,
                        modifier = Modifier.clickable { speechRate = rate }
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) DeepBackground else PrimaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Actions: Save to Library & Export Transcript
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        mediaStudioRepository.saveVideo(currentProject)
                        statusMessage = "Saved '${currentProject.title}' to your Media Library!"
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save to Library", color = MintGreen, fontSize = 12.sp)
            }

            Button(
                onClick = { showExportDialog = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlueGray)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryText, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View Transcript", color = PrimaryText, fontSize = 12.sp)
            }
        }

        if (statusMessage != null) {
            Text(statusMessage ?: "", color = MintGreen, fontSize = 12.sp)
        }

        // Storyboard Breakdown
        Text("SCENE-BY-SCENE STORYBOARD", color = SecondaryText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        currentProject.scenes.forEach { scene ->
            val isCurrent = scene.sceneIndex == currentSceneIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        currentSceneIndex = scene.sceneIndex
                    },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = if (isCurrent) SurfaceVariantDark else CardBackground)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Scene ${scene.sceneIndex + 1}: ${scene.sceneTitle}",
                            color = if (isCurrent) MintGreen else PrimaryText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("${scene.durationSeconds}s", color = SecondaryText, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(scene.narrationText, color = SecondaryText, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }

    // Export Transcript Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Video Narration Script", color = PrimaryText, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Title: ${currentProject.title}", color = MintGreen, fontWeight = FontWeight.Bold)
                    Text("Total Runtime: ${currentProject.totalDurationSeconds} seconds", color = SecondaryText, fontSize = 11.sp)
                    Divider(color = BorderStroke)
                    Text(currentProject.fullTranscript, color = PrimaryText, fontSize = 12.sp, lineHeight = 18.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = MintGreen)
                }
            },
            containerColor = CardBackground
        )
    }
}
