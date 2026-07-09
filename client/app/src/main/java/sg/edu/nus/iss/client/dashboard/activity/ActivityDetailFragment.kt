// Author: HuaYuan Xie
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
        val exerciseType = ExerciseType.fromDisplayName(record.type)

        binding.tvActivityTitle.text = record.type
        binding.tvActivityType.text = record.type
        binding.tvActivityDate.text = ActivityDateFormatter.formatCompact(record.timestamp)
        binding.iconActivityType.setImageResource(exerciseType?.iconRes ?: R.drawable.ic_activity_default)

        applyAccentTheme(exerciseType)

        val endTime = record.timestamp.plusMinutes(record.durationMinutes.toLong())
        val timeRange = "${ActivityDateFormatter.formatTimeOnly(record.timestamp)} - ${ActivityDateFormatter.formatTimeOnly(endTime)}"
        binding.tvTimeRange.text = timeRange
        binding.tvDuration.text = "${record.durationMinutes} min"
        binding.tvDistance.text = "%.2f".format(record.distanceKm)
        binding.tvCalories.text = "${record.calories}"

        if (record.distanceKm > 0) {
            val totalSeconds = record.durationMinutes * 60
            val secondsPerKm = totalSeconds / record.distanceKm
            val paceMinutes = (secondsPerKm / 60).toInt()
            val paceSeconds = (secondsPerKm % 60).toInt()
            binding.tvPace.text = "%d'%02d".format(paceMinutes, paceSeconds)
        } else {
            binding.tvPace.text = "--'--"
        }
    }

    private fun applyAccentTheme(exerciseType: ExerciseType?) {
        val accentColor = exerciseType?.accentColor ?: DEFAULT_ACCENT_COLOR
        val accentBackground = exerciseType?.accentBackground ?: DEFAULT_ACCENT_BACKGROUND
        val accentColorDark = ColorUtils.blendARGB(accentColor, Color.BLACK, 0.35f)

        // Explicitly set both gradient stops to the same color rather than setColor(),
        // since bg_activity_header.xml now declares a real <gradient> (for Add Activity's
        // header) and setColor() alone wasn't reliably overriding it here - the XML's own
        // default gradient colors kept showing through for every exercise type.
        (binding.headerBanner.background.mutate() as GradientDrawable).colors =
            intArrayOf(accentBackground, accentBackground)
        (binding.iconContainer.background.mutate() as GradientDrawable).setColor(0xFFFFFFFF.toInt())
        (binding.durationCard.background.mutate() as GradientDrawable).colors =
            intArrayOf(accentColor, accentColorDark)

        listOf(binding.iconDistance, binding.iconPace, binding.iconCalories)
            .forEach { it.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
