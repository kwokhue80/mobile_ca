package sg.edu.nus.iss.client.dashboard

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import sg.edu.nus.iss.client.databinding.BottomSheetAddItemBinding
import sg.edu.nus.iss.client.network.RetrofitClient
import sg.edu.nus.iss.client.network.WellnessRecord
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddItemBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ITEM_NAME = "item_name"
        private const val TAG = "WellnessRecord"
        private const val MEAL_TYPE_PROMPT = "Select meal type"
        private const val EXERCISE_TYPE_PROMPT = "Select exercise type"

        /*
         * Backend WellnessRecordPayload.java expects:
         * yyyy-MM-dd HH:mm:ss
         */
        private val RECORD_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

        private val SLEEP_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ISO_LOCAL_DATE

        private val SLEEP_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm", Locale.US)

        private val MEAL_TYPES = setOf(
            "BREAKFAST",
            "BRUNCH",
            "MORNING_SNACK",
            "MORNING_TEA",
            "TEA_BREAK",
            "LUNCH",
            "AFTERNOON_SNACK",
            "AFTERNOON_TEA",
            "DINNER",
            "SUPPER",
            "SNACK",
            "DESSERT",
            "PRE_WORKOUT",
            "POST_WORKOUT",
            "MIDNIGHT_MEAL",
            "BEVERAGE",
            "OTHER"
        )

        private val EXERCISE_TYPES = setOf(
            "RUNNING",
            "WALKING",
            "SWIMMING",
            "HIKING",
            "CYCLING",
            "JOGGING",
            "STRENGTH_TRAINING",
            "WEIGHTLIFTING",
            "BODYWEIGHT_TRAINING",
            "HIIT",
            "CROSSFIT",
            "YOGA",
            "PILATES",
            "STRETCHING",
            "ROWING",
            "JUMP_ROPE",
            "DANCING",
            "BASKETBALL",
            "FOOTBALL",
            "BADMINTON",
            "TENNIS",
            "VOLLEYBALL",
            "MARTIAL_ARTS",
            "CLIMBING",
            "OTHER"
        )

        fun newInstance(itemName: String): AddItemBottomSheetFragment {
            return AddItemBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ITEM_NAME, itemName)
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.isFitToContents = true
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemName = arguments?.getString(ARG_ITEM_NAME).orEmpty()

        binding.tvAddItemTitle.text = "Add $itemName"
        binding.btnClose.setOnClickListener { dismiss() }

        setupFormByItemName(itemName)

        val basePaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                basePaddingBottom + navBarInset
            )
            insets
        }
    }

    private fun setupFormByItemName(itemName: String) {
        binding.contentContainer.removeAllViews()

        when (itemName) {
            "Food" -> setupFoodForm()
            "Sleep" -> setupSleepForm()
            "Hydration" -> setupHydrationForm()
            "Weight" -> setupWeightForm()
            "Exercise" -> setupExerciseForm()
            "Mood" -> setupMoodForm()
            else -> setupUnsupportedForm(itemName)
        }
    }

    private fun setupFoodForm() {
        addSectionDescription(
            "Food will be saved into food_logs through POST /api/wellness/records."
        )

        val mealTypeInput = addSpinner(
            label = "Meal Type",
            items = listOf(MEAL_TYPE_PROMPT) + MEAL_TYPES.toList()
        )

        val foodNameInput = addInput(
            label = "Food Name / Description",
            hint = "e.g. chicken rice",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val caloriesInput = addInput(
            label = "Calories (kcal)",
            hint = "e.g. 650",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Food")

        saveButton.setOnClickListener {
            val mealType = mealTypeInput.selectedItem?.toString().orEmpty()
            val foodName = foodNameInput.text.toString().trim()
            val calories = caloriesInput.text.toString().trim().toIntOrNull()

            if (mealType !in MEAL_TYPES) {
                Toast.makeText(
                    requireContext(),
                    "Meal type is required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (foodName.isEmpty()) {
                foodNameInput.error = "Food name is required"
                return@setOnClickListener
            }

            if (calories == null || calories <= 0) {
                caloriesInput.error = "Calories must be a positive number"
                return@setOnClickListener
            }

            val record = baseRecord().copy(
                mealType = mealType,
                mealDescription = foodName,
                mealCaloriesKcal = calories
            )

            saveWellnessRecord(
                formName = "Food",
                record = record,
                saveButton = saveButton
            )
        }
    }

    /*
     * Sleep Form - Frontend Scheme B
     *
     * User selects:
     * 1. Sleep Start Date
     * 2. Sleep Start Time
     * 3. Wake Up Date
     * 4. Wake Up Time
     * 5. Sleep Quality Rating
     *
     * Frontend calculates:
     * sleepMinutes = Wake Up Time - Sleep Start Time
     *
     * Frontend sends to backend:
     * recordDate = Wake Up Time
     * sleepMinutes = calculated minutes
     * sleepQualityRating = user rating
     *
     * Backend can continue using its existing logic:
     * end_time = recordDate
     * start_time = recordDate - sleepMinutes
     */
    private fun setupSleepForm() {
        addSectionDescription(
            "Select sleep start and wake-up date/time. The app will calculate sleep duration automatically."
        )

        val startDateInput = addPickerInput(
            label = "Sleep Start Date",
            hint = "Select date, e.g. 2026-07-05"
        )

        val startTimeInput = addPickerInput(
            label = "Sleep Start Time",
            hint = "Select time, e.g. 23:30"
        )

        val wakeUpDateInput = addPickerInput(
            label = "Wake Up Date",
            hint = "Select date, e.g. 2026-07-06"
        )

        val wakeUpTimeInput = addPickerInput(
            label = "Wake Up Time",
            hint = "Select time, e.g. 07:00"
        )

        startDateInput.setOnClickListener { showDatePicker(startDateInput) }
        startTimeInput.setOnClickListener { showTimePicker(startTimeInput) }
        wakeUpDateInput.setOnClickListener { showDatePicker(wakeUpDateInput) }
        wakeUpTimeInput.setOnClickListener { showTimePicker(wakeUpTimeInput) }

        val qualityInput = addInput(
            label = "Sleep Quality Rating",
            hint = "1 = poor, 5 = excellent",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Sleep")

        saveButton.setOnClickListener {
            val startDateText = startDateInput.text.toString().trim()
            val startTimeText = startTimeInput.text.toString().trim()
            val wakeUpDateText = wakeUpDateInput.text.toString().trim()
            val wakeUpTimeText = wakeUpTimeInput.text.toString().trim()
            val quality = qualityInput.text.toString().trim().toIntOrNull()

            if (startDateText.isEmpty()) {
                startDateInput.error = "Sleep Start Date is required"
                return@setOnClickListener
            }

            if (startTimeText.isEmpty()) {
                startTimeInput.error = "Sleep Start Time is required"
                return@setOnClickListener
            }

            if (wakeUpDateText.isEmpty()) {
                wakeUpDateInput.error = "Wake Up Date is required"
                return@setOnClickListener
            }

            if (wakeUpTimeText.isEmpty()) {
                wakeUpTimeInput.error = "Wake Up Time is required"
                return@setOnClickListener
            }

            val sleepStartDateTime = parseSleepDateTime(startDateText, startTimeText)
            val wakeUpDateTime = parseSleepDateTime(wakeUpDateText, wakeUpTimeText)

            if (sleepStartDateTime == null) {
                startDateInput.error = "Select a valid sleep start date and time"
                return@setOnClickListener
            }

            if (wakeUpDateTime == null) {
                wakeUpDateInput.error = "Select a valid wake up date and time"
                return@setOnClickListener
            }

            if (!wakeUpDateTime.isAfter(sleepStartDateTime)) {
                wakeUpTimeInput.error = "Wake up time must be after sleep start time"
                return@setOnClickListener
            }

            val sleepMinutes = Duration.between(sleepStartDateTime, wakeUpDateTime).toMinutes().toInt()

            if (sleepMinutes <= 0) {
                wakeUpTimeInput.error = "Sleep duration must be greater than 0 minutes"
                return@setOnClickListener
            }

            if (quality == null || quality !in 1..5) {
                qualityInput.error = "Please enter a rating from 1 to 5"
                return@setOnClickListener
            }

            val recordDateForBackend = wakeUpDateTime.format(RECORD_DATE_FORMATTER)

            Log.d(TAG, "Sleep sleepStartDateTime = ${sleepStartDateTime.toString()}")
            Log.d(TAG, "Sleep wakeUpDateTime = ${wakeUpDateTime.toString()}")
            Log.d(TAG, "Calculated sleepMinutes = $sleepMinutes")
            Log.d(TAG, "Backend recordDate = $recordDateForBackend")

            val record = WellnessRecord(
                recordDate = recordDateForBackend,
                sleepMinutes = sleepMinutes,
                sleepQualityRating = quality
            )

            saveWellnessRecord(
                formName = "Sleep",
                record = record,
                saveButton = saveButton
            )
        }
    }

    private fun setupHydrationForm() {
        addSectionDescription(
            "Hydration will be saved into hydration_logs. Backend expects waterIntakeMl."
        )

        val waterInput = addInput(
            label = "Water Intake (ml)",
            hint = "e.g. 500",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Hydration")

        saveButton.setOnClickListener {
            val waterMl = waterInput.text.toString().trim().toIntOrNull()

            if (waterMl == null || waterMl <= 0) {
                waterInput.error = "Water intake must be a positive number"
                return@setOnClickListener
            }

            val record = baseRecord().copy(
                waterIntakeMl = waterMl
            )

            saveWellnessRecord(
                formName = "Hydration",
                record = record,
                saveButton = saveButton
            )
        }
    }

    private fun setupWeightForm() {
        addSectionDescription(
            "Weight will be saved into weight_logs. Backend expects weightKg."
        )

        val weightInput = addInput(
            label = "Weight (kg)",
            hint = "e.g. 82.5",
            inputType = decimalInputType()
        )

        val saveButton = addButton("Save Weight")

        saveButton.setOnClickListener {
            val weightKg = weightInput.text.toString().trim().toDoubleOrNull()

            if (weightKg == null || weightKg <= 0.0) {
                weightInput.error = "Weight must be a positive number"
                return@setOnClickListener
            }

            val record = baseRecord().copy(
                weightKg = weightKg
            )

            saveWellnessRecord(
                formName = "Weight",
                record = record,
                saveButton = saveButton
            )
        }
    }

    private fun setupExerciseForm() {
        addSectionDescription(
            "Exercise will be saved into exercise_logs. Distance and calories burned are optional."
        )

        val exerciseTypeInput = addSpinner(
            label = "Exercise Type",
            items = listOf(EXERCISE_TYPE_PROMPT) + EXERCISE_TYPES.toList()
        )

        val durationInput = addInput(
            label = "Duration (minutes)",
            hint = "e.g. 30",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val distanceInput = addInput(
            label = "Distance (km, optional)",
            hint = "e.g. 3.2",
            inputType = decimalInputType()
        )

        val caloriesBurnedInput = addInput(
            label = "Calories Burned (kcal, optional)",
            hint = "e.g. 180",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Exercise")

        saveButton.setOnClickListener {
            val exerciseType = exerciseTypeInput.selectedItem?.toString().orEmpty()
            val duration = durationInput.text.toString().trim().toIntOrNull()

            val distanceText = distanceInput.text.toString().trim()
            val caloriesText = caloriesBurnedInput.text.toString().trim()

            val distance = if (distanceText.isEmpty()) null else distanceText.toDoubleOrNull()
            val caloriesBurned =
                if (caloriesText.isEmpty()) null else caloriesText.toIntOrNull()

            if (exerciseType !in EXERCISE_TYPES) {
                Toast.makeText(
                    requireContext(),
                    "Exercise type is required",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (duration == null || duration <= 0) {
                durationInput.error = "Duration must be a positive number"
                return@setOnClickListener
            }

            if (distanceText.isNotEmpty() && (distance == null || distance < 0.0)) {
                distanceInput.error = "Distance must be a valid number"
                return@setOnClickListener
            }

            if (caloriesText.isNotEmpty() && (caloriesBurned == null || caloriesBurned < 0)) {
                caloriesBurnedInput.error = "Calories burned must be a valid number"
                return@setOnClickListener
            }

            val record = baseRecord().copy(
                exerciseType = exerciseType,
                exerciseDurationMinutes = duration,
                exerciseDistanceKm = distance,
                exerciseCaloriesBurnedKcal = caloriesBurned
            )

            saveWellnessRecord(
                formName = "Exercise",
                record = record,
                saveButton = saveButton
            )
        }
    }

    private fun setupMoodForm() {
        addSectionDescription(
            "Mood will be saved into mood_logs. Backend currently expects moodRating only."
        )

        val moodInput = addInput(
            label = "Mood Rating",
            hint = "1 = low, 10 = very good",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Mood")

        saveButton.setOnClickListener {
            val moodRating = moodInput.text.toString().trim().toIntOrNull()

            if (moodRating == null || moodRating !in 1..10) {
                moodInput.error = "Please enter a rating from 1 to 10"
                return@setOnClickListener
            }

            val record = baseRecord().copy(
                moodRating = moodRating
            )

            saveWellnessRecord(
                formName = "Mood",
                record = record,
                saveButton = saveButton
            )
        }
    }

    private fun setupUnsupportedForm(itemName: String) {
        addSectionDescription(
            "$itemName is not supported by the current backend. " +
                    "Only Food, Sleep, Hydration, Weight, Exercise, and Mood are enabled."
        )
    }

    private fun baseRecord(): WellnessRecord {
        val formattedDate = LocalDateTime.now().format(RECORD_DATE_FORMATTER)

        Log.d(TAG, "Generated recordDate = $formattedDate")

        return WellnessRecord(
            recordDate = formattedDate
        )
    }

    private fun parseSleepDateTime(dateText: String, timeText: String): LocalDateTime? {
        return runCatching {
            LocalDateTime.of(
                LocalDate.parse(dateText, SLEEP_DATE_FORMATTER),
                LocalTime.parse(timeText, SLEEP_TIME_FORMATTER)
            )
        }.getOrNull()
    }

    private fun saveWellnessRecord(
        formName: String,
        record: WellnessRecord,
        saveButton: Button
    ) {
        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        Log.d(TAG, "Sending $formName record = $record")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient
                    .getApiService(requireContext())
                    .saveRecord(record)

                if (response.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "$formName saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    dismiss()
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()

                    Log.e(
                        TAG,
                        "Save $formName failed. Code=${response.code()}, Body=$errorBody"
                    )

                    Toast.makeText(
                        requireContext(),
                        "Save failed: ${response.code()} ${errorBody.take(160)}",
                        Toast.LENGTH_LONG
                    ).show()

                    saveButton.isEnabled = true
                    saveButton.text = "Save $formName"
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Save $formName exception", exception)

                Toast.makeText(
                    requireContext(),
                    "Save failed: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()

                saveButton.isEnabled = true
                saveButton.text = "Save $formName"
            }
        }
    }

    private fun normalizeEnumInput(rawValue: String): String {
        return rawValue
            .trim()
            .replace("-", "_")
            .replace(" ", "_")
            .uppercase(Locale.US)
    }

    private fun addSectionDescription(text: String) {
        val description = TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 0, 0, dp(16))
        }

        binding.contentContainer.addView(description)
    }

    private fun addInput(
        label: String,
        hint: String,
        inputType: Int
    ): EditText {
        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(8), 0, dp(6))
        }

        val input = EditText(requireContext()).apply {
            this.hint = hint
            this.inputType = inputType
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }

        binding.contentContainer.addView(labelView)

        binding.contentContainer.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return input
    }

    private fun addPickerInput(
        label: String,
        hint: String
    ): EditText {
        return addInput(
            label = label,
            hint = hint,
            inputType = InputType.TYPE_NULL
        ).apply {
            isFocusable = false
            isCursorVisible = false
            keyListener = null
        }
    }

    private fun addSpinner(
        label: String,
        items: List<String>
    ): Spinner {
        val labelView = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(8), 0, dp(6))
        }

        val spinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                items
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
        }

        binding.contentContainer.addView(labelView)
        binding.contentContainer.addView(
            spinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return spinner
    }

    private fun showDatePicker(targetInput: EditText) {
        val initialDate = runCatching {
            LocalDate.parse(targetInput.text.toString(), SLEEP_DATE_FORMATTER)
        }.getOrElse {
            LocalDate.now()
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                targetInput.error = null
                targetInput.setText(selectedDate.format(SLEEP_DATE_FORMATTER))
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    private fun showTimePicker(targetInput: EditText) {
        val initialTime = runCatching {
            LocalTime.parse(targetInput.text.toString(), SLEEP_TIME_FORMATTER)
        }.getOrElse {
            LocalTime.now()
        }

        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                val selectedTime = LocalTime.of(hourOfDay, minute)
                targetInput.error = null
                targetInput.setText(selectedTime.format(SLEEP_TIME_FORMATTER))
            },
            initialTime.hour,
            initialTime.minute,
            true
        ).show()
    }

    private fun addButton(text: String): Button {
        val button = Button(requireContext()).apply {
            this.text = text
            isAllCaps = false
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(16)
        }

        binding.contentContainer.addView(button, params)

        return button
    }

    private fun decimalInputType(): Int {
        return InputType.TYPE_CLASS_NUMBER or
                InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
