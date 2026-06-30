package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val dots by lazy { listOf(binding.dot0, binding.dot1) }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            dots.forEachIndexed { index, dot ->
                dot.setBackgroundResource(
                    if (index == position) R.drawable.dot_selected else R.drawable.dot_unselected
                )
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewPagerDashboard.adapter = DashboardPagerAdapter(requireActivity())
        binding.viewPagerDashboard.registerOnPageChangeCallback(pageChangeCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPagerDashboard.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}
