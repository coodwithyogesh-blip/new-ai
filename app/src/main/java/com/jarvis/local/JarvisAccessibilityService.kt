package com.jarvis.local

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.util.Locale

class JarvisAccessibilityService : AccessibilityService() {
    companion object {
        private var instance: JarvisAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null

        fun dispatch(context: android.content.Context, action: String, value: String = ""): Boolean {
            if (instance == null) return false
            val intent = android.content.Intent(context, JarvisAccessibilityService::class.java)
                .setAction("com.jarvis.local.ACCESSIBILITY_ACTION")
                .putExtra("action", action)
                .putExtra("value", value)
            context.startService(intent)
            return true
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        Toast.makeText(this, "JARVIS Universal App Control enabled.", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "com.jarvis.local.ACCESSIBILITY_ACTION") {
            val action = intent.getStringExtra("action").orEmpty()
            val value = intent.getStringExtra("value").orEmpty()
            execute(action, value, 0)
        }
        return START_NOT_STICKY
    }

    private fun execute(action: String, value: String, attempt: Int) {
        handler.postDelayed({
            when (action.lowercase(Locale.getDefault())) {
                "tap", "click" -> {
                    if (!tapText(value) && attempt < 8) execute(action, value, attempt + 1)
                    else if (attempt >= 8) notifyUser("Could not find: $value")
                }
                "type" -> {
                    if (!setText(value) && attempt < 8) execute(action, value, attempt + 1)
                    else if (attempt >= 8) notifyUser("No editable field found.")
                }
                "send" -> {
                    if (!tapAny(listOf(value, "send", "send message", "submit", "done")) && attempt < 8) {
                        execute(action, value, attempt + 1)
                    } else if (attempt >= 8) notifyUser("Send button was not found.")
                }
                "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
                "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
                "swipe_up" -> swipe(0.5f, 0.82f, 0.5f, 0.25f)
                "swipe_down" -> swipe(0.5f, 0.25f, 0.5f, 0.82f)
                "swipe_left" -> swipe(0.82f, 0.5f, 0.20f, 0.5f)
                "swipe_right" -> swipe(0.20f, 0.5f, 0.82f, 0.5f)
                else -> notifyUser("Unknown universal action: $action")
            }
        }, if (attempt == 0) 120L else 350L)
    }

    private fun roots(): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        getRootInActiveWindow()?.let { result.add(it) }
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            getWindows().forEach { window ->
                window.root?.let { root ->
                    if (result.none { it.windowId == root.windowId }) result.add(root)
                }
            }
        }
        return result
    }

    private fun allNodes(root: AccessibilityNodeInfo): Sequence<AccessibilityNodeInfo> = sequence {
        yield(root)
        for (i in 0 until root.childCount) {
            root.getChild(i)?.let { child ->
                yieldAll(allNodes(child))
            }
        }
    }

    private fun normalized(s: CharSequence?): String =
        s?.toString()?.trim()?.lowercase(Locale.getDefault()).orEmpty()

    private fun findByText(query: String): AccessibilityNodeInfo? {
        val q = normalized(query)
        if (q.isBlank()) return null
        for (root in roots()) {
            for (node in allNodes(root)) {
                val text = normalized(node.text)
                val desc = normalized(node.contentDescription)
                if (text == q || desc == q || text.contains(q) || desc.contains(q)) return node
            }
        }
        return null
    }

    private fun tapText(query: String): Boolean {
        val node = findByText(query) ?: return false
        if (clickNodeOrParent(node)) return true
        val r = android.graphics.Rect()
        node.getBoundsInScreen(r)
        if (r.isEmpty) return false
        val path = Path().apply { moveTo(r.centerX().toFloat(), r.centerY().toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 80))
            .build()
        return dispatchGesture(gesture, null, handler)
    }

    private fun tapAny(values: List<String>): Boolean {
        values.filter { it.isNotBlank() }.forEach { if (tapText(it)) return true }
        return false
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        repeat(5) {
            if (current?.isClickable == true) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current?.parent
        }
        return false
    }

    private fun setText(value: String): Boolean {
        var node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (node == null) {
            for (root in roots()) {
                node = allNodes(root).firstOrNull {
                    it.isEditable || normalized(it.className).contains("edittext")
                }
                if (node != null) break
            }
        }
        node ?: return false
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value
            )
        }
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true
        return clickNodeOrParent(node)
    }

    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dm = resources.displayMetrics
        val path = Path().apply {
            moveTo(dm.widthPixels * x1, dm.heightPixels * y1)
            lineTo(dm.widthPixels * x2, dm.heightPixels * y2)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 450))
            .build()
        return dispatchGesture(gesture, null, handler)
    }

    private fun notifyUser(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        getSharedPreferences("jarvis_automation", MODE_PRIVATE)
            .edit().putString("last_status", message).apply()
    }
}
