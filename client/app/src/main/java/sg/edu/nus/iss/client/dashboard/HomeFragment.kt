package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.chat.ChatFragment
import sg.edu.nus.iss.client.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            showMainContent()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> {
                    showMainContent()
                    true
                }
                R.id.nav_chat -> {
                    showChatContent()
                    true
                }
                R.id.nav_add -> {
                    AddManuallyBottomSheetFragment().show(childFragmentManager, "add_manually")
                    false
                }
                else -> false
            }
        }
    }

    fun showAddItemScreen(itemName: String) {
        AddItemBottomSheetFragment.newInstance(itemName).show(childFragmentManager, "add_item")
    }

    private fun showMainContent() {
        childFragmentManager.beginTransaction()
            .replace(R.id.homeContentContainer, DashboardFragment())
            .commit()
    }

    private fun showChatContent() {
        childFragmentManager.beginTransaction()
            .replace(R.id.homeContentContainer, ChatFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
