package io.github.rosemoe.sora.app.tests.drawer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

internal data class DrawerDemoTrack(
    val title: String,
    val subtitle: String,
    val fileName: String,
    val url: String
)

internal data class DrawerDemoPlayerState(
    val trackIndex: Int = 0,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val error: String? = null
)

/** Downloads CC0 instrumental tracks to app cache and plays them with Android MediaPlayer. */
internal class DrawerDemoPlayerController(
    context: Context,
    private val onStateChanged: (DrawerDemoPlayerState) -> Unit = {}
) : AutoCloseable {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = LinkedHashSet<(DrawerDemoPlayerState) -> Unit>()
    private val tracks = listOf(
        DrawerDemoTrack("Quiet Room", "Ambient · 68 BPM · AERIS MUSIC", "quiet-room.mp3",
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/quiet-room.mp3"),
        DrawerDemoTrack("Afterglow Vast", "Chill · A minor · AERIS MUSIC", "afterglow-vast.mp3",
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/afterglow-vast.mp3"),
        DrawerDemoTrack("Colder Air", "Ambient · Dark · AERIS MUSIC", "colder-air.mp3",
            "https://raw.githubusercontent.com/farmmer938-lgtm/aeris-cc0-music/main/tracks/colder-air.mp3")
    )

    var state: DrawerDemoPlayerState = DrawerDemoPlayerState()
        private set
    private var player: MediaPlayer? = null
    private var prepared = false
    private var generation = 0
    private var downloadThread: Thread? = null
    private var closed = false

    private val progressTask = object : Runnable {
        override fun run() {
            val current = player ?: return
            if (!prepared || closed) return
            val position = runCatching { current.currentPosition }.getOrDefault(state.positionMs)
            val duration = runCatching { current.duration }.getOrDefault(state.durationMs)
            publish(state.copy(
                positionMs = position.coerceAtLeast(0),
                durationMs = duration.coerceAtLeast(0),
                isPlaying = runCatching { current.isPlaying }.getOrDefault(state.isPlaying)
            ))
            if (state.isPlaying) mainHandler.postDelayed(this, 450L)
        }
    }

    fun currentTrack(): DrawerDemoTrack = tracks[state.trackIndex.coerceIn(tracks.indices)]

    fun addListener(listener: (DrawerDemoPlayerState) -> Unit): AutoCloseable {
        check(!closed) { "Player controller is closed" }
        listeners += listener
        listener(state)
        var removed = false
        return AutoCloseable {
            if (!removed) {
                removed = true
                listeners -= listener
            }
        }
    }

    fun play() {
        if (closed || state.isPreparing) return
        val current = player
        if (prepared && current != null) startPreparedPlayer(current) else loadCurrentTrack()
    }

    fun pause() {
        if (closed || !prepared) return
        val current = player ?: return
        if (runCatching { current.isPlaying }.getOrDefault(false)) {
            runCatching { current.pause() }
            mainHandler.removeCallbacks(progressTask)
            publish(state.copy(isPlaying = false))
        }
    }

    fun togglePlayback() {
        if (closed) return
        val current = player
        if (!prepared || current == null) {
            play()
            return
        }
        if (runCatching { current.isPlaying }.getOrDefault(false)) {
            runCatching { current.pause() }
            mainHandler.removeCallbacks(progressTask)
            publish(state.copy(isPlaying = false))
        } else startPreparedPlayer(current)
    }

    fun nextTrack() {
        if (!closed) selectTrack((state.trackIndex + 1) % tracks.size)
    }

    fun previousTrack() {
        if (closed) return
        if (state.positionMs > 3_000) seekTo(0)
        else selectTrack((state.trackIndex - 1 + tracks.size) % tracks.size)
    }

    fun seekTo(positionMs: Int) {
        val current = player ?: return
        if (!prepared) return
        val target = positionMs.coerceIn(0, state.durationMs.coerceAtLeast(0))
        runCatching { current.seekTo(target) }
        publish(state.copy(positionMs = target))
    }

    private fun selectTrack(index: Int) {
        generation++
        downloadThread?.interrupt()
        downloadThread = null
        runCatching { player?.release() }
        player = null
        prepared = false
        publish(DrawerDemoPlayerState(trackIndex = index, isPreparing = true))
        loadCurrentTrack(alreadyMarkedPreparing = true)
    }

    private fun loadCurrentTrack(alreadyMarkedPreparing: Boolean = false) {
        if (closed) return
        if (!alreadyMarkedPreparing) {
            generation++
            publish(state.copy(
                isPreparing = true,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
                error = null
            ))
        }
        val token = generation
        val track = currentTrack()
        downloadThread = Thread({
            try {
                val file = downloadToCache(track)
                mainHandler.post {
                    if (closed || token != generation) return@post
                    prepareLocalFile(file, token)
                }
            } catch (failure: Exception) {
                mainHandler.post {
                    if (closed || token != generation) return@post
                    publish(state.copy(
                        isPreparing = false,
                        isPlaying = false,
                        error = failure.message ?: "音乐下载失败"
                    ))
                }
            }
        }, "drawer-demo-audio-download").apply {
            isDaemon = true
            start()
        }
    }

    private fun downloadToCache(track: DrawerDemoTrack): File {
        val directory = File(appContext.cacheDir, "drawer-demo-audio")
        if (!directory.exists() && !directory.mkdirs()) error("无法创建播放器缓存目录")
        val target = File(directory, track.fileName)
        if (target.isFile && target.length() > 32_000L) return target
        val temporary = File(directory, track.fileName + ".part")
        val connection = (URL(track.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 25_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "ZeroStudio-Drawer-Api-Lab/1.0")
        }
        try {
            connection.connect()
            check(connection.responseCode in 200..299) {
                "音乐下载失败：HTTP " + connection.responseCode
            }
            val declaredLength = connection.contentLength.toLong()
            check(declaredLength <= MAX_AUDIO_BYTES) { "演示音乐文件过大" }
            var total = 0L
            connection.inputStream.use { input ->
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw InterruptedException("Audio download cancelled")
                        }
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= MAX_AUDIO_BYTES) { "演示音乐文件超过大小限制" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            check(total > 32_000L) { "下载的音乐文件不完整" }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }
            return target
        } finally {
            connection.disconnect()
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun prepareLocalFile(file: File, token: Int) {
        val media = MediaPlayer()
        player = media
        media.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        media.setOnPreparedListener { preparedPlayer ->
            if (closed || token != generation) {
                preparedPlayer.release()
                return@setOnPreparedListener
            }
            prepared = true
            val duration = runCatching { preparedPlayer.duration }.getOrDefault(0)
            publish(state.copy(
                isPreparing = false,
                isPlaying = true,
                positionMs = 0,
                durationMs = duration.coerceAtLeast(0),
                error = null
            ))
            runCatching { preparedPlayer.start() }.onFailure {
                publish(state.copy(isPlaying = false, error = it.message ?: "无法开始播放"))
            }
            mainHandler.removeCallbacks(progressTask)
            mainHandler.post(progressTask)
        }
        media.setOnCompletionListener {
            if (!closed && token == generation) nextTrack()
        }
        media.setOnErrorListener { failedPlayer, what, extra ->
            if (!closed && token == generation) {
                prepared = false
                publish(state.copy(
                    isPreparing = false,
                    isPlaying = false,
                    error = "播放器错误（" + what + "/" + extra + "）"
                ))
                runCatching { failedPlayer.release() }
                if (player === failedPlayer) player = null
            }
            true
        }
        runCatching {
            media.setDataSource(file.absolutePath)
            media.prepareAsync()
        }.onFailure {
            prepared = false
            publish(state.copy(
                isPreparing = false,
                isPlaying = false,
                error = it.message ?: "无法打开演示音乐"
            ))
            runCatching { media.release() }
            if (player === media) player = null
        }
    }

    private fun startPreparedPlayer(current: MediaPlayer) {
        runCatching { current.start() }.onSuccess {
            publish(state.copy(isPlaying = true, error = null))
            mainHandler.removeCallbacks(progressTask)
            mainHandler.post(progressTask)
        }.onFailure {
            publish(state.copy(isPlaying = false, error = it.message ?: "无法开始播放"))
        }
    }

    private fun publish(value: DrawerDemoPlayerState) {
        state = value
        runCatching { onStateChanged(value) }
        listeners.toList().forEach { listener -> runCatching { listener(value) } }
    }

    override fun close() {
        if (closed) return
        closed = true
        generation++
        downloadThread?.interrupt()
        downloadThread = null
        mainHandler.removeCallbacks(progressTask)
        runCatching { player?.release() }
        player = null
        prepared = false
        listeners.clear()
    }

    private companion object {
        const val MAX_AUDIO_BYTES = 15L * 1024L * 1024L
    }
}
