package com.example.anonx

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class CircleWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF5FA8FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
    }

    private var phase = 0f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerY = height / 2f
        val radius = 65f
        val spacing = 120f

        var x = -spacing

        while (x < width + spacing) {
            var angle = 0.0
            while (angle <= Math.PI * 2) {
                val cx = x + radius * cos(angle).toFloat()
                val cy = centerY + radius * sin(angle + phase).toFloat()
                canvas.drawCircle(cx, cy, 2.2f, paint)
                angle += 0.15
            }
            x += spacing
        }

        phase += 0.04f
        invalidate()
    }
}
