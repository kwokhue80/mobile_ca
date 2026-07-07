package sg.edu.nus.iss.client.dashboard.history

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.FragmentHistoryBinding
import sg.edu.nus.iss.client.dashboard.DashboardViewModel
import sg.edu.nus.iss.client.navigation.RouteManager
import java.time.LocalDate

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val pagerAdapter = HistoryPagerAdapter(
        onPageNumberClick = { page -> binding.viewPagerHistory.setCurrentItem(page, true) },
        onItemClick = { record -> RouteManager.toActivityDetail(this, record.id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dashboardViewModel = ViewModelProvider(requireActivity())[DashboardViewModel::class.java]

        binding.btnBack.setOnClickListener {
            RouteManager.back(this)
        }

        binding.btnPickDate.setOnClickListener { showDatePicker() }

        binding.viewPagerHistory.adapter = pagerAdapter
        binding.viewPagerHistory.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pagerAdapter.setCurrentPage(position)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dashboardViewModel.activityRecords.collect { records ->
                    pagerAdapter.submitList(records)
                }
            }
        }
    }

    private fun showDatePicker() {
        val today = LocalDate.now()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                binding.viewPagerHistory.setCurrentItem(pagerAdapter.jumpToDate(picked), true)
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
