package com.example

import com.example.data.TaskType
import com.example.engine.CommandType
import com.example.engine.TaskClassifier
import org.junit.Assert.*
import org.junit.Test

class TaskClassifierTest {

    @Test
    fun testAppDevelopmentClassification() {
        val result = TaskClassifier.classify("Carpenter app bana do", null)
        assertEquals(TaskType.APP_DEVELOPMENT, result.taskType)
        assertFalse(result.isCommand)
    }

    @Test
    fun testApkBuildClassification() {
        val result = TaskClassifier.classify("APK bana do", "Carpenter App")
        assertEquals(TaskType.APK_BUILD, result.taskType)
        assertFalse(result.isCommand)
    }

    @Test
    fun testThumbnailClassification() {
        val result = TaskClassifier.classify("YouTube thumbnail bana do", "Carpenter App")
        assertEquals(TaskType.THUMBNAIL_CREATION, result.taskType)
        assertFalse(result.isProjectModification)
    }

    @Test
    fun testProjectModification() {
        val result = TaskClassifier.classify("Isme login add karo", "Carpenter App")
        assertTrue(result.isProjectModification)
        assertEquals("Carpenter App", result.projectName)
    }

    @Test
    fun testStopCommand() {
        val result = TaskClassifier.classify("Ye task stop karo", null)
        assertTrue(result.isCommand)
        assertEquals(CommandType.CANCEL_TASK, result.commandType)
    }

    @Test
    fun testStatusQueryCommand() {
        val result = TaskClassifier.classify("Thumbnail ka status kya hai?", null)
        assertTrue(result.isCommand)
        assertEquals(CommandType.QUERY_STATUS, result.commandType)
    }
}
