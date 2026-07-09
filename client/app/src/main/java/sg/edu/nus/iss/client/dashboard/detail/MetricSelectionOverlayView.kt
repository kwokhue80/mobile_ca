// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.YAxis
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar

class MetricSelectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        private const val BAR_HALF_WIDTH = 0.25f
        private const val BAR_CORNER_RADIUS_DP = 6f
        private const val ZERO_BAR_HEIGHT_DP = 6f
        private const val AXIS_LABEL_TEXT_SIZE_SP = 11f
        private const val AXIS_LABEL_OFFSET_DP = 16f
    }

    private var chart: BarChart? = null
    private var bars: List<MetricBar> = emptyList()
    private var baseColor: Int = Color.GRAY
    private var goalMetColor: Int = Color.LTGRAY
    private var showGoalColor: Boolean = false
    private var selectedIndex: Int? = null
    private var customAxisLabels: List<Pair<Int, String>> = emptyList()

    private val density = context.resources.displayMetrics.density
    private val cornerRadiusPx = BAR_CORNER_RADIUS_DP * density
    private val zeroBarHeightPx = ZERO_BAR_HEIGHT_DP * density
    private val axisLabelOffsetPx = AXIS_LABEL_OFFSET_DP * density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val selectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5A5A5A")
        strokeWidth = 3f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val axisLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A7A7A")
        textSize = AXIS_LABEL_TEXT_SIZE_SP * context.resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }

    fun attachTo(barChart: BarChart) {
        chart = barChart
    }

    fun setBars(newBars: List<MetricBar>, base: Int, goalMet: Int, applyGoalColor: Boolean) {
        bars = newBars
        baseColor = base
        goalMetColor = goalMet
        showGoalColor = applyGoalColor
        invalidate()
    }

    fun setCustomAxisLabels(labels: List<Pair<Int, String>>) {
        customAxisLabels = labels
        invalidate()
    }

    fun showSelection(index: Int) {
        selectedIndex = index
        invalidate()
    }

    fun clearSelection() {
        selectedIndex = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barChart = chart ?: return
        if (bars.isEmpty()) return

        val transformer = barChart.getTransformer(YAxis.AxisDependency.LEFT)
        val zeroY = transformer.getPixelForValues(0f, 0f).y.toFloat()

        bars.forEachIndexed { index, bar ->
            val leftX = transformer.getPixelForValues(index - BAR_HALF_WIDTH, 0f).x.toFloat()
            val rightX = transformer.getPixelForValues(index + BAR_HALF_WIDTH, 0f).x.toFloat()
            val top = if (bar.value > 0.0) {
                transformer.getPixelForValues(index.toFloat(), bar.value.toFloat()).y.toFloat()
            } else {
                zeroY - zeroBarHeightPx
            }

            barPaint.color = if (showGoalColor && bar.meetsGoal) goalMetColor else baseColor
            val rect = RectF(leftX, top, rightX, zeroY)
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, barPaint)
        }

        if (customAxisLabels.isNotEmpty()) {
            val labelY = zeroY + axisLabelOffsetPx
            customAxisLabels.forEach { (index, label) ->
                val x = transformer.getPixelForValues(index.toFloat(), 0f).x.toFloat()
                canvas.drawText(label, x, labelY, axisLabelPaint)
            }
        }

        val selected = selectedIndex ?: return
        val axisMax = barChart.axisLeft.axisMaximum
        val topPoint = transformer.getPixelForValues(selected.toFloat(), axisMax)
        val bottomPoint = transformer.getPixelForValues(selected.toFloat(), 0f)
        canvas.drawLine(
            topPoint.x.toFloat(),
            topPoint.y.toFloat(),
            bottomPoint.x.toFloat(),
            bottomPoint.y.toFloat(),
            selectionLinePaint
        )
    }
}
