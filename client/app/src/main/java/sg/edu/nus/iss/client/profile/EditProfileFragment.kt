// Author: HuaYuan Xie
package sg.edu.nus.iss.client.profile

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.R
import sg.edu.nus.iss.client.databinding.FragmentEditProfileBinding
import sg.edu.nus.iss.client.navigation.RouteManager
import sg.edu.nus.iss.client.network.AuthApiService
import sg.edu.nus.iss.client.network.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var authApiService: AuthApiService
    private lateinit var profileViewModel: ProfileViewModel

    private var selectedDateOfBirth: LocalDate? = null
    private val dateDisplayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

    companion object {
        private const val MAX_HEIGHT_CM = 300.0
    }

    // Rejects any edit (including programmatic setText, e.g. loading a stale saved
    // value) that would push the height above MAX_HEIGHT_CM. The numberDecimal input
    // type already blocks negative values, so only the upper bound needs guarding here.
    private val heightRangeFilter = InputFilter { source, start, end, dest, dstart, dend ->
        val proposed = dest.toString().substring(0, dstart) +
            source.subSequence(start, end) +
            dest.toString().substring(dend)
        val value = proposed.toDoubleOrNull()
        if (proposed.isNotEmpty() && proposed != "." && value != null && value > MAX_HEIGHT_CM) "" else null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authApiService = RetrofitClient.getApiService(requireContext())
        profileViewModel = ViewModelProvider(
            this,
            ProfileViewModelFactory(authApiService)
        )[ProfileViewModel::class.java]

        val genderAdapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_dark,
            resources.getStringArray(R.array.gender_options)
        )
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGender.adapter = genderAdapter

        binding.etHeight.filters = arrayOf(heightRangeFilter)

        binding.btnBack.setOnClickListener { RouteManager.back(this) }
        binding.btnCancel.setOnClickListener { RouteManager.back(this) }
        binding.tvDateOfBirth.setOnClickListener { showDatePicker() }
        binding.btnConfirm.setOnClickListener { onConfirmClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.loadState.collect { state -> renderLoadState(state) }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.saveState.collect { state -> renderSaveState(state) }
            }
        }
    }

    private fun renderLoadState(state: ProfileLoadState) {
        when (state) {
            is ProfileLoadState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
                binding.formContainer.visibility = View.GONE
            }

            is ProfileLoadState.Loaded -> {
                binding.progressLoading.visibility = View.GONE
                binding.formContainer.visibility = View.VISIBLE

                binding.etFullName.setText(state.fullName)
                binding.etHeight.setText(state.heightCm)

                selectedDateOfBirth = state.dateOfBirth.takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                binding.tvDateOfBirth.text = selectedDateOfBirth?.format(dateDisplayFormatter) ?: "Select date"

                val genderOptions = resources.getStringArray(R.array.gender_options)
                val genderIndex = genderOptions.indexOfFirst { it.equals(state.gender, ignoreCase = true) }
                binding.spinnerGender.setSelection(if (genderIndex >= 0) genderIndex else 0)
            }

            is ProfileLoadState.Error -> {
                binding.progressLoading.visibility = View.GONE
                binding.formContainer.visibility = View.VISIBLE
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderSaveState(state: ProfileSaveState) {
        when (state) {
            is ProfileSaveState.Idle -> Unit
            is ProfileSaveState.Saving -> binding.btnConfirm.isEnabled = false

            is ProfileSaveState.Success -> {
                binding.btnConfirm.isEnabled = true
                Toast.makeText(requireContext(), "Profile saved", Toast.LENGTH_SHORT).show()
                profileViewModel.resetSaveState()
                RouteManager.back(this)
            }

            is ProfileSaveState.Error -> {
                binding.btnConfirm.isEnabled = true
                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                profileViewModel.resetSaveState()
            }
        }
    }

    private fun onConfirmClicked() {
        val fullName = binding.etFullName.text.toString().trim()
        val heightText = binding.etHeight.text.toString().trim()

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            return
        }
        val dateOfBirth = selectedDateOfBirth
        if (dateOfBirth == null) {
            Toast.makeText(requireContext(), "Please select your date of birth", Toast.LENGTH_SHORT).show()
            return
        }
        val heightCm = heightText.toDoubleOrNull()
        if (heightCm == null || heightCm <= 0 || heightCm > MAX_HEIGHT_CM) {
            binding.etHeight.error = "Enter a height between 0 and ${MAX_HEIGHT_CM.toInt()} cm"
            return
        }
        val gender = binding.spinnerGender.selectedItem.toString().uppercase(java.util.Locale.ROOT)

        profileViewModel.saveProfile(
            fullName = fullName,
            dateOfBirth = dateOfBirth.toString(),
            gender = gender,
            heightCm = heightCm
        )
    }

    private fun showDatePicker() {
        val initial = selectedDateOfBirth ?: LocalDate.now().minusYears(20)
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                selectedDateOfBirth = picked
                binding.tvDateOfBirth.text = picked.format(dateDisplayFormatter)
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}