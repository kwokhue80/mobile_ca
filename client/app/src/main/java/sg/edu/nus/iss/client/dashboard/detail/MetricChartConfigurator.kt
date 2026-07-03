package sg.edu.nus.iss.client.dashboard.detail

import android.graphics.Color
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar

object MetricChartConfigurator {

    private const val AXIS_TEXT_COLOR = "#7A7A7A"
    private const val SPARSE_LABEL_THRESHOLD = 12
    private const val TARGET_SPARSE_LABEL_COUNT = 6
    private const val CHART_EXTRA_BOTTOM_OFFSET_DP = 20f

    fun configure(
        chart: BarChart,
        overlay: MetricSelectionOverlayView,
        bars: List<MetricBar>,
        showGoalLine: Boolean,
        chartGoalValue: Double,
        baseColor: Int,
        goalMetColor: Int,
        selectedBarIndex: Int?,
        onBarSelected: (Int) -> Unit,
        onSelectionCleared: () -> Unit
    ) {
        overlay.attachTo(chart)
        overlay.setBars(bars, baseColor, goalMetColor, showGoalLine)

        val barEntries = bars.mapIndexed { index, bar -> BarEntry(index.toFloat(), bar.value.toFloat()) }
        val barDataSet = BarDataSet(barEntries, "").apply {
            setDrawValues(false)
            setColor(Color.TRANSPARENT)
            highLightColor = Color.TRANSPARENT
            highLightAlpha = 0
        }

        chart.data = BarData(barDataSet).apply { barWidth = 0.5f }

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
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.textColor = Color.parseColor(AXIS_TEXT_COLOR)
        chart.axisLeft.removeAllLimitLines()
        if (showGoalLine) {
            val limitLine = LimitLine(chartGoalValue.toFloat())
            limitLine.lineColor = goalMetColor
            limitLine.lineWidth = 1.5f
            chart.axisLeft.addLimitLine(limitLine)
            // Always keep the goal line comfortably in view, even when every bar
            // (e.g. smoothed 6-month monthly averages) sits well below the goal.
            val dataMax = bars.maxOfOrNull { it.value } ?: 0.0
            chart.axisLeft.axisMaximum = maxOf(dataMax * 1.1, chartGoalValue * 1.2).toFloat()
        } else {
            chart.axisLeft.resetAxisMaximum()
        }

        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.granularity = 1f
        chart.xAxis.axisMinimum = -0.5f
        chart.xAxis.axisMaximum = bars.size - 0.5f
        chart.xAxis.setAvoidFirstLastClipping(true)
        chart.xAxis.textColor = Color.parseColor(AXIS_TEXT_COLOR)

        val useSparseLabels = bars.size > SPARSE_LABEL_THRESHOLD
        if (useSparseLabels) {
            // MPAndroidChart's own label-collision logic can't reliably guarantee
            // specific indices (like the very last one) always render, so the
            // labels are drawn manually by the overlay instead.
            chart.xAxis.setDrawLabels(false)
            chart.setExtraBottomOffset(CHART_EXTRA_BOTTOM_OFFSET_DP)
            val lastIndex = bars.size - 1
            val interval = (bars.size / TARGET_SPARSE_LABEL_COUNT).coerceAtLeast(1)
            val labelIndices = (bars.indices.filter { it % interval == 0 } + lastIndex).distinct().sorted()
            overlay.setCustomAxisLabels(labelIndices.map { it to bars[it].axisLabel })
        } else {
            chart.xAxis.setDrawLabels(true)
            chart.setExtraBottomOffset(0f)
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(bars.map { it.axisLabel })
            chart.xAxis.setLabelCount(bars.size, false)
            overlay.setCustomAxisLabels(emptyList())
        }

        if (selectedBarIndex != null && bars.getOrNull(selectedBarIndex) != null) {
            chart.highlightValue(selectedBarIndex.toFloat(), 0, false)
            overlay.showSelection(selectedBarIndex)
        } else {
            chart.highlightValues(null)
            overlay.clearSelection()
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
        overlay.invalidate()
    }
}
