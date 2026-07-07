package sg.edu.nus.iss.client.dashboard.detail

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentMetricDetailBinding
import sg.edu.nus.iss.client.dashboard.detail.model.MetricDetailUiState
import sg.edu.nus.iss.client.dashboard.detail.model.MetricType
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate
import kotlin.math.roundToInt

private fun MetricType.toActivityGoalTypeOrNull(): ActivityGoalType? = when (this) {
    MetricType.DISTANCE -> ActivityGoalType.DISTANCE
    MetricType.CALORIES -> ActivityGoalType.CALORIES
    MetricType.FOOD_INTAKE -> null
    MetricType.SLEEP -> ActivityGoalType.SLEEP
    MetricType.HYDRATION -> ActivityGoalType.HYDRATION
    MetricType.WEIGHT -> ActivityGoalType.WEIGHT
}

class MetricDetailFragment : Fragment() {

    companion object {
        private const val ARG_METRIC_TYPE = "arg_metric_type"
        private const val BMI_HEIGHT_METERS = 1.70

        fun newInstance(metricType: MetricType): MetricDetailFragment {
            val fragment = MetricDetailFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_METRIC_TYPE, metricType.name)
            }
            return fragment
        }
    }

    private var _binding: FragmentMetricDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var metricType: MetricType
    private lateinit var viewModel: MetricDetailViewModel
    private lateinit var summaryRowAdapter: MetricSummaryRowAdapter
    private var currentGoalValue: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMetricDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        metricType = MetricType.valueOf(requireArguments().getString(ARG_METRIC_TYPE)!!)
        summaryRowAdapter = MetricSummaryRowAdapter(metricType)
        currentGoalValue = metricType.defaultGoal

        val factory = MetricDetailViewModelFactory(requireActivity().application, metricType)
        viewModel = ViewModelProvider(this, factory)[MetricDetailViewModel::class.java]
        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]

        binding.tvMetricTitle.text = metricType.displayName

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.rvSummaryRows.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSummaryRows.adapter = summaryRowAdapter
        binding.rvSummaryRows.isNestedScrollingEnabled = false

        binding.btnTabDay.setOnClickListener { viewModel.selectTimeRange(TimeRange.DAY) }
        binding.btnTabWeek.setOnClickListener { viewModel.selectTimeRange(TimeRange.WEEK) }
        binding.btnTabMonth.setOnClickListener { viewModel.selectTimeRange(TimeRange.MONTH) }
        binding.btnTabSixMonth.setOnClickListener { viewModel.selectTimeRange(TimeRange.SIX_MONTH) }

        binding.btnPrevPeriod.setOnClickListener { viewModel.goToPreviousPeriod() }
        binding.btnNextPeriod.setOnClickListener { viewModel.goToNextPeriod() }
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnClearSelection.setOnClickListener { viewModel.clearSelection() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userGoalsViewModel.goals.collect { goals ->
                    val activityGoalType = metricType.toActivityGoalTypeOrNull()
                    currentGoalValue = activityGoalType?.let { goals[it] } ?: metricType.defaultGoal
                    viewModel.setGoal(currentGoalValue)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }

        if (metricType == MetricType.SLEEP) {
            binding.tvSleepQuality.visibility = View.VISIBLE
        }
    }

    // Sleep quality (1-10, real backend data) is now part of the ViewModel's per-day
    // summary fetch (see MetricDetailUiState.sleepQualityScore), averaged over whatever
    // date range is currently being viewed.
    private fun renderSleepQuality(quality: Double?) {
        binding.tvSleepQuality.text = if (quality != null) {
            "${sleepQualityEmoji(quality)} Sleep quality: ${"%.1f".format(quality)}/5 (1=poor, 5=excellent)"
        } else {
            "Sleep quality: no data yet"
        }
    }

    private fun sleepQualityEmoji(quality: Double): String = when (quality.roundToInt().coerceIn(1, 5)) {
        1 -> "😞"
        2 -> "😕"
        3 -> "😐"
        4 -> "🙂"
        else -> "😄"
    }

    private fun render(state: MetricDetailUiState) {
        updateTabStyles(state.timeRange)

        if (metricType == MetricType.SLEEP) {
            renderSleepQuality(state.sleepQualityScore)
        }

        binding.tvPeriodLabel.text = state.periodLabel
        binding.btnNextPeriod.isEnabled = state.canGoNext
        binding.btnNextPeriod.alpha = if (state.canGoNext) 1f else 0.4f
        binding.btnPrevPeriod.isEnabled = state.canGoPrevious
        binding.btnPrevPeriod.alpha = if (state.canGoPrevious) 1f else 0.4f

        val isWeight = metricType == MetricType.WEIGHT
        val isSleepDay = metricType == MetricType.SLEEP && state.timeRange == TimeRange.DAY
        // Food Intake has no hourly breakdown (see MetricDetailViewModel.selectHourlyValue),
        // so its Day view is a single total value with no chart, same as Sleep's Day view.
        val isFoodIntakeDay = metricType == MetricType.FOOD_INTAKE && state.timeRange == TimeRange.DAY
        val isSingleValueDay = isSleepDay || isFoodIntakeDay

        val selectedBar = state.selectedBarIndex?.let { state.bars.getOrNull(it) }
        if (selectedBar != null) {
            binding.layoutSummarySelected.visibility = View.VISIBLE
            binding.layoutSummaryDefault.visibility = View.GONE
            binding.tvSelectedValue.text = if (isWeight) {
                val avgSuffix = if (state.timeRange == TimeRange.DAY) "" else "(avg)"
                "${metricType.formatValue(selectedBar.value)}${metricType.unit}$avgSuffix"
            } else if (state.timeRange == TimeRange.SIX_MONTH) {
                "${metricType.formatValue(selectedBar.value)} ${metricType.unit} per day (avg)"
            } else {
                "${metricType.formatValue(selectedBar.value)} ${metricType.unit}"
            }
            binding.tvSelectedRange.text = selectedBar.rangeLabel
        } else {
            binding.layoutSummarySelected.visibility = View.GONE
            binding.layoutSummaryDefault.visibility = View.VISIBLE
            val currentWeight = state.bars.lastOrNull { it.value > 0 }?.value ?: state.totalValue
            binding.tvSummaryDefaultValue.text = if (isSleepDay) {
                "You slept ${metricType.formatValue(state.totalValue)}${metricType.unit} today."
            } else if (isFoodIntakeDay) {
                "You consumed ${metricType.formatValue(state.totalValue)} ${metricType.unit} today."
            } else if (isWeight) {
                val avgSuffix = if (state.timeRange == TimeRange.DAY) "" else "(avg)"
                "${metricType.formatValue(currentWeight)}${metricType.unit}$avgSuffix"
            } else if (state.timeRange == TimeRange.MONTH || state.timeRange == TimeRange.SIX_MONTH) {
                "${metricType.formatValue(state.totalValue)} ${metricType.unit} per day (avg)"
            } else {
                "${metricType.formatValue(state.totalValue)} of ${metricType.formatValue(state.goalValue)} ${metricType.unit}"
            }
            binding.tvSummarySubtitle.text = state.subtitle
            binding.tvSummarySubtitle.visibility = if (isWeight || isSingleValueDay) View.GONE else View.VISIBLE
        }

        val hasSummaryRows = state.summaryRows.isNotEmpty()
        binding.rvSummaryRows.visibility = if (hasSummaryRows) View.VISIBLE else View.GONE
        binding.tvSummaryRowsHeader.visibility = if (hasSummaryRows) View.VISIBLE else View.GONE
        binding.tvSummaryRowsHeader.text = if (isWeight) "Weight (avg)" else "Daily average"
        summaryRowAdapter.submitList(state.summaryRows)

        val isWeightDay = isWeight && state.timeRange == TimeRange.DAY
        binding.tvDailyGoal.visibility = View.VISIBLE
        binding.tvDailyGoal.text = if (isWeight) {
            "Goal: ${metricType.formatValue(currentGoalValue)} ${metricType.unit}"
        } else {
            "Daily goal: ${metricType.formatValue(currentGoalValue)} ${metricType.unit}"
        }
        binding.chartContainer.visibility = if (isWeightDay || isSingleValueDay) View.GONE else View.VISIBLE
        binding.chartMetric.visibility = if (isWeight || isSingleValueDay) View.GONE else View.VISIBLE
        binding.chartSelectionOverlay.visibility = if (isWeight || isSingleValueDay) View.GONE else View.VISIBLE
        binding.chartMetricLine.visibility = if (isWeight) View.VISIBLE else View.GONE
        binding.cardBmi.visibility = if (isWeight) View.VISIBLE else View.GONE

        if (isWeight) {
            val currentWeight = selectedBar?.value
                ?: state.bars.lastOrNull { it.value > 0 }?.value
                ?: state.totalValue
            val bmiValue = currentWeight / (BMI_HEIGHT_METERS * BMI_HEIGHT_METERS)
            binding.tvBmiValue.text = "${"%.1f".format(bmiValue)}, ${bmiCategory(bmiValue)}"
            binding.bmiGauge.setBmi(bmiValue.toFloat())
        }

        if (isWeightDay || isSingleValueDay) {
            // No chart for Weight's/Sleep's/Food Intake's Day view; the text summary above is enough.
        } else if (isWeight) {
            MetricLineChartConfigurator.configure(
                chart = binding.chartMetricLine,
                bars = state.bars,
                showGoalLine = state.timeRange != TimeRange.DAY,
                chartGoalValue = state.chartGoalValue,
                baseColor = metricType.chartColor,
                goalMetColor = metricType.chartGoalMetColor,
                selectedBarIndex = state.selectedBarIndex,
                onBarSelected = { index -> viewModel.selectBar(index) },
                onSelectionCleared = { viewModel.clearSelection() }
            )
        } else {
            MetricChartConfigurator.configure(
                chart = binding.chartMetric,
                overlay = binding.chartSelectionOverlay,
                bars = state.bars,
                showGoalLine = state.timeRange != TimeRange.DAY,
                chartGoalValue = state.chartGoalValue,
                baseColor = metricType.chartColor,
                goalMetColor = metricType.chartGoalMetColor,
                selectedBarIndex = state.selectedBarIndex,
                onBarSelected = { index -> viewModel.selectBar(index) },
                onSelectionCleared = { viewModel.clearSelection() }
            )
        }
    }

    private fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal Weight"
        bmi < 30.0 -> "Overweight"
        bmi < 35.0 -> "Obese"
        bmi < 40.0 -> "Severely Obese"
        else -> "Morbidly Obese"
    }

    private fun updateTabStyles(activeRange: TimeRange) {
        val activeTextColor = 0xFFFFFFFF.toInt()
        val inactiveTextColor = 0xFF1F1F1F.toInt()
        val activeBackground = 0xFF0B57D0.toInt()
        val inactiveBackground = 0xFFEEF1F5.toInt()

        val tabButtons = listOf(
            binding.btnTabDay to TimeRange.DAY,
            binding.btnTabWeek to TimeRange.WEEK,
            binding.btnTabMonth to TimeRange.MONTH,
            binding.btnTabSixMonth to TimeRange.SIX_MONTH
        )
        tabButtons.forEach { (button, range) ->
            val isActive = range == activeRange
            button.setTextColor(if (isActive) activeTextColor else inactiveTextColor)
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(if (isActive) activeBackground else inactiveBackground)
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
