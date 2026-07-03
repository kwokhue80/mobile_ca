package sg.edu.nus.iss.client.dashboard.detail

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.dashboard.ActivityRecordAdapter
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.databinding.FragmentExerciseDaysDetailBinding
import sg.edu.nus.iss.client.databinding.ViewExerciseWeekBarItemBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ExerciseDaysDetailFragment : Fragment() {

    private var _binding: FragmentExerciseDaysDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExerciseDaysViewModel
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var weekActivitiesAdapter: ActivityRecordAdapter
    private lateinit var calendarDayAdapter: CalendarDayAdapter
    private lateinit var weekSummaryAdapter: ExerciseWeekSummaryAdapter
    private lateinit var barBindings: List<ViewExerciseWeekBarItemBinding>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseDaysDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ExerciseDaysViewModel::class.java]
        dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]
        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]

        weekActivitiesAdapter = ActivityRecordAdapter(
            onDeleteClick = { record -> dashboardViewModel.removeRecord(record.id) },
            onItemClick = { record -> RouteManager.toActivityDetail(this, record.id) }
        )
        binding.rvWeekActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWeekActivities.adapter = weekActivitiesAdapter

        calendarDayAdapter = CalendarDayAdapter()
        binding.rvCalendarDays.layoutManager = GridLayoutManager(requireContext(), 7)
        binding.rvCalendarDays.adapter = calendarDayAdapter

        weekSummaryAdapter = ExerciseWeekSummaryAdapter { row -> viewModel.jumpToWeek(row.weekStart) }
        binding.rvWeekSummaries.layoutManager = LinearLayoutManager(requireContext())
        binding.rvWeekSummaries.adapter = weekSummaryAdapter

        setupWeekBars()
        setupWeekdayHeader()

        binding.btnBack.setOnClickListener { RouteManager.back(this) }

        binding.btnTabWeek.setOnClickListener { viewModel.selectMode(ExerciseViewMode.WEEK) }
        binding.btnTabMonth.setOnClickListener { viewModel.selectMode(ExerciseViewMode.MONTH) }

        binding.btnWeekPrev.setOnClickListener { viewModel.goToPreviousWeek() }
        binding.btnWeekNext.setOnClickListener { viewModel.goToNextWeek() }
        binding.btnMonthPrev.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.btnMonthNext.setOnClickListener { viewModel.goToNextMonth() }
        binding.btnWeekPickDate.setOnClickListener { showDatePicker { date -> viewModel.selectWeekDate(date) } }
        binding.btnMonthPickDate.setOnClickListener { showDatePicker { date -> viewModel.selectMonthDate(date) } }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.activityRecords.collect { records -> viewModel.setRecords(records) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userGoalsViewModel.goals.collect { goals ->
                    val goalDays = (goals[ActivityGoalType.EXERCISE_DAYS] ?: ActivityGoalType.EXERCISE_DAYS.defaultValue).toInt()
                    viewModel.setWeeklyGoal(goalDays)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun setupWeekdayHeader() {
        val sundayFirst = listOf(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        )
        sundayFirst.forEach { day ->
            val tv = TextView(requireContext()).apply {
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1)
                textSize = 12f
                setTextColor(0xFF7A7A7A.toInt())
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            binding.calendarWeekdayHeader.addView(tv)
        }
    }

    private fun setupWeekBars() {
        barBindings = (0 until 7).map {
            ViewExerciseWeekBarItemBinding.inflate(layoutInflater, binding.weekBarContainer, true)
        }
    }

    private fun render(state: ExerciseDaysUiState) {
        val isWeek = state.mode == ExerciseViewMode.WEEK
        binding.groupWeek.visibility = if (isWeek) View.VISIBLE else View.GONE
        binding.groupMonth.visibility = if (isWeek) View.GONE else View.VISIBLE
        updateTabStyles(isWeek)

        binding.tvWeekPeriodLabel.text = state.weekPeriodLabel
        binding.tvWeekSummaryBig.text = state.weekSummaryBig
        binding.tvWeekSummarySuffix.text = state.weekSummarySuffix
        binding.tvWeekSummarySubtitle.text = state.weekSummarySubtitle
        binding.btnWeekNext.isEnabled = state.canGoNextWeek
        binding.btnWeekNext.alpha = if (state.canGoNextWeek) 1f else 0.4f

        state.weekBars.forEachIndexed { index, bar ->
            val barBinding = barBindings[index]
            barBinding.tvDayLabel.text = bar.label
            barBinding.barFill.setBackgroundResource(
                if (bar.hasExercise) R.drawable.bg_exercise_bar_done else R.drawable.bg_exercise_bar_rest
            )
            barBinding.ivCheck.visibility = if (bar.hasExercise) View.VISIBLE else View.GONE
            barBinding.barTodayStroke.visibility = if (bar.isToday) View.VISIBLE else View.GONE
        }

        weekActivitiesAdapter.submitList(state.weekActivities)

        binding.tvMonthPeriodLabel.text = state.monthPeriodLabel
        binding.tvMonthSummaryBig.text = state.monthSummaryBig
        binding.tvMonthSummarySuffix.text = state.monthSummarySuffix
        binding.tvMonthSummarySubtitle.text = state.monthSummarySubtitle
        binding.btnMonthNext.isEnabled = state.canGoNextMonth
        binding.btnMonthNext.alpha = if (state.canGoNextMonth) 1f else 0.4f
        calendarDayAdapter.submitList(state.calendarDays)
        weekSummaryAdapter.submitList(state.weekSummaryRows)
    }

    private fun updateTabStyles(isWeek: Boolean) {
        val activeTextColor = 0xFFFFFFFF.toInt()
        val inactiveTextColor = 0xFF1F1F1F.toInt()
        val activeBackground = 0xFF0B57D0.toInt()
        val inactiveBackground = 0xFFEEF1F5.toInt()

        binding.btnTabWeek.setTextColor(if (isWeek) activeTextColor else inactiveTextColor)
        binding.btnTabWeek.backgroundTintList =
            ColorStateList.valueOf(if (isWeek) activeBackground else inactiveBackground)
        binding.btnTabMonth.setTextColor(if (!isWeek) activeTextColor else inactiveTextColor)
        binding.btnTabMonth.backgroundTintList =
            ColorStateList.valueOf(if (!isWeek) activeBackground else inactiveBackground)
    }

    private fun showDatePicker(onDatePicked: (LocalDate) -> Unit) {
        val today = LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth -> onDatePicked(LocalDate.of(year, month + 1, dayOfMonth)) },
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
