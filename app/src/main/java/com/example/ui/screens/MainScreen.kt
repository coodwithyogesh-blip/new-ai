package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.TaskDetailDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val activeTaskCount by viewModel.activeTaskCount.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    val selectedTaskLogs by viewModel.selectedTaskLogs.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val currentEmotion by viewModel.currentEmotion.collectAsStateWithLifecycle()

    var showVoicePromptDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Glowing Orb Indicator
                        GlowingOrb(isSpeaking = isSpeaking, isRunningTasks = activeTaskCount > 0)

                        Column {
                            Text(
                                text = "Arushi",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (activeTaskCount > 0) "$activeTaskCount task(s) active" else "Universal AI Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTaskCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleMute() },
                        modifier = Modifier.testTag("mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Voice" else "Mute Voice",
                            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == ScreenTab.ASSISTANT,
                    onClick = { viewModel.setTab(ScreenTab.ASSISTANT) },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Assistant") },
                    label = { Text("Assistant") }
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.TASK_CENTER,
                    onClick = { viewModel.setTab(ScreenTab.TASK_CENTER) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeTaskCount > 0) {
                                    Badge { Text("$activeTaskCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Dashboard, contentDescription = "Task Center")
                        }
                    },
                    label = { Text("Task Center") }
                )
                NavigationBarItem(
                    selected = currentTab == ScreenTab.CAPABILITIES,
                    onClick = { viewModel.setTab(ScreenTab.CAPABILITIES) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Capabilities") },
                    label = { Text("Capabilities") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.ASSISTANT -> {
                    ChatScreen(
                        messages = chatMessages,
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        currentEmotion = currentEmotion,
                        onSendMessage = { prompt -> viewModel.handleUserInput(prompt) },
                        onSelectTaskById = { taskId ->
                            val task = allTasks.find { it.id == taskId }
                            if (task != null) {
                                viewModel.selectTask(task)
                            }
                        },
                        onToggleListen = {
                            showVoicePromptDialog = true
                        }
                    )
                }

                ScreenTab.TASK_CENTER -> {
                    TaskCenterScreen(
                        tasks = allTasks,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { viewModel.setFilter(it) },
                        onSelectTask = { viewModel.selectTask(it) },
                        onCancelTask = { viewModel.cancelTask(it) },
                        onRetryTask = { viewModel.retryTask(it) }
                    )
                }

                ScreenTab.CAPABILITIES -> {
                    CapabilitiesScreen(
                        onTriggerPrompt = { prompt ->
                            viewModel.setTab(ScreenTab.ASSISTANT)
                            viewModel.handleUserInput(prompt)
                        }
                    )
                }
            }
        }
    }

    // Task Detail Inspector Dialog
    selectedTask?.let { task ->
        TaskDetailDialog(
            task = task,
            logs = selectedTaskLogs,
            onDismiss = { viewModel.selectTask(null) },
            onCancel = { viewModel.cancelTask(task.id) },
            onRetry = { viewModel.retryTask(task.id) },
            onDelete = { viewModel.deleteTask(task.id) }
        )
    }

    // Voice Input Dialog
    if (showVoicePromptDialog) {
        VoiceInputDialog(
            onDismiss = { showVoicePromptDialog = false },
            onSubmitVoice = { spokenText ->
                showVoicePromptDialog = false
                viewModel.handleUserInput(spokenText)
            }
        )
    }
}

@Composable
fun GlowingOrb(isSpeaking: Boolean, isRunningTasks: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isSpeaking) 500 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val orbColors = when {
        isSpeaking -> listOf(Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF06B6D4))
        isRunningTasks -> listOf(Color(0xFF3B82F6), Color(0xFF10B981))
        else -> listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(if (isSpeaking || isRunningTasks) pulseScale else 1f)
            .clip(CircleShape)
            .background(Brush.radialGradient(orbColors)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun VoiceInputDialog(
    onDismiss: () -> Unit,
    onSubmitVoice: (String) -> Unit
) {
    var voiceText by remember { mutableStateOf("") }
    val sampleVoicePrompts = listOf(
        "Carpenter app bana do",
        "Build debug APK",
        "YouTube thumbnail bana do",
        "WhatsApp kholo",
        "Saare active tasks dikhao",
        "Is code ka bug fix karo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Voice Assistant Input")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Speak or enter your command in Hindi, English, or Hinglish:",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = voiceText,
                    onValueChange = { voiceText = it },
                    placeholder = { Text("e.g. 'Carpenter app bana do'") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Quick Voice Commands:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sampleVoicePrompts.take(4).forEach { prompt ->
                        SuggestionChip(
                            onClick = { voiceText = prompt },
                            label = { Text(prompt, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (voiceText.isNotBlank()) {
                        onSubmitVoice(voiceText)
                    }
                },
                enabled = voiceText.isNotBlank()
            ) {
                Text("Process Voice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
