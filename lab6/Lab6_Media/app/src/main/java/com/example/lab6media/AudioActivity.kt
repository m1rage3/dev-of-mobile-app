package com.example.lab6media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

class AudioActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var isPrepared = false

    private lateinit var audioManager: AudioManager
    private lateinit var statusText: TextView
    private lateinit var volumeSeekBar: SeekBar

    // что восстановить после поворота экрана
    private var resumePosition = 0
    private var playWhenReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)

        statusText = findViewById(R.id.statusText)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)

        // аппаратные кнопки громкости на этом экране тоже управляют медиапотоком
        volumeControlStream = AudioManager.STREAM_MUSIC

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupVolume()

        if (savedInstanceState != null) {
            resumePosition = savedInstanceState.getInt(KEY_POSITION, 0)
            playWhenReady = savedInstanceState.getBoolean(KEY_PLAYING, false)
        }

        // один плеер на весь экран, а не новый на каждый клик
        createPlayer()

        findViewById<Button>(R.id.btnPlayAudio).setOnClickListener { play() }
        findViewById<Button>(R.id.btnPauseAudio).setOnClickListener { pause() }
        findViewById<Button>(R.id.btnStopAudio).setOnClickListener { stop() }
    }

    private fun setupVolume() {
        // ползунок меняет громкость именно STREAM_MUSIC (не звонок и не уведомления)
        volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun createPlayer() {
        val uri = Uri.parse("android.resource://$packageName/${R.raw.sample_audio}")
        statusText.text = "Готовится…"
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(this@AudioActivity, uri)
                setOnPreparedListener { mp ->
                    isPrepared = true
                    if (resumePosition > 0) mp.seekTo(resumePosition)
                    if (playWhenReady) {
                        mp.start()
                        statusText.text = "Идёт воспроизведение"
                    } else {
                        statusText.text = if (resumePosition > 0) "На паузе" else "Готово к воспроизведению"
                    }
                }
                setOnCompletionListener {
                    statusText.text = "Воспроизведение завершено"
                }
                setOnErrorListener { _, what, extra ->
                    isPrepared = false
                    statusText.text = "Ошибка воспроизведения"
                    showError("Ошибка воспроизведения (код $what/$extra)")
                    true // ошибка обработана, приложение не падает
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            // например, файла нет или он повреждён
            player.release()
            mediaPlayer = null
            statusText.text = "Файл не найден"
            showError("Не удалось открыть аудиофайл: ${e.message}")
        }
    }

    private fun play() {
        val player = mediaPlayer
        if (player == null || !isPrepared) {
            showError("Подождите: файл ещё не готов")
            return
        }
        if (!player.isPlaying) {
            player.start()
            statusText.text = "Идёт воспроизведение"
        }
    }

    private fun pause() {
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            player.pause()
            statusText.text = "На паузе"
        }
    }

    // Stop: останавливаем и сбрасываем позицию в начало, плеер остаётся готовым
    private fun stop() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        if (player.isPlaying) player.pause()
        player.seekTo(0)
        statusText.text = "Остановлено"
    }

    private fun showError(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // запоминаем позицию и состояние, чтобы после поворота продолжить
        outState.putInt(KEY_POSITION, if (isPrepared) mediaPlayer?.currentPosition ?: 0 else resumePosition)
        outState.putBoolean(KEY_PLAYING, playWhenReady)
    }

    override fun onPause() {
        super.onPause()
        val player = mediaPlayer
        playWhenReady = isPrepared && player?.isPlaying == true
        // при повороте продолжим играть в новой Activity, а сейчас — пауза
        if (isPrepared && player?.isPlaying == true) {
            player.pause()
            if (!isChangingConfigurations) statusText.text = "На паузе"
        }
        if (!isChangingConfigurations) playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // обязательно освобождаем плеер, иначе утечка
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
    }

    companion object {
        private const val KEY_POSITION = "position"
        private const val KEY_PLAYING = "playing"
    }
}
