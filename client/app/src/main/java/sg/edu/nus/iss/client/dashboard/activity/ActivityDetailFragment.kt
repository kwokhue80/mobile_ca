package sg.edu.nus.iss.client.dashboard.activity

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
import sg.edu.nus.iss.client.databinding.FragmentActivityDetailBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import kotlin.math.roundToInt

class ActivityDetailFragment : Fragment() {

    companion object {
        private const val ARG_RECORD_ID = "arg_record_id"
        private val DEFAULT_ACCENT_COLOR = Color.parseColor("#1F1F1F")
        private val DEFAULT_ACCENT_BACKGROUND = Color.parseColor("#EEF1F5")

        fun newInstance(recordId: String): ActivityDetailFragment {
            val fragment = ActivityDetailFragment()
            fragment.arguments = Bundle().apply { putString(ARG_RECORD_ID, recordId) }
            return fragment
        }
    }

    private var _binding: FragmentActivityDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recordId = requireArguments().getString(ARG_RECORD_ID)!!
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.btnBack.setOnClickListener { RouteManager.back(this) }

        binding.rowStartTime.tvLabel.text = "Start time"
        binding.rowEndTime.tvLabel.text = "End time"
        binding.rowDuration.tvLabel.text = "Duration"
        binding.rowDistance.tvLabel.text = "Distance"
        binding.rowCalories.tvLabel.text = "Calories"

        binding.rowStartTime.ivIcon.setImageResource(R.drawable.ic_time)
        binding.rowEndTime.ivIcon.setImageResource(R.drawable.ic_time)
        binding.rowDuration.ivIcon.setImageResource(R.drawable.ic_time)
        binding.rowDistance.ivIcon.setImageResource(R.drawable.distance_icon)
        binding.rowCalories.ivIcon.setImageResource(R.drawable.calories_icon)

        binding.rowDuration.dividerRow.visibility = View.GONE
        binding.rowDistance.dividerRow.visibility = View.GONE
        binding.rowCalories.dividerRow.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.activityRecords.collect { records ->
                    val record = records.firstOrNull { it.id == recordId }
                    if (record != null) render(record)
                }
            }
        }
    }

    private fun render(record: ActivityRecord) {
        val exerciseType = ExerciseType.entries.firstOrNull { it.displayName == record.type }

        binding.tvActivityTitle.text = record.type
        binding.tvActivityType.text = record.type
        binding.tvActivityDate.text = ActivityDateFormatter.formatCompact(record.timestamp)
        binding.iconActivityType.setImageResource(exerciseType?.iconRes ?: R.drawable.ic_activity_default)

        applyAccentTheme(exerciseType)

        val endTime = record.timestamp.plusMinutes(record.durationMinutes.toLong())
        binding.rowStartTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(record.timestamp)
        binding.rowEndTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(endTime)
        binding.rowDuration.tvValue.text = "${record.durationMinutes} min"
        binding.rowDistance.tvValue.text = "%.2f km".format(record.distanceKm)
        binding.rowCalories.tvValue.text = "${record.calories} Cal"
    }

    private fun applyAccentTheme(exerciseType: ExerciseType?) {
        val accentColor = exerciseType?.accentColor ?: DEFAULT_ACCENT_COLOR
        val accentBackground = exerciseType?.accentBackground ?: DEFAULT_ACCENT_BACKGROUND

        (binding.headerBanner.background.mutate() as GradientDrawable).setColor(accentBackground)
        (binding.iconContainer.background.mutate() as GradientDrawable).setColor(0xFFFFFFFF.toInt())

        listOf(
            binding.rowStartTime.ivIcon, binding.rowEndTime.ivIcon, binding.rowDuration.ivIcon,
            binding.rowDistance.ivIcon, binding.rowCalories.ivIcon
        ).forEach { it.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
