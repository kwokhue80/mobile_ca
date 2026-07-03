package sg.edu.nus.iss.client.dashboard.detail

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentMentalHealthDetailBinding
import sg.edu.nus.iss.client.dashboard.detail.model.MetricBar
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate

class MentalHealthDetailFragment : Fragment() {

    companion object {
        private const val MOOD_COLOR = "#8B61FF"
        private const val MOOD_AXIS_MIN = 1f
        private const val MOOD_AXIS_MAX = 10f

        // 1..5 stays within the red family (dark -> bright red); 5..10 switches to
        // the pink family (deep -> pale pink). 1 = darkest red, 10 = lightest pink.
        private val RED_DARK = Color.parseColor("#8B0000")
        private val RED_LIGHT = Color.parseColor("#FF5252")
        private val PINK_DARK = Color.parseColor("#E64980")
        private val PINK_LIGHT = Color.parseColor("#FFD1E8")

        private fun moodColor(value: Double): Int {
            val clamped = value.coerceIn(1.0, 10.0)
            return if (clamped <= 5.0) {
                blendColor(RED_DARK, RED_LIGHT, ((clamped - 1.0) / 4.0).toFloat())
            } else {
                blendColor(PINK_DARK, PINK_LIGHT, ((clamped - 5.0) / 5.0).toFloat())
            }
        }

        private fun blendColor(from: Int, to: Int, fraction: Float): Int {
            val t = fraction.coerceIn(0f, 1f)
            val a = (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t).toInt()
            val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt()
            val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt()
            val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
            return Color.argb(a, r, g, b)
        }
    }

    private var _binding: FragmentMentalHealthDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MentalHealthDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMentalHealthDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MentalHealthDetailViewModel::class.java]

        binding.btnBack.setOnClickListener { RouteManager.back(this) }

        binding.btnTabDay.setOnClickListener { viewModel.selectTimeRange(TimeRange.DAY) }
        binding.btnTabWeek.setOnClickListener { viewModel.selectTimeRange(TimeRange.WEEK) }
        binding.btnTabMonth.setOnClickListener { viewModel.selectTimeRange(TimeRange.MONTH) }

        binding.btnPrevPeriod.setOnClickListener { viewModel.goToPreviousPeriod() }
        binding.btnNextPeriod.setOnClickListener { viewModel.goToNextPeriod() }
        binding.btnPickDate.setOnClickListener { showDatePicker() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MentalHealthUiState) {
        updateTabStyles(state.timeRange)

        binding.tvPeriodLabel.text = state.periodLabel
        binding.btnNextPeriod.isEnabled = state.canGoNext
        binding.btnNextPeriod.alpha = if (state.canGoNext) 1f else 0.4f

        binding.tvMoodSummary.text = state.summaryText

        val isDay = state.timeRange == TimeRange.DAY
        binding.layoutDayMood.visibility = if (isDay) View.VISIBLE else View.GONE
        binding.chartMoodContainer.visibility = if (isDay) View.GONE else View.VISIBLE
        binding.layoutMoodNodes.visibility = if (isDay) View.GONE else View.VISIBLE

        if (isDay) {
            renderDayMood(state.dayMoodValue)
        } else {
            renderChart(state.points)
            renderMoodNodes(state.moodNodes)
        }
    }

    private fun renderDayMood(value: Double) {
        binding.tvDayMoodEmoji.text = MentalHealthDetailViewModel.moodEmoji(value)
        binding.tvDayMoodWord.text = MentalHealthDetailViewModel.moodCategory(value)
        binding.tvDayMoodValue.text = "%.1f/10".format(value)
    }

    private fun renderChart(points: List<MetricBar>) {
        MetricLineChartConfigurator.configure(
            chart = binding.chartMood,
            bars = points,
            showGoalLine = false,
            chartGoalValue = 0.0,
            baseColor = Color.parseColor(MOOD_COLOR),
            goalMetColor = Color.parseColor(MOOD_COLOR),
            selectedBarIndex = null,
            onBarSelected = {},
            onSelectionCleared = {}
        )
        binding.chartMood.axisLeft.axisMinimum = MOOD_AXIS_MIN
        binding.chartMood.axisLeft.axisMaximum = MOOD_AXIS_MAX

        // Color each point (and the segment leading into it) by its own mood value.
        // MPAndroidChart only walks its multi-color list segment-by-segment in
        // LINEAR mode, so the shared configurator's cubic-bezier curve is swapped
        // out here rather than in the reusable configurator.
        val pointColors = points.map { moodColor(it.value) }
        (binding.chartMood.data?.getDataSetByIndex(0) as? LineDataSet)?.apply {
            mode = LineDataSet.Mode.LINEAR
            setColors(pointColors)
            setCircleColors(pointColors)
            setDrawFilled(false)
        }
        binding.chartMood.invalidate()
    }

    private fun renderMoodNodes(nodes: List<MoodNode>) {
        val slots = listOf(
            Triple(binding.nodeMood1, binding.tvNodeMood1Emoji, binding.tvNodeMood1Label),
            Triple(binding.nodeMood2, binding.tvNodeMood2Emoji, binding.tvNodeMood2Label),
            Triple(binding.nodeMood3, binding.tvNodeMood3Emoji, binding.tvNodeMood3Label)
        )
        slots.forEachIndexed { index, (container, emojiView, labelView) ->
            val node = nodes.getOrNull(index)
            container.visibility = if (node != null) View.VISIBLE else View.GONE
            if (node != null) {
                emojiView.text = MentalHealthDetailViewModel.moodEmoji(node.value)
                labelView.text = node.label
            }
        }
    }

    private fun updateTabStyles(activeRange: TimeRange) {
        val activeTextColor = 0xFFFFFFFF.toInt()
        val inactiveTextColor = 0xFF1F1F1F.toInt()
        val activeBackground = 0xFF0B57D0.toInt()
        val inactiveBackground = 0xFFEEF1F5.toInt()

        val tabButtons = listOf(
            binding.btnTabDay to TimeRange.DAY,
            binding.btnTabWeek to TimeRange.WEEK,
            binding.btnTabMonth to TimeRange.MONTH
        )
        tabButtons.forEach { (button, range) ->
            val isActive = range == activeRange
            button.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
            button.backgroundTintList =
                ColorStateList.valueOf(if (isActive) activeBackground else inactiveBackground)
        }
    }

    private fun showDatePicker() {
        val today = LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                viewModel.jumpToDate(LocalDate.of(year, month + 1, dayOfMonth))
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
