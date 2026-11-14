package com.example.lab7player

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver

class MediaService : Service() {

    companion object {
        const val ACTION_PLAY = "play"
        const val ACTION_PAUSE = "pause"
        const val ACTION_STOP = "stop"
        const val ACTION_NEXT = "next"
        const val ACTION_PREVIOUS = "previous"

        const val BROADCAST_STATE = "com.example.player.STATE_CHANGED"

        private const val CHANNEL_ID = "player_channel"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "MediaService"

        // последнее состояние: Activity читает его при открытии,
        // чтобы сразу показать актуальный трек
        @Volatile var lastState: String = "stopped"
        @Volatile var lastTitle: String = ""
    }

    data class TrackInfo(val resId: Int, val title: String, val artist: String)

    // плейлист задан прямо в коде
    private val playlist = listOf(
        TrackInfo(R.raw.sample_audio_1, "Трек 1", "Лаба 7"),
        TrackInfo(R.raw.sample_audio_2, "Трек 2", "Лаба 7"),
        TrackInfo(R.raw.sample_audio_3, "Трек 3", "Лаба 7")
    )
    private var currentIndex = 0
    private val currentTrack get() = playlist[currentIndex]

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat

    // команды от гарнитуры и кнопок уведомления приходят сюда
    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() = play()
        override fun onPause() = pause()
        override fun onStop() = stopAndDestroy()
        override fun onSkipToNext() = next()
        override fun onSkipToPrevious() = previous()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "Lab7Player").apply {
            setCallback(sessionCallback)
            isActive = true
        }
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            // MediaButtonReceiver запускает сервис через startForegroundService,
            // поэтому сразу показываем уведомление, а потом обрабатываем кнопку
            startForegroundWithNotification()
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            if (mediaPlayer?.isPlaying != true) {
                detachForeground()
                // кнопка пришла, а играть нечего — не держим сервис зря
                if (mediaPlayer == null) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stopAndDestroy()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
        }
        return START_NOT_STICKY
    }

    // ---------- плеер ----------

    // перед загрузкой нового трека освобождаем старый плеер
    private fun loadCurrentTrack() {
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer.create(this, currentTrack.resId)
        if (mediaPlayer == null) {
            Log.e(TAG, "Не удалось загрузить ${currentTrack.title}")
            sendState("error", currentTrack.title)
            return
        }
        mediaPlayer?.setOnCompletionListener {
            // автопереход на следующий трек
            if (currentIndex < playlist.lastIndex) {
                currentIndex++
                loadCurrentTrack()
                play()
            } else {
                currentIndex = 0 // плейлист закончился, в следующий раз начнём сначала
                stopAndDestroy()
            }
        }
        mediaPlayer?.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "Ошибка плеера: $what / $extra")
            sendState("error", currentTrack.title)
            true
        }
        updateMetadata()
    }

    fun play() {
        if (mediaPlayer == null) loadCurrentTrack()
        val player = mediaPlayer ?: return
        player.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startForegroundWithNotification()
        sendState("playing", currentTrack.title)
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) player.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        sendState("paused", currentTrack.title)
        // на паузе сервис больше не foreground, но уведомление с кнопками остаётся
        detachForeground()
        updateNotification()
    }

    fun next() {
        if (currentIndex < playlist.lastIndex) {
            currentIndex++
            loadCurrentTrack()
            play()
        }
    }

    fun previous() {
        if (currentIndex > 0) {
            currentIndex--
            loadCurrentTrack()
            play()
        }
    }

    private fun stopAndDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        sendState("stopped", currentTrack.title)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ---------- MediaSession ----------

    private fun updatePlaybackState(state: Int) {
        val position = try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: IllegalStateException) {
            0L
        }
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(state, position, 1f)
                .build()
        )
    }

    private fun updateMetadata() {
        mediaSession.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTrack.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentTrack.artist)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer?.duration?.toLong() ?: 0L)
                .build()
        )
    }

    // ---------- уведомление ----------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Плеер", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val isPlaying = mediaPlayer?.isPlaying == true
        val statusText = if (isPlaying) "Воспроизводится" else "На паузе"

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // кнопки уведомления работают через тот же MediaButtonReceiver, что и гарнитура
        fun mediaAction(action: Long) =
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, action)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTrack.title)
            .setContentText("${currentTrack.artist} · $statusText")
            .setSubText("${currentIndex + 1} из ${playlist.size}")
            .setSmallIcon(if (isPlaying) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause)
            .setContentIntent(openApp)
            .setDeleteIntent(mediaAction(PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_media_previous, "Назад",
                mediaAction(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Пауза" else "Играть",
                mediaAction(PlaybackStateCompat.ACTION_PLAY_PAUSE)
            )
            .addAction(android.R.drawable.ic_media_next, "Вперёд",
                mediaAction(PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun startForegroundWithNotification() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
    }

    private fun detachForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    }

    private fun updateNotification() {
        val allowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (allowed) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
        }
    }

    // ---------- связь с Activity ----------

    private fun sendState(state: String, title: String = "") {
        lastState = state
        lastTitle = title
        Intent(BROADCAST_STATE).apply {
            putExtra("state", state)
            putExtra("title", title)
            LocalBroadcastManager.getInstance(this@MediaService).sendBroadcast(this)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }
}
