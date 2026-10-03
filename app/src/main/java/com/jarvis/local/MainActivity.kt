package com.jarvis.local

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ChatMessage(val fromJarvis: Boolean, val text: String)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        setContent { JarvisApp(onSpeak = ::speak, onRequestMic = ::requestMic) }
    }

    private fun requestMic() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun JarvisApp(onSpeak: (String) -> Unit, onRequestMic: () -> Unit) {
    val messages = remember {
        mutableStateListOf(ChatMessage(true, "Good to see you. JARVIS local core is online."))
    }
    var input by remember { mutableStateOf("") }

    fun respond(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return
        messages.add(ChatMessage(false, q))
        val lower = q.lowercase(Locale.getDefault())
        val answer = when {
            "hello" in lower || "hi" in lower || "namaste" in lower ->
                "Hello. I am ready."
            "time" in lower ->
                "The current time is " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            "who are you" in lower || "what are you" in lower ->
                "I am JARVIS AI, a local-first Android assistant. Online APIs are not required for this core."
            "help" in lower ->
                "Try: hello, time, who are you, or ask me to remember something. Voice, memory, vision and device tools are designed as separate modules."
            else ->
                "I received that locally. The next JARVIS modules can route this request to memory, device tools, vision, or a local model."
        }
        messages.add(ChatMessage(true, answer))
        onSpeak(answer)
        input = ""
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070A12)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Header()
                JarvisOrb()
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    messages.takeLast(5).forEach {
                        ChatBubble(it)
                        Spacer(Modifier.height(8.dp))
                    }
                }
                QuickActions(onMic = onRequestMic, onAction = { respond(it) })
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk to JARVIS…") },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp)
                    )
                    IconButton(onClick = { respond(input) }) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("JARVIS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("LOCAL INTELLIGENCE CORE", style = MaterialTheme.typography.labelSmall, color = Color(0xFF70D8FF))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF38E08F)))
            Spacer(Modifier.size(6.dp))
            Text("ONLINE", color = Color(0xFF9CEBC4), style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        }
    }
}

@Composable
private fun JarvisOrb() {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = Modifier.fillMaxWidth().height(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size((128 * pulse).dp).alpha(0.16f)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF42C8FF), Color.Transparent)))
        )
        Box(
            modifier = Modifier.size(86.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF8AE7FF), Color(0xFF1565C0), Color(0xFF08111F))))
        )
        Text("J", color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromJarvis) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (message.fromJarvis) Color(0xFF101B2A) else Color(0xFF163B55),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(message.text, modifier = Modifier.padding(13.dp), color = Color.White)
        }
    }
}

@Composable
private fun QuickActions(onMic: () -> Unit, onAction: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onMic) {
            Icon(Icons.Default.Mic, contentDescription = null)
            Spacer(Modifier.size(5.dp))
            Text("Voice")
        }
        TextButton(onClick = { onAction("What can you do?") }) { Text("Capabilities") }
        TextButton(onClick = { onAction("What time is it?") }) { Text("Time") }
    }
}
