package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.*
import com.example.voice.VoiceEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String) {
    ASSISTANT("Assistant"),
    TASK_CENTER("Task Center"),
    CAPABILITIES("Capabilities")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ArushiDatabase.getDatabase(application)
    val voiceEngine = VoiceEngine(application)
    private val geminiService = GeminiService()

    val taskEngine = TaskExecutionEngine(
        context = application,
        db = db,
        voiceEngine = voiceEngine,
        geminiService = geminiService
    )

    // Current Screen Tab
    private val _currentTab = MutableStateFlow(ScreenTab.ASSISTANT)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Task Center Filter
    private val _selectedFilter = MutableStateFlow<String?>("ALL")
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    // Tasks & Chat flows
    val allTasks: StateFlow<List<TaskItem>> = db.taskDao().getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTaskCount: StateFlow<Int> = db.taskDao().getActiveTaskCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val chatMessages: StateFlow<List<ChatMessage>> = db.chatDao().getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Task for Detailed Dialog / Inspector
    private val _selectedTask = MutableStateFlow<TaskItem?>(null)
    val selectedTask: StateFlow<TaskItem?> = _selectedTask.asStateFlow()

    val selectedTaskLogs: StateFlow<List<TaskLog>> = _selectedTask
        .flatMapLatest { task ->
            if (task != null) db.taskDao().getLogsForTask(task.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice & Input State
    val isSpeaking = voiceEngine.isSpeaking
    val isMuted = voiceEngine.isMuted
    val currentEmotion = voiceEngine.currentEmotion

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    init {
        TaskNotificationHelper.initChannel(application)
        // Add welcome message if chat is empty
        viewModelScope.launch {
            val messages = db.chatDao().getAllMessages().firstOrNull() ?: emptyList()
            if (messages.isEmpty()) {
                val welcome = "Namaste! Main Arushi hoon — aapki universal multi-task AI assistant. Aap mujhe app banane, APK build karne, thumbnail design, coding, ya phone actions jaise multiple tasks de sakte hain."
                db.chatDao().insertMessage(
                    ChatMessage(
                        sender = "ARUSHI",
                        text = welcome,
                        emotion = TaskEmotion.START.name
                    )
                )
            }
        }
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun setFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun selectTask(task: TaskItem?) {
        _selectedTask.value = task
    }

    fun toggleMute() {
        voiceEngine.toggleMute()
    }

    fun startListening() {
        _isListening.value = true
    }

    fun stopListening() {
        _isListening.value = false
    }

    fun cancelTask(taskId: String) {
        taskEngine.cancelTask(taskId)
    }

    fun retryTask(taskId: String) {
        taskEngine.retryTask(taskId)
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            db.taskDao().deleteTaskById(taskId)
            if (_selectedTask.value?.id == taskId) {
                _selectedTask.value = null
            }
        }
    }

    fun handleUserInput(input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            // Record user message in chat
            db.chatDao().insertMessage(
                ChatMessage(
                    sender = "USER",
                    text = input
                )
            )

            // Step 1: Classify & Contextualize
            val classification = TaskClassifier.classify(
                input = input,
                currentActiveProject = taskEngine.activeProjectName
            )

            // Step 2: Handle Command vs Task Execution
            if (classification.isCommand) {
                handleCommand(classification)
                return@launch
            }

            // Step 3: Check for dependent task request
            // e.g., "Jab tak ye ban raha hai, thumbnail bhi bana do" -> Independent parallel task
            // e.g., "App banne ke baad APK bana do" -> Dependent task
            val isDependent = input.lowercase().contains("ke baad") || input.lowercase().contains("hone ke baad")
            val parentTaskId = if (isDependent) {
                db.taskDao().getActiveTasks().firstOrNull()?.firstOrNull()?.id
            } else {
                null
            }

            // Step 4: Submit to Task Execution Engine
            val taskId = taskEngine.submitTask(
                userRequest = input,
                taskType = classification.taskType,
                projectName = classification.projectName,
                parentTaskId = parentTaskId
            )
        }
    }

    private suspend fun handleCommand(cmd: ClassificationResult) {
        when (cmd.commandType) {
            CommandType.CANCEL_TASK -> {
                val active = db.taskDao().getActiveTasks().firstOrNull()?.firstOrNull()
                if (active != null) {
                    taskEngine.cancelTask(active.id)
                } else {
                    val reply = "Abhi koi running task nahi hai."
                    voiceEngine.speak(reply, TaskEmotion.NEUTRAL)
                    db.chatDao().insertMessage(ChatMessage(sender = "ARUSHI", text = reply))
                }
            }
            CommandType.QUERY_STATUS -> {
                val activeTasks = db.taskDao().getActiveTasks().firstOrNull() ?: emptyList()
                val reply = if (activeTasks.isNotEmpty()) {
                    val first = activeTasks.first()
                    "Abhi '${first.projectName ?: first.taskType}' task chal raha hai: ${first.currentStep}"
                } else {
                    "Abhi koi task process nahi ho raha hai."
                }
                voiceEngine.speak(reply, TaskEmotion.PROGRESS)
                db.chatDao().insertMessage(ChatMessage(sender = "ARUSHI", text = reply))
            }
            CommandType.SHOW_ACTIVE -> {
                _currentTab.value = ScreenTab.TASK_CENTER
                _selectedFilter.value = "ACTIVE"
                val reply = "Active tasks Task Center mein dikha rahi hoon."
                voiceEngine.speak(reply, TaskEmotion.NEUTRAL)
            }
            CommandType.RESUME_TASK -> {
                val pausedOrFailed = db.taskDao().getAllTasks().firstOrNull()?.firstOrNull { it.status == TaskStatus.FAILED.name }
                if (pausedOrFailed != null) {
                    taskEngine.retryTask(pausedOrFailed.id)
                    val reply = "Task resume kar diya gaya hai."
                    voiceEngine.speak(reply, TaskEmotion.START)
                    db.chatDao().insertMessage(ChatMessage(sender = "ARUSHI", text = reply))
                }
            }
            CommandType.FOCUS_TASK -> {
                val keyword = cmd.targetParameter?.lowercase() ?: ""
                val found = db.taskDao().getAllTasks().firstOrNull()?.firstOrNull {
                    it.userRequest.lowercase().contains(keyword) || it.taskType.lowercase().contains(keyword) || (it.projectName?.lowercase()?.contains(keyword) == true)
                }
                if (found != null) {
                    _selectedTask.value = found
                    _currentTab.value = ScreenTab.TASK_CENTER
                    val reply = "${found.projectName ?: found.taskType} task open kar diya gaya hai."
                    voiceEngine.speak(reply, TaskEmotion.SUCCESS)
                } else {
                    val reply = "Mujhe aisa koi task nahi mila."
                    voiceEngine.speak(reply, TaskEmotion.FAILURE)
                }
            }
            else -> {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.shutdown()
    }
}
