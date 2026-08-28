package com.sameerasw.medrop.utils

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.mutableStateOf

object HapticUtil {
    val isAppHapticsEnabled = mutableStateOf(true)

    fun performUIHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun performLightHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun performVirtualKeyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performHeavyHaptic(view: View) {
        if (!isAppHapticsEnabled.value) return
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun performCustomHaptic(
        view: View,
        strength: Float,
    ) {
        if (!isAppHapticsEnabled.value) return

        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    view.context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                view.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)) {
                    val effect =
                        VibrationEffect
                            .startComposition()
                            .addPrimitive(
                                VibrationEffect.Composition.PRIMITIVE_CLICK,
                                strength,
                            ).compose()

                    val attrs =
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                    vibrator.vibrate(effect, attrs)
                    return
                }
            } catch (_: Exception) {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasAmplitudeControl = vibrator.hasAmplitudeControl()
            if (hasAmplitudeControl) {
                val amplitude = (strength * strength * 255).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(12, amplitude)
                vibrator.vibrate(effect)
            } else {
                if (strength < 0.5f) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun startScanRumble(context: Context) {
        if (!isAppHapticsEnabled.value) return

        try {
            val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Ramp up in 400ms then loop on max strength
                val stepCount = 8
                val stepDuration = 50L
                val timings = LongArray(stepCount + 1) { if (it < stepCount) stepDuration else 200L }
                val amplitudes = IntArray(stepCount + 1) { index ->
                    if (index < stepCount) {
                        val progress = (index + 1).toFloat() / stepCount.toFloat()
                        (progress * 255).toInt().coerceIn(60, 255)
                    } else {
                        255
                    }
                }
                if (vibrator.hasAmplitudeControl()) {
                    // repeat from the max intensity index (stepCount)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, stepCount)
                    val attrs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                    } else null
                    if (attrs != null) {
                        vibrator.vibrate(effect, attrs)
                    } else {
                        vibrator.vibrate(effect)
                    }
                    return
                }
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000), 0)
            }
        } catch (_: Exception) {}
    }

    fun stopScanRumble(context: Context) {
        try {
            val vibrator =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
            vibrator.cancel()
        } catch (_: Exception) {}
    }

    fun performScanRumbleHaptic(context: Context, durationMs: Long = 500L) {
        startScanRumble(context)
    }
}
