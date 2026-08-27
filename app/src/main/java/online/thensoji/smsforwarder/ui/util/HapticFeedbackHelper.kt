package online.thensoji.smsforwarder.ui.util

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticType {
    CLICK,
    TICK,
    SUCCESS,
    ERROR
}

object HapticFeedbackHelper {

    /**
     * Performs crisp haptic feedback using modern Android vibration effects
     * with fallback to View.performHapticFeedback.
     */
    fun performHaptic(context: Context?, view: View? = null, type: HapticType = HapticType.CLICK) {
        val vibrator = getVibrator(context)

        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    when (type) {
                        HapticType.CLICK -> {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            return
                        }
                        HapticType.TICK -> {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                            return
                        }
                        HapticType.SUCCESS -> {
                            val timings = longArrayOf(0, 30, 60, 40)
                            val amplitudes = intArrayOf(0, 180, 0, 255)
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            return
                        }
                        HapticType.ERROR -> {
                            val timings = longArrayOf(0, 50, 60, 50)
                            val amplitudes = intArrayOf(0, 255, 0, 255)
                            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                            return
                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    when (type) {
                        HapticType.CLICK -> vibrator.vibrate(VibrationEffect.createOneShot(20, 180))
                        HapticType.TICK -> vibrator.vibrate(VibrationEffect.createOneShot(10, 120))
                        HapticType.SUCCESS -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40), -1))
                        HapticType.ERROR -> vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50), -1))
                    }
                    return
                } else {
                    @Suppress("DEPRECATION")
                    when (type) {
                        HapticType.CLICK -> vibrator.vibrate(20)
                        HapticType.TICK -> vibrator.vibrate(10)
                        HapticType.SUCCESS -> vibrator.vibrate(longArrayOf(0, 30, 60, 40), -1)
                        HapticType.ERROR -> vibrator.vibrate(longArrayOf(0, 50, 60, 50), -1)
                    }
                    return
                }
            } catch (_: Exception) {
                // Fallback to view-based feedback below
            }
        }

        // View-based fallback
        view?.let { v ->
            val feedbackConstant = when (type) {
                HapticType.CLICK -> HapticFeedbackConstants.KEYBOARD_TAP
                HapticType.TICK -> HapticFeedbackConstants.CLOCK_TICK
                HapticType.SUCCESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.KEYBOARD_TAP
                    }
                }
                HapticType.ERROR -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        HapticFeedbackConstants.REJECT
                    } else {
                        HapticFeedbackConstants.LONG_PRESS
                    }
                }
            }
            v.performHapticFeedback(feedbackConstant)
        }
    }

    private fun getVibrator(context: Context?): Vibrator? {
        if (context == null) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }
}
