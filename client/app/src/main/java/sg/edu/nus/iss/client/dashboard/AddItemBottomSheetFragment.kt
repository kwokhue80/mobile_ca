package sg.edu.nus.iss.client.dashboard

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddItemBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ITEM_NAME = "item_name"
        private const val TAG = "WellnessRecord"

        /*
         * Backend WellnessRecordPayload.java expects:
         * yyyy-MM-dd HH:mm:ss
         */
        private val RECORD_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US)

        /*
         * User-friendly input format for Sleep Start Time and Wake Up Time.
         * Example: 2026-07-05 23:00
         */
        private val USER_INPUT_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

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

        val mealTypeInput = addInput(
            label = "Meal Type",
            hint = "BREAKFAST / LUNCH / DINNER / SNACK / OTHER",
            inputType = InputType.TYPE_CLASS_TEXT
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
            val mealType = normalizeEnumInput(mealTypeInput.text.toString())
            val foodName = foodNameInput.text.toString().trim()
            val calories = caloriesInput.text.toString().trim().toIntOrNull()

            if (mealType.isEmpty()) {
                mealTypeInput.error = "Meal type is required"
                return@setOnClickListener
            }

            if (mealType !in MEAL_TYPES) {
                mealTypeInput.error = "Use BREAKFAST, LUNCH, DINNER, SNACK, or OTHER"
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
     * User enters:
     * 1. Sleep Start Time
     * 2. Wake Up Time
     * 3. Sleep Quality Rating
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
            "Enter sleep start and wake-up time. The app will calculate sleep duration automatically."
        )

        val startTimeInput = addInput(
            label = "Sleep Start Time",
            hint = "yyyy-MM-dd HH:mm, e.g. 2026-07-05 23:00",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val wakeUpTimeInput = addInput(
            label = "Wake Up Time",
            hint = "yyyy-MM-dd HH:mm, e.g. 2026-07-06 07:00",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val qualityInput = addInput(
            label = "Sleep Quality Rating",
            hint = "1 = poor, 10 = excellent",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Sleep")

        saveButton.setOnClickListener {
            val startTimeText = startTimeInput.text.toString().trim()
            val wakeUpTimeText = wakeUpTimeInput.text.toString().trim()
            val quality = qualityInput.text.toString().trim().toIntOrNull()

            val startTime = parseUserDateTime(startTimeText)
            val wakeUpTime = parseUserDateTime(wakeUpTimeText)

            if (startTime == null) {
                startTimeInput.error = "Use format yyyy-MM-dd HH:mm"
                return@setOnClickListener
            }

            if (wakeUpTime == null) {
                wakeUpTimeInput.error = "Use format yyyy-MM-dd HH:mm"
                return@setOnClickListener
            }

            if (!wakeUpTime.isAfter(startTime)) {
                wakeUpTimeInput.error = "Wake up time must be after sleep start time"
                return@setOnClickListener
            }

            val sleepMinutes = Duration.between(startTime, wakeUpTime).toMinutes().toInt()

            if (sleepMinutes <= 0) {
                wakeUpTimeInput.error = "Sleep duration must be greater than 0 minutes"
                return@setOnClickListener
            }

            if (quality == null || quality !in 1..10) {
                qualityInput.error = "Please enter a rating from 1 to 10"
                return@setOnClickListener
            }

            val recordDateForBackend = wakeUpTime.format(RECORD_DATE_FORMATTER)

            Log.d(TAG, "Sleep startTime = $startTimeText")
            Log.d(TAG, "Sleep wakeUpTime = $wakeUpTimeText")
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

        val exerciseTypeInput = addInput(
            label = "Exercise Type",
            hint = "WALKING / RUNNING / CYCLING / YOGA / OTHER",
            inputType = InputType.TYPE_CLASS_TEXT
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
            val exerciseType = normalizeEnumInput(exerciseTypeInput.text.toString())
            val duration = durationInput.text.toString().trim().toIntOrNull()

            val distanceText = distanceInput.text.toString().trim()
            val caloriesText = caloriesBurnedInput.text.toString().trim()

            val distance = if (distanceText.isEmpty()) null else distanceText.toDoubleOrNull()
            val caloriesBurned =
                if (caloriesText.isEmpty()) null else caloriesText.toIntOrNull()

            if (exerciseType.isEmpty()) {
                exerciseTypeInput.error = "Exercise type is required"
                return@setOnClickListener
            }

            if (exerciseType !in EXERCISE_TYPES) {
                exerciseTypeInput.error = "Use WALKING, RUNNING, CYCLING, YOGA, or OTHER"
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

    private fun parseUserDateTime(rawValue: String): LocalDateTime? {
        return runCatching {
            LocalDateTime.parse(rawValue, USER_INPUT_DATE_FORMATTER)
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