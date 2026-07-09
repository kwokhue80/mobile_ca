// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.detail

import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar

object MetricLineChartConfigurator {

    private const val AXIS_TEXT_COLOR = "#7A7A7A"

    fun configure(
        chart: LineChart,
        bars: List<MetricBar>,
        showGoalLine: Boolean,
        chartGoalValue: Double,
        baseColor: Int,
        goalMetColor: Int,
        selectedBarIndex: Int?,
        onBarSelected: (Int) -> Unit,
        onSelectionCleared: () -> Unit
    ) {
        // Days with no logged data (value 0) are omitted from the line entirely
        // (rather than plotted as a dip to zero), but keep their true index so the
        // remaining points still land in their correct position along the x-axis.
        val entries = bars.mapIndexedNotNull { index, bar ->
            if (bar.value > 0.0) Entry(index.toFloat(), bar.value.toFloat()) else null
        }
        val dataSet = LineDataSet(entries, "").apply {
            color = baseColor
            setDrawCircles(true)
            setCircleColor(baseColor)
            circleRadius = 4f
            setDrawCircleHole(false)
            lineWidth = 2f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = baseColor
            fillAlpha = 40
            highLightColor = baseColor
            highlightLineWidth = 1.5f
        }

        chart.data = LineData(dataSet)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.setScaleEnabled(false)
        chart.setPinchZoom(false)
        chart.isDoubleTapToZoomEnabled = false
        chart.axisRight.isEnabled = false
        chart.setHighlightPerTapEnabled(true)
        chart.setHighlightPerDragEnabled(false)

        chart.axisLeft.setDrawGridLines(false)
        chart.axisLeft.textColor = Color.parseColor(AXIS_TEXT_COLOR)
        chart.axisLeft.removeAllLimitLines()
        // Always start the y-axis at 0 rather than auto-scaling to the data's own
        // minimum - otherwise small real fluctuations (e.g. 50-55kg) get visually
        // exaggerated by a floor that isn't actually zero.
        chart.axisLeft.axisMinimum = 0f
        if (showGoalLine) {
            val limitLine = LimitLine(chartGoalValue.toFloat())
            limitLine.lineColor = goalMetColor
            limitLine.lineWidth = 1.5f
            chart.axisLeft.addLimitLine(limitLine)
            // Always keep the goal line comfortably in view, even when every point
            // (e.g. smoothed 6-month monthly averages) sits well below the goal.
            val dataMax = bars.maxOfOrNull { it.value } ?: 0.0
            chart.axisLeft.axisMaximum = maxOf(dataMax * 1.1, chartGoalValue * 1.2).toFloat()
        } else {
            chart.axisLeft.resetAxisMaximum()
        }

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.textColor = Color.parseColor(AXIS_TEXT_COLOR)
        chart.xAxis.valueFormatter = IndexAxisValueFormatter(bars.map { it.axisLabel })
        // Not forced: bars now always cover the full period (7 days, or the whole
        // month), so an unforced "nice interval" placement lands exactly on every
        // integer index. Forcing an exact count here previously divided the axis
        // range by (count-1), which doesn't land on integers for a 7-wide range and
        // silently dropped labels (e.g. Tue/Wed) instead of showing them.
        chart.xAxis.setLabelCount(bars.size.coerceAtMost(8), false)
        // Always span the full period (e.g. Mon-Sun), even when some days have no
        // data and were dropped from `entries` above - otherwise the chart would
        // auto-scale to only the days with real points, squishing them together.
        chart.xAxis.axisMinimum = -0.5f
        chart.xAxis.axisMaximum = bars.size - 0.5f
        chart.setExtraBottomOffset(6f)

        if (selectedBarIndex != null && bars.getOrNull(selectedBarIndex) != null) {
            chart.highlightValue(selectedBarIndex.toFloat(), 0, false)
        } else {
            chart.highlightValues(null)
        }

        chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val index = e?.x?.toInt() ?: return
                onBarSelected(index)
            }

            override fun onNothingSelected() {
                onSelectionCleared()
            }
        })

        chart.invalidate()
    }
}
