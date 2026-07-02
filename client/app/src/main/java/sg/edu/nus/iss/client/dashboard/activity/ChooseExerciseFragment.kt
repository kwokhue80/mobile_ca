package sg.edu.nus.iss.client.dashboard.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sg.edu.nus.iss.client.databinding.FragmentActivitiesBinding
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType
import sg.edu.nus.iss.client.navigation.RouteManager

class ChooseExerciseFragment : Fragment() {

    companion object {
        const val BACK_STACK_NAME = "choose_exercise"
    }

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

        binding.tvPageTitle.text = "Choose Exercise"

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        val adapter = ExerciseTypeAdapter(ExerciseType.entries) { exerciseType ->
            RouteManager.toActivityDuration(this, exerciseType)
        }
        binding.rvActivities.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivities.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
