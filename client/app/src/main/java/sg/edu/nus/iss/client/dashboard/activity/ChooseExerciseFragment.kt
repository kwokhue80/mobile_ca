package sg.edu.nus.iss.client.dashboard.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentActivitiesBinding
import sg.edu.nus.iss.client.dashboard.activity.model.ExerciseType

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
            requireActivity().supportFragmentManager.popBackStack()
        }

        val adapter = ExerciseTypeAdapter(ExerciseType.entries) { exerciseType ->
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ActivityDurationFragment.newInstance(exerciseType))
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
