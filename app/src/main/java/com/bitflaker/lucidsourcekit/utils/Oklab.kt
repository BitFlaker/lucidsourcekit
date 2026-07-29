package com.bitflaker.lucidsourcekit.utils

import androidx.annotation.ColorInt
import android.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Implementation of some Oklab color utils.
 * The raw values used in the conversion are defined in the blogpost linked below.
 *
 * [https://bottosson.github.io/posts/oklab](https://bottosson.github.io/posts/oklab/#converting-from-linear-srgb-to-oklab)
 *
 * [https://bottosson.github.io/posts/colorwrong](https://bottosson.github.io/posts/colorwrong/#what-can-we-do%3F)
 **/
class Oklab(var l: Float, var a: Float, var b: Float) {
    companion object {
        fun mix(@ColorInt a: Int, @ColorInt b: Int, t: Float): Int {
            val lab1 = fromRGB(a)
            val lab2 = fromRGB(b)
            return Oklab(
                lab1.l + (lab2.l - lab1.l) * t,
                lab1.a + (lab2.a - lab1.a) * t,
                lab1.b + (lab2.b - lab1.b) * t
            ).toLinearSRGB()
        }

        fun onColor(@ColorInt color: Int): Int {
            val lab = fromRGB(color)
            val onLab = if (lab.l > 0.55f) Oklab(0.30f, lab.a * 1.1f, lab.b * 1.1f) else Oklab(0.97f, lab.a * 0.15f, lab.b * 0.15f)
            return onLab.toLinearSRGB()
        }

        private fun fromRGB(c: Int): Oklab {
            val r = toLinear(Color.red(c) / 255f)
            val g = toLinear(Color.green(c) / 255f)
            val b = toLinear(Color.blue(c) / 255f)

            val l0 = cbrt(0.41222146f * r + 0.53633255f * g + 0.051445995f * b)
            val m0 = cbrt(0.2119035f * r + 0.6806995f * g + 0.10739696f * b)
            val s0 = cbrt(0.08830246f * r + 0.28171885f * g + 0.6299787f * b)

            return Oklab(
                0.21045426f * l0 + 0.7936178f * m0 - 0.004072047f * s0,
                1.9779985f * l0 - 2.4285922f * m0 + 0.4505937f * s0,
                0.025904037f * l0 + 0.78277177f * m0 - 0.80867577f * s0
            )
        }

        private fun toLinear(x: Float) = if (x >= 0.04045f) ((x + 0.055f) / (1f + 0.055f)).pow(2.4f) else  x / 12.92f
        private fun fromLinear(x: Float) = if (x >= 0.0031308f) 1.055f * x.pow(1f / 2.4f) - 0.055f else 12.92f * x
        private fun channel(value: Float): Int = (fromLinear(value.coerceIn(0f, 1f)) * 255f).roundToInt().coerceIn(0, 255)
    }

    fun toLinearSRGB(): Int {
        val l0 = (l + 0.39633778f * a + 0.21580376f * b).pow(3)
        val m0 = (l - 0.105561346f * a - 0.06385417f * b).pow(3)
        val s0 = (l - 0.08948418f * a - 1.2914855f * b).pow(3)

        return Color.rgb(
            channel(4.0767417f * l0 - 3.3077116f * m0 + 0.23096994f * s0),
            channel(-1.268438f * l0 + 2.6097574f * m0 - 0.34131938f * s0),
            channel(-0.0041960864f * l0 - 0.7034186f * m0 + 1.7076147f * s0)
        )
    }
}