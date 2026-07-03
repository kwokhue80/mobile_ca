package sg.edu.nus.iss.client.dashboard.goals

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
import sg.edu.nus.iss.client.databinding.FragmentGoalSettingBinding
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType
import sg.edu.nus.iss.client.navigation.RouteManager

class GoalSettingFragment : Fragment() {

    companion object {
        private const val ARG_ACTIVITY_GOAL_TYPE = "arg_activity_goal_type"

        fun newInstance(activityGoalType: ActivityGoalType): GoalSettingFragment {
            val fragment = GoalSettingFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_ACTIVITY_GOAL_TYPE, activityGoalType.name)
            }
            return fragment
        }
    }

    private var _binding: FragmentGoalSettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var activityGoalType: ActivityGoalType
    private lateinit var viewModel: GoalSettingViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGoalSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activityGoalType = ActivityGoalType.valueOf(requireArguments().getString(ARG_ACTIVITY_GOAL_TYPE)!!)
        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]

        val factory = GoalSettingViewModelFactory(activityGoalType, userGoalsViewModel.getGoal(activityGoalType))
        viewModel = ViewModelProvider(this, factory)[GoalSettingViewModel::class.java]

        binding.tvActivityTitle.text = activityGoalType.displayName
        binding.tvGoalUnit.text = activityGoalType.unitLabel

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.btnDecrement.setOnClickListener { viewModel.decrement() }
        binding.btnIncrement.setOnClickListener { viewModel.increment() }

        binding.btnSetGoal.setOnClickListener {
            userGoalsViewModel.setGoal(activityGoalType, viewModel.value.value)
            RouteManager.back(this)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.value.collect { value ->
                    binding.tvGoalValue.text = activityGoalType.formatValue(value)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
