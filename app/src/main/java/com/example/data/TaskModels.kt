package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskType(val label: String, val iconName: String) {
    CONVERSATION("Conversation", "chat"),
    QUESTION_ANSWER("Research & Q&A", "help"),
    CODING("Code Generation", "code"),
    APP_DEVELOPMENT("App Development", "developer_mode"),
    APK_BUILD("APK Build & Release", "build"),
    DEBUGGING("Bug Fix & Debug", "bug_report"),
    FILE_MANAGEMENT("File Management", "folder"),
    DOCUMENT_WORK("Document & Summary", "description"),
    IMAGE_GENERATION("Image Generation", "image"),
    THUMBNAIL_CREATION("Thumbnail Design", "palette"),
    VIDEO_EDITING("Video Planning", "video_call"),
    AUDIO_WORK("Audio & Voice", "audiotrack"),
    TRANSLATION("Translation", "translate"),
    EMAIL_DRAFTING("Email & Writing", "email"),
    REMINDER("Reminder & Task", "alarm"),
    PHONE_ACTION("Phone & Call", "call"),
    APP_OPENING("Open App", "open_in_new"),
    CLOUD_TASK("Cloud Operation", "cloud")
}

enum class TaskStatus(val label: String) {
    QUEUED("Queued"),
    RUNNING("Running"),
    WAITING("Waiting"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class TaskEmotion(val label: String, val pitch: Float, val speed: Float) {
    START("Confident & Helpful", 1.05f, 1.0f),
    PROGRESS("Calm & Reassuring", 0.98f, 0.95f),
    SUCCESS("Warm & Happy", 1.15f, 1.02f),
    ERROR("Calm & Focused", 0.92f, 0.90f),
    WAITING("Patient", 0.95f, 0.92f),
    FAILURE("Honest & Calm", 0.90f, 0.88f),
    NEUTRAL("Conversational", 1.0f, 1.0f)
}

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey val id: String,
    val userRequest: String,
    val taskType: String,
    val status: String, // from TaskStatus
    val priority: String = "MEDIUM",
    val currentStep: String,
    val provider: String,
    val providerJobId: String? = null,
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis(),
    val resultPayload: String? = null,
    val artifactType: String? = null, // "CODE", "IMAGE", "TEXT", "INTENT", "APK_INFO"
    val artifactData: String? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val parentTaskId: String? = null,
    val projectName: String? = null
)

@Entity(tableName = "task_logs")
data class TaskLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: String = "INFO" // INFO, STEP, WARN, ERROR, SUCCESS
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER", "ARUSHI", "SYSTEM"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedTaskId: String? = null,
    val emotion: String = "NEUTRAL"
)
