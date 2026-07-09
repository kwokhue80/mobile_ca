// Author: HuaYuan Xie
package sg.edu.nus.iss.client.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import sg.edu.nus.iss.client.databinding.BottomSheetAddManuallyBinding
import sg.edu.nus.iss.client.navigation.RouteManager

class AddManuallyBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddManuallyBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddManuallyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val basePaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePaddingBottom + navBarInset)
            insets
        }

        binding.btnAddFood.setOnClickListener { openAddItem("Food") }
        binding.btnAddSleep.setOnClickListener { openAddItem("Sleep") }
        binding.btnAddHydration.setOnClickListener { openAddItem("Hydration") }
        binding.btnAddWeight.setOnClickListener { openAddItem("Weight") }
        binding.btnAddExercise.setOnClickListener { openAddItem("Exercise") }
        binding.btnAddMood.setOnClickListener { openAddItem("Mood") }
    }

    private fun openAddItem(itemName: String) {
        RouteManager.showAddItem(this, itemName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
