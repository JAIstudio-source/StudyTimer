package com.madeby.JAI

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.annotation.RawRes

object AmbientSoundEngine {

    enum class Preset(val displayName: String, @RawRes val rawResId: Int) {
        OFF("Off", 0),
        RAIN("Light Rain", R.raw.light_rain),
        BIRDS("Nature Birds", R.raw.birds),
        LOFI_1("LoFi Beats 1", R.raw.lo_fi_1),
        LOFI_2("LoFi Beats 2", R.raw.lo_fi_2),
        CUSTOM("Custom Audio", 0)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var currentPreset = Preset.OFF
    private var customAudioUri: Uri? = null
    private var currentVolume = 0.5f

    fun getActivePreset(): Preset = currentPreset

    fun setVolume(volume: Float) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }

    fun selectPreset(context: Context, preset: Preset, customUri: Uri? = null) {
        stop()
        currentPreset = preset
        if (customUri != null) {
            customAudioUri = customUri
        }

        if (preset == Preset.OFF) return

        try {
            if (preset == Preset.CUSTOM && customAudioUri != null) {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, customAudioUri!!)
                    isLooping = true
                    setVolume(currentVolume, currentVolume)
                    prepareAsync()
                    setOnPreparedListener { mp -> mp.start() }
                }
            } else if (preset.rawResId != 0) {
                mediaPlayer = MediaPlayer.create(context, preset.rawResId)?.apply {
                    isLooping = true
                    setVolume(currentVolume, currentVolume)
                    start()
                }
            }
        } catch (_: Exception) {
            stop()
        }
    }

    fun play() {
        try {
            if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            }
        } catch (_: Exception) {}
    }

    fun pause() {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        if (currentPreset != Preset.CUSTOM) {
            currentPreset = Preset.OFF
        }
    }
}

