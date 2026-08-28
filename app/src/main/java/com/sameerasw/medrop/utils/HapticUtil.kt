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
}
