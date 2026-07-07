package sg.edu.nus.iss.client.dashboard.activity

import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.dashboard.util.ActivityDateFormatter
import sg.edu.nus.iss.client.databinding.FragmentActivityInputBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ActivityDurationFragment : Fragment() {

    companion object {
        private const val ARG_EXERCISE_TYPE = "arg_exercise_type"

        // Duration is computed (not user-entered), so it's always shown in this fixed
        // green rather than the exercise's own accent color, to visually separate
        // "computed" values from the editable/tappable ones.
        private val DURATION_VALUE_COLOR = Color.parseColor("#2E7D32")

        fun newInstance(exerciseType: ExerciseType): ActivityDurationFragment {
            val fragment = ActivityDurationFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_EXERCISE_TYPE, exerciseType.name)
            }
            return fragment
        }
    }

    private var _binding: FragmentActivityInputBinding? = null
    private val binding get() = _binding!!

    private lateinit var exerciseType: ExerciseType
    private lateinit var viewModel: ActivityDurationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exerciseType = ExerciseType.valueOf(requireArguments().getString(ARG_EXERCISE_TYPE)!!)
        viewModel = ViewModelProvider(this)[ActivityDurationViewModel::class.java]
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.tvActivityTitle.text = exerciseType.displayName
        binding.tvActivityType.text = exerciseType.displayName
        binding.tvActivityDate.text = "New activity"
        binding.iconActivityType.setImageResource(exerciseType.iconRes)
        applyAccentTheme()

        binding.rowStartTime.tvLabel.text = "Start time"
        binding.rowEndTime.tvLabel.text = "End time"
        binding.rowDuration.tvLabel.text = "Duration"
        binding.rowDuration.tvValue.setTextColor(DURATION_VALUE_COLOR)
        binding.rowDuration.dividerRow.visibility = View.GONE
        binding.rowDistance.dividerRow.visibility = View.GONE
        binding.rowCalories.dividerRow.visibility = View.GONE

        binding.rowStartTime.ivIcon.setImageResource(R.drawable.ic_time)
        binding.rowEndTime.ivIcon.setImageResource(R.drawable.ic_time)
        binding.rowDuration.ivIcon.setImageResource(R.drawable.ic_hourglass)
        binding.rowDistance.ivIcon.setImageResource(R.drawable.distance_icon)
        binding.rowCalories.ivIcon.setImageResource(R.drawable.calories_icon)
        listOf(
            binding.rowStartTime.ivIcon, binding.rowEndTime.ivIcon, binding.rowDuration.ivIcon,
            binding.rowDistance.ivIcon, binding.rowCalories.ivIcon
        ).forEach { it.setColorFilter(exerciseType.accentColor, PorterDuff.Mode.SRC_IN) }

        binding.rowDistance.tvLabel.text = "Distance"
        binding.rowDistance.tvUnit.text = "km"
        binding.rowCalories.tvLabel.text = "Calories"
        binding.rowCalories.tvUnit.text = "Cal"

        prefillSuggestedValues()

        binding.rowStartTime.root.setOnClickListener {
            showTimePicker(viewModel.startTime.value) { time -> viewModel.setStartTime(time) }
        }
        binding.rowEndTime.root.setOnClickListener {
            showTimePicker(viewModel.endTime.value) { time -> viewModel.setEndTime(time) }
        }

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.btnSave.setOnClickListener {
            val distanceText = binding.rowDistance.etValue.text.toString().trim()
            val caloriesText = binding.rowCalories.etValue.text.toString().trim()

            if (distanceText.isEmpty()) {
                binding.rowDistance.etValue.error = "Required"
                return@setOnClickListener
            }
            if (caloriesText.isEmpty()) {
                binding.rowCalories.etValue.error = "Required"
                return@setOnClickListener
            }

            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                type = exerciseType.displayName,
                timestamp = LocalDateTime.of(LocalDate.now(), viewModel.startTime.value),
                durationMinutes = viewModel.durationMinutes.value,
                distanceKm = distanceText.toDouble(),
                calories = caloriesText.toDouble().toInt()
            )
            dashboardViewModel.addRecord(record)
            RouteManager.backTo(this, R.id.chooseExerciseFragment, true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startTime.collect { time ->
                    binding.rowStartTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(time)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.endTime.collect { time ->
                    binding.rowEndTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(time)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.durationMinutes.collect { minutes ->
                    binding.rowDuration.tvValue.text = "$minutes min"
                }
            }
        }
    }

    private fun applyAccentTheme() {
        // The header's pale background tint alone (e.g. Walk/Hike's) reads as almost
        // neutral gray, not clearly "in family" with any bold button color below it.
        // Blending it partway toward the accent color keeps it a soft header while making
        // its hue actually legible, so it visually flows into the bold button beneath it.
        val headerDeepTint = ColorUtils.blendARGB(exerciseType.accentBackground, exerciseType.accentColor, 0.35f)
        (binding.headerBanner.background.mutate() as GradientDrawable).colors =
            intArrayOf(exerciseType.accentBackground, headerDeepTint)

        val rowIconCircles = listOf(
            binding.rowStartTime.iconCircle, binding.rowEndTime.iconCircle, binding.rowDuration.iconCircle,
            binding.rowDistance.iconCircle, binding.rowCalories.iconCircle
        )
        rowIconCircles.forEach {
            (it.background.mutate() as GradientDrawable).setColor(exerciseType.accentBackground)
        }

        // Same formula for every type (this is what Cycle already used), so Cycle's
        // button is unchanged and the others now match it instead of looking crushed/muddy.
        val buttonColorDark = ColorUtils.blendARGB(exerciseType.accentColor, Color.BLACK, 0.35f)
        (binding.btnSave.background.mutate() as GradientDrawable).colors =
            intArrayOf(exerciseType.accentColor, buttonColorDark)
    }

    /** One-time suggestion based on the exercise type's typical pace, seeded from the
     *  default 30-minute duration. The fields stay freely editable afterwards. */
    private fun prefillSuggestedValues() {
        val (speedKmh, calPerMin) = when (exerciseType) {
            ExerciseType.WALK -> 5.0 to 4.0
            ExerciseType.RUN -> 10.0 to 10.0
            ExerciseType.SWIM -> 2.0 to 8.0
            ExerciseType.HIKE -> 4.5 to 7.0
            ExerciseType.CYCLE -> 15.0 to 8.0
        }
        val durationMinutes = viewModel.durationMinutes.value
        val distance = Math.round(speedKmh * (durationMinutes / 60.0) * 100) / 100.0
        val calories = (calPerMin * durationMinutes).toInt()

        binding.rowDistance.etValue.setText(distance.toString())
        binding.rowCalories.etValue.setText(calories.toString())
    }

    private fun showTimePicker(initial: LocalTime, onPicked: (LocalTime) -> Unit) {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute -> onPicked(LocalTime.of(hour, minute)) },
            initial.hour,
            initial.minute,
            false
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
