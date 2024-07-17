package com.example.personademo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CircularOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint()

    init {
        paint.color = Color.BLACK
        paint.alpha = 150
        paint.style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        // Draw the circular mask
        val radius = Math.min(width, height) / 2
        canvas.drawOval(
            (width / 2) - radius, (height / 2) - radius,
            (width / 2) + radius, (height / 2) + radius,
            paint
        )
    }
}


