package sg.edu.nus.iss.client.dashboard.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Draws a 6-segment BMI scale (Underweight..Morbidly Obese) with a pin marker
 * over the segment the current BMI falls into. Segments are drawn at equal
 * width regardless of their underlying kg/m^2 span, matching standard BMI
 * gauge widgets; the marker is interpolated within its own segment's range.
 */
class BmiGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Segment(val boundaryLabel: String?, val color: Int)

    private val segments = listOf(
        Segment(null, Color.parseColor("#4FC3F7")),
        Segment("18.5", Color.parseColor("#66D18B")),
        Segment("25.0", Color.parseColor("#FFD54F")),
        Segment("30.0", Color.parseColor("#FFA75E")),
        Segment("35.0", Color.parseColor("#FF7A7A")),
        Segment("40.0", Color.parseColor("#F0568C"))
    )

    private val boundaries = floatArrayOf(18.5f, 25.0f, 30.0f, 35.0f, 40.0f)

    private var bmi: Float = 22f

    private val density = context.resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A7A7A")
        textSize = dp(11f)
        textAlign = Paint.Align.CENTER
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setBmi(value: Float) {
        bmi = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        if (w <= 0f) return

        val barHeight = dp(14f)
        val barTop = dp(28f)
        val barBottom = barTop + barHeight
        val segmentWidth = w / segments.size
        val cornerRadius = barHeight / 2f
        val gap = dp(2f)

        segments.forEachIndexed { index, segment ->
            val left = index * segmentWidth + if (index == 0) 0f else gap / 2f
            val right = (index + 1) * segmentWidth - if (index == segments.lastIndex) 0f else gap / 2f
            barPaint.color = segment.color
            val rect = RectF(left, barTop, right, barBottom)
            val isFirst = index == 0
            val isLast = index == segments.lastIndex
            val radii = floatArrayOf(
                if (isFirst) cornerRadius else 0f, if (isFirst) cornerRadius else 0f,
                if (isLast) cornerRadius else 0f, if (isLast) cornerRadius else 0f,
                if (isLast) cornerRadius else 0f, if (isLast) cornerRadius else 0f,
                if (isFirst) cornerRadius else 0f, if (isFirst) cornerRadius else 0f
            )
            val path = Path().apply { addRoundRect(rect, radii, Path.Direction.CW) }
            canvas.drawPath(path, barPaint)

            segment.boundaryLabel?.let {
                canvas.drawText(it, right, barBottom + dp(18f), labelPaint)
            }
        }

        val segmentIndex = when {
            bmi < boundaries[0] -> 0
            bmi < boundaries[1] -> 1
            bmi < boundaries[2] -> 2
            bmi < boundaries[3] -> 3
            bmi < boundaries[4] -> 4
            else -> 5
        }
        val segStart = if (segmentIndex == 0) boundaries[0] - 5f else boundaries[segmentIndex - 1]
        val segEnd = if (segmentIndex == segments.lastIndex) boundaries.last() + 5f else boundaries[segmentIndex]
        val fraction = ((bmi - segStart) / (segEnd - segStart)).coerceIn(0f, 1f)
        val markerX = (segmentIndex + fraction) * segmentWidth

        markerPaint.color = Color.BLACK
        val pinRadius = dp(9f)
        val pinCenterY = barTop - dp(14f)
        canvas.drawCircle(markerX, pinCenterY, pinRadius, markerPaint)
        val pinPath = Path().apply {
            moveTo(markerX - dp(6f), pinCenterY + dp(6f))
            lineTo(markerX + dp(6f), pinCenterY + dp(6f))
            lineTo(markerX, barTop - dp(1f))
            close()
        }
        canvas.drawPath(pinPath, markerPaint)
    }
}
