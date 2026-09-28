package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TaskItem
import com.example.data.TaskLog
import com.example.engine.SystemActionHelper
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailDialog(
    task: TaskItem,
    logs: List<TaskLog>,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = task.projectName ?: task.taskType,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: ${task.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (task.status in listOf("RUNNING", "QUEUED")) {
                                TextButton(onClick = onCancel) {
                                    Text("Stop Task", color = MaterialTheme.colorScheme.error)
                                }
                            } else if (task.status == "FAILED") {
                                TextButton(onClick = onRetry) {
                                    Text("Retry")
                                }
                            }
                            IconButton(onClick = onDelete) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Status & Meta Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(status = task.status)
                                    Text(
                                        text = "Priority: ${task.priority}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(
                                    text = "Request: \"${task.userRequest}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Provider: ${task.provider}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (task.parentTaskId != null) {
                                    Text(
                                        text = "Prerequisite: Waits for Task ${task.parentTaskId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Current State: ${task.currentStep}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Artifact Preview
                    if (!task.artifactData.isNullOrBlank()) {
                        item {
                            Text(
                                text = "Verified Artifact Output",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            when (task.artifactType) {
                                "THUMBNAIL" -> ThumbnailVisualCard(data = task.artifactData, context = context)
                                "CODE" -> CodeArtifactCard(code = task.artifactData, context = context)
                                "APK_INFO" -> ApkWorkflowCard(info = task.artifactData, context = context)
                                "INTENT" -> IntentArtifactCard(task = task, context = context)
                                else -> TextArtifactCard(text = task.artifactData, context = context)
                            }
                        }
                    }

                    // Execution Logs Section
                    item {
                        Text(
                            text = "Autonomous Execution Logs (${logs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "No execution logs recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(logs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (color, icon) = when (log.level) {
                                    "SUCCESS" -> Color(0xFF10B981) to Icons.Default.CheckCircle
                                    "ERROR" -> Color(0xFFEF4444) to Icons.Default.Error
                                    "WARN" -> Color(0xFFF59E0B) to Icons.Default.Warning
                                    "STEP" -> Color(0xFF6366F1) to Icons.Default.PlayArrow
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.Info
                                }
                                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.2f) to Color(0xFF047857)
        "RUNNING" -> Color(0xFF3B82F6).copy(alpha = 0.2f) to Color(0xFF1D4ED8)
        "WAITING" -> Color(0xFF8B5CF6).copy(alpha = 0.2f) to Color(0xFF6D28D9)
        "QUEUED" -> Color(0xFFF59E0B).copy(alpha = 0.2f) to Color(0xFFB45309)
        "FAILED" -> Color(0xFFEF4444).copy(alpha = 0.2f) to Color(0xFFB91C1C)
        else -> Color.Gray.copy(alpha = 0.2f) to Color.DarkGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = status, color = textColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ThumbnailVisualCard(data: String, context: Context) {
    val title = try { JSONObject(data).optString("title", "THUMBNAIL") } catch (e: Exception) { "THUMBNAIL" }
    val badge = try { JSONObject(data).optString("badge", "HD") } catch (e: Exception) { "HD" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Visual 16:9 Thumbnail representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81))
                        )
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFCD34D)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% PROVEN RESULTS • MUST WATCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        SystemActionHelper.shareContent(context, "YouTube Thumbnail design for: $title")
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share Thumbnail")
                }
            }
        }
    }
}

@Composable
fun CodeArtifactCard(code: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Kotlin Source", color = Color(0xFFCDD6F4), style = MaterialTheme.typography.labelMedium)
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = Color(0xFFCDD6F4))
                }
            }
            HorizontalDivider(color = Color(0xFF313244))
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFA6ADC8),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ApkWorkflowCard(info: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("GitHub Actions Debug APK Pipeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(info, style = MaterialTheme.typography.bodySmall)

            Button(
                onClick = {
                    SystemActionHelper.shareContent(context, info, "Share APK Build Instructions")
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Specs")
            }
        }
    }
}

@Composable
fun IntentArtifactCard(task: TaskItem, context: Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ready to Launch on Device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(task.userRequest, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = {
                    val req = task.userRequest.lowercase()
                    when {
                        req.contains("youtube") -> SystemActionHelper.openYouTube(context, null)
                        req.contains("whatsapp") -> SystemActionHelper.openWhatsApp(context, message = task.userRequest)
                        req.contains("call") -> SystemActionHelper.dialContact(context, task.userRequest.filter { it.isDigit() })
                        else -> SystemActionHelper.setReminderAlarm(context, task.userRequest)
                    }
                }
            ) {
                Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Launch Intent Now")
            }
        }
    }
}

@Composable
fun TextArtifactCard(text: String, context: Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Summary", text))
                        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                }
                IconButton(
                    onClick = {
                        SystemActionHelper.shareContent(context, text)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share")
                }
            }
        }
    }
}
