package br.com.ekko

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class EkkoService : Service(), TextToSpeech.OnInitListener {
    private var sr: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private val h = Handler(Looper.getMainLooper())
    private var running = false
    private var awaitUntil = 0L

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(s: Int) { tts.language = Locale("pt", "BR") }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("ekko", "Ekko", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "ekko")
            .setContentTitle("Ekko está ouvindo")
            .setContentText("Diga “Ekko” e o comando")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)
        if (!running) { running = true; listen() }
        return START_STICKY
    }

    private fun listen() {
        if (!running) return
        if (tts.isSpeaking) { restart(); return }
        sr?.destroy()
        sr = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle?) {
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { hear(it) }
                    restart()
                }
                override fun onError(e: Int) { restart() }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(r: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(b: Bundle?) {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR"))
        }
    }

    private fun restart() { h.postDelayed({ listen() }, 400) }

    private fun hear(t: String) {
        val m = Regex("^(?:ei |ok |oi |ou )?(?:ekko|eco|echo|eko)[,.! ]*(.*)", RegexOption.IGNORE_CASE).find(t.trim())
        val cmd = when {
            m != null -> m.groupValues[1]
            System.currentTimeMillis() < awaitUntil -> t
            else -> return
        }
        awaitUntil = 0
        if (cmd.isBlank()) { say("Pois não?"); awaitUntil = System.currentTimeMillis() + 8000; return }
        Commands.run(this, cmd, ::say)
    }

    private fun say(t: String) { h.post { tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "ekko") } }

    override fun onDestroy() {
        running = false
        sr?.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
