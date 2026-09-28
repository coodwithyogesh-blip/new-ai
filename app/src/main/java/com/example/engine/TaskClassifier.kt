package com.example.engine

import com.example.data.TaskType

data class ClassificationResult(
    val taskType: TaskType,
    val projectName: String?,
    val isProjectModification: Boolean,
    val isCommand: Boolean,
    val commandType: CommandType? = null,
    val targetParameter: String? = null
)

enum class CommandType {
    CANCEL_TASK,
    FOCUS_TASK,
    QUERY_STATUS,
    SHOW_ACTIVE,
    RESUME_TASK,
    CLEAR_CHAT
}

object TaskClassifier {

    fun classify(input: String, currentActiveProject: String?): ClassificationResult {
        val text = input.trim()
        val lower = text.lowercase()

        // 1. Check for Task Management Commands first
        if (lower.contains("stop karo") || lower.contains("cancel task") || lower.contains("rok do") || lower.contains("ruk jao")) {
            return ClassificationResult(
                taskType = TaskType.CONVERSATION,
                projectName = currentActiveProject,
                isProjectModification = false,
                isCommand = true,
                commandType = CommandType.CANCEL_TASK
            )
        }

        if (lower.contains("status kya hai") || lower.contains("status batao") || lower.contains("kahan tak pahuncha")) {
            return ClassificationResult(
                taskType = TaskType.CONVERSATION,
                projectName = currentActiveProject,
                isProjectModification = false,
                isCommand = true,
                commandType = CommandType.QUERY_STATUS
            )
        }

        if (lower.contains("active task") || lower.contains("saare task") || lower.contains("tasks dikhao")) {
            return ClassificationResult(
                taskType = TaskType.CONVERSATION,
                projectName = currentActiveProject,
                isProjectModification = false,
                isCommand = true,
                commandType = CommandType.SHOW_ACTIVE
            )
        }

        if (lower.contains("continue karo") || lower.contains("resume karo") || lower.contains("dobara chalao")) {
            return ClassificationResult(
                taskType = TaskType.CONVERSATION,
                projectName = currentActiveProject,
                isProjectModification = false,
                isCommand = true,
                commandType = CommandType.RESUME_TASK
            )
        }

        if (lower.contains("task dikhao") || lower.contains("open task")) {
            val taskKeyword = text.replace(Regex("(?i)(task dikhao|open task|wala)"), "").trim()
            return ClassificationResult(
                taskType = TaskType.CONVERSATION,
                projectName = currentActiveProject,
                isProjectModification = false,
                isCommand = true,
                commandType = CommandType.FOCUS_TASK,
                targetParameter = taskKeyword
            )
        }

        // 2. Identify Project Modification vs New Task
        val isModification = currentActiveProject != null && (
                lower.startsWith("isme") ||
                        lower.contains("login add karo") ||
                        lower.contains("add karo") ||
                        lower.contains("feature daal") ||
                        lower.contains("update karo") ||
                        lower.contains("modify karo") ||
                        lower.contains("isko change karo")
                )

        // 3. Classify Domain Tasks
        val taskType = when {
            lower.contains("apk") || lower.contains("build") || lower.contains("release") || lower.contains("publish") ->
                TaskType.APK_BUILD

            lower.contains("thumbnail") || lower.contains("banner") || lower.contains("cover") ->
                TaskType.THUMBNAIL_CREATION

            lower.contains("photo") || lower.contains("image") || lower.contains("tasveer") || lower.contains("draw") || lower.contains("picture") ->
                TaskType.IMAGE_GENERATION

            lower.contains("video") || lower.contains("reel") || lower.contains("shorts") || lower.contains("render") ->
                TaskType.VIDEO_EDITING

            lower.contains("call") || lower.contains("dial") || lower.contains("phone mila") ->
                TaskType.PHONE_ACTION

            lower.contains("whatsapp") || lower.contains("msg") || lower.contains("message bhejo") ->
                TaskType.PHONE_ACTION

            lower.contains("youtube") || lower.contains("kholo") || lower.contains("open") ->
                TaskType.APP_OPENING

            lower.contains("email") || lower.contains("mail") || lower.contains("patra") ->
                TaskType.EMAIL_DRAFTING

            lower.contains("reminder") || lower.contains("alarm") || lower.contains("yaad dilana") ->
                TaskType.REMINDER

            lower.contains("summarize") || lower.contains("summary") || lower.contains("pdf") || lower.contains("document") || lower.contains("doc") ->
                TaskType.DOCUMENT_WORK

            lower.contains("translate") || lower.contains("anuvad") || lower.contains("hindi mein") || lower.contains("english mein") ->
                TaskType.TRANSLATION

            lower.contains("fix") || lower.contains("bug") || lower.contains("error") || lower.contains("debugging") ->
                TaskType.DEBUGGING

            lower.contains("app") || lower.contains("application") || lower.contains("bana do") || lower.contains("project") ->
                TaskType.APP_DEVELOPMENT

            lower.contains("code") || lower.contains("function") || lower.contains("script") || lower.contains("class") ->
                TaskType.CODING

            lower.contains("audio") || lower.contains("voice") || lower.contains("music") || lower.contains("gana") ->
                TaskType.AUDIO_WORK

            lower.contains("file") || lower.contains("rename") || lower.contains("backup") || lower.contains("folder") ->
                TaskType.FILE_MANAGEMENT

            lower.contains("kya hai") || lower.contains("who is") || lower.contains("what is") || lower.contains("research") || lower.contains("kyun") ->
                TaskType.QUESTION_ANSWER

            else ->
                TaskType.CONVERSATION
        }

        // Project Name Extraction
        val projectName = if (isModification) {
            currentActiveProject
        } else if (taskType == TaskType.APP_DEVELOPMENT) {
            extractProjectName(text)
        } else {
            null
        }

        return ClassificationResult(
            taskType = taskType,
            projectName = projectName,
            isProjectModification = isModification,
            isCommand = false
        )
    }

    private fun extractProjectName(text: String): String {
        val clean = text.replace(Regex("(?i)(arushi|ek|ka|ke|liye|app|bana|do|banaiye|make|create|build|an|a|application|project)"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
        return if (clean.length in 2..30) {
            clean.split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) } + " App"
        } else {
            "Custom App"
        }
    }
}
