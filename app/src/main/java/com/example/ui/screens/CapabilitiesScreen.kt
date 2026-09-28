package com.example.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class CapabilityItem(
    val title: String,
    val description: String,
    val samplePrompt: String,
    val icon: ImageVector
)

@Composable
fun CapabilitiesScreen(
    onTriggerPrompt: (String) -> Unit
) {
    val capabilities = listOf(
        CapabilityItem(
            title = "Android Debug APK & Release",
            description = "Verifies AGP 9.1.1, Java 17, Gradle 9.3.1, assembleDebug, and GitHub Releases workflow for direct phone download.",
            samplePrompt = "Build debug APK and verify workflow",
            icon = Icons.Default.Android
        ),
        CapabilityItem(
            title = "App Development & Architecture",
            description = "Architects modern Android Jetpack Compose UI, MVVM ViewModels, and Room Database persistence.",
            samplePrompt = "Carpenter app bana do",
            icon = Icons.Default.DeveloperMode
        ),
        CapabilityItem(
            title = "YouTube Thumbnail Creation",
            description = "Renders high-impact 16:9 thumbnail banners with typography and bold contrast.",
            samplePrompt = "YouTube video ka thumbnail bana do",
            icon = Icons.Default.Palette
        ),
        CapabilityItem(
            title = "Autonomous Multi-Tasking",
            description = "Runs independent tasks in parallel and waits for prerequisite dependent tasks automatically.",
            samplePrompt = "Carpenter app bana do aur jab tak ye ban raha hai thumbnail bhi bana do",
            icon = Icons.Default.Speed
        ),
        CapabilityItem(
            title = "Code Generation & Debugging",
            description = "Generates clean Kotlin functions, analyzes crash logs, and resolves syntax or logic bugs.",
            samplePrompt = "Is code ka bug fix karo",
            icon = Icons.Default.Code
        ),
        CapabilityItem(
            title = "Document Work & Summaries",
            description = "Extracts key insights, executive summaries, and action items from text and documents.",
            samplePrompt = "Ye document summarize karo",
            icon = Icons.Default.Description
        ),
        CapabilityItem(
            title = "Email & Content Writing",
            description = "Drafts formal emails, proposals, project updates, and meeting notes.",
            samplePrompt = "Ek professional email draft karo",
            icon = Icons.Default.Email
        ),
        CapabilityItem(
            title = "Device Actions & Apps",
            description = "Directly triggers device dialer, WhatsApp messages, YouTube queries, and timers.",
            samplePrompt = "WhatsApp kholo",
            icon = Icons.Default.Launch
        ),
        CapabilityItem(
            title = "Emotion-Adaptive Voice",
            description = "Speaks in natural Hindi/English with emotion modulation (Confident, Calm, Warm, Focused).",
            samplePrompt = "Saare active tasks dikhao",
            icon = Icons.Default.RecordVoiceOver
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Arushi Capabilities",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Universal assistant capable of running any supported task independently or in parallel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(capabilities) { cap ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    cap.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cap.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = cap.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { onTriggerPrompt(cap.samplePrompt) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Try: \"${cap.samplePrompt}\"", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
