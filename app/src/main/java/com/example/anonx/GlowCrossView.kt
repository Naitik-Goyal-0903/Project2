package com.example.anonx

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class GlowCrossView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4FC3FF.toInt()
        strokeWidth = 4f
        setShadowLayer(30f, 0f, 0f, color)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f

        // vertical line
        canvas.drawLine(cx, 0f, cx, height.toFloat(), glowPaint)
        // horizontal line
        canvas.drawLine(0f, cy, width.toFloat(), cy, glowPaint)
    }
}
