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
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentDashboardBinding
import sg.edu.nus.iss.client.dashboard.model.ActivityRecord
import sg.edu.nus.iss.client.navigation.RouteManager

class DashboardFragment : Fragment() {
    companion object {
        private const val DOT_SIZE_DP = 10
        private const val DOT_MARGIN_DP = 10

        // Home only teases the most recent activity; the full history (now that
        // DashboardViewModel.activityRecords covers up to a year, not just 7 days)
        // lives in the History screen instead.
        private const val ACTIVITY_TRACKED_LIMIT = 20
    }

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel

    private val activityPagerAdapter = ActivityRecordPagerAdapter(
        onDeleteClick = { record -> confirmDeleteRecord(record) },
        onPageNumberClick = { page -> binding.viewPagerActivityRecords.setCurrentItem(page, true) },
        onItemClick = { record -> RouteManager.toActivityDetail(this, record.id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.viewPagerDashboard.adapter = DashboardPagerAdapter(requireActivity())
        TabLayoutMediator(binding.tabDots, binding.viewPagerDashboard) { tab, _ ->
            tab.view.isClickable = false
        }.attach()
        resizeDotTabs()

        binding.viewPagerActivityRecords.adapter = activityPagerAdapter
        binding.viewPagerActivityRecords.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                activityPagerAdapter.setCurrentPage(position)
            }
        })

        binding.btnAddActivity.setOnClickListener {
            RouteManager.toChooseExercise(this)
        }

        binding.btnSetGoals.setOnClickListener {
            RouteManager.toGoals(this)
        }

        binding.btnHistory.setOnClickListener {
            RouteManager.toHistory(this)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activityRecords.collect { records ->
                    activityPagerAdapter.submitList(records.take(ACTIVITY_TRACKED_LIMIT))
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
