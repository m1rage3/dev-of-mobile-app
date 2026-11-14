package com.example.lab7player

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlayPause: Button

    private var currentState = "stopped"

    // сервис сообщает о смене трека и состояния
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getStringExtra("state") ?: return
            val title = intent.getStringExtra("title") ?: ""
            updateUiForState(state, title)
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTitle = findViewById(R.id.tvTrackTitle)
        tvStatus = findViewById(R.id.tvStatus)
        btnPlayPause = findViewById(R.id.btnPlayPause)

        // на Android 13+ без разрешения уведомление не будет видно
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        btnPlayPause.setOnClickListener {
            sendCommand(if (currentState == "playing") MediaService.ACTION_PAUSE else MediaService.ACTION_PLAY)
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener { sendCommand(MediaService.ACTION_NEXT) }
        findViewById<Button>(R.id.btnPrevious).setOnClickListener { sendCommand(MediaService.ACTION_PREVIOUS) }
        findViewById<Button>(R.id.btnStop).setOnClickListener { sendCommand(MediaService.ACTION_STOP) }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(stateReceiver, IntentFilter(MediaService.BROADCAST_STATE))
        // показываем то, что сейчас играет (если музыка шла в фоне)
        updateUiForState(MediaService.lastState, MediaService.lastTitle)
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(stateReceiver)
    }

    private fun sendCommand(action: String) {
        Intent(this, MediaService::class.java).apply {
            this.action = action
            startService(this)
        }
    }

    private fun updateUiForState(state: String, title: String) {
        currentState = state
        tvTitle.text = title.ifEmpty { "Плейлист: 3 трека" }
        tvStatus.text = when (state) {
            "playing" -> "Воспроизводится"
            "paused" -> "На паузе"
            "error" -> "Ошибка воспроизведения"
            else -> "Остановлено"
        }
        btnPlayPause.text = if (state == "playing") "Пауза" else "Играть"
    }
}
