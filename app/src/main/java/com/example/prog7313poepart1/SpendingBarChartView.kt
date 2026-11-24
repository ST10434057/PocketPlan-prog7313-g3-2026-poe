package com.example.prog7313poepart1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class SpendingBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    data class BarItem(
        val label: String,
        val amount: Double
    )

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(245, 210, 50)
    }
    private val altBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(30, 47, 77)
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(210, 210, 210)
        strokeWidth = 2f
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 40, 40)
        textAlign = Paint.Align.CENTER
        textSize = 24f
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 0, 0)
        textAlign = Paint.Align.CENTER
        textSize = 24f
        isFakeBoldText = true
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 100, 100)
        textAlign = Paint.Align.CENTER
        textSize = 30f
    }
    private val minGoalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(46, 125, 50)
        strokeWidth = 4f
    }
    private val maxGoalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(198, 40, 40)
        strokeWidth = 4f
    }

    private var bars: List<BarItem> = emptyList()
    private var minGoal: Double? = null
    private var maxGoal: Double? = null

    fun setData(items: List<BarItem>, minimumGoal: Double?, maximumGoal: Double?) {
        bars = items.filter { it.amount > 0.0 }
        minGoal = minimumGoal?.takeIf { it > 0.0 }
        maxGoal = maximumGoal?.takeIf { it > 0.0 }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (bars.isEmpty()) {
            canvas.drawText("No spending data for this period", width / 2f, height / 2f, emptyPaint)
            return
        }

        val left = 56f
        val top = 36f
        val right = width - 32f
        val bottom = height - 64f
        val chartHeight = bottom - top
        val chartWidth = right - left

        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        val largestBar = bars.maxOfOrNull { it.amount } ?: 0.0
        val largestGoal = listOfNotNull(minGoal, maxGoal).maxOrNull() ?: 0.0
        val scaleMax = max(largestBar, largestGoal).coerceAtLeast(1.0)

        drawGoalLine(canvas, minGoal, scaleMax, top, bottom, left, right, minGoalPaint, "Min")
        drawGoalLine(canvas, maxGoal, scaleMax, top, bottom, left, right, maxGoalPaint, "Max")

        val slot = chartWidth / bars.size
        val barWidth = (slot * 0.58f).coerceAtMost(82f)

        bars.forEachIndexed { index, item ->
            val centerX = left + slot * index + slot / 2f
            val barHeight = (item.amount / scaleMax * chartHeight).toFloat()
            val rect = RectF(
                centerX - barWidth / 2f,
                bottom - barHeight,
                centerX + barWidth / 2f,
                bottom
            )

            canvas.drawRoundRect(rect, 12f, 12f, if (index % 2 == 0) barPaint else altBarPaint)
            canvas.drawText("R %.0f".format(item.amount), centerX, rect.top - 10f, valuePaint)
            canvas.drawText(item.label.take(10), centerX, height - 24f, labelPaint)
        }
    }

    private fun drawGoalLine(
        canvas: Canvas,
        goal: Double?,
        scaleMax: Double,
        top: Float,
        bottom: Float,
        left: Float,
        right: Float,
        paint: Paint,
        label: String
    ) {
        if (goal == null) {
            return
        }

        val y = bottom - (goal / scaleMax * (bottom - top)).toFloat()
        canvas.drawLine(left, y, right, y, paint)
        canvas.drawText("$label R %.0f".format(goal), right - 52f, y - 8f, valuePaint)
    }
}
