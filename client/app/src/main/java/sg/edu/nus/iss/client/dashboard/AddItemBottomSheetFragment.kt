package sg.edu.nus.iss.client.dashboard

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import sg.edu.nus.iss.client.databinding.BottomSheetAddItemBinding

class AddItemBottomSheetFragment : BottomSheetDialogFragment() {
    private var _binding: BottomSheetAddItemBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ITEM_NAME = "item_name"

        fun newInstance(itemName: String): AddItemBottomSheetFragment {
            return AddItemBottomSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_ITEM_NAME, itemName) }
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

        binding.btnClose.setOnClickListener {
            dismiss()
        }

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
            "Sleep" -> setupSleepForm()

            // Calories 改成 Food；保留 Calories 是为了兼容旧入口
            "Food", "Calories" -> setupFoodForm()

            // Distance 和 Exercise Days 合并到 Activity；保留旧名字是为了兼容旧入口
            "Activity", "Distance", "Exercise Days" -> setupDistanceForm()

            "Hydration" -> setupHydrationForm()
            "Weight" -> setupWeightForm()

            // Mental Health 改成 Mood；保留 Mental Health 是为了兼容旧入口
            "Mood", "Mental Health" -> setupMentalHealthForm()

            else -> setupGenericMetricForm(itemName)
        }
    }

    /**
     * Food form.
     *
     * Users manually enter meal type, food name, and calories.
     * Payload is prepared in camelCase to match Spring Boot DTO / Entity naming.
     */
    private fun setupFoodForm() {
        addSectionDescription("Record food name, meal type, and calories manually.")

        val mealTypeInput = addInput(
            label = "Meal Type",
            hint = "Breakfast / Lunch / Dinner / Snack",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val foodNameInput = addInput(
            label = "Food Name",
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
            val mealType = mealTypeInput.text.toString().trim()
            val foodName = foodNameInput.text.toString().trim()
            val caloriesText = caloriesInput.text.toString().trim()

            if (mealType.isEmpty()) {
                mealTypeInput.error = "Meal type is required"
                return@setOnClickListener
            }

            if (foodName.isEmpty()) {
                foodNameInput.error = "Food name is required"
                return@setOnClickListener
            }

            if (caloriesText.isEmpty()) {
                caloriesInput.error = "Calories is required"
                return@setOnClickListener
            }

            val calories = caloriesText.toIntOrNull()
            if (calories == null || calories < 0) {
                caloriesInput.error = "Please enter valid calories"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("mealType", normalizeMealType(mealType))
                put("foodName", foodName)
                put("caloriesKcal", calories)
                put("loggedAt", getCurrentDateTimeForBackend())
            }

            savePayloadLocallyForNow("Food", payload)
        }
    }

    /**
     * Sleep Details form.
     *
     * Current CA-friendly version:
     * - sleep duration in hours
     * - quality score 1 to 5
     *
     * Backend sleep_logs table uses start_time and end_time.
     * This form may need backend conversion or later UI adjustment.
     */
    private fun setupSleepForm() {
        addSectionDescription("Record sleep duration and sleep quality score.")

        val durationInput = addInput(
            label = "Sleep Duration (hours)",
            hint = "e.g. 7.5",
            inputType = decimalInputType()
        )

        val qualityInput = addInput(
            label = "Sleep Quality Score",
            hint = "1 = poor, 5 = excellent",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Sleep Details")
        saveButton.setOnClickListener {
            val durationText = durationInput.text.toString().trim()
            val qualityText = qualityInput.text.toString().trim()

            if (durationText.isEmpty()) {
                durationInput.error = "Sleep duration is required"
                return@setOnClickListener
            }

            if (qualityText.isEmpty()) {
                qualityInput.error = "Quality score is required"
                return@setOnClickListener
            }

            val qualityScore = qualityText.toIntOrNull()
            if (qualityScore == null || qualityScore !in 1..5) {
                qualityInput.error = "Please enter a score from 1 to 5"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("sleepDurationHours", durationText.toDouble())
                put("qualityScore", qualityScore)
            }

            savePayloadLocallyForNow("Sleep Details", payload)
        }
    }

    /**
     * Activity form.
     *
     * This form combines the old Distance and Exercise Days concepts.
     * Users record one activity with activity type, duration, optional distance,
     * and optional calories burned.
     *
     * Payload is prepared in camelCase to match Spring Boot DTO / Entity naming.
     */
    private fun setupDistanceForm() {
        addSectionDescription(
            "Record your physical activity, duration, optional distance, and optional calories burned."
        )

        val activityTypeInput = addInput(
            label = "Activity Type",
            hint = "Walking / Running / Swimming / Cycling",
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
            label = "Calories Burned (optional)",
            hint = "e.g. 180",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val saveButton = addButton("Save Activity")
        saveButton.setOnClickListener {
            val activityType = activityTypeInput.text.toString().trim()
            val durationText = durationInput.text.toString().trim()
            val distanceText = distanceInput.text.toString().trim()
            val caloriesBurnedText = caloriesBurnedInput.text.toString().trim()

            if (activityType.isEmpty()) {
                activityTypeInput.error = "Activity type is required"
                return@setOnClickListener
            }

            if (durationText.isEmpty()) {
                durationInput.error = "Duration is required"
                return@setOnClickListener
            }

            val durationMinutes = durationText.toIntOrNull()
            if (durationMinutes == null || durationMinutes <= 0) {
                durationInput.error = "Please enter a valid duration"
                return@setOnClickListener
            }

            val caloriesBurned = if (caloriesBurnedText.isEmpty()) {
                0
            } else {
                caloriesBurnedText.toIntOrNull()
            }

            if (caloriesBurned == null || caloriesBurned < 0) {
                caloriesBurnedInput.error = "Please enter valid calories burned"
                return@setOnClickListener
            }

            var distanceKm: Double? = null
            if (distanceText.isNotEmpty()) {
                val parsedDistance = distanceText.toDoubleOrNull()
                if (parsedDistance == null || parsedDistance < 0) {
                    distanceInput.error = "Please enter a valid distance"
                    return@setOnClickListener
                }
                distanceKm = parsedDistance
            }

            val payload = JSONObject().apply {
                put("exerciseType", normalizeExerciseType(activityType))
                put("durationMinutes", durationMinutes)
                put("caloriesBurnedKcal", caloriesBurned)
                put("loggedAt", getCurrentDateTimeForBackend())

                if (distanceKm != null) {
                    put("distanceKm", distanceKm)
                }
            }

            savePayloadLocallyForNow("Activity", payload)
        }
    }

    private fun setupHydrationForm() {
        addSectionDescription("Record water intake.")

        val amountInput = addInput(
            label = "Amount",
            hint = "e.g. 500",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "ml / glass / bottle",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Hydration")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "hydration",
                valueInput = amountInput,
                unitInput = unitInput,
                displayName = "Hydration"
            )
        }
    }

    private fun setupWeightForm() {
        addSectionDescription("Record current body weight.")

        val weightInput = addInput(
            label = "Weight",
            hint = "e.g. 82.5",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "kg",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("kg")

        val saveButton = addButton("Save Weight")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "weight",
                valueInput = weightInput,
                unitInput = unitInput,
                displayName = "Weight"
            )
        }
    }

    /**
     * Kept for compatibility only.
     * The Add Manually screen hides Steps, so users should not open this form now.
     */
    private fun setupStepsForm() {
        addSectionDescription("Record daily step count.")

        val stepsInput = addInput(
            label = "Steps",
            hint = "e.g. 8000",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "steps",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("steps")

        val saveButton = addButton("Save Steps")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "steps",
                valueInput = stepsInput,
                unitInput = unitInput,
                displayName = "Steps"
            )
        }
    }

    /**
     * Kept for compatibility only.
     * Exercise Days has been merged into Activity.
     */
    private fun setupExerciseDaysForm() {
        addSectionDescription("Record how many days you exercised in the selected period.")

        val daysInput = addInput(
            label = "Exercise Days",
            hint = "e.g. 3",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "days",
            inputType = InputType.TYPE_CLASS_TEXT
        )
        unitInput.setText("days")

        val saveButton = addButton("Save Exercise Days")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = "exercise_days",
                valueInput = daysInput,
                unitInput = unitInput,
                displayName = "Exercise Days"
            )
        }
    }

    /**
     * Mood form.
     *
     * This form replaces the old Mental Health form.
     * Users record a mood score and optional notes.
     *
     * Payload is prepared in camelCase to match Spring Boot DTO / Entity naming.
     */
    private fun setupMentalHealthForm() {
        addSectionDescription("Record your mood score and optional notes.")

        val scoreInput = addInput(
            label = "Mood Score",
            hint = "1 = very low, 10 = very good",
            inputType = InputType.TYPE_CLASS_NUMBER
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "How do you feel today?",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Mood")
        saveButton.setOnClickListener {
            val scoreText = scoreInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (scoreText.isEmpty()) {
                scoreInput.error = "Mood score is required"
                return@setOnClickListener
            }

            val score = scoreText.toIntOrNull()
            if (score == null || score !in 1..10) {
                scoreInput.error = "Please enter a score from 1 to 10"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("moodRating", score)
                put("notes", notes)
                put("loggedAt", getCurrentDateTimeForBackend())
            }

            savePayloadLocallyForNow("Mood", payload)
        }
    }

    /**
     * Kept for compatibility only.
     * Badges are usually generated automatically, not manually entered by users.
     */
    private fun setupBadgesForm() {
        addSectionDescription(
            "Badges are usually generated automatically. This temporary form can be adjusted later."
        )

        val badgeNameInput = addInput(
            label = "Badge Name",
            hint = "e.g. 7-day streak",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val notesInput = addInput(
            label = "Notes (optional)",
            hint = "Reason or description",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save Badge")
        saveButton.setOnClickListener {
            val badgeName = badgeNameInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            if (badgeName.isEmpty()) {
                badgeNameInput.error = "Badge name is required"
                return@setOnClickListener
            }

            val payload = JSONObject().apply {
                put("metricType", "badge")
                put("metricValue", badgeName)
                put("unit", "text")
                put("notes", notes)
            }

            savePayloadLocallyForNow("Badge", payload)
        }
    }

    private fun setupGenericMetricForm(itemName: String) {
        addSectionDescription("Record a wellness metric.")

        val valueInput = addInput(
            label = "$itemName Value",
            hint = "Enter value",
            inputType = decimalInputType()
        )

        val unitInput = addInput(
            label = "Unit",
            hint = "Enter unit",
            inputType = InputType.TYPE_CLASS_TEXT
        )

        val saveButton = addButton("Save $itemName")
        saveButton.setOnClickListener {
            saveGenericMetric(
                metricType = itemName.lowercase().replace(" ", "_"),
                valueInput = valueInput,
                unitInput = unitInput,
                displayName = itemName
            )
        }
    }

    private fun saveGenericMetric(
        metricType: String,
        valueInput: EditText,
        unitInput: EditText,
        displayName: String
    ) {
        val value = valueInput.text.toString().trim()
        val unit = unitInput.text.toString().trim()

        if (value.isEmpty()) {
            valueInput.error = "Value is required"
            return
        }

        if (unit.isEmpty()) {
            unitInput.error = "Unit is required"
            return
        }

        val payload = JSONObject().apply {
            put("metricType", metricType)
            put("metricValue", value.toDoubleOrNull() ?: value)
            put("unit", unit)
        }

        savePayloadLocallyForNow(displayName, payload)
    }

    /**
     * Temporary save behavior.
     *
     * Later this should be replaced by Retrofit API calls, for example:
     * POST /api/wellness/food
     * POST /api/wellness/exercise
     * POST /api/wellness/mood
     */
    private fun savePayloadLocallyForNow(formName: String, payload: JSONObject) {
        Toast.makeText(
            requireContext(),
            "$formName ready to save: $payload",
            Toast.LENGTH_LONG
        ).show()

        dismiss()
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
                InputType.TYPE_NUMBER_FLAG_DECIMAL or
                InputType.TYPE_NUMBER_FLAG_SIGNED
    }

    private fun getCurrentDateTimeForBackend(): String {
        val formatter = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            java.util.Locale.getDefault()
        )
        return formatter.format(java.util.Date())
    }

    private fun normalizeMealType(input: String): String {
        return when (input.trim().lowercase()) {
            "breakfast" -> "BREAKFAST"
            "brunch" -> "BRUNCH"
            "morning snack" -> "MORNING_SNACK"
            "morning tea" -> "MORNING_TEA"
            "tea break" -> "TEA_BREAK"
            "lunch" -> "LUNCH"
            "afternoon snack" -> "AFTERNOON_SNACK"
            "afternoon tea" -> "AFTERNOON_TEA"
            "dinner" -> "DINNER"
            "supper" -> "SUPPER"
            "snack" -> "SNACK"
            "dessert" -> "DESSERT"
            "pre workout" -> "PRE_WORKOUT"
            "post workout" -> "POST_WORKOUT"
            "midnight meal" -> "MIDNIGHT_MEAL"
            "beverage" -> "BEVERAGE"
            else -> "OTHER"
        }
    }

    private fun normalizeExerciseType(input: String): String {
        return when (input.trim().lowercase()) {
            "run", "running" -> "RUNNING"
            "walk", "walking" -> "WALKING"
            "swim", "swimming" -> "SWIMMING"
            "hike", "hiking" -> "HIKING"
            "cycle", "cycling", "bike", "biking" -> "CYCLING"
            "jog", "jogging" -> "JOGGING"
            "strength training" -> "STRENGTH_TRAINING"
            "weightlifting", "weight lifting" -> "WEIGHTLIFTING"
            "bodyweight training" -> "BODYWEIGHT_TRAINING"
            "hiit" -> "HIIT"
            "crossfit" -> "CROSSFIT"
            "yoga" -> "YOGA"
            "pilates" -> "PILATES"
            "stretching" -> "STRETCHING"
            "rowing" -> "ROWING"
            "jump rope" -> "JUMP_ROPE"
            "dancing" -> "DANCING"
            "basketball" -> "BASKETBALL"
            "football" -> "FOOTBALL"
            "badminton" -> "BADMINTON"
            "tennis" -> "TENNIS"
            "volleyball" -> "VOLLEYBALL"
            "martial arts" -> "MARTIAL_ARTS"
            "climbing" -> "CLIMBING"
            else -> "OTHER"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}