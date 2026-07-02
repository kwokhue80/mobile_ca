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
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate

class MetricDetailFragment : Fragment() {

    companion object {
        private const val ARG_METRIC_TYPE = "arg_metric_type"

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

        val factory = MetricDetailViewModelFactory(metricType)
        viewModel = ViewModelProvider(this, factory)[MetricDetailViewModel::class.java]

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
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: MetricDetailUiState) {
        updateTabStyles(state.timeRange)

        binding.tvPeriodLabel.text = state.periodLabel
        binding.btnNextPeriod.isEnabled = state.canGoNext
        binding.btnNextPeriod.alpha = if (state.canGoNext) 1f else 0.4f
        binding.btnPrevPeriod.isEnabled = state.canGoPrevious
        binding.btnPrevPeriod.alpha = if (state.canGoPrevious) 1f else 0.4f

        val selectedBar = state.selectedBarIndex?.let { state.bars.getOrNull(it) }
        if (selectedBar != null) {
            binding.layoutSummarySelected.visibility = View.VISIBLE
            binding.layoutSummaryDefault.visibility = View.GONE
            binding.tvSelectedValue.text = if (state.timeRange == TimeRange.SIX_MONTH) {
                "${metricType.formatValue(selectedBar.value)} ${metricType.unit} per day (avg)"
            } else {
                "${metricType.formatValue(selectedBar.value)} ${metricType.unit}"
            }
            binding.tvSelectedRange.text = selectedBar.rangeLabel
        } else {
            binding.layoutSummarySelected.visibility = View.GONE
            binding.layoutSummaryDefault.visibility = View.VISIBLE
            binding.tvSummaryDefaultValue.text = if (state.timeRange == TimeRange.MONTH || state.timeRange == TimeRange.SIX_MONTH) {
                "${metricType.formatValue(state.totalValue)} ${metricType.unit} per day (avg)"
            } else {
                "${metricType.formatValue(state.totalValue)} of ${metricType.formatValue(state.goalValue)} ${metricType.unit}"
            }
            binding.tvSummarySubtitle.text = state.subtitle
        }

        val hasSummaryRows = state.summaryRows.isNotEmpty()
        binding.rvSummaryRows.visibility = if (hasSummaryRows) View.VISIBLE else View.GONE
        binding.tvSummaryRowsHeader.visibility = if (hasSummaryRows) View.VISIBLE else View.GONE
        summaryRowAdapter.submitList(state.summaryRows)

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
