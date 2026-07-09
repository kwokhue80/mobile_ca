// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard.badges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentBadgesBinding
import sg.edu.nus.iss.client.dashboard.goals.UserGoalsViewModel
import sg.edu.nus.iss.client.navigation.RouteManager

class BadgesFragment : Fragment() {

    companion object {
        private const val GRID_SPAN_COUNT = 3
    }

    private var _binding: FragmentBadgesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BadgesViewModel
    private lateinit var adapter: BadgeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BadgesViewModel::class.java]
        val userGoalsViewModel = ViewModelProvider(requireActivity())[UserGoalsViewModel::class.java]
        adapter = BadgeAdapter(onCollectClick = { item -> viewModel.collect(item.type) })

        binding.rvBadges.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
        binding.rvBadges.adapter = adapter

        binding.btnBack.setOnClickListener { RouteManager.back(this) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.badgeItems.collect { items -> adapter.submitList(items) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                userGoalsViewModel.goals.collect { goals -> viewModel.refresh(goals) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
