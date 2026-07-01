package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentDashboardBinding
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord

class DashboardFragment : Fragment() {
    companion object {
        private const val DOT_SIZE_DP = 10
        private const val DOT_MARGIN_DP = 10
    }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel

    private val activityAdapter = ActivityRecordAdapter(onDeleteClick = { record ->
        confirmDeleteRecord(record)
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        binding.viewPagerDashboard.adapter = DashboardPagerAdapter(requireActivity())
        TabLayoutMediator(binding.tabDots, binding.viewPagerDashboard) { tab, _ ->
            tab.view.isClickable = false
        }.attach()
        resizeDotTabs()

        binding.rvActivityRecords.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivityRecords.adapter = activityAdapter
        binding.rvActivityRecords.isNestedScrollingEnabled = false

        binding.btnAddActivity.setOnClickListener {
            // TODO: navigate to Add Activity form
        }

        binding.btnHistory.setOnClickListener {
            // TODO: navigate to History page
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activityRecords.collect { records ->
                    activityAdapter.submitList(records)
                }
            }
        }
    }

    private fun resizeDotTabs() {
        val density = resources.displayMetrics.density
        val dotSizePx = (DOT_SIZE_DP * density).toInt()
        val dotMarginPx = (DOT_MARGIN_DP * density).toInt()
        for (i in 0 until binding.tabDots.tabCount) {
            val tabView = binding.tabDots.getTabAt(i)?.view ?: continue
            val params = tabView.layoutParams as? ViewGroup.MarginLayoutParams ?: continue
            params.width = dotSizePx
            params.height = dotSizePx
            params.marginStart = dotMarginPx
            params.marginEnd = dotMarginPx
            tabView.layoutParams = params
        }
    }

    private fun confirmDeleteRecord(record: ActivityRecord) {
        AlertDialog.Builder(requireContext())
            .setMessage("Are you sure you want to delete this record?")
            .setPositiveButton("Delete") { _, _ -> viewModel.removeRecord(record.id) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
