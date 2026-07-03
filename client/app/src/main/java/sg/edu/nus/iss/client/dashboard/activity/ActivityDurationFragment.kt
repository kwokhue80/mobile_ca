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
import sg.edu.nus.iss.client.databinding.FragmentGoalSettingBinding
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDateTime
import java.util.UUID

class ActivityDurationFragment : Fragment() {

    companion object {
        private const val ARG_EXERCISE_TYPE = "arg_exercise_type"

        fun newInstance(exerciseType: ExerciseType): ActivityDurationFragment {
            val fragment = ActivityDurationFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_EXERCISE_TYPE, exerciseType.name)
            }
            return fragment
        }
    }

    private var _binding: FragmentGoalSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var exerciseType: ExerciseType
    private lateinit var viewModel: ActivityDurationViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exerciseType = ExerciseType.valueOf(requireArguments().getString(ARG_EXERCISE_TYPE)!!)
        viewModel = ViewModelProvider(this)[ActivityDurationViewModel::class.java]
        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.tvActivityTitle.text = exerciseType.displayName
        binding.tvGoalUnit.text = "min"
        binding.btnSetGoal.text = "Confirm"

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.btnDecrement.setOnClickListener { viewModel.decrement() }
        binding.btnIncrement.setOnClickListener { viewModel.increment() }

        binding.btnSetGoal.setOnClickListener {
            val duration = viewModel.durationMinutes.value
            val (speedKmh, calPerMin) = when (exerciseType.displayName) {
                "Walk" -> 5.0 to 4.0
                "Run" -> 10.0 to 10.0
                "Swim" -> 2.0 to 8.0
                else -> 4.0 to 5.0
            }
            val record = ActivityRecord(
                id = UUID.randomUUID().toString(),
                type = exerciseType.displayName,
                timestamp = LocalDateTime.now(),
                durationMinutes = duration,
                distanceKm = Math.round(speedKmh * (duration / 60.0) * 100) / 100.0,
                calories = (calPerMin * duration).toInt()
            )
            dashboardViewModel.addRecord(record)
            RouteManager.backTo(this, R.id.chooseExerciseFragment, true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.durationMinutes.collect { minutes ->
                    binding.tvGoalValue.text = minutes.toString()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
