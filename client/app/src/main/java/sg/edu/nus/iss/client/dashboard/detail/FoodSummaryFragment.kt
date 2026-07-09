// Author: HuaYuan Xie
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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.dashboard.detail.model.TimeRange
import sg.edu.nus.iss.client.databinding.FragmentFoodSummaryBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate

/** Food Summary detail screen. D/W/M views only: Day is a plain list of that day's
 *  meals (meal type, food name, kcal - no chart/axis), Week and Month are per-day
 *  kcal bar charts where tapping a bar lists that day's meals below the chart. */
class FoodSummaryFragment : Fragment() {

    companion object {
        // Matches the Food Summary card's teal accent on the dashboard.
        private val CHART_COLOR = Color.parseColor("#27837B")
    }

    private var _binding: FragmentFoodSummaryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FoodSummaryViewModel
    private val dayMealsAdapter = FoodLogAdapter()
    private val selectedMealsAdapter = FoodLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[FoodSummaryViewModel::class.java]

        binding.btnBack.setOnClickListener { RouteManager.back(this) }

        binding.rvDayMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDayMeals.adapter = dayMealsAdapter
        binding.rvSelectedMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSelectedMeals.adapter = selectedMealsAdapter

        binding.btnTabDay.setOnClickListener { viewModel.selectTimeRange(TimeRange.DAY) }
        binding.btnTabWeek.setOnClickListener { viewModel.selectTimeRange(TimeRange.WEEK) }
        binding.btnTabMonth.setOnClickListener { viewModel.selectTimeRange(TimeRange.MONTH) }

        binding.btnPrevPeriod.setOnClickListener { viewModel.goToPreviousPeriod() }
        binding.btnNextPeriod.setOnClickListener { viewModel.goToNextPeriod() }
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnClearSelection.setOnClickListener { viewModel.clearSelection() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: FoodSummaryUiState) {
        updateTabStyles(state.timeRange)

        binding.tvPeriodLabel.text = state.periodLabel
        binding.btnNextPeriod.isEnabled = state.canGoNext
        binding.btnNextPeriod.alpha = if (state.canGoNext) 1f else 0.4f

        val isDay = state.timeRange == TimeRange.DAY
        binding.layoutDay.visibility = if (isDay) View.VISIBLE else View.GONE
        binding.layoutChart.visibility = if (isDay) View.GONE else View.VISIBLE

        if (isDay) {
            binding.tvDayTotal.text = "%,d kcal".format(state.dayTotalKcal)
            dayMealsAdapter.submitList(state.dayMeals)
            binding.tvDayEmpty.visibility = if (state.dayMeals.isEmpty()) View.VISIBLE else View.GONE
            return
        }

        MetricChartConfigurator.configure(
            chart = binding.chartFood,
            overlay = binding.chartSelectionOverlay,
            bars = state.bars,
            showGoalLine = false,
            chartGoalValue = 0.0,
            baseColor = CHART_COLOR,
            goalMetColor = CHART_COLOR,
            selectedBarIndex = state.selectedBarIndex,
            onBarSelected = { index -> viewModel.selectBar(index) },
            onSelectionCleared = { viewModel.clearSelection() }
        )

        val hasSelection = state.selectedBarIndex != null
        binding.layoutSelectedDay.visibility = if (hasSelection) View.VISIBLE else View.GONE
        binding.tvChartHint.visibility = if (hasSelection) View.GONE else View.VISIBLE
        if (hasSelection) {
            binding.tvSelectedDate.text = state.selectedDateLabel
            selectedMealsAdapter.submitList(state.selectedMeals)
            binding.tvSelectedEmpty.visibility =
                if (state.selectedMeals.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
