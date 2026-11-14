package com.example.lab6media

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class VideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private var isPrepared = false
    private var savedPosition = 0
    private var playWhenReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        videoView = findViewById(R.id.videoPlayer)
        progressBar = findViewById(R.id.videoProgress)
        statusText = findViewById(R.id.videoStatus)

        if (savedInstanceState != null) {
            savedPosition = savedInstanceState.getInt(KEY_POSITION, 0)
            playWhenReady = savedInstanceState.getBoolean(KEY_PLAYING, false)
        }

        // стандартные элементы управления: Play/Pause и прогресс-бар
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        videoView.setMediaController(controller)

        videoView.setOnPreparedListener {
            isPrepared = true
            progressBar.visibility = View.GONE
            if (savedPosition > 0) videoView.seekTo(savedPosition)
            if (playWhenReady) {
                videoView.start()
                statusText.text = "Воспроизводится"
            } else {
                statusText.text = "Готово"
            }
        }
        videoView.setOnCompletionListener {
            statusText.text = "Видео закончилось"
        }
        videoView.setOnErrorListener { _, what, extra ->
            isPrepared = false
            progressBar.visibility = View.GONE
            statusText.text = "Ошибка"
            showError("Ошибка видео (код $what/$extra)")
            true // не показываем системный диалог и не падаем
        }

        statusText.text = "Готовится…"
        progressBar.visibility = View.VISIBLE
        val uri = Uri.parse("android.resource://$packageName/${R.raw.sample_video}")
        videoView.setVideoURI(uri)

        findViewById<Button>(R.id.btnPlayVideo).setOnClickListener {
            if (!isPrepared) {
                showError("Подождите: файл готовится")
                return@setOnClickListener
            }
            videoView.start()
            statusText.text = "Воспроизводится"
        }
    }

    private fun showError(message: String) {
        Snackbar.make(videoView, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_POSITION, if (isPrepared) videoView.currentPosition else savedPosition)
        outState.putBoolean(KEY_PLAYING, playWhenReady)
    }

    override fun onPause() {
        super.onPause()
        playWhenReady = isChangingConfigurations && videoView.isPlaying
        if (isPrepared) savedPosition = videoView.currentPosition
        if (videoView.isPlaying) {
            videoView.pause()
            statusText.text = "На паузе"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // освобождаем внутренний MediaPlayer VideoView
        videoView.stopPlayback()
    }

    companion object {
        private const val KEY_POSITION = "video_position"
        private const val KEY_PLAYING = "video_playing"
    }
}
