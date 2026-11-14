package com.example.amdbalance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** 环形图：已用(灰红) vs 剩余(绿) */
class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(148, 163, 184)
        textAlign = Paint.Align.CENTER
    }

    private var used = 0.0
    private var remaining = 0.0
    private val rect = RectF()
    private val strokeWidth = 28f

    fun setData(used: Double, remaining: Double) {
        this.used = used
        this.remaining = remaining
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val r = minOf(w, h) / 2f - strokeWidth / 2f - 4f
        rect.set(cx - r, cy - r, cx + r, cy + r)

        arcPaint.strokeWidth = strokeWidth
        val total = used + remaining

        // 背景环
        arcPaint.color = Color.rgb(51, 65, 85)
        canvas.drawArc(rect, -90f, 360f, false, arcPaint)

        if (total > 0) {
            // 已用（橙色）
            val usedFraction = (used / total).toFloat()
            if (used > 0) {
                arcPaint.color = Color.rgb(251, 146, 60)
                canvas.drawArc(rect, -90f, usedFraction * 360f, false, arcPaint)
            }
            // 剩余（绿色）
            if (remaining > 0) {
                arcPaint.color = Color.rgb(74, 222, 128)
                canvas.drawArc(rect, -90f + usedFraction * 360f, (1f - usedFraction) * 360f, false, arcPaint)
            }
        }

        // 中心文字：剩余百分比
        val pct = if (total > 0) remaining / total * 100 else 0.0
        centerPaint.textSize = dp(22f)
        canvas.drawText("${pct.toInt()}%", cx, cy + dp(8f), centerPaint)
        subPaint.textSize = dp(12f)
        canvas.drawText("剩余", cx, cy + dp(24f), subPaint)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}