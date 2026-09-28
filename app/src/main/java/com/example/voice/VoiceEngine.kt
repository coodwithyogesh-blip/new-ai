package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.TaskEmotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentEmotion = MutableStateFlow(TaskEmotion.NEUTRAL)
    val currentEmotion: StateFlow<TaskEmotion> = _currentEmotion.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.let { engine ->
                // Try Hindi first if supported for bilingual natural Hindi/English, fallback to default/US
                val hindiLocale = Locale("hi", "IN")
                val result = engine.setLanguage(hindiLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.language = Locale.getDefault()
                }

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            }
        } else {
            Log.e("VoiceEngine", "TextToSpeech initialization failed: $status")
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        if (_isMuted.value) {
            stop()
        }
    }

    fun speak(text: String, emotion: TaskEmotion = TaskEmotion.NEUTRAL) {
        if (_isMuted.value || !isInitialized || text.isBlank()) return

        _currentEmotion.value = emotion
        tts?.let { engine ->
            // Modulate pitch and speech rate based on task emotion
            engine.setPitch(emotion.pitch)
            engine.setSpeechRate(emotion.speed)

            val utteranceId = "arushi_${System.currentTimeMillis()}"
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
