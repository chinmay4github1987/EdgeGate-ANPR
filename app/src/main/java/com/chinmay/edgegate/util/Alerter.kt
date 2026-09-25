package com.chinmay.edgegate.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator

/** Loud, unmissable feedback for a guard who may not be looking at the screen. */
class Alerter(context: Context, private val settings: Settings) {
    private val vibrator = context.getSystemService(Vibrator::class.java)
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 100) }.getOrNull()

    fun alarm() {
        if (!settings.current.alarmOnDeny) return
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 4_000)
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 300), -1))
    }

    fun ok() {
        if (!settings.current.beepOnAllow) return
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
    }

    /** "Silence alarm" button on the deny sheet. */
    fun silence() {
        tone?.stopTone()
        vibrator?.cancel()
    }
}
