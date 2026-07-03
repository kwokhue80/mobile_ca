package sg.edu.nus.iss.client.dashboard.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.dashboard.util.ActivityDateFormatter
import sg.edu.nus.iss.client.databinding.FragmentActivityDetailBinding
import sg.edu.nus.iss.client.navigation.RouteManager

class ActivityDetailFragment : Fragment() {

    companion object {
        private const val ARG_RECORD_ID = "arg_record_id"

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
        binding.tvActivityTitle.text = record.type
        binding.tvActivityType.text = record.type
        binding.tvActivityDate.text = ActivityDateFormatter.formatCompact(record.timestamp)
        binding.iconActivityType.setImageResource(
            when (record.type) {
                "Walk" -> R.drawable.ic_activity_walk
                "Run" -> R.drawable.ic_activity_run
                "Swim" -> R.drawable.ic_activity_swim
                else -> R.drawable.ic_activity_default
            }
        )

        val endTime = record.timestamp.plusMinutes(record.durationMinutes.toLong())
        binding.rowStartTime.tvLabel.text = "Start time"
        binding.rowStartTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(record.timestamp)
        binding.rowEndTime.tvLabel.text = "End time"
        binding.rowEndTime.tvValue.text = ActivityDateFormatter.formatTimeOnly(endTime)
        binding.rowDuration.tvLabel.text = "Duration"
        binding.rowDuration.tvValue.text = "${record.durationMinutes} min"
        binding.rowDistance.tvLabel.text = "Distance"
        binding.rowDistance.tvValue.text = "%.2f km".format(record.distanceKm)
        binding.rowCalories.tvLabel.text = "Calories"
        binding.rowCalories.tvValue.text = "${record.calories} Cal"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
