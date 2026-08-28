package com.sameerasw.medrop.ui.effects

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.RequiresApi
import com.sameerasw.medrop.R
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class NfcRippleEffect(view: View) {

    private val viewRef = WeakReference(view)
    private val shader: RuntimeShader

    private var animator: ValueAnimator? = null

    init {
        val shaderCode = loadShaderSource(view.context)
        shader = RuntimeShader(shaderCode)
    }

    private fun loadShaderSource(context: Context): String {
        return context.resources.openRawResource(R.raw.nfc_ripple).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    }

    fun animate(cx: Float, cy: Float, durationSec: Float = 3.2f) {
        val view = viewRef.get() ?: return
        val displayMetrics = view.resources.displayMetrics
        val density = displayMetrics.density

        val width = view.width.toFloat().takeIf { it > 0 } ?: (displayMetrics.widthPixels.toFloat())
        val height = view.height.toFloat().takeIf { it > 0 } ?: (displayMetrics.heightPixels.toFloat())

        val amplitude = 32f * density
        val frequency = 12f
        val decay = 4.5f
        val speed = 1400f * density

        shader.setFloatUniform("uResolution", width, height)
        shader.setFloatUniform("uOrigin", cx, cy)
        shader.setFloatUniform("uAmplitude", amplitude)
        shader.setFloatUniform("uFrequency", frequency)
        shader.setFloatUniform("uDecay", decay)
        shader.setFloatUniform("uSpeed", speed)

        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, durationSec).apply {
            duration = (durationSec * 1000f).toLong()
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val time = anim.animatedValue as Float
                shader.setFloatUniform("uTime", time)
                val effect = RenderEffect.createRuntimeShaderEffect(shader, "inputShader")
                view.setRenderEffect(effect)
                view.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.setRenderEffect(null)
                    view.invalidate()
                }
            })
            start()
        }
    }

    companion object {
        private val RIPPLE_TAG_KEY = R.id.ripple_effect_tag

        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        fun triggerOnView(view: View, cx: Float, cy: Float) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val targetView = view.rootView ?: view
                var effect = targetView.getTag(RIPPLE_TAG_KEY) as? NfcRippleEffect
                if (effect == null) {
                    effect = NfcRippleEffect(targetView)
                    targetView.setTag(RIPPLE_TAG_KEY, effect)
                }
                effect.animate(cx, cy)
            }
        }
    }
}

