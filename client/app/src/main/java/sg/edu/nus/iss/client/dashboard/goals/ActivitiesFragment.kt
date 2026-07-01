package sg.edu.nus.iss.client.dashboard.goals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentActivitiesBinding
import sg.edu.nus.iss.client.dashboard.goals.model.ActivityGoalType

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvPageTitle.text = "Activities"

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        val adapter = ActivityListAdapter(ActivityGoalType.entries) { activityGoalType ->
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GoalSettingFragment.newInstance(activityGoalType))
                .addToBackStack(null)
                .commit()
        }
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
